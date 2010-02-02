/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: jtagController.v
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

/* Verilog for cell testCell{sch} from Library jtag */
/* Created on Tue April 26, 2005 11:27:36 */
/* Last revised on Tue April 26, 2005 11:29:37 */
/* Written on Tue April 26, 2005 11:30:54 by Electric VLSI Design System, version 8.02l */

module redFour__NMOSwk_X_1_Delay_100(g, d, s);
  input g;
  input d;
  input s;

  supply0 gnd;
  rtranif1 #(100) NMOSfwk_0 (d, s, g);
endmodule   /* redFour__NMOSwk_X_1_Delay_100 */

module redFour__PMOSwk_X_0_833_Delay_100(g, d, s);
  input g;
  input d;
  input s;

  supply1 vdd;
  rtranif0 #(100) PMOSfwk_0 (d, s, g);
endmodule   /* redFour__PMOSwk_X_0_833_Delay_100 */

module scanChainFive__scanL(in, out);
  input in;
  output out;

  supply1 vdd;
  supply0 gnd;
  wire net_4, net_7;

  redFour__NMOSwk_X_1_Delay_100 NMOSwk_0(.g(out), .d(in), .s(net_7));
  redFour__NMOSwk_X_1_Delay_100 NMOSwk_1(.g(out), .d(net_7), .s(gnd));
  redFour__PMOSwk_X_0_833_Delay_100 PMOSwk_0(.g(out), .d(net_4), .s(vdd));
  redFour__PMOSwk_X_0_833_Delay_100 PMOSwk_1(.g(out), .d(in), .s(net_4));
  not (strong0, strong1) #(100) invV_0 (out, in);
endmodule   /* scanChainFive__scanL */

module redFour__NMOS_X_6_667_Delay_100(g, d, s);
  input g;
  input d;
  input s;

  supply0 gnd;
  tranif1 #(100) NMOSf_0 (d, s, g);
endmodule   /* redFour__NMOS_X_6_667_Delay_100 */

module redFour__PMOS_X_3_333_Delay_100(g, d, s);
  input g;
  input d;
  input s;

  supply1 vdd;
  tranif0 #(100) PMOSf_0 (d, s, g);
endmodule   /* redFour__PMOS_X_3_333_Delay_100 */

module scanChainFive__scanP(in, src, drn);
  input in;
  input src;
  output drn;

  supply1 vdd;
  supply0 gnd;
  wire net_1;

  redFour__NMOS_X_6_667_Delay_100 NMOS_0(.g(in), .d(drn), .s(src));
  redFour__PMOS_X_3_333_Delay_100 PMOS_0(.g(net_1), .d(drn), .s(src));
  not (strong0, strong1) #(0) inv_0 (net_1, in);
endmodule   /* scanChainFive__scanP */

module scanChainFive__scanRL(phi1, phi2, rd, sin, sout);
  input phi1;
  input phi2;
  input rd;
  input sin;
  output sout;

  supply1 vdd;
  supply0 gnd;
  wire net_0, net_2, net_3;

  scanChainFive__scanL foo1(.in(net_2), .out(net_3));
  scanChainFive__scanL foo2(.in(net_0), .out(sout));
  scanChainFive__scanP scanP_0(.in(rd), .src(vdd), .drn(net_0));
  scanChainFive__scanP scanP_1(.in(phi1), .src(net_3), .drn(net_0));
  scanChainFive__scanP scanP_2(.in(phi2), .src(sin), .drn(net_2));
endmodule   /* scanChainFive__scanRL */

module jtag__BR(SDI, phi1, phi2, read, SDO);
  input SDI;
  input phi1;
  input phi2;
  input read;
  output SDO;

  supply1 vdd;
  supply0 gnd;
  scanChainFive__scanRL scanRL_0(.phi1(phi1), .phi2(phi2), .rd(read), 
      .sin(SDI), .sout(SDO));
endmodule   /* jtag__BR */

module scanChainFive__scanIRH(mclr, phi1, phi2, rd, sin, wr, dout, doutb, 
      sout);
  input mclr;
  input phi1;
  input phi2;
  input rd;
  input sin;
  input wr;
  output dout;
  output doutb;
  output sout;

  supply1 vdd;
  supply0 gnd;
  wire net_2, net_4, net_6, net_7;

  scanChainFive__scanL foo1(.in(net_6), .out(net_7));
  scanChainFive__scanL foo2(.in(net_2), .out(sout));
  scanChainFive__scanL foo3(.in(net_4), .out(doutb));
  not (strong0, strong1) #(100) invLT_0 (dout, doutb);
  scanChainFive__scanP scanP_0(.in(wr), .src(sout), .drn(net_4));
  scanChainFive__scanP scanP_1(.in(rd), .src(gnd), .drn(net_2));
  scanChainFive__scanP scanP_2(.in(mclr), .src(vdd), .drn(net_4));
  scanChainFive__scanP scanP_3(.in(phi1), .src(net_7), .drn(net_2));
  scanChainFive__scanP scanP_4(.in(phi2), .src(sin), .drn(net_6));
endmodule   /* scanChainFive__scanIRH */

module scanChainFive__scanIRL(mclr, phi1, phi2, rd, sin, wr, dout, doutb, 
      sout);
  input mclr;
  input phi1;
  input phi2;
  input rd;
  input sin;
  input wr;
  output dout;
  output doutb;
  output sout;

  supply1 vdd;
  supply0 gnd;
  wire net_2, net_3, net_4, net_6;

  scanChainFive__scanL foo1(.in(net_2), .out(net_3));
  scanChainFive__scanL foo2(.in(net_4), .out(sout));
  scanChainFive__scanL foo3(.in(net_6), .out(doutb));
  not (strong0, strong1) #(100) invLT_0 (dout, doutb);
  scanChainFive__scanP scanP_0(.in(rd), .src(vdd), .drn(net_4));
  scanChainFive__scanP scanP_1(.in(mclr), .src(vdd), .drn(net_6));
  scanChainFive__scanP scanP_2(.in(wr), .src(sout), .drn(net_6));
  scanChainFive__scanP scanP_3(.in(phi1), .src(net_3), .drn(net_4));
  scanChainFive__scanP scanP_4(.in(phi2), .src(sin), .drn(net_2));
endmodule   /* scanChainFive__scanIRL */

module jtag__IR(SDI, phi1, phi2, read, reset, write, IR, IRb, SDO);
  input SDI;
  input phi1;
  input phi2;
  input read;
  input reset;
  input write;
  output [8:1] IR;
  output [8:1] IRb;
  output SDO;

  supply1 vdd;
  supply0 gnd;
  wire net_1, net_2, net_3, net_4, net_5, net_6, net_7;

  scanChainFive__scanIRH scanIRH_0(.mclr(reset), .phi1(phi1), .phi2(phi2), 
      .rd(read), .sin(net_1), .wr(write), .dout(IR[1]), .doutb(IRb[1]), 
      .sout(SDO));
  scanChainFive__scanIRL scanIRL_0(.mclr(reset), .phi1(phi1), .phi2(phi2), 
      .rd(read), .sin(net_3), .wr(write), .dout(IR[7]), .doutb(IRb[7]), 
      .sout(net_2));
  scanChainFive__scanIRL scanIRL_1(.mclr(reset), .phi1(phi1), .phi2(phi2), 
      .rd(read), .sin(net_5), .wr(write), .dout(IR[5]), .doutb(IRb[5]), 
      .sout(net_4));
  scanChainFive__scanIRL scanIRL_2(.mclr(reset), .phi1(phi1), .phi2(phi2), 
      .rd(read), .sin(net_2), .wr(write), .dout(IR[6]), .doutb(IRb[6]), 
      .sout(net_5));
  scanChainFive__scanIRL scanIRL_3(.mclr(reset), .phi1(phi1), .phi2(phi2), 
      .rd(read), .sin(net_7), .wr(write), .dout(IR[3]), .doutb(IRb[3]), 
      .sout(net_6));
  scanChainFive__scanIRL scanIRL_4(.mclr(reset), .phi1(phi1), .phi2(phi2), 
      .rd(read), .sin(net_6), .wr(write), .dout(IR[2]), .doutb(IRb[2]), 
      .sout(net_1));
  scanChainFive__scanIRL scanIRL_5(.mclr(reset), .phi1(phi1), .phi2(phi2), 
      .rd(read), .sin(net_4), .wr(write), .dout(IR[4]), .doutb(IRb[4]), 
      .sout(net_7));
  scanChainFive__scanIRL scanIRL_6(.mclr(reset), .phi1(phi1), .phi2(phi2), 
      .rd(read), .sin(SDI), .wr(write), .dout(IR[8]), .doutb(IRb[8]), 
      .sout(net_3));
