/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Maker.java
 * Silicon compiler tool (QUISC): make Electric circuitry
 * Written by Andrew R. Kostiuk, Queen's University.
 * Translated to Java by Steven M. Rubin, Sun Microsystems.
 *
 * Copyright (c) 2005 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.sc;

import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveArc;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitivePort;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.sc.SilComp.SCCELLNUMS;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Iterator;

/**
 * This is the generation part of the Silicon Compiler tool.
 */
public class Maker
{
	private static class SCMAKERDATA
	{
		/** cell being layed out */			SilComp.SCCELL	cell;
		/** list of rows */					SCMAKERROW		rows;
		/** list of channels */				SCMAKERCHANNEL	channels;
		/** list of vdd ports */			SCMAKERPOWER	power;
		/** list of ground ports */			SCMAKERPOWER	ground;
		/** minimum x position */			double			minx;
		/** maximum x position */			double			maxx;
		/** minimum y position */			double			miny;
		/** maximum y position */			double			maxy;
	};

	private static class SCMAKERROW
	{
		/** row number */					int				number;
		/** instances in rows */			SCMAKERINST		members;
		/** minimum X position */			double			minx;
		/** maximum X position */			double			maxx;
		/** minimum Y position */			double			miny;
		/** maximum Y position */			double			maxy;
		/** processing bits */				int				flags;
		/** last row */						SCMAKERROW		last;
		/** next row */						SCMAKERROW		next;
	};

	private static class SCMAKERINST
	{
		/** reference place */				Place.SCNBPLACE	place;
		/** reference row */				SCMAKERROW		row;
		/** X position */					double			xpos;
		/** Y position */					double			ypos;
		/** size in X */					double			xsize;
		/** size in Y */					double			ysize;
		/** processing flags */				int				flags;
		/** leaf instance */				NodeInst		instance;
		/** next in row */					SCMAKERINST		next;
	};

	private static class SCMAKERCHANNEL
	{
		/** number of channel */			int				number;
		/** list of tracks */				SCMAKERTRACK	tracks;
		/** number of tracks */				int				num_tracks;
		/** minimum Y position */			double			miny;
		/** Y size */						double			ysize;
		/** processing bits */				int				flags;
		/** last channel */					SCMAKERCHANNEL	last;
		/** next channel */					SCMAKERCHANNEL	next;
	};

	private static class SCMAKERTRACK
	{
		/** track number */					int				number;
		/** nodes in track */				SCMAKERNODE		nodes;
		/** reference track */				Route.SCROUTETRACK	track;
		/** Y position */					double			ypos;
		/** processing bits */				int				flags;
		/** previous track */				SCMAKERTRACK	last;
		/** next track */					SCMAKERTRACK	next;
	};

	private static class SCMAKERNODE
	{
		/** list of vias */					SCMAKERVIA		vias;
		/** next node in track */			SCMAKERNODE		next;
	};

	private static final int SCVIASPECIAL	= 0x00000001;
	private static final int SCVIAEXPORT	= 0x00000002;
	private static final int SCVIAPOWER		= 0x00000004;

	private static class SCMAKERVIA
	{
		/** X position */					double			xpos;
		/** associated channel port */		Route.SCROUTECHPORT	chport;
		/** associated leaf instance */		NodeInst		instance;
		/** flags for processing */			int				flags;
		/** export port */					Route.SCROUTEEXPORT	xport;
		/** next via */						SCMAKERVIA		next;
	};

	private static class SCMAKERPOWER
	{
		/** list of power ports */			SCMAKERPOWERPORT ports;
		/** vertical position of row */		double			ypos;
		/** next in row list */				SCMAKERPOWER	next;
		/** last in row list */				SCMAKERPOWER	last;
	};

	private static class SCMAKERPOWERPORT
	{
		/** instance */						SCMAKERINST		inst;
		/** port on instance */				SilComp.SCNIPORT port;
		/** resultant x position */			double			xpos;
		/** next in list */					SCMAKERPOWERPORT next;
		/** last in list */					SCMAKERPOWERPORT last;
	};

	private static PrimitiveNode sc_layer1proto;
	private static PrimitiveNode sc_layer2proto;
	private static PrimitiveNode sc_viaproto;
	private static PrimitiveNode sc_pwellproto;
	private static PrimitiveNode sc_nwellproto;
	private static ArcProto sc_layer1arc;
	private static ArcProto sc_layer2arc;

	/**
	 * Method to make Electric circuitry from the results of place-and-route.
	 *
	 *   o  Determination of final position
	 *   o  Include squeezing rows in vertical direction
	 *   o  Squeeze tracks together if nonadjacent via
	 *   o  Creating ties to power and ground
	 *   o  Routing Power and Ground buses
	 *   o  Creation in Electric's database
	 */
	public static String Sc_maker()
	{
		// check if working in a cell
		if (SilComp.sc_curcell == null) return "No cell selected";

		// check if placement structure exists
		if (SilComp.sc_curcell.placement == null)
			return "No PLACEMENT structure for cell '" + SilComp.sc_curcell.name + "'";

		// check if route structure exists
		if (SilComp.sc_curcell.route == null)
			return "No ROUTE structure for cell '" + SilComp.sc_curcell.name + "'";

		MakeCircuitry job = new MakeCircuitry();
		return null;
	}

	private static class MakeCircuitry extends Job
	{
		private MakeCircuitry()
		{
			super("Generate Silicon Compilation circuit", SilComp.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			startJob();
		}

		public boolean doIt()
		{
			long startTime = System.currentTimeMillis();
			System.out.println("Starting MAKER...");

			// set up make structure
			SCMAKERDATA make_data = Sc_maker_set_up(SilComp.sc_curcell);

			// create actual layout
			System.out.println("Creating cell " + make_data.cell.name);
			String err = Sc_maker_create_layout(make_data);
			if (err != null)
			{
				System.out.println("ERROR: " + err);
				return false;
			}

//			Sc_free_maker_data(make_data);

			long endTime = System.currentTimeMillis();
			System.out.println("Done (took " + TextUtils.getElapsedTime(endTime - startTime) + ")");
			return true;
		}
	}

