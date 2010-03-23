/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: CircuitChangeJobs.java
 *
 * Copyright (c) 2006 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.user;

import static com.sun.electric.database.text.ArrayIterator.i2i;

import com.sun.electric.database.IdMapper;
import com.sun.electric.database.constraint.Layout;
import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.geometry.Dimension2D;
import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.geometry.ERectangle;
import com.sun.electric.database.geometry.GenMath;
import com.sun.electric.database.geometry.Orientation;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.geometry.PolyBase;
import com.sun.electric.database.geometry.PolyMerge;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.EDatabase;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.topology.Geometric;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.CodeExpression;
import com.sun.electric.database.variable.DisplayedText;
import com.sun.electric.database.variable.EditWindow_;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.UserInterface;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.technology.technologies.Schematics;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.Tool;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.input.LibraryFiles;
import com.sun.electric.tool.user.dialogs.OpenFile;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.SizeListener;
import com.sun.electric.tool.user.ui.StatusBar;
import com.sun.electric.tool.user.ui.TopLevel;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JOptionPane;

/**
 * Class for Jobs that make changes to the circuit.
 */
public class CircuitChangeJobs
{
	// constructor, never used
	CircuitChangeJobs() {}

	/****************************** NODE TRANSFORMATION ******************************/

	public static class RotateSelected extends Job
	{
		private Cell cell;
		private int amount;
		private boolean mirror;
		private boolean mirrorH;
		private List<Geometric> highs;

