/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: OutputSpice.java
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
package com.sun.electric.tool.io;

import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.geometry.PolyMerge;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.network.JNetwork;
import com.sun.electric.database.network.Global;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.text.Version;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.FlagSet;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.technologies.Schematics;
import com.sun.electric.tool.io.OutputTopology;
import com.sun.electric.tool.simulation.Simulation;
import com.sun.electric.tool.user.User;

import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Point2D;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.List;
import java.util.HashMap;
import java.util.Date;

//#define DIFFTYPES       3	/* Types of diffusions & transistors plus 1 */
//

//#define sim_spice_puts(s,iscomment) sim_spice_xputs(s, sim_spice_file, iscomment)

//static CHAR       *sim_spice_ac;						/* AC analysis message */
//static CHAR       *sim_spice_dc;						/* DC analysis message */
//static CHAR       *sim_spice_tran;						/* Transient analysis message */
//static INTBIG      sim_spice_unnamednum;
//       INTBIG      sim_spice_nameuniqueid;				/* key for "SIM_spice_nameuniqueid" */
//       INTBIG      sim_spice_listingfilekey;			/* key for "SIM_listingfile" */
//       INTBIG      sim_spice_runargskey;				/* key for "SIM_spice_runarguments" */
//static INTBIG      sim_spicewirelisttotal = 0;
//static NETWORK   **sim_spicewirelist;
//static INTBIG      sim_spice_card_key = 0;				/* key for "SPICE_card" */
//static INTBIG      sim_spiceglobalnetcount;				/* number of global nets */
//static INTBIG      sim_spiceglobalnettotal = 0;			/* size of global net array */
//static CHAR      **sim_spiceglobalnets;					/* global net names */
//static INTBIG      sim_spiceNameUniqueID;				/* for short unique subckt names (JKG) */
//static FILE       *sim_spice_file;                      /* output stream */

///******************** SPICE NET QUEUE ********************/
//

//static SPNET     *sim_spice_netfree = NOSPNET;		/* list of free nets */
//static SPNET     *sim_spice_cur_net;				/* for polygon merging */
//
//static NETWORK   *sim_spice_gnd;					/* net of ground */
//static NETWORK   *sim_spice_vdd;					/* net of power */

/**
 * This is the Simulation Interface tool.
 */
public class OutputSpice extends OutputTopology
{
	/** key of Variable holding generic Spice templates. */		public static final Variable.Key SPICE_TEMPLATE_KEY = ElectricObject.newKey("ATTR_SPICE_template");
	/** key of Variable holding Spice 2 templates. */			public static final Variable.Key SPICE_2_TEMPLATE_KEY = ElectricObject.newKey("ATTR_SPICE_template_spice2");
	/** key of Variable holding Spice 3 templates. */			public static final Variable.Key SPICE_3_TEMPLATE_KEY = ElectricObject.newKey("ATTR_SPICE_template_spice3");
	/** key of Variable holding HSpice templates. */			public static final Variable.Key SPICE_H_TEMPLATE_KEY = ElectricObject.newKey("ATTR_SPICE_template_hspice");
	/** key of Variable holding PSpice templates. */			public static final Variable.Key SPICE_P_TEMPLATE_KEY = ElectricObject.newKey("ATTR_SPICE_template_pspice");
	/** key of Variable holding GnuCap templates. */			public static final Variable.Key SPICE_GC_TEMPLATE_KEY = ElectricObject.newKey("ATTR_SPICE_template_gnucap");
	/** key of Variable holding Smart Spice templates. */		public static final Variable.Key SPICE_SM_TEMPLATE_KEY = ElectricObject.newKey("ATTR_SPICE_template_smartspice");

	/** maximum subcircuit name length */						private static final int SPICEMAXLENSUBCKTNAME     = 70;
	/** legal characters in a spice deck */						private static final String SPICELEGALCHARS        = "!#$%*+-/<>[]_";
	/** legal characters in a CDL deck */						private static final String CDLNOBRACKETLEGALCHARS = "!#$%*+-/<>_";

	private static final int DIFF_NORMAL = 0;
	private static final int DIFF_PTYPE	 = 1;
	private static final int DIFF_NTYPE	 = 2;

	private Technology sim_spice_tech;
	private int sim_spice_netindex;
	/* diffusion layers indices */					private int [] sim_spice_diffusion_index = new int[3];
	private FlagSet markGeom;
	private double  sim_spice_mask_scale;			/* Mask shrink factor (default =1) */
	private boolean sim_spice_cdl;                       /* If "sim_spice_cdl" is true, put handle CDL format */

	/** Duplicated area on each layer */			private float [] sim_spice_extra_area;
	/** Legal characters */							private String sim_spicelegalchars;
	/** Template Key for current spice engine */	private Variable.Key sim_spice_preferedkey;
	/** Spice type: 2, 3, H, P, etc */				private int spiceEngine;

	private static class SpiceNet
	{
		/* network object associated with this */	JNetwork      network;					
			PolyMerge merge;
		/* internal unique net number */			int           netnumber;				
		/* area of diffusion */						float      [] diffarea;		
		/* perimeter of diffusion */				float      [] diffperim;	
		/* amount of resistance */					float         resistance;				
		/* amount of capacitance */					float         capacitance;				
		/* number of components connected to net */	int        [] components;	
	}

	/**
	 * The main entry point for Spice deck writing.
	 * @param cell the top-level cell to write.
	 * @param filePath the disk file to create with Spice.
	 * @return true on error.
	 */
	public static boolean writeSpiceFile(Cell cell, String filePath)
	{
		boolean error = false;
		OutputSpice out = new OutputSpice();
		if (out.openTextOutputStream(filePath)) error = true;
		if (out.writeCell(cell)) error = true;
		if (out.closeTextOutputStream()) error = true;
		if (!error) System.out.println(filePath + " written");
		return error;
	}

	/**
	 * Creates a new instance of Spice
	 */
	OutputSpice()
	{
	}

	protected void start()
	{
		// find the proper technology to use if this is schematics
		sim_spice_tech = Schematics.getDefaultSchematicTechnology();

		// allocate per-layer array for area calculations
		sim_spice_extra_area = new float[sim_spice_tech.getNumLayers()];

		// make sure key is cached
		spiceEngine = Simulation.getSpiceEngine();
		sim_spice_preferedkey = SPICE_TEMPLATE_KEY;
		switch (spiceEngine)
		{
			case Simulation.SPICE_ENGINE_2: sim_spice_preferedkey = SPICE_2_TEMPLATE_KEY;   break;
			case Simulation.SPICE_ENGINE_3: sim_spice_preferedkey = SPICE_3_TEMPLATE_KEY;   break;
			case Simulation.SPICE_ENGINE_H: sim_spice_preferedkey = SPICE_H_TEMPLATE_KEY;   break;
			case Simulation.SPICE_ENGINE_P: sim_spice_preferedkey = SPICE_P_TEMPLATE_KEY;   break;
			case Simulation.SPICE_ENGINE_G: sim_spice_preferedkey = SPICE_GC_TEMPLATE_KEY;   break;
			case Simulation.SPICE_ENGINE_S: sim_spice_preferedkey = SPICE_SM_TEMPLATE_KEY;   break;
		}

//		// get the mask scale
//		var = getval((INTBIG)sim_spice_tech, VTECHNOLOGY, VFLOAT, x_("SIM_spice_mask_scale"));
//		if (var != NOVARIABLE) sim_spice_mask_scale = castfloat(var->addr); else
			sim_spice_mask_scale = 1.0;

		// setup the legal characters
		sim_spicelegalchars = SPICELEGALCHARS;

		// start writing the spice deck
		sim_spice_cdl = false;
		if (sim_spice_cdl)
		{
//			// setup bracket conversion for CDL
//			curstate = io_getstatebits();
//			if ((curstate[1]&CDLNOBRACKETS) != 0)
//				sim_spicelegalchars = CDLNOBRACKETLEGALCHARS;
//
//			(void)estrcpy(deckfile, topCell->protoname);
//			(void)estrcat(deckfile, x_(".cdl"));
//			pt = deckfile;
//			prompt = 0;
//			if ((us_useroptions&NOPROMPTBEFOREWRITE) == 0) prompt = _("CDL File");
//			sim_spice_file = xcreate(deckfile, sim_filetypecdl, prompt, &pt);
//			if (pt != 0) (void)estrcpy(deckfile, pt);
//			if (sim_spice_file == NULL)
//			{
//				ttyputerr(_("Cannot create CDL file: %s"), deckfile);
//				return;
//			}
//			sim_spice_xprintf(true, "* First line is ignored\n");
		} else
		{
//			(void)estrcpy(deckfile, topCell->protoname);
//			(void)estrcat(deckfile, x_(".spi"));
//			pt = deckfile;
//			prompt = 0;
//			if ((us_useroptions&NOPROMPTBEFOREWRITE) == 0) prompt = _("SPICE File");
//			sim_spice_file = xcreate(deckfile, sim_filetypespice, prompt, &pt);
//			if (pt != 0) (void)estrcpy(deckfile, pt);
//			if (sim_spice_file == NULL)
//			{
//				ttyputerr(_("Cannot create SPICE file: %s"), deckfile);
//				return;
//			}
			sim_spice_writeheader(topCell);
		}

		// gather all global signal names (HSPICE and PSPICE only)
//		sim_spiceglobalnetcount = 0;
		Netlist netList = getNetlistForCell(topCell);
		Global.Set globals = netList.getGlobals();
		int globalSize = globals.size();
		if (!Simulation.isSpiceUseNodeNames() || spiceEngine != Simulation.SPICE_ENGINE_3)
		{
			if (globalSize > 0)
			{
				printWriter.print("\n.global");
				for(int i=0; i<globalSize; i++)
				{
					Global global = (Global)globals.get(i);
					printWriter.print(" " + global.getName());
				}
				printWriter.print("\n");
			}
		}

//		sim_spice_unnamednum = 1;
//
//		// initialize for parameterized cells
//		if (sim_spice_cdl || !Simulation.isSpiceUseCellParameters()) initparameterizedcells();
//
//		// we don't know the type of analysis yet...
//		sim_spice_ac = sim_spice_dc = sim_spice_tran = NULL;
//
//		// initialize the polygon merging system
//		mrginit();
//
//		// initialize SpiceCell structures
//		SpiceCell::clearAll();
	}

