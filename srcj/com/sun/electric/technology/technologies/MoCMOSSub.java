/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: MoCMOSSub.java
 * mocmossub technology description
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
 * This is the Complementary MOS (old, from MOSIS, Submicron, 2-6 metals [now 6], double poly, converts to newer MOCMOS) Technology.
 */
public class MoCMOSSub extends Technology
{
	/** the Complementary MOS (old, from MOSIS, Submicron, 2-6 metals [now 6], double poly, converts to newer MOCMOS) Technology object. */	public static final MoCMOSSub tech = new MoCMOSSub();

	private static final double X = -1;
	private double [] conDist, unConDist;

	// -------------------- private and protected methods ------------------------
	private MoCMOSSub()
	{
		super("mocmossub");
		setTechShortName("Submicron MOSIS CMOS");
		setTechDesc("MOSIS CMOS (old submicron rules, 2-6 metals [now 6], double poly, converts to newer MOSIS CMOS)");
		setFactoryScale(200, true);   // in nanometers: really 0.2 microns
		setNoNegatedArcs();
		setStaticTechnology();

        //Foundry
        Foundry mosis = new Foundry(Foundry.Type.MOSIS);
        foundries.add(mosis);

		setFactoryTransparentLayers(new Color []
		{
			new Color( 96,209,255), // Metal-1
			new Color(255,155,192), // Polysilicon-1
			new Color(107,226, 96), // P-Active
			new Color(224, 95,255), // Metal-2
			new Color(247,251, 20)  // Metal-3
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

		/** M1 layer */
		Layer M1_lay = Layer.newInstance(this, "Metal-3",
			new EGraphics(false, false, null, EGraphics.TRANSPARENT_5, 247,251, 20,/*0,0,0,*/0.8,true,
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

		/** M2 layer */
		Layer M2_lay = Layer.newInstance(this, "Metal-4",
			new EGraphics(true, true, null, 0, 0,0,0,0.8,true,
			new int[] { 0x0808,   //     X       X   
						0x1818,   //    XX      XX   
						0x2828,   //   X X     X X   
						0x4848,   //  X  X    X  X   
						0xfcfc,   // XXXXXX  XXXXXX  
						0x0808,   //     X       X   
						0x0808,   //     X       X   
						0x0000,   //                 
						0x0808,   //     X       X   
						0x1818,   //    XX      XX   
						0x2828,   //   X X     X X   
						0x4848,   //  X  X    X  X   
						0xfcfc,   // XXXXXX  XXXXXX  
						0x0808,   //     X       X   
						0x0808,   //     X       X   
						0x0000}));//                 

		/** M3 layer */
		Layer M3_lay = Layer.newInstance(this, "Metal-5",
			new EGraphics(true, true, null, 0, 0,0,0,0.8,true,
			new int[] { 0xfcfc,   // XXXXXX  XXXXXX  
						0x8080,   // X       X       
						0x8080,   // X       X       
						0xf8f8,   // XXXXX   XXXXX   
						0x0404,   //      X       X  
						0x0404,   //      X       X  
						0xf8f8,   // XXXXX   XXXXX   
						0x0000,   //                 
						0xfcfc,   // XXXXXX  XXXXXX  
						0x8080,   // X       X       
						0x8080,   // X       X       
						0xf8f8,   // XXXXX   XXXXX   
						0x0404,   //      X       X  
						0x0404,   //      X       X  
						0xf8f8,   // XXXXX   XXXXX   
						0x0000}));//                 

		/** M4 layer */
		Layer M4_lay = Layer.newInstance(this, "Metal-6",
			new EGraphics(true, true, null, 0, 161,184,69,0.8,true,
			new int[] { 0x1818,   //    XX      XX   
						0x6060,   //  XX      XX     
						0x8080,   // X       X       
						0xf8f8,   // XXXXX   XXXXX   
						0x8484,   // X    X  X    X  
						0x8484,   // X    X  X    X  
						0x7878,   //  XXXX    XXXX   
						0x0000,   //                 
						0x1818,   //    XX      XX   
						0x6060,   //  XX      XX     
						0x8080,   // X       X       
						0xf8f8,   // XXXXX   XXXXX   
						0x8484,   // X    X  X    X  
						0x8484,   // X    X  X    X  
						0x7878,   //  XXXX    XXXX   
						0x0000}));//                 

		/** P layer */
		Layer P_lay = Layer.newInstance(this, "Polysilicon-1",
			new EGraphics(false, false, null, EGraphics.TRANSPARENT_2, 255,155,192,/*224,95,255,*/0.8,true,
			new int[] { 0x1111,   //    X   X   X   X
						0xffff,   // XXXXXXXXXXXXXXXX
						0x1111,   //    X   X   X   X
						0x5555,   //  X X X X X X X X
						0x1111,   //    X   X   X   X
						0xffff,   // XXXXXXXXXXXXXXXX
						0x1111,   //    X   X   X   X
						0x5555,   //  X X X X X X X X
						0x1111,   //    X   X   X   X
						0xffff,   // XXXXXXXXXXXXXXXX
						0x1111,   //    X   X   X   X
						0x5555,   //  X X X X X X X X
						0x1111,   //    X   X   X   X
						0xffff,   // XXXXXXXXXXXXXXXX
						0x1111,   //    X   X   X   X
						0x5555}));//  X X X X X X X X

		/** P0 layer */
		Layer P0_lay = Layer.newInstance(this, "Polysilicon-2",
			new EGraphics(true, true, null, 0, 0,0,0,0.8,true,
			new int[] { 0xafaf,   // X X XXXXX X XXXX
						0x8888,   // X   X   X   X   
						0xfafa,   // XXXXX X XXXXX X 
						0x8888,   // X   X   X   X   
						0xafaf,   // X X XXXXX X XXXX
						0x8888,   // X   X   X   X   
						0xfafa,   // XXXXX X XXXXX X 
						0x8888,   // X   X   X   X   
						0xafaf,   // X X XXXXX X XXXX
						0x8888,   // X   X   X   X   
						0xfafa,   // XXXXX X XXXXX X 
						0x8888,   // X   X   X   X   
						0xafaf,   // X X XXXXX X XXXX
						0x8888,   // X   X   X   X   
						0xfafa,   // XXXXX X XXXXX X 
						0x8888}));// X   X   X   X   

		/** PA layer */
		Layer PA_lay = Layer.newInstance(this, "P-Active",
			new EGraphics(false, false, null, EGraphics.TRANSPARENT_3, 107,226, 96,/*247,251,20,*/0.8,true,
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

		/** NA layer */
		Layer NA_lay = Layer.newInstance(this, "N-Active",
			new EGraphics(false, false, null, EGraphics.TRANSPARENT_3, 107,226, 96,/*247,251,20,*/0.8,true,
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
			new EGraphics(true, true, null, 0, 162,170,97,0.8,true,
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
			new EGraphics(true, true, null, 0, 162,170,97,0.8,true,
			new int[] { 0x0100,   //        X        
						0x0000,   //                 
						0x0000,   //                 
						0x0000,   //                 
						0x0001,   //                X
						0x0000,   //                 
						0x0000,   //                 
						0x0000,   //                 
						0x0100,   //        X        
						0x0000,   //                 
						0x0000,   //                 
						0x0000,   //                 
						0x0001,   //                X
						0x0000,   //                 
						0x0000,   //                 
						0x0000}));//                 

		/** PW layer */
		Layer PW_lay = Layer.newInstance(this, "P-Well",
			new EGraphics(true, true, null, 0, 0,0,0,0.8,true,
			new int[] { 0x0202,   //       X       X 
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
						0x1010,   //    X       X    
						0x0808,   //     X       X   
						0x0404}));//      X       X  

		/** NW layer */
		Layer NW_lay = Layer.newInstance(this, "N-Well",
			new EGraphics(true, true, null, 0, 0,0,0,0.8,true,
			new int[] { 0x0002,   //               X 
						0x0000,   //                 
						0x0000,   //                 
						0x0000,   //                 
						0x0200,   //       X         
						0x0000,   //                 
						0x0000,   //                 
						0x0000,   //                 
						0x0002,   //               X 
						0x0000,   //                 
						0x0000,   //                 
						0x0000,   //                 
						0x0200,   //       X         
						0x0000,   //                 
						0x0000,   //                 
						0x0000}));//                 

		/** PC layer */
		Layer PC_lay = Layer.newInstance(this, "Poly-Cut",
			new EGraphics(false, false, null, 0, 161,151,126,0.8,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** AC layer */
		Layer AC_lay = Layer.newInstance(this, "Active-Cut",
			new EGraphics(false, false, null, 0, 161,151,126,0.8,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** V layer */
		Layer V_lay = Layer.newInstance(this, "Via1",
			new EGraphics(false, false, null, 0, 0,0,0,0.8,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** V0 layer */
		Layer V0_lay = Layer.newInstance(this, "Via2",
			new EGraphics(false, false, null, 0, 0,0,0,0.8,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** V1 layer */
		Layer V1_lay = Layer.newInstance(this, "Via3",
			new EGraphics(false, false, null, 0, 0,0,0,0.8,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** V2 layer */
		Layer V2_lay = Layer.newInstance(this, "Via4",
			new EGraphics(false, false, null, 0, 0,0,0,0.8,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** V3 layer */
		Layer V3_lay = Layer.newInstance(this, "Via5",
			new EGraphics(false, false, null, 0, 0,0,0,0.8,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** P1 layer */
		Layer P1_lay = Layer.newInstance(this, "Passivation",
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

		/** PC0 layer */
		Layer PC0_lay = Layer.newInstance(this, "Poly-Cap",
			new EGraphics(false, false, null, 0, 161,151,126,0.8,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** PAW layer */
		Layer PAW_lay = Layer.newInstance(this, "P-Active-Well",
			new EGraphics(false, false, null, EGraphics.TRANSPARENT_3, 107,226, 96,/*247,251,20,*/0.8,true,
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

		/** PM1 layer */
		Layer PM1_lay = Layer.newInstance(this, "Pseudo-Metal-3",
			new EGraphics(false, false, null, EGraphics.TRANSPARENT_5, 247,251, 20,/*0,0,0,*/0.8,true,
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

		/** PM2 layer */
		Layer PM2_lay = Layer.newInstance(this, "Pseudo-Metal-4",
			new EGraphics(true, true, null, 0, 0,0,0,0.8,true,
			new int[] { 0x0808,   //     X       X   
						0x1818,   //    XX      XX   
						0x2828,   //   X X     X X   
						0x4848,   //  X  X    X  X   
						0xfcfc,   // XXXXXX  XXXXXX  
						0x0808,   //     X       X   
						0x0808,   //     X       X   
						0x0000,   //                 
						0x0808,   //     X       X   
						0x1818,   //    XX      XX   
						0x2828,   //   X X     X X   
						0x4848,   //  X  X    X  X   
						0xfcfc,   // XXXXXX  XXXXXX  
						0x0808,   //     X       X   
						0x0808,   //     X       X   
						0x0000}));//                 

		/** PM3 layer */
		Layer PM3_lay = Layer.newInstance(this, "Pseudo-Metal-5",
			new EGraphics(true, true, null, 0, 0,0,0,0.8,true,
			new int[] { 0xfcfc,   // XXXXXX  XXXXXX  
						0x8080,   // X       X       
						0x8080,   // X       X       
						0xf8f8,   // XXXXX   XXXXX   
						0x0404,   //      X       X  
						0x0404,   //      X       X  
						0xf8f8,   // XXXXX   XXXXX   
						0x0000,   //                 
						0xfcfc,   // XXXXXX  XXXXXX  
						0x8080,   // X       X       
						0x8080,   // X       X       
						0xf8f8,   // XXXXX   XXXXX   
						0x0404,   //      X       X  
						0x0404,   //      X       X  
						0xf8f8,   // XXXXX   XXXXX   
						0x0000}));//                 

		/** PM4 layer */
		Layer PM4_lay = Layer.newInstance(this, "Pseudo-Metal-6",
			new EGraphics(true, true, null, 0, 161,184,69,0.8,true,
			new int[] { 0x1818,   //    XX      XX   
						0x6060,   //  XX      XX     
						0x8080,   // X       X       
						0xf8f8,   // XXXXX   XXXXX   
						0x8484,   // X    X  X    X  
						0x8484,   // X    X  X    X  
						0x7878,   //  XXXX    XXXX   
						0x0000,   //                 
						0x1818,   //    XX      XX   
						0x6060,   //  XX      XX     
						0x8080,   // X       X       
						0xf8f8,   // XXXXX   XXXXX   
						0x8484,   // X    X  X    X  
						0x8484,   // X    X  X    X  
						0x7878,   //  XXXX    XXXX   
						0x0000}));//                 

		/** PP layer */
		Layer PP_lay = Layer.newInstance(this, "Pseudo-Polysilicon",
			new EGraphics(false, false, null, EGraphics.TRANSPARENT_2, 255,155,192,/*224,95,255,*/0.8,true,
			new int[] { 0x1111,   //    X   X   X   X
						0xffff,   // XXXXXXXXXXXXXXXX
						0x1111,   //    X   X   X   X
						0x5555,   //  X X X X X X X X
						0x1111,   //    X   X   X   X
						0xffff,   // XXXXXXXXXXXXXXXX
						0x1111,   //    X   X   X   X
						0x5555,   //  X X X X X X X X
						0x1111,   //    X   X   X   X
						0xffff,   // XXXXXXXXXXXXXXXX
						0x1111,   //    X   X   X   X
						0x5555,   //  X X X X X X X X
						0x1111,   //    X   X   X   X
						0xffff,   // XXXXXXXXXXXXXXXX
						0x1111,   //    X   X   X   X
						0x5555}));//  X X X X X X X X

		/** PE layer */
		Layer PE_lay = Layer.newInstance(this, "Pseudo-Electrode",
			new EGraphics(true, true, null, 0, 0,0,0,0.8,true,
			new int[] { 0xafaf,   // X X XXXXX X XXXX
						0x8888,   // X   X   X   X   
						0xfafa,   // XXXXX X XXXXX X 
						0x8888,   // X   X   X   X   
						0xafaf,   // X X XXXXX X XXXX
						0x8888,   // X   X   X   X   
						0xfafa,   // XXXXX X XXXXX X 
						0x8888,   // X   X   X   X   
						0xafaf,   // X X XXXXX X XXXX
						0x8888,   // X   X   X   X   
						0xfafa,   // XXXXX X XXXXX X 
						0x8888,   // X   X   X   X   
						0xafaf,   // X X XXXXX X XXXX
						0x8888,   // X   X   X   X   
						0xfafa,   // XXXXX X XXXXX X 
						0x8888}));// X   X   X   X   

		/** PPA layer */
		Layer PPA_lay = Layer.newInstance(this, "Pseudo-P-Active",
			new EGraphics(false, false, null, EGraphics.TRANSPARENT_3, 107,226, 96,/*247,251,20,*/0.8,true,
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

		/** PNA layer */
		Layer PNA_lay = Layer.newInstance(this, "Pseudo-N-Active",
			new EGraphics(false, false, null, EGraphics.TRANSPARENT_3, 107,226, 96,/*247,251,20,*/0.8,true,
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
			new EGraphics(true, true, null, 0, 162,170,97,0.8,true,
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
			new EGraphics(true, true, null, 0, 162,170,97,0.8,true,
			new int[] { 0x0100,   //        X        
						0x0000,   //                 
						0x0000,   //                 
						0x0000,   //                 
						0x0001,   //                X
						0x0000,   //                 
						0x0000,   //                 
						0x0000,   //                 
						0x0100,   //        X        
						0x0000,   //                 
						0x0000,   //                 
						0x0000,   //                 
						0x0001,   //                X
						0x0000,   //                 
						0x0000,   //                 
						0x0000}));//                 

		/** PPW layer */
		Layer PPW_lay = Layer.newInstance(this, "Pseudo-P-Well",
			new EGraphics(true, true, null, 0, 0,0,0,0.8,true,
			new int[] { 0x0202,   //       X       X 
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
						0x1010,   //    X       X    
						0x0808,   //     X       X   
						0x0404}));//      X       X  

		/** PNW layer */
		Layer PNW_lay = Layer.newInstance(this, "Pseudo-N-Well",
			new EGraphics(true, true, null, 0, 0,0,0,0.8,true,
			new int[] { 0x0002,   //               X 
						0x0000,   //                 
						0x0000,   //                 
						0x0000,   //                 
						0x0200,   //       X         
						0x0000,   //                 
						0x0000,   //                 
						0x0000,   //                 
						0x0002,   //               X 
						0x0000,   //                 
						0x0000,   //                 
						0x0000,   //                 
						0x0200,   //       X         
						0x0000,   //                 
						0x0000,   //                 
						0x0000}));//                 

		/** PF layer */
		Layer PF_lay = Layer.newInstance(this, "Pad-Frame",
			new EGraphics(false, false, null, 0, 170,83,170,0.8,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		// The layer functions
		M_lay.setFunction(Layer.Function.METAL1);								// Metal-1
		M0_lay.setFunction(Layer.Function.METAL2);								// Metal-2
		M1_lay.setFunction(Layer.Function.METAL3);								// Metal-3
		M2_lay.setFunction(Layer.Function.METAL4);								// Metal-4
		M3_lay.setFunction(Layer.Function.METAL5);								// Metal-5
		M4_lay.setFunction(Layer.Function.METAL6);								// Metal-6
		P_lay.setFunction(Layer.Function.POLY1);								// Polysilicon-1
		P0_lay.setFunction(Layer.Function.POLY2);								// Polysilicon-2
		PA_lay.setFunction(Layer.Function.DIFFP);								// P-Active
		NA_lay.setFunction(Layer.Function.DIFFN);								// N-Active
		PS_lay.setFunction(Layer.Function.IMPLANTP);							// P-Select
		NS_lay.setFunction(Layer.Function.IMPLANTN);							// N-Select
		PW_lay.setFunction(Layer.Function.WELLP);								// P-Well
		NW_lay.setFunction(Layer.Function.WELLN);								// N-Well
		PC_lay.setFunction(Layer.Function.CONTACT1, Layer.Function.CONPOLY);	// Poly-Cut
		AC_lay.setFunction(Layer.Function.CONTACT1, Layer.Function.CONDIFF);	// Active-Cut
		V_lay.setFunction(Layer.Function.CONTACT2, Layer.Function.CONMETAL);	// Via1
		V0_lay.setFunction(Layer.Function.CONTACT3, Layer.Function.CONMETAL);	// Via2
		V1_lay.setFunction(Layer.Function.CONTACT4, Layer.Function.CONMETAL);	// Via3
		V2_lay.setFunction(Layer.Function.CONTACT5, Layer.Function.CONMETAL);	// Via4
		V3_lay.setFunction(Layer.Function.CONTACT6, Layer.Function.CONMETAL);	// Via5
		P1_lay.setFunction(Layer.Function.OVERGLASS);							// Passivation
		T_lay.setFunction(Layer.Function.TRANSISTOR, Layer.Function.PSEUDO);	// Transistor
		PC0_lay.setFunction(Layer.Function.CAP);								// Poly-Cap
		PAW_lay.setFunction(Layer.Function.DIFFP);								// P-Active-Well
		PM_lay.setFunction(Layer.Function.METAL1, Layer.Function.PSEUDO);		// Pseudo-Metal-1
		PM0_lay.setFunction(Layer.Function.METAL2, Layer.Function.PSEUDO);		// Pseudo-Metal-2
		PM1_lay.setFunction(Layer.Function.METAL3, Layer.Function.PSEUDO);		// Pseudo-Metal-3
		PM2_lay.setFunction(Layer.Function.METAL4, Layer.Function.PSEUDO);		// Pseudo-Metal-4
		PM3_lay.setFunction(Layer.Function.METAL5, Layer.Function.PSEUDO);		// Pseudo-Metal-5
		PM4_lay.setFunction(Layer.Function.METAL6, Layer.Function.PSEUDO);		// Pseudo-Metal-6
		PP_lay.setFunction(Layer.Function.POLY1, Layer.Function.PSEUDO);		// Pseudo-Polysilicon
		PE_lay.setFunction(Layer.Function.POLY2, Layer.Function.PSEUDO);		// Pseudo-Electrode
		PPA_lay.setFunction(Layer.Function.DIFFP, Layer.Function.PSEUDO);		// Pseudo-P-Active
		PNA_lay.setFunction(Layer.Function.DIFFN, Layer.Function.PSEUDO);		// Pseudo-N-Active
		PPS_lay.setFunction(Layer.Function.IMPLANTP, Layer.Function.PSEUDO);	// Pseudo-P-Select
		PNS_lay.setFunction(Layer.Function.IMPLANTN, Layer.Function.PSEUDO);	// Pseudo-N-Select
		PPW_lay.setFunction(Layer.Function.WELLP, Layer.Function.PSEUDO);		// Pseudo-P-Well
		PNW_lay.setFunction(Layer.Function.WELLN, Layer.Function.PSEUDO);		// Pseudo-N-Well
		PF_lay.setFunction(Layer.Function.ART);									// Pad-Frame

		// The CIF names
		M_lay.setFactoryCIFLayer("CMF");	// Metal-1
		M0_lay.setFactoryCIFLayer("CMS");	// Metal-2
		M1_lay.setFactoryCIFLayer("CMT");	// Metal-3
		M2_lay.setFactoryCIFLayer("CMQ");	// Metal-4
		M3_lay.setFactoryCIFLayer("CMP");	// Metal-5
		M4_lay.setFactoryCIFLayer("CM6");	// Metal-6
		P_lay.setFactoryCIFLayer("CPG");	// Polysilicon-1
		P0_lay.setFactoryCIFLayer("CEL");	// Polysilicon-2
		PA_lay.setFactoryCIFLayer("CAA");	// P-Active
		NA_lay.setFactoryCIFLayer("CAA");	// N-Active
		PS_lay.setFactoryCIFLayer("CSP");	// P-Select
		NS_lay.setFactoryCIFLayer("CSN");	// N-Select
		PW_lay.setFactoryCIFLayer("CWP");	// P-Well
		NW_lay.setFactoryCIFLayer("CWN");	// N-Well
		PC_lay.setFactoryCIFLayer("CCG");	// Poly-Cut
		AC_lay.setFactoryCIFLayer("CCG");	// Active-Cut
		V_lay.setFactoryCIFLayer("CVA");	// Via1
		V0_lay.setFactoryCIFLayer("CVS");	// Via2
		V1_lay.setFactoryCIFLayer("CVT");	// Via3
		V2_lay.setFactoryCIFLayer("CVQ");	// Via4
		V3_lay.setFactoryCIFLayer("CV5");	// Via5
		P1_lay.setFactoryCIFLayer("COG");	// Passivation
		T_lay.setFactoryCIFLayer("");		// Transistor
		PC0_lay.setFactoryCIFLayer("CPC");	// Poly-Cap
		PAW_lay.setFactoryCIFLayer("CAA");	// P-Active-Well
		PM_lay.setFactoryCIFLayer("");		// Pseudo-Metal-1
		PM0_lay.setFactoryCIFLayer("");		// Pseudo-Metal-2
		PM1_lay.setFactoryCIFLayer("");		// Pseudo-Metal-3
		PM2_lay.setFactoryCIFLayer("");		// Pseudo-Metal-4
		PM3_lay.setFactoryCIFLayer("");		// Pseudo-Metal-5
		PM4_lay.setFactoryCIFLayer("");		// Pseudo-Metal-6
		PP_lay.setFactoryCIFLayer("");		// Pseudo-Polysilicon
		PE_lay.setFactoryCIFLayer("");		// Pseudo-Electrode
		PPA_lay.setFactoryCIFLayer("");		// Pseudo-P-Active
		PNA_lay.setFactoryCIFLayer("");		// Pseudo-N-Active
		PPS_lay.setFactoryCIFLayer("CSP");	// Pseudo-P-Select
		PNS_lay.setFactoryCIFLayer("CSN");	// Pseudo-N-Select
		PPW_lay.setFactoryCIFLayer("CWP");	// Pseudo-P-Well
		PNW_lay.setFactoryCIFLayer("CWN");	// Pseudo-N-Well
		PF_lay.setFactoryCIFLayer("CX");	// Pad-Frame

		// The DXF names
		M_lay.setFactoryDXFLayer("");		// Metal-1
		M0_lay.setFactoryDXFLayer("");		// Metal-2
		M1_lay.setFactoryDXFLayer("");		// Metal-3
		M2_lay.setFactoryDXFLayer("");		// Metal-4
		M3_lay.setFactoryDXFLayer("");		// Metal-5
		M4_lay.setFactoryDXFLayer("");		// Metal-6
		P_lay.setFactoryDXFLayer("");		// Polysilicon-1
		P0_lay.setFactoryDXFLayer("");		// Polysilicon-2
		PA_lay.setFactoryDXFLayer("");		// P-Active
		NA_lay.setFactoryDXFLayer("");		// N-Active
		PS_lay.setFactoryDXFLayer("");		// P-Select
		NS_lay.setFactoryDXFLayer("");		// N-Select
		PW_lay.setFactoryDXFLayer("");		// P-Well
		NW_lay.setFactoryDXFLayer("");		// N-Well
		PC_lay.setFactoryDXFLayer("");		// Poly-Cut
		AC_lay.setFactoryDXFLayer("");		// Active-Cut
		V_lay.setFactoryDXFLayer("");		// Via1
		V0_lay.setFactoryDXFLayer("");		// Via2
		V1_lay.setFactoryDXFLayer("");		// Via3
		V2_lay.setFactoryDXFLayer("");		// Via4
		V3_lay.setFactoryDXFLayer("");		// Via5
		P1_lay.setFactoryDXFLayer("");		// Passivation
		T_lay.setFactoryDXFLayer("");		// Transistor
		PC0_lay.setFactoryDXFLayer("");		// Poly-Cap
		PAW_lay.setFactoryDXFLayer("");		// P-Active-Well
		PM_lay.setFactoryDXFLayer("");		// Pseudo-Metal-1
		PM0_lay.setFactoryDXFLayer("");		// Pseudo-Metal-2
		PM1_lay.setFactoryDXFLayer("");		// Pseudo-Metal-3
		PM2_lay.setFactoryDXFLayer("");		// Pseudo-Metal-4
		PM3_lay.setFactoryDXFLayer("");		// Pseudo-Metal-5
		PM4_lay.setFactoryDXFLayer("");		// Pseudo-Metal-6
		PP_lay.setFactoryDXFLayer("");		// Pseudo-Polysilicon
		PE_lay.setFactoryDXFLayer("");		// Pseudo-Electrode
		PPA_lay.setFactoryDXFLayer("");		// Pseudo-P-Active
		PNA_lay.setFactoryDXFLayer("");		// Pseudo-N-Active
		PPS_lay.setFactoryDXFLayer("");		// Pseudo-P-Select
		PNS_lay.setFactoryDXFLayer("");		// Pseudo-N-Select
		PPW_lay.setFactoryDXFLayer("");		// Pseudo-P-Well
		PNW_lay.setFactoryDXFLayer("");		// Pseudo-N-Well
		PF_lay.setFactoryDXFLayer("");		// Pad-Frame

		// The GDS names
		mosis.setFactoryGDSLayer(M_lay, "49");		// Metal-1
		mosis.setFactoryGDSLayer(M0_lay, "51");	// Metal-2
		mosis.setFactoryGDSLayer(M1_lay, "62");	// Metal-3
		mosis.setFactoryGDSLayer(M2_lay, "31");	// Metal-4
		mosis.setFactoryGDSLayer(M3_lay, "33");	// Metal-5
		mosis.setFactoryGDSLayer(M4_lay, "38");	// Metal-6
		mosis.setFactoryGDSLayer(P_lay, "46");		// Polysilicon-1
		mosis.setFactoryGDSLayer(P0_lay, "56");	// Polysilicon-2
		mosis.setFactoryGDSLayer(PA_lay, "43");	// P-Active
		mosis.setFactoryGDSLayer(NA_lay, "43");	// N-Active
		mosis.setFactoryGDSLayer(PS_lay, "44");	// P-Select
		mosis.setFactoryGDSLayer(NS_lay, "45");	// N-Select
		mosis.setFactoryGDSLayer(PW_lay, "41");	// P-Well
		mosis.setFactoryGDSLayer(NW_lay, "42");	// N-Well
		mosis.setFactoryGDSLayer(PC_lay, "25");	// Poly-Cut
		mosis.setFactoryGDSLayer(AC_lay, "25");	// Active-Cut
		mosis.setFactoryGDSLayer(V_lay, "50");		// Via1
		mosis.setFactoryGDSLayer(V0_lay, "61");	// Via2
		mosis.setFactoryGDSLayer(V1_lay, "30");	// Via3
		mosis.setFactoryGDSLayer(V2_lay, "32");	// Via4
		mosis.setFactoryGDSLayer(V3_lay, "39");	// Via5
		mosis.setFactoryGDSLayer(P1_lay, "52");	// Passivation
		mosis.setFactoryGDSLayer(T_lay, "");		// Transistor
		mosis.setFactoryGDSLayer(PC0_lay, "28");	// Poly-Cap
		mosis.setFactoryGDSLayer(PAW_lay, "43");	// P-Active-Well
//		mosis.setFactoryGDSLayer(PM_lay, "");		// Pseudo-Metal-1
//		mosis.setFactoryGDSLayer(PM0_lay, "");		// Pseudo-Metal-2
//		mosis.setFactoryGDSLayer(PM1_lay, "");		// Pseudo-Metal-3
//		mosis.setFactoryGDSLayer(PM2_lay, "");		// Pseudo-Metal-4
//		mosis.setFactoryGDSLayer(PM3_lay, "");		// Pseudo-Metal-5
//		mosis.setFactoryGDSLayer(PM4_lay, "");		// Pseudo-Metal-6
//		mosis.setFactoryGDSLayer(PP_lay, "");		// Pseudo-Polysilicon
//		mosis.setFactoryGDSLayer(PE_lay, "");		// Pseudo-Electrode
//		mosis.setFactoryGDSLayer(PPA_lay, "");		// Pseudo-P-Active
//		mosis.setFactoryGDSLayer(PNA_lay, "");		// Pseudo-N-Active
//		mosis.setFactoryGDSLayer(PPS_lay, "");		// Pseudo-P-Select
//		mosis.setFactoryGDSLayer(PNS_lay, "");		// Pseudo-N-Select
//		mosis.setFactoryGDSLayer(PPW_lay, "");		// Pseudo-P-Well
//		mosis.setFactoryGDSLayer(PNW_lay, "");		// Pseudo-N-Well
		mosis.setFactoryGDSLayer(PF_lay, "19");	// Pad-Frame

		// The layer height
		M_lay.setFactory3DInfo(0, 17);		// Metal-1
		M0_lay.setFactory3DInfo(0, 19);		// Metal-2
		M1_lay.setFactory3DInfo(0, 21);		// Metal-3
		M2_lay.setFactory3DInfo(0, 23);		// Metal-4
		M3_lay.setFactory3DInfo(0, 25);		// Metal-5
		M4_lay.setFactory3DInfo(0, 27);		// Metal-6
		P_lay.setFactory3DInfo(0, 15);		// Polysilicon-1
		P0_lay.setFactory3DInfo(0, 16);		// Polysilicon-2
		PA_lay.setFactory3DInfo(0, 13);		// P-Active
		NA_lay.setFactory3DInfo(0, 13);		// N-Active
		PS_lay.setFactory3DInfo(0, 12);		// P-Select
		NS_lay.setFactory3DInfo(0, 12);		// N-Select
		PW_lay.setFactory3DInfo(0, 11);		// P-Well
		NW_lay.setFactory3DInfo(0, 11);		// N-Well
		PC_lay.setFactory3DInfo(2, 16);		// Poly-Cut
		AC_lay.setFactory3DInfo(4, 15);		// Active-Cut
		V_lay.setFactory3DInfo(2, 18);		// Via1
		V0_lay.setFactory3DInfo(2, 20);		// Via2
		V1_lay.setFactory3DInfo(2, 22);		// Via3
		V2_lay.setFactory3DInfo(2, 24);		// Via4
		V3_lay.setFactory3DInfo(2, 26);		// Via5
		P1_lay.setFactory3DInfo(0, 30);		// Passivation
		T_lay.setFactory3DInfo(0, 31);		// Transistor
		PC0_lay.setFactory3DInfo(0, 28);	// Poly-Cap
		PAW_lay.setFactory3DInfo(0, 29);	// P-Active-Well
		PM_lay.setFactory3DInfo(0, 17);		// Pseudo-Metal-1
		PM0_lay.setFactory3DInfo(0, 19);	// Pseudo-Metal-2
		PM1_lay.setFactory3DInfo(0, 21);	// Pseudo-Metal-3
		PM2_lay.setFactory3DInfo(0, 23);	// Pseudo-Metal-4
		PM3_lay.setFactory3DInfo(0, 25);	// Pseudo-Metal-5
		PM4_lay.setFactory3DInfo(0, 27);	// Pseudo-Metal-6
		PP_lay.setFactory3DInfo(0, 12);		// Pseudo-Polysilicon
		PE_lay.setFactory3DInfo(0, 13);		// Pseudo-Electrode
		PPA_lay.setFactory3DInfo(0, 11);	// Pseudo-P-Active
		PNA_lay.setFactory3DInfo(0, 11);	// Pseudo-N-Active
		PPS_lay.setFactory3DInfo(0, 2);		// Pseudo-P-Select
		PNS_lay.setFactory3DInfo(0, 2);		// Pseudo-N-Select
		PPW_lay.setFactory3DInfo(0, 0);		// Pseudo-P-Well
		PNW_lay.setFactory3DInfo(0, 0);		// Pseudo-N-Well
		PF_lay.setFactory3DInfo(0, 33);		// Pad-Frame

		// The SPICE information
		M_lay.setFactoryParasitics(0.06f, 0.07f, 0);	// Metal-1
		M0_lay.setFactoryParasitics(0.06f, 0.04f, 0);	// Metal-2
		M1_lay.setFactoryParasitics(0.06f, 0.04f, 0);	// Metal-3
		M2_lay.setFactoryParasitics(0.03f, 0.04f, 0);	// Metal-4
		M3_lay.setFactoryParasitics(0.03f, 0.04f, 0);	// Metal-5
		M4_lay.setFactoryParasitics(0.03f, 0.04f, 0);	// Metal-6
		P_lay.setFactoryParasitics(2.5f, 0.09f, 0);		// Polysilicon-1
		P0_lay.setFactoryParasitics(50.0f, 1.0f, 0);	// Polysilicon-2
		PA_lay.setFactoryParasitics(2.5f, 0.9f, 0);		// P-Active
		NA_lay.setFactoryParasitics(3.0f, 0.9f, 0);		// N-Active
		PS_lay.setFactoryParasitics(0, 0, 0);			// P-Select
		NS_lay.setFactoryParasitics(0, 0, 0);			// N-Select
		PW_lay.setFactoryParasitics(0, 0, 0);			// P-Well
		NW_lay.setFactoryParasitics(0, 0, 0);			// N-Well
		PC_lay.setFactoryParasitics(2.2f, 0, 0);		// Poly-Cut
		AC_lay.setFactoryParasitics(2.5f, 0, 0);		// Active-Cut
		V_lay.setFactoryParasitics(1.0f, 0, 0);			// Via1
		V0_lay.setFactoryParasitics(0.9f, 0, 0);		// Via2
		V1_lay.setFactoryParasitics(0.8f, 0, 0);		// Via3
		V2_lay.setFactoryParasitics(0.8f, 0, 0);		// Via4
		V3_lay.setFactoryParasitics(0.8f, 0, 0);		// Via5
		P1_lay.setFactoryParasitics(0, 0, 0);			// Passivation
		T_lay.setFactoryParasitics(0, 0, 0);			// Transistor
		PC0_lay.setFactoryParasitics(0, 0, 0);			// Poly-Cap
		PAW_lay.setFactoryParasitics(0, 0, 0);			// P-Active-Well
		PM_lay.setFactoryParasitics(0, 0, 0);			// Pseudo-Metal-1
		PM0_lay.setFactoryParasitics(0, 0, 0);			// Pseudo-Metal-2
		PM1_lay.setFactoryParasitics(0, 0, 0);			// Pseudo-Metal-3
		PM2_lay.setFactoryParasitics(0, 0, 0);			// Pseudo-Metal-4
		PM3_lay.setFactoryParasitics(0, 0, 0);			// Pseudo-Metal-5
		PM4_lay.setFactoryParasitics(0, 0, 0);			// Pseudo-Metal-6
		PP_lay.setFactoryParasitics(0, 0, 0);			// Pseudo-Polysilicon
		PE_lay.setFactoryParasitics(0, 0, 0);			// Pseudo-Electrode
		PPA_lay.setFactoryParasitics(0, 0, 0);			// Pseudo-P-Active
		PNA_lay.setFactoryParasitics(0, 0, 0);			// Pseudo-N-Active
		PPS_lay.setFactoryParasitics(0, 0, 0);			// Pseudo-P-Select
		PNS_lay.setFactoryParasitics(0, 0, 0);			// Pseudo-N-Select
		PPW_lay.setFactoryParasitics(0, 0, 0);			// Pseudo-P-Well
		PNW_lay.setFactoryParasitics(0, 0, 0);			// Pseudo-N-Well
		PF_lay.setFactoryParasitics(0, 0, 0);			// Pad-Frame
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
			//          M M M M M M P P P N S S W W P A V V V V V P T P P M M M M M M P P P N S S W W P
			//          e e e e e e o o A A e e e e o c i i i i i a r C a e e e e e e o o A A e e e e a
			//          t t t t t t l l c c l l l l l t a a a a a s a a c t t t t t t l l c c l l l l d
			//          1 2 3 4 5 6 y y t t P N l l y C 1 2 3 4 5 s n p t 1 2 3 4 5 6 1 2 t t P N P N F
			//                      1 2         P N C               s   W P P P P P P P P P P P P P P r
			/* Met1  */ 3,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,
			/* Met2  */   3,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,
			/* Met3  */     3,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,
			/* Met4  */       3,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,
			/* Met5  */         3,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,
			/* Met6  */           3,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,
			/* Poly1 */             3,X,1,1,X,X,X,X,X,2,X,X,X,X,X,X,X,X,1,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,
			/* Poly2 */               3,1,1,X,X,X,X,3,X,X,X,X,X,X,X,X,X,1,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,
			/* PAct  */                 3,3,X,X,X,X,2,X,X,X,X,X,X,X,X,X,3,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,
			/* NAct  */                   3,X,X,X,X,2,X,X,X,X,X,X,X,X,X,3,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,
			/* SelP  */                     2,0,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,
			/* SelN  */                       2,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,
			/* WellP */                        18,0,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,
			/* WellN */                          18,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,
			/* PolyC */                             3,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,
			/* ActC  */                               3,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,
			/* Via1  */                                 3,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,
			/* Via2  */                                   3,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,
			/* Via3  */                                     4,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,
			/* Via4  */                                       3,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,
			/* Via5  */                                         3,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,
			/* Pass  */                                           X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,
			/* Trans */                                             X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,
			/* PCap  */                                               X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,
			/* PactW */                                                 3,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,
			/* Met1P */                                                   X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,
			/* Met2P */                                                     X,X,X,X,X,X,X,X,X,X,X,X,X,X,
			/* Met3P */                                                       X,X,X,X,X,X,X,X,X,X,X,X,X,
			/* Met4P */                                                         X,X,X,X,X,X,X,X,X,X,X,X,
			/* Met5P */                                                           X,X,X,X,X,X,X,X,X,X,X,
			/* Met6P */                                                             X,X,X,X,X,X,X,X,X,X,
			/* Poly1P */                                                              X,X,X,X,X,X,X,X,X,
			/* Poly2P */                                                                X,X,X,X,X,X,X,X,
			/* PActP */                                                                   X,X,X,X,X,X,X,
			/* NActP */                                                                     X,X,X,X,X,X,
			/* SelPP */                                                                       X,X,X,X,X,
			/* SelNP */                                                                         X,X,X,X,
			/* WelPP */                                                                           X,X,X,
			/* WelNP */                                                                             X,X,
			/* PadFr */                                                                               X
		};
		conDist = new double[]
		{
			/*          M M M M M M P P P N S S W W P A V V V V V P T P P M M M M M M P P P N S S W W P */
			/*          e e e e e e o o A A e e e e o c i i i i i a r C a e e e e e e o o A A e e e e a */
			/*          t t t t t t l l c c l l l l l t a a a a a s a a c t t t t t t l l c c l l l l d */
			/*          1 2 3 4 5 6 y y t t P N l l y C 1 2 3 4 5 s n p t 1 2 3 4 5 6 1 2 t t P N P N F */
			/*                      1 2         P N C               s   W P P P P P P P P P P P P P P r */
			/* Met1  */ 3,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,
			/* Met2  */   3,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,
			/* Met3  */     3,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,
			/* Met4  */       3,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,
			/* Met5  */         3,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,
			/* Met6  */           3,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,
			/* Poly1 */             3,X,1,1,X,X,X,X,X,X,X,X,X,X,X,X,X,X,1,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,
			/* Poly2 */               3,1,1,X,X,X,X,X,X,X,X,X,X,X,X,X,X,1,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,
			/* PAct  */                 3,3,X,X,X,X,X,X,X,X,X,X,X,X,X,X,3,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,
			/* NAct  */                   3,X,X,X,X,X,X,X,X,X,X,X,X,X,X,3,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,
			/* SelP  */                     X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,
			/* SelN  */                       X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,
			/* WellP */                         6,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,
			/* WellN */                           6,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,
			/* PolyC */                             3,3,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,
			/* ActC  */                               3,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,
			/* Via1  */                                 3,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,
			/* Via2  */                                   3,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,
			/* Via3  */                                     4,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,
			/* Via4  */                                       3,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,
			/* Via5  */                                         3,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,
			/* Pass  */                                           X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,
			/* Trans */                                             X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,
			/* PCap  */                                               X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,
			/* PactW */                                                 3,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,
			/* Met1P */                                                   X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,
			/* Met2P */                                                     X,X,X,X,X,X,X,X,X,X,X,X,X,X,
			/* Met3P */                                                       X,X,X,X,X,X,X,X,X,X,X,X,X,
			/* Met4P */                                                         X,X,X,X,X,X,X,X,X,X,X,X,
			/* Met5P */                                                           X,X,X,X,X,X,X,X,X,X,X,
			/* Met6P */                                                             X,X,X,X,X,X,X,X,X,X,
			/* Poly1P */                                                              X,X,X,X,X,X,X,X,X,
			/* Poly2P */                                                                X,X,X,X,X,X,X,X,
			/* PActP */                                                                   X,X,X,X,X,X,X,
			/* NActP */                                                                     X,X,X,X,X,X,
			/* SelPP */                                                                       X,X,X,X,X,
			/* SelNP */                                                                         X,X,X,X,
			/* WelPP */                                                                           X,X,X,
			/* WelNP */                                                                             X,X,
			/* PadFr */                                                                               X
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

		/** Metal-3 arc */
		ArcProto Metal_3_arc = ArcProto.newInstance(this, "Metal-3", 3, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(M1_lay, 0, Poly.Type.FILLED)
		});
		Metal_3_arc.setFunction(ArcProto.Function.METAL3);
		Metal_3_arc.setFactoryFixedAngle(true);
		Metal_3_arc.setWipable();
		Metal_3_arc.setFactoryAngleIncrement(90);

		/** Metal-4 arc */
		ArcProto Metal_4_arc = ArcProto.newInstance(this, "Metal-4", 3, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(M2_lay, 0, Poly.Type.FILLED)
		});
		Metal_4_arc.setFunction(ArcProto.Function.METAL4);
		Metal_4_arc.setFactoryFixedAngle(true);
		Metal_4_arc.setWipable();
		Metal_4_arc.setFactoryAngleIncrement(90);

		/** Metal-5 arc */
		ArcProto Metal_5_arc = ArcProto.newInstance(this, "Metal-5", 3, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(M3_lay, 0, Poly.Type.FILLED)
		});
		Metal_5_arc.setFunction(ArcProto.Function.METAL5);
		Metal_5_arc.setFactoryFixedAngle(true);
		Metal_5_arc.setWipable();
		Metal_5_arc.setFactoryAngleIncrement(90);

		/** Metal-6 arc */
		ArcProto Metal_6_arc = ArcProto.newInstance(this, "Metal-6", 3, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(M4_lay, 0, Poly.Type.FILLED)
		});
		Metal_6_arc.setFunction(ArcProto.Function.METAL6);
		Metal_6_arc.setFactoryFixedAngle(true);
		Metal_6_arc.setWipable();
		Metal_6_arc.setFactoryAngleIncrement(90);

		/** Polysilicon-1 arc */
		ArcProto Polysilicon_1_arc = ArcProto.newInstance(this, "Polysilicon-1", 2, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(P_lay, 0, Poly.Type.FILLED)
		});
		Polysilicon_1_arc.setFunction(ArcProto.Function.POLY1);
		Polysilicon_1_arc.setFactoryFixedAngle(true);
		Polysilicon_1_arc.setWipable();
		Polysilicon_1_arc.setFactoryAngleIncrement(90);

		/** Polysilicon-2 arc */
		ArcProto Polysilicon_2_arc = ArcProto.newInstance(this, "Polysilicon-2", 3, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(P0_lay, 0, Poly.Type.FILLED)
		});
		Polysilicon_2_arc.setFunction(ArcProto.Function.POLY2);
		Polysilicon_2_arc.setFactoryFixedAngle(true);
		Polysilicon_2_arc.setWipable();
		Polysilicon_2_arc.setFactoryAngleIncrement(90);

		/** P-Active arc */
		ArcProto P_Active_arc = ArcProto.newInstance(this, "P-Active", 15, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(PA_lay, 12, Poly.Type.FILLED),
			new Technology.ArcLayer(PS_lay, 8, Poly.Type.FILLED),
			new Technology.ArcLayer(NW_lay, 0, Poly.Type.FILLED)
		});
		P_Active_arc.setFunction(ArcProto.Function.DIFFP);
		P_Active_arc.setFactoryFixedAngle(true);
		P_Active_arc.setWipable();
		P_Active_arc.setFactoryAngleIncrement(90);
		P_Active_arc.setWidthOffset(0);

		/** N-Active arc */
		ArcProto N_Active_arc = ArcProto.newInstance(this, "N-Active", 15, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(NA_lay, 12, Poly.Type.FILLED),
			new Technology.ArcLayer(NS_lay, 8, Poly.Type.FILLED),
			new Technology.ArcLayer(PW_lay, 0, Poly.Type.FILLED)
		});
		N_Active_arc.setFunction(ArcProto.Function.DIFFN);
		N_Active_arc.setFactoryFixedAngle(true);
		N_Active_arc.setWipable();
		N_Active_arc.setFactoryAngleIncrement(90);
		N_Active_arc.setWidthOffset(0);

		/** Active arc */
		ArcProto Active_arc = ArcProto.newInstance(this, "Active", 3, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(NA_lay, 0, Poly.Type.FILLED),
			new Technology.ArcLayer(PA_lay, 0, Poly.Type.FILLED)
		});
		Active_arc.setFunction(ArcProto.Function.DIFF);
		Active_arc.setFactoryFixedAngle(true);
		Active_arc.setWipable();
		Active_arc.setFactoryAngleIncrement(90);

		//******************** RECTANGLE DESCRIPTIONS ********************

		Technology.TechPoint [] box_1 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(5), EdgeV.fromBottom(5)),
			new Technology.TechPoint(EdgeH.fromRight(5), EdgeV.fromTop(5)),
			new Technology.TechPoint(EdgeH.fromCenter(-2), EdgeV.fromCenter(-2)),
			new Technology.TechPoint(EdgeH.fromCenter(2), EdgeV.fromCenter(2)),
		};
		Technology.TechPoint [] box_2 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(6), EdgeV.fromBottom(6)),
			new Technology.TechPoint(EdgeH.fromRight(6), EdgeV.makeCenter()),
		};
		Technology.TechPoint [] box_3 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(6), EdgeV.makeCenter()),
			new Technology.TechPoint(EdgeH.fromRight(6), EdgeV.fromTop(6)),
		};
		Technology.TechPoint [] box_4 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(3.5), EdgeV.fromBottom(9)),
			new Technology.TechPoint(EdgeH.fromRight(3.5), EdgeV.fromTop(9)),
		};
		Technology.TechPoint [] box_5 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(2), EdgeV.fromBottom(2)),
			new Technology.TechPoint(EdgeH.fromRight(2), EdgeV.fromTop(2)),
		};
		Technology.TechPoint [] box_6 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(0.5), EdgeV.fromBottom(0.5)),
			new Technology.TechPoint(EdgeH.fromRight(0.5), EdgeV.fromTop(0.5)),
		};
		Technology.TechPoint [] box_7 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(1), EdgeV.fromBottom(1)),
			new Technology.TechPoint(EdgeH.fromLeft(3), EdgeV.fromBottom(3)),
		};
		Technology.TechPoint [] box_8 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(6.5), EdgeV.fromBottom(6.5)),
			new Technology.TechPoint(EdgeH.fromRight(6.5), EdgeV.fromTop(6.5)),
		};
		Technology.TechPoint [] box_9 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(4), EdgeV.fromBottom(4)),
			new Technology.TechPoint(EdgeH.fromRight(4), EdgeV.fromTop(4)),
		};
		Technology.TechPoint [] box_10 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(6), EdgeV.fromBottom(6)),
			new Technology.TechPoint(EdgeH.fromRight(6), EdgeV.fromTop(6)),
		};
		Technology.TechPoint [] box_11 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(1), EdgeV.fromBottom(1)),
			new Technology.TechPoint(EdgeH.fromRight(1), EdgeV.fromTop(1)),
		};
		Technology.TechPoint [] box_12 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge()),
			new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.makeTopEdge()),
		};

		//******************** NODES ********************

		/** Metal-1-Pin */
		PrimitiveNode mp_node = PrimitiveNode.newInstance("Metal-1-Pin", this, 3, 3, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(PM_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, box_12)
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
		PrimitiveNode mp0_node = PrimitiveNode.newInstance("Metal-2-Pin", this, 3, 3, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(PM0_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, box_12)
			});
		mp0_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, mp0_node, new ArcProto [] {Metal_2_arc}, "metal-2", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
			});
		mp0_node.setFunction(PrimitiveNode.Function.PIN);
		mp0_node.setArcsWipe();
		mp0_node.setArcsShrink();

