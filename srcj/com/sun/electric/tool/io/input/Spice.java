/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Spice.java
 * Input/output tool: reader for Spice netlists (.spi)
 * Inspired by Graham Petley, written by Steven M. Rubin, Sun Microsystems.
 *
 * Copyright (c) 2009 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.io.input;

import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.geometry.Orientation;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.id.CellId;
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.technology.technologies.Schematics;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.IconParameters;
import com.sun.electric.tool.placement.Placement;
import com.sun.electric.tool.placement.PlacementFrame;
import com.sun.electric.tool.placement.Placement.PlacementPreferences;
import com.sun.electric.tool.placement.PlacementFrame.PlacementExport;
import com.sun.electric.tool.placement.PlacementFrame.PlacementNetwork;
import com.sun.electric.tool.placement.PlacementFrame.PlacementNode;
import com.sun.electric.tool.placement.PlacementFrame.PlacementPort;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Class for reading and displaying Spice decks (.spi files).
 */
public class Spice extends Input
{
	private Map<String,SubcktDef> allCells = new HashMap<String,SubcktDef>();

	private SpicePreferences localPrefs;

	public static class SpicePreferences extends InputPreferences
    {
		public String placementAlgorithm;
        public IconParameters iconParameters = IconParameters.makeInstance(false);

        public SpicePreferences(boolean factory)
        {
            super(factory);
        }

		public void initFromUserDefaults()
		{
			placementAlgorithm = Placement.getAlgorithmName();
            iconParameters.initFromUserDefaults();
		}

        @Override
        public Library doInput(URL fileURL, Library lib, Technology tech, Map<Library,Cell> currentCells, Map<CellId,BitSet> nodesToExpand, Job job)
        {
        	Spice in = new Spice(this);
			in.readDirectory(fileURL, lib);
			return lib;
        }
    }

	/**
	 * Creates a new instance of Spice.
	 */
	Spice(SpicePreferences ap) { localPrefs = ap; }

	/**
	 * Method to import a library from disk.
	 * @param lib the library to fill
	 */
	public void readDirectory(URL dirURL, Library lib)
	{
		try
		{
			String dirName = dirURL.getPath();
			File dir = new File(dirName);
			if (dir == null)
			{
				System.out.println("Unable to find files in directory " + dirName);
				return;
			}
			String [] filesInDir = dir.list();
			for(int i=0; i<filesInDir.length; i++)
			{
				String fileName = filesInDir[i];
				int lastDotPos = fileName.lastIndexOf('.');
				if (lastDotPos < 0) continue;
				String ext = fileName.substring(lastDotPos+1).toLowerCase();
				if (ext.equals("spi") || ext.equals("sp"))
				{
					URL url = TextUtils.makeURLToFile(dirName + fileName);
					if (url == null) continue;
					if (openTextInput(url)) continue;
					System.out.println("Reading " + fileName);
					readSimFile();
					closeInput();
				}
			}
		} catch (IOException e) {}

		// file read, now create all circuitry
		placeCells(lib);
	}