endmodule   /* jtag__IR */

module redFour__nor2n_X_3_Delay_100_drive0_strong0_drive1_strong1(ina, inb, 
      out);
  input ina;
  input inb;
  output out;

  supply1 vdd;
  supply0 gnd;
  nor (strong0, strong1) #(100) nor2_0 (out, ina, inb);
endmodule   /* redFour__nor2n_X_3_Delay_100_drive0_strong0_drive1_strong1 */

module jtag__IRdecode(IR, IRb, Bypass, ExTest, SamplePreload, ScanPath);
  input [4:1] IR;
  input [4:1] IRb;
  output Bypass;
  output ExTest;
  output SamplePreload;
  output [12:0] ScanPath;

  supply1 vdd;
  supply0 gnd;
  wire H00, H01, H10, H11, L00, L01, L10, L11, net_19, net_21, net_23, net_25;
  wire net_26, net_27, net_28, net_29, net_30, net_31, net_32, net_33, net_34;
  wire net_35, net_36, net_37;

  not (strong0, strong1) #(100) inv_0 (Bypass, net_19);
  not (strong0, strong1) #(100) inv_1 (SamplePreload, net_21);
  not (strong0, strong1) #(100) inv_2 (ExTest, net_23);
  not (strong0, strong1) #(100) inv_3 (ScanPath[12], net_25);
  not (strong0, strong1) #(100) inv_4 (ScanPath[11], net_26);
  not (strong0, strong1) #(100) inv_5 (ScanPath[10], net_27);
  not (strong0, strong1) #(100) inv_6 (ScanPath[9], net_28);
  not (strong0, strong1) #(100) inv_7 (ScanPath[8], net_29);
  not (strong0, strong1) #(100) inv_8 (ScanPath[7], net_30);
  not (strong0, strong1) #(100) inv_9 (ScanPath[6], net_31);
  not (strong0, strong1) #(100) inv_10 (ScanPath[5], net_32);
  not (strong0, strong1) #(100) inv_11 (ScanPath[4], net_33);
  not (strong0, strong1) #(100) inv_12 (ScanPath[3], net_34);
  not (strong0, strong1) #(100) inv_13 (ScanPath[2], net_35);
  not (strong0, strong1) #(100) inv_14 (ScanPath[1], net_36);
  not (strong0, strong1) #(100) inv_15 (ScanPath[0], net_37);
  nand (strong0, strong1) #(100) nand2_0 (net_19, L11, H11);
  nand (strong0, strong1) #(100) nand2_1 (net_21, L10, H11);
  nand (strong0, strong1) #(100) nand2_2 (net_23, L01, H11);
  nand (strong0, strong1) #(100) nand2_3 (net_25, L00, H11);
  nand (strong0, strong1) #(100) nand2_4 (net_26, L11, H10);
  nand (strong0, strong1) #(100) nand2_5 (net_27, L10, H10);
  nand (strong0, strong1) #(100) nand2_6 (net_28, L01, H10);
  nand (strong0, strong1) #(100) nand2_7 (net_29, L00, H10);
  nand (strong0, strong1) #(100) nand2_8 (net_30, L11, H01);
  nand (strong0, strong1) #(100) nand2_9 (net_31, L10, H01);
  nand (strong0, strong1) #(100) nand2_10 (net_32, L01, H01);
  nand (strong0, strong1) #(100) nand2_11 (net_33, L00, H01);
  nand (strong0, strong1) #(100) nand2_12 (net_34, L11, H00);
  nand (strong0, strong1) #(100) nand2_13 (net_35, L10, H00);
  nand (strong0, strong1) #(100) nand2_14 (net_36, L01, H00);
  nand (strong0, strong1) #(100) nand2_15 (net_37, L00, H00);
  redFour__nor2n_X_3_Delay_100_drive0_strong0_drive1_strong1 
      nor2n_0(.ina(IR[1]), .inb(IR[2]), .out(L00));
  redFour__nor2n_X_3_Delay_100_drive0_strong0_drive1_strong1 
      nor2n_1(.ina(IRb[1]), .inb(IR[2]), .out(L01));
  redFour__nor2n_X_3_Delay_100_drive0_strong0_drive1_strong1 
      nor2n_2(.ina(IR[1]), .inb(IRb[2]), .out(L10));
  redFour__nor2n_X_3_Delay_100_drive0_strong0_drive1_strong1 
      nor2n_3(.ina(IRb[1]), .inb(IRb[2]), .out(L11));
  redFour__nor2n_X_3_Delay_100_drive0_strong0_drive1_strong1 
      nor2n_4(.ina(IR[3]), .inb(IR[4]), .out(H00));
  redFour__nor2n_X_3_Delay_100_drive0_strong0_drive1_strong1 
      nor2n_5(.ina(IRb[3]), .inb(IR[4]), .out(H01));
  redFour__nor2n_X_3_Delay_100_drive0_strong0_drive1_strong1 
      nor2n_6(.ina(IR[3]), .inb(IRb[4]), .out(H10));
  redFour__nor2n_X_3_Delay_100_drive0_strong0_drive1_strong1 
      nor2n_7(.ina(IRb[3]), .inb(IRb[4]), .out(H11));
endmodule   /* jtag__IRdecode */

module redFour__PMOSwk_X_0_222_Delay_100(g, d, s);
  input g;
  input d;
  input s;

  supply1 vdd;
  rtranif0 #(100) PMOSfwk_0 (d, s, g);
endmodule   /* redFour__PMOSwk_X_0_222_Delay_100 */

module jtag__clockGen(clk, phi1_fb, phi2_fb, phi1_out, phi2_out);
  input clk;
  input phi1_fb;
  input phi2_fb;
  output phi1_out;
  output phi2_out;

  supply1 vdd;
  supply0 gnd;
  wire net_0, net_1, net_3, net_4, net_6;

  not (strong0, strong1) #(100) inv_0 (phi2_out, net_3);
  not (strong0, strong1) #(100) inv_1 (phi1_out, net_6);
  not (strong0, strong1) #(100) inv_2 (net_4, clk);
  not (strong0, strong1) #(100) invLT_0 (net_0, phi1_fb);
  not (strong0, strong1) #(100) invLT_1 (net_1, phi2_fb);
  nand (strong0, strong1) #(100) nand2_0 (net_3, net_0, net_4);
  nand (strong0, strong1) #(100) nand2_1 (net_6, net_1, clk);
endmodule   /* jtag__clockGen */

module jtag__capture_ctl(capture, phi2, sel, out, phi1);
  input capture;
  input phi2;
  input sel;
  output out;
  input phi1;

  supply1 vdd;
  supply0 gnd;
  wire net_1, net_2, net_3, net_4;

  scanChainFive__scanL foo(.in(net_2), .out(net_3));
  not (strong0, strong1) #(100) inv_0 (net_1, capture);
  not (strong0, strong1) #(100) inv_1 (out, net_4);
  nand (strong0, strong1) #(100) nand3_0 (net_4, sel, net_3, phi1);
  scanChainFive__scanP scanP_0(.in(phi2), .src(net_1), .drn(net_2));
