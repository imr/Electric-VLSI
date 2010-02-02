/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: VerilogTestHarnessTemplate.v
 * Written by Jonathan Gainsley, Sun Microsystems.
 *
 * Copyright (c) 2005 Sun Microsystems and Static Free Software
 *
 * Electric(tm) is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
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

/*
  Verilog Jtag Test Harness

  Provides tasks for implementing a jtag tester in verilog that will interface
  with the Chip Test Software.  The tester will provide interactive testing
  through a Verilog process running in interactive mode.  This encounters
  several very annoying subtleties which I describe below.

  1. Registers passed as arguments to tasks are not updated until the end of a task.
  Ex:
  task clockit;
    output clk;
    begin
      #100 clk = 1;
      #100 clk = 0;
    end
  endtask

  usage:
    myclk = 0;
    clockit(myclk);

  In the usage, myclk will never go high.  This is because outputs are not updated
  until the end of the task, at which point myclk will receive the value of 0.

  The solution is to instantiate a module that passes in signals as seen below, and
  set the signals as defined within the module during the task.  The module instantiation
  therefore handles passing of register values up the hierarchy.

  module clocker(aclk);
    output aclk;
    reg aclk;

    task clockit;
      begin
        #100 aclk = 1;
        #100 aclk = 0;
      end
    endtask
  endmodule

  usage:
    clocker aclocker(myclk);        // instantiate module
    myclk = 0;                      // initialize clock
    aclocker.clockit;               // clock the clock


  2. In interactive mode, one controls simulation using $stop, $continue, and breakpoints.
  Interactive mode provides a prompt from which the user can issue commands.  However, none
  of these commands are acted upon until the user issues a continue ('.') statement.  At this
  point simulation continues until a $stop or breakpoint is reached.  Thus the flag defined
  below, __task_done_flag__ serves as mechanism for allowing a single task to be run before
  returning control back to the user in the form of the interactive prompt.

  However, tasks that could be nested or called directly from the interactive prompt could
  not simply set __task_done_flag__ when done, as a sub-task may then interrupt simulation during a
  the calling task's simulation flow.  So, I take advantage of the fact that output registers are
  not updated until the end a task.  By passing the __task_done_flag__ as a flag to tasks, sub-tasks
  merely update the calling task's __task_done_flag__ local register.  It is only when the top-level
  task returns, that it sets the __task_done_flag__ defined in the test_bench module.  At this
  point control is returned to the user via the always statement that hits stop on posedge __task_done_flag__.

  */


/* Timing */
`timescale 1ps / 1ps

//INCLUDE

//`define TCK_INTERVAL     20000
//`define TRST_INTERVAL    5000
`define TCK_INTERVAL     8000
`define TRST_INTERVAL    1500


`define INSTRUCTION_REG_LENGTH 8

/* TMS sequences to access desired TAP states */
/* IMPORTANT: assumes sequence starts or finishes in idle state */

`define SHIFT_IR         4'b1100
`define SHIFT_IR_LEN     4
`define SHIFT_DR         3'b100
`define SHIFT_DR_LEN     3
`define CAPTURE_DR       2'b10
`define IDLE             3'b110  // returns to idle from any shift or capture state
`define IDLE_LEN         3
`define TAP_WALK         28'b1010010101101101001010111110  // tests otherwise unused state transitions
`define TAP_WALK_LEN     28


/*************************************************************************
 *
 *
 * Test Bench
 *
 *
 *************************************************************************/

module test_bench;

  supply1 vdd;
  supply0 gnd;

  reg __task_done_flag__;               // flag used to signal that an interactive task was completed
  reg [31:0] FID;                       // journal file identifier

/** IO */
//IO

/** Top Level Module */
//TOPMODULE

  initial begin

//INITIALBEGIN
    __task_done_flag__ = 0;
    #0

    $stop;
  end

  always @(posedge __task_done_flag__) begin
    #0 __task_done_flag__ = 0;
    #0 $stop;
  end

  // task to let simulation run for a certain period of time
  // usage: wait_ticks(100, done_flag)
  task wait_ticks;
    input ticks;
    output done_flag;
    integer ticks;
    begin
      #ticks done_flag = 1;
    end
  endtask

endmodule


/*************************************************************************
 *
 * JtagTester
 *
 *************************************************************************/
