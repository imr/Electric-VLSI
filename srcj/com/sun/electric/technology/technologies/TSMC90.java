/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: TSMC90.java
 *
 * Copyright (c) 2004 Sun Microsystems
 */
package com.sun.electric.technology.technologies;

import com.sun.electric.database.geometry.EGraphics;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitiveArc;
import com.sun.electric.technology.PrimitivePort;
import com.sun.electric.technology.EdgeH;
import com.sun.electric.technology.EdgeV;
import com.sun.electric.technology.SizeOffset;
import com.sun.electric.tool.drc.DRC;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.ui.EditWindow;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.Iterator;
import java.util.prefs.Preferences;

/**
 * This is the TSMC90 CMOS technology.
 */
public class TSMC90 extends Technology
{
	/** the TSMC90 Technology object. */	public static final TSMC90 tech = new TSMC90();

	private TSMC90()
	{
		setTechName("tsmc90");
		setTechShortName("TSMC90 CMOS");
		setTechDesc("TSMC 90-nm CMOS");
		setFactoryScale(100, true);			// in nanometers: really 0.1 micron
		setNoNegatedArcs();
		setStaticTechnology();
		setFactoryTransparentLayers(new Color []
		{
			new Color( 96,209,255), // Metal-1
			new Color(255,155,192), // Polysilicon
			new Color(107,226, 96), // Active
			new Color(224, 95,255), // Metal-2
			new Color(247,251, 20)  // Metal-3
		});

		//**************************************** LAYERS ****************************************

		/** metal-1 layer */
		Layer metal1_lay = Layer.newInstance(this, "Metal-1",
			new EGraphics(EGraphics.SOLID, EGraphics.PATTERNED, EGraphics.TRANSPARENT_1, 96,209,255, 0.8,1,
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
			new EGraphics(EGraphics.SOLID, EGraphics.PATTERNED, EGraphics.TRANSPARENT_4, 224,95,255, 0.7,1,
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
			new EGraphics(EGraphics.SOLID, EGraphics.PATTERNED, EGraphics.TRANSPARENT_5, 247,251,20, 0.6,1,
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
			new EGraphics(EGraphics.PATTERNED, EGraphics.PATTERNED, 0, 150,150,255, 0.5,1,
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
			new EGraphics(EGraphics.OUTLINEPAT, EGraphics.OUTLINEPAT, 0, 255,190,6, 0.4,1,
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
			new EGraphics(EGraphics.PATTERNED, EGraphics.PATTERNED, 0, 0,255,255, 0.3,1,
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

		/** metal-7 layer */
		Layer metal7_lay = Layer.newInstance(this, "Metal-7",
			new EGraphics(EGraphics.PATTERNED, EGraphics.PATTERNED, 0, 255,0,0, 0.3,1,
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

		/** metal-8 layer */
		Layer metal8_lay = Layer.newInstance(this, "Metal-8",
			new EGraphics(EGraphics.PATTERNED, EGraphics.PATTERNED, 0, 0,0,255, 0.3,1,
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

		/** metal-9 layer */
		Layer metal9_lay = Layer.newInstance(this, "Metal-9",
			new EGraphics(EGraphics.PATTERNED, EGraphics.PATTERNED, 0, 0,255,0, 0.3,1,
			new int[] { 0x1010,   //    X       X    
						0x2828,   //   X X     X X   
						0x4444,   //  X   X   X   X  
						0x8282,   // X     X X     X 
						0x0101,   //        X       X
						0x8282,   // X     X X     X 
						0x4444,   //  X   X   X   X  
						0x2828,   //   X X     X X   
						0x1010,   //    X       X    
						0x2828,   //   X X     X X   
						0x4444,   //  X   X   X   X  
						0x8282,   // X     X X     X 
						0x0101,   //        X       X
						0x8282,   // X     X X     X 
						0x4444,   //  X   X   X   X  
						0x2828}));//   X X     X X   

		/** poly layer */
		Layer poly_lay = Layer.newInstance(this, "Polysilicon",
			new EGraphics(EGraphics.SOLID, EGraphics.PATTERNED, EGraphics.TRANSPARENT_2, 255,155,192, 0.5,1,
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

		/** P active layer */
		Layer pActive_lay = Layer.newInstance(this, "P-Active",
			new EGraphics(EGraphics.SOLID, EGraphics.PATTERNED, EGraphics.TRANSPARENT_3, 107,226,96, 0.5,1,
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
			new EGraphics(EGraphics.SOLID, EGraphics.PATTERNED, EGraphics.TRANSPARENT_3, 107,226,96, 0.5,1,
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
			new EGraphics(EGraphics.PATTERNED, EGraphics.PATTERNED, 0, 255,255,0, 0.2,0,
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
			new EGraphics(EGraphics.PATTERNED, EGraphics.PATTERNED, 0, 255,255,0, 0.2,0,
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
			new EGraphics(EGraphics.PATTERNED, EGraphics.PATTERNED, 0, 139,99,46, 0.2,0,
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
			new EGraphics(EGraphics.PATTERNED, EGraphics.PATTERNED, 0, 139,99,46, 0.2,0,
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
			new EGraphics(EGraphics.SOLID, EGraphics.SOLID, 0, 100,100,100, 1.0,1,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** active cut layer */
		Layer activeCut_lay = Layer.newInstance(this, "Active-Cut",
			new EGraphics(EGraphics.SOLID, EGraphics.SOLID, 0, 100,100,100, 1.0,1,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** via1 layer */
		Layer via1_lay = Layer.newInstance(this, "Via1", 
			new EGraphics(EGraphics.SOLID, EGraphics.SOLID, 0, 180,180,180, 1.0,1,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** via2 layer */
		Layer via2_lay = Layer.newInstance(this, "Via2",
			new EGraphics(EGraphics.SOLID, EGraphics.SOLID, 0, 180,180,180, 1.0,1,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** via3 layer */
		Layer via3_lay = Layer.newInstance(this, "Via3",
			new EGraphics(EGraphics.SOLID, EGraphics.SOLID, 0, 180,180,180, 1.0,1,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** via4 layer */
		Layer via4_lay = Layer.newInstance(this, "Via4",
			new EGraphics(EGraphics.SOLID, EGraphics.SOLID, 0, 180,180,180, 1.0,1,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** via5 layer */
		Layer via5_lay = Layer.newInstance(this, "Via5",
			new EGraphics(EGraphics.SOLID, EGraphics.SOLID, 0, 180,180,180, 1.0,1,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** via6 layer */
		Layer via6_lay = Layer.newInstance(this, "Via6",
			new EGraphics(EGraphics.SOLID, EGraphics.SOLID, 0, 180,180,180, 1.0,1,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** via7 layer */
		Layer via7_lay = Layer.newInstance(this, "Via7",
			new EGraphics(EGraphics.SOLID, EGraphics.SOLID, 0, 180,180,180, 1.0,1,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** via8 layer */
		Layer via8_lay = Layer.newInstance(this, "Via8",
			new EGraphics(EGraphics.SOLID, EGraphics.SOLID, 0, 180,180,180, 1.0,1,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** passivation layer */
		Layer passivation_lay = Layer.newInstance(this, "Passivation",
			new EGraphics(EGraphics.PATTERNED, EGraphics.PATTERNED, 0, 100,100,100, 1.0,1,
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
			new EGraphics(EGraphics.SOLID, EGraphics.PATTERNED, EGraphics.TRANSPARENT_2, 255,155,192, 0.5,1,
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

		/** P act well layer */
		Layer pActiveWell_lay = Layer.newInstance(this, "P-Active-Well",
			new EGraphics(EGraphics.SOLID, EGraphics.PATTERNED, EGraphics.TRANSPARENT_3, 107,226,96, 1.0,0,
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
			new EGraphics(EGraphics.PATTERNED, EGraphics.PATTERNED, 0, 230,230,230, 1.0,0,
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
			new EGraphics(EGraphics.SOLID, EGraphics.PATTERNED, EGraphics.TRANSPARENT_1, 96,209,255, 0.8,1,
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
			new EGraphics(EGraphics.SOLID, EGraphics.PATTERNED, EGraphics.TRANSPARENT_4, 224,95,255, 0.7,1,
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
			new EGraphics(EGraphics.SOLID, EGraphics.PATTERNED, EGraphics.TRANSPARENT_5, 247,251,20, 0.6,1,
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
			new EGraphics(EGraphics.PATTERNED, EGraphics.PATTERNED, 0, 150,150,255, 0.5,1,
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
			new EGraphics(EGraphics.OUTLINEPAT, EGraphics.OUTLINEPAT, 0, 255,190,6, 0.4,1,
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
			new EGraphics(EGraphics.PATTERNED, EGraphics.PATTERNED, 0, 0,255,255, 0.3,1,
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

		/** pseudo metal-7 */
		Layer pseudoMetal7_lay = Layer.newInstance(this, "Pseudo-Metal-7",
			new EGraphics(EGraphics.PATTERNED, EGraphics.PATTERNED, 0, 255,0,0, 0.3,1,
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

		/** pseudo metal-8 */
		Layer pseudoMetal8_lay = Layer.newInstance(this, "Pseudo-Metal-8",
			new EGraphics(EGraphics.PATTERNED, EGraphics.PATTERNED, 0, 0,0,255, 0.3,1,
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

		/** pseudo metal-9 */
		Layer pseudoMetal9_lay = Layer.newInstance(this, "Pseudo-Metal-9",
			new EGraphics(EGraphics.PATTERNED, EGraphics.PATTERNED, 0, 0,255,0, 0.3,1,
			new int[] { 0x1010,   //    X       X    
						0x2828,   //   X X     X X   
						0x4444,   //  X   X   X   X  
						0x8282,   // X     X X     X 
						0x0101,   //        X       X
						0x8282,   // X     X X     X 
						0x4444,   //  X   X   X   X  
						0x2828,   //   X X     X X   
						0x1010,   //    X       X    
						0x2828,   //   X X     X X   
						0x4444,   //  X   X   X   X  
						0x8282,   // X     X X     X 
						0x0101,   //        X       X
						0x8282,   // X     X X     X 
						0x4444,   //  X   X   X   X  
						0x2828}));//   X X     X X   

		/** pseudo poly layer */
		Layer pseudoPoly_lay = Layer.newInstance(this, "Pseudo-Polysilicon",
			new EGraphics(EGraphics.SOLID, EGraphics.PATTERNED, EGraphics.TRANSPARENT_2, 255,155,192, 1.0,1,
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

		/** pseudo P active */
		Layer pseudoPActive_lay = Layer.newInstance(this, "Pseudo-P-Active",
			new EGraphics(EGraphics.SOLID, EGraphics.PATTERNED, EGraphics.TRANSPARENT_3, 107,226,96, 1.0,1,
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
			new EGraphics(EGraphics.SOLID, EGraphics.PATTERNED, EGraphics.TRANSPARENT_3, 107,226,96, 1.0,1,
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
			new EGraphics(EGraphics.PATTERNED, EGraphics.PATTERNED, 0, 255,255,0, 1.0,0,
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
			new EGraphics(EGraphics.PATTERNED, EGraphics.PATTERNED, 0, 255,255,0, 1.0,0,
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
			new EGraphics(EGraphics.PATTERNED, EGraphics.PATTERNED, 0, 139,99,46, 1.0,0,
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
			new EGraphics(EGraphics.PATTERNED, EGraphics.PATTERNED, 0, 139,99,46, 1.0,0,
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
			new EGraphics(EGraphics.SOLID, EGraphics.PATTERNED, 0, 255,0,0, 1.0,0,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		// The layer functions
		metal1_lay.setFunction(Layer.Function.METAL1);									// Metal-1
		metal2_lay.setFunction(Layer.Function.METAL2);									// Metal-2
		metal3_lay.setFunction(Layer.Function.METAL3);									// Metal-3
		metal4_lay.setFunction(Layer.Function.METAL4);									// Metal-4
		metal5_lay.setFunction(Layer.Function.METAL5);									// Metal-5
		metal6_lay.setFunction(Layer.Function.METAL6);									// Metal-6
		metal7_lay.setFunction(Layer.Function.METAL7);									// Metal-7
		metal8_lay.setFunction(Layer.Function.METAL8);									// Metal-8
		metal9_lay.setFunction(Layer.Function.METAL9);									// Metal-9
		poly_lay.setFunction(Layer.Function.POLY1);										// Polysilicon
		pActive_lay.setFunction(Layer.Function.DIFFP);									// P-Active
		nActive_lay.setFunction(Layer.Function.DIFFN);									// N-Active
		pSelect_lay.setFunction(Layer.Function.IMPLANTP);								// P-Select
		nSelect_lay.setFunction(Layer.Function.IMPLANTN);								// N-Select
		pWell_lay.setFunction(Layer.Function.WELLP);									// P-Well
		nWell_lay.setFunction(Layer.Function.WELLN);									// N-Well
		polyCut_lay.setFunction(Layer.Function.CONTACT1, Layer.Function.CONPOLY);		// Poly-Cut
		activeCut_lay.setFunction(Layer.Function.CONTACT1, Layer.Function.CONDIFF);		// Active-Cut
		via1_lay.setFunction(Layer.Function.CONTACT2, Layer.Function.CONMETAL);			// Via-1
		via2_lay.setFunction(Layer.Function.CONTACT3, Layer.Function.CONMETAL);			// Via-2
		via3_lay.setFunction(Layer.Function.CONTACT4, Layer.Function.CONMETAL);			// Via-3
		via4_lay.setFunction(Layer.Function.CONTACT5, Layer.Function.CONMETAL);			// Via-4
		via5_lay.setFunction(Layer.Function.CONTACT6, Layer.Function.CONMETAL);			// Via-5
		passivation_lay.setFunction(Layer.Function.OVERGLASS);							// Passivation
		transistorPoly_lay.setFunction(Layer.Function.GATE);							// Transistor-Poly
		pActiveWell_lay.setFunction(Layer.Function.DIFFP);								// P-Active-Well
		silicideBlock_lay.setFunction(Layer.Function.ART);								// Silicide-Block
		pseudoMetal1_lay.setFunction(Layer.Function.METAL1, Layer.Function.PSEUDO);		// Pseudo-Metal-1
		pseudoMetal2_lay.setFunction(Layer.Function.METAL2, Layer.Function.PSEUDO);		// Pseudo-Metal-2
		pseudoMetal3_lay.setFunction(Layer.Function.METAL3, Layer.Function.PSEUDO);		// Pseudo-Metal-3
		pseudoMetal4_lay.setFunction(Layer.Function.METAL4, Layer.Function.PSEUDO);		// Pseudo-Metal-4
		pseudoMetal5_lay.setFunction(Layer.Function.METAL5, Layer.Function.PSEUDO);		// Pseudo-Metal-5
		pseudoMetal6_lay.setFunction(Layer.Function.METAL6, Layer.Function.PSEUDO);		// Pseudo-Metal-6
		pseudoMetal7_lay.setFunction(Layer.Function.METAL7, Layer.Function.PSEUDO);		// Pseudo-Metal-7
		pseudoMetal8_lay.setFunction(Layer.Function.METAL8, Layer.Function.PSEUDO);		// Pseudo-Metal-8
		pseudoMetal9_lay.setFunction(Layer.Function.METAL9, Layer.Function.PSEUDO);		// Pseudo-Metal-9
		pseudoPoly_lay.setFunction(Layer.Function.POLY1, Layer.Function.PSEUDO);		// Pseudo-Polysilicon
		pseudoPActive_lay.setFunction(Layer.Function.DIFFP, Layer.Function.PSEUDO);		// Pseudo-P-Active
		pseudoNActive_lay.setFunction(Layer.Function.DIFFN, Layer.Function.PSEUDO);		// Pseudo-N-Active
		pseudoPSelect_lay.setFunction(Layer.Function.IMPLANTP, Layer.Function.PSEUDO);	// Pseudo-P-Select
		pseudoNSelect_lay.setFunction(Layer.Function.IMPLANTN, Layer.Function.PSEUDO);	// Pseudo-N-Select
		pseudoPWell_lay.setFunction(Layer.Function.WELLP, Layer.Function.PSEUDO);		// Pseudo-P-Well
		pseudoNWell_lay.setFunction(Layer.Function.WELLN, Layer.Function.PSEUDO);		// Pseudo-N-Well
		padFrame_lay.setFunction(Layer.Function.ART);									// Pad-Frame

		// The CIF names
		metal1_lay.setFactoryCIFLayer("CMF");				// Metal-1
		metal2_lay.setFactoryCIFLayer("CMS");				// Metal-2
		metal3_lay.setFactoryCIFLayer("CMT");				// Metal-3
		metal4_lay.setFactoryCIFLayer("CMQ");				// Metal-4
		metal5_lay.setFactoryCIFLayer("CMP");				// Metal-5
		metal6_lay.setFactoryCIFLayer("CM6");				// Metal-6
		metal7_lay.setFactoryCIFLayer("CM7");				// Metal-7
		metal8_lay.setFactoryCIFLayer("CM8");				// Metal-8
		metal9_lay.setFactoryCIFLayer("CM9");				// Metal-9
		poly_lay.setFactoryCIFLayer("CPG");					// Polysilicon
		pActive_lay.setFactoryCIFLayer("CAA");				// P-Active
		nActive_lay.setFactoryCIFLayer("CAA");				// N-Active
		pSelect_lay.setFactoryCIFLayer("CSP");				// P-Select
		nSelect_lay.setFactoryCIFLayer("CSN");				// N-Select
		pWell_lay.setFactoryCIFLayer("CWP");				// P-Well
		nWell_lay.setFactoryCIFLayer("CWN");				// N-Well
		polyCut_lay.setFactoryCIFLayer("CCC");				// Poly-Cut
		activeCut_lay.setFactoryCIFLayer("CCC");			// Active-Cut
		via1_lay.setFactoryCIFLayer("CVA");					// Via-1
		via2_lay.setFactoryCIFLayer("CVS");					// Via-2
		via3_lay.setFactoryCIFLayer("CVT");					// Via-3
		via4_lay.setFactoryCIFLayer("CVQ");					// Via-4
		via5_lay.setFactoryCIFLayer("CV5");					// Via-5
		passivation_lay.setFactoryCIFLayer("COG");			// Passivation
		transistorPoly_lay.setFactoryCIFLayer("CPG");		// Transistor-Poly
		pActiveWell_lay.setFactoryCIFLayer("CAA");			// P-Active-Well
		silicideBlock_lay.setFactoryCIFLayer("CSB");		// Silicide-Block
		pseudoMetal1_lay.setFactoryCIFLayer("");			// Pseudo-Metal-1
		pseudoMetal2_lay.setFactoryCIFLayer("");			// Pseudo-Metal-2
		pseudoMetal3_lay.setFactoryCIFLayer("");			// Pseudo-Metal-3
		pseudoMetal4_lay.setFactoryCIFLayer("");			// Pseudo-Metal-4
		pseudoMetal5_lay.setFactoryCIFLayer("");			// Pseudo-Metal-5
		pseudoMetal6_lay.setFactoryCIFLayer("");			// Pseudo-Metal-6
		pseudoMetal7_lay.setFactoryCIFLayer("");			// Pseudo-Metal-7
		pseudoMetal8_lay.setFactoryCIFLayer("");			// Pseudo-Metal-8
		pseudoMetal9_lay.setFactoryCIFLayer("");			// Pseudo-Metal-9
		pseudoPoly_lay.setFactoryCIFLayer("");				// Pseudo-Polysilicon
		pseudoPActive_lay.setFactoryCIFLayer("");			// Pseudo-P-Active
		pseudoNActive_lay.setFactoryCIFLayer("");			// Pseudo-N-Active
		pseudoPSelect_lay.setFactoryCIFLayer("CSP");		// Pseudo-P-Select
		pseudoNSelect_lay.setFactoryCIFLayer("CSN");		// Pseudo-N-Select
		pseudoPWell_lay.setFactoryCIFLayer("CWP");			// Pseudo-P-Well
		pseudoNWell_lay.setFactoryCIFLayer("CWN");			// Pseudo-N-Well
		padFrame_lay.setFactoryCIFLayer("XP");				// Pad-Frame

		// The GDS names
		metal1_lay.setFactoryGDSLayer("49");				// Metal-1
		metal2_lay.setFactoryGDSLayer("51");				// Metal-2
		metal3_lay.setFactoryGDSLayer("62");				// Metal-3
		metal4_lay.setFactoryGDSLayer("31");				// Metal-4
		metal5_lay.setFactoryGDSLayer("33");				// Metal-5
		metal6_lay.setFactoryGDSLayer("37");				// Metal-6
		metal7_lay.setFactoryGDSLayer("38");				// Metal-7
		metal8_lay.setFactoryGDSLayer("39");				// Metal-8
		metal9_lay.setFactoryGDSLayer("40");				// Metal-9
		poly_lay.setFactoryGDSLayer("46");					// Polysilicon
		pActive_lay.setFactoryGDSLayer("43");				// P-Active
		nActive_lay.setFactoryGDSLayer("43");				// N-Active
		pSelect_lay.setFactoryGDSLayer("44");				// P-Select
		nSelect_lay.setFactoryGDSLayer("45");				// N-Select
		pWell_lay.setFactoryGDSLayer("41");					// P-Well
		nWell_lay.setFactoryGDSLayer("42");					// N-Well
		polyCut_lay.setFactoryGDSLayer("47");				// Poly-Cut
		activeCut_lay.setFactoryGDSLayer("48");				// Active-Cut
		via1_lay.setFactoryGDSLayer("50");					// Via-1
		via2_lay.setFactoryGDSLayer("61");					// Via-2
		via3_lay.setFactoryGDSLayer("30");					// Via-3
		via4_lay.setFactoryGDSLayer("32");					// Via-4
		via5_lay.setFactoryGDSLayer("36");					// Via-5
		passivation_lay.setFactoryGDSLayer("52");			// Passivation
		transistorPoly_lay.setFactoryGDSLayer("46");		// Transistor-Poly
		pActiveWell_lay.setFactoryGDSLayer("43");			// P-Active-Well
		silicideBlock_lay.setFactoryGDSLayer("29");			// Silicide-Block
		pseudoMetal1_lay.setFactoryGDSLayer("");			// Pseudo-Metal-1
		pseudoMetal2_lay.setFactoryGDSLayer("");			// Pseudo-Metal-2
		pseudoMetal3_lay.setFactoryGDSLayer("");			// Pseudo-Metal-3
		pseudoMetal4_lay.setFactoryGDSLayer("");			// Pseudo-Metal-4
		pseudoMetal5_lay.setFactoryGDSLayer("");			// Pseudo-Metal-5
		pseudoMetal6_lay.setFactoryGDSLayer("");			// Pseudo-Metal-6
		pseudoMetal7_lay.setFactoryGDSLayer("");			// Pseudo-Metal-7
		pseudoMetal8_lay.setFactoryGDSLayer("");			// Pseudo-Metal-8
		pseudoMetal9_lay.setFactoryGDSLayer("");			// Pseudo-Metal-9
		pseudoPoly_lay.setFactoryGDSLayer("");				// Pseudo-Polysilicon
		pseudoPActive_lay.setFactoryGDSLayer("");			// Pseudo-P-Active
		pseudoNActive_lay.setFactoryGDSLayer("");			// Pseudo-N-Active
		pseudoPSelect_lay.setFactoryGDSLayer("");			// Pseudo-P-Select
		pseudoNSelect_lay.setFactoryGDSLayer("");			// Pseudo-N-Select
		pseudoPWell_lay.setFactoryGDSLayer("");				// Pseudo-P-Well
		pseudoNWell_lay.setFactoryGDSLayer("");				// Pseudo-N-Well
		padFrame_lay.setFactoryGDSLayer("26");				// Pad-Frame

		// The layer height
		metal1_lay.setFactory3DInfo(0.1, 17);				// Metal-1
		metal2_lay.setFactory3DInfo(0.2, 19);				// Metal-2
		metal3_lay.setFactory3DInfo(0.3, 21);				// Metal-3
		metal4_lay.setFactory3DInfo(0.4, 23);				// Metal-4
		metal5_lay.setFactory3DInfo(0.5, 25);				// Metal-5
		metal6_lay.setFactory3DInfo(0.6, 27);				// Metal-6
		metal7_lay.setFactory3DInfo(0.7, 29);				// Metal-7
		metal8_lay.setFactory3DInfo(0.8, 31);				// Metal-8
		metal9_lay.setFactory3DInfo(0.9, 33);				// Metal-9
		poly_lay.setFactory3DInfo(0.7, 15);					// Polysilicon
		pActive_lay.setFactory3DInfo(0.9, 13);				// P-Active
		nActive_lay.setFactory3DInfo(1.0, 13);				// N-Active
		pSelect_lay.setFactory3DInfo(0.1, 12);				// P-Select
		nSelect_lay.setFactory3DInfo(0.2, 12);				// N-Select
		pWell_lay.setFactory3DInfo(0.3, 11);				// P-Well
		nWell_lay.setFactory3DInfo(0.4, 11);				// N-Well
		polyCut_lay.setFactory3DInfo(2, 16);				// Poly-Cut
		activeCut_lay.setFactory3DInfo(4, 15);				// Active-Cut
		via1_lay.setFactory3DInfo(2, 18);					// Via-1
		via2_lay.setFactory3DInfo(2, 20);					// Via-2
		via3_lay.setFactory3DInfo(2, 22);					// Via-3
		via4_lay.setFactory3DInfo(2, 24);					// Via-4
		via5_lay.setFactory3DInfo(2, 26);					// Via-5
		passivation_lay.setFactory3DInfo(0, 30);			// Passivation
		transistorPoly_lay.setFactory3DInfo(0.5, 15);		// Transistor-Poly
		pActiveWell_lay.setFactory3DInfo(0, 13);			// P-Active-Well
		silicideBlock_lay.setFactory3DInfo(0, 10);			// Silicide-Block
		pseudoMetal1_lay.setFactory3DInfo(0, 17);			// Pseudo-Metal-1
		pseudoMetal2_lay.setFactory3DInfo(0, 19);			// Pseudo-Metal-2
		pseudoMetal3_lay.setFactory3DInfo(0, 21);			// Pseudo-Metal-3
		pseudoMetal4_lay.setFactory3DInfo(0, 23);			// Pseudo-Metal-4
		pseudoMetal5_lay.setFactory3DInfo(0, 25);			// Pseudo-Metal-5
		pseudoMetal6_lay.setFactory3DInfo(0, 27);			// Pseudo-Metal-6
		pseudoMetal7_lay.setFactory3DInfo(0, 29);			// Pseudo-Metal-7
		pseudoMetal8_lay.setFactory3DInfo(0, 31);			// Pseudo-Metal-8
		pseudoMetal9_lay.setFactory3DInfo(0, 33);			// Pseudo-Metal-9
		pseudoPoly_lay.setFactory3DInfo(0, 12);				// Pseudo-Polysilicon
		pseudoPActive_lay.setFactory3DInfo(0, 11);			// Pseudo-P-Active
		pseudoNActive_lay.setFactory3DInfo(0, 11);			// Pseudo-N-Active
		pseudoPSelect_lay.setFactory3DInfo(0, 2);			// Pseudo-P-Select
		pseudoNSelect_lay.setFactory3DInfo(0, 2);			// Pseudo-N-Select
		pseudoPWell_lay.setFactory3DInfo(0, 0);				// Pseudo-P-Well
		pseudoNWell_lay.setFactory3DInfo(0, 0);				// Pseudo-N-Well
		padFrame_lay.setFactory3DInfo(0, 33);				// Pad-Frame

		// The Spice parasitics
		metal1_lay.setFactoryParasitics(0.06, 0.07, 0);			// Metal-1
		metal2_lay.setFactoryParasitics(0.06, 0.04, 0);			// Metal-2
		metal3_lay.setFactoryParasitics(0.06, 0.04, 0);			// Metal-3
		metal4_lay.setFactoryParasitics(0.03, 0.04, 0);			// Metal-4
		metal5_lay.setFactoryParasitics(0.03, 0.04, 0);			// Metal-5
		metal6_lay.setFactoryParasitics(0.03, 0.04, 0);			// Metal-6
		metal7_lay.setFactoryParasitics(0.03, 0.04, 0);			// Metal-7
		metal8_lay.setFactoryParasitics(0.03, 0.04, 0);			// Metal-8
		metal9_lay.setFactoryParasitics(0.03, 0.04, 0);			// Metal-9
		poly_lay.setFactoryParasitics(2.5, 0.09, 0);			// Polysilicon
		pActive_lay.setFactoryParasitics(2.5, 0.9, 0);			// P-Active
		nActive_lay.setFactoryParasitics(3.0, 0.9, 0);			// N-Active
		pSelect_lay.setFactoryParasitics(0, 0, 0);				// P-Select
		nSelect_lay.setFactoryParasitics(0, 0, 0);				// N-Select
		pWell_lay.setFactoryParasitics(0, 0, 0);				// P-Well
		nWell_lay.setFactoryParasitics(0, 0, 0);				// N-Well
		polyCut_lay.setFactoryParasitics(2.2, 0, 0);			// Poly-Cut
		activeCut_lay.setFactoryParasitics(2.5, 0, 0);			// Active-Cut
		via1_lay.setFactoryParasitics(1.0, 0, 0);				// Via-1
		via2_lay.setFactoryParasitics(0.9, 0, 0);				// Via-2
		via3_lay.setFactoryParasitics(0.8, 0, 0);				// Via-3
		via4_lay.setFactoryParasitics(0.8, 0, 0);				// Via-4
		via5_lay.setFactoryParasitics(0.8, 0, 0);				// Via-5
		passivation_lay.setFactoryParasitics(0, 0, 0);			// Passivation
		transistorPoly_lay.setFactoryParasitics(2.5, 0.09, 0);	// Transistor-Poly
		pActiveWell_lay.setFactoryParasitics(0, 0, 0);			// P-Active-Well
		silicideBlock_lay.setFactoryParasitics(0, 0, 0);		// Silicide-Block
		pseudoMetal1_lay.setFactoryParasitics(0, 0, 0);			// Pseudo-Metal-1
		pseudoMetal2_lay.setFactoryParasitics(0, 0, 0);			// Pseudo-Metal-2
		pseudoMetal3_lay.setFactoryParasitics(0, 0, 0);			// Pseudo-Metal-3
		pseudoMetal4_lay.setFactoryParasitics(0, 0, 0);			// Pseudo-Metal-4
		pseudoMetal5_lay.setFactoryParasitics(0, 0, 0);			// Pseudo-Metal-5
		pseudoMetal6_lay.setFactoryParasitics(0, 0, 0);			// Pseudo-Metal-6
		pseudoMetal7_lay.setFactoryParasitics(0, 0, 0);			// Pseudo-Metal-7
		pseudoMetal8_lay.setFactoryParasitics(0, 0, 0);			// Pseudo-Metal-8
		pseudoMetal9_lay.setFactoryParasitics(0, 0, 0);			// Pseudo-Metal-9
		pseudoPoly_lay.setFactoryParasitics(0, 0, 0);			// Pseudo-Polysilicon
		pseudoPActive_lay.setFactoryParasitics(0, 0, 0);		// Pseudo-P-Active
		pseudoNActive_lay.setFactoryParasitics(0, 0, 0);		// Pseudo-N-Active
		pseudoPSelect_lay.setFactoryParasitics(0, 0, 0);		// Pseudo-P-Select
		pseudoNSelect_lay.setFactoryParasitics(0, 0, 0);		// Pseudo-N-Select
		pseudoPWell_lay.setFactoryParasitics(0, 0, 0);			// Pseudo-P-Well
		pseudoNWell_lay.setFactoryParasitics(0, 0, 0);			// Pseudo-N-Well
		padFrame_lay.setFactoryParasitics(0, 0, 0);				// Pad-Frame

		setFactoryParasitics(50, 0.04);

		//**************************************** ARCS ****************************************

		/** metal 1 arc */
		PrimitiveArc metal1_arc = PrimitiveArc.newInstance(this, "Metal-1", 1.2, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(metal1_lay, 0, Poly.Type.FILLED)
		});
		metal1_arc.setFunction(PrimitiveArc.Function.METAL1);
		metal1_arc.setFactoryFixedAngle(true);
		metal1_arc.setWipable();
		metal1_arc.setFactoryAngleIncrement(90);

		/** metal 2 arc */
		PrimitiveArc metal2_arc = PrimitiveArc.newInstance(this, "Metal-2", 1.4, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(metal2_lay, 0, Poly.Type.FILLED)
		});
		metal2_arc.setFunction(PrimitiveArc.Function.METAL2);
		metal2_arc.setFactoryFixedAngle(true);
		metal2_arc.setWipable();
		metal2_arc.setFactoryAngleIncrement(90);

		/** metal 3 arc */
		PrimitiveArc metal3_arc = PrimitiveArc.newInstance(this, "Metal-3", 1.4, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(metal3_lay, 0, Poly.Type.FILLED)
		});
		metal3_arc.setFunction(PrimitiveArc.Function.METAL3);
		metal3_arc.setFactoryFixedAngle(true);
		metal3_arc.setWipable();
		metal3_arc.setFactoryAngleIncrement(90);

		/** metal 4 arc */
		PrimitiveArc metal4_arc = PrimitiveArc.newInstance(this, "Metal-4", 1.4, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(metal4_lay, 0, Poly.Type.FILLED)
		});
		metal4_arc.setFunction(PrimitiveArc.Function.METAL4);
		metal4_arc.setFactoryFixedAngle(true);
		metal4_arc.setWipable();
		metal4_arc.setFactoryAngleIncrement(90);

		/** metal 5 arc */
		PrimitiveArc metal5_arc = PrimitiveArc.newInstance(this, "Metal-5", 1.4, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(metal5_lay, 0, Poly.Type.FILLED)
		});
		metal5_arc.setFunction(PrimitiveArc.Function.METAL5);
		metal5_arc.setFactoryFixedAngle(true);
		metal5_arc.setWipable();
		metal5_arc.setFactoryAngleIncrement(90);

		/** metal 6 arc */
		PrimitiveArc metal6_arc = PrimitiveArc.newInstance(this, "Metal-6", 1.4, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(metal6_lay, 0, Poly.Type.FILLED)
		});
		metal6_arc.setFunction(PrimitiveArc.Function.METAL6);
		metal6_arc.setFactoryFixedAngle(true);
		metal6_arc.setWipable();
		metal6_arc.setFactoryAngleIncrement(90);

		/** metal 7 arc */
		PrimitiveArc metal7_arc = PrimitiveArc.newInstance(this, "Metal-7", 1.4, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(metal7_lay, 0, Poly.Type.FILLED)
		});
		metal7_arc.setFunction(PrimitiveArc.Function.METAL7);
		metal7_arc.setFactoryFixedAngle(true);
		metal7_arc.setWipable();
		metal7_arc.setFactoryAngleIncrement(90);

		/** metal 8 arc */
		PrimitiveArc metal8_arc = PrimitiveArc.newInstance(this, "Metal-8", 4.2, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(metal8_lay, 0, Poly.Type.FILLED)
		});
		metal8_arc.setFunction(PrimitiveArc.Function.METAL8);
		metal8_arc.setFactoryFixedAngle(true);
		metal8_arc.setWipable();
		metal8_arc.setFactoryAngleIncrement(90);

		/** metal 9 arc */
		PrimitiveArc metal9_arc = PrimitiveArc.newInstance(this, "Metal-9", 4.2, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(metal9_lay, 0, Poly.Type.FILLED)
		});
		metal9_arc.setFunction(PrimitiveArc.Function.METAL9);
		metal9_arc.setFactoryFixedAngle(true);
		metal9_arc.setWipable();
		metal9_arc.setFactoryAngleIncrement(90);

		/** polysilicon arc */
		PrimitiveArc poly_arc = PrimitiveArc.newInstance(this, "Polysilicon", 1.0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(poly_lay, 0, Poly.Type.FILLED)
		});
		poly_arc.setFunction(PrimitiveArc.Function.POLY1);
		poly_arc.setFactoryFixedAngle(true);
		poly_arc.setWipable();
		poly_arc.setFactoryAngleIncrement(90);

		/** P-active arc */
		PrimitiveArc pActive_arc = PrimitiveArc.newInstance(this, "P-Active", 5.5, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(pActive_lay, 4.4, Poly.Type.FILLED),
			new Technology.ArcLayer(nWell_lay, 0, Poly.Type.FILLED),
			new Technology.ArcLayer(pSelect_lay, 1.8, Poly.Type.FILLED)
		});
		pActive_arc.setFunction(PrimitiveArc.Function.DIFFP);
		pActive_arc.setFactoryFixedAngle(true);
		pActive_arc.setWipable();
		pActive_arc.setFactoryAngleIncrement(90);
		pActive_arc.setWidthOffset(4.4);

		/** N-active arc */
		PrimitiveArc nActive_arc = PrimitiveArc.newInstance(this, "N-Active", 5.5, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(nActive_lay, 4.4, Poly.Type.FILLED),
			new Technology.ArcLayer(pWell_lay, 0, Poly.Type.FILLED),
			new Technology.ArcLayer(nSelect_lay, 1.8, Poly.Type.FILLED)
		});
		nActive_arc.setFunction(PrimitiveArc.Function.DIFFN);
		nActive_arc.setFactoryFixedAngle(true);
		nActive_arc.setWipable();
		nActive_arc.setFactoryAngleIncrement(90);
		nActive_arc.setWidthOffset(4.4);

		/** General active arc */
		PrimitiveArc active_arc = PrimitiveArc.newInstance(this, "Active", 1.1, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(pActive_lay, 0, Poly.Type.FILLED),
			new Technology.ArcLayer(nActive_lay, 0, Poly.Type.FILLED)
		});
		active_arc.setFunction(PrimitiveArc.Function.DIFF);
		active_arc.setFactoryFixedAngle(true);
		active_arc.setWipable();
		active_arc.setFactoryAngleIncrement(90);
		active_arc.setNotUsed();

		//**************************************** NODES ****************************************

		/** metal-1-pin */
		PrimitiveNode metal1Pin_node = PrimitiveNode.newInstance("Metal-1-Pin", this, 1.2, 1.2, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(pseudoMetal1_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		metal1Pin_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metal1Pin_node, new ArcProto[] {metal1_arc}, "metal-1", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		metal1Pin_node.setFunction(NodeProto.Function.PIN);
		metal1Pin_node.setArcsWipe();
		metal1Pin_node.setArcsShrink();

		/** metal-2-pin */
		PrimitiveNode metal2Pin_node = PrimitiveNode.newInstance("Metal-2-Pin", this, 1.4, 1.4, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(pseudoMetal2_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		metal2Pin_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metal2Pin_node, new ArcProto[] {metal2_arc}, "metal-2", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		metal2Pin_node.setFunction(NodeProto.Function.PIN);
		metal2Pin_node.setArcsWipe();
		metal2Pin_node.setArcsShrink();

		/** metal-3-pin */
		PrimitiveNode metal3Pin_node = PrimitiveNode.newInstance("Metal-3-Pin", this, 1.4, 1.4, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(pseudoMetal3_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		metal3Pin_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metal3Pin_node, new ArcProto[] {metal3_arc}, "metal-3", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		metal3Pin_node.setFunction(NodeProto.Function.PIN);
		metal3Pin_node.setArcsWipe();
		metal3Pin_node.setArcsShrink();

		/** metal-4-pin */
		PrimitiveNode metal4Pin_node = PrimitiveNode.newInstance("Metal-4-Pin", this, 1.4, 1.4, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(pseudoMetal4_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		metal4Pin_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metal4Pin_node, new ArcProto[] {metal4_arc}, "metal-4", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		metal4Pin_node.setFunction(NodeProto.Function.PIN);
		metal4Pin_node.setArcsWipe();
		metal4Pin_node.setArcsShrink();

		/** metal-5-pin */
		PrimitiveNode metal5Pin_node = PrimitiveNode.newInstance("Metal-5-Pin", this, 1.4, 1.4, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(pseudoMetal5_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		metal5Pin_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metal5Pin_node, new ArcProto[] {metal5_arc}, "metal-5", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		metal5Pin_node.setFunction(NodeProto.Function.PIN);
		metal5Pin_node.setArcsWipe();
		metal5Pin_node.setArcsShrink();

		/** metal-6-pin */
		PrimitiveNode metal6Pin_node = PrimitiveNode.newInstance("Metal-6-Pin", this, 1.4, 1.4, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(pseudoMetal6_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		metal6Pin_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metal6Pin_node, new ArcProto[] {metal6_arc}, "metal-6", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		metal6Pin_node.setFunction(NodeProto.Function.PIN);
		metal6Pin_node.setArcsWipe();
		metal6Pin_node.setArcsShrink();

		/** metal-7-pin */
		PrimitiveNode metal7Pin_node = PrimitiveNode.newInstance("Metal-7-Pin", this, 1.4, 1.4, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(pseudoMetal7_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		metal7Pin_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metal7Pin_node, new ArcProto[] {metal7_arc}, "metal-7", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		metal7Pin_node.setFunction(NodeProto.Function.PIN);
		metal7Pin_node.setArcsWipe();
		metal7Pin_node.setArcsShrink();

		/** metal-8-pin */
		PrimitiveNode metal8Pin_node = PrimitiveNode.newInstance("Metal-8-Pin", this, 4.2, 4.2, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(pseudoMetal8_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		metal8Pin_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metal8Pin_node, new ArcProto[] {metal8_arc}, "metal-8", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		metal8Pin_node.setFunction(NodeProto.Function.PIN);
		metal8Pin_node.setArcsWipe();
		metal8Pin_node.setArcsShrink();

		/** metal-9-pin */
		PrimitiveNode metal9Pin_node = PrimitiveNode.newInstance("Metal-9-Pin", this, 4.2, 4.2, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(pseudoMetal9_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		metal9Pin_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metal9Pin_node, new ArcProto[] {metal9_arc}, "metal-9", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		metal9Pin_node.setFunction(NodeProto.Function.PIN);
		metal9Pin_node.setArcsWipe();
		metal9Pin_node.setArcsShrink();

		/** Polysilicon-pin */
		PrimitiveNode polyPin_node = PrimitiveNode.newInstance("Polysilicon-Pin", this, 1.0, 1.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(pseudoPoly_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		polyPin_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, polyPin_node, new ArcProto[] {poly_arc}, "polysilicon", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		polyPin_node.setFunction(NodeProto.Function.PIN);
		polyPin_node.setArcsWipe();
		polyPin_node.setArcsShrink();

		/** P-active-pin */
		PrimitiveNode pActivePin_node = PrimitiveNode.newInstance("P-Active-Pin", this, 5.5, 5.5, new SizeOffset(2.2, 2.2, 2.2, 2.2),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(pseudoPActive_lay,  0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(2.2)),
				new Technology.NodeLayer(pseudoNWell_lay,  0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox()),
				new Technology.NodeLayer(pseudoPSelect_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(0.9))
			});
		pActivePin_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, pActivePin_node, new ArcProto[] {pActive_arc}, "p-active", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		pActivePin_node.setFunction(NodeProto.Function.PIN);
		pActivePin_node.setArcsWipe();
		pActivePin_node.setArcsShrink();

		/** N-active-pin */
		PrimitiveNode nActivePin_node = PrimitiveNode.newInstance("N-Active-Pin", this, 5.5, 5.5, new SizeOffset(2.2, 2.2, 2.2, 2.2),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(pseudoNActive_lay,  0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(2.2)),
				new Technology.NodeLayer(pseudoPWell_lay,  0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox()),
				new Technology.NodeLayer(pseudoNSelect_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(0.9))
			});
		nActivePin_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, nActivePin_node, new ArcProto[] {nActive_arc}, "n-active", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		nActivePin_node.setFunction(NodeProto.Function.PIN);
		nActivePin_node.setArcsWipe();
		nActivePin_node.setArcsShrink();

		/** General active-pin */
		PrimitiveNode activePin_node = PrimitiveNode.newInstance("Active-Pin", this, 1.1, 1.1, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(pseudoPActive_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox()),
				new Technology.NodeLayer(pseudoNActive_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		activePin_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, activePin_node, new ArcProto[] {active_arc, pActive_arc, nActive_arc}, "active", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		activePin_node.setFunction(NodeProto.Function.PIN);
		activePin_node.setArcsWipe();
		activePin_node.setArcsShrink();
		activePin_node.setNotUsed();

		/** metal-1-P-active-contact */
		PrimitiveNode metal1PActiveContact_node = PrimitiveNode.newInstance("Metal-1-P-Active-Con", this, 7.0, 7.0, new SizeOffset(2.2, 2.2, 2.2, 2.2),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(metal1_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(2.875)),
				new Technology.NodeLayer(pActive_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(2.2)),
				new Technology.NodeLayer(nWell_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX,Technology.TechPoint.makeFullBox()),
				new Technology.NodeLayer(pSelect_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(0.9)),
				new Technology.NodeLayer(activeCut_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(2.9))
			});
		metal1PActiveContact_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metal1PActiveContact_node, new ArcProto[] {pActive_arc, metal1_arc}, "metal-1-p-act", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(3.5), EdgeV.fromBottom(3.5), EdgeH.fromRight(3.5), EdgeV.fromTop(3.5))
			});
		metal1PActiveContact_node.setFunction(NodeProto.Function.CONTACT);
//		metal1PActiveContact_node.setSpecialType(PrimitiveNode.MULTICUT);
//		metal1PActiveContact_node.setSpecialValues(new double [] {2, 2, 1.5, 3});
		metal1PActiveContact_node.setMinSize(7, 7, "");

		/** metal-1-N-active-contact */
		PrimitiveNode metal1NActiveContact_node = PrimitiveNode.newInstance("Metal-1-N-Active-Con", this, 7.0, 7.0, new SizeOffset(2.2, 2.2, 2.2, 2.2),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(metal1_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(2.875)),
				new Technology.NodeLayer(nActive_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(2.2)),
				new Technology.NodeLayer(pWell_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox()),
				new Technology.NodeLayer(nSelect_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(0.9)),
				new Technology.NodeLayer(activeCut_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(2.9))
			});
		metal1NActiveContact_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metal1NActiveContact_node, new ArcProto[] {nActive_arc, metal1_arc}, "metal-1-n-act", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(3.5), EdgeV.fromBottom(3.5), EdgeH.fromRight(3.5), EdgeV.fromTop(3.5))
			});
		metal1NActiveContact_node.setFunction(NodeProto.Function.CONTACT);
//		metal1NActiveContact_node.setSpecialType(PrimitiveNode.MULTICUT);
//		metal1NActiveContact_node.setSpecialValues(new double [] {2, 2, 1.5, 3});
		metal1NActiveContact_node.setMinSize(7, 7, "");

		/** metal-1-polysilicon-contact */
		PrimitiveNode metal1PolyContact_node = PrimitiveNode.newInstance("Metal-1-Polysilicon-Con", this, 2.6, 2.6, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(metal1_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(0.675)),
				new Technology.NodeLayer(poly_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox()),
				new Technology.NodeLayer(polyCut_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(0.7))
			});
		metal1PolyContact_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metal1PolyContact_node, new ArcProto[] {poly_arc, metal1_arc}, "metal-1-polysilicon", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(1.3), EdgeV.fromBottom(1.3), EdgeH.fromRight(1.3), EdgeV.fromTop(1.3))
			});
		metal1PolyContact_node.setFunction(NodeProto.Function.CONTACT);
//		metal1PolyContact_node.setSpecialType(PrimitiveNode.MULTICUT);
//		metal1PolyContact_node.setSpecialValues(new double [] {2, 2, 1.5, 3});
		metal1PolyContact_node.setMinSize(2.6, 2.6, "");

		/** P-Transistor */
		Technology.NodeLayer pTransistorPolyLayer = new Technology.NodeLayer(transistorPoly_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
			new Technology.TechPoint(EdgeH.fromLeft(4.5), EdgeV.fromBottom(0.6)),
			new Technology.TechPoint(EdgeH.fromRight(4.5), EdgeV.fromTop(0.6))}, 1, 1, 2, 2);
		Technology.NodeLayer pTransistorPolyTLayer = new Technology.NodeLayer(poly_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
			new Technology.TechPoint(EdgeH.fromLeft(4.5), EdgeV.fromTop(2.2)),
			new Technology.TechPoint(EdgeH.fromRight(4.5), EdgeV.fromTop(0.6))}, 1, 1, 2, 2);
		Technology.NodeLayer pTransistorPolyBLayer = new Technology.NodeLayer(poly_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
			new Technology.TechPoint(EdgeH.fromLeft(4.5), EdgeV.fromBottom(0.6)),
			new Technology.TechPoint(EdgeH.fromRight(4.5), EdgeV.fromBottom(2.2))}, 1, 1, 2, 2);
		Technology.NodeLayer pTransistorPolyCLayer = new Technology.NodeLayer(transistorPoly_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
			new Technology.TechPoint(EdgeH.fromLeft(4.5), EdgeV.fromBottom(2.2)),
			new Technology.TechPoint(EdgeH.fromRight(4.5), EdgeV.fromTop(2.2))}, 1, 1, 2, 2);
		Technology.NodeLayer pTransistorActiveLayer = new Technology.NodeLayer(pActive_lay, 1, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
			new Technology.TechPoint(EdgeH.fromLeft(2.2), EdgeV.fromBottom(2.2)),
			new Technology.TechPoint(EdgeH.fromRight(2.2), EdgeV.fromTop(2.2))}, 4, 4, 0, 0);
		Technology.NodeLayer pTransistorActiveLLayer = new Technology.NodeLayer(pActive_lay, 1, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
			new Technology.TechPoint(EdgeH.fromLeft(2.2), EdgeV.fromBottom(2.2)),
			new Technology.TechPoint(EdgeH.fromLeft(4.4), EdgeV.fromTop(2.2))}, 4, 4, 0, 0);
		Technology.NodeLayer pTransistorActiveRLayer = new Technology.NodeLayer(pActive_lay, 3, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
			new Technology.TechPoint(EdgeH.fromRight(4.4), EdgeV.fromBottom(2.2)),
			new Technology.TechPoint(EdgeH.fromRight(2.2), EdgeV.fromTop(2.2))}, 4, 4, 0, 0);
		Technology.NodeLayer pTransistorWellLayer = new Technology.NodeLayer(nWell_lay, -1, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
			new Technology.TechPoint(EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge()),
			new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.makeTopEdge())}, 10, 10, 6, 6);
		Technology.NodeLayer pTransistorSelectLayer = new Technology.NodeLayer(pSelect_lay, -1, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
			new Technology.TechPoint(EdgeH.fromLeft(0.9), EdgeV.makeBottomEdge()),
			new Technology.TechPoint(EdgeH.fromRight(0.9), EdgeV.makeTopEdge())}, 6, 6, 2, 2);
		PrimitiveNode pTransistor_node = PrimitiveNode.newInstance("P-Transistor", this, 10.0, 6.4, new SizeOffset(4.5, 4.5, 2.2, 2.2),
			new Technology.NodeLayer [] {pTransistorActiveLayer, pTransistorPolyLayer, pTransistorWellLayer, pTransistorSelectLayer});
		pTransistor_node.setElectricalLayers(new Technology.NodeLayer [] {pTransistorActiveLLayer, pTransistorActiveRLayer,
			pTransistorPolyCLayer, pTransistorPolyTLayer, pTransistorPolyBLayer, pTransistorWellLayer, pTransistorSelectLayer});
		pTransistor_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, pTransistor_node, new ArcProto[] {poly_arc}, "p-trans-poly-top", 90,90, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(5), EdgeV.fromTop(0.7), EdgeH.fromRight(5), EdgeV.fromTop(0.7)),
				PrimitivePort.newInstance(this, pTransistor_node, new ArcProto[] {pActive_arc}, "p-trans-diff-left", 180,90, 1, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(3.5), EdgeV.fromBottom(3.2), EdgeH.fromLeft(3.5), EdgeV.fromTop(3.2)),
				PrimitivePort.newInstance(this, pTransistor_node, new ArcProto[] {poly_arc}, "p-trans-poly-bottom", 270,90, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(5), EdgeV.fromBottom(0.7), EdgeH.fromRight(5), EdgeV.fromBottom(0.7)),
				PrimitivePort.newInstance(this, pTransistor_node, new ArcProto[] {pActive_arc}, "p-trans-diff-right", 0,90, 2, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromRight(3.5), EdgeV.fromBottom(3.2), EdgeH.fromRight(3.5), EdgeV.fromTop(3.2))
			});
		pTransistor_node.setFunction(NodeProto.Function.TRAPMOS);
		pTransistor_node.setCanShrink();
//		pTransistor_node.setHoldsOutline();
//		pTransistor_node.setSpecialType(PrimitiveNode.SERPTRANS);
//		pTransistor_node.setSpecialValues(new double [] {7, 1.5, 2.5, 2, 1, 2});
		pTransistor_node.setMinSize(10, 6.4, "");

		/** N-Transistor */
		/** P-Transistor */
		Technology.NodeLayer nTransistorPolyLayer = new Technology.NodeLayer(transistorPoly_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
			new Technology.TechPoint(EdgeH.fromLeft(4.5), EdgeV.fromBottom(0.6)),
			new Technology.TechPoint(EdgeH.fromRight(4.5), EdgeV.fromTop(0.6))}, 1, 1, 2, 2);
		Technology.NodeLayer nTransistorPolyTLayer = new Technology.NodeLayer(poly_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
			new Technology.TechPoint(EdgeH.fromLeft(4.5), EdgeV.fromTop(2.2)),
			new Technology.TechPoint(EdgeH.fromRight(4.5), EdgeV.fromTop(0.6))}, 1, 1, 2, 2);
		Technology.NodeLayer nTransistorPolyBLayer = new Technology.NodeLayer(poly_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
			new Technology.TechPoint(EdgeH.fromLeft(4.5), EdgeV.fromBottom(0.6)),
			new Technology.TechPoint(EdgeH.fromRight(4.5), EdgeV.fromBottom(2.2))}, 1, 1, 2, 2);
		Technology.NodeLayer nTransistorPolyCLayer = new Technology.NodeLayer(transistorPoly_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
			new Technology.TechPoint(EdgeH.fromLeft(4.5), EdgeV.fromBottom(2.2)),
			new Technology.TechPoint(EdgeH.fromRight(4.5), EdgeV.fromTop(2.2))}, 1, 1, 2, 2);
		Technology.NodeLayer nTransistorActiveLayer = new Technology.NodeLayer(nActive_lay, 1, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
			new Technology.TechPoint(EdgeH.fromLeft(2.2), EdgeV.fromBottom(2.2)),
			new Technology.TechPoint(EdgeH.fromRight(2.2), EdgeV.fromTop(2.2))}, 4, 4, 0, 0);
		Technology.NodeLayer nTransistorActiveLLayer = new Technology.NodeLayer(nActive_lay, 1, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
			new Technology.TechPoint(EdgeH.fromLeft(2.2), EdgeV.fromBottom(2.2)),
			new Technology.TechPoint(EdgeH.fromLeft(4.4), EdgeV.fromTop(2.2))}, 4, 4, 0, 0);
		Technology.NodeLayer nTransistorActiveRLayer = new Technology.NodeLayer(nActive_lay, 3, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
			new Technology.TechPoint(EdgeH.fromRight(4.4), EdgeV.fromBottom(2.2)),
			new Technology.TechPoint(EdgeH.fromRight(2.2), EdgeV.fromTop(2.2))}, 4, 4, 0, 0);
		Technology.NodeLayer nTransistorWellLayer = new Technology.NodeLayer(pWell_lay, -1, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
			new Technology.TechPoint(EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge()),
			new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.makeTopEdge())}, 10, 10, 6, 6);
		Technology.NodeLayer nTransistorSelectLayer = new Technology.NodeLayer(nSelect_lay, -1, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
			new Technology.TechPoint(EdgeH.fromLeft(0.9), EdgeV.makeBottomEdge()),
			new Technology.TechPoint(EdgeH.fromRight(0.9), EdgeV.makeTopEdge())}, 6, 6, 2, 2);
		PrimitiveNode nTransistor_node = PrimitiveNode.newInstance("N-Transistor", this, 10.0, 6.4, new SizeOffset(4.5, 4.5, 2.2, 2.2),
			new Technology.NodeLayer [] {nTransistorActiveLayer, nTransistorPolyLayer, nTransistorWellLayer, nTransistorSelectLayer});
		nTransistor_node.setElectricalLayers(new Technology.NodeLayer [] {nTransistorActiveLLayer, nTransistorActiveRLayer,
			nTransistorPolyCLayer, nTransistorPolyTLayer, nTransistorPolyBLayer, nTransistorWellLayer, nTransistorSelectLayer});
		nTransistor_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, nTransistor_node, new ArcProto[] {poly_arc}, "n-trans-poly-top", 90,90, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(5), EdgeV.fromTop(0.7), EdgeH.fromRight(5), EdgeV.fromTop(0.7)),
				PrimitivePort.newInstance(this, nTransistor_node, new ArcProto[] {nActive_arc}, "n-trans-diff-left", 180,90, 1, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(3.5), EdgeV.fromBottom(3.2), EdgeH.fromLeft(3.5), EdgeV.fromTop(3.2)),
				PrimitivePort.newInstance(this, nTransistor_node, new ArcProto[] {poly_arc}, "n-trans-poly-bottom", 270,90, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(5), EdgeV.fromBottom(0.7), EdgeH.fromRight(5), EdgeV.fromBottom(0.7)),
				PrimitivePort.newInstance(this, nTransistor_node, new ArcProto[] {nActive_arc}, "n-trans-diff-right", 0,90, 2, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromRight(3.5), EdgeV.fromBottom(3.2), EdgeH.fromRight(3.5), EdgeV.fromTop(3.2))
			});
		nTransistor_node.setFunction(NodeProto.Function.TRANMOS);
		nTransistor_node.setCanShrink();
//		nTransistor_node.setHoldsOutline();
//		nTransistor_node.setSpecialType(PrimitiveNode.SERPTRANS);
//		nTransistor_node.setSpecialValues(new double [] {7, 1.5, 2.5, 2, 1, 2});
		nTransistor_node.setMinSize(10, 6.4, "");

		/** metal-1-metal-2-contact */
		PrimitiveNode metal1Metal2Contact_node = PrimitiveNode.newInstance("Metal-1-Metal-2-Con", this, 1.4, 2.9, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(metal1_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox()),
				new Technology.NodeLayer(metal2_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox()),
				new Technology.NodeLayer(via1_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(0.05), EdgeV.fromBottom(0.8)),
					new Technology.TechPoint(EdgeH.fromRight(0.05), EdgeV.fromTop(0.8))})
			});
		metal1Metal2Contact_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metal1Metal2Contact_node, new ArcProto[] {metal1_arc, metal2_arc}, "metal-1-metal-2", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(0.7), EdgeV.fromBottom(1.45), EdgeH.fromRight(0.7), EdgeV.fromTop(1.45))
			});
		metal1Metal2Contact_node.setFunction(NodeProto.Function.CONTACT);
//		metal1Metal2Contact_node.setSpecialType(PrimitiveNode.MULTICUT);
//		metal1Metal2Contact_node.setSpecialValues(new double [] {2, 2, 1, 3});
		metal1Metal2Contact_node.setMinSize(1.4, 2.9, "");

		/** metal-2-metal-3-contact */
		PrimitiveNode metal2Metal3Contact_node = PrimitiveNode.newInstance("Metal-2-Metal-3-Con", this, 1.4, 2.9, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(metal2_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox()),
				new Technology.NodeLayer(metal3_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox()),
				new Technology.NodeLayer(via2_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(0.05), EdgeV.fromBottom(0.8)),
					new Technology.TechPoint(EdgeH.fromRight(0.05), EdgeV.fromTop(0.8))})
			});
		metal2Metal3Contact_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metal2Metal3Contact_node, new ArcProto[] {metal2_arc, metal3_arc}, "metal-2-metal-3", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(0.7), EdgeV.fromBottom(1.45), EdgeH.fromRight(0.7), EdgeV.fromTop(1.45))
			});
		metal2Metal3Contact_node.setFunction(NodeProto.Function.CONTACT);
//		metal2Metal3Contact_node.setSpecialType(PrimitiveNode.MULTICUT);
//		metal2Metal3Contact_node.setSpecialValues(new double [] {2, 2, 1, 3});
		metal2Metal3Contact_node.setMinSize(1.4, 2.9, "");

		/** metal-3-metal-4-contact */
		PrimitiveNode metal3Metal4Contact_node = PrimitiveNode.newInstance("Metal-3-Metal-4-Con", this, 1.4, 2.9, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(metal3_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox()),
				new Technology.NodeLayer(metal4_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox()),
				new Technology.NodeLayer(via3_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(0.05), EdgeV.fromBottom(0.8)),
					new Technology.TechPoint(EdgeH.fromRight(0.05), EdgeV.fromTop(0.8))})
			});
		metal3Metal4Contact_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metal3Metal4Contact_node, new ArcProto[] {metal3_arc, metal4_arc}, "metal-3-metal-4", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(0.7), EdgeV.fromBottom(1.45), EdgeH.fromRight(0.7), EdgeV.fromTop(1.45))
			});
		metal3Metal4Contact_node.setFunction(NodeProto.Function.CONTACT);
//		metal3Metal4Contact_node.setSpecialType(PrimitiveNode.MULTICUT);
//		metal3Metal4Contact_node.setSpecialValues(new double [] {2, 2, 2, 3});
		metal3Metal4Contact_node.setMinSize(1.4, 2.9, "");

		/** metal-4-metal-5-contact */
		PrimitiveNode metal4Metal5Contact_node = PrimitiveNode.newInstance("Metal-4-Metal-5-Con", this, 1.4, 2.9, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(metal4_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox()),
				new Technology.NodeLayer(metal5_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox()),
				new Technology.NodeLayer(via4_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(0.05), EdgeV.fromBottom(0.8)),
					new Technology.TechPoint(EdgeH.fromRight(0.05), EdgeV.fromTop(0.8))})
			});
		metal4Metal5Contact_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metal4Metal5Contact_node, new ArcProto[] {metal4_arc, metal5_arc}, "metal-4-metal-5", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(0.7), EdgeV.fromBottom(1.45), EdgeH.fromRight(0.7), EdgeV.fromTop(1.45))
			});
		metal4Metal5Contact_node.setFunction(NodeProto.Function.CONTACT);
//		metal4Metal5Contact_node.setSpecialType(PrimitiveNode.MULTICUT);
//		metal4Metal5Contact_node.setSpecialValues(new double [] {2, 2, 1, 3});
		metal4Metal5Contact_node.setMinSize(1.4, 2.9, "");

		/** metal-5-metal-6-contact */
		PrimitiveNode metal5Metal6Contact_node = PrimitiveNode.newInstance("Metal-5-Metal-6-Con", this, 1.4, 2.9, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(metal5_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox()),
				new Technology.NodeLayer(metal6_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox()),
				new Technology.NodeLayer(via5_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(0.05), EdgeV.fromBottom(0.8)),
					new Technology.TechPoint(EdgeH.fromRight(0.05), EdgeV.fromTop(0.8))})
			});
		metal5Metal6Contact_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metal5Metal6Contact_node, new ArcProto[] {metal5_arc, metal6_arc}, "metal-5-metal-6", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(0.7), EdgeV.fromBottom(1.45), EdgeH.fromRight(0.7), EdgeV.fromTop(1.45))
			});
		metal5Metal6Contact_node.setFunction(NodeProto.Function.CONTACT);
//		metal5Metal6Contact_node.setSpecialType(PrimitiveNode.MULTICUT);
//		metal5Metal6Contact_node.setSpecialValues(new double [] {3, 3, 2, 4});
		metal5Metal6Contact_node.setMinSize(1.4, 2.9, "");

		/** metal-6-metal-7-contact */
		PrimitiveNode metal6Metal7Contact_node = PrimitiveNode.newInstance("Metal-6-Metal-7-Con", this, 1.4, 2.9, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(metal6_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox()),
				new Technology.NodeLayer(metal7_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox()),
				new Technology.NodeLayer(via6_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(0.05), EdgeV.fromBottom(0.8)),
					new Technology.TechPoint(EdgeH.fromRight(0.05), EdgeV.fromTop(0.8))})
			});
		metal6Metal7Contact_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metal6Metal7Contact_node, new ArcProto[] {metal6_arc, metal7_arc}, "metal-6-metal-7", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(0.7), EdgeV.fromBottom(1.45), EdgeH.fromRight(0.7), EdgeV.fromTop(1.45))
			});
		metal6Metal7Contact_node.setFunction(NodeProto.Function.CONTACT);
