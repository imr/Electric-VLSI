/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Config.java
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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.URL;
import java.net.URLConnection;

public class Config
{	
	/*
	 * electrical parameters used for deriving capacitance info for charge
	 * sharing.  Default values aren't for any particular process, but are
	 * self-consistent.
	 *	Area capacitances are all in pfarads/sq-micron units.
	 *	Perimeter capacitances are all in pfarads/micron units.
	 */
	static double  irsim_CM2A = .00000;	/* 2nd metal capacitance -- area */
	static double  irsim_CM2P = .00000;	/* 2nd metal capacitance -- perimeter */
	static double  irsim_CMA  = .00003;	/* 1st metal capacitance -- area */
	static double  irsim_CMP  = .00000;	/* 1st metal capacitance -- perimeter */
	static double  irsim_CPA  = .00004;	/* poly capacitance -- area */
	static double  irsim_CPP  = .00000;	/* poly capacitance -- perimeter */
	static double  irsim_CDA  = .00010;	/* n-diffusion capacitance -- area */
	static double  irsim_CDP  = .00060;	/* n-diffusion capacitance -- perimeter */
	static double  irsim_CPDA = .00010;	/* p-diffusion capacitance -- area */
	static double  irsim_CPDP = .00060;	/* p-diffusion capacitance -- perimeter */
	static double  irsim_CGA  = .00040;	/* gate capacitance -- area */
	
					/* the following are computed from above */
	static double	irsim_CTDW;				/* xtor diff-width capacitance -- perimeter */
	static double	irsim_CPTDW;		
	static double	irsim_CTDE;				/* xtor diff-extension cap. -- perimeter */
	static double	irsim_CPTDE;	
	static double	irsim_CTGA;				/* xtor gate capacitance -- area */

	static double  irsim_LAMBDA     = 2.5;		/* microns/lambda */
	static double  irsim_LAMBDA2    = 6.25;		/* LAMBDA**2 */
	static long    irsim_LAMBDACM   = 250;		/* centi-microns/lambda */
	static double  irsim_LOWTHRESH  = 0.3;		/* low voltage threshold, normalized units */
	static double  irsim_HIGHTHRESH = 0.8;		/* high voltage threshold,normalized units */
	static double  irsim_DIFFEXT    = 0;		/* width of source/drain diffusion */
	
	static int     irsim_config_flags;
	
	public static final double	CM_M	= 100.0;		/* centimicrons per micron */
	
	/* values of irsim_config_flags */
	
	public static final int	CNTPULLUP	= 0x2;		/* set if capacitance from gate of pullup */
								/* should be included. */
	
	public static final int	DIFFPERIM	= 0x4;		/* set if diffusion perimeter does not */
								/* include sources/drains of transistors. */
	
	public static final int	SUBPAREA	= 0x8;		/* set if poly over xistor doesn't make a capacitor */
	
	public static final int	DIFFEXTF	= 0x10;	/* set if we should add capacitance due to */
								/* diffusion-extension of source/drain. */

	public static final int	TDIFFCAP	= 0x1;	/* set if DIFFPERIM or DIFFEXTF are true    */


	static	int     nerrs;		/* errors found in config file */
	static	int     maxerr;
	static	String	[] ttype_drop = new String[Sim.NTTYPES];
	
	
	/*
	 * info on resistance vs. width and length are stored first sorted by
	 * width, then by length.
	 */
	static class Length
	{
		Length    next;		/* next element with same width */
		long      l;		/* length of this channel in centimicrons */
		double    r;		/* equivalent resistance/square */
	};
	
	static class Width
	{
		Width     next;		/* next width */
		long      w;		/* width of this channel in centimicrons */
		Length    list;		/* list of length structures */
	}
	static Width [][] resistances = new Width[Sim.R_TYPES][Sim.NTTYPES];
	
	
	/* linear interpolation, assume that x1 < x <= x2 */
	static double interp(double x, double x1, double y1, double x2, double y2)
	{
		return ((x - x1) / (x2 - x1)) * (y2 - y1) + y1;
	}

	
	static class ResEntry
	{
		ResEntry    r_next;
		Sim.Resists   r;
	};

	static final int	RES_TAB_SIZE	= 67;
	static ResEntry    [][] irsim_res_htab = new ResEntry[Sim.NTTYPES][];
	static boolean first = true;
	
