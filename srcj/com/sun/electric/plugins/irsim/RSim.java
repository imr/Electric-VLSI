/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: RSim.java
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

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

public class RSim
{

	public static class AssertWhen
	{
		Sim.Node   node; /* which node we will check */
		char	   val;  /* what value has the node */
		AssertWhen nxt;
	};
	
	
	/* front end for mos simulator -- Chris Terman (6/84) */
	/* sunbstantial changes: Arturo Salz (88) */
	
	static final int	MAXARGS      = 100;	/* maximum number of command-line arguments */
	static final int	MAXCOL        = 80;	/* maximum width of print line */

	public static class Sequence
	{
		Sequence  next;			/* next vector in linked list */
		int       which;		/* 0 => node; 1 => vector */
	//	union
	//	{
			Sim.Node  n;
			Sim.Bits  b;
	//	} ptr;					/* pointer to node/vector */
		int     vsize;			/* size of each value */
		int     nvalues;		/* number of values specified */
		char    []values;		/* array of values */
	};

	static HashMap cmdTbl;
	
	static Sim.Bits blist = null;				/* list of vectors */
	
	static Sequence slist = null;				/* list of sequences */
	static int		maxsequence = 0;			/* longest sequence defined */
	
	static Sequence xclock = null;				/* vectors which make up clock */
	static int		maxclock = 0;				/* longest clock sequence defined */
	
	static List	    wlist;						/* list of nodes to be displayed */
	static List	    wvlist;						/* list of vectors to be displayed */
	
	static int		column = 0;					/* current output column */
	static boolean	stoped_state = false;		/* have we stoped ? */

	static long	    stepsize = 50000;			/* simulation step, in Delta's */
	static boolean	ddisplay = true;			/* if <>0 run "d" at end of step */

	static String	potchars = "luxh.";			/* set of potential characters */
	static boolean  irsim_analyzerON = false;	/* set when analyzer is running */
	static long     irsim_sim_time0 = 0;		/* starting time (see flush_hist) */
	
	static List  irsim_hinputs = new ArrayList();	/* list of nodes to be driven high */
	static List  irsim_linputs = new ArrayList();	/* list of nodes to be driven low */
	static List  irsim_uinputs = new ArrayList();	/* list of nodes to be driven X */
	static List  irsim_xinputs = new ArrayList();	/* list of nodes to be removed from input */
	
	static List  [] irsim_listTbl = new List[8];

	static void irsim_init_rsim()
	{
		blist = null;
		slist = null;
		maxsequence = 0;
		xclock = null;
		maxclock = 0;
		wlist = new ArrayList();
		wvlist = new ArrayList();
		column = 0;
		stoped_state = false;
		stepsize = 50000;
		ddisplay = true;
		irsim_analyzerON = false;
		irsim_sim_time0 = 0;
		for(int i = 0; i < 8; i++) irsim_listTbl[i] = null;
		irsim_listTbl[Sim.INPUT_NUM(Sim.H_INPUT)] = irsim_hinputs;
		irsim_listTbl[Sim.INPUT_NUM(Sim.L_INPUT)] = irsim_linputs;
		irsim_listTbl[Sim.INPUT_NUM(Sim.U_INPUT)] = irsim_uinputs;
		irsim_listTbl[Sim.INPUT_NUM(Sim.X_INPUT)] = irsim_xinputs;
	}

	static void irsim_issuecommand(String command)
	{
		if (Simulation.isIRSIMShowsCommands()) System.out.println("> " + command);
		String [] strings = Sim.parse_line(command, true);
		if (strings.length > 0) exec_cmd(strings);
	}

	/*
	 * Execute a builtin command or read commands from a '.cmd' file.
	 */
	static int exec_cmd(String [] args)
	{
		// search command table, dispatch to handler, if any
		Command cmd = (Command)cmdTbl.get(args[0]);
		if (cmd != null)
		{
			if (args.length < cmd.lowArgs)
			{
				System.out.println("missing arguments to '" + cmd.commandName + "'");
				return 0;
			}
			if (args.length > cmd.highArgs)
			{
				System.out.println("too many arguments for '" + cmd.commandName + "'");
				return 0;
			}
			return cmd.doIt(args);
		}
	
		System.out.println("unrecognized command: " + args[0]);
		return 0;
	}

	static final int SETIN_CALL      = 1;
	static final int DISPLAY_CALL    = 2;
	static final int SETTRACE_CALL   = 3;
	static final int SETSTOP_CALL    = 4;
	static final int QUEST_CALL      = 5;
	static final int PATH_CALL       = 6;
	static final int FINDONE_CALL    = 7;
	static final int ASSERTWHEN_CALL = 8;

	/*
	 * Apply given function to each argument on the command line.
	 * Arguments are checked first to ensure they are the name of a node or
	 * vector; wild-card patterns are allowed as names.
	 * Either 'fun' or 'vfunc' is called with the node/vector as 1st argument:
	 *	'fun' is called if name refers to a node.
	 *	'vfun' is called if name refers to a vector.  If 'vfun' is null
	 *	then 'fun' is called on each node of the vector.
	 * The parameter (2nd argument) passed to the specified function will be:
	 *	If 'arg' is the special constant '+' then
	 *	    if the name is preceded by a '-' pass a pointer to '-'
	 *	    otherwise pass a pointer to '+'.
	 *	else 'arg' is passed as is.
	 */
	static Object apply(int fun, int vfun, String[] args, int applyStart)
	{
		Find1Arg f = null;
		if (fun == FINDONE_CALL)
		{
			f = new Find1Arg();
			f.num = 0;
			f.vec = null;
			f.node = null;
		}
		for(int i = applyStart; i < args.length; i += 1)
		{
			String p = args[i];
			String flag = args[0];
			if (args[0].equals("+"))
			{
				if (p.startsWith("-"))
				{
					flag = "-";
					p = p.substring(1);
				}
				else
					flag = "+";
			}
	
			int found = 0;
//			if (wildCard[i])
//			{
//				for(Sim.Bits b = blist; b != null; b = b.next)
//					if (irsim_str_match(p, b.name))
//				{
//					if (vfun != null)
//						((int(*)(Sim.Bits,CHAR*))(*vfun))(b, flag);
//					else
//						for(j = 0; j < b.nbits; j += 1)
//							((int(*)(Sim.Node,CHAR*))(*fun))(b.nodes[j], flag);
//					found = 1;
//				}
//				found += irsim_match_net(p, (int(*)(Sim.Node, CHAR*))fun, flag);
//			} else
			{
				Sim.Node  n = Sim.Node.irsim_find(p);
	
				if (n != null)
				{
					switch (fun)
					{
						case SETIN_CALL:
							found += irsim_setin(n, flag);
							break;
						case DISPLAY_CALL:
							found += xwatch(n, flag);
							break;
						case SETTRACE_CALL:
							found += xtrace(n, flag);
							break;
						case SETSTOP_CALL:
							found += nstop(n, flag);
							break;
						case QUEST_CALL:
							found += irsim_info(n, flag);
							break;
						case PATH_CALL:
							found += do_cpath(n);
							break;
						case FINDONE_CALL:
							f.node = n;
							f.num++;
						case ASSERTWHEN_CALL:
							setupAssertWhen(n, flag);
							break;
					}
				} else
				{
					for(Sim.Bits b = blist; b != null; b = b.next)
						if (p.equalsIgnoreCase(b.name))
					{
						switch (fun)
						{
							case SETTRACE_CALL:
								vtrace(b, flag);
								break;
							case DISPLAY_CALL:
								found += vwatch(b, flag);
								break;
							case SETSTOP_CALL:
								found += vstop(b, flag);
								break;
							case FINDONE_CALL:
								f.vec = b;
								f.num++;
								break;
						}
						found = 1;
						break;
					}
				}
			}
			if (found == 0)
				System.out.println(p + ": No such node or vector");
		}
		return f;
	}

	/*
	 * set/clear input status of node and add/remove it to/from corresponding list.
	 */
	static int irsim_setin(Sim.Node n, String which)
	{
		while((n.nflags & Sim.ALIAS) != 0)
			n = n.nlink;
	
		char wChar = which.charAt(0);
		if ((n.nflags & (Sim.POWER_RAIL | Sim.MERGED)) != 0)	// Gnd, Vdd, or merged node
		{
			String pots = "lxuh";
			if ((n.nflags & Sim.MERGED) != 0 || pots.charAt(n.npot) != wChar)
				System.out.println("Can't drive `" + n.nname + "' to `" + wChar + "'");
		} else
		{
			List list = irsim_listTbl[Sim.INPUT_NUM((int)n.nflags)];
	
			switch(wChar)
			{
				case 'h':
					if (list != null && list != irsim_hinputs)
					{
						n.nflags = n.nflags & ~Sim.INPUT_MASK;
						irsim_idelete(n, list);
					}
					if (! (list == irsim_hinputs || WASINP(n, Sim.HIGH)))
					{
						n.nflags = (n.nflags & ~Sim.INPUT_MASK) | Sim.H_INPUT;
						irsim_iinsert(n, irsim_hinputs);
					}
					break;
	
				case 'l':
					if (list != null && list != irsim_linputs)
					{
						n.nflags = n.nflags & ~Sim.INPUT_MASK;
						irsim_idelete(n, list);
					}
					if (! (list == irsim_linputs || WASINP(n, Sim.LOW)))
					{
						n.nflags = (n.nflags & ~Sim.INPUT_MASK) | Sim.L_INPUT;
						irsim_iinsert(n, irsim_linputs);
					}
					break;
	
				case 'u':
					if (list != null && list != irsim_uinputs)
					{
						n.nflags = n.nflags & ~Sim.INPUT_MASK;
						irsim_idelete(n, list);
					}
					if (! (list == irsim_uinputs || WASINP(n, Sim.X)))
					{
						n.nflags = (n.nflags & ~Sim.INPUT_MASK) | Sim.U_INPUT;
						irsim_iinsert(n, irsim_uinputs);
					}
					break;
	
				case 'x':
					if (list == irsim_xinputs)
						break;
					if (list != null)
					{
						n.nflags = n.nflags & ~Sim.INPUT_MASK;
						irsim_idelete(n, list);
					}
					if ((n.nflags & Sim.INPUT) != 0)
					{
						n.nflags = (n.nflags & ~Sim.INPUT_MASK) | Sim.X_INPUT;
						irsim_iinsert(n, irsim_xinputs);
					}
					break;
	
				default:
					return(0);
			}
		}
		return(1);
	}
	
	
	static boolean WASINP(Sim.Node n, int p)
	{
		return (n.nflags & Sim.INPUT) != 0 && n.npot == p;
	}

