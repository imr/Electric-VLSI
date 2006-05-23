/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: NetSchem.java
 * Written by: Dmitry Nadezhin, Sun Microsystems.
 *
 * Copyright (c) 2003 Sun Microsystems and Static Free Software
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
package com.sun.electric.database.network;
import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.network.NetCell.NetName;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.technologies.Schematics;
import com.sun.electric.tool.user.ErrorLogger;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * This is the mirror of group of Icon and Schematic cells in Network tool.
 */
class NetSchem extends NetCell {

	static void updateCellGroup(Cell.CellGroup cellGroup) {
        NetworkManager mgr = cellGroup.getDatabase().getNetworkManager();
		Cell mainSchematics = cellGroup.getMainSchematics();
		NetSchem mainSchem = null;
		if (mainSchematics != null) mainSchem = (NetSchem)mgr.getNetCell(mainSchematics);
		for (Iterator<Cell> it = cellGroup.getCells(); it.hasNext();) {
			Cell cell = it.next();
			if (cell.isIcon()) {
				NetSchem icon = (NetSchem)mgr.getNetCell(cell);
				if (icon == null) continue;
				icon.setImplementation(mainSchem != null ? mainSchem : icon);
			}
		}
	}

	private class Proxy implements Nodable {
		NodeInst nodeInst;
		int arrayIndex;
		int nodeOffset;

		Proxy(NodeInst nodeInst, int arrayIndex) {
			this.nodeInst = nodeInst;
			this.arrayIndex = arrayIndex;
		}
	
		/**
		 * Method to return the prototype of this Nodable.
		 * @return the prototype of this Nodable.
		 */
		public NodeProto getProto() {
			NetSchem schem = networkManager.getNetCell((Cell)nodeInst.getProto()).getSchem();
			return schem.cell;
		}

		/**
		 * Method to tell whether this is a cell instance.
		 * @return true becaue NetSchem objects are always cell instances.
		 */
		public boolean isCellInstance() { return true; }

		/**
		 * Method to return the Cell that contains this Nodable.
		 * @return the Cell that contains this Nodable.
		 */
		public Cell getParent() { return cell; }

		/**
		 * Method to return the name of this Nodable.
		 * @return the name of this Nodable.
		 */
		public String getName() { return getNameKey().toString(); }

		/**
		 * Method to return the name key of this Nodable.
		 * @return the name key of this Nodable.
		 */
		public Name getNameKey() { return nodeInst.getNameKey().subname(arrayIndex); }

		/**
         * Method to return the Variable on this ElectricObject with a given key.
         * @param key the key of the Variable.
         * @return the Variable with that key, or null if there is no such Variable.
		 */
		public Variable getVar(Variable.Key key) { return nodeInst.getVar(key); }

        /**
         * Method to return the Variable on this ElectricObject with the given key
         * that is a parameter.  If the variable is not found on this object, it
         * is also searched for on the default var owner.
         * @param key the key of the variable
         * @return the Variable with that key, that may exist either on this object
         * or the default owner.  Returns null if none found.
         */
        public Variable getParameter(Variable.Key key) { return nodeInst.getParameter(key); }

        /**
         * Method to create a Variable on this ElectricObject with the specified values.
         * @param name the name of the Variable.
         * @param value the object to store in the Variable.
         * @return the Variable that has been created.
         */
        public Variable newVar(String name, Object value) { return nodeInst.newVar(name, value); }

        /**
         * Method to create a Variable on this ElectricObject with the specified values.
         * @param key the key of the Variable.
         * @param value the object to store in the Variable.
         * @return the Variable that has been created.
         */
        public Variable newVar(Variable.Key key, Object value) { return nodeInst.newVar(key, value); }

        /**
         * Method to delete a Variable from this ElectricObject.
         * @param key the key of the Variable to delete.
         */
        public void delVar(Variable.Key key) { nodeInst.delVar(key); }

        /**
         * This method can be overridden by extending objects.
         * For objects (such as instances) that have instance variables that are
         * inherited from some Object that has the default variables, this gets
         * the object that has the default variables. From that object the
         * default values of the variables can then be found.
         * @return the object that holds the default variables and values.
         */
        public Cell getVarDefaultOwner() { return nodeInst.getVarDefaultOwner(); }

		/**
		 * Method to return an iterator over all Variables on this Nodable.
		 * @return an iterator over all Variables on this Nodable.
		 */
		public Iterator<Variable> getVariables() { return nodeInst.getVariables(); }

        /**
         * Method to return an Iterator over all Variables marked as parameters on this ElectricObject.
         * This may also include any parameters on the defaultVarOwner object that are not on this object.
         * @return an Iterator over all Variables on this ElectricObject.
         */
        public Iterator<Variable> getParameters() { return nodeInst.getParameters(); }

		/**
		 * Returns a printable version of this Nodable.
		 * @return a printable version of this Nodable.
		 */
		public String toString() { return "NetSchem.Proxy " + getName(); }

        // JKG: trying this out
        public boolean contains(NodeInst ni, int arrayIndex) {
            if (nodeInst == ni && this.arrayIndex == arrayIndex) return true;
            return false;
        }

        public NodeInst getNodeInst() { return nodeInst; }

	}

	/* Implementation of this NetSchem. */							NetSchem implementation;
	/* Mapping from ports of this to ports of implementation. */	int[] portImplementation;

	/**
     * Equivalence of ports.
     * equivPorts.size == ports.size.
	 * equivPorts[i] contains minimal index among ports of its group.
     */																private int[] equivPortsF, equivPortsT;
	/** Node offsets. */											int[] nodeOffsets;
	/** Node offsets. */											int[] drawnOffsets;
	/** Node offsets. */											Proxy[] nodeProxies;
	/** Proxies with global rebindes. */							Map<Proxy,Set<Global>> proxyExcludeGlobals;
	/** Map from names to proxies. Contains non-temporary names. */	Map<Name,Proxy> name2proxy = new HashMap<Name,Proxy>();
	/** */															Global.Set globals = Global.Set.empty;
	/** */															int[] portOffsets = new int[1];
	/** */															int netNamesOffset;