//		metal6Metal7Contact_node.setSpecialType(PrimitiveNode.MULTICUT);
//		metal6Metal7Contact_node.setSpecialValues(new double [] {3, 3, 2, 4});
		metal6Metal7Contact_node.setMinSize(1.4, 2.9, "");

		/** metal-7-metal-8-contact */
		PrimitiveNode metal7Metal8Contact_node = PrimitiveNode.newInstance("Metal-7-Metal-8-Con", this, 4.2, 5.2, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(metal7_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox()),
				new Technology.NodeLayer(metal8_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox()),
				new Technology.NodeLayer(via7_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(0.3), EdgeV.fromBottom(0.8)),
					new Technology.TechPoint(EdgeH.fromRight(0.3), EdgeV.fromTop(0.8))})
			});
		metal7Metal8Contact_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metal7Metal8Contact_node, new ArcProto[] {metal7_arc, metal8_arc}, "metal-7-metal-8", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(2.1), EdgeV.fromBottom(2.6), EdgeH.fromRight(2.1), EdgeV.fromTop(2.6))
			});
		metal7Metal8Contact_node.setFunction(NodeProto.Function.CONTACT);
//		metal7Metal8Contact_node.setSpecialType(PrimitiveNode.MULTICUT);
//		metal7Metal8Contact_node.setSpecialValues(new double [] {3, 3, 2, 4});
		metal7Metal8Contact_node.setMinSize(4.2, 5.2, "");

		/** metal-8-metal-9-contact */
		PrimitiveNode metal8Metal9Contact_node = PrimitiveNode.newInstance("Metal-8-Metal-9-Con", this, 4.2, 5.2, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(metal8_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox()),
				new Technology.NodeLayer(metal9_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox()),
				new Technology.NodeLayer(via8_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(0.3), EdgeV.fromBottom(0.8)),
					new Technology.TechPoint(EdgeH.fromRight(0.3), EdgeV.fromTop(0.8))})
			});
		metal8Metal9Contact_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metal8Metal9Contact_node, new ArcProto[] {metal8_arc, metal9_arc}, "metal-8-metal-9", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(2.1), EdgeV.fromBottom(2.6), EdgeH.fromRight(2.1), EdgeV.fromTop(2.6))
			});
		metal8Metal9Contact_node.setFunction(NodeProto.Function.CONTACT);
