/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: MoCMOS.java
 *
 * Copyright (c) 2003 Sun Microsystems and Static Free Software
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
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;

/**
 * This is the MOSIS CMOS technology.
 */
public class MoCMOS extends Technology
{
	/** the MOSIS CMOS Technology object. */	public static final MoCMOS tech = new MoCMOS();
	/** metal-1-pin */						private PrimitiveNode metal1Pin_node;
	/** metal-2-pin */						private PrimitiveNode metal2Pin_node;
	/** metal-3-pin */						private PrimitiveNode metal3Pin_node;
	/** metal-4-pin */						private PrimitiveNode metal4Pin_node;
	/** metal-5-pin */						private PrimitiveNode metal5Pin_node;
	/** metal-6-pin */						private PrimitiveNode metal6Pin_node;
	/** polysilicon-1-pin */				private PrimitiveNode poly1Pin_node;
	/** polysilicon-2-pin */				private PrimitiveNode poly2Pin_node;
	/** P-active-pin */						private PrimitiveNode pActivePin_node;
	/** N-active-pin */						private PrimitiveNode nActivePin_node;
	/** General active-pin */				private PrimitiveNode activePin_node;
	/** metal-1-P-active-contact */			private PrimitiveNode metal1PActiveContact_node;
	/** metal-1-N-active-contact */			private PrimitiveNode metal1NActiveContact_node;
	/** metal-1-polysilicon-1-contact */	private PrimitiveNode metal1Poly1Contact_node;
	/** metal-1-polysilicon-2-contact */	private PrimitiveNode metal1Poly2Contact_node;
	/** metal-1-polysilicon-1-2-contact */	private PrimitiveNode metal1Poly12Contact_node;
	/** P-Transistor */						private PrimitiveNode pTransistor_node;
	/** N-Transistor */						private PrimitiveNode nTransistor_node;
	/** Scalable-P-Transistor */			private PrimitiveNode scalablePTransistor_node;
	/** Scalable-N-Transistor */			private PrimitiveNode scalableNTransistor_node;
	/** metal-1-metal-2-contact */			private PrimitiveNode metal1Metal2Contact_node;
	/** metal-2-metal-3-contact */			private PrimitiveNode metal2Metal3Contact_node;
	/** metal-3-metal-4-contact */			private PrimitiveNode metal3Metal4Contact_node;
	/** metal-4-metal-5-contact */			private PrimitiveNode metal4Metal5Contact_node;
	/** metal-5-metal-6-contact */			private PrimitiveNode metal5Metal6Contact_node;
	/** Metal-1-P-Well Contact */			private PrimitiveNode metal1PWellContact_node;
	/** Metal-1-N-Well Contact */			private PrimitiveNode metal1NWellContact_node;
	/** Metal-1-Node */						private PrimitiveNode metal1Node_node;
	/** Metal-2-Node */						private PrimitiveNode metal2Node_node;
	/** Metal-3-Node */						private PrimitiveNode metal3Node_node;
	/** Metal-4-Node */						private PrimitiveNode metal4Node_node;
	/** Metal-5-Node */						private PrimitiveNode metal5Node_node;
	/** Metal-6-Node */						private PrimitiveNode metal6Node_node;
	/** Polysilicon-1-Node */				private PrimitiveNode poly1Node_node;
	/** Polysilicon-2-Node */				private PrimitiveNode poly2Node_node;
	/** P-Active-Node */					private PrimitiveNode pActiveNode_node;
	/** N-Active-Node */					private PrimitiveNode nActiveNode_node;
	/** P-Select-Node */					private PrimitiveNode pSelectNode_node;
	/** N-Select-Node */					private PrimitiveNode nSelectNode_node;
	/** PolyCut-Node */						private PrimitiveNode polyCutNode_node;
	/** ActiveCut-Node */					private PrimitiveNode activeCutNode_node;
	/** Via-1-Node */						private PrimitiveNode via1Node_node;
	/** Via-2-Node */						private PrimitiveNode via2Node_node;
	/** Via-3-Node */						private PrimitiveNode via3Node_node;
	/** Via-4-Node */						private PrimitiveNode via4Node_node;
	/** Via-5-Node */						private PrimitiveNode via5Node_node;
	/** P-Well-Node */						private PrimitiveNode pWellNode_node;
	/** N-Well-Node */						private PrimitiveNode nWellNode_node;
	/** Passivation-Node */					private PrimitiveNode passivationNode_node;
	/** Pad-Frame-Node */					private PrimitiveNode padFrameNode_node;
	/** Poly-Cap-Node */					private PrimitiveNode polyCapNode_node;
	/** P-Active-Well-Node */				private PrimitiveNode pActiveWellNode_node;
	/** Polysilicon-1-Transistor-Node */	private PrimitiveNode polyTransistorNode_node;
	/** Silicide-Block-Node */				private PrimitiveNode silicideBlockNode_node;

