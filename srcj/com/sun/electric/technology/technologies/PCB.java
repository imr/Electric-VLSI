/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: PCB.java
 * Printed-Circuit Board technology description
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
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.technology.DRCRules;
import com.sun.electric.technology.EdgeH;
import com.sun.electric.technology.EdgeV;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveArc;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitivePort;
import com.sun.electric.technology.SizeOffset;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.utils.MOSRules;

import java.awt.Color;

/**
 * This is the Printed Circuit Board (eight-layer) Technology.
 */
public class PCB extends Technology
{
	/** the Printed Circuit Board (eight-layer) Technology object. */	public static final PCB tech = new PCB();

	// -------------------- private and protected methods ------------------------
	private PCB()
	{
		setTechName("pcb");
		setTechDesc("Printed Circuit Board (eight-layer)");
		setFactoryScale(1270000, true);   // in nanometers: really 1270 microns
		setNoNegatedArcs();
		setStaticTechnology();
		setFactoryTransparentLayers(new Color []
		{
			new Color(  0,  0,  0), // layer 1
			new Color(255,  0,  0), // layer 2
			new Color(  0,255,  0), // layer 3
			new Color(  0,  0,255), // layer 4
			new Color(255,255,  0), // layer 5
		});

		//**************************************** LAYERS ****************************************

		/** S layer */
		Layer S_lay = Layer.newInstance(this, "Signal1",
			new EGraphics(EGraphics.SOLID, EGraphics.SOLID, EGraphics.TRANSPARENT_1, 0,255,0, 0.8,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** S0 layer */
		Layer S0_lay = Layer.newInstance(this, "Signal2",
			new EGraphics(EGraphics.SOLID, EGraphics.SOLID, EGraphics.TRANSPARENT_2, 0,0,255, 0.8,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** S1 layer */
		Layer S1_lay = Layer.newInstance(this, "Signal3",
			new EGraphics(EGraphics.SOLID, EGraphics.SOLID, EGraphics.TRANSPARENT_3, 255,255,0, 0.8,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** S2 layer */
		Layer S2_lay = Layer.newInstance(this, "Signal4",
			new EGraphics(EGraphics.SOLID, EGraphics.SOLID, EGraphics.TRANSPARENT_4, 116,0,0, 0.8,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** S3 layer */
		Layer S3_lay = Layer.newInstance(this, "Signal5",
			new EGraphics(EGraphics.SOLID, EGraphics.SOLID, EGraphics.TRANSPARENT_5, 0,0,0, 0.8,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** S4 layer */
		Layer S4_lay = Layer.newInstance(this, "Signal6",
			new EGraphics(EGraphics.SOLID, EGraphics.SOLID, 0, 255,255,0, 0.8,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** S5 layer */
		Layer S5_lay = Layer.newInstance(this, "Signal7",
			new EGraphics(EGraphics.SOLID, EGraphics.SOLID, 0, 255,190,6, 0.8,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** S6 layer */
		Layer S6_lay = Layer.newInstance(this, "Signal8",
			new EGraphics(EGraphics.SOLID, EGraphics.SOLID, 0, 0,255,255, 0.8,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** P layer */
		Layer P_lay = Layer.newInstance(this, "Power1",
			new EGraphics(EGraphics.SOLID, EGraphics.SOLID, EGraphics.TRANSPARENT_1, 0,255,0, 0.8,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** P0 layer */
		Layer P0_lay = Layer.newInstance(this, "Power2",
			new EGraphics(EGraphics.SOLID, EGraphics.SOLID, EGraphics.TRANSPARENT_2, 0,0,255, 0.8,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** P1 layer */
		Layer P1_lay = Layer.newInstance(this, "Power3",
			new EGraphics(EGraphics.SOLID, EGraphics.SOLID, EGraphics.TRANSPARENT_3, 255,255,0, 0.8,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** P2 layer */
		Layer P2_lay = Layer.newInstance(this, "Power4",
			new EGraphics(EGraphics.SOLID, EGraphics.SOLID, EGraphics.TRANSPARENT_4, 116,0,0, 0.8,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** P3 layer */
		Layer P3_lay = Layer.newInstance(this, "Power5",
			new EGraphics(EGraphics.SOLID, EGraphics.SOLID, EGraphics.TRANSPARENT_5, 0,0,0, 0.8,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** P4 layer */
		Layer P4_lay = Layer.newInstance(this, "Power6",
			new EGraphics(EGraphics.SOLID, EGraphics.SOLID, 0, 255,255,0, 0.8,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** P5 layer */
		Layer P5_lay = Layer.newInstance(this, "Power7",
			new EGraphics(EGraphics.SOLID, EGraphics.SOLID, 0, 255,190,6, 0.8,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** P6 layer */
		Layer P6_lay = Layer.newInstance(this, "Power8",
			new EGraphics(EGraphics.SOLID, EGraphics.SOLID, 0, 0,255,255, 0.8,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** T layer */
		Layer T_lay = Layer.newInstance(this, "TopSilk",
			new EGraphics(EGraphics.SOLID, EGraphics.SOLID, 0, 230,230,230, 0.8,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** B layer */
		Layer B_lay = Layer.newInstance(this, "BottomSilk",
			new EGraphics(EGraphics.SOLID, EGraphics.SOLID, 0, 100,100,100, 0.8,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** T0 layer */
		Layer T0_lay = Layer.newInstance(this, "TopSolder",
			new EGraphics(EGraphics.SOLID, EGraphics.SOLID, 0, 175,255,175, 0.8,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** B0 layer */
		Layer B0_lay = Layer.newInstance(this, "BottomSolder",
			new EGraphics(EGraphics.SOLID, EGraphics.SOLID, 0, 89,159,85, 0.8,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** D layer */
		Layer D_lay = Layer.newInstance(this, "Drill",
			new EGraphics(EGraphics.SOLID, EGraphics.SOLID, 0, 2,15,159, 0.8,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** D0 layer */
		Layer D0_lay = Layer.newInstance(this, "DrillNonPlated",
			new EGraphics(EGraphics.SOLID, EGraphics.SOLID, 0, 150,150,255, 0.8,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** D1 layer */
		Layer D1_lay = Layer.newInstance(this, "Drawing",
			new EGraphics(EGraphics.SOLID, EGraphics.SOLID, 0, 255,150,150, 0.8,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		// The layer functions
		S_lay.setFunction(Layer.Function.METAL1);		// Signal1
		S0_lay.setFunction(Layer.Function.METAL2);		// Signal2
		S1_lay.setFunction(Layer.Function.METAL3);		// Signal3
		S2_lay.setFunction(Layer.Function.METAL4);		// Signal4
		S3_lay.setFunction(Layer.Function.METAL5);		// Signal5
		S4_lay.setFunction(Layer.Function.METAL6);		// Signal6
		S5_lay.setFunction(Layer.Function.METAL7);		// Signal7
		S6_lay.setFunction(Layer.Function.METAL8);		// Signal8
		P_lay.setFunction(Layer.Function.METAL1);		// Power1
		P0_lay.setFunction(Layer.Function.METAL2);		// Power2
		P1_lay.setFunction(Layer.Function.METAL3);		// Power3
		P2_lay.setFunction(Layer.Function.METAL4);		// Power4
		P3_lay.setFunction(Layer.Function.METAL5);		// Power5
		P4_lay.setFunction(Layer.Function.METAL6);		// Power6
		P5_lay.setFunction(Layer.Function.METAL7);		// Power7
		P6_lay.setFunction(Layer.Function.METAL8);		// Power8
		T_lay.setFunction(Layer.Function.ART);		// TopSilk
		B_lay.setFunction(Layer.Function.ART);		// BottomSilk
		T0_lay.setFunction(Layer.Function.METAL1);		// TopSolder
		B0_lay.setFunction(Layer.Function.METAL8);		// BottomSolder
		D_lay.setFunction(Layer.Function.CONTACT1, Layer.Function.CONMETAL);		// Drill
		D0_lay.setFunction(Layer.Function.ART);		// DrillNonPlated
		D1_lay.setFunction(Layer.Function.ART);		// Drawing

		// The CIF names
		S_lay.setFactoryCIFLayer("PC1");		// Signal1
		S0_lay.setFactoryCIFLayer("PC2");		// Signal2
		S1_lay.setFactoryCIFLayer("PC3");		// Signal3
		S2_lay.setFactoryCIFLayer("PC4");		// Signal4
		S3_lay.setFactoryCIFLayer("PC5");		// Signal5
		S4_lay.setFactoryCIFLayer("PC6");		// Signal6
		S5_lay.setFactoryCIFLayer("PC7");		// Signal7
		S6_lay.setFactoryCIFLayer("PC8");		// Signal8
		P_lay.setFactoryCIFLayer("PN1");		// Power1
		P0_lay.setFactoryCIFLayer("PN2");		// Power2
		P1_lay.setFactoryCIFLayer("PN3");		// Power3
		P2_lay.setFactoryCIFLayer("PN4");		// Power4
		P3_lay.setFactoryCIFLayer("PN5");		// Power5
		P4_lay.setFactoryCIFLayer("PN6");		// Power6
		P5_lay.setFactoryCIFLayer("PN7");		// Power7
		P6_lay.setFactoryCIFLayer("PN8");		// Power8
		T_lay.setFactoryCIFLayer("PSSC");		// TopSilk
		B_lay.setFactoryCIFLayer("PSSS");		// BottomSilk
		T0_lay.setFactoryCIFLayer("PSMC");		// TopSolder
		B0_lay.setFactoryCIFLayer("PSMS");		// BottomSolder
		D_lay.setFactoryCIFLayer("PD");		// Drill
		D0_lay.setFactoryCIFLayer("PDNP");		// DrillNonPlated
		D1_lay.setFactoryCIFLayer("PF");		// Drawing

		// The DXF names
		S_lay.setFactoryDXFLayer("");		// Signal1
		S0_lay.setFactoryDXFLayer("");		// Signal2
		S1_lay.setFactoryDXFLayer("");		// Signal3
		S2_lay.setFactoryDXFLayer("");		// Signal4
		S3_lay.setFactoryDXFLayer("");		// Signal5
		S4_lay.setFactoryDXFLayer("");		// Signal6
		S5_lay.setFactoryDXFLayer("");		// Signal7
		S6_lay.setFactoryDXFLayer("");		// Signal8
		P_lay.setFactoryDXFLayer("");		// Power1
		P0_lay.setFactoryDXFLayer("");		// Power2
		P1_lay.setFactoryDXFLayer("");		// Power3
		P2_lay.setFactoryDXFLayer("");		// Power4
		P3_lay.setFactoryDXFLayer("");		// Power5
		P4_lay.setFactoryDXFLayer("");		// Power6
		P5_lay.setFactoryDXFLayer("");		// Power7
		P6_lay.setFactoryDXFLayer("");		// Power8
		T_lay.setFactoryDXFLayer("");		// TopSilk
		B_lay.setFactoryDXFLayer("");		// BottomSilk
		T0_lay.setFactoryDXFLayer("");		// TopSolder
		B0_lay.setFactoryDXFLayer("");		// BottomSolder
		D_lay.setFactoryDXFLayer("");		// Drill
		D0_lay.setFactoryDXFLayer("");		// DrillNonPlated
		D1_lay.setFactoryDXFLayer("");		// Drawing

		// The GDS names
		S_lay.setFactoryGDSLayer("");		// Signal1
		S0_lay.setFactoryGDSLayer("");		// Signal2
		S1_lay.setFactoryGDSLayer("");		// Signal3
		S2_lay.setFactoryGDSLayer("");		// Signal4
		S3_lay.setFactoryGDSLayer("");		// Signal5
		S4_lay.setFactoryGDSLayer("");		// Signal6
		S5_lay.setFactoryGDSLayer("");		// Signal7
		S6_lay.setFactoryGDSLayer("");		// Signal8
		P_lay.setFactoryGDSLayer("");		// Power1
		P0_lay.setFactoryGDSLayer("");		// Power2
		P1_lay.setFactoryGDSLayer("");		// Power3
		P2_lay.setFactoryGDSLayer("");		// Power4
		P3_lay.setFactoryGDSLayer("");		// Power5
		P4_lay.setFactoryGDSLayer("");		// Power6
		P5_lay.setFactoryGDSLayer("");		// Power7
		P6_lay.setFactoryGDSLayer("");		// Power8
		T_lay.setFactoryGDSLayer("");		// TopSilk
		B_lay.setFactoryGDSLayer("");		// BottomSilk
		T0_lay.setFactoryGDSLayer("");		// TopSolder
		B0_lay.setFactoryGDSLayer("");		// BottomSolder
		D_lay.setFactoryGDSLayer("");		// Drill
		D0_lay.setFactoryGDSLayer("");		// DrillNonPlated
		D1_lay.setFactoryGDSLayer("");		// Drawing

		//******************** ARCS ********************

		/** Signal-1 arc */
		PrimitiveArc Signal_1_arc = PrimitiveArc.newInstance(this, "Signal-1", 0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(S_lay, 0, Poly.Type.FILLED)
		});
		Signal_1_arc.setFunction(PrimitiveArc.Function.METAL1);
		Signal_1_arc.setWipable();
		Signal_1_arc.setFactoryAngleIncrement(45);

		/** Signal-2 arc */
		PrimitiveArc Signal_2_arc = PrimitiveArc.newInstance(this, "Signal-2", 0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(S0_lay, 0, Poly.Type.FILLED)
		});
		Signal_2_arc.setFunction(PrimitiveArc.Function.METAL2);
		Signal_2_arc.setWipable();
		Signal_2_arc.setFactoryAngleIncrement(45);

		/** Signal-3 arc */
		PrimitiveArc Signal_3_arc = PrimitiveArc.newInstance(this, "Signal-3", 0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(S1_lay, 0, Poly.Type.FILLED)
		});
		Signal_3_arc.setFunction(PrimitiveArc.Function.METAL3);
		Signal_3_arc.setWipable();
		Signal_3_arc.setFactoryAngleIncrement(45);

		/** Signal-4 arc */
		PrimitiveArc Signal_4_arc = PrimitiveArc.newInstance(this, "Signal-4", 0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(S2_lay, 0, Poly.Type.FILLED)
		});
		Signal_4_arc.setFunction(PrimitiveArc.Function.METAL4);
		Signal_4_arc.setWipable();
		Signal_4_arc.setFactoryAngleIncrement(45);

		/** Signal-5 arc */
		PrimitiveArc Signal_5_arc = PrimitiveArc.newInstance(this, "Signal-5", 0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(S3_lay, 0, Poly.Type.FILLED)
		});
		Signal_5_arc.setFunction(PrimitiveArc.Function.METAL5);
		Signal_5_arc.setWipable();
		Signal_5_arc.setFactoryAngleIncrement(45);

		/** Signal-6 arc */
		PrimitiveArc Signal_6_arc = PrimitiveArc.newInstance(this, "Signal-6", 0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(S4_lay, 0, Poly.Type.FILLED)
		});
		Signal_6_arc.setFunction(PrimitiveArc.Function.METAL6);
		Signal_6_arc.setWipable();
		Signal_6_arc.setFactoryAngleIncrement(45);

		/** Signal-7 arc */
		PrimitiveArc Signal_7_arc = PrimitiveArc.newInstance(this, "Signal-7", 0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(S5_lay, 0, Poly.Type.FILLED)
		});
		Signal_7_arc.setFunction(PrimitiveArc.Function.METAL7);
		Signal_7_arc.setWipable();
		Signal_7_arc.setFactoryAngleIncrement(45);

		/** Signal-8 arc */
		PrimitiveArc Signal_8_arc = PrimitiveArc.newInstance(this, "Signal-8", 0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(S6_lay, 0, Poly.Type.FILLED)
		});
		Signal_8_arc.setFunction(PrimitiveArc.Function.METAL8);
		Signal_8_arc.setWipable();
		Signal_8_arc.setFactoryAngleIncrement(45);

		/** Power-1 arc */
		PrimitiveArc Power_1_arc = PrimitiveArc.newInstance(this, "Power-1", 0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(P_lay, 0, Poly.Type.FILLED)
		});
		Power_1_arc.setFunction(PrimitiveArc.Function.METAL1);
		Power_1_arc.setWipable();
		Power_1_arc.setFactoryAngleIncrement(45);

		/** Power-2 arc */
		PrimitiveArc Power_2_arc = PrimitiveArc.newInstance(this, "Power-2", 0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(P0_lay, 0, Poly.Type.FILLED)
		});
		Power_2_arc.setFunction(PrimitiveArc.Function.METAL2);
		Power_2_arc.setWipable();
		Power_2_arc.setFactoryAngleIncrement(45);

		/** Power-3 arc */
		PrimitiveArc Power_3_arc = PrimitiveArc.newInstance(this, "Power-3", 0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(P1_lay, 0, Poly.Type.FILLED)
		});
		Power_3_arc.setFunction(PrimitiveArc.Function.METAL3);
		Power_3_arc.setWipable();
		Power_3_arc.setFactoryAngleIncrement(45);

		/** Power-4 arc */
		PrimitiveArc Power_4_arc = PrimitiveArc.newInstance(this, "Power-4", 0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(P2_lay, 0, Poly.Type.FILLED)
		});
		Power_4_arc.setFunction(PrimitiveArc.Function.METAL4);
		Power_4_arc.setWipable();
		Power_4_arc.setFactoryAngleIncrement(45);

		/** Power-5 arc */
		PrimitiveArc Power_5_arc = PrimitiveArc.newInstance(this, "Power-5", 0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(P3_lay, 0, Poly.Type.FILLED)
		});
		Power_5_arc.setFunction(PrimitiveArc.Function.METAL5);
		Power_5_arc.setWipable();
		Power_5_arc.setFactoryAngleIncrement(45);

		/** Power-6 arc */
		PrimitiveArc Power_6_arc = PrimitiveArc.newInstance(this, "Power-6", 0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(P4_lay, 0, Poly.Type.FILLED)
		});
		Power_6_arc.setFunction(PrimitiveArc.Function.METAL6);
		Power_6_arc.setWipable();
		Power_6_arc.setFactoryAngleIncrement(45);

		/** Power-7 arc */
		PrimitiveArc Power_7_arc = PrimitiveArc.newInstance(this, "Power-7", 0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(P5_lay, 0, Poly.Type.FILLED)
		});
		Power_7_arc.setFunction(PrimitiveArc.Function.METAL7);
		Power_7_arc.setWipable();
		Power_7_arc.setFactoryAngleIncrement(45);

		/** Power-8 arc */
		PrimitiveArc Power_8_arc = PrimitiveArc.newInstance(this, "Power-8", 0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(P6_lay, 0, Poly.Type.FILLED)
		});
		Power_8_arc.setFunction(PrimitiveArc.Function.METAL8);
		Power_8_arc.setWipable();
		Power_8_arc.setFactoryAngleIncrement(45);

		/** Top-Silk arc */
		PrimitiveArc Top_Silk_arc = PrimitiveArc.newInstance(this, "Top-Silk", 0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(T_lay, 0, Poly.Type.FILLED)
		});
		Top_Silk_arc.setFunction(PrimitiveArc.Function.NONELEC);
		Top_Silk_arc.setWipable();
		Top_Silk_arc.setFactoryAngleIncrement(45);

		/** Bottom-Silk arc */
		PrimitiveArc Bottom_Silk_arc = PrimitiveArc.newInstance(this, "Bottom-Silk", 0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(B_lay, 0, Poly.Type.FILLED)
		});
		Bottom_Silk_arc.setFunction(PrimitiveArc.Function.NONELEC);
		Bottom_Silk_arc.setWipable();
		Bottom_Silk_arc.setFactoryAngleIncrement(45);

		/** Top-Solder arc */
		PrimitiveArc Top_Solder_arc = PrimitiveArc.newInstance(this, "Top-Solder", 0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(T0_lay, 0, Poly.Type.FILLED)
		});
		Top_Solder_arc.setFunction(PrimitiveArc.Function.NONELEC);
		Top_Solder_arc.setWipable();
		Top_Solder_arc.setFactoryAngleIncrement(45);

		/** Bottom-Solder arc */
		PrimitiveArc Bottom_Solder_arc = PrimitiveArc.newInstance(this, "Bottom-Solder", 0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(B0_lay, 0, Poly.Type.FILLED)
		});
		Bottom_Solder_arc.setFunction(PrimitiveArc.Function.NONELEC);
		Bottom_Solder_arc.setWipable();
		Bottom_Solder_arc.setFactoryAngleIncrement(45);

		/** Drawing arc */
		PrimitiveArc Drawing_arc = PrimitiveArc.newInstance(this, "Drawing", 0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(D1_lay, 0, Poly.Type.FILLED)
		});
		Drawing_arc.setFunction(PrimitiveArc.Function.NONELEC);
		Drawing_arc.setWipable();
		Drawing_arc.setFactoryAngleIncrement(45);

		//******************** RECTANGLE DESCRIPTIONS ********************

		Technology.TechPoint [] box_1 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.makeCenter()),
			new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.makeCenter()),
		};
		Technology.TechPoint [] box_2 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge()),
			new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.makeTopEdge()),
		};

		//******************** NODES ********************

		/** Signal-1-Pin */
		PrimitiveNode sp_node = PrimitiveNode.newInstance("Signal-1-Pin", this, 1.25, 1.25, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(S_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, box_1)
			});
		sp_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, sp_node, new ArcProto [] {Signal_1_arc, Power_1_arc}, "signal-1", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		sp_node.setFunction(PrimitiveNode.Function.PIN);
		sp_node.setWipeOn1or2();
		sp_node.setSquare();

		/** Signal-2-Pin */
		PrimitiveNode sp0_node = PrimitiveNode.newInstance("Signal-2-Pin", this, 1.25, 1.25, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(S0_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, box_1)
			});
		sp0_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, sp0_node, new ArcProto [] {Signal_2_arc, Power_2_arc}, "signal-2", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		sp0_node.setFunction(PrimitiveNode.Function.PIN);
		sp0_node.setWipeOn1or2();
		sp0_node.setSquare();

		/** Signal-3-Pin */
		PrimitiveNode sp1_node = PrimitiveNode.newInstance("Signal-3-Pin", this, 1.25, 1.25, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(S1_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, box_1)
			});
		sp1_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, sp1_node, new ArcProto [] {Signal_3_arc, Power_3_arc}, "signal-3", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		sp1_node.setFunction(PrimitiveNode.Function.PIN);
		sp1_node.setWipeOn1or2();
		sp1_node.setSquare();

		/** Signal-4-Pin */
		PrimitiveNode sp2_node = PrimitiveNode.newInstance("Signal-4-Pin", this, 1.25, 1.25, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(S2_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, box_1)
			});
		sp2_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, sp2_node, new ArcProto [] {Signal_4_arc, Power_4_arc}, "signal-4", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		sp2_node.setFunction(PrimitiveNode.Function.PIN);
		sp2_node.setWipeOn1or2();
		sp2_node.setSquare();

		/** Signal-5-Pin */
		PrimitiveNode sp3_node = PrimitiveNode.newInstance("Signal-5-Pin", this, 1.25, 1.25, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(S3_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, box_1)
			});
		sp3_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, sp3_node, new ArcProto [] {Signal_5_arc, Power_5_arc}, "signal-5", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		sp3_node.setFunction(PrimitiveNode.Function.PIN);
		sp3_node.setWipeOn1or2();
		sp3_node.setSquare();

		/** Signal-6-Pin */
		PrimitiveNode sp4_node = PrimitiveNode.newInstance("Signal-6-Pin", this, 1.25, 1.25, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(S4_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, box_1)
			});
		sp4_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, sp4_node, new ArcProto [] {Signal_6_arc, Power_6_arc}, "signal-6", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		sp4_node.setFunction(PrimitiveNode.Function.PIN);
		sp4_node.setWipeOn1or2();
		sp4_node.setSquare();

		/** Signal-7-Pin */
		PrimitiveNode sp5_node = PrimitiveNode.newInstance("Signal-7-Pin", this, 1.25, 1.25, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(S5_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, box_1)
			});
		sp5_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, sp5_node, new ArcProto [] {Signal_7_arc, Power_7_arc}, "signal-7", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		sp5_node.setFunction(PrimitiveNode.Function.PIN);
		sp5_node.setWipeOn1or2();
		sp5_node.setSquare();

		/** Signal-8-Pin */
		PrimitiveNode sp6_node = PrimitiveNode.newInstance("Signal-8-Pin", this, 1.25, 1.25, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(S6_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, box_1)
			});
		sp6_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, sp6_node, new ArcProto [] {Signal_8_arc, Power_8_arc}, "signal-8", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		sp6_node.setFunction(PrimitiveNode.Function.PIN);
		sp6_node.setWipeOn1or2();
		sp6_node.setSquare();

		/** Power-1-Pin */
		PrimitiveNode pp_node = PrimitiveNode.newInstance("Power-1-Pin", this, 1.25, 1.25, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(P_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, box_1)
			});
		pp_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, pp_node, new ArcProto [] {Signal_1_arc, Power_1_arc}, "power-1", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		pp_node.setFunction(PrimitiveNode.Function.PIN);
		pp_node.setWipeOn1or2();
		pp_node.setSquare();

		/** Power-2-Pin */
		PrimitiveNode pp0_node = PrimitiveNode.newInstance("Power-2-Pin", this, 1.25, 1.25, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(P0_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, box_1)
			});
		pp0_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, pp0_node, new ArcProto [] {Signal_2_arc, Power_2_arc}, "power-2", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		pp0_node.setFunction(PrimitiveNode.Function.PIN);
		pp0_node.setWipeOn1or2();
		pp0_node.setSquare();

		/** Power-3-Pin */
		PrimitiveNode pp1_node = PrimitiveNode.newInstance("Power-3-Pin", this, 1.25, 1.25, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(P1_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, box_1)
			});
		pp1_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, pp1_node, new ArcProto [] {Signal_3_arc, Power_3_arc}, "power-3", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		pp1_node.setFunction(PrimitiveNode.Function.PIN);
		pp1_node.setWipeOn1or2();
		pp1_node.setSquare();

		/** Power-4-Pin */
		PrimitiveNode pp2_node = PrimitiveNode.newInstance("Power-4-Pin", this, 1.25, 1.25, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(P2_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, box_1)
			});
		pp2_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, pp2_node, new ArcProto [] {Signal_4_arc, Power_4_arc}, "power-4", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		pp2_node.setFunction(PrimitiveNode.Function.PIN);
		pp2_node.setWipeOn1or2();
		pp2_node.setSquare();

		/** Power-5-Pin */
		PrimitiveNode pp3_node = PrimitiveNode.newInstance("Power-5-Pin", this, 1.25, 1.25, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(P3_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, box_1)
			});
		pp3_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, pp3_node, new ArcProto [] {Signal_5_arc, Power_5_arc}, "power-5", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		pp3_node.setFunction(PrimitiveNode.Function.PIN);
		pp3_node.setWipeOn1or2();
		pp3_node.setSquare();

		/** Power-6-Pin */
		PrimitiveNode pp4_node = PrimitiveNode.newInstance("Power-6-Pin", this, 1.25, 1.25, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(P4_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, box_1)
			});
		pp4_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, pp4_node, new ArcProto [] {Signal_6_arc, Power_6_arc}, "power-6", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		pp4_node.setFunction(PrimitiveNode.Function.PIN);
		pp4_node.setWipeOn1or2();
		pp4_node.setSquare();

		/** Power-7-Pin */
		PrimitiveNode pp5_node = PrimitiveNode.newInstance("Power-7-Pin", this, 1.25, 1.25, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(P5_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, box_1)
			});
		pp5_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, pp5_node, new ArcProto [] {Signal_7_arc, Power_7_arc}, "power-7", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		pp5_node.setFunction(PrimitiveNode.Function.PIN);
		pp5_node.setWipeOn1or2();
		pp5_node.setSquare();

		/** Power-8-Pin */
		PrimitiveNode pp6_node = PrimitiveNode.newInstance("Power-8-Pin", this, 1.25, 1.25, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(P6_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, box_1)
			});
		pp6_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, pp6_node, new ArcProto [] {Signal_8_arc, Power_8_arc}, "power-8", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		pp6_node.setFunction(PrimitiveNode.Function.PIN);
		pp6_node.setWipeOn1or2();
		pp6_node.setSquare();

		/** Top-Silk-Pin */
		PrimitiveNode tsp_node = PrimitiveNode.newInstance("Top-Silk-Pin", this, 1.25, 1.25, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(T_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, box_1)
			});
		tsp_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, tsp_node, new ArcProto [] {Top_Silk_arc}, "top-silk", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		tsp_node.setFunction(PrimitiveNode.Function.PIN);
		tsp_node.setWipeOn1or2();
		tsp_node.setSquare();

		/** Bottom-Silk-Pin */
		PrimitiveNode bsp_node = PrimitiveNode.newInstance("Bottom-Silk-Pin", this, 1.25, 1.25, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(B_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, box_1)
			});
		bsp_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, bsp_node, new ArcProto [] {Bottom_Silk_arc}, "bottom-silk", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		bsp_node.setFunction(PrimitiveNode.Function.PIN);
		bsp_node.setWipeOn1or2();
		bsp_node.setSquare();

		/** Top-Solder-Pin */
		PrimitiveNode tsp0_node = PrimitiveNode.newInstance("Top-Solder-Pin", this, 1.25, 1.25, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(T0_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, box_1)
			});
		tsp0_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, tsp0_node, new ArcProto [] {Top_Solder_arc}, "top-solder", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		tsp0_node.setFunction(PrimitiveNode.Function.PIN);
		tsp0_node.setWipeOn1or2();
		tsp0_node.setSquare();

		/** Bottom-Solder-Pin */
		PrimitiveNode bsp0_node = PrimitiveNode.newInstance("Bottom-Solder-Pin", this, 1.25, 1.25, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(B0_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, box_1)
			});
		bsp0_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, bsp0_node, new ArcProto [] {Bottom_Solder_arc}, "bottom-solder", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		bsp0_node.setFunction(PrimitiveNode.Function.PIN);
		bsp0_node.setWipeOn1or2();
		bsp0_node.setSquare();

