/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: CellModelPrefs.java
 *
 * Copyright (c) 2005 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.io.output;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.EDatabase;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.text.Pref;
import com.sun.electric.tool.io.FileType;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * User: gainsley
 * Date: Jul 17, 2006
 */
public class CellModelPrefs {

    public static final CellModelPrefs spiceModelPrefs = new CellModelPrefs("Spice", FileType.SPICE, true);
    public static final CellModelPrefs verilogModelPrefs = new CellModelPrefs("Verilog", FileType.VERILOG, false);

    private final String type;
    private final FileType fileType;
    private final boolean canLayoutFromNetlist;
    private Map<String,Pref> modelFilePrefs;

    private CellModelPrefs(String type, FileType fileType, boolean canLayoutFromNetlist) {
        this.type = type;
        this.fileType = fileType;
        this.canLayoutFromNetlist = canLayoutFromNetlist;
        this.modelFilePrefs = new HashMap<String,Pref>();
    }

    public String getType() {
        return type;
    }
    public FileType getFileType() {
        return fileType;
    }
    public boolean isCanLayoutFromNetlist() { return canLayoutFromNetlist; }

    public boolean isUseModelFromFile(Cell cell) {
        return isUseModelFromFile(getModelFileUnfiltered(cell));
    }

    public static boolean isUseModelFromFile(String unfilteredFileName) {
        return unfilteredFileName != null &&  unfilteredFileName.length() > 0 &&
                !unfilteredFileName.startsWith("-----") && !unfilteredFileName.startsWith("+++++");
    }

    public boolean isUseLayoutView(Cell cell) {
        return isUseLayoutView(getModelFileUnfiltered(cell));
    }

    public static boolean isUseLayoutView(String unfilteredFileName) {
        return unfilteredFileName != null && unfilteredFileName.length() > 0 &&
                unfilteredFileName.startsWith("+++++");
    }

    public String getModelFile(Cell cell) {
        return getModelFile(getModelFileUnfiltered(cell));
    }

    public static String getModelFile(String unfilteredFileName) {
        if (unfilteredFileName != null && unfilteredFileName.length() > 0 &&
            (unfilteredFileName.startsWith("+++++") || unfilteredFileName.startsWith("-----")))
            return unfilteredFileName.substring(5);
        return unfilteredFileName;
    }

    public void setModelFile(Cell cell, String fileName, boolean useModelFromFile, boolean useLayoutView) {
        assert !(useModelFromFile && useLayoutView);
        String prefName = type + "ModelFileFor_" + cell.getLibrary().getName() +"_" + cell.getName() + "_" + cell.getView().getAbbreviation();
        Pref modelPref = modelFilePrefs.get(prefName);
        if (modelPref == null)
        {
            modelPref = Pref.makeStringPref(prefName, cell.getLibrary().getPrefs(), "");
            modelFilePrefs.put(prefName, modelPref);
        }
        if (!useModelFromFile && useLayoutView)
            fileName = "+++++" + fileName;
        if (!useModelFromFile && !useLayoutView)
            fileName = "-----" + fileName;
        modelPref.setString(fileName);
    }

    public Map<Cell,String> getUnfilteredFileNames(EDatabase database) {
        HashMap<Cell,String> m = new HashMap<Cell,String>();
        for (Iterator<Library> lit = database.getLibraries(); lit.hasNext(); ) {
            Library lib = lit.next();
            for (Iterator<Cell> cit = lib.getCells(); cit.hasNext(); ) {
                Cell cell= cit.next();
                String prefName = type + "ModelFileFor_" + cell.getLibrary().getName() +"_" + cell.getName() + "_" + cell.getView().getAbbreviation();
                Pref modelPref = modelFilePrefs.get(prefName);
                if (modelPref == null) continue;
                String unfilterdFileName = modelPref.getString();
                if (unfilterdFileName.length() == 0) continue;
                m.put(cell, unfilterdFileName);
            }
        }
        return m;
    }

    private String getModelFileUnfiltered(Cell cell) {
        String prefName = type + "ModelFileFor_" + cell.getLibrary().getName() +"_" + cell.getName() + "_" + cell.getView().getAbbreviation();
        Pref modelPref = modelFilePrefs.get(prefName);
        if (modelPref == null)
        {
            modelPref = Pref.makeStringPref(prefName, cell.getLibrary().getPrefs(), "");
            modelFilePrefs.put(prefName, modelPref);
        }
        return modelPref.getString();
    }

    public void factoryResetModelFile(Cell cell) {
        String prefName = type + "ModelFileFor_" + cell.getLibrary().getName() +"_" + cell.getName() + "_" + cell.getView().getAbbreviation();
        Pref modelPref = modelFilePrefs.get(prefName);
        if (modelPref == null) return;
        if (!modelPref.getStringFactoryValue().equals(modelPref.getString()))
        	modelPref.setString(modelPref.getStringFactoryValue());
    }

}
