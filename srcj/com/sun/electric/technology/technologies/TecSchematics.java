package com.sun.electric.technology.technologies;

import com.sun.electric.technology.Technology;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitiveArc;
import com.sun.electric.technology.PrimitivePort;
import com.sun.electric.technology.EdgeH;
import com.sun.electric.technology.EdgeV;
import com.sun.electric.database.geometry.EGraphics;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.prototype.NodeProto;

import java.awt.Point;
import java.awt.geom.Point2D;

/**
 * This is the Schematics technology.
 */
public class TecSchematics extends Technology
{
	public static final TecSchematics tech = new TecSchematics();
	/** wire-pin */						private PrimitiveNode wirePin_node;
	/** bus-pin */						private PrimitiveNode busPin_node;
	/** wire-con */						private PrimitiveNode wireCon_node;
	/** buffer */						private PrimitiveNode buffer_node;
	/** and */							private PrimitiveNode and_node;
	/** or */							private PrimitiveNode or_node;
	/** xor */							private PrimitiveNode xor_node;
	/** flipflop */						private PrimitiveNode flipflop_node;
	/** mux */							private PrimitiveNode mux_node;
	/** bbox */							private PrimitiveNode bbox_node;
	/** switch */						private PrimitiveNode switch_node;
	/** offpage */						private PrimitiveNode offpage_node;
	/** power */						private PrimitiveNode power_node;
	/** ground */						private PrimitiveNode ground_node;
	/** source */						private PrimitiveNode source_node;
	/** transistor */					private PrimitiveNode transistor_node;
	/** resistor */						private PrimitiveNode resistor_node;
	/** capacitor */					private PrimitiveNode capacitor_node;
	/** diode */						private PrimitiveNode diode_node;
	/** inductor */						private PrimitiveNode inductor_node;
	/** meter */						private PrimitiveNode meter_node;
	/** well */							private PrimitiveNode well_node;
	/** substrate */					private PrimitiveNode substrate_node;
	/** twoport */						private PrimitiveNode twoport_node;
	/** transistor-4 */					private PrimitiveNode transistor4_node;
	/** global */						private PrimitiveNode global_node;

//#define SCALABLEGATES 1   /* uncomment for experimental scalable gate code */
//
///* twentieths of a unit fractions */
//#define D1	(WHOLE/20)		/* 0.05 */
//#define FO	(WHOLE/40)		/* 0.025 */
//#define D2	(WHOLE/10)		/* 0.10 */
//#define D3	(WHOLE/20 * 3)	/* 0.15 */
//#define D4	(WHOLE/5)		/* 0.20 */
//#define D5	(WHOLE/4)		/* 0.25 */
//#define D6	(WHOLE/10 * 3)	/* 0.30 */
//#define D7	(WHOLE/20 * 7)	/* 0.35 */
//#define D8	(WHOLE/5 * 2)	/* 0.40 */
//#define D9	(WHOLE/20 * 9)	/* 0.45 */
//#define D12	(WHOLE/5 * 3)	/* 0.60 */
//#define D13	(WHOLE/20 * 13)	/* 0.65 */
//#define D14	(WHOLE/10 * 7)	/* 0.70 */
//#define D16	(WHOLE/5 * 4)	/* 0.80 */
//
///* right of center by this amount */
//#define CENTERR0Q   0,Q0	/* 0.25 */
//#define CENTERR0T   0,T0	/* 0.75 */
//#define CENTERR1Q   0,Q1	/* 1.25 */
//#define CENTERR1T   0,T1	/* 1.75 */
//#define CENTERR2Q   0,Q2	/* 2.25 */
//#define CENTERR2T   0,T2	/* 2.75 */
//#define CENTERR3Q   0,Q3	/* 3.25 */
//#define CENTERR3T   0,T3	/* 3.75 */
//
///* left of center by this amount */
//#define CENTERL0Q   0,-Q0	/* 0.25 */
//#define CENTERL0T   0,-T0	/* 0.75 */
//#define CENTERL1Q   0,-Q1	/* 1.25 */
//#define CENTERL1T   0,-T1	/* 1.75 */
//#define CENTERL2Q   0,-Q2	/* 2.25 */
//#define CENTERL2T   0,-T2	/* 2.75 */
//#define CENTERL3Q   0,-Q3	/* 3.25 */
//#define CENTERL3T   0,-T3	/* 3.75 */
//#define CENTERL4Q   0,-Q4	/* 4.25 */
//#define CENTERL9    0,-K9	/* 9.00 */
//#define CENTERL10   0,-K10	/* 10.00 */
//
///* up from center by this amount */
//#define CENTERU0Q   0,Q0	/* 0.25 */
//#define CENTERU0T   0,T0	/* 0.75 */
//#define CENTERU1Q   0,Q1	/* 1.25 */
//#define CENTERU1T   0,T1	/* 1.75 */
//#define CENTERU2Q   0,Q2	/* 2.25 */
//#define CENTERU2T   0,T2	/* 2.75 */
//#define CENTERU3Q   0,Q3	/* 3.25 */
//#define CENTERU3T   0,T3	/* 3.75 */
//
///* down from center by this amount */
//#define CENTERD0Q   0,-Q0	/* 0.25 */
//#define CENTERD0T   0,-T0	/* 0.75 */
//#define CENTERD1Q   0,-Q1	/* 1.25 */
//#define CENTERD1T   0,-T1	/* 1.75 */
//#define CENTERD2Q   0,-Q2	/* 2.25 */
//#define CENTERD2T   0,-T2	/* 2.75 */
//#define CENTERD3Q   0,-Q3	/* 3.25 */
//#define CENTERD3T   0,-T3	/* 3.75 */
//
///* this much from the center to the left edge */
//#define LEFTBYP1   -D1,0		/* 0.1 */
//#define LEFTBYP125 (-K1/15),0   /* 0.13333... */  /* wanted 0.125 but can't */
//#define LEFTBYP166 (-K1/12),0	/* 0.16666... */
//#define LEFTBYP2   -D2,0		/* 0.2 */
//#define LEFTBYP25  (-D2-FO),0   /* 0.25 */
//#define LEFTBYP3   -D3,0		/* 0.3 */
//#define LEFTBYP33  (-K1/6),0	/* 0.3333... */
//#define LEFTBYP35  -(D3+FO),0	/* 0.35 (21/60) */
//#define LEFTBYP3666 -22,0		/* 0.3666... (22/60) */
//#define LEFTBYP4   -D4,0		/* 0.4 */
//#define LEFTBYP45  -(D4+FO),0   /* 0.45 (27/60) */
//#define LEFTBYP5   -D5,0		/* 0.5 */
//#define LEFTBYP6   -D6,0		/* 0.6 */
//#define LEFTBYP6333  -38,0		/* 0.6333... (38/60) */
//#define LEFTBYP66  (-K1/3),0	/* 0.6666... */
//#define LEFTBYP7   -D7,0		/* 0.7 */
//#define LEFTBYP75  (-D7-FO),0   /* 0.75 */
//#define LEFTBYP8   -D8,0        /* 0.8 */
//#define LEFTBYP875 -D9,0        /* 0.9 */ /* wanted 0.875 but can't */
//#define LEFTBYP9   -D9,0		/* 0.9 */
//#define LEFTBYP12  -D12,0		/* 1.2 */
//#define LEFTBYP14  -D14,0		/* 1.4 */
//#define LEFTBY1P6   -H0-D6,0	/* 1.6 */
//
///* this much from the center to the right edge */
//#define RIGHTBYP1   D1,0		/* 0.1       (6/60) */
//#define RIGHTBYP125 (K1/15),0   /* 0.133...  (8/60) */ /* not precise */
//#define RIGHTBYP166 (K1/12),0	/* 0.166...  (10/60) */
//#define RIGHTBYP2   D2,0		/* 0.2       (12/60) */
//#define RIGHTBYP25  (D2+FO),0   /* 0.25      (15/60) */
//#define RIGHTBYP3   D3,0		/* 0.3       (18/60) */
//#define RIGHTBYP33  (K1/6),0	/* 0.33...   (20/60) */
//#define RIGHTBYP35  (D3+FO),0	/* 0.35      (21/60) */
//#define RIGHTBYP3666  22,0		/* 0.3666... (22/60) */
//#define RIGHTBYP3833  23,0		/* 0.3833... (23/60) */
//#define RIGHTBYP4   D4,0		/* 0.4       (24/60) */
//#define RIGHTBYP433  26,0		/* 0.433...  (26/60) */
//#define RIGHTBYP45  (D4+FO),0   /* 0.45      (27/60) */
//#define RIGHTBYP5   D5,0		/* 0.5       (30/60) */
//#define RIGHTBYP5166   31,0		/* 0.5166... (31/60) */
//#define RIGHTBYP55  (D5+FO),0	/* 0.55      (33/60) */
//#define RIGHTBYP566   34,0		/* 0.566...  (34/60) */
//#define RIGHTBYP6   D6,0		/* 0.6       (36/60) */
//#define RIGHTBYP6166   37,0		/* 0.6166... (37/60) */
//#define RIGHTBYP6333   38,0		/* 0.6333... (38/60) */
//#define RIGHTBYP66  (K1/3),0	/* 0.66...   (40/60) */
//#define RIGHTBYP7   D7,0		/* 0.7       (42/60) */
//#define RIGHTBYP75  (D7+FO),0   /* 0.75      (45/60) */
//#define RIGHTBYP8   D8,0        /* 0.8       (48/60) */
//#define RIGHTBYP875 D9,0        /* 0.9       (54/60) */ /* not precise */
//#define RIGHTBYP9   D9,0		/* 0.9       (54/60) */
//
///* this much from the center to the bottom edge */
//#define BOTBYP1    -D1,0		/* 0.1 */
//#define BOTBYP125  (-K1/15),0   /* 0.133...  (8/60) */ /* not precise */
//#define BOTBYP166  (-K1/12),0	/* 0.166... (10/60) */
//#define BOTBYP2    -D2,0		/* 0.2 */
//#define BOTBYP25   (-D2-FO),0	/* 0.25 */
//#define BOTBYP3    -D3,0		/* 0.3 */
//#define BOTBYP33   (-K1/6),0	/* 0.3333... */
//#define BOTBYP375  -D4,0        /* 0.4 */
//#define BOTBYP4    -D4,0		/* 0.4 */
//#define BOTBYP5    -D5,0		/* 0.5 */
//#define BOTBYP6    -D6,0		/* 0.6 */
//#define BOTBYP66   (-K1/3),0	/* 0.6666... */
//#define BOTBYP7    -D7,0		/* 0.7 */
//#define BOTBYP75   (-D7-FO),0	/* 0.75 */
//#define BOTBYP8    -D8,0		/* 0.8 */
//#define BOTBYP875  -D9,0        /* 0.9 */ /* wanted 0.875 but can't */
//#define BOTBYP9    -D9,0		/* 0.9 */
//
///* this much from the center to the top edge */
//#define TOPBYP1     D1,0		/* 0.1 */
//#define TOPBYP2     D2,0		/* 0.2 */
//#define TOPBYP25    (D2+FO),0   /* 0.25 */
//#define TOPBYP3     D3,0		/* 0.3 */
//#define TOPBYP33    (K1/6),0	/* 0.3333... */
//#define TOPBYP4     D4,0		/* 0.4 */
//#define TOPBYP5     D5,0		/* 0.5 */
//#define TOPBYP5833  35,0		/* 0.58333... (35/60) */
//#define TOPBYP6     D6,0		/* 0.6 */
//#define TOPBYP66    (K1/3),0	/* 0.6666... */
//#define TOPBYP7     D7,0		/* 0.7 */
//#define TOPBYP75    45,0		/* 0.75       (45/60) */
//#define TOPBYP8     D8,0		/* 0.8 */
//#define TOPBYP866   (K1/12*5),0	/* 0.8666... */
//#define TOPBYP875   D9,0        /* 0.9 */ /* wanted 0.875 but can't */
//#define TOPBYP9     D9,0		/* 0.9 */

//INTBIG            sch_meterkey;			/* key for "SCHEM_meter_type" */
//INTBIG            sch_diodekey;			/* key for "SCHEM_diode" */
//INTBIG            sch_capacitancekey;	/* key for "SCHEM_capacitance" */
//INTBIG            sch_resistancekey;	/* key for "SCHEM_resistance" */
//INTBIG            sch_inductancekey;	/* key for "SCHEM_inductance" */
//INTBIG            sch_functionkey;		/* key for "SCHEM_function" */
//INTBIG            sch_spicemodelkey;	/* key for "SIM_spice_model" */
//INTBIG            sch_globalnamekey;	/* key for "SCHEM_global_name" */
//ARCPROTO         *sch_wirearc, *sch_busarc;
//static INTBIG     sch_bubblediameter = K1+D4;

//typedef struct
//{
//	/* for arc drawing */
//	INTBIG        bubblebox, arrowbox;
//
//	/* for node drawing */
//	TECH_POLYGON *layerlist;
//
//	/* control of the bar in a switch node */
//	INTBIG        switchbarvalue;
//
//	/* control of the bus pin node */
//	INTBIG        buspinlayer;
//	INTBIG        buspinsize;
//	INTBIG       *buspinpoints;
//
//	/* for extra steiner points on nodes */
//	INTBIG        extrasteinerpoint;
//	TECH_PORTS   *extrasteinerport[10];
//} SCHPOLYLOOP;
//static SCHPOLYLOOP sch_oneprocpolyloop;
//
//static PORTPROTO *sch_anddiffports, *sch_ordiffports, *sch_xordiffports;
//INTBIG            sch_wirepinsizex;		/* X size if wire-pin primitives */
//INTBIG            sch_wirepinsizey;		/* Y size if wire-pin primitives */


