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
 * the Free Software Foundation; either version 3 of the License, or
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

import com.sun.electric.Main;
import com.sun.electric.database.EObjectInputStream;
import com.sun.electric.database.EObjectOutputStream;
import com.sun.electric.database.ImmutableArcInst;
import com.sun.electric.database.ImmutableNodeInst;
import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.geometry.ERectangle;
import com.sun.electric.database.geometry.Orientation;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.EDatabase;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.id.ArcProtoId;
import com.sun.electric.database.id.IdManager;
import com.sun.electric.database.id.PrimitiveNodeId;
import com.sun.electric.database.id.TechId;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.ImmutableArrayList;
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.text.Setting;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.text.Version;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.topology.Geometric;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.UserInterface;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.technology.technologies.EFIDO;
import com.sun.electric.technology.technologies.FPGA;
import com.sun.electric.technology.technologies.GEM;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.technology.technologies.Schematics;
import com.sun.electric.technology.xml.XmlParam;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.erc.ERC;
import com.sun.electric.tool.user.ActivityLogger;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.projectSettings.ProjSettings;
import com.sun.electric.tool.user.projectSettings.ProjSettingsNode;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.NotSerializableException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TreeMap;

import javax.swing.SwingUtilities;

/**
 * Technology is the base class for all of the specific technologies in Electric.
 *
 * It is organized into two main areas: nodes and arcs.
 * Both nodes and arcs are composed of Layers.
 *<P>
 * Subclasses of Technology usually start by defining the Layers (such as Metal-1, Metal-2, etc.)
 * Then the ArcProto objects are created, built entirely from Layers.
 * Next PrimitiveNode objects are created, and they have Layers as well as connectivity to the ArcProtos.
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
public class Technology implements Comparable<Technology>, Serializable
{
	// true to handle duplicate points in an outline as a "break"
	public static final boolean HANDLEBROKENOUTLINES = true;

	private static final boolean LAZY_TECHNOLOGIES = false;
    /** Jelib writes base sizes since this Electric Version */
    public static final Version DISK_VERSION_1 = Version.parseVersion("8.05g");
    /** Jelib writes oversize over standard primitive since this Electric Version */
    public static final Version DISK_VERSION_2 = Version.parseVersion("8.05o");
	public static final Technology[] NULL_ARRAY = {};
	public static final ImmutableArrayList<Technology> EMPTY_LIST = new ImmutableArrayList<Technology>(NULL_ARRAY);

	/** key of Variable for saving scalable transistor contact information. */
	public static final Variable.Key TRANS_CONTACT = Variable.newKey("MOCMOS_transcontacts");

	// strings used in the Component Menu
	public static final String SPECIALMENUCELL   = "Cell";
	public static final String SPECIALMENUMISC   = "Misc.";
	public static final String SPECIALMENUPURE   = "Pure";
	public static final String SPECIALMENUSPICE  = "Spice";
	public static final String SPECIALMENUEXPORT = "Export";
	public static final String SPECIALMENUTEXT   = "Text";
	public static final String SPECIALMENUHIGH   = "High";
	public static final String SPECIALMENUPORT   = "Port";

    public static class Distance implements Serializable {
        public double k;
        public double lambdaValue;
        public final List<DistanceRule> terms = new ArrayList<DistanceRule>();

        public void assign(Distance d) {
            k = d.k;
            lambdaValue = d.lambdaValue;
            for (DistanceRule term: d.terms)
                terms.add(term.clone());
        }

        public Distance clone() {
            Distance d = new Distance();
            d.assign(this);
            return d;
        }

        public double getLambda(DistanceContext context) {
            double value = lambdaValue;
            for (DistanceRule term: terms)
                value += term.getLambda(context);
            return value;
        }
        public void addLambda(double value) {
            lambdaValue += value;
        }
        public void addRule(String ruleName, double k) {
            DistanceRule term = new DistanceRule();
            term.ruleName = ruleName;
            term.k = k;
            terms.add(term);
        }
        public boolean isEmpty() { return lambdaValue == 0 && terms.isEmpty(); }
    }

    public static interface DistanceContext {
        public double getRule(String ruleName);
    }

    public static DistanceContext EMPTY_CONTEXT = new DistanceContext() {
        public double getRule(String ruleName) {
            throw new UnsupportedOperationException();
        }
    };

    public static class DistanceRule implements Serializable, Cloneable {
        public String ruleName;
        public double k;

        public DistanceRule clone() {
            try {
                return (DistanceRule)super.clone();
            } catch (CloneNotSupportedException e) {
                throw new AssertionError();
            }
        }

        private double getLambda(DistanceContext context) {
            return context.getRule(ruleName)*k;
        }
    }

   /**
	 * Defines a single layer of a ArcProto.
	 * A ArcProto has a list of these ArcLayer objects, one for
	 * each layer in a typical ArcInst.
	 * Each ArcProto is composed of a number of ArcLayer descriptors.
	 * A descriptor converts a specific ArcInst into a polygon that describe this particular layer.
	 */
	protected static class ArcLayer
	{
		private final Layer layer;
		private final Poly.Type style;
        private final Distance xmlExtend;
        private int gridExtend;

		/**
		 * Constructs an <CODE>ArcLayer</CODE> with the specified description.
		 * @param layer the Layer of this ArcLayer.
         * @param arcLayerWidth the width of this ArcLayer in standard ArcInst.
		 * @param style the Poly.Style of this ArcLayer.
		 */
        public ArcLayer(Layer layer, double arcLayerWidth, Poly.Type style) {
            this(layer, style, arcLayerWidth*0.5);
            gridExtend = (int)DBMath.lambdaToGrid(arcLayerWidth*0.5);
        }

		/**
		 * Constructs an <CODE>ArcLayer</CODE> with the specified description.
		 * @param layer the Layer of this ArcLayer.
		 * @param style the Poly.Style of this ArcLayer.
         * @param ruleNames rule names to make an expression for for extend of this ArcLayer
		 */
        public ArcLayer(Layer layer, Poly.Type style, String ... ruleNames) {
            this(layer, style, 0, ruleNames);
        }

		/**
		 * Constructs an <CODE>ArcLayer</CODE> with the specified description.
		 * @param layer the Layer of this ArcLayer.
		 * @param style the Poly.Style of this ArcLayer.
         * @param lambdaExtend lambda fraction of extend
         * @param ruleNames rule names to make an expression for for extend of this ArcLayer
		 */
        public ArcLayer(Layer layer, Poly.Type style, double lambdaExtend, String ... ruleNames) {
            this(layer, style, new Distance());
            if (ruleNames.length > 0)
                xmlExtend.addRule(ruleNames[0], 0.5);
            for (int i = 1; i < ruleNames.length; i++)
                xmlExtend.addRule(ruleNames[i], 1);
            xmlExtend.addLambda(DBMath.round(lambdaExtend));
        }

		/**
		 * Constructs an <CODE>ArcLayer</CODE> with the specified description.
		 * @param layer the Layer of this ArcLayer.
         * @param xmlExtend Xml expression for extend of this ArcLayer depending on tech parameters
		 * @param style the Poly.Style of this ArcLayer.
		 */
        public ArcLayer(Layer layer, Poly.Type style, Distance xmlExtend) {
            this(layer, style, 0, xmlExtend);
        }

        private ArcLayer(Layer layer, Poly.Type style, long gridExtend, Distance xmlExtend) {
            if (gridExtend < 0 || gridExtend >= Integer.MAX_VALUE/8)
                throw new IllegalArgumentException("gridExtend=" + gridExtend);
            this.layer = layer;
            this.gridExtend = (int)gridExtend;
            this.style = style;
            this.xmlExtend = xmlExtend;
        }

		/**
		 * Returns the Layer from the Technology to be used for this ArcLayer.
		 * @return the Layer from the Technology to be used for this ArcLayer.
		 */
		Layer getLayer() { return layer; }

		/**
		 * Returns the distance from the center of the standard ArcInst to the outsize of this ArcLayer in grid units.
         * The distance from the center of arbitrary ArcInst ai to the outsize of its ArcLayer is
         * ai.getD().getExtendOverMin() + arcLayer.getGridExtend()
		 * @return the distance from the outside of the ArcInst to this ArcLayer in grid units.
		 */
		int getGridExtend() { return gridExtend; }

		/**
         * Returns ArcLayer which differs from this ArcLayer by extebd.
         * Extend is specified in grid units.
         * The distance from the center of arbitrary ArcInst ai to the outsize of its ArcLayer is
         * ai.getD().getExtendOverMin() + arcLayer.getGridExtend()
         * @param gridExtend new extend to this ArcLayer in grid units.
         */
		ArcLayer withGridExtend(long gridExtend) {
            if (this.gridExtend == gridExtend) return this;
            return new ArcLayer(layer, style, gridExtend, xmlExtend);
        }

		/**
		 * Returns the Poly.Style of this ArcLayer.
		 * @return the Poly.Style of this ArcLayer.
		 */
		Poly.Type getStyle() { return style; }

        void dump(PrintWriter out) {
            out.println("\t\tarcLayer layer=" + layer.getName() +
                    " style=" + style.name() +
                    " extend=" + DBMath.gridToLambda(gridExtend));
        }

        Xml.ArcLayer makeXml() {
            Xml.ArcLayer al = new Xml.ArcLayer();
            al.layer = layer.getName();
            al.style = style;
            al.extend.addLambda(DBMath.gridToLambda(gridExtend));
//            al.extend.assign(xmlExtend);
            return al;
        }

        XmlParam.ArcLayer makeXmlParam() {
            XmlParam.ArcLayer al = new XmlParam.ArcLayer();
            al.layer = layer.getName();
            al.style = style;
            al.extend.assign(xmlExtend);
            return al;
        }

        void resize(DistanceContext context, ArcProto ap) {
            double lambdaExtend = xmlExtend.getLambda(context);
            if (Double.isNaN(lambdaExtend) && !ap.isNotUsed())
            {
                System.out.println("Can't resize arc layer " + layer + " of " + ap.getFullName());
//                lambdaExtend = ap.getLambdaBaseExtend();
            }
            long gridExtend = DBMath.lambdaToGrid(lambdaExtend);
            if (gridExtend < 0 || gridExtend >= Integer.MAX_VALUE/8)
                throw new IllegalArgumentException("gridExtend=" + gridExtend);
            this.gridExtend = (int)gridExtend;
        }
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
	public static class TechPoint implements Serializable
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
		 * Method to make a copy of this TechPoint, with all newly allocated parts.
		 * @return a new TechPoint with the values in this one.
		 */
		public TechPoint duplicate()
		{
			TechPoint newTP = new TechPoint(new EdgeH(x.getMultiplier(), x.getAdder()), new EdgeV(y.getMultiplier(), y.getAdder()));
			return newTP;
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
         * Method similat to makeIndented(double amount) where the X and Y specified amounts are different
         * @param amountX the amount to indent the box along X.
         * @param amountY the amount to indent the box along Y.
         * @return a new TechPoint array that describes this indented box.
         */
        public static TechPoint [] makeIndented(double amountX, double amountY)
		{
			return new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(amountX), EdgeV.fromBottom(amountY)),
					new Technology.TechPoint(EdgeH.fromRight(amountX), EdgeV.fromTop(amountY))};
		}

        /**
         * Method to make a 2-long TechPoint array that describes indentation from the center by a specified amount.
         * @param amountX the amount to indent from the center the box along X.
         * @param amountY the amount to indent from the center the box along Y.
         * @return a new TechPoint array that describes this indented box.
         */
        public static TechPoint [] makeIndentedFromCenter(double amountX, double amountY)
		{
			return new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromCenter(-amountX), EdgeV.fromCenter(-amountY)),
					new Technology.TechPoint(EdgeH.fromCenter(amountX), EdgeV.fromCenter(amountY))};
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

        TechPoint makeCorrection(EPoint correction) {
            EdgeH h = new EdgeH(x.getMultiplier(), x.getAdder() + correction.getLambdaX()*x.getMultiplier()*2);
            EdgeV v = new EdgeV(y.getMultiplier(), y.getAdder() + correction.getLambdaY()*y.getMultiplier()*2);
            return new TechPoint(h, v);
        }
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
        private long cutGridSizeX, cutGridSizeY, cutGridSep1D, cutGridSep2D;
        String sizeRule, cutSep1DRule, cutSep2DRule;

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

//		/**
//		 * Indicates that the "points" list defines a minimum sized rectangle.
//		 * It contains two diagonally opposite points, like BOX,
//		 * and also contains a minimum box size beyond which the polygon will not shrink
//		 * (again, two diagonally opposite points).
//		 */
//		public static final int MINBOX = 2;

		/**
		 * Indicates that the "points" list defines a rectangle,
         * where centers of multi-cut are located
		 * It contains two diagonally opposite points.
		 */
		public static final int MULTICUTBOX = 3;

		/**
		 * Constructs a <CODE>NodeLayer</CODE> with the specified description.
		 * @param layer the <CODE>Layer</CODE> this is on.
		 * @param portNum a 0-based index of the port (from the actual NodeInst) on this layer.
		 * A negative value indicates that this layer is not connected to an electrical layer.
		 * @param style the Poly.Type this NodeLayer will generate (polygon, circle, text, etc.).
		 * @param representation tells how to interpret "points".  It can be POINTS, BOX, or MULTICUTBOX.
		 * @param points the list of coordinates (stored as TechPoints) associated with this NodeLayer.
		 */
		public NodeLayer(Layer layer, int portNum, Poly.Type style, int representation, TechPoint [] points)
		{
			this.layer = layer;
			this.portNum = portNum;
			this.style = style;
			this.representation = representation;
			this.points = points;
			descriptor = TextDescriptor.EMPTY;
			this.lWidth = this.rWidth = this.extentT = this.extendB = 0;
		}

		/**
		 * Constructs a <CODE>NodeLayer</CODE> with the specified description.
		 * This form of the method, with 4 additional parameters at the end,
		 * is only used for serpentine transistors.
		 * @param layer the <CODE>Layer</CODE> this is on.
		 * @param portNum a 0-based index of the port (from the actual NodeInst) on this layer.
		 * A negative value indicates that this layer is not connected to an electrical layer.
		 * @param style the Poly.Type this NodeLayer will generate (polygon, circle, text, etc.).
		 * @param representation tells how to interpret "points".  It can be POINTS, BOX, or MULTICUTBIX.
		 * @param points the list of coordinates (stored as TechPoints) associated with this NodeLayer.
		 * @param lWidth the left extension of this layer, measured from the <I>centerline</I>.
		 * The centerline is the path that the serpentine transistor follows (it defines the path of the
		 * polysilicon).  So, for example, if lWidth is 4 and rWidth is 4, it creates a NodeLayer that is 8 wide
		 * (with 4 to the left and 4 to the right of the centerline).
		 * Left and Right widths define the size of the Active layers.
		 * @param rWidth the right extension the right of this layer, measured from the <I>centerline</I>.
		 * @param extentT the top extension of this layer, measured from the end of the <I>centerline</I>.
		 * The top and bottom extensions apply to the ends of the centerline, and not to each segment
		 * along it.  They define the extension of the polysilicon.  For example, if extendT is 2,
		 * it indicates that the NodeLayer extends by 2 from the top end of the centerline.
		 * @param extendB the bottom extension of this layer, measured from the end of the <I>centerline</I>.
		 */
		public NodeLayer(Layer layer, int portNum, Poly.Type style, int representation, TechPoint [] points,
			double lWidth, double rWidth, double extentT, double extendB)
		{
			this.layer = layer;
			this.portNum = portNum;
			this.style = style;
			this.representation = representation;
			this.points = points;
			descriptor = TextDescriptor.EMPTY;
			this.lWidth = lWidth;
			this.rWidth = rWidth;
			this.extentT = extentT;
			this.extendB = extendB;
		}

        /**
         * Constructs a <CODE>NodeLayer</CODE> from given node
          * @param node
         */
        public NodeLayer(NodeLayer node)
        {
            this.layer = node.getLayerOrPseudoLayer();
			this.portNum = node.getPortNum();
			this.style = node.getStyle();
			this.representation = node.getRepresentation();
            this.descriptor = TextDescriptor.EMPTY;
            TechPoint [] oldPoints = node.getPoints();
			this.points = new TechPoint[oldPoints.length];
			for(int i=0; i<oldPoints.length; i++) points[i] = oldPoints[i].duplicate();
			this.lWidth = this.rWidth = this.extentT = this.extendB = 0;
        }

        public static NodeLayer makeMulticut(Layer layer, int portNum, Poly.Type style, TechPoint[] techPoints,
                String sizeRule, String cutSep1DRule, String cutSep2DRule) {
			NodeLayer nl = new NodeLayer(layer, portNum, style, Technology.NodeLayer.MULTICUTBOX, techPoints);
            nl.sizeRule = sizeRule;
            nl.cutSep1DRule = cutSep1DRule;
            nl.cutSep2DRule = cutSep2DRule;
            return nl;
        }

        public static NodeLayer makeMulticut(Layer layer, int portNum, Poly.Type style, TechPoint[] techPoints,
                double sizeX, double sizeY, double sep1d, double sep2d) {
			NodeLayer nl = new NodeLayer(layer, portNum, style, Technology.NodeLayer.MULTICUTBOX, techPoints);
            nl.cutGridSizeX = DBMath.lambdaToGrid(sizeX);
            nl.cutGridSizeY = DBMath.lambdaToGrid(sizeY);
            nl.cutGridSep1D =  DBMath.lambdaToGrid(sep1d);
            nl.cutGridSep2D =  DBMath.lambdaToGrid(sep2d);
            return nl;
        }

		/**
		 * Returns the <CODE>Layer</CODE> object associated with this NodeLayer.
		 * @return the <CODE>Layer</CODE> object associated with this NodeLayer.
		 */
		public Layer getLayer() { return layer.getNonPseudoLayer(); }

		/**
		 * Tells whether this NodeLayer is associated with pseudo-layer.
		 * @return true if this NodeLayer is associated with pseudo-layer.
		 */
		public boolean isPseudoLayer() { return layer.isPseudoLayer(); }

		/**
		 * Returns the <CODE>Layer</CODE> or pseudo-layer object associated with this NodeLayer.
		 * @return the <CODE>Layer</CODE> or pseudo-layer object associated with this NodeLayer.
		 */
		public Layer getLayerOrPseudoLayer() { return layer; }

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
		 * It can be POINTS, BOX, MINBOX, or MULTICUTBOX.
		 */
		public int getRepresentation() { return representation; }

		public static String getRepresentationName(int rep)
		{
			if (rep == POINTS) return "points";
			if (rep == BOX) return "box";
//			if (rep == MINBOX) return "min-box";
			if (rep == MULTICUTBOX) return "multi-cut-box";
			return "?";
		}

		/**
		 * Returns the list of coordinates (stored as TechPoints) associated with this NodeLayer.
		 * @return the list of coordinates (stored as TechPoints) associated with this NodeLayer.
		 */
		public TechPoint [] getPoints() { return points; }

        /**
         * Method to set new points to this NodeLayer
         * @param pts
         */
        public void setPoints(TechPoint [] pts) {points = pts; }

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
		public void setDescriptor(TextDescriptor descriptor)
		{
			this.descriptor = descriptor;
		}

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

        public double getMulticutSizeX() { return DBMath.gridToLambda(cutGridSizeX); }
        public double getMulticutSizeY() { return DBMath.gridToLambda(cutGridSizeY); }
        public double getMulticutSep1D() { return DBMath.gridToLambda(cutGridSep1D); }
        public double getMulticutSep2D() { return DBMath.gridToLambda(cutGridSep2D); }

        void dump(PrintWriter out, boolean isSerp) {
            out.println("\tlayer=" + getLayerOrPseudoLayer().getName() + " port=" + getPortNum() + " style=" + getStyle().name() + " repr=" + getRepresentation());
            if (getMessage() != null) {
                TextDescriptor td = getDescriptor();
                out.println("\t\tmessage=\"" + getMessage() + "\" td=" + Long.toHexString(td.lowLevelGet()) + " colorIndex=" + td.getColorIndex() + " disp=" + td.isDisplay());
            }
            if (getMulticutSizeX() != 0 || getMulticutSizeY() != 0 || getMulticutSep1D() != 0 || getMulticutSep2D() != 0)
                out.println("\t\tmultiSizeX=" + getMulticutSizeX() + " multiSizeY=" + getMulticutSizeY() + " multiSep=" + getMulticutSep1D() + " multiSpe2D=" + getMulticutSep2D());

            if (isSerp)
                out.println("\t\tLWidth=" + getSerpentineLWidth() + " rWidth=" + getSerpentineRWidth() + " bExtend=" + getSerpentineExtentB() + " tExtend=" + getSerpentineExtentT());
            for (Technology.TechPoint p: getPoints())
                out.println("\t\tpoint xm=" + p.getX().getMultiplier() + " xa=" + p.getX().getAdder() + " ym=" + p.getY().getMultiplier() + " ya=" + p.getY().getAdder());
        }

        Xml.NodeLayer makeXml(boolean isSerp, EPoint correction, boolean inLayers, boolean inElectricalLayers) {
            Xml.NodeLayer nld = new Xml.NodeLayer();
            nld.layer = getLayer().getNonPseudoLayer().getName();
            nld.style = getStyle();
            nld.portNum = getPortNum();
            nld.inLayers = inLayers;
            nld.inElectricalLayers = inElectricalLayers;
            nld.representation = getRepresentation();
            Technology.TechPoint[] points = getPoints();
            if (nld.representation == Technology.NodeLayer.BOX || nld.representation == Technology.NodeLayer.MULTICUTBOX) {
                nld.lx.k = points[0].getX().getMultiplier()*2;
                nld.lx.addLambda(DBMath.round(points[0].getX().getAdder() + correction.getLambdaX()*points[0].getX().getMultiplier()*2));
                nld.hx.k = points[1].getX().getMultiplier()*2;
                nld.hx.addLambda(DBMath.round(points[1].getX().getAdder() + correction.getLambdaX()*points[1].getX().getMultiplier()*2));
                nld.ly.k = points[0].getY().getMultiplier()*2;
                nld.ly.addLambda(DBMath.round(points[0].getY().getAdder() + correction.getLambdaY()*points[0].getY().getMultiplier()*2));
                nld.hy.k = points[1].getY().getMultiplier()*2;
                nld.hy.addLambda(DBMath.round(points[1].getY().getAdder() + correction.getLambdaY()*points[1].getY().getMultiplier()*2));
            } else {
                for (Technology.TechPoint p: points)
                    nld.techPoints.add(p.makeCorrection(correction));
            }
            if (nld.representation == Technology.NodeLayer.MULTICUTBOX) {
                nld.sizex = DBMath.round(getMulticutSizeX());
                nld.sizey = DBMath.round(getMulticutSizeY());
                nld.sep1d = DBMath.round(getMulticutSep1D());
                nld.sep2d = DBMath.round(getMulticutSep2D());
            }
//            nld.sizeRule = sizeRule;
//            nld.sepRule = cutSep1DRule;
//            nld.sepRule2D = cutSep2DRule;
            if (isSerp) {
                nld.lWidth = DBMath.round(getSerpentineLWidth());
                nld.rWidth = DBMath.round(getSerpentineRWidth());
                nld.tExtent = DBMath.round(getSerpentineExtentT());
                nld.bExtent = DBMath.round(getSerpentineExtentB());
            }
            return nld;
        }

        XmlParam.NodeLayer makeXmlParam(boolean isSerp, EPoint correction, boolean inLayers, boolean inElectricalLayers) {
            XmlParam.NodeLayer nld = new XmlParam.NodeLayer();
            nld.layer = getLayer().getNonPseudoLayer().getName();
            nld.style = getStyle();
            nld.portNum = getPortNum();
            nld.inLayers = inLayers;
            nld.inElectricalLayers = inElectricalLayers;
            nld.representation = getRepresentation();
            Technology.TechPoint[] points = getPoints();
            if (nld.representation == Technology.NodeLayer.BOX || nld.representation == Technology.NodeLayer.MULTICUTBOX) {
                nld.lx.k = points[0].getX().getMultiplier()*2;
                nld.lx.addLambda(DBMath.round(points[0].getX().getAdder() + correction.getLambdaX()*points[0].getX().getMultiplier()*2));
                nld.hx.k = points[1].getX().getMultiplier()*2;
                nld.hx.addLambda(DBMath.round(points[1].getX().getAdder() + correction.getLambdaX()*points[1].getX().getMultiplier()*2));
                nld.ly.k = points[0].getY().getMultiplier()*2;
                nld.ly.addLambda(DBMath.round(points[0].getY().getAdder() + correction.getLambdaY()*points[0].getY().getMultiplier()*2));
                nld.hy.k = points[1].getY().getMultiplier()*2;
                nld.hy.addLambda(DBMath.round(points[1].getY().getAdder() + correction.getLambdaY()*points[1].getY().getMultiplier()*2));
            } else {
                for (Technology.TechPoint p: points)
                    nld.techPoints.add(p.makeCorrection(correction));
            }
            nld.sizeRule = sizeRule;
            nld.sepRule = cutSep1DRule;
            nld.sepRule2D = cutSep2DRule;
            if (isSerp) {
                nld.lWidth = DBMath.round(getSerpentineLWidth());
                nld.rWidth = DBMath.round(getSerpentineRWidth());
                nld.tExtent = DBMath.round(getSerpentineExtentT());
                nld.bExtent = DBMath.round(getSerpentineExtentB());
            }
            return nld;
        }

        void resize(DistanceContext context) {
            if (sizeRule != null) {
                double lambdaSize = context.getRule(sizeRule);
                cutGridSizeX = cutGridSizeY = (int)DBMath.lambdaToGrid(lambdaSize);
                double lambdaCutSep1D = context.getRule(cutSep1DRule);
                cutGridSep1D = (int)DBMath.lambdaToGrid(lambdaCutSep1D);
                if (cutSep2DRule != null) {
                    double lambdaCutSep2D = context.getRule(cutSep2DRule);
                    cutGridSep2D = (int)DBMath.lambdaToGrid(lambdaCutSep2D);
                } else {
                    cutGridSep2D = cutGridSep1D;
                }
            }
        }
	}

    public class SizeCorrector {
        public final HashMap<ArcProtoId,Integer> arcExtends = new HashMap<ArcProtoId,Integer>();
        public final HashMap<PrimitiveNodeId,EPoint> nodeExtends = new HashMap<PrimitiveNodeId,EPoint>();

        private SizeCorrector(Version version, boolean isJelib) {
            int techVersion = 0;
            if (isJelib) {
                if (version.compareTo(DISK_VERSION_2) >= 0)
                    techVersion = 2;
                else if (version.compareTo(DISK_VERSION_1) >= 0)
                    techVersion = 1;
            }
            for (ArcProto ap: arcs.values()) {
                int correction = 0;
                switch (techVersion) {
                    case 0:
                        correction = ap.getGridBaseExtend() + (int)DBMath.lambdaToGrid(0.5*ap.getLambdaElibWidthOffset());
                        break;
                    case 1:
                        correction = ap.getGridBaseExtend();
                        break;
                }
                arcExtends.put(ap.getId(), Integer.valueOf(correction));
            }
            for (PrimitiveNode pn: nodes.values()) {
                EPoint correction = techVersion == 2 ? EPoint.ORIGIN : pn.getSizeCorrector(techVersion);
//                switch (techVersion) {
//                    case 0:
//                        correction = EPoint.fromGrid(-pn.sizeCorrector.getGridX(), -pn.sizeCorrector.getGridY());
//                        break;
//                    case 1:
//                        SizeOffset so = pn.getProtoSizeOffset();
//                        double lambdaX = -0.5*(so.getLowXOffset() + so.getHighXOffset()) - pn.sizeCorrector.getLambdaX();
//                        double lambdaY = -0.5*(so.getLowYOffset() + so.getHighYOffset()) - pn.sizeCorrector.getLambdaY();
//                        correction = EPoint.fromLambda(lambdaX, lambdaY);
//                        break;
//                }
                nodeExtends.put(pn.getId(), correction);
            }
        }

        public boolean isIdentity() {
            for (Integer arcExtend: arcExtends.values()) {
                if (arcExtend.intValue() != 0)
                    return false;
            }
            for (EPoint nodeExtend: nodeExtends.values()) {
                if (nodeExtend.getX() != 0 || nodeExtend.getY() != 0)
                    return false;
            }
            return true;
        }

        public long getExtendFromDisk(ArcProto ap, double width) {
            return DBMath.lambdaToGrid(0.5*width) - arcExtends.get(ap.getId()).longValue();
        }

        public long getExtendToDisk(ImmutableArcInst a) {
            return a.getGridExtendOverMin() + arcExtends.get(a.protoId).intValue();
        }

        public long getWidthToDisk(ImmutableArcInst a) {
            return 2*getExtendToDisk(a);
        }

        public EPoint getSizeFromDisk(PrimitiveNode pn, double width, double height) {
            EPoint correction = nodeExtends.get(pn.getId());
            return EPoint.fromLambda(width - 2*correction.getLambdaX(), height - 2*correction.getLambdaY());
        }

        public EPoint getSizeToDisk(ImmutableNodeInst n) {
            EPoint size = n.size;
            EPoint correction = nodeExtends.get(n.protoId);
            if (!correction.equals(EPoint.ORIGIN)) {
                size = EPoint.fromLambda(size.getLambdaX() + 2*correction.getLambdaX(), size.getLambdaY() + 2*correction.getLambdaY());
            }
            return size;
        }
    }

    public SizeCorrector getSizeCorrector(Version version, Map<Setting,Object> projectSettings, boolean isJelib, boolean keepExtendOverMin) {
        return new SizeCorrector(version, isJelib);
    }

    protected void setArcCorrection(SizeCorrector sc, String arcName, double lambdaBaseWidth) {
        ArcProto ap = findArcProto(arcName);
        Integer correction = sc.arcExtends.get(ap.getId());
        int gridBaseExtend = (int)DBMath.lambdaToGrid(0.5*lambdaBaseWidth);
        if (gridBaseExtend != ap.getGridBaseExtend()) {
            correction = Integer.valueOf(correction.intValue() + gridBaseExtend - ap.getGridBaseExtend());
            sc.arcExtends.put(ap.getId(), correction);
        }
    }

	/** technology is not electrical */									private static final int NONELECTRICAL =       01;
	/** has no directional arcs */										private static final int NODIRECTIONALARCS =   02;
	/** has no negated arcs */											private static final int NONEGATEDARCS =       04;
	/** nonstandard technology (cannot be edited) */					private static final int NONSTANDARD =        010;
	/** statically allocated (don't deallocate memory) */				private static final int STATICTECHNOLOGY =   020;
	/** no primitives in this technology (don't auto-switch to it) */	private static final int NOPRIMTECHNOLOGY =   040;

	/** preferences for all technologies */					private static Pref.Group prefs = null;
	/** the current technology in Electric */				private static Technology curTech = null;
	/** the current tlayout echnology in Electric */		private static Technology curLayoutTech = null;
	/** counter for enumerating technologies */				private static int techNumber = 0;

    /** Generic technology for this Technology */           final Generic generic;
	/** name of this technology */							private final TechId techId;
	/** short, readable name of this technology */			private String techShortName;
	/** full description of this technology */				private String techDesc;
	/** flags for this technology */						private int userBits;
	/** 0-based index of this technology */					private int techIndex;
	/** true if "scale" is relevant to this technology */	private boolean scaleRelevant;
	/** number of transparent layers in technology */		private int transparentLayers;
	/** the saved transparent colors for this technology */	private Pref [] transparentColorPrefs;
	/** the color map for this technology */				private Color [] colorMap;
	/** list of layers in this technology */				private final List<Layer> layers = new ArrayList<Layer>();
	/** map from layer names to layers in this technology */private final HashMap<String,Layer> layersByName = new HashMap<String,Layer>();
    /** True when layer allocation is finished. */          private boolean layersAllocationLocked;
	/** list of primitive nodes in this technology */		private final LinkedHashMap<String,PrimitiveNode> nodes = new LinkedHashMap<String,PrimitiveNode>();
    /** array of nodes by nodeId.chronIndex */              private PrimitiveNode[] nodesByChronIndex = {};
    /** Old names of primitive nodes */                     protected final HashMap<String,PrimitiveNode> oldNodeNames = new HashMap<String,PrimitiveNode>();
    /** count of primitive nodes in this technology */      private int nodeIndex = 0;
	/** list of arcs in this technology */					private final LinkedHashMap<String,ArcProto> arcs = new LinkedHashMap<String,ArcProto>();
    /** array of arcs by arcId.chronIndex */                private ArcProto[] arcsByChronIndex = {};
    /** Old names of arcs */                                protected final HashMap<String,ArcProto> oldArcNames = new HashMap<String,ArcProto>();
	/** Spice header cards, level 1. */						private String [] spiceHeaderLevel1;
	/** Spice header cards, level 2. */						private String [] spiceHeaderLevel2;
	/** Spice header cards, level 3. */						private String [] spiceHeaderLevel3;
    /** resolution for this Technology */                   private Pref prefResolution;
    /** static list of all Manufacturers in Electric */     protected final List<Foundry> foundries = new ArrayList<Foundry>();
    /** default foundry for this Technology */              private final Setting cacheFoundry;
	/** scale for this Technology. */						private Setting cacheScale;
    /** number of metals for this Technology. */            private final Setting cacheNumMetalLayers;
	/** Minimum resistance for this Technology. */			private Setting cacheMinResistance;
	/** Minimum capacitance for this Technology. */			private Setting cacheMinCapacitance;
    /** Gate Length subtraction (in microns) for this Tech*/private final Setting cacheGateLengthSubtraction;
    /** Include gate in Resistance calculation */           private final Setting cacheIncludeGate;
    /** Include ground network in parasitics calculation */ private final Setting cacheIncludeGnd;
    /** Include ground network in parasitics calculation */ private final Setting cacheMaxSeriesResistance;
