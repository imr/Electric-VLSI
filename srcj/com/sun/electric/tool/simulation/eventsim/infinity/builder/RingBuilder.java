package com.sun.electric.tool.simulation.eventsim.infinity.builder;

import com.sun.electric.tool.simulation.eventsim.core.classRegistry.ClassRegistry;
import com.sun.electric.tool.simulation.eventsim.handshakeNet.modelBuilder.ChannelRecord;
import com.sun.electric.tool.simulation.eventsim.handshakeNet.modelBuilder.ComponentRecord;
import com.sun.electric.tool.simulation.eventsim.handshakeNet.modelLoader.ModelDescription;
import com.sun.electric.tool.simulation.eventsim.infinity.components.Stage;

public class RingBuilder {

	public static final String STAGE= "Stage";
	public static final String CHANNEL= "Channel";
	
	static ClassRegistry classRegistry= ClassRegistry.getInstance();
	static ModelDescription modelDescr= ModelDescription.getInstance();
	
	public static void build() {
		
		final int ringLength= 5;
		
		ComponentRecord stage[]= new ComponentRecord[ringLength];
		// make records of stages
		for (int i= 0; i< ringLength; i++) {
			stage[i]= BuilderUtils.makeComponent(STAGE+"_"+i, Stage.class, null);
			modelDescr.componentNameMap.put(stage[i].name, stage[i]);
		}
		
		// make channels stage[i].OUT -> stage[(i+1)%ringLength].IN
		for (int i= 0; i<ringLength; i++) {
			ChannelRecord cr= BuilderUtils.makeChannel(CHANNEL+"_"+i, stage[i].name, Stage.OUT, stage[(i+1)%ringLength].name, Stage.IN);
			modelDescr.connectionRecordList.add(cr);
		}
		
		// initialize stages 
		
		for (int i=0; i<ringLength-2; i++) {
			BuilderUtils.initStage(stage[i], i, i+16);
		}
		
//		BuilderUtils.initStage(stage[0], 0, 7);
//		BuilderUtils.initStage(stage[1], 5, 31);


	}
	

	
} // class ringBuilder
