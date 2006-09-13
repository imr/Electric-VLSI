/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: MoCMOSOld.java
 * mocmosold technology description
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
 * This is the Complementary MOS (old, from MOSIS, P-Well, double metal) Technology.
 */
public class MoCMOSOld extends Technology
{
	/** the Complementary MOS (old, from MOSIS, P-Well, double metal) Technology object. */	public static final MoCMOSOld tech = new MoCMOSOld();

	private static final double X = -1;
	private double [] conDist, unConDist;

	// -------------------- private and protected methods ------------------------
	private MoCMOSOld()
	{
		super("mocmosold");
		setTechShortName("Old MOSIS CMOS");
		setTechDesc("MOSIS CMOS (old rules, P-Well, double metal)");
		setFactoryScale(1000, true);   // in nanometers: really 1 microns
		setNoNegatedArcs();
		setStaticTechnology();

        // Foundry
        Foundry mosis = new Foundry(Foundry.Type.MOSIS);
        foundries.add(mosis);
        
		setFactoryTransparentLayers(new Color []
		{
			new Color( 96,209,255), // Metal-1
			new Color(255,155,192), // Polysilicon
			new Color(107,226, 96), // S-Active
			new Color(224, 95,255), // Metal-2
			new Color(240,221,181)  // P-Well
		});

		//**************************************** LAYERS ****************************************

		/** M layer */
		Layer M_lay = Layer.newInstance(this, "Metal-1",
			new EGraphics(false, false, null, EGraphics.TRANSPARENT_1,  96,209,255,/*107,226,96,*/0.8,true,
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
		Layer M0_lay = Layer.newInstance(this, "Metal-2",
			new EGraphics(false, false, null, EGraphics.TRANSPARENT_4, 224, 95,255,/*0,0,0,*/0.8,true,
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
		Layer P_lay = Layer.newInstance(this, "Polysilicon",
			new EGraphics(false, false, null, EGraphics.TRANSPARENT_2, 255,155,192,/*224,95,255,*/0.8,true,
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
		Layer SA_lay = Layer.newInstance(this, "S-Active",
			new EGraphics(false, false, null, EGraphics.TRANSPARENT_3, 107,226, 96,/*240,221,181,*/0.8,true,
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
		Layer DA_lay = Layer.newInstance(this, "D-Active",
			new EGraphics(false, false, null, EGraphics.TRANSPARENT_3, 107,226, 96,/*240,221,181,*/0.8,true,
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
		Layer PS_lay = Layer.newInstance(this, "P-Select",
			new EGraphics(true, true, null, 0, 89,44,51,0.8,true,
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
		Layer NS_lay = Layer.newInstance(this, "N-Select",
			new EGraphics(true, true, null, 0, 89,44,51,0.8,true,
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
		Layer PW_lay = Layer.newInstance(this, "P-Well",
			new EGraphics(false, false, null, EGraphics.TRANSPARENT_5, 240,221,181,/*0,0,0,*/0.8,true,
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
		Layer NW_lay = Layer.newInstance(this, "N-Well",
			new EGraphics(false, false, null, EGraphics.TRANSPARENT_5, 240,221,181,/*0,0,0,*/0.8,true,
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
		Layer CC_lay = Layer.newInstance(this, "Contact-Cut",
			new EGraphics(false, false, null, 0, 107,137,72,0.8,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** V layer */
		Layer V_lay = Layer.newInstance(this, "Via",
			new EGraphics(false, false, null, 0, 107,137,72,0.8,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** P0 layer */
		Layer P0_lay = Layer.newInstance(this, "Passivation",
			new EGraphics(true, true, null, 0, 0,0,0,0.8,true,
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
		Layer T_lay = Layer.newInstance(this, "Transistor",
			new EGraphics(false, false, null, 0, 200,200,200,0.8,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** PC layer */
		Layer PC_lay = Layer.newInstance(this, "Poly-Cut",
			new EGraphics(false, false, null, 0, 107,137,72,0.8,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** AC layer */
		Layer AC_lay = Layer.newInstance(this, "Active-Cut",
			new EGraphics(false, false, null, 0, 107,137,72,0.8,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** SAW layer */
		Layer SAW_lay = Layer.newInstance(this, "S-Active-Well",
			new EGraphics(false, false, null, EGraphics.TRANSPARENT_3, 107,226, 96,/*240,221,181,*/0.8,true,
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
		Layer PM_lay = Layer.newInstance(this, "Pseudo-Metal-1",
			new EGraphics(false, false, null, EGraphics.TRANSPARENT_1,  96,209,255,/*107,226,96,*/0.8,true,
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
		Layer PM0_lay = Layer.newInstance(this, "Pseudo-Metal-2",
			new EGraphics(false, false, null, EGraphics.TRANSPARENT_4, 224, 95,255,/*0,0,0,*/0.8,true,
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
		Layer PP_lay = Layer.newInstance(this, "Pseudo-Polysilicon",
			new EGraphics(false, false, null, EGraphics.TRANSPARENT_2, 255,155,192,/*224,95,255,*/0.8,true,
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
		Layer PSA_lay = Layer.newInstance(this, "Pseudo-S-Active",
			new EGraphics(false, false, null, EGraphics.TRANSPARENT_3, 107,226, 96,/*240,221,181,*/0.8,true,
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
		Layer PDA_lay = Layer.newInstance(this, "Pseudo-D-Active",
			new EGraphics(false, false, null, EGraphics.TRANSPARENT_3, 107,226, 96,/*240,221,181,*/0.8,true,
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
		Layer PPS_lay = Layer.newInstance(this, "Pseudo-P-Select",
			new EGraphics(true, true, null, 0, 89,44,51,0.8,true,
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
		Layer PNS_lay = Layer.newInstance(this, "Pseudo-N-Select",
			new EGraphics(true, true, null, 0, 89,44,51,0.8,true,
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
		Layer PPW_lay = Layer.newInstance(this, "Pseudo-P-Well",
			new EGraphics(false, false, null, EGraphics.TRANSPARENT_5, 240,221,181,/*0,0,0,*/0.8,true,
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
		Layer PNW_lay = Layer.newInstance(this, "Pseudo-N-Well",
			new EGraphics(false, false, null, EGraphics.TRANSPARENT_5, 240,221,181,/*0,0,0,*/0.8,true,
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
		Layer PF_lay = Layer.newInstance(this, "Pad-Frame",
			new EGraphics(false, false, null, 0, 224,57,192,0.8,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		// The layer functions
		M_lay.setFunction(Layer.Function.METAL1);		// Metal-1
		M0_lay.setFunction(Layer.Function.METAL2);		// Metal-2
		P_lay.setFunction(Layer.Function.POLY1);		// Polysilicon
		SA_lay.setFunction(Layer.Function.DIFFP);		// S-Active
		DA_lay.setFunction(Layer.Function.DIFFN);		// D-Active
		PS_lay.setFunction(Layer.Function.IMPLANTP);		// P-Select
		NS_lay.setFunction(Layer.Function.IMPLANTN);		// N-Select
		PW_lay.setFunction(Layer.Function.WELLP);		// P-Well
		NW_lay.setFunction(Layer.Function.WELLN);		// N-Well
		CC_lay.setFunction(Layer.Function.CONTACT1);		// Contact-Cut
		V_lay.setFunction(Layer.Function.CONTACT2, Layer.Function.CONMETAL);		// Via
		P0_lay.setFunction(Layer.Function.OVERGLASS);		// Passivation
		T_lay.setFunction(Layer.Function.TRANSISTOR, Layer.Function.PSEUDO);		// Transistor
		PC_lay.setFunction(Layer.Function.CONTACT1, Layer.Function.CONPOLY);		// Poly-Cut
		AC_lay.setFunction(Layer.Function.CONTACT1, Layer.Function.CONDIFF);		// Active-Cut
		SAW_lay.setFunction(Layer.Function.DIFFP);		// S-Active-Well
		PM_lay.setFunction(Layer.Function.METAL1, Layer.Function.PSEUDO);		// Pseudo-Metal-1
		PM0_lay.setFunction(Layer.Function.METAL2, Layer.Function.PSEUDO);		// Pseudo-Metal-2
		PP_lay.setFunction(Layer.Function.POLY1, Layer.Function.PSEUDO);		// Pseudo-Polysilicon
		PSA_lay.setFunction(Layer.Function.DIFFP, Layer.Function.PSEUDO);		// Pseudo-S-Active
		PDA_lay.setFunction(Layer.Function.DIFFN, Layer.Function.PSEUDO);		// Pseudo-D-Active
		PPS_lay.setFunction(Layer.Function.IMPLANTP, Layer.Function.PSEUDO);		// Pseudo-P-Select
		PNS_lay.setFunction(Layer.Function.IMPLANTN, Layer.Function.PSEUDO);		// Pseudo-N-Select
		PPW_lay.setFunction(Layer.Function.WELLP, Layer.Function.PSEUDO);		// Pseudo-P-Well
		PNW_lay.setFunction(Layer.Function.WELLN, Layer.Function.PSEUDO);		// Pseudo-N-Well
		PF_lay.setFunction(Layer.Function.ART);		// Pad-Frame

		// The CIF names
		M_lay.setFactoryCIFLayer("CMF");		// Metal-1
		M0_lay.setFactoryCIFLayer("CMS");		// Metal-2
		P_lay.setFactoryCIFLayer("CPG");		// Polysilicon
		SA_lay.setFactoryCIFLayer("CAA");		// S-Active
		DA_lay.setFactoryCIFLayer("CAA");		// D-Active
		PS_lay.setFactoryCIFLayer("CSG");		// P-Select
		NS_lay.setFactoryCIFLayer("CSG");		// N-Select
		PW_lay.setFactoryCIFLayer("CWG");		// P-Well
		NW_lay.setFactoryCIFLayer("CWG");		// N-Well
		CC_lay.setFactoryCIFLayer("CC");		// Contact-Cut
		V_lay.setFactoryCIFLayer("CVA");		// Via
		P0_lay.setFactoryCIFLayer("COG");		// Passivation
		T_lay.setFactoryCIFLayer("");		// Transistor
		PC_lay.setFactoryCIFLayer("CCP");		// Poly-Cut
		AC_lay.setFactoryCIFLayer("CCA");		// Active-Cut
		SAW_lay.setFactoryCIFLayer("CAA");		// S-Active-Well
		PM_lay.setFactoryCIFLayer("");		// Pseudo-Metal-1
		PM0_lay.setFactoryCIFLayer("");		// Pseudo-Metal-2
		PP_lay.setFactoryCIFLayer("");		// Pseudo-Polysilicon
		PSA_lay.setFactoryCIFLayer("");		// Pseudo-S-Active
		PDA_lay.setFactoryCIFLayer("");		// Pseudo-D-Active
		PPS_lay.setFactoryCIFLayer("");		// Pseudo-P-Select
		PNS_lay.setFactoryCIFLayer("");		// Pseudo-N-Select
		PPW_lay.setFactoryCIFLayer("");		// Pseudo-P-Well
		PNW_lay.setFactoryCIFLayer("");		// Pseudo-N-Well
		PF_lay.setFactoryCIFLayer("CX");		// Pad-Frame

		// The DXF names
		M_lay.setFactoryDXFLayer("");		// Metal-1
		M0_lay.setFactoryDXFLayer("");		// Metal-2
		P_lay.setFactoryDXFLayer("");		// Polysilicon
		SA_lay.setFactoryDXFLayer("");		// S-Active
		DA_lay.setFactoryDXFLayer("");		// D-Active
		PS_lay.setFactoryDXFLayer("");		// P-Select
		NS_lay.setFactoryDXFLayer("");		// N-Select
		PW_lay.setFactoryDXFLayer("");		// P-Well
		NW_lay.setFactoryDXFLayer("");		// N-Well
		CC_lay.setFactoryDXFLayer("");		// Contact-Cut
		V_lay.setFactoryDXFLayer("");		// Via
		P0_lay.setFactoryDXFLayer("");		// Passivation
		T_lay.setFactoryDXFLayer("");		// Transistor
		PC_lay.setFactoryDXFLayer("");		// Poly-Cut
		AC_lay.setFactoryDXFLayer("");		// Active-Cut
		SAW_lay.setFactoryDXFLayer("");		// S-Active-Well
		PM_lay.setFactoryDXFLayer("");		// Pseudo-Metal-1
		PM0_lay.setFactoryDXFLayer("");		// Pseudo-Metal-2
		PP_lay.setFactoryDXFLayer("");		// Pseudo-Polysilicon
		PSA_lay.setFactoryDXFLayer("");		// Pseudo-S-Active
		PDA_lay.setFactoryDXFLayer("");		// Pseudo-D-Active
		PPS_lay.setFactoryDXFLayer("");		// Pseudo-P-Select
		PNS_lay.setFactoryDXFLayer("");		// Pseudo-N-Select
		PPW_lay.setFactoryDXFLayer("");		// Pseudo-P-Well
		PNW_lay.setFactoryDXFLayer("");		// Pseudo-N-Well
		PF_lay.setFactoryDXFLayer("");		// Pad-Frame

		// The GDS names
		mosis.setFactoryGDSLayer(M_lay, "10");		// Metal-1
		mosis.setFactoryGDSLayer(M0_lay, "19");		// Metal-2
		mosis.setFactoryGDSLayer(P_lay, "12");		// Polysilicon
		mosis.setFactoryGDSLayer(SA_lay, "2");		// S-Active
		mosis.setFactoryGDSLayer(DA_lay, "2");		// D-Active
		mosis.setFactoryGDSLayer(PS_lay, "8");		// P-Select
		mosis.setFactoryGDSLayer(NS_lay, "7");		// N-Select
		mosis.setFactoryGDSLayer(PW_lay, "1");		// P-Well
		mosis.setFactoryGDSLayer(NW_lay, "1");		// N-Well
		mosis.setFactoryGDSLayer(CC_lay, "9");		// Contact-Cut
		mosis.setFactoryGDSLayer(V_lay, "18");		// Via
		mosis.setFactoryGDSLayer(P0_lay, "11");		// Passivation
		mosis.setFactoryGDSLayer(T_lay, "");		// Transistor
		mosis.setFactoryGDSLayer(PC_lay, "9");		// Poly-Cut
		mosis.setFactoryGDSLayer(AC_lay, "9");		// Active-Cut
		mosis.setFactoryGDSLayer(SAW_lay, "2");		// S-Active-Well
		mosis.setFactoryGDSLayer(PM_lay, "");		// Pseudo-Metal-1
		mosis.setFactoryGDSLayer(PM0_lay, "");		// Pseudo-Metal-2
		mosis.setFactoryGDSLayer(PP_lay, "");		// Pseudo-Polysilicon
		mosis.setFactoryGDSLayer(PSA_lay, "");		// Pseudo-S-Active
		mosis.setFactoryGDSLayer(PDA_lay, "");		// Pseudo-D-Active
		mosis.setFactoryGDSLayer(PPS_lay, "");		// Pseudo-P-Select
		mosis.setFactoryGDSLayer(PNS_lay, "");		// Pseudo-N-Select
		mosis.setFactoryGDSLayer(PPW_lay, "");		// Pseudo-P-Well
		mosis.setFactoryGDSLayer(PNW_lay, "");		// Pseudo-N-Well
		mosis.setFactoryGDSLayer(PF_lay, "");		// Pad-Frame

		// The SPICE information
		M_lay.setFactoryParasitics(0.03f, 0.03f, 0);		// Metal-1
		M0_lay.setFactoryParasitics(0.03f, 0.03f, 0);		// Metal-2
		P_lay.setFactoryParasitics(50.0f, 0.04f, 0);		// Polysilicon
		SA_lay.setFactoryParasitics(10.0f, 0.1f, 0);		// S-Active
		DA_lay.setFactoryParasitics(10.0f, 0.1f, 0);		// D-Active
		PS_lay.setFactoryParasitics(0, 0, 0);		// P-Select
		NS_lay.setFactoryParasitics(0, 0, 0);		// N-Select
		PW_lay.setFactoryParasitics(0, 0, 0);		// P-Well
		NW_lay.setFactoryParasitics(0, 0, 0);		// N-Well
		CC_lay.setFactoryParasitics(0, 0, 0);		// Contact-Cut
		V_lay.setFactoryParasitics(0, 0, 0);		// Via
		P0_lay.setFactoryParasitics(0, 0, 0);		// Passivation
		T_lay.setFactoryParasitics(0, 0, 0);		// Transistor
		PC_lay.setFactoryParasitics(0, 0, 0);		// Poly-Cut
		AC_lay.setFactoryParasitics(0, 0, 0);		// Active-Cut
		SAW_lay.setFactoryParasitics(0, 0, 0);		// S-Active-Well
		PM_lay.setFactoryParasitics(0, 0, 0);		// Pseudo-Metal-1
		PM0_lay.setFactoryParasitics(0, 0, 0);		// Pseudo-Metal-2
		PP_lay.setFactoryParasitics(0, 0, 0);		// Pseudo-Polysilicon
		PSA_lay.setFactoryParasitics(0, 0, 0);		// Pseudo-S-Active
		PDA_lay.setFactoryParasitics(0, 0, 0);		// Pseudo-D-Active
		PPS_lay.setFactoryParasitics(0, 0, 0);		// Pseudo-P-Select
		PNS_lay.setFactoryParasitics(0, 0, 0);		// Pseudo-N-Select
		PPW_lay.setFactoryParasitics(0, 0, 0);		// Pseudo-P-Well
		PNW_lay.setFactoryParasitics(0, 0, 0);		// Pseudo-N-Well
		PF_lay.setFactoryParasitics(0, 0, 0);		// Pad-Frame
		setFactoryParasitics(50, 50);
		String [] headerLevel1 =
		{
			"*CMOS/BULK-NWELL (PRELIMINARY PARAMETERS)",
			".OPTIONS NOMOD DEFL=3UM DEFW=3UM DEFAD=70P DEFAS=70P LIMPTS=1000",
			"+ITL4=1000 ITL5=0 RELTOL=0.01 ABSTOL=500PA VNTOL=500UV LVLTIM=2",
			"+LVLCOD=1",
			".MODEL N NMOS LEVEL=1",
			"+KP=60E-6 VTO=0.7 GAMMA=0.3 LAMBDA=0.05 PHI=0.6",
			"+LD=0.4E-6 TOX=40E-9 CGSO=2.0E-10 CGDO=2.0E-10 CJ=.2MF/M^2",
			".MODEL P PMOS LEVEL=1",
			"+KP=20E-6 VTO=0.7 GAMMA=0.4 LAMBDA=0.05 PHI=0.6",
			"+LD=0.6E-6 TOX=40E-9 CGSO=3.0E-10 CGDO=3.0E-10 CJ=.2MF/M^2",
			".MODEL DIFFCAP D CJO=.2MF/M^2"
		};
		setSpiceHeaderLevel1(headerLevel1);
		String [] headerLevel2 =
		{
			"* MOSIS 3u CMOS PARAMS",
			".OPTIONS NOMOD DEFL=2UM DEFW=6UM DEFAD=100P DEFAS=100P",
			"+LIMPTS=1000 ITL4=1000 ITL5=0 ABSTOL=500PA VNTOL=500UV",
			"* Note that ITL5=0 sets ITL5 to infinity",
			".MODEL N NMOS LEVEL=2 LD=0.3943U TOX=502E-10",
			"+NSUB=1.22416E+16 VTO=0.756 KP=4.224E-05 GAMMA=0.9241",
			"+PHI=0.6 UO=623.661 UEXP=8.328627E-02 UCRIT=54015.0",
			"+DELTA=5.218409E-03 VMAX=50072.2 XJ=0.4U LAMBDA=2.975321E-02",
			"+NFS=4.909947E+12 NEFF=1.001E-02 NSS=0.0 TPG=1.0",
			"+RSH=20.37 CGDO=3.1E-10 CGSO=3.1E-10",
			"+CJ=3.205E-04 MJ=0.4579 CJSW=4.62E-10 MJSW=0.2955 PB=0.7",
			".MODEL P PMOS LEVEL=2 LD=0.2875U TOX=502E-10",
			"+NSUB=1.715148E+15 VTO=-0.7045 KP=1.686E-05 GAMMA=0.3459",
			"+PHI=0.6 UO=248.933 UEXP=1.02652 UCRIT=182055.0",
			"+DELTA=1.0E-06 VMAX=100000.0 XJ=0.4U LAMBDA=1.25919E-02",
			"+NFS=1.0E+12 NEFF=1.001E-02 NSS=0.0 TPG=-1.0",
			"+RSH=79.10 CGDO=2.89E-10 CGSO=2.89E-10",
			"+CJ=1.319E-04 MJ=0.4125 CJSW=3.421E-10 MJSW=0.198 PB=0.66",
			".TEMP 25.0"
		};
		setSpiceHeaderLevel2(headerLevel2);

		//******************** DESIGN RULES ********************

		unConDist = new double[]
		{
			//          M M P S D S S W W C V P T P A S M M P S D S S W W P
			//          e e o A A e e e e u i a r o c a e e o A A e e e e a
			//          t t l c c l l l l t a s a l t c t t l c c l l l l d
			//          1 2 y t t P N l l     s n y C t 1 2 y t t P N P N F
			//                        P N       s C   W P P P P P P P P P r
			/* Met1  */ 3,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,
			/* Met2  */   4,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,
			/* Poly  */     2,1,1,X,X,X,X,X,2,X,X,4,X,X,X,X,X,X,X,X,X,X,X,X,
			/* SAct  */       3,3,X,X,4,X,X,2,X,X,X,5,X,X,X,X,X,X,X,X,X,X,X,
			/* DAct  */         3,X,X,X,X,X,2,X,X,X,5,X,X,X,X,X,X,X,X,X,X,X,
			/* SelP  */           X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,
			/* SelN  */             X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,
			/* WellP */               X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,
			/* WellN */                 X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,
			/* Cut   */                   2,2,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,
			/* Via   */                     2,X,2,X,X,X,X,X,X,X,X,X,X,X,X,X,
			/* Pass  */                       X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,
			/* Trans */                         X,X,X,X,X,X,X,X,X,X,X,X,X,X,
			/* PolyC */                           X,X,X,X,X,X,X,X,X,X,X,X,X,
			/* ActC  */                             X,X,X,X,X,X,X,X,X,X,X,X,
			/* SactW */                               X,X,X,X,X,X,X,X,X,X,X,
			/* Met1P */                                 X,X,X,X,X,X,X,X,X,X,
			/* Met2P */                                   X,X,X,X,X,X,X,X,X,
			/* PolyP */                                     X,X,X,X,X,X,X,X,
			/* SActP */                                       X,X,X,X,X,X,X,
			/* DActP */                                         X,X,X,X,X,X,
			/* SelPP */                                           X,X,X,X,X,
			/* SelNP */                                             X,X,X,X,
			/* WelPP */                                               X,X,X,
			/* WelNP */                                                 X,X,
			/* PadFr */                                                   X,
		};
		conDist = new double[]
		{
			//          M M P S D S S W W C V P T P A S M M P S D S S W W P
			//          e e o A A e e e e u i a r o c a e e o A A e e e e a
			//          t t l c c l l l l t a s a l t c t t l c c l l l l d
			//          1 2 y t t P N l l     s n y C t 1 2 y t t P N P N F
			//                        P N       s C   W P P P P P P P P P r
			/* Met1  */ X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,
			/* Met2  */   X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,
			/* Poly  */     X,1,1,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,
			/* SAct  */       X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,
			/* DAct  */         X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,
			/* SelP  */           X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,
			/* SelN  */             X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,
			/* WellP */               X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,
			/* WellN */                 X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,
			/* Cut   */                   X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,
			/* Via   */                     2,X,X,2,2,X,X,X,X,X,X,X,X,X,X,X,
			/* Pass  */                       X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,
			/* Trans */                         X,X,X,X,X,X,X,X,X,X,X,X,X,X,
			/* PolyC */                           2,X,X,X,X,X,X,X,X,X,X,X,X,
			/* ActC  */                             2,X,X,X,X,X,X,X,X,X,X,X,
			/* SactW */                               X,X,X,X,X,X,X,X,X,X,X,
			/* Met1P */                                 X,X,X,X,X,X,X,X,X,X,
			/* Met2P */                                   X,X,X,X,X,X,X,X,X,
			/* PolyP */                                     X,X,X,X,X,X,X,X,
			/* SActP */                                       X,X,X,X,X,X,X,
			/* DActP */                                         X,X,X,X,X,X,
			/* SelPP */                                           X,X,X,X,X,
			/* SelNP */                                             X,X,X,X,
			/* WelPP */                                               X,X,X,
			/* WelNP */                                                 X,X,
			/* PadFr */                                                   X,
		};

		//******************** ARCS ********************

		/** Metal-1 arc */
		ArcProto Metal_1_arc = ArcProto.newInstance(this, "Metal-1", 3, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(M_lay, 0, Poly.Type.FILLED)
		});
		Metal_1_arc.setFunction(ArcProto.Function.METAL1);
		Metal_1_arc.setFactoryFixedAngle(true);
		Metal_1_arc.setWipable();
		Metal_1_arc.setFactoryAngleIncrement(90);

		/** Metal-2 arc */
		ArcProto Metal_2_arc = ArcProto.newInstance(this, "Metal-2", 3, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(M0_lay, 0, Poly.Type.FILLED)
		});
		Metal_2_arc.setFunction(ArcProto.Function.METAL2);
		Metal_2_arc.setFactoryFixedAngle(true);
		Metal_2_arc.setWipable();
		Metal_2_arc.setFactoryAngleIncrement(90);

		/** Polysilicon arc */
		ArcProto Polysilicon_arc = ArcProto.newInstance(this, "Polysilicon", 2, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(P_lay, 0, Poly.Type.FILLED)
		});
		Polysilicon_arc.setFunction(ArcProto.Function.POLY1);
		Polysilicon_arc.setFactoryFixedAngle(true);
		Polysilicon_arc.setWipable();
		Polysilicon_arc.setFactoryAngleIncrement(90);

		/** S-Active arc */
		ArcProto S_Active_arc = ArcProto.newInstance(this, "S-Active", 6, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(SA_lay, 4, Poly.Type.FILLED),
			new Technology.ArcLayer(PS_lay, 0, Poly.Type.FILLED)
		});
		S_Active_arc.setFunction(ArcProto.Function.DIFFP);
		S_Active_arc.setFactoryFixedAngle(true);
		S_Active_arc.setWipable();
		S_Active_arc.setFactoryAngleIncrement(90);
		S_Active_arc.setWidthOffset(0);

		/** D-Active arc */
		ArcProto D_Active_arc = ArcProto.newInstance(this, "D-Active", 10, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(DA_lay, 8, Poly.Type.FILLED),
			new Technology.ArcLayer(PW_lay, 0, Poly.Type.FILLED)
		});
		D_Active_arc.setFunction(ArcProto.Function.DIFFN);
		D_Active_arc.setFactoryFixedAngle(true);
		D_Active_arc.setWipable();
		D_Active_arc.setFactoryAngleIncrement(90);
		D_Active_arc.setWidthOffset(0);

		/** Active arc */
		ArcProto Active_arc = ArcProto.newInstance(this, "Active", 2, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(DA_lay, 0, Poly.Type.FILLED),
			new Technology.ArcLayer(SA_lay, 0, Poly.Type.FILLED)
		});
		Active_arc.setFunction(ArcProto.Function.DIFF);
		Active_arc.setFactoryFixedAngle(true);
		Active_arc.setWipable();
		Active_arc.setFactoryAngleIncrement(90);

		//******************** RECTANGLE DESCRIPTIONS ********************

		Technology.TechPoint [] box_1 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(4), EdgeV.fromBottom(4)),
			new Technology.TechPoint(EdgeH.fromRight(4), EdgeV.makeCenter()),
		};
		Technology.TechPoint [] box_2 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(4), EdgeV.makeCenter()),
			new Technology.TechPoint(EdgeH.fromRight(4), EdgeV.fromTop(4)),
		};
		Technology.TechPoint [] box_3 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(2), EdgeV.fromBottom(6)),
			new Technology.TechPoint(EdgeH.fromRight(2), EdgeV.fromTop(6)),
		};
		Technology.TechPoint [] box_4 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(2), EdgeV.fromBottom(2)),
			new Technology.TechPoint(EdgeH.fromRight(2), EdgeV.makeCenter()),
		};
		Technology.TechPoint [] box_5 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(2), EdgeV.makeCenter()),
			new Technology.TechPoint(EdgeH.fromRight(2), EdgeV.fromTop(2)),
		};
		Technology.TechPoint [] box_6 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.makeLeftEdge(), EdgeV.fromBottom(4)),
			new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.fromTop(4)),
		};
		Technology.TechPoint [] box_7 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(1), EdgeV.fromBottom(1)),
			new Technology.TechPoint(EdgeH.fromRight(1), EdgeV.fromTop(1)),
			new Technology.TechPoint(EdgeH.fromCenter(-2), EdgeV.fromCenter(-2)),
			new Technology.TechPoint(EdgeH.fromCenter(2), EdgeV.fromCenter(2)),
		};
		Technology.TechPoint [] box_8 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(5), EdgeV.fromBottom(5)),
			new Technology.TechPoint(EdgeH.fromRight(5), EdgeV.fromTop(5)),
			new Technology.TechPoint(EdgeH.fromCenter(-2), EdgeV.fromCenter(-2)),
			new Technology.TechPoint(EdgeH.fromCenter(2), EdgeV.fromCenter(2)),
		};
		Technology.TechPoint [] box_9 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(1), EdgeV.fromBottom(1)),
			new Technology.TechPoint(EdgeH.fromLeft(3), EdgeV.fromBottom(3)),
		};
		Technology.TechPoint [] box_10 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(3), EdgeV.fromBottom(3)),
			new Technology.TechPoint(EdgeH.fromRight(3), EdgeV.fromTop(3)),
			new Technology.TechPoint(EdgeH.fromCenter(-2), EdgeV.fromCenter(-2)),
			new Technology.TechPoint(EdgeH.fromCenter(2), EdgeV.fromCenter(2)),
		};
		Technology.TechPoint [] box_11 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(4), EdgeV.fromBottom(4)),
			new Technology.TechPoint(EdgeH.fromRight(4), EdgeV.fromTop(4)),
		};
		Technology.TechPoint [] box_12 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(2), EdgeV.fromBottom(2)),
			new Technology.TechPoint(EdgeH.fromRight(2), EdgeV.fromTop(2)),
		};
		Technology.TechPoint [] box_13 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge()),
			new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.makeTopEdge()),
		};

		//******************** NODES ********************

		/** Metal-1-Pin */
		PrimitiveNode mp_node = PrimitiveNode.newInstance("Metal-1-Pin", this, 4, 4, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(PM_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, box_13)
			});
		mp_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, mp_node, new ArcProto [] {Metal_1_arc}, "metal-1", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
			});
		mp_node.setFunction(PrimitiveNode.Function.PIN);
		mp_node.setArcsWipe();
		mp_node.setArcsShrink();

		/** Metal-2-Pin */
		PrimitiveNode mp0_node = PrimitiveNode.newInstance("Metal-2-Pin", this, 4, 4, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(PM0_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, box_13)
			});
		mp0_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, mp0_node, new ArcProto [] {Metal_2_arc}, "metal-2", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
			});
		mp0_node.setFunction(PrimitiveNode.Function.PIN);
		mp0_node.setArcsWipe();
		mp0_node.setArcsShrink();

		/** Polysilicon-Pin */
		PrimitiveNode pp_node = PrimitiveNode.newInstance("Polysilicon-Pin", this, 2, 2, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(PP_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, box_13)
			});
		pp_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, pp_node, new ArcProto [] {Polysilicon_arc}, "polysilicon", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(1), EdgeV.fromBottom(1), EdgeH.fromRight(1), EdgeV.fromTop(1))
			});
		pp_node.setFunction(PrimitiveNode.Function.PIN);
		pp_node.setArcsWipe();
		pp_node.setArcsShrink();

		/** S-Active-Pin */
		PrimitiveNode sap_node = PrimitiveNode.newInstance("S-Active-Pin", this, 6, 6, new SizeOffset(2, 2, 2, 2),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(PSA_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, box_12),
				new Technology.NodeLayer(PPS_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, box_13)
			});
		sap_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, sap_node, new ArcProto [] {S_Active_arc}, "s-active", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(3), EdgeV.fromBottom(3), EdgeH.fromRight(3), EdgeV.fromTop(3))
			});
		sap_node.setFunction(PrimitiveNode.Function.PIN);
		sap_node.setArcsWipe();
		sap_node.setArcsShrink();

		/** D-Active-Pin */
		PrimitiveNode dap_node = PrimitiveNode.newInstance("D-Active-Pin", this, 10, 10, new SizeOffset(4, 4, 4, 4),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(PDA_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, box_11),
				new Technology.NodeLayer(PPW_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, box_13)
			});
		dap_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, dap_node, new ArcProto [] {D_Active_arc}, "d-active", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(5), EdgeV.fromBottom(5), EdgeH.fromRight(5), EdgeV.fromTop(5))
			});
		dap_node.setFunction(PrimitiveNode.Function.PIN);
		dap_node.setArcsWipe();
		dap_node.setArcsShrink();

		/** Active-Pin */
		PrimitiveNode ap_node = PrimitiveNode.newInstance("Active-Pin", this, 2, 2, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(PDA_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, box_13),
				new Technology.NodeLayer(PSA_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, box_13)
			});
		ap_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, ap_node, new ArcProto [] {Active_arc, S_Active_arc, D_Active_arc}, "active", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(1), EdgeV.fromBottom(1), EdgeH.fromRight(1), EdgeV.fromTop(1))
			});
		ap_node.setFunction(PrimitiveNode.Function.PIN);
		ap_node.setArcsWipe();
		ap_node.setArcsShrink();

		/** Metal-1-S-Active-Con */
		PrimitiveNode msac_node = PrimitiveNode.newInstance("Metal-1-S-Active-Con", this, 10, 10, new SizeOffset(2, 2, 2, 2),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(M_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.MINBOX, box_10),
				new Technology.NodeLayer(SA_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_12),
				new Technology.NodeLayer(PS_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_13),
				new Technology.NodeLayer(AC_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_9)
			});
		msac_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, msac_node, new ArcProto [] {S_Active_arc, Metal_1_arc}, "metal-1-s-act", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(3), EdgeV.fromBottom(3), EdgeH.fromRight(3), EdgeV.fromTop(3))
			});
		msac_node.setFunction(PrimitiveNode.Function.CONTACT);
		msac_node.setSpecialType(PrimitiveNode.MULTICUT);
		msac_node.setSpecialValues(new double [] {2, 2, 2, 2, 2, 2});

		/** Metal-1-D-Active-Con */
		PrimitiveNode mdac_node = PrimitiveNode.newInstance("Metal-1-D-Active-Con", this, 14, 14, new SizeOffset(4, 4, 4, 4),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(M_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.MINBOX, box_8),
				new Technology.NodeLayer(DA_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_11),
				new Technology.NodeLayer(PW_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_13),
				new Technology.NodeLayer(AC_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_9)
			});
		mdac_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, mdac_node, new ArcProto [] {D_Active_arc, Metal_1_arc}, "metal-1-d-act", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(5), EdgeV.fromBottom(5), EdgeH.fromRight(5), EdgeV.fromTop(5))
			});
		mdac_node.setFunction(PrimitiveNode.Function.CONTACT);
		mdac_node.setSpecialType(PrimitiveNode.MULTICUT);
		mdac_node.setSpecialValues(new double [] {2, 2, 2, 2, 2, 2});

		/** Metal-1-Polysilicon-Con */
		PrimitiveNode mpc_node = PrimitiveNode.newInstance("Metal-1-Polysilicon-Con", this, 6, 6, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(M_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.MINBOX, box_7),
				new Technology.NodeLayer(P_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_13),
				new Technology.NodeLayer(PC_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_9)
			});
		mpc_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, mpc_node, new ArcProto [] {Polysilicon_arc, Metal_1_arc}, "metal-1-polysilicon", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(1), EdgeV.fromBottom(1), EdgeH.fromRight(1), EdgeV.fromTop(1))
			});
		mpc_node.setFunction(PrimitiveNode.Function.CONTACT);
		mpc_node.setSpecialType(PrimitiveNode.MULTICUT);
		mpc_node.setSpecialValues(new double [] {2, 2, 2, 2, 2, 2});

		/** S-Transistor */
		PrimitiveNode st_node = PrimitiveNode.newInstance("S-Transistor", this, 6, 10, new SizeOffset(2, 2, 4, 4),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(P_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_6, 1, 1, 2, 2),
				new Technology.NodeLayer(SA_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_12, 3, 3, 0, 0),
				new Technology.NodeLayer(PS_lay, -1, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_13, 5, 5, 2, 2)
			});
		st_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, st_node, new ArcProto [] {Polysilicon_arc}, "s-trans-poly-left", 180,90, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.fromBottom(5), EdgeH.fromLeft(1), EdgeV.fromTop(5)),
				PrimitivePort.newInstance(this, st_node, new ArcProto [] {S_Active_arc}, "s-trans-diff-top", 90,90, 2, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(3), EdgeV.fromTop(3), EdgeH.fromRight(3), EdgeV.fromTop(2)),
				PrimitivePort.newInstance(this, st_node, new ArcProto [] {Polysilicon_arc}, "s-trans-poly-right", 0,90, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromRight(1), EdgeV.fromBottom(5), EdgeH.makeRightEdge(), EdgeV.fromTop(5)),
				PrimitivePort.newInstance(this, st_node, new ArcProto [] {S_Active_arc}, "s-trans-diff-bottom", 270,90, 3, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(3), EdgeV.fromBottom(2), EdgeH.fromRight(3), EdgeV.fromBottom(3))
			});
		st_node.setFunction(PrimitiveNode.Function.TRAPMOS);
		st_node.setHoldsOutline();
		st_node.setCanShrink();
		st_node.setSpecialType(PrimitiveNode.SERPTRANS);
		st_node.setSpecialValues(new double [] {0.0333333, 1, 1, 2, 1, 1});

		/** D-Transistor */
		PrimitiveNode dt_node = PrimitiveNode.newInstance("D-Transistor", this, 10, 14, new SizeOffset(4, 4, 6, 6),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(P_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_3, 1, 1, 2, 2),
				new Technology.NodeLayer(PW_lay, -1, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_13, 7, 7, 4, 4),
				new Technology.NodeLayer(DA_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_11, 3, 3, 0, 0)
			});
		dt_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, dt_node, new ArcProto [] {Polysilicon_arc}, "d-trans-poly-left", 180,90, 1, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(2), EdgeV.fromBottom(7), EdgeH.fromLeft(3), EdgeV.fromTop(7)),
				PrimitivePort.newInstance(this, dt_node, new ArcProto [] {D_Active_arc}, "d-trans-diff-top", 90,90, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(5), EdgeV.fromTop(5), EdgeH.fromRight(5), EdgeV.fromTop(4)),
				PrimitivePort.newInstance(this, dt_node, new ArcProto [] {Polysilicon_arc}, "d-trans-poly-right", 0,90, 1, PortCharacteristic.UNKNOWN,
					EdgeH.fromRight(3), EdgeV.fromBottom(7), EdgeH.fromRight(2), EdgeV.fromTop(7)),
				PrimitivePort.newInstance(this, dt_node, new ArcProto [] {D_Active_arc}, "d-trans-diff-bottom", 270,90, 3, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(5), EdgeV.fromBottom(4), EdgeH.fromRight(5), EdgeV.fromBottom(5))
			});
		dt_node.setFunction(PrimitiveNode.Function.TRANMOS);
		dt_node.setHoldsOutline();
		dt_node.setCanShrink();
		dt_node.setSpecialType(PrimitiveNode.SERPTRANS);
		dt_node.setSpecialValues(new double [] {0.0333333, 1, 1, 2, 1, 1});

		/** Metal-1-Metal-2-Con */
		PrimitiveNode mmc_node = PrimitiveNode.newInstance("Metal-1-Metal-2-Con", this, 4, 4, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(M_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_13),
				new Technology.NodeLayer(M0_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_13),
				new Technology.NodeLayer(V_lay, 0, Poly.Type.CLOSED, Technology.NodeLayer.BOX, box_9)
			});
		mmc_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, mmc_node, new ArcProto [] {Metal_1_arc, Metal_2_arc}, "metal-1-metal-2", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
			});
		mmc_node.setFunction(PrimitiveNode.Function.CONTACT);
		mmc_node.setSpecialType(PrimitiveNode.MULTICUT);
		mmc_node.setSpecialValues(new double [] {2, 2, 1, 1, 3, 3});

		/** Metal-1-Well-Con */
		PrimitiveNode mwc_node = PrimitiveNode.newInstance("Metal-1-Well-Con", this, 14, 14, new SizeOffset(4, 4, 4, 4),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(SAW_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_11),
				new Technology.NodeLayer(M_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.MINBOX, box_8),
				new Technology.NodeLayer(PW_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_13),
				new Technology.NodeLayer(PS_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.MINBOX, box_8),
				new Technology.NodeLayer(AC_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_9)
			});
		mwc_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, mwc_node, new ArcProto [] {Metal_1_arc, Active_arc}, "metal-1-well", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(5.5), EdgeV.fromBottom(5.5), EdgeH.fromRight(5.5), EdgeV.fromTop(5.5))
			});
		mwc_node.setFunction(PrimitiveNode.Function.WELL);
		mwc_node.setSpecialType(PrimitiveNode.MULTICUT);
		mwc_node.setSpecialValues(new double [] {2, 2, 2, 2, 2, 2});

		/** Metal-1-Substrate-Con */
		PrimitiveNode msc_node = PrimitiveNode.newInstance("Metal-1-Substrate-Con", this, 6, 6, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(M_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.MINBOX, box_7),
				new Technology.NodeLayer(DA_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_13),
				new Technology.NodeLayer(AC_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_9)
			});
		msc_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, msc_node, new ArcProto [] {Metal_1_arc, Active_arc}, "metal-1-substrate", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
			});
		msc_node.setFunction(PrimitiveNode.Function.SUBSTRATE);
		msc_node.setSpecialType(PrimitiveNode.MULTICUT);
		msc_node.setSpecialValues(new double [] {2, 2, 2, 2, 2, 2});

		/** Metal-1-Node */
		PrimitiveNode mn_node = PrimitiveNode.newInstance("Metal-1-Node", this, 4, 4, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(M_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_13)
			});
		mn_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, mn_node, new ArcProto [] {Metal_1_arc}, "metal-1", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		mn_node.setFunction(PrimitiveNode.Function.NODE);
		mn_node.setHoldsOutline();
		mn_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Metal-2-Node */
		PrimitiveNode mn0_node = PrimitiveNode.newInstance("Metal-2-Node", this, 4, 4, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(M0_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_13)
			});
		mn0_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, mn0_node, new ArcProto [] {Metal_2_arc}, "metal-2", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		mn0_node.setFunction(PrimitiveNode.Function.NODE);
		mn0_node.setHoldsOutline();
		mn0_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Polysilicon-Node */
		PrimitiveNode pn_node = PrimitiveNode.newInstance("Polysilicon-Node", this, 4, 4, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(P_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_13)
			});
		pn_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, pn_node, new ArcProto [] {Polysilicon_arc}, "polysilicon", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		pn_node.setFunction(PrimitiveNode.Function.NODE);
		pn_node.setHoldsOutline();
		pn_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Active-Node */
		PrimitiveNode an_node = PrimitiveNode.newInstance("Active-Node", this, 4, 4, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(SA_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_13)
			});
		an_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, an_node, new ArcProto [] {Active_arc, S_Active_arc, D_Active_arc}, "active", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		an_node.setFunction(PrimitiveNode.Function.NODE);
		an_node.setHoldsOutline();
		an_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** D-Active-Node */
		PrimitiveNode dan_node = PrimitiveNode.newInstance("D-Active-Node", this, 4, 4, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(DA_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_13)
			});
		dan_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, dan_node, new ArcProto [] {Active_arc, S_Active_arc, D_Active_arc}, "active", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		dan_node.setFunction(PrimitiveNode.Function.NODE);
		dan_node.setHoldsOutline();
		dan_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** P-Select-Node */
		PrimitiveNode psn_node = PrimitiveNode.newInstance("P-Select-Node", this, 6, 6, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(PS_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_13)
			});
		psn_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, psn_node, new ArcProto [] {}, "select", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		psn_node.setFunction(PrimitiveNode.Function.NODE);
		psn_node.setHoldsOutline();
		psn_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Cut-Node */
		PrimitiveNode cn_node = PrimitiveNode.newInstance("Cut-Node", this, 2, 2, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(CC_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_13)
			});
		cn_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, cn_node, new ArcProto [] {}, "cut", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		cn_node.setFunction(PrimitiveNode.Function.NODE);
		cn_node.setHoldsOutline();
		cn_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Poly-Cut-Node */
		PrimitiveNode pcn_node = PrimitiveNode.newInstance("Poly-Cut-Node", this, 2, 2, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(PC_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_13)
			});
		pcn_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, pcn_node, new ArcProto [] {}, "polycut", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		pcn_node.setFunction(PrimitiveNode.Function.NODE);
		pcn_node.setHoldsOutline();
		pcn_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Active-Cut-Node */
		PrimitiveNode acn_node = PrimitiveNode.newInstance("Active-Cut-Node", this, 2, 2, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(AC_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_13)
			});
		acn_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, acn_node, new ArcProto [] {}, "activecut", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		acn_node.setFunction(PrimitiveNode.Function.NODE);
		acn_node.setHoldsOutline();
		acn_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Via-Node */
		PrimitiveNode vn_node = PrimitiveNode.newInstance("Via-Node", this, 2, 2, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(V_lay, 0, Poly.Type.CLOSED, Technology.NodeLayer.BOX, box_13)
			});
		vn_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, vn_node, new ArcProto [] {}, "via", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		vn_node.setFunction(PrimitiveNode.Function.NODE);
		vn_node.setHoldsOutline();
		vn_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** P-Well-Node */
		PrimitiveNode pwn_node = PrimitiveNode.newInstance("P-Well-Node", this, 6, 6, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(PW_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_13)
			});
		pwn_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, pwn_node, new ArcProto [] {S_Active_arc}, "well", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		pwn_node.setFunction(PrimitiveNode.Function.NODE);
		pwn_node.setHoldsOutline();
		pwn_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Passivation-Node */
		PrimitiveNode pn0_node = PrimitiveNode.newInstance("Passivation-Node", this, 8, 8, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(P0_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_13)
			});
		pn0_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, pn0_node, new ArcProto [] {}, "passivation", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		pn0_node.setFunction(PrimitiveNode.Function.NODE);
		pn0_node.setHoldsOutline();
		pn0_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Pad-Frame-Node */
		PrimitiveNode pfn_node = PrimitiveNode.newInstance("Pad-Frame-Node", this, 8, 8, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(PF_lay, 0, Poly.Type.CLOSED, Technology.NodeLayer.BOX, box_13)
			});
		pfn_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, pfn_node, new ArcProto [] {}, "pad-frame", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		pfn_node.setFunction(PrimitiveNode.Function.NODE);
		pfn_node.setHoldsOutline();
		pfn_node.setSpecialType(PrimitiveNode.POLYGONAL);

		// The pure layer nodes
		M_lay.setPureLayerNode(mn_node);		// Metal-1
		M0_lay.setPureLayerNode(mn0_node);		// Metal-2
		P_lay.setPureLayerNode(pn_node);		// Polysilicon
		SA_lay.setPureLayerNode(an_node);		// S-Active
		DA_lay.setPureLayerNode(dan_node);		// D-Active
		PS_lay.setPureLayerNode(psn_node);		// P-Select
		NS_lay.setPureLayerNode(psn_node);		// N-Select
		PW_lay.setPureLayerNode(pwn_node);		// P-Well
		NW_lay.setPureLayerNode(pwn_node);		// N-Well
		CC_lay.setPureLayerNode(cn_node);		// Contact-Cut
		V_lay.setPureLayerNode(vn_node);		// Via
		P0_lay.setPureLayerNode(pn0_node);		// Passivation
		PC_lay.setPureLayerNode(pcn_node);		// Poly-Cut
		AC_lay.setPureLayerNode(acn_node);		// Active-Cut
		SAW_lay.setPureLayerNode(pwn_node);		// S-Active-Well
		PF_lay.setPureLayerNode(pfn_node);		// Pad-Frame

        // Information for palette
        int maxY = 2 /*metal arcs*/ + 3 /* active arcs */ + 1 /* text */ + 1 /* poly*/ + 1 /* trans */;
        nodeGroups = new Object[maxY][3];
        int count = -1;

        nodeGroups[++count][0] = st_node; nodeGroups[count][1] = dt_node; nodeGroups[count][2] = msc_node;
        nodeGroups[++count][0] = S_Active_arc; nodeGroups[count][1] = sap_node; nodeGroups[count][2] = msac_node;
        nodeGroups[++count][0] = D_Active_arc; nodeGroups[count][1] = dap_node; nodeGroups[count][2] = mdac_node;
        nodeGroups[++count][0] = Active_arc; nodeGroups[count][1] = ap_node; nodeGroups[count][2] = mwc_node;
        nodeGroups[++count][0] = Polysilicon_arc; nodeGroups[count][1] = pp_node; nodeGroups[count][2] = mpc_node;
        nodeGroups[++count][0] = Metal_1_arc; nodeGroups[count][1] = mp_node; nodeGroups[count][2] = mmc_node;
        nodeGroups[++count][0] = Metal_2_arc; nodeGroups[count][1] = mp0_node;
        nodeGroups[++count][0] = "Pure"; nodeGroups[count][1] = "Misc."; nodeGroups[count][2] = "Cell";
	};

	/**
	 * Method to return the "factory "design rules for this Technology.
	 * @return the design rules for this Technology.
     * @param resizeNodes


     */
	public DRCRules getFactoryDesignRules(boolean resizeNodes)
	{
		return MOSRules.makeSimpleRules(this, conDist, unConDist);
	}
}