	static void irsim_idelete(Sim.Node n, List list)
	{
		list.remove(n);
	}
	
	
	static void irsim_iinsert(Sim.Node n, List list)
	{
		list.add(n);
	}
	
	
	static void irsim_iinsert_once(Sim.Node n, List list)
	{
		if (list.contains(n)) return;	
		irsim_iinsert(n, list);
	}

	static int irsim_info(Sim.Node n, String which)
	{
		if (n == null)
			return(0);
	
		String name = n.nname;
		while((n.nflags & Sim.ALIAS) != 0)
			n = n.nlink;
	
		if ((n.nflags & Sim.MERGED) != 0)
		{
			System.out.println(name + " => node is inside a transistor stack");
			return(1);
		}
	
		String infstr = "";
		infstr += pvalue(name, n);
		if ((n.nflags & Sim.INPUT) != 0)
			infstr += "[NOTE: node is an input] ";
		infstr += "(vl=" + n.vlow + " vh=" + n.vhigh + ") ";
		if ((n.nflags & Sim.USERDELAY) != 0)
			infstr += "(tplh=" + n.tplh + ", tphl=" + n.tphl + ") ";
		infstr += "(" + n.ncap + " pf) ";
		System.out.println(infstr);
	
		infstr = "";
		if (which.startsWith("?"))
		{
			infstr += "is computed from:";
			for(Sim.Tlist l = n.nterm; l != null; l = l.next)
			{
				Sim.Trans t = l.xtor;
				infstr += "  " + ptrans(t);
			}
		} else
		{
			infstr += "affects:";
			for(Sim.Tlist l = n.ngate; l != null; l = l.next)
				infstr += ptrans(l.xtor);
		}
		System.out.println(infstr);
	
		if (n.events != null)
		{
			System.out.println("Pending events:");
			String trans = "0XX1";
			for(Sim.Event e = n.events; e != null; e = e.nlink)
				System.out.println("   transition to " + trans.charAt(e.eval) + " at " + Sim.d2ns(e.ntime)+ "ns");
		}
	
		return(1);
	}

	static String pvalue(String node_name, Sim.Node node)
	{
		char pot = 0;
		switch (node.npot)
		{
			case 0: pot = '0';   break;
			case 1: pot = 'X';   break;
			case 2: pot = 'X';   break;
			case 3: pot = '1';   break;
		}
	    return node_name + "=" + pot + " ";
	}
	
	static String [] states = { "OFF", "ON", "UKNOWN", "WEAK" };

	static String pgvalue(Sim.Trans t)
	{
		String infstr = "";	
		if ((t.ttype & Sim.GATELIST) != 0)
		{
			infstr += "(";
			for(t = (Sim.Trans) t.gate; t != null; t = t.getSTrans())
			{
				Sim.Node n = (Sim.Node)t.gate;
				infstr += pvalue(n.nname, n);
			}
	
			infstr += ") ";
		} else
		{
			Sim.Node n = (Sim.Node)t.gate;
			infstr += pvalue(n.nname, n);
		}
		return infstr;
	}
	
	
	static String pr_one_res(double r)
	{
		String ret = Double.toString(r);
		if (r < 1e-9 || r > 100e9)
			return ret;

		int e = 3;
		if (r >= 1000.0)
			do { e++; r *= 0.001; } while(r >= 1000.0);
		else if (r < 1 && r > 0)
			do { e--; r *= 1000; } while(r < 1.0);
		switch (e)
		{
			case 0: ret += "n";   break;
			case 1: ret += "u";   break;
			case 2: ret += "m";   break;
			case 4: ret += "K";   break;
			case 5: ret += "M";   break;
			case 6: ret += "G";   break;
		}
		return ret;
	}
	
	
	static String pr_t_res(Sim.Resists r)
	{
		String v1 = pr_one_res(r.rstatic);
		String v2 = pr_one_res(r.dynres[Sim.R_HIGH]);
		String v3 = pr_one_res(r.dynres[Sim.R_LOW]);
		return "[" + v1 + ", " + v2 + ", " + v3 + "]";
	}
	
	
	static String ptrans(Sim.Trans t)
	{
		String infstr = Sim.irsim_ttype[Sim.BASETYPE(t.ttype)] + " ";
		if (Sim.BASETYPE(t.ttype) != Sim.RESIST)
			infstr += pgvalue(t);
	
		infstr += pvalue(t.source.nname, t.source);
		infstr += pvalue(t.drain.nname, t.drain);
		infstr += pr_t_res(t.r);
		if (t.tlink != t && (Eval.irsim_treport & Sim.REPORT_TCOORD) != 0)
			infstr += " <" + t.x + "," + t.y + ">";
		return infstr;
	}
	
//	/* visit each node in network, calling function passed as arg with any node
//	 * whose name matches pattern
//	 */
//	int irsim_match_net(CHAR *pattern, int (*fun)(Sim.Node, CHAR*), CHAR *arg)
//	{
//		int   index;
//		Sim.Node  n;
//		int            total = 0;
//	
//		for(index = 0; index < HASHSIZE; index++)
//			for(n = hash[index]; n; n = n.hnext)
//				if (irsim_str_match(pattern, n.nname))
//					total += (*fun)(n, arg);
//	
//		return(total);
//	}

	/* compare pattern with string, case doesn't matter.  "*" wildcard accepted */
	static boolean irsim_str_match(String pStr, String sStr)
	{
		int p = 0, s = 0;
		for(;;)
		{
			if (getCh(pStr, p) == '*')
			{
				// skip past multiple wildcards
				do
					p++;
				while(getCh(pStr, p) == '*');
	
				// if pattern ends with wild card, automatic match
				if (p >= pStr.length())
					return true;
	
				/* *p now points to first non-wildcard character, find matching
				 * character in string, then recursively match remaining pattern.
				 * if recursive match fails, assume current '*' matches more...
				 */
				while(s < sStr.length())
				{
					while(getCh(sStr, s) != getCh(pStr, p))
					{
						s++;
						if (s >= sStr.length()) return false;
					}
					s++;
					if (irsim_str_match(pStr.substring(p+1), sStr.substring(s)))
						return true;
				}
	
				// couldn't find matching character after '*', no match
				return false;
			}
			else if (p >= pStr.length())
				return(s >= sStr.length());
			else if (getCh(pStr, p++) != getCh(sStr, s++))
				break;
		}
		return false;
	}
	
	static int getCh(String s, int index)
	{
		if (index >= s.length()) return 0;
		return Character.toLowerCase(s.charAt(index));
	}

	
	/*
	 * map a character into one of the potentials that a node can be set/compared
	 */
	static int ch2pot(char ch)
	{
		String s = "0ux1lUXhLUXH";
		for(int i = 0; i < s.length(); i++)
			if (s.charAt(i) == ch)
				return(i & (Sim.N_POTS - 1));
	
		System.out.println(ch + ": unknown node value");
		return(Sim.N_POTS);
	}
	
	
	static void CHECK_STOP()
	{
		if (stoped_state)
		{
			System.out.println("Can't do that while stoped, try \"C\"");
			return;
		}
	}
	
	static Sim.Node UnAlias(Sim.Node n)
	{
		while ((n.nflags & Sim.ALIAS) != 0) n = n.nlink;
		return n;
	}

