/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Route.java
 * Silicon compiler tool (QUISC): routing
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

import com.sun.electric.database.geometry.GenMath;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.TextUtils;

/**
 * This is the routing part of the Silicon Compiler tool.
 */
public class Route
{
	/** not verbose default */			private static final boolean DEBUG = false;

	/** seen in processing */			private static final int SCROUTESEEN		= 0x00000001;
	/** unusable in current track */	private static final int SCROUTEUNUSABLE	= 0x00000002;
	/** temporary not use */			private static final int SCROUTETEMPNUSE	= 0x00000004;

	/** fuzzy window for pass th. */	private static final double DEFAULT_FUZZY_WINDOW_LIMIT	= 6400;

	/** global feed through number */	private static int		sc_feednumber;

	public static class SCROUTE
	{
		/** list of channels */				SCROUTECHANNEL	channels;
		/** exported ports */				SCROUTEEXPORT	exports;
		/** route rows */					SCROUTEROW		rows;
	};

	private static class SCROUTEROW
	{
		/** number, 0 = bottom */			int				number;
		/** list of extracted nodes */		SCROUTENODE		nodes;
		/** reference actual row */			Place.SCROWLIST	row;
		/** last in row list */				SCROUTEROW		last;
		/** next in row list */				SCROUTEROW		next;
	};

	private static class SCROUTENODE
	{
		/** extracted node */				SilComp.SCEXTNODE ext_node;
		/** reference row */				SCROUTEROW		row;
		/** first port in row */			SCROUTEPORT		firstport;
		/** last port in row */				SCROUTEPORT		lastport;
		/** same nodes in above rows */		SCROUTENODE		same_next;
		/** same nodes in below rows */		SCROUTENODE		same_last;
		/** nodes in same row */			SCROUTENODE		next;
	};

	public static class SCROUTEPORT
	{
		/** reference place */				Place.SCNBPLACE	place;
		/** particular port */				SilComp.SCNIPORT port;
		/** reference node */				SCROUTENODE		node;
		/** flags for processing */			int				flags;
		/** previous port in list */		SCROUTEPORT		last;
		/** next port in list */			SCROUTEPORT		next;
	};

	public static class SCROUTECHANNEL
	{
		/** number, 0 is bottom */			int				number;
		/** list of nodes */				SCROUTECHNODE	nodes;
		/** list of tracks */				SCROUTETRACK	tracks;
		/** last in channel list */			SCROUTECHANNEL	last;
		/** next in channel list */			SCROUTECHANNEL	next;
	};

	public static class SCROUTECHNODE
	{
		/** extracted node */				SilComp.SCEXTNODE ext_node;
		/** optional net number */			int				number;
		/** first port in row */			SCROUTECHPORT	firstport;
		/** last port in row */				SCROUTECHPORT	lastport;
		/** reference channel */			SCROUTECHANNEL	channel;
		/** flags for processing */			int				flags;
		/** same nodes in above rows */		SCROUTECHNODE	same_next;
		/** same nodes in below rows */		SCROUTECHNODE	same_last;
		/** nodes in same row */			SCROUTECHNODE	next;
	};

	public static class SCROUTECHPORT
	{
		/** reference port */				SCROUTEPORT		port;
		/** reference channel node */		SCROUTECHNODE	node;
		/** x position */					double			xpos;
		/** flags for processing */			int				flags;
		/** previous port in list */		SCROUTECHPORT	last;
		/** next port in list */			SCROUTECHPORT	next;
	};

	private static class SCROUTEVCG
	{
		/** channel node */					SCROUTECHNODE	chnode;
		/** flags for processing */			int				flags;
		/** edges of graph */				SCROUTEVCGEDGE	edges;
	};

	private static class SCROUTEVCGEDGE
	{
		/** to which node */				SCROUTEVCG		node;
		/** next in list */					SCROUTEVCGEDGE	next;
	};

	private static class SCROUTEZRG
	{
		/** number of zone */				int				number;
		/** list of channel nodes */		SCROUTEZRGMEM	chnodes;
		/** last zone */					SCROUTEZRG		last;
		/** next zone */					SCROUTEZRG		next;
	};

	private static class SCROUTEZRGMEM
	{
		/** channel node */					SCROUTECHNODE	chnode;
		/** next in zone */					SCROUTEZRGMEM	next;
	};

	public static class SCROUTETRACK
	{
		/** number of track, 0 = top */		int				number;
		/** track member */					SCROUTETRACKMEM	nodes;
		/** last track in list */			SCROUTETRACK	last;
		/** next track in list */			SCROUTETRACK	next;
	};

	public static class SCROUTETRACKMEM
	{
		/** channel node */					SCROUTECHNODE	node;
		/** next in same track */			SCROUTETRACKMEM	next;
	};

	public static class SCROUTEEXPORT
	{
		/** export port */					SilComp.SCPORT	xport;
		/** channel port */					SCROUTECHPORT	chport;
		/** next export port */				SCROUTEEXPORT	next;
	};

	/**
	 * Method to do routing.  Here is the description:
	 *
	 * Preliminary Channel Assignment:
	 *    o  Squeeze cells together
	 *    o  Below unless required above
	 *    o  Includes stitches and lateral feeds
	 * Feed Through Decision:
	 *    o  Preferred window for path
	 *    o  Include Fuzzy Window
	 * Make Exports:
	 *    o  Path to closest outside edge
	 * Track Routing:
	 *    o  Create Vertical Constraint Graph
	 *    o  Create Zone Representation for channel
	 *    o  Decrease height of VCG and maximize channel use
	 */
	public static String routeCells()
	{
		// check if working in a cell
		if (SilComp.sc_curcell == null) return "No cell selected";

		// check if placement structure exists
		if (SilComp.sc_curcell.placement == null)
			return "No PLACEMENT structure for cell '" + SilComp.sc_curcell.name + "'";

		// create route structure
//		Sc_free_route(sc_curcell.route);
		SCROUTE route = new SCROUTE();
		SilComp.sc_curcell.route = route;
		route.channels = null;
		route.exports = null;
		route.rows = null;

		// first squeeze cell together
		Sc_route_squeeze_cells(SilComp.sc_curcell.placement.rows);

		// create list of rows and their usage of extracted nodes
		SCROUTEROW row_list = Sc_route_create_row_list(SilComp.sc_curcell.placement.rows, SilComp.sc_curcell);
		route.rows = row_list;

		// create Route Channel List
		SCROUTECHANNEL channel_list = Sc_route_create_channel_list(SilComp.sc_curcell.placement.num_rows + 1);
		route.channels = channel_list;

		// Do primary channel assignment
		Sc_route_channel_assign(row_list, channel_list, SilComp.sc_curcell);

		// decide upon any pass through cells required
		Sc_route_create_pass_throughs(channel_list, SilComp.sc_curcell.placement.rows);

		// decide upon export positions
		route.exports = Sc_route_decide_exports(SilComp.sc_curcell);

		// route tracks in each channel
		Sc_route_tracks_in_channels(channel_list, SilComp.sc_curcell);

		return null;
	}

	/**
	 * Method to try to squeeze adjacent cells in a row as close together.
	 * Checks where their active areas start and uses the minimum active distance.
	 * @param rows pointer to start of row list.
	 */
	private static void Sc_route_squeeze_cells(Place.SCROWLIST rows)
	{
		for (Place.SCROWLIST row = rows; row != null; row = row.next)
		{
			for (Place.SCNBPLACE place = row.start; place != null; place = place.next)
			{
				if (place.next == null) continue;

				// determine allowable overlap
				SilComp.SCCELLNUMS cell1_nums = SilComp.Sc_leaf_cell_get_nums((Cell)place.cell.np);
				SilComp.SCCELLNUMS cell2_nums = SilComp.Sc_leaf_cell_get_nums((Cell)place.next.cell.np);
				double overlap = 0;
				if ((row.row_num % 2) != 0)
				{
					// odd row, cell are transposed
					overlap = cell2_nums.right_active + cell1_nums.left_active
						- SilComp.getMinActiveDistance();
				} else
				{
					// even row
					overlap = cell1_nums.right_active + cell2_nums.left_active
						- SilComp.getMinActiveDistance();
				}

				// move rest of row
				for (Place.SCNBPLACE place2 = place.next; place2 != null; place2 = place2.next)
					place2.xpos -= overlap;
			}
		}
	}

	/**
	 * Method to create list of which extracted nodes each member of the rows of
	 * a placement need connection to.
	 * @param rows pointer to start of placement rows.
	 * @param cell pointer to parent cell.
	 * @return created list.
	 */
	private static SCROUTEROW Sc_route_create_row_list(Place.SCROWLIST rows, SilComp.SCCELL cell)
	{
		// clear all reference pointers in extracted node list
		for (SilComp.SCEXTNODE enode = cell.ex_nodes; enode != null; enode = enode.next)
			enode.ptr = null;

		// create a route row list for each placement row
		SCROUTEROW first_rrow = null, last_rrow = null;
		SCROUTENODE same_node = null;
		for (Place.SCROWLIST row = rows; row != null; row = row.next)
		{
			SCROUTEROW new_rrow = new SCROUTEROW();
			new_rrow.number = row.row_num;
			new_rrow.nodes = null;
			new_rrow.row = row;
			new_rrow.last = last_rrow;
			new_rrow.next = null;
			if (last_rrow != null)
			{
				last_rrow.next = new_rrow;
				last_rrow = new_rrow;
			} else
			{
				first_rrow = last_rrow = new_rrow;
			}

			// create an entry of every extracted node in each row
			SCROUTENODE last_node = null;
			for (SilComp.SCEXTNODE enode = cell.ex_nodes; enode != null; enode = enode.next)
			{
				SCROUTENODE new_node = new SCROUTENODE();
				new_node.ext_node = enode;
				new_node.row = new_rrow;
				new_node.firstport = null;
				new_node.lastport = null;
				new_node.same_next = null;
				new_node.same_last = same_node;
				new_node.next = null;
				if (last_node != null)
				{
					last_node.next = new_node;
				} else
				{
					new_rrow.nodes = new_node;
				}
				last_node = new_node;
				if (same_node != null)
				{
					same_node.same_next = new_node;
					same_node = same_node.next;
				} else
				{
					enode.ptr = new_node;
				}
			}
			same_node = new_rrow.nodes;

			// set reference to all ports on row
			for (Place.SCNBPLACE place = row.start; place != null; place = place.next)
			{
				for (SilComp.SCNIPORT port = place.cell.ports; port != null; port = port.next)
				{
					SCROUTENODE new_node = (SCROUTENODE)port.ext_node.ptr;
					if (new_node != null)
					{
						for (int i = 0; i < row.row_num; i++)
							new_node = new_node.same_next;
						SCROUTEPORT new_port = new SCROUTEPORT();
						new_port.place = place;
						new_port.port = port;
						new_port.node = new_node;
						new_port.next = null;
						new_port.last = new_node.lastport;
						if (new_node.lastport != null)
						{
							new_node.lastport.next = new_port;
						} else
						{
							new_node.firstport = new_port;
						}
						new_node.lastport = new_port;
					}
				}
			}
		}

		return first_rrow;
	}

