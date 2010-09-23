/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: PlacementAdapter.java
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
package com.sun.electric.tool.placement;

import com.sun.electric.database.ImmutableArcInst;
import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.geometry.ERectangle;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.PrefPackage;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.technology.technologies.Schematics;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.placement.PlacementFrame.PlacementNetwork;
import com.sun.electric.tool.placement.forceDirected1.PlacementForceDirectedTeam5;
import com.sun.electric.tool.placement.forceDirected2.PlacementForceDirectedStaged;
import com.sun.electric.tool.placement.genetic1.g1.GeneticPlacement;
import com.sun.electric.tool.placement.genetic2.PlacementGenetic;
import com.sun.electric.tool.placement.metrics.AbstractMetric;
import com.sun.electric.tool.placement.metrics.boundingbox.BBMetric;
import com.sun.electric.tool.placement.metrics.mst.MSTMetric;
import com.sun.electric.tool.placement.simulatedAnnealing1.SimulatedAnnealing;
import com.sun.electric.tool.placement.simulatedAnnealing2.PlacementSimulatedAnnealing;
import com.sun.electric.tool.util.concurrent.utils.ElapseTimer;
import com.sun.electric.util.CollectionFactory;
import com.sun.electric.util.math.Orientation;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;

/**
 * PlacementExport describes exports in the cell. Placement algorithms do not
 * usually need this information: it exists as a way to communicate the
 * information internally.
 */
public class PlacementAdapter {

   	private static final boolean specialDebugFlag = false;

    public static class PlacementPrefs extends PrefPackage {
        private Object[][] values;
        private final static String NODE_NAME = "tool/placement";

        public PlacementPrefs(boolean factory) {
            super(factory);
            Preferences prefs = (factory ? getFactoryPrefRoot() : getPrefRoot()).node(NODE_NAME);
            values = new Object[placementAlgorithms.length][];
            for (int i = 0; i < values.length; i++) {
                PlacementFrame pf = placementAlgorithms[i];
                values[i] = new Object[pf.getParameters().size()];
                for (int j = 0; j < pf.getParameters().size(); j++) {
                    PlacementFrame.PlacementParameter par = pf.getParameters().get(j);
                    String key = pf.getAlgorithmName() + "-" + par.key;
                    Object value;
                    switch (par.getType()) {
                        case PlacementFrame.PlacementParameter.TYPEINTEGER:
                            value = Integer.valueOf(prefs.getInt(key, ((Integer)par.factoryValue).intValue()));
                            break;
                        case PlacementFrame.PlacementParameter.TYPESTRING:
                            value = String.valueOf(prefs.get(key, (String)par.factoryValue));
                            break;
                        case PlacementFrame.PlacementParameter.TYPEDOUBLE:
                            value = Double.valueOf(prefs.getDouble(key, ((Double)par.factoryValue).doubleValue()));
                            break;
                        default:
                            throw new AssertionError();
                    }
                    if (value.equals(par.factoryValue)) {
                        value = par.factoryValue;
                    }
                    values[i][j] = value;
                }
            }
        }

        /**
         * Store annotated option fields of the subclass into the speciefied Preferences subtree.
         * @param prefRoot the root of the Preferences subtree.
         * @param removeDefaults remove from the Preferences subtree options which have factory default value.
         */
        @Override
        protected void putPrefs(Preferences prefRoot, boolean removeDefaults) {
            super.putPrefs(prefRoot, removeDefaults);
            Preferences prefs = prefRoot.node(NODE_NAME);
            assert values.length == placementAlgorithms.length;
            for (int i = 0; i < values.length; i++) {
                PlacementFrame pf = placementAlgorithms[i];
                assert values[i].length == pf.getParameters().size();
                for (int j = 0; j < pf.getParameters().size(); j++) {
                    PlacementFrame.PlacementParameter par = pf.getParameters().get(j);
                    String key = pf.getAlgorithmName() + "-" + par.key;
                    Object v = values[i][j];
                    if (removeDefaults && v.equals(par.factoryValue)) {
                        prefs.remove(key);
                    } else {
                        switch (par.getType()) {
                            case PlacementFrame.PlacementParameter.TYPEINTEGER:
                                prefs.putInt(key, ((Integer)v).intValue());
                                break;
                            case PlacementFrame.PlacementParameter.TYPESTRING:
                                prefs.put(key, (String)v);
                                break;
                            case PlacementFrame.PlacementParameter.TYPEDOUBLE:
                                prefs.putDouble(key, ((Double)v).doubleValue());
                                break;
                            default:
                                throw new AssertionError();
                        }
                    }
                }
            }
        }

