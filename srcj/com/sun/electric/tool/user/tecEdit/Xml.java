/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Xml.java
 *
 * Copyright (c) 2007 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.user.tecEdit;

import com.sun.electric.database.geometry.EGraphics;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.Technology;
import java.awt.Color;
import java.io.PrintStream;
import java.util.Calendar;
import java.util.Date;

/**
 * This class generates Xml description from information in Info classes.
 */
public class Xml {
    
    /** Creates a new instance of Xml */
    private Xml() {
    }
    
    static void writeXml(PrintStream out, Technology tech, GeneralInfo gi, LayerInfo[] lList, ArcInfo[] aList, NodeInfo nist) {
        String techName = tech.getTechName();
        out.println("---------------------------------------");
        out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        out.println();
        out.println("<!--");
		out.println(" *");
		out.println(" * Electric(tm) VLSI Design System");
		out.println(" *");
		out.println(" * File: " + techName + ".xml");
		out.println(" * " + techName + " technology description");
		out.println(" * Generated automatically from a library");
		out.println(" *");
		Calendar cal = Calendar.getInstance();
		cal.setTime(new Date());
		out.println(" * Copyright (c) " + cal.get(Calendar.YEAR) + " Sun Microsystems and Static Free Software");
		out.println(" *");
		out.println(" * Electric(tm) is free software; you can redistribute it and/or modify");
		out.println(" * it under the terms of the GNU General Public License as published by");
		out.println(" * the Free Software Foundation; either version 2 of the License, or");
		out.println(" * (at your option) any later version.");
		out.println(" *");
		out.println(" * Electric(tm) is distributed in the hope that it will be useful,");
		out.println(" * but WITHOUT ANY WARRANTY; without even the implied warranty of");
		out.println(" * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the");
		out.println(" * GNU General Public License for more details.");
		out.println(" *");
		out.println(" * You should have received a copy of the GNU General Public License");
		out.println(" * along with Electric(tm); see the file COPYING.  If not, write to");
		out.println(" * the Free Software Foundation, Inc., 59 Temple Place, Suite 330,");
		out.println(" * Boston, Mass 02111-1307, USA.");
		out.println(" */");
        out.println("-->");
        out.println();
        out.println("<!DOCTYPE Technology SYSTEM \"Technology.dtd\">");
        out.println();
        out.println("<Technology version=\"0\"");
        printlnAttribute(out, "  name", techName);
        if (tech.getTechShortName() != null)
            printlnAttribute(out, "  shortName", gi.shortName);
        printlnAttribute(out, "  defaultFoundry", gi.defaultFoundry);
        
        printAttribute(out, "  minNumMetals", gi.defaultNumMetals);
        printAttribute(out, " maxNumMetals", gi.defaultNumMetals);
        printlnAttribute(out, " defaultNumMetals", gi.defaultNumMetals);
        
        printAttribute(out, "  minResistance", gi.minRes);
        printlnAttribute(out, " minCapacitance", gi.minCap);
        
        printlnAttribute(out, "  gateLengthSubtraction", gi.gateShrinkage);
        printlnAttribute(out, "  gateInclusion", gi.includeGateInResistance);
        printlnAttribute(out, "  groundNetInclusion", gi.includeGround);
        
        out.println("  >");
        out.println();
        
        
        if (gi.transparentColors != null) {
            out.println("  <!-- Transparent layers -->");
            for (int i = 0; i < gi.transparentColors.length; i++) {
                Color color = gi.transparentColors[i];
                out.print("  <TransparentLayer");
                printAttribute(out, " i", i + 1);
                printAttribute(out, " r", color.getRed());
                printAttribute(out, " g", color.getGreen());
                printAttribute(out, " b", color.getBlue());
                out.println("/>");
            }
            out.println();
        }
        
        for (LayerInfo li: lList) {
            writeXml(out, li);
            out.println();
        }
        out.println();
        
        for (ArcInfo ai: aList) {
            writeXml(out, ai);
            out.println();
        }
        
        out.println("</Technology>");
        out.println("---------------------------------------");
    }
    
