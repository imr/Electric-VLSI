/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: SStep.java
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

public class SStep extends Eval
{

	/* event-driven switch-level simulation step Chris Terman 7/84 */
	
	/* the following file contains most of the declarations, conversion
	 * tables, etc, that depend on the particular representation chosen
	 * for node values.  This info is automatically created for interval
	 * value sets (see Chapter 5 of my thesis) by gentbl.c.  Except
	 * for the charged_state and thev_value arrays and their associated
	 * code, the rest of the code is independent of the value set details...
	 */

	/* index for each value interval */
	static final int EMPTY	= 0;
	static final int DH		= 1;
	static final int DHWH	= 2;
	static final int DHCH	= 3;
	static final int DHcH	= 4;
	static final int DHZ	= 5;
	static final int DHcL	= 6;
	static final int DHCL	= 7;
	static final int DHWL	= 8;
	static final int DHDL	= 9;
	static final int WH		= 10;
	static final int WHCH	= 11;
	static final int WHcH	= 12;
	static final int WHZ	= 13;
	static final int WHcL	= 14;
	static final int WHCL	= 15;
	static final int WHWL	= 16;
	static final int WHDL	= 17;
	static final int CH		= 18;
	static final int CHcH	= 19;
	static final int CHZ	= 20;
	static final int CHcL	= 21;
	static final int CHCL	= 22;
	static final int CHWL	= 23;
	static final int CHDL	= 24;
	static final int cH		= 25;
	static final int cHZ	= 26;
	static final int cHcL	= 27;
	static final int cHCL	= 28;
	static final int cHWL	= 29;
	static final int cHDL	= 30;
	static final int Z		= 31;
	static final int ZcL	= 32;
	static final int ZCL	= 33;
	static final int ZWL	= 34;
	static final int ZDL	= 35;
	static final int cL		= 36;
	static final int cLCL	= 37;
	static final int cLWL	= 38;
	static final int cLDL	= 39;
	static final int CL		= 40;
	static final int CLWL	= 41;
	static final int CLDL	= 42;
	static final int WL		= 43;
	static final int WLDL	= 44;
	static final int DL		= 45;

	/* conversion between interval and logic value */
	static byte [] logic_state = new byte[]
	{
		0,			/* EMPTY state */
		Sim.HIGH,	/* DH */
		Sim.HIGH,	/* DHWH */
		Sim.HIGH,	/* DHCH */
		Sim.HIGH,	/* DHcH */
		Sim.X,		/* DHZ */
		Sim.X,		/* DHcL */
		Sim.X,		/* DHCL */
		Sim.X,		/* DHWL */
		Sim.X,		/* DHDL */
		Sim.HIGH,	/* WH */
		Sim.HIGH,	/* WHCH */
		Sim.HIGH,	/* WHcH */
		Sim.X,		/* WHZ */
		Sim.X,		/* WHcL */
		Sim.X,		/* WHCL */
		Sim.X,		/* WHWL */
		Sim.X,		/* WHDL */
		Sim.HIGH,	/* CH */
		Sim.HIGH,	/* CHcH */
		Sim.X,		/* CHZ */
		Sim.X,		/* CHcL */
		Sim.X,		/* CHCL */
		Sim.X,		/* CHWL */
		Sim.X,		/* CHDL */
		Sim.HIGH,	/* cH */
		Sim.X,		/* cHZ */
		Sim.X,		/* cHcL */
		Sim.X,		/* cHCL */
		Sim.X,		/* cHWL */
		Sim.X,		/* cHDL */
		Sim.X,		/* Z */
		Sim.X,		/* ZcL */
		Sim.X,		/* ZCL */
		Sim.X,		/* ZWL */
		Sim.X,		/* ZDL */
		Sim.LOW,	/* cL */
		Sim.LOW,	/* cLCL */
		Sim.LOW,	/* cLWL */
		Sim.LOW,	/* cLDL */
		Sim.LOW,	/* CL */
		Sim.LOW,	/* CLWL */
		Sim.LOW,	/* CLDL */
		Sim.LOW,	/* WL */
		Sim.LOW,	/* WLDL */
		Sim.LOW,	/* DL */
	};

