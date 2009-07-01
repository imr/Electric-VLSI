/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: TechEditWizardData.java
 * Create an Electric XML Technology from a simple numeric description of design rules
 * Written in Perl by Andrew Wewist, translated to Java by Steven Rubin.
 *
 * Copyright (c) 2008 Sun Microsystems and Static Free Software
 *
 * Electric(tm) is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
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
package com.sun.electric.tool.user.tecEditWizard;

import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.geometry.*;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.IOTool;
import com.sun.electric.tool.user.dialogs.OpenFile;
import com.sun.electric.tool.user.User;
import com.sun.electric.technology.*;

import java.awt.Color;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

/**
 * Class to handle the "Technology Creation Wizard" dialog.
 */
public class TechEditWizardData
{
	/************************************** THE DATA **************************************/

	private String tech_name;
	private String tech_description;
	private int num_metal_layers;
	private int stepsize;   // value in nm
    private int resolution; // technology resolution in the same scale as stepsize
    private boolean pWellFlag = true; // to control if process is a pwell process or not. If true, Tech Creation Wizard will not create pwell layers
    private boolean horizontalFlag = true; // to control if transistor gates are aligned horizontally. True by default . If transistors are horizontal -> M1 is horizontal?
    private boolean extraInfoFlag = false; // to control if protection polys are added to transistors. False by default

    // DIFFUSION RULES
	private WizardField diff_width = new WizardField();
	private WizardField diff_poly_overhang = new WizardField();		// min. diff overhang from gate edge
	private WizardField diff_contact_overhang = new WizardField();	// min. diff overhang contact
	private WizardField diff_contact_overhang_min_short = new WizardField();				// diff overhang contact. It should hold the min short value
	private WizardField diff_contact_overhang_min_long = new WizardField();				// diff overhang contact. It should hold the min long value
	private WizardField diff_spacing = new WizardField();

	// POLY RULES
	private WizardField poly_width = new WizardField();
	private WizardField poly_endcap = new WizardField();			// min. poly gate extension from edge of diffusion
	private WizardField poly_spacing = new WizardField();
	private WizardField poly_diff_spacing = new WizardField();		// min. spacing between poly and diffusion
    private WizardField poly_protection_spacing = new WizardField();		// min. spacing between poly and dummy poly

    // GATE RULES
	private WizardField gate_length = new WizardField();			// min. transistor gate length
	private WizardField gate_width = new WizardField();				// min. transistor gate width
	private WizardField gate_spacing = new WizardField();			// min. gate to gate spacing on diffusion
	private WizardField gate_contact_spacing = new WizardField();	// min. spacing from gate edge to contact inside diffusion

    // Special rules for OD18 transistors if specified.
    private WizardField gate_od18_length = new WizardField();       // transistor gate length for OD18 transistors
    private WizardField gate_od18_width = new WizardField();        // transistor gate width for OD18 transistors
    private WizardField[] od18_diff_overhang = new WizardField[]{new WizardField(), new WizardField()};             // OD18 X and Y overhang

    // Special rules for native transistors if specified
    private WizardField gate_nt_length  = new WizardField();        // transistor gate length for native transistors
    private WizardField gate_nt_width = new WizardField();          // transistor gate width for OD18 transistors
    private WizardField poly_nt_endcap = new WizardField();         // gate extension from edge of diffusion for native transistors
    private WizardField nt_diff_overhang = new WizardField();       // extension from OD

    // Special rules for vth/vtl transistors if specified.
    private WizardField vthl_diff_overhang  = new WizardField();    // Overhang of VTH/VTL with respecto to OD
    private WizardField vthl_poly_overhang = new WizardField();     // Overhang of VTH/VTL with respecto to the gate

    // Special rules for resistors
//    private WizardField poly_resistor_length =
//    private WizardField poly_resistor_width =
//    private WizardField rpo_contact_spacing =
//    private WizardField rpo_poly_overhang =

    private WizardField[] extraVariables = new WizardField[]{
        new WizardField("poly_resistor_length"),   // Poly resistor length
        new WizardField("poly_resistor_width"),   // Poly resistor width
        new WizardField("rpo_contact_spacing"),   // Spacing btw rpo edge and contact cut
        new WizardField("rpo_poly_overhang"),   // RPO overhang from poly
        new WizardField("rh_odpoly_overhang")   // RH overhang from poly/OD
    };

    // CONTACT RULES
	private WizardField contact_size = new WizardField();
	private WizardField contact_spacing = new WizardField();
    private WizardField contact_array_spacing = new WizardField();
    private WizardField contact_metal_overhang_inline_only = new WizardField();	// metal overhang when overhanging contact from two sides only
	private WizardField contact_metal_overhang_all_sides = new WizardField();	// metal overhang when surrounding contact
	private WizardField contact_poly_overhang = new WizardField();				// poly overhang contact. It should hold the recommended value
	private WizardField polycon_diff_spacing = new WizardField();				// spacing between poly-metal contact edge and diffusion

	// WELL AND IMPLANT RULES
	private WizardField nplus_width = new WizardField();
	private WizardField nplus_overhang_diff = new WizardField();
	private WizardField nplus_overhang_poly = new WizardField();
	private WizardField nplus_spacing = new WizardField();

	private WizardField pplus_width = new WizardField();
	private WizardField pplus_overhang_diff = new WizardField();
	private WizardField pplus_overhang_poly = new WizardField();
	private WizardField pplus_spacing = new WizardField();

	private WizardField nwell_width = new WizardField();
	private WizardField nwell_overhang_diff_p = new WizardField();
    private WizardField nwell_overhang_diff_n = new WizardField();
    private WizardField nwell_spacing = new WizardField();

	// METAL RULES
	private WizardField [] metal_width;
	private WizardField [] metal_spacing;

	// VIA RULES
	private WizardField [] via_size;
	private WizardField [] via_inline_spacing;
	private WizardField [] via_array_spacing;
	private WizardField [] via_overhang;

    // generic cross contacts
    private static class ContactNode
    {
        String layer;
        WizardField overX; // overhang X value
        WizardField overY; // overhang Y value

        ContactNode(String l, double overXV, String overXS, double overYV, String overYS)
        {
            layer = l;
            overX = new WizardField(overXV, overXS);
            overY = new WizardField(overYV, overYS);
        }
    }
    private static class Contact
    {
        // some primitives might not have prefix. "-" should not be in the prefix to avoid
        // being displayed in the palette
        String prefix;
        List<ContactNode> layers;

        // odd metals go vertical
        Contact (String p)
        {
            prefix = p;
            layers = new ArrayList<ContactNode>();
        }
    }
    private Map<String,List<Contact>> metalContacts;
    private Map<String,List<Contact>> otherContacts;

    private static class PaletteGroup
    {
        String name;
        List<Xml.ArcProto> arcs;
        List<Xml.MenuNodeInst> pins;
        List<Xml.MenuNodeInst> elements; // contact or transistor

        void addArc(Xml.ArcProto arc)
        {
            if (arcs == null)
            {
                arcs = new ArrayList<Xml.ArcProto>();
            }
            arcs.add(arc);
        }
        private void add(List<Xml.MenuNodeInst> list, Xml.PrimitiveNodeGroup element, String shortName)
        {
            assert element.isSingleton;
            Xml.PrimitiveNode pn = element.nodes.get(0);
            Xml.MenuNodeInst n = new Xml.MenuNodeInst();
            n.protoName = pn.name;
            n.function = pn.function;
            if (shortName != null)
            {
                n.text = shortName;
            }
            list.add(n);
        }
        void addPinOrResistor(Xml.PrimitiveNodeGroup pin, String shortName)
        {
            if (pins == null)
            {
                pins = new ArrayList<Xml.MenuNodeInst>();
            }
            add(pins, pin, shortName);
        }
        void addElement(Xml.PrimitiveNodeGroup element, String shortName)
        {
            if (elements == null)
            {
                elements = new ArrayList<Xml.MenuNodeInst>();
            }
            add(elements, element, shortName);
        }
    }

    // ANTENNA RULES
	private double poly_antenna_ratio;
	private double [] metal_antenna_ratio;

	// GDS-II LAYERS
    public static class LayerInfo
    {
        String name;
        int value; // normal value
        int type; // datatype of the normal value
        int pin; // pin value
        int pinType; // pin datatype
        int text; // text value
        int textType; // text datatype
        String graphicsTemplate; // uses other template for the graphics
        Color graphicsColor; // uses this color with no fill
        EGraphics.Outline graphicsOutline; // uses this outline with graphicsColor
        int [] graphicsPattern; // uses this pattern with graphicsColor

        LayerInfo(String n)
        {
            name = n;
        }
        String getValueWithType() {return (type != 0) ? value + "/" + type : value + "";}
        String getPinWithType() {return (pinType != 0) ? pin + "/" + pinType : pin + "";}
        String getTextWithType() {return (textType != 0) ? text + "/" + textType : text + "";}
        void setGDSData(int[] vals)
        {
            assert(vals.length == 6);
            value = vals[0];
            type = vals[1];
            pin = vals[2];
            pinType = vals[3];
            text = vals[4];
            textType = vals[5];
        }

        void setGraphicsTemplate(String s)
        {
            if (s.startsWith("[")) // color
            {
                StringTokenizer p = new StringTokenizer(s, ", []", false);
                int[] colors = new int[3];
                String outlineOrPattern = null; // EGraphics.Outline.NOPAT.name(); // default
                int itemCount = 0;
                while (p.hasMoreTokens())
                {
                    String str = p.nextToken();
                    if (itemCount < 3)
                        colors[itemCount++] = Integer.parseInt(str);
                    else
                        outlineOrPattern = str;
                    assert(itemCount < 4);
                }

                EGraphics.Outline outline = EGraphics.Outline.findOutline(EGraphics.Outline.NOPAT.name());
                int[] pattern = new int[16];

                graphicsColor = new Color(colors[0], colors[1], colors[2]);
                if (outlineOrPattern != null)
                {
                    EGraphics.Outline out = EGraphics.Outline.findOutline(outlineOrPattern);
                    if (out != null) // manages to parse a valid Outline
                        outline = out;
                    else
                    {
                        assert(outlineOrPattern.startsWith("{"));

                        // Pattern information
                        StringTokenizer pat = new StringTokenizer(outlineOrPattern, "/ {}", false);
                        int count = 0;
                        while (pat.hasMoreTokens())
                        {
                            String str = pat.nextToken();
                            int num = Integer.parseInt(str);
                            assert(count < 16);
                            pattern[count++] = num;
                        }
                        if (count != 16)
                        assert(count == 16);
                    }
                }
                graphicsOutline = outline;
                graphicsPattern = pattern;
            }
            else
                graphicsTemplate = s;
        }

        public String toString()
        {
            String val = getValueWithType(); // useful datatype
            if (pin != 0)
            {
                val = val + "," + getPinWithType() + "p";
            }
            if (text != 0)
            {
                val = val + "," + getTextWithType() + "t";
            }
            return val;
        }
    }

    private LayerInfo diff_layer = new LayerInfo("Diff");
    private LayerInfo poly_layer = new LayerInfo("Poly");
    private LayerInfo nplus_layer = new LayerInfo("NPlus");
	private LayerInfo pplus_layer = new LayerInfo("PPlus");
	private LayerInfo nwell_layer = new LayerInfo("N-Well");
	private LayerInfo contact_layer = new LayerInfo("Contact");
	private LayerInfo [] metal_layers;
	private LayerInfo [] via_layers;
    private LayerInfo marking_layer = new LayerInfo("Marking");		// Device marking layer

    // extra layers
    private List<LayerInfo> extraLayers;

    LayerInfo[] getBasicLayers()
    {
        List<LayerInfo> layers = new ArrayList<LayerInfo>();

        layers.add(diff_layer);
        layers.add(poly_layer);
        if (getExtraInfoFlag())
            layers.addAll(extraLayers);
        layers.add(nplus_layer);
        layers.add(pplus_layer);
        layers.add(nwell_layer);
        layers.add(contact_layer);
        layers.add(marking_layer);
        LayerInfo[] array = new LayerInfo[layers.size()];
        layers.toArray(array);
        return array;
    }

    public TechEditWizardData()
	{
		stepsize = 100;
        num_metal_layers = 2;
		metal_width = new WizardField[num_metal_layers];
		metal_spacing = new WizardField[num_metal_layers];
		via_size = new WizardField[num_metal_layers-1];
		via_inline_spacing = new WizardField[num_metal_layers-1];
		via_array_spacing = new WizardField[num_metal_layers-1];
		via_overhang = new WizardField[num_metal_layers-1];
		metal_antenna_ratio = new double[num_metal_layers];

        metalContacts = new HashMap<String,List<Contact>>();
        otherContacts = new HashMap<String,List<Contact>>();

        metal_layers = new LayerInfo[num_metal_layers];
		via_layers = new LayerInfo[num_metal_layers-1];

        for(int i=0; i<num_metal_layers; i++)
		{
			metal_width[i] = new WizardField();
			metal_spacing[i] = new WizardField();
            metal_layers[i] = new LayerInfo("Metal-"+(i+1));
        }

        for(int i=0; i<num_metal_layers-1; i++)
		{
			via_size[i] = new WizardField();
			via_inline_spacing[i] = new WizardField();
			via_array_spacing[i] = new WizardField();
			via_overhang[i] = new WizardField();
            via_layers[i] = new LayerInfo("Via-"+(i+1));
        }

        // extra layers
        extraLayers = new ArrayList<LayerInfo>();

    }

	/************************************** ACCESSOR METHODS **************************************/

	public String getTechName() { return tech_name; }
	public void setTechName(String s) { tech_name = s; }

	public String getTechDescription() { return tech_description; }
	public void setTechDescription(String s) { tech_description = s; }

	public int getStepSize() { return stepsize; }
	public void setStepSize(int n) { stepsize = n; }

    public int getResolution() { return resolution; }
	public void setResolution(int n) { resolution = n; }

    public int getNumMetalLayers() { return num_metal_layers; }
	public void setNumMetalLayers(int n)
	{
		int smallest = Math.min(n, num_metal_layers);

		WizardField [] new_metal_width = new WizardField[n];
		for(int i=0; i<smallest; i++) new_metal_width[i] = metal_width[i];
		for(int i=smallest; i<n; i++) new_metal_width[i] = new WizardField();
		metal_width = new_metal_width;

		WizardField [] new_metal_spacing = new WizardField[n];
		for(int i=0; i<smallest; i++) new_metal_spacing[i] = metal_spacing[i];
		for(int i=smallest; i<n; i++) new_metal_spacing[i] = new WizardField();
		metal_spacing = new_metal_spacing;

		WizardField [] new_via_size = new WizardField[n-1];
		for(int i=0; i<smallest-1; i++) new_via_size[i] = via_size[i];
		for(int i=smallest-1; i<n-1; i++) new_via_size[i] = new WizardField();
		via_size = new_via_size;

		WizardField [] new_via_spacing = new WizardField[n-1];
		for(int i=0; i<smallest-1; i++) new_via_spacing[i] = via_inline_spacing[i];
		for(int i=smallest-1; i<n-1; i++) new_via_spacing[i] = new WizardField();
		via_inline_spacing = new_via_spacing;

		WizardField [] new_via_array_spacing = new WizardField[n-1];
		for(int i=0; i<smallest-1; i++) new_via_array_spacing[i] = via_array_spacing[i];
		for(int i=smallest-1; i<n-1; i++) new_via_array_spacing[i] = new WizardField();
		via_array_spacing = new_via_array_spacing;

		WizardField [] new_via_overhang_inline = new WizardField[n-1];
		for(int i=0; i<smallest-1; i++) new_via_overhang_inline[i] = via_overhang[i];
		for(int i=smallest-1; i<n-1; i++) new_via_overhang_inline[i] = new WizardField();
		via_overhang = new_via_overhang_inline;

        double [] new_metal_antenna_ratio = new double[n];
		for(int i=0; i<smallest; i++) new_metal_antenna_ratio[i] = metal_antenna_ratio[i];
		metal_antenna_ratio = new_metal_antenna_ratio;

		LayerInfo [] new_gds_metal_layer = new LayerInfo[n];
        for(int i=0; i<smallest; i++)
        {
            new_gds_metal_layer[i] = metal_layers[i];
        }
        for(int i=smallest-1; i<n; i++)
        {
            new_gds_metal_layer[i] = new LayerInfo("Metal-"+(i+1));
        }
        metal_layers = new_gds_metal_layer;

        LayerInfo [] new_gds_via_layer = new LayerInfo[n-1];
		for(int i=0; i<smallest-1; i++) new_gds_via_layer[i] = via_layers[i];
        for(int i=smallest-1; i<n-1; i++) new_gds_via_layer[i] = new LayerInfo("Via-"+(i+1));
        via_layers = new_gds_via_layer;

		num_metal_layers = n;
	}

    // Flags
    boolean getPWellProcess() { return pWellFlag;}
    void setPWellProcess(boolean b) { pWellFlag = b; }
    boolean getHorizontalTransistors() { return horizontalFlag;}
    void setHorizontalTransistors(boolean b) { horizontalFlag = b; }
    boolean getExtraInfoFlag() { return extraInfoFlag;}
    void setExtraInfoFlag(boolean b) { extraInfoFlag = b; }

    // DIFFUSION RULES
	WizardField getDiffWidth() { return diff_width; }
	void setDiffWidth(WizardField v) { diff_width = v; }
	WizardField getDiffPolyOverhang() { return diff_poly_overhang; }
	void setDiffPolyOverhang(WizardField v) { diff_poly_overhang = v; }
	WizardField getDiffContactOverhang() { return diff_contact_overhang; }
	void setDiffContactOverhang(WizardField v) { diff_contact_overhang = v; }
	WizardField getDiffSpacing() { return diff_spacing; }
	void setDiffSpacing(WizardField v) { diff_spacing = v; }

	// POLY RULES
	WizardField getPolyWidth() { return poly_width; }
	void setPolyWidth(WizardField v) { poly_width = v; }
	WizardField getPolyEndcap() { return poly_endcap; }
	void setPolyEndcap(WizardField v) { poly_endcap = v; }
	WizardField getPolySpacing() { return poly_spacing; }
	void setPolySpacing(WizardField v) { poly_spacing = v; }
	WizardField getPolyDiffSpacing() { return poly_diff_spacing; }
	void setPolyDiffSpacing(WizardField v) { poly_diff_spacing = v; }
    WizardField getPolyProtectionSpacing() { return poly_protection_spacing; }
	void setPolyProtectionSpacing(WizardField v) { poly_protection_spacing = v; }

    // GATE RULES
	WizardField getGateLength() { return gate_length; }
	void setGateLength(WizardField v) { gate_length = v; }
	WizardField getGateWidth() { return gate_width; }
	void setGateWidth(WizardField v) { gate_width = v; }
	WizardField getGateSpacing() { return gate_spacing; }
	void setGateSpacing(WizardField v) { gate_spacing = v; }
	WizardField getGateContactSpacing() { return gate_contact_spacing; }
	void setGateContactSpacing(WizardField v) { gate_contact_spacing = v; }