//	/** Logical effort global fanout preference. */			private final Setting cacheGlobalFanout;
//	/** Logical effort convergence (epsilon) preference. */	private final Setting cacheConvergenceEpsilon;
//	/** Logical effort maximum iterations preference. */	private final Setting cacheMaxIterations;
	/** Logical effort gate capacitance preference. */		private Setting cacheGateCapacitance;
	/** Logical effort wire ratio preference. */			private Setting cacheWireRatio;
	/** Logical effort diff alpha preference. */			private Setting cacheDiffAlpha;
//	/** Logical effort keeper ratio preference. */			private final Setting cacheKeeperRatio;

//	/** Default Logical effort global fanout. */			private static double DEFAULT_GLOBALFANOUT = 4.7;
//	/** Default Logical effort convergence (epsilon). */	private static double DEFAULT_EPSILON      = 0.001;
//	/** Default Logical effort maximum iterations. */		private static int    DEFAULT_MAXITER      = 30;
//	/** Default Logical effort keeper ratio. */				private static double DEFAULT_KEEPERRATIO  = 0.1;
	/** Default Logical effort gate capacitance. */			private static double DEFAULT_GATECAP      = 0.4;
	/** Default Logical effort wire ratio. */				private static double DEFAULT_WIRERATIO    = 0.16;
	/** Default Logical effort diff alpha. */				private static double DEFAULT_DIFFALPHA    = 0.7;

	/** To group elements for the component menu */         protected Object[][] nodeGroups;
	/** Default element groups for the component menu */    protected Object[][] factoryNodeGroups;
	/** indicates n-type objects. */						public static final int N_TYPE = 1;
	/** indicates p-type objects. */						public static final int P_TYPE = 0;
	/** Cached rules for the technology. */		            protected DRCRules cachedRules = null;
    /** Xml representation of this Technology */            protected Xml.Technology xmlTech;
    /** Preference for saving component menus */			private Pref componentMenuPref = null;
    /** Preference for saving layer order */				private Pref layerOrderPref = null;

	/****************************** CONTROL ******************************/

	/**
	 * Constructs a <CODE>Technology</CODE>.
	 * This should not be called directly, but instead is invoked through each subclass's factory.
	 */
	protected Technology(Generic generic, String techName) {
        this(generic, techName, Foundry.Type.NONE, 0);
    }

	/**
	 * Constructs a <CODE>Technology</CODE>.
	 * This should not be called directly, but instead is invoked through each subclass's factory.
	 */
	protected Technology(Generic generic, String techName, Foundry.Type defaultFoundry, int defaultNumMetals) {
        this(generic.getId().idManager, generic,techName, defaultFoundry, defaultNumMetals);
    }
	/**
	 * Constructs a <CODE>Technology</CODE>.
	 * This should not be called directly, but instead is invoked through each subclass's factory.
	 */
	protected Technology(IdManager idManager, Generic generic, String techName, Foundry.Type defaultFoundry, int defaultNumMetals)
	{
        if (this instanceof Generic) {
            assert generic == null;
            generic = (Generic)this;
        }
        this.generic = generic;
		this.techId = idManager.newTechId(techName);
		//this.scale = 1.0;
		this.scaleRelevant = true;
		this.techIndex = techNumber++;
		userBits = 0;
		if (prefs == null) prefs = Pref.groupForPackage(Schematics.class);
        cacheFoundry = TechSetting.makeStringSetting(this, "SelectedFoundryFor"+techName,
        	"Technology tab", techName + " foundry", getProjectSettings(), "Foundry", defaultFoundry.getName().toUpperCase());
        cacheNumMetalLayers = TechSetting.makeIntSetting(this, techName + "NumberOfMetalLayers",
            "Technology tab", techName + ": Number of Metal Layers", getProjectSettings(), "NumMetalLayers", defaultNumMetals);

        cacheMaxSeriesResistance = makeParasiticSetting("MaxSeriesResistance", 10.0);
        cacheGateLengthSubtraction = makeParasiticSetting("GateLengthSubtraction", 0.0);
		cacheIncludeGate = makeParasiticSetting("Gate Inclusion", false);
		cacheIncludeGnd = makeParasiticSetting("Ground Net Inclusion", false);
//		cacheGlobalFanout = makeLESetting("GlobalFanout", DEFAULT_GLOBALFANOUT);
//		cacheConvergenceEpsilon = makeLESetting("ConvergenceEpsilon", DEFAULT_EPSILON);
//		cacheMaxIterations = makeLESetting("MaxIterations", DEFAULT_MAXITER);
//		cacheGateCapacitance = makeLESetting("GateCapacitance", DEFAULT_GATECAP);
//		cacheWireRatio = makeLESetting("WireRatio", DEFAULT_WIRERATIO);
//		cacheDiffAlpha = makeLESetting("DiffAlpha", DEFAULT_DIFFALPHA);
//        cacheKeeperRatio = makeLESetting("KeeperRatio", DEFAULT_KEEPERRATIO);
	}

    protected Object writeReplace() { return new TechnologyKey(this); }

    private static class TechnologyKey extends EObjectInputStream.Key<Technology> {
        public TechnologyKey() {}
        private TechnologyKey(Technology tech) { super(tech); }

        @Override
        public void writeExternal(EObjectOutputStream out, Technology tech) throws IOException {
            TechId techId = tech.getId();
            if (techId.idManager != out.getIdManager())
                throw new NotSerializableException(tech + " from other IdManager");
            if (out.getDatabase().getTechPool().getTech(techId) != tech)
                throw new NotSerializableException(tech + " not linked");
            out.writeInt(techId.techIndex);
        }

        @Override
        public Technology readExternal(EObjectInputStream in) throws IOException, ClassNotFoundException {
            int techIndex = in.readInt();
            TechId techId = in.getIdManager().getTechId(techIndex);
            Technology tech = in.getDatabase().getTech(techId);
            if (tech == null)
                throw new InvalidObjectException(techId + " not linked");
            return tech;
        }
    }

    public Technology(Generic generic, Xml.Technology t) {
        this(generic, t.techName, Foundry.Type.valueOf(t.defaultFoundry), t.defaultNumMetals);
        xmlTech = t;
        setTechShortName(t.shortTechName);
        setTechDesc(t.description);
        setFactoryScale(t.scaleValue, t.scaleRelevant);
        setFactoryParasitics(t.minResistance, t.minCapacitance);
        if (!t.transparentLayers.isEmpty())
            setFactoryTransparentLayers(t.transparentLayers.toArray(new Color[t.transparentLayers.size()]));
        HashMap<String,Layer> layers = new HashMap<String,Layer>();
        for (Xml.Layer l: t.layers) {
            Layer layer = Layer.newInstance(this, l.name, l.desc);
            layers.put(l.name, layer);
            layer.setFunction(l.function, l.extraFunction);
            if (l.cif != null)
                layer.setFactoryCIFLayer(l.cif);
            if (l.skill != null)
                layer.setFactorySkillLayer(l.skill);
            layer.setFactory3DInfo(l.thick3D, l.height3D, l.mode3D, l.factor3D);
            layer.setFactoryParasitics(l.resistance, l.capacitance, l.edgeCapacitance);
        }
        HashMap<String,ArcProto> arcs = new HashMap<String,ArcProto>();
        for (Xml.ArcProto a: t.arcs) {
            if (findArcProto(a.name) != null) {
                System.out.println("Error: technology " + getTechName() + " has multiple arcs named " + a.name);
                continue;
            }
            ArcLayer[] arcLayers = new ArcLayer[a.arcLayers.size()];
//            long minGridExtend = Long.MAX_VALUE;
//            long maxGridExtend = Long.MIN_VALUE;
//            for (int i = 0; i < arcLayers.length; i++) {
//                Xml.ArcLayer al = a.arcLayers.get(i);
//                long gridLayerExtend = DBMath.lambdaToGrid(al.extend.getLambda(context));
//                minGridExtend = Math.min(minGridExtend, gridLayerExtend);
//                maxGridExtend = Math.max(maxGridExtend, gridLayerExtend);
//            }
//            if (maxGridExtend < 0 || maxGridExtend > Integer.MAX_VALUE/8) {
//                System.out.println("ArcProto " + getTechName() + ":" + a.name + " has invalid width offset " + DBMath.gridToLambda(2*maxGridExtend));
//                continue;
//            }
//            long gridFullExtend = minGridExtend + DBMath.lambdaToGrid(a.elibWidthOffset*0.5);
            for (int i = 0; i < arcLayers.length; i++) {
                Xml.ArcLayer al = a.arcLayers.get(i);
                Distance d = new Distance();
                d.addLambda(al.extend.value);
                arcLayers[i] = new ArcLayer(layers.get(al.layer),al.style, d);
            }
//            if (minGridExtend < 0 || minGridExtend != DBMath.lambdaToGrid(a.arcLayers.get(0).extend.getLambda(context)))
//            	assert true;
            Double diskOffset1 = a.diskOffset.get(Integer.valueOf(1));
            Double diskOffset2 = a.diskOffset.get(Integer.valueOf(2));
            long halfElibWidthOffset = 0;
            if (diskOffset1 != null && diskOffset2 != null)
                halfElibWidthOffset = DBMath.lambdaToGrid(diskOffset1.doubleValue() - diskOffset2.doubleValue());
            ArcProto ap = new ArcProto(this, a.name, halfElibWidthOffset, 0, a.function, arcLayers, arcs.size());
//            ArcProto ap = new ArcProto(this, a.name, (int)maxGridExtend, (int)minGridExtend, defaultWidth, a.function, arcLayers, arcs.size());
            addArcProto(ap);

            if (a.oldName != null)
                oldArcNames.put(a.oldName, ap);
            arcs.put(a.name, ap);
            if (a.wipable)
                ap.setWipable();
            if (a.curvable)
                ap.setCurvable();
            if (a.special)
                ap.setSpecialArc();
            if (a.notUsed)
                ap.setNotUsed(true);
            if (a.skipSizeInPalette)
                ap.setSkipSizeInPalette();
            ap.setFactoryExtended(a.extended);
            ap.setFactoryFixedAngle(a.fixedAngle);
            ap.setFactoryAngleIncrement(a.angleIncrement);
            ERC.getERCTool().setAntennaRatio(ap, a.antennaRatio);
//            if (a.arcPin != null)
//                ap.makeWipablePin(a.arcPin.name, a.arcPin.portName, a.arcPin.elibSize, makeConnections(a.arcPin.portArcs, arcs));
        }
        setNoNegatedArcs();
        DistanceContext context = EMPTY_CONTEXT;
        for (Xml.PrimitiveNode n: t.nodes) {
            EPoint sizeCorrector1 = n.diskOffset.get(Integer.valueOf(1));
            EPoint sizeCorrector2 = n.diskOffset.get(Integer.valueOf(2));
            if (sizeCorrector2 == null)
                sizeCorrector2 = EPoint.ORIGIN;
            if (sizeCorrector1 == null)
                sizeCorrector1 = sizeCorrector2;
            String minSizeRule = null;
            long lx, hx, ly, hy;
            if (n.nodeSizeRule != null) {
                hx = DBMath.lambdaToGrid(0.5*n.nodeSizeRule.width);
                lx = -hx;
                hy = DBMath.lambdaToGrid(0.5*n.nodeSizeRule.height);
                ly = -hy;
                minSizeRule = n.nodeSizeRule.rule;
            } else {
                lx = Long.MAX_VALUE;
                hx = Long.MIN_VALUE;
                ly = Long.MAX_VALUE;
                hy = Long.MIN_VALUE;
                for (int i = 0; i < n.nodeLayers.size(); i++) {
                    Xml.NodeLayer nl = n.nodeLayers.get(i);
                    long x, y;
                    if (nl.representation == NodeLayer.BOX || nl.representation == NodeLayer.MULTICUTBOX) {
                        x = DBMath.lambdaToGrid(nl.lx.value);
                        lx = Math.min(lx, x);
                        hx = Math.max(hx, x);
                        x = DBMath.lambdaToGrid(nl.hx.value);
                        lx = Math.min(lx, x);
                        hx = Math.max(hx, x);
                        y = DBMath.lambdaToGrid(nl.ly.value);
                        ly = Math.min(ly, y);
                        hy = Math.max(hy, y);
                        y = DBMath.lambdaToGrid(nl.hy.value);
                        ly = Math.min(ly, y);
                        hy = Math.max(hy, y);
                    } else {
                        for (TechPoint p: nl.techPoints) {
                            x = p.x.getGridAdder();
                            lx = Math.min(lx, x);
                            hx = Math.max(hx, x);
                            y = p.y.getGridAdder();
                            ly = Math.min(ly, y);
                            hy = Math.max(hy, y);
                        }
                    }
                }
            }
            ERectangle fullRectangle = ERectangle.fromGrid(lx, ly, hx - lx, hy - ly);
            EPoint fullSize = EPoint.fromGrid((hx - lx + 1)/2, (hy - ly + 1)/2);
            boolean needElectricalLayers = false;
            ArrayList<NodeLayer> nodeLayers = new ArrayList<NodeLayer>();
            ArrayList<NodeLayer> electricalNodeLayers = new ArrayList<NodeLayer>();
            for (int i = 0; i < n.nodeLayers.size(); i++) {
                Xml.NodeLayer nl = n.nodeLayers.get(i);
                TechPoint[] techPoints;
                if (nl.representation == NodeLayer.BOX || nl.representation == NodeLayer.MULTICUTBOX) {
                    techPoints = new TechPoint[2];
                    if (nl.lx.value > nl.hx.value || nl.lx.k > nl.hx.k ||
                            nl.ly.value > nl.hy.value || nl.ly.k > nl.hy.k)
                        System.out.println("Strange polygon in " + getTechName() + ":" + n.name);
                    techPoints[0] = makeTechPoint(nl.lx, nl.ly, context, fullSize);
                    techPoints[1] = makeTechPoint(nl.hx, nl.hy, context, fullSize);
                } else {
                    techPoints = nl.techPoints.toArray(new TechPoint[nl.techPoints.size()]);
                    for (int j = 0; j < techPoints.length; j++)
                        techPoints[j] = makeTechPoint(techPoints[j], fullSize);
                }
                NodeLayer nodeLayer;
                Layer layer = layers.get(nl.layer);
                if (n.shrinkArcs) {
                    if (layer.getPseudoLayer() == null)
                        layer.makePseudo();
                    layer = layer.getPseudoLayer();
                }
                if (nl.representation == NodeLayer.MULTICUTBOX) {
                    nodeLayer = NodeLayer.makeMulticut(layer, nl.portNum, nl.style, techPoints, nl.sizex, nl.sizey, nl.sep1d, nl.sep2d);
                }
                else if (n.specialType == PrimitiveNode.SERPTRANS)
                    nodeLayer = new NodeLayer(layer, nl.portNum, nl.style, nl.representation, techPoints, nl.lWidth, nl.rWidth, nl.tExtent, nl.bExtent);
                else
                    nodeLayer = new NodeLayer(layer, nl.portNum, nl.style, nl.representation, techPoints);
                if (!(nl.inLayers && nl.inElectricalLayers))
                    needElectricalLayers = true;
                if (nl.inLayers)
                    nodeLayers.add(nodeLayer);
                if (nl.inElectricalLayers)
                    electricalNodeLayers.add(nodeLayer);
            }
            if (n.sizeOffset != null) {
                lx += n.sizeOffset.getLowXGridOffset();
                hx -= n.sizeOffset.getHighXGridOffset();
                ly += n.sizeOffset.getLowYGridOffset();
                hy -= n.sizeOffset.getHighYGridOffset();
            }
            ERectangle baseRectangle = ERectangle.fromGrid(lx, ly, hx - lx, hy - ly);
            PrimitiveNode pnp = PrimitiveNode.newInstance(n.name, this, sizeCorrector1, sizeCorrector2, minSizeRule,
                    DBMath.round(n.defaultWidth.value + 2*fullSize.getLambdaX()),
                    DBMath.round(n.defaultHeight.value + 2*fullSize.getLambdaY()),
                    fullRectangle, baseRectangle, nodeLayers.toArray(new NodeLayer[nodeLayers.size()]));
            if (n.oldName != null)
                oldNodeNames.put(n.oldName, pnp);
            pnp.setFunction(n.function);
            if (needElectricalLayers)
                pnp.setElectricalLayers(electricalNodeLayers.toArray(new NodeLayer[electricalNodeLayers.size()]));
            if (n.shrinkArcs) {
                pnp.setArcsWipe();
                pnp.setArcsShrink();
            }
            if (n.square)
                pnp.setSquare();
            if (n.canBeZeroSize)
                pnp.setCanBeZeroSize();
            if (n.wipes)
                pnp.setWipeOn1or2();
            if (n.lockable)
                pnp.setLockedPrim();
            if (n.edgeSelect)
                pnp.setEdgeSelect();
            if (n.skipSizeInPalette)
                pnp.setSkipSizeInPalette();
            if (n.notUsed)
                pnp.setNotUsed(true);
            if (n.lowVt)
                pnp.setNodeBit(PrimitiveNode.LOWVTBIT);
            if (n.highVt)
                pnp.setNodeBit(PrimitiveNode.HIGHVTBIT);
            if (n.nativeBit)
                pnp.setNodeBit(PrimitiveNode.NATIVEBIT);
            if (n.od18)
                pnp.setNodeBit(PrimitiveNode.OD18BIT);
            if (n.od25)
                pnp.setNodeBit(PrimitiveNode.OD25BIT);
            if (n.od33)
                pnp.setNodeBit(PrimitiveNode.OD33BIT);

            PrimitivePort[] ports = new PrimitivePort[n.ports.size()];
            for (int i = 0; i < ports.length; i++) {
                Xml.PrimitivePort p = n.ports.get(i);
                if (p.lx.value > p.hx.value || p.lx.k > p.hx.k || p.ly.value > p.hy.value || p.ly.k > p.hy.k)
                {
                	double lX = p.lx.value - fullSize.getLambdaX()*p.lx.k;
                	double hX = p.hx.value - fullSize.getLambdaX()*p.hx.k;
                	double lY = p.ly.value - fullSize.getLambdaY()*p.ly.k;
                	double hY = p.hy.value - fullSize.getLambdaY()*p.hy.k;
                    String explain = " (LX=" + TextUtils.formatDouble(p.lx.k/2) + "W";
                    if (lX >= 0) explain += "+";
                    explain += TextUtils.formatDouble(lX) + ", HX=" + TextUtils.formatDouble(p.hx.k/2) + "W";
                    if (hX >= 0) explain += "+";
                    explain += TextUtils.formatDouble(hX) + ", LY=" + TextUtils.formatDouble(p.ly.k/2) + "H";
                    if (lY >= 0) explain += "+";
                    explain += TextUtils.formatDouble(lY) + ", HY=" + TextUtils.formatDouble(p.hy.k/2) + "H";
                    if (hY >= 0) explain += "+";
                    explain += TextUtils.formatDouble(hY);
                    explain += " but size is " + fullSize.getLambdaX()*2 + "x" + fullSize.getLambdaY()*2 + ")";
                    System.out.println("Warning: port " + p.name + " in primitive " + getTechName() + ":" + n.name + " has negative size" + explain);
                }
                EdgeH elx = makeEdgeH(p.lx, context, fullSize);
                EdgeH ehx = makeEdgeH(p.hx, context, fullSize);
                EdgeV ely = makeEdgeV(p.ly, context, fullSize);
                EdgeV ehy = makeEdgeV(p.hy, context, fullSize);
                ports[i] = PrimitivePort.newInstance(this, pnp, makeConnections(n.name, p.name, p.portArcs, arcs), p.name,
                        p.portAngle, p.portRange, p.portTopology, PortCharacteristic.UNKNOWN,
                        elx, ely, ehx, ehy);
            }
            pnp.addPrimitivePorts(ports);
            pnp.setSpecialType(n.specialType);
            switch (n.specialType) {
                case com.sun.electric.technology.PrimitiveNode.POLYGONAL:
					pnp.setHoldsOutline();
                    break;
                case com.sun.electric.technology.PrimitiveNode.SERPTRANS:
					pnp.setHoldsOutline();
                    pnp.setCanShrink();
                    pnp.setSpecialValues(n.specialValues);
                    break;
                default:
                    break;
            }
            if (n.function == PrimitiveNode.Function.NODE) {
                assert pnp.getLayers().length == 1;
                Layer layer = pnp.getLayers()[0].getLayer();
                assert layer.getPureLayerNode() == null;
                layer.setPureLayerNode(pnp);
            }
            if (n.spiceTemplate != null)
            	pnp.setSpiceTemplate(n.spiceTemplate);
        }
        for (Xml.Layer l: t.layers) {
            if (l.pureLayerNode == null) continue;
            Layer layer = layers.get(l.name);
            PrimitiveNode pn = layer.makePureLayerNode(l.pureLayerNode.name, l.pureLayerNode.size.value, l.pureLayerNode.style,
                    l.pureLayerNode.port, makeConnections(l.pureLayerNode.name, l.pureLayerNode.port, l.pureLayerNode.portArcs, arcs));
            if (l.pureLayerNode.oldName != null)
                oldNodeNames.put(l.pureLayerNode.oldName, pn);
        }
        convertMenuPalette(t.menuPalette);
        for (Xml.SpiceHeader h: t.spiceHeaders) {
            String[] spiceLines = h.spiceLines.toArray(new String[h.spiceLines.size()]);
            switch (h.level) {
                case 1:
                    setSpiceHeaderLevel1(spiceLines);
                    break;
                case 2:
                    setSpiceHeaderLevel2(spiceLines);
                    break;
                case 3:
                    setSpiceHeaderLevel3(spiceLines);
                    break;
            }
        }
        for (Xml.Foundry f: t.foundries) {
            ArrayList<String> gdsLayers = new ArrayList<String>();
            for (Layer layer: this.layers) {
                String gds = f.layerGds.get(layer.getName());
                if (gds == null) continue;
                gdsLayers.add(layer.getName() + " " + gds);
            }
            Foundry foundry = new Foundry(this, Foundry.Type.valueOf(f.name), f.rules, gdsLayers.toArray(new String[gdsLayers.size()]));
            foundries.add(foundry);
        }
    }

    private ArcProto[] makeConnections(String nodeName, String portName, List<String> portArcs, HashMap<String,ArcProto> arcs) {
        ArcProto[] connections = new ArcProto[portArcs.size()];
        for (int j = 0; j < connections.length; j++) {
            ArcProto ap = arcs.get(portArcs.get(j));
            if (ap == null)
            {
                String error = "No such arcProto '" + portArcs.get(j) + "' found for node '" + nodeName +
                "', port '" + portName + "'";
                throw new NoSuchElementException(error);
            }
            connections[j] = ap;
        }
        return connections;
    }

    private TechPoint makeTechPoint(TechPoint p, EPoint correction) {
        EdgeH h = p.getX();
        EdgeV v = p.getY();
        h = new EdgeH(h.getMultiplier(), h.getAdder() - correction.getLambdaX()*h.getMultiplier()*2);
        v = new EdgeV(v.getMultiplier(), v.getAdder() - correction.getLambdaY()*v.getMultiplier()*2);
        return new TechPoint(h, v);
    }

    private TechPoint makeTechPoint(Xml.Distance x, Xml.Distance y, DistanceContext context, EPoint correction) {
        return new TechPoint(makeEdgeH(x, context, correction), makeEdgeV(y, context, correction));
    }

    private EdgeH makeEdgeH(Xml.Distance x, DistanceContext context, EPoint correction) {
        return new EdgeH(x.k*0.5, x.value - correction.getLambdaX()*x.k);
    }

    private EdgeV makeEdgeV(Xml.Distance y, DistanceContext context, EPoint correction) {
        return new EdgeV(y.k*0.5, y.value - correction.getLambdaY()*y.k);
    }

    private void convertMenuPalette(Xml.MenuPalette menuPalette) {
        if (menuPalette == null) return;
        int numColumns = menuPalette.numColumns;
        ArrayList<Object[]> rows = new ArrayList<Object[]>();
        Object[] row = null;
        for (int i = 0; i < menuPalette.menuBoxes.size(); i++) {
            int column = i % numColumns;
            if (column == 0) {
                row = new Object[numColumns];
                rows.add(row);
            }
            List<Object> menuBoxList = menuPalette.menuBoxes.get(i);
            if (menuBoxList == null || menuBoxList.isEmpty()) continue;
            if (menuBoxList.size() == 1) {
                row[column] = convertMenuItem(menuBoxList.get(0));
            } else {
                ArrayList<Object> list = new ArrayList<Object>();
                for (Object o: menuBoxList)
                {
                	if (o == null) continue;
                    list.add(convertMenuItem(o));
                }
                row[column] = list;
            }
        }
        nodeGroups = factoryNodeGroups = rows.toArray(new Object[rows.size()][]);
    }

    private Object convertMenuItem(Object menuItem) {
        if (menuItem instanceof Xml.ArcProto)
            return findArcProto(((Xml.ArcProto)menuItem).name);
        if (menuItem instanceof Xml.PrimitiveNode)
            return findNodeProto(((Xml.PrimitiveNode)menuItem).name);
        if (menuItem instanceof Xml.MenuNodeInst) {
            Xml.MenuNodeInst n = (Xml.MenuNodeInst)menuItem;
            boolean hasText = (n.text != null);
            PrimitiveNode pn = findNodeProto(n.protoName);
            if (pn != null)
            	return makeNodeInst(pn, n.function, n.rotation, hasText, n.text, n.fontSize);
        }
        return menuItem.toString();
    }

    public Xml.Technology getXmlTech() { return xmlTech; }

    protected void resizeXml(XMLRules rules) {
//        for (Xml.ArcProto xap: xmlTech.arcs) {
//            ArcProto ap = findArcProto(xap.name);
//            assert xap.arcLayers.size() == ap.layers.length;
//            Double widthOffsetObject = xap.widthOffset.get(Integer.valueOf(0))*2;
//            double widthOffset = widthOffsetObject != null ? widthOffsetObject.doubleValue() : 0;
//            for (int i = 0; i < ap.layers.length; i++) {
//                Xml.ArcLayer xal = xap.arcLayers.get(i);
//                double layerWidthOffset = widthOffset - 2*xal.extend.value;
//                ap.layers[i] = ap.layers[i].withGridOffset(DBMath.lambdaToSizeGrid(layerWidthOffset));
//            }
//            ap.computeLayerGridExtendRange();
//        }
    }

	/**
	 * This is called once, at the start of Electric, to initialize the technologies.
	 * Because of Java's "lazy evaluation", the only way to force the technology constructors to fire
	 * and build a proper list of technologies, is to call each class.
	 * So, each technology is listed here.  If a new technology is created, this must be added to this list.
	 */
	public static void initAllTechnologies()
	{
        /** static list of all Technologies in Electric */		TreeMap<String,String> lazyClasses = new TreeMap<String,String>();
        /** static list of xml Technologies in Electric */		TreeMap<String,URL> lazyUrls = new TreeMap<String,URL>();

		// technology initialization may set preferences, so batch them
		Pref.delayPrefFlushing();

		// Because of lazy evaluation, technologies aren't initialized unless they're referenced here
        EDatabase database = EDatabase.serverDatabase();
		sysGeneric = new Generic(database.getIdManager()); sysGeneric.setup(database);
		sysArtwork = new Artwork(sysGeneric); sysArtwork.setup(database);
		sysFPGA = new FPGA(sysGeneric); sysFPGA.setup(database);
		sysSchematics = new Schematics(sysGeneric); sysSchematics.setup(database);

//		MoCMOS.tech.setup();

		// finished batching preferences
		Pref.resumePrefFlushing();

//		// setup the generic technology to handle all connections
//		Generic.tech.makeUnivList();

        lazyUrls.put("bicmos",       Technology.class.getResource("technologies/bicmos.xml"));
        lazyUrls.put("bipolar",      Technology.class.getResource("technologies/bipolar.xml"));
        lazyUrls.put("cmos",         Technology.class.getResource("technologies/cmos.xml"));
        lazyClasses.put("efido",     "com.sun.electric.technology.technologies.EFIDO");
        lazyClasses.put("gem",       "com.sun.electric.technology.technologies.GEM");
        lazyClasses.put("pcb",       "com.sun.electric.technology.technologies.PCB");
        lazyClasses.put("rcmos",     "com.sun.electric.technology.technologies.RCMOS");
        if (true) {
            lazyClasses.put("mocmos","com.sun.electric.technology.technologies.MoCMOS");
        } else {
            lazyUrls.put("mocmos",   Technology.class.getResource("technologies/mocmos.xml"));
        }
        lazyUrls.put("mocmosold",    Technology.class.getResource("technologies/mocmosold.xml"));
        lazyUrls.put("mocmossub",    Technology.class.getResource("technologies/mocmossub.xml"));
        lazyUrls.put("nmos",         Technology.class.getResource("technologies/nmos.xml"));
        lazyUrls.put("tft",          Technology.class.getResource("technologies/tft.xml"));
        lazyUrls.put("tsmc180",      Main.class.getResource("plugins/tsmc/tsmc180.xml"));
        if (true) {
            lazyClasses.put("cmos90","com.sun.electric.plugins.tsmc.CMOS90");
        } else {
            lazyUrls.put("cmos90",   Main.class.getResource("plugins/tsmc/cmos90.xml"));
        }
        List<String> softTechnologies = getSoftTechnologies();
        for(String softTechFile : softTechnologies)
        {
        	URL url = TextUtils.makeURLToFile(softTechFile);
        	if (TextUtils.URLExists(url))
        	{
	        	String softTechName = TextUtils.getFileNameWithoutExtension(url);
	        	lazyUrls.put(softTechName, url);
        	} else
        	{
        		System.out.println("WARNING: could not find added technology: " + softTechFile);
        		System.out.println("  (fix this error in the 'Added Technologies' Project Settings)");
        	}
        }

        if (!LAZY_TECHNOLOGIES) {
            // initialize technologies that may not be present
            for(String techClassName: lazyClasses.values())
                setupTechnology(database, techClassName);
            for(URL techUrl: lazyUrls.values()) {
                if (techUrl != null)
                    setupTechnology(database, techUrl);
            }
        }

		// set the current technology, given priority to user defined
        curLayoutTech = getMocmosTechnology();
        Technology  tech = Technology.findTechnology(User.getDefaultTechnology());
        if (tech == null) tech = curLayoutTech;
        tech.setCurrent();

	}

    protected static Generic sysGeneric;
    protected static Artwork sysArtwork;
    protected static FPGA sysFPGA;
    protected static Schematics sysSchematics;

	private static Pref softTechnologyList;

	/**
	 * Method to get an array of additional technologies that should be added to Electric.
	 * These added technologies are XML files, and the list is the path to those files.
	 * @return an array of additional technologies that should be added to Electric.
	 */
	public static List<String> getSoftTechnologies()
	{
		if (softTechnologyList == null)
			softTechnologyList = Pref.makeStringPref("SoftTechnologies", Technology.prefs, "");
		String techString = softTechnologyList.getString();
		List<String> techList = new ArrayList<String>();
		String [] techArray = techString.split(";");
		for(int i=0; i<techArray.length; i++)
			if (techArray[i].length() > 0) techList.add(techArray[i]);
		return techList;
	}

	/**
	 * Method to set an array of additional technologies that should be added to Electric.
	 * These added technologies are XML files, and the list is the path to those files.
	 * @param a an array of additional technologies that should be added to Electric.
	 */
	public static void setSoftTechnologies(List<String> a)
	{
		StringBuffer sb = new StringBuffer();
		for(int i=0; i<a.size(); i++)
		{
			if (i != 0) sb.append(";");
			sb.append(a.get(i));
		}
		if (softTechnologyList == null)
			softTechnologyList = Pref.makeStringPref("SoftTechnologies", Technology.prefs, "");
		softTechnologyList.setString(sb.toString());
	}

	private static void setupTechnology(EDatabase database, String techClassName) {
        Pref.delayPrefFlushing();
        try {
            Class<?> techClass = Class.forName(techClassName);
            Technology tech = (Technology)techClass.getConstructor(Generic.class)
                    .newInstance(database.getGeneric());
            tech.setup(database);
//            Generic.tech.makeUnivList();
        } catch (ClassNotFoundException e) {
            if (Job.getDebug())
                System.out.println("GNU Release can't find extra technologies");

        } catch (Exception e) {
            System.out.println("Exceptions while importing extra technologies");
            ActivityLogger.logException(e);
        } finally {
            Pref.resumePrefFlushing();
        }
    }

    private static void setupTechnology(EDatabase database, URL urlXml) {
        Pref.delayPrefFlushing();
        try {
            Xml.Technology t = Xml.parseTechnology(urlXml);
            if (t == null)
            {
                throw new Exception("Can't load extra technology: " + urlXml);
            }
            else if (Technology.findTechnology(t.techName) != null)
            {
                // name is being used.
                throw new Exception("Technology with the same name exists: " + t.techName);
            }
            Class<?> techClass = Technology.class;
            if (t.className != null)
                techClass = Class.forName(t.className);
            Technology tech = (Technology)techClass.getConstructor(Generic.class, Xml.Technology.class)
                    .newInstance(database.getGeneric(), t);
            tech.setup(database);
//            Generic.tech.makeUnivList();
        } catch (ClassNotFoundException e) {
            if (Job.getDebug())
                System.out.println("GNU Release can't find extra technologies");

        } catch (Exception e) {
            System.out.println("Can't load extra technology: " + urlXml);
            ActivityLogger.logException(e.getCause() != null ? e.getCause() : e);
        } finally {
            Pref.resumePrefFlushing();
        }
    }

    private static Technology mocmos = null;
    private static boolean mocmosCached = false;
    /**
     * Method to return the MOSIS CMOS technology.
     * @return the MOSIS CMOS technology object.
     */
    public static Technology getMocmosTechnology() {
        if (!mocmosCached) {
            mocmosCached = true;
            mocmos = findTechnology("mocmos");
        }
        return mocmos;
    }

    private static Technology tsmc180 = null;
    private static boolean tsmc180Cached = false;
	/**
	 * Method to return the TSMC 180 nanometer technology.
	 * Since the technology is a "plugin" and not distributed universally, it may not exist.
	 * @return the TSMC180 technology object (null if it does not exist).
	 */
    public static Technology getTSMC180Technology() {
    	if (!tsmc180Cached) {
            tsmc180Cached = true;
            tsmc180 = findTechnology("tsmc180");
            if (tsmc180 == null)
            {
//                System.out.println("Error loading tsmc180");
                tsmc180Cached = false;
            }
        }
 		return tsmc180;
    }

    private static Technology cmos90 = null;
    private static boolean cmos90Cached = false;
	/**
	 * Method to return the CMOS 90 nanometer technology.
	 * Since the technology is a "plugin" and not distributed universally, it may not exist.
	 * @return the CMOS90 technology object (null if it does not exist).
	 */
    public static Technology getCMOS90Technology()
    {
    	if (!cmos90Cached) {
            cmos90Cached = true;
            cmos90 = findTechnology("cmos90");
            if (cmos90 == null)
            {
//                System.out.println("Error loading cmos90");
                cmos90Cached = false;
            }
        }
 		return cmos90;
    }

	/**
	 * Method to initialize a technology.
	 * Calls the technology's specific "init()" method (if any).
	 * Also sets up mappings from pseudo-layers to real layers.
	 */
	public void setup(EDatabase database)
	{
        database.addTech(this);

        // initialize all design rules in the technology (overwrites arc widths)
		setState();

        if (cacheMinResistance == null || cacheMinCapacitance == null) {
            setFactoryParasitics(10, 0);
        }
        if (cacheGateCapacitance == null || cacheWireRatio == null || cacheDiffAlpha == null) {
            setFactoryLESettings(DEFAULT_GATECAP, DEFAULT_WIRERATIO, DEFAULT_DIFFALPHA);
        }
        layersAllocationLocked = true;
        for (Foundry foundry: foundries) {
            foundry.finish();
        }
        for (Layer layer: layers) {
            if (!layer.isPseudoLayer())
                layer.finish();
        }

        check();
	}

	/**
	 * Method to set state of a technology.
	 * It gets overridden by individual technologies.
     */
	public void setState() {
        EDatabase.theDatabase.checkChanging();
        if (xmlTech != null)
            cachedRules = getFactoryDesignRules();
    }

    protected void setNotUsed(int numPolys) {
        int numMetals = getNumMetals();
        for (PrimitiveNode pn: nodes.values()) {
            boolean isUsed = true;
            for (NodeLayer nl: pn.getLayers())
                isUsed = isUsed && nl.getLayer().getFunction().isUsed(numMetals, numPolys);
            pn.setNotUsed(!isUsed);
        }
        for (ArcProto ap: arcs.values()) {
            boolean isUsed = true;
            for (ArcLayer al: ap.layers)
                isUsed = isUsed && al.getLayer().getFunction().isUsed(numMetals, numPolys);
            ap.setNotUsed(!isUsed);
        }
    }

	/**
	 * Returns the current Technology.
	 * @return the current Technology.
	 * The current technology is maintained by the system as a default
	 * in situations where a technology cannot be determined.
	 */
	public static Technology getCurrent()
    {
        if (curTech == null)
        {
            System.out.println("The current technology is null. Check the technology settings.");
            // tries to get the User default
            curTech = findTechnology(User.getDefaultTechnology());
            if (curTech == null)
            {
                System.out.println("User default technology is not loaded. Check the technology settings");
                // tries to get MoCMOS tech
                curTech = getMocmosTechnology();
                if (curTech == null)
                {
                    System.out.println("Major error: MoCMOS technology not loaded. Check the technology settings");
                }
            }
        }
        return curTech;
    }

	/**
	 * Set this to be the current Technology
	 * The current technology is maintained by the system as a default
	 * in situations where a technology cannot be determined.
	 */
	public void setCurrent()
	{
		curTech = this;
		if (isLayout())
			curLayoutTech = this;
	}

