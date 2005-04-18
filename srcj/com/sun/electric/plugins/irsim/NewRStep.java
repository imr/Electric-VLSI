/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: NewRStep.java
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
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.tool.simulation.Simulation;

import java.util.Iterator;

/**
 * Event-driven timing simulation step for irsim.
 *
 * Use diamond shape region in R-V plane for dc voltage computation.
 * Use 2-pole 1-zero model for pure charge sharing delay calculations.
 * Use 2-pole zero-at-origin model for driven spike analysis.
 * Details of the models can be found in Chorng-Yeong Chu's thesis:
 * "Improved Models for Switch-Level Simulation" also available as a
 * Stanford Technical Report CSL-TR-88-368.
 */
public class NewRStep extends Eval
{
	/* flags used in thevenin structure */
	/** set for a definite rooted path */			private static final int	T_DEFINITE	= 0x01;
	/** set if user specified delay encountered */	private static final int	T_UDELAY	= 0x02;
	/** set if charge-sharing spike possible */		private static final int	T_SPIKE		= 0x04;
	/** set if this branch is driven */				private static final int	T_DRIVEN	= 0x08;
	/** set for the reference node in pure c-s */	private static final int	T_REFNODE	= 0x10;
	/** set if connecting through an X trans. */	private static final int	T_XTRAN		= 0x20;
	/** set if we should consider input slope */	private static final int	T_INT		= 0x40;
	/** set if branch driven to dominant voltage */	private static final int	T_DOMDRIVEN	= 0x80;
	/** nmos-pmos ratio for spike analysis */		private static final double	NP_RATIO	= 0.7;

	private static final int SPIKETBLSIZE    = 10;

	private static final int NLSPKMIN        = 0;
	private static final int NLSPKMAX        = 1;
	private static final int LINEARSPK       = 2;

	/**
	 * one for each possible dominant voltage
	 */
	private static class Dominant
	{
		/** list of nodes driven to this potential */	Sim.Node  nd;
		/** TRUE if this pot needs spike analysis */	boolean   spike;
	};

	private static class SpikeRec
	{
		/** charging delay */	double  chDelay;
		/** driven delay */		double  drDelay;
		/** spike peak */		float   peak;
		/** spike charge */		int     charge;
	}

	/** dominant voltage structure */		private Dominant  [] domPot;
	/** 1 if debug and node is watched */	private int          incLevel;

	private Sim.Thev  [] inputThev;

	private static float spikeTable[][][] =
	{
		/* non-linear nmos driven low / pmos driven high */
		new float[][] {
		/* .01 */  new float[] {0.005f,  0.051f,  0.106f,  0.163f, 0.225f,  0.293f,  0.367f,  0.452f, 0.552f,  0.683f,  0.899f},
		/* 0.1 */  new float[] {0.005f,  0.051f,  0.105f,  0.162f, 0.223f,  0.288f,  0.360f,  0.441f, 0.537f,  0.661f,  0.852f},
		/* 0.2 */  new float[] {0.005f,  0.051f,  0.104f,  0.159f, 0.217f,  0.278f,  0.345f,  0.419f, 0.505f,  0.614f,  0.768f},
		/* 0.3 */  new float[] {0.005f,  0.051f,  0.102f,  0.154f, 0.208f,  0.265f,  0.325f,  0.390f, 0.464f,  0.555f,  0.676f},
		/* 0.4 */  new float[] {0.005f,  0.050f,  0.099f,  0.148f, 0.197f,  0.248f,  0.300f,  0.355f, 0.417f,  0.490f,  0.583f},
		/* 0.5 */  new float[] {0.005f,  0.049f,  0.096f,  0.140f, 0.184f,  0.226f,  0.270f,  0.315f, 0.363f,  0.419f,  0.487f},
		/* 0.6 */  new float[] {0.005f,  0.048f,  0.090f,  0.129f, 0.166f,  0.200f,  0.234f,  0.269f, 0.304f,  0.344f,  0.392f},
		/* 0.7 */  new float[] {0.005f,  0.046f,  0.083f,  0.114f, 0.142f,  0.168f,  0.192f,  0.216f, 0.240f,  0.266f,  0.295f},
		/* 0.8 */  new float[] {0.005f,  0.042f,  0.071f,  0.093f, 0.112f,  0.128f,  0.142f,  0.156f, 0.169f,  0.182f,  0.198f},
		/* 0.9 */  new float[] {0.005f,  0.033f,  0.050f,  0.061f, 0.069f,  0.076f,  0.081f,  0.086f, 0.090f,  0.093f,  0.099f},
		/* .99 */  new float[] {0.003f,  0.008f,  0.009f,  0.009f, 0.009f,  0.010f,  0.010f,  0.010f, 0.010f,  0.010f,  0.010f},
		},

		/* non-linear nmos driven high / pmos driven low */
		new float[][] {
		/* .01 */  new float[] {0.100f,  0.313f,  0.441f,  0.540f, 0.623f,  0.696f,  0.762f,  0.824f, 0.882f,  0.937f,  0.984f},
		/* 0.1 */  new float[] {0.097f,  0.292f,  0.404f,  0.489f, 0.560f,  0.624f,  0.682f,  0.736f, 0.789f,  0.830f,  0.893f},
		/* 0.2 */  new float[] {0.094f,  0.272f,  0.370f,  0.443f, 0.503f,  0.557f,  0.606f,  0.652f, 0.698f,  0.745f,  0.793f},
		/* 0.3 */  new float[] {0.091f,  0.252f,  0.337f,  0.398f, 0.449f,  0.494f,  0.534f,  0.573f, 0.612f,  0.652f,  0.694f},
		/* 0.4 */  new float[] {0.087f,  0.232f,  0.304f,  0.355f, 0.396f,  0.432f,  0.465f,  0.496f, 0.527f,  0.560f,  0.594f},
		/* 0.5 */  new float[] {0.083f,  0.209f,  0.269f,  0.310f, 0.342f,  0.370f,  0.396f,  0.420f, 0.444f,  0.468f,  0.496f},
		/* 0.6 */  new float[] {0.078f,  0.184f,  0.231f,  0.262f, 0.286f,  0.307f,  0.325f,  0.343f, 0.360f,  0.377f,  0.397f},
		/* 0.7 */  new float[] {0.071f,  0.155f,  0.189f,  0.210f, 0.227f,  0.241f,  0.253f,  0.264f, 0.275f,  0.286f,  0.298f},
		/* 0.8 */  new float[] {0.061f,  0.120f,  0.140f,  0.153f, 0.162f,  0.169f,  0.176f,  0.182f, 0.187f,  0.193f,  0.199f},
		/* 0.9 */  new float[] {0.045f,  0.073f,  0.081f,  0.085f, 0.088f,  0.091f,  0.093f,  0.095f, 0.096f,  0.098f,  0.100f},
		/* .99 */  new float[] {0.009f,  0.010f,  0.010f,  0.010f, 0.010f,  0.010f,  0.010f,  0.010f, 0.010f,  0.010f,  0.010f},
		},

		/* linear RC (nmos-pmos mix)*/
		new float[][] {
		/* .01 */  new float[] {0.010f,  0.099f,  0.198f,  0.296f, 0.394f,  0.491f,  0.589f,  0.688f, 0.787f,  0.887f,  0.979f},
		/* 0.1 */  new float[] {0.010f,  0.095f,  0.185f,  0.272f, 0.357f,  0.441f,  0.525f,  0.610f, 0.699f,  0.792f,  0.887f},
		/* 0.2 */  new float[] {0.010f,  0.091f,  0.173f,  0.250f, 0.324f,  0.396f,  0.468f,  0.541f, 0.617f,  0.699f,  0.787f},
		/* 0.3 */  new float[] {0.010f,  0.087f,  0.162f,  0.230f, 0.294f,  0.355f,  0.416f,  0.477f, 0.541f,  0.610f,  0.688f},
		/* 0.4 */  new float[] {0.010f,  0.083f,  0.150f,  0.209f, 0.264f,  0.315f,  0.365f,  0.416f, 0.468f,  0.525f,  0.589f},
		/* 0.5 */  new float[] {0.010f,  0.078f,  0.137f,  0.188f, 0.233f,  0.275f,  0.315f,  0.355f, 0.396f,  0.441f,  0.491f},
		/* 0.6 */  new float[] {0.009f,  0.072f,  0.123f,  0.164f, 0.200f,  0.233f,  0.264f,  0.294f, 0.324f,  0.357f,  0.394f},
		/* 0.7 */  new float[] {0.009f,  0.065f,  0.106f,  0.138f, 0.164f,  0.188f,  0.209f,  0.230f, 0.250f,  0.272f,  0.296f},
		/* 0.8 */  new float[] {0.009f,  0.055f,  0.085f,  0.106f, 0.123f,  0.137f,  0.150f,  0.162f, 0.173f,  0.185f,  0.198f},
		/* 0.9 */  new float[] {0.008f,  0.039f,  0.055f,  0.065f, 0.072f,  0.078f,  0.083f,  0.087f, 0.091f,  0.095f,  0.099f},
		/* .99 */  new float[] {0.004f,  0.008f,  0.009f,  0.009f, 0.009f,  0.010f,  0.010f,  0.010f, 0.010f,  0.010f,  0.010f},
		}
	};