	/*
	 * Routine to free all memory allocated in this module.
	 */
	static void irsim_freeconfigmemory()
	{
		if (first)
		{
			for(int i=0; i<Sim.NTTYPES; i++) irsim_res_htab[i] = null;
			irsim_config_flags = 0;
			first = false;
			nerrs = 0;
			for(int t = 0; t < Sim.NTTYPES; t++)
				for(int c = 0; c < Sim.R_TYPES; c++)
					resistances[c][t] = null;
			return;
		}
	
		// deallocate
		for(int i=0; i<Sim.NTTYPES; i++)
		{
			ResEntry [] rtab = irsim_res_htab[i];
			if (rtab != null)
			{
				for(int n = 0; n < RES_TAB_SIZE; n++)
				{
					while (rtab[n] != null)
					{
						ResEntry r = rtab[n];
						rtab[n] = r.r_next;
//						efree((CHAR *)r);
					}
				}
//				efree((CHAR *)irsim_res_htab[i]);
				irsim_res_htab[i] = null;
			}
		}
		for(int t = 0; t < Sim.NTTYPES; t++)
		{
			for(int c = 0; c < Sim.R_TYPES; c++)
			{
				Width w = resistances[c][t];
				while (resistances[c][t] != null)
				{
					w = resistances[c][t];
					resistances[c][t] = w.next;
					while (w.list != null)
					{
						Length l = w.list;
						w.list = l.next;
//						efree((CHAR *)l);
					}
//					efree((CHAR *)w);
				}
			}
		}
	}	
	
