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
	private static final boolean DEBUG = false;

	private boolean	firstCall;	    /* reset when calling init_vdd_gnd */
	protected Analyzer theAnalyzer;
	protected Sim theSim;
	private  int      nPending;         /* number of pending events */

	public static class Event
	{
		/** pointers in doubly-linked list */			Event    fLink, bLink;
		/** link for list of events for this node */	Event    nLink;
		/** node this event is all about */				Sim.Node eNode;
														Sim.Node cause;
		/** time, in DELTAs, of this event */			long     nTime;
		/** delay associated with this event */			long     delay;
		/** rise/fall time, in DELTAs */				short    rTime;
		/** new value */								byte     eval;
		/** type of event */							byte     type;
	};

	/**
	 * table to convert transistor type and gate node value into switch state
	 * indexed by [transistor-type][gate-node-value].
	 */
	private static int [][] switchState = new int[][]
	{
		/** NCHAH */	new int[] {Sim.OFF,  Sim.UNKNOWN, Sim.UNKNOWN, Sim.ON},
		/** PCHAN */	new int[] {Sim.ON,   Sim.UNKNOWN, Sim.UNKNOWN, Sim.OFF},
		/** RESIST */	new int[] {Sim.WEAK, Sim.WEAK,    Sim.WEAK,    Sim.WEAK},
		/** DEP */		new int[] {Sim.WEAK, Sim.WEAK,    Sim.WEAK,    Sim.WEAK}
	};

	public Eval(Analyzer analyzer, Sim sim)
	{
		firstCall = true;
		theAnalyzer = analyzer;
		theSim = sim;
	}

	protected void modelEvaluate(Sim.Node node) {}

	/**
	 * Set the firstCall flags.  Used when moving back to time 0.
	 */
	public void reInit()
	{
		firstCall = true;
	}

	public boolean step(long stopTime)
	{
		boolean retCode = false;

		// look through input lists updating any nodes which just become inputs
		MarkNOinputs();			// nodes no longer inputs
		SetInputs(theAnalyzer.hInputs, Sim.HIGH);		// HIGH inputs
		theAnalyzer.hInputs.clear();
		SetInputs(theAnalyzer.lIinputs, Sim.LOW);		// LOW inputs
		theAnalyzer.lIinputs.clear();
		SetInputs(theAnalyzer.uInputs, Sim.X);			// X inputs
		theAnalyzer.uInputs.clear();

		/*
		 * On the first call to step, make sure transistors with gates
		 * of vdd and gnd are set up correctly.  Mark initial inputs first!
		 */
		if (firstCall)
		{
			/*
			 * find transistors with gates of VDD or GND and calculate values for source
			 * and drain nodes just in case event driven calculations don't get to them.
			 */
			enqueueInput(theSim.powerNode, Sim.HIGH);
			enqueueInput(theSim.groundNode, Sim.LOW);
			firstCall = false;
		}

		for(;;)
		{
			// process events until we reach specified stop time or events run out.
			for(;;)
			{
				Event evList = getNextEvent(stopTime);
				if (evList == null) break;
				MarkNodes(evList);
				if (theAnalyzer.xInputs.size() > 0) EvalNOinputs();

				long brkFlag = EvalNodes(evList);

//				if (stopping(STOPREASONSIMULATE))
//				{
//					if (RSim.analyzerON)
//						Analyzer.updateWindow(Sched.curDelta);
//					return retCode;
//				}
				if ((brkFlag & (Sim.WATCHVECTOR | Sim.STOPONCHANGE | Sim.STOPVECCHANGE)) != 0)
				{
					if ((brkFlag & (Sim.WATCHVECTOR | Sim.STOPVECCHANGE)) != 0)
						theAnalyzer.dispWatchVec(brkFlag);
					if ((brkFlag & (Sim.STOPONCHANGE | Sim.STOPVECCHANGE)) != 0)
					{
						if (theAnalyzer.analyzerON)
							theAnalyzer.updateWindow(theSim.curDelta);
						return true;
					}
				}
			}

			if (theAnalyzer.xInputs.size() > 0)
			{
				EvalNOinputs();
				continue;
			}
			break;
		}

		theSim.curDelta = stopTime;
		if (theAnalyzer.analyzerON)
			theAnalyzer.updateWindow(theSim.curDelta);
		return retCode;
	}

	/**
	 * Print watched node event.
	 */
	private void pr_watched(Event e, Sim.Node n)
	{
		if ((n.nFlags & Sim.INPUT) != 0)
		{
			System.out.println(" @ " + Sim.deltaToNS(e.nTime) + "ns input " + n.nName + ": . " + Sim.vChars.charAt(e.eval));
			return;
		}
		System.out.print(" @ " + Sim.deltaToNS(e.nTime) + "ns " + n.nName + ": " +
			Sim.vChars.charAt(n.nPot) + " . " + Sim.vChars.charAt(e.eval));

		int tmp = (theSim.irDebug & Sim.DEBUG_EV) != 0 ? (Sim.REPORT_TAU | Sim.REPORT_DELAY) : theSim.tReport;
		switch (tmp & (Sim.REPORT_TAU | Sim.REPORT_DELAY))
		{
			case Sim.REPORT_TAU:
				System.out.println(" (tau=" + Sim.deltaToNS(e.rTime));
				break;
			case Sim.REPORT_DELAY:
				System.out.println(" (delay=" + Sim.deltaToNS(e.delay) + "ns)");
				break;
			default:
				System.out.println(" (tau=" + Sim.deltaToNS(e.rTime) + "ns, delay=" + Sim.deltaToNS(e.delay) + "ns)");
				break;
		}
	}

	/**
	 * Run through the event list, marking all nodes that need to be evaluated.
	 */
	private void MarkNodes(Event evList)
	{
		Event e = evList;
		long allFlags = 0;
		do
		{
			Sim.Node n = e.eNode;

			allFlags |= n.nFlags;

			if (e.type == Sim.DECAY_EV && ((theSim.tReport & Sim.REPORT_DECAY) != 0 ||
				(n.nFlags & (Sim.WATCHED | Sim.STOPONCHANGE)) != 0))
			{
				System.out.println(" @ " + Sim.deltaToNS(e.nTime) + "ns " + n.nName+ ": decay " +
					Sim.vChars.charAt(n.nPot) + " . X");
			} else if ((n.nFlags & (Sim.WATCHED | Sim.STOPONCHANGE)) != 0)
				pr_watched(e, n);

			n.nPot = e.eval;

			// Add the new value to the history list (if they differ)
			if ((n.nFlags & Sim.INPUT) == 0 && ((short)n.curr.val != n.nPot))
				theSim.addHist(n, n.nPot, false, e.nTime, e.delay, e.rTime);

			if (n.awPending != null && n.awPot == n.nPot)
				theAnalyzer.evalAssertWhen(n);

			/* for each transistor controlled by event node, mark
			 * source and drain nodes as needing recomputation.
			 *
			 * Added MOSSIMs speed up by first checking if the
			 * node needs to be rechecked  mh
			 *
			 * Fixed it so nodes with pending events also get
			 * re_evaluated. Kevin Karplus
			 */
			for(Iterator it = n.nGateList.iterator(); it.hasNext(); )
			{
				Sim.Trans t = (Sim.Trans)it.next();
				t.state = (byte)computeTransState(t);
				if ((t.source.nFlags & Sim.INPUT) == 0)
					t.source.nFlags |= Sim.VISITED;
				if ((t.drain.nFlags & Sim.INPUT) == 0)
					t.drain.nFlags |= Sim.VISITED;
			}
			freeFromNode(e, n);    // remove to avoid punting this event
			e = (Event)e.fLink;
		}
		while(e != null);

		// run thorugh event list again, marking src/drn of input nodes
		if ((allFlags & Sim.INPUT) != 0)
		{
			for(e = evList; e != null; e = (Event)e.fLink)
			{
				Sim.Node n = e.eNode;

				if ((n.nFlags & (Sim.INPUT | Sim.POWER_RAIL)) != Sim.INPUT)
					continue;

				for(Iterator it = n.nTermList.iterator(); it.hasNext(); )
				{
					Sim.Trans t = (Sim.Trans)it.next();
					if (t.state != Sim.OFF)
					{
						Sim.Node other = Sim.otherNode(t, n);
						if ((other.nFlags & (Sim.INPUT | Sim.VISITED)) == 0)
							other.nFlags |= Sim.VISITED;
					}
				}
			}
		}
	}

	private long EvalNodes(Event evList)
	{
		long brkFlag = 0;
		Event event = evList;

		do
		{
			theSim.nEvent += 1;		// advance counter to that of this event
			Sim.Node n = theSim.curNode = event.eNode;
			n.setTime(event.nTime);	// set up the cause stuff
			n.setCause(event.cause);
if (DEBUG) System.out.println("Removing event at " + event.nTime + " in EvalNodes");
			nPending--;

			/*
			 * now calculate new value for each marked node.  Some nodes marked
			 * above may become unmarked by earlier calculations before we get
			 * to them in this loop...
			 */
			for(Iterator it = n.nGateList.iterator(); it.hasNext(); )
			{
				Sim.Trans t = (Sim.Trans)it.next();
				if ((t.source.nFlags & Sim.VISITED) != 0)
					modelEvaluate(t.source);
				if ((t.drain.nFlags & Sim.VISITED) != 0)
					modelEvaluate(t.drain);
			}

			if ((n.nFlags & (Sim.INPUT | Sim.POWER_RAIL)) == Sim.INPUT)
			{
				for(Iterator it = n.nTermList.iterator(); it.hasNext(); )
				{
					Sim.Trans t = (Sim.Trans)it.next();
					Sim.Node other = Sim.otherNode(t, n);
					if ((other.nFlags & Sim.VISITED) != 0)
						modelEvaluate(other);
				}
			}

			// see if we want to halt if this node changes value
			brkFlag |= n.nFlags;

			event = (Event)event.fLink;
		}
		while(event != null);

		return brkFlag;
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

			n.nPot = (short)val;
			n.nFlags &= ~Sim.INPUT_MASK;
			n.nFlags |= Sim.INPUT;

			// enqueue event so consequences are computed.
			enqueueInput(n, val);

			if (n.curr.val != val || !n.curr.inp)
				theSim.addHist(n, val, true, theSim.curDelta, 0L, 0L);
		}
	}

	private void MarkNOinputs()
	{
		for(Iterator it = theAnalyzer.xInputs.iterator(); it.hasNext(); )
		{
			Sim.Node n = (Sim.Node)it.next();
			n.nFlags &= ~(Sim.INPUT_MASK | Sim.INPUT);
			n.nFlags |= Sim.VISITED;
		}
	}

	/**
	 * nodes which are no longer inputs
	 */
	private void EvalNOinputs()
	{
		for(Iterator it = theAnalyzer.xInputs.iterator(); it.hasNext(); )
		{
			Sim.Node n = (Sim.Node)it.next();
			theSim.curNode = n;
			theSim.addHist(n, n.curr.val, false, theSim.curDelta, 0L, 0L);
			if ((n.nFlags & Sim.VISITED) != 0)
				modelEvaluate(n);
		}
		theAnalyzer.xInputs.clear();
	}

	/**
	 * compute state of transistor.  If gate is a simple node, state is determined
	 * by type of implant and value of node.  If gate is a list of nodes, then
	 * this transistor represents a stack of transistors in the original network,
	 * and we perform the logical AND of all the gate node values to see if
	 * transistor is on.
	 */
	public int computeTransState(Sim.Trans t)
	{
	    if ((t.tType & Sim.GATELIST) == 0)
	    	return switchState[Sim.baseType(t.tType)][((Sim.Node)t.gate).nPot];
		switch(Sim.baseType(t.tType))
		{
			case Sim.NCHAN:
				int result = Sim.ON;
				for(Sim.Trans l = (Sim.Trans) t.gate; l != null; l = l.getSTrans())
				{
					Sim.Node n = (Sim.Node)l.gate;
					if (n.nPot == Sim.LOW)
						return Sim.OFF;
					else if (n.nPot == Sim.X)
						result = Sim.UNKNOWN;
				}
				return result;

			case Sim.PCHAN:
				result = Sim.ON;
				for(Sim.Trans l = (Sim.Trans) t.gate; l != null; l = l.getSTrans())
				{
					Sim.Node n = (Sim.Node)l.gate;
					if (n.nPot == Sim.HIGH)
						return Sim.OFF;
					else if (n.nPot == Sim.X)
						result = Sim.UNKNOWN;
				}
				return result;

			case Sim.DEP:
			case Sim.RESIST:
				return Sim.WEAK;
		}
		System.out.println("**** internal error: unrecongized transistor type (" + Sim.baseType(t.tType) + ")");
		return Sim.UNKNOWN;
	}

	/***************************************** SCHED *****************************************/

	private static final int TSIZE		= 1024;	/* size of event array, must be power of 2 */
	private static final int TMASK		= (TSIZE - 1);

	private Event [] evArray = new Event[TSIZE];	    /* used as head of doubly-linked lists */

	private Event getEVArray(long t) { return evArray[(int)(t & TMASK)]; }

	/**
	 * find the next event to be processed by scanning event wheel.  Return
	 * the list of events to be processed at this time, removing it first
	 * from the time wheel.
	 */
	private Event getNextEvent(long stopTime)
	{
		if (nPending == 0) return null;

if (DEBUG) System.out.println("Find events up to " + stopTime);
		Event event = null;
		boolean eventValid = false;
		long time = theSim.maxTime;
		for(long i = theSim.curDelta, limit = i + TSIZE; i < limit; i++)
		{
			event = getEVArray(i);
			if (event != event.fLink)
			{
				if (event.fLink.nTime < limit)		// common case
				{
					eventValid = true;
					break;
				}
				if (event.fLink.nTime < time)
					time = (event.fLink).nTime;
			}
		}
		if (!eventValid)
		{
			if (time == theSim.maxTime)
			{
				System.out.println("*** internal error: no events but npending set");
				return null;
			}

			event = getEVArray(time);
		}

		Event evList = event.fLink;

		time = evList.nTime;

		if (time >= stopTime)
		{
if (DEBUG) System.out.println("Time="+time+" which is beyond stop time="+stopTime);
			return null;
		}

		theSim.curDelta = time;			// advance simulation time

		if (event.bLink.nTime != time)	// check tail of list
		{
			do
				event = event.fLink;
			while(event.nTime == time);

			event = event.bLink;		// grab part of the list
			evList.bLink.fLink = event.fLink;
			event.fLink.bLink = evList.bLink;
			evList.bLink = event;
			event.fLink = null;
		} else
		{
			event = evList.bLink;		// grab the entire list
			event.bLink.fLink = null;
			evList.bLink = event.bLink;
			event.fLink = event.bLink = event;
		}
if (DEBUG)
{
	System.out.print("FOUND EVENTS:");
	Event xx = evList;
	do
	{
		System.out.print(" time="+xx.nTime);
		xx = (Event)xx.fLink;
	}
	while(xx != null);
	System.out.println();
}
		return evList;
	}

	/**
	 * remove event from all structures it belongs to and return it to free pool
	 */
	private void freeEvent(Event event)
	{
		// unhook from doubly-linked event list
		event.bLink.fLink = event.fLink;
		event.fLink.bLink = event.bLink;
		nPending--;

		freeFromNode(event, event.eNode);
	}

	/**
	 * Add an event to event list, specifying transition delay and new value.
	 * 0 delay transitions are converted into unit delay transitions (0.01 ns).
	 */
	public void enqueueEvent(Sim.Node n, int newValue, long delta, long rTime)
	{
		Event newEV = new Event();

		// remember facts about this event
		long eTime = theSim.curDelta + delta;
		newEV.nTime = eTime;
		newEV.rTime = (short)rTime;
		newEV.eNode = n;
		newEV.cause = theSim.curNode;
		newEV.delay = delta;
		if (newValue == Sim.DECAY)		// change value to X here
		{
			newEV.eval = Sim.X;
			newEV.type = Sim.DECAY_EV;
		} else
		{
			newEV.eval = (byte)newValue;
			newEV.type = Sim.REVAL;		// for incremental simulation
		}

		/* add the new event to the event list at the appropriate entry
		 * in event wheel.  Event lists are kept sorted by increasing
		 * event time.
		 */
		Event marker = getEVArray(eTime);

		// Check whether we need to insert-sort in the list
		if ((marker.bLink != marker) && (((Event)marker.bLink).nTime > eTime))
		{
			do { marker = (Event)marker.fLink; } while (marker.nTime <= eTime);
		}

		// insert event right before event pointed to by marker
		newEV.fLink = marker;
		newEV.bLink = marker.bLink;
		marker.bLink.fLink = newEV;
		marker.bLink = newEV;
		nPending++;
if (DEBUG) System.out.println("Adding event at " + newEV.nTime + " in enqueueEvent (cur="+theSim.curDelta+" delta="+delta);
		/*
		 * thread event onto list of events for this node, keeping it
		 * in sorted order
		 */
		if ((n.events != null) && (n.events.nTime > eTime))
		{
			for(marker = n.events; (marker.nLink != null) &&
				(marker.nLink.nTime > eTime); marker = marker.nLink);
			newEV.nLink = marker.nLink;
			marker.nLink = newEV;
		} else
		{
			newEV.nLink = n.events;
			n.events = newEV;
		}
	}

	/**
	 * same as enqueueEvent, but assumes 0 delay and rtise/fall time
	 */
	private void enqueueInput(Sim.Node n, int newValue)
	{
		// Punt any pending events for this node.
		while(n.events != null)
			freeEvent(n.events);

		Event newEV = new Event();

		// remember facts about this event
		long eTime = theSim.curDelta;
		newEV.nTime = eTime;
		newEV.rTime = 0;   newEV.delay = 0;
		newEV.eNode = n;
		newEV.cause = n;
		newEV.eval = (byte)newValue;
		newEV.type = Sim.REVAL;			// anything, doesn't matter

		// Add new event to HEAD of list at appropriate entry in event wheel
		Event marker = getEVArray(eTime);
		newEV.fLink = marker.fLink;
		newEV.bLink = marker;
		marker.fLink.bLink = newEV;
		marker.fLink = newEV;
		nPending++;
if (DEBUG) System.out.println("Adding event at " + newEV.nTime + " in enqueueInput");
		// thread event onto (now empty) list of events for this node
		newEV.nLink = null;
		n.events = newEV;
	}

	/**
	 * Initialize event structures
	 */
	public void initEvent()
	{
		for(int i = 0; i < TSIZE; i++)
		{
			Event event = new Event();
			evArray[i] = event;
			event.fLink = event.bLink = event;
		}
		nPending = 0;
		theSim.nEvent = 0;
	}

	protected void puntEvent(Sim.Node node, Event ev)
	{
		if ((node.nFlags & Sim.WATCHED) != 0)
			System.out.println("    punting transition of " + node.nName + " . " +
				Sim.vChars.charAt(ev.eval) + " scheduled for " + Sim.deltaToNS(ev.nTime) + "ns");

		if (ev.type != Sim.DECAY_EV)		// don't save punted decay events
			theSim.addPunted(ev.eNode, ev, theSim.curDelta);
		freeEvent(ev);
	}

	private void requeueEvents(Event evList, boolean thread)
	{
		nPending = 0;
		Event next = null;
		for(Event ev = evList; ev != null; ev = next)
		{
			next = (Event)ev.fLink;

			nPending++;
			long eTime = ev.nTime;
if (DEBUG) System.out.println("Adding of event at time "+eTime + " in requeueEvents");
			Event target = getEVArray(eTime);

			if ((target.bLink != target) && (((Event)target.bLink).nTime > eTime))
			{
				do { target = (Event)target.fLink; } while(target.nTime <= eTime);
			}

			ev.fLink = target;
			ev.bLink = target.bLink;
			target.bLink.fLink = ev;
			target.bLink = ev;

			if (thread)
			{
				Sim.Node n = ev.eNode;
				Event marker;

				if ((n.events != null) && (n.events.nTime > eTime))
				{
					for(marker = n.events; (marker.nLink != null) &&
						(marker.nLink.nTime > eTime); marker = marker.nLink);
					ev.nLink = marker.nLink;
					marker.nLink = ev;
				} else
				{
					ev.nLink = n.events;
					n.events = ev;
				}
			}
		}
	}

	public void printPendingEvents()
	{
		if (nPending == 0) return;
		System.out.println("Warning: there are " + nPending + " pending events:");

		for(int i=0; i<TSIZE; i++)
		{
			Event hdr = evArray[i];
			for(Event evhdr = hdr.fLink; evhdr != hdr; evhdr = evhdr.fLink)
			{
				if (!(evhdr instanceof Event)) continue;
				Event ev = (Event)evhdr;
				System.out.println("   Event at time " + Sim.deltaToNS(ev.nTime) + " caused by node " + ev.cause.nName);
			}
		}
	}

	/**
	 * Back the event queues up to time 'bTime'.  This is the opposite of
	 * advancing the simulation time.  Mark all pending events as PENDING,
	 * and re-enqueue them according to their creation-time (nTime - delay).
	 */
	public Event backSimTime(long bTime, int isInc)
	{
		int nEvents = 0;
		Event tmpList = null;

		// first empty out the time wheel onto the temporary list
		for(int i=0; i<TSIZE; i++)
		{
			Event hdr = evArray[i];
			Event next = null;
			for(Event evhdr = hdr.fLink; evhdr != hdr; evhdr = next)
			{
				next = evhdr.fLink;
				if (!(evhdr instanceof Event)) continue;
				Event ev = (Event)evhdr;

				ev.bLink.fLink = ev.fLink;	// remove event
				ev.fLink.bLink = ev.bLink;
				if (isInc != 0)
					freeFromNode(ev, ev.eNode);

				if (isInc == 0 && ev.nTime - ev.delay >= bTime)
				{
					freeFromNode(ev, ev.eNode);
				} else
				{
					ev.fLink = tmpList;		// move it to tmp list
					tmpList = ev;

					nEvents++;
				}
			}
		}

		if (isInc == 0)
		{
			requeueEvents(tmpList, false);
			return null;
		}

		if (isInc != 1)	// only for fault simulation (isInc == 2)
		{
			nPending = 0;
			return tmpList;
		}

		// now move the temporary list to the time wheel
		Event next = null;
		for(Event ev = tmpList; ev != null; ev = next)
		{
			next = (Event)ev.fLink;

			ev.nTime -= ev.delay;
			ev.type = Sim.PENDING;
			long eTime = ev.nTime;
			Event target = getEVArray(eTime);

			if ((target.bLink != target) && (((Event)target.bLink).nTime > eTime))
			{
				do { target = (Event)target.fLink; } while(target.nTime <= eTime);
			}

			ev.fLink = target;
			ev.bLink = target.bLink;
			target.bLink.fLink = ev;
			target.bLink = ev;
		}

		nPending = nEvents;
		return null;
	}

	private void freeFromNode(Event ev, Sim.Node nd)
	{
		if (nd.events == ev)
			nd.events = ev.nLink;
		else
		{
			Event evp = null;
			for(evp = nd.events; evp.nLink != ev; evp = evp.nLink);
			evp.nLink = ev.nLink;
		}
	}

}
