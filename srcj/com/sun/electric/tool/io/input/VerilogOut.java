/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: VerilogOut.java
 *
 * Copyright (c) 2004 Sun Microsystems and Static Free Software
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

import com.sun.electric.database.text.TextUtils;
import com.sun.electric.tool.io.input.Input;
import com.sun.electric.tool.io.input.Simulate;

import java.io.InputStream;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.DataInputStream;
import java.net.URL;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Class for reading and displaying waveforms from Verilog output.
 * Thease are contained in .spo files.
 */
public class VerilogOut extends Simulate
{
	VerilogOut() {}

	/**
	 * Method to read an Verilog output file.
	 */
	protected SimData readSimulationOutput(URL fileURL)
		throws IOException
	{
		// open the file
		InputStream stream = TextUtils.getURLStream(fileURL);
		if (stream == null) return null;
		if (openTextInput(fileURL, stream)) return null;

		// show progress reading .tr0 file
		startProgressDialog("Verilog output", fileURL.getFile());

		// read the actual signal data from the .tr0 file
		SimData sd = readVerilogFile();

		// stop progress dialog, close the file
		stopProgressDialog();
		closeInput();

		// return the simulation data
		return sd;
	}

	private SimData readVerilogFile()
		throws IOException
	{
//		sim_timescale = 1.0;
//		sim_vercurscope[0] = 0;
//		sim_verlineno = 0;
//		sim_vercurlevel = 0;
//		numsignals = 0;
//		verhash = 0;
//		hashtablesize = 1;
//		vslast = NOVERSIGNAL;
//		sosymbol = newstringobj(sim_tool->cluster);
//		soname = newstringobj(sim_tool->cluster);
//		socontext = newstringobj(sim_tool->cluster);
		for(;;)
		{
			String verLine = getLineFromSimulator();
			if (verLine == null) break;
			List keywordList = getKeywords(verLine);
			if (keywordList.size() == 0) continue;
			String firstKeyword = (String)keywordList.iterator().next();

			/* ignore "$date", "$version" or "$timescale" */
			if (firstKeyword.equals("$date") || firstKeyword.equals("$version"))
			{
				sim_verparsetoend(keywordList);
				continue;
			}
			if (firstKeyword.equals("$timescale"))
			{
//				verLine = getLineFromSimulator();
//				if (verLine == null) break;
//				keywordList = getKeywords(line);
//				if (keywordList.size() > 0)
//				{
//					String argument = (String)keywordList.iterator().next();
//				units = -1;
//				pt = sim_verline;
//				keyword = getkeyword(&pt, x_(" "));
//				for(pt = keyword; *pt != 0; pt++)
//					if (!isdigit(*pt)) break;
//				if (*pt == 0)
//				{
//					ttyputerr(_("No time units on line %ld"), pt, sim_verlineno);
//				} else
//				{
//					if (namesame(pt, "ps") == 0) units = INTTIMEUNITPSEC; else
//					if (namesame(pt, "s") == 0) units = INTTIMEUNITSEC; else
//						ttyputerr(_("Unknown time units: '%s' on line %ld"), pt, sim_verlineno);
//				}
//				if (units >= 0)
//				{
//					*pt = 0;
//					sim_timescale = figureunits(keyword, VTUNITSTIME, units);
//				}
				sim_verparsetoend(keywordList);
				continue;
			}
			if (firstKeyword.equals("$scope"))
			{
//				if (namesame(keyword, x_("module")) == 0 || namesame(keyword, x_("task")) == 0 ||
//					namesame(keyword, x_("function")) == 0)
//				{
//					keyword = getkeyword(&pt, x_(" "));
//					if (keyword != NOSTRING)
//					{
//						if (sim_vercurscope[0] != 0) strcat(sim_vercurscope, x_("."));
//						strcat(sim_vercurscope, keyword);
//						sim_vercurlevel++;
//					}
//				}
				sim_verparsetoend(keywordList);
				continue;
			}

			if (firstKeyword.equals("$upscope"))
			{
//				if (sim_vercurlevel <= 0 || sim_vercurscope[0] == 0)
//				{
//					ttyputerr(_("Unbalanced $upscope on line %ld"), sim_verlineno);
//					continue;
//				} else
//				{
//					len = strlen(sim_vercurscope);
//					for(i=len-1; i>0; i--) if (sim_vercurscope[i] == '.') break;
//					if (sim_vercurscope[i] == '.') sim_vercurscope[i] = 0;
//					sim_vercurlevel--;
//				}
				sim_verparsetoend(keywordList);
				continue;
			}

			if (firstKeyword.equals("$var"))
			{
//				keyword = getkeyword(&pt, x_(" "));
//				if (keyword != NOSTRING)
//				{
//					if (namesame(keyword, x_("wire")) == 0 ||
//						namesame(keyword, x_("supply0")) == 0 ||
//						namesame(keyword, x_("supply1")) == 0 ||
//						namesame(keyword, x_("reg")) == 0 ||
//						namesame(keyword, x_("parameter")) == 0 ||
//						namesame(keyword, x_("trireg")) == 0)
//					{
//						/* get the bus width */
//						keyword = getkeyword(&pt, x_(" "));
//						if (keyword == NOSTRING) continue;
//						width = myatoi(keyword);
//
//						/* get the symbol name for this signal */
//						keyword = getkeyword(&pt, x_(" "));
//						if (keyword == NOSTRING) continue;
//						clearstringobj(sosymbol);
//						addstringtostringobj(keyword, sosymbol);
//
//						/* get the signal name */
//						keyword = getkeyword(&pt, x_(" "));
//						if (keyword == NOSTRING) continue;
//						clearstringobj(soname);
//						addstringtostringobj(keyword, soname);
//
//						/* see if there is an index */
//						keyword = getkeyword(&pt, x_(" "));
//						if (keyword == NOSTRING) continue;
//						foundend = FALSE;
//						if (namesame(keyword, x_("$end")) == 0) foundend = TRUE; else
//							addstringtostringobj(keyword, soname);
//
//						/* set the context */
//						clearstringobj(socontext);
//						addstringtostringobj(sim_vercurscope, socontext);
//
//						/* allocate one big object with structure and names */
//						structlen = sizeof (VERSIGNAL);
//						symbol = getstringobj(sosymbol);   symbollen = (estrlen(symbol)+1) * SIZEOFCHAR;
//						name = getstringobj(soname);       namelen = (estrlen(name)+1) * SIZEOFCHAR;
//						context = getstringobj(socontext); contextlen = (estrlen(context)+1) * SIZEOFCHAR;
//						structsize = structlen + symbollen + namelen + contextlen;
//						onestruct = (UCHAR1 *)emalloc(structsize, sim_tool->cluster);
//						if (onestruct == 0) continue;
//						vs = (VERSIGNAL *)onestruct;
//						vs->symbol = (CHAR *)&onestruct[structlen];
//						vs->signalname = (CHAR *)&onestruct[structlen+symbollen];
//						vs->signalcontext = (CHAR *)&onestruct[structlen+symbollen+namelen];
//						strcpy(vs->symbol, symbol);
//						strcpy(vs->signalname, name);
//						strcpy(vs->signalcontext, context);
//						vs->nextversignal = NOVERSIGNAL;
//						if (vslast == NOVERSIGNAL) sim_verfirstsignal = vs; else
//							vslast->nextversignal = vs;
//						vslast = vs;
//						vs->level = sim_vercurlevel;
//						vs->flags = 0;
//						vs->signal = 0;
//						vs->total = 0;
//						vs->count = 0;
//						vs->width = width;
//						vs->realversignal = NOVERSIGNAL;
//						numsignals++;
//
//						if (width > 1)
//						{
//							/* create fake signals for the individual entries */
//							vslist = (VERSIGNAL **)emalloc(width * (sizeof (VERSIGNAL *)), sim_tool->cluster);
//							if (vslist == 0) continue;
//							vs->signals = vslist;
//							for(i=0; i<width; i++)
//							{
//								structlen = sizeof (VERSIGNAL);
//								infstr = initinfstr();
//								formatinfstr(infstr, x_("%s[%ld]"), vs->signalname, i);
//								name = returninfstr(infstr);       namelen = (estrlen(name)+1) * SIZEOFCHAR;
//								context = sim_vercurscope;         contextlen = (estrlen(context)+1) * SIZEOFCHAR;
//								structsize = structlen + namelen + contextlen;
//								onestruct = (UCHAR1 *)emalloc(structsize, sim_tool->cluster);
//								if (onestruct == 0) break;
//								subvs = (VERSIGNAL *)onestruct;
//								subvs->symbol = 0;
//								subvs->signalname = (CHAR *)&onestruct[structlen];
//								subvs->signalcontext = (CHAR *)&onestruct[structlen+namelen];
//								strcpy(subvs->signalname, name);
//								strcpy(subvs->signalcontext, context);
//								subvs->nextversignal = NOVERSIGNAL;
//								if (vslast == NOVERSIGNAL) sim_verfirstsignal = subvs; else
//									vslast->nextversignal = subvs;
//								vslast = subvs;
//								subvs->level = sim_vercurlevel;
//								subvs->flags = 0;
//								subvs->signal = 0;
//								subvs->total = 0;
//								subvs->count = 0;
//								subvs->width = 1;
//								subvs->realversignal = NOVERSIGNAL;
//								vslist[i] = subvs;
//								numsignals++;
//							}
//						}
//						if (foundend) continue;
//					} else
//					{
//						ttyputerr(_("Invalid $var on line %ld: %s"), sim_verlineno, sim_verline);
//						continue;
//					}
//				}
				sim_verparsetoend(keywordList);
				continue;
			}

			if (firstKeyword.equals("$enddefinitions"))
			{
				sim_verparsetoend(keywordList);
//				infstr = initinfstr();
//				formatinfstr(infstr, _("Found %ld signal names"), numsignals);
//				DiaSetTextProgress(sim_verprogressdialog, _("Building signal table..."));
//				DiaSetCaptionProgress(sim_verprogressdialog, returninfstr(infstr));
//
//				/* build a table for finding signal names */
//				for(i=0; i<256; i++) sim_charused[i] = -1;
//				sim_numcharsused = 0;
//				for(vs = sim_verfirstsignal; vs != NOVERSIGNAL; vs = vs->nextversignal)
//				{
//					if (vs->symbol == 0) continue;
//					for(pt = vs->symbol; *pt != 0; pt++)
//					{
//						i = *pt & 0xFF;
//						if (sim_charused[i] < 0)
//							sim_charused[i] = ++sim_numcharsused;
//					}
//				}
//
//				hashtablesize = pickprime(numsignals*2);
//				verhash = (VERSIGNAL **)emalloc(hashtablesize * (sizeof (VERSIGNAL *)), sim_tool->cluster);
//				if (verhash == 0) return;
//				for(i=0; i<hashtablesize; i++) verhash[i] = NOVERSIGNAL;
//
//				/* insert the signals */
//				for(vs = sim_verfirstsignal; vs != NOVERSIGNAL; vs = vs->nextversignal)
//				{
//					if (vs->symbol == 0) continue;
//					hashcode = sim_vergetsignalhash(vs->symbol) % hashtablesize;
//					for(i=0; i<hashtablesize; i++)
//					{
//						if (verhash[hashcode] == NOVERSIGNAL)
//						{
//							verhash[hashcode] = vs;
//							break;
//						}
//						if (strcmp(vs->symbol, verhash[hashcode]->symbol) == 0)
//						{
//							/* same symbol name: merge the signals */
//							vs->realversignal = verhash[hashcode];
//							break;
//						}
//						hashcode++;
//						if (hashcode >= hashtablesize) hashcode = 0;
//					}
//				}
//				DiaSetTextProgress(sim_verprogressdialog, _("Reading stimulus..."));
//				continue;
//			}
//			if (firstKeyword.equals("$dumpvars"))
//			{
//				curtime = 0.0;
//				for(;;)
//				{
//					if (xfgets(sim_verline, 300, sim_verfd)) break;
//					sim_verlineno++;
//					if ((sim_verlineno%1000) == 0)
//					{
//						if (stopping(STOPREASONSIMULATE)) break;
//						curposition = xtell(sim_verfd);
//						DiaSetProgress(sim_verprogressdialog, curposition, sim_verfilesize);
//					}
//					if (sim_verline[0] == '0' || sim_verline[0] == '1' ||
//						sim_verline[0] == 'x' || sim_verline[0] == 'z')
//					{
//						symname = &sim_verline[1];
//						hashcode = sim_vergetsignalhash(symname) % hashtablesize;
//						vs = NOVERSIGNAL;
//						for(i=0; i<hashtablesize; i++)
//						{
//							vs = verhash[hashcode];
//							if (vs == NOVERSIGNAL) break;
//							if (strcmp(symname, vs->symbol) == 0) break;
//							hashcode++;
//							if (hashcode >= hashtablesize) hashcode = 0;
//						}
//						if (vs == NOVERSIGNAL)
//						{
//							ttyputmsg(_("Unknown symbol '%s' on line %ld"), symname, sim_verlineno);
//							continue;
//						}
//
//						/* insert the stimuli */
//						switch (sim_verline[0])
//						{
//							case '0': state = (LOGIC_LOW << 8) | GATE_STRENGTH;   break;
//							case '1': state = (LOGIC_HIGH << 8) | GATE_STRENGTH;  break;
//							case 'x': state = (LOGIC_X << 8) | GATE_STRENGTH;     break;
//							case 'z': state = (LOGIC_Z << 8) | GATE_STRENGTH;     break;
//						}
//						sim_versetvalue(vs, curtime, state);
//						continue;
//					}
//					if (sim_verline[0] == '$')
//					{
//						if (namesame(&sim_verline[0], x_("$end")) == 0) continue;
//						ttyputmsg(_("Unknown directive on line %ld: %s"), sim_verlineno, sim_verline);
//						continue;
//					}
//					if (sim_verline[0] == '#')
//					{
//						curtime = myatoi(&sim_verline[1]) * sim_timescale;
//						continue;
//					}
//					if (sim_verline[0] == 'b')
//					{
//						for(pt = &sim_verline[1]; *pt != 0; pt++)
//							if (*pt == ' ') break;
//						if (*pt == 0)
//						{
//							ttyputmsg(_("Bus has missing signal name on line %ld: %s"), sim_verlineno, sim_verline);
//							continue;
//						}
//						symname = &pt[1];
//						hashcode = sim_vergetsignalhash(symname) % hashtablesize;
//						vs = NOVERSIGNAL;
//						for(i=0; i<hashtablesize; i++)
//						{
//							vs = verhash[hashcode];
//							if (vs == NOVERSIGNAL) break;
//							if (strcmp(symname, vs->symbol) == 0) break;
//							hashcode++;
//							if (hashcode >= hashtablesize) hashcode = 0;
//						}
//						if (vs == NOVERSIGNAL)
//						{
//							ttyputmsg(_("Unknown symbol '%s' on line %ld"), symname, sim_verlineno);
//							continue;
//						}
//						for(i=0; i<vs->width; i++)
//						{
//							switch (sim_verline[i+1])
//							{
//								case '0': state = (LOGIC_LOW << 8) | GATE_STRENGTH;   break;
//								case '1': state = (LOGIC_HIGH << 8) | GATE_STRENGTH;  break;
//								case 'x': state = (LOGIC_X << 8) | GATE_STRENGTH;     break;
//								case 'z': state = (LOGIC_Z << 8) | GATE_STRENGTH;     break;
//							}
//							sim_versetvalue(vs->signals[i], curtime, state);
//						}
//						continue;
//					}
//					ttyputmsg(_("Unknown stimulus on line %ld: %s"), sim_verlineno, sim_verline);
//				}
			}
		}

//		SimData sd = new SimData();
//		sd.signalNames = sim_spice_signames;
//		sd.events = sim_spice_numbers;
		System.out.println("CANNOT READ VERILOG OUTPUT YET");
		return null;
	}

	private void sim_verparsetoend(List keywordList)
		throws IOException
	{
		for(;;)
		{
			for(Iterator it = keywordList.iterator(); it.hasNext(); )
			{
				String keyword = (String)it.next();
				if (keyword.equals("$end")) return;
			}
			String verLine = getLineFromSimulator();
			if (verLine == null) break;
			keywordList = getKeywords(verLine);
		}
	}

	private List getKeywords(String line)
	{
		List keywords = new ArrayList();

		int len = line.length();
		int startOfKeyword = -1;
		for(int i=0; i<len; i++)
		{
			char ch = line.charAt(i);
			if (startOfKeyword < 0)
			{
				if (ch == ' ') continue;
				startOfKeyword = i;
			} else
			{
				if (ch != ' ') continue;
				keywords.add(line.substring(startOfKeyword, i));
				startOfKeyword = -1;
			}
		}
		if (startOfKeyword >= 0)
		{
			keywords.add(line.substring(startOfKeyword, len));
		}
		return keywords;
	}

}
