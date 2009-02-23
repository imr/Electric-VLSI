/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: TechFactory.java
 *
 * Copyright (c) 2003 Sun Microsystems and Static Free Software
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
package com.sun.electric.technology;

import com.sun.electric.Main;
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.text.Setting;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.ActivityLogger;

import java.net.URL;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 *
 */
public abstract class TechFactory {
    public final String techName;
    
    public static TechFactory fromXml(URL url, Xml.Technology xmlTech) {
        String techName = null;
        if (xmlTech != null)
            techName = xmlTech.techName;
        if (techName == null)
            techName = TextUtils.getFileNameWithoutExtension(url);
        return new FromXml(techName, url, xmlTech);
    }
    
    public Technology newInstance(Generic generic) {
        return newInstance(generic, Setting.getFactorySettingContext());
    }

    public Technology newInstance(Generic generic, Setting.Context settingContext) {
        Pref.delayPrefFlushing();
        Setting.Context oldSettingState = Setting.setSettingContextOfCurrentThread(null);
        try {
            Technology tech = newInstanceImpl(generic);
            tech.setup(settingContext);
            return tech;
        } catch (ClassNotFoundException e) {
            if (Job.getDebug())
                System.out.println("GNU Release can't find extra technologies");
        } catch (Exception e) {
            System.out.println("Exceptions while importing extra technologies");
            ActivityLogger.logException(e);
        } finally {
            Setting.setSettingContextOfCurrentThread(oldSettingState);
            Pref.resumePrefFlushing();
        }
        return null;
    }
    
    public static Map<String,TechFactory> getKnownTechs(String softTechnologies) {
        LinkedHashMap<String,TechFactory> m = new LinkedHashMap<String,TechFactory>();
        c(m, "artwork", "com.sun.electric.technology.technologies.Artwork");
        c(m, "fpga", "com.sun.electric.technology.technologies.FPGA");
        c(m, "schematic", "com.sun.electric.technology.technologies.Schematics");
        r(m, "bicmos",     "technology/technologies/bicmos.xml");
        r(m, "bipolar",      "technology/technologies/bipolar.xml");
        r(m, "cmos",         "technology/technologies/cmos.xml");
        c(m, "efido",     "com.sun.electric.technology.technologies.EFIDO");
        c(m, "gem",       "com.sun.electric.technology.technologies.GEM");
        c(m, "pcb",       "com.sun.electric.technology.technologies.PCB");
        c(m, "rcmos",     "com.sun.electric.technology.technologies.RCMOS");
        if (true) {
            c(m, "mocmos","com.sun.electric.technology.technologies.MoCMOS");
        } else {
            r(m, "mocmos",   "technology/technologies/mocmos.xml");
        }
        r(m, "mocmosold",    "technology/technologies/mocmosold.xml");
        r(m, "mocmossub",    "technology/technologies/mocmossub.xml");
        r(m, "nmos",         "technology/technologies/nmos.xml");
        r(m, "tft",          "technology/technologies/tft.xml");
        r(m, "tsmc180",      "plugins/tsmc/tsmc180.xml");
//        r("tsmc45",        "plugins/tsmc/tsmc45.xml");
        if (true) {
            c(m, "cmos90","com.sun.electric.plugins.tsmc.CMOS90");
        } else {
            r(m, "cmos90",   "plugins/tsmc/cmos90.xml");
        }
		for(String softTechFile: softTechnologies.split(";")) {
//		for(String softTechFile: ToolSettings.getSoftTechnologiesSetting().getString().split(";")) {
			if (softTechFile.length() == 0) continue;
        	URL url = TextUtils.makeURLToFile(softTechFile);
        	if (TextUtils.URLExists(url))
        	{
	        	String softTechName = TextUtils.getFileNameWithoutExtension(url);
	        	m.put(softTechName, fromXml(url, null));
        	} else
        	{
        		System.out.println("WARNING: could not find added technology: " + softTechFile);
        		System.out.println("  (fix this error in the 'Added Technologies' Project Settings)");
        	}
        }
        return Collections.unmodifiableMap(m);
    }

    public static TechFactory getTechFactory(String techName) { return getKnownTechs("").get(techName); }
    
    TechFactory(String techName) {
        this.techName = techName;
    }
    
    private static void c(Map<String,TechFactory> m, String techName, String techClassName) {
        m.put(techName, new FromClass(techName, techClassName));
    }

    private static void r(Map<String,TechFactory> m, String techName, String resourceName) {
        m.put(techName, fromXml(Main.class.getResource(resourceName), null));
    }
    
    abstract Technology newInstanceImpl(Generic generic) throws Exception;

    private static class FromClass extends TechFactory {
        private final String techClassName;
        
        private FromClass(String techName, String techClassName) {
            super(techName);
            this.techClassName = techClassName;
        }
      
        @Override
        Technology newInstanceImpl(Generic generic) throws Exception {
            Class<?> techClass = Class.forName(techClassName);
            return (Technology)techClass.getConstructor(Generic.class).newInstance(generic);
        }
    }

    private static class FromXml extends TechFactory {
        private final URL urlXml;
        private Xml.Technology xmlTech;
        private boolean xmlParsed;
        
        private FromXml(String techName, URL urlXml, Xml.Technology xmlTech) {
            super(techName);
            this.urlXml = urlXml;
            this.xmlTech = xmlTech;
        }

        @Override
        Technology newInstanceImpl(Generic generic) throws Exception {
            if (xmlTech == null && !xmlParsed) {
                xmlTech = Xml.parseTechnology(urlXml);
                xmlParsed = true;
                if (xmlTech == null)
                    throw new Exception("Can't load extra technology: " + urlXml);
                if (!xmlTech.techName.equals(techName)) {
                    String techName = xmlTech.techName;
                    xmlTech = null;
                    throw new Exception("Tech name " + techName + " doesn't match file name:" +urlXml);
                }
            }
            if (xmlTech == null)
                return null;
            Class<?> techClass = Technology.class;
            if (xmlTech.className != null)
                techClass = Class.forName(xmlTech.className);
            return (Technology)techClass.getConstructor(Generic.class, Xml.Technology.class).newInstance(generic, xmlTech);
        }
    }
}
