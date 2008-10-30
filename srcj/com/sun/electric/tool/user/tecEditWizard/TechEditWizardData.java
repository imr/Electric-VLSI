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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Class to handle the "Technology Creation Wizard" dialog.
 */
public class TechEditWizardData
{
	/************************************** THE DATA **************************************/

	private String tech_name;
	private String tech_description;
	private int num_metal_layers;
	private int stepsize;

	// DIFFUSION RULES
	private WizardField diff_width = new WizardField();
	private WizardField diff_poly_overhang = new WizardField();		// min. diff overhang from gate edge
	private WizardField diff_contact_overhang = new WizardField();	// min. diff overhang contact
	private WizardField diff_spacing = new WizardField();

	// POLY RULES
	private WizardField poly_width = new WizardField();
	private WizardField poly_endcap = new WizardField();			// min. poly gate extension from edge of diffusion
	private WizardField poly_spacing = new WizardField();
	private WizardField poly_diff_spacing = new WizardField();		// min. spacing between poly and diffusion

	// GATE RULES
	private WizardField gate_length = new WizardField();			// min. transistor gate length
	private WizardField gate_width = new WizardField();				// min. transistor gate width
	private WizardField gate_spacing = new WizardField();			// min. gate to gate spacing on diffusion
	private WizardField gate_contact_spacing = new WizardField();	// min. spacing from gate edge to contact inside diffusion

	// CONTACT RULES
	private WizardField contact_size = new WizardField();
	private WizardField contact_spacing = new WizardField();
    private WizardField contact_array_spacing = new WizardField();
    private WizardField contact_metal_overhang_inline_only = new WizardField();	// metal overhang when overhanging contact from two sides only
	private WizardField contact_metal_overhang_all_sides = new WizardField();	// metal overhang when surrounding contact
	private WizardField contact_poly_overhang = new WizardField();				// poly overhang contact
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
	private WizardField [] via_spacing;
	private WizardField [] via_array_spacing;
	private WizardField [] via_overhang_inline;

	// ANTENNA RULES
	private double poly_antenna_ratio;
	private double [] metal_antenna_ratio;

	// GDS-II LAYERS
	private int gds_diff_layer;
	private int gds_poly_layer;
	private int gds_nplus_layer;
	private int gds_pplus_layer;
	private int gds_nwell_layer;
	private int gds_contact_layer;
	private int [] gds_metal_layer;
	private int [] gds_via_layer;
	private int gds_marking_layer;		// Device marking layer

	public TechEditWizardData()
	{
		stepsize = 100;
		num_metal_layers = 2;
		metal_width = new WizardField[num_metal_layers];
		metal_spacing = new WizardField[num_metal_layers];
		via_size = new WizardField[num_metal_layers-1];
		via_spacing = new WizardField[num_metal_layers-1];
		via_array_spacing = new WizardField[num_metal_layers-1];
		via_overhang_inline = new WizardField[num_metal_layers-1];
		metal_antenna_ratio = new double[num_metal_layers];
		gds_metal_layer = new int[num_metal_layers];
		gds_via_layer = new int[num_metal_layers-1];
		for(int i=0; i<num_metal_layers; i++)
		{
			metal_width[i] = new WizardField();
			metal_spacing[i] = new WizardField();
		}
		for(int i=0; i<num_metal_layers-1; i++)
		{
			via_size[i] = new WizardField();
			via_spacing[i] = new WizardField();
			via_array_spacing[i] = new WizardField();
			via_overhang_inline[i] = new WizardField();
		}
	}

	/************************************** ACCESSOR METHODS **************************************/

	public String getTechName() { return tech_name; }
	public void setTechName(String s) { tech_name = s; }

	public String getTechDescription() { return tech_description; }
	public void setTechDescription(String s) { tech_description = s; }

	public int getStepSize() { return stepsize; }
	public void setStepSize(int n) { stepsize = n; }

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
		for(int i=0; i<smallest-1; i++) new_via_spacing[i] = via_spacing[i];
		for(int i=smallest-1; i<n-1; i++) new_via_spacing[i] = new WizardField();
		via_spacing = new_via_spacing;

		WizardField [] new_via_array_spacing = new WizardField[n-1];
		for(int i=0; i<smallest-1; i++) new_via_array_spacing[i] = via_array_spacing[i];
		for(int i=smallest-1; i<n-1; i++) new_via_array_spacing[i] = new WizardField();
		via_array_spacing = new_via_array_spacing;

		WizardField [] new_via_overhang_inline = new WizardField[n-1];
		for(int i=0; i<smallest-1; i++) new_via_overhang_inline[i] = via_overhang_inline[i];
		for(int i=smallest-1; i<n-1; i++) new_via_overhang_inline[i] = new WizardField();
		via_overhang_inline = new_via_overhang_inline;

		double [] new_metal_antenna_ratio = new double[n];
		for(int i=0; i<smallest; i++) new_metal_antenna_ratio[i] = metal_antenna_ratio[i];
		metal_antenna_ratio = new_metal_antenna_ratio;

		int [] new_gds_metal_layer = new int[n];
		for(int i=0; i<smallest; i++) new_gds_metal_layer[i] = gds_metal_layer[i];
		gds_metal_layer = new_gds_metal_layer;

		int [] new_gds_via_layer = new int[n-1];
		for(int i=0; i<smallest-1; i++) new_gds_via_layer[i] = gds_via_layer[i];
		gds_via_layer = new_gds_via_layer;