	private void readSimFile()
		throws IOException
	{
		SubcktDef current = null;
		for(;;)
		{
			String line = getNextLine();
			if (line == null) break;
			line = line.trim();
			if (line.length() == 0) continue;
			if (line.charAt(0) == '*') continue;

			// handle ".subckt" header
			String lineLC = line.toLowerCase();
			if (lineLC.startsWith(".subckt"))
			{
				String[] expPieces = line.split(" ");
				current = new SubcktDef();
				allCells.put(expPieces[1], current);
				for(int i=2; i<expPieces.length; i++) current.exports.add(expPieces[i]);
				continue;
			}
			if (lineLC.startsWith(".ends")) current = null;

			// handle transistors
			if (lineLC.charAt(0) == 'm' && current != null)
			{
				TransistorDef td = new TransistorDef();
				current.transistors.add(td);
				String[] traPieces = line.split(" ");
				if (traPieces.length < 6)
				{
					System.out.println("Error on Transistor line: " + line);
					continue;
				}
				td.name = traPieces[0].substring(1);
				td.drain = traPieces[1];
				td.gate = traPieces[2];
				td.source = traPieces[3];
				td.bias = traPieces[4];
				td.type = traPieces[5].toLowerCase();
				for(int i=6; i<traPieces.length; i++)
				{
					if (traPieces[i].toLowerCase().startsWith("w="))
						td.width = traPieces[i].substring(2);
					if (traPieces[i].toLowerCase().startsWith("l="))
						td.length = traPieces[i].substring(2);
				}
				continue;
			}

			// handle resistors
			if (lineLC.charAt(0) == 'r' && current != null)
			{
				ResistorDef rd = new ResistorDef();
				current.resistors.add(rd);
				String[] resPieces = line.split(" ");
				if (resPieces.length < 3)
				{
					System.out.println("Error on Resistor line: " + line);
					continue;
				}
				rd.name = resPieces[0].substring(1);
				rd.left = resPieces[1];
				rd.right = resPieces[2];
				if (resPieces.length >= 4)
					rd.resistance = resPieces[3];
				continue;
			}

			// handle capacitors
			if (lineLC.charAt(0) == 'c' && current != null)
			{
				CapacitorDef cd = new CapacitorDef();
				current.capacitors.add(cd);
				String[] capPieces = line.split(" ");
				if (capPieces.length < 3)
				{
					System.out.println("Error on Capacitor line: " + line);
					continue;
				}
				cd.name = capPieces[0].substring(1);
				cd.top = capPieces[1];
				cd.bottom = capPieces[2];
				if (capPieces.length >= 4)
					cd.capacitance = capPieces[3];
				continue;
			}

			// handle instances
			if (lineLC.charAt(0) == 'x' && current != null)
			{
				InstanceDef id = new InstanceDef();
				current.instances.add(id);
				String[] iPieces = line.split(" ");
				if (iPieces.length < 2)
				{
					System.out.println("Error on Instance line: " + line);
					continue;
				}
				id.name = iPieces[0].substring(1);
				id.instName = iPieces[iPieces.length-1];
				for(int i=1; i<iPieces.length-1; i++) id.signals.add(iPieces[i]);
				continue;
			}
		}
	}

