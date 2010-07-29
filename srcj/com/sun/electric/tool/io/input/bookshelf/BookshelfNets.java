/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: BookshelfNets.java
 *
 * Copyright (c) 2010 Sun Microsystems and Static Free Software
 *
 * Electric(tm) is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
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
package com.sun.electric.tool.io.input.bookshelf;

import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.geometry.Orientation;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.io.input.bookshelf.BookshelfNodes.BookshelfNode;
import com.sun.electric.tool.io.input.bookshelf.BookshelfNodes.BookshelfPin;
import com.sun.electric.tool.util.CollectionFactory;

/**
 * @author fschmidt
 * 
 */
public class BookshelfNets {

	private String fileName;
	private Library lib;
	private Map<String, PortInst> netIndex = CollectionFactory.createHashMap();

	public BookshelfNets(String fileName, Library lib) {
		this.fileName = fileName;
		this.lib = lib;
	}

	public void parse() throws IOException {

		Job.getUserInterface().setProgressNote("Parse Net List File: Step 1/4");

		netIndex.clear();

		File file = new File(this.fileName);
		FileReader freader = new FileReader(file);
		BufferedReader rin = new BufferedReader(freader);

		Map<String, List<BookshelfPin>> pins = CollectionFactory.createHashMap();

		// make a map of ports on each node
		Map<BookshelfNode, Set<String>> nodePorts = new HashMap<BookshelfNode, Set<String>>();

		List<BookshelfNet> allNets = new ArrayList<BookshelfNet>();
		String line;
		while ((line = rin.readLine()) != null) {
			if (line.startsWith("NetDegree")) {
				NetDesc desc = parseNetDesc(line);
				BookshelfNet net = new BookshelfNet(desc.netName);
				allNets.add(net);
				for (int i = 0; i < desc.elements; i++) {
					line = rin.readLine();
					BookshelfPin pin = parsePin(line, net);
					if (!pins.containsKey(pin.getNodeName())) {
						pins.put(pin.getNodeName(), new ArrayList<BookshelfPin>());
					}
					pins.get(pin.getNodeName()).add(pin);
					net.getPins().add(pin);

					// make a list of ports on each node
					BookshelfNode bn = BookshelfNode.findNode(pin.getNodeName());
					if (bn != null) {
						Set<String> portsOnNode = nodePorts.get(bn);
						if (portsOnNode == null)
							nodePorts.put(bn, portsOnNode = new TreeSet<String>());
						portsOnNode.add(pin.getLocation().getX() + "," + pin.getLocation().getY());
					}
				}
			}
		}

		Job.getUserInterface().setProgressNote("Parse Net List File: Step 2/4");

		// figure out what cell instances exist
		Map<String, List<BookshelfNode>> nodesBySize = new HashMap<String, List<BookshelfNode>>();
		int allNodesNum = BookshelfNode.getAllNodes().size();
		int counter = 0;
		for (BookshelfNode bn : BookshelfNode.getAllNodes()) {
			Job.getUserInterface().setProgressValue(counter * 100 / allNodesNum);
			String bnName = bn.getWidth() + "X" + bn.getHeight();
			bnName += "Y" + Boolean.toString(bn.isTerminal());
			Set<String> ports = nodePorts.get(bn);
			if (ports != null) {
				for (String portName : ports)
					bnName += "/" + portName;
			}
			List<BookshelfNode> nodes = nodesBySize.get(bnName);
			if (nodes == null)
				nodesBySize.put(bnName, nodes = new ArrayList<BookshelfNode>());
			nodes.add(bn);
			counter++;
		}

		Job.getUserInterface().setProgressNote("Parse Net List File: Step 3/4");

		// now create cells
		int cellNumber = 1;
		counter = 0;
		allNodesNum = nodesBySize.keySet().size();
		for (String desc : nodesBySize.keySet()) {
			Job.getUserInterface().setProgressValue(counter * 100 / allNodesNum);
			List<BookshelfNode> nodes = nodesBySize.get(desc);
			String[] parts = desc.split("/");
			String[] size = parts[0].split("Y")[0].split("X");
			String terminal = parts[0].split("Y")[1];
			double width = TextUtils.atof(size[0]);
			double height = TextUtils.atof(size[1]);
			Cell cell = Cell.makeInstance(lib, "Cell" + cellNumber + "{lay}");
			for (BookshelfNode bn : nodes)
				bn.setPrototype(cell);
			cellNumber++;
			NodeProto np = Artwork.tech().boxNode;
			NodeInst nInst = NodeInst.makeInstance(np, new EPoint(0, 0), width, height, cell);
			if (terminal.equals(Boolean.toString(true))) {
				nInst.setLocked();
			}
			int portNum = 1;
			for (int i = 1; i < parts.length; i++) {
				String[] xy = parts[i].split(",");
				double x = TextUtils.atof(xy[0]);
				double y = TextUtils.atof(xy[1]);
				NodeProto pin = Artwork.tech().pinNode;
				NodeInst ni = NodeInst.makeInstance(pin, new EPoint(x, y), 0, 0, cell);
				PortInst pi = ni.getOnlyPortInst();
				Export.newInstance(cell, pi, "P" + portNum);
				portNum++;
			}
			counter++;
		}

		// now create all the nodes
		Cell mainCell = Cell.makeInstance(lib, lib.getName() + "{lay}");
		Collection<BookshelfNode> allNodes = BookshelfNode.getAllNodes();
		for (BookshelfNode bn : allNodes) {
			Cell np = bn.getPrototype();
			NodeInst ni = NodeInst.newInstance(np, new Point2D.Double(bn.getX(), bn.getY()), bn.getWidth(), bn
					.getHeight(), mainCell, Orientation.IDENT, bn.getName());
			bn.setInstance(ni);
		}

		Job.getUserInterface().setProgressNote("Parse Net List File: Step 4/4");

		// now run the nets
		allNodesNum = allNets.size();
		counter = 0;
		for (BookshelfNet bn : allNets) {
			Job.getUserInterface().setProgressValue(counter * 100 / allNodesNum);
			Job.getUserInterface().setProgressNote(
					"Parse Net List File: Step 4/4 (" + counter + "/" + allNodesNum + ")");
			BookshelfPin lastPin = null;
			for (BookshelfPin bp : bn.getPins()) {
				if (lastPin != null) {
					// find the exports
					BookshelfNode bn1 = BookshelfNode.findNode(lastPin.getNodeName());
					BookshelfNode bn2 = BookshelfNode.findNode(bp.getNodeName());
					EPoint ep1 = new EPoint(lastPin.getLocation().getX(), lastPin.getLocation().getY());
					EPoint ep2 = new EPoint(bp.getLocation().getX(), bp.getLocation().getY());

					// find the proper pin
					PortInst pi1 = null;
					for (Iterator<Export> it = bn1.getPrototype().getExports(); it.hasNext();) {
						Export e = it.next();
						NodeInst ni = e.getOriginalPort().getNodeInst();
						if (ni.getAnchorCenterX() == ep1.getX() && ni.getAnchorCenterY() == ep1.getY()) {
							pi1 = bn1.getInstance().findPortInstFromProto(e);
							break;
						}
					}

					// find the proper pin
					PortInst pi2 = null;
					for (Iterator<Export> it = bn2.getPrototype().getExports(); it.hasNext();) {
						Export e = it.next();
						NodeInst ni = e.getOriginalPort().getNodeInst();
						if (ni.getAnchorCenterX() == ep2.getX() && ni.getAnchorCenterY() == ep2.getY()) {
							pi2 = bn2.getInstance().findPortInstFromProto(e);
							break;
						}
					}
					if (pi1 == null) {
						System.out.println("UNABLE TO FIND PORT AT (" + lastPin.getLocation().getX() + ","
								+ lastPin.getLocation().getY() + ") ON INSTANCE " + bn1.getName() + " (CELL "
								+ bn1.getPrototype().describe(false) + ")");
						continue;
					}
					if (pi2 == null) {
						System.out.println("UNABLE TO FIND PORT AT (" + bp.getLocation().getX() + ","
								+ bp.getLocation().getY() + ") ON INSTANCE " + bn2.getName() + " (CELL "
								+ bn2.getPrototype().describe(false) + ")");
						continue;
					}
					EPoint p1 = EPoint.fromLambda(bn1.getX() + ep1.getLambdaX(), bn1.getY() + ep1.getLambdaY());
					EPoint p2 = EPoint.fromLambda(bn2.getX() + ep2.getLambdaX(), bn2.getY() + ep2.getLambdaY());
					// if (!pi1.getCenter().equals(p1)) {
					// System.out.println("pi1=" + pi1.getCenter() + " p1=" +
					// p1);
					// }
					// if (!pi2.getCenter().equals(p2)) {
					// System.out.println("pi2=" + pi2.getCenter() + " p2=" +
					// p2);
					// }
					ArcProto ap = Generic.tech().unrouted_arc;
					newInstance(mainCell, ap, bn.name, pi1, pi2, p1, p2, 0, 0, 0);
				}
				lastPin = bp;
				counter++;
			}

		}
	}