	// -------------------- private and protected methods ------------------------
	private MoCMOS()
	{
		setTechName("mocmos");
		setTechDesc("MOSIS CMOS");
		setScale(400);
		setNoNegatedArcs();
		setStaticTechnology();

		//**************************************** LAYERS ****************************************

		/** metal-1 layer */
		Layer metal1_lay = Layer.newInstance(this, "Metal-1",
			new EGraphics(EGraphics.SOLIDC, EGraphics.PATTERNED, 96,209,255,0.8,1,
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

		/** metal-2 layer */
		Layer metal2_lay = Layer.newInstance(this, "Metal-2",
			new EGraphics(EGraphics.SOLIDC, EGraphics.PATTERNED, 224,95,255,0.7,1,
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

		/** metal-3 layer */
		Layer metal3_lay = Layer.newInstance(this, "Metal-3",
			new EGraphics(EGraphics.SOLIDC, EGraphics.PATTERNED, 247,251,20,0.6,1,
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

		/** metal-4 layer */
		Layer metal4_lay = Layer.newInstance(this, "Metal-4",
			new EGraphics(EGraphics.PATTERNED, EGraphics.PATTERNED, 150,150,255,0.5,1,
			new int[] { 0xFFFF,   // XXXXXXXXXXXXXXXX
						0x0000,   //                 
						0xFFFF,   // XXXXXXXXXXXXXXXX
						0x0000,   //                 
						0xFFFF,   // XXXXXXXXXXXXXXXX
						0x0000,   //                 
						0xFFFF,   // XXXXXXXXXXXXXXXX
						0x0000,   //                 
						0xFFFF,   // XXXXXXXXXXXXXXXX
						0x0000,   //                 
						0xFFFF,   // XXXXXXXXXXXXXXXX
						0x0000,   //                 
						0xFFFF,   // XXXXXXXXXXXXXXXX
						0x0000,   //                 
						0xFFFF,   // XXXXXXXXXXXXXXXX
						0x0000}));//                 

		/** metal-5 layer */
		Layer metal5_lay = Layer.newInstance(this, "Metal-5",
			new EGraphics(EGraphics.PATTERNED|EGraphics.OUTLINEPAT, EGraphics.PATTERNED|EGraphics.OUTLINEPAT, 255,190,6,0.4,1,
			new int[] { 0x8888,   // X   X   X   X   
						0x1111,   //    X   X   X   X
						0x2222,   //   X   X   X   X 
						0x4444,   //  X   X   X   X  
						0x8888,   // X   X   X   X   
						0x1111,   //    X   X   X   X
						0x2222,   //   X   X   X   X 
						0x4444,   //  X   X   X   X  
						0x8888,   // X   X   X   X   
						0x1111,   //    X   X   X   X
						0x2222,   //   X   X   X   X 
						0x4444,   //  X   X   X   X  
						0x8888,   // X   X   X   X   
						0x1111,   //    X   X   X   X
						0x2222,   //   X   X   X   X 
						0x4444}));//  X   X   X   X  

		/** metal-6 layer */
		Layer metal6_lay = Layer.newInstance(this, "Metal-6",
			new EGraphics(EGraphics.PATTERNED, EGraphics.PATTERNED, 0,255,255,0.3,1,
			new int[] { 0x8888,   // X   X   X   X   
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
						0x2222,   //   X   X   X   X 
						0x1111}));//    X   X   X   X

		/** poly layer */
		Layer poly1_lay = Layer.newInstance(this, "Polysilicon-1",
			new EGraphics(EGraphics.SOLIDC, EGraphics.PATTERNED, 255,155,192,0.5,1,
			new int[] { 0x1111,   //    X   X   X   X
						0xFFFF,   // XXXXXXXXXXXXXXXX
						0x1111,   //    X   X   X   X
						0x5555,   //  X X X X X X X X
						0x1111,   //    X   X   X   X
						0xFFFF,   // XXXXXXXXXXXXXXXX
						0x1111,   //    X   X   X   X
						0x5555,   //  X X X X X X X X
						0x1111,   //    X   X   X   X
						0xFFFF,   // XXXXXXXXXXXXXXXX
						0x1111,   //    X   X   X   X
						0x5555,   //  X X X X X X X X
						0x1111,   //    X   X   X   X
						0xFFFF,   // XXXXXXXXXXXXXXXX
						0x1111,   //    X   X   X   X
						0x5555}));//  X X X X X X X X

		/** poly2 layer */
		Layer poly2_lay = Layer.newInstance(this, "Polysilicon-2",
			new EGraphics(EGraphics.PATTERNED, EGraphics.PATTERNED, 255,190,6,1.0,1,
			new int[] { 0xAFAF,   // X X XXXXX X XXXX
						0x8888,   // X   X   X   X   
						0xFAFA,   // XXXXX X XXXXX X 
						0x8888,   // X   X   X   X   
						0xAFAF,   // X X XXXXX X XXXX
						0x8888,   // X   X   X   X   
						0xFAFA,   // XXXXX X XXXXX X 
						0x8888,   // X   X   X   X   
						0xAFAF,   // X X XXXXX X XXXX
						0x8888,   // X   X   X   X   
						0xFAFA,   // XXXXX X XXXXX X 
						0x8888,   // X   X   X   X   
						0xAFAF,   // X X XXXXX X XXXX
						0x8888,   // X   X   X   X   
						0xFAFA,   // XXXXX X XXXXX X 
						0x8888}));// X   X   X   X   

		/** P active layer */
		Layer pActive_lay = Layer.newInstance(this, "P-Active",
			new EGraphics(EGraphics.SOLIDC, EGraphics.PATTERNED, 107,226,96,0.5,1,
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

		/** N active layer */
		Layer nActive_lay = Layer.newInstance(this, "N-Active",
			new EGraphics(EGraphics.SOLIDC, EGraphics.PATTERNED, 107,226,96,0.5,1,
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

		/** P Select layer */
		Layer pSelect_lay = Layer.newInstance(this, "P-Select",
			new EGraphics(EGraphics.PATTERNED, EGraphics.PATTERNED, 139,99,46,0.2,0,
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

		/** N Select layer */
		Layer nSelect_lay = Layer.newInstance(this, "N-Select",
			new EGraphics(EGraphics.PATTERNED, EGraphics.PATTERNED, 255,255,0,0.2,0,
			new int[] { 0x0101,   //        X       X
						0x0000,   //                 
						0x1010,   //    X       X    
						0x0000,   //                 
						0x0101,   //        X       X
						0x0000,   //                 
						0x1010,   //    X       X    
						0x0000,   //                 
						0x0101,   //        X       X
						0x0000,   //                 
						0x1010,   //    X       X    
						0x0000,   //                 
						0x0101,   //        X       X
						0x0000,   //                 
						0x1010,   //    X       X    
						0x0000}));//                 

		/** P Well layer */
		Layer pWell_lay = Layer.newInstance(this, "P-Well",
			new EGraphics(EGraphics.PATTERNED, EGraphics.PATTERNED, 139,99,46,0.2,0,
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

		/** N Well implant */
		Layer nWell_lay = Layer.newInstance(this, "N-Well",
			new EGraphics(EGraphics.PATTERNED, EGraphics.PATTERNED, 255,255,0,0.2,0,
			new int[] { 0x0202,   //       X       X 
						0x0000,   //                 
						0x2020,   //   X       X     
						0x0000,   //                 
						0x0202,   //       X       X 
						0x0000,   //                 
						0x2020,   //   X       X     
						0x0000,   //                 
						0x0202,   //       X       X 
						0x0000,   //                 
						0x2020,   //   X       X     
						0x0000,   //                 
						0x0202,   //       X       X 
						0x0000,   //                 
						0x2020,   //   X       X     
						0x0000}));//                 

		/** poly cut layer */
		Layer polyCut_lay = Layer.newInstance(this, "Poly-Cut",
			new EGraphics(EGraphics.SOLIDC, EGraphics.SOLIDC, 100,100,100,1.0,1,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** active cut layer */
		Layer activeCut_lay = Layer.newInstance(this, "Active-Cut",
			new EGraphics(EGraphics.SOLIDC, EGraphics.SOLIDC, 100,100,100,1.0,1,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** via1 layer */
		Layer via1_lay = Layer.newInstance(this, "Via1", 
			new EGraphics(EGraphics.SOLIDC, EGraphics.SOLIDC, 180,180,180,1.0,1,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** via2 layer */
		Layer via2_lay = Layer.newInstance(this, "Via2",
			new EGraphics(EGraphics.SOLIDC, EGraphics.SOLIDC, 180,180,180,1.0,1,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** via3 layer */
		Layer via3_lay = Layer.newInstance(this, "Via3",
			new EGraphics(EGraphics.SOLIDC, EGraphics.SOLIDC, 180,180,180,1.0,1,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** via4 layer */
		Layer via4_lay = Layer.newInstance(this, "Via4",
			new EGraphics(EGraphics.SOLIDC, EGraphics.SOLIDC, 180,180,180,1.0,1,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** via5 layer */
		Layer via5_lay = Layer.newInstance(this, "Via5",
			new EGraphics(EGraphics.SOLIDC, EGraphics.SOLIDC, 180,180,180,1.0,1,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** passivation layer */
		Layer passivation_lay = Layer.newInstance(this, "Passivation",
			new EGraphics(EGraphics.PATTERNED, EGraphics.PATTERNED, 100,100,100,1.0,1,
			new int[] { 0x1C1C,   //    XXX     XXX  
						0x3E3E,   //   XXXXX   XXXXX 
						0x3636,   //   XX XX   XX XX 
						0x3E3E,   //   XXXXX   XXXXX 
						0x1C1C,   //    XXX     XXX  
						0x0000,   //                 
						0x0000,   //                 
						0x0000,   //                 
						0x1C1C,   //    XXX     XXX  
						0x3E3E,   //   XXXXX   XXXXX 
						0x3636,   //   XX XX   XX XX 
						0x3E3E,   //   XXXXX   XXXXX 
						0x1C1C,   //    XXX     XXX  
						0x0000,   //                 
						0x0000,   //                 
						0x0000}));//                 

		/** poly/trans layer */
		Layer transistorPoly_lay = Layer.newInstance(this, "Transistor-Poly",
			new EGraphics(EGraphics.SOLIDC, EGraphics.PATTERNED, 255,155,192,0.5,1,
			new int[] { 0x1111,   //    X   X   X   X
						0xFFFF,   // XXXXXXXXXXXXXXXX
						0x1111,   //    X   X   X   X
						0x5555,   //  X X X X X X X X
						0x1111,   //    X   X   X   X
						0xFFFF,   // XXXXXXXXXXXXXXXX
						0x1111,   //    X   X   X   X
						0x5555,   //  X X X X X X X X
						0x1111,   //    X   X   X   X
						0xFFFF,   // XXXXXXXXXXXXXXXX
						0x1111,   //    X   X   X   X
						0x5555,   //  X X X X X X X X
						0x1111,   //    X   X   X   X
						0xFFFF,   // XXXXXXXXXXXXXXXX
						0x1111,   //    X   X   X   X
						0x5555}));//  X X X X X X X X

		/** poly cap layer */
		Layer polyCap_lay = Layer.newInstance(this, "Poly-Cap",
			new EGraphics(EGraphics.SOLIDC, EGraphics.SOLIDC, 0,0,0,1.0,1,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** P act well layer */
		Layer pActiveWell_lay = Layer.newInstance(this, "P-Active-Well",
			new EGraphics(EGraphics.SOLIDC, EGraphics.PATTERNED, 107,226,96,1.0,0,
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

		/** Silicide block */
		Layer silicideBlock_lay = Layer.newInstance(this, "Silicide-Block",
			new EGraphics(EGraphics.PATTERNED, EGraphics.PATTERNED, 230,230,230,1.0,0,
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

		/** pseudo metal 1 */
		Layer pseudoMetal1_lay = Layer.newInstance(this, "Pseudo-Metal-1",
			new EGraphics(EGraphics.SOLIDC, EGraphics.PATTERNED, 96,209,255,0.8,1,
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

		/** pseudo metal-2 */
		Layer pseudoMetal2_lay = Layer.newInstance(this, "Pseudo-Metal-2",
			new EGraphics(EGraphics.SOLIDC, EGraphics.PATTERNED, 224,95,255,0.7,1,
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

		/** pseudo metal-3 */
		Layer pseudoMetal3_lay = Layer.newInstance(this, "Pseudo-Metal-3",
			new EGraphics(EGraphics.SOLIDC, EGraphics.PATTERNED, 247,251,20,0.6,1,
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

		/** pseudo metal-4 */
		Layer pseudoMetal4_lay = Layer.newInstance(this, "Pseudo-Metal-4",
			new EGraphics(EGraphics.PATTERNED, EGraphics.PATTERNED, 150,150,255,0.5,1,
			new int[] { 0xFFFF,   // XXXXXXXXXXXXXXXX
						0x0000,   //                 
						0xFFFF,   // XXXXXXXXXXXXXXXX
						0x0000,   //                 
						0xFFFF,   // XXXXXXXXXXXXXXXX
						0x0000,   //                 
						0xFFFF,   // XXXXXXXXXXXXXXXX
						0x0000,   //                 
						0xFFFF,   // XXXXXXXXXXXXXXXX
						0x0000,   //                 
						0xFFFF,   // XXXXXXXXXXXXXXXX
						0x0000,   //                 
						0xFFFF,   // XXXXXXXXXXXXXXXX
						0x0000,   //                 
						0xFFFF,   // XXXXXXXXXXXXXXXX
						0x0000}));//                 

		/** pseudo metal-5 */
		Layer pseudoMetal5_lay = Layer.newInstance(this, "Pseudo-Metal-5",
			new EGraphics(EGraphics.PATTERNED|EGraphics.OUTLINEPAT, EGraphics.PATTERNED|EGraphics.OUTLINEPAT, 255,190,6,0.4,1,
			new int[] { 0x8888,   // X   X   X   X   
						0x1111,   //    X   X   X   X
						0x2222,   //   X   X   X   X 
						0x4444,   //  X   X   X   X  
						0x8888,   // X   X   X   X   
						0x1111,   //    X   X   X   X
						0x2222,   //   X   X   X   X 
						0x4444,   //  X   X   X   X  
						0x8888,   // X   X   X   X   
						0x1111,   //    X   X   X   X
						0x2222,   //   X   X   X   X 
						0x4444,   //  X   X   X   X  
						0x8888,   // X   X   X   X   
						0x1111,   //    X   X   X   X
						0x2222,   //   X   X   X   X 
						0x4444}));//  X   X   X   X  

		/** pseudo metal-6 */
		Layer pseudoMetal6_lay = Layer.newInstance(this, "Pseudo-Metal-6",
			new EGraphics(EGraphics.PATTERNED, EGraphics.PATTERNED, 0,255,255,0.3,1,
			new int[] { 0x8888,   // X   X   X   X   
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
						0x2222,   //   X   X   X   X 
						0x1111}));//    X   X   X   X

		/** pseudo poly layer */
		Layer pseudoPoly1_lay = Layer.newInstance(this, "Pseudo-Polysilicon",
			new EGraphics(EGraphics.SOLIDC, EGraphics.PATTERNED, 255,155,192,1.0,1,
			new int[] { 0x1111,   //    X   X   X   X
						0xFFFF,   // XXXXXXXXXXXXXXXX
						0x1111,   //    X   X   X   X
						0x5555,   //  X X X X X X X X
						0x1111,   //    X   X   X   X
						0xFFFF,   // XXXXXXXXXXXXXXXX
						0x1111,   //    X   X   X   X
						0x5555,   //  X X X X X X X X
						0x1111,   //    X   X   X   X
						0xFFFF,   // XXXXXXXXXXXXXXXX 
						0x1111,   //  X X X X X X X X
						0x1111,   //    X   X   X   X
						0xFFFF,   // XXXXXXXXXXXXXXXX
						0x1111,   //    X   X   X   X
						0x5555}));//  X X X X X X X X

		/** pseudo poly2 layer */
		Layer pseudoPoly2_lay = Layer.newInstance(this, "Pseudo-Electrode",
			new EGraphics(EGraphics.PATTERNED, EGraphics.PATTERNED, 255,190,6,1.0,1,
			new int[] { 0xAFAF,   // X X XXXXX X XXXX
						0x8888,   // X   X   X   X   
						0xFAFA,   // XXXXX X XXXXX X 
						0x8888,   // X   X   X   X   
						0xAFAF,   // X X XXXXX X XXXX
						0x8888,   // X   X   X   X   
						0xFAFA,   // XXXXX X XXXXX X 
						0x8888,   // X   X   X   X   
						0xAFAF,   // X X XXXXX X XXXX
						0x8888,   // X   X   X   X   
						0xFAFA,   // XXXXX X XXXXX X 
						0x8888,   // X   X   X   X   
						0xAFAF,   // X X XXXXX X XXXX
						0x8888,   // X   X   X   X   
						0xFAFA,   // XXXXX X XXXXX X 
						0x8888}));// X   X   X   X   

		/** pseudo P active */
		Layer pseudoPActive_lay = Layer.newInstance(this, "Pseudo-P-Active",
			new EGraphics(EGraphics.SOLIDC, EGraphics.PATTERNED, 107,226,96,1.0,1,
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

		/** pseudo N active */
		Layer pseudoNActive_lay = Layer.newInstance(this, "Pseudo-N-Active",
			new EGraphics(EGraphics.SOLIDC, EGraphics.PATTERNED, 107,226,96,1.0,1,
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

		/** pseudo P Select */
		Layer pseudoPSelect_lay = Layer.newInstance(this, "Pseudo-P-Select",
			new EGraphics(EGraphics.PATTERNED, EGraphics.PATTERNED, 255,255,0,1.0,0,
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

		/** pseudo N Select */
		Layer pseudoNSelect_lay = Layer.newInstance(this, "Pseudo-N-Select",
			new EGraphics(EGraphics.PATTERNED, EGraphics.PATTERNED, 255,255,0,1.0,0,
			new int[] { 0x0101,   //        X       X
						0x0000,   //                 
						0x1010,   //    X       X    
						0x0000,   //                 
						0x0101,   //        X       X
						0x0000,   //                 
						0x1010,   //    X       X    
						0x0000,   //                 
						0x0101,   //        X       X
						0x0000,   //                 
						0x1010,   //    X       X    
						0x0000,   //                 
						0x0101,   //        X       X
						0x0000,   //                 
						0x1010,   //    X       X    
						0x0000}));//                 

		/** pseudo P Well */
		Layer pseudoPWell_lay = Layer.newInstance(this, "Pseudo-P-Well",
			new EGraphics(EGraphics.PATTERNED, EGraphics.PATTERNED, 139,99,46,1.0,0,
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

		/** pseudo N Well */
		Layer pseudoNWell_lay = Layer.newInstance(this, "Pseudo-N-Well",
			new EGraphics(EGraphics.PATTERNED, EGraphics.PATTERNED, 139,99,46,1.0,0,
			new int[] { 0x0202,   //       X       X 
						0x0000,   //                 
						0x2020,   //   X       X     
						0x0000,   //                 
						0x0202,   //       X       X 
						0x0000,   //                 
						0x2020,   //   X       X     
						0x0000,   //                 
						0x0202,   //       X       X 
						0x0000,   //                 
						0x2020,   //   X       X     
						0x0000,   //                 
						0x0202,   //       X       X 
						0x0000,   //                 
						0x2020,   //   X       X     
						0x0000}));//                 

		/** pad frame */
		Layer padFrame_lay = Layer.newInstance(this, "Pad-Frame",
			new EGraphics(EGraphics.SOLIDC, EGraphics.PATTERNED, 255,0,0,1.0,0,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		// The layer functions
		metal1_lay.setFunction(Layer.Function.METAL1);														// Metal-1
		metal2_lay.setFunction(Layer.Function.METAL2);														// Metal-2
		metal3_lay.setFunction(Layer.Function.METAL3);														// Metal-3
		metal4_lay.setFunction(Layer.Function.METAL4);														// Metal-4
		metal5_lay.setFunction(Layer.Function.METAL5);														// Metal-5
		metal6_lay.setFunction(Layer.Function.METAL6);														// Metal-6
		poly1_lay.setFunction(Layer.Function.POLY1);														// Polysilicon-1
		poly2_lay.setFunction(Layer.Function.POLY2);														// Polysilicon-2
		pActive_lay.setFunction(Layer.Function.DIFFP);														// P-Active
		nActive_lay.setFunction(Layer.Function.DIFFN);														// N-Active
		pSelect_lay.setFunction(Layer.Function.IMPLANTP);													// P-Select
		nSelect_lay.setFunction(Layer.Function.IMPLANTN);													// N-Select
		pWell_lay.setFunction(Layer.Function.WELLP);														// P-Well
		nWell_lay.setFunction(Layer.Function.WELLN);														// N-Well
		polyCut_lay.setFunction(Layer.Function.CONTACT1, Layer.Function.CONPOLY);							// Poly-Cut
		activeCut_lay.setFunction(Layer.Function.CONTACT1, Layer.Function.CONDIFF);							// Active-Cut
		via1_lay.setFunction(Layer.Function.CONTACT2, Layer.Function.CONMETAL);								// Via-1
		via2_lay.setFunction(Layer.Function.CONTACT3, Layer.Function.CONMETAL);								// Via-2
		via3_lay.setFunction(Layer.Function.CONTACT4, Layer.Function.CONMETAL);								// Via-3
		via4_lay.setFunction(Layer.Function.CONTACT5, Layer.Function.CONMETAL);								// Via-4
		via5_lay.setFunction(Layer.Function.CONTACT6, Layer.Function.CONMETAL);								// Via-5
		passivation_lay.setFunction(Layer.Function.OVERGLASS);												// Passivation
		transistorPoly_lay.setFunction(Layer.Function.GATE);												// Transistor-Poly
		polyCap_lay.setFunction(Layer.Function.CAP);														// Poly-Cap
		pActiveWell_lay.setFunction(Layer.Function.DIFFP);													// P-Active-Well
		silicideBlock_lay.setFunction(Layer.Function.ART);													// Silicide-Block
		pseudoMetal1_lay.setFunction(Layer.Function.METAL1, Layer.Function.PSEUDO);							// Pseudo-Metal-1
		pseudoMetal2_lay.setFunction(Layer.Function.METAL2, Layer.Function.PSEUDO);							// Pseudo-Metal-2
		pseudoMetal3_lay.setFunction(Layer.Function.METAL3, Layer.Function.PSEUDO);							// Pseudo-Metal-3
		pseudoMetal4_lay.setFunction(Layer.Function.METAL4, Layer.Function.PSEUDO);							// Pseudo-Metal-4
		pseudoMetal5_lay.setFunction(Layer.Function.METAL5, Layer.Function.PSEUDO);							// Pseudo-Metal-5
		pseudoMetal6_lay.setFunction(Layer.Function.METAL6, Layer.Function.PSEUDO);							// Pseudo-Metal-6
		pseudoPoly1_lay.setFunction(Layer.Function.POLY1, Layer.Function.PSEUDO);							// Pseudo-Polysilicon-1
		pseudoPoly2_lay.setFunction(Layer.Function.POLY2, Layer.Function.PSEUDO);							// Pseudo-Polysilicon-2
		pseudoPActive_lay.setFunction(Layer.Function.DIFFP, Layer.Function.PSEUDO);							// Pseudo-P-Active
		pseudoNActive_lay.setFunction(Layer.Function.DIFFN, Layer.Function.PSEUDO);							// Pseudo-N-Active
		pseudoPSelect_lay.setFunction(Layer.Function.IMPLANTP, Layer.Function.PSEUDO);						// Pseudo-P-Select
		pseudoNSelect_lay.setFunction(Layer.Function.IMPLANTN, Layer.Function.PSEUDO);						// Pseudo-N-Select
		pseudoPWell_lay.setFunction(Layer.Function.WELLP, Layer.Function.PSEUDO);							// Pseudo-P-Well
		pseudoNWell_lay.setFunction(Layer.Function.WELLN, Layer.Function.PSEUDO);							// Pseudo-N-Well
		padFrame_lay.setFunction(Layer.Function.ART);														// Pad-Frame

		// The CIF names
		metal1_lay.setCIFLayer("CMF");				// Metal-1
		metal2_lay.setCIFLayer("CMS");				// Metal-2
		metal3_lay.setCIFLayer("CMT");				// Metal-3
		metal4_lay.setCIFLayer("CMQ");				// Metal-4
		metal5_lay.setCIFLayer("CMP");				// Metal-5
		metal6_lay.setCIFLayer("CM6");				// Metal-6
		poly1_lay.setCIFLayer("CPG");				// Polysilicon-1
		poly2_lay.setCIFLayer("CEL");				// Polysilicon-2
		pActive_lay.setCIFLayer("CAA");				// P-Active
		nActive_lay.setCIFLayer("CAA");				// N-Active
		pSelect_lay.setCIFLayer("CSP");				// P-Select
		nSelect_lay.setCIFLayer("CSN");				// N-Select
		pWell_lay.setCIFLayer("CWP");				// P-Well
		nWell_lay.setCIFLayer("CWN");				// N-Well
		polyCut_lay.setCIFLayer("CCC");				// Poly-Cut
		activeCut_lay.setCIFLayer("CCC");			// Active-Cut
		via1_lay.setCIFLayer("CVA");				// Via-1
		via2_lay.setCIFLayer("CVS");				// Via-2
		via3_lay.setCIFLayer("CVT");				// Via-3
		via4_lay.setCIFLayer("CVQ");				// Via-4
		via5_lay.setCIFLayer("CV5");				// Via-5
		passivation_lay.setCIFLayer("COG");			// Passivation
		transistorPoly_lay.setCIFLayer("CPG");		// Transistor-Poly
		polyCap_lay.setCIFLayer("CPC");				// Poly-Cap
		pActiveWell_lay.setCIFLayer("CAA");			// P-Active-Well
		silicideBlock_lay.setCIFLayer("CSB");		// Silicide-Block
		pseudoMetal1_lay.setCIFLayer("");			// Pseudo-Metal-1
		pseudoMetal2_lay.setCIFLayer("");			// Pseudo-Metal-2
		pseudoMetal3_lay.setCIFLayer("");			// Pseudo-Metal-3
		pseudoMetal4_lay.setCIFLayer("");			// Pseudo-Metal-4
		pseudoMetal5_lay.setCIFLayer("");			// Pseudo-Metal-5
		pseudoMetal6_lay.setCIFLayer("");			// Pseudo-Metal-6
		pseudoPoly1_lay.setCIFLayer("");			// Pseudo-Polysilicon-1
		pseudoPoly2_lay.setCIFLayer("");			// Pseudo-Polysilicon-2
		pseudoPActive_lay.setCIFLayer("");			// Pseudo-P-Active
		pseudoNActive_lay.setCIFLayer("");			// Pseudo-N-Active
		pseudoPSelect_lay.setCIFLayer("CSP");		// Pseudo-P-Select
		pseudoNSelect_lay.setCIFLayer("CSN");		// Pseudo-N-Select
		pseudoPWell_lay.setCIFLayer("CWP");			// Pseudo-P-Well
		pseudoNWell_lay.setCIFLayer("CWN");			// Pseudo-N-Well
		padFrame_lay.setCIFLayer("XP");				// Pad-Frame

		// The GDS names
		metal1_lay.setGDSLayer("49");				// Metal-1
		metal2_lay.setGDSLayer("51");				// Metal-2
		metal3_lay.setGDSLayer("62");				// Metal-3
		metal4_lay.setGDSLayer("31");				// Metal-4
		metal5_lay.setGDSLayer("33");				// Metal-5
		metal6_lay.setGDSLayer("37");				// Metal-6
		poly1_lay.setGDSLayer("46");				// Polysilicon-1
		poly2_lay.setGDSLayer("56");				// Polysilicon-2
		pActive_lay.setGDSLayer("43");				// P-Active
		nActive_lay.setGDSLayer("43");				// N-Active
		pSelect_lay.setGDSLayer("44");				// P-Select
		nSelect_lay.setGDSLayer("45");				// N-Select
		pWell_lay.setGDSLayer("41");				// P-Well
		nWell_lay.setGDSLayer("42");				// N-Well
		polyCut_lay.setGDSLayer("47");				// Poly-Cut
		activeCut_lay.setGDSLayer("48");			// Active-Cut
		via1_lay.setGDSLayer("50");					// Via-1
		via2_lay.setGDSLayer("61");					// Via-2
		via3_lay.setGDSLayer("30");					// Via-3
		via4_lay.setGDSLayer("32");					// Via-4
		via5_lay.setGDSLayer("36");					// Via-5
		passivation_lay.setGDSLayer("52");			// Passivation
		transistorPoly_lay.setGDSLayer("46");		// Transistor-Poly
		polyCap_lay.setGDSLayer("28");				// Poly-Cap
		pActiveWell_lay.setGDSLayer("43");			// P-Active-Well
		silicideBlock_lay.setGDSLayer("29");		// Silicide-Block
		pseudoMetal1_lay.setGDSLayer("");			// Pseudo-Metal-1
		pseudoMetal2_lay.setGDSLayer("");			// Pseudo-Metal-2
		pseudoMetal3_lay.setGDSLayer("");			// Pseudo-Metal-3
		pseudoMetal4_lay.setGDSLayer("");			// Pseudo-Metal-4
		pseudoMetal5_lay.setGDSLayer("");			// Pseudo-Metal-5
		pseudoMetal6_lay.setGDSLayer("");			// Pseudo-Metal-6
		pseudoPoly1_lay.setGDSLayer("");			// Pseudo-Polysilicon-1
		pseudoPoly2_lay.setGDSLayer("");			// Pseudo-Polysilicon-2
		pseudoPActive_lay.setGDSLayer("");			// Pseudo-P-Active
		pseudoNActive_lay.setGDSLayer("");			// Pseudo-N-Active
		pseudoPSelect_lay.setGDSLayer("");			// Pseudo-P-Select
		pseudoNSelect_lay.setGDSLayer("");			// Pseudo-N-Select
		pseudoPWell_lay.setGDSLayer("");			// Pseudo-P-Well
		pseudoNWell_lay.setGDSLayer("");			// Pseudo-N-Well
		padFrame_lay.setGDSLayer("26");				// Pad-Frame

		// The Skill names
		metal1_lay.setSkillLayer("metal1");			// Metal-1
		metal2_lay.setSkillLayer("metal2");			// Metal-2
		metal3_lay.setSkillLayer("metal3");			// Metal-3
		metal4_lay.setSkillLayer("metal4");			// Metal-4
		metal5_lay.setSkillLayer("metal5");			// Metal-5
		metal6_lay.setSkillLayer("metal6");			// Metal-6
		poly1_lay.setSkillLayer("poly");			// Polysilicon-1
		poly2_lay.setSkillLayer("");				// Polysilicon-2
		pActive_lay.setSkillLayer("aa");			// P-Active
		nActive_lay.setSkillLayer("aa");			// N-Active
		pSelect_lay.setSkillLayer("pplus");			// P-Select
		nSelect_lay.setSkillLayer("nplus");			// N-Select
		pWell_lay.setSkillLayer("pwell");			// P-Well
		nWell_lay.setSkillLayer("nwell");			// N-Well
		polyCut_lay.setSkillLayer("pcont");			// Poly-Cut
		activeCut_lay.setSkillLayer("acont");		// Active-Cut
		via1_lay.setSkillLayer("via");				// Via-1
		via2_lay.setSkillLayer("via2");				// Via-2
		via3_lay.setSkillLayer("via3");				// Via-3
		via4_lay.setSkillLayer("via4");				// Via-4
		via5_lay.setSkillLayer("via5");				// Via-5
		passivation_lay.setSkillLayer("glasscut");	// Passivation
		transistorPoly_lay.setSkillLayer("poly");	// Transistor-Poly
		polyCap_lay.setSkillLayer("");				// Poly-Cap
		pActiveWell_lay.setSkillLayer("aa");		// P-Active-Well
		silicideBlock_lay.setSkillLayer("");		// Silicide-Block
		pseudoMetal1_lay.setSkillLayer("");			// Pseudo-Metal-1
		pseudoMetal2_lay.setSkillLayer("");			// Pseudo-Metal-2
		pseudoMetal3_lay.setSkillLayer("");			// Pseudo-Metal-3
		pseudoMetal4_lay.setSkillLayer("");			// Pseudo-Metal-4
		pseudoMetal5_lay.setSkillLayer("");			// Pseudo-Metal-5
		pseudoMetal6_lay.setSkillLayer("");			// Pseudo-Metal-6
		pseudoPoly1_lay.setSkillLayer("");			// Pseudo-Polysilicon-1
		pseudoPoly2_lay.setSkillLayer("");			// Pseudo-Polysilicon-2
		pseudoPActive_lay.setSkillLayer("");		// Pseudo-P-Active
		pseudoNActive_lay.setSkillLayer("");		// Pseudo-N-Active
		pseudoPSelect_lay.setSkillLayer("pplus");	// Pseudo-P-Select
		pseudoNSelect_lay.setSkillLayer("nplus");	// Pseudo-N-Select
		pseudoPWell_lay.setSkillLayer("pwell");		// Pseudo-P-Well
		pseudoNWell_lay.setSkillLayer("nwell");		// Pseudo-N-Well
		padFrame_lay.setSkillLayer("");				// Pad-Frame

		// The layer height
		metal1_lay.setHeight(0, 17);				// Metal-1
		metal2_lay.setHeight(0, 19);				// Metal-2
		metal3_lay.setHeight(0, 21);				// Metal-3
		metal4_lay.setHeight(0, 23);				// Metal-4
		metal5_lay.setHeight(0, 25);				// Metal-5
		metal6_lay.setHeight(0, 27);				// Metal-6
		poly1_lay.setHeight(0, 15);					// Polysilicon-1
		poly2_lay.setHeight(0, 16);					// Polysilicon-2
		pActive_lay.setHeight(0, 13);				// P-Active
		nActive_lay.setHeight(0, 13);				// N-Active
		pSelect_lay.setHeight(0, 12);				// P-Select
		nSelect_lay.setHeight(0, 12);				// N-Select
		pWell_lay.setHeight(0, 11);					// P-Well
		nWell_lay.setHeight(0, 11);					// N-Well
		polyCut_lay.setHeight(2, 16);				// Poly-Cut
		activeCut_lay.setHeight(4, 15);				// Active-Cut
		via1_lay.setHeight(2, 18);					// Via-1
		via2_lay.setHeight(2, 20);					// Via-2
		via3_lay.setHeight(2, 22);					// Via-3
		via4_lay.setHeight(2, 24);					// Via-4
		via5_lay.setHeight(2, 26);					// Via-5
		passivation_lay.setHeight(0, 30);			// Passivation
		transistorPoly_lay.setHeight(0, 15);		// Transistor-Poly
		polyCap_lay.setHeight(0, 28);				// Poly-Cap
		pActiveWell_lay.setHeight(0, 13);			// P-Active-Well
		silicideBlock_lay.setHeight(0, 10);			// Silicide-Block
		pseudoMetal1_lay.setHeight(0, 17);			// Pseudo-Metal-1
		pseudoMetal2_lay.setHeight(0, 19);			// Pseudo-Metal-2
		pseudoMetal3_lay.setHeight(0, 21);			// Pseudo-Metal-3
		pseudoMetal4_lay.setHeight(0, 23);			// Pseudo-Metal-4
		pseudoMetal5_lay.setHeight(0, 25);			// Pseudo-Metal-5
		pseudoMetal6_lay.setHeight(0, 27);			// Pseudo-Metal-6
		pseudoPoly1_lay.setHeight(0, 12);			// Pseudo-Polysilicon-1
		pseudoPoly2_lay.setHeight(0, 13);			// Pseudo-Polysilicon-2
		pseudoPActive_lay.setHeight(0, 11);			// Pseudo-P-Active
		pseudoNActive_lay.setHeight(0, 11);			// Pseudo-N-Active
		pseudoPSelect_lay.setHeight(0, 2);			// Pseudo-P-Select
		pseudoNSelect_lay.setHeight(0, 2);			// Pseudo-N-Select
		pseudoPWell_lay.setHeight(0, 0);			// Pseudo-P-Well
		pseudoNWell_lay.setHeight(0, 0);			// Pseudo-N-Well
		padFrame_lay.setHeight(0, 33);				// Pad-Frame

		// The layer height
		metal1_lay.setDefaultParasitics(0.06, 0.07, 0);			// Metal-1
		metal2_lay.setDefaultParasitics(0.06, 0.04, 0);			// Metal-2
		metal3_lay.setDefaultParasitics(0.06, 0.04, 0);			// Metal-3
		metal4_lay.setDefaultParasitics(0.03, 0.04, 0);			// Metal-4
		metal5_lay.setDefaultParasitics(0.03, 0.04, 0);			// Metal-5
		metal6_lay.setDefaultParasitics(0.03, 0.04, 0);			// Metal-6
		poly1_lay.setDefaultParasitics(2.5, 0.09, 0);			// Polysilicon-1
		poly2_lay.setDefaultParasitics(50.0, 1.0, 0);			// Polysilicon-2
		pActive_lay.setDefaultParasitics(2.5, 0.9, 0);			// P-Active
		nActive_lay.setDefaultParasitics(3.0, 0.9, 0);			// N-Active
		pSelect_lay.setDefaultParasitics(0, 0, 0);				// P-Select
		nSelect_lay.setDefaultParasitics(0, 0, 0);				// N-Select
		pWell_lay.setDefaultParasitics(0, 0, 0);				// P-Well
		nWell_lay.setDefaultParasitics(0, 0, 0);				// N-Well
		polyCut_lay.setDefaultParasitics(2.2, 0, 0);			// Poly-Cut
		activeCut_lay.setDefaultParasitics(2.5, 0, 0);			// Active-Cut
		via1_lay.setDefaultParasitics(1.0, 0, 0);				// Via-1
		via2_lay.setDefaultParasitics(0.9, 0, 0);				// Via-2
		via3_lay.setDefaultParasitics(0.8, 0, 0);				// Via-3
		via4_lay.setDefaultParasitics(0.8, 0, 0);				// Via-4
		via5_lay.setDefaultParasitics(0.8, 0, 0);				// Via-5
		passivation_lay.setDefaultParasitics(0, 0, 0);			// Passivation
		transistorPoly_lay.setDefaultParasitics(2.5, 0.09, 0);	// Transistor-Poly
		polyCap_lay.setDefaultParasitics(0, 0, 0);				// Poly-Cap
		pActiveWell_lay.setDefaultParasitics(0, 0, 0);			// P-Active-Well
		silicideBlock_lay.setDefaultParasitics(0, 0, 0);		// Silicide-Block
		pseudoMetal1_lay.setDefaultParasitics(0, 0, 0);			// Pseudo-Metal-1
		pseudoMetal2_lay.setDefaultParasitics(0, 0, 0);			// Pseudo-Metal-2
		pseudoMetal3_lay.setDefaultParasitics(0, 0, 0);			// Pseudo-Metal-3
		pseudoMetal4_lay.setDefaultParasitics(0, 0, 0);			// Pseudo-Metal-4
		pseudoMetal5_lay.setDefaultParasitics(0, 0, 0);			// Pseudo-Metal-5
		pseudoMetal6_lay.setDefaultParasitics(0, 0, 0);			// Pseudo-Metal-6
		pseudoPoly1_lay.setDefaultParasitics(0, 0, 0);			// Pseudo-Polysilicon-1
		pseudoPoly2_lay.setDefaultParasitics(0, 0, 0);			// Pseudo-Polysilicon-2
		pseudoPActive_lay.setDefaultParasitics(0, 0, 0);		// Pseudo-P-Active
		pseudoNActive_lay.setDefaultParasitics(0, 0, 0);		// Pseudo-N-Active
		pseudoPSelect_lay.setDefaultParasitics(0, 0, 0);		// Pseudo-P-Select
		pseudoNSelect_lay.setDefaultParasitics(0, 0, 0);		// Pseudo-N-Select
		pseudoPWell_lay.setDefaultParasitics(0, 0, 0);			// Pseudo-P-Well
		pseudoNWell_lay.setDefaultParasitics(0, 0, 0);			// Pseudo-N-Well
		padFrame_lay.setDefaultParasitics(0, 0, 0);				// Pad-Frame

		setDefaultParasitics(50, 0.04);

		///* The low 5 bits map Metal-1, Poly-1, Active, Metal-2, and Metal-3 */
		//static TECH_COLORMAP colmap[32] =
		//{                  /*     Metal-3 Metal-2 Active Polysilicon-1 Metal-1 */
		//	{200,200,200}, /*  0:                                              */
		//	{ 96,209,255}, /*  1:                                      Metal-1 */
		//	{255,155,192}, /*  2:                        Polysilicon-1         */
		//	{111,144,177}, /*  3:                        Polysilicon-1 Metal-1 */
		//	{107,226, 96}, /*  4:                 Active                       */
		//	{ 83,179,160}, /*  5:                 Active               Metal-1 */
		//	{161,151,126}, /*  6:                 Active Polysilicon-1         */
		//	{110,171,152}, /*  7:                 Active Polysilicon-1 Metal-1 */
		//	{224, 95,255}, /*  8:         Metal-2                              */
		//	{135,100,191}, /*  9:         Metal-2                      Metal-1 */
		//	{170, 83,170}, /* 10:         Metal-2        Polysilicon-1         */
		//	{152,104,175}, /* 11:         Metal-2        Polysilicon-1 Metal-1 */
		//	{150,124,163}, /* 12:         Metal-2 Active                       */
		//	{129,144,165}, /* 13:         Metal-2 Active               Metal-1 */
		//	{155,133,151}, /* 14:         Metal-2 Active Polysilicon-1         */
		//	{141,146,153}, /* 15:         Metal-2 Active Polysilicon-1 Metal-1 */
		//	{247,251, 20}, /* 16: Metal-3                                      */
		//	{154,186, 78}, /* 17: Metal-3                              Metal-1 */
		//	{186,163, 57}, /* 18: Metal-3                Polysilicon-1         */
		//	{167,164, 99}, /* 19: Metal-3                Polysilicon-1 Metal-1 */
		//	{156,197, 41}, /* 20: Metal-3         Active                       */
		//	{138,197, 83}, /* 21: Metal-3         Active               Metal-1 */
		//	{161,184, 69}, /* 22: Metal-3         Active Polysilicon-1         */
		//	{147,183, 97}, /* 23: Metal-3         Active Polysilicon-1 Metal-1 */
		//	{186,155, 76}, /* 24: Metal-3 Metal-2                              */
		//	{155,163,119}, /* 25: Metal-3 Metal-2                      Metal-1 */
		//	{187,142, 97}, /* 26: Metal-3 Metal-2        Polysilicon-1         */
		//	{165,146,126}, /* 27: Metal-3 Metal-2        Polysilicon-1 Metal-1 */
		//	{161,178, 82}, /* 28: Metal-3 Metal-2 Active                       */
		//	{139,182,111}, /* 29: Metal-3 Metal-2 Active               Metal-1 */
		//	{162,170, 97}, /* 30: Metal-3 Metal-2 Active Polysilicon-1         */
		//	{147,172,116}  /* 31: Metal-3 Metal-2 Active Polysilicon-1 Metal-1 */
		//};

		//******************** DESIGN RULES ********************
//		private final int WIDELIMIT = Technology.K10;					/* wide rules apply to geometry larger than this */
//
//		/* the meaning of "when" in the DRC table */
//		private final int M2=      01;		/* only applies if there are 2 metal layers in process */
//		private final int M3=      02;		/* only applies if there are 3 metal layers in process */
//		private final int M4=      04;		/* only applies if there are 4 metal layers in process */
//		private final int M5=     010;		/* only applies if there are 5 metal layers in process */
//		private final int M6=     020;		/* only applies if there are 6 metal layers in process */
//		private final int M23=     03;		/* only applies if there are 2-3 metal layers in process */
//		private final int M234=    07;		/* only applies if there are 2-4 metal layers in process */
//		private final int M2345=  017;		/* only applies if there are 2-5 metal layers in process */
//		private final int M456=   034;		/* only applies if there are 4-6 metal layers in process */
//		private final int M56=    030;		/* only applies if there are 5-6 metal layers in process */
//		private final int M3456=  036;		/* only applies if there are 3-6 metal layers in process */
//
//		private final int AC=     040;		/* only applies if alternate contact rules are in effect */
//		private final int NAC=   0100;		/* only applies if alternate contact rules are not in effect */
//		private final int SV=    0200;		/* only applies if stacked vias are allowed */
//		private final int NSV=   0400;		/* only applies if stacked vias are not allowed */
//		private final int DE=   01000;		/* only applies if deep rules are in effect */
//		private final int SU=   02000;		/* only applies if submicron rules are in effect */
//		private final int SC=   04000;		/* only applies if scmos rules are in effect */
//
//		/* the meaning of "ruletype" in the DRC table */
//		private final int MINWID=     1;		/* a minimum-width rule */
//		private final int NODSIZ=     2;		/* a node size rule */
//		private final int SURROUND=   3;		/* a general surround rule */
//		private final int VIASUR=     4;		/* a via surround rule */
//		private final int TRAWELL=    5;		/* a transistor well rule */
//		private final int TRAPOLY=    6;		/* a transistor poly rule */
//		private final int TRAACTIVE=  7;		/* a transistor active rule */
//		private final int SPACING=    8;		/* a spacing rule */
//		private final int SPACINGM=   9;		/* a multi-cut spacing rule */
//		private final int SPACINGW=  10;		/* a wide spacing rule */
//		private final int SPACINGE=  11;		/* an edge spacing rule */
//		private final int CONSPA=    12;		/* a connected spacing rule */
//		private final int UCONSPA=   13;		/* an unconnected spacing rule */
//		private final int CUTSPA=    14;		/* a contact cut spacing rule */
//		private final int CUTSIZE=   15;		/* a contact cut size rule */
//		private final int CUTSUR=    16;		/* a contact cut surround rule */
//		private final int ASURROUND= 17;		/* arc surround rule */
//
//		struct
//		{
//			CHAR *rule;				/* the name of the rule */
//			INTBIG when;			/* when the rule is used */
//			INTBIG ruletype;		/* the type of the rule */
//			CHAR *layer1, *layer2;	/* two layers that are used by the rule */
//			INTBIG distance;		/* the spacing of the rule */
//			CHAR *nodename;			/* the node that is used by the rule */
//		} drcrules[] =
//		{
//			{x_("1.1"),  DE|SU,           MINWID,   x_("P-Well"),          0,                   K12, 0},
//			{x_("1.1"),  DE|SU,           MINWID,   x_("N-Well"),          0,                   K12, 0},
//			{x_("1.1"),  DE|SU,           MINWID,   x_("Pseudo-P-Well"),   0,                   K12, 0},
//			{x_("1.1"),  DE|SU,           MINWID,   x_("Pseudo-N-Well"),   0,                   K12, 0},
//			{x_("1.1"),  SC,              MINWID,   x_("P-Well"),          0,                   K10, 0},
//			{x_("1.1"),  SC,              MINWID,   x_("N-Well"),          0,                   K10, 0},
//			{x_("1.1"),  SC,              MINWID,   x_("Pseudo-P-Well"),   0,                   K10, 0},
//			{x_("1.1"),  SC,              MINWID,   x_("Pseudo-N-Well"),   0,                   K10, 0},
//
//			{x_("1.2"),  DE|SU,           UCONSPA,  x_("P-Well"),         x_("P-Well"),         K18, 0},
//			{x_("1.2"),  DE|SU,           UCONSPA,  x_("N-Well"),         x_("N-Well"),         K18, 0},
//			{x_("1.2"),  SC,              UCONSPA,  x_("P-Well"),         x_("P-Well"),         K9,  0},
//			{x_("1.2"),  SC,              UCONSPA,  x_("N-Well"),         x_("N-Well"),         K9,  0},
//
//			{x_("1.3"),  0,               CONSPA,   x_("P-Well"),         x_("P-Well"),         K6,  0},
//			{x_("1.3"),  0,               CONSPA,   x_("N-Well"),         x_("N-Well"),         K6,  0},
//
//			{x_("1.4"),  0,               UCONSPA,  x_("P-Well"),         x_("N-Well"),         0,   0},
//
//			{x_("2.1"),  0,               MINWID,   x_("P-Active"),        0,                   K3,  0},
//			{x_("2.1"),  0,               MINWID,   x_("N-Active"),        0,                   K3,  0},
//
//			{x_("2.2"),  0,               SPACING,  x_("P-Active"),       x_("P-Active"),       K3,  0},
//			{x_("2.2"),  0,               SPACING,  x_("N-Active"),       x_("N-Active"),       K3,  0},
//			{x_("2.2"),  0,               SPACING,  x_("P-Active-Well"),  x_("P-Active-Well"),  K3,  0},
//			{x_("2.2"),  0,               SPACING,  x_("P-Active"),       x_("N-Active"),       K3,  0},
//			{x_("2.2"),  0,               SPACING,  x_("P-Active"),       x_("P-Active-Well"),  K3,  0},
//			{x_("2.2"),  0,               SPACING,  x_("N-Active"),       x_("P-Active-Well"),  K3,  0},
//
//			{x_("2.3"),  DE|SU,           SURROUND, x_("N-Well"),         x_("P-Active"),       K6, x_("Metal-1-P-Active-Con")},
//			{x_("2.3"),  DE|SU,           ASURROUND,x_("N-Well"),         x_("P-Active"),       K6, x_("P-Active")},
//			{x_("2.3"),  DE|SU,           SURROUND, x_("P-Well"),         x_("N-Active"),       K6, x_("Metal-1-N-Active-Con")},
//			{x_("2.3"),  DE|SU,           ASURROUND,x_("P-Well"),         x_("N-Active"),       K6, x_("N-Active")},
//			{x_("2.3"),  DE|SU,           TRAWELL,   0,                    0,                   K6,  0},
//			{x_("2.3"),  SC,              SURROUND, x_("N-Well"),         x_("P-Active"),       K5, x_("Metal-1-P-Active-Con")},
//			{x_("2.3"),  SC,              ASURROUND,x_("N-Well"),         x_("P-Active"),       K5, x_("P-Active")},
//			{x_("2.3"),  SC,              SURROUND, x_("P-Well"),         x_("N-Active"),       K5, x_("Metal-1-N-Active-Con")},
//			{x_("2.3"),  SC,              ASURROUND,x_("P-Well"),         x_("N-Active"),       K5, x_("N-Active")},
//			{x_("2.3"),  SC,              TRAWELL,   0,                    0,                   K5,  0},
//
//			{x_("3.1"),  0,               MINWID,   x_("Polysilicon-1"),   0,                   K2,  0},
//			{x_("3.1"),  0,               MINWID,   x_("Transistor-Poly"), 0,                   K2,  0},
//
//			{x_("3.2"),  DE|SU,           SPACING,  x_("Polysilicon-1"),  x_("Polysilicon-1"),  K3,  0},
//			{x_("3.2"),  DE|SU,           SPACING,  x_("Polysilicon-1"),  x_("Transistor-Poly"),K3,  0},
//			{x_("3.2"),  SC,              SPACING,  x_("Polysilicon-1"),  x_("Polysilicon-1"),  K2,  0},
//			{x_("3.2"),  SC,              SPACING,  x_("Polysilicon-1"),  x_("Transistor-Poly"),K2,  0},
//
//			{x_("3.2a"), DE,              SPACING,  x_("Transistor-Poly"),x_("Transistor-Poly"),K4,  0},
//			{x_("3.2a"), SU,              SPACING,  x_("Transistor-Poly"),x_("Transistor-Poly"),K3,  0},
//			{x_("3.2a"), SC,              SPACING,  x_("Transistor-Poly"),x_("Transistor-Poly"),K2,  0},
//
//			{x_("3.3"),  DE,              TRAPOLY,   0,                    0,                   H2,  0},
//			{x_("3.3"),  SU|SC,           TRAPOLY,   0,                    0,                   K2,  0},
//
//			{x_("3.4"),  DE,              TRAACTIVE, 0,                    0,                   K4,  0},
//			{x_("3.4"),  SU|SC,           TRAACTIVE, 0,                    0,                   K3,  0},
//
//			{x_("3.5"),  0,               SPACING,  x_("Polysilicon-1"),  x_("P-Active"),       K1,  0},
//			{x_("3.5"),  0,               SPACING,  x_("Transistor-Poly"),x_("P-Active"),       K1,  0},
//			{x_("3.5"),  0,               SPACING,  x_("Polysilicon-1"),  x_("N-Active"),       K1,  0},
//			{x_("3.5"),  0,               SPACING,  x_("Transistor-Poly"),x_("N-Active"),       K1,  0},
//			{x_("3.5"),  0,               SPACING,  x_("Polysilicon-1"),  x_("P-Active-Well"),  K1,  0},
//			{x_("3.5"),  0,               SPACING,  x_("Transistor-Poly"),x_("P-Active-Well"),  K1,  0},
//
//			{x_("4.4"),  DE,              MINWID,   x_("P-Select"),        0,                   K4,  0},
//			{x_("4.4"),  DE,              MINWID,   x_("N-Select"),        0,                   K4,  0},
//			{x_("4.4"),  DE,              MINWID,   x_("Pseudo-P-Select"), 0,                   K4,  0},
//			{x_("4.4"),  DE,              MINWID,   x_("Pseudo-N-Select"), 0,                   K4,  0},
//			{x_("4.4"),  DE,              SPACING,  x_("P-Select"),       x_("P-Select"),       K4,  0},
//			{x_("4.4"),  DE,              SPACING,  x_("N-Select"),       x_("N-Select"),       K4,  0},
//			{x_("4.4"),  SU|SC,           MINWID,   x_("P-Select"),        0,                   K2,  0},
//			{x_("4.4"),  SU|SC,           MINWID,   x_("N-Select"),        0,                   K2,  0},
//			{x_("4.4"),  SU|SC,           MINWID,   x_("Pseudo-P-Select"), 0,                   K2,  0},
//			{x_("4.4"),  SU|SC,           MINWID,   x_("Pseudo-N-Select"), 0,                   K2,  0},
//			{x_("4.4"),  SU|SC,           SPACING,  x_("P-Select"),       x_("P-Select"),       K2,  0},
//			{x_("4.4"),  SU|SC,           SPACING,  x_("N-Select"),       x_("N-Select"),       K2,  0},
//			{x_("4.4"),  0,               SPACING,  x_("P-Select"),       x_("N-Select"),       0,   0},
//
//			{x_("5.1"),  0,               MINWID,   x_("Poly-Cut"),        0,                   K2,  0},
//
//			{x_("5.2"),        NAC,       NODSIZ,    0,                    0,                   K5, x_("Metal-1-Polysilicon-1-Con")},
//			{x_("5.2"),        NAC,       SURROUND, x_("Polysilicon-1"),  x_("Metal-1"),        H0, x_("Metal-1-Polysilicon-1-Con")},
//			{x_("5.2"),        NAC,       CUTSUR,    0,                    0,                   H1, x_("Metal-1-Polysilicon-1-Con")},
//			{x_("5.2b"),       AC,        NODSIZ,    0,                    0,                   K4, x_("Metal-1-Polysilicon-1-Con")},
//			{x_("5.2b"),       AC,        SURROUND, x_("Polysilicon-1"),  x_("Metal-1"),        0,  x_("Metal-1-Polysilicon-1-Con")},
//			{x_("5.2b"),       AC,        CUTSUR,    0,                    0,                   K1, x_("Metal-1-Polysilicon-1-Con")},
//
//			{x_("5.3"),     DE,           CUTSPA,    0,                    0,                   K4, x_("Metal-1-Polysilicon-1-Con")},
//			{x_("5.3"),     DE,           SPACING,  x_("Poly-Cut"),       x_("Poly-Cut"),       K4,  0},
//			{x_("5.3,6.3"), DE|NAC,       SPACING,  x_("Active-Cut"),     x_("Poly-Cut"),       K4,  0},
//			{x_("5.3"),     SU,           CUTSPA,    0,                    0,                   K3, x_("Metal-1-Polysilicon-1-Con")},
//			{x_("5.3"),     SU,           SPACING,  x_("Poly-Cut"),       x_("Poly-Cut"),       K3,  0},
//			{x_("5.3,6.3"), SU|NAC,       SPACING,  x_("Active-Cut"),     x_("Poly-Cut"),       K3,  0},
//			{x_("5.3"),     SC,           CUTSPA,    0,                    0,                   K2, x_("Metal-1-Polysilicon-1-Con")},
//			{x_("5.3"),     SC,           SPACING,  x_("Poly-Cut"),       x_("Poly-Cut"),       K2,  0},
//			{x_("5.3,6.3"), SC|NAC,       SPACING,  x_("Active-Cut"),     x_("Poly-Cut"),       K2,  0},
//
//			{x_("5.4"),  0,               SPACING,  x_("Poly-Cut"),       x_("Transistor-Poly"),K2,  0},
//
//			{x_("5.5b"), DE|SU|AC,        UCONSPA,  x_("Poly-Cut"),       x_("Polysilicon-1"),  K5,  0},
//			{x_("5.5b"), DE|SU|AC,        UCONSPA,  x_("Poly-Cut"),       x_("Transistor-Poly"),K5,  0},
//			{x_("5.5b"), SC|   AC,        UCONSPA,  x_("Poly-Cut"),       x_("Polysilicon-1"),  K4,  0},
//			{x_("5.5b"), SC|   AC,        UCONSPA,  x_("Poly-Cut"),       x_("Transistor-Poly"),K4,  0},
//
//			{x_("5.6b"),       AC,        SPACING,  x_("Poly-Cut"),       x_("P-Active"),       K2,  0},
//			{x_("5.6b"),       AC,        SPACING,  x_("Poly-Cut"),       x_("N-Active"),       K2,  0},
//
//			{x_("5.7b"),       AC,        SPACINGM, x_("Poly-Cut"),       x_("P-Active"),       K3,  0},
//			{x_("5.7b"),       AC,        SPACINGM, x_("Poly-Cut"),       x_("N-Active"),       K3,  0},
//
//			{x_("6.1"),  0,               MINWID,   x_("Active-Cut"),      0,                   K2,  0},
//
//			{x_("6.2"),        NAC,       NODSIZ,    0,                    0,                   K5, x_("Metal-1-P-Active-Con")},
//			{x_("6.2"),        NAC,       SURROUND, x_("P-Active"),       x_("Metal-1"),        H0, x_("Metal-1-P-Active-Con")},
//			{x_("6.2"),        NAC,       SURROUND, x_("P-Select"),       x_("P-Active"),       K2, x_("Metal-1-P-Active-Con")},
//			{x_("6.2"),  DE|SU|NAC,       SURROUND, x_("N-Well"),         x_("P-Active"),       K6, x_("Metal-1-P-Active-Con")},
//			{x_("6.2"),  SC|   NAC,       SURROUND, x_("N-Well"),         x_("P-Active"),       K5, x_("Metal-1-P-Active-Con")},
//			{x_("6.2"),        NAC,       CUTSUR,    0,                    0,                   H1, x_("Metal-1-P-Active-Con")},
//			{x_("6.2b"),       AC,        NODSIZ,    0,                    0,                   K4, x_("Metal-1-P-Active-Con")},
//			{x_("6.2b"),       AC,        SURROUND, x_("P-Active"),       x_("Metal-1"),        0,  x_("Metal-1-P-Active-Con")},
//			{x_("6.2b"),       AC,        SURROUND, x_("P-Select"),       x_("P-Active"),       K2, x_("Metal-1-P-Active-Con")},
//			{x_("6.2b"), DE|SU|AC,        SURROUND, x_("N-Well"),         x_("P-Active"),       K6, x_("Metal-1-P-Active-Con")},
//			{x_("6.2b"), SC|   AC,        SURROUND, x_("N-Well"),         x_("P-Active"),       K5, x_("Metal-1-P-Active-Con")},
//			{x_("6.2b"),       AC,        CUTSUR,    0,                    0,                   K1, x_("Metal-1-P-Active-Con")},
//
//			{x_("6.2"),        NAC,       NODSIZ,    0,                    0,                   K5, x_("Metal-1-N-Active-Con")},
//			{x_("6.2"),        NAC,       SURROUND, x_("N-Active"),       x_("Metal-1"),        H0, x_("Metal-1-N-Active-Con")},
//			{x_("6.2"),        NAC,       SURROUND, x_("N-Select"),       x_("N-Active"),       K2, x_("Metal-1-N-Active-Con")},
//			{x_("6.2"),  DE|SU|NAC,       SURROUND, x_("P-Well"),         x_("N-Active"),       K6, x_("Metal-1-N-Active-Con")},
//			{x_("6.2"),  SC|   NAC,       SURROUND, x_("P-Well"),         x_("N-Active"),       K5, x_("Metal-1-N-Active-Con")},
//			{x_("6.2"),        NAC,       CUTSUR,    0,                    0,                   H1, x_("Metal-1-N-Active-Con")},
//			{x_("6.2b"),       AC,        NODSIZ,    0,                    0,                   K4, x_("Metal-1-N-Active-Con")},
//			{x_("6.2b"),       AC,        SURROUND, x_("N-Active"),       x_("Metal-1"),        0,  x_("Metal-1-N-Active-Con")},
//			{x_("6.2b"),       AC,        SURROUND, x_("N-Select"),       x_("N-Active"),       K2, x_("Metal-1-N-Active-Con")},
//			{x_("6.2b"), DE|SU|AC,        SURROUND, x_("P-Well"),         x_("N-Active"),       K6, x_("Metal-1-N-Active-Con")},
//			{x_("6.2b"), SC|   AC,        SURROUND, x_("P-Well"),         x_("N-Active"),       K5, x_("Metal-1-N-Active-Con")},
//			{x_("6.2b"),       AC,        CUTSUR,    0,                    0,                   K1, x_("Metal-1-N-Active-Con")},
//
//			{x_("6.2"),        NAC,       NODSIZ,    0,                    0,                   K5, x_("Metal-1-P-Well-Con")},
//			{x_("6.2"),        NAC,       SURROUND, x_("P-Active-Well"),  x_("Metal-1"),        H0, x_("Metal-1-P-Well-Con")},
//			{x_("6.2"),        NAC,       SURROUND, x_("P-Select"),       x_("P-Active-Well"),  K2, x_("Metal-1-P-Well-Con")},
//			{x_("6.2"),        NAC,       SURROUND, x_("P-Well"),         x_("P-Active-Well"),  K3, x_("Metal-1-P-Well-Con")},
//			{x_("6.2"),        NAC,       CUTSUR,    0,                    0,                   H1, x_("Metal-1-P-Well-Con")},
//			{x_("6.2b"),       AC,        NODSIZ,    0,                    0,                   K4, x_("Metal-1-P-Well-Con")},
//			{x_("6.2b"),       AC,        SURROUND, x_("P-Active-Well"),  x_("Metal-1"),        0,  x_("Metal-1-P-Well-Con")},
//			{x_("6.2b"),       AC,        SURROUND, x_("P-Select"),       x_("P-Active-Well"),  K2, x_("Metal-1-P-Well-Con")},
//			{x_("6.2b"),       AC,        SURROUND, x_("P-Well"),         x_("P-Active-Well"),  K3, x_("Metal-1-P-Well-Con")},
//			{x_("6.2b"),       AC,        CUTSUR,    0,                    0,                   K1, x_("Metal-1-P-Well-Con")},
//
//			{x_("6.2"),        NAC,       NODSIZ,    0,                    0,                   K5, x_("Metal-1-N-Well-Con")},
//			{x_("6.2"),        NAC,       SURROUND, x_("N-Active"),       x_("Metal-1"),        H0, x_("Metal-1-N-Well-Con")},
//			{x_("6.2"),        NAC,       SURROUND, x_("N-Select"),       x_("N-Active"),       K2, x_("Metal-1-N-Well-Con")},
//			{x_("6.2"),        NAC,       SURROUND, x_("N-Well"),         x_("N-Active"),       K3, x_("Metal-1-N-Well-Con")},
//			{x_("6.2"),        NAC,       CUTSUR,    0,                    0,                   H1, x_("Metal-1-N-Well-Con")},
//			{x_("6.2b"),       AC,        NODSIZ,    0,                    0,                   K4, x_("Metal-1-N-Well-Con")},
//			{x_("6.2b"),       AC,        SURROUND, x_("N-Active"),       x_("Metal-1"),        0,  x_("Metal-1-N-Well-Con")},
//			{x_("6.2b"),       AC,        SURROUND, x_("N-Select"),       x_("N-Active"),       K2, x_("Metal-1-N-Well-Con")},
//			{x_("6.2b"),       AC,        SURROUND, x_("N-Well"),         x_("N-Active"),       K3, x_("Metal-1-N-Well-Con")},
//			{x_("6.2b"),       AC,        CUTSUR,    0,                    0,                   K1, x_("Metal-1-N-Well-Con")},
//
//			{x_("6.3"),  DE,              CUTSPA,    0,                    0,                   K4, x_("Metal-1-P-Active-Con")},
//			{x_("6.3"),  DE,              CUTSPA,    0,                    0,                   K4, x_("Metal-1-N-Active-Con")},
//			{x_("6.3"),  DE,              SPACING,  x_("Active-Cut"),     x_("Active-Cut"),     K4,  0},
//			{x_("6.3"),  SU,              CUTSPA,    0,                    0,                   K3, x_("Metal-1-P-Active-Con")},
//			{x_("6.3"),  SU,              CUTSPA,    0,                    0,                   K3, x_("Metal-1-N-Active-Con")},
//			{x_("6.3"),  SU,              SPACING,  x_("Active-Cut"),     x_("Active-Cut"),     K3,  0},
//			{x_("6.3"),  SC,              CUTSPA,    0,                    0,                   K2, x_("Metal-1-P-Active-Con")},
//			{x_("6.3"),  SC,              CUTSPA,    0,                    0,                   K2, x_("Metal-1-N-Active-Con")},
//			{x_("6.3"),  SC,              SPACING,  x_("Active-Cut"),     x_("Active-Cut"),     K2,  0},
//
//			{x_("6.4"),  0,               SPACING,  x_("Active-Cut"),     x_("Transistor-Poly"),K2,  0},
//
//			{x_("6.5b"),       AC,        UCONSPA,  x_("Active-Cut"),     x_("P-Active"),       K5,  0},
//			{x_("6.5b"),       AC,        UCONSPA,  x_("Active-Cut"),     x_("N-Active"),       K5,  0},
//
//			{x_("6.6b"),       AC,        SPACING,  x_("Active-Cut"),     x_("Polysilicon-1"),  K2,  0},
//			{x_("6.8b"),       AC,        SPACING,  x_("Active-Cut"),     x_("Poly-Cut"),       K4,  0},
//
//			{x_("7.1"),  0,               MINWID,   x_("Metal-1"),         0,                   K3,  0},
//
//			{x_("7.2"),  DE|SU,           SPACING,  x_("Metal-1"),        x_("Metal-1"),        K3,  0},
//			{x_("7.2"),  SC,              SPACING,  x_("Metal-1"),        x_("Metal-1"),        K2,  0},
//
//			{x_("7.4"),  DE|SU,           SPACINGW, x_("Metal-1"),        x_("Metal-1"),        K6,  0},
//			{x_("7.4"),  SC,              SPACINGW, x_("Metal-1"),        x_("Metal-1"),        K4,  0},
//
//			{x_("8.1"),  DE,              CUTSIZE,   0,                    0,                   K3, x_("Metal-1-Metal-2-Con")},
//			{x_("8.1"),  DE,              NODSIZ,    0,                    0,                   K5, x_("Metal-1-Metal-2-Con")},
//			{x_("8.1"),  SU|SC,           CUTSIZE,   0,                    0,                   K2, x_("Metal-1-Metal-2-Con")},
//			{x_("8.1"),  SU|SC,           NODSIZ,    0,                    0,                   K4, x_("Metal-1-Metal-2-Con")},
//
//			{x_("8.2"),  0,               SPACING,  x_("Via1"),           x_("Via1"),           K3,  0},
//
//			{x_("8.3"),  0,               VIASUR,   x_("Metal-1"),         0,                   K1, x_("Metal-1-Metal-2-Con")},
//
//			{x_("8.4"),        NSV,       SPACING,  x_("Poly-Cut"),       x_("Via1"),           K2,  0},
//			{x_("8.4"),        NSV,       SPACING,  x_("Active-Cut"),     x_("Via1"),           K2,  0},
//
//			{x_("8.5"),        NSV,       SPACINGE, x_("Via1"),           x_("Polysilicon-1"),  K2,  0},
//			{x_("8.5"),        NSV,       SPACINGE, x_("Via1"),           x_("Transistor-Poly"),K2,  0},
//			{x_("8.5"),        NSV,       SPACINGE, x_("Via1"),           x_("Polysilicon-2"),  K2,  0},
//			{x_("8.5"),        NSV,       SPACINGE, x_("Via1"),           x_("P-Active"),       K2,  0},
//			{x_("8.5"),        NSV,       SPACINGE, x_("Via1"),           x_("N-Active"),       K2,  0},
//
//			{x_("9.1"),  0,               MINWID,   x_("Metal-2"),         0,                   K3,  0},
//
//			{x_("9.2"),  DE,              SPACING,  x_("Metal-2"),        x_("Metal-2"),        K4,  0},
//			{x_("9.2"),  SU|SC,           SPACING,  x_("Metal-2"),        x_("Metal-2"),        K3,  0},
//
//			{x_("9.3"),  0,               VIASUR,   x_("Metal-2"),         0,                   K1, x_("Metal-1-Metal-2-Con")},
//
//			{x_("9.4"),  DE,              SPACINGW, x_("Metal-2"),        x_("Metal-2"),        K8,  0},
//			{x_("9.4"),  SU|SC,           SPACINGW, x_("Metal-2"),        x_("Metal-2"),        K6,  0},
//
//			{x_("11.1"), SU,              MINWID,   x_("Polysilicon-2"),   0,                   K7,  0},
//			{x_("11.1"), SC,              MINWID,   x_("Polysilicon-2"),   0,                   K3,  0},
//
//			{x_("11.2"), 0,               SPACING,  x_("Polysilicon-2"),  x_("Polysilicon-2"),  K3,  0},
//
//			{x_("11.3"), SU,              SURROUND, x_("Polysilicon-2"),  x_("Polysilicon-1"),  K5, x_("Metal-1-Polysilicon-1-2-Con")},
//			{x_("11.3"), SU,              NODSIZ,    0,                    0,                   K15,x_("Metal-1-Polysilicon-1-2-Con")},
//			{x_("11.3"), SU,              CUTSUR,    0,                    0,                   H6, x_("Metal-1-Polysilicon-1-2-Con")},
//			{x_("11.3"), SC,              SURROUND, x_("Polysilicon-2"),  x_("Polysilicon-1"),  K2, x_("Metal-1-Polysilicon-1-2-Con")},
//			{x_("11.3"), SC,              NODSIZ,    0,                    0,                   K9, x_("Metal-1-Polysilicon-1-2-Con")},
//			{x_("11.3"), SC,              CUTSUR,    0,                    0,                   H3, x_("Metal-1-Polysilicon-1-2-Con")},
//
//			{x_("14.1"), DE,              CUTSIZE,   0,                    0,                   K3, x_("Metal-2-Metal-3-Con")},
//			{x_("14.1"), DE,              MINWID,   x_("Via2"),            0,                   K3,  0},
//			{x_("14.1"), DE,              NODSIZ,    0,                    0,                   K5, x_("Metal-2-Metal-3-Con")},
//			{x_("14.1"), SU|SC,           CUTSIZE,   0,                    0,                   K2, x_("Metal-2-Metal-3-Con")},
//			{x_("14.1"), SU|SC,           MINWID,   x_("Via2"),            0,                   K2,  0},
//			{x_("14.1"), SU|SC|    M23,   NODSIZ,    0,                    0,                   K6, x_("Metal-2-Metal-3-Con")},
//			{x_("14.1"), SU|SC|    M456,  NODSIZ,    0,                    0,                   K4, x_("Metal-2-Metal-3-Con")},
//
//			{x_("14.2"), 0,               SPACING,  x_("Via2"),           x_("Via2"),           K3,  0},
//
//			{x_("14.3"), 0,               VIASUR,   x_("Metal-2"),         0,                   K1, x_("Metal-2-Metal-3-Con")},
//
//			{x_("14.4"), SU|SC|NSV,       SPACING,  x_("Via1"),           x_("Via2"),           K2,  0},
//
//			{x_("15.1"), SC|       M3,    MINWID,   x_("Metal-3"),         0,                   K6,  0},
//			{x_("15.1"), SU|       M3,    MINWID,   x_("Metal-3"),         0,                   K5,  0},
//			{x_("15.1"), SC|       M456,  MINWID,   x_("Metal-3"),         0,                   K3,  0},
//			{x_("15.1"), SU|       M456,  MINWID,   x_("Metal-3"),         0,                   K3,  0},
//			{x_("15.1"), DE,              MINWID,   x_("Metal-3"),         0,                   K3,  0},
//
//			{x_("15.2"), DE,              SPACING,  x_("Metal-3"),        x_("Metal-3"),        K4,  0},
//			{x_("15.2"), SU,              SPACING,  x_("Metal-3"),        x_("Metal-3"),        K3,  0},
//			{x_("15.2"), SC|       M3,    SPACING,  x_("Metal-3"),        x_("Metal-3"),        K4,  0},
//			{x_("15.2"), SC|       M456,  SPACING,  x_("Metal-3"),        x_("Metal-3"),        K3,  0},
//
//			{x_("15.3"), DE,              VIASUR,   x_("Metal-3"),         0,                   K1, x_("Metal-2-Metal-3-Con")},
//			{x_("15.3"), SU|SC|    M3,    VIASUR,   x_("Metal-3"),         0,                   K2, x_("Metal-2-Metal-3-Con")},
//			{x_("15.3"), SU|SC|    M456,  VIASUR,   x_("Metal-3"),         0,                   K1, x_("Metal-2-Metal-3-Con")},
//
//			{x_("15.4"), DE,              SPACINGW, x_("Metal-3"),        x_("Metal-3"),        K8,  0},
//			{x_("15.4"), SU,              SPACINGW, x_("Metal-3"),        x_("Metal-3"),        K6,  0},
//			{x_("15.4"), SC|       M3,    SPACINGW, x_("Metal-3"),        x_("Metal-3"),        K8,  0},
//			{x_("15.4"), SC|       M456,  SPACINGW, x_("Metal-3"),        x_("Metal-3"),        K6,  0},
//
//			{x_("21.1"), DE,              CUTSIZE,   0,                    0,                   K3, x_("Metal-3-Metal-4-Con")},
//			{x_("21.1"), DE,              MINWID,   x_("Via3"),            0,                   K3,  0},
//			{x_("21.1"), DE,              NODSIZ,    0,                    0,                   K5, x_("Metal-3-Metal-4-Con")},
//			{x_("21.1"), SU|SC,           CUTSIZE,   0,                    0,                   K2, x_("Metal-3-Metal-4-Con")},
//			{x_("21.1"), SU|SC,           MINWID,   x_("Via3"),            0,                   K2,  0},
//			{x_("21.1"), SU|       M4,    NODSIZ,    0,                    0,                   K6, x_("Metal-3-Metal-4-Con")},
//			{x_("21.1"), SU|       M56,   NODSIZ,    0,                    0,                   K4, x_("Metal-3-Metal-4-Con")},
//			{x_("21.1"), SC,              NODSIZ,    0,                    0,                   K6, x_("Metal-3-Metal-4-Con")},
//
//			{x_("21.2"), 0,               SPACING,  x_("Via3"),           x_("Via3"),           K3,  0},
//
//			{x_("21.3"), 0,               VIASUR,   x_("Metal-3"),         0,                   K1, x_("Metal-3-Metal-4-Con")},
//
//			{x_("22.1"),           M4,    MINWID,   x_("Metal-4"),         0,                   K6,  0},
//			{x_("22.1"),           M56,   MINWID,   x_("Metal-4"),         0,                   K3,  0},
//
//			{x_("22.2"),           M4,    SPACING,  x_("Metal-4"),        x_("Metal-4"),        K6,  0},
//			{x_("22.2"), DE|       M56,   SPACING,  x_("Metal-4"),        x_("Metal-4"),        K4,  0},
//			{x_("22.2"), SU|       M56,   SPACING,  x_("Metal-4"),        x_("Metal-4"),        K3,  0},
//
//			{x_("22.3"),           M4,    VIASUR,   x_("Metal-4"),         0,                   K2, x_("Metal-3-Metal-4-Con")},
//			{x_("22.3"),           M56,   VIASUR,   x_("Metal-4"),         0,                   K1, x_("Metal-3-Metal-4-Con")},
//
//			{x_("22.4"),           M4,    SPACINGW, x_("Metal-4"),        x_("Metal-4"),        K12, 0},
//			{x_("22.4"), DE|       M56,   SPACINGW, x_("Metal-4"),        x_("Metal-4"),        K8,  0},
//			{x_("22.4"), SU|       M56,   SPACINGW, x_("Metal-4"),        x_("Metal-4"),        K6,  0},
//
//			{x_("25.1"), DE,              CUTSIZE,   0,                    0,                   K3, x_("Metal-4-Metal-5-Con")},
//			{x_("25.1"), DE,              MINWID,   x_("Via4"),            0,                   K3,  0},
//			{x_("25.1"), SU,              CUTSIZE,   0,                    0,                   K2, x_("Metal-4-Metal-5-Con")},
//			{x_("25.1"), SU,              MINWID,   x_("Via4"),            0,                   K2,  0},
//			{x_("25.1"), SU,              NODSIZ,    0,                    0,                   K4, x_("Metal-4-Metal-5-Con")},
//			{x_("25.1"), DE|       M5,    NODSIZ,    0,                    0,                   K7, x_("Metal-4-Metal-5-Con")},
//			{x_("25.1"), DE|       M6,    NODSIZ,    0,                    0,                   K5, x_("Metal-4-Metal-5-Con")},
//
//			{x_("25.2"), 0,               SPACINGW, x_("Via4"),           x_("Via4"),           K3,  0},
//
//			{x_("25.3"), 0,               VIASUR,   x_("Metal-4"),         0,                   K1, x_("Metal-4-Metal-5-Con")},
//
//			{x_("26.1"),           M5,    MINWID,   x_("Metal-5"),         0,                   K4,  0},
//			{x_("26.1"),           M6,    MINWID,   x_("Metal-5"),         0,                   K3,  0},
//
//			{x_("26.2"),           M5,    SPACING,  x_("Metal-5"),        x_("Metal-5"),        K4,  0},
//			{x_("26.2"), DE|       M6,    SPACING,  x_("Metal-5"),        x_("Metal-5"),        K4,  0},
//			{x_("26.2"), SU|       M6,    SPACING,  x_("Metal-5"),        x_("Metal-5"),        K3,  0},
//
//			{x_("26.3"), DE|       M5,    VIASUR,   x_("Metal-5"),         0,                   K2, x_("Metal-4-Metal-5-Con")},
//			{x_("26.3"), SU|       M5,    VIASUR,   x_("Metal-5"),         0,                   K1, x_("Metal-4-Metal-5-Con")},
//			{x_("26.3"),           M6,    VIASUR,   x_("Metal-5"),         0,                   K1, x_("Metal-4-Metal-5-Con")},
//
//			{x_("26.4"),           M5,    SPACINGW, x_("Metal-5"),        x_("Metal-5"),        K8,  0},
//			{x_("26.4"), DE|       M6,    SPACINGW, x_("Metal-5"),        x_("Metal-5"),        K8,  0},
//			{x_("26.4"), SU|       M6,    SPACINGW, x_("Metal-5"),        x_("Metal-5"),        K6,  0},
//
//			{x_("29.1"), DE,              CUTSIZE,   0,                    0,                   K4, x_("Metal-5-Metal-6-Con")},
//			{x_("29.1"), DE,              MINWID,   x_("Via5"),            0,                   K4,  0},
//			{x_("29.1"), DE,              NODSIZ,    0,                    0,                   K8, x_("Metal-5-Metal-6-Con")},
//			{x_("29.1"), SU,              CUTSIZE,   0,                    0,                   K3, x_("Metal-5-Metal-6-Con")},
//			{x_("29.1"), SU,              MINWID,   x_("Via5"),            0,                   K3,  0},
//			{x_("29.1"), SU,              NODSIZ,    0,                    0,                   K5, x_("Metal-5-Metal-6-Con")},
//
//			{x_("29.2"), 0,               SPACING,  x_("Via5"),           x_("Via5"),           K4,  0},
//
//			{x_("29.3"), 0,               VIASUR,   x_("Metal-5"),         0,                   K1, x_("Metal-5-Metal-6-Con")},
//
//			{x_("30.1"), 0,               MINWID,   x_("Metal-6"),         0,                   K4,  0},
//
//			{x_("30.2"), 0,               SPACING,  x_("Metal-6"),        x_("Metal-6"),        K4,  0},
//
//			{x_("30.3"), DE,              VIASUR,   x_("Metal-6"),         0,                   K2, x_("Metal-5-Metal-6-Con")},
//			{x_("30.3"), SU,              VIASUR,   x_("Metal-6"),         0,                   K1, x_("Metal-5-Metal-6-Con")},
//
//			{x_("30.4"), 0,               SPACINGW, x_("Metal-6"),        x_("Metal-6"),        K8,  0},
//
//			{0,      0,               0,             0,                    0,                   0,   0}
//		};

		//**************************************** ARCS ****************************************

		/** metal 1 arc */
		PrimitiveArc metal1_arc = PrimitiveArc.newInstance(this, "Metal-1", 3.0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(metal1_lay, 0, Poly.Type.FILLED)
		});
		metal1_arc.setFunction(PrimitiveArc.Function.METAL1);
		metal1_arc.setFixedAngle();
		metal1_arc.setWipable();
		metal1_arc.setAngleIncrement(90);

		/** metal 2 arc */
		PrimitiveArc metal2_arc = PrimitiveArc.newInstance(this, "Metal-2", 3.0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(metal2_lay, 0, Poly.Type.FILLED)
		});
		metal2_arc.setFunction(PrimitiveArc.Function.METAL2);
		metal2_arc.setFixedAngle();
		metal2_arc.setWipable();
		metal2_arc.setAngleIncrement(90);

		/** metal 3 arc */
		PrimitiveArc metal3_arc = PrimitiveArc.newInstance(this, "Metal-3", 3.0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(metal3_lay, 0, Poly.Type.FILLED)
		});
		metal3_arc.setFunction(PrimitiveArc.Function.METAL3);
		metal3_arc.setFixedAngle();
		metal3_arc.setWipable();
		metal3_arc.setAngleIncrement(90);

		/** metal 4 arc */
		PrimitiveArc metal4_arc = PrimitiveArc.newInstance(this, "Metal-4", 3.0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(metal4_lay, 0, Poly.Type.FILLED)
		});
		metal4_arc.setFunction(PrimitiveArc.Function.METAL4);
		metal4_arc.setFixedAngle();
		metal4_arc.setWipable();
		metal4_arc.setAngleIncrement(90);

		/** metal 5 arc */
		PrimitiveArc metal5_arc = PrimitiveArc.newInstance(this, "Metal-5", 3.0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(metal5_lay, 0, Poly.Type.FILLED)
		});
		metal5_arc.setFunction(PrimitiveArc.Function.METAL5);
		metal5_arc.setFixedAngle();
		metal5_arc.setWipable();
		metal5_arc.setAngleIncrement(90);

		/** metal 6 arc */
		PrimitiveArc metal6_arc = PrimitiveArc.newInstance(this, "Metal-6", 4.0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(metal6_lay, 0, Poly.Type.FILLED)
		});
		metal6_arc.setFunction(PrimitiveArc.Function.METAL6);
		metal6_arc.setFixedAngle();
		metal6_arc.setWipable();
		metal6_arc.setAngleIncrement(90);

		/** polysilicon 1 arc */
		PrimitiveArc poly1_arc = PrimitiveArc.newInstance(this, "Polysilicon-1", 2.0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(poly1_lay, 0, Poly.Type.FILLED)
		});
		poly1_arc.setFunction(PrimitiveArc.Function.POLY1);
		poly1_arc.setFixedAngle();
		poly1_arc.setWipable();
		poly1_arc.setAngleIncrement(90);

		/** polysilicon 2 arc */
		PrimitiveArc poly2_arc = PrimitiveArc.newInstance(this, "Polysilicon-2", 7.0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(poly2_lay, 0, Poly.Type.FILLED)
		});
		poly2_arc.setFunction(PrimitiveArc.Function.POLY2);
		poly2_arc.setFixedAngle();
		poly2_arc.setWipable();
		poly2_arc.setAngleIncrement(90);
		poly2_arc.setNotUsed();

		/** P-active arc */
		PrimitiveArc pActive_arc = PrimitiveArc.newInstance(this, "P-Active", 15.0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(pActive_lay, 12, Poly.Type.FILLED),
			new Technology.ArcLayer(nWell_lay, 0, Poly.Type.FILLED),
			new Technology.ArcLayer(pSelect_lay, 8, Poly.Type.FILLED)
		});
		pActive_arc.setFunction(PrimitiveArc.Function.DIFFP);
		pActive_arc.setFixedAngle();
		pActive_arc.setWipable();
		pActive_arc.setAngleIncrement(90);
		pActive_arc.setWidthOffset(12.0);

		/** N-active arc */
		PrimitiveArc nActive_arc = PrimitiveArc.newInstance(this, "N-Active", 15.0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(nActive_lay, 12, Poly.Type.FILLED),
			new Technology.ArcLayer(pWell_lay, 0, Poly.Type.FILLED),
			new Technology.ArcLayer(nSelect_lay, 8, Poly.Type.FILLED)
		});
		nActive_arc.setFunction(PrimitiveArc.Function.DIFFN);
		nActive_arc.setFixedAngle();
		nActive_arc.setWipable();
		nActive_arc.setAngleIncrement(90);
		nActive_arc.setWidthOffset(12.0);

		/** General active arc */
		PrimitiveArc active_arc = PrimitiveArc.newInstance(this, "Active", 3.0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(pActive_lay, 0, Poly.Type.FILLED),
			new Technology.ArcLayer(nActive_lay, 0, Poly.Type.FILLED)
		});
		active_arc.setFunction(PrimitiveArc.Function.DIFF);
		active_arc.setFixedAngle();
		active_arc.setWipable();
		active_arc.setAngleIncrement(90);
		active_arc.setNotUsed();

		//**************************************** NODES ****************************************

		/** metal-1-pin */
		metal1Pin_node = PrimitiveNode.newInstance("Metal-1-Pin", this, 3.0, 3.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(pseudoMetal1_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX)
			});
		metal1Pin_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metal1Pin_node, new ArcProto[] {metal1_arc}, "metal-1", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
			});
		metal1Pin_node.setFunction(NodeProto.Function.PIN);
		metal1Pin_node.setArcsWipe();
		metal1Pin_node.setArcsShrink();

		/** metal-2-pin */
		metal2Pin_node = PrimitiveNode.newInstance("Metal-2-Pin", this, 3.0, 3.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(pseudoMetal2_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX)
			});
		metal2Pin_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metal2Pin_node, new ArcProto[] {metal2_arc}, "metal-2", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
			});
		metal2Pin_node.setFunction(NodeProto.Function.PIN);
		metal2Pin_node.setArcsWipe();
		metal2Pin_node.setArcsShrink();

		/** metal-3-pin */
		metal3Pin_node = PrimitiveNode.newInstance("Metal-3-Pin", this, 3.0, 3.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(pseudoMetal3_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX)
			});
		metal3Pin_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metal3Pin_node, new ArcProto[] {metal3_arc}, "metal-3", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
			});
		metal3Pin_node.setFunction(NodeProto.Function.PIN);
		metal3Pin_node.setArcsWipe();
		metal3Pin_node.setArcsShrink();

		/** metal-4-pin */
		metal4Pin_node = PrimitiveNode.newInstance("Metal-4-Pin", this, 3.0, 3.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(pseudoMetal4_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX)
			});
		metal4Pin_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metal4Pin_node, new ArcProto[] {metal4_arc}, "metal-4", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
			});
		metal4Pin_node.setFunction(NodeProto.Function.PIN);
		metal4Pin_node.setArcsWipe();
		metal4Pin_node.setArcsShrink();

		/** metal-5-pin */
		metal5Pin_node = PrimitiveNode.newInstance("Metal-5-Pin", this, 3.0, 3.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(pseudoMetal5_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX)
			});
		metal5Pin_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metal5Pin_node, new ArcProto[] {metal5_arc}, "metal-5", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
			});
		metal5Pin_node.setFunction(NodeProto.Function.PIN);
		metal5Pin_node.setArcsWipe();
		metal5Pin_node.setArcsShrink();
		metal5Pin_node.setNotUsed();

		/** metal-6-pin */
		metal6Pin_node = PrimitiveNode.newInstance("Metal-6-Pin", this, 3.0, 3.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(pseudoMetal6_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX)
			});
		metal6Pin_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metal6Pin_node, new ArcProto[] {metal6_arc}, "metal-6", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
			});
		metal6Pin_node.setFunction(NodeProto.Function.PIN);
		metal6Pin_node.setArcsWipe();
		metal6Pin_node.setArcsShrink();
		metal6Pin_node.setNotUsed();

		/** polysilicon-1-pin */
		poly1Pin_node = PrimitiveNode.newInstance("Polysilicon-1-Pin", this, 2.0, 2.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(pseudoPoly1_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX)
			});
		poly1Pin_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, poly1Pin_node, new ArcProto[] {poly1_arc}, "polysilicon-1", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(1), EdgeV.fromBottom(1), EdgeH.fromRight(1), EdgeV.fromTop(1))
			});
		poly1Pin_node.setFunction(NodeProto.Function.PIN);
		poly1Pin_node.setArcsWipe();
		poly1Pin_node.setArcsShrink();

		/** polysilicon-2-pin */
		poly2Pin_node = PrimitiveNode.newInstance("Polysilicon-2-Pin", this, 3.0, 3.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(pseudoPoly2_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX)
			});
		poly2Pin_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, poly2Pin_node, new ArcProto[] {poly2_arc}, "polysilicon-2", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
			});
		poly2Pin_node.setFunction(NodeProto.Function.PIN);
		poly2Pin_node.setArcsWipe();
		poly2Pin_node.setArcsShrink();
		poly2Pin_node.setNotUsed();

		/** P-active-pin */
		pActivePin_node = PrimitiveNode.newInstance("P-Active-Pin", this, 15.0, 15.0, new SizeOffset(6, 6, 6, 6),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(pseudoPActive_lay,  0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, Technology.TechPoint.IN6BOX),
				new Technology.NodeLayer(pseudoNWell_lay,  0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX),
				new Technology.NodeLayer(pseudoPSelect_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, Technology.TechPoint.IN4BOX)
			});
		pActivePin_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, pActivePin_node, new ArcProto[] {pActive_arc}, "p-active", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(7.5), EdgeV.fromBottom(7.5), EdgeH.fromRight(7.5), EdgeV.fromTop(7.5))
			});
		pActivePin_node.setFunction(NodeProto.Function.PIN);
		pActivePin_node.setArcsWipe();
		pActivePin_node.setArcsShrink();

		/** N-active-pin */
		nActivePin_node = PrimitiveNode.newInstance("N-Active-Pin", this, 15.0, 15.0, new SizeOffset(6, 6, 6, 6),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(pseudoNActive_lay,  0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, Technology.TechPoint.IN6BOX),
				new Technology.NodeLayer(pseudoPWell_lay,  0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX),
				new Technology.NodeLayer(pseudoNSelect_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, Technology.TechPoint.IN4BOX)
			});
		nActivePin_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, nActivePin_node, new ArcProto[] {nActive_arc}, "n-active", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(7.5), EdgeV.fromBottom(7.5), EdgeH.fromRight(7.5), EdgeV.fromTop(7.5))
			});
		nActivePin_node.setFunction(NodeProto.Function.PIN);
		nActivePin_node.setArcsWipe();
		nActivePin_node.setArcsShrink();

		/** General active-pin */
		activePin_node = PrimitiveNode.newInstance("Active-Pin", this, 3.0, 3.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(pseudoPActive_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX),
				new Technology.NodeLayer(pseudoNActive_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX)
			});
		activePin_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, activePin_node, new ArcProto[] {active_arc, pActive_arc, nActive_arc}, "active", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
			});
		activePin_node.setFunction(NodeProto.Function.PIN);
		activePin_node.setArcsWipe();
		activePin_node.setArcsShrink();

		/** metal-1-P-active-contact */
		metal1PActiveContact_node = PrimitiveNode.newInstance("Metal-1-P-Active-Con", this, 17.0, 17.0, new SizeOffset(6, 6, 6, 6),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(metal1_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(6.5), EdgeV.fromBottom(6.5)),
					new Technology.TechPoint(EdgeH.fromRight(6.5), EdgeV.fromTop(6.5))}),
				new Technology.NodeLayer(pActive_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.IN6BOX),
				new Technology.NodeLayer(nWell_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX,Technology.TechPoint.FULLBOX),
				new Technology.NodeLayer(pSelect_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.IN4BOX),
				new Technology.NodeLayer(activeCut_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(7.5), EdgeV.fromBottom(7.5)),
					new Technology.TechPoint(EdgeH.fromRight(7.5), EdgeV.fromTop(7.5))})
			});
		metal1PActiveContact_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metal1PActiveContact_node, new ArcProto[] {pActive_arc, metal1_arc}, "metal-1-p-act", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(8), EdgeV.fromBottom(8), EdgeH.fromRight(8), EdgeV.fromTop(8))
			});
		metal1PActiveContact_node.setFunction(NodeProto.Function.CONTACT);
		metal1PActiveContact_node.setSpecialValues(new int [] {PrimitiveNode.MULTICUT, 2, 2, (int)1.5, 3, 0, 0, 0, 0});

		/** metal-1-N-active-contact */
		metal1NActiveContact_node = PrimitiveNode.newInstance("Metal-1-N-Active-Con", this, 17.0, 17.0, new SizeOffset(6, 6, 6, 6),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(metal1_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(6.5), EdgeV.fromBottom(6.5)),
					new Technology.TechPoint(EdgeH.fromRight(6.5), EdgeV.fromTop(6.5))}),
				new Technology.NodeLayer(nActive_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.IN6BOX),
				new Technology.NodeLayer(pWell_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX),
				new Technology.NodeLayer(nSelect_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.IN4BOX),
				new Technology.NodeLayer(activeCut_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(7.5), EdgeV.fromBottom(7.5)),
					new Technology.TechPoint(EdgeH.fromRight(7.5), EdgeV.fromTop(7.5))})
			});
		metal1NActiveContact_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metal1NActiveContact_node, new ArcProto[] {nActive_arc, metal1_arc}, "metal-1-n-act", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(8), EdgeV.fromBottom(8), EdgeH.fromRight(8), EdgeV.fromTop(8))
			});
		metal1NActiveContact_node.setFunction(NodeProto.Function.CONTACT);
		metal1NActiveContact_node.setSpecialValues(new int [] {PrimitiveNode.MULTICUT, 2, 2, (int)1.5, 3, 0, 0, 0, 0});

		/** metal-1-polysilicon-1-contact */
		metal1Poly1Contact_node = PrimitiveNode.newInstance("Metal-1-Polysilicon-1-Con", this, 5.0, 5.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(metal1_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(0.5), EdgeV.fromBottom(0.5)),
					new Technology.TechPoint(EdgeH.fromRight(0.5), EdgeV.fromTop(0.5))}),
				new Technology.NodeLayer(poly1_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX),
				new Technology.NodeLayer(polyCut_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5)),
					new Technology.TechPoint(EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))})
			});
		metal1Poly1Contact_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metal1Poly1Contact_node, new ArcProto[] {poly1_arc, metal1_arc}, "metal-1-polysilicon-1", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(2), EdgeV.fromBottom(2), EdgeH.fromRight(2), EdgeV.fromTop(2))
			});
		metal1Poly1Contact_node.setFunction(NodeProto.Function.CONTACT);
		metal1Poly1Contact_node.setSpecialValues(new int [] {PrimitiveNode.MULTICUT, 2, 2, (int)1.5, 3, 0, 0, 0, 0});

		/** metal-1-polysilicon-2-contact */
		metal1Poly2Contact_node = PrimitiveNode.newInstance("Metal-1-Polysilicon-2-Con", this, 10.0, 10.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(metal1_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.IN3BOX),
				new Technology.NodeLayer(poly2_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX),
				new Technology.NodeLayer(polyCut_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.IN4BOX)
			});
		metal1Poly2Contact_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metal1Poly2Contact_node, new ArcProto[] {poly2_arc, metal1_arc}, "metal-1-polysilicon-2", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(4.5), EdgeV.fromBottom(4.5), EdgeH.fromRight(4.5), EdgeV.fromTop(4.5))
			});
		metal1Poly2Contact_node.setFunction(NodeProto.Function.CONTACT);
		metal1Poly2Contact_node.setSpecialValues(new int [] {PrimitiveNode.MULTICUT, 2, 2, 4, 3, 0, 0, 0, 0});
		metal1Poly2Contact_node.setNotUsed();

		/** metal-1-polysilicon-1-2-contact */
		metal1Poly12Contact_node = PrimitiveNode.newInstance("Metal-1-Polysilicon-1-2-Con", this, 15.0, 15.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(metal1_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(5.5), EdgeV.fromBottom(5.5)),
					new Technology.TechPoint(EdgeH.fromRight(5.5), EdgeV.fromTop(5.5))}),
				new Technology.NodeLayer(poly1_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.IN5BOX),
				new Technology.NodeLayer(poly2_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX),
				new Technology.NodeLayer(polyCut_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(6.5), EdgeV.fromBottom(6.5)),
					new Technology.TechPoint(EdgeH.fromRight(6.5), EdgeV.fromTop(6.5))})
			});
		metal1Poly12Contact_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metal1Poly12Contact_node, new ArcProto[] {poly1_arc, poly2_arc, metal1_arc}, "metal-1-polysilicon-1-2", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(7), EdgeV.fromBottom(7), EdgeH.fromRight(7), EdgeV.fromTop(7))
			});
		metal1Poly12Contact_node.setFunction(NodeProto.Function.CONTACT);
		metal1Poly12Contact_node.setSpecialValues(new int [] {PrimitiveNode.MULTICUT, 2, 2, (int)6.5, 3, 0, 0, 0, 0});
		metal1Poly12Contact_node.setNotUsed();

		/** P-Transistor */
		pTransistor_node = PrimitiveNode.newInstance("P-Transistor", this, 15.0, 22.0, new SizeOffset(6, 6, 10, 10),
			new Technology.NodeLayer [] {
				new Technology.NodeLayer(pActive_lay, 1, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(6), EdgeV.fromBottom(7)),
					new Technology.TechPoint(EdgeH.fromRight(6), EdgeV.fromTop(7))}, 4, 4, 0, 0),
				new Technology.NodeLayer(transistorPoly_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(4), EdgeV.fromBottom(10)),
					new Technology.TechPoint(EdgeH.fromRight(4), EdgeV.fromTop(10))}, 1, 1, 2, 2),
				new Technology.NodeLayer(nWell_lay, -1, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.LEFTEDGE, EdgeV.fromBottom(1)),
					new Technology.TechPoint(EdgeH.RIGHTEDGE, EdgeV.fromTop(1))}, 10, 10, 6, 6),
				new Technology.NodeLayer(pSelect_lay, -1, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(4), EdgeV.fromBottom(5)),
					new Technology.TechPoint(EdgeH.fromRight(4), EdgeV.fromTop(5))}, 6, 6, 2, 2)
			});
		pTransistor_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, pTransistor_node, new ArcProto[] {poly1_arc}, "p-trans-poly-left", 180,90, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(4), EdgeV.fromBottom(11), EdgeH.fromLeft(4), EdgeV.fromTop(11)),
				PrimitivePort.newInstance(this, pTransistor_node, new ArcProto[] {pActive_arc}, "p-trans-diff-top", 90,90, 1, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(7.5), EdgeV.fromTop(7.5), EdgeH.fromRight(7.5), EdgeV.fromTop(7)),
				PrimitivePort.newInstance(this, pTransistor_node, new ArcProto[] {poly1_arc}, "p-trans-poly-right", 0,90, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromRight(4), EdgeV.fromBottom(11), EdgeH.fromRight(4), EdgeV.fromTop(11)),
				PrimitivePort.newInstance(this, pTransistor_node, new ArcProto[] {pActive_arc}, "p-trans-diff-bottom", 270,90, 2, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(7.5), EdgeV.fromBottom(7), EdgeH.fromRight(7.5), EdgeV.fromBottom(7.5))
			});
		pTransistor_node.setFunction(NodeProto.Function.TRAPMOS);
		pTransistor_node.setHoldsOutline();
		pTransistor_node.setCanShrink();
		pTransistor_node.setSpecialValues(new int [] {PrimitiveNode.SERPTRANS, 7, (int)1.5, (int)2.5, 2, 1, 2, 0, 0});

		/** N-Transistor */
		nTransistor_node = PrimitiveNode.newInstance("N-Transistor", this, 15.0, 22.0, new SizeOffset(6, 6, 10, 10),
			new Technology.NodeLayer [] {
				new Technology.NodeLayer(nActive_lay, 1, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(6), EdgeV.fromBottom(7)),
					new Technology.TechPoint(EdgeH.fromRight(6), EdgeV.fromTop(7))}, 4, 4, 0, 0),
				new Technology.NodeLayer(transistorPoly_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(4), EdgeV.fromBottom(10)),
					new Technology.TechPoint(EdgeH.fromRight(4), EdgeV.fromTop(10))}, 1, 1, 2, 2),
				new Technology.NodeLayer(pWell_lay, -1, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.LEFTEDGE, EdgeV.fromBottom(1)),
					new Technology.TechPoint(EdgeH.RIGHTEDGE, EdgeV.fromTop(1))}, 10, 10, 6, 6),
				new Technology.NodeLayer(nSelect_lay, -1, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(4), EdgeV.fromBottom(5)),
					new Technology.TechPoint(EdgeH.fromRight(4), EdgeV.fromTop(5))}, 6, 6, 2, 2)
			});
		nTransistor_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, nTransistor_node, new ArcProto[] {poly1_arc}, "n-trans-poly-left", 180,90, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(4), EdgeV.fromBottom(11), EdgeH.fromLeft(4), EdgeV.fromTop(11)),
				PrimitivePort.newInstance(this, nTransistor_node, new ArcProto[] {nActive_arc}, "n-trans-diff-top", 90,90, 1, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(7.5), EdgeV.fromTop(7.5), EdgeH.fromRight(7.5), EdgeV.fromTop(7)),
				PrimitivePort.newInstance(this, nTransistor_node, new ArcProto[] {poly1_arc}, "n-trans-poly-right", 0,90, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromRight(4), EdgeV.fromBottom(11), EdgeH.fromRight(4), EdgeV.fromTop(11)),
				PrimitivePort.newInstance(this, nTransistor_node, new ArcProto[] {nActive_arc}, "n-trans-diff-bottom", 270,90, 2, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(7.5), EdgeV.fromBottom(7), EdgeH.fromRight(7.5), EdgeV.fromBottom(7.5))
			});
		nTransistor_node.setFunction(NodeProto.Function.TRANMOS);
		nTransistor_node.setHoldsOutline();
		nTransistor_node.setCanShrink();
		nTransistor_node.setSpecialValues(new int [] {PrimitiveNode.SERPTRANS, 7, (int)1.5, (int)2.5, 2, 1, 2, 0, 0});

		/** Scalable-P-Transistor */
		scalablePTransistor_node = PrimitiveNode.newInstance("P-Transistor-Scalable", this, 17.0, 26.0, new SizeOffset(7, 7, 12, 12),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(pActive_lay, 1, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(6), EdgeV.fromTop(6)),
					new Technology.TechPoint(EdgeH.fromRight(6), EdgeV.fromTop(11))}),
				new Technology.NodeLayer(metal1_lay, 1, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(6.5), EdgeV.fromTop(6.5)),
					new Technology.TechPoint(EdgeH.fromRight(6.5), EdgeV.fromTop(10.5))}),
				new Technology.NodeLayer(pActive_lay, 3, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(6), EdgeV.fromBottom(11)),
					new Technology.TechPoint(EdgeH.fromRight(6), EdgeV.fromBottom(6))}),
				new Technology.NodeLayer(metal1_lay, 3, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(6.5), EdgeV.fromBottom(10.5)),
					new Technology.TechPoint(EdgeH.fromRight(6.5), EdgeV.fromBottom(6.5))}),
				new Technology.NodeLayer(pActive_lay, -1, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(7), EdgeV.fromBottom(9)),
					new Technology.TechPoint(EdgeH.fromRight(7), EdgeV.fromTop(9))}),
				new Technology.NodeLayer(transistorPoly_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(5), EdgeV.fromBottom(12)),
					new Technology.TechPoint(EdgeH.fromRight(5), EdgeV.fromTop(12))}),
				new Technology.NodeLayer(nWell_lay, -1, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX),
				new Technology.NodeLayer(pSelect_lay, -1, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.IN4BOX),
				new Technology.NodeLayer(activeCut_lay, -1, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(7.5), EdgeV.fromBottom(9.5)),
					new Technology.TechPoint(EdgeH.fromLeft(9.5), EdgeV.fromBottom(7.5))}),
				new Technology.NodeLayer(activeCut_lay, -1, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(7.5), EdgeV.fromTop(7.5)),
					new Technology.TechPoint(EdgeH.fromLeft(9.5), EdgeV.fromTop(9.5))})
			});
		scalablePTransistor_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, scalablePTransistor_node, new ArcProto[] {poly1_arc}, "p-trans-sca-poly-left", 180,90, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromCenter(-3.5), EdgeV.CENTER, EdgeH.fromCenter(-3.5), EdgeV.CENTER),
				PrimitivePort.newInstance(this, scalablePTransistor_node, new ArcProto[] {pActive_arc, metal1_arc}, "p-trans-sca-diff-top", 90,90, 1, PortProto.Characteristic.UNKNOWN,
					EdgeH.CENTER, EdgeV.fromCenter(4.5), EdgeH.CENTER, EdgeV.fromCenter(4.5)),
				PrimitivePort.newInstance(this, scalablePTransistor_node, new ArcProto[] {poly1_arc}, "p-trans-sca-poly-right", 0,90, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromCenter(3.5), EdgeV.CENTER, EdgeH.fromCenter(3.5), EdgeV.CENTER),
				PrimitivePort.newInstance(this, scalablePTransistor_node, new ArcProto[] {pActive_arc, metal1_arc}, "p-trans-sca-diff-bottom", 270,90, 2, PortProto.Characteristic.UNKNOWN,
					EdgeH.CENTER, EdgeV.fromCenter(-4.5), EdgeH.CENTER, EdgeV.fromCenter(-4.5))
			});
		scalablePTransistor_node.setFunction(NodeProto.Function.TRAPMOS);
		scalablePTransistor_node.setCanShrink();
		scalablePTransistor_node.setNotUsed();

		/** Scalable-N-Transistor */
		scalableNTransistor_node = PrimitiveNode.newInstance("N-Transistor-Scalable", this, 17.0, 26.0, new SizeOffset(7, 7, 12, 12),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(nActive_lay, 1, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(6), EdgeV.fromTop(6)),
					new Technology.TechPoint(EdgeH.fromRight(6), EdgeV.fromTop(11))}),
				new Technology.NodeLayer(metal1_lay, 1, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(6.5), EdgeV.fromTop(6.5)),
					new Technology.TechPoint(EdgeH.fromRight(6.5), EdgeV.fromTop(10.5))}),
				new Technology.NodeLayer(nActive_lay, 3, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(6), EdgeV.fromBottom(11)),
					new Technology.TechPoint(EdgeH.fromRight(6), EdgeV.fromBottom(6))}),
				new Technology.NodeLayer(metal1_lay, 3, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(6.5), EdgeV.fromBottom(10.5)),
					new Technology.TechPoint(EdgeH.fromRight(6.5), EdgeV.fromBottom(6.5))}),
				new Technology.NodeLayer(nActive_lay, -1, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(7), EdgeV.fromBottom(9)),
					new Technology.TechPoint(EdgeH.fromRight(7), EdgeV.fromTop(9))}),
				new Technology.NodeLayer(transistorPoly_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(5), EdgeV.fromBottom(12)),
					new Technology.TechPoint(EdgeH.fromRight(5), EdgeV.fromTop(12))}),
				new Technology.NodeLayer(pWell_lay, -1, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX),
				new Technology.NodeLayer(nSelect_lay, -1, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.IN4BOX),
				new Technology.NodeLayer(activeCut_lay, -1, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(7.5), EdgeV.fromBottom(9.5)),
					new Technology.TechPoint(EdgeH.fromLeft(9.5), EdgeV.fromBottom(7.5))}),
				new Technology.NodeLayer(activeCut_lay, -1, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(7.5), EdgeV.fromTop(7.5)),
					new Technology.TechPoint(EdgeH.fromLeft(9.5), EdgeV.fromTop(9.5))})
			});
		scalableNTransistor_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, scalableNTransistor_node, new ArcProto[] {poly1_arc}, "n-trans-sca-poly-left", 180,90, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromCenter(-3.5), EdgeV.CENTER, EdgeH.fromCenter(-3.5), EdgeV.CENTER),
				PrimitivePort.newInstance(this, scalableNTransistor_node, new ArcProto[] {nActive_arc, metal1_arc}, "n-trans-sca-diff-top", 90,90, 1, PortProto.Characteristic.UNKNOWN,
					EdgeH.CENTER, EdgeV.fromCenter(4.5), EdgeH.CENTER, EdgeV.fromCenter(4.5)),
				PrimitivePort.newInstance(this, scalableNTransistor_node, new ArcProto[] {poly1_arc}, "n-trans-sca-poly-right", 0,90, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromCenter(3.5), EdgeV.CENTER, EdgeH.fromCenter(3.5), EdgeV.CENTER),
				PrimitivePort.newInstance(this, scalableNTransistor_node, new ArcProto[] {nActive_arc, metal1_arc}, "n-trans-sca-diff-bottom", 270,90, 2, PortProto.Characteristic.UNKNOWN,
					EdgeH.CENTER, EdgeV.fromCenter(-4.5), EdgeH.CENTER, EdgeV.fromCenter(-4.5))
			});
		scalableNTransistor_node.setFunction(NodeProto.Function.TRANMOS);
		scalableNTransistor_node.setCanShrink();
		scalableNTransistor_node.setNotUsed();

		/** metal-1-metal-2-contact */
		metal1Metal2Contact_node = PrimitiveNode.newInstance("Metal-1-Metal-2-Con", this, 5.0, 5.0, new SizeOffset(0.5, 0.5, 0.5, 0.5),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(metal1_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.IN0HBOX),
				new Technology.NodeLayer(metal2_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.IN0HBOX),
				new Technology.NodeLayer(via1_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.IN1HBOX)
			});
		metal1Metal2Contact_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metal1Metal2Contact_node, new ArcProto[] {metal1_arc, metal2_arc}, "metal-1-metal-2", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
			});
		metal1Metal2Contact_node.setFunction(NodeProto.Function.CONTACT);
		metal1Metal2Contact_node.setSpecialValues(new int [] {PrimitiveNode.MULTICUT, 2, 2, 1, 3, 0, 0, 0, 0});

		/** metal-2-metal-3-contact */
		metal2Metal3Contact_node = PrimitiveNode.newInstance("Metal-2-Metal-3-Con", this, 6.0, 6.0, new SizeOffset(1, 1, 1, 1),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(metal2_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.IN1BOX),
				new Technology.NodeLayer(metal3_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.IN1BOX),
				new Technology.NodeLayer(via2_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.IN2BOX)
			});
		metal2Metal3Contact_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metal2Metal3Contact_node, new ArcProto[] {metal2_arc, metal3_arc}, "metal-2-metal-3", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(2.5), EdgeV.fromBottom(2.5), EdgeH.fromRight(2.5), EdgeV.fromTop(2.5))
			});
		metal2Metal3Contact_node.setFunction(NodeProto.Function.CONTACT);
		metal2Metal3Contact_node.setSpecialValues(new int [] {PrimitiveNode.MULTICUT, 2, 2, 1, 3, 0, 0, 0, 0});

		/** metal-3-metal-4-contact */
		metal3Metal4Contact_node = PrimitiveNode.newInstance("Metal-3-Metal-4-Con", this, 6.0, 6.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(metal3_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.IN1BOX),
				new Technology.NodeLayer(metal4_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX),
				new Technology.NodeLayer(via3_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.IN2BOX)
			});
		metal3Metal4Contact_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metal3Metal4Contact_node, new ArcProto[] {metal3_arc, metal4_arc}, "metal-3-metal-4", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(2.5), EdgeV.fromBottom(2.5), EdgeH.fromRight(2.5), EdgeV.fromTop(2.5))
			});
		metal3Metal4Contact_node.setFunction(NodeProto.Function.CONTACT);
		metal3Metal4Contact_node.setSpecialValues(new int [] {PrimitiveNode.MULTICUT, 2, 2, 2, 3, 0, 0, 0, 0});

		/** metal-4-metal-5-contact */
		metal4Metal5Contact_node = PrimitiveNode.newInstance("Metal-4-Metal-5-Con", this, 7.0, 7.0, new SizeOffset(1.5, 1.5, 1.5, 1.5),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(metal4_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.IN1HBOX),
				new Technology.NodeLayer(metal5_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.IN1HBOX),
				new Technology.NodeLayer(via4_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.IN2HBOX)
			});
		metal4Metal5Contact_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metal4Metal5Contact_node, new ArcProto[] {metal4_arc, metal5_arc}, "metal-4-metal-5", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(2.5), EdgeV.fromBottom(2.5), EdgeH.fromRight(2.5), EdgeV.fromTop(2.5))
			});
		metal4Metal5Contact_node.setFunction(NodeProto.Function.CONTACT);
		metal4Metal5Contact_node.setSpecialValues(new int [] {PrimitiveNode.MULTICUT, 2, 2, 1, 3, 0, 0, 0, 0});
		metal4Metal5Contact_node.setNotUsed();

		/** metal-5-metal-6-contact */
		metal5Metal6Contact_node = PrimitiveNode.newInstance("Metal-5-Metal-6-Con", this, 8.0, 8.0, new SizeOffset(1, 1, 1, 1),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(metal5_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.IN1BOX),
				new Technology.NodeLayer(metal6_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.IN1BOX),
				new Technology.NodeLayer(via5_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.IN3BOX)
			});
		metal5Metal6Contact_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metal5Metal6Contact_node, new ArcProto[] {metal5_arc, metal6_arc}, "metal-5-metal-6", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(2.5), EdgeV.fromBottom(2.5), EdgeH.fromRight(2.5), EdgeV.fromTop(2.5))
			});
		metal5Metal6Contact_node.setFunction(NodeProto.Function.CONTACT);
		metal5Metal6Contact_node.setSpecialValues(new int [] {PrimitiveNode.MULTICUT, 3, 3, 2, 4, 0, 0, 0, 0});
		metal5Metal6Contact_node.setNotUsed();

		/** Metal-1-P-Well Contact */
		metal1PWellContact_node = PrimitiveNode.newInstance("Metal-1-P-Well-Con", this, 17.0, 17.0, new SizeOffset(6, 6, 6, 6),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(metal1_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.IN6HBOX),
				new Technology.NodeLayer(pActiveWell_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.IN6BOX),
				new Technology.NodeLayer(pWell_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX),
				new Technology.NodeLayer(pSelect_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.IN4BOX),
				new Technology.NodeLayer(activeCut_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.IN7HBOX)
			});
		metal1PWellContact_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metal1PWellContact_node, new ArcProto[] {metal1_arc, active_arc}, "metal-1-well", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(8), EdgeV.fromBottom(8), EdgeH.fromRight(8), EdgeV.fromTop(8))
			});
		metal1PWellContact_node.setFunction(NodeProto.Function.WELL);
		metal1PWellContact_node.setSpecialValues(new int [] {PrimitiveNode.MULTICUT, 2, 2, (int)1.5, 3, 0, 0, 0, 0});

		/** Metal-1-N-Well Contact */
		metal1NWellContact_node = PrimitiveNode.newInstance("Metal-1-N-Well-Con", this, 17.0, 17.0, new SizeOffset(6, 6, 6, 6),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(metal1_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.IN6HBOX),
				new Technology.NodeLayer(nActive_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.IN6BOX),
				new Technology.NodeLayer(nWell_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX),
				new Technology.NodeLayer(nSelect_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.IN4BOX),
				new Technology.NodeLayer(activeCut_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.IN7HBOX)
			});
		metal1NWellContact_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metal1NWellContact_node, new ArcProto[] {metal1_arc, active_arc}, "metal-1-substrate", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(8), EdgeV.fromBottom(8), EdgeH.fromRight(8), EdgeV.fromTop(8))
			});
		metal1NWellContact_node.setFunction(NodeProto.Function.SUBSTRATE);
		metal1NWellContact_node.setSpecialValues(new int [] {PrimitiveNode.MULTICUT, 2, 2, (int)1.5, 3, 0, 0, 0, 0});

		/** Metal-1-Node */
		metal1Node_node = PrimitiveNode.newInstance("Metal-1-Node", this, 3.0, 3.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(metal1_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX)
			});
		metal1Node_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metal1Node_node, new ArcProto[] {metal1_arc}, "metal-1", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
			});
		metal1Node_node.setFunction(NodeProto.Function.NODE);
		metal1Node_node.setHoldsOutline();
		metal1Node_node.setSpecialValues(new int [] {PrimitiveNode.POLYGONAL, 0, 0, 0, 0, 0, 0, 0, 0});

		/** Metal-2-Node */
		metal2Node_node = PrimitiveNode.newInstance("Metal-2-Node", this, 3.0, 3.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(metal2_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX)
			});
		metal2Node_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metal2Node_node, new ArcProto[] {metal2_arc}, "metal-2", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
			});
		metal2Node_node.setFunction(NodeProto.Function.NODE);
		metal2Node_node.setHoldsOutline();
		metal2Node_node.setSpecialValues(new int [] {PrimitiveNode.POLYGONAL, 0, 0, 0, 0, 0, 0, 0, 0});

		/** Metal-3-Node */
		metal3Node_node = PrimitiveNode.newInstance("Metal-3-Node", this, 3.0, 3.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(metal3_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX)
			});
		metal3Node_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metal3Node_node, new ArcProto[] {metal3_arc}, "metal-3", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
			});
		metal3Node_node.setFunction(NodeProto.Function.NODE);
		metal3Node_node.setHoldsOutline();
		metal3Node_node.setSpecialValues(new int [] {PrimitiveNode.POLYGONAL, 0, 0, 0, 0, 0, 0, 0, 0});

		/** Metal-4-Node */
		metal4Node_node = PrimitiveNode.newInstance("Metal-4-Node", this, 3.0, 3.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(metal4_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX)
			});
		metal4Node_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metal4Node_node, new ArcProto[] {metal4_arc}, "metal-4", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
			});
		metal4Node_node.setFunction(NodeProto.Function.NODE);
		metal4Node_node.setHoldsOutline();
		metal4Node_node.setSpecialValues(new int [] {PrimitiveNode.POLYGONAL, 0, 0, 0, 0, 0, 0, 0, 0});

		/** Metal-5-Node */
		metal5Node_node = PrimitiveNode.newInstance("Metal-5-Node", this, 3.0, 3.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(metal5_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX)
			});
		metal5Node_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metal5Node_node, new ArcProto[] {metal5_arc}, "metal-5", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
			});
		metal5Node_node.setFunction(NodeProto.Function.NODE);
		metal5Node_node.setHoldsOutline();
		metal5Node_node.setSpecialValues(new int [] {PrimitiveNode.POLYGONAL, 0, 0, 0, 0, 0, 0, 0, 0});
		metal5Node_node.setNotUsed();

		/** Metal-6-Node */
		metal6Node_node = PrimitiveNode.newInstance("Metal-6-Node", this, 3.0, 3.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(metal6_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX)
			});
		metal6Node_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metal6Node_node, new ArcProto[] {metal6_arc}, "metal-6", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
			});
		metal6Node_node.setFunction(NodeProto.Function.NODE);
		metal6Node_node.setHoldsOutline();
		metal6Node_node.setSpecialValues(new int [] {PrimitiveNode.POLYGONAL, 0, 0, 0, 0, 0, 0, 0, 0});
		metal6Node_node.setNotUsed();

		/** Polysilicon-1-Node */
		poly1Node_node = PrimitiveNode.newInstance("Polysilicon-1-Node", this, 2.0, 2.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(poly1_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX)
			});
		poly1Node_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, poly1Node_node, new ArcProto[] {poly1_arc}, "polysilicon-1", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(1), EdgeV.fromBottom(1), EdgeH.fromRight(1), EdgeV.fromTop(1))
			});
		poly1Node_node.setFunction(NodeProto.Function.NODE);
		poly1Node_node.setHoldsOutline();
		poly1Node_node.setSpecialValues(new int [] {PrimitiveNode.POLYGONAL, 0, 0, 0, 0, 0, 0, 0, 0});

		/** Polysilicon-2-Node */
		poly2Node_node = PrimitiveNode.newInstance("Polysilicon-2-Node", this, 3.0, 3.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(poly2_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX)
			});
		poly2Node_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, poly2Node_node, new ArcProto[] {poly2_arc}, "polysilicon-2", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
			});
		poly2Node_node.setFunction(NodeProto.Function.NODE);
		poly2Node_node.setHoldsOutline();
		poly2Node_node.setSpecialValues(new int [] {PrimitiveNode.POLYGONAL, 0, 0, 0, 0, 0, 0, 0, 0});
		poly2Node_node.setNotUsed();

		/** P-Active-Node */
		pActiveNode_node = PrimitiveNode.newInstance("P-Active-Node", this, 3.0, 3.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(pActive_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX)
			});
		pActiveNode_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, pActiveNode_node, new ArcProto[] {active_arc, pActive_arc, nActive_arc}, "active", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
			});
		pActiveNode_node.setFunction(NodeProto.Function.NODE);
		pActiveNode_node.setHoldsOutline();
		pActiveNode_node.setSpecialValues(new int [] {PrimitiveNode.POLYGONAL, 0, 0, 0, 0, 0, 0, 0, 0});

		/** N-Active-Node */
		nActiveNode_node = PrimitiveNode.newInstance("N-Active-Node", this, 3.0, 3.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(nActive_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX)
			});
		nActiveNode_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, nActiveNode_node, new ArcProto[] {active_arc, pActive_arc, nActive_arc}, "active", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
			});
		nActiveNode_node.setFunction(NodeProto.Function.NODE);
		nActiveNode_node.setHoldsOutline();
		nActiveNode_node.setSpecialValues(new int [] {PrimitiveNode.POLYGONAL, 0, 0, 0, 0, 0, 0, 0, 0});

		/** P-Select-Node */
		pSelectNode_node = PrimitiveNode.newInstance("P-Select-Node", this, 4.0, 4.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(pSelect_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX)
			});
		pSelectNode_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, pSelectNode_node, new ArcProto[0], "select", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.LEFTEDGE, EdgeV.BOTTOMEDGE, EdgeH.RIGHTEDGE, EdgeV.TOPEDGE)
			});
		pSelectNode_node.setFunction(NodeProto.Function.NODE);
		pSelectNode_node.setHoldsOutline();
		pSelectNode_node.setSpecialValues(new int [] {PrimitiveNode.POLYGONAL, 0, 0, 0, 0, 0, 0, 0, 0});

		/** N-Select-Node */
		nSelectNode_node = PrimitiveNode.newInstance("N-Select-Node", this, 4.0, 4.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(nSelect_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX)
			});
		nSelectNode_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, nSelectNode_node, new ArcProto[0], "select", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.LEFTEDGE, EdgeV.BOTTOMEDGE, EdgeH.RIGHTEDGE, EdgeV.TOPEDGE)
			});
		nSelectNode_node.setFunction(NodeProto.Function.NODE);
		nSelectNode_node.setHoldsOutline();
		nSelectNode_node.setSpecialValues(new int [] {PrimitiveNode.POLYGONAL, 0, 0, 0, 0, 0, 0, 0, 0});

		/** PolyCut-Node */
		polyCutNode_node = PrimitiveNode.newInstance("Poly-Cut-Node", this, 2.0, 2.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(polyCut_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX)
			});
		polyCutNode_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, polyCutNode_node, new ArcProto[0], "polycut", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.LEFTEDGE, EdgeV.BOTTOMEDGE, EdgeH.RIGHTEDGE, EdgeV.TOPEDGE)
			});
		polyCutNode_node.setFunction(NodeProto.Function.NODE);
		polyCutNode_node.setHoldsOutline();
		polyCutNode_node.setSpecialValues(new int [] {PrimitiveNode.POLYGONAL, 0, 0, 0, 0, 0, 0, 0, 0});

		/** ActiveCut-Node */
		activeCutNode_node = PrimitiveNode.newInstance("Active-Cut-Node", this, 2.0, 2.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(activeCut_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX)
			});
		activeCutNode_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, activeCutNode_node, new ArcProto[0], "activecut", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.LEFTEDGE, EdgeV.BOTTOMEDGE, EdgeH.RIGHTEDGE, EdgeV.TOPEDGE)
			});
		activeCutNode_node.setFunction(NodeProto.Function.NODE);
		activeCutNode_node.setHoldsOutline();
		activeCutNode_node.setSpecialValues(new int [] {PrimitiveNode.POLYGONAL, 0, 0, 0, 0, 0, 0, 0, 0});

		/** Via-1-Node */
		via1Node_node = PrimitiveNode.newInstance("Via-1-Node", this, 2.0, 2.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(via1_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX)
			});
		via1Node_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, via1Node_node, new ArcProto[0], "via-1", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.LEFTEDGE, EdgeV.BOTTOMEDGE, EdgeH.RIGHTEDGE, EdgeV.TOPEDGE)
			});
		via1Node_node.setFunction(NodeProto.Function.NODE);
		via1Node_node.setHoldsOutline();
		via1Node_node.setSpecialValues(new int [] {PrimitiveNode.POLYGONAL, 0, 0, 0, 0, 0, 0, 0, 0});

		/** Via-2-Node */
		via2Node_node = PrimitiveNode.newInstance("Via-2-Node", this, 2.0, 2.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(via2_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX)
			});
		via2Node_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, via2Node_node, new ArcProto[0], "via-2", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.LEFTEDGE, EdgeV.BOTTOMEDGE, EdgeH.RIGHTEDGE, EdgeV.TOPEDGE)
			});
		via2Node_node.setFunction(NodeProto.Function.NODE);
		via2Node_node.setHoldsOutline();
		via2Node_node.setSpecialValues(new int [] {PrimitiveNode.POLYGONAL, 0, 0, 0, 0, 0, 0, 0, 0});

		/** Via-3-Node */
		via3Node_node = PrimitiveNode.newInstance("Via-3-Node", this, 2.0, 2.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(via3_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX)
			});
		via3Node_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, via3Node_node, new ArcProto[0], "via-3", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.LEFTEDGE, EdgeV.BOTTOMEDGE, EdgeH.RIGHTEDGE, EdgeV.TOPEDGE)
			});
		via3Node_node.setFunction(NodeProto.Function.NODE);
		via3Node_node.setHoldsOutline();
		via3Node_node.setSpecialValues(new int [] {PrimitiveNode.POLYGONAL, 0, 0, 0, 0, 0, 0, 0, 0});

		/** Via-4-Node */
		via4Node_node = PrimitiveNode.newInstance("Via-4-Node", this, 2.0, 2.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(via4_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX)
			});
		via4Node_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, via4Node_node, new ArcProto[0], "via-4", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.LEFTEDGE, EdgeV.BOTTOMEDGE, EdgeH.RIGHTEDGE, EdgeV.TOPEDGE)
			});
		via4Node_node.setFunction(NodeProto.Function.NODE);
		via4Node_node.setHoldsOutline();
		via4Node_node.setSpecialValues(new int [] {PrimitiveNode.POLYGONAL, 0, 0, 0, 0, 0, 0, 0, 0});
		via4Node_node.setNotUsed();

		/** Via-5-Node */
		via5Node_node = PrimitiveNode.newInstance("Via-5-Node", this, 2.0, 2.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(via5_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX)
			});
		via5Node_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, via5Node_node, new ArcProto[0], "via-5", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.LEFTEDGE, EdgeV.BOTTOMEDGE, EdgeH.RIGHTEDGE, EdgeV.TOPEDGE)
			});
		via5Node_node.setFunction(NodeProto.Function.NODE);
		via5Node_node.setHoldsOutline();
		via5Node_node.setSpecialValues(new int [] {PrimitiveNode.POLYGONAL, 0, 0, 0, 0, 0, 0, 0, 0});
		via5Node_node.setNotUsed();

		/** P-Well-Node */
		pWellNode_node = PrimitiveNode.newInstance("P-Well-Node", this, 12.0, 12.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(pWell_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX)
			});
		pWellNode_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, pWellNode_node, new ArcProto[] {pActive_arc}, "well", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(3), EdgeV.fromBottom(3), EdgeH.fromRight(3), EdgeV.fromTop(3))
			});
		pWellNode_node.setFunction(NodeProto.Function.NODE);
		pWellNode_node.setHoldsOutline();
		pWellNode_node.setSpecialValues(new int [] {PrimitiveNode.POLYGONAL, 0, 0, 0, 0, 0, 0, 0, 0});

		/** N-Well-Node */
		nWellNode_node = PrimitiveNode.newInstance("N-Well-Node", this, 12.0, 12.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(nWell_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX)
			});
		nWellNode_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, nWellNode_node, new ArcProto[] {pActive_arc}, "well", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(3), EdgeV.fromBottom(3), EdgeH.fromRight(3), EdgeV.fromTop(3))
			});
		nWellNode_node.setFunction(NodeProto.Function.NODE);
		nWellNode_node.setHoldsOutline();
		nWellNode_node.setSpecialValues(new int [] {PrimitiveNode.POLYGONAL, 0, 0, 0, 0, 0, 0, 0, 0});

		/** Passivation-Node */
		passivationNode_node = PrimitiveNode.newInstance("Passivation-Node", this, 8.0, 8.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(nWell_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX)
			});
		passivationNode_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, passivationNode_node, new ArcProto[0], "passivation", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.LEFTEDGE, EdgeV.BOTTOMEDGE, EdgeH.RIGHTEDGE, EdgeV.TOPEDGE)
			});
		passivationNode_node.setFunction(NodeProto.Function.NODE);
		passivationNode_node.setHoldsOutline();
		passivationNode_node.setSpecialValues(new int [] {PrimitiveNode.POLYGONAL, 0, 0, 0, 0, 0, 0, 0, 0});

		/** Pad-Frame-Node */
		padFrameNode_node = PrimitiveNode.newInstance("Pad-Frame-Node", this, 8.0, 8.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(padFrame_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX)
			});
		padFrameNode_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, padFrameNode_node, new ArcProto[0], "pad-frame", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.LEFTEDGE, EdgeV.BOTTOMEDGE, EdgeH.RIGHTEDGE, EdgeV.TOPEDGE)
			});
		padFrameNode_node.setFunction(NodeProto.Function.NODE);
		padFrameNode_node.setHoldsOutline();
		padFrameNode_node.setSpecialValues(new int [] {PrimitiveNode.POLYGONAL, 0, 0, 0, 0, 0, 0, 0, 0});

		/** Poly-Cap-Node */
		polyCapNode_node = PrimitiveNode.newInstance("Poly-Cap-Node", this, 8.0, 8.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(polyCap_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX)
			});
		polyCapNode_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, polyCapNode_node, new ArcProto[0], "poly-cap", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.LEFTEDGE, EdgeV.BOTTOMEDGE, EdgeH.RIGHTEDGE, EdgeV.TOPEDGE)
			});
		polyCapNode_node.setFunction(NodeProto.Function.NODE);
		polyCapNode_node.setHoldsOutline();
		polyCapNode_node.setSpecialValues(new int [] {PrimitiveNode.POLYGONAL, 0, 0, 0, 0, 0, 0, 0, 0});

		/** P-Active-Well-Node */
		pActiveWellNode_node = PrimitiveNode.newInstance("P-Active-Well-Node", this, 8.0, 8.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(pActiveWell_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX)
			});
		pActiveWellNode_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, pActiveWellNode_node, new ArcProto[0], "p-active-well", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.LEFTEDGE, EdgeV.BOTTOMEDGE, EdgeH.RIGHTEDGE, EdgeV.TOPEDGE)
			});
		pActiveWellNode_node.setFunction(NodeProto.Function.NODE);
		pActiveWellNode_node.setHoldsOutline();
		pActiveWellNode_node.setSpecialValues(new int [] {PrimitiveNode.POLYGONAL, 0, 0, 0, 0, 0, 0, 0, 0});

		/** Polysilicon-1-Transistor-Node */
		polyTransistorNode_node = PrimitiveNode.newInstance("Transistor-Poly-Node", this, 2.0, 2.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(transistorPoly_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX)
			});
		polyTransistorNode_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, polyTransistorNode_node, new ArcProto[] {poly1_arc}, "trans-poly-1", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(1), EdgeV.fromBottom(1), EdgeH.fromRight(1), EdgeV.fromTop(1))
			});
		polyTransistorNode_node.setFunction(NodeProto.Function.NODE);
		polyTransistorNode_node.setHoldsOutline();
		polyTransistorNode_node.setSpecialValues(new int [] {PrimitiveNode.POLYGONAL, 0, 0, 0, 0, 0, 0, 0, 0});

		/** Silicide-Block-Node */
		silicideBlockNode_node = PrimitiveNode.newInstance("Silicide-Block-Node", this, 2.0, 2.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(transistorPoly_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX)
			});
		silicideBlockNode_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, silicideBlockNode_node, new ArcProto[] {poly1_arc}, "silicide-block", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.LEFTEDGE, EdgeV.BOTTOMEDGE, EdgeH.RIGHTEDGE, EdgeV.TOPEDGE)
			});
		silicideBlockNode_node.setFunction(NodeProto.Function.NODE);
		silicideBlockNode_node.setHoldsOutline();
		silicideBlockNode_node.setSpecialValues(new int [] {PrimitiveNode.POLYGONAL, 0, 0, 0, 0, 0, 0, 0, 0});
	}

	/**
	 * Routine to convert old primitive names to their proper NodeProtos.
	 * @param name the name of the old primitive.
	 * @return the proper PrimitiveNode to use (or null if none can be determined).
	 */
	public PrimitiveNode convertOldNodeName(String name)
	{
		if (name.equals("Metal-1-Substrate-Con")) return(metal1NWellContact_node);
		if (name.equals("Metal-1-Well-Con")) return(metal1PWellContact_node);
		return null;
	}
    
    /**
     * Routine to return a gate PortInst for this transistor NodeInst.
     * Implementation Note: May want to make this a more general
     * method, getPrimitivePort(PortType), if the number of port
     * types increases.  Note: You should be calling 
     * NodeInst.getTransistorGatePort() instead of this, most likely.
     * @param ni the NodeInst
     * @return a PortInst for the gate of the transistor
     */
    public PortInst getTransistorGatePort(NodeInst ni) 
    {
		PrimitiveNode np = (PrimitiveNode)ni.getProto();
        if (np == pTransistor_node) return ni.findPortInst("p-trans-poly-left");
        if (np == nTransistor_node) return ni.findPortInst("n-trans-poly-left");
        if (np == scalablePTransistor_node) return ni.findPortInst("p-trans-sca-poly-left");
        if (np == scalableNTransistor_node) return ni.findPortInst("n-trans-sca-poly-left");
        return null;
    }
    
    /**
     * Routine to return a gate PortInst for this transistor NodeInst.
     * Implementation Note: May want to make this a more general
     * method, getPrimitivePort(PortType), if the number of port
     * types increases.  Note: You should be calling 
     * NodeInst.getTransistorSourcePort() instead of this, most likely.
     * @param ni the NodeInst
     * @return a PortInst for the gate of the transistor
     */
    public PortInst getTransistorSourcePort(NodeInst ni)
    {
		PrimitiveNode np = (PrimitiveNode)ni.getProto();
        if (np == pTransistor_node) return ni.findPortInst("p-trans-diff-top");
        if (np == nTransistor_node) return ni.findPortInst("n-trans-diff-top");
        if (np == scalablePTransistor_node) return ni.findPortInst("p-trans-sca-diff-top");
        if (np == scalableNTransistor_node) return ni.findPortInst("n-trans-sca-diff-top");
        return null;
    }

    /**
     * Routine to return a gate PortInst for this transistor NodeInst.
     * Implementation Note: May want to make this a more general
     * method, getPrimitivePort(PortType), if the number of port
     * types increases.  Note: You should be calling 
     * NodeInst.getTransistorDrainPort() instead of this, most likely.
     * @param ni the NodeInst
     * @return a PortInst for the gate of the transistor
     */
    public PortInst getTransistorDrainPort(NodeInst ni)
    {
		PrimitiveNode np = (PrimitiveNode)ni.getProto();
        if (np == pTransistor_node) return ni.findPortInst("p-trans-diff-bottom");
        if (np == nTransistor_node) return ni.findPortInst("n-trans-diff-bottom");
        if (np == scalablePTransistor_node) return ni.findPortInst("p-trans-sca-diff-bottom");
        if (np == scalableNTransistor_node) return ni.findPortInst("n-trans-sca-diff-bottom");
        return null;
    }

