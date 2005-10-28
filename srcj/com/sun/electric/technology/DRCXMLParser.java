/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: DRCTemplate.java
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
package com.sun.electric.technology;

import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.SAXParser;
import java.io.InputStream;
import java.io.IOException;
import java.net.URLConnection;
import java.net.URL;
import java.util.List;
import java.util.ArrayList;

import com.sun.electric.database.text.TextUtils;

/**
 * Created by IntelliJ IDEA.
 * User: gg151869
 * Date: Oct 19, 2005
 * Time: 4:36:26 PM
 * To change this template use File | Settings | File Templates.
 */
public class DRCXMLParser {

    public List<DRCTemplate> process(URL fileURL)
    {
        List<DRCTemplate> drcRules = new ArrayList<DRCTemplate>();
        try
        {

            // Factory call
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setValidating(true);
            // create the parser
            SAXParser parser = factory.newSAXParser();
            URLConnection urlCon = fileURL.openConnection();
			InputStream inputStream = urlCon.getInputStream();
			System.out.println("Parsing XML file ...");
            parser.parse(inputStream, new DRCXMLHandler(drcRules));

			System.out.println("End Parsing XML file ...");
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return drcRules;
    }

    private static class DRCXMLHandler extends DefaultHandler
    {
        private List drcRules = null;
        private DRCTemplate.DRCMode foundry = DRCTemplate.DRCMode.NONE;

        DRCXMLHandler(List drcList)
        {
            this.drcRules = drcList;
        }

        public InputSource resolveEntity (String publicId, String systemId) throws IOException, SAXException
        {
            URL fileURL = this.getClass().getResource("DRC.dtd");
            URLConnection urlCon = fileURL.openConnection();
			InputStream inputStream = urlCon.getInputStream();
            return new InputSource(inputStream);
        }

        public void startElement (String uri, String localName, String qName, Attributes attributes)
        {
            if (qName.equals("Foundry"))
            {
                foundry = DRCTemplate.DRCMode.valueOf(attributes.getValue(0));
                return;
            }
            boolean layerRule = qName.equals("LayerRule");
            boolean layersRule = qName.equals("LayersRule");
            boolean nodeLayersRule = qName.equals("NodeLayersRule");
            boolean nodeRule = qName.equals("NodeRule");

            if (!layerRule && !layersRule && !nodeLayersRule && !nodeRule) return;
            String ruleName = "", layerNames = "", nodeNames = null;
            int when = DRCTemplate.DRCMode.ALL.mode();
            DRCTemplate.DRCRuleType type = DRCTemplate.DRCRuleType.NONE;
            double value = Double.NaN;

            for (int i = 0; i < attributes.getLength(); i++)
            {
                if (attributes.getQName(i).equals("ruleName"))
                    ruleName = attributes.getValue(i);
                else if (attributes.getQName(i).startsWith("layerName"))
                    layerNames = attributes.getValue(i);
                else if (attributes.getQName(i).startsWith("nodeName"))
                    nodeNames = attributes.getValue(i);
                else if (attributes.getQName(i).equals("type"))
                    type = DRCTemplate.DRCRuleType.valueOf(attributes.getValue(i));
                else if (attributes.getQName(i).equals("when"))
                {
                    String[] modes = TextUtils.parseLine(attributes.getValue(i), "|");
                    for (int j = 0; j < modes.length; j++)
                    {
                        DRCTemplate.DRCMode m = DRCTemplate.DRCMode.valueOf(modes[j]);
                        when |= m.mode();
                    }
                    if (foundry != DRCTemplate.DRCMode.NONE)
                        when |= foundry.mode();
                }
                else if (attributes.getQName(i).equals("value"))
                    value = Double.parseDouble(attributes.getValue(i));
                else
                    new Error("Invalid attribute in DRCXMLParser");
            }

            // They could be several layer names or pairs of names for the same rule
            if (layerRule)
            {
                String[] layers = TextUtils.parseLine(layerNames, ",");
                for (int i = 0; i < layers.length; i++)
                {
                    DRCTemplate tmp = new DRCTemplate(ruleName, when, type, layers[i], null, value, null);
                    drcRules.add(tmp);
                }
            }
            else if (nodeRule)
            {
                if (nodeNames == null)
                {
                    DRCTemplate tmp = new DRCTemplate(ruleName, when, type, null, null, value, null);
                    drcRules.add(tmp);
                }
                else
                {
                    String[] names = TextUtils.parseLine(nodeNames, ",");
                    for (int i = 0; i < names.length; i++)
                    {
                        DRCTemplate tmp = new DRCTemplate(ruleName, when, type, null, null, value, names[i]);
                        drcRules.add(tmp);
                    }
                }
            }
            else if (layersRule || nodeLayersRule)
            {
                String[] layerPairs = TextUtils.parseLine(layerNames, "{}");
                for (int i = 0; i < layerPairs.length; i++)
                {
                    String[] pair = TextUtils.parseLine(layerPairs[i], ",");
                    if (pair.length != 2) continue;
                    if (nodeNames == null)
                    {
                        DRCTemplate tmp = new DRCTemplate(ruleName, when, type, pair[0], pair[1], value, null);
                        drcRules.add(tmp);
                    }
                    else
                    {
                        String[] names = TextUtils.parseLine(nodeNames, ",");
                        for (int j = 0; j < names.length; j++)
                        {
                            DRCTemplate tmp = new DRCTemplate(ruleName, when, type, pair[0], pair[1], value, names[j]);
                            drcRules.add(tmp);
                        }
                    }
                }
            }
        }

        public void endDocument ()
        {

        }

        public void fatalError(SAXParseException e)
        {
            System.out.println("Parser Fatal Error");
            e.printStackTrace();
        }

        public void warning(SAXParseException e)
        {
            System.out.println("Parser Warning");
            e.printStackTrace();
        }

        public void error(SAXParseException e)
        {
            System.out.println("Parser Error");
            e.printStackTrace();
        }
    }
}