	static int irsim_config(URL configURL)
	{
		for(int i = 0; i < Sim.NTTYPES; i++)
			ttype_drop[i] = Sim.irsim_ttype[i] + "-with-drop";
		
		String fileName = configURL.getFile();
		InputStream inputStream = null;
		try
		{
			URLConnection urlCon = configURL.openConnection();
			inputStream = urlCon.getInputStream();
			InputStreamReader is = new InputStreamReader(inputStream);
			LineNumberReader lineReader = new LineNumberReader(is);
			for(;;)
			{
				String line = lineReader.readLine();
				if (line == null) break;
				if (line.startsWith("; configuration file"))
				{
//					rewind(cfile);
					maxerr = 1;
				}
				else
					maxerr = 15;
				if (line.startsWith(";")) continue;
	
				String [] targ = Sim.parse_line(line, false);
				if (targ.length == 0) continue;
				if (targ[0].equals("resistance"))
				{
					if (targ.length >= 6)
						insert(fileName, lineReader.getLineNumber(), targ[1], targ[2], targ[3], targ[4], targ[5]);
					else
					{
						Sim.irsim_error(fileName, lineReader.getLineNumber(), "syntax error in resistance spec");
						nerrs++;
					}
					continue;
				} else
				{
					if (targ[0].equals("capm2a")) irsim_CM2A = TextUtils.atof(targ[1]); else
					if (targ[0].equals("capm2p")) irsim_CM2P = TextUtils.atof(targ[1]); else
					if (targ[0].equals("capma")) irsim_CMA = TextUtils.atof(targ[1]); else
					if (targ[0].equals("capmp")) irsim_CMP = TextUtils.atof(targ[1]); else
					if (targ[0].equals("cappa")) irsim_CPA = TextUtils.atof(targ[1]); else
					if (targ[0].equals("cappp")) irsim_CPP = TextUtils.atof(targ[1]); else
					if (targ[0].equals("capda")) irsim_CDA = TextUtils.atof(targ[1]); else
					if (targ[0].equals("capdp")) irsim_CDP = TextUtils.atof(targ[1]); else
					if (targ[0].equals("cappda")) irsim_CPDA = TextUtils.atof(targ[1]); else
					if (targ[0].equals("cappdp")) irsim_CPDP = TextUtils.atof(targ[1]); else
					if (targ[0].equals("capga")) irsim_CGA = TextUtils.atof(targ[1]); else
					if (targ[0].equals("lambda")) irsim_LAMBDA = TextUtils.atof(targ[1]); else
					if (targ[0].equals("lowthresh")) irsim_LOWTHRESH = TextUtils.atof(targ[1]); else
					if (targ[0].equals("highthresh")) irsim_HIGHTHRESH = TextUtils.atof(targ[1]); else
					if (targ[0].equals("diffperim"))
					{
						if (TextUtils.atof(targ[1]) != 0.0) irsim_config_flags |= DIFFPERIM;
					} else if (targ[0].equals("cntpullup"))
					{
						if (TextUtils.atof(targ[1]) != 0.0) irsim_config_flags |= CNTPULLUP;
					} else if (targ[0].equals("subparea"))
					{
						if (TextUtils.atof(targ[1]) != 0.0) irsim_config_flags |= SUBPAREA;
					} else if (targ[0].equals("diffext"))
					{
						if (TextUtils.atof(targ[1]) != 0.0)
						{
							irsim_DIFFEXT = TextUtils.atof(targ[1]);
							irsim_config_flags |= DIFFEXTF;
						}
					} else
					{
						Sim.irsim_error(fileName, lineReader.getLineNumber(), "unknown electrical parameter: (" + targ[0] + ")");
						nerrs++;
					}
				}
				if (nerrs >= maxerr)
				{
					if (maxerr == 1)
						System.out.println("I think " + fileName + " is not an electrical parameters file");
					else
						System.out.println("Too many errors in '" + fileName + "'");
					return(1);
				}
			}
			inputStream.close();
		} catch (IOException e)
		{
			System.out.println("Error reading electrical parameters file");
		}
		irsim_LAMBDA2 = irsim_LAMBDA * irsim_LAMBDA;
		irsim_LAMBDACM = (long)(irsim_LAMBDA * CM_M);
		irsim_CTGA = ((irsim_config_flags & SUBPAREA) != 0 ? (irsim_CGA - irsim_CPA) : irsim_CGA) / (CM_M * CM_M);
		switch(irsim_config_flags & (DIFFEXTF | DIFFPERIM))
		{
			case 0:
				irsim_CTDE = irsim_CTDW = 0.0; irsim_CPTDE = irsim_CPTDW = 0.0;	
				break;
				case DIFFPERIM:
				irsim_config_flags |= TDIFFCAP;
				irsim_CTDE = irsim_CPTDE = 0.0;
				irsim_CTDW = -(irsim_CDP / CM_M);
				irsim_CPTDW = -(irsim_CPDP / CM_M);
				break;
			case DIFFEXTF:
				irsim_config_flags |= TDIFFCAP;
				irsim_CTDE = (2 * irsim_DIFFEXT * irsim_LAMBDA * irsim_CDP);
				irsim_CPTDE = (2 * irsim_DIFFEXT * irsim_LAMBDA * irsim_CPDP);
				irsim_CTDW = (irsim_CDP + irsim_DIFFEXT * irsim_LAMBDA * irsim_CDA) / CM_M;
				irsim_CPTDW = (irsim_CPDP + irsim_DIFFEXT * irsim_LAMBDA * irsim_CPDA) / CM_M;
				break;
			case (DIFFEXTF | DIFFPERIM):
				irsim_config_flags |= TDIFFCAP;
				irsim_CTDE = (2 * irsim_DIFFEXT * irsim_LAMBDA * irsim_CDP);
				irsim_CPTDE = (2 * irsim_DIFFEXT * irsim_LAMBDA * irsim_CPDP);
				irsim_CTDW = (irsim_DIFFEXT * irsim_LAMBDA * irsim_CDA) / CM_M;
				irsim_CPTDW = (irsim_DIFFEXT * irsim_LAMBDA * irsim_CPDA) / CM_M;
				break;
		}
	
		if ((irsim_config_flags & CNTPULLUP) != 0)
			System.out.println("warning: cntpullup is not supported");

		return(0);
	}
	
	
	/*
	 * given a list of length structures, sorted by incresing length return
	 * resistance of given channel.  If no exact match, return result of
	 * linear interpolation using two closest channels.
	 */
	static double lresist(Length list, long l, double size)
	{
		Length q = null;
		for(Length p = list; p != null; q = p, p = p.next)
		{
			if (p.l == l ||(p.l > l && q == null))
				return(p.r * size);
			if (p.l > l)
				return(size * interp(l, q.l, q.r, p.l, p.r));
		}
		if (q != null)
			return(q.r *size);
		return(1E4 * size);
	}
	
	
	/*
	 * given a pointer to the width structures for a particular type of
	 * channel compute the resistance for the specified channel.
	 */
	static double wresist(Width list, long w, long l)
	{
		double size = ((double) l) / ((double) w);

		Width q = null;
		for(Width p = list; p != null; q = p, p = p.next)
		{
			if (p.w == w ||(p.w > w && q == null))
				return(lresist(p.list, l, size));
			if (p.w > w)
			{
				double temp = lresist(q.list, l, size);
				return(interp(w, q.w, temp, p.w, lresist(p.list, l, size)));
			}
		}
		if (q != null)
			return(lresist(q.list, l, size));
		return(1E4 * size);
	}
	
	
	/*
	 * Compute equivalent resistance given width, length and type of transistor.
	 * for all contexts (STATIC, DYNHIGH, DYNLOW).  Place the result on the
	 * transistor 
	 */
	static Sim.Resists irsim_requiv(int type, long width, long length)
	{
		type = Sim.BASETYPE(type);
	
		ResEntry [] rtab = irsim_res_htab[type];
		if (rtab == null)
		{
			rtab = new ResEntry[RES_TAB_SIZE];
			for(int n = 0; n < RES_TAB_SIZE; n++) rtab[n] = null;
			irsim_res_htab[type] = rtab;
		}
		int n = (int)(Math.abs(length * 110133 + width) % RES_TAB_SIZE);
		ResEntry r = null;
		for(r = rtab[n]; r != null; r = r.r_next)
		{
			if ((long)r.r.length == length && (long)r.r.width == width) return(r.r);
		}
	
		r = new ResEntry();
		r.r = new Sim.Resists();
		r.r_next = rtab[n];
		rtab[n] = r;
	
		r.r.length = length;
		r.r.width = width;
	
		if (type == Sim.RESIST)
		{
			r.r.dynres[Sim.R_LOW] = r.r.dynres[Sim.R_HIGH] = r.r.rstatic = (float) length / irsim_LAMBDACM;
		} else
		{
			r.r.rstatic = (float)wresist(resistances[Sim.STATIC][type], width, length);
			r.r.dynres[Sim.R_LOW] = (float)wresist(resistances[Sim.DYNLOW][type], width, length);
			r.r.dynres[Sim.R_HIGH] = (float)wresist(resistances[Sim.DYNHIGH][type], width, length);
		}
		return r.r;
	}
	
	
	static Length linsert(Length list, long l, double resist)
	{
		Length ret = list;
		Length p = list, q = null;
		for( ; p != null; q = p, p = p.next)
		{
			if (p.l == l)
			{
				p.r = resist;
				return ret;
			}
			if (p.l > l) break;
		}
		Length lnew = new Length();
		lnew.next = p;
		lnew.l = l;
		lnew.r = resist;
		if (q == null)
			ret = lnew;
		else
			q.next = lnew;
		return ret;
	}
	
	
	/* add a new data point to the interpolation array */
	static void winsert(int c, int t, long w, long l, double resist)
	{
		Width list = resistances[c][t];
	
		Width p = list, q = null;
		for( ; p != null; q = p, p = p.next)
		{
			if (p.w == w)
			{
				p.list = linsert(p.list, l, resist);
				return;
			}
			if (p.w > w) break;
		}
		Width wnew = new Width();
		Length lnew = new Length();
		wnew.next = p;
		wnew.list = lnew;
		wnew.w = w;
		if (q == null)
			resistances[c][t] = wnew;
		else
			q.next = wnew;
		lnew.next = null;
		lnew.l = l;
		lnew.r = resist;
	}
	
	
	/* interpret resistance specification command */
	static void insert(String fileName, int lineNo, String type, String context, String w, String l, String r)
	{
		long width = (long)(TextUtils.atof(w) * CM_M);
		long length = (long)(TextUtils.atof(l) * CM_M);
		double resist = TextUtils.atof(r);
		if (width <= 0 || length <= 0 || resist <= 0)
		{
			Sim.irsim_error(fileName, lineNo, "bad w, l, or r in config file");
			nerrs++;
			return;
		}
	
		int c = 0;
		if (context.equalsIgnoreCase("static"))
			c = Sim.STATIC;
		else if (context.equalsIgnoreCase("dynamic-high"))
			c = Sim.DYNHIGH;
		else if (context.equalsIgnoreCase("dynamic-low"))
			c = Sim.DYNLOW;
		else if (context.equalsIgnoreCase("power"))
			c = Sim.POWER;
		else
		{
			Sim.irsim_error(fileName, lineNo, "bad resistance context in config file");
			nerrs++;
			return;
		}
	
		for(int t = 0; t < Sim.NTTYPES; t++)
		{
			if (Sim.irsim_ttype[t].equalsIgnoreCase(type))
			{
				if (c == Sim.POWER)
					return;
				winsert(c, t, width, length, resist*width/length);
				return;
			}
			else if (ttype_drop[t].equalsIgnoreCase(type))
				return;
		}
	
		Sim.irsim_error(fileName, lineNo, "bad resistance transistor type");
		nerrs++;
	}
}
