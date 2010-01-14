/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: NetCell.java
 * Written by: Dmitry Nadezhin, Sun Microsystems.
 *
 * Copyright (c) 2003 Sun Microsystems and Static Free Software
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
package com.sun.electric.database.network;

import com.sun.electric.database.CellTree;
import com.sun.electric.database.EquivPorts;
import com.sun.electric.database.Snapshot;
import com.sun.electric.database.geometry.GenMath;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.EDatabase;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitivePort;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.technology.technologies.Schematics;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * This is the Cell mirror in Network tool.
 */
public class NetCell {

    /** Check immutable algorithm which computes equivalent ports */
    private static final boolean CHECK_EQUIV_PORTS = true;
    /** If bit set, netlist is valid for cell tree.*/
    static final int VALID = 1;
    /** If bit set, netlist is valid with current  equivPorts of subcells.*/
    static final int LOCALVALID = 2;
    /** Separator for net names of unconnected port instances */
    static final char PORT_SEPARATOR = '.';
    /** Network manager to which this NetCell belongs. */
    final NetworkManager networkManager;
    /** Database to which this NetCell belongs. */
    final EDatabase database;
    /** This NetCell is schematic */
    final boolean isSchem;
    /** Cell from database. */
    final Cell cell;
    /** Flags of this NetCell. */
    int flags;
    /** The number of times this NetCell has been <i>structurally modified</i>.
     * Structural modifications are those that change the number of networks in
     * its netlists, or otherwise perturb them in such a fashion that iterations in
     * progress may yield incorrect results.<p>
     *
     * This field is used by the netlist implementation returned by the
     * <tt>getNetlist</tt> methods. If the value of this field changes unexpectedly,
     * the netlist will throw a <tt>ConcurrentModificationException</tt> in
     * response to the <tt>getNetwork</tt> and other operations.  This provides
     * <i>fail-fast</i> behavior, rather than non-deterministic behavior in
     * the face of concurrent modification during netlist examination.<p>
     */
//    int modCount = 0;
    WeakReference<Snapshot> expectedSnapshot; // for Schem netlists
    WeakReference<CellTree> expectedCellTree; // for Layout netlisits
    /**
     * Equivalence of ports.
     * equivPorts.size == ports.size.
     * equivPorts[i] contains minimal index among ports of its group.
     */
    int[] equivPortsN;
    int[] equivPortsP;
    int[] equivPortsA;
    /** Node offsets. */
    int[] ni_pi;
    /** */
    int arcsOffset;
    /** */
    private int[] headConn;
    /** */
    private int[] tailConn;
    /** */
    int[] drawns;
    /** */
    int numDrawns;
    /** */
    int numExportedDrawns;
    /** */
    int numConnectedDrawns;
    /** A map from canonic String to NetName. */
    HashMap<Name, GenMath.MutableInteger> netNames = new HashMap<Name, GenMath.MutableInteger>();
    /** Counter for enumerating NetNames. */
    private int netNameCount;
    /** Counter for enumerating NetNames. */
    int exportedNetNameCount;
    /** Netlist for ShortResistors.NO option. */
    NetlistImpl netlistN;
    /** Netlist for true ShortResistors.PARASITIC option. */
    NetlistShorted netlistP;
    /** Netlist for true ShortResistors.ALL option. */
    NetlistShorted netlistA;
    /** */
    private static PortProto busPinPort = Schematics.tech().busPinNode.getPort(0);
    /** */
    private static ArcProto busArc = Schematics.tech().bus_arc;

    NetCell(Cell cell) {
//        System.out.println("Create NetCell " + cell.libDescribe());
        this.database = cell.getDatabase();
        this.networkManager = database.getNetworkManager();
        this.isSchem = this instanceof NetSchem;
        if (isSchem) {
            expectedSnapshot = new WeakReference<Snapshot>(null);
        } else {
            expectedCellTree = new WeakReference<CellTree>(null);
        }
        this.cell = cell;
    }

