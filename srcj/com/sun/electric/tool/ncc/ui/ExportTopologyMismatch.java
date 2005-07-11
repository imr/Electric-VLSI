/* -*- tab-width: 4 -*-
*
* Electric(tm) VLSI Design System
*
* File: Ncc.java
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
package com.sun.electric.tool.ncc.ui;

import com.sun.electric.tool.ncc.netlist.NetObject;
import com.sun.electric.tool.ncc.netlist.Port;

public class ExportTopologyMismatch extends ExportMismatch{

    private Port exp1, exp2;
    private NetObject sug = null;  // Port or Wire
    
    public ExportTopologyMismatch() {
        nameMatch = true;
        topologyMatch = false;
    }
    
    public ExportTopologyMismatch(String name1, String name2) {
        desingNames[0] = name1;
        desingNames[1] = name2;
        nameMatch = true;
        topologyMatch = false;
    }
    
    public Port getFirstExport() {
        return exp1;
    }

    public void setFirstExport(Port exp1) {
        this.exp1 = exp1;
    }

    public Port getSecondExport() {
        return exp2;
    }

    public void setSecondExport(Port exp2) {
        this.exp2 = exp2;
    }

    public NetObject getSuggestion() {
        return sug;
    }
    
    public void setSuggestion(NetObject sug) {
        this.sug = sug;
    }

}
