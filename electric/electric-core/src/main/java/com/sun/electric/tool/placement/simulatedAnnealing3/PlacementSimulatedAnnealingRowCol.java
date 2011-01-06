/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: PlacementSimulatedAnnealingRowCol.java
 * Written by Team 6: Sebastian Roether, Jochen Lutz
 * Modified for column/row placement by Steven M. Rubin
 *
 * This code has been developed at the Karlsruhe Institute of Technology (KIT), Germany,
 * as part of the course "Multicore Programming in Practice: Tools, Models, and Languages".
 * Contact instructor: Dr. Victor Pankratius (pankratius@ipd.uka.de)
 *
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.electric.tool.placement.simulatedAnnealing3;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.tool.placement.PlacementFrame;
import com.sun.electric.util.math.Orientation;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Implementation of the simulated annealing placement algorithm for row/column placement.
 */
public class PlacementSimulatedAnnealingRowCol extends PlacementFrame
{
	// parameters
	private PlacementParameter numThreadsParam = new PlacementParameter("threads",
		"Number of threads:", Runtime.getRuntime().availableProcessors());
	private PlacementParameter maxRuntimeParam = new PlacementParameter("runtime",
		"Runtime (in seconds, 0 for no limit):", 240);
	private static final int PLACEMENTSTYLE_COLUMNS = 0;
//	private static final int PLACEMENTSTYLE_ROWS = 1;
	private PlacementParameter placementStyleParam = new PlacementParameter("placementStyle",
		"Placement Style:", 0, new String[] {"Column-based Placement", "Row-based Placement"});
	private PlacementParameter flipAlternateColsRows = new PlacementParameter("flipColRow",
		"Column/Row Placement: flip alternate columns/rows", true);

	// Temperature control
	private int iterationsPerTemperatureStep;	// if maximum runtime is > 0 this is calculated each temperature step
	private double startingTemperature;
	private double temperature;
	private int temperatureSteps;				// how often will the temperature be decreased
	private int temperatureStep;
	private int perturbationsTried;
	private long stepStartTime;
	private long timestampStart;
	private double maxChipLength;
	private int stepsPerUpdate = 50;

	/** list of proxy nodes */										private List<ProxyNode> nodesToPlace;
	/** map from original PlacementNodes to proxy nodes */			private Map<PlacementNode, ProxyNode> proxyMap;
	/** true if doing column placement */							private static boolean columnPlacement;
	/** number of stacks of cells */								private static int numStacks;
	/** the contents of the stacks */								private List<ProxyNode>[] stackContents;
	/** the height (of columns) or width (of rows) */				private double[] stackSizes;
	/** X coordinates (of columns) or Y coordinates (of rows) */	private static double[] stackCoords;
	/** indicator of stack usage in a thread */						private static boolean[] stacksBusy;
	/** for statistics gathering */									private int better, worse, worseAccepted;
	/** global random object */										private Random randNum = new Random();

	/**
	 * Method to return the name of this placement algorithm.
	 * @return the name of this placement algorithm.
	 */
	public String getAlgorithmName() { return "Simulated-Annealing-Row/Col"; }

