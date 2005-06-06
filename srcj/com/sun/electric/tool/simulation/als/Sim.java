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
import com.sun.electric.tool.simulation.Simulation;
import com.sun.electric.tool.simulation.Stimuli;
import com.sun.electric.tool.simulation.als.ALS.Load;
import com.sun.electric.tool.user.ui.WaveformWindow;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class Sim
{
	private ALS als;

	boolean simals_tracing = false;
	private Load       simals_chekroot;

	private static String [] simals_statedesc = {"High", "Undefined", "Low"};
	private static String [] simals_strengthdesc = {"Off-", "Weak-", "Weak-", "", "", "Strong-", "Strong-"};

	private HashMap tracking;

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
	double simals_initialize_simulator(boolean force)
	{
		als.simals_time_abs = 0.0;
		tracking = new HashMap();

		while (als.simals_linkfront != null)
		{
			ALS.Link link = als.simals_linkfront;
			if (als.simals_linkfront.down != null)
			{
				als.simals_linkfront.down.right = als.simals_linkfront.right;
				als.simals_linkfront = als.simals_linkfront.down;
			} else
			{
				als.simals_linkfront = als.simals_linkfront.right;
			}
		}
		als.simals_linkback = null;

		for (ALS.Link linkhead = als.simals_setroot; linkhead != null; linkhead = linkhead.right)
		{
			ALS.Link linkptr2 = new ALS.Link();
			if (linkptr2 == null) return(als.simals_time_abs);
			linkptr2.type = linkhead.type;
			linkptr2.ptr = linkhead.ptr;
			linkptr2.state = linkhead.state;
			linkptr2.strength = linkhead.strength;
			linkptr2.priority = linkhead.priority;
			linkptr2.time = linkhead.time;
			linkptr2.primhead = null;
			simals_insert_link_list(linkptr2);
		}

		for (ALS.Node nodehead = als.simals_noderoot; nodehead != null; nodehead = nodehead.next)
		{
			nodehead.sum_state = Stimuli.LOGIC_LOW;
			nodehead.sum_strength = Stimuli.OFF_STRENGTH;
			nodehead.new_state = new Integer(Stimuli.LOGIC_LOW);
			nodehead.new_strength = Stimuli.OFF_STRENGTH;
			nodehead.maxsize = 0;
			nodehead.arrive = 0;
			nodehead.depart = 0;
			nodehead.tk_sec = 0;
			nodehead.t_last = 0.0;
			for (ALS.Stat stathead = nodehead.statptr; stathead != null; stathead = stathead.next)
			{
				stathead.new_state = Stimuli.LOGIC_LOW;
				stathead.new_strength = Stimuli.OFF_STRENGTH;
				stathead.sched_op = 0;
			}
		}

		// now run the simulation
		boolean update = Simulation.isALSResimulateEach();
		if (force) update = true;
		if (update)
		{
			// fire events until end of time or quiesced
			System.out.print("Simulating...");

			// determine highest time to simulate
			Rectangle2D bounds = als.sd.getBounds();
			double tMax = bounds.getMaxX();
			for(Iterator it = als.ww.getPanels(); it.hasNext(); )
			{
				WaveformWindow.Panel wp = (WaveformWindow.Panel)it.next();
				double panelMax = wp.getMaxTimeRange();
				if (panelMax > tMax) tMax = panelMax;
			}

			while (als.simals_linkfront != null && als.simals_linkfront.time <= tMax)
			{
				if (simals_fire_event()) break;
				if (simals_chekroot != null)
				{
					if (simals_schedule_new_events()) break;
				}
			}

			// redisplay results
			simals_fill_display_arrays();
			System.out.println("Done.  Ran to time " + TextUtils.convertToEngineeringNotation(als.simals_time_abs));
		}

		return als.simals_time_abs;
	}

	/**
	 * Method to extract the ALS simulation data and update the Stimuli database
	 */
	private void simals_fill_display_arrays()
	{
		for(Iterator it = tracking.keySet().iterator(); it.hasNext(); )
		{
			ALS.Node node = (ALS.Node)it.next();
			DigitalSignal sig = node.sig;
			List trakHeads = (List)tracking.get(node);
			int count = trakHeads.size();

			double [] timeVector = new double[count+1];
			int [] stateVector = new int[count+1];
			timeVector[0] = 0;
			stateVector[0] = Stimuli.LOGIC_LOW | Stimuli.OFF_STRENGTH;
			int j=1;
			for(Iterator dIt = trakHeads.iterator(); dIt.hasNext(); )
			{
				ALS.Trak trakhead = (ALS.Trak)dIt.next();
				timeVector[j] = trakhead.time;
				stateVector[j] = trakhead.state;
				j++;
			}
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
	boolean simals_fire_event()
	{
		als.simals_time_abs = als.simals_linkfront.time;
		ALS.Link linkhead = als.simals_linkfront;
		if (als.simals_linkfront.down != null)
		{
			als.simals_linkfront = als.simals_linkfront.down;
			als.simals_linkfront.left = null;
			als.simals_linkfront.right = linkhead.right;
			als.simals_linkfront.up = linkhead.up;
			if (als.simals_linkfront.right != null)
			{
				als.simals_linkfront.right.left = als.simals_linkfront;
			} else
			{
				als.simals_linkback = als.simals_linkfront;
			}
		} else
		{
			als.simals_linkfront = als.simals_linkfront.right;
			if (als.simals_linkfront != null)
			{
				als.simals_linkfront.left = null;
			} else
			{
				als.simals_linkback = null;
			}
		}

		simals_tracing = false;
		switch (linkhead.type)
		{
			case 'G':
				ALS.Stat stathead = (ALS.Stat)linkhead.ptr;
				if (stathead.nodeptr.tracenode)
				{
					String s2 = als.simals_compute_node_name(stathead.nodeptr);
					System.out.println(TextUtils.convertToEngineeringNotation(als.simals_time_abs) +
						": Firing gate " + stathead.primptr.name + stathead.primptr.level + ", net " + s2);
					simals_tracing = true;
				}
				if (stathead.sched_state != linkhead.state ||
					stathead.sched_op != linkhead.operatr ||
					stathead.sched_strength != linkhead.strength)
				{
					break;
				}
				stathead.sched_op = 0;

				char operatr = linkhead.operatr;
				int operand = 0;
				if (operatr < 128)
				{
					operand = ((Integer)linkhead.state).intValue();
				} else
				{
					operatr -= 128;
//					ALS.Node nodehead = (ALS.Node)linkhead.state;
//					operand = nodehead.sum_state;
				}

				int state = 0;
				switch (operatr)
				{
					case '=':
						state = operand;
						break;
					case '+':
						state = stathead.nodeptr.sum_state + operand;
						break;
					case '-':
						state = stathead.nodeptr.sum_state - operand;
						break;
					case '*':
						state = stathead.nodeptr.sum_state * operand;
						break;
					case '/':
						state = stathead.nodeptr.sum_state / operand;
						break;
					case '%':
						state = stathead.nodeptr.sum_state % operand;
						break;
					default:
						System.out.println("Invalid arithmetic operator: " + operatr);
						return true;
				}

				if (state == stathead.new_state &&
					linkhead.strength == stathead.new_strength)
				{
					break;
				}
				stathead.new_state = state;
				stathead.new_strength = linkhead.strength;
				simals_create_check_list(stathead.nodeptr, linkhead);
				break;

			case 'N':
				ALS.Node nodehead = (ALS.Node)linkhead.ptr;
				if (nodehead.tracenode)
				{
					String s2 = als.simals_compute_node_name(nodehead);
					System.out.println(TextUtils.convertToEngineeringNotation(als.simals_time_abs) + ": Changed state of net " + s2);
					simals_tracing = true;
				}
				if (linkhead.state == nodehead.new_state &&
					linkhead.strength == nodehead.new_strength)
						break;

				nodehead.new_state = linkhead.state;
				nodehead.new_strength = linkhead.strength;
				simals_create_check_list(nodehead, linkhead);
				break;

			case 'C':
				double time = als.simals_time_abs;
				ALS.Row rowhead = (ALS.Row)linkhead.ptr;
				for(Iterator it = rowhead.inList.iterator(); it.hasNext(); )
				{
					ALS.Link vecthead = (ALS.Link)it.next();
					ALS.Link linkptr2 = new ALS.Link();
					linkptr2.type = 'N';
					linkptr2.ptr = vecthead.ptr;
					linkptr2.state = vecthead.state;
					linkptr2.strength = vecthead.strength;
					linkptr2.priority = vecthead.priority;
					linkptr2.time = time;
					linkptr2.primhead = null;
					simals_insert_link_list(linkptr2);
					time += vecthead.time;
				}
				if (((Integer)linkhead.state).intValue() == 0)
				{
					simals_calculate_clock_time(linkhead, rowhead);
					return false;
				}
				linkhead.state = new Integer(((Integer)linkhead.state).intValue() - 1);
				if (((Integer)linkhead.state).intValue() != 0)
				{
					simals_calculate_clock_time(linkhead, rowhead);
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
	void simals_create_check_list(ALS.Node nodehead, ALS.Link linkhead)
	{
		// get initial state of the node
		int state = ((Integer)nodehead.new_state).intValue();
		int strength = nodehead.new_strength;

		// print state of signal if this signal is being traced
		if (simals_tracing)
		{
			System.out.println("  Formerly " + simals_strengthdesc[nodehead.sum_strength] + simals_statedesc[nodehead.sum_state+3] +
				", starts at " + simals_strengthdesc[strength] + simals_statedesc[state+3]);
		}

		// look at all factors affecting the node
		for (ALS.Stat stathead = nodehead.statptr; stathead != null; stathead = stathead.next)
		{
			int thisstate = stathead.new_state;
			int thisstrength = stathead.new_strength;
			if (simals_tracing)
				System.out.println("    " + simals_strengthdesc[thisstrength] + simals_statedesc[thisstate+3] +
					" from " + stathead.primptr.name + stathead.primptr.level);

			// higher strength overrides previous node state
			if (thisstrength > strength)
			{
				state = thisstate;
				strength = thisstrength;
				continue;
			}

			// same strength: must arbitrate
			if (thisstrength == strength)
			{
				if (thisstate != state)
					state = Stimuli.LOGIC_X;
			}
		}

		// if the node has nothing driving it, set it to the old value
		if (strength == Stimuli.OFF_STRENGTH)
		{
			state = nodehead.sum_state;
			strength = Stimuli.NODE_STRENGTH;
		}

		// stop now if node state did not change
		if (nodehead.sum_state == state && nodehead.sum_strength == strength)
		{
			if (simals_tracing) System.out.println("    NO CHANGE");
			return;
		}

		if (nodehead.sig != null)
		{
			List nodeData = (List)tracking.get(nodehead);
			if (nodeData == null)
			{
				nodeData = new ArrayList();
				tracking.put(nodehead, nodeData);
			}

			ALS.Trak trakhead = new ALS.Trak();
			trakhead.state = state | strength;
			trakhead.time = als.simals_time_abs;
			nodeData.add(trakhead);
		}
		if (simals_tracing)
			System.out.println("    BECOMES " + simals_strengthdesc[strength] + simals_statedesc[state+3]);

		nodehead.sum_state = state;
		nodehead.sum_strength = strength;
		nodehead.t_last = als.simals_time_abs;

		als.simals_drive_node = nodehead;
		simals_chekroot = nodehead.pinptr;
	}

	/**
	 * Method to examine the truth tables for the transitions that are
	 * specified in the checking list.  If there is a match between a truth table
	 * entry and the state of the logic network the transition is scheduled
	 * for firing.  Returns true on error.
	 */
	boolean simals_schedule_new_events()
	{
		for (ALS.Load chekhead = simals_chekroot; chekhead != null; chekhead = chekhead.next)
		{
			ALS.Model primhead = (ALS.Model)chekhead.ptr;

			if (primhead.type == 'F')
			{
				ALS.Func funchead = (ALS.Func)primhead.ptr;
				funchead.procptr.simulate(primhead);
				continue;
			}

			for (ALS.Row rowhead = (ALS.Row)primhead.ptr; rowhead != null; rowhead = rowhead.next)
			{
				int flag = 1;
				for(Iterator it = rowhead.inList.iterator(); it.hasNext(); )
				{
					ALS.IO iohead = (ALS.IO)it.next();
					int operatr = iohead.operatr;
					int operand;
					if (operatr < 128)
					{
						operand = ((Integer)iohead.operand).intValue();
					} else
					{
						operatr -= 128;
						ALS.Node nodehead = (ALS.Node)iohead.operand;
						operand = nodehead.sum_state;
					}

					switch (operatr)
					{
						case '=':
							if (((ALS.Node)iohead.nodeptr).sum_state != operand) flag = 0;
							break;
						case '!':
							if (((ALS.Node)iohead.nodeptr).sum_state == operand) flag = 0;
							break;
						case '<':
							if (((ALS.Node)iohead.nodeptr).sum_state >= operand) flag = 0;
							break;
						case '>':
							if (((ALS.Node)iohead.nodeptr).sum_state <= operand) flag = 0;
							break;
						default:
							System.out.println("Invalid logical operator: " + operatr);
							return true;
					}

					if (flag == 0) break;
				}

				if (flag != 0)
				{
					if (simals_calculate_event_time(primhead, rowhead)) return true;
					break;
				}
			}
		}
		simals_chekroot = null;
		return false;
	}

	/**
	 * Method to calculate the time when the next occurance of a set of
	 * clock vectors is to be added to the event scheduling linklist.
	 *
	 * Calling Arguments:
	 *	linkhead = pointer to the link element to be reinserted into the list
	 *  rowhead  = pointer to a row element containing timing information
	 */
	void simals_calculate_clock_time(ALS.Link linkhead, ALS.Row rowhead)
	{
		double time = als.simals_time_abs;

		if (rowhead.delta != 0) time += rowhead.delta;
		if (rowhead.linear != 0)
		{
			double prob = Math.random();
			time += 2.0 * prob * rowhead.linear;
		}

		/*
		 * if (rowhead.exp)
		 * {
		 * 	prob = rand() / MAXINTBIG;
		 * 	time += (-log(prob) * (rowhead.exp));
		 * }
		 */

		linkhead.time = time;
		simals_insert_link_list(linkhead);
	}

	/**
	 * Method to calculate the time of occurance of an event and then
	 * places an entry into the event scheduling linklist for later execution.
	 * Returns true on error.
	 *
	 * Calling Arguments:
	 *	primhead  = pointer to the primitive to be scheduled for firing
	 *	rowhead  = pointer to the row containing the event to be scheduled
	 */
	boolean simals_calculate_event_time(ALS.Model primhead, ALS.Row rowhead)
	{
		double time = 0.0;
		int priority = primhead.priority;

		if (rowhead.delta != 0) time += rowhead.delta;
		if (rowhead.abs != 0) time += rowhead.abs;
		if (rowhead.linear != 0)
		{
			double prob = Math.random();
			time += 2.0 * prob * rowhead.linear;
		}

		/*
		 * if (rowhead.exp)
		 * {
		 * 	prob = rand() / MAXINTBIG;
		 * 	time += (-log(prob) * (rowhead.exp));
		 * }
		 */

		if (rowhead.random != 0)
		{
			double prob = Math.random();
			if (prob <= rowhead.random)
			{
				priority = -1;
			}
		}

		if (primhead.fanout != 0)
		{
			Iterator it = rowhead.outList.iterator();
			ALS.IO ioPtr = (ALS.IO)it.next();
			ALS.Stat stathead = (ALS.Stat)ioPtr.nodeptr;
			time *= stathead.nodeptr.load;
		}
		time += als.simals_time_abs;

		for(Iterator it = rowhead.outList.iterator(); it.hasNext(); )
		{
			ALS.IO iohead = (ALS.IO)it.next();
			ALS.Stat stathead = (ALS.Stat)iohead.nodeptr;
			if (stathead.sched_op == iohead.operatr &&
				stathead.sched_state.equals(iohead.operand) &&
					stathead.sched_strength == iohead.strength)
			{
				continue;
			}

			ALS.Link linkptr2 = new ALS.Link();
			linkptr2.type = 'G';
			linkptr2.ptr = stathead;
			linkptr2.operatr = stathead.sched_op = iohead.operatr;
			linkptr2.state = stathead.sched_state = iohead.operand;
			linkptr2.strength = stathead.sched_strength = iohead.strength;
			linkptr2.time = time;
			linkptr2.priority = priority;
			linkptr2.primhead = primhead;
			if (simals_tracing)
			{
				System.out.println("      Schedule(G): " + stathead.primptr.name + stathead.primptr.level +
					" at " + TextUtils.convertToEngineeringNotation(time));
			}
			simals_insert_link_list(linkptr2);
		}
		return false;
	}

	/**
	 * Method to insert a data element into a linklist that is sorted
	 * by time and then priority.  This link list is used to schedule events
	 * for the simulation.
	 *
	 * Calling Arguments:
	 *	linkhead = pointer to the data element that is going to be inserted
	 */
	void simals_insert_set_list(ALS.Link linkhead)
	{
		// linkPtr1Is: 0: ALS.simals_setroot  1: linkptr1.right
		int linkPtr1Is = 0;
		ALS.Link linkptr1 = null;

		for(;;)
		{
			ALS.Link linkptr2 = als.simals_setroot;
			if (linkPtr1Is == 1) linkptr2 = linkptr1.right;
			if (linkptr2 == null)
			{
				if (linkPtr1Is == 0) als.simals_setroot = linkhead; else
					linkptr1.right = linkhead;
				break;
			}
			if (linkptr2.time > linkhead.time || (linkptr2.time == linkhead.time &&
				linkptr2.priority > linkhead.priority))
			{
				linkhead.right = linkptr2;
				if (linkPtr1Is == 0) als.simals_setroot = linkhead; else
					linkptr1.right = linkhead;
				break;
			}
			linkptr1 = linkptr2;
			linkPtr1Is = 1;
		}
	}

	/**
	 * Method to insert a data element into a linklist that is 2
	 * dimensionally sorted first by time and then priority.  This link list is
	 * used to schedule events for the simulation.
	 *
	 * Calling Arguments:
	 *	linkhead = pointer to the data element that is going to be inserted
	 */
	void simals_insert_link_list(ALS.Link linkhead)
	{
		// linkPtr1Is: 0: ALS.simals_linkback  1: linkptr2.up  2: linkptr2.left
		int linkPtr1Is = 0;
		ALS.Link linkptr2 = als.simals_linkback;
		ALS.Link linkPtr2Val = linkptr2;
		ALS.Link linkptr3 = null;
		for(;;)
		{
			if (linkptr2 == null)
			{
				als.simals_linkfront = linkhead;
				switch (linkPtr1Is)
				{
					case 0: als.simals_linkback = linkhead;   break;
					case 1: linkPtr2Val.up = linkhead;        break;
					case 2: linkPtr2Val.left = linkhead;      break;
				}
				linkhead.left = null;
				linkhead.right = linkptr3;
				linkhead.up = linkhead;
				linkhead.down = null;
				return;
			}

			if (linkptr2.time < linkhead.time)
			{
				linkptr2.right = linkhead;
				switch (linkPtr1Is)
				{
					case 0: als.simals_linkback = linkhead;   break;
					case 1: linkPtr2Val.up = linkhead;        break;
					case 2: linkPtr2Val.left = linkhead;      break;
				}
				linkhead.left = linkptr2;
				linkhead.right = linkptr3;
				linkhead.up = linkhead;
				linkhead.down = null;
				return;
			}

			if (linkptr2.time == linkhead.time)
			{
				if (linkptr2.priority > linkhead.priority)
				{
					linkhead.left = linkptr2.left;
					linkhead.right = linkptr2.right;
					linkhead.down = linkptr2;
					linkhead.up = linkptr2.up;
					linkptr2.up = linkhead;
					switch (linkPtr1Is)
					{
						case 0: als.simals_linkback = linkhead;   break;
						case 1: linkPtr2Val.up = linkhead;        break;
						case 2: linkPtr2Val.left = linkhead;      break;
					}
					if (linkhead.left != null)
					{
						linkhead.left.right = linkhead;
					} else
					{
						als.simals_linkfront = linkhead;
					}
					return;
				}

				linkPtr1Is = 1;
				linkPtr2Val = linkptr2;
				linkptr2 = linkptr2.up;
				linkptr3 = null;
				for(;;)
				{
					if (linkptr2.priority <= linkhead.priority)
					{
						linkptr2.down = linkhead;
						switch (linkPtr1Is)
						{
							case 0: als.simals_linkback = linkhead;   break;
							case 1: linkPtr2Val.up = linkhead;        break;
							case 2: linkPtr2Val.left = linkhead;      break;
						}
						linkhead.up = linkptr2;
						linkhead.down = linkptr3;
						return;
					}

					linkptr3 = linkptr2;
					linkPtr1Is = 1;
					linkPtr2Val = linkptr2;
					linkptr2 = linkptr2.up;
				}
			}

			linkptr3 = linkptr2;
			linkPtr1Is = 2;
			linkPtr2Val = linkptr2;
			linkptr2 = linkptr2.left;
		}
	}
}