	/**
	 * Method to create the data structures to define the precise layout of the cell.
	 * Decide exactly where cells are placed, tracks are laid, via are positioned, etc.
	 * @param cell pointer to cell to layout.
	 * @return created data.
	 */
	private static SCMAKERDATA Sc_maker_set_up(SilComp.SCCELL cell)
	{
		// create top level data structure
		SCMAKERDATA data = new SCMAKERDATA();
		data.cell = cell;
		data.rows = null;
		data.channels = null;
		data.power = null;
		data.ground = null;
		data.minx = Double.MAX_VALUE;
		data.maxx = Double.MIN_VALUE;
		data.miny = Double.MAX_VALUE;
		data.maxy = Double.MIN_VALUE;

		// create Maker Channel and Track data structures
		double row_to_track = (SilComp.getViaSize() / 2) + SilComp.getMinMetalSpacing();
		double min_track_to_track = (SilComp.getViaSize() / 2) +
			SilComp.getMinMetalSpacing() + (SilComp.getHorizArcWidth() / 2);
		double max_track_to_track = SilComp.getViaSize() + SilComp.getMinMetalSpacing();
		SCMAKERCHANNEL last_mchan = null;
		for (Route.SCROUTECHANNEL chan = cell.route.channels; chan != null; chan = chan.next)
		{
			// create Maker Channel structute
			SCMAKERCHANNEL mchan = new SCMAKERCHANNEL();
			mchan.number = chan.number;
			mchan.tracks = null;
			mchan.num_tracks = 0;
			mchan.ysize = 0;
			mchan.flags = 0;
			mchan.next = null;
			mchan.last = last_mchan;
			if (last_mchan != null)
			{
				last_mchan.next = mchan;
			} else
			{
				data.channels = mchan;
			}
			last_mchan = mchan;

			// create Make Track structures
			SCMAKERTRACK last_mtrack = null;
			double ypos = 0;
			for (Route.SCROUTETRACK track = chan.tracks; track != null; track = track.next)
			{
				SCMAKERTRACK mtrack = new SCMAKERTRACK();
				mtrack.number = track.number;
				mtrack.nodes = null;
				mtrack.track = track;
				mtrack.flags = 0;
				mtrack.next = null;
				mtrack.last = last_mtrack;
				if (last_mtrack != null)
				{
					last_mtrack.next = mtrack;
				} else
				{
					mchan.tracks = mtrack;
				}
				last_mtrack = mtrack;
				mchan.num_tracks++;
				if (mtrack.number == 0)
				{
					ypos += row_to_track;
					mtrack.ypos = ypos;
				} else
				{
					// determine if min or max track to track spacing is used
					double deltay = min_track_to_track;
					Route.SCROUTETRACKMEM tr1_mem = track.nodes;
					Route.SCROUTETRACKMEM tr2_mem = track.last.nodes;
					Route.SCROUTECHPORT tr1_port = tr1_mem.node.firstport;
					Route.SCROUTECHPORT tr2_port = tr2_mem.node.firstport;
					while (tr1_port != null && tr2_port != null)
					{
						if (Math.abs(tr1_port.xpos - tr2_port.xpos) < max_track_to_track)
						{
							deltay = max_track_to_track;
							break;
						}
						if (tr1_port.xpos < tr2_port.xpos)
						{
							tr1_port = tr1_port.next;
							if (tr1_port == null)
							{
								tr1_mem = tr1_mem.next;
								if (tr1_mem != null)
									tr1_port = tr1_mem.node.firstport;
							}
						} else
						{
							tr2_port = tr2_port.next;
							if (tr2_port == null)
							{
								tr2_mem = tr2_mem.next;
								if (tr2_mem != null)
									tr2_port = tr2_mem.node.firstport;
							}
						}
					}
					ypos += deltay;
					mtrack.ypos = ypos;
				}
				if (track.next == null)
					ypos += row_to_track;
			}
			mchan.ysize = ypos;
		}

		// create Maker Rows and Instances data structures
		SCMAKERCHANNEL mchan = data.channels;
		mchan.miny = 0;
		double ypos = mchan.ysize;
		SCMAKERROW last_mrow = null;
		for (Place.SCROWLIST row = cell.placement.rows; row != null; row = row.next)
		{
			// create maker row data structure
			SCMAKERROW mrow = new SCMAKERROW();
			mrow.number = row.row_num;
			mrow.members = null;
			mrow.minx = Double.MAX_VALUE;
			mrow.maxx = Double.MIN_VALUE;
			mrow.miny = Double.MAX_VALUE;
			mrow.maxy = Double.MIN_VALUE;
			mrow.flags = 0;
			mrow.next = null;
			mrow.last = last_mrow;
			if (last_mrow != null)
			{
				last_mrow.next = mrow;
			} else
			{
				data.rows = mrow;
			}
			last_mrow = mrow;

			// determine permissible top and bottom overlap
			double toffset = Double.MIN_VALUE;
			double boffset = Double.MAX_VALUE;
			for (Place.SCNBPLACE place = row.start; place != null; place = place.next)
			{
				if (place.cell.type != SilComp.SCLEAFCELL) continue;
				SCCELLNUMS cNums = SilComp.Sc_leaf_cell_get_nums((Cell)place.cell.np);
				toffset = Math.max(toffset, SilComp.Sc_leaf_cell_ysize((Cell)place.cell.np) - cNums.top_active);
				boffset = Math.min(boffset, cNums.bottom_active);
			}
			ypos -= boffset;

			// create maker instance structure for each member in the row
			SCMAKERINST last_minst = null;
			for (Place.SCNBPLACE place = row.start; place != null; place = place.next)
			{
				if (place.cell.type != SilComp.SCLEAFCELL &&
					place.cell.type != SilComp.SCFEEDCELL &&
					place.cell.type != SilComp.SCLATERALFEED) continue;
				SCMAKERINST minst = new SCMAKERINST();
				minst.place = place;
				minst.row = mrow;
				minst.xpos = place.xpos;
				minst.ypos = ypos;
				minst.xsize = place.cell.size;
				if (place.cell.type == SilComp.SCLEAFCELL)
				{
					minst.ysize = SilComp.Sc_leaf_cell_ysize((Cell)place.cell.np);

					// add power ports
					for (SilComp.SCNIPORT iport = place.cell.power; iport != null; iport = iport.next)
					{
						SCMAKERPOWERPORT power_port = new SCMAKERPOWERPORT();
						power_port.inst = minst;
						power_port.port = iport;
						if ((mrow.number % 2) != 0)
						{
							power_port.xpos = minst.xpos + minst.xsize - iport.xpos;
						} else
						{
							power_port.xpos = minst.xpos + iport.xpos;
						}
						power_port.next = null;
						power_port.last = null;
						double port_ypos = minst.ypos + SilComp.Sc_leaf_port_ypos((Export)iport.port);
						SCMAKERPOWER plist;
						for (plist = data.power; plist != null; plist = plist.next)
						{
							if (plist.ypos == port_ypos) break;
						}
						if (plist == null)
						{
							plist = new SCMAKERPOWER();
							plist.ports = null;
							plist.ypos = port_ypos;
							SCMAKERPOWER last_plist = null;
							SCMAKERPOWER next_plist;
							for (next_plist = data.power; next_plist != null;
								next_plist = next_plist.next)
							{
								if (port_ypos < next_plist.ypos) break;
								last_plist = next_plist;
							}
							plist.next = next_plist;
							plist.last = last_plist;
							if (last_plist != null)
							{
								last_plist.next = plist;
							} else
							{
								data.power = plist;
							}
							if (next_plist != null)
							{
								next_plist.last = plist;
							}
						}
						SCMAKERPOWERPORT last_port = null;
						SCMAKERPOWERPORT next_port;
						for (next_port = plist.ports; next_port != null;
							next_port = next_port.next)
						{
							if (power_port.xpos < next_port.xpos) break;
							last_port = next_port;
						}
						power_port.next = next_port;
						power_port.last = last_port;
						if (last_port != null)
						{
							last_port.next = power_port;
						} else
						{
							plist.ports = power_port;
						}
						if (next_port != null)
						{
							next_port.last = power_port;
						}
					}

					// add ground ports
					for (SilComp.SCNIPORT iport = place.cell.ground; iport != null; iport = iport.next)
					{
						SCMAKERPOWERPORT power_port = new SCMAKERPOWERPORT();
						power_port.inst = minst;
						power_port.port = iport;
						if ((mrow.number % 2) != 0)
						{
							power_port.xpos = minst.xpos + minst.xsize -
								iport.xpos;
						} else
						{
							power_port.xpos = minst.xpos + iport.xpos;
						}
						power_port.next = null;
						power_port.last = null;
						double port_ypos = minst.ypos + SilComp.Sc_leaf_port_ypos((Export)iport.port);
						SCMAKERPOWER plist;
						for (plist = data.ground; plist != null; plist = plist.next)
						{
							if (plist.ypos == port_ypos) break;
						}
						if (plist == null)
						{
							plist = new SCMAKERPOWER();
							plist.ports = null;
							plist.ypos = port_ypos;
							SCMAKERPOWER last_plist = null;
							SCMAKERPOWER next_plist;
							for (next_plist = data.ground; next_plist != null; next_plist = next_plist.next)
							{
								if (port_ypos < next_plist.ypos) break;
								last_plist = next_plist;
							}
							plist.next = next_plist;
							plist.last = last_plist;
							if (last_plist != null)
							{
								last_plist.next = plist;
							} else
							{
								data.ground = plist;
							}
							if (next_plist != null)
							{
								next_plist.last = plist;
							}
						}
						SCMAKERPOWERPORT last_port = null;
						SCMAKERPOWERPORT next_port;
						for (next_port = plist.ports; next_port != null; next_port = next_port.next)
						{
							if (power_port.xpos < next_port.xpos) break;
							last_port = next_port;
						}
						power_port.next = next_port;
						power_port.last = last_port;
						if (last_port != null)
						{
							last_port.next = power_port;
						} else
						{
							plist.ports = power_port;
						}
						if (next_port != null)
						{
							next_port.last = power_port;
						}
					}
				} else if (place.cell.type == SilComp.SCFEEDCELL)
				{
					minst.ysize = boffset;
				} else if (place.cell.type == SilComp.SCLATERALFEED)
				{
					Route.SCROUTEPORT rport = (Route.SCROUTEPORT)place.cell.ports.port;
					minst.ysize = SilComp.Sc_leaf_port_ypos((Export)rport.port.port);
				} else
				{
					System.out.println("ERROR - unknown cell type in maker set up");
					minst.ysize = 0;
				}
				minst.instance = null;
				minst.flags = 0;
				place.cell.tp = minst;
				minst.next = null;
				if (last_minst != null)
				{
					last_minst.next = minst;
				} else
				{
					mrow.members = minst;
				}
				last_minst = minst;

				// set limits of row
				mrow.minx = Math.min(mrow.minx, minst.xpos);
				mrow.maxx = Math.max(mrow.maxx, minst.xpos + minst.xsize);
				mrow.miny = Math.min(mrow.miny, minst.ypos);
				mrow.maxy = Math.max(mrow.maxy, minst.ypos + minst.ysize);
			}
			data.minx = Math.min(data.minx, mrow.minx);
			data.maxx = Math.max(data.maxx, mrow.maxx);
			data.miny = Math.min(data.miny, mrow.miny);
			data.maxy = Math.max(data.maxy, mrow.maxy);
			ypos += toffset;
			mchan = mchan.next;
			mchan.miny = ypos;
			ypos += mchan.ysize;
		}

		// create via list for all tracks
		for (mchan = data.channels; mchan != null; mchan = mchan.next)
		{
			// get bottom track and work up
			SCMAKERTRACK mtrack;
			for (mtrack = mchan.tracks; mtrack != null; mtrack = mtrack.next)
			{
				if (mtrack.next == null) break;
			}
			for ( ; mtrack != null; mtrack = mtrack.last)
			{
				ypos = mchan.miny + (mchan.ysize - mtrack.ypos);
				mtrack.ypos = ypos;
				for (Route.SCROUTETRACKMEM mem = mtrack.track.nodes; mem != null; mem = mem.next)
				{
					SCMAKERNODE mnode = new SCMAKERNODE();
					mnode.vias = null;
					mnode.next = mtrack.nodes;
					mtrack.nodes = mnode;
					SCMAKERVIA lastvia = null;
					for (Route.SCROUTECHPORT chport = mem.node.firstport; chport != null; chport = chport.next)
					{
						SCMAKERVIA mvia = new SCMAKERVIA();
						mvia.xpos = chport.xpos;
						mvia.chport = chport;
						mvia.instance = null;
						mvia.flags = 0;
						mvia.xport = null;
						mvia.next = null;
						if (lastvia != null)
						{
							lastvia.next = mvia;
						} else
						{
							mnode.vias = mvia;
						}
						lastvia = mvia;

						// check for power port
						if (mvia.chport.port.place.cell.type == SilComp.SCLEAFCELL)
						{
							int type = SilComp.Sc_leaf_port_type((Export)mvia.chport.port.port.port);
							if (type == SilComp.SCPWRPORT || type == SilComp.SCGNDPORT)
								mvia.flags |= SCVIAPOWER;
						}

						// check for export
						for (Route.SCROUTEEXPORT xport = data.cell.route.exports; xport != null; xport = xport.next)
						{
							if (xport.chport == mvia.chport)
							{
								mvia.flags |= SCVIAEXPORT;
								mvia.xport = xport;
								break;
							}
						}

						data.minx = Math.min(data.minx, chport.xpos);
						data.maxx = Math.max(data.maxx, chport.xpos);
						data.miny = Math.min(data.miny, chport.xpos);
						data.maxy = Math.max(data.maxy, chport.xpos);
					}
				}
			}
		}

		return data;
	}

