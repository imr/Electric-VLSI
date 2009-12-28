/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: TechConversionResult.java
 * Technology Editor, conversion of technology libraries to technologies
 * Written by Steven M. Rubin, Sun Microsystems.
 *
 * Copyright (c) 2007 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.user.tecEdit;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.EditWindow_;
import com.sun.electric.tool.Job;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Class to define error messages that arise during technology creation.
 * This class is necessary because errors occur during Jobs, but the display
 * of the errors must be done on the client side.  Therefore, objects of
 * this class are passed from the server to the client for display.
 */
public class TechConversionResult implements Serializable
{
	private boolean good;
	private String errorMessage;
	private NodeInst ni;
	private Cell cell;
	private List<Example> neList;
	private Sample ns;
	private double sampleCoord;
	private boolean xDir;

	public TechConversionResult() { good = true; }

	/**
	 * Method to return the success status.
	 * @return true if the conversion failed.
	 */
	public boolean failed() { return !good; }

	/**
	 * Method to mark a general error.
	 * @param ni the NodeInst that caused the error (may be null).
	 * @param cell the Cell where the error occurred (may be null).
	 * @param errorMessage the message to display.
	 */
	public void markError(NodeInst ni, Cell cell, String errorMessage)
	{
		good = false;
		this.cell = cell;
		this.ni = ni;
		this.errorMessage = errorMessage;
	}

	/**
	 * Method to mark an error in determining stretching rules.
	 * @param neList the Examples that invoked the error.
	 * @param ns the Sample on the main Example that failed.
	 * @param cell the Cell with the error.
	 * @param sampleCoord the coordinate (in X or Y) where the failure occurred.
	 * @param xDir true for an X-axis error, false for Y-axis.
	 */
	public void markStretchProblem(List<Example> neList, Sample ns, Cell cell, double sampleCoord, boolean xDir)
	{
		good = false;
		this.neList = neList;
		this.ns = ns;
		this.cell = cell;
		this.sampleCoord = sampleCoord;
		this.xDir = xDir;
	}

	/**
	 * Method to return the error message associated with this conversion.
	 * @return the error message associated with this conversion.
	 */
	public String getErrorMessage()
	{
		String fullErrorMsg = errorMessage;
		if (cell != null)
		{
    		String prefixErrorMsg = "Cell " + cell.describe(false);
    		if (ni != null) prefixErrorMsg += ", node " + ni.describe(false);
    		fullErrorMsg = prefixErrorMsg + ": " + fullErrorMsg;
		}
		return fullErrorMsg;
	}

	/**
	 * Method to highlight the error.
	 */
	public void showError()
	{
		// more complex errors have their own way to display
		if (errorMessage == null)
		{
			explainStretchProblem();
			return;
		}

		if (cell != null)
		{
			Job.getUserInterface().displayCell(cell);
    		EditWindow_ wnd = Job.getUserInterface().getCurrentEditWindow_();
    		if (wnd != null && ni != null)
    		{
    			wnd.clearHighlighting();
    			wnd.addElectricObject(ni, cell);
    			wnd.finishedHighlighting();
    		}
		}
		String msg = getErrorMessage();
		Job.getUserInterface().showErrorMessage(msg, "Analysis Failure");
		System.out.println(msg);
	}