	/**
	 * Method to do placement by simulated annealing.
	 * @param placementNodes a list of all nodes that are to be placed.
	 * @param allNetworks a list of all networks that connect the nodes.
	 * @param cellName the name of the cell being placed.
	 */
	public void runPlacement(List<PlacementNode> placementNodes, List<PlacementNetwork> allNetworks, String cellName)
	{
		columnPlacement = placementStyleParam.getIntValue() == PLACEMENTSTYLE_COLUMNS;

		// create proxies for placement nodes and insert in lists and maps
		nodesToPlace = new ArrayList<ProxyNode>(placementNodes.size());
		proxyMap = new HashMap<PlacementNode, ProxyNode>();
		for (PlacementNode p : placementNodes)
		{
			ProxyNode proxy = new ProxyNode(p);
			nodesToPlace.add(proxy);
			proxyMap.put(p, proxy);
		}

		// create an initial layout to be improved by simulated annealing
		initLayout();

		// make sure there aren't too many threads
		int threadCount = numThreadsParam.getIntValue();
		if (threadCount >= numStacks/2)
		{
			threadCount = numStacks/2-1;
			System.out.println("Note: Using only " + threadCount + " threads");
		}
		setParamterValues(threadCount, maxRuntimeParam.getIntValue());

		// Calculate the working area for the process.
		// nodes will not be placed outside the working area
		// This is because there is no use in moving nodes "miles" away.
		double totalArea = 0;
		for (ProxyNode node : nodesToPlace)
			totalArea += node.getWidth() * node.getHeight();
		maxChipLength = Math.sqrt(totalArea) * 4;

		// calculate the starting temperature
		temperatureStep = 0;
		perturbationsTried = 0;
		iterationsPerTemperatureStep = runtime > 0 ? 100 : getDesiredMoves();
		startingTemperature = getStartingTemperature(maxChipLength, runtime, allNetworks);
		temperature = startingTemperature;
		temperatureSteps = countTemperatureSteps(startingTemperature);

		// initialize statistics
		better = worse = worseAccepted = 0;

		// for stopping the algorithm in time
		timestampStart = System.currentTimeMillis();

		// for timing the steps
		stepStartTime = System.nanoTime();

		// start the Simulated Annealing threads
		SimulatedAnnealing[] threads = new SimulatedAnnealing[numOfThreads];
		for (int n = 0; n < numOfThreads; n++)
		{
			threads[n] = new SimulatedAnnealing();
			threads[n].start();
		}

		// wait for the threads to finish
		for (int i = 0; i < numOfThreads; i++)
		{
			try
			{
				threads[i].join();
			} catch (InterruptedException e)
			{
				e.printStackTrace();
			}
		}

		// apply the placement of the proxies to the actual nodes
		for (PlacementNode pn : placementNodes)
		{
			ProxyNode p = proxyMap.get(pn);
			pn.setPlacement(p.getPlacementX(), p.getPlacementY());
			pn.setOrientation(p.getPlacementOrientation());
		}

		// show results
		DecimalFormat formater = new DecimalFormat("###,###,###");
		String betterStr = formater.format(better);
		String worseStr = formater.format(worse);
		String worseAcceptedStr = formater.format(worseAccepted);
		System.out.println("Made " + betterStr + " moves that improve results, " +
			worseStr + " moves that worsened it (and " + worseAcceptedStr + " were accepted)");
	}

	/**
	 * Method that generates an initial node placement.
	 */
	private void initLayout()
	{
		double totalSize = 0;
		double maxGirth = 0;
		for(ProxyNode plNode : nodesToPlace)
		{
			totalSize += plNode.getCellSize();
			maxGirth = Math.max(maxGirth, plNode.getCellGirth());
		}
		double avgCellSize = totalSize / nodesToPlace.size();
		double avgStackSize = Math.sqrt(totalSize * maxGirth);
		numStacks = (int)Math.round(totalSize / avgStackSize);
		int numPerStack = (int)Math.ceil(nodesToPlace.size() / (double)numStacks);
		stackCoords = new double[numStacks];
		stackContents = new List[numStacks];
		stackSizes = new double[numStacks];
		stacksBusy = new boolean[numStacks];
		for(int i=0; i<numStacks; i++)
		{
			stackContents[i] = new ArrayList<ProxyNode>();
			stackSizes[i] = 0;
			stacksBusy[i] = false;
		}

		double girthPos = -maxGirth;
		double stackPos = 0;
		int stackIndex = -1;
		for (int i = 0; i < nodesToPlace.size(); i++)
		{
			if ((i % numPerStack) == 0)
			{
				girthPos += maxGirth;
				stackIndex++;
				stackPos = 0;
				stackCoords[stackIndex] = girthPos;
			}
			ProxyNode plNode = nodesToPlace.get(i);
			Orientation o = getOrientation(stackIndex);
			if (columnPlacement) plNode.setPlacement(girthPos, stackPos, stackIndex, o, true); else
				plNode.setPlacement(stackPos, girthPos, stackIndex, o, true);
			stackPos += avgCellSize;

			List<ProxyNode> pnList = stackContents[stackIndex];
			stackSizes[stackIndex] += plNode.getCellSize();
			pnList.add(plNode);
		}

		for(int i=0; i<numStacks; i++)
		{
			List<ProxyNode> pnList = stackContents[i];
			Collections.sort(pnList);
			evenStack(i);
		}
	}