	private static float delayTable[][] =
	{
	/* .01 */  new float[] {9.12006e+00f,  6.31441e-01f,  2.57972e-01f,  1.44287e-01f,
	           9.08200e-02f,  6.01663e-02f,  4.03907e-02f,  2.65355e-02f,
	           1.61530e-02f,  7.81327e-03f,  9.32511e-04f},
	/* 0.1 */  new float[] {6.75920e+01f,  4.23025e+00f,  1.67348e+00f,  9.22844e-01f,
	           5.78068e-01f,  3.83611e-01f,  2.59448e-01f,  1.72782e-01f,
	           1.07510e-01f,  5.40007e-02f,  7.10297e-03f},
	/* 0.2 */  new float[] {1.19421e+02f,  7.15202e+00f,  2.80304e+00f,  1.54448e+00f,
	           9.70528e-01f,  6.47718e-01f,  4.41433e-01f,  2.96820e-01f,
	           1.86968e-01f,  9.55642e-02f,  1.30529e-02f},
	/* 0.3 */  new float[] {1.63622e+02f,  9.51381e+00f,  3.72112e+00f,  2.05847e+00f,
	           1.30175e+00f,  8.75381e-01f,  6.01600e-01f,  4.08196e-01f,
	           2.59717e-01f,  1.34386e-01f,  1.87625e-02f},
	/* 0.4 */  new float[] {2.01466e+02f,  1.14592e+01f,  4.49666e+00f,  2.50681e+00f,
	           1.59963e+00f,  1.08575e+00f,  7.53099e-01f,  5.15657e-01f,
	           3.31075e-01f,  1.72965e-01f,  2.44792e-02f},
	/* 0.5 */  new float[] {2.33031e+02f,  1.30456e+01f,  5.16491e+00f,  2.91360e+00f,
	           1.88135e+00f,  1.29123e+00f,  9.04789e-01f,  6.25272e-01f,
	           4.04824e-01f,  2.13117e-01f,  3.03870e-02f},
	/* 0.6 */  new float[] {2.57614e+02f,  1.42986e+01f,  5.75449e+00f,  3.30168e+00f,
	           2.16446e+00f,  1.50508e+00f,  1.06642e+00f,  7.43858e-01f,
	           4.85264e-01f,  2.56919e-01f,  3.66949e-02f},
	/* 0.7 */  new float[] {2.73495e+02f,  1.52373e+01f,  6.30568e+00f,  3.70558e+00f,
	           2.47626e+00f,  1.74816e+00f,  1.25340e+00f,  8.82200e-01f,
	           5.79179e-01f,  3.07615e-01f,  4.37233e-02f},
	/* 0.8 */  new float[] {2.76873e+02f,  1.59248e+01f,  6.91424e+00f,  4.20379e+00f,
	           2.87725e+00f,  2.06596e+00f,  1.49889e+00f,  1.06318e+00f,
	           7.00760e-01f,  3.71884e-01f,  5.21156e-02f},
	/* 0.9 */  new float[] {2.57710e+02f,  1.67902e+01f,  7.96238e+00f,  5.07908e+00f,
	           3.57464e+00f,  2.60911e+00f,  1.90987e+00f,  1.35912e+00f,
	           8.94002e-01f,  4.70028e-01f,  6.37819e-02f},
	/* .99 */  new float[] {1.96679e+02f,  2.57710e+01f,  1.38437e+01f,  9.11648e+00f,
	           6.44036e+00f,  4.66063e+00f,  3.35777e+00f,  2.33745e+00f,
	           1.49276e+00f,  7.51022e-01f,  9.21218e-02f}
	};


	public NewRStep(Analyzer analyzer, Sim sim)
	{
		super(analyzer, sim);
		initThevs();
		theSim.tUnitDelay = 0;
		theSim.tDecay = 0;
		domPot = new Dominant[Sim.N_POTS];
		for(int i=0; i<Sim.N_POTS; i++) domPot[i] = new Dominant();
	}

	public void modelEvaluate(Sim.Node n)
	{
		for(int i = Sim.LOW; i <= Sim.HIGH; i++)
		{
			domPot[i].nd = null;
			domPot[i].spike = false;
		}

		if ((n.nFlags & Sim.VISITED) != 0)
			theSim.buildConnList(n);

		boolean changes = computeDC(n);
		if (!changes)
			cleanEvents(n);
		else if (theSim.withDriven)
			scheduleDriven();
		else
			schedulePureCS(n);

		if (theSim.tDecay != 0 && ! theSim.withDriven)
			enqueDecay(n);

		undoConnList(n);
	}

