/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Simulate.java
 * Input/output tool: superclass for simulation-output formats that display their results in a waveform window.
 * Written by Steven M. Rubin, Sun Microsystems.
 *
 * Copyright (c) 2003 Sun Microsystems and Static Free Software
 *
 * Electric(tm) is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Electric(tm) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Electric(tm); see the file COPYING.  If not, write to
 * the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, Mass 02111-1307, USA.
 */
package com.sun.electric.tool.io.input;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.io.IOTool;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.simulation.Simulation;
import com.sun.electric.tool.user.dialogs.OpenFile;
import com.sun.electric.tool.user.ui.WaveformWindow;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.io.IOException;
import java.util.List;
import java.util.Iterator;
import java.net.URL;

import javax.swing.JOptionPane;


/**
 * This class reads simulation output files and plots them.
 */
public class Simulate extends Input
{
	Simulate() {}

	/**
	 * Method called from the pulldown menus to read Spice output and plot it.
	 */
	public static void plotSpiceResults()
	{
		FileType type = getCurrentSpiceOutputType();
		if (type == null) return;
		plotSimulationResults(type, null, null, null);
	}

	/**
	 * Method called from the pulldown menus to read Spice output for the current cell.
	 */
	public static void plotSpiceResultsThisCell()
	{
		Cell cell = WindowFrame.needCurCell();
		if (cell == null) return;
		FileType type = getCurrentSpiceOutputType();
		if (type == null) return;
		plotSimulationResults(type, cell, null, null);
	}

	/**
	 * Method called from the pulldown menus to read Verilog output and plot it.
	 */
	public static void plotVerilogResults()
	{
		plotSimulationResults(FileType.VERILOGOUT, null, null, null);
	}

	/**
	 * Method called from the pulldown menus to read Verilog output for the current cell.
	 */
	public static void plotVerilogResultsThisCell()
	{
		Cell cell = WindowFrame.needCurCell();
		if (cell == null) return;
		plotSimulationResults(FileType.VERILOGOUT, cell, null, null);
	}

	/**
	 * Method to read simulation output of a given type.
	 */
	public static void plotSimulationResults(FileType type, Cell cell, URL fileURL, WaveformWindow ww)
	{
		Simulate is = null;
		if (type == FileType.HSPICEOUT)
		{
			is = (Simulate)new HSpiceOut();
		} else if (type == FileType.PSPICEOUT)
		{
			is = (Simulate)new PSpiceOut();
		} else if (type == FileType.RAWSPICEOUT)
		{
			is = (Simulate)new RawSpiceOut();
		} else if (type == FileType.RAWSSPICEOUT)
		{
			is = (Simulate)new SmartSpiceOut();
		} else if (type == FileType.SPICEOUT)
		{
			is = (Simulate)new SpiceOut();
		} else if (type == FileType.VERILOGOUT)
		{
			is = (Simulate)new VerilogOut();
		}
		if (is == null)
		{
			System.out.println("Cannot handle " + type.getName() + " files yet");
			return;
		}

		if (cell == null)
		{
			if (fileURL == null)
			{
				String fileName = OpenFile.chooseInputFile(type, null);
				if (fileName == null) return;
				fileURL = TextUtils.makeURLToFile(fileName);
			}
			String cellName = TextUtils.getFileNameWithoutExtension(fileURL);
			Library curLib = Library.getCurrent();
			cell = curLib.findNodeProto(cellName);
			if (cell != null)
			{
				// prompt for the cell
				List cellList = curLib.getCellsSortedByName();
				String [] cellNames = new String[cellList.size()];
				int i = 0;
				for(Iterator it = cellList.iterator(); it.hasNext(); )
				{
					Cell aCell = (Cell)it.next();
					cellNames[i++] = aCell.describe();
				}
				String defaultCell = cellNames[0];
				if (cell != null) defaultCell = cell.describe();
				String selectedCell = (String)JOptionPane.showInputDialog(null, "Which cell is associated with this data",
					"Cells in library " + curLib.getName(), JOptionPane.INFORMATION_MESSAGE, null, cellNames, defaultCell);
				if (selectedCell != null)
					cell = curLib.findNodeProto(selectedCell);
			}
		} else
		{
			if (fileURL == null)
			{
				String [] extensions = type.getExtensions();
				String filePath = TextUtils.getFilePath(cell.getLibrary().getLibFile());
				String fileName = cell.getName() + "." + extensions[0];
				fileURL = TextUtils.makeURLToFile(filePath + fileName);
			}
		}
		ReadSimulationOutput job = new ReadSimulationOutput(type, is, fileURL, cell, ww);
	}