        public Object getParameter(PlacementFrame.PlacementParameter par) {
            int i = indexOfFrame(par);
            PlacementFrame pf = placementAlgorithms[i];
            int j = indexOfParameter(pf, par.key);
            return values[i][j];
        }

        public PlacementPrefs withParameter(PlacementFrame.PlacementParameter par, Object value) {
            int i = indexOfFrame(par);
            PlacementFrame pf = placementAlgorithms[i];
            int j = indexOfParameter(pf, par.key);
            Object oldValue = values[i][j];
            if (oldValue.equals(value)) {
                return this;
            }
            assert value.getClass() == par.factoryValue.getClass();
            if (value.equals(par.factoryValue))
                value = par.factoryValue;
            Object[] vs = values[i].clone();
            vs[j] = value;
            Object[][] newValues = values.clone();
            newValues[i] = vs;
            return (PlacementPrefs)withField("values", newValues);
        }

        private int indexOfFrame(PlacementFrame.PlacementParameter par) {
            PlacementFrame rf = par.getOwner();
            for (int i = 0; i < placementAlgorithms.length; i++) {
                if (rf.getClass() == placementAlgorithms[i].getClass()) return i;
            }
            return -1;
        }

        private static int indexOfParameter(PlacementFrame pf, String parameterKey) {
            for (int j = 0; j < pf.getParameters().size(); j++) {
                if (parameterKey.equals(pf.getParameters().get(j).key))
                    return j;
            }
            return -1;
        }
    }

    public static void setValue(PlacementFrame.PlacementParameter par, Object value) {
        par.setValue(value);
    }

	/**
	 * Class to define a node that is being placed. This is a shadow class for
	 * the internal Electric object "NodeInst". There are minor differences
	 * between PlacementNode and NodeInst, for example, PlacementNode is
	 * presumed to be centered in the middle, with port offsets based on that
	 * center, whereas the NodeInst has a cell-center that may not be in the
	 * middle.
	 */
	public static class PlacementNode extends PlacementFrame.PlacementNode {
		private final NodeProto original;
		private final String nodeName;
		private final int techBits;
		private final double width, height;
		private final List<PlacementFrame.PlacementPort> ports;
		private Map<String, Object> addedVariables;
		private final boolean terminal;

		/**
		 * Method to create a PlacementNode object.
		 *
		 * @param type
		 *            the original Electric type of this PlacementNode.
		 * @param name
		 *            the name to give the node once placed (can be null).
		 * @param tBits
		 *            the technology-specific bits of this PlacementNode
		 *            (typically 0 except for specialized Schematics
		 *            components).
		 * @param wid
		 *            the width of this PlacementNode.
		 * @param hei
		 *            the height of this PlacementNode.
		 * @param pps
		 *            a list of PlacementPort on the PlacementNode, indicating
		 *            connection locations.
		 * @param terminal
		 */
		public PlacementNode(NodeProto type, String name, int tBits, double wid, double hei, List<PlacementPort> pps,
				boolean terminal) {
            original = type;
			nodeName = name;
            techBits = tBits;
            width = wid;
            height = hei;
            ports = new ArrayList<PlacementFrame.PlacementPort>(pps);
            this.terminal = terminal;
        }

		/**
		 * Method to add variables to this PlacementNode. Variables are extra
		 * name/value pairs, for example a transistor width and length.
		 *
		 * @param name
		 *            the name of the variable to add.
		 * @param value
		 *            the value of the variable to add.
		 */
		public void addVariable(String name, Object value) {
			if (addedVariables == null)
				addedVariables = new HashMap<String, Object>();
			addedVariables.put(name, value);
		}

		/**
		 * Method to return the NodeProto of this PlacementNode.
		 *
		 * @return the NodeProto of this PlacementNode.
		 */
		public NodeProto getType() {
			return original;
		}

		/**
		 * Method to return the name of NodeProto of this PlacementNode.
		 *
		 * @return the name NodeProto of this PlacementNode.
		 */
		public String getTypeName() {
			return original.getName();
		}