/* this tables must correspond with the above table (nodeprotos) */
//static INTBIG node_minsize[NODEPROTOCOUNT*2] = {
//	XX,XX, XX,XX, XX,XX,							/* metal 1/2/3 pin */
//	XX,XX, XX,XX, XX,XX,							/* metal 4/5/6 pin */
//	XX,XX, XX,XX,									/* polysilicon 1/2 pin */
//	XX,XX, XX,XX,									/* P/N active pin */
//	XX,XX,											/* active pin */
//	K17,K17, K17,K17,								/* metal 1 to P/N active contact */
//	K5,K5, K10,K10,									/* metal 1 to polysilicon 1/2 contact */
//	K15,K15,										/* poly capacitor */
//	K15,K22, K15,K22,								/* P/N transistor */
//	K17,K26, K17,K26,								/* scalable P/N transistor */
//	K5,K5, K6,K6, K6,K6,							/* via 1/2/3 */
//	K7,K7, K8,K8,									/* via 4/5 */
//	K17,K17, K17,K17,								/* p-well / n-well contact */
//	XX,XX, XX,XX, XX,XX,							/* metal 1/2/3 node */
//	XX,XX, XX,XX, XX,XX,							/* metal 4/5/6 node */
//	XX,XX, XX,XX,									/* polysilicon 1/2 node */
//	XX,XX, XX,XX,									/* active N-Active node */
//	XX,XX, XX,XX,									/* P/N select node */
//	XX,XX, XX,XX,									/* poly cut / active cut */
//	XX,XX, XX,XX, XX,XX,							/* via 1/2/3 node */
//	XX,XX, XX,XX,									/* via 4/5 node */
//	XX,XX, XX,XX,									/* P/N well node */
//	XX,XX,											/* overglass node */
//	XX,XX,											/* pad frame node */
//	XX,XX,											/* poly-cap node */
//	XX,XX,											/* p-active-well node */
//	XX,XX,											/* transistor poly node */
//	XX,XX};											/* silicide-block node */