    public static NetCell newInstance(Cell cell) {
        return cell.isIcon() || cell.isSchematic() ? new NetSchem(cell) : new NetCell(cell);
    }

    public Netlist getNetlist(Netlist.ShortResistors shortResistors) {
        if (isSchem) {
            if (database.backup() != expectedSnapshot.get()) {
                ((NetSchem) this).updateSchematic();
            }
        } else {
            if (cell.tree() != expectedCellTree.get()) {
                updateLayout();
            }
        }
        switch (shortResistors) {
            case NO:
                return netlistN;
            case PARASITIC:
                return netlistP;
            case ALL:
                return netlistA;
            default:
                throw new AssertionError();
        }
    }

    /**
     * Get an iterator over all of the Nodables of this Cell.
     */
    Iterator<Nodable> getNodables() {
        return cell.getNodables();
    }

    /**
     * Get a set of global signal in this Cell and its descendants.
     */
    Global.Set getGlobals() {
        return Global.Set.empty;
    }

    /*
     * Get offset in networks map for given global signal.
     */
    int getNetMapOffset(Global global) {
        return -1;
    }

    /**
     * Get offset in networks map for given global of nodable.
     * @param no nodable.
     * @param global global signal.
     * @return offset in networks map.
     */
    int getNetMapOffset(Nodable no, Global global) {
        return -1;
    }

    /**
     * Get offset in networks map for given subnetwork of nodable.
     * @param no nodable.
     * @param equivPortIndex index of entry in equivPortsX
     * @return offset in networks map.
     */
    int getNetMapOffset(Nodable no, int equivPortIndex) {
        NodeInst ni = (NodeInst) no;
        return drawns[ni_pi[ni.getNodeIndex()] + equivPortIndex];
    }

    /*
     * Get offset in networks map for given port instance.
     */
    int getNetMapOffset(Nodable no, PortProto portProto, int busIndex) {
        NodeInst ni = (NodeInst) no;
        return drawns[ni_pi[ni.getNodeIndex()] + portProto.getPortIndex()];
    }

    /**
     * Method to return the port width of port of the Nodable.
     * @return the either the port width.
     */
    int getBusWidth(Nodable no, PortProto portProto) {
        return 1;
    }

    /*
     * Get offset in networks map for given export..
     */
    int getNetMapOffset(Export export, int busIndex) {
        return drawns[export.getPortIndex()];
    }

    /*
     * Get offset in networks map for given export name.
     */
    int getNetMapOffset(Name exportName) {
        Export export = cell.findExport(exportName);
        return export != null ? drawns[export.getPortIndex()] : -1;
    }

    /*
     * Get offset in networks map for given arc.
     */
    int getNetMapOffset(ArcInst ai, int busIndex) {
        return getArcDrawn(ai);
    }

    /**
     * Method to return either the network name or the bus name on this ArcInst.
     * @return the either the network name or the bus name on this ArcInst.
     */
    Name getBusName(ArcInst ai) {
        return null;
    }

    /**
     * Method to return the bus width on this ArcInst.
     * @return the either the bus width on this ArcInst.
     */
    int getBusWidth(ArcInst ai) {
        int drawn = getArcDrawn(ai);
        if (drawn < 0) {
            return 0;
        }
        return 1;
    }

    int getArcDrawn(ArcInst ai) {
        assert ai.getParent() == cell;
        int arcIndex = cell.getMemoization().getArcIndex(ai.getD());
        return drawns[arcsOffset + arcIndex];
    }

