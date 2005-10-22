package com.sun.electric.tool.drc;

import org.xml.sax.SAXParseException;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.SAXParser;
import java.io.InputStream;
import java.net.URLConnection;
import java.net.URL;
import java.util.List;
import java.util.ArrayList;

import com.sun.electric.technology.DRCTemplate;
import com.sun.electric.database.variable.EvalJavaBsh;
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

        DRCXMLHandler(List drcList)
        {
            this.drcRules = drcList;
        }

        public void startElement (String uri, String localName, String qName, Attributes attributes)
        {
            boolean layerRule = qName.equals("LayerRule");
            boolean layersRule = qName.equals("LayersRule");
            boolean nodeLayersRule = qName.equals("NodeLayersRule");
            boolean nodeRule = qName.equals("NodeRule");

            if (layerRule || layersRule || nodeLayersRule || nodeRule)
            {
                String ruleName = "", layerNames = "", nodeName = "";
                int when = DRCTemplate.NONE, type = DRCTemplate.NONE;
                double value = Double.NaN;

                for (int i = 0; i < attributes.getLength(); i++)
                {
                    if (attributes.getQName(i).equals("ruleName"))
                        ruleName = attributes.getValue(i);
                    else if (attributes.getQName(i).startsWith("layerName"))
                        layerNames = attributes.getValue(i);
                    else if (attributes.getQName(i).equals("type"))
                    {
                        Integer obj = (Integer)EvalJavaBsh.evalJavaBsh.doEvalLine("import com.sun.electric.technology.DRCTemplate; " + attributes.getValue(i));
                        type = obj;
                    }
                    else if (attributes.getQName(i).equals("when"))
                    {
                        Integer obj = (Integer)EvalJavaBsh.evalJavaBsh.doEvalLine("import com.sun.electric.technology.DRCTemplate; " + attributes.getValue(i));
                        when = obj;
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
                    DRCTemplate tmp = new DRCTemplate(ruleName, when, type, null, null, value, null);
                    drcRules.add(tmp);
                }
                else if (layersRule || nodeLayersRule)
                {
                    String[] layerPairs = TextUtils.parseLine(layerNames, "{}");
                    for (int i = 0; i < layerPairs.length; i++)
                    {
                        String[] pair = TextUtils.parseLine(layerPairs[i], ",");
                        if (pair.length != 2) continue;
                        DRCTemplate tmp = new DRCTemplate(ruleName, when, type, pair[0], pair[1],
                                value, (layersRule?null:nodeName));
                        drcRules.add(tmp);
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
