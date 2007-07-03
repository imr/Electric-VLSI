package com.sun.electric.tool.simulation.eventsim.infinity.builder;

import com.sun.electric.tool.simulation.eventsim.core.common.Parameters;
import com.sun.electric.tool.simulation.eventsim.handshakeNet.modelBuilder.ChannelRecord;
import com.sun.electric.tool.simulation.eventsim.handshakeNet.modelBuilder.ComponentRecord;
import com.sun.electric.tool.simulation.eventsim.handshakeNet.modelBuilder.ConnectionPoint;
import com.sun.electric.tool.simulation.eventsim.infinity.components.Stage;

public class BuilderUtils {
	/**
	 * Make a component record
	 * @param name component name
	 * @param t component type
	 * @param param parameters for the component
	 * @return
	 */
	static ComponentRecord makeComponent(String name, Class t, Parameters param) {
		ComponentRecord cr= new ComponentRecord();
		cr.name= name;
		cr.type= t;
		cr.typeName= null;
		cr.attributes= (param == null)? new Parameters() : param;
		return cr;
	} // makeComponent
	

	
	/**
	 * make a ChannelRecord for a point to point channel
	 * @param fromCmp source component name
	 * @param fromPort port on the source component
	 * @param toCmp destination component name
	 * @param toPort port on the destination component
	 * @return
	 */
	static ChannelRecord makeChannel(String name, String fromCmp, String fromPort, 
			String toCmp, String toPort) {
		// make a channel record
		ChannelRecord cr= new ChannelRecord();
		cr.name= name;
		// source connection point
		ConnectionPoint from= new ConnectionPoint();
		from.componentName= fromCmp;
		from.terminalName= fromPort;
		// destination connection point
		ConnectionPoint to= new ConnectionPoint();
		to.componentName= toCmp;
		to.terminalName= toPort;
		// add connection points to the channel
		cr.sources.add(from);
		cr.destinations.add(to);
		// here is the channel
		return cr;
	} // makeChannel
	
	
	static void initStage(ComponentRecord stage, int data, int address) {
		if (stage.attributes == null) stage.attributes= new Parameters();
		stage.attributes.add(Stage.INITIALLY_FULL, "true");
		stage.attributes.add(Stage.INITIAL_DATA, Integer.toString(data));
		stage.attributes.add(Stage.INITIAL_ADDRESS, Integer.toString(address));
	}
	
} // class BuilderUtils
