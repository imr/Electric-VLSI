/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Eval.java
 * IRSIM simulator
 * Translated by Steven M. Rubin, Sun Microsystems.
 *
 * Copyright (C) 1988, 1990 Stanford University.
 * Permission to use, copy, modify, and distribute this
 * software and its documentation for any purpose and without
 * fee is hereby granted, provided that the above copyright
 * notice appear in all copies.  Stanford University
 * makes no representations about the suitability of this
 * software for any purpose.  It is provided "as is" without
 * express or implied warranty.
 */

package com.sun.electric.plugins.irsim;

import java.util.Iterator;
import java.util.List;

public class Eval
{
	private boolean	firstcall;	    /* reset when calling init_vdd_gnd */
	protected Analyzer theAnalyzer;
	protected Sim theSim;

	public Eval(Analyzer analyzer, Sim sim)
	{
		firstcall = true;
		theAnalyzer = analyzer;
		theSim = sim;
	}

	protected void modelEvaluate(Sim.Node node) {}

	/**
	 * find transistors with gates of VDD or GND and calculate values for source
	 * and drain nodes just in case event driven calculations don't get to them.
	 */
	private void init_vdd_gnd()
	{
		irsim_enqueue_input(theSim.irsim_VDD_node, Sim.HIGH);
		irsim_enqueue_input(theSim.irsim_GND_node, Sim.LOW);

		firstcall = false;		// initialization now taken care of
	}

	/**
	 * Set the firstcall flags.  Used when moving back to time 0.
	 */
	public void irsim_ReInit()
	{
		firstcall = true;
	}

	public boolean irsim_step(long stop_time)
	{
		boolean    ret_code = false;

		// look through input lists updating any nodes which just become inputs
		MarkNOinputs();			// nodes no longer inputs
		SetInputs(theAnalyzer.irsim_hinputs, Sim.HIGH);		// HIGH inputs
		theAnalyzer.irsim_hinputs.clear();
		SetInputs(theAnalyzer.irsim_linputs, Sim.LOW);		// LOW inputs
		theAnalyzer.irsim_linputs.clear();
		SetInputs(theAnalyzer.irsim_uinputs, Sim.X);		// X inputs
		theAnalyzer.irsim_uinputs.clear();

		/*
		 * On the first call to step, make sure transistors with gates
		 * of vdd and gnd are set up correctly.  Mark initial inputs first!
		 */
		if (firstcall)
			init_vdd_gnd();

		for(;;)
		{
			// process events until we reach specified stop time or events run out.
			for(;;)
			{
				Sim.Event evlist = irsim_get_next_event(stop_time);
				if (evlist == null) break;
				MarkNodes(evlist);
				if (theAnalyzer.irsim_xinputs.size() > 0) EvalNOinputs();

				long brk_flag = EvalNodes(evlist);

//				if (stopping(STOPREASONSIMULATE))
//				{
//					if (RSim.irsim_analyzerON)
//						Analyzer.irsim_UpdateWindow(Sched.irsim_cur_delta);
//					return ret_code;
//				}
				if ((brk_flag & (Sim.WATCHVECTOR | Sim.STOPONCHANGE | Sim.STOPVECCHANGE)) != 0)
				{
					if ((brk_flag & (Sim.WATCHVECTOR | Sim.STOPVECCHANGE)) != 0)
						theAnalyzer.irsim_disp_watch_vec(brk_flag);
					if ((brk_flag & (Sim.STOPONCHANGE | Sim.STOPVECCHANGE)) != 0)
					{
						if (theAnalyzer.irsim_analyzerON)
							theAnalyzer.irsim_UpdateWindow(theSim.irsim_cur_delta);
						return true;
					}
				}
			}

			if (theAnalyzer.irsim_xinputs.size() > 0)
			{
				EvalNOinputs();
				continue;
			}
			break;
		}

		theSim.irsim_cur_delta = stop_time;
		if (theAnalyzer.irsim_analyzerON)
			theAnalyzer.irsim_UpdateWindow(theSim.irsim_cur_delta);
		return ret_code;
	}

	/**
	 * compute state of transistor.  If gate is a simple node, state is determined
	 * by type of implant and value of node.  If gate is a list of nodes, then
	 * this transistor represents a stack of transistors in the original network,
	 * and we perform the logical AND of all the gate node values to see if
	 * transistor is on.
	 */
	public int compute_trans_state(Sim.Trans t)
	{
	    if ((t.ttype & Sim.GATELIST) != 0) return irsim_ComputeTransState(t);
		return Sim.irsim_switch_state[Sim.BASETYPE(t.ttype)][((Sim.Node)t.gate).npot];
	}

