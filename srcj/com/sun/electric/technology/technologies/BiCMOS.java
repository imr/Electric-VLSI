/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: BiCMOS.java
 * Bipolar/CMOS technology description
 * Generated automatically from a library
 *
 * Copyright (c) 2004 Sun Microsystems and Static Free Software
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
package com.sun.electric.technology.technologies;

import com.sun.electric.database.geometry.EGraphics;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.technology.*;
import com.sun.electric.technology.technologies.utils.MOSRules;

import java.awt.Color;

/**
 * This is the Bipolar/CMOS (from MOSIS, N-Well, SCE Rules) Technology.
 */
public class BiCMOS extends Technology
{
	/** the Bipolar/CMOS (from MOSIS, N-Well, SCE Rules) Technology object. */	public static final BiCMOS tech = new BiCMOS();
	private static final double XX = -1;
	private double [] conDist, unConDist;

	// -------------------- private and protected methods ------------------------
	private BiCMOS()
	{
		super("bicmos");
		setTechDesc("Bipolar/CMOS (from MOSIS, N-Well, SCE Rules)");
		setFactoryScale(1000, true);   // in nanometers: really 1 microns
		setNoNegatedArcs();
		setStaticTechnology();

        // Foundry
        Foundry mosis = new Foundry(Foundry.Type.MOSIS);
        foundries.add(mosis);

		setFactoryTransparentLayers(new Color []
		{
			new Color( 96,209,255), // layer 1
			new Color(255,155,192), // layer 2
			new Color(107,226, 96), // layer 3
			new Color(224, 95,255), // layer 4
			new Color(240,221,181), // layer 5
		});

		//**************************************** LAYERS ****************************************

		/** PS layer */
		Layer PS_lay = Layer.newInstance(this, "P_Select",
			new EGraphics(true, true, EGraphics.Outline.PAT_S, 0, 255,255,0, 0.8,true,
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
		Layer NS_lay = Layer.newInstance(this, "N_Select",
			new EGraphics(true, true, EGraphics.Outline.PAT_S, 0, 175,255,175, 0.8,true,
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

		/** NW layer */
		Layer NW_lay = Layer.newInstance(this, "N_Well",
			new EGraphics(false, false, null, EGraphics.TRANSPARENT_5, 240,221,181,/*0,0,0,*/ 0.8,true,
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

		/** V layer */
		Layer V_lay = Layer.newInstance(this, "Via",
			new EGraphics(false, false, null, 0, 0,0,0, 0.8,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** P layer */
		Layer P_lay = Layer.newInstance(this, "Passivation",
			new EGraphics(true, true, null, 0, 100,100,100, 0.8,true,
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

		/** PF layer */
		Layer PF_lay = Layer.newInstance(this, "Pad_Frame",
			new EGraphics(false, false, null, 0, 255,0,0, 0.8,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** T layer */
		Layer T_lay = Layer.newInstance(this, "Transistor",
			new EGraphics(false, false, null, 0, 200,200,200, 0.8,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** AC layer */
		Layer AC_lay = Layer.newInstance(this, "Active_Cut",
			new EGraphics(false, false, null, 0, 0,0,0, 0.8,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** PM layer */
		Layer PM_lay = Layer.newInstance(this, "Pseudo_Metal_1",
			new EGraphics(false, false, null, EGraphics.TRANSPARENT_1, 96,209,255,/*107,226,96,*/ 0.8,true,
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
		Layer PM0_lay = Layer.newInstance(this, "Pseudo_Metal_2",
			new EGraphics(false, false, null, EGraphics.TRANSPARENT_4, 224, 95,255,/*0,0,0,*/ 0.8,true,
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
		Layer PP_lay = Layer.newInstance(this, "Pseudo_Polysilicon",
			new EGraphics(false, false, null, EGraphics.TRANSPARENT_2, 255,155,192,/*224,95,255,*/ 0.8,true,
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

		/** PPS layer */
		Layer PPS_lay = Layer.newInstance(this, "Pseudo_P_Select",
			new EGraphics(true, true, EGraphics.Outline.PAT_S, 0, 255,255,0, 0.8,true,
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
		Layer PNS_lay = Layer.newInstance(this, "Pseudo_N_Select",
			new EGraphics(true, true, EGraphics.Outline.PAT_S, 0, 175,255,175, 0.8,true,
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

		/** PNW layer */
		Layer PNW_lay = Layer.newInstance(this, "Pseudo_N_Well",
			new EGraphics(false, false, null, EGraphics.TRANSPARENT_5, 240,221,181,/*0,0,0,*/ 0.8,true,
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

		/** PP0 layer */
		Layer PP0_lay = Layer.newInstance(this, "Pseudo_Polysilicon_2",
			new EGraphics(false, false, null, 0, 255,0,255, 0.8,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** M layer */
		Layer M_lay = Layer.newInstance(this, "M1",
			new EGraphics(false, false, null, EGraphics.TRANSPARENT_1, 96,209,255,/*107,226,96,*/ 0.8,true,
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
		Layer M0_lay = Layer.newInstance(this, "M2",
			new EGraphics(false, false, null, EGraphics.TRANSPARENT_4, 224, 95,255,/*0,0,0,*/ 0.8,true,
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

		/** P0 layer */
		Layer P0_lay = Layer.newInstance(this, "Poly1",
			new EGraphics(false, false, null, EGraphics.TRANSPARENT_2, 255,155,192,/*224,95,255,*/ 0.8,true,
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

		/** P1 layer */
		Layer P1_lay = Layer.newInstance(this, "Poly2",
			new EGraphics(true, true, EGraphics.Outline.PAT_S, 0, 255,0,255, 0.8,true,
			new int[] { 0xe0e0,   // XXX     XXX     
						0x7070,   //  XXX     XXX    
						0x3838,   //   XXX     XXX   
						0x1c1c,   //    XXX     XXX  
						0x0e0e,   //     XXX     XXX 
						0x0707,   //      XXX     XXX
						0x8383,   // X     XXX     XX
						0xc1c1,   // XX     XXX     X
						0xe0e0,   // XXX     XXX     
						0x7070,   //  XXX     XXX    
						0x3838,   //   XXX     XXX   
						0x1c1c,   //    XXX     XXX  
						0x0e0e,   //     XXX     XXX 
						0x0707,   //      XXX     XXX
						0x8383,   // X     XXX     XX
						0xc1c1}));// XX     XXX     X

		/** A layer */
		Layer A_lay = Layer.newInstance(this, "Active",
			new EGraphics(false, false, null, EGraphics.TRANSPARENT_3, 107,226, 96,/*240,221,181,*/ 0.8,true,
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

		/** PC layer */
		Layer PC_lay = Layer.newInstance(this, "Poly1_Cut",
			new EGraphics(false, false, null, 0, 0,0,0, 0.8,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** PC0 layer */
		Layer PC0_lay = Layer.newInstance(this, "Poly2_Cut",
			new EGraphics(false, false, null, 0, 0,0,0, 0.8,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** PA layer */
		Layer PA_lay = Layer.newInstance(this, "Pseudo_Active",
			new EGraphics(false, false, null, EGraphics.TRANSPARENT_3, 107,226, 96,/*240,221,181,*/ 0.8,true,
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

		/** PBA layer */
		Layer PBA_lay = Layer.newInstance(this, "P_Base_Active",
			new EGraphics(true, true, EGraphics.Outline.PAT_S, EGraphics.TRANSPARENT_3, 107,226, 96,/*240,221,181,*/ 0.8,true,
			new int[] { 0x4444,   //  X   X   X   X  
						0x2222,   //   X   X   X   X 
						0x1111,   //    X   X   X   X
						0x8888,   // X   X   X   X   
						0x4444,   //  X   X   X   X  
						0x2222,   //   X   X   X   X 
						0x1111,   //    X   X   X   X
						0x0888,   //     X   X   X   
						0x4444,   //  X   X   X   X  
						0x2222,   //   X   X   X   X 
						0x1111,   //    X   X   X   X
						0x8888,   // X   X   X   X   
						0x4444,   //  X   X   X   X  
						0x2222,   //   X   X   X   X 
						0x1111,   //    X   X   X   X
						0x0888}));//     X   X   X   

		/** B layer */
		Layer B_lay = Layer.newInstance(this, "BCCD",
			new EGraphics(true, true, EGraphics.Outline.PAT_S, 0, 255,150,150, 0.8,true,
			new int[] { 0x8888,   // X   X   X   X   
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
						0x0000,   //                 
						0x2222,   //   X   X   X   X 
						0x0000}));//                 

		/** OS layer */
		Layer OS_lay = Layer.newInstance(this, "Ohmic_Substrate",
			new EGraphics(false, false, null, EGraphics.TRANSPARENT_3, 107,226, 96,/*240,221,181,*/ 0.8,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** OW layer */
		Layer OW_lay = Layer.newInstance(this, "Ohmic_Well",
			new EGraphics(false, false, null, EGraphics.TRANSPARENT_3, 107,226, 96,/*240,221,181,*/ 0.8,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		// The layer functions
		PS_lay.setFunction(Layer.Function.IMPLANTP);		// P_Select
		NS_lay.setFunction(Layer.Function.IMPLANTN);		// N_Select
		NW_lay.setFunction(Layer.Function.WELLN);		// N_Well
		V_lay.setFunction(Layer.Function.CONTACT2);		// Via
		P_lay.setFunction(Layer.Function.OVERGLASS);		// Passivation
		PF_lay.setFunction(Layer.Function.ART);		// Pad_Frame
		T_lay.setFunction(Layer.Function.TRANSISTOR, Layer.Function.PSEUDO);		// Transistor
		AC_lay.setFunction(Layer.Function.CONTACT1);		// Active_Cut
		PM_lay.setFunction(Layer.Function.METAL1, Layer.Function.PSEUDO);		// Pseudo_Metal_1
		PM0_lay.setFunction(Layer.Function.METAL2, Layer.Function.PSEUDO);		// Pseudo_Metal_2
		PP_lay.setFunction(Layer.Function.POLY1, Layer.Function.PSEUDO);		// Pseudo_Polysilicon
		PPS_lay.setFunction(Layer.Function.IMPLANTP, Layer.Function.PSEUDO);		// Pseudo_P_Select
		PNS_lay.setFunction(Layer.Function.IMPLANTN, Layer.Function.PSEUDO);		// Pseudo_N_Select
		PNW_lay.setFunction(Layer.Function.WELLN, Layer.Function.PSEUDO);		// Pseudo_N_Well
		PP0_lay.setFunction(Layer.Function.POLY2, Layer.Function.PSEUDO);		// Pseudo_Polysilicon_2
		M_lay.setFunction(Layer.Function.METAL1);		// M1
		M0_lay.setFunction(Layer.Function.METAL2);		// M2
		P0_lay.setFunction(Layer.Function.POLY1);		// Poly1
		P1_lay.setFunction(Layer.Function.POLY2);		// Poly2
		A_lay.setFunction(Layer.Function.DIFF);		// Active
		PC_lay.setFunction(Layer.Function.CONTACT1);		// Poly1_Cut
		PC0_lay.setFunction(Layer.Function.CONTACT3);		// Poly2_Cut
		PA_lay.setFunction(Layer.Function.DIFF, Layer.Function.PSEUDO);		// Pseudo_Active
		PBA_lay.setFunction(Layer.Function.DIFFP);		// P_Base_Active
		B_lay.setFunction(Layer.Function.IMPLANTN);		// BCCD
		OS_lay.setFunction(Layer.Function.SUBSTRATE);		// Ohmic_Substrate
		OW_lay.setFunction(Layer.Function.WELL);		// Ohmic_Well

		// The CIF names
		PS_lay.setFactoryCIFLayer("CSP");		// P_Select
		NS_lay.setFactoryCIFLayer("CSN");		// N_Select
		NW_lay.setFactoryCIFLayer("CWN");		// N_Well
		V_lay.setFactoryCIFLayer("CVA");		// Via
		P_lay.setFactoryCIFLayer("COG");		// Passivation
		PF_lay.setFactoryCIFLayer("XP");		// Pad_Frame
		T_lay.setFactoryCIFLayer("");		// Transistor
		AC_lay.setFactoryCIFLayer("CCA");		// Active_Cut
		PM_lay.setFactoryCIFLayer("");		// Pseudo_Metal_1
		PM0_lay.setFactoryCIFLayer("");		// Pseudo_Metal_2
		PP_lay.setFactoryCIFLayer("");		// Pseudo_Polysilicon
		PPS_lay.setFactoryCIFLayer("");		// Pseudo_P_Select
		PNS_lay.setFactoryCIFLayer("");		// Pseudo_N_Select
		PNW_lay.setFactoryCIFLayer("");		// Pseudo_N_Well
		PP0_lay.setFactoryCIFLayer("");		// Pseudo_Polysilicon_2
		M_lay.setFactoryCIFLayer("CMF");		// M1
		M0_lay.setFactoryCIFLayer("CMS");		// M2
		P0_lay.setFactoryCIFLayer("CPG");		// Poly1
		P1_lay.setFactoryCIFLayer("CEL");		// Poly2
		A_lay.setFactoryCIFLayer("CAA");		// Active
		PC_lay.setFactoryCIFLayer("CCP");		// Poly1_Cut
		PC0_lay.setFactoryCIFLayer("CCE");		// Poly2_Cut
		PA_lay.setFactoryCIFLayer("");		// Pseudo_Active
		PBA_lay.setFactoryCIFLayer("CBA");		// P_Base_Active
		B_lay.setFactoryCIFLayer("CCD");		// BCCD
		OS_lay.setFactoryCIFLayer("CAA");		// Ohmic_Substrate
		OW_lay.setFactoryCIFLayer("CAA");		// Ohmic_Well

		// The DXF names
		PS_lay.setFactoryDXFLayer("");		// P_Select
		NS_lay.setFactoryDXFLayer("");		// N_Select
		NW_lay.setFactoryDXFLayer("");		// N_Well
		V_lay.setFactoryDXFLayer("");		// Via
		P_lay.setFactoryDXFLayer("");		// Passivation
		PF_lay.setFactoryDXFLayer("");		// Pad_Frame
		T_lay.setFactoryDXFLayer("");		// Transistor
		AC_lay.setFactoryDXFLayer("");		// Active_Cut
		PM_lay.setFactoryDXFLayer("");		// Pseudo_Metal_1
		PM0_lay.setFactoryDXFLayer("");		// Pseudo_Metal_2
		PP_lay.setFactoryDXFLayer("");		// Pseudo_Polysilicon
		PPS_lay.setFactoryDXFLayer("");		// Pseudo_P_Select
		PNS_lay.setFactoryDXFLayer("");		// Pseudo_N_Select
		PNW_lay.setFactoryDXFLayer("");		// Pseudo_N_Well
		PP0_lay.setFactoryDXFLayer("");		// Pseudo_Polysilicon_2
		M_lay.setFactoryDXFLayer("");		// M1
		M0_lay.setFactoryDXFLayer("");		// M2
		P0_lay.setFactoryDXFLayer("");		// Poly1
		P1_lay.setFactoryDXFLayer("");		// Poly2
		A_lay.setFactoryDXFLayer("");		// Active
		PC_lay.setFactoryDXFLayer("");		// Poly1_Cut
		PC0_lay.setFactoryDXFLayer("");		// Poly2_Cut
		PA_lay.setFactoryDXFLayer("");		// Pseudo_Active
		PBA_lay.setFactoryDXFLayer("");		// P_Base_Active
		B_lay.setFactoryDXFLayer("");		// BCCD
		OS_lay.setFactoryDXFLayer("");		// Ohmic_Substrate
		OW_lay.setFactoryDXFLayer("");		// Ohmic_Well

		// The GDS names
		mosis.setFactoryGDSLayer(PS_lay, "8");		// P_Select
		mosis.setFactoryGDSLayer(NS_lay, "7");		// N_Select
		mosis.setFactoryGDSLayer(NW_lay, "1");		// N_Well
		mosis.setFactoryGDSLayer(V_lay, "");		// Via
		mosis.setFactoryGDSLayer(P_lay, "13");		// Passivation
		mosis.setFactoryGDSLayer(PF_lay, "9");		// Pad_Frame
		mosis.setFactoryGDSLayer(T_lay, "");		// Transistor
		mosis.setFactoryGDSLayer(AC_lay, "35");		// Active_Cut
		mosis.setFactoryGDSLayer(PM_lay, "");		// Pseudo_Metal_1
		mosis.setFactoryGDSLayer(PM0_lay, "");		// Pseudo_Metal_2
		mosis.setFactoryGDSLayer(PP_lay, "");		// Pseudo_Polysilicon
		mosis.setFactoryGDSLayer(PPS_lay, "");		// Pseudo_P_Select
		mosis.setFactoryGDSLayer(PNS_lay, "");		// Pseudo_N_Select
		mosis.setFactoryGDSLayer(PNW_lay, "");		// Pseudo_N_Well
		mosis.setFactoryGDSLayer(PP0_lay, "");		// Pseudo_Polysilicon_2
		mosis.setFactoryGDSLayer(M_lay, "10");		// M1
		mosis.setFactoryGDSLayer(M0_lay, "12");		// M2
		mosis.setFactoryGDSLayer(P0_lay, "4");		// Poly1
		mosis.setFactoryGDSLayer(P1_lay, "19");		// Poly2
		mosis.setFactoryGDSLayer(A_lay, "31");		// Active
		mosis.setFactoryGDSLayer(PC_lay, "45");		// Poly1_Cut
		mosis.setFactoryGDSLayer(PC0_lay, "55");		// Poly2_Cut
		mosis.setFactoryGDSLayer(PA_lay, "");		// Pseudo_Active
		mosis.setFactoryGDSLayer(PBA_lay, "33");		// P_Base_Active
		mosis.setFactoryGDSLayer(B_lay, "17");		// BCCD
		mosis.setFactoryGDSLayer(OS_lay, "3");		// Ohmic_Substrate
		mosis.setFactoryGDSLayer(OW_lay, "3");		// Ohmic_Well

		//******************** DESIGN RULES ********************

		conDist = new double[] {
			//            P  N  N  V  P  P  T  A  P  P  P  P  P  P  P  M  M  P  P  A  P  P  P  P  B  O  O  
			//            S  S  W        F     C  M  M  P  P  N  N  P     0  0  1     C  C  A  B     S  W  
			//                                       0     S  S  W  0                    0     A           
			//                                                                                             
			//                                                                                             
			//                                                                                             
			/* PS     */ XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,
			/* NS     */    XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,
			/* NW     */       XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,
			/* V      */          2,XX,XX,XX,2,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,2,XX,XX,XX,XX,XX,XX,
			/* P      */             XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,
			/* PF     */                XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,
			/* T      */                   XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,
			/* AC     */                      2,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,
			/* PM     */                         XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,
			/* PM0    */                            XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,
			/* PP     */                               XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,
			/* PPS    */                                  XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,
			/* PNS    */                                     XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,
			/* PNW    */                                        XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,
			/* PP0    */                                           XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,
			/* M      */                                              XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,
			/* M0     */                                                 XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,
			/* P0     */                                                    XX,XX,1,XX,XX,XX,XX,XX,XX,XX,
			/* P1     */                                                       XX,XX,XX,XX,XX,XX,XX,XX,XX,
			/* A      */                                                          XX,XX,XX,XX,XX,XX,XX,XX,
			/* PC     */                                                             2,XX,XX,XX,XX,XX,XX,
			/* PC0    */                                                                XX,XX,XX,XX,XX,XX,
			/* PA     */                                                                   XX,XX,XX,XX,XX,
			/* PBA    */                                                                      XX,XX,XX,XX,
			/* B      */                                                                         XX,XX,XX,
			/* OS     */                                                                            XX,XX,
			/* OW     */                                                                               XX
		};

		unConDist = new double[] {
			//            P  N  N  V  P  P  T  A  P  P  P  P  P  P  P  M  M  P  P  A  P  P  P  P  B  O  O  
			//            S  S  W        F     C  M  M  P  P  N  N  P     0  0  1     C  C  A  B     S  W  
			//                                       0     S  S  W  0                    0     A           
			//                                                                                             
			//                                                                                             
			//                                                                                             
			/* PS     */ XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,
			/* NS     */    XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,
			/* NW     */       XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,
			/* V      */          3,XX,XX,2,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,2,XX,2,XX,XX,XX,XX,XX,XX,XX,
			/* P      */             XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,
			/* PF     */                XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,
			/* T      */                   XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,
			/* AC     */                      XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,5,XX,XX,XX,XX,XX,XX,XX,
			/* PM     */                         XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,
			/* PM0    */                            XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,
			/* PP     */                               XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,
			/* PPS    */                                  XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,
			/* PNS    */                                     XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,
			/* PNW    */                                        XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,
			/* PP0    */                                           XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,
			/* M      */                                              3,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,
			/* M0     */                                                 4,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,
			/* P0     */                                                    2,XX,1,4,XX,XX,XX,XX,XX,XX,
			/* P1     */                                                       3,XX,XX,XX,XX,XX,XX,XX,XX,
			/* A      */                                                          3,XX,XX,XX,XX,XX,XX,XX,
			/* PC     */                                                             XX,XX,XX,XX,XX,XX,XX,
			/* PC0    */                                                                XX,XX,XX,XX,XX,XX,
			/* PA     */                                                                   XX,XX,XX,XX,XX,
			/* PBA    */                                                                      XX,XX,XX,XX,
			/* B      */                                                                         XX,XX,XX,
			/* OS     */                                                                            XX,XX,
			/* OW     */                                                                               XX
		};

		//******************** ARCS ********************

		/** Metal_1 arc */
		ArcProto Metal_1_arc = ArcProto.newInstance(this, "Metal_1", 3, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(M_lay, 0, Poly.Type.FILLED)
		});
		Metal_1_arc.setFunction(ArcProto.Function.METAL1);
		Metal_1_arc.setWipable();
		Metal_1_arc.setFactoryFixedAngle(true);
		Metal_1_arc.setFactoryAngleIncrement(90);

		/** Metal_2 arc */
		ArcProto Metal_2_arc = ArcProto.newInstance(this, "Metal_2", 3, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(M0_lay, 0, Poly.Type.FILLED)
		});
		Metal_2_arc.setFunction(ArcProto.Function.METAL2);
		Metal_2_arc.setWipable();
		Metal_2_arc.setFactoryFixedAngle(true);
		Metal_2_arc.setFactoryAngleIncrement(90);

		/** Polysilicon arc */
		ArcProto Polysilicon_arc = ArcProto.newInstance(this, "Polysilicon", 2, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(P0_lay, 0, Poly.Type.FILLED)
		});
		Polysilicon_arc.setFunction(ArcProto.Function.POLY1);
		Polysilicon_arc.setWipable();
		Polysilicon_arc.setFactoryFixedAngle(true);
		Polysilicon_arc.setFactoryAngleIncrement(90);

		/** Polysilicon_2 arc */
		ArcProto Polysilicon_2_arc = ArcProto.newInstance(this, "Polysilicon_2", 2, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(P1_lay, 0, Poly.Type.FILLED)
		});
		Polysilicon_2_arc.setFunction(ArcProto.Function.POLY2);
		Polysilicon_2_arc.setWipable();
		Polysilicon_2_arc.setFactoryFixedAngle(true);
		Polysilicon_2_arc.setFactoryAngleIncrement(90);

		/** Active arc */
		ArcProto Active_arc = ArcProto.newInstance(this, "Active", 2, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(A_lay, 0, Poly.Type.FILLED)
		});
		Active_arc.setFunction(ArcProto.Function.METAL5);
		Active_arc.setWipable();
		Active_arc.setFactoryFixedAngle(true);
		Active_arc.setFactoryAngleIncrement(90);

		/** Pdiff arc */
		ArcProto Pdiff_arc = ArcProto.newInstance(this, "Pdiff", 12, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(NW_lay, 0, Poly.Type.FILLED),
			new Technology.ArcLayer(A_lay, 10, Poly.Type.FILLED),
			new Technology.ArcLayer(PS_lay, 6, Poly.Type.CLOSED)
		});
		Pdiff_arc.setFunction(ArcProto.Function.DIFFP);
		Pdiff_arc.setWipable();
		Pdiff_arc.setWidthOffset(0);
		Pdiff_arc.setFactoryFixedAngle(true);
		Pdiff_arc.setFactoryAngleIncrement(90);

		/** Ndiff arc */
		ArcProto Ndiff_arc = ArcProto.newInstance(this, "Ndiff", 6, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(A_lay, 4, Poly.Type.FILLED),
			new Technology.ArcLayer(NS_lay, 0, Poly.Type.FILLED)
		});
		Ndiff_arc.setFunction(ArcProto.Function.DIFFN);
		Ndiff_arc.setWipable();
		Ndiff_arc.setWidthOffset(0);
		Ndiff_arc.setFactoryFixedAngle(true);
		Ndiff_arc.setFactoryAngleIncrement(90);

		//******************** RECTANGLE DESCRIPTIONS ********************

		Technology.TechPoint [] box_1 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(2), EdgeV.fromBottom(2)),
			new Technology.TechPoint(EdgeH.fromRight(2), EdgeV.makeCenter()),
		};
		Technology.TechPoint [] box_2 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(2), EdgeV.makeCenter()),
			new Technology.TechPoint(EdgeH.fromRight(2), EdgeV.fromTop(2)),
		};
		Technology.TechPoint [] box_3 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.makeLeftEdge(), EdgeV.fromBottom(4)),
			new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.fromTop(4)),
		};
		Technology.TechPoint [] box_4 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(4), EdgeV.fromBottom(4)),
			new Technology.TechPoint(EdgeH.fromRight(4), EdgeV.fromTop(4)),
			new Technology.TechPoint(EdgeH.fromCenter(-2), EdgeV.fromCenter(-2)),
			new Technology.TechPoint(EdgeH.fromCenter(2), EdgeV.fromCenter(2)),
		};
		Technology.TechPoint [] box_5 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromCenter(-1), EdgeV.fromCenter(-1)),
			new Technology.TechPoint(EdgeH.fromCenter(1), EdgeV.fromCenter(1)),
		};
		Technology.TechPoint [] box_6 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(5), EdgeV.fromBottom(5)),
			new Technology.TechPoint(EdgeH.fromRight(5), EdgeV.makeCenter()),
		};
		Technology.TechPoint [] box_7 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(5), EdgeV.makeCenter()),
			new Technology.TechPoint(EdgeH.fromRight(5), EdgeV.fromTop(5)),
		};
		Technology.TechPoint [] box_8 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(3), EdgeV.fromBottom(7)),
			new Technology.TechPoint(EdgeH.fromRight(3), EdgeV.fromTop(7)),
		};
		Technology.TechPoint [] box_9 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(1), EdgeV.fromBottom(1)),
			new Technology.TechPoint(EdgeH.fromRight(1), EdgeV.fromTop(1)),
			new Technology.TechPoint(EdgeH.fromCenter(-2), EdgeV.fromCenter(-2)),
			new Technology.TechPoint(EdgeH.fromCenter(2), EdgeV.fromCenter(2)),
		};
		Technology.TechPoint [] box_10 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(3), EdgeV.fromBottom(3)),
			new Technology.TechPoint(EdgeH.fromRight(3), EdgeV.fromTop(3)),
			new Technology.TechPoint(EdgeH.fromCenter(-2), EdgeV.fromCenter(-2)),
			new Technology.TechPoint(EdgeH.fromCenter(2), EdgeV.fromCenter(2)),
		};
		Technology.TechPoint [] box_11 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(6), EdgeV.fromBottom(6)),
			new Technology.TechPoint(EdgeH.fromRight(6), EdgeV.fromTop(6)),
			new Technology.TechPoint(EdgeH.fromCenter(-2), EdgeV.fromCenter(-2)),
			new Technology.TechPoint(EdgeH.fromCenter(2), EdgeV.fromCenter(2)),
		};
		Technology.TechPoint [] box_12 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(5), EdgeV.fromBottom(5)),
			new Technology.TechPoint(EdgeH.fromRight(5), EdgeV.fromTop(5)),
		};
		Technology.TechPoint [] box_13 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(1), EdgeV.fromBottom(1)),
			new Technology.TechPoint(EdgeH.fromLeft(3), EdgeV.fromBottom(3)),
		};
		Technology.TechPoint [] box_14 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(36), EdgeV.fromBottom(11)),
			new Technology.TechPoint(EdgeH.fromRight(8), EdgeV.fromTop(23)),
		};
		Technology.TechPoint [] box_15 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(34), EdgeV.fromBottom(9)),
			new Technology.TechPoint(EdgeH.fromRight(6), EdgeV.fromTop(21)),
		};
		Technology.TechPoint [] box_16 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(35), EdgeV.fromBottom(10)),
			new Technology.TechPoint(EdgeH.fromRight(7), EdgeV.fromTop(22)),
		};
		Technology.TechPoint [] box_17 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(25), EdgeV.fromBottom(11)),
			new Technology.TechPoint(EdgeH.fromRight(19), EdgeV.fromTop(23)),
		};
		Technology.TechPoint [] box_18 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(24), EdgeV.fromBottom(22)),
			new Technology.TechPoint(EdgeH.fromRight(18), EdgeV.fromTop(10)),
		};
		Technology.TechPoint [] box_19 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(36), EdgeV.fromBottom(23)),
			new Technology.TechPoint(EdgeH.fromRight(8), EdgeV.fromTop(11)),
		};
		Technology.TechPoint [] box_20 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(3), EdgeV.fromBottom(22)),
			new Technology.TechPoint(EdgeH.fromRight(39), EdgeV.fromTop(10)),
		};
		Technology.TechPoint [] box_21 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(2), EdgeV.fromBottom(21)),
			new Technology.TechPoint(EdgeH.fromRight(38), EdgeV.fromTop(9)),
		};
		Technology.TechPoint [] box_22 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(2), EdgeV.fromBottom(9)),
			new Technology.TechPoint(EdgeH.fromRight(38), EdgeV.fromTop(21)),
		};
		Technology.TechPoint [] box_23 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(24), EdgeV.fromBottom(10)),
			new Technology.TechPoint(EdgeH.fromRight(18), EdgeV.fromTop(22)),
		};
		Technology.TechPoint [] box_24 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.makeLeftEdge(), EdgeV.fromBottom(7)),
			new Technology.TechPoint(EdgeH.fromRight(36), EdgeV.fromTop(19)),
		};
		Technology.TechPoint [] box_25 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(3), EdgeV.fromBottom(10)),
			new Technology.TechPoint(EdgeH.fromRight(39), EdgeV.fromTop(22)),
		};
		Technology.TechPoint [] box_26 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.makeLeftEdge(), EdgeV.fromBottom(19)),
			new Technology.TechPoint(EdgeH.fromRight(36), EdgeV.fromTop(7)),
		};
		Technology.TechPoint [] box_27 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(25), EdgeV.fromBottom(23)),
			new Technology.TechPoint(EdgeH.fromRight(19), EdgeV.fromTop(11)),
		};
		Technology.TechPoint [] box_28 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(12), EdgeV.fromBottom(21)),
			new Technology.TechPoint(EdgeH.fromRight(28), EdgeV.fromTop(9)),
		};
		Technology.TechPoint [] box_29 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(14), EdgeV.fromBottom(23)),
			new Technology.TechPoint(EdgeH.fromRight(30), EdgeV.fromTop(11)),
		};
		Technology.TechPoint [] box_30 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(13), EdgeV.fromBottom(22)),
			new Technology.TechPoint(EdgeH.fromRight(29), EdgeV.fromTop(10)),
		};
		Technology.TechPoint [] box_31 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(4), EdgeV.fromBottom(23)),
			new Technology.TechPoint(EdgeH.fromRight(40), EdgeV.fromTop(11)),
		};
		Technology.TechPoint [] box_32 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(13), EdgeV.fromBottom(10)),
			new Technology.TechPoint(EdgeH.fromRight(29), EdgeV.fromTop(22)),
		};
		Technology.TechPoint [] box_33 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(4), EdgeV.fromBottom(11)),
			new Technology.TechPoint(EdgeH.fromRight(40), EdgeV.fromTop(23)),
		};
		Technology.TechPoint [] box_34 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(35), EdgeV.fromBottom(22)),
			new Technology.TechPoint(EdgeH.fromRight(7), EdgeV.fromTop(10)),
		};
		Technology.TechPoint [] box_35 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(34), EdgeV.fromBottom(21)),
			new Technology.TechPoint(EdgeH.fromRight(6), EdgeV.fromTop(9)),
		};
		Technology.TechPoint [] box_36 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(22), EdgeV.fromBottom(20)),
			new Technology.TechPoint(EdgeH.fromRight(16), EdgeV.fromTop(8)),
		};
		Technology.TechPoint [] box_37 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(22), EdgeV.fromBottom(8)),
			new Technology.TechPoint(EdgeH.fromRight(16), EdgeV.fromTop(20)),
		};
		Technology.TechPoint [] box_38 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(14), EdgeV.fromBottom(11)),
			new Technology.TechPoint(EdgeH.fromRight(30), EdgeV.fromTop(23)),
		};
		Technology.TechPoint [] box_39 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(12), EdgeV.fromBottom(9)),
			new Technology.TechPoint(EdgeH.fromRight(28), EdgeV.fromTop(21)),
		};
		Technology.TechPoint [] box_40 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.makeLeftEdge(), EdgeV.fromBottom(7)),
			new Technology.TechPoint(EdgeH.fromRight(36), EdgeV.fromTop(7)),
		};
		Technology.TechPoint [] box_41 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(2), EdgeV.fromBottom(9)),
			new Technology.TechPoint(EdgeH.fromRight(38), EdgeV.fromTop(9)),
		};
		Technology.TechPoint [] box_42 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(3), EdgeV.fromBottom(10)),
			new Technology.TechPoint(EdgeH.fromRight(39), EdgeV.fromTop(10)),
		};
		Technology.TechPoint [] box_43 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(4), EdgeV.fromBottom(11)),
			new Technology.TechPoint(EdgeH.fromRight(40), EdgeV.fromTop(11)),
		};
		Technology.TechPoint [] box_44 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(2), EdgeV.makeBottomEdge()),
			new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.makeTopEdge()),
		};
		Technology.TechPoint [] box_45 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(12), EdgeV.fromBottom(9)),
			new Technology.TechPoint(EdgeH.fromRight(28), EdgeV.fromTop(9)),
		};
		Technology.TechPoint [] box_46 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(24), EdgeV.fromBottom(10)),
			new Technology.TechPoint(EdgeH.fromRight(18), EdgeV.fromTop(10)),
		};
		Technology.TechPoint [] box_47 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(22), EdgeV.fromBottom(8)),
			new Technology.TechPoint(EdgeH.fromRight(16), EdgeV.fromTop(8)),
		};
		Technology.TechPoint [] box_48 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(13), EdgeV.fromBottom(10)),
			new Technology.TechPoint(EdgeH.fromRight(29), EdgeV.fromTop(10)),
		};
		Technology.TechPoint [] box_49 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(14), EdgeV.fromBottom(11)),
			new Technology.TechPoint(EdgeH.fromRight(30), EdgeV.fromTop(11)),
		};
		Technology.TechPoint [] box_50 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(12), EdgeV.fromBottom(6)),
			new Technology.TechPoint(EdgeH.fromRight(6), EdgeV.fromTop(6)),
		};
		Technology.TechPoint [] box_51 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(25), EdgeV.fromBottom(11)),
			new Technology.TechPoint(EdgeH.fromRight(19), EdgeV.fromTop(11)),
		};
		Technology.TechPoint [] box_52 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(34), EdgeV.fromBottom(9)),
			new Technology.TechPoint(EdgeH.fromRight(6), EdgeV.fromTop(9)),
		};
		Technology.TechPoint [] box_53 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(35), EdgeV.fromBottom(10)),
			new Technology.TechPoint(EdgeH.fromRight(7), EdgeV.fromTop(10)),
		};
		Technology.TechPoint [] box_54 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(36), EdgeV.fromBottom(11)),
			new Technology.TechPoint(EdgeH.fromRight(8), EdgeV.fromTop(11)),
		};
		Technology.TechPoint [] box_55 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(3), EdgeV.fromBottom(3)),
			new Technology.TechPoint(EdgeH.fromRight(3), EdgeV.fromTop(3)),
		};
		Technology.TechPoint [] box_56 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(2), EdgeV.fromBottom(2)),
			new Technology.TechPoint(EdgeH.fromRight(2), EdgeV.fromTop(2)),
		};
		Technology.TechPoint [] box_57 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(1), EdgeV.fromBottom(1)),
			new Technology.TechPoint(EdgeH.fromRight(1), EdgeV.fromTop(1)),
		};
		Technology.TechPoint [] box_58 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge()),
			new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.makeTopEdge()),
		};

		//******************** NODES ********************

		/** Active_Pin */
		PrimitiveNode ap_node = PrimitiveNode.newInstance("Active_Pin", this, 2, 2, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(PA_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, box_58)
			});
		ap_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, ap_node, new ArcProto [] {Active_arc, Pdiff_arc, Ndiff_arc}, "active", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(1), EdgeV.fromBottom(1), EdgeH.fromRight(1), EdgeV.fromTop(1))
			});
		ap_node.setFunction(PrimitiveNode.Function.PIN);
		ap_node.setArcsWipe();
		ap_node.setArcsShrink();

		/** M1_Pin */
		PrimitiveNode mp_node = PrimitiveNode.newInstance("M1_Pin", this, 4, 4, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(PM_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, box_58)
			});
		mp_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, mp_node, new ArcProto [] {Metal_1_arc}, "metal-1", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
			});
		mp_node.setFunction(PrimitiveNode.Function.PIN);
		mp_node.setArcsWipe();
		mp_node.setArcsShrink();

		/** M2_Pin */
		PrimitiveNode mp0_node = PrimitiveNode.newInstance("M2_Pin", this, 4, 4, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(PM0_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, box_58)
			});
		mp0_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, mp0_node, new ArcProto [] {Metal_2_arc}, "metal-2", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
			});
		mp0_node.setFunction(PrimitiveNode.Function.PIN);
		mp0_node.setArcsWipe();
		mp0_node.setArcsShrink();

		/** Poly1_Pin */
		PrimitiveNode pp_node = PrimitiveNode.newInstance("Poly1_Pin", this, 2, 2, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(PP_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, box_58)
			});
		pp_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, pp_node, new ArcProto [] {Polysilicon_arc}, "polysilicon", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(1), EdgeV.fromBottom(1), EdgeH.fromRight(1), EdgeV.fromTop(1))
			});
		pp_node.setFunction(PrimitiveNode.Function.PIN);
		pp_node.setArcsWipe();
		pp_node.setArcsShrink();

		/** Poly2_Pin */
		PrimitiveNode pp0_node = PrimitiveNode.newInstance("Poly2_Pin", this, 2, 2, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(PP0_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, box_58)
			});
		pp0_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, pp0_node, new ArcProto [] {Polysilicon_2_arc}, "p2-pin", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(1), EdgeV.fromBottom(1), EdgeH.fromRight(1), EdgeV.fromTop(1))
			});
		pp0_node.setFunction(PrimitiveNode.Function.PIN);
		pp0_node.setArcsWipe();
		pp0_node.setArcsShrink();

		/** Ndiff_Pin */
		PrimitiveNode np_node = PrimitiveNode.newInstance("Ndiff_Pin", this, 8, 8, new SizeOffset(2, 2, 2, 2),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(PA_lay, 0, Poly.Type.CLOSED, Technology.NodeLayer.BOX, box_56),
				new Technology.NodeLayer(PNS_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, box_58)
			});
		np_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, np_node, new ArcProto [] {}, "Ndiff_Pin", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(3), EdgeV.fromBottom(3), EdgeH.fromRight(3), EdgeV.fromTop(3))
			});
		np_node.setFunction(PrimitiveNode.Function.PIN);
		np_node.setArcsWipe();
		np_node.setArcsShrink();

		/** Pdiff_Pin */
		PrimitiveNode pp1_node = PrimitiveNode.newInstance("Pdiff_Pin", this, 8, 8, new SizeOffset(2, 2, 2, 2),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(PA_lay, 0, Poly.Type.CLOSED, Technology.NodeLayer.BOX, box_56),
				new Technology.NodeLayer(PPS_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, box_58)
			});
		pp1_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, pp1_node, new ArcProto [] {}, "Pdiff_Pin", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(3), EdgeV.fromBottom(3), EdgeH.fromRight(3), EdgeV.fromTop(3))
			});
		pp1_node.setFunction(PrimitiveNode.Function.PIN);
		pp1_node.setArcsWipe();
		pp1_node.setArcsShrink();

		/** NPN1_transistor */
		PrimitiveNode nt_node = PrimitiveNode.newInstance("NPN1_transistor", this, 46, 24, new SizeOffset(22, 16, 8, 8),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(M_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_53),
				new Technology.NodeLayer(A_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_52),
				new Technology.NodeLayer(PBA_lay, -1, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_50),
				new Technology.NodeLayer(M_lay, 1, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_48),
				new Technology.NodeLayer(A_lay, 2, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_47),
				new Technology.NodeLayer(M_lay, 2, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_46),
				new Technology.NodeLayer(A_lay, 1, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_45),
				new Technology.NodeLayer(NW_lay, -1, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_44),
				new Technology.NodeLayer(M_lay, 3, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_42),
				new Technology.NodeLayer(A_lay, 3, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_41),
				new Technology.NodeLayer(AC_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_54),
				new Technology.NodeLayer(AC_lay, 2, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_51),
				new Technology.NodeLayer(AC_lay, 1, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_49),
				new Technology.NodeLayer(NS_lay, 2, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_47),
				new Technology.NodeLayer(PS_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_52),
				new Technology.NodeLayer(PS_lay, 1, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_45),
				new Technology.NodeLayer(AC_lay, 3, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_43),
				new Technology.NodeLayer(NS_lay, 3, Poly.Type.CLOSED, Technology.NodeLayer.BOX, box_40)
			});
		nt_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, nt_node, new ArcProto [] {Metal_1_arc}, "B2", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(36), EdgeV.fromBottom(11), EdgeH.fromRight(8), EdgeV.fromTop(11)),
				PrimitivePort.newInstance(this, nt_node, new ArcProto [] {Metal_1_arc}, "B1", 0,180, 1, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(14), EdgeV.fromBottom(11), EdgeH.fromRight(30), EdgeV.fromTop(11)),
				PrimitivePort.newInstance(this, nt_node, new ArcProto [] {Metal_1_arc}, "E1", 0,180, 2, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(25), EdgeV.fromBottom(11), EdgeH.fromRight(19), EdgeV.fromTop(11)),
				PrimitivePort.newInstance(this, nt_node, new ArcProto [] {Metal_1_arc}, "C1", 0,180, 3, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(4), EdgeV.fromBottom(11), EdgeH.fromRight(40), EdgeV.fromTop(11))
			});
		nt_node.setFunction(PrimitiveNode.Function.TRANPN);

		/** NPN2_Transistor */
		PrimitiveNode nt0_node = PrimitiveNode.newInstance("NPN2_Transistor", this, 46, 36, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(A_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_39),
				new Technology.NodeLayer(A_lay, 5, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_37),
				new Technology.NodeLayer(A_lay, 1, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_36),
				new Technology.NodeLayer(M_lay, 6, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_34),
				new Technology.NodeLayer(M_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_32),
				new Technology.NodeLayer(M_lay, 2, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_30),
				new Technology.NodeLayer(A_lay, 2, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_28),
				new Technology.NodeLayer(A_lay, 6, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_35),
				new Technology.NodeLayer(M_lay, 3, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_25),
				new Technology.NodeLayer(M_lay, 5, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_23),
				new Technology.NodeLayer(A_lay, 3, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_22),
				new Technology.NodeLayer(A_lay, 4, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_21),
				new Technology.NodeLayer(M_lay, 4, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_20),
				new Technology.NodeLayer(M_lay, 1, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_18),
				new Technology.NodeLayer(M_lay, 7, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_16),
				new Technology.NodeLayer(A_lay, 7, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_15),
				new Technology.NodeLayer(NW_lay, -1, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_44),
				new Technology.NodeLayer(PBA_lay, -1, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_50),
				new Technology.NodeLayer(AC_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_38),
				new Technology.NodeLayer(PS_lay, 6, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_35),
				new Technology.NodeLayer(AC_lay, 3, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_33),
				new Technology.NodeLayer(PS_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_39),
				new Technology.NodeLayer(AC_lay, 4, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_31),
				new Technology.NodeLayer(AC_lay, 2, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_29),
				new Technology.NodeLayer(NS_lay, 4, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_26),
				new Technology.NodeLayer(NS_lay, 3, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_24),
				new Technology.NodeLayer(AC_lay, 6, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_19),
				new Technology.NodeLayer(NS_lay, 5, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_37),
				new Technology.NodeLayer(NS_lay, 1, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_36),
				new Technology.NodeLayer(AC_lay, 1, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_27),
				new Technology.NodeLayer(AC_lay, 5, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_17),
				new Technology.NodeLayer(PS_lay, 2, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_28),
				new Technology.NodeLayer(PS_lay, 7, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_15),
				new Technology.NodeLayer(AC_lay, 7, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_14)
			});
		nt0_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, nt0_node, new ArcProto [] {Metal_1_arc}, "B1", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(14), EdgeV.fromBottom(11), EdgeH.fromRight(30), EdgeV.fromTop(23)),
				PrimitivePort.newInstance(this, nt0_node, new ArcProto [] {Metal_1_arc}, "E2", 0,180, 1, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(25), EdgeV.fromBottom(23), EdgeH.fromRight(19), EdgeV.fromTop(11)),
				PrimitivePort.newInstance(this, nt0_node, new ArcProto [] {Metal_1_arc}, "B3", 0,180, 2, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(14), EdgeV.fromBottom(23), EdgeH.fromRight(30), EdgeV.fromTop(11)),
				PrimitivePort.newInstance(this, nt0_node, new ArcProto [] {Metal_1_arc}, "C1", 0,180, 3, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(4), EdgeV.fromBottom(11), EdgeH.fromRight(40), EdgeV.fromTop(23)),
				PrimitivePort.newInstance(this, nt0_node, new ArcProto [] {Metal_1_arc}, "C2", 0,180, 4, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(4), EdgeV.fromBottom(23), EdgeH.fromRight(40), EdgeV.fromTop(11)),
				PrimitivePort.newInstance(this, nt0_node, new ArcProto [] {Metal_1_arc}, "E1", 0,180, 5, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(25), EdgeV.fromBottom(11), EdgeH.fromRight(19), EdgeV.fromTop(23)),
				PrimitivePort.newInstance(this, nt0_node, new ArcProto [] {Metal_1_arc}, "B4", 0,180, 6, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(36), EdgeV.fromBottom(23), EdgeH.fromRight(8), EdgeV.fromTop(11)),
				PrimitivePort.newInstance(this, nt0_node, new ArcProto [] {Metal_1_arc}, "B2", 0,180, 7, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(36), EdgeV.fromBottom(11), EdgeH.fromRight(8), EdgeV.fromTop(23))
			});
		nt0_node.setFunction(PrimitiveNode.Function.TRANPN);

		/** M1_Pdiff_Con */
		PrimitiveNode mpc_node = PrimitiveNode.newInstance("M1_Pdiff_Con", this, 16, 16, new SizeOffset(5, 5, 5, 5),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(A_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_12),
				new Technology.NodeLayer(M_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.MINBOX, box_11),
				new Technology.NodeLayer(NW_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_58),
				new Technology.NodeLayer(PS_lay, 0, Poly.Type.CLOSED, Technology.NodeLayer.BOX, box_55),
				new Technology.NodeLayer(AC_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_13)
			});
		mpc_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, mpc_node, new ArcProto [] {Metal_1_arc, Pdiff_arc}, "m1_pdiff", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(6), EdgeV.fromBottom(6), EdgeH.fromRight(6), EdgeV.fromTop(6))
			});
		mpc_node.setFunction(PrimitiveNode.Function.CONTACT);
		mpc_node.setSpecialType(PrimitiveNode.MULTICUT);
		mpc_node.setSpecialValues(new double [] {2, 2, 2, 2, 2, 2});

		/** M1_Ndiff_Con */
		PrimitiveNode mnc_node = PrimitiveNode.newInstance("M1_Ndiff_Con", this, 10, 10, new SizeOffset(2, 2, 2, 2),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(M_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.MINBOX, box_10),
				new Technology.NodeLayer(A_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_56),
				new Technology.NodeLayer(NS_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_58),
				new Technology.NodeLayer(AC_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_13)
			});
		mnc_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, mnc_node, new ArcProto [] {Metal_1_arc, Ndiff_arc}, "M1_Ndiff", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(3), EdgeV.fromBottom(3), EdgeH.fromRight(3), EdgeV.fromTop(3))
			});
		mnc_node.setFunction(PrimitiveNode.Function.CONTACT);
		mnc_node.setSpecialType(PrimitiveNode.MULTICUT);
		mnc_node.setSpecialValues(new double [] {2, 2, 2, 2, 2, 2});

		/** M1_Poly1_Con */
		PrimitiveNode mpc0_node = PrimitiveNode.newInstance("M1_Poly1_Con", this, 6, 6, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(M_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.MINBOX, box_9),
				new Technology.NodeLayer(P0_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_58),
				new Technology.NodeLayer(PC_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_13)
			});
		mpc0_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, mpc0_node, new ArcProto [] {Polysilicon_arc, Metal_1_arc}, "metal-1-polysilicon", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(1), EdgeV.fromBottom(1), EdgeH.fromRight(1), EdgeV.fromTop(1))
			});
		mpc0_node.setFunction(PrimitiveNode.Function.CONTACT);
		mpc0_node.setSpecialType(PrimitiveNode.MULTICUT);
		mpc0_node.setSpecialValues(new double [] {2, 2, 2, 2, 2, 2});

		/** M1_Poly2_Con */
		PrimitiveNode mpc1_node = PrimitiveNode.newInstance("M1_Poly2_Con", this, 6, 6, new SizeOffset(1, 1, 1, 1),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(M_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_57),
				new Technology.NodeLayer(P1_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_58),
				new Technology.NodeLayer(PC0_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_13)
			});
		mpc1_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, mpc1_node, new ArcProto [] {Metal_1_arc, Polysilicon_2_arc}, "M1P2", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(1), EdgeV.fromBottom(1), EdgeH.fromRight(1), EdgeV.fromTop(1))
			});
		mpc1_node.setFunction(PrimitiveNode.Function.CONTACT);
		mpc1_node.setSpecialType(PrimitiveNode.MULTICUT);
		mpc1_node.setSpecialValues(new double [] {2, 2, 1, 1, 2, 2});

		/** PMOSFET */
		PrimitiveNode p_node = PrimitiveNode.newInstance("PMOSFET", this, 12, 16, new SizeOffset(5, 5, 7, 7),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(P0_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_8, 1, 1, 2, 2),
				new Technology.NodeLayer(NW_lay, -1, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_58, 8, 8, 5, 5),
				new Technology.NodeLayer(A_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_12, 3, 3, 0, 0),
				new Technology.NodeLayer(PS_lay, -1, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_55, 5, 5, 2, 2)
			});
		p_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, p_node, new ArcProto [] {Polysilicon_arc}, "pmos_poly_lt", 180,90, 1, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(3), EdgeV.fromCenter(-1), EdgeH.fromLeft(4), EdgeV.fromCenter(1)),
				PrimitivePort.newInstance(this, p_node, new ArcProto [] {Pdiff_arc}, "pmos_diff_top", 90,90, 3, PortCharacteristic.UNKNOWN,
					EdgeH.fromCenter(-1), EdgeV.fromTop(6), EdgeH.fromCenter(1), EdgeV.fromTop(5)),
				PrimitivePort.newInstance(this, p_node, new ArcProto [] {Polysilicon_arc}, "pmos_poly_rt", 0,90, 1, PortCharacteristic.UNKNOWN,
					EdgeH.fromRight(4), EdgeV.fromCenter(-1), EdgeH.fromRight(3), EdgeV.fromCenter(1)),
				PrimitivePort.newInstance(this, p_node, new ArcProto [] {Pdiff_arc}, "pmos_diff_bot", 270,90, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromCenter(-1), EdgeV.fromBottom(5), EdgeH.fromCenter(1), EdgeV.fromBottom(6))
			});
		p_node.setFunction(PrimitiveNode.Function.TRAPMOS);
		p_node.setHoldsOutline();
		p_node.setCanShrink();
		p_node.setSpecialType(PrimitiveNode.SERPTRANS);
		p_node.setSpecialValues(new double [] {0.0416667, 0, 1, 2, 0, 1});

		/** M1_M2_Con */
		PrimitiveNode mmc_node = PrimitiveNode.newInstance("M1_M2_Con", this, 4, 4, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(M_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_58),
				new Technology.NodeLayer(M0_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_58),
				new Technology.NodeLayer(V_lay, 0, Poly.Type.CLOSED, Technology.NodeLayer.BOX, box_5)
			});
		mmc_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, mmc_node, new ArcProto [] {Metal_1_arc, Metal_2_arc}, "metal-1-metal-2", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
			});
		mmc_node.setFunction(PrimitiveNode.Function.CONTACT);

		/** M1_N_Well_Con */
		PrimitiveNode mnwc_node = PrimitiveNode.newInstance("M1_N_Well_Con", this, 12, 12, new SizeOffset(3, 3, 3, 3),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(NW_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_58),
				new Technology.NodeLayer(OW_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_55),
				new Technology.NodeLayer(M_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.MINBOX, box_4),
				new Technology.NodeLayer(NS_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_57),
				new Technology.NodeLayer(AC_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_13)
			});
		mnwc_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, mnwc_node, new ArcProto [] {Metal_1_arc, Active_arc}, "metal-1-well", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(4), EdgeV.fromBottom(4), EdgeH.fromRight(4), EdgeV.fromTop(4))
			});
		mnwc_node.setFunction(PrimitiveNode.Function.WELL);
		mnwc_node.setSpecialType(PrimitiveNode.MULTICUT);
		mnwc_node.setSpecialValues(new double [] {2, 2, 2, 2, 2, 2});

		/** Poly1_Poly2_Cap */
		PrimitiveNode ppc_node = PrimitiveNode.newInstance("Poly1_Poly2_Cap", this, 12, 12, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(P0_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_58),
				new Technology.NodeLayer(P1_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_56)
			});
		ppc_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, ppc_node, new ArcProto [] {Polysilicon_arc, Polysilicon_2_arc}, "P1P2", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(3), EdgeV.fromBottom(3), EdgeH.fromRight(3), EdgeV.fromTop(3))
			});
		ppc_node.setFunction(PrimitiveNode.Function.CAPAC);

		/** NMOSFET */
		PrimitiveNode n_node = PrimitiveNode.newInstance("NMOSFET", this, 6, 10, new SizeOffset(2, 2, 4, 4),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(P0_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_3, 1, 1, 2, 2),
				new Technology.NodeLayer(A_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_56, 3, 3, 0, 0),
				new Technology.NodeLayer(NS_lay, -1, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_58, 5, 5, 2, 2)
			});
		n_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, n_node, new ArcProto [] {Polysilicon_arc}, "nmos_poly_lt", 180,90, 1, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.fromCenter(-1), EdgeH.fromLeft(1), EdgeV.fromCenter(1)),
				PrimitivePort.newInstance(this, n_node, new ArcProto [] {Ndiff_arc}, "nmos_diff_top", 90,90, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromCenter(-1), EdgeV.fromTop(3), EdgeH.fromCenter(1), EdgeV.fromTop(2)),
				PrimitivePort.newInstance(this, n_node, new ArcProto [] {Polysilicon_arc}, "nmos_poly_rt", 0,90, 1, PortCharacteristic.UNKNOWN,
					EdgeH.fromRight(1), EdgeV.fromCenter(-1), EdgeH.makeRightEdge(), EdgeV.fromCenter(1)),
				PrimitivePort.newInstance(this, n_node, new ArcProto [] {Ndiff_arc}, "nmos_diff_bot", 270,90, 3, PortCharacteristic.UNKNOWN,
					EdgeH.fromCenter(-1), EdgeV.fromBottom(2), EdgeH.fromCenter(1), EdgeV.fromBottom(3))
			});
		n_node.setFunction(PrimitiveNode.Function.TRANMOS);
		n_node.setHoldsOutline();
		n_node.setCanShrink();
		n_node.setSpecialType(PrimitiveNode.SERPTRANS);
		n_node.setSpecialValues(new double [] {0.0333333, 0, 1, 2, 0, 1});

		/** M1_Substrate_Con */
		PrimitiveNode msc_node = PrimitiveNode.newInstance("M1_Substrate_Con", this, 10, 10, new SizeOffset(3, 3, 3, 3),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(M_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_55),
				new Technology.NodeLayer(OS_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_56),
				new Technology.NodeLayer(PS_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_58),
				new Technology.NodeLayer(AC_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_13)
			});
		msc_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, msc_node, new ArcProto [] {Metal_1_arc}, "M1_Substrate", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(4), EdgeV.fromBottom(4), EdgeH.fromRight(4), EdgeV.fromTop(4))
			});
		msc_node.setFunction(PrimitiveNode.Function.UNKNOWN);
		msc_node.setSpecialType(PrimitiveNode.MULTICUT);
		msc_node.setSpecialValues(new double [] {2, 2, 1, 1, 2, 2});

		/** Active_Node */
		PrimitiveNode an_node = PrimitiveNode.newInstance("Active_Node", this, 4, 4, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(A_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_58)
			});
		an_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, an_node, new ArcProto [] {Active_arc, Pdiff_arc, Ndiff_arc}, "active", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		an_node.setFunction(PrimitiveNode.Function.NODE);
		an_node.setHoldsOutline();
		an_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** P_Select_Node */
		PrimitiveNode psn_node = PrimitiveNode.newInstance("P_Select_Node", this, 6, 6, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(PS_lay, 0, Poly.Type.CLOSED, Technology.NodeLayer.BOX, box_58)
			});
		psn_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, psn_node, new ArcProto [] {}, "select", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		psn_node.setFunction(PrimitiveNode.Function.NODE);
		psn_node.setHoldsOutline();
		psn_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Poly_2_Cut_Node */
		PrimitiveNode pcn_node = PrimitiveNode.newInstance("Poly_2_Cut_Node", this, 4, 4, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(PC0_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_58)
			});
		pcn_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, pcn_node, new ArcProto [] {Polysilicon_2_arc}, "Poly_2_Cut", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		pcn_node.setFunction(PrimitiveNode.Function.NODE);
		pcn_node.setHoldsOutline();
		pcn_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Active_Cut_Node */
		PrimitiveNode acn_node = PrimitiveNode.newInstance("Active_Cut_Node", this, 2, 2, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(AC_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_58)
			});
		acn_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, acn_node, new ArcProto [] {}, "activecut", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		acn_node.setFunction(PrimitiveNode.Function.NODE);
		acn_node.setHoldsOutline();
		acn_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Via_Node */
		PrimitiveNode vn_node = PrimitiveNode.newInstance("Via_Node", this, 2, 2, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(V_lay, 0, Poly.Type.CLOSED, Technology.NodeLayer.BOX, box_58)
			});
		vn_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, vn_node, new ArcProto [] {}, "via", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		vn_node.setFunction(PrimitiveNode.Function.NODE);
		vn_node.setHoldsOutline();
		vn_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Passivation_Node */
		PrimitiveNode pn_node = PrimitiveNode.newInstance("Passivation_Node", this, 8, 8, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(P_lay, 0, Poly.Type.CLOSED, Technology.NodeLayer.BOX, box_58)
			});
		pn_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, pn_node, new ArcProto [] {}, "passivation", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		pn_node.setFunction(PrimitiveNode.Function.NODE);
		pn_node.setHoldsOutline();
		pn_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Pad_Frame_Node */
		PrimitiveNode pfn_node = PrimitiveNode.newInstance("Pad_Frame_Node", this, 8, 8, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(PF_lay, 0, Poly.Type.CLOSED, Technology.NodeLayer.BOX, box_58)
			});
		pfn_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, pfn_node, new ArcProto [] {}, "pad-frame", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		pfn_node.setFunction(PrimitiveNode.Function.NODE);
		pfn_node.setHoldsOutline();
		pfn_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** M1_Node */
		PrimitiveNode mn_node = PrimitiveNode.newInstance("M1_Node", this, 4, 4, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(M_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_58)
			});
		mn_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, mn_node, new ArcProto [] {Metal_1_arc}, "metal-1", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		mn_node.setFunction(PrimitiveNode.Function.NODE);
		mn_node.setHoldsOutline();
		mn_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** M2_Node */
		PrimitiveNode mn0_node = PrimitiveNode.newInstance("M2_Node", this, 4, 4, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(M0_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_58)
			});
		mn0_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, mn0_node, new ArcProto [] {Metal_2_arc}, "metal-2", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		mn0_node.setFunction(PrimitiveNode.Function.NODE);
		mn0_node.setHoldsOutline();
		mn0_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Poly1_Node */
		PrimitiveNode pn0_node = PrimitiveNode.newInstance("Poly1_Node", this, 4, 4, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(P0_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_58)
			});
		pn0_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, pn0_node, new ArcProto [] {Polysilicon_arc}, "polysilicon", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		pn0_node.setFunction(PrimitiveNode.Function.NODE);
		pn0_node.setHoldsOutline();
		pn0_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Poly2_Node */
		PrimitiveNode pn1_node = PrimitiveNode.newInstance("Poly2_Node", this, 2, 2, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(P1_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_58)
			});
		pn1_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, pn1_node, new ArcProto [] {Polysilicon_2_arc}, "P2", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		pn1_node.setFunction(PrimitiveNode.Function.NODE);
		pn1_node.setHoldsOutline();
		pn1_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Ndiff_Node */
		PrimitiveNode nn_node = PrimitiveNode.newInstance("Ndiff_Node", this, 8, 8, new SizeOffset(2, 2, 2, 2),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(A_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_56),
				new Technology.NodeLayer(NS_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_58)
			});
		nn_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, nn_node, new ArcProto [] {Active_arc, Pdiff_arc, Ndiff_arc}, "active", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		nn_node.setFunction(PrimitiveNode.Function.NODE);
		nn_node.setHoldsOutline();
		nn_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Poly1_Cut_Node */
		PrimitiveNode pcn0_node = PrimitiveNode.newInstance("Poly1_Cut_Node", this, 2, 2, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(PC_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_58)
			});
		pcn0_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, pcn0_node, new ArcProto [] {}, "polycut", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		pcn0_node.setFunction(PrimitiveNode.Function.NODE);
		pcn0_node.setHoldsOutline();
		pcn0_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** N_Well_Node */
		PrimitiveNode nwn_node = PrimitiveNode.newInstance("N_Well_Node", this, 6, 6, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(NW_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_58)
			});
		nwn_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, nwn_node, new ArcProto [] {Pdiff_arc}, "well", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		nwn_node.setFunction(PrimitiveNode.Function.NODE);
		nwn_node.setHoldsOutline();
		nwn_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** N_Select_Node */
		PrimitiveNode nsn_node = PrimitiveNode.newInstance("N_Select_Node", this, 4, 4, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(NS_lay, 0, Poly.Type.CLOSED, Technology.NodeLayer.BOX, box_58)
			});
		nsn_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, nsn_node, new ArcProto [] {}, "N_Select", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		nsn_node.setFunction(PrimitiveNode.Function.NODE);
		nsn_node.setHoldsOutline();
		nsn_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** P_Base_Active_Node */
		PrimitiveNode pban_node = PrimitiveNode.newInstance("P_Base_Active_Node", this, 4, 4, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(PBA_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_58)
			});
		pban_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, pban_node, new ArcProto [] {}, "P_Base", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		pban_node.setFunction(PrimitiveNode.Function.NODE);
		pban_node.setHoldsOutline();
		pban_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** BCCD_Node */
		PrimitiveNode bn_node = PrimitiveNode.newInstance("BCCD_Node", this, 4, 4, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(B_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_58)
			});
		bn_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, bn_node, new ArcProto [] {}, "BCCD", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		bn_node.setFunction(PrimitiveNode.Function.NODE);
		bn_node.setHoldsOutline();
		bn_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Pdiff_Node */
		PrimitiveNode pn2_node = PrimitiveNode.newInstance("Pdiff_Node", this, 8, 8, new SizeOffset(2, 2, 2, 2),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(A_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_56),
				new Technology.NodeLayer(PS_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_58)
			});
		pn2_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, pn2_node, new ArcProto [] {}, "Pdiff", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		pn2_node.setFunction(PrimitiveNode.Function.NODE);
		pn2_node.setHoldsOutline();
		pn2_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Ohmic_Well */
		PrimitiveNode ow_node = PrimitiveNode.newInstance("Ohmic_Well", this, 4, 4, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(OW_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_58)
			});
		ow_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, ow_node, new ArcProto [] {}, "Ohmic_Well", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		ow_node.setFunction(PrimitiveNode.Function.NODE);
		ow_node.setHoldsOutline();
		ow_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Ohmic_Substrate */
		PrimitiveNode os_node = PrimitiveNode.newInstance("Ohmic_Substrate", this, 4, 4, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(OS_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_58)
			});
		os_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, os_node, new ArcProto [] {}, "Ohmic_Substrate", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		os_node.setFunction(PrimitiveNode.Function.NODE);
		os_node.setHoldsOutline();
		os_node.setSpecialType(PrimitiveNode.POLYGONAL);

		// The pure layer nodes
		PS_lay.setPureLayerNode(psn_node);		// P_Select
		NS_lay.setPureLayerNode(nsn_node);		// N_Select
		NW_lay.setPureLayerNode(nwn_node);		// N_Well
		V_lay.setPureLayerNode(vn_node);		// Via
		P_lay.setPureLayerNode(pn_node);		// Passivation
		PF_lay.setPureLayerNode(pfn_node);		// Pad_Frame
		AC_lay.setPureLayerNode(acn_node);		// Active_Cut
		M_lay.setPureLayerNode(mn_node);		// M1
		M0_lay.setPureLayerNode(mn0_node);		// M2
		P0_lay.setPureLayerNode(pn0_node);		// Poly1
		P1_lay.setPureLayerNode(pn1_node);		// Poly2
		A_lay.setPureLayerNode(an_node);		// Active
		PC_lay.setPureLayerNode(pcn0_node);		// Poly1_Cut
		PC0_lay.setPureLayerNode(pcn_node);		// Poly2_Cut
		PBA_lay.setPureLayerNode(pban_node);		// P_Base_Active
		B_lay.setPureLayerNode(bn_node);		// BCCD
		OS_lay.setPureLayerNode(os_node);		// Ohmic_Substrate
		OW_lay.setPureLayerNode(ow_node);		// Ohmic_Well

        // Information for palette
        int maxY = 2 /*metal arcs*/ + 3 /* active arcs */ + 1 /* text */ + 2 /* poly*/ + 2 /* trans */;
        nodeGroups = new Object[maxY][3];
        int count = -1;

        nodeGroups[++count][0] = Polysilicon_arc; nodeGroups[count][1] = pp_node; nodeGroups[count][2] = mpc0_node;
        nodeGroups[++count][0] = Polysilicon_2_arc; nodeGroups[count][1] = pp0_node; nodeGroups[count][2] = mpc1_node;
        nodeGroups[++count][0] = Metal_1_arc; nodeGroups[count][1] = mp_node;
        nodeGroups[++count][0] = Metal_2_arc; nodeGroups[count][1] = mp0_node; nodeGroups[count][2] = mmc_node;
        nodeGroups[++count][0] = Pdiff_arc; nodeGroups[count][1] = pp1_node; nodeGroups[count][2] = mpc_node;
        nodeGroups[++count][0] = Ndiff_arc; nodeGroups[count][1] = np_node; nodeGroups[count][2] = mnc_node;
        nodeGroups[++count][0] = Active_arc; nodeGroups[count][1] = ap_node; nodeGroups[count][2] = mnwc_node;
        nodeGroups[++count][0] = msc_node; nodeGroups[count][1] = nt0_node; nodeGroups[count][2] = nt_node;
        nodeGroups[++count][0] = ppc_node; nodeGroups[count][1] = n_node; nodeGroups[count][2] = p_node;
        nodeGroups[++count][0] = "Pure"; nodeGroups[count][1] = "Misc."; nodeGroups[count][2] = "Cell";
	}

    public DRCRules getFactoryDesignRules(boolean resizeNodes)
	{
		return MOSRules.makeSimpleRules(this, conDist, unConDist);
	}
}
