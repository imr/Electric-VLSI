/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: GeneticPlacement.java
 * Written by Team 3: Christian Wittner
 * 
 * This code has been developed at the Karlsruhe Institute of Technology (KIT), Germany, 
 * as part of the course "Multicore Programming in Practice: Tools, Models, and Languages".
 * Contact instructor: Dr. Victor Pankratius (pankratius@ipd.uka.de)
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
package com.sun.electric.tool.placement.genetic1.g1;

import com.sun.electric.database.geometry.Orientation;
import com.sun.electric.tool.placement.PlacementFrame;
import com.sun.electric.tool.placement.genetic1.Chromosome;
import com.sun.electric.tool.placement.genetic1.Population;
import com.sun.electric.tool.placement.genetic1.PopulationCreation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 *         Genetic placement test framework class. Allows to plug in to utilize
 *         different mutation, crossover, initial population creation and
 *         selection algorithms.
 */
public class GeneticPlacement extends PlacementFrame {

	int numThreads;
	int maxRuntime;
	boolean printDebugInformation;

	private int generation;

	private PopulationCreation popCreator;
	private Population population;
	static ThreadPoolExecutor threadpool;

	private double previousFittestSolutionValue = Double.MAX_VALUE;
	double improvementRate;
	private Chromosome fittestSolution;

	boolean isMemoryBarrierReached;
	Runtime rt;

	ArrayList<Population> subPopulations;
	private long startTotal;
	private long stopTotal;
	private int placementWidth = -1;

	public static Random randomGenerator;

	public final static double PlacementWidthRatio = 1.2;

	public void setBenchmarkValues(int runtime, int threads, boolean debug) {
		maxRuntime = runtime;
		numThreads = threads;
		printDebugInformation = debug;
	}

	public static Random getRandomGenerator() {
		return randomGenerator;
	}

	public static PlacementNodeProxy[] nodeProxies;

	// default values used for placement calculations
	// some can be altered through constructor call
	static int NBR_OF_THREADS;

	final static int EPOCH_LENGTH_START = 40;
	final static int EPOCH_LENGTH_MIN = 20;
	final static int EPOCH_LENGTH_MAX = 400;
	final static int EPOCH_LENGTH_STEP = 2;
	final static double EPOCH_LENGTH_RAISE = 0.005;
	final static double EPOCH_LENGTH_LOWER = 0.05;
	public static int current_epoch_length;

	final static int POPULATION_SIZE_PER_THREAD_START = 64;
	final static int POPULATION_SIZE_PER_THREAD_MIN = 32;
	final static int POPULATION_SIZE_PER_THREAD_MAX = 1024;
	final static int POPULATION_SIZE_PER_THREAD_STEP = 16;
	final static double POPULATION_SIZE_PER_THREAD_RAISE = 0.005;
	final static double POPULATION_SIZE_PER_THREAD_LOWER = 0.05;
	static int current_population_size_per_thread;

	public static long MAX_RUNTIME;
	public static long START_TIME;

	// log level and their meaning
	// OFF
	// SEVERE (highest value)
	// WARNING
	// INFO Whats happening in the master thread.
	// CONFIG
	// FINE Control flow of worker threads
	// FINER Genetic operations
	// FINEST (lowes) Datastructure level: Gene Chromsome etc.
	//	  
	// ALL
	//	  

	// level that will be displayed for all!
	final static Level LOGGER_LEVEL = Level.ALL;

	// level of this class each class should have this!
	final static Level LOG_LEVEL = Level.INFO;

	public final static boolean IS_PROGRESS_LOGGING_ENABLED = false;

	static String PROGRESS_LOG_FILENAME;
	static File PROGRESS_LOG_FILE;
	public static PrintWriter PROGRESS_LOGGER;

	// over all switch to disable code passages for logging
	public final static boolean IS_LOGGING_ENABLED = false;

	final static boolean DEBUG = true;
	final static boolean MEASURE_PERFORMANCE = false;

	final static String LOG_FILE_NAME = "genetic.log";

	static Calendar calendar;

	public static Logger logger;

	void init() {

		// reset variable values to default start values
		current_population_size_per_thread = POPULATION_SIZE_PER_THREAD_START;
		current_epoch_length = EPOCH_LENGTH_START;

		// use configuration evantually set by setBenchmark
		NBR_OF_THREADS = numThreads;

		MAX_RUNTIME = System.currentTimeMillis() + maxRuntime * 1000;
		START_TIME = System.currentTimeMillis();

		threadpool = (ThreadPoolExecutor) Executors
				.newFixedThreadPool(NBR_OF_THREADS);
		subPopulations = new ArrayList<Population>(NBR_OF_THREADS);

	}