    // CONTACT RULES
	WizardField getContactSize() { return contact_size; }
	void setContactSize(WizardField v) { contact_size = v; }
	WizardField getContactSpacing() { return contact_spacing; }
	void setContactSpacing(WizardField v) { contact_spacing = v; }
    WizardField getContactArraySpacing() { return contact_array_spacing; }
	void setContactArraySpacing(WizardField v) { contact_array_spacing = v; }
    WizardField getContactMetalOverhangInlineOnly() { return contact_metal_overhang_inline_only; }
	void setContactMetalOverhangInlineOnly(WizardField v) { contact_metal_overhang_inline_only = v; }
	WizardField getContactMetalOverhangAllSides() { return contact_metal_overhang_all_sides; }
	void setContactMetalOverhangAllSides(WizardField v) { contact_metal_overhang_all_sides = v; }
	WizardField getContactPolyOverhang() { return contact_poly_overhang; }
	void setContactPolyOverhang(WizardField v) { contact_poly_overhang = v; }
	WizardField getPolyconDiffSpacing() { return polycon_diff_spacing; }
	void setPolyconDiffSpacing(WizardField v) { polycon_diff_spacing = v; }

	// WELL AND IMPLANT RULES
	WizardField getNPlusWidth() { return nplus_width; }
	void setNPlusWidth(WizardField v) { nplus_width = v; }
	WizardField getNPlusOverhangDiff() { return nplus_overhang_diff; }
    void setNPlusOverhangDiff(WizardField v) { nplus_overhang_diff = v; }
    WizardField getNPlusOverhangPoly() { return nplus_overhang_poly; }
    void setNPlusOverhangPoly(WizardField v) { nplus_overhang_poly = v; }
	WizardField getNPlusSpacing() { return nplus_spacing; }
	void setNPlusSpacing(WizardField v) { nplus_spacing = v; }

	WizardField getPPlusWidth() { return pplus_width; }
	void setPPlusWidth(WizardField v) { pplus_width = v; }
	WizardField getPPlusOverhangDiff() { return pplus_overhang_diff; }
	void setPPlusOverhangDiff(WizardField v) { pplus_overhang_diff = v; }
	WizardField getPPlusOverhangPoly() { return pplus_overhang_poly; }
	void setPPlusOverhangPoly(WizardField v) { pplus_overhang_poly = v; }
	WizardField getPPlusSpacing() { return pplus_spacing; }
	void setPPlusSpacing(WizardField v) { pplus_spacing = v; }

	WizardField getNWellWidth() { return nwell_width; }
	void setNWellWidth(WizardField v) { nwell_width = v; }
	WizardField getNWellOverhangDiffP() { return nwell_overhang_diff_p; }
	void setNWellOverhangDiffP(WizardField v) { nwell_overhang_diff_p = v; }
	WizardField getNWellOverhangDiffN() { return nwell_overhang_diff_n; }
	void setNWellOverhangDiffN(WizardField v) { nwell_overhang_diff_n = v; }
	WizardField getNWellSpacing() { return nwell_spacing; }
	void setNWellSpacing(WizardField v) { nwell_spacing = v; }

	// METAL RULES
	WizardField [] getMetalWidth() { return metal_width; }
	void setMetalWidth(int met, WizardField value) { metal_width[met] = value; }
	WizardField [] getMetalSpacing() { return metal_spacing; }
	void setMetalSpacing(int met, WizardField value) { metal_spacing[met] = value; }

	// VIA RULES
	WizardField [] getViaSize() { return via_size; }
	void setViaSize(int via, WizardField value) { via_size[via] = value; }
	WizardField [] getViaSpacing() { return via_inline_spacing; }
	void setViaSpacing(int via, WizardField value) { via_inline_spacing[via] = value; }
	WizardField [] getViaArraySpacing() { return via_array_spacing; }
	void setViaArraySpacing(int via, WizardField value) { via_array_spacing[via] = value; }
	WizardField [] getViaOverhangInline() { return via_overhang; }
	void setViaOverhangInline(int via, WizardField value) { via_overhang[via] = value; }

	// ANTENNA RULES
	public double getPolyAntennaRatio() { return poly_antenna_ratio; }
	void setPolyAntennaRatio(double v) { poly_antenna_ratio = v; }
	public double [] getMetalAntennaRatio() { return metal_antenna_ratio; }
	void setMetalAntennaRatio(int met, double value) { metal_antenna_ratio[met] = value; }

	// GDS-II LAYERS
    static int[] getGDSValuesFromString(String s)
    {
        int[] vals = new int[6];
        StringTokenizer parse = new StringTokenizer(s, ",", false);

        while (parse.hasMoreTokens())
        {
            String v = parse.nextToken();
            int pos = 0;
            int index = v.indexOf("/");

            if (v.contains("p")) // pin section
            {
                pos = 2;
            } else if (v.contains("t")) // text section
            {
                pos = 4;
            }
            if (index != -1) // datatype value
            {
                vals[pos] = TextUtils.atoi(v.substring(0, index));
                vals[pos+1] = TextUtils.atoi(v.substring(index+1));
            }
            else
                vals[pos] = TextUtils.atoi(v);
        }
        return vals;
    }

	TechEditWizardData.LayerInfo [] getGDSMetal() { return metal_layers; }
	TechEditWizardData.LayerInfo [] getGDSVia() { return via_layers; }

	private String errorInData()
	{
		// check the General data
		if (tech_name == null || tech_name.length() == 0) return "General panel: No technology name";
		if (stepsize == 0) return "General panel: Invalid unit size";

        // check the Active data
		if (diff_width.v == 0) return "Active panel: Invalid width";

		// check the Poly data
		if (poly_width.v == 0) return "Poly panel: Invalid width";

		// check the Gate data
		if (gate_width.v == 0) return "Gate panel: Invalid width";
		if (gate_length.v == 0) return "Gate panel: Invalid length";

		// check the Contact data
		if (contact_size.v == 0) return "Contact panel: Invalid size";

		// check the Well/Implant data
		if (nplus_width.v == 0) return "Well/Implant panel: Invalid NPlus width";
		if (pplus_width.v == 0) return "Well/Implant panel: Invalid PPlus width";
		if (nwell_width.v == 0) return "Well/Implant panel: Invalid NWell width";

		// check the Metal data
		for(int i=0; i<num_metal_layers; i++)
			if (metal_width[i].v == 0) return "Metal panel: Invalid Metal-" + (i+1) + " width";

		// check the Via data
		for(int i=0; i<num_metal_layers-1; i++)
			if (via_size[i].v == 0) return "Via panel: Invalid Via-" + (i+1) + " size";
		return null;
	}

	/************************************** IMPORT RAW NUMBERS FROM DISK **************************************/

	/**
	 * Method to import data from a file to this object.
	 * @return true on success; false on failure.
	 */
	boolean importData()
	{
		String fileName = OpenFile.chooseInputFile(FileType.ANY, "Technology Wizard File");
		if (fileName == null) return false;
        return importData(fileName);
    }

    /**
     * Method to import data from a given file to this object. It is also in the regression so
     * keep the access.
     * @param fileName the name of the file to import.
     * @return true on success; false on failure.
     */
    public boolean importData(String fileName)
    {
        URL url = TextUtils.makeURLToFile(fileName);

        // clean arrays first
        metalContacts.clear();
        otherContacts.clear();
        extraLayers.clear();
        
        try
		{
			URLConnection urlCon = url.openConnection();
			InputStreamReader is = new InputStreamReader(urlCon.getInputStream());
			LineNumberReader lineReader = new LineNumberReader(is);
			for(;;)
			{
				String buf = lineReader.readLine();
				if (buf == null) break;
				buf = buf.trim();
				if (buf.length() == 0 || buf.startsWith("#")) continue;

				// parse the assignment
				if (buf.startsWith("$") || buf.startsWith("@"))
				{
					int spacePos = buf.indexOf(' ');
					int equalsPos = buf.indexOf('=');
					if (equalsPos < 0)
					{
						Job.getUserInterface().showErrorMessage("Missing '=' on line " + lineReader.getLineNumber(),
							"Syntax Error In Technology File");
						break;
					}
					if (spacePos < 0) spacePos = equalsPos; else
						spacePos = Math.min(spacePos, equalsPos);
					String varName = buf.substring(1, spacePos);

					int semiPos = buf.indexOf(';');
					if (semiPos < 0)
					{
						Job.getUserInterface().showErrorMessage("Missing ';' on line " + lineReader.getLineNumber(),
							"Syntax Error In Technology File");
						break;
					}
					equalsPos++;
					while (equalsPos < semiPos && buf.charAt(equalsPos) == ' ') equalsPos++;
					String varValue = buf.substring(equalsPos, semiPos);

					// now figure out what to assign
					if (varName.equalsIgnoreCase("tech_libname")) { } else
					if (varName.equalsIgnoreCase("tech_name")) setTechName(stripQuotes(varValue)); else
					if (varName.equalsIgnoreCase("tech_description")) setTechDescription(stripQuotes(varValue)); else
					if (varName.equalsIgnoreCase("num_metal_layers")) setNumMetalLayers(TextUtils.atoi(varValue)); else
                    if (varName.equalsIgnoreCase("pwell_process")) setPWellProcess(Boolean.valueOf(varValue)); else
                    if (varName.equalsIgnoreCase("horizontal_transistors")) setHorizontalTransistors(Boolean.valueOf(varValue)); else
                    if (varName.equalsIgnoreCase("extra_info")) setExtraInfoFlag(Boolean.valueOf(varValue)); else
                    if (varName.equalsIgnoreCase("stepsize")) setStepSize(TextUtils.atoi(varValue)); else
                    if (varName.equalsIgnoreCase("resolution")) setResolution(TextUtils.atoi(varValue)); else

                    if (varName.equalsIgnoreCase("diff_width")) diff_width.v = TextUtils.atof(varValue); else
					if (varName.equalsIgnoreCase("diff_width_rule")) diff_width.rule = stripQuotes(varValue); else
					if (varName.equalsIgnoreCase("diff_poly_overhang")) diff_poly_overhang.v = TextUtils.atof(varValue); else
					if (varName.equalsIgnoreCase("diff_poly_overhang_rule")) diff_poly_overhang.rule = stripQuotes(varValue); else
					if (varName.equalsIgnoreCase("diff_contact_overhang")) diff_contact_overhang.v = TextUtils.atof(varValue); else
					if (varName.equalsIgnoreCase("diff_contact_overhang_rule")) diff_contact_overhang.rule = stripQuotes(varValue); else
                    if (varName.equalsIgnoreCase("diff_contact_overhang_short_min")) diff_contact_overhang_min_short.v = TextUtils.atof(varValue); else
                    if (varName.equalsIgnoreCase("diff_contact_overhang_short_min_rule")) diff_contact_overhang_min_short.rule = stripQuotes(varValue); else
                    if (varName.equalsIgnoreCase("diff_contact_overhang_long_min")) diff_contact_overhang_min_long.v = TextUtils.atof(varValue); else
                    if (varName.equalsIgnoreCase("diff_contact_overhang_long_min_rule")) diff_contact_overhang_min_long.rule = stripQuotes(varValue); else
					if (varName.equalsIgnoreCase("diff_spacing")) diff_spacing.v = TextUtils.atof(varValue); else
					if (varName.equalsIgnoreCase("diff_spacing_rule")) diff_spacing.rule = stripQuotes(varValue); else

					if (varName.equalsIgnoreCase("poly_width")) poly_width.v = TextUtils.atof(varValue); else
					if (varName.equalsIgnoreCase("poly_width_rule")) poly_width.rule = stripQuotes(varValue); else
					if (varName.equalsIgnoreCase("poly_endcap")) poly_endcap.v = TextUtils.atof(varValue); else
					if (varName.equalsIgnoreCase("poly_endcap_rule")) poly_endcap.rule = stripQuotes(varValue); else
					if (varName.equalsIgnoreCase("poly_spacing")) poly_spacing.v = TextUtils.atof(varValue); else
					if (varName.equalsIgnoreCase("poly_spacing_rule")) poly_spacing.rule = stripQuotes(varValue); else
					if (varName.equalsIgnoreCase("poly_diff_spacing")) poly_diff_spacing.v = TextUtils.atof(varValue); else
					if (varName.equalsIgnoreCase("poly_diff_spacing_rule")) poly_diff_spacing.rule = stripQuotes(varValue); else
					if (varName.equalsIgnoreCase("poly_protection_spacing")) poly_protection_spacing.v = TextUtils.atof(varValue); else
					if (varName.equalsIgnoreCase("poly_protection_spacing_rule")) poly_protection_spacing.rule = stripQuotes(varValue); else

                    if (varName.equalsIgnoreCase("gate_length")) gate_length.v = TextUtils.atof(varValue); else
					if (varName.equalsIgnoreCase("gate_length_rule")) gate_length.rule = stripQuotes(varValue); else
					if (varName.equalsIgnoreCase("gate_width")) gate_width.v = TextUtils.atof(varValue); else
					if (varName.equalsIgnoreCase("gate_width_rule")) gate_width.rule = stripQuotes(varValue); else
					if (varName.equalsIgnoreCase("gate_spacing")) gate_spacing.v = TextUtils.atof(varValue); else
					if (varName.equalsIgnoreCase("gate_spacing_rule")) gate_spacing.rule = stripQuotes(varValue); else
					if (varName.equalsIgnoreCase("gate_contact_spacing")) gate_contact_spacing.v = TextUtils.atof(varValue); else
					if (varName.equalsIgnoreCase("gate_contact_spacing_rule")) gate_contact_spacing.rule = stripQuotes(varValue); else

                    if (varName.equalsIgnoreCase("gate_od18_length")) fillRule(varValue, gate_od18_length); else
                    if (varName.equalsIgnoreCase("gate_od18_width")) fillRule(varValue, gate_od18_width); else
                    if (varName.equalsIgnoreCase("od18_diff_overhang")) fillRule(varValue, od18_diff_overhang); else

                    if (varName.equalsIgnoreCase("gate_nt_length")) fillRule(varValue, gate_nt_length); else
                    if (varName.equalsIgnoreCase("gate_nt_width")) fillRule(varValue, gate_nt_width); else
                    if (varName.equalsIgnoreCase("poly_nt_endcap")) fillRule(varValue, poly_nt_endcap); else
                    if (varName.equalsIgnoreCase("nt_diff_overhang")) fillRule(varValue, nt_diff_overhang); else

                    if (varName.equalsIgnoreCase("vthl_diff_overhang")) fillRule(varValue, vthl_diff_overhang); else
                    if (varName.equalsIgnoreCase("vthl_poly_overhang")) fillRule(varValue, vthl_poly_overhang); else

                    if (varName.equalsIgnoreCase("contact_size")) contact_size.v = TextUtils.atof(varValue); else
					if (varName.equalsIgnoreCase("contact_size_rule")) contact_size.rule = stripQuotes(varValue); else
					if (varName.equalsIgnoreCase("contact_spacing")) contact_spacing.v = TextUtils.atof(varValue); else
					if (varName.equalsIgnoreCase("contact_spacing_rule")) contact_spacing.rule = stripQuotes(varValue); else
                    if (varName.equalsIgnoreCase("contact_array_spacing")) contact_array_spacing.v = TextUtils.atof(varValue); else
					if (varName.equalsIgnoreCase("contact_array_spacing_rule")) contact_array_spacing.rule = stripQuotes(varValue); else
                    if (varName.equalsIgnoreCase("contact_metal_overhang_inline_only")) contact_metal_overhang_inline_only.v = TextUtils.atof(varValue); else
					if (varName.equalsIgnoreCase("contact_metal_overhang_inline_only_rule")) contact_metal_overhang_inline_only.rule = stripQuotes(varValue); else
					if (varName.equalsIgnoreCase("contact_metal_overhang_all_sides")) contact_metal_overhang_all_sides.v = TextUtils.atof(varValue); else
					if (varName.equalsIgnoreCase("contact_metal_overhang_all_sides_rule")) contact_metal_overhang_all_sides.rule = stripQuotes(varValue); else
					if (varName.equalsIgnoreCase("contact_poly_overhang")) contact_poly_overhang.v = TextUtils.atof(varValue); else
                    if (varName.equalsIgnoreCase("contact_poly_overhang_rule")) contact_poly_overhang.rule = stripQuotes(varValue); else
                    if (varName.equalsIgnoreCase("polycon_diff_spacing")) polycon_diff_spacing.v = TextUtils.atof(varValue); else
					if (varName.equalsIgnoreCase("polycon_diff_spacing_rule")) polycon_diff_spacing.rule = stripQuotes(varValue); else

					if (varName.equalsIgnoreCase("nplus_width")) nplus_width.v = TextUtils.atof(varValue); else
					if (varName.equalsIgnoreCase("nplus_width_rule")) nplus_width.rule = stripQuotes(varValue); else
					if (varName.equalsIgnoreCase("nplus_overhang_diff")) nplus_overhang_diff.v = TextUtils.atof(varValue); else
					if (varName.equalsIgnoreCase("nplus_overhang_diff_rule")) nplus_overhang_diff.rule = stripQuotes(varValue); else
					if (varName.equalsIgnoreCase("nplus_overhang_poly")) nplus_overhang_poly.v = TextUtils.atof(varValue); else
					if (varName.equalsIgnoreCase("nplus_overhang_poly_rule")) nplus_overhang_poly.rule = stripQuotes(varValue); else
                    if (varName.equalsIgnoreCase("nplus_spacing")) nplus_spacing.v = TextUtils.atof(varValue); else
					if (varName.equalsIgnoreCase("nplus_spacing_rule")) nplus_spacing.rule = stripQuotes(varValue); else

					if (varName.equalsIgnoreCase("pplus_width")) pplus_width.v = TextUtils.atof(varValue); else
					if (varName.equalsIgnoreCase("pplus_width_rule")) pplus_width.rule = stripQuotes(varValue); else
					if (varName.equalsIgnoreCase("pplus_overhang_diff")) pplus_overhang_diff.v = TextUtils.atof(varValue); else
					if (varName.equalsIgnoreCase("pplus_overhang_diff_rule")) pplus_overhang_diff.rule = stripQuotes(varValue); else
					if (varName.equalsIgnoreCase("pplus_overhang_poly")) pplus_overhang_poly.v = TextUtils.atof(varValue); else
					if (varName.equalsIgnoreCase("pplus_overhang_poly_rule")) pplus_overhang_poly.rule = stripQuotes(varValue); else
					if (varName.equalsIgnoreCase("pplus_spacing")) pplus_spacing.v = TextUtils.atof(varValue); else
					if (varName.equalsIgnoreCase("pplus_spacing_rule")) pplus_spacing.rule = stripQuotes(varValue); else

					if (varName.equalsIgnoreCase("nwell_width")) nwell_width.v = TextUtils.atof(varValue); else
					if (varName.equalsIgnoreCase("nwell_width_rule")) nwell_width.rule = stripQuotes(varValue); else
					if (varName.equalsIgnoreCase("nwell_overhang_diff_p")) nwell_overhang_diff_p.v = TextUtils.atof(varValue); else
					if (varName.equalsIgnoreCase("nwell_overhang_diff_rule_p")) nwell_overhang_diff_p.rule = stripQuotes(varValue); else
					if (varName.equalsIgnoreCase("nwell_overhang_diff_n")) nwell_overhang_diff_n.v = TextUtils.atof(varValue); else
					if (varName.equalsIgnoreCase("nwell_overhang_diff_rule_n")) nwell_overhang_diff_n.rule = stripQuotes(varValue); else
                    if (varName.equalsIgnoreCase("nwell_spacing")) nwell_spacing.v = TextUtils.atof(varValue); else
					if (varName.equalsIgnoreCase("nwell_spacing_rule")) nwell_spacing.rule = stripQuotes(varValue); else

					if (varName.equalsIgnoreCase("metal_width")) fillWizardArray(varValue, metal_width, num_metal_layers, false); else
					if (varName.equalsIgnoreCase("metal_width_rule")) fillWizardArray(varValue, metal_width, num_metal_layers, true); else
					if (varName.equalsIgnoreCase("metal_spacing")) fillWizardArray(varValue, metal_spacing, num_metal_layers, false); else
					if (varName.equalsIgnoreCase("metal_spacing_rule")) fillWizardArray(varValue, metal_spacing, num_metal_layers, true); else
					if (varName.equalsIgnoreCase("via_size")) fillWizardArray(varValue, via_size, num_metal_layers-1, false); else
					if (varName.equalsIgnoreCase("via_size_rule")) fillWizardArray(varValue, via_size, num_metal_layers-1, true); else
					if (varName.equalsIgnoreCase("via_spacing")) fillWizardArray(varValue, via_inline_spacing, num_metal_layers-1, false); else
					if (varName.equalsIgnoreCase("via_spacing_rule")) fillWizardArray(varValue, via_inline_spacing, num_metal_layers-1, true); else
					if (varName.equalsIgnoreCase("via_array_spacing")) fillWizardArray(varValue, via_array_spacing, num_metal_layers-1, false); else
					if (varName.equalsIgnoreCase("via_array_spacing_rule")) fillWizardArray(varValue, via_array_spacing, num_metal_layers-1, true); else
					if (varName.equalsIgnoreCase("via_overhang_inline")) fillWizardArray(varValue, via_overhang, num_metal_layers-1, false); else
					if (varName.equalsIgnoreCase("via_overhang_inline_rule")) fillWizardArray(varValue, via_overhang, num_metal_layers-1, true); else

                    if (varName.equalsIgnoreCase("metal_contacts_series")) fillContactSeries(varValue, metalContacts); else
                    if (varName.equalsIgnoreCase("contacts_series")) fillContactSeries(varValue, otherContacts); else
                    // Special layers
                    if (varName.equalsIgnoreCase("extra_layers")) fillLayerSeries(varValue, extraLayers); else

                    if (varName.equalsIgnoreCase("poly_antenna_ratio")) setPolyAntennaRatio(TextUtils.atof(varValue)); else
					if (varName.equalsIgnoreCase("metal_antenna_ratio")) metal_antenna_ratio = makeDoubleArray(varValue); else

					if (varName.equalsIgnoreCase("gds_diff_layer")) diff_layer.setGDSData(getGDSValuesFromString(varValue)); else
					if (varName.equalsIgnoreCase("gds_poly_layer")) poly_layer.setGDSData(getGDSValuesFromString(varValue)); else
                    if (varName.equalsIgnoreCase("gds_nplus_layer")) nplus_layer.setGDSData(getGDSValuesFromString(varValue)); else
					if (varName.equalsIgnoreCase("gds_pplus_layer")) pplus_layer.setGDSData(getGDSValuesFromString(varValue)); else
					if (varName.equalsIgnoreCase("gds_nwell_layer")) nwell_layer.setGDSData(getGDSValuesFromString(varValue)); else
                    if (varName.equalsIgnoreCase("gds_contact_layer")) contact_layer.setGDSData(getGDSValuesFromString(varValue)); else
					if (varName.equalsIgnoreCase("gds_metal_layer")) metal_layers = setGDSDataArray(varValue, num_metal_layers, "Metal-"); else
					if (varName.equalsIgnoreCase("gds_via_layer")) via_layers = setGDSDataArray(varValue, num_metal_layers - 1, "Via-"); else
                    if (varName.equalsIgnoreCase("gds_marking_layer")) marking_layer.setGDSData(getGDSValuesFromString(varValue));
                    else
                    {
                        WizardField wf = findWizardField(varName);
                        if (wf != null)
                        {
                            fillRule(varValue, wf);
                        }
                        else
                        {
                            Job.getUserInterface().showErrorMessage("Unknown keyword '" + varName + "' on line " + lineReader.getLineNumber(),
                                "Syntax Error In Technology File");
                            break;
                        }
                    }
				}
			}
			lineReader.close();
		} catch (IOException e)
		{
			System.out.println("Error reading " + fileName);
			return false;
		}
		return true;
	}

