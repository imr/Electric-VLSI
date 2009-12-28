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

public class Stage extends Component {
 
	
	public static final String INITIALLY_FULL= "initiallyFull";
	public static final boolean INITIALLY_FULL_DEF= false;
	
	public static final String INITIAL_DATA= "initialData";
	public static final long INITIAL_DATA_DEF= 0;

	public static final String INITIAL_ADDRESS= "initialAddress";
	public static final long INITIAL_ADDRESS_DEF= 0;
	
	public static final String FORWARD_DELAY= "forwardDelay";
	public static final Delay FORWARD_DELAY_DEF= new Delay(200);
	public static final String FORWARD_DELAY_VARIATION= "forwardDelayVariation";
	public static final int FORWARD_DELAY_VARIATION_DEF= 0;	
	
	public static final String BACKWARD_DELAY= "backwardDelay";
	public static final Delay BACKWARD_DELAY_DEF= new Delay(150);
	public static final String BACKWARD_DELAY_VARIATION= "backwardDelayVariation";
	public static final int BACKWARD_DELAY_VARIATION_DEF= 0;
	
	public static final String SHIFT= "shiftStage";
	public static final boolean SHIFT_DEF= false;
	public static final String SHIFT_BITS= "shiftBits";
	public static final int SHIFT_BITS_DEF= 1;
	
	public static final String IN= "In";
	public static final String OUT= "Out";
	
	private boolean initiallyFull;
	private Datum initialData;
	private Datum data= null;
	private Delay forwardDelay;
	private int forwardDelayVariation;
	private Delay backwardDelay;
	private int backwardDelayVariation;
	private boolean shift;
	private int bitsToShift;
	
	private StageWorker worker;
	
	public Stage(String name, Parameters params) throws EventSimErrorException {
		super(name, params);
	}

	public Stage(String name, Parameters params, CompositeEntity g)
			throws EventSimErrorException {
		super(name, params, g);

	}

	@Override
	protected void assemble() {
		worker= new StageWorker("StageWorker");
		addWorker(worker);
		expose(worker.inTerm);
		expose(worker.outTerm);
	}

	/**
	 * Get the state of the component
	 * @return String describing the state of the component
	 */
	public String getInfo() {
		String info= ((shift)? "SHIFT " : "" ) + "Stage " + getBaseName() + ", contains:" + data
			+ ", addressShift= " + shift + ", bitsToShift= " + bitsToShift
			+ ", ackHere= " + worker.ackHere
			+ ", newInputHere= " + worker.newInputHere
			+ ", initialRound= " + worker.initialRound;
		return info;
	} // getInfo

	
	@Override
	protected void processParameters() throws EventSimErrorException {
		
		initiallyFull= INITIALLY_FULL_DEF;
		initialData= null;

		forwardDelay= FORWARD_DELAY_DEF;
		forwardDelayVariation= FORWARD_DELAY_VARIATION_DEF;
		backwardDelay= BACKWARD_DELAY_DEF;
		backwardDelayVariation= BACKWARD_DELAY_VARIATION_DEF;
		shift= SHIFT_DEF;
		bitsToShift= SHIFT_BITS_DEF;
		
		Boolean isFull= globals.booleanValue(INITIALLY_FULL);
		if (isFull != null) initiallyFull= isFull;
		if (initiallyFull) initialData= new Datum();
		
		Long id= globals.longValue(INITIAL_DATA);
		if (id != null) {
			if (initialData == null) {
				initialData= new Datum(id, 0);
				initiallyFull= true;
			}
			else {
				initialData.data= id;
			}
		}
		Integer ia= globals.intValue(INITIAL_ADDRESS);
		if (id != null) {
			if (initialData == null) {
				initialData= new Datum(0, ia);
				initiallyFull= true;
			}
			else {
				initialData.address= ia;
			}
		}
		
		Integer sd= globals.intValue(FORWARD_DELAY);
		if (sd != null && sd >= 0) forwardDelay= new Delay(sd);
		
		Integer sdv= globals.intValue(FORWARD_DELAY_VARIATION);
		if (sdv != null && sdv >= 0) forwardDelayVariation= sdv;

		Integer bd= globals.intValue(BACKWARD_DELAY);
		if (bd != null && bd >= 0) backwardDelay= new Delay(bd);
		
		Integer bdv= globals.intValue(BACKWARD_DELAY_VARIATION);
		if (bdv != null && bdv >= 0) backwardDelayVariation= bdv;
		
		Boolean isShift= globals.booleanValue(SHIFT);
		if (isShift != null) shift= isShift;
		
		Integer sb= globals.intValue(SHIFT_BITS);
		if (sb != null) bitsToShift= sb;
		
		
		if (parameters != null) {
			for (int i= 0; i< parameters.size(); i++) {
				String pName= parameters.getName(i);
				String sValue= parameters.getValue(i);
				try {
					if (pName.equals(INITIALLY_FULL)) {
						initiallyFull= Boolean.parseBoolean(sValue);
					}
					else if (pName.equals(INITIAL_DATA)) {
						long d= Long.parseLong(sValue);
						if (initialData==null) {
							initialData= new Datum(d, 0);
							initiallyFull= true;
						}
						else {
							initialData.data= d;
						}
					}
					else if (pName.equals(INITIAL_ADDRESS)) {
						int a= Integer.parseInt(sValue);
						if (initialData==null) {
							initialData= new Datum(0, a);
							initiallyFull= true;
						}
						else {
							initialData.address= a;
						}
					}				
					else if (pName.equals(FORWARD_DELAY)) {
						int d= Integer.parseInt(sValue);
						if (d >=0) {
							forwardDelay= new Delay(d);
						}
						else {
							throw new Exception("Negative delay provided");
						}
					}
					else if (pName.equals(FORWARD_DELAY_VARIATION)) {
						int d= Integer.parseInt(sValue);
						if (d >=0) {
							forwardDelayVariation= d;
						}
						else {
							throw new Exception("Negative delay variation provided");
						}
					}					
					else if (pName.equals(BACKWARD_DELAY)) {
						int d= Integer.parseInt(sValue);
						if (d >=0) {
							backwardDelay= new Delay(d);
						}
						else {
							throw new Exception("Negative delay provided");
						}
					}
					else if (pName.equals(BACKWARD_DELAY_VARIATION)) {
						int d= Integer.parseInt(sValue);
						if (d >=0) {
							backwardDelayVariation= d;
						}
						else {
							throw new Exception("Negative delay variation provided");
						}
					}
					else if (pName.equals(SHIFT)) {
						shift= Boolean.parseBoolean(sValue);
					}
					else if (pName.equals(SHIFT_BITS)) {
						int b= Integer.parseInt(sValue);
						if (b >= 0) {
							bitsToShift= b;
						}
						else {
							throw new Exception("Negative number provided for bit shift");
						}
					}
					else {
						logError("Ignored unknown parameter: " + pName);
					}
				}
				catch (Exception e) {
					fatalError("Bad parameter provided for " + pName + ": " + sValue);
				}
			} // for
		} // if
	} 
	
