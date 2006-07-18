/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ElectricObject.java
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
package com.sun.electric.database.variable;

import com.sun.electric.database.ImmutableElectricObject;
import com.sun.electric.database.geometry.GenMath;
import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.EDatabase;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.tool.user.ActivityLogger;
import com.sun.electric.tool.user.User;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * This class is the base class of all Electric objects that can be extended with "Variables".
 * <P>
 * This class should be thread-safe.
 */
public abstract class ElectricObject implements Serializable
{
	// ------------------------ private data ------------------------------------

	// ------------------------ private and protected methods -------------------

	/**
	 * The protected constructor.
	 */
	protected ElectricObject() {}

	/**
	 * Returns persistent data of this ElectricObject with Variables.
	 * @return persistent data of this ElectricObject.
	 */
	public abstract ImmutableElectricObject getImmutable();

	// ------------------------ public methods -------------------

	/**
	 * Returns true if object is linked into database
	 */
	public abstract boolean isLinked();

	/**
	 * Method to return the Variable on this ElectricObject with a given name.
	 * @param name the name of the Variable.
	 * @return the Variable with that name, or null if there is no such Variable.
	 */
	public Variable getVar(String name)
	{
		Variable.Key key = Variable.findKey(name);
		return getVar(key, null);
	}

	/**
	 * Method to return the Variable on this ElectricObject with a given key.
	 * @param key the key of the Variable.
	 * @return the Variable with that key, or null if there is no such Variable.
	 */
	public Variable getVar(Variable.Key key)
	{
		return getVar(key, null);
	}

	/**
	 * Method to return the Variable on this ElectricObject with a given name and type.
	 * @param name the name of the Variable.
	 * @param type the required type of the Variable.
	 * @return the Variable with that name and type, or null if there is no such Variable.
	 */
	public Variable getVar(String name, Class type)
	{
		Variable.Key key = Variable.findKey(name);
		return getVar(key, type);
	}

	/**
	 * Method to return the Variable on this ElectricObject with a given key and type.
	 * @param key the key of the Variable. Returns null if key is null.
	 * @param type the required type of the Variable. Ignored if null.
	 * @return the Variable with that key and type, or null if there is no such Variable
	 * or default Variable value.
	 */
	public Variable getVar(Variable.Key key, Class type)
	{
		checkExamine();
		if (key == null) return null;
		Variable var;
		synchronized(this) {
			var = getImmutable().getVar(key);
		}
		if (var != null) {
			if (type == null) return var;				   // null type means any type
			if (type.isInstance(var.getObject())) return var;
		}
		return null;
	}

	/**
	 * Returns the TextDescriptor on this ElectricObject selected by variable key.
	 * This key may be a key of variable on this ElectricObject or one of the
	 * special keys:
	 * <code>NodeInst.NODE_NAME</code>
	 * <code>NodeInst.NODE_PROTO</code>
	 * <code>ArcInst.ARC_NAME</code>
	 * <code>Export.EXPORT_NAME</code>
	 * The TextDescriptor gives information for displaying the Variable.
	 * @param varKey key of variable or special key.
	 * @return the TextDescriptor on this ElectricObject.
	 */
	public TextDescriptor getTextDescriptor(Variable.Key varKey)
	{
		Variable var = getVar(varKey);
		if (var == null) return null;
		return var.getTextDescriptor();
	}

 	/**
	 * Returns the TextDescriptor on this ElectricObject selected by variable key.
	 * This key may be a key of variable on this ElectricObject or one of the
	 * special keys:
	 * <code>NodeInst.NODE_NAME</code>
	 * <code>NodeInst.NODE_PROTO</code>
	 * <code>ArcInst.ARC_NAME</code>
	 * <code>Export.EXPORT_NAME</code>
	 * The TextDescriptor gives information for displaying the Variable.
	 * @param varKey key of variable or special key.
	 * @return the TextDescriptor on this ElectricObject.
	 */
	public MutableTextDescriptor getMutableTextDescriptor(Variable.Key varKey)
	{
		TextDescriptor td = getTextDescriptor(varKey);
		if (td == null) return null;
		return new MutableTextDescriptor(td);
	}

	/**
	 * Method to return true if the Variable on this ElectricObject with given key is a parameter.
	 * Parameters are those Variables that have values on instances which are
	 * passed down the hierarchy into the contents.
	 * Parameters can only exist on Cell and NodeInst objects.
	 * @param varKey key to test
	 * @return true if the Variable with given key is a parameter.
	 */
	public boolean isParam(Variable.Key varKey) { return false; }

	/**
	 * Method to return the number of displayable Variables on this ElectricObject.
	 * A displayable Variable is one that will be shown with its object.
	 * Displayable Variables can only sensibly exist on NodeInst and ArcInst objects.
	 * @return the number of displayable Variables on this ElectricObject.
	 */
	public int numDisplayableVariables(boolean multipleStrings)
	{
        //checkExamine();
		int numVars = 0;
		for (Iterator<Variable> it = getVariables(); it.hasNext(); )
		{
			Variable var = it.next();
			if (var.isDisplay())
			{
				int len = var.getLength();
				if (len > 1 && var.getTextDescriptor().getDispPart() == TextDescriptor.DispPos.NAMEVALUE) len++;
				if (!multipleStrings) len = 1;
				numVars += len;
			}
		}
		return numVars;
	}
	