	/**
	 * Method to create the basic channel list.
	 * The number of channels is one more than the number of rows.
	 * @param number number of channels to create.
	 * @return result list.
	 */
	private static SCROUTECHANNEL Sc_route_create_channel_list(int number)
	{
		// create channel list
		SCROUTECHANNEL first_chan = null, last_chan = null;
		for (int i = 0; i < number; i++)
		{
			SCROUTECHANNEL new_chan = new SCROUTECHANNEL();
			new_chan.number = i;
			new_chan.nodes = null;
			new_chan.tracks = null;
			new_chan.next = null;
			new_chan.last = last_chan;
			if (last_chan != null)
			{
				last_chan.next = new_chan;
			} else
			{
				first_chan = new_chan;
			}
			last_chan = new_chan;
		}

		return first_chan;
	}

	/**
	 * Method to do primary channel assignment for all ports.
	 * The basis algorithm is:
	 *
	 *		if no ports higher
	 *			use below channel
	 *		else
	 *			if ports lower
	 *				use channel with closest to other ports
	 *			else
	 *				use above channel
	 * @param rows list of rows of ports.
	 * @param channels list of channels.
	 * @param cell pointer to parent cell.
	 */
	private static void Sc_route_channel_assign(SCROUTEROW rows, SCROUTECHANNEL channels, SilComp.SCCELL cell)
	{
		// clear flags
		for (SCROUTEROW row = rows; row != null; row = row.next)
		{
			for (SCROUTENODE node = row.nodes; node != null; node = node.next)
			{
				for (SCROUTEPORT port = node.firstport; port != null; port = port.next)
					port.flags &= ~SCROUTESEEN;
			}
		}

		for (SCROUTEROW row = rows; row != null; row = row.next)
		{
			for (SCROUTENODE node = row.nodes; node != null; node = node.next)
			{
				if (node.firstport == null)
					continue;

				// check for ports above
				boolean ports_above = false;
				for (SCROUTENODE node2 = node.same_next; node2 != null; node2 = node2.same_next)
				{
					if (node2.firstport != null)
					{
						ports_above = true;
						break;
					}
				}

				// if none found above, any ports in this list only going up
				if (!ports_above && node.firstport != node.lastport)
				{
					for (SCROUTEPORT port = node.firstport; port != null; port = port.next)
					{
						int direct = SilComp.Sc_leaf_port_direction((PortProto)port.port.port);
						if ((direct & SilComp.SCPORTDIRUP) != 0 && (direct & SilComp.SCPORTDIRDOWN) == 0)
						{
							ports_above = true;
							break;
						}
					}
				}

				// check for ports below
				boolean ports_below = false;
				for (SCROUTENODE node2 = node.same_last; node2 != null; node2 = node2.same_last)
				{
					if (node2.firstport != null)
					{
						ports_below = true;
						break;
					}
				}

				// if none found below, any ports in this row only going down
				if (!ports_below && node.firstport != node.lastport)
				{
					for (SCROUTEPORT port = node.firstport; port != null; port = port.next)
					{
						int direct = SilComp.Sc_leaf_port_direction((PortProto)port.port.port);
						if ((direct & SilComp.SCPORTDIRDOWN) != 0 && (direct & SilComp.SCPORTDIRUP) == 0)
						{
							ports_below = true;
							break;
						}
					}
				}

				// do not add if only one port unless an export
				if (!ports_above && !ports_below)
				{
					if (node.firstport == node.lastport)
					{
						SilComp.SCPORT xport;
						for (xport = cell.ports; xport != null; xport = xport.next)
						{
							if (xport.node.ports.ext_node == node.ext_node)
								break;
						}
						if (xport == null)
							continue;

						// if top row, put in above channel
						if (row.number != 0 && row.next == null)
							ports_above = true;
					}
				}

				// assign ports to channel
				for (SCROUTEPORT port = node.firstport; port != null; port = port.next)
				{
					if ((port.flags & SCROUTESEEN) != 0) continue;

					// check how ports can be connected to
					int direct = SilComp.Sc_leaf_port_direction((PortProto)port.port.port);

					// for ports both up and down
					if ((direct & SilComp.SCPORTDIRUP) != 0 && (direct & SilComp.SCPORTDIRDOWN) != 0)
					{
						if (!ports_above)
						{
							// add to channel below
							Sc_route_add_port_to_channel(port, node.ext_node, channels, row.number);
						} else
						{
							if (ports_below)
							{
								// add to channel where closest
								int offset = 0;
								if (Sc_route_nearest_port(port, node, row.number, cell) > 0)
								{
									offset = 1;
								}
								Sc_route_add_port_to_channel(port, node.ext_node, channels, row.number + offset);
							} else
							{
								// add to channel above
								Sc_route_add_port_to_channel(port, node.ext_node, channels, row.number + 1);
							}
						}
						port.flags |= SCROUTESEEN;
					}

					// for ports only up
					else if ((direct & SilComp.SCPORTDIRUP) != 0)
					{
						// add to channel above
						Sc_route_add_port_to_channel(port, node.ext_node, channels, row.number + 1);
						port.flags |= SCROUTESEEN;
					}

					// for ports only down
					else if ((direct & SilComp.SCPORTDIRDOWN) != 0)
					{
						// add to channel below
						Sc_route_add_port_to_channel(port, node.ext_node, channels, row.number);
						port.flags |= SCROUTESEEN;
					}

					// ports left
					else if ((direct & SilComp.SCPORTDIRLEFT) != 0)
					{
						Sc_route_add_lateral_feed(port, channels, ports_above, ports_below, cell);
					}

					// ports right
					else if ((direct & SilComp.SCPORTDIRRIGHT) != 0)
					{
						Sc_route_add_lateral_feed(port, channels, ports_above, ports_below, cell);
					} else
					{
						System.out.println("ERROR - no direction for " + port.place.cell.name + " port " +
							((PortProto)port.port.port).getName());
						port.flags |= SCROUTESEEN;
					}
				}
			}
		}
	}

	/**
	 * Method to return the offset to the row which has the closest port to the indicated port.
	 * The offset is +1 for every row above, -1 for every row below.
	 * @param port pointer to current port.
	 * @param node pointer to reference node.
	 * @param row_num row number of port.
	 * @param cell pointer to parent cell.
	 * @return offset of row of closest port.
	 */
	private static double Sc_route_nearest_port(SCROUTEPORT port, SCROUTENODE node, int row_num, SilComp.SCCELL cell)
	{
		double min_dist = Double.MAX_VALUE;
		double which_row = 0;
		double xpos1;
		if ((row_num % 2) != 0)
		{
			xpos1 = port.place.xpos + port.place.cell.size -
				port.port.xpos;
		} else
		{
			xpos1 = port.place.xpos + port.port.xpos;
		}

		// find closest above
		double offset = 0;
		for (SCROUTENODE nnode = node.same_next; nnode != null; nnode = nnode.same_next)
		{
			offset++;
			for (SCROUTEPORT nport = nnode.firstport; nport != null; nport = nport.next)
			{
				double dist = Math.abs(offset) * cell.placement.avg_height * 2;
				double xpos2;
				if (((row_num + offset) % 2) != 0)
				{
					xpos2 = nport.place.xpos + nport.place.cell.size -
						nport.port.xpos;
				} else
				{
					xpos2 = nport.place.xpos + nport.port.xpos;
				}
				dist += Math.abs(xpos2 - xpos1);
				if (dist < min_dist)
				{
					min_dist = dist;
					which_row = offset;
				}
			}
		}

		// check below
		offset = 0;
		for (SCROUTENODE nnode = node.same_last; nnode != null; nnode = nnode.same_last)
		{
			offset--;
			for (SCROUTEPORT nport = nnode.firstport; nport != null; nport = nport.next)
			{
				double dist = Math.abs(offset) * cell.placement.avg_height * 2;
				double xpos2;
				if (((row_num + offset) % 2) != 0)
				{
					xpos2 = nport.place.xpos + nport.place.cell.size -
						nport.port.xpos;
				} else
				{
					xpos2 = nport.place.xpos + nport.port.xpos;
				}
				dist += Math.abs(xpos2 - xpos1);
				if (dist < min_dist)
				{
					min_dist = dist;
					which_row = offset;
				}
			}
		}

		return which_row;
	}

	/**
	 * Method to add the indicated port to the indicated channel.
	 * Create node for that channel if it doesn't already exist.
	 * @param port pointer to route port.
	 * @param ext_node value of reference extracted node.
	 * @param channels start of channel list.
	 * @param chan_num number of wanted channel.
	 */
	private static void Sc_route_add_port_to_channel(SCROUTEPORT port, SilComp.SCEXTNODE ext_node,
		SCROUTECHANNEL channels, int chan_num)
	{
		// get correct channel
		SCROUTECHANNEL channel = channels;
		for (int i = 0; i < chan_num; i++)
			channel = channel.next;

		// check if node already exists for this channel
		SCROUTECHNODE node;
		for (node = channel.nodes; node != null; node = node.next)
		{
			if (node.ext_node == ext_node) break;
		}
		if (node == null)
		{
			node = new SCROUTECHNODE();
			node.ext_node = ext_node;
			node.number = 0;
			node.firstport = null;
			node.lastport = null;
			node.channel = channel;
			node.flags = SCROUTESEEN;
			node.same_next = null;
			node.same_last = null;
			node.next = channel.nodes;
			channel.nodes = node;

			// resolve any references to other channels
			// check previous channels
			for (SCROUTECHANNEL nchan = channel.last; nchan != null; nchan = nchan.last)
			{
				SCROUTECHNODE nnode;
				for (nnode = nchan.nodes; nnode != null; nnode = nnode.next)
				{
					if (nnode.ext_node == ext_node)
					{
						nnode.same_next = node;
						node.same_last = nnode;
						break;
					}
				}
				if (nnode != null) break;
			}

			// check later channels
			for (SCROUTECHANNEL nchan = channel.next; nchan != null; nchan = nchan.next)
			{
				SCROUTECHNODE nnode;
				for (nnode = nchan.nodes; nnode != null; nnode = nnode.next)
				{
					if (nnode.ext_node == ext_node)
					{
						nnode.same_last = node;
						node.same_next = nnode;
						break;
					}
				}
				if (nnode != null) break;
			}
		}

		// add port to node
		SCROUTECHPORT nport = new SCROUTECHPORT();
		nport.port = port;
		nport.node = node;
		nport.xpos = 0;
		nport.flags = 0;
		nport.next = null;
		nport.last = node.lastport;
		if (node.lastport != null)
		{
			node.lastport.next = nport;
		} else
		{
			node.firstport = nport;
		}
		node.lastport = nport;
	}

