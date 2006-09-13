/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: nMOS.java
 * nmos technology description
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
 * This is the n-channel MOS (from Mead & Conway) Technology.
 */
public class nMOS extends Technology
{
	/** the n-channel MOS (from Mead & Conway) Technology object. */	public static final nMOS tech = new nMOS();

	private static final double XX = -1;
	private double [] conDist, unConDist;

	// -------------------- private and protected methods ------------------------
	private nMOS()
	{
		super("nmos");
		setTechShortName("nMOS");
		setTechDesc("nMOS (Mead & Conway abstract rules)");
		setFactoryScale(2000, true);   // in nanometers: really 2 microns
		setNoNegatedArcs();
		setStaticTechnology();

        //Foundry
        Foundry noFoundry = new Foundry(Foundry.Type.NONE);
        foundries.add(noFoundry);

		setFactoryTransparentLayers(new Color []
		{
			new Color(  0,  0,200), // Metal
			new Color(220,  0,120), // Polysilicon
			new Color( 70,250, 70), // Diffusion
			new Color(250,250,  0), // Implant
			new Color(180,180,180)  // Buried-Contact
		});

		//**************************************** LAYERS ****************************************

		/** M layer */
		Layer M_lay = Layer.newInstance(this, "Metal",
			new EGraphics(false, false, null, EGraphics.TRANSPARENT_1,   0,  0,200,/*70,250,70,*/0.8,true,
			new int[] { 0x0000,   //                 
						0x0000,   //                 
						0x2222,   //   X   X   X   X 
						0x8888,   // X   X   X   X   
						0x0000,   //                 
						0x0000,   //                 
						0x2222,   //   X   X   X   X 
						0x8888,   // X   X   X   X   
						0x0000,   //                 
						0x0000,   //                 
						0x2222,   //   X   X   X   X 
						0x8888,   // X   X   X   X   
						0x0000,   //                 
						0x0000,   //                 
						0x2222,   //   X   X   X   X 
						0x8888}));// X   X   X   X   

		/** P layer */
		Layer P_lay = Layer.newInstance(this, "Polysilicon",
			new EGraphics(false, false, null, EGraphics.TRANSPARENT_2, 220,  0,120,/*250,250,0,*/0.8,true,
			new int[] { 0x1111,   //    X   X   X   X
						0x8888,   // X   X   X   X   
						0x4444,   //  X   X   X   X  
						0x2222,   //   X   X   X   X 
						0x1111,   //    X   X   X   X
						0x8888,   // X   X   X   X   
						0x4444,   //  X   X   X   X  
						0x2222,   //   X   X   X   X 
						0x1111,   //    X   X   X   X
						0x8888,   // X   X   X   X   
						0x4444,   //  X   X   X   X  
						0x2222,   //   X   X   X   X 
						0x1111,   //    X   X   X   X
						0x8888,   // X   X   X   X   
						0x4444,   //  X   X   X   X  
						0x2222}));//   X   X   X   X 

		/** D layer */
		Layer D_lay = Layer.newInstance(this, "Diffusion",
			new EGraphics(false, false, null, EGraphics.TRANSPARENT_3,  70,250, 70,/*180,180,180,*/0.8,true,
			new int[] { 0x4444,   //  X   X   X   X  
						0x1111,   //    X   X   X   X
						0x4444,   //  X   X   X   X  
						0x1111,   //    X   X   X   X
						0x4444,   //  X   X   X   X  
						0x1111,   //    X   X   X   X
						0x4444,   //  X   X   X   X  
						0x1111,   //    X   X   X   X
						0x4444,   //  X   X   X   X  
						0x1111,   //    X   X   X   X
						0x4444,   //  X   X   X   X  
						0x1111,   //    X   X   X   X
						0x4444,   //  X   X   X   X  
						0x1111,   //    X   X   X   X
						0x4444,   //  X   X   X   X  
						0x1111}));//    X   X   X   X

		/** I layer */
		Layer I_lay = Layer.newInstance(this, "Implant",
			new EGraphics(false, false, null, EGraphics.TRANSPARENT_4, 250,250,  0,/*0,0,0,*/0.8,true,
			new int[] { 0x0000,   //                 
						0x0000,   //                 
						0x1111,   //    X   X   X   X
						0x0000,   //                 
						0x0000,   //                 
						0x0000,   //                 
						0x1111,   //    X   X   X   X
						0x0000,   //                 
						0x0000,   //                 
						0x0000,   //                 
						0x1111,   //    X   X   X   X
						0x0000,   //                 
						0x0000,   //                 
						0x0000,   //                 
						0x1111,   //    X   X   X   X
						0x0000}));//                 

		/** CC layer */
		Layer CC_lay = Layer.newInstance(this, "Contact-Cut",
			new EGraphics(false, false, null, 0, 180,130,0,0.8,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** BC layer */
		Layer BC_lay = Layer.newInstance(this, "Buried-Contact",
			new EGraphics(false, false, null, EGraphics.TRANSPARENT_5, 180,180,180,/*0,0,0,*/0.8,true,
			new int[] { 0x0000,   //                 
						0x2222,   //   X   X   X   X 
						0x4444,   //  X   X   X   X  
						0x8888,   // X   X   X   X   
						0x0000,   //                 
						0x2222,   //   X   X   X   X 
						0x4444,   //  X   X   X   X  
						0x8888,   // X   X   X   X   
						0x0000,   //                 
						0x2222,   //   X   X   X   X 
						0x4444,   //  X   X   X   X  
						0x8888,   // X   X   X   X   
						0x0000,   //                 
						0x2222,   //   X   X   X   X 
						0x4444,   //  X   X   X   X  
						0x8888}));// X   X   X   X   

		/** O layer */
		Layer O_lay = Layer.newInstance(this, "Overglass",
			new EGraphics(false, false, null, 0, 0,0,0,0.8,true,
			new int[] { 0x0000,   //                 
						0x2222,   //   X   X   X   X 
						0x5555,   //  X X X X X X X X
						0x2222,   //   X   X   X   X 
						0x0000,   //                 
						0x2222,   //   X   X   X   X 
						0x5555,   //  X X X X X X X X
						0x2222,   //   X   X   X   X 
						0x0000,   //                 
						0x2222,   //   X   X   X   X 
						0x5555,   //  X X X X X X X X
						0x2222,   //   X   X   X   X 
						0x0000,   //                 
						0x2222,   //   X   X   X   X 
						0x5555,   //  X X X X X X X X
						0x2222}));//   X   X   X   X 

		/** LI layer */
		Layer LI_lay = Layer.newInstance(this, "Light-Implant",
			new EGraphics(false, false, null, 0, 150,90,0,0.8,true,
			new int[] { 0x0000,   //                 
						0xcccc,   // XX  XX  XX  XX  
						0x0000,   //                 
						0xcccc,   // XX  XX  XX  XX  
						0x0000,   //                 
						0x0000,   //                 
						0x0000,   //                 
						0x0000,   //                 
						0x0000,   //                 
						0xcccc,   // XX  XX  XX  XX  
						0x0000,   //                 
						0xcccc,   // XX  XX  XX  XX  
						0x0000,   //                 
						0x0000,   //                 
						0x0000,   //                 
						0x0000}));//                 

		/** OC layer */
		Layer OC_lay = Layer.newInstance(this, "Oversize-Contact",
			new EGraphics(false, false, null, 0, 0,0,0,0.8,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** HE layer */
		Layer HE_lay = Layer.newInstance(this, "Hard-Enhancement",
			new EGraphics(false, false, null, 0, 0,0,0,0.8,true,
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

		/** LE layer */
		Layer LE_lay = Layer.newInstance(this, "Light-Enhancement",
			new EGraphics(false, false, null, 0, 0,0,0,0.8,true,
			new int[] { 0x4040,   //  X       X      
						0x8080,   // X       X       
						0x0101,   //        X       X
						0x0202,   //       X       X 
						0x0101,   //        X       X
						0x8080,   // X       X       
						0x4040,   //  X       X      
						0x2020,   //   X       X     
						0x4040,   //  X       X      
						0x8080,   // X       X       
						0x0101,   //        X       X
						0x0202,   //       X       X 
						0x0101,   //        X       X
						0x8080,   // X       X       
						0x4040,   //  X       X      
						0x2020}));//   X       X     

		/** T layer */
		Layer T_lay = Layer.newInstance(this, "Transistor",
			new EGraphics(false, false, null, 0, 200,200,200,0.8,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** PM layer */
		Layer PM_lay = Layer.newInstance(this, "Pseudo-Metal",
			new EGraphics(false, false, null, EGraphics.TRANSPARENT_1,   0,  0,200,/*70,250,70,*/0.8,true,
			new int[] { 0x0000,   //                 
						0x0000,   //                 
						0x2222,   //   X   X   X   X 
						0x8888,   // X   X   X   X   
						0x0000,   //                 
						0x0000,   //                 
						0x2222,   //   X   X   X   X 
						0x8888,   // X   X   X   X   
						0x0000,   //                 
						0x0000,   //                 
						0x2222,   //   X   X   X   X 
						0x8888,   // X   X   X   X   
						0x0000,   //                 
						0x0000,   //                 
						0x2222,   //   X   X   X   X 
						0x8888}));// X   X   X   X   

		/** PP layer */
		Layer PP_lay = Layer.newInstance(this, "Pseudo-Polysilicon",
			new EGraphics(false, false, null, EGraphics.TRANSPARENT_2, 220,  0,120,/*250,250,0,*/0.8,true,
			new int[] { 0x1111,   //    X   X   X   X
						0x8888,   // X   X   X   X   
						0x4444,   //  X   X   X   X  
						0x2222,   //   X   X   X   X 
						0x1111,   //    X   X   X   X
						0x8888,   // X   X   X   X   
						0x4444,   //  X   X   X   X  
						0x2222,   //   X   X   X   X 
						0x1111,   //    X   X   X   X
						0x8888,   // X   X   X   X   
						0x4444,   //  X   X   X   X  
						0x2222,   //   X   X   X   X 
						0x1111,   //    X   X   X   X
						0x8888,   // X   X   X   X   
						0x4444,   //  X   X   X   X  
						0x2222}));//   X   X   X   X 

		/** PD layer */
		Layer PD_lay = Layer.newInstance(this, "Pseudo-Diffusion",
			new EGraphics(false, false, null, EGraphics.TRANSPARENT_3,  70,250, 70,/*180,180,180,*/0.8,true,
			new int[] { 0x4444,   //  X   X   X   X  
						0x1111,   //    X   X   X   X
						0x4444,   //  X   X   X   X  
						0x1111,   //    X   X   X   X
						0x4444,   //  X   X   X   X  
						0x1111,   //    X   X   X   X
						0x4444,   //  X   X   X   X  
						0x1111,   //    X   X   X   X
						0x4444,   //  X   X   X   X  
						0x1111,   //    X   X   X   X
						0x4444,   //  X   X   X   X  
						0x1111,   //    X   X   X   X
						0x4444,   //  X   X   X   X  
						0x1111,   //    X   X   X   X
						0x4444,   //  X   X   X   X  
						0x1111}));//    X   X   X   X

		// The layer functions
		M_lay.setFunction(Layer.Function.METAL1);		// Metal
		P_lay.setFunction(Layer.Function.POLY1);		// Polysilicon
		D_lay.setFunction(Layer.Function.DIFF);		// Diffusion
		I_lay.setFunction(Layer.Function.IMPLANT, Layer.Function.DEPLETION|Layer.Function.HEAVY);		// Implant
		CC_lay.setFunction(Layer.Function.CONTACT1);		// Contact-Cut
		BC_lay.setFunction(Layer.Function.IMPLANT);		// Buried-Contact
		O_lay.setFunction(Layer.Function.OVERGLASS);		// Overglass
		LI_lay.setFunction(Layer.Function.IMPLANT, Layer.Function.DEPLETION|Layer.Function.LIGHT);		// Light-Implant
		OC_lay.setFunction(Layer.Function.CONTACT3);		// Oversize-Contact
		HE_lay.setFunction(Layer.Function.IMPLANT, Layer.Function.ENHANCEMENT|Layer.Function.HEAVY);		// Hard-Enhancement
		LE_lay.setFunction(Layer.Function.IMPLANT, Layer.Function.ENHANCEMENT|Layer.Function.LIGHT);		// Light-Enhancement
		T_lay.setFunction(Layer.Function.TRANSISTOR, Layer.Function.PSEUDO);		// Transistor
		PM_lay.setFunction(Layer.Function.METAL1, Layer.Function.PSEUDO);		// Pseudo-Metal
		PP_lay.setFunction(Layer.Function.POLY1, Layer.Function.PSEUDO);		// Pseudo-Polysilicon
		PD_lay.setFunction(Layer.Function.DIFF, Layer.Function.PSEUDO);		// Pseudo-Diffusion

		// The CIF names
		M_lay.setFactoryCIFLayer("NM");		// Metal
		P_lay.setFactoryCIFLayer("NP");		// Polysilicon
		D_lay.setFactoryCIFLayer("ND");		// Diffusion
		I_lay.setFactoryCIFLayer("NI");		// Implant
		CC_lay.setFactoryCIFLayer("NC");		// Contact-Cut
		BC_lay.setFactoryCIFLayer("NB");		// Buried-Contact
		O_lay.setFactoryCIFLayer("NG");		// Overglass
		LI_lay.setFactoryCIFLayer("NJ");		// Light-Implant
		OC_lay.setFactoryCIFLayer("NO");		// Oversize-Contact
		HE_lay.setFactoryCIFLayer("NE");		// Hard-Enhancement
		LE_lay.setFactoryCIFLayer("NF");		// Light-Enhancement
		T_lay.setFactoryCIFLayer("");		// Transistor
		PM_lay.setFactoryCIFLayer("");		// Pseudo-Metal
		PP_lay.setFactoryCIFLayer("");		// Pseudo-Polysilicon
		PD_lay.setFactoryCIFLayer("");		// Pseudo-Diffusion

		// The DXF names
		M_lay.setFactoryDXFLayer("");		// Metal
		P_lay.setFactoryDXFLayer("");		// Polysilicon
		D_lay.setFactoryDXFLayer("");		// Diffusion
		I_lay.setFactoryDXFLayer("");		// Implant
		CC_lay.setFactoryDXFLayer("");		// Contact-Cut
		BC_lay.setFactoryDXFLayer("");		// Buried-Contact
		O_lay.setFactoryDXFLayer("");		// Overglass
		LI_lay.setFactoryDXFLayer("");		// Light-Implant
		OC_lay.setFactoryDXFLayer("");		// Oversize-Contact
		HE_lay.setFactoryDXFLayer("");		// Hard-Enhancement
		LE_lay.setFactoryDXFLayer("");		// Light-Enhancement
		T_lay.setFactoryDXFLayer("");		// Transistor
		PM_lay.setFactoryDXFLayer("");		// Pseudo-Metal
		PP_lay.setFactoryDXFLayer("");		// Pseudo-Polysilicon
		PD_lay.setFactoryDXFLayer("");		// Pseudo-Diffusion

		// The GDS names
//		M_lay.setFactoryGDSLayer("", Foundry.Type.MOSIS.name());		// Metal
//		P_lay.setFactoryGDSLayer("", Foundry.Type.MOSIS.name());		// Polysilicon
//		D_lay.setFactoryGDSLayer("", Foundry.Type.MOSIS.name());		// Diffusion
//		I_lay.setFactoryGDSLayer("", Foundry.Type.MOSIS.name());		// Implant
//		CC_lay.setFactoryGDSLayer("", Foundry.Type.MOSIS.name());		// Contact-Cut
//		BC_lay.setFactoryGDSLayer("", Foundry.Type.MOSIS.name());		// Buried-Contact
//		O_lay.setFactoryGDSLayer("", Foundry.Type.MOSIS.name());		// Overglass
//		LI_lay.setFactoryGDSLayer("", Foundry.Type.MOSIS.name());		// Light-Implant
//		OC_lay.setFactoryGDSLayer("", Foundry.Type.MOSIS.name());		// Oversize-Contact
//		HE_lay.setFactoryGDSLayer("", Foundry.Type.MOSIS.name());		// Hard-Enhancement
//		LE_lay.setFactoryGDSLayer("", Foundry.Type.MOSIS.name());		// Light-Enhancement
//		T_lay.setFactoryGDSLayer("", Foundry.Type.MOSIS.name());		// Transistor
//		PM_lay.setFactoryGDSLayer("", Foundry.Type.MOSIS.name());		// Pseudo-Metal
//		PP_lay.setFactoryGDSLayer("", Foundry.Type.MOSIS.name());		// Pseudo-Polysilicon
//		PD_lay.setFactoryGDSLayer("", Foundry.Type.MOSIS.name());		// Pseudo-Diffusion

		// The SPICE information
		M_lay.setFactoryParasitics(0.03f, 0.03f, 0);		// Metal
		P_lay.setFactoryParasitics(50.0f, 0.04f, 0);		// Polysilicon
		D_lay.setFactoryParasitics(10.0f, 0.1f, 0);		// Diffusion
		I_lay.setFactoryParasitics(0, 0, 0);		// Implant
		CC_lay.setFactoryParasitics(0, 0, 0);		// Contact-Cut
		BC_lay.setFactoryParasitics(0, 0, 0);		// Buried-Contact
		O_lay.setFactoryParasitics(0, 0, 0);		// Overglass
		LI_lay.setFactoryParasitics(0, 0, 0);		// Light-Implant
		OC_lay.setFactoryParasitics(0, 0, 0);		// Oversize-Contact
		HE_lay.setFactoryParasitics(0, 0, 0);		// Hard-Enhancement
		LE_lay.setFactoryParasitics(0, 0, 0);		// Light-Enhancement
		T_lay.setFactoryParasitics(0, 0, 0);		// Transistor
		PM_lay.setFactoryParasitics(0, 0, 0);		// Pseudo-Metal
		PP_lay.setFactoryParasitics(0, 0, 0);		// Pseudo-Polysilicon
		PD_lay.setFactoryParasitics(0, 0, 0);		// Pseudo-Diffusion
		setFactoryParasitics(50, 50);
		String [] headerLevel1 =
		{
			"*NMOS 4UM PROCESS",
			".OPTIONS DEFL=4UM DEFW=4UM DEFAS=80PM^2 DEFAD=80PM^2",
			".MODEL N NMOS LEVEL=1 VTO=1.1 KP=33UA/V^2 TOX=68NM GAMMA=.41",
			"+             LAMBDA=0.05 CGSO=0.18NF/M CGDO=0.18NF/M LD=0.4UM",
			"+             JS=.2A/M^2 CJ=.11MF/M^2",
			".MODEL D NMOS LEVEL=1 VTO=-3.4 KP=31UA/V^2 TOX=68NM GAMMA=.44",
			"+             LAMBDA=0.05 CGSO=0.18NF/M CGDO=0.18NF/M LD=0.4UM",
			"+             JS=.2A/M^2 CJ=.11MF/M^2",
			".MODEL DIFFCAP D CJO=.11MF/M^2"
		};
		setSpiceHeaderLevel1(headerLevel1);
		String [] headerLevel3 =
		{
			"*NMOS 4UM PROCESS",
			".OPTIONS DEFL=4UM DEFW=4UM DEFAS=80PM^2 DEFAD=80PM^2",
			"* RSH SET TO ZERO (MOSIS: RSH = 12)",
			".MODEL N NMOS LEVEL=3 VTO=0.849 LD=0.17U KP=2.98E-5 GAMMA=0.552",
			"+PHI=0.6 TOX=0.601E-7 NSUB=2.11E15 NSS=0 NFS=8.89E11 TPG=1 XJ=7.73E-7",
			"+UO=400 UEXP=1E-3 UCRIT=1.74E5 VMAX=1E5 NEFF=1E-2 DELTA=1.19",
			"+THETA=9.24E-3 ETA=0.77 KAPPA=3.25 RSH=0 CGSO=1.6E-10 CGDO=1.6E-10",
			"+CGBO=1.7E-10 CJ=1.1E-4 MJ=0.5 CJSW=1E-9",
			".MODEL D NMOS LEVEL=3 VTO=-3.07 LD=0.219U KP=2.76E-5 GAMMA=0.315",
			"+PHI=0.6 TOX=0.601E-7 NSUB=8.76E14 NSS=0 NFS=4.31E12 TPG=1 XJ=0.421U",
			"+UO=650 UEXP=1E-3 UCRIT=8.05E5 VMAX=1.96E5 NEFF=1E-2 DELTA=2.41",
			"+THETA=0 ETA=2.0 KAPPA=0.411 RSH=0 CGSO=1.6E-10 CGDO=1.6E-10",
			"+CGBO=1.7E-10 CJ=1.1E-4 MJ=0.5 CJSW=1E-9",
			"*MOSIS IS NOT RESPONSIBLE FOR THE FOLLOWING DIOD DATA",
			".MODEL DIFFCAP D CJO=.11MF/m^2"
		};
		setSpiceHeaderLevel3(headerLevel3);

		//******************** DESIGN RULES ********************

		conDist = new double[]
		{
			//            M  P  D  I  C  B  O  L  O  H  L  T  M  P  D
			//            e  o  i  m  u  u  v     v        r  e  o  i
			//            t  l  f  p  t  r  e  I  r  E  E  a  t  l  f
			//            a  y  f  l     i  r  m  C  n  n  n  a  y  f
			//            l        n     e  g  p  o  h  h  s  l  P  P
			//                     t     d  l  l  n           P      
			/* Metal  */ XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,
			/* Poly   */    XX, 0,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,
			/* Diff   */       XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,
			/* Implnt */          XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,
			/* Cut    */             XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,
			/* Buried */                XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,
			/* Overgl */                   XX,XX,XX,XX,XX,XX,XX,XX,XX,
			/* L Impl */                      XX,XX,XX,XX,XX,XX,XX,XX,
			/* OvrCon */                         XX,XX,XX,XX,XX,XX,XX,
			/* H Enh  */                            XX,XX,XX,XX,XX,XX,
			/* L Enh  */                               XX,XX,XX,XX,XX,
			/* Trans  */                                  XX,XX,XX,XX,
			/* MetalP */                                     XX,XX,XX,
			/* PolyP  */                                        XX,XX,
			/* DiffP  */                                           XX
		};
		unConDist = new double[]
		{
			//            M  P  D  I  C  B  O  L  O  H  L  T  M  P  D
			//            e  o  i  m  u  u  v     v        r  e  o  i
			//            t  l  f  p  t  r  e  I  r  E  E  a  t  l  f
			//            a  y  f  l     i  r  m  C  n  n  n  a  y  f
			//            l        n     e  g  p  o  h  h  s  l  P  P
			//                     t     d  l  l  n           P      
			/* Metal  */  3,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,
			/* Poly   */     2, 1, 1,XX, 2,XX,XX,XX,XX,XX,XX,XX,XX,XX,
			/* Diff   */        3, 2,XX, 2,XX,XX,XX,XX,XX,XX,XX,XX,XX,
			/* Implnt */          XX,XX,XX,XX,XX,XX,XX,XX, 2,XX,XX,XX,
			/* Cut    */             XX,XX,XX,XX,XX,XX,XX, 2,XX,XX,XX,
			/* Buried */                XX,XX,XX,XX,XX,XX,XX,XX,XX,XX,
			/* Overgl */                   XX,XX,XX,XX,XX,XX,XX,XX,XX,
			/* L Impl */                      XX,XX,XX,XX,XX,XX,XX,XX,
			/* OvrCon */                         XX,XX,XX,XX,XX,XX,XX,
			/* H Enh  */                            XX,XX,XX,XX,XX,XX,
			/* L Enh  */                               XX,XX,XX,XX,XX,
			/* Trans  */                                  XX,XX,XX,XX,
			/* MetalP */                                     XX,XX,XX,
			/* PolyP  */                                        XX,XX,
			/* DiffP  */                                           XX
		};

		//******************** ARCS ********************

		/** Metal arc */
		ArcProto Metal_arc = ArcProto.newInstance(this, "Metal", 3, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(M_lay, 0, Poly.Type.FILLED)
		});
		Metal_arc.setFunction(ArcProto.Function.METAL1);
		Metal_arc.setFactoryFixedAngle(true);
		Metal_arc.setWipable();
		Metal_arc.setFactoryAngleIncrement(90);

		/** Polysilicon arc */
		ArcProto Polysilicon_arc = ArcProto.newInstance(this, "Polysilicon", 2, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(P_lay, 0, Poly.Type.FILLED)
		});
		Polysilicon_arc.setFunction(ArcProto.Function.POLY1);
		Polysilicon_arc.setFactoryFixedAngle(true);
		Polysilicon_arc.setWipable();
		Polysilicon_arc.setFactoryAngleIncrement(90);

		/** Diffusion arc */
		ArcProto Diffusion_arc = ArcProto.newInstance(this, "Diffusion", 2, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(D_lay, 0, Poly.Type.FILLED)
		});
		Diffusion_arc.setFunction(ArcProto.Function.DIFF);
		Diffusion_arc.setFactoryFixedAngle(true);
		Diffusion_arc.setWipable();
		Diffusion_arc.setFactoryAngleIncrement(90);

		//******************** RECTANGLE DESCRIPTIONS ********************

		Technology.TechPoint [] box_1 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge()),
			new Technology.TechPoint(EdgeH.makeLeftEdge(), EdgeV.fromTop(2)),
			new Technology.TechPoint(EdgeH.fromLeft(1), EdgeV.fromTop(2)),
			new Technology.TechPoint(EdgeH.fromLeft(1), EdgeV.fromTop(1)),
			new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.fromTop(1)),
			new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.makeBottomEdge()),
		};
		Technology.TechPoint [] box_2 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(2), EdgeV.fromBottom(1)),
			new Technology.TechPoint(EdgeH.fromRight(1), EdgeV.makeTopEdge()),
		};
		Technology.TechPoint [] box_3 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge()),
			new Technology.TechPoint(EdgeH.makeLeftEdge(), EdgeV.fromTop(2)),
			new Technology.TechPoint(EdgeH.fromLeft(1), EdgeV.fromTop(2)),
			new Technology.TechPoint(EdgeH.fromLeft(1), EdgeV.fromTop(1)),
			new Technology.TechPoint(EdgeH.fromRight(1), EdgeV.fromTop(1)),
			new Technology.TechPoint(EdgeH.fromRight(1), EdgeV.fromTop(2)),
			new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.fromTop(2)),
			new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.makeBottomEdge()),
		};
		Technology.TechPoint [] box_4 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(2), EdgeV.fromBottom(1)),
			new Technology.TechPoint(EdgeH.fromRight(2), EdgeV.makeTopEdge()),
		};
		Technology.TechPoint [] box_5 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.makeLeftEdge(), EdgeV.fromBottom(1)),
			new Technology.TechPoint(EdgeH.makeLeftEdge(), EdgeV.fromTop(1)),
			new Technology.TechPoint(EdgeH.fromLeft(1), EdgeV.fromTop(1)),
			new Technology.TechPoint(EdgeH.fromLeft(1), EdgeV.makeTopEdge()),
			new Technology.TechPoint(EdgeH.fromRight(1), EdgeV.makeTopEdge()),
			new Technology.TechPoint(EdgeH.fromRight(1), EdgeV.makeBottomEdge()),
			new Technology.TechPoint(EdgeH.fromLeft(1), EdgeV.makeBottomEdge()),
			new Technology.TechPoint(EdgeH.fromLeft(1), EdgeV.fromBottom(1)),
		};
		Technology.TechPoint [] box_6 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(2), EdgeV.fromBottom(1)),
			new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.fromTop(1)),
		};
		Technology.TechPoint [] box_7 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.makeLeftEdge(), EdgeV.fromBottom(1)),
			new Technology.TechPoint(EdgeH.fromRight(1), EdgeV.fromTop(1)),
		};
		Technology.TechPoint [] box_8 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(2), EdgeV.makeBottomEdge()),
			new Technology.TechPoint(EdgeH.fromRight(1), EdgeV.makeTopEdge()),
		};
		Technology.TechPoint [] box_9 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(2), EdgeV.makeBottomEdge()),
			new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.makeTopEdge()),
		};
		Technology.TechPoint [] box_10 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(0.5), EdgeV.fromBottom(0.5)),
			new Technology.TechPoint(EdgeH.fromRight(0.5), EdgeV.fromTop(0.5)),
		};
		Technology.TechPoint [] box_11 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(2), EdgeV.makeBottomEdge()),
			new Technology.TechPoint(EdgeH.fromRight(2), EdgeV.makeCenter()),
		};
		Technology.TechPoint [] box_12 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(2), EdgeV.makeCenter()),
			new Technology.TechPoint(EdgeH.fromRight(2), EdgeV.makeTopEdge()),
		};
		Technology.TechPoint [] box_13 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.makeLeftEdge(), EdgeV.fromBottom(2)),
			new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.fromTop(2)),
		};
		Technology.TechPoint [] box_14 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.makeLeftEdge(), EdgeV.fromBottom(1)),
			new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.fromTop(1)),
		};
		Technology.TechPoint [] box_15 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(2), EdgeV.makeBottomEdge()),
			new Technology.TechPoint(EdgeH.fromRight(2), EdgeV.makeTopEdge()),
		};
		Technology.TechPoint [] box_16 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.makeBottomEdge()),
			new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.makeTopEdge()),
		};
		Technology.TechPoint [] box_17 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge()),
			new Technology.TechPoint(EdgeH.fromCenter(1), EdgeV.makeTopEdge()),
		};
		Technology.TechPoint [] box_18 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(1), EdgeV.fromBottom(1)),
			new Technology.TechPoint(EdgeH.fromLeft(3), EdgeV.fromBottom(3)),
		};
		Technology.TechPoint [] box_19 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(1), EdgeV.fromBottom(1)),
			new Technology.TechPoint(EdgeH.fromRight(1), EdgeV.fromTop(1)),
		};
		Technology.TechPoint [] box_20 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge()),
			new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.makeTopEdge()),
		};

		//******************** NODES ********************

		/** Metal-Pin */
		PrimitiveNode mp_node = PrimitiveNode.newInstance("Metal-Pin", this, 3, 3, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(PM_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, box_20)
			});
		mp_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, mp_node, new ArcProto [] {Metal_arc}, "metal", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
			});
		mp_node.setFunction(PrimitiveNode.Function.PIN);
		mp_node.setArcsWipe();
		mp_node.setArcsShrink();

		/** Polysilicon-Pin */
		PrimitiveNode pp_node = PrimitiveNode.newInstance("Polysilicon-Pin", this, 2, 2, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(PP_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, box_20)
			});
		pp_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, pp_node, new ArcProto [] {Polysilicon_arc}, "polysilicon", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(1), EdgeV.fromBottom(1), EdgeH.fromRight(1), EdgeV.fromTop(1))
			});
		pp_node.setFunction(PrimitiveNode.Function.PIN);
		pp_node.setArcsWipe();
		pp_node.setArcsShrink();

		/** Diffusion-Pin */
		PrimitiveNode dp_node = PrimitiveNode.newInstance("Diffusion-Pin", this, 2, 2, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(PD_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, box_20)
			});
		dp_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, dp_node, new ArcProto [] {Diffusion_arc}, "diffusion", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(1), EdgeV.fromBottom(1), EdgeH.fromRight(1), EdgeV.fromTop(1))
			});
		dp_node.setFunction(PrimitiveNode.Function.PIN);
		dp_node.setArcsWipe();
		dp_node.setArcsShrink();

		/** Metal-Polysilicon-Con */
		PrimitiveNode mpc_node = PrimitiveNode.newInstance("Metal-Polysilicon-Con", this, 4, 4, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(P_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_20),
				new Technology.NodeLayer(M_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_20),
				new Technology.NodeLayer(CC_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, box_18)
			});
		mpc_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, mpc_node, new ArcProto [] {Polysilicon_arc, Metal_arc}, "metal-poly", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(1), EdgeV.fromBottom(1), EdgeH.fromRight(1), EdgeV.fromTop(1))
			});
		mpc_node.setFunction(PrimitiveNode.Function.CONTACT);
		mpc_node.setSpecialType(PrimitiveNode.MULTICUT);
		mpc_node.setSpecialValues(new double [] {2, 2, 1, 1, 2, 2});

		/** Metal-Diffusion-Con */
		PrimitiveNode mdc_node = PrimitiveNode.newInstance("Metal-Diffusion-Con", this, 4, 4, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(D_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_20),
				new Technology.NodeLayer(M_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_20),
				new Technology.NodeLayer(CC_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, box_18)
			});
		mdc_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, mdc_node, new ArcProto [] {Diffusion_arc, Metal_arc}, "metal-diff", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(1), EdgeV.fromBottom(1), EdgeH.fromRight(1), EdgeV.fromTop(1))
			});
		mdc_node.setFunction(PrimitiveNode.Function.CONTACT);
		mdc_node.setSpecialType(PrimitiveNode.MULTICUT);
		mdc_node.setSpecialValues(new double [] {2, 2, 1, 1, 2, 2});

		/** Butting-Con */
		PrimitiveNode bc_node = PrimitiveNode.newInstance("Butting-Con", this, 6, 4, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(D_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_17),
				new Technology.NodeLayer(M_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_20),
				new Technology.NodeLayer(P_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_16),
				new Technology.NodeLayer(CC_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, box_19)
			});
		bc_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, bc_node, new ArcProto [] {Diffusion_arc, Metal_arc}, "but-diff", 180,90, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(1), EdgeV.fromBottom(1), EdgeH.fromLeft(3), EdgeV.fromTop(1)),
				PrimitivePort.newInstance(this, bc_node, new ArcProto [] {Polysilicon_arc, Metal_arc}, "but-poly", 0,90, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromRight(2), EdgeV.fromBottom(1), EdgeH.fromRight(1), EdgeV.fromTop(1))
			});
		bc_node.setFunction(PrimitiveNode.Function.CONNECT);

		/** Buried-Con-Cross */
		PrimitiveNode bcc_node = PrimitiveNode.newInstance("Buried-Con-Cross", this, 6, 4, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(P_lay, 2, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_15),
				new Technology.NodeLayer(D_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_14),
				new Technology.NodeLayer(BC_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_20)
			});
		bcc_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, bcc_node, new ArcProto [] {Diffusion_arc}, "bur-diff-right", 0,45, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromRight(1), EdgeV.fromBottom(2), EdgeH.fromRight(1), EdgeV.fromTop(2)),
				PrimitivePort.newInstance(this, bcc_node, new ArcProto [] {Diffusion_arc}, "bur-diff-left", 180,45, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(1), EdgeV.fromBottom(2), EdgeH.fromLeft(1), EdgeV.fromTop(2)),
				PrimitivePort.newInstance(this, bcc_node, new ArcProto [] {Polysilicon_arc}, "bur-poly-bottom", 270,45, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(3), EdgeV.fromBottom(1), EdgeH.fromRight(3), EdgeV.fromBottom(1)),
				PrimitivePort.newInstance(this, bcc_node, new ArcProto [] {Polysilicon_arc}, "bur-poly-top", 90,45, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(3), EdgeV.fromTop(1), EdgeH.fromRight(3), EdgeV.fromTop(1))
			});
		bcc_node.setFunction(PrimitiveNode.Function.CONNECT);

		/** Transistor */
		PrimitiveNode t_node = PrimitiveNode.newInstance("Transistor", this, 6, 6, new SizeOffset(2, 2, 2, 2),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(P_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_13, 1, 1, 2, 2),
				new Technology.NodeLayer(D_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_15, 3, 3, 0, 0)
			});
		t_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, t_node, new ArcProto [] {Polysilicon_arc}, "trans-poly-left", 180,85, 1, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(1), EdgeV.fromBottom(3), EdgeH.fromLeft(1), EdgeV.fromTop(3)),
				PrimitivePort.newInstance(this, t_node, new ArcProto [] {Diffusion_arc}, "trans-diff-top", 90,85, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(3), EdgeV.fromTop(1), EdgeH.fromRight(3), EdgeV.fromTop(1)),
				PrimitivePort.newInstance(this, t_node, new ArcProto [] {Polysilicon_arc}, "trans-poly-right", 0,85, 1, PortCharacteristic.UNKNOWN,
					EdgeH.fromRight(1), EdgeV.fromBottom(3), EdgeH.fromRight(1), EdgeV.fromTop(3)),
				PrimitivePort.newInstance(this, t_node, new ArcProto [] {Diffusion_arc}, "trans-diff-bottom", 270,85, 3, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(3), EdgeV.fromBottom(1), EdgeH.fromRight(3), EdgeV.fromBottom(1))
			});
		t_node.setFunction(PrimitiveNode.Function.TRANMOS);
		t_node.setHoldsOutline();
		t_node.setCanShrink();
		t_node.setSpecialType(PrimitiveNode.SERPTRANS);
		t_node.setSpecialValues(new double [] {0.025, 1, 1, 2, 1, 1});

		/** Implant-Transistor */
		PrimitiveNode it_node = PrimitiveNode.newInstance("Implant-Transistor", this, 6, 6, new SizeOffset(2, 2, 2, 2),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(P_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_13, 1, 1, 2, 2),
				new Technology.NodeLayer(I_lay, -1, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_10, 2.5, 2.5, 1.5, 1.5),
				new Technology.NodeLayer(D_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_15, 3, 3, 0, 0)
			});
		it_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, it_node, new ArcProto [] {Polysilicon_arc}, "imp-trans-poly-left", 180,85, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(1), EdgeV.fromBottom(3), EdgeH.fromLeft(1), EdgeV.fromTop(3)),
				PrimitivePort.newInstance(this, it_node, new ArcProto [] {Diffusion_arc}, "imp-trans-diff-top", 90,85, 2, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(3), EdgeV.fromTop(1), EdgeH.fromRight(3), EdgeV.fromTop(1)),
				PrimitivePort.newInstance(this, it_node, new ArcProto [] {Polysilicon_arc}, "imp-trans-poly-right", 0,85, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromRight(1), EdgeV.fromBottom(3), EdgeH.fromRight(1), EdgeV.fromTop(3)),
				PrimitivePort.newInstance(this, it_node, new ArcProto [] {Diffusion_arc}, "imp-trans-diff-bottom", 270,85, 3, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(3), EdgeV.fromBottom(1), EdgeH.fromRight(3), EdgeV.fromBottom(1))
			});
		it_node.setFunction(PrimitiveNode.Function.TRADMOS);
		it_node.setHoldsOutline();
		it_node.setCanShrink();
		it_node.setSpecialType(PrimitiveNode.SERPTRANS);
		it_node.setSpecialValues(new double [] {0.0333333, 1, 1, 2, 1, 1});

		/** Buried-Con-Cross-S */
		PrimitiveNode bccs_node = PrimitiveNode.newInstance("Buried-Con-Cross-S", this, 4, 4, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(D_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_14),
				new Technology.NodeLayer(BC_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_20),
				new Technology.NodeLayer(P_lay, 1, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_9)
			});
		bccs_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, bccs_node, new ArcProto [] {Diffusion_arc}, "bur-diff-left", 180,45, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(1), EdgeV.fromBottom(2), EdgeH.fromLeft(1), EdgeV.fromTop(2)),
				PrimitivePort.newInstance(this, bccs_node, new ArcProto [] {Polysilicon_arc}, "bur-poly-bottom", 270,45, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(3), EdgeV.fromBottom(1), EdgeH.fromRight(1), EdgeV.fromBottom(1)),
				PrimitivePort.newInstance(this, bccs_node, new ArcProto [] {Polysilicon_arc}, "bur-poly-top", 90,45, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(3), EdgeV.fromTop(1), EdgeH.fromRight(1), EdgeV.fromTop(1)),
				PrimitivePort.newInstance(this, bccs_node, new ArcProto [] {Diffusion_arc, Polysilicon_arc}, "bur-end-right", 0,45, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(3), EdgeV.fromBottom(2), EdgeH.fromRight(1), EdgeV.fromTop(2))
			});
		bccs_node.setFunction(PrimitiveNode.Function.CONNECT);

		/** Buried-Con-Cross-T */
		PrimitiveNode bcct_node = PrimitiveNode.newInstance("Buried-Con-Cross-T", this, 5, 4, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(D_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_14),
				new Technology.NodeLayer(BC_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_20),
				new Technology.NodeLayer(P_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_8)
			});
		bcct_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, bcct_node, new ArcProto [] {Polysilicon_arc}, "bur-poly-top", 90,45, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(3), EdgeV.fromTop(1), EdgeH.fromRight(2), EdgeV.fromTop(1)),
				PrimitivePort.newInstance(this, bcct_node, new ArcProto [] {Polysilicon_arc}, "bur-poly-bottom", 270,45, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(3), EdgeV.fromBottom(1), EdgeH.fromRight(2), EdgeV.fromBottom(1)),
				PrimitivePort.newInstance(this, bcct_node, new ArcProto [] {Diffusion_arc}, "bur-diff-left", 180,45, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(1), EdgeV.fromBottom(2), EdgeH.fromLeft(1), EdgeV.fromTop(2))
			});
		bcct_node.setFunction(PrimitiveNode.Function.CONNECT);

		/** Buried-Con-Polysurr */
		PrimitiveNode bcp_node = PrimitiveNode.newInstance("Buried-Con-Polysurr", this, 5, 4, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(P_lay, 1, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_9),
				new Technology.NodeLayer(BC_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_20),
				new Technology.NodeLayer(D_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_7)
			});
		bcp_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, bcp_node, new ArcProto [] {Diffusion_arc}, "bur-diff-left", 180,45, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(1), EdgeV.fromBottom(2), EdgeH.fromLeft(1), EdgeV.fromTop(2)),
				PrimitivePort.newInstance(this, bcp_node, new ArcProto [] {Polysilicon_arc}, "bur-poly-3", 0,135, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(3), EdgeV.fromBottom(1), EdgeH.fromRight(1), EdgeV.fromTop(1)),
				PrimitivePort.newInstance(this, bcp_node, new ArcProto [] {Polysilicon_arc}, "bur-poly-1", 0,135, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(3), EdgeV.fromBottom(1), EdgeH.fromRight(1), EdgeV.fromTop(1)),
				PrimitivePort.newInstance(this, bcp_node, new ArcProto [] {Polysilicon_arc}, "bur-poly-2", 0,135, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(3), EdgeV.fromBottom(1), EdgeH.fromRight(1), EdgeV.fromTop(1))
			});
		bcp_node.setFunction(PrimitiveNode.Function.CONNECT);

		/** Buried-Con-Diffsurr-I */
		PrimitiveNode bcdi_node = PrimitiveNode.newInstance("Buried-Con-Diffsurr-I", this, 5, 4, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(P_lay, 1, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_6),
				new Technology.NodeLayer(D_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.POINTS, box_5),
				new Technology.NodeLayer(BC_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_20)
			});
		bcdi_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, bcdi_node, new ArcProto [] {Diffusion_arc}, "bur-diff-left", 180,45, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(1), EdgeV.fromBottom(2), EdgeH.fromLeft(1), EdgeV.fromTop(2)),
				PrimitivePort.newInstance(this, bcdi_node, new ArcProto [] {Polysilicon_arc}, "bur-poly-right", 0,45, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromRight(1), EdgeV.fromBottom(2), EdgeH.fromRight(1), EdgeV.fromTop(2))
			});
		bcdi_node.setFunction(PrimitiveNode.Function.CONNECT);

		/** Buried-Con-Diffsurr-T */
		PrimitiveNode bcdt_node = PrimitiveNode.newInstance("Buried-Con-Diffsurr-T", this, 6, 4, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(P_lay, 1, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_4),
				new Technology.NodeLayer(D_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.POINTS, box_3),
				new Technology.NodeLayer(BC_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_20)
			});
		bcdt_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, bcdt_node, new ArcProto [] {Diffusion_arc}, "bur-diff-left", 180,45, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(1), EdgeV.fromBottom(1), EdgeH.fromLeft(1), EdgeV.fromTop(3)),
				PrimitivePort.newInstance(this, bcdt_node, new ArcProto [] {Polysilicon_arc}, "bur-poly-top", 90,45, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(3), EdgeV.fromTop(1), EdgeH.fromRight(3), EdgeV.fromTop(1)),
				PrimitivePort.newInstance(this, bcdt_node, new ArcProto [] {Diffusion_arc}, "bur-diff-right", 0,45, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromRight(1), EdgeV.fromBottom(1), EdgeH.fromRight(1), EdgeV.fromTop(3))
			});
		bcdt_node.setFunction(PrimitiveNode.Function.CONNECT);

		/** Buried-Con-Diffsurr-L */
		PrimitiveNode bcdl_node = PrimitiveNode.newInstance("Buried-Con-Diffsurr-L", this, 5, 4, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(BC_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_20),
				new Technology.NodeLayer(P_lay, 1, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_2),
				new Technology.NodeLayer(D_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.POINTS, box_1)
			});
		bcdl_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, bcdl_node, new ArcProto [] {Diffusion_arc}, "bur-diff-left", 180,45, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(1), EdgeV.fromBottom(1), EdgeH.fromLeft(1), EdgeV.fromTop(3)),
				PrimitivePort.newInstance(this, bcdl_node, new ArcProto [] {Polysilicon_arc}, "bur-poly-top", 90,45, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(3), EdgeV.fromTop(1), EdgeH.fromRight(2), EdgeV.fromTop(1))
			});
		bcdl_node.setFunction(PrimitiveNode.Function.CONNECT);

		/** Metal-Node */
		PrimitiveNode mn_node = PrimitiveNode.newInstance("Metal-Node", this, 3, 3, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(M_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_20)
			});
		mn_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, mn_node, new ArcProto [] {Metal_arc}, "metal", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		mn_node.setFunction(PrimitiveNode.Function.NODE);
		mn_node.setHoldsOutline();
		mn_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Polysilicon-Node */
		PrimitiveNode pn_node = PrimitiveNode.newInstance("Polysilicon-Node", this, 2, 2, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(P_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_20)
			});
		pn_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, pn_node, new ArcProto [] {Polysilicon_arc}, "polysilicon", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		pn_node.setFunction(PrimitiveNode.Function.NODE);
		pn_node.setHoldsOutline();
		pn_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Diffusion-Node */
		PrimitiveNode dn_node = PrimitiveNode.newInstance("Diffusion-Node", this, 2, 2, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(D_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_20)
			});
		dn_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, dn_node, new ArcProto [] {Diffusion_arc}, "diffusion", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		dn_node.setFunction(PrimitiveNode.Function.NODE);
		dn_node.setHoldsOutline();
		dn_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Implant-Node */
		PrimitiveNode in_node = PrimitiveNode.newInstance("Implant-Node", this, 2, 2, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(I_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_20)
			});
		in_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, in_node, new ArcProto [] {}, "implant", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		in_node.setFunction(PrimitiveNode.Function.NODE);
		in_node.setHoldsOutline();
		in_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Cut-Node */
		PrimitiveNode cn_node = PrimitiveNode.newInstance("Cut-Node", this, 2, 2, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(CC_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, box_20)
			});
		cn_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, cn_node, new ArcProto [] {}, "cut", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		cn_node.setFunction(PrimitiveNode.Function.NODE);
		cn_node.setHoldsOutline();
		cn_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Buried-Node */
		PrimitiveNode bn_node = PrimitiveNode.newInstance("Buried-Node", this, 2, 2, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(BC_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_20)
			});
		bn_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, bn_node, new ArcProto [] {}, "buried", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		bn_node.setFunction(PrimitiveNode.Function.NODE);
		bn_node.setHoldsOutline();
		bn_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Overglass-Node */
		PrimitiveNode on_node = PrimitiveNode.newInstance("Overglass-Node", this, 2, 2, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(O_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_20)
			});
		on_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, on_node, new ArcProto [] {}, "overglass", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		on_node.setFunction(PrimitiveNode.Function.NODE);
		on_node.setHoldsOutline();
		on_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Light-Implant-Node */
		PrimitiveNode lin_node = PrimitiveNode.newInstance("Light-Implant-Node", this, 2, 2, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(LI_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_20)
			});
		lin_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, lin_node, new ArcProto [] {}, "light-implant", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		lin_node.setFunction(PrimitiveNode.Function.NODE);
		lin_node.setHoldsOutline();
		lin_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Oversize-Cut-Node */
		PrimitiveNode ocn_node = PrimitiveNode.newInstance("Oversize-Cut-Node", this, 2, 2, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(OC_lay, 0, Poly.Type.CLOSED, Technology.NodeLayer.BOX, box_20)
			});
		ocn_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, ocn_node, new ArcProto [] {}, "oversize-contact", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		ocn_node.setFunction(PrimitiveNode.Function.NODE);
		ocn_node.setHoldsOutline();
		ocn_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Hard-Enhancement-Node */
		PrimitiveNode hen_node = PrimitiveNode.newInstance("Hard-Enhancement-Node", this, 2, 2, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(HE_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_20)
			});
		hen_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, hen_node, new ArcProto [] {}, "hard-enhancement", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		hen_node.setFunction(PrimitiveNode.Function.NODE);
		hen_node.setHoldsOutline();
		hen_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Light-Enhancement-Node */
		PrimitiveNode len_node = PrimitiveNode.newInstance("Light-Enhancement-Node", this, 2, 2, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(LE_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_20)
			});
		len_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, len_node, new ArcProto [] {}, "light-enhancement", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		len_node.setFunction(PrimitiveNode.Function.NODE);
		len_node.setHoldsOutline();
		len_node.setSpecialType(PrimitiveNode.POLYGONAL);

		// The pure layer nodes
		M_lay.setPureLayerNode(mn_node);		// Metal
		P_lay.setPureLayerNode(pn_node);		// Polysilicon
		D_lay.setPureLayerNode(dn_node);		// Diffusion
		I_lay.setPureLayerNode(in_node);		// Implant
		CC_lay.setPureLayerNode(cn_node);		// Contact-Cut
		BC_lay.setPureLayerNode(bn_node);		// Buried-Contact
		O_lay.setPureLayerNode(on_node);		// Overglass
		LI_lay.setPureLayerNode(lin_node);		// Light-Implant
		OC_lay.setPureLayerNode(ocn_node);		// Oversize-Contact
		HE_lay.setPureLayerNode(hen_node);		// Hard-Enhancement
		LE_lay.setPureLayerNode(len_node);		// Light-Enhancement

        // Building information for palette
        nodeGroups = new Object[7][3];
        int count = -1;

        nodeGroups[++count][0] = Polysilicon_arc; nodeGroups[count][1] = pp_node; nodeGroups[count][2] = mpc_node;
        nodeGroups[++count][0] = Metal_arc; nodeGroups[count][1] = mp_node; nodeGroups[count][2] = bc_node;
        nodeGroups[++count][0] = Diffusion_arc; nodeGroups[count][1] = dp_node; nodeGroups[count][2] = mdc_node;
        nodeGroups[++count][0] = bcc_node; nodeGroups[count][1] = t_node; nodeGroups[count][2] = it_node;
        nodeGroups[++count][0] = bccs_node; nodeGroups[count][1] = bcct_node; nodeGroups[count][2] = bcp_node;
        nodeGroups[++count][0] = bcdi_node; nodeGroups[count][1] = bcdt_node; nodeGroups[count][2] = bcdl_node;
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