	// -------------------- private and protected methods ------------------------
	private TecSchematics()
	{
		setTechName("schematic");
		setTechDesc("Schematic Capture");

		//**************************************** LAYERS ****************************************

		/** arc layer */
		Layer arc_lay = Layer.newInstance("Arc",
			new EGraphics(EGraphics.LAYERO, EGraphics.BLUE, EGraphics.SOLIDC, EGraphics.SOLIDC, 96,209,255,0.8,1,
			new int[] {0xFFFF,0xFFFF,0xFFFF,0xFFFF,0xFFFF,0xFFFF,0xFFFF,0xFFFF,
				0xFFFF,0xFFFF,0xFFFF,0xFFFF,0xFFFF,0xFFFF,0xFFFF,0xFFFF}));

		/** bus layer */
		Layer bus_lay = Layer.newInstance("Bus",
			new EGraphics(EGraphics.LAYERT3, EGraphics.COLORT3, EGraphics.SOLIDC, EGraphics.PATTERNED, 96,209,255,0.8,1,
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

		/** node layer */
		Layer node_lay = Layer.newInstance("Node",
			new EGraphics(EGraphics.LAYERO, EGraphics.RED, EGraphics.SOLIDC, EGraphics.SOLIDC, 96,209,255,0.8,1,
			new int[] {0xFFFF,0xFFFF,0xFFFF,0xFFFF,0xFFFF,0xFFFF,0xFFFF,0xFFFF,
				0xFFFF,0xFFFF,0xFFFF,0xFFFF,0xFFFF,0xFFFF,0xFFFF,0xFFFF}));

		/** text layer */
		Layer text_lay = Layer.newInstance("Text",
			new EGraphics(EGraphics.LAYERO, EGraphics.CELLTXT, EGraphics.SOLIDC, EGraphics.SOLIDC, 96,209,255,0.8,1,
			new int[] {0xFFFF,0xFFFF,0xFFFF,0xFFFF,0xFFFF,0xFFFF,0xFFFF,0xFFFF,
				0xFFFF,0xFFFF,0xFFFF,0xFFFF,0xFFFF,0xFFFF,0xFFFF,0xFFFF}));

		// The layer functions
		arc_lay.setFunction(Layer.Function.METAL1);														// arc
		bus_lay.setFunction(Layer.Function.BUS);														// bus
		node_lay.setFunction(Layer.Function.ART);														// node
		text_lay.setFunction(Layer.Function.ART);														// text


		//**************************************** ARCS ****************************************

		/** wire arc */
		PrimitiveArc wire_arc = PrimitiveArc.newInstance(this, "wire", 0.0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(arc_lay, 0, Poly.Type.FILLED),
			new Technology.ArcLayer(arc_lay, 0, Poly.Type.CIRCLE),
			new Technology.ArcLayer(arc_lay, 0, Poly.Type.VECTORS)
		});
		wire_arc.setFunction(PrimitiveArc.Function.METAL1);
		wire_arc.setFixedAngle();
		wire_arc.clearSlidable();
		wire_arc.setAngleIncrement(45);

		/** bus arc */
		PrimitiveArc bus_arc = PrimitiveArc.newInstance(this, "bus", 1.0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(bus_lay, 0, Poly.Type.FILLED),
			new Technology.ArcLayer(bus_lay, 0, Poly.Type.CIRCLE),
			new Technology.ArcLayer(arc_lay, 0, Poly.Type.VECTORS)
		});
		bus_arc.setFunction(PrimitiveArc.Function.BUS);
		bus_arc.setFixedAngle();
		bus_arc.clearSlidable();
		bus_arc.setAngleIncrement(45);


		//**************************************** NODES ****************************************

//static CHAR sch_NULLSTR[] = {x_("")};
//static CHAR sch_D[] = {x_("D")};
//static CHAR sch_E[] = {x_("E")};
//static CHAR sch_J[] = {x_("J")};
//static CHAR sch_K[] = {x_("K")};
//static CHAR sch_Q[] = {x_("Q")};
//static CHAR sch_R[] = {x_("R")};
//static CHAR sch_S[] = {x_("S")};
//static CHAR sch_T[] = {x_("T")};
//static CHAR sch_V[] = {x_("V")};
//static CHAR sch_PR[] = {x_("PR")};
//static CHAR sch_QB[] = {x_("QB")};
//
//static INTBIG sch_g_pindisc[]    = {CENTER,    CENTER,    RIGHTEDGE, CENTER};
//static INTBIG sch_g_buspindisc[] = {CENTER,    CENTER,    RIGHTBYP5, CENTER};
//static INTBIG sch_g_bustapdisc[] = {CENTER,    CENTER,    RIGHTBYP25,CENTER};
//static CHAR  *sch_g_wireconj[]   = {0,  0,     0, 0,      sch_J};
//static INTBIG sch_g_inv[]        = {RIGHTBYP66,CENTER,    LEFTEDGE,  TOPBYP875,
//								    LEFTEDGE,  BOTBYP875};
//static INTBIG sch_g_and[]        = {CENTERR0H, CENTER,    CENTERR0H, CENTERU3,
//								    CENTERR0H, CENTERD3};
//static INTBIG sch_g_andbox[]     = {CENTERR0H, CENTERU3,  CENTERL4,  CENTERU3,
//								    CENTERL4,  TOPEDGE,   CENTERL4,  BOTEDGE,
//								    CENTERL4,  CENTERD3,  CENTERR0H, CENTERD3};
//static INTBIG sch_g_or[]         = {CENTERL4,  TOPEDGE,   CENTERL4,  CENTERU3,
//								    CENTERL4,  CENTERU3,  CENTERL0T, CENTERU3,
//								    CENTERL4,  BOTEDGE,   CENTERL4,  CENTERD3,
//								    CENTERL4,  CENTERD3,  CENTERL0T, CENTERD3};
//static INTBIG sch_g_ort[]        = {CENTERL0T, CENTERD3,  CENTERL0T, CENTERU3,
//								    CENTERR4H, CENTER};
//static INTBIG sch_g_orb[]        = {CENTERL0T, CENTERU3,  CENTERR4H, CENTER,
//								    CENTERL0T, CENTERD3};
//static INTBIG sch_g_orl[]        = {CENTERL9,  CENTER,    CENTERL4,  CENTERU3,
//								    CENTERL4,  CENTERD3};
//static INTBIG sch_g_xor[]        = {CENTERL10, CENTER,    CENTERL5,  CENTERU3,
//								    CENTERL5,  CENTERD3};
//static INTBIG sch_g_ffbox[]      = {LEFTEDGE,  BOTEDGE,   RIGHTEDGE, TOPEDGE};
//static INTBIG sch_g_ffarrow[]    = {LEFTEDGE,  BOTBYP2,   LEFTBYP7,  CENTER,
//								    LEFTEDGE,  TOPBYP2};
//static CHAR *sch_g_fftextd[]    = {(CHAR *)-H0,  0,  (CHAR *)D4,0,   (CHAR *)-H0,  0,  (CHAR *)D8,0,
//								   (CHAR *)-D4,0,  (CHAR *)D8,0,   (CHAR *)-D4,0,  (CHAR *)D4,0,
//								   sch_D};
//static CHAR *sch_g_fftexte[]    = {(CHAR *)-H0,  0,  (CHAR *)-D4,0,   (CHAR *)-H0,  0,  (CHAR *)-D8,0,
//								   (CHAR *)-D4,0,  (CHAR *)-D8,0,   (CHAR *)-D4,0,  (CHAR *)-D4,0,
//								   sch_E};
//static CHAR *sch_g_fftextq[]    = {(CHAR *)H0,  0, (CHAR *)D4,0,   (CHAR *)H0,  0, (CHAR *)D8,0,
//								   (CHAR *)D4,0, (CHAR *)D8,0,   (CHAR *)D4,0, (CHAR *)D4,0,
//								   sch_Q};
//static CHAR *sch_g_fftextqb[]   = {(CHAR *)H0,  0, (CHAR *)-D4,0,   (CHAR *)H0,  0, (CHAR *)-D8,0,
//								   (CHAR *)D4,0, (CHAR *)-D8,0,   (CHAR *)D4,0, (CHAR *)-D4,0,
//								   sch_QB};
//static CHAR *sch_g_fftextpr[]   = {(CHAR *)-D6,0,  (CHAR *)D6,0,   (CHAR *)-D6,0,  (CHAR *)H0,  0,
//								   (CHAR *)D6,0, (CHAR *)H0,  0,   (CHAR *)D6,0, (CHAR *)D6,0,
//								   sch_PR};
//static CHAR *sch_g_fftextclr[]  = {(CHAR *)-D6,0,  (CHAR *)-D6,0,   (CHAR *)-D6,0,  (CHAR *)-H0,  0,
//								   (CHAR *)D6,0, (CHAR *)-H0,  0,   (CHAR *)D6,0, (CHAR *)-D6,0,
//								   0 /* "CLR" */};
//static CHAR *sch_g_meterv[]     = {(CHAR *)-H0,  0,  (CHAR *)-H0,  0,   (CHAR *)H0,  0, (CHAR *)H0,  0,
//								   sch_V};
//static INTBIG sch_g_ffn[]        = {LEFTBYP6,  TOPBYP2,   LEFTBYP4,  TOPBYP2,
//								    LEFTBYP4,  BOTBYP2,   LEFTBYP2,  BOTBYP2};
//static INTBIG sch_g_ffp[]        = {LEFTBYP6,  BOTBYP2,   LEFTBYP4,  BOTBYP2,
//								    LEFTBYP4,  TOPBYP2,   LEFTBYP2,  TOPBYP2};
//static INTBIG sch_g_ffms[]       = {LEFTBYP6,  BOTBYP2,   LEFTBYP4,  BOTBYP2,
//								    LEFTBYP4,  TOPBYP2,   LEFTBYP2,  TOPBYP2,
//								    LEFTBYP2,  BOTBYP2,   CENTER,    BOTBYP2};
//static INTBIG sch_g_mux[]        = {RIGHTBYP8, TOPBYP75,  RIGHTBYP8, BOTBYP75,
//								    LEFTBYP8,  BOTEDGE,   LEFTBYP8,  TOPEDGE};
//static INTBIG sch_g_bbox[]       = {LEFTEDGE,  BOTEDGE,   RIGHTEDGE, TOPEDGE};
//static INTBIG sch_g_switchin[]   = {RIGHTIN1,  CENTER,    RIGHTIN1Q, CENTER};
//static INTBIG sch_g_switchbar[]  = {RIGHTIN1,  CENTER,    LEFTIN1,   CENTER};
//static INTBIG sch_g_switchout[]  = {LEFTIN1,   BOTIN0H,   LEFTIN0T,  BOTIN0H};
//static INTBIG sch_g_offpage[]    = {LEFTEDGE,  BOTEDGE,   LEFTEDGE,  TOPEDGE,
//								    RIGHTBYP5, TOPEDGE,   RIGHTEDGE, CENTER,
//								    RIGHTBYP5, BOTEDGE};
//static INTBIG sch_g_pwr1[]       = {CENTER,    CENTER,    CENTER,    TOPEDGE};
//static INTBIG sch_g_pwr2[]       = {CENTER,    CENTER,    CENTER,    TOPBYP75};
//static INTBIG sch_g_gnd[]        = {CENTER,    CENTER,    CENTER,    TOPEDGE,
//								    LEFTEDGE,  CENTER,    RIGHTEDGE, CENTER,
//								    LEFTBYP75, BOTBYP25,  RIGHTBYP75,BOTBYP25,
//								    LEFTBYP5,  BOTBYP5,   RIGHTBYP5, BOTBYP5,
//								    LEFTBYP25, BOTBYP75,  RIGHTBYP25,BOTBYP75,
//								    CENTER,    BOTEDGE,   CENTER,    BOTEDGE};
//static INTBIG sch_g_resist[]     = {LEFTBYP66, CENTER,    LEFTBYP6,  CENTER,
//								    LEFTBYP5,  TOPEDGE,   LEFTBYP3,  BOTEDGE,
//								    LEFTBYP1,  TOPEDGE,   RIGHTBYP1, BOTEDGE,
//								    RIGHTBYP3, TOPEDGE,   RIGHTBYP5, BOTEDGE,
//								    RIGHTBYP6, CENTER,    RIGHTBYP66,CENTER};
//static INTBIG sch_g_capac[]      = {LEFTEDGE,  TOPBYP2,   RIGHTEDGE, TOPBYP2,
//								    LEFTEDGE,  BOTBYP2,   RIGHTEDGE, BOTBYP2,
//								    CENTER,    TOPBYP2,   CENTER,    TOPEDGE,
//								    CENTER,    BOTBYP2,   CENTER,    BOTEDGE};
//static INTBIG sch_g_capace[]     = {RIGHTBYP2, BOTBYP6,   RIGHTBYP6, BOTBYP6,
//								    RIGHTBYP4, BOTBYP4,   RIGHTBYP4, BOTBYP8};
//static INTBIG sch_g_source[]     = {CENTER,    CENTER,    RIGHTEDGE, CENTER};
//static INTBIG sch_g_sourcepl[]   = {LEFTBYP3,  TOPBYP6,   RIGHTBYP3, TOPBYP6,
//								    CENTER,    TOPBYP3,   CENTER,    TOPBYP9};
//static INTBIG sch_g_mos[]        = {LEFTEDGE,  BOTEDGE,   LEFTBYP75, BOTEDGE,
//								    LEFTBYP75, BOTBYP5,   RIGHTBYP75,BOTBYP5,
//								    RIGHTBYP75,BOTEDGE,   RIGHTEDGE, BOTEDGE};
//static INTBIG sch_g_trantop[]    = {LEFTBYP75, BOTBYP25,  RIGHTBYP75,BOTBYP25};
//static INTBIG sch_g_nmos[]       = {CENTER,    BOTBYP25,  CENTER,    TOPIN1};
//static INTBIG sch_g_nmos4[]      = {LEFTBYP5,  BOTBYP5,   LEFTBYP5,  BOTEDGE,
//									LEFTBYP5,  BOTBYP5,   LEFTBYP35, BOTBYP75,
//									LEFTBYP5,  BOTBYP5,   LEFTBYP66, BOTBYP75};
//static INTBIG sch_g_dmos4[]      = {LEFTBYP5,  BOTBYP75,  LEFTBYP5,  BOTEDGE,
//									LEFTBYP5,  BOTBYP75,  LEFTBYP35, BOTBYP9,
//									LEFTBYP5,  BOTBYP75,  LEFTBYP66, BOTBYP9};
//static INTBIG sch_g_pmos4[]      = {LEFTBYP5,  BOTBYP5,   LEFTBYP5,  BOTEDGE,
//									LEFTBYP5,  BOTEDGE,   LEFTBYP35, BOTBYP75,
//									LEFTBYP5,  BOTEDGE,   LEFTBYP66, BOTBYP75};
//static INTBIG sch_g_bip4[]       = {LEFTBYP5,  BOTEDGE,   CENTER,    BOTBYP25};
//static INTBIG sch_g_nmes4[]      = {LEFTBYP5,  BOTBYP25,  LEFTBYP5,  BOTEDGE,
//									LEFTBYP5,  BOTBYP25,  LEFTBYP35, BOTBYP5,
//									LEFTBYP5,  BOTBYP25,  LEFTBYP66, BOTBYP5};
//static INTBIG sch_g_pmes4[]      = {LEFTBYP5,  BOTBYP25,  LEFTBYP5,  BOTEDGE,
//									LEFTBYP5,  BOTEDGE,   LEFTBYP35, BOTBYP75,
//									LEFTBYP5,  BOTEDGE,   LEFTBYP66, BOTBYP75};
//static INTBIG sch_g_pmos[]       = {CENTER,    TOPBYP25,  CENTER,    TOPIN1};
//static INTBIG sch_g_pmoscir[]    = {CENTER,    CENTER,    CENTER,    BOTBYP25};
//static INTBIG sch_g_dmos[]       = {LEFTBYP75, BOTBYP75,  RIGHTBYP75,BOTBYP5};
//static INTBIG sch_g_btran1[]     = {LEFTEDGE,  BOTEDGE,   LEFTBYP75, BOTEDGE,
//								    LEFTBYP25, BOTBYP25,  RIGHTBYP25,BOTBYP25,
//								    RIGHTBYP75,BOTEDGE,   RIGHTEDGE, BOTEDGE};
//static INTBIG sch_g_btran2[]     = {LEFTBYP75, BOTBYP75,  LEFTBYP75, BOTEDGE,
//								    LEFTBYP5,  BOTBYP875};
//static INTBIG sch_g_btran3[]     = {LEFTBYP5,  BOTBYP375, LEFTBYP25, BOTBYP25,
//								    LEFTBYP25, BOTBYP5};
//static INTBIG sch_g_btran4[]     = {LEFTEDGE,  BOTEDGE,   LEFTBYP75, BOTEDGE,
//									LEFTBYP75, BOTEDGE,   LEFTBYP75, BOTBYP25,
//									LEFTBYP875,BOTBYP25,  RIGHTBYP875,BOTBYP25,
//									RIGHTBYP75,BOTBYP25,  RIGHTBYP75,BOTEDGE,
//									RIGHTBYP75,BOTEDGE,   RIGHTEDGE, BOTEDGE};
//static INTBIG sch_g_btran5[]     = {LEFTBYP125,CENTER,    CENTER,    BOTBYP25,
//								    RIGHTBYP125,CENTER};
//static INTBIG sch_g_btran6[]     = {LEFTBYP125,CENTER,    CENTER,    TOPBYP25,
//								    RIGHTBYP125,CENTER};
//static INTBIG sch_g_btran7[]     = {LEFTEDGE,  BOTEDGE,   LEFTBYP75, BOTEDGE,
//								    LEFTBYP75, BOTEDGE,   LEFTBYP75, BOTBYP25,
//								    LEFTBYP875,BOTBYP25,  LEFTBYP5,  BOTBYP25,
//								    LEFTBYP25, BOTBYP25,  RIGHTBYP25,BOTBYP25,
//								    RIGHTBYP5, BOTBYP25,  RIGHTBYP875,BOTBYP25,
//								    RIGHTBYP75,BOTBYP25,  RIGHTBYP75,BOTEDGE,
//								    RIGHTBYP75,BOTEDGE,   RIGHTEDGE, BOTEDGE};
//static INTBIG sch_g_diode1[]     = {LEFTEDGE,  TOPBYP5,   RIGHTEDGE, TOPBYP5,
//								    CENTER,    TOPBYP5,   CENTER,    TOPEDGE,
//								    CENTER,    BOTBYP5,   CENTER,    BOTEDGE};
//static INTBIG sch_g_diode2[]     = {LEFTEDGE,  BOTBYP5,   RIGHTEDGE, BOTBYP5,
//								    CENTER,    TOPBYP5};
//static INTBIG sch_g_diode3[]     = {LEFTEDGE,  TOPBYP75,  LEFTEDGE,  TOPBYP5,
//								    LEFTEDGE,  TOPBYP5,   RIGHTEDGE, TOPBYP5,
//								    RIGHTEDGE, TOPBYP5,   RIGHTEDGE, TOPBYP25,
//								    CENTER,    TOPBYP5,   CENTER,    TOPEDGE,
//								    CENTER,    BOTBYP5,   CENTER,    BOTEDGE};
//static INTBIG sch_g_induct1[]    = {CENTER,    TOPEDGE,   CENTER,    BOTEDGE};
//static INTBIG sch_g_induct2[]    = {LEFTBYP5,  TOPBYP33,  CENTER,    TOPBYP33};
//static INTBIG sch_g_induct3[]    = {LEFTBYP5,  CENTER,    CENTER,    CENTER};
//static INTBIG sch_g_induct4[]    = {LEFTBYP5,  BOTBYP33,  CENTER,    BOTBYP33};
//static INTBIG sch_g_meter[]      = {CENTER,    CENTER,    RIGHTEDGE, CENTER};
//static INTBIG sch_g_well[]       = {LEFTEDGE,  BOTEDGE,   RIGHTEDGE, BOTEDGE,
//								    CENTER,    TOPEDGE,   CENTER,    BOTEDGE};
//static INTBIG sch_g_global1[]    = {LEFTEDGE,  CENTER,    CENTER,    TOPEDGE,
//								    RIGHTEDGE, CENTER,    CENTER,    BOTEDGE};
//static INTBIG sch_g_global2[]    = {LEFTBYP9,  CENTER,    CENTER,    TOPBYP9,
//								    RIGHTBYP9, CENTER,    CENTER,    BOTBYP9};
//static INTBIG sch_g_substrate[]  = {CENTER,    CENTER,    CENTER,    TOPEDGE,
//								    LEFTEDGE,  CENTER,    RIGHTEDGE, CENTER,
//								    LEFTEDGE,  CENTER,    CENTER,    BOTEDGE,
//								    RIGHTEDGE, CENTER,    CENTER,    BOTEDGE};
//
//static INTBIG sch_g_twocsarr[]   = {RIGHTBYP3833,TOPBYP33,RIGHTBYP3833,BOTBYP33,
//								    RIGHTBYP3833,BOTBYP33,RIGHTBYP33,BOTBYP166,
//								    RIGHTBYP3833,BOTBYP33,RIGHTBYP433,BOTBYP166};
//static INTBIG sch_g_twoulpl[]    = {LEFTBYP35, TOPBYP66,  LEFTBYP45, TOPBYP66,
//								    LEFTBYP4,  TOPBYP5833,LEFTBYP4,  TOPBYP75};
//static INTBIG sch_g_twourpl[]    = {RIGHTBYP35,TOPBYP66,  RIGHTBYP45,TOPBYP66,
//								    RIGHTBYP4, TOPBYP5833,RIGHTBYP4, TOPBYP75};
//static INTBIG sch_g_twourrpl[]   = {RIGHTBYP5166,TOPBYP66,RIGHTBYP6166,TOPBYP66,
//								    RIGHTBYP566,TOPBYP5833,RIGHTBYP566,TOPBYP75};
//static INTBIG sch_g_twobox[]     = {LEFTBYP8,  BOTEDGE,   RIGHTBYP8, TOPEDGE};
//static INTBIG sch_g_twogwire[]   = {LEFTEDGE,  TOPBYP66,  LEFTBYP8,  TOPBYP66,
//								    LEFTEDGE,  BOTBYP66,  LEFTBYP8,  BOTBYP66,
//								    RIGHTEDGE, TOPBYP66,  RIGHTBYP8, TOPBYP66,
//								    RIGHTEDGE, BOTBYP66,  RIGHTBYP8, BOTBYP66};
//static INTBIG sch_g_twonormwire[]= {LEFTEDGE,  TOPBYP66,  LEFTBYP6,  TOPBYP66,
//								    LEFTEDGE,  BOTBYP66,  LEFTBYP6,  BOTBYP66,
//								    RIGHTEDGE, TOPBYP66,  RIGHTBYP6, TOPBYP66,
//								    RIGHTBYP6, TOPBYP66,  RIGHTBYP6, TOPBYP3,
//								    RIGHTEDGE, BOTBYP66,  RIGHTBYP6, BOTBYP66,
//								    RIGHTBYP6, BOTBYP66,  RIGHTBYP6, BOTBYP3};
//static INTBIG sch_g_twoccwire[]  = {LEFTBYP6,  TOPBYP66,  LEFTBYP6,  BOTBYP66};
//static INTBIG sch_g_twocswire[]  = {RIGHTBYP6, TOPBYP3,   RIGHTBYP45,CENTER,
//								    RIGHTBYP45,CENTER,    RIGHTBYP6, BOTBYP3,
//								    RIGHTBYP6, BOTBYP3,   RIGHTBYP75,CENTER,
//								    RIGHTBYP75,CENTER,    RIGHTBYP6, TOPBYP3};
//static INTBIG sch_g_twovsc[]     = {RIGHTBYP6, CENTER,    RIGHTBYP6, TOPBYP3};
//static INTBIG sch_g_twotr1[]     = {CENTER,    CENTER,    LEFTBYP8,  BOTEDGE,
//								    LEFTBYP8,  TOPEDGE};
//static INTBIG sch_g_twotr2[]     = {LEFTBY1P6, CENTER,    LEFTBYP8,  TOPEDGE,
//								    LEFTBYP8,  BOTEDGE};
//static INTBIG sch_g_twotr3[]     = {CENTER,    CENTER,    RIGHTBYP8, TOPEDGE,
//								    RIGHTBYP8, BOTEDGE};
//static INTBIG sch_g_twotrbox[]   = {LEFTBYP8,  TOPEDGE,   RIGHTBYP8, TOPEDGE,
//								    LEFTBYP8,  BOTEDGE,   RIGHTBYP8, BOTEDGE};
//static INTBIG sch_g_twotrwire[]  = {LEFTEDGE,  TOPBYP66,  LEFTBYP8,  TOPBYP66,
//								    LEFTEDGE,  BOTBYP66,  LEFTBYP8,  BOTBYP66,
//								    RIGHTEDGE, TOPBYP66,  RIGHTBYP9, TOPBYP66,
//								    RIGHTEDGE, BOTBYP66,  RIGHTBYP9, BOTBYP66};

		/** wire pin */
		wirePin_node = PrimitiveNode.newInstance("Wire_Pin", this, 0.5, 0.5, 0.0, 0.0,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(arc_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.AtCenter, EdgeV.AtCenter),
					new Technology.TechPoint(EdgeH.RightEdge, EdgeV.AtCenter)})
			});
		wirePin_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, wirePin_node, new ArcProto[] {wire_arc}, "wire", 0,180, 0, PortProto.Function.UNKNOWN,
					EdgeH.AtCenter, EdgeV.AtCenter, EdgeH.AtCenter, EdgeV.AtCenter)
			});
		wirePin_node.setFunction(NodeProto.Function.PIN);
		wirePin_node.setSquare();
		wirePin_node.setArcsWipe();

		/** bus pin */
		busPin_node = PrimitiveNode.newInstance("Bus_Pin", this, 2.0, 2.0, 0.0, 0.0,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(bus_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.AtCenter, EdgeV.AtCenter),
					new Technology.TechPoint(EdgeH.RightEdge, EdgeV.AtCenter)}),
				new Technology.NodeLayer(arc_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.AtCenter, EdgeV.AtCenter),
					new Technology.TechPoint(EdgeH.RightEdge, EdgeV.AtCenter)})
			});
		busPin_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, busPin_node, new ArcProto[] {wire_arc}, "bus", 0,180, 0, PortProto.Function.UNKNOWN,
					EdgeH.AtCenter, EdgeV.AtCenter, EdgeH.AtCenter, EdgeV.AtCenter)
			});
		busPin_node.setFunction(NodeProto.Function.PIN);
		busPin_node.setSquare();
		busPin_node.setArcsWipe();

		/** wire con */
		wireCon_node = PrimitiveNode.newInstance("Wire_Con", this, 2.0, 2.0, 0.0, 0.0,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(node_lay, 0, Poly.Type.CLOSEDRECT, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX),
				new Technology.NodeLayer(text_lay, 0, Poly.Type.TEXTCENT, Technology.NodeLayer.POINTS, Technology.TechPoint.ATCENTER)
			});
		wireCon_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, wireCon_node, new ArcProto[] {wire_arc}, "wire", 0,180, 0, PortProto.Function.UNKNOWN,
					EdgeH.fromLeft(0.5), EdgeV.fromBottom(0.5), EdgeH.fromRight(0.5), EdgeV.fromTop(0.5))
			});
		wireCon_node.setFunction(NodeProto.Function.CONNECT);