	/*
	 * Set value of a node/vector to the requested value (hlux).
	 */
	static int setvalue(String [] args)
	{
		apply(SETIN_CALL, 0, args, 1);
		return(0);
	}
	
	
	/*
	 * add/delete node to/from display list.
	 */
	static int xwatch(Sim.Node n, String flag)
	{
		n = UnAlias(n);
	
		if ((n.nflags & Sim.MERGED) == 0)
		{
			if (flag.startsWith("+"))
			{
				if (!wlist.contains(n)) wlist.add(n);
			} else
			{
				wlist.remove(n);
			}
		}
		return 1;
	}
	
	
	/*
	 * add/delete vector to/from display list
	 */
	static int vwatch(Sim.Bits b, String flag)
	{
		if (flag.startsWith("+"))
		{
			if (!wvlist.contains(b)) wvlist.add(b);
		} else
		{
			wvlist.remove(b);
		}
		return 1;
	}
	
	
	/* manipulate display list */
	static int display(String [] args)
	{
		apply(DISPLAY_CALL, 0, args, 1);
		return(0);
	}
	
	
	/* display bit vector. */
	static int dvec(Sim.Bits b)
	{
		int i = b.name.length() + 2 + b.nbits;
		if (column + i >= MAXCOL)
		{
			column = 0;
		}
		column += i;
		String bits = "";
		for(i = 0; i < b.nbits; i++)
			bits += Sim.irsim_vchars.charAt(b.nodes[i].npot);
	
		System.out.println(b.name + "=" + bits + " ");
	
		return(1);
	}
	
	
	/* 
	 * print value of specific node
	 */
	static void dnode(Sim.Node n)
	{
		String name = n.nname;
		n = UnAlias(n);
		int i = name.length() + ((n.nflags & Sim.MERGED) != 0 ? 23 : 3);
		if (column + i >= MAXCOL)
		{
			column = 0;
		}
		column += i;
	
		if ((n.nflags & Sim.MERGED) != 0)
			System.out.println(name + "=<in transistor stack> ");
		else
			System.out.println(name + "=" + Sim.irsim_vchars.charAt(n.npot) + " ");
	}
	
	
	/*
	 * print current simulated time and the state of the event list.
	 */
	static void prtime(int col)
	{
		if (col != 0)
			System.out.println("time = " + Sim.d2ns(Sched.irsim_cur_delta) + "ns");
		if (Sched.irsim_npending != 0)
			System.out.println("; there are pending events (" + Sched.irsim_npending + ")");
	}
	
	
	/*
	 * display node/vector values in display list
	 */
	static void pnwatchlist()
	{
		column = 0;
	
		// print value of each watched bit vector.
		for(Iterator it = wvlist.iterator(); it.hasNext(); )
		{
			Sim.Bits b = (Sim.Bits)it.next();
			dvec(b);
		}
	
		// now print value of each watched node.
		for(Iterator it = wlist.iterator(); it.hasNext(); )
		{
			Sim.Node n = (Sim.Node)it.next();
			dnode(n);
		}
	
		prtime(column);
	}
	
	/*
	 * set/clear trace bit in node
	 */
	static int xtrace(Sim.Node n, String flag)
	{
		n = UnAlias(n);
	
		if ((n.nflags & Sim.MERGED) != 0)
		{
			System.out.println("can't trace " + n.nname);
			return(1);
		}
	
		if (flag.startsWith("+"))
			n.nflags |= Sim.WATCHED;
		else if ((n.nflags & Sim.WATCHED) != 0)
		{
			System.out.println(n.nname + " was watched; not any more");
			n.nflags &= ~Sim.WATCHED;
		}
	
		return(1);
	}
	
	
	/*
	 * set/clear trace bit in vector
	 */
	static int vtrace(Sim.Bits b, String flag)
	{
		if (flag.startsWith("+"))
			b.traced |= Sim.WATCHVECTOR;
		else
		{
			for(int i = 0; i < b.nbits; i += 1)
				b.nodes[i].nflags &= ~Sim.WATCHVECTOR;
			b.traced &= ~Sim.WATCHVECTOR;
		}
		return(1);
	}
	
	/*
	 * just in case node appears in more than one bit vector, run through all
	 * the vectors being traced and make sure the flag is set for each node.
	 */
	static void set_vec_nodes(int flag)
	{
		for(Sim.Bits b = blist; b != null; b = b.next)
			if ((b.traced & flag) != 0)
				for(int i = 0; i < b.nbits; i += 1)
					b.nodes[i].nflags |= flag;
	}
	
	
	/*
	 * set/clear stop bit in node
	 */
	static int nstop(Sim.Node n, String flag)
	{
		n = UnAlias(n);
	
		if ((n.nflags & Sim.MERGED) != 0)
			return(1);
	
		if (flag.startsWith("-"))
			n.nflags &= ~Sim.STOPONCHANGE;
		else
			n.nflags |= Sim.STOPONCHANGE;
		return(1);
	}
	
	
	/*
	 * set/clear stop bit in vector
	 */
	static int vstop(Sim.Bits b, String flag)
	{
		if (flag.startsWith("+"))
			b.traced |= Sim.STOPVECCHANGE;
		else
		{
			for(int i = 0; i < b.nbits; i += 1)
				b.nodes[i].nflags &= ~Sim.STOPVECCHANGE;
			b.traced &= ~Sim.STOPVECCHANGE;
		}
		return(1);
	}
	
	
	/*
	 * mark nodes and vectors for tracing
	 */
	static int settrace(String [] args)
	{
		apply(SETTRACE_CALL, 0, args, 1);
		set_vec_nodes(Sim.WATCHVECTOR);
		return(0);
	}
	
	/*
	 * Helper routine for summing capacitance
	 */
	static float sumcapdoit(Sim.Node n, float capsum)
	{
		n = UnAlias(n);
	
		if ((n.nflags & (Sim.MERGED | Sim.ALIAS)) == 0)
			capsum += n.ncap;
	
		return capsum;
	}
	
	
	/*
	 * Print sum of capacitance of nodes
	 */
	static int sumcap()
	{
		float capsum = 0;
	
		System.out.println("Sum of nodal capacitances: ");
		for(Iterator it = Sim.Node.irsim_GetNodeList().iterator(); it.hasNext(); )
		{
			Sim.Node n = (Sim.Node)it.next();
			capsum = sumcapdoit(n, capsum);
		}
		System.out.println(capsum + " pF");
		return(0);
	}


	/*
	 * mark nodes and vectors for stoping
	 */
	static int setstop(String [] args)
	{
		apply(SETSTOP_CALL, 0, args, 1);
		set_vec_nodes(Sim.STOPVECCHANGE);
		return(0);
	}
	
	
	/*
	 * define bit vector
	 */
	static int dovector(String [] args)
	{
//		Sim.Node  n;
//		Sim.Bits  b, last;
//		int   i;
//	
		if (Sim.Node.irsim_find(args[1]) != null)
		{
//			Sim.irsim_error(filename, lineno, "'" + args[1] + "' is a node, can't be a vector");
			return(0);
		}
	
		// get rid of any vector with the same name
//		for(b = blist, last = null; b != null; last = b, b = b.next)
//			if (estrcmp(b.name, targv[1]) == 0)
//		{
//			if (undefseq((Sim.Node) b, &slist, &maxsequence) ||
//				undefseq((Sim.Node) b, &xclock, &maxclock))
//			{
//				Sim.irsim_error(filename, lineno,
//					"%s is a clock/sequence; can't change it while stoped"),
//						b.name);
//				return(0);
//			}
//			irsim_idelete((Sim.Node) b, &wvlist);	// untrace its nodes
//			if (last == null)
//				blist = b.next;
//			else
//				last.next = b.next;		// remove from display list
//			(void)vtrace(b, x_("-"));
//			if (irsim_analyzerON)
//				irsim_RemoveVector(b);
//			efree(b.name);
//			efree((CHAR *)b);
//			break;
//		}
//		Sim.Bits b = (Sim.Bits)emalloc(SIZEOF(Bits) + (args.length - 3) * SIZEOF(Sim.Node), sim_tool.cluster);
//		if ((b.name = (CHAR *)emalloc((estrlen(targv[1]) + 1) * SIZEOFCHAR, sim_tool.cluster)) == null)
//		{
//			if (b) efree((CHAR *)b);
//				Sim.irsim_error(filename, lineno, "Not enough memory for vector");
//			return(0);
//		}
//		b.traced = 0;
//		b.nbits = 0;
//		(void)estrcpy(b.name, targv[1]);
//	
//		for(i = 2; i < args.length; i += 1)
//		{
//			if ((n = NSubrs.irsim_find(targv[i])) == null)
//				Sim.irsim_error(filename, lineno, "cannot find node %s", targv[i]);
//			else
//			{
//				n = UnAlias(n);
//				if (n.nflags & MERGED)
//					Sim.irsim_error(filename, lineno, "%s can not be part of a vector",
//						n.nname);
//				else
//					b.nodes[b.nbits++] = n;
//			}
//		}
//	
//		if (b.nbits == args.length - 2)
//		{
//			b.next = blist;
//			blist = b;
//		} else
//		{
//			efree(b.name);
//			efree((CHAR *)b);
//		}
	
		return(0);
	}
	
	
	/* set bit vector */
	static int setvector(String [] args)
	{
//		CHAR           *val = targv[2];
//	
//		// find vector
//		boolean found = false;
//		Sim.Bits b;
//		for(b = blist; b != null; b = b.next)
//		{
//			if (b.name.equalsIgnoreCase(args[1]))
//			{
//				found = true;
//				break;
//			}
//		}
//		if (!found)
//		{
//			Sim.irsim_error(filename, lineno, args[1] + ": No such vector");
//			return(0);
//		}
//
//		// set nodes
//		if (args[2].length() != b.nbits)
//		{
//			Sim.irsim_error(filename, lineno, "wrong number of bits for this vector");
//			return(0);
//		}
//		for(int i = 0; i < b.nbits; i++)
//		{
//			if ((val[i] = potchars.charAt(ch2pot(val[i]))) == '.')
//				return(0);
//		}
//		for(int i = 0; i < b.nbits; i++)
//			irsim_setin(b.nodes[i], val++);
		return(0);
	}
	
	
	static int CompareVector(Sim.Node [] np, String name, int nbits, String mask, String value)
	{
		if (value.length() != nbits)
		{
			System.out.println("wrong number of bits for value");
			return 0;
		}
		if (mask != null && mask.length() != nbits)
		{
			System.out.println("wrong number of bits for mask");
			return 0;
		}
	
		for(int i = 0; i < nbits; i++)
		{
			if (mask != null && mask.charAt(i) != '0') continue;
			Sim.Node n = np[i];
			int val = ch2pot(value.charAt(i));
			if (val >= Sim.N_POTS)
				return 0;
			if (val == Sim.X_X) val = Sim.X;
				if (n.npot != val)
					return 1;
		}
		return 0;
	}
	
	
	static class Find1Arg
	{
		Sim.Node  node;
		Sim.Bits  vec;
		int   num;
	};
	