	/**
	 * Print decay event.
	 */
	private void pr_decay(Sim.Event e)
	{
		Sim.Node n = e.enode;
		System.out.println(" @ " + Sim.d2ns(e.ntime) + "ns " + n.nname+ ": decay " +
			Sim.irsim_vchars.charAt(n.npot) + " . X");
	}

	/**
	 * Print watched node event.
	 */
	private void pr_watched(Sim.Event e, Sim.Node n)
	{
		if ((n.nflags & Sim.INPUT) != 0)
		{
			System.out.println(" @ " + Sim.d2ns(e.ntime) + "ns input " + n.nname + ": . " + Sim.irsim_vchars.charAt(e.eval));
			return;
		}
		String buf = " (tau=" + Sim.d2ns(e.rtime) + "ns, delay=" + Sim.d2ns(e.delay) + "ns)";
		System.out.println(" @ " + Sim.d2ns(e.ntime) + "ns " + n.nname + ": " +
			Sim.irsim_vchars.charAt(n.npot) + " . " + Sim.irsim_vchars.charAt(e.eval) + buf);
	}

	/**
	 * Run through the event list, marking all nodes that need to be evaluated.
	 */
	private void MarkNodes(Sim.Event evlist)
	{
		Sim.Event e = evlist;
		long all_flags = 0;
		do
		{
			Sim.Node n = e.enode;

			all_flags |= n.nflags;

			if ((n.nflags & (Sim.WATCHED | Sim.STOPONCHANGE)) != 0)
				pr_decay(e);
			else if ((n.nflags & (Sim.WATCHED | Sim.STOPONCHANGE)) != 0)
				pr_watched(e, n);

			n.npot = e.eval;

			// Add the new value to the history list (if they differ)
			if ((n.nflags & Sim.INPUT) == 0 && ((short)n.curr.val != n.npot))
				theSim.irsim_AddHist(n, n.npot, false, e.ntime, (long) e.delay, (long) e.rtime);

			if (n.awpending != null  && n.awpot == n.npot)
				theAnalyzer.irsim_evalAssertWhen(n);

			/* for each transistor controlled by event node, mark
			 * source and drain nodes as needing recomputation.
			 *
			 * Added MOSSIMs speed up by first checking if the
			 * node needs to be rechecked  mh
			 *
			 * Fixed it so nodes with pending events also get
			 * re_evaluated. Kevin Karplus
			 */
			for(Sim.Tlist l = n.ngate; l != null; l = l.next)
			{
				Sim.Trans t = l.xtor;
				t.state = (byte)compute_trans_state(t);
				if ((t.source.nflags & Sim.INPUT) == 0)
					t.source.nflags |= Sim.VISITED;
				if ((t.drain.nflags & Sim.INPUT) == 0)
					t.drain.nflags |= Sim.VISITED;
			}
			free_from_node(e, n);    // remove to avoid punting this event
			e = (Sim.Event)e.flink;
		}
		while(e != null);

		// run thorugh event list again, marking src/drn of input nodes
		if ((all_flags & Sim.INPUT) != 0)
		{
			for(e = evlist; e != null; e = (Sim.Event)e.flink)
			{
				Sim.Node n = e.enode;

				if ((n.nflags & (Sim.INPUT | Sim.POWER_RAIL)) != Sim.INPUT)
					continue;

				for(Sim.Tlist l = n.nterm; l != null; l = l.next)
				{
					Sim.Trans t = l.xtor;
					if (t.state != Sim.OFF)
					{
						Sim.Node other = Sim.other_node(t, n);
						if ((other.nflags & (Sim.INPUT | Sim.VISITED)) == 0)
							other.nflags |= Sim.VISITED;
					}
				}
			}
		}
	}

	private long EvalNodes(Sim.Event evlist)
	{
		long brk_flag = 0;
		Sim.Event  event = evlist;

		do
		{
			theSim.irsim_nevent += 1;		// advance counter to that of this event
			Sim.Node n = theSim.irsim_cur_node = event.enode;
			n.setTime(event.ntime);	// set up the cause stuff
			n.setCause(event.cause);

			theSim.irsim_npending -= 1;

			/*
			 * now calculate new value for each marked node.  Some nodes marked
			 * above may become unmarked by earlier calculations before we get
			 * to them in this loop...
			 */
			for(Sim.Tlist l = n.ngate; l != null; l = l.next)
			{
				Sim.Trans t = l.xtor;
				if ((t.source.nflags & Sim.VISITED) != 0)
					modelEvaluate(t.source);
				if ((t.drain.nflags & Sim.VISITED) != 0)
					modelEvaluate(t.drain);
			}

			if ((n.nflags & (Sim.INPUT | Sim.POWER_RAIL)) == Sim.INPUT)
			{
				for(Sim.Tlist l = n.nterm; l != null; l = l.next)
				{
					Sim.Trans t = l.xtor;
					Sim.Node other = Sim.other_node(t, n);
					if ((other.nflags & Sim.VISITED) != 0)
						modelEvaluate(other);
				}
			}

			// see if we want to halt if this node changes value
			brk_flag |= n.nflags;

			event = (Sim.Event)event.flink;
		}
		while(event != null);

		return brk_flag;
	}