    private void initConnections() {
        int numPorts = cell.getNumPorts();
        int numNodes = cell.getNumNodes();
        int numArcs = cell.getNumArcs();
        if (ni_pi == null || ni_pi.length != numNodes) {
            ni_pi = new int[numNodes];
        }
        int offset = numPorts;
        for (int i = 0; i < numNodes; i++) {
            NodeInst ni = cell.getNode(i);
            ni_pi[i] = offset;
            offset += ni.getProto().getNumPorts();
        }
        arcsOffset = offset;
        offset += numArcs;
        if (headConn == null || headConn.length != offset) {
            headConn = new int[offset];
            tailConn = new int[offset];
            drawns = new int[offset];
        }
        for (int i = numPorts; i < arcsOffset; i++) {
            headConn[i] = i;
            tailConn[i] = i;
        }
        for (int i = 0; i < numPorts; i++) {
            int portOffset = i;
            Export export = cell.getPort(i);
            int orig = getPortInstOffset(export.getOriginalPort());
            headConn[portOffset] = headConn[orig];
            headConn[orig] = portOffset;
            tailConn[portOffset] = -1;
        }
        int arcIndex = 0;
        for (Iterator<ArcInst> it = cell.getArcs(); arcIndex < numArcs; arcIndex++) {
            ArcInst ai = it.next();
            int arcOffset = arcsOffset + arcIndex;
            int head = getPortInstOffset(ai.getHeadPortInst());
            headConn[arcOffset] = headConn[head];
            headConn[head] = arcOffset;
            int tail = getPortInstOffset(ai.getTailPortInst());
            tailConn[arcOffset] = tailConn[tail];
            tailConn[tail] = arcOffset;
        }
        //showConnections();
    }
//	private void showConnections() {
//		int numNodes = cell.getNumNodes();
//		for (int i = 0; i < numNodes; i++) {
//			NodeInst ni = cell.getNode(i);
//			int numPortInsts = ni.getProto().getNumPorts();
//			for (int j = 0; j < numPortInsts; j++) {
//				PortInst pi = ni.getPortInst(j);
//				System.out.println("Connections of " + pi);
//				int piOffset = getPortInstOffset(pi);
//				for (int k = piOffset; headConn[k] != piOffset; ) {
//					k = headConn[k];
//					if (k >= arcsOffset)
//						System.out.println("\thead_arc\t" + cell.getArc(k - arcsOffset).describe(true));
//					else
//						System.out.println("\tport\t" + cell.getPort(k));
//				}
//				for (int k = piOffset; tailConn[k] != piOffset; ) {
//					k = tailConn[k];
//					System.out.println("\ttail_arc\t" + cell.getArc(k - arcsOffset).describe(true));
//				}
//			}
//		}
//	}
    private ArrayList<PortInst> stack;

    private void addToDrawn1(PortInst pi) {
        int piOffset = getPortInstOffset(pi);
        if (drawns[piOffset] >= 0) {
            return;
        }
        PortProto pp = pi.getPortProto();
        if (pp instanceof PrimitivePort && ((PrimitivePort) pp).isIsolated()) {
            return;
        }
        drawns[piOffset] = numDrawns;
        if (NetworkTool.debug) {
            System.out.println(numDrawns + ": " + pi);
        }

        for (int k = piOffset; headConn[k] != piOffset;) {
            k = headConn[k];
            if (drawns[k] >= 0) {
                continue;
            }
            if (k < arcsOffset) {
                // This is port
                drawns[k] = numDrawns;
                if (NetworkTool.debug) {
                    System.out.println(numDrawns + ": " + cell.getPort(k));
                }
                continue;
            }
            ArcInst ai = cell.getArc(k - arcsOffset);
            ArcProto ap = ai.getProto();
            if (ap.getFunction() == ArcProto.Function.NONELEC) {
                continue;
            }
            if (pp == busPinPort && ap != busArc) {
                continue;
            }
            drawns[k] = numDrawns;
            if (NetworkTool.debug) {
                System.out.println(numDrawns + ": " + ai);
            }
            PortInst tpi = ai.getTailPortInst();
            if (tpi.getPortProto() == busPinPort && ap != busArc) {
                continue;
            }
            stack.add(tpi);
        }
        for (int k = piOffset; tailConn[k] != piOffset;) {
            k = tailConn[k];
            if (drawns[k] >= 0) {
                continue;
            }
            ArcInst ai = cell.getArc(k - arcsOffset);
            ArcProto ap = ai.getProto();
            if (ap.getFunction() == ArcProto.Function.NONELEC) {
                continue;
            }
            if (pp == busPinPort && ap != busArc) {
                continue;
            }
            drawns[k] = numDrawns;
            if (NetworkTool.debug) {
                System.out.println(numDrawns + ": " + ai);
            }
            PortInst hpi = ai.getHeadPortInst();
            if (hpi.getPortProto() == busPinPort && ap != busArc) {
                continue;
            }
            stack.add(hpi);
        }
    }