///* wire-con */
//static TECH_PORTS sch_wirecon_p[] = {				/* ports */
//	{new ArcProto[] {wire_arc, bus_arc}, x_("wire"), NOPORTPROTO, (180<<PORTARANGESH)|PORTISOLATED,
//		LEFTIN0H, BOTIN0H, RIGHTIN0H, TOPIN0H}};
//static TECH_POLYGON sch_wirecon_l[] = {				/* layers */
//	{node_lay, 0,                4, CLOSEDRECT, BOX, sch_g_bbox},
//	{text_lay, TXTSETQLAMBDA(8), 1, TEXTCENT, POINTS, (INTBIG *)sch_g_wireconj}};
//static TECH_NODES sch_wirecon = {
//	x_("Wire_Con"),NWIRECON,NONODEPROTO,			/* name */
//	K2,K2,											/* size */
//	1,sch_wirecon_p,								/* ports */
//	2,sch_wirecon_l,								/* layers */
//	(NPCONNECT<<NFUNCTIONSH),						/* userbits */
//	0,0,0,0,0,0,0,0,0};								/* characteristics */
//
///* general buffer */
//static TECH_PORTS sch_buf_p[] = {					/* ports */
//	{new ArcProto[] {wire_arc, bus_arc}, x_("a"), NOPORTPROTO, (180<<PORTANGLESH)|(0<<PORTARANGESH)|
//		(0<<PORTNETSH)|INPORT, LEFTEDGE, CENTER, LEFTEDGE, CENTER},
//	{ new ArcProto[] {wire_arc}, x_("c"), NOPORTPROTO, (270<<PORTANGLESH)|(0<<PORTARANGESH)|
//		(1<<PORTNETSH)|INPORT, CENTER, BOTBYP33, CENTER, BOTBYP33},
//	{new ArcProto[] {wire_arc, bus_arc}, x_("y"), NOPORTPROTO, (0<<PORTANGLESH)|(0<<PORTARANGESH)|
//		(2<<PORTNETSH)|OUTPORT, RIGHTBYP66, CENTER, RIGHTBYP66, CENTER}};
//static TECH_POLYGON sch_buf_l[] = {					/* layers */
//	{node_lay, 0, 3, CLOSED, POINTS, sch_g_inv}};
//static TECH_NODES sch_buf = {
//	x_("Buffer"), NBUF, NONODEPROTO,				/* name */
//	K6,K6,											/* size */
//	3,sch_buf_p,									/* ports */
//	1,sch_buf_l,									/* layers */
//	(NPBUFFER<<NFUNCTIONSH),						/* userbits */
//	0,0,0,0,0,0,0,0,0};								/* characteristics */
//
///* general and */
//static TECH_PORTS sch_and_p[] = {					/* ports */
//	{new ArcProto[] {wire_arc, bus_arc}, x_("a"), NOPORTPROTO, (180<<PORTANGLESH)|(0<<PORTARANGESH)|
//		(0<<PORTNETSH)|INPORT|PORTISOLATED, CENTERL4,BOTEDGE,CENTERL4,TOPEDGE},
//	{new ArcProto[] {wire_arc, bus_arc}, x_("y"), NOPORTPROTO, (0<<PORTANGLESH)|(0<<PORTARANGESH)|
//		(1<<PORTNETSH)|OUTPORT, CENTERR3H, CENTER, CENTERR3H, CENTER},
//	{new ArcProto[] {wire_arc, bus_arc}, x_("yt"), NOPORTPROTO, (0<<PORTANGLESH)|(0<<PORTARANGESH)|
//		(2<<PORTNETSH)|OUTPORT, CENTERR2T, CENTERU2, CENTERR2T, CENTERU2},
//	{new ArcProto[] {wire_arc, bus_arc}, x_("yc"), NOPORTPROTO, (0<<PORTANGLESH)|(0<<PORTARANGESH)|
//		(3<<PORTNETSH)|OUTPORT, CENTERR2T, CENTERD2, CENTERR2T, CENTERD2}};
//static TECH_POLYGON sch_and_l[] = {					/* layers */
//	{node_lay, 0, 3, CIRCLEARC,  POINTS, sch_g_and},
//	{node_lay, 0, 6, OPENED,     POINTS, sch_g_andbox}};
//static TECH_NODES sch_and = {
//	x_("And"), NAND, NONODEPROTO,					/* name */
//	K8,K6,											/* size */
//	4,sch_and_p,									/* ports */
//	2,sch_and_l,									/* layers */
//	(NPGATEAND<<NFUNCTIONSH),						/* userbits */
//	0,0,0,0,0,0,0,0,0};								/* characteristics */
//
///* general or */
//static TECH_PORTS sch_or_p[] = {					/* ports */
//	{new ArcProto[] {wire_arc, bus_arc}, x_("a"), NOPORTPROTO, (180<<PORTANGLESH)|(0<<PORTARANGESH)|
//		(0<<PORTNETSH)|INPORT|PORTISOLATED, CENTERL4,BOTEDGE, CENTERL3,TOPEDGE},
//	{new ArcProto[] {wire_arc, bus_arc}, x_("y"), NOPORTPROTO, (0<<PORTANGLESH)|(0<<PORTARANGESH)|
//		(1<<PORTNETSH)|OUTPORT, CENTERR4H, CENTER, CENTERR4H, CENTER},
//	{new ArcProto[] {wire_arc, bus_arc}, x_("yt"), NOPORTPROTO, (0<<PORTANGLESH)|(0<<PORTARANGESH)|
//		(2<<PORTNETSH)|OUTPORT, CENTERR2+D13, CENTERU2, CENTERR2+D13, CENTERU2},
//	{new ArcProto[] {wire_arc, bus_arc}, x_("yc"), NOPORTPROTO, (0<<PORTANGLESH)|(0<<PORTARANGESH)|
//		(3<<PORTNETSH)|OUTPORT, CENTERR2+D13, CENTERD2, CENTERR2+D13, CENTERD2}};
//static TECH_POLYGON sch_or_l[] = {					/* layers */
//	{node_lay, 0, 3, CIRCLEARC, POINTS, sch_g_orl},
//	{node_lay, 0, 3, CIRCLEARC, POINTS, sch_g_ort},
//	{node_lay, 0, 3, CIRCLEARC, POINTS, sch_g_orb},
//	{node_lay, 0, 8, VECTORS,   POINTS, sch_g_or}};
//static TECH_NODES sch_or = {
//	x_("Or"), NOR, NONODEPROTO,						/* name */
//	K10,K6,											/* size */
//	4,sch_or_p,										/* ports */
//	4,sch_or_l,										/* layers */
//	(NPGATEOR<<NFUNCTIONSH),						/* userbits */
//	0,0,0,0,0,0,0,0,0};								/* characteristics */
//
///* general xor */
//static TECH_PORTS sch_xor_p[] = {					/* ports */
//	{new ArcProto[] {wire_arc, bus_arc}, x_("a"), NOPORTPROTO, (180<<PORTANGLESH)|(0<<PORTARANGESH)|
//		(0<<PORTNETSH)|INPORT|PORTISOLATED, CENTERL4,BOTEDGE, CENTERL3,TOPEDGE},
//	{new ArcProto[] {wire_arc, bus_arc}, x_("y"), NOPORTPROTO, (0<<PORTANGLESH)|(0<<PORTARANGESH)|
//		(1<<PORTNETSH)|OUTPORT, CENTERR4H, CENTER, CENTERR4H, CENTER},
//	{new ArcProto[] {wire_arc, bus_arc}, x_("yt"), NOPORTPROTO, (0<<PORTANGLESH)|(0<<PORTARANGESH)|
//		(2<<PORTNETSH)|OUTPORT, CENTERR2+D13, CENTERU2, CENTERR2+D13, CENTERU2},
//	{new ArcProto[] {wire_arc, bus_arc}, x_("yc"), NOPORTPROTO, (0<<PORTANGLESH)|(0<<PORTARANGESH)|
//		(3<<PORTNETSH)|OUTPORT, CENTERR2+D13, CENTERD2, CENTERR2+D13, CENTERD2}};
//static TECH_POLYGON sch_xor_l[] = {					/* layers */
//	{node_lay, 0, 3, CIRCLEARC, POINTS, sch_g_orl},
//	{node_lay, 0, 3, CIRCLEARC, POINTS, sch_g_ort},
//	{node_lay, 0, 3, CIRCLEARC, POINTS, sch_g_orb},
//	{node_lay, 0, 3, CIRCLEARC, POINTS, sch_g_xor},
//	{node_lay, 0, 8, VECTORS,   POINTS, sch_g_or}};
//static TECH_NODES sch_xor = {
//	x_("Xor"), NXOR, NONODEPROTO,					/* name */
//	K10,K6,											/* size */
//	4,sch_xor_p,									/* ports */
//	5,sch_xor_l,									/* layers */
//	(NPGATEXOR<<NFUNCTIONSH),						/* userbits */
//	0,0,0,0,0,0,0,0,0};								/* characteristics */
//
///* general flip flop */
//static TECH_PORTS sch_ff_p[] = {					/* ports */
//	{ new ArcProto[] {wire_arc}, x_("i1"), NOPORTPROTO, (180<<PORTANGLESH)|(45<<PORTARANGESH)|
//		(0<<PORTNETSH)|INPORT,  LEFTEDGE, TOPBYP6, LEFTEDGE, TOPBYP6},
//	{ new ArcProto[] {wire_arc}, x_("i2"), NOPORTPROTO, (180<<PORTANGLESH)|(45<<PORTARANGESH)|
//		(1<<PORTNETSH)|INPORT,  LEFTEDGE, BOTBYP6, LEFTEDGE, BOTBYP6},
//	{ new ArcProto[] {wire_arc}, x_("q"), NOPORTPROTO, (0<<PORTANGLESH)|(45<<PORTARANGESH)|
//		(2<<PORTNETSH)|OUTPORT, RIGHTEDGE, TOPBYP6, RIGHTEDGE, TOPBYP6},
//	{ new ArcProto[] {wire_arc}, x_("qb"), NOPORTPROTO, (0<<PORTANGLESH)|(45<<PORTARANGESH)|
//		(3<<PORTNETSH)|OUTPORT, RIGHTEDGE, BOTBYP6, RIGHTEDGE, BOTBYP6},
//	{ new ArcProto[] {wire_arc}, x_("ck"), NOPORTPROTO, (180<<PORTANGLESH)|(45<<PORTARANGESH)|
//		(4<<PORTNETSH)|INPORT,  LEFTEDGE, CENTER, LEFTEDGE, CENTER},
//	{ new ArcProto[] {wire_arc}, x_("preset"), NOPORTPROTO, (90<<PORTANGLESH)|(45<<PORTARANGESH)|
//		(5<<PORTNETSH)|INPORT,  CENTER, TOPEDGE, CENTER, TOPEDGE},
//	{ new ArcProto[] {wire_arc}, x_("clear"), NOPORTPROTO,(270<<PORTANGLESH)|(45<<PORTARANGESH)|
//		(6<<PORTNETSH)|INPORT,  CENTER, BOTEDGE, CENTER, BOTEDGE}};
//static TECH_POLYGON sch_ffp_l[] = {					/* layers */
//	{node_lay, 0,                4, CLOSEDRECT, BOX,    sch_g_ffbox},
//	{text_lay, TXTSETQLAMBDA(4), 4, TEXTBOX,    POINTS, (INTBIG *)sch_g_fftextd},
//	{text_lay, TXTSETQLAMBDA(4), 4, TEXTBOX,    POINTS, (INTBIG *)sch_g_fftexte},
//	{text_lay, TXTSETQLAMBDA(4), 4, TEXTBOX,    POINTS, (INTBIG *)sch_g_fftextq},
//	{text_lay, TXTSETQLAMBDA(4), 4, TEXTBOX,    POINTS, (INTBIG *)sch_g_fftextqb},
//	{text_lay, TXTSETQLAMBDA(4), 4, TEXTBOX,    POINTS, (INTBIG *)sch_g_fftextpr},
//	{text_lay, TXTSETQLAMBDA(4), 4, TEXTBOX,    POINTS, (INTBIG *)sch_g_fftextclr},
//	{node_lay, 0,                3, OPENED,     POINTS, sch_g_ffarrow},
//	{node_lay, 0,                4, OPENED,     POINTS, sch_g_ffp}};
//static TECH_POLYGON sch_ffn_l[] = {					/* layers */
//	{node_lay, 0,                4, CLOSEDRECT, BOX,    sch_g_ffbox},
//	{text_lay, TXTSETQLAMBDA(4), 4, TEXTBOX,    POINTS, (INTBIG *)sch_g_fftextd},
//	{text_lay, TXTSETQLAMBDA(4), 4, TEXTBOX,    POINTS, (INTBIG *)sch_g_fftexte},
//	{text_lay, TXTSETQLAMBDA(4), 4, TEXTBOX,    POINTS, (INTBIG *)sch_g_fftextq},
//	{text_lay, TXTSETQLAMBDA(4), 4, TEXTBOX,    POINTS, (INTBIG *)sch_g_fftextqb},
//	{text_lay, TXTSETQLAMBDA(4), 4, TEXTBOX,    POINTS, (INTBIG *)sch_g_fftextpr},
//	{text_lay, TXTSETQLAMBDA(4), 4, TEXTBOX,    POINTS, (INTBIG *)sch_g_fftextclr},
//	{node_lay, 0,                3, OPENED,     POINTS, sch_g_ffarrow},
//	{node_lay, 0,                4, OPENED,     POINTS, sch_g_ffn}};
//static TECH_POLYGON sch_ffms_l[] = {				/* layers */
//	{node_lay, 0,                4, CLOSEDRECT, BOX,    sch_g_ffbox},
//	{text_lay, TXTSETQLAMBDA(4), 4, TEXTBOX,    POINTS, (INTBIG *)sch_g_fftextd},
//	{text_lay, TXTSETQLAMBDA(4), 4, TEXTBOX,    POINTS, (INTBIG *)sch_g_fftexte},
//	{text_lay, TXTSETQLAMBDA(4), 4, TEXTBOX,    POINTS, (INTBIG *)sch_g_fftextq},
//	{text_lay, TXTSETQLAMBDA(4), 4, TEXTBOX,    POINTS, (INTBIG *)sch_g_fftextqb},
//	{text_lay, TXTSETQLAMBDA(4), 4, TEXTBOX,    POINTS, (INTBIG *)sch_g_fftextpr},
//	{text_lay, TXTSETQLAMBDA(4), 4, TEXTBOX,    POINTS, (INTBIG *)sch_g_fftextclr},
//	{node_lay, 0,                3, OPENED,     POINTS, sch_g_ffarrow},
//	{node_lay, 0,                6, OPENED,     POINTS, sch_g_ffms}};
//static TECH_NODES sch_ff = {
//	x_("Flip-Flop"), NFF, NONODEPROTO,				/* name */
//	K6,K10,											/* size */
//	7,sch_ff_p,										/* ports */
//	9,sch_ffp_l,									/* layers */
//	(NPFLIPFLOP<<NFUNCTIONSH),						/* userbits */
//	0,0,0,0,0,0,0,0,0};								/* characteristics */
//
///* general MUX */
//static TECH_PORTS sch_mux_p[] = {					/* ports */
//	{new ArcProto[] {wire_arc, bus_arc}, x_("a"), NOPORTPROTO, (180<<PORTANGLESH)|(0<<PORTARANGESH)|
//		(0<<PORTNETSH)|INPORT|PORTISOLATED, LEFTBYP8,BOTEDGE,LEFTBYP8,TOPEDGE},
//	{ new ArcProto[] {bus_arc}, x_("s"), NOPORTPROTO, (270<<PORTANGLESH)|(0<<PORTARANGESH)|
//		(2<<PORTNETSH)|INPORT, CENTER, BOTBYP875, CENTER, BOTBYP875},
//	{new ArcProto[] {wire_arc, bus_arc}, x_("y"), NOPORTPROTO, (0<<PORTANGLESH)|(0<<PORTARANGESH)|
//		(1<<PORTNETSH)|OUTPORT, RIGHTBYP8, CENTER, RIGHTBYP8, CENTER}};
//	static TECH_POLYGON sch_mux_l[] = {					/* layers */
//	{node_lay, 0, 4, CLOSED,     POINTS, sch_g_mux}};
//static TECH_NODES sch_mux = {
//	x_("Mux"), NMUX, NONODEPROTO,					/* name */
//	K8,K10,											/* size */
//	3,sch_mux_p,									/* ports */
//	1,sch_mux_l,									/* layers */
//	(NPMUX<<NFUNCTIONSH),							/* userbits */
//	0,0,0,0,0,0,0,0,0};								/* characteristics */
//
///* black box */
//static TECH_PORTS sch_bbox_p[] = {					/* ports */
//	{new ArcProto[] {wire_arc, bus_arc}, x_("a"), NOPORTPROTO, (0<<PORTANGLESH)|(45<<PORTARANGESH)|
//		(0<<PORTNETSH)|PORTISOLATED, RIGHTEDGE, BOTEDGE, RIGHTEDGE, TOPEDGE},
//	{new ArcProto[] {wire_arc, bus_arc}, x_("b"), NOPORTPROTO, (90<<PORTANGLESH)|(45<<PORTARANGESH)|
//		(1<<PORTNETSH)|PORTISOLATED, LEFTEDGE,  TOPEDGE, RIGHTEDGE, TOPEDGE},
//	{new ArcProto[] {wire_arc, bus_arc}, x_("c"), NOPORTPROTO, (180<<PORTANGLESH)|(45<<PORTARANGESH)|
//		(2<<PORTNETSH)|PORTISOLATED, LEFTEDGE,  BOTEDGE, LEFTEDGE,  TOPEDGE},
//	{new ArcProto[] {wire_arc, bus_arc}, x_("d"), NOPORTPROTO, (270<<PORTANGLESH)|(45<<PORTARANGESH)|
//		(3<<PORTNETSH)|PORTISOLATED, LEFTEDGE,  BOTEDGE, RIGHTEDGE, BOTEDGE}};
//static TECH_POLYGON sch_bbox_l[] = {				/* layers */
//	{node_lay, 0,         4, CLOSEDRECT, BOX,    sch_g_bbox}};
//static TECH_NODES sch_bbox = {
//	x_("Bbox"), NBBOX, NONODEPROTO,					/* name */
//	K10,K10,										/* size */
//	4,sch_bbox_p,									/* ports */
//	1,sch_bbox_l,									/* layers */
//	(NPUNKNOWN<<NFUNCTIONSH),						/* userbits */
//	0,0,0,0,0,0,0,0,0};								/* characteristics */
//
///* switch */
//static TECH_PORTS sch_switch_p[] = {				/* ports */
//	{new ArcProto[] {wire_arc, bus_arc}, x_("a"), NOPORTPROTO, (180<<PORTANGLESH)|(90<<PORTARANGESH)|
//		(0<<PORTNETSH)|PORTISOLATED, LEFTIN1, BOTIN1, LEFTIN1, TOPIN1},
//	{new ArcProto[] {wire_arc, bus_arc}, x_("y"), NOPORTPROTO, (0<<PORTANGLESH)|(90<<PORTARANGESH)|
//		(1<<PORTNETSH), RIGHTIN1, CENTER, RIGHTIN1, CENTER}};
//static TECH_POLYGON sch_switch_l[] = {				/* layers */
//	{node_lay, 0, 2, DISC,       POINTS, sch_g_switchin},
//	{node_lay, 0, 2, OPENED,     POINTS, sch_g_switchbar},
//	{node_lay, 0, 2, DISC,       POINTS, sch_g_switchout}};
//static TECH_NODES sch_switch = {
//	x_("Switch"),NSWITCH,NONODEPROTO,				/* name */
//	K6,K2,											/* size */
//	2,sch_switch_p,									/* ports */
//	3,sch_switch_l,									/* layers */
//	(NPUNKNOWN<<NFUNCTIONSH),						/* userbits */
//	0,0,0,0,0,0,0,0,0};								/* characteristics */
//
///* off page connector */
//static TECH_PORTS sch_offpage_p[] = {				/* ports */
//	{new ArcProto[] {wire_arc, bus_arc}, x_("a"), NOPORTPROTO, (180<<PORTANGLESH)|(45<<PORTARANGESH),
//		LEFTEDGE,  CENTER, LEFTEDGE,  CENTER},
//	{new ArcProto[] {wire_arc, bus_arc}, x_("y"), NOPORTPROTO, (0<<PORTANGLESH)|(45<<PORTARANGESH),
//		RIGHTEDGE, CENTER, RIGHTEDGE, CENTER}};
//static TECH_POLYGON sch_offpage_l[] = {				/* layers */
//	{node_lay, 0,         5, CLOSED,   POINTS, sch_g_offpage}};
//static TECH_NODES sch_offpage = {
//	x_("Off-Page"), NOFFPAGE, NONODEPROTO,			/* name */
//	K4,K2,											/* size */
//	2,sch_offpage_p,								/* ports */
//	1,sch_offpage_l,								/* layers */
//	(NPCONNECT<<NFUNCTIONSH),						/* userbits */
//	0,0,0,0,0,0,0,0,0};								/* characteristics */
//
///* power */
//static TECH_PORTS sch_pwr_p[] = {					/* ports */
//	{ new ArcProto[] {wire_arc}, x_("pwr"), NOPORTPROTO, (180<<PORTARANGESH)|PWRPORT,
//		CENTER, CENTER, CENTER, CENTER}};
//static TECH_POLYGON sch_pwr_l[] = {					/* layers */
//	{node_lay, 0,        2, CIRCLE,   POINTS, sch_g_pwr1},
//	{node_lay, 0,        2, CIRCLE,   POINTS, sch_g_pwr2}};
//static TECH_NODES sch_pwr = {
//	x_("Power"), NPWR, NONODEPROTO,					/* name */
//	K3,K3,											/* size */
//	1,sch_pwr_p,									/* ports */
//	2,sch_pwr_l,									/* layers */
//	(NPCONPOWER<<NFUNCTIONSH)|NSQUARE,				/* userbits */
//	0,0,0,0,0,0,0,0,0};								/* characteristics */
//
///* ground */
//static TECH_PORTS sch_gnd_p[] = {					/* ports */
//	{ new ArcProto[] {wire_arc}, x_("gnd"), NOPORTPROTO, (90<<PORTANGLESH)|(90<<PORTARANGESH)|
//		GNDPORT, CENTER, TOPEDGE, CENTER, TOPEDGE}};
//static TECH_POLYGON sch_gnd_l[] = {					/* layers */
//	{node_lay, 0, 12, VECTORS, POINTS, sch_g_gnd}};
//static TECH_NODES sch_gnd = {
//	x_("Ground"), NGND, NONODEPROTO,				/* name */
//	K3,K4,											/* size */
//	1,sch_gnd_p,									/* ports */
//	1,sch_gnd_l,									/* layers */
//	(NPCONGROUND<<NFUNCTIONSH),						/* userbits */
//	0,0,0,0,0,0,0,0,0};								/* characteristics */
//
///* source */
//static TECH_PORTS sch_source_p[] = {				/* ports */
//	{ new ArcProto[] {wire_arc}, x_("plus"),   NOPORTPROTO, (90<<PORTANGLESH)|(0<<PORTARANGESH)|
//		(0<<PORTNETSH), CENTER, TOPEDGE, CENTER, TOPEDGE},
//	{ new ArcProto[] {wire_arc}, x_("minus"),  NOPORTPROTO, (270<<PORTANGLESH)|(0<<PORTARANGESH)|
//		(1<<PORTNETSH), CENTER, BOTEDGE, CENTER, BOTEDGE}};
//static TECH_POLYGON sch_sourcev_l[] = {				/* layers */
//	{node_lay, 0,                2, CIRCLE,   POINTS, sch_g_source},
//	{node_lay, 0,                4, VECTORS,  POINTS, sch_g_sourcepl}};
//static TECH_NODES sch_source = {
//	x_("Source"), NSOURCE, NONODEPROTO,				/* name */
//	K6,K6,											/* size */
//	2,sch_source_p,									/* ports */
//	2,sch_sourcev_l,								/* layers */
//	(NPSOURCE<<NFUNCTIONSH)|NSQUARE,				/* userbits */
//	0,0,0,0,0,0,0,0,0};								/* characteristics */
//
///* transistor */
//static TECH_PORTS sch_trans_p[] = {					/* ports */
//	{ new ArcProto[] {wire_arc}, x_("g"), NOPORTPROTO, (180<<PORTARANGESH)|
//		(0<<PORTNETSH)|INPORT, CENTER, TOPIN1, CENTER, TOPIN1},
//	{ new ArcProto[] {wire_arc}, x_("s"), NOPORTPROTO, (180<<PORTANGLESH)|(90<<PORTARANGESH)|
//		(1<<PORTNETSH)|BIDIRPORT, LEFTEDGE,  BOTEDGE,  LEFTEDGE,  BOTEDGE},
//	{ new ArcProto[] {wire_arc}, x_("d"), NOPORTPROTO, (0<<PORTANGLESH)|(90<<PORTARANGESH)|
//		(2<<PORTNETSH)|BIDIRPORT, RIGHTEDGE, BOTEDGE,  RIGHTEDGE, BOTEDGE}};
//static TECH_POLYGON sch_nmos_l[] = {				/* layers */
//	{node_lay, 0,         6, OPENED,     POINTS, sch_g_mos},
//	{node_lay, 0,         2, OPENED,     POINTS, sch_g_trantop},
//	{node_lay, 0,         2, OPENED,     POINTS, sch_g_nmos}};
//static TECH_POLYGON sch_pmos_l[] = {				/* layers */
//	{node_lay, 0,         6, OPENED,     POINTS, sch_g_mos},
//	{node_lay, 0,         2, OPENED,     POINTS, sch_g_trantop},
//	{node_lay, 0,         2, OPENED,     POINTS, sch_g_pmos},
//	{node_lay, 0,         2, CIRCLE,     POINTS, sch_g_pmoscir}};
//static TECH_POLYGON sch_dmos_l[] = {				/* layers */
//	{node_lay, 0,         6, OPENED,     POINTS, sch_g_mos},
//	{node_lay, 0,         2, OPENED,     POINTS, sch_g_trantop},
//	{node_lay, 0,         2, OPENED,     POINTS, sch_g_nmos},
//	{node_lay, 0,         4, FILLEDRECT, BOX,    sch_g_dmos}};
//static TECH_POLYGON sch_npn_l[] = {					/* layers */
//	{node_lay, 0,         6, OPENED,     POINTS, sch_g_btran1},
//	{node_lay, 0,         2, OPENED,     POINTS, sch_g_trantop},
//	{node_lay, 0,         2, OPENED,     POINTS, sch_g_nmos},
//	{node_lay, 0,         3, OPENED,     POINTS, sch_g_btran2}};
//static TECH_POLYGON sch_pnp_l[] = {					/* layers */
//	{node_lay, 0,         6, OPENED,     POINTS, sch_g_btran1},
//	{node_lay, 0,         2, OPENED,     POINTS, sch_g_trantop},
//	{node_lay, 0,         2, OPENED,     POINTS, sch_g_nmos},
//	{node_lay, 0,         3, OPENED,     POINTS, sch_g_btran3}};
//static TECH_POLYGON sch_njfet_l[] = {				/* layers */
//	{node_lay, 0,        10, VECTORS,    POINTS, sch_g_btran4},
//	{node_lay, 0,         2, OPENED,     POINTS, sch_g_trantop},
//	{node_lay, 0,         2, OPENED,     POINTS, sch_g_nmos},
//	{node_lay, 0,         3, OPENED,     POINTS, sch_g_btran5}};
//static TECH_POLYGON sch_pjfet_l[] = {				/* layers */
//	{node_lay, 0,        10, VECTORS,    POINTS, sch_g_btran4},
//	{node_lay, 0,         2, OPENED,     POINTS, sch_g_trantop},
//	{node_lay, 0,         2, OPENED,     POINTS, sch_g_nmos},
//	{node_lay, 0,         3, OPENED,     POINTS, sch_g_btran6}};
//static TECH_POLYGON sch_dmes_l[] = {				/* layers */
//	{node_lay, 0,        10, VECTORS,    POINTS, sch_g_btran4},
//	{node_lay, 0,         2, OPENED,     POINTS, sch_g_trantop},
//	{node_lay, 0,         2, OPENED,     POINTS, sch_g_nmos}};
//static TECH_POLYGON sch_emes_l[] = {				/* layers */
//	{node_lay, 0,        14, VECTORS,    POINTS, sch_g_btran7},
//	{node_lay, 0,         2, OPENED,     POINTS, sch_g_nmos}};
//static TECH_NODES sch_trans = {
//	x_("Transistor"), NTRANSISTOR, NONODEPROTO,		/* name */
//	K4,K4,											/* size */
//	3,sch_trans_p,									/* ports */
//	3,sch_nmos_l,									/* layers */
//	(NPTRANS<<NFUNCTIONSH),							/* userbits */
//	0,0,0,0,0,0,0,0,0};								/* characteristics */
//
///* resistor */
//static TECH_PORTS sch_resist_p[] = {				/* ports */
//	{ new ArcProto[] {wire_arc}, x_("a"), NOPORTPROTO, (180<<PORTANGLESH)|(90<<PORTARANGESH)|
//		(0<<PORTNETSH), LEFTBYP66,  CENTER, LEFTBYP66,  CENTER},
//	{ new ArcProto[] {wire_arc}, x_("b"), NOPORTPROTO, (0<<PORTANGLESH)|(90<<PORTARANGESH)|
//		(1<<PORTNETSH), RIGHTBYP66, CENTER, RIGHTBYP66, CENTER}};
//	static TECH_POLYGON sch_resist_l[] = {			/* layers */
//	{node_lay, 0,        10, OPENED,   POINTS, sch_g_resist}};
//static TECH_NODES sch_resist = {
//	x_("Resistor"), NRESISTOR, NONODEPROTO,			/* name */
//	K6,K1,											/* size */
//	2,sch_resist_p,									/* ports */
//	1,sch_resist_l,									/* layers */
//	(NPRESIST<<NFUNCTIONSH),						/* userbits */
//	0,0,0,0,0,0,0,0,0};								/* characteristics */
//
///* capacitor */
//static TECH_PORTS sch_capac_p[] = {					/* ports */
//	{ new ArcProto[] {wire_arc}, x_("a"), NOPORTPROTO, (90<<PORTANGLESH)|(90<<PORTARANGESH)|
//		(0<<PORTNETSH), CENTER, TOPEDGE, CENTER, TOPEDGE},
//	{ new ArcProto[] {wire_arc}, x_("b"), NOPORTPROTO, (270<<PORTANGLESH)|(90<<PORTARANGESH)|
//		(1<<PORTNETSH), CENTER, BOTEDGE,  CENTER, BOTEDGE}};
//static TECH_POLYGON sch_capac_l[] = {				/* layers */
//	{node_lay, 0,         8, VECTORS,  POINTS, sch_g_capac},
//	{node_lay, 0,         4, VECTORS,  POINTS, sch_g_capace}};
//static TECH_NODES sch_capac = {
//	x_("Capacitor"), NCAPACITOR, NONODEPROTO,		/* name */
//	K3,K4,											/* size */
//	2,sch_capac_p,									/* ports */
//	1,sch_capac_l,									/* layers */
//	(NPCAPAC<<NFUNCTIONSH),							/* userbits */
//	0,0,0,0,0,0,0,0,0};								/* characteristics */
//
///* diode */
//static TECH_PORTS sch_diode_p[] = {					/* ports */
//	{ new ArcProto[] {wire_arc}, x_("a"), NOPORTPROTO, (90<<PORTANGLESH)|(90<<PORTARANGESH)|
//		(0<<PORTNETSH), CENTER, TOPEDGE, CENTER, TOPEDGE},
//	{ new ArcProto[] {wire_arc}, x_("b"), NOPORTPROTO, (270<<PORTANGLESH)|(90<<PORTARANGESH)|
//		(1<<PORTNETSH), CENTER, BOTEDGE,  CENTER, BOTEDGE}};
//static TECH_POLYGON sch_diode_l[] = {				/* layers */
//	{node_lay, 0,         6, VECTORS,  POINTS, sch_g_diode1},
//	{node_lay, 0,         3, FILLED,   POINTS, sch_g_diode2}};
//static TECH_NODES sch_diode = {
//	x_("Diode"), NDIODE, NONODEPROTO,				/* name */
//	K2,K4,											/* size */
//	2,sch_diode_p,									/* ports */
//	2,sch_diode_l,									/* layers */
//	(NPDIODE<<NFUNCTIONSH),							/* userbits */
//	0,0,0,0,0,0,0,0,0};								/* characteristics */
//
///* inductor */
//static TECH_PORTS sch_induct_p[] = {				/* ports */
//	{ new ArcProto[] {wire_arc}, x_("a"), NOPORTPROTO, (90<<PORTANGLESH)|(90<<PORTARANGESH)|
//		(0<<PORTNETSH), CENTER, TOPEDGE, CENTER, TOPEDGE},
//	{ new ArcProto[] {wire_arc}, x_("b"), NOPORTPROTO, (270<<PORTANGLESH)|(90<<PORTARANGESH)|
//		(1<<PORTNETSH), CENTER, BOTEDGE,  CENTER, BOTEDGE}};
//static TECH_POLYGON sch_induct_l[] = {				/* layers */
//	{node_lay, 0,         2, OPENED,   POINTS, sch_g_induct1},
//	{node_lay, 0,         2, CIRCLE,   POINTS, sch_g_induct2},
//	{node_lay, 0,         2, CIRCLE,   POINTS, sch_g_induct3},
//	{node_lay, 0,         2, CIRCLE,   POINTS, sch_g_induct4}};
//static TECH_NODES sch_induct = {
//	x_("Inductor"), NINDUCTOR, NONODEPROTO,			/* name */
//	K2,K4,											/* size */
//	2,sch_induct_p,									/* ports */
//	4,sch_induct_l,									/* layers */
//	(NPINDUCT<<NFUNCTIONSH),						/* userbits */
//	0,0,0,0,0,0,0,0,0};								/* characteristics */
//
///* meter */
//static TECH_PORTS sch_meter_p[] = {					/* ports */
//	{ new ArcProto[] {wire_arc}, x_("a"),  NOPORTPROTO, (90<<PORTANGLESH)|(0<<PORTARANGESH)|
//		(0<<PORTNETSH), CENTER, TOPEDGE, CENTER, TOPEDGE},
//	{ new ArcProto[] {wire_arc}, x_("b"),  NOPORTPROTO, (270<<PORTANGLESH)|(0<<PORTARANGESH)|
//		(1<<PORTNETSH), CENTER, BOTEDGE, CENTER, BOTEDGE}};
//static TECH_POLYGON sch_meterv_l[] = {				/* layers */
//	{node_lay, 0,                2, CIRCLE,  POINTS, sch_g_meter},
//	{text_lay, TXTSETQLAMBDA(8), 4, TEXTBOX, BOX,    (INTBIG *)sch_g_meterv}};
//static TECH_NODES sch_meter = {
//	x_("Meter"), NMETER, NONODEPROTO,				/* name */
//	K6,K6,											/* size */
//	2,sch_meter_p,									/* ports */
//	2,sch_meterv_l,									/* layers */
//	(NPMETER<<NFUNCTIONSH)|NSQUARE,					/* userbits */
//	0,0,0,0,0,0,0,0,0};								/* characteristics */
//
///* well contact */
//static TECH_PORTS sch_well_p[] = {				/* ports */
//	{ new ArcProto[] {wire_arc}, x_("well"), NOPORTPROTO, (90<<PORTANGLESH)|(90<<PORTARANGESH),
//		CENTER, TOPEDGE, CENTER, TOPEDGE}};
//static TECH_POLYGON sch_well_l[] = {				/* layers */
//	{node_lay, 0, 4, VECTORS, POINTS, sch_g_well}};
//static TECH_NODES sch_well = {
//	x_("Well"), NWELL, NONODEPROTO,					/* name */
//	K4,K2,											/* size */
//	1,sch_well_p,									/* ports */
//	1,sch_well_l,									/* layers */
//	(NPWELL<<NFUNCTIONSH),							/* userbits */
//	0,0,0,0,0,0,0,0,0};								/* characteristics */
//
///* substrate contact */
//static TECH_PORTS sch_substrate_p[] = {				/* ports */
//	{ new ArcProto[] {wire_arc}, x_("substrate"), NOPORTPROTO, (90<<PORTANGLESH)|(90<<PORTARANGESH),
//		CENTER, TOPEDGE, CENTER, TOPEDGE}};
//static TECH_POLYGON sch_substrate_l[] = {			/* layers */
//	{node_lay, 0, 8, VECTORS, POINTS, sch_g_substrate}};
//static TECH_NODES sch_substrate = {
//	x_("Substrate"), NSUBSTRATE, NONODEPROTO,		/* name */
//	K3,K3,											/* size */
//	1,sch_substrate_p,								/* ports */
//	1,sch_substrate_l,								/* layers */
//	(NPSUBSTRATE<<NFUNCTIONSH),						/* userbits */
//	0,0,0,0,0,0,0,0,0};								/* characteristics */
//
///* two-port */
//static TECH_PORTS sch_twoport_p[] = {				/* ports */
//	{ new ArcProto[] {wire_arc}, x_("a"), NOPORTPROTO, (180<<PORTANGLESH)|(90<<PORTARANGESH)|
//		(0<<PORTNETSH), LEFTEDGE, TOPBYP66, LEFTEDGE, TOPBYP66},
//	{ new ArcProto[] {wire_arc}, x_("b"), NOPORTPROTO, (180<<PORTANGLESH)|(90<<PORTARANGESH)|
//		(1<<PORTNETSH), LEFTEDGE, BOTBYP66, LEFTEDGE, BOTBYP66},
//	{ new ArcProto[] {wire_arc}, x_("x"), NOPORTPROTO, (0<<PORTANGLESH)|(90<<PORTARANGESH)|
//		(2<<PORTNETSH), RIGHTEDGE, TOPBYP66, RIGHTEDGE, TOPBYP66},
//	{ new ArcProto[] {wire_arc}, x_("y"), NOPORTPROTO, (0<<PORTANGLESH)|(90<<PORTARANGESH)|
//		(3<<PORTNETSH), RIGHTEDGE, BOTBYP66, RIGHTEDGE, BOTBYP66}};
//static TECH_POLYGON sch_twoportg_l[] = {			/* layers */
//	{node_lay, 0,         4, CLOSEDRECT, BOX,  sch_g_twobox},
//	{node_lay, 0,         8, VECTORS,  POINTS, sch_g_twogwire},
//	{node_lay, 0,         4, VECTORS,  POINTS, sch_g_twoulpl},
//	{node_lay, 0,         4, VECTORS,  POINTS, sch_g_twourpl}};
//static TECH_POLYGON sch_twoportvcvs_l[] = {			/* layers */
//	{node_lay, 0,         4, CLOSEDRECT, BOX,  sch_g_twobox},
//	{node_lay, 0,        12, VECTORS,  POINTS, sch_g_twonormwire},
//	{node_lay, 0,         2, CIRCLE,   POINTS, sch_g_twovsc},
//	{node_lay, 0,         4, VECTORS,  POINTS, sch_g_twourpl},
//	{node_lay, 0,         4, VECTORS,  POINTS, sch_g_twoulpl}};
//static TECH_POLYGON sch_twoportvccs_l[] = {			/* layers */
//	{node_lay, 0,         4, CLOSEDRECT, BOX,  sch_g_twobox},
//	{node_lay, 0,        12, VECTORS,  POINTS, sch_g_twonormwire},
//	{node_lay, 0,         8, VECTORS,  POINTS, sch_g_twocswire},
//	{node_lay, 0,         6, VECTORS,  POINTS, sch_g_twocsarr},
//	{node_lay, 0,         4, VECTORS,  POINTS, sch_g_twoulpl}};
//static TECH_POLYGON sch_twoportccvs_l[] = {			/* layers */
//	{node_lay, 0,         4, CLOSEDRECT, BOX,  sch_g_twobox},
//	{node_lay, 0,         2, /*VECTORS*/OPENEDT1,  POINTS, sch_g_twoccwire},
//	{node_lay, 0,        12, VECTORS,  POINTS, sch_g_twonormwire},
//	{node_lay, 0,         2, CIRCLE,   POINTS, sch_g_twovsc},
//	{node_lay, 0,         4, VECTORS,  POINTS, sch_g_twourpl},
//	{node_lay, 0,         4, VECTORS,  POINTS, sch_g_twoulpl}};
//static TECH_POLYGON sch_twoportcccs_l[] = {			/* layers */
//	{node_lay, 0,         4, CLOSEDRECT, BOX,  sch_g_twobox},
//	{node_lay, 0,         2, /*VECTORS*/OPENEDT1,  POINTS, sch_g_twoccwire},
//	{node_lay, 0,        12, VECTORS,  POINTS, sch_g_twonormwire},
//	{node_lay, 0,         8, VECTORS,  POINTS, sch_g_twocswire},
//	{node_lay, 0,         6, VECTORS,  POINTS, sch_g_twocsarr},
//	{node_lay, 0,         4, VECTORS,  POINTS, sch_g_twoulpl}};
//static TECH_POLYGON sch_twoporttran_l[] = {			/* layers */
//	{node_lay, 0,         4, VECTORS,  POINTS, sch_g_twotrbox},
//	{node_lay, 0,         3, CIRCLEARC,POINTS, sch_g_twotr1},
//	{node_lay, 0,         3, CIRCLEARC,POINTS, sch_g_twotr2},
//	{node_lay, 0,         3, CIRCLEARC,POINTS, sch_g_twotr3},
//	{node_lay, 0,         8, VECTORS,  POINTS, sch_g_twotrwire},
//	{node_lay, 0,         4, VECTORS,  POINTS, sch_g_twoulpl},
//	{node_lay, 0,         4, VECTORS,  POINTS, sch_g_twourrpl}};
//static TECH_NODES sch_twoport = {
//	x_("Two-Port"), NTWOPORT, NONODEPROTO,			/* name */
//	K10,K6,											/* size */
//	4,sch_twoport_p,								/* ports */
//	4,sch_twoportg_l,								/* layers */
//	(NPTLINE<<NFUNCTIONSH),							/* userbits */
//	0,0,0,0,0,0,0,0,0};								/* characteristics */
//
///* 4-port transistor */
//static TECH_PORTS sch_trans4_p[] = {				/* ports */
//	{ new ArcProto[] {wire_arc}, x_("g"), NOPORTPROTO, (180<<PORTARANGESH)|
//		(0<<PORTNETSH)|INPORT, CENTER, TOPIN1, CENTER, TOPIN1},
//	{ new ArcProto[] {wire_arc}, x_("s"), NOPORTPROTO, (180<<PORTANGLESH)|(90<<PORTARANGESH)|
//		(1<<PORTNETSH)|BIDIRPORT, LEFTEDGE,  BOTEDGE,  LEFTEDGE,  BOTEDGE},
//	{ new ArcProto[] {wire_arc}, x_("d"), NOPORTPROTO, (0<<PORTANGLESH)|(90<<PORTARANGESH)|
//		(2<<PORTNETSH)|BIDIRPORT, RIGHTEDGE, BOTEDGE,  RIGHTEDGE, BOTEDGE},
//	{ new ArcProto[] {wire_arc}, x_("b"), NOPORTPROTO, (270<<PORTANGLESH)|(90<<PORTARANGESH)|
//		(3<<PORTNETSH)|BIDIRPORT, LEFTBYP5, BOTEDGE,  LEFTBYP5, BOTEDGE}};
//static TECH_POLYGON sch_nmos4_l[] = {				/* layers */
//	{node_lay, 0,         6, OPENED,     POINTS, sch_g_mos},
//	{node_lay, 0,         2, OPENED,     POINTS, sch_g_trantop},
//	{node_lay, 0,         2, OPENED,     POINTS, sch_g_nmos},
//	{node_lay, 0,         6, VECTORS,    POINTS, sch_g_nmos4}};
//static TECH_POLYGON sch_pmos4_l[] = {				/* layers */
//	{node_lay, 0,         6, OPENED,     POINTS, sch_g_mos},
//	{node_lay, 0,         2, OPENED,     POINTS, sch_g_trantop},
//	{node_lay, 0,         2, OPENED,     POINTS, sch_g_pmos},
//	{node_lay, 0,         2, CIRCLE,     POINTS, sch_g_pmoscir},
//	{node_lay, 0,         6, VECTORS,    POINTS, sch_g_pmos4}};
//static TECH_POLYGON sch_dmos4_l[] = {				/* layers */
//	{node_lay, 0,         6, OPENED,     POINTS, sch_g_mos},
//	{node_lay, 0,         2, OPENED,     POINTS, sch_g_trantop},
//	{node_lay, 0,         2, OPENED,     POINTS, sch_g_nmos},
//	{node_lay, 0,         4, FILLEDRECT, BOX,    sch_g_dmos},
//	{node_lay, 0,         6, VECTORS,    POINTS, sch_g_dmos4}};
//static TECH_POLYGON sch_npn4_l[] = {				/* layers */
//	{node_lay, 0,         6, OPENED,     POINTS, sch_g_btran1},
//	{node_lay, 0,         2, OPENED,     POINTS, sch_g_trantop},
//	{node_lay, 0,         2, OPENED,     POINTS, sch_g_nmos},
//	{node_lay, 0,         3, OPENED,     POINTS, sch_g_btran2},
//	{node_lay, 0,         2, OPENED,     POINTS, sch_g_bip4}};
//static TECH_POLYGON sch_pnp4_l[] = {				/* layers */
//	{node_lay, 0,         6, OPENED,     POINTS, sch_g_btran1},
//	{node_lay, 0,         2, OPENED,     POINTS, sch_g_trantop},
//	{node_lay, 0,         2, OPENED,     POINTS, sch_g_nmos},
//	{node_lay, 0,         3, OPENED,     POINTS, sch_g_btran3},
//	{node_lay, 0,         2, OPENED,     POINTS, sch_g_bip4}};
//static TECH_POLYGON sch_njfet4_l[] = {				/* layers */
//	{node_lay, 0,        10, VECTORS,    POINTS, sch_g_btran4},
//	{node_lay, 0,         2, OPENED,     POINTS, sch_g_trantop},
//	{node_lay, 0,         2, OPENED,     POINTS, sch_g_nmos},
//	{node_lay, 0,         3, OPENED,     POINTS, sch_g_btran5},
//	{node_lay, 0,         6, VECTORS,    POINTS, sch_g_pmes4}};
//static TECH_POLYGON sch_pjfet4_l[] = {				/* layers */
//	{node_lay, 0,        10, VECTORS,    POINTS, sch_g_btran4},
//	{node_lay, 0,         2, OPENED,     POINTS, sch_g_trantop},
//	{node_lay, 0,         2, OPENED,     POINTS, sch_g_nmos},
//	{node_lay, 0,         3, OPENED,     POINTS, sch_g_btran6},
//	{node_lay, 0,         6, VECTORS,    POINTS, sch_g_nmes4}};
//static TECH_POLYGON sch_dmes4_l[] = {				/* layers */
//	{node_lay, 0,        10, VECTORS,    POINTS, sch_g_btran4},
//	{node_lay, 0,         2, OPENED,     POINTS, sch_g_trantop},
//	{node_lay, 0,         2, OPENED,     POINTS, sch_g_nmos},
//	{node_lay, 0,         6, VECTORS,    POINTS, sch_g_nmes4}};
//static TECH_POLYGON sch_emes4_l[] = {				/* layers */
//	{node_lay, 0,        14, VECTORS,    POINTS, sch_g_btran7},
//	{node_lay, 0,         2, OPENED,     POINTS, sch_g_nmos},
//	{node_lay, 0,         6, VECTORS,    POINTS, sch_g_nmes4}};
//static TECH_NODES sch_trans4 = {
//	x_("4-Port-Transistor"), NTRANSISTOR4, NONODEPROTO,	/* name */
//	K4,K4,											/* size */
//	4,sch_trans4_p,									/* ports */
//	3,sch_nmos4_l,									/* layers */
//	(NPTRANS4<<NFUNCTIONSH),						/* userbits */
//	0,0,0,0,0,0,0,0,0};								/* characteristics */
//
///* global signal */
//static TECH_PORTS sch_globalsig_p[] = {				/* ports */
//	{ new ArcProto[] {wire_arc}, x_("global"), NOPORTPROTO, (270<<PORTANGLESH)|(90<<PORTARANGESH),
//		CENTER, BOTEDGE, CENTER, BOTEDGE}};
//static TECH_POLYGON sch_globalsig_l[] = {			/* layers */
//	{node_lay, 0, 4, CLOSED, POINTS, sch_g_global1},
//	{node_lay, 0, 4, CLOSED, POINTS, sch_g_global2}};
//static TECH_NODES sch_globalsig = {
//	x_("Global-Signal"), NGLOBALSIG, NONODEPROTO,	/* name */
//	K3,K3,											/* size */
//	1,sch_globalsig_p,								/* ports */
//	2,sch_globalsig_l,								/* layers */
//	(NPCONNECT<<NFUNCTIONSH),						/* userbits */
//	0,0,0,0,0,0,0,0,0};								/* characteristics */

	}
	