//		metal8Metal9Contact_node.setSpecialType(PrimitiveNode.MULTICUT);
//		metal8Metal9Contact_node.setSpecialValues(new double [] {3, 3, 2, 4});
		metal8Metal9Contact_node.setMinSize(4.2, 5.2, "");

		/** Metal-1-P-Well Contact */
		PrimitiveNode metal1PWellContact_node = PrimitiveNode.newInstance("Metal-1-P-Well-Con", this, 6.0, 6.0, new SizeOffset(1.7, 1.7, 1.7, 1.7),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(metal1_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(2.375)),
				new Technology.NodeLayer(pActiveWell_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(1.7)),
				new Technology.NodeLayer(pWell_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox()),
				new Technology.NodeLayer(pSelect_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(1.5)),
				new Technology.NodeLayer(activeCut_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(2.4))
			});
		metal1PWellContact_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metal1PWellContact_node, new ArcProto[] {metal1_arc, active_arc}, "metal-1-well", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(3), EdgeV.fromBottom(3), EdgeH.fromRight(3), EdgeV.fromTop(3))
			});
		metal1PWellContact_node.setFunction(NodeProto.Function.WELL);
//		metal1PWellContact_node.setSpecialType(PrimitiveNode.MULTICUT);
//		metal1PWellContact_node.setSpecialValues(new double [] {2, 2, 1.5, 3});
		metal1PWellContact_node.setMinSize(6, 6, "");

		/** Metal-1-N-Well Contact */
		PrimitiveNode metal1NWellContact_node = PrimitiveNode.newInstance("Metal-1-N-Well-Con", this, 6.0, 6.0, new SizeOffset(1.7, 1.7, 1.7, 1.7),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(metal1_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(2.375)),
				new Technology.NodeLayer(nActive_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(1.7)),
				new Technology.NodeLayer(nWell_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox()),
				new Technology.NodeLayer(nSelect_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(1.5)),
				new Technology.NodeLayer(activeCut_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(2.4))
			});
		metal1NWellContact_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metal1NWellContact_node, new ArcProto[] {metal1_arc, active_arc}, "metal-1-substrate", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(3), EdgeV.fromBottom(3), EdgeH.fromRight(3), EdgeV.fromTop(3))
			});
		metal1NWellContact_node.setFunction(NodeProto.Function.SUBSTRATE);
