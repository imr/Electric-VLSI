/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Sim.java
 * Asynchronous Logic Simulator engine
 * Original C Code written by Brent Serbin and Peter J. Gallant
 * Translated to Java by Steven M. Rubin, Sun Microsystems.
 *
 * Copyright (c) 2005 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.simulation.als;

import com.sun.electric.database.text.TextUtils;
import com.sun.electric.tool.simulation.DigitalSignal;
import com.sun.electric.tool.simulation.Signal;
import com.sun.electric.tool.simulation.Simulation;
import com.sun.electric.tool.simulation.Stimuli;
import com.sun.electric.tool.simulation.TimedSignal;
import com.sun.electric.tool.simulation.als.ALS.Load;
import com.sun.electric.tool.simulation.als.ALS.Stat;
import com.sun.electric.tool.user.waveform.Panel;
import com.sun.electric.tool.user.waveform.WaveformWindow;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

/**
 * Class to do the engine of the ALS Simulator.
 */
public class Sim
{
	private ALS als;
	private List<Load> chekList = new ArrayList<Load>();
//	private Load       chekRoot;
	private HashMap<ALS.Node,List<ALS.Trak>> tracking;

	private static String [] stateDesc = {"High", "Undefined", "Low"};
	private static String [] strengthDesc = {"Off-", "Weak-", "Weak-", "", "", "Strong-", "Strong-"};

	boolean tracing = false;

	Sim(ALS als)
	{
		this.als = als;
	}

	/**
	 * Method to initialize the simulator for a simulation run.  The
	 * vector link list is copied to a master event scheduling link list and
	 * the database is initialized to its starting values.  After these housekeeping
	 * tasks are completed the simulator is ready to start the actual simulation.
	 * Returns the time where the simulation quiesces.
	 */
	double initializeSimulator(boolean force)
	{
		als.timeAbs = 0.0;
		tracking = new HashMap<ALS.Node,List<ALS.Trak>>();

		while (als.linkFront != null)
		{
			ALS.Link link = als.linkFront;
			if (als.linkFront.down != null)
			{
				als.linkFront.down.right = als.linkFront.right;
				als.linkFront = als.linkFront.down;
			} else
			{
				als.linkFront = als.linkFront.right;
			}
		}
		als.linkBack = null;

		for (ALS.Link linkHead = als.setRoot; linkHead != null; linkHead = linkHead.right)
		{
			ALS.Link linkPtr2 = new ALS.Link();
			if (linkPtr2 == null) return(als.timeAbs);
			linkPtr2.type = linkHead.type;
			linkPtr2.ptr = linkHead.ptr;
			linkPtr2.state = linkHead.state;
			linkPtr2.strength = linkHead.strength;
			linkPtr2.priority = linkHead.priority;
			linkPtr2.time = linkHead.time;
			linkPtr2.primHead = null;
			insertLinkList(linkPtr2);
		}


		for(ALS.Node nodeHead : als.nodeList)
		{
			nodeHead.sumState = Stimuli.LOGIC_LOW;
			nodeHead.sumStrength = Stimuli.OFF_STRENGTH;
			nodeHead.newState = new Integer(Stimuli.LOGIC_LOW);
			nodeHead.newStrength = Stimuli.OFF_STRENGTH;
			nodeHead.arrive = 0;
			nodeHead.depart = 0;
			nodeHead.tLast = 0.0;
			for(Stat statHead : nodeHead.statList)
			{
				statHead.newState = Stimuli.LOGIC_LOW;
				statHead.newStrength = Stimuli.OFF_STRENGTH;
				statHead.schedOp = 0;
			}
		}

		// now run the simulation
		boolean update = Simulation.isBuiltInResimulateEach();
		if (force) update = true;
		if (update)
		{
			// fire events until end of time or quiesced
			System.out.print("Simulating...");

			// determine highest time to simulate
			Rectangle2D bounds = als.an.getBounds();
			double tMax = bounds.getMaxX();
			for(Iterator<Panel> it = als.ww.getPanels(); it.hasNext(); )
			{
				Panel wp = it.next();
				double panelMax = wp.getMaxXAxis();
				if (panelMax > tMax) tMax = panelMax;
			}

			while (als.linkFront != null && als.linkFront.time <= tMax)
			{
				if (fireEvent()) break;
				if (chekList.size() != 0)
				{
					if (scheduleNewEvents()) break;
				}
			}

			// redisplay results
			fillDisplayArrays();
			System.out.println("Done.  Ran to time " + TextUtils.convertToEngineeringNotation(als.timeAbs));
		}

		return als.timeAbs;
	}