	/**
	 * Class to read simulation output in a new thread.
	 */
	private static class ReadSimulationOutput extends Job
	{
		FileType type;
		Simulate is;
		URL fileURL;
		Cell cell;
		WaveformWindow ww;

		protected ReadSimulationOutput(FileType type, Simulate is, URL fileURL, Cell cell, WaveformWindow ww)
		{
			super("Read Simulation Output", IOTool.tool, Job.Type.EXAMINE, null, null, Job.Priority.USER);
			this.type = type;
			this.is = is;
			this.fileURL = fileURL;
			this.cell = cell;
			this.ww = ww;
			startJob();
		}

		public boolean doIt()
		{
			try
			{
				Simulation.SimData sd = is.readSimulationOutput(fileURL, cell);
				if (sd != null)
				{
					sd.setDataType(type);
					sd.setFileURL(fileURL);
					Simulation.showSimulationData(sd, ww);
				}
			} catch (IOException e)
			{
				System.out.println("End of file reached while reading " + fileURL);
			}
			return true;
		}
	}

	/**
	 * Method that is overridden by subclasses to actually do the work.
	 */
	protected Simulation.SimData readSimulationOutput(URL fileURL, Cell cell)
		throws IOException
	{
		return null;
	}

	public static FileType getCurrentSpiceOutputType()
	{
		String format = Simulation.getSpiceOutputFormat();
		int engine = Simulation.getSpiceEngine();
		if (format.equalsIgnoreCase("Standard"))
		{
			if (engine == Simulation.SPICE_ENGINE_H)
				return FileType.HSPICEOUT;
			if (engine == Simulation.SPICE_ENGINE_3 || engine == Simulation.SPICE_ENGINE_P)
				return FileType.PSPICEOUT;
			return FileType.SPICEOUT;
		}
		if (format.equalsIgnoreCase("Raw"))
		{
			return FileType.RAWSPICEOUT;
		}
		if (format.equalsIgnoreCase("Raw/Smart"))
		{
			return FileType.RAWSSPICEOUT;
		}
		return null;
	}

	/*
	 * Method to get the next line of text from the simulator.
	 * Returns null at end of file.
	 */
	protected String getLineFromSimulator()
		throws IOException
	{
		StringBuffer sb = new StringBuffer();
		int bytesRead = 0;
		for(;;)
		{
			int ch = lineReader.read();
			if (ch == -1) return null;
			bytesRead++;
			if (ch == '\n' || ch == '\r') break;
			sb.append((char)ch);
		}
		updateProgressDialog(bytesRead);
		return sb.toString();
	}