		/** Drill-Pin */
		PrimitiveNode dp_node = PrimitiveNode.newInstance("Drill-Pin", this, 1.25, 1.25, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(D_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, box_1)
			});
		dp_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, dp_node, new ArcProto [] {Signal_1_arc, Signal_2_arc, Signal_3_arc, Signal_4_arc, Signal_5_arc, Signal_6_arc, Signal_7_arc, Signal_8_arc}, "drill", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		dp_node.setFunction(PrimitiveNode.Function.PIN);
		dp_node.setWipeOn1or2();
		dp_node.setSquare();

		/** NonPlated-Drill-Pin */
		PrimitiveNode ndp_node = PrimitiveNode.newInstance("NonPlated-Drill-Pin", this, 1.25, 1.25, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(D0_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, box_1)
			});
		ndp_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, ndp_node, new ArcProto [] {}, "nondrill", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		ndp_node.setFunction(PrimitiveNode.Function.PIN);
		ndp_node.setWipeOn1or2();
		ndp_node.setSquare();

		/** Engineering-Drawing-Pin */
		PrimitiveNode edp_node = PrimitiveNode.newInstance("Engineering-Drawing-Pin", this, 1.25, 1.25, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(D1_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, box_1)
			});
		edp_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, edp_node, new ArcProto [] {Drawing_arc}, "engineering", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		edp_node.setFunction(PrimitiveNode.Function.PIN);
		edp_node.setWipeOn1or2();
		edp_node.setSquare();

		/** Signal-1-Node */
		PrimitiveNode sn_node = PrimitiveNode.newInstance("Signal-1-Node", this, 1.25, 1.25, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(S_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_2)
			});
		sn_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, sn_node, new ArcProto [] {Signal_1_arc, Power_1_arc}, "signal-1", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		sn_node.setFunction(PrimitiveNode.Function.NODE);
		sn_node.setHoldsOutline();
		sn_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Signal-2-Node */
		PrimitiveNode sn0_node = PrimitiveNode.newInstance("Signal-2-Node", this, 1.25, 1.25, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(S0_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_2)
			});
		sn0_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, sn0_node, new ArcProto [] {Signal_2_arc, Power_2_arc}, "signal-2", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		sn0_node.setFunction(PrimitiveNode.Function.NODE);
		sn0_node.setHoldsOutline();
		sn0_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Signal-3-Node */
		PrimitiveNode sn1_node = PrimitiveNode.newInstance("Signal-3-Node", this, 1.25, 1.25, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(S1_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_2)
			});
		sn1_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, sn1_node, new ArcProto [] {Signal_3_arc, Power_3_arc}, "signal-3", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		sn1_node.setFunction(PrimitiveNode.Function.NODE);
		sn1_node.setHoldsOutline();
		sn1_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Signal-4-Node */
		PrimitiveNode sn2_node = PrimitiveNode.newInstance("Signal-4-Node", this, 1.25, 1.25, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(S2_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_2)
			});
		sn2_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, sn2_node, new ArcProto [] {Signal_4_arc, Power_4_arc}, "signal-4", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		sn2_node.setFunction(PrimitiveNode.Function.NODE);
		sn2_node.setHoldsOutline();
		sn2_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Signal-5-Node */
		PrimitiveNode sn3_node = PrimitiveNode.newInstance("Signal-5-Node", this, 1.25, 1.25, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(S3_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_2)
			});
		sn3_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, sn3_node, new ArcProto [] {Signal_5_arc, Power_5_arc}, "signal-5", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		sn3_node.setFunction(PrimitiveNode.Function.NODE);
		sn3_node.setHoldsOutline();
		sn3_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Signal-6-Node */
		PrimitiveNode sn4_node = PrimitiveNode.newInstance("Signal-6-Node", this, 1.25, 1.25, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(S4_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_2)
			});
		sn4_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, sn4_node, new ArcProto [] {Signal_6_arc, Power_6_arc}, "signal-6", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		sn4_node.setFunction(PrimitiveNode.Function.NODE);
		sn4_node.setHoldsOutline();
		sn4_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Signal-7-Node */
		PrimitiveNode sn5_node = PrimitiveNode.newInstance("Signal-7-Node", this, 1.25, 1.25, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(S5_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_2)
			});
		sn5_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, sn5_node, new ArcProto [] {Signal_7_arc, Power_7_arc}, "signal-7", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		sn5_node.setFunction(PrimitiveNode.Function.NODE);
		sn5_node.setHoldsOutline();
		sn5_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Signal-8-Node */
		PrimitiveNode sn6_node = PrimitiveNode.newInstance("Signal-8-Node", this, 1.25, 1.25, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(S6_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_2)
			});
		sn6_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, sn6_node, new ArcProto [] {Signal_8_arc, Power_8_arc}, "signal-8", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		sn6_node.setFunction(PrimitiveNode.Function.NODE);
		sn6_node.setHoldsOutline();
		sn6_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Power-1-Node */
		PrimitiveNode pn_node = PrimitiveNode.newInstance("Power-1-Node", this, 1.25, 1.25, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(P_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_2)
			});
		pn_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, pn_node, new ArcProto [] {Signal_1_arc, Power_1_arc}, "power-1", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		pn_node.setFunction(PrimitiveNode.Function.NODE);
		pn_node.setHoldsOutline();
		pn_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Power-2-Node */
		PrimitiveNode pn0_node = PrimitiveNode.newInstance("Power-2-Node", this, 1.25, 1.25, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(P0_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_2)
			});
		pn0_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, pn0_node, new ArcProto [] {Signal_2_arc, Power_2_arc}, "power-2", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		pn0_node.setFunction(PrimitiveNode.Function.NODE);
		pn0_node.setHoldsOutline();
		pn0_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Power-3-Node */
		PrimitiveNode pn1_node = PrimitiveNode.newInstance("Power-3-Node", this, 1.25, 1.25, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(P1_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_2)
			});
		pn1_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, pn1_node, new ArcProto [] {Signal_3_arc, Power_3_arc}, "power-3", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		pn1_node.setFunction(PrimitiveNode.Function.NODE);
		pn1_node.setHoldsOutline();
		pn1_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Power-4-Node */
		PrimitiveNode pn2_node = PrimitiveNode.newInstance("Power-4-Node", this, 1.25, 1.25, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(P2_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_2)
			});
		pn2_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, pn2_node, new ArcProto [] {Signal_4_arc, Power_4_arc}, "power-4", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		pn2_node.setFunction(PrimitiveNode.Function.NODE);
		pn2_node.setHoldsOutline();
		pn2_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Power-5-Node */
		PrimitiveNode pn3_node = PrimitiveNode.newInstance("Power-5-Node", this, 1.25, 1.25, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(P3_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_2)
			});
		pn3_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, pn3_node, new ArcProto [] {Signal_5_arc, Power_5_arc}, "power-5", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		pn3_node.setFunction(PrimitiveNode.Function.NODE);
		pn3_node.setHoldsOutline();
		pn3_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Power-6-Node */
		PrimitiveNode pn4_node = PrimitiveNode.newInstance("Power-6-Node", this, 1.25, 1.25, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(P4_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_2)
			});
		pn4_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, pn4_node, new ArcProto [] {Signal_6_arc, Power_6_arc}, "power-6", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		pn4_node.setFunction(PrimitiveNode.Function.NODE);
		pn4_node.setHoldsOutline();
		pn4_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Power-7-Node */
		PrimitiveNode pn5_node = PrimitiveNode.newInstance("Power-7-Node", this, 1.25, 1.25, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(P5_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_2)
			});
		pn5_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, pn5_node, new ArcProto [] {Signal_7_arc, Power_7_arc}, "power-7", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		pn5_node.setFunction(PrimitiveNode.Function.NODE);
		pn5_node.setHoldsOutline();
		pn5_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Power-8-Node */
		PrimitiveNode pn6_node = PrimitiveNode.newInstance("Power-8-Node", this, 1.25, 1.25, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(P6_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_2)
			});
		pn6_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, pn6_node, new ArcProto [] {Signal_8_arc, Power_8_arc}, "power-8", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		pn6_node.setFunction(PrimitiveNode.Function.NODE);
		pn6_node.setHoldsOutline();
		pn6_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Top-Silk-Node */
		PrimitiveNode tsn_node = PrimitiveNode.newInstance("Top-Silk-Node", this, 1.25, 1.25, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(T_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_2)
			});
		tsn_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, tsn_node, new ArcProto [] {Top_Silk_arc}, "top-silk", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		tsn_node.setFunction(PrimitiveNode.Function.NODE);
		tsn_node.setHoldsOutline();
		tsn_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Bottom-Silk-Node */
		PrimitiveNode bsn_node = PrimitiveNode.newInstance("Bottom-Silk-Node", this, 1.25, 1.25, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(B_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_2)
			});
		bsn_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, bsn_node, new ArcProto [] {Bottom_Silk_arc}, "bottom-silk", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		bsn_node.setFunction(PrimitiveNode.Function.NODE);
		bsn_node.setHoldsOutline();
		bsn_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Top-Solder-Node */
		PrimitiveNode tsn0_node = PrimitiveNode.newInstance("Top-Solder-Node", this, 1.25, 1.25, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(T0_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_2)
			});
		tsn0_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, tsn0_node, new ArcProto [] {Top_Solder_arc}, "top-solder", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		tsn0_node.setFunction(PrimitiveNode.Function.NODE);
		tsn0_node.setHoldsOutline();
		tsn0_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Bottom-Solder-Node */
		PrimitiveNode bsn0_node = PrimitiveNode.newInstance("Bottom-Solder-Node", this, 1.25, 1.25, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(B0_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_2)
			});
		bsn0_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, bsn0_node, new ArcProto [] {Bottom_Solder_arc}, "bottom-solder", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		bsn0_node.setFunction(PrimitiveNode.Function.NODE);
		bsn0_node.setHoldsOutline();
		bsn0_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Engineering-Drawing-Node */
		PrimitiveNode edn_node = PrimitiveNode.newInstance("Engineering-Drawing-Node", this, 1.25, 1.25, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(D1_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_2)
			});
		edn_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, edn_node, new ArcProto [] {Drawing_arc}, "engineering", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		edn_node.setFunction(PrimitiveNode.Function.NODE);
		edn_node.setHoldsOutline();
		edn_node.setSpecialType(PrimitiveNode.POLYGONAL);

		// The pure layer nodes
		S_lay.setPureLayerNode(sn_node);		// Signal1
		S0_lay.setPureLayerNode(sn0_node);		// Signal2
		S1_lay.setPureLayerNode(sn1_node);		// Signal3
		S2_lay.setPureLayerNode(sn2_node);		// Signal4
		S3_lay.setPureLayerNode(sn3_node);		// Signal5
		S4_lay.setPureLayerNode(sn4_node);		// Signal6
		S5_lay.setPureLayerNode(sn5_node);		// Signal7
		S6_lay.setPureLayerNode(sn6_node);		// Signal8
		P_lay.setPureLayerNode(pn_node);		// Power1
		P0_lay.setPureLayerNode(pn0_node);		// Power2
		P1_lay.setPureLayerNode(pn1_node);		// Power3
		P2_lay.setPureLayerNode(pn2_node);		// Power4
		P3_lay.setPureLayerNode(pn3_node);		// Power5
		P4_lay.setPureLayerNode(pn4_node);		// Power6
		P5_lay.setPureLayerNode(pn5_node);		// Power7
		P6_lay.setPureLayerNode(pn6_node);		// Power8
		T_lay.setPureLayerNode(tsn_node);		// TopSilk
		B_lay.setPureLayerNode(bsn_node);		// BottomSilk
		T0_lay.setPureLayerNode(tsn0_node);		// TopSolder
		B0_lay.setPureLayerNode(bsn0_node);		// BottomSolder
		D1_lay.setPureLayerNode(edn_node);		// Drawing
	};
}
