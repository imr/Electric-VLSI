/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: UserCom.java
 * Asynchronous Logic Simulator user functions
 * Original C Code written by Brent Serbin and Peter J. Gallant
 * Translated to Java by Steven M. Rubin, Sun Microsystems.
 *
 * Copyright (c) 2005 Sun Microsystems and Static Free Software
 *
 * Electric(tm) is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Electric(tm) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Electric(tm); see the file COPYING.  If not, write to
 * the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, Mass 02111-1307, USA.
 */
package com.sun.electric.tool.simulation.als;

import com.sun.electric.database.text.TextUtils;
import com.sun.electric.tool.simulation.Stimuli;

public class UserCom
{
	/**
	 * Class to model a bidirectional p-channel transistor.  If the
	 * routine is called, a new node summing calculation is performed and the effects
	 * of the gate output on the node are ignored.  This algorithm is used in the
	 * XEROX Aquarius simulation engine.
	 *
	 * Calling Arguments:
	 *	primhead = pointer to a structure containing the calling arguments for
	 *		 the user defined function.  The nodes are stored in the list
	 *		 in the following order: ctl, sidea, sideb
	 */
	static class PMOSTran extends ALS.UserProc
	{
		PMOSTran(ALS als) { nameMe(als, "PMOStran"); }

		void simulate(ALS.Model primhead)
		{
			ALS.ALSExport ctl = primhead.exptr;
			ALS.ALSExport [] side = new ALS.ALSExport[2];
			side[0] = ctl.next;
			side[1] = side[0].next;
		
			if (ctl.nodeptr.sum_state == Stimuli.LOGIC_HIGH)
			{
				simals_schedule_node_update(primhead, side[0], '=',
					Stimuli.LOGIC_X, Stimuli.OFF_STRENGTH, als.simals_time_abs);
				simals_schedule_node_update(primhead, side[1], '=',
					Stimuli.LOGIC_X, Stimuli.OFF_STRENGTH, als.simals_time_abs);
				return;
			}
			simals_calculate_bidir_outputs(primhead, side, Stimuli.GATE_STRENGTH);
		}
	}

	static class PMOSTranWeak extends ALS.UserProc
	{
		PMOSTranWeak(ALS als) { nameMe(als, "pMOStranWeak"); }

		void simulate(ALS.Model primhead)
		{
			ALS.ALSExport ctl = primhead.exptr;
			ALS.ALSExport [] side = new ALS.ALSExport[2];
			side[0] = ctl.next;
			side[1] = side[0].next;
		
			if (ctl.nodeptr.sum_state == Stimuli.LOGIC_HIGH)
			{
				simals_schedule_node_update(primhead, side[0], '=',
					Stimuli.LOGIC_X, Stimuli.OFF_STRENGTH, als.simals_time_abs);
				simals_schedule_node_update(primhead, side[1], '=',
					Stimuli.LOGIC_X, Stimuli.OFF_STRENGTH, als.simals_time_abs);
				return;
			}
			simals_calculate_bidir_outputs(primhead, side, Stimuli.NODE_STRENGTH);
		}
	}
		
	/**
	 * Class to model a bidirectional n-channel transistor.  If the
	 * routine is called, a new node summing calculation is performed and the effects
	 * of the gate output on the node are ignored.  This algorithm is used in the
	 * XEROX Aquarius simulation engine.
	 *
	 * Calling Arguments:
	 *	primhead = pointer to a structure containing the calling arguments for
	 *		 the user defined function.  The nodes are stored in the list
	 *		 in the following order: ctl, sidea, sideb
	 */
	static class NMOSTran extends ALS.UserProc
	{
		NMOSTran(ALS als) { nameMe(als, "nMOStran"); }

		void simulate(ALS.Model primhead)
		{
			ALS.ALSExport ctl = primhead.exptr;
			ALS.ALSExport [] side = new ALS.ALSExport[2];
			side[0] = ctl.next;
			side[1] = side[0].next;
		
			if (ctl.nodeptr.sum_state == Stimuli.LOGIC_LOW)
			{
				simals_schedule_node_update(primhead, side[0], '=',
					Stimuli.LOGIC_X, Stimuli.OFF_STRENGTH, als.simals_time_abs);
				simals_schedule_node_update(primhead, side[1], '=',
					Stimuli.LOGIC_X, Stimuli.OFF_STRENGTH, als.simals_time_abs);
				return;
			}
			simals_calculate_bidir_outputs(primhead, side, Stimuli.GATE_STRENGTH);
		}
	}

	static class NMOSTranWeak extends ALS.UserProc
	{
		NMOSTranWeak(ALS als) { nameMe(als, "nMOStranWeak"); }