	public static void newInstance(Cell parent, ArcProto protoType, String name, PortInst headPort, PortInst tailPort,
			EPoint headPt, EPoint tailPt, long gridExtendOverMin, int angle, int flags) {
		if (true) {
			ArcInst.newInstance(parent, protoType, name, null, headPort, tailPort, headPt, tailPt, gridExtendOverMin,
					angle, flags);
		} else {
			ArcInst.makeInstance(protoType, headPort, tailPort);
			// // make sure the arc can connect to these ports
			// PortProto headProto = headPort.getPortProto();
			// PortProto tailProto = tailPort.getPortProto();
			// Name nameKey = Name.findName(name);
			// TextDescriptor nameDescriptor =
			// TextDescriptor.getArcTextDescriptor();
			//
			// // search for spare arcId
			// CellId parentId = parent.getId();
			// int arcId;
			// do {
			// arcId = parentId.newArcId();
			// } while (parent.getArcById(arcId) != null);
			// ImmutableArcInst d = ImmutableArcInst.newInstance(arcId,
			// protoType.getId(), nameKey, nameDescriptor,
			// tailPort.getNodeInst().getD().nodeId, tailProto.getId(), tailPt,
			// headPort.getNodeInst().getD().nodeId, headProto.getId(), headPt,
			// gridExtendOverMin, angle, flags);
			// Topology topology = parent.getTopology();
			// ArcInst ai = new ArcInst(topology, d, headPort, tailPort);
			// topology.addArc(ai);
		}
	}