	protected void done()
	{
//		// handle AC, DC, and TRAN analysis cards
//		analysiscards = 0;
//		if (sim_spice_dc != NULL) analysiscards++;
//		if (sim_spice_tran != NULL) analysiscards++;
//		if (sim_spice_ac != NULL) analysiscards++;
//		if (analysiscards > 1)
//			ttyputerr(_("WARNING: can only have one DC, Transient or AC source node"));
//		if (sim_spice_tran != NULL)
//		{
//			sim_spice_xprintf(false, "%s\n", sim_spice_tran);
//		} else if (sim_spice_ac != NULL)
//		{
//			sim_spice_xprintf(false, "%s\n", sim_spice_ac);
//		} else if (sim_spice_dc != NULL)
//		{
//			sim_spice_xprintf(false, "%s\n", sim_spice_dc);
//		}
//
		if (!sim_spice_cdl)
		{
//			sim_spice_writetrailer(np);
			sim_spice_xprintf(false, ".END\n");
		}

		if (sim_spice_cdl)
		{
//			// write the control files
//			(void)estrcpy(templatefile, np->protoname);
//			(void)estrcat(templatefile, x_(".cdltemplate"));
//			sim_spice_file = xcreate(templatefile, sim_filetypectemp, _("CDL Template File"), &pt);
//			if (pt != 0) (void)estrcpy(templatefile, pt);
//			if (sim_spice_file == NULL)
//			{
//				ttyputerr(_("Cannot create CDL template file: %s"), templatefile);
//				return;
//			}
//			for(i=estrlen(deckfile)-1; i>0; i--) if (deckfile[i] == DIRSEP) break;
//			if (deckfile[i] == DIRSEP) deckfile[i++] = 0;
//			var = getval((INTBIG)io_tool, VTOOL, VSTRING, x_("IO_cdl_library_name"));
//			if (var == NOVARIABLE) libname = x_(""); else
//				libname = (CHAR *)var->addr;
//			var = getval((INTBIG)io_tool, VTOOL, VSTRING, x_("IO_cdl_library_path"));
//			if (var == NOVARIABLE) libpath = x_(""); else
//				libpath = (CHAR *)var->addr;
//			xprintf(sim_spice_file, x_("cdlInKeys = list(nil\n"));
//			xprintf(sim_spice_file, x_("    'searchPath             \"%s"), deckfile);
//			if (libpath[0] != 0)
//				xprintf(sim_spice_file, x_("\n                             %s"), libpath);
//			xprintf(sim_spice_file, x_("\"\n"));
//			xprintf(sim_spice_file, x_("    'cdlFile                \"%s\"\n"), &deckfile[i]);
//			xprintf(sim_spice_file, x_("    'userSkillFile          \"\"\n"));
//			xprintf(sim_spice_file, x_("    'opusLib                \"%s\"\n"), libname);
//			xprintf(sim_spice_file, x_("    'primaryCell            \"%s\"\n"), sim_spice_cellname(np));
//			xprintf(sim_spice_file, x_("    'caseSensitivity        \"preserve\"\n"));
//			xprintf(sim_spice_file, x_("    'hierarchy              \"flatten\"\n"));
//			xprintf(sim_spice_file, x_("    'cellTable              \"\"\n"));
//			xprintf(sim_spice_file, x_("    'viewName               \"netlist\"\n"));
//			xprintf(sim_spice_file, x_("    'viewType               \"\"\n"));
//			xprintf(sim_spice_file, x_("    'pr                     nil\n"));
//			xprintf(sim_spice_file, x_("    'skipDevice             nil\n"));
//			xprintf(sim_spice_file, x_("    'schemaLib              \"sample\"\n"));
//			xprintf(sim_spice_file, x_("    'refLib                 \"\"\n"));
//			xprintf(sim_spice_file, x_("    'globalNodeExpand       \"full\"\n"));
//			xprintf(sim_spice_file, x_(")\n"));
//			xclose(sim_spice_file);
//			ttyputmsg(_("%s written"), templatefile);
//			ttyputmsg(x_("Now type: exec nino CDLIN %s &"), templatefile);
		}

//		// run spice (if requested)
//		var = getvalkey((INTBIG)sim_tool, VTOOL, VINTEGER, sim_dontrunkey);
//		if (var != NOVARIABLE && var->addr != SIMRUNNO)
//		{
//			ttyputmsg(_("Running SPICE..."));
//			var = getvalkey((INTBIG)sim_tool, VTOOL, VSTRING, sim_spice_listingfilekey);
//			if (var == NOVARIABLE) sim_spice_execute(deckfile, x_(""), np); else
//				sim_spice_execute(deckfile, (CHAR *)var->addr, np);
//		}
	}

