/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: NetFactory.java
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
// Annotated on 29 January 2004 by Ivan Sutherland

/**
 * NetFactory is a mish mash of methods that get Jemini started.
 * Methods called testOne, testTwo, and so forth test different input
 * files.  The readAndCompare method reads two files and starts up the
 * Jemini process on them.  I change this code often during testing to
 * control the tests I run.
*/

package com.sun.electric.tool.ncc.factory;
import com.sun.electric.tool.ncc.basicA.Messenger;
import com.sun.electric.tool.ncc.trees.JemEquivRecord;
import com.sun.electric.tool.ncc.trees.JemCircuit;
import com.sun.electric.tool.ncc.trees.NetObject;
import com.sun.electric.tool.ncc.jemNets.Wire;
import com.sun.electric.tool.ncc.jemNets.Part;

import com.sun.electric.tool.ncc.strategy.JemStrat;
import com.sun.electric.tool.ncc.strategy.JemStratCount;
import com.sun.electric.tool.ncc.strategy.JemStratPrint;
import com.sun.electric.tool.ncc.strategy.JemStratCheck;
import com.sun.electric.tool.ncc.strategy.JemManager;
/*
 import com.sun.electric.tool.ncc.jemNets.TransistorOne;
 import com.sun.electric.tool.ncc.jemNets.TransistorTwo;
 import com.sun.electric.tool.ncc.strategy.JemStratMergePar;
 import com.sun.electric.tool.ncc.strategy.JemStratPWsplit;
 */

import java.util.Iterator;

public class NetFactory {
    /** Here is the NetFactory constructor */
    public NetFactory(){}

    /**
     * Here is a Method to print out a JemCircuit
     * @param the JemCircuit to print
     */
    private void printCircuit(JemCircuit cc){
        Messenger.line(Wire.getWireCount() + " Wires made");
        Messenger.line(Part.getPartCount() + " Parts made");
        for (Iterator it= cc.getNetObjs(); it.hasNext();) {
            NetObject n= (NetObject)it.next();
            n.printMe(10);
        }
    }

    /**
     * The readAndCompare method reads the named files and starts
     * Jemini working on them.
     * @param String the first file name to use
     * @param String the second file name to use
     */
    private void readAndCompare(String aaa, String bbb){
        NetReaderB nra= NetReaderB.please();
        NetReaderB nrb= NetReaderB.please();
        Messenger.line("Reading input:");
        JemCircuit c= nra.read(aaa);
        JemCircuit d= nrb.read(bbb);

        JemEquivRecord g= JemEquivRecord.please(0);
        g.addCircuit(c);
        g.addCircuit(d);

        JemManager.doYourJob(g);
    }

    public void testOne(String netListA, String netListB){
    //    readAndCompare("equilibrate.flat", "equilibrate.flat");
    //    readAndCompare("stagePairJac.flat", "stagePairJac.flat"); //won't read
   //     readAndCompare("rxPadArray2.flat", "rxPadArray2.flat"); //no passes
   //     readAndCompare("rxGroup.flat", "rxGroup.flat");
   //   readAndCompare("expTail.flat", "expTail.flat"); //takes 500 passes
  //      readAndCompare("expArings.flat", "expArings.flat");
       // readAndCompare("twogate", "twogateb");
       readAndCompare(netListA, netListB);
    }

}