	/**
	 * Change the state of the nodes in the given input list to their new value,
	 * setting their INPUT flag and enqueueing the event.
	 */
	private void SetInputs(List list, int val)
	{
		for(Iterator it = list.iterator(); it.hasNext(); )
		{
			Sim.Node n = (Sim.Node)it.next();

			n.npot = (short)val;
			n.nflags &= ~Sim.INPUT_MASK;
			n.nflags |= Sim.INPUT;

			// enqueue event so consequences are computed.
			irsim_enqueue_input(n, val);

			if ((int)n.curr.val != val || ! (n.curr.inp))
				theSim.irsim_AddHist(n, val, true, theSim.irsim_cur_delta, 0L, 0L);
		}
	}

	private void MarkNOinputs()
	{
		for(Iterator it = theAnalyzer.irsim_xinputs.iterator(); it.hasNext(); )
		{
			Sim.Node n = (Sim.Node)it.next();
			n.nflags &= ~(Sim.INPUT_MASK | Sim.INPUT);
			n.nflags |= Sim.VISITED;
		}
	}

	/**
	 * nodes which are no longer inputs
	 */
	private void EvalNOinputs()
	{
		for(Iterator it = theAnalyzer.irsim_xinputs.iterator(); it.hasNext(); )
		{
			Sim.Node n = (Sim.Node)it.next();
			theSim.irsim_cur_node = n;
			theSim.irsim_AddHist(n, (int) n.curr.val, false, theSim.irsim_cur_delta, 0L, 0L);
			if ((n.nflags & Sim.VISITED) != 0)
				modelEvaluate(n);
		}
		theAnalyzer.irsim_xinputs.clear();
	}

	private int irsim_ComputeTransState(Sim.Trans t)
	{
		switch(Sim.BASETYPE(t.ttype))
		{
			case Sim.NCHAN:
				int result = Sim.ON;
				for(Sim.Trans l = (Sim.Trans) t.gate; l != null; l = l.getSTrans())
				{
					Sim.Node n = (Sim.Node)l.gate;
					if (n.npot == Sim.LOW)
						return Sim.OFF;
					else if (n.npot == Sim.X)
						result = Sim.UNKNOWN;
				}
				return result;

			case Sim.PCHAN:
				result = Sim.ON;
				for(Sim.Trans l = (Sim.Trans) t.gate; l != null; l = l.getSTrans())
				{
					Sim.Node n = (Sim.Node)l.gate;
					if (n.npot == Sim.HIGH)
						return Sim.OFF;
					else if (n.npot == Sim.X)
						result = Sim.UNKNOWN;
				}
				return result;

			case Sim.DEP:
			case Sim.RESIST:
				return Sim.WEAK;

			default:
		}
		System.out.println("**** internal error: unrecongized transistor type (" + Sim.BASETYPE(t.ttype) + ")");
		return Sim.UNKNOWN;
	}

	/***************************************** SCHED *****************************************/

	private static final int TSIZE		= 1024;	/* size of event array, must be power of 2 */
	private static final int TMASK		= (TSIZE - 1);

	private Sim.Event [] ev_array = new Sim.Event[TSIZE];	    /* used as head of doubly-linked lists */

	private Sim.Event EV_LIST(long t) { return ev_array[(int)(t & TMASK)]; }