module jtagTester(tck, tms, tdi, trstb, tdob);
  output tck;
  output tms;
  output tdi;
  output trstb;
  input tdob;

  reg tck, tms, tdi, trstb;
  wire tdob;

  supply1 vdd;
  supply0 gnd;

  /**********************************************/
  /*                                            */
  /*  jtagTester task and function definitions  */
  /*                                            */
  /**********************************************/


    /* General Process:
      1. reset_jtag - resets TAP controller
      2. load_instruction - tells the controller which chain to use
      3. start_scan_data - tells the controller to switch to data
      4. scan_data - scan in the data (repeat this command as needed)
      5. end_scan_data - finishes scanning in data, returns controller to idle
      6. repeat from #3
    */

  // cycle the jtag clock a certain number of times
  // usage: cycle_tck(#, done_flag);
  task cycle_tck;
    input cycle_cnt;
    output done_flag;
    integer cycle_cnt;
    integer i;
    begin
      for (i = 1; i <= cycle_cnt; i = i+1) begin
        #`TCK_INTERVAL tck = 1;
        #`TCK_INTERVAL tck = 0;
      end
      done_flag = 1;
    end
  endtask

  // resets, then leaves TAP controller in an idle state
  // usage: reset_jtag(done_flag)
  task reset_jtag;
    output done_flag;
    begin
      trstb = 1;
      tck = 0;
      tms = 1;
      tdi = 0;
      #`TRST_INTERVAL
      $display("* Resetting the JTAG Controller *");
      trstb = 0;
      #`TRST_INTERVAL trstb = 1;
      tms = 0;
      #1 cycle_tck(1, done_flag);
      done_flag = 1;
    end
  endtask

  // steers TAP controller to a specified state
  // usage: goto(regi, regi_len, done_flag);
  task goto;
    input regi;
    input regi_len;
    output done_flag;
    integer regi, regi_len;
    integer i;
    begin
      for (i = regi_len-1; i >= 0; i = i-1) begin
        tms = regi[i];
        #1 cycle_tck(1, done_flag);
      end
      done_flag = 1;
    end
  endtask

  // steers TAP controller to a specified state,
  // but sends last bit of scanned out data
  // usage: goto_send_tdo(regi, regi_len, done_flag);
  task goto_send_tdo;
    input regi;
    input regi_len;
    output done_flag;
    integer regi, regi_len;
    integer sendLastBit;
    integer i;
    begin
      sendLastBit = 1;
      for (i = regi_len-1; i >= 0; i = i-1) begin
        tms = regi[i];
        if (sendLastBit == 1) begin
          sendLastBit = 0;
          #`TCK_INTERVAL tck = 1;
          $display("SCANDATAOUT: 1 %0b", tdob);
          #`TCK_INTERVAL tck = 0;
        end else begin
          #1 cycle_tck(1, done_flag);
        end
      end
      done_flag = 1;
    end
  endtask

  // forces TAP controller to idle state regardless of current state
  // usage: go_idle(done_flag)
  task go_idle;
    output done_flag;
    begin
      #1 tms = 1;
      #1 cycle_tck(5, done_flag);
      #1 tms = 0;
      #1 cycle_tck(1, done_flag);
      done_flag = 1;
    end
  endtask

  // Load an instruction into the jtag instruction register
  // An instruction is 8 bits: {rdEn, wrtEn, 0, 0, scanChainSelect[3:0]}
  // pass the instruction as an 8 bit binary number
  // usage: load_instruction(instruction, done_flag)
  task load_instruction;
    input instruction;
    output done_flag;
    reg[7:0] instruction;
    integer i;
    begin
      $display("* Loading Instruction %b", instruction);
      goto(`SHIFT_IR, `SHIFT_IR_LEN, done_flag);
      tdi = instruction[0];
      for (i = 1; i < `INSTRUCTION_REG_LENGTH; i = i + 1) begin
        #1 cycle_tck(1, done_flag);
        tdi = instruction[i];
      end // last bit of instruction will be shifted in below when we exit shift state
      #1 goto(`IDLE, `IDLE_LEN, done_flag);
      done_flag = 1;
    end
  endtask

  // Switch to scanning data mode
  // usage: start_scan_data(done_flag)
  task start_scan_data;
    output done_flag;
    begin
      goto(`SHIFT_DR, `SHIFT_DR_LEN, done_flag);
    end
  endtask

  // Scan in data.
  // NOTE!!! The last bit to scan in gets sent to end_scan_data, do not send it here.
  // bits is a 128 bit long register, i.e. 128'b10101...01, and bits_len is how many bits in
  // the register will be scanned in (1 to 128).
  // bits[0] gets scanned in first, bits[bits_len-1] gets scanned in last.
  // usage: scan_data(bits, bits_len, done_flag)
  task scan_data;
    input bits;
    input bits_len;
    output done_flag;
    reg [127:0] bits;
    integer bits_len;
    integer i;
    reg [127:0] bits_out;
    begin
      for (i = 0; i < bits_len; i = i + 1) begin
        tdi = bits[i];
        #`TCK_INTERVAL tck = 1;
        bits_out[i] = tdob;
        #`TCK_INTERVAL tck = 0;
      end
      $display("SCANDATAOUT: %0d %b", bits_len, bits_out);
      done_flag = 1;
    end
  endtask

  // Finish scanning data
  // Also pass in the last bit to scan in
  // usage: end_scan_data(lastbit, done_flag)
  task end_scan_data;
    input lastbit;
    output done_flag;
    begin
      tdi = lastbit;
      goto_send_tdo(`IDLE, `IDLE_LEN, done_flag);  // last bit scanned in during state change
      done_flag = 1;
    end
  endtask

endmodule

/*************************************************************************
 *
 * LogicSettable
 *
 *************************************************************************/
module logicSettable(port);
  output port;
  reg port;

  // set the port to a certain value
  // usage: set(value, done_flag)
  task set;
    input value;
    output done_flag;
    integer value;
    begin
//      #1 port = value;
      #1 force port = value;
      done_flag = 1;
    end
  endtask

  // get the value of the port
  // usage: get(done_flag)
  task get;
    output done_flag;
    begin
      #1 $display("GETVALUE: %d", port);
      done_flag = 1;
    end
  endtask

endmodule