		void simulate(ALS.Model primhead)
		{
			ALS.ALSExport ctl = primhead.exptr;
			ALS.ALSExport [] side = new ALS.ALSExport[2];
			side[0] = ctl.next;
			side[1] = side[0].next;
		
			if (ctl.nodeptr.sum_state == Stimuli.LOGIC_LOW)
			{
				simals_schedule_node_update(primhead, side[0], '=',
					Stimuli.LOGIC_X, Stimuli.OFF_STRENGTH, als.simals_time_abs);
				simals_schedule_node_update(primhead, side[1], '=',
					Stimuli.LOGIC_X, Stimuli.OFF_STRENGTH, als.simals_time_abs);
				return;
			}
			simals_calculate_bidir_outputs(primhead, side, Stimuli.NODE_STRENGTH);
		}
	}

	/**
	 * Class to fake out the function of a JK flipflop.
	 *
	 * Calling Arguments:
	 *	primhead = pointer to a structure containing the calling arguments for
	 *		 the user defined function.  The nodes are stored in the list
	 *		 in the following order: ck, j, k, q, qbar
	 */
	static class JKFlop extends ALS.UserProc
	{
		JKFlop(ALS als) { nameMe(als, "JKFFLOP"); }

		void simulate(ALS.Model primhead)
		{
			ALS.ALSExport argptr = primhead.exptr;
			int ck = argptr.nodeptr.sum_state;
			if (ck != Stimuli.LOGIC_LOW) return;
		
			argptr = argptr.next;
			int j = argptr.nodeptr.sum_state;
			argptr = argptr.next;
			int k = argptr.nodeptr.sum_state;
			argptr = argptr.next;
			ALS.ALSExport argptrbar = argptr.next;
		
			if (j == Stimuli.LOGIC_LOW)
			{
				if (k == Stimuli.LOGIC_LOW) return;
				simals_schedule_node_update(primhead, argptr, '=',
					Stimuli.LOGIC_LOW, Stimuli.GATE_STRENGTH, als.simals_time_abs);
				simals_schedule_node_update(primhead, argptrbar, '=',
					Stimuli.LOGIC_HIGH, Stimuli.GATE_STRENGTH, als.simals_time_abs);
				return;
			}
			if (k == Stimuli.LOGIC_LOW)
			{
				simals_schedule_node_update(primhead, argptr, '=',
					Stimuli.LOGIC_HIGH, Stimuli.GATE_STRENGTH, als.simals_time_abs);
				simals_schedule_node_update(primhead, argptrbar, '=',
					Stimuli.LOGIC_LOW, Stimuli.GATE_STRENGTH, als.simals_time_abs);
				return;
			}
		
			int out = argptr.nodeptr.sum_state;
			if (out == Stimuli.LOGIC_HIGH)
			{
				simals_schedule_node_update(primhead, argptr, '=',
					Stimuli.LOGIC_LOW, Stimuli.GATE_STRENGTH, als.simals_time_abs);
				simals_schedule_node_update(primhead, argptrbar, '=',
					Stimuli.LOGIC_HIGH, Stimuli.GATE_STRENGTH, als.simals_time_abs);
			} else
			{
				simals_schedule_node_update(primhead, argptr, '=',
					Stimuli.LOGIC_HIGH, Stimuli.GATE_STRENGTH, als.simals_time_abs);
				simals_schedule_node_update(primhead, argptrbar, '=',
				Stimuli.LOGIC_LOW, Stimuli.GATE_STRENGTH, als.simals_time_abs);
			}
		}
	}

	/**
	 * Class to mimic the function of a D flip flop.
	 *
	 * Arguments:
	 *	primhead = pointer to a structure containing the
	 *		 calling arguments for the user function.
	 *		The nodes are expected to appear in the
	 *		 parameter list in the following order:
	 *
	 *		 data_in, clk, q
	 */
	static class DFFlop extends ALS.UserProc
	{
		DFFlop(ALS als) { nameMe(als, "DFFLOP"); }

		void simulate(ALS.Model primhead)
		{
			ALS.ALSExport argptr = primhead.exptr;
		
			// data_in signal now selected
			int d_in = argptr.nodeptr.sum_state;
			argptr = argptr.next;
		
			// clk signal now selected
			int clk = argptr.nodeptr.sum_state;
		
			// do nothing if not a +ve clock edge
			if (clk != Stimuli.LOGIC_LOW) return;
		
			/* If this part of the procedure has been reached, the
				data_out signal should be updated since clk is high.
				Therefore, the value of data_out should be set to
				the value of d_in (one of LOGIC_LOW, LOGIC_HIGH,
				or LOGIC_X). */
		
			// select data_out signal
			argptr = argptr.next;
			int q = d_in;
			simals_schedule_node_update(primhead, argptr, '=',
				q, Stimuli.GATE_STRENGTH, als.simals_time_abs);
		}
	}