	/**
	 * Method to add a lateral feed for the port indicated.
	 * Add a "stitch" if port of same type adjecent, else add full lateral feed.
	 * Add to appropriate channel(s) if full feed.
	 * @param port pointer to port in question.
	 * @param channels list of channels.
	 * @param ports_above true if ports above.
	 * @param ports_below true if ports below.
	 * @param cell pointer to parent cell.
	 */
	private static void Sc_route_add_lateral_feed(SCROUTEPORT port, SCROUTECHANNEL channels,
		boolean ports_above, boolean ports_below, SilComp.SCCELL cell)
	{
		int direct = SilComp.Sc_leaf_port_direction((PortProto)port.port.port);

		// determine if stitch
		Place.SCNBPLACE nplace = null;
		int sdirect = 0;
		if ((direct & SilComp.SCPORTDIRLEFT) != 0)
		{
			if ((port.node.row.number % 2) != 0)
			{
				// odd row
				for (nplace = port.place.next; nplace != null; nplace = nplace.next)
				{
					if (nplace.cell.type == SilComp.SCLEAFCELL)
						break;
				}
			} else
			{
				// even row
				for (nplace = port.place.last; nplace != null; nplace = nplace.last)
				{
					if (nplace.cell.type == SilComp.SCLEAFCELL)
						break;
				}
			}
			sdirect = SilComp.SCPORTDIRRIGHT;
		} else
		{
			if ((port.node.row.number % 2) != 0)
			{
				// odd row
				for (nplace = port.place.last; nplace != null; nplace = nplace.last)
				{
					if (nplace.cell.type == SilComp.SCLEAFCELL) break;
				}
			} else
			{
				// even row
				for (nplace = port.place.next; nplace != null; nplace = nplace.next)
				{
					if (nplace.cell.type == SilComp.SCLEAFCELL)
						break;
				}
			}
			sdirect =SilComp. SCPORTDIRLEFT;
		}
		if (nplace != null)
		{
			// search for same port with correct direction
			SCROUTEPORT port2;
			for (port2 = port.next; port2 != null; port2 = port2.next)
			{
				if (port2.place == nplace &&
					SilComp.Sc_leaf_port_direction((PortProto)port2.port.port) == sdirect)
						break;
			}
			if (port2 != null)
			{
				// stitch feed
				port.flags |= SCROUTESEEN;
				port2.flags |= SCROUTESEEN;
				Place.SCNBPLACE splace = new Place.SCNBPLACE();
				splace.cell = null;
				SilComp.SCNITREE sinst = SilComp.Sc_new_instance("Stitch", SilComp.SCSTITCH);
				splace.cell = sinst;

				// save two ports
				SilComp.SCNIPORT sport = SilComp.Sc_new_instance_port(sinst);
				sport.port = port;
				SilComp.SCNIPORT sport2 = SilComp.Sc_new_instance_port(sinst);
				sport2.port = port2;

				// insert in place
				if ((direct & SilComp.SCPORTDIRLEFT) != 0)
				{
					if ((port.node.row.number % 2) != 0)
					{
						splace.last = port.place;
						splace.next = port.place.next;
						if (splace.last != null) splace.last.next = splace;
						if (splace.next != null) splace.next.last = splace;
					} else
					{
						splace.last = port.place.last;
						splace.next = port.place;
						if (splace.last != null) splace.last.next = splace;
						if (splace.next != null) splace.next.last = splace;
					}
				} else
				{
					if ((port.node.row.number % 2) != 0)
					{
						splace.last = port.place.last;
						splace.next = port.place;
						if (splace.last != null) splace.last.next = splace;
						if (splace.next != null) splace.next.last = splace;
					} else
					{
						splace.last = port.place;
						splace.next = port.place.next;
						if (splace.last != null) splace.last.next = splace;
						if (splace.next != null) splace.next.last = splace;
					}
				}
				return;
			}
		}

		// full lateral feed
		port.flags |= SCROUTESEEN;
		Place.SCNBPLACE splace = new Place.SCNBPLACE();
		splace.cell = null;
		SilComp.SCNITREE sinst = SilComp.Sc_new_instance("Lateral Feed", SilComp.SCLATERALFEED);
		sinst.size = SilComp.getFeedThruSize();
		splace.cell = sinst;

		// save port
		SilComp.SCNIPORT sport = SilComp.Sc_new_instance_port(sinst);
		sport.xpos = SilComp.getFeedThruSize() / 2;

		// create new route port
		SCROUTEPORT nport = new SCROUTEPORT();
		nport.place = port.place;
		nport.port = port.port;
		nport.node = port.node;
		nport.flags = 0;
		nport.last = null;
		nport.next = null;
		sport.port = nport;

		// insert in place
		if ((direct & SilComp.SCPORTDIRLEFT) != 0)
		{
			if ((port.node.row.number % 2) != 0)
			{
				splace.last = port.place;
				splace.next = port.place.next;
			} else
			{
				splace.last = port.place.last;
				splace.next = port.place;
			}
		} else
		{
			if ((port.node.row.number % 2) != 0)
			{
				splace.last = port.place.last;
				splace.next = port.place;
			} else
			{
				splace.last = port.place;
				splace.next = port.place.next;
			}
		}
		if (splace.last != null)
		{
			splace.last.next = splace;
		} else
		{
			port.node.row.row.start = splace;
		}
		if (splace.next != null)
		{
			splace.next.last = splace;
		} else
		{
			port.node.row.row.end = splace;
		}
		Sc_route_resolve_new_xpos(splace, port.node.row.row);

		// change route port to lateral feed
		port.place = splace;
		port.port = sport;

		// channel assignment of lateral feed
		if (!ports_above)
		{
			// add to channel below
			Sc_route_add_port_to_channel(port, port.node.ext_node, channels, port.node.row.number);
		} else
		{
			if (ports_below)
			{
				// add to channel where closest
				int offset = 0;
				if (Sc_route_nearest_port(port, port.node, port.node.row.number, cell) > 0)
				{
					offset = 1;
				}
				Sc_route_add_port_to_channel(port, port.node.ext_node, channels, port.node.row.number + offset);
			} else
			{
				// add to channel above
				Sc_route_add_port_to_channel(port, port.node.ext_node, channels, port.node.row.number + 1);
			}
		}
	}

	/**
	 * Method to create pass throughs required to join electrically equivalent nodes
	 * in different channels.
	 * @param channels pointer to current channels.
	 * @param rows pointer to placed rows.
	 */
	private static void Sc_route_create_pass_throughs(SCROUTECHANNEL channels, Place.SCROWLIST rows)
	{
		sc_feednumber = 0;

		// clear the flag on all channel nodes
		for (SCROUTECHANNEL chan = channels; chan != null; chan = chan.next)
		{
			for (SCROUTECHNODE chnode = chan.nodes; chnode != null; chnode = chnode.next)
				chnode.flags &= ~SCROUTESEEN;
		}

		// find all nodes which exist in more than one channel
		for (SCROUTECHANNEL chan = channels; chan != null; chan = chan.next)
		{
			for (SCROUTECHNODE chnode = chan.nodes; chnode != null; chnode = chnode.next)
			{
				if ((chnode.flags & SCROUTESEEN) != 0) continue;
				chnode.flags |= SCROUTESEEN;
				SCROUTECHNODE old_chnode = chnode;
				for (SCROUTECHNODE chnode2 = chnode.same_next; chnode2 != null;
					chnode2 = chnode2.same_next)
				{
					chnode2.flags |= SCROUTESEEN;
					Sc_route_between_ch_nodes(old_chnode, chnode2, channels, rows);
					old_chnode = chnode2;
				}
			}
		}
	}