endmodule   /* jtag__capture_ctl */

module jtag__shift_ctl(phi1_fb, phi2_fb, sel, shift, phi1_out, phi2_out, 
      phi1_in, phi2_in);
  input phi1_fb;
  input phi2_fb;
  input sel;
  input shift;
  output phi1_out;
  output phi2_out;
  input phi1_in;
  input phi2_in;

  supply1 vdd;
  supply0 gnd;
  wire net_1, net_2, net_3, net_4, net_7;

  jtag__clockGen clockGen_0(.clk(net_7), .phi1_fb(phi1_fb), .phi2_fb(phi2_fb), 
      .phi1_out(phi1_out), .phi2_out(phi2_out));
  scanChainFive__scanL foo(.in(net_2), .out(net_3));
  not (strong0, strong1) #(100) inv_0 (net_7, net_4);
  not (strong0, strong1) #(100) inv_1 (net_1, shift);
  nand (strong0, strong1) #(100) nand3_0 (net_4, sel, net_3, phi1_in);
  scanChainFive__scanP scanP_0(.in(phi2_in), .src(net_1), .drn(net_2));
endmodule   /* jtag__shift_ctl */

module jtag__update_ctl(sel, update, out, phi2);
  input sel;
  input update;
  output out;
  input phi2;

  supply1 vdd;
  supply0 gnd;
  wire net_1;

  not (strong0, strong1) #(100) inv_0 (out, net_1);
  nand (strong0, strong1) #(100) nand3_0 (net_1, sel, update, phi2);
endmodule   /* jtag__update_ctl */

module jtag__jtagIRControl(capture, phi1_fb, phi1_in, phi2_fb, phi2_in, shift, 
      update, phi1_out, phi2_out, read, write);
  input capture;
  input phi1_fb;
  input phi1_in;
  input phi2_fb;
  input phi2_in;
  input shift;
  input update;
  output phi1_out;
  output phi2_out;
  output read;
  output write;

  supply1 vdd;
  supply0 gnd;
  jtag__capture_ctl capture__0(.capture(capture), .phi2(phi2_in), .sel(vdd), 
      .out(read), .phi1(phi1_in));
  jtag__shift_ctl shift_ct_0(.phi1_fb(phi1_fb), .phi2_fb(phi2_fb), .sel(vdd), 
      .shift(shift), .phi1_out(phi1_out), .phi2_out(phi2_out), 
      .phi1_in(phi1_in), .phi2_in(phi2_in));
  jtag__update_ctl update_c_0(.sel(vdd), .update(update), .out(write), 
      .phi2(phi2_in));
endmodule   /* jtag__jtagIRControl */

module redFour__NMOS_X_8_Delay_100(g, d, s);
  input g;
  input d;
  input s;

  supply0 gnd;
  tranif1 #(100) NMOSf_0 (d, s, g);
endmodule   /* redFour__NMOS_X_8_Delay_100 */

module redFour__PMOS_X_4_Delay_100(g, d, s);
  input g;
  input d;
  input s;

  supply1 vdd;
  tranif0 #(100) PMOSf_0 (d, s, g);
endmodule   /* redFour__PMOS_X_4_Delay_100 */

module jtag__tsinvBig(Din, en, enb, Dout);
  input Din;
  input en;
  input enb;
  output Dout;

  supply1 vdd;
  supply0 gnd;
  wire net_13, net_14, net_22, net_23;

  redFour__NMOS_X_8_Delay_100 NMOS_0(.g(Din), .d(net_13), .s(gnd));
  redFour__NMOS_X_8_Delay_100 NMOS_1(.g(en), .d(Dout), .s(net_13));
  redFour__NMOS_X_8_Delay_100 NMOS_2(.g(en), .d(Dout), .s(net_23));
  redFour__NMOS_X_8_Delay_100 NMOS_3(.g(Din), .d(net_23), .s(gnd));
  redFour__PMOS_X_4_Delay_100 PMOS_0(.g(enb), .d(Dout), .s(net_14));
  redFour__PMOS_X_4_Delay_100 PMOS_1(.g(Din), .d(net_14), .s(vdd));
  redFour__PMOS_X_4_Delay_100 PMOS_2(.g(enb), .d(Dout), .s(net_22));
  redFour__PMOS_X_4_Delay_100 PMOS_3(.g(Din), .d(net_22), .s(vdd));
endmodule   /* jtag__tsinvBig */

module jtag__jtagScanControl(TDI, capture, phi1_fb, phi1_in, phi2_fb, phi2_in, 
      sel, shift, update, TDO, phi1_out, phi2_out, read, write);
  input TDI;
  input capture;
  input phi1_fb;
  input phi1_in;
  input phi2_fb;
  input phi2_in;
  input sel;
  input shift;
  input update;
  output TDO;
  output phi1_out;
  output phi2_out;
  output read;
  output write;

  supply1 vdd;
  supply0 gnd;
  wire net_0, net_2;

  jtag__capture_ctl capture__0(.capture(capture), .phi2(phi2_in), .sel(sel), 
      .out(read), .phi1(phi1_in));
  not (strong0, strong1) #(100) inv_0 (net_2, sel);
  not (strong0, strong1) #(100) inv_1 (net_0, TDI);
  jtag__shift_ctl shift_ct_0(.phi1_fb(phi1_fb), .phi2_fb(phi2_fb), .sel(sel), 
      .shift(shift), .phi1_out(phi1_out), .phi2_out(phi2_out), 
      .phi1_in(phi1_in), .phi2_in(phi2_in));
  jtag__tsinvBig tsinvBig_0(.Din(net_0), .en(sel), .enb(net_2), .Dout(TDO));
  jtag__update_ctl update_c_0(.sel(sel), .update(update), .out(write), 
      .phi2(phi2_in));
endmodule   /* jtag__jtagScanControl */

module redFour__NMOS_X_5_667_Delay_100(g, d, s);
  input g;
  input d;
  input s;

  supply0 gnd;
  tranif1 #(100) NMOSf_0 (d, s, g);
endmodule   /* redFour__NMOS_X_5_667_Delay_100 */

module redFour__PMOS_X_2_833_Delay_100(g, d, s);
  input g;
  input d;
  input s;

  supply1 vdd;
  tranif0 #(100) PMOSf_0 (d, s, g);
endmodule   /* redFour__PMOS_X_2_833_Delay_100 */

module jtag__tsinv(Din, Dout, en, enb);
  input Din;
  input Dout;
  input en;
  input enb;

  supply1 vdd;
  supply0 gnd;
  wire net_1, net_2;

  redFour__NMOS_X_5_667_Delay_100 NMOS_0(.g(Din), .d(net_1), .s(gnd));
  redFour__NMOS_X_5_667_Delay_100 NMOS_1(.g(en), .d(Dout), .s(net_1));
  redFour__PMOS_X_2_833_Delay_100 PMOS_0(.g(Din), .d(net_2), .s(vdd));
  redFour__PMOS_X_2_833_Delay_100 PMOS_1(.g(enb), .d(Dout), .s(net_2));
endmodule   /* jtag__tsinv */

module jtag__mux2_phi2(Din0, Din1, phi2, sel, Dout);
  input Din0;
  input Din1;
  input phi2;
  input sel;
  output Dout;

  supply1 vdd;
  supply0 gnd;
  wire net_1, net_2, net_3, net_5, net_6;

  not (strong0, strong1) #(100) inv_0 (net_5, sel);
  not (strong0, strong1) #(100) inv_1 (net_1, net_6);
  not (strong0, strong1) #(100) inv_2 (Dout, net_3);
  scanChainFive__scanL scanL_0(.in(net_2), .out(net_3));
  scanChainFive__scanP scanP_0(.in(phi2), .src(net_1), .drn(net_2));
  jtag__tsinv tsinv_0(.Din(Din0), .Dout(net_6), .en(net_5), .enb(sel));
  jtag__tsinv tsinv_1(.Din(Din1), .Dout(net_6), .en(sel), .enb(net_5));