		/**
		 * Method to return a list of PlacementPorts on this PlacementNode.
		 *
		 * @return a list of PlacementPorts on this PlacementNode.
		 */
		public List<PlacementFrame.PlacementPort> getPorts() {
			return ports;
		}

		/**
		 * Method to return the technology-specific information of this
		 * PlacementNode.
		 *
		 * @return the technology-specific information of this PlacementNode
		 *         (typically 0 except for specialized Schematics components).
		 */
		public int getTechBits() {
			return techBits;
		}

		/**
		 * Method to return the width of this PlacementNode.
		 *
		 * @return the width of this PlacementNode.
		 */
        @Override
		public double getWidth() {
			return width;
		}

		/**
		 * Method to return the height of this PlacementNode.
		 *
		 * @return the height of this PlacementNode.
		 */
        @Override
		public double getHeight() {
			return height;
		}

		/**
		 * @return the terminal
		 */
		public boolean isTerminal() {
			return terminal;
		}
        
        @Override
		public String toString() {
			String name = original.describe(false);
			if (nodeName != null)
				name += "[" + nodeName + "]";
			if (getTechBits() != 0)
				name += "(" + getTechBits() + ")";
			return name;
		}
    }

	/**
	 * Class to define ports on PlacementNode objects. This is a shadow class
	 * for the internal Electric object "PortInst".
	 */
	public static class PlacementPort extends PlacementFrame.PlacementPort {
		private PortProto proto;

		/**
		 * Constructor to create a PlacementPort.
		 *
		 * @param x
		 *            the X offset of this PlacementPort from the center of its
		 *            PlacementNode.
		 * @param y
		 *            the Y offset of this PlacementPort from the center of its
		 *            PlacementNode.
		 * @param pp
		 *            the Electric PortProto of this PlacementPort.
		 */
		public PlacementPort(double x, double y, PortProto pp) {
            super(x, y);
            proto = pp;
		}

		/**
		 * Method to return the Electric PortProto that this PlacementPort uses.
		 *
		 * @return the Electric PortProto that this PlacementPort uses.
		 */
		PortProto getPortProto() {
			return proto;
		}

		public String toString() {
			return proto.getName();
		}
    }

	/**
	 * Class to define an Export that will be placed in the circuit.
	 */
	public static class PlacementExport {
		private PlacementPort portToExport;
		private String exportName;
		private PortCharacteristic characteristic;

		/**
		 * Constructor to create a PlacementExport with the information about an
		 * Export to be created.
		 *
		 * @param port
		 *            the PlacementPort that is being exported.
		 * @param name
		 *            the name to give the Export.
		 * @param chr
		 *            the PortCharacteristic (input, output, etc.) to give the
		 *            Export.
		 */
		public PlacementExport(PlacementPort port, String name, PortCharacteristic chr) {
			portToExport = port;
			exportName = name;
			characteristic = chr;
		}

		PlacementPort getPort() {
			return portToExport;
		}

		String getName() {
			return exportName;
		}

		PortCharacteristic getCharacteristic() {
			return characteristic;
		}
	}

	/**
	 * Static list of all Placement algorithms. When you create a new algorithm,
	 * add it to the following list.
	 */
	private static PlacementFrame[] placementAlgorithms = { new SimulatedAnnealing(), // team
			// 2
			new PlacementSimulatedAnnealing(), // team 6
			new GeneticPlacement(), // team 3
			new PlacementGenetic(), // team 4
			new PlacementForceDirectedTeam5(), // team 5
			new PlacementForceDirectedStaged(), // team 7
			new PlacementMinCut(), new PlacementSimple(), new PlacementRandom() };

	/**
	 * Method to return a list of all Placement algorithms.
	 *
	 * @return a list of all Placement algorithms.
	 */
	public static PlacementFrame[] getPlacementAlgorithms() {
		return placementAlgorithms;
	}

