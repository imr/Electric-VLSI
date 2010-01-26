/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: PrimitiveNode.java
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
package com.sun.electric.technology;

import com.sun.electric.database.EObjectInputStream;
import com.sun.electric.database.EObjectOutputStream;
import com.sun.electric.database.EditingPreferences;
import com.sun.electric.database.ImmutableNodeInst;
import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.geometry.Dimension2D;
import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.geometry.ERectangle;
import com.sun.electric.database.geometry.Orientation;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.id.PortProtoId;
import com.sun.electric.database.id.PrimitiveNodeId;
import com.sun.electric.database.id.PrimitivePortId;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.ArrayIterator;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.technology.technologies.Schematics;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * A PrimitiveNode represents information about a NodeProto that lives in a
 * Technology.  It has a name, and several functions that describe how
 * to draw it
 */
public class PrimitiveNode implements NodeProto, Comparable<PrimitiveNode>, Serializable
{
	/**
	 * Function is a typesafe enum class that describes the function of a NodeProto.
	 * Functions are technology-independent and include different types of transistors,
	 * contacts, and other circuit elements.
	 */
	public static enum Function
	{
		/** Describes a node with unknown behavior. */
		UNKNOWN("unknown", "node", false, false),

		/** Describes a single-layer pin.
		 * Pins connects wires of a single layer, have no geometry, and connect in the center of the node. */
		PIN("pin", "pin", false, false),

		/** Describes a two-layer contact.
		 * Contacts connects wires of two different layers in the center of the node. */
		CONTACT("contact", "contact", false, false),

		/** Describes a pure-layer node.
		 * Pure-layer nodes have a solid piece of geometry on a single layer. */
		NODE("pure-layer-node", "plnode", false, false),

		/** Describes a node that connects all ports. */
		CONNECT("connection", "conn", false, false),

		/** Describes an nMOS transistor.
		 * It has gate on the first and third ports, the source on the second port, and the drain on the fourth port. */
		TRANMOS("nMOS-transistor", "nmos", true, false),

		/** Describes a pMOS transistor.
		 * It has gate on the first and third ports, the source on the second port, and the drain on the fourth port. */
		TRAPMOS("pMOS-transistor", "pmos", true, false),

		/** Describes an nMOS depletion transistor (should be named TRANMOSD but isn't for historical purposes).
		 * It has gate on the first and third ports, the source on the second port, and the drain on the fourth port. */
		TRADMOS("depletion-nMOS-transistor", "nmos-d", true, false),

		/** Describes a pMOS depletion transistor.
		 * It has gate on the first and third ports, the source on the second port, and the drain on the fourth port. */
		TRAPMOSD("depletion-pMOS-transistor", "pmos-d", true, false),

		/** Describes an nMOS native transistor.
		 * It has gate on the first and third ports, the source on the second port, and the drain on the fourth port. */
		TRANMOSNT("native-nMOS-transistor", "nmos-nt", true, false),

		/** Describes a pMOS native transistor.
		 * It has gate on the first and third ports, the source on the second port, and the drain on the fourth port. */
		TRAPMOSNT("native-pMOS-transistor", "pmos-nt", true, false),

		/** Describes an nMOS floating gate transistor.
		 * It has gate on the first and third ports, the source on the second port, and the drain on the fourth port. */
		TRANMOSFG("floating-gate-nMOS-transistor", "nmos-fg", true, false),

		/** Describes a pMOS floating gate transistor.
		 * It has gate on the first and third ports, the source on the second port, and the drain on the fourth port. */
		TRAPMOSFG("floating-gate-pMOS-transistor", "pmos-fg", true, false),

		/** Describes an nMOS carbon nanotube transistor.
		 * It has gate on the first and third ports, the source on the second port, and the drain on the fourth port. */
		TRANMOSCN("carbon-nanotube-nMOS-transistor", "nmos-cn", true, false),

		/** Describes a pMOS carbon nanotube transistor.
		 * It has gate on the first and third ports, the source on the second port, and the drain on the fourth port. */
		TRAPMOSCN("carbon-nanotube-pMOS-transistor", "pmos-cn", true, false),

		/** Describes an nMOS low-threshold transistor.
		 * It has gate on the first and third ports, the source on the second port, and the drain on the fourth port. */
		TRANMOSVTL("low-threshold-nMOS-transistor", "nmos-vtl", true, false),

		/** Describes a pMOS low-threshold transistor.
		 * It has gate on the first and third ports, the source on the second port, and the drain on the fourth port. */
		TRAPMOSVTL("low-threshold-pMOS-transistor", "pmos-vtl", true, false),

		/** Describes an nMOS high-threshold transistor.
		 * It has gate on the first and third ports, the source on the second port, and the drain on the fourth port. */
		TRANMOSVTH("high-threshold-nMOS-transistor", "nmos-vth", true, false),

		/** Describes a pMOS high-threshold transistor.
		 * It has gate on the first and third ports, the source on the second port, and the drain on the fourth port. */
		TRAPMOSVTH("high-threshold-pMOS-transistor", "pmos-vth", true, false),

		/** Describes an nMOS high-voltage (1) transistor.
		 * It has gate on the first and third ports, the source on the second port, and the drain on the fourth port. */
		TRANMOSHV1("high-voltage-1-nMOS-transistor", "nmos-hv1", true, false),

		/** Describes a pMOS high-voltage (1) transistor.
		 * It has gate on the first and third ports, the source on the second port, and the drain on the fourth port. */
		TRAPMOSHV1("high-voltage-1-pMOS-transistor", "pmos-hv1", true, false),

		/** Describes an nMOS higher-voltage (2) transistor.
		 * It has gate on the first and third ports, the source on the second port, and the drain on the fourth port. */
		TRANMOSHV2("high-voltage-2-nMOS-transistor", "nmos-hv2", true, false),

		/** Describes a pMOS higher-voltage (2) transistor.
		 * It has gate on the first and third ports, the source on the second port, and the drain on the fourth port. */
		TRAPMOSHV2("high-voltage-2-pMOS-transistor", "pmos-hv2", true, false),

		/** Describes an nMOS highest-voltage (3) transistor.
		 * It has gate on the first and third ports, the source on the second port, and the drain on the fourth port. */
		TRANMOSHV3("high-voltage-3-nMOS-transistor", "nmos-hv3", true, false),

		/** Describes a pMOS highest-voltage (3) transistor.
		 * It has gate on the first and third ports, the source on the second port, and the drain on the fourth port. */
		TRAPMOSHV3("high-voltage-3-pMOS-transistor", "pmos-hv3", true, false),

		/** Describes an nMOS native high-voltage (1) transistor.
		 * It has gate on the first and third ports, the source on the second port, and the drain on the fourth port. */
		TRANMOSNTHV1("native-high-voltage-1-nMOS-transistor", "nmos-nt-hv1", true, false),

		/** Describes a pMOS native high-voltage (1) transistor.
		 * It has gate on the first and third ports, the source on the second port, and the drain on the fourth port. */
		TRAPMOSNTHV1("native-high-voltage-1-pMOS-transistor", "pmos-nt-hv1", true, false),

		/** Describes an nMOS native higher-voltage (2) transistor.
		 * It has gate on the first and third ports, the source on the second port, and the drain on the fourth port. */
		TRANMOSNTHV2("native-high-voltage-2-nMOS-transistor", "nmos-nt-hv2", true, false),

		/** Describes a pMOS native higher-voltage (2) transistor.
		 * It has gate on the first and third ports, the source on the second port, and the drain on the fourth port. */
		TRAPMOSNTHV2("native-high-voltage-2-pMOS-transistor", "pmos-nt-hv2", true, false),

		/** Describes an nMOS native highest-voltage (3) transistor.
		 * It has gate on the first and third ports, the source on the second port, and the drain on the fourth port. */
		TRANMOSNTHV3("native-high-voltage-3-nMOS-transistor", "nmos-nt-hv3", true, false),

		/** Describes a pMOS native highest-voltage (3) transistor.
		 * It has gate on the first and third ports, the source on the second port, and the drain on the fourth port. */
		TRAPMOSNTHV3("native-high-voltage-3-pMOS-transistor", "pmos-nt-hv3", true, false),

		/** Describes a NPN junction transistor.
		 * It has base on the first port, emitter on the second port, and collector on the third port. */
		TRANPN("NPN-transistor", "npn", true, false),

		/** Describes a PNP junction transistor.
		 * It has base on the first port, emitter on the second port, and collector on the third port. */
		TRAPNP("PNP-transistor", "pnp", true, false),

		/** Describes a N-channel junction transistor.
		 * It has gate on the first port, source on the second port, and drain on the third port. */
		TRANJFET("n-type-JFET-transistor", "njfet", true, false),

		/** Describes a P-channel junction transistor.
		 * It has gate on the first port, source on the second port, and drain on the third port. */
		TRAPJFET("p-type-JFET-transistor", "pjfet", true, false),

		/** Describes a MESFET depletion transistor.
		 * It has gate on the first port, source on the second port, and drain on the third port. */
		TRADMES("depletion-mesfet", "dmes", true, false),

		/** Describes a MESFET enhancement transistor.
		 * It has gate on the first port, source on the second port, and drain on the third port. */
		TRAEMES("enhancement-mesfet", "emes", true, false),

		/** Describes a general-purpose transistor.
		 * It is defined self-referentially by the prototype name of the primitive. */
		TRANSREF("prototype-defined-transistor", "tref", true, false),

		/** Describes an undetermined transistor.
		 * It has gate on the first port, source on the second port, and drain on the third port.
		 * The specific transistor type can be determined by examining the value from the NodeInst's "getTechSpecific" method. */
		TRANS("transistor", "trans", true, false),

		/** Describes a 4-port nMOS transistor.
		 * It has gate on the first port, source on the second port, drain on the third port, and substrate on the fourth port. */
		TRA4NMOS("nMOS-4-port-transistor", "nmos-4", true, false),

		/** Describes a 4-port pMOS transistor.
		 * It has gate on the first port, source on the second port, drain on the third port, and substrate on the fourth port. */
		TRA4PMOS("pMOS-4-port-transistor", "pmos-4", true, false),

		/** Describes a 4-port nMOS depletion transistor (should be named TRA4NMOSD but isn't for historical purposes).
		 * It has gate on the first port, source on the second port, drain on the third port, and substrate on the fourth port. */
		TRA4DMOS("depletion-nMOS-4-port-transistor", "nmos-d-4", true, false),

		/** Describes a 4-port pMOS depletion transistor.
		 * It has gate on the first port, source on the second port, drain on the third port, and substrate on the fourth port. */
		TRA4PMOSD("depletion-pMOS-4-port-transistor", "pmos-d-4", true, false),

		/** Describes a 4-port nMOS native transistor.
		 * It has gate on the first port, source on the second port, drain on the third port, and substrate on the fourth port. */
		TRA4NMOSNT("native-nMOS-4-port-transistor", "nmos-nt-4", true, false),

		/** Describes a 4-port pMOS native transistor.
		 * It has gate on the first port, source on the second port, drain on the third port, and substrate on the fourth port. */
		TRA4PMOSNT("native-pMOS-4-port-transistor", "pmos-nt-4", true, false),

		/** Describes a 4-port nMOS floating gate transistor.
		 * It has gate on the first and third ports, the source on the second port, and the drain on the fourth port. */
		TRA4NMOSFG("floating-gate-nMOS-4-port-transistor", "nmos-fg-4", true, false),

		/** Describes a 4-port pMOS floating gate transistor.
		 * It has gate on the first and third ports, the source on the second port, and the drain on the fourth port. */
		TRA4PMOSFG("floating-gate-pMOS-4-port-transistor", "pmos-fg-4", true, false),

		/** Describes a 4-port nMOS carbon nanotube transistor.
		 * It has gate on the first and third ports, the source on the second port, and the drain on the fourth port. */
		TRA4NMOSCN("carbon-nanotube-nMOS-4-port-transistor", "nmos-cn-4", true, false),

		/** Describes a 4-port pMOS carbon nanotube transistor.
		 * It has gate on the first and third ports, the source on the second port, and the drain on the fourth port. */
		TRA4PMOSCN("carbon-nanotube-pMOS-4-port-transistor", "pmos-cn-4", true, false),

		/** Describes a 4-port nMOS low-threshold transistor.
		 * It has gate on the first port, source on the second port, drain on the third port, and substrate on the fourth port. */
		TRA4NMOSVTL("low-threshold-nMOS-4-port-transistor", "nmos-vtl-4", true, false),

		/** Describes a 4-port pMOS low-threshold transistor.
		 * It has gate on the first port, source on the second port, drain on the third port, and substrate on the fourth port. */
		TRA4PMOSVTL("low-threshold-pMOS-4-port-transistor", "pmos-vtl-4", true, false),

		/** Describes a 4-port nMOS high-threshold transistor.
		 * It has gate on the first port, source on the second port, drain on the third port, and substrate on the fourth port. */
		TRA4NMOSVTH("high-threshold-nMOS-4-port-transistor", "nmos-vth-4", true, false),

		/** Describes a 4-port pMOS high-threshold transistor.
		 * It has gate on the first port, source on the second port, drain on the third port, and substrate on the fourth port. */
		TRA4PMOSVTH("high-threshold-pMOS-4-port-transistor", "pmos-vth-4", true, false),

		/** Describes a 4-port nMOS high-voltage (1) transistor.
		 * It has gate on the first port, source on the second port, drain on the third port, and substrate on the fourth port. */
		TRA4NMOSHV1("high-voltage-1-nMOS-4-port-transistor", "nmos-hv1-4", true, false),

		/** Describes a 4-port pMOS high-threshold (1) transistor.
		 * It has gate on the first port, source on the second port, drain on the third port, and substrate on the fourth port. */
		TRA4PMOSHV1("high-voltage-1-pMOS-4-port-transistor", "pmos-hv1-4", true, false),

		/** Describes a 4-port nMOS higher-voltage (2) transistor.
		 * It has gate on the first port, source on the second port, drain on the third port, and substrate on the fourth port. */
		TRA4NMOSHV2("high-voltage-2-nMOS-4-port-transistor", "nmos-hv2-4", true, false),

		/** Describes a 4-port pMOS higher-threshold (2) transistor.
		 * It has gate on the first port, source on the second port, drain on the third port, and substrate on the fourth port. */
		TRA4PMOSHV2("high-voltage-2-pMOS-4-port-transistor", "pmos-hv2-4", true, false),

