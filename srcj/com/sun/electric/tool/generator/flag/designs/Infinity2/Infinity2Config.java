package com.sun.electric.tool.generator.flag.designs.Infinity2;

import com.sun.electric.tool.generator.flag.FlagConfig;
import com.sun.electric.tool.generator.flag.scan.ScanChain;
import com.sun.electric.tool.generator.layout.TechType;

public class Infinity2Config extends FlagConfig {
	private Infinity2Config() {
		techTypeEnum = TechType.TechTypeEnum.CMOS90;
	    m2PwrGndWid = 9;
	    m3PwrGndWid = 21;
	    m3PwrGndPitch = 132;
	    signalWid = 2.8;
	    trackPitch = 6;
		pinHeight = 12; 
		rowPitch = 144;
		fillCellWidth = 264;
		minM2Len = 10;

		chains.add(new ScanChain("sid[1:9]", "sod[1:9]", ""));
		chains.add(new ScanChain("sic[1:9]", "soc[1:9]", ""));
		chains.add(new ScanChain("sir[1:9]", "sor[1:9]", ""));
	}
	public static Infinity2Config CONFIG = new Infinity2Config();
}