	/**
	 * Method to extract the ALS simulation data and update the Stimuli database
	 */
	private void fillDisplayArrays()
	{
		HashSet<DigitalSignal> sigsChanged = new HashSet<DigitalSignal>();
		for(ALS.Node node : tracking.keySet())
		{
			DigitalSignal sig = node.sig;
			List<ALS.Trak> trakHeads = tracking.get(node);
			int count = trakHeads.size();
			double [] timeVector = new double[count+1];
			int [] stateVector = new int[count+1];
			timeVector[0] = 0;
			stateVector[0] = Stimuli.LOGIC_LOW | Stimuli.OFF_STRENGTH;
			int j=1;
			for(ALS.Trak trakHead : trakHeads)
			{
				timeVector[j] = trakHead.time;
				stateVector[j] = trakHead.state;
				j++;
			}
			sig.setTimeVector(timeVector);
			sig.setStateVector(stateVector);
			sigsChanged.add(sig);
		}

		// set all other signals to "empty"
		int [] stateVector = new int[1];
		stateVector[0] = 0;
		double [] timeVector = new double[1];
		timeVector[0] = 0;
		for(Signal s : als.an.getSignals())
		{
			DigitalSignal sig = (DigitalSignal)s;
			if (sigsChanged.contains(sig)) continue;
			sig.setTimeVector(timeVector);
			sig.setStateVector(stateVector);
		}
		
		als.ww.repaint();
	}

	/**
	 * Method to get the entry from the front of the event scheduling
	 * link list and updates the database accordingly.  If a node is updated by a
	 * user defined vector the node value is changed as specified in the linklist
	 * entry.  If a transition fired, all the output nodes are updated as specified
	 * in the truth table for the transtion.  Returns true on error.
	 */
	private boolean fireEvent()
	{
		als.timeAbs = als.linkFront.time;
		ALS.Link linkHead = als.linkFront;
		if (als.linkFront.down != null)
		{
			als.linkFront = als.linkFront.down;
			als.linkFront.left = null;
			als.linkFront.right = linkHead.right;
			als.linkFront.up = linkHead.up;
			if (als.linkFront.right != null)
			{
				als.linkFront.right.left = als.linkFront;
			} else
			{
				als.linkBack = als.linkFront;
			}
		} else
		{
			als.linkFront = als.linkFront.right;
			if (als.linkFront != null)
			{
				als.linkFront.left = null;
			} else
			{
				als.linkBack = null;
			}
		}

		tracing = false;
		switch (linkHead.type)
		{
			case 'G':
				ALS.Stat statHead = (ALS.Stat)linkHead.ptr;
				if (statHead.nodePtr.traceNode)
				{
					String s2 = als.computeNodeName(statHead.nodePtr);
					System.out.println(TextUtils.convertToEngineeringNotation(als.timeAbs) +
						": Firing gate " + statHead.primPtr.name + statHead.primPtr.level + ", net " + s2);
					tracing = true;
				}
				if (statHead.schedState != linkHead.state ||
					statHead.schedOp != linkHead.operatr ||
					statHead.schedStrength != linkHead.strength)
				{
					break;
				}
				statHead.schedOp = 0;

				char operatr = linkHead.operatr;
				int operand = 0;
				if (operatr < 128)
				{
					operand = ((Integer)linkHead.state).intValue();
				} else
				{
					operatr -= 128;
//					ALS.Node nodeHead = (ALS.Node)linkHead.state;
//					operand = nodeHead.sumState;
				}

				int state = 0;
				switch (operatr)
				{
					case '=':
						state = operand;
						break;
					case '+':
						state = statHead.nodePtr.sumState + operand;
						break;
					case '-':
						state = statHead.nodePtr.sumState - operand;
						break;
					case '*':
						state = statHead.nodePtr.sumState * operand;
						break;
					case '/':
						state = statHead.nodePtr.sumState / operand;
						break;
					case '%':
						state = statHead.nodePtr.sumState % operand;
						break;
					default:
						System.out.println("Invalid arithmetic operator: " + operatr);
						return true;
				}

				if (state == statHead.newState &&
					linkHead.strength == statHead.newStrength)
				{
					break;
				}
				statHead.newState = state;
				statHead.newStrength = linkHead.strength;
				createCheckList(statHead.nodePtr, linkHead);
				break;

			case 'N':
				ALS.Node nodeHead = (ALS.Node)linkHead.ptr;
				if (nodeHead.traceNode)
				{
					String s2 = als.computeNodeName(nodeHead);
					System.out.println(TextUtils.convertToEngineeringNotation(als.timeAbs) + ": Changed state of net " + s2);
					tracing = true;
				}
				if (linkHead.state == nodeHead.newState &&
					linkHead.strength == nodeHead.newStrength)
						break;

				nodeHead.newState = linkHead.state;
				nodeHead.newStrength = linkHead.strength;
				createCheckList(nodeHead, linkHead);
				break;

			case 'C':
				double time = als.timeAbs;
				ALS.Row rowHead = (ALS.Row)linkHead.ptr;
				for(Object obj : rowHead.inList)
				{
					ALS.Link vectHead = (ALS.Link)obj;
					ALS.Link linkPtr2 = new ALS.Link();
					linkPtr2.type = 'N';
					linkPtr2.ptr = vectHead.ptr;
					linkPtr2.state = vectHead.state;
					linkPtr2.strength = vectHead.strength;
					linkPtr2.priority = vectHead.priority;
					linkPtr2.time = time;
					linkPtr2.primHead = null;
					insertLinkList(linkPtr2);
					time += vectHead.time;
				}
				if (((Integer)linkHead.state).intValue() == 0)
				{
					calculateClockTime(linkHead, rowHead);
					return false;
				}
				linkHead.state = new Integer(((Integer)linkHead.state).intValue() - 1);
				if (((Integer)linkHead.state).intValue() != 0)
				{
					calculateClockTime(linkHead, rowHead);
					return false;
				}
		}

		return false;
	}