	/**
	 * Method to add all displayable Variables on this Electric object to an array of Poly objects.
	 * @param rect a rectangle describing the bounds of the object on which the Variables will be displayed.
	 * @param polys an array of Poly objects that will be filled with the displayable Variables.
	 * @param start the starting index in the array of Poly objects to fill with displayable Variables.
	 * @param wnd window in which the Variables will be displayed.
	 * @param multipleStrings true to break multiline text into multiple Polys.
	 * @return the number of Polys that were added.
	 */
	public int addDisplayableVariables(Rectangle2D rect, Poly [] polys, int start, EditWindow0 wnd, boolean multipleStrings)
	{
		checkExamine();
		int numAddedVariables = 0;
		double cX = rect.getCenterX();
		double cY = rect.getCenterY();
		for (Iterator<Variable> it = getVariables(); it.hasNext(); )
		{
			Variable var = it.next();
			if (!var.isDisplay()) continue;
			Poly [] polyList = getPolyList(var, cX, cY, wnd, multipleStrings);
			for(int i=0; i<polyList.length; i++)
			{
				int index = start + numAddedVariables;
				polys[index] = polyList[i];
				polys[index].setStyle(Poly.rotateType(polys[index].getStyle(), this));
				numAddedVariables++;
			}
		}
		return numAddedVariables;
	}

	/**
	 * Method to compute a Poly that describes text.
	 * The text can be described by an ElectricObject (Exports or cell instance names).
	 * The text can be described by a node or arc name.
	 * The text can be described by a variable on an ElectricObject.
	 * @param wnd the EditWindow0 in which the text will be drawn.
	 * @param varKey the Variable.Key on the ElectricObject (may be null).
	 * @return a Poly that covers the text completely.
	 * Even though the Poly is scaled for a particular EditWindow,
	 * its coordinates are in object space, not screen space.
	 */
	public Poly computeTextPoly(EditWindow0 wnd, Variable.Key varKey)
	{
		checkExamine();
		Poly poly = null;
		if (varKey != null)
		{
			if (this instanceof Export)
			{
				Export pp = (Export)this;
				if (varKey == Export.EXPORT_NAME)
				{
					poly = pp.getNamePoly();
				} else
				{
					PortInst pi = pp.getOriginalPort();
					Rectangle2D bounds = pp.getNamePoly().getBounds2D();
					TextDescriptor td = pp.getTextDescriptor(Export.EXPORT_NAME);
					Poly [] polys = pp.getPolyList(pp.getVar(varKey), bounds.getCenterX(), bounds.getCenterY(), wnd, false);
					if (polys.length > 0)
					{
						poly = polys[0];
//						poly.transform(pi.getNodeInst().rotateOut());
					}
				}
			} else if (this instanceof PortInst)
			{
				PortInst pi = (PortInst)this;
				Rectangle2D bounds = pi.getPoly().getBounds2D();
				Poly [] polys = pi.getPolyList(pi.getVar(varKey), bounds.getCenterX(), bounds.getCenterY(), wnd, false);
				if (polys.length > 0)
				{
					poly = polys[0];
					poly.transform(pi.getNodeInst().rotateOut());
				}
			} else if (this instanceof Geometric)
			{
				Geometric geom = (Geometric)this;
				if (varKey == NodeInst.NODE_NAME || varKey == ArcInst.ARC_NAME)
				{
					TextDescriptor td = geom.getTextDescriptor(varKey);
					Poly.Type style = td.getPos().getPolyType();
					Point2D [] pointList = null;
					if (style == Poly.Type.TEXTBOX)
					{
						pointList = Poly.makePoints(geom.getBounds());
					} else
					{
						pointList = new Point2D.Double[1];
						pointList[0] = new Point2D.Double(geom.getTrueCenterX()+td.getXOff(), geom.getTrueCenterY()+td.getYOff());
					}
					poly = new Poly(pointList);
					poly.setStyle(style);
					if (geom instanceof NodeInst)
					{
						poly.transform(((NodeInst)geom).rotateOutAboutTrueCenter());
					}
					poly.setTextDescriptor(td);
					if (varKey == NodeInst.NODE_NAME) poly.setString(((NodeInst)geom).getName()); else
						poly.setString(((ArcInst)geom).getName());
				} else if (varKey == NodeInst.NODE_PROTO)
				{
					if (!(geom instanceof NodeInst)) return null;
					NodeInst ni = (NodeInst)this;
					TextDescriptor td = ni.getTextDescriptor(NodeInst.NODE_PROTO);
					Poly.Type style = td.getPos().getPolyType();
					Point2D [] pointList = null;
					if (style == Poly.Type.TEXTBOX)
					{
						pointList = Poly.makePoints(ni.getBounds());
					} else
					{
						pointList = new Point2D.Double[1];
						pointList[0] = new Point2D.Double(ni.getTrueCenterX()+td.getXOff(), ni.getTrueCenterY()+td.getYOff());
					}
					poly = new Poly(pointList);
					poly.setStyle(style);
					poly.setTextDescriptor(td);
					poly.setString(ni.getProto().describe(false));
				} else
				{
					double x = geom.getTrueCenterX();
					double y = geom.getTrueCenterY();
					if (geom instanceof NodeInst)
					{
						NodeInst ni = (NodeInst)geom;
						Rectangle2D uBounds = ni.getUntransformedBounds();
						x = uBounds.getCenterX();
						y = uBounds.getCenterY();
					}
					Poly [] polys = geom.getPolyList(geom.getVar(varKey), x, y, wnd, false);
					if (polys.length > 0)
					{
						poly = polys[0];
						if (geom instanceof NodeInst)
						{
							NodeInst ni = (NodeInst)geom;
							poly.transform(ni.rotateOut());
						}
					}
				}
			} else if (this instanceof Cell)
			{
				Cell cell = (Cell)this;
				Poly [] polys = cell.getPolyList(cell.getVar(varKey), 0, 0, wnd, false);
				if (polys.length > 0) poly = polys[0];
			}
		}
		if (poly != null)
			poly.setExactTextBounds(wnd, this);
		return poly;
	}