    private void addToDrawn(PortInst pi) {
        assert stack.isEmpty();
        stack.add(pi);
        while (!stack.isEmpty()) {
            pi = stack.remove(stack.size() - 1);
            PortProto pp = pi.getPortProto();
            NodeProto np = pp.getParent();
            int numPorts = np.getNumPorts();
            if (numPorts == 1 || np instanceof Cell) {
                addToDrawn1(pi);
                continue;
            }
            NodeInst ni = pi.getNodeInst();
            int topology = ((PrimitivePort) pp).getTopology();
            for (int i = 0; i < numPorts; i++) {
                if (((PrimitivePort) np.getPort(i)).getTopology() != topology) {
                    continue;
                }
                addToDrawn1(ni.getPortInst(i));
            }
        }
    }

    void makeDrawns() {
        initConnections();
        Arrays.fill(drawns, -1);
        stack = new ArrayList<PortInst>();
        numDrawns = 0;
        for (int i = 0, numPorts = cell.getNumPorts(); i < numPorts; i++) {
            if (drawns[i] >= 0) {
                continue;
            }
            drawns[i] = numDrawns;
            Export export = cell.getPort(i);
            addToDrawn(export.getOriginalPort());
            numDrawns++;
        }
        numExportedDrawns = numDrawns;
        for (int i = 0, numArcs = cell.getNumArcs(); i < numArcs; i++) {
            if (drawns[arcsOffset + i] >= 0) {
                continue;
            }
            ArcInst ai = cell.getArc(i);
            ArcProto ap = ai.getProto();
            if (ap.getFunction() == ArcProto.Function.NONELEC) {
                continue;
            }
            drawns[arcsOffset + i] = numDrawns;
            if (NetworkTool.debug) {
                System.out.println(numDrawns + ": " + ai);
            }
            PortInst hpi = ai.getHeadPortInst();
            if (hpi.getPortProto() != busPinPort || ap == busArc) {
                addToDrawn(hpi);
            }
            PortInst tpi = ai.getTailPortInst();
            if (tpi.getPortProto() != busPinPort || ap == busArc) {
                addToDrawn(tpi);
            }
            numDrawns++;
        }
        numConnectedDrawns = numDrawns;
        for (int i = 0, numNodes = cell.getNumNodes(); i < numNodes; i++) {
            NodeInst ni = cell.getNode(i);
            NodeProto np = ni.getProto();
            if (ni.isIconOfParent()
                    || np.getFunction() == PrimitiveNode.Function.ART && np != Generic.tech().simProbeNode
                    || np == Artwork.tech().pinNode
                    || np == Generic.tech().invisiblePinNode) {
                continue;
            }
            int numPortInsts = np.getNumPorts();
            for (int j = 0; j < numPortInsts; j++) {
                PortInst pi = ni.getPortInst(j);
                int piOffset = getPortInstOffset(pi);
                if (drawns[piOffset] >= 0) {
                    continue;
                }
                if (pi.getPortProto() instanceof PrimitivePort && ((PrimitivePort) pi.getPortProto()).isIsolated()) {
                    continue;
                }
                addToDrawn(pi);
                numDrawns++;
            }
        }
        stack = null;
        headConn = tailConn = null;
        // showDrawns();
//  		System.out.println(cell + " has " + cell.getNumPorts() + " ports, " + cell.getNumNodes() + " nodes, " +
//  			cell.getNumArcs() + " arcs, " + (arcsOffset - cell.getNumPorts()) + " portinsts, " + netMap.length + "(" + piDrawns + ") drawns");
    }

