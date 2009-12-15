/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: AnalogSignal.java
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
package com.sun.electric.tool.simulation;

import java.awt.geom.Rectangle2D;

/**
 * Class to define an analog signal in the simulation waveform window.
 */
public class AnalogSignal extends Signal implements MultiSweepSignal
{
	/** the Analysis object in which this DigitalSignal resides. */		private final AnalogAnalysis an;
	/** index of this signal in its AnalogAnalysis */					private final int index;

	/**
	 * Constructor for an analog signal.
	 * @param an the AnalogAnalysis object in which this signal will reside.
	 */
	protected AnalogSignal(AnalogAnalysis an)
	{
		this.an = an;
		index = an.getSignals().size();
		an.addSignal(this);
	}

	/**
	 * Method to return the AnalogAnalysis in which this signal resides.
	 * @return the AnalogAnalysis in which this signal resides.
	 */
	@Override
	public AnalogAnalysis getAnalysis() { return an; }

	/**
	 * Method to return the index of this AnalogSignal in its AnalogAnalysis.
	 * @return the index of this AnalogSignal in its AnalogAnalysis.
	 */
	public int getIndexInAnalysis() { return index; }

	/**
	 * Method to return the waveform of this signal in specified sweep.
	 * @param sweep sweep index
	 * @return the waveform of this signal in specified sweep.
	 */
	public Waveform getWaveform(int sweep) {
		return an.getWaveform(this, sweep);
	}

    public NewSignal<ScalarSample> getSweep(int sweep) {
        return an.getWaveform(this, sweep);
    }

	/**
	 * Method to return the number of sweeps in this signal.
	 * @return the number of sweeps in this signal.
	 * If this signal is not a sweep signal, returns 1.
	 */
	public int getNumSweeps()
	{
		return an.getNumSweeps();
	}

	/**
	 * Method to compute the low and high range of time and value on this signal.
	 * The result is stored in the "bounds", "leftEdge", and "rightEdge" field variables.
	 */
	protected void calcBounds()
	{
		// determine extent of the data
		double lowTime=0, highTime=0, lowValue=0, highValue=0;
		boolean first = true;
		double[] result = new double[3];
		for (int sweep = 0, numSweeps = getNumSweeps(); sweep < numSweeps; sweep++)
		{
			Waveform waveform = getWaveform(sweep);
            if (waveform instanceof BTreeNewSignal) {
                // Hack
                BTreeNewSignal btns = (BTreeNewSignal)waveform;
                NewSignal.Approximation<ScalarSample> approx = btns.getPreferredApproximation();
                if (approx.getTime(0) < lowTime)
                    lowTime = approx.getTime(0);
                if (approx.getTime(approx.getNumEvents()-1) > highTime)
                    highTime = approx.getTime(approx.getNumEvents()-1);
                if (approx.getSample(btns.eventWithMinValue).getValue() < lowValue)
                    lowValue = approx.getSample(btns.eventWithMinValue).getValue();
                if (approx.getSample(btns.eventWithMaxValue).getValue() > highValue)
                    highValue = approx.getSample(btns.eventWithMaxValue).getValue();
                continue;
            }

			for(int i=0, numEvents = waveform.getNumEvents(); i<numEvents; i++)
			{
				waveform.getEvent(i, result);
				double time = result[0];
				if (sweep == 0)
				{
					if (i == 0) leftEdge = time; else
						if (i == numEvents-1) rightEdge = time;
				}
				double lowVal = result[1];
				double highVal = result[2];
				if (first)
				{
					first = false;
					lowTime = highTime = time;
					lowValue = lowVal;
					highValue = highVal;
				} else
				{
					if (time < lowTime) lowTime = time;
					if (time > highTime) highTime = time;
					if (lowVal < lowValue) lowValue = lowVal;
					if (highVal > highValue) highValue = highVal;
				}
			}
		}
		bounds = new Rectangle2D.Double(lowTime, lowValue, highTime-lowTime, highValue-lowValue);
	}
}
