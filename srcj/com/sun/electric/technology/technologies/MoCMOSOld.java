package com.sun.electric.technology.technologies;

/**
 * Electric(tm) VLSI Design System
 *
 * File: MoCMOSOld.java
 * mocmosold technology description
 * Generated automatically from a library
 *
 * Copyright (c) 2003 Static Free Software.
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
 *
 * Static Free Software
 * 4119 Alpine Road
 * Portola Valley, California 94028
 * info@staticfreesoft.com
 */
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitiveArc;
import com.sun.electric.technology.PrimitivePort;
import com.sun.electric.technology.EdgeH;
import com.sun.electric.technology.EdgeV;
import com.sun.electric.technology.SizeOffset;
import com.sun.electric.database.geometry.EGraphics;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.prototype.NodeProto;

import java.awt.Point;
import java.awt.geom.Point2D;

/**
 * This is the Complementary MOS (old, from MOSIS, P-Well, double metal) technology.
 */
public class MoCMOSOld extends Technology
{
	public static final MoCMOSOld tech = new MoCMOSOld();
	// -------------------- private and protected methods ------------------------
	private MoCMOSOld()
	{
		setTechName("mocmosold");
		setTechDesc("Complementary MOS (old, from MOSIS, P-Well, double metal)");
		setScale(2000);

		setNoNegatedArcs();

		setStaticTechnology();

		//**************************************** LAYERS ****************************************

		/** M layer */
		Layer M_lay = Layer.newInstance("Metal-1",
			new EGraphics(EGraphics.LAYERT1, EGraphics.COLORT1, EGraphics.SOLIDC, EGraphics.SOLIDC, 107,226,96,0.8,1,
			new int[] { 0x2222,   //   X   X   X   X 
						0x0000,   //                 
						0x8888,   // X   X   X   X   
						0x0000,   //                 
						0x2222,   //   X   X   X   X 
						0x0000,   //                 
						0x8888,   // X   X   X   X   
						0x0000,   //                 
						0x2222,   //   X   X   X   X 
						0x0000,   //                 
						0x8888,   // X   X   X   X   
						0x0000,   //                 
						0x2222,   //   X   X   X   X 
						0x0000,   //                 
						0x8888,   // X   X   X   X   
						0x0000}));//                 

		/** M0 layer */
		Layer M0_lay = Layer.newInstance("Metal-2",
			new EGraphics(EGraphics.LAYERT4, EGraphics.COLORT4, EGraphics.SOLIDC, EGraphics.SOLIDC, 21372,4096,31016,0.8,1,
			new int[] { 0x1010,   //    X       X    
						0x2020,   //   X       X     
						0x4040,   //  X       X      
						0x8080,   // X       X       
						0x0101,   //        X       X
						0x0202,   //       X       X 
						0x0404,   //      X       X  
						0x0808,   //     X       X   
						0x1010,   //    X       X    
						0x2020,   //   X       X     
						0x4040,   //  X       X      
						0x8080,   // X       X       
						0x0101,   //        X       X
						0x0202,   //       X       X 
						0x0404,   //      X       X  
						0x0808}));//     X       X   

		/** P layer */
		Layer P_lay = Layer.newInstance("Polysilicon",
			new EGraphics(EGraphics.LAYERT2, EGraphics.COLORT2, EGraphics.SOLIDC, EGraphics.SOLIDC, 224,95,255,0.8,1,
			new int[] { 0x0808,   //     X       X   
						0x0404,   //      X       X  
						0x0202,   //       X       X 
						0x0101,   //        X       X
						0x8080,   // X       X       
						0x4040,   //  X       X      
						0x2020,   //   X       X     
						0x1010,   //    X       X    
						0x0808,   //     X       X   
						0x0404,   //      X       X  
						0x0202,   //       X       X 
						0x0101,   //        X       X
						0x8080,   // X       X       
						0x4040,   //  X       X      
						0x2020,   //   X       X     
						0x1010}));//    X       X    

		/** SA layer */
		Layer SA_lay = Layer.newInstance("S-Active",
			new EGraphics(EGraphics.LAYERT3, EGraphics.COLORT3, EGraphics.SOLIDC, EGraphics.SOLIDC, 240,221,181,0.8,1,
			new int[] { 0x0000,   //                 
						0x0303,   //       XX      XX
						0x4848,   //  X  X    X  X   
						0x0303,   //       XX      XX
						0x0000,   //                 
						0x3030,   //   XX      XX    
						0x8484,   // X    X  X    X  
						0x3030,   //   XX      XX    
						0x0000,   //                 
						0x0303,   //       XX      XX
						0x4848,   //  X  X    X  X   
						0x0303,   //       XX      XX
						0x0000,   //                 
						0x3030,   //   XX      XX    
						0x8484,   // X    X  X    X  
						0x3030}));//   XX      XX    

		/** DA layer */
		Layer DA_lay = Layer.newInstance("D-Active",
			new EGraphics(EGraphics.LAYERT3, EGraphics.COLORT3, EGraphics.SOLIDC, EGraphics.SOLIDC, 240,221,181,0.8,1,
			new int[] { 0x0000,   //                 
						0x0303,   //       XX      XX
						0x4848,   //  X  X    X  X   
						0x0303,   //       XX      XX
						0x0000,   //                 
						0x3030,   //   XX      XX    
						0x8484,   // X    X  X    X  
						0x3030,   //   XX      XX    
						0x0000,   //                 
						0x0303,   //       XX      XX
						0x4848,   //  X  X    X  X   
						0x0303,   //       XX      XX
						0x0000,   //                 
						0x3030,   //   XX      XX    
						0x8484,   // X    X  X    X  
						0x3030}));//   XX      XX    

		/** PS layer */
		Layer PS_lay = Layer.newInstance("P-Select",
			new EGraphics(EGraphics.LAYERO, EGraphics.YELLOW, EGraphics.PATTERNED, EGraphics.PATTERNED, 89,44,51,0.8,1,
			new int[] { 0x1010,   //    X       X    
						0x2020,   //   X       X     
						0x4040,   //  X       X      
						0x8080,   // X       X       
						0x0101,   //        X       X
						0x0202,   //       X       X 
						0x0404,   //      X       X  
						0x0808,   //     X       X   
						0x1010,   //    X       X    
						0x2020,   //   X       X     
						0x4040,   //  X       X      
						0x8080,   // X       X       
						0x0101,   //        X       X
						0x0202,   //       X       X 
						0x0404,   //      X       X  
						0x0808}));//     X       X   

		/** NS layer */
		Layer NS_lay = Layer.newInstance("N-Select",
			new EGraphics(EGraphics.LAYERO, EGraphics.YELLOW, EGraphics.PATTERNED, EGraphics.PATTERNED, 89,44,51,0.8,1,
			new int[] { 0x1010,   //    X       X    
						0x2020,   //   X       X     
						0x4040,   //  X       X      
						0x8080,   // X       X       
						0x0101,   //        X       X
						0x0202,   //       X       X 
						0x0404,   //      X       X  
						0x0808,   //     X       X   
						0x1010,   //    X       X    
						0x2020,   //   X       X     
						0x4040,   //  X       X      
						0x8080,   // X       X       
						0x0101,   //        X       X
						0x0202,   //       X       X 
						0x0404,   //      X       X  
						0x0808}));//     X       X   

		/** PW layer */
		Layer PW_lay = Layer.newInstance("P-Well",
			new EGraphics(EGraphics.LAYERT5, EGraphics.COLORT5, EGraphics.SOLIDC, EGraphics.SOLIDC, 0,0,0,0.8,1,
			new int[] { 0x1000,   //    X            
						0x0020,   //           X     
						0x0000,   //                 
						0x0000,   //                 
						0x0001,   //                X
						0x0200,   //       X         
						0x0000,   //                 
						0x0000,   //                 
						0x1000,   //    X            
						0x0020,   //           X     
						0x0000,   //                 
						0x0000,   //                 
						0x0001,   //                X
						0x0200,   //       X         
						0x0000,   //                 
						0x0000}));//                 

		/** NW layer */
		Layer NW_lay = Layer.newInstance("N-Well",
			new EGraphics(EGraphics.LAYERT5, EGraphics.COLORT5, EGraphics.SOLIDC, EGraphics.SOLIDC, 0,0,0,0.8,1,
			new int[] { 0x1000,   //    X            
						0x0020,   //           X     
						0x0000,   //                 
						0x0000,   //                 
						0x0001,   //                X
						0x0200,   //       X         
						0x0000,   //                 
						0x0000,   //                 
						0x1000,   //    X            
						0x0020,   //           X     
						0x0000,   //                 
						0x0000,   //                 
						0x0001,   //                X
						0x0200,   //       X         
						0x0000,   //                 
						0x0000}));//                 

		/** CC layer */
		Layer CC_lay = Layer.newInstance("Contact-Cut",
			new EGraphics(EGraphics.LAYERO, EGraphics.BLACK, EGraphics.SOLIDC, EGraphics.SOLIDC, 107,137,72,0.8,1,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** V layer */
		Layer V_lay = Layer.newInstance("Via",
			new EGraphics(EGraphics.LAYERO, EGraphics.BLACK, EGraphics.SOLIDC, EGraphics.SOLIDC, 107,137,72,0.8,1,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** P0 layer */
		Layer P0_lay = Layer.newInstance("Passivation",
			new EGraphics(EGraphics.LAYERO, EGraphics.DGRAY, EGraphics.PATTERNED, EGraphics.PATTERNED, 0,0,0,0.8,1,
			new int[] { 0x1c1c,   //    XXX     XXX  
						0x3e3e,   //   XXXXX   XXXXX 
						0x3636,   //   XX XX   XX XX 
						0x3e3e,   //   XXXXX   XXXXX 
						0x1c1c,   //    XXX     XXX  
						0x0000,   //                 
						0x0000,   //                 
						0x0000,   //                 
						0x1c1c,   //    XXX     XXX  
						0x3e3e,   //   XXXXX   XXXXX 
						0x3636,   //   XX XX   XX XX 
						0x3e3e,   //   XXXXX   XXXXX 
						0x1c1c,   //    XXX     XXX  
						0x0000,   //                 
						0x0000,   //                 
						0x0000}));//                 

		/** T layer */
		Layer T_lay = Layer.newInstance("Transistor",
			new EGraphics(EGraphics.LAYERO, EGraphics.ALLOFF, EGraphics.SOLIDC, EGraphics.SOLIDC, 200,200,200,0.8,1,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** PC layer */
		Layer PC_lay = Layer.newInstance("Poly-Cut",
			new EGraphics(EGraphics.LAYERO, EGraphics.BLACK, EGraphics.SOLIDC, EGraphics.SOLIDC, 107,137,72,0.8,1,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** AC layer */
		Layer AC_lay = Layer.newInstance("Active-Cut",
			new EGraphics(EGraphics.LAYERO, EGraphics.BLACK, EGraphics.SOLIDC, EGraphics.SOLIDC, 107,137,72,0.8,1,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** SAW layer */
		Layer SAW_lay = Layer.newInstance("S-Active-Well",
			new EGraphics(EGraphics.LAYERT3, EGraphics.COLORT3, EGraphics.SOLIDC, EGraphics.SOLIDC, 240,221,181,0.8,1,
			new int[] { 0x0000,   //                 
						0x0303,   //       XX      XX
						0x4848,   //  X  X    X  X   
						0x0303,   //       XX      XX
						0x0000,   //                 
						0x3030,   //   XX      XX    
						0x8484,   // X    X  X    X  
						0x3030,   //   XX      XX    
						0x0000,   //                 
						0x0303,   //       XX      XX
						0x4848,   //  X  X    X  X   
						0x0303,   //       XX      XX
						0x0000,   //                 
						0x3030,   //   XX      XX    
						0x8484,   // X    X  X    X  
						0x3030}));//   XX      XX    

		/** PM layer */
		Layer PM_lay = Layer.newInstance("Pseudo-Metal-1",
			new EGraphics(EGraphics.LAYERT1, EGraphics.COLORT1, EGraphics.SOLIDC, EGraphics.SOLIDC, 107,226,96,0.8,1,
			new int[] { 0x2222,   //   X   X   X   X 
						0x0000,   //                 
						0x8888,   // X   X   X   X   
						0x0000,   //                 
						0x2222,   //   X   X   X   X 
						0x0000,   //                 
						0x8888,   // X   X   X   X   
						0x0000,   //                 
						0x2222,   //   X   X   X   X 
						0x0000,   //                 
						0x8888,   // X   X   X   X   
						0x0000,   //                 
						0x2222,   //   X   X   X   X 
						0x0000,   //                 
						0x8888,   // X   X   X   X   
						0x0000}));//                 

		/** PM0 layer */
		Layer PM0_lay = Layer.newInstance("Pseudo-Metal-2",
			new EGraphics(EGraphics.LAYERT4, EGraphics.COLORT4, EGraphics.SOLIDC, EGraphics.SOLIDC, 21372,4096,31016,0.8,1,
			new int[] { 0x1010,   //    X       X    
						0x2020,   //   X       X     
						0x4040,   //  X       X      
						0x8080,   // X       X       
						0x0101,   //        X       X
						0x0202,   //       X       X 
						0x0404,   //      X       X  
						0x0808,   //     X       X   
						0x1010,   //    X       X    
						0x2020,   //   X       X     
						0x4040,   //  X       X      
						0x8080,   // X       X       
						0x0101,   //        X       X
						0x0202,   //       X       X 
						0x0404,   //      X       X  
						0x0808}));//     X       X   

		/** PP layer */
		Layer PP_lay = Layer.newInstance("Pseudo-Polysilicon",
			new EGraphics(EGraphics.LAYERT2, EGraphics.COLORT2, EGraphics.SOLIDC, EGraphics.SOLIDC, 224,95,255,0.8,1,
			new int[] { 0x0808,   //     X       X   
						0x0404,   //      X       X  
						0x0202,   //       X       X 
						0x0101,   //        X       X
						0x8080,   // X       X       
						0x4040,   //  X       X      
						0x2020,   //   X       X     
						0x1010,   //    X       X    
						0x0808,   //     X       X   
						0x0404,   //      X       X  
						0x0202,   //       X       X 
						0x0101,   //        X       X
						0x8080,   // X       X       
						0x4040,   //  X       X      
						0x2020,   //   X       X     
						0x1010}));//    X       X    

		/** PSA layer */
		Layer PSA_lay = Layer.newInstance("Pseudo-S-Active",
			new EGraphics(EGraphics.LAYERT3, EGraphics.COLORT3, EGraphics.SOLIDC, EGraphics.SOLIDC, 240,221,181,0.8,1,
			new int[] { 0x0000,   //                 
						0x0303,   //       XX      XX
						0x4848,   //  X  X    X  X   
						0x0303,   //       XX      XX
						0x0000,   //                 
						0x3030,   //   XX      XX    
						0x8484,   // X    X  X    X  
						0x3030,   //   XX      XX    
						0x0000,   //                 
						0x0303,   //       XX      XX
						0x4848,   //  X  X    X  X   
						0x0303,   //       XX      XX
						0x0000,   //                 
						0x3030,   //   XX      XX    
						0x8484,   // X    X  X    X  
						0x3030}));//   XX      XX    

		/** PDA layer */
		Layer PDA_lay = Layer.newInstance("Pseudo-D-Active",
			new EGraphics(EGraphics.LAYERT3, EGraphics.COLORT3, EGraphics.SOLIDC, EGraphics.SOLIDC, 240,221,181,0.8,1,
			new int[] { 0x0000,   //                 
						0x0303,   //       XX      XX
						0x4848,   //  X  X    X  X   
						0x0303,   //       XX      XX
						0x0000,   //                 
						0x3030,   //   XX      XX    
						0x8484,   // X    X  X    X  
						0x3030,   //   XX      XX    
						0x0000,   //                 
						0x0303,   //       XX      XX
						0x4848,   //  X  X    X  X   
						0x0303,   //       XX      XX
						0x0000,   //                 
						0x3030,   //   XX      XX    
						0x8484,   // X    X  X    X  
						0x3030}));//   XX      XX    

		/** PPS layer */
		Layer PPS_lay = Layer.newInstance("Pseudo-P-Select",
			new EGraphics(EGraphics.LAYERO, EGraphics.YELLOW, EGraphics.PATTERNED, EGraphics.PATTERNED, 89,44,51,0.8,1,
			new int[] { 0x1010,   //    X       X    
						0x2020,   //   X       X     
						0x4040,   //  X       X      
						0x8080,   // X       X       
						0x0101,   //        X       X
						0x0202,   //       X       X 
						0x0404,   //      X       X  
						0x0808,   //     X       X   
						0x1010,   //    X       X    
						0x2020,   //   X       X     
						0x4040,   //  X       X      
						0x8080,   // X       X       
						0x0101,   //        X       X
						0x0202,   //       X       X 
						0x0404,   //      X       X  
						0x0808}));//     X       X   

		/** PNS layer */
		Layer PNS_lay = Layer.newInstance("Pseudo-N-Select",
			new EGraphics(EGraphics.LAYERO, EGraphics.YELLOW, EGraphics.PATTERNED, EGraphics.PATTERNED, 89,44,51,0.8,1,
			new int[] { 0x1010,   //    X       X    
						0x2020,   //   X       X     
						0x4040,   //  X       X      
						0x8080,   // X       X       
						0x0101,   //        X       X
						0x0202,   //       X       X 
						0x0404,   //      X       X  
						0x0808,   //     X       X   
						0x1010,   //    X       X    
						0x2020,   //   X       X     
						0x4040,   //  X       X      
						0x8080,   // X       X       
						0x0101,   //        X       X
						0x0202,   //       X       X 
						0x0404,   //      X       X  
						0x0808}));//     X       X   

		/** PPW layer */
		Layer PPW_lay = Layer.newInstance("Pseudo-P-Well",
			new EGraphics(EGraphics.LAYERT5, EGraphics.COLORT5, EGraphics.SOLIDC, EGraphics.SOLIDC, 0,0,0,0.8,1,
			new int[] { 0x1000,   //    X            
						0x0020,   //           X     
						0x0000,   //                 
						0x0000,   //                 
						0x0001,   //                X
						0x0200,   //       X         
						0x0000,   //                 
						0x0000,   //                 
						0x1000,   //    X            
						0x0020,   //           X     
						0x0000,   //                 
						0x0000,   //                 
						0x0001,   //                X
						0x0200,   //       X         
						0x0000,   //                 
						0x0000}));//                 

		/** PNW layer */
		Layer PNW_lay = Layer.newInstance("Pseudo-N-Well",
			new EGraphics(EGraphics.LAYERT5, EGraphics.COLORT5, EGraphics.SOLIDC, EGraphics.SOLIDC, 0,0,0,0.8,1,
			new int[] { 0x1000,   //    X            
						0x0020,   //           X     
						0x0000,   //                 
						0x0000,   //                 
						0x0001,   //                X
						0x0200,   //       X         
						0x0000,   //                 
						0x0000,   //                 
						0x1000,   //    X            
						0x0020,   //           X     
						0x0000,   //                 
						0x0000,   //                 
						0x0001,   //                X
						0x0200,   //       X         
						0x0000,   //                 
						0x0000}));//                 

		/** PF layer */
		Layer PF_lay = Layer.newInstance("Pad-Frame",
			new EGraphics(EGraphics.LAYERO, EGraphics.RED, EGraphics.SOLIDC, EGraphics.SOLIDC, 224,57,192,0.8,1,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		// The layer functions
		M_lay.setFunction(Layer.Function.METAL1);		// Metal-1
		M0_lay.setFunction(Layer.Function.METAL2);		// Metal-2
		P_lay.setFunction(Layer.Function.POLY1);		// Polysilicon
		SA_lay.setFunction(Layer.Function.DIFF, Layer.Function.PTYPE);		// S-Active
		DA_lay.setFunction(Layer.Function.DIFF, Layer.Function.NTYPE);		// D-Active
		PS_lay.setFunction(Layer.Function.IMPLANT, Layer.Function.PTYPE);		// P-Select
		NS_lay.setFunction(Layer.Function.IMPLANT, Layer.Function.NTYPE);		// N-Select
		PW_lay.setFunction(Layer.Function.WELL, Layer.Function.PTYPE);		// P-Well
		NW_lay.setFunction(Layer.Function.WELL, Layer.Function.NTYPE);		// N-Well
		CC_lay.setFunction(Layer.Function.CONTACT1);		// Contact-Cut
		V_lay.setFunction(Layer.Function.CONTACT2, Layer.Function.CONMETAL);		// Via
		P0_lay.setFunction(Layer.Function.OVERGLASS);		// Passivation
		T_lay.setFunction(Layer.Function.TRANSISTOR, Layer.Function.PSEUDO);		// Transistor
		PC_lay.setFunction(Layer.Function.CONTACT1, Layer.Function.CONPOLY);		// Poly-Cut
		AC_lay.setFunction(Layer.Function.CONTACT1, Layer.Function.CONDIFF);		// Active-Cut
		SAW_lay.setFunction(Layer.Function.DIFF, Layer.Function.PTYPE);		// S-Active-Well
		PM_lay.setFunction(Layer.Function.METAL1, Layer.Function.PSEUDO);		// Pseudo-Metal-1
		PM0_lay.setFunction(Layer.Function.METAL2, Layer.Function.PSEUDO);		// Pseudo-Metal-2
		PP_lay.setFunction(Layer.Function.POLY1, Layer.Function.PSEUDO);		// Pseudo-Polysilicon
		PSA_lay.setFunction(Layer.Function.DIFF, Layer.Function.PTYPE|Layer.Function.PSEUDO);		// Pseudo-S-Active
		PDA_lay.setFunction(Layer.Function.DIFF, Layer.Function.NTYPE|Layer.Function.PSEUDO);		// Pseudo-D-Active
		PPS_lay.setFunction(Layer.Function.IMPLANT, Layer.Function.PTYPE|Layer.Function.PSEUDO);		// Pseudo-P-Select
		PNS_lay.setFunction(Layer.Function.IMPLANT, Layer.Function.NTYPE|Layer.Function.PSEUDO);		// Pseudo-N-Select
		PPW_lay.setFunction(Layer.Function.WELL, Layer.Function.PTYPE|Layer.Function.PSEUDO);		// Pseudo-P-Well
		PNW_lay.setFunction(Layer.Function.WELL, Layer.Function.NTYPE|Layer.Function.PSEUDO);		// Pseudo-N-Well
		PF_lay.setFunction(Layer.Function.ART);		// Pad-Frame

		// The CIF names
		M_lay.setCIFLayer("CMF");		// Metal-1
		M0_lay.setCIFLayer("CMS");		// Metal-2
		P_lay.setCIFLayer("CPG");		// Polysilicon
		SA_lay.setCIFLayer("CAA");		// S-Active
		DA_lay.setCIFLayer("CAA");		// D-Active
		PS_lay.setCIFLayer("CSG");		// P-Select
		NS_lay.setCIFLayer("CSG");		// N-Select
		PW_lay.setCIFLayer("CWG");		// P-Well
		NW_lay.setCIFLayer("CWG");		// N-Well
		CC_lay.setCIFLayer("CC");		// Contact-Cut
		V_lay.setCIFLayer("CVA");		// Via
		P0_lay.setCIFLayer("COG");		// Passivation
		T_lay.setCIFLayer("");		// Transistor
		PC_lay.setCIFLayer("CCP");		// Poly-Cut
		AC_lay.setCIFLayer("CCA");		// Active-Cut
		SAW_lay.setCIFLayer("CAA");		// S-Active-Well
		PM_lay.setCIFLayer("");		// Pseudo-Metal-1
		PM0_lay.setCIFLayer("");		// Pseudo-Metal-2
		PP_lay.setCIFLayer("");		// Pseudo-Polysilicon
		PSA_lay.setCIFLayer("");		// Pseudo-S-Active
		PDA_lay.setCIFLayer("");		// Pseudo-D-Active
		PPS_lay.setCIFLayer("");		// Pseudo-P-Select
		PNS_lay.setCIFLayer("");		// Pseudo-N-Select
		PPW_lay.setCIFLayer("");		// Pseudo-P-Well
		PNW_lay.setCIFLayer("");		// Pseudo-N-Well
		PF_lay.setCIFLayer("CX");		// Pad-Frame

		// The DXF names
		M_lay.setDXFLayer("");		// Metal-1
		M0_lay.setDXFLayer("");		// Metal-2
		P_lay.setDXFLayer("");		// Polysilicon
		SA_lay.setDXFLayer("");		// S-Active
		DA_lay.setDXFLayer("");		// D-Active
		PS_lay.setDXFLayer("");		// P-Select
		NS_lay.setDXFLayer("");		// N-Select
		PW_lay.setDXFLayer("");		// P-Well
		NW_lay.setDXFLayer("");		// N-Well
		CC_lay.setDXFLayer("");		// Contact-Cut
		V_lay.setDXFLayer("");		// Via
		P0_lay.setDXFLayer("");		// Passivation
		T_lay.setDXFLayer("");		// Transistor
		PC_lay.setDXFLayer("");		// Poly-Cut
		AC_lay.setDXFLayer("");		// Active-Cut
		SAW_lay.setDXFLayer("");		// S-Active-Well
		PM_lay.setDXFLayer("");		// Pseudo-Metal-1
		PM0_lay.setDXFLayer("");		// Pseudo-Metal-2
		PP_lay.setDXFLayer("");		// Pseudo-Polysilicon
		PSA_lay.setDXFLayer("");		// Pseudo-S-Active
		PDA_lay.setDXFLayer("");		// Pseudo-D-Active
		PPS_lay.setDXFLayer("");		// Pseudo-P-Select
		PNS_lay.setDXFLayer("");		// Pseudo-N-Select
		PPW_lay.setDXFLayer("");		// Pseudo-P-Well
		PNW_lay.setDXFLayer("");		// Pseudo-N-Well
		PF_lay.setDXFLayer("");		// Pad-Frame

		// The GDS names
		M_lay.setGDSLayer("10");		// Metal-1
		M0_lay.setGDSLayer("19");		// Metal-2
		P_lay.setGDSLayer("12");		// Polysilicon
		SA_lay.setGDSLayer("2");		// S-Active
		DA_lay.setGDSLayer("2");		// D-Active
		PS_lay.setGDSLayer("8");		// P-Select
		NS_lay.setGDSLayer("7");		// N-Select
		PW_lay.setGDSLayer("1");		// P-Well
		NW_lay.setGDSLayer("1");		// N-Well
		CC_lay.setGDSLayer("9");		// Contact-Cut
		V_lay.setGDSLayer("18");		// Via
		P0_lay.setGDSLayer("11");		// Passivation
		T_lay.setGDSLayer("");		// Transistor
		PC_lay.setGDSLayer("9");		// Poly-Cut
		AC_lay.setGDSLayer("9");		// Active-Cut
		SAW_lay.setGDSLayer("2");		// S-Active-Well
		PM_lay.setGDSLayer("");		// Pseudo-Metal-1
		PM0_lay.setGDSLayer("");		// Pseudo-Metal-2
		PP_lay.setGDSLayer("");		// Pseudo-Polysilicon
		PSA_lay.setGDSLayer("");		// Pseudo-S-Active
		PDA_lay.setGDSLayer("");		// Pseudo-D-Active
		PPS_lay.setGDSLayer("");		// Pseudo-P-Select
		PNS_lay.setGDSLayer("");		// Pseudo-N-Select
		PPW_lay.setGDSLayer("");		// Pseudo-P-Well
		PNW_lay.setGDSLayer("");		// Pseudo-N-Well
		PF_lay.setGDSLayer("");		// Pad-Frame

		//******************** ARCS ********************

		/** Metal-1 arc */
		PrimitiveArc Metal_1_arc = PrimitiveArc.newInstance(this, "Metal-1", 3, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(M_lay, 0, Poly.Type.FILLED)
		});
		Metal_1_arc.setFunction(PrimitiveArc.Function.METAL1);
		Metal_1_arc.setFixedAngle();
		Metal_1_arc.setWipable();
		Metal_1_arc.setAngleIncrement(90);

		/** Metal-2 arc */
		PrimitiveArc Metal_2_arc = PrimitiveArc.newInstance(this, "Metal-2", 3, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(M0_lay, 0, Poly.Type.FILLED)
		});
		Metal_2_arc.setFunction(PrimitiveArc.Function.METAL2);
		Metal_2_arc.setFixedAngle();
		Metal_2_arc.setWipable();
		Metal_2_arc.setAngleIncrement(90);

		/** Polysilicon arc */
		PrimitiveArc Polysilicon_arc = PrimitiveArc.newInstance(this, "Polysilicon", 2, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(P_lay, 0, Poly.Type.FILLED)
		});
		Polysilicon_arc.setFunction(PrimitiveArc.Function.POLY1);
		Polysilicon_arc.setFixedAngle();
		Polysilicon_arc.setWipable();
		Polysilicon_arc.setAngleIncrement(90);

		/** S-Active arc */
		PrimitiveArc S_Active_arc = PrimitiveArc.newInstance(this, "S-Active", 6, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(SA_lay, 4, Poly.Type.FILLED),
			new Technology.ArcLayer(PS_lay, 0, Poly.Type.FILLED)
		});
		S_Active_arc.setFunction(PrimitiveArc.Function.DIFFP);
		S_Active_arc.setFixedAngle();
		S_Active_arc.setWipable();
		S_Active_arc.setAngleIncrement(90);
		S_Active_arc.setWidthOffset(0);

		/** D-Active arc */
		PrimitiveArc D_Active_arc = PrimitiveArc.newInstance(this, "D-Active", 10, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(DA_lay, 8, Poly.Type.FILLED),
			new Technology.ArcLayer(PW_lay, 0, Poly.Type.FILLED)
		});
		D_Active_arc.setFunction(PrimitiveArc.Function.DIFFN);
		D_Active_arc.setFixedAngle();
		D_Active_arc.setWipable();
		D_Active_arc.setAngleIncrement(90);
		D_Active_arc.setWidthOffset(0);

		/** Active arc */
		PrimitiveArc Active_arc = PrimitiveArc.newInstance(this, "Active", 2, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(DA_lay, 0, Poly.Type.FILLED),
			new Technology.ArcLayer(SA_lay, 0, Poly.Type.FILLED)
		});
		Active_arc.setFunction(PrimitiveArc.Function.DIFF);
		Active_arc.setFixedAngle();
		Active_arc.setWipable();
		Active_arc.setAngleIncrement(90);

		//******************** RECTANGLE DESCRIPTIONS ********************

		Technology.TechPoint [] box_1 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(4), EdgeV.fromBottom(4)),
			new Technology.TechPoint(EdgeH.fromRight(4), EdgeV.CENTER),
		};
		Technology.TechPoint [] box_2 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(4), EdgeV.CENTER),
			new Technology.TechPoint(EdgeH.fromRight(4), EdgeV.fromTop(4)),
		};
		Technology.TechPoint [] box_3 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(2), EdgeV.fromBottom(6)),
			new Technology.TechPoint(EdgeH.fromRight(2), EdgeV.fromTop(6)),
		};
		Technology.TechPoint [] box_4 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(2), EdgeV.fromBottom(2)),
			new Technology.TechPoint(EdgeH.fromRight(2), EdgeV.CENTER),
		};
		Technology.TechPoint [] box_5 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(2), EdgeV.CENTER),
			new Technology.TechPoint(EdgeH.fromRight(2), EdgeV.fromTop(2)),
		};
		Technology.TechPoint [] box_6 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.LEFTEDGE, EdgeV.fromBottom(4)),
			new Technology.TechPoint(EdgeH.RIGHTEDGE, EdgeV.fromTop(4)),
		};
		Technology.TechPoint [] box_7 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(1), EdgeV.fromBottom(1)),
			new Technology.TechPoint(EdgeH.fromLeft(3), EdgeV.fromBottom(3)),
		};

		//******************** NODES ********************

		/** Metal-1-Pin */
		PrimitiveNode mp_node = PrimitiveNode.newInstance("Metal-1-Pin", this, 4, 4, new SizeOffset(0, 0, 0, 0),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(PM_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX)
			});
		mp_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, mp_node, new ArcProto [] {Metal_1_arc}, "metal-1", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
			});
		mp_node.setFunction(NodeProto.Function.PIN);
		mp_node.setArcsWipe();
		mp_node.setArcsShrink();

		/** Metal-2-Pin */
		PrimitiveNode mp0_node = PrimitiveNode.newInstance("Metal-2-Pin", this, 4, 4, new SizeOffset(0, 0, 0, 0),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(PM0_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX)
			});
		mp0_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, mp0_node, new ArcProto [] {Metal_2_arc}, "metal-2", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
			});
		mp0_node.setFunction(NodeProto.Function.PIN);
		mp0_node.setArcsWipe();
		mp0_node.setArcsShrink();

		/** Polysilicon-Pin */
		PrimitiveNode pp_node = PrimitiveNode.newInstance("Polysilicon-Pin", this, 2, 2, new SizeOffset(0, 0, 0, 0),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(PP_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX)
			});
		pp_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, pp_node, new ArcProto [] {Polysilicon_arc}, "polysilicon", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(1), EdgeV.fromBottom(1), EdgeH.fromRight(1), EdgeV.fromTop(1))
			});
		pp_node.setFunction(NodeProto.Function.PIN);
		pp_node.setArcsWipe();
		pp_node.setArcsShrink();

		/** S-Active-Pin */
		PrimitiveNode sap_node = PrimitiveNode.newInstance("S-Active-Pin", this, 6, 6, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(PSA_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, Technology.TechPoint.IN2BOX),
				new Technology.NodeLayer(PPS_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX)
			});
		sap_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, sap_node, new ArcProto [] {S_Active_arc}, "s-active", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(3), EdgeV.fromBottom(3), EdgeH.fromRight(3), EdgeV.fromTop(3))
			});
		sap_node.setFunction(NodeProto.Function.PIN);
		sap_node.setArcsWipe();
		sap_node.setArcsShrink();

		/** D-Active-Pin */
		PrimitiveNode dap_node = PrimitiveNode.newInstance("D-Active-Pin", this, 10, 10, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(PPW_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX),
				new Technology.NodeLayer(PDA_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, Technology.TechPoint.IN4BOX)
			});
		dap_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, dap_node, new ArcProto [] {D_Active_arc}, "d-active", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(5), EdgeV.fromBottom(5), EdgeH.fromRight(5), EdgeV.fromTop(5))
			});
		dap_node.setFunction(NodeProto.Function.PIN);
		dap_node.setArcsWipe();
		dap_node.setArcsShrink();

		/** Active-Pin */
		PrimitiveNode ap_node = PrimitiveNode.newInstance("Active-Pin", this, 2, 2, new SizeOffset(0, 0, 0, 0),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(PDA_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX),
				new Technology.NodeLayer(PSA_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX)
			});
		ap_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, ap_node, new ArcProto [] {Active_arc, S_Active_arc, D_Active_arc}, "active", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(1), EdgeV.fromBottom(1), EdgeH.fromRight(1), EdgeV.fromTop(1))
			});
		ap_node.setFunction(NodeProto.Function.PIN);
		ap_node.setArcsWipe();
		ap_node.setArcsShrink();

		/** Metal-1-S-Active-Con */
		PrimitiveNode msac_node = PrimitiveNode.newInstance("Metal-1-S-Active-Con", this, 10, 10, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(M_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.MINBOX, Technology.TechPoint.IN3BOX),
				new Technology.NodeLayer(SA_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.IN2BOX),
				new Technology.NodeLayer(PS_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX),
				new Technology.NodeLayer(AC_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_7)
			});
		msac_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, msac_node, new ArcProto [] {S_Active_arc, Metal_1_arc}, "metal-1-s-act", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(3), EdgeV.fromBottom(3), EdgeH.fromRight(3), EdgeV.fromTop(3))
			});
		msac_node.setFunction(NodeProto.Function.CONTACT);

		/** Metal-1-D-Active-Con */
		PrimitiveNode mdac_node = PrimitiveNode.newInstance("Metal-1-D-Active-Con", this, 14, 14, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(PW_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX),
				new Technology.NodeLayer(M_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.MINBOX, Technology.TechPoint.IN5BOX),
				new Technology.NodeLayer(DA_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.IN4BOX),
				new Technology.NodeLayer(AC_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_7)
			});
		mdac_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, mdac_node, new ArcProto [] {D_Active_arc, Metal_1_arc}, "metal-1-d-act", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(5), EdgeV.fromBottom(5), EdgeH.fromRight(5), EdgeV.fromTop(5))
			});
		mdac_node.setFunction(NodeProto.Function.CONTACT);

		/** Metal-1-Polysilicon-Con */
		PrimitiveNode mpc_node = PrimitiveNode.newInstance("Metal-1-Polysilicon-Con", this, 6, 6, new SizeOffset(0, 0, 0, 0),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(M_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.MINBOX, Technology.TechPoint.IN1BOX),
				new Technology.NodeLayer(P_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX),
				new Technology.NodeLayer(PC_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_7)
			});
		mpc_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, mpc_node, new ArcProto [] {Polysilicon_arc, Metal_1_arc}, "metal-1-polysilicon", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(1), EdgeV.fromBottom(1), EdgeH.fromRight(1), EdgeV.fromTop(1))
			});
		mpc_node.setFunction(NodeProto.Function.CONTACT);

		/** S-Transistor */
		PrimitiveNode st_node = PrimitiveNode.newInstance("S-Transistor", this, 6, 10, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(P_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_6),
				new Technology.NodeLayer(SA_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.IN2BOX),
				new Technology.NodeLayer(PS_lay, -1, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX)
			});
		st_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, st_node, new ArcProto [] {Polysilicon_arc}, "s-trans-poly-left", 180,90, 2, PortProto.Characteristic.UNKNOWN,
					EdgeH.LEFTEDGE, EdgeV.fromBottom(5), EdgeH.fromLeft(1), EdgeV.fromTop(5)),
				PrimitivePort.newInstance(this, st_node, new ArcProto [] {S_Active_arc}, "s-trans-diff-top", 90,90, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(3), EdgeV.fromTop(3), EdgeH.fromRight(3), EdgeV.fromTop(2)),
				PrimitivePort.newInstance(this, st_node, new ArcProto [] {Polysilicon_arc}, "s-trans-poly-right", 0,90, 2, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromRight(1), EdgeV.fromBottom(5), EdgeH.RIGHTEDGE, EdgeV.fromTop(5)),
				PrimitivePort.newInstance(this, st_node, new ArcProto [] {S_Active_arc}, "s-trans-diff-bottom", 270,90, 1, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(3), EdgeV.fromBottom(2), EdgeH.fromRight(3), EdgeV.fromBottom(3))
			});
		st_node.setFunction(NodeProto.Function.TRAPMOS);
		st_node.setHoldsOutline();
		st_node.setShrunk();

		/** D-Transistor */
		PrimitiveNode dt_node = PrimitiveNode.newInstance("D-Transistor", this, 10, 14, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(P_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_3),
				new Technology.NodeLayer(PW_lay, -1, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX),
				new Technology.NodeLayer(DA_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.IN4BOX)
			});
		dt_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, dt_node, new ArcProto [] {Polysilicon_arc}, "d-trans-poly-left", 180,90, 2, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(2), EdgeV.fromBottom(7), EdgeH.fromLeft(3), EdgeV.fromTop(7)),
				PrimitivePort.newInstance(this, dt_node, new ArcProto [] {D_Active_arc}, "d-trans-diff-top", 90,90, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(5), EdgeV.fromTop(5), EdgeH.fromRight(5), EdgeV.fromTop(4)),
				PrimitivePort.newInstance(this, dt_node, new ArcProto [] {Polysilicon_arc}, "d-trans-poly-right", 0,90, 2, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromRight(3), EdgeV.fromBottom(7), EdgeH.fromRight(2), EdgeV.fromTop(7)),
				PrimitivePort.newInstance(this, dt_node, new ArcProto [] {D_Active_arc}, "d-trans-diff-bottom", 270,90, 1, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(5), EdgeV.fromBottom(4), EdgeH.fromRight(5), EdgeV.fromBottom(5))
			});
		dt_node.setFunction(NodeProto.Function.TRANMOS);
		dt_node.setHoldsOutline();
		dt_node.setShrunk();

		/** Metal-1-Metal-2-Con */
		PrimitiveNode mmc_node = PrimitiveNode.newInstance("Metal-1-Metal-2-Con", this, 4, 4, new SizeOffset(0, 0, 0, 0),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(M_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX),
				new Technology.NodeLayer(M0_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX),
				new Technology.NodeLayer(V_lay, 0, Poly.Type.CLOSED, Technology.NodeLayer.BOX, box_7)
			});
		mmc_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, mmc_node, new ArcProto [] {Metal_1_arc, Metal_2_arc}, "metal-1-metal-2", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
			});
		mmc_node.setFunction(NodeProto.Function.CONTACT);

		/** Metal-1-Well-Con */
		PrimitiveNode mwc_node = PrimitiveNode.newInstance("Metal-1-Well-Con", this, 14, 14, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(SAW_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.IN4BOX),
				new Technology.NodeLayer(M_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.MINBOX, Technology.TechPoint.IN5BOX),
				new Technology.NodeLayer(PW_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX),
				new Technology.NodeLayer(PS_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.MINBOX, Technology.TechPoint.IN5BOX),
				new Technology.NodeLayer(AC_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_7)
			});
		mwc_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, mwc_node, new ArcProto [] {Metal_1_arc, Active_arc}, "metal-1-well", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(5.5), EdgeV.fromBottom(5.5), EdgeH.fromRight(5.5), EdgeV.fromTop(5.5))
			});
		mwc_node.setFunction(NodeProto.Function.WELL);

		/** Metal-1-Substrate-Con */
		PrimitiveNode msc_node = PrimitiveNode.newInstance("Metal-1-Substrate-Con", this, 6, 6, new SizeOffset(0, 0, 0, 0),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(M_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.MINBOX, Technology.TechPoint.IN1BOX),
				new Technology.NodeLayer(DA_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX),
				new Technology.NodeLayer(AC_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_7)
			});
		msc_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, msc_node, new ArcProto [] {Metal_1_arc, Active_arc}, "metal-1-substrate", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
			});
		msc_node.setFunction(NodeProto.Function.SUBSTRATE);

		/** Metal-1-Node */
		PrimitiveNode mn_node = PrimitiveNode.newInstance("Metal-1-Node", this, 4, 4, new SizeOffset(0, 0, 0, 0),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(M_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX)
			});
		mn_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, mn_node, new ArcProto [] {Metal_1_arc}, "metal-1", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
			});
		mn_node.setFunction(NodeProto.Function.NODE);
		mn_node.setHoldsOutline();

		/** Metal-2-Node */
		PrimitiveNode mn0_node = PrimitiveNode.newInstance("Metal-2-Node", this, 4, 4, new SizeOffset(0, 0, 0, 0),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(M0_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX)
			});
		mn0_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, mn0_node, new ArcProto [] {Metal_2_arc}, "metal-2", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
			});
		mn0_node.setFunction(NodeProto.Function.NODE);
		mn0_node.setHoldsOutline();

		/** Polysilicon-Node */
		PrimitiveNode pn_node = PrimitiveNode.newInstance("Polysilicon-Node", this, 4, 4, new SizeOffset(0, 0, 0, 0),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(P_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX)
			});
		pn_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, pn_node, new ArcProto [] {Polysilicon_arc}, "polysilicon", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(1), EdgeV.fromBottom(1), EdgeH.fromRight(1), EdgeV.fromTop(1))
			});
		pn_node.setFunction(NodeProto.Function.NODE);
		pn_node.setHoldsOutline();

		/** Active-Node */
		PrimitiveNode an_node = PrimitiveNode.newInstance("Active-Node", this, 4, 4, new SizeOffset(0, 0, 0, 0),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(SA_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX)
			});
		an_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, an_node, new ArcProto [] {Active_arc, S_Active_arc, D_Active_arc}, "active", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(1), EdgeV.fromBottom(1), EdgeH.fromRight(1), EdgeV.fromTop(1))
			});
		an_node.setFunction(NodeProto.Function.NODE);
		an_node.setHoldsOutline();

		/** D-Active-Node */
		PrimitiveNode dan_node = PrimitiveNode.newInstance("D-Active-Node", this, 4, 4, new SizeOffset(0, 0, 0, 0),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(DA_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX)
			});
		dan_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, dan_node, new ArcProto [] {Active_arc, S_Active_arc, D_Active_arc}, "active", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(1), EdgeV.fromBottom(1), EdgeH.fromRight(1), EdgeV.fromTop(1))
			});
		dan_node.setFunction(NodeProto.Function.NODE);
		dan_node.setHoldsOutline();

		/** P-Select-Node */
		PrimitiveNode psn_node = PrimitiveNode.newInstance("P-Select-Node", this, 6, 6, new SizeOffset(0, 0, 0, 0),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(PS_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX)
			});
		psn_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, psn_node, new ArcProto [] {}, "select", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.LEFTEDGE, EdgeV.BOTTOMEDGE, EdgeH.RIGHTEDGE, EdgeV.TOPEDGE)
			});
		psn_node.setFunction(NodeProto.Function.NODE);
		psn_node.setHoldsOutline();

		/** Cut-Node */
		PrimitiveNode cn_node = PrimitiveNode.newInstance("Cut-Node", this, 2, 2, new SizeOffset(0, 0, 0, 0),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(CC_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX)
			});
		cn_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, cn_node, new ArcProto [] {}, "cut", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.LEFTEDGE, EdgeV.BOTTOMEDGE, EdgeH.RIGHTEDGE, EdgeV.TOPEDGE)
			});
		cn_node.setFunction(NodeProto.Function.NODE);
		cn_node.setHoldsOutline();

		/** Poly-Cut-Node */
		PrimitiveNode pcn_node = PrimitiveNode.newInstance("Poly-Cut-Node", this, 2, 2, new SizeOffset(0, 0, 0, 0),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(PC_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX)
			});
		pcn_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, pcn_node, new ArcProto [] {}, "polycut", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.LEFTEDGE, EdgeV.BOTTOMEDGE, EdgeH.RIGHTEDGE, EdgeV.TOPEDGE)
			});
		pcn_node.setFunction(NodeProto.Function.NODE);
		pcn_node.setHoldsOutline();

		/** Active-Cut-Node */
		PrimitiveNode acn_node = PrimitiveNode.newInstance("Active-Cut-Node", this, 2, 2, new SizeOffset(0, 0, 0, 0),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(AC_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX)
			});
		acn_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, acn_node, new ArcProto [] {}, "activecut", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.LEFTEDGE, EdgeV.BOTTOMEDGE, EdgeH.RIGHTEDGE, EdgeV.TOPEDGE)
			});
		acn_node.setFunction(NodeProto.Function.NODE);
		acn_node.setHoldsOutline();

		/** Via-Node */
		PrimitiveNode vn_node = PrimitiveNode.newInstance("Via-Node", this, 2, 2, new SizeOffset(0, 0, 0, 0),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(V_lay, 0, Poly.Type.CLOSED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX)
			});
		vn_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, vn_node, new ArcProto [] {}, "via", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.LEFTEDGE, EdgeV.BOTTOMEDGE, EdgeH.RIGHTEDGE, EdgeV.TOPEDGE)
			});
		vn_node.setFunction(NodeProto.Function.NODE);
		vn_node.setHoldsOutline();

		/** P-Well-Node */
		PrimitiveNode pwn_node = PrimitiveNode.newInstance("P-Well-Node", this, 6, 6, new SizeOffset(0, 0, 0, 0),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(PW_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX)
			});
		pwn_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, pwn_node, new ArcProto [] {S_Active_arc}, "well", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(3), EdgeV.fromBottom(3), EdgeH.fromRight(3), EdgeV.fromTop(3))
			});
		pwn_node.setFunction(NodeProto.Function.NODE);
		pwn_node.setHoldsOutline();

		/** Passivation-Node */
		PrimitiveNode pn0_node = PrimitiveNode.newInstance("Passivation-Node", this, 8, 8, new SizeOffset(0, 0, 0, 0),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(P0_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX)
			});
		pn0_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, pn0_node, new ArcProto [] {}, "passivation", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.LEFTEDGE, EdgeV.BOTTOMEDGE, EdgeH.RIGHTEDGE, EdgeV.TOPEDGE)
			});
		pn0_node.setFunction(NodeProto.Function.NODE);
		pn0_node.setHoldsOutline();

		/** Pad-Frame-Node */
		PrimitiveNode pfn_node = PrimitiveNode.newInstance("Pad-Frame-Node", this, 8, 8, new SizeOffset(0, 0, 0, 0),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(PF_lay, 0, Poly.Type.CLOSED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX)
			});
		pfn_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, pfn_node, new ArcProto [] {}, "pad-frame", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.LEFTEDGE, EdgeV.BOTTOMEDGE, EdgeH.RIGHTEDGE, EdgeV.TOPEDGE)
			});
		pfn_node.setFunction(NodeProto.Function.NODE);
		pfn_node.setHoldsOutline();
	};
}