	/**
	 * Method to return the bounds of this ElectricObject in an EditWindow.
	 * @param wnd the EditWindow0 in which the object is being displayed.
	 * @return the bounds of the text (does not include the bounds of the object).
	 */
	public Rectangle2D getTextBounds(EditWindow0 wnd)
	{
		Rectangle2D bounds = null;
		for(Iterator<Variable> vIt = getVariables(); vIt.hasNext(); )
		{
			Variable var = vIt.next();
			if (!var.isDisplay()) continue;
			TextDescriptor td = var.getTextDescriptor();
//			if (td.getSize().isAbsolute()) continue;
			Poly poly = computeTextPoly(wnd, var.getKey());
			if (poly == null) continue;
			Rectangle2D polyBound = poly.getBounds2D();
			if (bounds == null) bounds = polyBound; else
				Rectangle2D.union(bounds, polyBound, bounds);
		}

		if (this instanceof ArcInst)
		{
			ArcInst ai = (ArcInst)this;
			Name name = ai.getNameKey();
			if (!name.isTempname())
			{
				Poly poly = computeTextPoly(wnd, ArcInst.ARC_NAME);
				if (poly != null)
				{
					Rectangle2D polyBound = poly.getBounds2D();
					if (bounds == null) bounds = polyBound; else
						Rectangle2D.union(bounds, polyBound, bounds);
				}
			}
		}
		if (this instanceof NodeInst)
		{
			NodeInst ni = (NodeInst)this;
			Name name = ni.getNameKey();
			if (!name.isTempname())
			{
				Poly poly = computeTextPoly(wnd, NodeInst.NODE_NAME);
				if (poly != null)
				{
					Rectangle2D polyBound = poly.getBounds2D();
					if (bounds == null) bounds = polyBound; else
						Rectangle2D.union(bounds, polyBound, bounds);
				}
			}
			for(Iterator<Export> it = ni.getExports(); it.hasNext(); )
			{
				Export pp = it.next();
				Poly poly = pp.computeTextPoly(wnd, Export.EXPORT_NAME);
				if (poly != null)
				{
					Rectangle2D polyBound = poly.getBounds2D();
					if (bounds == null) bounds = polyBound; else
						Rectangle2D.union(bounds, polyBound, bounds);
				}
			}
		}
		return bounds;
	}