    private WizardField findWizardField(String varName)
    {
        for (WizardField wf : extraVariables)
        {
            if (wf.name.equals(varName))
                return wf;
        }
        return null;
    }

    private String stripQuotes(String str)
	{
		if (str.startsWith("\"") && str.endsWith("\""))
			return str.substring(1, str.length()-1);
		return str;
	}

	private LayerInfo [] setGDSDataArray(String str, int len, String extra)
	{
		LayerInfo [] foundArray = new LayerInfo[len];
		for(int i=0; i<len; i++) foundArray[i] = new LayerInfo(extra + (i+1));
        StringTokenizer parse = new StringTokenizer(str, "( \")", false);
        int count = 0;
        while (parse.hasMoreTokens())
        {
            if (count >= len)
            {
                System.out.println("More GDS values than metal layers in TechEditWizardData");
                break;
            }
            else
            {
                String value = parse.nextToken();
                // array delimeters must be discarded here because GDS string may
                // contain "," for the pin/text definition ("," can't be used in the StringTokenizer
                if (!value.equals(","))
                    foundArray[count++].setGDSData(getGDSValuesFromString(value));
            }
        }
        return foundArray;
	}

	private double [] makeDoubleArray(String str)
	{
		WizardField [] foundArray = new WizardField[num_metal_layers];
		for(int i=0; i<num_metal_layers; i++) foundArray[i] = new WizardField();
		fillWizardArray(str, foundArray, num_metal_layers, false);
		double [] retArray = new double[foundArray.length];
		for(int i=0; i<foundArray.length; i++)
			retArray[i] = foundArray[i].v;
		return retArray;
	}

	private void fillWizardArray(String str, WizardField [] fieldArray, int expectedLength, boolean getRule)
	{
		if (!str.startsWith("("))
		{
			Job.getUserInterface().showErrorMessage("Array does not start with '(' on " + str,
				"Syntax Error In Technology File");
			return;
		}

		int pos = 1;
		int index = 0;
		for(;;)
		{
			while (pos < str.length() && str.charAt(pos) == ' ') pos++;

            if (index >= fieldArray.length)
            {
//                 Job.getUserInterface().showErrorMessage("Invalid metal index: " + index,
//						"Syntax Error In Technology File");
					return;
            }
            if (getRule)
			{
				if (str.charAt(pos) != '"')
				{
					Job.getUserInterface().showErrorMessage("Rule element does not start with quote on " + str,
						"Syntax Error In Technology File");
					return;
				}
				pos++;
				int end = pos;
				while (end < str.length() && str.charAt(end) != '"') end++;
				if (str.charAt(end) != '"')
				{
					Job.getUserInterface().showErrorMessage("Rule element does not end with quote on " + str,
						"Syntax Error In Technology File");
					return;
				}
				fieldArray[index++].rule = str.substring(pos, end);
				pos = end+1;
			} else
			{
				double v = TextUtils.atof(str.substring(pos));
                fieldArray[index++].v = v;
			}
			while (pos < str.length() && str.charAt(pos) != ',' && str.charAt(pos) != ')') pos++;
			if (str.charAt(pos) != ',') break;
			pos++;
		}
	}

    // fillRule
    private void fillRule(String str, WizardField... rules)
    {
        StringTokenizer parse = new StringTokenizer(str, "(,)", false);
        int count = 0;
        int pos = 0;

        while (parse.hasMoreTokens())
        {
            String value = parse.nextToken();
            switch (count)
            {
                case 0:
                case 2:
                    rules[pos].v = Double.parseDouble(value);
                    break;
                case 1:
                case 3:
                    rules[pos].rule = value;
                    break;
                default:
                    assert(false); // only 2 values
            }
            count++;
            if (count == 2) pos++;
        }
    }

    // fillLayerSeries
    private void fillLayerSeries(String str, List<LayerInfo> layersList)
    {
        StringTokenizer parse = new StringTokenizer(str, "()", false);

        while (parse.hasMoreTokens())
        {
            String value = parse.nextToken();

            if (!value.contains(",")) // only white space
                continue;

            // Sequence ("layer name", "GDS value")
            StringTokenizer p = new StringTokenizer(value, " \"", false);
            int itemCount = 0; // 2 max items: layer name and GDS value
            LayerInfo layer = null;

            while (p.hasMoreTokens())
            {
                String s = p.nextToken();
                if (s.startsWith(",")) continue; // skipping comma. Not in the parser because of the color
                switch (itemCount)
                {
                    case 0:
                        layer = new LayerInfo(s);
                        layersList.add(layer);
                        break;
                    case 1:
                        layer.setGDSData(getGDSValuesFromString(s));
                        break;
                    case 2:
                        layer.setGraphicsTemplate(s);
                        break;
                    default:
                        assert(false);
                }
                itemCount++;
            }
            if (itemCount != 2 && itemCount != 3)
            assert(itemCount == 2 || itemCount == 3);
        }
    }

    // to get general contact
    private void fillContactSeries(String str, Map<String,List<Contact>> contactMap)
    {
        StringTokenizer parse = new StringTokenizer(str, "[]", false);
        List<ContactNode> nodeList = new ArrayList<ContactNode>();

        while (parse.hasMoreTokens())
        {
            String value = parse.nextToken();

            if (value.equals(";")) // end of line
                continue;

            // checking the metal pair lists.
            // overhang values should be in by now
            if (value.contains("{"))
            {
                assert(nodeList.size() > 0);
                int index = value.indexOf("{");
                assert(index != -1); // it should come with a prefix name
                String prefix = value.substring(0, index);
                String v = value.substring(index);
                StringTokenizer p = new StringTokenizer(v, "{}", false);

                while (p.hasMoreTokens())
                {
                    String pair = p.nextToken();
                    // getting metal numbers {a,b,c}
                    StringTokenizer n = new StringTokenizer(pair, ", ", false); // getting the layer names
                    List<String> layerNames = new ArrayList<String>();

                    while (n.hasMoreTokens())
                    {
                        String l = n.nextToken();
                        layerNames.add(l);

                    }
                    assert (nodeList.size() == layerNames.size());
                    Contact cont = new Contact(prefix);
                    for (int i = 0; i < layerNames.size(); i++)
                    {
                        String name = layerNames.get(i);
                        ContactNode tmp = nodeList.get(i);
                        ContactNode node = new ContactNode(name,  tmp.overX.v,  tmp.overX.rule,
                        tmp.overY.v,  tmp.overY.rule);
                        cont.layers.add(node);
                    }

                    String layer1 = layerNames.get(0);
                    String layer2 = layerNames.get(1); // n/p plus regions should go at the end
                    // Always store them by lowMetal-highMetal if happens
                    if (layer1.compareToIgnoreCase(layer2) > 0) // layer1 name is second
                    {
                        String temp = layer1;
                        layer1 = layer2;
                        layer2 = temp;
                    }
                    String key = layer1 + "-" + layer2;
                    List<Contact> l = contactMap.get(key);
                    if (l == null)
                    {
                        l = new ArrayList<Contact>();
                        contactMap.put(key, l);
                    }
                    l.add(cont);
                }
            }
            else
            {
                // syntax: A(overX, overXS, overY, overYS)(Layer2, overX, overXS, overY, overYS)
                // pair of layers found
                StringTokenizer p = new StringTokenizer(value, "()", false);

                while (p.hasMoreTokens())
                {
                    String s = p.nextToken();
                    // layer info
                    int itemCount = 0; // 4 max items: metal layer, overhang X, overhang X rule, overhang Y, overhang Y rule
                    StringTokenizer x = new StringTokenizer(s, ", ", false);
                    double overX = 0, overY = 0;
                    String overXS = null, overYS = null;

                    while (x.hasMoreTokens() && itemCount < 4)
                    {
                        String item = x.nextToken();
                        switch (itemCount)
                        {
                            case 0: // overhang X value
                               overX = Double.valueOf(item);
                                break;
                            case 1: // overhang X rule name
                                overXS = item;
                                break;
                            case 2: // overhang Y value
                                overY = Double.valueOf(item);
                                break;
                            case 3: // overhang Y rule name
                                overYS = item;
                                break;
                        }
                        itemCount++;
                    }
                    assert(itemCount == 4);
                    ContactNode node = new ContactNode("", overX, overXS, overY, overYS);
                    nodeList.add(node);
                }
            }
        }
    }

    /************************************** EXPORT RAW NUMBERS TO DISK **************************************/

	void exportData()
	{
		String fileName = OpenFile.chooseOutputFile(FileType.TEXT, "Technology Wizard File", "Technology.txt");
		if (fileName == null) return;
		try
		{
			PrintWriter printWriter = new PrintWriter(new BufferedWriter(new FileWriter(fileName)));
			dumpNumbers(printWriter);
			printWriter.close();
		} catch (IOException e)
		{
			System.out.println("Error writing XML file");
			return;
		}
	}