	/* assert a bit vector */
	static int doAssert(String [] args)
	{
		String mask = null;
		StringBuffer value = null;
		if (args.length == 4)
		{
			mask = args[2];
			value = new StringBuffer(args[3]);
		} else
		{
			value = new StringBuffer(args[2]);
		}
	
		Find1Arg f = (Find1Arg)apply(FINDONE_CALL, 0, args, 1);

		int comp = 0, nbits = 0;
		String name = null;
		Sim.Node [] nodes = null;
		if (f.num > 1)
			System.out.println(args[1] + " matches more than one node or vector");
		else if (f.node != null)
		{
			name = f.node.nname;
			f.node = UnAlias(f.node);
			Sim.Node [] nodeList = new Sim.Node[1];
			nodeList[0] = f.node;
			comp = CompareVector(nodeList, name, 1, mask, value.toString());
			nodes = nodeList;
			nbits = 1;
		}
		else if (f.vec != null)
		{
			comp = CompareVector(f.vec.nodes, f.vec.name, f.vec.nbits, mask, value.toString());
			name = f.vec.name;
			nbits = f.vec.nbits;
			nodes = f.vec.nodes; 
		}
		if (comp != 0)
		{
			System.out.println("assertion failed on '" + name + "' ");
			String infstr = "";
			for(int i = 0; i < nbits; i++)
			{
				if (i < mask.length() && mask.charAt(i) != '0')
				{
					infstr += "-";
					value.setCharAt(i, '-');
				}
				else
					infstr += Sim.irsim_vchars.charAt(nodes[i].npot);
			}
			System.out.println("Want (" + value + ") but got (" + infstr + ")");
		}
		return(0);
	}
	
	
	
	static int doUntil(String [] args)
	{
		String mask = null;
		StringBuffer value = null;
		int ccount = 0;
		if (args.length == 5)
		{
			mask = args[2];
			value = new StringBuffer(args[3]);
			ccount = TextUtils.atoi(args[4]);
		} else
		{
			mask = null;
			value = new StringBuffer(args[2]);
			ccount = TextUtils.atoi(args[3]);
		}
	
		Find1Arg f = (Find1Arg)apply(FINDONE_CALL, 0, args, 1);
		String name = null;
		int comp = 0;
		int nbits = 0;
		Sim.Node [] nodes = null;
		if (f.num > 1)
			System.out.println(args[1] + " matches more than one node or vector");
		else if (f.node != null)
		{
			name = f.node.nname;
			f.node = UnAlias(f.node);
			Sim.Node [] nodeList = new Sim.Node[1];
			nodeList[0] = f.node;
			int cnt = 0;
			while ((cnt <= ccount) && (comp = CompareVector(nodeList, name, 1, mask, value.toString())) != 0)
			{
				cnt++;
				clockit(1);
			}
			nodes = new Sim.Node[1];
			nodes[0] = f.node;
			nbits = 1;
		}
		else if (f.vec != null)
		{
			int cnt = 0;
			while ((cnt <= ccount) && (comp = CompareVector(f.vec.nodes, f.vec.name, f.vec.nbits, mask,
				value.toString())) != 0)
			{
				cnt++;
				clockit(1);
			}
			name = f.vec.name;
			nbits = f.vec.nbits;
			nodes = f.vec.nodes;
		}
		if (comp != 0)
		{
			System.out.println("assertion failed on '" + name + "' ");
			String infstr = "";
			for(int i = 0; i < nbits; i++)
			{
				if (mask != null && mask.charAt(i) != '0')
				{
					infstr += "-";
					value.setCharAt(i, '-');
				}
				else
					infstr += Sim.irsim_vchars.charAt(nodes[i].npot);
			}
			System.out.println("Want (" + value + ") but got (" + infstr + ")");
		}
		return(0);
	}
	
	
	static	Sim.Node aw_trig; /* keeps current AssertWhen trigger */
	static AssertWhen aw_p;   /* track pointer on the current AssertWhen list */
	
	static int setupAssertWhen(Sim.Node n, String val)
	{
		AssertWhen p = new AssertWhen();
		p.node = n;
		p.val  = val.charAt(0);
		p.nxt = null;
	
		if (aw_trig.awpending == null)
		{
			// first time
			aw_trig.awpending = p;
			aw_p = p;
		} else
		{
			// more than 1 matching nodes
			aw_p.nxt = p;
			aw_p = p;
		}
		return 1;
	}
	
	static int doAssertWhen(String [] args)
	{
		Find1Arg trig = (Find1Arg)apply(FINDONE_CALL, 0, args, 1);
	
		if (trig.num > 1)
			System.out.println(args[1] + " matches more than one node or vector");
		else if (trig.node != null)
		{	
			trig.node = UnAlias(trig.node);
			aw_trig = trig.node;
			aw_trig.awpot = (short)ch2pot(args[2].charAt(0));
			apply(ASSERTWHEN_CALL, 0, args, 3);
		}
		else if (trig.vec != null) 
			System.out.println("trigger to assertWhen " + args[1] + " can't be a vector");
		return(0);
	}
	