	private void placeCells(Library lib)
	{
		PlacementPreferences prefs = new PlacementPreferences(false);
		prefs.placementAlgorithm = localPrefs.placementAlgorithm;
		PlacementFrame pla = Placement.getCurrentPlacementAlgorithm(prefs);
		System.out.println("Placing cells using the " + pla.getAlgorithmName() + " algorithm");

		// make icons
		for(String name : allCells.keySet())
		{
			SubcktDef sd = allCells.get(name);
			Cell cell = Cell.makeInstance(lib, name + "{ic}");
			sd.iconCell = cell;
			double yPos = 1;
			double lowY = yPos;
			for(String export : sd.exports)
			{
				PrimitiveNode np = Schematics.tech().wirePinNode;
				EPoint center1 = new EPoint(0, yPos);
				EPoint center2 = new EPoint(2, yPos);
				NodeInst ni1 = NodeInst.makeInstance(np, center1, np.getDefWidth(), np.getDefHeight(), cell);
				PortInst pi1 = ni1.getOnlyPortInst();
				NodeInst ni2 = NodeInst.makeInstance(np, center2, np.getDefWidth(), np.getDefHeight(), cell);
				PortInst pi2 = ni2.getOnlyPortInst();
				ArcProto ap = Schematics.tech().wire_arc;
				ArcInst.makeInstance(ap, pi1, pi2);
				Export e = Export.newInstance(cell, pi1, export, PortCharacteristic.UNKNOWN, localPrefs.iconParameters);
				TextDescriptor newTD = e.getTextDescriptor(Export.EXPORT_NAME).withPos(TextDescriptor.Position.LEFT);
				e.setTextDescriptor(Export.EXPORT_NAME, newTD);
				yPos += 2;
			}
			if (yPos == lowY) yPos += 2;
			double width = 10;
			double height = yPos-lowY;
			EPoint center = new EPoint(7, (yPos-2+lowY)/2);
			PrimitiveNode np = Artwork.tech().boxNode;
			NodeInst ni = NodeInst.makeInstance(np, center, width, height, cell);
			ni.setName(name.replaceAll("@", "_"));
			TextDescriptor newTD = ni.getTextDescriptor(NodeInst.NODE_NAME).withRelSize(2);
			ni.setTextDescriptor(NodeInst.NODE_NAME, newTD);
		}

		// now make the schematic cells
		for(String name : allCells.keySet())
		{
			SubcktDef sd = allCells.get(name);

			// use the placement tool to situate the objects
			List<PlacementNode> nodesToPlace = new ArrayList<PlacementNode>();
			Map<String,PlacementNetwork> allNetworks = new HashMap<String,PlacementNetwork>();
			List<PlacementExport> exportsToPlace = new ArrayList<PlacementExport>();

			// make PlacementNodes for the transistors
			for(TransistorDef td : sd.transistors)
			{
				int bits = 0;
				if (td.type.equals("p")) bits = Schematics.getPrimitiveFunctionBits(PrimitiveNode.Function.TRAPMOS); else
					bits = Schematics.getPrimitiveFunctionBits(PrimitiveNode.Function.TRANMOS);
				PrimitiveNode np = Schematics.tech().transistorNode;
				NodeInst niDummy = NodeInst.makeDummyInstance(np);

				List<PlacementPort> ports = new ArrayList<PlacementPort>();
				PortInst piSource = niDummy.getTransistorSourcePort();
				ports.add(addPlacementPort(niDummy, piSource, td.source, allNetworks, sd.exports, sd.usedExports, exportsToPlace));
				PortInst piGate = niDummy.getTransistorGatePort();
				ports.add(addPlacementPort(niDummy, piGate, td.gate, allNetworks, sd.exports, sd.usedExports, exportsToPlace));
				PortInst piDrain = niDummy.getTransistorDrainPort();
				ports.add(addPlacementPort(niDummy, piDrain, td.drain, allNetworks, sd.exports, sd.usedExports, exportsToPlace));

				PlacementNode plNode = new PlacementNode(np, "m" + td.name, bits, np.getDefWidth(), np.getDefHeight(), ports);
				nodesToPlace.add(plNode);
				for(PlacementPort plPort : ports)
					plPort.setPlacementNode(plNode);
				plNode.setOrientation(Orientation.IDENT);
				if (td.width != null) plNode.addVariable(Schematics.ATTR_WIDTH, td.width);
				if (td.length != null) plNode.addVariable(Schematics.ATTR_LENGTH, td.length);
			}

			// place resistors and capacitors
			for(ResistorDef rd : sd.resistors)
			{
				PrimitiveNode np = Schematics.tech().resistorNode;
				NodeInst niDummy = NodeInst.makeDummyInstance(np);

				List<PlacementPort> ports = new ArrayList<PlacementPort>();
				PortInst piSource = niDummy.getPortInst(0);
				ports.add(addPlacementPort(niDummy, piSource, rd.left, allNetworks, sd.exports, sd.usedExports, exportsToPlace));
				PortInst piGate = niDummy.getPortInst(1);
				ports.add(addPlacementPort(niDummy, piGate, rd.right, allNetworks, sd.exports, sd.usedExports, exportsToPlace));

				PlacementNode plNode = new PlacementNode(np, "r" + rd.name, 0, np.getDefWidth(), np.getDefHeight(), ports);
				nodesToPlace.add(plNode);
				for(PlacementPort plPort : ports)
					plPort.setPlacementNode(plNode);
				plNode.setOrientation(Orientation.IDENT);
				if (rd.resistance != null) plNode.addVariable(Schematics.SCHEM_RESISTANCE, rd.resistance);
			}
			for(CapacitorDef cd : sd.capacitors)
			{
				PrimitiveNode np = Schematics.tech().capacitorNode;
				NodeInst niDummy = NodeInst.makeDummyInstance(np);

				List<PlacementPort> ports = new ArrayList<PlacementPort>();
				PortInst piSource = niDummy.getPortInst(0);
				ports.add(addPlacementPort(niDummy, piSource, cd.top, allNetworks, sd.exports, sd.usedExports, exportsToPlace));
				PortInst piGate = niDummy.getPortInst(1);
				ports.add(addPlacementPort(niDummy, piGate, cd.bottom, allNetworks, sd.exports, sd.usedExports, exportsToPlace));

				PlacementNode plNode = new PlacementNode(np, "c" + cd.name, 0, np.getDefWidth(), np.getDefHeight(), ports);
				nodesToPlace.add(plNode);
				for(PlacementPort plPort : ports)
					plPort.setPlacementNode(plNode);
				plNode.setOrientation(Orientation.IDENT);
				if (cd.capacitance != null) plNode.addVariable(Schematics.SCHEM_CAPACITANCE, cd.capacitance);
			}

			// place instances
			for(InstanceDef id : sd.instances)
			{
				SubcktDef subSD = allCells.get(id.instName);
				if (subSD == null)
				{
					System.out.println("Cannot find subcircuit "+id.instName);
					continue;
				}
				Cell np = subSD.iconCell;
				NodeInst niDummy = NodeInst.makeDummyInstance(np);
				if (subSD.exports.size() != id.signals.size())
				{
					System.out.println("Error: Subcircuit " + id.instName + " has " + subSD.exports.size() +
						" exports but instance " + id.name + " of it in cell " + name + "{sch}" +
						" has " + id.signals.size() + " signals on it");
					continue;
				}
				List<PlacementPort> ports = new ArrayList<PlacementPort>();
				for(int i=0; i<subSD.exports.size(); i++)
				{
					PortProto pp = subSD.iconCell.findPortProto(subSD.exports.get(i));
					if (pp == null) continue;
					PortInst pi = niDummy.findPortInstFromProto(pp);
					String sigName = id.signals.get(i);
					ports.add(addPlacementPort(niDummy, pi, sigName, allNetworks, sd.exports, sd.usedExports, exportsToPlace));
				}

				PlacementNode plNode = new PlacementNode(np, id.name.replace('@', '_'), 0, np.getDefWidth(), np.getDefHeight(), ports);
				nodesToPlace.add(plNode);
				for(PlacementPort plPort : ports)
					plPort.setPlacementNode(plNode);
				plNode.setOrientation(Orientation.IDENT);
			}

			// add dummy pins for unused exports
			for(String export : sd.exports)
			{
				if (sd.usedExports.contains(export)) continue;
				PrimitiveNode np = Schematics.tech().wirePinNode;
				NodeInst niDummy = NodeInst.makeDummyInstance(np);
				List<PlacementPort> ports = new ArrayList<PlacementPort>();
				PortInst pi = niDummy.getPortInst(0);
				ports.add(addPlacementPort(niDummy, pi, export, allNetworks, sd.exports, sd.usedExports, exportsToPlace));

				PlacementNode plNode = new PlacementNode(np, null, 0, np.getDefWidth(), np.getDefHeight(), ports);
				nodesToPlace.add(plNode);
				for(PlacementPort plPort : ports)
					plPort.setPlacementNode(plNode);
				plNode.setOrientation(Orientation.IDENT);
			}

			// make a list of PlacementNetworks
			List<PlacementNetwork> nets = new ArrayList<PlacementNetwork>();
			for(String netName : allNetworks.keySet())
			{
				PlacementNetwork net = allNetworks.get(netName);
				for(PlacementPort port : net.getPortsOnNet())
					port.setPlacementNetwork(net);
				if (net.getPortsOnNet() == null || net.getPortsOnNet().size() <= 1) continue;
				nets.add(net);
			}

			// run placement
			pla.doPlacement(lib, name + "{sch}", nodesToPlace, nets, exportsToPlace, sd.iconCell, localPrefs.iconParameters);

			// the old way...
//			Cell cell = Cell.makeInstance(lib, name + "{sch}");
//			if (sd.iconCell != null)
//			{
//				EPoint center = new EPoint(0, 15);
//				NodeInst.makeInstance(sd.iconCell, center, sd.iconCell.getDefWidth(), sd.iconCell.getDefHeight(), cell);
//			}
//
//			// place transistors
//			double pPos = 0, nPos = 0;
//			for(TransistorDef td : sd.transistors)
//			{
//				EPoint center = null;
//				PrimitiveNode.Function func = null;
//				if (td.type.equals("p"))
//				{
//					center = new EPoint(pPos, 5);
//					pPos += 10;
//					func = PrimitiveNode.Function.TRAPMOS;
//				} else
//				{
//					center = new EPoint(nPos, -5);
//					nPos += 10;
//					func = PrimitiveNode.Function.TRANMOS;
//				}
//				PrimitiveNode np = Schematics.tech().transistorNode;
//				Orientation o = Orientation.R;
//				NodeInst ni = NodeInst.makeInstance(np, center, np.getDefWidth(), np.getDefHeight(),
//					cell, o, "m" + td.name, func);
//				TextDescriptor tdesc = TextDescriptor.newTextDescriptor(MutableTextDescriptor.getNodeTextDescriptor());
//		        TextDescriptor.Rotation r = TextDescriptor.Rotation.getRotation(270);
//				ni.setTextDescriptor(NodeInst.NODE_NAME, tdesc.withRotation(r).withPos(TextDescriptor.Position.DOWNLEFT));
//				if (td.width != null)
//				{
//					Variable var = ni.newDisplayVar(Schematics.ATTR_WIDTH, td.width);
//					TextDescriptor newTD = var.getTextDescriptor().
//						withRotation(r).withOff(0.5, -0.5).withRelSize(1).withPos(TextDescriptor.Position.DOWN);
//					ni.setTextDescriptor(Schematics.ATTR_WIDTH, newTD);
//				}
//				if (td.length != null)
//				{
//					Variable var = ni.newDisplayVar(Schematics.ATTR_LENGTH, td.length);
//					TextDescriptor newTD = var.getTextDescriptor().
//						withRotation(r).withOff(-0.5, -0.75).withRelSize(0.5).withPos(TextDescriptor.Position.DOWN);
//					ni.setTextDescriptor(Schematics.ATTR_LENGTH, newTD);
//				}
//				PortInst piSource = ni.getTransistorSourcePort();
//				addLead(piSource, 0, -2, cell, td.source, sd.exports, sd.usedExports);
//				PortInst piGate = ni.getTransistorGatePort();
//				addLead(piGate, -2, 0, cell, td.gate, sd.exports, sd.usedExports);
//				PortInst piDrain = ni.getTransistorDrainPort();
//				addLead(piDrain, 0, 2, cell, td.drain, sd.exports, sd.usedExports);
//			}
//
//			// place resistors and capacitors
//			for(ResistorDef rd : sd.resistors)
//			{
//				EPoint center = new EPoint(pPos, 5);
//				pPos += 10;
//				PrimitiveNode np = Schematics.tech().resistorNode;
//				NodeInst ni = NodeInst.makeInstance(np, center, np.getDefWidth(), np.getDefHeight(),
//					cell, Orientation.IDENT, "r" + rd.name);
//				if (rd.resistance != null)
//				{
//					Variable var = ni.newDisplayVar(Schematics.SCHEM_RESISTANCE, rd.resistance);
//					TextDescriptor newTD = var.getTextDescriptor().
//						withOff(0, 0.5).withPos(TextDescriptor.Position.UP);
//					ni.setTextDescriptor(Schematics.SCHEM_RESISTANCE, newTD);
//				}
//				PortInst piSource = ni.getPortInst(0);
//				addLead(piSource, -2, 0, cell, rd.left, sd.exports, sd.usedExports);
//				PortInst piGate = ni.getPortInst(1);
//				addLead(piGate, 2, 0, cell, rd.right, sd.exports, sd.usedExports);
//			}
//			for(CapacitorDef cd : sd.capacitors)
//			{
//				EPoint center = new EPoint(nPos, -5);
//				nPos += 10;
//				PrimitiveNode np = Schematics.tech().capacitorNode;
//				NodeInst ni = NodeInst.makeInstance(np, center, np.getDefWidth(), np.getDefHeight(),
//					cell, Orientation.IDENT, "c" + cd.name);
//				if (cd.capacitance != null)
//					ni.newDisplayVar(Schematics.SCHEM_CAPACITANCE, cd.capacitance);
//				PortInst piSource = ni.getPortInst(0);
//				addLead(piSource, 0, 2, cell, cd.top, sd.exports, sd.usedExports);
//				PortInst piGate = ni.getPortInst(1);
//				addLead(piGate, 0, -2, cell, cd.bottom, sd.exports, sd.usedExports);
//			}
//
//			// place instances
//			double iPos = 0;
//			for(InstanceDef id : sd.instances)
//			{
//				SubcktDef subSD = allCells.get(id.instName);
//				if (subSD == null)
//				{
//					System.out.println("Cannot find subcircuit "+id.instName);
//					continue;
//				}
//				Cell np = subSD.iconCell;
//				EPoint center = new EPoint(iPos, -10-np.getDefHeight());
//				iPos += 30;
//				NodeInst ni = NodeInst.makeInstance(np, center, np.getDefWidth(), np.getDefHeight(), cell);
//				ni.setName(id.name.replace('@', '_'));
//				TextDescriptor newTD = ni.getTextDescriptor(NodeInst.NODE_NAME).
//					withOff(1, -1).withPos(TextDescriptor.Position.DOWN);
//				ni.setTextDescriptor(NodeInst.NODE_NAME, newTD);
//				if (subSD.exports.size() != id.signals.size())
//				{
//					System.out.println("Error: Subcircuit " + id.instName + " has " + subSD.exports.size() +
//						" exports but instance " + id.name + " of it in cell " + cell.describe(false) +
//						" has " + id.signals.size() + " signals on it");
//					continue;
//				}
//				for(int i=0; i<subSD.exports.size(); i++)
//				{
//					PortProto pp = subSD.iconCell.findPortProto(subSD.exports.get(i));
//					if (pp == null) continue;
//					PortInst pi = ni.findPortInstFromProto(pp);
//					String sigName = id.signals.get(i);
//					addLead(pi, -2, 0, cell, sigName, sd.exports, sd.usedExports);
//				}
//			}
//
//			// add dummy pins for unused exports
//			double dummyPos = 30;
//			for(String export : sd.exports)
//			{
//				if (sd.usedExports.contains(export)) continue;
//				EPoint ctr = new EPoint(dummyPos, 15);
//				dummyPos += 10;
//				PrimitiveNode np = Schematics.tech().wirePinNode;
//				NodeInst ni = NodeInst.makeInstance(np, ctr, np.getDefWidth(), np.getDefHeight(), cell);
//				Export.newInstance(cell, ni.getOnlyPortInst(), export, PortCharacteristic.UNKNOWN);
//			}
		}
	}