endmodule   /* jtag__mux2_phi2 */

module jtag__scanAmp1w1648(in, out);
  input in;
  output out;

  supply1 vdd;
  supply0 gnd;
  wire net_0;

  tranif1 nmos_0(gnd, net_0, in);
  tranif1 nmos_1(gnd, out, net_0);
  tranif0 pmos_0(net_0, vdd, in);
  tranif0 pmos_1(out, vdd, net_0);
endmodule   /* jtag__scanAmp1w1648 */

module redFour__nand2n_X_3_5_Delay_100_drive0_strong0_drive1_strong1(ina, inb, 
      out);
  input ina;
  input inb;
  output out;

  supply1 vdd;
  supply0 gnd;
  nand (strong0, strong1) #(100) nand2_0 (out, ina, inb);
endmodule   /* redFour__nand2n_X_3_5_Delay_100_drive0_strong0_drive1_strong1 */

module redFour__nand2n_X_1_25_Delay_100_drive0_strong0_drive1_strong1(ina, inb, 
      out);
  input ina;
  input inb;
  output out;

  supply1 vdd;
  supply0 gnd;
  nand (strong0, strong1) #(100) nand2_0 (out, ina, inb);
endmodule   /* redFour__nand2n_X_1_25_Delay_100_drive0_strong0_drive1_strong1 */

module redFour__nor2n_X_1_25_Delay_100_drive0_strong0_drive1_strong1(ina, inb, 
      out);
  input ina;
  input inb;
  output out;

  supply1 vdd;
  supply0 gnd;
  nor (strong0, strong1) #(100) nor2_0 (out, ina, inb);
endmodule   /* redFour__nor2n_X_1_25_Delay_100_drive0_strong0_drive1_strong1 */

module orangeTSMC180nm__wire_R_26m_100_C_0_025f(a);
  input a;

  supply0 gnd;
endmodule   /* orangeTSMC180nm__wire_R_26m_100_C_0_025f */

module orangeTSMC180nm__wire180_width_3_layer_1_LEWIRE_1_100(a);
  input a;

  supply0 gnd;
  orangeTSMC180nm__wire_R_26m_100_C_0_025f wire_0(.a(a));
endmodule   /* orangeTSMC180nm__wire180_width_3_layer_1_LEWIRE_1_100 */

module jtag__o2a(inAa, inAb, inOb, out);
  input inAa;
  input inAb;
  input inOb;
  output out;

  supply1 vdd;
  supply0 gnd;
  wire net_0;

  nor (strong0, strong1) #(100) nor2_0 (net_0, inAa, inAb);
  redFour__nor2n_X_1_25_Delay_100_drive0_strong0_drive1_strong1 
      nor2n_0(.ina(inOb), .inb(net_0), .out(out));
  orangeTSMC180nm__wire180_width_3_layer_1_LEWIRE_1_100 wire180_0(.a(net_0));
endmodule   /* jtag__o2a */

module orangeTSMC180nm__wire_R_26m_500_C_0_025f(a);
  input a;

  supply0 gnd;
endmodule   /* orangeTSMC180nm__wire_R_26m_500_C_0_025f */

module orangeTSMC180nm__wire180_width_3_layer_1_LEWIRE_1_500(a);
  input a;

  supply0 gnd;
  orangeTSMC180nm__wire_R_26m_500_C_0_025f wire_0(.a(a));
endmodule   /* orangeTSMC180nm__wire180_width_3_layer_1_LEWIRE_1_500 */

module jtag__slaveBit(din, phi2, slave);
  input din;
  input phi2;
  output slave;

  supply1 vdd;
  supply0 gnd;
  wire net_6, net_7;

  not (strong0, strong1) #(100) inv_0 (slave, net_7);
  scanChainFive__scanL scanL_0(.in(net_6), .out(net_7));
  scanChainFive__scanP scanP_0(.in(phi2), .src(din), .drn(net_6));
  orangeTSMC180nm__wire180_width_3_layer_1_LEWIRE_1_500 wire180_0(.a(slave));
endmodule   /* jtag__slaveBit */

module redFour__NMOS_X_1_667_Delay_100(g, d, s);
  input g;
  input d;
  input s;

  supply0 gnd;
  tranif1 #(100) NMOSf_0 (d, s, g);
endmodule   /* redFour__NMOS_X_1_667_Delay_100 */

module orangeTSMC180nm__wire_R_26m_750_C_0_025f(a);
  input a;

  supply0 gnd;
endmodule   /* orangeTSMC180nm__wire_R_26m_750_C_0_025f */

module orangeTSMC180nm__wire180_width_3_layer_1_LEWIRE_1_750(a);
  input a;

  supply0 gnd;
  orangeTSMC180nm__wire_R_26m_750_C_0_025f wire_0(.a(a));
endmodule   /* orangeTSMC180nm__wire180_width_3_layer_1_LEWIRE_1_750 */

module orangeTSMC180nm__wire_R_26m_1000_C_0_025f(a);
  input a;

  supply0 gnd;
endmodule   /* orangeTSMC180nm__wire_R_26m_1000_C_0_025f */

module orangeTSMC180nm__wire180_width_3_layer_1_LEWIRE_1_1000(a);
  input a;

  supply0 gnd;
  orangeTSMC180nm__wire_R_26m_1000_C_0_025f wire_0(.a(a));
endmodule   /* orangeTSMC180nm__wire180_width_3_layer_1_LEWIRE_1_1000 */

module jtag__stateBit(next, phi1, phi2, rst, master, slave, slaveBar);
  input next;
  input phi1;
  input phi2;
  input rst;
  output master;
  output slave;
  output slaveBar;

  supply1 vdd;
  supply0 gnd;
  wire net_12, net_13, net_14, net_17;

  redFour__NMOS_X_1_667_Delay_100 NMOS_0(.g(rst), .d(net_12), .s(gnd));
  not (strong0, strong1) #(100) inv_0 (slave, slaveBar);
  not (strong0, strong1) #(100) inv_1 (slaveBar, net_17);
  not (strong0, strong1) #(100) inv_2 (master, net_13);
  scanChainFive__scanL scanL_0(.in(net_12), .out(net_13));
  scanChainFive__scanL scanL_1(.in(net_14), .out(net_17));
  scanChainFive__scanP scanP_0(.in(phi1), .src(next), .drn(net_12));
  scanChainFive__scanP scanP_1(.in(phi2), .src(net_13), .drn(net_14));
  orangeTSMC180nm__wire180_width_3_layer_1_LEWIRE_1_750 wire180_0(.a(master));
  orangeTSMC180nm__wire180_width_3_layer_1_LEWIRE_1_1000 wire180_1(.a(slave));
  orangeTSMC180nm__wire180_width_3_layer_1_LEWIRE_1_500 
      wire180_2(.a(slaveBar));
  orangeTSMC180nm__wire180_width_3_layer_1_LEWIRE_1_100 wire180_3(.a(next));
endmodule   /* jtag__stateBit */

module redFour__PMOS_X_1_5_Delay_100(g, d, s);
  input g;
  input d;
  input s;

  supply1 vdd;
  tranif0 #(100) PMOSf_0 (d, s, g);
endmodule   /* redFour__PMOS_X_1_5_Delay_100 */