	private void dumpNumbers(PrintWriter pw)
	{
        pw.print("#### Electric(tm) VLSI Design System, version ");
        if (User.isIncludeDateAndVersionInOutput())
        {
            pw.println(com.sun.electric.database.text.Version.getVersion());
        } else
        {
            pw.println();
        }
        pw.println("#### ");
        pw.println("#### Technology wizard data file");
		pw.println("####");
		pw.println("#### All dimensions in nanometers.");

        if (IOTool.isUseCopyrightMessage())
        {
            String str = IOTool.getCopyrightMessage();
            int start = 0;
            while (start < str.length())
            {
                int endPos = str.indexOf('\n', start);
                if (endPos < 0) endPos = str.length();
                String oneLine = str.substring(start, endPos);
                pw.println("#### " + oneLine);
                start = endPos+1;
            }
        }

        pw.println();
		pw.println("$tech_name = \"" + tech_name + "\";");
		pw.println("$tech_description = \"" + tech_description + "\";");
		pw.println("$num_metal_layers = " + num_metal_layers + ";");
		pw.println("$pwell_process = " + pWellFlag + ";");
		pw.println("$horizontal_transistors = " + horizontalFlag + ";");
        pw.println("$extra_info = " + extraInfoFlag + ";");
        pw.println();
		pw.println("## stepsize is minimum granularity that will be used as movement grid");
		pw.println("## set to manufacturing grid or lowest common denominator with design rules");
		pw.println("$stepsize = " + stepsize + ";");
        pw.println();
		pw.println("######  DIFFUSION RULES  #####");
		pw.println("$diff_width = " + TextUtils.formatDouble(diff_width.v) + ";");
		pw.println("$diff_width_rule = \"" + diff_width.rule + "\";");
		pw.println("$diff_poly_overhang = " + TextUtils.formatDouble(diff_poly_overhang.v) + ";        # min. diff overhang from gate edge");
		pw.println("$diff_poly_overhang_rule = \"" + diff_poly_overhang.rule + "\";        # min. diff overhang from gate edge");
		pw.println("$diff_contact_overhang = " + TextUtils.formatDouble(diff_contact_overhang.v) + ";     # min. diff overhang contact");
		pw.println("$diff_contact_overhang_rule = \"" + diff_contact_overhang.rule + "\";     # min. diff overhang contact");
		pw.println("$diff_spacing = " + TextUtils.formatDouble(diff_spacing.v) + ";");
		pw.println("$diff_spacing_rule = \"" + diff_spacing.rule + "\";");
		pw.println();
		pw.println("######  POLY RULES  #####");
		pw.println("$poly_width = " + TextUtils.formatDouble(poly_width.v) + ";");
		pw.println("$poly_width_rule = \"" + poly_width.rule + "\";");
		pw.println("$poly_endcap = " + TextUtils.formatDouble(poly_endcap.v) + ";               # min. poly gate extension from edge of diffusion");
		pw.println("$poly_endcap_rule = \"" + poly_endcap.rule + "\";               # min. poly gate extension from edge of diffusion");
		pw.println("$poly_spacing = " + TextUtils.formatDouble(poly_spacing.v) + ";");
		pw.println("$poly_spacing_rule = \"" + poly_spacing.rule + "\";");
		pw.println("$poly_diff_spacing = " + TextUtils.formatDouble(poly_diff_spacing.v) + ";         # min. spacing between poly and diffusion");
		pw.println("$poly_diff_spacing_rule = \"" + poly_diff_spacing.rule + "\";         # min. spacing between poly and diffusion");
        pw.println("$poly_protection_spacing = " + TextUtils.formatDouble(poly_protection_spacing.v) + ";         # min. spacing between poly and dummy poly");
		pw.println("$poly_protection_spacing_rule = \"" + poly_protection_spacing.rule + "\";         # min. spacing between poly and dummy poly");
        pw.println();
		pw.println("######  GATE RULES  #####");
		pw.println("$gate_length = " + TextUtils.formatDouble(gate_length.v) + ";               # min. transistor gate length");
		pw.println("$gate_length_rule = \"" + gate_length.rule + "\";               # min. transistor gate length");
		pw.println("$gate_width = " + TextUtils.formatDouble(gate_width.v) + ";                # min. transistor gate width");
		pw.println("$gate_width_rule = \"" + gate_width.rule + "\";                # min. transistor gate width");
		pw.println("$gate_spacing = " + TextUtils.formatDouble(gate_spacing.v) + ";             # min. gate to gate spacing on diffusion");
		pw.println("$gate_spacing_rule = \"" + gate_spacing.rule + "\";             # min. gate to gate spacing on diffusion");
		pw.println("$gate_contact_spacing = " + TextUtils.formatDouble(gate_contact_spacing.v) + ";      # min. spacing from gate edge to contact inside diffusion");
		pw.println("$gate_contact_spacing_rule = \"" + gate_contact_spacing.rule + "\";      # min. spacing from gate edge to contact inside diffusion");
		pw.println();
		pw.println("######  CONTACT RULES  #####");
		pw.println("$contact_size = " + TextUtils.formatDouble(contact_size.v) + ";");
		pw.println("$contact_size_rule = \"" + contact_size.rule + "\";");
		pw.println("$contact_spacing = " + TextUtils.formatDouble(contact_spacing.v) + ";");
		pw.println("$contact_spacing_rule = \"" + contact_spacing.rule + "\";");
        pw.println("$contact_array_spacing = " + TextUtils.formatDouble(contact_array_spacing.v) + ";");
		pw.println("$contact_array_spacing_rule = \"" + contact_array_spacing.rule + "\";");
        pw.println("$contact_metal_overhang_inline_only = " + TextUtils.formatDouble(contact_metal_overhang_inline_only.v) + ";      # metal overhang when overhanging contact from two sides only");
		pw.println("$contact_metal_overhang_inline_only_rule = \"" + contact_metal_overhang_inline_only.rule + "\";      # metal overhang when overhanging contact from two sides only");

        pw.println("$contact_metal_overhang_all_sides = " + TextUtils.formatDouble(contact_metal_overhang_all_sides.v) + ";         # metal overhang when surrounding contact");
		pw.println("$contact_metal_overhang_all_sides_rule = \"" + contact_metal_overhang_all_sides.rule + "\";         # metal overhang when surrounding contact");
		pw.println("$contact_poly_overhang = " + TextUtils.formatDouble(contact_poly_overhang.v) + ";                    # poly overhang contact, recommended value");
		pw.println("$contact_poly_overhang_rule = \"" + contact_poly_overhang.rule + "\";                    # poly overhang contact, recommended value");
		pw.println("$polycon_diff_spacing = " + TextUtils.formatDouble(polycon_diff_spacing.v) + ";                    # spacing between poly-metal contact edge and diffusion");
		pw.println("$polycon_diff_spacing_rule = \"" + polycon_diff_spacing.rule + "\";                    # spacing between poly-metal contact edge and diffusion");
		pw.println();
		pw.println("######  WELL AND IMPLANT RULES  #####");
		pw.println("$nplus_width = " + TextUtils.formatDouble(nplus_width.v) + ";");
		pw.println("$nplus_width_rule = \"" + nplus_width.rule + "\";");
		pw.println("$nplus_overhang_diff = " + TextUtils.formatDouble(nplus_overhang_diff.v) + ";");
		pw.println("$nplus_overhang_diff_rule = \"" + nplus_overhang_diff.rule + "\";");
		pw.println("$nplus_overhang_poly = " + TextUtils.formatDouble(nplus_overhang_poly.v) + ";");
		pw.println("$nplus_overhang_poly_rule = \"" + nplus_overhang_poly.rule + "\";");
		pw.println("$nplus_spacing = " + TextUtils.formatDouble(nplus_spacing.v) + ";");
		pw.println("$nplus_spacing_rule = \"" + nplus_spacing.rule + "\";");
		pw.println();
		pw.println("$pplus_width = " + TextUtils.formatDouble(pplus_width.v) + ";");
		pw.println("$pplus_width_rule = \"" + pplus_width.rule + "\";");
		pw.println("$pplus_overhang_diff = " + TextUtils.formatDouble(pplus_overhang_diff.v) + ";");
		pw.println("$pplus_overhang_diff_rule = \"" + pplus_overhang_diff.rule + "\";");
        pw.println("$pplus_overhang_poly = " + TextUtils.formatDouble(pplus_overhang_poly.v) + ";");
		pw.println("$pplus_overhang_poly_rule = \"" + pplus_overhang_poly.rule + "\";");
        pw.println("$pplus_spacing = " + TextUtils.formatDouble(pplus_spacing.v) + ";");
		pw.println("$pplus_spacing_rule = \"" + pplus_spacing.rule + "\";");
		pw.println();
		pw.println("$nwell_width = " + TextUtils.formatDouble(nwell_width.v) + ";");
		pw.println("$nwell_width_rule = \"" + nwell_width.rule + "\";");
		pw.println("$nwell_overhang_diff_p = " + TextUtils.formatDouble(nwell_overhang_diff_p.v) + ";");
		pw.println("$nwell_overhang_diff_rule_p = \"" + nwell_overhang_diff_p.rule + "\";");
        pw.println("$nwell_overhang_diff_n = " + TextUtils.formatDouble(nwell_overhang_diff_n.v) + ";");
		pw.println("$nwell_overhang_diff_rule_n = \"" + nwell_overhang_diff_n.rule + "\";");
        pw.println("$nwell_spacing = " + TextUtils.formatDouble(nwell_spacing.v) + ";");
		pw.println("$nwell_spacing_rule = \"" + nwell_spacing.rule + "\";");
		pw.println();
		pw.println("######  METAL RULES  #####");
		pw.print("@metal_width = (");
		for(int i=0; i<num_metal_layers; i++)
		{
			if (i > 0) pw.print(", ");
			pw.print(TextUtils.formatDouble(metal_width[i].v));
		}
		pw.println(");");
		pw.print("@metal_width_rule = (");
		for(int i=0; i<num_metal_layers; i++)
		{
			if (i > 0) pw.print(", ");
			pw.print("\"" + metal_width[i].rule + "\"");
		}
		pw.println(");");
		pw.print("@metal_spacing = (");
		for(int i=0; i<num_metal_layers; i++)
		{
			if (i > 0) pw.print(", ");
			pw.print(TextUtils.formatDouble(metal_spacing[i].v));
		}
		pw.println(");");
		pw.print("@metal_spacing_rule = (");
		for(int i=0; i<num_metal_layers; i++)
		{
			if (i > 0) pw.print(", ");
			pw.print("\"" + metal_spacing[i].rule + "\"");
		}
		pw.println(");");
		pw.println();
		pw.println("######  VIA RULES  #####");
		pw.print("@via_size = (");
		for(int i=0; i<num_metal_layers-1; i++)
		{
			if (i > 0) pw.print(", ");
			pw.print(TextUtils.formatDouble(via_size[i].v));
		}
		pw.println(");");
		pw.print("@via_size_rule = (");
		for(int i=0; i<num_metal_layers-1; i++)
		{
			if (i > 0) pw.print(", ");
			pw.print("\"" + via_size[i].rule + "\"");
		}
		pw.println(");");
		pw.print("@via_spacing = (");
		for(int i=0; i<num_metal_layers-1; i++)
		{
			if (i > 0) pw.print(", ");
			pw.print(TextUtils.formatDouble(via_inline_spacing[i].v));
		}
		pw.println(");");
		pw.print("@via_spacing_rule = (");
		for(int i=0; i<num_metal_layers-1; i++)
		{
			if (i > 0) pw.print(", ");
			pw.print("\"" + via_inline_spacing[i].rule + "\"");
		}
		pw.println(");");
		pw.println();
		pw.println("## \"sep2d\" spacing, close proximity via array spacing");
		pw.print("@via_array_spacing = (");
		for(int i=0; i<num_metal_layers-1; i++)
		{
			if (i > 0) pw.print(", ");
			pw.print(TextUtils.formatDouble(via_array_spacing[i].v));
		}
		pw.println(");");
		pw.print("@via_array_spacing_rule = (");
		for(int i=0; i<num_metal_layers-1; i++)
		{
			if (i > 0) pw.print(", ");
			pw.print("\"" + via_array_spacing[i].rule + "\"");
		}
		pw.println(");");
		pw.print("@via_overhang_inline = (");
		for(int i=0; i<num_metal_layers-1; i++)
		{
			if (i > 0) pw.print(", ");
			pw.print(TextUtils.formatDouble(via_overhang[i].v));
		}
		pw.println(");");
		pw.print("@via_overhang_inline_rule = (");
		for(int i=0; i<num_metal_layers-1; i++)
		{
			if (i > 0) pw.print(", ");
			pw.print("\"" + via_overhang[i].rule + "\"");
		}
		pw.println(");");
		pw.println();
		pw.println("######  ANTENNA RULES  #####");
		pw.println("$poly_antenna_ratio = " + TextUtils.formatDouble(poly_antenna_ratio) + ";");
		pw.print("@metal_antenna_ratio = (");
		for(int i=0; i<num_metal_layers; i++)
		{
			if (i > 0) pw.print(", ");
			pw.print(TextUtils.formatDouble(metal_antenna_ratio[i]));
		}
		pw.println(");");
		pw.println();
		pw.println("######  GDS-II LAYERS  #####");
		pw.println("$gds_diff_layer = " + diff_layer + ";");
		pw.println("$gds_poly_layer = " + poly_layer + ";");
		pw.println("$gds_nplus_layer = " + nplus_layer + ";");
		pw.println("$gds_pplus_layer = " + pplus_layer + ";");
		pw.println("$gds_nwell_layer = " + nwell_layer + ";");
        pw.println("$gds_contact_layer = " + contact_layer + ";");
		pw.print("@gds_metal_layer = (");
		for(int i=0; i<num_metal_layers; i++)
		{
			if (i > 0) pw.print(", ");
			pw.print(metal_layers[i]);
		}
		pw.println(");");
		pw.print("@gds_via_layer = (");
		for(int i=0; i<num_metal_layers-1; i++)
		{
			if (i > 0) pw.print(", ");
			pw.print(via_layers[i]);
		}
		pw.println(");");
		pw.println();
		pw.println("## Device marking layer");
		pw.println("$gds_marking_layer = " + marking_layer + ";");
		pw.println();
		pw.println("# End of techfile");
	}

    /************************************** WRITE XML FILE **************************************/

	void writeXML()
	{
		String errorMessage = errorInData();
		if (errorMessage != null)
		{
			Job.getUserInterface().showErrorMessage("ERROR: " + errorMessage,
				"Missing Technology Data");
			return;
		}
        String suggestedName = getTechName() + ".xml";
        String fileName = OpenFile.chooseOutputFile(FileType.XML, "Technology XML File", suggestedName); //"Technology.xml");
		if (fileName == null) return;
		try
		{
            dumpXMLFile(fileName);
		} catch (IOException e)
		{
			System.out.println("Error writing XML file");
			return;
		}
	}

    /**
     * Method to create the XML version of a PrimitiveNode representing a pin
     * @return
     */
    private Xml.PrimitiveNodeGroup makeXmlPrimitivePin(Xml.Technology t, String name, double size,
                                                       SizeOffset so, Xml.NodeLayer... list)
    {
        List<Xml.NodeLayer> nodesList = new ArrayList<Xml.NodeLayer>(list.length);
        List<Xml.PrimitivePort> nodePorts = new ArrayList<Xml.PrimitivePort>();
        List<String> portNames = new ArrayList<String>();

        for (Xml.NodeLayer lb : list)
        {
            if (lb == null) continue; // in case the pwell layer off
            nodesList.add(lb);
        }

        portNames.add(name);
        nodePorts.add(makeXmlPrimitivePort(name.toLowerCase(), 0, 180, 0, null, 0, -1, 0, 1, 0, -1, 0, 1, portNames));
        Xml.PrimitiveNodeGroup n = makeXmlPrimitive(t.nodeGroups, name + "-Pin", PrimitiveNode.Function.PIN, size, size, 0, 0,
                so, nodesList, nodePorts, null, true);
        return n;
    }

    /**
     * Method to creat the XML version of a PrimitiveNode representing a contact
     * @return
     */
    private Xml.PrimitiveNodeGroup makeXmlPrimitiveCon(List<Xml.PrimitiveNodeGroup> nodeGroups, String name,
                                                       PrimitiveNode.Function function, double sizeX, double sizeY,
                                                       SizeOffset so, List<String> portNames, Xml.NodeLayer... list)
    {
        List<Xml.NodeLayer> nodesList = new ArrayList<Xml.NodeLayer>(list.length);
        List<Xml.PrimitivePort> nodePorts = new ArrayList<Xml.PrimitivePort>();

        for (Xml.NodeLayer lb : list)
        {
            if (lb == null) continue; // in case the pwell layer off
            nodesList.add(lb);
        }

        nodePorts.add(makeXmlPrimitivePort(name.toLowerCase(), 0, 180, 0, null, 0, -1, 0, 1, 0, -1, 0, 1, portNames));
        return makeXmlPrimitive(nodeGroups, name + "-Con", function, sizeX, sizeY, 0, 0,
                so, nodesList, nodePorts, null, false);
    }

    /**
     * Method to create the XML version of a PrimitiveNode
     * @return
     */
    private Xml.PrimitiveNodeGroup makeXmlPrimitive(List<Xml.PrimitiveNodeGroup> nodeGroups,
                                                    String name, PrimitiveNode.Function function,
                                                    double width, double height,
                                                    double ppLeft, double ppBottom,
                                                    SizeOffset so, List<Xml.NodeLayer> nodeLayers,
                                                    List<Xml.PrimitivePort> nodePorts,
                                                    PrimitiveNode.NodeSizeRule nodeSizeRule, boolean isArcsShrink)
    {
        Xml.PrimitiveNodeGroup ng = new Xml.PrimitiveNodeGroup();
        ng.isSingleton = true;
        Xml.PrimitiveNode n = new Xml.PrimitiveNode();
        n.name = name;
        n.function = function;
        ng.nodes.add(n);

        ng.shrinkArcs = isArcsShrink;
//        n.square = isSquare();
//        n.canBeZeroSize = isCanBeZeroSize();
//        n.wipes = isWipeOn1or2();
//        n.lockable = isLockedPrim();
//        n.edgeSelect = isEdgeSelect();
//        n.skipSizeInPalette = isSkipSizeInPalette();
//        n.notUsed = isNotUsed();
//        n.lowVt = isNodeBitOn(PrimitiveNode.LOWVTBIT);
//        n.highVt = isNodeBitOn(PrimitiveNode.HIGHVTBIT);
//        n.nativeBit  = isNodeBitOn(PrimitiveNode.NATIVEBIT);
//        n.od18 = isNodeBitOn(PrimitiveNode.OD18BIT);
//        n.od25 = isNodeBitOn(PrimitiveNode.OD25BIT);
//        n.od33 = isNodeBitOn(PrimitiveNode.OD33BIT);

//        PrimitiveNode.NodeSizeRule nodeSizeRule = getMinSizeRule();
//        EPoint minFullSize = nodeSizeRule != null ?
//            EPoint.fromLambda(0.5*nodeSizeRule.getWidth(), 0.5*nodeSizeRule.getHeight()) :
//            EPoint.fromLambda(0.5*getDefWidth(), 0.5*getDefHeight());
//         EPoint minFullSize = EPoint.fromLambda(0.5*width, 0.5*height);
        EPoint topLeft = EPoint.fromLambda(ppLeft, ppBottom + height);
        EPoint size =  EPoint.fromLambda(width, height);

        double getDefWidth = width, getDefHeight = height;
        if (function == PrimitiveNode.Function.PIN && isArcsShrink) {
//            assert getNumPorts() == 1;
//            assert nodeSizeRule == null;
//            PrimitivePort pp = getPort(0);
//            assert pp.getLeft().getMultiplier() == -0.5 && pp.getRight().getMultiplier() == 0.5 && pp.getBottom().getMultiplier() == -0.5 && pp.getTop().getMultiplier() == 0.5;
//            assert pp.getLeft().getAdder() == -pp.getRight().getAdder() && pp.getBottom().getAdder() == -pp.getTop().getAdder();
//            minFullSize = EPoint.fromLambda(ppLeft, ppBottom);
        }
//            DRCTemplate nodeSize = xmlRules.getRule(pnp.getPrimNodeIndexInTech(), DRCTemplate.DRCRuleType.NODSIZ);
//        SizeOffset so = getProtoSizeOffset();
        if (so != null &&
            (so.getLowXOffset() == 0 && so.getHighXOffset() == 0 &&
                so.getLowYOffset() == 0 && so.getHighYOffset() == 0))
            so = null;

            ERectangle base = calcBaseRectangle(so, nodeLayers, nodeSizeRule);
            ng.baseLX.value = base.getLambdaMinX();
            ng.baseHX.value = base.getLambdaMaxX();
            ng.baseLY.value = base.getLambdaMinY();
            ng.baseHY.value = base.getLambdaMaxY();
//            n.sizeOffset = so;

//        if (!minFullSize.equals(EPoint.ORIGIN))
//            n.diskOffset = minFullSize;
//        if (so != null) {
//            EPoint p2 = EPoint.fromGrid(
//                    minFullSize.getGridX() - ((so.getLowXGridOffset() + so.getHighXGridOffset()) >> 1),
//                    minFullSize.getGridY() - ((so.getLowYGridOffset() + so.getHighYGridOffset()) >> 1));
//            n.diskOffset.put(Integer.valueOf(1), minFullSize);
//            n.diskOffset.put(Integer.valueOf(2), p2);
//            n.diskOffset.put(Integer.valueOf(2), minFullSize);
//        }
//        n.defaultWidth.addLambda(DBMath.round(getDefWidth)); // - 2*minFullSize.getLambdaX());
//        n.defaultHeight.addLambda(DBMath.round(getDefHeight)); // - 2*minFullSize.getLambdaY());
        ERectangle baseRectangle = ERectangle.fromGrid(topLeft.getGridX(), topLeft.getGridY(),
            size.getGridX(), size.getGridY());
/*        n.nodeBase = baseRectangle;*/

//        List<Technology.NodeLayer> nodeLayers = Arrays.asList(getLayers());
//        List<Technology.NodeLayer> electricalNodeLayers = nodeLayers;
//        if (getElectricalLayers() != null)
//            electricalNodeLayers = Arrays.asList(getElectricalLayers());
        boolean isSerp = false; //getSpecialType() == PrimitiveNode.SERPTRANS;
        if (nodeLayers != null)
            ng.nodeLayers.addAll(nodeLayers);
//        int m = 0;
//        for (Technology.NodeLayer nld: electricalNodeLayers) {
//            int j = nodeLayers.indexOf(nld);
//            if (j < 0) {
//                n.nodeLayers.add(nld.makeXml(isSerp, minFullSize, false, true));
//                continue;
//            }
//            while (m < j)
//                n.nodeLayers.add(nodeLayers.get(m++).makeXml(isSerp, minFullSize, true, false));
//            n.nodeLayers.add(nodeLayers.get(m++).makeXml(isSerp, minFullSize, true, true));
//        }
//        while (m < nodeLayers.size())
//            n.nodeLayers.add(nodeLayers.get(m++).makeXml(isSerp, minFullSize, true, false));

//        for (Iterator<PrimitivePort> pit = getPrimitivePorts(); pit.hasNext(); ) {
//            PrimitivePort pp = pit.next();
//            n.ports.add(pp.makeXml(minFullSize));
//        }
        ng.specialType = PrimitiveNode.NORMAL; // getSpecialType();
//        if (getSpecialValues() != null)
//            n.specialValues = getSpecialValues().clone();
        if (nodeSizeRule != null) {
            ng.nodeSizeRule = new Xml.NodeSizeRule();
            ng.nodeSizeRule.width = nodeSizeRule.getWidth();
            ng.nodeSizeRule.height = nodeSizeRule.getHeight();
            ng.nodeSizeRule.rule = nodeSizeRule.getRuleName();
        }
//        n.spiceTemplate = "";//getSpiceTemplate();

        // ports
        ng.ports.addAll(nodePorts);

        nodeGroups.add(ng);

        return ng;
    }

    private ERectangle calcBaseRectangle(SizeOffset so, List<Xml.NodeLayer> nodeLayers, PrimitiveNode.NodeSizeRule nodeSizeRule) {
        long lx, hx, ly, hy;
        if (nodeSizeRule != null) {
            hx = DBMath.lambdaToGrid(0.5*nodeSizeRule.getWidth());
            lx = -hx;
            hy = DBMath.lambdaToGrid(0.5*nodeSizeRule.getHeight());
            ly = -hy;
        } else {
            lx = Long.MAX_VALUE;
            hx = Long.MIN_VALUE;
            ly = Long.MAX_VALUE;
            hy = Long.MIN_VALUE;
            for (int i = 0; i < nodeLayers.size(); i++) {
                Xml.NodeLayer nl = nodeLayers.get(i);
                long x, y;
                if (nl.representation == Technology.NodeLayer.BOX || nl.representation == Technology.NodeLayer.MULTICUTBOX) {
                    x = DBMath.lambdaToGrid(nl.lx.value);
                    lx = Math.min(lx, x);
                    hx = Math.max(hx, x);
                    x = DBMath.lambdaToGrid(nl.hx.value);
                    lx = Math.min(lx, x);
                    hx = Math.max(hx, x);
                    y = DBMath.lambdaToGrid(nl.ly.value);
                    ly = Math.min(ly, y);
                    hy = Math.max(hy, y);
                    y = DBMath.lambdaToGrid(nl.hy.value);
                    ly = Math.min(ly, y);
                    hy = Math.max(hy, y);
                } else {
                    for (Technology.TechPoint p: nl.techPoints) {
                        x = p.getX().getGridAdder();
                        lx = Math.min(lx, x);
                        hx = Math.max(hx, x);
                        y = p.getY().getGridAdder();
                        ly = Math.min(ly, y);
                        hy = Math.max(hy, y);
                    }
                }
            }
        }
        if (so != null) {
            lx += so.getLowXGridOffset();
            hx -= so.getHighXGridOffset();
            ly += so.getLowYGridOffset();
            hy -= so.getHighYGridOffset();
        }
        return ERectangle.fromGrid(lx, ly, hx - lx, hy - ly);
    }

