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

import com.sun.electric.database.geometry.Orientation;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitivePort;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Schematics;
import com.sun.electric.tool.user.User;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Iterator;

/**
 * The generation part of the Silicon Compiler tool.
 */
public class Maker
{
	private static class MakerData
	{
		/** cell being layed out */			GetNetlist.SCCell	cell;
		/** list of rows */					MakerRow		rows;
		/** list of channels */				MakerChannel	channels;
		/** list of vdd ports */			MakerPower		power;
		/** list of ground ports */			MakerPower		ground;
		/** minimum x position */			double			minX;
		/** maximum x position */			double			maxX;
		/** minimum y position */			double			minY;
		/** maximum y position */			double			maxY;
	};

	private static class MakerRow
	{
		/** row number */					int				number;
		/** instances in rows */			MakerInst		members;
		/** minimum X position */			double			minX;
		/** maximum X position */			double			maxX;
		/** minimum Y position */			double			minY;
		/** maximum Y position */			double			maxY;
		/** processing bits */				int				flags;
		/** last row */						MakerRow		last;
		/** next row */						MakerRow		next;
	};

	private static class MakerInst
	{
		/** reference place */				Place.NBPlace	place;
		/** reference row */				MakerRow		row;
		/** X position */					double			xPos;
		/** Y position */					double			yPos;
		/** size in X */					double			xSize;
		/** size in Y */					double			ySize;
		/** processing flags */				int				flags;
		/** leaf instance */				NodeInst		instance;
		/** next in row */					MakerInst		next;
	};

	private static class MakerChannel
	{
		/** number of channel */			int				number;
		/** list of tracks */				MakerTrack		tracks;
		/** number of tracks */				int				numTracks;
		/** minimum Y position */			double			minY;
		/** Y size */						double			ySize;
		/** processing bits */				int				flags;
		/** last channel */					MakerChannel	last;
		/** next channel */					MakerChannel	next;
	};

	private static class MakerTrack
	{
		/** track number */					int				number;
		/** nodes in track */				MakerNode		nodes;
		/** reference track */				Route.RouteTrack	track;
		/** Y position */					double			yPos;
		/** processing bits */				int				flags;
		/** previous track */				MakerTrack		last;
		/** next track */					MakerTrack		next;
	};

	private static class MakerNode
	{
		/** list of vias */					MakerVia		vias;
		/** next node in track */			MakerNode		next;
	};

	private static final int VIASPECIAL	= 0x00000001;
	private static final int VIAEXPORT	= 0x00000002;
	private static final int VIAPOWER	= 0x00000004;

	private static class MakerVia
	{
		/** X position */					double			xPos;
		/** associated channel port */		Route.RouteChPort	chPort;
		/** associated leaf instance */		NodeInst		instance;
		/** flags for processing */			int				flags;
		/** export port */					Route.RouteExport	xPort;
		/** next via */						MakerVia		next;
	};

	private static class MakerPower
	{
		/** list of power ports */			MakerPowerPort ports;
		/** vertical position of row */		double			yPos;
		/** next in row list */				MakerPower		next;
		/** last in row list */				MakerPower		last;
	};

	private static class MakerPowerPort
	{
		/** instance */						MakerInst		inst;
		/** port on instance */				GetNetlist.SCNiPort port;
		/** resultant x position */			double			xPos;
		/** next in list */					MakerPowerPort	next;
		/** last in list */					MakerPowerPort	last;
	};

	private PrimitiveNode layer1Proto;
	private PrimitiveNode layer2Proto;
	private PrimitiveNode viaProto;
	private PrimitiveNode pWellProto;
	private PrimitiveNode nWellProto;
	private ArcProto layer1Arc;
	private ArcProto layer2Arc;

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
	public Object makeLayout(Library destLib, GetNetlist gnl)
	{
		// check if working in a cell
		if (gnl.curSCCell == null) return "No cell selected";

		// check if placement structure exists
		if (gnl.curSCCell.placement == null)
			return "No PLACEMENT structure for cell '" + gnl.curSCCell.name + "'";

		// check if route structure exists
		if (gnl.curSCCell.route == null)
			return "No ROUTE structure for cell '" + gnl.curSCCell.name + "'";

		// set up make structure
		MakerData makeData = setUp(gnl.curSCCell);

		// create actual layout
		Object result = createLayout(destLib, makeData);
		if (result instanceof String) return result;

		return result;
	}