	/**
	 * Method that counts how often the temperature will be decreased before going below 1.
	 */
	private int countTemperatureSteps(double startingTemperature)
	{
		double temperature = startingTemperature;
		int steps = 0;
		while (temperature > 1)
		{
			steps++;
			temperature = coolDown(temperature);
		}
		return steps;
	}

	/**
	 * Method to calculate the initial temperature. It uses the standard deviation
	 * of the net length for random placements to derive the starting temperature.
	 * @param length maximum length of the chip
	 * @return the initial temperature
	 */
	private double getStartingTemperature(double length, double runtime, List<PlacementNetwork> allNetworks)
	{
		double[] metrics = new double[1000]; // sample size
		double sigma = 0;
		double average = 0;

		double width = length;
		double height = length;

		// collect samples of the cost function
		Map<ProxyNode,Point2D> origCoords = new HashMap<ProxyNode,Point2D>();
		for (ProxyNode p : nodesToPlace)
			origCoords.put(p, new Point2D.Double(p.getPlacementX(), p.getPlacementY()));
		for (int i = 0; i < metrics.length; i++)
		{
			for (ProxyNode p : nodesToPlace)
				p.setPlacement(randNum.nextDouble() * width, randNum.nextDouble() * height, p.getColumnRowIndex(),
					p.getPlacementOrientation(), false);
			metrics[i] = netLength(allNetworks);
		}
		for (ProxyNode p : nodesToPlace)
		{
			Point2D origPt = origCoords.get(p);
			p.setPlacement(origPt.getX(), origPt.getY(), p.getColumnRowIndex(),
				p.getPlacementOrientation(), false);
		}

		// calculate average
		for (int i = 0; i < metrics.length; i++)
			average += metrics[i];
		average /= metrics.length;

		// calculate standard deviation
		for (int i = 0; i < metrics.length; i++)
			sigma += (metrics[i] - average) * (metrics[i] - average);
		sigma = Math.sqrt(sigma / metrics.length);

		// set starting temperature so that the acceptance function
		// accepts worsening of -sigma with a probability of 0.3
		// e^(-sigma /startingTemperature) = 0.3 # solve for startingTemperature
		// THIS IS TWEAKING DATA
		double startingTemperature = sigma * (-1 / (2 * Math.log(0.3)));

		// If a maximum runtime is set, only the last temperature steps are done
		if (runtime > 0)
		{
			double msPerMove = guessMillisecondsPerMove();
			int desired_moves = getDesiredMoves();
			int possibleSteps = Math.max((int) ((runtime * 1000) / (msPerMove * desired_moves)), 5);
			int stepsUntilStop = countTemperatureSteps(startingTemperature);

			for (int i = 0; i < stepsUntilStop - possibleSteps; i++)
				startingTemperature = coolDown(startingTemperature);
		}

		return startingTemperature;
	}

	/**
	 * Method to calculate the cooling of the simulated annealing process.
	 * @param temp the current temperature
	 * @return the lowered temperature
	 */
	private double coolDown(double temp)
	{
		return temp * 0.99 - 0.1;
	}

	/**
	 * Method that calculates how many moves per temperature step are necessary.
	 * @param nodes
	 * @return
	 */
	private int getDesiredMoves()
	{
		// THIS IS TWEAKING DATA
		return Math.max((int) (nodesToPlace.size() * Math.sqrt(nodesToPlace.size())), 8719);
	}