	/**
	 * Method to explain a stretching rule problem.
	 * @param neList the list of Examples
	 * @param ns the sample in the main Example
	 * @param np the Cell.
	 * @param sampleCoord the coordinate of the stretching failure.
	 * @param xDir true if this is in the X direction, false for Y.
	 */
	private void explainStretchProblem()
	{
		Job.getUserInterface().displayCell(cell);
		EditWindow_ wnd = Job.getUserInterface().getCurrentEditWindow_();
		if (wnd == null) return;
		wnd.clearHighlighting();
		Map<Sample,double[]> factorList = new HashMap<Sample,double[]>();
		double[] factors = showSampleInExample(wnd, ns, neList.get(0), sampleCoord, xDir, true);
		factorList.put(ns, factors);
		for(int n=1; n<neList.size(); n++)
		{
			Example e = neList.get(n);
			for(Sample s : e.samples)
			{
				if (s.assoc == ns)
				{
					factors = showSampleInExample(wnd, s, e, sampleCoord, xDir, false);
					factorList.put(s, factors);
				}
			}
		}

		// see if the offending sample can be guessed
		Set<Sample> allSamples = factorList.keySet();
		Map<Sample,Integer> offendingSample = new HashMap<Sample,Integer>();
		for(int i=0; i<4; i++)
		{
			double v1 = 0, v2 = 0;
			int v1Count = 0, v2Count = 0;
			Sample s1 = null, s2 = null;
			for(Sample s : allSamples)
			{
				factors = factorList.get(s);
				if (v1Count == 0)
				{
					v1Count++;
					v1 = factors[i];
					s1 = s;
					continue;
				}
				if (v1 == factors[i])
				{
					v1Count++;
					continue;
				}
				if (v2Count == 0)
				{
					v2Count++;
					v2 = factors[i];
					s2 = s;
					continue;
				}
				if (v2 == factors[i])
				{
					v2Count++;
					continue;
				}
			}
			if (v1Count+v2Count != allSamples.size()) continue;
			Sample offendingSamp = null;
			if (v1Count == 1) offendingSamp = s1;
			if (v2Count == 1) offendingSamp = s2;
			if (offendingSamp != null)
			{
				Integer prev = offendingSample.get(offendingSamp);
				if (prev == null) prev = new Integer(1); else prev = new Integer(prev.intValue()+1);
				offendingSample.put(offendingSamp, prev);
			}
		}
		Sample offendingSamp = null;
		int num = 0;
		for(Sample s : offendingSample.keySet())
		{
			int numTimes = offendingSample.get(s).intValue();
			if (numTimes > num)
			{
				num = numTimes;
				offendingSamp = s;
			}
		}

		// show the offending sample if found, all if not
		String err = "Cannot determine " + (xDir?"X":"Y") + " stretching rule for layer " + Info.getSampleName(ns.layer) +
			" in " + cell;
		String additional = "\nOne of these stretching rules must be the same for every example:" +
			"\nOut (the distance from the center to the point)" +
			"\nIn (the distance from the edge to the point)" +
			"\nPer (the percentage of the point from the center to the edge)" +
			"\nOpp (the distance from the opposite edge to the point)";
		if (offendingSamp == null)
		{
			offendingSamp = ns;
			for(Sample s : allSamples)
				wnd.addElectricObject(s.node, cell);
		} else
		{
			wnd.addElectricObject(offendingSamp.node, cell);
			additional += "\n\nThe error is probably with node " + offendingSamp.node.describe(false);
		}
		wnd.finishedHighlighting();

		Job.getUserInterface().showErrorMessage(err+additional, "Analysis Failure");
		System.out.println(err);
	}

