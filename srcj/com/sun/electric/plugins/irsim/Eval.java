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

import com.sun.electric.database.geometry.GenMath;

import java.util.Iterator;
import java.util.List;

public class Eval
{
	int    irsim_model_num;		/* index of model */

	static Eval   irsim_model;
	static int    irsim_treport = 0;			/* report format */

	static	boolean	firstcall = true;	    /* reset when calling init_vdd_gnd */
	
	static final int REPORT_DECAY	= 0x01;

	/* Routine added to initialize globals */
	static void irsim_init_eval()
	{
		firstcall = true;
		irsim_model = NewRStep.linearModel;
		irsim_treport = 0;
	}

	void modelEvaluate(Sim.Node node) {}

	/*
	 * find transistors with gates of VDD or GND and calculate values for source
	 * and drain nodes just in case event driven calculations don't get to them.
	 */
	static void init_vdd_gnd()
	{
		Sched.irsim_enqueue_input(Sim.irsim_VDD_node, Sim.HIGH);
		Sched.irsim_enqueue_input(Sim.irsim_GND_node, Sim.LOW);
	
		firstcall = false;		// initialization now taken care of
	}
	
	/*
	 * Reset the firstcall flag.  Usually after reading history dump or net state
	 */
	static void irsim_NoInit()
	{
		firstcall = false;
	}
	
	
	/*
	 * Set the firstcall flags.  Used when moving back to time 0.
	 */
	static void irsim_ReInit()
	{
		firstcall = true;
	}
	
	/*
	 * Print decay event.
	 */
	static void pr_decay(Sim.Event e)
	{
		Sim.Node n = e.enode;
		System.out.println(" @ " + Sim.d2ns(e.ntime) + "ns " + n.nname+ ": decay " +
			Sim.irsim_vchars.charAt(n.npot) + " . X");
	}
	
	
	/*
	 * Print watched node event.
	 */
	static void pr_watched(Sim.Event e, Sim.Node n)
	{
		if ((n.nflags & Sim.INPUT) != 0)
		{
			System.out.println(" @ " + Sim.d2ns(e.ntime) + "ns input " + n.nname + ": . " + Sim.irsim_vchars.charAt(e.eval));
			return;
		}
	
		int tmp = irsim_treport;
	
		String buf = null;
		switch(tmp & (Sim.REPORT_TAU | Sim.REPORT_DELAY))
		{
			case Sim.REPORT_TAU:
				buf = " (tau=" + Sim.d2ns(e.rtime) + "ns)";
				break;
			case Sim.REPORT_DELAY:
				buf = " (delay=" + Sim.d2ns(e.delay) + "ns)";
				break;
			default:
				buf = " (tau=" + Sim.d2ns(e.rtime) + "ns, delay=" + Sim.d2ns(e.delay) + "ns)";
		}
		System.out.println(" @ " + Sim.d2ns(e.ntime) + "ns " + n.nname + ": " +
			Sim.irsim_vchars.charAt(n.npot) + " . " + Sim.irsim_vchars.charAt(e.eval) + buf);
	}