	/**
	 * find the next event to be processed by scanning event wheel.  Return
	 * the list of events to be processed at this time, removing it first
	 * from the time wheel.
	 */
	private Sim.Event irsim_get_next_event(long stop_time)
	{
		if (theSim.irsim_npending == 0) return null;

		Sim.Event event = null;
		boolean eventValid = false;
		long time = theSim.irsim_max_time;
		for(long i = theSim.irsim_cur_delta, limit = i + TSIZE; i < limit; i++)
		{
			event = EV_LIST(i);
			if (event != event.flink)
			{
				if (event.flink.ntime < limit)		// common case
				{
					eventValid = true;
					break;
				}
				if (event.flink.ntime < time)
					time = (event.flink).ntime;
			}
		}
		if (!eventValid)
		{
			if (time == (long)theSim.irsim_max_time)
			{
				System.out.println("*** internal error: no events but npending set");
				return null;
			}

			event = EV_LIST(time);
		}

		Sim.Event evlist = event.flink;

		time = evlist.ntime;

		if (time >= stop_time) return null;

		theSim.irsim_cur_delta = time;			// advance simulation time

		if (event.blink.ntime != time)	// check tail of list
		{
			do
				event = event.flink;
			while(event.ntime == time);

			event = event.blink;		// grab part of the list
			evlist.blink.flink = event.flink;
			event.flink.blink = evlist.blink;
			evlist.blink = event;
			event.flink = null;
		} else
		{
			event = evlist.blink;		// grab the entire list
			event.blink.flink = null;
			evlist.blink = event.blink;
			event.flink = event.blink = event;
		}
		return evlist;
	}

	/**
	 * remove event from all structures it belongs to and return it to free pool
	 */
	private void irsim_free_event(Sim.Event event)
	{
		// unhook from doubly-linked event list
		event.blink.flink = event.flink;
		event.flink.blink = event.blink;
		theSim.irsim_npending -= 1;

		free_from_node(event, event.enode);
	}

	/**
	 * Add an event to event list, specifying transition delay and new value.
	 * 0 delay transitions are converted into unit delay transitions (0.01 ns).
	 */
	public void irsim_enqueue_event(Sim.Node n, int newvalue, long delta, long rtime)
	{
		Sim.Event newev = new Sim.Event();

		// remember facts about this event
		long etime = theSim.irsim_cur_delta + (long)delta;
		newev.ntime = etime;
		newev.rtime = (short)rtime;
		newev.enode = n;
		newev.cause = theSim.irsim_cur_node;
		newev.delay = delta;
		if (newvalue == Sim.DECAY)		// change value to X here
		{
			newev.eval = Sim.X;
			newev.type = Sim.DECAY_EV;
		} else
		{
			newev.eval = (byte)newvalue;
			newev.type = Sim.REVAL;		// for incremental simulation
		}

		/* add the new event to the event list at the appropriate entry
		 * in event wheel.  Event lists are kept sorted by increasing
		 * event time.
		 */
		Sim.Event marker = EV_LIST(etime);

		// Check whether we need to insert-sort in the list
		if ((marker.blink != marker) && (((Sim.Event)marker.blink).ntime > etime))
		{
			do { marker = (Sim.Event)marker.flink; } while (marker.ntime <= etime);
		}

		// insert event right before event pointed to by marker
		newev.flink = marker;
		newev.blink = marker.blink;
		marker.blink.flink = newev;
		marker.blink = newev;
		theSim.irsim_npending += 1;

		/*
		 * thread event onto list of events for this node, keeping it
		 * in sorted order
		 */
		if ((n.events != null) && (n.events.ntime > etime))
		{
			for(marker = n.events; (marker.nlink != null) &&
				(marker.nlink.ntime > etime); marker = marker.nlink);
			newev.nlink = marker.nlink;
			marker.nlink = newev;
		} else
		{
			newev.nlink = n.events;
			n.events = newev;
		}
	}

	/**
	 * same as irsim_enqueue_event, but assumes 0 delay and rtise/fall time
	 */
	private void irsim_enqueue_input(Sim.Node n, int newvalue)
	{
		// Punt any pending events for this node.
		while(n.events != null)
			irsim_free_event(n.events);

		Sim.Event newev = new Sim.Event();

		// remember facts about this event
		long etime = theSim.irsim_cur_delta;
		newev.ntime = etime;
		newev.rtime = 0;   newev.delay = 0;
		newev.enode = n;
		newev.cause = n;
		newev.eval = (byte)newvalue;
		newev.type = Sim.REVAL;			// anything, doesn't matter

		// Add new event to HEAD of list at appropriate entry in event wheel

		Sim.Event marker = EV_LIST(etime);
		newev.flink = marker.flink;
		newev.blink = marker;
		marker.flink.blink = newev;
		marker.flink = newev;
		theSim.irsim_npending += 1;

		// thread event onto (now empty) list of events for this node
		newev.nlink = null;
		n.events = newev;
	}