//static INTBIG sch_node_widoff[NODEPROTOCOUNT*4] = {
//	  0, 0, 0, 0,    0, 0, 0, 0,    0, 0, 0, 0,					/* pins */
//	  0,K1, 0, 0,    0,H0, 0, 0,   K1,H0, 0, 0,    0,H0, 0, 0,	/* gates: buffer, and, or, xor */
//	  0, 0, 0, 0,    0, 0, 0, 0,								/* flipflop, mux */
//	  0, 0, 0, 0,    0, 0, 0, 0,								/* box/switch */
//	  0, 0, 0, 0,												/* offpage */
//	  0, 0, 0, 0,    0, 0, 0, 0,    0, 0, 0, 0,					/* pwr/gnd/source */
//	  0, 0, 0,K1,    0, 0, 0, 0,    0, 0, 0, 0,					/* trans/resist/capac */
//	  0, 0, 0, 0,    0, 0, 0, 0,								/* diode/inductor */
//	  0, 0, 0, 0,												/* meter */
//	  0, 0, 0, 0,    0, 0, 0, 0,								/* well/substrate */
//	  0, 0, 0, 0,    0, 0, 0,K1,    0, 0, 0, 0					/* twoport/4-port/global */
//};

//static CHAR *sch_node_vhdlstring[NODEPROTOCOUNT] = {
//	x_(""), x_(""), x_(""),										/* pins */
//	x_("buffer/inverter"), x_("and%ld/nand%ld"), x_("or%ld/nor%ld"), x_("xor%ld/xnor%ld"),	/* gates */
//	x_("ff"), x_("mux%ld"),										/* flipflop, mux */
//	x_(""), x_(""),												/* box/switch */
//	x_(""),														/* offpage */
//	x_(""), x_(""), x_(""),										/* pwr/gnd/source */
//	x_(""), x_(""), x_(""),										/* trans/resist/capac */
//	x_(""), x_(""),												/* diode/inductor */
//	x_(""),														/* meter */
//	x_(""), x_(""),												/* well/substrate */
//	x_(""), x_(""), x_("")										/* twoport/4-port/global */
//};