	/**
	 * Class to convert the value of 8 input bits into a state
	 * representation in the range 0x00-0xff.  This function can be called for
	 * the compact representation of the state of a bus structure in hexadecimal
	 * format.
	 *
	 * Calling Arguments:
	 *	primhead = pointer to a structure containing the calling arguments for
	 *		 the user defined function.  The nodes are ordered b7, b6, b5,
	 *		 b4, b3, b2, b1, b0, output in this list.
	 */
	static class BusToState extends ALS.UserProc
	{
		BusToState(ALS als) { nameMe(als, "BUS_TO_STATE"); }

		void simulate(ALS.Model primhead)
		{
			ALS.ALSExport argptr = primhead.exptr;
			int state = 0;
			for (int i = 7; i > -1; --i)
			{
				int bit = argptr.nodeptr.sum_state;
				if (bit == Stimuli.LOGIC_HIGH) state += (0x01 << i);
				argptr = argptr.next;
			}
			simals_schedule_node_update(primhead, argptr, '=',
				state, Stimuli.VDD_STRENGTH, als.simals_time_abs);
		}
	}

	/**
	 * Class to convert a value in the range of 0x00-0xff into an 8 bit
	 * logic representation (logic L = -1 and H = -3).  This function can be called to
	 * provide an easy means of representing bus values with a single integer entry.
	 *
	 * Calling Arguments:
	 *	primhead = pointer to a structure containing the calling arguments for
	 *		 the user defined function.  The nodes are stored in the list
	 *		 in the following order: input, b7, b6, b5, b4, b3, b2, b1, b0
	 */
	static class StateToBus extends ALS.UserProc
	{
		StateToBus(ALS als) { nameMe(als, "STATE_TO_BUS"); }

		void simulate(ALS.Model primhead)
		{
			ALS.ALSExport argptr = primhead.exptr;
			int input = argptr.nodeptr.sum_state;
		
			for (int i = 7; i > -1; --i)
			{
				argptr = argptr.next;
		
				int mask = (0x01 << i);
				if ((input & mask) != 0)
				{
					simals_schedule_node_update(primhead, argptr, '=',
						Stimuli.LOGIC_HIGH, Stimuli.VDD_STRENGTH, als.simals_time_abs);
				} else
				{
					simals_schedule_node_update(primhead, argptr, '=',
						Stimuli.LOGIC_LOW, Stimuli.VDD_STRENGTH, als.simals_time_abs);
				}
			}
		}
	}