module jtag__stateBitHI(next, phi1, phi2, rstb, master, slave, slaveBar);
  input next;
  input phi1;
  input phi2;
  input rstb;
  output master;
  output slave;
  output slaveBar;

  supply1 vdd;
  supply0 gnd;
  wire net_10, net_11, net_12, net_15;

  redFour__PMOS_X_1_5_Delay_100 PMOS_0(.g(rstb), .d(net_12), .s(vdd));
  not (strong0, strong1) #(100) inv_0 (slave, slaveBar);
  not (strong0, strong1) #(100) inv_1 (slaveBar, net_15);
  not (strong0, strong1) #(100) inv_2 (master, net_10);
  scanChainFive__scanL scanL_0(.in(net_12), .out(net_10));
  scanChainFive__scanL scanL_1(.in(net_11), .out(net_15));
  scanChainFive__scanP scanP_0(.in(phi1), .src(next), .drn(net_12));
  scanChainFive__scanP scanP_1(.in(phi2), .src(net_10), .drn(net_11));
  orangeTSMC180nm__wire180_width_3_layer_1_LEWIRE_1_1000 wire180_0(.a(slave));
  orangeTSMC180nm__wire180_width_3_layer_1_LEWIRE_1_500 
      wire180_1(.a(slaveBar));
  orangeTSMC180nm__wire180_width_3_layer_1_LEWIRE_1_100 wire180_2(.a(next));
  orangeTSMC180nm__wire180_width_3_layer_1_LEWIRE_1_750 wire180_3(.a(master));
endmodule   /* jtag__stateBitHI */

module orangeTSMC180nm__wire_R_26m_675_C_0_025f(a);
  input a;

  supply0 gnd;
endmodule   /* orangeTSMC180nm__wire_R_26m_675_C_0_025f */

module orangeTSMC180nm__wire180_width_3_layer_1_LEWIRE_1_675(a);
  input a;

  supply0 gnd;
  orangeTSMC180nm__wire_R_26m_675_C_0_025f wire_0(.a(a));
endmodule   /* orangeTSMC180nm__wire180_width_3_layer_1_LEWIRE_1_675 */

module orangeTSMC180nm__wire_R_26m_1500_C_0_025f(a);
  input a;

  supply0 gnd;
endmodule   /* orangeTSMC180nm__wire_R_26m_1500_C_0_025f */

module orangeTSMC180nm__wire180_width_3_layer_1_LEWIRE_1_1500(a);
  input a;

  supply0 gnd;
  orangeTSMC180nm__wire_R_26m_1500_C_0_025f wire_0(.a(a));
endmodule   /* orangeTSMC180nm__wire180_width_3_layer_1_LEWIRE_1_1500 */

module jtag__tapCtlJKL(TMS, TRSTb, phi1, phi2, CapDR, CapIR, Idle, PauseDR, 
      PauseIR, Reset, Reset_s, SelDR, SelIR, ShftDR, ShftIR, UpdDR, UpdIR, 
      X1DR, X1IR, X2DR, X2IR);
  input TMS;
  input TRSTb;
  input phi1;
  input phi2;
  output CapDR;
  output CapIR;
  output Idle;
  output PauseDR;
  output PauseIR;
  output Reset;
  output Reset_s;
  output SelDR;
  output SelIR;
  output ShftDR;
  output ShftIR;
  output UpdDR;
  output UpdIR;
  output X1DR;
  output X1IR;
  output X2DR;
  output X2IR;

  supply1 vdd;
  supply0 gnd;
  wire net_0, net_2, net_4, net_6, net_12, net_13, net_14, net_15, net_16;
  wire net_17, net_18, net_19, net_20, net_22, net_23, net_24, net_25, net_26;
  wire net_28, net_29, net_31, net_32, net_34, net_40, net_43, net_44, net_48;
  wire net_50, net_52, net_54, net_55, net_56, net_58, net_59, net_60, net_64;
  wire net_67, net_68, net_70, net_71, net_72, net_74, net_75, net_76, net_79;
  wire net_80, rst, stateBit_1_slave, stateBit_5_slaveBar, stateBit_6_slaveBar;
  wire stateBit_9_slaveBar, stateBit_10_slaveBar, stateBit_11_slave;
  wire stateBit_12_slave;

  not (strong0, strong1) #(100) inv_0 (rst, TRSTb);
  not (strong0, strong1) #(100) inv_1 (net_24, net_12);
  redFour__nand2n_X_3_5_Delay_100_drive0_strong0_drive1_strong1 
      nand2n_0(.ina(net_13), .inb(net_14), .out(net_0));
  redFour__nand2n_X_1_25_Delay_100_drive0_strong0_drive1_strong1 
      nand2n_1(.ina(net_15), .inb(net_16), .out(net_4));
  redFour__nand2n_X_1_25_Delay_100_drive0_strong0_drive1_strong1 
      nand2n_2(.ina(net_17), .inb(net_18), .out(net_2));
  redFour__nand2n_X_1_25_Delay_100_drive0_strong0_drive1_strong1 
      nand2n_3(.ina(net_19), .inb(net_20), .out(net_6));
  redFour__nor2n_X_1_25_Delay_100_drive0_strong0_drive1_strong1 
      nor2n_0(.ina(net_12), .inb(net_23), .out(net_22));
  redFour__nor2n_X_1_25_Delay_100_drive0_strong0_drive1_strong1 
      nor2n_1(.ina(net_24), .inb(net_26), .out(net_25));
  redFour__nor2n_X_1_25_Delay_100_drive0_strong0_drive1_strong1 
      nor2n_2(.ina(net_24), .inb(net_29), .out(net_28));
  redFour__nor2n_X_1_25_Delay_100_drive0_strong0_drive1_strong1 
      nor2n_3(.ina(net_24), .inb(net_32), .out(net_31));
  redFour__nor2n_X_1_25_Delay_100_drive0_strong0_drive1_strong1 
      nor2n_4(.ina(net_12), .inb(net_26), .out(net_34));
  jtag__o2a o2a_0(.inAa(net_2), .inAb(net_43), .inOb(net_12), .out(net_40));
  jtag__o2a o2a_1(.inAa(net_6), .inAb(net_0), .inOb(net_12), .out(net_44));
  jtag__o2a o2a_2(.inAa(net_50), .inAb(net_0), .inOb(net_24), .out(net_48));
  jtag__o2a o2a_3(.inAa(net_54), .inAb(net_55), .inOb(net_12), .out(net_52));
  jtag__o2a o2a_4(.inAa(net_58), .inAb(net_59), .inOb(net_12), .out(net_56));
  jtag__o2a o2a_5(.inAa(net_58), .inAb(net_43), .inOb(net_24), .out(net_60));
  jtag__o2a o2a_6(.inAa(net_54), .inAb(net_67), .inOb(net_24), .out(net_64));
  jtag__o2a o2a_7(.inAa(net_70), .inAb(net_71), .inOb(net_24), .out(net_68));
  jtag__o2a o2a_8(.inAa(net_74), .inAb(net_75), .inOb(net_24), .out(net_72));
  jtag__o2a o2a_9(.inAa(Reset_s), .inAb(net_79), .inOb(net_24), .out(net_76));
  jtag__o2a o2a_10(.inAa(net_4), .inAb(net_67), .inOb(net_12), .out(net_80));
  jtag__slaveBit slaveBit_0(.din(TMS), .phi2(phi2), .slave(net_12));
  jtag__stateBit stateBit_0(.next(net_25), .phi1(phi1), .phi2(phi2), .rst(rst), 
      .master(SelIR), .slave(net_79), .slaveBar(net_23));
  jtag__stateBit stateBit_1(.next(net_48), .phi1(phi1), .phi2(phi2), .rst(rst), 
      .master(SelDR), .slave(stateBit_1_slave), .slaveBar(net_26));
  jtag__stateBit stateBit_2(.next(net_34), .phi1(phi1), .phi2(phi2), .rst(rst), 
      .master(CapDR), .slave(net_75), .slaveBar(net_16));
  jtag__stateBit stateBit_3(.next(net_22), .phi1(phi1), .phi2(phi2), .rst(rst), 
      .master(CapIR), .slave(net_71), .slaveBar(net_18));
  jtag__stateBit stateBit_4(.next(net_44), .phi1(phi1), .phi2(phi2), .rst(rst), 
      .master(Idle), .slave(net_50), .slaveBar(net_20));
  jtag__stateBit stateBit_5(.next(net_68), .phi1(phi1), .phi2(phi2), .rst(rst), 
      .master(X1IR), .slave(net_58), .slaveBar(stateBit_5_slaveBar));
  jtag__stateBit stateBit_6(.next(net_72), .phi1(phi1), .phi2(phi2), .rst(rst), 
      .master(X1DR), .slave(net_54), .slaveBar(stateBit_6_slaveBar));
  jtag__stateBit stateBit_7(.next(net_80), .phi1(phi1), .phi2(phi2), .rst(rst), 
      .master(ShftDR), .slave(net_74), .slaveBar(net_15));
  jtag__stateBit stateBit_8(.next(net_40), .phi1(phi1), .phi2(phi2), .rst(rst), 
      .master(ShftIR), .slave(net_70), .slaveBar(net_17));
  jtag__stateBit stateBit_9(.next(net_28), .phi1(phi1), .phi2(phi2), .rst(rst), 
      .master(X2IR), .slave(net_43), .slaveBar(stateBit_9_slaveBar));
  jtag__stateBit stateBit_10(.next(net_31), .phi1(phi1), .phi2(phi2), 
      .rst(rst), .master(X2DR), .slave(net_67), 
      .slaveBar(stateBit_10_slaveBar));
  jtag__stateBit stateBit_11(.next(net_64), .phi1(phi1), .phi2(phi2), 
      .rst(rst), .master(UpdDR), .slave(stateBit_11_slave), 
      .slaveBar(net_14));
  jtag__stateBit stateBit_12(.next(net_60), .phi1(phi1), .phi2(phi2), 
      .rst(rst), .master(UpdIR), .slave(stateBit_12_slave), 
      .slaveBar(net_13));
  jtag__stateBit stateBit_13(.next(net_56), .phi1(phi1), .phi2(phi2), 
      .rst(rst), .master(PauseIR), .slave(net_59), .slaveBar(net_29));
  jtag__stateBit stateBit_14(.next(net_52), .phi1(phi1), .phi2(phi2), 
      .rst(rst), .master(PauseDR), .slave(net_55), .slaveBar(net_32));
  jtag__stateBitHI stateBit_15(.next(net_76), .phi1(phi1), .phi2(phi2), 
      .rstb(TRSTb), .master(Reset), .slave(Reset_s), .slaveBar(net_19));
  orangeTSMC180nm__wire180_width_3_layer_1_LEWIRE_1_100 wire180_0(.a(net_4));
  orangeTSMC180nm__wire180_width_3_layer_1_LEWIRE_1_100 wire180_1(.a(net_2));
  orangeTSMC180nm__wire180_width_3_layer_1_LEWIRE_1_100 wire180_2(.a(net_6));
  orangeTSMC180nm__wire180_width_3_layer_1_LEWIRE_1_675 wire180_3(.a(net_0));
  orangeTSMC180nm__wire180_width_3_layer_1_LEWIRE_1_1500 wire180_4(.a(rst));