	/**
	 * Method to write cellGeom
	 */
	protected void writeCellTopology(Cell cell, CellNetInfo cni, VarContext context)
	{
		// look for a model file on the current cell
		Variable var = cell.getVar("SIM_spice_behave_file");
		if (var != null)
		{
			sim_spice_xprintf(true, "* Cell " + cell.describe() + " is described in this file:\n");
			sim_spice_addincludefile(var.getObject().toString());
			return;
		}

		// gather networks in the cell
		Netlist netList = cni.getNetList();

		// make sure power and ground appear at the top level
		if (cell == topCell && !Simulation.isSpiceForceGlobalPwrGnd())
		{
			if (cni.getPowerNet() == null)
				System.out.println("WARNING: cannot find power at top level of circuit");
			if (cni.getGroundNet() == null)
				System.out.println("WARNING: cannot find ground at top level of circuit");
		}

//		int bipolars = 0, nmostrans = 0, pmostrans = 0;

		// create list of electrical nets in this cell
		HashMap sim_spice_firstnet = new HashMap();

//		// must have a default node 0 in subcells
//		nodeindex = subcellindex = 1;
		sim_spice_netindex = 2;	   // save 1 for the substrate

		// look at all networks in the cell
		for(Iterator it = netList.getNetworks(); it.hasNext(); )
		{
			JNetwork net = (JNetwork)it.next();

			// create a "SpiceNet" for the network
			SpiceNet spNet = new SpiceNet();
			spNet.network = net;
			spNet.netnumber = sim_spice_netindex++;
			spNet.merge = new PolyMerge();
			sim_spice_firstnet.put(net, spNet);
		}

		// reset
		for (int j = 0; j < sim_spice_extra_area.length; j++) sim_spice_extra_area[j] = 0;
		for (int j = 0; j < 3; j++) sim_spice_diffusion_index[j] = -1;

		// accumulate geometry of all nodes
		for(Iterator aIt = cell.getNodes(); aIt.hasNext(); )
		{
			NodeInst ni = (NodeInst)aIt.next();
			sim_spice_nodearea(netList, sim_spice_firstnet, ni);
		}

		// accumulate geometry of all arcs
		for(Iterator aIt = cell.getArcs(); aIt.hasNext(); )
		{
			ArcInst ai = (ArcInst)aIt.next();

			// don't count non-electrical arcs
			if (ai.getProto().getFunction() == ArcProto.Function.NONELEC) continue;

			// ignore busses
//			if (ai->network->buswidth > 1) continue;
			JNetwork net = netList.getNetwork(ai, 0);
			SpiceNet spNet = (SpiceNet)sim_spice_firstnet.get(net);
			if (spNet == null) continue;

			sim_spice_arcarea(spNet.merge, ai);
		}

		// get merged polygons so far
		for(Iterator it = netList.getNetworks(); it.hasNext(); )
		{
			JNetwork net = (JNetwork)it.next();
			SpiceNet spNet = (SpiceNet)sim_spice_firstnet.get(net);
			for(Iterator lIt = spNet.merge.getLayersUsed(); lIt.hasNext(); )
			{
				Layer layer = (Layer)lIt.next();
				List points = spNet.merge.getMergedPoints(layer);
				if (points == null) continue;

				// compute perimeter
				double perim = 0;
				int count = points.size();
				for(int i=0; i<count; i++)
				{
					int j = i - 1;
					if (j < 0) j = count - 1;
					perim += ((Point2D)points.get(i)).distance((Point2D)points.get(j));
				}

				// get area
				double area = 0;
//				area = areapoints(count, xbuf, ybuf);
//				if (sim_spice_extra_area[layer] != 0.0)
//				{
//					area -= sim_spice_extra_area[layer];
//					sim_spice_extra_area[layer] = 0.0; // but only once
//				}

				int i = sim_spice_layerdifftype(layer);
				if (i != DIFF_NORMAL)
				{
					spNet.diffarea[i] += area * sim_spice_mask_scale * sim_spice_mask_scale;
					spNet.diffperim[i] += perim * sim_spice_mask_scale;
				} else
				{
//					spNet.capacitance += scaletodispunitsq((INTBIG)layer.getCapacitance() * area), DISPUNITMIC) *
//							sim_spice_mask_scale * sim_spice_mask_scale;
//					spNet.capacitance += scaletodispunit((INTBIG)layer.getEdgeCapacitance() * perim), DISPUNITMIC) *
//							sim_spice_mask_scale;
				}
			}		
		}

		// make sure the ground net is number zero
		JNetwork groundNet = cni.getGroundNet();
		if (cell == topCell && groundNet != null)
		{
			SpiceNet spNet = (SpiceNet)sim_spice_firstnet.get(groundNet);
			if (spNet != null) spNet.netnumber = 0;
		}

//		posnet = negnet = subnet = NOSPNET;
//
//		// second pass through the node list 
//		for(ni = np->firstnodeinst; ni != NONODEINST; ni = ni->nextnodeinst)
//		{
//			state = nodefunction(ni);
//			switch (state)
//			{
//				case NPTRANPN:
//				case NPTRA4NPN:
//				case NPTRAPNP:
//				case NPTRA4PNP:
//				case NPTRANS:
//					nodetype = DIFF_NORMAL;
//					bipolars++;
//					break;
//				case NPTRAEMES:
//				case NPTRA4EMES:
//				case NPTRADMES:
//				case NPTRA4DMES:
//				case NPTRADMOS:
//				case NPTRA4DMOS:
//				case NPTRANMOS:
//				case NPTRA4NMOS:
//					nodetype = DIFF_NTYPE;
//					nmostrans++;
//					break;
//				case NPTRAPMOS:
//				case NPTRA4PMOS:
//					nodetype = DIFF_PTYPE;
//					pmostrans++;
//					break;
//				case NPSUBSTRATE:
//					if (subnet == NOSPNET)
//						subnet = sim_spice_getnet(ni, ni->proto->firstportproto->network);
//					continue;
//				case NPTRANSREF:
//				case NPTRANJFET:
//				case NPTRA4NJFET:
//				case NPTRAPJFET:
//				case NPTRA4PJFET:
//				case NPRESIST:
//				case NPCAPAC:
//				case NPECAPAC:
//				case NPINDUCT:
//				case NPDIODE:
//				case NPDIODEZ:
//				case NPCONGROUND:
//				case NPCONPOWER:
//					nodetype = DIFF_NORMAL;
//					break;
//				default:
//					continue;
//			}
//
//			/*
//			 * find all wired ports on component and increment their count,
//			 * but only if they are a drain or source
//			 */
//			if (nodetype != DIFF_NORMAL)
//			{
//				transistorports(ni, &gate, &gatedummy, &source, &drain);
//				for(spnet = sim_spice_firstnet; spnet != NOSPNET; spnet = spnet->nextnet)
//				{
//					for(pi = ni->firstportarcinst; pi != NOPORTARCINST; pi = pi->nextportarcinst)
//					{
//						if (pi->proto != source) continue;
//						if (spnet->network == pi->conarcinst->network) break;
//					}
//					if (pi != NOPORTARCINST) spnet->components[nodetype]++;
//					for(pi = ni->firstportarcinst; pi != NOPORTARCINST; pi = pi->nextportarcinst)
//					{
//						if (pi->proto != drain) continue;
//						if (spnet->network == pi->conarcinst->network) break;
//					}
//					if (pi != NOPORTARCINST) spnet->components[nodetype]++;
//				}
//			}
//		}
//
//		// use ground net for substrate
//		if (subnet == NOSPNET && sim_spice_gnd != NONETWORK)
//			subnet = (SPNET *)sim_spice_gnd->temp1;
//
//		if (pmostrans != 0 && posnet == NOSPNET)
//		{
//			if (sim_spice_vdd == NONETWORK)
//			{
//				infstr = initinfstr();
//				formatinfstr(infstr, _("WARNING: no power connection for P-transistor wells in cell %s"),
//					describenodeproto(np));
//				sim_spice_dumpstringerror(infstr, 0);
//			} else posnet = (SPNET *)sim_spice_vdd->temp1;
//		}
//		if (nmostrans != 0 && negnet == NOSPNET)
//		{
//			if (sim_spice_gnd == NONETWORK)
//			{
//				infstr = initinfstr();
//				formatinfstr(infstr, _("WARNING: no connection for N-transistor wells in cell %s"),
//					describenodeproto(np));
//				sim_spice_dumpstringerror(infstr, 0);
//			} else negnet = (SPNET *)sim_spice_gnd->temp1;
//		}
//
//		if (bipolars != 0 && subnet == NOSPNET)
//		{
//			infstr = initinfstr();
//			formatinfstr(infstr, _("WARNING: no explicit connection to the substrate in cell %s"),
//				describenodeproto(np));
//			sim_spice_dumpstringerror(infstr, 0);
//			if (sim_spice_gnd != NONETWORK)
//			{
//				ttyputmsg(_("     A connection to ground will be used if necessary."));
//				subnet = (SPNET *)sim_spice_gnd->temp1;
//			}
//		}

		// generate header for subckt or top-level cell
		if (cell == topCell && !sim_spice_cdl)
		{
			sim_spice_xprintf(true, "\n*** TOP LEVEL CELL: " + cell.describe() + "\n");
		} else
		{
			String cellName = cni.getParameterizedName();
			sim_spice_xprintf(false, "\n*** CELL: " + cell.describe() + "\n.SUBCKT " + cellName);
			for(Iterator sIt = cni.getCellAggregateSignals(); sIt.hasNext(); )
			{
				CellAggregateSignal cas = (CellAggregateSignal)sIt.next();

				// ignore networks that aren't exported
				PortProto pp = cas.getExport();
				if (pp == null) continue;

//				if (cs.isGlobal()) continue;
				if (sim_spice_cdl)
				{
//					// if this is output and the last was input (or visa-versa), insert "/"
//					if (i > 0 && netlist[i-1]->temp2 != net->temp2)
//						sim_spice_xprintf(false, " /");
				}

				int low = cas.getLowIndex(), high = cas.getHighIndex();
				if (low > high)
				{
					// single signal
					sim_spice_xprintf(false, " " + cas.getName());
				} else
				{
					for(int j=low; j<=high; j++)
					{
						sim_spice_xprintf(false, " " + cas.getName() + "[" + j + "]");
					}
				}
			}

			Global.Set globals = netList.getGlobals();
			int globalSize = globals.size();
			if (!Simulation.isSpiceUseNodeNames() || spiceEngine == Simulation.SPICE_ENGINE_3)
			{
				for(int i=0; i<globalSize; i++)
				{
					Global global = (Global)globals.get(i);
					printWriter.print(" " + global.getName());
				}
			}
//			if (!sim_spice_cdl && Simulation.isSpiceUseCellParameters())
//			{
//				// add in parameters to this cell
//				for(i=0; i<np->numvar; i++)
//				{
//					var = &np->firstvar[i];
//					if (TDGETISPARAM(var->textdescript) == 0) continue;
//					sim_spice_xprintf(false, " %s=%s", truevariablename(var),
//						describesimplevariable(var));
//				}
//			}
			sim_spice_xprintf(false, "\n");

			// generate pin descriptions for reference (when not using node names)
			for(int i=0; i<globalSize; i++)
			{
				Global global = (Global)globals.get(i);
				sim_spice_xprintf(true, "** GLOBAL " + global.getName() + "\n");
			}

			// write exports to this cell
			for(Iterator sIt = cni.getCellAggregateSignals(); sIt.hasNext(); )
			{
				CellAggregateSignal cas = (CellAggregateSignal)sIt.next();

				// ignore networks that aren't exported
				PortProto pp = cas.getExport();
				if (pp == null) continue;

//				if (cs.isGlobal()) continue;

				int low = cas.getLowIndex(), high = cas.getHighIndex();
				if (low > high)
				{
					// single signal
					sim_spice_xprintf(true, "** PORT " + cas.getName() + "\n");
				} else
				{
					sim_spice_xprintf(true, "** PORT " + cas.getName() + "[" + low + ":" + high + "]\n");
				}
			}
		}

//		// now run through all components in the cell
//		resistnum = capacnum = inductnum = diodenum = 1;

		// third pass through the node list, print it this time
		for(Iterator nIt = netList.getNodables(); nIt.hasNext(); )
		{
			Nodable no = (Nodable)nIt.next();
			NodeProto niProto = no.getProto();

			// handle sub-cell calls
			if (niProto instanceof Cell)
			{
//				// look for a SPICE template on the prototype
//				vartemplate = getvalkey((INTBIG)ni->proto, VNODEPROTO, VSTRING, sim_spice_preferedkey);
//				if (vartemplate == NOVARIABLE)
//					vartemplate = getvalkey((INTBIG)ni->proto, VNODEPROTO, VSTRING, sim_spice_template_key);
//				cnp = contentsview(ni->proto);
//				if (cnp == NONODEPROTO) cnp = ni->proto; else
//				{
//					// if there is a contents view, look for a SPICE template there, too
//					if (vartemplate == NOVARIABLE)
//					{
//						vartemplate = getvalkey((INTBIG)cnp, VNODEPROTO, VSTRING, sim_spice_preferedkey);
//						if (vartemplate == NOVARIABLE)
//							vartemplate = getvalkey((INTBIG)cnp, VNODEPROTO, VSTRING, sim_spice_template_key);
//					}
//				}

				// get the ports on this node (in proper order)
				CellNetInfo subCni = getCellNetInfo(parameterizedName(no, context));

//				// handle self-defined models
//				if (vartemplate != NOVARIABLE)
//				{
//					infstr = initinfstr();
//					for(pt = (CHAR *)vartemplate->addr; *pt != 0; pt++)
//					{
//						if (pt[0] != '$' || pt[1] != '(')
//						{
//							addtoinfstr(infstr, *pt);
//							continue;
//						}
//						start = pt + 2;
//						for(pt = start; *pt != 0; pt++)
//							if (*pt == ')') break;
//						save = *pt;
//						*pt = 0;
//						pp = getportproto(ni->proto, start);
//						if (pp != NOPORTPROTO)
//						{
//							// port name found: use its spice node
//							spnet = sim_spice_getnet(ni, pp->network);
//							if (spnet == NOSPNET)
//							{
//								if (Simulation.isSpiceUseNodeNames())
//									formatinfstr(infstr, x_("UNNAMED%ld"), sim_spice_unnamednum++); else
//										formatinfstr(infstr, x_("%ld"), sim_spice_netindex++);
//								err++;
//							} else
//								addstringtoinfstr(infstr, sim_spice_netname(spnet->network, nodewidth, nindex));
//						} else
//						{
//							// no port name found, look for variable name
//							esnprintf(line, 100, x_("ATTR_%s"), start);
//							var = getval((INTBIG)ni, VNODEINST, -1, line);
//							if (var == NOVARIABLE)
//								var = getval((INTBIG)ni, VNODEINST, -1, start);
//							if (var == NOVARIABLE)
//							{
//								addstringtoinfstr(infstr, x_("??"));
//								err++;
//							} else
//							{
//								if (nodewidth > 1)
//								{
//									// see if this name is arrayed, and pick a single entry from it if so
//									count = net_evalbusname(APBUS, describesimplevariable(var),
//										&strings, NOARCINST, NONODEPROTO, 0);
//									if (count == nodewidth)
//										addstringtoinfstr(infstr, strings[nindex]); else
//											addstringtoinfstr(infstr, describesimplevariable(var));
//								} else
//								{
//									addstringtoinfstr(infstr, describesimplevariable(var));
//								}
//							}
//						}
//						*pt = save;
//						if (save == 0) break;
//					}
//					spInst = new SpiceInst( spCell, returninfstr(infstr) );
//					spInst->addParamM( ni );
//					continue;
//				}

				String modelChar = "X";
				if (no.getName() != null) modelChar += getSafeNetName(no.getName());
				sim_spice_xprintf(false, modelChar);
				for(Iterator sIt = subCni.getCellAggregateSignals(); sIt.hasNext(); )
				{
					CellAggregateSignal cas = (CellAggregateSignal)sIt.next();

					// ignore networks that aren't exported
					PortProto pp = cas.getExport();
					if (pp == null) continue;

					int low = cas.getLowIndex(), high = cas.getHighIndex();
					if (low > high)
					{
						// single signal
						JNetwork net = netList.getNetwork(no, pp, 0);
						CellSignal cs = cni.getCellSignal(net);
						sim_spice_xprintf(false, " " + cs.getName());
					} else
					{
						for(int j=low; j<=high; j++)
						{
							JNetwork net = netList.getNetwork(no, cas.getExport(), j-low);
							CellSignal cs = cni.getCellSignal(net);
							sim_spice_xprintf(false, " " + cs.getName());
						}
					}
				}

				if (!Simulation.isSpiceUseNodeNames() || spiceEngine == Simulation.SPICE_ENGINE_3)
				{
					Global.Set globals = subCni.getNetList().getGlobals();
					int globalSize = globals.size();
					for(int i=0; i<globalSize; i++)
					{
						Global global = globals.get(i);
						sim_spice_xprintf(false, " " + global.getName());
					}
				}
				sim_spice_xprintf(false, " " + subCni.getParameterizedName());

//				if (!sim_spice_cdl && Simulation.isSpiceUseCellParameters())
//				{
//					// add in parameters to this instance
//					for(i=0; i<cnp->numvar; i++)
//					{
//						var = &cnp->firstvar[i];
//						if (TDGETISPARAM(var->textdescript) == 0) continue;
//						nivar = getvalkey((INTBIG)ni, VNODEINST, -1, var->key);
//						CHAR *paramStr = (nivar == NOVARIABLE ? (CHAR*)x_("??") : describesimplevariable(nivar));
//						spInst->addParam( new SpiceParam( paramStr ) );
//					}
//				}
//				if (err != 0)
//				{
//					infstr = initinfstr();
//					formatinfstr(infstr, _("WARNING: subcell %s is not fully connected in cell %s"),
//						describenodeinst(ni), describenodeproto(np));
//					sim_spice_dumpstringerror(infstr, spInst);
//				}
				sim_spice_xprintf(false, "\n");
				continue;
			}

			// get the type of this node
			NodeInst ni = (NodeInst)no;
			NodeProto.Function state = ni.getFunction();

			// handle resistors, inductors, capacitors, and diodes
			if (state == NodeProto.Function.RESIST || state == NodeProto.Function.INDUCT ||
				state == NodeProto.Function.CAPAC || state == NodeProto.Function.ECAPAC ||
				state == NodeProto.Function.DIODE || state == NodeProto.Function.DIODEZ)
			{
//				switch (state)
//				{
//					case NPRESIST:		// resistor
//						var = getvalkey((INTBIG)ni, VNODEINST, -1, sch_resistancekey);
//						if (var == NOVARIABLE) extra = x_(""); else
//						{
//							extra = describesimplevariable(var);
//							if (isanumber(extra))
//							{
//								purevalue = (float)eatof(extra);
//								extra = displayedunits(purevalue, VTUNITSRES, INTRESUNITOHM);
//							}
//						}
//						sim_spice_writetwoport(ni, state, extra, spCell, &resistnum, 1);
//						break;
//					case NPCAPAC:
//					case NPECAPAC:	// capacitor
//						var = getvalkey((INTBIG)ni, VNODEINST, -1, sch_capacitancekey);
//						if (var == NOVARIABLE) extra = x_(""); else
//						{
//							extra = describesimplevariable(var);
//							if (isanumber(extra))
//							{
//								purevalue = (float)eatof(extra);
//								extra = displayedunits(purevalue, VTUNITSCAP, INTCAPUNITFARAD);
//							}
//						}
//						sim_spice_writetwoport(ni, state, extra, spCell, &capacnum, 1);
//						break;
//					case NPINDUCT:		// inductor
//						var = getvalkey((INTBIG)ni, VNODEINST, -1, sch_inductancekey);
//						if (var == NOVARIABLE) extra = x_(""); else
//						{
//							extra = describesimplevariable(var);
//							if (isanumber(extra))
//							{
//								purevalue = (float)eatof(extra);
//								extra = displayedunits(purevalue, VTUNITSIND, INTINDUNITHENRY);
//							}
//						}
//						sim_spice_writetwoport(ni, state, extra, spCell, &inductnum, 1);
//						break;
//					case NPDIODE:		// diode
//					case NPDIODEZ:		// Zener diode
//						var = getvalkey((INTBIG)ni, VNODEINST, -1, sch_diodekey);
//						if (var == NOVARIABLE) extra = x_(""); else
//							extra = describesimplevariable(var);
//						sim_spice_writetwoport(ni, state, extra, spCell, &diodenum, 1);
//						break;
//				}
//				spInst->addParamM( ni );
				continue;
			}

			// the default is to handle everything else as a transistor
			if (niProto.getGroupFunction() != NodeProto.Function.TRANS)
				continue;

			CellSignal gateCs = cni.getCellSignal(netList.getNetwork(ni.getTransistorGatePort()));
			CellSignal sourceCs = cni.getCellSignal(netList.getNetwork(ni.getTransistorSourcePort()));
			CellSignal drainCs = cni.getCellSignal(netList.getNetwork(ni.getTransistorDrainPort()));
			CellSignal biasCs = null;
			PortInst biasPort = ni.getTransistorBiasPort();
			if (biasPort != null)
			{
				biasCs = cni.getCellSignal(netList.getNetwork(biasPort));
			}

//			// make sure transistor is connected to nets
//			if (gateCs == null || sourceCs == null || drainCs == null)
//			{
//				formatinfstr(infstr, _("WARNING: %s not fully connected in cell %s"),
//					describenodeinst(ni), describenodeproto(np));
//				sim_spice_dumpstringerror(infstr, 0);
//			}

			// get any special model information
			String info = null;
//			var = getvalkey((INTBIG)ni, VNODEINST, VSTRING, sch_spicemodelkey);
//			if (var != NOVARIABLE) info = (CHAR *)var->addr;

			String modelChar = "";
			if (state == NodeProto.Function.TRANSREF)			// self-referential transistor
			{
				modelChar = "X"; 
				biasCs = cni.getCellSignal(cni.getGroundNet());
//				info = sim_spice_cellname(niProto);
			} else if (state == NodeProto.Function.TRANMOS)			// NMOS (Enhancement) transistor
			{
				modelChar = "M";
				biasCs = cni.getCellSignal(cni.getGroundNet());
				if (info == null) info = "N";
			} else if (state == NodeProto.Function.TRA4NMOS)		// NMOS (Complementary) 4-port transistor
			{
				modelChar = "M";
				if (info == null) info = "N";
			} else if (state == NodeProto.Function.TRADMOS)			// DMOS (Depletion) transistor
			{
				modelChar = "M";
				biasCs = cni.getCellSignal(cni.getGroundNet());
				if (info == null) info = "D";
			} else if (state == NodeProto.Function.TRA4DMOS)		// DMOS (Depletion) 4-port transistor
			{
				modelChar = "M";
				if (info == null) info = "D";
			} else if (state == NodeProto.Function.TRAPMOS)			// PMOS (Complementary) transistor
			{
				modelChar = "M";
				biasCs = cni.getCellSignal(cni.getPowerNet());
				if (info == null) info = "P";
			} else if (state == NodeProto.Function.TRA4PMOS)		// PMOS (Complementary) 4-port transistor
			{
				modelChar = "M";
				if (info == null) info = "P";
			} else if (state == NodeProto.Function.TRANPN)			// NPN (Junction) transistor
			{
				modelChar = "Q";
//				biasn = subnet != NOSPNET ? subnet : 0;
				if (info == null) info = "NBJT";
			} else if (state == NodeProto.Function.TRA4NPN)			// NPN (Junction) 4-port transistor
			{
				modelChar = "Q";
				if (info == null) info = "NBJT";
			} else if (state == NodeProto.Function.TRAPNP)			// PNP (Junction) transistor
			{
				modelChar = "Q";
//				biasn = subnet != NOSPNET ? subnet : 0;
				if (info == null) info = "PBJT";
			} else if (state == NodeProto.Function.TRA4PNP)			// PNP (Junction) 4-port transistor
			{
				modelChar = "Q";
				if (info == null) info = "PBJT";
			} else if (state == NodeProto.Function.TRANJFET)		// NJFET (N Channel) transistor
			{
				modelChar = "J";
				biasCs = null;
				if (info == null) info = "NJFET";
			} else if (state == NodeProto.Function.TRA4NJFET)		// NJFET (N Channel) 4-port transistor
			{
				modelChar = "J";
				if (info == null) info = "NJFET";
			} else if (state == NodeProto.Function.TRAPJFET)			// PJFET (P Channel) transistor
			{
				modelChar = "J";
				biasCs = null;
				if (info == null) info = "PJFET";
			} else if (state == NodeProto.Function.TRA4PJFET)		// PJFET (P Channel) 4-port transistor
			{
				modelChar = "J";
				if (info == null) info = "PJFET";
			} else if (state == NodeProto.Function.TRADMES ||		// DMES (Depletion) transistor
				state == NodeProto.Function.TRA4DMES)				// DMES (Depletion) 4-port transistor
			{
				modelChar = "Z";
				biasCs = null;
				info = "DMES";
			} else if (state == NodeProto.Function.TRAEMES ||		// EMES (Enhancement) transistor
				state == NodeProto.Function.TRA4EMES)				// EMES (Enhancement) 4-port transistor
			{
				modelChar = "Z";
				biasCs = null;
				info = "EMES";
			} else if (state == NodeProto.Function.TRANS)			// special transistor
			{
				modelChar = "Q";
//				biasn = subnet != NOSPNET ? subnet : 0;
			}
			if (ni.getName() != null) modelChar += getSafeNetName(ni.getName());
			sim_spice_xprintf(false, modelChar + " " + drainCs.getName() + " " + gateCs.getName() + " " + sourceCs.getName());
			if (biasCs != null) sim_spice_xprintf(false, " " + biasCs.getName());
			if (info != null) sim_spice_xprintf(false, " " + info);

//			// compute length and width (or area for nonMOS transistors)
//			nodelambda = lambdaofnode(ni);
//			reallambda = ni->parent->lib->lambda[sim_spice_tech->techindex];
//			transistorsize(ni, &lx, &ly);
//
//			if (lx >= 0 && ly >= 0)
//			{
//				if (!Simulation.isSpiceWriteTransSizeInLambda())
//				{
//					// write sizes in microns
//					if (nodelambda != reallambda && nodelambda != 0)
//					{
//						lx = muldiv(lx, reallambda, nodelambda);
//						ly = muldiv(ly, reallambda, nodelambda);
//					}
//
//					a = sim_spice_mask_scale * lx;
//					b = sim_spice_mask_scale * ly;
//					if (state == NPTRANMOS  || state == NPTRADMOS  || state == NPTRAPMOS ||
//						state == NPTRA4NMOS || state == NPTRA4DMOS || state == NPTRA4PMOS ||
//						((state == NPTRANJFET || state == NPTRAPJFET || state == NPTRADMES ||
//						  state == NPTRAEMES) && spiceEngine == Simulation.SPICE_ENGINE_H))
//					{
//						esnprintf(line, 100, x_("L=%3.2fU"), scaletodispunit((INTBIG)a, DISPUNITMIC));
//						spInst->addParam( new SpiceParam( line ) );
//						esnprintf(line, 100, x_("W=%3.2fU"), scaletodispunit((INTBIG)b, DISPUNITMIC));
//						spInst->addParam( new SpiceParam( line ) );
//					}
//					if (state != NPTRANMOS && state != NPTRADMOS  && state != NPTRAPMOS &&
//						state != NPTRA4NMOS && state != NPTRA4DMOS && state != NPTRA4PMOS)
//					{
//						esnprintf(line, 100, x_("AREA=%3.2fP"), scaletodispunitsq((INTBIG)(a*b), DISPUNITMIC));
//						spInst->addParam( new SpiceParam( line ) );
//					}
//				} else
//					// write sizes in lambda
//				{
//					if (state == NPTRANMOS  || state == NPTRADMOS  || state == NPTRAPMOS ||
//						state == NPTRA4NMOS || state == NPTRA4DMOS || state == NPTRA4PMOS ||
//						((state == NPTRANJFET || state == NPTRAPJFET || state == NPTRADMES ||
//						  state == NPTRAEMES) && spiceEngine == Simulation.SPICE_ENGINE_H))
//					{
//						esnprintf(line, 100, x_("L=%4.2f"), scaletodispunit((INTBIG)lx, DISPUNITLAMBDA));
//						spInst->addParam( new SpiceParam( line ) );
//						esnprintf(line, 100, x_("W=%4.2f"), scaletodispunit((INTBIG)ly, DISPUNITLAMBDA));
//						spInst->addParam( new SpiceParam( line ) );
//					}
//					if (state != NPTRANMOS && state != NPTRADMOS  && state != NPTRAPMOS &&
//						state != NPTRA4NMOS && state != NPTRA4DMOS && state != NPTRA4PMOS)
//					{
//						esnprintf(line, 100, x_("AREA=%4.2f"), scaletodispunitsq((INTBIG)(lx*ly), DISPUNITLAMBDA));
//						spInst->addParam( new SpiceParam( line ) );
//					}
//				}				
//			} else
//			{
//				// if there is nonnumeric size on a schematic transistor, get it
//				varl = getvalkey((INTBIG)ni, VNODEINST, -1, el_attrkey_length);
//				varw = getvalkey((INTBIG)ni, VNODEINST, -1, el_attrkey_width);
//				if (varl != NOVARIABLE && varw != NOVARIABLE)
//				{
//					if (!Simulation.isSpiceWriteTransSizeInLambda())
//					{
//						// write sizes in microns
//						pt = describevariable(varl, -1, -1);
//						if (isanumber(pt))
//						{
//							lx = muldiv(atofr(pt), nodelambda, WHOLE);
//							if (nodelambda != reallambda && nodelambda != 0)
//								lx = muldiv(lx, reallambda, nodelambda);
//							a = sim_spice_mask_scale * lx;
//							esnprintf(line, 100, x_("L=%3.2fU"), scaletodispunit((INTBIG)a, DISPUNITMIC));
//						} else esnprintf(line, 100, x_("L=%s"), pt);
//						spInst->addParam( new SpiceParam( line ) );
//						pt = describevariable(varw, -1, -1);
//						if (isanumber(pt))
//						{
//							lx = muldiv(atofr(pt), nodelambda, WHOLE);
//							if (nodelambda != reallambda && nodelambda != 0)
//								lx = muldiv(lx, reallambda, nodelambda);
//							a = sim_spice_mask_scale * lx;
//							esnprintf(line, 100, x_("W=%3.2fU"), scaletodispunit((INTBIG)a, DISPUNITMIC));
//						} else esnprintf(line, 100, x_("W=%s"), pt);
//						spInst->addParam( new SpiceParam( line ) );
//					} else
//					{
//						// write sizes in lambda
//						pt = describevariable(varl, -1, -1);
//						if (isanumber(pt))
//						{
//							lx = atofr(pt);
//							esnprintf(line, 100, x_("L=%4.2f"), scaletodispunit((INTBIG)lx, DISPUNITLAMBDA));
//						} else esnprintf(line, 100, x_("L=%s"), pt);
//						spInst->addParam( new SpiceParam( line ) );
//						pt = describevariable(varw, -1, -1);
//						if (isanumber(pt))
//						{
//							lx = atofr(pt);
//							esnprintf(line, 100, x_("W=%4.2f"), scaletodispunit((INTBIG)lx, DISPUNITLAMBDA));
//						} else esnprintf(line, 100, x_("W=%s"), pt);
//						spInst->addParam( new SpiceParam( line ) );
//					}
//				}
//			}

//			// make sure transistor is connected to nets
//			if (sourcen == NOSPNET || gaten == NOSPNET || drainn == NOSPNET) continue;
//
//			// compute area of source and drain
//			if (!sim_spice_cdl)
//			{
//				if (state == NPTRANMOS  || state == NPTRADMOS  || state == NPTRAPMOS ||
//					state == NPTRA4NMOS || state == NPTRA4DMOS || state == NPTRA4PMOS)
//				{
//					switch (state)
//					{
//						case NPTRADMOS:
//						case NPTRA4DMOS:
//						case NPTRANMOS:
//						case NPTRA4NMOS: i = DIFF_NTYPE; break;
//						case NPTRAPMOS:
//						case NPTRA4PMOS: i = DIFF_PTYPE; break;
//						default:         i = DIFF_NORMAL;  break;
//					}
//
//					/* we should not look at the DIFF_NORMAL entry of components[],
//					 * but the diffareas will be zero anyhow,
//					 */
//					if (sourcen->components[i] != 0)
//					{
//						a = scaletodispunitsq((INTBIG)(sourcen->diffarea[i] / sourcen->components[i]),
//							DISPUNITMIC);
//						if (a > 0.0)
//						{
//							esnprintf(line, 100, x_("AS=%5.2fP"), a);
//							spInst->addParam( new SpiceParam( line ) );
//						}
//					}
//					if (drainn->components[i] != 0)
//					{
//						b = scaletodispunitsq((INTBIG)(drainn->diffarea[i] / drainn->components[i]),
//							DISPUNITMIC);
//						if (b > 0.0)
//						{
//							esnprintf(line, 100, x_("AD=%5.2fP"), b);
//							spInst->addParam( new SpiceParam( line ) );
//						}
//					}
//
//					// compute perimeters of source and drain
//					if (sourcen->components[i] != 0)
//					{
//						a = scaletodispunit((INTBIG)(sourcen->diffperim[i] / sourcen->components[i]),
//							DISPUNITMIC);
//						if (a > 0.0)
//						{
//							esnprintf(line, 100, x_("PS=%5.2fU"), a);
//							spInst->addParam( new SpiceParam( line ) );
//						}
//					}
//					if (drainn->components[i] != 0)
//					{
//						b = scaletodispunit((INTBIG)(drainn->diffperim[i] / drainn->components[i]),
//							DISPUNITMIC);
//						if (b > 0.0)
//						{
//							esnprintf(line, 100, x_("PD=%5.2fU"), b);
//							spInst->addParam( new SpiceParam( line ) );
//						}
//					}
//				}
//			}
			sim_spice_xprintf(false, "\n");
		}

		// print resistances and capacitances
		if (!sim_spice_cdl)
		{
			if (Simulation.isSpiceUseParasitics())
			{
//				// print parasitic capacitances
//				first = 1;
//				for(spnet = sim_spice_firstnet; spnet != NOSPNET; spnet = spnet->nextnet)
//				{
//					spnet->resistance = scaletodispunitsq((INTBIG)spnet->resistance, DISPUNITMIC);
//					if (spnet->resistance > sim_spice_tech.getMinResistance())
//					{
//						if (first != 0)
//						{
//							first = 0;
//							sim_spice_xprintf(true, "** Extracted Parasitic Elements:\n");
//						}
//						sim_spice_xprintf(false, "R%ld ? ? %9.2f\n", resistnum++, spnet->resistance);
//					}
//
//					if (spnet->network == sim_spice_gnd) continue;
//					if (spnet->capacitance > sim_spice_tech.getMinCapacitance())
//					{
//						if (first != 0)
//						{
//							first = 0;
//							sim_spice_xprintf(true, "** Extracted Parasitic Elements:\n");
//						}
//						sim_spice_xprintf(false, "C%ld%s 0 %9.2fF\n", capacnum++,
//							sim_spice_nodename(spnet), spnet->capacitance);
//					}
//				}
			}
		}

//		// write out any directly-typed SPICE cards
//		if (sim_spice_card_key == 0)
//			sim_spice_card_key = makekey(x_("SIM_spice_card"));
//		spCell->setup();
//		if (cell == topCell && !sim_spice_cdl && spiceEngine == Simulation.SPICE_ENGINE_G)
//			SpiceCell::traverseAll();
//		for(ni = np->firstnodeinst; ni != NONODEINST; ni = ni->nextnodeinst)
//		{
//			if (ni->proto != gen_invispinprim) continue;
//			var = getvalkey((INTBIG)ni, VNODEINST, -1, sim_spice_card_key);
//			if (var == NOVARIABLE) continue;
//			if ((var->type&VTYPE) != VSTRING) continue;
//			if ((var->type&VDISPLAY) == 0) continue;
//			if ((var->type&VISARRAY) == 0)
//			{
//				sim_spice_xprintf(false, "%s\n", (CHAR *)var->addr);
//			} else
//			{
//				len = getlength(var);
//				for(i=0; i<len; i++)
//					sim_spice_xprintf(false, "%s\n", ((CHAR **)var->addr)[i]);
//			}
//		}

		/*
		 * Now we're finished writing the subcircuit.
		 * Only the top-level cell can contain meters and connections.
		 */
		if (cell == topCell && !sim_spice_cdl)
		{
//			// miscellaneous checks
//			for(spnet = sim_spice_firstnet; spnet != NOSPNET; spnet = spnet->nextnet)
//			{
//				for (i = 0; i < DIFFTYPES; i++)
//				{
//					if (spnet->diffarea[i] == 0.0 || spnet->components[i] != 0) continue;
//
//					// do not issue errors for active area on supply rails (probably well contacts)
//					if (spnet->network == sim_spice_vdd || spnet->network == sim_spice_gnd) continue;
//					switch (i)
//					{
//						case DIFF_NTYPE:
//							uncon_diff_type = x_(" N-type");
//							break;
//						case DIFF_PTYPE:
//							uncon_diff_type = x_(" P-type");
//							break;
//						case DIFF_NORMAL:
//						default:
//							uncon_diff_type = x_("");
//							break;
//					}
//					infstr = initinfstr();
//					formatinfstr(infstr, _("WARNING: SPICE node%s has unconnected%s device diffusion in cell %s"),
//						sim_spice_nodename(spnet), uncon_diff_type, describenodeproto(np));
//					sim_spice_dumpstringerror(infstr, 0);
//				}
//			}
		} else
		{
			sim_spice_xprintf(false, ".ENDS " + cni.getParameterizedName() + "\n");
		}
	}