    /**
     * Method to create the XML version of a ArcProto
     * @param name
     * @param function
     * @return
     */
    private Xml.ArcProto makeXmlArc(Xml.Technology t, String name, com.sun.electric.technology.ArcProto.Function function,
                                    double ant, Xml.ArcLayer ... arcLayers)
    {
        Xml.ArcProto a = new Xml.ArcProto();
        a.name = name;
        a.function = function;
        a.wipable = true;
//        a.curvable = false;
//        a.special = false;
//        a.notUsed = false;
//        a.skipSizeInPalette = false;

//        a.elibWidthOffset = getLambdaElibWidthOffset();
        a.extended = true;
        a.fixedAngle = true;
        a.angleIncrement = 90;
        a.antennaRatio = DBMath.round(ant);

        for (Xml.ArcLayer al: arcLayers)
        {
            if (al == null) continue; // in case the pwell layer off
            a.arcLayers.add(al);
        }
        t.arcs.add(a);
        return a;
    }

    private Xml.Layer makeXmlLayer(List<Xml.Layer> layers, Map<Xml.Layer, WizardField> layer_width, String name,
                                   Layer.Function function, int extraf, EGraphics graph,
                                   WizardField width, boolean pureLayerNode, boolean pureLayerPortArc)
    {
        Xml.Layer l = makeXmlLayer(layers, name, function, extraf, graph, width.v, pureLayerNode, pureLayerPortArc);
        layer_width.put(l, width);
        return l;
    }

    /**
     * Method to create the XML version of a Layer.
     * @return
     */
    private Xml.Layer makeXmlLayer(List<Xml.Layer> layers, String name,
                                   Layer.Function function, int extraf, EGraphics graph,
                                   double width, boolean pureLayerNode, boolean pureLayerPortArc)
    {
        Xml.Layer l = new Xml.Layer();
        l.name = name;
        l.function = function;
        l.extraFunction = extraf;
        graph = graph.withTransparencyMode(EGraphics.J3DTransparencyOption.NONE);
        graph = graph.withTransparencyFactor(1);
        l.desc = graph;
        l.thick3D = 1;
        l.height3D = 1;
        l.cif = "Not set"; //"C" + cifLetter + cifLetter;
        l.skill = name;
        l.resistance = 1;
        l.capacitance = 0;
        l.edgeCapacitance = 0;
//            if (layer.getPseudoLayer() != null)
//                l.pseudoLayer = layer.getPseudoLayer().getName();

        // if pureLayerNode is false, pureLayerPortArc must be false
        assert(pureLayerNode || !pureLayerPortArc);

        if (pureLayerNode) {
            l.pureLayerNode = new Xml.PureLayerNode();
            l.pureLayerNode.name = name + "-Node";
            l.pureLayerNode.style = Poly.Type.FILLED;
            l.pureLayerNode.size.addLambda(scaledValue(width));
            l.pureLayerNode.port = "Port_" + name;
/*            l.pureLayerNode.size.addRule(width.rule, 1);*/
            if (pureLayerPortArc)
                l.pureLayerNode.portArcs.add(name);
//            for (ArcProto ap: pureLayerNode.getPort(0).getConnections()) {
//                if (ap.getTechnology() != tech) continue;
//                l.pureLayerNode.portArcs.add(ap.getName());
//            }
        }
        layers.add(l);
        return l;
    }

    /**
     * Method to create the XML version of NodeLayer
     */
    private Xml.NodeLayer makeXmlNodeLayer(double lx, double hx, double ly, double hy, Xml.Layer lb, Poly.Type style)
    {
        return makeXmlNodeLayer(lx, hx, ly, hy, lb, style, true, true, 0);
    }

    /**
     * Method to create the XML version of NodeLayer either graphical or electrical.
     * makeXmlNodeLayer is the default one where layer is available in both mode.
     */
    private Xml.NodeLayer makeXmlNodeLayer(double lx, double hx, double ly, double hy, Xml.Layer lb, Poly.Type style,
                                           boolean inLayers, boolean electricalLayers, int port)
    {
        Xml.NodeLayer nl = new Xml.NodeLayer();
        nl.layer = lb.name;
        nl.style = style;
        nl.portNum = port;
        nl.inLayers = inLayers;
        nl.inElectricalLayers = electricalLayers;
        nl.representation = Technology.NodeLayer.BOX;
        nl.lx.k = -1; nl.hx.k = 1; nl.ly.k = -1; nl.hy.k = 1;
        nl.lx.addLambda(-lx); nl.hx.addLambda(hx); nl.ly.addLambda(-ly); nl.hy.addLambda(hy);
        return nl;
    }

    /**
     * Method to create the default XML version of a MultiCUt NodeLayer
     * @return
     */
    private Xml.NodeLayer makeXmlMulticut(Xml.Layer lb, double sizeRule, double sepRule, double sepRule2D)
    {
        return makeXmlMulticut(0, 0, 0, 0, lb, sizeRule, sepRule, sepRule2D);
    }

    /**
     * Method to create the default XML version of a MultiCUt NodeLayer
     * @return
     */
    private Xml.NodeLayer makeXmlMulticut(double lx, double hx, double ly, double hy, Xml.Layer lb,
                                          double sizeRule, double sepRule, double sepRule2D)
    {
        Xml.NodeLayer nl = new Xml.NodeLayer();
        nl.layer = lb.name;
        nl.style = Poly.Type.FILLED;
        nl.inLayers = nl.inElectricalLayers = true;
        nl.representation = Technology.NodeLayer.MULTICUTBOX;
        nl.lx.k = -1; nl.hx.k = 1; nl.ly.k = -1; nl.hy.k = 1;
        nl.lx.addLambda(-lx); nl.hx.addLambda(hx); nl.ly.addLambda(-ly); nl.hy.addLambda(hy);

//        nl.sizeRule = sizeRule;
        nl.sizex = sizeRule;
        nl.sizey = sizeRule;
        nl.sep1d = sepRule;
        nl.sep2d = sepRule2D;
        return nl;

    }

    /**
     * Method to create the XML versio nof PrimitivePort
     * @return New Xml.PrimitivePort
     */
    private Xml.PrimitivePort makeXmlPrimitivePort(String name, int portAngle, int portRange, int portTopology,
                                                   EPoint minFullSize,
                                                   double lx, int slx, double hx, int shx,
                                                   double ly, int sly, double hy, int shy, List<String> portArcs)
    {
        Xml.PrimitivePort ppd = new Xml.PrimitivePort();
        double lambdaX = (minFullSize != null) ? minFullSize.getLambdaX() : 0;
        double lambdaY = (minFullSize != null) ? minFullSize.getLambdaY() : 0;
        ppd.name = name;
        ppd.portAngle = portAngle;
        ppd.portRange = portRange;
        ppd.portTopology = portTopology;

        ppd.lx.k = slx;//-1; //getLeft().getMultiplier()*2;
        ppd.lx.addLambda(DBMath.round(lx + lambdaX*ppd.lx.k));
        ppd.hx.k = shx;//1; //getRight().getMultiplier()*2;
        ppd.hx.addLambda(DBMath.round(hx + lambdaX*ppd.hx.k));
        ppd.ly.k = sly;//-1; // getBottom().getMultiplier()*2;
        ppd.ly.addLambda(DBMath.round(ly + lambdaY*ppd.ly.k));
        ppd.hy.k = shy;//1; // getTop().getMultiplier()*2;
        ppd.hy.addLambda(DBMath.round(hy + lambdaY*ppd.hy.k));

        if (portArcs != null) {
            for (String s: portArcs)
            {
                ppd.portArcs.add(s);
            }
        }
        return ppd;
    }

    /**
     * To create zero, cross, aligned and squared contacts from the same set of rules
     */
    private Xml.PrimitiveNodeGroup makeContactSeries(List<Xml.PrimitiveNodeGroup> nodeGroups, String composeName,
                                           double contSize, Xml.Layer conLayer, double spacing, double arraySpacing,
                                           double extLayer1, Xml.Layer layer1,
                                           double extLayer2, Xml.Layer layer2)
    {
        List<String> portNames = new ArrayList<String>();

        portNames.add(layer1.name);
        portNames.add(layer2.name);

        // align contact
        double hlaLong1 = DBMath.round(contSize/2 + extLayer1);
        double hlaLong2 = DBMath.round(contSize/2 + extLayer2);
        double longD = DBMath.isGreaterThan(extLayer1, extLayer2) ? extLayer1 : extLayer2;

        // long square contact. Standard ones
        return (makeXmlPrimitiveCon(nodeGroups, composeName, PrimitiveNode.Function.CONTACT, -1, -1,
            null, /*new SizeOffset(longD, longD, longD, longD),*/ portNames,
                makeXmlNodeLayer(hlaLong1, hlaLong1, hlaLong1, hlaLong1, layer1, Poly.Type.FILLED), // layer1
                makeXmlNodeLayer(hlaLong2, hlaLong2, hlaLong2, hlaLong2, layer2, Poly.Type.FILLED), // layer2
                makeXmlMulticut(conLayer, contSize, spacing, arraySpacing))); // contact
    }