endmodule   /* jtag__tapCtlJKL */

module jtag__jtagControl(TCK, TDI, TDIx, TMS, TRSTb, phi1_fb, phi2_fb, Cap, 
      ExTest, SelBS, SelDR, Shft, TDOb, Upd, phi1, phi2);
  input TCK;
  input TDI;
  input TDIx;
  input TMS;
  input TRSTb;
  input phi1_fb;
  input phi2_fb;
  output Cap;
  output ExTest;
  output SelBS;
  output [12:0] SelDR;
  output Shft;
  output TDOb;
  output Upd;
  output phi1;
  output phi2;

  supply1 vdd;
  supply0 gnd;
  wire jtagScan_0_write, net_0, net_1, net_2, net_3, net_6, net_8, net_10;
  wire net_33, net_35, net_37, net_38, net_41, net_47, net_48, net_50, net_51;
  wire net_52, net_55, net_56, net_62, net_64, net_68, net_73, net_75, net_79;
  wire net_97, net_99, net_103, net_128, tapCtlJK_0_Idle, tapCtlJK_0_PauseDR;
  wire tapCtlJK_0_PauseIR, tapCtlJK_0_Reset, tapCtlJK_0_SelDR, tapCtlJK_0_SelIR;
  wire tapCtlJK_0_X1DR, tapCtlJK_0_X2DR, tapCtlJK_0_X2IR;
  wire [8:1] IR;
  wire [8:1] IRb;

  jtag__BR BR_0(.SDI(TDI), .phi1(net_68), .phi2(net_73), .read(net_99), 
      .SDO(net_97));
  jtag__IR IR_0(.SDI(TDI), .phi1(net_79), .phi2(net_75), .read(net_55), 
      .reset(net_56), .write(net_103), .IR(IR[8:1]), .IRb(IRb[8:1]), 
      .SDO(net_128));
  jtag__IRdecode IRdecode_0(.IR(IR[4:1]), .IRb(IRb[4:1]), .Bypass(net_41), 
      .ExTest(ExTest), .SamplePreload(net_47), .ScanPath(SelDR[12:0]));
  redFour__PMOSwk_X_0_222_Delay_100 PMOSwk_0(.g(gnd), .d(TDIx), .s(vdd));
  jtag__clockGen clockGen_0(.clk(TCK), .phi1_fb(phi1_fb), .phi2_fb(phi2_fb), 
      .phi1_out(net_10), .phi2_out(net_8));
  not (strong0, strong1) #(100) inv_0 (net_0, net_3);
  not (strong0, strong1) #(100) inv_1 (SelBS, net_48);
  not (strong0, strong1) #(100) inv_2 (net_6, net_50);
  not (strong0, strong1) #(100) inv_3 (Cap, net_37);
  not (strong0, strong1) #(100) inv_4 (Shft, net_51);
  not (strong0, strong1) #(100) inv_5 (net_51, net_52);
  not (strong0, strong1) #(100) inv_6 (Upd, net_38);
  jtag__jtagIRControl jtagIRCo_0(.capture(net_62), .phi1_fb(net_79), 
      .phi1_in(phi1), .phi2_fb(net_75), .phi2_in(phi2), .shift(net_2), 
      .update(net_64), .phi1_out(net_79), .phi2_out(net_75), .read(net_55), 
      .write(net_103));
  jtag__jtagScanControl jtagScan_0(.TDI(net_97), .capture(Cap), 
      .phi1_fb(net_68), .phi1_in(phi1), .phi2_fb(net_73), .phi2_in(phi2), 
      .sel(net_41), .shift(Shft), .update(gnd), .TDO(TDIx), .phi1_out(net_68), 
      .phi2_out(net_73), .read(net_99), .write(jtagScan_0_write));
  jtag__mux2_phi2 mux2_phi_0(.Din0(TDIx), .Din1(net_128), .phi2(phi2), 
      .sel(net_0), .Dout(net_50));
  nand (strong0, strong1) #(100) nand2_0 (net_37, IR[8], net_35);
  nand (strong0, strong1) #(100) nand2_1 (net_38, IR[7], net_33);
  nor (strong0, strong1) #(100) nor2_0 (net_3, net_1, net_2);
  nor (strong0, strong1) #(100) nor2_1 (net_48, net_47, ExTest);
  jtag__scanAmp1w1648 scanAmp1_0(.in(net_6), .out(TDOb));
  jtag__scanAmp1w1648 scanAmp1_1(.in(net_8), .out(phi2));
  jtag__scanAmp1w1648 scanAmp1_2(.in(net_10), .out(phi1));
  jtag__tapCtlJKL tapCtlJK_0(.TMS(TMS), .TRSTb(TRSTb), .phi1(phi1), 
      .phi2(phi2), .CapDR(net_35), .CapIR(net_62), .Idle(tapCtlJK_0_Idle), 
      .PauseDR(tapCtlJK_0_PauseDR), .PauseIR(tapCtlJK_0_PauseIR), 
      .Reset(tapCtlJK_0_Reset), .Reset_s(net_56), .SelDR(tapCtlJK_0_SelDR), 
      .SelIR(tapCtlJK_0_SelIR), .ShftDR(net_52), .ShftIR(net_2), 
      .UpdDR(net_33), .UpdIR(net_64), .X1DR(tapCtlJK_0_X1DR), .X1IR(net_1), 
      .X2DR(tapCtlJK_0_X2DR), .X2IR(tapCtlJK_0_X2IR));