	/**
	 * Entry point to do Placement of a Cell and create a new, placed Cell.
	 * Gathers the requirements for Placement into a collection of shadow
	 * objects (PlacementNode, PlacementPort, PlacementNetwork, and
	 * PlacementExport). Then invokes the alternate version of "doPlacement()"
	 * that works from shadow objedts.
	 *
	 * @param cell
	 *            the Cell to place. Objects in that Cell will be reorganized in
	 *            and placed in a new Cell.
	 * @return the new Cell with the placement results.
	 */
	public static Cell doPlacement(PlacementFrame pla, Cell cell, Placement.PlacementPreferences prefs, PlacementAdapter.PlacementPrefs placementOptions) {
        // get network information for the Cell
		Netlist netList = cell.getNetlist();
		if (netList == null) {
			System.out
					.println("Sorry, a deadlock aborted routing (network information unavailable).  Please try again");
			return null;
		}

		// convert nodes in the Cell into PlacementNode objects
		NodeProto iconToPlace = null;
		List<PlacementNode> nodesToPlace = CollectionFactory.createArrayList();
		Map<NodeInst, Map<PortProto, PlacementPort>> convertedNodes = CollectionFactory.createHashMap();
		List<PlacementExport> exportsToPlace = CollectionFactory.createArrayList();
		for (Iterator<NodeInst> it = cell.getNodes(); it.hasNext();) {
			NodeInst ni = it.next();
			if (ni.isIconOfParent()) {
				iconToPlace = ni.getProto();
				continue;
			}
			boolean validNode = ni.isCellInstance();
			if (!validNode) {
				if (ni.getProto().getTechnology() != Generic.tech()) {
					PrimitiveNode.Function fun = ni.getFunction();
					if (fun != PrimitiveNode.Function.CONNECT && fun != PrimitiveNode.Function.CONTACT && !fun.isPin())
						validNode = true;
				}
				if (ni.hasExports())
					validNode = true;
			}
			if (validNode) {
				// make a list of PlacementPorts on this NodeInst
				NodeProto np = ni.getProto();
				List<PlacementPort> pl = new ArrayList<PlacementPort>();
				Map<PortProto, PlacementPort> placedPorts = new HashMap<PortProto, PlacementPort>();
				if (ni.isCellInstance()) {
					for (Iterator<Export> eIt = ((Cell) np).getExports(); eIt.hasNext();) {
						Export e = eIt.next();
						Poly poly = e.getPoly();
						PlacementPort plPort = new PlacementPort(poly.getCenterX(), poly.getCenterY(), e);
						pl.add(plPort);
						placedPorts.put(e, plPort);
					}
				} else {
					NodeInst niDummy = NodeInst.makeDummyInstance(np);
					for (Iterator<PortInst> pIt = niDummy.getPortInsts(); pIt.hasNext();) {
						PortInst pi = pIt.next();
						Poly poly = pi.getPoly();
						double offX = poly.getCenterX() - niDummy.getTrueCenterX();
						double offY = poly.getCenterY() - niDummy.getTrueCenterY();
						PlacementPort plPort = new PlacementPort(offX, offY, pi.getPortProto());
						pl.add(plPort);
						placedPorts.put(pi.getPortProto(), plPort);
					}
				}

				// add to the list of PlacementExports
				for (Iterator<Export> eIt = ni.getExports(); eIt.hasNext();) {
					Export e = eIt.next();
					PlacementPort plPort = placedPorts.get(e.getOriginalPort().getPortProto());
					PlacementExport plExport = new PlacementExport(plPort, e.getName(), e.getCharacteristic());
					exportsToPlace.add(plExport);
				}

				// make the PlacementNode for this NodeInst
				String name = ni.getName();
				if (ni.getNameKey().isTempname())
					name = null;
				PlacementNode plNode = new PlacementNode(np, name, ni.getTechSpecific(), np.getDefWidth(), np
						.getDefHeight(), pl, ni.isLocked());

				nodesToPlace.add(plNode);
				for (PlacementFrame.PlacementPort plPort : pl)
					plPort.setPlacementNode(plNode);
				plNode.setOrientation(Orientation.IDENT);
				convertedNodes.put(ni, placedPorts);
			}
		}

		// gather connectivity information in a list of PlacementNetwork objects
		Map<Network, PortInst[]> portInstsByNetwork = null;
		if (cell.getView() != View.SCHEMATIC)
			portInstsByNetwork = netList.getPortInstsByNetwork();
		List<PlacementNetwork> allNetworks = new ArrayList<PlacementNetwork>();
		for (Iterator<Network> it = netList.getNetworks(); it.hasNext();) {
			Network net = it.next();
			List<PlacementFrame.PlacementPort> portsOnNet = new ArrayList<PlacementFrame.PlacementPort>();
			PortInst[] portInsts = null;
			if (portInstsByNetwork != null)
				portInsts = portInstsByNetwork.get(net);
			else {
				List<PortInst> portList = new ArrayList<PortInst>();
				for (Iterator<PortInst> pIt = net.getPorts(); pIt.hasNext();)
					portList.add(pIt.next());
				portInsts = portList.toArray(new PortInst[] {});
			}
			for (int i = 0; i < portInsts.length; i++) {
				PortInst pi = portInsts[i];
				NodeInst ni = pi.getNodeInst();
				PortProto pp = pi.getPortProto();
				Map<PortProto, PlacementPort> convertedPorts = convertedNodes.get(ni);
				if (convertedPorts == null)
					continue;
				PlacementPort plPort = convertedPorts.get(pp);
				if (plPort != null)
					portsOnNet.add(plPort);
			}
			if (portsOnNet.size() > 1) {
				PlacementNetwork plNet = new PlacementNetwork(portsOnNet);
				for (PlacementFrame.PlacementPort plPort : portsOnNet)
					plPort.setPlacementNetwork(plNet);
				allNetworks.add(plNet);
			}
		}

		// do the placement from the shadow objects
		Cell newCell = doPlacement(pla, cell.getLibrary(), cell.noLibDescribe(), nodesToPlace, allNetworks, exportsToPlace,
				iconToPlace, placementOptions);

		return newCell;
	}