    /**
     * Leave as oublic for the regression.
     * @param fileName
     * @throws IOException
     */
    public void dumpXMLFile(String fileName)
        throws IOException
    {
        Xml.Technology t = new Xml.Technology();

        t.techName = getTechName();
        t.shortTechName = getTechName();
        t.description = getTechDescription();
        t.minNumMetals = t.maxNumMetals = t.defaultNumMetals = getNumMetalLayers();
        t.scaleValue = getStepSize();
        t.scaleRelevant = true;
        t.resolutionValue = getResolution();
//        t.scaleRelevant = isScaleRelevant();
        t.defaultFoundry = "NONE";
        t.minResistance = 1.0;
        t.minCapacitance = 0.1;
        // menus
        t.menuPalette = new Xml.MenuPalette();
        t.menuPalette.numColumns = 3;

        /** RULES **/
        Xml.Foundry f = new Xml.Foundry();
        f.name = Foundry.Type.NONE.getName();
        t.foundries.add(f);

        // LAYER COLOURS
		Color [] metal_colour = new Color[]
		{
			new Color(0,150,255),   // cyan/blue
			new Color(148,0,211),   // purple
			new Color(255,215,0),   // yellow
			new Color(132,112,255), // mauve
			new Color(255,160,122), // salmon
			new Color(34,139,34),   // dull green
			new Color(178,34,34),   // dull red
			new Color(34,34,178),   // dull blue
			new Color(153,153,153), // light gray
			new Color(102,102,102)  // dark gray
		};
		Color poly_colour = new Color(255,155,192);   // pink
		Color diff_colour = new Color(107,226,96);    // light green
		Color via_colour = new Color(205,205,205);    // lighter gray
		Color contact_colour = new Color(100,100,100);   // darker gray
        Color nplus_colour = new Color(224,238,224);
		Color pplus_colour = new Color(224,224,120);
		Color nwell_colour = new Color(140,140,140);
        // Five transparent colors: poly_colour, diff_colour, metal_colour[0->2]
        Color[] colorMap = {poly_colour, diff_colour, metal_colour[0], metal_colour[1], metal_colour[2]};
        for (int i = 0; i < colorMap.length; i++) {
            Color transparentColor = colorMap[i];
            t.transparentLayers.add(transparentColor);
        }

        // Layers
        List<Xml.Layer> metalLayers = new ArrayList<Xml.Layer>();
        List<Xml.Layer> dummyMetalLayers = new ArrayList<Xml.Layer>();
        List<Xml.Layer> exclusionMetalLayers = new ArrayList<Xml.Layer>();
        List<Xml.Layer> viaLayers = new ArrayList<Xml.Layer>();
        Map<Xml.Layer,WizardField> layer_width = new LinkedHashMap<Xml.Layer,WizardField>();
        int[] nullPattern = new int[] {0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
            0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000};
        int[] dexclPattern = new int[] {
                        0x1010,   //    X       X
                        0x2020,   //   X       X
                        0x4040,   //  X       X
                        0x8080,   // X       X
                        0x4040,   //  X       X
                        0x2020,   //   X       X
                        0x1010,   //    X       X
                        0x0808,   //     X       X
                        0x1010,   //    X       X
                        0x2020,   //   X       X
                        0x4040,   //  X       X
                        0x8080,   // X       X
                        0x4040,   //  X       X
                        0x2020,   //   X       X
                        0x1010,   //    X       X
                        0x0808}; //     X       X

        for (int i = 0; i < num_metal_layers; i++)
        {
            // Adding the metal
            int metalNum = i + 1;
            double opacity = (75 - metalNum * 5)/100.0;
            int metLayHigh = i / 10;
            int metLayDig = i % 10;
            int r = metal_colour[metLayDig].getRed() * (10-metLayHigh) / 10;
            int g = metal_colour[metLayDig].getGreen() * (10-metLayHigh) / 10;
            int b = metal_colour[metLayDig].getBlue() * (10-metLayHigh) / 10;
            int tcol = 0;
            int[] pattern = null;

            switch (metLayDig)
            {
                case 0: tcol = 3;   break;
                case 1: tcol = 4;   break;
                case 2: tcol = 5;   break;
                case 3: pattern = new int[] {0xFFFF,   // XXXXXXXXXXXXXXXX
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
                        0x0000}; break;
                case 4: pattern = new int[] { 0x8888,   // X   X   X   X
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
                        0x4444};
                    break;
                case 5: pattern = new int[] { 0x1111,   //    X   X   X   X
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
                        0x5555};
                    break;
                case 6: pattern =  new int[] { 0x8888,   // X   X   X   X
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
                        0x1111};
                    break;
                case 7: pattern =  new int[] { 0x2222,   //   X   X   X   X
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
                        0x0000};
                    break;
                case 8: pattern =  new int[] {0x0000,   //
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
                        0x0000,   //
                        0x2222,   //   X   X   X   X
                        0x0000,   //
                        0x8888};  // X   X   X   X
                    break;
                case 9: pattern = new int[] { 0x5555,   //  X X X X X X X X
                        0x5555,   //  X X X X X X X X
                        0x5555,   //  X X X X X X X X
                        0x5555,   //  X X X X X X X X
                        0x5555,   //  X X X X X X X X
                        0x5555,   //  X X X X X X X X
                        0x5555,   //  X X X X X X X X
                        0x5555,   //  X X X X X X X X
                        0x5555,   //  X X X X X X X X
                        0x5555,   //  X X X X X X X X
                        0x5555,   //  X X X X X X X X
                        0x5555,   //  X X X X X X X X
                        0x5555,   //  X X X X X X X X
                        0x5555,   //  X X X X X X X X
                        0x5555,   //  X X X X X X X X
                        0x5555};
                    break;
            }
            boolean onDisplay = true, onPrinter = true;

            if (pattern == null)
            {
                pattern = nullPattern;
                onDisplay = false; onPrinter = false;
            }
            EGraphics graph = new EGraphics(onDisplay, onPrinter, null, tcol, r, g, b, opacity, true, pattern);
            Layer.Function fun = Layer.Function.getMetal(metalNum);
            if (fun == null)
                throw new IOException("invalid number of metals");
            String metalName = "Metal-"+metalNum;
            Xml.Layer layer = makeXmlLayer(t.layers, layer_width, metalName, fun, 0, graph,
                metal_width[i], true, true);
            metalLayers.add(layer);

            if (getExtraInfoFlag())
            {
                // dummy layers
                graph = new EGraphics(true, true, null, tcol, r, g, b, opacity, false, nullPattern);
                layer = makeXmlLayer(t.layers, "DMY-"+metalName, Layer.Function.getDummyMetal(metalNum), 0, graph,
                    5*metal_width[i].v, true, false);
                dummyMetalLayers.add(layer);

                // exclusion layers for metals
                graph = new EGraphics(true, true, null, tcol, r, g, b, opacity, true, dexclPattern);
                layer = makeXmlLayer(t.layers, "DEXCL-"+metalName, Layer.Function.getDummyExclMetal(i), 0, graph,
                    2*metal_width[i].v, true, false);
                exclusionMetalLayers.add(layer);
            }
        }

        // Vias
        for (int i = 0; i < num_metal_layers - 1; i++)
        {
            // Adding the metal
            int metalNum = i + 1;
            // adding the via
            int r = via_colour.getRed();
            int g = via_colour.getGreen();
            int b = via_colour.getBlue();
            double opacity = 0.7;
            EGraphics graph = new EGraphics(false, false, null, 0, r, g, b, opacity, true, nullPattern);
            Layer.Function fun = Layer.Function.getContact(metalNum+1); //via contact starts with CONTACT2
            if (fun == null)
                throw new IOException("invalid number of vias");
            viaLayers.add(makeXmlLayer(t.layers, layer_width, "Via-"+metalNum, fun, Layer.Function.CONMETAL,
                graph, via_size[i], true, false));
        }

        // Poly
        String polyN = poly_layer.name;
        EGraphics graph = new EGraphics(false, false, null, 1, 0, 0, 0, 1, true, nullPattern);
        Xml.Layer polyLayer = makeXmlLayer(t.layers, layer_width, polyN, Layer.Function.POLY1, 0, graph,
            poly_width, true, true);
        // PolyGate
        Xml.Layer polyGateLayer = makeXmlLayer(t.layers, layer_width, polyN+"Gate", Layer.Function.GATE, 0, graph,
            poly_width, true, false); // false for the port otherwise it won't find any type

        if (getExtraInfoFlag())
        {
            // exclusion layer poly
            graph = new EGraphics(true, true, null, 1, 0, 0, 0, 1, true, dexclPattern);
            Xml.Layer exclusionPolyLayer = makeXmlLayer(t.layers, "DEXCL-"+polyN, Layer.Function.DEXCLPOLY1, 0, graph,
                2*poly_width.v, true, false);
            makeLayerGDS(t, exclusionPolyLayer, "150/21");
        }

        // PolyCon and DiffCon
        graph = new EGraphics(false, false, null, 0, contact_colour.getRed(), contact_colour.getGreen(),
            contact_colour.getBlue(), 0.5, true, nullPattern);
        // PolyCon
        Xml.Layer polyConLayer = makeXmlLayer(t.layers, layer_width, "Poly-Cut", Layer.Function.CONTACT1,
            Layer.Function.CONPOLY, graph, contact_size, true, false);
        // DiffCon
        Xml.Layer diffConLayer = makeXmlLayer(t.layers, layer_width, diff_layer.name+"-Cut", Layer.Function.CONTACT1,
            Layer.Function.CONDIFF, graph, contact_size, true, false);

        // P-Diff and N-Diff
        graph = new EGraphics(false, false, null, 2, 0, 0, 0, 1, true, nullPattern);
        // N-Diff
        Xml.Layer diffNLayer = makeXmlLayer(t.layers, layer_width, "N-"+ diff_layer.name, Layer.Function.DIFFN, 0, graph,
            diff_width, true, true);
        // P-Diff
        Xml.Layer diffPLayer = makeXmlLayer(t.layers, layer_width, "P-"+ diff_layer.name, Layer.Function.DIFFP, 0, graph,
            diff_width, true, true);

        if (getExtraInfoFlag())
        {
            // exclusion layer N/P diff
            graph = new EGraphics(true, true, null, 2, 0, 0, 0, 1, true, dexclPattern);
            Xml.Layer exclusionDiffPLayer = makeXmlLayer(t.layers, "DEXCL-P-"+ diff_layer.name, Layer.Function.DEXCLDIFF, 0, graph,
                2*diff_width.v, true, false);
            Xml.Layer exclusionDiffNLayer = makeXmlLayer(t.layers, "DEXCL-N-"+ diff_layer.name, Layer.Function.DEXCLDIFF, 0, graph,
                2*diff_width.v, true, false);
            makeLayerGDS(t, exclusionDiffPLayer, "150/20");
            makeLayerGDS(t, exclusionDiffNLayer, "150/20");
        }

        // NPlus and PPlus
        int [] patternSlash = new int[] { 0x1010,   //    X       X
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
                        0x0808};

        int [] patternBackSlash = new int[] { 0x0202,   //       X       X
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
                        0x0404};

        int[] patternDots = new int[] {
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
                        0x0000,   //
                        0x0202,   //       X       X
                        0x0000,   //
                        0x2020,   //   X       X
                        0x0000};   //

        int[] patternSparseDots = new int[] {
                        0x0200,   //       X
                        0x0000,   //
                        0x0020,   //           X
                        0x0000,   //
                        0x0002,   //               X
                        0x0000,   //
                        0x2000,   //   X
                        0x0000,   //
                        0x0200,   //       X
                        0x0000,   //
                        0x0020,   //           X
                        0x0000,   //
                        0x0002,   //               X
                        0x0000,   //
                        0x2000,   //   X
                        0x0000};   //

        // NPlus
        graph = new EGraphics(true, true, null, 0, nplus_colour.getRed(), nplus_colour.getGreen(),
            nplus_colour.getBlue(), 1, true, patternSlash);
        Xml.Layer nplusLayer = makeXmlLayer(t.layers, layer_width, nplus_layer.name, Layer.Function.IMPLANTN, 0, graph,
            nplus_width, true, false);
        // PPlus
        graph = new EGraphics(true, true, null, 0, pplus_colour.getRed(), pplus_colour.getGreen(),
            pplus_colour.getBlue(), 1, true, patternDots);
        Xml.Layer pplusLayer = makeXmlLayer(t.layers, layer_width, pplus_layer.name, Layer.Function.IMPLANTP, 0, graph,
            pplus_width, true, false);

        // N-Well
        graph = new EGraphics(true, true, null, 0, nwell_colour.getRed(), nwell_colour.getGreen(),
            nwell_colour.getBlue(), 1, true, patternDots);
        Xml.Layer nwellLayer = makeXmlLayer(t.layers, layer_width, nwell_layer.name, Layer.Function.WELLN, 0, graph,
            nwell_width, true, false);
        // P-Well
        graph = new EGraphics(true, true, null, 0, nwell_colour.getRed(), nwell_colour.getGreen(),
            nwell_colour.getBlue(), 1, true, patternBackSlash);
        Xml.Layer pwellLayer = makeXmlLayer(t.layers, layer_width, "P-Well", Layer.Function.WELLP, 0, graph,
            nwell_width, true, false);

        // DeviceMark
        graph = new EGraphics(false, false, null, 0, 255, 0, 0, 0.4, true, nullPattern);
        Xml.Layer deviceMarkLayer = makeXmlLayer(t.layers, layer_width, "DeviceMark", Layer.Function.CONTROL, 0, graph,
            nplus_width, true, false);

        // Extra layers
        if (getExtraInfoFlag())
        {
            for (LayerInfo info : extraLayers)
            {
                graph = null;
                // either color or template
                assert (info.graphicsTemplate == null || info.graphicsColor == null);
                if (info.graphicsTemplate != null)
                {
                    // look for layer name and get its EGraphics
                    for (Xml.Layer l : t.layers)
                    {
                        if (l.name.equals(info.graphicsTemplate))
                        {
                            graph = l.desc;
                            break;
                        }
                    }
                    if (graph == null)
                        System.out.println("No template layer " + info.graphicsTemplate + " found");
                }
                else if (info.graphicsColor != null)
                {
                    graph = new EGraphics(true, true, info.graphicsOutline, 0,
                        info.graphicsColor.getRed(), info.graphicsColor.getGreen(), info.graphicsColor.getBlue(),
                        1, true, info.graphicsPattern);
                }
                if (graph == null)
                    graph = new EGraphics(false, false, null, 0, 255, 0, 0, 0.4, true, nullPattern);

                Xml.Layer layer = makeXmlLayer(t.layers, layer_width, info.name, Layer.Function.ART, 0, graph,
                    nplus_width, true, false);
                makeLayerGDS(t, layer, String.valueOf(info));
            }
        }

        // Palette elements should be added at the end so they will appear in groups
        PaletteGroup[] metalPalette = new PaletteGroup[num_metal_layers];

        // write arcs
        // metal arcs
        for(int i=1; i<=num_metal_layers; i++)
        {
            double ant = (int)Math.round(metal_antenna_ratio[i-1]) | 200;
            PaletteGroup group = new PaletteGroup();
            metalPalette[i-1] = group;
            group.addArc(makeXmlArc(t, "Metal-"+i, ArcProto.Function.getContact(i), ant,
                makeXmlArcLayer(metalLayers.get(i-1), metal_width[i-1])));
        }

        List<String> portNames = new ArrayList<String>();
        /**************************** POLY Nodes/Arcs ***********************************************/
        // poly arc
        double ant = (int)Math.round(poly_antenna_ratio) | 200;
        PaletteGroup polyGroup = new PaletteGroup();
        polyGroup.addArc(makeXmlArc(t, polyLayer.name, ArcProto.Function.getPoly(1), ant,
                makeXmlArcLayer(polyLayer, poly_width)));
        // poly pin
        double hla = scaledValue(poly_width.v / 2);
        polyGroup.addPinOrResistor(makeXmlPrimitivePin(t, polyLayer.name, hla, null, // new SizeOffset(hla, hla, hla, hla),
            makeXmlNodeLayer(hla, hla, hla, hla, polyLayer, Poly.Type.CROSSED)), null);
        // poly contact
        portNames.clear();
        portNames.add(polyLayer.name);
        portNames.add(metalLayers.get(0).name);
        hla = scaledValue((contact_size.v/2 + contact_poly_overhang.v));
        Xml.Layer m1Layer = metalLayers.get(0);
        double contSize = scaledValue(contact_size.v);
        double contSpacing = scaledValue(contact_spacing.v);
        double contArraySpacing = scaledValue(contact_array_spacing.v);
        double metal1Over = scaledValue(contact_size.v/2 + contact_metal_overhang_all_sides.v);

        // only for standard cases when getProtectionPoly() is false
        if (!getExtraInfoFlag())
        {
            polyGroup.addElement(makeContactSeries(t.nodeGroups, polyLayer.name, contSize, polyConLayer, contSpacing, contArraySpacing,
                        scaledValue(contact_poly_overhang.v), polyLayer,
                        scaledValue(via_overhang[0].v), m1Layer), null);
        }

        /**************************** N/P-Diff Nodes/Arcs/Group ***********************************************/
        PaletteGroup[] diffPalette = new PaletteGroup[2];
        diffPalette[0] = new PaletteGroup(); diffPalette[1] = new PaletteGroup();

        // ndiff/pdiff pins
        hla = scaledValue((contact_size.v/2 + diff_contact_overhang.v));
        double nsel = scaledValue(contact_size.v/2 + diff_contact_overhang.v + nplus_overhang_diff.v);
        double psel = scaledValue(contact_size.v/2 + diff_contact_overhang.v + pplus_overhang_diff.v);
        double nwell = scaledValue(contact_size.v/2 + diff_contact_overhang.v + nwell_overhang_diff_p.v);
        double nso = scaledValue(nwell_overhang_diff_p.v /*+ diff_contact_overhang.v*/); // valid for elements that have nwell layers
        double pso = (!pWellFlag)?nso:scaledValue(nplus_overhang_diff.v/* + diff_contact_overhang.v*/);

        // ndiff/pdiff contacts
        String[] diffNames = {"P", "N"};
        double[] sos = {nso, pso};
        double[] sels = {psel, nsel};
        Xml.Layer[] diffLayers = {diffPLayer, diffNLayer};
        Xml.Layer[] plusLayers = {pplusLayer, nplusLayer};

        // Active and poly contacts. They are defined first that the Full types
        for (Map.Entry<String,List<Contact>> e : otherContacts.entrySet())
        {
            // generic contacts
            String name = null;

            for (Contact c : e.getValue())
            {
                Xml.Layer ly = null, lx = null;
                Xml.Layer conLay = diffConLayer;
                PaletteGroup g = null;
                ContactNode metalLayer = c.layers.get(0);
                ContactNode otherLayer = c.layers.get(1);

                if (!TextUtils.isANumber(metalLayer.layer)) // horizontal must be!
                {
                    assert (TextUtils.isANumber(otherLayer.layer));
                    metalLayer = c.layers.get(1);
                    otherLayer = c.layers.get(0);
                }

                int m1 = Integer.valueOf(metalLayer.layer);
                ly = metalLayers.get(m1-1);
                String layerName = otherLayer.layer;
                if (layerName.equals(diffLayers[0].name))
                {
                    lx = diffLayers[0];
                    g = diffPalette[0];
                }
                else if (layerName.equals(diffLayers[1].name))
                {
                    lx = diffLayers[1];
                    g = diffPalette[1];
                }
                else if (layerName.equals(polyLayer.name))
                {
                    lx = polyLayer;
                    conLay = polyConLayer;
                    g = polyGroup;
                }
                else
                    assert(false); // it should not happen
                name = ly.name + "-" + lx.name;
                double h1x = scaledValue(contact_size.v/2 + metalLayer.overX.v);
                double h1y = scaledValue(contact_size.v/2 + metalLayer.overY.v);
                double h2x = scaledValue(contact_size.v/2 + otherLayer.overX.v);
                double h2y = scaledValue(contact_size.v/2 + otherLayer.overY.v);
                double longX = (Math.abs(metalLayer.overX.v - otherLayer.overX.v));
                double longY = (Math.abs(metalLayer.overY.v - otherLayer.overY.v));

                portNames.clear();
                portNames.add(lx.name);
                portNames.add(ly.name);
                Xml.NodeLayer extraN = null;
                if (c.layers.size() == 3) // easy solution for now
                {
                    ContactNode node = c.layers.get(2);
                    Xml.Layer lz = (diffLayers[0] == lx) ? plusLayers[0] : plusLayers[1];
                    double h3x = scaledValue(contact_size.v/2 + node.overX.v);
                    double h3y = scaledValue(contact_size.v/2 + node.overY.v);
                    extraN = makeXmlNodeLayer(h3x, h3x, h3y, h3y, lz, Poly.Type.FILLED);

                    // This assumes no well is defined
                    longX = (Math.abs(node.overX.v - otherLayer.overX.v));
                    longY = (Math.abs(node.overY.v - otherLayer.overY.v));
                }
                longX = scaledValue(longX);
                longY = scaledValue(longY);

                // some primitives might not have prefix. "-" should not be in the prefix to avoid
                // being displayed in the palette
                String p = (c.prefix == null || c.prefix.equals("")) ? "" : c.prefix + "-";
                g.addElement(makeXmlPrimitiveCon(t.nodeGroups, p + name, PrimitiveNode.Function.CONTACT, -1, -1,
                    new SizeOffset(longX, longX, longY, longY), portNames,
                    makeXmlNodeLayer(h1x, h1x, h1y, h1y, ly, Poly.Type.FILLED), // layer1
                    makeXmlNodeLayer(h2x, h2x, h2y, h2y, lx, Poly.Type.FILLED), // layer2
                    extraN,
                    makeXmlMulticut(conLay, contSize, contSpacing, contArraySpacing)), c.prefix); // contact
            }
        }

        // ndiff/pdiff contact
        for (int i = 0; i < 2; i++)
        {
            portNames.clear();
            portNames.add(diffLayers[i].name);
            portNames.add(m1Layer.name);
            String composeName = diffNames[i] + "-" + diff_layer.name; //Diff";
            Xml.NodeLayer wellNode, wellNodePin;
            ArcProto.Function arcF;
            Xml.ArcLayer arcL;
            WizardField arcVal;

            if (i == Technology.P_TYPE)
            {
                wellNodePin = makeXmlNodeLayer(nwell, nwell, nwell, nwell, nwellLayer, Poly.Type.CROSSED);
                wellNode = makeXmlNodeLayer(nwell, nwell, nwell, nwell, nwellLayer, Poly.Type.FILLED);
                arcF = ArcProto.Function.DIFFP;
                arcL = makeXmlArcLayer(nwellLayer, diff_width, nwell_overhang_diff_p);
                arcVal = pplus_overhang_diff;
            }
            else
            {
                wellNodePin = (!pWellFlag)?makeXmlNodeLayer(nwell, nwell, nwell, nwell, pwellLayer, Poly.Type.CROSSED):null;
                wellNode = (!pWellFlag)?makeXmlNodeLayer(nwell, nwell, nwell, nwell, pwellLayer, Poly.Type.FILLED):null;
                arcF = ArcProto.Function.DIFFN;
                arcL = (!pWellFlag)?makeXmlArcLayer(pwellLayer, diff_width, nwell_overhang_diff_p):null;
                arcVal = nplus_overhang_diff;
            }

            PaletteGroup diffG = diffPalette[i];
            
            // active arc
            diffG.addArc(makeXmlArc(t, composeName, arcF, 0,
                makeXmlArcLayer(diffLayers[i], diff_width),
                makeXmlArcLayer(plusLayers[i], diff_width, arcVal),
                arcL));

            // active pin
            diffG.addPinOrResistor(makeXmlPrimitivePin(t, composeName, hla,
                new SizeOffset(sos[i], sos[i], sos[i], sos[i]),
                makeXmlNodeLayer(hla, hla, hla, hla, diffLayers[i], Poly.Type.CROSSED),
                makeXmlNodeLayer(sels[i], sels[i], sels[i], sels[i], plusLayers[i], Poly.Type.CROSSED),
                wellNodePin), null);

            // F stands for full (all layers)
            diffG.addElement(makeXmlPrimitiveCon(t.nodeGroups, "F-"+composeName, PrimitiveNode.Function.CONTACT,
                hla, hla, new SizeOffset(sos[i], sos[i], sos[i], sos[i]), portNames,
                makeXmlNodeLayer(metal1Over, metal1Over, metal1Over, metal1Over, m1Layer, Poly.Type.FILLED), // meta1 layer
                makeXmlNodeLayer(hla, hla, hla, hla, diffLayers[i], Poly.Type.FILLED), // active layer
                makeXmlNodeLayer(sels[i], sels[i], sels[i], sels[i], plusLayers[i], Poly.Type.FILLED), // select layer
                wellNode, // well layer
                makeXmlMulticut(diffConLayer, contSize, contSpacing, contArraySpacing)), "Full-" + diffNames[i]); // contact
        }

        /**************************** N/P-Well Contacts ***********************************************/
        nwell = scaledValue(contact_size.v/2 + diff_contact_overhang.v + nwell_overhang_diff_n.v);
        nso = scaledValue(/*diff_contact_overhang.v +*/ nwell_overhang_diff_n.v); // valid for elements that have nwell layers
        pso = (!pWellFlag)?nso:scaledValue(/*diff_contact_overhang.v +*/ nplus_overhang_diff.v);
        double[] wellSos = {pso, nso};
        PaletteGroup[] wellPalette = new PaletteGroup[2];

        Xml.Layer[] wellLayers = {pwellLayer, nwellLayer};

        // nwell/pwell contact
        for (int i = 0; i < 2; i++)
        {
            String composeName = diffNames[i] + "-Well";
            Xml.NodeLayer wellNodeLayer = null, wellNodePinLayer = null;
            PaletteGroup g = new PaletteGroup();
            PrimitiveNode.Function func = null;
            Xml.ArcLayer arcL;
            WizardField arcVal;

            wellPalette[i] = g;

            portNames.clear();
            if (i == Technology.P_TYPE)
            {
                if (!pWellFlag)
                {
                    portNames.add(pwellLayer.name);
                    wellNodePinLayer = makeXmlNodeLayer(nwell, nwell, nwell, nwell, pwellLayer, Poly.Type.CROSSED);
                    wellNodeLayer = makeXmlNodeLayer(nwell, nwell, nwell, nwell, pwellLayer, Poly.Type.FILLED);
                }
                func = PrimitiveNode.Function.WELL;
                arcL = (!pWellFlag)?makeXmlArcLayer(pwellLayer, diff_width, nwell_overhang_diff_p):null;
                arcVal = pplus_overhang_diff;
            }
            else
            {
                portNames.add(nwellLayer.name);
                wellNodePinLayer = makeXmlNodeLayer(nwell, nwell, nwell, nwell, nwellLayer, Poly.Type.CROSSED);
                wellNodeLayer = makeXmlNodeLayer(nwell, nwell, nwell, nwell, nwellLayer, Poly.Type.FILLED);
                func = PrimitiveNode.Function.SUBSTRATE;
                arcL = makeXmlArcLayer(nwellLayer, diff_width, nwell_overhang_diff_p);
                arcVal = nplus_overhang_diff;
            }
            portNames.add(m1Layer.name);

            // simple arc. S for simple
            g.addArc(makeXmlArc(t, "S-"+composeName, ArcProto.Function.WELL, 0,
                makeXmlArcLayer(wellLayers[i], diff_width, nwell_overhang_diff_p)));

            // three layers arcs
            g.addArc(makeXmlArc(t, composeName, ArcProto.Function.WELL, 0,
                    makeXmlArcLayer(diffLayers[i], diff_width),
                    makeXmlArcLayer(plusLayers[i], diff_width, arcVal),
                    arcL));

            // well pin
            g.addPinOrResistor(makeXmlPrimitivePin(t, composeName, hla,
                new SizeOffset(wellSos[i], wellSos[i], wellSos[i], wellSos[i]),
                makeXmlNodeLayer(hla, hla, hla, hla, diffLayers[i], Poly.Type.CROSSED),
                makeXmlNodeLayer(sels[i], sels[i], sels[i], sels[i], plusLayers[i], Poly.Type.CROSSED),
                wellNodePinLayer), null);

            // well contact
            // F stands for full
            g.addElement(makeXmlPrimitiveCon(t.nodeGroups, "F-"+composeName, func,
                hla, hla, new SizeOffset(wellSos[i], wellSos[i], wellSos[i], wellSos[i]), portNames,
                makeXmlNodeLayer(metal1Over, metal1Over, metal1Over, metal1Over, m1Layer, Poly.Type.FILLED), // meta1 layer
                makeXmlNodeLayer(hla, hla, hla, hla, diffLayers[i], Poly.Type.FILLED), // active layer
                makeXmlNodeLayer(sels[i], sels[i], sels[i], sels[i], plusLayers[i], Poly.Type.FILLED), // select layer
                wellNodeLayer, // well layer
                makeXmlMulticut(diffConLayer, contSize, contSpacing, contArraySpacing)), "Full-"+diffNames[i] + "W"); // contact
        }

        /**************************** Metals Nodes/Arcs ***********************************************/

        // Pins and contacts
        for(int i=1; i<num_metal_layers; i++)
		{
            hla = scaledValue(metal_width[i-1].v / 2);
            Xml.Layer lb = metalLayers.get(i-1);
            Xml.Layer lt = metalLayers.get(i);
            PaletteGroup group = metalPalette[i-1];  // structure created by the arc definition

            // Pin bottom metal
            group.addPinOrResistor(makeXmlPrimitivePin(t, lb.name, hla, null, //new SizeOffset(hla, hla, hla, hla),
                makeXmlNodeLayer(hla, hla, hla, hla, lb, Poly.Type.CROSSED)), null);
            if (i == num_metal_layers - 1) // last pin!
            {
                metalPalette[i].addPinOrResistor(makeXmlPrimitivePin(t, lt.name, hla, null, //new SizeOffset(hla, hla, hla, hla),
                    makeXmlNodeLayer(hla, hla, hla, hla, lt, Poly.Type.CROSSED)), null);
            }

            if (!getExtraInfoFlag())
            {
                // original contact Square
                // via
                Xml.Layer via = viaLayers.get(i-1);
                double viaSize = scaledValue(via_size[i-1].v);
                double viaSpacing = scaledValue(via_inline_spacing[i-1].v);
                double viaArraySpacing = scaledValue(via_array_spacing[i-1].v);
                String name = lb.name + "-" + lt.name;

                double longDist = scaledValue(via_overhang[i-1].v);
                group.addElement(makeContactSeries(t.nodeGroups, name, viaSize, via, viaSpacing, viaArraySpacing,
                    longDist, lt, longDist, lb), null);
            }
        }

        // metal contacts
        for (Map.Entry<String,List<Contact>> e : metalContacts.entrySet())
        {
            // generic contacts
            for (Contact c : e.getValue())
            {
                // We know those layer names are numbers!
                assert(c.layers.size() == 2);
                ContactNode verticalLayer = c.layers.get(0);
                ContactNode horizontalLayer = c.layers.get(1);

                int i = Integer.valueOf(verticalLayer.layer);
                int j = Integer.valueOf(horizontalLayer.layer);
                Xml.Layer ly = metalLayers.get(i-1);
                Xml.Layer lx = metalLayers.get(j-1);
                String name = (j>i)?ly.name + "-" + lx.name:lx.name + "-" + ly.name;
                int via = (j>i)?i:j;
                double metalContSize = scaledValue(via_size[via-1].v);
                double spacing = scaledValue(via_inline_spacing[via-1].v);
                double arraySpacing = scaledValue(via_array_spacing[via-1].v);
                Xml.Layer metalConLayer = viaLayers.get(via-1);
                double h1x = scaledValue(via_size[via-1].v/2 + verticalLayer.overX.v);
                double h1y = scaledValue(via_size[via-1].v/2 + verticalLayer.overY.v);
                double h2x = scaledValue(via_size[via-1].v/2 + horizontalLayer.overX.v);
                double h2y = scaledValue(via_size[via-1].v/2 + horizontalLayer.overY.v);

//                double longX = scaledValue(DBMath.isGreaterThan(verticalLayer.overX.v, horizontalLayer.overX.v) ? verticalLayer.overX.v : horizontalLayer.overX.v);
//                double longY = scaledValue(DBMath.isGreaterThan(verticalLayer.overY.v, horizontalLayer.overY.v) ? verticalLayer.overY.v : horizontalLayer.overY.v);
                double longX = scaledValue(Math.abs(verticalLayer.overX.v - horizontalLayer.overX.v));
                double longY = scaledValue(Math.abs(verticalLayer.overY.v - horizontalLayer.overY.v));
                portNames.clear();
                portNames.add(lx.name);
                portNames.add(ly.name);

                // some primitives might not have prefix. "-" should not be in the prefix to avoid
                // being displayed in the palette
                String p = (c.prefix == null || c.prefix.equals("")) ? "" : c.prefix + "-";
                metalPalette[via-1].addElement(makeXmlPrimitiveCon(t.nodeGroups, p + name, PrimitiveNode.Function.CONTACT, -1, -1,
                    new SizeOffset(longX, longX, longY, longY),
                    portNames,
                    makeXmlNodeLayer(h1x, h1x, h1y, h1y, ly, Poly.Type.FILLED), // layer1
                    makeXmlNodeLayer(h2x, h2x, h2y, h2y, lx, Poly.Type.FILLED), // layer2
                    makeXmlMulticut(metalConLayer, metalContSize, spacing, arraySpacing)), c.prefix); // contact
            }
        }

        /**************************** Transistors ***********************************************/
        /** Transistors **/
        // write the transistors
        List<Xml.NodeLayer> nodesList = new ArrayList<Xml.NodeLayer>();
        List<Xml.PrimitivePort> nodePorts = new ArrayList<Xml.PrimitivePort>();
        EPoint minFullSize = null; //EPoint.fromLambda(0, 0);  // default zero    horizontalFlag
        PaletteGroup[] transPalette = new PaletteGroup[2];

        for(int i = 0; i < 2; i++)
        {
            String name;
            double selecty = 0, selectx = 0;
            Xml.Layer wellLayer = null, activeLayer, selectLayer;
            double sox = 0, soy = 0;
            double impx = scaledValue((gate_width.v)/2);
            double impy = scaledValue((gate_length.v+diff_poly_overhang.v*2)/2);
            double nwell_overhangX = 0, nwell_overhangY = 0;
            PaletteGroup g = new PaletteGroup();
            transPalette[i] = g;

            double protectDist = scaledValue(poly_protection_spacing.v);
            double extraSelX = 0, extraSelY = 0;
            PrimitiveNode.Function func = null, prFunc = null;

            if (i==Technology.P_TYPE)
            {
				name = "P";
                nwell_overhangY = nwell_overhangX = nwell_overhang_diff_n.v;
                wellLayer = nwellLayer;
                activeLayer = diffPLayer;
                selectLayer = pplusLayer;
                extraSelX = pplus_overhang_poly.v;
                extraSelY = pplus_overhang_diff.v;
                func = PrimitiveNode.Function.TRAPMOS;
                prFunc = PrimitiveNode.Function.RESPPOLY;
            }
            else
            {
				name = "N";
                activeLayer = diffNLayer;
                selectLayer = nplusLayer;
                extraSelX = nplus_overhang_poly.v;
                extraSelY = nplus_overhang_diff.v;
                func = PrimitiveNode.Function.TRANMOS;
                prFunc = PrimitiveNode.Function.RESNPOLY;
                if (!pWellFlag)
                {
                    nwell_overhangY = nwell_overhangX = nwell_overhang_diff_p.v;
                    wellLayer = pwellLayer;
                }
                else
                {
                    nwell_overhangX = poly_endcap.v+extraSelX;
                    nwell_overhangY = extraSelY;
                }
            }

            selectx = scaledValue(gate_width.v/2+poly_endcap.v+extraSelX);
            selecty = scaledValue(gate_length.v/2+diff_poly_overhang.v+extraSelY);

            // Using P values in transistors
            double wellx = scaledValue((gate_width.v/2+nwell_overhangX));
            double welly = scaledValue((gate_length.v/2+diff_poly_overhang.v+nwell_overhangY));

            sox = scaledValue(nwell_overhangX);
            soy = scaledValue(diff_poly_overhang.v+nwell_overhangY);

            if (DBMath.isLessThan(wellx, selectx))
            {
                sox = scaledValue(poly_endcap.v+extraSelX);
                wellx = selectx;
            }
            if (DBMath.isLessThan(welly, selecty))
            {
                soy = scaledValue(diff_poly_overhang.v+extraSelY);
                welly = selecty;
            }

            nodesList.clear();
            nodePorts.clear();
            portNames.clear();

            // Gate layer Electrical
            double gatey = scaledValue(gate_length.v/2);
            double gatex = impx;
            // Poly layers
            // left electrical
            double endPolyx = scaledValue((gate_width.v+poly_endcap.v*2)/2);
            double endPolyy = gatey;
            double endLeftOrRight = -impx;   // for horizontal transistors. Default
            double endTopOrBotton = endPolyy; // for horizontal transistors. Default
            double diffX = 0, diffY = scaledValue(gate_length.v/2+gate_contact_spacing.v+contact_size.v/2);  // impy
            double xSign = 1, ySign = -1;
            double polyX = endPolyx, polyY = 0;

            if (!horizontalFlag) // swap the numbers to get vertical transistors
            {
                double tmp;
                tmp = impx; impx = impy; impy = tmp;
                tmp = wellx; wellx = welly; welly = tmp;
                tmp = sox; sox = soy; soy = tmp;
                tmp = selectx; selectx = selecty; selecty = tmp;
                tmp = gatex; gatex = gatey; gatey = tmp;
                tmp = endPolyx; endPolyx = endPolyy; endPolyy = tmp;
                tmp = diffX; diffX = diffY; diffY = tmp;
                tmp = polyX; polyX = polyY; polyY = tmp;
                tmp = xSign; xSign = ySign; ySign = tmp;
                endLeftOrRight = endPolyx;
                endTopOrBotton = -impx;
            }

            // Well layer
            Xml.NodeLayer xTranWellLayer = null;
            if (wellLayer != null)
            {
                xTranWellLayer = (makeXmlNodeLayer(wellx, wellx, welly, welly, wellLayer, Poly.Type.FILLED));
                nodesList.add(xTranWellLayer);
            }

            // Active layers
            nodesList.add(makeXmlNodeLayer(impx, impx, impy, impy, activeLayer, Poly.Type.FILLED, true, false, -1));
            // electrical active layers
            nodesList.add(makeXmlNodeLayer(impx, impx, impy, 0, activeLayer, Poly.Type.FILLED, false, true, 3));  // bottom
            nodesList.add(makeXmlNodeLayer(impx, impx, 0, impy, activeLayer, Poly.Type.FILLED, false, true, 1));  // top

            // Diff port
            portNames.clear();
            portNames.add(activeLayer.name);

            Xml.PrimitivePort diffTopPort = makeXmlPrimitivePort("diff-top", 90, 90, 1, minFullSize,
                diffX, -1, diffX, 1, diffY, 1, diffY, 1, portNames);
            // bottom port
            Xml.PrimitivePort diffBottomPort = makeXmlPrimitivePort("diff-bottom", 270, 90, 2, minFullSize,
                xSign*diffX, -1, xSign*diffX, 1, ySign*diffY, -1, ySign*diffY, -1, portNames);

            // Electric layers
            // Gate layer Electrical
            nodesList.add(makeXmlNodeLayer(gatex, gatex, gatey, gatey, polyGateLayer, Poly.Type.FILLED, false, true, -1));

            // Poly layers
            // left electrical
            nodesList.add(makeXmlNodeLayer(endPolyx, endLeftOrRight, endPolyy, endTopOrBotton, polyLayer,
                Poly.Type.FILLED, false, true, 0));
            // right electrical
            nodesList.add(makeXmlNodeLayer(endLeftOrRight, endPolyx, endTopOrBotton, endPolyy, polyLayer,
                Poly.Type.FILLED, false, true, 2));

            // non-electrical poly (just one poly layer)
            nodesList.add(makeXmlNodeLayer(endPolyx, endPolyx, endPolyy, endPolyy, polyLayer, Poly.Type.FILLED, true, false, -1));

            // Poly port
            portNames.clear();
            portNames.add(polyLayer.name);
            Xml.PrimitivePort polyLeftPort = makeXmlPrimitivePort("poly-left", 180, 90, 0, minFullSize,
                ySign*polyX, -1, ySign*polyX,
                -1, xSign*polyY, -1, xSign*polyY, 1, portNames);
            // right port
            Xml.PrimitivePort polyRightPort = makeXmlPrimitivePort("poly-right", 0, 180, 0, minFullSize,
                polyX, 1, polyX, 1, polyY, -1, polyY, 1, portNames);

            // Select layer
            Xml.NodeLayer xTranSelLayer = (makeXmlNodeLayer(selectx, selectx, selecty, selecty, selectLayer, Poly.Type.FILLED));
            nodesList.add(xTranSelLayer);

            //One (undocumented) requirement of transistors is that the ports must appear in the
            //order: Poly-left, Diff-top, Poly-right, Diff-bottom.  This requirement is
            //because of the methods Technology.getTransistorGatePort(),
            //Technology.getTransistorAltGatePort(), Technology.getTransistorSourcePort(),
            //and Technology.getTransistorDrainPort().
            // diff-top = 1, diff-bottom = 2, polys=0
            // ports in the correct order: Poly-left, Diff-top, Poly-right, Diff-bottom
            nodePorts.add(polyLeftPort);
            nodePorts.add(diffTopPort);
            nodePorts.add(polyRightPort);
            nodePorts.add(diffBottomPort);

            // Standard Transistor
            Xml.PrimitiveNodeGroup n = makeXmlPrimitive(t.nodeGroups, name + "-Transistor", func, 0, 0, 0, 0,
                new SizeOffset(sox, sox, soy, soy), nodesList, nodePorts, null, false);
            g.addElement(n, name);

            // Extra transistors which don't have select nor well
            // Extra protection poly. No ports are necessary.
            if (getExtraInfoFlag())
            {
                // removing well and select for simplicity
//                nodesList.remove(xTranSelLayer);
//                nodesList.remove(xTranWellLayer);
//                // new sox and soy
//                sox = scaledValue(poly_endcap.v);
//                soy = scaledValue(diff_poly_overhang.v);
//                n = makeXmlPrimitive(t.nodeGroups, name + "-Transistor-S", PrimitiveNode.Function.TRANMOS, 0, 0, 0, 0,
//                new SizeOffset(sox, sox, soy, soy), nodesListW, nodePorts, null, false);
//                g.addElement(n);

                /*************************************/
                // Short transistors
                // Adding extra transistors whose select and well are aligned with poly along the X axis
                nodesList.remove(xTranSelLayer);
                double shortSelectX = scaledValue(gate_width.v/2+poly_endcap.v);
                xTranSelLayer = (makeXmlNodeLayer(shortSelectX, shortSelectX, selecty, selecty, selectLayer, Poly.Type.FILLED));
                nodesList.add(xTranSelLayer);
                double shortSox = sox;

                shortSox = scaledValue(poly_endcap.v);
                if (wellLayer != null)
                {
                    nodesList.remove(xTranWellLayer);
                    xTranWellLayer = (makeXmlNodeLayer(shortSelectX, shortSelectX, welly, welly, wellLayer, Poly.Type.FILLED));
                    nodesList.add(xTranWellLayer);
                }
                n = makeXmlPrimitive(t.nodeGroups, name + "-Transistor-S", func, 0, 0, 0, 0,
                     new SizeOffset(shortSox, shortSox, soy, soy), nodesList, nodePorts, null, false);
                g.addElement(n, name + "-S");

                /*************************************/
                // Short transistors with VTH and VTL

                double vthlx = scaledValue(gate_width.v/2+vthl_diff_overhang.v);
                double vthly = scaledValue(gate_length.v/2+ vthl_poly_overhang.v);

                // VTH Transistor
                String tmp = "VTH-" + name;
                Xml.Layer vthLayer = t.findLayer(tmp);
                Xml.NodeLayer nl = makeXmlNodeLayer(vthlx, vthlx, vthly, vthly, vthLayer, Poly.Type.FILLED);
                nodesList.add(nl);

                n = makeXmlPrimitive(t.nodeGroups, tmp + "-Transistor-S", func, 0, 0, 0, 0,
                     new SizeOffset(shortSox, shortSox, soy, soy), nodesList, nodePorts, null, false);
                g.addElement(n, tmp + "-S");

                // VTL Transistor
                nodesList.remove(nl);
                tmp = "VTL-" + name;
                vthLayer = t.findLayer(tmp);
                nl = makeXmlNodeLayer(vthlx, vthlx, vthly, vthly, vthLayer, Poly.Type.FILLED);
                nodesList.add(nl);

                n = makeXmlPrimitive(t.nodeGroups, tmp + "-Transistor-S", func, 0, 0, 0, 0,
                     new SizeOffset(shortSox, shortSox, soy, soy), nodesList, nodePorts, null, false);
                g.addElement(n, tmp + "-S");

                /*************************************/
                // Transistors with extra polys

                // different select for those with extra protection layers
                nodesList.remove(xTranSelLayer);
                double endOfProtectionY = gate_length.v + poly_protection_spacing.v;
                double selectExtraY = scaledValue(gate_length.v/2 + endOfProtectionY + extraSelX); // actually is extraSelX because of the poly distance!
                xTranSelLayer = (makeXmlNodeLayer(selectx, selectx, selectExtraY, selectExtraY, selectLayer, Poly.Type.FILLED));
                nodesList.add(xTranSelLayer);

                // not sure which condition to apply. It doesn't apply  nwell_overhang_diff due to the extra poly
                if (DBMath.isLessThan(welly, selectExtraY))
                {
                    welly = selectExtraY;
                    soy = scaledValue(endOfProtectionY + extraSelX);
                }
                if (wellLayer != null)
                {
                    nodesList.remove(xTranWellLayer);
                    xTranWellLayer = (makeXmlNodeLayer(wellx, wellx, welly, welly, wellLayer, Poly.Type.FILLED));
                    nodesList.add(xTranWellLayer);
                }
                if (!horizontalFlag)
                {
                    System.out.println("Not working with !horizontal");
                    assert(false);
                }
                portNames.clear();
                portNames.add(polyLayer.name);

                // bottom or left
                Xml.NodeLayer bOrL = (makeXmlNodeLayer(gatex, gatex,
                    DBMath.round((protectDist + 3*endPolyy)),
                    -DBMath.round(endPolyy + protectDist),
                   polyLayer, Poly.Type.FILLED, true, false, -1/*3*/)); // port 3 for left/bottom extra poly lb=left bottom
                // Adding left
                nodesList.add(bOrL);
                n = makeXmlPrimitive(t.nodeGroups, name + "-Transistor-B", func, 0, 0, 0, 0,
                new SizeOffset(sox, sox, soy, soy), nodesList, nodePorts, null, false);
                g.addElement(n, name + "-B");

                // top or right
                Xml.NodeLayer tOrR = (makeXmlNodeLayer(gatex, gatex,
                    -DBMath.round(endPolyy + protectDist),
                    DBMath.round((protectDist + 3*endPolyy)),
                    polyLayer, Poly.Type.FILLED, true, false, -1/*4*/)); // port 4 for right/top extra poly rt=right top

                // Adding both
                nodesList.add(tOrR);
                n = makeXmlPrimitive(t.nodeGroups, name + "-Transistor-TB", func, 0, 0, 0, 0,
                new SizeOffset(sox, sox, soy, soy), nodesList, nodePorts, null, false);
                g.addElement(n, name + "-TB");

                // Adding right
                nodesList.remove(bOrL);
                n = makeXmlPrimitive(t.nodeGroups, name + "-Transistor-T", func, 0, 0, 0, 0,
                new SizeOffset(sox, sox, soy, soy), nodesList, nodePorts, null, false);
                g.addElement(n, name +"-T");

                /*************************************/
                // Short transistors woth OD18

                double od18x = scaledValue(gate_od18_width.v/2+od18_diff_overhang[0].v);
                double od18y = scaledValue(gate_od18_length.v/2+diff_poly_overhang.v+od18_diff_overhang[1].v);

                nodePorts.clear();
                nodesList.clear();
                prepareTransistor(gate_od18_width.v, gate_od18_length.v, poly_endcap.v, diff_poly_overhang.v,
                    gate_contact_spacing.v, contact_size.v, activeLayer, polyLayer, polyGateLayer, nodesList, nodePorts);

                // OD18
                Xml.Layer od18Layer = t.findLayer("OD_18");
                nodesList.add(makeXmlNodeLayer(od18x, od18x, od18y, od18y, od18Layer, Poly.Type.FILLED));

                // adding short select
                shortSelectX = scaledValue(gate_od18_width.v/2+poly_endcap.v);
                selecty = scaledValue(gate_od18_length.v/2+diff_poly_overhang.v+extraSelY);
                xTranSelLayer = (makeXmlNodeLayer(shortSelectX, shortSelectX, selecty, selecty, selectLayer, Poly.Type.FILLED));
                nodesList.add(xTranSelLayer);

                // adding well
                if (wellLayer != null)
                {
                    xTranWellLayer = (makeXmlNodeLayer(od18x, od18x, od18y, od18y, wellLayer, Poly.Type.FILLED));
                    nodesList.add(xTranWellLayer);
                }

                sox = scaledValue(od18_diff_overhang[0].v);
                soy = scaledValue(diff_poly_overhang.v+od18_diff_overhang[1].v);
                n = makeXmlPrimitive(t.nodeGroups, "OD18-" + name + "-Transistor-S", func, 0, 0, 0, 0,
                new SizeOffset(sox, sox, soy, soy), nodesList, nodePorts, null, false);
                g.addElement(n, "18-" + name + "-S");

                /*************************************/
                // Short transistors with native

                if (i==Technology.N_TYPE)
                {
                    double ntx = scaledValue(gate_nt_width.v/2+nt_diff_overhang.v);
                    double nty = scaledValue(gate_nt_length.v/2+diff_poly_overhang.v+nt_diff_overhang.v);

                    nodePorts.clear();
                    nodesList.clear();
                    prepareTransistor(gate_nt_width.v, gate_nt_length.v, poly_nt_endcap.v, diff_poly_overhang.v,
                        gate_contact_spacing.v, contact_size.v, activeLayer, polyLayer, polyGateLayer, nodesList, nodePorts);

                    // NT-N
                    Xml.Layer ntLayer = t.findLayer("NT-N");
                    nodesList.add(makeXmlNodeLayer(ntx, ntx, nty, nty, ntLayer, Poly.Type.FILLED));

                    // adding short select
                    shortSelectX = scaledValue(gate_nt_width.v/2+poly_nt_endcap.v);
                    selecty = scaledValue(gate_nt_length.v/2+diff_poly_overhang.v+extraSelY);
                    xTranSelLayer = (makeXmlNodeLayer(shortSelectX, shortSelectX, selecty, selecty, selectLayer, Poly.Type.FILLED));
                    nodesList.add(xTranSelLayer);

                    // adding well
                    if (wellLayer != null)
                    {
                        xTranWellLayer = (makeXmlNodeLayer(ntx, ntx, nty, nty, wellLayer, Poly.Type.FILLED));
                        nodesList.add(xTranWellLayer);
                    }

                    sox = scaledValue(poly_nt_endcap.v);
                    soy = scaledValue(diff_poly_overhang.v+nt_diff_overhang.v);
                    n = makeXmlPrimitive(t.nodeGroups, "NT-" + name + "-Transistor-S", func, 0, 0, 0, 0,
                        new SizeOffset(sox, sox, soy, soy), nodesList, nodePorts, null, false);
                    g.addElement(n, "NT-" + name + "-S");
                }

                /*************************************/
                // Poly Resistors
                nodesList.clear();
                nodePorts.clear();
                WizardField polyRL = findWizardField("poly_resistor_length");
                WizardField polyRW = findWizardField("poly_resistor_width");
                WizardField rpoS = findWizardField("rpo_contact_spacing");
                WizardField rpoPEx = findWizardField("rpo_poly_overhang");
                WizardField rhOverhang = findWizardField("rh_odpoly_overhang");

                // poly
                double soxNoScaled = (rpoS.v + contact_poly_overhang.v + contact_spacing.v + 2 * contact_size.v);
                double halfTotalL = scaledValue(polyRL.v/2 + soxNoScaled);
                double halfTotalW = scaledValue(polyRW.v/2);
                nodesList.add(makeXmlNodeLayer(halfTotalL, halfTotalL, halfTotalW, halfTotalW, polyLayer,
                    Poly.Type.FILLED, true, true, -1));

                // RPO
                Xml.Layer rpoLayer = t.findLayer("RPO");
                double rpoY = scaledValue(polyRW.v/2 + rpoPEx.v);
                double rpoX = scaledValue(polyRL.v/2);
                nodesList.add(makeXmlNodeLayer(rpoX, rpoX, rpoY, rpoY, rpoLayer,
                    Poly.Type.FILLED, true, true, -1));

                // left cuts
                double cutDistance = scaledValue(rpoS.v + polyRL.v/2);
                // M1 and Poly overhang will be the same for now
//                double absVal = (contact_poly_overhang.v - via_overhang[0].v);
                double m1Distance = cutDistance - scaledValue(contact_poly_overhang.v);
                double m1Y = scaledValue(polyRW.v/2); // - absVal);
                double m1W = scaledValue(2 * contact_poly_overhang.v + contact_spacing.v + 2 * contact_size.v);
                double cutSizeHalf = scaledValue(contact_size.v/2);
                double cutEnd = cutDistance+contSize;
                double cutSpacing = scaledValue(contact_spacing.v);
                double cutEnd2 = cutEnd+contSize+cutSpacing;

                portNames.clear();
                portNames.add(m1Layer.name);
                // left port
                Xml.PrimitivePort port = makeXmlPrimitivePort("left-rpo", 0, 180, 0, minFullSize,
                    -(cutEnd + cutSpacing), -1, -cutEnd, 1, -cutSizeHalf, -1, cutSizeHalf, 1, portNames);
                nodePorts.add(port);
                // right port
                port = makeXmlPrimitivePort("right-rpo", 0, 180, 1, minFullSize,
                    cutEnd, -1, (cutEnd + cutSpacing), 1, -cutSizeHalf, -1, cutSizeHalf, 1, portNames);
                nodePorts.add(port);

                // metal left
                nodesList.add(makeXmlNodeLayer((m1Distance + m1W), -m1Distance, m1Y, m1Y, m1Layer,
                    Poly.Type.FILLED, true, true, 0));
                // right metal
                nodesList.add(makeXmlNodeLayer(-m1Distance, (m1Distance + m1W), m1Y, m1Y, m1Layer,
                    Poly.Type.FILLED, true, true, 1));

                // select
                double selectY = scaledValue(polyRW.v/2 + rhOverhang.v);
                double selectX = scaledValue(polyRL.v/2 + soxNoScaled + extraSelX);
                nodesList.add(makeXmlNodeLayer(selectX, selectX, selectY, selectY, selectLayer,
                    Poly.Type.FILLED, true, true, -1));
                // RH
                Xml.Layer rhLayer = t.findLayer("RH");
                nodesList.add(makeXmlNodeLayer(selectX, selectX, selectY, selectY, rhLayer,
                    Poly.Type.FILLED, true, true, -1));

                // cuts
                nodesList.add(makeXmlMulticut(cutEnd2, -cutDistance, cutSizeHalf, cutSizeHalf, polyConLayer, contSize, contSpacing, contArraySpacing));
                nodesList.add(makeXmlMulticut(-cutDistance, cutEnd2, 0, 0, polyConLayer, contSize, contSpacing, contArraySpacing));

                sox = scaledValue(soxNoScaled + extraSelX);
                soy = scaledValue(rpoPEx.v);
//                n = makeXmlPrimitive(t.nodeGroups, name + "-Poly-RPO-Resistor", prFunc, 0, 0, 0, 0,
//                    new SizeOffset(sox, sox, soy, soy), nodesList, nodePorts, null, false);
//                g.addPinOrResistor(n, name + "-RPoly");
            }
        }

        // Aggregating all palette groups into one
        List<PaletteGroup> allGroups = new ArrayList<PaletteGroup>();

        allGroups.add(transPalette[0]); allGroups.add(transPalette[1]);
        allGroups.add(diffPalette[0]); allGroups.add(diffPalette[1]);
        allGroups.add(wellPalette[0]); allGroups.add(wellPalette[1]);
        allGroups.add(polyGroup);
        for (PaletteGroup g : metalPalette)
            allGroups.add(g);

        // Adding elements in palette
        for (PaletteGroup o : allGroups)
        {
            t.menuPalette.menuBoxes.add(o.arcs);  // arcs
            t.menuPalette.menuBoxes.add(o.pins);  // pins
            t.menuPalette.menuBoxes.add(o.elements);  // contacts
        }

        // Writting GDS values
        makeLayerGDS(t, diffPLayer, String.valueOf(diff_layer));
        makeLayerGDS(t, diffNLayer, String.valueOf(diff_layer));
        makeLayerGDS(t, pplusLayer, String.valueOf(pplus_layer));
        makeLayerGDS(t, nplusLayer, String.valueOf(nplus_layer));
        makeLayerGDS(t, nwellLayer, String.valueOf(nwell_layer));
        makeLayerGDS(t, deviceMarkLayer, String.valueOf(marking_layer));
        makeLayerGDS(t, polyConLayer, String.valueOf(contact_layer));
        makeLayerGDS(t, diffConLayer, String.valueOf(contact_layer));
        makeLayerGDS(t, polyLayer, String.valueOf(poly_layer));
        makeLayerGDS(t, polyGateLayer, String.valueOf(poly_layer));

        for (int i = 0; i < num_metal_layers; i++) {
            Xml.Layer met = metalLayers.get(i);
            makeLayerGDS(t, met, String.valueOf(metal_layers[i]));

            if (getExtraInfoFlag())
            {
                // Type is always 1
                makeLayerGDS(t, dummyMetalLayers.get(i), metal_layers[i].value + "/1");
                // exclusion always takes 150
                makeLayerGDS(t, exclusionMetalLayers.get(i), "150/" + (i + 1));
            }

            if (i > num_metal_layers - 2) continue;

            Xml.Layer via = viaLayers.get(i);
            makeLayerGDS(t, via, String.valueOf(via_layers[i]));
        }

        // Writting Layer Rules
        for (Xml.Layer l : diffLayers)
        {
            makeLayerRuleMinWid(t, l, diff_width);
            makeLayersRule(t, l, DRCTemplate.DRCRuleType.SPACING, diff_spacing);
        }

        WizardField[] plus_diff = {pplus_overhang_diff, nplus_overhang_diff};
        WizardField[] plus_width = {pplus_width, nplus_width};
        WizardField[] plus_spacing = {pplus_spacing, nplus_spacing};

        for (int i = 0; i < plusLayers.length; i++)
        {
            makeLayerRuleMinWid(t, plusLayers[i], plus_width[i]);
            makeLayersRuleSurround(t, plusLayers[i], diffLayers[i], plus_diff[i]);
            makeLayersRule(t, plusLayers[i], DRCTemplate.DRCRuleType.SPACING, plus_spacing[i]);
        }

        Xml.Layer[] wells = {pwellLayer, nwellLayer};

        for (Xml.Layer w : wells)
        {
            makeLayerRuleMinWid(t, w, nwell_width);
            makeLayersRuleSurround(t, w, diffPLayer, nwell_overhang_diff_p);
            makeLayersRuleSurround(t, w, diffNLayer, nwell_overhang_diff_n);
            makeLayersRule(t, w, DRCTemplate.DRCRuleType.SPACING, nwell_spacing);
        }

        Xml.Layer[] polys = {polyLayer, polyGateLayer};
        for (Xml.Layer w : polys)
        {
            makeLayerRuleMinWid(t, w, poly_width);
            makeLayersRule(t, w, DRCTemplate.DRCRuleType.SPACING, poly_spacing);
        }

        for (int i = 0; i < num_metal_layers; i++) {
            Xml.Layer met = metalLayers.get(i);
            makeLayerRuleMinWid(t, met, metal_width[i]);

            if (i >= num_metal_layers - 1) continue;
            Xml.Layer via = viaLayers.get(i);
            makeLayerRuleMinWid(t, via, via_size[i]);
            makeLayersRule(t, via, DRCTemplate.DRCRuleType.CONSPA, via_inline_spacing[i]);
            makeLayersRule(t, via, DRCTemplate.DRCRuleType.UCONSPA2D, via_array_spacing[i]);
        }

        // Finish menu with Pure, Misc and Cell
        List<Object> l = new ArrayList<Object>();
        l.add(new String("Pure"));
        t.menuPalette.menuBoxes.add(l);
        l = new ArrayList<Object>();
        l.add(new String("Misc."));
        t.menuPalette.menuBoxes.add(l);
        l = new ArrayList<Object>();
        l.add(new String("Cell"));
        t.menuPalette.menuBoxes.add(l);


        // write finally the file
        boolean includeDateAndVersion = User.isIncludeDateAndVersionInOutput();
        String copyrightMessage = IOTool.isUseCopyrightMessage() ? IOTool.getCopyrightMessage() : null;
        t.writeXml(fileName, includeDateAndVersion, copyrightMessage);
    }

