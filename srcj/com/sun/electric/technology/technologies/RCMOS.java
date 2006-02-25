/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: RCMOS.java
 * Round CMOS technology description (CalTech rules)
 * Specified by: Dick Lyon, Carver Mead, and Erwin Liu
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

import java.awt.Color;


/**
 * This is the Complementary MOS (old, N-Well, from Griswold) Technology.
 */
public class RCMOS extends Technology
{
	/** the Round CMOS Technology object. */	public static final RCMOS tech = new RCMOS();

	// -------------------- private and protected methods ------------------------
	private RCMOS()
	{
		super("rcmos");
		setTechShortName("Round CMOS");
		setTechDesc("Complementary MOS (round, from MOSIS, P-Well, double metal)");
		setFactoryScale(2000, true);   // in nanometers: really 2 microns
		setNoNegatedArcs();
		setNonStandard();
		setStaticTechnology();

        //Foundry
        Foundry noFoundry = new Foundry(Foundry.Type.NONE);
        foundries.add(noFoundry);
        
		setFactoryTransparentLayers(new Color []
		{
			new Color( 96,209,255), // Metal-1
			new Color(255,155,192), // Polysilicon
			new Color(107,226, 96), // Diffusion
			new Color(240,221,181), // Well
			new Color(224, 95,255)  // Metal-2
		});

		//**************************************** LAYERS ****************************************

		/** Metal-1 layer */
		Layer metal1_lay = Layer.newInstance(this, "Metal-1",
			new EGraphics(false, true, null, EGraphics.TRANSPARENT_1, 96,209,255,0.8,true,
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
		Layer metal2_lay = Layer.newInstance(this, "Metal-2",
			new EGraphics(false, true, null, EGraphics.TRANSPARENT_5, 224,95,255,0.8,true,
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
		Layer polysilicon_lay = Layer.newInstance(this, "Polysilicon",
			new EGraphics(false, true, null, EGraphics.TRANSPARENT_2, 255,155,192,0.8,true,
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

		/** S-Active layer */
		Layer sActive_lay = Layer.newInstance(this, "S-Active",
			new EGraphics(false, true, null, EGraphics.TRANSPARENT_3, 107,226,96,0.8,true,
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

		/** D-Active layer */
		Layer dActive_lay = Layer.newInstance(this, "D-Active",
			new EGraphics(false, true, null, EGraphics.TRANSPARENT_3, 107,226,96,0.8,true,
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

		/** Select layer */
		Layer select_lay = Layer.newInstance(this, "Select",
			new EGraphics(true, true, null, 0, 255,255,0,0.8,true,
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

		/** Well layer */
		Layer well_lay = Layer.newInstance(this, "Well",
			new EGraphics(false, true, null, EGraphics.TRANSPARENT_4, 240,221,181,0.8,true,
			new int[] { 0x0000,   //                 
						0x00c0,   //         XX      
						0x0000,   //                 
						0x0000,   //                 
						0x0000,   //                 
						0x00c0,   //         XX      
						0x0000,   //                 
						0x0000,   //                 
						0x0000,   //                 
						0x00c0,   //         XX      
						0x0000,   //                 
						0x0000,   //                 
						0x0000,   //                 
						0x00c0,   //         XX      
						0x0000,   //                 
						0x0000}));//                 

		/** Cut layer */
		Layer cut_lay = Layer.newInstance(this, "Contact-Cut",
			new EGraphics(false, false, null, 0, 0,0,0,0.8,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** Via layer */
		Layer via_lay = Layer.newInstance(this, "Via",
			new EGraphics(false, false, null, 0, 0,0,0,0.8,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** Passivation layer */
		Layer passivation_lay = Layer.newInstance(this, "Passivation",
			new EGraphics(true, true, null, 0, 100,100,100,0.8,true,
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

		/** Poly Cut layer */
		Layer polyCut_lay = Layer.newInstance(this, "Poly-Cut",
			new EGraphics(false, false, null, 0, 0,0,0,0.8,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** Active Cut layer */
		Layer activeCut_lay = Layer.newInstance(this, "Active-Cut",
			new EGraphics(false, false, null, 0, 0,0,0,0.8,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** Pseudo-Metal-1 layer */
		Layer pseudoMetal1_lay = Layer.newInstance(this, "Pseudo-Metal-1",
			new EGraphics(false, false, null, EGraphics.TRANSPARENT_1, 96,209,255,0.8,true,
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

		/** Pseudo-Metal-2 layer */
		Layer pseudoMetal2_lay = Layer.newInstance(this, "Pseudo-Metal-2",
			new EGraphics(false, false, null, EGraphics.TRANSPARENT_5, 224,95,255,0.8,true,
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

		/** Pseudo-Polysilicon layer */
		Layer pseudoPolysilicon_lay = Layer.newInstance(this, "Pseudo-Polysilicon",
			new EGraphics(false, false, null, EGraphics.TRANSPARENT_2, 255,155,192,0.8,true,
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

		/** Pseudo-S-Active layer */
		Layer pseudoSActive_lay = Layer.newInstance(this, "Pseudo-S-Active",
			new EGraphics(false, false, null, EGraphics.TRANSPARENT_3, 107,226,96,0.8,true,
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

		/** Pseudo-D-Active layer */
		Layer pseudoDActive_lay = Layer.newInstance(this, "Pseudo-D-Active",
			new EGraphics(false, false, null, EGraphics.TRANSPARENT_3, 107,226,96,0.8,true,
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

		/** Pseudo-Select layer */
		Layer pseudoSelect_lay = Layer.newInstance(this, "Pseudo-Select",
			new EGraphics(true, true, null, 0, 255,255,0,0.8,true,
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

		/** Pseudo-Well layer */
		Layer pseudoWell_lay = Layer.newInstance(this, "Pseudo-Well",
			new EGraphics(false, false, null, EGraphics.TRANSPARENT_4, 240,221,181,0.8,true,
			new int[] { 0x0000,   //                 
						0x00c0,   //         XX      
						0x0000,   //                 
						0x0000,   //                 
						0x0000,   //                 
						0x00c0,   //         XX      
						0x0000,   //                 
						0x0000,   //                 
						0x0000,   //                 
						0x00c0,   //         XX      
						0x0000,   //                 
						0x0000,   //                 
						0x0000,   //                 
						0x00c0,   //         XX      
						0x0000,   //                 
						0x0000}));//                 

		// The layer functions
		metal1_lay.setFunction(Layer.Function.METAL1);									// Metal-1
		metal2_lay.setFunction(Layer.Function.METAL2);									// Metal-2
		polysilicon_lay.setFunction(Layer.Function.POLY1);								// Polysilicon
		sActive_lay.setFunction(Layer.Function.DIFF);									// S-Active
		dActive_lay.setFunction(Layer.Function.DIFF);									// D-Active
		select_lay.setFunction(Layer.Function.IMPLANTP);								// Select
		well_lay.setFunction(Layer.Function.WELLP);										// Well
		cut_lay.setFunction(Layer.Function.CONTACT1);									// Contact-Cut
		via_lay.setFunction(Layer.Function.CONTACT2);									// Via
		passivation_lay.setFunction(Layer.Function.OVERGLASS);							// Passivation
		polyCut_lay.setFunction(Layer.Function.CONTACT1);								// Poly-Cut
		activeCut_lay.setFunction(Layer.Function.CONTACT1);								// Active-Cut
		pseudoMetal1_lay.setFunction(Layer.Function.METAL1, Layer.Function.PSEUDO);		// Pseudo-Metal-1
		pseudoMetal2_lay.setFunction(Layer.Function.METAL2, Layer.Function.PSEUDO);		// Pseudo-Metal-2
		pseudoPolysilicon_lay.setFunction(Layer.Function.POLY1, Layer.Function.PSEUDO);	// Pseudo-Polysilicon
		pseudoSActive_lay.setFunction(Layer.Function.DIFF, Layer.Function.PSEUDO);		// Pseudo-S-Active
		pseudoDActive_lay.setFunction(Layer.Function.DIFF, Layer.Function.PSEUDO);		// Pseudo-D-Active
		pseudoSelect_lay.setFunction(Layer.Function.IMPLANTP, Layer.Function.PSEUDO);	// Pseudo-Select
		pseudoWell_lay.setFunction(Layer.Function.WELLP, Layer.Function.PSEUDO);		// Pseudo-Well

		// The CIF names
		metal1_lay.setFactoryCIFLayer("CMF");			// Metal-1
		metal2_lay.setFactoryCIFLayer("CMS");			// Metal-2
		polysilicon_lay.setFactoryCIFLayer("CPG");		// Polysilicon
		sActive_lay.setFactoryCIFLayer("CAA");			// S-Active
		dActive_lay.setFactoryCIFLayer("CAA");			// D-Active
		select_lay.setFactoryCIFLayer("CSG");			// Select
		well_lay.setFactoryCIFLayer("CWG");				// Well
		cut_lay.setFactoryCIFLayer("CC");				// Contact-Cut
		via_lay.setFactoryCIFLayer("CVA");				// Via
		passivation_lay.setFactoryCIFLayer("COG");		// Passivation
		polyCut_lay.setFactoryCIFLayer("CCP");			// Poly-Cut
		activeCut_lay.setFactoryCIFLayer("CCA");		// Active-Cut
		pseudoMetal1_lay.setFactoryCIFLayer("");		// Pseudo-Metal-1
		pseudoMetal2_lay.setFactoryCIFLayer("");		// Pseudo-Metal-2
		pseudoPolysilicon_lay.setFactoryCIFLayer("");	// Pseudo-Polysilicon
		pseudoSActive_lay.setFactoryCIFLayer("");		// Pseudo-S-Active
		pseudoDActive_lay.setFactoryCIFLayer("");		// Pseudo-D-Active
		pseudoSelect_lay.setFactoryCIFLayer("");		// Pseudo-Select
		pseudoWell_lay.setFactoryCIFLayer("");			// Pseudo-Well

		//******************** ARCS ********************

		/** Metal-1 arc */
		ArcProto metal1_arc = ArcProto.newInstance(this, "Metal-1", 3, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(metal1_lay, 0, Poly.Type.FILLED)
		});
		metal1_arc.setFunction(ArcProto.Function.METAL1);
		metal1_arc.setFactoryFixedAngle(false);
		metal1_arc.setCurvable();
		metal1_arc.setExtended(false);
		metal1_arc.setFactoryAngleIncrement(0);

		/** Metal-2 arc */
		ArcProto metal2_arc = ArcProto.newInstance(this, "Metal-2", 3, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(metal2_lay, 0, Poly.Type.FILLED)
		});
		metal2_arc.setFunction(ArcProto.Function.METAL1);
		metal2_arc.setFactoryFixedAngle(false);
		metal2_arc.setCurvable();
		metal2_arc.setExtended(false);
		metal2_arc.setFactoryAngleIncrement(0);

		/** Polysilicon arc */
		ArcProto polysilicon_arc = ArcProto.newInstance(this, "Polysilicon", 2, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(polysilicon_lay, 0, Poly.Type.FILLED)
		});
		polysilicon_arc.setFunction(ArcProto.Function.METAL1);
		polysilicon_arc.setFactoryFixedAngle(false);
		polysilicon_arc.setCurvable();
		polysilicon_arc.setExtended(false);
		polysilicon_arc.setFactoryAngleIncrement(0);

		/** S-Active arc */
		ArcProto sActive_arc = ArcProto.newInstance(this, "S-Active", 6, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(sActive_lay, 4, Poly.Type.FILLED),
			new Technology.ArcLayer(select_lay, 0, Poly.Type.FILLED)
		});
		sActive_arc.setFunction(ArcProto.Function.DIFFN);
		sActive_arc.setFactoryFixedAngle(false);
		sActive_arc.setCurvable();
		sActive_arc.setExtended(false);
		sActive_arc.setFactoryAngleIncrement(0);
		sActive_arc.setWidthOffset(4);

		/** D-Active arc */
		ArcProto dActive_arc = ArcProto.newInstance(this, "D-Active", 10, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(dActive_lay, 8, Poly.Type.FILLED),
			new Technology.ArcLayer(well_lay, 0, Poly.Type.FILLED)
		});
		dActive_arc.setFunction(ArcProto.Function.DIFFP);
		dActive_arc.setFactoryFixedAngle(false);
		dActive_arc.setCurvable();
		dActive_arc.setExtended(false);
		dActive_arc.setFactoryAngleIncrement(0);
		dActive_arc.setWidthOffset(8);

		/** Substrate-Active arc */
		ArcProto substrateActive_arc = ArcProto.newInstance(this, "Substrate-Active", 2, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(dActive_lay, 0, Poly.Type.FILLED),
			new Technology.ArcLayer(sActive_lay, 0, Poly.Type.FILLED)
		});
		substrateActive_arc.setFunction(ArcProto.Function.DIFFS);
		substrateActive_arc.setFactoryFixedAngle(false);
		substrateActive_arc.setCurvable();
		substrateActive_arc.setExtended(false);
		substrateActive_arc.setFactoryAngleIncrement(0);

		/** Well arc */
		ArcProto wellActive_arc = ArcProto.newInstance(this, "Well-Active", 6, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(dActive_lay, 4, Poly.Type.FILLED),
			new Technology.ArcLayer(sActive_lay, 4, Poly.Type.FILLED),
			new Technology.ArcLayer(well_lay, 0, Poly.Type.FILLED),
			new Technology.ArcLayer(select_lay, 0, Poly.Type.FILLED)
		});
		wellActive_arc.setFunction(ArcProto.Function.DIFFW);
		wellActive_arc.setFactoryFixedAngle(false);
		wellActive_arc.setCurvable();
		wellActive_arc.setExtended(false);
		wellActive_arc.setFactoryAngleIncrement(0);
		wellActive_arc.setWidthOffset(4);

		/** S-Transistor arc */
		ArcProto sTransistor_arc = ArcProto.newInstance(this, "S-Transistor", 6, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(dActive_lay, 4, Poly.Type.FILLED),
			new Technology.ArcLayer(polysilicon_lay, 4, Poly.Type.FILLED),
			new Technology.ArcLayer(select_lay, 0, Poly.Type.FILLED)
		});
		sTransistor_arc.setFunction(ArcProto.Function.DIFFN);
		sTransistor_arc.setFactoryFixedAngle(false);
		sTransistor_arc.setCurvable();
		sTransistor_arc.setExtended(false);
		sTransistor_arc.setFactoryAngleIncrement(0);
		sTransistor_arc.setWidthOffset(4);

		/** D-Transistor arc */
		ArcProto dTransistor_arc = ArcProto.newInstance(this, "D-Transistor", 10, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(dActive_lay, 8, Poly.Type.FILLED),
			new Technology.ArcLayer(polysilicon_lay, 8, Poly.Type.FILLED),
			new Technology.ArcLayer(well_lay, 0, Poly.Type.FILLED)
		});
		dTransistor_arc.setFunction(ArcProto.Function.DIFFP);
		dTransistor_arc.setFactoryFixedAngle(false);
		dTransistor_arc.setCurvable();
		dTransistor_arc.setExtended(false);
		dTransistor_arc.setFactoryAngleIncrement(0);
		dTransistor_arc.setWidthOffset(8);

		//******************** NODES ********************

		/** Metal-1-Pin */
		PrimitiveNode metal1Pin_node = PrimitiveNode.newInstance("Metal-1-Pin", this, 3, 3, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(metal1_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.makeCenter()),
					new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.makeCenter())})
			});
		metal1Pin_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, metal1Pin_node, new ArcProto [] {metal1_arc}, "metal-1", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		metal1Pin_node.setFunction(PrimitiveNode.Function.PIN);
		metal1Pin_node.setSquare();

		/** Metal-2-Pin */
		PrimitiveNode metal2Pin_node = PrimitiveNode.newInstance("Metal-2-Pin", this, 3, 3, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(metal2_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.makeCenter()),
					new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.makeCenter())})
			});
		metal2Pin_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, metal2Pin_node, new ArcProto [] {metal2_arc}, "metal-2", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		metal2Pin_node.setFunction(PrimitiveNode.Function.PIN);
		metal2Pin_node.setSquare();

		/** Polysilicon-Pin */
		PrimitiveNode polysiliconPin_node = PrimitiveNode.newInstance("Polysilicon-Pin", this, 2, 2, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(polysilicon_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.makeCenter()),
					new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.makeCenter())})
			});
		polysiliconPin_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, polysiliconPin_node, new ArcProto [] {polysilicon_arc}, "polysilicon", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		polysiliconPin_node.setFunction(PrimitiveNode.Function.PIN);
		polysiliconPin_node.setSquare();

		/** S-Active-Pin */
		PrimitiveNode sActivePin_node = PrimitiveNode.newInstance("S-Active-Pin", this, 6, 6, new SizeOffset(2,2,2,2),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(sActive_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.makeCenter()),
					new Technology.TechPoint(EdgeH.fromRight(2), EdgeV.makeCenter())}),
				new Technology.NodeLayer(select_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.makeCenter()),
					new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.makeCenter())})
			});
		sActivePin_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, sActivePin_node, new ArcProto [] {sActive_arc}, "s-active", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		sActivePin_node.setFunction(PrimitiveNode.Function.PIN);
		sActivePin_node.setSquare();

		/** D-Active-Pin */
		PrimitiveNode dActivePin_node = PrimitiveNode.newInstance("D-Active-Pin", this, 10, 10, new SizeOffset(4,4,4,4),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(dActive_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.makeCenter()),
					new Technology.TechPoint(EdgeH.fromRight(4), EdgeV.makeCenter())}),
				new Technology.NodeLayer(well_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.makeCenter()),
					new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.makeCenter())})
			});
		dActivePin_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, dActivePin_node, new ArcProto [] {dActive_arc}, "d-active", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		dActivePin_node.setFunction(PrimitiveNode.Function.PIN);
		dActivePin_node.setSquare();

		/** Substrate-Active-Pin */
		PrimitiveNode substrateActivePin_node = PrimitiveNode.newInstance("Substrate-Active-Pin", this, 2, 2, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(dActive_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.makeCenter()),
					new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.makeCenter())}),
				new Technology.NodeLayer(sActive_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.makeCenter()),
					new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.makeCenter())})
			});
		substrateActivePin_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, substrateActivePin_node, new ArcProto [] {substrateActive_arc}, "active", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		substrateActivePin_node.setFunction(PrimitiveNode.Function.PIN);
		substrateActivePin_node.setSquare();

		/** Well-Active-Pin */
		PrimitiveNode wellActivePin_node = PrimitiveNode.newInstance("Well-Active-Pin", this, 6, 6, new SizeOffset(2,2,2,2),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(dActive_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.makeCenter()),
					new Technology.TechPoint(EdgeH.fromRight(2), EdgeV.makeCenter())}),
				new Technology.NodeLayer(sActive_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.makeCenter()),
					new Technology.TechPoint(EdgeH.fromRight(2), EdgeV.makeCenter())}),
				new Technology.NodeLayer(well_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.makeCenter()),
					new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.makeCenter())}),
				new Technology.NodeLayer(select_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.makeCenter()),
					new Technology.TechPoint(EdgeH.fromRight(1), EdgeV.makeCenter())})
			});
		wellActivePin_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, wellActivePin_node, new ArcProto [] {wellActive_arc}, "active", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		wellActivePin_node.setFunction(PrimitiveNode.Function.PIN);
		wellActivePin_node.setSquare();

		/** S-Transistor-Pin */
		PrimitiveNode sTransistorPin_node = PrimitiveNode.newInstance("S-Transistor", this, 6, 6, new SizeOffset(2,2,2,2),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(sActive_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.makeCenter()),
					new Technology.TechPoint(EdgeH.fromRight(2), EdgeV.makeCenter())}),
				new Technology.NodeLayer(polysilicon_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.makeCenter()),
					new Technology.TechPoint(EdgeH.fromRight(2), EdgeV.makeCenter())}),
				new Technology.NodeLayer(select_lay, -1, Poly.Type.DISC, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.makeCenter()),
					new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.makeCenter())})
			});
		sTransistorPin_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, sTransistorPin_node, new ArcProto [] {sTransistor_arc, sActive_arc, polysilicon_arc},
					"s-trans", 0,180, 0, PortCharacteristic.UNKNOWN,
						EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		sTransistorPin_node.setFunction(PrimitiveNode.Function.PIN);

		/** D-Transistor-Pin */
		PrimitiveNode dTransistorPin_node = PrimitiveNode.newInstance("D-Transistor", this, 10, 10, new SizeOffset(4,4,4,4),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(dActive_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.makeCenter()),
					new Technology.TechPoint(EdgeH.fromRight(4), EdgeV.makeCenter())}),
				new Technology.NodeLayer(polysilicon_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.makeCenter()),
					new Technology.TechPoint(EdgeH.fromRight(4), EdgeV.makeCenter())}),
				new Technology.NodeLayer(well_lay, -1, Poly.Type.DISC, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.makeCenter()),
					new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.makeCenter())})
			});
		dTransistorPin_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, dTransistorPin_node, new ArcProto [] {dTransistor_arc, dActive_arc, polysilicon_arc},
					"d-trans", 0,180, 0, PortCharacteristic.UNKNOWN,
						EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		dTransistorPin_node.setFunction(PrimitiveNode.Function.PIN);

		/** Metal-1-S-active-contact */
		PrimitiveNode metal1SActiveCon_node = PrimitiveNode.newInstance("Metal-1-S-Active-Con", this, 10, 10, new SizeOffset(2,2,2,2),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(metal1_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.makeCenter()),
					new Technology.TechPoint(EdgeH.fromRight(3), EdgeV.makeCenter())}),
				new Technology.NodeLayer(sActive_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.makeCenter()),
					new Technology.TechPoint(EdgeH.fromRight(2), EdgeV.makeCenter())}),
				new Technology.NodeLayer(select_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.makeCenter()),
					new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.makeCenter())}),
				new Technology.NodeLayer(activeCut_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.makeCenter()),
					new Technology.TechPoint(EdgeH.fromCenter(1), EdgeV.makeCenter())})
			});
		metal1SActiveCon_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, metal1SActiveCon_node, new ArcProto [] {sActive_arc, metal1_arc},
					"metal-1-s-act", 0,180, 0, PortCharacteristic.UNKNOWN,
						EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		metal1SActiveCon_node.setFunction(PrimitiveNode.Function.CONTACT);
		metal1SActiveCon_node.setSquare();

		/** Metal-1-D-active-contact */
		PrimitiveNode metal1DActiveCon_node = PrimitiveNode.newInstance("Metal-1-D-Active-Con", this, 14, 14, new SizeOffset(4,4,4,4),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(metal1_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.makeCenter()),
					new Technology.TechPoint(EdgeH.fromRight(5), EdgeV.makeCenter())}),
				new Technology.NodeLayer(dActive_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.makeCenter()),
					new Technology.TechPoint(EdgeH.fromRight(4), EdgeV.makeCenter())}),
				new Technology.NodeLayer(well_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.makeCenter()),
					new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.makeCenter())}),
				new Technology.NodeLayer(activeCut_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.makeCenter()),
					new Technology.TechPoint(EdgeH.fromCenter(1), EdgeV.makeCenter())})
			});
		metal1DActiveCon_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, metal1DActiveCon_node, new ArcProto [] {dActive_arc, metal1_arc},
					"metal-1-d-act", 0,180, 0, PortCharacteristic.UNKNOWN,
						EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		metal1DActiveCon_node.setFunction(PrimitiveNode.Function.CONTACT);
		metal1DActiveCon_node.setSquare();

		/** Metal-1-polysilicon-contact */
		PrimitiveNode metal1PolyCon_node = PrimitiveNode.newInstance("Metal-1-Polysilicon-Con", this, 6, 6, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(metal1_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.makeCenter()),
					new Technology.TechPoint(EdgeH.fromRight(1), EdgeV.makeCenter())}),
				new Technology.NodeLayer(polysilicon_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.makeCenter()),
					new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.makeCenter())}),
				new Technology.NodeLayer(polyCut_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.makeCenter()),
					new Technology.TechPoint(EdgeH.fromCenter(1), EdgeV.makeCenter())})
			});
		metal1PolyCon_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, metal1PolyCon_node, new ArcProto [] {polysilicon_arc, metal1_arc},
					"metal-1-polysilicon", 0,180, 0, PortCharacteristic.UNKNOWN,
						EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		metal1PolyCon_node.setFunction(PrimitiveNode.Function.CONTACT);
		metal1PolyCon_node.setSquare();

		/** Metal-1-Metal-2-contact */
		PrimitiveNode metal1Metal2Con_node = PrimitiveNode.newInstance("Metal-1-Metal-2-Con", this, 4, 4, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(metal1_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.makeCenter()),
					new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.makeCenter())}),
				new Technology.NodeLayer(metal2_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.makeCenter()),
					new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.makeCenter())}),
				new Technology.NodeLayer(via_lay, 0, Poly.Type.CIRCLE, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.makeCenter()),
					new Technology.TechPoint(EdgeH.fromCenter(1), EdgeV.makeCenter())})
			});
		metal1Metal2Con_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, metal1Metal2Con_node, new ArcProto [] {metal1_arc, metal2_arc},
					"metal-1-metal-2", 0,180, 0, PortCharacteristic.UNKNOWN,
						EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		metal1Metal2Con_node.setFunction(PrimitiveNode.Function.CONTACT);
		metal1Metal2Con_node.setSquare();

		/** Metal-1-Well Contact */
		PrimitiveNode metal1WellCon_node = PrimitiveNode.newInstance("Metal-1-Well-Con", this, 10, 10, new SizeOffset(2,2,2,2),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(metal1_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.makeCenter()),
					new Technology.TechPoint(EdgeH.fromRight(5), EdgeV.makeCenter())}),
				new Technology.NodeLayer(sActive_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.makeCenter()),
					new Technology.TechPoint(EdgeH.fromRight(2), EdgeV.makeCenter())}),
				new Technology.NodeLayer(well_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.makeCenter()),
					new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.makeCenter())}),
				new Technology.NodeLayer(select_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.makeCenter()),
					new Technology.TechPoint(EdgeH.fromRight(2), EdgeV.makeCenter())}),
				new Technology.NodeLayer(activeCut_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.makeCenter()),
					new Technology.TechPoint(EdgeH.fromCenter(1), EdgeV.makeCenter())})
			});
		metal1WellCon_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, metal1WellCon_node, new ArcProto [] {metal1_arc, wellActive_arc},
					"metal-1-well", 0,180, 0, PortCharacteristic.UNKNOWN,
						EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		metal1WellCon_node.setFunction(PrimitiveNode.Function.WELL);
		metal1WellCon_node.setSquare();

		/** Metal-1-Substrate Contact */
		PrimitiveNode metal1SubstrateCon_node = PrimitiveNode.newInstance("Metal-1-Substrate-Con", this, 6, 6, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(metal1_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.makeCenter()),
					new Technology.TechPoint(EdgeH.fromRight(1), EdgeV.makeCenter())}),
				new Technology.NodeLayer(sActive_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.makeCenter()),
					new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.makeCenter())}),
				new Technology.NodeLayer(activeCut_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.makeCenter()),
					new Technology.TechPoint(EdgeH.fromCenter(1), EdgeV.makeCenter())})
			});
		metal1SubstrateCon_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, metal1SubstrateCon_node, new ArcProto [] {metal1_arc, substrateActive_arc},
					"metal-1-substrate", 0,180, 0, PortCharacteristic.UNKNOWN,
						EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		metal1SubstrateCon_node.setFunction(PrimitiveNode.Function.WELL);
		metal1SubstrateCon_node.setSquare();

		/** Metal-1 node */
		PrimitiveNode metal1_node = PrimitiveNode.newInstance("Metal-1-Node", this, 4, 4, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(metal1_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		metal1_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, metal1_node, new ArcProto [] {metal1_arc}, "metal-1", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		metal1_node.setFunction(PrimitiveNode.Function.NODE);
		metal1_node.setHoldsOutline();
		metal1_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Metal-2 node */
		PrimitiveNode metal2_node = PrimitiveNode.newInstance("Metal-2-Node", this, 4, 4, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(metal2_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		metal2_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, metal2_node, new ArcProto [] {metal2_arc}, "metal-2", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		metal2_node.setFunction(PrimitiveNode.Function.NODE);
		metal2_node.setHoldsOutline();
		metal2_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Polysilicon node */
		PrimitiveNode polysilicon_node = PrimitiveNode.newInstance("Polysilicon-Node", this, 4, 4, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(polysilicon_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		polysilicon_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, polysilicon_node, new ArcProto [] {polysilicon_arc}, "polysilicon", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		polysilicon_node.setFunction(PrimitiveNode.Function.NODE);
		polysilicon_node.setHoldsOutline();
		polysilicon_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Active node */
		PrimitiveNode active_node = PrimitiveNode.newInstance("Active-Node", this, 4, 4, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(sActive_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		active_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, active_node, new ArcProto [] {sActive_arc}, "active", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		active_node.setFunction(PrimitiveNode.Function.NODE);
		active_node.setHoldsOutline();
		active_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Select node */
		PrimitiveNode select_node = PrimitiveNode.newInstance("Select-Node", this, 4, 4, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(select_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		select_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, select_node, new ArcProto [] {}, "select", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		select_node.setFunction(PrimitiveNode.Function.NODE);
		select_node.setHoldsOutline();
		select_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Cut node */
		PrimitiveNode cut_node = PrimitiveNode.newInstance("Cut-Node", this, 2, 2, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(cut_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		cut_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, cut_node, new ArcProto [] {}, "cut", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		cut_node.setFunction(PrimitiveNode.Function.NODE);
		cut_node.setHoldsOutline();
		cut_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Poly Cut node */
		PrimitiveNode polyCut_node = PrimitiveNode.newInstance("Poly-Cut-Node", this, 2, 2, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(polyCut_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		polyCut_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, polyCut_node, new ArcProto [] {}, "polycut", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		polyCut_node.setFunction(PrimitiveNode.Function.NODE);
		polyCut_node.setHoldsOutline();
		polyCut_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Active Cut node */
		PrimitiveNode activeCut_node = PrimitiveNode.newInstance("Active-Cut-Node", this, 2, 2, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(activeCut_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		activeCut_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, activeCut_node, new ArcProto [] {}, "activecut", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		activeCut_node.setFunction(PrimitiveNode.Function.NODE);
		activeCut_node.setHoldsOutline();
		activeCut_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Via node */
		PrimitiveNode via_node = PrimitiveNode.newInstance("Via-Node", this, 2, 2, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(via_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		via_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, via_node, new ArcProto [] {}, "via", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		via_node.setFunction(PrimitiveNode.Function.NODE);
		via_node.setHoldsOutline();
		via_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Well node */
		PrimitiveNode well_node = PrimitiveNode.newInstance("Well-Node", this, 6, 6, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(well_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		well_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, well_node, new ArcProto [] {}, "well", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		well_node.setFunction(PrimitiveNode.Function.NODE);
		well_node.setHoldsOutline();
		well_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Passivation node */
		PrimitiveNode passivation_node = PrimitiveNode.newInstance("Passivation-Node", this, 8, 8, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(passivation_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		passivation_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, passivation_node, new ArcProto [] {}, "passivation", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		passivation_node.setFunction(PrimitiveNode.Function.NODE);
		passivation_node.setHoldsOutline();
		passivation_node.setSpecialType(PrimitiveNode.POLYGONAL);

		// The pure layer nodes
		metal1_lay.setPureLayerNode(metal1_node);		// Metal-1-Node
		metal2_lay.setPureLayerNode(metal2_node);		// Metal-2-Node
		polysilicon_lay.setPureLayerNode(polysilicon_node);		// Polysilicon-Node
		sActive_lay.setPureLayerNode(active_node);		// Active-Node
		select_lay.setPureLayerNode(select_node);		// Select-Node
		cut_lay.setPureLayerNode(cut_node);		// Cut-Node
		polyCut_lay.setPureLayerNode(polyCut_node);		// Poly-Cut-Node
		activeCut_lay.setPureLayerNode(activeCut_node);		// Active-Cut-Node
		via_lay.setPureLayerNode(via_node);		// Via-Node
		well_lay.setPureLayerNode(well_node);		// Well-Node
		passivation_lay.setPureLayerNode(passivation_node);		// Passivation-Node

        // Information for palette
        int maxY = 2 /*metal arcs*/ + 4 /* active arcs */ + 2 /* transistor arcs*/ + 1 /* poly*/;
        nodeGroups = new Object[maxY][3];
        int count = -1;

        nodeGroups[++count][0] = sTransistor_arc; nodeGroups[count][1] = sTransistorPin_node; nodeGroups[count][2] = "Pure";
        nodeGroups[++count][0] = dTransistor_arc; nodeGroups[count][1] = dTransistorPin_node; nodeGroups[count][2] = "Misc.";
        nodeGroups[++count][0] = wellActive_arc; nodeGroups[count][1] = wellActivePin_node; nodeGroups[count][2] = metal1WellCon_node;
        nodeGroups[++count][0] = sActive_arc; nodeGroups[count][1] = sActivePin_node; nodeGroups[count][2] = metal1SActiveCon_node;
        nodeGroups[++count][0] = dActive_arc; nodeGroups[count][1] = dActivePin_node; nodeGroups[count][2] = metal1DActiveCon_node;
        nodeGroups[++count][0] = substrateActive_arc; nodeGroups[count][1] = substrateActivePin_node; nodeGroups[count][2] = metal1SubstrateCon_node;
        nodeGroups[++count][0] = polysilicon_arc; nodeGroups[count][1] = polysiliconPin_node; nodeGroups[count][2] = metal1PolyCon_node;
        nodeGroups[++count][0] = metal1_arc; nodeGroups[count][1] = metal1Pin_node; nodeGroups[count][2] = metal1Metal2Con_node;
        nodeGroups[++count][0] = metal2_arc; nodeGroups[count][1] = metal2Pin_node; nodeGroups[count][2] = "Cell";

	};
}
