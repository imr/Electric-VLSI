/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: JemManager.java
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
 * JemManager keeps lists of active JemEquivRecord. it knows what the
 * strategies do and puts newly-created JemEquivRecord on the right
 * lists.
 */
package com.sun.electric.tool.ncc.strategy;
import com.sun.electric.tool.ncc.trees.JemEquivRecord;
import com.sun.electric.tool.ncc.trees.JemCircuit;
// import com.sun.electric.tool.ncc.trees.JemTree;
import com.sun.electric.tool.ncc.trees.NetObject;
import com.sun.electric.tool.ncc.basicA.Messenger;
//import com.sun.electric.tool.ncc.strategy.JemSets;
//import com.sun.electric.tool.ncc.strategy.JemStrat;
import com.sun.electric.tool.ncc.strategy.JemStratFixed;
import com.sun.electric.tool.ncc.strategy.JemStratVariable;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;

public class JemManager {

    public static void doYourJob(JemEquivRecord g){
		JemSets myJemSets= new JemSets();
		
		JemStratFixed.doYourJob(myJemSets, g);
		JemStratVariable.doYourJob(myJemSets);

        Messenger.freshLine();

//		myJemSets.showTheFrontier(myJemSets.starter);
    }

}
