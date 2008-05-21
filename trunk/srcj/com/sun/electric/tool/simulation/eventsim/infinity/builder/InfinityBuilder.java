package com.sun.electric.tool.simulation.eventsim.infinity.builder;

import com.sun.electric.tool.simulation.eventsim.core.classRegistry.ClassRegistry;
import com.sun.electric.tool.simulation.eventsim.handshakeNet.modelBuilder.ChannelRecord;
import com.sun.electric.tool.simulation.eventsim.handshakeNet.modelBuilder.ComponentRecord;
import com.sun.electric.tool.simulation.eventsim.handshakeNet.modelLoader.ModelDescription;
import com.sun.electric.tool.simulation.eventsim.infinity.components.Branch;
import com.sun.electric.tool.simulation.eventsim.infinity.components.Merge;
import com.sun.electric.tool.simulation.eventsim.infinity.components.Stage;

public class InfinityBuilder {

	public static final String LSTAGE= "LeftStage";
	public static final String RSTAGE= "RightStage";
	public static final String MSTAGE= "MiddleStage";
	public static final String MERGE= "Merge";
	public static final String BRANCH= "Branch";
	
	public static final String LCHANNEL= "LeftChannel";
	public static final String MCHANNEL= "MiddleChannel";
	public static final String RCHANNEL= "RightChannel";

	public static final String LTOMERGE= "LeftFifoToMergeChannel";
	public static final String RTOMERGE= "RightFifoToMergeChannel";
	public static final String MERGETOMID= "MergeToMiddleFifoChannel";
	public static final String MIDTOBRANCH= "MiddleFifoToBranchChannel";
	public static final String BRANCHTOL= "BranchToLeftFifoChannel";
	public static final String BRANCHTOR= "BranchToRightFifoChannel";
	
	static ClassRegistry classRegistry= ClassRegistry.getInstance();
	static ModelDescription modelDescr= ModelDescription.getInstance();
	
	public static void build() {
		
		final int leftFifoLen= 50;
		final int midFifoLen= 50;
		final int rightFifoLen= 50;
		
		// make records of stages - left fifo
		ComponentRecord leftStage[]= new ComponentRecord[leftFifoLen];
		for (int i= 0; i< leftFifoLen; i++) {
			leftStage[i]= BuilderUtils.makeComponent(LSTAGE+"_"+i, Stage.class, null);
			modelDescr.componentNameMap.put(leftStage[i].name, leftStage[i]);
		}
		
		// make records of stages - left fifo
		ComponentRecord midStage[]= new ComponentRecord[midFifoLen];
		for (int i= 0; i< midFifoLen; i++) {
			midStage[i]= BuilderUtils.makeComponent(MSTAGE+"_"+i, Stage.class, null);
			modelDescr.componentNameMap.put(midStage[i].name, midStage[i]);
		}
		
		// make records of stages - right fifo
		ComponentRecord rightStage[]= new ComponentRecord[rightFifoLen];
		for (int i= 0; i< rightFifoLen; i++) {
			rightStage[i]= BuilderUtils.makeComponent(RSTAGE+"_"+i, Stage.class, null);
			modelDescr.componentNameMap.put(rightStage[i].name, rightStage[i]);
		}
		
		ComponentRecord merge= BuilderUtils.makeComponent(MERGE, Merge.class, null);
		ComponentRecord branch= BuilderUtils.makeComponent(BRANCH, Branch.class, null);
		
		// connect the left fifo
		for (int i= 0; i<leftFifoLen-1; i++) {
			ChannelRecord cr= BuilderUtils.makeChannel(LCHANNEL+"_"+i, 
					leftStage[i].name, Stage.OUT, leftStage[i+1].name, Stage.IN);
			modelDescr.connectionRecordList.add(cr);
		}
		// connect the middle fifo
		for (int i= 0; i<midFifoLen-1; i++) {
			ChannelRecord cr= BuilderUtils.makeChannel(MCHANNEL+"_"+i, 
					midStage[i].name, Stage.OUT, midStage[i+1].name, Stage.IN);
			modelDescr.connectionRecordList.add(cr);
		}
		// connect the right fifo
		for (int i= 0; i<rightFifoLen-1; i++) {
			ChannelRecord cr= BuilderUtils.makeChannel(RCHANNEL+"_"+i, 
					rightStage[i].name, Stage.OUT, rightStage[i+1].name, Stage.IN);
			modelDescr.connectionRecordList.add(cr);
		}
		
		// connect the merge
		ChannelRecord lToMerge= BuilderUtils.makeChannel(LTOMERGE, 
				leftStage[leftFifoLen-1].name, Stage.OUT, merge.name, Merge.IN_0);
		ChannelRecord rToMerge= BuilderUtils.makeChannel(RTOMERGE, 
				rightStage[rightFifoLen-1].name, Stage.OUT, merge.name, Merge.IN_1);
		ChannelRecord mergeToMid= BuilderUtils.makeChannel(MERGETOMID, 
				merge.name, Merge.OUT, midStage[0].name, Stage.IN);
		ChannelRecord midToBranch= BuilderUtils.makeChannel(MIDTOBRANCH, 
				midStage[midFifoLen-1].name, Stage.OUT, branch.name, Branch.IN);
		ChannelRecord branchToL= BuilderUtils.makeChannel(BRANCHTOL, 
				branch.name, Branch.OUT_0, leftStage[0].name, Stage.IN);
		ChannelRecord branchToR= BuilderUtils.makeChannel(BRANCHTOR, 
				branch.name, Branch.OUT_1, rightStage[0].name, Stage.IN);
		
		modelDescr.connectionRecordList.add(lToMerge);
		modelDescr.connectionRecordList.add(rToMerge);
		modelDescr.connectionRecordList.add(mergeToMid);
		modelDescr.connectionRecordList.add(midToBranch);
		modelDescr.connectionRecordList.add(branchToL);
		modelDescr.connectionRecordList.add(branchToR);
		
		// initialize stages - data, where to shift, what bits to use to shift etc
		
		
	} // build
	
} // class InfinityBuilder