	/**
	 * Initialize event structures
	 */
	public void irsim_init_event()
	{
		for(int i = 0; i < TSIZE; i++)
		{
			Sim.Event event = new Sim.Event();
			ev_array[i] = event;
			event.flink = event.blink = event;
		}
		theSim.irsim_npending = 0;
		theSim.irsim_nevent = 0;
	}

	protected void irsim_PuntEvent(Sim.Node node, Sim.Event ev)
	{
		if ((node.nflags & Sim.WATCHED) != 0)
			System.out.println("    punting transition of " + node.nname + " . " +
				Sim.irsim_vchars.charAt(ev.eval) + " scheduled for " + Sim.d2ns(ev.ntime) + "ns");

		if (ev.type != Sim.DECAY_EV)		// don't save punted decay events
			theSim.irsim_AddPunted(ev.enode, ev, theSim.irsim_cur_delta);
		irsim_free_event(ev);
	}

	private void irsim_requeue_events(Sim.Event evlist, boolean thread)
	{
		theSim.irsim_npending = 0;
		Sim.Event next = null;
		for(Sim.Event ev = evlist; ev != null; ev = next)
		{
			next = (Sim.Event)ev.flink;

			theSim.irsim_npending++;
			long etime = ev.ntime;
			Sim.Event target = EV_LIST(etime);

			if ((target.blink != target) && (((Sim.Event)target.blink).ntime > etime))
			{
				do { target = (Sim.Event)target.flink; } while(target.ntime <= etime);
			}

			ev.flink = target;
			ev.blink = target.blink;
			target.blink.flink = ev;
			target.blink = ev;

			if (thread)
			{
				Sim.Node   n = ev.enode;
				Sim.Event  marker;

				if ((n.events != null) && (n.events.ntime > etime))
				{
					for(marker = n.events; (marker.nlink != null) &&
						(marker.nlink.ntime > etime); marker = marker.nlink);
					ev.nlink = marker.nlink;
					marker.nlink = ev;
				} else
				{
					ev.nlink = n.events;
					n.events = ev;
				}
			}
		}
	}

		// Incremental simulation routines

	/**
	 * Back the event queues up to time 'btime'.  This is the opposite of
	 * advancing the simulation time.  Mark all pending events as PENDING,
	 * and re-enqueue them according to their creation-time (ntime - delay).
	 */
	public Sim.Event irsim_back_sim_time(long btime, int is_inc)
	{
		int nevents = 0;
		Sim.Event tmplist = null;

		// first empty out the time wheel onto the temporary list
		for(int i=0; i<TSIZE; i++)
		{
			Sim.Event hdr = ev_array[i];
			Sim.Event next = null;
			for(Sim.Event evhdr = hdr.flink; evhdr != hdr; evhdr = next)
			{
				next = evhdr.flink;
				if (!(evhdr instanceof Sim.Event)) continue;
				Sim.Event ev = (Sim.Event)evhdr;

				ev.blink.flink = ev.flink;	// remove event
				ev.flink.blink = ev.blink;
				if (is_inc != 0)
					free_from_node(ev, ev.enode);

				if (is_inc == 0 && ev.ntime - ev.delay >= btime)
				{
					free_from_node(ev, ev.enode);
				} else
				{
					ev.flink = tmplist;		// move it to tmp list
					tmplist = ev;

					nevents++;
				}
			}
		}

		if (is_inc == 0)
		{
			irsim_requeue_events(tmplist, false);
			return null;
		}

		if (is_inc != 1)	// only for fault simulation (is_inc == 2)
		{
			theSim.irsim_npending = 0;
			return tmplist;
		}

		// now move the temporary list to the time wheel
		Sim.Event next = null;
		for(Sim.Event ev = tmplist; ev != null; ev = next)
		{
			long   etime;
			Sim.Event  target;

			next = (Sim.Event)ev.flink;

			ev.ntime -= (long)ev.delay;
			ev.type = Sim.PENDING;
			etime = ev.ntime;
			target = EV_LIST(etime);

			if ((target.blink != target) && (((Sim.Event)target.blink).ntime > etime))
			{
				do { target = (Sim.Event)target.flink; } while(target.ntime <= etime);
			}

			ev.flink = target;
			ev.blink = target.blink;
			target.blink.flink = ev;
			target.blink = ev;
		}

		theSim.irsim_npending = nevents;
		return null;
	}

	private void free_from_node(Sim.Event ev, Sim.Node nd)
	{
		if (nd.events == ev)
			nd.events = ev.nlink;
		else
		{
			Sim.Event evp = null;
			for(evp = nd.events; evp.nlink != ev; evp = evp.nlink);
			evp.nlink = ev.nlink;
		}
	}

}
