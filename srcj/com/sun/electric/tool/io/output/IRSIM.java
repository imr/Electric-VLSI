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

import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.geometry.DBMath;
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
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.TransistorSize;
import com.sun.electric.technology.technologies.Schematics;
import com.sun.electric.tool.simulation.Simulation;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.extract.*;

import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Class to write IRSIM netlists.
 */
public class IRSIM extends Output
        implements ParasiticGenerator
{
    private VarContext context;
    private List components;
    private Technology technology;

    /**
     * Class to define a component extracted from circuitry that is to be sent to the IRSIM simulator.
     */
	public static class ComponentInfoOLD
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
		IRSIM out = new IRSIM(cell);
        out.writeNetlist(cell, context, filePath);
        //out.writeNetlistOLD(cell, context, filePath+".old");
	}

	/**
	 * The main entry point for IRSIM extraction.
	 * @param cell the top-level cell to extract.
	 * @param context the hierarchical context to the cell.
	 * @return a List of ComponentInfoOLD objects that describes the circuit.
	 */
	public static List getIRSIMComponents(Cell cell, VarContext context)
	{
		// gather all components
		IRSIM out = new IRSIM(cell);
		return out.getNetlist(cell, context);
	}

    private List getNetlistOld(Cell cell, VarContext context)
	{
		// gather all components
		IRSIMNetlister netlister = new IRSIMNetlister();
		Netlist netlist = cell.getNetlist(true);
        this.context = context;
       if (context == null) this.context = VarContext.globalContext;
        components = new ArrayList();
		HierarchyEnumerator.enumerateCell(cell, context, netlist, netlister);
		return components;
	}

	private List getNetlist(Cell cell, VarContext context)
	{
		// gather all components
//		IRSIMNetlister netlister = new IRSIMNetlister();
//		Netlist netlist = cell.getNetlist(true);
        this.context = context;
       if (context == null) this.context = VarContext.globalContext;
//        components = new ArrayList();
//		HierarchyEnumerator.enumerateCell(cell, context, netlist, netlister);
        components = ParasiticTool.calculateParasistic(this, cell, context);
		return components;
	}

	private void writeNetlist(Cell cell, VarContext context, String filePath)
	{
		// gather all components
        List parasitics = getNetlist(cell, context);

		// write them
		if (openTextOutputStream(filePath)) return;

		// write the header
		double scale = technology.getScale() / 10;
		printWriter.println("| units: " + scale + " tech: " + technology.getTechName() + " format: SU");
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
		for(Iterator it = parasitics.iterator(); it.hasNext(); )
		{
			ExtractedPBucket ci = (ExtractedPBucket)it.next();
            String info = ci.getInfo(technology.getScale());
            if (info != null) printWriter.println(info);
		}

		if (closeTextOutputStream()) return;
		System.out.println(filePath + " written");
	}

	private void writeNetlistOLD(Cell cell, VarContext context, String filePath)
	{
		// gather all components
        components = getNetlistOld(cell, context);

		// write them
		if (openTextOutputStream(filePath)) return;

		// write the header
		double scale = technology.getScale() / 10;
		printWriter.println("| units: " + scale + " tech: " + technology.getTechName() + " format: SU");
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
			ComponentInfoOLD ci = (ComponentInfoOLD)it.next();
			if (ci.type == 'r' || ci.type == 'C')
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
	private IRSIM(Cell cell)
	{
        context = null;

        technology = cell.getTechnology();
		if (technology == Schematics.tech)
			technology = Technology.findTechnology(User.getSchematicTechnology());
	}

	/** IRSIM Netlister */

	private class IRSIMNetlister extends HierarchyEnumerator.Visitor
    {
        public HierarchyEnumerator.CellInfo newCellInfo() { return new IRSIMCellInfo(); }

        public boolean enterCell(HierarchyEnumerator.CellInfo info)
		{
            IRSIMCellInfo iinfo = (IRSIMCellInfo)info;
            iinfo.extInit();
            double scale = technology.getScale() / 10;

            Netlist netlist = info.getNetlist();
            // Calculating capacitance and resistance for arcs
            for (Iterator it = info.getCell().getArcs(); it.hasNext(); )
            {
                ArcInst arc = (ArcInst)it.next();
                int width = netlist.getBusWidth(arc);
                Network net = netlist.getNetwork(arc, 0);
                ComponentInfoOLD ci = new ComponentInfoOLD();
                String removeContext = context.getInstPath("/");
                int len = removeContext.length();
                if (len > 0) len++;
                ci.type = 'C';
                ci.netName1 = iinfo.getUniqueNetName(net, "/").substring(len);
                ci.netName2 = "gnd";
                Poly[] polys = technology.getShapeOfArc(arc);
                if (polys.length != 1)
                    System.out.println("Error, invalid geometry associated to arc " + arc + " in cell " + iinfo.getCell());
                else
                {
                    Poly poly = polys[0];
                    double area = poly.getArea();
                    ci.rcValue = area * poly.getLayer().getCapacitance() * scale * scale;
                }
            }
            return true;
        }        

        public void exitCell(HierarchyEnumerator.CellInfo info) {}

        public boolean visitNodeInst(Nodable no, HierarchyEnumerator.CellInfo info)
        {
            IRSIMCellInfo iinfo = (IRSIMCellInfo)info;

            NodeProto np = no.getProto();						// check if prototype is Primitive transistor
            if (!(np instanceof PrimitiveNode)) return true;	// descend and enumerate
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
					extra = valueVar.describe(info.getContext(), ni);
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

	            ComponentInfoOLD ci = new ComponentInfoOLD();
	            ci.ni = ni;
	            if (fun == PrimitiveNode.Function.RESIST) ci.type = 'r'; else ci.type = 'C';
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
            ComponentInfoOLD ci = new ComponentInfoOLD();
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
            if (dim == null || dim.getDoubleLength() == 0 || dim.getDoubleWidth() == 0)
            {
            	dim = new TransistorSize(new Double(2), new Double(2), new Double(2));
                System.out.println("Warning, ignoring non fet transistor " + ni + " in cell " + iinfo.getCell());
                return false;
            }
            float m = iinfo.getMFactor();
            ci.length = dim.getDoubleLength();
            ci.width = dim.getDoubleWidth() * m;

			// no parasitics yet
            double activeLen = dim.getDoubleActiveLength();
//            ci.sourceArea = dim.getDoubleWidth() * 6;
//            ci.sourcePerim = dim.getDoubleWidth() + 12;
//            ci.drainArea = dim.getDoubleWidth() * 6;
////            ci.drainPerim = dim.getDoubleWidth() + 12;
            ci.sourceArea = DBMath.round(dim.getDoubleWidth() * activeLen);
            ci.sourcePerim = DBMath.round((dim.getDoubleWidth() + activeLen)*2);
            ci.drainArea = ci.sourceArea ;
            ci.drainPerim = ci.sourcePerim;
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
    
    //---------------------------- ParasiticGenerator interface --------------------

    public ExtractedPBucket createBucket(NodeInst ni, Netlist netlistOLD,
                                         ParasiticTool.ParasiticCellInfo info)
    {
        ExtractedPBucket bucket = null;
        Netlist netlist = info.getNetlist();

        // Depending on primitive node
        if (ni.isPrimitiveTransistor())
        {
            PortInst g = ni.getTransistorGatePort();
            PortInst d = ni.getTransistorDrainPort();
            PortInst s = ni.getTransistorSourcePort();

            if (g == null || d == null || s == null)
            {
                System.out.println("PortInst for " + ni + " null!");
                return null;
            }
            Network gnet = netlist.getNetwork(g);
            Network dnet = netlist.getNetwork(d);
            Network snet = netlist.getNetwork(s);
            if (gnet == null || dnet == null || snet == null)
            {
                System.out.println("Warning, ignoring unconnected transistor " + ni + " in cell " + info.getCell());
                return null;
            }

            TransistorSize dim = ni.getTransistorSize(info.getContext());
            if (dim == null || dim.getDoubleLength() == 0 || dim.getDoubleWidth() == 0)
            {
                System.out.println("Warning, ignoring non fet transistor " + ni + " in cell " + info.getCell());
                return null;
            }

            // print out transistor
            String removeContext = context.getInstPath("/");
            int len = removeContext.length();
            if (len > 0) len++;
            String gName = info.getUniqueNetName(gnet, "/").substring(len);
            String sName = info.getUniqueNetName(snet, "/").substring(len);
            String dName = info.getUniqueNetName(dnet, "/").substring(len);

            bucket = new TransistorPBucket(ni, dim, gName, sName, dName, info.getMFactor());
        }
        else
        {
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
					extra = valueVar.describe(info.getContext(), ni);
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
	                return null;
	            }
	            Network net1 = netlist.getNetwork(end1);
	            Network net2 = netlist.getNetwork(end2);
	            if (net1 == null || net2 == null)
	            {
	                System.out.println("Warning, ignoring unconnected component " + ni + " in cell " + info.getCell());
	                return null;
	            }
	            String removeContext = context.getInstPath("/");
	            int len = removeContext.length();
	            if (len > 0) len++;

                char type = (fun == PrimitiveNode.Function.RESIST) ?
                    'r' : 'C';
//                StringBuffer line = new StringBuffer();
//                line.append(type);
//                line.append(" " + info.getUniqueNetName(net1, "/").substring(len));
//                line.append(" " + info.getUniqueNetName(net2, "/").substring(len));
//                line.append(" " + TextUtils.atof(extra));

                double rcValue = TextUtils.atof(extra);
                    bucket = new RCPBucket(type, info.getUniqueNetName(net1, "/").substring(len),
                            info.getUniqueNetName(net2, "/").substring(len), rcValue);
            }
        }
        return bucket;
    }
}