	static void	irsim_evalAssertWhen(Sim.Node n)
	{
		for (AssertWhen p = n.awpending; p != null; )
		{
			String name = p.node.nname;
			StringBuffer sb = new StringBuffer();
			sb.append((char)p.val);
			Sim.Node [] nodes = new Sim.Node[1];
			nodes[0] = p.node;
			int comp = CompareVector(nodes, name, 1, null, sb.toString());
			if (comp != 0) 
				System.out.println("assertion failed on '" + name + "'");
			p = p.nxt;
		}
		n.awpending = null;
	}
	
	
	
	
	static int collect_inputs(Sim.Node n, Sim.Node [] inps)
	{
		if ((n.nflags & (Sim.INPUT|Sim.ALIAS|Sim.POWER_RAIL|Sim.VISITED|Sim.INPUT_MASK)) == Sim.INPUT)
		{
			n.setNext(inps[n.npot]);
			inps[n.npot] = n;
			n.nflags |= Sim.VISITED;
		}
		return(0);
	}
	
	
	/* display current inputs */
	static int inputs(String [] args)
	{
		Sim.Node [] inptbl = new Sim.Node[Sim.N_POTS];
	
		inptbl[Sim.HIGH] = inptbl[Sim.LOW] = inptbl[Sim.X] = null;
		for(Iterator it = Sim.Node.irsim_GetNodeList().iterator(); it.hasNext(); )
		{
			Sim.Node n = (Sim.Node)it.next();
			collect_inputs(n, inptbl);
		}

		System.out.print("h inputs:");
		for(Iterator it = irsim_hinputs.iterator(); it.hasNext(); )
		{
			Sim.Node n = (Sim.Node)it.next();
			System.out.print(" " + n.nname);
		}
		for(Sim.Node n = inptbl[Sim.HIGH]; n != null; n.nflags &= ~Sim.VISITED, n = n.getNext())
			System.out.print(" " + n.nname);
		System.out.println();

		System.out.print("l inputs:");
		for(Iterator it = irsim_linputs.iterator(); it.hasNext(); )
		{
			Sim.Node n = (Sim.Node)it.next();
			System.out.print(" " + n.nname);
		}
		for(Sim.Node n = inptbl[Sim.LOW]; n != null; n.nflags &= ~Sim.VISITED, n = n.getNext())
			System.out.print(" " + n.nname);
		System.out.println();

		System.out.println("u inputs:");
		for(Iterator it = irsim_uinputs.iterator(); it.hasNext(); )
		{
			Sim.Node n = (Sim.Node)it.next();
			System.out.println(" " + n.nname);
		}
		for(Sim.Node n = inptbl[Sim.X]; n != null; n.nflags &= ~Sim.VISITED, n = n.getNext())
			System.out.println(" " + n.nname);
		System.out.println();
		return(0);
	}
	
	
	/* set stepsize */
	static int setstep(String [] args)
	{
		if (args.length == 1)
			System.out.println("stepsize = " + Sim.d2ns(stepsize));
		else if (args.length == 2)
		{
			long  newsize = Sim.ns2d(TextUtils.atof(args[1]));
	
			if (newsize <= 0)
			{
				System.out.println("bad step size: " + args[1]);
			} else
				stepsize = newsize;
		}
		return(0);
	}
	
	
	/*
	 * Display traced vectors that just changed.  There should be at least one.
	 */
	static void irsim_disp_watch_vec(long which)
	{
		which &= (Sim.WATCHVECTOR | Sim.STOPVECCHANGE);
		String temp = " @ " + Sim.d2ns(Sched.irsim_cur_delta) + "ns ";
		System.out.println(temp);
		column = temp.length();
		for(Sim.Bits b = blist; b != null; b = b.next)
		{
			if ((b.traced & which) == 0)
				continue;
			int i;
			for(i = b.nbits - 1; i >= 0; i--)
				if (b.nodes[i].getTime() == Sched.irsim_cur_delta)
					break;
			if (i >= 0)
				dvec(b);
		}
	}
	
	
	/* 
	 * Settle network until the specified stop time is reached.
	 * Premature returns (before stop time) indicate that a node/vector whose
	 * stop-bit set has just changed value, so popup a stdin command interpreter.
	 */
	static long relax(long stoptime)
	{
		while (Eval.irsim_step(stoptime))
		{
		}
		return(Sched.irsim_cur_delta - stoptime);
	}
	
	
	/*
	 * relax network, optionally set stepsize
	 */
	static int dostep(String [] args)
	{
		CHECK_STOP();
	
		long newsize = stepsize;
		if (args.length == 2)
		{
			newsize = Sim.ns2d(TextUtils.atof(args[1]));
			if (newsize <= 0)
			{
				System.out.println("bad step size: " + args[1]);
				return(0);
			}
		}
	
		relax(Sched.irsim_cur_delta + newsize);
		if (ddisplay)
			pnwatchlist();
	
		return(0);
	}
	
	
	/*
	 * destroy sequence for given node/vector: update sequence list and length.
	 * return -1 if we can't destroy the sequence (in stopped state).
	 */
	static Sequence undefseq(Object p, Sequence list, GenMath.MutableInteger lmax)
	{
		Sequence u, t;
		for(u=null, t = list; t != null; u = t, t = t.next)
			if (t.n == p)
				break;
		if (t != null)
		{
			if (stoped_state)	// disallow changing sequences if stoped
				return list;
			if (u == null)
				list = t.next;
			else
				u.next = t.next;
			int i = 0;
			for(t = list; t != null; t = t.next)
				if (t.nvalues > i) i = t.nvalues;
					lmax.setValue(i);
		}
		return list;
	}
	
	
	/*
	 * process command line to yield a sequence structure.  first arg is the
	 * name of the node/vector for which the sequence is to be defined, second
	 * and following args are the values.
	 */
	static Sequence defsequence(String [] args, Sequence list, GenMath.MutableInteger lmax)
	{
		// if no arguments, get rid of all the sequences we have defined
		if (args.length == 1)
		{
			while (list != null)
				list = undefseq(list.n, list, lmax);
			return list;
		}
	
		// see if we can determine if name is for node or vector
		boolean isOK = false;
		int which = 0, size = 0;
		Sim.Bits b = null;
		Sim.Node n = null;
		for(b = blist; b != null; b = b.next)
			if (b.name.equalsIgnoreCase(args[1]))
		{
			which = 1;    size = b.nbits;    isOK = true;   break;
		}
		if (!isOK)
		{
			n = Sim.Node.irsim_find(args[1]);
			if (n == null)
			{
				System.out.println(args[0] + ": No such node or vector");
				return list;
			}
			n = UnAlias(n);
			if ((n.nflags & Sim.MERGED) != 0)
			{
				System.out.println(n.nname + " can't be part of a sequence");
				return list;
			}
			which = 0; size = 1;
		}
	
		if (args.length == 2)	// just destroy the given sequence
		{
			list = undefseq(which != 0 ? b : n, list, lmax);
			return list;
		}
	
		// make sure each value specification is the right length
		for(int i = 2; i < args.length; i += 1)
			if (args[i].length() != size)
		{
			System.out.println("value \"" + args[i] + "\" is not compatible with size of " + args[2] + " (" + size + ")");
			return list;
		}
	
		Sequence s = new Sequence();
		s.values = new char[args.length-1];
		s.which = which;
		s.vsize = size;
		s.nvalues = args.length - 2;
		if (which != 0)	s.b = b;
			else	s.n = n;
	
		// process each value specification saving results in sequence
		int q = 0;
		for(int i = 2; i < args.length; i += 1)
		{
			for(int p=0; p<args[i].length(); p++)
				if ((s.values[q++] = potchars.charAt(ch2pot(args[i].charAt(p)))) == '.')
			{
				return list;
			}
		}
	
		// all done!  remove any old sequences for this node or vector.
		list = undefseq(s.n, list, lmax);
	
		// insert result onto list
		s.next = list;
		list = s;
		if (s.nvalues > lmax.intValue())
			lmax.setValue(s.nvalues);
		return list;
	}
	
	
	/*
	 * mark any vector that contains a deleted node (used by netupdate).
	 */
	static int mark_deleted_vectors()
	{
		int total = 0;
	
		for(Sim.Bits b = blist; b != null; b = b.next)
		{
			for(int i = b.nbits - 1; i >= 0; i--)
			{
				if ((b.nodes[i].nflags & Sim.DELETED) != 0)
				{
					b.traced = Sim.DELETED;
					total ++;
					break;
				}
				b.nodes[i] = UnAlias(b.nodes[i]);
			}
		}
		return(total);
	}
	
	
//	/*
//	 * Remove all deleted nodes/vectors from the sequence list
//	 */
//	static int rm_from_seq(Sequence *list)
//	{
//		Sequence  s;
//		int   max;
//	
//		max = 0;
//		while ((s = *list) != null)
//		{
//			if (((s.which) ? s.ptr.b.traced : s.ptr.n.nflags) & DELETED)
//			{
//				*list = s.next;
//				efree((CHAR *)s);
//			} else
//			{
//				if (s.which == 0)
//					s.ptr.n = UnAlias(s.ptr.n);
//	
//				if (s.nvalues > max)
//					max = s.nvalues;
//				list = &(s.next);
//			}
//		}
//		return(max);
//	}
	
	
	/*
	 * Remove any deleted node/vector from any lists in which it may appear.
	 */
	static void irsim_rm_del_from_lists()
	{
//		Sim.Input  w, *list;
//		int            vec_del;
	
		int vec_del = mark_deleted_vectors();
	
//		maxsequence = rm_from_seq(&slist);
//		maxclock = rm_from_seq(&xclock);
	
		if (irsim_analyzerON)
			Analyzer.irsim_RemoveAllDeleted();
	
		List newWVList = new ArrayList();
		for(Iterator it = wvlist.iterator(); it.hasNext(); )
		{
			Sim.Bits b = (Sim.Bits)it.next();
			if ((b.traced & Sim.DELETED) == 0) newWVList.add(b);
		}
		wvlist = newWVList;
	
		List newWList = new ArrayList();
		for(Iterator it = wlist.iterator(); it.hasNext(); )
		{
			Sim.Node n = (Sim.Node)it.next();
			if ((n.nflags & Sim.DELETED) == 0) newWList.add(n);
		}
		wlist = newWList;
	
//		if (vec_del)
//		{
//			Sim.Bits  b, *lst;
//	
//			for(lst = &blist; (b = *lst) != null;)
//			{
//				if (b.traced & DELETED)
//				{
//					*lst = b.next;
//					efree(b.name);
//					efree((CHAR *)b);
//				}
//				else
//					lst = &b.next;
//			}
//		}
	}
	
	
	/*
	 * set each node/vector in sequence list to its next value
	 */
	static void vecvalue(RSim.Sequence list, int index)
	{
		for(; list != null; list = list.next)
		{
			int offset = list.vsize * (index % list.nvalues);
//			Sim.Node n = (list.which == 0) ? list.n : list.b.nodes;
//			for(int i = 0; i < list.vsize; i++)
//				irsim_setin(*n++, &list.values[offset++]);
		}
	}
	
	
	/*
	 * setup sequence of values for a node
	 */
	static int setseq(String [] args)
	{
		CHECK_STOP();
		// process sequence and add to list
		GenMath.MutableInteger mi = new GenMath.MutableInteger(maxsequence);
		slist = defsequence(args, slist, mi);
		maxsequence = mi.intValue();
		return(0);
	}
	
	
	/*
	 * define clock sequences(s)
	 */
	static int setclock(String [] args)
	{
		CHECK_STOP();
		// process sequence and add to clock list
		GenMath.MutableInteger mi = new GenMath.MutableInteger(maxclock);
		xclock = defsequence(args, xclock, mi);
		maxclock = mi.intValue();
		return(0);
	}
	
	
	/*
	 * Step each clock node through one simulation step
	 */
	static int  which_phase = 0;
	static int step_phase()
	{
		vecvalue(xclock, which_phase);
		if (relax(Sched.irsim_cur_delta + stepsize) != 0)
			return(1);
		which_phase = (which_phase + 1) % maxclock;
		return(0);
	}
	
	
	/* Do one simulation step */
	static int dophase(String [] args)
	{
		CHECK_STOP();
	
		step_phase();
		if (ddisplay)
			pnwatchlist();
	
		return(0);
	}
	
	
	/*
	 * clock circuit specified number of times
	 */
	static int clockit(int n)
	{
		int  i = 0;
	
		if (xclock == null)
		{
			System.out.println("no clock nodes defined!");
		} else
		{
			/* run 'em by setting each clock node to successive values of its
			 * associated sequence until all phases have been run.
			 */
			while (n-- > 0)
			{
				for(i = 0; i < maxclock; i += 1)
				{
					if (step_phase() != 0)
					{
						n = 0;
						break;
					}
				}
			}
	
			// finally display results if requested to do so
			if (ddisplay)
				pnwatchlist();
		}
		return(maxclock - i);
	}
	
	
	/*
	 * clock circuit through all the input vectors previously set up
	 */
	static int runseq(String [] args)
	{
		CHECK_STOP();
	
		// calculate how many clock cycles to run
		int n = 1;
		if (args.length == 2)
		{
			n = TextUtils.atoi(args[1]);
			if (n <= 0)
				n = 1;
		}
	
		/* run 'em by setting each input node to successive values of its
		 * associated sequence.
		 */
		if (slist == null)
		{
			System.out.println("no input vectors defined!");
		} else
			while (n-- > 0)
				for(int i = 0; i < maxsequence; i += 1)
		{
			vecvalue(slist, i);
			if (clockit(1) != 0)
				return(0);
			if (ddisplay)
				pnwatchlist();
		}
	
		return(0);
	}
	
	
	/*
	 * process "c" command line
	 */
	static int doclock(String [] args)
	{
		if (stoped_state)		// continue after stop
			return(1);
	
		// calculate how many clock cycles to run
		int  n = 1;
		if (args.length == 2)
		{
			n = TextUtils.atoi(args[1]);
			if (n <= 0)
				n = 1;
		}
	
		clockit(n);		// do the hard work
		return(0);
	}
	
	
	/*
	 * output message to console/log file
	 */
	static int domsg(String [] args)
	{
		String infstr = "";
		for(int n=1; n<args.length; n++)
		{
			if (n != 1) infstr += " ";
			infstr += args[n];
		}
		System.out.println(infstr);
		return(0);
	}
	
	
	/*
	 * Return a number whose bits corresponding to the index of 'words' match
	 * the argument.
	 * If 'offwrd' is given then that argument turns all bits off.
	 * If 'offwrd' is given and the argument is '*' turns all bits on.
	 * if the argument is '?' the display the avaliable options (words).
	 * With no arguments just prints the word whose corresponding bit is set.
	 */
	static int do_flags(String [] args, int bits, String name, String offwrd, String [] words)
	{
		if (args.length == 1)
		{
			System.out.println(name + ": ");
			if (bits == 0 && offwrd != null)
				System.out.println(offwrd);
			else
			{
				for(int i = 0; words[i] != null; i++)
					if ((bits & (1 << i)) != 0)
						System.out.println(" " + words[i]);
			}
		}
		else if (args.length == 2 && args[1].equals("?"))
		{
			System.out.println(name + " options are:");
			if (offwrd != null)
				System.out.println("[*][" + offwrd + "]");
			for(int t = '[', i = 0; words[i] != null; i++, t = ' ')
				System.out.println(t + words[i]);
			System.out.println("]");
		}
		else if (args.length == 2 && offwrd != null && args[1].equalsIgnoreCase(offwrd))
		{
			bits = 0;
		}
		else if (args.length == 2 && offwrd != null && args[1].equals("*"))
		{
			int i;
			for(i = 0; words[i] != null; i++);
			bits = (1 << i) - 1;
		}
		else
		{
			int t = 1, tmp = bits;
			for(bits = 0; t < args.length; t++)
			{
				int i;
				for(i = 0; words[i] != null; i++)
					if (words[i].equalsIgnoreCase(args[t]))
				{
					bits |= (1 << i);
					break;
				}
				if (words[i] == null)
				{
					System.out.println(args[t] + ": Invalid " + name + " option");
					bits = tmp;
					break;
				}
			}
		}
		return(bits);
	}
	

