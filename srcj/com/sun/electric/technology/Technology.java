/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Technology.java
 *
 * Copyright (c) 2003 Sun Microsystems and Static Free Software
 *
 * Electric(tm) is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Electric(tm) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Electric(tm); see the file COPYING.  If not, write to
 * the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, Mass 02111-1307, USA.
 */
package com.sun.electric.technology;

import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.geometry.EMath;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitiveArc;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.technology.technologies.Schematics;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.technology.technologies.CMOS;
import com.sun.electric.technology.technologies.MoCMOS;
import com.sun.electric.technology.technologies.MoCMOSOld;
import com.sun.electric.technology.technologies.MoCMOSSub;
import com.sun.electric.technology.technologies.nMOS;
import com.sun.electric.tool.drc.DRC;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.ui.EditWindow;

import java.awt.Dimension;
import java.awt.geom.Dimension2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Comparator;
import java.util.Collections;
import java.util.prefs.Preferences;
import java.util.prefs.BackingStoreException;

/**
 * Technology is the base class for all of the specific technologies in Electric.
 *
 * It is organized into two main areas: nodes and arcs.
 * Both nodes and arcs are composed of Layers.
 *<P>
 * Subclasses of Technology usually start by defining the Layers (such as Metal-1, Metal-2, etc.)
 * Then the PrimitiveArc objects are created, built entirely from Layers.
 * Next PrimitiveNode objects are created, and they have Layers as well as connectivity to the PrimitiveArcs.
 * The Technology concludes with miscellaneous data assignments of technology-wide information.
 * <P>
 * Here are the nodes in a sample CMOS technology.
 * Note that there are two types of transistors and diffusion contacts, one for Well and one for Substrate.
 * Each layer that can exist as a wire must have a pin node (in this case, metal, polysilicon, and two flavors of diffusion.
 * Note that there are pure-layer nodes at the bottom which allow arbitrary geometry to be constructed.
 * <CENTER><IMG SRC="doc-files/Technology-1.gif"></CENTER>
 * <P>
 * The Schematic technology has some unusual features.
 * <CENTER><IMG SRC="doc-files/Technology-2.gif"></CENTER>
 * <P>
 * Conceptually, a Technology has 3 types of information:
 * <UL><LI><I>Geometry</I>.  Each node and arc can be described in terms of polygons on differnt Layers.
 * The ArcLayer and NodeLayer subclasses help define those polygons.
 * <LI><I>Connectivity</I>.  The very structure of the nodes and arcs establisheds a set of rules of connectivity.
 * Examples include the list of allowable arc types that may connect to each port, and the use of port "network numbers"
 * to identify those that are connected internally.
 * <LI><I>Behavior</I>.  Behavioral information takes many forms, but they can all find a place here.
 * For example, each layer, node, and arc has a "function" that describes its general behavior.
 * Some information applies to the technology as a whole, for example SPICE model cards.
 * Other examples include Design Rules and technology characteristics.
 * </UL>
 * @author Steven M. Rubin
 */
public class Technology extends ElectricObject
{
	/** technology is not electrical */									private static final int NONELECTRICAL =       01;
	/** has no directional arcs */										private static final int NODIRECTIONALARCS =   02;
	/** has no negated arcs */											private static final int NONEGATEDARCS =       04;
	/** nonstandard technology (cannot be edited) */					private static final int NONSTANDARD =        010;
	/** statically allocated (don't deallocate memory) */				private static final int STATICTECHNOLOGY =   020;
	/** no primitives in this technology (don't auto-switch to it) */	private static final int NOPRIMTECHNOLOGY =   040;

	/**
	 * Defines a single layer of a PrimitiveArc.
	 * A PrimitiveArc has a list of these ArcLayer objects, one for
	 * each layer in a typical ArcInst.
	 * Each PrimitiveArc is composed of a number of ArcLayer descriptors.
	 * A descriptor converts a specific ArcInst into a polygon that describe this particular layer.
	 */
	public static class ArcLayer
	{
		private Layer layer;
		private double offset;
		private Poly.Type style;

		/**
		 * Constructs an <CODE>ArcLayer</CODE> with the specified description.
		 * @param layer the Layer of this ArcLayer.
		 * @param offset the distance from the outside of the ArcInst to this ArcLayer.
		 * @param style the Poly.Style of this ArcLayer.
		 */
		public ArcLayer(Layer layer, double offset, Poly.Type style)
		{
			this.layer = layer;
			this.offset = offset;
			this.style = style;
		}

		/**
		 * Returns the Layer from the Technology to be used for this ArcLayer.
		 * @return the Layer from the Technology to be used for this ArcLayer.
		 */
		public Layer getLayer() { return layer; }

		/**
		 * Returns the distance from the outside of the ArcInst to this ArcLayer.
		 * This is the difference between the width of this layer and the overall width of the arc.
		 * For example, a value of 4 on an arc that is 6 wide indicates that this layer should be only 2 wide.
		 * @return the distance from the outside of the ArcInst to this ArcLayer.
		 */
		public double getOffset() { return offset; }

		/**
		 * Sets the distance from the outside of the ArcInst to this ArcLayer.
		 * This is the difference between the width of this layer and the overall width of the arc.
		 * For example, a value of 4 on an arc that is 6 wide indicates that this layer should be only 2 wide.
		 * @param offset the distance from the outside of the ArcInst to this ArcLayer.
		 */
		public void setOffset(double offset) { this.offset = offset; }

		/**
		 * Returns the Poly.Style of this ArcLayer.
		 * @return the Poly.Style of this ArcLayer.
		 */
		public Poly.Type getStyle() { return style; }
	}

	/**
	 * Defines a point in space that is relative to a NodeInst's bounds.
	 * The TechPoint has two coordinates: X and Y.
	 * Each of these coordinates is represented by an Edge class (EdgeH for X
	 * and EdgeV for Y).
	 * The Edge classes have two numbers: a multiplier and an adder.
	 * The desired coordinate takes the NodeInst's center, adds in the
	 * product of the Edge multiplier and the NodeInst's size, and then adds
	 * in the Edge adder.
	 * <P>
	 * Arrays of TechPoint objects can be used to describe the bounds of
	 * a particular layer in a NodeInst.  Typically, four TechPoint objects
	 * can describe a rectangle.  Circles only need two (center and edge).
	 * The <CODE>Poly.Style</CODE> class defines the possible types of
	 * geometry.
	 * @see EdgeH
	 * @see EdgeV
	 */
	public static class TechPoint
	{
		private EdgeH x;
		private EdgeV y;

		/**
		 * Constructs a <CODE>TechPoint</CODE> with the specified description.
		 * @param x the EdgeH that converts a NodeInst into an X coordinate on that NodeInst.
		 * @param y the EdgeV that converts a NodeInst into a Y coordinate on that NodeInst.
		 */
		public TechPoint(EdgeH x, EdgeV y)
		{
			this.x = x;
			this.y = y;
		}