		/** Describes a 4-port nMOS highest-voltage (3) transistor.
		 * It has gate on the first port, source on the second port, drain on the third port, and substrate on the fourth port. */
		TRA4NMOSHV3("high-voltage-3-nMOS-4-port-transistor", "nmos-hv3-4", true, false),

		/** Describes a 4-port pMOS highest-threshold (3) transistor.
		 * It has gate on the first port, source on the second port, drain on the third port, and substrate on the fourth port. */
		TRA4PMOSHV3("high-voltage-3-pMOS-4-port-transistor", "pmos-hv3-4", true, false),

		/** Describes a 4-port nMOS native high-voltage (1) transistor.
		 * It has gate on the first port, source on the second port, drain on the third port, and substrate on the fourth port. */
		TRA4NMOSNTHV1("native-high-voltage-1-nMOS-4-port-transistor", "nmos-nt-hv1-4", true, false),

		/** Describes a 4-port pMOS native high-threshold (1) transistor.
		 * It has gate on the first port, source on the second port, drain on the third port, and substrate on the fourth port. */
		TRA4PMOSNTHV1("native-high-voltage-1-pMOS-4-port-transistor", "pmos-nt-hv1-4", true, false),

		/** Describes a 4-port nMOS native higher-voltage (2) transistor.
		 * It has gate on the first port, source on the second port, drain on the third port, and substrate on the fourth port. */
		TRA4NMOSNTHV2("native-high-voltage-2-nMOS-4-port-transistor", "nmos-nt-hv2-4", true, false),

		/** Describes a 4-port pMOS native higher-threshold (2) transistor.
		 * It has gate on the first port, source on the second port, drain on the third port, and substrate on the fourth port. */
		TRA4PMOSNTHV2("native-high-voltage-2-pMOS-4-port-transistor", "pmos-nt-hv2-4", true, false),

		/** Describes a 4-port nMOS native highest-voltage (3) transistor.
		 * It has gate on the first port, source on the second port, drain on the third port, and substrate on the fourth port. */
		TRA4NMOSNTHV3("native-high-voltage-3-nMOS-4-port-transistor", "nmos-nt-hv3-4", true, false),

		/** Describes a 4-port pMOS native highest-threshold (3) transistor.
		 * It has gate on the first port, source on the second port, drain on the third port, and substrate on the fourth port. */
		TRA4PMOSNTHV3("native-high-voltage-3-pMOS-4-port-transistor", "pmos-nt-hv3-4", true, false),

		/** Describes a 4-port NPN junction transistor.
		 * It has base on the first port, emitter on the second port, collector on the third port, and substrate on the fourth port. */
		TRA4NPN("NPN-4-port-transistor", "npn-4", true, false),

		/** Describes a 4-port PNP junction transistor.
		 * It has base on the first port, emitter on the second port, collector on the third port, and substrate on the fourth port. */
		TRA4PNP("PNP-4-port-transistor", "pnp-4", true, false),

		/** Describes a 4-port N-channel junction transistor.
		 * It has gate on the first port, source on the second port, drain on the third port, and substrate on the fourth port. */
		TRA4NJFET("n-type-JFET-4-port-transistor", "njfet-4", true, false),

		/** Describes a 4-port P-channel junction transistor.
		 * It has gate on the first port, source on the second port, drain on the third port, and substrate on the fourth port. */
		TRA4PJFET("p-type-JFET-4-port-transistor", "pjfet-4", true, false),

		/** Describes a 4-port MESFET depletion transistor.
		 * It has gate on the first port, source on the second port, drain on the third port, and substrate on the fourth port. */
		TRA4DMES("4-port-depletion-mesfet", "dmes-4", true, false),

		/** Describes a 4-port MESFET enhancement transistor.
		 * It has gate on the first port, source on the second port, drain on the third port, and substrate on the fourth port. */
		TRA4EMES("4-port-enhancement-mesfet", "emes-4", true, false),

		/** Describes a general-purpose transistor.
		 * It has gate on the first port, source on the second port, drain on the third port, and substrate on the fourth port.
		 * The specific transistor type can be determined by examining the value from the NodeInst's "getTechSpecific" method. */
		TRANS4("4-port-transistor", "trans-4", true, false),

		/** Describes a resistor. */
		RESIST("resistor", "res", false, false),

		/** Describes a poly resistor. */
		//@Deprecated
		PRESIST("poly-resistor", "pres", false, false),

        /** Describes a hi ressistant poly resistor. */
		RESHIRESPOLY2("hi-res-poly2-resistor", "p2res", false, false),

        /** Describes an n-poly resistor */
		RESNPOLY("n-type-poly-resistor", "resnpoly", false, false),

		/** Describes a p-poly resistor */
		RESPPOLY("p-type-poly-resistor", "resppoly", false, false),

        /** Describes an n-poly non silicide resistor */
		RESNNSPOLY("n-type-no-silicide-poly-resistor", "resnnspoly", false, false),

		/** Describes a p-poly non silicide resistor */
		RESPNSPOLY("p-type-no-silicide-poly-resistor", "respnspoly", false, false),

        /** Describes a well resistor. */
		//@Deprecated
		WRESIST("well-resistor", "wres", false, false),

		/** Describes an n-well resistor */
		RESNWELL("n-type-well-resistor", "resnwell", false, false),

		/** Describes a p-well resistor */
		RESPWELL("p-type-well-resistor", "respwell", false, false),

        /** Describes an n-active resistor */
		RESNACTIVE("n-type-active-resistor", "resnactive", false, false),

		/** Describes a p-active resistor */
		RESPACTIVE("p-type-active-resistor", "respactive", false, false),

        /** Describes an esd device */
		ESDDEVICE("esd-device", "esdd", false, false),

		/** Describes a capacitor. */
		CAPAC("capacitor", "cap", false, false),

		/** Describes an electrolytic capacitor. */
		ECAPAC("electrolytic-capacitor", "ecap", false, false),

        /** Describes a poly2 capacitor. */
		POLY2CAPAC("poly2-capacitor", "poly2cap", false, false),

        /** Describes a diode. */
		DIODE("diode", "diode", false, false),

		/** Describes a zener diode. */
		DIODEZ("zener-diode", "zdiode", false, false),

		/** Describes an inductor. */
		INDUCT("inductor", "ind", false, false),

		/** Describes a meter. */
		METER("meter", "meter", false, false),

		/** Describes a transistor base. */
		BASE("base", "base", false, false),

		/** Describes a transistor emitter. */
		EMIT("emitter", "emit", false, false),

		/** Describes a transistor collector. */
		COLLECT("collector", "coll", false, false),

		/** Describes a buffer.
		 * It has input on the first port, clocking on the second port, and output on the third port. */
		BUFFER("buffer", "buf", false, false),

		/** Describes an AND gate.
		 * It has inputs on the first port and output on the second port. */
		GATEAND("AND-gate", "and", false, false),

		/** Describes an OR gate.
		 * It has inputs on the first port and output on the second port. */
		GATEOR("OR-gate", "or", false, false),

		/** Describes an XOR gate.
		 * It has inputs on the first port and output on the second port. */
		GATEXOR("XOR-gate", "xor", false, false),

		/** Describes a RS flip-flop with master-slave triggering. */
		FLIPFLOPRSMS("flip-flop-RS-MS", "ffRSms", false, true),

		/** Describes a RS flip-flop with positive triggering. */
		FLIPFLOPRSP("flip-flop-RS-P", "ffRSp", false, true),

		/** Describes a RS flip-flop with negative triggering. */
		FLIPFLOPRSN("flip-flop-RS-N", "ffRSn", false, true),

		/** Describes a JK flip-flop with master-slave triggering. */
		FLIPFLOPJKMS("flip-flop-JK-MS", "ffJKms", false, true),

		/** Describes a JK flip-flop with positive triggering. */
		FLIPFLOPJKP("flip-flop-JK-P", "ffJKp", false, true),

		/** Describes a JK flip-flop with negative triggering. */
		FLIPFLOPJKN("flip-flop-JK-N", "ffJKn", false, true),

		/** Describes a D flip-flop with master-slave triggering. */
		FLIPFLOPDMS("flip-flop-D-MS", "ffDms", false, true),

		/** Describes a D flip-flop with positive triggering. */
		FLIPFLOPDP("flip-flop-D-P", "ffDp", false, true),

		/** Describes a D flip-flop with negative triggering. */
		FLIPFLOPDN("flip-flop-D-N", "ffDn", false, true),

		/** Describes a T flip-flop with master-slave triggering. */
		FLIPFLOPTMS("flip-flop-T-MS", "ffTms", false, true),

		/** Describes a T flip-flop with positive triggering. */
		FLIPFLOPTP("flip-flop-T-P", "ffTp", false, true),

		/** Describes a T flip-flop with negative triggering. */
		FLIPFLOPTN("flip-flop-T-N", "ffTn", false, true),

		/** Describes a multiplexor. */
		MUX("multiplexor", "mux", false, false),

		/** Describes a power connection. */
		CONPOWER("power", "pwr", false, false),

		/** Describes a ground connection. */
		CONGROUND("ground", "gnd", false, false),

		/** Describes voltage or current source. */
		SOURCE("source", "source", false, false),

		/** Describes a substrate contact. */
		SUBSTRATE("substrate", "substr", false, false),

		/** Describes a well contact. */
		WELL("well", "well", false, false),

		/** Describes a pure artwork. */
		ART("artwork", "art", false, false),

		/** Describes an array. */
		ARRAY("array", "array", false, false),

		/** Describes an alignment object. */
		ALIGN("align", "align", false, false),

		/** Describes a current-controlled voltage source. */
		CCVS("ccvs", "ccvs", false, false),

		/** Describes a current-controlled current source. */
		CCCS("cccs", "cccs", false, false),

		/** Describes a voltage-controlled voltage source. */
		VCVS("vcvs", "vcvs", false, false),

		/** Describes a voltage-controlled current source. */
		VCCS("vccs", "vccs", false, false),

		/** Describes a transmission line. */
		TLINE("transmission-line", "transm", false, false);

		private final String name;
		private final String shortName;
		private final Name basename;
		private final boolean isTransistor;
		private final boolean isFlipFlop;

		private Function(String name, String shortName, boolean isTransistor, boolean isFlipFlop)
		{
			this.name = name;
			this.shortName = shortName;
			this.basename = Name.findName(TextUtils.canonicString(shortName)+"@0").getBasename();
			this.isTransistor = isTransistor;
			this.isFlipFlop = isFlipFlop;
		}

		/**
		 * Method to return a List of all Functions that exist.
		 * @return a List of all Functions that exist.
		 */
		public static List<Function> getFunctions() { return Arrays.asList(Function.class.getEnumConstants()); }

        /**
         * Method to find a Function by its type name.
         * @param typeName the name of the type to find.
		 * @return a Function (null if not found).
		 */
		public static Function findType(String typeName)
		{
            List<Function> allFuncs = getFunctions();
			for(Function fun : allFuncs)
			{
				if (fun.enumName().equalsIgnoreCase(typeName)) return fun;
			}
			return null;
		}

        /**
		 * Method to find a Function from its name.
		 * @param name the name to find.
		 * @return a Function (null if not found).
		 */
		public static Function findName(String name)
		{
			List<Function> allFuncs = getFunctions();
			for(Function fun : allFuncs)
			{
				if (fun.name.equalsIgnoreCase(name)) return fun;
			}
			return null;
		}

		/**
		 * Returns a name of this Function.
		 * @return a name of this Function.
		 */
		public String getName() { return name; }

		/**
		 * Returns the constant name for this Function.
		 * Constant names are used when writing Java code, so they must be the same as the actual symbol name.
		 * @return the constant name for this Function.
		 */
		public String getConstantName() { return name(); }

		/**
		 * Returns a short name of this Function.
		 * @return a short name of this Function.
		 */
		public String getShortName() { return shortName; }

		/**
		 * Returns a base name of this Function for autonaming.
		 * @return a base name of this Function for autonaming.
		 */
		public Name getBasename() { return basename; }

        /**
         * Method to tell whether this function describes a contact.
         * @return true if this function is a contact.
         */
        public boolean isContact() { return this == CONTACT; }

        /**
         * Method to tell whether this function describes a pin.
         * @return true if this function is a pin.
         */
        public boolean isPin() { return this == PIN; }

        /**
         * Method to tell whether this function describes a capacitor (normal or electrolytic).
         * @return true if this function describes a capacitor (normal or electrolytic).
         */
        public boolean isCapacitor() { return this == CAPAC || this == ECAPAC || this == POLY2CAPAC; }

        /**
         * Method to tell whether this function describes a resistor (normal, poly or nwell resistor).
         * @return true if this function describes a resistor (normal, poly or nwell resistor).
         */
        public boolean isResistor() { return this == RESIST || isComplexResistor();}

        /**
         * Method to tell whether this function describes a non simple resistor
         * @return true if this function describes a non simple resistor.
         */
        public boolean isComplexResistor() { return this == PRESIST || this == WRESIST || this == RESHIRESPOLY2 ||
            this == RESNPOLY || this == RESPPOLY ||
            this == RESNNSPOLY || this == RESPNSPOLY ||
            this == RESNWELL || this == RESPWELL ||
            this == RESNACTIVE || this == RESPACTIVE;}

        /**
         * Method to tell whether this function describes an ESD device.
         * @return true if this function describes an ESD device.
         */
        public boolean isESDDevice() {return this == ESDDEVICE;}

		/**
		 * Method to tell whether this function describes a transistor.
		 * @return true if this function describes a transistor.
		 */
		public boolean isTransistor() { return isTransistor; }