	/****************************** SUBCLASSED METHODS FOR THE TOPOLOGY ANALYZER ******************************/

	/**
	 * Method to adjust a cell name to be safe for Spice output.
	 * @param name the cell name.
	 * @return the name, adjusted for Spice output.
	 */
	protected String getSafeCellName(String name)
	{
		return getSafeNetName(name);
	}

	/*
	 * Method to adjust a network name to be safe for Spice output.
	 * Spice has a list of legal punctuation characters that it allows.
	 */
	protected String getSafeNetName(String name)
	{
		// simple names are trivially accepted as is
		boolean allAlnum = true;
		int len = name.length();
		for(int i=0; i<len; i++)
		{
			if (!Character.isLetterOrDigit(name.charAt(i)))
			{
				allAlnum = false;
				break;
			}
		}
		if (allAlnum) return name;

		StringBuffer sb = new StringBuffer();
		for(int t=0; t<name.length(); t++)
		{
			char chr = name.charAt(t);
			boolean legalChar = Character.isLetterOrDigit(chr);
			if (!legalChar)
			{
				for(int j=0; j<sim_spicelegalchars.length(); j++)
				{
					char legalChr = sim_spicelegalchars.charAt(j);
					if (chr == legalChr) { legalChar = true;   break; }
				}
			}
			if (!legalChar) chr = '_';
			sb.append(chr);
		}
		return sb.toString();
	}

