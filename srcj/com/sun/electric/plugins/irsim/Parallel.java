/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Parallel.java
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

public class Parallel
{
	static int  []  nored = new int[Sim.NTTYPES];
	
	/*
	 * Run through the list of nodes, collapsing all transistors with the same
	 * gate/source/drain into a compound transistor.
	 */
	public static void irsim_make_parallel(Sim.Node nlist)
	{
		long cl = Sim.VISITED;
		for(cl = ~cl; nlist != null; nlist.nflags &= cl, nlist = nlist.getNext())
		{
			for(Sim.Tlist l1 = nlist.nterm; l1 != null; l1 = l1.next)
			{
				Sim.Trans t1 = l1.xtor;
				int type = t1.ttype;
				if ((type & (Sim.GATELIST | Sim.ORED)) != 0)
					continue;	// ORED implies processed, so skip as well
	
				long hval = Sim.hash_terms(t1);
				Sim.Tlist prev = l1;
				for(Sim.Tlist l2 = l1.next; l2 != null; prev = l2, l2 = l2.next)
				{
					Sim.Trans t2 = l2.xtor;
					if (t1.gate != t2.gate || Sim.hash_terms(t2) != hval || 
						type != (t2.ttype & ~Sim.ORED))
							continue;
	
					if ((t1.ttype & Sim.ORED) == 0)
					{
						t2 = new Sim.Trans();
						t2.r = (Sim.Resists)new Sim.TranResist();
						t2.r.dynres[Sim.R_LOW] = t1.r.dynres[Sim.R_LOW];
						t2.r.dynres[Sim.R_HIGH] = t1.r.dynres[Sim.R_HIGH];
						t2.r.rstatic = t1.r.rstatic;
						t2.gate = t1.gate;
						t2.source = t1.source;
						t2.drain = t1.drain;
						t2.ttype = (byte)((t1.ttype & ~Sim.ORLIST) | Sim.ORED);
						t2.state = t1.state;
						t2.tflags = t1.tflags;
						t2.tlink = t1;
						t1.setSTrans(null);
						t1.setDTrans(t2);
						REPLACE(((Sim.Node)t1.gate).ngate, t1, t2);
						REPLACE(t1.source.nterm, t1, t2);
						REPLACE(t1.drain.nterm, t1, t2);
						t1.ttype |= Sim.ORLIST;
						t1 = t2;
						t2 = l2.xtor;
						nored[Sim.BASETYPE(t1.ttype)]++;
					}
	
					{
						Sim.Resists  r1 = t1.r, r2 = t2.r;
	
						r1.rstatic = (float)NewRStep.COMBINE(r1.rstatic, r2.rstatic);
						r1.dynres[Sim.R_LOW] = (float)NewRStep.COMBINE(r1.dynres[Sim.R_LOW], r2.dynres[Sim.R_LOW]);
						r1.dynres[Sim.R_HIGH] = (float)NewRStep.COMBINE(r1.dynres[Sim.R_HIGH], r2.dynres[Sim.R_HIGH]);
					}
	
					((Sim.Node)t2.gate).ngate = Sim.DISCONNECT(((Sim.Node)t2.gate).ngate, t2);	// disconnect gate
					if (t2.source == nlist)		// disconnect term1
						t2.drain.nterm = Sim.DISCONNECT(t2.drain.nterm, t2);
					else
						t2.source.nterm = Sim.DISCONNECT(t2.source.nterm, t2);
	
					prev.next = l2.next;			// disconnect term2
					FREE_LINK(l2);
					l2 = prev;
	
					if ((t2.ttype & Sim.ORED) != 0)
					{
						Sim.Trans  t;
	
						for(t = t2.tlink; t.getSTrans() != null; t = t.getSTrans())
							t.setDTrans(t1);
						t.setSTrans(t1.tlink);
						t1.tlink = t2.tlink;
	
//						efree(t2.r);
						Sim.FREE_TRANS(t2);
					} else
					{
						t2.ttype |= Sim.ORLIST;	// mark as part of or
						t2.setDTrans(t1);		// this is the real txtor
						t2.setSTrans(t1.tlink);	// link unto t1 list
						t1.tlink = t2;
						nored[Sim.BASETYPE(t1.ttype)]++;
					}
				}
			}
		}
	}