		/**
		 * Method to tell whether this function describes a field-effect transtor.
		 * This includes the nMOS, PMOS, and DMOS transistors, as well as the DMES and EMES transistors.
		 * @return true if this function describes a field-effect transtor.
		 */
		public boolean isFET()
		{
			if (this == TRANMOS      || this == TRA4NMOS      || this == TRAPMOS      || this == TRA4PMOS ||
				this == TRADMOS      || this == TRA4DMOS      || this == TRAPMOSD     || this == TRA4PMOSD ||
				this == TRANMOSNT    || this == TRA4NMOSNT    || this == TRAPMOSNT    || this == TRA4PMOSNT ||
				this == TRANMOSFG    || this == TRA4NMOSFG    || this == TRAPMOSFG    || this == TRA4PMOSFG ||
				this == TRANMOSCN    || this == TRA4NMOSCN    || this == TRAPMOSCN    || this == TRA4PMOSCN ||
				this == TRANMOSVTL   || this == TRA4NMOSVTL   || this == TRAPMOSVTL   || this == TRA4PMOSVTL ||
				this == TRANMOSVTH   || this == TRA4NMOSVTH   || this == TRAPMOSVTH   || this == TRA4PMOSVTH ||
				this == TRANMOSHV1   || this == TRA4NMOSHV1   || this == TRAPMOSHV1   || this == TRA4PMOSHV1 ||
				this == TRANMOSHV2   || this == TRA4NMOSHV2   || this == TRAPMOSHV2   || this == TRA4PMOSHV2 ||
				this == TRANMOSHV3   || this == TRA4NMOSHV3   || this == TRAPMOSHV3   || this == TRA4PMOSHV3 ||
				this == TRANMOSNTHV1 || this == TRA4NMOSNTHV1 || this == TRAPMOSNTHV1 || this == TRA4PMOSNTHV1 ||
				this == TRANMOSNTHV2 || this == TRA4NMOSNTHV2 || this == TRAPMOSNTHV2 || this == TRA4PMOSNTHV2 ||
				this == TRANMOSNTHV3 || this == TRA4NMOSNTHV3 || this == TRAPMOSNTHV3 || this == TRA4PMOSNTHV3 ||
				this == TRANJFET     || this == TRA4NJFET     || this == TRAPJFET     || this == TRA4PJFET ||
				this == TRADMES      || this == TRA4DMES      || this == TRAEMES      || this == TRA4EMES)
					return true;
			return false;
		}

		/**
		 * Method to tell whether this function describes a 4-port transtor and to return the equivalent 3-port function.
		 * @return the equivalent 3-port transistor function for this 4-port function.
		 * Returns null if this is not a 4-port transistor function.
		 */
		public Function make3PortTransistor()
		{
			if (!isTransistor) return null;
			if (this == TRA4NMOS) return TRANMOS;
			if (this == TRA4PMOS) return TRAPMOS;
			if (this == TRA4DMOS) return TRADMOS;
			if (this == TRA4PMOSD) return TRAPMOSD;
			if (this == TRA4NMOSNT) return TRANMOSNT;
			if (this == TRA4PMOSNT) return TRAPMOSNT;
			if (this == TRA4NMOSFG) return TRANMOSFG;
			if (this == TRA4PMOSFG) return TRAPMOSFG;
			if (this == TRA4NMOSCN) return TRANMOSCN;
			if (this == TRA4PMOSCN) return TRAPMOSCN;
			if (this == TRA4NMOSVTL) return TRANMOSVTL;
			if (this == TRA4PMOSVTL) return TRAPMOSVTL;
			if (this == TRA4NMOSVTH) return TRANMOSVTH;
			if (this == TRA4PMOSVTH) return TRAPMOSVTH;
			if (this == TRA4NMOSHV1) return TRANMOSHV1;
			if (this == TRA4PMOSHV1) return TRAPMOSHV1;
			if (this == TRA4NMOSHV2) return TRANMOSHV2;
			if (this == TRA4PMOSHV2) return TRAPMOSHV2;
			if (this == TRA4NMOSHV3) return TRANMOSHV3;
			if (this == TRA4PMOSHV3) return TRAPMOSHV3;
			if (this == TRA4NMOSNTHV1) return TRANMOSNTHV1;
			if (this == TRA4PMOSNTHV1) return TRAPMOSNTHV1;
			if (this == TRA4NMOSNTHV2) return TRANMOSNTHV2;
			if (this == TRA4PMOSNTHV2) return TRAPMOSNTHV2;
			if (this == TRA4NMOSNTHV3) return TRANMOSNTHV3;
			if (this == TRA4PMOSNTHV3) return TRAPMOSNTHV3;
			if (this == TRA4NPN) return TRANPN;
			if (this == TRA4PNP) return TRAPNP;
			if (this == TRA4NJFET) return TRANJFET;
			if (this == TRA4PJFET) return TRAPJFET;
			if (this == TRA4DMES) return TRADMES;
			if (this == TRA4EMES) return TRAEMES;
			if (this == TRANS4) return TRANS;
			return null;
		}

		/**
		 * Method to tell whether this function describes an n-Type transtor.
		 * This includes the MOS transistors, as well as the DMES and EMES transistors.
		 * @return true if this function describes an n-Type transtor.
		 */
		public boolean isNTypeTransistor()
		{
			if (this == TRANMOS  || this == TRA4NMOS ||
				this == TRADMOS  || this == TRA4DMOS ||
				this == TRANMOSNT || this == TRA4NMOSNT ||
				this == TRANMOSFG || this == TRA4NMOSFG ||
				this == TRANMOSCN || this == TRA4NMOSCN ||
				this == TRANMOSVTL || this == TRA4NMOSVTL ||
				this == TRANMOSVTH || this == TRA4NMOSVTH ||
				this == TRANMOSHV1 || this == TRA4NMOSHV1 ||
				this == TRANMOSHV2 || this == TRA4NMOSHV2 ||
				this == TRANMOSHV3 || this == TRA4NMOSHV3 ||
				this == TRANMOSNTHV1 || this == TRA4NMOSNTHV1 ||
				this == TRANMOSNTHV2 || this == TRA4NMOSNTHV2 ||
				this == TRANMOSNTHV3 || this == TRA4NMOSNTHV3 ||
				this == TRADMES  || this == TRA4DMES ||
				this == TRAEMES  || this == TRA4EMES)
					return true;
			return false;
		}

		/**
		 * Method to tell whether this function describes a p-Type transtor.
		 * @return true if this function describes a p-Type transtor.
		 */
		public boolean isPTypeTransistor()
		{
			if (this == TRAPMOS  || this == TRA4PMOS ||
				this == TRAPMOSD || this == TRA4PMOSD ||
				this == TRAPMOSNT || this == TRA4PMOSNT ||
				this == TRAPMOSFG || this == TRA4PMOSFG ||
				this == TRAPMOSCN || this == TRA4PMOSCN ||
				this == TRAPMOSVTL || this == TRA4PMOSVTL ||
				this == TRAPMOSVTH || this == TRA4PMOSVTH ||
				this == TRAPMOSHV1 || this == TRA4PMOSHV1 ||
				this == TRAPMOSHV2 || this == TRA4PMOSHV2 ||
				this == TRAPMOSHV3 || this == TRA4PMOSHV3 ||
				this == TRAPMOSNTHV1 || this == TRA4PMOSNTHV1 ||
				this == TRAPMOSNTHV2 || this == TRA4PMOSNTHV2 ||
				this == TRAPMOSNTHV3 || this == TRA4PMOSNTHV3)
					return true;
			return false;
		}

		/**
		 * Method to tell whether this function describes a bipolar transistor.
		 * This includes NPN and PNP transistors.
		 * @return true if this function describes a bipolar transtor.
		 */
		public boolean isBipolar()
		{
			return this == TRANPN || this == TRA4NPN || this == TRAPNP || this == TRA4PNP;
		}

		/**
		 * Method to tell whether this function describes a flip-flop.
		 * @return true if this function describes a flip-flop.
		 */
		public boolean isFlipFlop() { return isFlipFlop; }

		/**
		 * Returns a printable version of this Function.
		 * @return a printable version of this Function.
		 */
		public String toString() { return name; }

		/** Returns name of the enumeration constant. For example "TRANMOS".
		 * By convention, the enumeration names encode information that isn't
		 * otherwise available. The transistor "TRANMOS" is the same as "TRA4NMOS"
		 * except that the latter has a substrate connection. RKao
		 * @return name of the enumeration constant
		 */
		public String enumName() {return super.toString();}
	}

	// constants used in the "specialType" field
	/** Defines a normal node. */							public static final int NORMAL    = 0;
	/** Defines a serpentine transistor. */					public static final int SERPTRANS = 1;
	/** Defines a polygonal transistor. */					public static final int POLYGONAL = 2;
	/** set if node is a low vt transistor */               public static final int LOWVTBIT =   010;
    /** set if node is a high vt transistor */              public static final int HIGHVTBIT =  020;
    /** set if node is a native transistor */               public static final int NATIVEBIT =  040;
    /** set if node is a od18 transistor */                 public static final int OD18BIT =   0100;
    /** set if node is a od25 transistor */                 public static final int OD25BIT =   0200;
    /** set if node is a od33 transistor */                 public static final int OD33BIT =   0400;
    /** set if node is a cross contact */				    public static final int CROSSCONTACT =     010000000;
    /** set if node is an aligned contact */				public static final int ALIGNCONTACT =     020000000;

	/** set if nonmanhattan instances shrink */				private static final int NODESHRINK =              01;
	/** set if instances can be wiped */					private static final int ARCSWIPE =             01000;
	/** set if node is to be kept square in size */			private static final int NSQUARE =              02000;
	/** primitive can hold trace information */				private static final int HOLDSTRACE =           04000;
	/** set if this primitive can be zero-sized */			private static final int CANBEZEROSIZE =       010000;
	/** set to erase if connected to 1 or 2 arcs */			private static final int WIPEON1OR2 =          020000;
	/** set if primitive is lockable (cannot move) */		private static final int LOCKEDPRIM =          040000;
	/** set if primitive is selectable by edge, not area */	private static final int NEDGESELECT =        0100000;
	/** set if nonmanhattan arcs on this shrink */			private static final int ARCSHRINK =          0200000;
//	/** set if nonmanhattan arcs on this shrink */			private static final int NINVISIBLE =         0400000;
	/** set if node will be considered in palette */        private static final int SKIPSIZEINPALETTE = 01000000;
	/** set if not used (don't put in menu) */				private static final int NNOTUSED =          02000000;

    // --------------------- private data -----------------------------------

	/** The Id of this PrimitiveNode. */			private final PrimitiveNodeId protoId;
	/** This PrimitiveNode's Technology. */			private final Technology tech;
    /** The group containing this PrimitiveNode */  PrimitiveNodeGroup group;
	/** The function of this PrimitiveNode. */		private Function function;
	/** layers describing this primitive */			private Technology.NodeLayer [] layers;
	/** electrical layers describing this */		private Technology.NodeLayer [] electricalLayers;
	/** PrimitivePorts on the PrimitiveNode. */		private PrimitivePort[] primPorts = {};
    /** array of ports by portId.chronIndex */      private PrimitivePort[] portsByChronIndex = {};
	/** flag bits */								private int userBits;
    /** Index of this PrimitiveNode per tech */     private int techPrimNodeIndex = -1;
	/** special type of unusual primitives */		private int specialType;
	/** special factors for unusual primitives */	private double[] specialValues;
    /** true if contains MULTICUTBOX layers */      private int numMultiCuts;
    /** minimum width and height rule */            private NodeSizeRule minNodeSize;
    /** size corrector */                           private final EPoint[] sizeCorrectors = { EPoint.ORIGIN, EPoint.ORIGIN };
	/** offset from database to user */				private SizeOffset offset;
    /** base (highlight) rectangle of standard node */private ERectangle baseRectangle;
    /** full (true) rectangle of standard node */   private ERectangle fullRectangle;
	/** amount to automatically grow to fit arcs */	private Dimension2D autoGrowth;
	/** template for Spice decks (null if none) */	private String spiceTemplate;
    /** factory default instance */                 private ImmutableNodeInst factoryDefaultInst;

	// ------------------ private and protected methods ----------------------

	/**
	 * The constructor is never called externally.  Use the factory "newInstance" instead.
	 */
	protected PrimitiveNode(String protoName, Technology tech, EPoint sizeCorrector1, EPoint sizeCorrector2, String minSizeRule,
            double defWidth, double defHeight, ERectangle fullRectangle, ERectangle baseRectangle, Technology.NodeLayer [] layers)
	{
		// things in the base class
		if (!Technology.jelibSafeName(protoName))
			System.out.println("PrimitiveNode name " + protoName + " is not safe to write in the JELIB");
		this.protoId = tech.getId().newPrimitiveNodeId(protoName);
		this.function = Function.UNKNOWN;

		// things in this class
		this.tech = tech;
		this.layers = layers;
		this.electricalLayers = null;
		this.userBits = 0;
		specialType = NORMAL;
        sizeCorrectors[0] = sizeCorrector1;
        sizeCorrectors[1] = sizeCorrector2;
        this.fullRectangle = fullRectangle;
        this.baseRectangle = baseRectangle;

        if (minSizeRule != null) {
            minNodeSize = new NodeSizeRule(minSizeRule);
        }

        double factoryExtendX = DBMath.round(0.5*(defWidth - fullRectangle.getLambdaWidth()));
        double factoryExtendY = DBMath.round(0.5*(defHeight - fullRectangle.getLambdaHeight()));
        factoryDefaultInst = ImmutableNodeInst.newInstance(0, protoId, function.getBasename(), null, Orientation.IDENT,
                EPoint.ORIGIN, EPoint.fromLambda(factoryExtendX*2, factoryExtendY*2), 0, 0, null);

        double lx = baseRectangle.getLambdaMinX() - fullRectangle.getLambdaMinX();
        double hx = fullRectangle.getLambdaMaxX() - baseRectangle.getLambdaMaxX();
        double ly = baseRectangle.getLambdaMinY() - fullRectangle.getLambdaMinY();
        double hy = fullRectangle.getLambdaMaxY() - baseRectangle.getLambdaMaxY();
        offset = new SizeOffset(lx, hx, ly, hy);
		this.autoGrowth = null;

        int numMultiCuts = 0;
        for (Technology.NodeLayer nodeLayer: layers) {
            if (nodeLayer.getRepresentation() == Technology.NodeLayer.MULTICUTBOX)
                numMultiCuts++;
        }
        this.numMultiCuts = numMultiCuts;

		// add to the nodes in this technology
		tech.addNodeProto(this);
        check();
	}

    protected Object writeReplace() { return new PrimitiveNodeKey(this); }

    private static class PrimitiveNodeKey extends EObjectInputStream.Key<PrimitiveNode> {
        public PrimitiveNodeKey() {}
        private PrimitiveNodeKey(PrimitiveNode pn) { super(pn); }

