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

import com.sun.electric.database.text.TextUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Config
{
	/*
	 * info on resistance vs. width and length are stored first sorted by
	 * width, then by length.
	 */
	private static class Length
	{
		/** next element with same width */				Length    next;		
		/** length of this channel in centimicrons */	long      l;		
		/** equivalent resistance/square */				double    r;		
	};

	private static class Width
	{
		/** next width */								Width     next;		
		/** width of this channel in centimicrons */	long      w;		
		/** list of length structures */				Length    list;		
	}

	/** centimicrons per micron */														private static final double CM_M	  = 100.0;	

	/* values of irsim_config_flags */
	/** set if capacitance from gate of pullup should be included. */					private static final int CNTPULLUP	= 0x2;		
	/** set if diffusion perimeter does not include sources/drains of transistors. */	private static final int DIFFPERIM	= 0x4;		
	/** set if poly over xistor doesn't make a capacitor */								private static final int SUBPAREA	= 0x8;		
	/** set if we should add capacitance due to diffusion-extension of source/drain. */	private static final int DIFFEXTF	= 0x10;		
	/** set if DIFFPERIM or DIFFEXTF are true */										private static final int TDIFFCAP	= 0x1;		

	private static final int RES_TAB_SIZE = 67;

	/*
	 * electrical parameters used for deriving capacitance info for charge
	 * sharing.  Default values aren't for any particular process, but are
	 * self-consistent.
	 *	Area capacitances are all in pfarads/sq-micron units.
	 *	Perimeter capacitances are all in pfarads/micron units.
	 */
	/** 2nd metal capacitance -- area */			public  double irsim_CM2A = .00000;		
	/** 2nd metal capacitance -- perimeter */		public  double irsim_CM2P = .00000;		
	/** 1st metal capacitance -- area */			public  double irsim_CMA  = .00003;		
	/** 1st metal capacitance -- perimeter */		public  double irsim_CMP  = .00000;		
	/** poly capacitance -- area */					public  double irsim_CPA  = .00004;		
	/** poly capacitance -- perimeter */			public  double irsim_CPP  = .00000;		
	/** n-diffusion capacitance -- area */			public  double irsim_CDA  = .00010;		
	/** n-diffusion capacitance -- perimeter */		public  double irsim_CDP  = .00060;		
	/** p-diffusion capacitance -- area */			public  double irsim_CPDA = .00010;		
	/** p-diffusion capacitance -- perimeter */		public  double irsim_CPDP = .00060;		
	/** gate capacitance -- area */					public  double irsim_CGA  = .00040;		

	/** microns/lambda */							public  double irsim_LAMBDA     = 2.5;	
	/** LAMBDA**2 */								public  double irsim_LAMBDA2    = 6.25;	
	/** centi-microns/lambda */						public  long   irsim_LAMBDACM   = 250;	
	/** low voltage threshold, normalized units */	public  double irsim_LOWTHRESH  = 0.3;	
	/** high voltage threshold,normalized units */	public  double irsim_HIGHTHRESH = 0.8;	
	/** width of source/drain diffusion */			private double irsim_DIFFEXT    = 0;	

	/* the following are computed from above */
	/** xtor diff-width capacitance -- perimeter */	private double irsim_CTDW;				
													private double irsim_CPTDW;
	/** xtor diff-extension cap. -- perimeter */	private double irsim_CTDE;				
													private double irsim_CPTDE;
	/** xtor gate capacitance -- area */			public  double irsim_CTGA;				

	private List [][]  irsim_res_htab;
	private int        irsim_config_flags;
	private String []  ttype_drop;
	private Width [][] resistances;

	/**
	 * Class for storing and reading configuration information.
	 */
	public Config()
	{
		irsim_res_htab = new List[Sim.NTTYPES][];
		for(int i=0; i<Sim.NTTYPES; i++) irsim_res_htab[i] = null;
		irsim_config_flags = 0;
		resistances = new Width[Sim.R_TYPES][Sim.NTTYPES];
		for(int t = 0; t < Sim.NTTYPES; t++)
			for(int c = 0; c < Sim.R_TYPES; c++)
				resistances[c][t] = null;
	}

	/**
	 * Method to load configuration information from a disk file.
	 * @param configURL a URL to the file with configuration information.
	 * @return true on error
	 */
	public boolean irsim_config(URL configURL)
	{
		ttype_drop = new String[Sim.NTTYPES];
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
				if (line.startsWith(";")) continue;

				String [] targ = Sim.parse_line(line, false);
				if (targ.length == 0) continue;
				if (targ[0].equals("resistance"))
				{
					if (targ.length >= 6)
						insert(fileName, lineReader.getLineNumber(), targ[1], targ[2], targ[3], targ[4], targ[5]); else
					{
						Sim.irsim_error(fileName, lineReader.getLineNumber(), "syntax error in resistance spec");
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
					}
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

		return false;
	}

	/**
	 * Compute equivalent resistance given width, length and type of transistor.
	 * for all contexts (STATIC, DYNHIGH, DYNLOW).  Place the result on the
	 * transistor
	 */
	public Sim.Resists irsim_requiv(int type, long width, long length)
	{
		type = Sim.BASETYPE(type);

		List [] rtab = irsim_res_htab[type];
		if (rtab == null)
		{
			rtab = new List[RES_TAB_SIZE];
			for(int n = 0; n < RES_TAB_SIZE; n++) rtab[n] = null;
			irsim_res_htab[type] = rtab;
		}
		int n = (int)(Math.abs(length * 110133 + width) % RES_TAB_SIZE);
		if (rtab[n] != null)
		{
			for(Iterator it = rtab[n].iterator(); it.hasNext(); )
			{
				Sim.Resists rr = (Sim.Resists)it.next();
				if (rr.length == length && rr.width == width) return rr;
			}
		}

		Sim.Resists rr = new Sim.Resists();
		if (rtab[n] == null) rtab[n] = new ArrayList();
		rtab[n].add(rr);

		rr.length = length;
		rr.width = width;

		if (type == Sim.RESIST)
		{
			rr.dynres[Sim.R_LOW] = rr.dynres[Sim.R_HIGH] = rr.rstatic = (float) length / irsim_LAMBDACM;
		} else
		{
			rr.rstatic = (float)wresist(resistances[Sim.STATIC][type], width, length);
			rr.dynres[Sim.R_LOW] = (float)wresist(resistances[Sim.DYNLOW][type], width, length);
			rr.dynres[Sim.R_HIGH] = (float)wresist(resistances[Sim.DYNHIGH][type], width, length);
		}
		return rr;
	}

	/**
	 * linear interpolation, assume that x1 < x <= x2
	 */
	private double interp(double x, double x1, double y1, double x2, double y2)
	{
		return ((x - x1) / (x2 - x1)) * (y2 - y1) + y1;
	}

	/**
	 * given a list of length structures, sorted by incresing length return
	 * resistance of given channel.  If no exact match, return result of
	 * linear interpolation using two closest channels.
	 */
	private double lresist(Length list, long l, double size)
	{
		Length q = null;
		for(Length p = list; p != null; q = p, p = p.next)
		{
			if (p.l == l ||(p.l > l && q == null))
				return p.r * size;
			if (p.l > l)
				return size * interp(l, q.l, q.r, p.l, p.r);
		}
		if (q != null)
			return q.r *size;
		return 1E4 * size;
	}

	/**
	 * given a pointer to the width structures for a particular type of
	 * channel compute the resistance for the specified channel.
	 */
	private double wresist(Width list, long w, long l)
	{
		double size = ((double) l) / ((double) w);

		Width q = null;
		for(Width p = list; p != null; q = p, p = p.next)
		{
			if (p.w == w || (p.w > w && q == null))
				return lresist(p.list, l, size);
			if (p.w > w)
			{
				double temp = lresist(q.list, l, size);
				return interp(w, q.w, temp, p.w, lresist(p.list, l, size));
			}
		}
		if (q != null)
			return lresist(q.list, l, size);
		return 1E4 * size;
	}

	private Length linsert(Length list, long l, double resist)
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

	/**
	 * add a new data point to the interpolation array
	 */
	private void winsert(int c, int t, long w, long l, double resist)
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

	/**
	 * interpret resistance specification command
	 */
	private void insert(String fileName, int lineNo, String type, String context, String w, String l, String r)
	{
		long width = (long)(TextUtils.atof(w) * CM_M);
		long length = (long)(TextUtils.atof(l) * CM_M);
		double resist = TextUtils.atof(r);
		if (width <= 0 || length <= 0 || resist <= 0)
		{
			Sim.irsim_error(fileName, lineNo, "bad w, l, or r in config file");
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
	}
}