	private double guessMillisecondsPerMove()
	{
		double startTime = System.currentTimeMillis();
		double sampleSize = 20000;
		for (int i = 0; i < sampleSize; i++)
		{
			ProxyNode proxy = nodesToPlace.get(randNum.nextInt(nodesToPlace.size()));
			netLength(proxy.getNets());
		}

		// a move actually takes more time than we measured
		return (System.currentTimeMillis() - startTime) / sampleSize;
	}

	/**
	 * Synchronized method that does temperature and time control. Threads
	 * should call this periodically.
	 * @param tries how many perturbation the thread tried since last calling update
	 */
	private synchronized void update(int tries)
	{
		perturbationsTried += tries;

		// decrease temperature if enough iterations were done
		if (perturbationsTried >= iterationsPerTemperatureStep)
		{
			long stepDuration = System.nanoTime() - stepStartTime;
			stepStartTime = System.nanoTime();

			// adjust how many moves to try before the next temperature decrease
			if (runtime > 0)
			{
				// use the observed time from the last step to calculate how many moves to do
				long elapsedTime = System.currentTimeMillis() - timestampStart;
				long remainingTime = runtime * 1000 - elapsedTime;
				long remainingSteps = temperatureSteps - temperatureStep;
				double allowedTimePerStep = 1e6 * ((double) remainingTime / remainingSteps);

				iterationsPerTemperatureStep *= allowedTimePerStep / stepDuration;
				iterationsPerTemperatureStep = Math.max(stepsPerUpdate, iterationsPerTemperatureStep);
			}

			temperatureStep++;
			temperature = coolDown(temperature);
			perturbationsTried = 0;
		}
	}

	private Orientation getOrientation(int index)
	{
		Orientation o = Orientation.IDENT;
		if (flipAlternateColsRows.getBooleanValue() && (index%2) != 0)
		{
			if (columnPlacement) return Orientation.X;
			return Orientation.Y;
		}
		return o;
	}

	/**
	 * Class that does the actual simulated annealing. All instances of this
	 * class share the same data set. Simulated annealing goes like this:
	 *
	 * - find a measure of how good a placement is (the total net length)
	 * - define a "starting temperature"
	 * - given a placement, do something that could change this measure
	 *   (move nodes or swap them)
	 * - if the measure is better, fine. If not the probability of accepting this
	 *   depends on the temperature and on how much the placement is worse than
	 *   before. The higher the temperature, the higher the probability of bad
	 *   moves being accepted
	 * - decrease temperature
	 * - repeat until temperature reaches 0
	 *
	 * The effects of this implementations are:
	 * - The less likely it is that a move is accepted, the better the speedup
	 * - The more threads started, the more likely it is moves are conflicting when
	 *   accepted because they work with the same data
	 * - starting more threads than there are cores is most likely useless (?)
	 */
	class SimulatedAnnealing extends Thread
	{
		Random rand = new Random();

