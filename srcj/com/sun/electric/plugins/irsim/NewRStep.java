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

public class NewRStep extends Eval
{
	/*
	 * Event-driven timing simulation step for irsim.
	 *
	 * Use diamond shape region in R-V plane for dc voltage computation.
	 * Use 2-pole 1-zero model for pure charge sharing delay calculations.
	 * Use 2-pole zero-at-origin model for driven spike analysis.
	 * Details of the models can be found in Chorng-Yeong Chu's thesis:
	 * "Improved Models for Switch-Level Simulation" also available as a
	 * Stanford Technical Report CSL-TR-88-368.
	 */

	/* flags used in thevenin structure */
	private static final int	T_DEFINITE	= 0x01;	/* set for a definite rooted path	    */
	private static final int	T_UDELAY	= 0x02;	/* set if user specified delay encountered  */
	private static final int	T_SPIKE		= 0x04;	/* set if charge-sharing spike possible	    */
	private static final int	T_DRIVEN	= 0x08;	/* set if this branch is driven		    */
	private static final int	T_REFNODE	= 0x10;	/* set for the reference node in pure c-s   */
	private static final int	T_XTRAN		= 0x20;	/* set if connecting through an X trans.    */
	private static final int	T_INT		= 0x40;	/* set if we should consider input slope    */
	private static final int	T_DOMDRIVEN	= 0x80;	/* set if branch driven to dominant voltage */
	private static final double	NP_RATIO	= 0.7;	    /* nmos-pmos ratio for spike analysis */

	private static class Dominant			/* one for each possible dominant voltage */
	{
		Sim.Node  nd;			/* list of nodes driven to this potential */
		boolean   spike;		/* TRUE if this pot needs spike analysis */
	};

	private static class SpikeRec
	{
		double  ch_delay;		/* charging delay */
		double  dr_delay;		/* driven delay */
		float   peak;		/* spike peak */
		int     charge;		/* spike charge */
	}

	private static	Dominant  [] dom_pot;	/* dominant voltage structure */

	private static	Sim.Thev  init_thev;		/* pre-initialized thevenin structs */
	private static	Sim.Thev  [] input_thev;

	private static final int SPIKETBLSIZE    = 10;

