/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: NetSchem.java
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

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.prototype.NodeProto;
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
import com.sun.electric.tool.user.ActivityLogger;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * This is the mirror of group of Icon and Schematic cells in Network tool.
 */
class NetSchem extends NetCell {

	static void updateCellGroup(Cell.CellGroup cellGroup) {
		Cell mainSchematics = cellGroup.getMainSchematics();
		NetSchem mainSchem = null;
		if (mainSchematics != null) mainSchem = (NetSchem)Network.getNetCell(mainSchematics);
		for (Iterator it = cellGroup.getCells(); it.hasNext();) {
			Cell cell = (Cell)it.next();
			if (cell.isIcon()) {
				NetSchem icon = (NetSchem)Network.getNetCell(cell);
				if (icon == null) continue;
				icon.setImplementation(mainSchem != null ? mainSchem : icon);
				for (Iterator vit = cell.getVersions(); vit.hasNext();) {
					Cell verCell = (Cell)vit.next();
					if (verCell == cell) continue;
					icon = (NetSchem)Network.getNetCell(verCell);
					if (icon == null) continue;
					icon.setImplementation(mainSchem != null ? mainSchem : icon);
				}
			}
		}
	}

	private static class MultiProxy {
		NodeInst nodeInst;
		int arrayIndex;

		MultiProxy(NodeInst nodeInst, int arrayIndex) {
			this.nodeInst = nodeInst;
			this.arrayIndex = arrayIndex;
		}
	}

	private class Proxy implements Nodable {
		NodeInst nodeInst;
		int arrayIndex;
		int nodeOffset;
		MultiProxy[] shared;

		Proxy(NodeInst nodeInst, int arrayIndex) {
			this.nodeInst = nodeInst;
			this.arrayIndex = arrayIndex;
		}
	
		private final boolean isShared() { return shared != null; }

		private boolean hasMulti(Cell iconCell) {
			if (shared == null)
				return nodeInst.getProto() == iconCell;
			for (int i = 0; i < shared.length; i++) {
				if (shared[i].nodeInst.getProto() == iconCell)
					return true;
			}
			return false;
		}

		private void addMulti(NodeInst nodeInst, int arrayIndex) {
			if (shared == null) {
				shared = new MultiProxy[2];
				shared[0] = new MultiProxy(this.nodeInst, this.arrayIndex);
			} else {
				MultiProxy[] newShared = new MultiProxy[shared.length + 1];
				for (int i = 0; i < shared.length; i++) newShared[i] = shared[i];
				shared = newShared;
			}
			shared[shared.length - 1] = new MultiProxy(nodeInst, arrayIndex);
		}

		/**
		 * Method to return the prototype of this Nodable.
		 * @return the prototype of this Nodable.
		 */
		public NodeProto getProto() {
			NetSchem schem = Network.getNetCell((Cell)nodeInst.getProto()).getSchem();
			return schem.cell;
		}

		/**
		 * Method to return the number of actual NodeProtos which
		 * produced this Nodable.
		 * @return number of actual NodeProtos.
		 */
		public int getNumActualProtos() { return shared == null ? 1 : shared.length; }

		/**
		 * Method to return the i-th actual NodeProto which produced
		 * this Nodable.
		 * @param i specified index of actual NodeProto.
		 * @return actual NodeProt.
		 */
		public NodeProto getActualProto(int i) {
			if (shared == null && i == 0) return nodeInst.getProto();
			return shared[i].nodeInst.getProto();
		}

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
		 * Method to return the Variable on this Nodable with a given name.
		 * @param name the name of the Variable.
		 * @return the Variable with that name, or null if there is no such Variable.
		 */
		public Variable getVar(String name) {
			if (shared == null)
				return nodeInst.getVar(name);
			Variable var = null;
			for (int i = 0; i < shared.length; i++) {
				Variable v = shared[i].nodeInst.getVar(name);
				if (v == null) continue;
				if (var == null) {
					var = v;
				} else if (!v.getObject().equals(var.getObject())) {
					String msg = "Network: Cell " + cell.describe() + " has multipart icon <" + getName() +
						"> with ambigouos definition of variable " + name;
                    System.out.println(msg);
                    ErrorLogger.MessageLog log = Network.errorLogger.logError(msg, cell, Network.errorSortNodes);
                    log.addGeom(shared[i].nodeInst, true, cell, null);
				}
			}
			return var;
		}