	private void cleanEvents(Sim.Node n)
	{
		do
		{
			for(;;)
			{
				Event ev = n.events;
				if (ev == null) break;
				puntEvent(n, ev);
			}
		}
		while((n = n.nLink) != null);
	}

	private void enqueDecay(Sim.Node n)
	{
		do
		{
			Event ev = n.events;
			if (((ev == null) ? n.nPot : ev.eval) != Sim.X)
			{
				if ((theSim.irDebug & Sim.DEBUG_EV) != 0 && (n.nFlags & Sim.WATCHED) != 0)
					System.out.println("  decay transition for " + n.nName + " @ " +
						Sim.deltaToNS(theSim.curDelta + theSim.tDecay)+ "ns");
				enqueueEvent(n, Sim.DECAY, theSim.tDecay, theSim.tDecay);
			}
			n = n.nLink;
		}
		while(n != null);
	}

	private void undoConnList(Sim.Node n)
	{
		Sim.Node next = null;
		do
		{
			next = n.nLink;
			n.nLink = null;

			n.getThev().setT(null);

			for(Iterator it = n.nTermList.iterator(); it.hasNext(); )
			{
				Sim.Trans t = (Sim.Trans)it.next();
				if (t.state == Sim.OFF) continue;

				if ((t.tFlags & (Sim.PBROKEN | Sim.BROKEN)) == 0)
				{
					Sim.Thev  r = t.getSThev();
					if (r != null) r.setT(null);

					r = t.getDThev();
					if (r != null) r.setT(null);
				}
				t.setSThev(null);
				t.setDThev(null);
				t.tFlags &= ~(Sim.CROSSED | Sim.BROKEN | Sim.PBROKEN | Sim.PARALLEL);
			}
		}
		while((n = next) != null);
	}

	/**
	 * Schedule the final value for node 'nd'.  Check to see if this final value
	 * invalidates other events.  Since this event has more recent information
	 * regarding the state of the network, delete transitions scheduled to come
	 * after it.  Zero-delay transitions are avoided by turning them into unit
	 * delays (1 delta).  Events scheduled to occur at the same time as this event
	 * and driving the node to the same value are not punted.
	 * Finally, before scheduling the final value, check that it is different
	 * from the previously calculated value for the node.
	 */
	private void queueFVal(Sim.Node nd, int fVal, double tau, double delay)
	{
		boolean queued = false;
		long delta = theSim.curDelta + Sim.psToDelta(delay);

		// avoid zero delay
		if (delta == theSim.curDelta) delta++;

		Event ev = null;
		for(;;)
		{
			ev = nd.events;
			if (ev == null || ev.nTime < delta) break;
			if (ev.nTime == delta && ev.eval == fVal) break;
			puntEvent(nd, ev);
		}

		delta -= theSim.curDelta;

		if (fVal != ((ev == null) ? nd.nPot : ev.eval))
		{
			enqueueEvent(nd, fVal, delta, Sim.psToDelta(tau));
			queued = true;
		}
		if ((theSim.irDebug & Sim.DEBUG_EV) != 0 && (nd.nFlags & Sim.WATCHED) != 0)
			printFinal(nd, queued, tau, delta);
	}

	private void queueSpike(Sim.Node nd, SpikeRec spk)
	{
		for(;;)
		{
			Event ev = nd.events;
			if (ev == null) break;
			puntEvent(nd, ev);
		}

		// no spike, just punt events
		if (spk == null) return;

		long chDelta = Sim.psToDelta(spk.chDelay);
		long drDelta = Sim.psToDelta(spk.drDelay);

		if (chDelta == 0) chDelta = 1;
		if (drDelta == 0) drDelta = 1;

		if ((theSim.irDebug & Sim.DEBUG_EV) != 0 && (nd.nFlags & Sim.WATCHED) != 0)
			printSpike(nd, spk, chDelta, drDelta);

		// no zero delay spikes, done
		if (drDelta <= chDelta) return;

		// enqueue spike and final value events
		enqueueEvent(nd, spk.charge, chDelta, chDelta);
		enqueueEvent(nd, nd.nPot, drDelta, chDelta);
	}

	private void scheduleDriven()
	{
		for(int dom = 0; dom < Sim.N_POTS; dom++)
		{
			Sim.Thev r = null;
			for(Sim.Node nd = domPot[dom].nd; nd != null; nd = r.getN())
			{
				incLevel = ((theSim.irDebug & (Sim.DEBUG_TAU | Sim.DEBUG_TW)) == (Sim.DEBUG_TAU | Sim.DEBUG_TW) &&
					(nd.nFlags & Sim.WATCHED) != 0) ? 1 : 0;

				r = getTau(nd, (Sim.Trans) null, dom, incLevel);

                if (incLevel == 0 && ((theSim.irDebug & Sim.DEBUG_TAU) == Sim.DEBUG_TAU &&
                    (nd.nFlags & Sim.WATCHED) != 0))
                        printTau(nd, r, -1);

				r.tauA = r.rDom * r.cA;
				r.tauD = r.rDom * r.cD;

				if ((r.flags & T_SPIKE) != 0)		// deal with these later
					continue;

				double tau = 0, delay = 0;
				if (nd.nPot == r.finall)		// no change, just punt
				{
					Event  ev;

					while((ev = nd.events) != null)
						puntEvent(nd, ev);
					continue;
				} else if (theSim.tUnitDelay != 0)
				{
					delay = theSim.tUnitDelay;
					tau = 0.0;
				} else if ((r.flags & T_UDELAY) != 0)
				{
					switch (r.finall)
					{
						case Sim.LOW:  tau = Sim.deltaToPS(r.tpHL);			break;
						case Sim.HIGH: tau = Sim.deltaToPS(r.tpLH);			break;
						case Sim.X:	   tau = Sim.deltaToPS(Math.min(r.tpHL, r.tpLH));	break;
					}
					delay = tau;
				} else
				{
					if (r.finall == Sim.X)
					{
                        if (Simulation.isIRSIMDelayedX()) {
                            // When fighting, instead of using the dominant time constant,
                            // use a value proportional to the difference in pull up and pull down
                            // strengths
                            double reff;
                            if (r.rUp.min == r.rDown.min) {
                                reff = Sim.LIMIT;
                            } else if (r.rUp.min > r.rDown.min) {
                                reff = 1 / ( (1/r.rDown.min) - (1/r.rUp.min));
                            } else {
                                reff = 1 / ( (1/r.rUp.min) - (1/r.rDown.min));
                            }
                            tau = reff * r.cA;
                        } else
						    tau = r.rMin * r.cA;
					} else if ((r.flags & T_DEFINITE) != 0)
					{
						tau = r.rMax * r.cA;
					} else
					{
						tau = r.rDom * r.cA;
					}

					if ((r.flags & T_INT) != 0 && r.tIn > 0.5)
					{
						delay = Math.sqrt(tau * tau + Sim.deltaToPS((long)r.tIn) * r.cA);
					} else
					{
						delay = tau;
					}
				}

				queueFVal(nd, r.finall, tau, delay);
			}

			if (domPot[dom].spike)
			{
				for(Sim.Node nd = domPot[dom].nd; nd != null; nd = nd.getThev().getN())
				{
					r = nd.getThev();
					if ((r.flags & T_SPIKE) == 0) continue;

					incLevel = ((theSim.irDebug & (Sim.DEBUG_TAUP | Sim.DEBUG_TW)) == (Sim.DEBUG_TAUP | Sim.DEBUG_TW) &&
						(nd.nFlags & Sim.WATCHED) != 0) ? 1 : 0;

					r.tauP = getTauP(nd, (Sim.Trans) null, dom, incLevel);

					r.tauP *= r.rDom / r.tauA;

					queueSpike(nd, computeSpike(nd, r, dom));
				}
			}
		}
	}

