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
package com.sun.electric.tool.io.output;

import com.sun.electric.database.text.Pref;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.tool.io.FileType;

import java.util.HashMap;

/**
 * Created by IntelliJ IDEA.
 * User: gainsley
 * Date: Jul 17, 2006
 * Time: 1:57:53 PM
 * To change this template use File | Settings | File Templates.
 */
public class CellModelPrefs {

    public static final CellModelPrefs spiceModelPrefs = new CellModelPrefs("Spice", FileType.SPICE, true);
    public static final CellModelPrefs verilogModelPrefs = new CellModelPrefs("Verilog", FileType.VERILOG, false);

    private final String type;
    private final FileType fileType;
    private final boolean canLayoutFromNetlist;
    private HashMap<String,Pref> modelFilePrefs;

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
        String fileName = getModelFileUnfiltered(cell);
        if (fileName.length() > 0 && !fileName.startsWith("-----") && !fileName.startsWith("+++++"))
            return true;
        return false;
    }

    public boolean isUseLayoutView(Cell cell) {
        String fileName = getModelFileUnfiltered(cell);
        if (fileName.length() > 0 && fileName.startsWith("+++++"))
            return true;
        return false;
    }

    public String getModelFile(Cell cell) {
        String fileName = getModelFileUnfiltered(cell);
        if (fileName.length() > 0 &&
            (fileName.startsWith("+++++") || fileName.startsWith("-----")))
            return fileName.substring(5);
        return fileName;
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

}