		/**
         * Method to return the Variable on this ElectricObject with a given key.
         * @param key the key of the Variable.
         * @return the Variable with that key, or null if there is no such Variable.
		 */
		public Variable getVar(Variable.Key key) {
			if (shared == null)
				return nodeInst.getVar(key);
			Variable var = null;
			for (int i = 0; i < shared.length; i++) {
				Variable v = shared[i].nodeInst.getVar(key);
				if (v == null) continue;
				if (var == null) {
					var = v;
				} else if (!v.getObject().equals(var.getObject())) {
					String msg = "Network: Cell " + cell.describe() + " has multipart icon <" + getName() +
						"> with ambigouos definition of variable " + key.getName();
                    System.out.println(msg);
                    ErrorLogger.MessageLog log = Network.errorLogger.logError(msg, cell, Network.errorSortNodes);
                    log.addGeom(shared[i].nodeInst, true, cell, null);
				}
			}
			return var;
		}

        /**
         * Method to return the Variable on this ElectricObject with the given key
         * that is a parameter.  If the variable is not found on this object, it
         * is also searched for on the default var owner.
         * @param name the name of the variable
         * @return the Variable with that key, that may exist either on this object
         * or the default owner.  Returns null if none found.
         */
        public Variable getParameter(String name) {
            if (shared == null)
                return nodeInst.getParameter(name);
            Variable var = null;
            for (int i = 0; i < shared.length; i++) {
                Variable v = shared[i].nodeInst.getParameter(name);
                if (v == null) continue;
                if (var == null) {
                    var = v;
                } else if (!v.getObject().equals(var.getObject())) {
                    String msg = "Network: Cell " + cell.describe() + " has multipart icon <" + getName() +
                        "> with ambigouos definition of parameter " + name;
                    System.out.println(msg);
                    ErrorLogger.MessageLog log = Network.errorLogger.logError(msg, cell, Network.errorSortNodes);
                    log.addGeom(shared[i].nodeInst, true, cell, null);
                }
            }
            return var;
        }

        /**
         * Method to create a Variable on this ElectricObject with the specified values.
         * @param name the name of the Variable.
         * @param value the object to store in the Variable.
         * @return the Variable that has been created.
         */
        public Variable newVar(String name, Object value) {
            if (shared == null)
                return nodeInst.newVar(name, value);
            // just create new var on first of shared nodeInsts
            Variable v = shared[0].nodeInst.newVar(name, value);
            return v;
        }

        /**
         * Method to create a Variable on this ElectricObject with the specified values.
         * @param key the key of the Variable.
         * @param value the object to store in the Variable.
         * @return the Variable that has been created.
         */
        public Variable newVar(Variable.Key key, Object value) {
            if (shared == null)
                return nodeInst.newVar(key, value);
            // just create new var on first of shared nodeInsts
            Variable v = shared[0].nodeInst.newVar(key, value);
            return v;
        }

        /**
         * Method to put an Object into an entry in an arrayed Variable on this ElectricObject.
         * @param key the key of the arrayed Variable.
         * @param value the object to store in an entry of the arrayed Variable.
         * @param index the location in the arrayed Variable to store the value.
         */
        public void setVar(Variable.Key key, Object value, int index) {
            if (shared == null)
                nodeInst.setVar(key, value, index);
            else {
                // find which one has var on it
                for (int i=0; i < shared.length; i++) {
                    if (shared[i].nodeInst.getVar(key) != null) {
                        shared[i].nodeInst.setVar(key, value, index);
                        return;
                    }
                }
                // if none had var on them, nothing is done (conforms to ElectricObject.setVar())
            }
        }

        /**
         * Method to delete a Variable from this ElectricObject.
         * @param key the key of the Variable to delete.
         */
        public void delVar(Variable.Key key) {
            if (shared == null)
                nodeInst.delVar(key);
            else {
                // find which one has var on it
                for (int i=0; i < shared.length; i++) {
                    if (shared[i].nodeInst.getVar(key) != null) {
                        shared[i].nodeInst.delVar(key);
                        return;
                    }
                }
                // if none had var on them, nothing is done (conforms to ElectricObject.setVar())
            }
        }