	static String [] rep = new String[] { "decay", "delay", "tau", "tcoord" };

	/*
	 * set irsim_treport parameter
	 */
	static int setreport(String [] args)
	{
		Eval.irsim_treport = do_flags(args, Eval.irsim_treport, "report", "none", rep);
		return(0);
	}
	
	
	/*
	 * set which evaluation model to use
	 */
	static int setmodel(String [] args)
	{
		if (args[0].equals("linear"))
		{
			Eval.irsim_model = NewRStep.linearModel;
		} else if (args[0].equals("switch"))
		{
			Eval.irsim_model = SStep.switchModel;
		}
		return(0);
	}
	

	/*
	 * display info about a node
	 */
	static int quest(String [] args)
	{
		apply(QUEST_CALL, 0, args, 1);
		return(0);
	}

	/*
	 * set decay parameter
	 */
	static int setdecay(String [] args)
	{
		if (args.length == 1)
		{
			if (NewRStep.irsim_tdecay == 0)
				System.out.println("decay = No decay");
			else
				System.out.println("decay = " + Sim.d2ns(NewRStep.irsim_tdecay) + "ns");
		} else
		{
			NewRStep.irsim_tdecay = Sim.ns2d(TextUtils.atof(args[1]));
			if (NewRStep.irsim_tdecay < 0)
				NewRStep.irsim_tdecay = 0;
		}
		return(0);
	}
	
	
	/*
	 * set unitdelay parameter
	 */
	static int setunit(String [] args)
	{
		if (args.length == 1)
		{
			if (NewRStep.irsim_tunitdelay == 0)
				System.out.println("unitdelay = OFF");
			else
				System.out.println("unitdelay = " + Sim.d2ns(NewRStep.irsim_tunitdelay));
		} else
		{
			NewRStep.irsim_tunitdelay = (int) Sim.ns2d(TextUtils.atof(args[1]));
			if (NewRStep.irsim_tunitdelay < 0)
				NewRStep.irsim_tunitdelay = 0;
		}
		return(0);
	}
	
	static long ptime;
	/*
	 * print traceback of node's activity and that of its ancestors
	 */
	static void cpath(Sim.Node n, int level)
	{
		// no last transition!
		if ((n.nflags & Sim.MERGED) != 0 || n.getCause() == null)
		{
			System.out.println("  there is no previous transition!");
		}
	
		/* here if we come across a node which has changed more recently than
		 * the time reached during the backtrace.  We can't continue the
		 * backtrace in any reasonable fashion, so we stop here.
		 */
		else if (level != 0 && n.getTime() > ptime)
		{
			System.out.println("  transition of " + n.nname + ", which has since changed again");
		}
		/* here if there seems to be a cause for this node's transition.
		 * If the node appears to have 'caused' its own transition (n.t.cause
		 * == n), that means it was input.  Otherwise continue backtrace...
		 */
		else if (n.getCause() == n)
		{
			System.out.println("  " + n.nname + " . " + Sim.irsim_vchars.charAt(n.npot) +
				" @ " + Sim.d2ns(n.getTime()) + "ns , node was an input");
		}
		else if ((n.getCause().nflags & Sim.VISITED) != 0)
		{
			System.out.println("  ... loop in traceback");
		}
		else
		{
			long  delta_t = n.getTime() - n.getCause().getTime();
	
			n.nflags |= Sim.VISITED;
			ptime = n.getTime();
			cpath(n.getCause(), level + 1);
			n.nflags &= ~Sim.VISITED;
			if (delta_t < 0)
				System.out.println("  " + n.nname + " . " + Sim.irsim_vchars.charAt(n.npot) +
					" @ " + Sim.d2ns(n.getTime()) + "ns   (??)");
			else
				System.out.println("  " + n.nname + " . " + Sim.irsim_vchars.charAt(n.npot) +
					" @ " + Sim.d2ns(n.getTime()) + "ns   (" + Sim.d2ns(delta_t) + "ns)");
		}
	}
	
	
	static int do_cpath(Sim.Node n)
	{
		System.out.println("critical path for last transition of " + n.nname + ":");
		n = UnAlias(n);
		cpath(n, 0);
		return(1);
	}
	
	
	/*
	 * discover and print critical path for node's last transistion
	 */
	static int dopath(String [] args)
	{
		apply(PATH_CALL, 0, args, 1);
		return(0);
	}
	
	
	static final int	NBUCKETS		= 20;	/* number of buckets in histogram */
	
