package com.sun.electric.tool.user.ncc;

import java.util.Iterator;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.variable.VarContext;

public abstract class ExportConflict {
    protected Cell cell;
    protected VarContext context;
    protected String name;
    
    public ExportConflict(Cell cel, VarContext con, String nm) {
        cell = cel;
        context = con;
        name = nm;
    }
    
    public Cell       getCell()    { return cell; }
    public VarContext getContext() { return context; }
    public String     getName()    { return name; }
    
    protected abstract String getDescription(int col);

    
    public static class NetworkConflict extends ExportConflict {
        
        private Network localNet, globalNet;
        private String descr[] = new String[2];
        
        public NetworkConflict(Cell cel, VarContext con, String nm,
                                     Network lNet, Network gNet) {
            super(cel, con, nm);
            localNet = lNet;
            globalNet = gNet;
            descr[0] = createDescription(globalNet);
            descr[1] = createDescription(localNet);
        }
        
        protected String getDescription(int col) {
            if (col != 0 && col != 1) return null;
            return descr[col];
        }
        
        public Network getNetwork(int col) {
            if (col == 0)
                return globalNet;
            else if (col == 1)
                return localNet;
            else
                return null;
        }

        public Network getLocalNetwork()  { return localNet; }
        public Network getGlobalNetwork() { return globalNet; }
        
        private String createDescription(Network net) {
            StringBuffer buf = new StringBuffer(10);
            buf.append("{");
            for (Iterator it = net.getNames(); it.hasNext();) {
                buf.append(" " + (String)it.next());
                if (it.hasNext()) buf.append(",");
            }
            buf.append(" }");
            return buf.toString();
        }
    }
    
    
    public static class CharactConflict extends ExportConflict {
        
        private String localType, globalType;
        private Export localExport;
        
        public CharactConflict(Cell cel, VarContext con, String nm,
                                     String gType, String lType, Export exp) {
            super(cel, con, nm);
            localType = lType;
            globalType = gType;
            localExport = exp;
        }
        
        protected String getDescription(int col) {
            if (col == 0)
                return globalType;
            else if (col == 1)
                return localType;
            else
                return null;
        }
        
        public Export getGlobalExport() {
            return localExport;
        }
    }
}
