/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: SeaOfGatesFactory.java
 *
 * Copyright (c) 2010 Sun Microsystems and Static Free Software
 *
 * Electric(tm) is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
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
package com.sun.electric.tool.routing.seaOfGates;

/**
 * @author Felix Schmidt
 * 
 */
public class SeaOfGatesEngineFactory {

    public enum SeaOfGatesEngineType {
        newInfrastructure, oldThreads, defaultVersion, newInfrastructure2, newInfrastructure3
    }

    /**
     * Create a SeaOfGates version using the default version
     * 
     * @return
     */
    public static SeaOfGatesEngine createSeaOfGatesEngine() {
        return createSeaOfGatesEngine(SeaOfGatesEngineType.defaultVersion);
    }

    public static SeaOfGatesEngine createSeaOfGatesEngine(SeaOfGatesEngineType version) {
        SeaOfGatesEngine result = null;

        if (version.equals(SeaOfGatesEngineType.newInfrastructure))
            result = createSeaOfGatesEngineNew();
        else if (version.equals(SeaOfGatesEngineType.oldThreads))
            result = createSeaOfGatesEngineOld();
        else if (version.equals(SeaOfGatesEngineType.newInfrastructure2))
            result = createSeaOfGatesEngineNew2();
        else if (version.equals(SeaOfGatesEngineType.newInfrastructure3))
            result = createSeaOfGatesEngineNew3();
        else if (version.equals(SeaOfGatesEngineType.defaultVersion))
            result = createSeaOfGatesEngineOld();

        return result;
    }

    public static SeaOfGatesEngine createSeaOfGatesEngineOld() {
        return new SeaOfGatesEngineOld();
    }

    public static SeaOfGatesEngine createSeaOfGatesEngineNew() {
        return new SeaOfGatesEngineNew();
    }

    public static SeaOfGatesEngine createSeaOfGatesEngineNew2() {
        return new SeaOfGatesEngineNew2();
    }

    public static SeaOfGatesEngine createSeaOfGatesEngineNew3() {
        return new SeaOfGatesEngineNew3();
    }

}