	public static ExecutorService getThreadPool() {
		return threadpool;
	}

	static List<PlacementNetwork> allNetworks;

	public GeneticPlacement() {

		maxRuntime = 30;
		numThreads = Runtime.getRuntime().availableProcessors();

		popCreator = new PopulationCreationRandomWithPlaceHolder2(new Random(
				System.currentTimeMillis()));
		// popCreator = new PopulationOptimizedCreation();

		rt = Runtime.getRuntime();

		if (IS_PROGRESS_LOGGING_ENABLED) {
			PROGRESS_LOG_FILENAME = "progress_log_";
			Calendar now = Calendar.getInstance();
			PROGRESS_LOG_FILENAME = PROGRESS_LOG_FILENAME.concat(now
					.get(Calendar.YEAR)
					+ now.get(Calendar.MONTH)
					+ now.get(Calendar.DAY_OF_MONTH)
					+ "_"
					+ now.get(Calendar.HOUR_OF_DAY)
					+ now.get(Calendar.MINUTE)
					+ now.get(Calendar.SECOND)
					+ ".log");

			PROGRESS_LOG_FILE = new File(PROGRESS_LOG_FILENAME);
			try {
				PROGRESS_LOGGER = new PrintWriter(PROGRESS_LOG_FILE);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		if (IS_LOGGING_ENABLED) {
			logger = Logger.getLogger(GeneticPlacement.class.getName());
			logger.setLevel(LOGGER_LEVEL);

			try {
				Handler fh = new FileHandler(LOG_FILE_NAME, true);
				fh.setFormatter(new SimpleFormatter());
				logger.addHandler(fh);

			} catch (SecurityException e1) {
				System.err.println("couldn't create/append to log file "
						+ LOG_FILE_NAME);
				System.exit(-1);
			} catch (IOException e1) {
				System.err.println("couldn't create/append to log file "
						+ LOG_FILE_NAME);
				System.exit(-1);
			}
		}

		randomGenerator = new Random(System.currentTimeMillis());

	}

	@Override
	public String getAlgorithmName() {
		return "Genetic-1";
	}

	private static boolean beenRun = false;

	@Override
	public void runPlacement(List<PlacementNode> nodesToPlace,
			List<PlacementNetwork> allNetworks, String cellName) {

		if (beenRun) System.out.println("WARNING: The Genetic-1 placement code is not reentrant and can be run only once in an Electric session.");
		beenRun = true;

		init();

		if (GeneticPlacement.IS_LOGGING_ENABLED)
			System.out.println("nodes :" + nodesToPlace.size());

		if (GeneticPlacement.IS_LOGGING_ENABLED)
			logger.log(LOG_LEVEL, "cell:" + cellName + " nodes:"
					+ nodesToPlace.size() + " threads:" + NBR_OF_THREADS
					+ " population per thread:"
					+ POPULATION_SIZE_PER_THREAD_START + "epoch:"
					+ current_epoch_length);

		if (GeneticPlacement.IS_LOGGING_ENABLED)
			logger.log(LOG_LEVEL, "start wrapping placement nodes in proxies");

		// wrap placement nodes in proxy classes
		GeneticPlacement.nodeProxies = new PlacementNodeProxy[nodesToPlace
				.size()];

		for (int i = 0; i < nodesToPlace.size(); i++) {
			nodeProxies[i] = new PlacementNodeProxy(nodesToPlace.get(i));
		}

		if (GeneticPlacement.IS_LOGGING_ENABLED)
			logger.log(LOG_LEVEL, "done wrapping placement nodes in proxies");

		if (MEASURE_PERFORMANCE) {
			startTotal = System.currentTimeMillis();
		}

		GeneticPlacement.allNetworks = allNetworks;

		if (GeneticPlacement.IS_LOGGING_ENABLED)
			logger.log(LOG_LEVEL, "start population creation");
		spawnInitalPopulation(nodeProxies);

		if (GeneticPlacement.IS_LOGGING_ENABLED)
			logger.log(LOG_LEVEL, "done population creation");

		assert (population.chromosomes.size() % NBR_OF_THREADS == 0);

		subPopulations.clear();

		if (GeneticPlacement.IS_LOGGING_ENABLED)
			logger.log(LOG_LEVEL, "start partitioning population");
		// Initially partition population and distribute to the threads/cpus
		{
			while (!population.chromosomes.isEmpty()) {
				// create subpopulation
				Population subPopulation = new Population(
						POPULATION_SIZE_PER_THREAD_START);
				// assign subset of chromosomes to subpopulation
				for (int i = 0; i < POPULATION_SIZE_PER_THREAD_START; i++) {
					subPopulation.chromosomes.add(population.chromosomes
							.remove(0));
				}
				subPopulations.add(subPopulation);
			}
		}

		assert (subPopulations.size() == NBR_OF_THREADS);
		assert (population.chromosomes.size() == 0);

		ArrayList<SubPopulationProcessing> tasks = new ArrayList<SubPopulationProcessing>(
				NBR_OF_THREADS);

		if (GeneticPlacement.IS_LOGGING_ENABLED)
			logger.log(LOG_LEVEL, "done partitioning population");

		if (GeneticPlacement.IS_LOGGING_ENABLED)
			logger.log(LOG_LEVEL, "start parallel processing loop");

		// create worker threads
		for (int i = 0; i < NBR_OF_THREADS; i++) {
			// TODO: nbrOfGeneration has to be a multiple of EPOCH_LENGTH
			// TODO: does task have to be cleaned manually?
			tasks
					.add(new SubPopulationProcessing(
							current_epoch_length,
							randomGenerator.nextLong(),
							placementWidth,
							nodeProxies,
							allNetworks,
							subPopulations.get(0).chromosomes.get(0).Index2GenePositionInChromosome.length));
		}

		PopulationMutation2.resetMutationRates();

		// for (generation = 0; generation < NBR_OF_GENERATIONS; generation +=
		// EPOCH_LENGTH
		// * NBR_OF_THREADS) {
		if (GeneticPlacement.IS_LOGGING_ENABLED)
			System.out.println("current: " + System.currentTimeMillis()
					+ " start: " + START_TIME + " diff sec: "
					+ (System.currentTimeMillis() - START_TIME) / 1000);

		while (System.currentTimeMillis() < MAX_RUNTIME) {

			// have cpus calculate a epoch length
			for (int i = 0; i < NBR_OF_THREADS; i++) {
				tasks.get(i).setSubPolulation(subPopulations.get(i));
			}

			// check that each chromosome is only in one of the subpopulations
			assert (isChromosomeOnlyInOneSubPopulation());
			assert (tasks.size() == NBR_OF_THREADS);
			// execute threads in pool and wait for their termination
			try {
				List<Callable<Population>> copyList = new ArrayList<Callable<Population>>();
				for(SubPopulationProcessing pop : tasks) copyList.add(pop);
				List<Future<Population>> threadResults = threadpool.invokeAll(copyList);
				assert (threadpool.getPoolSize() == NBR_OF_THREADS);

				// verify if all tasks terminated normally
				for (Future<Population> f : threadResults) {
					f.get();
					assert (f.isDone());
				}

			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			// sort solutions by fitness and evenly distribute to the threads
			collectAndRedistributeIndividuals();

			// return best result of the global population
			if (GeneticPlacement.IS_LOGGING_ENABLED)
				System.out.println("Best fitness value: "
						+ fittestSolution.fitness + " improvement rate: "
						+ improvementRate);
		}

		if (GeneticPlacement.IS_LOGGING_ENABLED)
			logger.log(LOG_LEVEL, "done parallel processing loop");

		// TODO: apply placement of fittest solution

		// TODO: call our placement routine on the gene representation of the
		// placement nodes
		// should eventually done by evaluate method in the chromosome as it is
		// a
		// prerequisite
		// to calculate the metric

		// if fittest solution == null we probably haven't had time to finish at
		// least one
		// epoch!
		if (fittestSolution != null) {
			// apply gene placement to referenced placement nodes
			for (int i = 0; i < nodesToPlace.size(); i++) {
				nodesToPlace.get(i).setPlacement(fittestSolution.GeneXPos[i],
						fittestSolution.GeneYPos[i]);
				nodesToPlace.get(i).setOrientation(
						Orientation.fromAngle(fittestSolution.GeneRotation[i]));
			}
		}

		// write to images
		// PNGOut.write(fittestSolution);

		// write fittestSolution to xml
		// XMLStorage.writeChromosome2File(fittestSolution, "fittestChromsome"
		// + System.currentTimeMillis() + ".xml");

		// Chromosome cXML = XMLStorage.loadChromosome("fittestChromsome.xml");
		// assert (XMLStorage.isHavingSameValues(fittestSolution, cXML));

		if (MEASURE_PERFORMANCE) {
			stopTotal = System.currentTimeMillis();
			System.out.println("total placement time: "
					+ (stopTotal - startTotal) / 1000f + "sec");
		}

		threadpool.shutdownNow();
	}

	private void collectAndRedistributeIndividuals() {

		// sort all subpopulations
		for (Population p : subPopulations) {
			Collections.sort(p.chromosomes);
		}

		// pick any as when this code is reached first fittestSolution might
		// still be null
		fittestSolution = subPopulations.get(0).chromosomes.get(0);

		// remember fittest solution so far
		for (Population subp : subPopulations) {
			if (subp.chromosomes.get(0).fitness.doubleValue() < fittestSolution.fitness.doubleValue())
				fittestSolution = subp.chromosomes.get(0);
		}

		// replace the weakest solutions of each subpopulation with the
		// top performers of the other subpopulations
		{
			// calculate how many we can transfer
			// has to work with different nbr of threads an population sizes
			int subPopSize = subPopulations.get(0).chromosomes.size();

			// optimum pick 7% of top of each list
			int nbrOfExchangeIndividuals = (int) (.7 * subPopSize);

			// trim down as no more than 25% of a subpopulation should be
			// altered
			if (nbrOfExchangeIndividuals * numThreads > .25 * subPopSize)
				nbrOfExchangeIndividuals = (int) ((.25 * subPopSize) / numThreads);

			if (nbrOfExchangeIndividuals < 1)
				nbrOfExchangeIndividuals = 1;

			// now exchange the calculated numbers
			for (Population subp1 : subPopulations) {
				for (Population subp2 : subPopulations) {
					if (subp1 != subp2) {
						for (int i = 0; i < nbrOfExchangeIndividuals; i++) {

							// get chromosome of subp1 and copy information
							// copy is requirred not to have two thread
							// concurrently access one chromosome
							Chromosome c1 = subp1.chromosomes
									.get(subp1.chromosomes.size() - 1 - i);
							Chromosome c2 = subp2.chromosomes.get(i);

							c1.altered = c2.altered;
							c1.fitness = c2.fitness;
							for (int rotation = 0; rotation < c1.GeneRotation.length; rotation++) {
								c1.GeneRotation[rotation] = c2.GeneRotation[rotation];
							}
							for (int xpadding = 0; xpadding < c1.GeneXPadding.length; xpadding++) {
								c1.GeneXPadding[xpadding] = c2.GeneXPadding[xpadding];
							}
							for (int ypadding = 0; ypadding < c1.GeneYPadding.length; ypadding++) {
								c1.GeneYPadding[ypadding] = c2.GeneYPadding[ypadding];
							}
							for (int xpos = 0; xpos < c1.GeneXPos.length; xpos++) {
								c1.GeneXPos[xpos] = c2.GeneXPos[xpos];
							}
							for (int ypos = 0; ypos < c1.GeneYPos.length; ypos++) {
								c1.GeneYPos[ypos] = c2.GeneYPos[ypos];
							}
							for (int index = 0; index < c1.Index2GenePositionInChromosome.length; index++) {
								c1.Index2GenePositionInChromosome[index] = c2.Index2GenePositionInChromosome[index];
							}
						}
					}
				}
			}
		}

		generation += current_epoch_length * NBR_OF_THREADS;

		{

			// calculate improvement rate and set mutation rate accordingly
			improvementRate = (previousFittestSolutionValue - fittestSolution.fitness.doubleValue())
					/ previousFittestSolutionValue;
			previousFittestSolutionValue = fittestSolution.fitness.doubleValue();

			isMemoryBarrierReached = rt.totalMemory() == rt.maxMemory()
					&& rt.freeMemory() < 0.1 * rt.maxMemory();

			// if improvementRate = 1 don't touch a thing
			// probably just the first evaluation :)
			if (improvementRate < 1) {
				if (!isMemoryBarrierReached
						&& improvementRate > PopulationMutation2.MUTATION_RATE_INCREASE)
					PopulationMutation2.increaseMutationRate();
				else if (improvementRate < PopulationMutation2.MUTATION_RATE_LOWER)
					PopulationMutation2.lowerMutationRate();

				// adapt population size based on progress
				if (!isMemoryBarrierReached
						&& improvementRate < POPULATION_SIZE_PER_THREAD_RAISE
						&& current_population_size_per_thread < POPULATION_SIZE_PER_THREAD_MAX) {
					current_population_size_per_thread += POPULATION_SIZE_PER_THREAD_STEP;
				} else if (improvementRate > POPULATION_SIZE_PER_THREAD_LOWER
						&& current_population_size_per_thread > POPULATION_SIZE_PER_THREAD_MIN)
					current_population_size_per_thread -= POPULATION_SIZE_PER_THREAD_STEP;

				// adapt epoch length based on progress
				if (!isMemoryBarrierReached
						&& improvementRate < EPOCH_LENGTH_RAISE
						&& current_epoch_length < EPOCH_LENGTH_MAX) {
					current_epoch_length += EPOCH_LENGTH_STEP;
				} else if (improvementRate > EPOCH_LENGTH_LOWER
						&& current_epoch_length > EPOCH_LENGTH_MIN)
					current_epoch_length -= EPOCH_LENGTH_STEP;
			}

			// in debug print information
			if (GeneticPlacement.IS_LOGGING_ENABLED)
				logger.log(LOG_LEVEL, "Generation " + generation
						+ " best fitness value: " + fittestSolution.fitness
						+ " improvement rate: " + improvementRate);


		}
		
	}

	private void spawnInitalPopulation(PlacementNodeProxy[] nodeProxies) {
		// calculate all cell area
		int allCellArea = 0;
		for (PlacementNodeProxy proxy : nodeProxies) {
			allCellArea += proxy.width * proxy.height;
		}

		// calculate placementWidth
		allCellArea *= PlacementWidthRatio;
		placementWidth = (int) Math.sqrt(allCellArea);

		// TODO:make parallel
		{
			// create initial population
			population = popCreator.generatePopulation(nodeProxies,
					allNetworks, POPULATION_SIZE_PER_THREAD_START
							* NBR_OF_THREADS);

			assert (population.chromosomes.size() == POPULATION_SIZE_PER_THREAD_START
					* NBR_OF_THREADS);
		}
	}

	/**
	 * Use for assertion that a chromosome is only in one subpopulation.
	 * 
	 * @return true if each chromosome only in one subpopulation false
	 *         otherwise.
	 */
	private boolean isChromosomeOnlyInOneSubPopulation() {
		Population sp1, sp2;
		for (int i = 0; i < subPopulations.size(); i++) {
			sp1 = subPopulations.get(i);
			for (Chromosome c : sp1.chromosomes)
				for (int ii = i + 1; ii < subPopulations.size(); ii++) {
					sp2 = subPopulations.get(ii);
					if (sp1 != sp2 && sp2.chromosomes.contains(c))
						return false;
				}
		}

		return true;
	}

	/**
	 * @deprecated
	 * 
	 *             Don't use. For Performance test Purpose only.
	 * 
	 * @param nets
	 */
	public static void setAllNetworks(List<PlacementNetwork> nets) {
		allNetworks = nets;
	}

	public static List<PlacementNetwork> getAllNetworks() {
		return allNetworks;
	}

	public static int getNBR_OF_THREADS() {
		return NBR_OF_THREADS;
	}

	public static int getPOPULATION_SIZE() {
		return POPULATION_SIZE_PER_THREAD_START;
	}

	public void setNbrOfThreads(int nbrOfThreads) {
		NBR_OF_THREADS = nbrOfThreads;
		// TODO: eventual overhead as it done already in init()
		threadpool = (ThreadPoolExecutor) Executors
				.newFixedThreadPool(NBR_OF_THREADS);
		subPopulations = new ArrayList<Population>(NBR_OF_THREADS);

	}

	public void setEpochLength(int epochLength) {
		current_epoch_length = epochLength;

	}

	public void setPopulationSizePerThread(int populationSize) {
		current_population_size_per_thread = populationSize;

	}

	// full telemetry output to log file
	void logProgress(int population_size) {

		GeneticPlacement.PROGRESS_LOGGER
				.println((System.currentTimeMillis() - GeneticPlacement.START_TIME) / 1000
						+ ";"
						+ "main"
						+ ";"
						+ generation
						+ ";"
						+ fittestSolution.fitness
						+ ";"
						+ population_size
						+ ";"
						+ GeneticPlacement.current_population_size_per_thread
						+ ";"
						+ PopulationMutation2.chromosomeAlterPaddingRate
						+ ";"
						+ PopulationMutation2.genePaddingChangeRate_current
						+ ";"
						+ PopulationMutation2.chrosomeMaxPaddingChangeStep
						+ ";"
						+ PopulationMutation2.chromosomeMoveRate
						+ ";"
						+ PopulationMutation2.geneMoveRate_current
						+ ";"
						+ PopulationMutation2.geneMoveDistance
						+ ";"
						+ PopulationMutation2.chromosomeSwapRate
						+ ";"
						+ PopulationMutation2.geneSwapRate_current
						+ ";"
						+ PopulationMutation2.chromsomeRotationRate
						+ ";"
						+ improvementRate);

	}

	public int getRUNTIME() {
		return maxRuntime;
	}
}