		public void run()
		{
			// Thread wont stop until temperature is below 1
			while (temperature > 1)
			{
				// Try some perturbations before checking if temperature has to be lowered
				for (int step = 0; step < stepsPerUpdate; step++)
				{
					// decide whether to work in two stacks or one
					ProxyNode node = null;
					int oldIndex, newIndex;
					int r = rand.nextInt(100);
					if (r < 5)
					{
						// work within one stack
						for(;;)
						{
							oldIndex = lockRandomStack();
							if (oldIndex < 0) continue;
							if (stackContents[oldIndex].size() <= 1) { releaseStack(oldIndex);  continue; }
							break;
						}
						newIndex = oldIndex;
						node = getRandomNode(oldIndex);
					} else
					{
						// work between two stacks
						for(;;)
						{
							oldIndex = lockRandomStack();
							if (oldIndex < 0) continue;
							newIndex = lockRandomStack();
							if (newIndex < 0) { releaseStack(oldIndex);  continue; }
							break;
						}
						node = getRandomNode(oldIndex);

						// swap stacks if it makes the lengths become more uniform
						if (stackSizes[newIndex] > stackSizes[oldIndex])
						{
							int swap = oldIndex;   oldIndex = newIndex;   newIndex = swap;
							node = getRandomNode(oldIndex);
						}
					}

					// add a random translation to the node's location but keep it inside the "working area"
					Point2D newPosition = randomMove(node);
					double newX = (node.getPlacementX() + newPosition.getX()
						* (0.1 + 0.1 * temperature / startingTemperature)) % maxChipLength;
					double newY = (node.getPlacementY() + newPosition.getY()
						* (0.1 + 0.1 * temperature / startingTemperature)) % maxChipLength;
					if (columnPlacement) newX = stackCoords[newIndex]; else
						newY = stackCoords[newIndex];
					Orientation newOrient = getOrientation(newIndex);

					// determine the network length before the move
					double networkMetricBefore = 0;
					for(PlacementNetwork net : node.getNets())
						networkMetricBefore += netLength(net, -1, -1);

					// make the move (set in temporary "proposed" variables)
					node.setProposed(newX, newY, newIndex, newOrient);
					if (oldIndex != newIndex)
					{
						// redo the old stack without the moved node
						double oldStackSize = stackSizes[oldIndex] - node.getCellSize();
						double bottom = -oldStackSize/2;
						List<ProxyNode> pnList = stackContents[oldIndex];
						for(ProxyNode pn : pnList)
						{
							if (pn == node) continue;
							double x = pn.getPlacementX();
							double y = pn.getPlacementY();
							double size = pn.getCellSize();
							if (columnPlacement) y = bottom + size/2; else
								x = bottom + size/2;
							bottom += size;
							pn.setProposed(x, y, pn.getColumnRowIndex(), pn.getPlacementOrientation());
						}

						// build new list for destination stack with moved node inserted
						boolean notInserted = true;
						List<ProxyNode> newList = new ArrayList<ProxyNode>();
						for(ProxyNode pn : stackContents[newIndex])
						{
							if (notInserted)
							{
								if (pn.compareTo(node) <= 0)
								{
									newList.add(node);
									notInserted = false;
								}
							}
							newList.add(pn);
						}
						if (notInserted) newList.add(node);

						// redo the new stack with the moved node
						double newStackSize = stackSizes[newIndex] + node.getCellSize();
						bottom = -newStackSize/2;
						for(ProxyNode pn : newList)
						{
							double x = pn.getPlacementX();
							double y = pn.getPlacementY();
							double size = pn.getCellSize();
							if (columnPlacement) y = bottom + size/2; else
								x = bottom + size/2;
							bottom += size;
							if (pn != node)
								pn.setProposed(x, y, pn.getColumnRowIndex(), pn.getPlacementOrientation());
						}
					} else
					{
						// rearrange the stack
						boolean notInserted = true;
						List<ProxyNode> newList = new ArrayList<ProxyNode>();
						for(ProxyNode pn : stackContents[oldIndex])
						{
							if (pn == node) continue;
							if (notInserted)
							{
								if (pn.compareTo(node) <= 0)
								{
									newList.add(node);
									notInserted = false;
								}
							}
							newList.add(pn);
						}
						if (notInserted) newList.add(node);

						// redo the new stack with the moved node
						double bottom = -stackSizes[oldIndex]/2;
						for(ProxyNode pn : newList)
						{
							double x = pn.getPlacementX();
							double y = pn.getPlacementY();
							double size = pn.getCellSize();
							if (columnPlacement) y = bottom + size/2; else
								x = bottom + size/2;
							bottom += size;
							pn.setProposed(x, y, pn.getColumnRowIndex(), pn.getPlacementOrientation());
						}
					}

					// determine the network length after the move
					double networkMetricAfter = 0;
					for (PlacementNetwork net : node.getNets())
						networkMetricAfter += netLength(net, newIndex, oldIndex);

					// the worse the gain of a perturbation, the lower the probability that this perturbation
					// is actually applied (positive gains are always accepted)
					double gain = networkMetricBefore - networkMetricAfter;
					if (gain < 0) worse++; else better++;
					if (Math.exp(gain / temperature) >= Math.random())
					{
						if (gain < 0) worseAccepted++;
						remove(node);
						node.setPlacement(node.getProposedX(), node.getProposedY(), node.getProposedIndex(),
							getOrientation(node.getProposedIndex()), true);
						put(node);
						evenStack(oldIndex);
						if (newIndex != oldIndex) evenStack(newIndex);
					}
					releaseStack(oldIndex);
					if (newIndex != oldIndex) releaseStack(newIndex);
				}

				update(stepsPerUpdate);
			}
		}

