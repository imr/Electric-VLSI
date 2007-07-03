package com.sun.electric.tool.simulation.eventsim.infinity.simulator;

import java.lang.reflect.Method;
import java.util.Iterator;

import com.sun.electric.tool.simulation.eventsim.core.classRegistry.ClassRegistry;
import com.sun.electric.tool.simulation.eventsim.core.classRegistry.ComponentInfo;
import com.sun.electric.tool.simulation.eventsim.core.engine.Director;
import com.sun.electric.tool.simulation.eventsim.core.engine.EventSimErrorException;
import com.sun.electric.tool.simulation.eventsim.core.globals.Globals;
import com.sun.electric.tool.simulation.eventsim.core.hierarchy.Entities;
import com.sun.electric.tool.simulation.eventsim.core.hierarchy.Entity;
import com.sun.electric.tool.simulation.eventsim.core.simulation.Delay;
import com.sun.electric.tool.simulation.eventsim.handshakeNet.modelBuilder.HandshakeNetModelBuilder;
import com.sun.electric.tool.simulation.eventsim.handshakeNet.modelBuilder.ModelNotBuiltException;
import com.sun.electric.tool.simulation.eventsim.infinity.builder.CrossTestBuilder;
import com.sun.electric.tool.simulation.eventsim.infinity.builder.RingBuilder;
import com.sun.electric.tool.simulation.eventsim.infinity.components.Stage;

public class Simulator {

	static Director d= Director.getInstance();
	
	public static void main(String[] args) {
		System.out.println("start");
		try {
			Globals globals= Globals.getInstance();
			if (args.length == 1) {
				globals.load(args[0]);
			}
			
			Simulator sim= new Simulator();
			sim.buildClassRegistry();
			

			// RingBuilder.build();
			CrossTestBuilder.build();
			sim.buildHandshakeModel();
			

			long start= System.currentTimeMillis();
			
			d.reset();

			sim.printInfo();
			System.out.println();
			
//			for (int i=0; i< 20; i++) {
				long count= d.run(new Delay(50));
//				long count= d.step(40);
				System.out.println("Simulated events: " + count);
				sim.printInfo();
//				System.out.println();
//			for (int i=0; i<100; i++) {
//				long count= d.step(1);
//				System.out.println("Simulated events: " + count + ", time= " + d.getTime());
//				sim.printInfo();
//				System.out.println();
//			}
//			}
			
			long stop= System.currentTimeMillis();
			
			
		}
		catch (Exception e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
		System.out.println("done");
	}
	
	void buildClassRegistry() {
		ClassRegistry classRegistry= ClassRegistry.getInstance();
		
		ComponentInfo ci= new ComponentInfo();
		ci.className= "Stage";
		ci.setClass(com.sun.electric.tool.simulation.eventsim.infinity.components.Stage.class);
		ci.addInput("In");
		ci.addOutput("Out");
		classRegistry.register(ci.getName(), ci);
		
		ci= new ComponentInfo();
		ci.className= "Branch";
		ci.setClass(com.sun.electric.tool.simulation.eventsim.infinity.components.Branch.class);
		ci.addInput("In");
		ci.addOutput("Out0");
		ci.addOutput("Out1");
		classRegistry.register(ci.getName(), ci);

		ci= new ComponentInfo();
		ci.className= "Merge";
		ci.setClass(com.sun.electric.tool.simulation.eventsim.infinity.components.Merge.class);
		ci.addInput("In0");
		ci.addInput("In1");
		ci.addOutput("Out");
		classRegistry.register(ci.getName(), ci);

		ci= new ComponentInfo();
		ci.className= "Cross";
		ci.setClass(com.sun.electric.tool.simulation.eventsim.infinity.components.Cross.class);
		ci.addInput("In0");
		ci.addInput("In1");
		ci.addOutput("Out0");
		ci.addOutput("Out1");
		classRegistry.register(ci.getName(), ci);
		
	} // buildClassregistry
	
	void buildHandshakeModel() throws ModelNotBuiltException, EventSimErrorException {
		HandshakeNetModelBuilder modelBuilder= new HandshakeNetModelBuilder();
		modelBuilder.buildModel();
	}
	
	void printInfo() throws Exception {
		Iterator<Entity> ei= Entities.iterator();
		while (ei.hasNext() ) {
			Entity e= ei.next();
			Method m= null;
			try {
				m= e.getClass().getMethod("getInfo", null);
			}
			catch (SecurityException se) {
				d.fatalError("Runner caught security exception when inspecting class "
						+ e.getClass().getName());
			}
			catch (NoSuchMethodException nme) {
				m= null;
			}
			if (m != null) {
				try {
					// get status
					Object so= m.invoke(e, null );
					// print status
					System.out.println(so.toString());
				}
				catch (Exception ie) {
					d.fatalError("Exception caught when invoking "
							+ "method "
							+ m.getName() 
							+ " on object" + e
							+ ". Exception message= "
							+ ie.getMessage());
				}
				
			}
		}
	} // printInfo
	
	void printStageInfo() {
		Iterator<Entity> ei= Entities.iterator();
		while (ei.hasNext() ) {
			Entity e= ei.next();
			if (e instanceof Stage) {
				Stage s= (Stage)e;
				s.getInfo();
			}
		}
	} // printStageInfo
	
} // Simulator