endmodule   /* jtag__jtagControl */

module jtag__JTAGamp(leaf, root);
  input [8:1] leaf;
  input [5:1] root;

  supply1 vdd;
  supply0 gnd;
  jtag__scanAmp1w1648 toLeaf_5_(.in(root[5]), .out(leaf[5]));
  jtag__scanAmp1w1648 toLeaf_4_(.in(root[4]), .out(leaf[4]));
  jtag__scanAmp1w1648 toLeaf_3_(.in(root[3]), .out(leaf[3]));
  jtag__scanAmp1w1648 toLeaf_2_(.in(root[2]), .out(leaf[2]));
  jtag__scanAmp1w1648 toLeaf_1_(.in(root[1]), .out(leaf[1]));
endmodule   /* jtag__JTAGamp */

module jtag__jtagScanCtlWBuf(TDI, cap, phi1, phi2, sel, shift, upd, TDO, 
      leaf);
  input TDI;
  input cap;
  input phi1;
  input phi2;
  input sel;
  input shift;
  input upd;
  output TDO;
  input [8:1] leaf;

  supply1 vdd;
  supply0 gnd;
  wire [5:2] a;

  jtag__JTAGamp JTAGamp_0(.leaf(leaf[8:1]), .root({a[5], a[4], a[3], a[2], 
      TDI}));
  jtag__jtagScanControl jtagScan_0(.TDI(leaf[8]), .capture(cap), 
      .phi1_fb(leaf[6]), .phi1_in(phi1), .phi2_fb(leaf[7]), .phi2_in(phi2), 
      .sel(sel), .shift(shift), .update(upd), .TDO(TDO), .phi1_out(a[3]), 
      .phi2_out(a[2]), .read(a[5]), .write(a[4]));
endmodule   /* jtag__jtagScanCtlWBuf */

module jtag__jtagScanCtlGroup(TDI, capture, phi1_in, phi2_in, selBS, sel, 
      shift, update, TDO, BS, leaf0, leaf1, leaf2, leaf3, leaf4, leaf5, leaf6, 
      leaf7, leaf8, leaf9, leaf10, leaf11, leaf12);
  input TDI;
  input capture;
  input phi1_in;
  input phi2_in;
  input selBS;
  input [12:0] sel;
  input shift;
  input update;
  output TDO;
  input [8:1] BS;
  input [8:1] leaf0;
  input [8:1] leaf1;
  input [8:1] leaf2;
  input [8:1] leaf3;
  input [8:1] leaf4;
  input [8:1] leaf5;
  input [8:1] leaf6;
  input [8:1] leaf7;
  input [8:1] leaf8;
  input [8:1] leaf9;
  input [8:1] leaf10;
  input [8:1] leaf11;
  input [8:1] leaf12;

  supply1 vdd;
  supply0 gnd;
  jtag__jtagScanCtlWBuf jtagScan_1(.TDI(TDI), .cap(capture), .phi1(phi1_in), 
      .phi2(phi2_in), .sel(sel[0]), .shift(shift), .upd(update), .TDO(TDO), 
      .leaf(leaf0[8:1]));
  jtag__jtagScanCtlWBuf jtagScan_2(.TDI(TDI), .cap(capture), .phi1(phi1_in), 
      .phi2(phi2_in), .sel(sel[10]), .shift(shift), .upd(update), .TDO(TDO), 
      .leaf(leaf10[8:1]));
  jtag__jtagScanCtlWBuf jtagScan_3(.TDI(TDI), .cap(capture), .phi1(phi1_in), 
      .phi2(phi2_in), .sel(sel[12]), .shift(shift), .upd(update), .TDO(TDO), 
      .leaf(leaf12[8:1]));
  jtag__jtagScanCtlWBuf jtagScan_4(.TDI(TDI), .cap(capture), .phi1(phi1_in), 
      .phi2(phi2_in), .sel(sel[11]), .shift(shift), .upd(update), .TDO(TDO), 
      .leaf(leaf11[8:1]));
  jtag__jtagScanCtlWBuf jtagScan_5(.TDI(TDI), .cap(capture), .phi1(phi1_in), 
      .phi2(phi2_in), .sel(sel[9]), .shift(shift), .upd(update), .TDO(TDO), 
      .leaf(leaf9[8:1]));
  jtag__jtagScanCtlWBuf jtagScan_6(.TDI(TDI), .cap(capture), .phi1(phi1_in), 
      .phi2(phi2_in), .sel(sel[8]), .shift(shift), .upd(update), .TDO(TDO), 
      .leaf(leaf8[8:1]));
  jtag__jtagScanCtlWBuf jtagScan_7(.TDI(TDI), .cap(capture), .phi1(phi1_in), 
      .phi2(phi2_in), .sel(sel[6]), .shift(shift), .upd(update), .TDO(TDO), 
      .leaf(leaf6[8:1]));
  jtag__jtagScanCtlWBuf jtagScan_8(.TDI(TDI), .cap(capture), .phi1(phi1_in), 
      .phi2(phi2_in), .sel(sel[5]), .shift(shift), .upd(update), .TDO(TDO), 
      .leaf(leaf5[8:1]));
  jtag__jtagScanCtlWBuf jtagScan_9(.TDI(TDI), .cap(capture), .phi1(phi1_in), 
      .phi2(phi2_in), .sel(sel[4]), .shift(shift), .upd(update), .TDO(TDO), 
      .leaf(leaf4[8:1]));
  jtag__jtagScanCtlWBuf jtagScan_10(.TDI(TDI), .cap(capture), .phi1(phi1_in), 
      .phi2(phi2_in), .sel(sel[3]), .shift(shift), .upd(update), .TDO(TDO), 
      .leaf(leaf3[8:1]));
  jtag__jtagScanCtlWBuf jtagScan_11(.TDI(TDI), .cap(capture), .phi1(phi1_in), 
      .phi2(phi2_in), .sel(sel[2]), .shift(shift), .upd(update), .TDO(TDO), 
      .leaf(leaf2[8:1]));
  jtag__jtagScanCtlWBuf jtagScan_12(.TDI(TDI), .cap(capture), .phi1(phi1_in), 
      .phi2(phi2_in), .sel(sel[1]), .shift(shift), .upd(update), .TDO(TDO), 
      .leaf(leaf1[8:1]));
  jtag__jtagScanCtlWBuf jtagScan_13(.TDI(TDI), .cap(capture), .phi1(phi1_in), 
      .phi2(phi2_in), .sel(sel[7]), .shift(shift), .upd(update), .TDO(TDO), 
      .leaf(leaf7[8:1]));
  jtag__jtagScanCtlWBuf jtagScan_16(.TDI(TDI), .cap(capture), .phi1(phi1_in), 
      .phi2(phi2_in), .sel(selBS), .shift(shift), .upd(update), .TDO(TDO), 
      .leaf(BS[8:1]));
endmodule   /* jtag__jtagScanCtlGroup */

