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

import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.geometry.EGraphics;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.technology.Technology.TechPoint;
import com.sun.electric.tool.user.tecEdit.ArcInfo;

import java.awt.Color;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
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
        public MenuPalette menuPalette;
        public final List<Foundry> foundries = new ArrayList<Foundry>();
        
        public Layer findLayer(String name) {
            for (Layer layer: layers) {
                if (layer.name.equals(name))
                    return layer;
            }
            return null;
        }
        
        public ArcProto findArc(String name) {
            for (ArcProto arc: arcs) {
                if (arc.name.equals(name))
                    return arc;
            }
            return null;
        }
        
        public PrimitiveNode findNode(String name) {
            for (PrimitiveNode node: nodes) {
                if (node.name.equals(name))
                    return node;
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
        public String pseudoLayer;
        public PureLayerNode pureLayerNode;
    }
    
    public static class PureLayerNode {
        public String name;
        public Poly.Type style;
        public String port;
        public final Distance size = new Distance();
        public final List<String> portArcs = new ArrayList<String>();
    }
    
    public static class ArcProto {
        public String name;
        public com.sun.electric.technology.ArcProto.Function function;
        public boolean wipable;
        public boolean curvable;
        public boolean special;
        public boolean notUsed;
        public boolean skipSizeInPalette;
        
        public double widthOffset;
        public final Distance defaultWidth = new Distance();
        public boolean extended;
        public boolean fixedAngle;
        public int angleIncrement;
        public double antennaRatio;
        public final List<ArcLayer> arcLayers = new ArrayList<ArcLayer>();
    }
    
    public static class ArcLayer {
        public String layer;
        public final Distance width = new Distance();
        public Poly.Type style;
    }
    
    public static class PrimitiveNode {
        public String name;
        public boolean shrinkArcs;
        public boolean square;
        public boolean canBeZeroSize;
        public boolean wipes;
        public boolean lockable;
        public boolean edgeSelect;
        public boolean skipSizeInPalette;
        public boolean notUsed;
        public boolean lowVt;
        public boolean highVt;
        public boolean nativeBit;
        public boolean od18;
        public boolean od25;
        public boolean od33;
        
        public com.sun.electric.technology.PrimitiveNode.Function function;
        public final Distance defaultWidth = new Distance();
        public final Distance defaultHeight = new Distance();
        public SizeOffset sizeOffset;
        public final List<NodeLayer> nodeLayers = new ArrayList<NodeLayer>();
        public final List<PrimitivePort> ports = new ArrayList<PrimitivePort>();
        public int specialType;
        public double[] specialValues;
        public com.sun.electric.technology.PrimitiveNode.NodeSizeRule nodeSizeRule;
    }
    
    public static class NodeLayer {
        public String layer;
        public Poly.Type style;
        public int portNum;
        public boolean inLayers;
        public boolean inElectricalLayers;
        public int representation;
        public final List<TechPoint> techPoints = new ArrayList<TechPoint>();
        public double sizex, sizey, sep1d, sep2d;
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
    
    public static class MenuPalette {
        public int numColumns;
        public ArrayList<ArrayList<Object>> menuBoxes = new ArrayList<ArrayList<Object>>();
    }
    
    public static class Distance {
        public double value;
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
        opacity(true),
        foreground(true),
        display3D,
        cifLayer,
        parasitics,
        pseudoLayer(true),
        pureLayerNode,
        
        arcProto,
        wipable,
        curvable,
        special,
        notUsed,
        skipSizeInPalette,
        
        extended(true),
        fixedAngle(true),
        angleIncrement(true),
        antennaRatio(true),
        
        widthOffset(true),
        defaultWidth,
        arcLayer,
        
        primitiveNode,
        shrinkArcs,
        square,
        canBeZeroSize,
        wipes,
        lockable,
        edgeSelect,
//        skipSizeInPalette,
//        notUsed,
        lowVt,
        highVt,
        nativeBit,
        od18,
        od25,
        od33,
//        defaultWidth,
        defaultHeight,
        sizeOffset,
        nodeLayer,
        box,
        minbox,
        multicutbox,
        points,
        techPoint,
        primitivePort,
        portAngle,
        portTopology(true),
//        techPoint,
        portArc(true),
        polygonal,
        serpTrans,
        specialValue(true),
        minSizeRule,
        spiceHeader,
        spiceLine,
        menuPalette,
        menuBox,
        menuArc(true),
        menuNode(true),
        menuText(true),
        lambda(true),
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
        private double opacity;
        private boolean foreground;
        private ArcProto curArc;
        private PrimitiveNode curNode;
        private NodeLayer curNodeLayer;
        private PrimitivePort curPort;
        private int curSpecialValueIndex;
        private ArrayList<Object> curMenuBox;
        private Distance curDistance;
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
                        if (extraFunStr.equals("depletion_heavy"))
                            curLayer.extraFunction = com.sun.electric.technology.Layer.Function.DEPLETION|com.sun.electric.technology.Layer.Function.HEAVY;
                        else if (extraFunStr.equals("depletion_light"))
                            curLayer.extraFunction = com.sun.electric.technology.Layer.Function.DEPLETION|com.sun.electric.technology.Layer.Function.LIGHT;
                        else if (extraFunStr.equals("enhancement_heavy"))
                            curLayer.extraFunction = com.sun.electric.technology.Layer.Function.ENHANCEMENT|com.sun.electric.technology.Layer.Function.HEAVY;
                        else if (extraFunStr.equals("enhancement_light"))
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
                case pureLayerNode:
                    curLayer.pureLayerNode = new PureLayerNode();
                    curLayer.pureLayerNode.name = a("name");
                    String styleStr = a_("style");
                    curLayer.pureLayerNode.style = styleStr != null ? Poly.Type.valueOf(styleStr) : Poly.Type.FILLED;
                    curLayer.pureLayerNode.port = a("port");
                    curDistance = curLayer.pureLayerNode.size;
                    break;
                case arcProto:
                    curArc = new ArcProto();
                    curArc.name = a("name");
                    curArc.function = com.sun.electric.technology.ArcProto.Function.valueOf(a("fun"));
                    break;
                case wipable:
                    curArc.wipable = true;
                    break;
                case curvable:
                    curArc.curvable = true;
                    break;
                case special:
                    curArc.special = true;
                    break;
                case notUsed:
                    if (curArc != null)
                        curArc.notUsed = true;
                    if (curNode != null)
                        curNode.notUsed = true;
                    break;
                case skipSizeInPalette:
                    if (curArc != null)
                        curArc.skipSizeInPalette = true;
                    if (curNode != null)
                        curNode.skipSizeInPalette = true;
                    break;
                case defaultWidth:
                    if (curArc != null)
                        curDistance = curArc.defaultWidth;
                    if (curNode != null)
                        curDistance = curNode.defaultWidth;
                    break;
                case arcLayer:
                    ArcLayer arcLayer = new ArcLayer();
                    arcLayer.layer = a("layer");
                    curDistance = arcLayer.width;
                    arcLayer.style = Poly.Type.valueOf(a("style"));
                    curArc.arcLayers.add(arcLayer);
                    break;
                case primitiveNode:
                    curNode = new PrimitiveNode();
                    curNode.name = a("name");
                    curNode.function = com.sun.electric.technology.PrimitiveNode.Function.valueOf(a("fun"));
                    break;
                case shrinkArcs:
                    curNode.shrinkArcs = true;
                    break;
                case square:
                    curNode.square = true;
                    break;
                case canBeZeroSize:
                    curNode.canBeZeroSize = true;
                    break;
                case wipes:
                    curNode.wipes = true;
                    break;
                case lockable:
                    curNode.lockable = true;
                    break;
                case edgeSelect:
                    curNode.edgeSelect = true;
                    break;
                case lowVt:
                    curNode.lowVt = true;
                    break;
                case highVt:
                    curNode.highVt = true;
                    break;
                case nativeBit:
                    curNode.nativeBit = true;
                    break;
                case od18:
                    curNode.od18 = true;
                    break;
                case od25:
                    curNode.od25 = true;
                    break;
                case od33:
                    curNode.od33 = true;
                    break;
                case defaultHeight:
                    curDistance = curNode.defaultHeight;
                    break;
                case sizeOffset:
                    double lx = Double.parseDouble(a("lx"));
                    double hx = Double.parseDouble(a("hx"));
                    double ly = Double.parseDouble(a("ly"));
                    double hy = Double.parseDouble(a("hy"));
                    curNode.sizeOffset = new SizeOffset(lx, hx, ly, hy);
                    break;
                case nodeLayer:
                    curNodeLayer = new NodeLayer();
                    curNodeLayer.layer = a("layer");
                    curNodeLayer.style = Poly.Type.valueOf(a("style"));
                    String portNum = a_("portNum");
                    if (portNum != null)
                        curNodeLayer.portNum = Integer.parseInt(portNum);
                    String electrical = a_("electrical");
                    if (electrical != null) {
                        if (Boolean.parseBoolean(electrical))
                            curNodeLayer.inElectricalLayers = true;
                        else
                            curNodeLayer.inLayers = true;
                    } else {
                        curNodeLayer.inElectricalLayers = curNodeLayer.inLayers = true;
                    }
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
                case multicutbox:
                    curNodeLayer.representation = com.sun.electric.technology.Technology.NodeLayer.MULTICUTBOX;
                    curNodeLayer.sizex = Double.parseDouble(a("sizex"));
                    curNodeLayer.sizey = Double.parseDouble(a("sizey"));
                    curNodeLayer.sep1d = Double.parseDouble(a("sep1d"));
                    curNodeLayer.sep2d = Double.parseDouble(a("sep2d"));
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
                case polygonal:
                    curNode.specialType = com.sun.electric.technology.PrimitiveNode.POLYGONAL;
                    break;
                case serpTrans:
                    curNode.specialType = com.sun.electric.technology.PrimitiveNode.SERPTRANS;
                    curNode.specialValues = new double[6];
                    curSpecialValueIndex = 0;
                    break;
                case minSizeRule:
                    curNode.nodeSizeRule = new com.sun.electric.technology.PrimitiveNode.NodeSizeRule(
                            Double.parseDouble(a("width")),
                            Double.parseDouble(a("height")),
                            a("rule"));
                    break;
                case spiceHeader:
                    curSpiceHeader = new SpiceHeader();
                    curSpiceHeader.level = Integer.parseInt(a("level"));
                    tech.spiceHeaders.add(curSpiceHeader);
                    break;
                case spiceLine:
                    curSpiceHeader.spiceLines.add(a("line"));
                    break;
                case menuPalette:
                    tech.menuPalette = new MenuPalette();
                    tech.menuPalette.numColumns = Integer.parseInt(a("numColumns"));
                    break;
                case menuBox:
                    curMenuBox = new ArrayList<Object>();
                    tech.menuPalette.menuBoxes.add(curMenuBox);
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
                    case opacity:
                        opacity = Double.parseDouble(text);
                        break;
                    case foreground:
                        foreground = Boolean.parseBoolean(text);
                        break;
                    case pseudoLayer:
                        curLayer.pseudoLayer = text;
                        break;
                    case widthOffset:
                        curArc.widthOffset = Double.parseDouble(text);
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
                    case portTopology:
                        curPort.portTopology = Integer.parseInt(text);
                        break;
                    case portArc:
                        if (curLayer != null && curLayer.pureLayerNode != null)
                            curLayer.pureLayerNode.portArcs.add(text);
                        if (curPort != null)
                            curPort.portArcs.add(text);
                        break;
                    case specialValue:
                        curNode.specialValues[curSpecialValueIndex++] = Double.parseDouble(text);
                        break;
                    case menuArc:
                        curMenuBox.add(tech.findArc(text));
                        break;
                    case menuNode:
                        curMenuBox.add(tech.findNode(text));
                        break;
                    case menuText:
                        curMenuBox.add(text);
                        break;
                    case lambda:
                        curDistance.value += Double.parseDouble(text);
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
                            curR, curG, curB, opacity, foreground, pattern.clone());
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
                case pureLayerNode:
                    
                case wipable:
                case curvable:
                case special:
                case notUsed:
                case skipSizeInPalette:
                case defaultWidth:
                case arcLayer:
                    
                case shrinkArcs:
                case square:
                case canBeZeroSize:
                case wipes:
                case lockable:
                case edgeSelect:
                case lowVt:
                case highVt:
                case nativeBit:
                case od18:
                case od25:
                case od33:
                    
                case defaultHeight:
                case sizeOffset:
                case box:
                case minbox:
                case points:
                case multicutbox:
                case techPoint:
                case portAngle:
                case polygonal:
                case serpTrans:
                case minSizeRule:
                case spiceLine:
                case menuPalette:
                case menuBox:
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
    
    public static void writeXml(String fileName, Xml.Technology t) {
        try {
            PrintStream buffWriter = new PrintStream(new FileOutputStream(fileName));
            Writer writer = new Writer(buffWriter);
            writer.writeTechnology(t);
            buffWriter.close();
            System.out.println("Wrote " + fileName);
        } catch (IOException e) {
            System.out.println("Error creating " + fileName);
        }
    }
    
    
    private static class Writer {
        private static final int INDENT_WIDTH = 4;
        private final PrintStream out;
        private int indent;
        private boolean indentEmitted;
        
        Writer(PrintStream out) {
            this.out = out;
        }
        
        private void writeTechnology(Xml.Technology t) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(new Date());
            
            header();
            
            pl("");
            out.println("<!--");
            pl(" *");
            pl(" * Electric(tm) VLSI Design System");
            pl(" *");
            pl(" * File: " + t.techName + ".xml");
            pl(" * " + t.techName + " technology description");
            pl(" * Generated automatically from a library");
            pl(" *");
            pl(" * Copyright (c) " + cal.get(Calendar.YEAR) + " Sun Microsystems and Static Free Software");
            pl(" *");
            pl(" * Electric(tm) is free software; you can redistribute it and/or modify");
            pl(" * it under the terms of the GNU General Public License as published by");
            pl(" * the Free Software Foundation; either version 2 of the License, or");
            pl(" * (at your option) any later version.");
            pl(" *");
            pl(" * Electric(tm) is distributed in the hope that it will be useful,");
            pl(" * but WITHOUT ANY WARRANTY; without even the implied warranty of");
            pl(" * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the");
            pl(" * GNU General Public License for more details.");
            pl(" *");
            pl(" * You should have received a copy of the GNU General Public License");
            pl(" * along with Electric(tm); see the file COPYING.  If not, write to");
            pl(" * the Free Software Foundation, Inc., 59 Temple Place, Suite 330,");
            pl(" * Boston, Mass 02111-1307, USA.");
            pl(" */");
            out.println("-->");
            l();
            
            b(XmlKeyword.technology); a("name", t.techName); l();
            a("xmlns", "http://electric.sun.com/Technology"); l();
            a("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance"); l();
            a("xsi:schemaLocation", "http://electric.sun.com/Technology ../../technology/Technology.xsd"); cl();
            l();
            
            bcpel(XmlKeyword.shortName, t.shortTechName);
            bcpel(XmlKeyword.description, t.description);
            b(XmlKeyword.numMetals); a("min", t.minNumMetals); a("max", t.maxNumMetals); a("default", t.defaultNumMetals); el();
            b(XmlKeyword.scale); a("value", t.scaleValue); a("relevant", t.scaleRelevant); el();
            b(XmlKeyword.defaultFoundry); a("value", t.defaultFoundry); el();
            b(XmlKeyword.minResistance); a("value", t.minResistance); el();
            b(XmlKeyword.minCapacitance); a("value", t.minCapacitance); el();
//        printlnAttribute("  gateLengthSubtraction", gi.gateShrinkage);
//        printlnAttribute("  gateInclusion", gi.includeGateInResistance);
//        printlnAttribute("  groundNetInclusion", gi.includeGround);
            l();
            
            if (t.transparentLayers.size() != 0) {
                comment("Transparent layers");
                for (int i = 0; i < t.transparentLayers.size(); i++) {
                    Color color = t.transparentLayers.get(i);
                    b(XmlKeyword.transparentLayer); a("transparent", i + 1); cl();
                    bcpel(XmlKeyword.r, color.getRed());
                    bcpel(XmlKeyword.g, color.getGreen());
                    bcpel(XmlKeyword.b, color.getBlue());
                    el(XmlKeyword.transparentLayer);
                }
                l();
            }
            
            comment("**************************************** LAYERS ****************************************");
            for (Xml.Layer li: t.layers) {
                writeXml(li);
            }
            
            comment("******************** ARCS ********************");
            for (Xml.ArcProto ai: t.arcs) {
                writeXml(ai);
                l();
            }
            
            comment("******************** NODES ********************");
            for (Xml.PrimitiveNode ni: t.nodes) {
                writeXml(ni);
                l();
            }
            
            for (Xml.SpiceHeader spiceHeader: t.spiceHeaders)
                writeSpiceHeaderXml(spiceHeader);
            
//            writeMenuPaletteXml(gi.menuPalette);
//            
//            writeDesignRulesXml(gi.defaultFoundry, lList, gi.conDist, gi.unConDist);
//            
//            el(XmlKeyword.technology);
        }
        
        private void writeXml(Xml.Layer li) {
            EGraphics desc = li.desc;
            String funString = null;
            int funExtra = li.extraFunction;
            if (funExtra != 0) {
                final int deplEnhMask = com.sun.electric.technology.Layer.Function.DEPLETION|com.sun.electric.technology.Layer.Function.ENHANCEMENT;
                if ((funExtra&deplEnhMask) != 0) {
                    funString = com.sun.electric.technology.Layer.Function.getExtraName(funExtra&(deplEnhMask));
                    funExtra &= ~deplEnhMask;
                    if (funExtra != 0)
                        funString += "_" + com.sun.electric.technology.Layer.Function.getExtraName(funExtra);
                } else {
                    funString = com.sun.electric.technology.Layer.Function.getExtraName(funExtra);
                }
            }
            b(XmlKeyword.layer); a("name", li.name); a("fun", li.function.name()); a("extraFun", funString); cl();
            
            if (desc.getTransparentLayer() > 0) {
                b(XmlKeyword.transparentColor); a("transparent", desc.getTransparentLayer()); el();
            } else {
                Color color = desc.getColor();
                b(XmlKeyword.opaqueColor); a("r", color.getRed()); a("g", color.getGreen()); a("b", color.getBlue()); el();
            }
            
            bcpel(XmlKeyword.patternedOnDisplay, desc.isPatternedOnDisplay());
            bcpel(XmlKeyword.patternedOnPrinter, desc.isPatternedOnPrinter());
            
            int [] pattern = desc.getPattern();
            for(int j=0; j<16; j++) {
                String p = "";
                for(int k=0; k<16; k++)
                    p += (pattern[j] & (1 << (15-k))) != 0 ? 'X' : ' ';
                bcpel(XmlKeyword.pattern, p);
            }
            
            if (li.desc.getOutlined() != null)
                bcpel(XmlKeyword.outlined, desc.getOutlined().getConstName());
            bcpel(XmlKeyword.opacity, desc.getOpacity());
            bcpel(XmlKeyword.foreground, desc.getForeground());
            
            // write the 3D information
            if (li.thick3D != 0 || li.height3D != 0) {
                b(XmlKeyword.display3D); a("thick", li.thick3D); a("height", li.height3D); el();
            }
            
            if (li.cif != null && li.cif.length() > 0) {
                b(XmlKeyword.cifLayer); a("cif", li.cif); el();
            }
            
            // write the SPICE information
            if (li.resistance != 0 || li.capacitance != 0 || li.edgeCapacitance != 0) {
                b(XmlKeyword.parasitics); a("resistance", li.resistance); a("capacitance", li.capacitance); a("edgeCapacitance", li.edgeCapacitance); el();
            }
            bcpel(XmlKeyword.pseudoLayer, li.pseudoLayer);
            if (li.pureLayerNode != null) {
                String nodeName = li.pureLayerNode.name;
                Poly.Type style = li.pureLayerNode.style;
                String styleStr = style == Poly.Type.FILLED ? null : style.name();
                String portName = li.pureLayerNode.port;
                b(XmlKeyword.pureLayerNode); a("name", nodeName); a("style", styleStr); a("port", portName); cl();
                bcpel(XmlKeyword.lambda, li.pureLayerNode.size.value);
                for (String portArc: li.pureLayerNode.portArcs)
                    bcpel(XmlKeyword.portArc, portArc);
                el(XmlKeyword.pureLayerNode);
            }
            el(XmlKeyword.layer);
            l();
        }
        
        private void writeXml(Xml.ArcProto ai) {
            b(XmlKeyword.arcProto); a("name", ai.name); a("fun", ai.function.getConstantName()); cl();
            
            if (ai.wipable)
                bel(XmlKeyword.wipable);
            if (ai.curvable)
                bel(XmlKeyword.curvable);
            if (ai.special)
                bel(XmlKeyword.special);
            if (ai.notUsed)
                bel(XmlKeyword.notUsed);
            if (ai.skipSizeInPalette)
                bel(XmlKeyword.skipSizeInPalette);
            bcpel(XmlKeyword.extended, ai.extended);
            bcpel(XmlKeyword.fixedAngle, ai.fixedAngle);
            bcpel(XmlKeyword.angleIncrement, ai.angleIncrement);
            bcpel(XmlKeyword.antennaRatio, ai.antennaRatio);
            
            if (ai.widthOffset != 0)
                bcpel(XmlKeyword.widthOffset, ai.widthOffset);
            
            bcl(XmlKeyword.defaultWidth);
            bcpel(XmlKeyword.lambda, DBMath.round(ai.defaultWidth.value - ai.widthOffset));
            el(XmlKeyword.defaultWidth);
            
            for (Xml.ArcLayer al: ai.arcLayers) {
                String style = al.style == Poly.Type.FILLED ? "FILLED" : "CLOSED";
                b(XmlKeyword.arcLayer); a("layer", al.layer); a("style", style);
                double extend = DBMath.round(ai.widthOffset - al.width.value);
                if (extend == 0) {
                    el();
                } else {
                    cl();
                    bcpel(XmlKeyword.lambda, extend);
                    el(XmlKeyword.arcLayer);
                }
            }
            el(XmlKeyword.arcProto);
        }
        
        private void writeXml(Xml.PrimitiveNode ni) {
            b(XmlKeyword.primitiveNode); a("name", ni.name); a("fun", ni.function.name()); cl();
            
            if (ni.shrinkArcs)
                bel(XmlKeyword.shrinkArcs);
            if (ni.square)
                bel(XmlKeyword.square);
            if (ni.canBeZeroSize)
                bel(XmlKeyword.canBeZeroSize);
            if (ni.wipes)
                bel(XmlKeyword.wipes);
            if (ni.lockable)
                bel(XmlKeyword.lockable);
            if (ni.edgeSelect)
                bel(XmlKeyword.edgeSelect);
            if (ni.skipSizeInPalette)
                bel(XmlKeyword.skipSizeInPalette);
            if (ni.notUsed)
                bel(XmlKeyword.notUsed);
            if (ni.lowVt)
                bel(XmlKeyword.lowVt);
            if (ni.highVt)
                bel(XmlKeyword.highVt);
            if (ni.nativeBit)
                bel(XmlKeyword.nativeBit);
            if (ni.od18)
                bel(XmlKeyword.od18);
            if (ni.od25)
                bel(XmlKeyword.od25);
            if (ni.od33)
                bel(XmlKeyword.od33);
            
            bcl(XmlKeyword.defaultWidth);
            bcpel(XmlKeyword.lambda, DBMath.round(ni.defaultWidth.value));
            el(XmlKeyword.defaultWidth);
            
            bcl(XmlKeyword.defaultHeight);
            bcpel(XmlKeyword.lambda, DBMath.round(ni.defaultHeight.value));
            el(XmlKeyword.defaultHeight);
            if (ni.sizeOffset != null) {
                double lx = ni.sizeOffset.getLowXOffset();
                double hx = ni.sizeOffset.getHighXOffset();
                double ly = ni.sizeOffset.getLowYOffset();
                double hy = ni.sizeOffset.getHighYOffset();
                b(XmlKeyword.sizeOffset); a("lx", lx); a("hx", hx); a("ly", ly); a("hy", hy); el();
            }
            
            for(int j=0; j<ni.nodeLayers.size(); j++) {
                Xml.NodeLayer nl = ni.nodeLayers.get(j);
                Integer portNum = nl.portNum != 0 ? Integer.valueOf(nl.portNum) : null;
                b(XmlKeyword.nodeLayer); a("layer", nl.layer); a("style", nl.style.name()); a(" portNum", portNum);
                if (!(nl.inLayers && nl.inElectricalLayers))
                    a("electrical", nl.inElectricalLayers);
                cl();
                switch (nl.representation) {
                    case com.sun.electric.technology.Technology.NodeLayer.BOX:
                        b(XmlKeyword.box); el();
                        break;
                    case com.sun.electric.technology.Technology.NodeLayer.MINBOX:
                        b(XmlKeyword.minbox); el();
                        break;
                    case com.sun.electric.technology.Technology.NodeLayer.POINTS:
                        b(XmlKeyword.points); el();
                        break;
                    case com.sun.electric.technology.Technology.NodeLayer.MULTICUTBOX:
                        b(XmlKeyword.multicutbox); a("sizex", nl.sizex); a("sizey", nl.sizey); a("sep1d", nl.sep1d);  a("sep2d", nl.sep2d); el();
                        break;
                }
                for (TechPoint tp: nl.techPoints) {
                    double xm = tp.getX().getMultiplier();
                    double xa = tp.getX().getAdder();
                    double ym = tp.getY().getMultiplier();
                    double ya = tp.getY().getAdder();
                    b(XmlKeyword.techPoint); a("xm", xm); a("xa", xa); a("ym", ym); a("ya", ya); el();
                }
                el(XmlKeyword.nodeLayer);
            }
            for (int j = 0; j < ni.ports.size(); j++) {
                Xml.PrimitivePort pd = ni.ports.get(j);
                b(XmlKeyword.primitivePort); a("name", pd.name); cl();
                b(XmlKeyword.portAngle); a("primary", pd.portAngle); a("range", pd.portRange); el();
                bcpel(XmlKeyword.portTopology, pd.portTopology);
                for (int k = 0; k < 2; k++) {
                    TechPoint p = k == 0 ? pd.p0 : pd.p1;
                    double xm = p.getX().getMultiplier();
                    double xa = p.getX().getAdder();
                    double ym = p.getY().getMultiplier();
                    double ya = p.getY().getAdder();
                    b(XmlKeyword.techPoint); a("xm", xm); a("xa", xa); a("ym", ym); a("ya", ya); el();
                }
                for (String portArc: pd.portArcs)
                    bcpel(XmlKeyword.portArc, portArc);
                el(XmlKeyword.primitivePort);
            }
            switch (ni.specialType) {
                case com.sun.electric.technology.PrimitiveNode.POLYGONAL:
                    bel(XmlKeyword.polygonal);
                    break;
                case com.sun.electric.technology.PrimitiveNode.SERPTRANS:
                    b(XmlKeyword.serpTrans); cl();
                    for (int i = 0; i < 6; i++) {
                        bcpel(XmlKeyword.specialValue, ni.specialValues[i]);
                    }
                    el(XmlKeyword.serpTrans);
                    break;
            }
            if (ni.nodeSizeRule != null) {
                com.sun.electric.technology.PrimitiveNode.NodeSizeRule r = ni.nodeSizeRule;
                b(XmlKeyword.minSizeRule); a("width", r.getWidth()); a("height", r.getHeight()); a("rule", r.getRuleName()); el();
            }
            
            el(XmlKeyword.primitiveNode);
        }
        
        private void writeSpiceHeaderXml(Xml.SpiceHeader spiceHeader) {
            b(XmlKeyword.spiceHeader); a("level", spiceHeader.level); cl();
            for (String line: spiceHeader.spiceLines) {
                b(XmlKeyword.spiceLine); a("line", line); el();
            }
            el(XmlKeyword.spiceHeader);
            l();
        }
        
//        private void writeMenuPaletteXml(Object[][] menuPalette) {
//            if (menuPalette == null) return;
//            int numColumns = menuPalette[0].length;
//            b("menuPalette"); a("numColumns", numColumns); cl();
//            for (Object[] menuLine: menuPalette) {
//                l();
//                for (int i = 0; i < numColumns; i++)
//                    writeMenuBoxXml(menuLine[i]);
//            }
//            l();
//            el("menuPalette");
//            l();
//        }
//        
//        private void writeMenuBoxXml(Object o) {
//            b("menuBox");
//            if (o == null) {
//                el();
//                return;
//            }
//            cl();
//            if (o instanceof ArcInfo)
//                bcpel("menuArc", ((ArcInfo)o).name);
//            else if (o instanceof NodeInfo)
//                bcpel("menuNode", ((NodeInfo)o).name);
//            else
//                bcpel("menuText", o);
//            el("menuBox");
//        }
//        
//        private void writeDesignRulesXml(String foundry, LayerInfo[] lList, double[] conDist, double[] uConDist) {
//            b("Foundry"); a("name", foundry); cl();
//            
//            if (conDist != null && uConDist != null) {
//                int layerTotal = lList.length;
//                int ruleIndex = 0;
//                for (int i1 = 0; i1 < layerTotal; i1++) {
//                    LayerInfo l1 = lList[i1];
//                    for (int i2 = i1; i2 < layerTotal; i2++) {
//                        LayerInfo l2 = lList[i2];
//                        double conSpa = conDist[ruleIndex];
//                        double uConSpa = uConDist[ruleIndex];
//                        if (conSpa > -1)
//                            printDesignRule("C" + ruleIndex, l1, l2, "CONSPA", conSpa);
//                        if (uConSpa > -1)
//                            printDesignRule("U" + ruleIndex, l1, l2, "UCONSPA", uConSpa);
//                        ruleIndex++;
//                    }
//                }
//            }
//            el("Foundry");
//        }
//        
        private void header() {
            checkIndent();
            out.print("<?xml"); a("version", "1.0"); a("encoding", "UTF-8"); out.println("?>");
        }
        
        private void comment(String s) {
            checkIndent();
            out.print("<!-- "); p(s); out.print(" -->"); l();
        }
        
        /**
         * Print attribute.
         */
        private void a(String name, Object value) {
            checkIndent();
            if (value == null) return;
            out.print(" " + name + "=\"");
            p(value.toString());
            out.print("\"");
        }
        
        private void bcpel(XmlKeyword key, Object v) {
            if (v == null) return;
            b(key); c(); p(v.toString()); el(key);
        }
        
        private void bcl(XmlKeyword key) {
            b(key); cl();
        }
        
        private void bel(XmlKeyword key) {
            b(key); el();
        }
        
        /**
         * Print text with replacement of special chars.
         */
        private void pl(String s) {
            checkIndent();
            p(s); l();
        }
        
        /**
         * Print text with replacement of special chars.
         */
        private void p(String s) {
            assert indentEmitted;
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                switch (c) {
                    case '<':
                        out.print("&lt;");
                        break;
                    case '>':
                        out.print("&gt;");
                        break;
                    case '&':
                        out.print("&amp;");
                        break;
                    case '\'':
                        out.print("&apos;");
                        break;
                    case '"':
                        out.print("quot;");
                        break;
                    default:
                        out.print(c);
                }
            }
        }
        
        /**
         * Print element name, and indent.
         */
        private void b(XmlKeyword key) {
            checkIndent();
            out.print('<');
            out.print(key.name());
            indent += INDENT_WIDTH;
        }
        
        private void cl() {
            assert indentEmitted;
            out.print('>');
            l();
        }
        
        private void c() {
            assert indentEmitted;
            out.print('>');
        }
        
        private void el() {
            e(); l();
        }
        
        private void e() {
            assert indentEmitted;
            out.print("/>");
            indent -= INDENT_WIDTH;
        }
        
        private void el(XmlKeyword key) {
            indent -= INDENT_WIDTH;
            checkIndent();
            out.print("</");
            out.print(key.name());
            out.print(">");
            l();
        }
        
        private void checkIndent() {
            if (indentEmitted) return;
            for (int i = 0; i < indent; i++)
                out.print(' ');
            indentEmitted = true;
        }
        
        /**
         *  Print new line.
         */
        private void l() {
            out.println();
            indentEmitted = false;
        }
    }
}