	static class Accounts
	{
		long  begin, end, size;
		long  [] table;
		Accounts() { table = new long[NBUCKETS]; }
	};
	
	
	/*
	 * print histogram of circuit activity in specified time interval
	 */
	static int doactivity(String [] args)
	{
//		static CHAR   st[] = {x_("**************************************************")};
//	#   define	SIZE_ST		(sizeof(st) - 1)

		Accounts      ac = new Accounts();
		if (args.length == 2)
		{
			ac.begin = Sim.ns2d(TextUtils.atof(args[1]));
			ac.end = Sched.irsim_cur_delta;
		} else
		{
			ac.begin = Sim.ns2d(TextUtils.atof(args[1]));
			ac.end = Sim.ns2d(TextUtils.atof(args[2]));
		}
	
		if (ac.end < ac.begin)
		{
			long swp = ac.end;   ac.end = ac.begin;   ac.begin = swp;
		}
	
		// collect histogram info by walking the network
		for(int i = 0; i < NBUCKETS; ac.table[i++] = 0);
	
		ac.size = (ac.end - ac.begin + 1) / NBUCKETS;
		if (ac.size <= 0)
			ac.size = 1;
	
		for(Iterator it = Sim.Node.irsim_GetNodeList().iterator(); it.hasNext(); )
		{
			Sim.Node n = (Sim.Node)it.next();
			if ((n.nflags & (Sim.ALIAS | Sim.MERGED | Sim.POWER_RAIL)) == 0)
			{
				if (n.getTime() >= ac.begin && n.getTime() <= ac.end)
					ac.table[(int)((n.getTime() - ac.begin) / ac.size)] += 1;
			}
		}

		// print out what we found
		int total = 0;
		for(int i = 0; i < NBUCKETS; i++) total += ac.table[i];
	
		System.out.println("Histogram of circuit activity: " + Sim.d2ns(ac.begin) +
				" . " + Sim.d2ns(ac.end) + "ns (bucket size = " + Sim.d2ns(ac.size) + ")");
	
//		for(int i = 0; i < NBUCKETS; i += 1)
//			System.out.println(" " + Sim.d2ns(ac.begin + (i * ac.size)) + " -" + Sim.d2ns(ac.begin + (i + 1) * ac.size) +
//				ac.table[i] + "  " + st[SIZE_ST - (SIZE_ST * ac.table[i]) / total]);
	
		return(0);
	}
	
	
	/*
	 * Print list of nodes which last changed value in specified time interval
	 */
	static int dochanges(String [] args)
	{
		Accounts  ac = new Accounts();
	
		if (args.length == 2)
		{
			ac.begin = Sim.ns2d(TextUtils.atof(args[1]));
			ac.end = Sched.irsim_cur_delta;
		} else
		{
			ac.begin = Sim.ns2d(TextUtils.atof(args[1]));
			ac.end = Sim.ns2d(TextUtils.atof(args[2]));
		}
	
		column = 0;
		System.out.print("Nodes with last transition in interval " + Sim.d2ns(ac.begin) + " . " + Sim.d2ns(ac.end) + "ns:");
	
		for(Iterator it = Sim.Node.irsim_GetNodeList().iterator(); it.hasNext(); )
		{
			Sim.Node n = (Sim.Node)it.next();
			n = UnAlias(n);
			
			if ((n.nflags & (Sim.MERGED | Sim.ALIAS)) != 0)
				return(0);
		
			if (n.getTime() >= ac.begin && n.getTime() <= ac.end)
			{
				int i = n.nname.length() + 2;
				if (column + i >= MAXCOL)
				{
					column = 0;
				}
				column += i;
				System.out.print("  " + n.nname);
			}
		}
		System.out.println();
		return(0);
	}
	
	
	/*
	 * Print list of nodes with undefined (X) value
	 */
	static int doprintX(String [] args)
	{
		System.out.print("Nodes with undefined potential:");
		column = 0;
		for(Iterator it = Sim.Node.irsim_GetNodeList().iterator(); it.hasNext(); )
		{
			Sim.Node n = (Sim.Node)it.next();
			n = UnAlias(n);
			
			if ((n.nflags & (Sim.MERGED | Sim.ALIAS)) == 0 && n.npot == Sim.X)
			{
				int i = n.nname.length() + 2;
				if (column + i >= MAXCOL)
				{
					column = 0;
				}
				column += i;
				System.out.print("  " + n.nname);
			}
		}
		System.out.println();
		return(0);
	}
	
	/*
	 * Print nodes that are aliases
	 */
	static int doprintAlias(String [] args)
	{
		if (Sim.irsim_naliases == 0)
			System.out.println("there are no aliases");
		else
		{
			System.out.println("there are " + Sim.irsim_naliases + " aliases:");
			for(Iterator it = Sim.Node.irsim_GetNodeList().iterator(); it.hasNext(); )
			{
				Sim.Node n = (Sim.Node)it.next();
				if ((n.nflags & Sim.ALIAS) != 0)
				{
					n = UnAlias(n);
					String is_merge = (n.nflags & Sim.MERGED) != 0 ? " (part of a stack)" : "";
					System.out.println("  " + n.nname + " . " + n.nname + is_merge);
				}
			}
		}
		return(0);
	}
	
	
	/*
	 * Helper routine to print pending events
	 */
	static int print_list(int n, Sim.Event l, Sim.Event eolist)
	{
		if (l == null)
			return(n);
		for(eolist = eolist.flink; l != eolist && n != 0; l = l.flink, n--)
		{
			System.out.println("Node " + l.enode.nname + " . " + Sim.irsim_vchars.charAt(l.eval) +
				" @ " + Sim.d2ns(l.ntime) + "ns (" + Sim.d2ns(l.ntime - Sched.irsim_cur_delta) + "ns)");
		}
		return(n);
	}
	
	/*
	 * Print list of transistors with src/drn shorted (or between power supplies).
	 */
	static int print_tcap(String [] args)
	{
		if (Sim.irsim_tcap.getSTrans() == Sim.irsim_tcap)
			System.out.println("there are no shorted transistors");
		else
			System.out.println("shorted transistors:");
		for(Sim.Trans t = Sim.irsim_tcap.getSTrans(); t != Sim.irsim_tcap; t = t.getSTrans())
		{
			System.out.println(" " + Sim.irsim_ttype[Sim.BASETYPE(t.ttype)] + " g=" + ((Sim.Node)t.gate).nname + " s=" +
					t.source.nname + " d=" + t.drain.nname + " (" +
					(t.r.length / Config.irsim_LAMBDACM) + "x" + (t.r.width / Config.irsim_LAMBDACM) + ")");
		}
		return(0);
	}


	/*
	 * Move back simulation time to specified time.
	 */
	static int back_time(String [] args)
	{
		CHECK_STOP();
	
		long newt = Sim.ns2d(TextUtils.atof(args[1]));
		if (newt < irsim_sim_time0 || newt > Sched.irsim_cur_delta)
		{
			System.out.println(args[1] + ": invalid time");
			return(0);
		}
	
		Sched.irsim_cur_delta = newt;
		irsim_ClearInputs();
		Sched.irsim_back_sim_time(Sched.irsim_cur_delta, 0);
		Sched.irsim_cur_node = null;			// fudge
		List nodes = Sim.Node.irsim_GetNodeList();
		for(Iterator it = nodes.iterator(); it.hasNext(); )
		{
			Sim.Node n = (Sim.Node)it.next();
			Hist.irsim_backToTime(n);
		}
		if (Sched.irsim_cur_delta == 0)
			Eval.irsim_ReInit();
	
		if (irsim_analyzerON)
			Analyzer.irsim_RestartAnalyzer(irsim_sim_time0, Sched.irsim_cur_delta, 1);
	
		pnwatchlist();
		return(0);
	}
	
	static void irsim_ClearInputs()
	{
		for(int i = 0; i < 5; i++)
		{
			if (irsim_listTbl[i] == null)
				continue;
			for(Iterator it = irsim_listTbl[i].iterator(); it.hasNext(); )
			{
				Sim.Node n = (Sim.Node)it.next();
				if ((n.nflags & Sim.POWER_RAIL) == 0)
					n.nflags &= ~(Sim.INPUT_MASK | Sim.INPUT);
			}
			irsim_listTbl[i] = null;
		}
		for(Iterator it = Sim.Node.irsim_GetNodeList().iterator(); it.hasNext(); )
		{
			Sim.Node n = (Sim.Node)it.next();
			clear_input(n);
		}
	}
	
	static int clear_input(Sim.Node n)
	{
		if ((n.nflags & Sim.POWER_RAIL) == 0)
			n.nflags &= ~Sim.INPUT;
		return(0);
	}

	static int tranCntNSD = 0, tranCntNG = 0;

	/*
	 * Print event statistics.
	 */
	static int do_stats(String [] args)
	{
		if (args.length == 2)
		{	
			if (tranCntNG == 0 && tranCntNSD == 0)
			{
				for(Iterator it = Sim.Node.irsim_GetNodeList().iterator(); it.hasNext(); )
				{
					Sim.Node n = (Sim.Node)it.next();
					if ((n.nflags & (Sim.ALIAS | Sim.POWER_RAIL)) == 0)
					{
						int i = 0;
						for(Sim.Tlist l = n.ngate; l != null; l = l.next, i++);
						tranCntNG += i;
						i = 0;
						for(Sim.Tlist l = n.nterm; l != null; l = l.next, i++);
						tranCntNSD += i;
					}
				}
				System.out.println("avg: # gates/node = " + Double.toString(tranCntNG / Sim.irsim_nnodes) +
					",  # src-drn/node = " + Double.toString(tranCntNSD / Sim.irsim_nnodes));
			}
		}
		System.out.println("changes = " + Hist.irsim_num_edges);
		System.out.println("punts (cns) = " + Hist.irsim_num_punted + " (" + Hist.irsim_num_cons_punted + ")");
		String n1 = "0.0";
		String n2 = "0.0";
		if (Hist.irsim_num_punted != 0)
		{
			n1 = Double.toString(100.0 / (Hist.irsim_num_edges / Hist.irsim_num_punted + 1.0));
			n2 = Double.toString(Hist.irsim_num_cons_punted * 100.0 / Hist.irsim_num_punted);
		}
		System.out.println("punts = " + n1 + "%, cons_punted = " + n2 + "%");
	
		System.out.println("nevents = " + Sched.irsim_nevent);
	
		return(0);
	}
	
	/*
	 * Flush out the recorded history up to the (optional) specified time.
	 */
	static int flush_hist(String [] args)
	{
		long ftime = Sched.irsim_cur_delta;
		if (args.length != 1)
		{
			ftime = Sim.ns2d(TextUtils.atof(args[1]));
			if (ftime < 0 || ftime > Sched.irsim_cur_delta)
			{
				System.out.println(args[1] + ": Invalid flush time");
				return(0);
			}
		}
	
		if (ftime == 0)
			return(0);
	
		Hist.irsim_FlushHist(ftime);
		irsim_sim_time0 = ftime;
	
		if (irsim_analyzerON)
			Analyzer.irsim_RestartAnalyzer(irsim_sim_time0, Sched.irsim_cur_delta, 1);
	
		return(0);
	}
	