    private static void writeXml(PrintStream out, LayerInfo li) {
        EGraphics desc = li.desc;
        out.print("  <Layer");
        printAttribute(out, " name", li.name);
        printAttribute(out, " fun", li.fun.name());
        int funExtra = li.funExtra;
        if (funExtra != 0) {
            String funString;
            if ((funExtra&Layer.DEPLETION) != 0 || (funExtra&Layer.ENHANCEMENT) != 0) {
                funString = Layer.Function.getExtraConstantName(funExtra&(Layer.DEPLETION|Layer.ENHANCEMENT));
                funExtra &= ~(Layer.DEPLETION|Layer.ENHANCEMENT);
                if (funExtra != 0)
                    funString += "_" + Layer.Function.getExtraConstantName(funExtra);
            } else {
                funString = Layer.Function.getExtraConstantName(funExtra);
            }
            printAttribute(out, " extraFun", funString);
        }
        out.println();
        printAttribute(out, "    patternedOnDisplay", desc.isPatternedOnDisplay());
        printAttribute(out, " patternedOnPrinter", desc.isPatternedOnPrinter());
        if (li.desc.getOutlined() != null)
            printAttribute(out, " outlined", desc.getOutlined().getConstName());
        out.println(">");
        
        if (desc.getTransparentLayer() > 0) {
            out.print("      <TransparentColor");
            printAttribute(out, " transparent", desc.getTransparentLayer());
        } else {
            out.print("      <OpaqueColor");
            Color color = desc.getColor();
            printAttribute(out, " r", color.getRed());
            printAttribute(out, " g", color.getGreen());
            printAttribute(out, " b", color.getBlue());
        }
        out.println("/>");
        
        boolean hasPattern = false;
        int [] pattern = li.desc.getPattern();
        for(int j=0; j<16; j++) if (pattern[j] != 0) hasPattern = true;
        if (hasPattern) {
            for(int j=0; j<16; j++) {
                out.print("      <PatternLine line=\"");
                for(int k=0; k<16; k++)
                    out.print((pattern[j] & (1 << (15-k))) != 0 ? 'X' : ' ');
                out.println("\"/>");
            }
        }
        
        // write the 3D information
        if (li.thick3d != 0 || li.height3d != 0) {
            out.print("      <Layer3D");
            printAttribute(out, " thick", li.thick3d);
            printAttribute(out, " height", li.height3d);
            out.println("/>");
        }
        
        if (li.cif != null && li.cif.length() > 0) {
            out.print("      <CifLayer");
            printAttribute(out, " cif", li.cif);
            out.println("/>");
        }
        
        // write the SPICE information
        if (li.spiRes != 0 || li.spiCap != 0 || li.spiECap != 0) {
            out.print("      <Parasitics");
            printAttribute(out, " resistance", li.spiRes);
            printAttribute(out, " capacitance", li.spiCap);
            printAttribute(out, " edgeCapacitance", li.spiECap);
            out.println("/>");
        }
        out.println("  </Layer>");
    }
    
    private static void writeXml(PrintStream out, ArcInfo ai) {
        out.print("  <ArcProto");
        printAttribute(out, " name", ai.name);
        printlnAttribute(out, " fun", ai.func.getConstantName());
        printlnAttribute(out, "    widthOffset", ai.widthOffset);
        printAttribute(out, "    defaultWidth", ai.maxWidth);
        out.println(">");
        for (ArcInfo.LayerDetails al: ai.arcDetails) {
            out.print("    <ArcLayer");
            printAttribute(out, " layer", al.layer.name);
            printAttribute(out, " widthOffset", al.width);
            printAttribute(out, " style", al.style == Poly.Type.FILLED ? "FILLED" : "CLOSED");
            out.println("/>");
        }
        out.println("  </ArcProto>");
    }
    
    private static void printlnAttribute(PrintStream out, String name, Object value) {
        out.println(name + "=\"" + value + "\"");
    }
    
    private static void printAttribute(PrintStream out, String name, Object value) {
        out.print(name + "=\"" + value + "\"");
    }
}
