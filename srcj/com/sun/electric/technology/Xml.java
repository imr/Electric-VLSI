/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Xml.java
 *
 * Copyright (c) 2003 Sun Microsystems and Static Free Software
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

import com.sun.electric.database.geometry.EGraphics;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.technology.Technology.TechPoint;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.omg.CORBA.COMM_FAILURE;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 *
 */
public class Xml {
    
    public static class Technology {
        public String techName;
        public String shortTechName;
        public String description;
        public int minNumMetals;
        public int maxNumMetals;
        public int defaultNumMetals;
        public double scaleValue;
        public boolean scaleRelevant;
        public String defaultFoundry;
        public double minResistance;
        public double minCapacitance;
        public final List<Color> transparentLayers = new ArrayList<Color>();
        public final List<Layer> layers = new ArrayList<Layer>();
        public final List<ArcProto> arcs = new ArrayList<ArcProto>();
        public final List<PrimitiveNode> nodes = new ArrayList<PrimitiveNode>();
        public final List<SpiceHeader> spiceHeaders = new ArrayList<SpiceHeader>();
        public final List<Foundry> foundries = new ArrayList<Foundry>();
        
        public Layer findLayer(String name) {
            for (Layer layer: layers) {
                if (layer.name.equals(name))
                    return layer;
            }
            return null;
        }
    }
    
    public static class Layer {
        public String name;
        public com.sun.electric.technology.Layer.Function function;
        public int extraFunction;
        public EGraphics desc;
        public double thick3D;
        public double height3D;
        public String cif;
        public double resistance;
        public double capacitance;
        public double edgeCapacitance;
    }
    
    public static class ArcProto {
        public String name;
        public com.sun.electric.technology.ArcProto.Function function;
        public double widthOffset;
        public double defaultWidth;
        public boolean extended;
        public boolean fixedAngle;
        public boolean wipable;
        public int angleIncrement;
        public double antennaRatio;
        public final List<ArcLayer> arcLayers = new ArrayList<ArcLayer>();
    }
    
    public static class ArcLayer {
        public String layer;
        public double widthOffset;
        public Poly.Type style;
    }
    
    public static class PrimitiveNode {
        public String name;
        public com.sun.electric.technology.PrimitiveNode.Function function;
        public double defaultWidth;
        public double defaultHeight;
        public SizeOffset sizeOffset;
        public boolean shrinkArcs;
        public final List<NodeLayer> nodeLayers = new ArrayList<NodeLayer>();
        public final List<PrimitivePort> ports = new ArrayList<PrimitivePort>();
        public int specialType;
        public double[] specialValues;
    }
    
    public static class NodeLayer {
        public String layer;
        public Poly.Type style;
        public int portNum;
        public int representation;
        public final List<TechPoint> techPoints = new ArrayList<TechPoint>();
    }
    
    public static class PrimitivePort {
        public String name;
        public int portAngle;
        public int portRange;
        public int portTopology;
        public TechPoint p0, p1;
        public final List<String> portArcs = new ArrayList<String>();
    }
    
    public static class SpiceHeader {
        public int level;
        public final List<String> spiceLines = new ArrayList<String>();
    }
    
    public static class Foundry {
        public String name;
        public final List<LayersRule> layerRules = new ArrayList<LayersRule>();
    }
    
    public static class LayersRule {
        public String ruleName;
        public String layerNames;
        public String type;
        public String when;
        public double value;
    }
    
    private Xml() {}
    
    private static enum XmlKeyword {
        technology,
        shortName(true),
        description(true),
        numMetals,
        scale,
        defaultFoundry,
        minResistance,
        minCapacitance,
        transparentLayer,
        r(true),
        g(true),
        b(true),
        layer,
        transparentColor,
        opaqueColor,
        patternedOnDisplay(true),
        patternedOnPrinter(true),
        pattern(true),
        outlined(true),
        display3D,
        cifLayer,
        parasitics,
        arcProto,
        widthOffset(true),
        defaultWidth(true),
        extended(true),
        fixedAngle(true),
        wipable(true),
        angleIncrement(true),
        antennaRatio(true),
        arcLayer,
        primitiveNode,
//        defaultWidth(true),
        defaultHeight(true),
        sizeOffset,
        shrinkArcs,
        nodeLayer,
        box,
        minbox,
        points,
        techPoint,
        primitivePort,
        portAngle,
        portTopology(true),
//        techPoint,
        portArc(true),
        multiCut,
        polygonal,
        serpTrans,
        specialValue(true),
        spiceHeader,
        spiceLine,
        Foundry,
        LayersRule;
        
