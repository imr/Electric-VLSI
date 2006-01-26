package com.sun.electric.tool.ncc;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.user.User;

import java.util.Iterator;
import java.util.HashMap;
import java.util.Map;

/**
 * Take care of Back-annotation after NCC has finished
 */
public class NccBackAnnotate {

    // ------------------- Job Wrapper Methods ---------------------------

    public static void backAnnotateNetNamesJob(NccResult result) {
        if (result == null) return;
        BackAnnotateJob job = new BackAnnotateJob(result, 0);
        job.startJob();
    }

    private static class BackAnnotateJob extends Job {
        private NccResult result;
        private int type;

		private BackAnnotateJob(NccResult result, int type) {
            super("BackAnnotateJob", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
            this.result = result;
            this.type = type;
        }
        public boolean doIt() throws JobException {
            switch(type) {
                case 0: {
                    backAnnotateNetNames(result);
                }
            }
            return true;
        }
    }

    // ---------------------- Real Methods -----------------------------

    public static void backAnnotateNetNames(NccResult result) {
        if (result == null) return;
        NetEquivalence equivs = result.getNetEquivalence();
        NccGlobals globals = result.getGlobalData();
        Cell [] rootCells = globals.getRootCells();
        // get layout cell
        if (rootCells.length != 2) return;
        int i=1;
        Cell layCell = rootCells[i];
        if (layCell.getView() != View.LAYOUT)
            i=0;
        layCell = rootCells[i];
        if (layCell.getView() != View.LAYOUT) return;       // no layout cell
        VarContext context = globals.getRootContexts()[i];

        // back annotate layout cell
        backAnnotateNetNames(equivs);
        //backAnnotateNetNames(layCell, VarContext.globalContext, equivs);
    }

    private static void backAnnotateNetNames(NetEquivalence equivs) {
        if (equivs.equivNets.length == 0) return;
        if (equivs.equivNets[0].length ==0) return;

        // find which of two is layout
        int lay = 0;
        int sch = 1;
        Network net = equivs.equivNets[lay][0].getNet();
        if (net.getParent().getView() != View.LAYOUT)
            lay = 1;
        net = equivs.equivNets[lay][0].getNet();
        if (net.getParent().getView() != View.LAYOUT)
            return;                     // no layout Cell
        sch = (lay == 1) ? 0 : 1;

        HashMap<String,Network> backAnnotated = new HashMap<String,Network>();          // prevent duplicates
        HashMap<ArcInst,String> newArcNames = new HashMap<ArcInst,String>();            // store all changes till end
        for (int i=0; i<equivs.equivNets[0].length; i++) {
            // get lay net name
            Network layNet = equivs.equivNets[lay][i].getNet();
            String layName = layNet.getName();
//            if (!layNet.hasNames()) continue;
//            String layName = (String)layNet.getNames().next();

            // get sch net name
            Network schNet = equivs.equivNets[sch][i].getNet();
            String schName = schNet.getName();
//            if (!schNet.hasNames()) continue;
//            String schName = (String)schNet.getNames().next();

            // only back-annotate if parent cells are from same cell group
            //if (layNet.getParent().getCellGroup() != schNet.getParent().getCellGroup()) continue;

            // debug
            System.out.print("("+layNet.getParent().describe(false)+")lay:\t"+layName+"\t("+schNet.getParent().describe(false)+")sch:");
            for (Iterator<String> it = schNet.getNames(); it.hasNext();) {
                System.out.print("\t"+(String)it.next());
            }
            System.out.println();

            // check layout net
            if (layNet.isExported()) continue;      // exported, do not name
            if (layName.indexOf('@') == -1)
                continue;               // this net already has a name

            // check schematic net
            if (schName.indexOf('@') != -1) continue;   // ignore default net names

            // if sch net name already in layout, skip
            Cell layCell = layNet.getParent();
            boolean skip = false;
            for (Iterator<Network> it = layNet.getNetlist().getNetworks(); it.hasNext();) {
                net = (Network)it.next();
                if (net.hasName(schName)) {
                    skip = true;
                    //System.out.println("Found name '"+schName+"' already on net in cell "+layCell.describe(true)+", layNet to backAnnotate is "+layName);
                    break;
                }
            }
            if (skip) continue;

            // skip if already back-annotated (multiple instances allow for
            // multiple nets in list pointing to same net in cell).
            if (backAnnotated.containsKey(layCell.describe(false) + layName))
                continue;

            // name an arc on net if possible
            Iterator<ArcInst> arcIt = layNet.getArcs();
            if (arcIt.hasNext()) {
                ArcInst ai = (ArcInst)arcIt.next();
                //ai.setName(schName);
                newArcNames.put(ai, schName);
                System.out.println("Back-annotated in cell "+layCell.describe(true)+", net '"+layName+"' to '"+schName+"'");
                backAnnotated.put(layCell.describe(false) + layName, layNet);
            }
        }
        for (Iterator<Map.Entry<ArcInst,String>> it = newArcNames.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<ArcInst,String> entry = (Map.Entry<ArcInst,String>)it.next();
            ArcInst ai = (ArcInst)entry.getKey();
            String name = (String)entry.getValue();
            ai.setName(name);
        }
    }

}
