/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Main.java
 *
 * Copyright (c) 2010 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.util.concurrent.test.blackScholes;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import com.sun.electric.database.text.TextUtils;
import com.sun.electric.tool.util.CollectionFactory;
import com.sun.electric.tool.util.concurrent.Parallel;
import com.sun.electric.tool.util.concurrent.datastructures.LockFreeStack;
import com.sun.electric.tool.util.concurrent.exceptions.PoolExistsException;
import com.sun.electric.tool.util.concurrent.patterns.PTask;
import com.sun.electric.tool.util.concurrent.patterns.PForJob.BlockedRange;
import com.sun.electric.tool.util.concurrent.patterns.PForJob.BlockedRange1D;
import com.sun.electric.tool.util.concurrent.patterns.PForJob.PForTask;
import com.sun.electric.tool.util.concurrent.runtime.Scheduler;
import com.sun.electric.tool.util.concurrent.runtime.Scheduler.SchedulingStrategy;
import com.sun.electric.tool.util.concurrent.runtime.taskParallel.ThreadPool;
import com.sun.electric.tool.util.concurrent.test.blackScholes.OptionData.OptionType;

/**
 * @author Felix Schmidt
 * 
 */
public class Main {

	public Main() {
	}

	public static void main(String[] args) throws IOException, PoolExistsException,
			InterruptedException {
		if (args.length != 6) {
			System.out
					.println("Parameters: <threads> <inputFile> <outputFile> <grain> <outtest> <scheduler:stack:queue:workStealing>");
		}

		int grain = 128;

		int numOfThreads = Integer.parseInt(args[0]);
		String inputFile = args[1];
		String outputFile = args[2];
		String outtest = null;
		String scheduler = args[5];
		try {
			SchedulingStrategy strategy = SchedulingStrategy.valueOf(scheduler);
		} catch (Exception ex) {
			System.out.println("No scheduler " + scheduler + " available. Use: "
					+ Scheduler.getAvailableScheduler());
			System.exit(1);
		}

		grain = Integer.parseInt(args[3]);
		outtest = args[4];

		List<OptionData> options = Main.readInputFile(inputFile);

		System.out.println("#options: " + options.size());
		System.out.println("#runs   : " + GlobalVars.NUM_RUNS);

		ThreadPool.initialize(new LockFreeStack<PTask>(), numOfThreads);

		long start = System.currentTimeMillis();

		Parallel.For(new BlockedRange1D(0, options.size(), grain), new BS_Task(options, false));

		long end = System.currentTimeMillis();

		ThreadPool.getThreadPool().shutdown();

		System.out.println(TextUtils.getElapsedTime(end - start));

		Main.writeOutputFile(outputFile, options);

		if (outtest != null) {
			BufferedWriter bw = new BufferedWriter(new FileWriter(outtest, true));
			bw.write(numOfThreads + "," + grain + "," + String.valueOf(end - start));
			bw.newLine();
			bw.flush();
		}
	}

	public static class BS_Task extends PForTask {

		private final List<OptionData> options;
		private final boolean debug;

		public BS_Task(List<OptionData> options, boolean debug) {
			this.options = options;
			this.debug = debug;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * com.sun.electric.tool.util.concurrent.patterns.PForJob.PForTask#execute
		 * (com.sun.electric.tool.util.concurrent.patterns.PForJob.BlockedRange)
		 */
		@Override
		public void execute(BlockedRange range) {
			BlockedRange1D tmpRange = (BlockedRange1D) range;

			double priceDelta = 0.0;

			for (int j = 0; j < GlobalVars.NUM_RUNS; j++) {
				for (int i = tmpRange.start(); i < tmpRange.end(); i++) {
					OptionData data = options.get(i);
					data.setPrice(BlkSchlsEqEuroNoDiv(data));

					if (debug) {
						priceDelta = data.getRefValue() - data.getPrice();
						if (Math.abs(priceDelta) >= 1e-4) {
							System.out.println("Error on " + i + ". Computed=" + data.getPrice()
									+ ", Ref=" + data.getRefValue() + ", Delta=" + priceDelta);
						}
					}
				}

			}
		}
	}

