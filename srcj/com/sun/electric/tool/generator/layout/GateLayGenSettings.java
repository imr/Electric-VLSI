package com.sun.electric.tool.generator.layout;

import com.sun.electric.database.text.Pref;
import com.sun.electric.database.text.Setting;
import com.sun.electric.tool.Tool;
import com.sun.electric.tool.user.projectSettings.ProjSettings;
import com.sun.electric.tool.user.projectSettings.ProjSettingsNode;

/**
 * Constains project settings for the gate layout generator
 */
public class GateLayGenSettings extends Tool {
	public static GateLayGenSettings tool = new GateLayGenSettings();
	// per-package namespace for preferences
	private Pref.Group prefs = Pref.groupForPackage(this.getClass());
    
    /**
	 * The constructor sets up the DRC tool.
	 */
	private GateLayGenSettings()
	{
		super("GateLayoutGenerator");
	}

    @Override
    public ProjSettingsNode getProjectSettings() {
        return ProjSettings.getSettings().getNode("GateLayoutGenerator");
    }
    
    private Setting cachefoundry;
    private Setting cacheenableNCC;
    private Setting cachequantError;
    private Setting cachemaxmos;
    private Setting cachevddy;
    private Setting cachegndy;
    private Setting cachenheight;
    private Setting cachepheight;
    private Setting cachesimpleName;
   
    @Override
    protected void initProjectSettings() {
        makeStringSetting ("foundry",    "Gate Layout Generator Tab", "Foundry", "MOCMOS");
        makeStringSetting ("enableNCC",  "Gate Layout Generator Tab", "Enable NCC checking of layout", "purpleFour");
        makeIntSetting    ("quantError", "Gate Layout Generator Tab", "Allowable quantization error", 0);
        makeIntSetting    ("maxmos",     "Gate Layout Generator Tab", "Maximum width of MOS transistors", 1000);
        makeIntSetting    ("vddy",       "Gate Layout Generator Tab", "Y coordinate of VDD bus", 21);
        makeIntSetting    ("gndy",       "Gate Layout Generator Tab", "Y coordinate of GND bus", -21);
        makeIntSetting    ("nheight",    "Gate Layout Generator Tab", "Height of Nwell", 84);
        makeIntSetting    ("pheight",    "Gate Layout Generator Tab", "Height of Pwell", 84);
        makeBooleanSetting("simpleName", "Gate Layout Generator Tab", "Name is gate type plus size", true);
    }

    public static void setFoundry(String val) { tool.cachefoundry.setString(val); }
//    public static Tech.Type getFoundry() {
//        String tech = foundry.getString();        
//        return Tech.Type.valueOf(tech);       
//    }   
    public static String getFoundry() {return tool.cachefoundry.getString();}   
    public static Setting getFoundrySetting() { return tool.cachefoundry; }
    
    public static void setEnableNCC(String val) {tool.cacheenableNCC.setString(val);}
    public static String getEnableNCC() { return tool.cacheenableNCC.getString(); }
    public static Setting getEnableNCCSetting() {return tool.cacheenableNCC;}
    
    public static void setSizeQuantizationError(int val) { 
    	tool.cachequantError.setInt(val); 
    }
    public static int getSizeQuantizationError() {return tool.cachequantError.getInt();}
    public static Setting getSizeQuantizationErrorSetting() { return tool.cachequantError; }
    
    public static void setMaxMosWidth(int val) {tool.cachemaxmos.setInt(val);}
    public static int getMaxMosWidth() {return tool.cachemaxmos.getInt();}
    public static Setting getMaxMosWidthSetting() {return tool.cachemaxmos;}
    
    public static void setVddY(int val) {tool.cachevddy.setInt(val);}
    public static int getVddY() {return tool.cachevddy.getInt();}
    public static Setting getVddYSetting() {return tool.cachevddy;}
    
    public static void setGndY(int val) {tool.cachegndy.setInt(val);}
    public static int getGndY() {return tool.cachegndy.getInt();}
    public static Setting getGndYSetting() {return tool.cachegndy;}
    
    public static void setNmosWellHeight(int val) {tool.cachenheight.setInt(val);}
    public static int getNmosWellHeight() {return tool.cachenheight.getInt();}
    public static Setting getNmosWellHeightSetting() {return tool.cachenheight;}
    
    public static void setPmosWellHeight(int val) {tool.cachepheight.setInt(val);}
    public static int getPmosWellHeight() {return tool.cachepheight.getInt();}
    public static Setting getPmosWellHeightSetting() {return tool.cachepheight;}
    
    public static void setSimpleName(boolean val) {tool.cachesimpleName.setBoolean(val);}
    public static boolean getSimpleName() {return tool.cachesimpleName.getBoolean();}
    public static Setting getSimpleNameSetting() {return tool.cachesimpleName;}
}
