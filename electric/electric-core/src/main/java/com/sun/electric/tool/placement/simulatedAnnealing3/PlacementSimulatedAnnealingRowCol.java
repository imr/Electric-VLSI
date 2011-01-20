/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: PlacementSimulatedAnnealingRowCol.java
 * Written by Team 6: Sebastian Roether, Jochen Lutz
 * Rewritten for column/row placement by Steven M. Rubin
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

import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

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
		"Placement style:", 0, new String[] {"Column-based Placement", "Row-based Placement"});
	private PlacementParameter flipAlternateColsRows = new PlacementParameter("flipColRow",
		"Column/row placement: flip alternate columns/rows", true);
	private PlacementParameter makeStacksEven = new PlacementParameter("makeStacksEven",
		"Force rows/columns to be equal length", true);

	// Temperature control
	/** current "temperature" for allowing bad moves */				private double temperature;
	/** starting time of run (for termination and temp changes) */	private long timestampStart;
	/** number of moves per iteration of algorithm */				private int stepsPerUpdate;
	/** number of temperature steps before completion */			private int numTemperatureSteps;
	/** number of temperature steps completed so far */				private int numStepsDone;

	/** list of proxy nodes */										private List<ProxyNode> nodesToPlace;
	/** map from original PlacementNodes to proxy nodes */			private Map<PlacementNode, ProxyNode> proxyMap;
	/** true if doing column placement */							private boolean columnPlacement;
	/** number of stacks of cells */								private int numStacks;
	/** the contents of the stacks */								private List<ProxyNode>[] stackContents;
	/** the height (of columns) or width (of rows) */				private double[] stackSizes;
	/** X coordinates (of columns) or Y coordinates (of rows) */	private double[] stackCoords;
	/** indicator of stack usage in a thread */						private boolean[] stacksBusy;
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
		Set<Double> girths = new HashSet<Double>();
		for (PlacementNode p : placementNodes)
		{
			ProxyNode proxy = new ProxyNode(p);
			double girth = proxy.getCellGirth();
			girths.add(new Double(girth));
			nodesToPlace.add(proxy);
			proxyMap.put(p, proxy);
		}

		// warn if the girths are uneven
		if (girths.size() != 1)
		{
			StringBuffer sb = new StringBuffer();
			for(Double girth : girths)
				sb.append(" " + girth);
			System.out.println("WARNING: Stacks have uneven girth:" + sb.toString());
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

		// initialize temperature
		stepsPerUpdate = (int)Math.sqrt(nodesToPlace.size());
		temperature = 2000.0;
		numTemperatureSteps = countTemperatureSteps(temperature);
		numStepsDone = 0;
		timestampStart = System.currentTimeMillis();

		// initialize statistics
		better = worse = worseAccepted = 0;

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

		if (makeStacksEven.getBooleanValue())
		{
			// even the stacks as much as possible
			evenAllStacks(allNetworks);
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

	private void evenAllStacks(List<PlacementNetwork> allNetworks)
	{
		boolean [] stackConsidered = new boolean[numStacks];
		for(;;)
		{
			// find initial network length
			double initialLength = netLength(allNetworks);
			int bestOtherStack = -1;
			int placeInOtherStack = -1;
			double bestGain = 0;
			ProxyNode nodeToMove = null;
			for(int i=0; i<numStacks; i++) stackConsidered[i] = false;

			// now look at all tall stacks and find something that can be moved out of them
			for(;;)
			{
				// find tallest stack that hasn't already been considered
				int tallestStack = -1;
				for(int i=0; i<numStacks; i++)
				{
					if (stackConsidered[i]) continue;
					if (tallestStack < 0 || stackSizes[i] > stackSizes[tallestStack])
						tallestStack = i;
				}
				if (tallestStack < 0) break;
				stackConsidered[tallestStack] = true;

				// look at all nodes in the tall stack
				for(ProxyNode pn : stackContents[tallestStack])
				{
					double size = pn.getCellSize();

					// find another stack in which this node could go to even the sizes
					for(int i=0; i<numStacks; i++)
					{
						if (i == tallestStack) continue;
						if (stackSizes[i] + size > stackSizes[tallestStack] - size) continue;

						// could go in this stack: look for the best place for it
						for(int j=0; j<stackContents[i].size(); j++)
						{
							proposeMove(pn, tallestStack, i, j);

							double proposedLength = 0;
							for(PlacementNetwork net : allNetworks)
								proposedLength += netLength(net, tallestStack, i);
							double gain = initialLength - proposedLength;
							if (gain > bestGain)
							{
								bestOtherStack = i;
								placeInOtherStack = j;
								bestGain = gain;
								nodeToMove = pn;
							}
						}
					}
				}
				if (bestGain > 0)
				{
					// make the move
					proposeMove(nodeToMove, tallestStack, bestOtherStack, placeInOtherStack);
					implementMove(nodeToMove, tallestStack, bestOtherStack, placeInOtherStack);
					break;
				}
			}
			if (bestGain == 0) break;
		}
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
		double temp = startingTemperature;
		int steps = 0;
		while (temp > 1)
		{
			steps++;
			temp = coolDown(temp);
		}
		return steps;
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
					int r = rand.nextInt(numStacks);
					if (r == 0)
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

							// cannot play with stacks that are nearly empty
							if (stackContents[oldIndex].size() <= 1 && stackContents[newIndex].size() <= 1)
							{
								releaseStack(oldIndex);
								releaseStack(newIndex);
								continue;
							}
							break;
						}
						node = getRandomNode(oldIndex);

						// swap stacks if it makes the lengths become more uniform
						boolean doSwap = stackSizes[newIndex] > stackSizes[oldIndex];

//						// do not swap stacks during the first 75% of the time
//						if (numStepsDone < numTemperatureSteps * 3 / 4) doSwap = false;
//
//						// do force swap to prevent zero-size stacks
//						if (stackContents[oldIndex].size() <= 1) doSwap = true;

						// swap if requested
						if (doSwap)
						{
							int swap = oldIndex;   oldIndex = newIndex;   newIndex = swap;
							node = getRandomNode(oldIndex);
						}
					}

					int newPlaceInStack = rand.nextInt(stackContents[newIndex].size()+1);
					if (newIndex == oldIndex)
					{
						if (stackContents[newIndex].size() >= 2)
						{
							while(newPlaceInStack < stackContents[newIndex].size())
							{
								if (stackContents[newIndex].get(newPlaceInStack) != node &&
									(newPlaceInStack == 0 || stackContents[newIndex].get(newPlaceInStack-1) != node)) break;
								newPlaceInStack = rand.nextInt(stackContents[newIndex].size()+1);
							}
						}
						int oldPlaceInStack = stackContents[oldIndex].indexOf(node);
						if (oldPlaceInStack < newPlaceInStack) newPlaceInStack--;
					}

					// determine the network length before the move
					double networkMetricBefore = 0;
					for(PlacementNetwork net : node.getNets())
						networkMetricBefore += netLength(net, -1, -1);

					// make the move (set in temporary "proposed" variables)
					proposeMove(node, oldIndex, newIndex, newPlaceInStack);

					// determine the network length after the move
					double networkMetricAfter = 0;
					for (PlacementNetwork net : node.getNets())
						networkMetricAfter += netLength(net, newIndex, oldIndex);

					// the worse the gain of a perturbation, the lower the probability that this perturbation
					// is actually applied (positive gains are always accepted)
					double gain = networkMetricBefore - networkMetricAfter;
					if (gain < 0) worse++; else better++;
					if (gain > 0 || Math.exp(gain / temperature) >= Math.random())
					{
						if (gain < 0) worseAccepted++;
						implementMove(node, oldIndex, newIndex, newPlaceInStack);
					}
					releaseStack(oldIndex);
					if (newIndex != oldIndex) releaseStack(newIndex);
				}

				update();
			}
		}
	}

	/**
	 * Method that does temperature and time control. Threads
	 * should call this periodically.
	 */
	private void update()
	{
		// adjust how many moves to try before the next temperature decrease
		if (runtime > 0)
		{
			// use the time to calculate new temperature
			long elapsedTime = System.currentTimeMillis() - timestampStart;
			double fractionDone = elapsedTime / (runtime * 1000.0);
			int stepsToDo = (int)(numTemperatureSteps * fractionDone);
			for( ; numStepsDone < stepsToDo; numStepsDone++)
				temperature = coolDown(temperature);
		} else
		{
			temperature = coolDown(temperature);
		}
	}

	private void implementMove(ProxyNode node, int oldIndex, int newIndex, int newPlaceInStack)
	{
		remove(node);
		node.setPlacement(node.getProposedX(), node.getProposedY(), node.getProposedIndex(),
			getOrientation(node.getProposedIndex()), true);
		put(node, newIndex, newPlaceInStack);
		evenStack(oldIndex);
		if (newIndex != oldIndex) evenStack(newIndex);
	}

	private void proposeMove(ProxyNode node, int oldIndex, int newIndex, int newPlaceInStack)
	{
		Orientation newOrient = getOrientation(newIndex);
		if (oldIndex != newIndex)
		{
			// set proposed locations in the old stack without the moved node
			double bottom = 0;
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

			// set proposed locations in the new stack with the moved node
			bottom = 0;
			boolean notInserted = true;
			List<ProxyNode> newList = stackContents[newIndex];
			for(int i=0; i<newList.size(); i++)
			{
				ProxyNode pn = newList.get(i);
				Orientation o = pn.getPlacementOrientation();
				double x = pn.getPlacementX();
				double y = pn.getPlacementY();
				if (notInserted && i == newPlaceInStack)
				{
					if (columnPlacement) x = stackCoords[newIndex]; else
						y = stackCoords[newIndex];
					pn = node;
					o = newOrient;
					i--;
					notInserted = false;
				}
				double size = pn.getCellSize();
				if (columnPlacement) y = bottom + size/2; else
					x = bottom + size/2;
				bottom += size;
				pn.setProposed(x, y, newIndex, o);
			}
			if (notInserted)
			{
				double size = node.getCellSize();
				if (columnPlacement)
					node.setProposed(stackCoords[newIndex], bottom + size/2, newIndex, newOrient); else
						node.setProposed(bottom + size/2, stackCoords[newIndex], newIndex, newOrient);
			}
		} else
		{
			// redo the new stack with the moved node
			double bottom = 0;
			boolean notInserted = true;
			for(int i=0; i<stackContents[newIndex].size(); i++)
			{
				ProxyNode pn = stackContents[newIndex].get(i);
				if (pn == node) continue;
				if (notInserted && i == newPlaceInStack)
				{
					pn = node;
					i--;
					notInserted = false;
				}
				double x = pn.getPlacementX();
				double y = pn.getPlacementY();
				double size = pn.getCellSize();
				if (columnPlacement) y = bottom + size/2; else
					x = bottom + size/2;
				bottom += size;
				pn.setProposed(x, y, newIndex, pn.getPlacementOrientation());
			}
			if (notInserted)
			{
				double size = node.getCellSize();
				if (columnPlacement)
					node.setProposed(node.getPlacementX(), bottom + size/2, newIndex, newOrient); else
						node.setProposed(bottom + size/2, node.getPlacementY(), newIndex, newOrient);
			}
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
	private void put(ProxyNode node, int index, int position)
	{
		List<ProxyNode> pnList = stackContents[index];
		pnList.add(position, node);
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
			System.out.println("ERROR: could not remove node from stack "+index);
		}
		stackSizes[index] -= node.getCellSize();
	}

	/**
	 * Method to rearrange a stack so that all cells touch.
	 * @param index the stack index.
	 */
	private void evenStack(int index)
	{
		double bottom = 0;
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
	class ProxyNode implements Comparable<ProxyNode>
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
				double req = stackCoords[ind];
				if (columnPlacement)
				{
					if (x != req)
						System.out.println("Moving node from ("+this.x+"["+this.index+"],"+this.y+") to ("+x+"["+ind+"],"+y+") BUT STACK "+ind+" IS AT "+req);
				} else
				{
					if (y != req)
						System.out.println("Moving node from ("+this.x+","+this.y+"["+this.index+"]) to ("+x+","+y+"["+ind+"]) BUT STACK "+ind+" IS AT "+req);
				}
				Orientation oReq = getOrientation(ind);
				if (o != oReq)
					System.out.println("Rotating node from ("+this.x+","+this.y+")[S="+this.index+" O="+orientation.toString()+
						"] to ("+x+","+y+")[S="+ind+" O="+o.toString()+"] BUT O SHOULD BE '"+oReq.toString()+"'");
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