/******************** VARIABLE AGGREGATION ********************/

//TECH_VARIABLES sch_variables[] =
//{
//	/* set general information about the technology */
//	{x_("TECH_layer_names"), (CHAR *)sch_layer_names, 0.0,
//		VSTRING|VDONTSAVE|VISARRAY|(MAXLAYERS<<VLENGTHSH)},
//	{x_("TECH_layer_function"), (CHAR *)sch_layer_function, 0.0,
//		VINTEGER|VDONTSAVE|VISARRAY|(MAXLAYERS<<VLENGTHSH)},
//	{x_("TECH_node_width_offset"), (CHAR *)sch_node_widoff, 0.0,
//		VFRACT|VDONTSAVE|VISARRAY|((NODEPROTOCOUNT*4)<<VLENGTHSH)},
//	{x_("TECH_vhdl_names"), (CHAR *)sch_node_vhdlstring, 0.0,
//		VSTRING|VDONTSAVE|VISARRAY|(NODEPROTOCOUNT<<VLENGTHSH)},
//
//	/* set information for the USER analysis tool */
//	{x_("USER_layer_letters"), (CHAR *)sch_layer_letters, 0.0,
//		VSTRING|VDONTSAVE|VISARRAY|(MAXLAYERS<<VLENGTHSH)},
//	{NULL, NULL, 0.0, 0}
//};

