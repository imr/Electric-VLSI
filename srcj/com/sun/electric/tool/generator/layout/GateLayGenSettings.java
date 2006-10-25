package com.sun.electric.tool.generator.layout;

import com.sun.electric.database.text.Pref;
import com.sun.electric.tool.logicaleffort.LETool;
import com.sun.electric.tool.ncc.NccPreferences;
import com.sun.electric.tool.user.projectSettings.ProjSettings;

/**
 * Constains project settings for the gate layout generator
 */
public class GateLayGenSettings {
	public static GateLayGenSettings tool = new GateLayGenSettings();
	// per-package namespace for preferences
	private Pref.Group prefs = Pref.groupForPackage(this.getClass());

    private static Pref foundry = Pref.makeStringSetting(
    		"foundry", tool.prefs, tool,
		    ProjSettings.getSettings().getNode("GateLayoutGenerator"),
		    null,
            "Gate Layout Generator Tab", 
            "Foundry",
			"MOCMOS");
    public static void setFoundry(String val) { foundry.setString(val); }
//    public static Tech.Type getFoundry() {
//        String tech = foundry.getString();        
//        return Tech.Type.valueOf(tech);       
//    }   
    public static String getFoundry() {return foundry.getString();}   
    
    private static Pref enableNCC = Pref.makeStringSetting(
    		"enableNCC", tool.prefs, tool, 
		    ProjSettings.getSettings().getNode("GateLayoutGenerator"),
		    null,
            "Gate Layout Generator Tab", 
            "Enable NCC checking of layout",
            "purpleFour");
    public static void setEnableNCC(String val) {enableNCC.setString(val);}
    public static String getEnableNCC() { return enableNCC.getString(); }
    
    private static Pref quantError = Pref.makeIntSetting(
    		"quantError", tool.prefs, tool,
		    ProjSettings.getSettings().getNode("GateLayoutGenerator"),
		    null,
            "Gate Layout Generator Tab", 
            "Allowable quantization error",
    		0);
    public static void setSizeQuantizationError(int val) { 
    	quantError.setInt(val); 
    }
    public static int getSizeQuantizationError() {return quantError.getInt();}
    
    private static Pref maxMosWidth = Pref.makeIntSetting(
    		"maxmos", tool.prefs, tool, 
		    ProjSettings.getSettings().getNode("GateLayoutGenerator"),
		    null,
            "Gate Layout Generator Tab", 
            "Maximum width of MOS transistors",
    		1000);
    public static void setMaxMosWidth(int val) {maxMosWidth.setInt(val);}
    public static int getMaxMosWidth() {return maxMosWidth.getInt();}
    
    private static Pref vddY = Pref.makeIntSetting(
    		"vddy", tool.prefs, tool,
		    ProjSettings.getSettings().getNode("GateLayoutGenerator"),
		    null,
            "Gate Layout Generator Tab", 
            "Y coordinate of VDD bus",
    		21);
    public static void setVddY(int val) {vddY.setInt(val);}
    public static int getVddY() {return vddY.getInt();}
    
    private static Pref gndY = Pref.makeIntSetting(
    		"gndy", tool.prefs, tool,
		    ProjSettings.getSettings().getNode("GateLayoutGenerator"),
		    null,
            "Gate Layout Generator Tab", 
            "Y coordinate of GND bus",
    		-21);
    public static void setGndY(int val) {gndY.setInt(val);}
    public static int getGndY() {return gndY.getInt();}
    
    private static Pref nwellHeight = Pref.makeIntSetting(
    		"nheight", tool.prefs, tool, 
		    ProjSettings.getSettings().getNode("GateLayoutGenerator"),
		    null,
            "Gate Layout Generator Tab", 
            "Height of Nwell",
    		84);
    public static void setNmosWellHeight(int val) {nwellHeight.setInt(val);}
    public static int getNmosWellHeight() {return nwellHeight.getInt();}
    
    private static Pref pwellHeight = Pref.makeIntSetting(
    		"pheight", tool.prefs, tool,
		    ProjSettings.getSettings().getNode("GateLayoutGenerator"),
		    null,
            "Gate Layout Generator Tab", 
            "Height of Pwell",
    		84);
    public static void setPmosWellHeight(int val) {pwellHeight.setInt(val);}
    public static int getPmosWellHeight() {return pwellHeight.getInt();}
    
    private static Pref simpleName = Pref.makeBooleanSetting(
    		"simpleName", tool.prefs, tool, 
		    ProjSettings.getSettings().getNode("GateLayoutGenerator"),
		    null,
            "Gate Layout Generator Tab", 
            "Name is gate type plus size",
    		true);
    public static void setSimpleName(boolean val) {simpleName.setBoolean(val);}
    public static boolean getSimpleName() {return simpleName.getBoolean();}
    
    


}
