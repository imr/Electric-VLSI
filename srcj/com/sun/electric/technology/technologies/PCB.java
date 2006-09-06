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
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.technology.*;

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
		super("pcb");
		setTechDesc("Printed Circuit Board (eight-layer)");
		setFactoryScale(1270000, true);   // in nanometers: really 1270 microns
		setNoNegatedArcs();
		setStaticTechnology();

        //Foundry
        Foundry mosis = new Foundry(Foundry.Type.NONE);
        foundries.add(mosis);
        
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
			new EGraphics(false, false, null, EGraphics.TRANSPARENT_1,   0,  0,  0,/*0,255,0,*/ 0.8,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** S0 layer */
		Layer S0_lay = Layer.newInstance(this, "Signal2",
			new EGraphics(false, false, null, EGraphics.TRANSPARENT_2, 255,  0,  0,/*0,0,255,*/ 0.8,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** S1 layer */
		Layer S1_lay = Layer.newInstance(this, "Signal3",
			new EGraphics(false, false, null, EGraphics.TRANSPARENT_3,   0,255,  0,/*255,255,0,*/ 0.8,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** S2 layer */
		Layer S2_lay = Layer.newInstance(this, "Signal4",
			new EGraphics(false, false, null, EGraphics.TRANSPARENT_4,   0,  0,255,/*116,0,0,*/ 0.8,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** S3 layer */
		Layer S3_lay = Layer.newInstance(this, "Signal5",
			new EGraphics(false, false, null, EGraphics.TRANSPARENT_5, 255,255,  0,/*0,0,0,*/ 0.8,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** S4 layer */
		Layer S4_lay = Layer.newInstance(this, "Signal6",
			new EGraphics(false, false, null, 0, 255,255,0, 0.8,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** S5 layer */
		Layer S5_lay = Layer.newInstance(this, "Signal7",
			new EGraphics(false, false, null, 0, 255,190,6, 0.8,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** S6 layer */
		Layer S6_lay = Layer.newInstance(this, "Signal8",
			new EGraphics(false, false, null, 0, 0,255,255, 0.8,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** P layer */
		Layer P_lay = Layer.newInstance(this, "Power1",
			new EGraphics(false, false, null, EGraphics.TRANSPARENT_1,   0,  0,  0,/*0,255,0,*/ 0.8,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** P0 layer */
		Layer P0_lay = Layer.newInstance(this, "Power2",
			new EGraphics(false, false, null, EGraphics.TRANSPARENT_2, 255,  0,  0,/*0,0,255,*/ 0.8,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** P1 layer */
		Layer P1_lay = Layer.newInstance(this, "Power3",
			new EGraphics(false, false, null, EGraphics.TRANSPARENT_3,   0,255,  0,/*255,255,0,*/ 0.8,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** P2 layer */
		Layer P2_lay = Layer.newInstance(this, "Power4",
			new EGraphics(false, false, null, EGraphics.TRANSPARENT_4,   0,  0,255,/*116,0,0,*/ 0.8,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** P3 layer */
		Layer P3_lay = Layer.newInstance(this, "Power5",
			new EGraphics(false, false, null, EGraphics.TRANSPARENT_5, 255,255,  0,/*0,0,0,*/ 0.8,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** P4 layer */
		Layer P4_lay = Layer.newInstance(this, "Power6",
			new EGraphics(false, false, null, 0, 255,255,0, 0.8,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** P5 layer */
		Layer P5_lay = Layer.newInstance(this, "Power7",
			new EGraphics(false, false, null, 0, 255,190,6, 0.8,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** P6 layer */
		Layer P6_lay = Layer.newInstance(this, "Power8",
			new EGraphics(false, false, null, 0, 0,255,255, 0.8,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** T layer */
		Layer T_lay = Layer.newInstance(this, "TopSilk",
			new EGraphics(false, false, null, 0, 230,230,230, 0.8,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** B layer */
		Layer B_lay = Layer.newInstance(this, "BottomSilk",
			new EGraphics(false, false, null, 0, 100,100,100, 0.8,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** T0 layer */
		Layer T0_lay = Layer.newInstance(this, "TopSolder",
			new EGraphics(false, false, null, 0, 175,255,175, 0.8,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** B0 layer */
		Layer B0_lay = Layer.newInstance(this, "BottomSolder",
			new EGraphics(false, false, null, 0, 89,159,85, 0.8,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** D layer */
		Layer D_lay = Layer.newInstance(this, "Drill",
			new EGraphics(false, false, null, 0, 2,15,159, 0.8,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** D0 layer */
		Layer D0_lay = Layer.newInstance(this, "DrillNonPlated",
			new EGraphics(false, false, null, 0, 150,150,255, 0.8,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** D1 layer */
		Layer D1_lay = Layer.newInstance(this, "Drawing",
			new EGraphics(false, false, null, 0, 255,150,150, 0.8,true,
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
//		S_lay.setFactoryGDSLayer("", Foundry.Type.MOSIS.name());		// Signal1
//		S0_lay.setFactoryGDSLayer("", Foundry.Type.MOSIS.name());		// Signal2
//		S1_lay.setFactoryGDSLayer("", Foundry.Type.MOSIS.name());		// Signal3
//		S2_lay.setFactoryGDSLayer("", Foundry.Type.MOSIS.name());		// Signal4
//		S3_lay.setFactoryGDSLayer("", Foundry.Type.MOSIS.name());		// Signal5
//		S4_lay.setFactoryGDSLayer("", Foundry.Type.MOSIS.name());		// Signal6
//		S5_lay.setFactoryGDSLayer("", Foundry.Type.MOSIS.name());		// Signal7
//		S6_lay.setFactoryGDSLayer("", Foundry.Type.MOSIS.name());		// Signal8
//		P_lay.setFactoryGDSLayer("", Foundry.Type.MOSIS.name());		// Power1
//		P0_lay.setFactoryGDSLayer("", Foundry.Type.MOSIS.name());		// Power2
//		P1_lay.setFactoryGDSLayer("", Foundry.Type.MOSIS.name());		// Power3
//		P2_lay.setFactoryGDSLayer("", Foundry.Type.MOSIS.name());		// Power4
//		P3_lay.setFactoryGDSLayer("", Foundry.Type.MOSIS.name());		// Power5
//		P4_lay.setFactoryGDSLayer("", Foundry.Type.MOSIS.name());		// Power6
//		P5_lay.setFactoryGDSLayer("", Foundry.Type.MOSIS.name());		// Power7
//		P6_lay.setFactoryGDSLayer("", Foundry.Type.MOSIS.name());		// Power8
//		T_lay.setFactoryGDSLayer("", Foundry.Type.MOSIS.name());		// TopSilk
//		B_lay.setFactoryGDSLayer("", Foundry.Type.MOSIS.name());		// BottomSilk
//		T0_lay.setFactoryGDSLayer("", Foundry.Type.MOSIS.name());		// TopSolder
//		B0_lay.setFactoryGDSLayer("", Foundry.Type.MOSIS.name());		// BottomSolder
//		D_lay.setFactoryGDSLayer("", Foundry.Type.MOSIS.name());		// Drill
//		D0_lay.setFactoryGDSLayer("", Foundry.Type.MOSIS.name());		// DrillNonPlated
//		D1_lay.setFactoryGDSLayer("", Foundry.Type.MOSIS.name());		// Drawing

		//******************** ARCS ********************
        ArcProto[] SignalArcs = new ArcProto[8];

		/** Signal-1 arc */
		SignalArcs[0] = ArcProto.newInstance(this, "Signal-1", 0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(S_lay, 0, Poly.Type.FILLED)
		});
		SignalArcs[0].setFunction(ArcProto.Function.METAL1);
		SignalArcs[0].setWipable();
		SignalArcs[0].setFactoryAngleIncrement(45);

		/** Signal-2 arc */
		SignalArcs[1] = ArcProto.newInstance(this, "Signal-2", 0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(S0_lay, 0, Poly.Type.FILLED)
		});
		SignalArcs[1].setFunction(ArcProto.Function.METAL2);
		SignalArcs[1].setWipable();
		SignalArcs[1].setFactoryAngleIncrement(45);

		/** Signal-3 arc */
		SignalArcs[2] = ArcProto.newInstance(this, "Signal-3", 0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(S1_lay, 0, Poly.Type.FILLED)
		});
		SignalArcs[2].setFunction(ArcProto.Function.METAL3);
		SignalArcs[2].setWipable();
		SignalArcs[2].setFactoryAngleIncrement(45);

		/** Signal-4 arc */
		SignalArcs[3] = ArcProto.newInstance(this, "Signal-4", 0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(S2_lay, 0, Poly.Type.FILLED)
		});
		SignalArcs[3].setFunction(ArcProto.Function.METAL4);
		SignalArcs[3].setWipable();
		SignalArcs[3].setFactoryAngleIncrement(45);

		/** Signal-5 arc */
		SignalArcs[4] = ArcProto.newInstance(this, "Signal-5", 0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(S3_lay, 0, Poly.Type.FILLED)
		});
		SignalArcs[4].setFunction(ArcProto.Function.METAL5);
		SignalArcs[4].setWipable();
		SignalArcs[4].setFactoryAngleIncrement(45);

		/** Signal-6 arc */
		SignalArcs[5] = ArcProto.newInstance(this, "Signal-6", 0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(S4_lay, 0, Poly.Type.FILLED)
		});
		SignalArcs[5].setFunction(ArcProto.Function.METAL6);
		SignalArcs[5].setWipable();
		SignalArcs[5].setFactoryAngleIncrement(45);

		/** Signal-7 arc */
		SignalArcs[6] = ArcProto.newInstance(this, "Signal-7", 0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(S5_lay, 0, Poly.Type.FILLED)
		});
		SignalArcs[6].setFunction(ArcProto.Function.METAL7);
		SignalArcs[6].setWipable();
		SignalArcs[6].setFactoryAngleIncrement(45);

		/** Signal-8 arc */
		SignalArcs[7] = ArcProto.newInstance(this, "Signal-8", 0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(S6_lay, 0, Poly.Type.FILLED)
		});
		SignalArcs[7].setFunction(ArcProto.Function.METAL8);
		SignalArcs[7].setWipable();
		SignalArcs[7].setFactoryAngleIncrement(45);

        ArcProto[] PowerArcs = new ArcProto[8];
		/** Power-1 arc */
		PowerArcs[0] = ArcProto.newInstance(this, "Power-1", 0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(P_lay, 0, Poly.Type.FILLED)
		});
		PowerArcs[0].setFunction(ArcProto.Function.METAL1);
		PowerArcs[0].setWipable();
		PowerArcs[0].setFactoryAngleIncrement(45);

		/** Power-2 arc */
		PowerArcs[1] = ArcProto.newInstance(this, "Power-2", 0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(P0_lay, 0, Poly.Type.FILLED)
		});
		PowerArcs[1].setFunction(ArcProto.Function.METAL2);
		PowerArcs[1].setWipable();
		PowerArcs[1].setFactoryAngleIncrement(45);

		/** Power-3 arc */
		PowerArcs[2] = ArcProto.newInstance(this, "Power-3", 0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(P1_lay, 0, Poly.Type.FILLED)
		});
		PowerArcs[2].setFunction(ArcProto.Function.METAL3);
		PowerArcs[2].setWipable();
		PowerArcs[2].setFactoryAngleIncrement(45);

		/** Power-4 arc */
		PowerArcs[3] = ArcProto.newInstance(this, "Power-4", 0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(P2_lay, 0, Poly.Type.FILLED)
		});
		PowerArcs[3].setFunction(ArcProto.Function.METAL4);
		PowerArcs[3].setWipable();
		PowerArcs[3].setFactoryAngleIncrement(45);

		/** Power-5 arc */
		PowerArcs[4] = ArcProto.newInstance(this, "Power-5", 0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(P3_lay, 0, Poly.Type.FILLED)
		});
		PowerArcs[4].setFunction(ArcProto.Function.METAL5);
		PowerArcs[4].setWipable();
		PowerArcs[4].setFactoryAngleIncrement(45);

		/** Power-6 arc */
		PowerArcs[5] = ArcProto.newInstance(this, "Power-6", 0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(P4_lay, 0, Poly.Type.FILLED)
		});
		PowerArcs[5].setFunction(ArcProto.Function.METAL6);
		PowerArcs[5].setWipable();
		PowerArcs[5].setFactoryAngleIncrement(45);

		/** Power-7 arc */
		PowerArcs[6] = ArcProto.newInstance(this, "Power-7", 0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(P5_lay, 0, Poly.Type.FILLED)
		});
		PowerArcs[6].setFunction(ArcProto.Function.METAL7);
		PowerArcs[6].setWipable();
		PowerArcs[6].setFactoryAngleIncrement(45);

		/** Power-8 arc */
		PowerArcs[7] = ArcProto.newInstance(this, "Power-8", 0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(P6_lay, 0, Poly.Type.FILLED)
		});
		PowerArcs[7].setFunction(ArcProto.Function.METAL8);
		PowerArcs[7].setWipable();
		PowerArcs[7].setFactoryAngleIncrement(45);

		/** Top-Silk arc */
		ArcProto Top_Silk_arc = ArcProto.newInstance(this, "Top-Silk", 0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(T_lay, 0, Poly.Type.FILLED)
		});
		Top_Silk_arc.setFunction(ArcProto.Function.NONELEC);
		Top_Silk_arc.setWipable();
		Top_Silk_arc.setFactoryAngleIncrement(45);

		/** Bottom-Silk arc */
		ArcProto Bottom_Silk_arc = ArcProto.newInstance(this, "Bottom-Silk", 0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(B_lay, 0, Poly.Type.FILLED)
		});
		Bottom_Silk_arc.setFunction(ArcProto.Function.NONELEC);
		Bottom_Silk_arc.setWipable();
		Bottom_Silk_arc.setFactoryAngleIncrement(45);

		/** Top-Solder arc */
		ArcProto Top_Solder_arc = ArcProto.newInstance(this, "Top-Solder", 0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(T0_lay, 0, Poly.Type.FILLED)
		});
		Top_Solder_arc.setFunction(ArcProto.Function.NONELEC);
		Top_Solder_arc.setWipable();
		Top_Solder_arc.setFactoryAngleIncrement(45);

		/** Bottom-Solder arc */
		ArcProto Bottom_Solder_arc = ArcProto.newInstance(this, "Bottom-Solder", 0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(B0_lay, 0, Poly.Type.FILLED)
		});
		Bottom_Solder_arc.setFunction(ArcProto.Function.NONELEC);
		Bottom_Solder_arc.setWipable();
		Bottom_Solder_arc.setFactoryAngleIncrement(45);

		/** Drawing arc */
		ArcProto Drawing_arc = ArcProto.newInstance(this, "Drawing", 0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(D1_lay, 0, Poly.Type.FILLED)
		});
		Drawing_arc.setFunction(ArcProto.Function.NONELEC);
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
        PrimitiveNode[] spinNodes = new PrimitiveNode[8];

		/** Signal-1-Pin */
		spinNodes[0] = PrimitiveNode.newInstance("Signal-1-Pin", this, 1.25, 1.25, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(S_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, box_1)
			});
		spinNodes[0].addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, spinNodes[0], new ArcProto [] {SignalArcs[0], PowerArcs[0]}, "signal-1", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		spinNodes[0].setFunction(PrimitiveNode.Function.PIN);
		spinNodes[0].setWipeOn1or2();
		spinNodes[0].setSquare();

		/** Signal-2-Pin */
		spinNodes[1] = PrimitiveNode.newInstance("Signal-2-Pin", this, 1.25, 1.25, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(S0_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, box_1)
			});
		spinNodes[1].addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, spinNodes[1], new ArcProto [] {SignalArcs[1], PowerArcs[1]}, "signal-2", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		spinNodes[1].setFunction(PrimitiveNode.Function.PIN);
		spinNodes[1].setWipeOn1or2();
		spinNodes[1].setSquare();

		/** Signal-3-Pin */
		spinNodes[2] = PrimitiveNode.newInstance("Signal-3-Pin", this, 1.25, 1.25, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(S1_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, box_1)
			});
		spinNodes[2].addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, spinNodes[2], new ArcProto [] {SignalArcs[2], PowerArcs[2]}, "signal-3", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		spinNodes[2].setFunction(PrimitiveNode.Function.PIN);
		spinNodes[2].setWipeOn1or2();
		spinNodes[2].setSquare();

		/** Signal-4-Pin */
		spinNodes[3] = PrimitiveNode.newInstance("Signal-4-Pin", this, 1.25, 1.25, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(S2_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, box_1)
			});
		spinNodes[3].addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, spinNodes[3], new ArcProto [] {SignalArcs[3], PowerArcs[3]}, "signal-4", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		spinNodes[3].setFunction(PrimitiveNode.Function.PIN);
		spinNodes[3].setWipeOn1or2();
		spinNodes[3].setSquare();

		/** Signal-5-Pin */
		spinNodes[4] = PrimitiveNode.newInstance("Signal-5-Pin", this, 1.25, 1.25, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(S3_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, box_1)
			});
		spinNodes[4].addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, spinNodes[4], new ArcProto [] {SignalArcs[4], PowerArcs[4]}, "signal-5", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		spinNodes[4].setFunction(PrimitiveNode.Function.PIN);
		spinNodes[4].setWipeOn1or2();
		spinNodes[4].setSquare();

		/** Signal-6-Pin */
		spinNodes[5] = PrimitiveNode.newInstance("Signal-6-Pin", this, 1.25, 1.25, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(S4_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, box_1)
			});
		spinNodes[5].addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, spinNodes[5], new ArcProto [] {SignalArcs[5], PowerArcs[5]}, "signal-6", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		spinNodes[5].setFunction(PrimitiveNode.Function.PIN);
		spinNodes[5].setWipeOn1or2();
		spinNodes[5].setSquare();

		/** Signal-7-Pin */
		spinNodes[6] = PrimitiveNode.newInstance("Signal-7-Pin", this, 1.25, 1.25, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(S5_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, box_1)
			});
		spinNodes[6].addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, spinNodes[6], new ArcProto [] {SignalArcs[6], PowerArcs[6]}, "signal-7", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		spinNodes[6].setFunction(PrimitiveNode.Function.PIN);
		spinNodes[6].setWipeOn1or2();
		spinNodes[6].setSquare();

		/** Signal-8-Pin */
		spinNodes[7] = PrimitiveNode.newInstance("Signal-8-Pin", this, 1.25, 1.25, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(S6_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, box_1)
			});
		spinNodes[7].addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, spinNodes[7], new ArcProto [] {SignalArcs[7], PowerArcs[7]}, "signal-8", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		spinNodes[7].setFunction(PrimitiveNode.Function.PIN);
		spinNodes[7].setWipeOn1or2();
		spinNodes[7].setSquare();

        PrimitiveNode[] ppinNodes = new PrimitiveNode[8];

		/** Power-1-Pin */
		ppinNodes[0] = PrimitiveNode.newInstance("Power-1-Pin", this, 1.25, 1.25, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(P_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, box_1)
			});
		ppinNodes[0].addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, ppinNodes[0], new ArcProto [] {SignalArcs[0], PowerArcs[0]}, "power-1", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		ppinNodes[0].setFunction(PrimitiveNode.Function.PIN);
		ppinNodes[0].setWipeOn1or2();
		ppinNodes[0].setSquare();

		/** Power-2-Pin */
		ppinNodes[1] = PrimitiveNode.newInstance("Power-2-Pin", this, 1.25, 1.25, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(P0_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, box_1)
			});
		ppinNodes[1].addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, ppinNodes[1], new ArcProto [] {SignalArcs[1], PowerArcs[1]}, "power-2", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		ppinNodes[1].setFunction(PrimitiveNode.Function.PIN);
		ppinNodes[1].setWipeOn1or2();
		ppinNodes[1].setSquare();

		/** Power-3-Pin */
		ppinNodes[2] = PrimitiveNode.newInstance("Power-3-Pin", this, 1.25, 1.25, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(P1_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, box_1)
			});
		ppinNodes[2].addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, ppinNodes[2], new ArcProto [] {SignalArcs[2], PowerArcs[2]}, "power-3", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		ppinNodes[2].setFunction(PrimitiveNode.Function.PIN);
		ppinNodes[2].setWipeOn1or2();
		ppinNodes[2].setSquare();

		/** Power-4-Pin */
		ppinNodes[3] = PrimitiveNode.newInstance("Power-4-Pin", this, 1.25, 1.25, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(P2_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, box_1)
			});
		ppinNodes[3].addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, ppinNodes[3], new ArcProto [] {SignalArcs[3], PowerArcs[3]}, "power-4", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		ppinNodes[3].setFunction(PrimitiveNode.Function.PIN);
		ppinNodes[3].setWipeOn1or2();
		ppinNodes[3].setSquare();

		/** Power-5-Pin */
		ppinNodes[4] = PrimitiveNode.newInstance("Power-5-Pin", this, 1.25, 1.25, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(P3_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, box_1)
			});
		ppinNodes[4].addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, ppinNodes[4], new ArcProto [] {SignalArcs[4], PowerArcs[4]}, "power-5", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		ppinNodes[4].setFunction(PrimitiveNode.Function.PIN);
		ppinNodes[4].setWipeOn1or2();
		ppinNodes[4].setSquare();

		/** Power-6-Pin */
		ppinNodes[5] = PrimitiveNode.newInstance("Power-6-Pin", this, 1.25, 1.25, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(P4_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, box_1)
			});
		ppinNodes[5].addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, ppinNodes[5], new ArcProto [] {SignalArcs[5], PowerArcs[5]}, "power-6", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		ppinNodes[5].setFunction(PrimitiveNode.Function.PIN);
		ppinNodes[5].setWipeOn1or2();
		ppinNodes[5].setSquare();

		/** Power-7-Pin */
		ppinNodes[6] = PrimitiveNode.newInstance("Power-7-Pin", this, 1.25, 1.25, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(P5_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, box_1)
			});
		ppinNodes[6].addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, ppinNodes[6], new ArcProto [] {SignalArcs[6], PowerArcs[6]}, "power-7", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		ppinNodes[6].setFunction(PrimitiveNode.Function.PIN);
		ppinNodes[6].setWipeOn1or2();
		ppinNodes[6].setSquare();

		/** Power-8-Pin */
		ppinNodes[7] = PrimitiveNode.newInstance("Power-8-Pin", this, 1.25, 1.25, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(P6_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, box_1)
			});
		ppinNodes[7].addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, ppinNodes[7], new ArcProto [] {SignalArcs[7], PowerArcs[7]}, "power-8", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		ppinNodes[7].setFunction(PrimitiveNode.Function.PIN);
		ppinNodes[7].setWipeOn1or2();
		ppinNodes[7].setSquare();

        PrimitiveNode[] tspinNodes = new PrimitiveNode[2];
        PrimitiveNode[] bspinNodes = new PrimitiveNode[2];

		/** Top-Silk-Pin */
		tspinNodes[0] = PrimitiveNode.newInstance("Top-Silk-Pin", this, 1.25, 1.25, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(T_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, box_1)
			});
		tspinNodes[0].addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, tspinNodes[0], new ArcProto [] {Top_Silk_arc}, "top-silk", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		tspinNodes[0].setFunction(PrimitiveNode.Function.PIN);
		tspinNodes[0].setWipeOn1or2();
		tspinNodes[0].setSquare();

		/** Bottom-Silk-Pin */
		bspinNodes[0] = PrimitiveNode.newInstance("Bottom-Silk-Pin", this, 1.25, 1.25, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(B_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, box_1)
			});
		bspinNodes[0].addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, bspinNodes[0], new ArcProto [] {Bottom_Silk_arc}, "bottom-silk", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		bspinNodes[0].setFunction(PrimitiveNode.Function.PIN);
		bspinNodes[0].setWipeOn1or2();
		bspinNodes[0].setSquare();

		/** Top-Solder-Pin */
		tspinNodes[1] = PrimitiveNode.newInstance("Top-Solder-Pin", this, 1.25, 1.25, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(T0_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, box_1)
			});
		tspinNodes[1].addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, tspinNodes[1], new ArcProto [] {Top_Solder_arc}, "top-solder", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		tspinNodes[1].setFunction(PrimitiveNode.Function.PIN);
		tspinNodes[1].setWipeOn1or2();
		tspinNodes[1].setSquare();

		/** Bottom-Solder-Pin */
		bspinNodes[1] = PrimitiveNode.newInstance("Bottom-Solder-Pin", this, 1.25, 1.25, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(B0_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, box_1)
			});
		bspinNodes[1].addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, bspinNodes[1], new ArcProto [] {Bottom_Solder_arc}, "bottom-solder", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		bspinNodes[1].setFunction(PrimitiveNode.Function.PIN);
		bspinNodes[1].setWipeOn1or2();
		bspinNodes[1].setSquare();

		/** Drill-Pin */
		PrimitiveNode dp_node = PrimitiveNode.newInstance("Drill-Pin", this, 1.25, 1.25, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(D_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, box_1)
			});
		dp_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, dp_node, new ArcProto [] {SignalArcs[0], SignalArcs[1], SignalArcs[2], SignalArcs[3], SignalArcs[4], SignalArcs[5], SignalArcs[6], SignalArcs[7]}, "drill", 0,180, 0, PortCharacteristic.UNKNOWN,
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

        PrimitiveNode[] snNodes = new PrimitiveNode[8];

		/** Signal-1-Node */
		snNodes[0] = PrimitiveNode.newInstance("Signal-1-Node", this, 1.25, 1.25, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(S_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_2)
			});
		snNodes[0].addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, snNodes[0], new ArcProto [] {SignalArcs[0], PowerArcs[0]}, "signal-1", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		snNodes[0].setFunction(PrimitiveNode.Function.NODE);
		snNodes[0].setHoldsOutline();
		snNodes[0].setSpecialType(PrimitiveNode.POLYGONAL);

		/** Signal-2-Node */
		snNodes[1] = PrimitiveNode.newInstance("Signal-2-Node", this, 1.25, 1.25, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(S0_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_2)
			});
		snNodes[1].addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, snNodes[1], new ArcProto [] {SignalArcs[1], PowerArcs[1]}, "signal-2", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		snNodes[1].setFunction(PrimitiveNode.Function.NODE);
		snNodes[1].setHoldsOutline();
		snNodes[1].setSpecialType(PrimitiveNode.POLYGONAL);

		/** Signal-3-Node */
		snNodes[2] = PrimitiveNode.newInstance("Signal-3-Node", this, 1.25, 1.25, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(S1_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_2)
			});
		snNodes[2].addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, snNodes[2], new ArcProto [] {SignalArcs[2], PowerArcs[2]}, "signal-3", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		snNodes[2].setFunction(PrimitiveNode.Function.NODE);
		snNodes[2].setHoldsOutline();
		snNodes[2].setSpecialType(PrimitiveNode.POLYGONAL);

		/** Signal-4-Node */
		snNodes[3] = PrimitiveNode.newInstance("Signal-4-Node", this, 1.25, 1.25, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(S2_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_2)
			});
		snNodes[3].addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, snNodes[3], new ArcProto [] {SignalArcs[3], PowerArcs[3]}, "signal-4", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		snNodes[3].setFunction(PrimitiveNode.Function.NODE);
		snNodes[3].setHoldsOutline();
		snNodes[3].setSpecialType(PrimitiveNode.POLYGONAL);

		/** Signal-5-Node */
		snNodes[4] = PrimitiveNode.newInstance("Signal-5-Node", this, 1.25, 1.25, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(S3_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_2)
			});
		snNodes[4].addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, snNodes[4], new ArcProto [] {SignalArcs[4], PowerArcs[4]}, "signal-5", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		snNodes[4].setFunction(PrimitiveNode.Function.NODE);
		snNodes[4].setHoldsOutline();
		snNodes[4].setSpecialType(PrimitiveNode.POLYGONAL);

		/** Signal-6-Node */
		snNodes[5] = PrimitiveNode.newInstance("Signal-6-Node", this, 1.25, 1.25, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(S4_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_2)
			});
		snNodes[5].addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, snNodes[5], new ArcProto [] {SignalArcs[5], PowerArcs[5]}, "signal-6", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		snNodes[5].setFunction(PrimitiveNode.Function.NODE);
		snNodes[5].setHoldsOutline();
		snNodes[5].setSpecialType(PrimitiveNode.POLYGONAL);

		/** Signal-7-Node */
		snNodes[6] = PrimitiveNode.newInstance("Signal-7-Node", this, 1.25, 1.25, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(S5_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_2)
			});
		snNodes[6].addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, snNodes[6], new ArcProto [] {SignalArcs[6], PowerArcs[6]}, "signal-7", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		snNodes[6].setFunction(PrimitiveNode.Function.NODE);
		snNodes[6].setHoldsOutline();
		snNodes[6].setSpecialType(PrimitiveNode.POLYGONAL);

		/** Signal-8-Node */
		snNodes[7] = PrimitiveNode.newInstance("Signal-8-Node", this, 1.25, 1.25, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(S6_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_2)
			});
		snNodes[7].addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, snNodes[7], new ArcProto [] {SignalArcs[7], PowerArcs[7]}, "signal-8", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		snNodes[7].setFunction(PrimitiveNode.Function.NODE);
		snNodes[7].setHoldsOutline();
		snNodes[7].setSpecialType(PrimitiveNode.POLYGONAL);

        PrimitiveNode[] pnNodes = new PrimitiveNode[8];
		/** Power-1-Node */
		pnNodes[0] = PrimitiveNode.newInstance("Power-1-Node", this, 1.25, 1.25, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(P_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_2)
			});
		pnNodes[0].addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, pnNodes[0], new ArcProto [] {SignalArcs[0], PowerArcs[0]}, "power-1", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		pnNodes[0].setFunction(PrimitiveNode.Function.NODE);
		pnNodes[0].setHoldsOutline();
		pnNodes[0].setSpecialType(PrimitiveNode.POLYGONAL);

		/** Power-2-Node */
		pnNodes[1] = PrimitiveNode.newInstance("Power-2-Node", this, 1.25, 1.25, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(P0_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_2)
			});
		pnNodes[1].addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, pnNodes[1], new ArcProto [] {SignalArcs[1], PowerArcs[1]}, "power-2", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		pnNodes[1].setFunction(PrimitiveNode.Function.NODE);
		pnNodes[1].setHoldsOutline();
		pnNodes[1].setSpecialType(PrimitiveNode.POLYGONAL);

		/** Power-3-Node */
		pnNodes[2] = PrimitiveNode.newInstance("Power-3-Node", this, 1.25, 1.25, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(P1_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_2)
			});
		pnNodes[2].addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, pnNodes[2], new ArcProto [] {SignalArcs[2], PowerArcs[2]}, "power-3", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		pnNodes[2].setFunction(PrimitiveNode.Function.NODE);
		pnNodes[2].setHoldsOutline();
		pnNodes[2].setSpecialType(PrimitiveNode.POLYGONAL);

		/** Power-4-Node */
		pnNodes[3] = PrimitiveNode.newInstance("Power-4-Node", this, 1.25, 1.25, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(P2_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_2)
			});
		pnNodes[3].addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, pnNodes[3], new ArcProto [] {SignalArcs[3], PowerArcs[3]}, "power-4", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		pnNodes[3].setFunction(PrimitiveNode.Function.NODE);
		pnNodes[3].setHoldsOutline();
		pnNodes[3].setSpecialType(PrimitiveNode.POLYGONAL);

		/** Power-5-Node */
		pnNodes[4] = PrimitiveNode.newInstance("Power-5-Node", this, 1.25, 1.25, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(P3_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_2)
			});
		pnNodes[4].addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, pnNodes[4], new ArcProto [] {SignalArcs[4], PowerArcs[4]}, "power-5", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		pnNodes[4].setFunction(PrimitiveNode.Function.NODE);
		pnNodes[4].setHoldsOutline();
		pnNodes[4].setSpecialType(PrimitiveNode.POLYGONAL);

		/** Power-6-Node */
		pnNodes[5] = PrimitiveNode.newInstance("Power-6-Node", this, 1.25, 1.25, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(P4_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_2)
			});
		pnNodes[5].addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, pnNodes[5], new ArcProto [] {SignalArcs[5], PowerArcs[5]}, "power-6", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		pnNodes[5].setFunction(PrimitiveNode.Function.NODE);
		pnNodes[5].setHoldsOutline();
		pnNodes[5].setSpecialType(PrimitiveNode.POLYGONAL);

		/** Power-7-Node */
		pnNodes[6] = PrimitiveNode.newInstance("Power-7-Node", this, 1.25, 1.25, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(P5_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_2)
			});
		pnNodes[6].addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, pnNodes[6], new ArcProto [] {SignalArcs[6], PowerArcs[6]}, "power-7", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		pnNodes[6].setFunction(PrimitiveNode.Function.NODE);
		pnNodes[6].setHoldsOutline();
		pnNodes[6].setSpecialType(PrimitiveNode.POLYGONAL);

		/** Power-8-Node */
		pnNodes[7] = PrimitiveNode.newInstance("Power-8-Node", this, 1.25, 1.25, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(P6_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_2)
			});
		pnNodes[7].addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, pnNodes[7], new ArcProto [] {SignalArcs[7], PowerArcs[7]}, "power-8", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		pnNodes[7].setFunction(PrimitiveNode.Function.NODE);
		pnNodes[7].setHoldsOutline();
		pnNodes[7].setSpecialType(PrimitiveNode.POLYGONAL);

        PrimitiveNode[] tsnNodes = new PrimitiveNode[2];
        PrimitiveNode[] bsnNodes = new PrimitiveNode[2];

		/** Top-Silk-Node */
		tsnNodes[0] = PrimitiveNode.newInstance("Top-Silk-Node", this, 1.25, 1.25, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(T_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_2)
			});
		tsnNodes[0].addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, tsnNodes[0], new ArcProto [] {Top_Silk_arc}, "top-silk", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		tsnNodes[0].setFunction(PrimitiveNode.Function.NODE);
		tsnNodes[0].setHoldsOutline();
		tsnNodes[0].setSpecialType(PrimitiveNode.POLYGONAL);

		/** Bottom-Silk-Node */
		bsnNodes[0] = PrimitiveNode.newInstance("Bottom-Silk-Node", this, 1.25, 1.25, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(B_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_2)
			});
		bsnNodes[0].addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, bsnNodes[0], new ArcProto [] {Bottom_Silk_arc}, "bottom-silk", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		bsnNodes[0].setFunction(PrimitiveNode.Function.NODE);
		bsnNodes[0].setHoldsOutline();
		bsnNodes[0].setSpecialType(PrimitiveNode.POLYGONAL);

		/** Top-Solder-Node */
		tsnNodes[1] = PrimitiveNode.newInstance("Top-Solder-Node", this, 1.25, 1.25, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(T0_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_2)
			});
		tsnNodes[1].addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, tsnNodes[1], new ArcProto [] {Top_Solder_arc}, "top-solder", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		tsnNodes[1].setFunction(PrimitiveNode.Function.NODE);
		tsnNodes[1].setHoldsOutline();
		tsnNodes[1].setSpecialType(PrimitiveNode.POLYGONAL);

		/** Bottom-Solder-Node */
		bsnNodes[1] = PrimitiveNode.newInstance("Bottom-Solder-Node", this, 1.25, 1.25, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(B0_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, box_2)
			});
		bsnNodes[1].addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, bsnNodes[1], new ArcProto [] {Bottom_Solder_arc}, "bottom-solder", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		bsnNodes[1].setFunction(PrimitiveNode.Function.NODE);
		bsnNodes[1].setHoldsOutline();
		bsnNodes[1].setSpecialType(PrimitiveNode.POLYGONAL);

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
		S_lay.setPureLayerNode(snNodes[0]);		// Signal1
		S0_lay.setPureLayerNode(snNodes[1]);		// Signal2
		S1_lay.setPureLayerNode(snNodes[2]);		// Signal3
		S2_lay.setPureLayerNode(snNodes[3]);		// Signal4
		S3_lay.setPureLayerNode(snNodes[4]);		// Signal5
		S4_lay.setPureLayerNode(snNodes[5]);		// Signal6
		S5_lay.setPureLayerNode(snNodes[6]);		// Signal7
		S6_lay.setPureLayerNode(snNodes[7]);		// Signal8
		P_lay.setPureLayerNode(pnNodes[0]);		// Power1
		P0_lay.setPureLayerNode(pnNodes[1]);		// Power2
		P1_lay.setPureLayerNode(pnNodes[2]);		// Power3
		P2_lay.setPureLayerNode(pnNodes[3]);		// Power4
		P3_lay.setPureLayerNode(pnNodes[4]);		// Power5
		P4_lay.setPureLayerNode(pnNodes[5]);		// Power6
		P5_lay.setPureLayerNode(pnNodes[6]);		// Power7
		P6_lay.setPureLayerNode(pnNodes[7]);		// Power8
		T_lay.setPureLayerNode(tsnNodes[0]);		// TopSilk
		B_lay.setPureLayerNode(bsnNodes[0]);		// BottomSilk
		T0_lay.setPureLayerNode(tsnNodes[1]);		// TopSolder
		B0_lay.setPureLayerNode(bsnNodes[1]);		// BottomSolder
		D1_lay.setPureLayerNode(edn_node);		// Drawing

        // Building information for palette
        nodeGroups = new Object[12][4];
        int count = -1;

        for (int i = 0; i < snNodes.length; i++)
        {
            // signal and power arcs
            nodeGroups[++count][0] = SignalArcs[i]; nodeGroups[count][1] = PowerArcs[i];
            // signal and power pins
            nodeGroups[count][2] = spinNodes[i]; nodeGroups[count][3] = ppinNodes[i];
        }
        nodeGroups[++count][0] = Top_Silk_arc; nodeGroups[count][1] = tspinNodes[0];
        nodeGroups[count][2] = Bottom_Silk_arc; nodeGroups[count][3] = bspinNodes[0];
        nodeGroups[++count][0] = Top_Solder_arc; nodeGroups[count][1] = tspinNodes[1];
        nodeGroups[count][2] = Bottom_Solder_arc; nodeGroups[count][3] = bspinNodes[1];
        nodeGroups[++count][0] = Drawing_arc; nodeGroups[count][1] = dp_node;
        nodeGroups[count][2] = ndp_node; nodeGroups[count][3] = edp_node;
        nodeGroups[++count][0] = "Pure"; nodeGroups[count][1] = "Misc."; nodeGroups[count][2] = "Cell";
	};
}