	/**
	 * Method to create the actual layout in the associated VLSI layout tool using
	 * the passed layout data.
	 * @param data pointer to layout data.
	 */
	private static String Sc_maker_create_layout(SCMAKERDATA data)
	{
		double row_to_track = (SilComp.getViaSize() / 2) + SilComp.getMinMetalSpacing();
		double track_to_track = SilComp.getViaSize() + SilComp.getMinMetalSpacing();

		String err = Sc_setup_for_maker();
		if (err != null) return err;

		// create new cell
		Cell bcell = Cell.makeInstance(Library.getCurrent(), data.cell.name + "{lay}");
		if (bcell == null) return "Cannot create leaf cell '" + data.cell.name + "{lay}' in MAKER";

		// create instances for cell
		for (SCMAKERROW row = data.rows; row != null; row = row.next)
		{
			boolean flipX = false;
			if ((row.number % 2) != 0) flipX = true;
			for (SCMAKERINST inst = row.members; inst != null; inst = inst.next)
			{
				SilComp.SCNITREE node = inst.place.cell;
				if (node.type == SilComp.SCLEAFCELL)
				{
					Cell subCell = (Cell)node.np;
					Rectangle2D bounds = subCell.getBounds();
					double wid = inst.xsize;
					double hEdge = -bounds.getMinX();
					if (flipX)
					{
						wid = -wid;
						hEdge = bounds.getMaxX();
					}
					Point2D ctr = new Point2D.Double(inst.xpos + hEdge, inst.ypos - bounds.getMinY());
					inst.instance = NodeInst.makeInstance(subCell, ctr, wid, inst.ysize, bcell, 0, node.name, 0);
					if (inst.instance == null)
						return "Cannot create leaf instance '" + node.name+ "' in MAKER";
				} else if (node.type == SilComp.SCFEEDCELL)
				{
					// feed through node
					Point2D ctr = new Point2D.Double(inst.xpos + (inst.xsize / 2), inst.ypos + inst.ysize + SilComp.getVertArcWidth() / 2);
					inst.instance = NodeInst.makeInstance(sc_layer2proto, ctr, SilComp.getVertArcWidth(), SilComp.getVertArcWidth(), bcell);
					if (inst.instance == null)
						return "Cannot create leaf feed in MAKER";
				} else if (node.type == SilComp.SCLATERALFEED)
				{
					// lateral feed node
					Point2D ctr = new Point2D.Double(inst.xpos + (inst.xsize / 2), inst.ypos + inst.ysize);
					inst.instance = NodeInst.makeInstance(sc_viaproto, ctr, sc_viaproto.getDefWidth(), sc_viaproto.getDefHeight(), bcell);
					if (inst.instance == null)
						return "Cannot create via in MAKER";
				}
			}
		}

		// create vias and vertical tracks
		for (SCMAKERCHANNEL chan = data.channels; chan != null; chan = chan.next)
		{
			for (SCMAKERTRACK track = chan.tracks; track != null; track = track.next)
			{
				for (SCMAKERNODE mnode = track.nodes; mnode != null; mnode = mnode.next)
				{
					for (SCMAKERVIA via = mnode.vias; via != null; via = via.next)
					{
						if ((via.flags & SCVIAPOWER) != 0)
						{
							via.instance = NodeInst.makeInstance(sc_layer1proto, new Point2D.Double(via.xpos, track.ypos),
								SilComp.getHorizArcWidth(), SilComp.getHorizArcWidth(), bcell);
							if (via.instance == null)
								return "Cannot create via in MAKER";

							// create vertical power track
							SCMAKERINST inst = (SCMAKERINST)via.chport.port.place.cell.tp;
							if (Sc_create_track_layer1(inst.instance, (PortProto)via.chport.port.port.port,
								via.instance, null, SilComp.getHorizArcWidth(), bcell) == null)
							{
								return "Cannot create layer2 track in MAKER";
							}
							continue;
						}

						// create a via if next via (if it exists) is farther
						// than the track to track spacing, else create a layer2 node
						if (via.next != null && (via.next.flags & SCVIAPOWER) == 0)
						{
							if (Math.abs(via.next.xpos - via.xpos) < track_to_track)
							{
								if ((via.flags & SCVIAEXPORT) != 0)
								{
									via.next.flags |= SCVIASPECIAL;
								} else
								{
									via.flags |= SCVIASPECIAL;
								}
							}
						}
						if ((via.flags & SCVIASPECIAL) != 0)
						{
							via.instance = NodeInst.makeInstance(sc_layer2proto, new Point2D.Double(via.xpos, track.ypos),
								SilComp.getVertArcWidth(), SilComp.getVertArcWidth(), bcell);
							if (via.instance == null)
								return "Cannot create leaf feed in MAKER";
						} else
						{
							via.instance = NodeInst.makeInstance(sc_viaproto, new Point2D.Double(via.xpos, track.ypos),
								sc_viaproto.getDefWidth(), sc_viaproto.getDefHeight(), bcell);
							if (via.instance == null)
								return "Cannot create via in MAKER";
						}

						// create vertical track
						SilComp.SCNITREE node = via.chport.port.place.cell;
						if (node.type == SilComp.SCLEAFCELL)
						{
							SCMAKERINST inst = (SCMAKERINST)node.tp;
							if (Sc_create_track_layer2(inst.instance, (PortProto)via.chport.port.port.port,
								via.instance, null, SilComp.getVertArcWidth(), bcell) == null)
							{
								return "Cannot create layer2 track in MAKER";
							}
						} else if (node.type == SilComp.SCFEEDCELL ||
							node.type == SilComp.SCLATERALFEED)
						{
							SCMAKERINST inst = (SCMAKERINST)node.tp;
							if (Sc_create_track_layer2(inst.instance, null,
								via.instance, null, SilComp.getVertArcWidth(), bcell) == null)
							{
								return "Cannot create layer2 track in MAKER";
							}
						}
					}
				}
			}
		}

		// create horizontal tracks
		for (SCMAKERCHANNEL chan = data.channels; chan != null; chan = chan.next)
		{
			for (SCMAKERTRACK track = chan.tracks; track != null; track = track.next)
			{
				for (SCMAKERNODE mnode = track.nodes; mnode != null; mnode = mnode.next)
				{
					for (SCMAKERVIA via = mnode.vias; via != null; via = via.next)
					{
						if (via.next != null)
						{
							if ((via.flags & SCVIASPECIAL) != 0)
							{
								if (Math.abs(via.next.xpos - via.xpos) < track_to_track)
								{
									if (Sc_create_track_layer2(via.instance, null,
										via.next.instance, null, SilComp.getVertArcWidth(), bcell) == null)
									{
										return "Cannot create layer1 track in MAKER";
									}
								}
							} else
							{
								if ((via.flags & SCVIAPOWER) == 0 &&
									(via.next.flags & SCVIAPOWER) == 0 &&
									(via.next.xpos - via.xpos) < track_to_track)
								{
									if (Sc_create_track_layer2(via.instance, null,
										via.next.instance, null, SilComp.getVertArcWidth(), bcell) == null)
									{
										return "Cannot create layer1 track in MAKER";
									}
								}
								for (SCMAKERVIA via2 = via.next; via2 != null; via2 = via2.next)
								{
									if ((via2.flags & SCVIASPECIAL) != 0) continue;
									if (Sc_create_track_layer1(via.instance, null, via2.instance, null,
										SilComp.getHorizArcWidth(), bcell) == null)
									{
										return "Cannot create layer1 track in MAKER";
									}
									break;
								}
							}
						}
					}
				}
			}
		}

		// create stitches and lateral feeds
		for (Place.SCROWLIST rlist = data.cell.placement.rows; rlist != null; rlist = rlist.next)
		{
			for (Place.SCNBPLACE place = rlist.start; place != null; place = place.next)
			{
				if (place.cell.type == SilComp.SCSTITCH)
				{
					Route.SCROUTEPORT rport = (Route.SCROUTEPORT)place.cell.ports.port;
					SCMAKERINST inst = (SCMAKERINST)rport.place.cell.tp;
					NodeInst inst1 = inst.instance;
					PortProto port1 = (PortProto)rport.port.port;
					rport = (Route.SCROUTEPORT)place.cell.ports.next.port;
					inst = (SCMAKERINST)rport.place.cell.tp;
					NodeInst inst2 = inst.instance;
					PortProto port2 = (PortProto)rport.port.port;
					if (Sc_create_track_layer1(inst1, port1, inst2, port2,
						SilComp.getHorizArcWidth(), bcell) == null)
					{
						return "Cannot create layer1 track in MAKER";
					}
				} else if (place.cell.type == SilComp.SCLATERALFEED)
				{
					Route.SCROUTEPORT rport = (Route.SCROUTEPORT)place.cell.ports.port;
					SCMAKERINST inst = (SCMAKERINST)rport.place.cell.tp;
					NodeInst inst1 = inst.instance;
					PortProto port1 = (PortProto)rport.port.port;
					inst = (SCMAKERINST)place.cell.tp;
					NodeInst inst2 = inst.instance;
					if (Sc_create_track_layer1(inst1, port1, inst2, null,
						SilComp.getHorizArcWidth(), bcell) == null)
					{
						return "Cannot create layer2 track in MAKER";
					}
				}
			}
		}

		// export ports
		for (SCMAKERCHANNEL chan = data.channels; chan != null; chan = chan.next)
		{
			for (SCMAKERTRACK track = chan.tracks; track != null; track = track.next)
			{
				for (SCMAKERNODE mnode = track.nodes; mnode != null; mnode = mnode.next)
				{
					for (SCMAKERVIA via = mnode.vias; via != null; via = via.next)
					{
						if ((via.flags & SCVIAEXPORT) != 0)
						{
							if (Sc_create_export_port(via.instance, null,
								via.xport.xport.name, via.xport.xport.bits & SilComp.SCPORTTYPE, bcell) == null)
							{
								return "Cannot create export port '" + via.xport.xport.name + "' in MAKER";
							}
						}
					}
				}
			}
		}

		// create power buses
		NodeInst lastpower = null;
		double xpos = data.minx - row_to_track - (SilComp.getMainPowerWireWidth()/ 2);

		int mainpwrrail = 0; // ScGetParameter(SC_PARAM_MAKE_MAIN_PWR_RAIL);

		for (SCMAKERPOWER plist = data.power; plist != null; plist = plist.next)
		{
			double ypos = plist.ypos;

			// create main power bus node
			NodeInst binst = null;
			if (mainpwrrail == 0)
			{
				binst = NodeInst.makeInstance(sc_layer1proto, new Point2D.Double(xpos, ypos),
					SilComp.getMainPowerWireWidth(), SilComp.getMainPowerWireWidth(), bcell);
			} else
			{
				binst = NodeInst.makeInstance(sc_layer2proto, new Point2D.Double(xpos, ypos),
					SilComp.getMainPowerWireWidth(), SilComp.getMainPowerWireWidth(), bcell);
			}
			if (binst == null)
				return "Cannot create via in MAKER";
			if (lastpower != null)
			{
				// join to previous
				if (mainpwrrail == 0)
				{
					if (Sc_create_track_layer1(binst, null, lastpower, null,
						SilComp.getMainPowerWireWidth(), bcell) == null)
					{
						return "Cannot create layer1 track in MAKER";
					}
				} else
				{
					if (Sc_create_track_layer2(binst, null, lastpower, null,
						SilComp.getMainPowerWireWidth(), bcell) == null)
					{
						return "Cannot create layer1 track in MAKER";
					}
				}
			}
			lastpower = binst;

			for (SCMAKERPOWERPORT pport = plist.ports; pport != null; pport = pport.next)
			{
				if (pport.last == null)
				{
					// connect to main power node
					if (Sc_create_track_layer1(lastpower, null,
						pport.inst.instance, (PortProto)pport.port.port,
						SilComp.getPowerWireWidth(), bcell) == null)
					{
						return "Cannot create layer1 track in MAKER";
					}
				}

				// connect to next if it exists
				if (pport.next != null)
				{
					if (Sc_create_track_layer1(pport.inst.instance, (PortProto)pport.port.port,
						pport.next.inst.instance, (PortProto)pport.next.port.port,
						SilComp.getPowerWireWidth(), bcell) == null)
					{
						return "Cannot create layer1 track in MAKER";
					}
				}
			}
		}

		// create ground buses
		NodeInst lastground = null;
		xpos = data.maxx + row_to_track + (SilComp.getMainPowerWireWidth() / 2);

		for (SCMAKERPOWER plist = data.ground; plist != null; plist = plist.next)
		{
			double ypos = plist.ypos;

			// create main ground bus node
			NodeInst binst = null;
			if (mainpwrrail == 0)
			{
				binst = NodeInst.makeInstance(sc_layer1proto, new Point2D.Double(xpos, ypos),
					SilComp.getMainPowerWireWidth(), SilComp.getMainPowerWireWidth(), bcell);
			} else
			{
				binst = NodeInst.makeInstance(sc_layer2proto, new Point2D.Double(xpos, ypos),
					SilComp.getMainPowerWireWidth(), SilComp.getMainPowerWireWidth(), bcell);
			}
			if (binst == null) return "Cannot create via in MAKER";
			if (lastground != null)
			{
				// join to previous
				if (mainpwrrail == 0)
				{
					if (Sc_create_track_layer1(binst, null, lastground, null,
						SilComp.getMainPowerWireWidth(), bcell) == null)
					{
						return "Cannot create layer1 track in MAKER";
					}
				} else
				{
					if (Sc_create_track_layer2(binst, null, lastground, null,
						SilComp.getMainPowerWireWidth(), bcell) == null)
					{
						return "Cannot create layer1 track in MAKER";
					}
				}
			} else
			{
				if (Sc_create_export_port(binst, null, "gnd", SilComp.SCGNDPORT, bcell) == null)
				{
					return "Cannot create export port 'gnd' in MAKER";
				}
			}
			lastground = binst;

			for (SCMAKERPOWERPORT pport = plist.ports; pport != null; pport = pport.next)
			{
				if (pport.next == null)
				{
					// connect to main ground node
					if (Sc_create_track_layer1(lastground, null,
						pport.inst.instance, (PortProto)pport.port.port,
						SilComp.getPowerWireWidth(), bcell) == null)
					{
						return "Cannot create layer1 track in MAKER";
					}
				}
				// connect to next if it exists
				else
				{
					if (Sc_create_track_layer1(pport.inst.instance, (PortProto)pport.port.port,
						pport.next.inst.instance, (PortProto)pport.next.port.port,
						SilComp.getPowerWireWidth(), bcell) == null)
					{
						return "Cannot create layer1 track in MAKER";
					}
				}
			}
		}
		if (lastpower != null)
		{
			// export as cell vdd
			if (Sc_create_export_port(lastpower, null, "vdd", SilComp.SCPWRPORT, bcell) == null)
			{
				return "Cannot create export port 'vdd' in MAKER";
			}
		}

		// create overall P-wells if pwell size not zero
		if (SilComp.getPWellHeight() != 0)
		{
			for (SCMAKERROW row = data.rows; row != null; row = row.next)
			{
				SCMAKERINST firstinst = null;
				SCMAKERINST previnst = null;
				for (SCMAKERINST inst = row.members; inst != null; inst = inst.next)
				{
					if (inst.place.cell.type != SilComp.SCLEAFCELL) continue;
					if (firstinst == null)
					{
						firstinst = inst;
					} else
					{
						previnst = inst;
					}
				}
				if (previnst != null)
				{
					xpos = (firstinst.xpos + previnst.xpos + previnst.xsize) / 2;
					double xsize = (previnst.xpos + previnst.xsize) - firstinst.xpos;
					double ysize = SilComp.getPWellHeight();
					if (ysize > 0)
					{
						double ypos = firstinst.ypos + SilComp.getPWellOffset() +
							(SilComp.getPWellHeight() / 2);
						if (sc_pwellproto != null)
						{
							NodeInst binst = NodeInst.makeInstance(sc_pwellproto, new Point2D.Double(xpos, ypos), xsize, ysize, bcell);
							if (binst == null)
								return "Unable to create P-WELL in MAKER";
						}
					}

					ysize = SilComp.getNWellHeight();
					if (ysize > 0)
					{
						double ypos = firstinst.ypos + firstinst.ysize - SilComp.getNWellOffset() -
							(SilComp.getNWellHeight() / 2);
						if (sc_nwellproto != null)
						{
							NodeInst binst = NodeInst.makeInstance(sc_nwellproto, new Point2D.Double(xpos, ypos), xsize, ysize, bcell);
							if (binst == null)
								return "Unable to create N-WELL in MAKER";
						}
					}
				}
			}
		}

		// show the cell
		WindowFrame.createEditWindow(bcell);

		return null;
	}

