/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: jemStratPartWirePort.java
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

/**
 * JemStratPartsWiresPorts divides into Parts, Wires, and Ports:
 * doYourJob(JemSets) uses starter as the seed and marks parts, wires,
 * and ports with their top element.
 */
package com.sun.electric.tool.ncc.strategy;
import com.sun.electric.tool.ncc.basicA.Messenger;
import com.sun.electric.tool.ncc.trees.*;
import com.sun.electric.tool.ncc.lists.*;
import com.sun.electric.tool.ncc.jemNets.*;
//import com.sun.electric.tool.ncc.jemNets.Port;
//import com.sun.electric.tool.ncc.strategy.JemSets;

//import java.util.Collection;
import java.util.Iterator;

public class JemStratPartWirePort extends JemStrat {
    private int numParts;
    private int numWires;
    private int numPorts;

    private static final int NUM_CODES= 3;
    protected static final Integer CODE_PART= new Integer(0);
    protected static final Integer CODE_WIRE= new Integer(1);
    protected static final Integer CODE_PORT= new Integer(2);

    private JemStratPartWirePort(){}

    // ---------- to do the job -------------

    public static JemRecord doYourJob(JemSets jss){
    	JemStratPartWirePort pwp = new JemStratPartWirePort();
    	return pwp.doYourJob2(jss);
    }
    
    private JemRecord doYourJob2(JemSets jss){
        JemEquivRecord ss= (JemEquivRecord)jss.starter;
        preamble(ss);
        JemEquivList offspring= doFor(ss);
        summary(offspring);
        
        Messenger.line("Jemini proceeds with these maximum counts: ");

		jss.starter = getOffspringParent(offspring);		
		jss.parts= pickAnOffspring(CODE_PART, offspring, "Parts and");
		jss.wires= pickAnOffspring(CODE_WIRE, offspring, "Wires and");
		jss.ports= pickAnOffspring(CODE_PORT, offspring, "Ports");

		Messenger.line("JemStratPartWirePort: "); 
		Messenger.line(offspringStats(offspring));

        Messenger.freshLine();
        return jss.starter;
    }

    //do something before starting
    private void preamble(JemRecord j){
        startTime("jemStratPartWirePort" , j.nameString());
    }

    //summarize at the end
    private void summary(JemEquivList cc){
        Messenger.line("jemStratPartWirePort separated " +
                            numParts + " Parts and " +
                            numWires + " Wires and " +
                            numPorts + " Ports into " +
                            NUM_CODES + " distinct hash groups");
        //	+ numPorts + " Ports.");
        Messenger.line(cc.sizeInfoString());
        elapsedTime(numParts + numWires + numPorts);
    }

    //------------- for NetObject ------------

    public Integer doFor(NetObject n){
        if(n instanceof Wire){
            numWires++;
            return CODE_WIRE;
        } else if(n instanceof Port){
            numPorts++;
            return CODE_PORT;
        } else if(n instanceof Part){
            numParts++;
            return CODE_PART;
        } else {
			error(true, "unrecognized NetObject");
			return CODE_ERROR;
		}
    }
    
}