	/**
	 * Method to obtain Netlist information for a cell.
	 * This is pushed to the writer because each writer may have different requirements for resistor inclusion.
	 * Spice ignores resistors.
	 */
	protected Netlist getNetlistForCell(Cell cell)
	{
		// get network information about this cell
		boolean shortResistors = false;
		Netlist netList = cell.getNetlist(shortResistors);
		return netList;
	}

	/**
	 * Method to tell whether the topological analysis should mangle cell names that are parameterized.
	 */
	protected boolean canParameterizeNames() { return !sim_spice_cdl; }

	/**
	 * Method to tell set a limit on the number of characters in a name.
	 * @return the limit to name size (SPICE limits to 32 character names?????). 
	 */
	protected int maxNameLength() { return SPICEMAXLENSUBCKTNAME; }

	/******************** DECK GENERATION SUPPORT ********************/

	/*
	 * write a header for "cell" to spice deck "sim_spice_file"
	 * The model cards come from a file specified by tech:~.SIM_spice_model_file
	 * or else tech:~.SIM_spice_header_level%ld
	 * The spice model file can be located in el_libdir
	 */
	private void sim_spice_writeheader(Cell cell)
	{
		// Print the header line for SPICE 
		sim_spice_xprintf(true, "*** SPICE deck for cell " + cell.noLibDescribe() +
			" from library " + cell.getLibrary().getLibName() + "\n");
		if (User.isIncludeDateAndVersionInOutput())
		{
			SimpleDateFormat sdf = new SimpleDateFormat("EEE MMMM dd, yyyy HH:mm:ss");
			sim_spice_xprintf(true, "*** Created on " + sdf.format(topCell.getCreationDate()) + "\n");
			sim_spice_xprintf(true, "*** Last revised on " + sdf.format(topCell.getRevisionDate()) + "\n");
			sim_spice_xprintf(true, "*** Written on " + sdf.format(new Date()) +
				" by Electric VLSI Design System, version " + Version.CURRENT + " */\n");
		} else
		{
			sim_spice_xprintf(true, "*** Written by Electric VLSI Design System\n");
		}
		emitCopyright("*** ", "");

		sim_spice_xprintf(true, "*** UC SPICE *** , MIN_RESIST " + sim_spice_tech.getMinResistance() +
			", MIN_CAPAC " + sim_spice_tech.getMinCapacitance() + "FF\n");
		sim_spice_xprintf(false, ".OPTIONS NOMOD NOPAGE\n");

		// if sizes to be written in lambda, tell spice conversion factor
		if (Simulation.isSpiceWriteTransSizeInLambda())
		{
			double scale = sim_spice_tech.getScale();
			sim_spice_xprintf(false, "*** Lambda Conversion ***\n");
//			sim_spice_xprintf(false, ".opt scale=%3.3fU\n\n", scaletodispunit((INTBIG)lambda, DISPUNITMIC));
		}

		// see if spice model/option cards from file if specified
		Variable var = sim_spice_tech.getVar("SIM_spice_model_file");
		if (var != null)
		{
			String pt = var.getObject().toString();
			if (pt.startsWith(":::::"))
			{
//				// extension specified: look for a file with the cell name and that extension
//				estrcpy(headerpath, truepath(cell->lib->libfile));
//				pathlen = estrlen(headerpath);
//				liblen = estrlen(skippath(headerpath));
//				if (liblen < pathlen) headerpath[pathlen-liblen-1] = 0; else
//					headerpath[0] = 0;
//				infstr = initinfstr();
//				formatinfstr(infstr, x_("%s%c%s.%s"), headerpath, DIRSEP, sim_spice_cellname(cell), &pt[5]);
//				pt = returninfstr(infstr);
//				if (fileexistence(pt) == 1)
//				{
//					sim_spice_xprintf(true, "* Model cards are described in this file:\n");
//					sim_spice_addincludefile(pt);
//					return;
//				}
			} else
			{
				// normal header file specified
				sim_spice_xprintf(true, "* Model cards are described in this file:\n");
//				io = xopen(pt, el_filetypetext, el_libdir, &truefile);
//				if (io == 0)
//				{
//					ttyputmsg(_("Warning: cannot find model file '%s'"), pt);
//				} else
//				{
//					pt = truefile;
//				}
				sim_spice_addincludefile(pt);
				return;
			}
		}

		// no header files: write predefined header for this level and technology
		int level = TextUtils.atoi(Simulation.getSpiceLevel());
		Variable hVar = sim_spice_tech.getVar("SIM_spice_header_level" + level);
		if (var != null)
		{
			Object obj = hVar.getObject();
			if (obj instanceof String [])
			{
				String [] strings = (String [])obj;
				for(int i=0; i<strings.length; i++)
				{
					sim_spice_xprintf(false, strings[i] + "\n");
				}
				return;
			}
		}
		System.out.println("WARNING: no model cards for SPICE level " + level +
			" in " + sim_spice_tech.getTechName() + " technology");
	}

