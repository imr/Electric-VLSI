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

import java.util.Iterator;
import java.util.List;

/**
 * The routing part of the Silicon Compiler tool.
 */
public class Route
{
	/** not verbose default */			private static final boolean DEBUG = false;

	/** seen in processing */			private static final int ROUTESEEN		= 0x00000001;
	/** unusable in current track */	private static final int ROUTEUNUSABLE	= 0x00000002;
	/** temporary not use */			private static final int ROUTETEMPNUSE	= 0x00000004;

	/** fuzzy window for pass th. */	private static final double DEFAULT_FUZZY_WINDOW_LIMIT	= 6400;

	/** global feed through number */	private int		feedNumber;

	/**
	 * Class for communicating routing information between router and netlist reader.
	 */
	public static class SCRoute
	{
		/** list of channels */				RouteChannel	channels;
		/** exported ports */				RouteExport		exports;
		/** route rows */					RouteRow		rows;
	};

	private static class RouteRow
	{
		/** number, 0 = bottom */			int				number;
		/** list of extracted nodes */		RouteNode		nodes;
		/** reference actual row */			Place.RowList	row;
		/** last in row list */				RouteRow		last;
		/** next in row list */				RouteRow		next;
	};

	private static class RouteNode
	{
		/** extracted node */				GetNetlist.ExtNode extNode;
		/** reference row */				RouteRow		row;
		/** first port in row */			RoutePort		firstPort;
		/** last port in row */				RoutePort		lastPort;
		/** same nodes in above rows */		RouteNode		sameNext;
		/** same nodes in below rows */		RouteNode		sameLast;
		/** nodes in same row */			RouteNode		next;
	};

	/**
	 * Class for communicating routing information between router and maker.
	 */
	public static class RoutePort
	{
		/** reference place */				Place.NBPlace	place;
		/** particular port */				GetNetlist.SCNiPort port;
		/** reference node */				RouteNode		node;
		/** flags for processing */			int				flags;
		/** previous port in list */		RoutePort		last;
		/** next port in list */			RoutePort		next;
	};

	/**
	 * Class for communicating channel information between router and maker.
	 */
	public static class RouteChannel
	{
		/** number, 0 is bottom */			int				number;
		/** list of nodes */				RouteChNode		nodes;
		/** list of tracks */				RouteTrack		tracks;
		/** last in channel list */			RouteChannel	last;
		/** next in channel list */			RouteChannel	next;
	};

	/**
	 * Class for communicating routing information between router and maker.
	 */
	public static class RouteChNode
	{
		/** extracted node */				GetNetlist.ExtNode extNode;
		/** optional net number */			int				number;
		/** first port in row */			RouteChPort		firstPort;
		/** last port in row */				RouteChPort		lastPort;
		/** reference channel */			RouteChannel	channel;
		/** flags for processing */			int				flags;
		/** same nodes in above rows */		RouteChNode		sameNext;
		/** same nodes in below rows */		RouteChNode		sameLast;
		/** nodes in same row */			RouteChNode		next;
	};

	/**
	 * Class for communicating routing information between router and maker.
	 */
	public static class RouteChPort
	{
		/** reference port */				RoutePort		port;
		/** reference channel node */		RouteChNode		node;
		/** x position */					double			xPos;
		/** flags for processing */			int				flags;
		/** previous port in list */		RouteChPort		last;
		/** next port in list */			RouteChPort		next;
	};

	private static class RouteVCG
	{
		/** channel node */					RouteChNode		chNode;
		/** flags for processing */			int				flags;
		/** edges of graph */				RouteVCGEdge	edges;
	};

	private static class RouteVCGEdge
	{
		/** to which node */				RouteVCG		node;
		/** next in list */					RouteVCGEdge	next;
	};

	private static class RouteZRG
	{
		/** number of zone */				int				number;
		/** list of channel nodes */		RouteZRGMem		chNodes;
		/** last zone */					RouteZRG		last;
		/** next zone */					RouteZRG		next;
	};

	private static class RouteZRGMem
	{
		/** channel node */					RouteChNode		chNode;
		/** next in zone */					RouteZRGMem		next;
	};

	/**
	 * Class for communicating routing information between router and maker.
	 */
	public static class RouteTrack
	{
		/** number of track, 0 = top */		int				number;
		/** track member */					RouteTrackMem	nodes;
		/** last track in list */			RouteTrack		last;
		/** next track in list */			RouteTrack		next;
	};

	/**
	 * Class for communicating routing information between router and maker.
	 */
	public static class RouteTrackMem
	{
		/** channel node */					RouteChNode		node;
		/** next in same track */			RouteTrackMem	next;
	};

	/**
	 * Class for communicating routing information between router and maker.
	 */
	public static class RouteExport
	{
		/** export port */					GetNetlist.SCPort	xPort;
		/** channel port */					RouteChPort		chPort;
		/** next export port */				RouteExport		next;
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
	public String routeCells(GetNetlist gnl)
	{
		// check if working in a cell
		if (gnl.curSCCell == null) return "No cell selected";

		// check if placement structure exists
		if (gnl.curSCCell.placement == null)
			return "No PLACEMENT structure for cell '" + gnl.curSCCell.name + "'";

		// create route structure
		SCRoute route = new SCRoute();
		gnl.curSCCell.route = route;
		route.channels = null;
		route.exports = null;
		route.rows = null;

		// first squeeze cell together
		squeezeCells(gnl.curSCCell.placement.theRows);

		// create list of rows and their usage of extracted nodes
		RouteRow rowList = createRowList(gnl.curSCCell.placement.theRows, gnl.curSCCell);
		route.rows = rowList;

		// create Route Channel List
		RouteChannel channelList = createChannelList(gnl.curSCCell.placement.numRows + 1);
		route.channels = channelList;

		// Do primary channel assignment
		channelAssign(rowList, channelList, gnl.curSCCell);

		// decide upon any pass through cells required
		createPassThroughs(channelList, gnl.curSCCell.placement.theRows);

		// decide upon export positions
		route.exports = decideExports(gnl.curSCCell);

		// route tracks in each channel
		tracksInChannels(channelList, gnl.curSCCell);

		return null;
	}