	private static final int NLSPKMIN        = 0;
	private static final int NLSPKMAX        = 1;
	private static final int LINEARSPK       = 2;

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
		irsim_InitThevs();
		theSim.irsim_tunitdelay = 0;
		theSim.irsim_tdecay = 0;
		dom_pot = new Dominant[Sim.N_POTS];
		for(int i=0; i<Sim.N_POTS; i++) dom_pot[i] = new Dominant();
	}

	public void modelEvaluate(Sim.Node n)
	{
		for(int i = Sim.LOW; i <= Sim.HIGH; i++)
		{
			dom_pot[i].nd = null;
			dom_pot[i].spike = false;
		}

		if ((n.nflags & Sim.VISITED) != 0)
			theSim.irsim_BuildConnList(n);

		boolean changes = ComputeDC(n);
		if (!changes)
			CleanEvents(n);
		else if (theSim.irsim_withdriven)
			scheduleDriven();
		else
			schedulePureCS(n);

		if (theSim.irsim_tdecay != 0 && ! theSim.irsim_withdriven)
			EnqueDecay(n);

		UndoConnList(n);
	}

	private void CleanEvents(Sim.Node n)
	{
		do
		{
			for(;;)
			{
				Sim.Event ev = n.events;
				if (ev == null) break;
				irsim_PuntEvent(n, ev);
			}
		}
		while((n = n.nlink) != null);
	}

	private void EnqueDecay(Sim.Node n)
	{
		do
		{
			Sim.Event ev = n.events;
			if (((ev == null) ? n.npot : ev.eval) != Sim.X)
			{
				irsim_enqueue_event(n, Sim.DECAY, (long) theSim.irsim_tdecay, (long) theSim.irsim_tdecay);
			}
			n = n.nlink;
		}
		while(n != null);
	}

	private void UndoConnList(Sim.Node n)
	{
		Sim.Node next = null;
		do
		{
			next = n.nlink;
			n.nlink = null;

			n.getThev().setT(null);

			for(Sim.Tlist l = n.nterm; l != null; l = l.next)
			{
				Sim.Trans t = l.xtor;
				if (t.state == Sim.OFF)
					continue;

				if ((t.tflags & (Sim.PBROKEN | Sim.BROKEN)) == 0)
				{
					Sim.Thev  r = t.getSThev();

					if (r != null)
					{
						r.setT(null);
					}
					if ((r = t.getDThev()) != null)
					{
						r.setT(null);
					}
				}
				t.setSThev(null);
				t.setDThev(null);
				t.tflags &= ~(Sim.CROSSED | Sim.BROKEN | Sim.PBROKEN | Sim.PARALLEL);
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
	private void irsim_QueueFVal(Sim.Node nd, int fval, double tau, double delay)
	{
		boolean queued = false;
		long delta = theSim.irsim_cur_delta + (long) Sim.ps2d(delay);
		if (delta == theSim.irsim_cur_delta)			// avoid zero delay
			delta++;

		Sim.Event ev = null;
		for(;;)
		{
			ev = nd.events;
			if (ev == null || ev.ntime < delta) break;
			if (ev.ntime == delta && ev.eval == fval) break;
			irsim_PuntEvent(nd, ev);
		}

		delta -= theSim.irsim_cur_delta;

		if (fval != ((ev == null) ? nd.npot : ev.eval))
		{
			irsim_enqueue_event(nd, fval, (long) delta, (long) Sim.ps2d(tau));
			queued = true;
		}
	}

	private void QueueSpike(Sim.Node nd, SpikeRec spk)
	{
		for(;;)
		{
			Sim.Event ev = nd.events;
			if (ev == null) break;
			irsim_PuntEvent(nd, ev);
		}

		if (spk == null)		// no spike, just punt events
		{
			return;
		}

		long ch_delta = Sim.ps2d(spk.ch_delay);
		long dr_delta = Sim.ps2d(spk.dr_delay);

		if (ch_delta == 0)
			ch_delta = 1;
		if (dr_delta == 0)
			dr_delta = 1;

		if (dr_delta <= ch_delta)		// no zero delay spikes, done
		{
			return;
		}

		// enqueue spike and final value events
		irsim_enqueue_event(nd, (int) spk.charge, (long) ch_delta, (long) ch_delta);
		irsim_enqueue_event(nd, (int) nd.npot, (long) dr_delta, (long) ch_delta);
	}

	private void scheduleDriven()
	{
		for(int dom = 0; dom < Sim.N_POTS; dom++)
		{
			Sim.Thev r = null;
			for(Sim.Node nd = dom_pot[dom].nd; nd != null; nd = r.getN())
			{
				r = get_tau(nd, (Sim.Trans) null, dom);

				r.tauA = r.Rdom * r.Ca;
				r.tauD = r.Rdom * r.Cd;

				if ((r.flags & T_SPIKE) != 0)		// deal with these later
					continue;

				double tau = 0, delay = 0;
				if (nd.npot == r.finall)		// no change, just punt
				{
					Sim.Event  ev;

					while((ev = nd.events) != null)
						irsim_PuntEvent(nd, ev);
					continue;
				} else if (theSim.irsim_tunitdelay != 0)
				{
					delay = theSim.irsim_tunitdelay;
					tau = 0.0;
				} else if ((r.flags & T_UDELAY) != 0)
				{
					switch(r.finall)
					{
						case Sim.LOW:  tau = Sim.d2ps(r.tphl);			break;
						case Sim.HIGH: tau = Sim.d2ps(r.tplh);			break;
						case Sim.X:	   tau = Sim.d2ps(Math.min(r.tphl, r.tplh));	break;
					}
					delay = tau;
				} else
				{
					if (r.finall == Sim.X)
					{
						tau = r.Rmin * r.Ca;
					} else if ((r.flags & T_DEFINITE) != 0)
					{
						tau = r.Rmax * r.Ca;
					} else
					{
						tau = r.Rdom * r.Ca;
					}

					if ((r.flags & T_INT) != 0 && r.Tin > 0.5)
					{
						delay = Math.sqrt(tau * tau + Sim.d2ps((long)r.Tin) * r.Ca);
					} else
					{
						delay = tau;
					}
				}

				irsim_QueueFVal(nd, (int) r.finall, tau, delay);
			}

			if (dom_pot[dom].spike)
			{
				for(Sim.Node nd = dom_pot[dom].nd; nd != null; nd = nd.getThev().getN())
				{
					r = nd.getThev();
					if ((r.flags & T_SPIKE) == 0)
						continue;

					r.tauP = get_tauP(nd, (Sim.Trans) null, dom);

					r.tauP *= r.Rdom / r.tauA;

					QueueSpike(nd, ComputeSpike(nd, r, dom));
				}
			}
		}
	}

	private void schedulePureCS(Sim.Node nlist)
	{
		Sim.Thev r = nlist.getThev();

		int dom = r.finall;
		r.flags |= T_REFNODE;

		double taup = 0.0;
		for(Sim.Node nd = nlist; nd != null; nd = nd.nlink)
		{
			r = get_tau(nd, (Sim.Trans) null, dom);

			r.tauD = r.Rdom * r.Ca;

			switch(dom)
			{
				case Sim.LOW:
					r.tauA = r.Rdom * (r.Ca - r.Cd * r.V.max);
					break;
				case Sim.HIGH:
					r.tauA = r.Rdom * (r.Cd * (1 - r.V.min) - r.Ca);
					break;
				case Sim.X:			// approximate Vf = 0.5
					r.tauA = r.Rdom * (r.Ca - r.Cd * 0.5);
					break;
			}
			taup += r.tauA * nd.ncap;
		}

		r = nlist.getThev();
		taup = taup / (r.Clow.min + r.Chigh.max);		// tauP = tauP / CT

		for(Sim.Node  nd = nlist; nd != null; nd = nd.nlink)
		{
			r = nd.getThev();
			double delay = 0, tau = 0;
			if (r.finall != nd.npot)
			{
				switch(r.finall)
				{
					case Sim.LOW:  tau = (r.tauA - taup) / (1.0 - r.V.max);	break;
					case Sim.HIGH: tau = (taup - r.tauA) / r.V.min;		break;
					case Sim.X:    tau = (r.tauA - taup) * 2.0;		break;
				}
				if (tau < 0.0)
					tau = 0.0;
				if (theSim.irsim_tunitdelay != 0)
				{
					delay = theSim.irsim_tunitdelay;   tau = 0.0;
				} else
					delay = tau;
			}

			irsim_QueueFVal(nd, (int) r.finall, tau, delay);
		}
	}

	/**
	 * Compute the final value for each node on the connection list.
	 * This routine will update V.min and V.max and add the node to the
	 * corresponding dom_driver entry.  Return TRUE if any node changes value.
	 */
	private boolean ComputeDC(Sim.Node nlist)
	{
		boolean anyChange = false;
		for(Sim.Node thisone = nlist; thisone != null; thisone = thisone.nlink)
		{
			Sim.Thev r = get_dc_val(thisone, null);
			thisone.setThev(r);

			if (theSim.irsim_withdriven)
			{
				if (r.Rdown.min >= Sim.LIMIT)
					r.V.min = 1;
				else
					r.V.min = r.Rdown.min / (r.Rdown.min + r.Rup.max);
				if (r.Rup.min >= Sim.LIMIT)
					r.V.max = 0;
				else
					r.V.max = r.Rdown.max / (r.Rdown.max + r.Rup.min);
			} else		// use charge/total charge if undriven
			{
				r.V.min = r.Chigh.min / (r.Chigh.min + r.Clow.max);
				r.V.max = r.Chigh.max / (r.Chigh.max + r.Clow.min);
			}

			if (r.V.min >= thisone.vhigh)
				r.finall = Sim.HIGH;
			else if (r.V.max <= thisone.vlow)
				r.finall = Sim.LOW;
			else
				r.finall = Sim.X;

			if (theSim.irsim_withdriven)
			{
				/*
				 * if driven and indefinite, driven value must equal
				 * charging value otherwise the final value is X
				 */
				if (r.finall != Sim.X && (r.flags & T_DEFINITE) == 0)
				{
					char  cs_val;

					if (r.Chigh.min >= thisone.vhigh * (r.Chigh.min + r.Clow.max))
						cs_val = Sim.HIGH;
					else if (r.Chigh.max <= thisone.vlow * (r.Chigh.max + r.Clow.min))
						cs_val = Sim.LOW;
					else
						cs_val = Sim.X;			// always X

					if (cs_val != r.finall)
						r.finall = Sim.X;
				}

				r.setN(dom_pot[r.finall].nd);		// add it to list
				dom_pot[r.finall].nd = thisone;

				// possible spike if no transition and opposite charge exists
				if (r.finall == thisone.npot && (
					(r.finall == Sim.LOW && r.Chigh.min > Sim.SMALL) ||
					(r.finall == Sim.HIGH && r.Clow.min > Sim.SMALL)))
				{
					r.flags |= T_SPIKE;
					dom_pot[r.finall].spike = true;
					anyChange = true;
				}
			}

			if (r.finall != thisone.npot)
				anyChange = true;
		}
		return anyChange;
	}

	/**
	 * Compute the parametes used to calculate the final value (Chigh, Clow,
	 * Rup, Rdown) by doing a depth-first traversal of the tree rooted at node
	 * 'n'.  The traversal is done by a recursive walk through the tree. Note that
	 * the stage is already a simple tree; loops are broken by irsim_BuildConnList.  As
	 * a side effect also compute Req of the present transistor and all other
	 * transistors in parallel with it, and any specified user delays.
	 * The parameters are:
	 *
	 * n      : the node whose dc parameters we want.
	 * tran   : the transistor that leads to 'n' (null if none).
	 * level  : level of recursion if we are debugging this node, else 0.
	 */
	private Sim.Thev get_dc_val(Sim.Node n, Sim.Trans tran)
	{
		if ((n.nflags & Sim.INPUT) != 0)
		{
			Sim.Thev r = new Sim.Thev(input_thev[n.npot]);
			return r;
		}

		Sim.Thev r = new Sim.Thev(init_thev);
		switch(n.npot)
		{
			case Sim.LOW:   r.Clow.min = r.Clow.max = n.ncap;	break;
			case Sim.X:     r.Clow.max = r.Chigh.max = n.ncap;	break;
			case Sim.HIGH:  r.Chigh.min = r.Chigh.max = n.ncap;	break;
		}

		for(Sim.Tlist l = n.nterm; l != null; l = l.next)
		{
			Sim.Trans t = l.xtor;

			// ignore path going back or through a broken loop
			if (t == tran || t.state == Sim.OFF || (t.tflags & (Sim.BROKEN | Sim.PBROKEN)) != 0)
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
				cache = series_op(get_dc_val(other, t), t);
				if (n == t.source)
				{
					t.setDThev(cache);
				} else
				{
					t.setSThev(cache);
				}
			}
			parallel_op(r, cache);
		}

		if ((n.nflags & Sim.USERDELAY) != 0)		// record user delays, if any
		{
			r.tplh = n.tplh; r.tphl = n.tphl;
			r.flags |= T_UDELAY;
		}

		return r;
	}

	/**
	 * The following methods set Req to the appropriate dynamic resistance.
	 * If the transistor state is UNKNOWN then also set the T_XTRAN flag.
	 */
	private void GetReq(Sim.Thev r, Sim.Trans t, int type)
	{
		if ((t.tflags & Sim.PARALLEL) != 0)
			get_parallel(r, t, type);
		else
		{
			r.Req.min = t.r.dynres[type];
			if (t.state == Sim.UNKNOWN)
			{
				r.flags |= T_XTRAN;
			} else
				r.Req.max = t.r.dynres[type];
		}
	}

	private void GetMinR(Sim.Thev r, Sim.Trans t)
	{
		if ((t.tflags & Sim.PARALLEL) != 0)
			get_min_parallel(r, t);
		else
		{
			r.Req.min = Math.min(t.r.dynres[Sim.R_HIGH], t.r.dynres[Sim.R_LOW]);
			if (t.state == Sim.UNKNOWN)
			{
				r.flags |= T_XTRAN;
			} else
				r.Req.max = r.Req.min;
		}
	}

	/**
	 * Do the same as GetReq but deal with parallel transistors.
	 */
	private void get_parallel(Sim.Thev r, Sim.Trans t, int restype)
	{
		Sim.Resists rp = t.r;
		double gmin = 1.0 / rp.dynres[restype];
		double gmax = (t.state == Sim.UNKNOWN) ? 0.0 : gmin;

		for(t = theSim.irsim_parallel_xtors[t.n_par]; t != null; t = t.getDTrans())
		{
			rp = t.r;
			gmin += 1.0 / rp.dynres[restype];
			if (t.state != Sim.UNKNOWN)
				gmax += 1.0 / rp.dynres[restype];
		}
		r.Req.min = 1.0 / gmin;
		if (gmax == 0.0)
		{
			r.flags |= T_XTRAN;
		} else
		r.Req.max = 1.0 / gmax;
	}

	/**
	 * Do the same as get_parallel but use the minimum dynamic resistance.
	 */
	private void get_min_parallel(Sim.Thev r, Sim.Trans t)
	{
		Sim.Resists rp = t.r;
		double gmin = 1.0 / Math.min(rp.dynres[Sim.R_LOW], rp.dynres[Sim.R_HIGH]);
		double gmax = (t.state == Sim.UNKNOWN) ? 0.0 : gmin;

		for(t = theSim.irsim_parallel_xtors[t.n_par]; t != null; t = t.getDTrans())
		{
			rp = t.r;
			double tmp = 1.0 / Math.min(rp.dynres[Sim.R_LOW], rp.dynres[Sim.R_HIGH]);
			gmin += tmp;
			if (t.state != Sim.UNKNOWN)
				gmax += tmp;
		}
		r.Req.min = 1.0 / gmin;
		if (gmax == 0.0)
		{
			r.flags |= T_XTRAN;
		} else
			r.Req.max = 1.0 / gmax;
	}

	/**
	 * Add transistor 't' in series with thevenin struct 'r'.  As a side effect
	 * set Req for 't'.  The midpoint voltage is used to determine whether to
	 * use the dynamic-high or dynamic-low resistance.  If the branch connecting
	 * to 't' is not driven by an input then use the charge information.  The
	 * current estimates of both resistance or capacitance is used.
	 */
	private Sim.Thev series_op(Sim.Thev r, Sim.Trans t)
	{
		if ((r.flags & T_DRIVEN) == 0)
		{
			if (r.Chigh.min > r.Clow.max)
				GetReq(r, t, Sim.R_HIGH);
			else if (r.Chigh.max < r.Clow.min)
				GetReq(r, t, Sim.R_LOW);
			else
				GetMinR(r, t);
			return r;		// no driver, so just set Req
		}

		if (r.Rdown.min > r.Rup.max)
			GetReq(r, t, Sim.R_HIGH);
		else if (r.Rdown.max < r.Rup.min)
			GetReq(r, t, Sim.R_LOW);
		else
			GetMinR(r, t);

		double up_min = r.Rup.min;
		double down_min = r.Rdown.min;

		if (up_min < Sim.LIMIT)
			r.Rup.min += r.Req.min * (1.0 + up_min / r.Rdown.max);
		if (down_min < Sim.LIMIT)
			r.Rdown.min += r.Req.min * (1.0 + down_min / r.Rup.max);
		if ((r.flags & T_XTRAN) != 0)
		{
			r.flags &= ~T_DEFINITE;
			r.Rup.max = r.Rdown.max = Sim.LARGE;
		} else
		{
			if (r.Rup.max < Sim.LIMIT)
				r.Rup.max += r.Req.max * (1.0 + r.Rup.max / down_min);
			if (r.Rdown.max < Sim.LIMIT)
				r.Rdown.max += r.Req.max * (1.0 + r.Rdown.max / up_min);
		}
		return r;
	}

	/**
	 * make oldr = (oldr || newr), but watch out for infinte resistances.
	 */
	private double DoParallel(double oldr, double newr)
	{
		if (oldr > Sim.LIMIT) return newr;
		if (newr > Sim.LIMIT) return oldr;
		return Sim.COMBINE(oldr, newr);
	}

	/**
	 * Combine the new resistance block of the tree walk with the current one.
	 * Accumulate the low and high capacitances, the user-specified
	 * delay (if any), and the driven flag of the resulting structure.
	 */
	private void parallel_op(Sim.Thev r, Sim.Thev newrb)
	{
		r.Clow.max += newrb.Clow.max;
		r.Chigh.max += newrb.Chigh.max;
		if ((newrb.flags & T_XTRAN) == 0)
		{
			r.Clow.min += newrb.Clow.min;
			r.Chigh.min += newrb.Chigh.min;
		}

		/*
		* Accumulate the minimum user-specified delay only if the new block
		* has some drive associated with it.
		*/
		if ((newrb.flags & (T_DEFINITE | T_UDELAY)) == (T_DEFINITE | T_UDELAY))
		{
			if ((r.flags & T_UDELAY) != 0)
			{
				r.tplh = (short)Math.min(r.tplh, newrb.tplh);
				r.tphl = (short)Math.min(r.tphl, newrb.tphl);
			} else
			{
				r.tplh = newrb.tplh;
				r.tphl = newrb.tphl;
				r.flags |= T_UDELAY;
			}
		}

		if ((newrb.flags & T_DRIVEN) == 0)
			return;				// undriven, just update caps

		r.flags |= T_DRIVEN;		// combined result is driven

		r.Rup.min = DoParallel(r.Rup.min, newrb.Rup.min);
		r.Rdown.min = DoParallel(r.Rdown.min, newrb.Rdown.min);

		if ((r.flags & newrb.flags & T_DEFINITE) != 0)	// both definite blocks
		{
			r.Rup.max = DoParallel(r.Rup.max, newrb.Rup.max);
			r.Rdown.max = DoParallel(r.Rdown.max, newrb.Rdown.max);
		}
		else if ((newrb.flags & T_DEFINITE) != 0)		// only new is definite
		{
			r.Rup.max = newrb.Rup.max;
			r.Rdown.max = newrb.Rdown.max;
			r.flags |= T_DEFINITE;			    // result is definite
		} else				// new (perhaps r) is indefinite
		{
			if (newrb.Rup.max < r.Rup.max)	r.Rup.max = newrb.Rup.max;
			if (newrb.Rdown.max < r.Rdown.max) r.Rdown.max = newrb.Rdown.max;
		}
	}

	/**
	 * Determine the input time-constant (input-slope * rstatic).  We are only
	 * interseted in transistors that just turned on (its gate has a transition
	 * at time == Sched.irsim_cur_delta).  We must be careful not to report as a transition
	 * nodes that stop being inputs (hist.delay == 0 and hist.inp == 0).
	 */
	private boolean IsCurrTransition(Sim.HistEnt h)
	{
		return h.htime == theSim.irsim_cur_delta && (h.inp || h.delay != 0);
	}

	private boolean InputTau(Sim.Trans t, GenMath.MutableDouble pr)
	{
		return (t.tflags & Sim.PARALLEL) != 0 ? parallel_GetTin(t, pr) : GetTin(t, pr);
	}

	/**
	 * Return TRUE if we should consider the input slope of this transistor.  As
	 * a side-effect, return the input time constant in 'ptin'.
	 */
	private boolean GetTin(Sim.Trans t, GenMath.MutableDouble ptin)
	{
		Sim.HistEnt  h;
		boolean is_int = false;

		if (t.state != Sim.ON)
			return false;

		if ((t.ttype & Sim.GATELIST) == 0)
		{
			h = ((Sim.Node)t.gate).curr;
			if (IsCurrTransition(h))
			{
				ptin.setValue(h.rtime * t.r.rstatic);
				is_int = true;
			}
		} else
		{
			double  tmp = 0.0;

			for(t = (Sim.Trans) t.gate; t != null; t = t.getSTrans())
			{
				h = ((Sim.Node)t.gate).curr;
				if (IsCurrTransition(h))
				{
					is_int = true;
					tmp += h.rtime * t.r.rstatic;
				}
			}
			ptin.setValue(tmp);
		}
		return is_int;
	}

	private boolean parallel_GetTin(Sim.Trans t, GenMath.MutableDouble itau)
	{
		GenMath.MutableDouble tin = new GenMath.MutableDouble(0);
		boolean is_int = GetTin(t, tin);

		for(t = theSim.irsim_parallel_xtors[t.n_par]; t != null; t = t.getDTrans())
		{
			GenMath.MutableDouble tmp = new GenMath.MutableDouble(0);
			if (GetTin(t, tmp))
			{
				tin.setValue(is_int ? Sim.COMBINE_R(tin.doubleValue(), tmp.doubleValue()) : tmp.doubleValue());
				is_int = true;
			}
			itau.setValue(tin.doubleValue());
		}
		return is_int;
	}

	/**
	 * Compute the parameters needed to calculate the 1st order time-constants
	 * (Rmin, Rdom, Rmax, Ca, Cd, Tin) by doing a depth-first traversal of the
	 * tree rooted at node 'n'.  The parameters are gathered by performing a
	 * recursive tree walk similar to ComputeDC.  As a side effect, the tauP
	 * field will contain the multiplication factor to move a capacitor across
	 * a transistor using 'current distribution', this field may be required
	 * later when computing tauP.  The parameters are:
	 *
	 * n      : the node whose time-constant parameters we want.
	 * dom    : the value of the dominant driver for this stage.
	 * tran   : the transistor that leads to 'n' (null if none).
	 *
	 * This routine can be called more than once if the stage is dominated by
	 * more than 1 potential, hence the tau_done flag keeps track of the potential
	 * for which the parameters stored in the cache were computed.  If the flag
	 * value and the current dominant potential do not match, we go ahead and
	 * recompute the values.
	 */
	private Sim.Thev get_tau(Sim.Node n, Sim.Trans tran, int dom)
	{
		Sim.Thev r;
		if (tran == null)
			r = n.getThev();
		else
			r = (tran.source == n) ? tran.getSThev() : tran.getDThev();

		r.tau_done = (char)dom;

		if ((n.nflags & Sim.INPUT) != 0)
		{
			r.Tin = r.Rmin = r.Ca = r.Cd = 0.0;
			if (n.npot == dom)
			{
				r.Rdom = r.Rmax = 0.0;
				r.flags |= T_DOMDRIVEN;
			} else
			{
				r.flags &= ~(T_DOMDRIVEN | T_INT);
				if (dom == Sim.X)
					r.Rdom = r.Rmax = 0.0;
				else
					r.Rdom = r.Rmax = Sim.LARGE;
			}
			return r;
		}

		if ((n.getThev().flags & T_REFNODE) != 0)	    // reference node in pure CS
		{
			r.Rmin = r.Rdom = r.Rmax = 0.0;
			r.Ca = r.Cd = 0.0;
			return r;
		}

		r.Rmin = r.Rdom = r.Rmax = Sim.LARGE;
		r.Cd = n.ncap;
		if (dom == Sim.X)			// assume X nodes are charged high
		{
			r.Ca = (n.npot == Sim.LOW) ? 0.0 : n.ncap;
		} else
		{
			r.Ca = (n.npot == dom) ? 0.0 : n.ncap;
		}

		r.Tin = 0.0;
		r.flags &= ~(T_DOMDRIVEN | T_INT);
		for(Sim.Tlist l = n.nterm; l != null; l = l.next)
		{
			Sim.Trans t = l.xtor;
			if (t.state == Sim.OFF || t == tran || (t.tflags & (Sim.BROKEN | Sim.PBROKEN)) != 0)
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
			if (cache.tau_done != dom)
			{
				GenMath.MutableDouble  oldr = new GenMath.MutableDouble(0);

				cache = get_tau(other, t, dom);
				// Only use input slope for xtors on the dominant (driven) path
				if ((cache.flags & T_DOMDRIVEN) != 0 && InputTau(t, oldr))
				{
					cache.flags |= T_INT;
					cache.Tin += oldr.doubleValue();
				}

				oldr.setValue(cache.Rdom);

				cache.Rmin += cache.Req.min;
				cache.Rdom += cache.Req.min;
				if ((cache.flags & T_XTRAN) != 0)
				{
					cache.Rmax = Sim.LARGE;
				} else
				{
					cache.Rmax += cache.Req.max;
				}

				// Exclude capacitors if the other side of X transistor == dom
				if ((cache.flags & T_XTRAN) != 0 && other.npot == dom)
					cache.tauP = cache.Ca = cache.Cd = 0.0;
				else if (oldr.doubleValue() > Sim.LIMIT)
					cache.tauP = 1.0;
				else
				{
					cache.tauP = oldr.doubleValue() / cache.Rdom;
					cache.Ca *= cache.tauP;
					cache.Cd *= cache.tauP;
				}
			}

			r.Ca += cache.Ca;
			r.Cd += cache.Cd;
			r.Rmin = Sim.COMBINE(r.Rmin, cache.Rmin);
			if (r.Rdom > Sim.LIMIT)
			{
				r.Rdom = cache.Rdom;
				r.Rmax = cache.Rmax;
			}
			else if (cache.Rdom < Sim.LIMIT)
			{
				r.Rdom = Sim.COMBINE(r.Rdom, cache.Rdom);
				r.Rmax = Sim.COMBINE(r.Rmax, cache.Rmax);
			}
			if ((cache.flags & T_DOMDRIVEN) != 0)
				r.flags |= T_DOMDRIVEN;	// at least 1 dominant driven path

			if ((cache.flags & T_INT) != 0)
			{
				if ((r.flags & T_INT) != 0)
					r.Tin = Sim.COMBINE_R(r.Tin, cache.Tin);
				else
				{
					r.Tin = cache.Tin;
					r.flags |= T_INT;
				}
			}
		}

		return r;
	}

	/**
	 * Calculate the 2nd order time constant (tauP) for the net configuration
	 * as seen through node 'n'.  The net traversal and the parameters are
	 * similar to 'get_tau'.  Note that at this point we have not have calculated
	 * tauA for nodes not driven to the dominant potential, hence we need to
	 * compute those by first calling get_tau.  This routine will update the tauP
	 * entry as well as the taup_done flag.
	 */
	private double get_tauP(Sim.Node n, Sim.Trans tran, int dom)
	{
		if ((n.nflags & Sim.INPUT) != 0)
			return 0.0;

		Sim.Thev r = n.getThev();
		if (r.tau_done != dom)		// compute tauA for the node
		{
			r = get_tau(n, (Sim.Trans) null, dom);
			r.tauA = r.Rdom * r.Ca;
			r.tauD = r.Rdom * r.Cd;
		}

		double taup = r.tauA * n.ncap;

		for(Sim.Tlist l = n.nterm; l != null; l = l.next)
		{
			Sim.Trans t = l.xtor;
			if (t.state == Sim.OFF || t == tran || (t.tflags & (Sim.BROKEN | Sim.PBROKEN)) != 0)
				continue;

			Sim.Node other;
			if (t.source == n)
			{
				other = t.drain;	r = t.getDThev();
			} else
			{
				other = t.source;	r = t.getSThev();
			}

			if (r.taup_done != dom)
			{
				r.tauP *= get_tauP(other, t, dom);
				r.taup_done = (char)dom;
			}
			taup += r.tauP;
		}

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
	private SpikeRec ComputeSpike(Sim.Node nd, Sim.Thev r, int dom)
	{
		if (r.tauP <= Sim.SMALL)		// no capacitance, no spike
		{
			return null;
		}

		int rtype = (dom == Sim.LOW) ? Sim.R_LOW : Sim.R_HIGH;
		float nmos = 0, pmos = 0;
		for(Sim.Tlist l = nd.nterm; l != null; l = l.next)
		{
			Sim.Trans t = l.xtor;
			if (t.state == Sim.OFF || (t.tflags & Sim.BROKEN) != 0)
				continue;
			if (Sim.BASETYPE(t.ttype) == Sim.PCHAN)
				pmos += 1.0f / t.r.dynres[rtype];
			else
				nmos += 1.0f / t.r.dynres[rtype];
		}
		int tab_indx = 0;
		if (nmos > NP_RATIO * (pmos + nmos))		// mostly nmos
			tab_indx = (rtype == Sim.R_LOW) ? NLSPKMIN : NLSPKMAX;
		else if (pmos > NP_RATIO * (pmos + nmos))		// mostly pmos
			tab_indx = (rtype == Sim.R_LOW) ? NLSPKMAX : NLSPKMIN;
		else
			tab_indx = LINEARSPK;

		int alpha = (int) (SPIKETBLSIZE * r.tauA / (r.tauA + r.tauP - r.tauD));
		if (alpha < 0)
			alpha = 0;
		else if (alpha > SPIKETBLSIZE)
			alpha = SPIKETBLSIZE;

		int beta = (int) (SPIKETBLSIZE * (r.tauD - r.tauA) / r.tauD);
		if (beta < 0)
			beta = 0;
		else if (beta > SPIKETBLSIZE)
			beta = SPIKETBLSIZE;

		SpikeRec spk = new SpikeRec();
		spk.peak = spikeTable[tab_indx][beta][alpha];
		spk.ch_delay = delayTable[beta][alpha];

		if (dom == Sim.LOW)
		{
			if (spk.peak <= nd.vlow)		// spike is too small
			{
				return null;
			}
			spk.charge = (spk.peak >= nd.vhigh) ? Sim.HIGH : Sim.X;
		}
		else	// dom == HIGH
		{
			if (spk.peak <= 1.0 - nd.vhigh) return null;
			spk.charge = (spk.peak >= 1.0 - nd.vlow) ? Sim.LOW : Sim.X;
		}

		spk.ch_delay *= r.tauA * r.tauD / r.tauP;

		if (r.Rmax < Sim.LARGE)
			spk.dr_delay = r.Rmax * r.Ca;
		else
			spk.dr_delay = r.Rdom * r.Ca;

		return spk;
	}

	/**
	 * Initialize pre-initialized thevenin structs.  I want to get it right
	 * and this is much safer than letting the compiler initialize it.
	 */
	private void irsim_InitThevs()
	{
		init_thev = new Sim.Thev();		/* pre-initialized thevenin structs */
		input_thev = new Sim.Thev[Sim.N_POTS];
		for(int i=0; i<Sim.N_POTS; i++) input_thev[i] = new Sim.Thev();

		init_thev.setN(null);
		init_thev.flags		= 0;
		init_thev.Clow.min	= 0.0;
		init_thev.Clow.max	= 0.0;
		init_thev.Chigh.min	= 0.0;
		init_thev.Chigh.max	= 0.0;
		init_thev.Rup.min	= Sim.LARGE;
		init_thev.Rup.max	= Sim.LARGE;
		init_thev.Rdown.min	= Sim.LARGE;
		init_thev.Rdown.max	= Sim.LARGE;
		init_thev.Req.min	= Sim.LARGE;
		init_thev.Req.max	= Sim.LARGE;
		init_thev.V.min		= 1.0;
		init_thev.V.max		= 0.0;
		init_thev.Rmin		= Sim.LARGE;
		init_thev.Rdom		= Sim.LARGE;
		init_thev.Rmax		= Sim.LARGE;
		init_thev.Ca		= 0.0;
		init_thev.Cd		= 0.0;
		init_thev.tauD		= 0.0;
		init_thev.tauA		= 0.0;
		init_thev.tauP		= 0.0;
		init_thev.Tin		= Sim.SMALL;
		init_thev.tplh		= 0;
		init_thev.tphl		= 0;
		init_thev.finall	= Sim.X;
		init_thev.tau_done	= Sim.N_POTS;
		init_thev.taup_done	= Sim.N_POTS;

		Sim.Thev t =	input_thev[Sim.LOW];
		t.setN(null);
		t.flags		= T_DEFINITE | T_DRIVEN;
		t.Clow.min	= 0.0;
		t.Clow.max	= 0.0;
		t.Chigh.min	= 0.0;
		t.Chigh.max	= 0.0;
		t.Rup.min	= Sim.LARGE;
		t.Rup.max	= Sim.LARGE;
		t.Rdown.min	= Sim.SMALL;
		t.Rdown.max	= Sim.SMALL;
		t.Req.min	= Sim.LARGE;
		t.Req.max	= Sim.LARGE;
		t.V.min		= 0.0;
		t.V.max		= 0.0;
		t.Rmin		= Sim.SMALL;
		t.Rdom		= Sim.LARGE;
		t.Rmax		= Sim.LARGE;
		t.Ca		= 0.0;
		t.Cd		= 0.0;
		t.tauD		= 0.0;
		t.tauA		= 0.0;
		t.tauP		= 0.0;
		t.Tin		= Sim.SMALL;
		t.tplh		= 0;
		t.tphl		= 0;
		t.finall	= Sim.LOW;
		t.tau_done	= Sim.N_POTS;
		t.taup_done	= Sim.N_POTS;

		t = input_thev[Sim.HIGH];
		t.setN(null);
		t.flags		= T_DEFINITE | T_DRIVEN;
		t.Clow.min	= 0.0;
		t.Clow.max	= 0.0;
		t.Chigh.min	= 0.0;
		t.Chigh.max	= 0.0;
		t.Rup.min	= Sim.SMALL;
		t.Rup.max	= Sim.SMALL;
		t.Rdown.min	= Sim.LARGE;
		t.Rdown.max	= Sim.LARGE;
		t.Req.min	= Sim.LARGE;
		t.Req.max	= Sim.LARGE;
		t.V.min		= 1.0;
		t.V.max		= 1.0;
		t.Rmin		= Sim.SMALL;
		t.Rdom		= Sim.LARGE;
		t.Rmax		= Sim.LARGE;
		t.Ca		= 0.0;
		t.Cd		= 0.0;
		t.tauD		= 0.0;
		t.tauA		= 0.0;
		t.tauP		= 0.0;
		t.Tin		= Sim.SMALL;
		t.tplh		= 0;
		t.tphl		= 0;
		t.finall	= Sim.HIGH;
		t.tau_done	= Sim.N_POTS;
		t.taup_done	= Sim.N_POTS;

		t = input_thev[Sim.X];
		t.setN(null);
		t.flags		= T_DEFINITE | T_DRIVEN;
		t.Clow.min	= 0.0;
		t.Clow.max	= 0.0;
		t.Chigh.min	= 0.0;
		t.Chigh.max	= 0.0;
		t.Rup.min	= Sim.SMALL;
		t.Rup.max	= Sim.LARGE;
		t.Rdown.min	= Sim.SMALL;
		t.Rdown.max	= Sim.LARGE;
		t.Req.min	= Sim.LARGE;
		t.Req.max	= Sim.LARGE;
		t.V.min		= 1.0;
		t.V.max		= 0.0;
		t.Rmin		= Sim.SMALL;
		t.Rdom		= Sim.LARGE;
		t.Rmax		= Sim.LARGE;
		t.Ca		= 0.0;
		t.Cd		= 0.0;
		t.tauD		= 0.0;
		t.tauA		= 0.0;
		t.tauP		= 0.0;
		t.Tin		= Sim.SMALL;
		t.tplh		= 0;
		t.tphl		= 0;
		t.finall	= Sim.X;
		t.tau_done	= Sim.N_POTS;
		t.taup_done	= Sim.N_POTS;
	
		input_thev[Sim.X+1] = input_thev[Sim.X];
	}
}