		/**
		 * Method to make a 2-long TechPoint array that describes a point at the center of the node.
		 * @return a new TechPoint array that describes a point at the center of the node.
		 */
		public static TechPoint [] makeCenterBox()
		{
			return new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromCenter(0), EdgeV.fromCenter(0)),
					new Technology.TechPoint(EdgeH.fromCenter(0), EdgeV.fromCenter(0))};
		}

		/**
		 * Method to make a 2-long TechPoint array that describes a box that fills the node.
		 * @return a new TechPoint array that describes a box that fills the node.
		 */
		public static TechPoint [] makeFullBox()
		{
			return makeIndented(0);
		}

		/**
		 * Method to make a 2-long TechPoint array that describes indentation by a specified amount.
		 * @param amount the amount to indent the box.
		 * @return a new TechPoint array that describes this indented box.
		 */
		public static TechPoint [] makeIndented(double amount)
		{
			return new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(amount), EdgeV.fromBottom(amount)),
					new Technology.TechPoint(EdgeH.fromRight(amount), EdgeV.fromTop(amount))};
		}

		/**
		 * Returns the EdgeH that converts a NodeInst into an X coordinate on that NodeInst.
		 * @return the EdgeH that converts a NodeInst into an X coordinate on that NodeInst.
		 */
		public EdgeH getX() { return x; }

		/**
		 * Returns the EdgeV that converts a NodeInst into a Y coordinate on that NodeInst.
		 * @return the EdgeV that converts a NodeInst into a Y coordinate on that NodeInst.
		 */
		public EdgeV getY() { return y; }
	}

	/**
	 * Defines a single layer of a PrimitiveNode.
	 * A PrimitiveNode has a list of these NodeLayer objects, one for
	 * each layer in a typical NodeInst.
	 * Each PrimitiveNode is composed of a number of NodeLayer descriptors.
	 * A descriptor converts a specific NodeInst into a polygon that describe this particular layer.
	 */
	public static class NodeLayer
	{
		private Layer layer;
		private int portNum;
		private Poly.Type style;
		private int representation;
		private TechPoint [] points;
		private String message;
		private TextDescriptor descriptor;
		private double lWidth, rWidth, extentT, extendB;

		// the meaning of "representation"
		/**
		 * Indicates that the "points" list defines scalable points.
		 * Each point here becomes a point on the Poly.
		 */
		public static final int POINTS = 0;

		/**
		 * Indicates that the "points" list defines a rectangle.
		 * It contains two diagonally opposite points.
		 */
		public static final int BOX = 1;

		/**
		 * Indicates that the "points" list defines a minimum sized rectangle.
		 * It contains two diagonally opposite points, like BOX,
		 * and also contains a minimum box size beyond which the polygon will not shrink
		 * (again, two diagonally opposite points).
		 */
		public static final int MINBOX = 2;

		/**
		 * Constructs a <CODE>NodeLayer</CODE> with the specified description.
		 * @param layer the <CODE>Layer</CODE> this is on.
		 * @param portNum a 0-based index of the port (from the actual NodeInst) on this layer.
		 * A negative value indicates that this layer is not connected to an electrical layer.
		 * @param style the Poly.Type this NodeLayer will generate (polygon, circle, text, etc.).
		 * @param representation tells how to interpret "points".  It can be POINTS, BOX, or MINBOX.
		 * @param points the list of coordinates (stored as TechPoints) associated with this NodeLayer.
		 */
		public NodeLayer(Layer layer, int portNum, Poly.Type style, int representation, TechPoint [] points)
		{
			this.layer = layer;
			this.portNum = portNum;
			this.style = style;
			this.representation = representation;
			this.points = points;
			this.lWidth = this.rWidth = this.extentT = this.extendB = 0;
		}

		/**
		 * Constructs a <CODE>NodeLayer</CODE> with the specified description.
		 * @param layer the <CODE>Layer</CODE> this is on.
		 * @param portNum a 0-based index of the port (from the actual NodeInst) on this layer.
		 * A negative value indicates that this layer is not connected to an electrical layer.
		 * @param style the Poly.Type this NodeLayer will generate (polygon, circle, text, etc.).
		 * @param representation tells how to interpret "points".  It can be POINTS, BOX, or MINBOX.
		 * @param points the list of coordinates (stored as TechPoints) associated with this NodeLayer.
		 * @param lWidth the extension to the left of this layer (serpentine transistors only).
		 * @param rWidth the extension to the right of this layer (serpentine transistors only).
		 * @param extentT the extension to the top of this layer (serpentine transistors only).
		 * @param extendB the extension to the bottom of this layer (serpentine transistors only).
		 */
		public NodeLayer(Layer layer, int portNum, Poly.Type style, int representation, TechPoint [] points,
			double lWidth, double rWidth, double extentT, double extendB)
		{
			this.layer = layer;
			this.portNum = portNum;
			this.style = style;
			this.representation = representation;
			this.points = points;
			this.lWidth = lWidth;
			this.rWidth = rWidth;
			this.extentT = extentT;
			this.extendB = extendB;
		}

		/**
		 * Returns the <CODE>Layer</CODE> object associated with this NodeLayer.
		 * @return the <CODE>Layer</CODE> object associated with this NodeLayer.
		 */
		public Layer getLayer() { return layer; }

		/**
		 * Returns the 0-based index of the port associated with this NodeLayer.
		 * @return the 0-based index of the port associated with this NodeLayer.
		 */
		public int getPortNum() { return portNum; }

		/**
		 * Returns the Poly.Type this NodeLayer will generate.
		 * @return the Poly.Type this NodeLayer will generate.
		 * Examples are polygon, lines, splines, circle, text, etc.
		 */
		public Poly.Type getStyle() { return style; }

		/**
		 * Returns the method of interpreting "points".
		 * @return the method of interpreting "points".
		 * It can be POINTS, BOX, ABSPOINTS, or MINBOX.
		 */
		public int getRepresentation() { return representation; }

		/**
		 * Returns the list of coordinates (stored as TechPoints) associated with this NodeLayer.
		 * @return the list of coordinates (stored as TechPoints) associated with this NodeLayer.
		 */
		public TechPoint [] getPoints() { return points; }

		/**
		 * Returns the left edge coordinate (a scalable EdgeH object) associated with this NodeLayer.
		 * @return the left edge coordinate associated with this NodeLayer.
		 * It only makes sense if the representation is BOX or MINBOX.
		 * The returned coordinate is a scalable EdgeH object.
		 */
		public EdgeH getLeftEdge() { return points[0].getX(); }

		/**
		 * Returns the bottom edge coordinate (a scalable EdgeV object) associated with this NodeLayer.
		 * @return the bottom edge coordinate associated with this NodeLayer.
		 * It only makes sense if the representation is BOX or MINBOX.
		 * The returned coordinate is a scalable EdgeV object.
		 */
		public EdgeV getBottomEdge() { return points[0].getY(); }

		/**
		 * Returns the right edge coordinate (a scalable EdgeH object) associated with this NodeLayer.
		 * @return the right edge coordinate associated with this NodeLayer.
		 * It only makes sense if the representation is BOX or MINBOX.
		 * The returned coordinate is a scalable EdgeH object.
		 */
		public EdgeH getRightEdge() { return points[1].getX(); }

		/**
		 * Returns the top edge coordinate (a scalable EdgeV object) associated with this NodeLayer.
		 * @return the top edge coordinate associated with this NodeLayer.
		 * It only makes sense if the representation is BOX or MINBOX.
		 * The returned coordinate is a scalable EdgeV object.
		 */
		public EdgeV getTopEdge() { return points[1].getY(); }

		/**
		 * Returns the text message associated with this list NodeLayer.
		 * @return the text message associated with this list NodeLayer.
		 * This only makes sense if the style is one of the TEXT types.
		 */
		public String getMessage() { return message; }

		/**
		 * Sets the text to be drawn by this NodeLayer.
		 * @param message the text to be drawn by this NodeLayer.
		 * This only makes sense if the style is one of the TEXT types.
		 */
		public void setMessage(String message) { this.message = message; }

		/**
		 * Returns the text descriptor associated with this list NodeLayer.
		 * @return the text descriptor associated with this list NodeLayer.
		 * This only makes sense if the style is one of the TEXT types.
		 */
		public TextDescriptor getDescriptor() { return descriptor; }

		/**
		 * Sets the text descriptor to be drawn by this NodeLayer.
		 * @param descriptor the text descriptor to be drawn by this NodeLayer.
		 * This only makes sense if the style is one of the TEXT types.
		 */
		public void setDescriptor(TextDescriptor descriptor) { this.descriptor = descriptor; }

		/**
		 * Returns the left extension of this layer.
		 * Only makes sense when this is a layer in a serpentine transistor.
		 * @return the left extension of this layer.
		 */
		public double getSerpentineLWidth() { return lWidth; }
		/**
		 * Sets the left extension of this layer.
		 * Only makes sense when this is a layer in a serpentine transistor.
		 * @param lWidth the left extension of this layer.
		 */
		public void setSerpentineLWidth(double lWidth) { this.lWidth = lWidth; }

		/**
		 * Returns the right extension of this layer.
		 * Only makes sense when this is a layer in a serpentine transistor.
		 * @return the right extension of this layer.
		 */
		public double getSerpentineRWidth() { return rWidth; }
		/**
		 * Sets the right extension of this layer.
		 * Only makes sense when this is a layer in a serpentine transistor.
		 * @param rWidth the right extension of this layer.
		 */
		public void setSerpentineRWidth(double rWidth) { this.rWidth = rWidth; }

		/**
		 * Returns the top extension of this layer.
		 * Only makes sense when this is a layer in a serpentine transistor.
		 * @return the top extension of this layer.
		 */
		public double getSerpentineExtentT() { return extentT; }
		/**
		 * Sets the top extension of this layer.
		 * Only makes sense when this is a layer in a serpentine transistor.
		 * @param extentT the top extension of this layer.
		 */
		public void setSerpentineExtentT(double extentT) { this.extentT = extentT; }

		/**
		 * Returns the bottom extension of this layer.
		 * Only makes sense when this is a layer in a serpentine transistor.
		 * @return the bottom extension of this layer.
		 */
		public double getSerpentineExtentB() { return extendB; }
		/**
		 * Sets the bottom extension of this layer.
		 * Only makes sense when this is a layer in a serpentine transistor.
		 * @param extendB the bottom extension of this layer.
		 */
		public void setSerpentineExtentB(double extendB) { this.extendB = extendB; }
	}

	/** name of the technology */						private String techName;
	/** full description of the technology */			private String techDesc;
	/** flags for the technology */						private int userBits;
	/** 0-based index of the technology */				private int techIndex;
	/** critical dimensions for the technology */		private double scale;
	/** list of layers in the technology */				private List layers;
	/** count of layers in the technology */			private int layerIndex = 0;
	/** list of primitive nodes in the technology */	private List nodes;
	/** list of arcs in the technology */				private List arcs;
	/** minimum resistance in this Technology. */		private double minResistance;
	/** minimum capacitance in this Technology. */		private double minCapacitance;
	/** true if parasitic overrides were examined. */	private boolean parasiticOverridesGathered = false;

	/* static list of all Technologies in Electric */	private static List technologies = new ArrayList();
	/* the current technology in Electric */			private static Technology curTech = null;
	/* the current tlayout echnology in Electric */		private static Technology curLayoutTech = null;
	/* counter for enumerating technologies */			private static int techNumber = 0;

	/****************************** CONTROL ******************************/

	/**
	 * Constructs a <CODE>Technology</CODE>.
	 * This should not be called directly, but instead is invoked through each subclass's factory.
	 */
	protected Technology()
	{
		this.layers = new ArrayList();
		this.nodes = new ArrayList();
		this.arcs = new ArrayList();
		this.scale = 1.0;
		this.techIndex = techNumber++;
		userBits = 0;

		// add the technology to the global list
		technologies.add(this);
	}

	/**
	 * This is called once, at the start of Electric, to initialize the technologies.
	 * Because of Java's "lazy evaluation", the only way to force the technology constructors to fire
	 * and build a proper list of technologies, is to call each class.
	 * So, each technology is listed here.  If a new technology is created, this must be added to this list.
	 */
	public static void initAllTechnologies()
	{
		// Because of lazy evaluation, technologies aren't initialized unless they're referenced here
		Artwork.tech.setup();
		CMOS.tech.setup();
		MoCMOS.tech.setup();
		MoCMOSOld.tech.setup();
		MoCMOSSub.tech.setup();
		nMOS.tech.setup();
		Schematics.tech.setup();
		Generic.tech.setup();

		// set the current technology
		MoCMOS.tech.setCurrent();

		// setup the generic technology to handle all connections
		Generic.tech.makeUnivList();
	}

	/**
	 * Method to initialize a technology.
	 * Calls the technology's specific "init()" method (if any).
	 * Also sets up mappings from pseudo-layers to real layers.
	 */
	protected void setup()
	{
		// do any specific intialization
		init();

		// setup mapping from pseudo-layers to real layers
		for(Iterator it = this.getLayers(); it.hasNext(); )
		{
			Layer layer = (Layer)it.next();
			int extras = layer.getFunctionExtras();
			if ((extras & Layer.Function.PSEUDO) == 0) continue;
			Layer.Function fun = layer.getFunction();
			for(Iterator oIt = this.getLayers(); oIt.hasNext(); )
			{
				Layer oLayer = (Layer)oIt.next();
				int oExtras = oLayer.getFunctionExtras();
				Layer.Function oFun = oLayer.getFunction();
				if (oFun == fun && (oExtras == (extras & ~Layer.Function.PSEUDO)))
				{
					layer.setNonPseudoLayer(oLayer);
					break;
				}
			}
		}
	}

	/**
	 * Method to initialize a technology.
	 * It gets overridden by individual technologies.
	 */
	public void init() {}

	/**
	 * Returns the current Technology.
	 * @return the current Technology.
	 * The current technology is maintained by the system as a default
	 * in situations where a technology cannot be determined.
	 */
	public static Technology getCurrent() { return curTech; }

	/**
	 * Set this to be the current Technology
	 * The current technology is maintained by the system as a default
	 * in situations where a technology cannot be determined.
	 */
	public void setCurrent()
	{
		curTech = this;
		if (this != Generic.tech && this != Schematics.tech && this != Artwork.tech)
			curLayoutTech = this;
	}

	/**
	 * Returns the total number of Technologies currently in Electric.
	 * @return the total number of Technologies currently in Electric.
	 */
	public static int getNumTechnologies()
	{
		return technologies.size();
	}

	/**
	 * Find the Technology with a particular name.
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
	 * Get an iterator over all of the Technologies.
	 * @return an iterator over all of the Technologies.
	 */
	public static Iterator getTechnologies()
	{
		return technologies.iterator();
	}

	static class TechnologyCaseInsensitive implements Comparator
	{
		public int compare(Object o1, Object o2)
		{
			Technology c1 = (Technology)o1;
			Technology c2 = (Technology)o2;
			String s1 = c1.getTechName();
			String s2 = c2.getTechName();
			return s1.compareToIgnoreCase(s2);
		}
	}

	/**
	 * Method to return an iterator over all libraries.
	 * @return an iterator over all libraries.
	 */
	public static List getTechnologiesSortedByName()
	{
		List sortedList = new ArrayList();
		for(Iterator it = getTechnologies(); it.hasNext(); )
			sortedList.add(it.next());
		Collections.sort(sortedList, new TechnologyCaseInsensitive());
		return sortedList;
	}

	/****************************** LAYERS ******************************/

	/**
	 * Returns an Iterator on the Layers in this Technology.
	 * @return an Iterator on the Layers in this Technology.
	 */
	public Iterator getLayers()
	{
		return layers.iterator();
	}

	/**
	 * Returns the number of Layers in this Technology.
	 * @return the number of Layers in this Technology.
	 */
	public int getNumLayers()
	{
		return layers.size();
	}

	/**
	 * Method to find a Layer with a given name.
	 * @param layerName the name of the desired Layer.
	 * @return the Layer with that name (null if none found).
	 */
	public Layer findLayer(String layerName)
	{
		for(Iterator it = getLayers(); it.hasNext(); )
		{
			Layer layer = (Layer)it.next();
			if (layer.getName().equalsIgnoreCase(layerName)) return layer;
		}
		return null;
	}

	/**
	 * Method to add a new Layer to this Technology.
	 * This is usually done during initialization.
	 * @param layer the Layer to be added to this Technology.
	 */
	public void addLayer(Layer layer)
	{
		layer.setIndex(layerIndex++);
		layers.add(layer);
	}

	/**
	 * Method to tell whether two layers should be considered equivalent for the purposes of cropping.
	 * @param layer1 the first Layer.
	 * @param layer2 the second Layer.
	 * @return true if the layers are equivalent.
	 */
	public boolean sameLayer(Layer layer1, Layer layer2)
	{
		// SHOULD USE EQUIVALENCE TABLES!!!!
		return layer1 == layer2;
	}

	/****************************** ARCS ******************************/

	/**
	 * Returns the PrimitiveArc in this technology with a particular name.
	 * @param name the name of the PrimitiveArc.
	 * @return the PrimitiveArc in this technology with that name.
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
	 * Returns an Iterator on the PrimitiveArc objects in this technology.
	 * @return an Iterator on the PrimitiveArc objects in this technology.
	 */
	public Iterator getArcs()
	{
		return arcs.iterator();
	}

	/**
	 * Returns the number of PrimitiveArc objects in this technology.
	 * @return the number of PrimitiveArc objects in this technology.
	 */
	public int getNumArcs()
	{
		return arcs.size();
	}

	/**
	 * Method to add a new PrimitiveArc to this Technology.
	 * This is usually done during initialization.
	 * @param ap the PrimitiveArc to be added to this Technology.
	 */
	public void addArcProto(PrimitiveArc ap)
	{
		arcs.add(ap);
	}

	/**
	 * Sets the technology to have no directional arcs.
	 * Users should never call this method.
	 * It is set once by the technology during initialization.
	 * Directional arcs are those with arrows on them, indicating (only graphically) the direction of flow through the arc.
	 * @see ArcInst#setDirectional
	 * @see ArcProto#setDirectional
	 */
	protected void setNoDirectionalArcs() { userBits |= NODIRECTIONALARCS; }

	/**
	 * Returns true if this technology does not have directional arcs.
	 * @return true if this technology does not have directional arcs.
	 * Directional arcs are those with arrows on them, indicating (only graphically) the direction of flow through the arc.
	 * @see ArcInst#setDirectional
	 * @see ArcProto#setDirectional
	 */
	public boolean isNoDirectionalArcs() { return (userBits & NODIRECTIONALARCS) != 0; }

	/**
	 * Sets the technology to have no negated arcs.
	 * Users should never call this method.
	 * It is set once by the technology during initialization.
	 * Negated arcs have bubbles on them to graphically indicated negation.
	 * Only Schematics and related technologies allow negated arcs.
	 * @see ArcInst#setNegated
	 * @see ArcProto#setNegated
	 */
	protected void setNoNegatedArcs() { userBits |= NONEGATEDARCS; }

	/**
	 * Returns true if this technology does not have negated arcs.
	 * @return true if this technology does not have negated arcs.
	 * Negated arcs have bubbles on them to graphically indicated negation.
	 * Only Schematics and related technologies allow negated arcs.
	 * @see ArcInst#setNegated
	 * @see ArcProto#setNegated
	 */
	public boolean isNoNegatedArcs() { return (userBits & NONEGATEDARCS) != 0; }

	/**
	 * Returns the polygons that describe arc "ai".
	 * @param ai the ArcInst that is being described.
	 * @param wnd the window in which this arc is being displayed.
	 * @return an array of Poly objects that describes this ArcInst graphically.
	 * This array includes displayable variables on the ArcInst.
	 */
	public Poly [] getShapeOfArc(ArcInst ai, EditWindow wnd)
	{
		// get information about the arc
		PrimitiveArc ap = (PrimitiveArc)ai.getProto();
		Technology tech = ap.getTechnology();
		Technology.ArcLayer [] primLayers = ap.getLayers();

		// see how many polygons describe this arc
		boolean addArrow = false;
		if (!tech.isNoDirectionalArcs() && ai.isDirectional()) addArrow = true;
		int numDisplayable = ai.numDisplayableVariables(true);
		if (wnd == null) numDisplayable = 0;
		int maxPolys = primLayers.length + numDisplayable;
		if (addArrow) maxPolys++;
		Poly [] polys = new Poly[maxPolys];
		int polyNum = 0;

		// construct the polygons that describe the basic arc
		for(int i = 0; i < primLayers.length; i++)
		{
			Technology.ArcLayer primLayer = primLayers[i];
			polys[polyNum] = ai.makePoly(ai.getLength(), ai.getWidth() - primLayer.getOffset(), primLayer.getStyle());
			if (polys[polyNum] == null) return null;
			polys[polyNum].setLayer(primLayer.getLayer());
			polyNum++;
		}

		// add an arrow to the arc description
		if (addArrow)
		{
			Point2D headLoc = ai.getHead().getLocation();
			Point2D tailLoc = ai.getTail().getLocation();
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
			Point2D [] points = new Point2D.Double[numPoints];
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
		if (numDisplayable > 0)
		{
			Rectangle2D rect = ai.getBounds();
			ai.addDisplayableVariables(rect, polys, polyNum, wnd, true);
		}

		return polys;
	}

	/**
	 * Method to convert old primitive arc names to their proper ArcProtos.
	 * This method is overridden by those technologies that have any special arc name conversion issues.
	 * By default, there is nothing to be done, because by the time this
	 * method is called, normal searches have failed.
	 * @param name the unknown arc name, read from an old Library.
	 * @return the proper PrimitiveArc to use for this name.
	 */
	public PrimitiveArc convertOldArcName(String name) { return null; }

	/****************************** NODES ******************************/

	/**
	 * Returns the PrimitiveNode in this technology with a particular name.
	 * @param name the name of the PrimitiveNode.
	 * @return the PrimitiveNode in this technology with that name.
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
	 * Returns an Iterator on the PrimitiveNode objects in this technology.
	 * @return an Iterator on the PrimitiveNode objects in this technology.
	 */
	public Iterator getNodes()
	{
		return nodes.iterator();
	}

	/**
	 * Returns the number of PrimitiveNodes objects in this technology.
	 * @return the number of PrimitiveNodes objects in this technology.
	 */
	public int getNumNodes()
	{
		return nodes.size();
	}

	/**
	 * Method to add a new PrimitiveNode to this Technology.
	 * This is usually done during initialization.
	 * @param np the PrimitiveNode to be added to this Technology.
	 */
	public void addNodeProto(PrimitiveNode np)
	{
		nodes.add(np);
	}

	/**
	 * Method to return the pure "NodeProto Function" a primitive NodeInst in this Technology.
	 * This method is overridden by technologies (such as Schematics) that know the node's function.
	 * @param ni the NodeInst to check.
	 * @return the NodeProto.Function that describes the NodeInst.
	 */
	public NodeProto.Function getPrimitiveFunction(NodeInst ni) { return ni.getProto().getFunction(); }

	/**
	 * Method to return the size of a transistor NodeInst in this Technology.
     * You should most likely be calling NodeInst.getTransistorSize instead of this.
	 * @param ni the NodeInst.
     * @param context the VarContext in which any vars will be evaluated,
     * pass in VarContext.globalContext if no context needed.
	 * @return the size of the NodeInst.
	 */
	public Dimension getTransistorSize(NodeInst ni, VarContext context)
	{
		PrimitiveNode np = (PrimitiveNode)ni.getProto();
		SizeOffset so = np.getSizeOffset();
		double width = ni.getXSize() - so.getLowXOffset() - so.getHighXOffset();
		double height = ni.getYSize() - so.getLowYOffset() - so.getHighYOffset();
		Dimension dim = new Dimension();
		dim.setSize(width, height);
		return dim;
	}

    /**
     * Method to return a gate PortInst for this transistor NodeInst.
     * Implementation Note: May want to make this a more general
     * method, getPrimitivePort(PortType), if the number of port
     * types increases.  Note: You should be calling 
     * NodeInst.getTransistorGatePort() instead of this, most likely.
     * @param ni the NodeInst
     * @return a PortInst for the gate of the transistor
     */
	public PortInst getTransistorGatePort(NodeInst ni) { return ni.getPortInst(0); }
    
    /**
     * Method to return a gate PortInst for this transistor NodeInst.
     * Implementation Note: May want to make this a more general
     * method, getPrimitivePort(PortType), if the number of port
     * types increases.  Note: You should be calling 
     * NodeInst.getTransistorSourcePort() instead of this, most likely.
     * @param ni the NodeInst
     * @return a PortInst for the gate of the transistor
     */
	public PortInst getTransistorSourcePort(NodeInst ni) { return ni.getPortInst(1); }

    /**
     * Method to return a gate PortInst for this transistor NodeInst.
     * Implementation Note: May want to make this a more general
     * method, getPrimitivePort(PortType), if the number of port
     * types increases.  Note: You should be calling 
     * NodeInst.getTransistorDrainPort() instead of this, most likely.
     * @param ni the NodeInst
     * @return a PortInst for the gate of the transistor
     */
	public PortInst getTransistorDrainPort(NodeInst ni) { return ni.getPortInst(3); }

    /**
	 * Method to set the pure "NodeProto Function" for a primitive NodeInst in this Technology.
	 * This method is overridden by technologies (such as Schematics) that can change a node's function.
	 * @param ni the NodeInst to check.
	 * @param function the NodeProto.Function to set on the NodeInst.
	 */
	public void setPrimitiveFunction(NodeInst ni, NodeProto.Function function) {}

	/**
	 * Sets the technology to have no primitives.
	 * Users should never call this method.
	 * It is set once by the technology during initialization.
	 * This indicates to the user interface that it should not switch to this technology.
	 * The FPGA technology has this bit set because it initially contains no primitives,
	 * and they are only created dynamically.
	 */
	public void setNoPrimitiveNodes() { userBits |= NOPRIMTECHNOLOGY; }

	/**
	 * Returns true if this technology has no primitives.
	 * @return true if this technology has no primitives.
	 * This indicates to the user interface that it should not switch to this technology.
	 * The FPGA technology has this bit set because it initially contains no primitives,
	 * and they are only created dynamically.
	 */
	public boolean isNoPrimitiveNodes() { return (userBits & NOPRIMTECHNOLOGY) != 0; }

    /**
	 * Method to set default outline information on a NodeInst.
	 * Very few primitives have default outline information (usually just in the Artwork Technology).
	 * This method is overridden by the appropriate technology.
	 * @param ni the NodeInst to load with default outline information.
	 */
	public void setDefaultOutline(NodeInst ni) {}

	public static SizeOffset getSizeOffset(NodeInst ni)
	{
		NodeProto np = ni.getProto();
		return np.getSizeOffset();
	}

	private static final Technology.NodeLayer [] nullPrimLayers = new Technology.NodeLayer [0];

	/**
	 * Returns the polygons that describe node "ni".
	 * @param ni the NodeInst that is being described.
	 * The prototype of this NodeInst must be a PrimitiveNode and not a Cell.
	 * @param wnd the window in which this node will be drawn (null if no window scaling should be done).
	 * @return an array of Poly objects that describes this NodeInst graphically.
	 * This array includes displayable variables on the NodeInst.
	 */
	public Poly [] getShapeOfNode(NodeInst ni, EditWindow wnd)
	{
		return getShapeOfNode(ni, wnd, false, false);
	}

	/**
	 * Returns the polygons that describe node "ni".
	 * @param ni the NodeInst that is being described.
	 * The prototype of this NodeInst must be a PrimitiveNode and not a Cell.
	 * @param wnd the window in which this node will be drawn (null if no window scaling should be done).
	 * @param electrical true to get the "electrical" layers.
	 * When electrical layers are requested, each layer is tied to a specific port on the node.
	 * If any piece of geometry covers more than one port,
	 * it must be split for the purposes of an "electrical" description.
	 * For example, the MOS transistor has 2 layers: Active and Poly.
	 * But it has 3 electrical layers: Active, Active, and Poly.
	 * The active must be split since each half corresponds to a different PrimitivePort on the PrimitiveNode.
	 * @param reasonable true to get only a minimal set of contact cuts in large contacts.
	 * The minimal set covers all edge contacts, but ignores the inner cuts in large contacts.
	 * @return an array of Poly objects that describes this NodeInst graphically.
	 * This array includes displayable variables on the NodeInst.
	 */
	public Poly [] getShapeOfNode(NodeInst ni, EditWindow wnd, boolean electrical, boolean reasonable)
	{
		NodeProto prototype = ni.getProto();
		if (!(prototype instanceof PrimitiveNode)) return null;

		PrimitiveNode np = (PrimitiveNode)prototype;
		Technology.NodeLayer [] primLayers = np.getLayers();
		if (electrical)
		{
			Technology.NodeLayer [] eLayers = np.getElectricalLayers();
			if (eLayers != null) primLayers = eLayers;
		}

		// if node is erased, remove layers
		if (ni.isWiped()) primLayers = nullPrimLayers; else
		{
			if (np.isWipeOn1or2())
			{
				if (ni.pinUseCount()) primLayers = nullPrimLayers;
			}
		}
		return getShapeOfNode(ni, wnd, electrical, reasonable, primLayers);
	}

	/**
	 * Returns the polygons that describe node "ni", given a set of
	 * NodeLayer objects to use.
	 * @param ni the NodeInst that is being described.
	 * @param wnd the window in which this node will be drawn.
	 * If this is null, no window scaling can be done, so no text is included in the returned results.
	 * @param electrical true to get the "electrical" layers
	 * Like the list returned by "getLayers", the results describe this PrimitiveNode,
	 * but each layer is tied to a specific port on the node.
	 * If any piece of geometry covers more than one port,
	 * it must be split for the purposes of an "electrical" description.<BR>
	 * For example, the MOS transistor has 2 layers: Active and Poly.
	 * But it has 3 electrical layers: Active, Active, and Poly.
	 * The active must be split since each half corresponds to a different PrimitivePort on the PrimitiveNode.
	 * @param reasonable true to get only a minimal set of contact cuts in large contacts.
	 * The minimal set covers all edge contacts, but ignores the inner cuts in large contacts.
	 * @param primLayers an array of NodeLayer objects to convert to Poly objects.
	 * The prototype of this NodeInst must be a PrimitiveNode and not a Cell.
	 * @return an array of Poly objects that describes this NodeInst graphically.
	 * This array includes displayable variables on the NodeInst (if wnd != null).
	 */
	public Poly [] getShapeOfNode(NodeInst ni, EditWindow wnd, boolean electrical, boolean reasonable, Technology.NodeLayer [] primLayers)
	{
		// get information about the node
		double halfWidth = ni.getXSize() / 2;
		double lowX = ni.getTrueCenterX() - halfWidth;
		double highX = ni.getTrueCenterX() + halfWidth;
		double halfHeight = ni.getYSize() / 2;
		double lowY = ni.getTrueCenterY() - halfHeight;
		double highY = ni.getTrueCenterY() + halfHeight;

		PrimitiveNode np = (PrimitiveNode)ni.getProto();
		int specialType = np.getSpecialType();
		double [] specialValues = np.getSpecialValues();
		if (specialType != PrimitiveNode.SERPTRANS && np.isHoldsOutline())
		{
			Point2D [] outline = ni.getTrace();
			if (outline != null)
			{
				int numPolys = 1;
				if (wnd != null) numPolys += ni.numDisplayableVariables(true);
				Poly [] polys = new Poly[numPolys];
				Point2D [] pointList = new Point2D.Double[outline.length];
				for(int i=0; i<outline.length; i++)
				{
					pointList[i] = new Point2D.Double(ni.getTrueCenterX() + outline[i].getX(),
						ni.getTrueCenterY() + outline[i].getY());
				}
				polys[0] = new Poly(pointList);
				Technology.NodeLayer primLayer = primLayers[0];
				polys[0].setStyle(primLayer.getStyle());
				polys[0].setLayer(primLayer.getLayer());
				Rectangle2D rect = ni.getBounds();
				if (wnd != null) ni.addDisplayableVariables(rect, polys, 1, wnd, true);
				return polys;
			}
		}

		// determine the number of polygons (considering that it may be "wiped")
		int numBasicLayers = primLayers.length;

		// if a MultiCut contact, determine the number of extra cuts
		int numExtraCuts = 0;
		MultiCutData mcd = null;
		SerpentineTrans std = null;
		if (specialType == PrimitiveNode.MULTICUT)
		{
			mcd = new MultiCutData(ni, specialValues);
			if (reasonable) numExtraCuts = mcd.cutsReasonable; else
				numExtraCuts = mcd.cutsTotal;
			numBasicLayers--;
		} else if (specialType == PrimitiveNode.SERPTRANS)
		{
			std = new SerpentineTrans(ni, specialValues);
			if (std.layersTotal > 0)
			{
				numExtraCuts = std.layersTotal;
				numBasicLayers = 0;
			}
		}

		// construct the polygon array
		int numPolys = numBasicLayers + numExtraCuts;
		if (wnd != null) numPolys += ni.numDisplayableVariables(true);
		Poly [] polys = new Poly[numPolys];
		
		// add in the basic polygons
		for(int i = 0; i < numBasicLayers; i++)
		{
			Technology.NodeLayer primLayer = primLayers[i];
			int representation = primLayer.getRepresentation();
			Poly.Type style = primLayer.getStyle();
			if (representation == Technology.NodeLayer.BOX || representation == Technology.NodeLayer.MINBOX)
			{
				double portLowX = ni.getTrueCenterX() + primLayer.getLeftEdge().getMultiplier() * ni.getXSize() + primLayer.getLeftEdge().getAdder();
				double portHighX = ni.getTrueCenterX() + primLayer.getRightEdge().getMultiplier() * ni.getXSize() + primLayer.getRightEdge().getAdder();
				double portLowY = ni.getTrueCenterY() + primLayer.getBottomEdge().getMultiplier() * ni.getYSize() + primLayer.getBottomEdge().getAdder();
				double portHighY = ni.getTrueCenterY() + primLayer.getTopEdge().getMultiplier() * ni.getYSize() + primLayer.getTopEdge().getAdder();
				double portX = (portLowX + portHighX) / 2;
				double portY = (portLowY + portHighY) / 2;
				polys[i] = new Poly(portX, portY, portHighX-portLowX, portHighY-portLowY);
			} else if (representation == Technology.NodeLayer.POINTS)
			{
				TechPoint [] points = primLayer.getPoints();
				Point2D [] pointList = new Point2D.Double[points.length];
				for(int j=0; j<points.length; j++)
				{
					EdgeH xFactor = points[j].getX();
					EdgeV yFactor = points[j].getY();
					double x = 0, y = 0;
					if (xFactor != null && yFactor != null)
					{
						x = ni.getTrueCenterX() + xFactor.getMultiplier() * ni.getXSize() + xFactor.getAdder();
						y = ni.getTrueCenterY() + yFactor.getMultiplier() * ni.getYSize() + yFactor.getAdder();
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
				polys[i].setTextDescriptor(primLayer.getDescriptor());
			}
			polys[i].setStyle(style);
			polys[i].setLayer(primLayer.getLayer());
			if (electrical)
			{
				int portIndex = primLayer.getPortNum();
				PortProto port = null;
				if (portIndex >= 0) port = np.getPort(portIndex);
				polys[i].setPort(port);
			}
		}

		// add in the extra contact cuts
		if (mcd != null)
		{
			Technology.NodeLayer primLayer = primLayers[numBasicLayers];
			Poly.Type style = primLayer.getStyle();
			for(int i = 0; i < numExtraCuts; i++)
			{
				polys[numBasicLayers+i] = mcd.fillCutPoly(ni, i);
				polys[numBasicLayers+i].setStyle(style);
				polys[numBasicLayers+i].setLayer(primLayer.getLayer());
			}
		}

		// add in the extra transistor layers
		if (std != null)
		{
			for(int i = 0; i < numExtraCuts; i++)
			{
				polys[numBasicLayers+i] = std.fillTransPoly(ni, i);
			}
		}

		// add in the displayable variables
		if (wnd != null)
		{
			Rectangle2D rect = ni.getBounds();
			ni.addDisplayableVariables(rect, polys, numBasicLayers+numExtraCuts, wnd, true);
		}
		return polys;
	}

	/**
	 * Class MultiCutData determines the locations of cuts in a multi-cut contact node.
	 */
	public static class MultiCutData
	{
		/** the size of each cut */													double cutSizeX, cutSizeY;
		/** the separation between cuts */											double cutSep;
		/** the indent of the edge cuts to the node */								double cutIndent;
		/** the number of cuts in X and Y */										int cutsX, cutsY;
		/** the total number of cuts */												int cutsTotal;
		/** the "reasonable" number of cuts (around the outside only) */			int cutsReasonable;
		/** the X coordinate of the leftmost cut's center */						double cutBaseX;
		/** the Y coordinate of the topmost cut's center */							double cutBaseY;
		/** cut position of last top-edge cut (for interior-cut elimination) */		double cutTopEdge;
		/** cut position of last left-edge cut  (for interior-cut elimination) */	double cutLeftEdge;
		/** cut position of last right-edge cut  (for interior-cut elimination) */	double cutRightEdge;

		/**
		 * Constructor to initialize for multiple cuts.
		 * @param ni the NodeInst with multiple cuts.
		 * @param specialValues the array of special values for the NodeInst.
		 * The values in "specialValues" are:
		 *     cuts sized "cutSizeX" x "cutSizeY" (specialValues[0] x specialValues[1])
		 *     cuts indented at least "cutIndent" from the node edge (specialValues[2])
		 *     cuts separated by "cutSep" (specialValues[3])
		 */
		public MultiCutData(NodeInst ni, double [] specialValues)
		{
			cutSizeX = specialValues[0];
			cutSizeY = specialValues[1];
			cutIndent = specialValues[2];
			cutSep = specialValues[3];

			// determine the actual node size
			PrimitiveNode np = (PrimitiveNode)ni.getProto();
			SizeOffset so = Technology.getSizeOffset(ni);
			double cutLX = so.getLowXOffset();
			double cutHX = so.getHighXOffset();
			double cutLY = so.getLowYOffset();
			double cutHY = so.getHighYOffset();
			double cutAreaWidth = ni.getXSize() - cutLX - cutHX;
			double cutAreaHeight = ni.getYSize() - cutLY - cutHY;

			// number of cuts depends on the size
			cutsX = (int)(cutAreaWidth-cutIndent*2+cutSep) / (int)(cutSizeX+cutSep);
			cutsY = (int)(cutAreaHeight-cutIndent*2+cutSep) / (int)(cutSizeY+cutSep);
			if (cutsX <= 0) cutsX = 1;
			if (cutsY <= 0) cutsY = 1;
			cutsReasonable = cutsTotal = cutsX * cutsY;
			if (cutsTotal != 1)
			{
				// prepare for the multiple contact cut locations
				cutBaseX = (cutAreaWidth-cutIndent*2 - cutSizeX*cutsX -
					cutSep*(cutsX-1)) / 2 + (cutLX + cutIndent + cutSizeX/2) + ni.getGrabCenterX() - ni.getXSize() / 2;
				cutBaseY = (cutAreaHeight-cutIndent*2 - cutSizeY*cutsY -
					cutSep*(cutsY-1)) / 2 + (cutLY + cutIndent + cutSizeY/2) + ni.getGrabCenterY() - ni.getYSize() / 2;
				if (cutsX > 2 && cutsY > 2)
				{
					cutsReasonable = cutsX * 2 + (cutsY-2) * 2;
					cutTopEdge = cutsX*2;
					cutLeftEdge = cutsX*2 + cutsY-2;
					cutRightEdge = cutsX*2 + (cutsY-2)*2;
				}
			}
		}

		/**
		 * Method to return the number of cuts in the contact node.
		 * @return the number of cuts in the contact node.
		 */
		public int numCuts() { return cutsTotal; }

		/**
		 * Method to fill in the contact cuts of a MOS contact when there are
		 * multiple cuts.  Node is in "ni" and the contact cut number (0 based) is
		 * in "cut".
		 */
		private Poly fillCutPoly(NodeInst ni, int cut)
		{
			if (cutsX > 2 && cutsY > 2)
			{
				// rearrange cuts so that the initial ones go around the outside
				if (cut < cutsX)
				{
					// bottom edge: it's ok as is
				} else if (cut < cutTopEdge)
				{
					// top edge: shift up
					cut += cutsX * (cutsY-2);
				} else if (cut < cutLeftEdge)
				{
					// left edge: rearrange
					cut = (int)((cut - cutTopEdge) * cutsX + cutsX);
				} else if (cut < cutRightEdge)
				{
					// right edge: rearrange
					cut = (int)((cut - cutLeftEdge) * cutsX + cutsX*2-1);
				} else
				{
					// center: rearrange and scale down
					cut = cut - (int)cutRightEdge;
					int cutx = cut % (cutsX-2);
					int cuty = cut / (cutsX-2);
					cut = cuty * cutsX + cutx+cutsX+1;
				}
			}

			// locate the X center of the cut
			double cX;
			if (cutsX == 1)
			{
				cX = ni.getTrueCenterX();
			} else
			{
				cX = cutBaseX + (cut % cutsX) * (cutSizeX + cutSep);
			}

			// locate the Y center of the cut
			double cY;
			if (cutsY == 1)
			{
				cY = ni.getTrueCenterY();
			} else
			{
				cY = cutBaseY + (cut / cutsX) * (cutSizeY + cutSep);
			}
			return new Poly(cX, cY, cutSizeX, cutSizeY);
		}
	}

	/**
	 * Class SerpentineTrans here.
	 */
	private static class SerpentineTrans
	{
		/** the number of polygons that make up this serpentine transistor */	int layersTotal;
		/** the number of segments in this serpentine transistor */				int numSegments;
		/** the extra gate width of this serpentine transistor */				double extraScale;
		/** the node layers that make up this serpentine transistor */			Technology.NodeLayer [] primLayers;
		/** the gate coordinates for this serpentine transistor */				Point2D [] points;

		/**
		 * Constructor throws initialize for a serpentine transistor.
		 * @param ni the NodeInst with a serpentine transistor.
		 * @param specialValues the array of special values for the NodeInst.
		 * The values in "specialValues" are:
		 *     layer count is [0]
		 *     active port inset [1] from end of serpentine path
		 *     active port is [2] from poly edge
		 *     poly width is [3]
		 *     poly port inset [4] from poly edge
		 *     poly port is [5] from active edge
		 */
		public SerpentineTrans(NodeInst ni, double [] specialValues)
		{
			layersTotal = 0;
			points = ni.getTrace();
			if (points != null)
			{
				if (points.length < 2) points = null;
			}
			if (points != null)
			{
				PrimitiveNode np = (PrimitiveNode)ni.getProto();
				primLayers = np.getLayers();
				int count = primLayers.length;
				numSegments = points.length - 1;
				layersTotal = count * numSegments;

				extraScale = 0;
				Variable varw = ni.getVar("transistor_width", Integer.class);
				if (varw != null)
				{
					Object obj = varw.getObject();
					extraScale = ((Integer)obj).intValue() / 120 / 2;
				}
			}
		}

		private static final int LEFTANGLE =  900;
		private static final int RIGHTANGLE =  2700;

		/**
		 * Method to describe a box of a serpentine transistor.
		 * If the variable "trace" exists on the node, get that
		 * x/y/x/y information as the centerline of the serpentine path.  The outline is
		 * placed in the polygon "poly".
		 * NOTE: For each trace segment, the left hand side of the trace
		 * will contain the polygons that appear ABOVE the gate in the node
		 * definition. That is, the "top" port and diffusion will be above a
		 * gate segment that extends from left to right, and on the left of a
		 * segment that goes from bottom to top.
		 */
		Poly fillTransPoly(NodeInst ni, int box)
		{
			// compute the segment (along the serpent) and element (of transistor)
			int segment = box % numSegments;
			int element = box / numSegments;

			// see if nonstandard width is specified
			double lwid = primLayers[element].getSerpentineLWidth();
			double rwid = primLayers[element].getSerpentineRWidth();
			double extendt = primLayers[element].getSerpentineExtentT();
			double extendb = primLayers[element].getSerpentineExtentB();
			lwid += extraScale;
			rwid += extraScale;

			// prepare to fill the serpentine transistor
			double xoff = ni.getTrueCenterX();
			double yoff = ni.getTrueCenterY();
			int thissg = segment;   int next = segment+1;
			Point2D thisPt = points[thissg];
			Point2D nextPt = points[next];
			int angle = EMath.figureAngle(thisPt, nextPt);

			// push the points at the ends of the transistor
			if (thissg == 0)
			{
				// extend "thissg" 180 degrees back
				int ang = angle+1800;
				thisPt = EMath.addPoints(thisPt, EMath.cos(ang) * extendt, EMath.sin(ang) * extendt);
			}
			if (next == numSegments)
			{
				// extend "next" 0 degrees forward
				nextPt = EMath.addPoints(nextPt, EMath.cos(angle) * extendb, EMath.sin(angle) * extendb);
			}

			// compute endpoints of line parallel to and left of center line
			int ang = angle+LEFTANGLE;
			double sin = EMath.sin(ang) * lwid;
			double cos = EMath.cos(ang) * lwid;
			Point2D thisL = EMath.addPoints(thisPt, cos, sin);
			Point2D nextL = EMath.addPoints(nextPt, cos, sin);

			// compute endpoints of line parallel to and right of center line
			ang = angle+RIGHTANGLE;
			sin = EMath.sin(ang) * rwid;
			cos = EMath.cos(ang) * rwid;
			Point2D thisR = EMath.addPoints(thisPt, cos, sin);
			Point2D nextR = EMath.addPoints(nextPt, cos, sin);

			// determine proper intersection of this and the previous segment
			if (thissg != 0)
			{
				Point2D otherPt = points[thissg-1];
				int otherang = EMath.figureAngle(otherPt, thisPt);
				if (otherang != angle)
				{
					ang = otherang + LEFTANGLE;
					thisL = EMath.intersect(EMath.addPoints(thisPt, EMath.cos(ang)*lwid, EMath.sin(ang)*lwid),
						otherang, thisL,angle);
					ang = otherang + RIGHTANGLE;
					thisR = EMath.intersect(EMath.addPoints(thisPt, EMath.cos(ang)*rwid, EMath.sin(ang)*rwid),
						otherang, thisR,angle);
				}
			}

			// determine proper intersection of this and the next segment
			if (next != numSegments)
			{
				Point2D otherPt = points[next+1];
				int otherang = EMath.figureAngle(nextPt, otherPt);
				if (otherang != angle)
				{
					ang = otherang + LEFTANGLE;
					Point2D newPtL = EMath.addPoints(nextPt, EMath.cos(ang)*lwid, EMath.sin(ang)*lwid);
					nextL = EMath.intersect(newPtL, otherang, nextL,angle);
					ang = otherang + RIGHTANGLE;
					Point2D newPtR = EMath.addPoints(nextPt, EMath.cos(ang)*rwid, EMath.sin(ang)*rwid);
					nextR = EMath.intersect(newPtR, otherang, nextR,angle);
				}
			}

			// fill the polygon
			Point2D [] points = new Point2D.Double[4];
			points[0] = EMath.addPoints(thisL, xoff, yoff);
			points[1] = EMath.addPoints(thisR, xoff, yoff);
			points[2] = EMath.addPoints(nextR, xoff, yoff);
			points[3] = EMath.addPoints(nextL, xoff, yoff);
			Poly retPoly = new Poly(points);

			// see if the sides of the polygon intersect
//			ang = figureangle(poly->xv[0], poly->yv[0], poly->xv[1], poly->yv[1]);
//			angle = figureangle(poly->xv[2], poly->yv[2], poly->xv[3], poly->yv[3]);
//			if (intersect(poly->xv[0], poly->yv[0], ang, poly->xv[2], poly->yv[2], angle, &x, &y) >= 0)
//			{
//				// lines intersect, see if the point is on one of the lines
//				if (x >= mini(poly->xv[0], poly->xv[1]) && x <= maxi(poly->xv[0], poly->xv[1]) &&
//					y >= mini(poly->yv[0], poly->yv[1]) && y <= maxi(poly->yv[0], poly->yv[1]))
//				{
//					if (abs(x-poly->xv[0])+abs(y-poly->yv[0]) > abs(x-poly->xv[1])+abs(y-poly->yv[1]))
//					{
//						poly->xv[1] = x;   poly->yv[1] = y;
//						poly->xv[2] = poly->xv[3];   poly->yv[2] = poly->yv[3];
//					} else
//					{
//						poly->xv[0] = x;   poly->yv[0] = y;
//					}
//					poly->count = 3;
//				}
//			}

			Technology.NodeLayer primLayer = primLayers[element];
			retPoly.setStyle(primLayer.getStyle());
			retPoly.setLayer(primLayer.getLayer());
			return retPoly;
		}

		/**
		 * Method to describe a port in a transistor that may be part of a serpentine
		 * path.  If the variable "trace" exists on the node, get that x/y/x/y
		 * information as the centerline of the serpentine path.  The port path
		 * is shrunk by "diffinset" in the length and is pushed "diffextend" from the centerline.
		 * The default width of the transistor is "defwid".  The outline is placed
		 * in the polygon "poly".
		 * The assumptions about directions are:
		 * Segments have port 1 to the left, and port 3 to the right of the gate
		 * trace. Port 0, the "left-hand" end of the gate, appears at the starting
		 * end of the first trace segment; port 2, the "right-hand" end of the gate,
		 * appears at the end of the last trace segment.  Port 3 is drawn as a
		 * reflection of port 1 around the trace.
		 * The values "diffinset", "diffextend", "defwid", "polyinset", and "polyextend"
		 * are used to determine the offsets of the ports:
		 * The poly ports are extended "polyextend" beyond the appropriate end of the trace
		 * and are inset by "polyinset" from the polysilicon edge.
		 * The diffusion ports are extended "diffextend" from the polysilicon edge
		 * and set in "diffinset" from the ends of the trace segment.
		 */
//		Poly fillTransPort(NodeInst ni, PortProto *pp, XARRAY trans,
//			TECH_NODES *nodedata, int diffinset, int diffextend, int defwid,
//			int polyinset, int polyextend)
//		{
//			/* see if the transistor has serpentine information */
//			var = gettrace(ni);
//			if (var != NOVARIABLE)
//			{
//				/* trace data is there: make sure there are enough points */
//				total = getlength(var);
//				if (total <= 2) var = NOVARIABLE;
//			}
//
//			/* nonserpentine transtors fill in the normal way */
//			lambda = lambdaofnode(ni);
//			if (var == NOVARIABLE)
//			{
//				tech_fillportpoly(ni, pp, poly, trans, nodedata, -1, lambda);
//				return;
//			}
//
//			/* prepare to fill the serpentine transistor port */
//			list = (INTBIG *)var->addr;
//			poly->style = OPENED;
//			xoff = (ni->highx+ni->lowx)/2;
//			yoff = (ni->highy+ni->lowy)/2;
//			total /= 2;
//
//			/* see if nonstandard width is specified */
//			defwid = lambda * defwid / WHOLE;
//			diffinset = lambda * diffinset / WHOLE;   diffextend = lambda * diffextend / WHOLE;
//			polyinset = lambda * polyinset / WHOLE;   polyextend = lambda * polyextend / WHOLE;
//			varw = getvalkey((INTBIG)ni, VNODEINST, VFRACT, el_transistor_width_key);
//			if (varw != NOVARIABLE) defwid = lambda * varw->addr / WHOLE;
//
//			/* determine which port is being described */
//			for(lpp = ni->proto->firstportproto, which=0; lpp != NOPORTPROTO;
//				lpp = lpp->nextportproto, which++) if (lpp == pp) break;
//
//			/* ports 0 and 2 are poly (simple) */
//			if (which == 0)
//			{
//				if (poly->limit < 2) (void)extendpolygon(poly, 2);
//				thisx = list[0];   thisy = list[1];
//				nextx = list[2];   nexty = list[3];
//				angle = figureangle(thisx, thisy, nextx, nexty);
//				ang = (angle+1800) % 3600;
//				thisx += mult(cosine(ang), polyextend) + xoff;
//				thisy += mult(sine(ang), polyextend) + yoff;
//				ang = (angle+LEFTANGLE) % 3600;
//				nextx = thisx + mult(cosine(ang), defwid/2-polyinset);
//				nexty = thisy + mult(sine(ang), defwid/2-polyinset);
//				xform(nextx, nexty, &poly->xv[0], &poly->yv[0], trans);
//				ang = (angle+RIGHTANGLE) % 3600;
//				nextx = thisx + mult(cosine(ang), defwid/2-polyinset);
//				nexty = thisy + mult(sine(ang), defwid/2-polyinset);
//				xform(nextx, nexty, &poly->xv[1], &poly->yv[1], trans);
//				poly->count = 2;
//				return;
//			}
//			if (which == 2)
//			{
//				if (poly->limit < 2) (void)extendpolygon(poly, 2);
//				thisx = list[(total-1)*2];   thisy = list[(total-1)*2+1];
//				nextx = list[(total-2)*2];   nexty = list[(total-2)*2+1];
//				angle = figureangle(thisx, thisy, nextx, nexty);
//				ang = (angle+1800) % 3600;
//				thisx += mult(cosine(ang), polyextend) + xoff;
//				thisy += mult(sine(ang), polyextend) + yoff;
//				ang = (angle+LEFTANGLE) % 3600;
//				nextx = thisx + mult(cosine(ang), defwid/2-polyinset);
//				nexty = thisy + mult(sine(ang), defwid/2-polyinset);
//				xform(nextx, nexty, &poly->xv[0], &poly->yv[0], trans);
//				ang = (angle+RIGHTANGLE) % 3600;
//				nextx = thisx + mult(cosine(ang), defwid/2-polyinset);
//				nexty = thisy + mult(sine(ang), defwid/2-polyinset);
//				xform(nextx, nexty, &poly->xv[1], &poly->yv[1], trans);
//				poly->count = 2;
//				return;
//			}
//
//			/* THE ORIGINAL CODE TREATED PORT 1 AS THE NEGATED PORT ... SRP */
//			/* port 3 is the negated path side of port 1 */
//			if (which == 3)
//			{
//				diffextend = -diffextend;
//				defwid = -defwid;
//			}
//
//			/* extra port on some n-transistors */
//			if (which == 4) diffextend = defwid = 0;
//
//			/* polygon will need total points */
//			if (poly->limit < total) (void)extendpolygon(poly, total);
//
//			for(next=1; next<total; next++)
//			{
//				thissg = next-1;
//				thisx = list[thissg*2];   thisy = list[thissg*2+1];
//				nextx = list[next*2];   nexty = list[next*2+1];
//				angle = figureangle(thisx, thisy, nextx, nexty);
//
//				/* determine the points */
//				if (thissg == 0)
//				{
//					/* extend "thissg" 0 degrees forward */
//					thisx += mult(cosine(angle), diffinset);
//					thisy += mult(sine(angle), diffinset);
//				}
//				if (next == total-1)
//				{
//					/* extend "next" 180 degrees back */
//					ang = (angle+1800) % 3600;
//					nextx += mult(cosine(ang), diffinset);
//					nexty += mult(sine(ang), diffinset);
//				}
//
//				/* compute endpoints of line parallel to center line */
//				ang = (angle+LEFTANGLE) % 3600;   sin = sine(ang);   cos = cosine(ang);
//				thisx += mult(cos, defwid/2+diffextend);   thisy += mult(sin, defwid/2+diffextend);
//				nextx += mult(cos, defwid/2+diffextend);   nexty += mult(sin, defwid/2+diffextend);
//
//				if (thissg != 0)
//				{
//					/* compute intersection of this and previous line */
//
//					/* LINTED "pthisx", "pthisy", and "pangle" used in proper order */
//					(void)intersect(pthisx, pthisy, pangle, thisx, thisy, angle, &x, &y);
//					thisx = x;   thisy = y;
//					xform(thisx+xoff, thisy+yoff, &poly->xv[thissg], &poly->yv[thissg], trans);
//				} else
//					xform(thisx+xoff, thisy+yoff, &poly->xv[0], &poly->yv[0], trans);
//				pthisx = thisx;   pthisy = thisy;
//				pangle = angle;
//			}
//
//			xform(nextx+xoff, nexty+yoff, &poly->xv[total-1], &poly->yv[total-1], trans);
//			poly->count = total;
//		}
	}

	/**
	 * Method to convert old primitive node names to their proper NodeProtos.
	 * This method is overridden by those technologies that have any special node name conversion issues.
	 * By default, there is nothing to be done, because by the time this
	 * method is called, normal searches have failed.
	 * @param name the unknown node name, read from an old Library.
	 * @return the proper PrimitiveNode to use for this name.
	 */
	public PrimitiveNode convertOldNodeName(String name) { return null; }

	/****************************** PORTS ******************************/

	/**
	 * Returns a polygon that describes a particular port on a NodeInst.
	 * @param ni the NodeInst that has the port of interest.
	 * The prototype of this NodeInst must be a PrimitiveNode and not a Cell.
	 * @param pp the PrimitivePort on that NodeInst that is being described.
	 * @return a Poly object that describes this PrimitivePort graphically.
	 */
	public Poly getShapeOfPort(NodeInst ni, PrimitivePort pp)
	{
		PrimitiveNode np = (PrimitiveNode)ni.getProto();
//		double [] specialValues = np.getSpecialValues();
//		if (np.getSpecialType() == PrimitiveNode.SERPTRANS)
//		{
//			// serpentine transistors use a more complex port determination (tech_filltransport)
//			Poly portpoly = new Poly(0, 0, 0, 0);
//			return portpoly;
//		} else
		{
			// standard port determination, see if there is outline information
			if (np.isHoldsOutline())
			{
				// outline may determine the port
				Point2D [] outline = ni.getTrace();
				if (outline != null)
				{
					double cX = ni.getTrueCenterX();
					double cY = ni.getTrueCenterY();
					Point2D [] pointList = new Point2D.Double[outline.length];
					for(int i=0; i<outline.length; i++)
					{
						pointList[i] = new Point2D.Double(cX + outline[i].getX(), cY + outline[i].getY());
					}
					Poly portPoly = new Poly(pointList);
					portPoly.setStyle(Poly.Type.FILLED);
					portPoly.setTextDescriptor(pp.getTextDescriptor());
					return portPoly;
				}
			}

			// standard port computation
			double halfWidth = ni.getXSize() / 2;
//			double lowX = ni.getTrueCenterX() - halfWidth;
//			double highX = ni.getTrueCenterX() + halfWidth;
//			double halfHeight = ni.getYSize() / 2;
//			double lowY = ni.getTrueCenterY() - halfHeight;
//			double highY = ni.getTrueCenterY() + halfHeight;

			double portLowX = ni.getTrueCenterX() + pp.getLeft().getMultiplier() * ni.getXSize() + pp.getLeft().getAdder();
			double portHighX = ni.getTrueCenterX() + pp.getRight().getMultiplier() * ni.getXSize() + pp.getRight().getAdder();
			double portLowY = ni.getTrueCenterY() + pp.getBottom().getMultiplier() * ni.getYSize() + pp.getBottom().getAdder();
			double portHighY = ni.getTrueCenterY() +pp.getTop().getMultiplier() * ni.getYSize() + pp.getTop().getAdder();
			double portX = (portLowX + portHighX) / 2;
			double portY = (portLowY + portHighY) / 2;
			Poly portPoly = new Poly(portX, portY, portHighX-portLowX, portHighY-portLowY);
			portPoly.setStyle(Poly.Type.FILLED);
			portPoly.setTextDescriptor(pp.getTextDescriptor());
			return portPoly;
		}
	}

	/**
	 * Method to convert old primitive port names to their proper PortProtos.
	 * This method is overridden by those technologies that have any special port name conversion issues.
	 * By default, there is little to be done, because by the time this
	 * method is called, normal searches have failed.
	 * @param portName the unknown port name, read from an old Library.
	 * @param np the PrimitiveNode on which this port resides.
	 * @return the proper PrimitivePort to use for this name.
	 */
	public PrimitivePort convertOldPortName(String portName, PrimitiveNode np)
	{
		// some technologies switched from ports ending in "-bot" to the ending "-bottom"
		int len = portName.length() - 4;
		if (len > 0 && portName.substring(len).equals("-bot"))
		{
			PrimitivePort pp = (PrimitivePort)np.findPortProto(portName + "tom");
			if (pp != null) return pp;
		}
		return null;
	}

	/****************************** PARASITICS ******************************/

	/**
	 * Returns the minimum resistance of this Technology.
	 * @return the minimum resistance of this Technology.
	 */
	public double getMinResistance() { gatherParasiticOverrides();   return minResistance; }

	/**
	 * Sets the minimum resistance of this Technology.
	 * @param minResistance the minimum resistance of this Technology.
	 */
	public void setMinResistance(double minResistance)
	{
		this.minResistance = minResistance;
		Preferences prefs = Preferences.userNodeForPackage(getClass());
		prefs.putDouble("MinResistance_" + techName, minResistance);
		try
		{
	        prefs.flush();
		} catch (BackingStoreException e)
		{
			System.out.println("Failed to save minimum resistance option for technology " + techName);
		}
	}

	/**
	 * Returns the minimum capacitance of this Technology.
	 * @return the minimum capacitance of this Technology.
	 */
	public double getMinCapacitance() { gatherParasiticOverrides();   return minCapacitance; }

	/**
	 * Sets the minimum capacitance of this Technology.
	 * @param minCapacitance the minimum capacitance of this Technology.
	 */
	public void setMinCapacitance(double minCapacitance)
	{
		this.minCapacitance = minCapacitance;
		Preferences prefs = Preferences.userNodeForPackage(getClass());
		prefs.putDouble("MinCapacitance_" + techName, minCapacitance);
		try
		{
	        prefs.flush();
		} catch (BackingStoreException e)
		{
			System.out.println("Failed to save minimum capacitance option for technology " + techName);
		}
	}

	/**
	 * Method to set default parasitic values on this Technology.
	 * These values are not saved in the options.
	 * @param minResistance the minimum resistance in this Technology.
	 * @param minCapacitance the minimum capacitance in this Technology.
	 */
	public void setDefaultParasitics(double minResistance, double minCapacitance)
	{
		this.minResistance = minResistance;
		this.minCapacitance = minCapacitance;
	}

	/**
	 * Method to examine all parasitic overrides for this Layer's Technology.
	 * It only needs to be done once per session, before any of the parasitics
	 * are used.
	 */
	public void gatherParasiticOverrides()
	{
		if (parasiticOverridesGathered) return;
		parasiticOverridesGathered = true;

		Preferences prefs = Preferences.userNodeForPackage(getClass());
		minResistance = prefs.getDouble("MinResistance_" + techName, minResistance);
		minCapacitance = prefs.getDouble("MinCapacitance_" + techName, minCapacitance);

		for(Iterator it = getLayers(); it.hasNext(); )
		{
			Layer layer = (Layer)it.next();
			double resistance = prefs.getDouble("LayerResistance_" + techName + "_" + layer.getName(), layer.getResistance());
			double capacitance = prefs.getDouble("LayerCapacitance_" + techName + "_" + layer.getName(), layer.getCapacitance());
			double edgeCapacitance = prefs.getDouble("LayerEdgeCapacitance_" + techName + "_" + layer.getName(), layer.getEdgeCapacitance());
			layer.setDefaultParasitics(resistance, capacitance, edgeCapacitance);
		}
	}

	/****************************** MISCELANEOUS ******************************/

	/**
	 * Sets the technology to be "non-electrical".
	 * Users should never call this method.
	 * It is set once by the technology during initialization.
	 * Examples of non-electrical technologies are "Artwork" and "Gem".
	 */
	protected void setNonElectrical() { userBits |= NONELECTRICAL; }

	/**
	 * Returns true if this technology is "non-electrical".
	 * @return true if this technology is "non-electrical".
	 * Examples of non-electrical technologies are "Artwork" and "Gem".
	 */
	public boolean isNonElectrical() { return (userBits & NONELECTRICAL) != 0; }

	/**
	 * Sets the technology to be non-standard.
	 * Users should never call this method.
	 * It is set once by the technology during initialization.
	 * A non-standard technology cannot be edited in the technology editor.
	 * Examples are Schematics and Artwork, which have more complex graphics.
	 */
	protected void setNonStandard() { userBits |= NONSTANDARD; }

	/**
	 * Returns true if this technology is non-standard.
	 * @return true if this technology is non-standard.
	 * A non-standard technology cannot be edited in the technology editor.
	 * Examples are Schematics and Artwork, which have more complex graphics.
	 */
	public boolean isNonStandard() { return (userBits & NONSTANDARD) != 0; }

	/**
	 * Sets the technology to be "static".
	 * Users should never call this method.
	 * It is set once by the technology during initialization.
	 * Static technologies are the core set of technologies in Electric that are
	 * essential, and cannot be deleted.
	 * The technology-editor can create others later, and they can be deleted.
	 */
	protected void setStaticTechnology() { userBits |= NONSTANDARD; }

	/**
	 * Returns true if this technoology is "static" (cannot be deleted).
	 * @return true if this technoology is "static" (cannot be deleted).
	 * Static technologies are the core set of technologies in Electric that are
	 * essential, and cannot be deleted.
	 * The technology-editor can create others later, and they can be deleted.
	 */
	public boolean isStaticTechnology() { return (userBits & NONSTANDARD) != 0; }

	/**
	 * Returns the name of this technology.
	 * Each technology has a unique name, such as "mocmos" (MOSIS CMOS).
	 * @return the name of this technology.
	 * @see Technology#setTechName
	 */
	public String getTechName() { return techName; }

	/**
	 * Sets the name of this technology.
	 * Technology names must be unique.
	 */
	protected void setTechName(String techName) { this.techName = techName; }

	/**
	 * Returns the full description of this Technology.
	 * Full descriptions go beyond the one-word technology name by including such
	 * information as foundry, nuumber of available layers, and process specifics.
	 * For example, "Complementary MOS (from MOSIS, Submicron, 2-6 metals [4], double poly)".
	 * @return the full description of this Technology.
	 */
	public String getTechDesc() { return techDesc; }

	/**
	 * Sets the full description of this Technology.
	 * Full descriptions go beyond the one-word technology name by including such
	 * information as foundry, nuumber of available layers, and process specifics.
	 * For example, "Complementary MOS (from MOSIS, Submicron, 2-6 metals [4], double poly)".
	 */
	public void setTechDesc(String techDesc) { this.techDesc = techDesc; }

	/**
	 * Returns the default scale for this Technology.
	 * The technology's scale is for manufacturing output, which must convert
	 * the unit-based values in Electric to real-world values (in nanometers).
	 * @return the default scale for this Technology.
	 */
	public double getScale() { return scale; }

	/**
	 * Sets the default scale of this technology.
	 * The technology's scale is for manufacturing output, which must convert
	 * the unit-based values in Electric to real-world values (in nanometers).
	 * @param scale the new scale between this technology and the real units.
	 */
	public void setScale(double scale)
	{
		if (scale != 0) this.scale = scale;
	}

	/**
	 * Returns the 0-based index of this Technology.
	 * Each Technology has a unique index that can be used for array lookup.
	 * @return the index of this Technology.
	 */
	public int getIndex() { return techIndex; }

	/**
	 * Method to determine whether a new technology with the given name would be legal.
	 * All technology names must be unique, so the name cannot already be in use.
	 * @param techName the name of the new technology that will be created.
	 * @return true if the name is valid.
	 */
	private static boolean validTechnology(String techName)
	{
		if (Technology.findTechnology(techName) != null)
		{
			System.out.println("ERROR: Multiple technologies named " + techName);
			return false;
		}
		return true;
	}

	/*
	 * Method to write a description of this Technology.
	 * Displays the description in the Messages Window.
	 */
	public void getInfo()
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

	/**
	 * Method to determine the appropriate Technology to use for a Cell.
	 * @param cell the Cell to examine.
	 * @return the Technology for that cell.
	 */
	public static Technology whatTechnology(NodeProto cell)
	{
		Technology tech = whatTechnology(cell, null, 0, 0, null, 0, 0);
		return tech;
	}

	/**
	 * Method to determine the appropriate technology to use for a cell.
	 * The contents of the cell can be defined by the lists of NodeInsts and ArcInsts, or
	 * if they are null, then by the contents of the Cell.
	 * @param cell the Cell to examine.
	 * @param nodeProtoList the list of prototypes of NodeInsts in the Cell.
	 * @param startNodeProto the starting point in the "nodeProtoList" array.
	 * @param endNodeProto the ending point in the "nodeProtoList" array.
	 * @param arcProtoList the list of prototypes of ArcInsts in the Cell.
	 * @param startArcProto the starting point in the "arcProtoList" array.
	 * @param endArcProto the ending point in the "arcProtoList" array.
	 * @return the Technology for that cell.
	 */
	public static Technology whatTechnology(NodeProto cell, NodeProto [] nodeProtoList, int startNodeProto, int endNodeProto,
		ArcProto [] arcProtoList, int startArcProto, int endArcProto)
	{
		// primitives know their technology
		if (cell instanceof PrimitiveNode) return(((PrimitiveNode)cell).getTechnology());

		// count the number of technologies
		int maxTech = 0;
		for(Iterator it = Technology.getTechnologies(); it.hasNext(); )
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
				if (np == null) continue;
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
				if (ap == null) continue;
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
		for(Iterator it = Technology.getTechnologies(); it.hasNext(); )
		{
			Technology tech = (Technology)it.next();

			// always ignore the generic technology
			if (tech == Generic.tech) continue;

			// find the most popular of ALL technologies
			if (useCount[tech.getIndex()] > best)
			{
				best = useCount[tech.getIndex()];
				bestTech = tech;
			}

			// find the most popular of the layout technologies
			if (tech == Schematics.tech || tech == Artwork.tech) continue;
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
			if (useCount[Artwork.tech.getIndex()] > 0) return(Artwork.tech);

			// in icons, if there is nothing, presume artwork
			if (bestTech == null) return(Artwork.tech);

			// use artwork as a default
			retTech = Artwork.tech;
		} else if (((Cell)cell).getView() == View.SCHEMATIC)
		{
			// in schematic, if there are any schematic components, use it
			if (useCount[Schematics.tech.getIndex()] > 0) return(Schematics.tech);

			// in schematic, if there is nothing, presume schematic
			if (bestTech == null) return(Schematics.tech);

			// use schematic as a default
			retTech = Schematics.tech;
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

	/**
	 * Returns a printable version of this Technology.
	 * @return a printable version of this Technology.
	 */
	public String toString()
	{
		return "Technology " + techName;
	}

}