	/**
	 * Method to route between two channel nodes.
	 * Consider both the use of pass throughs and the use of nodes of the same extracted node in a row.
	 * Note that there may be more than one row between the two channels.
	 * @param node1 first node (below).
	 * @param node2 second node (above).
	 * @param channels list of channels.
	 * @param rows list of placed rows.
	 */
	private static void Sc_route_between_ch_nodes(SCROUTECHNODE node1, SCROUTECHNODE node2,
		SCROUTECHANNEL channels, Place.SCROWLIST rows)
	{
		SilComp.SCEXTNODE ext_node = node1.ext_node;

		// determine limits of second channel
		double minx2 = Sc_route_min_port_pos(node2);
		double maxx2 = Sc_route_max_port_pos(node2);

		// do for all intervening channels
		for (SCROUTECHANNEL chan = node1.channel; chan != node2.channel; chan = chan.next,
			node1 = node1.same_next)
		{
			// determine limits of first channel node
			double minx1 = Sc_route_min_port_pos(node1);
			double maxx1 = Sc_route_max_port_pos(node1);

			// determine preferred region of pass through
			double pminx, pmaxx;
			if (maxx1 <= minx2)
			{
				// no overlap with first node to left
				pminx = maxx1;
				pmaxx = minx2;
			} else if (maxx2 <= minx1)
			{
				// no overlap with first node to right
				pminx = maxx2;
				pmaxx = minx1;
			} else
			{
				// have some overlap
				pminx = Math.max(minx1, minx2);
				pmaxx = Math.min(minx1, minx2);
			}

			// set window fuzzy limits
			pminx -= DEFAULT_FUZZY_WINDOW_LIMIT;
			pmaxx += DEFAULT_FUZZY_WINDOW_LIMIT;

			// determine which row we are in
			Place.SCROWLIST row;
			for (row = rows; row != null; row = row.next)
			{
				if (row.row_num == chan.number) break;
			}

			// check for any possible ports which can be used
			SCROUTENODE rnode;
			for (rnode = (SCROUTENODE)ext_node.ptr; rnode != null; rnode = rnode.same_next)
			{
				if (rnode.row.number == row.row_num) break;
			}
			if (rnode != null)
			{
				// port of correct type exists somewhere in this row
				SCROUTEPORT rport;
				for (rport = rnode.firstport; rport != null; rport = rport.next)
				{
					double pos = Sc_route_port_position(rport);
					int direct = SilComp.Sc_leaf_port_direction((PortProto)rport.port.port);
					if ((direct & SilComp.SCPORTDIRUP) == 0 && (direct & SilComp.SCPORTDIRDOWN) != 0) continue;
					if (pos >= pminx && pos <= pmaxx) break;
				}
				if (rport != null)
				{
					// found suitable port, ensure it exists in both channels
					SCROUTECHPORT chport = null;
					for (SCROUTECHNODE node = chan.nodes; node != null; node = node.next)
					{
						if (node.ext_node == node1.ext_node)
						{
							for (chport = node.firstport; chport != null;
								chport = chport.next)
							{
								if (chport.port == rport) break;
							}
						}
					}
					if (chport == null)
					{
						// add port to this channel
						Sc_route_add_port_to_channel(rport, ext_node, channels, chan.number);
					}
					chport = null;
					for (SCROUTECHNODE node = chan.next.nodes; node != null; node = node.next)
					{
						if (node.ext_node == node1.ext_node)
						{
							for (chport = node.firstport; chport != null;
								chport = chport.next)
							{
								if (chport.port == rport)
									break;
							}
						}
					}
					if (chport == null)
					{
						// add port to next channel
						Sc_route_add_port_to_channel(rport, ext_node, channels, chan.next.number);
					}
					continue;
				}
			}

			// if no port found, find best position for feed through
			double bestpos = Double.MAX_VALUE;
			Place.SCNBPLACE bestplace = null;
			for (Place.SCNBPLACE place = row.start; place != null; place = place.next)
			{
				// not allowed to feed at stitch
				if (place.cell.type == SilComp.SCSTITCH ||
					(place.last != null && place.last.cell.type == SilComp.SCSTITCH))
						continue;

				// not allowed to feed at lateral feed
				if (place.cell.type == SilComp.SCLATERALFEED ||
					(place.last != null && place.last.cell.type == SilComp.SCLATERALFEED))
						continue;
				if (place.xpos >= pminx && place.xpos <= pmaxx)
				{
					bestplace = place;
					break;
				}
				double pos;
				if (place.xpos < pminx)
				{
					pos = Math.abs(pminx - place.xpos);
				} else
				{
					pos = Math.abs(pmaxx - place.xpos);
				}
				if (pos < bestpos)
				{
					bestpos = pos;
					bestplace = place;
				}
			}

			// insert feed through at the indicated place
			Sc_route_insert_feed_through(bestplace, row, channels, chan.number, node1);
		}
	}

	/**
	 * Method to return the position of the port which is farthest left (minimum).
	 * @param node pointer to channel node.
	 * @return leftmost port position.
	 */
	private static double Sc_route_min_port_pos(SCROUTECHNODE node)
	{
		double minx = Double.MAX_VALUE;
		for (SCROUTECHPORT chport = node.firstport; chport != null; chport = chport.next)
		{
			// determine position
			double pos = Sc_route_port_position(chport.port);

			// check versus minimum
			if (pos < minx)
				minx = pos;
		}

		return minx;
	}

	/**
	 * Method to return the position of the port which is farthest right (maximum).
	 * @param node pointer to channel node.
	 * @return rightmost port position.
	 */
	private static double Sc_route_max_port_pos(SCROUTECHNODE node)
	{
		double maxx = Double.MIN_VALUE;
		for (SCROUTECHPORT chport = node.firstport; chport != null; chport = chport.next)
		{
			// determine position
			double pos = Sc_route_port_position(chport.port);

			// check versus maximum
			if (pos > maxx) maxx = pos;
		}

		return maxx;
	}

	/**
	 * Method to return the x position of the indicated port.
	 * @param port pointer to port in question.
	 * @return x position.
	 */
	private static double Sc_route_port_position(SCROUTEPORT port)
	{
		double pos = port.place.xpos;
		if ((port.node.row.number % 2) != 0)
		{
			pos += port.place.cell.size - port.port.xpos;
		} else
		{
			pos += port.port.xpos;
		}

		return pos;
	}

	/**
	 * Method to insert a feed through in front of the indicated place.
	 * @param place place where to insert in front of.
	 * @param row row of place.
	 * @param channels channel list.
	 * @param chan_num number of particular channel below.
	 * @param node channel node within the channel.
	 */
	private static void Sc_route_insert_feed_through(Place.SCNBPLACE place, Place.SCROWLIST row,
		SCROUTECHANNEL channels, int chan_num, SCROUTECHNODE node)
	{
		// create a special instance
		SilComp.SCNITREE inst = new SilComp.SCNITREE();
		inst.name = "Feed_Through";
		inst.number = 0;
		inst.type = SilComp.SCFEEDCELL;
		inst.np = null;
		inst.size = SilComp.getFeedThruSize();
		inst.connect = null;
		inst.ports = null;
		inst.flags = 0;
		inst.tp = null;

		// create instance port
		SilComp.SCNIPORT port = new SilComp.SCNIPORT();
		port.port = new Integer(sc_feednumber++);
		port.ext_node = node.ext_node;
		port.bits = 0;
		port.xpos = SilComp.getFeedThruSize() / 2;
		port.next = null;
		inst.ports = port;

		// create the appropriate place
		Place.SCNBPLACE nplace = new Place.SCNBPLACE();
		nplace.cell = inst;
		nplace.last = place.last;
		nplace.next = place;
		if (nplace.last != null)
			nplace.last.next = nplace;
		place.last = nplace;
		if (place == row.start)
			row.start = nplace;

		Sc_route_resolve_new_xpos(nplace, row);

		// create a route port entry for this new port
		SCROUTENODE rnode;
		for (rnode = (SCROUTENODE)node.ext_node.ptr; rnode != null; rnode = rnode.same_next)
		{
			if (rnode.row.number == row.row_num) break;
		}
		SCROUTEPORT rport = new SCROUTEPORT();
		rport.place = nplace;
		rport.port = port;
		rport.node = rnode;
		rport.flags = 0;
		rport.next = null;
		rport.last = rnode.lastport;
		if (rnode.lastport != null)
		{
			rnode.lastport.next = rport;
		} else
		{
			rnode.firstport = rport;
		}
		rnode.lastport = rport;

		// add to channels
		Sc_route_add_port_to_channel(rport, node.ext_node, channels, chan_num);
		Sc_route_add_port_to_channel(rport, node.ext_node, channels, chan_num + 1);
	}

	/**
	 * Method to resolve the position of the new place and update the row.
	 * @param place new place.
	 * @param row pointer to existing row.
	 */
	private static void Sc_route_resolve_new_xpos(Place.SCNBPLACE place, Place.SCROWLIST row)
	{
		double xpos;
		if (place.last != null)
		{
			if (place.last.cell.type == SilComp.SCLEAFCELL)
			{
				SilComp.SCCELLNUMS cnums = SilComp.Sc_leaf_cell_get_nums((Cell)place.last.cell.np);
				xpos = place.last.xpos + place.last.cell.size;
				double overlap = 0;
				if ((row.row_num % 2) != 0)
				{
					// odd row, cells are transposed
					overlap = cnums.left_active - SilComp.getMinActiveDistance();
				} else
				{
					// even row
					overlap = cnums.right_active - SilComp.getMinActiveDistance();
				}
				if (overlap < 0 && place.cell.type != SilComp.SCLATERALFEED)
					overlap = 0;
				xpos -= overlap;
				place.xpos = xpos;
				xpos += place.cell.size;
			} else
			{
				xpos = place.last.xpos + place.last.cell.size;
				place.xpos = xpos;
				xpos += place.cell.size;
			}
		} else
		{
			place.xpos = 0;
			xpos = place.cell.size;
		}

		if (place.next != null)
		{
			double oldxpos = place.next.xpos;
			double nxpos = 0;
			if (place.next.cell.type == SilComp.SCLEAFCELL)
			{
				SilComp.SCCELLNUMS cnums = SilComp.Sc_leaf_cell_get_nums((Cell)place.next.cell.np);
				double overlap = 0;
				if ((row.row_num % 2) != 0)
				{
					// odd row, cells are transposed
					overlap = cnums.right_active - SilComp.getMinActiveDistance();
				} else
				{
					// even row
					overlap = cnums.left_active - SilComp.getMinActiveDistance();
				}
				if (overlap < 0 && place.cell.type != SilComp.SCLATERALFEED)
					overlap = 0;
				nxpos = xpos - overlap;
			} else
			{
				nxpos = xpos;
			}

			// update rest of the row
			for (place = place.next; place != null; place = place.next)
				place.xpos += nxpos - oldxpos;
			row.row_size += nxpos - oldxpos;
		}
	}

	/**
	 * Method to decide upon the exports positions.
	 * If port is available on either the top or bottom channel, no action is required.
	 * If however the port is not available, add special place to the beginning or
	 * end of a row to allow routing to left or right edge of cell (whichever is shorter).
	 * @param cell pointer to cell.
	 * @return created data.
	 */
	private static SCROUTEEXPORT Sc_route_decide_exports(SilComp.SCCELL cell)
	{
		SCROUTEEXPORT lexport = null;

		// check all exports
		for (SilComp.SCPORT port = cell.ports; port != null; port = port.next)
		{
			// get extracted node
			SilComp.SCEXTNODE enode = port.node.ports.ext_node;

			SCROUTECHNODE chnode = null;
			for (SCROUTECHANNEL chan = cell.route.channels; chan != null; chan = chan.next)
			{
				for (chnode = chan.nodes; chnode != null; chnode = chnode.next)
				{
					if (chnode.ext_node == enode) break;
				}
				if (chnode != null) break;
			}

			// find limits of channel node
			boolean bottom = false, top = false, left = false, right = false;
			double best_dist = Double.MAX_VALUE;
			SCROUTECHPORT best_chport = null;
			for (SCROUTECHNODE chnode2 = chnode; chnode2 != null; chnode2 = chnode2.same_next)
			{
				SCROUTECHPORT chport;
				for (chport = chnode2.firstport; chport != null; chport = chport.next)
				{
					// check for bottom channel
					if (chport.node.channel.number == 0)
					{
						bottom = true;
						best_chport = chport;
						break;
					}

					// check for top channel
					if (chport.node.channel.number == cell.placement.num_rows)
					{
						top = true;
						best_chport = chport;
						break;
					}

					// check distance to left boundary
					double dist = Sc_route_port_position(chport.port);
					if (dist < best_dist)
					{
						best_dist = dist;
						left = true;
						right = false;
						best_chport = chport;
					}

					// check distance to right boundary
					double maxx = chport.port.node.row.row.end.xpos +
						chport.port.node.row.row.end.cell.size;
					dist = maxx - Sc_route_port_position(chport.port);
					if (dist < best_dist)
					{
						best_dist = dist;
						right = true;
						left = false;
						best_chport = chport;
					}
				}
				if (chport != null) break;
			}
			if (top)
			{
				// EMPTY
			} else if (bottom)
			{
				// EMPTY
			} else if (right)
			{
				// create special place for export at end of row
				best_chport = Sc_route_create_special(port.node, best_chport, false, cell);
			} else if (left)
			{
				// create special place for export at start of row
				best_chport = Sc_route_create_special(port.node, best_chport, true, cell);
			}

			// add port to export list
			SCROUTEEXPORT nexport = new SCROUTEEXPORT();
			nexport.xport = port;
			nexport.chport = best_chport;
			nexport.next = lexport;
			lexport = nexport;
		}

		return lexport;
	}