	/** */															Name[] drawnNames;
	/** */															int[] drawnWidths;

	/** Netlist for different shortResistors options. */			private Netlist netlistF, netlistT;


	NetSchem(Cell cell) {
		super(cell);
		setImplementation(this);
		updateCellGroup(cell.getCellGroup());
	}

// 	NetSchem(Cell.CellGroup cellGroup) {
// 		super(cellGroup.getMainSchematics());
// 	}

	private void setImplementation(NetSchem implementation) {
		if (this.implementation == implementation) return;
		this.implementation = implementation;
		updatePortImplementation();
	}

	private boolean updatePortImplementation() {
		boolean changed = false;
		int numPorts = cell.getNumPorts();
		if (portImplementation == null || portImplementation.length != numPorts) {
			changed = true;
			portImplementation = new int[numPorts];
		}
		Cell c = implementation.cell;
		for (int i = 0; i < numPorts; i++) {
			Export e = (Export)cell.getPort(i);
			int equivIndex = -1;
			if (c != null) {
				Export equiv = e.getEquivalentPort(c);
				if (equiv != null) equivIndex = equiv.getPortIndex();
			}
			if (portImplementation[i] != equivIndex) {
				changed = true;
				portImplementation[i] = equivIndex;
			}
			if (equivIndex < 0) {
				String msg = cell + ": Icon port <"+e.getNameKey()+"> has no equivalent port";
				System.out.println(msg);
                networkManager.pushHighlight(e);
				networkManager.logError(msg, NetworkTool.errorSortPorts);
			}
		}
		if (c != null && numPorts != c.getNumPorts()) {
			for (int i = 0; i < c.getNumPorts(); i++) {
				Export e = (Export)c.getPort(i);
				if (e.getEquivalentPort(cell) == null) {
					String msg = c + ": Schematic port <"+e.getNameKey()+"> has no equivalent port in " + cell;
					System.out.println(msg);
                    networkManager.pushHighlight(e);
					networkManager.logError(msg, NetworkTool.errorSortPorts);
				}
			}
		}
		return changed;
	}

	int getPortOffset(int portIndex, int busIndex) {
		portIndex = portImplementation[portIndex];
		if (portIndex < 0) return -1;
		int portOffset = implementation.portOffsets[portIndex] + busIndex;
		if (busIndex < 0 || portOffset >= implementation.portOffsets[portIndex+1]) return -1;
		return portOffset;
	}

	NetSchem getSchem() { return implementation; }

	static int getPortOffset(NetworkManager networkManager, PortProto pp, int busIndex) {
		int portIndex = pp.getPortIndex();
		NodeProto np = pp.getParent();
		if (!(np instanceof Cell))
			return busIndex == 0 ? portIndex : -1;
		NetCell netCell = networkManager.getNetCell((Cell)np);
		return netCell.getPortOffset(portIndex, busIndex);
	}

	Netlist getNetlist(boolean shortResistors) {
		if ((flags & VALID) == 0) redoNetworks();
		return shortResistors ? netlistT : netlistF;
	}

	/**
	 * Get an iterator over all of the Nodables of this Cell.
	 */
	Iterator<Nodable> getNodables()
	{
		ArrayList<Nodable> nodables = new ArrayList<Nodable>();
		for (Iterator<NodeInst> it = cell.getNodes(); it.hasNext();) {
			NodeInst ni = it.next();
			if (nodeOffsets[ni.getNodeIndex()] < 0) continue;
			nodables.add(ni);
		}
		for (int i = 0; i < nodeProxies.length; i++) {
			Proxy proxy = nodeProxies[i];
			if (proxy == null) continue;
			nodables.add(proxy);
		}
		return nodables.iterator();
	}

	/**
	 * Get a set of global signal in this Cell and its descendants.
	 */
	Global.Set getGlobals() { return globals; }
	
	/*
	 * Get offset in networks map for given global signal.
	 */
	int getNetMapOffset(Global global) { return globals.indexOf(global); }

	/**
	 * Get offset in networks map for given global of nodable.
	 * @param no nodable.
	 * @param global global signal.
	 * @return offset in networks map.
	 */
    int getNetMapOffset(Nodable no, Global global) {
		if (!(no instanceof Proxy)) return -1;
		Proxy proxy = (Proxy)no;
		NetSchem schem = networkManager.getNetCell((Cell)proxy.nodeInst.getProto()).getSchem();
		int i = schem.globals.indexOf(global);
		if (i < 0) return -1;
		return proxy.nodeOffset + i;
	}

	/*
	 * Get offset in networks map for given port instance.
	 */
	int getNetMapOffset(Nodable no, PortProto portProto, int busIndex) {
		Proxy proxy;
		if (no instanceof NodeInst) {
			NodeInst ni = (NodeInst)no;
			int nodeIndex = ni.getNodeIndex();
			int proxyOffset = nodeOffsets[nodeIndex];
			//if (proxyOffset >= 0) {
				int drawn = drawns[ni_pi[nodeIndex] + portProto.getPortIndex()];
				if (drawn < 0) return -1;
				if (busIndex < 0 || busIndex >= drawnWidths[drawn]) return -1;
				return drawnOffsets[drawn] + busIndex;
			//} else {
			//	return -1;
			//}
// 			proxy = nodeProxies[~proxyOffset + arrayIndex];
// 			NetCell netCell = networkManager.getNetCell((Cell)ni.getProto());
		} else {
			proxy = (Proxy)no;
		}
		if (proxy == null) return -1;
		int portOffset = NetSchem.getPortOffset(networkManager, portProto, busIndex);
		if (portOffset < 0) return -1;
		return proxy.nodeOffset + portOffset;
	}