	private static double BlkSchlsEqEuroNoDiv(OptionData data) {
		double result = 0.0;

		double logValues = Math.log(data.getSpot() / data.getStrike());
		double xPowerTerm = data.getVolatility() * data.getVolatility() * 0.5;
		double xD1 = data.getRiskFree() + xPowerTerm;
		xD1 = xD1 * data.getTtm();
		xD1 = xD1 + logValues;

		double xDen = data.getVolatility() * Math.sqrt(data.getTtm());
		xD1 = xD1 / xDen;
		double xD2 = xD1 - xDen;

		double NofXd1 = CNDF(xD1);
		double NofXd2 = CNDF(xD2);

		double FutureValueX = data.getStrike() * (Math.exp(-(data.getRiskFree() * data.getTtm())));

		if (data.getType() == OptionType.call) {
			result = (data.getSpot() * NofXd1) - (FutureValueX * NofXd2);
		} else {
			double NegNofXd1 = (1.0 - NofXd1);
			double NegNofXd2 = (1.0 - NofXd2);

			result = (FutureValueX * NegNofXd2) - (data.getSpot() * NegNofXd1);
		}

		return result;
	}

	private static double CNDF(double inputX) {
		int sign = 0;

		if (inputX < 0.0) {
			inputX = -inputX;
			sign = 1;
		}

		double expValues = Math.exp(-0.5f * inputX * inputX);
		double xNPrimeofX = expValues;
		xNPrimeofX = xNPrimeofX * GlobalVars.inv_sqrt_2xPI;

		double xK2 = 1.0 / (1.0 + (0.2316419 * inputX));
		double xK2_2 = xK2 * xK2;
		double xK2_3 = xK2_2 * xK2;
		double xK2_4 = xK2_3 * xK2;
		double xK2_5 = xK2_4 * xK2;

		double xLocal_1 = xK2 * 0.319381530;
		double xLocal_2 = xK2_2 * (-0.356563782);
		double xLocal_3 = xK2_3 * 1.781477937;
		xLocal_2 = xLocal_2 + xLocal_3;
		xLocal_3 = xK2_4 * (-1.821255978);
		xLocal_2 = xLocal_2 + xLocal_3;
		xLocal_3 = xK2_5 * 1.330274429;
		xLocal_2 = xLocal_2 + xLocal_3;

		xLocal_1 = xLocal_2 + xLocal_1;
		double xLocal = xLocal_1 * xNPrimeofX;
		xLocal = 1.0 - xLocal;

		if (sign == 1) {
			xLocal = 1.0 - xLocal;
		}

		return xLocal;
	}

	private static List<OptionData> readInputFile(String inputFile) throws IOException {
		List<OptionData> options = CollectionFactory.createLinkedList();

		BufferedReader reader = new BufferedReader(new FileReader(new File(inputFile)));
		String numberLine = reader.readLine();
		int numberOfOptions = Integer.parseInt(numberLine);

		for (int i = 0; i < numberOfOptions; i++) {
			options.add(parseOptionData(reader.readLine()));
		}

		reader.close();

		return options;
	}

	private static void writeOutputFile(String inputFile, List<OptionData> data) throws IOException {

		PrintWriter writer = new PrintWriter(new File(inputFile));

		int i = 0;
		for (OptionData d : data) {
			writer.println(i + ";" + d.getPrice());
			i++;
		}
		writer.close();
	}

	// S, K, r, q, vol, T, P/C, Divs, DG RefValue
	private static OptionData parseOptionData(String line) {
		OptionData data = new OptionData();

		String[] splited = line.split(" ");

		data.setSpot(Double.parseDouble(splited[0]));
		data.setStrike(Double.parseDouble(splited[1]));
		data.setRiskFree(Double.parseDouble(splited[2]));
		data.setDivq(Double.parseDouble(splited[3]));
		data.setVolatility(Double.parseDouble(splited[4]));
		data.setTtm(Double.parseDouble(splited[5]));

		if (splited[6].toLowerCase() == "c") {
			data.setType(OptionType.call);
		} else {
			data.setType(OptionType.put);
		}

		data.setDivs(Double.parseDouble(splited[7]));

		data.setRefValue(Double.parseDouble(splited[splited.length - 1]));

		return data;
	}

}