	/**
	 * Method to create the data structures to define the precise layout of the cell.
	 * Decide exactly where cells are placed, tracks are laid, via are positioned, etc.
	 * @param cell pointer to cell to layout.
	 * @return created data.
	 */
	private MakerData setUp(GetNetlist.SCCell cell)
	{
		// create top level data structure
		MakerData data = new MakerData();
		data.cell = cell;
		data.rows = null;
		data.channels = null;
		data.power = null;
		data.ground = null;
		data.minX = Double.MAX_VALUE;
		data.maxX = Double.MIN_VALUE;
		data.minY = Double.MAX_VALUE;
		data.maxY = Double.MIN_VALUE;

		// create Maker Channel and Track data structures
		double rowToTrack = (SilComp.getViaSize() / 2) + SilComp.getMinMetalSpacing();
		double minTrackToTrack = (SilComp.getViaSize() / 2) +
			SilComp.getMinMetalSpacing() + (SilComp.getHorizArcWidth() / 2);
		double maxTrackToTrack = SilComp.getViaSize() + SilComp.getMinMetalSpacing();
		MakerChannel lastMChan = null;
		for (Route.RouteChannel chan = cell.route.channels; chan != null; chan = chan.next)
		{
			// create Maker Channel structute
			MakerChannel mChan = new MakerChannel();
			mChan.number = chan.number;
			mChan.tracks = null;
			mChan.numTracks = 0;
			mChan.ySize = 0;
			mChan.flags = 0;
			mChan.next = null;
			mChan.last = lastMChan;
			if (lastMChan != null)
			{
				lastMChan.next = mChan;
			} else
			{
				data.channels = mChan;
			}
			lastMChan = mChan;

			// create Make Track structures
			MakerTrack lastMTrack = null;
			double yPos = 0;
			for (Route.RouteTrack track = chan.tracks; track != null; track = track.next)
			{
				MakerTrack mTrack = new MakerTrack();
				mTrack.number = track.number;
				mTrack.nodes = null;
				mTrack.track = track;
				mTrack.flags = 0;
				mTrack.next = null;
				mTrack.last = lastMTrack;
				if (lastMTrack != null)
				{
					lastMTrack.next = mTrack;
				} else
				{
					mChan.tracks = mTrack;
				}
				lastMTrack = mTrack;
				mChan.numTracks++;
				if (mTrack.number == 0)
				{
					yPos += rowToTrack;
					mTrack.yPos = yPos;
				} else
				{
					// determine if min or max track to track spacing is used
					double deltaY = minTrackToTrack;
					Route.RouteTrackMem tr1Mem = track.nodes;
					Route.RouteTrackMem tr2Mem = track.last.nodes;
					Route.RouteChPort tr1Port = tr1Mem.node.firstPort;
					Route.RouteChPort tr2Port = tr2Mem.node.firstPort;
					while (tr1Port != null && tr2Port != null)
					{
						if (Math.abs(tr1Port.xPos - tr2Port.xPos) < maxTrackToTrack)
						{
							deltaY = maxTrackToTrack;
							break;
						}
						if (tr1Port.xPos < tr2Port.xPos)
						{
							tr1Port = tr1Port.next;
							if (tr1Port == null)
							{
								tr1Mem = tr1Mem.next;
								if (tr1Mem != null)
									tr1Port = tr1Mem.node.firstPort;
							}
						} else
						{
							tr2Port = tr2Port.next;
							if (tr2Port == null)
							{
								tr2Mem = tr2Mem.next;
								if (tr2Mem != null)
									tr2Port = tr2Mem.node.firstPort;
							}
						}
					}
					yPos += deltaY;
					mTrack.yPos = yPos;
				}
				if (track.next == null)
					yPos += rowToTrack;
			}
			mChan.ySize = yPos;
		}

		// create Maker Rows and Instances data structures
		MakerChannel mChan = data.channels;
		mChan.minY = 0;
		double yPos = mChan.ySize;
		MakerRow lastMRow = null;
		for(Place.RowList row : cell.placement.theRows)
		{
			// create maker row data structure
			MakerRow mRow = new MakerRow();
			mRow.number = row.rowNum;
			mRow.members = null;
			mRow.minX = Double.MAX_VALUE;
			mRow.maxX = Double.MIN_VALUE;
			mRow.minY = Double.MAX_VALUE;
			mRow.maxY = Double.MIN_VALUE;
			mRow.flags = 0;
			mRow.next = null;
			mRow.last = lastMRow;
			if (lastMRow != null)
			{
				lastMRow.next = mRow;
			} else
			{
				data.rows = mRow;
			}
			lastMRow = mRow;

			// determine permissible top and bottom overlap
			double tOffset = Double.MIN_VALUE;
			double bOffset = Double.MAX_VALUE;
			for (Place.NBPlace place = row.start; place != null; place = place.next)
			{
				if (place.cell.type != GetNetlist.LEAFCELL) continue;
				GetNetlist.SCCellNums cNums = GetNetlist.getLeafCellNums((Cell)place.cell.np);
				tOffset = Math.max(tOffset, SilComp.leafCellYSize((Cell)place.cell.np) - cNums.topActive);
				bOffset = Math.min(bOffset, cNums.bottomActive);
			}
			yPos -= bOffset;

			// create maker instance structure for each member in the row
			MakerInst lastMInst = null;
			for (Place.NBPlace place = row.start; place != null; place = place.next)
			{
				if (place.cell.type != GetNetlist.LEAFCELL &&
					place.cell.type != GetNetlist.FEEDCELL &&
					place.cell.type != GetNetlist.LATERALFEED) continue;
				MakerInst mInst = new MakerInst();
				mInst.place = place;
				mInst.row = mRow;
				mInst.xPos = place.xPos;
				mInst.yPos = yPos;
				mInst.xSize = place.cell.size;
				if (place.cell.type == GetNetlist.LEAFCELL)
				{
					mInst.ySize = SilComp.leafCellYSize((Cell)place.cell.np);

					// add power ports
					for (GetNetlist.SCNiPort iport = place.cell.power; iport != null; iport = iport.next)
					{
						MakerPowerPort powerPort = new MakerPowerPort();
						powerPort.inst = mInst;
						powerPort.port = iport;
						if ((mRow.number % 2) != 0)
						{
							powerPort.xPos = mInst.xPos + mInst.xSize - iport.xPos;
						} else
						{
							powerPort.xPos = mInst.xPos + iport.xPos;
						}
						powerPort.next = null;
						powerPort.last = null;
						double portYPos = mInst.yPos + SilComp.leafPortYPos((Export)iport.port);
						MakerPower pList;
						for (pList = data.power; pList != null; pList = pList.next)
						{
							if (pList.yPos == portYPos) break;
						}
						if (pList == null)
						{
							pList = new MakerPower();
							pList.ports = null;
							pList.yPos = portYPos;
							MakerPower lastPList = null;
							MakerPower nextPList;
							for (nextPList = data.power; nextPList != null; nextPList = nextPList.next)
							{
								if (portYPos < nextPList.yPos) break;
								lastPList = nextPList;
							}
							pList.next = nextPList;
							pList.last = lastPList;
							if (lastPList != null)
							{
								lastPList.next = pList;
							} else
							{
								data.power = pList;
							}
							if (nextPList != null)
							{
								nextPList.last = pList;
							}
						}
						MakerPowerPort lastPort = null;
						MakerPowerPort nextPort;
						for (nextPort = pList.ports; nextPort != null; nextPort = nextPort.next)
						{
							if (powerPort.xPos < nextPort.xPos) break;
							lastPort = nextPort;
						}
						powerPort.next = nextPort;
						powerPort.last = lastPort;
						if (lastPort != null)
						{
							lastPort.next = powerPort;
						} else
						{
							pList.ports = powerPort;
						}
						if (nextPort != null)
						{
							nextPort.last = powerPort;
						}
					}

					// add ground ports
					for (GetNetlist.SCNiPort iPort = place.cell.ground; iPort != null; iPort = iPort.next)
					{
						MakerPowerPort powerPort = new MakerPowerPort();
						powerPort.inst = mInst;
						powerPort.port = iPort;
						if ((mRow.number % 2) != 0)
						{
							powerPort.xPos = mInst.xPos + mInst.xSize - iPort.xPos;
						} else
						{
							powerPort.xPos = mInst.xPos + iPort.xPos;
						}
						powerPort.next = null;
						powerPort.last = null;
						double portYPos = mInst.yPos + SilComp.leafPortYPos((Export)iPort.port);
						MakerPower pList;
						for (pList = data.ground; pList != null; pList = pList.next)
						{
							if (pList.yPos == portYPos) break;
						}
						if (pList == null)
						{
							pList = new MakerPower();
							pList.ports = null;
							pList.yPos = portYPos;
							MakerPower lastPList = null;
							MakerPower nextPList;
							for (nextPList = data.ground; nextPList != null; nextPList = nextPList.next)
							{
								if (portYPos < nextPList.yPos) break;
								lastPList = nextPList;
							}
							pList.next = nextPList;
							pList.last = lastPList;
							if (lastPList != null)
							{
								lastPList.next = pList;
							} else
							{
								data.ground = pList;
							}
							if (nextPList != null)
							{
								nextPList.last = pList;
							}
						}
						MakerPowerPort lastPort = null;
						MakerPowerPort nextPort;
						for (nextPort = pList.ports; nextPort != null; nextPort = nextPort.next)
						{
							if (powerPort.xPos < nextPort.xPos) break;
							lastPort = nextPort;
						}
						powerPort.next = nextPort;
						powerPort.last = lastPort;
						if (lastPort != null)
						{
							lastPort.next = powerPort;
						} else
						{
							pList.ports = powerPort;
						}
						if (nextPort != null)
						{
							nextPort.last = powerPort;
						}
					}
				} else if (place.cell.type == GetNetlist.FEEDCELL)
				{
					mInst.ySize = bOffset;
				} else if (place.cell.type == GetNetlist.LATERALFEED)
				{
					Route.RoutePort rPort = (Route.RoutePort)place.cell.ports.port;
					mInst.ySize = SilComp.leafPortYPos((Export)rPort.port.port);
				} else
				{
					System.out.println("ERROR - unknown cell type in maker set up");
					mInst.ySize = 0;
				}
				mInst.instance = null;
				mInst.flags = 0;
				place.cell.tp = mInst;
				mInst.next = null;
				if (lastMInst != null)
				{
					lastMInst.next = mInst;
				} else
				{
					mRow.members = mInst;
				}
				lastMInst = mInst;

				// set limits of row
				mRow.minX = Math.min(mRow.minX, mInst.xPos);
				mRow.maxX = Math.max(mRow.maxX, mInst.xPos + mInst.xSize);
				mRow.minY = Math.min(mRow.minY, mInst.yPos);
				mRow.maxY = Math.max(mRow.maxY, mInst.yPos + mInst.ySize);
			}
			data.minX = Math.min(data.minX, mRow.minX);
			data.maxX = Math.max(data.maxX, mRow.maxX);
			data.minY = Math.min(data.minY, mRow.minY);
			data.maxY = Math.max(data.maxY, mRow.maxY);
			yPos += tOffset;
			mChan = mChan.next;
			mChan.minY = yPos;
			yPos += mChan.ySize;
		}

		// create via list for all tracks
		for (mChan = data.channels; mChan != null; mChan = mChan.next)
		{
			// get bottom track and work up
			MakerTrack mTrack;
			for (mTrack = mChan.tracks; mTrack != null; mTrack = mTrack.next)
			{
				if (mTrack.next == null) break;
			}
			for ( ; mTrack != null; mTrack = mTrack.last)
			{
				yPos = mChan.minY + (mChan.ySize - mTrack.yPos);
				mTrack.yPos = yPos;
				for (Route.RouteTrackMem mem = mTrack.track.nodes; mem != null; mem = mem.next)
				{
					MakerNode mNode = new MakerNode();
					mNode.vias = null;
					mNode.next = mTrack.nodes;
					mTrack.nodes = mNode;
					MakerVia lastVia = null;
					for (Route.RouteChPort chPort = mem.node.firstPort; chPort != null; chPort = chPort.next)
					{
						MakerVia mVia = new MakerVia();
						mVia.xPos = chPort.xPos;
						mVia.chPort = chPort;
						mVia.instance = null;
						mVia.flags = 0;
						mVia.xPort = null;
						mVia.next = null;
						if (lastVia != null)
						{
							lastVia.next = mVia;
						} else
						{
							mNode.vias = mVia;
						}
						lastVia = mVia;

						// check for power port
						if (mVia.chPort.port.place.cell.type == GetNetlist.LEAFCELL)
						{
							int type = GetNetlist.getLeafPortType((Export)mVia.chPort.port.port.port);
							if (type == GetNetlist.PWRPORT || type == GetNetlist.GNDPORT)
								mVia.flags |= VIAPOWER;
						}

						// check for export
						for (Route.RouteExport xPort = data.cell.route.exports; xPort != null; xPort = xPort.next)
						{
							if (xPort.chPort == mVia.chPort)
							{
								mVia.flags |= VIAEXPORT;
								mVia.xPort = xPort;
								break;
							}
						}

						data.minX = Math.min(data.minX, chPort.xPos);
						data.maxX = Math.max(data.maxX, chPort.xPos);
						data.minY = Math.min(data.minY, chPort.xPos);
						data.maxY = Math.max(data.maxY, chPort.xPos);
					}
				}
			}
		}

		return data;
	}

