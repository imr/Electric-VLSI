/* -*- tab-width: 4 -*-
*
* Electric(tm) VLSI Design System
*
* File: HighlightTools.java
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
package com.sun.electric.tool.user.ncc;

import java.awt.Frame;
import java.beans.PropertyVetoException;
import java.util.Iterator;

import javax.swing.JFrame;
import javax.swing.JInternalFrame;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.ncc.result.NetObjReport;
import com.sun.electric.tool.ncc.result.PartReport;
import com.sun.electric.tool.ncc.result.PortReport;
import com.sun.electric.tool.ncc.result.WireReport;
import com.sun.electric.tool.user.Highlighter;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.ui.WindowContent;
import com.sun.electric.tool.user.ui.WindowFrame;

public class HighlightTools {
    
    public static Highlighter getHighlighter(Cell cell, VarContext context) {
        Highlighter highlighter = null;
        // validate the cell (it may have been deleted)
        if (cell != null) {
            if (!cell.isLinked()) System.out.println("Cell is deleted");
            // make sure it is shown
            boolean found = false;
            EditWindow wnd = null;
            for(Iterator<WindowFrame> it = WindowFrame.getWindows(); it.hasNext(); ) {
                WindowFrame wf = (WindowFrame)it.next();
                WindowContent content = wf.getContent();
                if (!(content instanceof EditWindow)) continue;
                wnd = (EditWindow)content;
                if (wnd.getCell() == cell) {
                    if (((context != null) && context.equals(wnd.getVarContext())) ||
                            (context == null)) {
                        // already displayed.  force window "wf" to front
                        showFrame(wf);
                        found = true;
                        break;
                    }
                }
            }
            if (!found) {
                // make a new window for the cell
                WindowFrame wf = WindowFrame.createEditWindow(cell);
                wnd = (EditWindow)wf.getContent();
                wnd.setCell(cell, context, null);
            }
            highlighter = wnd.getHighlighter();
            highlighter.clear();
        }
        return highlighter;
    }
    
    private static void showFrame(WindowFrame wf) {
        if (TopLevel.isMDIMode()) {
            JInternalFrame jif = wf.getInternalFrame();
            try {
                jif.setIcon(false);
                jif.setSelected(true);
            } catch (PropertyVetoException e) {}
            if (!jif.isVisible()) {
                jif.toFront();
                TopLevel.addToDesktop(jif);
            } else
                jif.toFront();
        } else {
            JFrame jf = wf.getFrame();
            jf.setState(Frame.NORMAL);
            if (!jf.isVisible()) {
                jf.toFront();
                assert !Job.BATCHMODE;
                jf.setVisible(true);
            } else 
                jf.toFront();
        }                
    }
    
    public static void highlightPortExports(Highlighter highlighter, Cell cell, PortReport p) {
        String name = p.getWireName();
        Netlist netlist = cell.acquireUserNetlist();
        if (netlist == null) {
            System.out.println("Sorry, a deadlock aborted mimic-routing (network information unavailable).  Please try again");
            return;
        }
        
        for (Iterator<Network> it = netlist.getNetworks(); it.hasNext(); ) {
            Network net = (Network)it.next();
            if (! net.hasName(name)) continue;
            highlighter.addNetwork(net, cell);
            for (Iterator<Export> it2 = net.getExports(); it2.hasNext(); ) {
                Export exp = (Export)it2.next();
                // All the following appear to have no affect
                //highlighter.addText(exp, cell, null);
                //highlighter.addElectricObject(exp, cell);
                //highlighter.addObject(exp, cell);
            }
        }
    }

    public static void highlightPart(Highlighter highlighter, Cell cell, PartReport part) {
        String name = part.getNameProxy().leafName();
        Netlist netlist = cell.acquireUserNetlist();
        if (netlist == null) {
            System.out.println("Sorry, a deadlock aborted mimic-routing (network information unavailable).  Please try again");
            return;
        }
        for(Iterator<Nodable> it = netlist.getNodables(); it.hasNext(); ) {
            Nodable nod = (Nodable)it.next();
            if (name.equals(nod.getName()))
                highlighter.addElectricObject(nod.getNodeInst(), cell);
        }
    }
    
    public static void highlightWire(Highlighter highlighter, Cell cell, WireReport wire) {
        highlightNetNamed(highlighter, cell, wire.getNameProxy().leafName());
    }
    
    public static void highlightNetNamed(Highlighter highlighter, Cell cell, String name) {
        Netlist netlist = cell.acquireUserNetlist();
        if (netlist == null) {
            System.out.println("Sorry, a deadlock aborted mimic-routing (network information unavailable).  Please try again");
            return;
        }
        for(Iterator<Network> it = netlist.getNetworks(); it.hasNext(); ) {
            Network net = (Network)it.next();
            if (net.hasName(name)) {
                highlighter.addNetwork(net, cell);
                for (Iterator<Export> it2 = net.getExports(); it2.hasNext(); ) {
                    Export exp = (Export)it2.next();
                    highlighter.addText(exp, cell, null);
                }
            }
        }
    }

    public static void highlightPortOrWire(Highlighter highlighter, Cell cell, NetObjReport portOrWire) {
        if (portOrWire instanceof WireReport)
            highlightWire(highlighter, cell, (WireReport)portOrWire);
        else if (portOrWire instanceof PortReport)
            // highlight port exports
            highlightPortExports(highlighter, cell, (PortReport)portOrWire);
    }
}