	/**
	 * Method to return the port width of port of the Nodable.
	 * @return the either the port width.
	 */
	int getBusWidth(Nodable no, PortProto portProto) {
		if (no instanceof NodeInst) {
			NodeInst ni = (NodeInst)no;
			int nodeIndex = ni.getNodeIndex();
			int proxyOffset = nodeOffsets[nodeIndex];
			//if (proxyOffset >= 0) {
				int drawn = drawns[ni_pi[nodeIndex] + portProto.getPortIndex()];
				if (drawn < 0) return 0;
				return drawnWidths[drawn];
			//}
		} else {
			return portProto.getNameKey().busWidth();
		}
	}

	/*
	 * Get offset in networks map for given export.
	 */
	int getNetMapOffset(Export export, int busIndex) {
		int drawn = drawns[export.getPortIndex()];
		if (drawn < 0) return -1;
		if (busIndex < 0 || busIndex >= drawnWidths[drawn]) return -1;
		return drawnOffsets[drawn] + busIndex;
	}

	/*
	 * Get offset in networks map for given arc.
	 */
	int getNetMapOffset(ArcInst ai, int busIndex) {
		int drawn = drawns[arcsOffset + ai.getArcIndex()];
		if (drawn < 0) return -1;
		if (busIndex < 0 || busIndex >= drawnWidths[drawn]) return -1;
		return drawnOffsets[drawn] + busIndex;
	}

	/**
	 * Method to return either the network name or the bus name on this ArcInst.
	 * @return the either the network name or the bus n1ame on this ArcInst.
	 */
	Name getBusName(ArcInst ai) {
		int drawn = drawns[arcsOffset + ai.getArcIndex()];
		return drawnNames[drawn];
	}

	/**
	 * Method to return the bus width on this ArcInst.
	 * @return the either the bus width on this ArcInst.
	 */
	public int getBusWidth(ArcInst ai)
	{
		if ((flags & VALID) == 0) redoNetworks();
		int drawn = drawns[arcsOffset + ai.getArcIndex()];
		if (drawn < 0) return 0;
		return drawnWidths[drawn];
	}

	void invalidateUsagesOf(boolean strong)
	{
		super.invalidateUsagesOf(strong);
		if (cell.isIcon()) return;
		for (Iterator<Cell> it = cell.getCellGroup().getCells(); it.hasNext();) {
			Cell c = it.next();
			if (!c.isIcon()) continue;
			NetSchem icon = (NetSchem)networkManager.getNetCell(c);
			icon.setInvalid(strong, strong);
		}
	}