	/**
	 * Method to calculate the sum state and strength for a node and if
	 * it has changed from a previous check it will enter the input transition list
	 * into a master check list that is used by the event scheduling routine.
	 * It should be noted that it is neccessary to calculate the sum strength and
	 * state for a node because it is possible to have nodes that have more than
	 * one transition driving it.
	 */
	private void createCheckList(ALS.Node nodeHead, ALS.Link linkHead)
	{
		// get initial state of the node
		int state = ((Integer)nodeHead.newState).intValue();
		int strength = nodeHead.newStrength;

		// print state of signal if this signal is being traced
		if (tracing)
		{
			System.out.println("  Formerly " + strengthDesc[nodeHead.sumStrength] + stateDesc[nodeHead.sumState+3] +
				", starts at " + strengthDesc[strength] + stateDesc[state+3]);
		}

		// look at all factors affecting the node
		for(Stat statHead : nodeHead.statList)
		{
			int thisState = statHead.newState;
			int thisStrength = statHead.newStrength;
			if (tracing)
				System.out.println("    " + strengthDesc[thisStrength] + stateDesc[thisState+3] +
					" from " + statHead.primPtr.name + statHead.primPtr.level);

			// higher strength overrides previous node state
			if (thisStrength > strength)
			{
				state = thisState;
				strength = thisStrength;
				continue;
			}

			// same strength: must arbitrate
			if (thisStrength == strength)
			{
				if (thisState != state)
					state = Stimuli.LOGIC_X;
			}
		}

		// if the node has nothing driving it, set it to the old value
		if (strength == Stimuli.OFF_STRENGTH)
		{
			state = nodeHead.sumState;
			strength = Stimuli.NODE_STRENGTH;
		}

		// stop now if node state did not change
		if (nodeHead.sumState == state && nodeHead.sumStrength == strength)
		{
			if (tracing) System.out.println("    NO CHANGE");
			return;
		}

		if (nodeHead.sig != null)
		{
			List<ALS.Trak> nodeData = tracking.get(nodeHead);
			if (nodeData == null)
			{
				nodeData = new ArrayList<ALS.Trak>();
				tracking.put(nodeHead, nodeData);
			}

			ALS.Trak trakHead = new ALS.Trak();
			trakHead.state = state | strength;
			trakHead.time = als.timeAbs;
			nodeData.add(trakHead);
		}
		if (tracing)
			System.out.println("    BECOMES " + strengthDesc[strength] + stateDesc[state+3]);

		nodeHead.sumState = state;
		nodeHead.sumStrength = strength;
		nodeHead.tLast = als.timeAbs;

		als.driveNode = nodeHead;
		for(Load l : nodeHead.pinList)
			chekList.add(l);
	}