/******************** ROUTINES ********************/
//
//BOOLEAN sch_initprocess(TECHNOLOGY *tech, INTBIG pass)
//{
//	switch (pass)
//	{
//		case 0:
//			/* initialize the technology variable */
//			sch_tech = tech;
//			break;
//
//		case 1:
//			/* cache pointers to the primitives */
//			sch_wirepinprim = getnodeproto(x_("schematic:Wire_Pin"));
//			sch_buspinprim = getnodeproto(x_("schematic:Bus_Pin"));
//			sch_wireconprim = getnodeproto(x_("schematic:Wire_Con"));
//			sch_bufprim = getnodeproto(x_("schematic:Buffer"));
//			sch_andprim = getnodeproto(x_("schematic:And"));
//			sch_orprim = getnodeproto(x_("schematic:Or"));
//			sch_xorprim = getnodeproto(x_("schematic:Xor"));
//			sch_ffprim = getnodeproto(x_("schematic:Flip-Flop"));
//			sch_muxprim = getnodeproto(x_("schematic:Mux"));
//			sch_bboxprim = getnodeproto(x_("schematic:Bbox"));
//			sch_switchprim = getnodeproto(x_("schematic:Switch"));
//			sch_offpageprim = getnodeproto(x_("schematic:Off-Page"));
//			sch_pwrprim = getnodeproto(x_("schematic:Power"));
//			sch_gndprim = getnodeproto(x_("schematic:Ground"));
//			sch_sourceprim = getnodeproto(x_("schematic:Source"));
//			sch_transistorprim = getnodeproto(x_("schematic:Transistor"));
//			sch_resistorprim = getnodeproto(x_("schematic:Resistor"));
//			sch_capacitorprim = getnodeproto(x_("schematic:Capacitor"));
//			sch_diodeprim = getnodeproto(x_("schematic:Diode"));
//			sch_inductorprim = getnodeproto(x_("schematic:Inductor"));
//			sch_meterprim = getnodeproto(x_("schematic:Meter"));
//			sch_wellprim = getnodeproto(x_("schematic:Well"));
//			sch_substrateprim = getnodeproto(x_("schematic:Substrate"));
//			sch_twoportprim = getnodeproto(x_("schematic:Two-Port"));
//			sch_transistor4prim = getnodeproto(x_("schematic:4-Port-Transistor"));
//			sch_globalprim = getnodeproto(x_("schematic:Global-Signal"));
//
//			sch_wirearc = getarcproto(x_("schematic:wire"));
//			sch_busarc = getarcproto(x_("schematic:bus"));
//
//			sch_meterkey = makekey(x_("SCHEM_meter_type"));
//			sch_diodekey = makekey(x_("SCHEM_diode"));
//			sch_capacitancekey = makekey(x_("SCHEM_capacitance"));
//			sch_resistancekey = makekey(x_("SCHEM_resistance"));
//			sch_inductancekey = makekey(x_("SCHEM_inductance"));
//			sch_functionkey = makekey(x_("SCHEM_function"));
//			sch_spicemodelkey = makekey(x_("SIM_spice_model"));
//			sch_globalnamekey = makekey(x_("SCHEM_global_name"));
//
//			/* differential ports are enabled */
//			sch_anddiffports = sch_ordiffports = sch_xordiffports = NOPORTPROTO;
//
//			/* translate strings on schematic nodes */
//			sch_g_fftextclr[16] = _("CLR");
//			sch_wirepinsizex = sch_wirepinprim->highx - sch_wirepinprim->lowx;
//			sch_wirepinsizey = sch_wirepinprim->highy - sch_wirepinprim->lowy;
//			break;
//
//		case 2:
//			/* set the default transistor placement to be rotated */
//			nextchangequiet();
//			setvalkey((INTBIG)sch_transistorprim, VNODEPROTO, us_placement_angle_key, 900, VINTEGER|VDONTSAVE);
//			nextchangequiet();
//			setvalkey((INTBIG)sch_transistor4prim, VNODEPROTO, us_placement_angle_key, 900, VINTEGER|VDONTSAVE);
//			break;
//	}
//	return(FALSE);
//}
//
//void sch_termprocess(void)
//{
//	/* put all ports into play so that deallocation will be complete */
//	if (sch_anddiffports == NOPORTPROTO) return;
//	sch_andprim->firstportproto->nextportproto->nextportproto = sch_anddiffports;
//	sch_orprim->firstportproto->nextportproto->nextportproto = sch_ordiffports;
//	sch_xorprim->firstportproto->nextportproto->nextportproto = sch_xordiffports;
//	sch_anddiffports = sch_ordiffports = sch_xordiffports = NOPORTPROTO;
//}
//
//void sch_setmode(INTBIG count, CHAR *par[])
//{
//	REGISTER CHAR *pp;
//	REGISTER INTBIG l;
//
//	if (count == 0)
//	{
//		/* report size of negating bubbles */
//		ttyputmsg(M_("Diameter of negating bubbles is %s"), frtoa(sch_bubblediameter));
//		return;
//	}
//
//	l = estrlen(pp = par[0]);
//	if (namesamen(pp, x_("negating-bubble-diameter"), l) == 0)
//	{
//		/* get new negating bubble diameter */
//		if (count <= 1)
//		{
//			ttyputmsg(M_("Diameter of negating bubbles is %s"), frtoa(sch_bubblediameter));
//			return;
//		}
//		l = atofr(par[1]);
//		if (l > 0) sch_bubblediameter = l; else
//			ttyputerr(M_("Bubble diameter must be positive and nonzero"));
//		return;
//	}
//	if (namesamen(pp, x_("disable-differential-ports"), l) == 0)
//	{
//		if (sch_anddiffports != NOPORTPROTO)
//		{
//			ttyputerr(M_("Differential ports are already disabled"));
//			return;
//		}
//		sch_anddiffports = sch_andprim->firstportproto->nextportproto->nextportproto;
//		sch_andprim->firstportproto->nextportproto->nextportproto = NOPORTPROTO;
//
//		sch_ordiffports = sch_orprim->firstportproto->nextportproto->nextportproto;
//		sch_orprim->firstportproto->nextportproto->nextportproto = NOPORTPROTO;
//
//		sch_xordiffports = sch_xorprim->firstportproto->nextportproto->nextportproto;
//		sch_xorprim->firstportproto->nextportproto->nextportproto = NOPORTPROTO;
//		net_redoprim();
//		return;
//	}
//	if (namesamen(pp, x_("enable-differential-ports"), l) == 0)
//	{
//		if (sch_anddiffports == NOPORTPROTO)
//		{
//			ttyputerr(M_("Differential ports are already enabled"));
//			return;
//		}
//		sch_andprim->firstportproto->nextportproto->nextportproto = sch_anddiffports;
//		sch_orprim->firstportproto->nextportproto->nextportproto = sch_ordiffports;
//		sch_xorprim->firstportproto->nextportproto->nextportproto = sch_xordiffports;
//		sch_anddiffports = sch_ordiffports = sch_xordiffports = NOPORTPROTO;
//		net_redoprim();
//		return;
//	}
//	ttyputbadusage(x_("technology tell schematic"));
//}
//
//INTBIG sch_request(CHAR *command, va_list ap)
//{
//	REGISTER PORTPROTO *pp;
//
//	if (namesame(command, x_("ignoring-resistor-topology")) == 0)
//	{
//		pp = sch_resistorprim->firstportproto->nextportproto;
//		if ((pp->userbits&PORTNET) == 0) return(1);
//		return(0);
//	}
//	if (namesame(command, x_("ignore-resistor-topology")) == 0)
//	{
//		pp = sch_resistorprim->firstportproto->nextportproto;
//		pp->userbits = (pp->userbits & ~PORTNET);
//		net_redoprim();
//		return(0);
//	}
//	if (namesame(command, x_("include-resistor-topology")) == 0)
//	{
//		pp = sch_resistorprim->firstportproto->nextportproto;
//		pp->userbits = (pp->userbits & ~PORTNET) | (1 << PORTNETSH);
//		net_redoprim();
//		return(0);
//	}
//
//	if (namesame(command, x_("get-bubble-size")) == 0)
//	{
//		return(sch_bubblediameter);
//	}
//	if (namesame(command, x_("set-bubble-size")) == 0)
//	{
//		sch_bubblediameter = va_arg(ap, INTBIG);
//		return(0);
//	}
//	return(0);
//}
//
//INTBIG sch_nodepolys(NODEINST *ni, INTBIG *reasonable, WINDOWPART *win)
//{
//	return(sch_intnodepolys(ni, reasonable, win, &tech_oneprocpolyloop, &sch_oneprocpolyloop));
//}
//
//INTBIG sch_intnodepolys(NODEINST *ni, INTBIG *reasonable, WINDOWPART *win, POLYLOOP *pl, SCHPOLYLOOP *schpl)
//{
//	REGISTER INTBIG total, pindex, buscon, nonbuscon, hei, arcs, i, implicitcon;
//	INTBIG depth;
//	NODEINST **nilist, *upni;
//	REGISTER PORTARCINST *pi;
//	REGISTER PORTEXPINST *pe;
//	REGISTER PORTPROTO *pp;
//
//	/* get the default number of polygons and list of layers */
//	pindex = ni->proto->primindex;
//	total = sch_nodeprotos[pindex-1]->layercount;
//	schpl->layerlist = sch_nodeprotos[pindex-1]->layerlist;
//
//	/* special cases for special primitives */
//	switch (pindex)
//	{
//		case NWIREPIN:	/* wire pins disappear with one or two wires */
//			if (tech_pinusecount(ni, win)) total = 0;
//			break;
//
//		case NBUSPIN:	/* bus pins get bigger in "T" configurations, disappear when alone and exported */
//			buscon = nonbuscon = 0;
//			for (pi = ni->firstportarcinst; pi != NOPORTARCINST; pi = pi->nextportarcinst)
//			{
//				if (pi->conarcinst->proto == sch_busarc) buscon++; else
//					nonbuscon++;
//			}
//			if (buscon == 0 && nonbuscon == 0) implicitcon = 1; else
//				implicitcon = 0;
//
//			/* if the next level up the hierarchy is visible, consider arcs connected there */
//			if (win != NOWINDOWPART && ni->firstportexpinst != NOPORTEXPINST)
//			{
//				db_gettraversalpath(ni->parent, NOWINDOWPART, &nilist, &depth);
//				if (depth == 1)
//				{
//					upni = nilist[0];
//					if (upni->proto == ni->parent && upni->parent == win->curnodeproto)
//					{
//						for(pe = ni->firstportexpinst; pe != NOPORTEXPINST; pe = pe->nextportexpinst)
//						{
//							for (pi = upni->firstportarcinst; pi != NOPORTARCINST; pi = pi->nextportarcinst)
//							{
//								if (pi->proto != pe->exportproto) continue;
//								if (pi->conarcinst->proto == sch_busarc) buscon++; else
//									nonbuscon++;
//							}
//						}
//					}
//				}
//			}
//
//			/* bus pins don't show wire pin in center if not tapped */
//			if (nonbuscon == 0) total--;
//
//			schpl->buspinlayer = bus_lay;
//			schpl->buspinpoints = sch_g_buspindisc;
//			if (buscon+implicitcon > 2)
//			{
//				/* larger pin because it is connected to 3 or more bus arcs */
//				schpl->buspinsize = H0;
//			} else
//			{
//				/* smaller pin because it has 0, 1, or 2 connections */
//				schpl->buspinsize = Q0;
//				if (buscon == 0)
//				{
//					if (nonbuscon+implicitcon > 2)
//					{
//						schpl->buspinlayer = arc_lay;
//						schpl->buspinpoints = sch_g_bustapdisc;
//						total--;
//					} else
//					{
//						if (ni->firstportexpinst != NOPORTEXPINST)
//							total = 0;
//					}
//				}
//			}
//			break;
//
//		case NFF:	/* determine graphics to use for FlipFlops */
//			switch (ni->userbits&FFCLOCK)
//			{
//				case FFCLOCKMS:		/* FlipFlop is Master/slave */
//					schpl->layerlist = sch_ffms_l;
//					break;
//				case FFCLOCKP:		/* FlipFlop is Positive clock */
//					schpl->layerlist = sch_ffp_l;
//					break;
//				case FFCLOCKN:		/* FlipFlop is Negative clock */
//					schpl->layerlist = sch_ffn_l;
//					break;
//			}
//			break;
//
//		case NSWITCH:	/* add in multiple connection sites for switch */
//			hei = (ni->highy - ni->lowy) / lambdaofnode(ni);
//			if (hei >= 4) total += (hei/2)-1;
//			if (((hei/2)&1) == 0) schpl->switchbarvalue = 0; else
//				schpl->switchbarvalue = K1;
//			break;
//
//		case NTRANSISTOR:	/* determine graphics to use for transistors */
//			switch (ni->userbits&NTECHBITS)
//			{
//				case TRANNMOS:				/* Transistor is N channel MOS */
//					schpl->layerlist = sch_nmos_l;
//					total = 3;
//					break;
//				case TRANDMOS:				/* Transistor is Depletion MOS */
//					schpl->layerlist = sch_dmos_l;
//					total = 4;
//					break;
//				case TRANPMOS:				/* Transistor is P channel MOS */
//					schpl->layerlist = sch_pmos_l;
//					total = 4;
//					break;
//				case TRANNPN:				/* Transistor is NPN Junction */
//					schpl->layerlist = sch_npn_l;
//					total = 4;
//					break;
//				case TRANPNP:				/* Transistor is PNP Junction */
//					schpl->layerlist = sch_pnp_l;
//					total = 4;
//					break;
//				case TRANNJFET:				/* Transistor is N Channel Junction FET */
//					schpl->layerlist = sch_njfet_l;
//					total = 4;
//					break;
//				case TRANPJFET:				/* Transistor is P Channel Junction FET */
//					schpl->layerlist = sch_pjfet_l;
//					total = 4;
//					break;
//				case TRANDMES:				/* Transistor is Depletion MESFET */
//					schpl->layerlist = sch_dmes_l;
//					total = 3;
//					break;
//				case TRANEMES:				/* Transistor is Enhancement MESFET */
//					schpl->layerlist = sch_emes_l;
//					total = 2;
//					break;
//			}
//			break;
//
//		case NCAPACITOR:	/* determine graphics to use for capacitors */
//			if ((ni->userbits&NTECHBITS) == CAPACELEC)
//				total++;
//			break;
//
//		case NTWOPORT:	/* determine graphics to use for Two-Ports */
//			switch (ni->userbits&NTECHBITS)
//			{
//				case TWOPVCCS:					/* Two-port is Transconductance (VCCS) */
//					schpl->layerlist = sch_twoportvccs_l;
//					total = 5;
//					break;
//				case TWOPCCVS:					/* Two-port is Transresistance (CCVS) */
//					schpl->layerlist = sch_twoportccvs_l;
//					total = 6;
//					break;
//				case TWOPVCVS:					/* Two-port is Voltage gain (VCVS) */
//					schpl->layerlist = sch_twoportvcvs_l;
//					total = 5;
//					break;
//				case TWOPCCCS:					/* Two-port is Current gain (CCCS) */
//					schpl->layerlist = sch_twoportcccs_l;
//					total = 6;
//					break;
//				case TWOPTLINE:					/* Two-port is Transmission Line */
//					schpl->layerlist = sch_twoporttran_l;
//					total = 7;
//					break;
//			}
//			break;
//		case NTRANSISTOR4:	/* determine graphics to use for 4-port transistors */
//			switch (ni->userbits&NTECHBITS)
//			{
//				case TRANNMOS:				/* Transistor is N channel MOS */
//					schpl->layerlist = sch_nmos4_l;
//					total = 4;
//					break;
//				case TRANDMOS:				/* Transistor is Depletion MOS */
//					schpl->layerlist = sch_dmos4_l;
//					total = 5;
//					break;
//				case TRANPMOS:				/* Transistor is P channel MOS */
//					schpl->layerlist = sch_pmos4_l;
//					total = 5;
//					break;
//				case TRANNPN:				/* Transistor is NPN Junction */
//					schpl->layerlist = sch_npn4_l;
//					total = 5;
//					break;
//				case TRANPNP:				/* Transistor is PNP Junction */
//					schpl->layerlist = sch_pnp4_l;
//					total = 5;
//					break;
//				case TRANNJFET:				/* Transistor is N Channel Junction FET */
//					schpl->layerlist = sch_njfet4_l;
//					total = 5;
//					break;
//				case TRANPJFET:				/* Transistor is P Channel Junction FET */
//					schpl->layerlist = sch_pjfet4_l;
//					total = 5;
//					break;
//				case TRANDMES:				/* Transistor is Depletion MESFET */
//					schpl->layerlist = sch_dmes4_l;
//					total = 4;
//					break;
//				case TRANEMES:				/* Transistor is Enhancement MESFET */
//					schpl->layerlist = sch_emes4_l;
//					total = 3;
//					break;
//			}
//			break;
//	}
//
//	schpl->extrasteinerpoint = total;
//	switch (pindex)
//	{
//		case NSWITCH:
//		case NOFFPAGE:
//		case NPWR:
//		case NGND:
//		case NSOURCE:
//		case NTRANSISTOR:
//		case NRESISTOR:
//		case NCAPACITOR:
//		case NDIODE:
//		case NINDUCTOR:
//		case NMETER:
//		case NWELL:
//		case NSUBSTRATE:
//		case NTWOPORT:
//		case NTRANSISTOR4:
//			for(i=0; i<sch_nodeprotos[pindex-1]->portcount; i++)
//			{
//				pp = sch_nodeprotos[pindex-1]->portlist[i].addr;
//				arcs = 0;
//				for(pi = ni->firstportarcinst; pi != NOPORTARCINST; pi = pi->nextportarcinst)
//					if (pi->proto == pp) arcs++;
//				if (arcs > 1)
//				{
//					schpl->extrasteinerport[total - schpl->extrasteinerpoint] = &sch_nodeprotos[pindex-1]->portlist[i];
//					total++;
//				}
//			}
//			break;
//	}
//
//	/* add in displayable variables */
//	pl->realpolys = total;
//	total += tech_displayablenvars(ni, pl->curwindowpart, pl);
//	if (reasonable != 0) *reasonable = total;
//	return(total);
//}
//
//void sch_shapenodepoly(NODEINST *ni, INTBIG box, POLYGON *poly)
//{
//	sch_intshapenodepoly(ni, box, poly, &tech_oneprocpolyloop, &sch_oneprocpolyloop);
//}
//
//void sch_intshapenodepoly(NODEINST *ni, INTBIG box, POLYGON *poly, POLYLOOP *pl, SCHPOLYLOOP *schpl)
//{
//	REGISTER INTBIG lambda, width, height;
//	REGISTER VARIABLE *var;
//	REGISTER TECH_PORTS *tp;
//	REGISTER TECH_POLYGON *lay;
//
//	/* handle displayable variables */
//	if (box >= pl->realpolys)
//	{
//		var = tech_filldisplayablenvar(ni, poly, pl->curwindowpart, 0, pl);
//		return;
//	}
//
//	/* get the unit size (lambda) */
//	switch (ni->proto->primindex)
//	{
//#ifdef SCALABLEGATES
//		case NAND:
//			width = ni->highx - ni->lowx;
//			height = ni->highy - ni->lowy;
//			lambda = width / 8;
//			if (height < lambda * 6) lambda = height / 6;
//			break;
//		case NOR:
//		case NXOR:
//			width = ni->highx - ni->lowx;
//			height = ni->highy - ni->lowy;
//			lambda = width / 10;
//			if (height < lambda * 6) lambda = height / 6;
//			break;
//#endif
//		default:
//			lambda = lambdaofnode(ni);
//			break;
//	}
//
//	if (box >= schpl->extrasteinerpoint)
//	{
//		/* handle extra steiner points */
//		tp = schpl->extrasteinerport[box - schpl->extrasteinerpoint];
//		if (poly->limit < 2) (void)extendpolygon(poly, 2);
//		poly->xv[0] = (getrange(ni->lowx, ni->highx, tp->lowxmul, tp->lowxsum, lambda) +
//			getrange(ni->lowx, ni->highx, tp->highxmul, tp->highxsum, lambda)) / 2;
//		poly->yv[0] = (getrange(ni->lowy, ni->highy, tp->lowymul, tp->lowysum, lambda) +
//			getrange(ni->lowy, ni->highy, tp->highymul, tp->highysum, lambda)) / 2;
//		poly->xv[1] = poly->xv[0] + sch_wirepinsizex/2;
//		poly->yv[1] = poly->yv[0];
//		poly->count = 2;
//		poly->style = DISC;
//		poly->layer = arc_lay;
//		poly->tech = sch_tech;
//		poly->desc = sch_layers[poly->layer];
//		return;
//	}
//
//	/* handle extra blobs on tall switches */
//	lay = &schpl->layerlist[box];
//	switch (ni->proto->primindex)
//	{
//		case NBUSPIN:
//			if (box == 0)
//			{
//				sch_buspin_l[0].layernum = (INTSML)schpl->buspinlayer;
//				sch_buspin_l[0].points = schpl->buspinpoints;
//				sch_g_buspindisc[4] = schpl->buspinsize;
//			}
//			break;
//		case NSWITCH:
//			if (box >= 2)
//			{
//				sch_g_switchout[3] = sch_g_switchout[7] = WHOLE*2*(box-1) - WHOLE;
//				box = 2;
//				lay = &schpl->layerlist[box];
//			}
//			if (lay->points == sch_g_switchbar)
//				sch_g_switchbar[7] = schpl->switchbarvalue;
//			break;
//		case NFF:
//			if (lay->points == (INTBIG *)sch_g_fftextd)
//			{
//				switch (ni->userbits&FFTYPE)
//				{
//					case FFTYPERS: sch_g_fftextd[16] = sch_R;        break;
//					case FFTYPEJK: sch_g_fftextd[16] = sch_J;        break;
//					case FFTYPED:  sch_g_fftextd[16] = sch_D;        break;
//					case FFTYPET:  sch_g_fftextd[16] = sch_T;        break;
//				}
//			} else if (lay->points == (INTBIG *)sch_g_fftexte)
//			{
//				switch (ni->userbits&FFTYPE)
//				{
//					case FFTYPERS: sch_g_fftexte[16] = sch_S;        break;
//					case FFTYPEJK: sch_g_fftexte[16] = sch_K;        break;
//					case FFTYPED:  sch_g_fftexte[16] = sch_E;        break;
//					case FFTYPET:  sch_g_fftexte[16] = sch_NULLSTR;  break;
//				}
//			}
//			break;
//		case NDIODE:	/* determine graphics to use for diodes */
//			if (box == 0)
//			{
//				switch (ni->userbits&NTECHBITS)
//				{
//					case DIODENORM:					/* Diode is normal */
//						lay->points = sch_g_diode1;
//						lay->count = 6;
//						break;
//					case DIODEZENER:				/* Diode is Zener */
//						lay->points = sch_g_diode3;
//						lay->count = 10;
//						break;
//				}
//			}
//			break;
//	}
//
//	tech_fillpoly(poly, lay, ni, lambda, FILLED);
//	TDCLEAR(poly->textdescript);
//	TDSETSIZE(poly->textdescript, lay->portnum);
//	poly->tech = sch_tech;
//	poly->desc = sch_layers[poly->layer];
//}
//
//INTBIG sch_allnodepolys(NODEINST *ni, POLYLIST *plist, WINDOWPART *win, BOOLEAN onlyreasonable)
//{
//	REGISTER INTBIG tot, j;
//	INTBIG reasonable;
//	REGISTER NODEPROTO *np;
//	REGISTER POLYGON *poly;
//	POLYLOOP mypl;
//	SCHPOLYLOOP myschpl;
//
//	np = ni->proto;
//	mypl.curwindowpart = win;
//	tot = sch_intnodepolys(ni, &reasonable, win, &mypl, &myschpl);
//	if (onlyreasonable) tot = reasonable;
//	if (mypl.realpolys < tot) tot = mypl.realpolys;
//	if (ensurepolylist(plist, tot, db_cluster)) return(-1);
//	for(j = 0; j < tot; j++)
//	{
//		poly = plist->polygons[j];
//		poly->tech = sch_tech;
//		sch_intshapenodepoly(ni, j, poly, &mypl, &myschpl);
//	}
//	return(tot);
//}
//
//INTBIG sch_nodeEpolys(NODEINST *ni, INTBIG *reasonable, WINDOWPART *win)
//{
//	Q_UNUSED( ni );
//	Q_UNUSED( win );
//
//	if (reasonable != 0) *reasonable = 0;
//	return(0);
//}
//
//void sch_shapeEnodepoly(NODEINST *ni, INTBIG box, POLYGON *poly)
//{
//	Q_UNUSED( ni );
//	Q_UNUSED( box );
//	Q_UNUSED( poly );
//}
//
//INTBIG sch_allnodeEpolys(NODEINST *ni, POLYLIST *plist, WINDOWPART *win, BOOLEAN onlyreasonable)
//{
//	Q_UNUSED( ni );
//	Q_UNUSED( plist );
//	Q_UNUSED( win );
//	Q_UNUSED( onlyreasonable );
//	return(0);
//}
//
//void sch_nodesizeoffset(NODEINST *ni, INTBIG *lx, INTBIG *ly, INTBIG *hx, INTBIG *hy)
//{
//	REGISTER INTBIG index, width, height, unitsize;
//
//	index = ni->proto->primindex;
//	switch (index)
//	{
//#ifdef SCALABLEGATES
//		case NAND:
//			width = ni->highx - ni->lowx;
//			height = ni->highy - ni->lowy;
//			unitsize = width / 8;
//			if (height < unitsize * 6) unitsize = height / 6;
//			*lx = 0;
//			*hx = unitsize/2;
//			*ly = *hy = 0;
//			break;
//		case NOR:
//			width = ni->highx - ni->lowx;
//			height = ni->highy - ni->lowy;
//			unitsize = width / 10;
//			if (height < unitsize * 6) unitsize = height / 6;
//			*lx = unitsize;
//			*hx = unitsize/2;
//			*ly = *hy = 0;
//			break;
//		case NXOR:
//			width = ni->highx - ni->lowx;
//			height = ni->highy - ni->lowy;
//			unitsize = width / 10;
//			if (height < unitsize * 6) unitsize = height / 6;
//			*lx = 0;
//			*hx = unitsize/2;
//			*ly = *hy = 0;
//			break;
//#endif
//		default:
//			tech_nodeprotosizeoffset(ni->proto, lx, ly, hx, hy, lambdaofnode(ni));
//			break;
//	}
//}
//
//void sch_shapeportpoly(NODEINST *ni, PORTPROTO *pp, POLYGON *poly, XARRAY trans,
//	BOOLEAN purpose)
//{
//	REGISTER INTBIG pindex, i, e, total, besti, xposition, yposition, x, y, lambda,
//		wantx, wanty, bestdist, bestx, besty, dist, width, height;
//	REGISTER PORTARCINST *pi;
//	REGISTER ARCINST *ai;
//	REGISTER WINDOWPART *w;
//
//	pindex = ni->proto->primindex;
//
//	switch (ni->proto->primindex)
//	{
//#ifdef SCALABLEGATES
//		case NAND:
//			width = ni->highx - ni->lowx;
//			height = ni->highy - ni->lowy;
//			lambda = width / 8;
//			if (height < lambda * 6) lambda = height / 6;
//			break;
//		case NOR:
//		case NXOR:
//			width = ni->highx - ni->lowx;
//			height = ni->highy - ni->lowy;
//			lambda = width / 10;
//			if (height < lambda * 6) lambda = height / 6;
//			break;
//#endif
//		default:
//			lambda = lambdaofnode(ni);
//			break;
//	}
//
//	/* special case for extendible primitives */
//	if (purpose && sch_nodeprotos[pindex-1]->portlist[0].addr == pp)
//	{
//		/* initialize */
//		wantx = poly->xv[0];   wanty = poly->yv[0];
//		poly->count = 1;
//		poly->style = FILLED;
//		bestdist = MAXINTBIG;
//		besti = bestx = besty = 0;
//
//		/* schematic gates must keep connections discrete and separate */
//		if (pindex == NAND || pindex == NOR || pindex == NXOR || pindex == NMUX)
//		{
//			/* determine total number of arcs already on this port */
//			for(total=0, pi = ni->firstportarcinst; pi != NOPORTARCINST; pi = pi->nextportarcinst)
//				if (pi->proto == pp) total++;
//
//			/* cycle through the arc positions */
//			total = maxi(total+2, 3);
//			for(i=0; i<total; i++)
//			{
//				/* compute the position along the left edge */
//				yposition = (i+1)/2 * WHOLE * 2;
//				if ((i&1) != 0) yposition = -yposition;
//
//				/* compute indentation (for OR and XOR) */
//				if (pindex != NMUX) xposition = -K4; else
//					xposition = -(ni->highx - ni->lowx) * 4 / 10 * WHOLE / lambda;
//				if (pindex == NOR || pindex == NXOR) switch (i)
//				{
//					case 0: xposition += T0;   break;
//					case 1:
//					case 2: xposition += H0;   break;
//				}
//
//				/* fill the polygon with that point */
//				x = getrange(ni->lowx, ni->highx, 0, xposition, lambda);
//				y = getrange(ni->lowy, ni->highy, 0, yposition, lambda);
//				xform(x, y, &poly->xv[0], &poly->yv[0], trans);
//				x = poly->xv[0];   y = poly->yv[0];
//
//				/* check for duplication */
//				for(pi = ni->firstportarcinst; pi != NOPORTARCINST; pi = pi->nextportarcinst)
//				{
//					ai = pi->conarcinst;
//					if (ai->end[0].portarcinst == pi) e = 0; else e = 1;
//					if (ai->end[e].xpos == x && ai->end[e].ypos == y) break;
//				}
//
//				/* if there is no duplication, this is a possible position */
//				if (pi == NOPORTARCINST)
//				{
//					dist = abs(wantx - x) + abs(wanty - y);
//					if (dist < bestdist)
//					{
//						bestdist = dist;   bestx = x;   besty = y;   besti = i;
//					}
//				}
//			}
//			if (bestdist == MAXINTBIG) ttyputerr(_("Warning: cannot find gate port"));
//
//			/* set the closest port */
//			poly->xv[0] = bestx;   poly->yv[0] = besty;
//
//			/* make sure the node is large enough */
//			if (besti*lambda*2 >= ni->highy - ni->lowy)
//			{
//				startobjectchange((INTBIG)ni, VNODEINST);
//				modifynodeinst(ni, 0, -lambda*2, 0, lambda*2, 0, 0);
//				endobjectchange((INTBIG)ni, VNODEINST);
//
//				/* make this gate change visible if it is in a window */
//				for(w = el_topwindowpart; w != NOWINDOWPART; w = w->nextwindowpart)
//					if (w->curnodeproto == ni->parent) break;
//				if (w != NOWINDOWPART) (void)asktool(us_tool, x_("flush-changes"));
//			}
//			return;
//		}
//
//		/* switches must discretize the location of connections */
//		if (pindex == NSWITCH)
//		{
//			/* cycle through the possible positions */
//			total = (ni->highy - ni->lowy) / lambda / 2;
//			for(i=0; i<total; i++)
//			{
//				yposition = i * 2 * WHOLE + K1;
//				xposition = -K2;
//				x = getrange(ni->lowx, ni->highx, 0, xposition, lambda);
//				y = getrange(ni->lowy, ni->highy, -H0, yposition, lambda);
//				xform(x, y, &poly->xv[0], &poly->yv[0], trans);
//				x = poly->xv[0];   y = poly->yv[0];
//				dist = abs(wantx - x) + abs(wanty - y);
//				if (dist < bestdist)
//				{
//					bestdist = dist;   bestx = x;   besty = y;   besti = i;
//				}
//			}
//			if (bestdist == MAXINTBIG) ttyputerr(_("Warning: cannot find switch port"));
//
//			/* set the closest port */
//			poly->xv[0] = bestx;   poly->yv[0] = besty;
//			return;
//		}
//	}
//	tech_fillportpoly(ni, pp, poly, trans, sch_nodeprotos[pindex-1], CLOSED, lambda);
//}
//
//INTBIG sch_arcpolys(ARCINST *ai, WINDOWPART *win)
//{
//	return(sch_intarcpolys(ai, win, &tech_oneprocpolyloop, &sch_oneprocpolyloop));
//}
//
//INTBIG sch_intarcpolys(ARCINST *ai, WINDOWPART *win, POLYLOOP *pl, SCHPOLYLOOP *schpl)
//{
//	REGISTER INTBIG i;
//
//	i = sch_arcprotos[ai->proto->arcindex]->laycount;
//	schpl->bubblebox = schpl->arrowbox = -1;
//	if ((ai->userbits&(ISNEGATED|NOTEND0)) == ISNEGATED) schpl->bubblebox = i++;
//	if ((ai->userbits&ISDIRECTIONAL) != 0) schpl->arrowbox = i++;
//
//	/* add in displayable variables */
//	pl->realpolys = i;
//	i += tech_displayableavars(ai, win, pl);
//	return(i);
//}
//
//void sch_shapearcpoly(ARCINST *ai, INTBIG box, POLYGON *poly)
//{
//	sch_intshapearcpoly(ai, box, poly, &tech_oneprocpolyloop, &sch_oneprocpolyloop);
//}
//
//void sch_intshapearcpoly(ARCINST *ai, INTBIG box, POLYGON *poly, POLYLOOP *pl, SCHPOLYLOOP *schpl)
//{
//	REGISTER INTBIG aindex, bubbleend;
//	REGISTER INTBIG angle;
//	REGISTER INTBIG x1,y1, cosdist, sindist, x2,y2, lambda, i,
//		saveendx, saveendy, bubblesize;
//	REGISTER TECH_ARCLAY *thista;
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
//	thista = &sch_arcprotos[aindex]->list[box];
//	poly->layer = thista->lay;
//	poly->desc = sch_layers[poly->layer];
//	lambda = lambdaofarc(ai);
//	if (schpl->bubblebox < 0 && schpl->arrowbox < 0)
//	{
//		/* simple arc */
//		makearcpoly(ai->length, ai->width-thista->off*lambda/WHOLE,
//			ai, poly, thista->style);
//		return;
//	}
//
//	/* prepare special information for negated and/or directional arcs */
//	bubbleend = 0;
//	x1 = ai->end[0].xpos;   y1 = ai->end[0].ypos;
//	x2 = ai->end[1].xpos;   y2 = ai->end[1].ypos;
//	angle = ((ai->userbits&AANGLE) >> AANGLESH) * 10;
//	if ((ai->userbits&REVERSEEND) != 0)
//	{
//		i = x1;   x1 = x2;   x2 = i;
//		i = y1;   y1 = y2;   y2 = i;
//		bubbleend = 1;
//		angle = (angle+1800) % 3600;
//	}
//	bubblesize = sch_bubblediameter * lambda;
//	cosdist = mult(cosine(angle), bubblesize) / WHOLE;
//	sindist = mult(sine(angle), bubblesize) / WHOLE;
//
//	/* handle the main body of the arc */
//	if (box == 0)
//	{
//		if (schpl->bubblebox >= 0)
//		{
//			/* draw the arc, shortened at the end for the negating bubble */
//			saveendx = ai->end[bubbleend].xpos;
//			saveendy = ai->end[bubbleend].ypos;
//			ai->end[bubbleend].xpos = x1 + cosdist;
//			ai->end[bubbleend].ypos = y1 + sindist;
//			makearcpoly(ai->length-sch_bubblediameter,
//				ai->width-thista->off*lambda/WHOLE, ai, poly, thista->style);
//			ai->end[bubbleend].xpos = saveendx;
//			ai->end[bubbleend].ypos = saveendy;
//		} else
//		{
//			makearcpoly(ai->length, ai->width-thista->off*lambda/WHOLE,
//				ai, poly, thista->style);
//		}
//		return;
//	}
//
//	/* draw the negating bubble */
//	if (box == schpl->bubblebox)
//	{
//		poly->count = 2;
//		if (poly->limit < 2) (void)extendpolygon(poly, 2);
//		poly->xv[0] = x1 + cosdist / 2;
//		poly->yv[0] = y1 + sindist / 2;
//		poly->xv[1] = x1;   poly->yv[1] = y1;
//		poly->style = CIRCLE;
//		return;
//	}
//
//	/* draw the directional arrow */
//	if (box == schpl->arrowbox)
//	{
//		if ((ai->userbits&(ISNEGATED|NOTEND0)) == ISNEGATED)
//		{
//			x1 += cosdist;
//			y1 += sindist;
//		}
//		poly->style = VECTORS;
//		poly->layer = -1;
//		if (aindex == ABUS)
//		{
//			poly->desc = &sch_t_lay;
//			x2 -= cosdist / 2;
//			y2 -= sindist / 2;
//		}
//		if (poly->limit < 2) (void)extendpolygon(poly, 2);
//		poly->count = 2;
//		poly->xv[0] = x1;   poly->yv[0] = y1;
//		poly->xv[1] = x2;   poly->yv[1] = y2;
//		if ((ai->userbits&NOTEND1) == 0)
//			tech_addheadarrow(poly, angle, x2, y2, lambda);
//	}
//}
//
//INTBIG sch_allarcpolys(ARCINST *ai, POLYLIST *plist, WINDOWPART *win)
//{
//	REGISTER INTBIG tot, j;
//	POLYLOOP mypl;
//	SCHPOLYLOOP myschpl;
//
//	mypl.curwindowpart = win;
//	tot = sch_intarcpolys(ai, win, &mypl, &myschpl);
//	tot = mypl.realpolys;
//	if (ensurepolylist(plist, tot, db_cluster)) return(-1);
//	for(j = 0; j < tot; j++)
//	{
//		sch_intshapearcpoly(ai, j, plist->polygons[j], &mypl, &myschpl);
//	}
//	return(tot);
//}
}