	/**
	 * Method to create an array of Poly objects that describes a displayable Variables on this Electric object.
	 * @param var the Variable on this ElectricObject to describe.
	 * @param cX the center X coordinate of the ElectricObject.
	 * @param cY the center Y coordinate of the ElectricObject.
	 * @param wnd window in which the Variable will be displayed.
	 * @param multipleStrings true to break multiline text into multiple Polys.
	 * @return an array of Poly objects that describe the Variable. May return zero length array.
	 */
	public Poly [] getPolyList(Variable var, double cX, double cY, EditWindow0 wnd, boolean multipleStrings)
	{
		double offX = var.getXOff();
		double offY = var.getYOff();
		int varLength = var.getLength();
		double lineOffX = 0, lineOffY = 0;
		AffineTransform trans = null;
		Poly.Type style = var.getPos().getPolyType();
		TextDescriptor td = var.getTextDescriptor();
		if (this instanceof NodeInst && (offX != 0 || offY != 0))
		{
			td = td.withOff(0, 0);
//			MutableTextDescriptor mtd = new MutableTextDescriptor(td);
//			mtd.setOff(0, 0);
//			td = mtd;
		}
		boolean headerString = false;
		double fontHeight = 1;
		double scale = 1;
		if (wnd != null)
		{
			fontHeight = td.getTrueSize(wnd);
			scale = wnd.getScale();
		}
		fontHeight *= User.getGlobalTextScale();
		if (varLength > 1)
		{
			// compute text height
			double lineDist = fontHeight / scale;
			int rotQuadrant = td.getRotation().getIndex();
			switch (rotQuadrant)
			{
				case 0: lineOffY = lineDist;    break;		// 0 degrees rotation
				case 1: lineOffX = -lineDist;   break;		// 90 degrees rotation
				case 2: lineOffY = -lineDist;   break;		// 180 degrees rotation
				case 3: lineOffX = lineDist;    break;		// 270 degrees rotation
			}

			// multiline text on rotated nodes must compensate for node rotation
			Poly.Type rotStyle = style;
			if (this instanceof NodeInst)
			{
				if (style != Poly.Type.TEXTCENT && style != Poly.Type.TEXTBOX)
				{
					NodeInst ni = (NodeInst)this;
					trans = ni.rotateIn();
					int origAngle = style.getTextAngle();
					if (ni.isMirroredAboutXAxis() != ni.isMirroredAboutYAxis() &&
						((origAngle%1800) == 0 || (origAngle%1800) == 1350)) origAngle += 1800;
					int angle = (origAngle - ni.getAngle() + 3600) % 3600;
					style = Poly.Type.getTextTypeFromAngle(angle);
				}
			}
			if (td.getDispPart() == TextDescriptor.DispPos.NAMEVALUE)
			{
				headerString = true;
				varLength++;
			}
			if (multipleStrings)
			{
				if (rotStyle == Poly.Type.TEXTCENT || rotStyle == Poly.Type.TEXTBOX ||
					rotStyle == Poly.Type.TEXTLEFT || rotStyle == Poly.Type.TEXTRIGHT)
				{
					cX += lineOffX * (varLength-1) / 2;
					cY += lineOffY * (varLength-1) / 2;
				}
				if (rotStyle == Poly.Type.TEXTBOT || rotStyle == Poly.Type.TEXTBOTLEFT || rotStyle == Poly.Type.TEXTBOTRIGHT)
				{
					cX += lineOffX * (varLength-1);
					cY += lineOffY * (varLength-1);
				}
			} else
			{
				if (rotStyle == Poly.Type.TEXTCENT || rotStyle == Poly.Type.TEXTBOX ||
					rotStyle == Poly.Type.TEXTLEFT || rotStyle == Poly.Type.TEXTRIGHT)
				{
					cX -= lineOffX * (varLength-1) / 2;
					cY -= lineOffY * (varLength-1) / 2;
				}
				if (rotStyle == Poly.Type.TEXTTOP || rotStyle == Poly.Type.TEXTTOPLEFT || rotStyle == Poly.Type.TEXTTOPRIGHT)
				{
					cX -= lineOffX * (varLength-1);
					cY -= lineOffY * (varLength-1);
				}
				varLength = 1;
				headerString = false;
			}
		}

		VarContext context = null;
		if (wnd != null) context = wnd.getVarContext();
		Poly [] polys = new Poly[varLength];
		for(int i=0; i<varLength; i++)
		{
			String message = null;
			TextDescriptor entryTD = td;
			if (varLength > 1 && headerString)
			{
				if (i == 0)
				{
					message = var.getTrueName()+ "[" + (varLength-1) + "]:";
					entryTD = entryTD.withUnderline(true);
//					MutableTextDescriptor mtd = new MutableTextDescriptor(td);
//					mtd.setUnderline(true);
//					entryTD = mtd;
				} else
				{
					message = var.describe(i-1, context, this);
				}
			} else
			{
				message = var.describe(i, context, this);
			}

			Point2D [] pointList = null;
			if (style == Poly.Type.TEXTBOX && this instanceof Geometric)
			{
				Geometric geom = (Geometric)this;
				Rectangle2D bounds = geom.getBounds();
				pointList = Poly.makePoints(bounds);
			} else
			{
				pointList = new Point2D.Double[1];
				pointList[0] = new Point2D.Double(cX+offX, cY+offY);
				if (trans != null)
					trans.transform(pointList[0], pointList[0]);
			}
			polys[i] = new Poly(pointList);
			polys[i].setString(message);
			polys[i].setStyle(style);
			polys[i].setTextDescriptor(entryTD);
			polys[i].setDisplayedText(new DisplayedText(this, var.getKey()));
			polys[i].setLayer(null);
			cX -= lineOffX;
			cY -= lineOffY;
		}
		return polys;
	}

	/**
	 * Method to create a non-displayable Variable on this ElectricObject with the specified values.
	 * @param name the name of the Variable.
	 * @param value the object to store in the Variable.
	 * @return the Variable that has been created.
	 */
	public Variable newVar(String name, Object value) { return newVar(Variable.newKey(name), value); }

	/**
	 * Method to create a displayable Variable on this ElectricObject with the specified values.
	 * @param key the key of the Variable.
	 * @param value the object to store in the Variable.
	 * @return the Variable that has been created.
	 */
	public Variable newDisplayVar(Variable.Key key, Object value) { return newVar(key, value, true); }

	/**
	 * Method to create a non-displayable Variable on this ElectricObject with the specified values.
	 * Notify to observers as well.
	 * @param key the key of the Variable.
	 * @param value the object to store in the Variable.
	 * @return the Variable that has been created.
	 */
	public Variable newVar(Variable.Key key, Object value)
	{
		return newVar(key, value, false);
	}

 	/**
	 * Method to create a Variable on this ElectricObject with the specified values.
	 * @param key the key of the Variable.
	 * @param value the object to store in the Variable.
	 * @param display true if the Variale is displayable.
	 * @return the Variable that has been created.
	 */
	public Variable newVar(Variable.Key key, Object value, boolean display) {
		TextDescriptor td = null;
		if (this instanceof Cell) td = TextDescriptor.cacheCellDescriptor.newTextDescriptor(display);
		else if (this instanceof Export) td = TextDescriptor.cacheExportDescriptor.newTextDescriptor(display);
		else if (this instanceof NodeInst) td = TextDescriptor.cacheNodeDescriptor.newTextDescriptor(display);
		else if (this instanceof ArcInst) td = TextDescriptor.cacheArcDescriptor.newTextDescriptor(display);
		else td = TextDescriptor.cacheAnnotationDescriptor.newTextDescriptor(display);
		return newVar(key, value, td);
	}

 	/**
	 * Method to create a Variable on this ElectricObject with the specified values.
	 * @param key the key of the Variable.
	 * @param value the object to store in the Variable.
	 * @param td text descriptor of the Variable
	 * @return the Variable that has been created.
	 */
	public Variable newVar(Variable.Key key, Object value, TextDescriptor td)
	{
		if (value == null) return null;
 		if (isDeprecatedVariable(key)) {
			System.out.println("Deprecated variable " + key + " on " + this);
		}
		Variable var = null;
		try {
			var = Variable.newInstance(key, value, td);
		} catch (IllegalArgumentException e) {
			ActivityLogger.logException(e);
			return null;
		}
		addVar(var);
		return getVar(key);
//        setChanged();
//        notifyObservers(v);
//        clearChanged();
	}