	private class StageWorker extends ComponentWorker {

		// input to the stage
		InputTerminal inTerm;
		// output from the stage
		OutputTerminal outTerm;
		
		Command inputHereCmd;
		Command ackHereCmd;
		
		boolean ackHere= true;
		boolean newInputHere= false;

		boolean initialRound= true;
		
		public StageWorker(String n) {
			super(n);
			build();
		}

		public StageWorker(String name, CompositeEntity g) {
			super(name, g);
			build();
		}
		
		
		void build() {
			inTerm= new DelayedInputTerminal(IN);
			outTerm= new DelayedOutputTerminal(OUT);
			inputHereCmd= new InputHereCommand();
			ackHereCmd= new AckHereCommand();
			attachInput(inTerm, inputHereCmd);
			attachOutput(outTerm, ackHereCmd);
		}
		
		@Override
		public void init() throws EventSimErrorException {
			initialRound= true;
			if (initiallyFull) {
				data= initialData;
//				System.out.println(Stage.this.getName() + " Initial data out: " + data + ", initialRound: " + initialRound);
//				System.out.flush();
				// if a shift stage, shift right away
				if (shift) data.rotateAddress(bitsToShift);
				outTerm.outputAvailable(data, Delay.randomizeDelay(forwardDelay, forwardDelayVariation));
				ackHere= false;
				newInputHere= false;
			}
			else {
				ackHere= true;
				newInputHere= false;
			}
		}
		
		private void dataOut() throws EventSimErrorException {
			Object d= inTerm.getData();
			if (d instanceof Datum) {
				data= (Datum)d;
			}
			else {
				fatalError("Non datum object received: " + d + " of type " + d.getClass().getName());
			}
			ackHere= false;
			newInputHere= false;
			// is this a shift stage?
			if (shift) data.rotateAddress(bitsToShift);
			outTerm.outputAvailable(data, Delay.randomizeDelay(forwardDelay, forwardDelayVariation));
			// data= null;
			// if (!(initialRound  && initiallyFull)) {
				inTerm.ackInput(Delay.randomizeDelay(backwardDelay, backwardDelayVariation));						
			// }
			initialRound= false;
			System.out.println(Stage.this.getName() + " Data out: " + data + ", initialRound: " + initialRound);
			System.out.flush();
		}
		
		class InputHereCommand extends Command {
			@Override
			protected void execute(Object params) throws EventSimErrorException {
				if (newInputHere) {
					fatalError("Two inputs received without an ack.");
				}
				else {
					newInputHere= true;
//					System.out.println(Stage.this.getName() + " InputHere");
//					System.out.flush();
					if (ackHere) {
						dataOut();
					}
				}
			} // execute
		} // class InputHereCommand
		
		class AckHereCommand extends Command {
			@Override
			protected void execute(Object params) throws EventSimErrorException {
				if (ackHere) {
					fatalError("Two acks received in a row");
				}
				else {
					System.out.println(Stage.this.getName() + " AckHere");
					System.out.flush();
					ackHere= true;
 					data= null;
					if (newInputHere) {
						dataOut();
					}
				}
			} // execute
		} // class AckHereCommand
		
	}// class StageWorker

} // class Stage
