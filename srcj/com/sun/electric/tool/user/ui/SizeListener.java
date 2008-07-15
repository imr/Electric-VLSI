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
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.user.CircuitChangeJobs;
import com.sun.electric.tool.user.Highlighter;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.dialogs.EDialog;

import java.awt.Cursor;
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
	private Geometric stretchGeom;
	private EventListener oldListener;
	private Cursor oldCursor;
	private Point2D farthestPoint;
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
		if (geomList.size() != 1)
		{
			System.out.println("Select just one object to size");
			return;
		}
		Geometric geom = geomList.get(0);
		EventListener newListener = null;

		// remember the listener that was there before
		EventListener oldListener = WindowFrame.getListener();
		Cursor oldCursor = TopLevel.getCurrentCursor();

		System.out.println("Click to stretch " + geom);
		newListener = oldListener;
		if (newListener == null || !(newListener instanceof SizeListener))
		{
			currentListener = new SizeListener();
            newListener = currentListener;
            WindowFrame.setListener(newListener);
        }
		((SizeListener)newListener).stretchGeom = geom;
        // Only store data when the previous event listener is not this one
        if (!(oldListener instanceof SizeListener))
        {
            ((SizeListener)newListener).oldListener = oldListener;
		    ((SizeListener)newListener).oldCursor = oldCursor;
        }

        // change the cursor
		TopLevel.setCurrentCursor(sizeCursor);
	}

    public static void restorePreviousListener(Object toDelete)
    {
        if (currentListener == null) return; // nothing to restore
        if (currentListener.stretchGeom == toDelete)
            currentListener.restoringOriginalSetup(null);
    }

    /**
	 * Method to present a dialog to resize all selected nodes.
	 */
	public static void sizeAllNodes()
	{
		SizeObjects dialog = new SizeObjects(TopLevel.getCurrentJFrame(), true, true);
		dialog.setVisible(true);
	}

	/**
	 * Method to present a dialog to resize all selected arcs.
	 */
	public static void sizeAllArcs()
	{
		SizeObjects dialog = new SizeObjects(TopLevel.getCurrentJFrame(), true, false);
		dialog.setVisible(true);
	}

	/**
	 * Class to handle the "Size all selected nodes/arcs" dialog.
	 */
	private static class SizeObjects extends EDialog
	{
		private JTextField xSize, ySize;
		boolean nodes;

		/** Creates new form Size all selected nodes/arcs */
		public SizeObjects(java.awt.Frame parent, boolean modal, boolean nodes)
		{
			super(parent, modal);

			EditWindow wnd = EditWindow.needCurrent();
			if (wnd == null) return;
			Highlighter highlighter = wnd.getHighlighter();

			getContentPane().setLayout(new GridBagLayout());
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
			} else
			{
				setTitle("Set Arc Size");
			}

			JLabel xSizeLabel = new JLabel(label);
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx = 0;
			gbc.gridy = 0;
			gbc.insets = new java.awt.Insets(4, 4, 4, 4);
			getContentPane().add(xSizeLabel, gbc);

			xSize = new JTextField();
			xSize.setColumns(6);
			gbc = new GridBagConstraints();
			gbc.gridx = 1;
			gbc.gridy = 0;
			gbc.weightx = 1;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.insets = new java.awt.Insets(4, 4, 4, 4);
			getContentPane().add(xSize, gbc);

			JButton ok = new JButton("OK");
			gbc = new GridBagConstraints();
			gbc.gridx = 1;
			gbc.gridy = 2;
			gbc.insets = new java.awt.Insets(4, 4, 4, 4);
			getContentPane().add(ok, gbc);

			JButton cancel = new JButton("Cancel");
			gbc = new GridBagConstraints();
			gbc.gridx = 0;
			gbc.gridy = 2;
			gbc.insets = new java.awt.Insets(4, 4, 4, 4);
			getContentPane().add(cancel, gbc);

			addWindowListener(new WindowAdapter()
			{
				public void windowClosing(WindowEvent evt) { SizeObjectsClosing(evt); }
			});
			cancel.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt) { cancel(evt); }
			});
			ok.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt) { ok(evt); }
			});

			pack();

			getRootPane().setDefaultButton(ok);

			// determine default size
			double xS = 0, yS = 0;
			for(Geometric geom : highlighter.getHighlightedEObjs(true, true))
			{
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
			xSize.setText(TextUtils.formatDouble(xS));
			if (nodes)
				ySize.setText(TextUtils.formatDouble(yS));
		}

		private void cancel(java.awt.event.ActionEvent evt)
		{
			SizeObjectsClosing(null);
		}

		private void ok(java.awt.event.ActionEvent evt)
		{
			// resize the objects
			EditWindow wnd = EditWindow.needCurrent();
			if (wnd == null) return;
			Highlighter highlighter = wnd.getHighlighter();
			List<Geometric> highlighted = highlighter.getHighlightedEObjs(true, true);
			double xS = TextUtils.atof(xSize.getText());
			double yS = 0;
			if (nodes)
				yS = TextUtils.atof(ySize.getText());
			new ResizeStuff(wnd.getCell(), highlighted, xS, yS, nodes);
			SizeObjectsClosing(null);
		}

		private void SizeObjectsClosing(WindowEvent evt)
		{
			setVisible(false);
			dispose();
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
		farthestPoint = null;
		showHighlight(evt, (EditWindow)evt.getSource());
	}

	public void mouseMoved(MouseEvent evt)
	{
		farthestPoint = null;
		showHighlight(evt, (EditWindow)evt.getSource());
		farthestPoint = null;
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

        // Checking the element hasn't been removed
        assert(stretchGeom.isLinked());

        // handle scaling the selected objects
        if (stretchGeom instanceof NodeInst)
        {
            NodeInst ni = (NodeInst)stretchGeom;
            Point2D newCenter = new Point2D.Double(ni.getAnchorCenterX(), ni.getAnchorCenterY());
            Point2D newSize = getNewNodeSize(evt, newCenter);
            new ScaleNode(ni, new EPoint(newCenter.getX(), newCenter.getY()), newSize.getX(), newSize.getY());
        } else
        {
            ArcInst ai = (ArcInst)stretchGeom;
            double newLambdaBaseWidth = getNewArcSize(evt);
            new ScaleArc(ai, newLambdaBaseWidth);
        }
        wnd.repaint();
	}

    private void restoringOriginalSetup(EditWindow wnd)
    {
        // restore the listener to the former state
        WindowFrame.setListener(oldListener);
        TopLevel.setCurrentCursor(oldCursor);
        if (wnd != null)
            showHighlight(null, wnd);
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
		highlighter.addElectricObject(stretchGeom, stretchGeom.getParent());
		highlighter.finished();
		if (evt != null)
		{
			if (stretchGeom instanceof NodeInst)
			{
				NodeInst ni = (NodeInst)stretchGeom;
				Point2D newCenter = new Point2D.Double(ni.getAnchorCenterX(), ni.getAnchorCenterY());
				Point2D newSize = getNewNodeSize(evt, newCenter);
				Poly stretchedPoly = ni.getBaseShape(EPoint.snap(newCenter), newSize.getX(), newSize.getY());
				Point2D [] stretchedPoints = stretchedPoly.getPoints();
				for(int i=0; i<stretchedPoints.length; i++)
				{
					int lastI = i - 1;
					if (lastI < 0) lastI = stretchedPoints.length - 1;
					highlighter.addLine(stretchedPoints[lastI], stretchedPoints[i], ni.getParent());
				}
			} else
			{
				// construct the polygons that describe the basic arc
				long newGridWidth = DBMath.lambdaToSizeGrid(getNewArcSize(evt));
				ArcInst ai = (ArcInst)stretchGeom;
				Poly stretchedPoly = ai.makeLambdaPoly(newGridWidth, Poly.Type.CLOSED);
				if (stretchedPoly == null) return;
				Point2D [] stretchedPoints = stretchedPoly.getPoints();
				for(int i=0; i<stretchedPoints.length; i++)
				{
					int lastI = i - 1;
					if (lastI < 0) lastI = stretchedPoints.length - 1;
					highlighter.addLine(stretchedPoints[lastI], stretchedPoints[i], ai.getParent());
				}
			}
			wnd.repaint();
		}
	}

	/**
	 * Method to determine the proper size for the NodeInst being stretched, given a cursor location.
	 * @param evt the event with the current cursor location.
	 */
	private Point2D getNewNodeSize(MouseEvent evt, Point2D newCenter)
	{
		// get the coordinates of the cursor in database coordinates
		EditWindow wnd = (EditWindow)evt.getSource();
		int oldx = evt.getX();
		int oldy = evt.getY();
		Point2D pt = wnd.screenToDatabase(oldx, oldy);

		// get information about the node being stretched
		NodeInst ni = (NodeInst)stretchGeom;
		if (ni.getProto() instanceof Cell) return EPoint.ORIGIN;

		// setup outline of node with standard offset
		Poly nodePoly = ni.getBaseShape();
		AffineTransform transIn = ni.transformIn();
		transIn.transform(pt, pt);
		nodePoly.transform(transIn);

		// determine the closest point on the outline
		Point2D [] points = nodePoly.getPoints();
		Point2D closest = null;
		Point2D farthest = null;
		if (farthestPoint != null)
		{
			for(int i=0; i<points.length; i++)
			{
				if (points[i].equals(farthestPoint))
				{
					closest = points[(i + points.length/2) % points.length];
					farthest = farthestPoint;
					break;
				}
			}
		}
		if (farthest == null || closest == null)
		{
			double closestDist = Double.MAX_VALUE;
			for(int i=0; i<points.length; i++)
			{
				double dist = pt.distance(points[i]);
				if (dist < closestDist)
				{
					closestDist = dist;
					closest = points[i];
					farthest = points[(i + points.length/2) % points.length];
				}
			}
		}
		farthestPoint = new Point2D.Double(farthest.getX(), farthest.getY());

		// if SHIFT and CONTROL is held, use center-based sizing
		boolean centerBased = (evt.getModifiersEx()&MouseEvent.SHIFT_DOWN_MASK) != 0 &&
			(evt.getModifiersEx()&MouseEvent.CTRL_DOWN_MASK) != 0;

		// if CONTROL is held, constrain to single-axis stretching
		boolean singleAxis = (evt.getModifiersEx()&MouseEvent.SHIFT_DOWN_MASK) == 0 &&
			(evt.getModifiersEx()&MouseEvent.CTRL_DOWN_MASK) != 0;

		// if SHIFT held (or if a square primitive) make growth the same in both axes
		boolean square = !ni.isCellInstance() && ((PrimitiveNode)ni.getProto()).isSquare();
		if ((evt.getModifiersEx()&MouseEvent.SHIFT_DOWN_MASK) != 0 &&
			(evt.getModifiersEx()&MouseEvent.CTRL_DOWN_MASK) == 0) square = true;

		// determine the amount of growth of the node
		double growthRatioX, growthRatioY;
		if (centerBased)
		{
			double ptToCenterX = Math.abs(pt.getX());
			double closestToCenterX = Math.abs(closest.getX());
			double ptToCenterY = Math.abs(pt.getY());
			double closestToCenterY = Math.abs(closest.getY());
			growthRatioX = ptToCenterX / closestToCenterX;
			growthRatioY = ptToCenterY / closestToCenterY;
		} else
		{
			double ptToFarthestX = pt.getX() - farthest.getX();
			double closestToFarthestX = closest.getX() - farthest.getX();
			double ptToFarthestY = pt.getY() - farthest.getY();
			double closestToFarthestY = closest.getY() - farthest.getY();
			growthRatioX = ptToFarthestX / closestToFarthestX;
			growthRatioY = ptToFarthestY / closestToFarthestY;
		}
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
			if (grx > gry) growthRatioY = 1; else
				growthRatioX = 1;
		}
		if (square)
		{
			if (Math.abs(growthRatioX) > Math.abs(growthRatioY)) growthRatioY = growthRatioX; else
				growthRatioX = growthRatioY;
		}

		// compute the new node size
		double newXSize = ni.getLambdaBaseXSize() * growthRatioX;
		double newYSize = ni.getLambdaBaseYSize() * growthRatioY;
		Point2D newSize = new Point2D.Double(newXSize, newYSize);

		// grid align the new node size
		EditWindow.gridAlignSize(newSize);

		// determine the new center point
		if (!centerBased)
		{
			double closestX = closest.getX();
			double closestY = closest.getY();
			double farthestX = farthest.getX();
			double farthestY = farthest.getY();
			double newClosestX = farthestX + Math.abs(newSize.getX())*(closestX > farthestX ? 1 : -1);
			double newClosestY = farthestY + Math.abs(newSize.getY())*(closestY > farthestY ? 1 : -1);

			// try to ensure smart grid aligning
//			double TINY = 1e-6/DBMath.GRID;
//			if (newClosestX > closestX)
//				farthestX -= TINY;
//			else if (newClosestX < closestX)
//				farthestX += TINY;
//			if (newClosestY > closestY)
//				farthestY -= TINY;
//			else if (newClosestY < closestY)
//				farthestY += TINY;

			newCenter.setLocation((newClosestX - closestX) / 2, (newClosestY - closestY) / 2);
			ni.transformOut().transform(newCenter, newCenter);
		}

		return newSize;
	}

	/**
	 * Method to determine the proper size for the ArcInst being stretched, given a cursor location.
	 * @param evt the event with the current cursor location.
	 * @return the new base size for the ArcInst in lambda units.
	 */
	private double getNewArcSize(MouseEvent evt)
	{
		// get the coordinates of the cursor in database coordinates
		EditWindow wnd = (EditWindow)evt.getSource();
		int oldx = evt.getX();
		int oldy = evt.getY();
		Point2D pt = wnd.screenToDatabase(oldx, oldy);

		// get information about the arc being stretched
		ArcInst ai = (ArcInst)stretchGeom;

		// determine point on arc that is closest to the cursor
		Point2D ptOnLine = DBMath.closestPointToLine(ai.getHeadLocation(), ai.getTailLocation(), pt);
		double newLambdaBaseWidth = ptOnLine.distance(pt)*2;
		Point2D newLambdaBaseSize = new Point2D.Double(newLambdaBaseWidth, newLambdaBaseWidth);
		EditWindow.gridAlignSize(newLambdaBaseSize);
		return newLambdaBaseSize.getX();
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