	/*
	 * Write a trailer from an external file, defined as a variable on
	 * the current technology in this library: tech:~.SIM_spice_trailer_file
	 * if it is available.
	 */
	private void sim_spice_writetrailer(Cell cell)
	{
		// get spice trailer cards from file if specified
		Variable var = sim_spice_tech.getVar("SIM_spice_trailer_file");
		if (var != null)
		{
			String pt = var.getObject().toString();
			if (pt.startsWith(":::::"))
			{
				// extension specified: look for a file with the cell name and that extension
//				estrcpy(trailerpath, truepath(cell->lib->libfile));
//				pathlen = estrlen(trailerpath);
//				liblen = estrlen(skippath(trailerpath));
//				if (liblen < pathlen) trailerpath[pathlen-liblen-1] = 0; else
//					trailerpath[0] = 0;
//				infstr = initinfstr();
//				formatinfstr(infstr, x_("%s%c%s.%s"), trailerpath, DIRSEP, sim_spice_cellname(cell), &pt[5]);
//				pt = returninfstr(infstr);
//				if (fileexistence(pt) == 1)
//				{
//					xprintf(sim_spice_file, x_("* Trailer cards are described in this file:\n"));
//					sim_spice_addincludefile(pt);
//				}
			} else
			{
				// normal trailer file specified
				sim_spice_xprintf(true, "* Trailer cards are described in this file:\n");
				sim_spice_addincludefile(pt);
			}
		}
	}