	/**
	 * Method to create an export at the given instance at the given port.
	 * Note that ports of primitive instances are passed as NULL and must be determined.
	 * @param inst pointer to instance.
	 * @param port pointer to port on the instance.
	 * @param name name of the Export.
	 * @param type type of port (eg. input, output, etc.)
	 * @param bcell cell in which to create.
	 * @return the new Export (null on error).
	 */
	private static Export Sc_create_export_port(NodeInst inst, PortProto port, String name, int type, Cell bcell)
	{
		// check if primative
		if (port == null)
			port = inst.getProto().getPort(0);

		PortInst pi = inst.findPortInstFromProto(port);
		Export xPort = Export.newInstance(bcell, pi, name);
		if (xPort == null) return null;
		switch (type)
		{
			case SilComp.SCINPORT:    xPort.setCharacteristic(PortCharacteristic.IN);     break;
			case SilComp.SCOUTPORT:   xPort.setCharacteristic(PortCharacteristic.OUT);    break;
			case SilComp.SCBIDIRPORT: xPort.setCharacteristic(PortCharacteristic.BIDIR);  break;
			case SilComp.SCPWRPORT:   xPort.setCharacteristic(PortCharacteristic.PWR);    break;
			default:                  xPort.setCharacteristic(PortCharacteristic.GND);    break;
		}
		return xPort;
	}