	private void schedulePureCS(Sim.Node nList)
	{
		Sim.Thev r = nList.getThev();

		int dom = r.finall;
		r.flags |= T_REFNODE;

		double tauP = 0.0;
		for(Sim.Node nd = nList; nd != null; nd = nd.nLink)
		{
			incLevel = ((theSim.irDebug & (Sim.DEBUG_TAU | Sim.DEBUG_TW)) == (Sim.DEBUG_TAU | Sim.DEBUG_TW) &&
				(nd.nFlags & Sim.WATCHED) != 0) ? 1 : 0;

			r = getTau(nd, (Sim.Trans) null, dom, incLevel);

			r.tauD = r.rDom * r.cA;

			switch(dom)
			{
				case Sim.LOW:
					r.tauA = r.rDom * (r.cA - r.cD * r.v.max);
					break;
				case Sim.HIGH:
					r.tauA = r.rDom * (r.cD * (1 - r.v.min) - r.cA);
					break;
				case Sim.X:			// approximate Vf = 0.5
					r.tauA = r.rDom * (r.cA - r.cD * 0.5);
					break;
			}
			tauP += r.tauA * nd.nCap;
		}

		r = nList.getThev();
		tauP = tauP / (r.cLow.min + r.cHigh.max);		// tauP = tauP / CT

		for(Sim.Node  nd = nList; nd != null; nd = nd.nLink)
		{
			r = nd.getThev();
			double delay = 0, tau = 0;
			if (r.finall != nd.nPot)
			{
				switch (r.finall)
				{
					case Sim.LOW:  tau = (r.tauA - tauP) / (1.0 - r.v.max);	 break;
					case Sim.HIGH: tau = (tauP - r.tauA) / r.v.min;		     break;
					case Sim.X:    tau = (r.tauA - tauP) * 2.0;		         break;
				}
				if (tau < 0.0) tau = 0.0;
				if (theSim.tUnitDelay != 0)
				{
					delay = theSim.tUnitDelay;   tau = 0.0;
				} else
					delay = tau;
			}

			queueFVal(nd, r.finall, tau, delay);
		}
	}

	/**
	 * Compute the final value for each node on the connection list.
	 * This routine will update V.min and V.max and add the node to the
	 * corresponding dom_driver entry.  Return TRUE if any node changes value.
	 */
	private boolean computeDC(Sim.Node nList)
	{
		boolean anyChange = false;
		for(Sim.Node thisOne = nList; thisOne != null; thisOne = thisOne.nLink)
		{
			incLevel = ((theSim.irDebug & (Sim.DEBUG_DC | Sim.DEBUG_TW)) == (Sim.DEBUG_DC | Sim.DEBUG_TW) &&
				(thisOne.nFlags & Sim.WATCHED) != 0) ? 1 : 0;

			Sim.Thev r = getDCVal(thisOne, null);
			thisOne.setThev(r);

			if (theSim.withDriven)
			{
				if (r.rDown.min >= Sim.LIMIT)
					r.v.min = 1;
				else
					r.v.min = r.rDown.min / (r.rDown.min + r.rUp.max);
				if (r.rUp.min >= Sim.LIMIT)
					r.v.max = 0;
				else
					r.v.max = r.rDown.max / (r.rDown.max + r.rUp.min);
			} else		// use charge/total charge if undriven
			{
				r.v.min = r.cHigh.min / (r.cHigh.min + r.cLow.max);
				r.v.max = r.cHigh.max / (r.cHigh.max + r.cLow.min);
			}

			if (r.v.min >= thisOne.vHigh)
				r.finall = Sim.HIGH;
			else if (r.v.max <= thisOne.vLow)
				r.finall = Sim.LOW;
			else
				r.finall = Sim.X;

			if (theSim.withDriven)
			{
				/*
				 * if driven and indefinite, driven value must equal
				 * charging value otherwise the final value is X
				 */
				if (r.finall != Sim.X && (r.flags & T_DEFINITE) == 0)
				{
					char cs_val = Sim.X;// always X
					if (r.cHigh.min >= thisOne.vHigh * (r.cHigh.min + r.cLow.max))
						cs_val = Sim.HIGH;
					else if (r.cHigh.max <= thisOne.vLow * (r.cHigh.max + r.cLow.min))
						cs_val = Sim.LOW;

					if (cs_val != r.finall)
						r.finall = Sim.X;
				}

				r.setN(domPot[r.finall].nd);		// add it to list
				domPot[r.finall].nd = thisOne;

				// possible spike if no transition and opposite charge exists
				if (r.finall == thisOne.nPot && (
					(r.finall == Sim.LOW && r.cHigh.min > Sim.SMALL) ||
					(r.finall == Sim.HIGH && r.cLow.min > Sim.SMALL)))
				{
					r.flags |= T_SPIKE;
					domPot[r.finall].spike = true;
					anyChange = true;
				}
			}

			if (r.finall != thisOne.nPot)
				anyChange = true;

			if (((theSim.irDebug & Sim.DEBUG_DC) == Sim.DEBUG_DC &&
				(thisOne.nFlags & Sim.WATCHED) != 0))
					printFVal(thisOne, r);
		}
		return anyChange;
	}

