package com.sun.electric.tool.generator.layout;

import com.sun.electric.database.text.Setting;
import com.sun.electric.tool.Tool;
import com.sun.electric.tool.ToolSettings;

/**
 * Constains project preferences for the gate layout generator
 */
public class GateLayGenSettings extends Tool {
	public static GateLayGenSettings tool = new GateLayGenSettings();
	// per-package namespace for preferences
//	private Pref.Group prefs = Pref.groupForPackage(this.getClass());

    /**
	 * The constructor sets up the DRC tool.
	 */
	private GateLayGenSettings()
	{
		super("GateLayoutGenerator", "GateLayoutGenerator");
	}

//    public static Tech.Type getFoundry() {
//        String tech = foundry.getString();
//        return Tech.Type.valueOf(tech);
//    }
//    public static String getFoundry() {return getFoundrySetting().getString();}
    public static Setting getFoundrySetting() { return ToolSettings.getFoundrySetting(); }

//    public static String getEnableNCC() { return getEnableNCCSetting().getString(); }
    public static Setting getEnableNCCSetting() {return ToolSettings.getEnableNCCSetting();}

//    public static int getSizeQuantizationError() {return getSizeQuantizationErrorSetting().getInt();}
    public static Setting getSizeQuantizationErrorSetting() { return ToolSettings.getSizeQuantizationErrorSetting(); }

//    public static int getMaxMosWidth() {return getMaxMosWidthSetting().getInt();}
    public static Setting getMaxMosWidthSetting() {return ToolSettings.getMaxMosWidthSetting();}

//    public static int getVddY() {return getVddYSetting().getInt();}
    public static Setting getVddYSetting() {return ToolSettings.getVddYSetting();}

//    public static int getGndY() {return getGndYSetting().getInt();}
    public static Setting getGndYSetting() {return ToolSettings.getGndYSetting();}

//    public static int getNmosWellHeight() {return getNmosWellHeightSetting().getInt();}
    public static Setting getNmosWellHeightSetting() {return ToolSettings.getNmosWellHeightSetting();}

//    public static int getPmosWellHeight() {return getPmosWellHeightSetting().getInt();}
    public static Setting getPmosWellHeightSetting() {return ToolSettings.getPmosWellHeightSetting();}

//    public static boolean getSimpleName() {return getSimpleNameSetting().getBoolean();}
    public static Setting getSimpleNameSetting() {return ToolSettings.getSimpleNameSetting();}
}