		num_metal_layers = n;
	}

	// DIFFUSION RULES
	public WizardField getDiffWidth() { return diff_width; }
	public void setDiffWidth(WizardField v) { diff_width = v; }
	public WizardField getDiffPolyOverhang() { return diff_poly_overhang; }
	public void setDiffPolyOverhang(WizardField v) { diff_poly_overhang = v; }
	public WizardField getDiffContactOverhang() { return diff_contact_overhang; }
	public void setDiffContactOverhang(WizardField v) { diff_contact_overhang = v; }
	public WizardField getDiffSpacing() { return diff_spacing; }
	public void setDiffSpacing(WizardField v) { diff_spacing = v; }

	// POLY RULES
	public WizardField getPolyWidth() { return poly_width; }
	public void setPolyWidth(WizardField v) { poly_width = v; }
	public WizardField getPolyEndcap() { return poly_endcap; }
	public void setPolyEndcap(WizardField v) { poly_endcap = v; }
	public WizardField getPolySpacing() { return poly_spacing; }
	public void setPolySpacing(WizardField v) { poly_spacing = v; }
	public WizardField getPolyDiffSpacing() { return poly_diff_spacing; }
	public void setPolyDiffSpacing(WizardField v) { poly_diff_spacing = v; }

	// GATE RULES
	public WizardField getGateLength() { return gate_length; }
	public void setGateLength(WizardField v) { gate_length = v; }
	public WizardField getGateWidth() { return gate_width; }
	public void setGateWidth(WizardField v) { gate_width = v; }
	public WizardField getGateSpacing() { return gate_spacing; }
	public void setGateSpacing(WizardField v) { gate_spacing = v; }
	public WizardField getGateContactSpacing() { return gate_contact_spacing; }
	public void setGateContactSpacing(WizardField v) { gate_contact_spacing = v; }


    // CONTACT RULES
	public WizardField getContactSize() { return contact_size; }
	public void setContactSize(WizardField v) { contact_size = v; }
	public WizardField getContactSpacing() { return contact_spacing; }
	public void setContactSpacing(WizardField v) { contact_spacing = v; }
    public WizardField getContactArraySpacing() { return contact_array_spacing; }
	public void setContactArraySpacing(WizardField v) { contact_array_spacing = v; }
    public WizardField getContactMetalOverhangInlineOnly() { return contact_metal_overhang_inline_only; }
	public void setContactMetalOverhangInlineOnly(WizardField v) { contact_metal_overhang_inline_only = v; }
	public WizardField getContactMetalOverhangAllSides() { return contact_metal_overhang_all_sides; }
	public void setContactMetalOverhangAllSides(WizardField v) { contact_metal_overhang_all_sides = v; }
	public WizardField getContactPolyOverhang() { return contact_poly_overhang; }
	public void setContactPolyOverhang(WizardField v) { contact_poly_overhang = v; }
	public WizardField getPolyconDiffSpacing() { return polycon_diff_spacing; }
	public void setPolyconDiffSpacing(WizardField v) { polycon_diff_spacing = v; }

	// WELL AND IMPLANT RULES
	public WizardField getNPlusWidth() { return nplus_width; }
	public void setNPlusWidth(WizardField v) { nplus_width = v; }
	public WizardField getNPlusOverhangDiff() { return nplus_overhang_diff; }
    public void setNPlusOverhangDiff(WizardField v) { nplus_overhang_diff = v; }
    public WizardField getNPlusOverhangPoly() { return nplus_overhang_poly; }
    public void setNPlusOverhangPoly(WizardField v) { nplus_overhang_poly = v; }
	public WizardField getNPlusSpacing() { return nplus_spacing; }
	public void setNPlusSpacing(WizardField v) { nplus_spacing = v; }

	public WizardField getPPlusWidth() { return pplus_width; }
	public void setPPlusWidth(WizardField v) { pplus_width = v; }
	public WizardField getPPlusOverhangDiff() { return pplus_overhang_diff; }
	public void setPPlusOverhangDiff(WizardField v) { pplus_overhang_diff = v; }
	public WizardField getPPlusOverhangPoly() { return pplus_overhang_poly; }
	public void setPPlusOverhangPoly(WizardField v) { pplus_overhang_poly = v; }
	public WizardField getPPlusSpacing() { return pplus_spacing; }
	public void setPPlusSpacing(WizardField v) { pplus_spacing = v; }

	public WizardField getNWellWidth() { return nwell_width; }
	public void setNWellWidth(WizardField v) { nwell_width = v; }
	public WizardField getNWellOverhangDiffP() { return nwell_overhang_diff_p; }
	public void setNWellOverhangDiffP(WizardField v) { nwell_overhang_diff_p = v; }
	public WizardField getNWellOverhangDiffN() { return nwell_overhang_diff_n; }
	public void setNWellOverhangDiffN(WizardField v) { nwell_overhang_diff_n = v; }
	public WizardField getNWellSpacing() { return nwell_spacing; }
	public void setNWellSpacing(WizardField v) { nwell_spacing = v; }

	// METAL RULES
	public WizardField [] getMetalWidth() { return metal_width; }
	public void setMetalWidth(int met, WizardField value) { metal_width[met] = value; }
	public WizardField [] getMetalSpacing() { return metal_spacing; }
	public void setMetalSpacing(int met, WizardField value) { metal_spacing[met] = value; }

	// VIA RULES
	public WizardField [] getViaSize() { return via_size; }
	public void setViaSize(int via, WizardField value) { via_size[via] = value; }
	public WizardField [] getViaSpacing() { return via_spacing; }
	public void setViaSpacing(int via, WizardField value) { via_spacing[via] = value; }
	public WizardField [] getViaArraySpacing() { return via_array_spacing; }
	public void setViaArraySpacing(int via, WizardField value) { via_array_spacing[via] = value; }
	public WizardField [] getViaOverhangInline() { return via_overhang_inline; }
	public void setViaOverhangInline(int via, WizardField value) { via_overhang_inline[via] = value; }

	// ANTENNA RULES
	public double getPolyAntennaRatio() { return poly_antenna_ratio; }
	public void setPolyAntennaRatio(double v) { poly_antenna_ratio = v; }
	public double [] getMetalAntennaRatio() { return metal_antenna_ratio; }
	public void setMetalAntennaRatio(int met, double value) { metal_antenna_ratio[met] = value; }

	// GDS-II LAYERS
	public int getGDSDiff() { return gds_diff_layer; }
	public void setGDSDiff(int l) { gds_diff_layer = l; }
	public int getGDSPoly() { return gds_poly_layer; }
	public void setGDSPoly(int l) { gds_poly_layer = l; }
	public int getGDSNPlus() { return gds_nplus_layer; }
	public void setGDSNPlus(int l) { gds_nplus_layer = l; }
	public int getGDSPPlus() { return gds_pplus_layer; }
	public void setGDSPPlus(int l) { gds_pplus_layer = l; }
	public int getGDSNWell() { return gds_nwell_layer; }
	public void setGDSNWell(int l) { gds_nwell_layer = l; }
	public int getGDSContact() { return gds_contact_layer; }
	public void setGDSContact(int l) { gds_contact_layer = l; }
	public int [] getGDSMetal() { return gds_metal_layer; }
	public void setGDSMetal(int met, int l) { gds_metal_layer[met] = l; }
	public int [] getGDSVia() { return gds_via_layer; }
	public void setGDSVia(int via, int l) { gds_via_layer[via] = l; }
	public int getGDSMarking() { return gds_marking_layer; }
	public void setGDSMarking(int l) { gds_marking_layer = l; }

	public String errorInData()
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
					if (varName.equalsIgnoreCase("stepsize")) setStepSize(TextUtils.atoi(varValue)); else

					if (varName.equalsIgnoreCase("diff_width")) diff_width.v = TextUtils.atof(varValue); else
					if (varName.equalsIgnoreCase("diff_width_rule")) diff_width.rule = stripQuotes(varValue); else
					if (varName.equalsIgnoreCase("diff_poly_overhang")) diff_poly_overhang.v = TextUtils.atof(varValue); else
					if (varName.equalsIgnoreCase("diff_poly_overhang_rule")) diff_poly_overhang.rule = stripQuotes(varValue); else
					if (varName.equalsIgnoreCase("diff_contact_overhang")) diff_contact_overhang.v = TextUtils.atof(varValue); else
					if (varName.equalsIgnoreCase("diff_contact_overhang_rule")) diff_contact_overhang.rule = stripQuotes(varValue); else
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

					if (varName.equalsIgnoreCase("gate_length")) gate_length.v = TextUtils.atof(varValue); else
					if (varName.equalsIgnoreCase("gate_length_rule")) gate_length.rule = stripQuotes(varValue); else
					if (varName.equalsIgnoreCase("gate_width")) gate_width.v = TextUtils.atof(varValue); else
					if (varName.equalsIgnoreCase("gate_width_rule")) gate_width.rule = stripQuotes(varValue); else
					if (varName.equalsIgnoreCase("gate_spacing")) gate_spacing.v = TextUtils.atof(varValue); else
					if (varName.equalsIgnoreCase("gate_spacing_rule")) gate_spacing.rule = stripQuotes(varValue); else
					if (varName.equalsIgnoreCase("gate_contact_spacing")) gate_contact_spacing.v = TextUtils.atof(varValue); else
					if (varName.equalsIgnoreCase("gate_contact_spacing_rule")) gate_contact_spacing.rule = stripQuotes(varValue); else

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
					if (varName.equalsIgnoreCase("via_spacing")) fillWizardArray(varValue, via_spacing, num_metal_layers-1, false); else
					if (varName.equalsIgnoreCase("via_spacing_rule")) fillWizardArray(varValue, via_spacing, num_metal_layers-1, true); else
					if (varName.equalsIgnoreCase("via_array_spacing")) fillWizardArray(varValue, via_array_spacing, num_metal_layers-1, false); else
					if (varName.equalsIgnoreCase("via_array_spacing_rule")) fillWizardArray(varValue, via_array_spacing, num_metal_layers-1, true); else
					if (varName.equalsIgnoreCase("via_overhang_inline")) fillWizardArray(varValue, via_overhang_inline, num_metal_layers-1, false); else
					if (varName.equalsIgnoreCase("via_overhang_inline_rule")) fillWizardArray(varValue, via_overhang_inline, num_metal_layers-1, true); else

					if (varName.equalsIgnoreCase("poly_antenna_ratio")) setPolyAntennaRatio(TextUtils.atof(varValue)); else
					if (varName.equalsIgnoreCase("metal_antenna_ratio")) metal_antenna_ratio = makeDoubleArray(varValue); else

					if (varName.equalsIgnoreCase("gds_diff_layer")) setGDSDiff(TextUtils.atoi(varValue)); else
					if (varName.equalsIgnoreCase("gds_poly_layer")) setGDSPoly(TextUtils.atoi(varValue)); else
					if (varName.equalsIgnoreCase("gds_nplus_layer")) setGDSNPlus(TextUtils.atoi(varValue)); else
					if (varName.equalsIgnoreCase("gds_pplus_layer")) setGDSPPlus(TextUtils.atoi(varValue)); else
					if (varName.equalsIgnoreCase("gds_nwell_layer")) setGDSNWell(TextUtils.atoi(varValue)); else
					if (varName.equalsIgnoreCase("gds_contact_layer")) setGDSContact(TextUtils.atoi(varValue)); else
					if (varName.equalsIgnoreCase("gds_metal_layer")) gds_metal_layer = makeIntArray(varValue); else
					if (varName.equalsIgnoreCase("gds_via_layer")) gds_via_layer = makeIntArray(varValue); else
					if (varName.equalsIgnoreCase("gds_marking_layer")) setGDSMarking(TextUtils.atoi(varValue)); else
					{
						Job.getUserInterface().showErrorMessage("Unknown keyword '" + varName + "' on line " + lineReader.getLineNumber(),
							"Syntax Error In Technology File");
						break;
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

	private String stripQuotes(String str)
	{
		if (str.startsWith("\"") && str.endsWith("\""))
			return str.substring(1, str.length()-1);
		return str;
	}

	private int [] makeIntArray(String str)
	{
		WizardField [] foundArray = new WizardField[num_metal_layers];
		for(int i=0; i<num_metal_layers; i++) foundArray[i] = new WizardField();
		fillWizardArray(str, foundArray, num_metal_layers, false);
		int [] retArray = new int[foundArray.length];
		for(int i=0; i<foundArray.length; i++)
			retArray[i] = (int)foundArray[i].v;
		return retArray;
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
                 Job.getUserInterface().showErrorMessage("Invalid metal index: " + index,
						"Syntax Error In Technology File");
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

	/************************************** EXPORT RAW NUMBERS TO DISK **************************************/

	public void exportData()
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
		pw.println("#### Technology wizard data file");
		pw.println("####");
		pw.println("#### All dimensions in nanometers.");
		pw.println();
		pw.println("$tech_name = \"" + tech_name + "\";");
		pw.println("$tech_description = \"" + tech_description + "\";");
		pw.println("$num_metal_layers = " + num_metal_layers + ";");
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
		pw.println("$contact_poly_overhang = " + TextUtils.formatDouble(contact_poly_overhang.v) + ";                    # poly overhang contact");
		pw.println("$contact_poly_overhang_rule = \"" + contact_poly_overhang.rule + "\";                    # poly overhang contact");
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
			pw.print(TextUtils.formatDouble(via_spacing[i].v));
		}
		pw.println(");");
		pw.print("@via_spacing_rule = (");
		for(int i=0; i<num_metal_layers-1; i++)
		{
			if (i > 0) pw.print(", ");
			pw.print("\"" + via_spacing[i].rule + "\"");
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
			pw.print(TextUtils.formatDouble(via_overhang_inline[i].v));
		}
		pw.println(");");
		pw.print("@via_overhang_inline_rule = (");
		for(int i=0; i<num_metal_layers-1; i++)
		{
			if (i > 0) pw.print(", ");
			pw.print("\"" + via_overhang_inline[i].rule + "\"");
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
		pw.println("$gds_diff_layer = " + gds_diff_layer + ";");
		pw.println("$gds_poly_layer = " + gds_poly_layer + ";");
		pw.println("$gds_nplus_layer = " + gds_nplus_layer + ";");
		pw.println("$gds_pplus_layer = " + gds_pplus_layer + ";");
		pw.println("$gds_nwell_layer = " + gds_nwell_layer + ";");
		pw.println("$gds_contact_layer = " + gds_contact_layer + ";");
		pw.print("@gds_metal_layer = (");
		for(int i=0; i<num_metal_layers; i++)
		{
			if (i > 0) pw.print(", ");
			pw.print(gds_metal_layer[i]);
		}
		pw.println(");");
		pw.print("@gds_via_layer = (");
		for(int i=0; i<num_metal_layers-1; i++)
		{
			if (i > 0) pw.print(", ");
			pw.print(gds_via_layer[i]);
		}
		pw.println(");");
		pw.println();
		pw.println("## Device marking layer");
		pw.println("$gds_marking_layer = " + gds_marking_layer + ";");
		pw.println();
		pw.println("# End of techfile");
	}

	/************************************** WRITE XML FILE **************************************/

	public void writeXML()
	{
		String errorMessage = errorInData();
		if (errorMessage != null)
		{
			Job.getUserInterface().showErrorMessage("ERROR: " + errorMessage,
				"Missing Technology Data");
			return;
		}
		String fileName = OpenFile.chooseOutputFile(FileType.XML, "Technology XML File", "Technology.xml");
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
    private Xml.PrimitiveNode makeXmlPrimitivePin(List<Xml.PrimitiveNode> nodes, String name, double size,
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
        nodePorts.add(makeXmlPrimitivePort(name.toLowerCase(), 0, 180, 0, null, 0, 0, 0, 0, portNames));
        return makeXmlPrimitive(nodes, name + "-Pin", PrimitiveNode.Function.PIN, size, size, 0, 0,
                so, nodesList, nodePorts, null, true);
    }

    /**
     * Method to creat the XML version of a PrimitiveNode representing a contact
     * @return
     */
    private Xml.PrimitiveNode makeXmlPrimitiveCon(List<Xml.PrimitiveNode> nodes, String name, double size,
                                                  SizeOffset so, List<String> portNames, Xml.NodeLayer... list)
    {
        List<Xml.NodeLayer> nodesList = new ArrayList<Xml.NodeLayer>(list.length);
        List<Xml.PrimitivePort> nodePorts = new ArrayList<Xml.PrimitivePort>();

        for (Xml.NodeLayer lb : list)
        {
            if (lb == null) continue; // in case the pwell layer off
            nodesList.add(lb);
        }

        nodePorts.add(makeXmlPrimitivePort(name.toLowerCase(), 0, 180, 0, null, 0, 0, 0, 0, portNames));
        return makeXmlPrimitive(nodes, name + "-Con", PrimitiveNode.Function.CONTACT, size, size, 0, 0,
                so, nodesList, nodePorts, null, false);
    }

    /**
     * Method to create the XML version of a PrimitiveNode
     * @return
     */
    private Xml.PrimitiveNode makeXmlPrimitive(List<Xml.PrimitiveNode> nodes,
                                               String name, PrimitiveNode.Function function,
                                               double width, double height,
                                               double ppLeft, double ppBottom,
                                               SizeOffset so, List<Xml.NodeLayer> nodeLayers,
                                               List<Xml.PrimitivePort> nodePorts,
                                               PrimitiveNode.NodeSizeRule nodeSizeRule, boolean isArcsShrink)
    {
        Xml.PrimitiveNode n = new Xml.PrimitiveNode();

        n.name = name;
        n.function = function;
        n.shrinkArcs = isArcsShrink;
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
         EPoint minFullSize = EPoint.fromLambda(0.5*width, 0.5*height);
        EPoint topLeft = EPoint.fromLambda(ppLeft, ppBottom + height);
        EPoint size =  EPoint.fromLambda(width, height);

        double getDefWidth = width, getDefHeight = height;
        if (function == PrimitiveNode.Function.PIN && isArcsShrink) {
//            assert getNumPorts() == 1;
//            assert nodeSizeRule == null;
//            PrimitivePort pp = getPort(0);
//            assert pp.getLeft().getMultiplier() == -0.5 && pp.getRight().getMultiplier() == 0.5 && pp.getBottom().getMultiplier() == -0.5 && pp.getTop().getMultiplier() == 0.5;
//            assert pp.getLeft().getAdder() == -pp.getRight().getAdder() && pp.getBottom().getAdder() == -pp.getTop().getAdder();
            minFullSize = EPoint.fromLambda(ppLeft, ppBottom);
        }
//            DRCTemplate nodeSize = xmlRules.getRule(pnp.getPrimNodeIndexInTech(), DRCTemplate.DRCRuleType.NODSIZ);
//        SizeOffset so = getProtoSizeOffset();
        if (so != null &&
            (so.getLowXOffset() == 0 && so.getHighXOffset() == 0 &&
                so.getLowYOffset() == 0 && so.getHighYOffset() == 0))
            so = null;
        n.sizeOffset = so;
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
            n.nodeLayers.addAll(nodeLayers);
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
        n.specialType = PrimitiveNode.NORMAL; // getSpecialType();
//        if (getSpecialValues() != null)
//            n.specialValues = getSpecialValues().clone();
        if (nodeSizeRule != null) {
            n.nodeSizeRule = new Xml.NodeSizeRule();
            n.nodeSizeRule.width = nodeSizeRule.getWidth();
            n.nodeSizeRule.height = nodeSizeRule.getHeight();
            n.nodeSizeRule.rule = nodeSizeRule.getRuleName();
        }
//        n.spiceTemplate = "";//getSpiceTemplate();

        // ports
        n.ports.addAll(nodePorts);

        nodes.add(n);

        return n;
    }

    /**
     * Method to create the XML version of a ArcProto
     * @param name
     * @param function
     * @return
     */
    private Xml.ArcProto makeXmlArc(List<Xml.ArcProto> arcs, String name, com.sun.electric.technology.ArcProto.Function function,
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
        arcs.add(a);
        return a;
    }

    /**
     * Method to create the XML version of a Layer.
     * @return
     */
    private Xml.Layer makeXmlLayer(List<Xml.Layer> layers, Map<Xml.Layer, WizardField> layer_width, String name,
                                   Layer.Function function, int extraf, EGraphics graph, char cifLetter,
                                   WizardField width, boolean pureLayerNode, boolean pureLayerPortArc) {
        Xml.Layer l = new Xml.Layer();
        l.name = name;
        l.function = function;
        l.extraFunction = extraf;
        l.desc = graph;
        l.thick3D = 1;
        l.height3D = 1;
        l.mode3D = "NONE";
        l.factor3D = 1;
        l.cif = "C" + cifLetter + cifLetter;
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
            l.pureLayerNode.size.addLambda(scaledValue(width.v));
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
        layer_width.put(l, width);
        return l;
    }

    /**
     * Method to create the XML version of NodeLayer
     */
    private Xml.NodeLayer makeXmlNodeLayer(double lx, double hx, double ly, double hy, Xml.Layer lb, Poly.Type style, 
                                           boolean electricalLayers)
    {
        Xml.NodeLayer nl = new Xml.NodeLayer();
        nl.layer = lb.name;
        nl.style = style;
        nl.inLayers = true;
        nl.inElectricalLayers = electricalLayers;
        nl.representation = Technology.NodeLayer.BOX;
        nl.lx.k = -1; nl.hx.k = 1; nl.ly.k = -1; nl.hy.k = 1;
        nl.lx.addLambda(-lx); nl.hx.addLambda(hx); nl.ly.addLambda(-ly); nl.hy.addLambda(hy);
        return nl;
    }
    
    private Xml.NodeLayer makeXmlMulticut(Xml.Layer lb, double sizeRule, double sepRule, double sepRule2D) {
        Xml.NodeLayer nl = new Xml.NodeLayer();
        nl.layer = lb.name;
        nl.style = Poly.Type.FILLED;
        nl.inLayers = nl.inElectricalLayers = true;
        nl.representation = Technology.NodeLayer.MULTICUTBOX;
        nl.lx.k = -1; nl.hx.k = 1; nl.ly.k = -1; nl.hy.k = 1;

//        nl.sizeRule = sizeRule;
        nl.sizex = sizeRule;
        nl.sizey = sizeRule;
        nl.sep1d = sepRule;
        nl.sep2d = sepRule2D;
        return nl;
        
    }

    /**
     * Method to create the XML versio nof PrimitivePort
     * @return
     */
    private Xml.PrimitivePort makeXmlPrimitivePort(String name, int portAngle, int portRange, int portTopology,
                                                   EPoint minFullSize, double lx, double hx, double ly, double hy,
                                                   List<String> portArcs)
    {
        Xml.PrimitivePort ppd = new Xml.PrimitivePort();
        double lambdaX = (minFullSize != null) ? minFullSize.getLambdaX() : 0;
        double lambdaY = (minFullSize != null) ? minFullSize.getLambdaY() : 0;
        ppd.name = name;
        ppd.portAngle = portAngle;
        ppd.portRange = portRange;
        ppd.portTopology = portTopology;

        ppd.lx.k = -1; //getLeft().getMultiplier()*2;
        ppd.lx.addLambda(DBMath.round(lx + lambdaX*ppd.lx.k));
        ppd.hx.k = 1; //getRight().getMultiplier()*2;
        ppd.hx.addLambda(DBMath.round(hx + lambdaX*ppd.hx.k));
        ppd.ly.k = -1; // getBottom().getMultiplier()*2;
        ppd.ly.addLambda(DBMath.round(ly + lambdaY*ppd.ly.k));
        ppd.hy.k = 1; // getTop().getMultiplier()*2;
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
//        t.scaleRelevant = isScaleRelevant();
        t.defaultFoundry = "NONE";
        t.minResistance = 1.0;
        t.minCapacitance = 0.1;

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
		Color contact_colour = new Color(40,40,40);   // darker gray
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
        List<Xml.Layer> viaLayers = new ArrayList<Xml.Layer>();
        Map<Xml.Layer,WizardField> layer_width = new LinkedHashMap<Xml.Layer,WizardField>();
        int[] nullPattern = new int[] {0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
            0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000};
        int cifNumber = 0;

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
            Xml.Layer layer = makeXmlLayer(t.layers, layer_width, "Metal-"+metalNum, fun, 0, graph,
                (char)('A' + cifNumber++), metal_width[i], true, true);
            metalLayers.add(layer);
        }

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
            Layer.Function fun = Layer.Function.getContact(metalNum);
            if (fun == null)
                throw new IOException("invalid number of vias");
            viaLayers.add(makeXmlLayer(t.layers, layer_width, "Via-"+metalNum, fun, Layer.Function.CONMETAL,
                graph, (char)('A' + cifNumber++), via_size[i], true, false));
        }

        // Poly
        EGraphics graph = new EGraphics(false, false, null, 1, 0, 0, 0, 1, true, nullPattern);
        Xml.Layer polyLayer = makeXmlLayer(t.layers, layer_width, "Poly", Layer.Function.POLY1, 0, graph,
            (char)('A' + cifNumber++), poly_width, true, true);
        // PolyGate
        Xml.Layer polyGateLayer = makeXmlLayer(t.layers, layer_width, "PolyGate", Layer.Function.GATE, 0, graph,
            (char)('A' + cifNumber++), poly_width, false, false);

        // PolyCon and DiffCon
        graph = new EGraphics(false, false, null, 0, contact_colour.getRed(), contact_colour.getGreen(),
            contact_colour.getBlue(), 1, true, nullPattern);
        // PolyCon
        Xml.Layer polyConLayer = makeXmlLayer(t.layers, layer_width, "PolyCon", Layer.Function.CONTACT1,
            Layer.Function.CONPOLY, graph, (char)('A' + cifNumber++), contact_size, true, false);
        // DiffCon
        Xml.Layer diffConLayer = makeXmlLayer(t.layers, layer_width, "DiffCon", Layer.Function.CONTACT1,
            Layer.Function.CONDIFF, graph, (char)('A' + cifNumber++), contact_size, true, false);

        // P-Diff and N-Diff
        graph = new EGraphics(false, false, null, 2, 0, 0, 0, 1, true, nullPattern);
        // N-Diff
        Xml.Layer diffNLayer = makeXmlLayer(t.layers, layer_width, "N-Diff", Layer.Function.DIFFN, 0, graph,
            (char)('A' + cifNumber++), diff_width, true, true);
        // P-Diff
        Xml.Layer diffPLayer = makeXmlLayer(t.layers, layer_width, "P-Diff", Layer.Function.DIFFP, 0, graph,
            (char)('A' + cifNumber++), diff_width, true, true);

        // NPlus and PPlus
        int [] pattern = new int[] { 0x1010,   //    X       X
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
        // NPlus
        graph = new EGraphics(true, true, null, 0, nplus_colour.getRed(), nplus_colour.getGreen(),
            nplus_colour.getBlue(), 1, true, pattern);
        Xml.Layer nplusLayer = makeXmlLayer(t.layers, layer_width, "NPlus", com.sun.electric.technology.Layer.Function.IMPLANTN, 0, graph,
            (char)('A' + cifNumber++), nplus_width, true, false);
        // PPlus
        graph = new EGraphics(true, true, null, 0, pplus_colour.getRed(), pplus_colour.getGreen(),
            pplus_colour.getBlue(), 1, true, pattern);
        Xml.Layer pplusLayer = makeXmlLayer(t.layers, layer_width, "PPlus", Layer.Function.IMPLANTP, 0, graph,
            (char)('A' + cifNumber++), pplus_width, true, false);

        // N-Well
        pattern = new int[] { 0x0202,   //       X       X
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
        graph = new EGraphics(true, true, null, 0, nwell_colour.getRed(), nwell_colour.getGreen(),
            nwell_colour.getBlue(), 1, true, pattern);
        Xml.Layer nwellLayer = makeXmlLayer(t.layers, layer_width, "N-Well", Layer.Function.WELLN, 0, graph,
            (char)('A' + cifNumber++), nwell_width, true, false);
        Xml.Layer pwellLayer = makeXmlLayer(t.layers, layer_width, "P-Well", Layer.Function.WELLP, 0, graph,
            (char)('A' + cifNumber++), nwell_width, true, false);

        graph = new EGraphics(false, false, null, 0, 255, 0, 0, 0.4, true, nullPattern);
        // DeviceMark
        Xml.Layer deviceMarkLayer = makeXmlLayer(t.layers, layer_width, "DeviceMark", Layer.Function.CONTROL, 0, graph,
            (char)('A' + cifNumber++), nplus_width, true, false);

        // write arcs
        // metal arcs
        for(int i=1; i<=num_metal_layers; i++)
        {
            double ant = (int)Math.round(metal_antenna_ratio[i-1]) | 200;
            makeXmlArc(t.arcs, "Metal-"+i, ArcProto.Function.getContact(i), ant,
                makeXmlArcLayer(metalLayers.get(i-1), metal_width[i-1]));
        }

        List<String> portNames = new ArrayList<String>();
        /**************************** POLY Nodes/Arcs ***********************************************/
        // poly arc
        double ant = (int)Math.round(poly_antenna_ratio) | 200;
        makeXmlArc(t.arcs, "Poly", ArcProto.Function.getPoly(1), ant,
                makeXmlArcLayer(polyLayer, poly_width));
        // poly pin
        double hla = scaledValue(poly_width.v / 2);
        makeXmlPrimitivePin(t.nodes, polyLayer.name, hla, null, // new SizeOffset(hla, hla, hla, hla),
            makeXmlNodeLayer(hla, hla, hla, hla, polyLayer, Poly.Type.CROSSED, true));
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
        makeXmlPrimitiveCon(t.nodes, polyLayer.name, hla, null, portNames,
                makeXmlNodeLayer(metal1Over, metal1Over, metal1Over, metal1Over, m1Layer, Poly.Type.FILLED, true), // meta1 layer
                makeXmlNodeLayer(hla, hla, hla, hla, polyLayer, Poly.Type.FILLED, true), // poly layer
                makeXmlMulticut(diffConLayer, contSize, contSpacing, contArraySpacing)); // contact
        
        /**************************** N/P-Diff Nodes/Arcs ***********************************************/
    boolean pwellOn = User.isPWellProcessLayoutTechnology();

        // NDiff/PDiff arcs
        makeXmlArc(t.arcs, "N-Diff", ArcProto.Function.DIFFN, 0,
                makeXmlArcLayer(diffNLayer, diff_width),
                makeXmlArcLayer(nplusLayer, diff_width, nplus_overhang_diff),
            (pwellOn)?makeXmlArcLayer(pwellLayer, diff_width, nwell_overhang_diff_p):null);
        makeXmlArc(t.arcs, "P-Diff", ArcProto.Function.DIFFP, 0,
                makeXmlArcLayer(diffPLayer, diff_width),
                makeXmlArcLayer(pplusLayer, diff_width, pplus_overhang_diff),
                makeXmlArcLayer(nwellLayer, diff_width, nwell_overhang_diff_p));

        // ndiff/pdiff pins
        hla = scaledValue((contact_size.v/2 + diff_contact_overhang.v));
        double nsel = scaledValue(contact_size.v/2 + diff_contact_overhang.v + nplus_overhang_diff.v);
        double psel = scaledValue(contact_size.v/2 + diff_contact_overhang.v + pplus_overhang_diff.v);
        double nwell = scaledValue(contact_size.v/2 + diff_contact_overhang.v + nwell_overhang_diff_p.v);
        double nso = scaledValue(nwell_overhang_diff_p.v); // valid for elements that have nwell layers
        double pso = scaledValue(nplus_overhang_diff.v);

        makeXmlPrimitivePin(t.nodes, "N-Diff", hla,
            new SizeOffset(pso, pso, pso, pso),
            makeXmlNodeLayer(hla, hla, hla, hla, diffNLayer, Poly.Type.CROSSED, true),
            makeXmlNodeLayer(nsel, nsel, nsel, nsel, nplusLayer, Poly.Type.CROSSED, true),
            (pwellOn)?makeXmlNodeLayer(nwell, nwell, nwell, nwell, pwellLayer, Poly.Type.CROSSED, true):null);
        makeXmlPrimitivePin(t.nodes, "P-Diff", hla,
            new SizeOffset(nso, nso, nso, nso),
            makeXmlNodeLayer(hla, hla, hla, hla, diffPLayer, Poly.Type.CROSSED, true),
            makeXmlNodeLayer(psel, psel, psel, psel, pplusLayer, Poly.Type.CROSSED, true),
            makeXmlNodeLayer(nwell, nwell, nwell, nwell, nwellLayer, Poly.Type.CROSSED, true));

        // ndiff/pdiff contacts
        hla = scaledValue((contact_size.v/2 + diff_contact_overhang.v));
        portNames.clear();
        portNames.add(diffNLayer.name);
        portNames.add(m1Layer.name);
        // ndiff contact
        makeXmlPrimitiveCon(t.nodes, "N-Diff", hla, new SizeOffset(pso, pso, pso, pso), portNames,
                makeXmlNodeLayer(metal1Over, metal1Over, metal1Over, metal1Over, m1Layer, Poly.Type.FILLED, true), // meta1 layer
                makeXmlNodeLayer(hla, hla, hla, hla, diffNLayer, Poly.Type.FILLED, true), // active layer
                makeXmlNodeLayer(nsel, nsel, nsel, nsel, nplusLayer, Poly.Type.FILLED, true), // select layer
            (pwellOn)?makeXmlNodeLayer(nwell, nwell, nwell, nwell, pwellLayer, Poly.Type.FILLED, true):null,
                makeXmlMulticut(diffConLayer, contSize, contSpacing, contArraySpacing)); // contact
        // pdiff contact
        portNames.clear();
        portNames.add(diffPLayer.name);
        portNames.add(m1Layer.name);
        makeXmlPrimitiveCon(t.nodes, "P-Diff", hla, new SizeOffset(nso, nso, nso, nso), portNames,
                makeXmlNodeLayer(metal1Over, metal1Over, metal1Over, metal1Over, m1Layer, Poly.Type.FILLED, true), // meta1 layer
                makeXmlNodeLayer(hla, hla, hla, hla, diffPLayer, Poly.Type.FILLED, true), // active layer
                makeXmlNodeLayer(psel, psel, psel, psel, pplusLayer, Poly.Type.FILLED, true), // select layer
                makeXmlNodeLayer(nwell, nwell, nwell, nwell, nwellLayer, Poly.Type.FILLED, true), // well layer
                makeXmlMulticut(diffConLayer, contSize, contSpacing, contArraySpacing)); // contact

        /**************************** N/P-Well Contacts ***********************************************/
        nwell = scaledValue(contact_size.v/2 + diff_contact_overhang.v + nwell_overhang_diff_n.v);
        nso = scaledValue(nwell_overhang_diff_n.v); // valid for elements that have nwell layers

        // NWell/PWell arcs
        if (pwellOn)
        {
            makeXmlArc(t.arcs, "P-Well", ArcProto.Function.WELL, 0,
                makeXmlArcLayer(pwellLayer, diff_width, nwell_overhang_diff_p));
        }
        makeXmlArc(t.arcs, "N-Well", ArcProto.Function.WELL, 0,
                makeXmlArcLayer(nwellLayer, diff_width, nwell_overhang_diff_p));

        portNames.clear();
        if (pwellOn)
            portNames.add(pwellLayer.name);
        portNames.add(m1Layer.name);
        // pwell contact
        makeXmlPrimitiveCon(t.nodes, "P-Well", hla, new SizeOffset(pso, pso, pso, pso), portNames,
                makeXmlNodeLayer(metal1Over, metal1Over, metal1Over, metal1Over, m1Layer, Poly.Type.FILLED, true), // meta1 layer
                makeXmlNodeLayer(hla, hla, hla, hla, diffPLayer, Poly.Type.FILLED, true), // active layer
                makeXmlNodeLayer(nsel, psel, psel, psel, pplusLayer, Poly.Type.FILLED, true), // select layer
            (pwellOn)?makeXmlNodeLayer(nwell, nwell, nwell, nwell, pwellLayer, Poly.Type.FILLED, true):null,
                makeXmlMulticut(diffConLayer, contSize, contSpacing, contArraySpacing)); // contact
        // nwell contact
        portNames.clear();
        portNames.add(nwellLayer.name);
        portNames.add(m1Layer.name);
        makeXmlPrimitiveCon(t.nodes, "N-Well", hla, new SizeOffset(nso, nso, nso, nso), portNames,
                makeXmlNodeLayer(metal1Over, metal1Over, metal1Over, metal1Over, m1Layer, Poly.Type.FILLED, true), // meta1 layer
                makeXmlNodeLayer(hla, hla, hla, hla, diffNLayer, Poly.Type.FILLED, true), // active layer
                makeXmlNodeLayer(nsel, nsel, nsel, nsel, nplusLayer, Poly.Type.FILLED, true), // select layer
                makeXmlNodeLayer(nwell, nwell, nwell, nwell, nwellLayer, Poly.Type.FILLED, true), // well layer
                makeXmlMulticut(diffConLayer, contSize, contSpacing, contArraySpacing)); // contact

        /**************************** Metals Nodes/Arcs ***********************************************/
        // Pins and contacts
        for(int i=1; i<num_metal_layers; i++)
		{
            hla = scaledValue(metal_width[i-1].v / 2);
            Xml.Layer lb = metalLayers.get(i-1);

            // Pin bottom metal
            makeXmlPrimitivePin(t.nodes, lb.name, hla, null, //new SizeOffset(hla, hla, hla, hla),
                makeXmlNodeLayer(hla, hla, hla, hla, lb, Poly.Type.CROSSED, true));

            // Contact Square
            double metalW = via_size[i-1].v/2 + contact_metal_overhang_all_sides.v;
            hla = scaledValue(metalW);
            Xml.Layer lt = metalLayers.get(i);
            // via
            Xml.Layer via = viaLayers.get(i-1);
            double viaSize = scaledValue(via_size[i-1].v);
            double viaSpacing = scaledValue(via_spacing[i-1].v);
            double viaArraySpacing = scaledValue(via_array_spacing[i-1].v);
            String name = lb.name + "-" + lt.name;

            // Square contacts
            portNames.clear();
            portNames.add(lt.name);
            portNames.add(lb.name);
            makeXmlPrimitiveCon(t.nodes, name, hla, null /*new SizeOffset(hla, hla, hla, hla)*/, portNames,
                makeXmlNodeLayer(hla, hla, hla, hla, lb, Poly.Type.FILLED, true), // bottom layer
                makeXmlNodeLayer(hla, hla, hla, hla, lt, Poly.Type.FILLED, true), // top layer
                makeXmlMulticut(via, viaSize, viaSpacing, viaArraySpacing)); // via
        }

        /** Transistors **/
        // write the transistors
        List<Xml.NodeLayer> nodesList = new ArrayList<Xml.NodeLayer>();
        List<Xml.PrimitivePort> nodePorts = new ArrayList<Xml.PrimitivePort>();
        EPoint minFullSize = null; //EPoint.fromLambda(0, 0);  // default zero
		for(int i = 0; i < 2; i++)
        {
            String name;
            double selecty = 0, selectx = 0;
            Xml.Layer wellLayer = null, activeLayer, selectLayer;
            double sox = 0, soy = 0;
            double width = scaledValue((gate_width.v));
            double length = scaledValue((gate_length.v));
            double impx = scaledValue((gate_width.v)/2);
            double impy = scaledValue((gate_length.v+diff_poly_overhang.v*2)/2);
            double wellx = scaledValue((gate_width.v/2+nwell_overhang_diff_p.v));
            double welly = scaledValue((gate_length.v/2+diff_poly_overhang.v+nwell_overhang_diff_p.v));

            if (i==0)
			{
				name = "P";
                wellLayer = nwellLayer;
                activeLayer = diffPLayer;
                selectLayer = pplusLayer;
                sox = scaledValue(nwell_overhang_diff_p.v);
                soy = scaledValue(diff_poly_overhang.v+nwell_overhang_diff_p.v);
                selectx = scaledValue((gate_width.v/2+(poly_endcap.v+pplus_overhang_poly.v)));
                selecty = scaledValue((gate_length.v/2+diff_poly_overhang.v+pplus_overhang_diff.v));
            } else
			{
				name = "N";
                activeLayer = diffNLayer;
                selectLayer = nplusLayer;
                sox = scaledValue(poly_endcap.v+pplus_overhang_poly.v);
                soy = scaledValue(diff_poly_overhang.v+pplus_overhang_diff.v);
                selectx = scaledValue((gate_width.v/2+(poly_endcap.v+nplus_overhang_poly.v)));
                selecty = scaledValue((gate_length.v/2+diff_poly_overhang.v+nplus_overhang_diff.v));
            }
            nodesList.clear();
            nodePorts.clear();
            portNames.clear();

            // Well layer
            if (wellLayer != null)
                nodesList.add(makeXmlNodeLayer(wellx, wellx, welly, welly, wellLayer, Poly.Type.FILLED, true));

            // Active layers
            nodesList.add(makeXmlNodeLayer(impx, impx, impy, impy, activeLayer, Poly.Type.FILLED, true));
            // top port
            portNames.clear();
            portNames.add(activeLayer.name);
            nodePorts.add(makeXmlPrimitivePort("trans-diff-top", 90, 90, 0, minFullSize, 0, 0, impy, impy, portNames));
            // right port
            nodePorts.add(makeXmlPrimitivePort("trans-diff-bottom", 270, 90, 0, minFullSize, 0, 0, -impy, -impy, portNames));

            // Gate layer Electrical
            double gatey = scaledValue(gate_length.v/2);
            nodesList.add(makeXmlNodeLayer(impx, impx, gatey, gatey, polyGateLayer, Poly.Type.FILLED, true));

            // Poly layers
            // left electrical
            double endPoly = scaledValue((gate_width.v+poly_endcap.v*2)/2);
            nodesList.add(makeXmlNodeLayer(endPoly, -impx, gatey, gatey, polyLayer, Poly.Type.FILLED, true));
            // right electrical
            nodesList.add(makeXmlNodeLayer(-impx, endPoly, gatey, gatey, polyLayer, Poly.Type.FILLED, true));
            // non-electrical poly (just one poly layer)
            nodesList.add(makeXmlNodeLayer(endPoly, endPoly, gatey, gatey, polyLayer, Poly.Type.FILLED, false));

            // left port
            portNames.clear();
            portNames.add(polyLayer.name);
            nodePorts.add(makeXmlPrimitivePort("trans-poly-left", 180, 90, 0, minFullSize, -endPoly, -endPoly, 0, 0, portNames));
            // right port
            nodePorts.add(makeXmlPrimitivePort("trans-poly-right", 0, 180, 0, minFullSize, endPoly, endPoly, 0, 0, portNames));

            // Select layer
            nodesList.add(makeXmlNodeLayer(selectx, selectx, selecty, selecty, selectLayer, Poly.Type.FILLED, true));

            // Transistor
            makeXmlPrimitive(t.nodes, name + "-Transistor", PrimitiveNode.Function.TRANMOS, 0, 0, 0, 0,
                new SizeOffset(sox, sox, soy, soy), nodesList, nodePorts, null, false);
        }
        
        /** RULES **/
        Xml.Foundry f = new Xml.Foundry();
        f.name = Foundry.Type.NONE.getName();
        t.foundries.add(f);

        // Writting GDS values
        makeLayerGDS(t, diffPLayer, String.valueOf(gds_diff_layer));
        makeLayerGDS(t, diffNLayer, String.valueOf(gds_diff_layer));
        makeLayerGDS(t, pplusLayer, String.valueOf(gds_pplus_layer));
        makeLayerGDS(t, nplusLayer, String.valueOf(gds_nplus_layer));
        makeLayerGDS(t, nwellLayer, String.valueOf(gds_nwell_layer));
        makeLayerGDS(t, deviceMarkLayer, String.valueOf(gds_marking_layer));
        makeLayerGDS(t, polyConLayer, String.valueOf(gds_contact_layer));
        makeLayerGDS(t, diffConLayer, String.valueOf(gds_contact_layer));
        makeLayerGDS(t, polyLayer, String.valueOf(gds_poly_layer));
        makeLayerGDS(t, polyGateLayer, String.valueOf(gds_poly_layer));

        for (int i = 0; i < num_metal_layers; i++) {
            Xml.Layer met = metalLayers.get(i);
            makeLayerGDS(t, met, String.valueOf(gds_metal_layer[i]));

            if (i > num_metal_layers - 2) continue;

            Xml.Layer via = viaLayers.get(i);
            makeLayerGDS(t, via, String.valueOf(gds_via_layer[i]));
        }

        // Writting Layer Rules
        makeLayerRuleMinWid(t, diffPLayer, diff_width);
        makeLayerRuleMinWid(t, diffNLayer, diff_width);
        makeLayerRuleMinWid(t, pplusLayer, pplus_width);
        makeLayersRuleSurround(t, pplusLayer, diffPLayer, pplus_overhang_diff);
        makeLayerRuleMinWid(t, nplusLayer, nplus_width);
        makeLayersRuleSurround(t, nplusLayer, diffNLayer, nplus_overhang_diff);
        makeLayerRuleMinWid(t, nwellLayer, nwell_width);
        makeLayersRuleSurround(t, nwellLayer, diffPLayer, nwell_overhang_diff_p);
        makeLayersRuleSurround(t, nwellLayer, diffNLayer, nwell_overhang_diff_n);

        makeLayerRuleMinWid(t, polyLayer, poly_width);
        
        for (int i = 0; i < num_metal_layers; i++) {
            Xml.Layer met = metalLayers.get(i);
            makeLayerRuleMinWid(t, met, metal_width[i]);
            
            if (i >= num_metal_layers - 1) continue;
            Xml.Layer via = viaLayers.get(i);
            makeLayerRuleMinWid(t, via, via_size[i]);
            makeLayersRule(t, via, DRCTemplate.DRCRuleType.CONSPA, via_spacing[i]);
            makeLayersRule(t, via, DRCTemplate.DRCRuleType.UCONSPA2D, via_array_spacing[i]);
        }
        
        // write finally the file
        t.writeXml(fileName);
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