	private double[] showSampleInExample(EditWindow_ wnd, Sample ns, Example e, double sampleCoord,
		boolean xDir, boolean mainExample)
	{
		Cell cell = ns.node.getParent();
		Rectangle2D exampleBounds = wnd.getCell().getBounds();
		double singleStep = Math.max(exampleBounds.getHeight(), exampleBounds.getWidth()) / 45;
		double doubleStep = singleStep * 2;
		double halfStep = singleStep / 2;
		double exampleHalfSize = (xDir ? (e.hx - e.lx) : (e.hy - e.ly)) / 2;
		double exampleCtr = (xDir ? (e.lx + e.hx) : (e.ly + e.hy)) / 2;

		// first draw bold bars above/below example at the edges and center
		Point2D pt1 = makeDisplayPoint(e, 0, exampleCtr-exampleHalfSize, xDir);
		Point2D pt2 = makeDisplayPoint(e, singleStep, exampleCtr-exampleHalfSize, xDir);
		wnd.addHighlightLine(pt1, pt2, cell, true, false);
		pt1 = makeDisplayPoint(e, 0, exampleCtr+exampleHalfSize, xDir);
		pt2 = makeDisplayPoint(e, singleStep, exampleCtr+exampleHalfSize, xDir);
		wnd.addHighlightLine(pt1, pt2, cell, true, false);
		pt1 = makeDisplayPoint(e, 0, exampleCtr, xDir);
		pt2 = makeDisplayPoint(e, doubleStep, exampleCtr, xDir);
		wnd.addHighlightLine(pt1, pt2, cell, true, false);

		Rectangle2D mainSampleBounds = ns.assoc.node.getBounds();
		Rectangle2D sampleBounds = ns.node.getBounds();
		boolean adjusted = false;
		if (mainExample) adjusted = true; else
		{
			if (xDir)
			{
				if (sampleCoord == mainSampleBounds.getMinX())
					{ adjusted = true;   sampleCoord = sampleBounds.getMinX(); }
				if (sampleCoord == mainSampleBounds.getMaxX())
					{ adjusted = true;   sampleCoord = sampleBounds.getMaxX(); }
			} else
			{
				if (sampleCoord == mainSampleBounds.getMinY())
					{ adjusted = true;   sampleCoord = sampleBounds.getMinY(); }
				if (sampleCoord == mainSampleBounds.getMaxY())
					{ adjusted = true;   sampleCoord = sampleBounds.getMaxY(); }
			}
		}

		// determine the 4 stretching factors (out, in, %, opp)
		double [] factors = new double[4];
		double percentOut = Math.abs(sampleCoord-exampleCtr) / exampleHalfSize;
		double distOut = Math.abs(sampleCoord-exampleCtr);
		factors[0] = distOut;
		factors[1] = exampleHalfSize-distOut;
		factors[2] = Math.round(percentOut*100);
		factors[3] = distOut+exampleHalfSize;

		if (adjusted)
		{
			double arrowCoord = sampleCoord;
			double arrowCoordInv = sampleCoord;
			double farEnd, closeEnd;
			if (sampleCoord > exampleCtr)
			{
				arrowCoord -= singleStep;
				arrowCoordInv += singleStep;
				closeEnd = exampleCtr + exampleHalfSize;
				farEnd = exampleCtr - exampleHalfSize;
			} else
			{
				arrowCoord += singleStep;
				arrowCoordInv -= singleStep;
				closeEnd = exampleCtr - exampleHalfSize;
				farEnd = exampleCtr + exampleHalfSize;
			}

			// now draw bar where problem is
			pt1 = makeDisplayPoint(e, halfStep, sampleCoord, xDir);
			pt2 = makeDisplayPoint(e, doubleStep+singleStep, sampleCoord, xDir);
			wnd.addHighlightLine(pt1, pt2, cell, false, false);

			// draw arrow from opposite edge to problem location
			if (sampleCoord != farEnd)
			{
				pt1 = makeDisplayPoint(e, singleStep, farEnd, xDir);
				pt2 = makeDisplayPoint(e, singleStep, sampleCoord, xDir);
				wnd.addHighlightLine(pt1, pt2, cell, false, false);
				pt1 = makeDisplayPoint(e, singleStep, sampleCoord, xDir);
				pt2 = makeDisplayPoint(e, singleStep-singleStep/3, arrowCoord, xDir);
				wnd.addHighlightLine(pt1, pt2, cell, false, false);
				pt1 = makeDisplayPoint(e, singleStep, sampleCoord, xDir);
				pt2 = makeDisplayPoint(e, singleStep+singleStep/3, arrowCoord, xDir);
				wnd.addHighlightLine(pt1, pt2, cell, false, false);
			}

			// draw arrow from center to problem location
			if (sampleCoord != exampleCtr)
			{
				pt1 = makeDisplayPoint(e, doubleStep, exampleCtr, xDir);
				pt2 = makeDisplayPoint(e, doubleStep, sampleCoord, xDir);
				wnd.addHighlightLine(pt1, pt2, cell, false, false);
				pt1 = makeDisplayPoint(e, doubleStep, sampleCoord, xDir);
				pt2 = makeDisplayPoint(e, doubleStep-singleStep/3, arrowCoord, xDir);
				wnd.addHighlightLine(pt1, pt2, cell, false, false);
				pt1 = makeDisplayPoint(e, doubleStep, sampleCoord, xDir);
				pt2 = makeDisplayPoint(e, doubleStep+singleStep/3, arrowCoord, xDir);
				wnd.addHighlightLine(pt1, pt2, cell, false, false);
			}

			// draw arrow from close edge to problem location
			if (sampleCoord != closeEnd)
			{
				pt1 = makeDisplayPoint(e, doubleStep, closeEnd, xDir);
				pt2 = makeDisplayPoint(e, doubleStep, sampleCoord, xDir);
				wnd.addHighlightLine(pt1, pt2, cell, false, false);
				pt1 = makeDisplayPoint(e, doubleStep, sampleCoord, xDir);
				pt2 = makeDisplayPoint(e, doubleStep-singleStep/3, arrowCoordInv, xDir);
				wnd.addHighlightLine(pt1, pt2, cell, false, false);
				pt1 = makeDisplayPoint(e, doubleStep, sampleCoord, xDir);
				pt2 = makeDisplayPoint(e, doubleStep+singleStep/3, arrowCoordInv, xDir);
				wnd.addHighlightLine(pt1, pt2, cell, false, false);
			}

			// write factors
			pt2 = makeDisplayPoint(e, doubleStep+halfStep, xDir ? exampleCtr : (exampleCtr-halfStep), xDir);
			wnd.addHighlightMessage(cell, "Out="+TextUtils.formatDouble(factors[0])+
				" Per="+((int)factors[2]), pt2);
			pt2 = makeDisplayPoint(e, doubleStep-halfStep, xDir ? closeEnd+halfStep : (closeEnd-halfStep), xDir);
			wnd.addHighlightMessage(cell, "In="+TextUtils.formatDouble(factors[1]), pt2);
			pt2 = makeDisplayPoint(e, singleStep+halfStep, xDir ? farEnd : (farEnd-halfStep), xDir);
			wnd.addHighlightMessage(cell, "Opp="+TextUtils.formatDouble(factors[3]), pt2);
		}
		return factors;
	}

	private Point2D makeDisplayPoint(Example e, double offsetOrtho, double sampleCoord, boolean xDir)
	{
		if (xDir) return new Point2D.Double(sampleCoord, e.hy+offsetOrtho);
		return new Point2D.Double(e.hx+offsetOrtho, sampleCoord);
	}

}
