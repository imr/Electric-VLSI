/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: XmlInit.java
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

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import com.sun.electric.util.CollectionFactory;
import com.sun.electric.util.config.model.Injection;
import com.sun.electric.util.config.model.Parameter;

/**
 * @author fschmidt
 * 
 */
public class XmlInitSax extends InitStrategy {

    private static Logger logger = Logger.getLogger(XmlInitSax.class.getName());

    private File xmlFile;
    private static final String SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";
    private static final String XML_SCHEMA = "http://www.w3.org/2001/XMLSchema";

    public XmlInitSax(String xmlFile) {
        this.xmlFile = new File(xmlFile);
    }

    public XmlInitSax(URL xmlFile) {
        try {
            this.xmlFile = new File(xmlFile.toURI());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void init(EConfig config) {
        Map<String, Injection> injections = CollectionFactory.createHashMap();
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setValidating(true);

            SAXParser saxParser = factory.newSAXParser();
            saxParser.setProperty(SCHEMA_LANGUAGE, XML_SCHEMA);
            saxParser.parse(xmlFile, new ParserHandler(injections));
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        //XmlConfigVerification.

        for (Injection in : injections.values()) {
            try {
                logger.log(Level.FINE, "bind injection: " + in.getName() + " to " + in.getImplementation());
                config.addConfigEntry(Class.forName(in.getName()), in.createConfigEntry());
            } catch (ClassNotFoundException e) {
                logger.log(Level.SEVERE, "unable to bind injection: " + in.getName(), e);
            }
            
        }

    }

    private static class ParserHandler extends DefaultHandler {

        private enum XmlTags {
            configuration, injection, parameters, parameter
        }

        private Injection currentInjection = null;
        private List<Parameter> currentParameters = null;
        private Parameter currentParameter = null;
        private Map<String, Injection> injections = null;

        public ParserHandler(Map<String, Injection> injections) {
            this.injections = injections;
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String,
         * java.lang.String, java.lang.String, org.xml.sax.Attributes)
         */
        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes)
                throws SAXException {

            if (localName.equals(XmlTags.configuration.toString())) {
            } else if (localName.equals(XmlTags.injection.toString())) {
                this.parseInjectionTag(attributes);
            } else if (localName.equals(XmlTags.parameters.toString())) {
                this.parseParametersTag(attributes);
            } else if (localName.equals(XmlTags.parameter.toString())) {
                this.parseParameterTag(attributes);
            } else {
                logger.log(Level.SEVERE, "unexpected opening tag: " + localName);
            }
        }

        private void parseInjectionTag(Attributes attributes) throws SAXException {
            if (currentInjection != null) {
                throw new SAXException("error: closing tag expected");
            } else {
                String name = attributes.getValue(Injection.Attributes.name.toString());
                String impl = attributes.getValue(Injection.Attributes.implementation.toString());
                String factoryMethod = attributes.getValue(Injection.Attributes.factoryMethod.toString());

                currentInjection = new Injection(name, impl, factoryMethod);
            }
        }

        private void parseParametersTag(Attributes attributes) throws SAXException {
            if (currentParameters != null) {
                throw new SAXException("error: closing tag expected");
            } else {
                currentParameters = CollectionFactory.createArrayList();
            }
        }

        private void parseParameterTag(Attributes attributes) throws SAXException {
            if (currentParameter != null) {
                throw new SAXException("error: closing tag expected");
            } else {
                String name = attributes.getValue(Parameter.Attributes.name.toString());
                String ref = attributes.getValue(Parameter.Attributes.ref.toString());
                String type = attributes.getValue(Parameter.Attributes.type.toString());
                String value = attributes.getValue(Parameter.Attributes.value.toString());

                currentParameter = new Parameter(name, ref, value, Parameter.Type.valueOf(type));
            }
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String,
         * java.lang.String, java.lang.String)
         */
        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (localName.equals(XmlTags.configuration.toString())) {
            } else if (localName.equals(XmlTags.injection.toString())) {
                injections.put(currentInjection.getName(), currentInjection);
                currentInjection = null;
            } else if (localName.equals(XmlTags.parameters.toString())) {
                currentInjection.setParameters(currentParameters);
                currentParameters = null;
            } else if (localName.equals(XmlTags.parameter.toString())) {
                currentParameters.add(currentParameter);
                currentParameter = null;
            } else {
                logger.log(Level.SEVERE, "unexpected closing tag: " + localName);
            }
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.xml.sax.helpers.DefaultHandler#error(org.xml.sax.SAXParseException
         * )
         */
        @Override
        public void error(SAXParseException e) throws SAXException {
            throw new SAXException(e);
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.xml.sax.helpers.DefaultHandler#warning(org.xml.sax.SAXParseException
         * )
         */
        @Override
        public void warning(SAXParseException e) throws SAXException {
            throw new SAXException(e);
        }

    }

}