	/**
	 * Method to examine the truth tables for the transitions that are
	 * specified in the checking list.  If there is a match between a truth table
	 * entry and the state of the logic network the transition is scheduled
	 * for firing.  Returns true on error.
	 */
	private boolean scheduleNewEvents()
	{
		// make a copy of the event list, and clear the main list
		List<Load> chekListCopy =  new ArrayList<Load>();
		for(Load l : chekList)
			chekListCopy.add(l);
		chekList.clear();

		for(Load chekHead : chekListCopy)
		{
			ALS.Model primHead = (ALS.Model)chekHead.ptr;
			if (primHead.type == 'F')
			{
				ALS.Func funcHead = (ALS.Func)primHead.ptr;
				funcHead.procPtr.simulate(primHead);
				continue;
			}

			for (ALS.Row rowHead = (ALS.Row)primHead.ptr; rowHead != null; rowHead = rowHead.next)
			{
				int flag = 1;
				for(Object obj : rowHead.inList)
				{
					ALS.IO ioHead = (ALS.IO)obj;
					int operatr = ioHead.operatr;
					int operand;
					if (operatr < 128)
					{
						operand = ((Integer)ioHead.operand).intValue();
					} else
					{
						operatr -= 128;
						ALS.Node nodeHead = (ALS.Node)ioHead.operand;
						operand = nodeHead.sumState;
					}

					switch (operatr)
					{
						case '=':
							if (((ALS.Node)ioHead.nodePtr).sumState != operand) flag = 0;
							break;
						case '!':
							if (((ALS.Node)ioHead.nodePtr).sumState == operand) flag = 0;
							break;
						case '<':
							if (((ALS.Node)ioHead.nodePtr).sumState >= operand) flag = 0;
							break;
						case '>':
							if (((ALS.Node)ioHead.nodePtr).sumState <= operand) flag = 0;
							break;
						default:
							System.out.println("Invalid logical operator: " + operatr);
							return true;
					}

					if (flag == 0) break;
				}

				if (flag != 0)
				{
					calculateEventTime(primHead, rowHead);
					break;
				}
			}
		}
		return false;
	}

	/**
	 * Method to calculate the time when the next occurance of a set of
	 * clock vectors is to be added to the event scheduling linklist.
	 *
	 * Calling Arguments:
	 *	linkHead = pointer to the link element to be reinserted into the list
	 *  rowHead  = pointer to a row element containing timing information
	 */
	private void calculateClockTime(ALS.Link linkHead, ALS.Row rowHead)
	{
		double time = als.timeAbs;

		if (rowHead.delta != 0) time += rowHead.delta;
		if (rowHead.linear != 0)
		{
			double prob = Math.random();
			time += 2.0 * prob * rowHead.linear;
		}

		/*
		 * if (rowHead.exp)
		 * {
		 * 	prob = rand() / MAXINTBIG;
		 * 	time += (-log(prob) * (rowHead.exp));
		 * }
		 */

		linkHead.time = time;
		insertLinkList(linkHead);
	}

	/**
	 * Method to calculate the time of occurance of an event and then
	 * places an entry into the event scheduling linklist for later execution.
	 * Returns true on error.
	 *
	 * Calling Arguments:
	 *	primHead  = pointer to the primitive to be scheduled for firing
	 *	rowHead  = pointer to the row containing the event to be scheduled
	 */
	private void calculateEventTime(ALS.Model primHead, ALS.Row rowHead)
	{
		double time = 0.0;
		int priority = primHead.priority;

		if (rowHead.delta != 0) time += rowHead.delta;
		if (rowHead.abs != 0) time += rowHead.abs;
		if (rowHead.linear != 0)
		{
			double prob = Math.random();
			time += 2.0 * prob * rowHead.linear;
		}

		/*
		 * if (rowHead.exp)
		 * {
		 * 	prob = rand() / MAXINTBIG;
		 * 	time += (-log(prob) * (rowHead.exp));
		 * }
		 */

		if (rowHead.random != 0)
		{
			double prob = Math.random();
			if (prob <= rowHead.random)
			{
				priority = -1;
			}
		}

		if (primHead.fanOut != 0)
		{
			Iterator<Object> it = rowHead.outList.iterator();
			ALS.IO ioPtr = (ALS.IO)it.next();
			ALS.Stat statHead = (ALS.Stat)ioPtr.nodePtr;
			time *= statHead.nodePtr.load;
		}
		time += als.timeAbs;

		for(Object obj : rowHead.outList)
		{
			ALS.IO ioHead = (ALS.IO)obj;
			ALS.Stat statHead = (ALS.Stat)ioHead.nodePtr;
			if (statHead.schedOp == ioHead.operatr &&
				statHead.schedState.equals(ioHead.operand) &&
				statHead.schedStrength == ioHead.strength)
			{
				continue;
			}

			ALS.Link linkPtr2 = new ALS.Link();
			linkPtr2.type = 'G';
			linkPtr2.ptr = statHead;
			linkPtr2.operatr = statHead.schedOp = ioHead.operatr;
			linkPtr2.state = statHead.schedState = ioHead.operand;
			linkPtr2.strength = statHead.schedStrength = ioHead.strength;
			linkPtr2.time = time;
			linkPtr2.priority = priority;
			linkPtr2.primHead = primHead;
			if (tracing)
			{
				System.out.println("      Schedule(G): " + statHead.primPtr.name + statHead.primPtr.level +
					" at " + TextUtils.convertToEngineeringNotation(time));
			}
			insertLinkList(linkPtr2);
		}
	}