	/**
	 * Method to create a special place on either the start or end of the row where
	 * the passed channel port real port resides.
	 * @param inst instance for place to point to.
	 * @param chport channel port in question.
	 * @param w true at start, false at end.
	 * @param cell parent cell.
	 * @return newly created channel port.
	 */
	private static SCROUTECHPORT Sc_route_create_special(SilComp.SCNITREE inst, SCROUTECHPORT chport, boolean where, SilComp.SCCELL cell)
	{
		inst.size = SilComp.getFeedThruSize();
		inst.ports.xpos = SilComp.getFeedThruSize() / 2;

		// find row
		Place.SCROWLIST row = chport.port.node.row.row;

		// create appropriate place
		Place.SCNBPLACE nplace = new Place.SCNBPLACE();
		nplace.cell = inst;
		if (where)
		{
			if (row.start != null)
			{
				double xpos = row.start.xpos - SilComp.getFeedThruSize();
				nplace.xpos = xpos;
				row.start.last = nplace;
			} else
			{
				nplace.xpos = 0;
			}
			nplace.last = null;
			nplace.next = row.start;
			row.start = nplace;
		} else
		{
			if (row.end != null)
			{
				nplace.xpos = row.end.xpos + row.end.cell.size;
				row.end.next = nplace;
			} else
			{
				nplace.xpos = 0;
			}
			nplace.next = null;
			nplace.last = row.end;
			row.end = nplace;
		}

		// create a route port entry for this new port
		SCROUTENODE rnode;
		for (rnode = (SCROUTENODE)chport.node.ext_node.ptr; rnode != null;
			rnode = rnode.same_next)
		{
			if (rnode.row.number == row.row_num) break;
		}
		SCROUTEPORT rport = new SCROUTEPORT();
		rport.place = nplace;
		rport.port = inst.ports;
		rport.node = rnode;
		rport.flags = 0;
		rport.next = null;
		rport.last = rnode.lastport;
		if (rnode.lastport != null)
		{
			rnode.lastport.next = rport;
		} else
		{
			rnode.firstport = rport;
		}
		rnode.lastport = rport;

		// add to channel
		Sc_route_add_port_to_channel(rport, chport.port.node.ext_node,
			cell.route.channels, chport.node.channel.number);

		return chport.node.lastport;
	}

	/**
	 * Method to route the tracks in each channel by using an improved channel router.
	 * @param channels list of all channels.
	 * @param cell pointer to parent cell.
	 */
	private static void Sc_route_tracks_in_channels(SCROUTECHANNEL channels, SilComp.SCCELL cell)
	{
		// do for each channel individually
		for (SCROUTECHANNEL chan = channels; chan != null; chan = chan.next)
		{
			if (DEBUG)
				System.out.println("**** Routing tracks for Channel " + chan.number+ " ****");

			// create Vertical Constraint Graph (VCG)
			SCROUTEVCG v_graph = Sc_route_create_VCG(chan, cell);

			// create Zone Representation Graph (ZRG)
			SCROUTEZRG zr_graph = Sc_route_create_ZRG(chan);

			// do track assignment
			SCROUTETRACK tracks = Sc_route_track_assignment(v_graph, zr_graph, chan.nodes);

			chan.tracks = tracks;
		}
	}

	/**
	 * Method to create the Vertical Constrain Graph (VCG) for the indicated channel.
	 * @param channel pointer to channel.
	 * @param cell pointer to parent cell.
	 * @return where to write created VCG.
	 */
	private static SCROUTEVCG Sc_route_create_VCG(SCROUTECHANNEL channel, SilComp.SCCELL cell)
	{
		// first number channel nodes to represent nets
		int net_number = 0;
		for (SCROUTECHNODE chnode = channel.nodes; chnode != null; chnode = chnode.next)
		{
			chnode.number = net_number++;

			// calculate actual port position
			for (SCROUTECHPORT chport = chnode.firstport; chport != null; chport = chport.next)
				chport.xpos = Sc_route_port_position(chport.port);

			// sort all channel ports on node from leftmost to rightmost
			for (SCROUTECHPORT chport = chnode.firstport; chport != null; chport = chport.next)
			{
				// bubble port left if necessay
				for (SCROUTECHPORT port2 = chport.last; port2 != null; port2 = chport.last)
				{
					if (port2.xpos <= chport.xpos) break;

					// move chport left
					chport.last = port2.last;
					port2.last = chport;
					if (chport.last != null)
						chport.last.next = chport;
					port2.next = chport.next;
					chport.next = port2;
					if (port2.next != null)
						port2.next.last = port2;
					if (port2 == chnode.firstport)
						chnode.firstport = chport;
					if (chport == chnode.lastport)
						chnode.lastport = port2;
				}
			}
		}

		// create the VCG root node
		SCROUTEVCG vcg_root = new SCROUTEVCG();
		vcg_root.chnode = null;
		vcg_root.edges = null;

		// create a VCG node for each channel node (or net)
		for (SCROUTECHNODE chnode = channel.nodes; chnode != null; chnode = chnode.next)
		{
			SCROUTEVCG vcg_node = new SCROUTEVCG();
			vcg_node.chnode = chnode;
			vcg_node.edges = null;
			SCROUTEVCGEDGE vcg_edge = new SCROUTEVCGEDGE();
			vcg_edge.node = vcg_node;
			vcg_edge.next = vcg_root.edges;
			vcg_root.edges = vcg_edge;
		}

		Sc_route_VCG_create_dependents(vcg_root, channel);

		// add any ports in this channel tied to power
		Sc_route_create_power_ties(channel, vcg_root, cell);

		// add any ports in this channel tied to ground
		Sc_route_create_ground_ties(channel, vcg_root, cell);

		// remove all dependent nodes from root of constraint graph*/
		// clear seen flag
		for (SCROUTEVCGEDGE vcg_edge = vcg_root.edges; vcg_edge != null; vcg_edge = vcg_edge.next)
			vcg_edge.node.flags &= ~SCROUTESEEN;

		// mark all VCG nodes that are called by others
		for (SCROUTEVCGEDGE vcg_edge = vcg_root.edges; vcg_edge != null; vcg_edge = vcg_edge.next)
		{
			for (SCROUTEVCGEDGE edge1 = vcg_edge.node.edges; edge1 != null; edge1 = edge1.next)
				edge1.node.flags |= SCROUTESEEN;
		}

		// remove all edges from root which are marked
		SCROUTEVCGEDGE edge1 = vcg_root.edges;
		for (SCROUTEVCGEDGE vcg_edge = vcg_root.edges; vcg_edge != null; vcg_edge = vcg_edge.next)
		{
			if ((vcg_edge.node.flags & SCROUTESEEN) != 0)
			{
				if (vcg_edge == vcg_root.edges)
				{
					vcg_root.edges = vcg_edge.next;
					edge1 = vcg_edge.next;
				} else
				{
					edge1.next = vcg_edge.next;
				}
			} else
			{
				edge1 = vcg_edge;
			}
		}

		// print out Vertical Constraint Graph if verbose flag set
		if (DEBUG)
		{
			System.out.println("************ VERTICAL CONSTRAINT GRAPH");
			for (edge1 = vcg_root.edges; edge1 != null; edge1 = edge1.next)
			{
				System.out.println("Net " + edge1.node.chnode.number + ":");
				Sc_route_print_VCG(edge1.node.edges, 1);
			}
		}

		return vcg_root;
	}

	/**
	 * Method to resolve any cyclic dependencies in the Vertical Constraint Graph.
	 * @param vcg_root pointer to root of VCG.
	 * @param channel pointer to particular channel.
	 */
	private static void Sc_route_VCG_create_dependents(SCROUTEVCG vcg_root, SCROUTECHANNEL channel)
	{
		boolean check = true;
		while (check)
		{
			check = false;
			Sc_route_VCG_set_dependents(vcg_root);
			GenMath.MutableInteger found = new GenMath.MutableInteger(0);
			GenMath.MutableDouble diff = new GenMath.MutableDouble(0);
			Place.SCNBPLACE place = Sc_route_VCG_cyclic_check(vcg_root, diff, found);
			if (found.intValue() != 0)
			{
				check = true;

				// move place and update row
				for (Place.SCNBPLACE place2 = place; place2 != null; place2 = place2.next)
					place2.xpos += diff.doubleValue();

				// update channel port positions
				for (SCROUTECHNODE chnode = channel.nodes; chnode != null; chnode = chnode.next)
				{
					// calculate actual port position
					for (SCROUTECHPORT chport = chnode.firstport; chport != null; chport = chport.next)
						chport.xpos = Sc_route_port_position(chport.port);

					// reorder port positions from left to right
					for (SCROUTECHPORT chport = chnode.firstport; chport != null; chport = chport.next)
					{
						for (SCROUTECHPORT port2 = chport.last; port2 != null; port2 = chport.last)
						{
							if (port2.xpos <= chport.xpos)
								break;

							// move chport left
							chport.last = port2.last;
							port2.last = chport;
							if (chport.last != null)
								chport.last.next = chport;
							port2.next = chport.next;
							chport.next = port2;
							if (port2.next != null)
								port2.next.last = port2;
							if (port2 == chnode.firstport)
								chnode.firstport = chport;
							if (chport == chnode.lastport)
								chnode.lastport = port2;
						}
					}
				}
			}
		}
	}

