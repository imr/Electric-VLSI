package com.sun.electric.tool.generator.flag.scan;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.tool.generator.flag.FlagConfig;
import com.sun.electric.tool.generator.flag.Utils;
import com.sun.electric.tool.generator.flag.router.Router;
import com.sun.electric.tool.generator.layout.LayoutLib;
import com.sun.electric.tool.generator.layout.TechType;

public class Scan {
	private final List<ScanChain> chains = new ArrayList<ScanChain>();
	private final Set<String> SCAN_PORT_NAME_SET = new HashSet<String>();
	private final FlagConfig config;
	
	private static void prln(String s) {Utils.prln(s);}
	private static void error(boolean cond, String msg) {Utils.error(cond, msg);}
	
	private TechType tech() {return config.techTypeEnum.getTechType();}

	private boolean vertOrHorizAligned(PortInst p1, PortInst p2) {
		double x1 = p1.getCenter().getX();
		double y1 = p1.getCenter().getY();
		double x2 = p2.getCenter().getX();
		double y2 = p2.getCenter().getY();
		return x1==x2 || y1==y2;
	}
	
	private void connectScanPorts(NodeInst niOut, NodeInst niIn, 
                                  List<String> outPortNm, 
                                  List<String> inPortNm,
                                  Router router) {
		for (int i=0; i<outPortNm.size(); i++) {
			PortInst piOut = niOut.findPortInst(outPortNm.get(i));
			PortInst piIn = niIn.findPortInst(inPortNm.get(i));
			if (!vertOrHorizAligned(piOut, piIn)) {
				prln("Can't connect scan ports with horizontal or vertical wire:");
				prln("    "+piOut+" "+piIn);
				continue;
			}
			
			LayoutLib.newArcInst(tech().m3(), config.signalWid, 
			           			 router.raiseToM3(piOut), 
			           			 router.raiseToM3(piIn));
			//prln("Scan chain connect: "+piOut+" to "+piIn);
		}
	}
	public Scan(List<ScanChain> chains, FlagConfig config) {
		this.chains.addAll(chains);
		this.config = config;
		for (ScanChain chain : chains) {
			SCAN_PORT_NAME_SET.addAll(chain.getInputNames());
			SCAN_PORT_NAME_SET.addAll(chain.getOutputNames());
			SCAN_PORT_NAME_SET.addAll(chain.getFeedthroughNames());
		}
	}

	public void stitchScanChains(List<NodeInst> layInsts, Router router) {
		for (ScanChain chain : chains) {
			NodeInst prevOut = null;
			List<String> prevOutPorts = null;
			for (NodeInst ni : layInsts) {
				List<String> inOrFeed = chain.getInputOrFeedNames(ni);
				if (inOrFeed!=null && prevOut!=null) {
					connectScanPorts(prevOut, ni, 
							         prevOutPorts, inOrFeed, router);
					prevOut = null;
				}
				List<String> newPrevOutPorts = chain.getOutputOrFeedNames(ni);
				if (newPrevOutPorts!=null) {
					if (prevOut!=null) {
						prln("Error: Dangling scan chain output. ");
						prln("  NodeInst: "+prevOut.getName());
						prln("  Port:     "+prevOutPorts.get(0));
					}
					prevOutPorts = newPrevOutPorts;
					prevOut = ni;
				}
			}
		}
	}
	public boolean isScan(PortInst pi) {
		String nm = pi.getPortProto().getName();
		return SCAN_PORT_NAME_SET.contains(nm);
	}
	


}
