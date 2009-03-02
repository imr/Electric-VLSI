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
import com.sun.electric.database.id.IdManager;
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.text.Setting;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.technology.TechFactory;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.ActivityLogger;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 *
 */
public abstract class TechFactory {
    final String techName;
    private final List<Param> techParams;

    public static class Param {
        public final String xmlPath;
        public final String prefPath;
        public final Object factoryValue;

        public Param(String xmlPath, String prefPath, Object factoryValue) {
            this.xmlPath = xmlPath;
            this.prefPath = prefPath;
            this.factoryValue = factoryValue;
        }
    }

    public static TechFactory fromXml(URL url, Xml.Technology xmlTech) {
        String techName = null;
        if (xmlTech != null)
            techName = xmlTech.techName;
        if (techName == null)
            techName = TextUtils.getFileNameWithoutExtension(url);
        return new FromXml(techName, url, xmlTech);
    }

    public Technology newInstance(Generic generic) {
        HashMap<String,Object> paramValues = new HashMap<String,Object>();
        for (Param param: techParams)
            paramValues.put(param.xmlPath, param.factoryValue);
        return newInstance(generic, paramValues, Setting.getFactorySettingContext());
    }

    public Technology newInstance(Generic generic, Map<String,Object> paramValues, Setting.Context settingContext) {
        Pref.delayPrefFlushing();
        Setting.Context oldSettingState = Setting.setSettingContextOfCurrentThread(null);
        try {
            Technology tech = newInstanceImpl(generic, paramValues);
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

    public List<Param> getTechParams() {
        return techParams;
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
        p(m, "mocmos","com.sun.electric.technology.technologies.MoCMOS");
        r(m, "mocmosold",    "technology/technologies/mocmosold.xml");
        r(m, "mocmossub",    "technology/technologies/mocmossub.xml");
        r(m, "nmos",         "technology/technologies/nmos.xml");
        r(m, "tft",          "technology/technologies/tft.xml");
        p(m, "tsmc180",      "com.sun.electric.plugins.tsmc.TSMC180");
//        r("tsmc45",        "plugins/tsmc/tsmc45.xml");
        p(m, "cmos90","com.sun.electric.plugins.tsmc.CMOS90");
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
        this(techName, Collections.<Param>emptyList());
    }

    TechFactory(String techName, List<Param> techParams) {
        this.techName = techName;
        this.techParams = Collections.unmodifiableList(new ArrayList<Param>(techParams));
    }

    private static void p(Map<String,TechFactory> m, String techName, String techClassName) {
        TechFactory techFactory;
        List<Param> params;
        try {
            Class<?> techClass = Class.forName(techClassName);
            Method getTechParamsMethod = techClass.getDeclaredMethod("getTechParams");
            getTechParamsMethod.setAccessible(true);
            params = (List<Param>)getTechParamsMethod.invoke(null);
            techFactory = new FromParamClass(techName, techClass, params);
        } catch (Exception e) {
            if (Job.getDebug())
                e.printStackTrace();
            return;
        }
        m.put(techName, techFactory);
    }

    private static void c(Map<String,TechFactory> m, String techName, String techClassName) {
        m.put(techName, new FromClass(techName, techClassName));
    }

    private static void r(Map<String,TechFactory> m, String techName, String resourceName) {
        URL url = Main.class.getResource(resourceName);
        if (url == null)
            return;
        m.put(techName, fromXml(url, null));
    }

    abstract Technology newInstanceImpl(Generic generic, Map<String,Object> paramValues) throws Exception;
    public abstract Xml.Technology getXml(final Map<String,Object> params) throws Exception;

    private static class FromClass extends TechFactory {
        private final String techClassName;

        private FromClass(String techName, String techClassName) {
            super(techName, Collections.<Param>emptyList());
            this.techClassName = techClassName;
        }

        @Override
        Technology newInstanceImpl(Generic generic, Map<String,Object> paramValues) throws Exception {
            assert paramValues.isEmpty();
            Class<?> techClass = Class.forName(techClassName);
            return (Technology)techClass.getConstructor(Generic.class).newInstance(generic);
        }

        @Override
        public Xml.Technology getXml(final Map<String,Object> params) throws Exception {
            IdManager idManager = new IdManager();
            Generic generic = Generic.newInstance(idManager);
            Setting.Context settingContext = new Setting.Context() {
                @Override public Object getValue(Setting setting) {
                    Object paramValue = params.get(setting.getXmlPath());
                    Object factoryValue = setting.getFactoryValue();
                    if (paramValue != null && paramValue.getClass() == factoryValue.getClass())
                        return paramValue;
                    return factoryValue;
                }
            };
            Technology tech = newInstance(generic, params, settingContext);
            return tech.makeXml();
        }
    }

    private static class FromParamClass extends TechFactory {
        private final Class<?> techClass;
        private final Method getPatchedXmlMethod;
        Constructor techConstructor;

        private FromParamClass(String techName, Class<?> techClass, List<Param> techParams) throws Exception {
            super(techName, techParams);
            this.techClass = techClass;
            getPatchedXmlMethod = techClass.getDeclaredMethod("getPatchedXml", Map.class);
            getPatchedXmlMethod.setAccessible(true);
            techConstructor = techClass.getConstructor(Generic.class, TechFactory.class, Map.class, Xml.Technology.class);
        }

        @Override
        Technology newInstanceImpl(Generic generic, Map<String,Object> paramValues) throws Exception {
            return (Technology)techConstructor.newInstance(generic, this, paramValues, getXml(paramValues));
        }

        @Override
        public Xml.Technology getXml(final Map<String,Object> params) throws Exception {
            return (Xml.Technology)getPatchedXmlMethod.invoke(null, params);
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
        Technology newInstanceImpl(Generic generic, Map<String,Object> paramValues) throws Exception {
            assert paramValues.isEmpty();
            Xml.Technology xml = getXml(paramValues);
            if (xml == null)
                return null;
            Class<?> techClass = Technology.class;
            if (xml.className != null)
                techClass = Class.forName(xml.className);
            return (Technology)techClass.getConstructor(Generic.class, TechFactory.class, Map.class, Xml.Technology.class).newInstance(generic, this, Collections.emptyMap(), xml);
        }

        public Xml.Technology getXml(final Map<String,Object> paramValues) throws Exception {
            assert paramValues.isEmpty();
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
            return xmlTech;
        }
    }
}
