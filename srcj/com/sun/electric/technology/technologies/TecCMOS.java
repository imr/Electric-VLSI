/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: TecCMOS.java
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

import java.awt.Point;
import java.awt.geom.Point2D;

/**
 * This is the Complementary MOS (old, N-Well, from Griswold) technology.
 */
public class TecCMOS extends Technology
{
	public static final TecCMOS tech = new TecCMOS();
	// -------------------- private and protected methods ------------------------
	private TecCMOS()
	{
		setTechName("cmos");
		setTechDesc("Complementary MOS (old, N-Well, from Griswold)");
		setScale(4000);
		setNoNegatedArcs();
		setStaticTechnology();

		//**************************************** LAYERS ****************************************

		/** M layer */
		Layer M_lay = Layer.newInstance("Metal",
			new EGraphics(EGraphics.LAYERT1, EGraphics.COLORT1, EGraphics.SOLIDC, EGraphics.SOLIDC, 0,255,0,0.8,1,
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

		/** P layer */
		Layer P_lay = Layer.newInstance("Polysilicon",
			new EGraphics(EGraphics.LAYERT2, EGraphics.COLORT2, EGraphics.SOLIDC, EGraphics.SOLIDC, 255,190,6,0.8,1,
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

		/** D layer */
		Layer D_lay = Layer.newInstance("Diffusion",
			new EGraphics(EGraphics.LAYERT3, EGraphics.COLORT3, EGraphics.SOLIDC, EGraphics.SOLIDC, 170,140,30,0.8,1,
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

		/** P0 layer */
		Layer P0_lay = Layer.newInstance("P+",
			new EGraphics(EGraphics.LAYERT4, EGraphics.COLORT4, EGraphics.SOLIDC, EGraphics.SOLIDC, 16508,4096,1064,0.8,1,
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
			new EGraphics(EGraphics.LAYERO, EGraphics.BLACK, EGraphics.SOLIDC, EGraphics.SOLIDC, 180,130,0,0.8,1,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** OC layer */
		Layer OC_lay = Layer.newInstance("Ohmic-Cut",
			new EGraphics(EGraphics.LAYERO, EGraphics.BLACK, EGraphics.SOLIDC, EGraphics.SOLIDC, 180,130,0,0.8,1,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** PW layer */
		Layer PW_lay = Layer.newInstance("P-Well",
			new EGraphics(EGraphics.LAYERT5, EGraphics.COLORT5, EGraphics.SOLIDC, EGraphics.SOLIDC, 0,0,0,0.8,1,
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

		/** O layer */
		Layer O_lay = Layer.newInstance("Overglass",
			new EGraphics(EGraphics.LAYERO, EGraphics.DGRAY, EGraphics.SOLIDC, EGraphics.SOLIDC, 0,0,0,0.8,1,
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

		/** PM layer */
		Layer PM_lay = Layer.newInstance("Pseudo-Metal",
			new EGraphics(EGraphics.LAYERT1, EGraphics.COLORT1, EGraphics.SOLIDC, EGraphics.SOLIDC, 0,255,0,0.8,1,
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

		/** PP layer */
		Layer PP_lay = Layer.newInstance("Pseudo-Polysilicon",
			new EGraphics(EGraphics.LAYERT2, EGraphics.COLORT2, EGraphics.SOLIDC, EGraphics.SOLIDC, 255,190,6,0.8,1,
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

		/** PD layer */
		Layer PD_lay = Layer.newInstance("Pseudo-Diffusion",
			new EGraphics(EGraphics.LAYERT3, EGraphics.COLORT3, EGraphics.SOLIDC, EGraphics.SOLIDC, 170,140,30,0.8,1,
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

		/** PP0 layer */
		Layer PP0_lay = Layer.newInstance("Pseudo-P+",
			new EGraphics(EGraphics.LAYERT4, EGraphics.COLORT4, EGraphics.SOLIDC, EGraphics.SOLIDC, 16508,4096,1064,0.8,1,
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

		/** PPW layer */
		Layer PPW_lay = Layer.newInstance("Pseudo-P-Well",
			new EGraphics(EGraphics.LAYERT5, EGraphics.COLORT5, EGraphics.SOLIDC, EGraphics.SOLIDC, 0,0,0,0.8,1,
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
		M_lay.setFunction(Layer.Function.METAL1);													// Metal
		P_lay.setFunction(Layer.Function.POLY1);													// Polysilicon
		D_lay.setFunction(Layer.Function.DIFF);														// Diffusion
		P0_lay.setFunction(Layer.Function.IMPLANT, Layer.Function.PTYPE);							// P+
		CC_lay.setFunction(Layer.Function.CONTACT1);												// Contact-Cut
		OC_lay.setFunction(Layer.Function.CONTACT2);												// Ohmic-Cut
		PW_lay.setFunction(Layer.Function.WELL, Layer.Function.PTYPE);								// P-Well
		O_lay.setFunction(Layer.Function.OVERGLASS);												// Overglass
		T_lay.setFunction(Layer.Function.TRANSISTOR, Layer.Function.PSEUDO);						// Transistor
		PM_lay.setFunction(Layer.Function.METAL1, Layer.Function.PSEUDO);							// Pseudo-Metal
		PP_lay.setFunction(Layer.Function.POLY1, Layer.Function.PSEUDO);							// Pseudo-Polysilicon
		PD_lay.setFunction(Layer.Function.DIFF, Layer.Function.PSEUDO);								// Pseudo-Diffusion
		PP0_lay.setFunction(Layer.Function.IMPLANT, Layer.Function.PTYPE|Layer.Function.PSEUDO);	// Pseudo-P+
		PPW_lay.setFunction(Layer.Function.WELL, Layer.Function.PTYPE|Layer.Function.PSEUDO);		// Pseudo-P-Well

		// The CIF names
		M_lay.setCIFLayer("CM");		// Metal
		P_lay.setCIFLayer("CP");		// Polysilicon
		D_lay.setCIFLayer("CD");		// Diffusion
		P0_lay.setCIFLayer("CS");		// P+
		CC_lay.setCIFLayer("CC");		// Contact-Cut
		OC_lay.setCIFLayer("CC");		// Ohmic-Cut
		PW_lay.setCIFLayer("CW");		// P-Well
		O_lay.setCIFLayer("CG");		// Overglass
		T_lay.setCIFLayer("");			// Transistor
		PM_lay.setCIFLayer("");			// Pseudo-Metal
		PP_lay.setCIFLayer("");			// Pseudo-Polysilicon
		PD_lay.setCIFLayer("");			// Pseudo-Diffusion
		PP0_lay.setCIFLayer("");		// Pseudo-P+
		PPW_lay.setCIFLayer("");		// Pseudo-P-Well

		// The DXF names
		M_lay.setDXFLayer("");			// Metal
		P_lay.setDXFLayer("");			// Polysilicon
		D_lay.setDXFLayer("");			// Diffusion
		P0_lay.setDXFLayer("");			// P+
		CC_lay.setDXFLayer("");			// Contact-Cut
		OC_lay.setDXFLayer("");			// Ohmic-Cut
		PW_lay.setDXFLayer("");			// P-Well
		O_lay.setDXFLayer("");			// Overglass
		T_lay.setDXFLayer("");			// Transistor
		PM_lay.setDXFLayer("");			// Pseudo-Metal
		PP_lay.setDXFLayer("");			// Pseudo-Polysilicon
		PD_lay.setDXFLayer("");			// Pseudo-Diffusion
		PP0_lay.setDXFLayer("");		// Pseudo-P+
		PPW_lay.setDXFLayer("");		// Pseudo-P-Well

		// The GDS names
		M_lay.setGDSLayer("");			// Metal
		P_lay.setGDSLayer("");			// Polysilicon
		D_lay.setGDSLayer("");			// Diffusion
		P0_lay.setGDSLayer("");			// P+
		CC_lay.setGDSLayer("");			// Contact-Cut
		OC_lay.setGDSLayer("");			// Ohmic-Cut
		PW_lay.setGDSLayer("");			// P-Well
		O_lay.setGDSLayer("");			// Overglass
		T_lay.setGDSLayer("");			// Transistor
		PM_lay.setGDSLayer("");			// Pseudo-Metal
		PP_lay.setGDSLayer("");			// Pseudo-Polysilicon
		PD_lay.setGDSLayer("");			// Pseudo-Diffusion
		PP0_lay.setGDSLayer("");		// Pseudo-P+
		PPW_lay.setGDSLayer("");		// Pseudo-P-Well

		//******************** ARCS ********************

		/** Metal arc */
		PrimitiveArc Metal_arc = PrimitiveArc.newInstance(this, "Metal", 3, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(M_lay, 0, Poly.Type.FILLED)
		});
		Metal_arc.setFunction(PrimitiveArc.Function.METAL1);
		Metal_arc.setFixedAngle();
		Metal_arc.setWipable();
		Metal_arc.setAngleIncrement(90);

		/** Polysilicon arc */
		PrimitiveArc Polysilicon_arc = PrimitiveArc.newInstance(this, "Polysilicon", 2, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(P_lay, 0, Poly.Type.FILLED)
		});
		Polysilicon_arc.setFunction(PrimitiveArc.Function.POLY1);
		Polysilicon_arc.setFixedAngle();
		Polysilicon_arc.setWipable();
		Polysilicon_arc.setAngleIncrement(90);

		/** Diffusion-p arc */
		PrimitiveArc Diffusion_p_arc = PrimitiveArc.newInstance(this, "Diffusion-p", 6, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(D_lay, 4, Poly.Type.FILLED),
			new Technology.ArcLayer(P0_lay, 0, Poly.Type.FILLED)
		});
		Diffusion_p_arc.setFunction(PrimitiveArc.Function.DIFFP);
		Diffusion_p_arc.setFixedAngle();
		Diffusion_p_arc.setWipable();
		Diffusion_p_arc.setAngleIncrement(90);
		Diffusion_p_arc.setWidthOffset(0);

		/** Diffusion-well arc */
		PrimitiveArc Diffusion_well_arc = PrimitiveArc.newInstance(this, "Diffusion-well", 8, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(D_lay, 6, Poly.Type.FILLED),
			new Technology.ArcLayer(PW_lay, 0, Poly.Type.FILLED)
		});
		Diffusion_well_arc.setFunction(PrimitiveArc.Function.DIFFN);
		Diffusion_well_arc.setFixedAngle();
		Diffusion_well_arc.setWipable();
		Diffusion_well_arc.setAngleIncrement(90);
		Diffusion_well_arc.setWidthOffset(0);

		//******************** RECTANGLE DESCRIPTIONS ********************

		Technology.TechPoint [] box_1 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromCenter(1), EdgeV.fromBottom(3)),
			new Technology.TechPoint(EdgeH.fromRight(1), EdgeV.fromTop(3)),
		};
		Technology.TechPoint [] box_2 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(2), EdgeV.fromBottom(2)),
			new Technology.TechPoint(EdgeH.RIGHTEDGE, EdgeV.fromTop(2)),
		};
		Technology.TechPoint [] box_3 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.LEFTEDGE, EdgeV.BOTTOMEDGE),
			new Technology.TechPoint(EdgeH.fromCenter(1), EdgeV.TOPEDGE),
		};
		Technology.TechPoint [] box_4 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(3), EdgeV.fromBottom(3)),
			new Technology.TechPoint(EdgeH.fromCenter(1), EdgeV.fromTop(3)),
		};
		Technology.TechPoint [] box_5 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5)),
			new Technology.TechPoint(EdgeH.CENTER, EdgeV.fromTop(1.5)),
		};
		Technology.TechPoint [] box_6 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(4), EdgeV.fromBottom(4)),
			new Technology.TechPoint(EdgeH.CENTER, EdgeV.fromTop(4)),
		};
		Technology.TechPoint [] box_7 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.CENTER, EdgeV.fromBottom(4)),
			new Technology.TechPoint(EdgeH.fromRight(4), EdgeV.fromTop(4)),
		};
		Technology.TechPoint [] box_8 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(3), EdgeV.fromBottom(1)),
			new Technology.TechPoint(EdgeH.fromRight(3), EdgeV.CENTER),
		};
		Technology.TechPoint [] box_9 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(3), EdgeV.CENTER),
			new Technology.TechPoint(EdgeH.fromRight(3), EdgeV.fromTop(1)),
		};
		Technology.TechPoint [] box_10 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(3), EdgeV.fromBottom(1)),
			new Technology.TechPoint(EdgeH.fromRight(3), EdgeV.fromTop(1)),
		};
		Technology.TechPoint [] box_11 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(1), EdgeV.fromBottom(3)),
			new Technology.TechPoint(EdgeH.fromRight(1), EdgeV.fromTop(3)),
		};
		Technology.TechPoint [] box_12 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(2), EdgeV.BOTTOMEDGE),
			new Technology.TechPoint(EdgeH.fromRight(2), EdgeV.CENTER),
		};
		Technology.TechPoint [] box_13 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(2), EdgeV.CENTER),
			new Technology.TechPoint(EdgeH.fromRight(2), EdgeV.TOPEDGE),
		};
		Technology.TechPoint [] box_14 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(2), EdgeV.BOTTOMEDGE),
			new Technology.TechPoint(EdgeH.fromRight(2), EdgeV.TOPEDGE),
		};
		Technology.TechPoint [] box_15 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.LEFTEDGE, EdgeV.fromBottom(2)),
			new Technology.TechPoint(EdgeH.RIGHTEDGE, EdgeV.fromTop(2)),
		};
		Technology.TechPoint [] box_16 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.fromLeft(1), EdgeV.fromBottom(1)),
			new Technology.TechPoint(EdgeH.fromLeft(3), EdgeV.fromBottom(3)),
		};

		//******************** NODES ********************

		/** Metal-Pin */
		PrimitiveNode mp_node = PrimitiveNode.newInstance("Metal-Pin", this, 3, 3, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(PM_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX)
			});
		mp_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, mp_node, new ArcProto [] {Metal_arc}, "metal", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
			});
		mp_node.setFunction(NodeProto.Function.PIN);
		mp_node.setArcsWipe();
		mp_node.setArcsShrink();

		/** Polysilicon-Pin */
		PrimitiveNode pp_node = PrimitiveNode.newInstance("Polysilicon-Pin", this, 2, 2, null,
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

		/** Diffusion-P-Pin */
		PrimitiveNode dpp_node = PrimitiveNode.newInstance("Diffusion-P-Pin", this, 6, 6, new SizeOffset(2, 2, 2, 2),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(PP0_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX),
				new Technology.NodeLayer(PD_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, Technology.TechPoint.IN2BOX)
			});
		dpp_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, dpp_node, new ArcProto [] {Diffusion_p_arc}, "diff-p", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(3), EdgeV.fromBottom(3), EdgeH.fromRight(3), EdgeV.fromTop(3))
			});
		dpp_node.setFunction(NodeProto.Function.PIN);
		dpp_node.setArcsWipe();
		dpp_node.setArcsShrink();

		/** Diffusion-Well-Pin */
		PrimitiveNode dwp_node = PrimitiveNode.newInstance("Diffusion-Well-Pin", this, 8, 8, new SizeOffset(3, 3, 3, 3),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(PPW_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX),
				new Technology.NodeLayer(PD_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, Technology.TechPoint.IN3BOX)
			});
		dwp_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, dwp_node, new ArcProto [] {Diffusion_well_arc}, "diff-w", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(4), EdgeV.fromBottom(4), EdgeH.fromRight(4), EdgeV.fromTop(4))
			});
		dwp_node.setFunction(NodeProto.Function.PIN);
		dwp_node.setArcsWipe();
		dwp_node.setArcsShrink();

		/** Metal-Polysilicon-Con */
		PrimitiveNode mpc_node = PrimitiveNode.newInstance("Metal-Polysilicon-Con", this, 4, 4, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(P_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX),
				new Technology.NodeLayer(M_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX),
				new Technology.NodeLayer(CC_lay, 0, Poly.Type.CLOSED, Technology.NodeLayer.BOX, box_16)
			});
		mpc_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, mpc_node, new ArcProto [] {Polysilicon_arc, Metal_arc}, "metal-poly", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(1), EdgeV.fromBottom(1), EdgeH.fromRight(1), EdgeV.fromTop(1))
			});
		mpc_node.setFunction(NodeProto.Function.CONTACT);

		/** Metal-Diff-P-Con */
		PrimitiveNode mdpc_node = PrimitiveNode.newInstance("Metal-Diff-P-Con", this, 8, 8, new SizeOffset(2, 2, 2, 2),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(P0_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX),
				new Technology.NodeLayer(M_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.IN2BOX),
				new Technology.NodeLayer(D_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.IN2BOX),
				new Technology.NodeLayer(CC_lay, 0, Poly.Type.CLOSED, Technology.NodeLayer.BOX, box_16)
			});
		mdpc_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, mdpc_node, new ArcProto [] {Diffusion_p_arc, Metal_arc}, "metal-diff-p", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(3), EdgeV.fromBottom(3), EdgeH.fromRight(3), EdgeV.fromTop(3))
			});
		mdpc_node.setFunction(NodeProto.Function.CONTACT);

		/** Metal-Diff-Well-Con */
		PrimitiveNode mdwc_node = PrimitiveNode.newInstance("Metal-Diff-Well-Con", this, 10, 10, new SizeOffset(3, 3, 3, 3),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(M_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.IN3BOX),
				new Technology.NodeLayer(D_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.IN3BOX),
				new Technology.NodeLayer(PW_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX),
				new Technology.NodeLayer(CC_lay, 0, Poly.Type.CLOSED, Technology.NodeLayer.BOX, box_16)
			});
		mdwc_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, mdwc_node, new ArcProto [] {Diffusion_well_arc, Metal_arc}, "metal-diff-w", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(4), EdgeV.fromBottom(4), EdgeH.fromRight(4), EdgeV.fromTop(4))
			});
		mdwc_node.setFunction(NodeProto.Function.CONTACT);

		/** Transistor */
		PrimitiveNode t_node = PrimitiveNode.newInstance("Transistor", this, 6, 6, new SizeOffset(2, 2, 2, 2),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(P_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_15),
				new Technology.NodeLayer(D_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_14),
				new Technology.NodeLayer(P0_lay, -1, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX)
			});
		t_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, t_node, new ArcProto [] {Polysilicon_arc}, "trans-poly-left", 180,85, 1, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(1), EdgeV.fromBottom(3), EdgeH.fromLeft(1), EdgeV.fromTop(3)),
				PrimitivePort.newInstance(this, t_node, new ArcProto [] {Diffusion_p_arc}, "trans-diff-top", 90,85, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(3), EdgeV.fromTop(1), EdgeH.fromRight(3), EdgeV.fromTop(1)),
				PrimitivePort.newInstance(this, t_node, new ArcProto [] {Polysilicon_arc}, "trans-poly-right", 0,85, 1, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromRight(1), EdgeV.fromBottom(3), EdgeH.fromRight(1), EdgeV.fromTop(3)),
				PrimitivePort.newInstance(this, t_node, new ArcProto [] {Diffusion_p_arc}, "trans-diff-bottom", 270,85, 2, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(3), EdgeV.fromBottom(1), EdgeH.fromRight(3), EdgeV.fromBottom(1))
			});
		t_node.setFunction(NodeProto.Function.TRANMOS);
		t_node.setHoldsOutline();
		t_node.setShrunk();

		/** Transistor-Well */
		PrimitiveNode tw_node = PrimitiveNode.newInstance("Transistor-Well", this, 8, 8, new SizeOffset(3, 3, 3, 3),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(P_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_11),
				new Technology.NodeLayer(D_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_10),
				new Technology.NodeLayer(PW_lay, -1, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX)
			});
		tw_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, tw_node, new ArcProto [] {Polysilicon_arc}, "transw-poly-left", 180,85, 2, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(2), EdgeV.fromBottom(4), EdgeH.fromLeft(2), EdgeV.fromTop(4)),
				PrimitivePort.newInstance(this, tw_node, new ArcProto [] {Diffusion_well_arc}, "transw-diff-top", 90,85, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(4), EdgeV.fromTop(2), EdgeH.fromRight(4), EdgeV.fromTop(2)),
				PrimitivePort.newInstance(this, tw_node, new ArcProto [] {Polysilicon_arc}, "transw-poly-right", 0,85, 2, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromRight(2), EdgeV.fromBottom(4), EdgeH.fromRight(2), EdgeV.fromTop(4)),
				PrimitivePort.newInstance(this, tw_node, new ArcProto [] {Diffusion_well_arc}, "transw-diff-bottom", 270,85, 1, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(4), EdgeV.fromBottom(2), EdgeH.fromRight(4), EdgeV.fromBottom(2))
			});
		tw_node.setFunction(NodeProto.Function.TRAPMOS);
		tw_node.setHoldsOutline();
		tw_node.setShrunk();

		/** Metal-Diff-Split-Cut */
		PrimitiveNode mdsc_node = PrimitiveNode.newInstance("Metal-Diff-Split-Cut", this, 14, 10, new SizeOffset(3, 3, 3, 3),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(D_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.IN3BOX),
				new Technology.NodeLayer(P0_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_5),
				new Technology.NodeLayer(PW_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX),
				new Technology.NodeLayer(M_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.IN3BOX),
				new Technology.NodeLayer(CC_lay, 1, Poly.Type.CLOSED, Technology.NodeLayer.BOX, box_7),
				new Technology.NodeLayer(OC_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, box_6)
			});
		mdsc_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, mdsc_node, new ArcProto [] {Metal_arc}, "metal-diff-splw-l", 180,90, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(4), EdgeV.fromBottom(4), EdgeH.fromCenter(-1), EdgeV.fromTop(4)),
				PrimitivePort.newInstance(this, mdsc_node, new ArcProto [] {Diffusion_well_arc, Metal_arc}, "metal-diff-splw-r", 0,90, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromCenter(1), EdgeV.fromBottom(4), EdgeH.fromRight(4), EdgeV.fromTop(4))
			});
		mdsc_node.setFunction(NodeProto.Function.WELL);

		/** Metal-Diff-SplitN-Cut */
		PrimitiveNode mdsc0_node = PrimitiveNode.newInstance("Metal-Diff-SplitN-Cut", this, 10, 8, new SizeOffset(2, 2, 1, 1),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(P0_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_3),
				new Technology.NodeLayer(D_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_2),
				new Technology.NodeLayer(M_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_2),
				new Technology.NodeLayer(CC_lay, 0, Poly.Type.CLOSED, Technology.NodeLayer.BOX, box_4),
				new Technology.NodeLayer(OC_lay, 1, Poly.Type.CROSSED, Technology.NodeLayer.BOX, box_1)
			});
		mdsc0_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, mdsc0_node, new ArcProto [] {Diffusion_p_arc, Metal_arc}, "metal-diff-splp-l", 0,90, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(3), EdgeV.fromBottom(3), EdgeH.CENTER, EdgeV.fromTop(3)),
				PrimitivePort.newInstance(this, mdsc0_node, new ArcProto [] {Metal_arc}, "metal-diff-splp-r", 180,90, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromCenter(2), EdgeV.fromBottom(3), EdgeH.fromRight(1), EdgeV.fromTop(3))
			});
		mdsc0_node.setFunction(NodeProto.Function.SUBSTRATE);

		/** Metal-Node */
		PrimitiveNode mn_node = PrimitiveNode.newInstance("Metal-Node", this, 3, 3, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(M_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX)
			});
		mn_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, mn_node, new ArcProto [] {Metal_arc}, "metal", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
			});
		mn_node.setFunction(NodeProto.Function.NODE);
		mn_node.setHoldsOutline();

		/** Polysilicon-Node */
		PrimitiveNode pn_node = PrimitiveNode.newInstance("Polysilicon-Node", this, 2, 2, null,
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

		/** Diffusion-Node */
		PrimitiveNode dn_node = PrimitiveNode.newInstance("Diffusion-Node", this, 2, 2, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(D_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX)
			});
		dn_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, dn_node, new ArcProto [] {}, "diffusion", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(1), EdgeV.fromBottom(1), EdgeH.fromRight(1), EdgeV.fromTop(1))
			});
		dn_node.setFunction(NodeProto.Function.NODE);
		dn_node.setHoldsOutline();

		/** P-Node */
		PrimitiveNode pn0_node = PrimitiveNode.newInstance("P-Node", this, 2, 2, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(P0_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX)
			});
		pn0_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, pn0_node, new ArcProto [] {}, "p+", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(1), EdgeV.fromBottom(1), EdgeH.fromRight(1), EdgeV.fromTop(1))
			});
		pn0_node.setFunction(NodeProto.Function.NODE);
		pn0_node.setHoldsOutline();

		/** Cut-Node */
		PrimitiveNode cn_node = PrimitiveNode.newInstance("Cut-Node", this, 2, 2, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(CC_lay, 0, Poly.Type.CLOSED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX)
			});
		cn_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, cn_node, new ArcProto [] {}, "cut", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(1), EdgeV.fromBottom(1), EdgeH.fromRight(1), EdgeV.fromTop(1))
			});
		cn_node.setFunction(NodeProto.Function.NODE);
		cn_node.setHoldsOutline();

		/** Ohmic-Cut-Node */
		PrimitiveNode ocn_node = PrimitiveNode.newInstance("Ohmic-Cut-Node", this, 2, 2, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(OC_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX)
			});
		ocn_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, ocn_node, new ArcProto [] {}, "ohmic-cut", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(1), EdgeV.fromBottom(1), EdgeH.fromRight(1), EdgeV.fromTop(1))
			});
		ocn_node.setFunction(NodeProto.Function.NODE);
		ocn_node.setHoldsOutline();

		/** Well-Node */
		PrimitiveNode wn_node = PrimitiveNode.newInstance("Well-Node", this, 4, 4, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(PW_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX)
			});
		wn_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, wn_node, new ArcProto [] {}, "well", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(1), EdgeV.fromBottom(1), EdgeH.fromRight(1), EdgeV.fromTop(1))
			});
		wn_node.setFunction(NodeProto.Function.NODE);
		wn_node.setHoldsOutline();

		/** Overglass-Node */
		PrimitiveNode on_node = PrimitiveNode.newInstance("Overglass-Node", this, 2, 2, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(O_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX)
			});
		on_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, on_node, new ArcProto [] {}, "overglass", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(1), EdgeV.fromBottom(1), EdgeH.fromRight(1), EdgeV.fromTop(1))
			});
		on_node.setFunction(NodeProto.Function.NODE);
		on_node.setHoldsOutline();
	};
}