//		metal1NWellContact_node.setSpecialType(PrimitiveNode.MULTICUT);
//		metal1NWellContact_node.setSpecialValues(new double [] {2, 2, 1.5, 3});
		metal1NWellContact_node.setMinSize(6, 6, "");

		/** Metal-1-Node */
		PrimitiveNode metal1Node_node = PrimitiveNode.newInstance("Metal-1-Node", this, 1.2, 1.2, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(metal1_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		metal1Node_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metal1Node_node, new ArcProto[] {metal1_arc}, "metal-1", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		metal1Node_node.setFunction(NodeProto.Function.NODE);
		metal1Node_node.setHoldsOutline();
		metal1Node_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Metal-2-Node */
		PrimitiveNode metal2Node_node = PrimitiveNode.newInstance("Metal-2-Node", this, 1.4, 1.4, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(metal2_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		metal2Node_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metal2Node_node, new ArcProto[] {metal2_arc}, "metal-2", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		metal2Node_node.setFunction(NodeProto.Function.NODE);
		metal2Node_node.setHoldsOutline();
		metal2Node_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Metal-3-Node */
		PrimitiveNode metal3Node_node = PrimitiveNode.newInstance("Metal-3-Node", this, 1.4, 1.4, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(metal3_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		metal3Node_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metal3Node_node, new ArcProto[] {metal3_arc}, "metal-3", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		metal3Node_node.setFunction(NodeProto.Function.NODE);
		metal3Node_node.setHoldsOutline();
		metal3Node_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Metal-4-Node */
		PrimitiveNode metal4Node_node = PrimitiveNode.newInstance("Metal-4-Node", this, 1.4, 1.4, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(metal4_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		metal4Node_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metal4Node_node, new ArcProto[] {metal4_arc}, "metal-4", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		metal4Node_node.setFunction(NodeProto.Function.NODE);
		metal4Node_node.setHoldsOutline();
		metal4Node_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Metal-5-Node */
		PrimitiveNode metal5Node_node = PrimitiveNode.newInstance("Metal-5-Node", this, 1.4, 1.4, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(metal5_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		metal5Node_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metal5Node_node, new ArcProto[] {metal5_arc}, "metal-5", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		metal5Node_node.setFunction(NodeProto.Function.NODE);
		metal5Node_node.setHoldsOutline();
		metal5Node_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Metal-6-Node */
		PrimitiveNode metal6Node_node = PrimitiveNode.newInstance("Metal-6-Node", this, 1.4, 1.4, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(metal6_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		metal6Node_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metal6Node_node, new ArcProto[] {metal6_arc}, "metal-6", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		metal6Node_node.setFunction(NodeProto.Function.NODE);
		metal6Node_node.setHoldsOutline();
		metal6Node_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Metal-7-Node */
		PrimitiveNode metal7Node_node = PrimitiveNode.newInstance("Metal-7-Node", this, 1.4, 1.4, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(metal7_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		metal7Node_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metal7Node_node, new ArcProto[] {metal7_arc}, "metal-7", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		metal7Node_node.setFunction(NodeProto.Function.NODE);
		metal7Node_node.setHoldsOutline();
		metal7Node_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Metal-8-Node */
		PrimitiveNode metal8Node_node = PrimitiveNode.newInstance("Metal-8-Node", this, 4.2, 4.2, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(metal8_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		metal8Node_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metal8Node_node, new ArcProto[] {metal8_arc}, "metal-8", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		metal8Node_node.setFunction(NodeProto.Function.NODE);
		metal8Node_node.setHoldsOutline();
		metal8Node_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Metal-9-Node */
		PrimitiveNode metal9Node_node = PrimitiveNode.newInstance("Metal-9-Node", this, 4.2, 4.2, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(metal9_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		metal9Node_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metal9Node_node, new ArcProto[] {metal9_arc}, "metal-9", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		metal9Node_node.setFunction(NodeProto.Function.NODE);
		metal9Node_node.setHoldsOutline();
		metal9Node_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Polysilicon-Node */
		PrimitiveNode polyNode_node = PrimitiveNode.newInstance("Polysilicon-Node", this, 1.0, 1.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(poly_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		polyNode_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, polyNode_node, new ArcProto[] {poly_arc}, "polysilicon", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		polyNode_node.setFunction(NodeProto.Function.NODE);
		polyNode_node.setHoldsOutline();
		polyNode_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** P-Active-Node */
		PrimitiveNode pActiveNode_node = PrimitiveNode.newInstance("P-Active-Node", this, 1.1, 1.1, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(pActive_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		pActiveNode_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, pActiveNode_node, new ArcProto[] {active_arc, pActive_arc, nActive_arc}, "active", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		pActiveNode_node.setFunction(NodeProto.Function.NODE);
		pActiveNode_node.setHoldsOutline();
		pActiveNode_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** N-Active-Node */
		PrimitiveNode nActiveNode_node = PrimitiveNode.newInstance("N-Active-Node", this, 1.1, 1.1, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(nActive_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		nActiveNode_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, nActiveNode_node, new ArcProto[] {active_arc, pActive_arc, nActive_arc}, "active", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		nActiveNode_node.setFunction(NodeProto.Function.NODE);
		nActiveNode_node.setHoldsOutline();
		nActiveNode_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** P-Select-Node */
		PrimitiveNode pSelectNode_node = PrimitiveNode.newInstance("P-Select-Node", this, 3.7, 3.7, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(pSelect_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		pSelectNode_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, pSelectNode_node, new ArcProto[0], "select", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		pSelectNode_node.setFunction(NodeProto.Function.NODE);
		pSelectNode_node.setHoldsOutline();
		pSelectNode_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** N-Select-Node */
		PrimitiveNode nSelectNode_node = PrimitiveNode.newInstance("N-Select-Node", this, 3.7, 3.7, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(nSelect_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		nSelectNode_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, nSelectNode_node, new ArcProto[0], "select", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		nSelectNode_node.setFunction(NodeProto.Function.NODE);
		nSelectNode_node.setHoldsOutline();
		nSelectNode_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** PolyCut-Node */
		PrimitiveNode polyCutNode_node = PrimitiveNode.newInstance("Poly-Cut-Node", this, 1.2, 1.2, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(polyCut_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		polyCutNode_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, polyCutNode_node, new ArcProto[0], "polycut", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		polyCutNode_node.setFunction(NodeProto.Function.NODE);
		polyCutNode_node.setHoldsOutline();
		polyCutNode_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** ActiveCut-Node */
		PrimitiveNode activeCutNode_node = PrimitiveNode.newInstance("Active-Cut-Node", this, 1.2, 1.2, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(activeCut_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		activeCutNode_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, activeCutNode_node, new ArcProto[0], "activecut", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		activeCutNode_node.setFunction(NodeProto.Function.NODE);
		activeCutNode_node.setHoldsOutline();
		activeCutNode_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Via-1-Node */
		PrimitiveNode via1Node_node = PrimitiveNode.newInstance("Via-1-Node", this, 1.4, 1.3, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(via1_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		via1Node_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, via1Node_node, new ArcProto[0], "via-1", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		via1Node_node.setFunction(NodeProto.Function.NODE);
		via1Node_node.setHoldsOutline();
		via1Node_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Via-2-Node */
		PrimitiveNode via2Node_node = PrimitiveNode.newInstance("Via-2-Node", this, 1.4, 1.3, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(via2_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		via2Node_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, via2Node_node, new ArcProto[0], "via-2", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		via2Node_node.setFunction(NodeProto.Function.NODE);
		via2Node_node.setHoldsOutline();
		via2Node_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Via-3-Node */
		PrimitiveNode via3Node_node = PrimitiveNode.newInstance("Via-3-Node", this, 1.4, 1.3, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(via3_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		via3Node_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, via3Node_node, new ArcProto[0], "via-3", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		via3Node_node.setFunction(NodeProto.Function.NODE);
		via3Node_node.setHoldsOutline();
		via3Node_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Via-4-Node */
		PrimitiveNode via4Node_node = PrimitiveNode.newInstance("Via-4-Node", this, 1.4, 1.3, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(via4_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		via4Node_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, via4Node_node, new ArcProto[0], "via-4", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		via4Node_node.setFunction(NodeProto.Function.NODE);
		via4Node_node.setHoldsOutline();
		via4Node_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Via-5-Node */
		PrimitiveNode via5Node_node = PrimitiveNode.newInstance("Via-5-Node", this, 1.4, 1.3, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(via5_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		via5Node_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, via5Node_node, new ArcProto[0], "via-5", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		via5Node_node.setFunction(NodeProto.Function.NODE);
		via5Node_node.setHoldsOutline();
		via5Node_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Via-6-Node */
		PrimitiveNode via6Node_node = PrimitiveNode.newInstance("Via-6-Node", this, 1.4, 1.3, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(via6_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		via6Node_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, via6Node_node, new ArcProto[0], "via-6", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		via6Node_node.setFunction(NodeProto.Function.NODE);
		via6Node_node.setHoldsOutline();
		via6Node_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Via-7-Node */
		PrimitiveNode via7Node_node = PrimitiveNode.newInstance("Via-7-Node", this, 3.6, 3.6, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(via7_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		via7Node_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, via7Node_node, new ArcProto[0], "via-7", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		via7Node_node.setFunction(NodeProto.Function.NODE);
		via7Node_node.setHoldsOutline();
		via7Node_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Via-8-Node */
		PrimitiveNode via8Node_node = PrimitiveNode.newInstance("Via-8-Node", this, 3.6, 3.6, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(via8_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		via8Node_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, via8Node_node, new ArcProto[0], "via-8", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		via8Node_node.setFunction(NodeProto.Function.NODE);
		via8Node_node.setHoldsOutline();
		via8Node_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** P-Well-Node */
		PrimitiveNode pWellNode_node = PrimitiveNode.newInstance("P-Well-Node", this, 5.5, 5.5, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(pWell_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		pWellNode_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, pWellNode_node, new ArcProto[] {pActive_arc}, "well", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		pWellNode_node.setFunction(NodeProto.Function.NODE);
		pWellNode_node.setHoldsOutline();
		pWellNode_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** N-Well-Node */
		PrimitiveNode nWellNode_node = PrimitiveNode.newInstance("N-Well-Node", this, 5.5, 5.5, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(nWell_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		nWellNode_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, nWellNode_node, new ArcProto[] {pActive_arc}, "well", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		nWellNode_node.setFunction(NodeProto.Function.NODE);
		nWellNode_node.setHoldsOutline();
		nWellNode_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Passivation-Node */
		PrimitiveNode passivationNode_node = PrimitiveNode.newInstance("Passivation-Node", this, 8.0, 8.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(passivation_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		passivationNode_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, passivationNode_node, new ArcProto[0], "passivation", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		passivationNode_node.setFunction(NodeProto.Function.NODE);
		passivationNode_node.setHoldsOutline();
		passivationNode_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Pad-Frame-Node */
		PrimitiveNode padFrameNode_node = PrimitiveNode.newInstance("Pad-Frame-Node", this, 8.0, 8.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(padFrame_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		padFrameNode_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, padFrameNode_node, new ArcProto[0], "pad-frame", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		padFrameNode_node.setFunction(NodeProto.Function.NODE);
		padFrameNode_node.setHoldsOutline();
		padFrameNode_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** P-Active-Well-Node */
		PrimitiveNode pActiveWellNode_node = PrimitiveNode.newInstance("P-Active-Well-Node", this, 5.5, 5.5, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(pActiveWell_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		pActiveWellNode_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, pActiveWellNode_node, new ArcProto[0], "p-active-well", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		pActiveWellNode_node.setFunction(NodeProto.Function.NODE);
		pActiveWellNode_node.setHoldsOutline();
		pActiveWellNode_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Polysilicon-Transistor-Node */
		PrimitiveNode polyTransistorNode_node = PrimitiveNode.newInstance("Transistor-Poly-Node", this, 1.0, 1.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(transistorPoly_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		polyTransistorNode_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, polyTransistorNode_node, new ArcProto[] {poly_arc}, "trans-poly-1", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		polyTransistorNode_node.setFunction(NodeProto.Function.NODE);
		polyTransistorNode_node.setHoldsOutline();
		polyTransistorNode_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Silicide-Block-Node */
		PrimitiveNode silicideBlockNode_node = PrimitiveNode.newInstance("Silicide-Block-Node", this, 2.0, 2.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(transistorPoly_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		silicideBlockNode_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, silicideBlockNode_node, new ArcProto[] {poly_arc}, "silicide-block", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		silicideBlockNode_node.setFunction(NodeProto.Function.NODE);
		silicideBlockNode_node.setHoldsOutline();
		silicideBlockNode_node.setSpecialType(PrimitiveNode.POLYGONAL);

		// The pure layer nodes
		metal1_lay.setPureLayerNode(metal1Node_node);					// Metal-1
		metal2_lay.setPureLayerNode(metal2Node_node);					// Metal-2
		metal3_lay.setPureLayerNode(metal3Node_node);					// Metal-3
		metal4_lay.setPureLayerNode(metal4Node_node);					// Metal-4
		metal5_lay.setPureLayerNode(metal5Node_node);					// Metal-5
		metal6_lay.setPureLayerNode(metal6Node_node);					// Metal-6
		metal7_lay.setPureLayerNode(metal7Node_node);					// Metal-7
		metal8_lay.setPureLayerNode(metal8Node_node);					// Metal-8
		metal9_lay.setPureLayerNode(metal9Node_node);					// Metal-9
		poly_lay.setPureLayerNode(polyNode_node);						// Polysilicon
		pActive_lay.setPureLayerNode(pActiveNode_node);					// P-Active
		nActive_lay.setPureLayerNode(nActiveNode_node);					// N-Active
		pSelect_lay.setPureLayerNode(pSelectNode_node);					// P-Select
		nSelect_lay.setPureLayerNode(nSelectNode_node);					// N-Select
		pWell_lay.setPureLayerNode(pWellNode_node);						// P-Well
		nWell_lay.setPureLayerNode(nWellNode_node);						// N-Well
		polyCut_lay.setPureLayerNode(polyCutNode_node);					// Poly-Cut
		activeCut_lay.setPureLayerNode(activeCutNode_node);				// Active-Cut
		via1_lay.setPureLayerNode(via1Node_node);						// Via-1
		via2_lay.setPureLayerNode(via2Node_node);						// Via-2
		via3_lay.setPureLayerNode(via3Node_node);						// Via-3
		via4_lay.setPureLayerNode(via4Node_node);						// Via-4
		via5_lay.setPureLayerNode(via5Node_node);						// Via-5
		via6_lay.setPureLayerNode(via6Node_node);						// Via-6
		via7_lay.setPureLayerNode(via7Node_node);						// Via-7
		via8_lay.setPureLayerNode(via8Node_node);						// Via-8
		passivation_lay.setPureLayerNode(passivationNode_node);			// Passivation
		transistorPoly_lay.setPureLayerNode(polyTransistorNode_node);	// Transistor-Poly
		pActiveWell_lay.setPureLayerNode(pActiveWellNode_node);			// P-Active-Well
		silicideBlock_lay.setPureLayerNode(silicideBlockNode_node);		// Silicide-Block
		padFrame_lay.setPureLayerNode(padFrameNode_node);				// Pad-Frame
	}

	/******************** SUPPORT METHODS ********************/

	/**
	 * Method for initializing this technology.
	 */
	public void init()
	{
	}

	/**
	 * Method to initialize this Technology.
	 * The method must be public and static so that it can be invoked by reflection.
	 * Reflection is used on this technology because it contains sensitive information
	 * and may not be part of a public build.
	 */
	public static void setItUp()
	{
		tech.setup();
	}

}
