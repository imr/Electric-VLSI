/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Sched.java
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
import com.sun.electric.plugins.irsim.Sim.Event;
import com.sun.electric.plugins.irsim.Sim.Node;

public class Sched
{	
	static final int TSIZE		= 1024;	/* size of event array, must be power of 2 */
	static final int TMASK		= (TSIZE - 1);
	
	static long   irsim_cur_delta;	    /* current simulated time */
	static Sim.Node   irsim_cur_node;	    /* node that belongs to current event */
	static long   irsim_nevent;		    /* number of current event */
	
//	Sim.Event  irsim_evfree = null;	    /* list of free event structures */
	static int    irsim_npending;	    /* number of pending events */
	
	
	static	Sim.Event  [] ev_array = new Sim.Event[TSIZE];	    /* used as head of doubly-linked lists */
	static Sim.Event EV_LIST(long t) { return ev_array[(int)(t & TMASK)]; }
	
	static void irsim_init_sched()
	{
//		irsim_evfree = null;
	}
	
	/*
	 * find the next event to be processed by scanning event wheel.  Return
	 * the list of events to be processed at this time, removing it first
	 * from the time wheel.
	 */
	static Sim.Event irsim_get_next_event(long stop_time)
	{
		if (irsim_npending == 0) return null;
	
		Sim.Event event = null;
		boolean eventValid = false;
		long time = Hist.irsim_max_time;
		for(long i = irsim_cur_delta, limit = i + TSIZE; i < limit; i++)
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
			if (time == (long)Hist.irsim_max_time)
			{
				System.out.println("*** internal error: no events but npending set");
				return null;
			}
	
			event = EV_LIST(time);
		}

		Sim.Event evlist = event.flink;

		time = evlist.ntime;

		if (time >= stop_time) return null;

		irsim_cur_delta = time;			// advance simulation time

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
	
	
	/*
	 * remove event from all structures it belongs to and return it to free pool
	 */
	static void irsim_free_event(Sim.Event event)
	{
		// unhook from doubly-linked event list
		event.blink.flink = event.flink;
		event.flink.blink = event.blink;
		irsim_npending -= 1;

		// add to free storage pool
//		event.flink = irsim_evfree;
//		irsim_evfree = event;
	
		free_from_node(event, event.enode);
	}