//	/**
//	 * Returns the total number of Technologies currently in Electric.
//	 * @return the total number of Technologies currently in Electric.
//	 */
//	public static int getNumTechnologies()
//	{
//		return technologies.size();
//	}

	/**
	 * Find the Technology with a particular name.
	 * @param name the name of the desired Technology
	 * @return the Technology with the same name, or null if no
	 * Technology matches.
	 */
	public static Technology findTechnology(String name)
	{
		if (name == null) return null;
//        TechId techId = EDatabase.theDatabase.getIdManager().newTechId(name);
//		Technology tech = findTechnology(techId);
//		if (tech != null) return tech;

		for (Iterator<Technology> it = getTechnologies(); it.hasNext(); )
		{
			Technology t = it.next();
			if (t.getTechName().equalsIgnoreCase(name))
				return t;
		}
		return null;
	}

	/**
	 * Find the Technology with a particular TechId.
	 * @param techId the TechId of the desired Technology
	 * @return the Technology with the same name, or null if no
	 * Technology matches.
	 */
    public static Technology findTechnology(TechId techId) {
        return EDatabase.theDatabase.getTech(techId);
    }

	/**
	 * Get an iterator over all of the Technologies.
	 * @return an iterator over all of the Technologies.
	 */
	public static Iterator<Technology> getTechnologies()
	{
		return EDatabase.theDatabase.getTechnologies().iterator();
	}

	/**
	 * Method to convert any old-style variable information to the new options.
	 * May be overrideen in subclasses.
	 * @param varName name of variable
	 * @param value value of variable
	 * @return map from project settings to sitting values if variable was converted
	 */
	public Map<Setting,Object> convertOldVariable(String varName, Object value)
	{
		return null;
	}

    /**
     * Method to clean libraries with unused primitive nodes.
     * May be overridden in technologies. By default it does nothing
     * @param ni NodeInst node to analyze
     * @param list nodes that will be removed in a remove job.
     * @return true if node is not in used
     */
    public boolean cleanUnusedNodesInLibrary(NodeInst ni, List<Geometric> list) {return false;}

    public void dump(PrintWriter out) {
        final String[] techBits = {
            "NONELECTRICAL", "NODIRECTIONALARCS", "NONEGATEDARCS",
            "NONSTANDARD", "STATICTECHNOLOGY", "NOPRIMTECHNOLOGY"
        };

        out.println("Technology " + getTechName());
        out.println(getClass().toString());
        out.println("shortName=" + getTechShortName());
        out.println("techDesc=" + getTechDesc());
        out.print("Bits: "); printlnBits(out, techBits, userBits);
        out.print("isScaleRelevant=" + isScaleRelevant()); printlnSetting(out, getScaleSetting());
        printlnSetting(out, getPrefFoundrySetting());
        printlnSetting(out, getNumMetalsSetting());
        dumpExtraProjectSettings(out);
        printlnSetting(out, getMinResistanceSetting());
        printlnSetting(out, getGateLengthSubtractionSetting());
        printlnSetting(out, getGateIncludedSetting());
        printlnSetting(out, getGroundNetIncludedSetting());
        printlnSetting(out, getMaxSeriesResistanceSetting());
        printlnSetting(out, getGateCapacitanceSetting());
        printlnSetting(out, getWireRatioSetting());
        printlnSetting(out, getDiffAlphaSetting());

        printlnPref(out, 0, prefResolution);
        assert getNumTransparentLayers() == (transparentColorPrefs != null ? transparentColorPrefs.length : 0);
        for (int i = 0; i < getNumTransparentLayers(); i++)
            out.println("TRANSPARENT_" + (i+1) + "=" + Integer.toHexString(transparentColorPrefs[i].getIntFactoryValue()));

        for (Layer layer: layers) {
            if (layer.isPseudoLayer()) continue;
            layer.dump(out);
        }
        for (ArcProto ap: arcs.values())
            ap.dump(out);
        if (!oldArcNames.isEmpty()) {
            out.println("OldArcNames:");
            for (Map.Entry<String,ArcProto> e: getOldArcNames().entrySet())
                out.println("\t" + e.getKey() + " --> " + e.getValue().getFullName());
        }
        for (PrimitiveNode pnp: nodes.values())
            pnp.dump(out);
        if (!oldNodeNames.isEmpty()) {
            out.println("OldNodeNames:");
            for (Map.Entry<String,PrimitiveNode> e: getOldNodeNames().entrySet())
                out.println("\t" + e.getKey() + " --> " + e.getValue().getFullName());
        }
        for (Foundry foundry: foundries) {
            out.println("Foundry " + foundry.getType());
            for (Layer layer: layers) {
                if (layer.isPseudoLayer()) continue;
                Setting setting = foundry.getGDSLayerSetting(layer);
                out.print("\t"); printlnSetting(out,setting);
            }
        }

        printSpiceHeader(out, 1, getSpiceHeaderLevel1());
        printSpiceHeader(out, 2, getSpiceHeaderLevel2());
        printSpiceHeader(out, 3, getSpiceHeaderLevel3());

        if (nodeGroups != null) {
            for (int i = 0; i < nodeGroups.length; i++) {
                Object[] nodeLine = nodeGroups[i];
                for (int j = 0; j < nodeLine.length; j++) {
                    Object entry = nodeLine[j];
                    if (entry == null) continue;
                    out.print(" menu " + i + " " + j);
                    if (entry instanceof List) {
                        List<?> list = (List<?>)entry;
                        for (Object o: list)
                            printMenuEntry(out, o);
                    } else {
                        printMenuEntry(out, entry);
                    }
                    out.println();
                }
            }
        }

        for (Iterator<Foundry> it = getFoundries(); it.hasNext();) {
            Foundry foundry = it.next();
            out.println("    <Foundry name=\"" + foundry.getType().getName() + "\">");
            for (Map.Entry<Layer,String> e: foundry.getGDSLayers().entrySet())
                out.println("        <layerGds layer=\"" + e.getKey().getName() + "\" gds=\"" + e.getValue() + "\"/>");
            List<DRCTemplate> rules = foundry.getRules();
            if (rules != null) {
                for (DRCTemplate rule: rules)
                    DRCTemplate.exportDRCRule(out, rule);
            }
            out.println("    </Foundry>");
        }
    }

    protected void dumpExtraProjectSettings(PrintWriter out) {}

    protected static void printlnSetting(PrintWriter out, Setting setting) {
        out.println(setting.getXmlPath() + "=" + setting.getValue() + "(" + setting.getFactoryValue() + ")");
    }

    static void printlnPref(PrintWriter out, int indent, Pref pref) {
        if (pref == null) return;
        while (indent-- > 0)
            out.print("\t");
        out.println(pref.getPrefName() + "=" + pref.getValue() + "(" + pref.getFactoryValue() + ")");
    }

    private static void printMenuEntry(PrintWriter out, Object entry) {
        if (entry instanceof ArcProto) {
            out.print(" arc " + ((ArcProto)entry).getName());
        } else if (entry instanceof PrimitiveNode) {
            out.print(" node " + ((PrimitiveNode)entry).getName());
        } else if (entry instanceof NodeInst) {
            NodeInst ni = (NodeInst)entry;
            PrimitiveNode pn = (PrimitiveNode)ni.getProto();
            out.print(" nodeInst " + pn.getName() + ":" + ni.getFunction() + ":" + ni.getOrient());
            for (Iterator<Variable> it = ni.getVariables(); it.hasNext(); ) {
                Variable var = it.next();
                out.print(":" + var.getObject()+ ":" + var.isDisplay() + ":" + var.getSize().getSize());
            }
        } else if (entry instanceof String) {
            out.print(" " + entry);
        } else {
            assert false;
        }
    }

    protected static void printlnBits(PrintWriter out, String[] bitNames, int bits) {
        for (int i = 0; i < Integer.SIZE; i++) {
            if ((bits & (1 << i)) == 0) continue;
            String bitName = i < bitNames.length ? bitNames[i] : null;
            if (bitName == null)
                bitName = "BIT" + i;
            out.print(" " + bitName);
        }
        out.println();
    }

    private void printSpiceHeader(PrintWriter out, int level, String[] header) {
        if (header == null) return;
        out.println("SpiceHeader " + level);
        for (String s: header)
            out.println("\t\"" + s + "\"");
    }

    /**
     * Create Xml structure of this Technology
     */
    public Xml.Technology makeXml() {
        Xml.Technology t = new Xml.Technology();
        t.techName = getTechName();
        if (getClass() != Technology.class)
            t.className = getClass().getName();

        Xml.Version version;
        version = new Xml.Version();
        version.techVersion = 1;
        version.electricVersion = Technology.DISK_VERSION_1;
        t.versions.add(version);
        version = new Xml.Version();
        version.techVersion = 2;
        version.electricVersion = Technology.DISK_VERSION_2;
        t.versions.add(version);

        t.shortTechName = getTechShortName();
        t.description = getTechDesc();
        int numMetals = ((Integer)getNumMetalsSetting().getFactoryValue()).intValue();
        t.minNumMetals = t.maxNumMetals = t.defaultNumMetals = numMetals;
        t.scaleValue = getScaleSetting().getDoubleFactoryValue();
        t.scaleRelevant = isScaleRelevant();
        t.defaultFoundry = (String)getPrefFoundrySetting().getFactoryValue();
        t.minResistance = getMinResistanceSetting().getDoubleFactoryValue();
        t.minCapacitance = getMinCapacitanceSetting().getDoubleFactoryValue();
        Color[] colorMap = getFactoryColorMap();
		for (int i = 0, numLayers = getNumTransparentLayers(); i < numLayers; i++) {
            Color transparentColor = colorMap[1 << i];
            t.transparentLayers.add(transparentColor);
        }
        for (Iterator<Layer> it = getLayers(); it.hasNext(); ) {
            Layer layer = it.next();
            if (layer.isPseudoLayer()) continue;
            t.layers.add(layer.makeXml());
        }
//        HashSet<PrimitiveNode> arcPins = new HashSet<PrimitiveNode>();
        for (Iterator<ArcProto> it = getArcs(); it.hasNext(); ) {
            ArcProto ap = it.next();
            t.arcs.add(ap.makeXml());
//            if (ap.arcPin != null)
//                arcPins.add(ap.arcPin);
        }
        for (Iterator<PrimitiveNode> it = getNodes(); it.hasNext(); ) {
            PrimitiveNode pnp = it.next();
            if (pnp.getFunction() == PrimitiveNode.Function.NODE) continue;
//            if (arcPins.contains(pnp)) continue;
            t.nodes.add(pnp.makeXml());
        }

        addSpiceHeader(t, 1, getSpiceHeaderLevel1());
        addSpiceHeader(t, 2, getSpiceHeaderLevel2());
        addSpiceHeader(t, 3, getSpiceHeaderLevel3());

        Object[][] origPalette = getNodesGrouped(null);
        int numRows = origPalette.length;
        int numCols = origPalette[0].length;
        for (Object[] row: origPalette) {
            assert row.length == numCols;
        }
        t.menuPalette = new Xml.MenuPalette();
        t.menuPalette.numColumns = numCols;
        for (int row = 0; row < numRows; row++) {
            for (int col = 0; col < numCols; col++) {
                Object origEntry = origPalette[row][col];
                ArrayList<Object> newBox = new ArrayList<Object>();
                if (origEntry instanceof List) {
                    List<?> list = (List<?>)origEntry;
                    for (Object o: list)
                        newBox.add(makeMenuEntry(t, o));
                } else if (origEntry != null) {
                    newBox.add(makeMenuEntry(t, origEntry));
                }
                t.menuPalette.menuBoxes.add(newBox);
            }
        }

        for (Iterator<Foundry> it = getFoundries(); it.hasNext(); ) {
            Foundry foundry = it.next();
            Xml.Foundry f = new Xml.Foundry();
            f.name = foundry.toString();
            Map<Layer,String> gdsMap = foundry.getGDSLayers();
            for (Map.Entry<Layer,String> e: gdsMap.entrySet()) {
                String gds = e.getValue();
                if (gds.length() == 0) continue;
                f.layerGds.put(e.getKey().getName(), gds);
            }
            List<DRCTemplate> rules = foundry.getRules();
            if (rules != null)
                f.rules.addAll(rules);
            t.foundries.add(f);
       }
        return t;
    }

    private static void addSpiceHeader(Xml.Technology t, int level, String[] spiceLines) {
        if (spiceLines == null) return;
        Xml.SpiceHeader spiceHeader = new Xml.SpiceHeader();
        spiceHeader.level = level;
        for (String spiceLine: spiceLines)
            spiceHeader.spiceLines.add(spiceLine);
        t.spiceHeaders.add(spiceHeader);
    }

    private static Object makeMenuEntry(Xml.Technology t, Object entry) {
        if (entry instanceof ArcProto)
            return t.findArc(((ArcProto)entry).getName());
        if (entry instanceof PrimitiveNode) {
            PrimitiveNode pn = (PrimitiveNode)entry;
            if (pn.getFunction() == PrimitiveNode.Function.PIN) {
                Xml.MenuNodeInst n = new Xml.MenuNodeInst();
                n.protoName = pn.getName();
                n.function = PrimitiveNode.Function.PIN;
                return n;
            }
            return t.findNode(((PrimitiveNode)entry).getName());
        }
        if (entry instanceof NodeInst) {
            NodeInst ni = (NodeInst)entry;
            Xml.MenuNodeInst n = new Xml.MenuNodeInst();
            n.protoName = ni.getProto().getName();
            n.function = ni.getFunction();
            n.rotation = ni.getOrient().getAngle();
            for (Iterator<Variable> it = ni.getVariables(); it.hasNext(); ) {
                Variable var = it.next();
                n.text = (String)var.getObject();
                n.fontSize = var.getSize().getSize();
            }
            return n;
        }
        assert entry instanceof String;
        return entry;
    }


    /**
     * Create Xml structure of this Technology
     */
    public XmlParam.Technology makeXmlParam() {
        XmlParam.Technology t = new XmlParam.Technology();
        t.techName = getTechName();
        if (getClass() != Technology.class)
            t.className = getClass().getName();
        t.shortTechName = getTechShortName();
        t.description = getTechDesc();
        int numMetals = ((Integer)getNumMetalsSetting().getFactoryValue()).intValue();
        t.minNumMetals = t.maxNumMetals = t.defaultNumMetals = numMetals;
        t.scaleValue = getScaleSetting().getDoubleFactoryValue();
        t.scaleRelevant = isScaleRelevant();
        t.defaultFoundry = (String)getPrefFoundrySetting().getFactoryValue();
        t.minResistance = getMinResistanceSetting().getDoubleFactoryValue();
        t.minCapacitance = getMinCapacitanceSetting().getDoubleFactoryValue();

        XmlParam.DisplayStyle displayStyle = new XmlParam.DisplayStyle();
        displayStyle.name = "Electric";
        t.displayStyles.add(displayStyle);
        Color[] colorMap = getFactoryColorMap();
		for (int i = 0, numLayers = getNumTransparentLayers(); i < numLayers; i++) {
            Color transparentColor = colorMap[1 << i];
            displayStyle.transparentLayers.add(transparentColor);
        }

        for (Iterator<Layer> it = getLayers(); it.hasNext(); ) {
            Layer layer = it.next();
            assert !layer.isPseudoLayer();
            layer.makeXmlParam(t, displayStyle);
        }
        HashSet<PrimitiveNode> arcPins = new HashSet<PrimitiveNode>();
        for (Iterator<ArcProto> it = getArcs(); it.hasNext(); ) {
            ArcProto ap = it.next();
            t.arcs.add(ap.makeXmlParam());
            if (ap.arcPin != null)
                arcPins.add(ap.arcPin);
        }
        for (Iterator<PrimitiveNode> it = getNodes(); it.hasNext(); ) {
            PrimitiveNode pnp = it.next();
            if (pnp.getFunction() == PrimitiveNode.Function.NODE) continue;
            if (arcPins.contains(pnp)) continue;
            t.nodes.add(pnp.makeXmlParam());
        }

        addSpiceHeader(t, 1, getSpiceHeaderLevel1());
        addSpiceHeader(t, 2, getSpiceHeaderLevel2());
        addSpiceHeader(t, 3, getSpiceHeaderLevel3());

        Object[][] origPalette = getNodesGrouped(null);
        int numRows = origPalette.length;
        int numCols = origPalette[0].length;
        for (Object[] row: origPalette) {
            assert row.length == numCols;
        }
        t.menuPalette = new XmlParam.MenuPalette();
        t.menuPalette.numColumns = numCols;
        for (int row = 0; row < numRows; row++) {
            for (int col = 0; col < numCols; col++) {
                Object origEntry = origPalette[row][col];
                ArrayList<Object> newBox = new ArrayList<Object>();
                if (origEntry instanceof List) {
                    List<?> list = (List<?>)origEntry;
                    for (Object o: list)
                        newBox.add(makeMenuEntry(t, o));
                } else if (origEntry != null) {
                    newBox.add(makeMenuEntry(t, origEntry));
                }
                t.menuPalette.menuBoxes.add(newBox);
            }
        }

        makeRuleSets(t);

        for (Iterator<Foundry> it = getFoundries(); it.hasNext(); ) {
            Foundry foundry = it.next();
            XmlParam.Foundry f = new XmlParam.Foundry();
            f.name = foundry.toString();
            Map<Layer,String> gdsMap = foundry.getGDSLayers();
            for (Map.Entry<Layer,String> e: gdsMap.entrySet()) {
                String gds = e.getValue();
                if (gds.length() == 0) continue;
                f.layerGds.put(e.getKey().getName(), gds);
            }
            List<DRCTemplate> rules = foundry.getRules();
            if (rules != null)
                f.rules.addAll(rules);
            t.foundries.add(f);
       }
        return t;
    }

    protected void makeRuleSets(XmlParam.Technology t) {
        XmlParam.RuleSet common = t.newRuleSet("common");
        make3d(t, common);
    }

    protected void make3d(XmlParam.Technology t, XmlParam.RuleSet ruleSet) {
        Map<XmlParam.Layer,XmlParam.Distance> thick3d = ruleSet.newLayerRule("thick3d");
        Map<XmlParam.Layer,XmlParam.Distance> height3d = ruleSet.newLayerRule("height3d");
        for (Iterator<Layer> it = getLayers(); it.hasNext(); ) {
            Layer layer = it.next();
            assert !layer.isPseudoLayer();
            layer.makeXmlParam(t, thick3d, height3d);
        }
    }

    private static void addSpiceHeader(XmlParam.Technology t, int level, String[] spiceLines) {
        if (spiceLines == null) return;
        XmlParam.SpiceHeader spiceHeader = new XmlParam.SpiceHeader();
        spiceHeader.level = level;
        for (String spiceLine: spiceLines)
            spiceHeader.spiceLines.add(spiceLine);
        t.spiceHeaders.add(spiceHeader);
    }

    private static Object makeMenuEntry(XmlParam.Technology t, Object entry) {
        if (entry instanceof ArcProto)
            return t.findArc(((ArcProto)entry).getName());
        if (entry instanceof PrimitiveNode) {
            PrimitiveNode pn = (PrimitiveNode)entry;
            if (pn.getFunction() == PrimitiveNode.Function.PIN) {
                XmlParam.MenuNodeInst n = new XmlParam.MenuNodeInst();
                n.protoName = pn.getName();
                n.function = PrimitiveNode.Function.PIN;
                return n;
            }
            return t.findNode(((PrimitiveNode)entry).getName());
        }
        if (entry instanceof NodeInst) {
            NodeInst ni = (NodeInst)entry;
            XmlParam.MenuNodeInst n = new XmlParam.MenuNodeInst();
            n.protoName = ni.getProto().getName();
            n.function = ni.getFunction();
            n.rotation = ni.getOrient().getAngle();
            for (Iterator<Variable> it = ni.getVariables(); it.hasNext(); ) {
                Variable var = it.next();
                n.text = (String)var.getObject();
                n.fontSize = var.getSize().getSize();
            }
            return n;
        }
        assert entry instanceof String;
        return entry;
    }

    /****************************** LAYERS ******************************/

	/**
	 * Returns an Iterator on the Layers in this Technology.
	 * @return an Iterator on the Layers in this Technology.
	 */
	public Iterator<Layer> getLayers()
	{
        layersAllocationLocked = true;
		return layers.iterator();
	}

	/**
	 * Returns a specific Layer number in this Technology.
	 * @param index the index of the desired Layer.
	 * @return the indexed Layer in this Technology.
	 */
	public Layer getLayer(int index)
	{
		return layers.get(index);
	}

	/**
	 * Returns the number of Layers in this Technology.
	 * @return the number of Layers in this Technology.
	 */
	public int getNumLayers()
	{
        layersAllocationLocked = true;
		return layers.size();
	}

	/**
	 * Method to find a Layer with a given name.
	 * @param layerName the name of the desired Layer.
	 * @return the Layer with that name (null if none found).
	 */
	public Layer findLayer(String layerName)
	{
        Layer layer = layersByName.get(layerName);
        if (layer != null) return layer;
		for(Iterator<Layer> it = getLayers(); it.hasNext(); )
		{
			layer = it.next();
			if (layer.getName().equalsIgnoreCase(layerName)) return layer;
		}
		for(Iterator<Layer> it = getLayers(); it.hasNext(); )
		{
			layer = it.next().getPseudoLayer();
            if (layer == null) continue;
			if (layer.getName().equalsIgnoreCase(layerName)) return layer;
		}
		return null;
	}

    /**
	 * Method to determine the index in the upper-left triangle array for two layers/nodes.
	 * @param index1 the first layer/node index.
	 * @param index2 the second layer/node index.
	 * @return the index in the array that corresponds to these two layers/nodes.
	 */