/* this tables must correspond with the above table (nodeprotos) */
//static CHAR *node_minsize_rule[NODEPROTOCOUNT] = {
//	x_(""), x_(""), x_(""),							/* metal 1/2/3 pin */
//	x_(""), x_(""), x_(""),							/* metal 4/5/6 pin */
//	x_(""), x_(""),									/* polysilicon 1/2 pin */
//	x_(""), x_(""),									/* P/N active pin */
//	x_(""),											/* active pin */
//	x_("6.2, 7.3"), x_("6.2, 7.3"),					/* metal 1 to P/N active contact */
//	x_("5.2, 7.3"), x_("???"),						/* metal 1 to polysilicon 1/2 contact */
//	x_("???"),										/* poly capacitor */
//	x_("2.1, 3.1"), x_("2.1, 3.1"),					/* P/N transistor */
//	x_("2.1, 3.1"), x_("2.1, 3.1"),					/* scalable P/N transistor */
//	x_("8.3, 9.3"), x_("14.3, 15.3"), x_("21.3, 22.3"),	/* via 1/2/3 */
//	x_("25.3, 26.3"), x_("29.3, 30.3"),				/* via 4/5 */
//	x_("4.2, 6.2, 7.3"), x_("4.2, 6.2, 7.3"),		/* p-well / n-well contact */
//	x_(""), x_(""), x_(""),							/* metal 1/2/3 node */
//	x_(""), x_(""), x_(""),							/* metal 4/5/6 node */
//	x_(""), x_(""),									/* polysilicon 1/2 node */
//	x_(""), x_(""),									/* active N-Active node */
//	x_(""), x_(""),									/* P/N select node */
//	x_(""), x_(""),									/* poly cut / active cut */
//	x_(""), x_(""), x_(""),							/* via 1/2/3 node */
//	x_(""), x_(""),									/* via 4/5 node */
//	x_(""), x_(""),									/* P/N well node */
//	x_(""),											/* overglass node */
//	x_(""),											/* pad frame node */
//	x_(""),											/* poly-cap node */
//	x_(""),											/* p-active-well node */
//	x_(""),											/* transistor poly node */
//	x_("")};										/* silicide-block node */