	static void FreeEventList(Sim.Event l)
	{
//		l.blink.flink = irsim_evfree;
//		irsim_evfree = l;
	}

	
	/*
	 * Add an event to event list, specifying transition delay and new value.
	 * 0 delay transitions are converted into unit delay transitions (0.01 ns).
	 */
	static void irsim_enqueue_event(Sim.Node n, int newvalue, long delta, long rtime)
	{
		Sim.Event newev = new Sim.Event();
	
		// remember facts about this event
		long etime = irsim_cur_delta + (long)delta;
		newev.ntime = etime;
		newev.rtime = (short)rtime;
		newev.enode = n;
		newev.cause = irsim_cur_node;
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
		irsim_npending += 1;

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
	
	
	/* same as irsim_enqueue_event, but assumes 0 delay and rtise/fall time */
	static void irsim_enqueue_input(Sim.Node n, int newvalue)
	{
		// Punt any pending events for this node.
		while(n.events != null)
			irsim_free_event(n.events);
	
		Sim.Event newev = new Sim.Event();
	
		// remember facts about this event
		long etime = irsim_cur_delta;
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
		irsim_npending += 1;

		// thread event onto (now empty) list of events for this node
		newev.nlink = null;
		n.events = newev;
	}
	
	
	/*
	 * Initialize event structures
	 */
	static void irsim_init_event()
	{
		for(int i = 0; i < TSIZE; i++)
		{
			Sim.Event event = new Sim.Event();
			ev_array[i] = event;
			event.flink = event.blink = event;
		}
		irsim_npending = 0;
		irsim_nevent = 0;
	}
	
	
	static void irsim_PuntEvent(Sim.Node node, Sim.Event ev)
	{
		if ((node.nflags & Sim.WATCHED) != 0)
			System.out.println("    punting transition of " + node.nname + " . " +
				Sim.irsim_vchars.charAt(ev.eval) + " scheduled for " + Sim.d2ns(ev.ntime) + "ns");
	
		if (ev.type != Sim.DECAY_EV)		// don't save punted decay events
			Hist.irsim_AddPunted(ev.enode, ev, irsim_cur_delta);
		irsim_free_event(ev);
	}
	
	
	static void irsim_requeue_events(Sim.Event evlist, boolean thread)
	{
		irsim_npending = 0;
		Sim.Event next = null;
		for(Sim.Event ev = evlist; ev != null; ev = next)
		{
			next = (Sim.Event)ev.flink;
	
			irsim_npending++;
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
	
	/*
	 * Back the event queues up to time 'btime'.  This is the opposite of
	 * advancing the simulation time.  Mark all pending events as PENDING,
	 * and re-enqueue them according to their creation-time (ntime - delay).
	 */
	static Sim.Event irsim_back_sim_time(long btime, int is_inc)
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
//					ev.flink = irsim_evfree;
//					irsim_evfree = ev;
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
			irsim_npending = 0;
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
	
		irsim_npending = nevents;
		return null;
	}
	
	
	/*
	 * Enqueue event type 'type' form history entry 'hist' for node 'nd'.
	 * Note that events with type > THREAD are not threaded onto a node's list
	 * of pending events, since we don't want the new_val routines to look at
	 * this event, instead we keep track of these events in the c.event event
	 * of every node.  Return FALSE if this history entry is the sentinel
	 * (Hist.irsim_last_hist), otherwise return TRUE.
	 */
	static boolean irsim_EnqueueHist(Sim.Node nd, Sim.HistEnt hist, int type)
	{
		if (hist == Hist.irsim_last_hist)			// never queue this up
		{
			nd.setEvent(null);
			return false;
		}
	
		Sim.Event newev = new Sim.Event();
	
		// remember facts about this event
		long etime = hist.htime;
		newev.ntime = etime;
		newev.eval = hist.val;
		newev.enode = nd;
		newev.delay = hist.delay;
		newev.rtime = hist.rtime;
	
		Sim.Event marker = EV_LIST(etime);
	
		// Check whether we need to insert-sort in the list
		if ((marker.blink != marker) && (((Sim.Event)marker.blink).ntime > etime))
		{
			do { marker = (Sim.Event)marker.flink; } while(marker.ntime <= etime);
		}
	
		// insert event right before event pointed to by marker
		newev.flink = marker;
		newev.blink = marker.blink;
		marker.blink.flink = newev;
		marker.blink = newev;
		irsim_npending += 1;
		if (hist.inp)
			type |= Sim.IS_INPUT;
		else if (newev.delay == 0)
			type |= Sim.IS_XINPUT;
		newev.type = (byte)type;
	
		if (type > Sim.THREAD)
		{
			nd.setEvent(newev);
			return true;
		}
	
		if ((nd.events != null) && (nd.events.ntime > etime))
		{
			for(marker = nd.events; (marker.nlink != null) &&
				(marker.nlink.ntime > etime); marker = marker.nlink);
			newev.nlink = marker.nlink;
			marker.nlink = newev;
		} else
		{
			newev.nlink = nd.events;
			nd.events = newev;
		}
		return true;
	}
	
	
	static void irsim_DequeueEvent(Sim.Node nd)
	{
		Sim.Event ev = nd.getEvent();
		ev.blink.flink = ev.flink;
		ev.flink.blink = ev.blink;
//		ev.flink = irsim_evfree;
//		irsim_evfree = ev;
		nd.setEvent(null);
	
		irsim_npending -= 1;
	}
	
	
	static void irsim_DelayEvent(Sim.Event ev, long delay)
	{
		Sim.Node nd = ev.enode;
		Sim.Event newev = new Sim.Event();
	
		// remember facts about this event
		newev.nlink = ev.nlink;
		newev.enode = ev.enode;
		newev.cause = ev.cause;
		newev.ntime = ev.ntime;
		newev.delay = ev.delay;
		newev.rtime = ev.rtime;
		newev.eval = ev.eval;
		newev.type = ev.type;
		
		newev.delay += (long)delay;
		newev.ntime += delay;
	
		long etime = newev.ntime;
	
		Sim.Event marker = EV_LIST(etime);
	
		// Check whether we need to insert-sort in the list
		if ((marker.blink != marker) && (((Sim.Event)marker.blink).ntime > etime))
		{
			do { marker = (Sim.Event)marker.flink; } while(marker.ntime <= etime);
		}
	
		// insert event right before event pointed to by marker
		newev.flink = marker;
		newev.blink = marker.blink;
		marker.blink.flink = newev;
		marker.blink = newev;
		irsim_npending += 1;
		if (newev.type > Sim.THREAD)
		{
			nd.setEvent(newev);
			return;
		}
	
		if ((nd.events != null) && (nd.events.ntime > etime))
		{
			for(marker = nd.events; (marker.nlink != null) &&
				(marker.nlink.ntime > etime); marker = marker.nlink);
			newev.nlink = marker.nlink;
			marker.nlink = newev;
		} else
		{
			newev.nlink = nd.events;
			nd.events = newev;
		}
	}
	
	
	static Sim.Event irsim_EnqueueOther(int type, long time)
	{
		Sim.Event newev = new Sim.Event();

		long etime = time;
		newev.ntime = etime;
		newev.type = (byte)type;
	
		Sim.Event marker = EV_LIST(etime);
	
		// Check whether we need to insert-sort in the list
		if ((marker.blink != marker) && (((Sim.Event)marker.blink).ntime > etime))
		{
			do { marker = (Sim.Event)marker.flink; } while(marker.ntime <= etime);
		}
	
		// insert event right before event pointed to by marker
		newev.flink = marker;
		newev.blink = marker.blink;
		marker.blink.flink = newev;
		marker.blink = newev;
		irsim_npending += 1;
		return newev;
	}

	static void free_from_node(Sim.Event ev, Sim.Node nd)
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
