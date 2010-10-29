/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Placement.java
 *
 * Copyright (c) 2009, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.PrefPackage;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.UserInterface;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.Tool;
import com.sun.electric.tool.placement.PlacementAdapter.PlacementExport;
import com.sun.electric.tool.placement.PlacementAdapter.PlacementNode;
import com.sun.electric.tool.placement.PlacementAdapter.PlacementPort;
import com.sun.electric.tool.placement.PlacementFrame.PlacementNetwork;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.ui.WindowFrame;
import com.sun.electric.util.CollectionFactory;
import com.sun.electric.util.math.Orientation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;

/**
 * Class to place cells for better routing.
 */
public class Placement extends Tool
{
	/** the Placement tool. */					private static Placement tool = new Placement();

	/**
	 * The constructor sets up the Placement tool.
	 */
	private Placement()
	{
		super("placement");
	}

	/**
	 * Method to initialize the Placement tool.
	 */
	public void init() {}

    /**
     * Method to retrieve the singleton associated with the Placement tool.
     * @return the Placement tool.
     */
    public static Placement getPlacementTool() { return tool; }

	/**
	 * Method to run placement on the current cell in a new Job.
	 */
	public static void placeCurrentCell()
	{
		// get cell information
		UserInterface ui = Job.getUserInterface();
		Cell cell = ui.needCurrentCell();
		if (cell == null) return;
		PlacementPreferences pp = new PlacementPreferences(false);
		new PlaceJob(cell, pp);
	}

	/**
	 * Class to do placement in a Job.
	 */
	private static class PlaceJob extends Job
	{
		private Cell cell;
		private PlacementPreferences prefs;
		private Cell newCell;

		private PlaceJob(Cell cell, PlacementPreferences prefs)
		{
			super("Place cells", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.cell = cell;
			this.prefs = prefs;
			startJob();
		}

		public boolean doIt() throws JobException
		{
			newCell = placeCellNoJob(cell, prefs);
			fieldVariableChanged("newCell");
            return true;
		}

		public void terminateOK()
		{
			if (newCell != null)
			{
	            WindowFrame wf = WindowFrame.getCurrentWindowFrame();
				if (User.isShowCellsInNewWindow()) wf = null;
				if (wf == null) wf = WindowFrame.createEditWindow(newCell);
	            wf.setCellWindow(newCell, null);
			}
		}
	}

	/**
	 * Entry point to do Placement of a Cell and create a new, placed Cell.
	 * Gathers the requirements for Placement into a collection of shadow
	 * objects (PlacementNode, PlacementPort, PlacementNetwork, and
	 * PlacementExport). Then invokes the alternate version of "doPlacement()"
	 * that works from shadow objedts.
	 *
	 * @param srcCell
	 *            the Cell to place. Objects in that Cell will be reorganized in
	 *            and placed in a new Cell.
	 * @return the new Cell with the placement results.
	 */
	public static Cell placeCellNoJob(Cell cell, PlacementPreferences prefs)
    {
		PlacementFrame pla = getCurrentPlacementAlgorithm(prefs);
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
		Cell newCell = PlacementAdapter.doPlacement(pla, cell.getLibrary(), cell.noLibDescribe(), nodesToPlace, allNetworks, exportsToPlace,
				iconToPlace, prefs);

		return newCell;
	}

	public static class PlacementPreferences extends PrefPackage implements Serializable
    {
        private static final String PLACEMENT_NODE = "tool/placement";

        /** The name of the Placement algorithm to use. The default is "Min-Cut". */
        @StringPref(node = PLACEMENT_NODE, key = "AlgorithmName", factory = "Min-Cut")
        public String placementAlgorithm;
        
        private Object[][] values;
        
        public PlacementPreferences(boolean factory)
		{
            super(factory);
            Preferences prefs = (factory ? getFactoryPrefRoot() : getPrefRoot()).node(PLACEMENT_NODE);
            values = new Object[PlacementAdapter.placementAlgorithms.length][];
            for (int i = 0; i < values.length; i++) {
                PlacementFrame pf = PlacementAdapter.placementAlgorithms[i];
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
            Preferences prefs = prefRoot.node(PLACEMENT_NODE);
            assert values.length == PlacementAdapter.placementAlgorithms.length;
            for (int i = 0; i < values.length; i++) {
                PlacementFrame pf = PlacementAdapter.placementAlgorithms[i];
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
            PlacementFrame pf = PlacementAdapter.placementAlgorithms[i];
            int j = indexOfParameter(pf, par.key);
            return values[i][j];
        }

        public void setParameter(PlacementFrame.PlacementParameter par, Object value) {
            int i = indexOfFrame(par);
            PlacementFrame pf = PlacementAdapter.placementAlgorithms[i];
            int j = indexOfParameter(pf, par.key);
            Object oldValue = values[i][j];
            if (oldValue.equals(value)) {
                return;
            }
            assert value.getClass() == par.factoryValue.getClass();
            if (value.equals(par.factoryValue))
                value = par.factoryValue;
            values[i][j] = value;
        }

        private int indexOfFrame(PlacementFrame.PlacementParameter par) {
            PlacementFrame rf = par.getOwner();
            for (int i = 0; i < PlacementAdapter.placementAlgorithms.length; i++) {
                if (rf.getClass() == PlacementAdapter.placementAlgorithms[i].getClass()) return i;
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

	/**
	 * Method to return the current Placement algorithm.
	 * This is a requested subclass of PlacementFrame.
	 * @return the current Placement algorithm.
	 */
	public static PlacementFrame getCurrentPlacementAlgorithm(PlacementPreferences prefs)
	{
		String algName = prefs.placementAlgorithm;
		for(PlacementFrame pfObj : PlacementAdapter.getPlacementAlgorithms())
		{
			if (algName.equals(pfObj.getAlgorithmName())) return pfObj;
		}
		return PlacementAdapter.getPlacementAlgorithms()[0];
	}

}