		/**
		 * @param cell
		 * @param highs the highlighted objects (list of highlights)
		 * @param amount angle in tenth degrees to rotate
		 * @param mirror whether or not to mirror. if true, amount is ignored, and mirrorH is used.
		 * @param mirrorH if true, mirror horizontally (flip over X-axis), otherwise mirror
		 * vertically (flip over Y-axis). Ignored if mirror is false.
		 */
		public RotateSelected(Cell cell, List<Geometric> highs, int amount, boolean mirror, boolean mirrorH)
		{
			super("Rotate selected objects", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.cell = cell;
			this.amount = amount;
			this.mirror = mirror;
			this.mirrorH = mirrorH;
			this.highs = highs;
			startJob();
		}

		public boolean doIt() throws JobException
		{
			// disallow rotating if lock is on
			if (cantEdit(cell, null, true, false, true) != 0) return false;

			// figure out which nodes get rotated/mirrored
			Set<Geometric> markObj = new HashSet<Geometric>();
			int nicount = 0;
			NodeInst theNi = null;
			Rectangle2D selectedBounds = new Rectangle2D.Double();

            // Add arc if both pins are in the list to avoid weird changes by the constraints system
            for (Iterator<ArcInst> it = cell.getArcs(); it.hasNext();)
            {
                ArcInst ai = it.next();
                PortInst tail = ai.getTailPortInst();
                PortInst head = ai.getHeadPortInst();

                if (highs.contains(head) && highs.contains(tail))
                    highs.add(ai); // better to add the arc here
            }

            for(Geometric geom : highs)
			{
				if (!(geom instanceof NodeInst)) continue;
                NodeInst ni = (NodeInst)geom;

                if (Generic.isSpecialGenericNode(ni))
                    continue; // like center

				if (cantEdit(cell, ni, true, false, true) != 0)
				{
					return false;
				}
				markObj.add(ni);
				if (nicount == 0)
				{
					selectedBounds.setRect(ni.getBounds());
				} else
				{
					Rectangle2D.union(selectedBounds, ni.getBounds(), selectedBounds);
				}
				theNi = ni;
				nicount++;
			}

			// must be at least 1 node
			if (nicount <= 0)
			{
				System.out.println("Must select at least 1 node for rotation");
				return false;
			}

			// if multiple nodes, find the center one
			if (nicount > 1)
			{
				Point2D center = new Point2D.Double(selectedBounds.getCenterX(), selectedBounds.getCenterY());
				theNi = null;
				double bestdist = Integer.MAX_VALUE;
				for(Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
				{
					NodeInst ni = it.next();
					if (!markObj.contains(ni)) continue;
					double dist = center.distance(ni.getTrueCenter());

					// LINTED "bestdist" used in proper order
					if (theNi == null || dist < bestdist)
					{
						theNi = ni;
						bestdist = dist;
					}
				}
			}

			// see which nodes already connect to the main rotation/mirror node (theNi)
			markObj.clear();
			for(Geometric geom : highs)
			{
				if (!(geom instanceof ArcInst)) continue;
				ArcInst ai = (ArcInst)geom;
				markObj.add(ai);
			}
			spreadRotateConnection(theNi, markObj);

			// now make sure that it is all connected
			for(Geometric geom : highs)
			{
				if (!(geom instanceof NodeInst)) continue;
				NodeInst ni = (NodeInst)geom;
				spreadRotateConnection(ni, markObj);
			}

			// do the rotation/mirror
			Orientation dOrient;
			if (mirror)
			{
				// do mirroring
				dOrient = mirrorH ? Orientation.Y : Orientation.X;
			} else
			{
				// do rotation
				dOrient = Orientation.fromAngle(amount);
			}
			AffineTransform trans = dOrient.rotateAbout(theNi.getAnchorCenter());

			Point2D.Double tmpPt1 = new Point2D.Double(), tmpPt2 = new Point2D.Double();

			// Rotate nodes in markObj
			for (Geometric geom : markObj)
			{
				if (!(geom instanceof NodeInst)) continue;
				NodeInst ni = (NodeInst)geom;
				boolean wasMirroredInX = ni.isMirroredAboutXAxis();
				boolean wasMirroredInY = ni.isMirroredAboutYAxis();
				trans.transform(ni.getAnchorCenter(), tmpPt1);
				ni.rotate(dOrient);
				ni.move(tmpPt1.getX() - ni.getAnchorCenterX(), tmpPt1.getY() - ni.getAnchorCenterY());

				if (Poly.NEWTEXTTREATMENT)
				{
					// rotate any text on the node
					rotateText(ni, NodeInst.NODE_NAME, dOrient, wasMirroredInX==wasMirroredInY);
					for(Iterator<Variable> it = ni.getParametersAndVariables(); it.hasNext(); )
					{
						Variable var = it.next();
						rotateText(ni, var.getKey(), dOrient, wasMirroredInX==wasMirroredInY);
					}

					// rotate any exports on the node
					for(Iterator<Export> it = ni.getExports(); it.hasNext(); )
					{
						Export e = it.next();
						rotateText(e, Export.EXPORT_NAME, dOrient, wasMirroredInX==wasMirroredInY);
					}
				}
			}

			// Rotate arcs in markObj
			for (Geometric geom : markObj)
			{
				if (!(geom instanceof ArcInst)) continue;
				ArcInst ai = (ArcInst)geom;
				if (markObj.contains(ai.getHeadPortInst().getNodeInst()))
					trans.transform(ai.getHeadLocation(), tmpPt1);
				else
					tmpPt1.setLocation(ai.getHeadLocation());
				if (markObj.contains(ai.getTailPortInst().getNodeInst()))
					trans.transform(ai.getTailLocation(), tmpPt2);
				else
					tmpPt2.setLocation(ai.getTailLocation());
				ai.modify(tmpPt1.getX() - ai.getHeadLocation().getX(), tmpPt1.getY() - ai.getHeadLocation().getY(),
					tmpPt2.getX() - ai.getTailLocation().getX(), tmpPt2.getY() - ai.getTailLocation().getY());

				if (Poly.NEWTEXTTREATMENT)
				{
					// rotate any text on the arc
					rotateText(ai, ArcInst.ARC_NAME, dOrient, !mirror);
					for(Iterator<Variable> it = ai.getParametersAndVariables(); it.hasNext(); )
					{
						Variable var = it.next();
						rotateText(ai, var.getKey(), dOrient, true);
					}
				}
			}

			return true;
		}

		/**
		 * Method to rotate text on an object.
		 * @param eObj the ElectricObject on which the text resides.
		 * @param key the Key for the text on the object.
		 * @param dOrient the amount of rotation.
		 * @param wasSameMirror true if the L-R mirroring was the same as the U-D mirroring.
		 */
		private void rotateText(ElectricObject eObj, Variable.Key key, Orientation dOrient, boolean wasSameMirror)
		{
			TextDescriptor td = eObj.getTextDescriptor(key);
			Poly.Type oldType = td.getPos().getPolyType();
			if (oldType == Poly.Type.TEXTCENT || oldType == Poly.Type.TEXTBOX) return;

			AffineTransform dTrans = dOrient.pureRotate();
			Poly.Type newType = oldType;
			if (dOrient == Orientation.X)
			{
				// mirror X (left-right)
				oldType = oldType.rotateTextAnchorIn(td.getRotation());
				if (oldType == Poly.Type.TEXTLEFT) oldType = Poly.Type.TEXTRIGHT; else
				if (oldType == Poly.Type.TEXTRIGHT) oldType = Poly.Type.TEXTLEFT; else
				if (oldType == Poly.Type.TEXTBOTLEFT) oldType = Poly.Type.TEXTBOTRIGHT; else
				if (oldType == Poly.Type.TEXTBOTRIGHT) oldType = Poly.Type.TEXTBOTLEFT; else
				if (oldType == Poly.Type.TEXTTOPLEFT) oldType = Poly.Type.TEXTTOPRIGHT; else
				if (oldType == Poly.Type.TEXTTOPRIGHT) oldType = Poly.Type.TEXTTOPLEFT;
				oldType = oldType.rotateTextAnchorOut(td.getRotation());
				newType = oldType;
			} else if (dOrient == Orientation.Y)
			{
				// mirror Y (top-bottom)
				oldType = oldType.rotateTextAnchorIn(td.getRotation());
				if (oldType == Poly.Type.TEXTBOT) oldType = Poly.Type.TEXTTOP; else
				if (oldType == Poly.Type.TEXTTOP) oldType = Poly.Type.TEXTBOT; else
				if (oldType == Poly.Type.TEXTBOTLEFT) oldType = Poly.Type.TEXTTOPLEFT; else
				if (oldType == Poly.Type.TEXTTOPLEFT) oldType = Poly.Type.TEXTBOTLEFT; else
				if (oldType == Poly.Type.TEXTBOTRIGHT) oldType = Poly.Type.TEXTTOPRIGHT; else
				if (oldType == Poly.Type.TEXTTOPRIGHT) oldType = Poly.Type.TEXTBOTRIGHT;
				oldType = oldType.rotateTextAnchorOut(td.getRotation());
				newType = oldType;
			} else
			{
				// determine change in angle because of node rotation
				int origAngle = oldType.getTextAngle();
				if (!wasSameMirror && ((origAngle%1800) == 0 || (origAngle%1800) == 1350))
					origAngle += 1800;
				Point2D pt = new Point2D.Double(100, 0);
				dTrans.transform(pt, pt);
				int xAngle = GenMath.figureAngle(new Point2D.Double(0, 0), pt);
				int angle = (origAngle + xAngle) % 3600;
				newType = Poly.Type.getTextTypeFromAngle(angle);
			}
			TextDescriptor.Position newPos = TextDescriptor.Position.getPosition(newType);
//System.out.println("WAS "+oldType.name()+" ROTATED "+dOrient.toString()+" IS "+newType.name());
			td = td.withPos(newPos);
			eObj.setTextDescriptor(key, td);

			// if an arc, rotate the offset point
			if (eObj instanceof ArcInst)
			{
				Point2D pt = new Point2D.Double(td.getXOff(), td.getYOff());
				dTrans.transform(pt, pt);
				eObj.setTextDescriptor(key, td.withOff(pt.getX(), pt.getY()));
			}
		}
	}

	/**
	 * Helper method for rotation to mark selected nodes that need not be
	 * connected with an invisible arc.
	 */
	private static void spreadRotateConnection(NodeInst theNi, Set<Geometric> markObj)
	{
		if (markObj.contains(theNi)) return;
		markObj.add(theNi);
		for(Iterator<Connection> it = theNi.getConnections(); it.hasNext(); )
		{
			Connection con = it.next();
			ArcInst ai = con.getArc();
			if (!markObj.contains(ai)) continue;
			int otherEnd = 1 - con.getEndIndex();
			NodeInst ni = ai.getPortInst(otherEnd).getNodeInst();
			if (markObj.contains(ni)) continue;
			markObj.add(ni);
			spreadRotateConnection(ni, markObj);
		}
	}

	/****************************** NODE ALIGNMENT ******************************/

	/**
	 * This class implement the command to align objects to the grid.
	 */
	public static class AlignObjects extends Job
	{
		private List<Geometric> list;			// list of highlighted objects to align
		private Dimension2D alignment;

		public AlignObjects(List<Geometric> highs, Dimension2D alignment)
		{
			super("Align Objects", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.list = highs;
			this.alignment = alignment;
			startJob();
		}

		public boolean doIt() throws JobException
		{
			// first adjust the nodes
			int adjustedNodes = 0;
			for(Geometric geom : list)
			{
				if (!(geom instanceof NodeInst)) continue;
				NodeInst ni = (NodeInst)geom;

				Point2D [] points = ni.getTrace();
				if (points != null)
				{
					AffineTransform transOut = ni.pureRotateOut();
					Point2D [] newPoints = new Point2D[points.length];
					boolean changed = false;
					for(int i=0; i<points.length; i++)
					{
						if (points[i] == null) continue;
						Point2D newPoint = new Point2D.Double(points[i].getX() + ni.getAnchorCenterX(), points[i].getY() + ni.getAnchorCenterY());
						transOut.transform(newPoint, newPoint);
						double oldX = newPoint.getX();
						double oldY = newPoint.getY();
						DBMath.gridAlign(newPoint, alignment);
						if (oldX != newPoint.getX() || oldY != newPoint.getY()) changed = true;
						newPoints[i] = newPoint;
					}
					if (changed)
					{
						adjustedNodes++;
						ni.setTrace(newPoints);
					}
					continue;
				}

				Point2D center = new Point2D.Double(ni.getAnchorCenterX(), ni.getAnchorCenterY());
				DBMath.gridAlign(center, alignment);
				double bodyXOffset = center.getX() - ni.getAnchorCenterX();
				double bodyYOffset = center.getY() - ni.getAnchorCenterY();

				double portXOffset = bodyXOffset;
				double portYOffset = bodyYOffset;
				boolean mixedportpos = false;
				boolean firstPort = true;
				for(Iterator<PortInst> pIt = ni.getPortInsts(); pIt.hasNext(); )
				{
					PortInst pi = pIt.next();
					Poly poly = pi.getPoly();
					Point2D portCenter = new Point2D.Double(poly.getCenterX(), poly.getCenterY());
					DBMath.gridAlign(portCenter, alignment);
					double pXO = portCenter.getX() - poly.getCenterX();
					double pYO = portCenter.getY() - poly.getCenterY();
					if (firstPort)
					{
						firstPort = false;
						portXOffset = pXO;   portYOffset = pYO;
					} else
					{
						if (portXOffset != pXO || portYOffset != pYO) mixedportpos = true;
					}
				}
				if (!mixedportpos)
				{
					bodyXOffset = portXOffset;   bodyYOffset = portYOffset;
				}

				// if a primitive has an offset, see if the node edges are aligned
				if (bodyXOffset != 0 || bodyYOffset != 0)
				{
					if (!ni.isCellInstance())
					{
						AffineTransform transr = ni.rotateOut();
						Technology tech = ni.getProto().getTechnology();
						Poly [] polyList = tech.getShapeOfNode(ni);
						for(int j=0; j<polyList.length; j++)
						{
							Poly poly = polyList[j];
							poly.transform(transr);
							Rectangle2D bounds = poly.getBox();
							if (bounds == null) continue;
							Point2D polyPoint1 = new Point2D.Double(bounds.getMinX(), bounds.getMinY());
							Point2D polyPoint2 = new Point2D.Double(bounds.getMaxX(), bounds.getMaxY());
							DBMath.gridAlign(polyPoint1, alignment);
							DBMath.gridAlign(polyPoint2, alignment);
							if (polyPoint1.getX() == bounds.getMinX() &&
								polyPoint2.getX() == bounds.getMaxX()) bodyXOffset = 0;
							if (polyPoint1.getY() == bounds.getMinY() &&
								polyPoint2.getY() == bounds.getMaxY()) bodyYOffset = 0;
							if (bodyXOffset == 0 && bodyYOffset == 0) break;
						}
					}
				}

				// move the node
				if (bodyXOffset != 0 || bodyYOffset != 0)
				{
					// turn off all constraints on arcs
					Map<ArcInst,Integer> constraints = new HashMap<ArcInst,Integer>();
					for(Iterator<Connection> aIt = ni.getConnections(); aIt.hasNext(); )
					{
						Connection con = aIt.next();
						ArcInst ai = con.getArc();
						int constr = 0;
						if (ai.isRigid()) constr |= 1;
						if (ai.isFixedAngle()) constr |= 2;
						constraints.put(ai, new Integer(constr));
					}
					ni.move(bodyXOffset, bodyYOffset);
					adjustedNodes++;

					// restore arc constraints
					for(Iterator<Connection> aIt = ni.getConnections(); aIt.hasNext(); )
					{
						Connection con = aIt.next();
						ArcInst ai = con.getArc();
						Integer constr = constraints.get(ai);
						if (constr == null) continue;
						if ((constr.intValue() & 1) != 0) ai.setRigid(true);
						if ((constr.intValue() & 2) != 0) ai.setFixedAngle(true);
					}
				}
			}

			// now adjust the arcs
			int adjustedArcs = 0;
			for(Geometric geom : list)
			{
				if (!(geom instanceof ArcInst)) continue;
				ArcInst ai = (ArcInst)geom;
				if (!ai.isLinked()) continue;

				Point2D origHead = ai.getHeadLocation();
				Point2D origTail = ai.getTailLocation();
				Point2D arcHead = new Point2D.Double(origHead.getX(), origHead.getY());
				Point2D arcTail = new Point2D.Double(origTail.getX(), origTail.getY());
				DBMath.gridAlign(arcHead, alignment);
				DBMath.gridAlign(arcTail, alignment);

				double headXOff = arcHead.getX() - origHead.getX();
				double headYOff = arcHead.getY() - origHead.getY();
				double tailXOff = arcTail.getX() - origTail.getX();
				double tailYOff = arcTail.getY() - origTail.getY();
				if (headXOff == 0 && tailXOff == 0 && headYOff == 0 && tailYOff == 0) continue;

				if (!ai.headStillInPort(arcHead, false))
				{
					if (!ai.headStillInPort(origHead, false)) continue;
					headXOff = headYOff = 0;
				}
				if (!ai.tailStillInPort(arcTail, false))
				{
					if (!ai.tailStillInPort(origTail, false)) continue;
					tailXOff = tailYOff = 0;
				}

				// make sure an arc does not change angle
				int ang = ai.getAngle();
				if (ang == 0 || ang == 1800)
				{
					// horizontal arc: both DY values must be the same
					if (headYOff != tailYOff) headYOff = tailYOff = 0;
				} else if (ang == 900 || ang == 2700)
				{
					// vertical arc: both DX values must be the same
					if (headXOff != tailXOff) headXOff = tailXOff = 0;
				}
				if (headXOff != 0 || tailXOff != 0 || headYOff != 0 || tailYOff != 0)
				{
					int constr = 0;
					if (ai.isRigid()) constr |= 1;
					if (ai.isFixedAngle()) constr |= 2;
					ai.setRigid(false);
					ai.setFixedAngle(false);

					ai.modify(headXOff, headYOff, tailXOff, tailYOff);
					adjustedArcs++;
					if ((constr & 1) != 0) ai.setRigid(true);
					if ((constr & 2) != 0) ai.setFixedAngle(true);
				}
			}

			// show results
			if (adjustedNodes == 0 && adjustedArcs == 0) System.out.println("No adjustments necessary"); else
				System.out.println("Adjusted " + adjustedNodes + " nodes and " + adjustedArcs + " arcs");
			return true;
		}
	}

	public static class AlignNodes extends Job
	{
		private NodeInst [] nis;
		private double [] dCX;
		private double [] dCY;

		public AlignNodes(NodeInst [] nis, double [] dCX, double [] dCY)
		{
			super("Align objects", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.nis = nis;
			this.dCX = dCX;
			this.dCY = dCY;
			startJob();
		}

		public boolean doIt() throws JobException
		{
			int numRemoved = 0;
			for(int i=0; i<nis.length; i++)
			{
				NodeInst ni = nis[i];
				int res = cantEdit(ni.getParent(), ni, true, false, true);
				if (res < 0) return false;
				if (res > 0)
				{
					numRemoved++;
					nis[i] = null;
				}
			}
			if (numRemoved > 0)
			{
				// make a smaller list
				int newSize = nis.length - numRemoved;
				if (newSize == 0) return true;
				NodeInst [] nnis = new NodeInst[newSize];
				double [] nCX = new double[newSize];
				double [] nCY = new double[newSize];
				int fill = 0;
				for(int i=0; i<nis.length; i++)
				{
					if (nis[i] == null) continue;
					nnis[fill] = nis[i];
					nCX[fill] = dCX[i];
					nCY[fill] = dCY[i];
					fill++;
				}
				nis = nnis;
				dCX = nCX;
				dCY = nCY;
			}

			NodeInst.modifyInstances(nis, dCX, dCY, null, null);
			return true;
		}
	}

	/****************************** ARC MODIFICATION ******************************/

	public enum ChangeArcEnum {
		RIGID("Rigid"),
		NONRIGID("Non-Rigid"),
		FIXEDANGLE("Fixed-Angle"),
		NONFIXEDANGLE("Not-Fixed-Angle"),
		DIRECTIONAL("Directional"),
		HEADEXTEND("extend the head end"),
		TAILEXTEND("extend the tail end");

		private String name;
		ChangeArcEnum(String n) { name = n; }
		public String toString() { return name; }
	}

	public static class ChangeArcProperties extends Job
	{
		private Cell cell;
		private ChangeArcEnum how;
		private List<ElectricObject> objList;
		private boolean repaintContents, repaintAny;

		public ChangeArcProperties(Cell cell, ChangeArcEnum how, List<Highlight> highlighted)
		{
			super("Align objects", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.cell = cell;
			this.how = how;
			this.objList = new ArrayList<ElectricObject>();

			for(Highlight h : highlighted)
			{
				if (!h.isHighlightEOBJ()) continue;
				objList.add(h.getElectricObject());
			}

			startJob();
		}

		public boolean doIt() throws JobException
		{
			// make sure changing arcs is allowed
			if (CircuitChangeJobs.cantEdit(cell, null, true, false, true) != 0) return false;

			int numSet = 0, numUnset = 0;
			for(ElectricObject eobj : objList)
			{
				if (eobj instanceof ArcInst)
				{
					ArcInst ai = (ArcInst)eobj;
					switch (how)
					{
						case RIGID:
							if (!ai.isRigid())
							{
								ai.setRigid(true);
								numSet++;
							}
							break;
						case NONRIGID:
							if (ai.isRigid())
							{
								ai.setRigid(false);
								numSet++;
							}
							break;
						case FIXEDANGLE:
							if (!ai.isFixedAngle())
							{
								ai.setFixedAngle(true);
								numSet++;
							}
							break;
						case NONFIXEDANGLE:
							if (ai.isFixedAngle())
							{
								ai.setFixedAngle(false);
								numSet++;
							}
							break;
						case DIRECTIONAL:		// toggle directionality
							if (ai.isHeadArrowed())
							{
								ai.setHeadArrowed(false);
								ai.setBodyArrowed(false);
								numUnset++;
							} else
							{
								ai.setHeadArrowed(true);
								ai.setBodyArrowed(true);
								numSet++;
							}
							break;
						case HEADEXTEND:		// end-extend the head
							if (ai.isHeadExtended())
							{
								ai.setHeadExtended(false);
								numUnset++;
							} else
							{
								ai.setHeadExtended(true);
								numSet++;
							}
							break;
						case TAILEXTEND:		// end-extend the tail
							if (ai.isTailExtended())
							{
								ai.setTailExtended(false);
								numUnset++;
							} else
							{
								ai.setTailExtended(true);
								numSet++;
							}
							break;
					}
				}
			}

			repaintAny = false;
			if (numSet == 0 && numUnset == 0) System.out.println("No changes were made"); else
			{
				String action = "";
				repaintAny = true;
				repaintContents = false;
				action = how.toString();

				switch (how)
				{
					case DIRECTIONAL:
					case HEADEXTEND:
					case TAILEXTEND:
						repaintContents = true;   break;
				}
				if (numUnset == 0) System.out.println("Made " + numSet + " arcs " + action); else
					if (numSet == 0) System.out.println("Made " + numUnset + " arcs not " + action); else
						System.out.println("Made " + numSet + " arcs " + action + "; and " + numUnset + " arcs not " + action);
			}
			fieldVariableChanged("repaintAny");
			fieldVariableChanged("repaintContents");
			return true;
		}

		public void terminateOK()
		{
			if (repaintAny)
			{
				if (repaintContents) EditWindow.repaintAllContents(); else
					EditWindow.repaintAll();
			}
		}
	}

	public static class ToggleNegationJob extends Job
	{
		private Cell cell;
		private List<ElectricObject> highlighted; // Can't use Highlight since it is not serializable
		private int numSet;

		public ToggleNegationJob(Cell cell, List<Highlight> highlighted)
		{
			super("Toggle negation", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.cell = cell;
			this.highlighted = new ArrayList<ElectricObject>();
			for(Highlight h : highlighted)
			{
				if (!h.isHighlightEOBJ()) continue;
				this.highlighted.add(h.getElectricObject());
			}
			startJob();
		}

		public boolean doIt() throws JobException
		{
			// make sure negation is allowed
			if (cantEdit(cell, null, true, false, true) != 0) return false;

			numSet = 0;
			for(ElectricObject eobj : highlighted)
			{
				if (eobj instanceof PortInst)
				{
					PortInst pi = (PortInst)eobj;
					for(Iterator<Connection> cIt = pi.getConnections(); cIt.hasNext(); )
					{
						Connection con = cIt.next();
						con.setNegated(!con.isNegated());
					}
				}
				if (eobj instanceof ArcInst)
				{
					ArcInst ai = (ArcInst)eobj;
					for(int i=0; i<2; i++)
					{
						ai.setNegated(i, !ai.isNegated((i)));
					}
				}
			}
			fieldVariableChanged("numSet");
			if (numSet == 0) System.out.println("No ports negated"); else
			{
				System.out.println("Negated " + numSet + " ports");
			}
			return true;
		}

		public void terminateOK()
		{
			if (numSet != 0)
			{
				EditWindow.repaintAllContents();
			}
		}
	}

	public static class RipTheBus extends Job
	{
		private Cell cell;
		private List<ArcInst> list;

		public RipTheBus(Cell cell, List<ArcInst> list)
		{
			super("Rip Bus", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.cell = cell;
			this.list = list;
			startJob();
		}

		public boolean doIt() throws JobException
		{
			// make sure ripping arcs is allowed
			if (cantEdit(cell, null, true, false, true) != 0) return false;

			for(ArcInst ai : list)
			{
				if (ai.getProto() != Schematics.tech().bus_arc) continue;
				Netlist netList = ai.getParent().getNetlist();
				if (netList == null)
				{
					System.out.println("Sorry, a deadlock aborted bus ripping (network information unavailable).  Please try again");
					break;
				}
				int busWidth = netList.getBusWidth(ai);
				String netName = netList.getNetworkName(ai);
				if (netName.length() == 0)
				{
					System.out.println("Bus " + ai.describe(true) + " has no name");
					continue;
				}

				// determine length of stub wires
				double stublen = (int)(ai.getLambdaLength() / 3 + 0.5);
				double lowXBus = 0, lowYBus = 0;
				int lowEnd = 1;
				double sepX = 0, sepY = 0;
				double lowX = 0, lowY = 0;

				// determine location of individual signals
				if (ai.getHeadLocation().getX() == ai.getTailLocation().getX())
				{
					lowX = ai.getHeadLocation().getX();
					if (lowX < ai.getParent().getBounds().getCenterX()) lowX += stublen; else
						lowX -= stublen;

					if (ai.getLocation(0).getY() < ai.getLocation(1).getY()) lowEnd = 0;
					lowY = (int)(ai.getLocation(lowEnd).getY());
					double highy = (int)(ai.getLocation(1-lowEnd).getY());
					if (highy-lowY >= busWidth-1)
					{
						// signals fit on grid
						sepY = (int)((highy-lowY) / (busWidth-1));
						lowY = (int)(((highy - lowY) - (sepY * (busWidth-1))) / 2 + lowY);
					} else
					{
						// signals don't fit: just make them even
						lowY = ai.getLocation(lowEnd).getY();
						highy = ai.getLocation(1-lowEnd).getY();
						sepY = (highy-lowY) / (busWidth-1);
					}
					lowXBus = ai.getTailLocation().getX();   lowYBus = lowY;
				} else if (ai.getTailLocation().getY() == ai.getHeadLocation().getY())
				{
					lowY = ai.getTailLocation().getY();
					if (lowY < ai.getParent().getBounds().getCenterY()) lowY += stublen; else
						lowY -= stublen;

					if (ai.getLocation(0).getX() < ai.getLocation(1).getX()) lowEnd = 0;
					lowX = (int)(ai.getLocation(lowEnd).getX());
					double highx = (int)(ai.getLocation(1-lowEnd).getX());
					if (highx-lowX >= busWidth-1)
					{
						// signals fit on grid
						sepX = (int)((highx-lowX) / (busWidth-1));
						lowX = (int)(((highx - lowX) - (sepX * (busWidth-1))) / 2 + lowX);
					} else
					{
						// signals don't fit: just make them even
						lowX = ai.getLocation(lowEnd).getX();
						highx = ai.getLocation(1-lowEnd).getX();
						sepX = (highx-lowX) / (busWidth-1);
					}
					lowXBus = lowX;   lowYBus = ai.getTailLocation().getY();
				} else
				{
					System.out.println("Bus " + ai.describe(true) + " must be horizontal or vertical to be ripped out");
					continue;
				}

				// copy names to a local array
				String [] localStrings = new String[busWidth];
				for(int i=0; i<busWidth; i++)
				{
					Network subNet = netList.getNetwork(ai, i);
					localStrings[i] = subNet.getName();
				}

				double sxw = Schematics.tech().wirePinNode.getDefWidth();
				double syw = Schematics.tech().wirePinNode.getDefHeight();
				double sxb = Schematics.tech().busPinNode.getDefWidth();
				double syb = Schematics.tech().busPinNode.getDefHeight();
				ArcProto apW = Schematics.tech().wire_arc;
				ArcProto apB = Schematics.tech().bus_arc;
				NodeInst niBLast = null;
				for(int i=0; i<busWidth; i++)
				{
					// make the wire pin
					NodeInst niw = NodeInst.makeInstance(Schematics.tech().wirePinNode, new Point2D.Double(lowX, lowY), sxw, syw, ai.getParent());
					if (niw == null) break;

					// make the bus pin
					NodeInst nib = NodeInst.makeInstance(Schematics.tech().busPinNode, new Point2D.Double(lowXBus, lowYBus), sxb, syb, ai.getParent());
					if (nib == null) break;

					// wire them
					PortInst head = niw.getOnlyPortInst();
					PortInst tail = nib.getOnlyPortInst();
					ArcInst aiw = ArcInst.makeInstance(apW, head, tail);
					if (aiw == null) break;
					aiw.setName(localStrings[i]);

					// wire to the bus pin
					if (i == 0)
					{
						PortInst first = ai.getPortInst(lowEnd);
						aiw = ArcInst.makeInstance(apB, first, tail);
					} else
					{
						PortInst first = niBLast.getOnlyPortInst();
						aiw = ArcInst.makeInstance(apB, first, tail);
					}
					if (aiw == null) break;

					// advance to the next segment
					niBLast = nib;
					lowX += sepX;     lowY += sepY;
					lowXBus += sepX;  lowYBus += sepY;
				}

				// wire up the last segment
				PortInst head = niBLast.getOnlyPortInst();
				PortInst tail = ai.getPortInst(1-lowEnd);
				ArcInst aiw = ArcInst.makeInstance(apB, head, tail);
				if (aiw == null) return false;
				aiw.setName(netName);

				// remove original arc
				ai.kill();
			}
			return true;
		}
	}

	/****************************** DELETE SELECTED OBJECTS ******************************/

	public static class DeleteSelected extends Job
	{
		private Cell cell;
		private List<DisplayedText> highlightedText;
		private List<Geometric> highlighted;
		private boolean reconstructArcsAndExports;
		private List<Geometric> thingsToHighlight;

		public DeleteSelected(Cell cell, List<DisplayedText> highlightedText, List<Geometric> highlighted, boolean reconstructArcsAndExports)
		{
			super("Delete selected objects", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.cell = cell;
			this.highlightedText = highlightedText;
			this.highlighted = highlighted;
			this.reconstructArcsAndExports = reconstructArcsAndExports;
			startJob();
		}

		public boolean doIt() throws JobException
		{
			// make sure deletion is allowed
			if (cantEdit(cell, null, true, false, true) != 0) return false;

			// delete the text
			for(DisplayedText dt : highlightedText)
			{
				// deleting variable on object
				Variable.Key key = dt.getVariableKey();
				ElectricObject eobj = dt.getElectricObject();

                // delete any reference to this Object in SizeListener
                SizeListener.restorePreviousListener(eobj);

                if (key == NodeInst.NODE_NAME)
				{
					// deleting the name of a node
					NodeInst ni = (NodeInst)eobj;
					ni.setName(null);
					ni.move(0, 0);
				} else if (key == ArcInst.ARC_NAME)
				{
					// deleting the name of an arc
					ArcInst ai = (ArcInst)eobj;
					ai.setName(null);
					ai.modify(0, 0, 0, 0);
				} else if (key == Export.EXPORT_NAME)
				{
					// deleting the name of an export
					Export pp = (Export)eobj;
					int errCode = cantEdit(cell, pp.getOriginalPort().getNodeInst(), true, true, true);
					if (errCode < 0) return false;
					if (errCode > 0) continue;
					pp.kill();
				} else
				{
					// deleting a variable
					if (eobj.isParam(key)) {
						if (eobj instanceof Cell)
							((Cell)eobj).getCellGroup().delParam((Variable.AttrKey)key);
						else if (eobj instanceof NodeInst)
							((NodeInst)eobj).delParameter(key);
					} else {
						eobj.delVar(key);
					}
				}
			}
			if (cell != null)
			{
				thingsToHighlight = new ArrayList<Geometric>();
				Set<ElectricObject> stuffToHighlight = new HashSet<ElectricObject>();
				eraseObjectsInList(cell, highlighted, reconstructArcsAndExports, stuffToHighlight);
				for(ElectricObject eObj : stuffToHighlight)
				{
					if (eObj instanceof ArcInst)
					{
						ArcInst ai = (ArcInst)eObj;
						thingsToHighlight.add(ai);
					} else if (eObj instanceof Export)
					{
						Export e = (Export)eObj;
						thingsToHighlight.add(e.getOriginalPort().getNodeInst());
					}
				}
				fieldVariableChanged("thingsToHighlight");
			}

			return true;
		}

		public void terminateOK()
		{
			// remove highlighting, show only reconstructed objects
			UserInterface ui = Job.getUserInterface();
			EditWindow_ wnd = ui.getCurrentEditWindow_();
			if (wnd != null)
			{
				wnd.clearHighlighting();
				if (thingsToHighlight != null)
				{
					for(Geometric geom: thingsToHighlight)
						wnd.addElectricObject(geom, cell);
				}
				wnd.finishedHighlighting();
			}
		}
	}

	public static class DeleteSelectedGeometry extends Job
	{
		private Cell cell;
		private ERectangle bounds;

		public DeleteSelectedGeometry(Cell cell, Rectangle2D bounds)
		{
			super("Delete selected geometry", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.cell = cell;
			this.bounds = ERectangle.fromLambda(bounds);
			startJob();
		}

		public boolean doIt() throws JobException
		{
			// disallow erasing if lock is on
			if (cantEdit(cell, null, true, false, true) != 0) return false;

			// grid the area
			double lX = Math.floor(bounds.getMinX());
			double hX = Math.ceil(bounds.getMaxX());
			double lY = Math.floor(bounds.getMinY());
			double hY = Math.ceil(bounds.getMaxY());

			// crop arcs that cross the area boundary
			List<ArcInst> arcsInCell = new ArrayList<ArcInst>();
			for(Iterator<ArcInst> aIt = cell.getArcs(); aIt.hasNext(); )
				arcsInCell.add(aIt.next());
			for(ArcInst ai : arcsInCell)
			{
				// if an end is inside, ignore
				Point2D headPt = ai.getHeadLocation();
				Point2D tailPt = ai.getTailLocation();

				// if length is zero, ignore
				if (tailPt.getX() == headPt.getX() &&
					tailPt.getY() == headPt.getY()) continue;

				// if the arc doesn't intersect the area, ignore
				double halfWidth = ai.getLambdaBaseWidth() / 2;
				double lXExt = lX - halfWidth;
				double hXExt = hX + halfWidth;
				double lYExt = lY - halfWidth;
				double hYExt = hY + halfWidth;
				Point2D tailPtAdj = new Point2D.Double(tailPt.getX(), tailPt.getY());
				Point2D headPtAdj = new Point2D.Double(headPt.getX(), headPt.getY());
				if (DBMath.clipLine(tailPtAdj, headPtAdj, lXExt, hXExt, lYExt, hYExt)) continue;
				if (tailPtAdj.distance(headPt) + headPtAdj.distance(tailPt) <
					headPtAdj.distance(headPt) + tailPtAdj.distance(tailPt))
				{
					Point2D swap = headPtAdj;
					headPtAdj = tailPtAdj;
					tailPtAdj = swap;
				}
				Name name = ai.getNameKey();
				String newName = null;
				if (!name.isTempname()) newName = name.toString();
				if (!tailPt.equals(tailPtAdj))
				{
					// create a pin at this point
					PrimitiveNode pin = ai.getProto().findPinProto();
					NodeInst ni = NodeInst.makeInstance(pin, tailPtAdj, pin.getDefWidth(), pin.getDefHeight(), cell);
					if (ni == null)
					{
						System.out.println("Error creating pin for shortening of "+ai);
						continue;
					}

					ArcInst ai1 = ArcInst.makeInstanceBase(ai.getProto(), ai.getLambdaBaseWidth(),
						ai.getTailPortInst(), ni.getOnlyPortInst(), ai.getTailLocation(),
							tailPtAdj, newName);
					if (ai1 == null)
					{
						System.out.println("Error shortening "+ai);
						continue;
					}
					newName = null;
					ai1.copyPropertiesFrom(ai);
				}
				if (!headPt.equals(headPtAdj))
				{
					// create a pin at this point
					PrimitiveNode pin = ai.getProto().findPinProto();
					NodeInst ni = NodeInst.makeInstance(pin, headPtAdj, pin.getDefWidth(), pin.getDefHeight(), cell);
					if (ni == null)
					{
						System.out.println("Error creating pin for shortening of "+ai);
						continue;
					}

					ArcInst ai1 = ArcInst.makeInstanceBase(ai.getProto(), ai.getLambdaBaseWidth(), ni.getOnlyPortInst(),
						ai.getHeadPortInst(), headPtAdj, ai.getHeadLocation(), newName);
					if (ai1 == null)
					{
						System.out.println("Error shortening "+ai);
						continue;
					}
					ai1.copyPropertiesFrom(ai);
				}

                // delete any reference to this Object in SizeListener
                SizeListener.restorePreviousListener(ai);

                ai.kill();
			}

			// now remove nodes in the area
			Set<NodeInst> nodesToDelete = new HashSet<NodeInst>();
			for(Iterator<NodeInst> nIt = cell.getNodes(); nIt.hasNext(); )
			{
				NodeInst ni = nIt.next();

				// special case for nodes with outline information: clip the outline
				if (!ni.isCellInstance())
				{
					if (ni.getProto().getFunction() == PrimitiveNode.Function.NODE)
					{
						// first see if it is completely ignored
						Rectangle2D pointBounds = ni.getBounds();
						if (pointBounds.getMinX() > hX || pointBounds.getMaxX() < lX ||
							pointBounds.getMinY() > hY || pointBounds.getMaxY() < lY) continue;

						// if it cannot be modified, stop
						int errorCode = cantEdit(cell, ni, true, false, true);
						if (errorCode < 0) return false;
						if (errorCode > 0) continue;

						// if it is completely covered, delete it
						if (pointBounds.getMinX() >= lX && pointBounds.getMaxX() <= hX &&
							pointBounds.getMinY() >= lY && pointBounds.getMaxY() <= hY)
						{
							nodesToDelete.add(ni);
							continue;
						}

						// crop it against the delete bounds
						Layer lay = ni.getProto().getTechnology().getLayer(0);
						PolyMerge merge = new PolyMerge();
						PolyBase poly;
						Point2D [] points = ni.getTrace();
						if (points == null) poly = new PolyBase(pointBounds); else
						{
							double cX = pointBounds.getCenterX();
							double cY = pointBounds.getCenterY();
							Point2D [] newPoints = new Point2D[points.length];
							for(int i=0; i<points.length; i++)
							{
								if (points[i] != null)
									newPoints[i] = new Point2D.Double(points[i].getX()+cX, points[i].getY()+cY);
							}
							poly = new PolyBase(newPoints);
							poly.transform(ni.rotateOut());
						}
						merge.addPolygon(lay, poly);
						PolyBase polySub = new PolyBase((lX+hX)/2, (lY+hY)/2, hX-lX, hY-lY);
						merge.subtract(lay, polySub);
						List<PolyBase> resultingPolys = merge.getMergedPoints(lay, true);
						if (resultingPolys != null)
						{
							// find the largest polygon
							PolyBase largest = null;
							double largestSize = 0;
							for(PolyBase pb : resultingPolys)
							{
								double sz = pb.getArea();
								if (sz >= largestSize)
								{
									largestSize = sz;
									largest = pb;
								}
							}
							if (largest != null)
								ni.setTrace(largest.getPoints());
							continue;
						}
					}
				}

				// if the node is outside of the area, ignore it
				double cX = ni.getTrueCenterX();
				double cY = ni.getTrueCenterY();
				if (cX >= hX || cX <= lX || cY >= hY || cY <= lY) continue;

				// if it cannot be modified, stop
				int errorCode = cantEdit(cell, ni, true, false, true);
				if (errorCode < 0) return false;
				if (errorCode > 0) continue;
				nodesToDelete.add(ni);
                // delete any reference to this Object in SizeListener
                SizeListener.restorePreviousListener(ni);
            }

			// delete the nodes
			cell.killNodes(nodesToDelete);
			return true;
		}
	}

	public static class DeleteArcs extends Job
	{
		private Set<ArcInst> arcsToDelete;

		public DeleteArcs(Set<ArcInst> arcsToDelete)
		{
			super("Delete arcs", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.arcsToDelete = arcsToDelete;
			startJob();
		}

		public boolean doIt() throws JobException
		{
			for(ArcInst ai : arcsToDelete)
			{
				NodeInst h = ai.getHeadPortInst().getNodeInst();
				NodeInst t = ai.getTailPortInst().getNodeInst();
				ai.kill();

				// also delete freed pin nodes
				if (h.getProto().getFunction().isPin() &&
					!h.hasConnections() && !h.hasExports())
				{
					h.kill();
				}
				if (t.getProto().getFunction().isPin() &&
					!t.hasConnections() && !t.hasExports())
				{
					t.kill();
				}
			}
			System.out.println("Deleted " + arcsToDelete.size() + " arcs");
			return true;
		}
	}

	/****************************** DELETE OBJECTS IN A LIST ******************************/

	/**
	 * Method to delete all of the Geometrics in a list.
	 * @param cell the cell with the objects to be deleted.
	 * @param list a List of Geometric or Highlight objects to be deleted.
	 * @param reconstructArcsAndExports true to reconstruct arcs to deleted cell instances.
	 * @param stuffToHighlight a set of objects to select (arcs and exports) if reconstruction is done.
	 */
	public static void eraseObjectsInList(Cell cell, List<Geometric> list, boolean reconstructArcsAndExports,
		Set<ElectricObject> stuffToHighlight)
	{
		// make sets of all of the arcs and nodes explicitly selected for deletion
		Set<ArcInst> arcsToDelete = new HashSet<ArcInst>();
		Set<NodeInst> nodesToDelete = new HashSet<NodeInst>();
		if (cantEdit(cell, null, true, false, true) != 0) return;
		for(Geometric geom : list)
		{
			if (geom instanceof ArcInst)
			{
				ArcInst ai = (ArcInst)geom;
				arcsToDelete.add(ai);
			} else if (geom instanceof NodeInst)
			{
				NodeInst ni = (NodeInst)geom;
				if (cantEdit(cell, ni, true, false, true) != 0) continue;
				nodesToDelete.add(ni);
			}
		}

		// make a set of additional nodes to potentially delete
		Set<NodeInst> alsoDeleteTheseNodes = new HashSet<NodeInst>();

		// also (potentially) delete nodes on the end of deleted arcs
		for(ArcInst ai : arcsToDelete)
		{
			alsoDeleteTheseNodes.add(ai.getHeadPortInst().getNodeInst());
			alsoDeleteTheseNodes.add(ai.getTailPortInst().getNodeInst());
		}

		// also mark all nodes on the other end of arcs connected to erased nodes
		for(NodeInst ni : nodesToDelete)
		{
			for(Iterator<Connection> sit = ni.getConnections(); sit.hasNext(); )
			{
				Connection con = sit.next();
				ArcInst ai = con.getArc();
				int otherEnd = 1 - con.getEndIndex();
				NodeInst oNi = ai.getPortInst(otherEnd).getNodeInst();
				alsoDeleteTheseNodes.add(oNi);
			}
		}

		// reconnect hair to cells (if requested)
		if (reconstructArcsAndExports)
		{
			for(NodeInst ni : nodesToDelete)
			{
				if (!ni.isCellInstance()) continue;

				// make a map of recreated ports
				Map<PortInst,PortInst> reassigned = new HashMap<PortInst,PortInst>();

				// reconstruct exports to deleted cell instances
				for(Iterator<Export> eIt = ni.getExports(); eIt.hasNext(); )
				{
					Export e = eIt.next();
					AffineTransform trans = new AffineTransform();
					Orientation orient = Orientation.IDENT;
					PortInst pi = e.getOriginalPort();
					NodeInst subNi;
					PortProto subPP;
					for(;;)
					{
						subNi = pi.getNodeInst();
						subPP = pi.getPortProto();
						trans = subNi.rotateOut(trans);
						orient = orient.concatenate(subNi.getOrient());
						if (!subNi.isCellInstance()) break;
						trans = subNi.translateOut(trans);
						pi = ((Export)subPP).getOriginalPort();
					}
					NodeProto subNp = subNi.getProto();
					Point2D ctr = new Point2D.Double(subNi.getTrueCenter().getX(), subNi.getTrueCenter().getY());
					trans.transform(ctr, ctr);
					NodeInst eNi = NodeInst.makeInstance(subNp, ctr, subNi.getXSize(), subNi.getYSize(),
						cell, orient, null);
					pi = eNi.findPortInstFromProto(subPP);
					reassigned.put(e.getOriginalPort(), pi);
					e.move(pi);
					if (stuffToHighlight != null) stuffToHighlight.add(e);
				}

				// reconstruct each connection to a deleted cell instance
				for(Iterator<Connection> cIt = ni.getConnections(); cIt.hasNext(); )
				{
					Connection con = cIt.next();
					ArcInst ai = con.getArc();
					if (arcsToDelete.contains(ai)) continue;

					// recreate them
					int thisEnd = con.getEndIndex();
					int otherEnd = 1 - thisEnd;
					PortInst thisPi = ai.getPortInst(thisEnd);
					PortInst otherPi = ai.getPortInst(otherEnd);
					NodeInst otherNi = otherPi.getNodeInst();
					if (otherNi == ni)
					{
						// special case: arc from node to itself gets preserved?
						continue;
					}
					if (nodesToDelete.contains(otherNi)) continue;

					// reconnect a piece of hair to a cell instance
					PortInst alreadyPI = reassigned.get(thisPi);
					if (alreadyPI == null)
					{
						PrimitiveNode pinNp = ai.getProto().findPinProto();
						NodeInst pin = NodeInst.makeInstance(pinNp, con.getLocation(), pinNp.getDefWidth(), pinNp.getDefHeight(), cell);
						alreadyPI = pin.getOnlyPortInst();
						reassigned.put(thisPi, alreadyPI);
					}
					ArcInst newAI = ArcInst.makeInstanceBase(ai.getProto(), ai.getLambdaBaseWidth(), otherPi, alreadyPI,
						ai.getConnection(otherEnd).getLocation(), con.getLocation(), ai.getName());
					if (stuffToHighlight != null) stuffToHighlight.add(newAI);
					alsoDeleteTheseNodes.remove(otherNi);
				}
			}
		}

		// now kill all of the arcs
		for(ArcInst ai : arcsToDelete)
		{
			ai.kill();
		}

		// reconnect all nodes that are in the middle of two arcs
		for(NodeInst ni : nodesToDelete)
		{
//			if (reconstructArcsAndExports && ni.isCellInstance()) continue;

			// see if any arcs can be reconnected as a result of this kill
			Reconnect re = Reconnect.erasePassThru(ni, false, false);
			if (re != null) re.reconnectArcs();
		}

		// next kill all of the nodes
		cell.killNodes(nodesToDelete);

		// kill all pin nodes that touched an arc and no longer do
		Set<NodeInst> deleteTheseNodes = new HashSet<NodeInst>();
		for(NodeInst ni : alsoDeleteTheseNodes)
		{
			if (!ni.isLinked()) continue;
			if (!ni.isCellInstance())
			{
				if (!ni.getProto().getFunction().isPin()) continue;
				if (ni.hasConnections() || ni.hasExports()) continue;
				deleteTheseNodes.add(ni);
			}
		}
		cell.killNodes(deleteTheseNodes);

		// kill all unexported pin or bus nodes left in the middle of arcs
		List<NodeInst> nodesToPassThru = new ArrayList<NodeInst>();
		for(NodeInst ni : alsoDeleteTheseNodes)
		{
			if (!ni.isCellInstance())
			{
				if (!ni.getProto().getFunction().isPin()) continue;
				if (ni.hasExports()) continue;
				if (!ni.isInlinePin()) continue;
				nodesToPassThru.add(ni);
			}
		}

		for(NodeInst ni : nodesToPassThru)
		{
			Reconnect re = Reconnect.erasePassThru(ni, false, false);
			if (re != null)
			{
				re.reconnectArcs();
				ni.kill();
			}
		}

		int numArcsDeleted = arcsToDelete.size();
		int numNodesDeleted = nodesToDelete.size();
		String msg = "Deleted";
		if (numNodesDeleted == 1) msg += " 1 node"; else
			if (numNodesDeleted > 1) msg += " " + numNodesDeleted + " nodes";
		if (numNodesDeleted > 0 && numArcsDeleted > 0) msg += " and";
		if (numArcsDeleted == 1) msg += " 1 arc"; else
			if (numArcsDeleted > 1) msg += " " + numArcsDeleted + " arcs";
		System.out.println(msg);
	}

	/****************************** CLEAN-UP ******************************/

	/**
	 * This class implements the changes needed to cleanup pins in a Cell.
	 */
	public static class CleanupChanges extends Job
	{
		private Cell cell;
		private boolean justThis;
		private Set<NodeInst> pinsToRemove;
		private List<Reconnect> pinsToPassThrough;
		private Map<NodeInst,EPoint> pinsToScale;
		private List<NodeInst> textToMove;
		private Set<ArcInst> arcsToKill;
		private int zeroSize, negSize, overSizePins;

		public CleanupChanges(Cell cell, boolean justThis, Set<NodeInst> pinsToRemove, List<Reconnect> pinsToPassThrough,
			Map<NodeInst,EPoint> pinsToScale, List<NodeInst> textToMove, Set<ArcInst> arcsToKill,
			int zeroSize, int negSize, int overSizePins)
		{
			super("Cleanup " + cell, User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.cell = cell;
			this.justThis = justThis;
			this.pinsToRemove = pinsToRemove;
			this.pinsToPassThrough = pinsToPassThrough;
			this.pinsToScale = pinsToScale;
			this.textToMove = textToMove;
			this.arcsToKill = arcsToKill;
			this.zeroSize = zeroSize;
			this.negSize = negSize;
			this.overSizePins = overSizePins;
			startJob();
		}

		public boolean doIt() throws JobException
		{
			// make sure moving the node is allowed
			if (cantEdit(cell, null, true, false, true) != 0) return false;

			// do the queued operations
			cell.killNodes(pinsToRemove);
			int pinsPassedThrough = 0;
			for(;;)
			{
				boolean found = false;
				for(Reconnect re : pinsToPassThrough)
				{
					if (!re.ni.isLinked()) continue;
					List<ArcInst> created = re.reconnectArcs();
					if (created.size() > 0)
					{
						re.ni.kill();
						pinsPassedThrough++;
						found = true;
					}
				}
				if (!found) break;
				pinsToPassThrough = getPinsToPassThrough(cell);
			}
			for(NodeInst ni : pinsToScale.keySet())
			{
				EPoint scale = pinsToScale.get(ni);
				ni.resize(scale.getX(), scale.getY());
			}
			for(NodeInst ni : textToMove)
			{
				ni.invisiblePinWithOffsetText(true);
			}
            cell.killArcs(arcsToKill);

			// report what was cleaned
			StringBuffer infstr = new StringBuffer();
			if (!justThis) infstr.append("Cell " + cell.describe(true) + ":");
			boolean spoke = false;
			if ((pinsToRemove.size()+pinsPassedThrough) != 0)
			{
				int removed = pinsToRemove.size() + pinsPassedThrough;
				infstr.append("Removed " + removed + " pins");
				spoke = true;
			}
			if (arcsToKill.size() != 0)
			{
				if (spoke) infstr.append("; ");
				infstr.append("Removed " + arcsToKill.size() + " duplicate arcs");
				spoke = true;
			}
			if (pinsToScale.size() != 0)
			{
				if (spoke) infstr.append("; ");
				infstr.append("Shrunk " + pinsToScale.size() + " pins");
				spoke = true;
			}
			if (zeroSize != 0)
			{
				if (spoke) infstr.append("; ");
				if (justThis)
				{
					infstr.append("Highlighted " + zeroSize + " zero-size pins");
				} else
				{
					infstr.append("Found " + zeroSize + " zero-size pins");
				}
				spoke = true;
			}
			if (negSize != 0)
			{
				if (spoke) infstr.append("; ");
				if (justThis)
				{
					infstr.append("Highlighted " + negSize + " negative-size pins");
				} else
				{
					infstr.append("Found " + negSize + " negative-size pins");
				}
				spoke = true;
			}
			if (overSizePins != 0)
			{
				if (spoke) infstr.append("; ");
				if (justThis)
				{
					infstr.append("Highlighted " + overSizePins + " oversize pins with arcs that don't touch");
				} else
				{
					infstr.append("Found " + overSizePins + " oversize pins with arcs that don't touch");
				}
				spoke = true;
			}
			if (textToMove.size() != 0)
			{
				if (spoke) infstr.append("; ");
				infstr.append("Moved text on " + textToMove.size() + " pins with offset text");
			}
			System.out.println(infstr.toString());
			return true;
		}
	}

	/**
	 * This class implements the changes needed to shorten selected arcs.
	 */
	public static class ShortenArcs extends Job
	{
		private Cell cell;
		private List<ArcInst> selected;

		public ShortenArcs(Cell cell, List<ArcInst> selected)
		{
			super("Shorten selected arcs", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.cell = cell;
			this.selected = selected;
			startJob();
		}

		public boolean doIt() throws JobException
		{
			// make sure shortening is allowed
			if (cantEdit(cell, null, true, false, true) != 0) return false;

			int l = 0;
			double [] dX = new double[2];
			double [] dY = new double[2];
			for(ArcInst ai : selected)
			{
				for(int j=0; j<2; j++)
				{
					Poly portPoly = ai.getPortInst(j).getPoly();
					double wid = ai.getLambdaBaseWidth();
					portPoly.reducePortPoly(ai.getPortInst(j), wid, ai.getAngle());
					Point2D closest = portPoly.closestPoint(ai.getLocation(1-j));
					dX[j] = closest.getX() - ai.getLocation(j).getX();
					dY[j] = closest.getY() - ai.getLocation(j).getY();
				}
				if (dX[0] != 0 || dY[0] != 0 || dX[1] != 0 || dY[1] != 0)
				{
					ai.modify(dX[ArcInst.HEADEND], dY[ArcInst.HEADEND], dX[ArcInst.TAILEND], dY[ArcInst.TAILEND]);
					l++;
				}
			}
			System.out.println("Shortened " + l + " arcs");
			return true;
		}
	}

	/****************************** MOVE SELECTED OBJECTS ******************************/

	public static class ManyMove extends Job
	{
		private static final boolean verbose = true;
		private Cell cell;
		private List<ElectricObject> highlightedObjs;
		private List<DisplayedText> highlightedText;
		private double dX, dY;
		private boolean updateStatusBar;

		public ManyMove(Cell cell, List<ElectricObject> highlightedObjs, List<DisplayedText> highlightedText, double dX, double dY)
		{
			super("Move", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.cell = cell;
			this.highlightedObjs = highlightedObjs;
			this.highlightedText = highlightedText;
			this.dX = dX;
			this.dY = dY;
			startJob();
		}

		public boolean doIt() throws JobException
		{
			// get information about what is highlighted
			if (highlightedObjs.size() + highlightedText.size() == 0) return false;

			// make sure moving is allowed
			if (cantEdit(cell, null, true, false, true) != 0) return false;

			// special case if moving only one node
			if (highlightedObjs.size() == 1 && highlightedText.size() == 0)
			{
				ElectricObject firstEObj = highlightedObjs.get(0);
				if (firstEObj instanceof NodeInst || firstEObj instanceof PortInst)
				{
					NodeInst ni;
					if (firstEObj instanceof PortInst) {
						ni = ((PortInst)firstEObj).getNodeInst();
					} else {
						ni = (NodeInst)firstEObj;
					}

					// make sure moving the node is allowed
					if (cantEdit(cell, ni, true, false, true) != 0) return false;

					ni.move(dX, dY);
					if (verbose) System.out.println("Moved node " + ni.describe(false) + " by (" + dX + "," + dY + ")");
					updateStatusBar = true;
					fieldVariableChanged("updateStatusBar");
					return true;
				}
			}

			// special case if moving diagonal fixed-angle arcs connected to single manhattan arcs
			boolean found = false;
			for(ElectricObject eobj : highlightedObjs)
			{
				if (eobj instanceof ArcInst)
				{
					ArcInst ai = (ArcInst)eobj;
					if (ai.getHeadLocation().getX() != ai.getTailLocation().getX() &&
						ai.getHeadLocation().getY() != ai.getTailLocation().getY())
					{
						if (ai.isFixedAngle() && !ai.isRigid())
						{
							int j;
							for(j=0; j<2; j++)
							{
								NodeInst ni = ai.getPortInst(j).getNodeInst();
								ArcInst oai = null;
								for(Iterator<Connection> pIt = ni.getConnections(); pIt.hasNext(); )
								{
									Connection con = pIt.next();
									if (con.getArc() == ai) continue;
									if (oai == null) oai = con.getArc(); else
									{
										oai = null;
										break;
									}
								}
								if (oai == null) break;
								if (oai.getHeadLocation().getX() != oai.getTailLocation().getX() &&
									oai.getHeadLocation().getY() != oai.getTailLocation().getY()) break;
								if (highlightedObjs.contains(oai)) break;
							}
							if (j >= 2) { found = true;   break; }
						}
					}
				}
			}
			if (found)
			{
				// meets the test: make the special move to slide other orthogonal arcs
				for(ElectricObject eobj : highlightedObjs)
				{
					if (!(eobj instanceof ArcInst)) continue;
					ArcInst ai = (ArcInst)eobj;

					double [] deltaXs = new double[2];
					double [] deltaYs = new double[2];
					NodeInst [] niList = new NodeInst[2];
					deltaXs[0] = deltaYs[0] = deltaXs[1] = deltaYs[1] = 0;
					int arcangle = ai.getAngle();
					int j;
					for(j=0; j<2; j++)
					{
						NodeInst ni = ai.getPortInst(j).getNodeInst();
						niList[j] = ni;
						ArcInst oai = null;
						for(Iterator<Connection> pIt = ni.getConnections(); pIt.hasNext(); )
						{
							Connection con = pIt.next();
							if (con.getArc() != ai) { oai = con.getArc();   break; }
						}
						if (oai == null) break;
						if (DBMath.doublesEqual(oai.getHeadLocation().getX(), oai.getTailLocation().getX()))
						{
							Point2D iPt = DBMath.intersect(oai.getHeadLocation(), 900,
								new Point2D.Double(ai.getHeadLocation().getX()+dX, ai.getHeadLocation().getY()+dY), arcangle);
							if (iPt != null)
							{
								deltaXs[j] = iPt.getX() - ai.getLocation(j).getX();
								deltaYs[j] = iPt.getY() - ai.getLocation(j).getY();
							}
						} else if (DBMath.doublesEqual(oai.getHeadLocation().getY(), oai.getTailLocation().getY()))
						{
							Point2D iPt = DBMath.intersect(oai.getHeadLocation(), 0,
								new Point2D.Double(ai.getHeadLocation().getX()+dX, ai.getHeadLocation().getY()+dY), arcangle);
							if (iPt != null)
							{
								deltaXs[j] = iPt.getX() - ai.getLocation(j).getX();
								deltaYs[j] = iPt.getY() - ai.getLocation(j).getY();
							}
						}
					}
					if (j < 2) continue;
					NodeInst.modifyInstances(niList, deltaXs, deltaYs, null, null);
				}
				if (verbose) System.out.println("Moved multiple objects by (" + TextUtils.formatDouble(dX) +
					"," + TextUtils.formatDouble(dY) + ")");
				updateStatusBar = true;
				fieldVariableChanged("updateStatusBar");
				return true;
			}

			// special case if moving only arcs and they slide
			boolean onlySlidable = true, foundArc = false;
			for(ElectricObject eobj : highlightedObjs)
			{
				if (eobj instanceof ArcInst)
				{
					ArcInst ai = (ArcInst)eobj;
					foundArc = true;
					// see if the arc moves in its ports
					if (ai.isSlidable())
					{
						Point2D newHead = new Point2D.Double(ai.getHeadLocation().getX()+dX, ai.getHeadLocation().getY()+dY);
						Point2D newTail = new Point2D.Double(ai.getTailLocation().getX()+dX, ai.getTailLocation().getY()+dY);
						if (ai.headStillInPort(newHead, true) && ai.tailStillInPort(newTail, true)) continue;
					}
				}
				onlySlidable = false;
			}
			if (foundArc && onlySlidable)
			{
				for(ElectricObject eobj : highlightedObjs)
				{
					if (eobj instanceof ArcInst)
					{
						ArcInst ai = (ArcInst)eobj;
						ai.modify(dX, dY, dX, dY);
						if (verbose) System.out.println("Moved arc " + ai.describe(false) + " by (" + dX + "," + dY + ")");
					}
				}
				updateStatusBar = true;
				fieldVariableChanged("updateStatusBar");
				return true;
			}

			// ignore arc motion if the nodes are locked
			for(int i=0; i<highlightedObjs.size(); i++)
			{
				ElectricObject eobj = highlightedObjs.get(i);
				if (eobj instanceof ArcInst)
				{
					ArcInst ai = (ArcInst)eobj;
					int errorCode = cantEdit(cell, ai.getHeadPortInst().getNodeInst(), true, false, true);
					if (errorCode < 0) return false;
					if (errorCode > 0)
					{
						highlightedObjs.remove(i);
						i--;
						continue;
					}

					errorCode = cantEdit(cell, ai.getTailPortInst().getNodeInst(), true, false, true);
					if (errorCode < 0) return false;
					if (errorCode > 0)
					{
						highlightedObjs.remove(i);
						i--;
						continue;
					}
				}
			}

			// make flag to track the nodes that move
			Set<NodeInst> flag = new HashSet<NodeInst>();

			// remember the location of every node and arc
			Map<NodeInst,Point2D.Double> nodeLocation = new HashMap<NodeInst,Point2D.Double>();
			for(Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
			{
				NodeInst ni = it.next();
				nodeLocation.put(ni, new Point2D.Double(ni.getAnchorCenterX(), ni.getAnchorCenterY()));
			}
			Map<ArcInst,Point2D.Double> arcLocation = new HashMap<ArcInst,Point2D.Double>();
			for(Iterator<ArcInst> it = cell.getArcs(); it.hasNext(); )
			{
				ArcInst ai = it.next();
				arcLocation.put(ai, new Point2D.Double(ai.getTrueCenterX(), ai.getTrueCenterY()));
			}

			// mark all nodes that want to move
			for(ElectricObject eobj : highlightedObjs)
			{
				if (eobj instanceof PortInst) eobj = ((PortInst)eobj).getNodeInst();
				if (eobj instanceof NodeInst)
				{
					NodeInst ni = (NodeInst)eobj;
					flag.add(ni);
				} else if (eobj instanceof ArcInst)
				{
					ArcInst ai = (ArcInst)eobj;
					NodeInst ni1 = ai.getHeadPortInst().getNodeInst();
					NodeInst ni2 = ai.getTailPortInst().getNodeInst();
					flag.add(ni1);
					flag.add(ni2);
					Layout.setTempRigid(ai, true);
				}
			}

			// count the number of nodes that will move
			int numNodes = 0;
			for(Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
			{
				NodeInst ni = it.next();
				if (!flag.contains(ni)) continue;

				// make sure moving the node is allowed
				int errorCode = cantEdit(cell, ni, true, false, true);
				if (errorCode < 0) return false;
				if (errorCode > 0)
				{
					flag.remove(ni);
					continue;
				}
				numNodes++;
			}

			// look at all nodes and move them appropriately
			if (numNodes > 0)
			{
				NodeInst [] nis = new NodeInst[numNodes];
				double [] dXs = new double[numNodes];
				double [] dYs = new double[numNodes];
				numNodes = 0;
				for(Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
				{
					NodeInst ni = it.next();
					if (!flag.contains(ni)) continue;
					nis[numNodes] = ni;
					dXs[numNodes] = dX;
					dYs[numNodes] = dY;
					numNodes++;
				}
				NodeInst.modifyInstances(nis, dXs, dYs, null, null);
			}
			flag = null;

			// look at all arcs and move them appropriately
			for(ElectricObject eobj : highlightedObjs)
			{
				if (!(eobj instanceof ArcInst)) continue;
				ArcInst ai = (ArcInst)eobj;
				Point2D pt = arcLocation.get(ai);
				if (pt.getX() != ai.getTrueCenterX() ||
					pt.getY() != ai.getTrueCenterY()) continue;

				// see if the arc moves in its ports
				boolean headInPort = false, tailInPort = false;
				if (!ai.isRigid() && ai.isSlidable())
				{
					headInPort = ai.headStillInPort(
						new Point2D.Double(ai.getHeadLocation().getX()+dX, ai.getHeadLocation().getY()+dY), true);
					tailInPort = ai.tailStillInPort(
						new Point2D.Double(ai.getTailLocation().getX()+dX, ai.getTailLocation().getY()+dY), true);
				}

				// if both ends slide in their port, move the arc
				if (headInPort && tailInPort)
				{
					ai.modify(dX, dY, dX, dY);
					continue;
				}

				// if neither end can slide in its port, move the nodes
				if (!headInPort && !tailInPort)
				{
					for(int k=0; k<2; k++)
					{
						NodeInst ni;
						if (k == 0) ni = ai.getHeadPortInst().getNodeInst(); else
							ni = ai.getTailPortInst().getNodeInst();
						Point2D nPt = nodeLocation.get(ni);
						if (ni.getAnchorCenterX() != nPt.getX() || ni.getAnchorCenterY() != nPt.getY()) continue;

						// fix all arcs that aren't sliding
						for(ElectricObject oEObj : highlightedObjs)
						{
							if (oEObj instanceof ArcInst)
							{
								ArcInst oai = (ArcInst)oEObj;
								Point2D aPt = arcLocation.get(oai);
								if (aPt.getX() != oai.getTrueCenterX() ||
									aPt.getY() != oai.getTrueCenterY()) continue;
								if (oai.headStillInPort(
										new Point2D.Double(ai.getHeadLocation().getX()+dX, ai.getHeadLocation().getY()+dY), true) ||
									oai.tailStillInPort(
										new Point2D.Double(ai.getTailLocation().getX()+dX, ai.getTailLocation().getY()+dY), true))
											continue;
								Layout.setTempRigid(oai, true);
							}
						}
						ni.move(dX - (ni.getAnchorCenterX() - nPt.getX()),
							dY - (ni.getAnchorCenterY() - nPt.getY()));
					}
					continue;
				}
			}

			// also move selected text
			moveSelectedText(cell, highlightedText);
			if (verbose) System.out.println("Moved multiple objects by (" + TextUtils.formatDouble(dX) +
				"," + TextUtils.formatDouble(dY) + ")");
			updateStatusBar = true;
			fieldVariableChanged("updateStatusBar");
			return true;
		}

		public void terminateOK()
		{
			if (updateStatusBar) StatusBar.updateStatusBar();
		}

		/**
		 * Method to move the "numtexts" text objects described (as highlight strings)
		 * in the array "textlist", by "odx" and "ody".  Geometry objects in "list" (NOGEOM-terminated)
		 * and the "total" nodes in "nodelist" have already been moved, so don't move any text that
		 * is on these objects.
		 */
		private void moveSelectedText(Cell cell, List<DisplayedText> highlightedText)
		{
			for(DisplayedText dt : highlightedText)
			{
				// disallow moving if lock is on
				if (cell != null)
				{
					int errorCode = cantEdit(cell, null, true, true, true);
					if (errorCode < 0) return;
					if (errorCode > 0) continue;
				}

				// handle nodes that move with text
				ElectricObject eobj = dt.getElectricObject();
				if (dt.movesWithText())
				{
					NodeInst ni = null;
					if (eobj instanceof NodeInst) ni = (NodeInst)eobj;
					if (eobj instanceof Export) ni = ((Export)eobj).getOriginalPort().getNodeInst();
					if (ni != null)
					{
						ni.move(dX, dY);
						continue;
					}
				}

				// moving variable on object
				Variable.Key varKey = dt.getVariableKey();
				TextDescriptor td = eobj.getTextDescriptor(varKey);
				if (td == null) continue;
				NodeInst ni = null;
				if (eobj instanceof NodeInst) ni = (NodeInst)eobj; else
					if (eobj instanceof PortInst) ni = ((PortInst)eobj).getNodeInst(); else
						if (eobj instanceof Export && varKey == Export.EXPORT_NAME) ni = ((Export)eobj).getOriginalPort().getNodeInst();
				if (ni != null)
				{
					Point2D curLoc = new Point2D.Double(ni.getAnchorCenterX()+td.getXOff(), ni.getAnchorCenterY()+td.getYOff());
					AffineTransform rotateOut = ni.rotateOut();
					rotateOut.transform(curLoc, curLoc);
					curLoc.setLocation(curLoc.getX()+dX, curLoc.getY()+dY);
					AffineTransform rotateIn = ni.rotateIn();
					rotateIn.transform(curLoc, curLoc);
					eobj.setOff(varKey, curLoc.getX()-ni.getAnchorCenterX(), curLoc.getY()-ni.getAnchorCenterY());
				} else
				{
					eobj.setOff(varKey, td.getXOff()+dX, td.getYOff()+dY);
				}
			}
		}
	}

	/****************************** RECENTER CELL ON SELECTION ******************************/

	public static class CellCenterToCenterOfSelection extends Job
	{
		private Cell cell;
		private EPoint ctr;

		public CellCenterToCenterOfSelection(Cell cell, EPoint ctr)
		{
			super("Cell center to center of selection", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.cell = cell;
			this.ctr = ctr;
			startJob();
		}

		public boolean doIt() throws JobException
		{
            NodeInst center = null;
            for(NodeInst ni : i2i(cell.getNodes()))
                if (ni.getProto() == Generic.tech().cellCenterNode)
                    center = ni;
            if (center == null)
            {
                System.out.println("No cell center; please add one.");
                return false;
            }

//            System.out.println("Moving cell center to (" + ctr.getX() + "," + ctr.getY() + ")");
            center.move(ctr.getX(), ctr.getY());
            return true;
		}
	}


	/**
	 * Store information of new arc to be created that reconnects
	 * two arcs that will be deleted
	 */
	private static class ReconnectedArc implements Serializable
	{
		/** port at other end of arc */						private PortInst [] reconPi;
		/** coordinate at other end of arc */				private EPoint [] recon;
		/** old arc insts that will be deleted */           private ArcInst [] reconAr;
		/** prototype of new arc */							private ArcProto ap;
		/** width of new arc */								private double wid;
		/** true to make new arc have arrow on head */		private boolean directionalHead;
		/** true to make new arc have arrow on tail */		private boolean directionalTail;
		/** true to make new arc have arrow on body */		private boolean directionalBody;
		/** true to extend the head of the new arc */		private boolean extendHead;
		/** true to extend the tail of the new arc */		private boolean extendTail;
		/** true to negate the head of the new arc */		private boolean negateHead;
		/** true to negate the tail of the new arc */		private boolean negateTail;
		/** the name to use on the reconnected arc */		private String arcName;
		/** TextDescriptor for the reconnected arc name */	private TextDescriptor arcNameTD;
	}

	public static List<Reconnect> getPinsToPassThrough(Cell cell)
	{
		List<Reconnect> pinsToPassThrough = new ArrayList<Reconnect>();
		for(Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = it.next();
			if (!ni.getFunction().isPin()) continue;

			// if the pin is an export, save it
			if (ni.hasExports()) continue;

			// if the pin is connected to two arcs along the same slope, delete it
			if (ni.isInlinePin())
			{
				Reconnect re = Reconnect.erasePassThru(ni, false, true);
				if (re != null)
				{
					pinsToPassThrough.add(re);
				}
			}
		}
		return pinsToPassThrough;
	}

	/**
	 * This class handles deleting pins that are between two arcs,
	 * and reconnecting the arcs without the pin.
	 */
	public static class Reconnect implements Serializable
	{
		/** node in the center of the reconnect */			private NodeInst ni;
		/** list of reconnected arcs */                     private ArrayList<ReconnectedArc> reconnectedArcs;

		/**
		 * Method to find a possible Reconnect through a given NodeInst.
		 * @param ni the NodeInst to examine.
		 * @param allowdiffs true to allow differences in the two arcs.
		 * If this is false, then different width arcs, or arcs that are not lined up
		 * precisely, will not be considered for reconnection.
		 * @param checkPermission true to check that the node can be changed.
		 * @return a Reconnect object that describes the reconnection to be done.
		 * Returns null if no reconnection can be found.
		 */
		public static Reconnect erasePassThru(NodeInst ni, boolean allowdiffs, boolean checkPermission)
		{
			// stop now if nothing connected to the node
			if (!ni.hasConnections()) return null;

			// disallow erasing if lock is on
			Cell cell = ni.getParent();
			if (checkPermission && cantEdit(cell, ni, true, false, true) != 0) return null;

			Reconnect recon = new Reconnect();
			recon.ni = ni;
			recon.reconnectedArcs = new ArrayList<ReconnectedArc>();

			// get all arcs connected to each portinst on node
			for (Iterator<PortInst> it = ni.getPortInsts(); it.hasNext(); ) {
				PortInst pi = it.next();

				List<ArcInst> arcs = new ArrayList<ArcInst>();
				for (Iterator<Connection> it2 = pi.getConnections(); it2.hasNext(); ) {
					Connection conn = it2.next();
					ArcInst ai = conn.getArc();
					// ignore arcs that connect from the node to itself
					if (ai.getHeadPortInst().getNodeInst() == ai.getTailPortInst().getNodeInst())
						continue;
					arcs.add(ai);
				}

				// go through all arcs on this portinst and see if any can be reconnected
				while (arcs.size() > 1) {
					ArcInst ai1 = arcs.remove(0);
					for (ArcInst ai2 : arcs) {
						ReconnectedArc ra = reconnectArcs(pi, ai1, ai2, allowdiffs);
						// if reconnection to be made, add to list
						if (ra != null) recon.reconnectedArcs.add(ra);
					}
				}
			}
			if (recon.reconnectedArcs.size() == 0) return null;

			return recon;
		}

		/** Returns null if couldn't reconnect arcs together */
		private static ReconnectedArc reconnectArcs(PortInst pi, ArcInst ai1, ArcInst ai2, boolean allowdiffs) {

			// verify that the two arcs to merge have the same type
			if (ai1.getProto() != ai2.getProto()) return null;

			ReconnectedArc ra = new ReconnectedArc();
			ra.ap = ai1.getProto();
			ra.reconPi = new PortInst[2];
			ra.recon = new EPoint[2];
			ra.reconAr = new ArcInst[2];
			ra.reconAr[0] = ai1;
			ra.reconAr[1] = ai2;
			Point2D [] orig = new Point2D[2];               // end points on port that will be deleted
			Point2D [] delta = new Point2D[2];              // deltaX,Y of arc

			// get end points of arcs
			for (int i=0; i<2; i++) {
				if (ai1.getPortInst(i) != pi) {
					ra.reconPi[0] = ai1.getPortInst(i);
					ra.recon[0] = ai1.getLocation(i);
				} else {
					orig[0] = ai1.getLocation(i);
				}
				if (ai2.getPortInst(i) != pi) {
					ra.reconPi[1] = ai2.getPortInst(i);
					ra.recon[1] = ai2.getLocation(i);
				} else {
					orig[1] = ai2.getLocation(i);
				}
			}
			delta[0] = new Point2D.Double(ra.recon[0].getX() - orig[0].getX(),
										  ra.recon[0].getY() - orig[0].getY());
			delta[1] = new Point2D.Double(ra.recon[1].getX() - orig[1].getX(),
										  ra.recon[1].getY() - orig[1].getY());
			if (!allowdiffs)
			{
				// verify that the two arcs to merge have the same width
				if (ai1.getLambdaBaseWidth() != ai2.getLambdaBaseWidth()) return null;

				// verify that the two arcs have the same slope
				if ((delta[1].getX()*delta[0].getY()) != (delta[0].getX()*delta[1].getY())) return null;

				// verify that the angle between two arcs is obtuse
				boolean zeroLength = false;
				if (delta[0].getX() == 0 && delta[0].getY() == 0) zeroLength = true;
				if (delta[1].getX() == 0 && delta[1].getY() == 0) zeroLength = true;
				if (!zeroLength && delta[0].getX()*delta[1].getX() + delta[0].getY()*delta[1].getY() >= 0) return null;

				if (orig[0].getX() != orig[1].getX() || orig[0].getY() != orig[1].getY())
				{
					// did not connect at the same location: be sure that angle is consistent
					if (delta[0].getX() != 0 || delta[0].getY() != 0)
					{
						if (((orig[0].getX()-orig[1].getX())*delta[0].getY()) !=
							(delta[0].getX()*(orig[0].getY()-orig[1].getY()))) return null;
					} else if (delta[1].getX() != 0 || delta[1].getY() != 0)
					{
						if (((orig[0].getX()-orig[1].getX())*delta[1].getY()) !=
							(delta[1].getX()*(orig[0].getY()-orig[1].getY()))) return null;
					} else return null;
				}
			}

			// ok to connect arcs
			ra.wid = ai1.getLambdaBaseWidth();

			ra.directionalHead = ai1.isHeadArrowed();
			ra.directionalTail = ai1.isTailArrowed();
			ra.directionalBody = ai1.isBodyArrowed();
			ra.extendHead = ai1.isHeadExtended();
			ra.extendTail = ai2.isTailExtended();
			ra.negateHead = ai1.isHeadNegated();
			ra.negateTail = ai2.isTailNegated();
			if (ai1.getName() != null && !ai1.getNameKey().isTempname())
			{
				ra.arcName = ai1.getName();
				ra.arcNameTD = ai1.getTextDescriptor(ArcInst.ARC_NAME);
			}
			if (ai2.getName() != null && !ai2.getNameKey().isTempname())
			{
				ra.arcName = ai2.getName();
				ra.arcNameTD = ai2.getTextDescriptor(ArcInst.ARC_NAME);
			}

			return ra;
		}

		/**
		 * Method to implement the reconnection in this Reconnect.
		 * @return list of newly created ArcInst that reconnects.
		 */
		public List<ArcInst> reconnectArcs()
		{
			List<ArcInst> newArcs = new ArrayList<ArcInst>();

			// reconnect the arcs
			for (ReconnectedArc ra : reconnectedArcs)
			{
				if (!ra.reconPi[0].getNodeInst().isLinked() || !ra.reconPi[1].getNodeInst().isLinked()) continue;
				ArcInst newAi = ArcInst.makeInstanceBase(ra.ap, ra.wid, ra.reconPi[0], ra.reconPi[1], ra.recon[0], ra.recon[1], null);
				if (newAi == null) continue;

				newAi.setHeadArrowed(ra.directionalHead);
				newAi.setTailArrowed(ra.directionalTail);
				newAi.setBodyArrowed(ra.directionalBody);
				newAi.setHeadExtended(ra.extendHead);
				newAi.setTailExtended(ra.extendTail);
				newAi.setHeadNegated(ra.negateHead);
				newAi.setTailNegated(ra.negateTail);
				if (ra.arcName != null)
				{
					newAi.setName(ra.arcName);
					newAi.setTextDescriptor(ArcInst.ARC_NAME, ra.arcNameTD);
				}
				newAi.copyVarsFrom(ra.reconAr[0]);
				newAi.copyVarsFrom(ra.reconAr[1]);
				newArcs.add(newAi);
			}
			return newArcs;
		}
	}

	/**
	 * Method to spread circuitry.
	 * @param cell the cell in which spreading happens.
	 * @param ni the NodeInst about which spreading happens (may be null).
	 * @param direction the direction to spread: 'u' for up, 'd' for down, 'l' for left, 'r' for right.
	 * @param amount the distance to spread (negative values compact).
	 * @param lX the low X bound of the node (the edge of spreading).
	 * @param hX the high X bound of the node (the edge of spreading).
	 * @param lY the low Y bound of the node (the edge of spreading).
	 * @param hY the high Y bound of the node (the edge of spreading).
	 */
	public static void spreadCircuitry(Cell cell, NodeInst ni, char direction, double amount, double lX, double hX, double lY, double hY)
	{
		// disallow spreading if lock is on
		if (cantEdit(cell, null, true, false, true) != 0) return;

		// initialize a collection of Geometrics that have been seen
		Set<Geometric> geomSeen = new HashSet<Geometric>();

		// set "already done" flag for nodes manhattan connected on spread line
		boolean mustBeHor = true;
		if (direction == 'l' || direction == 'r') mustBeHor = false;
		if (ni != null) manhattanTravel(ni, mustBeHor, geomSeen);

		// set "already done" flag for nodes that completely cover spread node or are in its line
		for(Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst oNi = it.next();
			Rectangle2D r = oNi.getBaseShape().getBounds2D();
			if (direction == 'l' || direction == 'r')
			{
				if (r.getMinX() < lX && r.getMaxX() > hX)
					geomSeen.add(oNi);
				if (oNi.getTrueCenterX() == (lX+hX)/2)
					geomSeen.add(oNi);
			} else
			{
				if (r.getMinY() < lY && r.getMaxY() > hY)
					geomSeen.add(oNi);
				if (oNi.getTrueCenterY() == (lY+hY)/2)
					geomSeen.add(oNi);
			}
		}

		// mark those arcinsts that should stretch during spread
		for(Iterator<ArcInst> it = cell.getArcs(); it.hasNext(); )
		{
			ArcInst ai = it.next();
			NodeInst no1 = ai.getTailPortInst().getNodeInst();
			NodeInst no2 = ai.getHeadPortInst().getNodeInst();
			double xC1 = no1.getTrueCenterX();
			double yC1 = no1.getTrueCenterY();
			double xC2 = no2.getTrueCenterX();
			double yC2 = no2.getTrueCenterY();

			// if one node is along spread line, make it "no1"
			if (geomSeen.contains(no2))
			{
				NodeInst swapNi = no1;  no1 = no2;  no2 = swapNi;
				double swap = xC1;     xC1 = xC2;  xC2 = swap;
				swap = yC1;     yC1 = yC2;  yC2 = swap;
			}

			// if both nodes are along spread line, leave arc alone
			if (geomSeen.contains(no2)) continue;

			boolean i = true;
			if (geomSeen.contains(no1))
			{
				// handle arcs connected to spread line
				switch (direction)
				{
					case 'l': if (xC2 <= lX) i = false;   break;
					case 'r': if (xC2 >= hX) i = false;   break;
					case 'u': if (yC2 >= hY) i = false;   break;
					case 'd': if (yC2 <= lY) i = false;   break;
				}
			} else
			{
				// handle arcs that cross the spread line
				switch (direction)
				{
					case 'l': if (xC1 > lX && xC2 <= lX) i = false; else
						if (xC2 > lX && xC1 <= lX) i = false;
						break;
					case 'r': if (xC1 < hX && xC2 >= hX) i = false; else
						if (xC2 < hX && xC1 >= hX) i = false;
						break;
					case 'u': if (yC1 > hY && yC2 <= hY) i = false; else
						if (yC2 > hY && yC1 <= hY) i = false;
						break;
					case 'd': if (yC1 < lY && yC2 >= lY) i = false; else
						if (yC2 < lY && yC1 >= lY) i = false;
						break;
				}
			}
			if (!i) geomSeen.add(ai);
		}

		// now look at every nodeinst in the cell
		boolean moved = false;
		boolean again = true;
		while (again)
		{
			again = false;
			for(Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
			{
				NodeInst oNi = it.next();

				// ignore this nodeinst if it has been spread already
				if (geomSeen.contains(oNi)) continue;

				// make sure nodeinst is on proper side of requested spread
				double xC1 = oNi.getTrueCenterX();
				double yC1 = oNi.getTrueCenterY();
				boolean doIt = false;
				switch (direction)
				{
					case 'l': if (xC1 < lX) doIt = true;   break;
					case 'r': if (xC1 > hX) doIt = true;   break;
					case 'u': if (yC1 > hY) doIt = true;   break;
					case 'd': if (yC1 < lY) doIt = true;   break;
				}
				if (!doIt) continue;

				// set every connecting nodeinst to be "spread"
				for(Iterator<ArcInst> aIt = cell.getArcs(); aIt.hasNext(); )
				{
					ArcInst ai = aIt.next();
					if (geomSeen.contains(ai))
					{
						// make arc temporarily unrigid
						Layout.setTempRigid(ai, false);
					} else
					{
						// make arc temporarily rigid
						Layout.setTempRigid(ai, true);
					}
				}
				netTravel(oNi, geomSeen);

				// move this nodeinst in proper direction to do spread
				switch(direction)
				{
					case 'l':
						oNi.move(-amount, 0);
						break;
					case 'r':
						oNi.move(amount, 0);
						break;
					case 'u':
						oNi.move(0, amount);
						break;
					case 'd':
						oNi.move(0, -amount);
						break;
				}

				// set loop iteration flag and node spread flag
				moved = true;
				again = true;
				break;
			}
		}
		if (!moved) System.out.println("Nothing changed");
	}

	/**
	 * Method to travel through the network, setting flags.
	 * @param ni the NodeInst from which to start traveling.
	 * @param geomSeen the HashSet bit to mark during travel.
	 */
	private static void netTravel(NodeInst ni, Set<Geometric> geomSeen)
	{
		if (geomSeen.contains(ni)) return;
		geomSeen.add(ni);

		for(Iterator<Connection> it = ni.getConnections(); it.hasNext(); )
		{
			Connection con = it.next();
			ArcInst ai = con.getArc();
			if (geomSeen.contains(ai)) continue;
			netTravel(ai.getHeadPortInst().getNodeInst(), geomSeen);
			netTravel(ai.getTailPortInst().getNodeInst(), geomSeen);
		}
	}

	/**
	 * Method to recursively travel along all arcs on a NodeInst.
	 * @param ni the NodeInst to examine.
	 * @param hor true to travel along horizontal arcs; false for vertical.
	 * @param geomSeen the HashSet used to mark nodes that are examined.
	 * This is called from "spread" to propagate along manhattan
	 * arcs that are in the correct orientation (along the spread line).
	 */
	private static void manhattanTravel(NodeInst ni, boolean hor, Set<Geometric> geomSeen)
	{
		geomSeen.add(ni);
		for(Iterator<Connection> it = ni.getConnections(); it.hasNext(); )
		{
			Connection con = it.next();
			ArcInst ai = con.getArc();
			int angle = ai.getAngle();
			if (hor)
			{
				// only want horizontal arcs
				if (angle != 0 && angle != 1800) continue;
			} else
			{
				// only want vertical arcs
				if (angle != 900 && angle != 2700) continue;
			}
			int otherEnd = 1 - con.getEndIndex();
			NodeInst other = ai.getPortInst(otherEnd).getNodeInst();
			if (geomSeen.contains(other)) continue;
			manhattanTravel(other, hor, geomSeen);
		}
	}

	/****************************** NODE AND ARC REPLACEMENT ******************************/

	private static class PossibleVariables
	{
		private static Map<PrimitiveNode,List<Variable.Key>> posVarsMap = new HashMap<PrimitiveNode,List<Variable.Key>>();

		private static void add(String varName, PrimitiveNode pn) {
			List<Variable.Key> varKeys = posVarsMap.get(pn);
			if (varKeys == null) {
				varKeys = new ArrayList<Variable.Key>();
				posVarsMap.put(pn, varKeys);
			}
			Variable.Key key = Variable.newKey(varName);
			if (!varKeys.contains(key)) varKeys.add(key);
		}

		static
		{
			add("ATTR_length",       Schematics.tech().transistorNode);
			add("ATTR_length",       Schematics.tech().transistor4Node);
			add("ATTR_width",        Schematics.tech().transistorNode);
			add("ATTR_width",        Schematics.tech().transistor4Node);
			add("ATTR_area",         Schematics.tech().transistorNode);
			add("ATTR_area",         Schematics.tech().transistor4Node);
			add("SIM_spice_model",   Schematics.tech().sourceNode);
			add("SIM_spice_model",   Schematics.tech().transistorNode);
			add("SIM_spice_model",   Schematics.tech().transistor4Node);
			add("SCHEM_meter_type",  Schematics.tech().meterNode);
			add("SCHEM_diode",       Schematics.tech().diodeNode);
			add("SCHEM_capacitance", Schematics.tech().capacitorNode);
			add("SCHEM_resistance",  Schematics.tech().resistorNode);
			add("SCHEM_inductance",  Schematics.tech().inductorNode);
			add("SCHEM_function",    Schematics.tech().bboxNode);
		}

		/**
		 * Get an iterator over valid Variable Keys for the primitive node
		 * @param pn the PrimitiveNode to examine.
		 * @return an Iterator over the Variable Keys on the Primitive Node.
		 */
		public Iterator<Variable.Key> getPossibleVarKeys(PrimitiveNode pn) {
			List<Variable.Key> varKeys = posVarsMap.get(pn);
			if (varKeys == null)
				varKeys = new ArrayList<Variable.Key>();
			return varKeys.iterator();
		}

		/**
		 * Method to decide a PrimitiveNode has a Variable key.
		 * @param key the Variable key.
		 * @param pn the PrimitiveNode to examine.
		 * @return true if a Variable key exists on the primitive node.
		 */
		public static boolean validKey(Variable.Key key, PrimitiveNode pn) {
			List varKeys = posVarsMap.get(pn);
			if (varKeys == null) return false;
			return varKeys.contains(key);
		}
	}

	/**
	 * Method to replace node "oldNi" with a new one of type "newNp"
	 * and return the new node.  Also removes any node-specific variables.
	 */
	public static NodeInst replaceNodeInst(NodeInst oldNi, NodeProto newNp, boolean ignorePortNames,
		boolean allowMissingPorts)
	{
		// replace the node
		NodeInst newNi = oldNi.replace(newNp, ignorePortNames, allowMissingPorts);
		if (newNi != null)
		{
			if (newNp instanceof PrimitiveNode)
			{
				// remove variables that make no sense
				for (Iterator<Variable> it = newNi.getVariables(); it.hasNext(); ) {
					Variable var = it.next();
					Variable.Key key = var.getKey();
					if (key != NodeInst.TRACE && !PossibleVariables.validKey(key, (PrimitiveNode)newNp))
					{
						newNi.delVar(var.getKey());
					}
				}
			}

			// now inherit parameters that now do exist
			inheritAttributes(newNi);
		}
		return newNi;
	}

	/****************************** INHERIT ATTRIBUTES ******************************/

	/**
	 * Method to inherit all prototype attributes down to instance "ni".
	 */
	public static void inheritAttributes(NodeInst ni)
	{
		// ignore primitives
		if (!ni.isCellInstance()) return;
		Cell cell = (Cell)ni.getProto();

		// first inherit parameters from this node's prototype
		if (cell.isIcon())
		{
			for(Iterator<Variable> vIt = cell.getParameters(); vIt.hasNext(); )
			{
				Variable var = vIt.next();
				inheritCellParameter(var, ni);
			}
		}

		// inherit attributes from this node's prototype
		for(Iterator<Variable> it = cell.getVariables(); it.hasNext(); )
		{
			Variable var = it.next();
			if (!var.getTextDescriptor().isInherit()) continue;
			inheritCellAttribute(var, ni);
		}

		// inherit directly from each port's prototype
		for(Iterator<Export> it = cell.getExports(); it.hasNext(); )
		{
			Export pp = it.next();
			inheritExportAttributes(pp, ni);
		}
	}

	/**
	 * Method to add all inheritable export variables from export "pp" on cell "np"
	 * to instance "ni".
	 */
	private static void inheritExportAttributes(Export pp, NodeInst ni)
	{
		for(Iterator<Variable> it = pp.getVariables(); it.hasNext(); )
		{
			Variable var = it.next();
			if (!var.getTextDescriptor().isInherit()) continue;

			Variable.Key attrKey = var.getKey();

			// see if the attribute is already there
			PortInst pi = ni.findPortInstFromProto(pp);
			Variable newVar = pi.getVar(attrKey);
			if (newVar != null) continue;

			// set the attribute
			TextDescriptor td = TextDescriptor.getPortInstTextDescriptor().withDisplay(false);
			Object value = inheritAddress(pp, var);
			value = Variable.withCode(value, CodeExpression.Code.NONE);
			pi.newVar(attrKey, value, td);
		}
	}

	/**
	 * Method to add inheritable variable "var" from cell "np" to instance "ni".
	 * If "icon" is not NONODEINST, use the position of the variable from it.
	 */
	private static void inheritCellAttribute(Variable var, NodeInst ni)
	{
		// see if the attribute is already there
		Variable.Key key = var.getKey();
		Variable newVar = ni.getVar(key);
		if (newVar != null)
		{
			// make sure visibility is OK
			if (!var.getTextDescriptor().isInterior())
			{
				// parameter should be visible: make it so
				if (!newVar.isDisplay())
				{
					ni.addVar(newVar.withDisplay(true));
				}
			} else
			{
				// parameter not normally visible: make it invisible if it has the default value
				if (newVar.isDisplay())
				{
					if (var.describe(-1).equals(newVar.describe(-1)))
					{
						ni.addVar(newVar.withDisplay(false));
					}
				}
			}
		} else {
			ni.addVar(var);
		}
	}

	/**
	 * Method to add parameter "var" to instance "ni".
	 */
	public static void inheritCellParameter(Variable var, NodeInst ni)
	{
		// see if the attribute is already there
		Variable.Key key = var.getKey();
		if (ni.isDefinedParameter(key))
		{
			Variable param = ni.getParameter(key);

			// make sure visibility is OK
			if (!var.isInterior())
			{
				// parameter should be visible: make it so
				if (!param.isDisplay())
					ni.addParameter(param.withDisplay(true));
			} else
			{
				// parameter not normally visible: make it invisible if it has the default value
				if (param.isDisplay())
				{
					if (var.describe(-1).equals(param.describe(-1)))
						ni.addParameter(param.withDisplay(false));
				}
			}
		} else
		{
			Cell cell = (Cell)ni.getProto();
			Variable iconVar = cell.getParameter(var.getKey());
			TextDescriptor td = iconVar.getTextDescriptor();
			Rectangle2D bounds = cell.getBounds();
			double xd = td.getXOff() - bounds.getCenterX();
			double yd = td.getYOff() - bounds.getCenterY();
			td = td.withOff(xd, yd);
			Object value = inheritAddress(cell, var);
            ni.addParameter(Variable.newInstance(var.getKey(), value, td));
		}
	}

	/**
	 * Helper method to determine the proper value of an inherited Variable.
	 * Normally, it is simply "var.getObject()", but if it is a string with the "++" or "--"
	 * sequence in it, then it indicates an auto-increments/decrements of that numeric value.
	 * The returned object has the "++"/"--" removed, and the original variable is modified.
	 * @param addr the ElectricObject on which this Variable resides.
	 * @param var the Variable being examined.
	 * @return the Object in the Variable.
	 */
	private static Object inheritAddress(ElectricObject addr, Variable var)
	{
		// if it isn't a string, just return its address
		Object obj = var.getObject();
		if (!(obj instanceof String)) return obj;
		if (var.isCode()) return obj;

		String str = (String)obj;
		int plusPlusPos = str.indexOf("++");
		int minusMinusPos = str.indexOf("--");
		if (plusPlusPos < 0 && minusMinusPos < 0) return obj;

		// construct the proper inherited string and increment the variable
		int incrPoint = Math.max(plusPlusPos, minusMinusPos);
		String retVal = str.substring(0, incrPoint) + str.substring(incrPoint+2);

		// increment the variable
		int i = incrPoint-1;
		for( ; i>=0; i--)
			if (!TextUtils.isDigit(str.charAt(i))) break;
		i++;
		int curVal = TextUtils.atoi(str.substring(i));
		if (str.charAt(incrPoint) == '+') curVal++; else curVal--;
		String newIncrString = str.substring(0, i) + curVal + str.substring(incrPoint);
		if (addr instanceof Cell)
		{
			((Cell)addr).getCellGroup().updateParam((Variable.AttrKey)var.getKey(), newIncrString);
		} else
		{
			addr.newVar(var.getKey(), newIncrString);
		}

		return retVal;
	}

	/****************************** LIBRARY CHANGES ******************************/

	/**
	 * This class implement the command to rename a technology.
	 */
	public static class RenameTechnology extends Job
	{
		private Technology tech;
		private String newName;

		public RenameTechnology(Technology tech, String newName)
		{
			super("Renaming " + tech, User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.tech = tech;
			this.newName = newName;
			startJob();
		}

		public boolean doIt() throws JobException
		{
			String oldName = tech.getTechName();
			tech.setTechName(newName);
			System.out.println("Technology '" + oldName + "' renamed to '" + newName + "'");

			// mark all libraries for saving
			for(Iterator<Library> it = Library.getLibraries(); it.hasNext(); )
			{
				Library oLib = it.next();
				if (oLib.isHidden()) continue;
				oLib.setChanged();
			}
			return true;
		}
	}

	/**
	 * This class implement the command to rename a library.
	 */
	public static class RenameLibrary extends Job
	{
		private Library lib;
		private String newName;
		private IdMapper idMapper;

		public RenameLibrary(Library lib, String newName)
		{
			super("Renaming " + lib, User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.lib = lib;
			this.newName = newName;
			startJob();
		}

		public boolean doIt() throws JobException
		{
			String oldName = lib.getName();
			idMapper = lib.setName(newName);
 			if (idMapper == null) return false;
			fieldVariableChanged("idMapper");
			System.out.println("Library '" + oldName + "' renamed to '" + newName + "'");
			return true;
		}

		public void terminateOK() {
			User.fixStaleCellReferences(idMapper);
		}
	}

	/**
	 * Method to implement the "Mark All Libraries for Saving" command.
	 */
	public static void markAllLibrariesForSavingCommand()
	{
		new MarkAllLibraries();
	}

	private static class MarkAllLibraries extends Job
	{
		MarkAllLibraries()
		{
			super("Making all libraries", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			startJob();
		}

		public boolean doIt() throws JobException
		{
			// mark all libraries as "changed"
			for(Iterator<Library> it = Library.getLibraries(); it.hasNext(); )
			{
				Library lib = it.next();
				if (lib.isHidden()) continue;

				// do not mark readable dump files for saving
				String ext = TextUtils.getExtension(lib.getLibFile());
				if (ext.equals("txt")) continue;

				lib.setChanged();
				if (lib.getLibFile() != null && OpenFile.getOpenFileType(lib.getLibFile().getFile(), FileType.JELIB) == FileType.DELIB)
				{
					// set all cells as changed as well
					for (Iterator<Cell> it2 = lib.getCells(); it2.hasNext(); )
					{
						it2.next().lowLevelSetRevisionDate(new Date());
					}
				}
			}
			System.out.println("All libraries now need to be saved");
			return true;
		}
	}

	/**
	 * Method to implement the "Mark All Libraries for Saving" command.
	 */
	public static void markCurrentLibForSavingCommand()
	{
		new MarkCurrentLibForSaving();
	}

	private static class MarkCurrentLibForSaving extends Job
	{
		MarkCurrentLibForSaving()
		{
			super("Making Current Lib", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			startJob();
		}
		public boolean doIt() throws JobException
		{
			Library lib = Library.getCurrent();
			if (lib.isHidden()) return true;

			// do not mark readable dump files for saving
			String ext = TextUtils.getExtension(lib.getLibFile());
			if (ext.equals("txt")) return true;

			lib.setChanged();
			if (lib.getLibFile() != null && OpenFile.getOpenFileType(lib.getLibFile().getFile(), FileType.JELIB) == FileType.DELIB)
			{
				// set all cells as changed as well
				for (Iterator<Cell> it2 = lib.getCells(); it2.hasNext(); )
				{
					it2.next().lowLevelSetRevisionDate(new Date());
				}
			}

			System.out.println("Library "+lib.getName()+" now needs to be saved");
			return true;
		}
	}

	/**
	 * This class implement the command to repair libraries.
	 */
	public static class CheckAndRepairJob extends Job
	{
		private boolean repair;

		public CheckAndRepairJob(boolean repair)
		{
			super((repair ? "Repair Libraries" : "Check Libraries"), User.getUserTool(), (repair ? Job.Type.CHANGE : Job.Type.SERVER_EXAMINE), null, null, Job.Priority.USER);
			this.repair = repair;
			startJob();
		}

		public boolean doIt() throws JobException
		{
			if (EDatabase.serverDatabase().checkInvariants())
			{
				ErrorLogger errorLogger = ErrorLogger.newInstance(repair ? "Repair Libraries" : "Check Libraries");
				int errorCount = 0;
				for(Iterator<Library> it = Library.getLibraries(); it.hasNext(); )
				{
					Library lib = it.next();
					errorCount += lib.checkAndRepair(repair, errorLogger);
				}
				if (errorCount > 0) System.out.println("Found " + errorCount + " errors"); else
					System.out.println("No errors found");
				errorLogger.termLogging(true);
			}
			return true;
		}
	}

	/**
	 * This class implement the command to reload a library
	 */
	public static class ReloadLibraryJob extends Job
	{
		private Library lib;

        public ReloadLibraryJob(Library lib)
		{
			super("Reload Library " + lib.getName(), User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.lib = lib;
			startJob();
		}

		public boolean doIt()
		{
			LibraryFiles.reloadLibrary(lib);
			return true;
		}
	}

	/****************************** DELETE UNUSED NODES ******************************/

//	public static class RemoveUnusedLayers extends Job
//	{
//		private Library library;
//
//		public RemoveUnusedLayers(Library lib)
//		{
//			super("Remove unused metal layers", null, Type.CHANGE, null, null, Priority.USER);
//			library = lib;
//			startJob();
//		}
//
//		public boolean doIt() throws JobException
//		{
//			// Only one library, the given one
//			if (library != null)
//			{
//				cleanUnusedNodesInLibrary(library);
//				return true;
//			}
//
//			// Checking all
//			for (Iterator<Library> libIter = Library.getLibraries(); libIter.hasNext();)
//			{
//				Library lib = libIter.next();
//				cleanUnusedNodesInLibrary(lib);
//			}
//			return true;
//		}
//
//		private void cleanUnusedNodesInLibrary(Library lib)
//		{
//			int action = -1;
//			List<Geometric> list = new ArrayList<Geometric>();
//
//			for (Iterator<Cell> cellsIter = lib.getCells(); cellsIter.hasNext();)
//			{
//				Cell cell = cellsIter.next();
//				if (cell.getView() != View.LAYOUT) continue; // only layout
//				list.clear();
//				Technology tech = cell.getTechnology();
//
//				for (int i = 0; i < cell.getNumArcs(); i++)
//				{
//					ArcInst ai = cell.getArc(i);
//					ArcProto ap = ai.getProto();
//					if (ap.isNotUsed())
//						list.add(ai);
//				}
//				for (int i = 0; i < cell.getNumNodes(); i++)
//				{
//					NodeInst ni = cell.getNode(i);
//					tech.cleanUnusedNodesInLibrary(ni, list);
//				}
//				if (action != 3 && list.size() > 0)
//				{
//					String [] options = {"Yes", "No", "Cancel", "Yes to All"};
//
//					action = JOptionPane.showOptionDialog(TopLevel.getCurrentJFrame(),
//							"Remove unused nodes in " + cell.libDescribe(), "Warning",
//							JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,
//							null, options, options[0]);
//					if (action == 2) return; // cancel
//				}
//				if (action != 1) // 1 is No to this local modification
//				{
//					System.out.println("Removing " + list.size() + " unused nodes in " + cell.libDescribe());
//					 eraseObjectsInList(cell, list);
//				}
//			}
//		}
//	}

	/****************************** DETERMINE ABILITY TO MAKE CHANGES ******************************/

	/**
	 * Method to test whether a NodeInst can be modified in a cell.
	 * Throws an exception if not.
	 * @param cell the Cell in which the NodeInst resides.
	 * @param item the NodeInst (may be null).
	 * @param giveError true to print an error message if the modification is disallowed.
	 * @param allowInstanceChange true to allow a change to an instance when instances are locked
	 * (this allows exports to be created and deleted).
	 */
	public static void testEditable(Cell cell, NodeInst item, boolean giveError, boolean allowInstanceChange)
		throws CantEditException
	{
		// if an instance is specified, check it
		if (item != null)
		{
			if (item.isLocked())
			{
				CantEditException e = new CantEditException();
				e.lockedNode = item;
				throw e;
			}
			boolean complexNode = false;
			if (!item.isCellInstance())
			{
				// see if a primitive is locked
				if (((PrimitiveNode)item.getProto()).isLockedPrim() &&
					User.isDisallowModificationLockedPrims())
				{
					CantEditException e = new CantEditException();
					e.lockedPrim = item;
					throw e;
				}
				PrimitiveNode.Function fun = item.getFunction();
				if (!fun.isPin() && !fun.isContact() &&
					fun != PrimitiveNode.Function.NODE && fun != PrimitiveNode.Function.CONNECT)
						complexNode = true;
			} else
			{
				// see if this type of cell is locked
				complexNode = true;
				if (!allowInstanceChange && cell.isInstancesLocked())
				{
					CantEditException e = new CantEditException();
					e.lockedInstances = cell;
					e.lockedExample = item;
					throw e;
				}
			}
			if (complexNode)
			{
				if (User.isDisallowModificationComplexNodes())
				{
					CantEditException e = new CantEditException();
					e.lockedComplex = item;
					throw e;
				}
			}
		}

		// check for general changes to the cell
		if (cell.isAllLocked())
		{
			CantEditException e = new CantEditException();
			e.lockedAll = cell;
			e.lockedExample = item;
			throw e;
		}
	}

	/**
	 * Method to tell whether a NodeInst can be modified in a cell.
	 * WARNING: method may change the database if the user disables a cell lock,
	 * so method must be called inside of a Change job.
	 * @param cell the Cell in which the NodeInst resides.
	 * @param item the NodeInst (may be null).
	 * @param giveError true to print an error message if the modification is disallowed.
	 * @param allowInstanceChange true to allow a change to an instance when instances are locked
	 * (this allows exports to be created and deleted).
	 * @param insideJob true if this is being run inside of a job.
	 * @return positive if the edit CANNOT be done.
	 * Return negative if the edit CANNOT be done and the overall operation should be cancelled.
	 * Return zero if the operation CAN be done.
	 */
	public static int cantEdit(Cell cell, NodeInst item, boolean giveError, boolean allowInstanceChange, boolean insideJob)
	{
		String [] options = {"Yes", "No", "Always", "Cancel"};
		// if an instance is specified, check it
		if (item != null)
		{
			if (item.isLocked())
			{
				if (!giveError) return 1;
				int ret = JOptionPane.showOptionDialog(TopLevel.getCurrentJFrame(),
					"Changes to locked " + item + " are disallowed.  Change anyway?",
					"Allow changes", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,
					null, options, options[1]);
				if (ret == 1) return 1;
				if (ret == 2)
				{
					if (insideJob) item.clearLocked(); else
						new ClearNodeLocked(item);
				}
				if (ret == 3 || ret == -1) return -1;  // -1 represents ESC or cancel
			}
			boolean complexNode = false;
			if (!item.isCellInstance())
			{
				// see if a primitive is locked
				if (((PrimitiveNode)item.getProto()).isLockedPrim() &&
					User.isDisallowModificationLockedPrims())
				{
					if (!giveError) return 1;
					int ret = JOptionPane.showOptionDialog(TopLevel.getCurrentJFrame(),
						"Changes to locked primitives (such as " + item + ") are disallowed.  Change anyway?",
						"Allow changes", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,
						null, options, options[1]);
					if (ret == 1) return 1;
					if (ret == 2) User.setDisallowModificationLockedPrims(false);
					if (ret == 3) return -1;
				}
				PrimitiveNode.Function fun = item.getFunction();
				if (!fun.isPin() && !fun.isContact() &&
					fun != PrimitiveNode.Function.NODE && fun != PrimitiveNode.Function.CONNECT)
						complexNode = true;
			} else
			{
				// see if this type of cell is locked
				complexNode = true;
				if (!allowInstanceChange && cell.isInstancesLocked())
				{
					if (!giveError) return 1;
					int ret = JOptionPane.showOptionDialog(TopLevel.getCurrentJFrame(),
						"Modification of instances in " + cell + " is disallowed.  You cannot move " + item +
						".  Change anyway?",
						"Allow changes", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,
						null, options, options[1]);
					if (ret == 1) return 1;
					if (ret == 2)
					{
						if (insideJob) cell.clearInstancesLocked(); else
							new ClearCellLocked(cell, false);
					}
					if (ret == 3) return -1;
				}
			}
			if (complexNode)
			{
				if (User.isDisallowModificationComplexNodes())
				{
					if (!giveError) return 1;
					int ret = JOptionPane.showOptionDialog(TopLevel.getCurrentJFrame(),
						"Changes to complex nodes (such as " + item + ") are disallowed.  Change anyway?",
						"Allow changes", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,
						null, options, options[1]);
					if (ret == 1) return 1;
					if (ret == 2) User.setDisallowModificationComplexNodes(false);
					if (ret == 3) return -1;
				}
			}
		}

        if (cell == null)
        {
            // operation should be cancelled since the cell is null
            return -1;
        }
        
        // check for general changes to the cell
		if (cell.isAllLocked())
		{
			if (!giveError) return 1;
			int ret = JOptionPane.showOptionDialog(TopLevel.getCurrentJFrame(),
				"Modification of " + cell + " is disallowed.  Change "+((item == null)? "" : item.toString())+" anyway?",
				"Allow changes", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,
				null, options, options[1]);
			if (ret == 1) return 1;
			if (ret == 2)
			{
				if (insideJob) cell.clearAllLocked(); else
					new ClearCellLocked(cell, true);
			}
			if (ret == 3) return -1;
		}
		return 0;
	}

	/**
	 * This class clears a node lock bit in a Job.
	 */
	public static class ClearNodeLocked extends Job
	{
		private NodeInst ni;

		public ClearNodeLocked(NodeInst ni)
		{
			super("Clear locked state of " + ni.describe(false), User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.ni = ni;
			startJob();
		}

		public boolean doIt()
		{
			ni.clearLocked();
			return true;
		}
	}

	/**
	 * This class clears a cell lock bit in a Job.
	 */
	public static class ClearCellLocked extends Job
	{
		private Cell cell;
		private boolean all;

		public ClearCellLocked(Cell cell, boolean all)
		{
			super("Clear locked state of " + cell.getName(), User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.cell = cell;
			this.all = all;
			startJob();
		}

		public boolean doIt()
		{
			if (all) cell.clearAllLocked(); else
				cell.clearInstancesLocked();
			return true;
		}
	}

    /****************************** Make Cell Annotation Job ******************************/
    public static class MakeCellAnnotationJob extends Job
    {
    	static final long serialVersionUID = 0;

		private transient EditWindow_ wnd;
        private Cell cell;
        private String newAnnotation;
        private Variable.Key key;

        public static void makeAnnotationMenuCommand(Tool tool, Variable.Key key, String newAnnotation)
        {
            UserInterface ui = Job.getUserInterface();
            EditWindow_ wnd = ui.needCurrentEditWindow_();
            if (wnd == null) return;
            Cell cell = ui.needCurrentCell();
            if (cell == null) return;
		    new MakeCellAnnotationJob(wnd, cell, tool, key, newAnnotation);
        }

        private MakeCellAnnotationJob(EditWindow_ wnd, Cell cell, Tool tool, Variable.Key k, String annotation)
        {
            super("Make Cell NCC Annotation", tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
            this.wnd = wnd;
            this.cell = cell;
            newAnnotation = annotation;
            key = k;
            startJob();
        }
		@Override
        public boolean doIt() throws JobException {
        	addAnnotation(cell, key, newAnnotation);
        	return true;
        }
		@Override
        public void terminateOK() {
        	wnd.clearHighlighting();
			wnd.addHighlightText(cell, cell, key);
			wnd.finishedHighlighting();
        }

        public static void addAnnotation(Cell c, Variable.Key k, String annotation)
        {
            Variable var = c.getVar(k);
            if (var == null) {
                String [] initial = new String[1];
                initial[0] = annotation;
                TextDescriptor td = TextDescriptor.getCellTextDescriptor().withInterior(true).withDispPart(TextDescriptor.DispPos.NAMEVALUE);
                var = c.newVar(k, initial, td);
                Job.error(var==null, "couldn't create" + k + " annotation");
            } else {
                Object oldObj = var.getObject();
                if (oldObj instanceof String) {
                    /* Groan! Menu command always creates attributes as arrays of strings.
                     * However, if user edits a single line attribute then dialog box
                     * converts it back into a String.  Be prepared to convert it back into an array*/
                    oldObj = new String[] {(String)oldObj};
                }
                Job.error(!(oldObj instanceof String[]), k + " annotation not String[]");
                String[] oldVal = (String[]) oldObj;
                TextDescriptor td = var.getTextDescriptor();

                int newLen = oldVal.length+1;
                String[] newVal = new String[newLen];
                for (int i=0; i<newLen-1; i++) newVal[i]=oldVal[i];
                newVal[newLen-1] = annotation;
                var = c.newVar(k, newVal, td);
            }
        }
    }
}