	/**
	 * Method to create a track between the two given ports on the first routing layer.
	 * Note that ports of primitive instances are passed as NULL and must be determined.
	 * @param insta pointer to first instance.
	 * @param portA pointer to first port.
	 * @param instB pointer to second instance.
	 * @param portB pointer to second port.
	 * @param width width of track.
	 * @param bcell cell in which to create.
	 * @return the created ArcInst.
	 */
	private static ArcInst Sc_create_track_layer1(NodeInst instA, PortProto portA, NodeInst instB, PortProto portB, double width, Cell bcell)
	{
		// copy into internal structures
		if (portA == null) portA = instA.getProto().getPort(0);
		if (portB == null) portB = instB.getProto().getPort(0);

		// find center positions
		PortInst piA = instA.findPortInstFromProto(portA);
		PortInst piB = instB.findPortInstFromProto(portB);
		Poly polyA = piA.getPoly();
		Poly polyB = piA.getPoly();
		double xA = polyA.getCenterX();
		double yA = polyA.getCenterY();
		double xB = polyB.getCenterX();
		double yB = polyB.getCenterY();

		// make sure the arc can connect
		if (!portA.getBasePort().connectsTo(sc_layer1arc))
		{
			// must place a via
			piA = Sc_create_connection(piA, xA, yA, sc_layer1arc);
		}
		if (!portB.getBasePort().connectsTo(sc_layer1arc))
		{
			// must place a via
			piB = Sc_create_connection(piB, xB, yB, sc_layer1arc);
		}

		ArcInst inst = ArcInst.makeInstance(sc_layer1arc, width, piA, piB);
		return inst;
	}