	//#define SP_BUF_SZ 1024
	//
	//
	//static CHAR	    sim_spice_cellname[100] = {x_("")};		/* Name extracted from .SPO file */
	//static CHAR   **sim_spice_signames;
	//static INTSML  *sim_spice_sigtypes;
	//static double  *sim_spice_time;
	//static float   *sim_spice_val;
	//static INTBIG   sim_spice_signals = 0;				/* entries in "signames/sigtypes" */
	//static INTBIG   sim_spice_iter;
	//static INTBIG   sim_spice_limit = 0;				/* entries in "time/val" */
	//static INTBIG   sim_spice_filepos;

//	/*
//	 * Method to return the SPICE network name of network "net".
//	 */
//	CHAR *sim_spice_signalname(NETWORK *net)
//	{
//		NODEPROTO *np;
//		REGISTER CHAR *prevstr, *signame;
//		CHAR **nodenames;
//		REGISTER NODEPROTO *netpar;
//		REGISTER VARIABLE *varname;
//		REGISTER PORTPROTO *pp;
//		REGISTER PORTARCINST *pi;
//		REGISTER PORTEXPINST *pe;
//		REGISTER NODEINST *ni;
//		REGISTER NETWORK *busnet;
//		REGISTER INTBIG sigcount, i, busidx;
//		REGISTER void *infstr;
//		INTBIG index;
//
//		// get simulation cell, quit if not simulating
//		if (sim_window_isactive(&np) == 0) return(0);
//
//		// shift this network up the hierarchy as far as it is exported
//		for(;;)
//		{
//			// if this network is at the current level of hierarchy, stop going up the hierarchy
//			if (net->parent == np) break;
//
//			// find the instance that is the proper parent of this cell
//			ni = descentparent(net->parent, &index, NOWINDOWPART, 0);
//			if (ni == NONODEINST) break;
//
//			// see if the network is exported from this level
//			for(pp = net->parent->firstportproto; pp != NOPORTPROTO; pp = pp->nextportproto)
//				if (pp->network == net) break;
//			if (pp != NOPORTPROTO)
//				pp = equivalentport(net->parent, pp, ni->proto);
//
//			// see if network is exported from this level as part of a bus
//			if (pp == NOPORTPROTO && net->buswidth <= 1 && net->buslinkcount > 0) {
//				// find bus that this singel wire net is part of
//				busidx = -1;
//				for (busnet = net->parent->firstnetwork; busnet != NONETWORK; busnet = busnet->nextnetwork) {
//					if (busnet->buswidth > 1) { // must be bus
//						for (i=0; i<busnet->buswidth; i++) {
//							if (net == busnet->networklist[i]) { 
//								busidx = i; // net part of bus
//								break;
//							}
//						}
//					}
//					if (busidx != -1) break;
//				}
//				// make sure won't access array out of bounds
//				if (busidx == -1) busidx = 0;
//				// see if bus exported
//				for(pp = busnet->parent->firstportproto; pp != NOPORTPROTO; pp = pp->nextportproto)
//					if (pp->network == busnet) {
//						break;
//					}
//				if (pp != NOPORTPROTO) {
//					pp = equivalentport(busnet->parent, pp, ni->proto);
//				}
//			}
//			if (pp == NOPORTPROTO) break;
//
//			// exported: find the network in the higher level
//			for(pi = ni->firstportarcinst; pi != NOPORTARCINST; pi = pi->nextportarcinst)
//				if (pi->proto == pp)
//			{
//				// if bussed, grab correct net in bus, otherwise just grab net
//				if (pi->conarcinst->network->buswidth > 1 && pi->conarcinst->network->buswidth > busidx) {
//					net = pi->conarcinst->network->networklist[busidx];
//				} else {
//					net = pi->conarcinst->network;
//				}
//				break;
//			}
//			if (pi == NOPORTARCINST)
//			{
//				for(pe = ni->firstportexpinst; pe != NOPORTEXPINST; pe = pe->nextportexpinst)
//					if (pe->proto == pp)
//				{
//					// if bussed, grab correct net in bus, otherwise just grab net
//					if (pe->exportproto->network->buswidth > 1 && pe->exportproto->network->buswidth > busidx) {
//						net = pe->exportproto->network->networklist[busidx];
//					} else {
//						net = pe->exportproto->network;
//					}
//					break;
//				}
//				if (pe == NOPORTEXPINST) break;
//			}
//		}
//
//		// construct a path to the top-level of simulation from the net's current level
//		netpar = net->parent;
//		prevstr = x_("");
//		for(;;)
//		{
//			if (insamecellgrp(netpar, np)) break;
//
//			// find the instance that is the proper parent of this cell
//			ni = descentparent(netpar, &index, NOWINDOWPART, 0);
//			if (ni == NONODEINST) break;
//
//			varname = getvalkey((INTBIG)ni, VNODEINST, VSTRING, el_node_name_key);
//			if (varname == 0)
//			{
//				ttyputerr(_("Back annotation is missing"));
//				return(0);
//			}
//			infstr = initinfstr();
//			addtoinfstr(infstr, 'x');
//			signame = (CHAR *)varname->addr;
//			if (ni->arraysize > 1)
//			{
//				sigcount = net_evalbusname(APBUS, signame, &nodenames,
//					NOARCINST, NONODEPROTO, 0);
//				if (index >= 0 && index < sigcount)
//					signame = nodenames[index];
//			}
//			addstringtoinfstr(infstr, signame);
//			if (*prevstr != 0)
//			{
//				addtoinfstr(infstr, '.');
//				addstringtoinfstr(infstr, prevstr);
//			}
//			prevstr = returninfstr(infstr);
//			netpar = ni->parent;
//		}
//
//		infstr = initinfstr();
//		addstringtoinfstr(infstr, prevstr);
//		if (*prevstr != 0) addtoinfstr(infstr, '.');
//		if (net->namecount <= 0) addstringtoinfstr(infstr, describenetwork(net)); else
//			addstringtoinfstr(infstr, networkname(net, 0));
//		return(returninfstr(infstr));
//	}
//
//	INTBIG sim_spice_findsignalname(CHAR *name)
//	{
//		REGISTER INTBIG i, len;
//
//		for(i=0; i<sim_spice_signals; i++)
//			if (namesame(name, sim_spice_signames[i]) == 0) return(i);
//
//		// try looking for signal names wrapped in "v()" or "i()"
//		len = strlen(name);
//		for(i=0; i<sim_spice_signals; i++)
//		{
//			if (namesamen(name, &sim_spice_signames[i][2], len) == 0)
//			{
//				if (sim_spice_signames[i][0] != 'i' && sim_spice_signames[i][0] != 'v')
//					continue;
//				if (sim_spice_signames[i][1] != '(') continue;
//				if (sim_spice_signames[i][len+2] != ')') continue;
//				if (sim_spice_signames[i][len+3] != 0) continue;
//				return(i);
//			}
//		}
//		return(-1);
//	}
//
//	/*
//	 * Method that feeds the current signals into the explorer window.
//	 */
//	void sim_spicereportsignals(WINDOWPART *simwin, void *(*addbranch)(CHAR*, void*),
//		void *(*findbranch)(CHAR*, void*), void *(*addleaf)(CHAR*, void*), CHAR *(*nodename)(void*))
//	{
//		REGISTER INTBIG i, j, k, len;
//		REGISTER CHAR *pt, *signame, save;
//		REGISTER void *curlevel, *nextlevel;
//
//		for(i=0; i<sim_spice_signals; i++)
//		{
//			signame = sim_spice_signames[i];
//			len = strlen(signame);
//			for(j=len-2; j>0; j--)
//				if (signame[j] == '.') break;
//			if (j <= 0)
//			{
//				// no "." qualifiers
//				(*addleaf)(signame, 0);
//			} else
//			{
//				// is down the hierarchy
//				signame[j] = 0;
//
//				// now find the location for this branch
//				curlevel = 0;
//				pt = signame;
//				for(;;)
//				{
//					for(k=0; pt[k] != 0; k++) if (pt[k] == '.') break;
//					save = pt[k];
//					pt[k] = 0;
//					nextlevel = (*findbranch)(pt, curlevel);
//					if (nextlevel == 0)
//						nextlevel = (*addbranch)(pt, curlevel);
//					curlevel = nextlevel;
//					if (save == 0) break;
//					pt[k] = save;
//					pt = &pt[k+1];
//				}
//				(*addleaf)(&signame[j+1], curlevel);
//				signame[j] = '.';
//			}
//		}
//	}
//
//	/*
//	 * routine to display the numbers in "sim_spice_numbers".  For each entry of the
//	 * linked list is another time sample, with "sim_spice_signals" trace points.  The
//	 * names of these signals is in "sim_spice_signames". Returns true on error.
//	 */
//	BOOLEAN sim_spice_plotvalues(NODEPROTO *np)
//	{
//		// count the number of values
//		for(num = sim_spice_numbers, numtotal=0; num != NONUMBERS; num = num->nextnumbers)
//			numtotal++;
//		if (numtotal == 0) return(FALSE);
//
//		// we have to establish a link for the plot cell
//		plotnp = np;
//
//		// if we're already simulating a cell, use that one
//		if (sim_simnt != NONODEPROTO) plotnp = sim_simnt; else
//			if (sim_spice_cellname[0] != '\0') plotnp = getnodeproto(sim_spice_cellname);
//		if (plotnp != NONODEPROTO && plotnp->primindex != 0) plotnp = NONODEPROTO;
//		if (plotnp != NONODEPROTO && np == NONODEPROTO)
//		{
//			infstr = initinfstr();
//			formatinfstr(infstr, _("Is this a simulation of cell %s?"),
//				describenodeproto(plotnp));
//			i = ttygetparam(returninfstr(infstr), &us_yesnop, 3, pars);
//			if (i == 1)
//			{
//				if (pars[0][0] == 'n') plotnp = NONODEPROTO;
//			}
//		}
//
//		// one final chance to pick a cell
//		if (plotnp == NONODEPROTO)
//		{
//			i = ttygetparam(_("Please select a cell to associate with this plot: "),
//				&us_showdp, 3, pars);
//			if (i > 0) plotnp = getnodeproto(pars[0]);
//		}
//		if (plotnp == NONODEPROTO) return(TRUE);
//
//		// get memory for the values
//		if (sim_spice_ensurespace(numtotal)) return(TRUE);
//
//		j = 0;
//		for(num = sim_spice_numbers; num != NONUMBERS; num = num->nextnumbers)
//		{
//			sim_spice_time[j++] = num->time;
//		}
//
//		// find former signal order
//		oldsigcount = 0;
//		numtraces = 1;
//		var = getvalkey((INTBIG)plotnp, VNODEPROTO, VSTRING|VISARRAY, sim_window_signalorder_key);
//		if (var != NOVARIABLE)
//		{
//			oldsigcount = getlength(var);
//			if (oldsigcount > 0)
//			{
//				sa = newstringarray(sim_tool->cluster);
//				for(i=0; i<oldsigcount; i++)
//				{
//					pt = ((CHAR **)var->addr)[i];
//					addtostringarray(sa, pt);
//
//					start = pt;
//					if (*pt == '-') pt++;
//					for( ; *pt != 0; pt++)
//						if (!isdigit(*pt) || *pt == ':') break;
//					if (*pt != ':') continue;
//					{
//						position = eatoi(start) + 1;
//						if (position > numtraces) numtraces = position;
//					}
//				}
//				oldsignames = getstringarray(sa, &oldsigcount);
//			}
//		}
//
//		// make the simulation window
//		if (sim_window_create(numtraces, plotnp, sim_spice_charhandlerwave,
//			sim_spice_charhandlerschem, SPICE)) return(TRUE);
//		sim_window_state = (sim_window_state & ~SIMENGINECUR) | SIMENGINECURSPICE;
//
//		for (i = 0; i<sim_spice_signals; i++)
//		{
//			if (namesame(sim_spice_signames[i], x_("vdd")) == 0 ||
//				namesame(sim_spice_signames[i], x_("v(vdd)")) == 0)
//			{
//				if (sim_spice_numbers != NONUMBERS && i < sim_spice_numbers->count)
//					sim_window_setvdd( sim_spice_numbers->list[i] );
//			}
//		}
//
//		// add in saved signals
//		for(j=0; j<oldsigcount; j++)
//		{
//			// see if the name is a bus, and ignore it
//			for(pt = oldsignames[j]; *pt != 0; pt++) if (*pt == '\t') break;
//			if (*pt == '\t') continue;
//
//			// a single signal
//			position = 0;
//			pt = oldsignames[j];
//			if (*pt == '-') pt++;
//			for( ; *pt != 0; pt++)
//				if (!isdigit(*pt) || *pt == ':') break;
//			if (*pt != ':') pt = oldsignames[j]; else
//			{
//				position = eatoi(oldsignames[j]);
//				pt++;
//			}
//			for(i=0; i<sim_spice_signals; i++)
//			{
//				if (namesame(sim_spice_signames[i], pt) != 0) continue;
//				tr = sim_window_newtrace(position, sim_spice_signames[i], 0);
//				k = 0;
//				for(num = sim_spice_numbers; num != NONUMBERS; num = num->nextnumbers)
//					sim_spice_val[k++] = num->list[i];
//				sim_window_loadanatrace(tr, numtotal, sim_spice_time, sim_spice_val);
//				break;
//			}
//		}
//
//		sim_window_renumberlines();
//		sim_window_auto_anarange();
//
//		// clean up
//		min = sim_spice_time[0];   max = sim_spice_time[numtotal-1];
//		if (min >= max)
//		{
//			ttyputmsg(_("Invalid time range"));
//			return(TRUE);
//		}
//		sim_window_settimerange(0, min, max);
//		sim_window_setmaincursor((max-min)*0.25f + min);
//		sim_window_setextensioncursor((max-min)*0.75f + min);
//		if (oldsigcount > 0) killstringarray(sa);
//		sim_window_redraw();
//		return(FALSE);
//	}
//
//	BOOLEAN sim_spice_charhandlerschem(WINDOWPART *w, INTSML chr, INTBIG special)
//	{
//		CHAR *par[3];
//
//		// special characters are not handled here
//		if (special != 0)
//			return(us_charhandler(w, chr, special));
//
//		switch (chr)
//		{
//			case '?':		// help
//				ttyputmsg(_("These keys may be typed in the SPICE waveform window:"));
//				ttyputinstruction(x_(" d"), 6, _("move down the hierarchy"));
//				ttyputinstruction(x_(" u"), 6, _("move up the hierarchy"));
//				return(FALSE);
//			case 'd':		// move down the hierarchy
//				us_editcell(0, par);
//				return(FALSE);
//			case 'u':		// move up the hierarchy
//				us_outhier(0, par);
//				return(FALSE);
//		}
//		return(us_charhandler(w, chr, special));
//	}
//
//	BOOLEAN sim_spice_charhandlerwave(WINDOWPART *w, INTSML chr, INTBIG special)
//	{
//		CHAR *par[3];
//		REGISTER INTBIG tr, i, j, thispos, frame;
//		REGISTER NUMBERS *num;
//
//		// special characters are not handled here
//		if (special != 0)
//			return(us_charhandler(w, chr, special));
//
//		// see if there are special functions for SPICE waveform simulation
//		switch (chr)
//		{
//			case '?':		// help
//				ttyputmsg(_("These keys may be typed in the SPICE Waveform window:"));
//				ttyputinstruction(x_(" 9"), 6, _("show entire vertical range"));
//				ttyputinstruction(x_(" 7"), 6, _("zoom vertical range in"));
//				ttyputinstruction(x_(" 0"), 6, _("zoom vertical range out"));
//				ttyputinstruction(x_(" 8"), 6, _("shift vertical range up"));
//				ttyputinstruction(x_(" 2"), 6, _("shift vertical range down"));
//				return(FALSE);
//			case '9':		// show entire vertical range
//				tr = sim_window_gethighlighttrace();
//				sim_window_auto_anarange() ;
//				sim_window_redraw();
//				return(FALSE);
//			case '7':		// zoom vertical range in
//				tr = sim_window_gethighlighttrace();
//				frame = sim_window_gettraceframe(tr);
//				sim_window_zoom_frame(frame);
//				sim_window_redraw();
//				return(FALSE);
//			case '0':		// zoom vertical range out
//				tr = sim_window_gethighlighttrace();
//				frame = sim_window_gettraceframe(tr);
//				sim_window_zoomout_frame(frame);
//				sim_window_redraw();
//				return(FALSE);
//			case '8':		// shift vertical range up
//				tr = sim_window_gethighlighttrace();
//				frame = sim_window_gettraceframe(tr);
//				sim_window_shiftup_frame(frame);
//				sim_window_redraw();
//				return(FALSE);
//			case '2':		// shift vertical range down
//				tr = sim_window_gethighlighttrace();
//				frame = sim_window_gettraceframe(tr);
//				sim_window_shiftdown_frame(frame);
//				sim_window_redraw();
//				return(FALSE);
//		}
//		return(us_charhandler(w, chr, special));
//	}
//
//	/*
//	 * Routine to add the highlighted signal to the waveform window.
//	 */
//	void sim_spice_addhighlightednet(CHAR *name, BOOLEAN overlay)
//	{
//		REGISTER ARCINST *ai;
//		NODEPROTO *np;
//		REGISTER INTBIG i, tr, frameno, j;
//		REGISTER CHAR *pt;
//		REGISTER NUMBERS *num;
//
//		if ((sim_window_isactive(&np) & SIMWINDOWWAVEFORM) == 0)
//		{
//			ttyputerr(_("Not displaying a waveform"));
//			return;
//		}
//
//		if (name != 0) pt = name; else
//		{
//			ai = (ARCINST *)asktool(us_tool, x_("get-arc"));
//			if (ai == NOARCINST)
//			{
//				ttyputerr(_("Select an arc first"));
//				return;
//			}
//			if (ai->network == NONETWORK)
//			{
//				ttyputerr(_("This arc has no network information"));
//				return;
//			}
//			pt = sim_spice_signalname(ai->network);
//			if (pt == 0)
//			{
//				ttyputerr(_("Cannot get SPICE signal for network %s"), pt);
//				return;
//			}
//		}
//		i = sim_spice_findsignalname(pt);
//		if (i < 0)
//		{
//			ttyputerr(_("Cannot find network %s in the simulation data"), pt);
//			return;
//		}
//
//		// figure out where to show the new signal
//		if (overlay)
//		{
//			frameno = sim_window_getcurframe();
//			if (frameno < 0) overlay = FALSE;
//		}
//		if (!overlay) frameno = -1; else
//		{
//			frameno = sim_window_getcurframe();
//			sim_window_cleartracehighlight();
//		}
//
//		// create a new trace in this slot
//		tr = sim_window_newtrace(frameno, sim_spice_signames[i], 0);
//		j = 0;
//		for(num = sim_spice_numbers; num != NONUMBERS; num = num->nextnumbers)
//		{
//			sim_spice_val[j++] = num->list[i];
//		}
//		sim_window_loadanatrace(tr, j, sim_spice_time, sim_spice_val);
//		sim_window_auto_anarange();
//		sim_window_redraw();
//		sim_window_cleartracehighlight();
//		sim_window_addhighlighttrace(tr);
//	}
//
//	/*
//	 * Routine to return the NETWORK associated with HSPICE signal name "name".
//	 */
//	NETWORK *sim_spice_networkfromname(CHAR *name)
//	{
//		NODEPROTO *np;
//		REGISTER NODEPROTO *cnp;
//		REGISTER VARIABLE *var;
//		REGISTER CHAR *pt, *start;
//		REGISTER NODEINST *ni;
//		REGISTER NETWORK *net;
//
//		// get simulation cell, quit if not simulating
//		if (sim_window_isactive(&np) == 0) return(NONETWORK);
//
//		// parse the names, separated by dots
//		start = name;
//		for(;;)
//		{
//			for(pt = start; *pt != 0 && *pt != '.'; pt++) ;
//			if (*pt == 0) break;
//
//			// get node name that is pushed into
//			*pt = 0;
//			if (*start == 'x') start++;
//			for(ni = np->firstnodeinst; ni != NONODEINST; ni = ni->nextnodeinst)
//			{
//				var = getvalkey((INTBIG)ni, VNODEINST, VSTRING, el_node_name_key);
//				if (var == NOVARIABLE) continue;
//				if (namesame(start, (CHAR *)var->addr) == 0) break;
//			}
//			*pt++ = '.';
//			if (ni == NONODEINST) return(NONETWORK);
//			start = pt;
//			np = ni->proto;
//			cnp = contentsview(np);
//			if (cnp != NONODEPROTO) np = cnp;
//		}
//
//		// find network "start" in cell "np"
//		net = getnetwork(start, np);
//		return(net);
//	}
}
