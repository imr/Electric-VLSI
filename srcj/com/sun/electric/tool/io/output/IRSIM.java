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

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.text.Version;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.TransistorSize;
import com.sun.electric.technology.technologies.Schematics;
import com.sun.electric.tool.extract.ExtractedPBucket;
import com.sun.electric.tool.extract.ParasiticGenerator;
import com.sun.electric.tool.extract.ParasiticTool;
import com.sun.electric.tool.extract.RCPBucket;
import com.sun.electric.tool.extract.TransistorPBucket;
import com.sun.electric.tool.user.User;

import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * Class to write IRSIM netlists.
 */
public class IRSIM extends Output
        implements ParasiticGenerator
{
    private VarContext context;
    private List<Object> components;
    private Technology technology;

	/**
	 * The main entry point for IRSIM deck writing.
     * @param cell the top-level cell to write.
     * @param context the hierarchical context to the cell.
	 * @param filePath the disk file to create.
	 */
	public static void writeIRSIMFile(Cell cell, VarContext context, String filePath)
	{
		IRSIM out = new IRSIM(cell);
        out.writeNetlist(cell, context, filePath);
	}

	/**
	 * The main entry point for IRSIM extraction.
	 * @param cell the top-level cell to extract.
	 * @param context the hierarchical context to the cell.
	 * @return a List of ComponentInfoOLD objects that describes the circuit.
	 */
	public static List<Object> getIRSIMComponents(Cell cell, VarContext context)
	{
		// gather all components
		IRSIM out = new IRSIM(cell);
		return out.getNetlist(cell, context);
	}

	private List<Object> getNetlist(Cell cell, VarContext context)
	{
        this.context = context;
        if (context == null) this.context = VarContext.globalContext;
        components = ParasiticTool.calculateParasistic(this, cell, context);
		return components;
	}

	private void writeNetlist(Cell cell, VarContext context, String filePath)
	{
		// gather all components
        List<Object> parasitics = getNetlist(cell, context);

		// write them
		if (openTextOutputStream(filePath)) return;

		// write the header
		double scale = technology.getScale() / 10;
        Technology layoutTech = Schematics.getDefaultSchematicTechnology();
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
		for(Object obj : parasitics)
		{
			ExtractedPBucket ci = (ExtractedPBucket)obj;
            String info = ci.getInfo(layoutTech);
            if (info != null && !info.equals("")) printWriter.println(info);
		}

		if (closeTextOutputStream()) return;
		System.out.println(filePath + " written");
        ParasiticTool.getParasiticErrorLogger().sortLogs();
        ParasiticTool.getParasiticErrorLogger().termLogging(true);
	}

	/**
	 * Creates a new instance of IRSIM
	 */
	private IRSIM(Cell cell)
	{
        context = null;

        technology = cell.getTechnology();
		if (technology == Schematics.tech)
			technology = User.getSchematicTechnology();
	}

    //---------------------------- ParasiticGenerator interface --------------------

    public ExtractedPBucket createBucket(NodeInst ni, ParasiticTool.ParasiticCellInfo info)
    {
        ExtractedPBucket bucket = null;
        Netlist netlist = info.getNetlist();
        int numRemoveParents = context.getNumLevels();

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
            String gName = info.getUniqueNetNameProxy(gnet, "/").toString(numRemoveParents);
            String sName = info.getUniqueNetNameProxy(snet, "/").toString(numRemoveParents);
            String dName = info.getUniqueNetNameProxy(dnet, "/").toString(numRemoveParents);

            bucket = new TransistorPBucket(ni, dim, gName, sName, dName, info.getMFactor());
        }
        else
        {
            // handle resistors and capacitors
			PrimitiveNode.Function fun = ni.getFunction();
            double rcValue = 0;
            char type = 0;
            Network net1 = null, net2 = null;
            String net1Name = null, net2Name = null;
            Technology tech = info.getCell().getTechnology();

            if (fun == PrimitiveNode.Function.CONTACT)
            {
                for (Iterator<Connection> it = ni.getConnections(); it.hasNext();)
                {
                    Connection c = it.next();
                    Network net = netlist.getNetwork(c.getArc(), 0);
                    if (net1 == null)
                    {
                        net1 = net;
                        net1Name = info.getUniqueNetNameProxy(net1, "/").toString(numRemoveParents) + "_" + c.getArc().getName();
                    }
                    else if (net2 == null)
                    {
                        net2 = net;
                        net2Name = info.getUniqueNetNameProxy(net2, "/").toString(numRemoveParents) + "_" + c.getArc().getName();
                    }
                    else
                        System.out.println("Warning: contact " + ni.describe(true) + " has more than 2 connections, RC estimation may be wrong");
                }

                // RC value will be via resistance divided by number of cuts of this contact
                // Searching for via layer
                PrimitiveNode pn = (PrimitiveNode)ni.getProto();
                Technology.MultiCutData mcd = new Technology.MultiCutData(ni.getD(), pn.getSpecialValues());
                int cuts = mcd.numCuts();
                Technology.NodeLayer[] layers = pn.getLayers();
                Layer thisLayer = null;

                for (int i = 0; i < layers.length; i++)
                {
                    if (layers[i].getLayer().getFunction().isContact())
                    {
                        thisLayer = layers[i].getLayer();
                        break;
                    }
                }
                if (thisLayer != null)
                    rcValue = thisLayer.getResistance()/cuts;
                type = 'r';
                // Only valid for layout
                if ((rcValue < tech.getMinResistance()))
                    return null;
            }
			else if (fun.isResistor() || fun.isCapacitor()) //fun == PrimitiveNode.Function.RESIST || fun == PrimitiveNode.Function.PRESIST ||
//                    fun == PrimitiveNode.Function.CAPAC || fun == PrimitiveNode.Function.ECAPAC)
			{
                PortInst end1 = ni.getPortInst(0);
                PortInst end2 = ni.getPortInst(1);

                if (end1 == null || end2 == null)
                {
                    System.out.println("PortInst for " + ni + " null!");
                    return null;
                }
                net1 = netlist.getNetwork(end1);
                net2 = netlist.getNetwork(end2);
                if (net1 == null || net2 == null)
                {
                    System.out.println("Warning, ignoring unconnected component " + ni + " in cell " + info.getCell());
                    return null;
                }
                net1Name = info.getUniqueNetNameProxy(net1, "/").toString(numRemoveParents);
                net2Name = info.getUniqueNetNameProxy(net2, "/").toString(numRemoveParents);

				Variable.Key varKey = Schematics.SCHEM_CAPACITANCE;
				TextDescriptor.Unit unit = TextDescriptor.Unit.CAPACITANCE;
				if (fun.isResistor()) //fun == PrimitiveNode.Function.RESIST || fun == PrimitiveNode.Function.PRESIST)
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
						extra = TextUtils.formatDoublePostFix(pureValue); // displayedUnits(pureValue, unit, TextUtils.UnitScale.NONE);
					}
				}
                rcValue = TextUtils.parsePostFixNumber(extra).doubleValue();
                //rcValue = TextUtils.atof(extra);

                if (fun.isResistor()) // == PrimitiveNode.Function.RESIST)
                {
                    type = 'r';
                }
                else
                {
                    type = 'C';
                    rcValue = Math.rint((rcValue/ 1e-15) * 1000) / 1000;
                }
            }
            if (type == 0) return null;
            if ((type == 'C' && rcValue < tech.getMinCapacitance()))
                return null;
            bucket = new RCPBucket(type, net1Name, net2Name, rcValue);
        }
        return bucket;
    }
}
