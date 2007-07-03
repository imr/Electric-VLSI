package com.sun.electric.tool.simulation.eventsim.infinity.components;

import com.sun.electric.tool.simulation.eventsim.core.common.Parameters;
import com.sun.electric.tool.simulation.eventsim.core.engine.Command;
import com.sun.electric.tool.simulation.eventsim.core.engine.EventSimErrorException;
import com.sun.electric.tool.simulation.eventsim.core.hierarchy.CompositeEntity;
import com.sun.electric.tool.simulation.eventsim.core.simulation.Delay;
import com.sun.electric.tool.simulation.eventsim.handshakeNet.component.Component;
import com.sun.electric.tool.simulation.eventsim.handshakeNet.component.ComponentWorker;
import com.sun.electric.tool.simulation.eventsim.handshakeNet.terminal.DelayedInputTerminal;
import com.sun.electric.tool.simulation.eventsim.handshakeNet.terminal.DelayedOutputTerminal;
import com.sun.electric.tool.simulation.eventsim.handshakeNet.terminal.InputTerminal;
import com.sun.electric.tool.simulation.eventsim.handshakeNet.terminal.OutputTerminal;
import com.sun.electric.tool.simulation.eventsim.infinity.common.Datum;

public class Branch extends Component {

	public static final String IN= "In";
	public static final String OUT_0= "Out0";
	public static final String OUT_1= "Out1";
	
	public static final String BRANCH_DELAY= "branchDelay";
	public static final Delay BRANCH_DELAY_DEF= new Delay(100);
	public static final String BRANCH_DELAY_VARIATION= "branchDelayVariation";
	public static final int BRANCH_DELAY_VARIATION_DEF= 0;

	public static final String ADDRESS_BIT= "addressBit";
	public static final int ADDRESS_BIT_DEF= 0;
	
	private Datum data= null;
	private Delay delay;
	private int delayVariation;	
	private int addressBit;
	
	private BranchWorker worker;
	
	public Branch(String name, Parameters params) throws EventSimErrorException {
		super(name, params);
	}

	public Branch(String name, Parameters params, CompositeEntity g)
			throws EventSimErrorException {
		super(name, params, g);
	}

	@Override
	protected void assemble() {
		worker= new BranchWorker("BranchWorker");
		addWorker(worker);
		expose(worker.inTerm);
		expose(worker.outTerm0);
		expose(worker.outTerm1);
	} // assemble
	
	/**
	 * Get the state of the component
	 * @return String describing the state of the component
	 */
	public String getInfo() {
		String info= "Branch " + getBaseName() + ", data= " + data 
			+ ", addressBit= " + addressBit + ", direction= " + worker.direction;
		return info;
	} // getInfo

	@Override
	protected void processParameters() throws EventSimErrorException {
		
		delay= BRANCH_DELAY_DEF;
		delayVariation= BRANCH_DELAY_VARIATION_DEF;	
		addressBit= ADDRESS_BIT_DEF;
		
		if (parameters != null) {
			for (int i= 0; i< parameters.size(); i++) {
				String pName= parameters.getName(i);
				String sValue= parameters.getValue(i);
				try {
					if (pName.equals(BRANCH_DELAY)) {
						int d= Integer.parseInt(sValue);
						if (d >=0) {
							delay= new Delay(d);
						}
						else {
							throw new Exception("Negative delay provided");
						}
					}
					else if (pName.equals(BRANCH_DELAY_VARIATION)) {
						int d= Integer.parseInt(sValue);
						if (d >=0) {
							delayVariation= d;
						}
						else {
							throw new Exception("Negative delay variation provided: " + d);
						}
					}
					else if (pName.equals(ADDRESS_BIT)) {
						int b= Integer.parseInt(sValue);
						if (b >=0) {
							addressBit= b;
						}
						else {
							throw new Exception("Negative address bit provided: " + b);
						}
					} 
				} // try
				catch (Exception e) {
					fatalError("Bad parameter provided for " + pName + ": " + sValue);
				}
			} // for
		} // if				
	} // processParameters

	private class BranchWorker extends ComponentWorker {

		InputTerminal inTerm;
		OutputTerminal outTerm0, outTerm1;
		
		Command inputHereCmd;
		Command ack0HereCmd;
		Command ack1HereCmd;
		
		boolean inputHere= false;
		boolean ackHere= true;
		int direction= -1; // just 0 or 1, only applicable if ackHere was false
		
		public BranchWorker(String n) {
			super(n);
			build();
		}

		public BranchWorker(String name, CompositeEntity g) {
			super(name, g);
			build();
		}
		
		void build() {
			inTerm= new DelayedInputTerminal(IN);
			outTerm0= new DelayedOutputTerminal(OUT_0);
			outTerm1= new DelayedOutputTerminal(OUT_1);
			inputHereCmd= new InputeHereCommand();
			ack0HereCmd= new AckHereCommand(0);
			ack1HereCmd= new AckHereCommand(1);
			attachInput(inTerm, inputHereCmd);
			attachOutput(outTerm0, ack0HereCmd);
			attachOutput(outTerm1, ack1HereCmd);
		} // build

		@Override
		public void init() throws EventSimErrorException {
			inputHere= false;
			ackHere= true;
			direction= -1;
		} // init
		
		private void dataOut() throws EventSimErrorException {
			ackHere= false;
			inputHere= false;
			OutputTerminal outTerm= (direction == 0)? outTerm0 : outTerm1;
			outTerm.outputAvailable(data, Delay.randomizeDelay(delay, delayVariation));
			inTerm.ackInput(Delay.randomizeDelay(delay, delayVariation));
		} // dataOut
		
		/**
		 * Triggered when an input arrives
		 */
		class InputeHereCommand extends Command {
			@Override
			protected void execute(Object params) throws EventSimErrorException {
				if (inputHere) {
					fatalError("Two inputs received without an ack.");
				}
				else {
					inputHere= true;
					Object d= inTerm.getData();
					if (d instanceof Datum) {
						// get data and establish the direction
						data= (Datum)d;
						direction= data.getAddressBit(addressBit);
					}
					else {
						fatalError("Non datum object received: " + d + " of type " + d.getClass().getName());
					}
					if (ackHere) {
						dataOut();
					}
				}
			} // execute
		} // class InputHereCommand
		
		/**
		 * Tiggered when an ack arrives
		 */
		class AckHereCommand extends Command {
			
			int ackNumber;
			
			public AckHereCommand(int n) {
				ackNumber= n;
			}
			
			@Override
			protected void execute(Object params) throws EventSimErrorException {
				if (ackHere) {
					fatalError("Two acks received in a row");
				}
				else if (ackNumber != direction) {
					fatalError("Ack received on input " + ackNumber + ", expected on " + direction);
				}
				else {
					ackHere= true;
					direction= -1;
					if (inputHere) {
						dataOut();
					}
				}
			} // execute		
		} // class AckHereCommand
		
	} // class BranchWorker
	
} // class Branch
