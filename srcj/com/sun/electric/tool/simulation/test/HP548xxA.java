/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: HP548xxA.java
 * Written by Ron Ho and Tom O'Neill, Sun Microsystems.
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
package com.sun.electric.tool.simulation.test;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * API for controlling Agilent 5485xA oscilloscopes. Seems to work for 5484xA
 * oscilloscopes as well.
 */
public class HP548xxA extends Equipment {

	// Add 1 to these to get index in results string
	public static final int STAT_LAST = 0;

	public static final int STAT_MIN = 1;

	public static final int STAT_MAX = 2;

	public static final int STAT_MEAN = 3;

	public static final int STAT_DEV = 4;

	public static final int STAT_NSAMP = 5;

	/**
	 * Minimum number of samples required for each channel during
	 * accumulateFrequencies().
	 * 
	 * @see #accumulateFrequencies
	 */
	public static final int MIN_NSAMP = 10;

	/** Number of oscilloscope channels */
	public static final int NUM_CHAN = 4;

	/**
	 * Number of statistics reported by accumulateFrequencies() for each channel
	 * 
	 * @see #accumulateFrequencies
	 */
	public static final int RESULTS_PER_CHANNEL = 6;

	/**
	 * Number of seconds to accumulate statistics in accumulateFrequencies().
	 * Due to GPIB and other delays, this number should be at least 3.
	 */
	public static final float DELAY_FOR_SAMPLES = 4.f;

	/*
	 * Start of GPIB return value expected from RESULTS? inquiry in
	 * accumulateFrequencies()
	 */
	private static final String FREQ_NAME_START = "Frequency(";

	/** Creates a new instance of HP54855A */
	public HP548xxA(String name) {
		super(name);
	}

	/**
	 * Performs a single measurement of frequency using first full cycle on
	 * screen for the waveform on channel <code>channel</code> (=1..3). For
	 * greater accuracy, use accumulateFrequencies().
	 * 
	 * @see #accumulateFrequencies
	 */
	public float getFrequency(int channel) {
		write("MEAS:FREQ? CHAN" + channel);
		String s = read(20).trim();
		return new Float(s.split("\\s")[0]).floatValue();
	}

	/**
	 * Accumulates frequency statistics on oscilloscope channels
	 * <code>ichan = 1..nchan</code> until at least <code>MIN_NSAMP</code>
	 * frequency samples are recorded for each channel. The statistics are
	 * defined by the constants <code>STAT_LAST</code>, etc. Channel
	 * <code>ichan = 0</code> contains cumulative/average statistics for all
	 * <code>nchan</code> channels.
	 * 
	 * @param nchan
	 *            Number of scope channels to measure frequency on
	 * @return Frequency statistics array <code>float[ichan][statistic]</code>
	 * @see #STAT_LAST
	 * @see #STAT_MEAN
	 * @see #STAT_DEV
	 */
	public float[][] accumulateFrequencies(int nchan) {
		checkChannelNumber(nchan);
		write("MEAS:CLEAR");
		write("MEAS:STATISTICS ON");
		write("MEAS:FREQ CHAN1");
		if (nchan > 1)
			write("MEAS:FREQ CHAN2");
		if (nchan > 2)
			write("MEAS:FREQ CHAN3");
		if (nchan > 3)
			write("MEAS:FREQ CHAN4");

		float[][] results = new float[nchan + 1][RESULTS_PER_CHANNEL];
		int minSamples = MIN_NSAMP + 1;
		int minSamplesOld = 0;

		do {
                    try { Thread.sleep((int)(1000*DELAY_FOR_SAMPLES)); } catch (InterruptedException _) { }
			getStatistics(nchan, results);

			System.out.print("Got");
			for (int ichan = 1; ichan <= nchan; ichan++) {
				int nsamp = Math.round(results[ichan][STAT_NSAMP]);
				System.out.print(" " + nsamp);
				if (nsamp < minSamples)
					minSamples = nsamp;
			}
			System.out.println(" samples");

			if (minSamples <= minSamplesOld) {
				Infrastructure.fatal("At least one channel not accumulating");
			}
			minSamplesOld = minSamples;
		} while (minSamples < MIN_NSAMP);

		return results;
	}