        @Override
        public void writeExternal(EObjectOutputStream out, PrimitiveNode pn) throws IOException {
            out.writeObject(pn.getTechnology());
            out.writeInt(pn.getId().chronIndex);
        }

        @Override
        public PrimitiveNode readExternal(EObjectInputStream in) throws IOException, ClassNotFoundException {
            Technology tech = (Technology)in.readObject();
            int chronIndex = in.readInt();
            PrimitiveNode pn = tech.getPrimitiveNodeByChronIndex(chronIndex);
            if (pn == null)
                throw new InvalidObjectException("primitive node not found");
            return pn;
        }
    }

	// ------------------------- public methods -------------------------------

	/**
	 * Method to create a new PrimitiveNode from the parameters.
     * Size corrector of PrimitiveNode is determined from width and height.
	 * @param protoName the name of the PrimitiveNode.
	 * Primitive names may not contain unprintable characters, spaces, tabs, a colon (:), semicolon (;) or curly braces ({}).
	 * @param tech the Technology of the PrimitiveNode.
	 * @param width the width of the PrimitiveNode.
	 * @param height the height of the PrimitiveNode.
	 * @param offset the offset from the edges of the reported/selected part of the PrimitiveNode.
	 * @param layers the Layers that comprise the PrimitiveNode.
	 * @return the newly created PrimitiveNode.
	 */
	public static PrimitiveNode newInstance(String protoName, Technology tech, double width, double height,
		SizeOffset offset, Technology.NodeLayer [] layers)
	{
        EPoint sizeCorrector = EPoint.fromLambda(0.5*width, 0.5*height);
		if (offset == null) offset = new SizeOffset(0,0,0,0);
        long lx = -sizeCorrector.getGridX() + offset.getLowXGridOffset();
        long hx = sizeCorrector.getGridX() - offset.getHighXGridOffset();
        long ly = -sizeCorrector.getGridY() + offset.getLowYGridOffset();
        long hy = sizeCorrector.getGridY() - offset.getHighYGridOffset();
        ERectangle fullRectangle = ERectangle.fromGrid(-sizeCorrector.getGridX(), -sizeCorrector.getGridY(),
                2*sizeCorrector.getGridX(), 2*sizeCorrector.getGridY());
        ERectangle baseRectangle = ERectangle.fromGrid(lx, ly, hx - lx, hy - ly);
        EPoint sizeCorrector2 = EPoint.fromGrid(baseRectangle.getGridWidth() >> 1, baseRectangle.getGridHeight() >> 1);
        EPoint fixupCorrector = EPoint.fromGrid(fullRectangle.getGridWidth(), fullRectangle.getGridHeight());
        return newInstance(protoName, tech, sizeCorrector, sizeCorrector2, fixupCorrector, null,
                width, height, fullRectangle, baseRectangle, layers);
	}

	/**
	 * Method to create a new PrimitiveNode from the parameters.
     * PrimitiveNode has zero size corrector.
	 * @param protoName the name of the PrimitiveNode.
	 * Primitive names may not contain unprintable characters, spaces, tabs, a colon (:), semicolon (;) or curly braces ({}).
	 * @param tech the Technology of the PrimitiveNode.
	 * @param width the width of the PrimitiveNode.
	 * @param height the height of the PrimitiveNode.
	 * @param layers the Layers that comprise the PrimitiveNode.
	 * @return the newly created PrimitiveNode.
	 */
	public static PrimitiveNode newInstance0(String protoName, Technology tech, double width, double height, Technology.NodeLayer [] layers)
	{
        return newInstance(protoName, tech, EPoint.ORIGIN, EPoint.ORIGIN, EPoint.ORIGIN, null, width, height, ERectangle.ORIGIN, ERectangle.ORIGIN, layers);
	}

	/**
	 * Method to create a new PrimitiveNode from the parameters.
	 * @param protoName the name of the PrimitiveNode.
	 * Primitive names may not contain unprintable characters, spaces, tabs, a colon (:), semicolon (;) or curly braces ({}).
	 * @param tech the Technology of the PrimitiveNode.
     * @param sizeCorrector size corrector for the PrimitiveNode,
	 * @param width the width of the PrimitiveNode.
	 * @param height the height of the PrimitiveNode.
	 * @param baseRectangle the reported/selected part of the PrimitiveNode with standard size.
	 * @param layers the Layers that comprise the PrimitiveNode.
	 * @return the newly created PrimitiveNode.
	 */
	static PrimitiveNode newInstance(String protoName, Technology tech, EPoint sizeCorrector1, EPoint sizeCorrector2, EPoint fixupCorrector,
        String minSizeRule, double width, double height,
        ERectangle fullRectangle, ERectangle baseRectangle, Technology.NodeLayer [] layers)
	{
		// check the arguments
		if (tech.findNodeProto(protoName) != null)
		{
			System.out.println("Error: technology " + tech.getTechName() + " has multiple nodes named " + protoName);
			return null;
		}
		if (width < 0.0 || height < 0.0)
		{
			System.out.println("Error: technology " + tech.getTechName() + " node " + protoName + " has negative size");
			return null;
		}

        for (Technology.NodeLayer nodeLayer: layers)
            nodeLayer.fixup(fixupCorrector);
		PrimitiveNode pn = new PrimitiveNode(protoName, tech, sizeCorrector1, sizeCorrector2, minSizeRule, width, height,
                fullRectangle, baseRectangle, layers);
		return pn;
	}

    static PrimitiveNode makeArcPin(ArcProto ap, String pinName, String portName,
            double elibSize0, double elibSize1, ArcProto ... extraArcs) {
        Technology tech = ap.getTechnology();
        long fullExtend = ap.getMaxLayerGridExtend();
        long baseExtend = ap.getGridBaseExtend();
        Technology.NodeLayer[] nodeLayers = new Technology.NodeLayer[ap.getNumArcLayers()];
        for (int i = 0; i < ap.getNumArcLayers(); i++) {
            Layer layer = ap.getLayer(i);
            if (layer.getPseudoLayer() == null)
                layer.makePseudo();
            layer = layer.getPseudoLayer();
            double indent = DBMath.gridToLambda(-ap.getLayerGridExtend(i));
            nodeLayers[i] = new Technology.NodeLayer(layer, 0, Poly.Type.CROSSED,
                    Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(indent));
        }

        EPoint sizeCorrector1 = EPoint.fromLambda(elibSize0, elibSize0);
        EPoint sizeCorrector2 = EPoint.fromLambda(elibSize1, elibSize1);
        ERectangle fullRectangle = ERectangle.fromGrid(-fullExtend, -fullExtend, 2*fullExtend, 2*fullExtend);
        ERectangle baseRectangle = ERectangle.fromGrid(-baseExtend, -baseExtend, 2*baseExtend, 2*baseExtend);
        PrimitiveNode arcPin = newInstance(pinName, tech, sizeCorrector1, sizeCorrector2, EPoint.ORIGIN, null,
                0, 0, fullRectangle, baseRectangle, nodeLayers);

        ArcProto[] connections = new ArcProto[1 + extraArcs.length];
        connections[0] = ap;
        System.arraycopy(extraArcs, 0, connections, 1, extraArcs.length);
        arcPin.addPrimitivePorts(new PrimitivePort [] {
            PrimitivePort.newInstance(tech, arcPin, connections, portName,
                    0,180, 0, PortCharacteristic.UNKNOWN,
                    EdgeH.fromLeft(0), EdgeV.fromBottom(0), EdgeH.fromRight(0), EdgeV.fromTop(0))
        }, false);
        arcPin.setFunction(PrimitiveNode.Function.PIN);
        arcPin.setArcsWipe();
        arcPin.setArcsShrink();
        if (ap.isSkipSizeInPalette() || ap.isSpecialArc())
            arcPin.setSkipSizeInPalette();
        return arcPin;
    }

    /** Method to return NodeProtoId of this NodeProto.
     * NodeProtoId identifies NodeProto independently of threads.
     * PrimitiveNodes are shared among threads, so this method returns this PrimitiveNode.
     * @return NodeProtoId of this NodeProto.
     */
    public PrimitiveNodeId getId() { return protoId; }

	/**
	 * Method to return the name of this PrimitiveNode in the Technology.
	 * @return the name of this PrimitiveNode.
	 */
	public String getName() { return protoId.name; }

	/**
	 * Method to return the full name of this PrimitiveNode.
	 * Full name has format "techName:primName"
	 * @return the full name of this PrimitiveNode.
	 */
	public String getFullName() { return protoId.fullName; }

	/**
	 * Method to set the function of this PrimitiveNode.
	 * The Function is a technology-independent description of the behavior of this PrimitiveNode.
	 * @param function the new function of this PrimitiveNode.
	 */
	public void setFunction(Function function) { checkChanging(); this.function = function; }

	/**
	 * Method to return the function of this PrimitiveNode.
	 * The Function is a technology-independent description of the behavior of this PrimitiveNode.
	 * @return the function of this PrimitiveNode.
	 */
	public Function getFunction() { return function; }

	/**
	 * Method to return the function of this PrimitiveNode, grouped according to its
	 * general function.
	 * For example, all transistors return the same value.
	 * @return the group function of this PrimitiveNode.
	 */
	public Function getGroupFunction()
	{
		if (function.isTransistor)
				return Function.TRANS;
		if (function.isResistor() || function.isCapacitor() ||
            function == Function.DIODE || function == Function.DIODEZ || function == Function.INDUCT)
				return Function.INDUCT;
		if (function == Function.CCVS || function == Function.CCCS ||
			function == Function.VCVS || function == Function.VCCS ||
			function == Function.TLINE)
				return Function.TLINE;
		if (function == Function.BASE || function == Function.EMIT ||
			function == Function.COLLECT)
				return Function.COLLECT;
		if (function == Function.BUFFER || function == Function.GATEAND ||
			function == Function.GATEOR || function == Function.MUX ||
			function == Function.GATEXOR)
				return Function.GATEXOR;
		if (function == Function.CONPOWER || function == Function.CONGROUND)
			return Function.CONGROUND;
		if (function == Function.METER || function == Function.SOURCE)
			return Function.SOURCE;
		if (function == Function.SUBSTRATE || function == Function.WELL)
			return Function.CONTACT;
		return function;
	}

	/**
	 * Method to tell whether this primitive node prototype has technology-specific information on it.
	 * At the current time, only certain Schematics primitives have this information.
	 * @return true this primitive node prototype has technology-specific information on it.
	 */
	public boolean isTechSpecific()
	{
		if (this == Schematics.tech().transistorNode || this == Schematics.tech().transistor4Node ||
			this == Schematics.tech().flipflopNode || this == Schematics.tech().diodeNode ||
			this == Schematics.tech().inductorNode || this == Schematics.tech().resistorNode ||
			this == Schematics.tech().capacitorNode) return true;
		return false;
	}

	/**
	 * Method to return the list of NodeLayers that comprise this PrimitiveNode.
	 * @return the list of Layers that comprise this PrimitiveNode.
	 */
	public Technology.NodeLayer [] getNodeLayers() { return layers; }

    /**
     * Method to reset the list of Layers that comprise this PrimitiveNode.
     * @param layers
     */
//    public void setLayers(Technology.NodeLayer [] layers)
//    {
//    	this.layers = layers;
//    }

	/**
	 * Method to return an iterator over the layers in this PrimitiveNode.
	 * @return an iterator over the layers in this PrimitiveNode.
	 */
	public Iterator<Layer> getLayerIterator()
	{
		return new NodeLayerIterator(layers);
	}

	/**
	 * Iterator for Layers on this NodeProto
	 */
	private static class NodeLayerIterator implements Iterator<Layer>
	{
		Technology.NodeLayer [] array;
		int pos;

		public NodeLayerIterator(Technology.NodeLayer [] a)
		{
			array = a;
			pos = 0;
		}

		public boolean hasNext()
		{
			return pos < array.length;
		}

		public Layer next() throws NoSuchElementException
		{
			if (pos >= array.length)
				throw new NoSuchElementException();
			return array[pos++].getLayer();
		}

		public void remove() throws UnsupportedOperationException, IllegalStateException
		{
			throw new UnsupportedOperationException();
		}
	}

	/**
	 * Method to return the list of electrical Layers that comprise this PrimitiveNode.
	 * Like the list returned by "getLayers", the results describe this PrimitiveNode,
	 * but each layer is tied to a specific port on the node.
	 * If any piece of geometry covers more than one port,
	 * it must be split for the purposes of an "electrical" description.<BR>
	 * For example, the MOS transistor has 2 layers: Active and Poly.
	 * But it has 3 electrical layers: Active, Active, and Poly.
	 * The active must be split since each half corresponds to a different PrimitivePort on the PrimitiveNode.
	 * @return the list of electrical Layers that comprise this PrimitiveNode.
	 */
	public Technology.NodeLayer [] getElectricalLayers() { return electricalLayers; }

	/**
	 * Method to set the list of electrical Layers that comprise this PrimitiveNode.
	 * Like the list returned by "getLayers", the results describe this PrimitiveNode,
	 * but each layer is tied to a specific port on the node.
	 * If any piece of geometry covers more than one port,
	 * it must be split for the purposes of an "electrical" description.<BR>
	 * For example, the MOS transistor has 2 layers: Active and Poly.
	 * But it has 3 electrical layers: Active, Active, and Poly.
	 * The active must be split since each half corresponds to a different PrimitivePort on the PrimitiveNode.
	 * @param electricalLayers the list of electrical Layers that comprise this PrimitiveNode.
	 */
	public void setElectricalLayers(Technology.NodeLayer [] electricalLayers) {
        this.electricalLayers = electricalLayers;
        if (false)
            layers = electricalLayers;
    }

	/**
	 * Method to find the NodeLayer on this PrimitiveNode with a given Layer.
	 * If there are more than 1 with the given Layer, the first is returned.
	 * @param layer the Layer to find.
	 * @return the NodeLayer that has this Layer.
	 */
	public Technology.NodeLayer findNodeLayer(Layer layer, boolean electrical)
	{
        // Give higher priority to electrical layers
        Technology.NodeLayer[] nodes = (electrical) ? electricalLayers : layers;

        if (nodes != null)
        {
            for(int j=0; j<nodes.length; j++)
            {
                Technology.NodeLayer oneLayer = nodes[j];
                if (oneLayer.getLayer() == layer) return oneLayer;
            }
        }
		return null;
	}