/******************** SIMULATION VARIABLES ********************/

/* for SPICE simulation */
//#define MIN_RESIST	50.0f		/* minimum resistance consider */
//#define MIN_CAPAC	 0.04f		/* minimum capacitance consider */
//static float sim_spice_resistance[MAXLAYERS] = {  /* per square micron */
//	0.06f, 0.06f, 0.06f,				/* metal 1/2/3 */
//	0.03f, 0.03f, 0.03f,				/* metal 4/5/6 */
//	2.5f, 50.0f,						/* poly 1/2 */
//	2.5f, 3.0f,							/* P/N active */
//	0.0, 0.0,							/* P/N select */
//	0.0, 0.0,							/* P/N well */
//	2.2f, 2.5f,							/* poly/act cut */
//	1.0f, 0.9f, 0.8f, 0.8f, 0.8f,		/* via 1/2/3/4/5 */
//	0.0,								/* overglass */
//	2.5f,								/* transistor poly */
//	0.0,								/* poly cap */
//	0.0,								/* P active well */
//	0.0, 0.0, 0.0, 0.0, 0.0, 0.0,		/* pseudo metal 1/2/3/4/5/6 */
//	0.0, 0.0,							/* pseudo poly 1/2 */
//	0.0, 0.0,							/* pseudo P/N active */
//	0.0, 0.0,							/* pseudo P/N select */
//	0.0, 0.0,							/* pseudo P/N well */
//	0.0};								/* pad frame */
//static float sim_spice_capacitance[MAXLAYERS] = { /* per square micron */
//	0.07f, 0.04f, 0.04f,				/* metal 1/2/3 */
//	0.04f, 0.04f, 0.04f,				/* metal 4/5/6 */
//	0.09f, 1.0f,						/* poly 1/2 */
//	0.9f, 0.9f,							/* P/N active */
//	0.0, 0.0,							/* P/N select */
//	0.0, 0.0,							/* P/N well */
//	0.0, 0.0,							/* poly/act cut */
//	0.0, 0.0, 0.0, 0.0, 0.0,			/* via 1/2/3/4/5 */
//	0.0,								/* overglass */
//	0.09f,								/* transistor poly */
//	0.0,								/* poly cap */
//	0.0,								/* P active well */
//	0.0, 0.0, 0.0, 0.0, 0.0, 0.0,		/* pseudo metal 1/2/3/4/5/6 */
//	0.0, 0.0,							/* pseudo poly 1/2 */
//	0.0, 0.0,							/* pseudo P/N active */
//	0.0, 0.0,							/* pseudo P/N select */
//	0.0, 0.0,							/* pseudo P/N well */
//	0.0};								/* pad frame */
//static CHAR *sim_spice_header_level1[] = {
//	x_("*CMOS/BULK-NWELL (PRELIMINARY PARAMETERS)"),
//	x_(".OPTIONS NOMOD DEFL=3UM DEFW=3UM DEFAD=70P DEFAS=70P LIMPTS=1000"),
//	x_("+ITL5=0 RELTOL=0.01 ABSTOL=500PA VNTOL=500UV LVLTIM=2"),
//	x_("+LVLCOD=1"),
//	x_(".MODEL N NMOS LEVEL=1"),
//	x_("+KP=60E-6 VTO=0.7 GAMMA=0.3 LAMBDA=0.05 PHI=0.6"),
//	x_("+LD=0.4E-6 TOX=40E-9 CGSO=2.0E-10 CGDO=2.0E-10 CJ=.2MF/M^2"),
//	x_(".MODEL P PMOS LEVEL=1"),
//	x_("+KP=20E-6 VTO=0.7 GAMMA=0.4 LAMBDA=0.05 PHI=0.6"),
//	x_("+LD=0.6E-6 TOX=40E-9 CGSO=3.0E-10 CGDO=3.0E-10 CJ=.2MF/M^2"),
//	x_(".MODEL DIFFCAP D CJO=.2MF/M^2"),
//	NOSTRING};
//static CHAR *sim_spice_header_level2[] = {
//	x_("* MOSIS 3u CMOS PARAMS"),
//	x_(".OPTIONS NOMOD DEFL=2UM DEFW=6UM DEFAD=100P DEFAS=100P"),
//	x_("+LIMPTS=1000 ITL5=0 ABSTOL=500PA VNTOL=500UV"),
//	x_("* Note that ITL5=0 sets ITL5 to infinity"),
//	x_(".MODEL N NMOS LEVEL=2 LD=0.3943U TOX=502E-10"),
//	x_("+NSUB=1.22416E+16 VTO=0.756 KP=4.224E-05 GAMMA=0.9241"),
//	x_("+PHI=0.6 UO=623.661 UEXP=8.328627E-02 UCRIT=54015.0"),
//	x_("+DELTA=5.218409E-03 VMAX=50072.2 XJ=0.4U LAMBDA=2.975321E-02"),
//	x_("+NFS=4.909947E+12 NEFF=1.001E-02 NSS=0.0 TPG=1.0"),
//	x_("+RSH=20.37 CGDO=3.1E-10 CGSO=3.1E-10"),
//	x_("+CJ=3.205E-04 MJ=0.4579 CJSW=4.62E-10 MJSW=0.2955 PB=0.7"),
//	x_(".MODEL P PMOS LEVEL=2 LD=0.2875U TOX=502E-10"),
//	x_("+NSUB=1.715148E+15 VTO=-0.7045 KP=1.686E-05 GAMMA=0.3459"),
//	x_("+PHI=0.6 UO=248.933 UEXP=1.02652 UCRIT=182055.0"),
//	x_("+DELTA=1.0E-06 VMAX=100000.0 XJ=0.4U LAMBDA=1.25919E-02"),
//	x_("+NFS=1.0E+12 NEFF=1.001E-02 NSS=0.0 TPG=-1.0"),
//	x_("+RSH=79.10 CGDO=2.89E-10 CGSO=2.89E-10"),
//	x_("+CJ=1.319E-04 MJ=0.4125 CJSW=3.421E-10 MJSW=0.198 PB=0.66"),
//	x_(".TEMP 25.0"),
//	NOSTRING};