	/**
	 * Method to create a track between the two given ports on the second routing layer.
	 * Note that ports of primitive instances are passed as NULL and must be determined.
	 * @param insta pointer to first instance.
	 * @param portA pointer to first port.
	 * @param instB pointer to second instance.
	 * @param portB pointer to second port.
	 * @param width width of track.
	 * @param bcell cell in which to create.
	 * @return the created ArcInst.
	 */
	private static ArcInst Sc_create_track_layer2(NodeInst instA, PortProto portA, NodeInst instB, PortProto portB, double width, Cell bcell)
	{
		// copy into internal structures
		if (portA == null) portA = instA.getProto().getPort(0);
		if (portB == null) portB = instB.getProto().getPort(0);

		// find center positions
		PortInst piA = instA.findPortInstFromProto(portA);
		PortInst piB = instB.findPortInstFromProto(portB);
		Poly polyA = piA.getPoly();
		Poly polyB = piA.getPoly();
		double xA = polyA.getCenterX();
		double yA = polyA.getCenterY();
		double xB = polyB.getCenterX();
		double yB = polyB.getCenterY();

		// make sure the arc can connect
		if (!portA.getBasePort().connectsTo(sc_layer2arc))
		{
			// must place a via
			piA = Sc_create_connection(piA, xA, yA, sc_layer1arc);
		}
		if (!portB.getBasePort().connectsTo(sc_layer2arc))
		{
			// must place a via
			piB = Sc_create_connection(piB, xB, yB, sc_layer1arc);
		}

		ArcInst inst = ArcInst.makeInstance(sc_layer2arc, width, piA, piB);
		return inst;
	}

