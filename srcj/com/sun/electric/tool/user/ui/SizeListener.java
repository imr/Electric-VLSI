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
package com.sun.electric.tool.user.ui;

import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.geometry.Orientation;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.SizeOffset;
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
		Geometric geom = (Geometric)geomList.get(0);
		if (geom instanceof Geometric)
		{
			EventListener newListener = null;

			// remember the listener that was there before
			EventListener oldListener = WindowFrame.getListener();
			Cursor oldCursor = TopLevel.getCurrentCursor();

			System.out.println("Click to stretch " + geom);
			newListener = oldListener;
			if (newListener == null || !(newListener instanceof SizeListener))
			{
				newListener = new SizeListener();
				WindowFrame.setListener(newListener);
			}
			((SizeListener)newListener).stretchGeom = geom;
			((SizeListener)newListener).oldListener = oldListener;
			((SizeListener)newListener).oldCursor = oldCursor;

			// change the cursor
			TopLevel.setCurrentCursor(sizeCursor);
		}
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
					SizeOffset so = ni.getSizeOffset();				
					xS = ni.getXSize() - so.getLowXOffset() - so.getHighXOffset();
					yS = ni.getYSize() - so.getLowYOffset() - so.getHighYOffset();
				} else if (geom instanceof ArcInst && !nodes)
				{
					ArcInst ai = (ArcInst)geom;
					xS = ai.getWidth() - ai.getProto().getWidthOffset();
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
			ResizeStuff job = new ResizeStuff(wnd.getCell(), highlighted, xS, yS, nodes);
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
			if (CircuitChangeJobs.cantEdit(cell, null, true) != 0) return false;

			boolean didSomething = false;
			for(Geometric geom : highlighted)
			{
				if (geom instanceof NodeInst && nodes)
				{
					NodeInst ni = (NodeInst)geom;
					SizeOffset so = ni.getSizeOffset();				
					double x = xS + so.getLowXOffset() + so.getHighXOffset();
					double y = yS + so.getLowYOffset() + so.getHighYOffset();
					if (!ni.isCellInstance() && ((PrimitiveNode)ni.getProto()).isSquare())
					{
						if (y > x) x = y; else y = x;
					}
					ni.resize(x - ni.getXSize(), y - ni.getYSize());
					didSomething = true;
				} else if (geom instanceof ArcInst && !nodes)
				{
					ArcInst ai = (ArcInst)geom;
					double w = xS + ai.getProto().getWidthOffset();
					ai.modify(w - ai.getWidth(), 0, 0, 0, 0);
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
		WindowFrame.setListener(oldListener);
		TopLevel.setCurrentCursor(oldCursor);
        EditWindow wnd = (EditWindow)evt.getSource();
		showHighlight(null, wnd);

		// handle scaling the selected objects
		if (stretchGeom instanceof NodeInst)
		{
			NodeInst ni = (NodeInst)stretchGeom;
			Point2D newCenter = new Point2D.Double(ni.getAnchorCenterX(), ni.getAnchorCenterY());
			Point2D newSize = getNewNodeSize(evt, newCenter);
			ScaleNode job = new ScaleNode(ni, new EPoint(newCenter.getX(), newCenter.getY()), newSize.getX(), newSize.getY());
		} else
		{
			ArcInst ai = (ArcInst)stretchGeom;
			double newWidth = getNewArcSize(evt);
			ScaleArc job = new ScaleArc(ai, newWidth);
		}
		wnd.repaint();
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
			// restore the listener to the former state
			WindowFrame.setListener(oldListener);
			TopLevel.setCurrentCursor(oldCursor);
			showHighlight(null, wnd);
			System.out.println("Aborted");
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
				SizeOffset so = ni.getSizeOffset();
				AffineTransform trans = ni.getOrient().rotateAbout(newCenter.getX(), newCenter.getY());

				double stretchedLowX = newCenter.getX() - newSize.getX()/2 + so.getLowXOffset();
				double stretchedHighX = newCenter.getX() + newSize.getX()/2 - so.getHighXOffset();
				double stretchedLowY = newCenter.getY() - newSize.getY()/2 + so.getLowYOffset();
				double stretchedHighY = newCenter.getY() + newSize.getY()/2 - so.getHighYOffset();
				Poly stretchedPoly = new Poly((stretchedLowX+stretchedHighX)/2, (stretchedLowY+stretchedHighY)/2,
					stretchedHighX-stretchedLowX, stretchedHighY-stretchedLowY);
				stretchedPoly.transform(trans);
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
				double newWidth = getNewArcSize(evt);
				ArcInst ai = (ArcInst)stretchGeom;
				Poly stretchedPoly = ai.makePoly(newWidth - ai.getProto().getWidthOffset(), Poly.Type.CLOSED);
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
		NodeProto np = ni.getProto();
		SizeOffset so = ni.getSizeOffset();

		// setup outline of node with standard offset
		double nodeLowX = ni.getTrueCenterX() - ni.getXSize()/2 + so.getLowXOffset();
		double nodeHighX = ni.getTrueCenterX() + ni.getXSize()/2 - so.getHighXOffset();
		double nodeLowY = ni.getTrueCenterY() - ni.getYSize()/2 + so.getLowYOffset();
		double nodeHighY = ni.getTrueCenterY() + ni.getYSize()/2 - so.getHighYOffset();
		Poly nodePoly = new Poly((nodeLowX+nodeHighX)/2, (nodeLowY+nodeHighY)/2, nodeHighX-nodeLowX, nodeHighY-nodeLowY);
		AffineTransform trans = ni.rotateOutAboutTrueCenter();
		nodePoly.transform(trans);
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

		// if Shift is held, use center-based sizing
		boolean centerBased = (evt.getModifiersEx()&MouseEvent.SHIFT_DOWN_MASK) != 0 &&
			(evt.getModifiersEx()&MouseEvent.CTRL_DOWN_MASK) != 0;

		// determine the amount of growth of the node
		AffineTransform transIn = ni.rotateIn();
		double closestX = closest.getX();
		double closestY = closest.getY();
		double farthestX = farthestPoint.getX();
		double farthestY = farthestPoint.getY();
		transIn.transform(pt, pt);
		transIn.transform(closest, closest);
		transIn.transform(farthest, farthest);

		double growthRatioX, growthRatioY;
		if (centerBased)
		{
			double ptToCenterX = Math.abs(pt.getX() - ni.getTrueCenterX());
			double closestToCenterX = Math.abs(closest.getX() - ni.getTrueCenterX());
			double ptToCenterY = Math.abs(pt.getY() - ni.getTrueCenterY());
			double closestToCenterY = Math.abs(closest.getY() - ni.getTrueCenterY());
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

		// see what keys are held
		boolean square = !ni.isCellInstance() && ((PrimitiveNode)ni.getProto()).isSquare();
		if ((evt.getModifiersEx()&MouseEvent.CTRL_DOWN_MASK) != 0 &&
			(evt.getModifiersEx()&MouseEvent.SHIFT_DOWN_MASK) == 0)
		{
			// if Control is held, constrain to single-axis stretching
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
		} else if ((evt.getModifiersEx()&MouseEvent.SHIFT_DOWN_MASK) != 0 &&
			(evt.getModifiersEx()&MouseEvent.CTRL_DOWN_MASK) == 0)
		{
			// if Shift is held, constrain proportions
			square = true;
		}
		if (square)
		{
			if (Math.abs(growthRatioX) > Math.abs(growthRatioY)) growthRatioY = growthRatioX; else
				growthRatioX = growthRatioY;
		}

		// compute the new node size
		double newXSize = (ni.getXSize() - so.getLowXOffset() - so.getHighXOffset()) * growthRatioX;
		double newYSize = (ni.getYSize() - so.getLowYOffset() - so.getHighYOffset()) * growthRatioY;
		Point2D newSize = new Point2D.Double(newXSize, newYSize);

		// grid align the new node size
		EditWindow.gridAlign(newSize);

		// determine the new center point
		if (!centerBased)
		{
			AffineTransform pureTrans = ni.pureRotateOut();
			Point2D xformedSize = new Point2D.Double();
			pureTrans.transform(newSize, xformedSize);
			if (closestX > farthestX) closestX = farthestX + Math.abs(xformedSize.getX()); else
				closestX = farthestX - Math.abs(xformedSize.getX());
			if (closestY > farthestY) closestY = farthestY + Math.abs(xformedSize.getY()); else
				closestY = farthestY - Math.abs(xformedSize.getY());
			newCenter.setLocation((closestX + farthestX) / 2, (closestY + farthestY) / 2);
		}

		// adjust size offset to produce real size
		newSize.setLocation(Math.abs(newSize.getX() + so.getLowXOffset() + so.getHighXOffset()),
			Math.abs(newSize.getY() + so.getLowYOffset() + so.getHighYOffset()));
		return newSize;
	}

	/**
	 * Method to determine the proper size for the ArcInst being stretched, given a cursor location.
	 * @param evt the event with the current cursor location.
	 * @return the new size for the ArcInst.
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
		ArcProto ap = ai.getProto();
		double offset = ap.getWidthOffset();

		// determine point on arc that is closest to the cursor
		Point2D ptOnLine = DBMath.closestPointToLine(ai.getHeadLocation(), ai.getTailLocation(), pt);
		double newWidth = ptOnLine.distance(pt)*2 + offset;
		Point2D newSize = new Point2D.Double(newWidth, newWidth);
		EditWindow.gridAlign(newSize);
		return newSize.getX();
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
			if (CircuitChangeJobs.cantEdit(stretchNode.getParent(), null, true) != 0) return false;

			Point2D [] points = stretchNode.getTrace();
			if (points != null)
			{
				double percX = newWidth / stretchNode.getXSize();
				double percY = newHeight / stretchNode.getYSize();
				AffineTransform trans = stretchNode.pureRotateOut();
				Point2D [] newPoints = new Point2D[points.length];
				Point2D newPoint = new Point2D.Double(0, 0);
				for(int i=0; i<points.length; i++)
				{
					trans.transform(points[i], newPoint);
					newPoints[i] = new Point2D.Double(newPoint.getX()*percX, newPoint.getY()*percY);
				}
				stretchNode.setTrace(newPoints);
			}
            double dWid = newWidth - stretchNode.getXSize();
            double dHei = newHeight - stretchNode.getYSize();
			stretchNode.modifyInstance(newCenter.getX() - stretchNode.getAnchorCenterX(),
				newCenter.getY() - stretchNode.getAnchorCenterY(), dWid, dHei, Orientation.IDENT);
			return true;
		}
	}

	private static class ScaleArc extends Job
	{
		private ArcInst stretchArc;
		private double newWidth;

		protected ScaleArc(ArcInst stretchArc, double newWidth)
		{
			super("Scale arc", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.stretchArc = stretchArc;
			this.newWidth = newWidth;
			startJob();
		}

		public boolean doIt() throws JobException
		{
			// make sure scaling the arc is allowed
			if (CircuitChangeJobs.cantEdit(stretchArc.getParent(), null, true) != 0) return false;

			stretchArc.modify(newWidth - stretchArc.getWidth(), 0, 0, 0, 0);
			return true;
		}
	}

}