	private PlacementPort addPlacementPort(NodeInst ni, PortInst pi, String name, Map<String,PlacementNetwork> allNetworks,
		List<String> exports, Set<String> usedExports, List<PlacementExport> exportsToPlace)
	{
		PlacementNetwork net = allNetworks.get(name);
		if (net == null) allNetworks.put(name, net = new PlacementNetwork(new ArrayList<PlacementPort>()));
		List<PlacementPort> portsOnNet = net.getPortsOnNet();

		Poly poly = pi.getPoly();
		double offX = poly.getCenterX() - ni.getTrueCenterX();
		double offY = poly.getCenterY() - ni.getTrueCenterY();
		PlacementPort plPort = new PlacementPort(offX, offY, pi.getPortProto());
		portsOnNet.add(plPort);

		// see if this port is exported
		if (exports.contains(name) && !usedExports.contains(name))
		{
			usedExports.add(name);
			PlacementExport plExport = new PlacementExport(plPort, name, PortCharacteristic.UNKNOWN);
			exportsToPlace.add(plExport);
		}
		return plPort;
	}

//	private void addLead(PortInst pi, double dX, double dY, Cell cell, String name, List<String> exports, Set<String> usedExports)
//	{
//		EPoint ctr1 = pi.getCenter();
//		EPoint ctr2 = new EPoint(ctr1.getX()+dX, ctr1.getY()+dY);
//
//		PrimitiveNode np = Schematics.tech().wirePinNode;
//		NodeInst ni = NodeInst.makeInstance(np, ctr2, np.getDefWidth(), np.getDefHeight(), cell);
//		ArcProto ap = Schematics.tech().wire_arc;
//		PortInst pi2 = ni.getOnlyPortInst();
//		ArcInst ai = ArcInst.makeInstance(ap, pi, pi2);
//
//		name = name.replace('@', '_');
//		if (exports.contains(name) && !usedExports.contains(name))
//		{
//			usedExports.add(name);
//			Export.newInstance(cell, pi2, name, PortCharacteristic.UNKNOWN);
//		} else
//		{
//			ai.setName(name);
//			TextDescriptor.Position p = (dX == 0) ? TextDescriptor.Position.LEFT : TextDescriptor.Position.DOWN;
//			TextDescriptor newTD = ai.getTextDescriptor(ArcInst.ARC_NAME).withPos(p);
//			ai.setTextDescriptor(ArcInst.ARC_NAME, newTD);
//		}
//	}

