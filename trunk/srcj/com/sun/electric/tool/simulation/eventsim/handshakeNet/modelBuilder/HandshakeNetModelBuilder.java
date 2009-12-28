/*
 * Created on May 18, 2005
 *
 */
package com.sun.electric.tool.simulation.eventsim.handshakeNet.modelBuilder;

import java.lang.reflect.Constructor;
import java.util.Iterator;

import com.sun.electric.tool.simulation.eventsim.core.classRegistry.ClassRegistry;
import com.sun.electric.tool.simulation.eventsim.core.common.Parameters;
import com.sun.electric.tool.simulation.eventsim.core.engine.Director;
import com.sun.electric.tool.simulation.eventsim.core.engine.EventSimErrorException;
import com.sun.electric.tool.simulation.eventsim.core.globals.Globals;
import com.sun.electric.tool.simulation.eventsim.core.hierarchy.CompositeEntity;
import com.sun.electric.tool.simulation.eventsim.handshakeNet.channel.Channel;
import com.sun.electric.tool.simulation.eventsim.handshakeNet.channel.LazyAckChannel;
import com.sun.electric.tool.simulation.eventsim.handshakeNet.channel.RendezvousChannel;
import com.sun.electric.tool.simulation.eventsim.handshakeNet.component.Component;
import com.sun.electric.tool.simulation.eventsim.handshakeNet.modelLoader.ModelDescription;
import com.sun.electric.tool.simulation.eventsim.handshakeNet.terminal.InputTerminal;
import com.sun.electric.tool.simulation.eventsim.handshakeNet.terminal.OutputTerminal;


/**
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All rights reserved. Use is subject to license terms.
 * 
 * @author ib27688
 * 
 * Build a GASP model from a model description.
 */
public class HandshakeNetModelBuilder {
	
	protected Globals globals= Globals.getInstance();

	ModelDescription modDes= ModelDescription.getInstance();
	ClassRegistry classRegistry= ClassRegistry.getInstance();
	Director director= Director.getInstance();
	
	public HandshakeNetModelBuilder() {
	} // ModelBuilder constructor

	/** 
	 * Build components and connections from the model description
	 */
	public CompositeEntity buildModel() throws ModelNotBuiltException, EventSimErrorException {
		// use model name for the "top" group
		CompositeEntity modelGroup= new CompositeEntity(modDes.name);
		
		// build components
		Iterator ci= modDes.componentNameMap.values().iterator();
		while (ci.hasNext()) {
			ComponentRecord cr= (ComponentRecord)ci.next();
			
			// Was the component type established?
			if (cr.type == null) {
				// not established, first find the component type
				Class c= classRegistry.getComponentType(cr.typeName);
				if (c != null) {
					cr.type= c;
				}
				else {
					// component type has not been established in the
					// registry - do we know of the type?
					Class newClass= null;
					try {
						newClass= Class.forName(cr.typeName);
					}
					catch (ClassNotFoundException e) {
						director.fatalError("Class not found, model not built: " + cr.typeName);
					}
					if (!(Component.class.isAssignableFrom(newClass))) {
						// OK, the new class extends a component
						cr.type= newClass;
						cr.typeName= newClass.getName();
					}
					else {
						director.fatalError("Class " + newClass.getName()
								+ " not an instance of " + Component.class.getName());
					}
				}
			}
			
			// get a constructor	
			try {
				
				// requires existence of a two parameter constructor
				Constructor ctor= cr.type.getDeclaredConstructor(
						new Class[] {String.class, Parameters.class});
				
				// System.out.println(cr.type.getName() + ": constructor found");
				// System.out.println(cr.attributes);
				
				cr.component= (Component)ctor.newInstance(
						new Object[] {cr.name, cr.attributes});					

				// System.out.println(cr.type.getName() + ": object made");

				
				modelGroup.addMember(cr.component);
			}
			catch (Exception e) {
				System.err.println(e.getClass().getName());
				e.printStackTrace();
				// System.out.println(e.getMessage());
				throw new ModelNotBuiltException("Error: Component "
						+ cr.name + " of type "
						+ cr.type.getName() 
						+ " cannot be built with a constructor that"
						+ " takes String and Parameters as arguments");
			} // catch
		} // while ci has next
		
		// now build connections
		
		// start by figuring out what to log
		Boolean lds= globals.booleanValue(BuildDefaults.LOG_CHANNEL);
		boolean logChannel= (lds != null)
			? lds
			: BuildDefaults.LOG_CHANNEL_DEF;
				
		
		ci= modDes.connectionRecordList.iterator();
		while (ci.hasNext()) {
			// get a connection
			ChannelRecord cr= (ChannelRecord) ci.next();
			
			// make a channel - let it be rendezvous channel for now
			Channel channel= new LazyAckChannel(cr.name, modelGroup);			
//			Channel channel= new RendezvousChannel(cr.name, modelGroup);
			
			// connect sources first - these are component outputs
			Iterator si= cr.sources.iterator();
			while (si.hasNext()) {
				// get a source connection point
				ConnectionPoint cp= (ConnectionPoint)si.next();
				// get the source component
				// ASSUME the parser has ensured that the components exist
				ComponentRecord cmpRec= (ComponentRecord)modDes.componentNameMap.get(cp.componentName);
				Component cmp= cmpRec.component;
								
				// get the output terminal - the source of data
				OutputTerminal out= cmp.getOutputTerminal(cp.terminalName);
				// is the terminal OK?
				if (out == null || !(out instanceof OutputTerminal)) {
					// terminal is not good
					throw new ModelNotBuiltException("Error: Output terminal "
							+ cp.terminalName 
							+ " does not exist in component "  
							+ cmp.getName() 
							+ ", of class " 
							+ cmp.getClass().getName());
				}
				
				// connect the state and the terminal
				channel.attach(out);

			} // while there are sources
			
			// connect destinations - these are component inputs
			Iterator di= cr.destinations.iterator();
			while (di.hasNext()) {
				// get a destination connection point
				ConnectionPoint cp= (ConnectionPoint)di.next();
				// get the destination component
				// ASSUME the parser has ensured that the components exist
				ComponentRecord cmpRec= (ComponentRecord)modDes.componentNameMap.get(cp.componentName);
				Component cmp= cmpRec.component;
				
				// get the output terminal - the source of data
				InputTerminal in= cmp.getInputTerminal(cp.terminalName);
				// is the terminal OK?
				if (in == null || !(in instanceof InputTerminal)) {
					// terminal is not good
					throw new ModelNotBuiltException("Error: Input terminal "
							+ cp.terminalName 
							+ " does not exist in component "  
							+ cmp.getName() 
							+ ", of class " 
							+ cmp.getClass().getName());
				}
				

				// connect the state and the terminals
				channel.attach(in);
				
			} // while there are sources
			
			
			// set the journaling
			channel.setJournalActivity(logChannel);
			
			modelGroup.addMember(channel);
			
		} // while there are connections
		
		return modelGroup;
		
	} // build model

	
	public static void main(String[] args) {
		
		Director director= Director.getInstance();
		
		ClassRegistry reg=  ClassRegistry.getInstance();
		
		try {		
			ModelDescription model= ModelDescription.getInstance();
			model.load("testModelLoader.xml");
			
			model.print();
			
			director.info("\n\n");
			
			HandshakeNetModelBuilder builder= new HandshakeNetModelBuilder();
			
			CompositeEntity grp;
		

			grp= builder.buildModel();
			
			grp.print();
		}
		catch (Exception e) {
			director.error(e.getMessage());
			e.printStackTrace();
		}
    			
	} // main
	
} // ModelBulder