	/**
	 * Method to try to squeeze adjacent cells in a row as close together.
	 * Checks where their active areas start and uses the minimum active distance.
	 * @param rows pointer to start of row list.
	 */
	private void squeezeCells(List<Place.RowList> theRows)
	{
		for(Place.RowList row : theRows)
		{
			for (Place.NBPlace place = row.start; place != null; place = place.next)
			{
				if (place.next == null) continue;

				// determine allowable overlap
				GetNetlist.SCCellNums cell1Nums = GetNetlist.getLeafCellNums((Cell)place.cell.np);
				GetNetlist.SCCellNums cell2Nums = GetNetlist.getLeafCellNums((Cell)place.next.cell.np);
				double overlap = 0;
				if ((row.rowNum % 2) != 0)
				{
					// odd row, cell are transposed
					overlap = cell2Nums.rightActive + cell1Nums.leftActive
						- SilComp.getMinActiveDistance();
				} else
				{
					// even row
					overlap = cell1Nums.rightActive + cell2Nums.leftActive
						- SilComp.getMinActiveDistance();
				}

				// move rest of row
				for (Place.NBPlace place2 = place.next; place2 != null; place2 = place2.next)
					place2.xPos -= overlap;
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
	private RouteRow createRowList(List<Place.RowList> theRows, GetNetlist.SCCell cell)
	{
		// clear all reference pointers in extracted node list
		for (GetNetlist.ExtNode eNode = cell.exNodes; eNode != null; eNode = eNode.next)
			eNode.ptr = null;

		// create a route row list for each placement row
		RouteRow firstRRow = null, lastRRow = null;
		RouteNode sameNode = null;
		for(Place.RowList row : theRows)
		{
			RouteRow newRRow = new RouteRow();
			newRRow.number = row.rowNum;
			newRRow.nodes = null;
			newRRow.row = row;
			newRRow.last = lastRRow;
			newRRow.next = null;
			if (lastRRow != null)
			{
				lastRRow.next = newRRow;
				lastRRow = newRRow;
			} else
			{
				firstRRow = lastRRow = newRRow;
			}

			// create an entry of every extracted node in each row
			RouteNode lastNode = null;
			for (GetNetlist.ExtNode enode = cell.exNodes; enode != null; enode = enode.next)
			{
				RouteNode newNode = new RouteNode();
				newNode.extNode = enode;
				newNode.row = newRRow;
				newNode.firstPort = null;
				newNode.lastPort = null;
				newNode.sameNext = null;
				newNode.sameLast = sameNode;
				newNode.next = null;
				if (lastNode != null)
				{
					lastNode.next = newNode;
				} else
				{
					newRRow.nodes = newNode;
				}
				lastNode = newNode;
				if (sameNode != null)
				{
					sameNode.sameNext = newNode;
					sameNode = sameNode.next;
				} else
				{
					enode.ptr = newNode;
				}
			}
			sameNode = newRRow.nodes;

			// set reference to all ports on row
			for (Place.NBPlace place = row.start; place != null; place = place.next)
			{
				for (GetNetlist.SCNiPort port = place.cell.ports; port != null; port = port.next)
				{
					RouteNode newNode = (RouteNode)port.extNode.ptr;
					if (newNode != null)
					{
						for (int i = 0; i < row.rowNum; i++)
							newNode = newNode.sameNext;
						RoutePort newPort = new RoutePort();
						newPort.place = place;
						newPort.port = port;
						newPort.node = newNode;
						newPort.next = null;
						newPort.last = newNode.lastPort;
						if (newNode.lastPort != null)
						{
							newNode.lastPort.next = newPort;
						} else
						{
							newNode.firstPort = newPort;
						}
						newNode.lastPort = newPort;
					}
				}
			}
		}

		return firstRRow;
	}

	/**
	 * Method to create the basic channel list.
	 * The number of channels is one more than the number of rows.
	 * @param number number of channels to create.
	 * @return result list.
	 */
	private RouteChannel createChannelList(int number)
	{
		// create channel list
		RouteChannel firstChan = null, lastChan = null;
		for (int i = 0; i < number; i++)
		{
			RouteChannel newChan = new RouteChannel();
			newChan.number = i;
			newChan.nodes = null;
			newChan.tracks = null;
			newChan.next = null;
			newChan.last = lastChan;
			if (lastChan != null)
			{
				lastChan.next = newChan;
			} else
			{
				firstChan = newChan;
			}
			lastChan = newChan;
		}

		return firstChan;
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
	private void channelAssign(RouteRow rows, RouteChannel channels, GetNetlist.SCCell cell)
	{
		// clear flags
		for (RouteRow row = rows; row != null; row = row.next)
		{
			for (RouteNode node = row.nodes; node != null; node = node.next)
			{
				for (RoutePort port = node.firstPort; port != null; port = port.next)
					port.flags &= ~ROUTESEEN;
			}
		}

		for (RouteRow row = rows; row != null; row = row.next)
		{
			for (RouteNode node = row.nodes; node != null; node = node.next)
			{
				if (node.firstPort == null)
					continue;

				// check for ports above
				boolean portsAbove = false;
				for (RouteNode node2 = node.sameNext; node2 != null; node2 = node2.sameNext)
				{
					if (node2.firstPort != null)
					{
						portsAbove = true;
						break;
					}
				}

				// if none found above, any ports in this list only going up
				if (!portsAbove && node.firstPort != node.lastPort)
				{
					for (RoutePort port = node.firstPort; port != null; port = port.next)
					{
						int direct = GetNetlist.getLeafPortDirection((PortProto)port.port.port);
						if ((direct & GetNetlist.PORTDIRUP) != 0 && (direct & GetNetlist.PORTDIRDOWN) == 0)
						{
							portsAbove = true;
							break;
						}
					}
				}

				// check for ports below
				boolean portsBelow = false;
				for (RouteNode node2 = node.sameLast; node2 != null; node2 = node2.sameLast)
				{
					if (node2.firstPort != null)
					{
						portsBelow = true;
						break;
					}
				}

				// if none found below, any ports in this row only going down
				if (!portsBelow && node.firstPort != node.lastPort)
				{
					for (RoutePort port = node.firstPort; port != null; port = port.next)
					{
						int direct = GetNetlist.getLeafPortDirection((PortProto)port.port.port);
						if ((direct & GetNetlist.PORTDIRDOWN) != 0 && (direct & GetNetlist.PORTDIRUP) == 0)
						{
							portsBelow = true;
							break;
						}
					}
				}

				// do not add if only one port unless an export
				if (!portsAbove && !portsBelow)
				{
					if (node.firstPort == node.lastPort)
					{
						GetNetlist.SCPort xPort;
						for (xPort = cell.ports; xPort != null; xPort = xPort.next)
						{
							if (xPort.node.ports.extNode == node.extNode)
								break;
						}
						if (xPort == null) continue;

						// if top row, put in above channel
						if (row.number != 0 && row.next == null)
							portsAbove = true;
					}
				}

				// assign ports to channel
				for (RoutePort port = node.firstPort; port != null; port = port.next)
				{
					if ((port.flags & ROUTESEEN) != 0) continue;

					// check how ports can be connected to
					int direct = GetNetlist.getLeafPortDirection((PortProto)port.port.port);

					// for ports both up and down
					if ((direct & GetNetlist.PORTDIRUP) != 0 && (direct & GetNetlist.PORTDIRDOWN) != 0)
					{
						if (!portsAbove)
						{
							// add to channel below
							addPortToChannel(port, node.extNode, channels, row.number);
						} else
						{
							if (portsBelow)
							{
								// add to channel where closest
								int offset = 0;
								if (nearestPort(port, node, row.number, cell) > 0)
								{
									offset = 1;
								}
								addPortToChannel(port, node.extNode, channels, row.number + offset);
							} else
							{
								// add to channel above
								addPortToChannel(port, node.extNode, channels, row.number + 1);
							}
						}
						port.flags |= ROUTESEEN;
					}

					// for ports only up
					else if ((direct & GetNetlist.PORTDIRUP) != 0)
					{
						// add to channel above
						addPortToChannel(port, node.extNode, channels, row.number + 1);
						port.flags |= ROUTESEEN;
					}

					// for ports only down
					else if ((direct & GetNetlist.PORTDIRDOWN) != 0)
					{
						// add to channel below
						addPortToChannel(port, node.extNode, channels, row.number);
						port.flags |= ROUTESEEN;
					}

					// ports left
					else if ((direct & GetNetlist.PORTDIRLEFT) != 0)
					{
						addLateralFeed(port, channels, portsAbove, portsBelow, cell);
					}

					// ports right
					else if ((direct & GetNetlist.PORTDIRRIGHT) != 0)
					{
						addLateralFeed(port, channels, portsAbove, portsBelow, cell);
					} else
					{
						System.out.println("ERROR - no direction for " + port.place.cell.name + " port " +
							((PortProto)port.port.port).getName());
						port.flags |= ROUTESEEN;
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
	 * @param rowNum row number of port.
	 * @param cell pointer to parent cell.
	 * @return offset of row of closest port.
	 */
	private double nearestPort(RoutePort port, RouteNode node, int rowNum, GetNetlist.SCCell cell)
	{
		double minDist = Double.MAX_VALUE;
		double whichRow = 0;
		double xPos1;
		if ((rowNum % 2) != 0)
		{
			xPos1 = port.place.xPos + port.place.cell.size -
				port.port.xPos;
		} else
		{
			xPos1 = port.place.xPos + port.port.xPos;
		}

		// find closest above
		double offset = 0;
		for (RouteNode nNode = node.sameNext; nNode != null; nNode = nNode.sameNext)
		{
			offset++;
			for (RoutePort nPort = nNode.firstPort; nPort != null; nPort = nPort.next)
			{
				double dist = Math.abs(offset) * cell.placement.avgHeight * 2;
				double xPos2;
				if (((rowNum + offset) % 2) != 0)
				{
					xPos2 = nPort.place.xPos + nPort.place.cell.size - nPort.port.xPos;
				} else
				{
					xPos2 = nPort.place.xPos + nPort.port.xPos;
				}
				dist += Math.abs(xPos2 - xPos1);
				if (dist < minDist)
				{
					minDist = dist;
					whichRow = offset;
				}
			}
		}

		// check below
		offset = 0;
		for (RouteNode nNode = node.sameLast; nNode != null; nNode = nNode.sameLast)
		{
			offset--;
			for (RoutePort nPort = nNode.firstPort; nPort != null; nPort = nPort.next)
			{
				double dist = Math.abs(offset) * cell.placement.avgHeight * 2;
				double xPos2;
				if (((rowNum + offset) % 2) != 0)
				{
					xPos2 = nPort.place.xPos + nPort.place.cell.size - nPort.port.xPos;
				} else
				{
					xPos2 = nPort.place.xPos + nPort.port.xPos;
				}
				dist += Math.abs(xPos2 - xPos1);
				if (dist < minDist)
				{
					minDist = dist;
					whichRow = offset;
				}
			}
		}

		return whichRow;
	}

	/**
	 * Method to add the indicated port to the indicated channel.
	 * Create node for that channel if it doesn't already exist.
	 * @param port pointer to route port.
	 * @param extNode value of reference extracted node.
	 * @param channels start of channel list.
	 * @param chanNum number of wanted channel.
	 */
	private void addPortToChannel(RoutePort port, GetNetlist.ExtNode extNode, RouteChannel channels, int chanNum)
	{
		// get correct channel
		RouteChannel channel = channels;
		for (int i = 0; i < chanNum; i++)
			channel = channel.next;

		// check if node already exists for this channel
		RouteChNode node;
		for (node = channel.nodes; node != null; node = node.next)
		{
			if (node.extNode == extNode) break;
		}
		if (node == null)
		{
			node = new RouteChNode();
			node.extNode = extNode;
			node.number = 0;
			node.firstPort = null;
			node.lastPort = null;
			node.channel = channel;
			node.flags = ROUTESEEN;
			node.sameNext = null;
			node.sameLast = null;
			node.next = channel.nodes;
			channel.nodes = node;

			// resolve any references to other channels
			// check previous channels
			for (RouteChannel nchan = channel.last; nchan != null; nchan = nchan.last)
			{
				RouteChNode nNode;
				for (nNode = nchan.nodes; nNode != null; nNode = nNode.next)
				{
					if (nNode.extNode == extNode)
					{
						nNode.sameNext = node;
						node.sameLast = nNode;
						break;
					}
				}
				if (nNode != null) break;
			}

			// check later channels
			for (RouteChannel nchan = channel.next; nchan != null; nchan = nchan.next)
			{
				RouteChNode nNode;
				for (nNode = nchan.nodes; nNode != null; nNode = nNode.next)
				{
					if (nNode.extNode == extNode)
					{
						nNode.sameLast = node;
						node.sameNext = nNode;
						break;
					}
				}
				if (nNode != null) break;
			}
		}

		// add port to node
		RouteChPort nPort = new RouteChPort();
		nPort.port = port;
		nPort.node = node;
		nPort.xPos = 0;
		nPort.flags = 0;
		nPort.next = null;
		nPort.last = node.lastPort;
		if (node.lastPort != null)
		{
			node.lastPort.next = nPort;
		} else
		{
			node.firstPort = nPort;
		}
		node.lastPort = nPort;
	}

	/**
	 * Method to add a lateral feed for the port indicated.
	 * Add a "stitch" if port of same type adjecent, else add full lateral feed.
	 * Add to appropriate channel(s) if full feed.
	 * @param port pointer to port in question.
	 * @param channels list of channels.
	 * @param portsAbove true if ports above.
	 * @param portsBelow true if ports below.
	 * @param cell pointer to parent cell.
	 */
	private void addLateralFeed(RoutePort port, RouteChannel channels,
		boolean portsAbove, boolean portsBelow, GetNetlist.SCCell cell)
	{
		int direct = GetNetlist.getLeafPortDirection((PortProto)port.port.port);

		// determine if stitch
		Place.NBPlace nPlace = null;
		int sDirect = 0;
		if ((direct & GetNetlist.PORTDIRLEFT) != 0)
		{
			if ((port.node.row.number % 2) != 0)
			{
				// odd row
				for (nPlace = port.place.next; nPlace != null; nPlace = nPlace.next)
				{
					if (nPlace.cell.type == GetNetlist.LEAFCELL) break;
				}
			} else
			{
				// even row
				for (nPlace = port.place.last; nPlace != null; nPlace = nPlace.last)
				{
					if (nPlace.cell.type == GetNetlist.LEAFCELL) break;
				}
			}
			sDirect = GetNetlist.PORTDIRRIGHT;
		} else
		{
			if ((port.node.row.number % 2) != 0)
			{
				// odd row
				for (nPlace = port.place.last; nPlace != null; nPlace = nPlace.last)
				{
					if (nPlace.cell.type == GetNetlist.LEAFCELL) break;
				}
			} else
			{
				// even row
				for (nPlace = port.place.next; nPlace != null; nPlace = nPlace.next)
				{
					if (nPlace.cell.type == GetNetlist.LEAFCELL) break;
				}
			}
			sDirect = GetNetlist.PORTDIRLEFT;
		}
		if (nPlace != null)
		{
			// search for same port with correct direction
			RoutePort port2;
			for (port2 = port.next; port2 != null; port2 = port2.next)
			{
				if (port2.place == nPlace &&
					GetNetlist.getLeafPortDirection((PortProto)port2.port.port) == sDirect)
						break;
			}
			if (port2 != null)
			{
				// stitch feed
				port.flags |= ROUTESEEN;
				port2.flags |= ROUTESEEN;
				Place.NBPlace sPlace = new Place.NBPlace();
				sPlace.cell = null;
				GetNetlist.SCNiTree sInst = new GetNetlist.SCNiTree("Stitch", GetNetlist.STITCH);
				sPlace.cell = sInst;

				// save two ports
				GetNetlist.SCNiPort sPort = new GetNetlist.SCNiPort(sInst);
				sPort.port = port;
				GetNetlist.SCNiPort sPort2 = new GetNetlist.SCNiPort(sInst);
				sPort2.port = port2;

				// insert in place
				if ((direct & GetNetlist.PORTDIRLEFT) != 0)
				{
					if ((port.node.row.number % 2) != 0)
					{
						sPlace.last = port.place;
						sPlace.next = port.place.next;
						if (sPlace.last != null) sPlace.last.next = sPlace;
						if (sPlace.next != null) sPlace.next.last = sPlace;
					} else
					{
						sPlace.last = port.place.last;
						sPlace.next = port.place;
						if (sPlace.last != null) sPlace.last.next = sPlace;
						if (sPlace.next != null) sPlace.next.last = sPlace;
					}
				} else
				{
					if ((port.node.row.number % 2) != 0)
					{
						sPlace.last = port.place.last;
						sPlace.next = port.place;
						if (sPlace.last != null) sPlace.last.next = sPlace;
						if (sPlace.next != null) sPlace.next.last = sPlace;
					} else
					{
						sPlace.last = port.place;
						sPlace.next = port.place.next;
						if (sPlace.last != null) sPlace.last.next = sPlace;
						if (sPlace.next != null) sPlace.next.last = sPlace;
					}
				}
				return;
			}
		}

		// full lateral feed
		port.flags |= ROUTESEEN;
		Place.NBPlace sPlace = new Place.NBPlace();
		sPlace.cell = null;
		GetNetlist.SCNiTree sInst = new GetNetlist.SCNiTree("Lateral Feed", GetNetlist.LATERALFEED);
		sInst.size = SilComp.getFeedThruSize();
		sPlace.cell = sInst;

		// save port
		GetNetlist.SCNiPort sPort = new GetNetlist.SCNiPort(sInst);
		sPort.xPos = SilComp.getFeedThruSize() / 2;

		// create new route port
		RoutePort nPort = new RoutePort();
		nPort.place = port.place;
		nPort.port = port.port;
		nPort.node = port.node;
		nPort.flags = 0;
		nPort.last = null;
		nPort.next = null;
		sPort.port = nPort;

		// insert in place
		if ((direct & GetNetlist.PORTDIRLEFT) != 0)
		{
			if ((port.node.row.number % 2) != 0)
			{
				sPlace.last = port.place;
				sPlace.next = port.place.next;
			} else
			{
				sPlace.last = port.place.last;
				sPlace.next = port.place;
			}
		} else
		{
			if ((port.node.row.number % 2) != 0)
			{
				sPlace.last = port.place.last;
				sPlace.next = port.place;
			} else
			{
				sPlace.last = port.place;
				sPlace.next = port.place.next;
			}
		}
		if (sPlace.last != null)
		{
			sPlace.last.next = sPlace;
		} else
		{
			port.node.row.row.start = sPlace;
		}
		if (sPlace.next != null)
		{
			sPlace.next.last = sPlace;
		} else
		{
			port.node.row.row.end = sPlace;
		}
		resolveNewXPos(sPlace, port.node.row.row);

		// change route port to lateral feed
		port.place = sPlace;
		port.port = sPort;

		// channel assignment of lateral feed
		if (!portsAbove)
		{
			// add to channel below
			addPortToChannel(port, port.node.extNode, channels, port.node.row.number);
		} else
		{
			if (portsBelow)
			{
				// add to channel where closest
				int offset = 0;
				if (nearestPort(port, port.node, port.node.row.number, cell) > 0)
				{
					offset = 1;
				}
				addPortToChannel(port, port.node.extNode, channels, port.node.row.number + offset);
			} else
			{
				// add to channel above
				addPortToChannel(port, port.node.extNode, channels, port.node.row.number + 1);
			}
		}
	}

	/**
	 * Method to create pass throughs required to join electrically equivalent nodes
	 * in different channels.
	 * @param channels pointer to current channels.
	 * @param rows pointer to placed rows.
	 */
	private void createPassThroughs(RouteChannel channels, List<Place.RowList> theRows)
	{
		feedNumber = 0;

		// clear the flag on all channel nodes
		for (RouteChannel chan = channels; chan != null; chan = chan.next)
		{
			for (RouteChNode chNode = chan.nodes; chNode != null; chNode = chNode.next)
				chNode.flags &= ~ROUTESEEN;
		}

		// find all nodes which exist in more than one channel
		for (RouteChannel chan = channels; chan != null; chan = chan.next)
		{
			for (RouteChNode chNode = chan.nodes; chNode != null; chNode = chNode.next)
			{
				if ((chNode.flags & ROUTESEEN) != 0) continue;
				chNode.flags |= ROUTESEEN;
				RouteChNode oldChNode = chNode;
				for (RouteChNode chNode2 = chNode.sameNext; chNode2 != null; chNode2 = chNode2.sameNext)
				{
					chNode2.flags |= ROUTESEEN;
					betweenChNodes(oldChNode, chNode2, channels, theRows);
					oldChNode = chNode2;
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
	private void betweenChNodes(RouteChNode node1, RouteChNode node2, RouteChannel channels, List<Place.RowList> theRows)
	{
		GetNetlist.ExtNode extNode = node1.extNode;

		// determine limits of second channel
		double minX2 = minPortPos(node2);
		double maxX2 = maxPortPos(node2);

		// do for all intervening channels
		for (RouteChannel chan = node1.channel; chan != node2.channel; chan = chan.next,
			node1 = node1.sameNext)
		{
			// determine limits of first channel node
			double minX1 = minPortPos(node1);
			double maxX1 = maxPortPos(node1);

			// determine preferred region of pass through
			double pMinX, pMaxX;
			if (maxX1 <= minX2)
			{
				// no overlap with first node to left
				pMinX = maxX1;
				pMaxX = minX2;
			} else if (maxX2 <= minX1)
			{
				// no overlap with first node to right
				pMinX = maxX2;
				pMaxX = minX1;
			} else
			{
				// have some overlap
				pMinX = Math.max(minX1, minX2);
				pMaxX = Math.min(minX1, minX2);
			}

			// set window fuzzy limits
			pMinX -= DEFAULT_FUZZY_WINDOW_LIMIT;
			pMaxX += DEFAULT_FUZZY_WINDOW_LIMIT;

			// determine which row we are in
			Place.RowList row = null;
			for(Place.RowList r : theRows)
			{
				row = r;
				if (row.rowNum == chan.number) break;
			}

			// check for any possible ports which can be used
			RouteNode rNode;
			for (rNode = (RouteNode)extNode.ptr; rNode != null; rNode = rNode.sameNext)
			{
				if (rNode.row.number == row.rowNum) break;
			}
			if (rNode != null)
			{
				// port of correct type exists somewhere in this row
				RoutePort rPort;
				for (rPort = rNode.firstPort; rPort != null; rPort = rPort.next)
				{
					double pos = portPosition(rPort);
					int direct = GetNetlist.getLeafPortDirection((PortProto)rPort.port.port);
					if ((direct & GetNetlist.PORTDIRUP) == 0 && (direct & GetNetlist.PORTDIRDOWN) != 0) continue;
					if (pos >= pMinX && pos <= pMaxX) break;
				}
				if (rPort != null)
				{
					// found suitable port, ensure it exists in both channels
					RouteChPort chPort = null;
					for (RouteChNode node = chan.nodes; node != null; node = node.next)
					{
						if (node.extNode == node1.extNode)
						{
							for (chPort = node.firstPort; chPort != null; chPort = chPort.next)
							{
								if (chPort.port == rPort) break;
							}
						}
					}
					if (chPort == null)
					{
						// add port to this channel
						addPortToChannel(rPort, extNode, channels, chan.number);
					}
					chPort = null;
					for (RouteChNode node = chan.next.nodes; node != null; node = node.next)
					{
						if (node.extNode == node1.extNode)
						{
							for (chPort = node.firstPort; chPort != null; chPort = chPort.next)
							{
								if (chPort.port == rPort) break;
							}
						}
					}
					if (chPort == null)
					{
						// add port to next channel
						addPortToChannel(rPort, extNode, channels, chan.next.number);
					}
					continue;
				}
			}

			// if no port found, find best position for feed through
			double bestPos = Double.MAX_VALUE;
			Place.NBPlace bestPlace = null;
			for (Place.NBPlace place = row.start; place != null; place = place.next)
			{
				// not allowed to feed at stitch
				if (place.cell.type == GetNetlist.STITCH ||
					(place.last != null && place.last.cell.type == GetNetlist.STITCH))
						continue;

				// not allowed to feed at lateral feed
				if (place.cell.type == GetNetlist.LATERALFEED ||
					(place.last != null && place.last.cell.type == GetNetlist.LATERALFEED))
						continue;
				if (place.xPos >= pMinX && place.xPos <= pMaxX)
				{
					bestPlace = place;
					break;
				}
				double pos;
				if (place.xPos < pMinX)
				{
					pos = Math.abs(pMinX - place.xPos);
				} else
				{
					pos = Math.abs(pMaxX - place.xPos);
				}
				if (pos < bestPos)
				{
					bestPos = pos;
					bestPlace = place;
				}
			}

			// insert feed through at the indicated place
			insertFeedThrough(bestPlace, row, channels, chan.number, node1);
		}
	}

	/**
	 * Method to return the position of the port which is farthest left (minimum).
	 * @param node pointer to channel node.
	 * @return leftmost port position.
	 */
	private double minPortPos(RouteChNode node)
	{
		double minX = Double.MAX_VALUE;
		for (RouteChPort chPort = node.firstPort; chPort != null; chPort = chPort.next)
		{
			// determine position
			double pos = portPosition(chPort.port);

			// check versus minimum
			if (pos < minX) minX = pos;
		}

		return minX;
	}

	/**
	 * Method to return the position of the port which is farthest right (maximum).
	 * @param node pointer to channel node.
	 * @return rightmost port position.
	 */
	private double maxPortPos(RouteChNode node)
	{
		double maxX = Double.MIN_VALUE;
		for (RouteChPort chPort = node.firstPort; chPort != null; chPort = chPort.next)
		{
			// determine position
			double pos = portPosition(chPort.port);

			// check versus maximum
			if (pos > maxX) maxX = pos;
		}

		return maxX;
	}

	/**
	 * Method to return the x position of the indicated port.
	 * @param port pointer to port in question.
	 * @return x position.
	 */
	private double portPosition(RoutePort port)
	{
		double pos = port.place.xPos;
		if ((port.node.row.number % 2) != 0)
		{
			pos += port.place.cell.size - port.port.xPos;
		} else
		{
			pos += port.port.xPos;
		}

		return pos;
	}

	/**
	 * Method to insert a feed through in front of the indicated place.
	 * @param place place where to insert in front of.
	 * @param row row of place.
	 * @param channels channel list.
	 * @param chanNum number of particular channel below.
	 * @param node channel node within the channel.
	 */
	private void insertFeedThrough(Place.NBPlace place, Place.RowList row,
		RouteChannel channels, int chanNum, RouteChNode node)
	{
		// create a special instance
		GetNetlist.SCNiTree inst = new GetNetlist.SCNiTree("Feed_Through", GetNetlist.FEEDCELL);
		inst.size = SilComp.getFeedThruSize();

		// create instance port
		GetNetlist.SCNiPort port = new GetNetlist.SCNiPort();
		port.port = new Integer(feedNumber++);
		port.extNode = node.extNode;
		port.bits = 0;
		port.xPos = SilComp.getFeedThruSize() / 2;
		port.next = null;
		inst.ports = port;

		// create the appropriate place
		Place.NBPlace nPlace = new Place.NBPlace();
		nPlace.cell = inst;
		nPlace.last = place.last;
		nPlace.next = place;
		if (nPlace.last != null)
			nPlace.last.next = nPlace;
		place.last = nPlace;
		if (place == row.start)
			row.start = nPlace;

		resolveNewXPos(nPlace, row);

		// create a route port entry for this new port
		RouteNode rNode;
		for (rNode = (RouteNode)node.extNode.ptr; rNode != null; rNode = rNode.sameNext)
		{
			if (rNode.row.number == row.rowNum) break;
		}
		RoutePort rPort = new RoutePort();
		rPort.place = nPlace;
		rPort.port = port;
		rPort.node = rNode;
		rPort.flags = 0;
		rPort.next = null;
		rPort.last = rNode.lastPort;
		if (rNode.lastPort != null)
		{
			rNode.lastPort.next = rPort;
		} else
		{
			rNode.firstPort = rPort;
		}
		rNode.lastPort = rPort;

		// add to channels
		addPortToChannel(rPort, node.extNode, channels, chanNum);
		addPortToChannel(rPort, node.extNode, channels, chanNum + 1);
	}

	/**
	 * Method to resolve the position of the new place and update the row.
	 * @param place new place.
	 * @param row pointer to existing row.
	 */
	private void resolveNewXPos(Place.NBPlace place, Place.RowList row)
	{
		double xPos;
		if (place.last != null)
		{
			if (place.last.cell.type == GetNetlist.LEAFCELL)
			{
				GetNetlist.SCCellNums cnums = GetNetlist.getLeafCellNums((Cell)place.last.cell.np);
				xPos = place.last.xPos + place.last.cell.size;
				double overlap = 0;
				if ((row.rowNum % 2) != 0)
				{
					// odd row, cells are transposed
					overlap = cnums.leftActive - SilComp.getMinActiveDistance();
				} else
				{
					// even row
					overlap = cnums.rightActive - SilComp.getMinActiveDistance();
				}
				if (overlap < 0 && place.cell.type != GetNetlist.LATERALFEED)
					overlap = 0;
				xPos -= overlap;
				place.xPos = xPos;
				xPos += place.cell.size;
			} else
			{
				xPos = place.last.xPos + place.last.cell.size;
				place.xPos = xPos;
				xPos += place.cell.size;
			}
		} else
		{
			place.xPos = 0;
			xPos = place.cell.size;
		}

		if (place.next != null)
		{
			double oldXPos = place.next.xPos;
			double nXPos = 0;
			if (place.next.cell.type == GetNetlist.LEAFCELL)
			{
				GetNetlist.SCCellNums cnums = GetNetlist.getLeafCellNums((Cell)place.next.cell.np);
				double overlap = 0;
				if ((row.rowNum % 2) != 0)
				{
					// odd row, cells are transposed
					overlap = cnums.rightActive - SilComp.getMinActiveDistance();
				} else
				{
					// even row
					overlap = cnums.leftActive - SilComp.getMinActiveDistance();
				}
				if (overlap < 0 && place.cell.type != GetNetlist.LATERALFEED)
					overlap = 0;
				nXPos = xPos - overlap;
			} else
			{
				nXPos = xPos;
			}

			// update rest of the row
			for (place = place.next; place != null; place = place.next)
				place.xPos += nXPos - oldXPos;
			row.rowSize += nXPos - oldXPos;
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
	private RouteExport decideExports(GetNetlist.SCCell cell)
	{
		RouteExport lExport = null;

		// check all exports
		for (GetNetlist.SCPort port = cell.ports; port != null; port = port.next)
		{
			// get extracted node
			GetNetlist.ExtNode eNode = port.node.ports.extNode;

			RouteChNode chNode = null;
			for (RouteChannel chan = cell.route.channels; chan != null; chan = chan.next)
			{
				for (chNode = chan.nodes; chNode != null; chNode = chNode.next)
				{
					if (chNode.extNode == eNode) break;
				}
				if (chNode != null) break;
			}

			// find limits of channel node
			boolean bottom = false, top = false, left = false, right = false;
			double bestDist = Double.MAX_VALUE;
			RouteChPort bestChPort = null;
			for (RouteChNode chNode2 = chNode; chNode2 != null; chNode2 = chNode2.sameNext)
			{
				RouteChPort chPort;
				for (chPort = chNode2.firstPort; chPort != null; chPort = chPort.next)
				{
					// check for bottom channel
					if (chPort.node.channel.number == 0)
					{
						bottom = true;
						bestChPort = chPort;
						break;
					}

					// check for top channel
					if (chPort.node.channel.number == cell.placement.numRows)
					{
						top = true;
						bestChPort = chPort;
						break;
					}

					// check distance to left boundary
					double dist = portPosition(chPort.port);
					if (dist < bestDist)
					{
						bestDist = dist;
						left = true;
						right = false;
						bestChPort = chPort;
					}

					// check distance to right boundary
					double maxX = chPort.port.node.row.row.end.xPos +
					chPort.port.node.row.row.end.cell.size;
					dist = maxX - portPosition(chPort.port);
					if (dist < bestDist)
					{
						bestDist = dist;
						right = true;
						left = false;
						bestChPort = chPort;
					}
				}
				if (chPort != null) break;
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
				bestChPort = createSpecial(port.node, bestChPort, false, cell);
			} else if (left)
			{
				// create special place for export at start of row
				bestChPort = createSpecial(port.node, bestChPort, true, cell);
			}

			// add port to export list
			RouteExport nExport = new RouteExport();
			nExport.xPort = port;
			nExport.chPort = bestChPort;
			nExport.next = lExport;
			lExport = nExport;
		}

		return lExport;
	}

	/**
	 * Method to create a special place on either the start or end of the row where
	 * the passed channel port real port resides.
	 * @param inst instance for place to point to.
	 * @param chPort channel port in question.
	 * @param w true at start, false at end.
	 * @param cell parent cell.
	 * @return newly created channel port.
	 */
	private RouteChPort createSpecial(GetNetlist.SCNiTree inst, RouteChPort chPort, boolean where, GetNetlist.SCCell cell)
	{
		inst.size = SilComp.getFeedThruSize();
		inst.ports.xPos = SilComp.getFeedThruSize() / 2;

		// find row
		Place.RowList row = chPort.port.node.row.row;

		// create appropriate place
		Place.NBPlace nPlace = new Place.NBPlace();
		nPlace.cell = inst;
		if (where)
		{
			if (row.start != null)
			{
				double xpos = row.start.xPos - SilComp.getFeedThruSize();
				nPlace.xPos = xpos;
				row.start.last = nPlace;
			} else
			{
				nPlace.xPos = 0;
			}
			nPlace.last = null;
			nPlace.next = row.start;
			row.start = nPlace;
		} else
		{
			if (row.end != null)
			{
				nPlace.xPos = row.end.xPos + row.end.cell.size;
				row.end.next = nPlace;
			} else
			{
				nPlace.xPos = 0;
			}
			nPlace.next = null;
			nPlace.last = row.end;
			row.end = nPlace;
		}

		// create a route port entry for this new port
		RouteNode rNode;
		for (rNode = (RouteNode)chPort.node.extNode.ptr; rNode != null; rNode = rNode.sameNext)
		{
			if (rNode.row.number == row.rowNum) break;
		}
		RoutePort rPort = new RoutePort();
		rPort.place = nPlace;
		rPort.port = inst.ports;
		rPort.node = rNode;
		rPort.flags = 0;
		rPort.next = null;
		rPort.last = rNode.lastPort;
		if (rNode.lastPort != null)
		{
			rNode.lastPort.next = rPort;
		} else
		{
			rNode.firstPort = rPort;
		}
		rNode.lastPort = rPort;

		// add to channel
		addPortToChannel(rPort, chPort.port.node.extNode,
			cell.route.channels, chPort.node.channel.number);

		return chPort.node.lastPort;
	}

	/**
	 * Method to route the tracks in each channel by using an improved channel router.
	 * @param channels list of all channels.
	 * @param cell pointer to parent cell.
	 */
	private void tracksInChannels(RouteChannel channels, GetNetlist.SCCell cell)
	{
		// do for each channel individually
		for (RouteChannel chan = channels; chan != null; chan = chan.next)
		{
			if (DEBUG)
				System.out.println("**** Routing tracks for Channel " + chan.number+ " ****");

			// create Vertical Constraint Graph (VCG)
			RouteVCG vGraph = createVCG(chan, cell);

			// create Zone Representation Graph (ZRG)
			RouteZRG zrGraph = createZRG(chan);

			// do track assignment
			RouteTrack tracks = trackAssignment(vGraph, zrGraph, chan.nodes);

			chan.tracks = tracks;
		}
	}

	/**
	 * Method to create the Vertical Constrain Graph (VCG) for the indicated channel.
	 * @param channel pointer to channel.
	 * @param cell pointer to parent cell.
	 * @return where to write created VCG.
	 */
	private RouteVCG createVCG(RouteChannel channel, GetNetlist.SCCell cell)
	{
		// first number channel nodes to represent nets
		int netNumber = 0;
		for (RouteChNode chNode = channel.nodes; chNode != null; chNode = chNode.next)
		{
			chNode.number = netNumber++;

			// calculate actual port position
			for (RouteChPort chPort = chNode.firstPort; chPort != null; chPort = chPort.next)
				chPort.xPos = portPosition(chPort.port);

			// sort all channel ports on node from leftmost to rightmost
			for (RouteChPort chPort = chNode.firstPort; chPort != null; chPort = chPort.next)
			{
				// bubble port left if necessay
				for (RouteChPort port2 = chPort.last; port2 != null; port2 = chPort.last)
				{
					if (port2.xPos <= chPort.xPos) break;

					// move chport left
					chPort.last = port2.last;
					port2.last = chPort;
					if (chPort.last != null)
						chPort.last.next = chPort;
					port2.next = chPort.next;
					chPort.next = port2;
					if (port2.next != null)
						port2.next.last = port2;
					if (port2 == chNode.firstPort)
						chNode.firstPort = chPort;
					if (chPort == chNode.lastPort)
						chNode.lastPort = port2;
				}
			}
		}

		// create the VCG root node
		RouteVCG vcgRoot = new RouteVCG();
		vcgRoot.chNode = null;
		vcgRoot.edges = null;

		// create a VCG node for each channel node (or net)
		for (RouteChNode chNode = channel.nodes; chNode != null; chNode = chNode.next)
		{
			RouteVCG vcgNode = new RouteVCG();
			vcgNode.chNode = chNode;
			vcgNode.edges = null;
			RouteVCGEdge vcgEdge = new RouteVCGEdge();
			vcgEdge.node = vcgNode;
			vcgEdge.next = vcgRoot.edges;
			vcgRoot.edges = vcgEdge;
		}

		vcgCreateDependents(vcgRoot, channel);

		// add any ports in this channel tied to power
		createPowerTies(channel, vcgRoot, cell);

		// add any ports in this channel tied to ground
		createGroundTies(channel, vcgRoot, cell);

		// remove all dependent nodes from root of constraint graph*/
		// clear seen flag
		for (RouteVCGEdge vcgEdge = vcgRoot.edges; vcgEdge != null; vcgEdge = vcgEdge.next)
			vcgEdge.node.flags &= ~ROUTESEEN;

		// mark all VCG nodes that are called by others
		for (RouteVCGEdge vcgEdge = vcgRoot.edges; vcgEdge != null; vcgEdge = vcgEdge.next)
		{
			for (RouteVCGEdge edge1 = vcgEdge.node.edges; edge1 != null; edge1 = edge1.next)
				edge1.node.flags |= ROUTESEEN;
		}

		// remove all edges from root which are marked
		RouteVCGEdge edge1 = vcgRoot.edges;
		for (RouteVCGEdge vcgEdge = vcgRoot.edges; vcgEdge != null; vcgEdge = vcgEdge.next)
		{
			if ((vcgEdge.node.flags & ROUTESEEN) != 0)
			{
				if (vcgEdge == vcgRoot.edges)
				{
					vcgRoot.edges = vcgEdge.next;
					edge1 = vcgEdge.next;
				} else
				{
					edge1.next = vcgEdge.next;
				}
			} else
			{
				edge1 = vcgEdge;
			}
		}

		// print out Vertical Constraint Graph if verbose flag set
		if (DEBUG)
		{
			System.out.println("************ VERTICAL CONSTRAINT GRAPH");
			for (edge1 = vcgRoot.edges; edge1 != null; edge1 = edge1.next)
			{
				System.out.println("Net " + edge1.node.chNode.number + ":");
				printVCG(edge1.node.edges, 1);
			}
		}

		return vcgRoot;
	}

	/**
	 * Method to resolve any cyclic dependencies in the Vertical Constraint Graph.
	 * @param vcgRoot pointer to root of VCG.
	 * @param channel pointer to particular channel.
	 */
	private void vcgCreateDependents(RouteVCG vcgRoot, RouteChannel channel)
	{
		boolean check = true;
		while (check)
		{
			check = false;
			vcgSetDependents(vcgRoot);
			GenMath.MutableInteger found = new GenMath.MutableInteger(0);
			GenMath.MutableDouble diff = new GenMath.MutableDouble(0);
			Place.NBPlace place = vcgCyclicCheck(vcgRoot, diff, found);
			if (found.intValue() != 0)
			{
				check = true;

				// move place and update row
				for (Place.NBPlace place2 = place; place2 != null; place2 = place2.next)
					place2.xPos += diff.doubleValue();

				// update channel port positions
				for (RouteChNode chNode = channel.nodes; chNode != null; chNode = chNode.next)
				{
					// calculate actual port position
					for (RouteChPort chPort = chNode.firstPort; chPort != null; chPort = chPort.next)
						chPort.xPos = portPosition(chPort.port);

					// reorder port positions from left to right
					for (RouteChPort chPort = chNode.firstPort; chPort != null; chPort = chPort.next)
					{
						for (RouteChPort port2 = chPort.last; port2 != null; port2 = chPort.last)
						{
							if (port2.xPos <= chPort.xPos) break;

							// move chPort left
							chPort.last = port2.last;
							port2.last = chPort;
							if (chPort.last != null)
								chPort.last.next = chPort;
							port2.next = chPort.next;
							chPort.next = port2;
							if (port2.next != null)
								port2.next.last = port2;
							if (port2 == chNode.firstPort)
								chNode.firstPort = chPort;
							if (chPort == chNode.lastPort)
								chNode.lastPort = port2;
						}
					}
				}
			}
		}
	}

	/**
	 * Method to create a directed edge if one channel node must be routed before another.
	 * @param vcgRoot root of Vertical Constraint Graph.
	 */
	private void vcgSetDependents(RouteVCG vcgRoot)
	{
		// clear all dependencies
		for (RouteVCGEdge edge1 = vcgRoot.edges; edge1 != null; edge1 = edge1.next)
			edge1.node.edges = null;

		// set all dependencies
		for (RouteVCGEdge edge1 = vcgRoot.edges; edge1 != null; edge1 = edge1.next)
		{
			for (RouteVCGEdge edge2 = edge1.next; edge2 != null; edge2 = edge2.next)
			{
				// Given two channel nodes, create a directed edge if
				// one must be routed before the other
				boolean depend1 = false, depend2 = false;
				for (RouteChPort port1 = edge1.node.chNode.firstPort; port1 != null; port1 = port1.next)
				{
					for (RouteChPort port2 = edge2.node.chNode.firstPort; port2 != null; port2 = port2.next)
					{
						if (Math.abs(port1.xPos - port2.xPos) < SilComp.getMinPortDistance())
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
					RouteVCGEdge vcgEdge = new RouteVCGEdge();
					vcgEdge.node = edge2.node;
					vcgEdge.next = edge1.node.edges;
					edge1.node.edges = vcgEdge;
				}
				if (depend2)
				{
					RouteVCGEdge vcgEdge = new RouteVCGEdge();
					vcgEdge.node = edge1.node;
					vcgEdge.next = edge2.node.edges;
					edge2.node.edges = vcgEdge;
				}
			}
		}
	}

	/**
	 * Method to return TRUE if cyclic dependency is found in Vertical Constraint Graph.
	 * Also set place and offset needed to resolve this conflict.
	 * Note that only the top row may be moved around as the bottom row
	 * may have already been used by another channel.
	 * @param vcgRoot root of Vertical Constraint Graph.
	 * @param diff offset required is stored here.
	 * @return pointer to place.
	 */
	private Place.NBPlace vcgCyclicCheck(RouteVCG vcgRoot, GenMath.MutableDouble diff, GenMath.MutableInteger found)
	{
		// check each VCG node
		Place.NBPlace place = null;
		for (RouteVCGEdge edge = vcgRoot.edges; edge != null; edge = edge.next)
		{
			// clear all flags
			for (RouteVCGEdge edge3 = vcgRoot.edges; edge3 != null; edge3 = edge3.next)
			{
				edge3.node.flags &= ~(ROUTESEEN | ROUTETEMPNUSE);
			}

			// mark this node
			edge.node.flags |= ROUTESEEN;

			// check single cycle
			for (RouteVCGEdge edge2 = edge.node.edges; edge2 != null; edge2 = edge2.next)
			{
				RouteVCG lastNode = edge.node;
				GenMath.MutableInteger subFound = new GenMath.MutableInteger(0);
				lastNode = vcgSingleCycle(edge2.node, lastNode, subFound);
				if (subFound.intValue() != 0)
				{
					// find place of conflict
					for (RouteChPort port1 = edge.node.chNode.firstPort; port1 != null; port1 = port1.next)
					{
						for (RouteChPort port2 = lastNode.chNode.firstPort; port2 != null; port2 = port2.next)
						{
							if (Math.abs(port1.xPos - port2.xPos) < SilComp.getMinPortDistance())
							{
								// determine which one goes first
								if (port1.port.node.row.number > port2.port.node.row.number)
								{
									place = port1.port.place;
									if (port1.xPos < port2.xPos)
									{
										diff.setValue((port2.xPos - port1.xPos) + SilComp.getMinPortDistance());
									} else
									{
										diff.setValue(SilComp.getMinPortDistance() - (port1.xPos - port2.xPos));
									}
								} else if (port2.port.node.row.number > port1.port.node.row.number)
								{
									place = port2.port.place;
									if (port2.xPos < port1.xPos)
									{
										diff.setValue((port1.xPos - port2.xPos) + SilComp.getMinPortDistance());
									} else
									{
										diff.setValue(SilComp.getMinPortDistance() - (port2.xPos - port1.xPos));
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
	 * @param lastNode last node searched.
	 * @param found MutableInteger to hold result: nonzero if marked node found.
	 * @return the last node searched.
	 */
	private RouteVCG vcgSingleCycle(RouteVCG node, RouteVCG lastNode, GenMath.MutableInteger found)
	{
		if (node == null)
		{
			found.setValue(0);
			return lastNode;
		}
		if ((node.flags & ROUTESEEN) != 0)
		{
			// marked node found
			found.setValue(1);
			return lastNode;
		}
		if ((node.flags & ROUTETEMPNUSE) != 0)
		{
			// been here before
			found.setValue(0);
			return lastNode;
		} else
		{
			// check others
			node.flags |= ROUTETEMPNUSE;
			RouteVCG saveNode = lastNode;
			for (RouteVCGEdge edge = node.edges; edge != null; edge = edge.next)
			{
				lastNode = node;
				lastNode = vcgSingleCycle(edge.node, lastNode, found);
				if (found.intValue() != 0) return lastNode;
			}
			lastNode = saveNode;
			found.setValue(0);
			return lastNode;
		}
	}

	/**
	 * Method to create the Zone Representation Graph (ZRG) for the indicated channel.
	 * @param channel pointer to channel.
	 * @return the created ZRG.
	 */
	private RouteZRG createZRG(RouteChannel channel)
	{
		RouteZRG firstZone = null, lastZone = null;
		int zNumber = 0;

		// create first zone
		RouteZRG zone = new RouteZRG();
		zone.number = zNumber++;
		zone.chNodes = null;
		zone.next = null;
		zone.last = null;
		firstZone = lastZone = zone;

		// clear flag on all channel nodes
		int numChNodes = 0;
		for (RouteChNode chNode = channel.nodes; chNode != null; chNode = chNode.next)
		{
			chNode.flags &= ~ROUTESEEN;
			numChNodes++;
		}

		// allocate enough space for channel node temporary list
		RouteChNode [] chNodeList = new RouteChNode[numChNodes+1];

		for(;;)
		{
			RouteChNode leftChNode = findLeftmostChNode(channel.nodes);
			if (leftChNode == null) break;
			createZRGTempList(channel.nodes, leftChNode.firstPort.xPos, chNodeList);
			if (zrgListCompatible(chNodeList, zone))
			{
				zrgAddChNodes(chNodeList, zone);
			} else
			{
				zone = new RouteZRG();
				zone.number = zNumber++;
				zone.chNodes = null;
				zone.next = null;
				zone.last = lastZone;
				lastZone.next = zone;
				lastZone = zone;
				zrgAddChNodes(chNodeList, zone);
			}
			leftChNode.flags |= ROUTESEEN;
		}

		// print out zone representation if verbose flag set
		if (DEBUG)
		{
			System.out.println("************ ZONE REPRESENTATION GRAPH");
			for (zone = firstZone; zone != null; zone = zone.next)
			{
				System.out.println("Zone " + zone.number + ":");
				for (RouteZRGMem mem = zone.chNodes; mem != null; mem = mem.next)
					System.out.println("    Node " + mem.chNode.number);
			}
		}
		return firstZone;
	}

	/**
	 * Method to return a pointer to the unmarked channel node of the indicated
	 * channel which has the left-most first port.
	 * If no channel nodes suitable found, return null.
	 * @param nodes pointer to a list of channel nodes.
	 * @return pointer to leftmost node, null if none unmarked found.
	 */
	private RouteChNode findLeftmostChNode(RouteChNode nodes)
	{
		RouteChNode leftChNode = null;
		double leftXPos = Double.MAX_VALUE;

		for (RouteChNode node = nodes; node != null; node = node.next)
		{
			if ((node.flags & ROUTESEEN) != 0) continue;
			if (node.firstPort.xPos < leftXPos)
			{
				leftXPos = node.firstPort.xPos;
				leftChNode = node;
			}
		}

		return leftChNode;
	}

	/**
	 * Method to fill in the temporary list of all channel nodes which encompass the indicated x position.
	 * @param nodes list of channel nodes.
	 * @param xPos X position of interest.
	 * @param list array of pointer to fill in, terminate with a null.
	 */
	private void createZRGTempList(RouteChNode nodes, double xPos, RouteChNode [] list)
	{
		int i = 0;
		for (RouteChNode node = nodes; node != null; node = node.next)
		{
			if (xPos > node.firstPort.xPos - SilComp.getMinPortDistance()
				&& xPos < node.lastPort.xPos + SilComp.getMinPortDistance())
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
	private boolean zrgListCompatible(RouteChNode [] list, RouteZRG zone)
	{
		if (zone.chNodes != null)
		{
			// check each member of current zone being in the list
			for (RouteZRGMem mem = zone.chNodes; mem != null; mem = mem.next)
			{
				int i;
				for (i = 0; list[i] != null; i++)
				{
					if (mem.chNode == list[i]) break;
				}
				if (list[i] == null) return false;
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
	private void zrgAddChNodes(RouteChNode [] list, RouteZRG zone)
	{
		for (int i = 0; list[i] != null; i++)
		{
			RouteZRGMem mem;
			for (mem = zone.chNodes; mem != null; mem = mem.next)
			{
				if (mem.chNode == list[i]) break;
			}
			if (mem == null)
			{
				mem = new RouteZRGMem();
				mem.chNode = list[i];
				mem.next = zone.chNodes;
				zone.chNodes = mem;
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
	private RouteTrack trackAssignment(RouteVCG vcg, RouteZRG zrg, RouteChNode nodes)
	{
		RouteTrack firstTrack = null, lastTrack = null;
		int trackNumber = 0;

		// create first track
		RouteTrack track = new RouteTrack();
		track.number = trackNumber++;
		track.nodes = null;
		track.last = null;
		track.next = null;
		firstTrack = lastTrack = track;

		// clear flags on all channel nodes
		int numberNodes = 0;
		for (RouteChNode node = nodes; node != null; node = node.next)
		{
			node.flags = 0;
			numberNodes++;
		}
		RouteChNode [] nList = new RouteChNode[numberNodes+1];

		// get channel node on longest path of VCG
		for(;;)
		{
			RouteChNode node = longestVCG(vcg);
			if (node == null) break;

			// clear flags of all nodes
			for (RouteChNode node2 = nodes; node2 != null; node2 = node2.next)
				node2.flags = 0;

			// add node to track
			addNodeToTrack(node, track);

			// mark all other nodes in the same zones as not usable
			markZones(node, zrg, ROUTEUNUSABLE);

			// find set of remaining nodes which can be added to track
			findBestNodes(vcg, zrg, nList, numberNodes + 1);

			// add to track
			for (int i = 0; nList[i] != null; i++)
			{
				addNodeToTrack(nList[i], track);
			}

			// delete track entries from VCG
			deleteFromVCG(track, vcg);

			// create next track
			track = new RouteTrack();
			track.number = trackNumber++;
			track.nodes = null;
			track.last = lastTrack;
			lastTrack.next = track;
			lastTrack = track;
		}

		// delete last track if empty
		if (track.nodes == null)
		{
			if (track.last != null)
			{
				track.last.next = null;
			} else
			{
				firstTrack = null;
			}
		}

		// print out track assignment if verbose flag set
		if (DEBUG)
		{
			System.out.println("************ TRACK ASSIGNMENT");
			for (track = firstTrack; track != null; track = track.next)
			{
				System.out.println("For Track #" + track.number + ":");
				for (RouteTrackMem mem = track.nodes; mem != null; mem = mem.next)
					System.out.println("    " + mem.node.number + "     " + mem.node.firstPort.xPos + "  " + mem.node.lastPort.xPos);
			}
		}

		return firstTrack;
	}

	/**
	 * Method to mark all channel nodes in the same zones as the indicated zone as indicated.
	 * @param node channel node in question.
	 * @param zrg zone representation graph.
	 * @param bits bits to OR in to nodes flag field.
	 */
	private void markZones(RouteChNode node, RouteZRG zrg, int bits)
	{
		// mark unusable nodes
		for (RouteZRG zone = zrg; zone != null; zone = zone.next)
		{
			RouteZRGMem zMem;
			for (zMem = zone.chNodes; zMem != null; zMem = zMem.next)
			{
				if (zMem.chNode == node) break;
			}
			if (zMem != null)
			{
				for (zMem = zone.chNodes; zMem != null; zMem = zMem.next)
					zMem.chNode.flags |= bits;
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
	private RouteChNode longestVCG(RouteVCG vcg)
	{
		RouteChNode node = null;
		double longestPath = 0;

		// check for all entries at the top level
		for (RouteVCGEdge edge = vcg.edges; edge != null; edge = edge.next)
		{
			double path = pathLength(edge.node);
			if (path > longestPath)
			{
				longestPath = path;
				node = edge.node.chNode;
			}
		}

		return node;
	}

	/**
	 * Method to return the length of the longest path starting at the indicated
	 * Vertical Constraint Graph Node.
	 * @param vcgNode vertical Constraint Graph node.
	 * @return longest path length.
	 */
	private double pathLength(RouteVCG vcgNode)
	{
		if (vcgNode.edges == null) return 1;

		// check path for all edges
		double longest = 0;
		for (RouteVCGEdge edge = vcgNode.edges; edge != null; edge = edge.next)
		{
			double path = pathLength(edge.node);
			if (path > longest) longest = path;
		}

		return longest + 1;
	}

	/**
	 * Method to add the indicated channel node to the track and mark as seen.
	 * Note add the node in left to right order.
	 * @param node pointer to channel node to add.
	 * @param track pointer to track to add to.
	 */
	private void addNodeToTrack(RouteChNode node, RouteTrack track)
	{
		RouteTrackMem mem = new RouteTrackMem();
		mem.node = node;
		mem.next = null;
		if (track.nodes == null)
		{
			track.nodes = mem;
		} else
		{
			RouteTrackMem oldMem = track.nodes;
			RouteTrackMem mem2;
			for (mem2 = track.nodes; mem2 != null; mem2 = mem2.next)
			{
				if (mem.node.firstPort.xPos > mem2.node.firstPort.xPos)
				{
					oldMem = mem2;
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
				oldMem.next = mem;
			}
		}

		node.flags |= ROUTESEEN;
	}

	/**
	 * Method to find the set of remaining nodes with no ancestors in the Vertical
	 * Constraint Graph which are available and are of maximum combined length.
	 * @param vcg vertical Constraint Graph.
	 * @param zrg zone Representation Graph.
	 * @param nList array to write list of selected nodes.
	 * @param num maximum size of nList.
	 */
	private void findBestNodes(RouteVCG vcg, RouteZRG zrg, RouteChNode [] nList, int num)
	{
		int i = 0;
		nList[i] = null;

		// try all combinations
		for(;;)
		{
			// find longest, usable edge
			RouteVCGEdge edge2 = null;
			double maxLength = 0;
			for (RouteVCGEdge edge = vcg.edges; edge != null; edge = edge.next)
			{
				if ((edge.node.chNode.flags & (ROUTESEEN | ROUTEUNUSABLE)) != 0) continue;
				double length = edge.node.chNode.lastPort.xPos - edge.node.chNode.firstPort.xPos;
				if (length >= maxLength)
				{
					maxLength = length;
					edge2 = edge;
				}
			}
			if (edge2 == null) break;

			// add to list
			nList[i++] = edge2.node.chNode;
			nList[i] = null;
			markZones(edge2.node.chNode, zrg, ROUTEUNUSABLE);
		}
	}

	/**
	 * Method to delete all channel nodes in the track from the top level of the
	 * Vertical Constraint Graph and update VCG.
	 * @param track pointer to track.
	 * @param vcg pointer to Vertical Constraint Graph.
	 */
	private void deleteFromVCG(RouteTrack track, RouteVCG vcg)
	{
		// for all track entries in VCG
		for (RouteTrackMem mem = track.nodes; mem != null; mem = mem.next)
		{
			RouteVCGEdge edge2 = vcg.edges;
			for (RouteVCGEdge edge = vcg.edges; edge != null; edge = edge.next)
			{
				if (edge.node.chNode != mem.node)
				{
					edge2 = edge;
					continue;
				}

				// remove from top level VCG
				if (edge == vcg.edges) vcg.edges = edge.next;
					else edge2.next = edge.next;

				// check if its edges have nodes which should be added to VCG
				for (edge2 = edge.node.edges; edge2 != null; edge2 = edge2.next)
					edge2.node.flags &= ~ROUTESEEN;

				// mark any child edges
				for (edge2 = edge.node.edges; edge2 != null; edge2 = edge2.next)
					markVCG(edge2.node.edges);

				markVCG(vcg.edges);
				RouteVCGEdge edge3 = null;
				for (edge2 = edge.node.edges; edge2 != null; edge2 = edge3)
				{
					edge3 = edge2.next;
					if ((edge2.node.flags & ROUTESEEN) == 0)
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
	private void markVCG(RouteVCGEdge edges)
	{
		if (edges == null) return;

		for ( ; edges != null; edges = edges.next)
		{
			edges.node.flags |= ROUTESEEN;
			markVCG(edges.node.edges);
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
	private void createPowerTies(RouteChannel chan, RouteVCG vcg, GetNetlist.SCCell cell)
	{
		// check for bottom channel
		if (chan.number == 0) return;

		// get correct row
		int rowIndex = 0;
		Place.RowList row = (Place.RowList)cell.placement.theRows.get(rowIndex);
		for (int num = 1; num < chan.number && row != null; num++)
		{
			rowIndex++;
			row = (Place.RowList)cell.placement.theRows.get(rowIndex);
		}
		if (row == null) return;

		// get correct route row
		RouteRow rRow = cell.route.rows;
		for (int num = 1; num < chan.number; num++)
			rRow = rRow.next;

		// check all places in row if Base Cell
		for (Place.NBPlace place = row.start; place != null; place = place.next)
		{
			if (place.cell.type != GetNetlist.LEAFCELL) continue;

			// check all ports of instance for reference to power
			for (GetNetlist.SCNiPort port = place.cell.ports; port != null; port = port.next)
			{
				if (port.extNode == cell.power)
				{
					// found one
					// should be a power port on this instance
					if (place.cell.power == null)
					{
						System.out.println("WARNING - Cannot find power on " + place.cell.name);
						continue;
					}

					// create new route node
					RouteNode rNode = new RouteNode();
					rNode.extNode = port.extNode;
					rNode.row = rRow;
					rNode.firstPort = null;
					rNode.lastPort = null;
					rNode.sameNext = null;
					rNode.sameLast = null;
					rNode.next = rRow.nodes;
					rRow.nodes = rNode;

					// create route ports to these ports
					RoutePort rPort1 = new RoutePort();
					rPort1.place = place;
					rPort1.port = port;
					rPort1.node = rNode;
					rNode.firstPort = rPort1;
					rPort1.flags = 0;
					rPort1.last = null;
					rPort1.next = null;
					RoutePort rPort2 = new RoutePort();
					rPort2.place = place;
					rPort2.port = place.cell.power;
					rPort2.node = rNode;
					rNode.lastPort = rPort2;
					rPort2.flags = 0;
					rPort2.last = rPort1;
					rPort1.next = rPort2;
					rPort2.next = null;

					// create channel node
					RouteChNode chNode = new RouteChNode();
					chNode.extNode = port.extNode;
					chNode.number = 0;
					chNode.firstPort = null;
					chNode.lastPort = null;
					chNode.channel = chan;
					chNode.flags = 0;
					chNode.sameNext = null;
					chNode.sameLast = null;
					chNode.next = chan.nodes;
					chan.nodes = chNode;

					// create channel ports
					RouteChPort chPort1 = new RouteChPort();
					chPort1.port = rPort1;
					chPort1.node = chNode;
					chPort1.xPos = portPosition(rPort1);
					chPort1.flags = 0;
					chPort1.last = null;
					chPort1.next = null;
					RouteChPort chPort2 = new RouteChPort();
					chPort2.port = rPort2;
					chPort2.node = chNode;
					chPort2.xPos = portPosition(rPort2);
					chPort2.flags = 0;
					chPort2.last = null;
					chPort2.next = null;
					if (chPort1.xPos <= chPort2.xPos)
					{
						chNode.firstPort = chPort1;
						chNode.lastPort = chPort2;
						chPort1.next = chPort2;
						chPort2.last = chPort1;
					} else
					{
						chNode.firstPort = chPort2;
						chNode.lastPort = chPort1;
						chPort2.next = chPort1;
						chPort1.last = chPort2;
					}

					// create a VCG node
					RouteVCG vNode = new RouteVCG();
					vNode.chNode = chNode;
					vNode.flags = 0;
					vNode.edges = null;

					// create a VCG edge
					RouteVCGEdge vEdge = new RouteVCGEdge();
					vEdge.node = vNode;
					vEdge.next = vcg.edges;
					vcg.edges = vEdge;

					// make this port dependent on any others which are
					// too close to the power port edge
					for (RouteVCGEdge edge1 = vEdge.next; edge1 != null; edge1 = edge1.next)
					{
						double minX = edge1.node.chNode.firstPort.xPos - SilComp.getMinPortDistance();
						double maxX = edge1.node.chNode.lastPort.xPos + SilComp.getMinPortDistance();
						if (chPort2.xPos > minX && chPort2.xPos < maxX)
						{
							// create dependency
							RouteVCGEdge edge2 = new RouteVCGEdge();
							edge2.node = vNode;
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
	private void createGroundTies(RouteChannel chan, RouteVCG vcg, GetNetlist.SCCell cell)
	{
		// check for not top channel
		if (chan.number == cell.placement.numRows) return;

		// get correct row (above)
		int rowIndex = 0;
		Place.RowList row = (Place.RowList)cell.placement.theRows.get(rowIndex);
		for (int num = 0; num < chan.number && row != null; num++)
		{
			rowIndex++;
			row = (Place.RowList)cell.placement.theRows.get(rowIndex);
		}
		if (row == null) return;

		// get correct route row (above)
		RouteRow rRow = cell.route.rows;
		for (int num = 0; num < chan.number; num++)
			rRow = rRow.next;

		// check all places in row if Base Cell
		for (Place.NBPlace place = row.start; place != null; place = place.next)
		{
			if (place.cell.type != GetNetlist.LEAFCELL) continue;

			// check all ports of instance for reference to ground
			for (GetNetlist.SCNiPort port = place.cell.ports; port != null; port = port.next)
			{
				if (port.extNode == cell.ground)
				{
					// found one
					// should be a ground port on this instance
					if (place.cell.ground == null)
					{
						System.out.println("WARNING - Cannot find ground on " + place.cell.name);
						continue;
					}

					// create new route node
					RouteNode rNode = new RouteNode();
					rNode.extNode = port.extNode;
					rNode.row = rRow;
					rNode.firstPort = null;
					rNode.lastPort = null;
					rNode.sameNext = null;
					rNode.sameLast = null;
					rNode.next = rRow.nodes;
					rRow.nodes = rNode;

					// create route ports to these ports
					RoutePort rPort1 = new RoutePort();
					rPort1.place = place;
					rPort1.port = port;
					rPort1.node = rNode;
					rNode.firstPort = rPort1;
					rPort1.flags = 0;
					rPort1.last = null;
					rPort1.next = null;
					RoutePort rPort2 = new RoutePort();
					rPort2.place = place;
					rPort2.port = place.cell.ground;
					rPort2.node = rNode;
					rNode.lastPort = rPort2;
					rPort2.flags = 0;
					rPort2.last = rPort1;
					rPort1.next = rPort2;
					rPort2.next = null;

					// create channel node
					RouteChNode chNode = new RouteChNode();
					chNode.extNode = port.extNode;
					chNode.number = 0;
					chNode.firstPort = null;
					chNode.lastPort = null;
					chNode.channel = chan;
					chNode.flags = 0;
					chNode.sameNext = null;
					chNode.sameLast = null;
					chNode.next = chan.nodes;
					chan.nodes = chNode;

					// create channel ports
					RouteChPort chPort1 = new RouteChPort();
					chPort1.port = rPort1;
					chPort1.node = chNode;
					chPort1.xPos = portPosition(rPort1);
					chPort1.flags = 0;
					chPort1.last = null;
					chPort1.next = null;
					RouteChPort chPort2 = new RouteChPort();
					chPort2.port = rPort2;
					chPort2.node = chNode;
					chPort2.xPos = portPosition(rPort2);
					chPort2.flags = 0;
					chPort2.last = null;
					chPort2.next = null;
					if (chPort1.xPos <= chPort2.xPos)
					{
						chNode.firstPort = chPort1;
						chNode.lastPort = chPort2;
						chPort1.next = chPort2;
						chPort2.last = chPort1;
					} else
					{
						chNode.firstPort = chPort2;
						chNode.lastPort = chPort1;
						chPort2.next = chPort1;
						chPort1.last = chPort2;
					}

					// create a VCG node
					RouteVCG vNode = new RouteVCG();
					vNode.chNode = chNode;
					vNode.flags = 0;
					vNode.edges = null;

					// create a VCG edge
					RouteVCGEdge vEdge = new RouteVCGEdge();
					vEdge.node = vNode;
					vEdge.next = vcg.edges;
					vcg.edges = vEdge;

					// make all others VCG nodes which are too close to
					// the ground port edge dependent on this node
					for (RouteVCGEdge edge1 = vEdge.next; edge1 != null; edge1 = edge1.next)
					{
						double minX = edge1.node.chNode.firstPort.xPos - SilComp.getMinPortDistance();
						double maxX = edge1.node.chNode.lastPort.xPos + SilComp.getMinPortDistance();
						if (chPort2.xPos > minX && chPort2.xPos < maxX)
						{
							// create dependency
							RouteVCGEdge edge2 = new RouteVCGEdge();
							edge2.node = edge1.node;
							edge2.next = vNode.edges;
							vNode.edges = edge2;
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
	private void printVCG(RouteVCGEdge edges, int level)
	{
		if (edges == null) return;

		StringBuffer sb = new StringBuffer();
		int i = level << 2;
		for (int j = 0; j < i; j++)
			sb.append(" ");

		for (; edges != null; edges = edges.next)
		{
			System.out.println(sb.toString() + "before Net " + edges.node.chNode.number);
			printVCG(edges.node.edges, level + 1);
		}
	}

}