/******************** VARIABLE AGGREGATION ********************/

//TECH_VARIABLES variables[] =
//{
//	/* set general information about the technology */
//	{x_("TECH_layer_names"), (CHAR *)layer_names, 0.0,
//		VSTRING|VDONTSAVE|VISARRAY|(MAXLAYERS<<VLENGTHSH)},
//	{x_("TECH_layer_function"), (CHAR *)layer_function, 0.0,
//		VINTEGER|VDONTSAVE|VISARRAY|(MAXLAYERS<<VLENGTHSH)},
//	{x_("TECH_arc_width_offset"), (CHAR *)arc_widoff, 0.0,
//		VFRACT|VDONTSAVE|VISARRAY|(ARCPROTOCOUNT<<VLENGTHSH)},
//	{x_("TECH_node_width_offset"), (CHAR *)node_widoff, 0.0,
//		VFRACT|VDONTSAVE|VISARRAY|((NODEPROTOCOUNT*4)<<VLENGTHSH)},
//	{x_("TECH_layer_3dthickness"), (CHAR *)3dthick_layers, 0.0,
//		VINTEGER|VDONTSAVE|VISARRAY|(MAXLAYERS<<VLENGTHSH)},
//	{x_("TECH_layer_3dheight"), (CHAR *)3dheight_layers, 0.0,
//		VINTEGER|VDONTSAVE|VISARRAY|(MAXLAYERS<<VLENGTHSH)},
//
//	/* set information for the USER tool */
//	{x_("USER_color_map"), (CHAR *)colmap, 0.0,
//		VCHAR|VDONTSAVE|VISARRAY|((sizeof colmap)<<VLENGTHSH)},
//	{x_("USER_layer_letters"), (CHAR *)layer_letters, 0.0,
//		VSTRING|VDONTSAVE|VISARRAY|(MAXLAYERS<<VLENGTHSH)},
//	{x_("USER_print_colors"), (CHAR *)printcolors_layers, 0.0,
//		VINTEGER|VDONTSAVE|VISARRAY|((MAXLAYERS*5)<<VLENGTHSH)},
//
//	/* set information for the DRC tool */
//	{x_("DRC_min_node_size"), (CHAR *)node_minsize, 0.0,
//		VFRACT|VDONTSAVE|VISARRAY|((NODEPROTOCOUNT*2)<<VLENGTHSH)},
//	{x_("DRC_min_node_size_rule"), (CHAR *)node_minsize_rule, 0.0,
//		VSTRING|VDONTSAVE|VISARRAY|(NODEPROTOCOUNT<<VLENGTHSH)},
//
//	/* set information for the I/O tool */
//	{x_("IO_cif_layer_names"), (CHAR *)cif_layers, 0.0,
//		VSTRING|VDONTSAVE|VISARRAY|(MAXLAYERS<<VLENGTHSH)},
//	{x_("IO_gds_layer_numbers"), (CHAR *)gds_layers, 0.0,
//		VSTRING|VDONTSAVE|VISARRAY|(MAXLAYERS<<VLENGTHSH)},
//	{x_("IO_skill_layer_names"), (CHAR *)skill_layers, 0.0,
//		VSTRING|VDONTSAVE|VISARRAY|(MAXLAYERS<<VLENGTHSH)},
//
//	/* set information for the SIM tool (SPICE) */
//	{x_("SIM_spice_min_resistance"), 0, MIN_RESIST, VFLOAT|VDONTSAVE},
//	{x_("SIM_spice_min_capacitance"), 0, MIN_CAPAC, VFLOAT|VDONTSAVE},
//	{x_("SIM_spice_resistance"), (CHAR *)sim_spice_resistance, 0.0,
//		VFLOAT|VISARRAY|(MAXLAYERS<<VLENGTHSH)|VDONTSAVE},
//	{x_("SIM_spice_capacitance"), (CHAR *)sim_spice_capacitance, 0.0,
//		VFLOAT|VISARRAY|(MAXLAYERS<<VLENGTHSH)|VDONTSAVE},
//	{x_("SIM_spice_header_level1"), (CHAR *)sim_spice_header_level1, 0.0,
//		VSTRING|VDONTSAVE|VISARRAY},
//	{x_("SIM_spice_header_level2"), (CHAR *)sim_spice_header_level2, 0.0,
//		VSTRING|VDONTSAVE|VISARRAY},
//	{NULL, NULL, 0.0, 0}
//};

/******************** TECHNOLOGY INTERFACE ROUTINES ********************/