	/**
	 * Method to create a directed edge if one channel node must be routed before another.
	 * @param vcg_root root of Vertical Constraint Graph.
	 */
	private static void Sc_route_VCG_set_dependents(SCROUTEVCG vcg_root)
	{
		// clear all dependencies
		for (SCROUTEVCGEDGE edge1 = vcg_root.edges; edge1 != null; edge1 = edge1.next)
			edge1.node.edges = null;

		// set all dependencies
		for (SCROUTEVCGEDGE edge1 = vcg_root.edges; edge1 != null; edge1 = edge1.next)
		{
			for (SCROUTEVCGEDGE edge2 = edge1.next; edge2 != null; edge2 = edge2.next)
			{
				// Given two channel nodes, create a directed edge if
				// one must be routed before the other
				boolean depend1 = false, depend2 = false;
				for (SCROUTECHPORT port1 = edge1.node.chnode.firstport; port1 != null; port1 = port1.next)
				{
					for (SCROUTECHPORT port2 = edge2.node.chnode.firstport; port2 != null; port2 = port2.next)
					{
						if (Math.abs(port1.xpos - port2.xpos) < SilComp.getMinPortDistance())
						{
							// determine which one goes first
							if (port1.port.node.row.number > port2.port.node.row.number)
							{
								depend1 = true;
							} else
							{
								depend2 = true;
							}
						}
					}
				}
				if (depend1)
				{
					SCROUTEVCGEDGE vcg_edge = new SCROUTEVCGEDGE();
					vcg_edge.node = edge2.node;
					vcg_edge.next = edge1.node.edges;
					edge1.node.edges = vcg_edge;
				}
				if (depend2)
				{
					SCROUTEVCGEDGE vcg_edge = new SCROUTEVCGEDGE();
					vcg_edge.node = edge1.node;
					vcg_edge.next = edge2.node.edges;
					edge2.node.edges = vcg_edge;
				}
			}
		}
	}

	/**
	 * Method to return TRUE if cyclic dependency is found in Vertical Constraint Graph.
	 * Also set place and offset needed to resolve this conflict.
	 * Note that only the top row may be moved around as the bottom row
	 * may have already been used by another channel.
	 * @param vcg_root root of Vertical Constraint Graph.
	 * @param diff offset required is stored here.
	 * @return pointer to place.
	 */
	private static Place.SCNBPLACE Sc_route_VCG_cyclic_check(SCROUTEVCG vcg_root, GenMath.MutableDouble diff, GenMath.MutableInteger found)
	{
		// check each VCG node
		Place.SCNBPLACE place = null;
		for (SCROUTEVCGEDGE edge = vcg_root.edges; edge != null; edge = edge.next)
		{
			// clear all flags
			for (SCROUTEVCGEDGE edge3 = vcg_root.edges; edge3 != null; edge3 = edge3.next)
			{
				edge3.node.flags &= ~(SCROUTESEEN | SCROUTETEMPNUSE);
			}

			// mark this node
			edge.node.flags |= SCROUTESEEN;

			// check single cycle
			for (SCROUTEVCGEDGE edge2 = edge.node.edges; edge2 != null; edge2 = edge2.next)
			{
				SCROUTEVCG last_node = edge.node;
				GenMath.MutableInteger subFound = new GenMath.MutableInteger(0);
				last_node = Sc_route_VCG_single_cycle(edge2.node, last_node, subFound);
				if (subFound.intValue() != 0)
				{
					// find place of conflict
					for (SCROUTECHPORT port1 = edge.node.chnode.firstport; port1 != null; port1 = port1.next)
					{
						for (SCROUTECHPORT port2 = last_node.chnode.firstport; port2 != null; port2 = port2.next)
						{
							if (Math.abs(port1.xpos - port2.xpos) < SilComp.getMinPortDistance())
							{
								// determine which one goes first
								if (port1.port.node.row.number >
									port2.port.node.row.number)
								{
									place = port1.port.place;
									if (port1.xpos < port2.xpos)
									{
										diff.setValue((port2.xpos - port1.xpos) + SilComp.getMinPortDistance());
									} else
									{
										diff.setValue(SilComp.getMinPortDistance() - (port1.xpos - port2.xpos));
									}
								} else if (port2.port.node.row.number > port1.port.node.row.number)
								{
									place = port2.port.place;
									if (port2.xpos < port1.xpos)
									{
										diff.setValue((port1.xpos - port2.xpos) + SilComp.getMinPortDistance());
									} else
									{
										diff.setValue(SilComp.getMinPortDistance() - (port2.xpos - port1.xpos));
									}
								} else
								{
									System.out.println("SEVERE ERROR - Cyclic conflict to same row, check leaf cells.");
									System.out.println("At " + port1.port.place.cell.name + " " + ((PortProto)port1.port.port.port).getName() +
										" to " + port2.port.place.cell.name + " " + ((PortProto)port2.port.port.port).getName());
									return null;
								}
								found.setValue(1);
								return place;
							}
						}
					}
					System.out.println("SEVERE WARNING - Cyclic conflict discovered but cannot find place to resolve.");
				}
			}
		}
		found.setValue(0);
		return place;
	}

	/**
	 * Method to decide whether Breadth First Search encounters the marked node.
	 * @param node node to start search.
	 * @param last_node last node searched.
	 * @param found MutableInteger to hold result: nonzero if marked node found.
	 * @return the last node searched.
	 */
	private static SCROUTEVCG Sc_route_VCG_single_cycle(SCROUTEVCG node, SCROUTEVCG last_node, GenMath.MutableInteger found)
	{
		if (node == null)
		{
			found.setValue(0);
			return last_node;
		}
		if ((node.flags & SCROUTESEEN) != 0)
		{
			// marked node found
			found.setValue(1);
			return last_node;
		}
		if ((node.flags & SCROUTETEMPNUSE) != 0)
		{
			// been here before
			found.setValue(0);
			return last_node;
		} else
		{
			// check others
			node.flags |= SCROUTETEMPNUSE;
			SCROUTEVCG save_node = last_node;
			for (SCROUTEVCGEDGE edge = node.edges; edge != null; edge = edge.next)
			{
				last_node = node;
				last_node = Sc_route_VCG_single_cycle(edge.node, last_node, found);
				if (found.intValue() != 0) return last_node;
			}
			last_node = save_node;
			found.setValue(0);
			return last_node;
		}
	}

	/**
	 * Method to create the Zone Representation Graph (ZRG) for the indicated channel.
	 * @param channel pointer to channel.
	 * @return the created ZRG.
	 */
	private static SCROUTEZRG Sc_route_create_ZRG(SCROUTECHANNEL channel)
	{
		SCROUTEZRG first_zone = null, last_zone = null;
		int z_number = 0;

		// create first zone
		SCROUTEZRG zone = new SCROUTEZRG();
		zone.number = z_number++;
		zone.chnodes = null;
		zone.next = null;
		zone.last = null;
		first_zone = last_zone = zone;

		// clear flag on all channel nodes
		int num_chnodes = 0;
		for (SCROUTECHNODE chnode = channel.nodes; chnode != null; chnode = chnode.next)
		{
			chnode.flags &= ~SCROUTESEEN;
			num_chnodes++;
		}

		// allocate enough space for channel node temporary list
		SCROUTECHNODE [] chnode_list = new SCROUTECHNODE[num_chnodes+1];

		for(;;)
		{
			SCROUTECHNODE left_chnode = Sc_route_find_leftmost_chnode(channel.nodes);
			if (left_chnode == null) break;
			Sc_route_create_zrg_temp_list(channel.nodes, left_chnode.firstport.xpos, chnode_list);
			if (Sc_route_zrg_list_compatible(chnode_list, zone))
			{
				Sc_route_zrg_add_chnodes(chnode_list, zone);
			} else
			{
				zone = new SCROUTEZRG();
				zone.number = z_number++;
				zone.chnodes = null;
				zone.next = null;
				zone.last = last_zone;
				last_zone.next = zone;
				last_zone = zone;
				Sc_route_zrg_add_chnodes(chnode_list, zone);
			}
			left_chnode.flags |= SCROUTESEEN;
		}

		// print out zone representation if verbose flag set
		if (DEBUG)
		{
			System.out.println("************ ZONE REPRESENTATION GRAPH");
			for (zone = first_zone; zone != null; zone = zone.next)
			{
				System.out.println("Zone " + zone.number + ":");
				for (SCROUTEZRGMEM mem = zone.chnodes; mem != null; mem = mem.next)
					System.out.println("    Node " + mem.chnode.number);
			}
		}
		return first_zone;
	}

	/**
	 * Method to return a pointer to the unmarked channel node of the indicated
	 * channel which has the left-most first port.
	 * If no channel nodes suitable found, return null.
	 * @param nodes pointer to a list of channel nodes.
	 * @return pointer to leftmost node, null if none unmarked found.
	 */
	private static SCROUTECHNODE Sc_route_find_leftmost_chnode(SCROUTECHNODE nodes)
	{
		SCROUTECHNODE left_chnode = null;
		double left_xpos = Double.MAX_VALUE;

		for (SCROUTECHNODE node = nodes; node != null; node = node.next)
		{
			if ((node.flags & SCROUTESEEN) != 0) continue;
			if (node.firstport.xpos < left_xpos)
			{
				left_xpos = node.firstport.xpos;
				left_chnode = node;
			}
		}

		return left_chnode;
	}

	/**
	 * Method to fill in the temporary list of all channel nodes which encompass the indicated x position.
	 * @param nodes list of channel nodes.
	 * @param xpos X position of interest.
	 * @param list array of pointer to fill in, terminate with a null.
	 */
	private static void Sc_route_create_zrg_temp_list(SCROUTECHNODE nodes, double xpos, SCROUTECHNODE [] list)
	{
		int i = 0;
		for (SCROUTECHNODE node = nodes; node != null; node = node.next)
		{
			if (xpos > node.firstport.xpos - SilComp.getMinPortDistance()
				&& xpos < node.lastport.xpos + SilComp.getMinPortDistance())
					list[i++] = node;
		}
		list[i] = null;
	}

	/**
	 * Method to return a TRUE if the indicated list of channel nodes is compatible
	 * with the indicated zone, else return FALSE.
	 * @param list array of pointers to channel nodes.
	 * @param zone pointer to current zone.
	 * @return true if compatible.
	 */
	private static boolean Sc_route_zrg_list_compatible(SCROUTECHNODE [] list, SCROUTEZRG zone)
	{
		if (zone.chnodes != null)
		{
			// check each member of current zone being in the list
			for (SCROUTEZRGMEM mem = zone.chnodes; mem != null; mem = mem.next)
			{
				int i;
				for (i = 0; list[i] != null; i++)
				{
					if (mem.chnode == list[i]) break;
				}
				if (list[i] == null)
					return false;
			}
			return true;
		} else
		{
			// no current channel nodes, so compatible
			return true;
		}
	}