	private boolean initNodables() {
		if (nodeOffsets == null || nodeOffsets.length != cell.getNumNodes())
			nodeOffsets = new int[cell.getNumNodes()];
		int numNodes = cell.getNumNodes();
		Global.Buf globalBuf = new Global.Buf();
		int nodeProxiesOffset = 0;
		Map<NodeInst,Set<Global>> nodeInstExcludeGlobal = null;
		for (int i = 0; i < numNodes; i++) {
			NodeInst ni = cell.getNode(i);
			NodeProto np = ni.getProto();
			NetCell netCell = null;
			if (ni.isCellInstance())
				netCell = networkManager.getNetCell((Cell)np);
			if (netCell != null && netCell instanceof NetSchem) {
				if (ni.getNameKey().hasDuplicates()) {
					String msg = cell + ": Node name <"+ni.getNameKey()+"> has duplicate subnames";
                    System.out.println(msg);
                    networkManager.pushHighlight(ni);
                    networkManager.logError(msg, NetworkTool.errorSortNodes);
                }
				nodeOffsets[i] = ~nodeProxiesOffset;
				nodeProxiesOffset += ni.getNameKey().busWidth();
			} else {
				if (ni.getNameKey().isBus()) {
					String msg = cell + ": Array name <"+ni.getNameKey()+"> can be assigned only to icon nodes";
                    System.out.println(msg);
                    networkManager.pushHighlight(ni);
                    networkManager.logError(msg, NetworkTool.errorSortNodes);
                }
				nodeOffsets[i] = 0;
			}
			if (netCell != null) {
				NetSchem sch = netCell.getSchem();
				if (sch != null && sch != this) {
					Global.Set gs = sch.globals;

					// Check for rebinding globals
					int numPortInsts = np.getNumPorts();
					Set<Global> gb = null;
					for (int j = 0; j < numPortInsts; j++) {
						PortInst pi = ni.getPortInst(j);
						int piOffset = getPortInstOffset(pi);
						int drawn = drawns[piOffset];
						if (drawn < 0 || drawn >= numConnectedDrawns) continue;
						int portIndex = ((NetSchem)netCell).portImplementation[j];
						if (portIndex < 0) continue;
						Export e = (Export)sch.cell.getPort(portIndex);
						if (!e.isGlobalPartition()) continue;
						if (gb == null) gb = new HashSet<Global>();
						for (int k = 0, busWidth = e.getNameKey().busWidth(); k < busWidth; k++) {
							int q = sch.equivPortsF[sch.portOffsets[portIndex] + k];
							for (int l = 0; l < sch.globals.size(); l++) {
								if (sch.equivPortsF[l] == q) {
									Global g = sch.globals.get(l);
									gb.add(g);
								}
							}
						}
					}
					if (gb != null) {
						// remember excluded globals for this NodeInst
						if (nodeInstExcludeGlobal == null)
							nodeInstExcludeGlobal = new HashMap<NodeInst,Set<Global>>();
						nodeInstExcludeGlobal.put(ni, gb);
						// fix Set of globals
						gs = gs.remove(gb.iterator());
					}

					String errorMsg = globalBuf.addToBuf(gs);
					if (errorMsg != null) {
						String msg = "Network: " + cell + " has globals with conflicting characteristic " + errorMsg;
                        System.out.println(msg);
                        networkManager.logError(msg, NetworkTool.errorSortNetworks);
                        // TODO: what to highlight?
                        // log.addGeom(shared[i].nodeInst, true, 0, null);
                    }
				}
			} else {
				Global g = globalInst(ni);
				if (g != null) {
					PortCharacteristic characteristic;
					if (g == Global.ground)
						characteristic = PortCharacteristic.GND;
					else if (g == Global.power)
						characteristic = PortCharacteristic.PWR;
					else {
						characteristic = PortCharacteristic.findCharacteristic(ni.getTechSpecific());
						if (characteristic == null) {
							String msg = "Network: " + cell + " has global " + g.getName() +
								" with unknown characteristic bits";
                            System.out.println(msg);
                            networkManager.pushHighlight(ni);
                            networkManager.logError(msg, NetworkTool.errorSortNetworks);
							characteristic = PortCharacteristic.UNKNOWN;
						}
					}
					String errorMsg = globalBuf.addToBuf(g, characteristic);
					if (errorMsg != null) {
						String msg = "Network: " + cell + " has global with conflicting characteristic " + errorMsg;
                        System.out.println(msg);
                        networkManager.logError(msg, NetworkTool.errorSortNetworks);
                        //log.addGeom(shared[i].nodeInst, true, 0, null);
                    }
				}
			}
		}
		Global.Set newGlobals = globalBuf.getBuf();
		boolean changed = false;
		if (globals != newGlobals) {
			changed = true;
			globals = newGlobals;
			if (NetworkTool.debug) System.out.println(cell+" has "+globals);
		}
		int mapOffset = portOffsets[0] = globals.size();
		int numPorts = cell.getNumPorts();
		for (int i = 1; i <= numPorts; i++) {
			Export export = (Export)cell.getPort(i - 1);
			if (NetworkTool.debug) System.out.println(export+" "+portOffsets[i-1]);
			mapOffset += export.getNameKey().busWidth();
			if (portOffsets[i] != mapOffset) {
				changed = true;
				portOffsets[i] = mapOffset;
			}
		}
		if (equivPortsF == null || equivPortsF.length != mapOffset) {
			equivPortsF = new int[mapOffset];
			equivPortsT = new int[mapOffset];
		}

		for (int i = 0; i < numDrawns; i++) {
			drawnOffsets[i] = mapOffset;
			mapOffset += drawnWidths[i];
			if (NetworkTool.debug) System.out.println("Drawn " + i + " has offset " + drawnOffsets[i]);
		}
		if (nodeProxies == null || nodeProxies.length != nodeProxiesOffset)
			nodeProxies = new Proxy[nodeProxiesOffset];
        Arrays.fill(nodeProxies, null);
		name2proxy.clear();
		proxyExcludeGlobals = null;
		for (int n = 0; n < numNodes; n++) {
			NodeInst ni = (NodeInst)cell.getNode(n);
			int proxyOffset = nodeOffsets[n];
			if (NetworkTool.debug) System.out.println(ni+" "+proxyOffset);
			if (proxyOffset >= 0) continue;
			Cell iconCell = (Cell)ni.getProto();
			NetSchem netSchem = networkManager.getNetCell(iconCell).getSchem();
			if (netSchem == null || ni.isIconOfParent()) continue;
			Set<Global> gs = nodeInstExcludeGlobal != null ? nodeInstExcludeGlobal.get(ni) : null; // exclude set of globals
            for (int i = 0; i < ni.getNameKey().busWidth(); i++) {
                Proxy proxy = new Proxy(ni, i);
                Name name = ni.getNameKey().subname(i);
                if (!name.isTempname()) {
                    Proxy namedProxy = name2proxy.get(name);
                    if (namedProxy != null) {
                        Cell namedIconCell = (Cell)namedProxy.nodeInst.getProto();
                        String msg = "Network: " + cell + " has instances " + ni + " and " +
                                namedProxy.nodeInst + " with same name <" + name + ">";
                        System.out.println(msg);
                        networkManager.pushHighlight(ni);
                        networkManager.pushHighlight(namedProxy.nodeInst);
                        networkManager.logError(msg, NetworkTool.errorSortNodes);
                    }
                    name2proxy.put(name, proxy);
                }
                if (NetworkTool.debug) System.out.println(proxy+" "+mapOffset+" "+netSchem.equivPortsF.length);
                proxy.nodeOffset = mapOffset;
                mapOffset += netSchem.equivPortsF.length;
                if (gs != null) {
                    if (proxyExcludeGlobals == null)
                        proxyExcludeGlobals = new HashMap<Proxy,Set<Global>>();
                    Set<Global> gs0 = proxyExcludeGlobals.get(proxy);
                    if (gs0 != null) {
                        gs = new HashSet<Global>(gs);
                        gs.addAll(gs0);
                    }
                    proxyExcludeGlobals.put(proxy, gs);
                }
                nodeProxies[~proxyOffset + i] = proxy;
            }
		}
		netNamesOffset = mapOffset;
		if (NetworkTool.debug) System.out.println("netNamesOffset="+netNamesOffset);
		return changed;
	}

// 	private static int portsSize(NodeProto np) {
// 		if (np instanceof PrimitiveNode) return np.getNumPorts();
// 		NetCell netCell = (NetCell) NetworkTool.getNetCell((Cell)np);
// 		NetSchem sch = netCell.getSchem();
// 		if (sch == null) return np.getNumPorts();
// 		return sch.equivPorts.length - sch.globals.size();
// 	}

