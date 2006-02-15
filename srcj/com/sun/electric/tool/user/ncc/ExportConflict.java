/* -*- tab-width: 4 -*-
*
* Electric(tm) VLSI Design System
*
* File: ExportConflict.java
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

import java.io.Serializable;
import java.util.Iterator;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.variable.VarContext;

/**
 *  This class is an abstract container for an Export Conflict.
 *  It holds Cell and Context of the conflict, and conflicting name  
 */
public abstract class ExportConflict implements Serializable {
    /** Conflict Context */  protected Cell cell;
    /** Conflict Cell    */  protected VarContext context;
    /** Conflicting name */  protected String name;
    
    public ExportConflict(Cell cel, VarContext con, String nm) {
        cell = cel;
        context = con;
        name = nm;
    }
    
    public Cell       getCell()    { return cell; }
    public VarContext getContext() { return context; }
    public String     getName()    { return name; }
    
    /**
     * Get text which should be printed as a hyperlink to conflicting 
     * Networks/Exports
     * @param col  table column
     * @return text to print as a hyperlink to conflicting Networks/Exports
     */
    protected abstract String getDescription(int col);

    /**
     * This class is a container for an Export/Global Network Conflict.
     * A local Network has the same name as a global Network, which creates
     * a conflict. 
     */
    public static class NetworkConflict extends ExportConflict {
    	static final long serialVersionUID = 0;

        
        /** Local  Network          */ private Network localNet;
        /** Global Network          */ private Network globalNet;
        /** Network hyperlink texts */ private String descr[] = new String[2];
        
        public NetworkConflict(Cell cel, VarContext con, String nm,
                                     Network lNet, Network gNet) {
            super(cel, con, nm);
            localNet = lNet;
            globalNet = gNet;
            descr[0] = createDescription(globalNet);
            descr[1] = createDescription(localNet);
        }
        
        /**
         * Get text which should be printed as a hyperlink to a conflicting Network.
         * This text id a list of Network names surrounded by curly brackets {}
         * Column 0 corresponds to the local Network, 1 - to the global one.
         * @param col  table column If column is not 0 or 1, then null is returned
         * @return text to print as a hyperlink to conflicting Networks/Exports
         */        
        protected String getDescription(int col) {
            if (col != 0 && col != 1) return null;
            return descr[col];
        }

        /**
         * Get conflicting Network. Column 0 corresponds to the local Network, 
         * 1 - to the global one.
         * @param col  table column If column is not 0 or 1, then null is returned
         * @return conflicting Network for the given table column
         */
        public Network getNetwork(int col) {
            if (col == 0)
                return globalNet;
            else if (col == 1)
                return localNet;
            else
                return null;
        }

        /**
         * Get the local conflicting Network
         * @return the local conflicting Network
         */
        public Network getLocalNetwork()  { return localNet; }
        
        /**
         * Get the global conflicting Network
         * @return the global conflicting Network
         */        
        public Network getGlobalNetwork() { return globalNet; }
        
        /**
         * Create text which should be printed as a hyperlink to conflicting Network.
         * This text id a list of Network names surrounded by curly brackets {}
         * @param net  Network to create a text for
         * @return hyperlink text for the provided Network
         */
        private String createDescription(Network net) {
            StringBuffer buf = new StringBuffer(10);
            buf.append("{");
            for (Iterator<String> it = net.getNames(); it.hasNext();) {
                buf.append(" " + (String)it.next());
                if (it.hasNext()) buf.append(",");
            }
            buf.append(" }");
            return buf.toString();
        }
    }
    
    /**
     * This class is a container for an Export/Global Characteristics conflict.
     * A local Export has the same name as a global signal, which creates
     * a conflict. 
     */    
    public static class CharactConflict extends ExportConflict {
    	static final long serialVersionUID = 0;
        
        /** Local Export type  */ private String localType;
        /** Global signal type */ private String globalType;
        /** Local Export       */ private Export localExport;
        
        public CharactConflict(Cell cel, VarContext con, String nm,
                                     String gType, String lType, Export exp) {
            super(cel, con, nm);
            localType = lType;
            globalType = gType;
            localExport = exp;
        }
        
        /**
         * Get text which should be printed as a hyperlink to conflicting 
         * local Export and global signal. This text is simply the type of 
         * Export or signal. Column 0 corresponds to the global signal, 
         * 1 - to the local Export.  
         * @param col  table column If column is not 0 or 1, then null is returned
         * @return text to print as a hyperlink to conflicting Networks/Exports
         */             
        protected String getDescription(int col) {
            if (col == 0)
                return globalType;
            else if (col == 1)
                return localType;
            else
                return null;
        }
        
        /**
         * Get local Export
         * @return local Export
         */
        public Export getLocalExport() { return localExport; }
    }
}