 	/**
	 * Method to add a Variable on this ElectricObject.
	 * It may add a repaired copy of this Variable in some cases.
	 * @param var Variable to add.
	 */
	public abstract void addVar(Variable var);

	/**
	 * Method to update a Variable on this ElectricObject with the specified values.
	 * If the Variable already exists, only the value is changed; the displayable attributes are preserved.
	 * @param key the key of the Variable.
	 * @param value the object to store in the Variable.
	 * @return the Variable that has been updated.
	 */
	public Variable updateVar(Variable.Key key, Object value)
	{
		Variable var = getVar(key);
		if (var == null) return newVar(key, value);
		addVar(var.withObject(value));
		return getVar(key);
	}

	/**
	 * Updates the TextDescriptor on this ElectricObject selected by varKey.
	 * The varKey may be a key of variable on this ElectricObject or one of the
	 * special keys:
	 * NodeInst.NODE_NAME
	 * NodeInst.NODE_PROTO
	 * ArcInst.ARC_NAME
	 * Export.EXPORT_NAME
	 * If varKey doesn't select any text descriptor, no action is performed.
	 * The TextDescriptor gives information for displaying the Variable.
	 * @param varKey key of variable or special key.
	 * @param td new value TextDescriptor
	 */
	public void setTextDescriptor(Variable.Key varKey, TextDescriptor td) {
		Variable var = getVar(varKey);
		if (var == null) return;
		if (!(this instanceof Cell))
			td = td.withParam(false);
		addVar(var.withTextDescriptor(td));
	}

	/**
	 * Method to set the X and Y offsets of the text in the TextDescriptor selected by key of
	 * variable or special key.
	 * The values are scaled by 4, so a value of 3 indicates a shift of 0.75 and a value of 4 shifts by 1.
	 * @param varKey key of variable or special key.
	 * @param xd the X offset of the text in the TextDescriptor.
	 * @param yd the Y offset of the text in the TextDescriptor.
	 * @see #setTextDescriptor(com.sun.electric.database.variable.Variable.Key,com.sun.electric.database.variable.TextDescriptor)
	 * @see com.sun.electric.database.variable.Variable#withOff(double,double)
	 */
	public synchronized void setOff(Variable.Key varKey, double xd, double yd) {
		TextDescriptor td = getTextDescriptor(varKey);
		if (td != null) setTextDescriptor(varKey, td.withOff(xd, yd));
	}

	/**
	 * Method to copy text descriptor from another ElectricObject to this ElectricObject.
	 * @param other the other ElectricObject from which to copy Variables.
	 * @param varKey selector of textdescriptor
	 */
	public void copyTextDescriptorFrom(ElectricObject other, Variable.Key varKey)
	{
		TextDescriptor td = other.getTextDescriptor(varKey);
		if (td == null) return;
		setTextDescriptor(varKey, td);
	}

	/**
	 * Rename a Variable. Note that this creates a new variable of
	 * the new name and copies all values from the old variable, and
	 * then deletes the old variable.
	 * @param name the name of the var to rename
	 * @param newName the new name of the variable
	 * @return the new renamed variable
	 */
	public Variable renameVar(String name, String newName) {
		return renameVar(Variable.findKey(name), newName);
	}

	/**
	 * Rename a Variable. Note that this creates a new variable of
	 * the new name and copies all values from the old variable, and
	 * then deletes the old variable.
	 * @param key the name key of the var to rename
	 * @param newName the new name of the variable
	 * @return the new renamed variable, or null on error (no action taken)
	 */
	public Variable renameVar(Variable.Key key, String newName) {
		// see if newName exists already
		Variable.Key newKey = Variable.newKey(newName);
		Variable var = getVar(newKey);
		if (var != null) return null;            // name already exists

		// get current Variable
		Variable oldvar = getVar(key);
		if (oldvar == null) return null;

		// create new var
		Variable newVar = newVar(newKey, oldvar.getObject(), oldvar.getTextDescriptor());
		if (newVar == null) return null;
		// copy settings from old var to new var
//        newVar.setTextDescriptor();
//        newVar.copyFlags(oldvar);
		// delete old var
		delVar(oldvar.getKey());

		return newVar;
	}

	/**
	 * Method to delete a Variable from this ElectricObject.
	 * @param key the key of the Variable to delete.
	 */
	public abstract void delVar(Variable.Key key);

	/**
	 * Method to copy all variables from another ElectricObject to this ElectricObject.
	 * @param other the other ElectricObject from which to copy Variables.
	 */
	public void copyVarsFrom(ElectricObject other)
	{
		checkChanging();
		Iterator<Variable> it = other.getVariables();
		synchronized(this) {
			while(it.hasNext())
			{
				Variable var = it.next();
				Variable newVar = this.newVar(var.getKey(), var.getObject(), var.getTextDescriptor());
				if (newVar != null)
				{
 //                   newVar.copyFlags(var);
 //                   newVar.setTextDescriptor();
				}
			}
		}
	}

	private static class ArrayName
	{
		private String baseName;
		private String indexPart;
	}