	private BookshelfPin parsePin(String line, BookshelfNet net) {

		String[] splited = line.trim().split(" ");

		double xPos = 0;
		double yPos = 0;
		if (splited.length == 6) {
			xPos = Double.parseDouble(splited[4]);
			yPos = Double.parseDouble(splited[5]);
		}

		Point2D location = new Point2D.Double(xPos, yPos);
		return new BookshelfPin(location, net, splited[0]);
	}

	private NetDesc parseNetDesc(String line) {
		NetDesc result = new NetDesc();

		String[] splited = line.split(" ");
		result.elements = Integer.parseInt(splited[2]);
		result.netName = splited[4];

		return result;
	}

	private class NetDesc {
		public String netName;
		public int elements;
	}

	public static class BookshelfNet {
		private List<BookshelfNode> nodes;
		private List<BookshelfPin> pins;
		private String name;

		public BookshelfNet(String name) {
			nodes = CollectionFactory.createArrayList();
			pins = CollectionFactory.createArrayList();
			this.name = name;
		}

		/**
		 * @param nodes
		 *            the nodes to set
		 */
		public void setNodes(List<BookshelfNode> nodes) {
			this.nodes = nodes;
		}

		/**
		 * @return the nodes
		 */
		public List<BookshelfNode> getNodes() {
			return nodes;
		}

		/**
		 * @param pins
		 *            the pins to set
		 */
		public void setPins(List<BookshelfPin> pins) {
			this.pins = pins;
		}

		/**
		 * @return the pins
		 */
		public List<BookshelfPin> getPins() {
			return pins;
		}
	}

}