	/**
	 * Compute the parametes used to calculate the final value (cHigh, cLow,
	 * rUp, rDown) by doing a depth-first traversal of the tree rooted at node
	 * 'n'.  The traversal is done by a recursive walk through the tree. Note that
	 * the stage is already a simple tree; loops are broken by buildConnList.  As
	 * a side effect also compute Req of the present transistor and all other
	 * transistors in parallel with it, and any specified user delays.
	 * The parameters are:
	 *
	 * n      : the node whose dc parameters we want.
	 * tran   : the transistor that leads to 'n' (null if none).
	 * level  : level of recursion if we are debugging this node, else 0.
	 */
	private Sim.Thev getDCVal(Sim.Node n, Sim.Trans tran)
	{
		if ((n.nFlags & Sim.INPUT) != 0)
		{
			Sim.Thev r = new Sim.Thev(inputThev[n.nPot]);
			return r;
		}

		Sim.Thev r = new Sim.Thev();
		switch (n.nPot)
		{
			case Sim.LOW:   r.cLow.min = r.cLow.max = n.nCap;	break;
			case Sim.X:     r.cLow.max = r.cHigh.max = n.nCap;	break;
			case Sim.HIGH:  r.cHigh.min = r.cHigh.max = n.nCap;	break;
		}

		for(Iterator it = n.nTermList.iterator(); it.hasNext(); )
		{
			Sim.Trans t = (Sim.Trans)it.next();

			// ignore path going back or through a broken loop
			if (t == tran || t.state == Sim.OFF || (t.tFlags & (Sim.BROKEN | Sim.PBROKEN)) != 0)
				continue;

			Sim.Thev cache;
			Sim.Node other;
			if (n == t.source)
			{
				other = t.drain;	cache = t.getDThev();
			} else
			{
				other = t.source;	cache = t.getSThev();
			}

			/*
			 * if cache is not empty use the value found there, otherwise
			 * compute what is on the other side of the transistor and
			 * transmit the result through a series operation.
			 */
			if (cache == null)
			{
				cache = seriesOP(getDCVal(other, t), t);
				if (n == t.source)
				{
					t.setDThev(cache);
				} else
				{
					t.setSThev(cache);
				}
			}
			parallelOP(r, cache);
		}

		if ((n.nFlags & Sim.USERDELAY) != 0)		// record user delays, if any
		{
			r.tpLH = n.tpLH; r.tpHL = n.tpHL;
			r.flags |= T_UDELAY;
		}

		return r;
	}

	/**
	 * The following methods set Req to the appropriate dynamic resistance.
	 * If the transistor state is UNKNOWN then also set the T_XTRAN flag.
	 */
	private void getReq(Sim.Thev r, Sim.Trans t, int type)
	{
		if ((t.tFlags & Sim.PARALLEL) != 0)
			getParallel(r, t, type);
		else
		{
			r.req.min = t.r.dynRes[type];
			if (t.state == Sim.UNKNOWN)
			{
				r.flags |= T_XTRAN;
			} else
			{
				r.req.max = t.r.dynRes[type];
			}
		}
	}

	private void getMinR(Sim.Thev r, Sim.Trans t)
	{
		if ((t.tFlags & Sim.PARALLEL) != 0)
			getMinParallel(r, t);
		else
		{
			r.req.min = Math.min(t.r.dynRes[Sim.R_HIGH], t.r.dynRes[Sim.R_LOW]);
			if (t.state == Sim.UNKNOWN)
			{
				r.flags |= T_XTRAN;
			} else
			{
				r.req.max = r.req.min;
			}
		}
	}

	/**
	 * Do the same as getReq but deal with parallel transistors.
	 */
	private void getParallel(Sim.Thev r, Sim.Trans t, int restype)
	{
		Sim.Resists rp = t.r;
		double gMin = 1.0 / rp.dynRes[restype];
		double gMax = (t.state == Sim.UNKNOWN) ? 0.0 : gMin;

		for(t = theSim.parallelTransistors[t.nPar]; t != null; t = t.getDTrans())
		{
			rp = t.r;
			gMin += 1.0 / rp.dynRes[restype];
			if (t.state != Sim.UNKNOWN)
				gMax += 1.0 / rp.dynRes[restype];
		}
		r.req.min = 1.0 / gMin;
		if (gMax == 0.0)
		{
			r.flags |= T_XTRAN;
		} else
		{
			r.req.max = 1.0 / gMax;
		}
	}

	/**
	 * Do the same as getParallel but use the minimum dynamic resistance.
	 */
	private void getMinParallel(Sim.Thev r, Sim.Trans t)
	{
		Sim.Resists rp = t.r;
		double gMin = 1.0 / Math.min(rp.dynRes[Sim.R_LOW], rp.dynRes[Sim.R_HIGH]);
		double gMax = (t.state == Sim.UNKNOWN) ? 0.0 : gMin;

		for(t = theSim.parallelTransistors[t.nPar]; t != null; t = t.getDTrans())
		{
			rp = t.r;
			double tmp = 1.0 / Math.min(rp.dynRes[Sim.R_LOW], rp.dynRes[Sim.R_HIGH]);
			gMin += tmp;
			if (t.state != Sim.UNKNOWN)
				gMax += tmp;
		}
		r.req.min = 1.0 / gMin;
		if (gMax == 0.0)
		{
			r.flags |= T_XTRAN;
		} else
		{
			r.req.max = 1.0 / gMax;
		}
	}

	/**
	 * Add transistor 't' in series with thevenin struct 'r'.  As a side effect
	 * set Req for 't'.  The midpoint voltage is used to determine whether to
	 * use the dynamic-high or dynamic-low resistance.  If the branch connecting
	 * to 't' is not driven by an input then use the charge information.  The
	 * current estimates of both resistance or capacitance is used.
	 */
	private Sim.Thev seriesOP(Sim.Thev r, Sim.Trans t)
	{
		if ((r.flags & T_DRIVEN) == 0)
		{
			if (r.cHigh.min > r.cLow.max)
				getReq(r, t, Sim.R_HIGH);
			else if (r.cHigh.max < r.cLow.min)
				getReq(r, t, Sim.R_LOW);
			else
				getMinR(r, t);
			return r;		// no driver, so just set Req
		}

		if (r.rDown.min > r.rUp.max)
			getReq(r, t, Sim.R_HIGH);
		else if (r.rDown.max < r.rUp.min)
			getReq(r, t, Sim.R_LOW);
		else
			getMinR(r, t);

		double upMin = r.rUp.min;
		double downMin = r.rDown.min;

		if (upMin < Sim.LIMIT)
			r.rUp.min += r.req.min * (1.0 + upMin / r.rDown.max);
		if (downMin < Sim.LIMIT)
			r.rDown.min += r.req.min * (1.0 + downMin / r.rUp.max);
		if ((r.flags & T_XTRAN) != 0)
		{
			r.flags &= ~T_DEFINITE;
			r.rUp.max = r.rDown.max = Sim.LARGE;
		} else
		{
			if (r.rUp.max < Sim.LIMIT)
				r.rUp.max += r.req.max * (1.0 + r.rUp.max / downMin);
			if (r.rDown.max < Sim.LIMIT)
				r.rDown.max += r.req.max * (1.0 + r.rDown.max / upMin);
		}
		return r;
	}

	/**
	 * make oldR = (oldR || newR), but watch out for infinte resistances.
	 */
	private double doParallel(double oldR, double newR)
	{
		if (oldR > Sim.LIMIT) return newR;
		if (newR > Sim.LIMIT) return oldR;
		return Sim.combine(oldR, newR);
	}