    /**
     * Tells whether this PrimitiveNode has NodeLayer with MULTICUTBOX representation.
     * For now, multicut primitives and resistor primitives have such NodeLayers.
     * @return true if this PrimitiveNode has NodeLayer with MULTICUTBOX representation.
     */
    public boolean hasMultiCuts() { return numMultiCuts > 0; }

    /**
     * Find a NodeLayer of this PrimitiveNode has NodeLayer with MULTICUTBOX representation.
     * If no such NodeLayer exists, returns null, if many - returns any of them..
     * @return a NodeLayer of this PrimitiveNode has NodeLayer with MULTICUTBOX representation.
     */
    public Technology.NodeLayer findMulticut() {
        for (Technology.NodeLayer nl: layers) {
            if (nl.getRepresentation() == Technology.NodeLayer.MULTICUTBOX)
                return nl;
        }
        return null;
    }

    /**
     * Tells whether this PrimitiveNode is multicut, i.e. it has exactly one NodeLayer with MULTICUTBOX representation,
     * @return true if this PrimitiveNode is multicut.
     */
    public boolean isMulticut() { return numMultiCuts == 1; }

	/**
	 * Method to return the default full width of this PrimitiveNode.
	 * @return the default width of this PrimitiveNode.
	 */
	public double getDefWidth() {
        EditingPreferences ep = EditingPreferences.getThreadEditingPreferences();
        return DBMath.gridToLambda(fullRectangle.getGridWidth() + 2*getDefaultGridExtendX(ep));
    }

	/**
	 * Method to return the default full height of this PrimitiveNode.
	 * @return the default height of this PrimitiveNode.
	 */
	public double getDefHeight() {
        EditingPreferences ep = EditingPreferences.getThreadEditingPreferences();
        return DBMath.gridToLambda(fullRectangle.getGridHeight() + 2*getDefaultGridExtendY(ep));
    }

	/**
	 * Method to return the default base width of this PrimitiveNode in lambda units
     * in specified EditingPreferences.
     * @param ep specified EditingPreferences
	 * @return the default base width of this PrimitiveNode in lambda units.
	 */
	public double getDefaultLambdaBaseWidth(EditingPreferences ep) {
        return DBMath.gridToLambda(getDefaultGridBaseWidth(ep));
    }

	/**
	 * Method to return the factory default base width of this PrimitiveNode in lambda units.
	 * @return the factory default base width of this PrimitiveNode in lambda units.
	 */
	public double getFactoryDefaultLambdaBaseWidth() {
        return DBMath.gridToLambda(getFactoryDefaultGridBaseWidth());
    }

	/**
	 * Method to return the default base hwight of this PrimitiveNode in lambda units
     * in specified EditingPreferences.
     * @param ep specified EditingPreferences
	 * @return the default base height of this PrimitiveNode in lambda units.
	 */
	public double getDefaultLambdaBaseHeight(EditingPreferences ep) {
        return DBMath.gridToLambda(getDefaultGridBaseHeight(ep));
    }

	/**
	 * Method to return the factory default base hwight of this PrimitiveNode in lambda units.
	 * @return the factory default base height of this PrimitiveNode in lambda units.
	 */
	public double getFactoryDefaultLambdaBaseHeight() {
        return DBMath.gridToLambda(getFactoryDefaultGridBaseHeight());
    }


	/**
	 * Method to return the default base width of this PrimitiveNode in grid units
     * in specified EditingPreferences.
     * @param ep specified EditingPreferences
	 * @return the default base width of this PrimitiveNode in grid units.
	 */
	public long getDefaultGridBaseWidth(EditingPreferences ep) {
        return baseRectangle.getGridWidth() + getDefaultInst(ep).size.getGridX();
    }

	/**
	 * Method to return the factory default base width of this PrimitiveNode in grid units.
	 * @return the factory default base width of this PrimitiveNode in grid units.
	 */
	public long getFactoryDefaultGridBaseWidth() {
        return baseRectangle.getGridWidth() + factoryDefaultInst.size.getGridX();
    }


	/**
	 * Method to return the default base height of this PrimitiveNode in grid units
     * in specified EditingPreferences.
     * @param ep specified EditingPreferences
	 * @return the default base height of this PrimitiveNode in grid units.
	 */
	public long getDefaultGridBaseHeight(EditingPreferences ep) {
        return baseRectangle.getGridHeight() + getDefaultInst(ep).size.getGridY();
    }

	/**
	 * Method to return the factory default base height of this PrimitiveNode in grid units.
	 * @return the factory default base height of this PrimitiveNode in grid units.
	 */
	public long getFactoryDefaultGridBaseHeight() {
        return baseRectangle.getGridHeight() + factoryDefaultInst.size.getGridY();
    }

	/**
	 * Method to return the defaut extend of this PrimitiveNode over minimal width\
     * in lambda units in specified EditingPreferences.
     * @param ep specified EditingPreferences
	 * @return the defaut extend of this PrimitiveNode over minimal width in lambda units.
	 */
	public double getDefaultLambdaExtendX(EditingPreferences ep) {
        return getDefaultInst(ep).size.getLambdaX()*0.5;
    }

	/**
	 * Method to return the defaut extend of this PrimitiveNode over minimal height\
     * in lambda units in specified EditingPreferences.
     * @param ep specified EditingPreferences
	 * @return the defaut extend of this PrimitiveNode overn ninimal height in lambda units.
	 */
	public double getDefaultLambdaExtendY(EditingPreferences ep) {
        return getDefaultInst(ep).size.getLambdaY()*0.5;
    }

	/**
	 * Method to return the defaut extend of this PrimitiveNode over minimal width\
     * in grid units in specified EditingPreferences.
     * @param ep specified EditingPreferences
	 * @return the defaut extend of this PrimitiveNode over minimal width in grid units.
	 */
	public long getDefaultGridExtendX(EditingPreferences ep) {
        return getDefaultInst(ep).size.getGridX() >> 1;
    }

	/**
	 * Method to return the defaut extend of this PrimitiveNode over minimal height\
     * in grid units in specified EditingPreferences.
     * @param ep specified EditingPreferences
	 * @return the defaut extend of this PrimitiveNode overn ninimal height in grid units.
	 */
	public long getDefaultGridExtendY(EditingPreferences ep) {
        return getDefaultInst(ep).size.getGridY() >> 1;
    }

	/**
	 * Method to return the factory defaut extend of this PrimitiveNode over minimal width\
     * in grid units.
	 * @return the defaut extend of this PrimitiveNode over minimal width in grid units.
	 */
	public long getFactoryDefaultGridExtendX() {
        return factoryDefaultInst.size.getGridX() >> 1;
    }

	/**
	 * Method to return the factory defaut extend of this PrimitiveNode over minimal height\
     * in grid units.
	 * @return the defaut extend of this PrimitiveNode overn ninimal height in grid units.
	 */
	public long getFactoryDefaultGridExtendY() {
        return factoryDefaultInst.size.getGridY() >> 1;
    }

    /**
     * Method to return the default immutable instance of this PrimitiveNode
     * in specified EditingPreferences.
     * @param ep specified EditingPreferences
     * @return the default immutable instance of this PrimitiveNode
     */
    public ImmutableNodeInst getDefaultInst(EditingPreferences ep) {
        ImmutableNodeInst defaultInst = ep.getDefaultNode(protoId);
        return defaultInst != null ? defaultInst : factoryDefaultInst;
    }

    /**
     * Method to return the factory default immutable instance of this PrimitiveNode
     * @return the factory default immutable instance of this PrimitiveNode
     */
    public ImmutableNodeInst getFactoryDefaultInst() {
        return factoryDefaultInst;
    }

	/**
	 * Method to get the size offset of this PrimitiveNode.
	 * To get the SizeOffset for a specific NodeInst, use Technology.getSizeOffset(ni).
	 * Use this method only to get the SizeOffset of a PrimitiveNode.
	 * @return the size offset of this PrimitiveNode.
	 */
	public SizeOffset getProtoSizeOffset() { return offset; }

	/**
	 * Method to get the base (highlight) ERectangle of this PrimitiveNode.
     * Base ERectangle is a highlight rectangle of standard-size NodeInst of
     * this PrimtiveNode
	 * To get the base ERectangle  for a specific NodeInst, use Technology.getBaseRectangle(ni).
	 * Use this method only to get the base ERectangle of a PrimitiveNode.
	 * @return the base ERectangle of this PrimitiveNode.
	 */
    public ERectangle getBaseRectangle() { return baseRectangle; }

	/**
	 * Method to get the full (true) ERectangle of this PrimitiveNode.
     * Base ERectangle is a highlight rectangle of standard-size NodeInst of
     * this PrimtiveNode
	 * To get the full (true) ERectangle  for a specific NodeInst, use Technology.getBaseRectangle(ni).
	 * Use this method only to get the base ERectangle of a PrimitiveNode.
	 * @return the base ERectangle of this PrimitiveNode.
	 */
    public ERectangle getFullRectangle() { return fullRectangle; }

    EPoint getSizeCorrector(int version) { return sizeCorrectors[version]; }

	/**
	 * Method to return the minimum size rule for this PrimitiveNode.
	 * @return the minimum size rule for this PrimitiveNode.
	 */
	public NodeSizeRule getMinSizeRule() { return minNodeSize; }

	/**
	 * Method to set the minimum height of this PrimitiveNode.
     * If no name is provided, it uses the PrimitiveNode name to compose the rule name.
	 * @param minHeight the minimum height of this PrimitiveNode.
	 */
	public void setMinSize(double minWidth, double minHeight, String minSizeRule)
	{
        setSizeCorrector(minWidth, minHeight);
        // If no name is provided, it uses the PrimitiveNode name to compose the rule name.
        // That is done in the class NodeSizeRule
        if (DBMath.areEquals(minWidth, minHeight))
            minNodeSize = new NodeSizeRule(minSizeRule);
        else
            // Different functions to check node sizes.
            minNodeSize = new AsymmetricNodeSizeRule(minSizeRule);
        check();
    }

    private void setSizeCorrector(double refWidth, double refHeight) {
        long extendX = DBMath.lambdaToGrid(0.5*refWidth);
        long extendY = DBMath.lambdaToGrid(0.5*refHeight);
        sizeCorrectors[0] = EPoint.fromGrid(extendX, extendY);
        fullRectangle = ERectangle.fromGrid(-extendX, -extendY, 2*extendX, 2*extendY);
        long lx = fullRectangle.getGridMinX() + offset.getLowXGridOffset();
        long hx = fullRectangle.getGridMaxX() - offset.getHighXGridOffset();
        long ly = fullRectangle.getGridMinY() + offset.getLowYGridOffset();
        long hy = fullRectangle.getGridMaxY() - offset.getHighYGridOffset();
        baseRectangle = ERectangle.fromGrid(lx, ly, hx - lx, hy - ly);
        sizeCorrectors[1] = EPoint.fromGrid(baseRectangle.getGridWidth() >> 1, baseRectangle.getGridHeight() >> 1);
        check();
    }

	/**
	 * Method to set the size offset of this PrimitiveNode.
	 * @param offset the size offset of this PrimitiveNode.
	 */
	public void setSizeOffset(SizeOffset offset) {
        this.offset = offset;
        long lx = fullRectangle.getGridMinX() + offset.getLowXGridOffset();
        long hx = fullRectangle.getGridMaxX() - offset.getHighXGridOffset();
        long ly = fullRectangle.getGridMinY() + offset.getLowYGridOffset();
        long hy = fullRectangle.getGridMaxY() - offset.getHighYGridOffset();
        baseRectangle = ERectangle.fromGrid(lx, ly, hx - lx, hy - ly);
        sizeCorrectors[1] = EPoint.fromGrid(baseRectangle.getGridWidth() >> 1, baseRectangle.getGridHeight() >> 1);
        check();
    }

	/**
	 * Method to set the auto-growth factor on this PrimitiveNode.
	 * The auto-growth factor is the amount to exand the node when new arcs
	 * want to connect to an expandable port and there is no room for the arcs.
	 * The only nodes that have auto-growth factors are the AND, OR, XOR, SWITCH, and MUX
	 * nodes of the Schematics technology.
	 * These nodes have ports that can accomodate any number of arcs.
	 * @param dX the X amount to grow this PrimitiveNode when arcs don't fit.
	 * @param dY the Y amount to grow this PrimitiveNode when arcs don't fit.
	 */
	public void setAutoGrowth(double dX, double dY) { autoGrowth = new Dimension2D.Double(dX, dY); }

	/**
	 * Method to get the auto-growth factor for this PrimitiveNode.
	 * The auto-growth factor is the amount to exand the node when new arcs
	 * want to connect to an expandable port and there is no room for the arcs.
	 * The only nodes that have auto-growth factors are the AND, OR, XOR, SWITCH, and MUX
	 * nodes of the Schematics technology.
	 * These nodes have ports that can accomodate any number of arcs.
	 * @return the amount to grow this PrimitiveNode when arcs don't fit.
	 */
	public Dimension2D getAutoGrowth() { return autoGrowth; }

	/**
	 * Method to set the Spice template on this PrimitiveNode.
	 * The Spice template is a string that has parameters which are
	 * filled-in to produce the line in the Spice deck corresponding to this PrimitiveNode.
	 * @param st the Spice template on this PrimitiveNode.
	 */
	public void setSpiceTemplate(String st) { spiceTemplate = st; }

	/**
	 * Method to get the Spice template on this PrimitiveNode.
	 * The Spice template is a string that has parameters which are
	 * filled-in to produce the line in the Spice deck corresponding to this PrimitiveNode.
	 * @return the Spice template on this PrimitiveNode.
	 */
	public String getSpiceTemplate() { return spiceTemplate; }

	/**
	 * Method to return the Technology of this PrimitiveNode.
	 * @return the Technology of this PrimitiveNode.
	 */
	public Technology getTechnology() { return tech; }

	/**
	 * Method to return the PrimitiveNodeGroup that contains this PrimitiveNode.
     * Return null if this PrimitiveNode is not contained in any group.
	 * @return the Technology of this PrimitiveNode.
	 */
	public PrimitiveNodeGroup getPrimitiveNodeGroup() { return group; }