        /**
         * This method can be overridden by extending objects.
         * For objects (such as instances) that have instance variables that are
         * inherited from some Object that has the default variables, this gets
         * the object that has the default variables. From that object the
         * default values of the variables can then be found.
         * @return the object that holds the default variables and values.
         */
        public ElectricObject getVarDefaultOwner() {
            //ActivityLogger.logMessage("getVarDefaultOwner called for "+this);
            return nodeInst.getVarDefaultOwner();
        }

		/**
		 * Method to return an iterator over all Variables on this Nodable.
		 * @return an iterator over all Variables on this Nodable.
		 */
		public Iterator getVariables()
		{
			if (shared == null)
				return nodeInst.getVariables();
			List allVariables = new ArrayList();
			for (int i = 0; i < shared.length; i++)
			{
				for(Iterator it = shared[i].nodeInst.getVariables(); it.hasNext(); )
					allVariables.add(it.next());
			}
			return allVariables.iterator();
		}

        /**
         * Method to return an Iterator over all Variables marked as parameters on this ElectricObject.
         * This may also include any parameters on the defaultVarOwner object that are not on this object.
         * @return an Iterator over all Variables on this ElectricObject.
         */
        public Iterator getParameters() {
            if (shared == null)
                return nodeInst.getParameters();
            HashMap allParameters = new HashMap();
            for (int i = 0; i < shared.length; i++)
            {
                for(Iterator it = shared[i].nodeInst.getParameters(); it.hasNext(); ) {
                    Variable v = (Variable)it.next();
                    Variable var = (Variable)allParameters.get(v.getKey());
                    if (var == null) {
                        allParameters.put(v.getKey(), v);
                        continue;
                    }
                    // if same value, just ignore second param
                    if (var.getObject().equals(v.getObject())) continue;
                    // print error, conflicting values
                    String msg = "Network: Cell " + cell.describe() + " has multipart icon <" + getName() +
                        "> with ambigouos definition of variable " + v.getKey().getName();
                    System.out.println(msg);
                    ErrorLogger.MessageLog log = Network.errorLogger.logError(msg, cell, Network.errorSortNodes);
                    log.addGeom(shared[i].nodeInst, true, cell, null);
                }
            }
            return allParameters.values().iterator();
        }

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

	/** Node offsets. */											int[] nodeOffsets;
	/** Node offsets. */											int[] drawnOffsets;
	/** Node offsets. */											Proxy[] nodeProxies;
	/** Map from names to proxies. Contains non-temporary names. */	Map name2proxy = new HashMap();
	/** */															Global.Set globals = Global.Set.empty;
	/** */															int[] portOffsets = new int[1];
	/** */															int netNamesOffset;

