package com.sun.electric.tool.simulation.eventsim.infinity.components;

import com.sun.electric.tool.simulation.eventsim.core.common.Parameters;
import com.sun.electric.tool.simulation.eventsim.core.engine.Command;
import com.sun.electric.tool.simulation.eventsim.core.engine.EventSimErrorException;
import com.sun.electric.tool.simulation.eventsim.core.hierarchy.CompositeEntity;
import com.sun.electric.tool.simulation.eventsim.core.simulation.Delay;
import com.sun.electric.tool.simulation.eventsim.handshakeNet.component.Component;
import com.sun.electric.tool.simulation.eventsim.handshakeNet.component.ComponentWorker;
import com.sun.electric.tool.simulation.eventsim.handshakeNet.terminal.DelayedInputTerminal;
import com.sun.electric.tool.simulation.eventsim.handshakeNet.terminal.InputTerminal;
import com.sun.electric.tool.simulation.eventsim.handshakeNet.terminal.OutputTerminal;
import com.sun.electric.tool.simulation.eventsim.infinity.common.Datum;

public class Merge extends Component {
	
	public static final String IN_0= "In0";
	public static final String IN_1= "In1";
	public static final String OUT= "Out";

	public static final String MERGE_DELAY= "mergeDelay";
	public static final Delay MERGE_DELAY_DEF= new Delay(100);
	public static final String MERGE_DELAY_VARIATION= "mergeDelayVariation";
	public static final int MERGE_DELAY_VARIATION_DEF= 0;
	
	private Datum data[]= new Datum[] { null, null };
	private Delay delay;
	private int delayVariation;	
	
	private MergeWorker worker;

	protected Merge(String name, Parameters params) throws EventSimErrorException {
		super(name, params);
	}
	
	protected Merge(String name, Parameters params, CompositeEntity g) throws EventSimErrorException {
		super(name, params, g);
	}


	@Override
	protected void assemble() {
		worker= new MergeWorker("MergeWorker");
		addWorker(worker);
		for (int i=0; i<2; i++) {
			expose(worker.inTerm[i]);
		}
		expose(worker.outTerm);
	}

	/**
	 * Get the state of the component
	 * @return String describing the state of the component
	 */
	public String getInfo() {
		String info= "Merge " + getBaseName() + "; data[0]= " + data[0] + ", data[1]=  " + data[1] 
                        + ", request granted= " + worker.grantedRequest;
		return info;
	} // getInfo
	
	@Override
	protected void processParameters() throws EventSimErrorException {
		
		delay= MERGE_DELAY_DEF;
		delayVariation= MERGE_DELAY_VARIATION_DEF;	
		
		if (parameters != null) {
			for (int i= 0; i< parameters.size(); i++) {
				String pName= parameters.getName(i);
				String sValue= parameters.getValue(i);
				try {
					if (pName.equals(MERGE_DELAY)) {
						int d= Integer.parseInt(sValue);
						if (d >=0) {
							delay= new Delay(d);
						}
						else {
							throw new Exception("Negative delay provided");
						}
					}
					else if (pName.equals(MERGE_DELAY_VARIATION)) {
						int d= Integer.parseInt(sValue);
						if (d >=0) {
							delayVariation= d;
						}
						else {
							throw new Exception("Negative delay variation provided: " + d);
						}
					}
				} // try
				catch (Exception e) {
					fatalError("Bad parameter provided for " + pName + ": " + sValue);
				}
			} // for
		} // if	
	}

	private class MergeWorker extends ComponentWorker {

		InputTerminal inTerm[]= new InputTerminal[2];		
		OutputTerminal outTerm;
		
		Arbiter arbiter;
		
		Command inHereCmd[]= new Command[2];
		Command grantCmd[]= new Command[2];
		Command ackHereCmd= null;;
		
		boolean inHere[]= new boolean[] { false, false };
		boolean ackHere= true;		
		// record which request was granted, 0 or 1
		int grantedRequest= -1;
		
		public MergeWorker(String name, CompositeEntity g) {
			super(name, g);
			build();
		}

		public MergeWorker(String n) {
			super(n);
			build();
		}

		private void build() {
			inTerm[0]= new DelayedInputTerminal(IN_0);
			inTerm[1]= new DelayedInputTerminal(IN_1);
			for (int i= 0; i<2; i++) {
				inHereCmd[i]= new RequestHereCommand(i);
				grantCmd[i]= new GrantCommand(i);
				attachInput(inTerm[i], inHereCmd[i]);
			}
			ackHereCmd= new AckHereCommand();
			attachOutput(outTerm, ackHereCmd);
		} // build
		
		@Override
		public void init() throws EventSimErrorException {
			for (int i=0; i<2; i++) {
				inHere[i]= false;
			}
			ackHere= true;
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
				data[grantNumber]= (Datum)(inTerm[grantNumber].getData());
				outTerm.outputAvailable(data[grantNumber], Delay.randomizeDelay(delay, delayVariation));
				ackHere= false;
				inHere[grantNumber]= false;
				inTerm[grantNumber].ackInput(Delay.randomizeDelay(delay, delayVariation));
			} // execute
		} // class GrantCommand
		

		
		private class AckHereCommand extends Command {
			
			@Override
			protected void execute(Object params) throws EventSimErrorException {
				if (ackHere) {
					fatalError("Two acks received in a row");
				}
				else {
					ackHere= true;
					grantedRequest= -1;
					arbiter.release();
				}
			} // execute
		} // class AckHereCommand
		
	} // class MergeWorker
	
} // class Merge