	/**
	 * Method to add an array of PrimitivePorts to this PrimitiveNode.
     * fullRectange shift is removed from port sizes.
	 * The method is only used during initialization.
	 * @param ports the array of PrimitivePorts to add.
	 */
	public void addPrimitivePortsFixed(PrimitivePort [] ports) {
        addPrimitivePorts(ports, true);
    }

	/**
	 * Method to add an array of PrimitivePorts to this PrimitiveNode.
	 * The method is only used during initialization.
	 * @param ports the array of PrimitivePorts to add.
     * @param fixFullRectangle remove fullRectangle shift from port sizes.
	 */
	public void addPrimitivePorts(PrimitivePort [] ports, boolean fixFullRectangle)
	{
        assert ports.length == primPorts.length;
        for (int i = 0; i < primPorts.length; i++) {
            assert primPorts[i] == ports[i];
            assert primPorts[i].getPortIndex() == i;
            if (fixFullRectangle)
                ports[i].fixupEdges(fullRectangle.getWidth(), fullRectangle.getHeight());
        }
        if (primPorts.length == 1) {
            PrimitivePort thePort = primPorts[0];
            PrimitivePortId newPortId = thePort.setEmptyId();
            portsByChronIndex = new PrimitivePort[newPortId.chronIndex + 1];
            portsByChronIndex[newPortId.chronIndex] = thePort;
        }
	}

    void addPrimitivePort(PrimitivePort pp) {
        assert pp.getParent() == this;
        assert pp.getPortIndex() == primPorts.length;
        PrimitivePort[] newPrimPorts = new PrimitivePort[primPorts.length + 1];
        System.arraycopy(primPorts, 0, newPrimPorts, 0, primPorts.length);
        newPrimPorts[pp.getPortIndex()] = pp;
        primPorts = newPrimPorts;

        PrimitivePortId primitivePortId = pp.getId();
        if (primitivePortId.chronIndex >= portsByChronIndex.length) {
            PrimitivePort[] newPortsByChronIndex = new PrimitivePort[primitivePortId.chronIndex + 1];
            System.arraycopy(portsByChronIndex, 0, newPortsByChronIndex, 0, portsByChronIndex.length);
            portsByChronIndex = newPortsByChronIndex;
        }
        portsByChronIndex[primitivePortId.chronIndex] = pp;
    }

	/**
	 * Method to find the PortProto that has a particular name.
	 * @return the PortProto, or null if there is no PortProto with that name.
	 */
	public PortProto findPortProto(String name)
	{
        if (name == null) return null;
		return findPortProto(Name.findName(name));
	}

	/**
	 * Method to find the PortProto that has a particular Name.
	 * @return the PortProto, or null if there is no PortProto with that name.
	 */
	public PortProto findPortProto(Name name)
	{
        if (name == null) return null;
		for (int i = 0; i < primPorts.length; i++)
		{
			PrimitivePort pp = primPorts[i];
			if (pp.getNameKey() == name)
				return pp;
		}
		return null;
	}

	/**
	 * Returns the PrimitivePort in this technology with a particular chron index
	 * @param chron index the Id of the PrimitivePort.
	 * @return the PrimitivePort in this technology with that Id.
	 */
	PrimitivePort getPrimitivePortByChronIndex(int chronIndex)
	{
        return chronIndex < portsByChronIndex.length ? portsByChronIndex[chronIndex] : null;
	}

	/**
	 * Method to return an iterator over all PortProtos of this NodeProto.
	 * @return an iterator over all PortProtos of this NodeProto.
	 */
	public Iterator<PortProto> getPorts()
	{
		return ArrayIterator.iterator((PortProto[])primPorts);
	}

	/**
	 * Method to return an iterator over all PrimitivePorts of this PrimitiveNode.
	 * @return an iterator over all PrimitvePorts of this NodeProto.
	 */
	public Iterator<PrimitivePort> getPrimitivePorts()
	{
		return ArrayIterator.iterator(primPorts);
	}

	/**
	 * Method to return the number of PortProtos on this NodeProto.
	 * @return the number of PortProtos on this NodeProto.
	 */
	public int getNumPorts()
	{
		return primPorts.length;
	}

	/**
	 * Method to return the PortProto at specified position.
	 * @param portIndex specified position of PortProto.
	 * @return the PortProto at specified position..
	 */
	public final PrimitivePort getPort(int portIndex)
	{
		return primPorts[portIndex];
	}

	/**
	 * Method to return the PortProto by thread-independent PortProtoId.
	 * @param portProtoId thread-independent PortProtoId.
	 * @return the PortProto.
     * @throws IllegalArgumentException if portProtoId is not from this NodeProto.
	 */
	public PrimitivePort getPort(PortProtoId portProtoId) {
        if (portProtoId.getParentId() != protoId) throw new IllegalArgumentException();
        int chronIndex = portProtoId.chronIndex;
        return chronIndex < portsByChronIndex.length ? portsByChronIndex[chronIndex] : null;
    }

	/**
	 * Method to return the PrimitivePort on this PrimitiveNode that can connect to an arc of the specified type.
	 * The method finds a PrimitivePort that can make the connection.
	 * @param arc the type of arc to connect to an instance of this PrimitiveNode.
	 * @return a PrimitivePort that can connect to this type of ArcProto.
	 * Returns null if this ArcProto cannot connect to anything on this PrimitiveNode.
	 */
	public PrimitivePort connectsTo(ArcProto arc)
	{
		for (int i = 0; i < primPorts.length; i++)
		{
			PrimitivePort pp = primPorts[i];
			if (pp.connectsTo(arc))
				return pp;
		}
		return null;
	}

	/**
	 * Method to return the special type of this PrimitiveNode.
	 * It can be one of NORMAL, SERPTRANS, POLYGONAL, or MULTICUT.
	 * @return the special type of this PrimitiveNode.
	 */
	public int getSpecialType() { return specialType; }

	/**
	 * Method to set the special type of this PrimitiveNode.
	 * @param specialType the newspecial type of this PrimitiveNode.
	 * It can be NORMAL, SERPTRANS, POLYGONAL, or MULTICUT.
	 */
	public void setSpecialType(int specialType) { this.specialType = specialType; }

	/**
	 * Method to return the name of a special PrimitiveNode type.
	 * @param t the integer special type.
	 * @return the name of that type.
	 */
	public static String getSpecialTypeName(int t)
	{
		if (t == NORMAL) return "normal";
		if (t == SERPTRANS) return "serp-trans";
		if (t == POLYGONAL) return "outline";
		return "?";
	}
	/**
	 * Method to return the special values stored on this PrimitiveNode.
	 * The special values are an array of integers that describe unusual features of the PrimitiveNode.
	 * They are only relevant for certain specialType cases:
	 * <UL>
	 * <LI>for MULTICUT:
	 *   <UL>
	 *   <LI>cut size is [0] x [1]
	 *   <LI>cut indented [2] x [3] from highlighting
	 *   <LI>cuts spaced [4] apart for 1-dimensional contact
	 *   <LI>cuts spaced [5] apart for 2-dimensional contact
	 *   </UL>
	 * <LI>for SERPTRANS:
	 *   <UL>
	 *   <LI>layer count is [0] (never used)
	 *   <LI>active port inset [1] from end of serpentine path
	 *   <LI>active port is [2] from poly edge
	 *   <LI>poly width is [3]
	 *   <LI>poly port inset [4] from poly edge
	 *   <LI>poly port is [5] from active edge
	 *   </UL>
	 * @return the special values stored on this PrimitiveNode.
	 */
	public double [] getSpecialValues() { return specialValues; }

    public EPoint getMulticut2Size() {
        Technology.NodeLayer cutLayer = findMulticut();
        assert cutLayer.getLeftEdge().getMultiplier() == -0.5;
        assert cutLayer.getBottomEdge().getMultiplier() == -0.5;
        assert cutLayer.getRightEdge().getMultiplier() == 0.5;
        assert cutLayer.getTopEdge().getMultiplier() == 0.5;
        double x = cutLayer.getMulticutSizeX() + cutLayer.getMulticutSep2D() + cutLayer.getLeftEdge().getAdder() - cutLayer.getRightEdge().getAdder();
        double y = cutLayer.getMulticutSizeY() + cutLayer.getMulticutSep2D() + cutLayer.getBottomEdge().getAdder() - cutLayer.getTopEdge().getAdder();
        x += fullRectangle.getLambdaWidth();
        y += fullRectangle.getLambdaHeight();
        return EPoint.fromLambda(x, y);
    }

	/**
	 * Method to set the special values stored on this PrimitiveNode.
	 * The special values are an array of values that describe unusual features of the PrimitiveNode.
	 * The meaning depends on the specialType (see the documentation for "getSpecialValues").
	 * @param specialValues the special values for this PrimitiveNode.
	 */
	public void setSpecialValues(double [] specialValues)
	{
		if (specialValues.length != 6)
			throw new IndexOutOfBoundsException("Invalid number of values in setSpecialValues");
		this.specialValues = specialValues;
	}

	/**
	 * Method to tell whether this PrimitiveNode is a Pin.
	 * Pin nodes have one port, no valid geometry, and are used to connect arcs.
	 * @return true if this PrimitiveNode is a Pin.
	 */
	public boolean isPin()
	{
		return (getFunction().isPin());
	}

	/**
	 * Method to describe this PrimitiveNode as a string.
	 * If the primitive is not from the current technology, prepend the technology name.
     * @param withQuotes to wrap description between quotes
	 * @return a description of this PrimitiveNode.
	 */
	public String describe(boolean withQuotes)
	{
		String name = "";
		if (tech != Technology.getCurrent())
			name += tech.getTechName() + ":";
		name += getName();
        return (withQuotes) ? "'"+name+"'" : name;
	}

    /**
     * Method to determine if node has a given bit on. This is usefull for different
     * @param bit bit containing information to query. It could be LOWVTTRANS,
     * HIGHVTTRANS, NATIVETRANS, OD18TRANS, OD25TRANS or OD33TRANS in case of transistors.
     * @return true if the given bit is on in the node.
     */
    public boolean isNodeBitOn(int bit)
    {
        assert (bit == LOWVTBIT ||
            bit == HIGHVTBIT ||
            bit == NATIVEBIT ||
            bit == OD18BIT ||
            bit == OD25BIT ||
            bit == OD33BIT);

        return (userBits & bit) != 0;
    }

    /**
     * Method to set certain bit during construction
     * @param bit
     */
    public void setNodeBit(int bit) { checkChanging(); userBits |= bit; }

	/**
	 * Method to allow instances of this PrimitiveNode not to be considered in
     * tech palette for the calculation of the largest icon.
	 * Valid for menu display
	 */
	public void setSkipSizeInPalette() { checkChanging(); userBits |= SKIPSIZEINPALETTE; }

	/**
	 * Method to tell if instaces of this PrimitiveNode are special (don't appear in menu).
	 * Valid for menu display
	 */
	public boolean isSkipSizeInPalette() { return (userBits & SKIPSIZEINPALETTE) != 0; }

	/**
	 * Method to allow instances of this PrimitiveNode to shrink.
	 * Shrinkage occurs on MOS transistors when they are connected to wires at angles that are not manhattan
	 * (the angle between the transistor and the wire is not a multiple of 90 degrees).
	 * The actual transistor must be shrunk back appropriately to prevent little tabs from emerging at the connection site.
	 * This state is only set on primitive node prototypes.
	 * If the actual NodeInst is to shrink, it must be marked with "setShortened".
	 * Note that shrinkage does not apply if there is no arc connected.
	 */
	public void setCanShrink() { checkChanging(); userBits |= NODESHRINK; }

	/**
	 * Method to prevent instances of this PrimitiveNode from shrinking.
	 * Shrinkage occurs on MOS transistors when they are connected to wires at angles that are not manhattan
	 * (the angle between the transistor and the wire is not a multiple of 90 degrees).
	 * The actual transistor must be shrunk back appropriately to prevent little tabs from emerging at the connection site.
	 * This state is only set on primitive node prototypes.
	 * If the actual NodeInst is to shrink, it must be marked with "setShortened".
	 * Note that shrinkage does not apply if there is no arc connected.
	 */
	public void clearCanShrink() { checkChanging(); userBits &= ~NODESHRINK; }

	/**
	 * Method to tell if instances of this PrimitiveNode can shrink.
	 * Shrinkage occurs on MOS transistors when they are connected to wires at angles that are not manhattan
	 * (the angle between the transistor and the wire is not a multiple of 90 degrees).
	 * The actual transistor must be shrunk back appropriately to prevent little tabs from emerging at the connection site.
	 * This state is only set on primitive node prototypes.
	 * If the actual NodeInst is to shrink, it must be marked with "setShortened".
	 * Note that shrinkage does not apply if there is no arc connected.
	 * @return true if instances of this PrimitiveNode can shrink.
	 */
	public boolean canShrink() { return (userBits & NODESHRINK) != 0; }

	/**
	 * Method to set this PrimitiveNode so that instances of it are "arc-wipable".
	 * For display efficiency reasons, pins that have arcs connected to them should not bother being drawn.
	 * Therefore, pin prototypes have this state set, and when instances of the
	 * appropriate arc prototypes connect to instances of these pins, they stop being drawn.
	 * It is necessary for the arc prototype to enable wiping (with setWipable).
	 * A NodeInst that becomes wiped out has "setWiped" called.
	 */
	public void setArcsWipe() { checkChanging(); userBits |= ARCSWIPE; }

	/**
	 * Method to set this PrimitiveNode so that instances of it are not "arc-wipable".
	 * For display efficiency reasons, pins that have arcs connected to them should not bother being drawn.
	 * Therefore, pin prototypes have this state set, and when instances of the
	 * appropriate arc prototypes connect to instances of these pins, they stop being drawn.
	 * It is necessary for the arc prototype to enable wiping (with setWipable).
	 * A NodeInst that becomes wiped out has "setWiped" called.
	 */
	public void clearArcsWipe() { checkChanging(); userBits &= ~ARCSWIPE; }