		/**
		 * Method that generates a random translation vector for a given ProxyNode.
		 * @param node ProxyNode to move.
		 * @return a translation vector for the ProxyNode.
		 */
		private Point2D randomMove(ProxyNode node)
		{
			// the length of the vector is a function of the maximum bounding box
			// the further away connected nodes are, the further the node may move
			double maxBoundingBox = maxChipLength;

			// shorter vectors are more likely
			// don't move nodes too far away if they are already well positioned
			double offsetX = Math.min(Math.max(rand.nextGaussian(), -1), 1) * maxBoundingBox;
			double offsetY = Math.min(Math.max(rand.nextGaussian(), -1), 1) * maxBoundingBox;
			return new Point2D.Double(offsetX, offsetY);
		}
	}

	private synchronized int lockRandomStack()
	{
		int index = randNum.nextInt(numStacks);
		for(int i=0; i<numStacks; i++)
		{
			if (!stacksBusy[index])
			{
				stacksBusy[index] = true;
				return index;
			}
			index = (index+1) % numStacks;
		}
		return -1;
	}

	private void releaseStack(int index)
	{
		stacksBusy[index] = false;
	}

	/**
	 * Method that approximates the conductor length of a set of nets when proxies are used
	 * @param networks the PlacementNetworks to analyze.
	 * @return the length of the connections on the networks.
	 */
	private double netLength(List<PlacementNetwork> networks)
	{
		double length = 0;
		for(PlacementNetwork net : networks)
			length += netLength(net, -1, -1);
		return length;
	}

	/**
	 * Method that calculates the bounding box net length approximation for a given net.
	 * It hashes the nodes of the ports in the nets to its proxies.
	 * Also, it may substitute a node with another one just for this calculation
	 * @param net the PlacementNetwork to analyze.
	 * @param workingIndex1 a stack number that is being "proposed".
	 * @param workingIndex1 another stack number that is being "proposed".
	 * @return the length of the connections on the network.
	 */
	private double netLength(PlacementNetwork net, int workingIndex1, int workingIndex2)
	{
		double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
		for (PlacementPort port : net.getPortsOnNet())
		{
			ProxyNode pn = proxyMap.get(port.getPlacementNode());
			double currX, currY;
			Orientation o;
			if (pn.getColumnRowIndex() == workingIndex1 || pn.getColumnRowIndex() == workingIndex2)
			{
				currX = pn.getProposedX();
				currY = pn.getProposedY();
				o = pn.getProposedOrientation();
			} else
			{
				currX = pn.getPlacementX();
				currY = pn.getPlacementY();
				o = pn.getPlacementOrientation();
			}
			if (o == Orientation.X) currX -= port.getOffX(); else
				currX += port.getOffX();
			if (o == Orientation.Y) currY -= port.getOffY(); else
				currY += port.getOffY();
			if (currX < minX) minX = currX;
			if (currX > maxX) maxX = currX;
			if (currY < minY) minY = currY;
			if (currY > maxY) maxY = currY;
		}
		return (maxX - minX) + (maxY - minY);
	}