	/**
	 * Method to return a unique object name in a Cell.
	 * @param name the original name that is not unique.
	 * @param cell the Cell in which this name resides.
	 * @param cls the class of the object on which this name resides.
	 * @param leaveIndexValues true to leave the index values untouches
	 * (i.e. "m[17]" will become "m_1[17]" instead of "m[18]").
	 * @return a unique name for that class in that Cell.
	 */
	public static String uniqueObjectName(String name, Cell cell, Class cls, boolean leaveIndexValues) {
		String newName = name;
		for (int i = 0; !cell.isUniqueName(newName, cls, null); i++) {
			newName = uniqueObjectNameLow(newName, cell, cls, null, null, leaveIndexValues);
			if (i > 100) {
				System.out.println("Can't create unique object name in " + cell + " from original " + name + " attempted " + newName);
				return null;
			}
		}
		return newName;
	}

	/**
	 * Method to return a unique object name in a Cell.
	 * @param name the original name that is not unique.
	 * @param cell the Cell in which this name resides.
	 * @param cls the class of the object on which this name resides.
	 * @param already a Set of names already in use (lower case).
	 * @param leaveIndexValues true to leave the index values untouches
	 * (i.e. "m[17]" will become "m_1[17]" instead of "m[18]").
	 * @return a unique name for that class in that Cell.
	 */
	public static String uniqueObjectName(String name, Cell cell, Class cls,
		Set already, HashMap<String,GenMath.MutableInteger> nextPlainIndex, boolean leaveIndexValues)
	{
		String newName = name;
		String lcName = TextUtils.canonicString(newName);
		for (int i = 0; already.contains(lcName); i++)
		{
			newName = uniqueObjectNameLow(newName, cell, cls, already, nextPlainIndex, leaveIndexValues);
			if (i > 100)
			{
				System.out.println("Can't create unique object name in " + cell + " from original " + name + " attempted " + newName);
				return null;
			}
			lcName = TextUtils.canonicString(newName);
		}
		return newName;
	}