	/**
	 * Method to create the actual layout in the associated VLSI layout tool using
	 * the passed layout data.
     * @param destLib destination library.
	 * @param data pointer to layout data.
	 */
	private Object createLayout(Library destLib, MakerData data)
	{
		double rowToTrack = (SilComp.getViaSize() / 2) + SilComp.getMinMetalSpacing();
		double trackToTtrack = SilComp.getViaSize() + SilComp.getMinMetalSpacing();

		String err = setupForMaker();
		if (err != null) return err;

		// create new cell
		Cell bCell = Cell.makeInstance(destLib, data.cell.name + "{lay}");
		if (bCell == null) return "Cannot create leaf cell '" + data.cell.name + "{lay}' in MAKER";

		// create instances for cell
		for (MakerRow row = data.rows; row != null; row = row.next)
		{
			boolean flipX = false;
			if ((row.number % 2) != 0) flipX = true;
			for (MakerInst inst = row.members; inst != null; inst = inst.next)
			{
				GetNetlist.SCNiTree node = inst.place.cell;
				if (node.type == GetNetlist.LEAFCELL)
				{
					Cell subCell = (Cell)node.np;
					Rectangle2D bounds = subCell.getBounds();
                    Orientation orient = Orientation.IDENT;
//					double wid = inst.xSize;
					double hEdge = -bounds.getMinX();
					if (flipX)
					{
                        orient = Orientation.X;
//						wid = -wid;
						hEdge = bounds.getMaxX();
					}
					Point2D ctr = new Point2D.Double(inst.xPos + hEdge, inst.yPos - bounds.getMinY());
					inst.instance = NodeInst.makeInstance(subCell, ctr, inst.xSize, inst.ySize, bCell, orient, node.name, 0);
//					inst.instance = NodeInst.makeInstance(subCell, ctr, wid, inst.ySize, bCell, 0, node.name, 0);
					if (inst.instance == null)
						return "Cannot create leaf instance '" + node.name+ "' in MAKER";
				} else if (node.type == GetNetlist.FEEDCELL)
				{
					// feed through node
					Point2D ctr = new Point2D.Double(inst.xPos + (inst.xSize / 2), inst.yPos + inst.ySize + SilComp.getVertArcWidth() / 2);
					inst.instance = NodeInst.makeInstance(layer2Proto, ctr, SilComp.getVertArcWidth(), SilComp.getVertArcWidth(), bCell);
					if (inst.instance == null)
						return "Cannot create leaf feed in MAKER";
				} else if (node.type == GetNetlist.LATERALFEED)
				{
					// lateral feed node
					Point2D ctr = new Point2D.Double(inst.xPos + (inst.xSize / 2), inst.yPos + inst.ySize);
					inst.instance = NodeInst.makeInstance(viaProto, ctr, viaProto.getDefWidth(), viaProto.getDefHeight(), bCell);
					if (inst.instance == null)
						return "Cannot create via in MAKER";
				}
			}
		}

		// create vias and vertical tracks
		for (MakerChannel chan = data.channels; chan != null; chan = chan.next)
		{
			for (MakerTrack track = chan.tracks; track != null; track = track.next)
			{
				for (MakerNode mNode = track.nodes; mNode != null; mNode = mNode.next)
				{
					for (MakerVia via = mNode.vias; via != null; via = via.next)
					{
						if ((via.flags & VIAPOWER) != 0)
						{
							via.instance = NodeInst.makeInstance(layer1Proto, new Point2D.Double(via.xPos, track.yPos),
								SilComp.getHorizArcWidth(), SilComp.getHorizArcWidth(), bCell);
							if (via.instance == null)
								return "Cannot create via in MAKER";

							// create vertical power track
							MakerInst inst = (MakerInst)via.chPort.port.place.cell.tp;
							if (trackLayer1(inst.instance, (PortProto)via.chPort.port.port.port,
								via.instance, null, SilComp.getHorizArcWidth(), bCell) == null)
							{
								return "Cannot create layer2 track in MAKER";
							}
							continue;
						}

						// create a via if next via (if it exists) is farther
						// than the track to track spacing, else create a layer2 node
						if (via.next != null && (via.next.flags & VIAPOWER) == 0)
						{
							if (Math.abs(via.next.xPos - via.xPos) < trackToTtrack)
							{
								if ((via.flags & VIAEXPORT) != 0)
								{
									via.next.flags |= VIASPECIAL;
								} else
								{
									via.flags |= VIASPECIAL;
								}
							}
						}
						if ((via.flags & VIASPECIAL) != 0)
						{
							via.instance = NodeInst.makeInstance(layer2Proto, new Point2D.Double(via.xPos, track.yPos),
								SilComp.getVertArcWidth(), SilComp.getVertArcWidth(), bCell);
							if (via.instance == null)
								return "Cannot create leaf feed in MAKER";
						} else
						{
							via.instance = NodeInst.makeInstance(viaProto, new Point2D.Double(via.xPos, track.yPos),
								viaProto.getDefWidth(), viaProto.getDefHeight(), bCell);
							if (via.instance == null)
								return "Cannot create via in MAKER";
						}

						// create vertical track
						GetNetlist.SCNiTree node = via.chPort.port.place.cell;
						if (node.type == GetNetlist.LEAFCELL)
						{
							MakerInst inst = (MakerInst)node.tp;
							if (trackLayer2(inst.instance, (PortProto)via.chPort.port.port.port,
								via.instance, null, SilComp.getVertArcWidth(), bCell) == null)
							{
								return "Cannot create layer2 track in MAKER";
							}
						} else if (node.type == GetNetlist.FEEDCELL ||
							node.type == GetNetlist.LATERALFEED)
						{
							MakerInst inst = (MakerInst)node.tp;
							if (trackLayer2(inst.instance, null,
								via.instance, null, SilComp.getVertArcWidth(), bCell) == null)
							{
								return "Cannot create layer2 track in MAKER";
							}
						}
					}
				}
			}
		}

		// create horizontal tracks
		for (MakerChannel chan = data.channels; chan != null; chan = chan.next)
		{
			for (MakerTrack track = chan.tracks; track != null; track = track.next)
			{
				for (MakerNode mNode = track.nodes; mNode != null; mNode = mNode.next)
				{
					for (MakerVia via = mNode.vias; via != null; via = via.next)
					{
						if (via.next != null)
						{
							if ((via.flags & VIASPECIAL) != 0)
							{
								if (Math.abs(via.next.xPos - via.xPos) < trackToTtrack)
								{
									if (trackLayer2(via.instance, null,
										via.next.instance, null, SilComp.getVertArcWidth(), bCell) == null)
									{
										return "Cannot create layer1 track in MAKER";
									}
								}
							} else
							{
								if ((via.flags & VIAPOWER) == 0 &&
									(via.next.flags & VIAPOWER) == 0 &&
									(via.next.xPos - via.xPos) < trackToTtrack)
								{
									if (trackLayer2(via.instance, null,
										via.next.instance, null, SilComp.getVertArcWidth(), bCell) == null)
									{
										return "Cannot create layer1 track in MAKER";
									}
								}
								for (MakerVia via2 = via.next; via2 != null; via2 = via2.next)
								{
									if ((via2.flags & VIASPECIAL) != 0) continue;
									if (trackLayer1(via.instance, null, via2.instance, null,
										SilComp.getHorizArcWidth(), bCell) == null)
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
		for(Place.RowList rlist : data.cell.placement.theRows)
		{
			for (Place.NBPlace place = rlist.start; place != null; place = place.next)
			{
				if (place.cell.type == GetNetlist.STITCH)
				{
					Route.RoutePort rPort = (Route.RoutePort)place.cell.ports.port;
					MakerInst inst = (MakerInst)rPort.place.cell.tp;
					NodeInst inst1 = inst.instance;
					PortProto port1 = (PortProto)rPort.port.port;
					rPort = (Route.RoutePort)place.cell.ports.next.port;
					inst = (MakerInst)rPort.place.cell.tp;
					NodeInst inst2 = inst.instance;
					PortProto port2 = (PortProto)rPort.port.port;
					if (trackLayer1(inst1, port1, inst2, port2,
						SilComp.getHorizArcWidth(), bCell) == null)
					{
						return "Cannot create layer1 track in MAKER";
					}
				} else if (place.cell.type == GetNetlist.LATERALFEED)
				{
					Route.RoutePort rPort = (Route.RoutePort)place.cell.ports.port;
					MakerInst inst = (MakerInst)rPort.place.cell.tp;
					NodeInst inst1 = inst.instance;
					PortProto port1 = (PortProto)rPort.port.port;
					inst = (MakerInst)place.cell.tp;
					NodeInst inst2 = inst.instance;
					if (trackLayer1(inst1, port1, inst2, null,
						SilComp.getHorizArcWidth(), bCell) == null)
					{
						return "Cannot create layer2 track in MAKER";
					}
				}
			}
		}

		// export ports
		for (MakerChannel chan = data.channels; chan != null; chan = chan.next)
		{
			for (MakerTrack track = chan.tracks; track != null; track = track.next)
			{
				for (MakerNode mNode = track.nodes; mNode != null; mNode = mNode.next)
				{
					for (MakerVia via = mNode.vias; via != null; via = via.next)
					{
						if ((via.flags & VIAEXPORT) != 0)
						{
							if (exportPort(via.instance, null,
								via.xPort.xPort.name, via.xPort.xPort.bits & GetNetlist.PORTTYPE, bCell) == null)
							{
								return "Cannot create export port '" + via.xPort.xPort.name + "' in MAKER";
							}
						}
					}
				}
			}
		}

		// create power buses
		NodeInst lastPower = null;
		double xPos = data.minX - rowToTrack - (SilComp.getMainPowerWireWidth()/ 2);

		String mainPowerArc = SilComp.getMainPowerArc();
		boolean mainPwrRailHoriz = mainPowerArc.equals("Horizontal Arc");

		for (MakerPower pList = data.power; pList != null; pList = pList.next)
		{
			double yPos = pList.yPos;

			// create main power bus node
			NodeInst bInst = null;
			if (mainPwrRailHoriz)
			{
				bInst = NodeInst.makeInstance(layer1Proto, new Point2D.Double(xPos, yPos),
					SilComp.getMainPowerWireWidth(), SilComp.getMainPowerWireWidth(), bCell);
			} else
			{
				bInst = NodeInst.makeInstance(layer2Proto, new Point2D.Double(xPos, yPos),
					SilComp.getMainPowerWireWidth(), SilComp.getMainPowerWireWidth(), bCell);
			}
			if (bInst == null)
				return "Cannot create via in MAKER";
			if (lastPower != null)
			{
				// join to previous
				if (mainPwrRailHoriz)
				{
					if (trackLayer1(bInst, null, lastPower, null,
						SilComp.getMainPowerWireWidth(), bCell) == null)
					{
						return "Cannot create layer1 track in MAKER";
					}
				} else
				{
					if (trackLayer2(bInst, null, lastPower, null,
						SilComp.getMainPowerWireWidth(), bCell) == null)
					{
						return "Cannot create layer1 track in MAKER";
					}
				}
			}
			lastPower = bInst;

			for (MakerPowerPort pPort = pList.ports; pPort != null; pPort = pPort.next)
			{
				if (pPort.last == null)
				{
					// connect to main power node
					if (trackLayer1(lastPower, null, pPort.inst.instance, (PortProto)pPort.port.port,
						SilComp.getPowerWireWidth(), bCell) == null)
					{
						return "Cannot create layer1 track in MAKER";
					}
				}

				// connect to next if it exists
				if (pPort.next != null)
				{
					if (trackLayer1(pPort.inst.instance, (PortProto)pPort.port.port,
							pPort.next.inst.instance, (PortProto)pPort.next.port.port,
							SilComp.getPowerWireWidth(), bCell) == null)
					{
						return "Cannot create layer1 track in MAKER";
					}
				}
			}
		}

		// create ground buses
		NodeInst lastGround = null;
		xPos = data.maxX + rowToTrack + (SilComp.getMainPowerWireWidth() / 2);

		for (MakerPower pList = data.ground; pList != null; pList = pList.next)
		{
			double yPos = pList.yPos;

			// create main ground bus node
			NodeInst bInst = null;
			if (mainPwrRailHoriz)
			{
				bInst = NodeInst.makeInstance(layer1Proto, new Point2D.Double(xPos, yPos),
					SilComp.getMainPowerWireWidth(), SilComp.getMainPowerWireWidth(), bCell);
			} else
			{
				bInst = NodeInst.makeInstance(layer2Proto, new Point2D.Double(xPos, yPos),
					SilComp.getMainPowerWireWidth(), SilComp.getMainPowerWireWidth(), bCell);
			}
			if (bInst == null) return "Cannot create via in MAKER";
			if (lastGround != null)
			{
				// join to previous
				if (mainPwrRailHoriz)
				{
					if (trackLayer1(bInst, null, lastGround, null,
						SilComp.getMainPowerWireWidth(), bCell) == null)
					{
						return "Cannot create layer1 track in MAKER";
					}
				} else
				{
					if (trackLayer2(bInst, null, lastGround, null,
						SilComp.getMainPowerWireWidth(), bCell) == null)
					{
						return "Cannot create layer1 track in MAKER";
					}
				}
			} else
			{
				if (exportPort(bInst, null, "gnd", GetNetlist.GNDPORT, bCell) == null)
				{
					return "Cannot create export port 'gnd' in MAKER";
				}
			}
			lastGround = bInst;

			for (MakerPowerPort pPort = pList.ports; pPort != null; pPort = pPort.next)
			{
				if (pPort.next == null)
				{
					// connect to main ground node
					if (trackLayer1(lastGround, null, pPort.inst.instance, (PortProto)pPort.port.port,
						SilComp.getPowerWireWidth(), bCell) == null)
					{
						return "Cannot create layer1 track in MAKER";
					}
				}
				// connect to next if it exists
				else
				{
					if (trackLayer1(pPort.inst.instance, (PortProto)pPort.port.port,
							pPort.next.inst.instance, (PortProto)pPort.next.port.port,
							SilComp.getPowerWireWidth(), bCell) == null)
					{
						return "Cannot create layer1 track in MAKER";
					}
				}
			}
		}
		if (lastPower != null)
		{
			// export as cell vdd
			if (exportPort(lastPower, null, "vdd", GetNetlist.PWRPORT, bCell) == null)
			{
				return "Cannot create export port 'vdd' in MAKER";
			}
		}

		// create overall P-wells if pwell size not zero
		if (SilComp.getPWellHeight() != 0)
		{
			for (MakerRow row = data.rows; row != null; row = row.next)
			{
				MakerInst firstInst = null;
				MakerInst prevInst = null;
				for (MakerInst inst = row.members; inst != null; inst = inst.next)
				{
					if (inst.place.cell.type != GetNetlist.LEAFCELL) continue;
					if (firstInst == null)
					{
						firstInst = inst;
					} else
					{
						prevInst = inst;
					}
				}
				if (prevInst != null)
				{
					xPos = (firstInst.xPos + prevInst.xPos + prevInst.xSize) / 2;
					double xSize = (prevInst.xPos + prevInst.xSize) - firstInst.xPos;
					double ySize = SilComp.getPWellHeight();
					if (ySize > 0)
					{
						double yPos = firstInst.yPos + SilComp.getPWellOffset() +
							(SilComp.getPWellHeight() / 2);
						if (pWellProto != null)
						{
							NodeInst bInst = NodeInst.makeInstance(pWellProto, new Point2D.Double(xPos, yPos), xSize, ySize, bCell);
							if (bInst == null)
								return "Unable to create P-WELL in MAKER";
						}
					}

					ySize = SilComp.getNWellHeight();
					if (ySize > 0)
					{
						double yPos = firstInst.yPos + firstInst.ySize - SilComp.getNWellOffset() -
							(SilComp.getNWellHeight() / 2);
						if (nWellProto != null)
						{
							NodeInst bInst = NodeInst.makeInstance(nWellProto, new Point2D.Double(xPos, yPos), xSize, ySize, bCell);
							if (bInst == null)
								return "Unable to create N-WELL in MAKER";
						}
					}
				}
			}
		}

		return bCell;
	}

	/**
	 * Method to create an export at the given instance at the given port.
	 * Note that ports of primitive instances are passed as NULL and must be determined.
	 * @param inst pointer to instance.
	 * @param port pointer to port on the instance.
	 * @param name name of the Export.
	 * @param type type of port (eg. input, output, etc.)
	 * @param bCell cell in which to create.
	 * @return the new Export (null on error).
	 */
	private Export exportPort(NodeInst inst, PortProto port, String name, int type, Cell bCell)
	{
		// check if primative
		if (port == null)
			port = inst.getProto().getPort(0);

		PortInst pi = inst.findPortInstFromProto(port);
		Export xPort = Export.newInstance(bCell, pi, name);
		if (xPort == null) return null;
		switch (type)
		{
			case GetNetlist.INPORT:    xPort.setCharacteristic(PortCharacteristic.IN);     break;
			case GetNetlist.OUTPORT:   xPort.setCharacteristic(PortCharacteristic.OUT);    break;
			case GetNetlist.BIDIRPORT: xPort.setCharacteristic(PortCharacteristic.BIDIR);  break;
			case GetNetlist.PWRPORT:   xPort.setCharacteristic(PortCharacteristic.PWR);    break;
			default:                   xPort.setCharacteristic(PortCharacteristic.GND);    break;
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
	 * @param bCell cell in which to create.
	 * @return the created ArcInst.
	 */
	private ArcInst trackLayer1(NodeInst instA, PortProto portA, NodeInst instB, PortProto portB, double width, Cell bCell)
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
		if (!portA.getBasePort().connectsTo(layer1Arc))
		{
			// must place a via
			piA = createConnection(piA, xA, yA, layer1Arc);
		}
		if (!portB.getBasePort().connectsTo(layer1Arc))
		{
			// must place a via
			piB = createConnection(piB, xB, yB, layer1Arc);
		}

		ArcInst inst = ArcInst.makeInstance(layer1Arc, width, piA, piB);
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
	 * @param bCell cell in which to create.
	 * @return the created ArcInst.
	 */
	private ArcInst trackLayer2(NodeInst instA, PortProto portA, NodeInst instB, PortProto portB, double width, Cell bCell)
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
		if (!portA.getBasePort().connectsTo(layer2Arc))
		{
			// must place a via
			piA = createConnection(piA, xA, yA, layer1Arc);
		}
		if (!portB.getBasePort().connectsTo(layer2Arc))
		{
			// must place a via
			piB = createConnection(piB, xB, yB, layer1Arc);
		}

		ArcInst inst = ArcInst.makeInstance(layer2Arc, width, piA, piB);
		return inst;
	}

	private PortInst createConnection(PortInst pi, double x, double y, ArcProto arc)
	{
		// always use the standard via (David Harris)
		if (viaProto == null) return null;
		PrimitiveNode.Function fun = viaProto.getFunction();
		if (fun != PrimitiveNode.Function.CONTACT && fun != PrimitiveNode.Function.CONNECT) return null;

		// override given arc and choose one that will connect
		if (pi.getPortProto() instanceof PrimitivePort)
			arc = ((PrimitivePort)pi.getPortProto()).getConnections()[0];

		// make sure that this contact connects to the desired arc
		if (!viaProto.getPort(0).connectsTo(arc)) return null;

		// use this via to make the connection
		NodeInst viaNode = NodeInst.makeInstance(viaProto, new Point2D.Double(x, y),
			viaProto.getDefWidth(), viaProto.getDefHeight(), pi.getNodeInst().getParent());
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
	private String setupForMaker()
	{
		Technology tech = Technology.getCurrent();
		if (tech == Schematics.tech)
			tech = User.getSchematicTechnology();
		String layer1 = SilComp.getHorizRoutingArc();
		String layer2 = SilComp.getVertRoutingArc();
		layer1Arc = tech.findArcProto(layer1);
		layer2Arc = tech.findArcProto(layer2);
		if (layer1Arc == null) return "Unable to find Horizontal Arc " + layer1 + " for MAKER";
		if (layer2Arc == null) return "Unable to find Vertical Arc " + layer2 + " for MAKER";

		// find the contact between the two layers
		for(Iterator<PrimitiveNode> it = tech.getNodes(); it.hasNext(); )
		{
			PrimitiveNode via = it.next();
			PrimitiveNode.Function fun = via.getFunction();
			if (fun != PrimitiveNode.Function.CONTACT && fun != PrimitiveNode.Function.CONNECT) continue;
			PrimitivePort pp = (PrimitivePort)via.getPort(0);
			if (!pp.connectsTo(layer1Arc)) continue;
			if (!pp.connectsTo(layer2Arc)) continue;
			viaProto = via;
			break;
		}
		if (viaProto == null) return "Unable to get VIA for MAKER";

		// find the pin nodes on the connecting layers
		layer1Proto = layer1Arc.findPinProto();
		if (layer1Proto == null)
			return "Unable to get LAYER1-NODE for MAKER";
		layer2Proto = layer2Arc.findPinProto();
		if (layer2Proto == null)
			return "Unable to get LAYER2-NODE for MAKER";

		/*
		 * find the pure-layer node on the P-well layer
		 * if the p-well size is zero don't look for the node
		 * allows technologies without p-wells to be routed (i.e. GaAs)
		 */
		if (SilComp.getPWellHeight() == 0) pWellProto = null; else
		{
			pWellProto = null;
			Layer pWellLay = tech.findLayerFromFunction(Layer.Function.WELLP);
			if (pWellLay != null) pWellProto = pWellLay.getPureLayerNode();
			if (pWellProto == null)
				return "Unable to get LAYER P-WELL for MAKER";
		}
		if (SilComp.getNWellHeight() == 0) nWellProto = null; else
		{
			nWellProto = null;
			Layer nWellLay = tech.findLayerFromFunction(Layer.Function.WELLN);
			if (nWellLay != null) nWellProto = nWellLay.getPureLayerNode();
			if (nWellProto == null)
				return "Unable to get LAYER P-WELL for MAKER";
		}

		return null;
	}

}
