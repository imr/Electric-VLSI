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
package com.sun.electric.tool.generator.layout.fill;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.sun.electric.technology.Technology;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.generator.layout.TechType;

/****************************** CONFIG ******************************/

public class FillGenConfig implements Serializable
{
    private TechType.TechTypeEnum techType = null;
    public FillGeneratorTool.FillTypeEnum fillType = FillGeneratorTool.FillTypeEnum.INVALID;
    public String fillLibName;
    List<ReserveConfig> reserves = new ArrayList<ReserveConfig>();
    public boolean evenLayersHorizontal;
    public double width, height, targetW, targetH, minTileSizeX, minTileSizeY;
    public ExportConfig perim;
    public int firstLayer, lastLayer;
    public int[] cellTiles;
    public boolean hierarchy;
    public double minOverlap;
    public double drcSpacingRule;
    public boolean binary;
    public boolean useMaster;
    public boolean onlyAround;
    public double gap; // allowed overlap between given cells and masters. Typical value is 1.5
    public FillGenType fillCellType = FillGenType.INTERNAL;
    public int level; // to control the level of hierarchy in case of onlyAround option
    public Job job;

    public enum FillGenType {
        INTERNAL(0), // uses internal router
        ONLYSKILL(1), // only generates the fill without connection
        SEAGATES(2); // uses sea-of-gates router for connection
        private final int mode;
        FillGenType(int m) { mode = m; }
        public int getMode() {return mode;}
        public static FillGenType find(int mode)
        {
            for (FillGenType m : FillGenType.values())
            {
                if (m.mode == mode) return m;
            }
            return INTERNAL; // It should not assume that is the default case
        }
    }

    public FillGenConfig(TechType.TechTypeEnum tech) { techType = tech; }

    public FillGenConfig(TechType.TechTypeEnum tech, FillGeneratorTool.FillTypeEnum type, String lib,
                         ExportConfig perim, int first, int last, double w, double h, boolean even,
                         int[] cellTiles, boolean hierarchy, double minO, double drcSpacingRule,
                         boolean binary, boolean useMaster, boolean onlyAround, double gap, FillGenType genType,
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

        techType = tech;

        this.fillLibName = lib;
        this.perim = perim;
        this.firstLayer = first;
        this.lastLayer = last;
        this.onlyAround = onlyAround;
        this.gap = gap; // only valid if onlyAround=true
        this.fillCellType = genType;
        this.level = level;
    }

    public TechType getTechType() { return techType.getTechType(); }
    public boolean is180Tech() { return techType == TechType.TechTypeEnum.MOCMOS || 
        techType == TechType.TechTypeEnum.TSMC180; }

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
        Job.error(layer<1 || layer>numMetals,
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