	/**
	 * Combine the new resistance block of the tree walk with the current one.
	 * Accumulate the low and high capacitances, the user-specified
	 * delay (if any), and the driven flag of the resulting structure.
	 */
	private void parallelOP(Sim.Thev r, Sim.Thev newRB)
	{
		r.cLow.max += newRB.cLow.max;
		r.cHigh.max += newRB.cHigh.max;
		if ((newRB.flags & T_XTRAN) == 0)
		{
			r.cLow.min += newRB.cLow.min;
			r.cHigh.min += newRB.cHigh.min;
		}

		/*
		 * Accumulate the minimum user-specified delay only if the new block
		 * has some drive associated with it.
		 */
		if ((newRB.flags & (T_DEFINITE | T_UDELAY)) == (T_DEFINITE | T_UDELAY))
		{
			if ((r.flags & T_UDELAY) != 0)
			{
				r.tpLH = (short)Math.min(r.tpLH, newRB.tpLH);
				r.tpHL = (short)Math.min(r.tpHL, newRB.tpHL);
			} else
			{
				r.tpLH = newRB.tpLH;
				r.tpHL = newRB.tpHL;
				r.flags |= T_UDELAY;
			}
		}

		if ((newRB.flags & T_DRIVEN) == 0)
			return;				// undriven, just update caps

		r.flags |= T_DRIVEN;		// combined result is driven

		r.rUp.min = doParallel(r.rUp.min, newRB.rUp.min);
		r.rDown.min = doParallel(r.rDown.min, newRB.rDown.min);

		if ((r.flags & newRB.flags & T_DEFINITE) != 0)	// both definite blocks
		{
			r.rUp.max = doParallel(r.rUp.max, newRB.rUp.max);
			r.rDown.max = doParallel(r.rDown.max, newRB.rDown.max);
		}
		else if ((newRB.flags & T_DEFINITE) != 0)		// only new is definite
		{
			r.rUp.max = newRB.rUp.max;
			r.rDown.max = newRB.rDown.max;
			r.flags |= T_DEFINITE;			    // result is definite
		} else				// new (perhaps r) is indefinite
		{
			if (newRB.rUp.max < r.rUp.max)	r.rUp.max = newRB.rUp.max;
			if (newRB.rDown.max < r.rDown.max) r.rDown.max = newRB.rDown.max;
		}
	}

	/**
	 * Determine the input time-constant (input-slope * rStatic).  We are only
	 * interseted in transistors that just turned on (its gate has a transition
	 * at time == Sched.curDelta).  We must be careful not to report as a transition
	 * nodes that stop being inputs (hist.delay == 0 and hist.inp == 0).
	 */
	private boolean isCurrTransition(Sim.HistEnt h)
	{
		return h.hTime == theSim.curDelta && (h.inp || h.delay != 0);
	}

	/**
	 * Return TRUE if we should consider the input slope of this transistor.  As
	 * a side-effect, return the input time constant in 'ptin'.
	 */
	private boolean getTin(Sim.Trans t, GenMath.MutableDouble ptin)
	{
		Sim.HistEnt  h;
		boolean isInt = false;

		if (t.state != Sim.ON)
			return false;

		if ((t.tType & Sim.GATELIST) == 0)
		{
			h = ((Sim.Node)t.gate).curr;
			if (isCurrTransition(h))
			{
				ptin.setValue(h.rTime * t.r.rStatic);
				isInt = true;
			}
		} else
		{
			double tmp = 0.0;

			for(t = (Sim.Trans) t.gate; t != null; t = t.getSTrans())
			{
				h = ((Sim.Node)t.gate).curr;
				if (isCurrTransition(h))
				{
					isInt = true;
					tmp += h.rTime * t.r.rStatic;
				}
			}
			ptin.setValue(tmp);
		}
		return isInt;
	}

	/* combine 2 resistors in parallel, watch out for zero resistance */
	private double combineR(double a, double b) { return ((a + b <= Sim.SMALL) ? 0 : Sim.combine(a, b)); }

	private boolean getParallelTin(Sim.Trans t, GenMath.MutableDouble iTau)
	{
		GenMath.MutableDouble tin = new GenMath.MutableDouble(0);
		boolean isInt = getTin(t, tin);

		for(t = theSim.parallelTransistors[t.nPar]; t != null; t = t.getDTrans())
		{
			GenMath.MutableDouble tmp = new GenMath.MutableDouble(0);
			if (getTin(t, tmp))
			{
				tin.setValue(isInt ? combineR(tin.doubleValue(), tmp.doubleValue()) : tmp.doubleValue());
				isInt = true;
			}
			iTau.setValue(tin.doubleValue());
		}
		return isInt;
	}

	/**
	 * Compute the parameters needed to calculate the 1st order time-constants
	 * (rMin, rDom, rMax, Ca, Cd, Tin) by doing a depth-first traversal of the
	 * tree rooted at node 'n'.  The parameters are gathered by performing a
	 * recursive tree walk similar to computeDC.  As a side effect, the tauP
	 * field will contain the multiplication factor to move a capacitor across
	 * a transistor using 'current distribution', this field may be required
	 * later when computing tauP.  The parameters are:
	 *
	 * n      : the node whose time-constant parameters we want.
	 * dom    : the value of the dominant driver for this stage.
	 * tran   : the transistor that leads to 'n' (null if none).
	 *
	 * This routine can be called more than once if the stage is dominated by
	 * more than 1 potential, hence the tauDone flag keeps track of the potential
	 * for which the parameters stored in the cache were computed.  If the flag
	 * value and the current dominant potential do not match, we go ahead and
	 * recompute the values.
	 */
	private Sim.Thev getTau(Sim.Node n, Sim.Trans tran, int dom, int level)
	{
		Sim.Thev r;
		if (tran == null)
			r = n.getThev();
		else
			r = (tran.source == n) ? tran.getSThev() : tran.getDThev();

		r.tauDone = (char)dom;

		if ((n.nFlags & Sim.INPUT) != 0)
		{
			r.tIn = r.rMin = r.cA = r.cD = 0.0;
			if (n.nPot == dom)
			{
				r.rDom = r.rMax = 0.0;
				r.flags |= T_DOMDRIVEN;
			} else
			{
				r.flags &= ~(T_DOMDRIVEN | T_INT);
				if (dom == Sim.X)
					r.rDom = r.rMax = 0.0;
				else
					r.rDom = r.rMax = Sim.LARGE;
			}
			return r;
		}

		if ((n.getThev().flags & T_REFNODE) != 0)	    // reference node in pure CS
		{
			r.rMin = r.rDom = r.rMax = 0.0;
			r.cA = r.cD = 0.0;
			return r;
		}

		r.rMin = r.rDom = r.rMax = Sim.LARGE;
		r.cD = n.nCap;
		if (dom == Sim.X)			// assume X nodes are charged high
		{
            if (Simulation.isIRSIMDelayedX()) {
                // X nodes are charged to X through Rup and Rdown fighting
                r.cA = n.nCap;
            } else
			    r.cA = (n.nPot == Sim.LOW) ? 0.0 : n.nCap;
		} else
		{
			r.cA = (n.nPot == dom) ? 0.0 : n.nCap;
		}

		r.tIn = 0.0;
		r.flags &= ~(T_DOMDRIVEN | T_INT);

		for(Iterator it = n.nTermList.iterator(); it.hasNext(); )
		{
			Sim.Trans t = (Sim.Trans)it.next();
			if (t.state == Sim.OFF || t == tran || (t.tFlags & (Sim.BROKEN | Sim.PBROKEN)) != 0)
				continue;
			Sim.Node  other;
			Sim.Thev cache;
			if (n == t.source)
			{
				other = t.drain;	cache = t.getDThev();
			} else
			{
				other = t.source;	cache = t.getSThev();
			}
			if (cache.tauDone != dom)
			{
				GenMath.MutableDouble oldR = new GenMath.MutableDouble(0);

				cache = getTau(other, t, dom, level + incLevel);

				// Only use input slope for xtors on the dominant (driven) path
				if ((cache.flags & T_DOMDRIVEN) != 0)
				{
					boolean inputTau = (t.tFlags & Sim.PARALLEL) != 0 ? getParallelTin(t, oldR) : getTin(t, oldR);
					if (inputTau)
					{
						cache.flags |= T_INT;
						cache.tIn += oldR.doubleValue();
					}
				}

				oldR.setValue(cache.rDom);

				cache.rMin += cache.req.min;
				cache.rDom += cache.req.min;
				if ((cache.flags & T_XTRAN) != 0)
				{
					cache.rMax = Sim.LARGE;
				} else
				{
					cache.rMax += cache.req.max;
				}

				// Exclude capacitors if the other side of X transistor == dom
				if ((cache.flags & T_XTRAN) != 0 && other.nPot == dom)
				{
					cache.tauP = cache.cA = cache.cD = 0.0;
				} else if (oldR.doubleValue() > Sim.LIMIT)
				{
					cache.tauP = 1.0;
				} else
				{
					cache.tauP = oldR.doubleValue() / cache.rDom;
					cache.cA *= cache.tauP;
					cache.cD *= cache.tauP;
				}
			}

			r.cA += cache.cA;
			r.cD += cache.cD;
			r.rMin = Sim.combine(r.rMin, cache.rMin);
			if (r.rDom > Sim.LIMIT)
			{
				r.rDom = cache.rDom;
				r.rMax = cache.rMax;
			} else if (cache.rDom < Sim.LIMIT)
			{
				r.rDom = Sim.combine(r.rDom, cache.rDom);
				r.rMax = Sim.combine(r.rMax, cache.rMax);
			}
			if ((cache.flags & T_DOMDRIVEN) != 0)
				r.flags |= T_DOMDRIVEN;	// at least 1 dominant driven path

			if ((cache.flags & T_INT) != 0)
			{
				if ((r.flags & T_INT) != 0)
					r.tIn = combineR(r.tIn, cache.tIn);
				else
				{
					r.tIn = cache.tIn;
					r.flags |= T_INT;
				}
			}
		}

		if (level > 0)
			printTau(n, r, level);

		return r;
	}