	/**
	 * Method that finds a random ProxyNode in a specific stack.
	 * @param index the stack index.
	 * @return a random ProxyNode.
	 */
	private ProxyNode getRandomNode(int index)
	{
		List<ProxyNode> pnList = stackContents[index];
		return pnList.get(randNum.nextInt(pnList.size()));
	}

	/**
	 * Adds a node to the stack lists.
	 * @param node the ProxyNode to add to the stack lists.
	 */
	private void put(ProxyNode node)
	{
		int index = node.getColumnRowIndex();
		List<ProxyNode> pnList = stackContents[index];
		boolean notInserted = true;
		for(int i=0; i<pnList.size(); i++)
		{
			ProxyNode pn = pnList.get(i);
			if (pn.compareTo(node) > 0) continue;
			pnList.add(i, node);
			notInserted = false;
			break;
		}
		if (notInserted) pnList.add(node);
		stackSizes[index] += node.getCellSize();
	}

	/**
	 * Removes a node from all stack lists.
	 * @param node the ProxyNode to remove.
	 */
	private void remove(ProxyNode node)
	{
		int index = node.getColumnRowIndex();
		List<ProxyNode> pnList = stackContents[index];
		if (!pnList.remove(node))
		{
			System.out.println("COULD NOT REMOVE NODE FROM STACK "+index);
		}
		stackSizes[index] -= node.getCellSize();
	}

	/**
	 * Method to rearrange a stack so that all cells touch and are centered
	 * @param index the stack index.
	 */
	private void evenStack(int index)
	{
		double bottom = -stackSizes[index]/2;
		List<ProxyNode> pnList = stackContents[index];
		for(ProxyNode pn : pnList)
		{
			double x = pn.getPlacementX();
			double y = pn.getPlacementY();
			double size = pn.getCellSize();
			if (columnPlacement) y = bottom + size/2; else
				x = bottom + size/2;
			bottom += size;
			pn.setPlacement(x, y, index, pn.getPlacementOrientation(), true);
		}
	}

	/*************************************** Proxy Node **********************************/

	/**
	 * This class is a proxy for the actual PlacementNode.
	 * It can be moved around and rotated without touching the actual PlacementNode.
	 */
	static class ProxyNode implements Comparable<ProxyNode>
	{
		private double x, y;						// current location
		private double newX, newY;					// proposed location when testing rearrangement
		private int index;							// stack number (row or column)
		private int proposedIndex;					// proposed stack number (row or column)
		private Orientation orientation;			// orientation of the placement node
		private Orientation proposedOrientation;	// proposed orientation
		private double width = 0;					// width of the placement node
		private double height = 0;					// height of the placement node
		private List<PlacementNetwork> nets = new ArrayList<PlacementNetwork>();

		/**
		 * Constructor to create a ProxyNode
		 * @param node the PlacementNode that should is being shadowed.
		 */
		public ProxyNode(PlacementNode node)
		{
			x = node.getPlacementX();
			y = node.getPlacementY();

			NodeProto np = ((com.sun.electric.tool.placement.PlacementAdapter.PlacementNode)node).getType();
			Rectangle2D spacing = null;
			if (np instanceof Cell)
			{
				spacing = ((Cell)np).findEssentialBounds();
			}

			if (spacing == null)
			{
				width = node.getWidth();
				height = node.getHeight();
			} else
			{
				width = spacing.getWidth();
				height = spacing.getHeight();
			}

			// create a list of all nets that node belongs to
			for(PlacementPort p : node.getPorts())
			{
				if (!nets.contains(p.getPlacementNetwork()) && p.getPlacementNetwork() != null)
					nets.add(p.getPlacementNetwork());
			}
			orientation = node.getPlacementOrientation();
		}

		public void setProposed(double x, double y, int ind, Orientation o)
		{
			newX = x;
			newY = y;
			proposedIndex = ind;
			proposedOrientation = o;
		}

		/**
		 * Method to get the proposed X-coordinate of this ProxyNode.
		 * @return the proposed X-coordinate of this ProxyNode.
		 */
		public double getProposedX() { return newX; }