	private static String uniqueObjectNameLow(String name, Cell cell, Class cls,
		Set already, HashMap<String,GenMath.MutableInteger> nextPlainIndex, boolean leaveIndexValues)
	{
		// first see if the name is unique
		if (already != null)
		{
			if (!already.contains(TextUtils.canonicString(name))) return name;
		} else
		{
			if (cell.isUniqueName(name, cls, null)) return name;
		}

		// see if there is a "++" anywhere to tell us what to increment
		int plusPlusPos = name.indexOf("++");
		if (plusPlusPos >= 0)
		{
			int numStart = plusPlusPos;
			while (numStart > 0 && TextUtils.isDigit(name.charAt(numStart-1))) numStart--;
			if (numStart < plusPlusPos)
			{
				int nextIndex = TextUtils.atoi(name.substring(numStart)) + 1;
				for( ; ; nextIndex++)
				{
					String newname = name.substring(0, numStart) + nextIndex + name.substring(plusPlusPos);
					if (already != null)
					{
						if (!already.contains(TextUtils.canonicString(newname))) return newname;
					} else
					{
						if (cell.isUniqueName(newname, cls, null)) return newname;
					}
				}
			}
		}

		// see if there is a "--" anywhere to tell us what to decrement
		int minusMinusPos = name.indexOf("--");
		if (minusMinusPos >= 0)
		{
			int numStart = minusMinusPos;
			while (numStart > 0 && TextUtils.isDigit(name.charAt(numStart-1))) numStart--;
			if (numStart < minusMinusPos)
			{
				int nextIndex = TextUtils.atoi(name.substring(numStart)) - 1;
				for( ; nextIndex >= 0; nextIndex--)
				{
					String newname = name.substring(0, numStart) + nextIndex + name.substring(minusMinusPos);
					if (already != null)
					{
						if (!already.contains(TextUtils.canonicString(newname))) return newname;
					} else
					{
						if (cell.isUniqueName(newname, cls, null)) return newname;
					}
				}
			}
		}

		// break the string into a list of ArrayName objects
		List<ArrayName> names = new ArrayList<ArrayName>();
		boolean inBracket = false;
		int len = name.length();
		int startOfBase = 0;
		int startOfIndex = -1;
		for(int i=0; i<len; i++)
		{
			char ch = name.charAt(i);
			if (ch == '[')
			{
				if (startOfIndex < 0) startOfIndex = i;
				inBracket = true;
			}
			if (ch == ']') inBracket = false;
			if ((ch == ',' && !inBracket) || i == len-1)
			{
				// remember this arrayname
				if (i == len-1) i++;
				ArrayName an = new ArrayName();
				int endOfBase = startOfIndex;
				if (endOfBase < 0) endOfBase = i;
				an.baseName = name.substring(startOfBase, endOfBase);
				if (startOfIndex >= 0) an.indexPart = name.substring(startOfIndex, i);
				names.add(an);
				startOfBase = i+1;
				startOfIndex = -1;
			}
		}

		char separateChar = '_';
		for(ArrayName an : names)
		{
			// adjust the index part if possible
			boolean indexAdjusted = false;
			String index = an.indexPart;
			if (index != null && !leaveIndexValues)
			{
				int possibleEnd = 0;
				int nameLen = index.length();

				// see if the index part can be incremented
				int possibleStart = -1;
				int endPos = nameLen-1;
				for(;;)
				{
					// find the range of characters in square brackets
					int startPos = index.lastIndexOf('[', endPos);
					if (startPos < 0) break;

					// see if there is a comma in the bracketed expression
					int i = index.indexOf(',', startPos);
					if (i >= 0 && i < endPos)
					{
						// this bracketed expression cannot be incremented: move on
						if (startPos > 0 && index.charAt(startPos-1) == ']')
						{
							endPos = startPos-1;
							continue;
						}
						break;
					}

					// see if there is a colon in the bracketed expression
					i = index.indexOf(':', startPos);
					if (i >= 0 && i < endPos)
					{
						// colon: make sure there are two numbers
						String firstIndex = index.substring(startPos+1, i);
						String secondIndex = index.substring(i+1, endPos);
						if (TextUtils.isANumber(firstIndex) && TextUtils.isANumber(secondIndex))
						{
							int startIndex = TextUtils.atoi(firstIndex);
							int endIndex = TextUtils.atoi(secondIndex);
							int spacing = Math.abs(endIndex - startIndex) + 1;
							for(int nextIndex = 1; ; nextIndex++)
							{
								String newIndex = index.substring(0, startPos) + "[" + (startIndex+spacing*nextIndex) +
									":" + (endIndex+spacing*nextIndex) + index.substring(endPos);
								boolean unique;
								if (already != null)
								{
									unique = !already.contains(TextUtils.canonicString(an.baseName + newIndex));
								} else
								{
									unique = cell.isUniqueName(an.baseName + newIndex, cls, null);
								}
								if (unique)
								{
									indexAdjusted = true;
									an.indexPart = newIndex;
									break;
								}
							}
							if (indexAdjusted) break;
						}

						// this bracketed expression cannot be incremented: move on
						if (startPos > 0 && index.charAt(startPos-1) == ']')
						{
							endPos = startPos-1;
							continue;
						}
						break;
					}

					// see if this bracketed expression is a pure number
					String bracketedExpression = index.substring(startPos+1, endPos);
					if (TextUtils.isANumber(bracketedExpression))
					{
						int nextIndex = TextUtils.atoi(bracketedExpression) + 1;
						for(; ; nextIndex++)
						{
							String newIndex = index.substring(0, startPos) + "[" + nextIndex + index.substring(endPos);
							boolean unique;
							if (already != null)
							{
								unique = !already.contains(TextUtils.canonicString(an.baseName + newIndex));
							} else
							{
								unique = cell.isUniqueName(an.baseName + newIndex, cls, null);
							}
							if (unique)
							{
								indexAdjusted = true;
								an.indexPart = newIndex;
								break;
							}
						}
						if (indexAdjusted) break;
					}

					// remember the first index that could be incremented in a pinch
					if (possibleStart < 0)
					{
						possibleStart = startPos;
						possibleEnd = endPos;
					}

					// this bracketed expression cannot be incremented: move on
					if (startPos > 0 && index.charAt(startPos-1) == ']')
					{
						endPos = startPos-1;
						continue;
					}
					break;
				}

				// if there was a possible place to increment, do it
				if (!indexAdjusted && possibleStart >= 0)
				{
					// nothing simple, but this one can be incremented
					int i;
					for(i=possibleEnd-1; i>possibleStart; i--)
						if (!TextUtils.isDigit(index.charAt(i))) break;
					int nextIndex = TextUtils.atoi(index.substring(i+1)) + 1;
					int startPos = i+1;
					if (index.charAt(startPos-1) == separateChar) startPos--;
					for(; ; nextIndex++)
					{
						String newIndex = index.substring(0, startPos) + separateChar + nextIndex + index.substring(possibleEnd);
						boolean unique;
						if (already != null)
						{
							unique = !already.contains(TextUtils.canonicString(an.baseName + newIndex));
						} else
						{
							unique = cell.isUniqueName(an.baseName + newIndex, cls, null);
						}
						if (unique)
						{
							indexAdjusted = true;
							an.indexPart = newIndex;
							break;
						}
					}
				}
			}

			// if the index was not adjusted, adjust the base part
			if (!indexAdjusted)
			{
				// array contents cannot be incremented: increment base name
				String base = an.baseName;
				int startPos = base.length();
				int endPos = base.length();

				// if there is a numeric part at the end, increment that
				String localSepString = String.valueOf(separateChar);
				while (startPos > 0 && TextUtils.isDigit(base.charAt(startPos-1))) startPos--;
				int nextIndex = 1;
				if (startPos >= endPos)
				{
					if (startPos > 0 && base.charAt(startPos-1) == separateChar) startPos--;
				} else
				{
					nextIndex = TextUtils.atoi(base.substring(startPos)) + 1;
					localSepString = "";
				}

				// find the unique index to use
				String prefix = base.substring(0, startPos) + localSepString;
				if (nextPlainIndex != null)
				{
					GenMath.MutableInteger nxt = nextPlainIndex.get(prefix);
					if (nxt == null)
					{
						nxt = new GenMath.MutableInteger(cell.getUniqueNameIndex(prefix, cls, nextIndex));
						nextPlainIndex.put(prefix, nxt);
					}
					nextIndex = nxt.intValue();
					nxt.increment();
				} else
				{
					nextIndex = cell.getUniqueNameIndex(prefix, cls, nextIndex);
				}
				an.baseName = prefix + nextIndex + base.substring(endPos);
			}
		}
		StringBuffer result = new StringBuffer();
		boolean first = true;
		for(ArrayName an : names)
		{
			if (first) first = false; else
				result.append(",");
			result.append(an.baseName);
			if (an.indexPart != null) result.append(an.indexPart);
		}
		return result.toString();
	}

