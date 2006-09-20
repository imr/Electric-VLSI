/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: FillGenConfig.java
 *
 * Copyright (c) 2006 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.generator.layout.fill;

import com.sun.electric.tool.generator.layout.Tech;
import com.sun.electric.tool.generator.layout.LayoutLib;
import com.sun.electric.tool.Job;
import com.sun.electric.technology.Technology;

import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;

/****************************** CONFIG ******************************/

public class FillGenConfig implements Serializable
{
    Tech.Type techNm = Tech.Type.INVALID;
    FillGeneratorTool.FillTypeEnum fillType = FillGeneratorTool.FillTypeEnum.INVALID;
    public String fillLibName;
    List<ReserveConfig> reserves = new ArrayList<ReserveConfig>();
    public boolean evenLayersHorizontal;
    public double width, height, targetW, targetH, minTileSizeX, minTileSizeY;
    public FillGeneratorTool.ExportConfig perim;
    public int firstLayer, lastLayer;
    public int[] cellTiles;
    public boolean hierarchy;
    public double minOverlap;
    public double drcSpacingRule;
    public boolean binary;
    public boolean useMaster;
    public boolean onlyAround;
    public double gap; // allowed overlap between given cells and masters. Typical value is 1.5
    public boolean onlySkill; // don't attempt to connect wires... -> very slow
    public int level; // to control the level of hierarchy in case of onlyAround option
    public Job job;

    public FillGenConfig(FillGeneratorTool.FillTypeEnum type, Tech.Type tech, String lib, FillGeneratorTool.ExportConfig perim,
                         int first, int last,
                         double w, double h, boolean even,
                         int[] cellTiles, boolean hierarchy, double minO, double drcSpacingRule,
                         boolean binary, boolean useMaster, boolean onlyAround, double gap, boolean onlySkill,
                         int level)
    {
        this.fillType = type;
        this.cellTiles = cellTiles;
        this.hierarchy = hierarchy;
        this.minOverlap = minO;
        this.drcSpacingRule = drcSpacingRule;
        this.binary = binary;
        this.width = w;
        this.height = h;
        this.evenLayersHorizontal = even;
        this.useMaster = useMaster;

        techNm = tech;

        LayoutLib.error((techNm != Tech.Type.MOCMOS && techNm != Tech.Type.TSMC180),
            "FillGeneratorTool only recognizes the technologies: "+
            Tech.Type.MOCMOS+" and "+Tech.Type.TSMC180+".\n"+
            "For 90nm use FillGenerator90");
        this.fillLibName = lib;
        this.perim = perim;
        this.firstLayer = first;
        this.lastLayer = last;
        this.onlyAround = onlyAround;
        this.gap = gap; // only valid if onlyAround=true
        this.onlySkill = onlySkill;
        this.level = level;
    }

    public void setTargetValues(double targetW, double targetH, double sx, double sy)
    {
        this.targetW = targetW;
        this.targetH = targetH;
        this.minTileSizeX = sx;
        this.minTileSizeY = sy;
    }

    public ReserveConfig reserveSpaceOnLayer(Technology tech, int layer,
                                             double vddReserved, FillGeneratorTool.Units vddUnits,
                                             double gndReserved, FillGeneratorTool.Units gndUnits)
    {
        int numMetals = tech.getNumMetals();
        LayoutLib.error(layer<1 || layer>numMetals,
                    "Bad layer. Layers must be between 2 and "+numMetals+ " inclusive: "+
                    layer);
        ReserveConfig config = new ReserveConfig(layer, vddReserved, gndReserved, vddUnits, gndUnits);
        reserves.add(config);
        return config;
    }

    public static class ReserveConfig implements Serializable
    {
        int layer;
        double vddReserved, gndReserved, vddWidth, gndWidth;
        FillGeneratorTool.Units vddUnits, gndUnits, vddWUnits, gndWUnits;

        ReserveConfig(int layer, double vddReserved, double gndReserved,
                      FillGeneratorTool.Units vddUnits, FillGeneratorTool.Units gndUnits)
        {
            this.layer = layer;
            this.vddReserved = vddReserved;
            this.gndReserved = gndReserved;
            this.vddUnits = vddUnits;
            this.gndUnits = gndUnits;
            this.vddWUnits = FillGeneratorTool.Units.NONE;
            this.gndWUnits = FillGeneratorTool.Units.NONE;
        }

        public void reserveWidthOnLayer(double vddW, FillGeneratorTool.Units vddUnits,
                                        double gndW, FillGeneratorTool.Units gndUnits)
        {
            this.vddWidth = vddW;
            this.gndWidth = gndW;
            this.vddWUnits = vddUnits;
            this.gndWUnits = gndUnits;
        }
    }
}