    private void prepareTransistor(double gateWidth, double gateLength, double polyEndcap, double diffPolyOverhang,
                                   double gateContactSpacing, double contactSize,
                                   Xml.Layer activeLayer, Xml.Layer polyLayer, Xml.Layer polyGateLayer,
                                   List<Xml.NodeLayer> nodesList, List<Xml.PrimitivePort> nodePorts)
    {
        double impx = scaledValue((gateWidth)/2);
        double impy = scaledValue((gateLength+diffPolyOverhang*2)/2);
        double diffY = scaledValue(gateLength/2+gateContactSpacing+contactSize/2);  // impy
        double diffX = 0;
        double xSign = 1, ySign = -1;

        // Active layers
        nodesList.add(makeXmlNodeLayer(impx, impx, impy, impy, activeLayer, Poly.Type.FILLED, true, false, -1));
        // electrical active layers
        nodesList.add(makeXmlNodeLayer(impx, impx, impy, 0, activeLayer, Poly.Type.FILLED, false, true, 3));  // bottom
        nodesList.add(makeXmlNodeLayer(impx, impx, 0, impy, activeLayer, Poly.Type.FILLED, false, true, 1));  // top

        // Diff port
        List<String> portNames = new ArrayList<String>();
        portNames.add(activeLayer.name);

        // top port
        Xml.PrimitivePort diffTopPort = makeXmlPrimitivePort("diff-top", 90, 90, 1, null,
            diffX, -1, diffX, 1, diffY, 1, diffY, 1, portNames);
        // bottom port
        Xml.PrimitivePort diffBottomPort = makeXmlPrimitivePort("diff-bottom", 270, 90, 2, null,
            xSign*diffX, -1, xSign*diffX, 1, ySign*diffY, -1, ySign*diffY, -1, portNames);

        // Electric layers
        // Gate layer Electrical
        double gatey = scaledValue(gateLength/2);
        double gatex = impx;
        double endPolyx = scaledValue((gateWidth+polyEndcap*2)/2);
        double endPolyy = gatey;
        double endLeftOrRight = -impx;
        double endTopOrBotton = endPolyy;
        double polyX = endPolyx;
        double polyY = 0;
        nodesList.add(makeXmlNodeLayer(gatex, gatex, gatey, gatey, polyGateLayer, Poly.Type.FILLED, false, true, -1));

        // Poly layers
        // left electrical
        nodesList.add(makeXmlNodeLayer(endPolyx, endLeftOrRight, endPolyy, endTopOrBotton, polyLayer,
            Poly.Type.FILLED, false, true, 0));
        // right electrical
        nodesList.add(makeXmlNodeLayer(endLeftOrRight, endPolyx, endTopOrBotton, endPolyy, polyLayer,
            Poly.Type.FILLED, false, true, 2));

        // non-electrical poly (just one poly layer)
        nodesList.add(makeXmlNodeLayer(endPolyx, endPolyx, endPolyy, endPolyy, polyLayer, Poly.Type.FILLED, true, false, -1));

        // Poly port
        portNames.clear();
        portNames.add(polyLayer.name);
        Xml.PrimitivePort polyLeftPort = makeXmlPrimitivePort("poly-left", 180, 90, 0, null,
            ySign*polyX, -1, ySign*polyX,
            -1, xSign*polyY, -1, xSign*polyY, 1, portNames);
        // right port
        Xml.PrimitivePort polyRightPort = makeXmlPrimitivePort("poly-right", 0, 180, 0, null,
            polyX, 1, polyX, 1, polyY, -1, polyY, 1, portNames);

        nodePorts.clear();
        nodePorts.add(polyLeftPort);
        nodePorts.add(diffTopPort);
        nodePorts.add(polyRightPort);
        nodePorts.add(diffBottomPort);
    }