	/*
	 * Function to write a two port device to the file. If the flag 'report'
	 * is set, then complain about the missing connections.
	 * Determine the port connections from the portprotos in the instance
	 * prototype. Get the part number from the 'part' number value;
	 * increment it. The type of device is declared in type; extra is the string
	 * data acquired before calling here.
	 * If the device is connected to the same net at both ends, do not
	 * write it. Is this OK?
	 */
//	void sim_spice_writetwoport(NODEINST *ni, INTBIG type, CHAR *extra,
//		SpiceCell *cell, INTBIG *part, INTBIG report)
//	{
//		REGISTER PORTPROTO *pp1, *pp2;
//		REGISTER SPNET *end1, *end2;
//		REGISTER void *infstr;
//		CHAR partChar;
//
//		pp1 = ni->proto->firstportproto;
//		pp2 = pp1->nextportproto;
//		end1 = sim_spice_getnet(ni, pp1->network);
//		end2 = sim_spice_getnet(ni, pp2->network);
//
//		// make sure the component is connected to nets
//		if (end1 == NOSPNET || end2 == NOSPNET)
//		{
//			infstr = initinfstr();
//			formatinfstr(infstr, _("WARNING: %s component not fully connected in cell %s"),
//				describenodeinst(ni), describenodeproto(ni->parent));
//			sim_spice_dumpstringerror(infstr, 0);
//		}
//		if (end1 != NOSPNET && end2 != NOSPNET)
//			if (end1->netnumber == end2->netnumber)
//		{
//			if (report)
//			{
//				infstr = initinfstr();
//				formatinfstr(infstr, _("WARNING: %s component appears to be shorted on net %s in cell %s"),
//					describenodeinst(ni), sim_spice_nodename(end1), describenodeproto(ni->parent));
//				sim_spice_dumpstringerror(infstr, 0);
//			}
//			return;
//		}
//
//		// next line is not really necessary any more
//		switch (type)
//		{
//			case NPRESIST:		// resistor
//				partChar = 'R';
//				break;
//			case NPCAPAC:	// capacitor
//			case NPECAPAC:
//				partChar = 'C';
//				break;
//			case NPINDUCT:		// inductor
//				partChar = 'L';
//				break;
//			case NPDIODE:		// diode
//			case NPDIODEZ:		// Zener diode
//				partChar = 'D';
//				break;
//			default:
//				return;
//		}
//		SpiceInst *spInst = new SpiceInst( cell, sim_spice_elementname(ni, partChar, part, 0));
//
//		new SpiceBind( spInst, (end2 != NOSPNET ? end2->spiceNet : 0) );
//		new SpiceBind( spInst, (end1 != NOSPNET ? end1->spiceNet : 0) );  // note order
//		if ((type == NPDIODE || type == NPDIODEZ) && extra[0] == 0) extra = x_("DIODE");
//		spInst->addParam( new SpiceParam( extra ) );
//	}

	/******************** PARASITIC CALCULATIONS ********************/

	/*
	 * Method to recursively determine the area of diffusion and capacitance
	 * associated with port "pp" of nodeinst "ni".  If the node is mult_layer, then
	 * determine the dominant capacitance layer, and add its area; all other
	 * layers will be added as well to the extra_area total.
	 * Continue out of the ports on a complex cell
	 */
	private void sim_spice_nodearea(Netlist netList, HashMap spiceNets, NodeInst ni)
	{
		// cells have no area or capacitance (for now)
		NodeProto np = ni.getProto();
		if (np instanceof Cell) return;  // No area for complex nodes

		NodeProto.Function function = ni.getFunction();

		// initialize to examine the polygons on this node
		Technology tech = np.getTechnology();
		AffineTransform trans = ni.rotateOut();

		/*
		 * NOW!  A fudge to make sure that well capacitors mask out the capacity
		 * to substrate of their top plate polysilicon  or metal
		 */
//		if (function == NPCAPAC || function == NPECAPAC) dominant = -1; else dominant = -2;
//		if (function == NPTRANMOS || function == NPTRA4NMOS ||
//			function == NPTRAPMOS || function == NPTRA4PMOS ||
//			function == NPTRADMOS || function == NPTRA4DMOS ||
//			function == NPTRAEMES || function == NPTRA4EMES ||
//			function == NPTRADMES || function == NPTRA4DMES)
//				function = NPTRANMOS;   // One will do

//		// do we need to test the layers?
//		if (dominant != -1)
//		{
//			if (tot != 0 && firstpoly != NOPOLYGON) dominant = firstpoly->layer;
//
//			// find the layer that will contribute the maximum capacitance
//			if (tot > 1 && tech == el_curtech)
//			{
//				worst = 0.0;
//				for(poly = firstpoly; poly != NOPOLYGON; poly = poly->nextpolygon)
//				{
//					if (sim_spice_layerdifftype(tech, poly->layer) != DIFF_NORMAL)
//					{
//						dominant = -1;      // flag for diffusion on this port
//						break;
//					} else
//					{
//						cap = (float)fabs(areapoly(poly));
//						if (cap * layer.getCapacitance() > worst)
//						{
//							worst = cap;
//							dominant = poly->layer;
//						}
//					}
//				}
//			}
//		}

		// make linked list of polygons
		Poly [] polyList = tech.getShapeOfNode(ni, null, true, true);
		int tot = polyList.length;
		for(int i=0; i<tot; i++)
		{
			Poly poly = polyList[i];

			// make sure this layer connects electrically to the desired port
			if (poly.getPort() == null) continue;
			PortProto pp = poly.getPort();
			JNetwork net = netList.getNetwork(ni, pp, 0);

			// don't bother with layers without capacity
			Layer layer = poly.getLayer();
			if ((sim_spice_layerdifftype(layer) == DIFF_NORMAL) &&
				layer.getCapacitance() == 0.0) continue;

			// leave out the gate capacitance of transistors
			if (function == NodeProto.Function.TRANMOS)
			{
				Layer.Function fun = layer.getFunction();
				if ((layer.getFunctionExtras() & Layer.Function.PSEUDO) == 0 && fun.isPoly())
					continue;
			}

			SpiceNet spNet = (SpiceNet)spiceNets.get(net);
			if (spNet == null) continue;

			// get the area of this polygon
			poly.transform(trans);
			if (layer.getTechnology() != Technology.getCurrent()) continue;
			spNet.merge.addPolygon(layer, poly);
//			if (sim_spice_layerdifftype(tech, poly->layer) == DIFF_NORMAL &&
//				poly->layer != dominant)
//					sim_spice_extra_area[poly->layer] += (float)fabs(areapoly(poly));
		}
	}

	/*
	 * Method to recursively determine the area of diffusion, capacitance, (NOT
	 * resistance) on arc "ai". If the arc contains active device diffusion, then
	 * it will contribute to the area of sources and drains, and the other layers
	 * will be ignored. This is not quite the same as the rule used for
	 * contact (node) structures. Note: the earlier version of this
	 * function assumed that diffusion arcs would always have zero capacitance
	 * values for the other layers; this produces an error if any of these layers
	 * have non-zero values assigned for other reasons. So we will check for the
	 * function of the arc, and if it contains active device, we will ignore any
	 * other layers
	 */
	private void sim_spice_arcarea(PolyMerge merge, ArcInst ai)
	{
		boolean isdiffarc = (sim_spice_arcisdiff(ai) != 0);    // check arc function

		Technology tech = ai.getProto().getTechnology();
		Poly [] arcInstPolyList = tech.getShapeOfArc(ai);
		int tot = arcInstPolyList.length;
		for(int j=0; j<tot; j++)
		{
			Poly poly = arcInstPolyList[j];
			if (poly.getStyle().isText()) continue;

			Layer layer = poly.getLayer();
			if (layer.getTechnology() != Technology.getCurrent()) continue;
			if ((layer.getFunctionExtras() & Layer.Function.PSEUDO) != 0) continue;

			if (sim_spice_layerdifftype(layer) != DIFF_NORMAL ||
				(!isdiffarc && layer.getCapacitance() > 0.0))
					merge.addPolygon(layer, poly);
		}
	}

	/******************** TEXT METHODS ********************/

	/*
	 * Method to insert an "include" of file "filename" into the stream "io".
	 */
	private void sim_spice_addincludefile(String fileName)
	{
		if (spiceEngine == Simulation.SPICE_ENGINE_2 || spiceEngine == Simulation.SPICE_ENGINE_3 ||
			spiceEngine == Simulation.SPICE_ENGINE_G || spiceEngine == Simulation.SPICE_ENGINE_S)
		{
			sim_spice_xprintf(false, ".include " + fileName + "\n");
		} else if (spiceEngine == Simulation.SPICE_ENGINE_H)
		{
			sim_spice_xprintf(false, ".include '" + fileName + "'\n");
		} else if (spiceEngine == Simulation.SPICE_ENGINE_P)
		{
			sim_spice_xprintf(false, ".INC " + fileName + "\n");
		}
	}