	private static Global globalInst(NodeInst ni) {
		NodeProto np = ni.getProto();
		if (np == Schematics.tech.groundNode) return Global.ground;
		if (np == Schematics.tech.powerNode) return Global.power;
		if (np == Schematics.tech.globalNode) {
			Variable var = ni.getVar(Schematics.SCHEM_GLOBAL_NAME, String.class);
			if (var != null) return Global.newGlobal((String)var.getObject());
		}
		return null;
	}

	void calcDrawnWidths() {
		Arrays.fill(drawnNames, null);
		Arrays.fill(drawnWidths, -1);
		int numPorts = cell.getNumPorts();
		int numNodes = cell.getNumNodes();
		int numArcs = cell.getNumArcs();

		for (int i = 0; i < numPorts; i++) {
			int drawn = drawns[i];
			Name name = cell.getPort(i).getNameKey();
			int newWidth = name.busWidth();
			int oldWidth = drawnWidths[drawn];
			if (oldWidth < 0) {
				drawnNames[drawn] = name;
				drawnWidths[drawn] = newWidth;
				continue;
			}
			if (oldWidth != newWidth) {
                reportDrawnWidthError((Export)cell.getPort(i), null, drawnNames[drawn].toString(), name.toString());
                if (oldWidth < newWidth) {
                    drawnNames[drawn] = name;
                    drawnWidths[drawn] = newWidth;
                }
            }
		}
		for (int i = 0; i < numArcs; i++) {
			int drawn = drawns[arcsOffset + i];
			if (drawn < 0) continue;
			ArcInst ai = cell.getArc(i);
			Name name = ai.getNameKey();
			if (name.isTempname()) continue;
			int newWidth = name.busWidth();
			int oldWidth = drawnWidths[drawn];
			if (oldWidth < 0) {
				drawnNames[drawn] = name;
				drawnWidths[drawn] = newWidth;
				continue;
			}
			if (oldWidth != newWidth) {
                reportDrawnWidthError(null, ai, drawnNames[drawn].toString(), name.toString());
                if (oldWidth < newWidth) {
                    drawnNames[drawn] = name;
                    drawnWidths[drawn] = newWidth;
                }
            }
		}
		ArcProto busArc = Schematics.tech.bus_arc;
		for (int i = 0; i < numArcs; i++) {
			int drawn = drawns[arcsOffset + i];
			if (drawn < 0) continue;
			ArcInst ai = cell.getArc(i);
			Name name = ai.getNameKey();
			if (!name.isTempname()) continue;
			int oldWidth = drawnWidths[drawn];
			if (oldWidth < 0) {
				drawnNames[drawn] = name;
				if (ai.getProto() != busArc)
					drawnWidths[drawn] = 1;
			}
		}
		for (int i = 0; i < numNodes; i++) {
			NodeInst ni = cell.getNode(i);
			NodeProto np = ni.getProto();
			if (!ni.isCellInstance()) {
				if (np.getFunction() == PrimitiveNode.Function.PIN) continue;
				if (np == Schematics.tech.offpageNode) continue;
			}
			int numPortInsts = np.getNumPorts();
			for (int j = 0; j < numPortInsts; j++) {
				PortInst pi = ni.getPortInst(j);
				int drawn = drawns[getPortInstOffset(pi)];
				if (drawn < 0) continue;
				int oldWidth = drawnWidths[drawn];
				int newWidth = 1;
				if (ni.isCellInstance()) {
					NetCell netCell = networkManager.getNetCell((Cell)np);
					if (netCell instanceof NetSchem) {
						int arraySize = ((Cell)np).isIcon() ? ni.getNameKey().busWidth() : 1;
						int portWidth = pi.getPortProto().getNameKey().busWidth();
						if (oldWidth == portWidth) continue;
						newWidth = arraySize*portWidth;
					}
				}
				if (oldWidth < 0) {
					drawnWidths[drawn] = newWidth;
					continue;
				}
				if (oldWidth != newWidth)  {
					String msg = "Network: Schematic " + cell + " has net <" +
						drawnNames[drawn] + "> with width conflict in connection " + pi.describe(true);
                    System.out.println(msg);
                    networkManager.pushHighlight(pi);
                    networkManager.logError(msg, NetworkTool.errorSortNetworks);
                }
			}
		}
		for (int i = 0; i < drawnWidths.length; i++) {
			if (drawnWidths[i] < 1)
				drawnWidths[i] = 1;
			if (NetworkTool.debug) System.out.println("Drawn "+i+" "+(drawnNames[i] != null ? drawnNames[i].toString() : "") +" has width " + drawnWidths[i]);
		}
	}

    // this method will not be called often because user will fix error, so it's not
    // very efficient.
    void reportDrawnWidthError(Export pp, ArcInst ai, String firstname, String badname) {
        // first occurrence is initial width which all subsequents are compared to
        int numPorts = cell.getNumPorts();
        int numArcs = cell.getNumArcs();

        String msg = "Network: Schematic " + cell + " has net with conflict width of names <" +
                           firstname + "> and <" + badname + ">";
        System.out.println(msg);

        boolean originalFound = false;
        for (int i = 0; i < numPorts; i++) {
            String name = cell.getPort(i).getName();
            if (name.equals(firstname)) {
                networkManager.pushHighlight((Export)cell.getPort(i));
                originalFound = true;
                break;
            }
        }
        if (!originalFound) {
            for (int i = 0; i < numArcs; i++) {
                String name = cell.getArc(i).getName();
                if (name.equals(firstname)) {
                    networkManager.pushHighlight(cell.getArc(i));
                    break;
                }
            }
        }
        if (ai != null) networkManager.pushHighlight(ai);
        if (pp != null) networkManager.pushHighlight(pp);
        networkManager.logError(msg, NetworkTool.errorSortNetworks);
    }