    private Xml.ArcLayer makeXmlArcLayer(Xml.Layer layer, WizardField ... flds) {
        Xml.ArcLayer al = new Xml.ArcLayer();
        al.layer = layer.name;
        al.style = Poly.Type.FILLED;
        for (int i = 0; i < flds.length; i++)
            al.extend.addLambda(scaledValue(flds[i].v/2));
        return al;
    }

//    private Technology.Distance makeXmlDistance(WizardField ... flds) {
//        Technology.Distance dist = new Technology.Distance();
//        dist.addRule(flds[0].rule, 0.5);
//        for (int i = 1; i < flds.length; i++)
//            dist.addRule(flds[i].rule, 1);
//        return dist;
//    }

    private void makeLayerGDS(Xml.Technology t, Xml.Layer l, String gdsVal) {
        for (Xml.Foundry f: t.foundries) {
            f.layerGds.put(l.name, gdsVal);
        }
    }

    private void makeLayerRuleMinWid(Xml.Technology t, Xml.Layer l, WizardField fld) {
        for (Xml.Foundry f: t.foundries) {
            f.rules.add(new DRCTemplate(fld.rule, DRCTemplate.DRCMode.ALL.mode(), DRCTemplate.DRCRuleType.MINWID,
                l.name, null, new double[] {scaledValue(fld.v)}, null, null));
        }
    }

    private void makeLayersRule(Xml.Technology t, Xml.Layer l, DRCTemplate.DRCRuleType ruleType, WizardField fld) {
        for (Xml.Foundry f: t.foundries) {
            f.rules.add(new DRCTemplate(fld.rule, DRCTemplate.DRCMode.ALL.mode(), ruleType,
                l.name, l.name, new double[] {scaledValue(fld.v)}, null, null));
        }
    }

    private void makeLayersRuleSurround(Xml.Technology t, Xml.Layer l1, Xml.Layer l2, WizardField fld) {
        double value = scaledValue(fld.v);
        for (Xml.Foundry f: t.foundries) {
            f.rules.add(new DRCTemplate(fld.rule, DRCTemplate.DRCMode.ALL.mode(), DRCTemplate.DRCRuleType.SURROUND,
                l1.name, l2.name, new double[] {value, value}, null, null));
        }
    }

    private double scaledValue(double val) { return DBMath.round(val / stepsize); }
}