	/*
	 * Run through the event list, marking all nodes that need to be evaluated.
	 */
	static void MarkNodes(Sim.Event evlist)
	{
		Sim.Event e = evlist;
		long all_flags = 0;
		do
		{
			Sim.Node n = e.enode;
	
			all_flags |= n.nflags;
	
			if (e.type == Sim.DECAY_EV && ((irsim_treport & REPORT_DECAY) != 0 ||
				(n.nflags & (Sim.WATCHED | Sim.STOPONCHANGE)) != 0))
					pr_decay(e);
			else if ((n.nflags & (Sim.WATCHED | Sim.STOPONCHANGE)) != 0)
				pr_watched(e, n);
	
			n.npot = e.eval;
	
			// Add the new value to the history list (if they differ)
			if ((n.nflags & Sim.INPUT) == 0 && ((short)n.curr.val != n.npot))
				Hist.irsim_AddHist(n, n.npot, false, e.ntime, (long) e.delay, (long) e.rtime);
	
			if (n.awpending != null  && n.awpot == n.npot) 
				RSim.irsim_evalAssertWhen(n);
	
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
			Sched.free_from_node(e, n);    // remove to avoid punting this event
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

	/* compute state of transistor.  If gate is a simple node, state is determined
	 * by type of implant and value of node.  If gate is a list of nodes, then
	 * this transistor represents a stack of transistors in the original network,
	 * and we perform the logical AND of all the gate node values to see if
	 * transistor is on.
	 */
	static int compute_trans_state(Sim.Trans t)
	{
	    if ((t.ttype & Sim.GATELIST) != 0) return irsim_ComputeTransState(t);
		return Sim.irsim_switch_state[Sim.BASETYPE(t.ttype)][((Sim.Node)t.gate).npot];
	}

	
	static long EvalNodes(Sim.Event evlist)
	{
		long brk_flag = 0;
		Sim.Event  event = evlist;
	
		do
		{
			Sched.irsim_nevent += 1;		// advance counter to that of this event
			Sim.Node n = Sched.irsim_cur_node = event.enode;
			n.setTime(event.ntime);	// set up the cause stuff
			n.setCause(event.cause);
	
			Sched.irsim_npending -= 1;
	
			/*
			 * now calculate new value for each marked node.  Some nodes marked
			 * above may become unmarked by earlier calculations before we get
			 * to them in this loop...
			 */
			for(Sim.Tlist l = n.ngate; l != null; l = l.next)
			{
				Sim.Trans t = l.xtor;
				if ((t.source.nflags & Sim.VISITED) != 0)
					irsim_model.modelEvaluate(t.source);
				if ((t.drain.nflags & Sim.VISITED) != 0)
					irsim_model.modelEvaluate(t.drain);
			}
	
			if ((n.nflags & (Sim.INPUT | Sim.POWER_RAIL)) == Sim.INPUT)
			{
				for(Sim.Tlist l = n.nterm; l != null; l = l.next)
				{	
					Sim.Trans t = l.xtor;
					Sim.Node other = Sim.other_node(t, n);
					if ((other.nflags & Sim.VISITED) != 0)
						irsim_model.modelEvaluate(other);
				}
			}
	
			// see if we want to halt if this node changes value
			brk_flag |= n.nflags;
	
			event = (Sim.Event)event.flink;
		}
		while(event != null);
	
		return(brk_flag);
	}
	
	
	/*
	 * Change the state of the nodes in the given input list to their new value,
	 * setting their INPUT flag and enqueueing the event.
	 */
	static void SetInputs(List list, int val)
	{
		for(Iterator it = list.iterator(); it.hasNext(); )
		{
			Sim.Node n = (Sim.Node)it.next();

			n.npot = (short)val;
			n.nflags &= ~Sim.INPUT_MASK;
			n.nflags |= Sim.INPUT;
	
			// enqueue event so consequences are computed.
			Sched.irsim_enqueue_input(n, val);
	
			if ((int)n.curr.val != val || ! (n.curr.inp))
				Hist.irsim_AddHist(n, val, true, Sched.irsim_cur_delta, 0L, 0L);
		}
	}
	
	
	static void MarkNOinputs()
	{	
		for(Iterator it = RSim.irsim_xinputs.iterator(); it.hasNext(); )
		{
			Sim.Node n = (Sim.Node)it.next();
			n.nflags &= ~(Sim.INPUT_MASK | Sim.INPUT);
			n.nflags |= Sim.VISITED;
		}
	}
	
	
		/* nodes which are no longer inputs */
	static void EvalNOinputs()
	{
		for(Iterator it = RSim.irsim_xinputs.iterator(); it.hasNext(); )
		{
			Sim.Node n = (Sim.Node)it.next();
			Sched.irsim_cur_node = n;
			Hist.irsim_AddHist(n, (int) n.curr.val, false, Sched.irsim_cur_delta, 0L, 0L);
			if ((n.nflags & Sim.VISITED) != 0)
				irsim_model.modelEvaluate(n);
		}
		RSim.irsim_xinputs.clear();
	}
	
	
	static boolean irsim_step(long stop_time)
	{
		boolean    ret_code = false;

		// look through input lists updating any nodes which just become inputs
		MarkNOinputs();			// nodes no longer inputs
		SetInputs(RSim.irsim_hinputs, Sim.HIGH);		// HIGH inputs
		RSim.irsim_hinputs.clear();
		SetInputs(RSim.irsim_linputs, Sim.LOW);		// LOW inputs
		RSim.irsim_linputs.clear();
		SetInputs(RSim.irsim_uinputs, Sim.X);		// X inputs
		RSim.irsim_uinputs.clear();
	
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
				Sim.Event evlist = Sched.irsim_get_next_event(stop_time);
				if (evlist == null) break;
				MarkNodes(evlist);
				if (RSim.irsim_xinputs.size() > 0) EvalNOinputs();
		
				long brk_flag = EvalNodes(evlist);
		
				Sched.FreeEventList(evlist);	// return event list to free pool

//				if (stopping(STOPREASONSIMULATE))
//				{
//					if (RSim.irsim_analyzerON)
//						Analyzer.irsim_UpdateWindow(Sched.irsim_cur_delta);
//					return ret_code;
//				}
				if ((brk_flag & (Sim.WATCHVECTOR | Sim.STOPONCHANGE | Sim.STOPVECCHANGE)) != 0)
				{
					if ((brk_flag & (Sim.WATCHVECTOR | Sim.STOPVECCHANGE)) != 0)
						RSim.irsim_disp_watch_vec(brk_flag);
					if ((brk_flag & (Sim.STOPONCHANGE | Sim.STOPVECCHANGE)) != 0)
					{
						if (RSim.irsim_analyzerON)
							Analyzer.irsim_UpdateWindow(Sched.irsim_cur_delta);
						return true;
					}
				}
			}
		
			if (RSim.irsim_xinputs.size() > 0)
			{
				EvalNOinputs();
				continue;
			}
			break;
		}
	
		Sched.irsim_cur_delta = stop_time;
		if (RSim.irsim_analyzerON)
			Analyzer.irsim_UpdateWindow(Sched.irsim_cur_delta);
		return ret_code;
	}
	
	
	static int irsim_ComputeTransState(Sim.Trans t)
	{
		switch(Sim.BASETYPE(t.ttype))
		{
			case Sim.NCHAN:
				int result = Sim.ON;
				for(Sim.Trans l = (Sim.Trans) t.gate; l != null; l = l.getSTrans())
				{
					Sim.Node n = (Sim.Node)l.gate;
					if (n.npot == Sim.LOW)
						return(Sim.OFF);
					else if (n.npot == Sim.X)
						result = Sim.UNKNOWN;
				}
				return(result);
	
			case Sim.PCHAN:
				result = Sim.ON;
				for(Sim.Trans l = (Sim.Trans) t.gate; l != null; l = l.getSTrans())
				{
					Sim.Node n = (Sim.Node)l.gate;
					if (n.npot == Sim.HIGH)
						return(Sim.OFF);
					else if (n.npot == Sim.X)
						result = Sim.UNKNOWN;
				}
				return(result);
	
			case Sim.DEP:
			case Sim.RESIST:
				return(Sim.WEAK);
	
			default:
		}
		System.out.println("**** internal error: unrecongized transistor type (" + Sim.BASETYPE(t.ttype) + ")");
		return(Sim.UNKNOWN);
	}
}