		/** Metal-3-Pin */
		PrimitiveNode mp1_node = PrimitiveNode.newInstance("Metal-3-Pin", this, 3, 3, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(PM1_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, box_12)
			});
		mp1_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, mp1_node, new ArcProto [] {Metal_3_arc}, "metal-3", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
			});
		mp1_node.setFunction(PrimitiveNode.Function.PIN);
		mp1_node.setArcsWipe();
		mp1_node.setArcsShrink();

		/** Metal-4-Pin */
		PrimitiveNode mp2_node = PrimitiveNode.newInstance("Metal-4-Pin", this, 3, 3, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(PM2_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, box_12)
			});
		mp2_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, mp2_node, new ArcProto [] {Metal_4_arc}, "metal-4", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
			});
		mp2_node.setFunction(PrimitiveNode.Function.PIN);
		mp2_node.setArcsWipe();
		mp2_node.setArcsShrink();

		/** Metal-5-Pin */
		PrimitiveNode mp3_node = PrimitiveNode.newInstance("Metal-5-Pin", this, 3, 3, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(PM3_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, box_12)
			});
		mp3_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, mp3_node, new ArcProto [] {Metal_5_arc}, "metal-5", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		mp3_node.setFunction(PrimitiveNode.Function.PIN);
		mp3_node.setArcsWipe();
		mp3_node.setArcsShrink();

		/** Metal-6-Pin */
		PrimitiveNode mp4_node = PrimitiveNode.newInstance("Metal-6-Pin", this, 3, 3, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(PM4_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, box_12)
			});
		mp4_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, mp4_node, new ArcProto [] {Metal_6_arc}, "metal-6", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
			});
		mp4_node.setFunction(PrimitiveNode.Function.PIN);
		mp4_node.setArcsWipe();
		mp4_node.setArcsShrink();

		/** Polysilicon-1-Pin */
		PrimitiveNode pp_node = PrimitiveNode.newInstance("Polysilicon-1-Pin", this, 2, 2, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(PP_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, box_12)
			});
		pp_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, pp_node, new ArcProto [] {Polysilicon_1_arc}, "polysilicon-1", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(1), EdgeV.fromBottom(1), EdgeH.fromRight(1), EdgeV.fromTop(1))
			});
		pp_node.setFunction(PrimitiveNode.Function.PIN);
		pp_node.setArcsWipe();
		pp_node.setArcsShrink();

		/** Polysilicon-2-Pin */
		PrimitiveNode pp0_node = PrimitiveNode.newInstance("Polysilicon-2-Pin", this, 3, 3, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(PE_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, box_12)
			});
		pp0_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, pp0_node, new ArcProto [] {Polysilicon_2_arc}, "polysilicon-2", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
			});
		pp0_node.setFunction(PrimitiveNode.Function.PIN);
		pp0_node.setArcsWipe();
		pp0_node.setArcsShrink();

		/** P-Active-Pin */
		PrimitiveNode pap_node = PrimitiveNode.newInstance("P-Active-Pin", this, 15, 15, new SizeOffset(6, 6, 6, 6),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(PPA_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, box_10),
				new Technology.NodeLayer(PPS_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, box_9),
				new Technology.NodeLayer(PNW_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, box_12)
			});
		pap_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, pap_node, new ArcProto [] {P_Active_arc}, "p-active", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(7.5), EdgeV.fromBottom(7.5), EdgeH.fromRight(7.5), EdgeV.fromTop(7.5))
			});
		pap_node.setFunction(PrimitiveNode.Function.PIN);
		pap_node.setArcsWipe();
		pap_node.setArcsShrink();

		/** N-Active-Pin */
		PrimitiveNode nap_node = PrimitiveNode.newInstance("N-Active-Pin", this, 15, 15, new SizeOffset(6, 6, 6, 6),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(PNA_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, box_10),
				new Technology.NodeLayer(PNS_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, box_9),
				new Technology.NodeLayer(PPW_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, box_12)
			});
		nap_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, nap_node, new ArcProto [] {N_Active_arc}, "n-active", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(7.5), EdgeV.fromBottom(7.5), EdgeH.fromRight(7.5), EdgeV.fromTop(7.5))
			});
		nap_node.setFunction(PrimitiveNode.Function.PIN);
		nap_node.setArcsWipe();
		nap_node.setArcsShrink();

		/** Active-Pin */
		PrimitiveNode ap_node = PrimitiveNode.newInstance("Active-Pin", this, 3, 3, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(PPA_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, box_12),
				new Technology.NodeLayer(PNA_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, box_12)
			});
		ap_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, ap_node, new ArcProto [] {Active_arc, P_Active_arc, N_Active_arc}, "active", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
			});
		ap_node.setFunction(PrimitiveNode.Function.PIN);
		ap_node.setArcsWipe();
		ap_node.setArcsShrink();

		/** Metal-1-P-Active-Con */
		PrimitiveNode mpac_node = PrimitiveNode.newInstance("Metal-1-P-Active-Con", this, 17, 17, new SizeOffset(6, 6, 6, 6),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(M_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_8),
				new Technology.NodeLayer(PA_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_10),
				new Technology.NodeLayer(NW_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_12),
				new Technology.NodeLayer(PS_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_9),
				new Technology.NodeLayer(AC_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_7)
			});
		mpac_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, mpac_node, new ArcProto [] {P_Active_arc, Metal_1_arc}, "metal-1-p-act", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(8), EdgeV.fromBottom(8), EdgeH.fromRight(8), EdgeV.fromTop(8))
			});
		mpac_node.setFunction(PrimitiveNode.Function.CONTACT);
		mpac_node.setSpecialType(PrimitiveNode.MULTICUT);
		mpac_node.setSpecialValues(new double [] {2, 2, 1.5, 1.5, 4, 4});

		/** Metal-1-N-Active-Con */
		PrimitiveNode mnac_node = PrimitiveNode.newInstance("Metal-1-N-Active-Con", this, 17, 17, new SizeOffset(6, 6, 6, 6),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(M_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_8),
				new Technology.NodeLayer(NA_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_10),
				new Technology.NodeLayer(PW_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_12),
				new Technology.NodeLayer(NS_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_9),
				new Technology.NodeLayer(AC_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_7)
			});
		mnac_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, mnac_node, new ArcProto [] {N_Active_arc, Metal_1_arc}, "metal-1-n-act", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(8), EdgeV.fromBottom(8), EdgeH.fromRight(8), EdgeV.fromTop(8))
			});
		mnac_node.setFunction(PrimitiveNode.Function.CONTACT);
		mnac_node.setSpecialType(PrimitiveNode.MULTICUT);
		mnac_node.setSpecialValues(new double [] {2, 2, 1.5, 1.5, 4, 4});

		/** Metal-1-Polysilicon-1-Con */
		PrimitiveNode mpc_node = PrimitiveNode.newInstance("Metal-1-Polysilicon-1-Con", this, 5, 5, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(P_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_12),
				new Technology.NodeLayer(M_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_6),
				new Technology.NodeLayer(PC_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_7)
			});
		mpc_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, mpc_node, new ArcProto [] {Polysilicon_1_arc, Metal_1_arc}, "metal-1-polysilicon-1", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(2), EdgeV.fromBottom(2), EdgeH.fromRight(2), EdgeV.fromTop(2))
			});
		mpc_node.setFunction(PrimitiveNode.Function.CONTACT);
		mpc_node.setSpecialType(PrimitiveNode.MULTICUT);
		mpc_node.setSpecialValues(new double [] {2, 2, 1.5, 1.5, 4, 4});

		/** Metal-1-Polysilicon-2-Con */
		PrimitiveNode mpc0_node = PrimitiveNode.newInstance("Metal-1-Polysilicon-2-Con", this, 4, 4, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(M_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_12),
				new Technology.NodeLayer(P0_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_6),
				new Technology.NodeLayer(PC_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_7)
			});
		mpc0_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, mpc0_node, new ArcProto [] {Polysilicon_2_arc, Metal_1_arc}, "metal-1-polysilicon-2", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
			});
		mpc0_node.setFunction(PrimitiveNode.Function.CONTACT);
		mpc0_node.setSpecialType(PrimitiveNode.MULTICUT);
		mpc0_node.setSpecialValues(new double [] {2, 2, 1, 1, 4, 4});

		/** Metal-1-Polysilicon-1-2-Con */
		PrimitiveNode mpc1_node = PrimitiveNode.newInstance("Metal-1-Polysilicon-1-2-Con", this, 7, 7, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(P_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_12),
				new Technology.NodeLayer(P0_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_5),
				new Technology.NodeLayer(PC_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_7)
			});
		mpc1_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, mpc1_node, new ArcProto [] {Polysilicon_1_arc, Polysilicon_2_arc, Metal_1_arc}, "metal-1-polysilicon-1-2", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		mpc1_node.setFunction(PrimitiveNode.Function.CONTACT);
		mpc1_node.setSpecialType(PrimitiveNode.MULTICUT);
		mpc1_node.setSpecialValues(new double [] {2, 2, 2.5, 2.5, 4, 4});

		/** P-Transistor */
		PrimitiveNode pt_node = PrimitiveNode.newInstance("P-Transistor", this, 15, 20, new SizeOffset(6, 6, 9, 9),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(P_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_4, 1, 1, 2.5, 2.5),
				new Technology.NodeLayer(PA_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_10, 4, 4, 0, 0),
				new Technology.NodeLayer(PS_lay, -1, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_9, 6, 6, 2, 2),
				new Technology.NodeLayer(NW_lay, -1, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_12, 10, 10, 6, 6)
			});
		pt_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, pt_node, new ArcProto [] {Polysilicon_1_arc}, "p-trans-poly-left", 180,90, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(4), EdgeV.fromBottom(10), EdgeH.fromLeft(4), EdgeV.fromTop(10)),
				PrimitivePort.newInstance(this, pt_node, new ArcProto [] {P_Active_arc}, "p-trans-diff-top", 90,90, 1, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(7.5), EdgeV.fromTop(6.5), EdgeH.fromRight(7.5), EdgeV.fromTop(6)),
				PrimitivePort.newInstance(this, pt_node, new ArcProto [] {Polysilicon_1_arc}, "p-trans-poly-right", 0,90, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromRight(4), EdgeV.fromBottom(10), EdgeH.fromRight(4), EdgeV.fromTop(10)),
				PrimitivePort.newInstance(this, pt_node, new ArcProto [] {P_Active_arc}, "p-trans-diff-bottom", 270,90, 3, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(7.5), EdgeV.fromBottom(6), EdgeH.fromRight(7.5), EdgeV.fromBottom(6.5))
			});
		pt_node.setFunction(PrimitiveNode.Function.TRAPMOS);
		pt_node.setHoldsOutline();
		pt_node.setCanShrink();
		pt_node.setSpecialType(PrimitiveNode.SERPTRANS);
		pt_node.setSpecialValues(new double [] {0.0416667, 1.5, 2.5, 2, 1, 2});

		/** N-Transistor */
		PrimitiveNode nt_node = PrimitiveNode.newInstance("N-Transistor", this, 15, 20, new SizeOffset(6, 6, 9, 9),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(P_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_4, 1, 1, 2.5, 2.5),
				new Technology.NodeLayer(NA_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_10, 4, 4, 0, 0),
				new Technology.NodeLayer(NS_lay, -1, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_9, 6, 6, 2, 2),
				new Technology.NodeLayer(PW_lay, -1, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_12, 10, 10, 6, 6)
			});
		nt_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, nt_node, new ArcProto [] {Polysilicon_1_arc}, "n-trans-poly-left", 180,90, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(4), EdgeV.fromBottom(10), EdgeH.fromLeft(4), EdgeV.fromTop(10)),
				PrimitivePort.newInstance(this, nt_node, new ArcProto [] {N_Active_arc}, "n-trans-diff-top", 90,90, 1, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(7.5), EdgeV.fromTop(6.5), EdgeH.fromRight(7.5), EdgeV.fromTop(6)),
				PrimitivePort.newInstance(this, nt_node, new ArcProto [] {Polysilicon_1_arc}, "n-trans-poly-right", 0,90, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromRight(4), EdgeV.fromBottom(10), EdgeH.fromRight(4), EdgeV.fromTop(10)),
				PrimitivePort.newInstance(this, nt_node, new ArcProto [] {N_Active_arc}, "n-trans-diff-bottom", 270,90, 3, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(7.5), EdgeV.fromBottom(6), EdgeH.fromRight(7.5), EdgeV.fromBottom(6.5))
			});
		nt_node.setFunction(PrimitiveNode.Function.TRANMOS);
		nt_node.setHoldsOutline();
		nt_node.setCanShrink();
		nt_node.setSpecialType(PrimitiveNode.SERPTRANS);
		nt_node.setSpecialValues(new double [] {0.0416667, 1.5, 2.5, 2, 1, 2});

		/** Metal-1-Metal-2-Con */
		PrimitiveNode mmc_node = PrimitiveNode.newInstance("Metal-1-Metal-2-Con", this, 4, 4, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(M_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_12),
				new Technology.NodeLayer(M0_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_12),
				new Technology.NodeLayer(V_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_7)
			});
		mmc_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, mmc_node, new ArcProto [] {Metal_1_arc, Metal_2_arc}, "metal-1-metal-2", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
			});
		mmc_node.setFunction(PrimitiveNode.Function.CONTACT);
		mmc_node.setSpecialType(PrimitiveNode.MULTICUT);
		mmc_node.setSpecialValues(new double [] {2, 2, 1, 1, 4, 4});

		/** Metal-2-Metal-3-Con */
		PrimitiveNode mmc0_node = PrimitiveNode.newInstance("Metal-2-Metal-3-Con", this, 4, 4, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(M0_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_12),
				new Technology.NodeLayer(M1_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_12),
				new Technology.NodeLayer(V0_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_7)
			});
		mmc0_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, mmc0_node, new ArcProto [] {Metal_2_arc, Metal_3_arc}, "metal-2-metal-3", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
			});
		mmc0_node.setFunction(PrimitiveNode.Function.CONTACT);
		mmc0_node.setSpecialType(PrimitiveNode.MULTICUT);
		mmc0_node.setSpecialValues(new double [] {2, 2, 1, 1, 4, 4});

		/** Metal-3-Metal-4-Con */
		PrimitiveNode mmc1_node = PrimitiveNode.newInstance("Metal-3-Metal-4-Con", this, 4, 4, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(M1_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_12),
				new Technology.NodeLayer(M2_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_12),
				new Technology.NodeLayer(V1_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_7)
			});
		mmc1_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, mmc1_node, new ArcProto [] {Metal_3_arc, Metal_4_arc}, "metal-3-metal-4", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
			});
		mmc1_node.setFunction(PrimitiveNode.Function.CONTACT);
		mmc1_node.setSpecialType(PrimitiveNode.MULTICUT);
		mmc1_node.setSpecialValues(new double [] {2, 2, 1, 1, 4, 4});

		/** Metal-4-Metal-5-Con */
		PrimitiveNode mmc2_node = PrimitiveNode.newInstance("Metal-4-Metal-5-Con", this, 4, 4, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(M2_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_12),
				new Technology.NodeLayer(M3_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_12),
				new Technology.NodeLayer(V2_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_7)
			});
		mmc2_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, mmc2_node, new ArcProto [] {Metal_4_arc, Metal_5_arc}, "metal-4-metal-5", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
			});
		mmc2_node.setFunction(PrimitiveNode.Function.CONTACT);
		mmc2_node.setSpecialType(PrimitiveNode.MULTICUT);
		mmc2_node.setSpecialValues(new double [] {2, 2, 1, 1, 4, 4});

		/** Metal-5-Metal-6-Con */
		PrimitiveNode mmc3_node = PrimitiveNode.newInstance("Metal-5-Metal-6-Con", this, 6, 6, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(M4_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_12),
				new Technology.NodeLayer(M3_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_11),
				new Technology.NodeLayer(V3_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_7)
			});
		mmc3_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, mmc3_node, new ArcProto [] {Metal_5_arc, Metal_6_arc}, "metal-5-metal-6", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(2.5), EdgeV.fromBottom(2.5), EdgeH.fromRight(2.5), EdgeV.fromTop(2.5))
			});
		mmc3_node.setFunction(PrimitiveNode.Function.CONTACT);
		mmc3_node.setSpecialType(PrimitiveNode.MULTICUT);
		mmc3_node.setSpecialValues(new double [] {2, 2, 2, 2, 4, 4});

		/** Metal-1-Well-Con */
		PrimitiveNode mwc_node = PrimitiveNode.newInstance("Metal-1-Well-Con", this, 14, 14, new SizeOffset(4, 4, 4, 4),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(PAW_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_9),
				new Technology.NodeLayer(M_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.MINBOX, box_1),
				new Technology.NodeLayer(PW_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_12),
				new Technology.NodeLayer(PS_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_5),
				new Technology.NodeLayer(AC_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_7)
			});
		mwc_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, mwc_node, new ArcProto [] {Metal_1_arc, Active_arc}, "metal-1-well", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(6.5), EdgeV.fromBottom(6.5), EdgeH.fromRight(6.5), EdgeV.fromTop(6.5))
			});
		mwc_node.setFunction(PrimitiveNode.Function.WELL);
		mwc_node.setSpecialType(PrimitiveNode.MULTICUT);
		mwc_node.setSpecialValues(new double [] {2, 2, 2, 2, 4, 4});

		/** Metal-1-Substrate-Con */
		PrimitiveNode msc_node = PrimitiveNode.newInstance("Metal-1-Substrate-Con", this, 14, 14, new SizeOffset(4, 4, 4, 4),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(NA_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_9),
				new Technology.NodeLayer(M_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.MINBOX, box_1),
				new Technology.NodeLayer(NW_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_12),
				new Technology.NodeLayer(NS_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_5),
				new Technology.NodeLayer(AC_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_7)
			});
		msc_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, msc_node, new ArcProto [] {Metal_1_arc, Active_arc}, "metal-1-substrate", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(6.5), EdgeV.fromBottom(6.5), EdgeH.fromRight(6.5), EdgeV.fromTop(6.5))
			});
		msc_node.setFunction(PrimitiveNode.Function.SUBSTRATE);
		msc_node.setSpecialType(PrimitiveNode.MULTICUT);
		msc_node.setSpecialValues(new double [] {2, 2, 2, 2, 4, 4});

		/** Metal-1-Node */
		PrimitiveNode mn_node = PrimitiveNode.newInstance("Metal-1-Node", this, 3, 3, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(M_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_12)
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
		PrimitiveNode mn0_node = PrimitiveNode.newInstance("Metal-2-Node", this, 3, 3, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(M0_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_12)
			});
		mn0_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, mn0_node, new ArcProto [] {Metal_2_arc}, "metal-2", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		mn0_node.setFunction(PrimitiveNode.Function.NODE);
		mn0_node.setHoldsOutline();
		mn0_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Metal-3-Node */
		PrimitiveNode mn1_node = PrimitiveNode.newInstance("Metal-3-Node", this, 3, 3, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(M1_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_12)
			});
		mn1_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, mn1_node, new ArcProto [] {Metal_3_arc}, "metal-3", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		mn1_node.setFunction(PrimitiveNode.Function.NODE);
		mn1_node.setHoldsOutline();
		mn1_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Metal-4-Node */
		PrimitiveNode mn2_node = PrimitiveNode.newInstance("Metal-4-Node", this, 3, 3, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(M2_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_12)
			});
		mn2_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, mn2_node, new ArcProto [] {Metal_4_arc}, "metal-4", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		mn2_node.setFunction(PrimitiveNode.Function.NODE);
		mn2_node.setHoldsOutline();
		mn2_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Metal-5-Node */
		PrimitiveNode mn3_node = PrimitiveNode.newInstance("Metal-5-Node", this, 3, 3, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(M3_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_12)
			});
		mn3_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, mn3_node, new ArcProto [] {Metal_5_arc}, "metal-5", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		mn3_node.setFunction(PrimitiveNode.Function.NODE);
		mn3_node.setHoldsOutline();
		mn3_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Metal-6-Node */
		PrimitiveNode mn4_node = PrimitiveNode.newInstance("Metal-6-Node", this, 3, 3, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(M4_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_12)
			});
		mn4_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, mn4_node, new ArcProto [] {Metal_6_arc}, "metal-6", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		mn4_node.setFunction(PrimitiveNode.Function.NODE);
		mn4_node.setHoldsOutline();
		mn4_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Polysilicon-1-Node */
		PrimitiveNode pn_node = PrimitiveNode.newInstance("Polysilicon-1-Node", this, 2, 2, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(P_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_12)
			});
		pn_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, pn_node, new ArcProto [] {Polysilicon_1_arc}, "polysilicon-1", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		pn_node.setFunction(PrimitiveNode.Function.NODE);
		pn_node.setHoldsOutline();
		pn_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Polysilicon-2-Node */
		PrimitiveNode pn0_node = PrimitiveNode.newInstance("Polysilicon-2-Node", this, 3, 3, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(P0_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_12)
			});
		pn0_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, pn0_node, new ArcProto [] {Polysilicon_2_arc}, "polysilicon-2", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		pn0_node.setFunction(PrimitiveNode.Function.NODE);
		pn0_node.setHoldsOutline();
		pn0_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Active-Node */
		PrimitiveNode an_node = PrimitiveNode.newInstance("Active-Node", this, 3, 3, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(PA_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_12)
			});
		an_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, an_node, new ArcProto [] {Active_arc, P_Active_arc, N_Active_arc}, "active", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		an_node.setFunction(PrimitiveNode.Function.NODE);
		an_node.setHoldsOutline();
		an_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** N-Active-Node */
		PrimitiveNode nan_node = PrimitiveNode.newInstance("N-Active-Node", this, 3, 3, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(NA_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_12)
			});
		nan_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, nan_node, new ArcProto [] {Active_arc, P_Active_arc, N_Active_arc}, "active", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		nan_node.setFunction(PrimitiveNode.Function.NODE);
		nan_node.setHoldsOutline();
		nan_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** P-Select-Node */
		PrimitiveNode psn_node = PrimitiveNode.newInstance("P-Select-Node", this, 4, 4, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(PS_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_12)
			});
		psn_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, psn_node, new ArcProto [] {}, "select", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		psn_node.setFunction(PrimitiveNode.Function.NODE);
		psn_node.setHoldsOutline();
		psn_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** N-Select-Node */
		PrimitiveNode nsn_node = PrimitiveNode.newInstance("N-Select-Node", this, 4, 4, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(NS_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_12)
			});
		nsn_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, nsn_node, new ArcProto [] {}, "select", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		nsn_node.setFunction(PrimitiveNode.Function.NODE);
		nsn_node.setHoldsOutline();
		nsn_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Poly-Cut-Node */
		PrimitiveNode pcn_node = PrimitiveNode.newInstance("Poly-Cut-Node", this, 2, 2, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(PC_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_12)
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
				new Technology.NodeLayer(AC_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_12)
			});
		acn_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, acn_node, new ArcProto [] {}, "activecut", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		acn_node.setFunction(PrimitiveNode.Function.NODE);
		acn_node.setHoldsOutline();
		acn_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Via-1-Node */
		PrimitiveNode vn_node = PrimitiveNode.newInstance("Via-1-Node", this, 2, 2, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(V_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_12)
			});
		vn_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, vn_node, new ArcProto [] {}, "via-1", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		vn_node.setFunction(PrimitiveNode.Function.NODE);
		vn_node.setHoldsOutline();
		vn_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Via-2-Node */
		PrimitiveNode vn0_node = PrimitiveNode.newInstance("Via-2-Node", this, 2, 2, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(V0_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_12)
			});
		vn0_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, vn0_node, new ArcProto [] {}, "via-2", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		vn0_node.setFunction(PrimitiveNode.Function.NODE);
		vn0_node.setHoldsOutline();
		vn0_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Via-3-Node */
		PrimitiveNode vn1_node = PrimitiveNode.newInstance("Via-3-Node", this, 2, 2, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(V1_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_12)
			});
		vn1_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, vn1_node, new ArcProto [] {}, "via-3", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		vn1_node.setFunction(PrimitiveNode.Function.NODE);
		vn1_node.setHoldsOutline();
		vn1_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Via-4-Node */
		PrimitiveNode vn2_node = PrimitiveNode.newInstance("Via-4-Node", this, 2, 2, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(V2_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_12)
			});
		vn2_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, vn2_node, new ArcProto [] {}, "via-4", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		vn2_node.setFunction(PrimitiveNode.Function.NODE);
		vn2_node.setHoldsOutline();
		vn2_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Via-5-Node */
		PrimitiveNode vn3_node = PrimitiveNode.newInstance("Via-5-Node", this, 2, 2, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(V3_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_12)
			});
		vn3_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, vn3_node, new ArcProto [] {}, "via-5", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		vn3_node.setFunction(PrimitiveNode.Function.NODE);
		vn3_node.setHoldsOutline();
		vn3_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** P-Well-Node */
		PrimitiveNode pwn_node = PrimitiveNode.newInstance("P-Well-Node", this, 12, 12, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(PW_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_12)
			});
		pwn_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, pwn_node, new ArcProto [] {P_Active_arc}, "well", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		pwn_node.setFunction(PrimitiveNode.Function.NODE);
		pwn_node.setHoldsOutline();
		pwn_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** N-Well-Node */
		PrimitiveNode nwn_node = PrimitiveNode.newInstance("N-Well-Node", this, 12, 12, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(NW_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_12)
			});
		nwn_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, nwn_node, new ArcProto [] {P_Active_arc}, "well", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		nwn_node.setFunction(PrimitiveNode.Function.NODE);
		nwn_node.setHoldsOutline();
		nwn_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Passivation-Node */
		PrimitiveNode pn1_node = PrimitiveNode.newInstance("Passivation-Node", this, 8, 8, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(P1_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_12)
			});
		pn1_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, pn1_node, new ArcProto [] {}, "passivation", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		pn1_node.setFunction(PrimitiveNode.Function.NODE);
		pn1_node.setHoldsOutline();
		pn1_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Pad-Frame-Node */
		PrimitiveNode pfn_node = PrimitiveNode.newInstance("Pad-Frame-Node", this, 8, 8, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(PF_lay, 0, Poly.Type.CLOSED, Technology.NodeLayer.BOX, box_12)
			});
		pfn_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, pfn_node, new ArcProto [] {}, "pad-frame", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		pfn_node.setFunction(PrimitiveNode.Function.NODE);
		pfn_node.setHoldsOutline();
		pfn_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Poly-Cap-Node */
		PrimitiveNode pcn0_node = PrimitiveNode.newInstance("Poly-Cap-Node", this, 8, 8, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(PC0_lay, 0, Poly.Type.CLOSED, Technology.NodeLayer.BOX, box_12)
			});
		pcn0_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, pcn0_node, new ArcProto [] {}, "poly-cap", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		pcn0_node.setFunction(PrimitiveNode.Function.NODE);
		pcn0_node.setHoldsOutline();
		pcn0_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** P-Active-Well-Node */
		PrimitiveNode pawn_node = PrimitiveNode.newInstance("P-Active-Well-Node", this, 8, 8, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(PAW_lay, 0, Poly.Type.CLOSED, Technology.NodeLayer.BOX, box_12)
			});
		pawn_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, pawn_node, new ArcProto [] {}, "p-active-well", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		pawn_node.setFunction(PrimitiveNode.Function.NODE);
		pawn_node.setHoldsOutline();
		pawn_node.setSpecialType(PrimitiveNode.POLYGONAL);

		// The pure layer nodes
		M_lay.setPureLayerNode(mn_node);		// Metal-1
		M0_lay.setPureLayerNode(mn0_node);		// Metal-2
		M1_lay.setPureLayerNode(mn1_node);		// Metal-3
		M2_lay.setPureLayerNode(mn2_node);		// Metal-4
		M3_lay.setPureLayerNode(mn3_node);		// Metal-5
		M4_lay.setPureLayerNode(mn4_node);		// Metal-6
		P_lay.setPureLayerNode(pn_node);		// Polysilicon-1
		P0_lay.setPureLayerNode(pn0_node);		// Polysilicon-2
		PA_lay.setPureLayerNode(an_node);		// P-Active
		NA_lay.setPureLayerNode(nan_node);		// N-Active
		PS_lay.setPureLayerNode(psn_node);		// P-Select
		NS_lay.setPureLayerNode(nsn_node);		// N-Select
		PW_lay.setPureLayerNode(pwn_node);		// P-Well
		NW_lay.setPureLayerNode(nwn_node);		// N-Well
		PC_lay.setPureLayerNode(pcn_node);		// Poly-Cut
		AC_lay.setPureLayerNode(acn_node);		// Active-Cut
		V_lay.setPureLayerNode(vn_node);		// Via1
		V0_lay.setPureLayerNode(vn0_node);		// Via2
		V1_lay.setPureLayerNode(vn1_node);		// Via3
		V2_lay.setPureLayerNode(vn2_node);		// Via4
		V3_lay.setPureLayerNode(vn3_node);		// Via5
		P1_lay.setPureLayerNode(pn1_node);		// Passivation
		PC0_lay.setPureLayerNode(pcn0_node);		// Poly-Cap
		PAW_lay.setPureLayerNode(pawn_node);		// P-Active-Well
		PF_lay.setPureLayerNode(pn1_node);		// Pad-Frame

        // Information for palette
        int maxY = 6 /*metal arcs*/ + 3 /* active arcs */ + 1 /* text */ + 2 /* poly*/ + 1 /* trans */;
        nodeGroups = new Object[maxY][3];
        int count = -1;

        nodeGroups[++count][0] = msc_node; nodeGroups[count][1] = pt_node; nodeGroups[count][2] = nt_node;
        nodeGroups[++count][0] = Active_arc; nodeGroups[count][1] = ap_node; nodeGroups[count][2] = mwc_node;
        nodeGroups[++count][0] = P_Active_arc; nodeGroups[count][1] = pap_node; nodeGroups[count][2] = mpac_node;
        nodeGroups[++count][0] = N_Active_arc; nodeGroups[count][1] = nap_node; nodeGroups[count][2] = mnac_node;
        nodeGroups[++count][0] = Polysilicon_1_arc; nodeGroups[count][1] = pp_node; nodeGroups[count][2] = mpc_node;
        nodeGroups[++count][0] = Polysilicon_2_arc; nodeGroups[count][1] = pp0_node; nodeGroups[count][2] = mpc0_node;
        nodeGroups[++count][0] = Metal_1_arc; nodeGroups[count][1] = mp_node;  nodeGroups[count][2] = mmc_node;
        nodeGroups[++count][0] = Metal_2_arc; nodeGroups[count][1] = mp0_node; nodeGroups[count][2] = mmc0_node;
        nodeGroups[++count][0] = Metal_3_arc; nodeGroups[count][1] = mp1_node; nodeGroups[count][2] = mmc1_node;
        nodeGroups[++count][0] = Metal_4_arc; nodeGroups[count][1] = mp2_node; nodeGroups[count][2] = mmc2_node;
        nodeGroups[++count][0] = Metal_5_arc; nodeGroups[count][1] = mp3_node; nodeGroups[count][2] = mmc3_node;
        nodeGroups[++count][0] = Metal_6_arc; nodeGroups[count][1] = mp4_node;
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
