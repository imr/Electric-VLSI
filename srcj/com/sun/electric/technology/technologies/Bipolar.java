/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Bipolar.java
 * bipolar technology description
 * Generated automatically from C Electric
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
 * This is the Bipolar (self-aligned, single poly) Technology.
 */
public class Bipolar extends Technology
{
	/** the Bipolar (self-aligned, single poly) Technology object. */	public static final Bipolar tech = new Bipolar();
	private static final double XX = -1;
	private double [] unConDist;

	// -------------------- private and protected methods ------------------------
	private Bipolar()
	{
		super("bipolar");
		setTechDesc("Bipolar (self-aligned, single poly)");
		setFactoryScale(2000, true);   // in nanometers: really 2 microns
		setNoNegatedArcs();
		setStaticTechnology();

        //Foundry
        Foundry mosis = new Foundry(Foundry.Type.MOSIS);
        foundries.add(mosis);

		setFactoryTransparentLayers(new Color []
		{
			new Color(255,  0,  0), // layer 1
			new Color( 50, 50,200), // layer 2
			new Color(115,255, 82), // layer 3
			new Color( 96,213,255), // layer 4
			new Color(205,205,205), // layer 5
		});

		//**************************************** LAYERS ****************************************

		/** M layer */
		Layer M_lay = Layer.newInstance(this, "Metal1",
			new EGraphics(false, false, null, EGraphics.TRANSPARENT_1, 255,  0,  0,/*115,255,82,*/ 0.8,true,
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

		/** M0 layer */
		Layer M0_lay = Layer.newInstance(this, "Metal2",
			new EGraphics(false, false, null, EGraphics.TRANSPARENT_4,  96,213,255,/*0,0,0,*/ 0.8,true,
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

		/** N layer */
		Layer N_lay = Layer.newInstance(this, "NPImplant",
			new EGraphics(true, true, null, 0, 89,159,85, 0.8,true,
			new int[] { 0xcccc,   // XX  XX  XX  XX  
						0xc0c0,   // XX      XX      
						0xcccc,   // XX  XX  XX  XX  
						0xc0c0,   // XX      XX      
						0xcccc,   // XX  XX  XX  XX  
						0xc0c0,   // XX      XX      
						0xcccc,   // XX  XX  XX  XX  
						0xc0c0,   // XX      XX      
						0xcccc,   // XX  XX  XX  XX  
						0xc0c0,   // XX      XX      
						0xcccc,   // XX  XX  XX  XX  
						0xc0c0,   // XX      XX      
						0xcccc,   // XX  XX  XX  XX  
						0xc0c0,   // XX      XX      
						0xcccc,   // XX  XX  XX  XX  
						0xc0c0}));// XX      XX      

		/** P layer */
		Layer P_lay = Layer.newInstance(this, "PPImplant",
			new EGraphics(true, true, null, 0, 2,15,159, 0.8,true,
			new int[] { 0x0000,   //                 
						0xcccc,   // XX  XX  XX  XX  
						0x0000,   //                 
						0xcccc,   // XX  XX  XX  XX  
						0x0000,   //                 
						0xcccc,   // XX  XX  XX  XX  
						0x0000,   //                 
						0xcccc,   // XX  XX  XX  XX  
						0x0000,   //                 
						0xcccc,   // XX  XX  XX  XX  
						0x0000,   //                 
						0xcccc,   // XX  XX  XX  XX  
						0x0000,   //                 
						0xcccc,   // XX  XX  XX  XX  
						0x0000,   //                 
						0xcccc}));// XX  XX  XX  XX  

		/** PD layer */
		Layer PD_lay = Layer.newInstance(this, "Poly_Definition",
			new EGraphics(false, false, null, EGraphics.TRANSPARENT_2,  50, 50,200,/*96,213,255,*/ 0.8,true,
			new int[] { 0x1111,   //    X   X   X   X
						0x3030,   //   XX      XX    
						0x7171,   //  XXX   X XXX   X
						0x3030,   //   XX      XX    
						0x1111,   //    X   X   X   X
						0x0303,   //       XX      XX
						0x1717,   //    X XXX   X XXX
						0x0303,   //       XX      XX
						0x1111,   //    X   X   X   X
						0x3030,   //   XX      XX    
						0x7171,   //  XXX   X XXX   X
						0x3030,   //   XX      XX    
						0x1111,   //    X   X   X   X
						0x0303,   //       XX      XX
						0x1717,   //    X XXX   X XXX
						0x0303}));//       XX      XX

		/** FI layer */
		Layer FI_lay = Layer.newInstance(this, "Field_Implant",
			new EGraphics(true, true, null, 0, 255,0,255, 0.8,true,
			new int[] { 0x0000,   //                 
						0x4141,   //  X     X X     X
						0x2222,   //   X   X   X   X 
						0x1414,   //    X X     X X  
						0x0000,   //                 
						0x1414,   //    X X     X X  
						0x2222,   //   X   X   X   X 
						0x4141,   //  X     X X     X
						0x0000,   //                 
						0x4141,   //  X     X X     X
						0x2222,   //   X   X   X   X 
						0x1414,   //    X X     X X  
						0x0000,   //                 
						0x1414,   //    X X     X X  
						0x2222,   //   X   X   X   X 
						0x4141}));//  X     X X     X

		/** I layer */
		Layer I_lay = Layer.newInstance(this, "Isolation",
			new EGraphics(false, false, null, EGraphics.TRANSPARENT_3, 115,255, 82,/*205,205,205,*/ 0.8,true,
			new int[] { 0x5555,   //  X X X X X X X X
						0xaaaa,   // X X X X X X X X 
						0x5555,   //  X X X X X X X X
						0xaaaa,   // X X X X X X X X 
						0x5555,   //  X X X X X X X X
						0xaaaa,   // X X X X X X X X 
						0x5555,   //  X X X X X X X X
						0xaaaa,   // X X X X X X X X 
						0x5555,   //  X X X X X X X X
						0xaaaa,   // X X X X X X X X 
						0x5555,   //  X X X X X X X X
						0xaaaa,   // X X X X X X X X 
						0x5555,   //  X X X X X X X X
						0xaaaa,   // X X X X X X X X 
						0x5555,   //  X X X X X X X X
						0xaaaa}));// X X X X X X X X 

		/** SI layer */
		Layer SI_lay = Layer.newInstance(this, "Sink_Implant",
			new EGraphics(true, true, null, 0, 186,0,255, 0.8,true,
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

		/** NI layer */
		Layer NI_lay = Layer.newInstance(this, "N_Implant",
			new EGraphics(true, true, null, 0, 139,99,46, 0.8,true,
			new int[] { 0x1c1c,   //    XXX     XXX  
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
						0xc1c1,   // XX     XXX     X
						0xe0e0,   // XXX     XXX     
						0x7070,   //  XXX     XXX    
						0x3838}));//   XXX     XXX   

		/** SE layer */
		Layer SE_lay = Layer.newInstance(this, "Silicide_Exclusion",
			new EGraphics(false, false, null, EGraphics.TRANSPARENT_5, 205,205,205,/*0,0,0,*/ 0.8,true,
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

		/** C layer */
		Layer C_lay = Layer.newInstance(this, "Contact",
			new EGraphics(false, false, null, 0, 255,255,0, 0.8,true,
			new int[] { 0xffff,   // XXXXXXXXXXXXXXXX
						0xffff,   // XXXXXXXXXXXXXXXX
						0xffff,   // XXXXXXXXXXXXXXXX
						0xffff,   // XXXXXXXXXXXXXXXX
						0xffff,   // XXXXXXXXXXXXXXXX
						0xffff,   // XXXXXXXXXXXXXXXX
						0xffff,   // XXXXXXXXXXXXXXXX
						0xffff,   // XXXXXXXXXXXXXXXX
						0xffff,   // XXXXXXXXXXXXXXXX
						0xffff,   // XXXXXXXXXXXXXXXX
						0xffff,   // XXXXXXXXXXXXXXXX
						0xffff,   // XXXXXXXXXXXXXXXX
						0xffff,   // XXXXXXXXXXXXXXXX
						0xffff,   // XXXXXXXXXXXXXXXX
						0xffff,   // XXXXXXXXXXXXXXXX
						0xffff}));// XXXXXXXXXXXXXXXX

		/** V layer */
		Layer V_lay = Layer.newInstance(this, "Via",
			new EGraphics(false, false, null, 0, 0,0,0, 0.8,true,
			new int[] { 0xffff,   // XXXXXXXXXXXXXXXX
						0xffff,   // XXXXXXXXXXXXXXXX
						0xffff,   // XXXXXXXXXXXXXXXX
						0xffff,   // XXXXXXXXXXXXXXXX
						0xffff,   // XXXXXXXXXXXXXXXX
						0xffff,   // XXXXXXXXXXXXXXXX
						0xffff,   // XXXXXXXXXXXXXXXX
						0xffff,   // XXXXXXXXXXXXXXXX
						0xffff,   // XXXXXXXXXXXXXXXX
						0xffff,   // XXXXXXXXXXXXXXXX
						0xffff,   // XXXXXXXXXXXXXXXX
						0xffff,   // XXXXXXXXXXXXXXXX
						0xffff,   // XXXXXXXXXXXXXXXX
						0xffff,   // XXXXXXXXXXXXXXXX
						0xffff,   // XXXXXXXXXXXXXXXX
						0xffff}));// XXXXXXXXXXXXXXXX

		/** SP layer */
		Layer SP_lay = Layer.newInstance(this, "Scratch_Protection",
			new EGraphics(false, false, null, 0, 100,100,100, 0.8,true,
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

		/** B layer */
		Layer B_lay = Layer.newInstance(this, "Buried",
			new EGraphics(false, false, null, 0, 255,255,0, 0.8,true,
			new int[] { 0xffff,   // XXXXXXXXXXXXXXXX
						0xffff,   // XXXXXXXXXXXXXXXX
						0xffff,   // XXXXXXXXXXXXXXXX
						0xffff,   // XXXXXXXXXXXXXXXX
						0xffff,   // XXXXXXXXXXXXXXXX
						0xffff,   // XXXXXXXXXXXXXXXX
						0xffff,   // XXXXXXXXXXXXXXXX
						0xffff,   // XXXXXXXXXXXXXXXX
						0xffff,   // XXXXXXXXXXXXXXXX
						0xffff,   // XXXXXXXXXXXXXXXX
						0xffff,   // XXXXXXXXXXXXXXXX
						0xffff,   // XXXXXXXXXXXXXXXX
						0xffff,   // XXXXXXXXXXXXXXXX
						0xffff,   // XXXXXXXXXXXXXXXX
						0xffff,   // XXXXXXXXXXXXXXXX
						0xffff}));// XXXXXXXXXXXXXXXX

		/** PM layer */
		Layer PM_lay = Layer.newInstance(this, "Pseudo_Metal1",
			new EGraphics(false, false, null, EGraphics.TRANSPARENT_1, 255,  0,  0,/*115,255,82,*/ 0.8,true,
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
		Layer PM0_lay = Layer.newInstance(this, "Pseudo_Metal2",
			new EGraphics(false, false, null, EGraphics.TRANSPARENT_4,  96,213,255,/*0,0,0,*/ 0.8,true,
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

		// The layer functions
		M_lay.setFunction(Layer.Function.METAL1);		// Metal1
		M0_lay.setFunction(Layer.Function.METAL2);		// Metal2
		N_lay.setFunction(Layer.Function.IMPLANTN);		// NPImplant
		P_lay.setFunction(Layer.Function.IMPLANTP);		// PPImplant
		PD_lay.setFunction(Layer.Function.POLY1);		// Poly_Definition
		FI_lay.setFunction(Layer.Function.IMPLANT, Layer.Function.LIGHT);		// Field_Implant
		I_lay.setFunction(Layer.Function.ISOLATION);		// Isolation
		SI_lay.setFunction(Layer.Function.DIFF, Layer.Function.HEAVY);		// Sink_Implant
		NI_lay.setFunction(Layer.Function.IMPLANTN);		// N_Implant
		SE_lay.setFunction(Layer.Function.GUARD);		// Silicide_Exclusion
		C_lay.setFunction(Layer.Function.CONTACT1);		// Contact
		V_lay.setFunction(Layer.Function.CONTACT2);		// Via
		SP_lay.setFunction(Layer.Function.OVERGLASS);		// Scratch_Protection
		B_lay.setFunction(Layer.Function.DIFF);		// Buried
		PM_lay.setFunction(Layer.Function.METAL1, Layer.Function.PSEUDO);		// Pseudo_Metal1
		PM0_lay.setFunction(Layer.Function.METAL2, Layer.Function.PSEUDO);		// Pseudo_Metal2

		// The CIF names
		M_lay.setFactoryCIFLayer("IM1");	// Metal1
		M0_lay.setFactoryCIFLayer("IM2");	// Metal2
		N_lay.setFactoryCIFLayer("INP");	// NPImplant
		P_lay.setFactoryCIFLayer("IPP");	// PPImplant
		PD_lay.setFactoryCIFLayer("IP");	// Poly_Definition
		FI_lay.setFactoryCIFLayer("IF");	// Field_Implant
		I_lay.setFactoryCIFLayer("II");		// Isolation
		SI_lay.setFactoryCIFLayer("IS");	// Sink_Implant
		NI_lay.setFactoryCIFLayer("INM");	// N_Implant
		SE_lay.setFactoryCIFLayer("ISE");	// Silicide_Exclusion
		C_lay.setFactoryCIFLayer("IC");		// Contact
		V_lay.setFactoryCIFLayer("IV");		// Via
		SP_lay.setFactoryCIFLayer("ISP");	// Scratch_Protection
		B_lay.setFactoryCIFLayer("IB");		// Buried
		PM_lay.setFactoryCIFLayer("");		// Pseudo_Metal1
		PM0_lay.setFactoryCIFLayer("");		// Pseudo_Metal2

		// The DXF names
		M_lay.setFactoryDXFLayer("");		// Metal1
		M0_lay.setFactoryDXFLayer("");		// Metal2
		N_lay.setFactoryDXFLayer("");		// NPImplant
		P_lay.setFactoryDXFLayer("");		// PPImplant
		PD_lay.setFactoryDXFLayer("");		// Poly_Definition
		FI_lay.setFactoryDXFLayer("");		// Field_Implant
		I_lay.setFactoryDXFLayer("");		// Isolation
		SI_lay.setFactoryDXFLayer("");		// Sink_Implant
		NI_lay.setFactoryDXFLayer("");		// N_Implant
		SE_lay.setFactoryDXFLayer("");		// Silicide_Exclusion
		C_lay.setFactoryDXFLayer("");		// Contact
		V_lay.setFactoryDXFLayer("");		// Via
		SP_lay.setFactoryDXFLayer("");		// Scratch_Protection
		B_lay.setFactoryDXFLayer("");		// Buried
		PM_lay.setFactoryDXFLayer("");		// Pseudo_Metal1
		PM0_lay.setFactoryDXFLayer("");		// Pseudo_Metal2

		// The GDS names
        mosis.setFactoryGDSLayer(M_lay, "8");		// Metal1
		mosis.setFactoryGDSLayer(M0_lay, "9");		// Metal2
		mosis.setFactoryGDSLayer(N_lay, "52");		// NPImplant
		mosis.setFactoryGDSLayer(P_lay, "53");		// PPImplant
		mosis.setFactoryGDSLayer(PD_lay, "4");		// Poly_Definition
		mosis.setFactoryGDSLayer(FI_lay, "2");		// Field_Implant
		mosis.setFactoryGDSLayer(I_lay, "3");		// Isolation
		mosis.setFactoryGDSLayer(SI_lay, "6");		// Sink_Implant
		mosis.setFactoryGDSLayer(NI_lay, "51");	// N_Implant
		mosis.setFactoryGDSLayer(SE_lay, "45");	// Silicide_Exclusion
		mosis.setFactoryGDSLayer(C_lay, "7");		// Contact
		mosis.setFactoryGDSLayer(V_lay, "81");		// Via
		mosis.setFactoryGDSLayer(SP_lay, "10");	// Scratch_Protection
		mosis.setFactoryGDSLayer(B_lay, "1");		// Buried
		mosis.setFactoryGDSLayer(PM_lay, "18");	// Pseudo_Metal1
		mosis.setFactoryGDSLayer(PM0_lay, "19");	// Pseudo_Metal2

		//******************** DESIGN RULES ********************

		unConDist = new double[] {
			//            M  M  N  P  P  F  I  S  N  S  C  V  S  B  P  P  
			//               0        D  I     I  I  E        P     M  M  
			//                                                         0  
			//                                                            
			//                                                            
			//                                                            
			/* M      */ 2,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,
			/* M0     */    2,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,
			/* N      */       XX,0,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,
			/* P      */          XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,
			/* PD     */             2,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,
			/* FI     */                2,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,
			/* I      */                   XX,XX,XX,XX,0.5,XX,XX,XX,XX,XX,
			/* SI     */                      XX,XX,XX,XX,XX,XX,XX,XX,XX,
			/* NI     */                         XX,XX,XX,XX,XX,XX,XX,XX,
			/* SE     */                            XX,XX,XX,XX,XX,XX,XX,
			/* C      */                               2,1,XX,XX,XX,XX,
			/* V      */                                  2,XX,XX,XX,XX,
			/* SP     */                                     XX,XX,XX,XX,
			/* B      */                                        XX,XX,XX,
			/* PM     */                                           XX,XX,
			/* PM0    */                                              XX
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
		ArcProto Metal_2_arc = ArcProto.newInstance(this, "Metal_2", 4, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(M0_lay, 0, Poly.Type.FILLED)
		});
		Metal_2_arc.setFunction(ArcProto.Function.METAL2);
		Metal_2_arc.setWipable();
		Metal_2_arc.setFactoryFixedAngle(true);
		Metal_2_arc.setFactoryAngleIncrement(90);

		/** NPPoly arc */
		ArcProto NPPoly_arc = ArcProto.newInstance(this, "NPPoly", 4, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(PD_lay, 2, Poly.Type.FILLED),
			new Technology.ArcLayer(N_lay, 0, Poly.Type.FILLED)
		});
		NPPoly_arc.setFunction(ArcProto.Function.POLY1);
		NPPoly_arc.setWidthOffset(0);
		NPPoly_arc.setFactoryFixedAngle(true);
		NPPoly_arc.setFactoryExtended(false);
		NPPoly_arc.setFactoryAngleIncrement(90);

		/** PPPoly arc */
		ArcProto PPPoly_arc = ArcProto.newInstance(this, "PPPoly", 4, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(PD_lay, 2, Poly.Type.FILLED),
			new Technology.ArcLayer(P_lay, 0, Poly.Type.FILLED)
		});
		PPPoly_arc.setFunction(ArcProto.Function.POLY2);
		PPPoly_arc.setWidthOffset(0);
		PPPoly_arc.setFactoryFixedAngle(true);
		PPPoly_arc.setFactoryExtended(false);
		PPPoly_arc.setFactoryAngleIncrement(90);

		//******************** RECTANGLE DESCRIPTIONS ********************

		Technology.TechPoint [] box_1 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge()),
			new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.makeTopEdge()),
		};
		Technology.TechPoint [] box_2 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(1), EdgeV.fromBottom(1)),
			new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.fromTop(1)),
		};
		Technology.TechPoint [] box_3 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.fromBottom(1)),
			new Technology.TechPoint(EdgeH.fromRight(1), EdgeV.fromTop(1)),
		};
		Technology.TechPoint [] box_4 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.makeBottomEdge()),
			new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.makeTopEdge()),
		};
		Technology.TechPoint [] box_5 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromRight(6), EdgeV.fromBottom(3)),
			new Technology.TechPoint(EdgeH.fromRight(3), EdgeV.fromTop(3)),
		};
		Technology.TechPoint [] box_6 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromRight(7), EdgeV.fromBottom(3.5)),
			new Technology.TechPoint(EdgeH.fromRight(3.5), EdgeV.fromTop(3.5)),
		};
		Technology.TechPoint [] box_7 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromRight(7), EdgeV.fromBottom(2)),
			new Technology.TechPoint(EdgeH.fromRight(2), EdgeV.fromTop(2)),
		};
		Technology.TechPoint [] box_8 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromRight(7), EdgeV.makeBottomEdge()),
			new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.makeTopEdge()),
		};
		Technology.TechPoint [] box_9 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(9), EdgeV.makeBottomEdge()),
			new Technology.TechPoint(EdgeH.fromRight(7), EdgeV.makeTopEdge()),
		};
		Technology.TechPoint [] box_10 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(9), EdgeV.fromBottom(2)),
			new Technology.TechPoint(EdgeH.fromRight(7), EdgeV.fromTop(2)),
		};
		Technology.TechPoint [] box_11 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(9), EdgeV.fromBottom(3.5)),
			new Technology.TechPoint(EdgeH.fromRight(7), EdgeV.fromTop(3.5)),
		};
		Technology.TechPoint [] box_12 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(10), EdgeV.fromBottom(3)),
			new Technology.TechPoint(EdgeH.fromRight(8), EdgeV.fromTop(3)),
		};
		Technology.TechPoint [] box_13 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge()),
			new Technology.TechPoint(EdgeH.fromLeft(9), EdgeV.makeTopEdge()),
		};
		Technology.TechPoint [] box_14 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(2), EdgeV.fromBottom(2)),
			new Technology.TechPoint(EdgeH.fromLeft(9), EdgeV.fromTop(2)),
		};
		Technology.TechPoint [] box_15 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(2.5), EdgeV.fromBottom(2.5)),
			new Technology.TechPoint(EdgeH.fromLeft(8.5), EdgeV.fromTop(2.5)),
		};
		Technology.TechPoint [] box_16 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(3.5), EdgeV.fromBottom(3.5)),
			new Technology.TechPoint(EdgeH.fromLeft(7.5), EdgeV.fromTop(3.5)),
		};
		Technology.TechPoint [] box_17 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(3), EdgeV.fromBottom(3)),
			new Technology.TechPoint(EdgeH.fromLeft(8), EdgeV.fromTop(3)),
		};
		Technology.TechPoint [] box_18 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge()),
			new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.fromBottom(2)),
		};
		Technology.TechPoint [] box_19 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.makeLeftEdge(), EdgeV.fromBottom(1.5)),
			new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.makeCenter()),
		};
		Technology.TechPoint [] box_20 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.makeLeftEdge(), EdgeV.makeCenter()),
			new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.fromTop(1.5)),
		};
		Technology.TechPoint [] box_21 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.makeLeftEdge(), EdgeV.fromTop(2)),
			new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.makeTopEdge()),
		};
		Technology.TechPoint [] box_22 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(1), EdgeV.fromBottom(1)),
			new Technology.TechPoint(EdgeH.fromRight(1), EdgeV.makeCenter()),
		};
		Technology.TechPoint [] box_23 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge()),
			new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.makeCenter()),
		};
		Technology.TechPoint [] box_24 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.makeLeftEdge(), EdgeV.makeCenter()),
			new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.makeTopEdge()),
		};
		Technology.TechPoint [] box_25 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(1), EdgeV.makeCenter()),
			new Technology.TechPoint(EdgeH.fromRight(1), EdgeV.fromTop(1)),
		};
		Technology.TechPoint [] box_26 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.makeLeftEdge(), EdgeV.fromBottom(2)),
			new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.makeCenter()),
		};
		Technology.TechPoint [] box_27 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.makeLeftEdge(), EdgeV.makeCenter()),
			new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.fromTop(2)),
		};
		Technology.TechPoint [] box_28 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(0.5), EdgeV.fromBottom(0.5)),
			new Technology.TechPoint(EdgeH.fromRight(0.5), EdgeV.fromTop(0.5)),
		};
		Technology.TechPoint [] box_29 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(1), EdgeV.fromBottom(1)),
			new Technology.TechPoint(EdgeH.fromLeft(3), EdgeV.fromBottom(3)),
		};
		Technology.TechPoint [] box_30 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge()),
			new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.makeTopEdge()),
		};
		Technology.TechPoint [] box_31 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(1), EdgeV.fromBottom(1)),
			new Technology.TechPoint(EdgeH.fromRight(1), EdgeV.fromTop(1)),
		};

		//******************** NODES ********************

		/** Metal1_Pin */
		PrimitiveNode mp_node = PrimitiveNode.newInstance("Metal1_Pin", this, 3, 3, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(PM_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, box_30)
			});
		mp_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, mp_node, new ArcProto [] {Metal_1_arc}, "metal1", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(1), EdgeV.fromBottom(1), EdgeH.fromRight(1), EdgeV.fromTop(1))
			});
		mp_node.setFunction(PrimitiveNode.Function.PIN);
		mp_node.setArcsWipe();
		mp_node.setArcsShrink();

		/** Metal2_Pin */
		PrimitiveNode mp0_node = PrimitiveNode.newInstance("Metal2_Pin", this, 4, 4, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(PM0_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, box_30)
			});
		mp0_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, mp0_node, new ArcProto [] {Metal_2_arc}, "metal2", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(1), EdgeV.fromBottom(1), EdgeH.fromRight(1), EdgeV.fromTop(1))
			});
		mp0_node.setFunction(PrimitiveNode.Function.PIN);
		mp0_node.setArcsWipe();
		mp0_node.setArcsShrink();

		/** NPPoly_pin */
		PrimitiveNode np_node = PrimitiveNode.newInstance("NPPoly_pin", this, 4, 4, new SizeOffset(1, 1, 1, 1),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(PD_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_31),
				new Technology.NodeLayer(N_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_30)
			});
		np_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, np_node, new ArcProto [] {NPPoly_arc}, "p", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(2), EdgeV.fromBottom(2), EdgeH.fromRight(2), EdgeV.fromTop(2))
			});
		np_node.setFunction(PrimitiveNode.Function.PIN);
		np_node.setArcsWipe();
		np_node.setArcsShrink();

		/** PPPoly_pin */
		PrimitiveNode pp_node = PrimitiveNode.newInstance("PPPoly_pin", this, 4, 4, new SizeOffset(1, 1, 1, 1),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(PD_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_31),
				new Technology.NodeLayer(P_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_30)
			});
		pp_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, pp_node, new ArcProto [] {PPPoly_arc}, "p", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(2), EdgeV.fromBottom(2), EdgeH.fromRight(2), EdgeV.fromTop(2))
			});
		pp_node.setFunction(PrimitiveNode.Function.PIN);
		pp_node.setArcsWipe();
		pp_node.setArcsShrink();

		/** Via */
		PrimitiveNode v_node = PrimitiveNode.newInstance("Via", this, 4, 4, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(M_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_28),
				new Technology.NodeLayer(M0_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_30),
				new Technology.NodeLayer(V_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_29)
			});
		v_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, v_node, new ArcProto [] {Metal_1_arc, Metal_2_arc}, "via", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(2), EdgeV.fromBottom(2), EdgeH.fromRight(2), EdgeV.fromTop(2))
			});
		v_node.setFunction(PrimitiveNode.Function.CONNECT);
		v_node.setSpecialType(PrimitiveNode.MULTICUT);
		v_node.setSpecialValues(new double [] {2, 2, 1, 1, 2, 2});

		/** M1_PP_Contact */
		PrimitiveNode mpc_node = PrimitiveNode.newInstance("M1_PP_Contact", this, 6, 6, new SizeOffset(2, 2, 2, 2),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(M_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_31),
				new Technology.NodeLayer(PD_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_31),
				new Technology.NodeLayer(P_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_30),
				new Technology.NodeLayer(C_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_29)
			});
		mpc_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, mpc_node, new ArcProto [] {Metal_1_arc, PPPoly_arc}, "m", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(2.5), EdgeV.fromBottom(2.5), EdgeH.fromRight(2.5), EdgeV.fromTop(2.5))
			});
		mpc_node.setFunction(PrimitiveNode.Function.CONNECT);
		mpc_node.setSpecialType(PrimitiveNode.MULTICUT);
		mpc_node.setSpecialValues(new double [] {2, 2, 0, 0, 2, 2});

		/** M1_NP_Contact */
		PrimitiveNode mnc_node = PrimitiveNode.newInstance("M1_NP_Contact", this, 6, 6, new SizeOffset(2, 2, 2, 2),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(PD_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_31),
				new Technology.NodeLayer(M_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_31),
				new Technology.NodeLayer(N_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_30),
				new Technology.NodeLayer(C_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_29)
			});
		mnc_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, mnc_node, new ArcProto [] {Metal_1_arc, NPPoly_arc}, "m", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(2.5), EdgeV.fromBottom(2.5), EdgeH.fromRight(2.5), EdgeV.fromTop(2.5))
			});
		mnc_node.setFunction(PrimitiveNode.Function.CONNECT);
		mnc_node.setSpecialType(PrimitiveNode.MULTICUT);
		mnc_node.setSpecialValues(new double [] {2, 2, 0, 0, 2, 2});

		/** NPResistor */
		PrimitiveNode n_node = PrimitiveNode.newInstance("NPResistor", this, 5, 7, new SizeOffset(1, 1, 2, 2),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(SE_lay, -1, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_27),
				new Technology.NodeLayer(SE_lay, -1, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_26),
				new Technology.NodeLayer(PD_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_25),
				new Technology.NodeLayer(PD_lay, 1, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_22),
				new Technology.NodeLayer(N_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_24),
				new Technology.NodeLayer(N_lay, 1, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_23)
			});
		n_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, n_node, new ArcProto [] {NPPoly_arc}, "p1", 90,0, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(2), EdgeV.fromTop(1), EdgeH.fromRight(2), EdgeV.fromTop(1)),
				PrimitivePort.newInstance(this, n_node, new ArcProto [] {NPPoly_arc}, "p2", 270,0, 1, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(2), EdgeV.fromBottom(1), EdgeH.fromRight(2), EdgeV.fromBottom(1))
			});
		n_node.setFunction(PrimitiveNode.Function.RESIST);

		/** NMResistor */
		PrimitiveNode n0_node = PrimitiveNode.newInstance("NMResistor", this, 5, 7, new SizeOffset(1, 1, 2, 2),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(SE_lay, -1, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_27),
				new Technology.NodeLayer(PD_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_25),
				new Technology.NodeLayer(SE_lay, -1, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_26),
				new Technology.NodeLayer(PD_lay, 1, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_22),
				new Technology.NodeLayer(N_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_21),
				new Technology.NodeLayer(NI_lay, -1, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_20),
				new Technology.NodeLayer(NI_lay, -1, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_19),
				new Technology.NodeLayer(N_lay, 1, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_18)
			});
		n0_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, n0_node, new ArcProto [] {NPPoly_arc}, "p1", 90,0, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(2), EdgeV.fromTop(1), EdgeH.fromRight(2), EdgeV.fromTop(1)),
				PrimitivePort.newInstance(this, n0_node, new ArcProto [] {NPPoly_arc}, "p2", 270,0, 1, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(2), EdgeV.fromBottom(1), EdgeH.fromRight(2), EdgeV.fromBottom(1))
			});
		n0_node.setFunction(PrimitiveNode.Function.UNKNOWN);

		/** npn111 */
		PrimitiveNode n1_node = PrimitiveNode.newInstance("npn111", this, 20, 11, new SizeOffset(10, 8, 3.5, 3.5),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(PD_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_17),
				new Technology.NodeLayer(I_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_16),
				new Technology.NodeLayer(PD_lay, 1, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_12),
				new Technology.NodeLayer(I_lay, 1, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_11),
				new Technology.NodeLayer(I_lay, 2, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_6),
				new Technology.NodeLayer(PD_lay, 2, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_5),
				new Technology.NodeLayer(SI_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_15),
				new Technology.NodeLayer(N_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_14),
				new Technology.NodeLayer(FI_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_13),
				new Technology.NodeLayer(N_lay, 1, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_10),
				new Technology.NodeLayer(FI_lay, 1, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_9),
				new Technology.NodeLayer(FI_lay, 2, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_8),
				new Technology.NodeLayer(P_lay, 2, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_7)
			});
		n1_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, n1_node, new ArcProto [] {NPPoly_arc}, "c", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(4), EdgeV.fromBottom(4), EdgeH.fromLeft(7), EdgeV.fromTop(4)),
				PrimitivePort.newInstance(this, n1_node, new ArcProto [] {NPPoly_arc}, "e", 0,180, 1, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(11), EdgeV.fromBottom(4), EdgeH.fromRight(9), EdgeV.fromTop(4)),
				PrimitivePort.newInstance(this, n1_node, new ArcProto [] {PPPoly_arc}, "b", 0,180, 2, PortCharacteristic.UNKNOWN,
					EdgeH.fromRight(5), EdgeV.fromBottom(4), EdgeH.fromRight(4), EdgeV.fromTop(4))
			});
		n1_node.setFunction(PrimitiveNode.Function.TRANPN);

		/** PNJunction */
		PrimitiveNode p_node = PrimitiveNode.newInstance("PNJunction", this, 4, 4, new SizeOffset(1, 1, 1, 1),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(PD_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_3),
				new Technology.NodeLayer(PD_lay, 1, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_2),
				new Technology.NodeLayer(P_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_4),
				new Technology.NodeLayer(N_lay, 1, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_1)
			});
		p_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, p_node, new ArcProto [] {PPPoly_arc}, "p", 0,0, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromRight(1), EdgeV.fromBottom(2), EdgeH.fromRight(1), EdgeV.fromTop(2)),
				PrimitivePort.newInstance(this, p_node, new ArcProto [] {NPPoly_arc}, "n", 180,0, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(1), EdgeV.fromBottom(2), EdgeH.fromLeft(1), EdgeV.fromTop(2))
			});
		p_node.setFunction(PrimitiveNode.Function.CONNECT);

		/** Metal1_Node */
		PrimitiveNode mn_node = PrimitiveNode.newInstance("Metal1_Node", this, 2, 2, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(M_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_30)
			});
		mn_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, mn_node, new ArcProto [] {Metal_1_arc}, "metal1", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		mn_node.setFunction(PrimitiveNode.Function.NODE);
		mn_node.setHoldsOutline();
		mn_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Metal2_Node */
		PrimitiveNode mn0_node = PrimitiveNode.newInstance("Metal2_Node", this, 2, 2, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(M0_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_30)
			});
		mn0_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, mn0_node, new ArcProto [] {Metal_2_arc}, "metal2", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		mn0_node.setFunction(PrimitiveNode.Function.NODE);
		mn0_node.setHoldsOutline();
		mn0_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** NPImplant_Node */
		PrimitiveNode nn_node = PrimitiveNode.newInstance("NPImplant_Node", this, 2, 2, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(N_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_30)
			});
		nn_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, nn_node, new ArcProto [] {}, "N+implant", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		nn_node.setFunction(PrimitiveNode.Function.NODE);
		nn_node.setHoldsOutline();
		nn_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** PPImplant_Node */
		PrimitiveNode pn_node = PrimitiveNode.newInstance("PPImplant_Node", this, 2, 2, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(P_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_30)
			});
		pn_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, pn_node, new ArcProto [] {}, "P+implant", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		pn_node.setFunction(PrimitiveNode.Function.NODE);
		pn_node.setHoldsOutline();
		pn_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Poly_Def_Node */
		PrimitiveNode pdn_node = PrimitiveNode.newInstance("Poly_Def_Node", this, 2, 2, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(PD_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_30)
			});
		pdn_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, pdn_node, new ArcProto [] {}, "poly-def", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		pdn_node.setFunction(PrimitiveNode.Function.NODE);
		pdn_node.setHoldsOutline();
		pdn_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Field_Implant_Node */
		PrimitiveNode fin_node = PrimitiveNode.newInstance("Field_Implant_Node", this, 4, 4, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(FI_lay, 0, Poly.Type.CLOSED, Technology.NodeLayer.BOX, box_30)
			});
		fin_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, fin_node, new ArcProto [] {}, "field", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		fin_node.setFunction(PrimitiveNode.Function.NODE);
		fin_node.setHoldsOutline();
		fin_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Isolation_Implant_Node */
		PrimitiveNode iin_node = PrimitiveNode.newInstance("Isolation_Implant_Node", this, 4, 4, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(I_lay, 0, Poly.Type.CLOSED, Technology.NodeLayer.BOX, box_30)
			});
		iin_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, iin_node, new ArcProto [] {}, "isolation", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		iin_node.setFunction(PrimitiveNode.Function.NODE);
		iin_node.setHoldsOutline();
		iin_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Sink_Implant_Node */
		PrimitiveNode sin_node = PrimitiveNode.newInstance("Sink_Implant_Node", this, 4, 4, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(SI_lay, 0, Poly.Type.CLOSED, Technology.NodeLayer.BOX, box_30)
			});
		sin_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, sin_node, new ArcProto [] {}, "sink-implant", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		sin_node.setFunction(PrimitiveNode.Function.NODE);
		sin_node.setHoldsOutline();
		sin_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** N_Implant_Node */
		PrimitiveNode nin_node = PrimitiveNode.newInstance("N_Implant_Node", this, 4, 4, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(NI_lay, 0, Poly.Type.CLOSED, Technology.NodeLayer.BOX, box_30)
			});
		nin_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, nin_node, new ArcProto [] {}, "N-implant", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		nin_node.setFunction(PrimitiveNode.Function.NODE);
		nin_node.setHoldsOutline();
		nin_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Silicode_Exclusion_Node */
		PrimitiveNode sen_node = PrimitiveNode.newInstance("Silicode_Exclusion_Node", this, 4, 4, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(SE_lay, 0, Poly.Type.CLOSED, Technology.NodeLayer.BOX, box_30)
			});
		sen_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, sen_node, new ArcProto [] {}, "silicide-exclusion", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		sen_node.setFunction(PrimitiveNode.Function.NODE);
		sen_node.setHoldsOutline();
		sen_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Contact_Node */
		PrimitiveNode cn_node = PrimitiveNode.newInstance("Contact_Node", this, 4, 4, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(C_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, box_30)
			});
		cn_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, cn_node, new ArcProto [] {}, "contact", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		cn_node.setFunction(PrimitiveNode.Function.NODE);
		cn_node.setHoldsOutline();
		cn_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Via_Node */
		PrimitiveNode vn_node = PrimitiveNode.newInstance("Via_Node", this, 4, 4, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(V_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_30)
			});
		vn_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, vn_node, new ArcProto [] {}, "via", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		vn_node.setFunction(PrimitiveNode.Function.NODE);
		vn_node.setHoldsOutline();
		vn_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Scratch_Protection_Node */
		PrimitiveNode spn_node = PrimitiveNode.newInstance("Scratch_Protection_Node", this, 4, 4, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(SP_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_30)
			});
		spn_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, spn_node, new ArcProto [] {}, "scratch-protection", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		spn_node.setFunction(PrimitiveNode.Function.NODE);
		spn_node.setHoldsOutline();
		spn_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Buried_Node */
		PrimitiveNode bn_node = PrimitiveNode.newInstance("Buried_Node", this, 4, 4, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(B_lay, 0, Poly.Type.CLOSED, Technology.NodeLayer.BOX, box_30)
			});
		bn_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, bn_node, new ArcProto [] {}, "buried", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		bn_node.setFunction(PrimitiveNode.Function.NODE);
		bn_node.setHoldsOutline();
		bn_node.setSpecialType(PrimitiveNode.POLYGONAL);

		// The pure layer nodes
		M_lay.setPureLayerNode(mn_node);		// Metal1
		M0_lay.setPureLayerNode(mn0_node);		// Metal2
		N_lay.setPureLayerNode(nn_node);		// NPImplant
		P_lay.setPureLayerNode(pn_node);		// PPImplant
		PD_lay.setPureLayerNode(pdn_node);		// Poly_Definition
		FI_lay.setPureLayerNode(fin_node);		// Field_Implant
		I_lay.setPureLayerNode(iin_node);		// Isolation
		SI_lay.setPureLayerNode(sin_node);		// Sink_Implant
		NI_lay.setPureLayerNode(nin_node);		// N_Implant
		SE_lay.setPureLayerNode(sen_node);		// Silicide_Exclusion
		C_lay.setPureLayerNode(cn_node);		// Contact
		V_lay.setPureLayerNode(vn_node);		// Via
		SP_lay.setPureLayerNode(spn_node);		// Scratch_Protection
		B_lay.setPureLayerNode(bn_node);		// Buried

        // Information for palette
        int maxY = 2 /*metal arcs*/ + 2 /* active arcs */ + 1 /* text */ + 1 /* trans */;
        nodeGroups = new Object[maxY][3];
        int count = -1;

        nodeGroups[++count][0] = Metal_1_arc; nodeGroups[count][1] = mp_node; nodeGroups[count][2] = n1_node;
        nodeGroups[++count][0] = Metal_2_arc; nodeGroups[count][1] = mp0_node; nodeGroups[count][2] = v_node;
        nodeGroups[++count][0] = PPPoly_arc; nodeGroups[count][1] = pp_node; nodeGroups[count][2] = mpc_node;
        nodeGroups[++count][0] = NPPoly_arc; nodeGroups[count][1] = np_node; nodeGroups[count][2] = mnc_node;
        nodeGroups[++count][0] = n0_node; nodeGroups[count][1] = n_node; nodeGroups[count][2] = p_node;
        nodeGroups[++count][0] = "Pure"; nodeGroups[count][1] = "Misc."; nodeGroups[count][2] = "Cell";
	}

	public DRCRules getFactoryDesignRules(boolean resizeNodes)
	{
		return MOSRules.makeSimpleRules(this, null, unConDist);
	}
}
