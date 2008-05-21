package com.sun.electric.tool.simulation.eventsim.handshakeNet.component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import com.sun.electric.tool.simulation.eventsim.core.common.Parameters;
import com.sun.electric.tool.simulation.eventsim.core.engine.EventSimErrorException;
import com.sun.electric.tool.simulation.eventsim.core.hierarchy.CompositeEntity;
import com.sun.electric.tool.simulation.eventsim.handshakeNet.terminal.InputTerminal;
import com.sun.electric.tool.simulation.eventsim.handshakeNet.terminal.OutputTerminal;


/**
 * Component consists of a set of behaviors encapsulated by internal 
 * component workers and internal component workers, and by a set of terminals
 * that can be exposed to the environment.
 * 
 * Note: for the sake of simplicity I decided not to keep track of 
 * internal components and channels. This decision is a subject of revision.
 * 
 * @author ib27688
 *
 */

public abstract class Component extends CompositeEntity {
	
	/** externally visible inputs */
	protected Map<String, InputTerminal> inputs= new HashMap<String,InputTerminal>();
	protected Map<InputTerminal, LinkedList<String>> virtualInputs= new HashMap<InputTerminal, LinkedList<String>>();
	/** externally visible outputs */
	protected Map<String, OutputTerminal> outputs= new HashMap<String,OutputTerminal>();
	protected Map<OutputTerminal, LinkedList<String>> virtualOutputs= new HashMap<OutputTerminal, LinkedList<String>>();
	/** internal ComponentWorkers */
	protected Set<ComponentWorker> workers= new HashSet<ComponentWorker>();
	/** parameters passed to this component */
	protected Parameters parameters;
	
	protected Component(String name, Parameters params) throws EventSimErrorException {
		super(name);
		parameters= params;
		buildComponent();
	} // constructor

	protected Component(String name, Parameters params, CompositeEntity g) throws EventSimErrorException {
		super(name, g);
		parameters= params;
		buildComponent();
	} // constructor

	/**
	 * Build a componnent: first assemble the pieces,
	 * then process any parameters that have been provided.
	 * @throws EventSimErrorException 
	 */
	protected void buildComponent() throws EventSimErrorException {
		assemble();
		processParameters();
	} // build component

	/**
	 * Assembly of a component, to be provided by a subclass.
	 */
	protected abstract void assemble();	
	
	/** 
	 * Process parameters provided at creation.
	 * This method is to be provided by a subclass.
	 * The method is called by a constructor, after the component has been assembled.
	 */
	protected abstract void processParameters() throws EventSimErrorException;

	/**
	 * An Iterable that allows one to iterate over names of input terminals.
	 * Intended for "assembly programs": they would make an instance of 
	 * a component and query that instance for names of input and
	 * output terminals
	 * @return iterable with the names of input terminals
	 */
	public Iterable<String> inputTerminalNames() {
		return inputs.keySet();
	} // inputTerminalNamesIterable

	/**
	 * return the iterable for virtual inputs associated with an input terminals
	 * @param t input terminals
	 * @return iterable over virtual ports for that input terminal
	 */
	public Iterable<String> virtualPortNames(InputTerminal t) {
		return virtualInputs.get(t);
	}
	
	/**
	 * An Iterable that allows one to iterate over names of output terminals.
	 * Intended for "assembly programs": they would make an instance of 
	 * a component and query that instance for names of input and
	 * output terminals
	 * @return iterable with the names of output terminals
	 */
	public Iterable<String> outputTerminalNames() {
		return outputs.keySet();
	} // outputTerminalNamesIterable
	
	/**
	 * return the iterable for virtual outputs associated with an output terminals
	 * @param t input terminals
	 * @return iterable over virtual ports for that input terminal
	 */
	public Iterable<String> virtualPortNames(OutputTerminal t) {
		return virtualOutputs.get(t);
	}
	
	
	public boolean isAttached(InputTerminal t) {
		return outputs.containsValue(t);
	} // isAttached
	
	public boolean isAttached(OutputTerminal t) {
		return outputs.containsValue(t);
	} // isAttached
	
	/**
	 * query for an input terminal by name 
	 * @param name terminal name
	 * @return terminal with the given name, null if not found
	 */
	public InputTerminal getInputTerminal(String name) {
		return inputs.get(name);
	} // getInputTerminal
	