	/*
	 * Return "Tlist" pointer LP to free pool.
	 */
	static void FREE_LINK(Sim.Tlist lp)
	{
		lp.next = Sim.irsim_freeLinks;
		Sim.irsim_freeLinks = lp;
	}

	/*
	 * Replace the first ocurrence of transistor "oldT" by "newT" on "list".
	 */
	static void REPLACE(Sim.Tlist list, Sim.Trans oldT, Sim.Trans newT)
	{
		for(Sim.Tlist lp = list; lp != null; lp = lp.next)
		{
			if (lp.xtor == oldT)
			{
				lp.xtor = newT;
				break;
			}
		}
	}


	
	static void irsim_UnParallelTrans(Sim.Trans t)
	{
		if ((t.ttype & Sim.ORLIST) == 0) return;				// should never be
	
		Sim.Trans tor = t.getDTrans();
		if (tor.tlink == t)
			tor.tlink = t.getSTrans();
		else
		{
			Sim.Trans  tp;
	
			for(tp = tor.tlink; tp != null; tp = tp.getSTrans())
			{
				if (tp.getSTrans() == t)
				{
					tp.setSTrans(t.getSTrans());
					break;
				}
			}
		}
	
		if (tor.tlink == null)
		{
			REPLACE(((Sim.Node)tor.gate).ngate, tor, t);
			REPLACE(tor.source.nterm, tor, t);
			REPLACE(tor.drain.nterm, tor, t);
//			efree(tor.r);
			Sim.FREE_TRANS(tor);
		} else
		{
			Sim.Resists ror = tor.r;
			Sim.Resists r = t.r;
	
			double dr = r.rstatic - ror.rstatic;
			ror.rstatic = (float)((ror.rstatic * r.rstatic) / dr);
			dr = r.dynres[Sim.R_LOW] - ror.dynres[Sim.R_LOW];
			ror.dynres[Sim.R_LOW] = (float)((ror.dynres[Sim.R_LOW] * r.dynres[Sim.R_LOW]) / dr);
			dr = r.dynres[Sim.R_HIGH] - ror.dynres[Sim.R_HIGH];
			ror.dynres[Sim.R_HIGH] = (float)((ror.dynres[Sim.R_HIGH] * r.dynres[Sim.R_HIGH]) / dr);
	
			if ((t.ttype & Sim.ALWAYSON) != 0)
			{
				Sim.irsim_on_trans = Sim.CONNECT(Sim.irsim_on_trans, t);
			} else
			{
				((Sim.Node)t.gate).ngate = Sim.CONNECT(((Sim.Node)t.gate).ngate, t);
			}
			if ((t.source.nflags & Sim.POWER_RAIL) == 0)
			{
				t.source.nterm = Sim.CONNECT(t.source.nterm, t);
			}
			if ((t.drain.nflags & Sim.POWER_RAIL) == 0)
			{
				t.drain.nterm = Sim.CONNECT(t.drain.nterm, t);
			}
		}
		t.ttype &= ~Sim.ORLIST;
		nored[Sim.BASETYPE(t.ttype)] -= 1;
	}
	
	
	static void irsim_pParallelTxtors()
	{
		String str = "Parallel txtors:";
		boolean any = false;
		for(int i = 0; i < Sim.NTTYPES; i++)
		{
			if (nored[i] != 0)
			{
				str += " " + Sim.irsim_ttype[i] + "=" + nored[i];
				any = true;
			}
		}
		if (any) System.out.println(str);
	}
}
