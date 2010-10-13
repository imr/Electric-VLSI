/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: SpringConfig.java
 *
 * Copyright (c) 2010 Sun Microsystems and Static Free Software
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
package com.sun.electric.util.config;

import com.sun.electric.Launcher;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 *
 * @author dn146861
 */
public class ClasspathConfig extends Configuration {

    private static class Dependency {
        private final String className;
        private final String factoryMethod;
        
        private Dependency(String className, String factoryMethod) {
            this.className = className;
            this.factoryMethod = factoryMethod;
        }
    }
    
    private class ConfigHandler extends DefaultHandler {
        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes)
                throws SAXException {
            if ("bean".equals(qName)) {
                String id = attributes.getValue("id");
                String className = attributes.getValue("class");
                String factoryMethod = attributes.getValue("factory-method");
                if (id != null && className != null && factoryMethod != null) {
                    dependencies.put(id, new Dependency(className, factoryMethod));
                }
            }
        }
    }
    
    private HashMap<String,Dependency> dependencies = new HashMap<String,Dependency>(); 
    
    public ClasspathConfig() {
        this(Launcher.class.getClassLoader().getResource("configuration.xml"));
    }
    
    public ClasspathConfig(URL configURL) {
        try {
            parseXml(configURL.openConnection().getInputStream());
        } catch (Exception e) {
            Logger.getLogger(Launcher.class.getName()).log(Level.SEVERE, "Error parsing " + configURL, e);
        }
    }
    
    private void parseXml(InputStream in) throws ParserConfigurationException, SAXException, IOException {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser parser = factory.newSAXParser();
        parser.parse(in, new ConfigHandler());
    }
    
    @Override
    protected Object lookupImpl(String name) {
        Dependency dependency = dependencies.get(name);
        try {
            Class cls = Launcher.classFromPlugins(dependency.className);
            Method method = cls.getMethod(dependency.factoryMethod);
            Object result = method.invoke(null);
            return result;
        } catch (ClassNotFoundException e) {
            return null;
        } catch (NoSuchMethodException e) {
            return null;
        } catch (IllegalAccessException e) {
            return null;
        } catch (InvocationTargetException e) {
            return null;
        }
    }
}