    void showDrawns() {
        java.io.PrintWriter out;
        String filePath = "tttt";
        try {
            out = new java.io.PrintWriter(new java.io.BufferedWriter(new java.io.FileWriter(filePath, true)));
        } catch (java.io.IOException e) {
            System.out.println("Error opening " + filePath);
            return;
        }

        out.println("Drawns " + cell);
        int numPorts = cell.getNumPorts();
        for (int drawn = 0; drawn < numDrawns; drawn++) {
            for (int i = 0; i < drawns.length; i++) {
                if (drawns[i] != drawn) {
                    continue;
                }
                if (i < numPorts) {
                    out.println(drawn + ": " + cell.getPort(i));
                } else if (i >= arcsOffset) {
                    out.println(drawn + ": " + cell.getArc(i - arcsOffset));
                } else {
                    int k = 1;
                    for (; k < cell.getNumNodes() && ni_pi[k] <= i; k++);
                    k--;
                    NodeInst ni = cell.getNode(k);
                    PortInst pi = ni.getPortInst(i - ni_pi[k]);
                    out.println(drawn + ": " + pi);
                }
            }
        }
        out.close();
    }

    void initNetnames() {
        for (GenMath.MutableInteger nn : netNames.values()) {
            nn.setValue(-1);
        }
        netNameCount = 0;
        for (Iterator<Export> it = cell.getExports(); it.hasNext();) {
            Export e = it.next();
            addNetNames(e.getNameKey(), e, null);
        }
        exportedNetNameCount = netNameCount;
        for (Iterator<ArcInst> it = cell.getArcs(); it.hasNext();) {
            ArcInst ai = it.next();
            if (ai.getProto().getFunction() == ArcProto.Function.NONELEC) {
                continue;
            }
            if (ai.getNameKey().isBus() && ai.getProto() != busArc) {
                String msg = "Network: " + cell + " has bus name <" + ai.getNameKey() + "> on arc that is not a bus";
                System.out.println(msg);
                networkManager.pushHighlight(ai);
                networkManager.logError(msg, NetworkTool.errorSortNetworks);
            }
            if (ai.isUsernamed()) {
                addNetNames(ai.getNameKey(), null, ai);
            }
        }
        for (Iterator<Map.Entry<Name, GenMath.MutableInteger>> it = netNames.entrySet().iterator(); it.hasNext();) {
            Map.Entry<Name, GenMath.MutableInteger> e = it.next();
            Name name = e.getKey();
            int index = e.getValue().intValue();
            if (index < 0) {
                it.remove();
            } else if (NetworkTool.debug) {
                System.out.println("NetName " + name + " " + index);
            }
        }
        assert netNameCount == netNames.size();
    }

    void addNetNames(Name name, Export e, ArcInst ai) {
        if (name.isBus()) {
            System.out.println("Network: Layout " + cell + " has bus port/arc " + name);
        }
        addNetName(name, e, ai);
    }

    void addNetName(Name name, Export e, ArcInst ai) {
        GenMath.MutableInteger nn = netNames.get(name);
        if (nn == null) {
            nn = new GenMath.MutableInteger(-1);
            netNames.put(name, nn);
        }
        if (nn.intValue() < 0) {
            nn.setValue(netNameCount++);
        }
    }