	/**
	 * Method to add the channel nodes in the list to the indicated zone if they
	 * are not already in the zone.
	 * @param list list of channel nodes.
	 * @param zone pointer to current zone.
	 */
	private static void Sc_route_zrg_add_chnodes(SCROUTECHNODE [] list, SCROUTEZRG zone)
	{
		for (int i = 0; list[i] != null; i++)
		{
			SCROUTEZRGMEM mem;
			for (mem = zone.chnodes; mem != null; mem = mem.next)
			{
				if (mem.chnode == list[i])
					break;
			}
			if (mem == null)
			{
				mem = new SCROUTEZRGMEM();
				mem.chnode = list[i];
				mem.next = zone.chnodes;
				zone.chnodes = mem;
			}
		}
	}

	/**
	 * Method to use the Vertical Constraint Graph and the Zone Representation
	 * Graph, assign channel nodes to tracks.
	 * @param vcg pointer to Vertical Constraint Graph.
	 * @param zrg pointer to Zone Representation Graph.
	 * @param nodes pointer to list of channel nodes.
	 * @return created tracks.
	 */
	private static SCROUTETRACK Sc_route_track_assignment(SCROUTEVCG vcg, SCROUTEZRG zrg, SCROUTECHNODE nodes)
	{
		SCROUTETRACK first_track = null, last_track = null;
		int track_number = 0;

		// create first track
		SCROUTETRACK track = new SCROUTETRACK();
		track.number = track_number++;
		track.nodes = null;
		track.last = null;
		track.next = null;
		first_track = last_track = track;

		// clear flags on all channel nodes
		int number_nodes = 0;
		for (SCROUTECHNODE node = nodes; node != null; node = node.next)
		{
			node.flags = 0;
			number_nodes++;
		}
		SCROUTECHNODE [] n_list = new SCROUTECHNODE[number_nodes+1];

		// get channel node on longest path of VCG
		for(;;)
		{
			SCROUTECHNODE node = Sc_route_longest_VCG(vcg);
			if (node == null) break;

			// clear flags of all nodes
			for (SCROUTECHNODE node2 = nodes; node2 != null; node2 = node2.next)
				node2.flags = 0;

			// add node to track
			Sc_route_add_node_to_track(node, track);

			// mark all other nodes in the same zones as not usable
			Sc_route_mark_zones(node, zrg, SCROUTEUNUSABLE);

			// find set of remaining nodes which can be added to track
			Sc_route_find_best_nodes(vcg, zrg, n_list, number_nodes + 1);

			// add to track
			for (int i = 0; n_list[i] != null; i++)
			{
				Sc_route_add_node_to_track(n_list[i], track);
			}

			// delete track entries from VCG
			Sc_route_delete_from_VCG(track, vcg);

			// create next track
			track = new SCROUTETRACK();
			track.number = track_number++;
			track.nodes = null;
			track.last = last_track;
			last_track.next = track;
			last_track = track;
		}

		// delete last track if empty
		if (track.nodes == null)
		{
			if (track.last != null)
			{
				track.last.next = null;
			} else
			{
				first_track = null;
			}
		}

		// print out track assignment if verbose flag set
		if (DEBUG)
		{
			System.out.println("************ TRACK ASSIGNMENT");
			for (track = first_track; track != null; track = track.next)
			{
				System.out.println("For Track #" + track.number + ":");
				for (SCROUTETRACKMEM mem = track.nodes; mem != null; mem = mem.next)
					System.out.println("    " + mem.node.number + "     " + mem.node.firstport.xpos + "  " + mem.node.lastport.xpos);
			}
		}

		return first_track;
	}

	/**
	 * Method to mark all channel nodes in the same zones as the indicated zone as indicated.
	 * @param node channel node in question.
	 * @param zrg zone representation graph.
	 * @param bits bits to OR in to nodes flag field.
	 */
	private static void Sc_route_mark_zones(SCROUTECHNODE node, SCROUTEZRG zrg, int bits)
	{
		// mark unusable nodes
		for (SCROUTEZRG zone = zrg; zone != null; zone = zone.next)
		{
			SCROUTEZRGMEM zmem;
			for (zmem = zone.chnodes; zmem != null; zmem = zmem.next)
			{
				if (zmem.chnode == node) break;
			}
			if (zmem != null)
			{
				for (zmem = zone.chnodes; zmem != null; zmem = zmem.next)
					zmem.chnode.flags |= bits;
			}
		}
	}

	/**
	 * Method to return a pointer to the channel node which is not dependent on
	 * any other nodes (i.e. top of Vertical Constraint Graph) and on
	 * the longest path of the VCG.  If a tie, return the first one.
	 * If none found, return null.  Remove and update VCG.
	 * @param vcg pointer to Vertical Constraint Graph.
	 * @return channel node, null if node.
	 */
	private static SCROUTECHNODE Sc_route_longest_VCG(SCROUTEVCG vcg)
	{
		SCROUTECHNODE node = null;
		double longest_path = 0;

		// check for all entries at the top level
		for (SCROUTEVCGEDGE edge = vcg.edges; edge != null; edge = edge.next)
		{
			double path = Sc_route_path_length(edge.node);
			if (path > longest_path)
			{
				longest_path = path;
				node = edge.node.chnode;
			}
		}

		return node;
	}

	/**
	 * Method to return the length of the longest path starting at the indicated
	 * Vertical Constraint Graph Node.
	 * @param vcg_node vertical Constraint Graph node.
	 * @return longest path length.
	 */
	private static double Sc_route_path_length(SCROUTEVCG vcg_node)
	{
		if (vcg_node.edges == null) return 1;

		// check path for all edges
		double longest = 0;
		for (SCROUTEVCGEDGE edge = vcg_node.edges; edge != null; edge = edge.next)
		{
			double path = Sc_route_path_length(edge.node);
			if (path > longest)
				longest = path;
		}

		return longest + 1;
	}

	/**
	 * Method to add the indicated channel node to the track and mark as seen.
	 * Note add the node in left to right order.
	 * @param node pointer to channel node to add.
	 * @param track pointer to track to add to.
	 */
	private static void Sc_route_add_node_to_track(SCROUTECHNODE node, SCROUTETRACK track)
	{
		SCROUTETRACKMEM mem = new SCROUTETRACKMEM();
		mem.node = node;
		mem.next = null;
		if (track.nodes == null)
		{
			track.nodes = mem;
		} else
		{
			SCROUTETRACKMEM oldmem = track.nodes;
			SCROUTETRACKMEM mem2;
			for (mem2 = track.nodes; mem2 != null; mem2 = mem2.next)
			{
				if (mem.node.firstport.xpos > mem2.node.firstport.xpos)
				{
					oldmem = mem2;
				} else
				{
					break;
				}
			}
			mem.next = mem2;
			if (mem2 == track.nodes)
			{
				track.nodes = mem;
			} else
			{
				oldmem.next = mem;
			}
		}

		node.flags |= SCROUTESEEN;
	}

	/**
	 * Method to find the set of remaining nodes with no ancestors in the Vertical
	 * Constraint Graph which are available and are of maximum combined length.
	 * @param vcg vertical Constraint Graph.
	 * @param zrg zone Representation Graph.
	 * @param n_list array to write list of selected nodes.
	 * @param num maximum size of n_list.
	 */
	private static void Sc_route_find_best_nodes(SCROUTEVCG vcg, SCROUTEZRG zrg, SCROUTECHNODE [] n_list, int num)
	{
		int i = 0;
		n_list[i] = null;

		// try all combinations
		for(;;)
		{
			// find longest, usable edge
			SCROUTEVCGEDGE edge2 = null;
			double max_length = 0;
			for (SCROUTEVCGEDGE edge = vcg.edges; edge != null; edge = edge.next)
			{
				if ((edge.node.chnode.flags & (SCROUTESEEN | SCROUTEUNUSABLE)) != 0)
					continue;
				double length = edge.node.chnode.lastport.xpos - edge.node.chnode.firstport.xpos;
				if (length >= max_length)
				{
					max_length = length;
					edge2 = edge;
				}
			}
			if (edge2 == null)
			{
				break;
			} else
			{
				// add to list
				n_list[i++] = edge2.node.chnode;
				n_list[i] = null;
				Sc_route_mark_zones(edge2.node.chnode, zrg, SCROUTEUNUSABLE);
			}
		}
	}

	/**
	 * Method to delete all channel nodes in the track from the top level of the
	 * Vertical Constraint Graph and update VCG.
	 * @param track pointer to track.
	 * @param vcg pointer to Vertical Constraint Graph.
	 */
	private static void Sc_route_delete_from_VCG(SCROUTETRACK track, SCROUTEVCG vcg)
	{
		// for all track entries in VCG
		for (SCROUTETRACKMEM mem = track.nodes; mem != null; mem = mem.next)
		{
			SCROUTEVCGEDGE edge2 = vcg.edges;
			for (SCROUTEVCGEDGE edge = vcg.edges; edge != null; edge = edge.next)
			{
				if (edge.node.chnode != mem.node)
				{
					edge2 = edge;
					continue;
				}

				// remove from top level VCG
				if (edge == vcg.edges) vcg.edges = edge.next;
					else edge2.next = edge.next;

				// check if its edges have nodes which should be added to VCG
				for (edge2 = edge.node.edges; edge2 != null; edge2 = edge2.next)
					edge2.node.flags &= ~SCROUTESEEN;

				// mark any child edges
				for (edge2 = edge.node.edges; edge2 != null; edge2 = edge2.next)
					Sc_route_mark_VCG(edge2.node.edges);

				Sc_route_mark_VCG(vcg.edges);
				SCROUTEVCGEDGE edge3 = null;
				for (edge2 = edge.node.edges; edge2 != null; edge2 = edge3)
				{
					edge3 = edge2.next;
					if ((edge2.node.flags & SCROUTESEEN) == 0)
					{
						// add to top level
						edge2.next = vcg.edges;
						vcg.edges = edge2;
					}
				}
				break;
			}
		}
	}