	/**
	 * Calculate the 2nd order time constant (tauP) for the net configuration
	 * as seen through node 'n'.  The net traversal and the parameters are
	 * similar to 'getTau'.  Note that at this point we have not have calculated
	 * tauA for nodes not driven to the dominant potential, hence we need to
	 * compute those by first calling getTau.  This routine will update the tauP
	 * entry as well as the tauPDone flag.
	 */
	private double getTauP(Sim.Node n, Sim.Trans tran, int dom, int level)
	{
		if ((n.nFlags & Sim.INPUT) != 0) return 0.0;

		Sim.Thev r = n.getThev();
		if (r.tauDone != dom)		// compute tauA for the node
		{
			r = getTau(n, (Sim.Trans) null, dom, 0);
			r.tauA = r.rDom * r.cA;
			r.tauD = r.rDom * r.cD;
		}

		double taup = r.tauA * n.nCap;

		for(Iterator it = n.nTermList.iterator(); it.hasNext(); )
		{
			Sim.Trans t = (Sim.Trans)it.next();
			if (t.state == Sim.OFF || t == tran || (t.tFlags & (Sim.BROKEN | Sim.PBROKEN)) != 0)
				continue;

			Sim.Node other;
			if (t.source == n)
			{
				other = t.drain;	r = t.getDThev();
			} else
			{
				other = t.source;	r = t.getSThev();
			}

			if (r.tauPDone != dom)
			{
				r.tauP *= getTauP(other, t, dom, level + incLevel);
				r.tauPDone = (char)dom;
			}
			taup += r.tauP;
		}
		if (level > 0)
			printTauP(n, level, taup);

		return taup;
	}

	/**
	 * Compute the size of spike.  If the spike is too small return null, else
	 * fill in the appropriate structure and return a pointer to it.  In order
	 * to determine in which table to lookup the spike peak we look at the
	 * conductivity of all ON transistors connected to node 'nd'; the type with
	 * the largest conductivity determines whether it is mostly an nmos or pmos
	 * network.  This simple scheme should work for most simple nets.
	 */
	private SpikeRec computeSpike(Sim.Node nd, Sim.Thev r, int dom)
	{
		if (r.tauP <= Sim.SMALL)		// no capacitance, no spike
		{
			if ((theSim.irDebug & Sim.DEBUG_SPK) != 0 && (nd.nFlags & Sim.WATCHED) != 0)
				System.out.println(" spike(" + nd.nName + ") ignored (taup=0)");
			return null;
		}

		int rType = (dom == Sim.LOW) ? Sim.R_LOW : Sim.R_HIGH;
		float nmos = 0, pmos = 0;
		for(Iterator it = nd.nTermList.iterator(); it.hasNext(); )
		{
			Sim.Trans t = (Sim.Trans)it.next();
			if (t.state == Sim.OFF || (t.tFlags & Sim.BROKEN) != 0)
				continue;
			if (Sim.baseType(t.tType) == Sim.PCHAN)
				pmos += 1.0f / t.r.dynRes[rType];
			else
				nmos += 1.0f / t.r.dynRes[rType];
		}
		int tabIndex = 0;
		if (nmos > NP_RATIO * (pmos + nmos))		// mostly nmos
			tabIndex = (rType == Sim.R_LOW) ? NLSPKMIN : NLSPKMAX;
		else if (pmos > NP_RATIO * (pmos + nmos))		// mostly pmos
			tabIndex = (rType == Sim.R_LOW) ? NLSPKMAX : NLSPKMIN;
		else
			tabIndex = LINEARSPK;

		int alpha = (int) (SPIKETBLSIZE * r.tauA / (r.tauA + r.tauP - r.tauD));
		if (alpha < 0) alpha = 0; else
			if (alpha > SPIKETBLSIZE) alpha = SPIKETBLSIZE;

		int beta = (int) (SPIKETBLSIZE * (r.tauD - r.tauA) / r.tauD);
		if (beta < 0) beta = 0; else
			if (beta > SPIKETBLSIZE) beta = SPIKETBLSIZE;

		SpikeRec spk = new SpikeRec();
		spk.peak = spikeTable[tabIndex][beta][alpha];
		spk.chDelay = delayTable[beta][alpha];

		if (dom == Sim.LOW)
		{
			if (spk.peak <= nd.vLow)		// spike is too small
			{
				if ((theSim.irDebug & Sim.DEBUG_SPK) != 0 && (nd.nFlags & Sim.WATCHED) != 0)
					printSpk(nd, r, tabIndex, dom, alpha, beta, spk, false);
				return null;
			}
			spk.charge = (spk.peak >= nd.vHigh) ? Sim.HIGH : Sim.X;
		} else	// dom == HIGH
		{
			if (spk.peak <= 1.0 - nd.vHigh)
			{
				if ((theSim.irDebug & Sim.DEBUG_SPK) != 0 && (nd.nFlags & Sim.WATCHED) != 0)
					printSpk(nd, r, tabIndex, dom, alpha, beta, spk, false);
				return null;
			}
			spk.charge = (spk.peak >= 1.0 - nd.vLow) ? Sim.LOW : Sim.X;
		}

		spk.chDelay *= r.tauA * r.tauD / r.tauP;

		if (r.rMax < Sim.LARGE)
			spk.drDelay = r.rMax * r.cA;
		else
			spk.drDelay = r.rDom * r.cA;

		if ((theSim.irDebug & Sim.DEBUG_SPK) != 0 && (nd.nFlags & Sim.WATCHED) != 0)
			printSpk(nd, r, tabIndex, dom, alpha, beta, spk, true);
		return spk;
	}