	private String lastLine = null;

	private String getNextLine()
		throws IOException
	{
		String line = lastLine;
		if (line == null) line = getLine();
		if (line != null)
		{
			lastLine = getLine();
			while (lastLine != null && lastLine.startsWith("+"))
			{
				line += lastLine.substring(1);
				lastLine = getLine();
			}
		}
		return line;
	}

	private static class SubcktDef
	{
		List<String> exports;
		List<TransistorDef> transistors;
		List<ResistorDef> resistors;
		List<CapacitorDef> capacitors;
		List<InstanceDef> instances;
		Set<String> usedExports;
		Cell iconCell;

		public SubcktDef()
		{
			exports = new ArrayList<String>();
			transistors = new ArrayList<TransistorDef>();
			resistors = new ArrayList<ResistorDef>();
			capacitors = new ArrayList<CapacitorDef>();
			instances = new ArrayList<InstanceDef>();
			usedExports = new HashSet<String>();
		}
	}

	private static class InstanceDef
	{
		String name;
		String instName;
		List<String> signals;

		public InstanceDef()
		{
			signals = new ArrayList<String>();
		}
	}

	private static class TransistorDef
	{
		String name;
		String source, gate, drain;
		String bias;
		String type;
		String width, length;
	}

	private static class ResistorDef
	{
		String name;
		String left, right;
		String resistance;
	}

	private static class CapacitorDef
	{
		String name;
		String top, bottom;
		String capacitance;
	}
}