	/** */															Name[] drawnNames;
	/** */															int[] drawnWidths;

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
				ErrorLogger.MessageLog log = Network.errorLogger.logError(msg, cell, Network.errorSortPorts);
				log.addExport(e, true, cell, null);
			}
		}
		if (!cell.isMultiPartIcon() && c != null && numPorts != c.getNumPorts()) {
			for (int i = 0; i < c.getNumPorts(); i++) {
				Export e = (Export)c.getPort(i);
				if (e.getEquivalentPort(cell) == null) {
					String msg = c + ": Schematic port <"+e.getNameKey()+"> has no equivalent port in " + cell.describe();
					System.out.println(msg);
					ErrorLogger.MessageLog log = Network.errorLogger.logError(msg, c, Network.errorSortPorts);
					log.addExport(e, true, c, null);
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
		return portOffset - implementation.portOffsets[0];
	}

	NetSchem getSchem() { return implementation; }

	static int getPortOffset(PortProto pp, int busIndex) {
		int portIndex = pp.getPortIndex();
		NodeProto np = pp.getParent();
		if (!(np instanceof Cell))
			return busIndex == 0 ? portIndex : -1;
		NetCell netCell = Network.getNetCell((Cell)np);
		return netCell.getPortOffset(portIndex, busIndex);
	}

	/**
	 * Get an iterator over all of the Nodables of this Cell.
	 */
	Iterator getNodables()
	{
		ArrayList nodables = new ArrayList();
		for (Iterator it = cell.getNodes(); it.hasNext();) {
			NodeInst ni = (NodeInst)it.next();
			if (nodeOffsets[ni.getNodeIndex()] < 0) continue;
			nodables.add(ni);
		}
		for (int i = 0; i < nodeProxies.length; i++) {
			Proxy proxy = nodeProxies[i];
			if (proxy == null) continue;
			if (proxy.isShared()) continue;
			nodables.add(proxy);
		}
		for (Iterator it = name2proxy.values().iterator(); it.hasNext();) {
			Proxy proxy = (Proxy)it.next();
			if (proxy.isShared())
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
// 			NetCell netCell = Network.getNetCell((Cell)ni.getProto());
		} else {
			proxy = (Proxy)no;
		}
		if (proxy == null) return -1;
		int portOffset = NetSchem.getPortOffset(portProto, busIndex);
		if (portOffset < 0) return -1;
		return proxy.nodeOffset + portOffset;
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
		for (Iterator it = cell.getCellGroup().getCells(); it.hasNext();) {
			Cell c = (Cell)it.next();
			if (!c.isIcon()) continue;
			Network.getNetCell(c).invalidateUsagesOf(strong);
			for (Iterator vit = c.getVersions(); vit.hasNext();) {
				Cell verCell = (Cell)vit.next();
				if (verCell == c) continue;
				Network.getNetCell(verCell).invalidateUsagesOf(strong);
			}
		}
	}

	private boolean initNodables() {
		if (nodeOffsets == null || nodeOffsets.length != cell.getNumNodes())
			nodeOffsets = new int[cell.getNumNodes()];
		int numNodes = cell.getNumNodes();
		Global.clearBuf();
		int nodeProxiesOffset = 0;
		for (int i = 0; i < numNodes; i++) {
			NodeInst ni = cell.getNode(i);
			NodeProto np = ni.getProto();
			NetCell netCell = null;
			if (np instanceof Cell)
				netCell = Network.getNetCell((Cell)np);
			if (netCell != null && netCell instanceof NetSchem) {
				if (ni.getNameKey().hasDuplicates()) {
					String msg = cell + ": Node name <"+ni.getNameKey()+"> has duplicate subnames";
                    System.out.println(msg);
                    ErrorLogger.MessageLog log = Network.errorLogger.logError(msg, cell, Network.errorSortNodes);
                    log.addGeom(ni, true, cell, null);
                }
				nodeOffsets[i] = ~nodeProxiesOffset;
				nodeProxiesOffset += ni.getNameKey().busWidth();
			} else {
				if (ni.getNameKey().isBus()) {
					String msg = cell + ": Array name <"+ni.getNameKey()+"> can be assigned only to icon nodes";
                    System.out.println(msg);
                    ErrorLogger.MessageLog log = Network.errorLogger.logError(msg, cell, Network.errorSortNodes);
                    log.addGeom(ni, true, cell, null);
                }
				nodeOffsets[i] = 0;
			}
			if (netCell != null) {
				NetSchem sch = Network.getNetCell((Cell)np).getSchem();
				if (sch != null && sch != this) {
					String errorMsg = Global.addToBuf(sch.globals);
					if (errorMsg != null) {
						String msg = "Network: Cell " + cell.describe() + " has globals with conflicting characteristic " + errorMsg;
                        System.out.println(msg);
                        ErrorLogger.MessageLog log = Network.errorLogger.logError(msg, cell, Network.errorSortNetworks);
                        // TODO: what to highlight?
                        // log.addGeom(shared[i].nodeInst, true, 0, null);
                    }
				}
			} else {
				Global g = globalInst(ni);
				if (g != null) {
					PortProto.Characteristic characteristic;
					if (g == Global.ground)
						characteristic = PortProto.Characteristic.GND;
					else if (g == Global.power)
						characteristic = PortProto.Characteristic.PWR;
					else {
						characteristic = PortProto.Characteristic.findCharacteristic(ni.getTechSpecific());
						if (characteristic == null) {
							String msg = "Network: Cell " + cell.describe() + " has global " + g.getName() +
								" with unknown characteristic bits";
                            System.out.println(msg);
                            ErrorLogger.MessageLog log = Network.errorLogger.logError(msg, cell, Network.errorSortNetworks);
                            log.addGeom(ni, true, cell, null);
							characteristic = PortProto.Characteristic.UNKNOWN;
						}
					}
					String errorMsg = Global.addToBuf(g, characteristic);
					if (errorMsg != null) {
						String msg = "Network: Cell " + cell.describe() + " has global with conflicting characteristic " + errorMsg;
                        System.out.println(msg);
                        ErrorLogger.MessageLog log = Network.errorLogger.logError(msg, cell, Network.errorSortNetworks);
                        //log.addGeom(shared[i].nodeInst, true, 0, null);
                    }
				}
			}
		}
		Global.Set newGlobals = Global.getBuf();
		boolean changed = false;
		if (globals != newGlobals) {
			changed = true;
			globals = newGlobals;
			if (Network.debug) System.out.println(cell+" has "+globals);
		}
		int mapOffset = portOffsets[0] = globals.size();
		int numPorts = cell.getNumPorts();
		for (int i = 1; i <= numPorts; i++) {
			Export export = (Export)cell.getPort(i - 1);
			if (Network.debug) System.out.println(export+" "+portOffsets[i-1]);
			mapOffset += export.getNameKey().busWidth();
			if (portOffsets[i] != mapOffset) {
				changed = true;
				portOffsets[i] = mapOffset;
			}
		}
		if (equivPorts == null || equivPorts.length != mapOffset)
			equivPorts = new int[mapOffset];

		for (int i = 0; i < numDrawns; i++) {
			drawnOffsets[i] = mapOffset;
			mapOffset += drawnWidths[i];
			if (Network.debug) System.out.println("Drawn " + i + " has offset " + drawnOffsets[i]);
		}
		if (nodeProxies == null || nodeProxies.length != nodeProxiesOffset)
			nodeProxies = new Proxy[nodeProxiesOffset];
		name2proxy.clear();
		for (int n = 0; n < numNodes; n++) {
			NodeInst ni = (NodeInst)cell.getNode(n);
			int proxyOffset = nodeOffsets[n];
			if (Network.debug) System.out.println(ni+" "+proxyOffset);
			if (proxyOffset >= 0) continue;
			Cell iconCell = (Cell)ni.getProto();
			NetSchem netSchem = Network.getNetCell(iconCell).getSchem();
			if (ni.isIconOfParent()) netSchem = null;
			for (int i = 0; i < ni.getNameKey().busWidth(); i++) {
				Proxy proxy = null;
				if (netSchem != null) {
					Name name = ni.getNameKey().subname(i);
					if (!name.isTempname()) {
						Proxy namedProxy = (Proxy)name2proxy.get(name);
						if (namedProxy != null) {
							Cell namedIconCell = (Cell)namedProxy.nodeInst.getProto();
							if (namedProxy.hasMulti(iconCell)) {
								String msg = "Network: Cell " + cell.describe() + " has instances of " + iconCell.describe() +
									" with same name <" + name + ">";
                                System.out.println(msg);
                                ErrorLogger.MessageLog log = Network.errorLogger.logError(msg, cell, Network.errorSortNodes);
                                log.addGeom(ni, true, cell, null);
							} else if (!iconCell.isMultiPartIcon() || !namedIconCell.isMultiPartIcon() ||
								Network.getNetCell(namedIconCell).getSchem() != netSchem) {
								String msg = "Network: Cell " + cell.describe() + " has instances of " + iconCell.describe() + " and " +
									namedIconCell.describe() + " with same name <" + name + ">";
                                System.out.println(msg);
                                ErrorLogger.MessageLog log = Network.errorLogger.logError(msg, cell, Network.errorSortNodes);
                                log.addGeom(ni, true, cell, null);
                                log.addGeom(namedProxy.nodeInst, true, cell, null);
							} else {
								proxy = namedProxy;
								proxy.addMulti(ni, i);
								System.out.println("Info: Cell " + cell.describe() + " has instances of multipart icon " + name);
							}
						}
					}
					if (proxy == null) {
						proxy = new Proxy(ni, i);
						if (!name.isTempname())
							name2proxy.put(name, proxy);
						if (Network.debug) System.out.println(proxy+" "+mapOffset+" "+netSchem.equivPorts.length);
						proxy.nodeOffset = mapOffset;
						mapOffset += (netSchem.equivPorts.length - netSchem.globals.size());
					}
				}
				nodeProxies[~proxyOffset + i] = proxy;
			}
		}
		netNamesOffset = mapOffset;
		if (Network.debug) System.out.println("netNamesOffset="+netNamesOffset);
		return changed;
	}

// 	private static int portsSize(NodeProto np) {
// 		if (np instanceof PrimitiveNode) return np.getNumPorts();
// 		NetCell netCell = (NetCell) Network.getNetCell((Cell)np);
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
				drawnWidths[drawn] = name.busWidth();
				continue;
			}
			if (oldWidth != newWidth) {
                reportDrawnWidthError((Export)cell.getPort(i), null, drawnNames[drawn].toString(), name.toString());
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
			if (np instanceof PrimitiveNode) {
				if (np.getFunction() == NodeProto.Function.PIN) continue;
				if (np == Schematics.tech.offpageNode) continue;
			}
			int numPortInsts = np.getNumPorts();
			for (int j = 0; j < numPortInsts; j++) {
				PortInst pi = ni.getPortInst(j);
				int drawn = drawns[getPortInstOffset(pi)];
				if (drawn < 0) continue;
				int oldWidth = drawnWidths[drawn];
				int newWidth = 1;
				if (np instanceof Cell) {
					NetCell netCell = Network.getNetCell((Cell)np);
					if (netCell instanceof NetSchem) {
						int arraySize = np.isIcon() ? ni.getNameKey().busWidth() : 1;
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
					String msg = "Network: Schematic cell " + cell.describe() + " has net <" +
						drawnNames[drawn] + "> with width conflict in connection " + pi.describe();
                    System.out.println(msg);
                    ErrorLogger.MessageLog log = Network.errorLogger.logError(msg, cell, Network.errorSortNetworks);
                    log.addPoly(pi.getPoly(), true, cell);
                }
			}
		}
		for (int i = 0; i < drawnWidths.length; i++) {
			if (drawnWidths[i] < 1)
				drawnWidths[i] = 1;
			if (Network.debug) System.out.println("Drawn "+i+" "+(drawnNames[i] != null ? drawnNames[i].toString() : "") +" has width " + drawnWidths[i]);
		}
	}

    // this method will not be called often because user will fix error, so it's not
    // very efficient.
    void reportDrawnWidthError(Export pp, ArcInst ai, String firstname, String badname) {
        // first occurrence is initial width which all subsequents are compared to
        int numPorts = cell.getNumPorts();
        int numArcs = cell.getNumArcs();

        String msg = "Network: Schematic cell " + cell.describe() + " has net with conflict width of names <" +
                           firstname + "> and <" + badname + ">";
        System.out.println(msg);
        ErrorLogger.MessageLog log = Network.errorLogger.logError(msg, cell, Network.errorSortNetworks);

        boolean originalFound = false;
        for (int i = 0; i < numPorts; i++) {
            String name = cell.getPort(i).getName();
            if (name.equals(firstname)) {
                log.addExport((Export)cell.getPort(i), true, cell, null);
                originalFound = true;
                break;
            }
        }
        if (!originalFound) {
            for (int i = 0; i < numArcs; i++) {
                String name = cell.getArc(i).getName();
                if (name.equals(firstname)) {
                    log.addGeom(cell.getArc(i), true, cell, null);
                    break;
                }
            }
        }
        if (ai != null) log.addGeom(ai, true, cell, null);
        if (pp != null) log.addExport(pp, true, cell, null);
    }

	void addNetNames(Name name) {
 		for (int i = 0; i < name.busWidth(); i++)
 			addNetName(name.subname(i));
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
			if (busWidth != drawnWidths[drawn]) continue;
			for (int i = 0; i < busWidth; i++) {
				userNetlist.connectMap(portOffset + i, drawnOffset + i);
				NetName nn = (NetName)netNames.get(expNm.subname(i));
				userNetlist.connectMap(portOffset + i, netNamesOffset + nn.index);
			}
		}

		// PortInsts
		int numNodes = cell.getNumNodes();
		for (int k = 0; k < numNodes; k++) {
			NodeInst ni = cell.getNode(k);
			if (ni.isIconOfParent()) continue;
			NodeProto np = ni.getProto();
			if (np instanceof PrimitiveNode) {
				// Connect global primitives
				Global g = globalInst(ni);
				if (g != null) {
					int drawn = drawns[ni_pi[k]];
					userNetlist.connectMap(globals.indexOf(g), drawnOffsets[drawn]);
				}
				if (np == Schematics.tech.wireConNode)
					connectWireCon(ni);
				continue;
			}
			if (nodeOffsets[k] >= 0) continue;
			NetCell netCell = Network.getNetCell((Cell)np);
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
				int portOffset = schem.portOffsets[portIndex] - schem.portOffsets[0];
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
					for (int j = 0; j < busWidth; j++)
						userNetlist.connectMap(busOffset + j, nodeOffset + j);
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
				NetName nn = (NetName)netNames.get(arcNm.subname(i));
				userNetlist.connectMap(drawnOffset + i, netNamesOffset + nn.index);
			}
		}
	}

	private void connectWireCon (NodeInst ni) {
		ArcInst ai1 = null;
		ArcInst ai2 = null;
		for (Iterator it = ni.getConnections(); it.hasNext();) {
			Connection con = (Connection)it.next();
			ArcInst ai = con.getArc();
			if (ai1 == null) {
				ai1 = ai;
			} else if (ai2 == null) {
				ai2 = ai;
			} else {
				String msg = "Network: Schematic cell " + cell.describe() + " has connector " + ni.describe() +
					" which merges more than two arcs";
                System.out.println(msg);
                ErrorLogger.MessageLog log = Network.errorLogger.logError(msg, cell, Network.errorSortNetworks);
                log.addGeom(ni, true, cell, null);
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
			userNetlist.connectMap(drawnOffsets[large] + i, drawnOffsets[small] + (i % drawnWidths[small]));
	}

	private void internalConnections()
	{
		int numNodes = cell.getNumNodes();
		for (int k = 0; k < numNodes; k++) {
			NodeInst ni = cell.getNode(k);
			int nodeOffset = ni_pi[k];
			NodeProto np = ni.getProto();
			if (np instanceof PrimitiveNode) {
				if (np == Schematics.tech.resistorNode && Network.shortResistors)
					userNetlist.connectMap(drawns[nodeOffset], drawns[nodeOffset + 1]);
				continue;
			}
			NetCell netCell = Network.getNetCell((Cell)np);
			if (nodeOffsets[k] < 0) continue;
			int[] eq = netCell.equivPorts;
			for (int i = 0; i < eq.length; i++) {
				int j = eq[i];
				if (i == j) continue;
				int di = drawns[nodeOffset + i];
				int dj = drawns[nodeOffset + j];
				if (di < 0 || dj < 0) continue;
				userNetlist.connectMap(di, dj);
			}
		}
		for (int k = 0; k < nodeProxies.length; k++) {
			Proxy proxy = nodeProxies[k];
			if (proxy == null) continue;
			NodeProto np = proxy.getProto();
			NetSchem schem = (NetSchem)Network.getNetCell((Cell)np);
			int numGlobals = schem.portOffsets[0];
			int nodeOffset = proxy.nodeOffset - numGlobals;
			int[] eq = schem.equivPorts;
			for (int i = 0; i < eq.length; i++) {
				int j = eq[i];
				if (i == j) continue;
				int io = (i >= numGlobals ? nodeOffset + i : this.globals.indexOf(schem.globals.get(i)));
				int jo = (j >= numGlobals ? nodeOffset + j : this.globals.indexOf(schem.globals.get(j)));
				userNetlist.connectMap(io, jo);
			}
		}
	}

	private void buildNetworkList()
	{
		userNetlist.initNetworks();
		for (int i = 0; i < globals.size(); i++) {
			userNetlist.getNetworkByMap(i).addName(globals.get(i).getName(), true);
		}
		for (Iterator it = netNames.values().iterator(); it.hasNext(); )
		{
			NetName nn = (NetName)it.next();
			if (nn.index < 0 || nn.index >= exportedNetNameCount) continue;
			userNetlist.getNetworkByMap(netNamesOffset + nn.index).addName(nn.name.toString(), true);
		}
		for (Iterator it = netNames.values().iterator(); it.hasNext(); )
		{
			NetName nn = (NetName)it.next();
			if (nn.index < exportedNetNameCount) continue;
			userNetlist.getNetworkByMap(netNamesOffset + nn.index).addName(nn.name.toString(), false);
		}
		
		// add temporary names to unnamed nets
		int numArcs = cell.getNumArcs();
		for (int i = 0; i < numArcs; i++) {
			ArcInst ai = cell.getArc(i);
			int drawn = drawns[arcsOffset + i];
			if (drawn < 0) continue;
			for (int j = 0; j < drawnWidths[drawn]; j++) {
				JNetwork network = userNetlist.getNetwork(ai, j);
				if (network == null || network.hasNames()) continue;
				if (drawnNames[drawn] == null) continue;
				String netName;
				if (drawnWidths[drawn] == 1)
					netName = drawnNames[drawn].toString();
				else if (drawnNames[drawn].isTempname())
					netName = drawnNames[drawn].toString() + "[" + j + "]";
				else
					netName = drawnNames[drawn].subname(j).toString();
				network.addName(netName, false);
			}
		}

		/*
		// debug info
		System.out.println("BuildNetworkList "+cell);
		int i = 0;
		for (int l = 0; l < userNetlist.networks.length; l++) {
			JNetwork network = userNetlist.networks[l];
			if (network == null) continue;
			String s = "";
			for (Iterator sit = network.getNames(); sit.hasNext(); )
			{
				String n = (String)sit.next();
				s += "/"+ n;
			}
			System.out.println("    "+i+"    "+s);
			i++;
			for (int k = 0; k < globals.size(); k++) {
				if (userNetlist.nm_net[userNetlist.netMap[k]] != l) continue;
				System.out.println("\t" + globals.get(k));
			}
			int numPorts = cell.getNumPorts();
			for (int k = 0; k < numPorts; k++) {
				Export e = (Export) cell.getPort(k);
				for (int j = 0; j < e.getNameKey().busWidth(); j++) {
					if (userNetlist.nm_net[userNetlist.netMap[portOffsets[k] + j]] != l) continue;
					System.out.println("\t" + e + " [" + j + "]");
				}
			}
			for (int k = 0; k < numDrawns; k++) {
				for (int j = 0; j < drawnWidths[k]; j++) {
					int ind = drawnOffsets[k] + j;
					int netInd = userNetlist.netMap[ind];
					if (userNetlist.nm_net[userNetlist.netMap[drawnOffsets[k] + j]] != l) continue;
					System.out.println("\tDrawn " + k + " [" + j + "]");
				}
			}
			for (Iterator it = netNames.values().iterator(); it.hasNext();) {
				NetName nn = (NetName)it.next();
				if (userNetlist.nm_net[userNetlist.netMap[netNamesOffset + nn.index]] != l) continue;
				System.out.println("\tNetName " + nn.name);
			}
		}
		*/
	}

	/**
	 * Update map of equivalent ports newEquivPort.
	 * @param currentTime time stamp of current network reevaluation
	 * or will be kept untouched if not.
	 */
	private boolean updateInterface() {
		boolean changed = false;
		for (int i = 0; i < equivPorts.length; i++) {
			if (equivPorts[i] != userNetlist.netMap[i]) {
				changed = true;
				equivPorts[i] = userNetlist.netMap[i];
			}
		}
		return changed;
	}

	boolean redoNetworks1()
	{
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
		userNetlist.initNetMap(mapSize);

		localConnections();
		internalConnections();
		buildNetworkList();
		if (updatePortImplementation()) changed = true;
		if (updateInterface()) changed = true;
		return changed;
	}
}