        private final boolean hasText;
        
        private XmlKeyword() {
            hasText = false;
        };
        private XmlKeyword(boolean hasText) {
            this.hasText = hasText;
        }
    };
    
    private static final HashMap<String,XmlKeyword> xmlKeywords = new HashMap<String,XmlKeyword>();
    static {
        for (XmlKeyword k: XmlKeyword.class.getEnumConstants())
            xmlKeywords.put(k.name(), k);
    }
    
    public static Technology parseTechnology(URL fileURL) {
//        System.out.println("Memory usage " + Main.getMemoryUsage() + " bytes");
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
//        factory.setValidating(true);
//        System.out.println("Memory usage " + Main.getMemoryUsage() + " bytes");
        // create the parser
        try {
            SAXParser parser = factory.newSAXParser();
            URLConnection urlCon = fileURL.openConnection();
            InputStream inputStream = urlCon.getInputStream();
            
            System.out.println("Parsing XML file \"" + fileURL + "\"");
            
            XMLReader handler = new XMLReader();
//            System.out.println("Memory usage " + Main.getMemoryUsage() + " bytes");
            parser.parse(inputStream, handler);
//            System.out.println("Memory usage " + Main.getMemoryUsage() + " bytes");
            System.out.println("End Parsing XML file ...");
            return handler.tech;
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Error Parsing XML file ...");
        return null;
    }
    
    private static class XMLReader extends DefaultHandler {
        private Xml.Technology tech = new Xml.Technology();
        private int curTransparent = 0;
        private int curR;
        private int curG;
        private int curB;
        private Layer curLayer;
        private boolean patternedOnDisplay;
        private boolean patternedOnPrinter;
        private final int[] pattern = new int[16];
        private int curPatternIndex;
        private EGraphics.Outline outline;
        private ArcProto curArc;
        private PrimitiveNode curNode;
        private NodeLayer curNodeLayer;
        private PrimitivePort curPort;
        private int curSpecialValueIndex;
        private SpiceHeader curSpiceHeader;
        private Foundry curFoundry;
        
        private boolean acceptCharacters;
        private StringBuilder charBuffer = new StringBuilder();
        private Attributes attributes;
        
        private void beginCharacters() {
            assert !acceptCharacters;
            acceptCharacters = true;
            assert charBuffer.length() == 0;
        }
        private String endCharacters() {
            assert acceptCharacters;
            String s = charBuffer.toString();
            charBuffer.setLength(0);
            acceptCharacters = false;
            return s;
        }
        
        
        ////////////////////////////////////////////////////////////////////
        // Default implementation of the EntityResolver interface.
        ////////////////////////////////////////////////////////////////////
        
        /**
         * Resolve an external entity.
         *
         * <p>Always return null, so that the parser will use the system
         * identifier provided in the XML document.  This method implements
         * the SAX default behaviour: application writers can override it
         * in a subclass to do special translations such as catalog lookups
         * or URI redirection.</p>
         *
         * @param publicId The public identifier, or null if none is
         *                 available.
         * @param systemId The system identifier provided in the XML
         *                 document.
         * @return The new input source, or null to require the
         *         default behaviour.
         * @exception java.io.IOException If there is an error setting
         *            up the new input source.
         * @exception org.xml.sax.SAXException Any SAX exception, possibly
         *            wrapping another exception.
         * @see org.xml.sax.EntityResolver#resolveEntity
         */
        public InputSource resolveEntity(String publicId, String systemId)
        throws IOException, SAXException {
            return null;
        }
        
        
        
        ////////////////////////////////////////////////////////////////////
        // Default implementation of DTDHandler interface.
        ////////////////////////////////////////////////////////////////////
        
        
        /**
         * Receive notification of a notation declaration.
         *
         * <p>By default, do nothing.  Application writers may override this
         * method in a subclass if they wish to keep track of the notations
         * declared in a document.</p>
         *
         * @param name The notation name.
         * @param publicId The notation public identifier, or null if not
         *                 available.
         * @param systemId The notation system identifier.
         * @exception org.xml.sax.SAXException Any SAX exception, possibly
         *            wrapping another exception.
         * @see org.xml.sax.DTDHandler#notationDecl
         */
        public void notationDecl(String name, String publicId, String systemId)
        throws SAXException {
            int x = 0;
        }
        
        
        /**
         * Receive notification of an unparsed entity declaration.
         *
         * <p>By default, do nothing.  Application writers may override this
         * method in a subclass to keep track of the unparsed entities
         * declared in a document.</p>
         *
         * @param name The entity name.
         * @param publicId The entity public identifier, or null if not
         *                 available.
         * @param systemId The entity system identifier.
         * @param notationName The name of the associated notation.
         * @exception org.xml.sax.SAXException Any SAX exception, possibly
         *            wrapping another exception.
         * @see org.xml.sax.DTDHandler#unparsedEntityDecl
         */
        public void unparsedEntityDecl(String name, String publicId,
                String systemId, String notationName)
                throws SAXException {
            int x = 0;
        }
        
        
        
        ////////////////////////////////////////////////////////////////////
        // Default implementation of ContentHandler interface.
        ////////////////////////////////////////////////////////////////////
        
        
        /**
         * Receive a Locator object for document events.
         *
         * <p>By default, do nothing.  Application writers may override this
         * method in a subclass if they wish to store the locator for use
         * with other document events.</p>
         *
         * @param locator A locator for all SAX document events.
         * @see org.xml.sax.ContentHandler#setDocumentLocator
         * @see org.xml.sax.Locator
         */
        public void setDocumentLocator(Locator locator) {
            System.out.println("setDocumentLocator publicId=" + locator.getPublicId() + " systemId=" + locator.getSystemId() +
                    " line=" + locator.getLineNumber() + " column=" + locator.getColumnNumber());
        }
        
        
        /**
         * Receive notification of the beginning of the document.
         *
         * <p>By default, do nothing.  Application writers may override this
         * method in a subclass to take specific actions at the beginning
         * of a document (such as allocating the root node of a tree or
         * creating an output file).</p>
         *
         * @exception org.xml.sax.SAXException Any SAX exception, possibly
         *            wrapping another exception.
         * @see org.xml.sax.ContentHandler#startDocument
         */
        public void startDocument()
        throws SAXException {
            System.out.println("startDocumnet");
        }
        
        
        /**
         * Receive notification of the end of the document.
         *
         * <p>By default, do nothing.  Application writers may override this
         * method in a subclass to take specific actions at the end
         * of a document (such as finalising a tree or closing an output
         * file).</p>
         *
         * @exception org.xml.sax.SAXException Any SAX exception, possibly
         *            wrapping another exception.
         * @see org.xml.sax.ContentHandler#endDocument
         */
        public void endDocument()
        throws SAXException {
            System.out.println("endDocumnet");
        }
        
        
        /**
         * Receive notification of the start of a Namespace mapping.
         *
         * <p>By default, do nothing.  Application writers may override this
         * method in a subclass to take specific actions at the start of
         * each Namespace prefix scope (such as storing the prefix mapping).</p>
         *
         * @param prefix The Namespace prefix being declared.
         * @param uri The Namespace URI mapped to the prefix.
         * @exception org.xml.sax.SAXException Any SAX exception, possibly
         *            wrapping another exception.
         * @see org.xml.sax.ContentHandler#startPrefixMapping
         */
        public void startPrefixMapping(String prefix, String uri)
        throws SAXException {
            System.out.println("startPrefixMapping prefix=" + prefix + " uri=" + uri);;
        }
        
        
        /**
         * Receive notification of the end of a Namespace mapping.
         *
         * <p>By default, do nothing.  Application writers may override this
         * method in a subclass to take specific actions at the end of
         * each prefix mapping.</p>
         *
         * @param prefix The Namespace prefix being declared.
         * @exception org.xml.sax.SAXException Any SAX exception, possibly
         *            wrapping another exception.
         * @see org.xml.sax.ContentHandler#endPrefixMapping
         */
        public void endPrefixMapping(String prefix)
        throws SAXException {
            System.out.println("endPrefixMapping prefix=" + prefix);
        }
        
        
        /**
         * Receive notification of the start of an element.
         *
         * <p>By default, do nothing.  Application writers may override this
         * method in a subclass to take specific actions at the start of
         * each element (such as allocating a new tree node or writing
         * output to a file).</p>
         *
         * @param uri The Namespace URI, or the empty string if the
         *        element has no Namespace URI or if Namespace
         *        processing is not being performed.
         * @param localName The local name (without prefix), or the
         *        empty string if Namespace processing is not being
         *        performed.
         * @param qName The qualified name (with prefix), or the
         *        empty string if qualified names are not available.
         * @param attributes The attributes attached to the element.  If
         *        there are no attributes, it shall be an empty
         *        Attributes object.
         * @exception org.xml.sax.SAXException Any SAX exception, possibly
         *            wrapping another exception.
         * @see org.xml.sax.ContentHandler#startElement
         */
        public void startElement(String uri, String localName,
                String qName, Attributes attributes)
                throws SAXException {
            boolean dump = false;
            XmlKeyword key = xmlKeywords.get(localName);
//            System.out.print("<" + key.name());
            this.attributes = attributes;
            switch (key) {
                case technology:
                    tech.techName = a("name");
                    dump = true;
                    break;
                case numMetals:
                    tech.minNumMetals = Integer.parseInt(a("min"));
                    tech.maxNumMetals = Integer.parseInt(a("max"));
                    tech.defaultNumMetals = Integer.parseInt(a("default"));
                    break;
                case scale:
                    tech.scaleValue = Double.parseDouble(a("value"));
                    tech.scaleRelevant = Boolean.parseBoolean(a("relevant"));
                    break;
                case defaultFoundry:
                    tech.defaultFoundry = a("value");
                    break;
                case minResistance:
                    tech.minResistance = Double.parseDouble(a("value"));
                    break;
                case minCapacitance:
                    tech.minCapacitance = Double.parseDouble(a("value"));
                    break;
                case transparentLayer:
                    curTransparent = Integer.parseInt(a("transparent"));
                    curR = curG = curB = 0;
                    break;
                case layer:
                    curLayer = new Layer();
                    curLayer.name = a("name");
                    curLayer.function = com.sun.electric.technology.Layer.Function.valueOf(a("fun"));
                    String extraFunStr = a_("extraFun");
                    if (extraFunStr != null) {
                        if (extraFunStr.equals("DEPLETION_HEAVY"))
                            curLayer.extraFunction = com.sun.electric.technology.Layer.Function.DEPLETION|com.sun.electric.technology.Layer.Function.HEAVY;
                        else if (extraFunStr.equals("DEPLETION_LIGHT"))
                            curLayer.extraFunction = com.sun.electric.technology.Layer.Function.DEPLETION|com.sun.electric.technology.Layer.Function.LIGHT;
                        else if (extraFunStr.equals("ENHANCEMENT_HEAVY"))
                            curLayer.extraFunction = com.sun.electric.technology.Layer.Function.ENHANCEMENT|com.sun.electric.technology.Layer.Function.HEAVY;
                        else if (extraFunStr.equals("ENHANCEMENT_LIGHT"))
                            curLayer.extraFunction = com.sun.electric.technology.Layer.Function.ENHANCEMENT|com.sun.electric.technology.Layer.Function.LIGHT;
                        else
                            curLayer.extraFunction = com.sun.electric.technology.Layer.Function.parseExtraName(extraFunStr);
                    }
                    curTransparent = 0;
                    curR = curG = curB = 0;
                    patternedOnDisplay = false;
                    patternedOnPrinter = false;
                    Arrays.fill(pattern, 0);
                    curPatternIndex = 0;
//                    EGraphics.Outline outline = null;
                    break;
                case transparentColor:
                    curTransparent = Integer.parseInt(a("transparent"));
                    if (curTransparent > 0) {
                        Color color = tech.transparentLayers.get(curTransparent - 1);
                        curR = color.getRed();
                        curG = color.getGreen();
                        curB = color.getBlue();
                    }
                    break;
                case opaqueColor:
                    curR = Integer.parseInt(a("r"));
                    curG = Integer.parseInt(a("g"));
                    curB = Integer.parseInt(a("b"));
                    break;
                case display3D:
                    curLayer.thick3D = Double.parseDouble(a("thick"));
                    curLayer.height3D = Double.parseDouble(a("height"));
                    break;
                case cifLayer:
                    curLayer.cif = a("cif");
                    break;
                case parasitics:
                    curLayer.resistance = Double.parseDouble(a("resistance"));
                    curLayer.capacitance = Double.parseDouble(a("capacitance"));
                    curLayer.edgeCapacitance = Double.parseDouble(a("edgeCapacitance"));
                    break;
                case arcProto:
                    curArc = new ArcProto();
                    curArc.name = a("name");
                    curArc.function = com.sun.electric.technology.ArcProto.Function.valueOf(a("fun"));
                    break;
                case arcLayer:
                    ArcLayer arcLayer = new ArcLayer();
                    arcLayer.layer = a("layer");
                    arcLayer.widthOffset = Double.parseDouble(a("widthOffset"));
                    arcLayer.style = Poly.Type.valueOf(a("style"));
                    curArc.arcLayers.add(arcLayer);
                    break;
                case primitiveNode:
                    curNode = new PrimitiveNode();
                    curNode.name = a("name");
                    curNode.function = com.sun.electric.technology.PrimitiveNode.Function.valueOf(a("fun"));
                    break;
                case sizeOffset:
                    double lx = Double.parseDouble(a("lx"));
                    double hx = Double.parseDouble(a("hx"));
                    double ly = Double.parseDouble(a("ly"));
                    double hy = Double.parseDouble(a("hy"));
                    curNode.sizeOffset = new SizeOffset(lx, hx, ly, hy);
                    break;
                case shrinkArcs:
                    curNode.shrinkArcs = true;
                    break;
                case nodeLayer:
                    curNodeLayer = new NodeLayer();
                    curNodeLayer.layer = a("layer");
                    curNodeLayer.style = Poly.Type.valueOf(a("style"));
                    String portNum = a_("portNum");
                    if (portNum != null)
                        curNodeLayer.portNum = Integer.parseInt(portNum);
                    break;
                case box:
                    curNodeLayer.representation = com.sun.electric.technology.Technology.NodeLayer.BOX;
                    break;
                case minbox:
                    curNodeLayer.representation = com.sun.electric.technology.Technology.NodeLayer.MINBOX;
                    break;
                case points:
                    curNodeLayer.representation = com.sun.electric.technology.Technology.NodeLayer.POINTS;
                    break;
                case techPoint:
                    double xm = Double.parseDouble(a("xm"));
                    double xa = Double.parseDouble(a("xa"));
                    double ym = Double.parseDouble(a("ym"));
                    double ya = Double.parseDouble(a("ya"));
                    TechPoint p = new TechPoint(new EdgeH(xm, xa), new EdgeV(ym, ya));
                    if (curNodeLayer != null)
                        curNodeLayer.techPoints.add(p);
                    if (curPort != null) {
                        if (curPort.p0 == null)
                            curPort.p0 = p;
                        else
                            curPort.p1 = p;
                    }
                    break;
                case primitivePort:
                    curPort = new PrimitivePort();
                    curPort.name = a("name");
                    break;
                case portAngle:
                    curPort.portAngle = Integer.parseInt(a("primary"));
                    curPort.portRange = Integer.parseInt(a("range"));
                    break;
                case multiCut:
                    curNode.specialType = com.sun.electric.technology.PrimitiveNode.MULTICUT;
                    curNode.specialValues = new double[6];
                    curSpecialValueIndex = 0;
                    break;
                case polygonal:
                    curNode.specialType = com.sun.electric.technology.PrimitiveNode.POLYGONAL;
                    break;
                case serpTrans:
                    curNode.specialType = com.sun.electric.technology.PrimitiveNode.SERPTRANS;
                    curNode.specialValues = new double[6];
                    curSpecialValueIndex = 0;
                    break;
                case spiceHeader:
                    curSpiceHeader = new SpiceHeader();
                    curSpiceHeader.level = Integer.parseInt(a("level"));
                    tech.spiceHeaders.add(curSpiceHeader);
                    break;
                case spiceLine:
                    curSpiceHeader.spiceLines.add(a("line"));
                    break;
                case Foundry:
                    curFoundry = new Foundry();
                    curFoundry.name = a("name");
                    tech.foundries.add(curFoundry);
                    break;
                case LayersRule:
                    LayersRule layersRule = new LayersRule();
                    layersRule.ruleName = a("ruleName");
                    layersRule.layerNames = a("layerNames");
                    layersRule.type = a("type");
                    layersRule.when = a("when");
                    layersRule.value = Double.parseDouble(a("value"));
                    curFoundry.layerRules.add(layersRule);
                    break;
                default:
                    assert key.hasText;
                    beginCharacters();
//                    System.out.print(">");
                    return;
            }
            assert !key.hasText;
//            System.out.println(">");
            if (dump) {
                System.out.println("startElement uri=" + uri + " localName=" + localName + " qName=" + qName);
                for (int i = 0; i < attributes.getLength(); i++) {
                    System.out.println("\tattribute " + i + " uri=" + attributes.getURI(i) +
                            " localName=" + attributes.getLocalName(i) + " QName=" + attributes.getQName(i) +
                            " type=" + attributes.getType(i) + " value=<" + attributes.getValue(i) + ">");
                }
            }
        }
        
        private String a(String attrName) {
            String v = attributes.getValue(attrName);
//            System.out.print(" " + attrName + "=\"" + v + "\"");
            return v;
        }
        
        private String a_(String attrName) {
            String v = attributes.getValue(attrName);
            if (v == null) return null;
//            System.out.print(" " + attrName + "=\"" + v + "\"");
            return v;
        }
        
        
        /**
         * Receive notification of the end of an element.
         *
         * <p>By default, do nothing.  Application writers may override this
         * method in a subclass to take specific actions at the end of
         * each element (such as finalising a tree node or writing
         * output to a file).</p>
         *
         * @param uri The Namespace URI, or the empty string if the
         *        element has no Namespace URI or if Namespace
         *        processing is not being performed.
         * @param localName The local name (without prefix), or the
         *        empty string if Namespace processing is not being
         *        performed.
         * @param qName The qualified name (with prefix), or the
         *        empty string if qualified names are not available.
         * @exception org.xml.sax.SAXException Any SAX exception, possibly
         *            wrapping another exception.
         * @see org.xml.sax.ContentHandler#endElement
         */
        public void endElement(String uri, String localName, String qName)
        throws SAXException {
            XmlKeyword key = xmlKeywords.get(localName);
            if (key.hasText) {
                String text = endCharacters();
//                System.out.println(text + "</" + localName + ">");
                switch (key) {
                    case shortName:
                        tech.shortTechName = text;
                        break;
                    case description:
                        tech.description = text;
                        break;
                    case r:
                        curR = Integer.parseInt(text);
                        break;
                    case g:
                        curG = Integer.parseInt(text);
                        break;
                    case b:
                        curB = Integer.parseInt(text);
                        break;
                    case patternedOnDisplay:
                        patternedOnDisplay = Boolean.parseBoolean(text);
                        break;
                    case patternedOnPrinter:
                        patternedOnPrinter = Boolean.parseBoolean(text);
                        break;
                    case pattern:
                        int p = 0;
                        assert text.length() == 16;
                        for (int j = 0; j < text.length(); j++) {
                            if (text.charAt(text.length() - j - 1) != ' ')
                                p |= (1 << j);
                        }
                        pattern[curPatternIndex++] = p;
                        break;
                    case outlined:
                        outline = EGraphics.Outline.valueOf(text);
                        break;
                    case widthOffset:
                        curArc.widthOffset = Double.parseDouble(text);
                        break;
                    case defaultWidth: // arc and node
                        if (curArc != null) curArc.defaultWidth = Double.parseDouble(text);
                        if (curNode != null) curNode.defaultWidth = Double.parseDouble(text);
                        break;
                    case extended:
                        curArc.extended = Boolean.parseBoolean(text);
                        break;
                    case fixedAngle:
                        curArc.fixedAngle = Boolean.parseBoolean(text);
                        break;
                    case wipable:
                        curArc.wipable = Boolean.parseBoolean(text);
                        break;
                    case angleIncrement:
                        curArc.angleIncrement = Integer.parseInt(text);
                        break;
                    case antennaRatio:
                        curArc.antennaRatio = Double.parseDouble(text);
                        break;
                    case defaultHeight:
                        curNode.defaultHeight = Double.parseDouble(text);
                        break;
                    case portTopology:
                        curPort.portTopology = Integer.parseInt(text);
                        break;
                    case portArc:
                        curPort.portArcs.add(text);
                        break;
                    case specialValue:
                        curNode.specialValues[curSpecialValueIndex++] = Double.parseDouble(text);
                        break;
                    default:
                        assert false;
                }
                return;
            }
//            System.out.println("</" + localName + ">");
            switch (key) {
                case technology:
                    break;
                case transparentLayer:
                    while (curTransparent > tech.transparentLayers.size())
                        tech.transparentLayers.add(null);
                    Color oldColor = tech.transparentLayers.set(curTransparent - 1, new Color(curR, curG, curB));
                    assert oldColor == null;
                    break;
                case layer:
                    assert curPatternIndex == pattern.length;
                    curLayer.desc = new EGraphics(patternedOnDisplay, patternedOnPrinter, outline, curTransparent,
                            curR, curG, curB, 0.8, true, pattern.clone());
                    assert tech.findLayer(curLayer.name) == null;
                    tech.layers.add(curLayer);
                    curLayer = null;
                    break;
                case arcProto:
                    tech.arcs.add(curArc);
                    curArc = null;
                    break;
                case primitiveNode:
                    tech.nodes.add(curNode);
                    curNode = null;
                    break;
                case nodeLayer:
                    curNode.nodeLayers.add(curNodeLayer);
                    curNodeLayer = null;
                    break;
                case primitivePort:
                    curNode.ports.add(curPort);
                    curPort = null;
                    break;
                    
                case spiceHeader:
                case numMetals:
                case scale:
                case defaultFoundry:
                case minResistance:
                case minCapacitance:
                case transparentColor:
                case opaqueColor:
                case display3D:
                case cifLayer:
                case parasitics:
                case arcLayer:
                case sizeOffset:
                case shrinkArcs:
                case box:
                case minbox:
                case points:
                case techPoint:
                case portAngle:
                case multiCut:
                case polygonal:
                case serpTrans:
                case spiceLine:
                case Foundry:
                case LayersRule:
                    break;
                default:
                    assert false;
            }
        }
        
        
        /**
         * Receive notification of character data inside an element.
         *
         * <p>By default, do nothing.  Application writers may override this
         * method to take specific actions for each chunk of character data
         * (such as adding the data to a node or buffer, or printing it to
         * a file).</p>
         *
         * @param ch The characters.
         * @param start The start position in the character array.
         * @param length The number of characters to use from the
         *               character array.
         * @exception org.xml.sax.SAXException Any SAX exception, possibly
         *            wrapping another exception.
         * @see org.xml.sax.ContentHandler#characters
         */
        public void characters(char ch[], int start, int length)
        throws SAXException {
            if (acceptCharacters) {
                charBuffer.append(ch, start, length);
            } else {
                boolean nonBlank = false;
                for (int i = 0; i < length; i++) {
                    char c = ch[start + i];
                    nonBlank = nonBlank || c != ' ' && c != '\n';
                }
                if (nonBlank) {
                    System.out.print("characters size=" + ch.length + " start=" + start + " length=" + length + " {");
                    for (int i = 0; i < length; i++)
                        System.out.print(ch[start + i]);
                    System.out.println("}");
                }
            }
        }
        
        
        /**
         * Receive notification of ignorable whitespace in element content.
         *
         * <p>By default, do nothing.  Application writers may override this
         * method to take specific actions for each chunk of ignorable
         * whitespace (such as adding data to a node or buffer, or printing
         * it to a file).</p>
         *
         * @param ch The whitespace characters.
         * @param start The start position in the character array.
         * @param length The number of characters to use from the
         *               character array.
         * @exception org.xml.sax.SAXException Any SAX exception, possibly
         *            wrapping another exception.
         * @see org.xml.sax.ContentHandler#ignorableWhitespace
         */
        public void ignorableWhitespace(char ch[], int start, int length)
        throws SAXException {
            int x = 0;
        }
        
        
        /**
         * Receive notification of a processing instruction.
         *
         * <p>By default, do nothing.  Application writers may override this
         * method in a subclass to take specific actions for each
         * processing instruction, such as setting status variables or
         * invoking other methods.</p>
         *
         * @param target The processing instruction target.
         * @param data The processing instruction data, or null if
         *             none is supplied.
         * @exception org.xml.sax.SAXException Any SAX exception, possibly
         *            wrapping another exception.
         * @see org.xml.sax.ContentHandler#processingInstruction
         */
        public void processingInstruction(String target, String data)
        throws SAXException {
            int x = 0;
        }
        
        
        /**
         * Receive notification of a skipped entity.
         *
         * <p>By default, do nothing.  Application writers may override this
         * method in a subclass to take specific actions for each
         * processing instruction, such as setting status variables or
         * invoking other methods.</p>
         *
         * @param name The name of the skipped entity.
         * @exception org.xml.sax.SAXException Any SAX exception, possibly
         *            wrapping another exception.
         * @see org.xml.sax.ContentHandler#processingInstruction
         */
        public void skippedEntity(String name)
        throws SAXException {
            int x = 0;
        }
        
        
        
        ////////////////////////////////////////////////////////////////////
        // Default implementation of the ErrorHandler interface.
        ////////////////////////////////////////////////////////////////////
        
        
        /**
         * Receive notification of a parser warning.
         *
         * <p>The default implementation does nothing.  Application writers
         * may override this method in a subclass to take specific actions
         * for each warning, such as inserting the message in a log file or
         * printing it to the console.</p>
         *
         * @param e The warning information encoded as an exception.
         * @exception org.xml.sax.SAXException Any SAX exception, possibly
         *            wrapping another exception.
         * @see org.xml.sax.ErrorHandler#warning
         * @see org.xml.sax.SAXParseException
         */
        public void warning(SAXParseException e)
        throws SAXException {
            int x = 0;
        }
        
        
        /**
         * Receive notification of a recoverable parser error.
         *
         * <p>The default implementation does nothing.  Application writers
         * may override this method in a subclass to take specific actions
         * for each error, such as inserting the message in a log file or
         * printing it to the console.</p>
         *
         * @param e The error information encoded as an exception.
         * @exception org.xml.sax.SAXException Any SAX exception, possibly
         *            wrapping another exception.
         * @see org.xml.sax.ErrorHandler#warning
         * @see org.xml.sax.SAXParseException
         */
        public void error(SAXParseException e)
        throws SAXException {
            System.out.println("error publicId=" + e.getPublicId() + " systemId=" + e.getSystemId() +
                    " line=" + e.getLineNumber() + " column=" + e.getColumnNumber() + " message=" + e.getMessage() + " exception=" + e.getException());
        }
        
        
        /**
         * Report a fatal XML parsing error.
         *
         * <p>The default implementation throws a SAXParseException.
         * Application writers may override this method in a subclass if
         * they need to take specific actions for each fatal error (such as
         * collecting all of the errors into a single report): in any case,
         * the application must stop all regular processing when this
         * method is invoked, since the document is no longer reliable, and
         * the parser may no longer report parsing events.</p>
         *
         * @param e The error information encoded as an exception.
         * @exception org.xml.sax.SAXException Any SAX exception, possibly
         *            wrapping another exception.
         * @see org.xml.sax.ErrorHandler#fatalError
         * @see org.xml.sax.SAXParseException
         */
        public void fatalError(SAXParseException e)
        throws SAXException {
            throw e;
        }
        
    }
    
}