	/**
	 * Method to tell if instances of this PrimitiveNode are "arc-wipable" by when created.
	 * For display efficiency reasons, pins that have arcs connected to them should not bother being drawn.
	 * Therefore, pin prototypes have this state set, and when instances of the
	 * appropriate arc prototypes connect to instances of these pins, they stop being drawn.
	 * It is necessary for the arc prototype to enable wiping (with setWipable).
	 * A NodeInst that becomes wiped out has "setWiped" called.
	 * @return true if instances of this PrimitiveNode are "arc-wipable" by when created.
	 */
	public boolean isArcsWipe() { return (userBits & ARCSWIPE) != 0; }

	/**
	 * Method to set this PrimitiveNode so that instances of it are "square".
	 * Square nodes must have the same X and Y size.
	 * This is useful for round components that really have only one dimension.
	 */
	public void setSquare() { checkChanging(); userBits |= NSQUARE; }

	/**
	 * Method to set this PrimitiveNode so that instances of it are not "square".
	 * Square nodes must have the same X and Y size.
	 * This is useful for round components that really have only one dimension.
	 */
	public void clearSquare() { checkChanging(); userBits &= ~NSQUARE; }

	/**
	 * Method to tell if instances of this PrimitiveNode are square.
	 * Square nodes must have the same X and Y size.
	 * This is useful for round components that really have only one dimension.
	 * @return true if instances of this PrimitiveNode are square.
	 */
	public boolean isSquare() { return (userBits & NSQUARE) != 0; }

	/**
	 * Method to set this PrimitiveNode so that instances of it may hold outline information.
	 * Outline information is an array of coordinates that define the node.
	 * It can be as simple as an opened-polygon that connects the points,
	 * or a serpentine transistor that lays down polysilicon to follow the points.
	 */
	public void setHoldsOutline() { checkChanging(); userBits |= HOLDSTRACE; }

	/**
	 * Method to set this PrimitiveNode so that instances of it may not hold outline information.
	 * Outline information is an array of coordinates that define the node.
	 * It can be as simple as an opened-polygon that connects the points,
	 * or a serpentine transistor that lays down polysilicon to follow the points.
	 */
	public void clearHoldsOutline() { checkChanging(); userBits &= ~HOLDSTRACE; }

	/**
	 * Method to tell if instances of this PrimitiveNode can hold an outline.
	 * Outline information is an array of coordinates that define the node.
	 * It can be as simple as an opened-polygon that connects the points,
	 * or a serpentine transistor that lays down polysilicon to follow the points.
	 * @return true if instances of this PrimitiveNode can hold an outline.
	 */
	public boolean isHoldsOutline() { return (userBits & HOLDSTRACE) != 0; }

	/**
	 * Method to set this PrimitiveNode so that it can be zero in size.
	 * The display system uses this to eliminate zero-size nodes that cannot be that way.
	 */
	public void setCanBeZeroSize() { checkChanging(); userBits |= CANBEZEROSIZE; }

	/**
	 * Method to set this PrimitiveNode so that it cannot be zero in size.
	 * The display system uses this to eliminate zero-size nodes that cannot be that way.
	 */
	public void clearCanBeZeroSize() { checkChanging(); userBits &= ~CANBEZEROSIZE; }

	/**
	 * Method to tell if instances of this PrimitiveNode can be zero in size.
	 * The display system uses this to eliminate zero-size nodes that cannot be that way.
	 * @return true if instances of this PrimitiveNode can be zero in size.
	 */
	public boolean isCanBeZeroSize() { return (userBits & CANBEZEROSIZE) != 0; }

	/**
	 * Method to set this PrimitiveNode so that instances of it are wiped when 1 or 2 arcs connect.
	 * This is used in Schematics pins, which are not shown if 1 or 2 arcs connect, but are shown
	 * when standing alone, or when 3 or more arcs make a "T" or other connection to it.
	 */
	public void setWipeOn1or2() { checkChanging(); userBits |= WIPEON1OR2; }

	/**
	 * Method to set this PrimitiveNode so that instances of it are not wiped when 1 or 2 arcs connect.
	 * Only Schematics pins enable this state.
	 */
	public void clearWipeOn1or2() { checkChanging(); userBits &= ~WIPEON1OR2; }

	/**
	 * Method to tell if instances of this PrimitiveNode are wiped when 1 or 2 arcs connect.
	 * This is used in Schematics pins, which are not shown if 1 or 2 arcs connect, but are shown
	 * when standing alone, or when 3 or more arcs make a "T" or other connection to it.
	 * @return true if instances of this PrimitiveNode are wiped when 1 or 2 arcs connect.
	 */
	public boolean isWipeOn1or2() { return (userBits & WIPEON1OR2) != 0; }

	/**
	 * Method to set this PrimitiveNode so that instances of it are locked.
	 * Locked Primitives cannot be created, deleted, or modified.
	 * Typically, array technologies (such as FPGA) have lockable primitives which are used for the fixed part of a design,
	 * and then locked to prevent the customization work from damaging the circuit.
	 */
	public void setLockedPrim() { checkChanging(); userBits |= LOCKEDPRIM; }

	/**
	 * Method to set this PrimitiveNode so that instances of it are not locked.
	 * Locked Primitives cannot be created, deleted, or modified.
	 * Typically, array technologies (such as FPGA) have lockable primitives which are used for the fixed part of a design,
	 * and then locked to prevent the customization work from damaging the circuit.
	 */
	public void clearLockedPrim() { checkChanging(); userBits &= ~LOCKEDPRIM; }

	/**
	 * Method to tell if instances of this PrimitiveNode are loced.
	 * Locked Primitives cannot be created, deleted, or modified.
	 * Typically, array technologies (such as FPGA) have lockable primitives which are used for the fixed part of a design,
	 * and then locked to prevent the customization work from damaging the circuit.
	 * @return true if instances of this PrimitiveNode are loced.
	 */
	public boolean isLockedPrim() { return (userBits & LOCKEDPRIM) != 0; }

	/**
	 * Method to set this PrimitiveNode so that instances of it are selectable only by their edges.
	 * Artwork primitives that are not filled-in or are outlines want edge-selection, instead
	 * of allowing a click anywhere in the bounding box to work.
	 */
	public void setEdgeSelect() { checkChanging(); userBits |= NEDGESELECT; }

	/**
	 * Method to set this PrimitiveNode so that instances of it are not selectable only by their edges.
	 * Artwork primitives that are not filled-in or are outlines want edge-selection, instead
	 * of allowing a click anywhere in the bounding box to work.
	 */
	public void clearEdgeSelect() { checkChanging(); userBits &= ~NEDGESELECT; }

	/**
	 * Method to tell if instances of this PrimitiveNode are selectable on their edges.
	 * Artwork primitives that are not filled-in or are outlines want edge-selection, instead
	 * of allowing a click anywhere in the bounding box to work.
	 * @return true if instances of this PrimitiveNode are selectable on their edges.
	 */
	public boolean isEdgeSelect() { return (userBits & NEDGESELECT) != 0; }

	/**
	 * Method to set this PrimitiveNode so that arcs connected to instances will shrink in nonmanhattan situations.
	 * This happens to pins where any combination of multiple arcs in angles that are not increments of 90 degrees
	 * will cause tabs to emerge at the connection site.
	 */
	public void setArcsShrink() { checkChanging(); userBits |= ARCSHRINK; }

	/**
	 * Method to set this PrimitiveNode so that arcs connected to instances will not shrink in nonmanhattan situations.
	 * This happens to pins where any combination of multiple arcs in angles that are not increments of 90 degrees
	 * will cause tabs to emerge at the connection site.
	 */
	public void clearArcsShrink() { checkChanging(); userBits &= ~ARCSHRINK; }

	/**
	 * Method to tell if instances of this PrimitiveNode cause arcs to shrink in nonmanhattan situations.
	 * This happens to pins where any combination of multiple arcs in angles that are not increments of 90 degrees
	 * will cause tabs to emerge at the connection site.
	 * @return true if instances of this PrimitiveNode cause arcs to shrink in nonmanhattan situations.
	 */
	public boolean isArcsShrink() { return (userBits & ARCSHRINK) != 0; }

	/**
	 * Method to set this PrimitiveNode so that it is not used.
	 * Unused nodes do not appear in the component menus and cannot be created by the user.
	 * The state is useful for hiding primitives that the user should not use.
     * @param set
     */
	public void setNotUsed(boolean set)
    {
        checkChanging();
        if (set)
            userBits |= NNOTUSED;
        else
            userBits &= ~NNOTUSED; // clear

    }

	/**
	 * Method to tell if this PrimitiveNode is used.
	 * Unused nodes do not appear in the component menus and cannot be created by the user.
	 * The state is useful for hiding primitives that the user should not use.
	 * @return true if this PrimitiveNode is used.
	 */
	public boolean isNotUsed() { return (userBits & NNOTUSED) != 0; }

    /**
     * Method to determine if PrimitiveNode represents a well node
     * @return true if this PrimitiveNode is a well node
     */
	public boolean isPureWellNode()
	{
	    // Not even looking at
		if (function != PrimitiveNode.Function.NODE) return false;
	    // only one layer
	    if (layers.length != 1) return false;
	    Layer layer = layers[0].getLayer();
	    return (layer.getFunction().isWell());
	}
    /**
     * Method to determine if PrimitiveNode represents substrate node
     * @return true if this PrimitiveNode is a substrate node
     */
	public boolean isPureSubstrateNode()
	{
	    // Not even looking at
		if (function != PrimitiveNode.Function.NODE) return false;
	    // only one layer
	    if (layers.length != 1) return false;
	    Layer layer = layers[0].getLayer();
	    return (layer.getFunction().isSubstrate());
	}

    /**
     * Method to retrieve index of the node in the given technology.
     * It must add the total number of layers to guarantee indexes don't collide with
     * layer indices. This function MUST be in sync with
     * The sequence of indices is: rules for single layers, rules for nodes, rules that
     * involve more than 1 layers.
     * @return the index of this node in its Technology.
     */
    public final int getPrimNodeInddexInTech() { return techPrimNodeIndex;}

    /**
     * Method to set the index of this node in its Technology.
     * @param index the index to use for this node in its Technology.
     */
    public void setPrimNodeIndexInTech(int index) {
        techPrimNodeIndex = index;
        assert techPrimNodeIndex == protoId.chronIndex;
    }

    /**
     * Compares PrimtiveNodes by their Technologies and definition order.
     * @param that the other PrimitiveNode.
     * @return a comparison between the PrimitiveNodes.
     */
	public int compareTo(PrimitiveNode that)
	{
		if (this.tech != that.tech)
		{
			int cmp = this.tech.compareTo(that.tech);
			if (cmp != 0) return cmp;
		}
		return this.techPrimNodeIndex - that.techPrimNodeIndex;
	}

	/**
	 * Returns a printable version of this PrimitiveNode.
	 * @return a printable version of this PrimitiveNode.
	 */
	public String toString()
	{
		return "node " + describe(true);
	}

    void copyState(PrimitiveNode that) {
        assert getId() == that.getId();
        userBits = that.userBits;
        assert specialType == that.specialType;
        if (specialValues != null) {
            assert specialValues.length == that.specialValues.length;
            System.arraycopy(that.specialValues, 0, specialValues, 0, specialValues.length);
        }
        System.arraycopy(that.sizeCorrectors, 0, sizeCorrectors, 0, sizeCorrectors.length);
        offset = that.offset;
        baseRectangle = that.baseRectangle;
        fullRectangle = that.fullRectangle;
        autoGrowth = that.autoGrowth;

        assert layers.length == that.layers.length;
        for (int i = 0; i < layers.length; i++) {
            layers[i].copyState(that.layers[i]);
        }
        if (electricalLayers != null) {
            assert electricalLayers.length == that.electricalLayers.length;
            for (int i = 0; i < electricalLayers.length; i++)
                electricalLayers[i].copyState(that.electricalLayers[i]);
        }

        assert primPorts.length == that.primPorts.length;
        for (int i = 0; i < primPorts.length; i++)
            primPorts[i].copyState(that.primPorts[i]);

        if (function == Function.NODE)
            factoryDefaultInst = that.factoryDefaultInst;
    }

    private static final String[] nodeBits = {
        "NODESHRINK", null, null,
        "LOWVTBIT", "HIGHVTBIT", "NATIVEBIT",
        "OD18BIT", "OD25BIT", "OD33BIT",
        "ARCSWIPE", "NSQUARE", "HOLDSTRACE",
        "CANBEZEROSIZE", "WIPEON1OR2", "LOCKEDPRIM",
        "NEDGESELECT", "ARCSHRINK", "NINVISIBLE",
        "SKIPSIZEINPALETTE", "NNOTUSED", null
    };

    void dump(PrintWriter out) {
        out.print("PrimitiveNode " + getName() + " " + getFunction());
        if (isNotUsed()) {
            out.println(" NOTUSED");
            return;
        }
        Technology.printlnBits(out, nodeBits, userBits);
        out.print("\tspecialType=" + specialType + " numMultiCuts=" + numMultiCuts);
        if (specialValues != null) {
            for (double v: specialValues)
                out.print(" " + v);
        }
        out.println();
        if (offset != null)
            out.println("\t" + offset);
        out.println("\tfullRectangle=" + fullRectangle);
        out.println("\tbaseRectangle=" + baseRectangle);
        if (minNodeSize != null)
            out.println("\tminNodeSize w=" + minNodeSize.getWidth() + " h=" + minNodeSize.getHeight() + " rule=" + minNodeSize.getRuleName());
        if (autoGrowth != null)
            out.println("\tautoGrowth " + autoGrowth);
        Technology.printlnPref(out, 1, "DefaultExtendXFor" + getName() + "IN" + tech.getTechName(), factoryDefaultInst.size.getLambdaX()*0.5);
        Technology.printlnPref(out, 1, "DefaultExtendYFor" + getName() + "IN" + tech.getTechName(), factoryDefaultInst.size.getLambdaY()*0.5);
        for (int techVersion = 0; techVersion < 2; techVersion++) {
            EPoint sizeCorrector = getSizeCorrector(techVersion);
            String diskOffset = "diskOffset" + (techVersion + 1);
            double x = sizeCorrector.getLambdaX();
            double y = sizeCorrector.getLambdaY();
            out.println("\t" + diskOffset + "=" + x + "," + y);
        }

        out.println("\tlayers:");
        boolean isSerp = specialType == SERPTRANS;
        dumpNodeLayers(out, layers, isSerp);
        if (electricalLayers != null) {
            out.println("\telectricalLayers:");
            dumpNodeLayers(out, electricalLayers, isSerp);
        }
        for (PrimitivePort pp: primPorts)
            pp.dump(out);
    }

