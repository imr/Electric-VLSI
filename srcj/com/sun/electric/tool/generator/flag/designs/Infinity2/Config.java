package com.sun.electric.tool.generator.flag.designs.Infinity2;

import com.sun.electric.tool.generator.flag.FlagConfig;
import com.sun.electric.tool.generator.flag.scan.ScanChain;
import com.sun.electric.tool.generator.layout.TechType;

public class Config extends FlagConfig {
	private Config() {
		tech = TechType.CMOS90;
	    m2PwrGndWid = 9;
	    m3PwrGndWid = 21;
	    m3PwrGndPitch = 132;
	    signalWid = 2.8;
	    trackPitch = 6;
		pinHeight = 12; 
		rowPitch = 144;
		fillCellWidth = 264;
		minM2Len = 10;

		chains.add(new ScanChain("si[1:9]", "so[1:9]", ""));
		chains.add(new ScanChain("cscanIn[1:9]", "cscanOut[1:9]", ""));
		chains.add(new ScanChain("rscni[1:9]", "rscnt[1:9]", ""));
	}
	public static Config CONFIG = new Config();
}