//	public int getRuleIndex(int index1, int index2)
//	{
//		if (index1 > index2) { int temp = index1; index1 = index2;  index2 = temp; }
//		int pIndex = (index1+1) * (index1/2) + (index1&1) * ((index1+1)/2);
//		pIndex = index2 + (getNumLayers()) * index1 - pIndex;
//		return getNumLayers() + getNumNodes() + pIndex;
//	}

    /**
     * Method to determine index of layer or node involved in the rule
     * @param name name of the layer or node
     * @return the index of the rule.
     */
    public int getRuleNodeIndex(String name)
    {
        // Checking if node is found
        // Be careful because iterator might change over time?
        int count = 0;
        for (Iterator<PrimitiveNode> it = getNodes(); it.hasNext(); count++)
        {
            PrimitiveNode pn = it.next();
            if (pn.getName().equalsIgnoreCase(name))
                return (getNumLayers() + count);   // it should use get
        }
        return -1;
    }

    public static Layer getLayerFromOverride(String override, int startPos, char endChr, Technology tech)
    {
        int endPos = override.indexOf(endChr, startPos);
        if (endPos < 0) return null;
        String layerName = override.substring(startPos, endPos);
        return tech.findLayer(layerName);
    }

	/**
	 * Method to find the Layer in this Technology that matches a function description.
	 * @param fun the layer function to locate.
	 * @param functionExtras
     * @return the Layer that matches this description (null if not found).
	 */
	public Layer findLayerFromFunction(Layer.Function fun, int functionExtras)
	{
		for(Iterator<Layer> it = this.getLayers(); it.hasNext(); )
		{
			Layer lay = it.next();
			Layer.Function lFun = lay.getFunction();
			if (lFun == fun)
            {
                // Nothing extra to look for or it really matches
                if (functionExtras == -1 || functionExtras == lay.getFunctionExtras())
                    return lay;
            }
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
        if (layersAllocationLocked)
            throw new IllegalStateException("layers allocation is locked");
        layer.setIndex(layers.size());
        layers.add(layer);
        layersByName.put(layer.getName(), layer);
	}

	/**
	 * Method to tell whether two layers should be considered equivalent for the purposes of cropping.
	 * The method is overridden by individual technologies to provide specific answers.
	 * @param layer1 the first Layer.
	 * @param layer2 the second Layer.
	 * @return true if the layers are equivalent.
	 */
	public boolean sameLayer(Layer layer1, Layer layer2)
	{
		if (layer1 == layer2) return true;
        // Only when the function and the extra bits match. Case of active and active well
        if (layer1.getFunction() == layer2.getFunction() &&
            layer1.getFunctionExtras() == layer2.getFunctionExtras()) return true;
        if (layer1.getFunction() == Layer.Function.POLY1 && layer2.getFunction() == Layer.Function.GATE) return true;
		if (layer2.getFunction() == Layer.Function.POLY1 && layer1.getFunction() == Layer.Function.GATE) return true;
		return false;
	}

	/**
	 * Method to make a sorted list of layers in this Technology.
	 * The list is sorted by depth (from bottom to top) stored in Function.height.
	 * @return a sorted list of Layers in this Technology.
	 */
	public List<Layer> getLayersSortedByHeight()
	{
		// determine order of overlappable layers in current technology
		List<Layer> layerList = new ArrayList<Layer>();
		for(Iterator<Layer> it = getLayers(); it.hasNext(); )
		{
			layerList.add(it.next());
		}
		Collections.sort(layerList, LAYERS_BY_HEIGHT);
		return(layerList);
	}

    /**
     * Method to make a sorted list of layers in this Technology
     * based on their name.
     * @return a sorted list of Layers in this Technology.
     */
    public List<Layer> getLayersSortedByName()
    {
        List<Layer> layerList = new ArrayList<Layer>();
        for(Iterator<Layer> it = getLayers(); it.hasNext(); )
        {
            layerList.add(it.next());
        }
        Collections.sort(layerList, Layer.layerSortByName);
        return(layerList);
    }

    /**
	 * Method to return a list of layers that are saved for this Technology.
	 * The saved layers are used in the "Layers" tab (which can be user-rearranged).
	 * @return a list of layers for this Technology in the saved order.
	 */
	public List<Layer> getSavedLayerOrder()
	{
		if (layerOrderPref == null)
			layerOrderPref = Pref.makeStringPref("LayerOrderfor"+getTechName(), prefs, "");
		String order = layerOrderPref.getString();
		if (order.length() == 0) return null;
		int pos = 0;
		List<Layer> layers = new ArrayList<Layer>();
		while (pos < order.length())
		{
			// get the next layer name in the string
			int end = order.indexOf(',', pos);
			if (end < 0) break;
			String layerName = order.substring(pos, end);
			pos = end + 1;

			// find the layer and add it to the list
			int colonPos = layerName.indexOf(':');
			Technology tech = this;
			if (colonPos >= 0)
			{
				String techName = layerName.substring(0, colonPos);
				tech = findTechnology(techName);
				if (tech == null) continue;
				layerName = layerName.substring(colonPos+1);
			}
			Layer layer = tech.findLayer(layerName);
			if (layer != null)
				layers.add(layer);
		}
		return layers;
	}

	/**
	 * Method to save a list of layers for this Technology in a preferred order.
	 * This ordering is managed by the "Layers" tab which users can rearrange.
	 * @param layers a list of layers for this Technology in a preferred order.
	 */
	public void setSavedLayerOrder(List<Layer> layers)
	{
		if (layerOrderPref == null)
			layerOrderPref = Pref.makeStringPref("LayerOrderfor"+getTechName(), prefs, "");
		StringBuffer sb = new StringBuffer();
		for(Layer lay : layers)
		{
			if (lay.getTechnology() != this) sb.append(lay.getTechnology().getTechName() + ":");
			sb.append(lay.getName() + ",");
		}
		layerOrderPref.setString(sb.toString());
	}

	public static final LayerHeight LAYERS_BY_HEIGHT = new LayerHeight(false);
    public static final LayerHeight LAYERS_BY_HEIGHT_LIFT_CONTACTS = new LayerHeight(true);

	private static class LayerHeight implements Comparator<Layer>
	{
        final boolean liftContacts;

        private LayerHeight(boolean liftContacts) {
            this.liftContacts = liftContacts;
        }

		public int compare(Layer l1, Layer l2)
		{
            Layer.Function f1 = l1.getFunction();
            Layer.Function f2 = l2.getFunction();
			int h1 = f1.getHeight();
			int h2 = f2.getHeight();
            if (liftContacts) {
                if (f1.isContact())
                    h1++;
                else if (f1.isMetal())
                    h1--;
                if (f2.isContact())
                    h2++;
                else if (f2.isMetal())
                    h2--;
            }
            int cmp = h1 - h2;
            if (cmp != 0) return cmp;
            Technology tech1 = l1.getTechnology();
            Technology tech2 = l2.getTechnology();
            if (tech1 != tech2) {
                int techIndex1 = tech1 != null ? tech1.getIndex() : -1;
                int techIndex2 = tech2 != null ? tech2.getIndex() : -1;
                return techIndex1 - techIndex2;
            }
			return l1.getIndex() - l2.getIndex();
		}
	}

    /**
     * Dummy method overridden by implementing technologies to define
     * the number of metal layers in the technology.  Applies to layout
     * technologies.  Can by changed by user preferences.
     * @return the number of metal layers currently specified for the technology
     */
    public int getNumMetals() { return cacheNumMetalLayers.getInt(); }
	/**
	 * Returns project Setting to tell the number of metal layers in the MoCMOS technology.
	 * @return project Setting to tell the number of metal layers in the MoCMOS technology (from 2 to 6).
	 */
	public Setting getNumMetalsSetting() { return cacheNumMetalLayers; }

	/****************************** ARCS ******************************/

	/**
	 * Method to create a new ArcProto from the parameters.
	 * @param protoName the name of this ArcProto.
	 * It may not have unprintable characters, spaces, or tabs in it.
     * @param lambdaWidthOffset width offset in lambda units.
	 * @param defaultWidth the default width of this ArcProto.
	 * @param layers the Layers that make up this ArcProto.
	 * @return the newly created ArcProto.
	 */
	protected ArcProto newArcProto(String protoName, double lambdaWidthOffset, double defaultWidth, ArcProto.Function function, Technology.ArcLayer... layers)
	{
		// check the arguments
		if (findArcProto(protoName) != null)
		{
			System.out.println("Error: technology " + getTechName() + " has multiple arcs named " + protoName);
			return null;
		}
        long gridWidthOffset = DBMath.lambdaToSizeGrid(lambdaWidthOffset);
		if (gridWidthOffset < 0 || gridWidthOffset > Integer.MAX_VALUE)
		{
			System.out.println("ArcProto " + getTechName() + ":" + protoName + " has invalid width offset " + lambdaWidthOffset);
			return null;
		}
		if (defaultWidth < DBMath.gridToLambda(gridWidthOffset))
		{
			System.out.println("ArcProto " + getTechName() + ":" + protoName + " has negative width");
			return null;
		}
        long defaultGridWidth = DBMath.lambdaToSizeGrid(defaultWidth);
        long gridFullExtend = defaultGridWidth/2;
		ArcProto ap = new ArcProto(this, protoName, gridFullExtend, (defaultGridWidth - gridWidthOffset)/2, function, layers, arcs.size());
		addArcProto(ap);
		return ap;
	}

	/**
	 * Returns the ArcProto in this technology with a particular name.
	 * @param name the name of the ArcProto.
	 * @return the ArcProto in this technology with that name.
	 */
	public ArcProto findArcProto(String name)
	{
		if (name == null) return null;
		ArcProto primArc = arcs.get(name);
		if (primArc != null) return primArc;

		for (Iterator<ArcProto> it = getArcs(); it.hasNext(); )
		{
			ArcProto ap = it.next();
			if (ap.getName().equalsIgnoreCase(name))
				return ap;
		}
		return null;
	}

	/**
	 * Returns the ArcProto in this technology with a particular Id
	 * @param arcProtoId the Id of the ArcProto.
	 * @return the ArcProto in this technology with that Id.
	 */
	public ArcProto getArcProto(ArcProtoId arcProtoId)
	{
        assert arcProtoId.techId == techId;
        int chronIndex = arcProtoId.chronIndex;
        return chronIndex < arcsByChronIndex.length ? arcsByChronIndex[chronIndex] : null;
	}

	/**
	 * Returns the ArcProto in this technology with a particular chron index
	 * @param chronIndex index the Id of the ArcProto.
	 * @return the ArcProto in this technology with that Id.
	 */
	ArcProto getArcProtoByChronIndex(int chronIndex)
	{
        return chronIndex < arcsByChronIndex.length ? arcsByChronIndex[chronIndex] : null;
	}

	/**
	 * Returns an Iterator on the ArcProto objects in this technology.
	 * @return an Iterator on the ArcProto objects in this technology.
	 */
	public Iterator<ArcProto> getArcs()
	{
		return arcs.values().iterator();
	}

    /**
     * Retusn a collection of the ArcProto objects in this technology
     * @return a collection of the ArcProto objects in this technology
     */
    public Collection<ArcProto> getArcsCollection() {return arcs.values();}

	/**
	 * Returns the number of ArcProto objects in this technology.
	 * @return the number of ArcProto objects in this technology.
	 */
	public int getNumArcs()
	{
		return arcs.size();
	}

    /**
	 * Method to add a new ArcProto to this Technology.
	 * This is usually done during initialization.
	 * @param ap the ArcProto to be added to this Technology.
	 */
	public void addArcProto(ArcProto ap)
	{
		assert findArcProto(ap.getName()) == null;
		assert ap.primArcIndex == arcs.size();
		arcs.put(ap.getName(), ap);
        ArcProtoId arcProtoId = ap.getId();
        if (arcProtoId.chronIndex >= arcsByChronIndex.length) {
            ArcProto[] newArcsByChronIndex = new ArcProto[arcProtoId.chronIndex + 1];
            System.arraycopy(arcsByChronIndex, 0, newArcsByChronIndex, 0, arcsByChronIndex.length);
            arcsByChronIndex = newArcsByChronIndex;
        }
        arcsByChronIndex[arcProtoId.chronIndex] = ap;
	}

	/**
	 * Sets the technology to have no directional arcs.
	 * Users should never call this method.
	 * It is set once by the technology during initialization.
	 * Directional arcs are those with arrows on them, indicating (only graphically) the direction of flow through the arc.
	 */
	protected void setNoDirectionalArcs() { userBits |= NODIRECTIONALARCS; }

	/**
	 * Returns true if this technology does not have directional arcs.
	 * @return true if this technology does not have directional arcs.
	 * Directional arcs are those with arrows on them, indicating (only graphically) the direction of flow through the arc.
	 */
	public boolean isNoDirectionalArcs() { return (userBits & NODIRECTIONALARCS) != 0; }

	/**
	 * Sets the technology to have no negated arcs.
	 * Users should never call this method.
	 * It is set once by the technology during initialization.
	 * Negated arcs have bubbles on them to graphically indicated negation.
	 * Only Schematics and related technologies allow negated arcs.
	 */
	protected void setNoNegatedArcs() { userBits |= NONEGATEDARCS; }

	/**
	 * Returns true if this technology does not have negated arcs.
	 * @return true if this technology does not have negated arcs.
	 * Negated arcs have bubbles on them to graphically indicated negation.
	 * Only Schematics and related technologies allow negated arcs.
	 */
	public boolean isNoNegatedArcs() { return (userBits & NONEGATEDARCS) != 0; }

	/**
	 * Returns the polygons that describe arc "ai".
	 * @param ai the ArcInst that is being described.
	 * @return an array of Poly objects that describes this ArcInst graphically.
	 */
	public Poly [] getShapeOfArc(ArcInst ai)
	{
		return getShapeOfArc(ai, null);
	}

	/**
	 * Returns the polygons that describe arc "ai".
	 * @param ai the ArcInst that is being described.
	 * @param onlyTheseLayers to filter the only required layers
	 * @return an array of Poly objects that describes this ArcInst graphically.
	 */
	public Poly [] getShapeOfArc(ArcInst ai, Layer.Function.Set onlyTheseLayers) {
        Poly.Builder polyBuilder = Poly.threadLocalLambdaBuilder();
        polyBuilder.setOnlyTheseLayers(onlyTheseLayers);
        return polyBuilder.getShapeArray(ai);
    }

    /**
     * Fill the polygons that describe arc "a".
     * @param b AbstractShapeBuilder to fill polygons.
     * @param a the ImmutableArcInst that is being described.
     */
    protected void getShapeOfArc(AbstractShapeBuilder b, ImmutableArcInst a) {
        getShapeOfArc(b, a, null);
    }

    /**
     * Fill the polygons that describe arc "a".
     * @param b AbstractShapeBuilder to fill polygons.
     * @param a the ImmutableArcInst that is being described.
     * @param layerOverride the layer to use for all generated polygons (if not null).
     */
    protected void getShapeOfArc(AbstractShapeBuilder b, ImmutableArcInst a, Layer layerOverride) {
        // get information about the arc
        assert a.protoId.techId == techId;
        ArcProto ap = getArcProto(a.protoId);
        assert ap.getTechnology() == this;
        int numArcLayers = ap.getNumArcLayers();

        // construct the polygons that describe the basic arc
        if (!isNoNegatedArcs() && (a.isHeadNegated() || a.isTailNegated())) {
            for (int i = 0; i < numArcLayers; i++) {
                Technology.ArcLayer primLayer = ap.getArcLayer(i);
                Layer layer = primLayer.getLayer();
                if (b.onlyTheseLayers != null && !b.onlyTheseLayers.contains(layer.getFunction(), layer.getFunctionExtras())) continue;
                if (layerOverride != null) layer = layerOverride;

                // remove a gap for the negating bubble
                int angle = a.getAngle();
                double gridBubbleSize = Schematics.getNegatingBubbleSize()*DBMath.GRID;
                double cosDist = DBMath.cos(angle) * gridBubbleSize;
                double sinDist = DBMath.sin(angle) * gridBubbleSize;
                if (a.isTailNegated())
                    b.pushPoint(a.tailLocation, cosDist, sinDist);
                else
                    b.pushPoint(a.tailLocation);
                if (a.isHeadNegated())
                    b.pushPoint(a.headLocation, -cosDist, -sinDist);
                else
                    b.pushPoint(a.headLocation);
                b.pushPoly(Poly.Type.OPENED, layer);
            }
        } else {
            for (int i = 0; i < numArcLayers; i++) {
                Technology.ArcLayer primLayer = ap.getArcLayer(i);
                Layer layer = primLayer.getLayer();
                if (b.onlyTheseLayers != null && !b.onlyTheseLayers.contains(layer.getFunction(), layer.getFunctionExtras())) continue;
                if (layerOverride != null) layer = layerOverride;
                b.makeGridPoly(a, 2*(a.getGridExtendOverMin() + ap.getLayerGridExtend(i)), primLayer.getStyle(), layer);
            }
        }

        // add an arrow to the arc description
        if (!isNoDirectionalArcs()) {
            final double lambdaArrowSize = 1.0*DBMath.GRID;
            int angle = a.getAngle();
            if (a.isBodyArrowed()) {
                b.pushPoint(a.headLocation);
                b.pushPoint(a.tailLocation);
                b.pushPoly(Poly.Type.VECTORS, generic.glyphLay);
            }
            if (a.isTailArrowed()) {
                int angleOfArrow = 3300;		// -30 degrees
                int backAngle1 = angle - angleOfArrow;
                int backAngle2 = angle + angleOfArrow;
                b.pushPoint(a.tailLocation);
                b.pushPoint(a.tailLocation, DBMath.cos(backAngle1)*lambdaArrowSize, DBMath.sin(backAngle1)*lambdaArrowSize);
                b.pushPoint(a.tailLocation);
                b.pushPoint(a.tailLocation, DBMath.cos(backAngle2)*lambdaArrowSize, DBMath.sin(backAngle2)*lambdaArrowSize);
                b.pushPoly(Poly.Type.VECTORS, generic.glyphLay);
            }
            if (a.isHeadArrowed()) {
                angle = (angle + 1800) % 3600;
                int angleOfArrow = 300;		// 30 degrees
                int backAngle1 = angle - angleOfArrow;
                int backAngle2 = angle + angleOfArrow;
                b.pushPoint(a.headLocation);
                b.pushPoint(a.headLocation, DBMath.cos(backAngle1)*lambdaArrowSize, DBMath.sin(backAngle1)*lambdaArrowSize);
                b.pushPoint(a.headLocation);
                b.pushPoint(a.headLocation, DBMath.cos(backAngle2)*lambdaArrowSize, DBMath.sin(backAngle2)*lambdaArrowSize);
                b.pushPoly(Poly.Type.VECTORS, Generic.tech().glyphLay);
            }
        }
    }

    /**
     * Tells if arc can be drawn by simplified algorithm
     * Overidden ins subclasses
     * @param a arc to test
     * @param explain if true then print explanation why arc is not easy
     * @return true if arc can be drawn by simplified algorithm
     */
    public boolean isEasyShape(ImmutableArcInst a, boolean explain) {
        if (a.isBodyArrowed() || a.isTailArrowed() || a.isHeadArrowed()) {
            if (explain) System.out.println("ARROWED");
            return false;
        }
        if (a.isTailNegated() || a.isHeadNegated()) {
            if (explain) System.out.println("NEGATED");
            return false;
        }
        ArcProto protoType = getArcProto(a.protoId);
        if (protoType.isCurvable() && a.getVar(ImmutableArcInst.ARC_RADIUS) != null) {
            if (explain) System.out.println("CURVABLE");
            return false;
        }
        if (!(a.tailLocation.isSmall() && a.headLocation.isSmall())) {
            if (explain) System.out.println("LARGE " + a.tailLocation + " " + a.headLocation);
            return false;
        }
        int minLayerExtend = (int)a.getGridExtendOverMin() + protoType.getMinLayerGridExtend();
        if (minLayerExtend <= 0) {
            if (minLayerExtend != 0 || protoType.getNumArcLayers() != 1) {
                if (explain) System.out.println(protoType + " many zero-width layers");
                return false;
            }
            return true;
        }
        for (int i = 0, numArcLayers = protoType.getNumArcLayers(); i < numArcLayers; i++) {
            if (protoType.getLayerStyle(i) != Poly.Type.FILLED) {
                if (explain) System.out.println("Wide should be filled");
                return false;
            }
        }
        if (!a.isManhattan()) {
            if (explain) System.out.println("NON-MANHATTAN");
            return false;
        }
        return true;
    }

	/**
	 * Method to convert old primitive arc names to their proper ArcProtos.
	 * @param name the unknown arc name, read from an old Library.
	 * @return the proper ArcProto to use for this name.
	 */
	public ArcProto convertOldArcName(String name) {
        return oldArcNames.get(name);
    }

    public Map<String,ArcProto> getOldArcNames() { return new TreeMap<String,ArcProto>(oldArcNames); }

	/****************************** NODES ******************************/

    /**
	 * Method to return a sorted list of nodes in the technology
	 * @return a list with all nodes sorted
	 */
	public List<PrimitiveNode> getNodesSortedByName()
	{
		TreeMap<String,PrimitiveNode> sortedMap = new TreeMap<String,PrimitiveNode>(TextUtils.STRING_NUMBER_ORDER);
		for(Iterator<PrimitiveNode> it = getNodes(); it.hasNext(); )
		{
			PrimitiveNode pn = it.next();
			sortedMap.put(pn.getName(), pn);
		}
		return new ArrayList<PrimitiveNode>(sortedMap.values());
	}

	/**
	 * Returns the PrimitiveNode in this technology with a particular name.
	 * @param name the name of the PrimitiveNode.
	 * @return the PrimitiveNode in this technology with that name.
	 */
	public PrimitiveNode findNodeProto(String name)
	{
		if (name == null) return null;
		PrimitiveNode primNode = nodes.get(name);
		if (primNode != null) return primNode;

		for (Iterator<PrimitiveNode> it = getNodes(); it.hasNext(); )
		{
			PrimitiveNode pn = it.next();
			if (pn.getName().equalsIgnoreCase(name))
				return pn;
		}
		return null;
	}

	/**
	 * Returns the PrimitiveNode in this technology with a particular Id
	 * @param primitiveNodeId the Id of the PrimitiveNode.
	 * @return the PrimitiveNiode in this technology with that Id.
	 */
	public PrimitiveNode getPrimitiveNode(PrimitiveNodeId primitiveNodeId)
	{
        assert primitiveNodeId.techId == techId;
        int chronIndex = primitiveNodeId.chronIndex;
        return chronIndex < nodesByChronIndex.length ? nodesByChronIndex[chronIndex] : null;
	}

	/**
	 * Returns the PrimitiveNode in this technology with a particular chron index
	 * @param chronIndex index the Id of the PrimitiveNode.
	 * @return the PrimitiveNode in this technology with that Id.
	 */
	PrimitiveNode getPrimitiveNodeByChronIndex(int chronIndex)
	{
        return chronIndex < nodesByChronIndex.length ? nodesByChronIndex[chronIndex] : null;
	}

	/**
	 * Returns an Iterator on the PrimitiveNode objects in this technology.
	 * @return an Iterator on the PrimitiveNode objects in this technology.
	 */
	public Iterator<PrimitiveNode> getNodes()
	{
		return nodes.values().iterator();
	}


    /**
     * Retusn a collection of the PrimitiveNode objects in this technology
     * @return a collection of the PrimitiveNode objects in this technology
     */
    public Collection<PrimitiveNode> getNodesCollection() {return nodes.values();}


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
		assert findNodeProto(np.getName()) == null;
        np.setPrimNodeIndexInTech(nodeIndex++);
		nodes.put(np.getName(), np);
        PrimitiveNodeId primitiveNodeId = np.getId();
        if (primitiveNodeId.chronIndex >= nodesByChronIndex.length) {
            PrimitiveNode[] newNodesByChronIndex = new PrimitiveNode[primitiveNodeId.chronIndex + 1];
            System.arraycopy(nodesByChronIndex, 0, newNodesByChronIndex, 0, nodesByChronIndex.length);
            nodesByChronIndex = newNodesByChronIndex;
        }
        nodesByChronIndex[primitiveNodeId.chronIndex] = np;
	}

	/**
	 * Method to return the pure "NodeProto Function" a PrimitiveNode in this Technology.
	 * This method is overridden by technologies (such as Schematics) that know the node's function.
	 * @param pn PrimitiveNode to check.
     * @param techBits tech bits
	 * @return the PrimitiveNode.Function that describes the PrinitiveNode with specific tech bits.
	 */
	public PrimitiveNode.Function getPrimitiveFunction(PrimitiveNode pn, int techBits) { return pn.getFunction(); }

    private static final Layer.Function.Set diffLayers = new Layer.Function.Set(Layer.Function.DIFFP, Layer.Function.DIFFN);

    /**
	 * Method to return the size of a resistor-type NodeInst in this Technology.
	 * @param ni the NodeInst.
     * @param context the VarContext in which any vars will be evaluated,
     * pass in VarContext.globalContext if no context needed, or set to null
     * to avoid evaluation of variables (if any).
	 * @return the size of the NodeInst.
	 */
    public PrimitiveNodeSize getResistorSize(NodeInst ni, VarContext context)
    {
        if (ni.isCellInstance()) return null;
        double length = ni.getLambdaBaseXSize();
        double width = ni.getLambdaBaseYSize();
//        SizeOffset so = ni.getSizeOffset();
//        double length = ni.getXSize() - so.getLowXOffset() - so.getHighXOffset();
//        double width = ni.getYSize() - so.getLowYOffset() - so.getHighYOffset();

        PrimitiveNodeSize size = new PrimitiveNodeSize(new Double(width), new Double(length), false);
        return size;
    }

    /**
     * Method to return length of active reqion. This will be used for
     * parasitics extraction. Electric layers are used for the calculation
     * @param ni the NodeInst.
     * @return length of the any active region
     */
    public double getTransistorActiveLength(NodeInst ni)
    {
        Poly [] diffList = getShapeOfNode(ni, true, false, diffLayers);
        double activeLen = 0;
        if (diffList.length > 0)
        {
            // Since electric layers are used, it takes the first active region
            Poly poly = diffList[0];
            activeLen = poly.getBounds2D().getHeight();
        }
        return activeLen;
    }

	/**
	 * Method to return the size of a transistor NodeInst in this Technology.
     * You should most likely be calling NodeInst.getTransistorSize instead of this.
	 * @param ni the NodeInst.
     * @param context the VarContext in which any vars will be evaluated,
     * pass in VarContext.globalContext if no context needed, or set to null
     * to avoid evaluation of variables (if any).
	 * @return the size of the NodeInst.
	 */
	public TransistorSize getTransistorSize(NodeInst ni, VarContext context)
	{
		double width = ni.getLambdaBaseXSize();
		double height = ni.getLambdaBaseYSize();

		// override if there is serpentine information
		Point2D [] trace = ni.getTrace();
		if (trace != null)
		{
			width = 0;
			for(int i=1; i<trace.length; i++)
				width += trace[i-1].distance(trace[i]);
			height = 2;
			double serpentineLength = ni.getSerpentineTransistorLength();
			if (serpentineLength > 0) height = serpentineLength;
            //System.out.println("No calculating length for active regions yet"); 
		}
        double activeLen = getTransistorActiveLength(ni);
		TransistorSize size = new TransistorSize(new Double(width), new Double(height), new Double(activeLen), true);
		return size;
	}

    /**
     * Method to set the size of a transistor NodeInst in this Technology.
     * You should be calling NodeInst.setTransistorSize instead of this.
     * @param ni the NodeInst
     * @param width the new width (positive values only)
     * @param length the new length (positive values only)
     */
    public void setPrimitiveNodeSize(NodeInst ni, double width, double length)
    {
        double oldWidth = ni.getLambdaBaseXSize();
        double oldLength = ni.getLambdaBaseYSize();
//        SizeOffset so = ni.getSizeOffset();
//        double oldWidth = ni.getXSize() - so.getLowXOffset() - so.getHighXOffset();
//        double oldLength = ni.getYSize() - so.getLowYOffset() - so.getHighYOffset();
        double dW = width - oldWidth;
        double dL = length - oldLength;
		ni.resize(dW, dL);
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
     * Method to return the other gate PortInst for this transistor NodeInst.
     * Only useful for layout transistors that have two gate ports.
     * Implementation Note: May want to make this a more general
     * method, getPrimitivePort(PortType), if the number of port
     * types increases.  Note: You should be calling
     * NodeInst.getTransistorGatePort() instead of this, most likely.
     * @param ni the NodeInst
     * @return a PortInst for the alternate gate of the transistor
     */
	public PortInst getTransistorAltGatePort(NodeInst ni)
	{
		if (ni.getProto().getTechnology() == Schematics.tech()) return ni.getPortInst(0);
		return ni.getPortInst(2);
	}

	/**
     * Method to return a base PortInst for this transistor NodeInst.
     * @param ni the NodeInst
     * @return a PortInst for the base of the transistor
     */
	public PortInst getTransistorBasePort(NodeInst ni) { return ni.getPortInst(0); }

    /**
     * Method to return a source PortInst for this transistor NodeInst.
     * Implementation Note: May want to make this a more general
     * method, getPrimitivePort(PortType), if the number of port
     * types increases.  Note: You should be calling
     * NodeInst.getTransistorSourcePort() instead of this, most likely.
     * @param ni the NodeInst
     * @return a PortInst for the source of the transistor
     */
	public PortInst getTransistorSourcePort(NodeInst ni) { return ni.getPortInst(1); }
    /**
     * Method to return a emitter PortInst for this transistor NodeInst.
     * @param ni the NodeInst
     * @return a PortInst for the emitter of the transistor
     */
	public PortInst getTransistorEmitterPort(NodeInst ni) { return ni.getPortInst(1); }

    /**
     * Method to return a drain PortInst for this transistor NodeInst.
     * Implementation Note: May want to make this a more general
     * method, getPrimitivePort(PortType), if the number of port
     * types increases.  Note: You should be calling
     * NodeInst.getTransistorDrainPort() instead of this, most likely.
     * @param ni the NodeInst
     * @return a PortInst for the drain of the transistor
     */
	public PortInst getTransistorDrainPort(NodeInst ni)
	{
		if (ni.getProto().getTechnology() == Schematics.tech()) return ni.getPortInst(2);
		return ni.getPortInst(3);
	}
    /**
     * Method to return a collector PortInst for this transistor NodeInst.
     * @param ni the NodeInst
     * @return a PortInst for the collector of the transistor
     */
	public PortInst getTransistorCollectorPort(NodeInst ni) { return ni.getPortInst(2); }

    /**
     * Method to return a bias PortInst for this transistor NodeInst.
     * Implementation Note: May want to make this a more general
     * method, getPrimitivePort(PortType), if the number of port
     * types increases.  Note: You should be calling
     * NodeInst.getTransistorBiasPort() instead of this, most likely.
     * @param ni the NodeInst
     * @return a PortInst for the bias of the transistor
     */
	public PortInst getTransistorBiasPort(NodeInst ni)
	{
		if (ni.getNumPortInsts() < 4) return null;
		if (ni.getProto().getTechnology() != Schematics.tech()) return null;
		return ni.getPortInst(3);
	}

    /**
	 * Method to set the pure "NodeProto Function" for a primitive NodeInst in this Technology.
	 * This method is overridden by technologies (such as Schematics) that can change a node's function.
	 * @param ni the NodeInst to check.
	 * @param function the PrimitiveNode.Function to set on the NodeInst.
	 */
	public void setPrimitiveFunction(NodeInst ni, PrimitiveNode.Function function) {}

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

	/**
	 * Method to get the base (highlight) ERectangle associated with a NodeInst
     * in this PrimitiveNode.
     * Base ERectangle is a highlight rectangle of standard-size NodeInst of
     * this PrimtiveNode
	 * By having this be a method of Technology, it can be overridden by
	 * individual Technologies that need to make special considerations.
	 * @param ni the NodeInst to query.
	 * @return the base ERectangle of this PrimitiveNode.
	 */
    public ERectangle getNodeInstBaseRectangle(NodeInst ni) {
		PrimitiveNode pn = (PrimitiveNode)ni.getProto();
		return pn.getBaseRectangle();
    }

	private static final Technology.NodeLayer [] nullPrimLayers = new Technology.NodeLayer [0];

	/**
	 * Returns the polygons that describe node "ni".
	 * @param ni the NodeInst that is being described.
	 * The prototype of this NodeInst must be a PrimitiveNode and not a Cell.
	 * @return an array of Poly objects that describes this NodeInst graphically.
	 */
	public Poly [] getShapeOfNode(NodeInst ni)
	{
		return getShapeOfNode(ni, false, false, null);
	}

	/**
	 * Returns the polygons that describe node "ni".
	 * @param ni the NodeInst that is being described.
	 * The prototype of this NodeInst must be a PrimitiveNode and not a Cell.
	 * @param electrical true to get the "electrical" layers.
	 * When electrical layers are requested, each layer is tied to a specific port on the node.
	 * If any piece of geometry covers more than one port,
	 * it must be split for the purposes of an "electrical" description.
	 * For example, the MOS transistor has 2 layers: Active and Poly.
	 * But it has 3 electrical layers: Active, Active, and Poly.
	 * The active must be split since each half corresponds to a different PrimitivePort on the PrimitiveNode.
	 * @param reasonable true to get only a minimal set of contact cuts in large contacts.
	 * The minimal set covers all edge contacts, but ignores the inner cuts in large contacts.
	 * @param onlyTheseLayers a set of layers to draw (if null, draw all layers).
	 * @return an array of Poly objects that describes this NodeInst graphically.
	 */
	public Poly [] getShapeOfNode(NodeInst ni, boolean electrical, boolean reasonable, Layer.Function.Set onlyTheseLayers)
	{
		if (ni.isCellInstance()) return null;

		PrimitiveNode np = (PrimitiveNode)ni.getProto();
		NodeLayer [] primLayers = np.getLayers();
		if (electrical)
		{
			NodeLayer [] eLayers = np.getElectricalLayers();
			if (eLayers != null) primLayers = eLayers;
		}

		if (onlyTheseLayers != null)
		{
			List<NodeLayer> layerArray = new ArrayList<NodeLayer>();

			for (int i = 0; i < primLayers.length; i++)
			{
				NodeLayer primLayer = primLayers[i];
				if (onlyTheseLayers.contains(primLayer.layer.getFunction(), primLayer.layer.getFunctionExtras()))
					layerArray.add(primLayer);
			}
			primLayers = new NodeLayer [layerArray.size()];
			layerArray.toArray(primLayers);
		}
		if (primLayers.length == 0)
			return new Poly[0];

		return getShapeOfNode(ni, electrical, reasonable, primLayers, null);
	}

	/**
	 * Returns the polygons that describe node "ni", given a set of
	 * NodeLayer objects to use.
	 * This method is overridden by specific Technologys.
	 * @param ni the NodeInst that is being described.
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
	 * @param layerOverride the layer to use for all generated polygons (if not null).
	 * The prototype of this NodeInst must be a PrimitiveNode and not a Cell.
	 * @return an array of Poly objects that describes this NodeInst graphically.
	 * This array includes displayable variables on the NodeInst.
	 */
	protected Poly [] getShapeOfNode(NodeInst ni, boolean electrical, boolean reasonable,
		Technology.NodeLayer [] primLayers, Layer layerOverride)
	{
		// if node is erased, remove layers
		if (!electrical)
		{
			if (ni.isWiped()) primLayers = nullPrimLayers; else
			{
				PrimitiveNode np = (PrimitiveNode)ni.getProto();
				if (np.isWipeOn1or2())
				{
					if (ni.pinUseCount()) primLayers = nullPrimLayers;
				}
			}
		}

		return computeShapeOfNode(ni, electrical, reasonable, primLayers, layerOverride);
	}

	/**
	 * Returns the polygons that describe node "ni", given a set of
	 * NodeLayer objects to use.
	 * This method is called by the specific Technology overrides of getShapeOfNode().
	 * @param ni the NodeInst that is being described.
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
	 * @param layerOverride the layer to use for all generated polygons (if not null).
	 * The prototype of this NodeInst must be a PrimitiveNode and not a Cell.
	 * @return an array of Poly objects that describes this NodeInst graphically.
	 */
	protected Poly [] computeShapeOfNode(NodeInst ni, boolean electrical, boolean reasonable, Technology.NodeLayer [] primLayers, Layer layerOverride)
	{
		PrimitiveNode np = (PrimitiveNode)ni.getProto();
        ERectangle fullRectangle = np.getFullRectangle();
		int specialType = np.getSpecialType();
		if (specialType != PrimitiveNode.SERPTRANS && np.isHoldsOutline())
		{
			Point2D [] outline = ni.getTrace();
			if (outline != null)
			{
				if (HANDLEBROKENOUTLINES)
				{
					List<Poly> polyList = new ArrayList<Poly>();
					int startPoint = 0;
					for(int i=1; i<outline.length; i++)
					{
						if (i == outline.length-1 || outline[i] == null ||
							(i-startPoint > 0 && outline[i].getX() == outline[i-1].getX() && outline[i].getY() == outline[i-1].getY()))
						{
							if (i == outline.length-1) i++;
							Point2D [] pointList = new Point2D.Double[i-startPoint];
							for(int j=startPoint; j<i; j++)
							{
								pointList[j-startPoint] = new Point2D.Double(ni.getAnchorCenterX() + outline[j].getX(),
									ni.getAnchorCenterY() + outline[j].getY());
							}
							Poly poly = new Poly(pointList);
							Technology.NodeLayer primLayer = primLayers[0];
							poly.setStyle(primLayer.getStyle());
							if (layerOverride != null) poly.setLayer(layerOverride); else
								poly.setLayer(primLayer.getLayer());
							if (electrical)
							{
								int portIndex = primLayer.getPortNum();
			                    assert(portIndex < np.getNumPorts()); // wrong number of ports. Probably missing during the definition
			                    if (portIndex >= 0) poly.setPort(np.getPort(portIndex));
							}
							polyList.add(poly);
							startPoint = i+1;
						}
					}
					Poly [] polys = new Poly[polyList.size()];
					for(int i=0; i<polyList.size(); i++) polys[i] = polyList.get(i);
					return polys;
				} else
				{
					Poly [] polys = new Poly[1];
					Point2D [] pointList = new Point2D.Double[outline.length];
					for(int i=0; i<outline.length; i++)
					{
						pointList[i] = new Point2D.Double(ni.getAnchorCenterX() + outline[i].getX(),
							ni.getAnchorCenterY() + outline[i].getY());
					}
					polys[0] = new Poly(pointList);
					Technology.NodeLayer primLayer = primLayers[0];
					polys[0].setStyle(primLayer.getStyle());
					if (layerOverride != null) polys[0].setLayer(layerOverride); else
						polys[0].setLayer(primLayer.getLayer());
					if (electrical)
					{
						int portIndex = primLayer.getPortNum();
	                    assert(portIndex < np.getNumPorts()); // wrong number of ports. Probably missing during the definition
	                    if (portIndex >= 0) polys[0].setPort(np.getPort(portIndex));
					}
					return polys;
				}
			}
		}

		// determine the number of polygons (considering that it may be "wiped")
		int numBasicLayers = primLayers.length;

		// if a MultiCut contact, determine the number of extra cuts
		int numExtraLayers = 0;
		MultiCutData mcd = null;
		SerpentineTrans std = null;
		if (np.hasMultiCuts())
		{
            for (NodeLayer nodeLayer: primLayers) {
                if (nodeLayer.representation == NodeLayer.MULTICUTBOX) {
                    mcd = new MultiCutData(ni.getD().size, fullRectangle, nodeLayer);
                    if (reasonable) numExtraLayers += (mcd.cutsReasonable - 1); else
                        numExtraLayers += (mcd.cutsTotal - 1);
                }
            }
//			mcd = new MultiCutData(ni.getD());
//			if (reasonable) numExtraLayers = mcd.cutsReasonable; else
//			numExtraLayers = mcd.cutsTotal;
//			numBasicLayers--;
		} else if (specialType == PrimitiveNode.SERPTRANS)
		{
			std = new SerpentineTrans(ni.getD(), np, primLayers);
			if (std.layersTotal > 0)
			{
				numExtraLayers = std.layersTotal;
				numBasicLayers = 0;
			}
		}

		// determine the number of negating bubbles
		int numNegatingBubbles = 0;
		for(Iterator<Connection> it = ni.getConnections(); it.hasNext(); )
		{
			Connection con = it.next();
			if (con.isNegated()) numNegatingBubbles++;
		}

		// construct the polygon array
		int numPolys = numBasicLayers + numExtraLayers + numNegatingBubbles;
		Poly [] polys = new Poly[numPolys];

        double xCenter = ni.getAnchorCenterX();
        double yCenter = ni.getAnchorCenterY();
// 			double xCenter = ni.getTrueCenterX();
// 			double yCenter = ni.getTrueCenterY();
        double xSize = ni.getXSize();
        double ySize = ni.getYSize();

		// add in the basic polygons
		int fillPoly = 0;
		for(int i = 0; i < numBasicLayers; i++)
		{
			Technology.NodeLayer primLayer = primLayers[i];
			int representation = primLayer.getRepresentation();
			if (representation == Technology.NodeLayer.BOX)
			{
				EdgeH leftEdge = primLayer.getLeftEdge();
				EdgeH rightEdge = primLayer.getRightEdge();
				EdgeV topEdge = primLayer.getTopEdge();
				EdgeV bottomEdge = primLayer.getBottomEdge();
				double portLowX = xCenter + leftEdge.getMultiplier() * xSize + leftEdge.getAdder();
				double portHighX = xCenter + rightEdge.getMultiplier() * xSize + rightEdge.getAdder();
				double portLowY = yCenter + bottomEdge.getMultiplier() * ySize + bottomEdge.getAdder();
				double portHighY = yCenter + topEdge.getMultiplier() * ySize + topEdge.getAdder();
				Point2D [] pointList = Poly.makePoints(portLowX, portHighX, portLowY, portHighY);
				polys[fillPoly] = new Poly(pointList);
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
						x = xCenter + xFactor.getMultiplier() * xSize + xFactor.getAdder();
						y = yCenter + yFactor.getMultiplier() * ySize + yFactor.getAdder();
					}
					pointList[j] = new Point2D.Double(x, y);
				}
				polys[fillPoly] = new Poly(pointList);
			} else if (representation == Technology.NodeLayer.MULTICUTBOX) {
                mcd = new MultiCutData(ni.getD().size, fullRectangle, primLayer);
                Poly.Type style = primLayer.getStyle();
                PortProto port = null;
                if (electrical) port = np.getPort(0);
                if (reasonable) numExtraLayers = mcd.cutsReasonable; else
                    numExtraLayers = mcd.cutsTotal;
                for(int j = 0; j < numExtraLayers; j++) {
                    polys[fillPoly] = mcd.fillCutPoly(ni.getD(), j);
                    polys[fillPoly].setStyle(style);
                    polys[fillPoly].setLayer(primLayer.getLayer());
                    polys[fillPoly].setPort(port);
                    fillPoly++;
                }
                continue;
            }

			Poly.Type style = primLayer.getStyle();
			if (style.isText())
			{
				polys[fillPoly].setString(primLayer.getMessage());
				polys[fillPoly].setTextDescriptor(primLayer.getDescriptor());
			}
			polys[fillPoly].setStyle(style);
			if (layerOverride != null) polys[fillPoly].setLayer(layerOverride); else
				polys[fillPoly].setLayer(primLayer.getLayerOrPseudoLayer());
			if (electrical)
			{
				int portIndex = primLayer.getPortNum();
                assert(portIndex < np.getNumPorts()); // wrong number of ports. Probably missing during the definition
                if (portIndex >= 0) polys[fillPoly].setPort(np.getPort(portIndex));
			}
			fillPoly++;
		}

		// add in negating bubbles
		if (numNegatingBubbles > 0)
		{
			double bubbleRadius = Schematics.getNegatingBubbleSize() / 2;
			for(Iterator<Connection> it = ni.getConnections(); it.hasNext(); )
			{
				Connection con = it.next();
				if (!con.isNegated()) continue;

				// add a negating bubble
				AffineTransform trans = ni.rotateIn();
				Point2D portLocation = new Point2D.Double(con.getLocation().getX(), con.getLocation().getY());
				trans.transform(portLocation, portLocation);
				double x = portLocation.getX();
				double y = portLocation.getY();
				PrimitivePort pp = (PrimitivePort)con.getPortInst().getPortProto();
				int angle = pp.getAngle() * 10;
				double dX = DBMath.cos(angle) * bubbleRadius;
				double dY = DBMath.sin(angle) * bubbleRadius;
				Point2D [] points = new Point2D[2];
				points[0] = new Point2D.Double(x+dX, y+dY);
				points[1] = new Point2D.Double(x, y);
				polys[fillPoly] = new Poly(points);
				polys[fillPoly].setStyle(Poly.Type.CIRCLE);
				polys[fillPoly].setLayer(Schematics.tech().node_lay);
				fillPoly++;
			}
		}

		// add in the extra transistor layers
		if (std != null)
		{
			std.initTransPolyFilling();
			for(int i = 0; i < numExtraLayers; i++)
			{
				polys[fillPoly] = std.fillTransPoly(electrical);
				fillPoly++;
			}
		}
        assert fillPoly == polys.length;
		return polys;
	}

	/**
	 * Method to determine if cut case is considered multi cut
	 * It gets overridden by CMOS90
	 */
	public boolean isMultiCutInTechnology(MultiCutData mcd)
	{
        if (mcd == null) return false;
        return (mcd.numCuts() > 1);
	}

    /**
     * Method to get a multi-cut structure associated to
     * a NodeInst representing a Multiple-cut contact.
     * @param ni the NodeInst being tested.
     * @return a non-null MultiCutData pointer if it is a Multiple-cut contact
     */
    public MultiCutData getMultiCutData(NodeInst ni)
    {
        if (ni.isCellInstance()) return null;
		PrimitiveNode pnp = (PrimitiveNode)ni.getProto();
		if (!pnp.isMulticut()) return null;

		return ((new MultiCutData(ni.getD(), ni.getTechPool())));
    }

    /**
	 * Method to decide whether a NodeInst is a multi-cut contact.
	 * The function is done by the Technologies so that it can be subclassed.
	 * @param ni the NodeInst being tested.
	 * @return true if it is a Multiple-cut contact.
	 */
	public boolean isMultiCutCase(NodeInst ni)
	{
        MultiCutData data = getMultiCutData(ni);
        if (data == null) return false;

		return (isMultiCutInTechnology(data));
	}

	/**
	 * Class MultiCutData determines the locations of cuts in a multi-cut contact node.
	 */
	public static class MultiCutData
	{
		/** the size of each cut */													private long cutSizeX, cutSizeY;
		/** the separation between cuts */											private long cutSep;
		/** the separation between cuts */											private long cutSep1D;
		/** the separation between cuts in 3-neiboring or more cases*/				private long cutSep2D;
		/** the number of cuts in X and Y */										private int cutsX, cutsY;
		/** the total number of cuts */												private int cutsTotal;
		/** the "reasonable" number of cuts (around the outside only) */			private int cutsReasonable;
		/** the X coordinate of the leftmost cut's center */						private long cutBaseX;
		/** the Y coordinate of the topmost cut's center */							private long cutBaseY;
		/** cut position of last top-edge cut (for interior-cut elimination) */		private double cutTopEdge;
		/** cut position of last left-edge cut  (for interior-cut elimination) */	private double cutLeftEdge;
		/** cut position of last right-edge cut  (for interior-cut elimination) */	private double cutRightEdge;


        private void calculateInternalData(EPoint size, ERectangle fullRectangle, NodeLayer cutLayer)
        {
            assert cutLayer.representation == NodeLayer.MULTICUTBOX;
            long gridWidth = size.getGridX() + fullRectangle.getGridWidth();
            long gridHeight = size.getGridY() + fullRectangle.getGridHeight();
            TechPoint[] techPoints = cutLayer.points;
            long lx = techPoints[0].getX().getGridAdder() + (long)(gridWidth*techPoints[0].getX().getMultiplier());
            long hx = techPoints[1].getX().getGridAdder() + (long)(gridWidth*techPoints[1].getX().getMultiplier());
            long ly = techPoints[0].getY().getGridAdder() + (long)(gridHeight*techPoints[0].getY().getMultiplier());
            long hy = techPoints[1].getY().getGridAdder() + (long)(gridHeight*techPoints[1].getY().getMultiplier());
            cutSizeX = cutLayer.cutGridSizeX;
            cutSizeY = cutLayer.cutGridSizeY;
            cutSep1D = cutLayer.cutGridSep1D;
            cutSep2D = cutLayer.cutGridSep2D;
            calculateInternalData(lx, hx, ly, hy);
        }

        private void calculateInternalData(long lx, long hx, long ly, long hy)
        {
			// determine the actual node size
            cutBaseX = (lx + hx)>>1;
            cutBaseY = (ly + hy)>>1;
			long cutAreaWidth = hx - lx;
			long cutAreaHeight = hy - ly;

			// number of cuts depends on the size
			// Checking first if configuration gives 2D cuts
            int oneDcutsX = 1 + (int)(cutAreaWidth / (cutSizeX+cutSep1D));
			int oneDcutsY = 1 + (int)(cutAreaHeight / (cutSizeY+cutSep1D));
            int twoDcutsX = 1 + (int)(cutAreaWidth / (cutSizeX+cutSep2D));
			int twoDcutsY = 1 + (int)(cutAreaHeight / (cutSizeY+cutSep2D));

			cutSep = cutSep1D;
			cutsX = oneDcutsX;
			cutsY = oneDcutsY;
			if (cutsX > 1 && cutsY > 1)
			{
				cutSep = cutSep2D;
				cutsX = twoDcutsX;
				cutsY = twoDcutsY;
				if (cutsX == 1 || cutsY == 1)
				{
					// 1D separation sees a 2D grid, but 2D separation sees a linear array: use 1D linear settings
					cutSep = cutSep1D;
					if (cutAreaWidth > cutAreaHeight)
					{
						cutsX = oneDcutsX;
					} else
					{
						cutsY = oneDcutsY;
					}
				}
			}
			if (cutsX <= 0) cutsX = 1;
			if (cutsY <= 0) cutsY = 1;
			cutsReasonable = cutsTotal = cutsX * cutsY;
			if (cutsTotal != 1)
			{
				// prepare for the multiple contact cut locations
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
		 * Constructor to initialize for multiple cuts.
		 */
		private MultiCutData(EPoint size, ERectangle fullRectangle, NodeLayer cutLayer)
		{
            calculateInternalData(size, fullRectangle, cutLayer);
		}

		/**
		 * Constructor to initialize for multiple cuts.
		 * @param niD the NodeInst with multiple cuts.
		 */
		public MultiCutData(ImmutableNodeInst niD, TechPool techPool)
		{
            this(niD.size,
                    techPool.getPrimitiveNode((PrimitiveNodeId)niD.protoId).getFullRectangle(),
                    techPool.getPrimitiveNode((PrimitiveNodeId)niD.protoId).findMulticut());
		}

		/**
		 * Method to return the number of cuts in the contact node.
		 * @return the number of cuts in the contact node.
		 */
		public int numCuts() { return cutsTotal; }

		/**
		 * Method to return the number of cuts along X axis in the contact node.
		 * @return the number of cuts in the contact node along X axis.
		 */
		public int numCutsX() { return cutsX; }

		/**
		 * Method to return the number of cuts along Y axis in the contact node.
		 * @return the number of cuts in the contact node along Y axis.
		 */
		public int numCutsY() { return cutsY; }

        /**
         * Method to return the size of the cut along X.
         */
        public double getCutSizeX() { return cutSizeX; }

        /**
         * Method to return the size of the cut along Y.
         */
        public double getCutSizeY() { return cutSizeY; }

        /**
		 * Method to fill in the contact cuts of a contact when there are
		 * multiple cuts.  Node is in "ni" and the contact cut number (0 based) is
		 * in "cut".
		 */
		protected Poly fillCutPoly(ImmutableNodeInst ni, int cut)
		{
            return (fillCutPoly(ni.anchor, cut));
		}

        /**
         * Method to fill in the contact cuts based on anchor information.
        */
        public Poly fillCutPoly(EPoint anchor, int cut)
		{
            long cX = anchor.getGridX() + cutBaseX;
            long cY = anchor.getGridY() + cutBaseY;
            if (cutsX > 1 || cutsY > 1) {
                if (cutsX > 2 && cutsY > 2) {
                    // rearrange cuts so that the initial ones go around the outside
                    if (cut < cutsX) {
                        // bottom edge: it's ok as is
                    } else if (cut < cutTopEdge) {
                        // top edge: shift up
                        cut += cutsX * (cutsY-2);
                    } else if (cut < cutLeftEdge) {
                        // left edge: rearrange
                        cut = (int)((cut - cutTopEdge) * cutsX + cutsX);
                    } else if (cut < cutRightEdge) {
                        // right edge: rearrange
                        cut = (int)((cut - cutLeftEdge) * cutsX + cutsX*2-1);
                    } else {
                        // center: rearrange and scale down
                        cut = cut - (int)cutRightEdge;
                        int cutx = cut % (cutsX-2);
                        int cuty = cut / (cutsX-2);
                        cut = cuty * cutsX + cutx+cutsX+1;
                    }
                }

                // locate the X center of the cut
                if (cutsX != 1)
                    cX += ((cut % cutsX)*2 - (cutsX - 1))*(cutSizeX + cutSep)*0.5;
                // locate the Y center of the cut
                if (cutsY != 1)
                    cY += ((cut / cutsX)*2 - (cutsY - 1))*(cutSizeY + cutSep)*0.5;
            }
            double lX = DBMath.gridToLambda(cX - (cutSizeX >> 1));
            double hX = DBMath.gridToLambda(cX + (cutSizeX >> 1));
            double lY = DBMath.gridToLambda(cY - (cutSizeY >> 1));
            double hY = DBMath.gridToLambda(cY + (cutSizeY >> 1));
            Point2D.Double[] points = new Point2D.Double[] {
                new Point2D.Double(lX, lY),
                new Point2D.Double(hX, lY),
                new Point2D.Double(hX, hY),
                new Point2D.Double(lX, hY)};
			return new Poly(points);
		}
	}

	/**
	 * Class SerpentineTrans here.
	 */
	private static class SerpentineTrans
	{
		private static final int LEFTANGLE =  900;
		private static final int RIGHTANGLE =  2700;

		/** the ImmutableNodeInst that is this serpentine transistor */			private ImmutableNodeInst theNode;
		/** the prototype of this serpentine transistor */						private PrimitiveNode theProto;
		/** the number of polygons that make up this serpentine transistor */	private int layersTotal;
		/** the number of segments in this serpentine transistor */				private int numSegments;
		/** the extra gate width of this serpentine transistor */				private double extraScale;
		/** the node layers that make up this serpentine transistor */			private Technology.NodeLayer [] primLayers;
		/** the gate coordinates for this serpentine transistor */				private Point2D [] points;
		/** the defining values for this serpentine transistor */				private double [] specialValues;
		/** true if there are separate field and gate polys */					private boolean fieldPolyOnEndsOnly;
		/** counter for filling the polygons of the serpentine transistor */	private int fillBox;

		/**
		 * Constructor throws initialize for a serpentine transistor.
		 * @param niD the NodeInst with a serpentine transistor.
		 */
		public SerpentineTrans(ImmutableNodeInst niD, PrimitiveNode protoType, Technology.NodeLayer [] pLayers)
		{
			theNode = niD;
			layersTotal = 0;
			points = niD.getTrace();
			if (points != null)
			{
				if (points.length < 2) points = null;
			}
			if (points != null)
			{
				theProto = protoType;
				specialValues = theProto.getSpecialValues();
				primLayers = pLayers;
				int count = primLayers.length;
				numSegments = points.length - 1;
				layersTotal = count;
//				layersTotal = count * numSegments;

				extraScale = 0;
				double length = niD.getSerpentineTransistorLength();
				if (length > 0) extraScale = (length - specialValues[3]) / 2;

				// see if there are separate field and gate poly layers
				fieldPolyOnEndsOnly = false;
				int numFieldPoly = 0, numGatePoly = 0;
				for(int i=0; i<count; i++)
				{
					if (primLayers[i].layer.getFunction().isPoly())
					{
						if (primLayers[i].layer.getFunction() == Layer.Function.GATE) numGatePoly++; else
							numFieldPoly++;
					}
				}
				if (numFieldPoly > 0 && numGatePoly > 0)
				{
					// when there are both field and gate poly elements, use field poly only on the ends
					fieldPolyOnEndsOnly = true;
//					layersTotal = (count-numFieldPoly) * numSegments + numFieldPoly;
				}
			}
		}

		/**
		 * Method to tell whether this SerpentineTrans object has valid outline information.
		 * @return true if the data exists.
		 */
		public boolean hasValidData() { return points != null; }

		/**
		 * Method to start the filling of polygons in the serpentine transistor.
		 * Call this before repeated calls to "fillTransPoly".
		 */
		private void initTransPolyFilling() { fillBox = 0; }

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
		private Poly fillTransPoly(boolean electrical)
		{
			int element = fillBox++;
			Technology.NodeLayer primLayer = primLayers[element];
			double extendt = primLayer.getSerpentineExtentT();
			double extendb = primLayer.getSerpentineExtentB();

			// if field poly appears only on the ends of the transistor, ignore interior requests
			boolean extendEnds = true;
			if (fieldPolyOnEndsOnly)
			{
				Layer layer = primLayer.getLayer();
				if (layer.getFunction().isPoly())
				{
					if (layer.getFunction() == Layer.Function.GATE)
					{
						// found the gate poly: do not extend it
						extendEnds = false;
					} else
					{
						// found piece of field poly
						if (extendt != 0)
						{
							// first endcap: extend "thissg" 180 degrees back
							int thissg = 0;   int nextsg = 1;
							Point2D thisPt = points[thissg];
							Point2D nextPt = points[nextsg];
							int angle = DBMath.figureAngle(thisPt, nextPt);
							nextPt = thisPt;
							int ang = angle+1800;
							thisPt = DBMath.addPoints(thisPt, DBMath.cos(ang) * extendt, DBMath.sin(ang) * extendt);
							return buildSerpentinePoly(element, 0, numSegments, electrical, thisPt, nextPt, angle);
						} else if (extendb != 0)
						{
							// last endcap: extend "next" 0 degrees forward
							int thissg = numSegments-1;   int nextsg = numSegments;
							Point2D thisPt = points[thissg];
							Point2D nextPt = points[nextsg];
							int angle = DBMath.figureAngle(thisPt, nextPt);
							thisPt = nextPt;
							nextPt = DBMath.addPoints(nextPt, DBMath.cos(angle) * extendb, DBMath.sin(angle) * extendb);
							return buildSerpentinePoly(element, 0, numSegments, electrical, thisPt, nextPt, angle);
						}
					}
				}
			}

			// fill the polygon
			Point2D [] outPoints = new Point2D.Double[(numSegments+1)*2];
			double xoff = theNode.anchor.getX();
			double yoff = theNode.anchor.getY();
			for(int segment=0; segment<numSegments; segment++)
			{
				int thissg = segment;   int nextsg = segment+1;
				Point2D thisPt = points[thissg];
				Point2D nextPt = points[nextsg];
				int angle = DBMath.figureAngle(thisPt, nextPt);
				if (extendEnds)
				{
					if (thissg == 0)
					{
						// extend "thissg" 180 degrees back
						int ang = angle+1800;
						thisPt = DBMath.addPoints(thisPt, DBMath.cos(ang) * extendt, DBMath.sin(ang) * extendt);
					}
					if (nextsg == numSegments)
					{
						// extend "next" 0 degrees forward
						nextPt = DBMath.addPoints(nextPt, DBMath.cos(angle) * extendb, DBMath.sin(angle) * extendb);
					}
				}

				// see if nonstandard width is specified
				double lwid = primLayer.getSerpentineLWidth();
				double rwid = primLayer.getSerpentineRWidth();
				lwid += extraScale;
				rwid += extraScale;

				// compute endpoints of line parallel to and left of center line
				int ang = angle+LEFTANGLE;
				double sin = DBMath.sin(ang) * lwid;
				double cos = DBMath.cos(ang) * lwid;
				Point2D thisL = DBMath.addPoints(thisPt, cos, sin);
				Point2D nextL = DBMath.addPoints(nextPt, cos, sin);

				// compute endpoints of line parallel to and right of center line
				ang = angle+RIGHTANGLE;
				sin = DBMath.sin(ang) * rwid;
				cos = DBMath.cos(ang) * rwid;
				Point2D thisR = DBMath.addPoints(thisPt, cos, sin);
				Point2D nextR = DBMath.addPoints(nextPt, cos, sin);

				// determine proper intersection of this and the previous segment
				if (thissg != 0)
				{
					Point2D otherPt = points[thissg-1];
					int otherang = DBMath.figureAngle(otherPt, thisPt);
					if (otherang != angle)
					{
						ang = otherang + LEFTANGLE;
						thisL = DBMath.intersect(DBMath.addPoints(thisPt, DBMath.cos(ang)*lwid, DBMath.sin(ang)*lwid),
							otherang, thisL,angle);
						ang = otherang + RIGHTANGLE;
						thisR = DBMath.intersect(DBMath.addPoints(thisPt, DBMath.cos(ang)*rwid, DBMath.sin(ang)*rwid),
							otherang, thisR,angle);
					}
				}

				// determine proper intersection of this and the next segment
				if (nextsg != numSegments)
				{
					Point2D otherPt = points[nextsg+1];
					int otherang = DBMath.figureAngle(nextPt, otherPt);
					if (otherang != angle)
					{
						ang = otherang + LEFTANGLE;
						Point2D newPtL = DBMath.addPoints(nextPt, DBMath.cos(ang)*lwid, DBMath.sin(ang)*lwid);
						nextL = DBMath.intersect(newPtL, otherang, nextL,angle);
						ang = otherang + RIGHTANGLE;
						Point2D newPtR = DBMath.addPoints(nextPt, DBMath.cos(ang)*rwid, DBMath.sin(ang)*rwid);
						nextR = DBMath.intersect(newPtR, otherang, nextR,angle);
					}
				}

				// fill the polygon
				if (segment == 0)
				{
					// fill in the first two points
					outPoints[0] = DBMath.addPoints(thisL, xoff, yoff);
					outPoints[1] = DBMath.addPoints(nextL, xoff, yoff);
					outPoints[(numSegments+1)*2-2] = DBMath.addPoints(nextR, xoff, yoff);
					outPoints[(numSegments+1)*2-1] = DBMath.addPoints(thisR, xoff, yoff);
				} else
				{
					outPoints[segment+1] = DBMath.addPoints(nextL, xoff, yoff);
					outPoints[(numSegments+1)*2-2-segment] = DBMath.addPoints(nextR, xoff, yoff);
				}
			}
			Poly retPoly = new Poly(outPoints);
			retPoly.setStyle(primLayer.getStyle());
			retPoly.setLayer(primLayer.getLayer());

			// include port information if requested
			if (electrical)
			{
				int portIndex = primLayer.getPortNum();
				if (portIndex >= 0)
				{
					assert theProto.getId() == theNode.protoId;
					PortProto port = theProto.getPort(portIndex);
					retPoly.setPort(port);
				}
			}
			return retPoly;

//			// the old way to compute it: segment-by-segment
//			int segment = 0, element = 0;
//			boolean isFieldPolyEndcap1 = false, isFieldPolyEndcap2 = false;
//			boolean extendEnds = true;
//			for(;;)
//			{
//				// compute the segment (along the serpent) and element (of transistor)
//				segment = fillBox % numSegments;
//				element = fillBox / numSegments;
//				fillBox++;
//
//				// if field poly appears only on the ends of the transistor, ignore interior requests
//				if (fieldPolyOnEndsOnly)
//				{
//					Layer layer = primLayers[element].getLayer();
//					if (layer.getFunction().isPoly())
//					{
//						if (layer.getFunction() == Layer.Function.GATE)
//						{
//							// found the gate poly: do not extend it
//							extendEnds = false;
//						} else
//						{
//							// found piece of field poly
//							if (segment == 0 && primLayers[element].getSerpentineExtentT() != 0) isFieldPolyEndcap1 = true; else
//								if (segment == numSegments-1 && primLayers[element].getSerpentineExtentB() != 0) isFieldPolyEndcap2 = true; else
//									continue;
//						}
//					}
//				}
//				break;
//			}
//
//			// prepare to fill the serpentine transistor
//			int thissg = segment;   int nextsg = segment+1;
//			Point2D thisPt = points[thissg];
//			Point2D nextPt = points[nextsg];
//			int angle = DBMath.figureAngle(thisPt, nextPt);
//			double extendt = primLayers[element].getSerpentineExtentT();
//			double extendb = primLayers[element].getSerpentineExtentB();
//
//			// special case for field poly endcaps
//			if (isFieldPolyEndcap1)
//			{
//				// first endcap: extend "thissg" 180 degrees back
//				nextPt = thisPt;
//				int ang = angle+1800;
//				thisPt = DBMath.addPoints(thisPt, DBMath.cos(ang) * extendt, DBMath.sin(ang) * extendt);
//				return buildSerpentinePoly(element, 0, numSegments, electrical, thisPt, nextPt, angle);
//			}
//			if (isFieldPolyEndcap2)
//			{
//				// last endcap: extend "next" 0 degrees forward
//				thisPt = nextPt;
//				nextPt = DBMath.addPoints(nextPt, DBMath.cos(angle) * extendb, DBMath.sin(angle) * extendb);
//				return buildSerpentinePoly(element, 0, numSegments, electrical, thisPt, nextPt, angle);
//			}
//
//			// push the points at the ends of the transistor
//			if (extendEnds)
//			{
//				if (thissg == 0)
//				{
//					// extend "thissg" 180 degrees back
//					int ang = angle+1800;
//					thisPt = DBMath.addPoints(thisPt, DBMath.cos(ang) * extendt, DBMath.sin(ang) * extendt);
//				}
//				if (nextsg == numSegments)
//				{
//					// extend "next" 0 degrees forward
//					nextPt = DBMath.addPoints(nextPt, DBMath.cos(angle) * extendb, DBMath.sin(angle) * extendb);
//				}
//			}
//
//			return buildSerpentinePoly(element, thissg, nextsg, electrical, thisPt, nextPt, angle);
		}

		private Poly buildSerpentinePoly(int element, int thissg, int nextsg, boolean electrical, Point2D thisPt, Point2D nextPt, int angle)
		{
			// see if nonstandard width is specified
			Technology.NodeLayer primLayer = primLayers[element];
			double lwid = primLayer.getSerpentineLWidth();
			double rwid = primLayer.getSerpentineRWidth();
			lwid += extraScale;
			rwid += extraScale;

			// compute endpoints of line parallel to and left of center line
			int ang = angle+LEFTANGLE;
			double sin = DBMath.sin(ang) * lwid;
			double cos = DBMath.cos(ang) * lwid;
			Point2D thisL = DBMath.addPoints(thisPt, cos, sin);
			Point2D nextL = DBMath.addPoints(nextPt, cos, sin);

			// compute endpoints of line parallel to and right of center line
			ang = angle+RIGHTANGLE;
			sin = DBMath.sin(ang) * rwid;
			cos = DBMath.cos(ang) * rwid;
			Point2D thisR = DBMath.addPoints(thisPt, cos, sin);
			Point2D nextR = DBMath.addPoints(nextPt, cos, sin);

			// determine proper intersection of this and the previous segment
			if (thissg != 0)
			{
				Point2D otherPt = points[thissg-1];
				int otherang = DBMath.figureAngle(otherPt, thisPt);
				if (otherang != angle)
				{
					ang = otherang + LEFTANGLE;
					thisL = DBMath.intersect(DBMath.addPoints(thisPt, DBMath.cos(ang)*lwid, DBMath.sin(ang)*lwid),
						otherang, thisL,angle);
					ang = otherang + RIGHTANGLE;
					thisR = DBMath.intersect(DBMath.addPoints(thisPt, DBMath.cos(ang)*rwid, DBMath.sin(ang)*rwid),
						otherang, thisR,angle);
				}
			}

			// determine proper intersection of this and the next segment
			if (nextsg != numSegments)
			{
				Point2D otherPt = points[nextsg+1];
				int otherang = DBMath.figureAngle(nextPt, otherPt);
				if (otherang != angle)
				{
					ang = otherang + LEFTANGLE;
					Point2D newPtL = DBMath.addPoints(nextPt, DBMath.cos(ang)*lwid, DBMath.sin(ang)*lwid);
					nextL = DBMath.intersect(newPtL, otherang, nextL,angle);
					ang = otherang + RIGHTANGLE;
					Point2D newPtR = DBMath.addPoints(nextPt, DBMath.cos(ang)*rwid, DBMath.sin(ang)*rwid);
					nextR = DBMath.intersect(newPtR, otherang, nextR,angle);
				}
			}

			// fill the polygon
			Point2D [] points = new Point2D.Double[4];
			double xoff = theNode.anchor.getX();
			double yoff = theNode.anchor.getY();
			points[0] = DBMath.addPoints(thisL, xoff, yoff);
			points[1] = DBMath.addPoints(thisR, xoff, yoff);
			points[2] = DBMath.addPoints(nextR, xoff, yoff);
			points[3] = DBMath.addPoints(nextL, xoff, yoff);
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

			retPoly.setStyle(primLayer.getStyle());
			retPoly.setLayer(primLayer.getLayer());

			// include port information if requested
			if (electrical)
			{
				int portIndex = primLayer.getPortNum();
				if (portIndex >= 0)
				{
					assert theProto.getId() == theNode.protoId;
					PortProto port = theProto.getPort(portIndex);
					retPoly.setPort(port);
				}
			}
			return retPoly;
		}

		/**
		 * Method to describe a port in a transistor that is part of a serpentine path.
		 * The port path is shrunk by "diffInset" in the length and is pushed "diffExtend" from the centerline.
		 * The default width of the transistor is "defWid".
		 * The assumptions about directions are:
		 * Segments have port 1 to the left, and port 3 to the right of the gate trace.
		 * Port 0, the "left-hand" end of the gate, appears at the starting
		 * end of the first trace segment; port 2, the "right-hand" end of the gate,
		 * appears at the end of the last trace segment.  Port 3 is drawn as a
		 * reflection of port 1 around the trace.
		 * The poly ports are extended "polyExtend" beyond the appropriate end of the trace
		 * and are inset by "polyInset" from the polysilicon edge.
		 * The diffusion ports are extended "diffExtend" from the polysilicon edge
		 * and set in "diffInset" from the ends of the trace segment.
		 */
		private Poly fillTransPort(PortProto pp)
		{
			double diffInset = specialValues[1];
			double diffExtend = specialValues[2];
			double defWid = specialValues[3] + extraScale;
			double polyInset = specialValues[4];
			double polyExtend = specialValues[5];

			// prepare to fill the serpentine transistor port
			double xOff = theNode.anchor.getX();
			double yOff = theNode.anchor.getY();
			int total = points.length;
			AffineTransform trans = theNode.orient.rotateAbout(theNode.anchor.getX(), theNode.anchor.getY());

			// determine which port is being described
			int which = 0;
			for(Iterator<PortProto> it = theProto.getPorts(); it.hasNext(); )
			{
				PortProto lpp = it.next();
				if (lpp == pp) break;
				which++;
			}

			// ports 0 and 2 are poly (simple)
			if (which == 0)
			{
				Point2D thisPt = new Point2D.Double(points[0].getX(), points[0].getY());
				Point2D nextPt = new Point2D.Double(points[1].getX(), points[1].getY());
				int angle = DBMath.figureAngle(thisPt, nextPt);
				int ang = (angle+1800) % 3600;
				thisPt.setLocation(thisPt.getX() + DBMath.cos(ang) * polyExtend + xOff,
					thisPt.getY() + DBMath.sin(ang) * polyExtend + yOff);

				ang = (angle+LEFTANGLE) % 3600;
				Point2D end1 = new Point2D.Double(thisPt.getX() + DBMath.cos(ang) * (defWid/2-polyInset),
					thisPt.getY() + DBMath.sin(ang) * (defWid/2-polyInset));

				ang = (angle+RIGHTANGLE) % 3600;
				Point2D end2 = new Point2D.Double(thisPt.getX() + DBMath.cos(ang) * (defWid/2-polyInset),
					thisPt.getY() + DBMath.sin(ang) * (defWid/2-polyInset));

				Point2D [] portPoints = new Point2D.Double[2];
				portPoints[0] = end1;
				portPoints[1] = end2;
				trans.transform(portPoints, 0, portPoints, 0, 2);
				Poly retPoly = new Poly(portPoints);
				retPoly.setStyle(Poly.Type.OPENED);
				return retPoly;
			}
			if (which == 2)
			{
				Point2D thisPt = new Point2D.Double(points[total-1].getX(), points[total-1].getY());
				Point2D nextPt = new Point2D.Double(points[total-2].getX(), points[total-2].getY());
				int angle = DBMath.figureAngle(thisPt, nextPt);
				int ang = (angle+1800) % 3600;
				thisPt.setLocation(thisPt.getX() + DBMath.cos(ang) * polyExtend + xOff,
					thisPt.getY() + DBMath.sin(ang) * polyExtend + yOff);

				ang = (angle+LEFTANGLE) % 3600;
				Point2D end1 = new Point2D.Double(thisPt.getX() + DBMath.cos(ang) * (defWid/2-polyInset),
					thisPt.getY() + DBMath.sin(ang) * (defWid/2-polyInset));

				ang = (angle+RIGHTANGLE) % 3600;
				Point2D end2 = new Point2D.Double(thisPt.getX() + DBMath.cos(ang) * (defWid/2-polyInset),
					thisPt.getY() + DBMath.sin(ang) * (defWid/2-polyInset));

				Point2D [] portPoints = new Point2D.Double[2];
				portPoints[0] = end1;
				portPoints[1] = end2;
				trans.transform(portPoints, 0, portPoints, 0, 2);
				Poly retPoly = new Poly(portPoints);
				retPoly.setStyle(Poly.Type.OPENED);
				return retPoly;
			}

			// port 3 is the negated path side of port 1
			if (which == 3)
			{
				diffExtend = -diffExtend;
				defWid = -defWid;
			}

			// extra port on some n-transistors
			if (which == 4) diffExtend = defWid = 0;

			Point2D [] portPoints = new Point2D.Double[total];
			Point2D lastPoint = null;
			int lastAngle = 0;
			for(int nextIndex=1; nextIndex<total; nextIndex++)
			{
				int thisIndex = nextIndex-1;
				Point2D thisPt = new Point2D.Double(points[thisIndex].getX() + xOff, points[thisIndex].getY() + yOff);
				Point2D nextPt = new Point2D.Double(points[nextIndex].getX() + xOff, points[nextIndex].getY() + yOff);
				int angle = DBMath.figureAngle(thisPt, nextPt);

				// determine the points
				if (thisIndex == 0)
				{
					// extend "this" 0 degrees forward
					thisPt.setLocation(thisPt.getX() + DBMath.cos(angle) * diffInset,
						thisPt.getY() + DBMath.sin(angle) * diffInset);
				}
				if (nextIndex == total-1)
				{
					// extend "next" 180 degrees back
					int backAng = (angle+1800) % 3600;
					nextPt.setLocation(nextPt.getX() + DBMath.cos(backAng) * diffInset,
						nextPt.getY() + DBMath.sin(backAng) * diffInset);
				}

				// compute endpoints of line parallel to center line
				int ang = (angle+LEFTANGLE) % 3600;
				double sine = DBMath.sin(ang);
				double cosine = DBMath.cos(ang);
				thisPt.setLocation(thisPt.getX() + cosine * (defWid/2+diffExtend),
					thisPt.getY() + sine * (defWid/2+diffExtend));
				nextPt.setLocation(nextPt.getX() + cosine * (defWid/2+diffExtend),
					nextPt.getY() + sine * (defWid/2+diffExtend));

				if (thisIndex != 0)
				{
					// compute intersection of this and previous line
					thisPt = DBMath.intersect(lastPoint, lastAngle, thisPt, angle);
				}
				portPoints[thisIndex] = thisPt;
				lastPoint = thisPt;
				lastAngle = angle;
				if (nextIndex == total-1)
					portPoints[nextIndex] = nextPt;
			}
			if (total > 0)
				trans.transform(portPoints, 0, portPoints, 0, total);
			Poly retPoly = new Poly(portPoints);
			retPoly.setStyle(Poly.Type.OPENED);
			return retPoly;
		}
	}

	/**
	 * Method to convert old primitive node names to their proper NodeProtos.
	 * @param name the unknown node name, read from an old Library.
	 * @return the proper PrimitiveNode to use for this name.
	 */
	public PrimitiveNode convertOldNodeName(String name) {
        return oldNodeNames.get(name);
    }

    public Map<String,PrimitiveNode> getOldNodeNames() { return new TreeMap<String,PrimitiveNode>(oldNodeNames); }

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
		return getShapeOfPort(ni, pp, null);
	}

	/**
	 * Returns a polygon that describes a particular port on a NodeInst.
	 * @param ni the NodeInst that has the port of interest.
	 * The prototype of this NodeInst must be a PrimitiveNode and not a Cell.
	 * @param pp the PrimitivePort on that NodeInst that is being described.
	 * @param selectPt if not null, it requests a new location on the port,
	 * away from existing arcs, and close to this point.
	 * This is useful for "area" ports such as the left side of AND and OR gates.
	 * @return a Poly object that describes this PrimitivePort graphically.
	 */
	public Poly getShapeOfPort(NodeInst ni, PrimitivePort pp, Point2D selectPt)
	{
		PrimitiveNode np = (PrimitiveNode)ni.getProto();
		if (np.getSpecialType() == PrimitiveNode.SERPTRANS)
		{
			// serpentine transistors use a more complex port determination
			SerpentineTrans std = new SerpentineTrans(ni.getD(), np, np.getLayers());
			if (std.hasValidData())
				return std.fillTransPort(pp);
		}

		// standard port determination, see if there is outline information
		if (np.isHoldsOutline())
		{
			// outline may determine the port
			Point2D [] outline = ni.getTrace();
			if (outline != null)
			{
				int endPortPoly = outline.length;
				if (HANDLEBROKENOUTLINES)
				{
					for(int i=1; i<outline.length; i++)
					{
						if (outline[i] == null || outline[i].getX() == outline[i-1].getX() && outline[i].getY() == outline[i-1].getY())
						{
							endPortPoly = i;
							break;
						}
					}
				}
				double cX = ni.getAnchorCenterX();
				double cY = ni.getAnchorCenterY();
				Point2D [] pointList = new Point2D.Double[endPortPoly];
				for(int i=0; i<endPortPoly; i++)
					pointList[i] = new Point2D.Double(cX + outline[i].getX(), cY + outline[i].getY());
				Poly portPoly = new Poly(pointList);
				if (ni.getFunction() == PrimitiveNode.Function.NODE)
				{
					portPoly.setStyle(Poly.Type.FILLED);
				} else
				{
					portPoly.setStyle(Poly.Type.OPENED);
				}
				portPoly.setTextDescriptor(TextDescriptor.getExportTextDescriptor());
				return portPoly;
			}
		}

		// standard port computation
		double portLowX = ni.getAnchorCenterX() + pp.getLeft().getMultiplier() * ni.getXSize() + pp.getLeft().getAdder();
		double portHighX = ni.getAnchorCenterX() + pp.getRight().getMultiplier() * ni.getXSize() + pp.getRight().getAdder();
		double portLowY = ni.getAnchorCenterY() + pp.getBottom().getMultiplier() * ni.getYSize() + pp.getBottom().getAdder();
		double portHighY = ni.getAnchorCenterY() + pp.getTop().getMultiplier() * ni.getYSize() + pp.getTop().getAdder();
		double portX = (portLowX + portHighX) / 2;
		double portY = (portLowY + portHighY) / 2;
		Poly portPoly = new Poly(portX, portY, portHighX-portLowX, portHighY-portLowY);
		portPoly.setStyle(Poly.Type.FILLED);
		portPoly.setTextDescriptor(TextDescriptor.getExportTextDescriptor());
		return portPoly;
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
        if (np.getNumPorts() == 1)
            return np.getPort(0);
		return null;
	}

    /**
     * Tells if all ArcProtos can connect to the PrimitivePort
     * @param pp PrimitivePort to test
     * @return true if all ArcProtos can connect to the PrimitivePort
     */
	public boolean isUniversalConnectivityPort(PrimitivePort pp) {
        return false;
    }


	/*********************** PARASITIC SETTINGS ***************************/

    private Setting makeParasiticSetting(String what, double factory) {
        String techShortName = getTechShortName();
        if (techShortName == null) techShortName = getTechName();
        return Setting.makeDoubleSetting(what + "IN" + getTechName(), prefs,
                getProjectSettings(), what,
                "Parasitic tab", techShortName + " " + what, factory);
    }

    private Setting makeParasiticSetting(String what, boolean factory) {
        String techShortName = getTechShortName();
        if (techShortName == null) techShortName = getTechName();
        return Setting.makeBooleanSetting(what + "IN" + getTechName(), prefs,
                getProjectSettings(), what,
                "Parasitic tab", techShortName + " " + what, factory);
    }

	/**
	 * Method to return the Pref object associated with all Technologies.
	 * The Pref object is used to save option information.
	 * Since preferences are organized by package, there is only one for
	 * the technologies (they are all in the same package).
	 * @return the Pref object associated with all Technologies.
	 */
	public static Pref.Group getTechnologyPreferences() { return prefs; }

	/**
	 * Returns the minimum resistance of this Technology.
     * Default value is 10.0
	 * @return the minimum resistance of this Technology.
	 */
	public double getMinResistance()
	{
		return cacheMinResistance.getDouble();
	}
	/**
	 * Returns project Setting to tell the minimum resistance of this Technology.
	 * @return project Setting to tell the minimum resistance of this Technology.
	 */
	public Setting getMinResistanceSetting() { return cacheMinResistance; }

	/**
	 * Returns the minimum capacitance of this Technology.
     * Default value is 0.0
	 * @return the minimum capacitance of this Technology.
	 */
	public double getMinCapacitance()
	{
        // 0.0 is the default value
		return cacheMinCapacitance.getDouble();
	}
	/**
	 * Returns project Setting to tell the minimum capacitance of this Technology.
	 * @return project Setting to tell the minimum capacitance of this Technology.
	 */
	public Setting getMinCapacitanceSetting() { return cacheMinCapacitance; }


    /**
     * Get the maximum series resistance for layout extraction
     *  for this Technology.
     * @return the maximum series resistance of extracted layout nets
     */
    public double getMaxSeriesResistance()
    {
        return cacheMaxSeriesResistance.getDouble();
    }
    /**
     * Returns project Setting to tell the maximum series resistance for layout extraction
     *  for this Technology.
     * @return project Setting to tell the maximum series resistance for layout extraction
     *  for this Technology.
     */
    public Setting getMaxSeriesResistanceSetting() { return cacheMaxSeriesResistance; }


	/**
	 * Returns true if gate is included in resistance calculation. False is the default.
	 * @return true if gate is included in resistance calculation.
	 */
	public boolean isGateIncluded()
	{
        // False is the default
		return cacheIncludeGate.getBoolean();
	}
    /**
     * Returns project Setting to tell gate inclusion.
     * @return project Setting to tell gate inclusion
     */
    public Setting getGateIncludedSetting() { return cacheIncludeGate; }

    /**
     * Returns true if ground network is included in parasitics calculation. False is the default.
     * @return true if ground network is included.
     */
    public boolean isGroundNetIncluded()
    {
        // False is the default
        return cacheIncludeGnd.getBoolean();
    }
	/**
	 * Returns project Setting to tell ground network inclusion.
	 * @return project Setting to tell ground network inclusion
	 */
	public Setting getGroundNetIncludedSetting() { return cacheIncludeGnd; }


    /**
     * Gets the gate length subtraction for this Technology (in microns).
     * This is used because there is sometimes a subtracted offset from the layout
     * to the drawn length.
     * @return the gate length subtraction for this Technology
     */
    public double getGateLengthSubtraction()
    {
        return cacheGateLengthSubtraction.getDouble();
    }
    /**
     * Returns project Setting to tell the gate length subtraction for this Technology (in microns)
     * This is used because there is sometimes a subtracted offset from the layout
     * to the drawn length.
     * @return project Setting to tell the subtraction value for a gate length in microns
     */
    public Setting getGateLengthSubtractionSetting() { return cacheGateLengthSubtraction; }


	/**
	 * Method to set default parasitic values on this Technology.
	 * These values are not saved in the options.
	 * @param minResistance the minimum resistance in this Technology.
	 * @param minCapacitance the minimum capacitance in this Technology.
	 */
	public void setFactoryParasitics(double minResistance, double minCapacitance)
	{
		cacheMinResistance = makeParasiticSetting("MininumResistance", minResistance);
		cacheMinCapacitance = makeParasiticSetting("MininumCapacitance", minCapacitance);
	}

    /*********************** LOGICAL EFFORT SETTINGS ***************************/

    private ProjSettingsNode getLESettingsNode() {
        ProjSettingsNode node = getProjectSettings().getNode("LogicalEffort");
//        if (node == null) {
//            node = new ProjSettingsNode();
//            getProjectSettings().putNode("LogicalEffort", node);
//        }
        return node;
    }

    private Setting makeLESetting(String what, double factory) {
        String techShortName = getTechShortName();
        if (techShortName == null) techShortName = getTechName();
        return Setting.makeDoubleSetting(what + "IN" + getTechName(), prefs,
                getLESettingsNode(), what,
                "Logical Effort tab", techShortName + " " + what, factory);
    }

//     private Setting makeLESetting(String what, int factory) {
//         String techShortName = getTechShortName();
//         if (techShortName == null) techShortName = getTechName();
//         return Setting.makeIntSetting(what + "IN" + getTechName(), prefs,
//                 getLESettingsNode(), what,
//                 "Logical Effort tab", techShortName + " " + what, factory);
//     }

    // ************************ tech specific?  - start *****************************
//    /**
//	 * Method to get the Global Fanout for Logical Effort.
//	 * The default is DEFAULT_GLOBALFANOUT.
//	 * @return the Global Fanout for Logical Effort.
//	 */
//	public double getGlobalFanout()
//	{
//		return cacheGlobalFanout.getDouble();
//	}
//	/**
//	 * Method to set the Global Fanout for Logical Effort.
//	 * @param fo the Global Fanout for Logical Effort.
//	 */
//	public void setGlobalFanout(double fo)
//	{
//		cacheGlobalFanout.setDouble(fo);
//	}
//
//	/**
//	 * Method to get the Convergence Epsilon value for Logical Effort.
//	 * The default is DEFAULT_EPSILON.
//	 * @return the Convergence Epsilon value for Logical Effort.
//	 */
//	public double getConvergenceEpsilon()
//	{
//		return cacheConvergenceEpsilon.getDouble();
//	}
//	/**
//	 * Method to set the Convergence Epsilon value for Logical Effort.
//	 * @param ep the Convergence Epsilon value for Logical Effort.
//	 */
//	public void setConvergenceEpsilon(double ep)
//	{
//		cacheConvergenceEpsilon.setDouble(ep);
//	}
//
//	/**
//	 * Method to get the maximum number of iterations for Logical Effort.
//	 * The default is DEFAULT_MAXITER.
//	 * @return the maximum number of iterations for Logical Effort.
//	 */
//	public int getMaxIterations()
//	{
//		return cacheMaxIterations.getInt();
//	}
//	/**
//	 * Method to set the maximum number of iterations for Logical Effort.
//	 * @param it the maximum number of iterations for Logical Effort.
//	 */
//	public void setMaxIterations(int it)
//	{
//		cacheMaxIterations.setInt(it);
//	}
//
//    /**
//     * Method to get the keeper size ratio for Logical Effort.
//     * The default is DEFAULT_KEEPERRATIO.
//     * @return the keeper size ratio for Logical Effort.
//     */
//    public double getKeeperRatio()
//    {
//        return cacheKeeperRatio.getDouble();
//    }
//    /**
//     * Method to set the keeper size ratio for Logical Effort.
//     * @param kr the keeper size ratio for Logical Effort.
//     */
//    public void setKeeperRatio(double kr)
//    {
//        cacheKeeperRatio.setDouble(kr);
//    }

    // ************************ tech specific?  - end *****************************

    protected void setFactoryLESettings(double gateCapacitance, double wireRation, double diffAlpha) {
		cacheGateCapacitance = makeLESetting("GateCapacitance", gateCapacitance);
		cacheWireRatio = makeLESetting("WireRatio", wireRation);
		cacheDiffAlpha = makeLESetting("DiffAlpha", diffAlpha);
    }

	/**
	 * Method to get the Gate Capacitance for Logical Effort.
	 * The default is DEFAULT_GATECAP.
	 * @return the Gate Capacitance for Logical Effort.
	 */
	public double getGateCapacitance()
	{
		return cacheGateCapacitance.getDouble();
	}
	/**
	 * Returns project Setting to tell the Gate Capacitance for Logical Effort.
	 * @return project Setting to tell the Gate Capacitance for Logical Effort.
	 */
	public Setting getGateCapacitanceSetting() { return cacheGateCapacitance; }

	/**
	 * Method to get the wire capacitance ratio for Logical Effort.
	 * The default is DEFAULT_WIRERATIO.
	 * @return the wire capacitance ratio for Logical Effort.
	 */
	public double getWireRatio()
	{
		return cacheWireRatio.getDouble();
	}
	/**
	 * Returns project Setting to tell the wire capacitance ratio for Logical Effort.
	 * @return project Setting to tell the wire capacitance ratio for Logical Effort.
	 */
	public Setting getWireRatioSetting() { return cacheWireRatio; }

	/**
	 * Method to get the diffusion to gate capacitance ratio for Logical Effort.
	 * The default is DEFAULT_DIFFALPHA.
	 * @return the diffusion to gate capacitance ratio for Logical Effort.
	 */
	public double getDiffAlpha()
	{
		return cacheDiffAlpha.getDouble();
	}
	/**
	 * Returns project Setting to tell the diffusion to gate capacitance ratio for Logical Effort.
	 * @return project Setting to tell the diffusion to gate capacitance ratio for Logical Effort.
	 */
	public Setting getDiffAlphaSetting() { return cacheDiffAlpha; }

    // ================================================================

	/**
	 * Method to return the level-1 header cards for SPICE in this Technology.
	 * The default is [""].
	 * @return the level-1 header cards for SPICE in this Technology.
	 */
	public String [] getSpiceHeaderLevel1() { return spiceHeaderLevel1; }

	/**
	 * Method to set the level-1 header cards for SPICE in this Technology.
	 * @param lines the level-1 header cards for SPICE in this Technology.
	 */
	public void setSpiceHeaderLevel1(String [] lines) { spiceHeaderLevel1 = lines; }

	/**
	 * Method to return the level-2 header cards for SPICE in this Technology.
	 * The default is [""].
	 * @return the level-2 header cards for SPICE in this Technology.
	 */
	public String [] getSpiceHeaderLevel2() { return spiceHeaderLevel2; }

	/**
	 * Method to set the level-2 header cards for SPICE in this Technology.
	 * @param lines the level-2 header cards for SPICE in this Technology.
	 */
	public void setSpiceHeaderLevel2(String [] lines) { spiceHeaderLevel2 = lines; }

	/**
	 * Method to return the level-3 header cards for SPICE in this Technology.
	 * The default is [""].
	 * @return the level-3 header cards for SPICE in this Technology.
	 */
	public String [] getSpiceHeaderLevel3() { return spiceHeaderLevel3; }

	/**
	 * Method to set the level-3 header cards for SPICE in this Technology.
	 * @param lines the level-3 header cards for SPICE in this Technology.
	 */
	public void setSpiceHeaderLevel3(String [] lines) { spiceHeaderLevel3 = lines; }

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
	protected void setStaticTechnology() { userBits |= STATICTECHNOLOGY; }

	/**
	 * Returns true if this technoology is "static" (cannot be deleted).
	 * @return true if this technoology is "static" (cannot be deleted).
	 * Static technologies are the core set of technologies in Electric that are
	 * essential, and cannot be deleted.
	 * The technology-editor can create others later, and they can be deleted.
	 */
	public boolean isStaticTechnology() { return (userBits & STATICTECHNOLOGY) != 0; }

	/**
	 * Returns the TechId of this technology.
	 * Each technology has a unique name, such as "mocmos" (MOSIS CMOS).
	 * @return the TechId of this technology.
	 */
	public TechId getId() { return techId; }

	/**
	 * Returns the name of this technology.
	 * Each technology has a unique name, such as "mocmos" (MOSIS CMOS).
	 * @return the name of this technology.
	 */
	public String getTechName() { return techId.techName; }

	/**
	 * Sets the name of this technology.
	 * Technology names must be unique.
	 */
	public void setTechName(String techName)
	{
        throw new UnsupportedOperationException(); // Correct implementation must also rename ProjectSettings and Preferences of this Technology

//		for(Iterator<Technology> it = Technology.getTechnologies(); it.hasNext(); )
//		{
//			Technology tech = it.next();
//			if (tech == this) continue;
//			if (tech.techName.equalsIgnoreCase(techName))
//			{
//				System.out.println("Cannot rename " + this + "to '" + techName + "' because that name is used by another technology");
//				return;
//			}
//		}
//		if (!jelibSafeName(techName))
//			System.out.println("Technology name " + techName + " is not safe to write into JELIB");
//		this.techName = techName;
	}

	/**
	 * Method checks that string is safe to write into JELIB file without
	 * conversion.
	 * @param str the string to check.
	 * @return true if string is safe to write into JELIB file.
	 */
	static boolean jelibSafeName(String str)
	{
        return TechId.jelibSafeName(str);
	}

	/**
	 * Returns the short name of this technology.
	 * The short name is user readable ("MOSIS CMOS" instead of "mocmos")
	 * but is shorter than the "description" which often includes options.
	 * @return the short name of this technology.
	 */
	public String getTechShortName() { return techShortName; }

	/**
	 * Sets the short name of this technology.
	 * The short name is user readable ("MOSIS CMOS" instead of "mocmos")
	 * but is shorter than the "description" which often includes options.
	 * @param techShortName the short name for this technology.
	 */
	protected void setTechShortName(String techShortName) { this.techShortName = techShortName; }

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
	 * Returns the scale for this Technology.
	 * The technology's scale is for manufacturing output, which must convert
	 * the unit-based values in Electric to real-world values (in nanometers).
	 * @return the scale for this Technology.
	 */
	public double getScale()
	{
		return cacheScale.getDouble();
	}

	/**
	 * Method to obtain the Variable name for scaling this Technology.
	 * Do not use this for arbitrary use.
	 * The method exists so that ELIB readers can handle the unusual location
	 * of scale information in the ELIB files.
	 * @return the Variable name for scaling this Technology.
	 */
	public String getScaleVariableName()
	{
		return "ScaleFOR" + getTechName();
	}

	/**
	 * Sets the factory scale of this technology.
	 * The technology's scale is for manufacturing output, which must convert
	 * the unit-based values in Electric to real-world values (in nanometers).
	 * @param factory the factory scale between this technology and the real units.
	 * @param scaleRelevant true if this is a layout technology, and the scale factor has meaning.
	 */
	protected void setFactoryScale(double factory, boolean scaleRelevant)
	{
		this.scaleRelevant = scaleRelevant;
        String techShortName = getTechShortName();
        if (techShortName == null) techShortName = getTechName();
		cacheScale = Setting.makeDoubleSetting(getScaleVariableName(), prefs,
                getProjectSettings(), "Scale", "Scale tab", techShortName + " scale", factory);
		cacheScale.setValidOption(isScaleRelevant());
    }

	/**
	 * Returns project Setting to tell the scale of this technology.
	 * The technology's scale is for manufacturing output, which must convert
	 * the unit-based values in Electric to real-world values (in nanometers).
	 * @return project Setting to tell the scale between this technology and the real units.
	 */
	public Setting getScaleSetting() { return cacheScale; }

	/**
	 * Method to tell whether scaling is relevant for this Technology.
	 * Most technolgies produce drawings that are exact images of a final product.
	 * For these technologies (CMOS, bipolar, etc.) the "scale" from displayed grid
	 * units to actual dimensions is a relevant factor.
	 * Other technologies, such as schematics, artwork, and generic,
	 * are not converted to physical objects, and "scale" is not relevant no meaning for them.
	 * @return true if scaling is relevant for this Technology.
	 */
	public boolean isScaleRelevant() { return scaleRelevant; }

    /**
     * Method to set Technology resolution in IO/DRC tools.
     * This has to be stored per technology.
     * @param factory factory value
     */
    protected void setFactoryResolution(double factory)
    {
        prefResolution = Pref.makeDoublePref("ResolutionValueFor"+getTechName(), prefs, factory);
    }

    /**
     * Method to set the technology resolution.
     * This is the minimum size unit that can be represented.
     * @param resolution new resolution value.
     */
	public void setResolution(double resolution)
	{
		if (prefResolution == null) setFactoryResolution(0);
		prefResolution.setDouble(resolution);
	}

    /**
     * Method to retrieve the resolution associated to the technology.
     * This is the minimum size unit that can be represented.
     * @return the technology's resolution value.
     */
    public double getResolution()
	{
        if (prefResolution == null) setFactoryResolution(0);
		return prefResolution.getDouble();
	}

    /**
     * Method to retrieve the default resolution associated to the technology.
     * This is the minimum size unit that can be represented.
     * @return the technology's default resolution value.
     */
    public double getFactoryResolution()
	{
        if (prefResolution == null) setFactoryResolution(0);
		return prefResolution.getDoubleFactoryValue();
	}

	/**
	 * Method to get foundry in Tech Palette. Different foundry can define different DRC rules.
	 * The default is "Generic".
	 * @return the foundry to use in Tech Palette
	 */
	public String getPrefFoundry()
    {
        return cacheFoundry.getString().toUpperCase();
    }

	/**
	 * Returns project Setting to tell foundry for DRC rules.
	 * @return project Setting to tell the foundry for DRC rules.
	 */
	public Setting getPrefFoundrySetting() { return cacheFoundry; }

    /**
	 * Find the Foundry in this technology with a particular name. Protected so sub classes will use it
	 * @param name the name of the desired Foundry.
	 * @return the Foundry with the same name, or null if no Foundry matches.
	 */
	protected Foundry findFoundry(String name)
	{
		if (name == null) return null;

        for (Foundry f : foundries)
        {
            Foundry.Type t = f.getType();
            if (t.getName().equalsIgnoreCase(name))
                return f;
        }
		return null;
	}

    /**
	 * Get an iterator over all of the Manufacturers.
	 * @return an iterator over all of the Manufacturers.
	 */
	public Iterator<Foundry> getFoundries()
	{
		return foundries.iterator();
	}

    /**
     * Method to create a new on this technology.
     * @param mode factory type
     * @param fileURL URL of xml file with description of rules
     * @param gdsLayers stirngs with definition of gds numbers for layers
     */
    protected void newFoundry(Foundry.Type mode, URL fileURL, String... gdsLayers) {
        Foundry foundry = new Foundry(this, mode, fileURL, gdsLayers);
        foundries.add(foundry);
    }

	/**
	 * Method to get the foundry index associated with this technology.
	 * @return the foundry index associated with this technology.
	 */
    public Foundry getSelectedFoundry()
    {
        String foundryName = getPrefFoundry();
        Foundry f = findFoundry(foundryName);
        if (f != null) return f;
        if (foundries.size() > 0)
        {
            f = foundries.get(0);
            System.out.println("Foundry '" + foundryName + "' not available in Technology '" +  this.getTechName() +
            "'. Setting '" + f.toString() + "' as foundry.");
            return f;
        }
        return f;
    }

    /**
     * Method to return the map from Layers of this Technology to their GDS names in current foundry.
     * Only Layers with non-empty GDS names are present in the map
     * @return the map from Layers to GDS names
     */
    public Map<Layer,String> getGDSLayers() {
        Foundry foundry = getSelectedFoundry();
        Map<Layer,String> gdsLayers = Collections.emptyMap();
        if (foundry != null) gdsLayers = foundry.getGDSLayers();
        return gdsLayers;
    }

	/**
	 * Sets the color map for transparent layers in this technology.
	 * Users should never call this method.
	 * It is set once by the technology during initialization.
	 * @param layers is an array of colors, one per transparent layer.
	 * This is expanded to a map that is 2 to the power "getNumTransparentLayers()".
	 * Color merging is computed automatically.
	 */
	protected void setFactoryTransparentLayers(Color [] layers)
	{
		// pull these values from preferences
		transparentLayers = layers.length;
		transparentColorPrefs = new Pref[transparentLayers];
		for(int i=0; i<layers.length; i++)
		{
			transparentColorPrefs[i] = Pref.makeIntPref("TransparentLayer"+(i+1)+"For"+getTechName(), prefs, layers[i].getRGB());
			layers[i] = new Color(transparentColorPrefs[i].getInt());
		}
		setColorMapFromLayers(layers);
	}

	/**
	 * Method to return the factory default colors for the transparent layers in this Technology.
	 * @return the factory default colors for the transparent layers in this Technology.
	 */
	public Color [] getFactoryTransparentLayerColors()
	{
		Color [] colors = new Color[transparentLayers];
		for(int i=0; i<transparentLayers; i++)
		{
			colors[i] = new Color(transparentColorPrefs[i].getIntFactoryValue());
		}
		return colors;
	}

	/**
	 * Method to return the colors for the transparent layers in this Technology.
	 * @return the factory for the transparent layers in this Technology.
	 */
	public Color [] getTransparentLayerColors()
	{
		Color [] colors = new Color[transparentLayers];
		for(int i=0; i<transparentLayers; i++)
		{
			colors[i] = new Color(transparentColorPrefs[i].getInt());
		}
		return colors;
	}

	/**
	 * Method to reload the color map when the layer color preferences have changed.
	 */
	public static void cacheTransparentLayerColors()
	{
        // recache technology color information
        for(Iterator<Technology> it = getTechnologies(); it.hasNext(); )
        {
            Technology tech = it.next();
            for(Iterator<Layer> lIt = tech.getLayers(); lIt.hasNext(); )
            {
                Layer layer = lIt.next();
                layer.getGraphics().recachePrefs();
            }

            if (tech.transparentColorPrefs == null || tech.transparentColorPrefs.length <= 0) continue;
            Color [] layers = new Color[tech.transparentColorPrefs.length];
            for(int i=0; i<tech.transparentColorPrefs.length; i++)
            {
                layers[i] = new Color(tech.transparentColorPrefs[i].getInt());
            }
            tech.setColorMapFromLayers(layers);
        }
	}

	public Color [] getFactoryColorMap()
	{
        if (transparentColorPrefs == null || transparentColorPrefs.length <= 0) return null;
        Color [] layers = new Color[transparentColorPrefs.length];
        for(int i=0; i<transparentColorPrefs.length; i++)
            layers[i] = new Color(transparentColorPrefs[i].getIntFactoryValue());
		Color [] map = getColorMap(layers, transparentColorPrefs.length);
		return map;
	}

	/**
	 * Returns the number of transparent layers in this technology.
	 * Informs the display system of the number of overlapping or transparent layers
	 * in use.
	 * @return the number of transparent layers in this technology.
	 * There may be 0 transparent layers in technologies that don't do overlapping,
	 * such as Schematics.
	 */
	public int getNumTransparentLayers() { return transparentLayers; }

	/**
	 * Sets the number of transparent layers in this technology.
	 * @param nl the number of transparent layers in this technology.
	 */
	public void setNumTransparentLayers(int nl) { transparentLayers = nl; }

	/**
	 * Sets the color map for transparent layers in this technology.
	 * @param map the color map for transparent layers in this technology.
	 * There must be a number of entries in this map equal to 2 to the power "getNumTransparentLayers()".
	 */
	public void setColorMap(Color [] map)
	{
		colorMap = map;
	}

	/**
	 * Sets the color map from transparent layers in this technology.
	 * @param layers an array of colors, one per transparent layer.
	 * This is expanded to a map that is 2 to the power "getNumTransparentLayers()".
	 * Color merging is computed automatically.
	 */
	public void setColorMapFromLayers(Color [] layers)
	{
		// update preferences
		if (transparentColorPrefs != null)
		{
			for(int i=0; i<layers.length; i++)
			{
				Pref pref = transparentColorPrefs[i];
                if (layers[i] != null)
				    pref.setInt(layers[i].getRGB());
			}
		}
		Color [] map = getColorMap(layers, transparentLayers);
		setColorMap(map);
	}

	public static Color [] getColorMap(Color [] layers, int numLayers)
	{
		int numEntries = 1 << numLayers;
		Color [] map = new Color[numEntries];
		for(int i=0; i<numEntries; i++)
		{
			int r=200, g=200, b=200;
			boolean hasPrevious = false;
			for(int j=0; j<numLayers; j++)
			{
				if ((i & (1<<j)) == 0) continue;
				if (hasPrevious)
				{
					// get the previous color
					double [] lastColor = new double[3];
					lastColor[0] = r / 255.0;
					lastColor[1] = g / 255.0;
					lastColor[2] = b / 255.0;
					normalizeColor(lastColor);

					// get the current color
					double [] curColor = new double[3];
					curColor[0] = layers[j].getRed() / 255.0;
					curColor[1] = layers[j].getGreen() / 255.0;
					curColor[2] = layers[j].getBlue() / 255.0;
					normalizeColor(curColor);

					// combine them
					for(int k=0; k<3; k++) curColor[k] += lastColor[k];
					normalizeColor(curColor);
					r = (int)(curColor[0] * 255.0);
					g = (int)(curColor[1] * 255.0);
					b = (int)(curColor[2] * 255.0);
				} else
				{
					r = layers[j].getRed();
					g = layers[j].getGreen();
					b = layers[j].getBlue();
					hasPrevious = true;
				}
			}
			map[i] = new Color(r, g, b);
		}
		return map;
	}

	/**
	 * Method to normalize a color stored in a 3-long array.
	 * @param a the array of 3 doubles that holds the color.
	 * All values range from 0 to 1.
	 * The values are adjusted so that they are normalized.
	 */
	private static void normalizeColor(double [] a)
	{
		double mag = Math.sqrt(a[0] * a[0] + a[1] * a[1] + a[2] * a[2]);
		if (mag > 1.0e-11f)
		{
			a[0] /= mag;
			a[1] /= mag;
			a[2] /= mag;
		}
	}

	/**
	 * Method to get the factory design rules.
	 * Individual technologies subclass this to create their own rules.
	 * @return the design rules for this Technology.
	 * Returns null if there are no design rules in this Technology.
     */
    public XMLRules getFactoryDesignRules() {
        XMLRules rules = new XMLRules(this);

        Foundry foundry = getSelectedFoundry();
        List<DRCTemplate> rulesList = foundry.getRules();
        boolean pWellProcess = User.isPWellProcessLayoutTechnology();

        // load the DRC tables from the explanation table
        if (rulesList != null) {
            for(DRCTemplate rule : rulesList)
            {
                if (rule.ruleType != DRCTemplate.DRCRuleType.NODSIZ)
                    rules.loadDRCRules(this, foundry, rule, pWellProcess);
            }
            for(DRCTemplate rule : rulesList)
            {
                if (rule.ruleType == DRCTemplate.DRCRuleType.NODSIZ)
                    rules.loadDRCRules(this, foundry, rule, pWellProcess);
            }
        }

        resizeArcs(rules);

        if (xmlTech != null)
            resizeXml(rules);
        return rules;
    }

    protected void resizeArcs(XMLRules rules) {
        TechDistanceContext context = new TechDistanceContext(rules);
        for (ArcProto ap: arcs.values())
            ap.resize(context);
        for (Layer layer: layers)
            layer.resizePureLayerNode(context);
        for (PrimitiveNode pn: nodes.values())
            pn.resize(context);
    }

    private class TechDistanceContext implements DistanceContext {
        private final Map<String,String> ruleAliases = getRuleAliases();
        private final XMLRules rules;
//        private final int numMetals = getNumMetals();

        TechDistanceContext(XMLRules rules) {
            this.rules = rules;
        }

        public double getRule(String ruleName) {
            String alias = ruleAliases.get(ruleName);
            if (alias != null)
                ruleName = alias;
            for (HashMap<XMLRules.XMLRule,XMLRules.XMLRule> map: rules.matrix) {
                if (map == null) continue;
                for (XMLRules.XMLRule rule: map.values()) {
                    if (rule.ruleType == DRCTemplate.DRCRuleType.NODSIZ) continue;
                    if (rule.ruleType == DRCTemplate.DRCRuleType.MINWIDCOND) continue;

                    if (rule.ruleName.startsWith(ruleName + " ") || rule.ruleName.equals(ruleName))
                        return rule.getValue(0);
                }
            }
            return Double.NaN;
        }
    }

    protected String getRuleSuffix() {
        return "";
    }

    protected Map<String,String> getRuleAliases() {
        return Collections.EMPTY_MAP;
    }

	/**
	 * Method to compare a Rules set with the "factory" set and construct an override string.
	 * @param origRules
	 * @param newRules
	 * @return a StringBuffer that describes any overrides.  Returns "" if there are none.
	 */
	public static StringBuffer getRuleDifferences(DRCRules origRules, DRCRules newRules)
	{
		return (new StringBuffer(""));
	}

	/**
	 * Method to be called from DRC:setRules
	 * @param newRules
	 */
	public void setRuleVariables(DRCRules newRules) {}

	/**
	 * Returns the color map for transparent layers in this technology.
	 * @return the color map for transparent layers in this technology.
	 * The number of entries in this map equals 2 to the power "getNumTransparentLayers()".
	 */
	public Color [] getColorMap() { return colorMap; }

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
//	private static boolean validTechnology(String techName)
//	{
//		if (Technology.findTechnology(techName) != null)
//		{
//			System.out.println("ERROR: Multiple technologies named " + techName);
//			return false;
//		}
//		return true;
//	}

	/**
	 * Method to determine the appropriate Technology to use for a Cell.
	 * @param cell the Cell to examine.
	 * @return the Technology for that cell.
	 */
	public static Technology whatTechnology(NodeProto cell)
	{
		Technology tech = whatTechnology(cell, null, 0, 0, null);
		return tech;
	}

	/**
	 * Method to determine the appropriate technology to use for a cell.
	 * The contents of the cell can be defined by the lists of NodeInsts and ArcInsts, or
	 * if they are null, then by the contents of the Cell.
	 * @param cellOrPrim the Cell to examine.
	 * @param nodeProtoList the list of prototypes of NodeInsts in the Cell.
	 * @param startNodeProto the starting point in the "nodeProtoList" array.
	 * @param endNodeProto the ending point in the "nodeProtoList" array.
	 * @param arcProtoList the list of prototypes of ArcInsts in the Cell.
	 * @return the Technology for that cell.
	 */
	public static Technology whatTechnology(NodeProto cellOrPrim, NodeProto [] nodeProtoList, int startNodeProto, int endNodeProto,
		ArcProto [] arcProtoList)
	{
		// primitives know their technology
		if (cellOrPrim instanceof PrimitiveNode)
			return(((PrimitiveNode)cellOrPrim).getTechnology());
		Cell cell = (Cell)cellOrPrim;

		// count the number of technologies
		int maxTech = 0;
		for(Iterator<Technology> it = Technology.getTechnologies(); it.hasNext(); )
		{
			Technology tech = it.next();
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
				if (np instanceof Cell)
				{
					Cell subCell = (Cell)np;
					if (subCell.isIcon())
						nodeTech = Schematics.tech();
				}
				if (nodeTech != null) useCount[nodeTech.getIndex()]++;
			}
		} else
		{
			for(Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
			{
				NodeInst ni = it.next();
				NodeProto np = ni.getProto();
				Technology nodeTech = np.getTechnology();
				if (ni.isCellInstance())
				{
					Cell subCell = (Cell)np;
					if (subCell.isIcon())
						nodeTech = Schematics.tech();
				}
				if (nodeTech != null) useCount[nodeTech.getIndex()]++;
			}
		}

		// count technologies of all arcs in the cell
		if (arcProtoList != null)
		{
			// iterate over the arcprotos in the list
			for(ArcProto ap: arcProtoList)
			{
				if (ap == null) continue;
				useCount[ap.getTechnology().getIndex()]++;
			}
		} else
		{
			for(Iterator<ArcInst> it = cell.getArcs(); it.hasNext(); )
			{
				ArcInst ai = it.next();
				ArcProto ap = ai.getProto();
				useCount[ap.getTechnology().getIndex()]++;
			}
		}

		// find a concensus
		int best = 0;         Technology bestTech = null;
		int bestLayout = 0;   Technology bestLayoutTech = null;
		for(Iterator<Technology> it = Technology.getTechnologies(); it.hasNext(); )
		{
			Technology tech = it.next();

			// always ignore the generic technology
			if (tech instanceof Generic) continue;

			// find the most popular of ALL technologies
			if (useCount[tech.getIndex()] > best)
			{
				best = useCount[tech.getIndex()];
				bestTech = tech;
			}

			// find the most popular of the layout technologies
			if (!tech.isLayout()) continue;
			if (useCount[tech.getIndex()] > bestLayout)
			{
				bestLayout = useCount[tech.getIndex()];
				bestLayoutTech = tech;
			}
		}

		Technology retTech = null;
		if (cell.isIcon() || cell.getView().isTextView())
		{
			// in icons, if there is any artwork, use it
			if (useCount[Artwork.tech().getIndex()] > 0) return(Artwork.tech());

			// in icons, if there is nothing, presume artwork
			if (bestTech == null) return(Artwork.tech());

			// use artwork as a default
			retTech = Artwork.tech();
		} else if (cell.isSchematic())
		{
			// in schematic, if there are any schematic components, use it
			if (useCount[Schematics.tech().getIndex()] > 0) return(Schematics.tech());

			// in schematic, if there is nothing, presume schematic
			if (bestTech == null) return(Schematics.tech());

			// use schematic as a default
			retTech = Schematics.tech();
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
     * Returns true if this Technology is a layout technology.
     * @return true if this Technology is a layout technology.
     */
    public boolean isLayout() {
        return !(this instanceof Artwork || this instanceof EFIDO ||
        	this instanceof GEM || this instanceof Generic || this instanceof Schematics);
    }

    /**
     * Returns true if this Technology is a schematics technology.
     * @return true if this Technology is a schematics technology.
     */
    public boolean isSchematics() {
        return this instanceof Schematics || this instanceof EFIDO || this instanceof GEM;
    }

    /**
     * Compares Technologies by their names.
     * @param that the other Technology.
     * @return a comparison between the Technologies.
     */
	public int compareTo(Technology that)
	{
		return TextUtils.STRING_NUMBER_ORDER.compare(getTechName(), that.getTechName());
	}

	/**
	 * Returns a printable version of this Technology.
	 * @return a printable version of this Technology.
	 */
	public String toString()
	{
		return "Technology " + getTechName();
	}

   /**
     * Method to check invariants in this Technology.
     * @exception AssertionError if invariants are not valid
     */
    private void check() {
        for (ArcProto ap: arcs.values()) {
            ap.check();
        }
    }

	///////////////////// Generic methods //////////////////////////////////////////////////////////////

//	/**
//	 * Method to change the design rules for layer "layername" layers so that
//	 * the layers are at least "width" wide.  Affects the default arc width
//	 * and the default pin size.
//	 */
//	protected void setLayerMinWidth(String layername, String rulename, double width)
//	{
//		// find the arc and set its default width
//		ArcProto ap = findArcProto(layername);
//		if (ap == null) return;
//
//		boolean hasChanged = false;
//
//        if (ap.getDefaultLambdaBaseWidth() != width)
////        if (ap.getDefaultLambdaFullWidth() != width + ap.getLambdaWidthOffset())
//            hasChanged = true;
//
//		// find the arc's pin and set its size and port offset
//		PrimitiveNode np = ap.findPinProto();
//		if (np == null) return;
//		SizeOffset so = np.getProtoSizeOffset();
//		double newWidth = width + so.getLowXOffset() + so.getHighXOffset();
//		double newHeight = width + so.getLowYOffset() + so.getHighYOffset();
//
//        if (np.getDefHeight() != newHeight || np.getDefWidth() != newWidth)
//            hasChanged = true;
//
//		PrimitivePort pp = (PrimitivePort)np.getPorts().next();
//		EdgeH left = pp.getLeft();
//		EdgeH right = pp.getRight();
//		EdgeV bottom = pp.getBottom();
//		EdgeV top = pp.getTop();
//		double indent = newWidth / 2;
//
//        if (left.getAdder() != indent || right.getAdder() != -indent ||
//            top.getAdder() != -indent || bottom.getAdder() != indent)
//            hasChanged = true;
//		if (hasChanged)
//		{
//			// describe the error
//            String errorMessage = "User preference of " + width + " overwrites original layer minimum size in layer '"
//					+ layername + "', primitive '" + np.getName() + ":" + getTechShortName() + "' by rule " + rulename;
//			if (Job.LOCALDEBUGFLAG) System.out.println(errorMessage);
//		}
//	}
//
//    protected void setDefNodeSize(PrimitiveNode nty, double wid, double hei)
//    {
//        double xindent = (nty.getDefWidth() - wid) / 2;
//		double yindent = (nty.getDefHeight() - hei) / 2;
//		nty.setSizeOffset(new SizeOffset(xindent, xindent, yindent, yindent));  // bug 1040
//    }

	/**
	 * Method to set the surround distance of layer "layer" from the via in node "nodename" to "surround".
	 */
//	protected void setLayerSurroundVia(PrimitiveNode nty, Layer layer, double surround)
//	{
//		// find the via size
//		double [] specialValues = nty.getSpecialValues();
//		double viasize = specialValues[0];
//		double layersize = viasize + surround*2;
//		double indent = (nty.getDefWidth() - layersize) / 2;
//
//		Technology.NodeLayer oneLayer = nty.findNodeLayer(layer, false);
//		if (oneLayer != null)
//		{
//			TechPoint [] points = oneLayer.getPoints();
//			EdgeH left = points[0].getX();
//			EdgeH right = points[1].getX();
//			EdgeV bottom = points[0].getY();
//			EdgeV top = points[1].getY();
//			left.setAdder(indent);
//			right.setAdder(-indent);
//			top.setAdder(-indent);
//			bottom.setAdder(indent);
//		}
//	}

	/********************* FOR GUI **********************/

	/** Temporary variable for holding names */		public static final Variable.Key TECH_TMPVAR = Variable.newKey("TECH_TMPVAR");

	/**
	 * Method to change the group of elements for the component menu.
	 * @param ng the new set of objects to display in the component menu.
	 * @param xml the XML for the new component menu groupings.
	 */
	public void setNodesGrouped(Object[][] ng, String xml)
	{
		nodeGroups = ng;
		if (componentMenuPref == null)
			componentMenuPref = Pref.makeStringPref("ComponentMenuXMLfor"+getTechName(), prefs, "");
		componentMenuPref.setString(xml);
	}

	/**
	 * Method to get the group of elements for the component menu.
	 * @return the XML for the new component menu groupings.
	 */
	public String getNodesGroupedXML()
	{
		if (componentMenuPref == null)
			componentMenuPref = Pref.makeStringPref("ComponentMenuXMLfor"+getTechName(), prefs, "");
		return componentMenuPref.getString();
	}

	/**
	 * Method to see if there are component menu preferences.
	 * If such preferences exist, the field variable "nodeGroups"
	 * is loaded with that menu.
	 */
	public void getPrefComponentMenu()
	{
		// if component menu is already defined, stop now
		if (nodeGroups != null) return;

		// see if there is a preference for the component menu
		String nodeGroupXML = getNodesGroupedXML();
		if (nodeGroupXML == null) return;
		if (nodeGroupXML.length() == 0) return;

		// parse the preference and build a component menu
        List<Xml.PrimitiveNode> xmlNodes = new ArrayList<Xml.PrimitiveNode>();
        for (PrimitiveNode pn: nodes.values()) {
            Xml.PrimitiveNode xpn = new Xml.PrimitiveNode();
            xpn.name = pn.getName();
            xmlNodes.add(xpn);
        }
        List<Xml.ArcProto> xmlArcs = new ArrayList<Xml.ArcProto>();
        for (ArcProto ap: arcs.values()) {
            Xml.ArcProto xap = new Xml.ArcProto();
            xap.name = ap.getName();
            xmlArcs.add(xap);
        }
		Xml.MenuPalette xx = Xml.parseComponentMenuXMLTechEdit(nodeGroupXML, xmlNodes, xmlArcs);
        convertMenuPalette(xx);
	}

	/**
	 * Method to construct a default group of elements for the palette.
	 * @return the default set of objects to display in the component menu.
	 */
	public Object[][] getDefaultNodesGrouped()
	{
		if (factoryNodeGroups != null) return factoryNodeGroups;

		// compute palette information automatically
		List<Object> things = new ArrayList<Object>();
		for(Iterator<ArcProto> it = getArcs(); it.hasNext(); )
		{
			ArcProto ap = it.next();
			if (!ap.isNotUsed()) things.add(ap);
		}
		for(Iterator<PrimitiveNode> it = getNodes(); it.hasNext(); )
		{
			PrimitiveNode np = it.next();
			if (np.isNotUsed()) continue;
			if (np.getFunction() == PrimitiveNode.Function.NODE) continue;
			things.add(np);
		}
		things.add(SPECIALMENUPURE);
		things.add(SPECIALMENUMISC);
		things.add(SPECIALMENUCELL);
		int columns = (things.size()+13) / 14;
		int rows = (things.size() + columns-1) / columns;
		factoryNodeGroups = new Object[rows][columns];
		int rowPos = 0, colPos = 0;
		for(Object obj : things)
		{
			factoryNodeGroups[rowPos][colPos] = obj;
			rowPos++;
			if (rowPos >= rows)
			{
				rowPos = 0;
				colPos++;
			}
		}
		return factoryNodeGroups;
	}

	/**
	 * Method to retrieve correct group of elements for the palette.
	 * @param curCell the current cell being displayed (may affect the palette).
	 * @return the new set of objects to display in the component menu.
	 */
	public Object[][] getNodesGrouped(Cell curCell)
	{
		// make sure any preferences are applied
		getPrefComponentMenu();

		// if there are no preferences, setup default
		if (nodeGroups == null) nodeGroups = getDefaultNodesGrouped();
		return filterNodeGroups(nodeGroups);
	}

	/**
	 * Method to remove component menu entries that are impossible
	 * because of inaccessible objects.
	 * @param oldNG the old list of component menu entries.
	 * @return a filtered list of component menu entries.
	 */
	public Object[][] filterNodeGroups(Object [][] oldNG)
	{
		// Check if some metal layers are not used
		List <Object>list = new ArrayList<Object>(oldNG.length);
		for (int i = 0; i < oldNG.length; i++)
		{
			Object[] objs = oldNG[i];
			if (objs != null)
			{
				Object obj = objs[0];
				boolean valid = true;
				if (obj instanceof ArcProto)
				{
					ArcProto ap = (ArcProto)obj;
					valid = !ap.isNotUsed();
				}
				if (valid)
					list.add(objs);
			}
		}
		Object[][] newMatrix = new Object[list.size()][oldNG[0].length];
		for (int i = 0; i < list.size(); i++)
		{
			Object[] objs = (Object[])list.get(i);
			for (int j = 0; j < objs.length; j++)
			{
				Object obj = objs[j];
				// Element is not used or first element in list is not used
				if ((obj instanceof PrimitiveNode && ((PrimitiveNode)obj).isNotUsed()))
					obj = null;
				else if (obj instanceof List)
				{
					List<?> l = (List)obj;
					Object o = l.get(0);
					if (o instanceof NodeInst)
					{
						NodeInst ni = (NodeInst)o;
						if (!ni.isCellInstance() && ((PrimitiveNode)ni.getProto()).isNotUsed())
							obj = null;
					}
					else if (o instanceof PrimitiveNode)
					{
						if (((PrimitiveNode)o).isNotUsed())
							obj = null;
					}
				}
				newMatrix[i][j] = obj;
			}
		}
		return newMatrix;
	}

    /**
     * Method to create temporary nodes for the palette
     * @param np prototype of the node to place in the palette.
     */
    public static NodeInst makeNodeInst(NodeProto np) {
        return makeNodeInst(np, np.getFunction(), 0, false, null, 0);
    }

    /**
     * Method to create temporary nodes for the palette
     * @param np prototype of the node to place in the palette.
     * @param func function of the node (helps parameterize the node).
     * @param angle initial placement angle of the node.
     */
    public static NodeInst makeNodeInst(NodeProto np, PrimitiveNode.Function func, int angle, boolean display,
                                        String varName, double fontSize)
    {
        SizeOffset so = np.getProtoSizeOffset();
        Point2D pt = new Point2D.Double((so.getHighXOffset() - so.getLowXOffset()) / 2,
            (so.getHighYOffset() - so.getLowYOffset()) / 2);
		Orientation orient = Orientation.fromAngle(angle);
		AffineTransform trans = orient.pureRotate();
        trans.transform(pt, pt);
        NodeInst ni = NodeInst.makeDummyInstance(np, new EPoint(pt.getX(), pt.getY()), np.getDefWidth(), np.getDefHeight(), orient);
        np.getTechnology().setPrimitiveFunction(ni, func);
        np.getTechnology().setDefaultOutline(ni);

	    if (varName != null)
	    {
	    	TextDescriptor td = TextDescriptor.getNodeTextDescriptor().withDisplay(display).withRelSize(fontSize);
	    	td = td.withOff(0, -Math.max(ni.getXSize(), ni.getYSize())/2-2).withPos(TextDescriptor.Position.UP);
	    	if (angle != 0) td = td.withRotation(TextDescriptor.Rotation.getRotation(360-angle/10));
            ni.newVar(TECH_TMPVAR, varName, td);
	    }

        return ni;
    }

    /**
     * This is the most basic function to determine the widest wire and the parallel distance
     * that run along them. Done because MOSRules doesn't consider the parallel distance as input.
     */
    public double[] getSpacingDistances(Poly poly1, Poly poly2)
    {
        double size1 = poly1.getMinSize();
        double size2 = poly1.getMinSize();
        double length = 0;
        double wideS = (size1 > size2) ? size1 : size2;
        double [] results = new double[2];
        results[0] = wideS;
        results[1] = length;
        return results;
    }

    /**
     * Method to retrieve cached rules
     * @return cached design rules.
     */
    public DRCRules getCachedRules() {return cachedRules;}

    /**
     * Method to set cached rules
     */
    public void setCachedRules(DRCRules rules) {cachedRules = rules;}

    /**
     * Method to determine if the rule name matches an existing VT Poly rule
     * @param theRule
     * @return true if it matches
     */
    public boolean isValidVTPolyRule(DRCTemplate theRule) {return false;}

//    /**
//     * Utility function to copy NodeLayers from existing PrimitiveNodes into new ones.
//     * @param source
//     * @param destination
//     * @param identifier
//     */
//    public void copyPrimitives(PrimitiveNode[] source, PrimitiveNode[] destination, String identifier)
//    {
//        for (int i = 0; i < source.length; i++)
//        {
//            PrimitiveNode metal = source[i];
//            Technology.NodeLayer [] layers = metal.getLayers();
//            Technology.NodeLayer [] nodes = new Technology.NodeLayer [layers.length];
//            for (int j = 0; j < layers.length; j++)
//            {
//                Technology.NodeLayer node = layers[j];
//                nodes[j] = new Technology.NodeLayer(node);
//            }
//            destination[i] = PrimitiveNode.newInstance(identifier+"-"+metal.getName(), this,
//                    metal.getDefWidth(), metal.getDefHeight(), null, nodes);
//            PrimitivePort port = metal.getPrimitivePorts().next(); // only 1 port
//            ArcProto[] arcs = port.getConnections();
//            ArcProto[] newArcs = new ArcProto[arcs.length];
//            System.arraycopy(arcs, 0, newArcs, 0, arcs.length);
//            destination[i].addPrimitivePorts(new PrimitivePort []
//                {
//                    PrimitivePort.newInstance(this, destination[i], newArcs, port.getName(), port.getAngle(),
//                            port.getAngleRange(), port.getTopology(), port.getCharacteristic(),
//                            EdgeH.fromCenter(0), EdgeV.fromCenter(0), EdgeH.fromCenter(0), EdgeV.fromCenter(0))
//                });
//            destination[i].setFunction(metal.getFunction());
//            destination[i].setSpecialType(metal.getSpecialType());
//            destination[i].setDefSize(0, 0);   // so it won't resize against any User's default? tricky
//            PrimitiveNode.NodeSizeRule minRuleSize = source[i].getMinSizeRule();
//            destination[i].setMinSize(minRuleSize.getWidth(), minRuleSize.getHeight(), minRuleSize.getRuleName());
//        }
//    }

    /**
	 * Class to extend prefs so that changes to MOSIS CMOS options will update the display.
	 */
	public static class TechSetting extends Setting
	{
        private Technology tech;

        private TechSetting(String prefName, Pref.Group group, Technology tech, ProjSettingsNode xmlNode, String xmlName, String location, String description, Object factoryObj) {
            super(prefName, Technology.prefs, xmlNode, xmlName, location, description, factoryObj);
            if (tech == null)
                throw new NullPointerException();
            this.tech = tech;
        }

        @Override
		protected void setSideEffect()
		{
			//technologyChangedFromDatabase(tech, true);
            if (tech == null) return;
            if (isUsed(tech)) {
                Job.getUserInterface().showInformationMessage("There is now inconsistent use of this technology parameter:\n" +
                	"               " + getDescription() + "\n" +
                    "Electric cannot handle this situation and errors may result.\n" +
                    "It is recommended that you restart Electric to avoid this instability.\n" +
                    "In the future, when the 'Project Settings Reconciliation' dialog appears,\n" +
                    "Click 'Use All Current Settings' or select the CURRENT VALUE of this parameter.",
                    "Technology Parameter Changed");
            }
            tech.setState();
            reloadUIData();
		}

        private boolean isUsed(Technology tech) {
            for (Iterator<Library> lit = Library.getLibraries(); lit.hasNext(); ) {
                Library lib = lit.next();
                for (Iterator<Cell> cit = lib.getCells(); cit.hasNext(); ) {
                    Cell cell = cit.next();
                    if (cell.getTechnology() != tech) continue;
                    for (Iterator<NodeInst> nit = cell.getNodes(); nit.hasNext(); ) {
                        NodeInst ni = nit.next();
                        if (ni.isCellInstance()) continue;
                        if (((PrimitiveNode)ni.getProto()).getTechnology() == tech) return true;
                    }
                    for (Iterator<ArcInst> ait = cell.getArcs(); ait.hasNext(); ) {
                        ArcInst ai = ait.next();
                        if (ai.getProto().getTechnology() == tech) return true;
                    }
                }
            }
            return false;
        }

        private static void reloadUIData()
        {
			SwingUtilities.invokeLater(new Runnable()
            {
	            public void run()
                {
                // Primitives cached must be redrawn
                // recache display information for all cells that use this
                    User.technologyChanged();
                    UserInterface ui = Job.getUserInterface();
                    ui.repaintAllWindows();
                }
            });
        }

		public static Setting makeBooleanSetting(Technology tech, String name, String location, String description,
                                              ProjSettingsNode xmlNode, String xmlName,
                                              boolean factory)
		{
            Setting setting = Setting.getSetting(xmlNode.getPath() + xmlName);
            if (setting != null) return setting;
            return new TechSetting(name, Technology.prefs, tech, xmlNode, xmlName, location, description, Boolean.valueOf(factory));
		}

		public static Setting makeIntSetting(Technology tech, String name, String location, String description,
                                          ProjSettingsNode xmlNode, String xmlName,
                                          int factory)
		{
            Setting setting = Setting.getSetting(xmlNode.getPath() + xmlName);
            if (setting != null) return setting;
            return new TechSetting(name, Technology.prefs, tech, xmlNode, xmlName, location, description, Integer.valueOf(factory));
		}

        public static Setting makeStringSetting(Technology tech, String name, String location, String description,
                                             ProjSettingsNode xmlNode, String xmlName,
                                             String factory)
		{
            Setting setting = Setting.getSetting(xmlNode.getPath() + xmlName);
            if (setting != null) return setting;
            return new TechSetting(name, Technology.prefs, tech, xmlNode, xmlName, location, description, factory);
		}
	}

    // -------------------------- Project Settings -------------------------

    public ProjSettingsNode getProjectSettings() {
        ProjSettingsNode node = ProjSettings.getSettings().getNode(getTechName());
//        if (node == null) {
//            node = new ProjSettingsNode();
//            ProjSettings.getSettings().putNode(getTechName(), node);
//        }
        return node;
    }
}
