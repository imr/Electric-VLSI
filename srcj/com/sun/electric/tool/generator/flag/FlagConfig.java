package com.sun.electric.tool.generator.flag;

import java.util.ArrayList;
import java.util.List;

import com.sun.electric.tool.generator.flag.scan.ScanChain;
import com.sun.electric.tool.generator.layout.TechType;
import com.sun.electric.tool.user.IconParameters;

public class FlagConfig {
	public TechType.TechTypeEnum techTypeEnum;
    public double m2PwrGndWid;
    public double m3PwrGndWid;
    public double m3PwrGndPitch;
    public double signalWid;
    public double trackPitch;
    public double pinHeight; 
    public double rowPitch;
    public double fillCellWidth;
    public double minM2Len;
    
    public List<ScanChain> chains = new ArrayList<ScanChain>();
    public IconParameters iconParameters = IconParameters.makeInstance(true);

}