	/************************** UNUSED USER FUNCTION ROUTINES **************************/
	
//	/**
//	 * Class is an event driven function which is called to calculate
//	 * statistics for nodes which are defined as queues.  Statistics are calculated
//	 * for the maximum number of tokens present, the number of tokens arriving
//	 * and departing, the time of last access, and the cumulative number of token
//	 * seconds.  These statistics are used to generate data regarding the average
//	 * number of tokens present in the queue and the amount of delay a token
//	 * experiences going through the queues in a simulation run.
//	 *
//	 * Calling Arguments:
//	 *	primhead = pointer to a structure containing the calling arguments for
//	 *		 the user defined function.  The nodes are stored in the list
//	 *		 in the following order: q_node
//	 */
//	static class QCalc extends ALS.UserProc
//	{
//		QCalc(ALS als) { nameMe(als, "Q_CALC"); }
//
//		void simulate(ALS.Model primhead)
//		{
//			ALS.ALSExport argptr = primhead.exptr;
//			ALS.Node nodehead = argptr.nodeptr;
//			ALS.Func funchead = (ALS.Func) primhead.ptr;
//			if (nodehead.maxsize == 0)
//			{
//				funchead.userint = 0;
//				funchead.userfloat = 0;
//			}
//		
//			nodehead.tk_sec += (nodehead.t_last - funchead.userfloat) * funchead.userint;
//			if (nodehead.sum_state > funchead.userint)
//			{
//				nodehead.arrive += nodehead.sum_state - funchead.userint;
//				if (nodehead.sum_state > nodehead.maxsize)
//					nodehead.maxsize = nodehead.sum_state;
//			} else
//			{
//				nodehead.depart += funchead.userint - nodehead.sum_state;
//			}
//		
//			funchead.userint = nodehead.sum_state;
//			funchead.userfloat = (float)nodehead.t_last;
//		}
//	}
	
//	/**
//	 * Class is an event driven function which is called to summarize
//	 * the number of characters arriving into a station.
//	 *
//	 * Calling Arguments:
//	 *	primhead = pointer to a structure containing the calling arguments for
//	 *		 the user defined function.  The nodes are stored in the list
//	 *		 in the following order: size
//	 */
//	static class Stats extends ALS.UserProc
//	{
//		Stats(ALS als) { nameMe(als, "STATS"); }
//
//		void simulate(ALS.Model primhead)
//		{
//			ALS.ALSExport argptr = primhead.exptr;
//			if ((argptr.nodeptr.sum_state % 100) != 0) return;
//		
//			System.out.println("Characters arriving into Station (N" + argptr.nodeptr.num +
//				") = " + argptr.nodeptr.sum_state + ", Time = " + als.simals_time_abs);
//		}
//	}

//	/**
//	 * Class fakes out the function of a synchronous reset counter.
//	 *
//	 * Calling Arguments:
//	 *	primhead = pointer to a structure containing the calling arguments for
//	 *		 the user defined function.  The nodes are stored in the list
//	 *		 in the following order: ck, reset, count, out
//	 */
//	static class Counter extends ALS.UserProc
//	{
//		Counter(ALS als) { nameMe(als, "COUNTER"); }
//
//		void simulate(ALS.Model primhead)
//		{
//			ALS.ALSExport argptr = primhead.exptr;
//			int ck = argptr.nodeptr.sum_state;
//			if (ck != Stimuli.LOGIC_HIGH) return;
//		
//			argptr = argptr.next;
//			int reset = argptr.nodeptr.sum_state;
//			argptr = argptr.next;
//			int count = argptr.nodeptr.sum_state;
//			ALS.ALSExport countptr = argptr;
//			argptr = argptr.next;
//			int out = argptr.nodeptr.sum_state;
//		
//			if (reset == Stimuli.LOGIC_LOW)
//			{
//				simals_schedule_node_update(primhead, countptr, '=',
//					0, Stimuli.GATE_STRENGTH, als.simals_time_abs + 30e-9);
//				simals_schedule_node_update(primhead, argptr, '=',
//						Stimuli.LOGIC_LOW, Stimuli.GATE_STRENGTH, als.simals_time_abs + 30e-9);
//				return;
//			}
//		
//			count = (count + 1) % 16;
//			simals_schedule_node_update(primhead, countptr, '=',
//				count, Stimuli.GATE_STRENGTH, als.simals_time_abs + 20e-9);
//		
//			if (count == 15)
//			{
//				simals_schedule_node_update(primhead, argptr, '=',
//					Stimuli.LOGIC_HIGH, Stimuli.GATE_STRENGTH, als.simals_time_abs + 18e-9);
//			} else
//			{
//				simals_schedule_node_update(primhead, argptr, '=',
//					Stimuli.LOGIC_LOW, Stimuli.GATE_STRENGTH, als.simals_time_abs + 22e-9);
//			}
//		}
//	}
	
//	/**
//	 * Class is an event driven function which is called to calculate
//	 * the binary exponential backoff time delay for an Ethernet system.
//	 *
//	 * Calling Arguments:
//	 *	primhead = pointer to a structure containing the calling arguments for
//	 *		 the user defined function.  The nodes are stored in the list
//	 *		 in the following order: retx, server
//	 */
//	static class DelayCalc extends ALS.UserProc
//	{
//		DelayCalc(ALS als) { nameMe(als, "DELAY_CALC"); }
//
//		void simulate(ALS.Model primhead)
//		{
//			double base_delay = 10e-6;
//		
//			ALS.ALSExport retx = primhead.exptr;
//			ALS.ALSExport server = retx.next;
//		
//			if (retx.nodeptr.sum_state == 0) return;
//		
//			double delay = 2.0 * Math.random() * base_delay *
//				(1 << (retx.nodeptr.sum_state - 1)) + als.simals_time_abs;
//			simals_schedule_node_update(primhead, server, '=',
//				1, Stimuli.VDD_STRENGTH, delay);
//			simals_schedule_node_update(primhead, server, '=',
//					Stimuli.LOGIC_X, Stimuli.OFF_STRENGTH, delay);
//		}
//	}
	
//	/**
//	 * Class simulates the operation of a FIFO element in a data
//	 * communication system.  Data is stored in a block of memory of fixed
//	 * size.  Read and write counters are used to index the memory array.  This
//	 * approach was chosen because this structure will simulate much faster than a
//	 * linked list.
//	 *
//	 * Calling Arguments:
//	 *	primhead = pointer to a structure containing the calling arguments for
//	 *		 the user defined function.  The nodes are stored in the list
//	 *		 in the following order: input, output, size
//	 */
//	private static final int FIFO_SIZE = 2048;
//
//	static class FIFOMem
//	{
//		int r_ptr;
//		int w_ptr;
//		int [] state;
//	}
//
//	static class FIFO extends ALS.UserProc
//	{
//		FIFO(ALS als) { nameMe(als, "FIFO"); }
//
//		void simulate(ALS.Model primhead)
//		{
//			ALS.ALSExport argptr = primhead.exptr;
//			ALS.ALSExport input_ptr = argptr;
//			argptr = argptr.next;
//			ALS.ALSExport output_ptr = argptr;
//			argptr = argptr.next;
//			ALS.ALSExport size_ptr = argptr;
//		
//			ALS.Func funchead = (ALS.Func) primhead.ptr;
//			FIFOMem fifohead = (FIFOMem) funchead.userptr;
//			if (fifohead == null)
//			{
//				fifohead = new FIFOMem();
//				fifohead.r_ptr = 0;
//				fifohead.w_ptr = 0;
//				fifohead.state = new int[FIFO_SIZE];
//				funchead.userptr = fifohead;
//			}
//		
//			if (input_ptr.nodeptr.sum_state > 0)
//			{
//				simals_schedule_node_update(primhead, input_ptr, '=',
//					0, Stimuli.VDD_STRENGTH, als.simals_time_abs);
//				simals_schedule_node_update(primhead, input_ptr, '=',
//					Stimuli.LOGIC_X, Stimuli.OFF_STRENGTH, als.simals_time_abs);
//				if (size_ptr.nodeptr.sum_state >= FIFO_SIZE)
//				{
//					System.out.println("Data loss has occured: Value = " + input_ptr.nodeptr.sum_state + ", Time = " + als.simals_time_abs);
//					return;
//				}
//				int new_size = size_ptr.nodeptr.sum_state + 1;
//				simals_schedule_node_update(primhead, size_ptr, '=',
//					new_size, Stimuli.VDD_STRENGTH, als.simals_time_abs);
//				simals_schedule_node_update(primhead, size_ptr, '=',
//					Stimuli.LOGIC_X, Stimuli.OFF_STRENGTH, als.simals_time_abs);
//				fifohead.state[fifohead.w_ptr] = input_ptr.nodeptr.sum_state;
//				fifohead.w_ptr = ((fifohead.w_ptr) + 1) % FIFO_SIZE;
//			}
//		
//			if (output_ptr.nodeptr.sum_state == 0 && size_ptr.nodeptr.sum_state != 0)
//			{
//				simals_schedule_node_update(primhead, output_ptr, '=',
//					fifohead.state[fifohead.r_ptr], Stimuli.VDD_STRENGTH, als.simals_time_abs);
//				simals_schedule_node_update(primhead, output_ptr, '=',
//					Stimuli.LOGIC_X, Stimuli.OFF_STRENGTH, als.simals_time_abs);
//				int new_size = size_ptr.nodeptr.sum_state - 1;
//				simals_schedule_node_update(primhead, size_ptr, '=',
//					new_size, Stimuli.VDD_STRENGTH, als.simals_time_abs);
//				simals_schedule_node_update(primhead, size_ptr, '=',
//						Stimuli.LOGIC_X, Stimuli.OFF_STRENGTH, als.simals_time_abs);
//				fifohead.r_ptr = ((fifohead.r_ptr) + 1) % FIFO_SIZE;
//			}
//		}
//	}
	
//	/**
//	 * Class examines the bus to see if a message is destined for it.
//	 * If so, the data value is examined to determine if the link that the data byte
//	 * passed across is in a "congested" state.  If so this element must fire an
//	 * XOFF character to the station on the remote side of the connection.  The
//	 * data value is also passed to a gate which will examine the byte and increment
//	 * the appropriate counter.
//	 *
//	 * Calling Arguments:
//	 *	primhead = pointer to a structure containing the calling arguments for
//	 *		 the user defined function.  The nodes are stored in the list
//	 *		 in the following order: address, add_in, data_in, data_type,
//	 *					 rem_xoff, add_out, data_out
//	 */
//	static class RXData extends ALS.UserProc
//	{
//		RXData(ALS als) { nameMe(als, "RX_DATA"); }
//
//		void simulate(ALS.Model primhead)
//		{		
//			ALS.ALSExport address = primhead.exptr;
//			ALS.ALSExport add_in = address.next;
//			if (address.nodeptr.sum_state != add_in.nodeptr.sum_state) return;
//			ALS.ALSExport data_in = add_in.next;
//			ALS.ALSExport data_type = data_in.next;
//			ALS.ALSExport rem_xoff = data_type.next;
//			ALS.ALSExport add_out = rem_xoff.next;
//			ALS.ALSExport data_out = add_out.next;
//		
//			simals_schedule_node_update(primhead, add_in, '=',
//				0, Stimuli.VDD_STRENGTH, als.simals_time_abs);
//			simals_schedule_node_update(primhead, add_in, '=',
//				Stimuli.LOGIC_X, Stimuli.OFF_STRENGTH, als.simals_time_abs);
//			simals_schedule_node_update(primhead, data_in, '=',
//				0, Stimuli.VDD_STRENGTH, als.simals_time_abs);
//			simals_schedule_node_update(primhead, data_in, '=',
//				Stimuli.LOGIC_X, Stimuli.OFF_STRENGTH, als.simals_time_abs);
//			if (((data_in.nodeptr.sum_state) % 2) != 0)
//			{
//				simals_schedule_node_update(primhead, data_type, '=',
//					data_in.nodeptr.sum_state, Stimuli.VDD_STRENGTH, als.simals_time_abs);
//				simals_schedule_node_update(primhead, data_type, '=',
//					Stimuli.LOGIC_X, Stimuli.OFF_STRENGTH, als.simals_time_abs);
//				if (rem_xoff.nodeptr.sum_state != 0)
//				{
//					simals_schedule_node_update(primhead, rem_xoff, '=',
//						0, Stimuli.VDD_STRENGTH, als.simals_time_abs);
//					simals_schedule_node_update(primhead, rem_xoff, '=',
//						Stimuli.LOGIC_X, Stimuli.OFF_STRENGTH, als.simals_time_abs);
//					simals_schedule_node_update(primhead, add_out, '=',
//						address.nodeptr.sum_state, Stimuli.VDD_STRENGTH, als.simals_time_abs + 50e-6);
//					simals_schedule_node_update(primhead, add_out, '=',
//						Stimuli.LOGIC_X, Stimuli.OFF_STRENGTH, als.simals_time_abs + 50e-6);
//					simals_schedule_node_update(primhead, data_out, '=',
//						5, Stimuli.VDD_STRENGTH, als.simals_time_abs + 50e-6);
//					simals_schedule_node_update(primhead, data_out, '=',
//						Stimuli.LOGIC_X, Stimuli.OFF_STRENGTH, als.simals_time_abs + 50e-6);
//				}
//			} else
//			{
//				simals_schedule_node_update(primhead, data_type, '=',
//					((data_in.nodeptr.sum_state) - 1), Stimuli.VDD_STRENGTH, als.simals_time_abs);
//				simals_schedule_node_update(primhead, data_type, '=',
//						Stimuli.LOGIC_X, Stimuli.OFF_STRENGTH, als.simals_time_abs);
//				if (rem_xoff.nodeptr.sum_state == 0)
//				{
//					simals_schedule_node_update(primhead, rem_xoff, '=',
//						1, Stimuli.VDD_STRENGTH, als.simals_time_abs);
//					simals_schedule_node_update(primhead, rem_xoff, '=',
//						Stimuli.LOGIC_X, Stimuli.OFF_STRENGTH, als.simals_time_abs);
//					simals_schedule_node_update(primhead, add_out, '=',
//						address.nodeptr.sum_state, Stimuli.VDD_STRENGTH, als.simals_time_abs + 50e-6);
//					simals_schedule_node_update(primhead, add_out, '=',
//						Stimuli.LOGIC_X, Stimuli.OFF_STRENGTH, als.simals_time_abs + 50e-6);
//					simals_schedule_node_update(primhead, data_out, '=',
//						7, Stimuli.VDD_STRENGTH, als.simals_time_abs + 50e-6);
//					simals_schedule_node_update(primhead, data_out, '=',
//							Stimuli.LOGIC_X, Stimuli.OFF_STRENGTH, als.simals_time_abs + 50e-6);
//				}
//			}
//		}
//	}
	
//	static class AFRegisters extends ALS.UserProc
//	{
//		AFRegisters(ALS als) { nameMe(als, "A_F_REGISTERS"); }
//
//		void simulate(ALS.Model primhead)
//		{
//			ALS.ALSExport ck = primhead.exptr;
//			ALS.ALSExport ain = ck.next;
//			ALS.ALSExport aload = ain.next;
//			ALS.ALSExport fin = aload.next;
//			ALS.ALSExport fload = fin.next;
//			ALS.ALSExport amid = fload.next;
//			ALS.ALSExport aout = amid.next;
//			ALS.ALSExport fmid = aout.next;
//			ALS.ALSExport fout = fmid.next;
//		
//			if (ck.nodeptr.sum_state == Stimuli.LOGIC_HIGH)
//			{
//				if (aload.nodeptr.sum_state == Stimuli.LOGIC_HIGH)
//				{
//					simals_schedule_node_update(primhead, amid, '=',
//						ain.nodeptr.sum_state, Stimuli.GATE_STRENGTH, als.simals_time_abs + 3.0e-9);
//				}
//				if (fload.nodeptr.sum_state == Stimuli.LOGIC_HIGH)
//				{
//					simals_schedule_node_update(primhead, fmid, '=',
//						fin.nodeptr.sum_state, Stimuli.GATE_STRENGTH, als.simals_time_abs + 3.0e-9);
//				}
//				return;
//			}
//		
//			if (ck.nodeptr.sum_state == Stimuli.LOGIC_LOW)
//			{
//				if (aload.nodeptr.sum_state == Stimuli.LOGIC_HIGH)
//				{
//					simals_schedule_node_update(primhead, aout, '=',
//						amid.nodeptr.sum_state, Stimuli.GATE_STRENGTH, als.simals_time_abs + 3.0e-9);
//				}
//				if (fload.nodeptr.sum_state == Stimuli.LOGIC_HIGH)
//				{
//					simals_schedule_node_update(primhead, fout, '=',
//						fmid.nodeptr.sum_state, Stimuli.GATE_STRENGTH, als.simals_time_abs + 3.0e-9);
//				}
//				return;
//			}
//		
//			simals_schedule_node_update(primhead, amid, '=',
//				Stimuli.LOGIC_X, Stimuli.GATE_STRENGTH, als.simals_time_abs + 3.0e-9);
//			simals_schedule_node_update(primhead, aout, '=',
//				Stimuli.LOGIC_X, Stimuli.GATE_STRENGTH, als.simals_time_abs + 3.0e-9);
//			simals_schedule_node_update(primhead, fmid, '=',
//				Stimuli.LOGIC_X, Stimuli.GATE_STRENGTH, als.simals_time_abs + 3.0e-9);
//			simals_schedule_node_update(primhead, aout, '=',
//				Stimuli.LOGIC_X, Stimuli.GATE_STRENGTH, als.simals_time_abs + 3.0e-9);
//		}
//	}
	
//	static class ControlLogic extends ALS.UserProc
//	{
//		ControlLogic(ALS als) { nameMe(als, "CONTROL_LOGIC"); }
//
//		void simulate(ALS.Model primhead)
//		{
//			ALS.ALSExport ain = primhead.exptr;
//			ALS.ALSExport fin = ain.next;
//			ALS.ALSExport b = fin.next;
//			ALS.ALSExport msb = b.next;
//			ALS.ALSExport aout = msb.next;
//			ALS.ALSExport fout = aout.next;
//		
//			if (b.nodeptr.sum_state == Stimuli.LOGIC_HIGH)
//			{
//				simals_schedule_node_update(primhead, aout, '=',
//					ain.nodeptr.sum_state, Stimuli.GATE_STRENGTH, als.simals_time_abs + 3.0e-9);
//			} else
//			{
//				 if (b.nodeptr.sum_state == Stimuli.LOGIC_LOW)
//				 {
//					simals_schedule_node_update(primhead, aout, '=',
//						Stimuli.LOGIC_LOW, Stimuli.GATE_STRENGTH, als.simals_time_abs + 3.0e-9);
//				 } else
//				 {
//					simals_schedule_node_update(primhead, aout, '=',
//						Stimuli.LOGIC_X, Stimuli.GATE_STRENGTH, als.simals_time_abs + 3.0e-9);
//				 }
//			}
//		
//			if (msb.nodeptr.sum_state == Stimuli.LOGIC_HIGH)
//			{
//				simals_schedule_node_update(primhead, fout, '=',
//					fin.nodeptr.sum_state, Stimuli.GATE_STRENGTH, als.simals_time_abs + 3.0e-9);
//			} else
//			{
//				if (msb.nodeptr.sum_state == Stimuli.LOGIC_LOW)
//				{
//					simals_schedule_node_update(primhead, fout, '=',
//						Stimuli.LOGIC_LOW, Stimuli.GATE_STRENGTH, als.simals_time_abs + 3.0e-9);
//				} else
//				{
//					simals_schedule_node_update(primhead, fout, '=',
//						Stimuli.LOGIC_X, Stimuli.GATE_STRENGTH, als.simals_time_abs + 3.0e-9);
//				}
//			}
//		}
//	}

//	static class Mod2Adder extends ALS.UserProc
//	{
//		Mod2Adder(ALS als) { nameMe(als, "MOD2_ADDER"); }
//
//		void simulate(ALS.Model primhead)
//		{
//			ALS.ALSExport ain = primhead.exptr;
//			ALS.ALSExport fin = ain.next;
//			ALS.ALSExport pin = fin.next;
//			ALS.ALSExport ck = pin.next;
//			ALS.ALSExport out = ck.next;
//		
//			if (ck.nodeptr.sum_state == Stimuli.LOGIC_LOW)
//			{
//				simals_schedule_node_update(primhead, out, '=',
//					Stimuli.LOGIC_LOW, Stimuli.GATE_STRENGTH, als.simals_time_abs + 3.0e-9);
//				return;
//			}
//
//			int sum = 0;
//			if (ck.nodeptr.sum_state == Stimuli.LOGIC_HIGH)
//			{
//				if (ain.nodeptr.sum_state == Stimuli.LOGIC_HIGH) ++sum;
//				if (fin.nodeptr.sum_state == Stimuli.LOGIC_HIGH) ++sum;
//				if (pin.nodeptr.sum_state == Stimuli.LOGIC_HIGH) ++sum;
//		
//				sum %= 2;
//				if (sum != 0)
//				{
//					simals_schedule_node_update(primhead, out, '=',
//						Stimuli.LOGIC_HIGH, Stimuli.GATE_STRENGTH, als.simals_time_abs + 5.0e-9);
//				} else
//				{
//					simals_schedule_node_update(primhead, out, '=',
//						Stimuli.LOGIC_LOW, Stimuli.GATE_STRENGTH, als.simals_time_abs + 5.0e-9);
//				}
//				return;
//			}
//		
//			simals_schedule_node_update(primhead, out, '=',
//				Stimuli.LOGIC_X, Stimuli.GATE_STRENGTH, als.simals_time_abs + 5.0e-9);
//		}
//	}

//	static class AboveAdder extends ALS.UserProc
//	{
//		AboveAdder(ALS als) { nameMe(als, "ABOVE_ADDER"); }
//
//		void simulate(ALS.Model primhead)
//		{
//			ALS.ALSExport ck = primhead.exptr;
//			ALS.ALSExport sync = ck.next;
//			ALS.ALSExport load_osr = sync.next;
//			ALS.ALSExport sum_in = load_osr.next;
//			ALS.ALSExport osr_in = sum_in.next;
//			ALS.ALSExport osr_mid = osr_in.next;
//			ALS.ALSExport osr_out = osr_mid.next;
//			ALS.ALSExport pmid = osr_out.next;
//			ALS.ALSExport pout = pmid.next;
//		
//			if (ck.nodeptr.sum_state == Stimuli.LOGIC_HIGH)
//			{
//				if (load_osr.nodeptr.sum_state == Stimuli.LOGIC_LOW)
//				{
//					simals_schedule_node_update(primhead, pmid, '=',
//						sum_in.nodeptr.sum_state, Stimuli.GATE_STRENGTH, als.simals_time_abs + 3.0e-9);
//					simals_schedule_node_update(primhead, osr_mid, '=',
//						osr_in.nodeptr.sum_state, Stimuli.GATE_STRENGTH, als.simals_time_abs + 3.0e-9);
//				}
//				if (load_osr.nodeptr.sum_state == Stimuli.LOGIC_HIGH)
//				{
//					simals_schedule_node_update(primhead, osr_mid, '=',
//						sum_in.nodeptr.sum_state, Stimuli.GATE_STRENGTH, als.simals_time_abs + 3.0e-9);
//				}
//				if (sync.nodeptr.sum_state == Stimuli.LOGIC_HIGH)
//				{
//					simals_schedule_node_update(primhead, pmid, '=',
//							Stimuli.LOGIC_LOW, Stimuli.GATE_STRENGTH, als.simals_time_abs + 3.0e-9);
//				}
//				return;
//			}
//		
//			if (ck.nodeptr.sum_state == Stimuli.LOGIC_LOW)
//			{
//				if (load_osr.nodeptr.sum_state == Stimuli.LOGIC_LOW)
//				{
//					simals_schedule_node_update(primhead, osr_out, '=',
//						osr_mid.nodeptr.sum_state, Stimuli.GATE_STRENGTH, als.simals_time_abs + 3.0e-9);
//					simals_schedule_node_update(primhead, pout, '=',
//						pmid.nodeptr.sum_state, Stimuli.GATE_STRENGTH, als.simals_time_abs + 3.0e-9);
//				}
//				return;
//			}
//		
//			simals_schedule_node_update(primhead, osr_mid, '=',
//					Stimuli.LOGIC_X, Stimuli.GATE_STRENGTH, als.simals_time_abs + 3.0e-9);
//			simals_schedule_node_update(primhead, osr_out, '=',
//					Stimuli.LOGIC_X, Stimuli.GATE_STRENGTH, als.simals_time_abs + 3.0e-9);
//			simals_schedule_node_update(primhead, pmid, '=',
//					Stimuli.LOGIC_X, Stimuli.GATE_STRENGTH, als.simals_time_abs + 3.0e-9);
//			simals_schedule_node_update(primhead, pout, '=',
//					Stimuli.LOGIC_X, Stimuli.GATE_STRENGTH, als.simals_time_abs + 3.0e-9);
//		}
//	}

//	/**
//	 * Class converts the value of 12 input bits into a state
//	 * representation in the range.  This function can be called for
//	 * the compact representation of the state of a bus structure in hexadecimal
//	 * format.
//	 *
//	 * Calling Arguments:
//	 *	primhead = pointer to a structure containing the calling arguments for
//	 *		 the user defined function.  The nodes are ordered b7, b6, b5,
//	 *		 b4, b3, b2, b1, b0, output in this list.
//	 */
//	static class Bus12ToState extends ALS.UserProc
//	{
//		Bus12ToState(ALS als) { nameMe(als, "BUS12_TO_STATE"); }
//
//		void simulate(ALS.Model primhead)
//		{
//			ALS.ALSExport argptr = primhead.exptr;
//			int state = 0;
//			for (int i = 11; i > -1; --i)
//			{
//				int bit = argptr.nodeptr.sum_state;
//				if (bit == Stimuli.LOGIC_HIGH) state += (0x01 << i);
//				argptr = argptr.next;
//			}
//		
//			simals_schedule_node_update(primhead, argptr, '=',
//				state, Stimuli.VDD_STRENGTH, als.simals_time_abs);
//		}
//	}
}