    private void internalConnections(int[] netMapF, int[] netMapP, int[] netMapA) {
        for (Iterator<NodeInst> it = cell.getNodes(); it.hasNext();) {
            NodeInst ni = it.next();
            int nodeOffset = ni_pi[ni.getNodeIndex()];
            if (!ni.isCellInstance()) {
                PrimitiveNode.Function fun = ni.getFunction();
                if (fun == PrimitiveNode.Function.RESIST) {
                    Netlist.connectMap(netMapP, drawns[nodeOffset], drawns[nodeOffset + 1]);
                    Netlist.connectMap(netMapA, drawns[nodeOffset], drawns[nodeOffset + 1]);
                } else if (fun.isComplexResistor()) {
                    Netlist.connectMap(netMapA, drawns[nodeOffset], drawns[nodeOffset + 1]);
                }
                continue;
            }
            Cell subCell = (Cell) ni.getProto();
            if (subCell.isIcon() || subCell.isSchematic()) {
                continue;
            }
            EquivPorts eq = subCell.treeUnsafe().getEquivPorts();
            int[] eqN = eq.getEquivPortsN();
            int[] eqP = eq.getEquivPortsP();
            int[] eqA = eq.getEquivPortsA();
            for (int i = 0; i < eqN.length; i++) {
                if (eqN[i] != i) {
                    Netlist.connectMap(netMapF, drawns[nodeOffset + i], drawns[nodeOffset + eqN[i]]);
                }
                if (eqP[i] != i) {
                    Netlist.connectMap(netMapP, drawns[nodeOffset + i], drawns[nodeOffset + eqP[i]]);
                }
                if (eqA[i] != i) {
                    Netlist.connectMap(netMapA, drawns[nodeOffset + i], drawns[nodeOffset + eqA[i]]);
                }
            }
        }
    }

    final int getPortInstOffset(PortInst pi) {
        return ni_pi[pi.getNodeInst().getNodeIndex()] + pi.getPortProto().getPortIndex();
    }

    int getPortOffset(int portIndex, int busIndex) {
        return busIndex == 0 ? portIndex : -1;
    }

    NetSchem getSchem() {
        return null;
    }

    private void buildNetworkList(int[] netMapN) {
        netlistN = new NetlistImpl(this, numExportedDrawns, netMapN);
        int[] netNameToNetIndex = new int[netNames.size()];
        Arrays.fill(netNameToNetIndex, -1);
        int numPorts = cell.getNumPorts();
        for (int i = 0; i < numPorts; i++) {
            Export e = cell.getPort(i);
            int drawn = drawns[i];
            setNetName(netNameToNetIndex, drawn, e.getNameKey(), true);
            netlistN.setEquivPortIndexByNetIndex(i, netlistN.getNetIndex(e, 0));
        }
        int numArcs = cell.getNumArcs(), arcIndex = 0;
        for (Iterator<ArcInst> it = cell.getArcs(); arcIndex < numArcs; arcIndex++) {
            ArcInst ai = it.next();
            if (!ai.isUsernamed()) {
                continue;
            }
            int drawn = drawns[arcsOffset + arcIndex];
            if (drawn < 0) {
                continue;
            }
            setNetName(netNameToNetIndex, drawn, ai.getNameKey(), false);
        }
        arcIndex = 0;
        for (Iterator<ArcInst> it = cell.getArcs(); arcIndex < numArcs; arcIndex++) {
            ArcInst ai = it.next();
            int drawn = drawns[arcsOffset + arcIndex];
            if (drawn < 0) {
                continue;
            }
            int netIndexN = netlistN.getNetIndexByMap(drawn);
            if (netlistN.hasNames(netIndexN)) {
                continue;
            }
            netlistN.addTempName(netIndexN, ai.getName());
        }
        for (int i = 0; i < cell.getNumNodes(); i++) {
            NodeInst ni = cell.getNode(i);
            for (int j = 0; j < ni.getProto().getNumPorts(); j++) {
                int drawn = drawns[ni_pi[i] + j];
                if (drawn < 0) {
                    continue;
                }
                int netIndexN = netlistN.getNetIndexByMap(drawn);
                if (netlistN.hasNames(netIndexN)) {
                    continue;
                }
                netlistN.addTempName(netIndexN, ni.getName() + PORT_SEPARATOR + ni.getProto().getPort(j).getName());
            }
        }

        // check names and equivPortIndexByNetIndex map
        for (int i = 0, numNetworks = netlistN.getNumNetworks(); i < numNetworks; i++) {
            assert netlistN.hasNames(i);
            assert netlistN.isExported(i) == (i < netlistN.getNumExternalNetworks());
            if (netlistN.isExported(i)) {
                int equivPortIndex = netlistN.getEquivPortIndexByNetIndex(i);
                assert equivPortIndex >= 0 && equivPortIndex < numPorts;
            }
        }
        /*
        // debug info
        System.out.println("BuildNetworkList "+this);
        int i = 0;
        for (Iterator<Network> nit = getNetworks(); nit.hasNext(); )
        {
        Network network = nit.next();
        String s = "";
        for (Iterator<String> sit = network.getNames(); sit.hasNext(); )
        {
        String n = sit.next();
        s += "/"+ n;
        }
        for (Iterator<PortInst> pit = network.getPorts(); pit.hasNext(); )
        {
        PortInst pi = pit.next();
        s += "|"+pi.getNodeInst().getProto()+"&"+pi.getPortProto().getName();
        }
        System.out.println("    "+i+"    "+s);
        i++;
        }
         */
    }

