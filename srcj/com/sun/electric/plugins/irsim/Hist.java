/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Hist.java
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

public class Hist
{	
//	Sim.HistEnt   irsim_freeHist = null;	/* list of free history entries */
	static Sim.HistEnt   irsim_last_hist;			/* pointer to dummy hist-entry that serves as tail for all nodes */
	static int    irsim_num_edges = 0;
	static int    irsim_num_punted = 0;
	static int    irsim_num_cons_punted = 0;
	
	static Sim.HistEnt   irsim_first_model;		/* first model entry */
	static long irsim_max_time;
	
	static	Sim.HistEnt   curr_model;		/* ptr. to current model entry */
	
	
	static void irsim_init_hist()
	{
		irsim_max_time = Sim.MAX_TIME;
	
		Sim.HistEnt dummy = new Sim.HistEnt();
		irsim_last_hist = dummy;
		dummy.next = irsim_last_hist;
		dummy.htime = irsim_max_time;
		dummy.val = Sim.X;
		dummy.inp = true;
		dummy.punt = false;
		dummy.delay = dummy.rtime = 0;

		Sim.HistEnt dummy_model = new Sim.HistEnt();
		dummy_model.htime = 0;
		dummy_model.val = (byte)Eval.irsim_model.irsim_model_num;
		dummy_model.inp = false;
		dummy_model.punt = false;
		dummy_model.next = null;
		irsim_first_model = curr_model = dummy_model;
	
		// initialize globals
//		irsim_freeHist = null;
		irsim_num_edges = 0;
		irsim_num_punted = 0;
		irsim_num_cons_punted = 0;
	}
	
	
	/*
	 * time at which history entry was enqueued
	 */

	static long QTIME(Sim.HistEnt h) { return h.htime - h.delay; }

	static long PuntTime(Sim.HistEnt h) { return h.htime - h.ptime; }

	
	static void irsim_SetFirstHist(Sim.Node node, int value, boolean inp, long time)
	{
		node.head.htime = time;
		node.head.val = (byte)value;
		node.head.inp = inp;
		node.head.rtime = node.head.delay = 0;
	}

	/*
	 * Add a new entry to the history list.  Update curr to point to this change.
	 */
	static void irsim_AddHist(Sim.Node node, int value, boolean inp, long time, long delay, long rtime)
	{
		irsim_num_edges++;
		Sim.HistEnt curr = node.curr;
	
		if ((node.nflags & Sim.HIST_OFF) != 0)
		{
			// Old entries are deleted. Keep only last entry for delay calculation
			irsim_FreeHistList(node);
			curr = node.curr;
		}
	
		while(curr.next.punt)		// skip past any punted events
			curr = curr.next;
	
		Sim.HistEnt newh = new Sim.HistEnt();
		if (newh == null) return;
	
		newh.next = curr.next;
		newh.htime = time;
		newh.val = (byte)value;
		newh.inp = inp;
		newh.punt = false;
		newh.delay = (short)delay;
		newh.rtime = (short)rtime;
		node.curr = curr.next = newh;
	}

	/*
	 * Add a punted event to the history list for the node.  Consecutive punted
	 * events are kept in punted-order, so that h.ptime < h.next.ptime.
	 * Adding a punted event does not change the current pointer, which always
	 * points to the last "effective" node change.
	 */
	static void irsim_AddPunted(Sim.Node node, Sim.Event ev, long tim)
	{
		Sim.HistEnt h = node.curr;
	
		irsim_num_punted++;
		if ((node.nflags & Sim.HIST_OFF) != 0)
			return;
	
		Sim.HistEnt newp = new Sim.HistEnt();
	
		newp.htime = ev.ntime;
		newp.val = ev.eval;
		newp.inp = false;
		newp.punt = true;
		newp.delay = (short)ev.delay;
		newp.rtime = ev.rtime;
		newp.ptime = (short)(newp.htime - tim);
	
		if (h.next.punt)		// there are some punted events already
		{
			irsim_num_cons_punted++;
			do { h = h.next; } while(h.next.punt);
		}
	
		newp.next = h.next;
		h.next = newp;
	}
	
	
	/*
	 * Free up a node's history list
	 */
	static void irsim_FreeHistList(Sim.Node node)
	{
		Sim.HistEnt h = node.head.next;
		if (h == irsim_last_hist)		// nothing to do
			return;

		Sim.HistEnt next;
		while((next = h.next) != irsim_last_hist)		// find last entry
			h = next;
	
//		h.next = irsim_freeHist;				// link list to free list
//		irsim_freeHist = node.head.next;
	
		node.head.next = irsim_last_hist;
		node.curr = node.head;
	}
	
	
	
	/*
	 * Add a new model entry, recording the time of the change.
	 */
	static void irsim_NewModel(int nmodel)
	{
		if ((long)curr_model.htime != Sched.irsim_cur_delta)
		{
			Sim.HistEnt newh;
	
			newh = new Sim.HistEnt();
	
			newh.next = null;
			newh.htime = Sched.irsim_cur_delta;
			newh.val = (byte)nmodel;
			curr_model.next = newh;
			curr_model = newh;
		}
		else
			curr_model.val = (byte)nmodel;
	}
	