	/* transmit interval through switch */
	static byte [][] transmit = new byte[][]
	{
		new byte[] {0, 0,	   0,    0},	/* EMPTY state */
		new byte[] {Z, DH,   DHZ,  WH},		/* DH */
		new byte[] {Z, DHWH, DHZ,  WH},		/* DHWH */
		new byte[] {Z, DHCH, DHZ,  WHCH},	/* DHCH */
		new byte[] {Z, DHcH, DHZ,  WHcH},	/* DHcH */
		new byte[] {Z, DHZ,  DHZ,  WHZ},	/* DHZ */
		new byte[] {Z, DHcL, DHcL, WHcL},	/* DHcL */
		new byte[] {Z, DHCL, DHCL, WHCL},	/* DHCL */
		new byte[] {Z, DHWL, DHWL, WHWL},	/* DHWL */
		new byte[] {Z, DHDL, DHDL, WHWL},	/* DHDL */
		new byte[] {Z, WH,   WHZ,  WH},		/* WH */
		new byte[] {Z, WHCH, WHZ,  WHCH},	/* WHCH */
		new byte[] {Z, WHcH, WHZ,  WHcH},	/* WHcH */
		new byte[] {Z, WHZ,  WHZ,  WHZ},	/* WHZ */
		new byte[] {Z, WHcL, WHcL, WHcL},	/* WHcL */
		new byte[] {Z, WHCL, WHCL, WHCL},	/* WHCL */
		new byte[] {Z, WHWL, WHWL, WHWL},	/* WHWL */
		new byte[] {Z, WHDL, WHDL, WHWL},	/* WHDL */
		new byte[] {Z, CH,   CHZ,  CH},		/* CH */
		new byte[] {Z, CHcH, CHZ,  CHcH},	/* CHcH */
		new byte[] {Z, CHZ,  CHZ,  CHZ},	/* CHZ */
		new byte[] {Z, CHcL, CHcL, CHcL},	/* CHcL */
		new byte[] {Z, CHCL, CHCL, CHCL},	/* CHCL */
		new byte[] {Z, CHWL, CHWL, CHWL},	/* CHWL */
		new byte[] {Z, CHDL, CHDL, CHWL},	/* CHDL */
		new byte[] {Z, cH,   cHZ,  cH},		/* cH */
		new byte[] {Z, cHZ,  cHZ,  cHZ},	/* cHZ */
		new byte[] {Z, cHcL, cHcL, cHcL},	/* cHcL */
		new byte[] {Z, cHCL, cHCL, cHCL},	/* cHCL */
		new byte[] {Z, cHWL, cHWL, cHWL},	/* cHWL */
		new byte[] {Z, cHDL, cHDL, cHWL},	/* cHDL */
		new byte[] {Z, Z,    Z,    Z},		/* Z */
		new byte[] {Z, ZcL,  ZcL,  ZcL},	/* ZcL */
		new byte[] {Z, ZCL,  ZCL,  ZCL},	/* ZCL */
		new byte[] {Z, ZWL,  ZWL,  ZWL},	/* ZWL */
		new byte[] {Z, ZDL,  ZDL,  ZWL},	/* ZDL */
		new byte[] {Z, cL,   ZcL,  cL},		/* cL */
		new byte[] {Z, cLCL, ZCL,  cLCL},	/* cLCL */
		new byte[] {Z, cLWL, ZWL,  cLWL},	/* cLWL */
		new byte[] {Z, cLDL, ZDL,  cLWL},	/* cLDL */
		new byte[] {Z, CL,   ZCL,  CL},		/* CL */
		new byte[] {Z, CLWL, ZWL,  CLWL},	/* CLWL */
		new byte[] {Z, CLDL, ZDL,  CLWL},	/* CLDL */
		new byte[] {Z, WL,   ZWL,  WL},		/* WL */
		new byte[] {Z, WLDL, ZDL,  WL},		/* WLDL */
		new byte[] {Z, DL,   ZDL,  WL}		/* DL */
	};

	/* tables for converting node value to corresponding charged state */
	static	byte [] charged_state = new byte[] {CL, CHCL, CHCL, CH};
	static	byte [] xcharged_state = new byte[] {cL, cHcL, cHcL, cH};
	
	/* table for converting node value to corresponding value state */
	static	byte [] thev_value = new byte[] {DL, DHDL, DHDL, DH};

