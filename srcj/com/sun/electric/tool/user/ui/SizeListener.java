/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: SizeListener.java
 *
 * Copyright (c) 2004 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.user.ui;

import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.geometry.Orientation;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.Geometric;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.user.CircuitChangeJobs;
import com.sun.electric.tool.user.Highlighter;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.dialogs.EDialog;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.EventListener;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextField;

/**
 * Class to interactively resize a node.
 */
public class SizeListener
	implements MouseMotionListener, MouseListener, MouseWheelListener, KeyListener
{
	private List<Geometric> stretchGeoms;
	private Cell stretchCell;
	private EventListener oldListener;
	private Cursor oldCursor;
	private Point2D farthestCorner, farthestEdge, closestCorner, closestEdge;
	private NodeInst selectedNode;
	private ArcInst selectedArc;
	private static Cursor sizeCursor = ToolBar.readCursor("CursorSize.gif", 14, 14);
	private static SizeListener currentListener = null;
	private SizeListener() {}

	/**
	 * Method to do an interactive sizing of the currently selected object.
	 */
	public static void sizeObjects()
	{
		EditWindow wnd = EditWindow.needCurrent();
		if (wnd == null) return;
		Highlighter highlighter = wnd.getHighlighter();

		List<Geometric> geomList = highlighter.getHighlightedEObjs(true, true);
		if (geomList == null) return;
		int numArcs = 0, numPrims = 0;
		for(Geometric geom : geomList)
		{
			if (geom instanceof ArcInst) numArcs++; else
			{
				NodeInst ni = (NodeInst)geom;
				if (!ni.isCellInstance()) numPrims++;
			}
		}
		if (numPrims == 0 && numArcs == 0)
		{
			System.out.println("You must select arcs or primitive nodes.  Cell instances cannot be resized.");
			return;
		}
		if (numPrims != 0 && numArcs != 0)
		{
			System.out.println("You must select either arcs or primitive nodes, but not a mix of both.");
			return;
		}

		// remember the listener that was there before
		EventListener oldListener = WindowFrame.getListener();
		Cursor oldCursor = TopLevel.getCurrentCursor();

		System.out.println("Click to stretch");
		EventListener newListener = oldListener;
		if (newListener == null || !(newListener instanceof SizeListener))
		{
			currentListener = new SizeListener();
			newListener = currentListener;
			WindowFrame.setListener(newListener);
		}
		((SizeListener)newListener).stretchGeoms = geomList;
		((SizeListener)newListener).stretchCell = wnd.getCell();

		// Only store data when the previous event listener is not this one
		if (!(oldListener instanceof SizeListener))
		{
			((SizeListener)newListener).oldListener = oldListener;
			((SizeListener)newListener).oldCursor = oldCursor;
		}

		// change the cursor
		TopLevel.setCurrentCursor(sizeCursor);
		((SizeListener)newListener).selectedNode = null;
		((SizeListener)newListener).selectedArc = null;
		((SizeListener)newListener).showHighlight(null, wnd);
	}

	public static void restorePreviousListener(Object toDelete)
	{
		if (currentListener == null) return; // nothing to restore
		if (currentListener.stretchGeoms == toDelete)
			currentListener.restoringOriginalSetup(null);
	}

	/**
	 * Method to present a dialog to resize all selected nodes.
	 */
	public static void sizeAllNodes()
	{
		new SizeObjects(TopLevel.getCurrentJFrame(), true, true);
	}

	/**
	 * Method to present a dialog to resize all selected arcs.
	 */
	public static void sizeAllArcs()
	{
		new SizeObjects(TopLevel.getCurrentJFrame(), true, false);
	}

	/**
	 * Class to handle the "Size all selected nodes/arcs" dialog.
	 */
	private static class SizeObjects extends EDialog
	{
		private JTextField xSize, ySize;
		private boolean nodes;

		/** Creates new form Size all selected nodes/arcs */
		public SizeObjects(Frame parent, boolean modal, boolean nodes)
		{
			super(parent, modal);

			EditWindow wnd = EditWindow.needCurrent();
			if (wnd == null) return;
			Highlighter highlighter = wnd.getHighlighter();

			getContentPane().setLayout(new GridBagLayout());
			addWindowListener(new WindowAdapter()
			{
				public void windowClosing(WindowEvent evt) { closeDialog(); }
			});

			String label = "Width:";
			this.nodes = nodes;
			if (nodes)
			{
				label = "X Size:";
				setTitle("Set Node Size");

				JLabel ySizeLabel = new JLabel("Y Size:");
				GridBagConstraints gbc = new GridBagConstraints();
				gbc.gridx = 0;
				gbc.gridy = 1;
				gbc.insets = new java.awt.Insets(4, 4, 4, 4);
				getContentPane().add(ySizeLabel, gbc);

				ySize = new JTextField();
				ySize.setColumns(6);
				gbc = new GridBagConstraints();
				gbc.gridx = 1;
				gbc.gridy = 1;
				gbc.weightx = 1;
				gbc.fill = GridBagConstraints.HORIZONTAL;
				gbc.insets = new java.awt.Insets(4, 4, 4, 4);
				getContentPane().add(ySize, gbc);
				EDialog.makeTextFieldSelectAllOnTab(ySize);
			} else
			{
				setTitle("Set Arc Size");
			}

			xSize = new JTextField();
			xSize.setColumns(6);
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx = 1;
			gbc.gridy = 0;
			gbc.weightx = 1;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.insets = new java.awt.Insets(4, 4, 4, 4);
			getContentPane().add(xSize, gbc);
			EDialog.makeTextFieldSelectAllOnTab(xSize);

			JLabel xSizeLabel = new JLabel(label);
			gbc = new GridBagConstraints();
			gbc.gridx = 0;
			gbc.gridy = 0;
			gbc.insets = new java.awt.Insets(4, 4, 4, 4);
			getContentPane().add(xSizeLabel, gbc);

			// determine default size
			double xS = 0, yS = 0;
			Technology tech = null;
			for(Geometric geom : highlighter.getHighlightedEObjs(true, true))
			{
				tech = geom.getParent().getTechnology();
				if (geom instanceof NodeInst && nodes)
				{
					NodeInst ni = (NodeInst)geom;
					xS = ni.getLambdaBaseXSize();
					yS = ni.getLambdaBaseYSize();
				} else if (geom instanceof ArcInst && !nodes)
				{
					ArcInst ai = (ArcInst)geom;
					xS = ai.getLambdaBaseWidth();
				}
			}
			xSize.setText(TextUtils.formatDistance(xS, tech));
			if (nodes)
				ySize.setText(TextUtils.formatDistance(yS, tech));

			JButton ok = new JButton("OK");
			gbc = new GridBagConstraints();
			gbc.gridx = 1;
			gbc.gridy = 2;
			gbc.insets = new java.awt.Insets(4, 4, 4, 4);
			getContentPane().add(ok, gbc);
			ok.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt) { ok(evt); }
			});
			getRootPane().setDefaultButton(ok);

			JButton cancel = new JButton("Cancel");
			gbc = new GridBagConstraints();
			gbc.gridx = 0;
			gbc.gridy = 2;
			gbc.insets = new java.awt.Insets(4, 4, 4, 4);
			getContentPane().add(cancel, gbc);
			cancel.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt) { cancel(evt); }
			});

			pack();
			finishInitialization();
			setVisible(true);
		}

		protected void escapePressed() { closeDialog(); }

		private void cancel(ActionEvent evt) { closeDialog(); }

		private void ok(ActionEvent evt)
		{
			// resize the objects
			EditWindow wnd = EditWindow.needCurrent();
			if (wnd == null) return;
			Highlighter highlighter = wnd.getHighlighter();
			List<Geometric> highlighted = highlighter.getHighlightedEObjs(true, true);
			double xS = TextUtils.atofDistance(xSize.getText());
			double yS = 0;
			if (nodes)
				yS = TextUtils.atofDistance(ySize.getText());
			new ResizeStuff(wnd.getCell(), highlighted, xS, yS, nodes);
			closeDialog();
		}
	}

	/**
	 * Class to resize objects in a new thread.
	 */
	private static class ResizeStuff extends Job
	{
		private Cell cell;
		private List<Geometric> highlighted;
		private double xS, yS;
		private boolean nodes;

		protected ResizeStuff(Cell cell, List<Geometric> highlighted, double xS, double yS, boolean nodes)
		{
			super("Resize Objects", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.cell = cell;
			this.highlighted = highlighted;
			this.xS = xS;
			this.yS = yS;
			this.nodes = nodes;
			startJob();
		}

		public boolean doIt() throws JobException
		{
			// make sure moving the node is allowed
			if (CircuitChangeJobs.cantEdit(cell, null, true, false, true) != 0) return false;

			boolean didSomething = false;
			for(Geometric geom : highlighted)
			{
				if (geom instanceof NodeInst && nodes)
				{
					NodeInst ni = (NodeInst)geom;
					double x = xS;
					double y = yS;
					if (!ni.isCellInstance() && ((PrimitiveNode)ni.getProto()).isSquare())
					{
						if (y > x) x = y; else y = x;
					}
					ni.resize(x - ni.getLambdaBaseXSize(), y - ni.getLambdaBaseYSize());
					didSomething = true;
				} else if (geom instanceof ArcInst && !nodes)
				{
					ArcInst ai = (ArcInst)geom;
					ai.setLambdaBaseWidth(xS);
					didSomething = true;
				}
			}
			if (!didSomething)
			{
				System.out.println("Could not find any " + (nodes?"nodes":"arcs") + " to resize");
			}
			return true;
		}
	}

	public void mousePressed(MouseEvent evt)
	{
		findClosestObjectAndPoint(evt);
		showHighlight(evt, (EditWindow)evt.getSource());
	}

	private void findClosestObjectAndPoint(MouseEvent evt)
	{
		closestCorner = farthestCorner = null;
		closestEdge = farthestEdge = null;
		selectedNode = null;
		selectedArc = null;

		// get the coordinates of the cursor in database coordinates
		EditWindow wnd = (EditWindow)evt.getSource();
		int oldx = evt.getX();
		int oldy = evt.getY();
		Point2D pt = wnd.screenToDatabase(oldx, oldy);

		double closestDist = Double.MAX_VALUE;
		for(Geometric geom : stretchGeoms)
		{
			if (geom instanceof ArcInst)
			{
				ArcInst ai = (ArcInst)geom;
				long gridWidth = DBMath.lambdaToSizeGrid(ai.getLambdaBaseWidth());
				Poly poly = ai.makeLambdaPoly(gridWidth, Poly.Type.CLOSED);
				if (poly == null) continue;
				Point2D [] stretchedPoints = poly.getPoints();
				int angle = ai.getDefinedAngle();
				for(int i=0; i<stretchedPoints.length; i++)
				{
					int lastI = i - 1;
					if (lastI < 0) lastI = stretchedPoints.length - 1;
					int thisAng = DBMath.figureAngle(stretchedPoints[lastI], stretchedPoints[i]);
					if (thisAng%1800 == angle%1800)
					{
	                    double cX = (stretchedPoints[lastI].getX() + stretchedPoints[i].getX()) / 2;
	                    double cY = (stretchedPoints[lastI].getY() + stretchedPoints[i].getY()) / 2;
						double dist = pt.distance(new Point2D.Double(cX, cY));
						if (dist < closestDist)
						{
							closestDist = dist;
							closestCorner = farthestCorner = null;
							closestEdge = farthestEdge = null;
							selectedNode = null;
							selectedArc = ai;
						}
					}
				}
			} else
			{
				// get information about the node being stretched
				NodeInst ni = (NodeInst)geom;
				if (ni.getProto() instanceof Cell) continue;

				// setup outline of node with standard offset
				Poly nodePoly = ni.getBaseShape();
//				AffineTransform transIn = ni.transformIn();
//				pt = transIn.transform(pt, null);
//				nodePoly.transform(transIn);

				// determine the closest point on the outline
				Point2D [] points = nodePoly.getPoints();
				for(int i=0; i<points.length; i++)
				{
					double dist = pt.distance(points[i]);
					if (dist < closestDist)
					{
						closestDist = dist;
						closestCorner = points[i];
						farthestCorner = points[(i + points.length/2) % points.length];
						closestEdge = farthestEdge = null;
						selectedNode = ni;
						selectedArc = null;
					}
					int lastI = i-1;
					if (lastI < 0) lastI = points.length-1;
					Point2D edge = new Point2D.Double((points[i].getX() + points[lastI].getX())/2,
						(points[i].getY() + points[lastI].getY())/2);
					dist = pt.distance(edge);
					if (dist < closestDist)
					{
						closestDist = dist;
						int oppI = (i + points.length/2) % points.length;
						lastI = oppI-1;
						if (lastI < 0) lastI = points.length-1;
						closestEdge = edge;
						farthestEdge = new Point2D.Double((points[oppI].getX() + points[lastI].getX())/2,
							(points[oppI].getY() + points[lastI].getY())/2);
						closestCorner = farthestCorner = null;
						selectedNode = ni;
						selectedArc = null;
					}
				}
			}
		}
	}

	public void mouseDragged(MouseEvent evt)
	{
		showHighlight(evt, (EditWindow)evt.getSource());
	}

	public void mouseReleased(MouseEvent evt)
	{
		// restore the listener to the former state
		EditWindow wnd = (EditWindow)evt.getSource();
		restoringOriginalSetup(wnd);

		for(Geometric geom : stretchGeoms)
		{
			// Checking the elements haven't been removed
			assert(geom.isLinked());
	
			// handle scaling the selected objects
			if (geom instanceof NodeInst)
			{
				NodeInst ni = (NodeInst)geom;
				Point2D newCenter = new Point2D.Double(ni.getAnchorCenterX(), ni.getAnchorCenterY());
				Point2D newSize = getNewNodeSize(evt, newCenter, ni);
				new ScaleNode(ni, new EPoint(newCenter.getX(), newCenter.getY()), newSize.getX(), newSize.getY());
			} else
			{
				ArcInst ai = (ArcInst)geom;
				double newLambdaBaseWidth = getNewArcSize(evt, ai);
				new ScaleArc(ai, newLambdaBaseWidth);
			}
		}
		wnd.repaint();
	}

	private void restoringOriginalSetup(EditWindow wnd)
	{
		// restore the listener to the former state
		WindowFrame.setListener(oldListener);
		TopLevel.setCurrentCursor(oldCursor);
		if (wnd != null)
		{
			Highlighter highlighter = wnd.getHighlighter();
			highlighter.clear();
			for(Geometric geom : stretchGeoms)
				highlighter.addElectricObject(geom, stretchCell);
			highlighter.finished();
		}
	}

	public void keyPressed(KeyEvent evt)
	{
		int chr = evt.getKeyCode();
		EditWindow wnd = (EditWindow)evt.getSource();
		Cell cell = wnd.getCell();
		if (cell == null) return;

		// ESCAPE for abort
		if (chr == KeyEvent.VK_ESCAPE)
		{
			restoringOriginalSetup(wnd);
			System.out.println("Sizing aborted");
		}
	}

	public void mouseMoved(MouseEvent evt) {}
	public void mouseClicked(MouseEvent evt) {}
	public void mouseEntered(MouseEvent evt) {}
	public void mouseExited(MouseEvent evt) {}
	public void mouseWheelMoved(MouseWheelEvent evt) {}
	public void keyReleased(KeyEvent evt) {}
	public void keyTyped(KeyEvent evt) {}

	private void showHighlight(MouseEvent evt, EditWindow wnd)
	{
		Highlighter highlighter = wnd.getHighlighter();
		highlighter.clear();

		for(Geometric geom : stretchGeoms)
		{
			// highlight the node/arc
			highlighter.addElectricObject(geom, stretchCell);

			double boxSize = 5 / wnd.getScale();
			Color dotColor = new Color(User.getColor(User.ColorPrefType.GRID));
			if (geom instanceof NodeInst)
			{
				NodeInst ni = (NodeInst)geom;
				if (ni.isCellInstance()) continue;

				Point2D newCenter, newSize;
				if (evt != null)
				{
					newCenter = new Point2D.Double(ni.getAnchorCenterX(), ni.getAnchorCenterY());
					newSize = getNewNodeSize(evt, newCenter, ni);
				} else
                {
                    newSize = new Point2D.Double(ni.getLambdaBaseXSize(), ni.getLambdaBaseYSize());
                    newCenter = new Point2D.Double(ni.getAnchorCenterX(), ni.getAnchorCenterY());
                }

                Poly stretchedPoly = ni.getBaseShape(EPoint.snap(newCenter), newSize.getX(), newSize.getY());
				Point2D [] stretchedPoints = stretchedPoly.getPoints();
				for(int i=0; i<stretchedPoints.length; i++)
				{
					int lastI = i - 1;
					if (lastI < 0) lastI = stretchedPoints.length - 1;
					highlighter.addLine(stretchedPoints[lastI], stretchedPoints[i], stretchCell);

                    double cX = (stretchedPoints[lastI].getX() + stretchedPoints[i].getX()) / 2;
                    double cY = (stretchedPoints[lastI].getY() + stretchedPoints[i].getY()) / 2;
                    Poly poly = new Poly(cX, cY, boxSize, boxSize);
                    poly.setStyle(Poly.Type.FILLED);
                    highlighter.addPoly(poly, stretchCell, dotColor);

                    poly = new Poly(stretchedPoints[i].getX(), stretchedPoints[i].getY(), boxSize, boxSize);
                    poly.setStyle(Poly.Type.FILLED);
                    highlighter.addPoly(poly, stretchCell, dotColor);
				}
			} else
			{
				// construct the polygons that describe the basic arc
				ArcInst ai = (ArcInst)geom;
				long newGridWidth = DBMath.lambdaToSizeGrid(getNewArcSize(evt, ai));
				Poly stretchedPoly = ai.makeLambdaPoly(newGridWidth, Poly.Type.CLOSED);
				if (stretchedPoly == null) return;
				Point2D [] stretchedPoints = stretchedPoly.getPoints();
				int angle = ai.getDefinedAngle();
				for(int i=0; i<stretchedPoints.length; i++)
				{
					int lastI = i - 1;
					if (lastI < 0) lastI = stretchedPoints.length - 1;
					highlighter.addLine(stretchedPoints[lastI], stretchedPoints[i], stretchCell);
					int thisAng = DBMath.figureAngle(stretchedPoints[lastI], stretchedPoints[i]);
					if (thisAng%1800 == angle%1800)
					{
	                    double cX = (stretchedPoints[lastI].getX() + stretchedPoints[i].getX()) / 2;
	                    double cY = (stretchedPoints[lastI].getY() + stretchedPoints[i].getY()) / 2;
	                    Poly poly = new Poly(cX, cY, boxSize, boxSize);
	                    poly.setStyle(Poly.Type.FILLED);
	                    highlighter.addPoly(poly, stretchCell, dotColor);						
					}
				}
			}
		}
		highlighter.finished();
	}

	/**
	 * Method to determine the proper size for the NodeInst being stretched, given a cursor location.
	 * @param evt the event with the current cursor location.
	 */
	private Point2D getNewNodeSize(MouseEvent evt, Point2D newCenter, NodeInst desiredNI)
	{
		NodeInst selNode = selectedNode;
		if (selNode == null) selNode = desiredNI;

		// get the coordinates of the cursor in database coordinates
		EditWindow wnd = (EditWindow)evt.getSource();
		int oldx = evt.getX();
		int oldy = evt.getY();
		Point2D pt = wnd.screenToDatabase(oldx, oldy);

		// determine the amount of growth of the node
		double growthRatioX = 1, growthRatioY = 1;
		Point2D closest = (closestCorner != null ? closestCorner : closestEdge);
		Point2D farthest = (farthestCorner != null ? farthestCorner : farthestEdge);
		// grid-align the growth amount
		long x = Math.round((pt.getX()-closest.getX()) / User.getAlignmentToGrid().getWidth());
        double newX = x * User.getAlignmentToGrid().getWidth() + closest.getX();
        long y = Math.round((pt.getY()-closest.getY()) / User.getAlignmentToGrid().getHeight());
        double newY = y * User.getAlignmentToGrid().getHeight() + closest.getY();
        pt.setLocation(newX, newY);

//		AffineTransform transIn = selNode.transformIn();
//		transIn.transform(pt, pt);

		// if SHIFT and CONTROL is held, use center-based sizing
		boolean centerBased = (evt.getModifiersEx()&MouseEvent.SHIFT_DOWN_MASK) != 0 &&
			(evt.getModifiersEx()&MouseEvent.CTRL_DOWN_MASK) != 0;

		// if CONTROL is held, constrain to single-axis stretching
		boolean singleAxis = (evt.getModifiersEx()&MouseEvent.SHIFT_DOWN_MASK) == 0 &&
			(evt.getModifiersEx()&MouseEvent.CTRL_DOWN_MASK) != 0 && farthestCorner != null;

		// if SHIFT held (or if a square primitive) make growth the same in both axes
		boolean square = !selNode.isCellInstance() && ((PrimitiveNode)selNode.getProto()).isSquare();
		if ((evt.getModifiersEx()&MouseEvent.SHIFT_DOWN_MASK) != 0 &&
			(evt.getModifiersEx()&MouseEvent.CTRL_DOWN_MASK) == 0) square = true;

        if (closest != null && farthest != null)
		{
			if (centerBased)
			{
				double ptToCenterX = Math.abs(pt.getX());
				double closestToCenterX = Math.abs(closest.getX());
                double ptToCenterY = Math.abs(pt.getY());
				double closestToCenterY = Math.abs(closest.getY());
                if (closestToCenterX != 0) growthRatioX = ptToCenterX / closestToCenterX;
				if (closestToCenterY != 0) growthRatioY = ptToCenterY / closestToCenterY;
			} else
			{
				double ptToFarthestX = pt.getX() - farthest.getX();
				double closestToFarthestX = closest.getX() - farthest.getX();
				double ptToFarthestY = pt.getY() - farthest.getY();
				double closestToFarthestY = closest.getY() - farthest.getY();
				if (closestToFarthestX != 0) growthRatioX = ptToFarthestX / closestToFarthestX;
				if (closestToFarthestY != 0) growthRatioY = ptToFarthestY / closestToFarthestY;
			}
		}
//		int direction = -1; // both X and Y
		if (singleAxis)
		{
			// constrain to single-axis stretching
			double grx = Math.abs(growthRatioX);
			if (grx < 1)
			{
				if (grx == 0) grx = 9999; else grx = 1/grx;
			}
			double gry = Math.abs(growthRatioY);
			if (gry < 1)
			{
				if (gry == 0) gry = 9999; else gry = 1/gry;
			}
			if (grx > gry)
			{
				growthRatioY = 1;
//				direction = 0; // Y
			} else
			{
				growthRatioX = 1;
//				direction = 1; // X
			}
		}

		if (square)
		{
			if (Math.abs(growthRatioX) > Math.abs(growthRatioY))
				growthRatioY = growthRatioX;
			else
				growthRatioX = growthRatioY;
		}

		// compute the new node size
		double newXSize = selNode.getLambdaBaseXSize() * growthRatioX;
		double newYSize = selNode.getLambdaBaseYSize() * growthRatioY;
		double signX = newXSize < 0 ? -1 : 1;
		double signY = newYSize < 0 ? -1 : 1;
		Point2D newSize = new Point2D.Double(Math.abs(newXSize), Math.abs(newYSize));

		// grid align the new node size
//		EditWindow.gridAlignSize(newSize, direction);

		// determine the new center point
		if (!centerBased)
		{
			if (closest != null && farthest != null)
			{
				double closestX = closest.getX();
				double closestY = closest.getY();
				double farthestX = farthest.getX();
				double farthestY = farthest.getY();
				double newClosestX = (closestX == farthestX ? closestX : farthestX + newSize.getX()*signX*(closestX > farthestX ? 1 : -1));
				double newClosestY = (closestY == farthestY ? closestY : farthestY + newSize.getY()*signY*(closestY > farthestY ? 1 : -1));

				newCenter.setLocation((farthestX + newClosestX) / 2, (farthestY + newClosestY) / 2);
//				selNode.transformOut().transform(newCenter, newCenter);
			} else
			{
				newCenter.setLocation(desiredNI.getAnchorCenterX(), desiredNI.getAnchorCenterY());
			}
		}

		if (selNode != desiredNI)
		{
			double offX = newCenter.getX() - selNode.getAnchorCenterX();
			double offY = newCenter.getY() - selNode.getAnchorCenterY();
			newCenter.setLocation(desiredNI.getAnchorCenterX() + offX, desiredNI.getAnchorCenterY() + offY);

			boolean selNodeSideways = (selNode.getAngle() == 900 || selNode.getAngle() == 2700);
			boolean desiredNodeSideways = (desiredNI.getAngle() == 900 || desiredNI.getAngle() == 2700);
			offX = newSize.getX() - selNode.getXSizeWithoutOffset();
			offY = newSize.getY() - selNode.getYSizeWithoutOffset();
			if (selNodeSideways)
			{
				offX = newSize.getY() - selNode.getXSizeWithoutOffset();
				offY = newSize.getX() - selNode.getYSizeWithoutOffset();			
			}
			offX = offX + desiredNI.getXSizeWithoutOffset();
			offY = offY + desiredNI.getYSizeWithoutOffset();
			if (desiredNodeSideways)
			{
				double swap = offX;   offX = offY;   offY = swap;
			}
			newSize.setLocation(offX, offY);
		}
		return newSize;
	}

	/**
	 * Method to determine the proper size for the ArcInst being stretched, given a cursor location.
	 * @param evt the event with the current cursor location.
	 * @return the new base size for the ArcInst in lambda units.
	 */
	private double getNewArcSize(MouseEvent evt, ArcInst desiredAI)
	{
		if (evt == null) return desiredAI.getLambdaBaseWidth();

		// get the coordinates of the cursor in database coordinates
		EditWindow wnd = (EditWindow)evt.getSource();
		int oldx = evt.getX();
		int oldy = evt.getY();
		Point2D pt = wnd.screenToDatabase(oldx, oldy);

		// determine point on arc that is closest to the cursor
		Point2D ptOnLine = DBMath.closestPointToLine(selectedArc.getHeadLocation(), selectedArc.getTailLocation(), pt);
		double newLambdaBaseWidth = ptOnLine.distance(pt)*2;
		Point2D newLambdaBaseSize = new Point2D.Double(newLambdaBaseWidth, newLambdaBaseWidth);

		EditWindow.gridAlignSize(newLambdaBaseSize, -1);
		double newWid = newLambdaBaseSize.getX();
		if (selectedArc != desiredAI)
		{
			double off = newWid - selectedArc.getLambdaBaseWidth();
			newWid = desiredAI.getLambdaBaseWidth() + off;
		}
		return newWid;
	}

	private static class ScaleNode extends Job
	{
		private NodeInst stretchNode;
		private EPoint newCenter;
		private double newWidth, newHeight;

		protected ScaleNode(NodeInst stretchNode, EPoint newCenter, double newWidth, double newHeight)
		{
			super("Scale node", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.stretchNode = stretchNode;
			this.newCenter = newCenter;
			this.newWidth = newWidth;
			this.newHeight = newHeight;
			startJob();
		}

		public boolean doIt() throws JobException
		{
			// make sure scaling the node is allowed
			if (CircuitChangeJobs.cantEdit(stretchNode.getParent(), null, true, false, true) != 0) return false;

			Point2D [] points = stretchNode.getTrace();
			if (points != null)
			{
				double percX = newWidth / stretchNode.getLambdaBaseXSize();
				double percY = newHeight / stretchNode.getLambdaBaseYSize();
				AffineTransform trans = stretchNode.pureRotateOut();
				Point2D [] newPoints = new Point2D[points.length];
				for(int i=0; i<points.length; i++)
				{
					if (points[i] == null) continue;
					Point2D newPoint = new Point2D.Double(points[i].getX() * percX, points[i].getY() * percY);
					trans.transform(newPoint, newPoint);
					newPoint.setLocation(newPoint.getX() + newCenter.getX(), newPoint.getY() + newCenter.getY());
					newPoints[i] = newPoint;
				}
				stretchNode.setTrace(newPoints);
				return true;
			}
			double dWid = newWidth - stretchNode.getLambdaBaseXSize();
			double dHei = newHeight - stretchNode.getLambdaBaseYSize();
			stretchNode.modifyInstance(newCenter.getX() - stretchNode.getAnchorCenterX(),
				newCenter.getY() - stretchNode.getAnchorCenterY(), dWid, dHei, Orientation.IDENT);
			return true;
		}
	}

	private static class ScaleArc extends Job
	{
		private ArcInst stretchArc;
		private double newLambdaBaseWidth;

		protected ScaleArc(ArcInst stretchArc, double newLambdaBaseWidth)
		{
			super("Scale arc", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.stretchArc = stretchArc;
			this.newLambdaBaseWidth = newLambdaBaseWidth;
			startJob();
		}

		public boolean doIt() throws JobException
		{
			// make sure scaling the arc is allowed
			if (CircuitChangeJobs.cantEdit(stretchArc.getParent(), null, true, false, true) != 0) return false;

			stretchArc.setLambdaBaseWidth(newLambdaBaseWidth);
			return true;
		}
	}

}