	@Override
	void addNetNames(Name name, Export e, ArcInst ai) {
 		for (int i = 0; i < name.busWidth(); i++)
 			addNetName(name.subname(i), e, ai);
	}

	private void localConnections() {

		// Exports
		int numExports = cell.getNumPorts();
		for (int k = 0; k < numExports; k++) {
			Export e = (Export) cell.getPort(k);
			int portOffset = portOffsets[k];
			Name expNm = e.getNameKey();
			int busWidth = expNm.busWidth();
			int drawn = drawns[k];
			int drawnOffset = drawnOffsets[drawn];
			for (int i = 0; i < busWidth; i++) {
                netlistF.connectMap(portOffset + i, drawnOffset + (busWidth == drawnWidths[drawn] ? i : i % drawnWidths[drawn]));
				NetName nn = netNames.get(expNm.subname(i).canonicString());
				netlistF.connectMap(portOffset + i, netNamesOffset + nn.index);
			}
		}

		// PortInsts
		int numNodes = cell.getNumNodes();
		for (int k = 0; k < numNodes; k++) {
			NodeInst ni = cell.getNode(k);
			if (ni.isIconOfParent()) continue;
			NodeProto np = ni.getProto();
			if (!ni.isCellInstance()) {
				// Connect global primitives
				Global g = globalInst(ni);
				if (g != null) {
					int drawn = drawns[ni_pi[k]];
					netlistF.connectMap(globals.indexOf(g), drawnOffsets[drawn]);
				}
				if (np == Schematics.tech.wireConNode)
					connectWireCon(ni);
				continue;
			}
			if (nodeOffsets[k] >= 0) continue;
			NetCell netCell = networkManager.getNetCell((Cell)np);
			if (!(netCell instanceof NetSchem)) continue;
			NetSchem icon = (NetSchem)netCell;
			NetSchem schem = netCell.getSchem();
			if (schem == null) continue;
			Name nodeName = ni.getNameKey();
			int arraySize = nodeName.busWidth();
			int proxyOffset = nodeOffsets[k];
			int numPorts = np.getNumPorts();
			for (int m = 0; m < numPorts; m++) {
				Export e = (Export) np.getPort(m);
				int portIndex = m;
				portIndex = icon.portImplementation[portIndex];
				if (portIndex < 0) continue;
				int portOffset = schem.portOffsets[portIndex];
				int busWidth = e.getNameKey().busWidth();
				int drawn = drawns[ni_pi[k] + m];
				if (drawn < 0) continue;
				int width = drawnWidths[drawn];
				if (width != busWidth && width != busWidth*arraySize) continue;
				for (int i = 0; i < arraySize; i++) {
					Proxy proxy = nodeProxies[~proxyOffset + i];
					if (proxy == null) continue;
					int nodeOffset = proxy.nodeOffset + portOffset;
					int busOffset = drawnOffsets[drawn];
					if (width != busWidth) busOffset += busWidth*i;
					for (int j = 0; j < busWidth; j++) {
						netlistF.connectMap(busOffset + j, nodeOffset + j);
					}
				}
			}
		}

		// Arcs
		int numArcs = cell.getNumArcs();
		for (int k = 0; k < numArcs; k++) {
			ArcInst ai = (ArcInst) cell.getArc(k);
			int drawn = drawns[arcsOffset + k];
			if (drawn < 0) continue;
			if (!ai.isUsernamed()) continue;
			int busWidth = drawnWidths[drawn];
			Name arcNm = ai.getNameKey();
			if (arcNm.busWidth() != busWidth) continue;
			int drawnOffset = drawnOffsets[drawn];
			for (int i = 0; i < busWidth; i++) {
				NetName nn = netNames.get(arcNm.subname(i).canonicString());
				netlistF.connectMap(drawnOffset + i, netNamesOffset + nn.index);
			}
		}

		// Globals of proxies
		for (int k = 0; k < nodeProxies.length; k++) {
			Proxy proxy = nodeProxies[k];
			if (proxy == null) continue;
			NodeProto np = proxy.getProto();
			NetSchem schem = (NetSchem)networkManager.getNetCell((Cell)np);
			int numGlobals = schem.portOffsets[0];
			if (numGlobals == 0) continue;
			Set/*<Global>*/ excludeGlobals = null;
			if (proxyExcludeGlobals != null)
				excludeGlobals = proxyExcludeGlobals.get(proxy);
			for (int i = 0; i < numGlobals; i++) {
				Global g = schem.globals.get(i);
				if (excludeGlobals != null && excludeGlobals.contains(g)) continue;
				netlistF.connectMap(this.globals.indexOf(g), proxy.nodeOffset + i);
			}
		}
	}

	private void connectWireCon (NodeInst ni) {
		ArcInst ai1 = null;
		ArcInst ai2 = null;
		for (Iterator<Connection> it = ni.getConnections(); it.hasNext();) {
			Connection con = it.next();
			ArcInst ai = con.getArc();
			if (ai1 == null) {
				ai1 = ai;
			} else if (ai2 == null) {
				ai2 = ai;
			} else {
				String msg = "Network: Schematic " + cell + " has connector " + ni +
					" which merges more than two arcs";
                System.out.println(msg);
                networkManager.pushHighlight(ni);
                networkManager.logError(msg, NetworkTool.errorSortNetworks);
				return;
			}
		}
		if (ai2 == null || ai1 == ai2) return;
		int large = drawns[arcsOffset + ai1.getArcIndex()];
		int small = drawns[arcsOffset + ai2.getArcIndex()];
		if (large < 0 || small < 0) return;
		if (drawnWidths[small] > drawnWidths[large]) {
			int temp = small;
			small = large;
			large = temp;
		}
		for (int i = 0; i < drawnWidths[large]; i++)
			netlistF.connectMap(drawnOffsets[large] + i, drawnOffsets[small] + (i % drawnWidths[small]));
	}

