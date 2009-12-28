package com.sun.electric.tool.simulation.eventsim.infinity.builder;

import com.sun.electric.tool.simulation.eventsim.core.classRegistry.ClassRegistry;
import com.sun.electric.tool.simulation.eventsim.handshakeNet.modelBuilder.ChannelRecord;
import com.sun.electric.tool.simulation.eventsim.handshakeNet.modelBuilder.ComponentRecord;
import com.sun.electric.tool.simulation.eventsim.handshakeNet.modelLoader.ModelDescription;
import com.sun.electric.tool.simulation.eventsim.infinity.components.Cross;
import com.sun.electric.tool.simulation.eventsim.infinity.components.Stage;

public class CrossTestBuilder {

	public static final String TOPSTAGE= "TopStage";
	public static final String BOTSTAGE= "BottomStage";
	public static final String CROSS= "Cross";
	public static final String TOPCHANNEL= "TopChannel";
	public static final String BOTTOMCHANNEL= "BottomChannel";
	
	static ClassRegistry classRegistry= ClassRegistry.getInstance();
	static ModelDescription modelDescr= ModelDescription.getInstance();
	
	public static void build() {
		
		final int topRingLength= 10;
		final int bottomRingLength= 15;
		
		// make records of stages - top ring
		ComponentRecord topStage[]= new ComponentRecord[topRingLength];
		for (int i= 0; i< topRingLength; i++) {
			topStage[i]= BuilderUtils.makeComponent(TOPSTAGE+"_"+i, Stage.class, null);
			modelDescr.componentNameMap.put(topStage[i].name, topStage[i]);
		}

		// make records of stages - bottom ring
		ComponentRecord bottomStage[]= new ComponentRecord[bottomRingLength];
		for (int i= 0; i< bottomRingLength; i++) {
			bottomStage[i]= BuilderUtils.makeComponent(BOTSTAGE+"_"+i, Stage.class, null);
			modelDescr.componentNameMap.put(bottomStage[i].name, bottomStage[i]);
		}
		
		ComponentRecord cross= BuilderUtils.makeComponent(CROSS, Cross.class, null); 
		modelDescr.componentNameMap.put(cross.name, cross);
		
		// connect the top ring
		for (int i= 0; i<topRingLength-1; i++) {
			ChannelRecord cr= BuilderUtils.makeChannel(TOPCHANNEL+"_"+i, TOPSTAGE+"_"+i, Stage.OUT, TOPSTAGE+"_"+(i+1), Stage.IN);
			modelDescr.connectionRecordList.add(cr);
		}
		
		// connect the bottom ring
		for (int i= 0; i<topRingLength-1; i++) {
			ChannelRecord cr= BuilderUtils.makeChannel(BOTTOMCHANNEL+"_"+i, BOTSTAGE+"_"+i, Stage.OUT, BOTSTAGE+"_"+(i+1), Stage.IN);
			modelDescr.connectionRecordList.add(cr);
		}
		
		// connect the cross
		ChannelRecord fromTop= BuilderUtils.makeChannel("ChannelFromTop", topStage[topRingLength-1].name, Stage.OUT, 
				cross.name, Cross.IN[0]);
		ChannelRecord toTop= BuilderUtils.makeChannel("ChannelToTop", cross.name, Cross.OUT[0], topStage[0].name, Stage.IN);
		ChannelRecord fromBottom= BuilderUtils.makeChannel("ChannelFromBottom", bottomStage[bottomRingLength-1].name, Stage.OUT, 
				cross.name, Cross.IN[1]);
		ChannelRecord toBottom= BuilderUtils.makeChannel("ChannelToBottom", cross.name, Cross.OUT[1], bottomStage[0].name, Stage.IN);
		modelDescr.connectionRecordList.add(fromTop);
		modelDescr.connectionRecordList.add(toTop);
		modelDescr.connectionRecordList.add(fromBottom);
		modelDescr.connectionRecordList.add(toBottom);

	
		// initialize stages - data, where to shift, what bits to shift etc
		
		
		
	} // build
	
} // class CrossTestBuilder
