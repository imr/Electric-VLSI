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

public class Cross extends Component {

	public static final String IN[]= new String[] {"In0", "In1"};
	public static final String OUT[]= new String[] { "Out0", "Out1"};
	
	public static final String CROSS_DELAY= "crossDelay";
	public static final Delay CROSS_DELAY_DEF= new Delay(100);
	public static final String CROSS_DELAY_VARIATION= "crossDelayVariation";
	public static final int CROSS_DELAY_VARIATION_DEF= 0;
	
	public static final String ADDRESS_BIT= "addressBit";
	public static final int ADDRESS_BIT_DEF= 0;
	
	private Datum data[]= new Datum[] { null, null };
	private Delay delay;
	private int delayVariation;
	private int addressBit;
	
	private CrossWorker crossWorker;
	
	public Cross(String name, Parameters params) throws EventSimErrorException {
		super(name, params);
	}

	public Cross(String name, Parameters params, CompositeEntity g)
			throws EventSimErrorException {
		super(name, params, g);
	}

	@Override
	protected void assemble() {
		crossWorker= new CrossWorker("CrossWorker");
		addWorker(crossWorker);
		for (int i=0; i<2; i++) {
			expose(crossWorker.inTerm[i]);
			expose(crossWorker.outTerm[i]);
		}
	} // assemble

	/**
	 * Get the state of the component
	 * @return String describing the state of the component
	 */
	public String getInfo() {
		String info= "Cross " + getBaseName() + "; data[0]= " + data[0] + ", data[1]=  " + data[1] 
                + ", request granted= " + crossWorker.grantedRequest;
		return info;
	} // getInfo
	
	@Override
	protected void processParameters() throws EventSimErrorException {
		
		delay= CROSS_DELAY_DEF;
		delayVariation= CROSS_DELAY_VARIATION_DEF;
		addressBit= ADDRESS_BIT_DEF;
		
		if (parameters != null) {
			for (int i= 0; i< parameters.size(); i++) {
				String pName= parameters.getName(i);
				String sValue= parameters.getValue(i);
				try {
					if (pName.equals(CROSS_DELAY)) {
						int d= Integer.parseInt(sValue);
						if (d >=0) {
							delay= new Delay(d);
						}
						else {
							throw new Exception("Negative delay provided");
						}
					}
					else if (pName.equals(CROSS_DELAY_VARIATION)) {
						int d= Integer.parseInt(sValue);
						if (d >=0) {
							delayVariation= d;
						}
						else {
							throw new Exception("Negative delay variation provided: " + d);
						}
					}
				}
				catch (Exception e) {
					fatalError("Bad parameter provided for " + pName + ": " + sValue);
				}
			}
		}
	} // processParameters

	private class CrossWorker extends ComponentWorker {

		InputTerminal inTerm[]= new InputTerminal[2];
		OutputTerminal outTerm[]= new OutputTerminal[2];
		
		Arbiter arbiter;
		
		Command inHereCmd[]= new Command[2];
		Command grantCmd[]= new Command[2];
		Command ackHereCmd[]= new Command[2];
		
		boolean inHere[]= new boolean[] { false, false };
		boolean ackHere[] =new boolean[] { true, true };		
		// record which request was granted, 0 or 1
		int grantedRequest= -1;
		int direction= -1; // just 0 or 1, only applicable if ackHere was false
		
		public CrossWorker(String name, CompositeEntity g) {
			super(name, g);
			build();
		}

		public CrossWorker(String n) {
			super(n);
			build();
		}

		private void build() {
			for (int i= 0; i<2; i++) {
				inTerm[i]= new DelayedInputTerminal(IN[i]);
				outTerm[i]= new DelayedOutputTerminal(OUT[i]);
				inHereCmd[i]= new RequestHereCommand(i);
				grantCmd[i]= new GrantCommand(i);
				attachInput(inTerm[i], inHereCmd[i]);
				ackHereCmd[i]= new AckHereCommand(i);
				attachOutput(outTerm[i], ackHereCmd[i]);
			}
		} // build
		
		@Override
		public void init() throws EventSimErrorException {
			for (int i=0; i<2; i++) {
				inHere[i]= false;
				ackHere[i]= true;
			}
		} // init
		
		private class RequestHereCommand extends Command {
		
			// which request is this?
			private int reqNumber;
			
			public RequestHereCommand(int n) {
				reqNumber= n;
			}
			
			@Override
			protected void execute(Object params) throws EventSimErrorException {
				if (inHere[reqNumber]) {
					fatalError("Two requests arrived on input " + reqNumber + " without an ack.");
				}
				else {
					inHere[reqNumber]= true;
					Object d= inTerm[reqNumber].getData();
					if (d instanceof Datum) {
						data[reqNumber] = (Datum)d;
						arbiter.request(grantCmd[reqNumber]);
					}
					else {
						fatalError("Non datum object received: " + d + " of type " + d.getClass().getName());
					}
				}
			} // execute
		} // clas RequestHereCommand
		
		private class GrantCommand extends Command {
			
			int grantNumber;
			
			public GrantCommand(int n) {
				grantNumber= n;
			}
			
			@Override
			protected void execute(Object params) throws EventSimErrorException {
				grantedRequest= grantNumber;
				// where to go
				direction= data[grantNumber].getAddressBit(addressBit);
				outTerm[direction].outputAvailable(data[grantNumber], Delay.randomizeDelay(delay, delayVariation));
				ackHere[direction]= false;
				inHere[grantNumber]= false;
				inTerm[grantNumber].ackInput(Delay.randomizeDelay(delay, delayVariation));
			} // execute
		} // class GrantCommand
				
		private class AckHereCommand extends Command {
			
			int ackNumber;
			
			public AckHereCommand(int n) {
				ackNumber= n;
			}
			
			@Override
			protected void execute(Object params) throws EventSimErrorException {
				if (ackHere[ackNumber]) {
					fatalError("Two acks received in a row");
				}
				else if (ackNumber != direction) {
					fatalError("Ack received on input " + ackNumber + ", expected on " + direction);
				}
				else {
					grantedRequest= -1;
					direction= -1;
					ackHere[ackNumber]= true;
					arbiter.release();
				}
			} // execute
		} // class AckHereCommand
		
	} // class CrossWorker
	
} // class Cross
