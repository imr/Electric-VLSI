/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: IRSIM.java
 * Input/output tool: IRSIM Netlist output
 * Written by Steven M. Rubin, Sun Microsystems.
 *
 * Copyright (c) 2004 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.io.output;

import com.sun.electric.database.geometry.Dimension2D;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.HierarchyEnumerator;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.text.Version;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.TransistorSize;
import com.sun.electric.technology.technologies.Schematics;
import com.sun.electric.tool.simulation.Simulation;
import com.sun.electric.tool.user.User;

import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Class to write IRSIM netlists.
 */
public class IRSIM extends Output
{
    private VarContext context;
    private List components;

    /**
     * Class to define a component extracted from circuitry that is to be sent to the IRSIM simulator.
     */
	public static class ComponentInfo
	{
		/** original node for this component */	public NodeInst ni;
		/** component type (n or p, R or C) */	public char     type;
		/** for transistors, the gate */		public String   netName1;
		/** for transistors, the source */		public String   netName2;
		/** for transistors, the drain */		public String   netName3;
		/** transistor size */					public double   length, width;
		/** transistor source info */			public double   sourceArea, sourcePerim;
		/** transistor drain info */			public double   drainArea, drainPerim;
		/** resistor/capacitor value */			public double   rcValue;
	}

	/**
	 * The main entry point for IRSIM deck writing.
	 * @param cell the top-level cell to write.
	 * @param context the hierarchical context to the cell.
	 * @param filePath the disk file to create with IRSIM.
	 */
	public static void writeIRSIMFile(Cell cell, VarContext context, String filePath)
	{
		IRSIM out = new IRSIM();
		out.writeNetlist(cell, context, filePath);
	}

	/**
	 * The main entry point for IRSIM extraction.
	 * @param cell the top-level cell to extract.
	 * @param context the hierarchical context to the cell.
	 * @return a List of ComponentInfo objects that describes the circuit.
	 */
	public static List getIRSIMComponents(Cell cell, VarContext context)
	{
		// gather all components
		IRSIM out = new IRSIM();
		List components = out.getNetlist(cell, context);
		return out.components;
	}

	private List getNetlist(Cell cell, VarContext context)
	{
		// gather all components
		IRSIMNetlister netlister = new IRSIMNetlister();
		Netlist netlist = cell.getNetlist(true);
        this.context = context;
        components = new ArrayList();
		HierarchyEnumerator.enumerateCell(cell, context, netlist, netlister);
		return components;
	}

	private void writeNetlist(Cell cell, VarContext context, String filePath)
	{
		// gather all components
        components = getNetlist(cell, context);

		// write them
		if (openTextOutputStream(filePath)) return;

		// write the header
		Technology tech = cell.getTechnology();
		if (tech == Schematics.tech)
			tech = Technology.findTechnology(User.getSchematicTechnology());
		double scale = tech.getScale() / 10;
		printWriter.println("| units: " + scale + " tech: " + tech.getTechName() + " format: SU");
		printWriter.println("| IRSIM file for cell " + cell.noLibDescribe() +
			" from library " + cell.getLibrary().getName());
		emitCopyright("| ", "");
		if (User.isIncludeDateAndVersionInOutput())
		{
			printWriter.println("| Created on " + TextUtils.formatDate(cell.getCreationDate()));
			printWriter.println("| Last revised on " + TextUtils.formatDate(cell.getRevisionDate()));
			printWriter.println("| Written on " + TextUtils.formatDate(new Date()) +
				" by Electric VLSI Design System, version " + Version.getVersion());
		} else
		{
			printWriter.println("| Written by Electric VLSI Design System");
		}

		// write the components
		for(Iterator it = components.iterator(); it.hasNext(); )
		{
			ComponentInfo ci = (ComponentInfo)it.next();
			if (ci.type == 'R' || ci.type == 'C')
			{
	            printWriter.print(ci.type);
				printWriter.print(" " + ci.netName1);
				printWriter.print(" " + ci.netName2);
				printWriter.println(" " + TextUtils.formatDouble(ci.rcValue));
			} else
			{
		        printWriter.print(ci.type);
		        printWriter.print(" " + ci.netName1);
		        printWriter.print(" " + ci.netName2);
		        printWriter.print(" " + ci.netName3);
		        printWriter.print(" " + TextUtils.formatDouble(ci.length));
		        printWriter.print(" " + TextUtils.formatDouble(ci.width));
		        printWriter.print(" " + TextUtils.formatDouble(ci.ni.getAnchorCenterX()));
		        printWriter.print(" " + TextUtils.formatDouble(ci.ni.getAnchorCenterY()));
		        if (ci.type == 'n') printWriter.print(" g=S_gnd");
		        if (ci.type == 'p') printWriter.print(" g=S_vdd");
				printWriter.print(" s=A_" + ci.sourceArea + ",P_" + ci.sourcePerim);
				printWriter.println(" d=A_" + ci.drainArea + ",P_" + ci.drainPerim);
			}
		}

		if (closeTextOutputStream()) return;
		System.out.println(filePath + " written");
	}