	/**
	 * Method to recursively mark all nodes of Vertical Constraint Graph called by other nodes.
	 * @param edges list of edges.
	 */
	private static void Sc_route_mark_VCG(SCROUTEVCGEDGE edges)
	{
		if (edges == null) return;

		for ( ; edges != null; edges = edges.next)
		{
			edges.node.flags |= SCROUTESEEN;
			Sc_route_mark_VCG(edges.node.edges);
		}
	}

	/**
	 * Method to add data to insure that input ports of the row below tied to power
	 * are handled correctly.  Due to the fact that the power ports are
	 * assumed to be in the horizontal routing layer, these ties must
	 * be at the bottom of the routing channel.
	 * @param chan pointer to current channel.
	 * @param vcg pointer to Vertical Constrant Graph.
	 * @param cell pointer to parent cell.
	 */
	private static void Sc_route_create_power_ties(SCROUTECHANNEL chan, SCROUTEVCG vcg, SilComp.SCCELL cell)
	{
		// check for bottom channel
		if (chan.number == 0) return;

		// get correct row
		Place.SCROWLIST row = cell.placement.rows;
		for (int num = 1; num < chan.number && row != null; num++)
			row = row.next;
		if (row == null) return;

		// get correct route row
		SCROUTEROW rrow = cell.route.rows;
		for (int num = 1; num < chan.number; num++)
			rrow = rrow.next;

		// check all places in row if Base Cell
		for (Place.SCNBPLACE place = row.start; place != null; place = place.next)
		{
			if (place.cell.type != SilComp.SCLEAFCELL) continue;

			// check all ports of instance for reference to power
			for (SilComp.SCNIPORT port = place.cell.ports; port != null; port = port.next)
			{
				if (port.ext_node == cell.power)
				{
					// found one
					// should be a power port on this instance
					if (place.cell.power == null)
					{
						System.out.println("WARNING - Cannot find power on " + place.cell.name);
						continue;
					}

					// create new route node
					SCROUTENODE rnode = new SCROUTENODE();
					rnode.ext_node = port.ext_node;
					rnode.row = rrow;
					rnode.firstport = null;
					rnode.lastport = null;
					rnode.same_next = null;
					rnode.same_last = null;
					rnode.next = rrow.nodes;
					rrow.nodes = rnode;

					// create route ports to these ports
					SCROUTEPORT rport1 = new SCROUTEPORT();
					rport1.place = place;
					rport1.port = port;
					rport1.node = rnode;
					rnode.firstport = rport1;
					rport1.flags = 0;
					rport1.last = null;
					rport1.next = null;
					SCROUTEPORT rport2 = new SCROUTEPORT();
					rport2.place = place;
					rport2.port = place.cell.power;
					rport2.node = rnode;
					rnode.lastport = rport2;
					rport2.flags = 0;
					rport2.last = rport1;
					rport1.next = rport2;
					rport2.next = null;

					// create channel node
					SCROUTECHNODE chnode = new SCROUTECHNODE();
					chnode.ext_node = port.ext_node;
					chnode.number = 0;
					chnode.firstport = null;
					chnode.lastport = null;
					chnode.channel = chan;
					chnode.flags = 0;
					chnode.same_next = null;
					chnode.same_last = null;
					chnode.next = chan.nodes;
					chan.nodes = chnode;

					// create channel ports
					SCROUTECHPORT chport1 = new SCROUTECHPORT();
					chport1.port = rport1;
					chport1.node = chnode;
					chport1.xpos = Sc_route_port_position(rport1);
					chport1.flags = 0;
					chport1.last = null;
					chport1.next = null;
					SCROUTECHPORT chport2 = new SCROUTECHPORT();
					chport2.port = rport2;
					chport2.node = chnode;
					chport2.xpos = Sc_route_port_position(rport2);
					chport2.flags = 0;
					chport2.last = null;
					chport2.next = null;
					if (chport1.xpos <= chport2.xpos)
					{
						chnode.firstport = chport1;
						chnode.lastport = chport2;
						chport1.next = chport2;
						chport2.last = chport1;
					} else
					{
						chnode.firstport = chport2;
						chnode.lastport = chport1;
						chport2.next = chport1;
						chport1.last = chport2;
					}

					// create a VCG node
					SCROUTEVCG vnode = new SCROUTEVCG();
					vnode.chnode = chnode;
					vnode.flags = 0;
					vnode.edges = null;

					// create a VCG edge
					SCROUTEVCGEDGE vedge = new SCROUTEVCGEDGE();
					vedge.node = vnode;
					vedge.next = vcg.edges;
					vcg.edges = vedge;

					// make this port dependent on any others which are
					// too close to the power port edge
					for (SCROUTEVCGEDGE edge1 = vedge.next; edge1 != null; edge1 = edge1.next)
					{
						double minx = edge1.node.chnode.firstport.xpos - SilComp.getMinPortDistance();
						double maxx = edge1.node.chnode.lastport.xpos + SilComp.getMinPortDistance();
						if (chport2.xpos > minx && chport2.xpos < maxx)
						{
							// create dependency
							SCROUTEVCGEDGE edge2 = new SCROUTEVCGEDGE();
							edge2.node = vnode;
							edge2.next = edge1.node.edges;
							edge1.node.edges = edge2;
						}
					}
				}
			}
		}
	}

	/**
	 * Method to add data to insure that input ports of the row below tied to ground are handled correctly.
	 * Due to the fact that the ground ports are assumed to be in the horizontal routing layer,
	 * these ties must be at the top of the routing channel.
	 * @param chan pointer to current channel.
	 * @param vcg pointer to Vertical Constrant Graph.
	 * @param cell pointer to parent cell.
	 */
	private static void Sc_route_create_ground_ties(SCROUTECHANNEL chan, SCROUTEVCG vcg, SilComp.SCCELL cell)
	{
		// check for not top channel
		if (chan.number == cell.placement.num_rows) return;

		// get correct row (above)
		Place.SCROWLIST row = cell.placement.rows;
		for (int num = 0; num < chan.number && row != null; num++)
			row = row.next;
		if (row == null) return;

		// get correct route row (above)
		SCROUTEROW rrow = cell.route.rows;
		for (int num = 0; num < chan.number; num++)
			rrow = rrow.next;

		// check all places in row if Base Cell
		for (Place.SCNBPLACE place = row.start; place != null; place = place.next)
		{
			if (place.cell.type != SilComp.SCLEAFCELL) continue;

			// check all ports of instance for reference to ground
			for (SilComp.SCNIPORT port = place.cell.ports; port != null; port = port.next)
			{
				if (port.ext_node == cell.ground)
				{
					// found one
					// should be a ground port on this instance
					if (place.cell.ground == null)
					{
						System.out.println("WARNING - Cannot find ground on " + place.cell.name);
						continue;
					}

					// create new route node
					SCROUTENODE rnode = new SCROUTENODE();
					rnode.ext_node = port.ext_node;
					rnode.row = rrow;
					rnode.firstport = null;
					rnode.lastport = null;
					rnode.same_next = null;
					rnode.same_last = null;
					rnode.next = rrow.nodes;
					rrow.nodes = rnode;

					// create route ports to these ports
					SCROUTEPORT rport1 = new SCROUTEPORT();
					rport1.place = place;
					rport1.port = port;
					rport1.node = rnode;
					rnode.firstport = rport1;
					rport1.flags = 0;
					rport1.last = null;
					rport1.next = null;
					SCROUTEPORT rport2 = new SCROUTEPORT();
					rport2.place = place;
					rport2.port = place.cell.ground;
					rport2.node = rnode;
					rnode.lastport = rport2;
					rport2.flags = 0;
					rport2.last = rport1;
					rport1.next = rport2;
					rport2.next = null;

					// create channel node
					SCROUTECHNODE chnode = new SCROUTECHNODE();
					chnode.ext_node = port.ext_node;
					chnode.number = 0;
					chnode.firstport = null;
					chnode.lastport = null;
					chnode.channel = chan;
					chnode.flags = 0;
					chnode.same_next = null;
					chnode.same_last = null;
					chnode.next = chan.nodes;
					chan.nodes = chnode;

					// create channel ports
					SCROUTECHPORT chport1 = new SCROUTECHPORT();
					chport1.port = rport1;
					chport1.node = chnode;
					chport1.xpos = Sc_route_port_position(rport1);
					chport1.flags = 0;
					chport1.last = null;
					chport1.next = null;
					SCROUTECHPORT chport2 = new SCROUTECHPORT();
					chport2.port = rport2;
					chport2.node = chnode;
					chport2.xpos = Sc_route_port_position(rport2);
					chport2.flags = 0;
					chport2.last = null;
					chport2.next = null;
					if (chport1.xpos <= chport2.xpos)
					{
						chnode.firstport = chport1;
						chnode.lastport = chport2;
						chport1.next = chport2;
						chport2.last = chport1;
					} else
					{
						chnode.firstport = chport2;
						chnode.lastport = chport1;
						chport2.next = chport1;
						chport1.last = chport2;
					}

					// create a VCG node
					SCROUTEVCG vnode = new SCROUTEVCG();
					vnode.chnode = chnode;
					vnode.flags = 0;
					vnode.edges = null;

					// create a VCG edge
					SCROUTEVCGEDGE vedge = new SCROUTEVCGEDGE();
					vedge.node = vnode;
					vedge.next = vcg.edges;
					vcg.edges = vedge;

					// make all others VCG nodes which are too close to
					// the ground port edge dependent on this node
					for (SCROUTEVCGEDGE edge1 = vedge.next; edge1 != null; edge1 = edge1.next)
					{
						double minx = edge1.node.chnode.firstport.xpos - SilComp.getMinPortDistance();
						double maxx = edge1.node.chnode.lastport.xpos + SilComp.getMinPortDistance();
						if (chport2.xpos > minx && chport2.xpos < maxx)
						{
							// create dependency
							SCROUTEVCGEDGE edge2 = new SCROUTEVCGEDGE();
							edge2.node = edge1.node;
							edge2.next = vnode.edges;
							vnode.edges = edge2;
						}
					}
				}
			}
		}
	}

	/**
	 * Method to recursively print out the VCG for the indicated edge list.
	 * @param edges list of VCG edges.
	 * @param level level of indentation.
	 */
	private static void Sc_route_print_VCG(SCROUTEVCGEDGE edges, int level)
	{
		if (edges == null) return;

		StringBuffer sb = new StringBuffer();
		int i = level << 2;
		for (int j = 0; j < i; j++)
			sb.append(" ");

		for (; edges != null; edges = edges.next)
		{
			System.out.println(sb.toString() + "before Net " + edges.node.chnode.number);
			Sc_route_print_VCG(edges.node.edges, level + 1);
		}
	}

}
