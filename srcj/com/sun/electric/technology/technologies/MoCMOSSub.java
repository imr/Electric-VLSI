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
import java.awt.Color;
import java.awt.geom.Point2D;

/**
 * This is the Complementary MOS (old, from MOSIS, Submicron, 2-6 metals [now 6], double poly, converts to newer MOCMOS) Technology.
 */
public class MoCMOSSub extends Technology
{
	/** the Complementary MOS (old, from MOSIS, Submicron, 2-6 metals [now 6], double poly, converts to newer MOCMOS) Technology object. */	public static final MoCMOSSub tech = new MoCMOSSub();

/** defines the 1st transparent layer. */			private static final int TRANSPARENT_1 = 1;
/** defines the 2nd transparent layer. */			private static final int TRANSPARENT_2 = 2;
/** defines the 3rd transparent layer. */			private static final int TRANSPARENT_3 = 3;
/** defines the 4th transparent layer. */			private static final int TRANSPARENT_4 = 4;
/** defines the 5th transparent layer. */			private static final int TRANSPARENT_5 = 5;

	// -------------------- private and protected methods ------------------------
	private MoCMOSSub()
	{
		setTechName("mocmossub");
		setTechDesc("Complementary MOS (old, from MOSIS, Submicron, 2-6 metals [now 6], double poly, converts to newer MOCMOS)");
		setScale(200);   // in nanometers: really 0.2 microns
		setNoNegatedArcs();
		setStaticTechnology();
		setNumTransparentLayers(5);
		setColorMap(new Color []
		{
			new Color(200,200,200), //  0:        +             +        +       +       
			new Color( 96,209,255), //  1: Metal-1+             +        +       +       
			new Color(255,155,192), //  2:        +Polysilicon-1+        +       +       
			new Color(111,144,177), //  3: Metal-1+Polysilicon-1+        +       +       
			new Color(107,226, 96), //  4:        +             +P-Active+       +       
			new Color( 83,179,160), //  5: Metal-1+             +P-Active+       +       
			new Color(161,151,126), //  6:        +Polysilicon-1+P-Active+       +       
			new Color(110,171,152), //  7: Metal-1+Polysilicon-1+P-Active+       +       
			new Color(224, 95,255), //  8:        +             +        +Metal-2+       
			new Color(135,100,191), //  9: Metal-1+             +        +Metal-2+       
			new Color(170, 83,170), // 10:        +Polysilicon-1+        +Metal-2+       
			new Color(152,104,175), // 11: Metal-1+Polysilicon-1+        +Metal-2+       
			new Color(150,124,163), // 12:        +             +P-Active+Metal-2+       
			new Color(129,144,165), // 13: Metal-1+             +P-Active+Metal-2+       
			new Color(155,133,151), // 14:        +Polysilicon-1+P-Active+Metal-2+       
			new Color(141,146,153), // 15: Metal-1+Polysilicon-1+P-Active+Metal-2+       
			new Color(247,251, 20), // 16:        +             +        +       +Metal-3
			new Color(154,186, 78), // 17: Metal-1+             +        +       +Metal-3
			new Color(186,163, 57), // 18:        +Polysilicon-1+        +       +Metal-3
			new Color(167,164, 99), // 19: Metal-1+Polysilicon-1+        +       +Metal-3
			new Color(156,197, 41), // 20:        +             +P-Active+       +Metal-3
			new Color(138,197, 83), // 21: Metal-1+             +P-Active+       +Metal-3
			new Color(161,184, 69), // 22:        +Polysilicon-1+P-Active+       +Metal-3
			new Color(147,183, 97), // 23: Metal-1+Polysilicon-1+P-Active+       +Metal-3
			new Color(186,155, 76), // 24:        +             +        +Metal-2+Metal-3
			new Color(155,163,119), // 25: Metal-1+             +        +Metal-2+Metal-3
			new Color(187,142, 97), // 26:        +Polysilicon-1+        +Metal-2+Metal-3
			new Color(165,146,126), // 27: Metal-1+Polysilicon-1+        +Metal-2+Metal-3
			new Color(161,178, 82), // 28:        +             +P-Active+Metal-2+Metal-3
			new Color(139,182,111), // 29: Metal-1+             +P-Active+Metal-2+Metal-3
			new Color(162,170, 97), // 30:        +Polysilicon-1+P-Active+Metal-2+Metal-3
			new Color(147,172,116), // 31: Metal-1+Polysilicon-1+P-Active+Metal-2+Metal-3
		});

		//**************************************** LAYERS ****************************************

		/** M layer */
		Layer M_lay = Layer.newInstance(this, "Metal-1",
			new EGraphics(EGraphics.SOLIDC, EGraphics.SOLIDC, TRANSPARENT_1, 107,226,96,0.8,1,
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
			new EGraphics(EGraphics.SOLIDC, EGraphics.SOLIDC, TRANSPARENT_4, 0,0,0,0.8,1,
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
			new EGraphics(EGraphics.SOLIDC, EGraphics.SOLIDC, TRANSPARENT_5, 0,0,0,0.8,1,
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
			new EGraphics(EGraphics.PATTERNED, EGraphics.PATTERNED, 0, 0,0,0,0.8,1,
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
			new EGraphics(EGraphics.PATTERNED, EGraphics.PATTERNED, 0, 0,0,0,0.8,1,
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
			new EGraphics(EGraphics.PATTERNED, EGraphics.PATTERNED, 0, 161,184,69,0.8,1,
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
			new EGraphics(EGraphics.SOLIDC, EGraphics.SOLIDC, TRANSPARENT_2, 224,95,255,0.8,1,
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
			new EGraphics(EGraphics.PATTERNED, EGraphics.PATTERNED, 0, 0,0,0,0.8,1,
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
			new EGraphics(EGraphics.SOLIDC, EGraphics.SOLIDC, TRANSPARENT_3, 247,251,20,0.8,1,
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
			new EGraphics(EGraphics.SOLIDC, EGraphics.SOLIDC, TRANSPARENT_3, 247,251,20,0.8,1,
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
			new EGraphics(EGraphics.PATTERNED, EGraphics.PATTERNED, 0, 162,170,97,0.8,1,
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
			new EGraphics(EGraphics.PATTERNED, EGraphics.PATTERNED, 0, 162,170,97,0.8,1,
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
			new EGraphics(EGraphics.PATTERNED, EGraphics.PATTERNED, 0, 0,0,0,0.8,1,
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
			new EGraphics(EGraphics.PATTERNED, EGraphics.PATTERNED, 0, 0,0,0,0.8,1,
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
			new EGraphics(EGraphics.SOLIDC, EGraphics.SOLIDC, 0, 161,151,126,0.8,1,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** AC layer */
		Layer AC_lay = Layer.newInstance(this, "Active-Cut",
			new EGraphics(EGraphics.SOLIDC, EGraphics.SOLIDC, 0, 161,151,126,0.8,1,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** V layer */
		Layer V_lay = Layer.newInstance(this, "Via1",
			new EGraphics(EGraphics.SOLIDC, EGraphics.SOLIDC, 0, 0,0,0,0.8,1,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** V0 layer */
		Layer V0_lay = Layer.newInstance(this, "Via2",
			new EGraphics(EGraphics.SOLIDC, EGraphics.SOLIDC, 0, 0,0,0,0.8,1,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** V1 layer */
		Layer V1_lay = Layer.newInstance(this, "Via3",
			new EGraphics(EGraphics.SOLIDC, EGraphics.SOLIDC, 0, 0,0,0,0.8,1,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** V2 layer */
		Layer V2_lay = Layer.newInstance(this, "Via4",
			new EGraphics(EGraphics.SOLIDC, EGraphics.SOLIDC, 0, 0,0,0,0.8,1,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** V3 layer */
		Layer V3_lay = Layer.newInstance(this, "Via5",
			new EGraphics(EGraphics.SOLIDC, EGraphics.SOLIDC, 0, 0,0,0,0.8,1,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** P1 layer */
		Layer P1_lay = Layer.newInstance(this, "Passivation",
			new EGraphics(EGraphics.PATTERNED, EGraphics.PATTERNED, 0, 0,0,0,0.8,1,
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
			new EGraphics(EGraphics.SOLIDC, EGraphics.SOLIDC, 0, 200,200,200,0.8,1,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** PC0 layer */
		Layer PC0_lay = Layer.newInstance(this, "Poly-Cap",
			new EGraphics(EGraphics.SOLIDC, EGraphics.SOLIDC, 0, 161,151,126,0.8,1,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** PAW layer */
		Layer PAW_lay = Layer.newInstance(this, "P-Active-Well",
			new EGraphics(EGraphics.SOLIDC, EGraphics.SOLIDC, TRANSPARENT_3, 247,251,20,0.8,1,
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
			new EGraphics(EGraphics.SOLIDC, EGraphics.SOLIDC, TRANSPARENT_1, 107,226,96,0.8,1,
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
			new EGraphics(EGraphics.SOLIDC, EGraphics.SOLIDC, TRANSPARENT_4, 0,0,0,0.8,1,
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
			new EGraphics(EGraphics.SOLIDC, EGraphics.SOLIDC, TRANSPARENT_5, 0,0,0,0.8,1,
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
			new EGraphics(EGraphics.PATTERNED, EGraphics.PATTERNED, 0, 0,0,0,0.8,1,
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
			new EGraphics(EGraphics.PATTERNED, EGraphics.PATTERNED, 0, 0,0,0,0.8,1,
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
			new EGraphics(EGraphics.PATTERNED, EGraphics.PATTERNED, 0, 161,184,69,0.8,1,
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
			new EGraphics(EGraphics.SOLIDC, EGraphics.SOLIDC, TRANSPARENT_2, 224,95,255,0.8,1,
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
			new EGraphics(EGraphics.PATTERNED, EGraphics.PATTERNED, 0, 0,0,0,0.8,1,
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
			new EGraphics(EGraphics.SOLIDC, EGraphics.SOLIDC, TRANSPARENT_3, 247,251,20,0.8,1,
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
			new EGraphics(EGraphics.SOLIDC, EGraphics.SOLIDC, TRANSPARENT_3, 247,251,20,0.8,1,
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
			new EGraphics(EGraphics.PATTERNED, EGraphics.PATTERNED, 0, 162,170,97,0.8,1,
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
			new EGraphics(EGraphics.PATTERNED, EGraphics.PATTERNED, 0, 162,170,97,0.8,1,
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
			new EGraphics(EGraphics.PATTERNED, EGraphics.PATTERNED, 0, 0,0,0,0.8,1,
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
			new EGraphics(EGraphics.PATTERNED, EGraphics.PATTERNED, 0, 0,0,0,0.8,1,
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
			new EGraphics(EGraphics.SOLIDC, EGraphics.SOLIDC, 0, 170,83,170,0.8,1,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		// The layer functions
		M_lay.setFunction(Layer.Function.METAL1);		// Metal-1
		M0_lay.setFunction(Layer.Function.METAL2);		// Metal-2
		M1_lay.setFunction(Layer.Function.METAL3);		// Metal-3
		M2_lay.setFunction(Layer.Function.METAL4);		// Metal-4
		M3_lay.setFunction(Layer.Function.METAL5);		// Metal-5
		M4_lay.setFunction(Layer.Function.METAL6);		// Metal-6
		P_lay.setFunction(Layer.Function.POLY1);		// Polysilicon-1
		P0_lay.setFunction(Layer.Function.POLY2);		// Polysilicon-2
		PA_lay.setFunction(Layer.Function.DIFFP);		// P-Active
		NA_lay.setFunction(Layer.Function.DIFFN);		// N-Active
		PS_lay.setFunction(Layer.Function.IMPLANTP);		// P-Select
		NS_lay.setFunction(Layer.Function.IMPLANTN);		// N-Select
		PW_lay.setFunction(Layer.Function.WELLP);		// P-Well
		NW_lay.setFunction(Layer.Function.WELLN);		// N-Well
		PC_lay.setFunction(Layer.Function.CONTACT1, Layer.Function.CONPOLY);		// Poly-Cut
		AC_lay.setFunction(Layer.Function.CONTACT1, Layer.Function.CONDIFF);		// Active-Cut
		V_lay.setFunction(Layer.Function.CONTACT2, Layer.Function.CONMETAL);		// Via1
		V0_lay.setFunction(Layer.Function.CONTACT3, Layer.Function.CONMETAL);		// Via2
		V1_lay.setFunction(Layer.Function.CONTACT4, Layer.Function.CONMETAL);		// Via3
		V2_lay.setFunction(Layer.Function.CONTACT5, Layer.Function.CONMETAL);		// Via4
		V3_lay.setFunction(Layer.Function.CONTACT6, Layer.Function.CONMETAL);		// Via5
		P1_lay.setFunction(Layer.Function.OVERGLASS);		// Passivation
		T_lay.setFunction(Layer.Function.TRANSISTOR, Layer.Function.PSEUDO);		// Transistor
		PC0_lay.setFunction(Layer.Function.CAP);		// Poly-Cap
		PAW_lay.setFunction(Layer.Function.DIFFP);		// P-Active-Well
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
		PPS_lay.setFunction(Layer.Function.IMPLANTP, Layer.Function.PSEUDO);		// Pseudo-P-Select
		PNS_lay.setFunction(Layer.Function.IMPLANTN, Layer.Function.PSEUDO);		// Pseudo-N-Select
		PPW_lay.setFunction(Layer.Function.WELLP, Layer.Function.PSEUDO);		// Pseudo-P-Well
		PNW_lay.setFunction(Layer.Function.WELLN, Layer.Function.PSEUDO);		// Pseudo-N-Well
		PF_lay.setFunction(Layer.Function.ART);		// Pad-Frame

		// The CIF names
		M_lay.setCIFLayer("CMF");		// Metal-1
		M0_lay.setCIFLayer("CMS");		// Metal-2
		M1_lay.setCIFLayer("CMT");		// Metal-3
		M2_lay.setCIFLayer("CMQ");		// Metal-4
		M3_lay.setCIFLayer("CMP");		// Metal-5
		M4_lay.setCIFLayer("CM6");		// Metal-6
		P_lay.setCIFLayer("CPG");		// Polysilicon-1
		P0_lay.setCIFLayer("CEL");		// Polysilicon-2
		PA_lay.setCIFLayer("CAA");		// P-Active
		NA_lay.setCIFLayer("CAA");		// N-Active
		PS_lay.setCIFLayer("CSP");		// P-Select
		NS_lay.setCIFLayer("CSN");		// N-Select
		PW_lay.setCIFLayer("CWP");		// P-Well
		NW_lay.setCIFLayer("CWN");		// N-Well
		PC_lay.setCIFLayer("CCG");		// Poly-Cut
		AC_lay.setCIFLayer("CCG");		// Active-Cut
		V_lay.setCIFLayer("CVA");		// Via1
		V0_lay.setCIFLayer("CVS");		// Via2
		V1_lay.setCIFLayer("CVT");		// Via3
		V2_lay.setCIFLayer("CVQ");		// Via4
		V3_lay.setCIFLayer("CV5");		// Via5
		P1_lay.setCIFLayer("COG");		// Passivation
		T_lay.setCIFLayer("");		// Transistor
		PC0_lay.setCIFLayer("CPC");		// Poly-Cap
		PAW_lay.setCIFLayer("CAA");		// P-Active-Well
		PM_lay.setCIFLayer("");		// Pseudo-Metal-1
		PM0_lay.setCIFLayer("");		// Pseudo-Metal-2
		PM1_lay.setCIFLayer("");		// Pseudo-Metal-3
		PM2_lay.setCIFLayer("");		// Pseudo-Metal-4
		PM3_lay.setCIFLayer("");		// Pseudo-Metal-5
		PM4_lay.setCIFLayer("");		// Pseudo-Metal-6
		PP_lay.setCIFLayer("");		// Pseudo-Polysilicon
		PE_lay.setCIFLayer("");		// Pseudo-Electrode
		PPA_lay.setCIFLayer("");		// Pseudo-P-Active
		PNA_lay.setCIFLayer("");		// Pseudo-N-Active
		PPS_lay.setCIFLayer("CSP");		// Pseudo-P-Select
		PNS_lay.setCIFLayer("CSN");		// Pseudo-N-Select
		PPW_lay.setCIFLayer("CWP");		// Pseudo-P-Well
		PNW_lay.setCIFLayer("CWN");		// Pseudo-N-Well
		PF_lay.setCIFLayer("CX");		// Pad-Frame

		// The DXF names
		M_lay.setDXFLayer("");		// Metal-1
		M0_lay.setDXFLayer("");		// Metal-2
		M1_lay.setDXFLayer("");		// Metal-3
		M2_lay.setDXFLayer("");		// Metal-4
		M3_lay.setDXFLayer("");		// Metal-5
		M4_lay.setDXFLayer("");		// Metal-6
		P_lay.setDXFLayer("");		// Polysilicon-1
		P0_lay.setDXFLayer("");		// Polysilicon-2
		PA_lay.setDXFLayer("");		// P-Active
		NA_lay.setDXFLayer("");		// N-Active
		PS_lay.setDXFLayer("");		// P-Select
		NS_lay.setDXFLayer("");		// N-Select
		PW_lay.setDXFLayer("");		// P-Well
		NW_lay.setDXFLayer("");		// N-Well
		PC_lay.setDXFLayer("");		// Poly-Cut
		AC_lay.setDXFLayer("");		// Active-Cut
		V_lay.setDXFLayer("");		// Via1
		V0_lay.setDXFLayer("");		// Via2
		V1_lay.setDXFLayer("");		// Via3
		V2_lay.setDXFLayer("");		// Via4
		V3_lay.setDXFLayer("");		// Via5
		P1_lay.setDXFLayer("");		// Passivation
		T_lay.setDXFLayer("");		// Transistor
		PC0_lay.setDXFLayer("");		// Poly-Cap
		PAW_lay.setDXFLayer("");		// P-Active-Well
		PM_lay.setDXFLayer("");		// Pseudo-Metal-1
		PM0_lay.setDXFLayer("");		// Pseudo-Metal-2
		PM1_lay.setDXFLayer("");		// Pseudo-Metal-3
		PM2_lay.setDXFLayer("");		// Pseudo-Metal-4
		PM3_lay.setDXFLayer("");		// Pseudo-Metal-5
		PM4_lay.setDXFLayer("");		// Pseudo-Metal-6
		PP_lay.setDXFLayer("");		// Pseudo-Polysilicon
		PE_lay.setDXFLayer("");		// Pseudo-Electrode
		PPA_lay.setDXFLayer("");		// Pseudo-P-Active
		PNA_lay.setDXFLayer("");		// Pseudo-N-Active
		PPS_lay.setDXFLayer("");		// Pseudo-P-Select
		PNS_lay.setDXFLayer("");		// Pseudo-N-Select
		PPW_lay.setDXFLayer("");		// Pseudo-P-Well
		PNW_lay.setDXFLayer("");		// Pseudo-N-Well
		PF_lay.setDXFLayer("");		// Pad-Frame

		// The GDS names
		M_lay.setGDSLayer("49");		// Metal-1
		M0_lay.setGDSLayer("51");		// Metal-2
		M1_lay.setGDSLayer("62");		// Metal-3
		M2_lay.setGDSLayer("31");		// Metal-4
		M3_lay.setGDSLayer("33");		// Metal-5
		M4_lay.setGDSLayer("38");		// Metal-6
		P_lay.setGDSLayer("46");		// Polysilicon-1
		P0_lay.setGDSLayer("56");		// Polysilicon-2
		PA_lay.setGDSLayer("43");		// P-Active
		NA_lay.setGDSLayer("43");		// N-Active
		PS_lay.setGDSLayer("44");		// P-Select
		NS_lay.setGDSLayer("45");		// N-Select
		PW_lay.setGDSLayer("41");		// P-Well
		NW_lay.setGDSLayer("42");		// N-Well
		PC_lay.setGDSLayer("25");		// Poly-Cut
		AC_lay.setGDSLayer("25");		// Active-Cut
		V_lay.setGDSLayer("50");		// Via1
		V0_lay.setGDSLayer("61");		// Via2
		V1_lay.setGDSLayer("30");		// Via3
		V2_lay.setGDSLayer("32");		// Via4
		V3_lay.setGDSLayer("39");		// Via5
		P1_lay.setGDSLayer("52");		// Passivation
		T_lay.setGDSLayer("");		// Transistor
		PC0_lay.setGDSLayer("28");		// Poly-Cap
		PAW_lay.setGDSLayer("43");		// P-Active-Well
		PM_lay.setGDSLayer("");		// Pseudo-Metal-1
		PM0_lay.setGDSLayer("");		// Pseudo-Metal-2
		PM1_lay.setGDSLayer("");		// Pseudo-Metal-3
		PM2_lay.setGDSLayer("");		// Pseudo-Metal-4
		PM3_lay.setGDSLayer("");		// Pseudo-Metal-5
		PM4_lay.setGDSLayer("");		// Pseudo-Metal-6
		PP_lay.setGDSLayer("");		// Pseudo-Polysilicon
		PE_lay.setGDSLayer("");		// Pseudo-Electrode
		PPA_lay.setGDSLayer("");		// Pseudo-P-Active
		PNA_lay.setGDSLayer("");		// Pseudo-N-Active
		PPS_lay.setGDSLayer("");		// Pseudo-P-Select
		PNS_lay.setGDSLayer("");		// Pseudo-N-Select
		PPW_lay.setGDSLayer("");		// Pseudo-P-Well
		PNW_lay.setGDSLayer("");		// Pseudo-N-Well
		PF_lay.setGDSLayer("19");		// Pad-Frame

		// The layer height
		M_lay.setHeight(0, 17);		// Metal-1
		M0_lay.setHeight(0, 19);		// Metal-2
		M1_lay.setHeight(0, 21);		// Metal-3
		M2_lay.setHeight(0, 23);		// Metal-4
		M3_lay.setHeight(0, 25);		// Metal-5
		M4_lay.setHeight(0, 27);		// Metal-6
		P_lay.setHeight(0, 15);		// Polysilicon-1
		P0_lay.setHeight(0, 16);		// Polysilicon-2
		PA_lay.setHeight(0, 13);		// P-Active
		NA_lay.setHeight(0, 13);		// N-Active
		PS_lay.setHeight(0, 12);		// P-Select
		NS_lay.setHeight(0, 12);		// N-Select
		PW_lay.setHeight(0, 11);		// P-Well
		NW_lay.setHeight(0, 11);		// N-Well
		PC_lay.setHeight(2, 16);		// Poly-Cut
		AC_lay.setHeight(4, 15);		// Active-Cut
		V_lay.setHeight(2, 18);		// Via1
		V0_lay.setHeight(2, 20);		// Via2
		V1_lay.setHeight(2, 22);		// Via3
		V2_lay.setHeight(2, 24);		// Via4
		V3_lay.setHeight(2, 26);		// Via5
		P1_lay.setHeight(0, 30);		// Passivation
		T_lay.setHeight(0, 31);		// Transistor
		PC0_lay.setHeight(0, 28);		// Poly-Cap
		PAW_lay.setHeight(0, 29);		// P-Active-Well
		PM_lay.setHeight(0, 17);		// Pseudo-Metal-1
		PM0_lay.setHeight(0, 19);		// Pseudo-Metal-2
		PM1_lay.setHeight(0, 21);		// Pseudo-Metal-3
		PM2_lay.setHeight(0, 23);		// Pseudo-Metal-4
		PM3_lay.setHeight(0, 25);		// Pseudo-Metal-5
		PM4_lay.setHeight(0, 27);		// Pseudo-Metal-6
		PP_lay.setHeight(0, 12);		// Pseudo-Polysilicon
		PE_lay.setHeight(0, 13);		// Pseudo-Electrode
		PPA_lay.setHeight(0, 11);		// Pseudo-P-Active
		PNA_lay.setHeight(0, 11);		// Pseudo-N-Active
		PPS_lay.setHeight(0, 2);		// Pseudo-P-Select
		PNS_lay.setHeight(0, 2);		// Pseudo-N-Select
		PPW_lay.setHeight(0, 0);		// Pseudo-P-Well
		PNW_lay.setHeight(0, 0);		// Pseudo-N-Well
		PF_lay.setHeight(0, 33);		// Pad-Frame

		// The SPICE information
		M_lay.setDefaultParasitics(0.06f, 0.07f, 0);		// Metal-1
		M0_lay.setDefaultParasitics(0.06f, 0.04f, 0);		// Metal-2
		M1_lay.setDefaultParasitics(0.06f, 0.04f, 0);		// Metal-3
		M2_lay.setDefaultParasitics(0.03f, 0.04f, 0);		// Metal-4
		M3_lay.setDefaultParasitics(0.03f, 0.04f, 0);		// Metal-5
		M4_lay.setDefaultParasitics(0.03f, 0.04f, 0);		// Metal-6
		P_lay.setDefaultParasitics(2.5f, 0.09f, 0);		// Polysilicon-1
		P0_lay.setDefaultParasitics(50.0f, 1.0f, 0);		// Polysilicon-2
		PA_lay.setDefaultParasitics(2.5f, 0.9f, 0);		// P-Active
		NA_lay.setDefaultParasitics(3.0f, 0.9f, 0);		// N-Active
		PS_lay.setDefaultParasitics(0, 0, 0);		// P-Select
		NS_lay.setDefaultParasitics(0, 0, 0);		// N-Select
		PW_lay.setDefaultParasitics(0, 0, 0);		// P-Well
		NW_lay.setDefaultParasitics(0, 0, 0);		// N-Well
		PC_lay.setDefaultParasitics(2.2f, 0, 0);		// Poly-Cut
		AC_lay.setDefaultParasitics(2.5f, 0, 0);		// Active-Cut
		V_lay.setDefaultParasitics(1.0f, 0, 0);		// Via1
		V0_lay.setDefaultParasitics(0.9f, 0, 0);		// Via2
		V1_lay.setDefaultParasitics(0.8f, 0, 0);		// Via3
		V2_lay.setDefaultParasitics(0.8f, 0, 0);		// Via4
		V3_lay.setDefaultParasitics(0.8f, 0, 0);		// Via5
		P1_lay.setDefaultParasitics(0, 0, 0);		// Passivation
		T_lay.setDefaultParasitics(0, 0, 0);		// Transistor
		PC0_lay.setDefaultParasitics(0, 0, 0);		// Poly-Cap
		PAW_lay.setDefaultParasitics(0, 0, 0);		// P-Active-Well
		PM_lay.setDefaultParasitics(0, 0, 0);		// Pseudo-Metal-1
		PM0_lay.setDefaultParasitics(0, 0, 0);		// Pseudo-Metal-2
		PM1_lay.setDefaultParasitics(0, 0, 0);		// Pseudo-Metal-3
		PM2_lay.setDefaultParasitics(0, 0, 0);		// Pseudo-Metal-4
		PM3_lay.setDefaultParasitics(0, 0, 0);		// Pseudo-Metal-5
		PM4_lay.setDefaultParasitics(0, 0, 0);		// Pseudo-Metal-6
		PP_lay.setDefaultParasitics(0, 0, 0);		// Pseudo-Polysilicon
		PE_lay.setDefaultParasitics(0, 0, 0);		// Pseudo-Electrode
		PPA_lay.setDefaultParasitics(0, 0, 0);		// Pseudo-P-Active
		PNA_lay.setDefaultParasitics(0, 0, 0);		// Pseudo-N-Active
		PPS_lay.setDefaultParasitics(0, 0, 0);		// Pseudo-P-Select
		PNS_lay.setDefaultParasitics(0, 0, 0);		// Pseudo-N-Select
		PPW_lay.setDefaultParasitics(0, 0, 0);		// Pseudo-P-Well
		PNW_lay.setDefaultParasitics(0, 0, 0);		// Pseudo-N-Well
		PF_lay.setDefaultParasitics(0, 0, 0);		// Pad-Frame
		setDefaultParasitics(50, 50);

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

		/** Metal-3 arc */
		PrimitiveArc Metal_3_arc = PrimitiveArc.newInstance(this, "Metal-3", 3, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(M1_lay, 0, Poly.Type.FILLED)
		});
		Metal_3_arc.setFunction(PrimitiveArc.Function.METAL3);
		Metal_3_arc.setFixedAngle();
		Metal_3_arc.setWipable();
		Metal_3_arc.setAngleIncrement(90);

		/** Metal-4 arc */
		PrimitiveArc Metal_4_arc = PrimitiveArc.newInstance(this, "Metal-4", 3, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(M2_lay, 0, Poly.Type.FILLED)
		});
		Metal_4_arc.setFunction(PrimitiveArc.Function.METAL4);
		Metal_4_arc.setFixedAngle();
		Metal_4_arc.setWipable();
		Metal_4_arc.setAngleIncrement(90);

		/** Metal-5 arc */
		PrimitiveArc Metal_5_arc = PrimitiveArc.newInstance(this, "Metal-5", 3, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(M3_lay, 0, Poly.Type.FILLED)
		});
		Metal_5_arc.setFunction(PrimitiveArc.Function.METAL5);
		Metal_5_arc.setFixedAngle();
		Metal_5_arc.setWipable();
		Metal_5_arc.setAngleIncrement(90);

		/** Metal-6 arc */
		PrimitiveArc Metal_6_arc = PrimitiveArc.newInstance(this, "Metal-6", 3, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(M4_lay, 0, Poly.Type.FILLED)
		});
		Metal_6_arc.setFunction(PrimitiveArc.Function.METAL6);
		Metal_6_arc.setFixedAngle();
		Metal_6_arc.setWipable();
		Metal_6_arc.setAngleIncrement(90);

		/** Polysilicon-1 arc */
		PrimitiveArc Polysilicon_1_arc = PrimitiveArc.newInstance(this, "Polysilicon-1", 2, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(P_lay, 0, Poly.Type.FILLED)
		});
		Polysilicon_1_arc.setFunction(PrimitiveArc.Function.POLY1);
		Polysilicon_1_arc.setFixedAngle();
		Polysilicon_1_arc.setWipable();
		Polysilicon_1_arc.setAngleIncrement(90);

		/** Polysilicon-2 arc */
		PrimitiveArc Polysilicon_2_arc = PrimitiveArc.newInstance(this, "Polysilicon-2", 3, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(P0_lay, 0, Poly.Type.FILLED)
		});
		Polysilicon_2_arc.setFunction(PrimitiveArc.Function.POLY2);
		Polysilicon_2_arc.setFixedAngle();
		Polysilicon_2_arc.setWipable();
		Polysilicon_2_arc.setAngleIncrement(90);

		/** P-Active arc */
		PrimitiveArc P_Active_arc = PrimitiveArc.newInstance(this, "P-Active", 15, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(PA_lay, 12, Poly.Type.FILLED),
			new Technology.ArcLayer(PS_lay, 8, Poly.Type.FILLED),
			new Technology.ArcLayer(NW_lay, 0, Poly.Type.FILLED)
		});
		P_Active_arc.setFunction(PrimitiveArc.Function.DIFFP);
		P_Active_arc.setFixedAngle();
		P_Active_arc.setWipable();
		P_Active_arc.setAngleIncrement(90);
		P_Active_arc.setWidthOffset(0);

		/** N-Active arc */
		PrimitiveArc N_Active_arc = PrimitiveArc.newInstance(this, "N-Active", 15, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(NA_lay, 12, Poly.Type.FILLED),
			new Technology.ArcLayer(NS_lay, 8, Poly.Type.FILLED),
			new Technology.ArcLayer(PW_lay, 0, Poly.Type.FILLED)
		});
		N_Active_arc.setFunction(PrimitiveArc.Function.DIFFN);
		N_Active_arc.setFixedAngle();
		N_Active_arc.setWipable();
		N_Active_arc.setAngleIncrement(90);
		N_Active_arc.setWidthOffset(0);

		/** Active arc */
		PrimitiveArc Active_arc = PrimitiveArc.newInstance(this, "Active", 3, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(NA_lay, 0, Poly.Type.FILLED),
			new Technology.ArcLayer(PA_lay, 0, Poly.Type.FILLED)
		});
		Active_arc.setFunction(PrimitiveArc.Function.DIFF);
		Active_arc.setFixedAngle();
		Active_arc.setWipable();
		Active_arc.setAngleIncrement(90);

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
				PrimitivePort.newInstance(this, mp_node, new ArcProto [] {Metal_1_arc}, "metal-1", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
			});
		mp_node.setFunction(NodeProto.Function.PIN);
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
				PrimitivePort.newInstance(this, mp0_node, new ArcProto [] {Metal_2_arc}, "metal-2", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
			});
		mp0_node.setFunction(NodeProto.Function.PIN);
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
				PrimitivePort.newInstance(this, mp1_node, new ArcProto [] {Metal_3_arc}, "metal-3", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
			});
		mp1_node.setFunction(NodeProto.Function.PIN);
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
				PrimitivePort.newInstance(this, mp2_node, new ArcProto [] {Metal_4_arc}, "metal-4", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
			});
		mp2_node.setFunction(NodeProto.Function.PIN);
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
				PrimitivePort.newInstance(this, mp3_node, new ArcProto [] {Metal_5_arc}, "metal-5", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		mp3_node.setFunction(NodeProto.Function.PIN);
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
				PrimitivePort.newInstance(this, mp4_node, new ArcProto [] {Metal_6_arc}, "metal-6", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
			});
		mp4_node.setFunction(NodeProto.Function.PIN);
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
				PrimitivePort.newInstance(this, pp_node, new ArcProto [] {Polysilicon_1_arc}, "polysilicon-1", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(1), EdgeV.fromBottom(1), EdgeH.fromRight(1), EdgeV.fromTop(1))
			});
		pp_node.setFunction(NodeProto.Function.PIN);
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
				PrimitivePort.newInstance(this, pp0_node, new ArcProto [] {Polysilicon_2_arc}, "polysilicon-2", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
			});
		pp0_node.setFunction(NodeProto.Function.PIN);
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
				PrimitivePort.newInstance(this, pap_node, new ArcProto [] {P_Active_arc}, "p-active", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(7.5), EdgeV.fromBottom(7.5), EdgeH.fromRight(7.5), EdgeV.fromTop(7.5))
			});
		pap_node.setFunction(NodeProto.Function.PIN);
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
				PrimitivePort.newInstance(this, nap_node, new ArcProto [] {N_Active_arc}, "n-active", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(7.5), EdgeV.fromBottom(7.5), EdgeH.fromRight(7.5), EdgeV.fromTop(7.5))
			});
		nap_node.setFunction(NodeProto.Function.PIN);
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
				PrimitivePort.newInstance(this, ap_node, new ArcProto [] {Active_arc, P_Active_arc, N_Active_arc}, "active", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
			});
		ap_node.setFunction(NodeProto.Function.PIN);
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
				PrimitivePort.newInstance(this, mpac_node, new ArcProto [] {P_Active_arc, Metal_1_arc}, "metal-1-p-act", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(8), EdgeV.fromBottom(8), EdgeH.fromRight(8), EdgeV.fromTop(8))
			});
		mpac_node.setFunction(NodeProto.Function.CONTACT);
		mpac_node.setSpecialType(PrimitiveNode.MULTICUT);
		mpac_node.setSpecialValues(new double [] {2, 2, 1.5, 4});

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
				PrimitivePort.newInstance(this, mnac_node, new ArcProto [] {N_Active_arc, Metal_1_arc}, "metal-1-n-act", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(8), EdgeV.fromBottom(8), EdgeH.fromRight(8), EdgeV.fromTop(8))
			});
		mnac_node.setFunction(NodeProto.Function.CONTACT);
		mnac_node.setSpecialType(PrimitiveNode.MULTICUT);
		mnac_node.setSpecialValues(new double [] {2, 2, 1.5, 4});

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
				PrimitivePort.newInstance(this, mpc_node, new ArcProto [] {Polysilicon_1_arc, Metal_1_arc}, "metal-1-polysilicon-1", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(2), EdgeV.fromBottom(2), EdgeH.fromRight(2), EdgeV.fromTop(2))
			});
		mpc_node.setFunction(NodeProto.Function.CONTACT);
		mpc_node.setSpecialType(PrimitiveNode.MULTICUT);
		mpc_node.setSpecialValues(new double [] {2, 2, 1.5, 4});

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
				PrimitivePort.newInstance(this, mpc0_node, new ArcProto [] {Polysilicon_2_arc, Metal_1_arc}, "metal-1-polysilicon-2", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
			});
		mpc0_node.setFunction(NodeProto.Function.CONTACT);
		mpc0_node.setSpecialType(PrimitiveNode.MULTICUT);
		mpc0_node.setSpecialValues(new double [] {2, 2, 1, 4});

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
				PrimitivePort.newInstance(this, mpc1_node, new ArcProto [] {Polysilicon_1_arc, Polysilicon_2_arc, Metal_1_arc}, "metal-1-polysilicon-1-2", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		mpc1_node.setFunction(NodeProto.Function.CONTACT);
		mpc1_node.setSpecialType(PrimitiveNode.MULTICUT);
		mpc1_node.setSpecialValues(new double [] {2, 2, 2.5, 4});

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
				PrimitivePort.newInstance(this, pt_node, new ArcProto [] {Polysilicon_1_arc}, "p-trans-poly-left", 180,90, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(4), EdgeV.fromBottom(10), EdgeH.fromLeft(4), EdgeV.fromTop(10)),
				PrimitivePort.newInstance(this, pt_node, new ArcProto [] {P_Active_arc}, "p-trans-diff-top", 90,90, 1, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(7.5), EdgeV.fromTop(6.5), EdgeH.fromRight(7.5), EdgeV.fromTop(6)),
				PrimitivePort.newInstance(this, pt_node, new ArcProto [] {Polysilicon_1_arc}, "p-trans-poly-right", 0,90, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromRight(4), EdgeV.fromBottom(10), EdgeH.fromRight(4), EdgeV.fromTop(10)),
				PrimitivePort.newInstance(this, pt_node, new ArcProto [] {P_Active_arc}, "p-trans-diff-bottom", 270,90, 3, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(7.5), EdgeV.fromBottom(6), EdgeH.fromRight(7.5), EdgeV.fromBottom(6.5))
			});
		pt_node.setFunction(NodeProto.Function.TRAPMOS);
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
				PrimitivePort.newInstance(this, nt_node, new ArcProto [] {Polysilicon_1_arc}, "n-trans-poly-left", 180,90, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(4), EdgeV.fromBottom(10), EdgeH.fromLeft(4), EdgeV.fromTop(10)),
				PrimitivePort.newInstance(this, nt_node, new ArcProto [] {N_Active_arc}, "n-trans-diff-top", 90,90, 1, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(7.5), EdgeV.fromTop(6.5), EdgeH.fromRight(7.5), EdgeV.fromTop(6)),
				PrimitivePort.newInstance(this, nt_node, new ArcProto [] {Polysilicon_1_arc}, "n-trans-poly-right", 0,90, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromRight(4), EdgeV.fromBottom(10), EdgeH.fromRight(4), EdgeV.fromTop(10)),
				PrimitivePort.newInstance(this, nt_node, new ArcProto [] {N_Active_arc}, "n-trans-diff-bottom", 270,90, 3, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(7.5), EdgeV.fromBottom(6), EdgeH.fromRight(7.5), EdgeV.fromBottom(6.5))
			});
		nt_node.setFunction(NodeProto.Function.TRANMOS);
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
				PrimitivePort.newInstance(this, mmc_node, new ArcProto [] {Metal_1_arc, Metal_2_arc}, "metal-1-metal-2", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
			});
		mmc_node.setFunction(NodeProto.Function.CONTACT);
		mmc_node.setSpecialType(PrimitiveNode.MULTICUT);
		mmc_node.setSpecialValues(new double [] {2, 2, 1, 4});

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
				PrimitivePort.newInstance(this, mmc0_node, new ArcProto [] {Metal_2_arc, Metal_3_arc}, "metal-2-metal-3", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
			});
		mmc0_node.setFunction(NodeProto.Function.CONTACT);
		mmc0_node.setSpecialType(PrimitiveNode.MULTICUT);
		mmc0_node.setSpecialValues(new double [] {2, 2, 1, 4});

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
				PrimitivePort.newInstance(this, mmc1_node, new ArcProto [] {Metal_3_arc, Metal_4_arc}, "metal-3-metal-4", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
			});
		mmc1_node.setFunction(NodeProto.Function.CONTACT);
		mmc1_node.setSpecialType(PrimitiveNode.MULTICUT);
		mmc1_node.setSpecialValues(new double [] {2, 2, 1, 4});

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
				PrimitivePort.newInstance(this, mmc2_node, new ArcProto [] {Metal_4_arc, Metal_5_arc}, "metal-4-metal-5", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
			});
		mmc2_node.setFunction(NodeProto.Function.CONTACT);
		mmc2_node.setSpecialType(PrimitiveNode.MULTICUT);
		mmc2_node.setSpecialValues(new double [] {2, 2, 1, 4});

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
				PrimitivePort.newInstance(this, mmc3_node, new ArcProto [] {Metal_5_arc, Metal_6_arc}, "metal-5-metal-6", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(2.5), EdgeV.fromBottom(2.5), EdgeH.fromRight(2.5), EdgeV.fromTop(2.5))
			});
		mmc3_node.setFunction(NodeProto.Function.CONTACT);
		mmc3_node.setSpecialType(PrimitiveNode.MULTICUT);
		mmc3_node.setSpecialValues(new double [] {2, 2, 2, 4});

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
				PrimitivePort.newInstance(this, mwc_node, new ArcProto [] {Metal_1_arc, Active_arc}, "metal-1-well", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(6.5), EdgeV.fromBottom(6.5), EdgeH.fromRight(6.5), EdgeV.fromTop(6.5))
			});
		mwc_node.setFunction(NodeProto.Function.WELL);
		mwc_node.setSpecialType(PrimitiveNode.MULTICUT);
		mwc_node.setSpecialValues(new double [] {2, 2, 2, 4});

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
				PrimitivePort.newInstance(this, msc_node, new ArcProto [] {Metal_1_arc, Active_arc}, "metal-1-substrate", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(6.5), EdgeV.fromBottom(6.5), EdgeH.fromRight(6.5), EdgeV.fromTop(6.5))
			});
		msc_node.setFunction(NodeProto.Function.SUBSTRATE);
		msc_node.setSpecialType(PrimitiveNode.MULTICUT);
		msc_node.setSpecialValues(new double [] {2, 2, 2, 4});

		/** Metal-1-Node */
		PrimitiveNode mn_node = PrimitiveNode.newInstance("Metal-1-Node", this, 3, 3, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(M_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_12)
			});
		mn_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, mn_node, new ArcProto [] {Metal_1_arc}, "metal-1", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
			});
		mn_node.setFunction(NodeProto.Function.NODE);
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
				PrimitivePort.newInstance(this, mn0_node, new ArcProto [] {Metal_2_arc}, "metal-2", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
			});
		mn0_node.setFunction(NodeProto.Function.NODE);
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
				PrimitivePort.newInstance(this, mn1_node, new ArcProto [] {Metal_3_arc}, "metal-3", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
			});
		mn1_node.setFunction(NodeProto.Function.NODE);
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
				PrimitivePort.newInstance(this, mn2_node, new ArcProto [] {Metal_4_arc}, "metal-4", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
			});
		mn2_node.setFunction(NodeProto.Function.NODE);
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
				PrimitivePort.newInstance(this, mn3_node, new ArcProto [] {Metal_5_arc}, "metal-5", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
			});
		mn3_node.setFunction(NodeProto.Function.NODE);
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
				PrimitivePort.newInstance(this, mn4_node, new ArcProto [] {Metal_6_arc}, "metal-6", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
			});
		mn4_node.setFunction(NodeProto.Function.NODE);
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
				PrimitivePort.newInstance(this, pn_node, new ArcProto [] {Polysilicon_1_arc}, "polysilicon-1", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(1), EdgeV.fromBottom(1), EdgeH.fromRight(1), EdgeV.fromTop(1))
			});
		pn_node.setFunction(NodeProto.Function.NODE);
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
				PrimitivePort.newInstance(this, pn0_node, new ArcProto [] {Polysilicon_2_arc}, "polysilicon-2", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
			});
		pn0_node.setFunction(NodeProto.Function.NODE);
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
				PrimitivePort.newInstance(this, an_node, new ArcProto [] {Active_arc, P_Active_arc, N_Active_arc}, "active", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
			});
		an_node.setFunction(NodeProto.Function.NODE);
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
				PrimitivePort.newInstance(this, nan_node, new ArcProto [] {Active_arc, P_Active_arc, N_Active_arc}, "active", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
			});
		nan_node.setFunction(NodeProto.Function.NODE);
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
				PrimitivePort.newInstance(this, psn_node, new ArcProto [] {}, "select", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		psn_node.setFunction(NodeProto.Function.NODE);
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
				PrimitivePort.newInstance(this, nsn_node, new ArcProto [] {}, "select", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		nsn_node.setFunction(NodeProto.Function.NODE);
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
				PrimitivePort.newInstance(this, pcn_node, new ArcProto [] {}, "polycut", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		pcn_node.setFunction(NodeProto.Function.NODE);
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
				PrimitivePort.newInstance(this, acn_node, new ArcProto [] {}, "activecut", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		acn_node.setFunction(NodeProto.Function.NODE);
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
				PrimitivePort.newInstance(this, vn_node, new ArcProto [] {}, "via-1", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		vn_node.setFunction(NodeProto.Function.NODE);
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
				PrimitivePort.newInstance(this, vn0_node, new ArcProto [] {}, "via-2", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		vn0_node.setFunction(NodeProto.Function.NODE);
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
				PrimitivePort.newInstance(this, vn1_node, new ArcProto [] {}, "via-3", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		vn1_node.setFunction(NodeProto.Function.NODE);
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
				PrimitivePort.newInstance(this, vn2_node, new ArcProto [] {}, "via-4", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		vn2_node.setFunction(NodeProto.Function.NODE);
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
				PrimitivePort.newInstance(this, vn3_node, new ArcProto [] {}, "via-5", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		vn3_node.setFunction(NodeProto.Function.NODE);
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
				PrimitivePort.newInstance(this, pwn_node, new ArcProto [] {P_Active_arc}, "well", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(3), EdgeV.fromBottom(3), EdgeH.fromRight(3), EdgeV.fromTop(3))
			});
		pwn_node.setFunction(NodeProto.Function.NODE);
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
				PrimitivePort.newInstance(this, nwn_node, new ArcProto [] {P_Active_arc}, "well", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(3), EdgeV.fromBottom(3), EdgeH.fromRight(3), EdgeV.fromTop(3))
			});
		nwn_node.setFunction(NodeProto.Function.NODE);
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
				PrimitivePort.newInstance(this, pn1_node, new ArcProto [] {}, "passivation", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		pn1_node.setFunction(NodeProto.Function.NODE);
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
				PrimitivePort.newInstance(this, pfn_node, new ArcProto [] {}, "pad-frame", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		pfn_node.setFunction(NodeProto.Function.NODE);
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
				PrimitivePort.newInstance(this, pcn0_node, new ArcProto [] {}, "poly-cap", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		pcn0_node.setFunction(NodeProto.Function.NODE);
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
				PrimitivePort.newInstance(this, pawn_node, new ArcProto [] {}, "p-active-well", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		pawn_node.setFunction(NodeProto.Function.NODE);
		pawn_node.setHoldsOutline();
		pawn_node.setSpecialType(PrimitiveNode.POLYGONAL);
	};
}