module jtag__jtagCentral_LEIGNORE_1(TCK, TDI, TMS, TRSTb, ExTest, TDOb, BS, 
      leaf0, leaf1, leaf2, leaf3, leaf4, leaf5, leaf6, leaf7, leaf8, leaf9, 
      leaf10, leaf11, leaf12);
  input TCK;
  input TDI;
  input TMS;
  input TRSTb;
  output ExTest;
  output TDOb;
  input [8:1] BS;
  input [8:1] leaf0;
  input [8:1] leaf1;
  input [8:1] leaf2;
  input [8:1] leaf3;
  input [8:1] leaf4;
  input [8:1] leaf5;
  input [8:1] leaf6;
  input [8:1] leaf7;
  input [8:1] leaf8;
  input [8:1] leaf9;
  input [8:1] leaf10;
  input [8:1] leaf11;
  input [8:1] leaf12;

  supply1 vdd;
  supply0 gnd;
  wire net_10, net_14, net_15, net_17, net_24, net_25, net_50;
  wire [0:12] net_6;

  jtag__jtagControl jtagCont_0(.TCK(TCK), .TDI(TDI), .TDIx(net_15), .TMS(TMS), 
      .TRSTb(TRSTb), .phi1_fb(net_24), .phi2_fb(net_10), .Cap(net_25), 
      .ExTest(ExTest), .SelBS(net_50), .SelDR({net_6[0], net_6[1], net_6[2], 
      net_6[3], net_6[4], net_6[5], net_6[6], net_6[7], net_6[8], net_6[9], 
      net_6[10], net_6[11], net_6[12]}), .Shft(net_17), .TDOb(TDOb), 
      .Upd(net_14), .phi1(net_24), .phi2(net_10));
  jtag__jtagScanCtlGroup jtagScan_0(.TDI(TDI), .capture(net_25), 
      .phi1_in(net_24), .phi2_in(net_10), .selBS(net_50), .sel({net_6[0], 
      net_6[1], net_6[2], net_6[3], net_6[4], net_6[5], net_6[6], net_6[7], 
      net_6[8], net_6[9], net_6[10], net_6[11], net_6[12]}), .shift(net_17), 
      .update(net_14), .TDO(net_15), .BS(BS[8:1]), .leaf0(leaf0[8:1]), 
      .leaf1(leaf1[8:1]), .leaf2(leaf2[8:1]), .leaf3(leaf3[8:1]), 
      .leaf4(leaf4[8:1]), .leaf5(leaf5[8:1]), .leaf6(leaf6[8:1]), 
      .leaf7(leaf7[8:1]), .leaf8(leaf8[8:1]), .leaf9(leaf9[8:1]), 
      .leaf10(leaf10[8:1]), .leaf11(leaf11[8:1]), .leaf12(leaf12[8:1]));
endmodule   /* jtag__jtagCentral_LEIGNORE_1 */

module scanFansFour__jtag_endcap(jtag);
  input [8:4] jtag;

endmodule   /* scanFansFour__jtag_endcap */

module testCell(TCK, TDI, TMS, TRSTb, TDOb);
  input TCK;
  input TDI;
  input TMS;
  input TRSTb;
  output TDOb;

  supply1 vdd;
  supply0 gnd;
  wire jtagCent_0_ExTest;
  wire [4:0] net_5;
  wire [4:0] net_6;
  wire [4:0] net_7;
  wire [4:0] net_8;
  wire [4:0] net_9;
  wire [4:0] net_10;
  wire [4:0] net_11;
  wire [4:0] net_12;
  wire [4:0] net_13;
  wire [4:0] net_14;
  wire [4:0] net_15;
  wire [4:0] net_16;
  wire [4:0] net_17;
  wire [4:0] net_18;

  jtag__jtagCentral_LEIGNORE_1 jtagCent_0(.TCK(TCK), .TDI(TDI), .TMS(TMS), 
      .TRSTb(TRSTb), .ExTest(jtagCent_0_ExTest), .TDOb(TDOb), .BS({net_6[0], 
      net_6[1], net_6[2], net_6[3], net_6[4], net_6[2], net_6[1], net_6[0]}), 
      .leaf0({net_7[0], net_7[1], net_7[2], net_7[3], net_7[4], net_7[2], 
      net_7[1], net_7[0]}), .leaf1({net_18[0], net_18[1], net_18[2], net_18[3], 
      net_18[4], net_18[2], net_18[1], net_18[0]}), .leaf2({net_17[0], 
      net_17[1], net_17[2], net_17[3], net_17[4], net_17[2], net_17[1], 
      net_17[0]}), .leaf3({net_16[0], net_16[1], net_16[2], net_16[3], 
      net_16[4], net_16[2], net_16[1], net_16[0]}), .leaf4({net_15[0], 
      net_15[1], net_15[2], net_15[3], net_15[4], net_15[2], net_15[1], 
      net_15[0]}), .leaf5({net_14[0], net_14[1], net_14[2], net_14[3], 
      net_14[4], net_14[2], net_14[1], net_14[0]}), .leaf6({net_13[0], 
      net_13[1], net_13[2], net_13[3], net_13[4], net_13[2], net_13[1], 
      net_13[0]}), .leaf7({net_12[0], net_12[1], net_12[2], net_12[3], 
      net_12[4], net_12[2], net_12[1], net_12[0]}), .leaf8({net_11[0], 
      net_11[1], net_11[2], net_11[3], net_11[4], net_11[2], net_11[1], 
      net_11[0]}), .leaf9({net_10[0], net_10[1], net_10[2], net_10[3], 
      net_10[4], net_10[2], net_10[1], net_10[0]}), .leaf10({net_9[0], 
      net_9[1], net_9[2], net_9[3], net_9[4], net_9[2], net_9[1], net_9[0]}), 
      .leaf11({net_8[0], net_8[1], net_8[2], net_8[3], net_8[4], net_8[2], 
      net_8[1], net_8[0]}), .leaf12({net_5[0], net_5[1], net_5[2], net_5[3], 
      net_5[4], net_5[2], net_5[1], net_5[0]}));
  scanFansFour__jtag_endcap jtag_end_0(.jtag({net_5[0], net_5[1], net_5[2], 
      net_5[4], net_5[3]}));
  scanFansFour__jtag_endcap jtag_end_1(.jtag({net_8[0], net_8[1], net_8[2], 
      net_8[4], net_8[3]}));
  scanFansFour__jtag_endcap jtag_end_2(.jtag({net_9[0], net_9[1], net_9[2], 
      net_9[4], net_9[3]}));
  scanFansFour__jtag_endcap jtag_end_3(.jtag({net_10[0], net_10[1], net_10[2], 
      net_10[4], net_10[3]}));
  scanFansFour__jtag_endcap jtag_end_4(.jtag({net_11[0], net_11[1], net_11[2], 
      net_11[4], net_11[3]}));
  scanFansFour__jtag_endcap jtag_end_5(.jtag({net_12[0], net_12[1], net_12[2], 
      net_12[4], net_12[3]}));
  scanFansFour__jtag_endcap jtag_end_6(.jtag({net_13[0], net_13[1], net_13[2], 
      net_13[4], net_13[3]}));
  scanFansFour__jtag_endcap jtag_end_7(.jtag({net_14[0], net_14[1], net_14[2], 
      net_14[4], net_14[3]}));
  scanFansFour__jtag_endcap jtag_end_8(.jtag({net_15[0], net_15[1], net_15[2], 
      net_15[4], net_15[3]}));
  scanFansFour__jtag_endcap jtag_end_9(.jtag({net_16[0], net_16[1], net_16[2], 
      net_16[4], net_16[3]}));
  scanFansFour__jtag_endcap jtag_end_10(.jtag({net_17[0], net_17[1], net_17[2], 
      net_17[4], net_17[3]}));
  scanFansFour__jtag_endcap jtag_end_11(.jtag({net_18[0], net_18[1], net_18[2], 
      net_18[4], net_18[3]}));
  scanFansFour__jtag_endcap jtag_end_12(.jtag({net_7[0], net_7[1], net_7[2], 
      net_7[4], net_7[3]}));
  scanFansFour__jtag_endcap jtag_end_13(.jtag({net_6[0], net_6[1], net_6[2], 
      net_6[4], net_6[3]}));
endmodule   /* testCell */