	/*
	 * Add a new change to the history list of node 'nd' caused by event 'ev'.
	 * Skip past any punted events already in the history and update nd.curr to
	 * point to the new change.
	 */
	static void irsim_NewEdge(Sim.Node nd, Sim.Event ev)
	{
		Sim.HistEnt p = null, h = null;
		for(p = nd.curr, h = p.next; h.punt; p = h, h = h.next);
	
		Sim.HistEnt newh = null;
		newh = new Sim.HistEnt();
	
		newh.htime = ev.ntime;
		newh.val = ev.eval;
		newh.inp = false;		// always true in incremental simulation
		newh.punt = false;
		newh.delay = (short)ev.delay;
		newh.rtime = ev.rtime;
	
		p.next = newh;
		newh.next = h;
	
		nd.curr = newh;		// newh becomes the current value
	}

	/*
	 * Delete the next effective change (following nd.curr) from the history.
	 * Punted events before the next change (in nd.t.punts) can now be freed.
	 * Punted events following the deleted edge are moved to nd.t.punts.
	 */
	static void irsim_DeleteNextEdge(Sim.Node nd)
	{
		Sim.HistEnt a = nd.getPunts();
		if (a != null)	// remove previously punted events
		{
			Sim.HistEnt b = null;
			for(b = a; b.next != null; b = b.next);
//			b.next = irsim_freeHist;
//			irsim_freeHist = a;
		}
	
		a = nd.curr;
		Sim.HistEnt b = null;
		Sim.HistEnt c = null;
		for(b = a.next; b.punt; a = b, b = b.next);
		for(c = b.next; c.punt; b = c, c = c.next);
		c = a.next;			// c => next edge
		a.next = b.next;
		a = c.next;			// a => first punted event after c
	
//		c.next = irsim_freeHist;			// free the next effective change
//		irsim_freeHist = c;
	
		if (a.punt)			// move punted events from hist
		{
			nd.setPunts(a);
			b.next = null;
		}
		else
			nd.setPunts(null);
	}
	
	static Sim.HistEnt NEXTH(Sim.HistEnt p)
	{
		Sim.HistEnt h;
		for(h = p.next; h.punt; h = h.next) ;
		return h;
	}

	static void irsim_FlushHist(long ftime)
	{	
		for(Iterator it = Sim.Node.irsim_GetNodeList().iterator(); it.hasNext(); )
		{
			Sim.Node n = (Sim.Node)it.next();
			Sim.HistEnt head = n.head;
			if (head.next == irsim_last_hist || (n.nflags & Sim.ALIAS) != 0)
				continue;
			Sim.HistEnt p = head;
			Sim.HistEnt h = NEXTH(p);
			while((long)h.htime < ftime)
			{
				p = h;
				h = NEXTH(h);
			}
			head.val = p.val;
			head.htime = p.htime;
			head.inp = p.inp;
			while (p.next != h)
				p = p.next;
			if (head.next != h)
			{
//				p.next = irsim_freeHist;
//				irsim_freeHist = head.next;
				head.next = h;
			}
			if ((long)n.curr.htime < ftime)
			{
				n.curr = head;
			}
		}
	}
	
	
	static int irsim_backToTime(Sim.Node nd)
	{
		if ((nd.nflags & (Sim.ALIAS | Sim.MERGED)) != 0)
			return(0);
	
		Sim.HistEnt h = nd.head;
		Sim.HistEnt p = NEXTH(h);
		while((long)p.htime < Sched.irsim_cur_delta)
		{
			h = p;
			p = NEXTH(p);
		}
		nd.curr = h;
	
		// queue pending events
		for(p = h, h = p.next; ; p = h, h = h.next)
		{
			long  qtime;
	
			if (h.punt)
			{
				if (PuntTime(h) < Sched.irsim_cur_delta)	// already punted, skip it
					continue;
	
				qtime = (long)h.htime - (long)h.delay;	// pending, enqueue it
				if (qtime < Sched.irsim_cur_delta)
				{
					long tmp = Sched.irsim_cur_delta;
					Sched.irsim_cur_delta = qtime;
					Sched.irsim_enqueue_event(nd, (int) h.val, (long) h.delay, (long) h.rtime);
					Sched.irsim_cur_delta = tmp;
				}
				p.next = h.next;
//				h.next = irsim_freeHist;
//				irsim_freeHist = h;
				h = p;
			} else
			{
				qtime = QTIME(h);
				if (qtime < Sched.irsim_cur_delta)		// pending, enqueue it
				{
					long tmp = Sched.irsim_cur_delta;
					Sched.irsim_cur_delta = qtime;
					Sched.irsim_enqueue_event(nd, (int) h.val, (long) h.delay, (long) h.rtime);
					Sched.irsim_cur_delta = tmp;
	
					p.next = h.next;		// and free it
//					h.next = irsim_freeHist;
//					irsim_freeHist = h;
					h = p;
				}
				else
					break;
			}
		}
	
		p.next = irsim_last_hist;
		p = h;
		// p now points to the 1st event in the future (to be deleted)
		if (p != irsim_last_hist)
		{
			while(h.next != irsim_last_hist)
				h = h.next;
//			h.next = irsim_freeHist;
//			irsim_freeHist = p;
		}
	
		h = nd.curr;
		nd.npot = h.val;
		nd.setTime(h.htime);
		if (h.inp)
			nd.nflags |= Sim.INPUT;
	
		if (nd.ngate != null)		// recompute transistor states
		{
			for(Sim.Tlist l = nd.ngate; l != null; l = l.next)
			{
				Sim.Trans t = l.xtor;
				t.state = (byte)Eval.compute_trans_state(t);
			}
		}
		return(0);
	}
}