    private void dumpNodeLayers(PrintWriter out, Technology.NodeLayer[] layers, boolean isSerp) {
        EPoint correction = EPoint.fromGrid(fullRectangle.getGridWidth(), fullRectangle.getGridHeight());
        for (Technology.NodeLayer nl: layers)
            nl.dump(out, correction, isSerp);
    }

    Xml.PrimitiveNodeGroup makeXml() {
        Xml.PrimitiveNodeGroup ng = new Xml.PrimitiveNodeGroup();
        ng.isSingleton = true;
        Xml.PrimitiveNode n = new Xml.PrimitiveNode();
        ng.nodes.add(n);
        n.name = getName();
        for (Map.Entry<String,PrimitiveNode> e: tech.getOldNodeNames().entrySet()) {
            if (e.getValue() != this) continue;
            assert n.oldName == null;
            n.oldName = e.getKey();
        }
        n.function = getFunction();
        ng.shrinkArcs = isArcsShrink();
        ng.square = isSquare();
        ng.canBeZeroSize = isCanBeZeroSize();
        ng.wipes = isWipeOn1or2();
        ng.lockable = isLockedPrim();
        ng.edgeSelect = isEdgeSelect();
        ng.skipSizeInPalette = isSkipSizeInPalette();
        ng.notUsed = isNotUsed();
        n.lowVt = isNodeBitOn(PrimitiveNode.LOWVTBIT);
        n.highVt = isNodeBitOn(PrimitiveNode.HIGHVTBIT);
        n.nativeBit  = isNodeBitOn(PrimitiveNode.NATIVEBIT);
        n.od18 = isNodeBitOn(PrimitiveNode.OD18BIT);
        n.od25 = isNodeBitOn(PrimitiveNode.OD25BIT);
        n.od33 = isNodeBitOn(PrimitiveNode.OD33BIT);

        PrimitiveNode.NodeSizeRule nodeSizeRule = getMinSizeRule();
        EPoint minFullSize = EPoint.fromGrid(
                (fullRectangle.getGridWidth() + 1 )/2,
                (fullRectangle.getGridHeight() + 1 )/2);
//        if (getFunction() == PrimitiveNode.Function.PIN && isArcsShrink()) {
//            assert getNumPorts() == 1;
//            PrimitivePort pp = getPort(0);
//            assert pp.getLeft().getMultiplier() == -0.5 && pp.getRight().getMultiplier() == 0.5 && pp.getBottom().getMultiplier() == -0.5 && pp.getTop().getMultiplier() == 0.5;
//            assert pp.getLeft().getAdder() == -pp.getRight().getAdder() && pp.getBottom().getAdder() == -pp.getTop().getAdder();
//            minFullSize = EPoint.fromLambda(pp.getLeft().getAdder(), pp.getBottom().getAdder());
//        }
//            DRCTemplate nodeSize = xmlRules.getRule(pnp.getPrimNodeIndexInTech(), DRCTemplate.DRCRuleType.NODSIZ);
        ng.baseLX.value = baseRectangle.getLambdaMinX();
        ng.baseHX.value = baseRectangle.getLambdaMaxX();
        ng.baseLY.value = baseRectangle.getLambdaMinY();
        ng.baseHY.value = baseRectangle.getLambdaMaxY();
//            EPoint minFullSize = EPoint.fromLambda(0.5*pnp.getDefWidth(), 0.5*pnp.getDefHeight());
        EPoint p1 = getSizeCorrector(0);
        EPoint p2 = getSizeCorrector(1);
        if (!p1.equals(p2))
            ng.diskOffset.put(Integer.valueOf(1), p1);
        if (!p2.equals(EPoint.ORIGIN))
            ng.diskOffset.put(Integer.valueOf(2), p2);
        ng.defaultWidth.addLambda(DBMath.gridToLambda(2*getFactoryDefaultGridExtendX()));
        ng.defaultHeight.addLambda(DBMath.gridToLambda(2*getFactoryDefaultGridExtendY()));

        List<Technology.NodeLayer> nodeLayers = Arrays.asList(getNodeLayers());
        List<Technology.NodeLayer> electricalNodeLayers = nodeLayers;
        if (getElectricalLayers() != null)
            electricalNodeLayers = Arrays.asList(getElectricalLayers());
        boolean isSerp = getSpecialType() == PrimitiveNode.SERPTRANS;
        int m = 0;
        for (Technology.NodeLayer nld: electricalNodeLayers) {
            int j = nodeLayers.indexOf(nld);
            if (j < 0) {
                ng.nodeLayers.add(nld.makeXml(isSerp, false, true));
                continue;
            }
            while (m < j)
                ng.nodeLayers.add(nodeLayers.get(m++).makeXml(isSerp, true, false));
            ng.nodeLayers.add(nodeLayers.get(m++).makeXml(isSerp, true, true));
        }
        while (m < nodeLayers.size())
            ng.nodeLayers.add(nodeLayers.get(m++).makeXml(isSerp, true, false));

        for (Iterator<PrimitivePort> pit = getPrimitivePorts(); pit.hasNext(); ) {
            PrimitivePort pp = pit.next();
            ng.ports.add(pp.makeXml());
        }
        ng.specialType = getSpecialType();
        if (getSpecialValues() != null)
            ng.specialValues = getSpecialValues().clone();
        if (nodeSizeRule != null) {
            ng.nodeSizeRule = new Xml.NodeSizeRule();
            ng.nodeSizeRule.width = nodeSizeRule.getWidth();
            ng.nodeSizeRule.height = nodeSizeRule.getHeight();
            ng.nodeSizeRule.rule = nodeSizeRule.getRuleName();
        }
        ng.spiceTemplate = getSpiceTemplate();
        return ng;
    }

	/**
	 * Method to get MinZ and MaxZ of the cell calculated based on nodes
	 * @param array array[0] is minZ and array[1] is max
	 */
	public void getZValues(double [] array)
	{
		for(int j=0; j<layers.length; j++)
		{
			Layer layer = layers[j].getLayer();

			// Skipping Glyph node
			if (layer.getTechnology() instanceof Generic) continue;
			double distance = layer.getDistance();
			double thickness = layer.getThickness();
			double z = distance + thickness;

			array[0] = (array[0] > distance) ? distance : array[0];
			array[1] = (array[1] < z) ? z : array[1];
		}
	}

	private void checkChanging() {}

    private void check() {
        assert fullRectangle.getGridMinX() == baseRectangle.getGridMinX() - offset.getLowXGridOffset();
        assert fullRectangle.getGridMaxX() == baseRectangle.getGridMaxX() + offset.getHighXGridOffset();
        assert fullRectangle.getGridMinY() == baseRectangle.getGridMinY() - offset.getLowYGridOffset();
        assert fullRectangle.getGridMaxY() == baseRectangle.getGridMaxY() + offset.getHighYGridOffset();
    }

    /**
     * Class to define a single rule on a node.
     */
    public class NodeSizeRule
    {
        protected final String ruleName;

        public class NodeSizeRuleError
        {
            public String message;
            public double actual, minSize;

            NodeSizeRuleError(String msg, double a, double s)
            {
                message = msg;
                actual = a;
                minSize = s;
            }
        }

        private NodeSizeRule(String r)
        {
            this.ruleName = r;
        }

        public String getRuleName()
        {
            if (ruleName != null)
                return ruleName;
            return getName() + " Min. Size";
        }

        public double getWidth() { return fullRectangle.getLambdaWidth(); }
        public double getHeight() { return fullRectangle.getLambdaHeight(); }

        /**
         * Methot to check whether the current NodeSize size meets
         * original conditions provided by the technology.
         * @param size
         */
        public List<NodeSizeRuleError> checkSize(PrimitiveNodeSize size)
        {
            List<NodeSizeRuleError> list = null;
            double base = baseRectangle.getLambdaWidth();
            double minSize = size.getDoubleAlongX();
            double diff = base - minSize;

            if (DBMath.isGreaterThan(diff, 0))
            {
                NodeSizeRuleError error = new NodeSizeRuleError("X axis", base, minSize);
                list = new ArrayList<NodeSizeRuleError>(2);
                list.add(error);
            }

            base = baseRectangle.getLambdaHeight();
            minSize = size.getDoubleAlongY();
            diff = base - minSize;

            if (DBMath.isGreaterThan(diff, 0))
            {
                NodeSizeRuleError error = new NodeSizeRuleError("Y axis", base, minSize);
                list = new ArrayList<NodeSizeRuleError>(2);
                list.add(error);
            }

            return list;
        }

//        /**
//         * Method to check if the given size complies with the node size rule. 0 is X, 1 is Y
//         * @param size
//         * @return
//         */
//        public List<NodeSizeRuleError> checkSize(EPoint size, EPoint base)
//        {
//            List<NodeSizeRuleError> list = null;
//            double diffX = getWidth() - size.getLambdaX();
//            if (DBMath.isGreaterThan(diffX, 0))
//            {
//                double actual = base.getLambdaX();
//                double minSize = actual + diffX;
//                NodeSizeRuleError error = new NodeSizeRuleError("X axis", actual, minSize);
//                list = new ArrayList<NodeSizeRuleError>(2);
//                list.add(error);
//            }
//            double diffY = getHeight() - size.getLambdaY();
//            if (DBMath.isGreaterThan(diffY, 0))
//            {
//                double actual = base.getLambdaY();
//                double minSize = actual + diffY;
//                NodeSizeRuleError error = new NodeSizeRuleError("Y axis", actual, minSize);
//                if (list == null)
//                    list = new ArrayList<NodeSizeRuleError>(1);
//                list.add(error);
//            }
//            return list;
//        }
    }

    /**
     * Class to detect those asymmetric metal contacts in new technologies where
     * witdh and height are different but they don't care about orientation
     */
    public class AsymmetricNodeSizeRule extends NodeSizeRule
    {
        AsymmetricNodeSizeRule(String r)
        {
            super(r);
        }

        public String getRuleName()
        {
            if (ruleName != null)
                return ruleName;
            return getName() + " Asymmetric Min. Size";
        }

        /**
         * Methot to check whether the current NodeSize size meets
         * original conditions provided by the technology.
         * In this case, the min. rule value
         * will be considered for the shortest side and the max. rule value for the longest side.
         * @param size
         */
        public List<NodeSizeRuleError> checkSize(PrimitiveNodeSize size)
        {
            List<NodeSizeRuleError> list = null;
            double lambdaX = size.getDoubleAlongX();
            double lambdaY = size.getDoubleAlongY();
            double sizeX = baseRectangle.getLambdaWidth();
            double sizeY = baseRectangle.getLambdaHeight();
            double shortest, longest;
            double shortVal, longVal;
            String shortMsg, longMsg;

            // The shortest side is along X
            if (DBMath.isLessThan(lambdaX, lambdaY))
            {
                shortest = lambdaX;
                shortMsg = "X axis";
                longest = lambdaY;
                longMsg = "Y axis";
            }
            else
            {
                shortest = lambdaY;
                shortMsg = "Y axis";
                longest = lambdaX;
                longMsg = "X axis";
            }

            // Getting the min. rule value
            if (DBMath.isLessThan(sizeX, sizeY))
            {
                shortVal = sizeX;
                longVal = sizeY;
            }
            else
            {
                shortVal = sizeY;
                longVal = sizeX;
            }

            double diffMin = shortVal - shortest;
            if (DBMath.isGreaterThan(diffMin, 0))
            {
                NodeSizeRuleError error = new NodeSizeRuleError(shortMsg, shortest, shortVal);
                list = new ArrayList<NodeSizeRuleError>(2);
                list.add(error);
            }
            double diffMax = longVal - longest;
            if (DBMath.isGreaterThan(diffMax, 0))
            {
                NodeSizeRuleError error = new NodeSizeRuleError(longMsg, longVal, longest);
                if (list == null)
                    list = new ArrayList<NodeSizeRuleError>(2);
                list.add(error);
            }
            return list;
        }

//        /**
//         * Method to check if the given size complies with the node size rule. In this case, the min. rule value
//         * will be considered for the shortest side and the max. rule value for the longest side.
//         * @param size
//         * @return
//         */
//        public List<NodeSizeRuleError> checkSize(EPoint size, EPoint base)
//        {
//            List<NodeSizeRuleError> list = null;
//            double lambdaX = size.getLambdaX();
//            double lambdaY = size.getLambdaY();
//            double sizeX = getWidth();
//            double sizeY = getHeight();
//            double shortest, longest;
//            double shortVal, longVal;
//            String shortMsg, longMsg;
//            double shortActual, longActual;
//
//            // The shortest side is along X
//            if (DBMath.isLessThan(lambdaX, lambdaY))
//            {
//                shortest = lambdaX;
//                shortMsg = "X axis";
//                shortActual = base.getLambdaX();
//                longest = lambdaY;
//                longMsg = "Y axis";
//                longActual = base.getLambdaY();
//            }
//            else
//            {
//                shortest = lambdaY;
//                shortMsg = "Y axis";
//                shortActual = base.getLambdaY();
//                longest = lambdaX;
//                longMsg = "X axis";
//                longActual = base.getLambdaX();
//            }
//
//            // Getting the min. rule value
//            if (DBMath.isLessThan(sizeX, sizeY))
//            {
//                shortVal = sizeX;
//                longVal = sizeY;
//            }
//            else
//            {
//                shortVal = sizeY;
//                longVal = sizeX;
//            }
//
//            double diffMin = shortVal - shortest;
//            if (DBMath.isGreaterThan(diffMin, 0))
//            {
//                NodeSizeRuleError error = new NodeSizeRuleError(shortMsg, shortActual, shortActual+diffMin);
//                list = new ArrayList<NodeSizeRuleError>(2);
//                list.add(error);
//            }
//            double diffMax = longVal - longest;
//            if (DBMath.isGreaterThan(diffMax, 0))
//            {
//                NodeSizeRuleError error = new NodeSizeRuleError(longMsg, longActual, longActual+diffMax);
//                if (list == null)
//                    list = new ArrayList<NodeSizeRuleError>(2);
//                list.add(error);
//            }
//            return list;
//        }
    }
}