	private void printFVal(Sim.Node n, Sim.Thev r)
	{
		System.out.print(" final_value(" + n.nName + ")  V=[" + r.v.min + ", " + r.v.max + "]  => " +
			Sim.vChars.charAt(r.finall));
		System.out.println((r.flags & T_SPIKE) != 0 ? "  (spk)" : "");
	}

	private void printFinal(Sim.Node nd, boolean queued, double tau, long delay)
	{
		Sim.Thev r = nd.getThev();
		long dtau = Sim.psToDelta(tau);

		System.out.print(" [event " + theSim.curNode.nName + "->" +
		Sim.vChars.charAt(theSim.curNode.nPot) + " @ " + Sim.deltaToNS(theSim.curDelta) + "] ");

		System.out.print(queued ? ("causes " + (theSim.withDriven ? "" : "CS") + "transition for") : "evaluates");

		System.out.print(" " + nd.nName + ": " + Sim.vChars.charAt(nd.nPot) + " -> " + Sim.vChars.charAt(r.finall));
		System.out.println(" (tau=" + Sim.deltaToNS(dtau) + "ns, delay=" + Sim.deltaToNS(delay) + "ns)");
	}

	private void printSpike(Sim.Node nd, SpikeRec spk, long chDelay, long drDelay)
	{
		System.out.print("  [event " + theSim.curNode.nName + "->" +
				Sim.vChars.charAt(theSim.curNode.nPot) + "@ " + Sim.deltaToNS(theSim.curDelta) + "] causes ");
		if (drDelay <= chDelay)
			System.out.print("suppressed ");

		System.out.print("spike for " + nd.nName + ": " + Sim.vChars.charAt(nd.nPot) + " -> " +
			Sim.vChars.charAt(spk.charge) + " -> " + Sim.vChars.charAt(nd.nPot));
		System.out.println(" (peak=" + spk.peak + " delay: ch=" + Sim.deltaToNS(chDelay) + "ns, dr=" + Sim.deltaToNS(drDelay) + "ns)");
	}

	private void printTauP(Sim.Node n, int level, double taup)
	{
		System.out.println("tauP(" + n.nName + ") = " + Sim.psToNS(taup) + " ns");
	}

	private void printTau(Sim.Node n, Sim.Thev r, int level)
	{
		System.out.println(" ...............compute_tau(" + n.nName + ")");
		System.out.print  ("                {Rmin=" + rToAscii(r.rMin) + "  Rdom=" + rToAscii(r.rDom) + "  Rmax=" + rToAscii(r.rMax) + "}");
		System.out.println(" {Ca=" + r.cA + "  Cd=" + r.cD + "}");

		System.out.print  ("                tauA=" + Sim.psToNS(r.rDom * r.cA) + "  tauD=" + Sim.psToNS(r.rDom * r.cD) + " ns, RTin=");
		if ((r.flags & T_INT) != 0)
			System.out.println(Sim.deltaToNS((long)r.tIn) + " ohm*ns");
		else
			System.out.println("-");
	}

	private String rToAscii(double r)
	{
		if (r >= Sim.LIMIT) return " - ";
		if (r > 1.0)
		{
			int exp = 0;
			for( ; r >= 1000.0; exp++, r *= 0.001) ;
			return TextUtils.formatDouble(r) + " KMG".charAt(exp);
		}
		return TextUtils.formatDouble(r);
	}


	private void printSpk(Sim.Node nd, Sim.Thev r, int tab, int dom, int alpha,
		int beta, SpikeRec spk, boolean isSpk)
	{
		System.out.print(" spike_analysis(" + nd.nName + "):");
		String net_type = "";
		if (tab == LINEARSPK)
			net_type = "n-p mix";
		else if (tab == NLSPKMIN)
			net_type = (dom == Sim.LOW) ? "nmos" : "pmos";
		else
			net_type = (dom == Sim.LOW) ? "pmos" : "nmos";

		System.out.print(" " + net_type + " driven " + ((dom == Sim.LOW) ? "low" : "high"));
		System.out.print("{tauA=" + Sim.psToNS(r.tauA) + "  tauD=" + Sim.psToNS(r.tauD) + "  tauP=" + Sim.psToNS(r.tauP) + "} ns  ");
		System.out.print("alpha=" + alpha + "  beta=" + beta + " => peak=" + spk.peak);
		if (isSpk)
			System.out.println(" v=" + Sim.vChars.charAt(spk.charge));
		else
			System.out.println(" (too small)");
	}

	/**
	 * Initialize pre-initialized thevenin structs.
	 */
	private void initThevs()
	{
		inputThev = new Sim.Thev[Sim.N_POTS];
		for(int i=0; i<Sim.N_POTS; i++) inputThev[i] = new Sim.Thev();

		Sim.Thev t = inputThev[Sim.LOW];
		t.flags		= T_DEFINITE | T_DRIVEN;
		t.rDown.min	= Sim.SMALL;
		t.rDown.max	= Sim.SMALL;
		t.v.min		= 0.0;
		t.rMin		= Sim.SMALL;
		t.finall	= Sim.LOW;

		t = inputThev[Sim.HIGH];
		t.flags		= T_DEFINITE | T_DRIVEN;
		t.rUp.min	= Sim.SMALL;
		t.rUp.max	= Sim.SMALL;
		t.v.min		= 1.0;
		t.rMin		= Sim.SMALL;
		t.finall	= Sim.HIGH;

		t = inputThev[Sim.X];
		t.flags		= T_DEFINITE | T_DRIVEN;
		t.rUp.min	= Sim.SMALL;
		t.rDown.min	= Sim.SMALL;
		t.rMin		= Sim.SMALL;

		inputThev[Sim.X_X] = inputThev[Sim.X];
	}
}