	/* result of shorting two intervals */
	static byte [][] smerge = new byte[][]
	{
	/* EMPTY state */
	  new byte[] {0,  0,  0,  0,  0,  0,  0,  0,
	  0,  0,  0,  0,  0,  0,  0,  0,
	  0,  0,  0,  0,  0,  0,  0,  0,
	  0,  0,  0,  0,  0,  0,  0,  0,
	  0,  0,  0,  0,  0,  0,  0,  0,
	  0,  0,  0,  0,  0,  0},
	/* DH */
	  new byte[] {0,  DH,  DH,  DH,  DH,  DH,  DH,  DH,
	  DH,  DHDL,  DH,  DH,  DH,  DH,  DH,  DH,
	  DH,  DHDL,  DH,  DH,  DH,  DH,  DH,  DH,
	  DHDL,  DH,  DH,  DH,  DH,  DH,  DHDL,  DH,
	  DH,  DH,  DH,  DHDL,  DH,  DH,  DH,  DHDL,
	  DH,  DH,  DHDL,  DH,  DHDL,  DHDL},
	/* DHWH */
	  new byte[] {0,  DH,  DHWH,  DHWH,  DHWH,  DHWH,  DHWH,  DHWH,
	  DHWL,  DHDL,  DHWH,  DHWH,  DHWH,  DHWH,  DHWH,  DHWH,
	  DHWL,  DHDL,  DHWH,  DHWH,  DHWH,  DHWH,  DHWH,  DHWL,
	  DHDL,  DHWH,  DHWH,  DHWH,  DHWH,  DHWL,  DHDL,  DHWH,
	  DHWH,  DHWH,  DHWL,  DHDL,  DHWH,  DHWH,  DHWL,  DHDL,
	  DHWH,  DHWL,  DHDL,  DHWL,  DHDL,  DHDL},
	/* DHCH */
	  new byte[] {0,  DH,  DHWH,  DHCH,  DHCH,  DHCH,  DHCH,  DHCL,
	  DHWL,  DHDL,  DHWH,  DHCH,  DHCH,  DHCH,  DHCH,  DHCL,
	  DHWL,  DHDL,  DHCH,  DHCH,  DHCH,  DHCH,  DHCL,  DHWL,
	  DHDL,  DHCH,  DHCH,  DHCH,  DHCL,  DHWL,  DHDL,  DHCH,
	  DHCH,  DHCL,  DHWL,  DHDL,  DHCH,  DHCL,  DHWL,  DHDL,
	  DHCL,  DHWL,  DHDL,  DHWL,  DHDL,  DHDL},
	/* DHcH */
	  new byte[] {0,  DH,  DHWH,  DHCH,  DHcH,  DHcH,  DHcL,  DHCL,
	  DHWL,  DHDL,  DHWH,  DHCH,  DHcH,  DHcH,  DHcL,  DHCL,
	  DHWL,  DHDL,  DHCH,  DHcH,  DHcH,  DHcL,  DHCL,  DHWL,
	  DHDL,  DHcH,  DHcH,  DHcL,  DHCL,  DHWL,  DHDL,  DHcH,
	  DHcL,  DHCL,  DHWL,  DHDL,  DHcL,  DHCL,  DHWL,  DHDL,
	  DHCL,  DHWL,  DHDL,  DHWL,  DHDL,  DHDL},
	/* DHZ */
	  new byte[] {0,  DH,  DHWH,  DHCH,  DHcH,  DHZ,  DHcL,  DHCL,
	  DHWL,  DHDL,  DHWH,  DHCH,  DHcH,  DHZ,  DHcL,  DHCL,
	  DHWL,  DHDL,  DHCH,  DHcH,  DHZ,  DHcL,  DHCL,  DHWL,
	  DHDL,  DHcH,  DHZ,  DHcL,  DHCL,  DHWL,  DHDL,  DHZ,
	  DHcL,  DHCL,  DHWL,  DHDL,  DHcL,  DHCL,  DHWL,  DHDL,
	  DHCL,  DHWL,  DHDL,  DHWL,  DHDL,  DHDL},
	/* DHcL */
	  new byte[] {0,  DH,  DHWH,  DHCH,  DHcL,  DHcL,  DHcL,  DHCL,
	  DHWL,  DHDL,  DHWH,  DHCH,  DHcL,  DHcL,  DHcL,  DHCL,
	  DHWL,  DHDL,  DHCH,  DHcL,  DHcL,  DHcL,  DHCL,  DHWL,
	  DHDL,  DHcL,  DHcL,  DHcL,  DHCL,  DHWL,  DHDL,  DHcL,
	  DHcL,  DHCL,  DHWL,  DHDL,  DHcL,  DHCL,  DHWL,  DHDL,
	  DHCL,  DHWL,  DHDL,  DHWL,  DHDL,  DHDL},
	/* DHCL */
	  new byte[] {0,  DH,  DHWH,  DHCL,  DHCL,  DHCL,  DHCL,  DHCL,
	  DHWL,  DHDL,  DHWH,  DHCL,  DHCL,  DHCL,  DHCL,  DHCL,
	  DHWL,  DHDL,  DHCL,  DHCL,  DHCL,  DHCL,  DHCL,  DHWL,
	  DHDL,  DHCL,  DHCL,  DHCL,  DHCL,  DHWL,  DHDL,  DHCL,
	  DHCL,  DHCL,  DHWL,  DHDL,  DHCL,  DHCL,  DHWL,  DHDL,
	  DHCL,  DHWL,  DHDL,  DHWL,  DHDL,  DHDL},
	/* DHWL */
	  new byte[] {0,  DH,  DHWL,  DHWL,  DHWL,  DHWL,  DHWL,  DHWL,
	  DHWL,  DHDL,  DHWL,  DHWL,  DHWL,  DHWL,  DHWL,  DHWL,
	  DHWL,  DHDL,  DHWL,  DHWL,  DHWL,  DHWL,  DHWL,  DHWL,
	  DHDL,  DHWL,  DHWL,  DHWL,  DHWL,  DHWL,  DHDL,  DHWL,
	  DHWL,  DHWL,  DHWL,  DHDL,  DHWL,  DHWL,  DHWL,  DHDL,
	  DHWL,  DHWL,  DHDL,  DHWL,  DHDL,  DHDL},
	/* DHDL */
	  new byte[] {0,  DHDL,  DHDL,  DHDL,  DHDL,  DHDL,  DHDL,  DHDL,
	  DHDL,  DHDL,  DHDL,  DHDL,  DHDL,  DHDL,  DHDL,  DHDL,
	  DHDL,  DHDL,  DHDL,  DHDL,  DHDL,  DHDL,  DHDL,  DHDL,
	  DHDL,  DHDL,  DHDL,  DHDL,  DHDL,  DHDL,  DHDL,  DHDL,
	  DHDL,  DHDL,  DHDL,  DHDL,  DHDL,  DHDL,  DHDL,  DHDL,
	  DHDL,  DHDL,  DHDL,  DHDL,  DHDL,  DHDL},
	/* WH */
	  new byte[] {0,  DH,  DHWH,  DHWH,  DHWH,  DHWH,  DHWH,  DHWH,
	  DHWL,  DHDL,  WH,  WH,  WH,  WH,  WH,  WH,
	  WHWL,  WHDL,  WH,  WH,  WH,  WH,  WH,  WHWL,
	  WHDL,  WH,  WH,  WH,  WH,  WHWL,  WHDL,  WH,
	  WH,  WH,  WHWL,  WHDL,  WH,  WH,  WHWL,  WHDL,
	  WH,  WHWL,  WHDL,  WHWL,  WHDL,  DL},
	/* WHCH */
	  new byte[] {0,  DH,  DHWH,  DHCH,  DHCH,  DHCH,  DHCH,  DHCL,
	  DHWL,  DHDL,  WH,  WHCH,  WHCH,  WHCH,  WHCH,  WHCL,
	  WHWL,  WHDL,  WHCH,  WHCH,  WHCH,  WHCH,  WHCL,  WHWL,
	  WHDL,  WHCH,  WHCH,  WHCH,  WHCL,  WHWL,  WHDL,  WHCH,
	  WHCH,  WHCL,  WHWL,  WHDL,  WHCH,  WHCL,  WHWL,  WHDL,
	  WHCL,  WHWL,  WHDL,  WHWL,  WHDL,  DL},
	/* WHcH */
	  new byte[] {0,  DH,  DHWH,  DHCH,  DHcH,  DHcH,  DHcL,  DHCL,
	  DHWL,  DHDL,  WH,  WHCH,  WHcH,  WHcH,  WHcL,  WHCL,
	  WHWL,  WHDL,  WHCH,  WHcH,  WHcH,  WHcL,  WHCL,  WHWL,
	  WHDL,  WHcH,  WHcH,  WHcL,  WHCL,  WHWL,  WHDL,  WHcH,
	  WHcL,  WHCL,  WHWL,  WHDL,  WHcL,  WHCL,  WHWL,  WHDL,
	  WHCL,  WHWL,  WHDL,  WHWL,  WHDL,  DL},
	/* WHZ */
	  new byte[] {0,  DH,  DHWH,  DHCH,  DHcH,  DHZ,  DHcL,  DHCL,
	  DHWL,  DHDL,  WH,  WHCH,  WHcH,  WHZ,  WHcL,  WHCL,
	  WHWL,  WHDL,  WHCH,  WHcH,  WHZ,  WHcL,  WHCL,  WHWL,
	  WHDL,  WHcH,  WHZ,  WHcL,  WHCL,  WHWL,  WHDL,  WHZ,
	  WHcL,  WHCL,  WHWL,  WHDL,  WHcL,  WHCL,  WHWL,  WHDL,
	  WHCL,  WHWL,  WHDL,  WHWL,  WHDL,  DL},
	/* WHcL */
	  new byte[] {0,  DH,  DHWH,  DHCH,  DHcL,  DHcL,  DHcL,  DHCL,
	  DHWL,  DHDL,  WH,  WHCH,  WHcL,  WHcL,  WHcL,  WHCL,
	  WHWL,  WHDL,  WHCH,  WHcL,  WHcL,  WHcL,  WHCL,  WHWL,
	  WHDL,  WHcL,  WHcL,  WHcL,  WHCL,  WHWL,  WHDL,  WHcL,
	  WHcL,  WHCL,  WHWL,  WHDL,  WHcL,  WHCL,  WHWL,  WHDL,
	  WHCL,  WHWL,  WHDL,  WHWL,  WHDL,  DL},
	/* WHCL */
	  new byte[] {0,  DH,  DHWH,  DHCL,  DHCL,  DHCL,  DHCL,  DHCL,
	  DHWL,  DHDL,  WH,  WHCL,  WHCL,  WHCL,  WHCL,  WHCL,
	  WHWL,  WHDL,  WHCL,  WHCL,  WHCL,  WHCL,  WHCL,  WHWL,
	  WHDL,  WHCL,  WHCL,  WHCL,  WHCL,  WHWL,  WHDL,  WHCL,
	  WHCL,  WHCL,  WHWL,  WHDL,  WHCL,  WHCL,  WHWL,  WHDL,
	  WHCL,  WHWL,  WHDL,  WHWL,  WHDL,  DL},
	/* WHWL */
	  new byte[] {0,  DH,  DHWL,  DHWL,  DHWL,  DHWL,  DHWL,  DHWL,
	  DHWL,  DHDL,  WHWL,  WHWL,  WHWL,  WHWL,  WHWL,  WHWL,
	  WHWL,  WHDL,  WHWL,  WHWL,  WHWL,  WHWL,  WHWL,  WHWL,
	  WHDL,  WHWL,  WHWL,  WHWL,  WHWL,  WHWL,  WHDL,  WHWL,
	  WHWL,  WHWL,  WHWL,  WHDL,  WHWL,  WHWL,  WHWL,  WHDL,
	  WHWL,  WHWL,  WHDL,  WHWL,  WHDL,  DL},
	/* WHDL */
	  new byte[] {0,  DHDL,  DHDL,  DHDL,  DHDL,  DHDL,  DHDL,  DHDL,
	  DHDL,  DHDL,  WHDL,  WHDL,  WHDL,  WHDL,  WHDL,  WHDL,
	  WHDL,  WHDL,  WHDL,  WHDL,  WHDL,  WHDL,  WHDL,  WHDL,
	  WHDL,  WHDL,  WHDL,  WHDL,  WHDL,  WHDL,  WHDL,  WHDL,
	  WHDL,  WHDL,  WHDL,  WHDL,  WHDL,  WHDL,  WHDL,  WHDL,
	  WHDL,  WHDL,  WHDL,  WHDL,  WHDL,  DL},
	/* CH */
	  new byte[] {0,  DH,  DHWH,  DHCH,  DHCH,  DHCH,  DHCH,  DHCL,
	  DHWL,  DHDL,  WH,  WHCH,  WHCH,  WHCH,  WHCH,  WHCL,
	  WHWL,  WHDL,  CH,  CH,  CH,  CH,  CHCL,  CHWL,
	  CHDL,  CH,  CH,  CH,  CHCL,  CHWL,  CHDL,  CH,
	  CH,  CHCL,  CHWL,  CHDL,  CH,  CHCL,  CHWL,  CHDL,
	  CHCL,  CHWL,  CHDL,  WL,  WLDL,  DL},
	/* CHcH */
	  new byte[] {0,  DH,  DHWH,  DHCH,  DHcH,  DHcH,  DHcL,  DHCL,
	  DHWL,  DHDL,  WH,  WHCH,  WHcH,  WHcH,  WHcL,  WHCL,
	  WHWL,  WHDL,  CH,  CHcH,  CHcH,  CHcL,  CHCL,  CHWL,
	  CHDL,  CHcH,  CHcH,  CHcL,  CHCL,  CHWL,  CHDL,  CHcH,
	  CHcL,  CHCL,  CHWL,  CHDL,  CHcL,  CHCL,  CHWL,  CHDL,
	  CHCL,  CHWL,  CHDL,  WL,  WLDL,  DL},
	/* CHZ */
	  new byte[] {0,  DH,  DHWH,  DHCH,  DHcH,  DHZ,  DHcL,  DHCL,
	  DHWL,  DHDL,  WH,  WHCH,  WHcH,  WHZ,  WHcL,  WHCL,
	  WHWL,  WHDL,  CH,  CHcH,  CHZ,  CHcL,  CHCL,  CHWL,
	  CHDL,  CHcH,  CHZ,  CHcL,  CHCL,  CHWL,  CHDL,  CHZ,
	  CHcL,  CHCL,  CHWL,  CHDL,  CHcL,  CHCL,  CHWL,  CHDL,
	  CHCL,  CHWL,  CHDL,  WL,  WLDL,  DL},
	/* CHcL */
	  new byte[] {0,  DH,  DHWH,  DHCH,  DHcL,  DHcL,  DHcL,  DHCL,
	  DHWL,  DHDL,  WH,  WHCH,  WHcL,  WHcL,  WHcL,  WHCL,
	  WHWL,  WHDL,  CH,  CHcL,  CHcL,  CHcL,  CHCL,  CHWL,
	  CHDL,  CHcL,  CHcL,  CHcL,  CHCL,  CHWL,  CHDL,  CHcL,
	  CHcL,  CHCL,  CHWL,  CHDL,  CHcL,  CHCL,  CHWL,  CHDL,
	  CHCL,  CHWL,  CHDL,  WL,  WLDL,  DL},
	/* CHCL */
	  new byte[] {0,  DH,  DHWH,  DHCL,  DHCL,  DHCL,  DHCL,  DHCL,
	  DHWL,  DHDL,  WH,  WHCL,  WHCL,  WHCL,  WHCL,  WHCL,
	  WHWL,  WHDL,  CHCL,  CHCL,  CHCL,  CHCL,  CHCL,  CHWL,
	  CHDL,  CHCL,  CHCL,  CHCL,  CHCL,  CHWL,  CHDL,  CHCL,
	  CHCL,  CHCL,  CHWL,  CHDL,  CHCL,  CHCL,  CHWL,  CHDL,
	  CHCL,  CHWL,  CHDL,  WL,  WLDL,  DL},
	/* CHWL */
	  new byte[] { 0,  DH,  DHWL,  DHWL,  DHWL,  DHWL,  DHWL,  DHWL,
	  DHWL,  DHDL,  WHWL,  WHWL,  WHWL,  WHWL,  WHWL,  WHWL,
	  WHWL,  WHDL,  CHWL,  CHWL,  CHWL,  CHWL,  CHWL,  CHWL,
	  CHDL,  CHWL,  CHWL,  CHWL,  CHWL,  CHWL,  CHDL,  CHWL,
	  CHWL,  CHWL,  CHWL,  CHDL,  CHWL,  CHWL,  CHWL,  CHDL,
	  CHWL,  CHWL,  CHDL,  WL,  WLDL,  DL},
	/* CHDL */
	  new byte[] {0,  DHDL,  DHDL,  DHDL,  DHDL,  DHDL,  DHDL,  DHDL,
	  DHDL,  DHDL,  WHDL,  WHDL,  WHDL,  WHDL,  WHDL,  WHDL,
	  WHDL,  WHDL,  CHDL,  CHDL,  CHDL,  CHDL,  CHDL,  CHDL,
	  CHDL,  CHDL,  CHDL,  CHDL,  CHDL,  CHDL,  CHDL,  CHDL,
	  CHDL,  CHDL,  CHDL,  CHDL,  CHDL,  CHDL,  CHDL,  CHDL,
	  CHDL,  CHDL,  CHDL,  WLDL,  WLDL,  DL},
	/* cH */
	  new byte[] {0,  DH,  DHWH,  DHCH,  DHcH,  DHcH,  DHcL,  DHCL,
	  DHWL,  DHDL,  WH,  WHCH,  WHcH,  WHcH,  WHcL,  WHCL,
	  WHWL,  WHDL,  CH,  CHcH,  CHcH,  CHcL,  CHCL,  CHWL,
	  CHDL,  cH,  cH,  cHcL,  cHCL,  cHWL,  cHDL,  cH,
	  cHcL,  cHCL,  cHWL,  cHDL,  cHcL,  cHCL,  cHWL,  cHDL,
	  CL,  CLWL,  CLDL,  WL,  WLDL,  DL},
	/* cHZ */
	  new byte[] {0,  DH,  DHWH,  DHCH,  DHcH,  DHZ,  DHcL,  DHCL,
	  DHWL,  DHDL,  WH,  WHCH,  WHcH,  WHZ,  WHcL,  WHCL,
	  WHWL,  WHDL,  CH,  CHcH,  CHZ,  CHcL,  CHCL,  CHWL,
	  CHDL,  cH,  cHZ,  cHcL,  cHCL,  cHWL,  cHDL,  cHZ,
	  cHcL,  cHCL,  cHWL,  cHDL,  cHcL,  cHCL,  cHWL,  cHDL,
	  CL,  CLWL,  CLDL,  WL,  WLDL,  DL},
	/* cHcL */
	  new byte[] {0,  DH,  DHWH,  DHCH,  DHcL,  DHcL,  DHcL,  DHCL,
	  DHWL,  DHDL,  WH,  WHCH,  WHcL,  WHcL,  WHcL,  WHCL,
	  WHWL,  WHDL,  CH,  CHcL,  CHcL,  CHcL,  CHCL,  CHWL,
	  CHDL,  cHcL,  cHcL,  cHcL,  cHCL,  cHWL,  cHDL,  cHcL,
	  cHcL,  cHCL,  cHWL,  cHDL,  cHcL,  cHCL,  cHWL,  cHDL,
	  CL,  CLWL,  CLDL,  WL,  WLDL,  DL},
	/* cHCL */
	  new byte[] {0,  DH,  DHWH,  DHCL,  DHCL,  DHCL,  DHCL,  DHCL,
	  DHWL,  DHDL,  WH,  WHCL,  WHCL,  WHCL,  WHCL,  WHCL,
	  WHWL,  WHDL,  CHCL,  CHCL,  CHCL,  CHCL,  CHCL,  CHWL,
	  CHDL,  cHCL,  cHCL,  cHCL,  cHCL,  cHWL,  cHDL,  cHCL,
	  cHCL,  cHCL,  cHWL,  cHDL,  cHCL,  cHCL,  cHWL,  cHDL,
	  CL,  CLWL,  CLDL,  WL,  WLDL,  DL},
	/* cHWL */
	  new byte[] {0,  DH,  DHWL,  DHWL,  DHWL,  DHWL,  DHWL,  DHWL,
	  DHWL,  DHDL,  WHWL,  WHWL,  WHWL,  WHWL,  WHWL,  WHWL,
	  WHWL,  WHDL,  CHWL,  CHWL,  CHWL,  CHWL,  CHWL,  CHWL,
	  CHDL,  cHWL,  cHWL,  cHWL,  cHWL,  cHWL,  cHDL,  cHWL,
	  cHWL,  cHWL,  cHWL,  cHDL,  cHWL,  cHWL,  cHWL,  cHDL,
	  CLWL,  CLWL,  CLDL,  WL,  WLDL,  DL},
	/* cHDL */
	  new byte[] {0,  DHDL,  DHDL,  DHDL,  DHDL,  DHDL,  DHDL,  DHDL,
	  DHDL,  DHDL,  WHDL,  WHDL,  WHDL,  WHDL,  WHDL,  WHDL,
	  WHDL,  WHDL,  CHDL,  CHDL,  CHDL,  CHDL,  CHDL,  CHDL,
	  CHDL,  cHDL,  cHDL,  cHDL,  cHDL,  cHDL,  cHDL,  cHDL,
	  cHDL,  cHDL,  cHDL,  cHDL,  cHDL,  cHDL,  cHDL,  cHDL,
	  CLDL,  CLDL,  CLDL,  WLDL,  WLDL,  DL},
	/* Z */
	  new byte[] {0,  DH,  DHWH,  DHCH,  DHcH,  DHZ,  DHcL,  DHCL,
	  DHWL,  DHDL,  WH,  WHCH,  WHcH,  WHZ,  WHcL,  WHCL,
	  WHWL,  WHDL,  CH,  CHcH,  CHZ,  CHcL,  CHCL,  CHWL,
	  CHDL,  cH,  cHZ,  cHcL,  cHCL,  cHWL,  cHDL,  Z,
	  ZcL,  ZCL,  ZWL,  ZDL,  cL,  cLCL,  cLWL,  cLDL,
	  CL,  CLWL,  CLDL,  WL,  WLDL,  DL},
	/* ZcL */
	  new byte[] {0,  DH,  DHWH,  DHCH,  DHcL,  DHcL,  DHcL,  DHCL,
	  DHWL,  DHDL,  WH,  WHCH,  WHcL,  WHcL,  WHcL,  WHCL,
	  WHWL,  WHDL,  CH,  CHcL,  CHcL,  CHcL,  CHCL,  CHWL,
	  CHDL,  cHcL,  cHcL,  cHcL,  cHCL,  cHWL,  cHDL,  ZcL,
	  ZcL,  ZCL,  ZWL,  ZDL,  cL,  cLCL,  cLWL,  cLDL,
	  CL,  CLWL,  CLDL,  WL,  WLDL,  DL},
	/* ZCL */
	  new byte[] {0,  DH,  DHWH,  DHCL,  DHCL,  DHCL,  DHCL,  DHCL,
	  DHWL,  DHDL,  WH,  WHCL,  WHCL,  WHCL,  WHCL,  WHCL,
	  WHWL,  WHDL,  CHCL,  CHCL,  CHCL,  CHCL,  CHCL,  CHWL,
	  CHDL,  cHCL,  cHCL,  cHCL,  cHCL,  cHWL,  cHDL,  ZCL,
	  ZCL,  ZCL,  ZWL,  ZDL,  cLCL,  cLCL,  cLWL,  cLDL,
	  CL,  CLWL,  CLDL,  WL,  WLDL,  DL},
	/* ZWL */
	  new byte[] {0,  DH,  DHWL,  DHWL,  DHWL,  DHWL,  DHWL,  DHWL,
	  DHWL,  DHDL,  WHWL,  WHWL,  WHWL,  WHWL,  WHWL,  WHWL,
	  WHWL,  WHDL,  CHWL,  CHWL,  CHWL,  CHWL,  CHWL,  CHWL,
	  CHDL,  cHWL,  cHWL,  cHWL,  cHWL,  cHWL,  cHDL,  ZWL,
	  ZWL,  ZWL,  ZWL,  ZDL,  cLWL,  cLWL,  cLWL,  cLDL,
	  CLWL,  CLWL,  CLDL,  WL,  WLDL,  DL},
	/* ZDL */
	  new byte[] {0,  DHDL,  DHDL,  DHDL,  DHDL,  DHDL,  DHDL,  DHDL,
	  DHDL,  DHDL,  WHDL,  WHDL,  WHDL,  WHDL,  WHDL,  WHDL,
	  WHDL,  WHDL,  CHDL,  CHDL,  CHDL,  CHDL,  CHDL,  CHDL,
	  CHDL,  cHDL,  cHDL,  cHDL,  cHDL,  cHDL,  cHDL,  ZDL,
	  ZDL,  ZDL,  ZDL,  ZDL,  cLDL,  cLDL,  cLDL,  cLDL,
	  CLDL,  CLDL,  CLDL,  WLDL,  WLDL,  DL},
	/* cL */
	  new byte[] {0,  DH,  DHWH,  DHCH,  DHcL,  DHcL,  DHcL,  DHCL,
	  DHWL,  DHDL,  WH,  WHCH,  WHcL,  WHcL,  WHcL,  WHCL,
	  WHWL,  WHDL,  CH,  CHcL,  CHcL,  CHcL,  CHCL,  CHWL,
	  CHDL,  cHcL,  cHcL,  cHcL,  cHCL,  cHWL,  cHDL,  cL,
	  cL,  cLCL,  cLWL,  cLDL,  cL,  cLCL,  cLWL,  cLDL,
	  CL,  CLWL,  CLDL,  WL,  WLDL,  DL},
	/* cLCL */
	  new byte[] {0,  DH,  DHWH,  DHCL,  DHCL,  DHCL,  DHCL,  DHCL,
	  DHWL,  DHDL,  WH,  WHCL,  WHCL,  WHCL,  WHCL,  WHCL,
	  WHWL,  WHDL,  CHCL,  CHCL,  CHCL,  CHCL,  CHCL,  CHWL,
	  CHDL,  cHCL,  cHCL,  cHCL,  cHCL,  cHWL,  cHDL,  cLCL,
	  cLCL,  cLCL,  cLWL,  cLDL,  cLCL,  cLCL,  cLWL,  cLDL,
	  CL,  CLWL,  CLDL,  WL,  WLDL,  DL},
	/* cLWL */
	  new byte[] {0,  DH,  DHWL,  DHWL,  DHWL,  DHWL,  DHWL,  DHWL,
	  DHWL,  DHDL,  WHWL,  WHWL,  WHWL,  WHWL,  WHWL,  WHWL,
	  WHWL,  WHDL,  CHWL,  CHWL,  CHWL,  CHWL,  CHWL,  CHWL,
	  CHDL,  cHWL,  cHWL,  cHWL,  cHWL,  cHWL,  cHDL,  cLWL,
	  cLWL,  cLWL,  cLWL,  cLDL,  cLWL,  cLWL,  cLWL,  cLDL,
	  CLWL,  CLWL,  CLDL,  WL,  WLDL,  DL},
	/* cLDL */
	  new byte[] {0,  DHDL,  DHDL,  DHDL,  DHDL,  DHDL,  DHDL,  DHDL,
	  DHDL,  DHDL,  WHDL,  WHDL,  WHDL,  WHDL,  WHDL,  WHDL,
	  WHDL,  WHDL,  CHDL,  CHDL,  CHDL,  CHDL,  CHDL,  CHDL,
	  CHDL,  cHDL,  cHDL,  cHDL,  cHDL,  cHDL,  cHDL,  cLDL,
	  cLDL,  cLDL,  cLDL,  cLDL,  cLDL,  cLDL,  cLDL,  cLDL,
	  CLDL,  CLDL,  CLDL,  WLDL,  WLDL,  DL},
	/* CL */
	  new byte[] {0,  DH,  DHWH,  DHCL,  DHCL,  DHCL,  DHCL,  DHCL,
	  DHWL,  DHDL,  WH,  WHCL,  WHCL,  WHCL,  WHCL,  WHCL,
	  WHWL,  WHDL,  CHCL,  CHCL,  CHCL,  CHCL,  CHCL,  CHWL,
	  CHDL,  CL,  CL,  CL,  CL,  CLWL,  CLDL,  CL,
	  CL,  CL,  CLWL,  CLDL,  CL,  CL,  CLWL,  CLDL,
	  CL,  CLWL,  CLDL,  WL,  WLDL,  DL},
	/* CLWL */
	  new byte[] {0,  DH,  DHWL,  DHWL,  DHWL,  DHWL,  DHWL,  DHWL,
	  DHWL,  DHDL,  WHWL,  WHWL,  WHWL,  WHWL,  WHWL,  WHWL,
	  WHWL,  WHDL,  CHWL,  CHWL,  CHWL,  CHWL,  CHWL,  CHWL,
	  CHDL,  CLWL,  CLWL,  CLWL,  CLWL,  CLWL,  CLDL,  CLWL,
	  CLWL,  CLWL,  CLWL,  CLDL,  CLWL,  CLWL,  CLWL,  CLDL,
	  CLWL,  CLWL,  CLDL,  WL,  WLDL,  DL},
	/* CLDL */
	  new byte[] {0,  DHDL,  DHDL,  DHDL,  DHDL,  DHDL,  DHDL,  DHDL,
	  DHDL,  DHDL,  WHDL,  WHDL,  WHDL,  WHDL,  WHDL,  WHDL,
	  WHDL,  WHDL,  CHDL,  CHDL,  CHDL,  CHDL,  CHDL,  CHDL,
	  CHDL,  CLDL,  CLDL,  CLDL,  CLDL,  CLDL,  CLDL,  CLDL,
	  CLDL,  CLDL,  CLDL,  CLDL,  CLDL,  CLDL,  CLDL,  CLDL,
	  CLDL,  CLDL,  CLDL,  WLDL,  WLDL,  DL},
	/* WL */
	  new byte[] {0,  DH,  DHWL,  DHWL,  DHWL,  DHWL,  DHWL,  DHWL,
	  DHWL,  DHDL,  WHWL,  WHWL,  WHWL,  WHWL,  WHWL,  WHWL,
	  WHWL,  WHDL,  WL,  WL,  WL,  WL,  WL,  WL,
	  WLDL,  WL,  WL,  WL,  WL,  WL,  WLDL,  WL,
	  WL,  WL,  WL,  WLDL,  WL,  WL,  WL,  WLDL,
	  WL,  WL,  WLDL,  WL,  WLDL,  DL},
	/* WLDL */
	  new byte[] {0,  DHDL,  DHDL,  DHDL,  DHDL,  DHDL,  DHDL,  DHDL,
	  DHDL,  DHDL,  WHDL,  WHDL,  WHDL,  WHDL,  WHDL,  WHDL,
	  WHDL,  WHDL,  WLDL,  WLDL,  WLDL,  WLDL,  WLDL,  WLDL,
	  WLDL,  WLDL,  WLDL,  WLDL,  WLDL,  WLDL,  WLDL,  WLDL,
	  WLDL,  WLDL,  WLDL,  WLDL,  WLDL,  WLDL,  WLDL,  WLDL,
	  WLDL,  WLDL,  WLDL,  WLDL,  WLDL,  DL},
	/* DL */
	  new byte[] {0,  DHDL,  DHDL,  DHDL,  DHDL,  DHDL,  DHDL,  DHDL,
	  DHDL,  DHDL,  DL,  DL,  DL,  DL,  DL,  DL,
	  DL,  DL,  DL,  DL,  DL,  DL,  DL,  DL,
	  DL,  DL,  DL,  DL,  DL,  DL,  DL,  DL,
	  DL,  DL,  DL,  DL,  DL,  DL,  DL,  DL,
	  DL,  DL,  DL,  DL,  DL,  DL}
	};
	
	