	/**
	 * Creates a new instance of IRSIM
	 */
	private IRSIM()
	{
        context = null;
	}

	/** IRSIM Netlister */
	private class IRSIMNetlister extends HierarchyEnumerator.Visitor
    {
        public HierarchyEnumerator.CellInfo newCellInfo() { return new IRSIMCellInfo(); }

        public boolean enterCell(HierarchyEnumerator.CellInfo info)
		{
            ((IRSIMCellInfo)info).extInit();
            return true;            
        }        

        public void exitCell(HierarchyEnumerator.CellInfo info) {}        

        public boolean visitNodeInst(Nodable no, HierarchyEnumerator.CellInfo info)
        {
            IRSIMCellInfo iinfo = (IRSIMCellInfo)info;

            NodeProto np = no.getProto();						// check if prototype is Primitive transistor
            if (!(np instanceof PrimitiveNode)) return true;	// descend and enumerate
            PrimitiveNode pn = (PrimitiveNode)np;
			NodeInst ni = (NodeInst)no; 						// Nodable is NodeInst because it is primitive node

			// handle resistors and capacitors
			PrimitiveNode.Function fun = ni.getFunction();
			if (fun == PrimitiveNode.Function.RESIST || fun == PrimitiveNode.Function.CAPAC ||
				fun == PrimitiveNode.Function.ECAPAC)
			{
				Variable.Key varKey = Schematics.SCHEM_CAPACITANCE;
				TextDescriptor.Unit unit = TextDescriptor.Unit.CAPACITANCE;
				if (fun == PrimitiveNode.Function.RESIST)
				{
					varKey = Schematics.SCHEM_RESISTANCE;
					unit = TextDescriptor.Unit.RESISTANCE;
				}
				Variable valueVar = ni.getVar(varKey);
				String extra = "";
				if (valueVar != null)
				{
					extra = valueVar.describe(context, ni);
					if (TextUtils.isANumber(extra))
					{
						double pureValue = TextUtils.atof(extra);
						extra = TextUtils.displayedUnits(pureValue, unit, TextUtils.UnitScale.NONE);
					}
				}
				PortInst end1 = ni.getPortInst(0);
				PortInst end2 = ni.getPortInst(1);
	            if (end1 == null || end2 == null)
	            {
	                System.out.println("PortInst for " + ni + " null!");
	                return false;
	            }
				Netlist netlist = info.getNetlist();
	            Network net1 = netlist.getNetwork(end1);
	            Network net2 = netlist.getNetwork(end2);
	            if (net1 == null || net2 == null)
	            {
	                System.out.println("Warning, ignoring unconnected component " + ni + " in cell " + iinfo.getCell());
	                return false;
	            }
	            String removeContext = context.getInstPath("/");
	            int len = removeContext.length();
	            if (len > 0) len++;

	            ComponentInfo ci = new ComponentInfo();
	            ci.ni = ni;
	            if (fun == PrimitiveNode.Function.RESIST) ci.type = 'R'; else ci.type = 'C';
	            ci.netName1 = iinfo.getUniqueNetName(net1, "/").substring(len);
	            ci.netName2 = iinfo.getUniqueNetName(net2, "/").substring(len);
	            ci.rcValue = TextUtils.atof(extra);
	            components.add(ci);
				return false;
			}

            if (!(ni.isPrimitiveTransistor())) return false;	// not transistor, ignore

            PortInst g = ni.getTransistorGatePort();
            PortInst d = ni.getTransistorDrainPort();
            PortInst s = ni.getTransistorSourcePort();
            if (g == null || d == null || s == null)
            {
                System.out.println("PortInst for " + ni + " null!");
                return false;
            }
			Netlist netlist = info.getNetlist();
            Network gnet = netlist.getNetwork(g);
            Network dnet = netlist.getNetwork(d);
            Network snet = netlist.getNetwork(s);
            if (gnet == null || dnet == null || snet == null)
            {
                System.out.println("Warning, ignoring unconnected transistor " + ni + " in cell " + iinfo.getCell());
                return false;
            }

            // print out transistor
            ComponentInfo ci = new ComponentInfo();
            ci.ni = ni;
            if (ni.getFunction() == PrimitiveNode.Function.TRANMOS || ni.getFunction() == PrimitiveNode.Function.TRA4NMOS)
            	ci.type = 'n'; else
            		ci.type = 'p';
            String removeContext = context.getInstPath("/");
            int len = removeContext.length();
            if (len > 0) len++;
            ci.netName1 = iinfo.getUniqueNetName(gnet, "/").substring(len);
            ci.netName2 = iinfo.getUniqueNetName(snet, "/").substring(len);
            ci.netName3 = iinfo.getUniqueNetName(dnet, "/").substring(len);
            TransistorSize dim = ni.getTransistorSize(iinfo.getContext());
            if (dim.getDoubleLength() == 0 || dim.getDoubleWidth() == 0)
            	dim = new TransistorSize(new Double(2), new Double(2));
            float m = iinfo.getMFactor();
            ci.length = dim.getDoubleLength();
            ci.width = dim.getDoubleWidth() * m;

			// no parasitics yet
            ci.sourceArea = dim.getDoubleWidth() * 6;
            ci.sourcePerim = dim.getDoubleWidth() + 12;
            ci.drainArea = dim.getDoubleWidth() * 6;
            ci.drainPerim = dim.getDoubleWidth() + 12;
/*
            ci.sourceArea = 0;
            ci.sourcePerim = 0;
            ci.drainArea = 0;
            ci.drainPerim = 0;
*/
            components.add(ci);
            return false;
        }
    }

    //----------------------------IRSIM Cell Info for HierarchyEnumerator--------------------

    /** IRSIM Cell Info class */
    private class IRSIMCellInfo extends HierarchyEnumerator.CellInfo
    {
        /** M-factor to be applied to size */       private float mFactor;

        /** initialize LECellInfo: assumes CellInfo.init() has been called */
        protected void extInit()
        {
            HierarchyEnumerator.CellInfo parent = getParentInfo();
            if (parent == null) mFactor = 1f;
            	else mFactor = ((IRSIMCellInfo)parent).getMFactor();
            // get mfactor from instance we pushed into
            Nodable ni = getContext().getNodable();
            if (ni == null) return;
            Variable mvar = ni.getVar(Simulation.M_FACTOR_KEY);
            if (mvar == null) return;
            Object mval = getContext().evalVar(mvar, null);
            if (mval == null) return;
            mFactor = mFactor * VarContext.objectToFloat(mval, 1f);
        }

        /** get mFactor */
        protected float getMFactor() { return mFactor; }
    }

}