	/*
	 * Function to return a spice "element" name.
	 * The first character (eg. R,L,C,D,X,...) is specified by "first".
	 * the rest of the name comes from the name on inst "ni".
	 * If there is no node name, a unique number specified by "counter" is used
	 * and counter is incremented.
	 *
	 * Warning: This method is not re-entrant.  You must use the returned string
	 * before calling the method again.
	 */
//	CHAR *sim_spice_elementname(NODEINST *ni, CHAR first, INTBIG *counter, CHAR *overridename)
//	{
//		VARIABLE *varname;
//		static CHAR s[200];
//
//		if (overridename != 0)
//		{
//			(void)esnprintf(s, 200, x_("%c%s"), first, getSafeNetName(overridename));
//		} else
//		{
//			varname = getvalkey((INTBIG)ni, VNODEINST, VSTRING, el_node_name_key);
//			if (varname == NOVARIABLE)
//			{
//				ttyputerr(_("WARNING: no name on node %s"), describenodeinst(ni));
//				(void)esnprintf(s, 200, x_("%c%ld"), first, (*counter)++);
//			} else
//			{
//				(void)esnprintf(s, 200, x_("%c%s"), first, getSafeNetName((CHAR *)varname->addr));
//			}
//		}
//
//		return(s);
//	}

	/*
	 * Method to return the net name of SPICE net "spnet".
	 * Unknown nets are assigned as node '*'.
	 *
	 * Warning: This method is not re-entrant.  You must use the returned string
	 * before calling the method again.
	 */
//	CHAR *sim_spice_nodename(SPNET *spnet)
//	{
//		REGISTER NETWORK *net;
//		REGISTER void *infstr;
//
//		if (spnet == NOSPNET) net = NONETWORK; else
//			net = spnet->network;
//		infstr = initinfstr();
//		formatinfstr(infstr, x_(" %s"), sim_spice_netname(net, 0, 0));
//		return(returninfstr(infstr));
//	}

	/*
	 * Method to return the net name of net "net".
	 */
//	CHAR *sim_spice_netname(NETWORK *net, INTBIG bussize, INTBIG busindex)
//	{
//		static CHAR s[80];
//		REGISTER SPNET *spnet;
//		REGISTER INTBIG count;
//		CHAR *pt, **strings;
//
//		if (net == NONETWORK)
//		{
//			if (Simulation.isSpiceUseNodeNames())
//				esnprintf(s, 80, x_("UNNAMED%ld"), sim_spice_unnamednum++); else
//					esnprintf(s, 80, x_("%ld"), sim_spice_netindex++);
//			return(s);
//		}
//
//		if (Simulation.isSpiceUseNodeNames())
//		{
//			// SPICE3, HSPICE, or PSPICE only
//			if (sim_spice_machine == SPICE3)
//			{
//				// SPICE3 treats Ground as "0" in top-level cell
//				if (net == sim_spice_gnd && net->parent == topCell) return(x_("0"));
//			}
//			if (net->globalnet >= 0) return(sim_spice_describenetwork(net));
//			if (net->namecount > 0)
//			{
//				pt = networkname(net, 0);
//				if (isdigit(pt[0]) == 0)
//				{
//					if (bussize > 1)
//					{
//						// see if this name is arrayed, and pick a single entry from it if so
//						count = net_evalbusname(APBUS, networkname(net, 0), &strings, NOARCINST, NONODEPROTO, 0);
//						if (count == bussize)
//							return(strings[busindex]);
//					}
//					return(getSafeNetName(pt));
//				}
//			}
//		}
//		spnet = (SPNET *)net->temp1;
//		(void)esnprintf(s, 80, x_("%ld"), spnet->netnumber);
//		return(s);
//	}

//	CHAR *sim_spice_describenetwork(NETWORK *net)
//	{
//		static CHAR gennetname[50];
//		REGISTER NODEPROTO *np;
//
//		// write global net name if present (HSPICE and PSPICE only)
//		if (net->globalnet >= 0)
//		{
//			if (net->globalnet == GLOBALNETGROUND) return(x_("gnd"));
//			if (net->globalnet == GLOBALNETPOWER) return(x_("vdd"));
//			np = net->parent;
//			if (net->globalnet >= np->globalnetcount)
//				return(_("UNKNOWN"));
//			return(np->globalnetnames[net->globalnet]);
//		}
//		if (net->namecount == 0)
//		{
//			esnprintf(gennetname, 50, _("UNNAMED%ld"), (INTBIG)net);
//			return(gennetname);
//		}
//		return(getSafeNetName(networkname(net, 0)));
//	}

	/******************** SUPPORT ********************/

	/*
	 * Method to return nonzero if layer "layer" is on diffusion
	 * Return the type of the diffusion
	 */
	private int sim_spice_layerdifftype(Layer layer)
	{
		int extras = layer.getFunctionExtras();
		if ((extras&Layer.Function.PSEUDO) == 0)
		{
			Layer.Function fun = layer.getFunction();
			if (fun == Layer.Function.DIFFP) return DIFF_PTYPE;
			if (fun == Layer.Function.DIFFN || fun == Layer.Function.DIFF) return DIFF_NTYPE;
		}
		return DIFF_NORMAL;
	}

	/*
	 * Method to return value if arc contains device active diffusion
	 * Return the type of the diffusion, else DIFF_NORMAL
	 */
	private int sim_spice_arcisdiff(ArcInst ai)
	{
		ArcProto.Function fun = ai.getProto().getFunction();
		if (fun == ArcProto.Function.DIFFP) return DIFF_PTYPE;
		if (fun == ArcProto.Function.DIFFN || fun == ArcProto.Function.DIFF) return DIFF_NTYPE;
		if (fun == ArcProto.Function.DIFF) return DIFF_NTYPE;
		if (fun == ArcProto.Function.DIFFS || fun == ArcProto.Function.DIFFW) return DIFF_NTYPE;
		return DIFF_NORMAL;
	}

	/*
	 * Method to search the net list for this cell and return the net number
	 * associated with nodeinst "ni", network "net"
	 */
//	SPNET *sim_spice_getnet(NODEINST *ni, NETWORK *net)
//	{
//		REGISTER SPNET *spnet;
//		REGISTER PORTARCINST *pi;
//		REGISTER PORTEXPINST *pe;
//
//		// search for arcs electrically connected to this port
//		for(pi = ni->firstportarcinst; pi != NOPORTARCINST; pi = pi->nextportarcinst)
//			if (pi->proto->network == net)
//		{
//			for(spnet = sim_spice_firstnet; spnet != NOSPNET; spnet = spnet->nextnet)
//				if (pi->conarcinst->network == spnet->network) return(spnet);
//		}
//
//		// search for exports on the node, connected to this port
//		for(pe = ni->firstportexpinst; pe != NOPORTEXPINST; pe = pe->nextportexpinst)
//			if (pe->proto->network == net)
//		{
//			for(spnet = sim_spice_firstnet; spnet != NOSPNET; spnet = spnet->nextnet)
//				if (pe->exportproto->network == spnet->network) return(spnet);
//		}
//		return(NOSPNET);
//	}

	/******************** LOW-LEVEL OUTPUT METHODS ********************/

	/*
	 * Method to report an error that is built in the infinite string.
	 * The error is sent to the messages window and also to the SPICE deck "f".
	 */
//	void sim_spice_dumpstringerror(void *infstr, SpiceInst *inst)
//	{
//		REGISTER CHAR *error;
//
//		error = returninfstr(infstr);
//		if (inst)
//			inst->addError( error );
//		else
//			sim_spice_xprintf(true, x_("*** %s\n"), error);
//		ttyputmsg(x_("%s"), error);
//	}

	/*
	 * Formatted output to file "stream".  All spice output is in upper case.
	 * The buffer can contain no more than 1024 chars including the newline
	 * and null characters.
	 * Doesn't return anything.
	 */
	private void sim_spice_xprintf(boolean iscomment, String str)
	{
		printWriter.print(str);
	}

	/*
	 * Method to write string "s" onto stream "stream", breaking
	 * it into lines of the proper width, and converting to upper case
	 * if SPICE2.
	 */
//	void sim_spice_xputs(CHAR *s, FILE *stream, BOOLEAN iscomment)
//	{
//		CHAR *pt, *lastspace, contchar;
//		BOOLEAN insidequotes = FALSE;
//		static INTBIG i=0;
//
//		// put in line continuations, if over 78 chars long
//		if (iscomment) contchar = '*'; else
//			contchar = '+';
//		lastspace = NULL;
//		for (pt = s; *pt; pt++)
//		{
//			if (sim_spice_machine == SPICE2)
//			{
//				if (islower(*pt)) *pt = toupper(*pt);
//			}
//			if (*pt == '\n')
//			{
//				i = 0;
//				lastspace = NULL;
//			} else
//			{
//				// removed '/' from check here, not sure why it's there
//				if (*pt == ' ' || *pt == '\'') lastspace = pt;
//				if (*pt == '\'') insidequotes = !insidequotes;
//				++i;
//				if (i >= 78 && !insidequotes)
//				{
//					if (lastspace != NULL)
//					{
//						if( *lastspace == '\'')
//						{
//							*lastspace = '\0';
//							xputs(s, stream);
//							xprintf(stream, x_("'\n%c  "), contchar);
//						} else
//						{
//							*lastspace = '\0';
//							xputs(s, stream);
//							xprintf(stream, x_("\n%c  "), contchar);
//						}
//						s = lastspace + 1;
//						i = 9 + pt-s+1;
//						lastspace = NULL;
//					} else
//					{
//						xprintf(stream, x_("\n%c  "), contchar);
//						i = 9 + 1;
//					}
//				}
//			}
//		}
//		xputs(s, stream);
//	}

	/*
	 * Method to determine the proper name to use for cell "np".
	 */
//	CHAR *sim_spice_cellname(NODEPROTO *np)
//	{
//		REGISTER void *infstr;
//
//		if (np->temp2 == 0) return(np->protoname);
//		infstr = initinfstr();
//		formatinfstr(infstr, "%s-%s", np->lib->libname, np->protoname);
//		return(returninfstr(infstr));
//	}

}