	/**
	 * Entry point for other tools that wish to describe a network to be placed.
	 * Creates a cell with the placed network.
	 *
	 * @param lib
	 *            the Library in which to create the placed Cell.
	 * @param cellName
	 *            the name of the Cell to create.
	 * @param nodesToPlace
	 *            a List of PlacementNodes to place in the Cell.
	 * @param allNetworks
	 *            a List of PlacementNetworks to connect in the Cell.
	 * @param exportsToPlace
	 *            a List of PlacementExports to create in the Cell.
	 * @param iconToPlace
	 *            non-null to place an instance of itself (the icon) in the
	 *            Cell.
	 * @return the newly created Cell.
	 */
	public static Cell doPlacement(PlacementFrame pla, Library lib, String cellName, List<PlacementNode> nodesToPlace,
			List<PlacementNetwork> allNetworks, List<PlacementExport> exportsToPlace, NodeProto iconToPlace,
            PlacementPrefs placementOptions) {
		ElapseTimer timer = ElapseTimer.createInstance().start();
		System.out.println("Running placement on cell '" + cellName + "' using the '" + pla.getAlgorithmName()
				+ "' algorithm");

		// do the real work of placement
        for (PlacementFrame.PlacementParameter par: pla.getParameters()) {
            par.setValue(placementOptions.getParameter(par));
        }
        List<PlacementFrame.PlacementNode> nodesToPlaceCopy = new ArrayList<PlacementFrame.PlacementNode>(nodesToPlace);
        
		pla.runPlacement(nodesToPlaceCopy, allNetworks, cellName);

		if (Job.getDebug() && specialDebugFlag) {
			AbstractMetric bmetric = new BBMetric(nodesToPlaceCopy, allNetworks);
			System.out.println("### BBMetric: " + bmetric.toString());

			AbstractMetric mstMetric = new MSTMetric(nodesToPlaceCopy, allNetworks);
			System.out.println("### MSTMetric: " + mstMetric.toString());

			FileOutputStream out; // declare a file output object
			PrintStream p; // declare a print stream object

			try {
				// Create a new file output stream
				// connected to "myfile.txt"
				out = new FileOutputStream("/home/Felix Schmidt/work/" + pla.getAlgorithmName() + "-" + cellName + "-"
						+ pla.runtime + "-" + pla.numOfThreads + ".txt");

				// Connect print stream to the output stream
				p = new PrintStream(out);

				p.println(bmetric.toString());
				p.println(mstMetric.toString());

				p.close();
			} catch (Exception e) {
				System.err.println("Error writing to file");
			}

		}

		// create a new cell for the placement results
		Cell newCell = Cell.makeInstance(lib, cellName); // newCellName

		// place the nodes in the new cell
		Map<PlacementNode, NodeInst> placedNodes = new HashMap<PlacementNode, NodeInst>();
		for (PlacementNode plNode : nodesToPlace) {
			double xPos = plNode.getPlacementX();
			double yPos = plNode.getPlacementY();
			Orientation orient = plNode.getPlacementOrientation();
			NodeProto np = plNode.getType();
			if (np instanceof Cell) {
				Cell placementCell = (Cell) np;
				Rectangle2D bounds = placementCell.getBounds();
				Point2D centerOffset = new Point2D.Double(bounds.getCenterX(), bounds.getCenterY());
				orient.pureRotate().transform(centerOffset, centerOffset);
				xPos -= centerOffset.getX();
				yPos -= centerOffset.getY();
			}
			NodeInst ni = NodeInst.makeInstance(np, new Point2D.Double(xPos, yPos), np.getDefWidth(),
					np.getDefHeight(), newCell, orient, plNode.nodeName, plNode.getTechBits());
			if (ni == null)
				System.out.println("Placement failed to create node");
			else {
				if(plNode.isTerminal())
					ni.setLocked();
				placedNodes.put(plNode, ni);
			}
			if (plNode.addedVariables != null) {
				for (String varName : plNode.addedVariables.keySet()) {
					Object value = plNode.addedVariables.get(varName);
                    Variable.Key key = Variable.newKey(varName);
					Variable var = ni.newDisplayVar(key, value);
					if (key == Schematics.SCHEM_RESISTANCE) {
						ni.setTextDescriptor(key, var.getTextDescriptor().withOff(0, 0.5).withDispPart(
								TextDescriptor.DispPos.VALUE));
					} else if (key == Schematics.ATTR_WIDTH) {
						ni.setTextDescriptor(key, var.getTextDescriptor().withOff(0.5, -1).withRelSize(1).withDispPart(
								TextDescriptor.DispPos.VALUE));
					} else if (key == Schematics.ATTR_LENGTH) {
						ni.setTextDescriptor(key, var.getTextDescriptor().withOff(-0.5, -1).withRelSize(0.5)
								.withDispPart(TextDescriptor.DispPos.VALUE));
					} else {
						ni.setTextDescriptor(key, var.getTextDescriptor().withDispPart(TextDescriptor.DispPos.VALUE));
					}
				}
			}
		}

		// place an icon if requested
		if (iconToPlace != null) {
			ERectangle bounds = newCell.getBounds();
			EPoint center = new EPoint(bounds.getMaxX() + iconToPlace.getDefWidth(), bounds.getMaxY()
					+ iconToPlace.getDefHeight());
			NodeInst.makeInstance(iconToPlace, center, iconToPlace.getDefWidth(), iconToPlace.getDefHeight(), newCell);
		}

		// place exports in the new cell
		for (PlacementExport plExport : exportsToPlace) {
			PlacementPort plPort = plExport.getPort();
			String exportName = plExport.getName();
			PlacementNode plNode = (PlacementNode)plPort.getPlacementNode();
			NodeInst newNI = placedNodes.get(plNode);
			if (newNI == null)
				continue;
			PortInst portToExport = newNI.findPortInstFromProto(plPort.getPortProto());
			Export.newInstance(newCell, portToExport, exportName, plExport.getCharacteristic());
		}

		ImmutableArcInst a = Generic.tech().unrouted_arc.getDefaultInst(newCell.getEditingPreferences());
		long gridExtend = a.getGridExtendOverMin();
		for (PlacementNetwork plNet : allNetworks) {
			PlacementFrame.PlacementPort lastPp = null;
			PortInst lastPi = null;
			EPoint lastPt = null;
			for (PlacementFrame.PlacementPort plPort : plNet.getPortsOnNet()) {
				PlacementNode plNode = (PlacementNode)plPort.getPlacementNode();
				NodeInst newNi = placedNodes.get(plNode);
				if (newNi != null) {
					PlacementPort thisPp = (PlacementPort)plPort;
					PortInst thisPi = newNi.findPortInstFromProto(thisPp.getPortProto());
					EPoint thisPt = new EPoint(plNode.getPlacementX() + plPort.getRotatedOffX(), plNode.getPlacementY()
							+ plPort.getRotatedOffY());
					if (lastPp != null) {
						// connect them
						ArcInst.newInstance(newCell, Generic.tech().unrouted_arc, null, null, lastPi, thisPi, lastPt,
								thisPt, gridExtend, ArcInst.DEFAULTANGLE, a.flags);
					}
					lastPp = thisPp;
					lastPi = thisPi;
					lastPt = thisPt;
				}
			}
		}

		timer.end();
		System.out.println("\t(took " + timer + ")");
		return newCell;
	}
}