	/**
	 * Method to insert a data element into a linklist that is 2
	 * dimensionally sorted first by time and then priority.  This link list is
	 * used to schedule events for the simulation.
	 *
	 * Calling Arguments:
	 *	linkHead = pointer to the data element that is going to be inserted
	 */
	void insertLinkList(ALS.Link linkHead)
	{
		// linkPtr1Is: 0: ALS.linkBack  1: linkPtr2.up  2: linkPtr2.left
		int linkPtr1Is = 0;
		ALS.Link linkPtr2 = als.linkBack;
		ALS.Link linkPtr2Val = linkPtr2;
		ALS.Link linkPtr3 = null;
		for(;;)
		{
			if (linkPtr2 == null)
			{
				als.linkFront = linkHead;
				switch (linkPtr1Is)
				{
					case 0: als.linkBack = linkHead;      break;
					case 1: linkPtr2Val.up = linkHead;    break;
					case 2: linkPtr2Val.left = linkHead;  break;
				}
				linkHead.left = null;
				linkHead.right = linkPtr3;
				linkHead.up = linkHead;
				linkHead.down = null;
				return;
			}

			if (linkPtr2.time < linkHead.time)
			{
				linkPtr2.right = linkHead;
				switch (linkPtr1Is)
				{
					case 0: als.linkBack = linkHead;      break;
					case 1: linkPtr2Val.up = linkHead;    break;
					case 2: linkPtr2Val.left = linkHead;  break;
				}
				linkHead.left = linkPtr2;
				linkHead.right = linkPtr3;
				linkHead.up = linkHead;
				linkHead.down = null;
				return;
			}

			if (linkPtr2.time == linkHead.time)
			{
				if (linkPtr2.priority > linkHead.priority)
				{
					linkHead.left = linkPtr2.left;
					linkHead.right = linkPtr2.right;
					linkHead.down = linkPtr2;
					linkHead.up = linkPtr2.up;
					linkPtr2.up = linkHead;
					switch (linkPtr1Is)
					{
						case 0: als.linkBack = linkHead;      break;
						case 1: linkPtr2Val.up = linkHead;    break;
						case 2: linkPtr2Val.left = linkHead;  break;
					}
					if (linkHead.left != null)
					{
						linkHead.left.right = linkHead;
					} else
					{
						als.linkFront = linkHead;
					}
					return;
				}

				linkPtr1Is = 1;
				linkPtr2Val = linkPtr2;
				linkPtr2 = linkPtr2.up;
				linkPtr3 = null;
				for(;;)
				{
					if (linkPtr2.priority <= linkHead.priority)
					{
						linkPtr2.down = linkHead;
						switch (linkPtr1Is)
						{
							case 0: als.linkBack = linkHead;      break;
							case 1: linkPtr2Val.up = linkHead;    break;
							case 2: linkPtr2Val.left = linkHead;  break;
						}
						linkHead.up = linkPtr2;
						linkHead.down = linkPtr3;
						return;
					}

					linkPtr3 = linkPtr2;
					linkPtr1Is = 1;
					linkPtr2Val = linkPtr2;
					linkPtr2 = linkPtr2.up;
				}
			}

			linkPtr3 = linkPtr2;
			linkPtr1Is = 2;
			linkPtr2Val = linkPtr2;
			linkPtr2 = linkPtr2.left;
		}
	}
}