	private void internalConnections()
	{
		int numNodes = cell.getNumNodes();
		for (int k = 0; k < numNodes; k++) {
			NodeInst ni = cell.getNode(k);
			int nodeOffset = ni_pi[k];
			NodeProto np = ni.getProto();
			if (!ni.isCellInstance()) {
				if (np == Schematics.tech.resistorNode)
					netlistT.connectMap(drawnOffsets[drawns[nodeOffset]], drawnOffsets[drawns[nodeOffset + 1]]);
				continue;
			}
			NetCell netCell = networkManager.getNetCell((Cell)np);
			if (nodeOffsets[k] < 0) continue;
			for (int i = 0, numPorts = netCell.getNumEquivPorts(); i < numPorts; i++) {
				int j = netCell.getEquivPorts(i);
				if (i == j) continue;
				int di = drawns[nodeOffset + i];
				int dj = drawns[nodeOffset + j];
				if (di < 0 || dj < 0) continue;
				netlistF.connectMap(drawnOffsets[di], drawnOffsets[dj]);
				netlistT.connectMap(drawnOffsets[di], drawnOffsets[dj]);
			}
		}
		for (int k = 0; k < nodeProxies.length; k++) {
			Proxy proxy = nodeProxies[k];
			if (proxy == null) continue;
			NodeProto np = proxy.getProto();
			NetSchem schem = (NetSchem)networkManager.getNetCell((Cell)np);
			int[] eqF = schem.equivPortsF;
			int[] eqT = schem.equivPortsT;
			assert(eqF.length == eqT.length);
			for (int i = 0; i < eqF.length; i++) {
				int io = proxy.nodeOffset + i;

				int jF = eqF[i];
				if (i != jF)
					netlistF.connectMap(io, proxy.nodeOffset + jF);

				int jT = eqT[i];
				if (i != jT)
					netlistT.connectMap(io, proxy.nodeOffset + jT);
			}
		}
	}

