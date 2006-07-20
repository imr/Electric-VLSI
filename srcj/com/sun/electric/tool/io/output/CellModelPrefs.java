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