//BOOLEAN initprocess(TECHNOLOGY *tech, INTBIG pass)
//{
//	/* initialize the technology variable */
//	switch (pass)
//	{
//		case 0:
//			tech = tech;
//			break;
//		case 1:
//			metal1poly2prim = getnodeproto(x_("mocmos:Metal-1-Polysilicon-2-Con"));
//			metal1poly12prim = getnodeproto(x_("mocmos:Metal-1-Polysilicon-1-2-Con"));
//			metal1metal2prim = getnodeproto(x_("mocmos:Metal-1-Metal-2-Con"));
//			metal4metal5prim = getnodeproto(x_("mocmos:Metal-4-Metal-5-Con"));
//			metal5metal6prim = getnodeproto(x_("mocmos:Metal-5-Metal-6-Con"));
//			ptransistorprim = getnodeproto(x_("mocmos:P-Transistor"));
//			ntransistorprim = getnodeproto(x_("mocmos:N-Transistor"));
//			metal1pwellprim = getnodeproto(x_("mocmos:Metal-1-P-Well-Con"));
//			metal1nwellprim = getnodeproto(x_("mocmos:Metal-1-N-Well-Con"));
//			scalablentransprim = getnodeproto(x_("mocmos:N-Transistor-Scalable"));
//			scalableptransprim = getnodeproto(x_("mocmos:P-Transistor-Scalable"));
//			transcontactkey = makekey(x_("transcontacts"));
//			break;
//		case 2:
//			/* load these DRC tables */
//			nextchangequiet();
//			if (setvalkey((INTBIG)tech, VTECHNOLOGY, dr_wide_limitkey,
//				WIDELIMIT, VFRACT|VDONTSAVE) == NOVARIABLE) return(TRUE);
//			state = 0;
//			setstate(MOCMOSSUBMRULES|MOCMOS4METAL);
//			nextchangequiet();
//			setvalkey((INTBIG)tech, VTECHNOLOGY, el_techstate_key, state,
//				VINTEGER|VDONTSAVE);
//			break;
//	}
//	return(FALSE);
//}
//
//static KEYWORD mocmosopt[] =
//{
//	{x_("2-metal-rules"),              0,{NOKEY,NOKEY,NOKEY,NOKEY,NOKEY}},
//	{x_("3-metal-rules"),              0,{NOKEY,NOKEY,NOKEY,NOKEY,NOKEY}},
//	{x_("4-metal-rules"),              0,{NOKEY,NOKEY,NOKEY,NOKEY,NOKEY}},
//	{x_("5-metal-rules"),              0,{NOKEY,NOKEY,NOKEY,NOKEY,NOKEY}},
//	{x_("6-metal-rules"),              0,{NOKEY,NOKEY,NOKEY,NOKEY,NOKEY}},
//	{x_("one-polysilicon"),            0,{NOKEY,NOKEY,NOKEY,NOKEY,NOKEY}},
//	{x_("two-polysilicon"),            0,{NOKEY,NOKEY,NOKEY,NOKEY,NOKEY}},
//	{x_("scmos-rules"),                0,{NOKEY,NOKEY,NOKEY,NOKEY,NOKEY}},
//	{x_("submicron-rules"),            0,{NOKEY,NOKEY,NOKEY,NOKEY,NOKEY}},
//	{x_("deep-rules"),                 0,{NOKEY,NOKEY,NOKEY,NOKEY,NOKEY}},
//	{x_("full-graphics"),              0,{NOKEY,NOKEY,NOKEY,NOKEY,NOKEY}},
//	{x_("alternate-active-poly"),      0,{NOKEY,NOKEY,NOKEY,NOKEY,NOKEY}},
//	{x_("standard-active-poly"),       0,{NOKEY,NOKEY,NOKEY,NOKEY,NOKEY}},
//	{x_("allow-stacked-vias"),         0,{NOKEY,NOKEY,NOKEY,NOKEY,NOKEY}},
//	{x_("disallow-stacked-vias"),      0,{NOKEY,NOKEY,NOKEY,NOKEY,NOKEY}},
//	{x_("stick-display"),              0,{NOKEY,NOKEY,NOKEY,NOKEY,NOKEY}},
//	{x_("switch-n-and-p"),             0,{NOKEY,NOKEY,NOKEY,NOKEY,NOKEY}},
//	{x_("show-scalable-transistors"),  0,{NOKEY,NOKEY,NOKEY,NOKEY,NOKEY}},
//	{x_("hide-scalable-transistors"),  0,{NOKEY,NOKEY,NOKEY,NOKEY,NOKEY}},
//	TERMKEY
//};
//COMCOMP parse = {mocmosopt, NOTOPLIST, NONEXTLIST, NOPARAMS,
//	0, x_(" \t"), M_("MOSIS CMOS Submicron option"), 0};
//
//INTBIG request(CHAR *command, va_list ap)
//{
//	REGISTER INTBIG realstate;
//	static INTBIG equivtable[3] = {poly1_lay,transistorPoly_lay, -1};
//
//	if (namesame(command, x_("has-state")) == 0) return(1);
//	if (namesame(command, x_("get-state")) == 0)
//	{
//		return(state);
//	}
//	if (namesame(command, x_("get-layer-equivalences")) == 0)
//	{
//		return((INTBIG)equivtable);
//	}
//	if (namesame(command, x_("set-state")) == 0)
//	{
//		setstate(va_arg(ap, INTBIG));
//		return(0);
//	}
//	if (namesame(command, x_("describe-state")) == 0)
//	{
//		return((INTBIG)describestate(va_arg(ap, INTBIG)));
//	}
//	if (namesame(command, x_("switch-n-and-p")) == 0)
//	{
//		switchnp();
//		return(0);
//	}
//	if (namesame(command, x_("factory-reset")) == 0)
//	{
//		realstate = state;
//		state++;
//		setstate(realstate);
//		return(0);
//	}
//	return(0);
//}
//
//void setmode(INTBIG count, CHAR *par[])
//{
//	REGISTER INTBIG l;
//	REGISTER CHAR *pp;
//	Q_UNUSED( count );
//
//	l = estrlen(pp = par[0]);
//
//	if (namesamen(pp, x_("full-graphics"), l) == 0)
//	{
//		setstate(state & ~MOCMOSSTICKFIGURE);
//		ttyputverbose(M_("MOSIS CMOS technology displays full graphics"));
//		return;
//	}
//	if (namesamen(pp, x_("stick-display"), l) == 0 && l >= 3)
//	{
//		setstate(state | MOCMOSSTICKFIGURE);
//		ttyputverbose(M_("MOSIS CMOS technology displays stick figures"));
//		return;
//	}
//
//	if (namesamen(pp, x_("alternate-active-poly"), l) == 0 && l >= 3)
//	{
//		setstate(state | MOCMOSALTAPRULES);
//		ttyputverbose(M_("MOSIS CMOS technology uses alternate active/poly rules"));
//		return;
//	}
//	if (namesamen(pp, x_("standard-active-poly"), l) == 0 && l >= 3)
//	{
//		setstate(state & ~MOCMOSALTAPRULES);
//		ttyputverbose(M_("MOSIS CMOS technology uses standard active/poly rules"));
//		return;
//	}
//
//	if (namesamen(pp, x_("disallow-stacked-vias"), l) == 0 && l >= 2)
//	{
//		setstate(state | MOCMOSNOSTACKEDVIAS);
//		ttyputverbose(M_("MOSIS CMOS technology disallows stacked vias"));
//		return;
//	}
//	if (namesamen(pp, x_("allow-stacked-vias"), l) == 0 && l >= 3)
//	{
//		setstate(state & ~MOCMOSNOSTACKEDVIAS);
//		ttyputverbose(M_("MOSIS CMOS technology allows stacked vias"));
//		return;
//	}
//
//	if (namesamen(pp, x_("scmos-rules"), l) == 0 && l >= 2)
//	{
//		setstate((state & ~MOCMOSRULESET) | MOCMOSSCMOSRULES);
//		ttyputverbose(M_("MOSIS CMOS technology uses standard SCMOS rules"));
//		return;
//	}
//	if (namesamen(pp, x_("submicron-rules"), l) == 0 && l >= 2)
//	{
//		setstate((state & ~MOCMOSRULESET) | MOCMOSSUBMRULES);
//		ttyputverbose(M_("MOSIS CMOS technology uses submicron rules"));
//		return;
//	}
//	if (namesamen(pp, x_("deep-rules"), l) == 0 && l >= 2)
//	{
//		setstate((state & ~MOCMOSRULESET) | MOCMOSDEEPRULES);
//		ttyputverbose(M_("MOSIS CMOS technology uses deep submicron rules"));
//		return;
//	}
//
//	if (namesamen(pp, x_("one-polysilicon"), l) == 0)
//	{
//		setstate(state & ~MOCMOSTWOPOLY);
//		ttyputverbose(M_("MOSIS CMOS technology uses 1-polysilicon rules"));
//		return;
//	}
//	if (namesamen(pp, x_("two-polysilicon"), l) == 0)
//	{
//		setstate(state | MOCMOSTWOPOLY);
//		ttyputverbose(M_("MOSIS CMOS technology uses 2-polysilicon rules"));
//		return;
//	}
//
//	if (namesamen(pp, x_("hide-scalable-transistors"), l) == 0)
//	{
//		setstate(state & ~MOCMOSSCALABLETRAN);
//		ttyputverbose(M_("MOSIS CMOS technology excludes scalable transistors"));
//		return;
//	}
//	if (namesamen(pp, x_("show-scalable-transistors"), l) == 0)
//	{
//		setstate(state | MOCMOSSCALABLETRAN);
//		ttyputverbose(M_("MOSIS CMOS technology includes scalable transistors"));
//		return;
//	}
//
//	if (namesamen(pp, x_("2-metal-rules"), l) == 0)
//	{
//		setstate((state & ~MOCMOSMETALS) | MOCMOS2METAL);
//		ttyputverbose(M_("MOSIS CMOS technology uses 2-metal rules"));
//		return;
//	}
//	if (namesamen(pp, x_("3-metal-rules"), l) == 0)
//	{
//		setstate((state & ~MOCMOSMETALS) | MOCMOS3METAL);
//		ttyputverbose(M_("MOSIS CMOS technology uses 3-metal rules"));
//		return;
//	}
//	if (namesamen(pp, x_("4-metal-rules"), l) == 0)
//	{
//		setstate((state & ~MOCMOSMETALS) | MOCMOS4METAL);
//		ttyputverbose(M_("MOSIS CMOS technology uses 4-metal rules"));
//		return;
//	}
//	if (namesamen(pp, x_("5-metal-rules"), l) == 0)
//	{
//		setstate((state & ~MOCMOSMETALS) | MOCMOS5METAL);
//		ttyputverbose(M_("MOSIS CMOS technology uses 5-metal rules"));
//		return;
//	}
//	if (namesamen(pp, x_("6-metal-rules"), l) == 0)
//	{
//		setstate((state & ~MOCMOSMETALS) | MOCMOS6METAL);
//		ttyputverbose(M_("MOSIS CMOS technology uses 6-metal rules"));
//		return;
//	}
//
//	if (namesamen(pp, x_("switch-n-and-p"), l) == 0 && l >= 2)
//	{
//		switchnp();
//	}
//
//	ttyputbadusage(x_("technology tell mocmos"));
//}
//
///******************** NODE DESCRIPTION (GRAPHICAL) ********************/
//
//INTBIG nodepolys(NODEINST *ni, INTBIG *reasonable, WINDOWPART *win)
//{
//	return(intnodepolys(ni, reasonable, win, &tech_oneprocpolyloop, &oneprocpolyloop));
//}
//
//INTBIG intnodepolys(NODEINST *ni, INTBIG *reasonable, WINDOWPART *win, POLYLOOP *pl, MOCPOLYLOOP *mocpl)
//{
//	REGISTER INTBIG pindex, count;
//	TECH_NODES *thistn;
//	REGISTER NODEPROTO *np;
//
//	np = ni->proto;
//	pindex = np->primindex;
//
//	/* non-stick-figures: standard components */
//	if ((state&MOCMOSSTICKFIGURE) == 0)
//	{
//		if (pindex == NSTRANSP || pindex == NSTRANSN)
//			return(initializescalabletransistor(ni, reasonable, win, pl, mocpl));
//		return(tech_nodepolys(ni, reasonable, win, pl));
//	}
//
//	/* stick figures: special cases for special primitives */
//	thistn = np->tech->nodeprotos[pindex-1];
//	count = thistn->layercount;
//	switch (pindex)
//	{
//		case NMETAL1P:
//		case NMETAL2P:
//		case NMETAL3P:
//		case NMETAL4P:
//		case NMETAL5P:
//		case NMETAL6P:
//		case NPOLY1P:
//		case NPOLY2P:
//		case NPACTP:
//		case NNACTP:
//		case NACTP:
//			/* pins disappear with one or two wires */
//			if (tech_pinusecount(ni, NOWINDOWPART)) count = 0;
//			break;
//		case NMETPACTC:
//		case NMETNACTC:
//		case NMETPOLY1C:
//		case NMETPOLY2C:
//		case NMETPOLY12C:
//		case NVIA1:
//		case NVIA2:
//		case NVIA3:
//		case NVIA4:
//		case NVIA5:
//		case NPWBUT:
//		case NNWBUT:
//			/* contacts draw a box the size of the port */
//			count = 1;
//			break;
//		case NTRANSP:
//		case NTRANSN:
//			/* prepare for possible serpentine transistor */
//			count = tech_inittrans(2, ni, pl);
//			break;
//	}
//
//	/* add in displayable variables */
//	pl->realpolys = count;
//	count += tech_displayablenvars(ni, pl->curwindowpart, pl);
//	if (reasonable != 0) *reasonable = count;
//	return(count);
//}
//
//void shapenodepoly(NODEINST *ni, INTBIG box, POLYGON *poly)
//{
//	intshapenodepoly(ni, box, poly, &tech_oneprocpolyloop, &oneprocpolyloop);
//}
//
//void intshapenodepoly(NODEINST *ni, INTBIG box, POLYGON *poly, POLYLOOP *pl, MOCPOLYLOOP *mocpl)
//{
//	TECH_POLYGON *lay;
//	REGISTER INTBIG pindex, lambda, cx, cy;
//	REGISTER NODEPROTO *np;
//	REGISTER TECH_NODES *thistn;
//	REGISTER TECH_PORTS *portdata;
//	static Graphics contactdesc = {LAYERO, BLACK, Graphics.SOLIDC, Graphics.SOLIDC,
//		{0,0,0,0,0,0,0,0}, NOVARIABLE, 0};
//
//	lay = 0;
//	if ((state&MOCMOSSTICKFIGURE) == 0)
//	{
//		/* non-stick-figures: standard components */
//		np = ni->proto;
//		pindex = np->primindex;
//		if (pindex == NSTRANSP || pindex == NSTRANSN)
//		{
//			iteratescalabletransistor(ni, box, poly, pl, mocpl);
//			return;
//		}
//
//		tech_shapenodepoly(ni, box, poly, pl);
//		return;
//	}
//
//	/* handle displayable variables */
//	if (box >= pl->realpolys)
//	{
//		(void)tech_filldisplayablenvar(ni, poly, pl->curwindowpart, 0, pl);
//		return;
//	}
//
//	np = ni->proto;
//	pindex = np->primindex;
//	thistn = tech->nodeprotos[pindex-1];
//	lambda = lambdaofnode(ni);
//	switch (pindex)
//	{
//		case NMETAL1P:
//		case NMETAL2P:
//		case NMETAL3P:
//		case NMETAL4P:
//		case NMETAL5P:
//		case NMETAL6P:
//		case NPOLY1P:
//		case NPOLY2P:
//		case NPACTP:
//		case NNACTP:
//		case NACTP:
//			/* pins disappear with one or two wires */
//			lay = &thistn->layerlist[box];
//			poly->layer = polyCut_lay;
//			if (poly->limit < 2) (void)extendpolygon(poly, 2);
//			cx = (ni->lowx + ni->highx) / 2;
//			cy = (ni->lowy + ni->highy) / 2;
//			poly->xv[0] = cx;   poly->yv[0] = cy;
//			poly->xv[1] = cx;   poly->yv[1] = cy + lambda/2;
//			poly->count = 2;
//			poly->style = DISC;
//			poly->desc = tech->layers[poly->layer];
//			break;
//		case NMETPACTC:
//		case NMETNACTC:
//		case NMETPOLY1C:
//		case NMETPOLY2C:
//		case NMETPOLY12C:
//		case NVIA1:
//		case NVIA2:
//		case NVIA3:
//		case NVIA4:
//		case NVIA5:
//		case NPWBUT:
//		case NNWBUT:
//			/* contacts draw a box the size of the port */
//			lay = &thistn->layerlist[box];
//			poly->layer = polyCut_lay;
//			if (poly->limit < 2) (void)extendpolygon(poly, 2);
//			portdata = &thistn->portlist[0];
//			subrange(ni->lowx, ni->highx, portdata->lowxmul, portdata->lowxsum,
//				portdata->highxmul, portdata->highxsum, &poly->xv[0], &poly->xv[1], lambda);
//			subrange(ni->lowy, ni->highy, portdata->lowymul, portdata->lowysum,
//				portdata->highymul, portdata->highysum, &poly->yv[0], &poly->yv[1], lambda);
//			poly->count = 2;
//			poly->style = CLOSED;
//			poly->desc = &contactdesc;
//
//			/* code cannot be called by multiple procesors: uses globals */
//			NOT_REENTRANT;
//
//			switch (pindex)
//			{
//				case NMETPACTC:   contactdesc.bits = LAYERT1|LAYERT3;  contactdesc.col = COLORT1|COLORT3;   break;
//				case NMETNACTC:   contactdesc.bits = LAYERT1|LAYERT3;  contactdesc.col = COLORT1|COLORT3;   break;
//				case NMETPOLY1C:  contactdesc.bits = LAYERT1|LAYERT2;  contactdesc.col = COLORT1|COLORT2;   break;
//				case NMETPOLY2C:  contactdesc.bits = LAYERO;           contactdesc.col = ORANGE;            break;
//				case NMETPOLY12C: contactdesc.bits = LAYERO;           contactdesc.col = ORANGE;            break;
//				case NVIA1:       contactdesc.bits = LAYERT1|LAYERT4;  contactdesc.col = COLORT1|COLORT4;   break;
//				case NVIA2:       contactdesc.bits = LAYERT4|LAYERT5;  contactdesc.col = COLORT4|COLORT5;   break;
//				case NVIA3:       contactdesc.bits = LAYERO;           contactdesc.col = LBLUE;             break;
//				case NVIA4:       contactdesc.bits = LAYERO;           contactdesc.col = LRED;              break;
//				case NVIA5:       contactdesc.bits = LAYERO;           contactdesc.col = CYAN;              break;
//				case NPWBUT:      contactdesc.bits = LAYERO;           contactdesc.col = BROWN;             break;
//				case NNWBUT:      contactdesc.bits = LAYERO;           contactdesc.col = YELLOW;            break;
//			}
//			break;
//		case NTRANSP:
//		case NTRANSN:
//			/* prepare for possible serpentine transistor */
//			lay = &thistn->gra[box].basics;
//			poly->layer = lay->layernum;
//			if (poly->layer == transistorPoly_lay)
//			{
//				ni->lowy += lambda;
//				ni->highy -= lambda;
//			} else
//			{
//				ni->lowx += lambda + lambda/2;
//				ni->highx -= lambda + lambda/2;
//			}
//			tech_filltrans(poly, &lay, thistn->gra, ni, lambda, box, (TECH_PORTS *)0, pl);
//			if (poly->layer == transistorPoly_lay)
//			{
//				ni->lowy -= lambda;
//				ni->highy += lambda;
//			} else
//			{
//				ni->lowx -= lambda + lambda/2;
//				ni->highx += lambda + lambda/2;
//			}
//			poly->desc = tech->layers[poly->layer];
//			break;
//		default:
//			lay = &thistn->layerlist[box];
//			tech_fillpoly(poly, lay, ni, lambda, FILLED);
//			poly->desc = tech->layers[poly->layer];
//			break;
//	}
//}
//
//INTBIG allnodepolys(NODEINST *ni, POLYLIST *plist, WINDOWPART *win, BOOLEAN onlyreasonable)
//{
//	REGISTER INTBIG tot, j;
//	INTBIG reasonable;
//	REGISTER NODEPROTO *np;
//	REGISTER POLYGON *poly;
//	POLYLOOP mypl;
//	MOCPOLYLOOP mymocpl;
//
//	np = ni->proto;
//	mypl.curwindowpart = win;
//	tot = intnodepolys(ni, &reasonable, win, &mypl, &mymocpl);
//	if (onlyreasonable) tot = reasonable;
//	if (mypl.realpolys < tot) tot = mypl.realpolys;
//	if (ensurepolylist(plist, tot, db_cluster)) return(-1);
//	for(j = 0; j < tot; j++)
//	{
//		poly = plist->polygons[j];
//		poly->tech = tech;
//		intshapenodepoly(ni, j, poly, &mypl, &mymocpl);
//	}
//	return(tot);
//}
//
//void nodesizeoffset(NODEINST *ni, INTBIG *lx, INTBIG *ly, INTBIG *hx, INTBIG *hy)
//{
//	REGISTER INTBIG pindex;
//	REGISTER NODEPROTO *np;
//	REGISTER INTBIG lambda, cx, cy;
//	INTBIG bx, by, ux, uy;
//	REGISTER TECH_NODES *thistn;
//	REGISTER TECH_PORTS *portdata;
//
//	np = ni->proto;
//	pindex = np->primindex;
//	lambda = lambdaofnode(ni);
//	switch (pindex)
//	{
//		case NMETAL1P:
//		case NMETAL2P:
//		case NMETAL3P:
//		case NMETAL4P:
//		case NMETAL5P:
//		case NMETAL6P:
//		case NPOLY1P:
//		case NPOLY2P:
//		case NPACTP:
//		case NNACTP:
//		case NACTP:
//			cx = (ni->lowx + ni->highx) / 2;
//			cy = (ni->lowy + ni->highy) / 2;
//			*lx = (cx - lambda) - ni->lowx;
//			*hx = ni->highx - (cx + lambda);
//			*ly = (cy - lambda) - ni->lowy;
//			*hy = ni->highy - (cy + lambda);
//			break;
//		case NMETPACTC:
//		case NMETNACTC:
//		case NMETPOLY1C:
//		case NMETPOLY2C:
//		case NMETPOLY12C:
//		case NVIA1:
//		case NVIA2:
//		case NVIA3:
//		case NVIA4:
//		case NVIA5:
//		case NPWBUT:
//		case NNWBUT:
//			/* contacts draw a box the size of the port */
//			thistn = tech->nodeprotos[pindex-1];
//			portdata = &thistn->portlist[0];
//			subrange(ni->lowx, ni->highx, portdata->lowxmul, portdata->lowxsum,
//				portdata->highxmul, portdata->highxsum, &bx, &ux, lambda);
//			subrange(ni->lowy, ni->highy, portdata->lowymul, portdata->lowysum,
//				portdata->highymul, portdata->highysum, &by, &uy, lambda);
//			*lx = bx - ni->lowx;
//			*hx = ni->highx - ux;
//			*ly = by - ni->lowy;
//			*hy = ni->highy - uy;
//			break;
//		default:
//			nodeprotosizeoffset(np, lx, ly, hx, hy, ni->parent);
//			if (pindex == NTRANSP || pindex == NTRANSN)
//			{
//				*lx += lambda + lambda/2;
//				*hx += lambda + lambda/2;
//				*ly += lambda;
//				*hy += lambda;
//			}
//			break;
//	}
//}
//
///******************** NODE DESCRIPTION (ELECTRICAL) ********************/
//
//INTBIG nodeEpolys(NODEINST *ni, INTBIG *reasonable, WINDOWPART *win)
//{
//	return(intEnodepolys(ni, reasonable, win, &tech_oneprocpolyloop, &oneprocpolyloop));
//}
//
//INTBIG intEnodepolys(NODEINST *ni, INTBIG *reasonable, WINDOWPART *win, POLYLOOP *pl, MOCPOLYLOOP *mocpl)
//{
//	REGISTER INTBIG pindex, count;
//
//	if ((state&MOCMOSSTICKFIGURE) != 0) return(0);
//
//	/* non-stick-figures: standard components */
//	pindex = ni->proto->primindex;
//	if (pindex == NSTRANSP || pindex == NSTRANSN)
//	{
//		count = initializescalabletransistor(ni, reasonable, win, pl, mocpl);
//		return(count);
//	}
//	return(tech_nodeEpolys(ni, reasonable, win, pl));
//}
//
//void shapeEnodepoly(NODEINST *ni, INTBIG box, POLYGON *poly)
//{
//	intshapeEnodepoly(ni, box, poly, &tech_oneprocpolyloop, &oneprocpolyloop);
//}
//
//void intshapeEnodepoly(NODEINST *ni, INTBIG box, POLYGON *poly, POLYLOOP *pl, MOCPOLYLOOP *mocpl)
//{
//	REGISTER INTBIG pindex;
//
//	pindex = ni->proto->primindex;
//	if (pindex == NSTRANSP || pindex == NSTRANSN)
//	{
//		iteratescalabletransistor(ni, box, poly, pl, mocpl);
//		return;
//	}
//
//	tech_shapeEnodepoly(ni, box, poly, pl);
//}
//
//INTBIG allnodeEpolys(NODEINST *ni, POLYLIST *plist, WINDOWPART *win, BOOLEAN onlyreasonable)
//{
//	REGISTER INTBIG tot, j;
//	INTBIG reasonable;
//	REGISTER POLYGON *poly;
//	POLYLOOP mypl;
//	MOCPOLYLOOP mymocpl;
//
//	mypl.curwindowpart = win;
//	tot = intEnodepolys(ni, &reasonable, win, &mypl, &mymocpl);
//	if (onlyreasonable) tot = reasonable;
//	if (ensurepolylist(plist, tot, db_cluster)) return(-1);
//	for(j = 0; j < tot; j++)
//	{
//		poly = plist->polygons[j];
//		poly->tech = tech;
//		intshapeEnodepoly(ni, j, poly, &mypl, &mymocpl);
//	}
//	return(tot);
//}
//
///******************** SCALABLE TRANSISTOR DESCRIPTION ********************/
//
//INTBIG initializescalabletransistor(NODEINST *ni, INTBIG *reasonable, WINDOWPART *win, POLYLOOP *pl, MOCPOLYLOOP *mocpl)
//{
//	REGISTER INTBIG pindex, count, lambda, activewid, requestedwid, extrainset, nodewid, extracuts;
//	REGISTER INTBIG cutsize, cutindent, cutsep;
//	INTBIG olx, ohx, oly, ohy;
//	REGISTER VARIABLE *var;
//	REGISTER CHAR *pt;
//	TECH_NODES *thistn;
//	REGISTER NODEPROTO *np;
//
//	/* determine the width */
//	np = ni->proto;
//	pindex = np->primindex;
//	lambda = lambdaofnode(ni);
//	nodewid = (ni->highx - ni->lowx) * WHOLE / lambda;
//	activewid = nodewid - K14;
//	extrainset = 0;
//
//	/* determine special configurations (number of active contacts, inset of active contacts) */
//	mocpl->numcontacts = 2;
//	mocpl->insetcontacts = FALSE;
//	var = getvalkey((INTBIG)ni, VNODEINST, VSTRING, transcontactkey);
//	if (var != NOVARIABLE)
//	{
//		pt = (CHAR *)var->addr;
//		if (*pt == '0' || *pt == '1' || *pt == '2')
//		{
//			mocpl->numcontacts = *pt - '0';
//			pt++;
//		}
//		if (*pt == 'i' || *pt == 'I') mocpl->insetcontacts = TRUE;
//	}
//	mocpl->boxoffset = 4 - mocpl->numcontacts * 2;
//
//	/* determine width */
//	var = getvalkey((INTBIG)ni, VNODEINST, -1, el_attrkey_width);
//	if (var != NOVARIABLE)
//	{
//		pt = describevariable(var, -1, -1);
//		if (*pt == '-' || *pt == '+' || isdigit(*pt))
//		{
//			requestedwid = atofr(pt);
//			if (requestedwid > activewid)
//			{
//				ttyputmsg(_("Warning: cell %s, node %s requests width of %s but is only %s wide"),
//					describenodeproto(ni->parent), describenodeinst(ni), frtoa(requestedwid),
//						frtoa(activewid));
//			}
//			if (requestedwid < activewid && requestedwid > 0)
//			{
//				extrainset = (activewid - requestedwid) / 2;
//				activewid = requestedwid;
//			}
//		}
//	}
//	mocpl->actinset = (nodewid-activewid) / 2;
//	mocpl->polyinset = mocpl->actinset - K2;
//	mocpl->actcontinset = K7 + extrainset;
//
//	/* contacts must be 5 wide at a minimum */
//	if (activewid < K5) mocpl->actcontinset -= (K5-activewid)/2;
//	mocpl->metcontinset = mocpl->actcontinset + H0;
//
//	/* determine the multicut information */
//	mocpl->moscutsize = cutsize = mpa.f1;
//	cutindent = mpa.f3;
//	mocpl->moscutsep = cutsep = mpa.f4;
//	nodesizeoffset(ni, &olx, &oly, &ohx, &ohy);
//	mocpl->numcuts = (activewid-cutindent*2+cutsep) / (cutsize+cutsep);
//	if (mocpl->numcuts <= 0) mocpl->numcuts = 1;
//	if (mocpl->numcuts != 1)
//		mocpl->moscutbase = (activewid-cutindent*2 - cutsize*mocpl->numcuts -
//			cutsep*(mocpl->numcuts-1)) / 2 + (nodewid-activewid)/2 + cutindent;
//
//	/* now compute the number of polygons */
//	extracuts = (mocpl->numcuts-1)*2 - (2-mocpl->numcontacts) * mocpl->numcuts;
//	count = tech_nodepolys(ni, reasonable, win, pl) + extracuts - mocpl->boxoffset;
//	thistn = np->tech->nodeprotos[pindex-1];
//	pl->realpolys = thistn->layercount + extracuts;
//	return(count);
//}
//
//void iteratescalabletransistor(NODEINST *ni, INTBIG box, POLYGON *poly, POLYLOOP *pl, MOCPOLYLOOP *mocpl)
//{
//	TECH_POLYGON *lay;
//	REGISTER INTBIG i, lambda, count, cut, pindex, shift;
//	REGISTER TECH_NODES *thistn;
//	REGISTER NODEPROTO *np;
//	TECH_POLYGON localtp;
//	INTBIG mypoints[8];
//
//	np = ni->proto;
//	pindex = np->primindex;
//	thistn = tech->nodeprotos[pindex-1];
//	box += mocpl->boxoffset;
//	if (box <= 7)
//	{
//		lay = &thistn->layerlist[box];
//		lambda = lambdaofnode(ni);
//		localtp.layernum = lay->layernum;
//		localtp.portnum = lay->portnum;
//		localtp.count = lay->count;
//		localtp.style = lay->style;
//		localtp.representation = lay->representation;
//		localtp.points = mypoints;
//		for(i=0; i<8; i++) mypoints[i] = lay->points[i];
//		switch (box)
//		{
//			case 4:		/* active that passes through gate */
//				mypoints[1] = mocpl->actinset;
//				mypoints[5] = -mocpl->actinset;
//				break;
//			case 0:		/* active surrounding contacts */
//			case 2:
//				mypoints[1] = mocpl->actcontinset;
//				mypoints[5] = -mocpl->actcontinset;
//				if (mocpl->insetcontacts)
//				{
//					if (mypoints[3] < 0) shift = -H0; else shift = H0;
//					mypoints[3] += shift;
//					mypoints[7] += shift;
//				}
//				break;
//			case 5:		/* poly */
//				mypoints[1] = mocpl->polyinset;
//				mypoints[5] = -mocpl->polyinset;
//				break;
//			case 1:		/* metal surrounding contacts */
//			case 3:
//				mypoints[1] = mocpl->metcontinset;
//				mypoints[5] = -mocpl->metcontinset;
//				if (mocpl->insetcontacts)
//				{
//					if (mypoints[3] < 0) shift = -H0; else shift = H0;
//					mypoints[3] += shift;
//					mypoints[7] += shift;
//				}
//				break;
//			case 6:		/* well and select */
//			case 7:
//				if (mocpl->insetcontacts)
//				{
//					mypoints[3] += H0;
//					mypoints[7] -= H0;
//				}
//				break;
//		}
//		tech_fillpoly(poly, &localtp, ni, lambda, FILLED);
//		poly->desc = tech->layers[poly->layer];
//		if (lay->portnum < 0) poly->portproto = NOPORTPROTO; else
//			poly->portproto = thistn->portlist[lay->portnum].addr;
//		return;
//	}
//	if (box >= pl->realpolys)
//	{
//		/* displayable variables */
//		(void)tech_filldisplayablenvar(ni, poly, pl->curwindowpart, 0, pl);
//		return;
//	}
//
//	/* multiple contact cuts */
//	count = thistn->layercount - 2;
//	if (box >= count)
//	{
//		lambda = lambdaofnode(ni);
//		lay = &thistn->layerlist[count+(box-count) / mocpl->numcuts];
//		cut = (box-count) % mocpl->numcuts;
//		localtp.layernum = lay->layernum;
//		localtp.portnum = lay->portnum;
//		localtp.count = lay->count;
//		localtp.style = lay->style;
//		localtp.representation = lay->representation;
//		localtp.points = mypoints;
//		for(i=0; i<8; i++) mypoints[i] = lay->points[i];
//
//		if (mocpl->numcuts == 1)
//		{
//			mypoints[1] = (ni->highx-ni->lowx)/2 * WHOLE/lambda - mocpl->moscutsize/2;
//			mypoints[5] = (ni->highx-ni->lowx)/2 * WHOLE/lambda + mocpl->moscutsize/2;
//		} else
//		{
//			mypoints[1] = mocpl->moscutbase + cut * (mocpl->moscutsize + mocpl->moscutsep);
//			mypoints[5] = mypoints[1] + mocpl->moscutsize;
//		}
//		if (mocpl->insetcontacts)
//		{
//			if (mypoints[3] < 0) shift = -H0; else shift = H0;
//			mypoints[3] += shift;
//			mypoints[7] += shift;
//		}
//
//		tech_fillpoly(poly, &localtp, ni, lambda, FILLED);
//		poly->desc = tech->layers[poly->layer];
//		poly->portproto = NOPORTPROTO;
//		return;
//	}
//	tech_shapenodepoly(ni, box, poly, pl);
//}
//
///******************** ARC DESCRIPTION ********************/
//
//INTBIG arcpolys(ARCINST *ai, WINDOWPART *win)
//{
//	return(intarcpolys(ai, win, &tech_oneprocpolyloop, &oneprocpolyloop));
//}
//
//INTBIG intarcpolys(ARCINST *ai, WINDOWPART *win, POLYLOOP *pl, MOCPOLYLOOP *mocpl)
//{
//	REGISTER INTBIG i;
//
//	i = 1;
//	mocpl->arrowbox = -1;
//	if ((ai->userbits&ISDIRECTIONAL) != 0) mocpl->arrowbox = i++;
//
//	/* add in displayable variables */
//	pl->realpolys = i;
//	i += tech_displayableavars(ai, win, pl);
//	return(i);
//}
//
//void shapearcpoly(ARCINST *ai, INTBIG box, POLYGON *poly)
//{
//	intshapearcpoly(ai, box, poly, &tech_oneprocpolyloop, &oneprocpolyloop);
//}
//
//void intshapearcpoly(ARCINST *ai, INTBIG box, POLYGON *poly, POLYLOOP *pl, MOCPOLYLOOP *mocpl)
//{
//	REGISTER INTBIG aindex;
//	REGISTER INTBIG angle;
//	REGISTER INTBIG x1,y1, x2,y2, i;
//	REGISTER TECH_ARCLAY *thista;
//	static Graphics intense = {LAYERO, RED, Graphics.SOLIDC, Graphics.SOLIDC,
//		{0,0,0,0,0,0,0,0}, NOVARIABLE, 0};
//
//	/* handle displayable variables */
//	if (box >= pl->realpolys)
//	{
//		(void)tech_filldisplayableavar(ai, poly, pl->curwindowpart, 0, pl);
//		return;
//	}
//
//	/* initialize for the arc */
//	aindex = ai->proto->arcindex;
//	thista = &arcprotos[aindex]->list[box];
//	poly->layer = thista->lay;
//	switch (ai->proto->arcindex)
//	{
//		case AMETAL1:
//		case AMETAL2:
//		case AMETAL3:
//		case AMETAL4:
//		case AMETAL5:
//		case AMETAL6:
//			intense.col = BLUE;
//			break;
//		case APOLY1:
//		case APOLY2:
//			intense.col = RED;
//			break;
//		case APACT:
//		case ANACT:
//		case AACT:
//			intense.col = DGREEN;
//			break;
//	}
//	if (mocpl->arrowbox < 0 || box == 0)
//	{
//		/* simple arc */
//		poly->desc = tech->layers[poly->layer];
//		makearcpoly(ai->length, ai->width-ai->proto->nominalwidth, ai, poly, thista->style);
//		return;
//	}
//
//	/* prepare special information for directional arcs */
//	poly->desc = &intense;
//	x1 = ai->end[0].xpos;   y1 = ai->end[0].ypos;
//	x2 = ai->end[1].xpos;   y2 = ai->end[1].ypos;
//	angle = ((ai->userbits&AANGLE) >> AANGLESH) * 10;
//	if ((ai->userbits&REVERSEEND) != 0)
//	{
//		i = x1;   x1 = x2;   x2 = i;
//		i = y1;   y1 = y2;   y2 = i;
//		angle = (angle+1800) % 3600;
//	}
//
//	/* draw the directional arrow */
//	poly->style = VECTORS;
//	poly->layer = -1;
//	if (poly->limit < 2) (void)extendpolygon(poly, 2);
//	poly->count = 0;
//	if ((ai->userbits&NOTEND1) == 0)
//		tech_addheadarrow(poly, angle, x2, y2, lambdaofarc(ai));
//}
//
//INTBIG allarcpolys(ARCINST *ai, POLYLIST *plist, WINDOWPART *win)
//{
//	REGISTER INTBIG tot, j;
//	POLYLOOP mypl;
//	MOCPOLYLOOP mymocpl;
//
//	mypl.curwindowpart = win;
//	tot = intarcpolys(ai, win, &mypl, &mymocpl);
//	if (ensurepolylist(plist, tot, db_cluster)) return(-1);
//	for(j = 0; j < tot; j++)
//	{
//		intshapearcpoly(ai, j, plist->polygons[j], &mypl, &mymocpl);
//	}
//	return(tot);
//}
//
//INTBIG arcwidthoffset(ARCINST *ai)
//{
//	return(ai->proto->nominalwidth);
//}
//
///******************** SUPPORT ROUTINES ********************/
//
///*
// * Routine to switch N and P layers (not terribly useful)
// */
//void switchnp(void)
//{
//	REGISTER LIBRARY *lib;
//	REGISTER NODEPROTO *np;
//	REGISTER NODEINST *ni, *rni;
//	REGISTER ARCINST *ai;
//	REGISTER ARCPROTO *ap, *app, *apn;
//	REGISTER PORTPROTO *pp, *rpp;
//	REGISTER PORTARCINST *pi;
//	REGISTER PORTEXPINST *pe;
//	REGISTER INTBIG i, j, k;
//
//	/* find the important node and arc prototypes */
//	setupprimswap(NPACTP, NNACTP, &primswap[0]);
//	setupprimswap(NMETPACTC, NMETNACTC, &primswap[1]);
//	setupprimswap(NTRANSP, NTRANSN, &primswap[2]);
//	setupprimswap(NPWBUT, NNWBUT, &primswap[3]);
//	setupprimswap(NPACTIVEN, NNACTIVEN, &primswap[4]);
//	setupprimswap(NSELECTPN, NSELECTNN, &primswap[5]);
//	setupprimswap(NWELLPN, NWELLNN, &primswap[6]);
//	app = apn = NOARCPROTO;
//	for(ap = tech->firstarcproto; ap != NOARCPROTO; ap = ap->nextarcproto)
//	{
//		if (namesame(ap->protoname, x_("P-Active")) == 0) app = ap;
//		if (namesame(ap->protoname, x_("N-Active")) == 0) apn = ap;
//	}
//
//	for(lib = el_curlib; lib != NOLIBRARY; lib = lib->nextlibrary)
//	{
//		for(np = lib->firstnodeproto; np != NONODEPROTO; np = np->nextnodeproto)
//		{
//			for(ni = np->firstnodeinst; ni != NONODEINST; ni = ni->nextnodeinst)
//			{
//				if (ni->proto->primindex == 0) continue;
//				if (ni->proto->tech != tech) continue;
//				for(i=0; i<7; i++)
//				{
//					for(k=0; k<2; k++)
//					{
//						if (ni->proto == primswap[i].np[k])
//						{
//							ni->proto = primswap[i].np[1-k];
//							for(pi = ni->firstportarcinst; pi != NOPORTARCINST; pi = pi->nextportarcinst)
//							{
//								for(j=0; j<primswap[i].portcount; j++)
//								{
//									if (pi->proto == primswap[i].pp[k][j])
//									{
//										pi->proto = primswap[i].pp[1-k][j];
//										break;
//									}
//								}
//							}
//							for(pe = ni->firstportexpinst; pe != NOPORTEXPINST; pe = pe->nextportexpinst)
//							{
//								for(j=0; j<primswap[i].portcount; j++)
//								{
//									if (pe->proto == primswap[i].pp[k][j])
//									{
//										pe->proto = primswap[i].pp[1-k][j];
//										pe->exportproto->subportproto = pe->proto;
//										break;
//									}
//								}
//							}
//							break;
//						}
//					}
//				}
//			}
//			for(ai = np->firstarcinst; ai != NOARCINST; ai = ai->nextarcinst)
//			{
//				if (ai->proto->tech != tech) continue;
//				if (ai->proto == app)
//				{
//					ai->proto = apn;
//				} else if (ai->proto == apn)
//				{
//					ai->proto = app;
//				}
//			}
//		}
//		for(np = lib->firstnodeproto; np != NONODEPROTO; np = np->nextnodeproto)
//		{
//			for(pp = np->firstportproto; pp != NOPORTPROTO; pp = pp->nextportproto)
//			{
//				/* find the primitive at the bottom */
//				rpp = pp->subportproto;
//				rni = pp->subnodeinst;
//				while (rni->proto->primindex == 0)
//				{
//					rni = rpp->subnodeinst;
//					rpp = rpp->subportproto;
//				}
//				pp->connects = rpp->connects;
//			}
//		}
//	}
//	for(i=0; i<7; i++)
//	{
//		ni = primswap[i].np[0]->firstinst;
//		primswap[i].np[0]->firstinst = primswap[i].np[1]->firstinst;
//		primswap[i].np[1]->firstinst = ni;
//	}
//}
//
///*
// * Helper routine for "switchnp()".
// */
//void setupprimswap(INTBIG index1, INTBIG index2, PRIMSWAP *swap)
//{
//	REGISTER NODEPROTO *np;
//	REGISTER PORTPROTO *pp;
//
//	swap->np[0] = swap->np[1] = NONODEPROTO;
//	for(np = tech->firstnodeproto; np != NONODEPROTO; np = np->nextnodeproto)
//	{
//		if (np->primindex == index1) swap->np[0] = np;
//		if (np->primindex == index2) swap->np[1] = np;
//	}
//	if (swap->np[0] == NONODEPROTO || swap->np[1] == NONODEPROTO) return;
//	swap->portcount = 0;
//	for(pp = swap->np[0]->firstportproto; pp != NOPORTPROTO; pp = pp->nextportproto)
//		swap->pp[0][swap->portcount++] = pp;
//	swap->portcount = 0;
//	for(pp = swap->np[1]->firstportproto; pp != NOPORTPROTO; pp = pp->nextportproto)
//		swap->pp[1][swap->portcount++] = pp;
//}
//
///*
// * Routine to set the technology to state "newstate", which encodes the number of metal
// * layers, whether it is a deep process, and other rules.
// */
//void setstate(INTBIG newstate)
//{
//	extern void tech_initmaxdrcsurround(void);
//
//	switch (newstate&MOCMOSMETALS)
//	{
//		/* cannot use deep rules if less than 5 layers of metal */
//		case MOCMOS2METAL:
//		case MOCMOS3METAL:
//		case MOCMOS4METAL:
//			if ((newstate&MOCMOSRULESET) == MOCMOSDEEPRULES)
//				newstate = (newstate & ~MOCMOSRULESET) | MOCMOSSUBMRULES;
//			break;
//
//		/* cannot use scmos rules if more than 4 layers of metal */
//		case MOCMOS5METAL:
//		case MOCMOS6METAL:
//			if ((newstate&MOCMOSRULESET) == MOCMOSSCMOSRULES)
//				newstate = (newstate & ~MOCMOSRULESET) | MOCMOSSUBMRULES;
//			break;
//	}
//
//	if (state == newstate) return;
//	state = newstate;
//
//	/* set stick-figure state */
//	if ((state&MOCMOSSTICKFIGURE) != 0)
//	{
//		/* stick figure drawing */
//		tech->nodesizeoffset = nodesizeoffset;
//		tech->arcpolys = arcpolys;
//		tech->shapearcpoly = shapearcpoly;
//		tech->allarcpolys = allarcpolys;
//		tech->arcwidthoffset = arcwidthoffset;
//	} else
//	{
//		/* full figure drawing */
//		tech->nodesizeoffset = 0;
//		tech->arcpolys = 0;
//		tech->shapearcpoly = 0;
//		tech->allarcpolys = 0;
//		tech->arcwidthoffset = 0;
//	}
//
//	/* set rules */
//	if (loadDRCtables()) return;
//
//	/* handle scalable transistors */
//	if ((state&MOCMOSSCALABLETRAN) == 0)
//	{
//		/* hide scalable transistors */
//		tpas.creation->userbits |= NNOTUSED;
//		tnas.creation->userbits |= NNOTUSED;
//	} else
//	{
//		/* show scalable transistors */
//		tpas.creation->userbits &= ~NNOTUSED;
//		tnas.creation->userbits &= ~NNOTUSED;
//	}
//
//	/* disable Metal-3/4/5/6-Pin, Metal-2/3/4/5-Metal-3/4/5/6-Con, Metal-3/4/5/6-Node, Via-2/3/4/5-Node */
//	pm3.creation->userbits |= NNOTUSED;
//	pm4.creation->userbits |= NNOTUSED;
//	pm5.creation->userbits |= NNOTUSED;
//	pm6.creation->userbits |= NNOTUSED;
//	m2m3.creation->userbits |= NNOTUSED;
//	m3m4.creation->userbits |= NNOTUSED;
//	m4m5.creation->userbits |= NNOTUSED;
//	m5m6.creation->userbits |= NNOTUSED;
//	m3.creation->userbits |= NNOTUSED;
//	m4.creation->userbits |= NNOTUSED;
//	m5.creation->userbits |= NNOTUSED;
//	m6.creation->userbits |= NNOTUSED;
//	v2.creation->userbits |= NNOTUSED;
//	v3.creation->userbits |= NNOTUSED;
//	v4.creation->userbits |= NNOTUSED;
//	v5.creation->userbits |= NNOTUSED;
//
//	/* disable Polysilicon-2 */
//	poly2_arc.creation->userbits |= ANOTUSED;
//	pp2.creation->userbits |= NNOTUSED;
//	mp2.creation->userbits |= NNOTUSED;
//	mp12.creation->userbits |= NNOTUSED;
//	p2.creation->userbits |= NNOTUSED;
//
//	/* disable metal 3-6 arcs */
//	metal3_arc.creation->userbits |= ANOTUSED;
//	metal4_arc.creation->userbits |= ANOTUSED;
//	metal5_arc.creation->userbits |= ANOTUSED;
//	metal6_arc.creation->userbits |= ANOTUSED;
//
//	/* enable the desired nodes */
//	switch (state&MOCMOSMETALS)
//	{
//		case MOCMOS6METAL:
//			pm6.creation->userbits &= ~NNOTUSED;
//			m5m6.creation->userbits &= ~NNOTUSED;
//			m6.creation->userbits &= ~NNOTUSED;
//			v5.creation->userbits &= ~NNOTUSED;
//			metal6_arc.creation->userbits &= ~ANOTUSED;
//			/* FALLTHROUGH */ 
//		case MOCMOS5METAL:
//			pm5.creation->userbits &= ~NNOTUSED;
//			m4m5.creation->userbits &= ~NNOTUSED;
//			m5.creation->userbits &= ~NNOTUSED;
//			v4.creation->userbits &= ~NNOTUSED;
//			metal5_arc.creation->userbits &= ~ANOTUSED;
//			/* FALLTHROUGH */ 
//		case MOCMOS4METAL:
//			pm4.creation->userbits &= ~NNOTUSED;
//			m3m4.creation->userbits &= ~NNOTUSED;
//			m4.creation->userbits &= ~NNOTUSED;
//			v3.creation->userbits &= ~NNOTUSED;
//			metal4_arc.creation->userbits &= ~ANOTUSED;
//			/* FALLTHROUGH */ 
//		case MOCMOS3METAL:
//			pm3.creation->userbits &= ~NNOTUSED;
//			m2m3.creation->userbits &= ~NNOTUSED;
//			m3.creation->userbits &= ~NNOTUSED;
//			v2.creation->userbits &= ~NNOTUSED;
//			metal3_arc.creation->userbits &= ~ANOTUSED;
//			break;
//	}
//	if ((state&MOCMOSRULESET) != MOCMOSDEEPRULES)
//	{
//		if ((state&MOCMOSTWOPOLY) != 0)
//		{
//			/* non-DEEP: enable Polysilicon-2 */
//			poly2_arc.creation->userbits &= ~ANOTUSED;
//			pp2.creation->userbits &= ~NNOTUSED;
//			mp2.creation->userbits &= ~NNOTUSED;
//			mp12.creation->userbits &= ~NNOTUSED;
//			p2.creation->userbits &= ~NNOTUSED;
//		}
//	}
//
//	/* now rewrite the description */
//	(void)reallocstring(&tech->techdescript, describestate(state), tech->cluster);
//
//	/* recache design rules */
//	tech_initmaxdrcsurround();
//}
//
///*
// * Routine to remove all information in the design rule tables.
// * Returns true on error.
// */
//BOOLEAN loadDRCtables(void)
//{
//	REGISTER INTBIG i, tot, totarraybits, layer1, layer2, layert1, layert2, temp, index,
//		distance, goodrule, when, node, pass, arc;
//	INTBIG *condist, *uncondist, *condistW, *uncondistW,
//		*condistM, *uncondistM, *minsize, *edgedist;
//	CHAR **condistrules, **uncondistrules, **condistWrules, **uncondistWrules,
//		**condistMrules, **uncondistMrules, **minsizerules, **edgedistrules,
//		proc[20], metal[20], rule[100];
//	REGISTER BOOLEAN errorfound;
//	REGISTER TECH_NODES *nty;
//	REGISTER TECH_ARCS *aty;
//	REGISTER NODEPROTO *np;
//	REGISTER ARCPROTO *ap;
//
//	/* allocate local copy of DRC tables */
//	tot = (MAXLAYERS * MAXLAYERS + MAXLAYERS) / 2;
//	condist = (INTBIG *)emalloc(tot * SIZEOFINTBIG, el_tempcluster);
//	uncondist = (INTBIG *)emalloc(tot * SIZEOFINTBIG, el_tempcluster);
//	condistW = (INTBIG *)emalloc(tot * SIZEOFINTBIG, el_tempcluster);
//	uncondistW = (INTBIG *)emalloc(tot * SIZEOFINTBIG, el_tempcluster);
//	condistM = (INTBIG *)emalloc(tot * SIZEOFINTBIG, el_tempcluster);
//	uncondistM = (INTBIG *)emalloc(tot * SIZEOFINTBIG, el_tempcluster);
//	edgedist = (INTBIG *)emalloc(tot * SIZEOFINTBIG, el_tempcluster);
//	condistrules = (CHAR **)emalloc(tot * (sizeof (CHAR *)), el_tempcluster);
//	uncondistrules = (CHAR **)emalloc(tot * (sizeof (CHAR *)), el_tempcluster);
//	condistWrules = (CHAR **)emalloc(tot * (sizeof (CHAR *)), el_tempcluster);
//	uncondistWrules = (CHAR **)emalloc(tot * (sizeof (CHAR *)), el_tempcluster);
//	condistMrules = (CHAR **)emalloc(tot * (sizeof (CHAR *)), el_tempcluster);
//	uncondistMrules = (CHAR **)emalloc(tot * (sizeof (CHAR *)), el_tempcluster);
//	edgedistrules = (CHAR **)emalloc(tot * (sizeof (CHAR *)), el_tempcluster);
//	minsize = (INTBIG *)emalloc(MAXLAYERS * SIZEOFINTBIG, el_tempcluster);
//	minsizerules = (CHAR **)emalloc(MAXLAYERS * (sizeof (CHAR *)), el_tempcluster);
//
//	/* clear all rules */
//	for(i=0; i<tot; i++)
//	{
//		condist[i] = uncondist[i] = XX;
//		condistW[i] = uncondistW[i] = XX;
//		condistM[i] = uncondistM[i] = XX;
//		edgedist[i] = XX;
//		(void)allocstring(&condistrules[i], x_(""), el_tempcluster);
//		(void)allocstring(&uncondistrules[i], x_(""), el_tempcluster);
//		(void)allocstring(&condistWrules[i], x_(""), el_tempcluster);
//		(void)allocstring(&uncondistWrules[i], x_(""), el_tempcluster);
//		(void)allocstring(&condistMrules[i], x_(""), el_tempcluster);
//		(void)allocstring(&uncondistMrules[i], x_(""), el_tempcluster);
//		(void)allocstring(&edgedistrules[i], x_(""), el_tempcluster);
//	}
//	for(i=0; i<MAXLAYERS; i++)
//	{
//		minsize[i] = XX;
//		(void)allocstring(&minsizerules[i], x_(""), el_tempcluster);
//	}
//
//	/* load the DRC tables from the explanation table */
//	errorfound = FALSE;
//	for(pass=0; pass<2; pass++)
//	{
//		for(i=0; drcrules[i].rule != 0; i++)
//		{
//			/* see if the rule applies */
//			if (pass == 0)
//			{
//				if (drcrules[i].ruletype == NODSIZ) continue;
//			} else
//			{
//				if (drcrules[i].ruletype != NODSIZ) continue;
//			}
//
//			when = drcrules[i].when;
//			goodrule = 1;
//			if ((when&(DE|SU|SC)) != 0)
//			{
//				switch (state&MOCMOSRULESET)
//				{
//					case MOCMOSDEEPRULES:  if ((when&DE) == 0) goodrule = 0;   break;
//					case MOCMOSSUBMRULES:  if ((when&SU) == 0) goodrule = 0;   break;
//					case MOCMOSSCMOSRULES: if ((when&SC) == 0) goodrule = 0;   break;
//				}
//				if (goodrule == 0) continue;
//			}
//			if ((when&(M2|M3|M4|M5|M6)) != 0)
//			{
//				switch (state&MOCMOSMETALS)
//				{
//					case MOCMOS2METAL:  if ((when&M2) == 0) goodrule = 0;   break;
//					case MOCMOS3METAL:  if ((when&M3) == 0) goodrule = 0;   break;
//					case MOCMOS4METAL:  if ((when&M4) == 0) goodrule = 0;   break;
//					case MOCMOS5METAL:  if ((when&M5) == 0) goodrule = 0;   break;
//					case MOCMOS6METAL:  if ((when&M6) == 0) goodrule = 0;   break;
//				}
//				if (goodrule == 0) continue;
//			}
//			if ((when&AC) != 0)
//			{
//				if ((state&MOCMOSALTAPRULES) == 0) continue;
//			}
//			if ((when&NAC) != 0)
//			{
//				if ((state&MOCMOSALTAPRULES) != 0) continue;
//			}
//			if ((when&SV) != 0)
//			{
//				if ((state&MOCMOSNOSTACKEDVIAS) != 0) continue;
//			}
//			if ((when&NSV) != 0)
//			{
//				if ((state&MOCMOSNOSTACKEDVIAS) == 0) continue;
//			}
//
//			/* find the layer names */
//			if (drcrules[i].layer1 == 0) layer1 = -1; else
//			{
//				for(layer1=0; layer1<tech->layercount; layer1++)
//					if (namesame(layer_names[layer1], drcrules[i].layer1) == 0) break;
//				if (layer1 >= tech->layercount)
//				{
//					ttyputerr(x_("Warning: no layer '%s' in mocmos technology"), drcrules[i].layer1);
//					errorfound = TRUE;
//					break;
//				}
//			}
//			if (drcrules[i].layer2 == 0) layer2 = -1; else
//			{
//				for(layer2=0; layer2<tech->layercount; layer2++)
//					if (namesame(layer_names[layer2], drcrules[i].layer2) == 0) break;
//				if (layer2 >= tech->layercount)
//				{
//					ttyputerr(x_("Warning: no layer '%s' in mocmos technology"), drcrules[i].layer2);
//					errorfound = TRUE;
//					break;
//				}
//			}
//			node = -1;
//			nty = 0;
//			aty = 0;
//			if (drcrules[i].nodename != 0)
//			{
//				if (drcrules[i].ruletype == ASURROUND)
//				{
//					for(arc=0; arc<tech->arcprotocount; arc++)
//					{
//						aty = tech->arcprotos[arc];
//						ap = aty->creation;
//						if (namesame(ap->protoname, drcrules[i].nodename) == 0) break;
//					}
//					if (arc >= tech->arcprotocount)
//					{
//						ttyputerr(x_("Warning: no arc '%s' in mocmos technology"), drcrules[i].nodename);
//						errorfound = TRUE;
//						break;
//					}
//				} else
//				{
//					for(node=0; node<tech->nodeprotocount; node++)
//					{
//						nty = tech->nodeprotos[node];
//						np = nty->creation;
//						if (namesame(np->protoname, drcrules[i].nodename) == 0) break;
//					}
//					if (node >= tech->nodeprotocount)
//					{
//						ttyputerr(x_("Warning: no node '%s' in mocmos technology"), drcrules[i].nodename);
//						errorfound = TRUE;
//						break;
//					}
//				}
//			}
//
//			/* get more information about the rule */
//			distance = drcrules[i].distance;
//			proc[0] = 0;
//			if ((when&(DE|SU|SC)) != 0)
//			{
//				switch (state&MOCMOSRULESET)
//				{
//					case MOCMOSDEEPRULES:  estrcpy(proc, x_("DEEP"));   break;
//					case MOCMOSSUBMRULES:  estrcpy(proc, x_("SUBM"));   break;
//					case MOCMOSSCMOSRULES: estrcpy(proc, x_("SCMOS"));  break;
//				}
//			}
//			metal[0] = 0;
//			if ((when&(M2|M3|M4|M5|M6)) != 0)
//			{
//				switch (state&MOCMOSMETALS)
//				{
//					case MOCMOS2METAL:  estrcpy(metal, x_("2m"));   break;
//					case MOCMOS3METAL:  estrcpy(metal, x_("3m"));   break;
//					case MOCMOS4METAL:  estrcpy(metal, x_("4m"));   break;
//					case MOCMOS5METAL:  estrcpy(metal, x_("5m"));   break;
//					case MOCMOS6METAL:  estrcpy(metal, x_("6m"));   break;
//				}
//				if (goodrule == 0) continue;
//			}
//			estrcpy(rule, drcrules[i].rule);
//			if (proc[0] != 0 || metal[0] != 0)
//			{
//				estrcat(rule, x_(", "));
//				estrcat(rule, metal);
//				estrcat(rule, proc);
//			}
//			layert1 = layer1;   layert2 = layer2;
//			if (layert1 > layert2) { temp = layert1; layert1 = layert2;  layert2 = temp; }
//			index = (layert1+1) * (layert1/2) + (layert1&1) * ((layert1+1)/2);
//			index = layert2 + tech->layercount * layert1 - index;
//
//			/* set the rule */
//			switch (drcrules[i].ruletype)
//			{
//				case MINWID:
//					minsize[layer1] = distance;
//					(void)reallocstring(&minsizerules[layer1], rule, el_tempcluster);
//					setlayerminwidth(drcrules[i].layer1, distance);
//					break;
//				case NODSIZ:
//					setdefnodesize(nty, node, distance, distance);
//					break;
//				case SURROUND:
//					setlayersurroundlayer(nty, layer1, layer2, distance, minsize);
//					break;
//				case ASURROUND:
//					setarclayersurroundlayer(aty, layer1, layer2, distance);
//					break;
//				case VIASUR:
//					setlayersurroundvia(nty, layer1, distance);
//					nty->f3 = (INTSML)distance;
//					break;
//				case TRAWELL:
//					settransistorwellsurround(distance);
//					break;
//				case TRAPOLY:
//					settransistorpolyoverhang(distance);
//					break;
//				case TRAACTIVE:
//					settransistoractiveoverhang(distance);
//					break;
//				case SPACING:
//					condist[index] = uncondist[index] = distance;
//					(void)reallocstring(&condistrules[index], rule, el_tempcluster);
//					(void)reallocstring(&uncondistrules[index], rule, el_tempcluster);
//					break;
//				case SPACINGM:
//					condistM[index] = uncondistM[index] = distance;
//					(void)reallocstring(&condistMrules[index], rule, el_tempcluster);
//					(void)reallocstring(&uncondistMrules[index], rule, el_tempcluster);
//					break;
//				case SPACINGW:
//					condistW[index] = uncondistW[index] = distance;
//					(void)reallocstring(&condistWrules[index], rule, el_tempcluster);
//					(void)reallocstring(&uncondistWrules[index], rule, el_tempcluster);
//					break;
//				case SPACINGE:
//					edgedist[index] = distance;
//					(void)reallocstring(&edgedistrules[index], rule, el_tempcluster);
//					break;
//				case CONSPA:
//					condist[index] = distance;
//					(void)reallocstring(&condistrules[index], rule, el_tempcluster);
//					break;
//				case UCONSPA:
//					uncondist[index] = distance;
//					(void)reallocstring(&uncondistrules[index], rule, el_tempcluster);
//					break;
//				case CUTSPA:
//					nty->f4 = (INTSML)distance;
//					break;
//				case CUTSIZE:
//					nty->f1 = nty->f2 = (INTSML)distance;
//					break;
//				case CUTSUR:
//					nty->f3 = (INTSML)distance;
//					break;
//			}
//		}
//	}
//	if (!errorfound)
//	{
//		/* clear the rules on the technology */
//		changesquiet(TRUE);
//		totarraybits = VDONTSAVE|VISARRAY|(tot << VLENGTHSH);
//		if (setvalkey((INTBIG)tech, VTECHNOLOGY, dr_connected_distanceskey,
//			(INTBIG)condist, VFRACT|totarraybits) == NOVARIABLE) return(TRUE);
//		if (setvalkey((INTBIG)tech, VTECHNOLOGY, dr_connected_distances_rulekey,
//			(INTBIG)condistrules, VSTRING|totarraybits) == NOVARIABLE) return(TRUE);
//		if (setvalkey((INTBIG)tech, VTECHNOLOGY, dr_unconnected_distanceskey,
//			(INTBIG)uncondist, VFRACT|totarraybits) == NOVARIABLE) return(TRUE);
//		if (setvalkey((INTBIG)tech, VTECHNOLOGY, dr_unconnected_distances_rulekey,
//			(INTBIG)uncondistrules, VSTRING|totarraybits) == NOVARIABLE) return(TRUE);
//		if (setvalkey((INTBIG)tech, VTECHNOLOGY, dr_connected_distancesWkey,
//			(INTBIG)condistW, VFRACT|totarraybits) == NOVARIABLE) return(TRUE);
//		if (setvalkey((INTBIG)tech, VTECHNOLOGY, dr_connected_distancesW_rulekey,
//			(INTBIG)condistWrules, VSTRING|totarraybits) == NOVARIABLE) return(TRUE);
//		if (setvalkey((INTBIG)tech, VTECHNOLOGY, dr_unconnected_distancesWkey,
//			(INTBIG)uncondistW, VFRACT|totarraybits) == NOVARIABLE) return(TRUE);
//		if (setvalkey((INTBIG)tech, VTECHNOLOGY, dr_unconnected_distancesW_rulekey,
//			(INTBIG)uncondistWrules, VSTRING|totarraybits) == NOVARIABLE) return(TRUE);
//		if (setvalkey((INTBIG)tech, VTECHNOLOGY, dr_connected_distancesMkey,
//			(INTBIG)condistM, VFRACT|totarraybits) == NOVARIABLE) return(TRUE);
//		if (setvalkey((INTBIG)tech, VTECHNOLOGY, dr_connected_distancesM_rulekey,
//			(INTBIG)condistMrules, VSTRING|totarraybits) == NOVARIABLE) return(TRUE);
//		if (setvalkey((INTBIG)tech, VTECHNOLOGY, dr_unconnected_distancesMkey,
//			(INTBIG)uncondistM, VFRACT|totarraybits) == NOVARIABLE) return(TRUE);
//		if (setvalkey((INTBIG)tech, VTECHNOLOGY, dr_unconnected_distancesM_rulekey,
//			(INTBIG)uncondistMrules, VSTRING|totarraybits) == NOVARIABLE) return(TRUE);
//		if (setvalkey((INTBIG)tech, VTECHNOLOGY, dr_edge_distanceskey,
//			(INTBIG)edgedist, VFRACT|totarraybits) == NOVARIABLE) return(TRUE);
//		if (setvalkey((INTBIG)tech, VTECHNOLOGY, dr_edge_distances_rulekey,
//			(INTBIG)edgedistrules, VSTRING|totarraybits) == NOVARIABLE) return(TRUE);
//		if (setvalkey((INTBIG)tech, VTECHNOLOGY, dr_wide_limitkey,
//			WIDELIMIT, VFRACT|VDONTSAVE) == NOVARIABLE) return(TRUE);
//
//		/* clear minimum size rules */
//		if (setvalkey((INTBIG)tech, VTECHNOLOGY, dr_min_widthkey,
//			(INTBIG)minsize, VFRACT|VDONTSAVE|VISARRAY|(MAXLAYERS<<VLENGTHSH)) == NOVARIABLE) return(TRUE);
//		if (setvalkey((INTBIG)tech, VTECHNOLOGY, dr_min_width_rulekey,
//			(INTBIG)minsizerules, VSTRING|VDONTSAVE|VISARRAY|(MAXLAYERS<<VLENGTHSH)) == NOVARIABLE) return(TRUE);
//		changesquiet(FALSE);
//
//		/* reset valid DRC dates */
//		dr_reset_dates();
//	}
//
//	/* free rule arrays */
//	/* clear all rules */
//	for(i=0; i<tot; i++)
//	{
//		efree((CHAR *)condistrules[i]);
//		efree((CHAR *)uncondistrules[i]);
//		efree((CHAR *)condistWrules[i]);
//		efree((CHAR *)uncondistWrules[i]);
//		efree((CHAR *)condistMrules[i]);
//		efree((CHAR *)uncondistMrules[i]);
//		efree((CHAR *)edgedistrules[i]);
//	}
//	for(i=0; i<MAXLAYERS; i++)
//	{
//		efree((CHAR *)minsizerules[i]);
//	}
//	efree((CHAR *)condist);
//	efree((CHAR *)uncondist);
//	efree((CHAR *)condistW);
//	efree((CHAR *)uncondistW);
//	efree((CHAR *)condistM);
//	efree((CHAR *)uncondistM);
//	efree((CHAR *)edgedist);
//	efree((CHAR *)condistrules);
//	efree((CHAR *)uncondistrules);
//	efree((CHAR *)condistWrules);
//	efree((CHAR *)uncondistWrules);
//	efree((CHAR *)condistMrules);
//	efree((CHAR *)uncondistMrules);
//	efree((CHAR *)edgedistrules);
//	efree((CHAR *)minsize);
//	efree((CHAR *)minsizerules);
//	return(errorfound);
//}
//
///*
// * Routine to describe the technology when it is in state "state".
// */
//CHAR *describestate(INTBIG state)
//{
//	REGISTER INTBIG nummetals, numpolys;
//	REGISTER CHAR *rules;
//	REGISTER void *infstr;
//
//	infstr = initinfstr();
//	switch (state&MOCMOSMETALS)
//	{
//		case MOCMOS2METAL: nummetals = 2;   break;
//		case MOCMOS3METAL: nummetals = 3;   break;
//		case MOCMOS4METAL: nummetals = 4;   break;
//		case MOCMOS5METAL: nummetals = 5;   break;
//		case MOCMOS6METAL: nummetals = 6;   break;
//		default: nummetals = 2;
//	}
//	switch (state&MOCMOSRULESET)
//	{
//		case MOCMOSSCMOSRULES:
//			rules = _("now standard");
//			break;
//		case MOCMOSDEEPRULES:
//			rules = _("now deep");
//			break;
//		case MOCMOSSUBMRULES:
//			rules = _("now submicron");
//			break;
//		default:
//			rules = 0;
//	}
//	if ((state&MOCMOSTWOPOLY) != 0) numpolys = 2; else
//		numpolys = 1;
//	formatinfstr(infstr, _("Complementary MOS (from MOSIS, 2-6 metals [now %ld], 1-2 polys [now %ld], flex rules [%s]"),
//		nummetals, numpolys, rules);
//	if ((state&MOCMOSSTICKFIGURE) != 0) addstringtoinfstr(infstr, _(", stick-figures"));
//	if ((state&MOCMOSNOSTACKEDVIAS) != 0) addstringtoinfstr(infstr, _(", stacked vias disallowed"));
//	if ((state&MOCMOSALTAPRULES) != 0) addstringtoinfstr(infstr, _(", alternate contact rules"));
//	if ((state&MOCMOSSCALABLETRAN) != 0) addstringtoinfstr(infstr, _(", shows scalable transistors"));
//	addstringtoinfstr(infstr, x_(")"));
//	return(returninfstr(infstr));
//}
//
///*
// * Routine to implement rule 3.3 which specifies the amount of poly overhang
// * on a transistor.
// */
//void settransistorpolyoverhang(INTBIG overhang)
//{
//	/* define the poly box in terms of the central transistor box */
//	trpbox[1] = trp1box[1] = trpobox[1] - overhang;
//	trpbox[5] = trp2box[5] = trpobox[5] + overhang;
//
//	/* the serpentine poly overhang */
//	tpa_l[TRANSPOLYLAYER].extendt = tpa_l[TRANSPOLYLAYER].extendb = (INTSML)overhang;
//	tpaE_l[TRANSEPOLY1LAYER].extendb = (INTSML)overhang;
//	tpaE_l[TRANSEPOLY2LAYER].extendt = (INTSML)overhang;
//	tna_l[TRANSPOLYLAYER].extendt = tna_l[TRANSPOLYLAYER].extendb = (INTSML)overhang;
//	tnaE_l[TRANSEPOLY1LAYER].extendb = (INTSML)overhang;
//	tnaE_l[TRANSEPOLY2LAYER].extendt = (INTSML)overhang;
//}
//
///*
// * Routine to implement rule 3.4 which specifies the amount of active overhang
// * on a transistor.
// */
//void settransistoractiveoverhang(INTBIG overhang)
//{
//	INTBIG polywidth, welloverhang;
//
//	/* pickup extension of well about active (2.3) */
//	welloverhang = trabox[1] - trwbox[1];
//
//	/* define the active box in terms of the central transistor box */
//	trabox[3] = trpobox[3] - overhang;
//	trabox[7] = trpobox[7] + overhang;
//	tra1box[7] = trpobox[7] + overhang;
//	tra2box[3] = trpobox[3] - overhang;
//
//	/* extension of well about active (2.3) */
//	trwbox[3] = trabox[3] - welloverhang;
//	trwbox[7] = trabox[7] + welloverhang;
//
//	/* extension of select about active = 2 (4.2) */
//	trsbox[3] = trabox[3] - K2;
//	trsbox[7] = trabox[7] + K2;
//
//	/* the serpentine active overhang */
//	polywidth = tpa.ysize/2 - trpobox[3];
//	tpa_l[TRANSACTLAYER].lwidth = (INTSML)(polywidth + overhang);
//	tpa_l[TRANSACTLAYER].rwidth = (INTSML)(polywidth + overhang);
//	tpaE_l[TRANSEACT1LAYER].lwidth = (INTSML)(polywidth + overhang);
//	tpaE_l[TRANSEACT2LAYER].rwidth = (INTSML)(polywidth + overhang);
//	tna_l[TRANSACTLAYER].lwidth = (INTSML)(polywidth + overhang);
//	tna_l[TRANSACTLAYER].rwidth = (INTSML)(polywidth + overhang);
//	tnaE_l[TRANSEACT1LAYER].lwidth = (INTSML)(polywidth + overhang);
//	tnaE_l[TRANSEACT2LAYER].rwidth = (INTSML)(polywidth + overhang);
//
//	/* serpentine: extension of well about active (2.3) */
//	tpa_l[TRANSWELLLAYER].lwidth = (INTSML)(polywidth + overhang + welloverhang);
//	tpa_l[TRANSWELLLAYER].rwidth = (INTSML)(polywidth + overhang + welloverhang);
//	tpaE_l[TRANSEWELLLAYER].lwidth = (INTSML)(polywidth + overhang + welloverhang);
//	tpaE_l[TRANSEWELLLAYER].rwidth = (INTSML)(polywidth + overhang + welloverhang);
//	tna_l[TRANSWELLLAYER].lwidth = (INTSML)(polywidth + overhang + welloverhang);
//	tna_l[TRANSWELLLAYER].rwidth = (INTSML)(polywidth + overhang + welloverhang);
//	tnaE_l[TRANSEWELLLAYER].lwidth = (INTSML)(polywidth + overhang + welloverhang);
//	tnaE_l[TRANSEWELLLAYER].rwidth = (INTSML)(polywidth + overhang + welloverhang);
//
//	/* serpentine: extension of select about active = 2 (4.2) */
//	tpa_l[TRANSSELECTLAYER].lwidth = (INTSML)(polywidth + overhang + K2);
//	tpa_l[TRANSSELECTLAYER].rwidth = (INTSML)(polywidth + overhang + K2);
//	tpaE_l[TRANSESELECTLAYER].lwidth = (INTSML)(polywidth + overhang + K2);
//	tpaE_l[TRANSESELECTLAYER].rwidth = (INTSML)(polywidth + overhang + K2);
//	tna_l[TRANSSELECTLAYER].lwidth = (INTSML)(polywidth + overhang + K2);
//	tna_l[TRANSSELECTLAYER].rwidth = (INTSML)(polywidth + overhang + K2);
//	tnaE_l[TRANSESELECTLAYER].lwidth = (INTSML)(polywidth + overhang + K2);
//	tnaE_l[TRANSESELECTLAYER].rwidth = (INTSML)(polywidth + overhang + K2);
//}
//
///*
// * Routine to implement rule 2.3 which specifies the amount of well surround
// * about active on a transistor.
// */
//void settransistorwellsurround(INTBIG overhang)
//{
//	/* define the well box in terms of the active box */
//	trwbox[1] = trabox[1] - overhang;
//	trwbox[3] = trabox[3] - overhang;
//	trwbox[5] = trabox[5] + overhang;
//	trwbox[7] = trabox[7] + overhang;
//
//	/* the serpentine poly overhang */
//	tpa_l[TRANSWELLLAYER].extendt = tpa_l[TRANSWELLLAYER].extendb = (INTSML)overhang;
//	tpa_l[TRANSWELLLAYER].lwidth = tpa_l[TRANSWELLLAYER].rwidth = (INTSML)(overhang+K4);
//	tpaE_l[TRANSEWELLLAYER].extendt = tpaE_l[TRANSEWELLLAYER].extendb = (INTSML)overhang;
//	tpaE_l[TRANSEWELLLAYER].lwidth = tpaE_l[TRANSEWELLLAYER].rwidth = (INTSML)(overhang+K4);
//	tna_l[TRANSWELLLAYER].extendt = tna_l[TRANSWELLLAYER].extendb = (INTSML)overhang;
//	tna_l[TRANSWELLLAYER].lwidth = tna_l[TRANSWELLLAYER].rwidth = (INTSML)(overhang+K4);
//	tnaE_l[TRANSEWELLLAYER].extendt = tnaE_l[TRANSEWELLLAYER].extendb = (INTSML)overhang;
//	tnaE_l[TRANSEWELLLAYER].lwidth = tnaE_l[TRANSEWELLLAYER].rwidth = (INTSML)(overhang+K4);
//}
//
///*
// * Routine to change the design rules for layer "layername" layers so that
// * the layers are at least "width" wide.  Affects the default arc width
// * and the default pin size.
// */
//void setlayerminwidth(CHAR *layername, INTBIG width)
//{
//	REGISTER ARCPROTO *ap;
//	REGISTER INTBIG lambda, i;
//	INTBIG lx, hx, ly, hy;
//	REGISTER NODEPROTO *np;
//	REGISTER TECH_NODES *nty;
//	REGISTER TECH_PORTS *npp;
//	REGISTER CHAR *pt;
//	REGISTER void *infstr;
//
//	/* next find that arc and set its default width */
//	for(ap = tech->firstarcproto; ap != NOARCPROTO; ap = ap->nextarcproto)
//		if (namesame(ap->protoname, layername) == 0) break;
//	if (ap == NOARCPROTO) return;
//	lambda = el_curlib->lambda[tech->techindex];
//	ap->nominalwidth = (width + arc_widoff[ap->arcindex]) * lambda / WHOLE;
//
//	/* finally, find that arc's pin and set its size and port offset */
//	infstr = initinfstr();
//	formatinfstr(infstr, x_("%s-Pin"), layername);
//	pt = returninfstr(infstr);
//	np = NONODEPROTO;
//	nty = 0;
//	for(i=0; i<tech->nodeprotocount; i++)
//	{
//		nty = tech->nodeprotos[i];
//		np = nty->creation;
//		if (namesame(np->protoname, pt) == 0) break;
//	}
//	if (np != NONODEPROTO)
//	{
//		nodeprotosizeoffset(np, &lx, &ly, &hx, &hy, NONODEPROTO);
//		lambda = el_curlib->lambda[tech->techindex];
//		np->lowx = -width * lambda / WHOLE / 2 - lx;
//		np->highx = width * lambda / WHOLE / 2 + hx;
//		np->lowy = -width * lambda / WHOLE / 2 - ly;
//		np->highy = width * lambda / WHOLE / 2 + hy;
//		npp = &nty->portlist[0];
//		npp->lowxsum = (INTSML)(width/2 + lx * WHOLE / lambda);
//		npp->lowysum = (INTSML)(width/2 + ly * WHOLE / lambda);
//		npp->highxsum = (INTSML)(-width/2 - hx * WHOLE / lambda);
//		npp->highysum = (INTSML)(-width/2 - hy * WHOLE / lambda);
//	}
//}
//
//INTBIG *offsetboxes[] =
//{
//	fullbox,
//	in0hbox,
//	in1box,
//	in1hbox,
//	in2box,
//	in2hbox,
//in3_2box,
//in3h_1hbox,
//	in3box,
//	in3hbox,
//	in4box,
//	in4hbox,
//	in5box,
//	in5hbox,
//	in6box,
//	in6hbox,
//	0
//};
//
///*
// * Routine to set the surround distance of layer "layer" from the via in node "nodename" to "surround".
// */
//void setlayersurroundvia(TECH_NODES *nty, INTBIG layer, INTBIG surround)
//{
//	REGISTER INTBIG i, j, viasize, layersize, indent;
//
//	/* find the via size */
//	viasize = nty->f1;
//	layersize = viasize + surround*2;
//	indent = (nty->xsize - layersize) / 2;
//	for(i=0; offsetboxes[i] != 0; i++)
//		if (offsetboxes[i][1] == indent) break;
//	if (offsetboxes[i] == 0)
//		ttyputerr(x_("MOSIS CMOS Submicron technology has no box that offsets by %s"), frtoa(indent)); else
//	{
//		for(j=0; j<nty->layercount; j++)
//			if (nty->layerlist[j].layernum == layer) break;
//		if (j >= nty->layercount) return;
//		nty->layerlist[j].points = offsetboxes[i];
//	}
//}
//
///*
// * Routine to set the surround distance of layer "outerlayer" from layer "innerlayer"
// * in node "nty" to "surround".  The array "minsize" is the minimum size of each layer.
// */
//void setlayersurroundlayer(TECH_NODES *nty, INTBIG outerlayer, INTBIG innerlayer, INTBIG surround, INTBIG *minsize)
//{
//	REGISTER INTBIG i, j, lxindent, hxindent, lyindent, hyindent, xsize, ysize;
//
//	/* find the inner layer */
//	for(j=0; j<nty->layercount; j++)
//		if (nty->layerlist[j].layernum == innerlayer) break;
//	if (j >= nty->layercount)
//	{
//		ttyputerr(x_("Internal error in MOCMOS surround computation"));
//		return;
//	}
//
//	/* find that layer in the specified node */
//	for(i=0; offsetboxes[i] != 0; i++)
//		if (offsetboxes[i] == nty->layerlist[j].points) break;
//	if (offsetboxes[i] == 0)
//	{
//		ttyputerr(x_("MOSIS CMOS Submicron technology cannot determine indentation of layer %ld on %s"),
//			innerlayer, nty->creation->protoname);
//		return;
//	}
//
//	/* determine if minimum size design rules are met */
//	lxindent = offsetboxes[i][1] - surround;
//	hxindent = -offsetboxes[i][5] - surround;
//	lyindent = offsetboxes[i][3] - surround;
//	hyindent = -offsetboxes[i][7] - surround;
//	xsize = nty->xsize - lxindent - hxindent;
//	ysize = nty->ysize - lyindent - hyindent;
//	if (xsize < minsize[outerlayer] || ysize < minsize[outerlayer])
//	{
//		/* make it irregular to force the proper minimum size */
//		if (xsize < minsize[outerlayer]) hxindent -= minsize[outerlayer] - xsize;
//		if (ysize < minsize[outerlayer]) hyindent -= minsize[outerlayer] - ysize;
//	}
//
//	/* find an appropriate descriptor for this surround amount */
//	for(i=0; offsetboxes[i] != 0; i++)
//		if (offsetboxes[i][1] == lxindent &&
//			offsetboxes[i][3] == lyindent &&
//			offsetboxes[i][5] == -hxindent &&
//			offsetboxes[i][7] == -hyindent) break;
//	if (offsetboxes[i] == 0)
//	{
//		ttyputerr(x_("MOSIS CMOS Submicron technology has no box that offsets lx=%s hx=%s ly=%s hy=%s"),
//			frtoa(lxindent), frtoa(hxindent), frtoa(lyindent), frtoa(hyindent));
//		return;
//	}
//
//	/* find the outer layer and set that size */
//	for(j=0; j<nty->layercount; j++)
//		if (nty->layerlist[j].layernum == outerlayer) break;
//	if (j >= nty->layercount)
//	{
//		ttyputerr(x_("Internal error in MOCMOS surround computation"));
//		return;
//	}
//	nty->layerlist[j].points = offsetboxes[i];
//}
//
///*
// * Routine to set the surround distance of layer "outerlayer" from layer "innerlayer"
// * in arc "aty" to "surround".
// */
//void setarclayersurroundlayer(TECH_ARCS *aty, INTBIG outerlayer, INTBIG innerlayer, INTBIG surround)
//{
//	REGISTER INTBIG j, indent;
//
//	/* find the inner layer */
//	for(j=0; j<aty->laycount; j++)
//		if (aty->list[j].lay == innerlayer) break;
//	if (j >= aty->laycount)
//	{
//		ttyputerr(x_("Internal error in MOCMOS surround computation"));
//		return;
//	}
//
//	indent = aty->list[j].off - surround*2;
//
//	for(j=0; j<aty->laycount; j++)
//		if (aty->list[j].lay == outerlayer) break;
//	if (j >= aty->laycount)
//	{
//		ttyputerr(x_("Internal error in MOCMOS surround computation"));
//		return;
//	}
//	aty->list[j].off = indent;
//}
//
///*
// * Routine to set the true node size (the highlighted area) of node "nodename" to "wid" x "hei".
// */
//void setdefnodesize(TECH_NODES *nty, INTBIG index, INTBIG wid, INTBIG hei)
//{
//	REGISTER INTBIG xindent, yindent;
//	REGISTER VARIABLE *var;
//
//	xindent = (nty->xsize - wid) / 2;
//	yindent = (nty->ysize - hei) / 2;
//
//	var = getval((INTBIG)tech, VTECHNOLOGY, VFRACT|VISARRAY, x_("TECH_node_width_offset"));
//	if (var != NOVARIABLE)
//	{
//		((INTBIG *)var->addr)[index*4] = xindent;
//		((INTBIG *)var->addr)[index*4+1] = xindent;
//		((INTBIG *)var->addr)[index*4+2] = yindent;
//		((INTBIG *)var->addr)[index*4+3] = yindent;
//	}
//}
}