	private void buildNetworkLists()
	{
		netlistF.initNetworks(equivPortsF.length);
		netlistT.initNetworks(equivPortsT.length);
		for (int i = 0; i < globals.size(); i++) {
			netlistF.getNetworkByMap(i).addUserName(globals.get(i).getNameKey(), true);
			netlistT.getNetworkByMap(i).addUserName(globals.get(i).getNameKey(), true);
		}
		for (NetName nn: netNames.values())
		{
			if (nn.index < 0 || nn.index >= exportedNetNameCount) continue;
			netlistF.getNetworkByMap(netNamesOffset + nn.index).addUserName(nn.name, true);
			netlistT.getNetworkByMap(netNamesOffset + nn.index).addUserName(nn.name, true);
		}
		for (NetName nn: netNames.values())
		{
			if (nn.index < exportedNetNameCount) continue;
			netlistF.getNetworkByMap(netNamesOffset + nn.index).addUserName(nn.name, false);
			netlistT.getNetworkByMap(netNamesOffset + nn.index).addUserName(nn.name, false);
		}
		
		// add temporary names to unnamed nets
		for (int i = 0, numArcs = cell.getNumArcs(); i < numArcs; i++) {
			ArcInst ai = cell.getArc(i);
			int drawn = drawns[arcsOffset + i];
			if (drawn < 0) continue;
			for (int j = 0; j < drawnWidths[drawn]; j++) {
				Network networkF = netlistF.getNetwork(ai, j);
				if (networkF != null && networkF.hasNames()) networkF = null;
				Network networkT = netlistT.getNetwork(ai, j);
				if (networkT != null && networkT.hasNames()) networkT = null;

				if (networkF == null && networkT == null) continue;
				if (drawnNames[drawn] == null) continue;
				String netName;
				if (drawnWidths[drawn] == 1)
					netName = drawnNames[drawn].toString();
				else if (drawnNames[drawn].isTempname())
					netName = drawnNames[drawn].toString() + "[" + j + "]";
				else
					netName = drawnNames[drawn].subname(j).toString();

				if (networkF != null)
					networkF.addTempName(netName);
				if (networkT != null)
					networkT.addTempName(netName);
			}
		}

		// add temporary names to unconnected ports
		for (Iterator<Nodable> it = getNodables(); it.hasNext();) {
			Nodable no = it.next();
			NodeProto np = no.getProto();
			for (int i = 0, numPorts = np.getNumPorts(); i < numPorts; i++) {
				PortProto pp = np.getPort(i);
				for (int k = 0, busWidth = pp.getNameKey().busWidth(); k < busWidth; k++) {
					Network networkF = netlistF.getNetwork(no, pp, k);
					if (networkF != null && !networkF.hasNames())
						networkF.addTempName(no.getName() + "." + pp.getNameKey().subname(k));
					Network networkT = netlistT.getNetwork(no, pp, k);
					if (networkT != null && !networkT.hasNames())
						networkT.addTempName(no.getName() + "." + pp.getNameKey().subname(k));
				}
			}
		}

		// add temporary names to unconnected ports
		for (int n = 0, numNodes = cell.getNumNodes(); n < numNodes; n++) {
			NodeInst ni = (NodeInst)cell.getNode(n);
			NodeProto np = ni.getProto();
            int arraySize = ni.getNameKey().busWidth();
			for (int i = 0, numPorts = np.getNumPorts(); i < numPorts; i++) {
				PortProto pp = np.getPort(i);
                int drawn = drawns[ni_pi[n] + i];
                if (drawn < 0) continue;
				int busWidth = pp.getNameKey().busWidth();
                int drawnWidth = drawnWidths[drawn];
                for (int l = 0; l < drawnWidth; l++) {
                    Network networkF = netlistF.getNetworkByMap(drawnOffsets[drawn] + l);
                    if (networkF != null && !networkF.hasNames()) {
                        int arrayIndex = (l / busWidth) % arraySize;
                        int busIndex = l % busWidth;
                        networkF.addTempName(ni.getNameKey().subname(arrayIndex) + "." + pp.getNameKey().subname(busIndex));
                    }
                    Network networkT = netlistT.getNetworkByMap(drawnOffsets[drawn] + l);
                    if (networkT != null && !networkT.hasNames()) {
                        int arrayIndex = (l / busWidth) % arraySize;
                        int busIndex = l % busWidth;
                        networkT.addTempName(ni.getNameKey().subname(arrayIndex) + "." + pp.getNameKey().subname(busIndex));
                    }
                }
			}
		}

        for (int i = 0, numNetworks = netlistF.getNumNetworks(); i < numNetworks; i++)
            assert netlistF.getNetwork(i).hasNames();
        for (int i = 0, numNetworks = netlistT.getNumNetworks(); i < numNetworks; i++)
            assert netlistT.getNetwork(i).hasNames();
		/*
		// debug info
		System.out.println("BuildNetworkList "+cell);
		for (int kk = 0; kk < 2; kk++) {
		    Netlist netlist;
			if (kk == 0) {
				netlist = netlistF;
				System.out.println("NetlistF");
			} else {
				netlist = netlistT;
				System.out.println("NetlistT");
			}
			int i = 0;
			for (int l = 0; l < netlist.networks.length; l++) {
				Network network = netlist.networks[l];
				if (network == null) continue;
				String s = "";
				for (Iterator<String> sit = network.getNames(); sit.hasNext(); )
				{
					String n = sit.next();
					s += "/"+ n;
				}
				System.out.println("    "+i+"    "+s);
				i++;
				for (int k = 0; k < globals.size(); k++) {
					if (netlist.nm_net[netlist.netMap[k]] != l) continue;
					System.out.println("\t" + globals.get(k));
				}
				int numPorts = cell.getNumPorts();
				for (int k = 0; k < numPorts; k++) {
					Export e = (Export) cell.getPort(k);
					for (int j = 0; j < e.getNameKey().busWidth(); j++) {
						if (netlist.nm_net[netlist.netMap[portOffsets[k] + j]] != l) continue;
						System.out.println("\t" + e + " [" + j + "]");
					}
				}
				for (int k = 0; k < numDrawns; k++) {
					for (int j = 0; j < drawnWidths[k]; j++) {
						int ind = drawnOffsets[k] + j;
						int netInd = netlist.netMap[ind];
						if (netlist.nm_net[netlist.netMap[drawnOffsets[k] + j]] != l) continue;
						System.out.println("\tDrawn " + k + " [" + j + "]");
					}
				}
				for (Iterator<NetName> it = netNames.values().iterator(); it.hasNext();) {
					NetName nn = it.next();
					if (netlist.nm_net[netlist.netMap[netNamesOffset + nn.index]] != l) continue;
					System.out.println("\tNetName " + nn.name);
				}
			}
		}
		*/
	}

	/**
	 * Update map of equivalent ports newEquivPort.
	 */
	private boolean updateInterface() {
		boolean changed = false;
		for (int i = 0; i < equivPortsF.length; i++) {
			if (equivPortsF[i] != netlistF.netMap[i]) {
				changed = true;
				equivPortsF[i] = netlistF.netMap[i];
			}
			if (equivPortsT[i] != netlistT.netMap[i]) {
				changed = true;
				equivPortsT[i] = netlistT.netMap[i];
			}
		}
		return changed;
	}

	boolean redoNetworks1()
	{
//		System.out.println("redoNetworks1 on " + cell);
		int numPorts = cell.getNumPorts();
		if (portOffsets.length != numPorts + 1)
			portOffsets = new int[numPorts + 1];

		/* Set index of NodeInsts */
		if (drawnNames == null || drawnNames.length != numDrawns) {
			drawnNames = new Name[numDrawns];
			drawnWidths = new int[numDrawns];
			drawnOffsets = new int[numDrawns];
		}
		calcDrawnWidths();

		boolean changed = initNodables();
		// Gather port and arc names
		int mapSize = netNamesOffset + netNames.size();
//        HashMap/*<Cell,Netlist>*/ subNetlistsF = new HashMap/*<Cell,Netlist>*/();
//        for (Iterator it = getNodables(); it.hasNext(); ) {
//            Nodable no = it.next();
//            if (!no.isCellInstance()) continue;
//            Cell subCell = (Cell)no.getProto();
//            subNetlistsF.put(subCell, networkManager.getNetlist(subCell, false));
//        }
		netlistF = new Netlist(this, false, mapSize);
		localConnections();

//        HashMap/*<Cell,Netlist>*/ subNetlistsT = new HashMap/*<Cell,Netlist>*/();
//        for (Iterator<Nodable> it = getNodables(); it.hasNext(); ) {
//            Nodable no = it.next();
//            if (!no.isCellInstance()) continue;
//            Cell subCell = (Cell)no.getProto();
//            subNetlistsT.put(subCell, networkManager.getNetlist(subCell, true));
//        }
		netlistT = new Netlist(this, true, netlistF);
		internalConnections();
		buildNetworkLists();
		if (updatePortImplementation()) changed = true;
		if (updateInterface()) changed = true;
		return changed;
	}
}