	/*
	 * Returns the statistics reported by the oscilloscope. Queries the scope
	 * for results and decodes the result string.
	 */
	private void getStatistics(int nchan, float[][] results) {

		/*
		 * For each channel, the result string includes the sequence
		 * "Frequency(n),current,minimum,maximum,mean,std_dev,nsamp". The
		 * strings for each channel are also separated by commas. Note
		 * measurements don't come in channel order.
		 */
		write("MEAS:RESULTS?");
		String s = read(400).trim();
		String[] strings = s.split(",");

		// Channel 0 will get cumulative results
		results[0][STAT_LAST] = results[0][STAT_MEAN] = results[0][STAT_DEV] = 0.f;
		results[0][STAT_MIN] = Float.MAX_VALUE;
		results[0][STAT_MAX] = 0.f;
		results[0][STAT_NSAMP] = 0.f;

		for (int imeas = 1; imeas <= nchan; imeas++) {
			int ind = (RESULTS_PER_CHANNEL + 1) * (imeas - 1);
			String freqName = strings[ind];
			if (freqName.startsWith(FREQ_NAME_START) == false) {
				Infrastructure
						.fatal("Expected Frequency(n), found " + freqName);
			}
			String freqNum = freqName.substring(FREQ_NAME_START.length(),
					freqName.length() - 1);

			int ichan = Integer.parseInt(freqNum);

			for (int iresult = 0; iresult < RESULTS_PER_CHANNEL; iresult++) {
				results[ichan][iresult] = Float.parseFloat(strings[ind
						+ iresult + 1]);
			}

			results[0][STAT_NSAMP] += results[ichan][STAT_NSAMP];
			results[0][STAT_MEAN] += results[ichan][STAT_MEAN];
			results[0][STAT_MIN] = Math.min(results[0][STAT_MIN],
					results[ichan][STAT_MIN]);
			results[0][STAT_MAX] = Math.max(results[0][STAT_MAX],
					results[ichan][STAT_MAX]);
		}

		// Convert sum of means to average of means
		results[0][STAT_MEAN] = results[0][STAT_MEAN] / nchan;
	}

	/**
	 * Creates files <code>(name)(n).dat</code> containing the frequencies
	 * measured on scope channels n=1..nchan-1 as functions of the voltage
	 * provided on the specified <code>PowerChannel</code>. Another file
	 * <code>(name)all.dat</code> provides all <code>nchan</code> results at
	 * once for convenience. Voltage is returned to the original value at end of
	 * the sweep.
	 * 
	 * @param nameStart
	 *            Start of file name
	 * @param supply
	 *            Voltage control
	 * @param startV
	 *            Starting voltage, in Volts
	 * @param endV
	 *            Ending voltage, in Volts
	 * @param stepV
	 *            Voltage step, in Volts
	 * @param nchan
	 *            Number of scope channels to measure frequency on
	 */
	public void frequencyVsVoltage(String nameStart, PowerChannel supply,
			float startV, float endV, float stepV, int nchan) {
		checkChannelNumber(nchan);
		int startMilliV = Math.round(startV * 1000.f);
		int endMilliV = Math.round(endV * 1000.f);
		int stepMilliV = Math.round(stepV * 1000.f);
		float origVdd = supply.getVoltageSetpoint();

		System.out.println("HP548xxA.frequencyVsVoltage() scan:");
		try {
			PrintWriter[] files = new PrintWriter[nchan + 1];
			for (int ichan = 0; ichan <= nchan; ichan++) {
				files[ichan] = openFile(nameStart + ichan + ".dat");
			}
			PrintWriter file = openFile(nameStart + "all.dat");

			for (int mV = startMilliV; mV <= endMilliV; mV += stepMilliV) {
				float thisV = mV / 1000.f;
				supply.setVoltageWait(thisV);
				float[][] frequencies = accumulateFrequencies(nchan);

				System.out.print(thisV + ":");
				file.print(thisV);
				for (int ichan = 0; ichan <= nchan; ichan++) {
					System.out.print(" "
							+ frequencies[ichan][HP548xxA.STAT_MEAN]);
					file.print(" " + frequencies[ichan][HP548xxA.STAT_MEAN]);
					file.print(" " + frequencies[ichan][HP548xxA.STAT_DEV]);

					files[ichan].print(thisV + " " + ichan);
					files[ichan].print(" "
							+ frequencies[ichan][HP548xxA.STAT_MEAN]);
					files[ichan].print(" "
							+ frequencies[ichan][HP548xxA.STAT_DEV]);
					files[ichan].print(" "
							+ frequencies[ichan][HP548xxA.STAT_MIN]);
					files[ichan].print(" "
							+ frequencies[ichan][HP548xxA.STAT_MAX]);
					files[ichan].println(" "
							+ frequencies[ichan][HP548xxA.STAT_NSAMP]);
				}
				System.out.println();
				file.println();
			}

			file.close();
			for (int ichan = 0; ichan <= nchan; ichan++) {
				files[ichan].close();
			}
		} catch (Exception e) {
			System.err.println("exception occurred: " + e);
		} //end catch

		supply.setVoltageWait(origVdd);
	}

	/* Creates a frequencyVsVoltage() file and writes a SPICE header to it */
	private PrintWriter openFile(String name) throws IOException {
		PrintWriter file = new PrintWriter(new FileWriter(name));
		file.println("$DATA1 SOURCE='Lab' VERSION='1'");
		file.println(".TITLE '* file " + name + "'");
		file.println("vdd osctype freq_ave sigma freqmax freqmin samples");
		return file;
	}

	// Can be used to check channel index or number of channels
	private void checkChannelNumber(int nchan) {
		if (nchan <= 0 || nchan > NUM_CHAN) {
			Infrastructure.fatal("Bad channel index " + nchan);
		}
	}

}