	/**
	 * Method to determine whether a Variable key on this object is deprecated.
	 * Deprecated Variable keys are those that were used in old versions of Electric,
	 * but are no longer valid.
	 * @param key the key of the Variable.
	 * @return true if the Variable key is deprecated.
	 */
	public boolean isDeprecatedVariable(Variable.Key key)
	{
		String name = key.toString();
		if (name.length() == 0) return true;
		if (name.length() == 1)
		{
			char chr = name.charAt(0);
			if (!Character.isLetter(chr)) return true;
		}
		return false;
	}

	/**
	 * Method to return an Iterator over all Variables on this ElectricObject.
	 * @return an Iterator over all Variables on this ElectricObject.
	 */
	public synchronized Iterator<Variable> getVariables() { return getImmutable().getVariables(); }

	/**
	 * Method to return the number of Variables on this ElectricObject.
	 * @return the number of Variables on this ElectricObject.
	 */
	public synchronized int getNumVariables() { return getImmutable().getNumVariables(); }

	/**
	 * Routing to check whether changing of this cell allowed or not.
	 * By default checks whole database change. Overriden in subclasses.
	 */
	public void checkChanging() {
        EDatabase database = getDatabase();
        if (database != null)
            database.checkChanging();
	}

	/**
	 * Routing to check whether undoing of this cell allowed or not.
	 * By default checks whole database undo. Overriden in subclasses.
	 */
	public void checkUndoing() {
        getDatabase().checkUndoing();
	}

	/**
	 * Method to make sure that this object can be examined.
	 * Ensures that an examine job is running.
	 */
	public void checkExamine() {
        EDatabase database = getDatabase();
        if (database != null)
            database.checkExamine();
	}

	/**
	 * Returns database to which this ElectricObject belongs.
	 * Some objects are not in database, for example Geometrics in PaletteFrame.
     * Method returns null for non-database objects.
     * @return database to which this ElectricObject belongs.
	 */
	public abstract EDatabase getDatabase();

	/**
	 * Method which indicates that this object is in database.
	 * Some objects are not in database, for example Geometrics in PaletteFrame.
	 * @return true if this object is in database, false if it is not a database object,
	 * or if it is a dummy database object (considered not to be in the database).
	 */
	protected boolean isDatabaseObject() { return true; }

	/**
	 * Method to determine the appropriate Cell associated with this ElectricObject.
	 * @return the appropriate Cell associated with this ElectricObject.
	 * Returns null if no Cell can be found.
	 */
	public Cell whichCell() { return null; }

	/**
	 * Method to write a description of this ElectricObject (lists all Variables).
	 * Displays the description in the Messages Window.
	 */
	public void getInfo()
	{
		checkExamine();
		boolean firstvar = true;
		for(Iterator<Variable> it = getVariables(); it.hasNext() ;)
		{
			Variable val = it.next();
			Variable.Key key = val.getKey();
			if (val == null) continue;
			if (firstvar) System.out.println("Variables:");   firstvar = false;
			Object addr = val.getObject();
			String par = isParam(key) ? "(param)" : "";
//			String par = val.isParam() ? "(param)" : "";
			if (addr instanceof Object[])
			{
				Object[] ary = (Object[]) addr;
				System.out.print("   " + key.getName() + "(" + ary.length + ") = [");
				for (int i = 0; i < ary.length; i++)
				{
					if (i > 4)
					{
						System.out.print("...");
						break;
					}
					if (ary[i] instanceof String) System.out.print("\"");
					System.out.print(ary[i]);
					if (ary[i] instanceof String) System.out.print("\"");
					if (i < ary.length-1) System.out.print(", ");
				}
				System.out.println("] "+par);
			} else
			{
				System.out.println("   " + key.getName() + "= " + addr + " "+par);
			}
		}
	}

	/**
	 * Returns a printable version of this ElectricObject.
	 * @return a printable version of this ElectricObject.
	 */
	public String toString()
	{
		return getClass().getName();
	}

//    /**
//     * Observer method to update variables in Icon instance if cell master changes
//     * @param o
//     * @param arg
//     */
//    public void update(Observable o, Object arg)
//    {
//        System.out.println("Entering update");
//        // New
//        if (arg instanceof Variable)
//        {
//            Variable var = (Variable)arg;
//            // You can't call newVar(var.getKey(), var.getObject()) to avoid infinite loop
//            newVar(var.getD());
//        }
//        else if (arg instanceof Object[])
//        {
//            Object[] array = (Object[])arg;
//
//            if (!(array[0] instanceof String))
//            {
//                System.out.println("Error in ElectricObject.update");
//                return;
//            }
//            String function = (String)array[0];
//            if (function.startsWith("setTextDescriptor"))
//            {
//                Variable.Key varKey = (Variable.Key)array[1];
//                TextDescriptor td = (TextDescriptor)array[2];
//                // setTextDescriptor(String varName, TextDescriptor td)
//                setTextDescriptor(varKey, td);
//            }
//            else if (function.startsWith("delVar"))
//            {
//                Variable.Key key = (Variable.Key)array[1];
//                delVarNoObserver(key);
//            }
////            else if (array[0] instanceof Variable.Key)
////            {
////                //  Variable updateVar(String name, Object value)
////                Variable.Key key = (Variable.Key)array[0];
////                updateVar(key, array[1]);
////            }
//        }
//    }
    
	/**
	 * Method to check invariants in this ElectricObject.
	 * @exception AssertionError if invariants are not valid
	 */
	protected void check() {}
}