		/**
		 * Method to get the proposed Y-coordinate of this ProxyNode.
		 * @return the proposed Y-coordinate of this ProxyNode.
		 */
		public double getProposedY() { return newY; }

		/**
		 * Method to get the proposed stack index of this ProxyNode.
		 * @return the proposed stack index of this ProxyNode.
		 */
		public int getProposedIndex() { return proposedIndex; }

		/**
		 * Method to get the proposed orientation of this ProxyNode.
		 * @return the proposed orientation of this ProxyNode.
		 */
		public Orientation getProposedOrientation() { return proposedOrientation; }

		/**
		 * Method that sets the node to a new position.
		 * @param x the X coordinate.
		 * @param y the Y coordinate.
		 * @param ind the column/row index.
		 */
		public void setPlacement(double x, double y, int ind, Orientation o, boolean check)
		{
			if (check)
			{
				double xReq = stackCoords[ind];
				if (x != xReq)
					System.out.println("Moving node from ("+this.x+"["+this.index+"],"+this.y+") to ("+x+"["+ind+"],"+y+") BUT COLUMN "+ind+" IS AT "+xReq);
				Orientation oReq = (ind%2) == 0 ? Orientation.IDENT : Orientation.X;
				if (o != oReq)
					System.out.println("Moving node from ("+this.x+"["+this.index+"],"+this.y+")"+this.orientation.toString()+
						" to ("+x+"["+ind+"],"+y+")"+o.toString());
			}

			this.x = x;
			this.y = y;
			index = ind;
			orientation = o;
		}

		/**
		 * Method that returns the column or row number, when doing column/row-based placement.
		 * @return the column/row index.
		 */
		public int getColumnRowIndex() { return index; }

		/**
		 * Method to get a list of nets this node belongs to.
		 * This is more convenient than iterating over the ports.
		 * Also it only contains nets that are not ignored (e.g. because they are too huge).
		 * @return the list of nets this node belongs to.
		 */
		public List<PlacementNetwork> getNets() { return nets; }

		/**
		 * Method to get the X-coordinate of this ProxyNode.
		 * @return the X-coordinate of this ProxyNode.
		 */
		public double getPlacementX() { return x; }

		/**
		 * Method to get the Y-coordinate of this ProxyNode.
		 * @return the Y-coordinate of this ProxyNode.
		 */
		public double getPlacementY() { return y; }

		/**
		 * Method to get the width of this ProxyNode.
		 * @return the width of this ProxyNode.
		 */
		public double getWidth() { return width; }

		/**
		 * Method to get the height of this ProxyNode.
		 * @return the height of this ProxyNode.
		 */
		public double getHeight() { return height; }

		/**
		 * Method to get the size of this ProxyNode along the stack dimension.
		 * For column-based placement, this is the height;
		 * for row-based placement, this is the width;
		 * @return the size of this ProxyNode.
		 */
		public double getCellSize()
		{
			if (columnPlacement) return height;
			return width;
		}

		/**
		 * Method to get the girth of this ProxyNode, which separation of the stacks.
		 * For column-based placement, this is the width;
		 * for row-based placement, this is the height;
		 * @return the girth of this ProxyNode.
		 */
		public double getCellGirth()
		{
			if (columnPlacement) return width;
			return height;
		}

		/**
		 * Method to get the orientation of this ProxyNode.
		 * @return the orientation of this ProxyNode.
		 */
		public Orientation getPlacementOrientation() { return orientation; }

		public int compareTo(ProxyNode o)
		{
			if (columnPlacement)
			{
				double y1 = getPlacementY();
				double y2 = o.getPlacementY();
				if (y1 < y2) return 1;
				if (y1 > y2) return -1;
			} else
			{
				double x1 = getPlacementX();
				double x2 = o.getPlacementX();
				if (x1 < x2) return 1;
				if (x1 > x2) return -1;
			}
			return 0;
		}
	}
}