	/**
	 * query for an input terminal by name 
	 * @param name terminal name
	 * @return terminal with the given name, null if not found
	 */
	public OutputTerminal getOutputTerminal(String name) {
		return outputs.get(name);
	} // getOutputTerminal
	
	
	/**
	 * Expose a terminal.
	 * The terminal must belong to a worker within a component.
	 */
	protected boolean expose(InputTerminal t) {
		boolean check= false;
		try {
			check=!(inputs.containsValue(t) || inputs.containsKey(t.getBaseAlias())); 
			if (check)	{
				inputs.put(t.getBaseAlias(), t);
				// get ready for adding virtual inputs
				virtualInputs.put(t, new LinkedList<String>());
			}
		}
		catch (Exception e) {
			logError(e.getMessage());
		}
		return check;
	} // expose
	
	
	/**
	 * Expose a terminal and its virtual names.
	 * The terminal must belong to a worker within a component.
	 */
	protected boolean expose(InputTerminal t, Iterable<String> virtualNames) {
		boolean check= false;
		try {
			check=!(inputs.containsValue(t) || inputs.containsKey(t.getBaseAlias())); 
			if (check)	{
				inputs.put(t.getBaseAlias(), t);
				// get ready for adding virtual inputs
				LinkedList<String> vNamesList= new LinkedList<String>();
				for (String name : virtualNames) {
					vNamesList.addLast(name);
				}
				virtualInputs.put(t, vNamesList);
			}
		}
		catch (Exception e) {
			logError(e.getMessage());
		}
		return check;
	} // expose
	
	
	/**
	 * Expose a terminal.
	 * The terminal must belong to a worker within a component.
	 */
	protected boolean expose(OutputTerminal t) {
		boolean check= false;
		try {
			check=!(outputs.containsValue(t) || outputs.containsKey(t.getBaseAlias())); 
			if (check)	{
				outputs.put(t.getBaseAlias(), t);
				// get ready for virtual outputs
				virtualOutputs.put(t, new LinkedList<String>());
			}
		}
		catch (Exception e) {
			logError(e.getMessage());
		}
		return check;
	} // expose
	
	/**
	 * Expose a terminal and its virtual names.
	 * The terminal must belong to a worker within a component.
	 */
	protected boolean expose(OutputTerminal t, Iterable<String> virtualNames) {
		boolean check= false;
		try {
			check=!(inputs.containsValue(t) || inputs.containsKey(t.getBaseAlias())); 
			if (check)	{
				outputs.put(t.getBaseAlias(), t);
				// get ready for adding virtual outputs
				LinkedList<String> vNamesList= new LinkedList<String>();
				for (String name : virtualNames) {
					vNamesList.addLast(name);
				}
				virtualOutputs.put(t, vNamesList);
			}
		}
		catch (Exception e) {
			logError(e.getMessage());
		}
		return check;
	} // expose
	
	/**
	 * Add a virtual port to a terminal
	 * @param t terminal
	 * @param vName vitrual port name
	 * @return true if successful
	 */
	protected boolean addVirtualPort(InputTerminal t, String vName) {
		boolean check= false;
		LinkedList<String> pList= virtualInputs.get(t);
		check= (pList != null);
		if (check) {
			pList.addLast(vName);
		}
		return check;
	}
	
	protected boolean addVirtualPort(InputTerminal t, String[] vName) {
		boolean check= false;
		LinkedList<String> pList= virtualInputs.get(t);
		check= (pList != null);
		if (check) {
			for (String vn : vName) {
				pList.addLast(vn);
			}
		}
		return check;
	}
	
	/**
	 * Add a virtual port to a terminal
	 * @param t terminal
	 * @param vName vitrual port name
	 * @return true if successful
	 */
	protected boolean addVirtualPort(OutputTerminal t, String vName) {
		boolean check= false;
		LinkedList<String> pList= virtualOutputs.get(t);
		check= (pList != null);
		if (check) {
			pList.addLast(vName);
		}
		return check;
	}
	
	protected boolean addVirtualPort(OutputTerminal t, String[] vName) {
		boolean check= false;
		LinkedList<String> pList= virtualOutputs.get(t);
		check= (pList != null);
		if (check) {
			for (String vn: vName) {
				pList.addLast(vn);
			}
		}
		return check;
	}
	
	protected void addWorker(ComponentWorker w) {
		// record the worker
		if (workers.add(w)) {
			// The worker is a new one
			// add members to the CompositeEntity, the worker and the terminals
			addMember(w);
			for (InputTerminal in : w.inputIterable()) {
				addMember(in);
			} // for
			for (OutputTerminal out : w.outputIterable()) {
				addMember(out);
			} // for
		} // if
	} // addWorker
	
	public long resolveLiteral(String literal) {
		throw new RuntimeException("Unknown literal \""+literal+"\" on component "+ this);
	}
	
} // class Component