	static SStep switchModel = new SStep();

	/* calculate new value for node and its electrical neighbors */

	void modelEvaluate(Sim.Node n)		// irsim_switch_model
	{
		if ((n.nflags & Sim.VISITED) != 0)
			Sim.irsim_BuildConnList(n);
	
		/* for each node on list we just built, recompute its value using a
		 * recursive tree walk.  If logic state of new value differs from
		 * logic state of current value, node is added to the event list (or
		 * if it's already there, just the eval field is updated).  If logic
		 * states do not differ, node's value is updated on the spot and any
		 * pending events removed.
		 */
		for(Sim.Node thisone = n; thisone != null; thisone = thisone.nlink)
		{
			int newval = 0;
			long tau = 0, delta = 0;
			if ((thisone.nflags & Sim.INPUT) != 0)
				newval = thisone.npot;
			else
			{
				newval = logic_state[sc_thev(thisone, (thisone.nflags & Sim.WATCHED) != 0 ? 1 : 0)];
				switch(newval)
				{
					case Sim.LOW:
						delta = thisone.tphl;
						break;
					case Sim.X:
						delta = 0;
						break;
					case Sim.HIGH:
						delta = thisone.tplh;
						break;
				}
				tau = delta;
				if (delta == 0)	// no zero-delay events
					delta = 1;
			}
	
			if ((thisone.nflags & Sim.INPUT) == 0)
			{
				/* 
				 * Check to see if this new value invalidates other events. 
				 * Since this event has newer info about the state of the network,
				 * delete transitions scheduled to come after it.
				 */
				Sim.Event e;
				while((e = thisone.events) != null && e.ntime >= Sched.irsim_cur_delta + delta)
				{
					/* 
					 * Do not try to kick the event scheduled at Sched.irsim_cur_delta if
					 * newval is equal to this.npot because that event sets
					 * this.npot, but its consequences has not been handled yet.
					 * Besides, this event will not be queued. 
					 *
					 * However, if there is event scheduled now but driving to a 
					 * different value, then kick it becuase we will enqueue this 
					 * one, and source/drain of transistors controlled by this
					 * node will be re-evaluated. At worst, some extra computation
					 * will be carried out due to VISITED flags set previously.
					 */
					/*		if (e.ntime == Sched.irsim_cur_delta and newval == thisone.npot) */
					if (e.ntime == (Sched.irsim_cur_delta + delta) && e.eval == newval)
						break;
					Sched.irsim_PuntEvent(thisone, e);
				}
	
				/*
				 * Now see if the new value is different from the last value
				 * scheduled for the node. If there are no pending events then
				 * just use the current value.
				 */
				boolean queued = false;
				if (newval != ((e == null) ? thisone.npot : e.eval))
				{
					queued = true;
					Sched.irsim_enqueue_event(thisone, newval, delta, tau);
				}
			}
		}
	
		// undo connection list
		Sim.Node next = null;
		for(Sim.Node thisone = n; thisone != null; thisone = next)
		{
			next = thisone.nlink;
			thisone.nlink = null;
		}
	}
	
	
	/* compute new value for a node using recursive tree walk.  We start off by
	 * assuming that node is charged to whatever logic state it had last.  The
	 * contribution of each path leading away from the node is computed
	 * recursively and merged with accumulated result.  After all paths have been
	 * examined, return the answer!
	 *
	 * Notes: The node-visited flag is set during the computation of a node's
	 *	  value. This keeps the tree walk exanding outward; loops are avoided.
	 *	  Since the flag is cleared at the end of the computation, all paths
	 *	  through the network will eventually be explored.  (If it had been
	 *	  left set, only a single path through a particular cycle would be
	 *	  explored).
	 *
	 *        In order to speed up the computation, the result of each recursive
	 *	  call is cached.  There are 2 caches associated with each transistor:
	 *	  the source cache remembers the value of the subnetwork that includes
	 *	  the transistor and everything attached to its drain. The drain cache
	 *	  is symmetrical.  Use of these caches reduces the complexity of the
	 *	  new-value computation from O(n**2) to O(n) where n is the number of
	 *	  nodes in the connection list built in the new_value routine.
	 */
	static int sc_thev(Sim.Node n, int level)
	{
		if ((n.nflags & Sim.INPUT) != 0) return thev_value[n.npot];

		// initial values and stuff...
		n.nflags |= Sim.VISITED;
		int result = (n.ngate == null) ? xcharged_state[n.npot] : charged_state[n.npot];
	
		for(Sim.Tlist l = n.nterm; l != null; l = l.next)
		{
			Sim.Trans t = l.xtor;
	
			// don't bother with off transistors
			if (t.state == Sim.OFF) continue;
	
			/* for each non-off transistor, do nothing if node on other side has
			 * its visited flag set (this is how cycles are detected).  Otherwise
			 * check cache and use value found there, if any.  As a last resort,
			 * actually compute value for network on other side of transistor,
			 * transmit the value through the transistor, and save that result
			 * for later use.
			 */
			if (n == t.source)
			{
				if ((t.drain.nflags & Sim.VISITED) == 0)
				{
					if (t.getDI() == EMPTY)
						t.setDI(transmit[sc_thev(t.drain, level != 0 ? level + 1 : 0)][t.state]);
					result = smerge[result][t.getDI()];
				}
			} else
			{
				if ((t.source.nflags & Sim.VISITED) == 0)
				{
					if (t.getSI() == EMPTY)
						t.setSI( transmit[sc_thev(t.source, level != 0 ? level + 1 : 0)][t.state]);
					result = smerge[result][t.getSI()];
				}
			}
		}
	
		n.nflags &= ~Sim.VISITED;
	
		return result;
	}
}
