/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: IRSIM.java
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
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.TransistorSize;
import com.sun.electric.tool.user.User;

import java.util.Date;

/**
 * Class to write IRSIM netlists.
 */
public class IRSIM extends Output
{
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

	private void writeNetlist(Cell cell, VarContext context, String filePath)
	{
		if (openTextOutputStream(filePath)) return;
		writeHeader(cell);

		IRSIMNetlister netlister = new IRSIMNetlister();
		Netlist netlist = cell.getNetlist(true);
		HierarchyEnumerator.enumerateCell(cell, context, netlist, netlister);
		if (closeTextOutputStream()) return;
		System.out.println(filePath + " written");
	}

	/**
	 * Creates a new instance of IRSIM
	 */
	private IRSIM()
	{
	}

	private void writeHeader(Cell cell)
	{
		// get scale in centimicrons
		double scale = Technology.getCurrent().getScale() / 10;
		printWriter.println("| units: " + scale + " tech: mocmos format: SU");
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
            if (!(ni.isPrimitiveTransistor())) return false;	// not transistor, ignore
            boolean isNMOS = false;
            if (ni.getFunction() == NodeProto.Function.TRANMOS ||
                ni.getFunction() == NodeProto.Function.TRA4NMOS)
                	isNMOS = true;

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
            if (isNMOS) printWriter.print("n"); else
				printWriter.print("p");
			printWriter.print(" " + iinfo.getUniqueNetName(gnet, "/"));
			printWriter.print(" " + iinfo.getUniqueNetName(snet, "/"));
			printWriter.print(" " + iinfo.getUniqueNetName(dnet, "/"));
            TransistorSize dim = ni.getTransistorSize(iinfo.getContext());
            float m = iinfo.getMFactor();
			printWriter.print(" " + dim.getLength());                // length
			printWriter.print(" " + (double)m * dim.getDoubleWidth());     // width
			printWriter.print(" " + ni.getAnchorCenterX());          // xpos
			printWriter.print(" " + ni.getAnchorCenterY());          // ypos
            if (isNMOS)
				printWriter.print(" g=S_gnd");
            else 
				printWriter.print(" g=S_vdd");

			// no parasitics yet
            double sourceArea = 0;
            double sourcePerim = 0;
            double drainArea = 0;
            double drainPerim = 0;
			printWriter.print(" s=A_" + sourceArea + ",P_" + sourcePerim);
			printWriter.print(" d=A_" + drainArea + ",P_" + drainPerim);
			printWriter.println();
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
            Variable mvar = ni.getVar("ATTR_M");
            if (mvar == null) return;
            Object mval = getContext().evalVar(mvar, null);
            if (mval == null) return;
            mFactor = mFactor * VarContext.objectToFloat(mval, 1f);
        }

        /** get mFactor */
        protected float getMFactor() { return mFactor; }
    }

}