    private void setNetName(int[] netNamesToNetIndex, int drawn, Name name, boolean exported) {
        int netIndexN = netlistN.getNetIndexByMap(drawn);
        assert netIndexN >= 0;
        GenMath.MutableInteger nn = netNames.get(name);
        if (netNamesToNetIndex[nn.intValue()] >= 0) {
            if (netNamesToNetIndex[nn.intValue()] == netIndexN) {
                return;
            }
            String msg = "Network: Layout " + cell + " has nets with same name " + name;
            System.out.println(msg);
            // because this should be an infrequent event that the user will fix, let's
            // put all the work here
            for (int i = 0, numPorts = cell.getNumPorts(); i < numPorts; i++) {
                Export e = cell.getPort(i);
                if (e.getName().equals(name.toString())) {
                    networkManager.pushHighlight(cell.getPort(i));
                }
            }
            for (Iterator<ArcInst> it = cell.getArcs(); it.hasNext();) {
                ArcInst ai = it.next();
                if (!ai.isUsernamed()) {
                    continue;
                }
                if (ai.getName().equals(name.toString())) {
                    networkManager.pushHighlight(ai);
                }
            }
            networkManager.logError(msg, NetworkTool.errorSortNetworks);
        } else {
            netNamesToNetIndex[nn.intValue()] = netIndexN;
        }
        netlistN.addUserName(netIndexN, name, exported);
    }