	private static PortInst Sc_create_connection(PortInst pi, double x, double y, ArcProto arc)
	{
		// always use the standard via (David Harris)
		if (sc_viaproto == null) return null;
		PrimitiveNode.Function fun = sc_viaproto.getFunction();
		if (fun != PrimitiveNode.Function.CONTACT && fun != PrimitiveNode.Function.CONNECT) return null;

		// make sure that this contact connects to the desired arc
		if (!sc_viaproto.getPort(0).connectsTo(arc)) return null;

		// use this via to make the connection
		NodeInst viaNode = NodeInst.makeInstance(sc_viaproto, new Point2D.Double(x, y),
			sc_viaproto.getDefWidth(), sc_viaproto.getDefHeight(), pi.getNodeInst().getParent());
		if (viaNode == null) return null;
		double wid = arc.getDefaultWidth();
		PortInst newPi = viaNode.getOnlyPortInst();
		ArcInst zeroArc = ArcInst.makeInstance(arc, wid, pi, newPi);
		if (zeroArc == null) return null;
		return newPi;
	}

	/**
	 * Method to locate the appropriate prototypes for circuit generation.
	 */
	private static String Sc_setup_for_maker()
	{
		Technology tech = Technology.getCurrent();
		String layer1 = SilComp.getHorizRoutingArc();
		String layer2 = SilComp.getVertRoutingArc();
		sc_layer1arc = tech.findArcProto(layer1);
		sc_layer2arc = tech.findArcProto(layer2);
		if (sc_layer1arc == null) return "Unable to find Horizontal Arc " + layer1 + " for MAKER";
		if (sc_layer2arc == null) return "Unable to find Vertical Arc " + layer2 + " for MAKER";

		// find the contact between the two layers
		for(Iterator it = tech.getNodes(); it.hasNext(); )
		{
			PrimitiveNode via = (PrimitiveNode)it.next();
			PrimitiveNode.Function fun = via.getFunction();
			if (fun != PrimitiveNode.Function.CONTACT && fun != PrimitiveNode.Function.CONNECT) continue;
			PrimitivePort pp = (PrimitivePort)via.getPort(0);
			if (!pp.connectsTo(sc_layer1arc)) continue;
			if (!pp.connectsTo(sc_layer2arc)) continue;
			sc_viaproto = via;
			break;
		}
		if (sc_viaproto == null) return "Unable to get VIA for MAKER";

		// find the pin nodes on the connecting layers
		sc_layer1proto = ((PrimitiveArc)sc_layer1arc).findPinProto();
		if (sc_layer1proto == null)
			return "Unable to get LAYER1-NODE for MAKER";
		sc_layer2proto = ((PrimitiveArc)sc_layer2arc).findPinProto();
		if (sc_layer2proto == null)
			return "Unable to get LAYER2-NODE for MAKER";

		/*
		 * find the pure-layer node on the P-well layer
		 * if the p-well size is zero don't look for the node
		 * allows technologies without p-wells to be routed (i.e. GaAs)
		 */
		if (SilComp.getPWellHeight() == 0) sc_pwellproto = null; else
		{
			sc_pwellproto = null;
			Layer pWellLay = tech.findLayerFromFunction(Layer.Function.WELLP);
			if (pWellLay != null) sc_pwellproto = pWellLay.getPureLayerNode();
			if (sc_pwellproto == null)
				return "Unable to get LAYER P-WELL for MAKER";
		}
		if (SilComp.getNWellHeight() == 0) sc_nwellproto = null; else
		{
			sc_nwellproto = null;
			Layer nWellLay = tech.findLayerFromFunction(Layer.Function.WELLP);
			if (nWellLay != null) sc_nwellproto = nWellLay.getPureLayerNode();
			if (sc_nwellproto == null)
				return "Unable to get LAYER P-WELL for MAKER";
		}

		return null;
	}

//
//	/***********************************************************************
//	Module:  Sc_free_maker_data
//	------------------------------------------------------------------------
//	Description:
//		Free the memory structures used by the maker.
//	------------------------------------------------------------------------
//	Calling Sequence:  Sc_free_maker_data(data);
//
//	Name		Type			Description
//	----		----			-----------
//	data		*SCMAKERDATA	Pointer to maker data.
//	------------------------------------------------------------------------
//	*/
//
//	void Sc_free_maker_data(SCMAKERDATA *data)
//	{
//		SCMAKERROW			*row, *nextrow;
//		SCMAKERINST			*inst, *nextinst;
//		SCMAKERCHANNEL		*chan, *nextchan;
//		SCMAKERTRACK		*track, *nexttrack;
//		SCMAKERNODE			*node, *nextnode;
//		SCMAKERVIA			*via, *nextvia;
//		SCMAKERPOWER		*power, *nextpower;
//		SCMAKERPOWERPORT	*pport, *nextpport;
//
//		if (data)
//		{
//			for (row = data.rows; row; row = nextrow)
//			{
//				nextrow = row.next;
//				for (inst = row.members; inst; inst = nextinst)
//				{
//					nextinst = inst.next;
//					efree((CHAR *)inst);
//				}
//				efree((CHAR *)row);
//			}
//			for (chan = data.channels; chan; chan = nextchan)
//			{
//				nextchan = chan.next;
//				for (track = chan.tracks; track; track = nexttrack)
//				{
//					nexttrack = track.next;
//					for (node = track.nodes; node; node = nextnode)
//					{
//						nextnode = node.next;
//						for (via = node.vias; via; via = nextvia)
//						{
//							nextvia = via.next;
//							efree((CHAR *)via);
//						}
//						efree((CHAR *)node);
//					}
//					efree((CHAR *)track);
//				}
//				efree((CHAR *)chan);
//			}
//			for (power = data.power; power; power = nextpower)
//			{
//				nextpower = power.next;
//				for (pport = power.ports; pport; pport = nextpport)
//				{
//					nextpport = pport.next;
//					efree((CHAR *)pport);
//				}
//				efree((CHAR *)power);
//			}
//			for (power = data.ground; power; power = nextpower)
//			{
//				nextpower = power.next;
//				for (pport = power.ports; pport; pport = nextpport)
//				{
//					nextpport = pport.next;
//					efree((CHAR *)pport);
//				}
//				efree((CHAR *)power);
//			}
//			efree((CHAR *)data);
//		}
//	}

}
