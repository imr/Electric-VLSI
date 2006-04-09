/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: DebugMenuRussell.java
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
package com.sun.electric.tool.user.menus;

import com.sun.electric.technology.Technology;
import com.sun.electric.tool.generator.layout.GateRegression;
import com.sun.electric.tool.generator.layout.Tech;
import com.sun.electric.tool.generator.layout.Test;
import com.sun.electric.tool.user.menus.MenuCommands.EMenu;
import com.sun.electric.tool.user.menus.MenuCommands.EMenuItem;

/**
 * Russell's TEST MENU
 */
public class DebugMenuRussell {

    static EMenu makeMenu() {

        return new EMenu("_Russell",
            new EMenuItem("Gate Generator Regression (MoCMOS)") { public void run() {
				new GateRegression(Tech.Type.MOCMOS); }},
            Technology.getTSMC90Technology() != null ? new EMenuItem("Gate Generator Regression (TSMC90)") { public void run() {
                new GateRegression(Tech.Type.TSMC90); }} : null,
            new EMenuItem("Random Test") { public void run() {
                new Test(); }});
    }
}