    /**
     * Update map of equivalent ports newEquivPort.
     */
    private boolean updateInterface() {
        boolean changed = false;
        int numPorts = cell.getNumPorts();
        if (equivPortsN == null || equivPortsN.length != numPorts) {
            changed = true;
            equivPortsN = new int[numPorts];
            equivPortsP = new int[numPorts];
            equivPortsA = new int[numPorts];
        }

        int[] netToPortN = new int[numPorts];
        int[] netToPortP = new int[numPorts];
        int[] netToPortA = new int[numPorts];
        Arrays.fill(netToPortN, -1);
        Arrays.fill(netToPortP, -1);
        Arrays.fill(netToPortA, -1);
        for (int i = 0; i < numPorts; i++) {
            int netN = netlistN.netMap[drawns[i]];
            if (netToPortN[netN] < 0) {
                netToPortN[netN] = i;
            }
            if (equivPortsN[i] != netToPortN[netN]) {
                changed = true;
                equivPortsN[i] = netToPortN[netN];
            }

            int netP = netlistP.netMap[drawns[i]];
            if (netToPortP[netP] < 0) {
                netToPortP[netP] = i;
            }
            if (equivPortsP[i] != netToPortP[netP]) {
                changed = true;
                equivPortsP[i] = netToPortP[netP];
            }

            int netA = netlistA.netMap[drawns[i]];
            if (netToPortA[netA] < 0) {
                netToPortA[netA] = i;
            }
            if (equivPortsA[i] != netToPortA[netA]) {
                changed = true;
                equivPortsA[i] = netToPortA[netA];
            }
        }

        if (CHECK_EQUIV_PORTS) {
            EquivPorts equivPorts = cell.tree().getEquivPorts();
            assert Arrays.equals(equivPortsN, equivPorts.getEquivPortsN());
            assert Arrays.equals(equivPortsP, equivPorts.getEquivPortsP());
            assert Arrays.equals(equivPortsA, equivPorts.getEquivPortsA());
        }

        return changed;
    }

//	/**
//	 * Show map of equivalent ports newEquivPort.
//	 */
//	private void showEquivPorts()
//	{
//		System.out.println("Equivalent ports of "+cell);
//		String s = "\t";
//		Export[] ports = new Export[cell.getNumPorts()];
//		int i = 0;
//		for (Iterator<Export> it = cell.getExports(); it.hasNext(); i++)
//			ports[i] = it.next();
//		for (i = 0; i < equivPorts.length; i++)
//		{
//			Export pi = ports[i];
//			if (equivPorts[i] != i) continue;
//			boolean found = false;
//			for (int j = i+1; j < equivPorts.length; j++)
//			{
//				if (equivPorts[i] != equivPorts[j]) continue;
//				Export pj = ports[j];
//				if (!found) s = s+" ( "+pi.getName();
//				found = true;
//				s = s+" "+pj.getName();
//			}
//			if (found)
//				s = s+")";
//			else
//				s = s+" "+pi.getName();
//		}
//		System.out.println(s);
//	}
    private void updateLayout() {
        synchronized (networkManager) {
            CellTree oldCellTree = expectedCellTree.get();
            CellTree newCellTree = cell.tree();
            if (oldCellTree == newCellTree) {
                return;
            }
            if (oldCellTree == null || !newCellTree.sameNetlist(oldCellTree)) {
                // clear errors for cell
                networkManager.startErrorLogging(cell);
                try {

                    makeDrawns();
                    // Gather port and arc names
                    initNetnames();

                    redoNetworks1();
                } finally {
                    networkManager.finishErrorLogging();
                }
            }
            expectedCellTree = new WeakReference<CellTree>(newCellTree);
        }
    }

    boolean redoNetworks1() {
        /* Set index of NodeInsts */
//        HashMap/*<Cell,Netlist>*/ subNetlists = new HashMap/*<Cell,Netlist>*/();
//        for (Iterator<Nodable> it = getNodables(); it.hasNext(); ) {
//            Nodable no = it.next();
//            if (!no.isCellInstance()) continue;
//            Cell subCell = (Cell)no.getProto();
//            subNetlists.put(subCell, networkManager.getNetlist(subCell, false));
//        }
        int[] netMapN = Netlist.initMap(numDrawns);
        int[] netMapP = netMapN.clone();
        int[] netMapA = netMapN.clone();
        internalConnections(netMapN, netMapP, netMapA);
        buildNetworkList(netMapN);
        netlistP = new NetlistShorted(netlistN, Netlist.ShortResistors.PARASITIC, netMapP);
        netlistA = new NetlistShorted(netlistN, Netlist.ShortResistors.ALL, netMapA);
        return updateInterface();
    }

    /**
     * Update netlists to current CellTree
     * Check if specified Netlist is no more fresh
     * @param netlist
     * @return true specified Netlist is no more fresh
     */
    boolean obsolete(Netlist netlist) {
        Netlist newNetlist = getNetlist(netlist.shortResistors);
        netlistN.expectedCellTree = netlistP.expectedCellTree = netlistA.expectedCellTree = expectedCellTree;
        return newNetlist != netlist;
    }
}