	static class Command
	{
		String commandName;
		String commandHelp;
		int lowArgs, highArgs;

		Command(String commandName, String commandHelp, int lowArgs, int highArgs)
		{
			this.commandName = commandName;
			this.commandHelp = commandHelp;
			this.lowArgs = lowArgs;
			this.highArgs = highArgs;
			cmdTbl.put(commandName, this);
		}

		int doIt(String [] args) { return 0; }
	}
	
	static class ExclCommand extends Command
	{
		ExclCommand() { super("!", "node/vector... . info regarding node(s) gate connections", 2, MAXARGS); }
		int doIt(String [] args) { return quest(args); }
	}
	
	static class QuestCommand extends Command
	{
		QuestCommand() { super("?", "node/vector... . info regarding node(s) src/drn connections", 2, MAXARGS); }
		int doIt(String [] args) { return quest(args); }
	}
	
	static class ActivityCommand extends Command
	{
		ActivityCommand() { super("activity", "from [to] . circuit activity in time interval", 2, 3); }
		int doIt(String [] args) { return doactivity(args); }
	}
	
	static class AliasCommand extends Command
	{
		AliasCommand() { super("alias", " .  print node aliases", 1, 1); }
		int doIt(String [] args) { return doprintAlias(args); }
	}
	
	static class AssertCommand extends Command
	{
		AssertCommand() { super("assert", "node/vector [mask] val . assert node/vector = val [& mask = 0]", 3, 4); }
		int doIt(String [] args) { return doAssert(args); }
	}
	
	static class AssertWhenCommand extends Command
	{
		AssertWhenCommand() { super("assertWhen", "nodeT valT node val . assert node = val when nodeT switches to valT", 5, 5); }
		int doIt(String [] args) { return doAssertWhen(args); }
	}
	
	static class BackCommand extends Command
	{
		BackCommand() { super("back", "time . move simulation time back to specified to time", 2, 2); }
		int doIt(String [] args) { return back_time(args); }
	}
	
	static class ClockCommand extends Command
	{
		ClockCommand() { super("c", "[n] . simulate for n clock cycles (default 1)\n  . or continue last simulation command prior to stoping", 1, 2); }
		int doIt(String [] args) { return doclock(args); }
	}
	
	static class ChangesCommand extends Command
	{
		ChangesCommand() { super("changes", "from [to] . print nodes that changed in time interval", 2, 3); }
		int doIt(String [] args) { return dochanges(args); }
	}
	
	static class ClockDefCommand extends Command
	{
		ClockDefCommand() { super("clock", "[node/vector [val]] . define clock sequence for node/vector", 1, MAXARGS); }
		int doIt(String [] args) { return setclock(args); }
	}
	
	static class DecayCommand extends Command
	{
		DecayCommand() { super("decay", "[file] . write net history to file", 1, 2); }
		int doIt(String [] args) { return setdecay(args); }
	}
	
	static class FlushCommand extends Command
	{
		FlushCommand() { super("flush", "[time] . flush out history up to time (default:current-time)", 1, 2); }
		int doIt(String [] args) { return flush_hist(args); }
	}
	
	static class HighCommand extends Command
	{
		HighCommand() { super("h", "node/vector... . drive node/vector(s) to 1 (high)", 2, MAXARGS); }
		int doIt(String [] args) { return setvalue(args); }
	}
	
	static class LowCommand extends Command
	{
		LowCommand() { super("l", "node/vector... . drive node/vector(s) to 0 (low)", 2, MAXARGS); }
		int doIt(String [] args) { return setvalue(args); }
	}
	
	static class UndefCommand extends Command
	{
		UndefCommand() { super("u", "node/vector... . drive node/vector(s) to X (undefined)", 2, MAXARGS); }
		int doIt(String [] args) { return setvalue(args); }
	}
	
	static class XCommand extends Command
	{
		XCommand() { super("x", "node/vector... . make node/vector(s) undriven (non-input)", 2, MAXARGS); }
		int doIt(String [] args) { return setvalue(args); }
	}

	static class InputsCommand extends Command
	{
		InputsCommand() { super("inputs", " . print currently driven (input) nodes", 1, 1); }
		int doIt(String [] args) { return inputs(args); }
	}

	static class SetModelCommand extends Command
	{
		SetModelCommand() { super("logfile", "[linear|switch] . print/change simulation model", 1, 2); }
		int doIt(String [] args) { return setmodel(args); }
	}

	static class PhaseCommand extends Command
	{
		PhaseCommand() { super("p", "step clock one simulation step (phase)", 1, 1); }
		int doIt(String [] args) { return dophase(args); }
	}

	static class PathCommand extends Command
	{
		PathCommand() { super("path", "node/vector... . critical path for last transition of node(s)", 2, MAXARGS); }
		int doIt(String [] args) { return dopath(args); }
	}

	static class PrintCommand extends Command
	{
		PrintCommand() { super("print", "[text...] . print specified text", 1, MAXARGS); }
		int doIt(String [] args) { return domsg(args); }
	}

	static class PrintUndefinedCommand extends Command
	{
		PrintUndefinedCommand() { super("printx", " . print all undefined (X) nodes", 1, 1); }
		int doIt(String [] args) { return doprintX(args); }
	}

	static class RunSeqCommand extends Command
	{
		RunSeqCommand() { super("R", "[n] . simulate for 'n' cycles (default: longest sequence)", 1, 2); }
		int doIt(String [] args) { return runseq(args); }
	}

	static class ReportCommand extends Command
	{
		ReportCommand() { super("report", "[args] . print/set trace-info or decay report (? for help)", 1, 10); }
		int doIt(String [] args) { return setreport(args); }
	}

	static class StepCommand extends Command
	{
		StepCommand() { super("s", "[time] . simulate for specified time (default: stepsize)", 1, 10); }
		int doIt(String [] args) { return dostep(args); }
	}

	static class SetCommand extends Command
	{
		SetCommand() { super("set", "vector value . assign value to vector", 3, 3); }
		int doIt(String [] args) { return setvector(args); }
	}

	static class StatsCommand extends Command
	{
		StatsCommand() { super("setpath", " . print event statistics", 1, 2); }
		int doIt(String [] args) { return do_stats(args); }
	}

	static class StepSizeCommand extends Command
	{
		StepSizeCommand() { super("stepsize", "[time] . print/set simulation step size", 1, 2); }
		int doIt(String [] args) { return setstep(args); }
	}

	static class StopCommand extends Command
	{
		StopCommand() { super("stop", "[-]node/vector... . pause simulation when node/vector(s) change", 2, MAXARGS); }
		int doIt(String [] args) { return setstop(args); }
	}

	static class TraceCommand extends Command
	{
		TraceCommand() { super("t", "[-]node/vector... . start/stop tracing specified node/vector(s)", 2, MAXARGS); }
		int doIt(String [] args) { return settrace(args); }
	}

	static class TCapCommand extends Command
	{
		TCapCommand() { super("tcap", " . print all shorted transistors", 1, 1); }
		int doIt(String [] args) { return print_tcap(args); }
	}

	static class UnitDelayCommand extends Command
	{
		UnitDelayCommand() { super("unitdelay", "[time] . force transitions to specified time (0 to disable)", 1, 2); }
		int doIt(String [] args) { return setunit(args); }
	}

	static class UntilCommand extends Command
	{
		UntilCommand() { super("until", "node/vec [mask] val count . sim until = val [& mask = 0] or count runout", 4, 5); }
		int doIt(String [] args) { return doUntil(args); }
	}

	static class MakeVectorCommand extends Command
	{
		MakeVectorCommand() { super("V", "[node/vector [val...]] . define input sequence for node/vector", 4, 5); }
		int doIt(String [] args) { return setseq(args); }
	}

	static class VectorCommand extends Command
	{
		VectorCommand() { super("vector", "name node... . (re)define vector 'name' composed of node(s)", 3, MAXARGS); }
		int doIt(String [] args) { return dovector(args); }
	}

	static class IncludeCommand extends Command
	{
		IncludeCommand() { super("w", "[-]node/vector... . add/delete node/vector(s) to display-list", 2, MAXARGS); }
		int doIt(String [] args) { return display(args); }
	}

	static void irsim_init_commands()
	{
		cmdTbl = new HashMap();
		new ExclCommand();
		new QuestCommand();
		new ActivityCommand();
		new AliasCommand();
		new AssertCommand();
		new AssertWhenCommand();
		new BackCommand();
		new ClockCommand();
		new ChangesCommand();
		new ClockDefCommand();
		new DecayCommand();
		new FlushCommand();
		new HighCommand();
		new LowCommand();
		new UndefCommand();
		new XCommand();	
		new InputsCommand();
		new SetModelCommand();
		new PhaseCommand();
		new PathCommand();
		new PrintCommand();
		new PrintUndefinedCommand();
		new RunSeqCommand();
		new ReportCommand();
		new StepCommand();
		new SetCommand();
		new StatsCommand();
		new StepSizeCommand();
		new StopCommand();
		new TraceCommand();
		new TCapCommand();
		new UnitDelayCommand();
		new UntilCommand();
		new MakeVectorCommand();
		new VectorCommand();
		new IncludeCommand();
	}
}
