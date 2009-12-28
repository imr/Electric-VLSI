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

import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.geometry.EGraphics;
import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.technology.Technology.TechPoint;
import com.sun.electric.tool.Job;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

import javax.xml.XMLConstants;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

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
	/** Default Logical effort gate capacitance. */			public static final double DEFAULT_LE_GATECAP      = 0.4;
	/** Default Logical effort wire ratio. */				public static final double DEFAULT_LE_WIRERATIO    = 0.16;
	/** Default Logical effort diff alpha. */				public static final double DEFAULT_LE_DIFFALPHA    = 0.7;

    public static class Technology implements Serializable {
        public String techName;
        public String className;
        public String shortTechName;
        public String description;
        public final List<Version> versions = new ArrayList<Version>();
        public int minNumMetals;
        public int maxNumMetals;
        public int defaultNumMetals;
        public double scaleValue;
        public double resolutionValue; // min resolution value allowed by the foundry
        public boolean scaleRelevant;
        public String defaultFoundry;
        public double minResistance;
        public double minCapacitance;
        public double leGateCapacitance = DEFAULT_LE_GATECAP;
        public double leWireRatio = DEFAULT_LE_WIRERATIO;
        public double leDiffAlpha = DEFAULT_LE_DIFFALPHA;
        public final List<Color> transparentLayers = new ArrayList<Color>();
        public final List<Layer> layers = new ArrayList<Layer>();
        public final List<ArcProto> arcs = new ArrayList<ArcProto>();
        public final List<PrimitiveNodeGroup> nodeGroups = new ArrayList<PrimitiveNodeGroup>();
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

        public Collection<String> getLayerNames()
        {
            List<String> l = new ArrayList<String>();
            for (Layer layer: layers)
                l.add(layer.name);
            return l;
        }

        public ArcProto findArc(String name) {
            for (ArcProto arc: arcs) {
                if (arc.name.equals(name))
                    return arc;
            }
            return null;
        }

        public PrimitiveNodeGroup findNodeGroup(String name) {
            for (PrimitiveNodeGroup nodeGroup: nodeGroups) {
                for (PrimitiveNode n: nodeGroup.nodes) {
                    if (n.name.equals(name))
                        return nodeGroup;
                }
            }
            return null;
        }

        public PrimitiveNode findNode(String name) {
            for (PrimitiveNodeGroup nodeGroup: nodeGroups) {
                for (PrimitiveNode n: nodeGroup.nodes) {
                    if (n.name.equals(name))
                        return n;
                }
            }
            return null;
        }

        public Collection<String> getNodeNames() {
            List<String> l = new ArrayList<String>();
            for (PrimitiveNodeGroup nodeGroup: nodeGroups) {
                for (PrimitiveNode n: nodeGroup.nodes) {
                    l.add(n.name);
                }
            }
            return l;
        }

        /**
         * Method to find the first PrimitiveNode which would have the given arc
         */
        public PrimitiveNode findPinNode(String arc)
        {
            for (PrimitiveNodeGroup nodeGroup: nodeGroups)
            {
                boolean foundPort = false;
                for (PrimitivePort p: nodeGroup.ports)
                {
                    if(p.portArcs.contains(arc))
                    {
                        foundPort = true;
                        break;
                    }
                }
                if (foundPort) // now checking if there is a pin associated with the arc
                {
                    for (PrimitiveNode n: nodeGroup.nodes)
                    {
                        if (n.function.isPin())
                            return n;
                    }
                }
            }
            return null;
        }

        public void writeXml(String fileName) {
            writeXml(fileName, true, null);
        }

        public void writeXml(String fileName, boolean includeDateAndVersion, String copyrightMessage) {
            try {
                PrintWriter out = new PrintWriter(fileName);
                Writer writer = new Writer(out);
                writer.writeTechnology(this, includeDateAndVersion, copyrightMessage);
                out.close();
                System.out.println("Wrote " + fileName);
                System.out.println(" (Add this file to the 'Added Technologies' Project Preferences to install it in Electric)");
            } catch (IOException e) {
                System.out.println("Error creating " + fileName);
            }
        }

        public Technology deepClone() {
            try {
                ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
                ObjectOutputStream out = new ObjectOutputStream(byteStream);
                out.writeObject(this);
                out.flush();
                byte[] serializedXml = byteStream.toByteArray();
                ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(serializedXml));
                Xml.Technology clone = (Xml.Technology)in.readObject();
                in.close();
                return clone;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    public static class Version implements Serializable {
        public int techVersion;
        public com.sun.electric.database.text.Version electricVersion;
    }

    public static class Layer implements Serializable {
        public String name;
        public com.sun.electric.technology.Layer.Function function;
        public int extraFunction;
        public EGraphics desc;
        public double thick3D;
        public double height3D;
        public String cif;
        public String skill;
        public double resistance;
        public double capacitance;
        public double edgeCapacitance;
        public PureLayerNode pureLayerNode;
    }

    public static class PureLayerNode implements Serializable {
        public String name;
        public String oldName;
        public Poly.Type style;
        public String port;
        public final Distance size = new Distance();
        public final List<String> portArcs = new ArrayList<String>();
    }

    public static class ArcProto implements Serializable {
        public String name;
        public String oldName;
        public com.sun.electric.technology.ArcProto.Function function;
        public boolean wipable;
        public boolean curvable;
        public boolean special;
        public boolean notUsed;
        public boolean skipSizeInPalette;

        public final Map<Integer,Double> diskOffset = new TreeMap<Integer,Double>();
        public final Distance defaultWidth = new Distance();
        public boolean extended;
        public boolean fixedAngle;
        public int angleIncrement;
        public double antennaRatio;
        public final List<ArcLayer> arcLayers = new ArrayList<ArcLayer>();
    }

    public static class ArcLayer implements Serializable {
        public String layer;
        public final Distance extend = new Distance();
        public Poly.Type style;
    }

    public static class PrimitiveNode implements Serializable {
        public String name;
        public com.sun.electric.technology.PrimitiveNode.Function function;
        public String oldName;
        public boolean lowVt;
        public boolean highVt;
        public boolean nativeBit;
        public boolean od18;
        public boolean od25;
        public boolean od33;
    }

    public static class PrimitiveNodeGroup implements Serializable {
        public boolean isSingleton;
        public final List<PrimitiveNode> nodes = new ArrayList<PrimitiveNode>();
        public boolean shrinkArcs;
        public boolean square;
        public boolean canBeZeroSize;
        public boolean wipes;
        public boolean lockable;
        public boolean edgeSelect;
        public boolean skipSizeInPalette;
        public boolean notUsed;

        public final Map<Integer,EPoint> diskOffset = new TreeMap<Integer,EPoint>();
        public final Distance defaultWidth = new Distance();
        public final Distance defaultHeight = new Distance();

        public final Distance baseLX = new Distance();
        public final Distance baseHX = new Distance();
        public final Distance baseLY = new Distance();
        public final Distance baseHY = new Distance();

        public ProtectionType protection;
        public final List<NodeLayer> nodeLayers = new ArrayList<NodeLayer>();
        public final List<PrimitivePort> ports = new ArrayList<PrimitivePort>();
        public int specialType;
        public double[] specialValues;
        public NodeSizeRule nodeSizeRule;
        public String spiceTemplate;
    }

    public enum ProtectionType {
        both, left, right, none;
    }

    public static class NodeLayer implements Serializable {
        public String layer;
        public BitSet inNodes;
        public Poly.Type style;
        public int portNum;
        public boolean inLayers;
        public boolean inElectricalLayers;
        public int representation;
        public final Distance lx = new Distance();
        public final Distance hx = new Distance();
        public final Distance ly = new Distance();
        public final Distance hy = new Distance();
        public final List<TechPoint> techPoints = new ArrayList<TechPoint>();
        public double sizex, sizey, sep1d, sep2d;
        public double lWidth, rWidth, tExtent, bExtent;
    }

    public static class NodeSizeRule implements Serializable {
        public double width;
        public double height;
        public String rule;
    }

    public static class PrimitivePort implements Serializable {
        public String name;
        public int portAngle;
        public int portRange;
        public int portTopology;
        public final Distance lx = new Distance();
        public final Distance hx = new Distance();
        public final Distance ly = new Distance();
        public final Distance hy = new Distance();
        public final List<String> portArcs = new ArrayList<String>();
    }

    public static class SpiceHeader implements Serializable {
        public int level;
        public final List<String> spiceLines = new ArrayList<String>();
    }

    public static class MenuPalette implements Serializable {
        public int numColumns;
        public List<List<?>> menuBoxes = new ArrayList<List<?>>();

        public String writeXml() {
            StringWriter sw = new StringWriter();
            PrintWriter out = new PrintWriter(sw);
            Xml.OneLineWriter writer = new Xml.OneLineWriter(out);
            writer.writeMenuPaletteXml(this);
            out.close();
            return sw.getBuffer().toString();
        }
    }

    public static class MenuNodeInst implements Serializable {
        /** the name of the prototype in the menu */			public String protoName;
        /** the function of the prototype */					public com.sun.electric.technology.PrimitiveNode.Function function;
        /** tech bits */                                        public int techBits;
        /** label to draw in the menu entry (may be null) */	public String text;
        /** the rotation of the node in the menu entry */		public int rotation;
    }

    public static class Distance implements Serializable {
        public double k;
        public double value;

        public void addLambda(double lambdaValue) {
            value += lambdaValue;
        }
    }

    public static class Foundry implements Serializable {
        public String name;
        public final Map<String,String> layerGds = new LinkedHashMap<String,String>();
        public final List<DRCTemplate> rules = new ArrayList<DRCTemplate>();
    }

    private Xml() {}

    private static enum XmlKeyword {
        technology,
        shortName(true),
        description(true),
        version,
        numMetals,
        scale,
        resolution,
        defaultFoundry,
        minResistance,
        minCapacitance,
        logicalEffort,
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
        skillLayer,
        parasitics,
        pureLayerNode,

        arcProto,
        oldName(true),
        wipable,
        curvable,
        special,
        notUsed,
        skipSizeInPalette,

        extended(true),
        fixedAngle(true),
        angleIncrement(true),
        antennaRatio(true),

        diskOffset,
        defaultWidth,
        arcLayer,

        primitiveNodeGroup,
        inNodes,
        primitiveNode,
        //oldName(true),
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
 //       defaultWidth,
        defaultHeight,
        nodeBase,
        sizeOffset,
        nodeLayer,
        box,
        multicutbox,
        serpbox,
        lambdaBox,
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
        // Protection layer for transistors
        protection,
//        location(true),
        minSizeRule,
        spiceTemplate,
        spiceHeader,
        spiceLine,
        menuPalette,
        menuBox,
        menuArc(true),
        menuNode(true),
        menuText(true),
        menuNodeInst,
        menuNodeText,
        lambda(true),
        Foundry,
        layerGds,
        LayerRule,
        LayersRule,
        NodeLayersRule,
        NodeRule;

        private final boolean hasText;

        private XmlKeyword() {
            hasText = false;
        };
        private XmlKeyword(boolean hasText) {
            this.hasText = hasText;
        }
    };

    private static final Map<String,XmlKeyword> xmlKeywords = new HashMap<String,XmlKeyword>();
    static {
        for (XmlKeyword k: XmlKeyword.class.getEnumConstants())
            xmlKeywords.put(k.name(), k);
    }

    private static Schema schema = null;

    private static synchronized void loadTechnologySchema() throws SAXException {
        if (schema != null) return;
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        URL technologySchemaUrl = Technology.class.getResource("Technology.xsd");
        if (technologySchemaUrl != null)
            schema = schemaFactory.newSchema(technologySchemaUrl);
        else
        {
            System.err.println("Schema file Technology.xsd, working without XML schema");
            System.out.println("Schema file Technology.xsd, working without XML schema");
        }
    }

    public static Technology parseTechnology(URL fileURL) {
//        System.out.println("Memory usage " + Main.getMemoryUsage() + " bytes");
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        try {
            if (schema == null)
                loadTechnologySchema();
            factory.setSchema(schema);
    //        factory.setValidating(true);
    //        System.out.println("Memory usage " + Main.getMemoryUsage() + " bytes");
            // create the parser
            long startTime = System.currentTimeMillis();
            SAXParser parser = factory.newSAXParser();
            URLConnection urlCon = fileURL.openConnection();
            InputStream inputStream = urlCon.getInputStream();

            XMLReader handler = new XMLReader();
            parser.parse(inputStream, handler);
            if (Job.getDebug())
            {
                long stopTime = System.currentTimeMillis();
                System.out.println("Loading technology " + fileURL + " ... " + (stopTime - startTime) + " msec");
            }
            return handler.tech;
        } catch (SAXParseException e) {
            String msg = "Error parsing Xml technology:\n" +
                    e.getMessage() + "\n" +
                    " Line " + e.getLineNumber() + " column " + e.getColumnNumber() + " of " + fileURL;
            msg = msg.replaceAll("\"http://electric.sun.com/Technology\":", "");
            System.out.println(msg);
            Job.getUserInterface().showErrorMessage(msg, "Error parsing Xml technology");
        } catch (Exception e) {
            String msg = "Error loading Xml technology " + fileURL + " :\n"
                   + e.getMessage() + "\n";
            System.out.println(msg);
            Job.getUserInterface().showErrorMessage(msg, "Error loading Xml technology");
        } catch (Error a)
        {
            String msg = "Assertion while loading Xml technology " + fileURL;
            System.out.println(msg);
            //Job.getUserInterface().showErrorMessage(msg, "Error loading Xml technology");
        }
        return null;
    }

    /**
     * Method to parse a string of XML that describes the component menu in a Technology Editing context.
     * Normal parsing of XML returns objects in the Xml class, but
     * this method returns objects in a given Technology-Editor world.
     * @param xml the XML string
     * @param nodeGroups the PrimitiveNodeGroup objects describing nodes in the technology.
     * @param arcs the ArcProto objects describing arcs in the technology.
     * @return the MenuPalette describing the component menu.
     */
    public static MenuPalette parseComponentMenuXMLTechEdit(String xml, List<PrimitiveNodeGroup> nodeGroups, List<ArcProto> arcs)
    {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        try
        {
            SAXParser parser = factory.newSAXParser();
            InputSource is = new InputSource(new StringReader(xml));
            XMLReader handler = new XMLReader(nodeGroups, arcs);
            parser.parse(is, handler);
            return handler.tech.menuPalette;
        } catch (Exception e)
        {
            System.out.println("Error parsing XML component menu data");
            e.printStackTrace();
        }
        return null;
    }

    private static class XMLReader extends DefaultHandler {
        private static boolean DEBUG = false;
        private Locator locator;

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
        private EGraphics.J3DTransparencyOption transparencyMode;
        private double transparencyFactor;
        private ArcProto curArc;
        private PrimitiveNodeGroup curNodeGroup;
        private boolean curNodeGroupHasNodeBase;
        private PrimitiveNode curNode;
        private NodeLayer curNodeLayer;
        private PrimitivePort curPort;
        private int curSpecialValueIndex;
        private ArrayList<Object> curMenuBox;
        private MenuNodeInst curMenuNodeInst;
        private Distance curDistance;
        private SpiceHeader curSpiceHeader;
        private Foundry curFoundry;
        private Collection<String> curLayerNamesList;
        private Collection<String> curNodeNamesList;

        private boolean acceptCharacters;
        private StringBuilder charBuffer = new StringBuilder();
        private Attributes attributes;

        XMLReader() {
        }

        XMLReader(List<PrimitiveNodeGroup> nodeGroups, List<ArcProto> arcs)
        {
            tech.arcs.addAll(arcs);
            tech.nodeGroups.addAll(nodeGroups);
        }

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
//            int x = 0;
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
//            int x = 0;
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
            this.locator = locator;
        }

        private void printLocator() {
            System.out.println("publicId=" + locator.getPublicId() + " systemId=" + locator.getSystemId() +
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
            if (DEBUG) {
                System.out.println("startDocument");
            }
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
            if (DEBUG) {
                System.out.println("endDocument");
            }
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
            if (DEBUG) {
                System.out.println("startPrefixMapping prefix=" + prefix + " uri=" + uri);
            }
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
            if (DEBUG) {
                System.out.println("endPrefixMapping prefix=" + prefix);
            }
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
                    tech.className = a_("class");
                    if (tech.className != null)
                    {
                        int index = tech.className.indexOf(".");
                        String realName = tech.className;
                        while (index != -1)
                        {
                            realName = realName.substring(index+1);
                            index = realName.indexOf(".");
                        }
                        if (!realName.toLowerCase().equals(tech.techName.toLowerCase()))
                            System.out.println("Mismatch between techName '" + tech.techName +
                                "' and className '" + realName + "' in the XML technology file.");
                    }
//                    dump = true;
                    break;
                case version:
                    Version localVersion = new Version();
                    localVersion.techVersion = Integer.parseInt(a("tech"));
                    localVersion.electricVersion = com.sun.electric.database.text.Version.parseVersion(a("electric"));
                    tech.versions.add(localVersion);
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
                case resolution:
                    tech.resolutionValue = Double.parseDouble(a("value")); // default is 0;
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
                case logicalEffort:
                    tech.leGateCapacitance = Double.parseDouble(a("gateCapacitance"));
                    tech.leWireRatio = Double.parseDouble(a("wireRatio"));
                    tech.leDiffAlpha = Double.parseDouble(a("diffAlpha"));
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
                    transparencyMode = EGraphics.DEFAULT_MODE;
                    transparencyFactor = EGraphics.DEFAULT_FACTOR;
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
                    String modeStr = a_("mode");
                    if (modeStr != null)
                        transparencyMode = EGraphics.J3DTransparencyOption.valueOf(modeStr);
                    String factorStr = a_("factor");
                    if (factorStr != null)
                         transparencyFactor = Double.parseDouble(factorStr);
                    break;
                case cifLayer:
                    curLayer.cif = a("cif");
                    break;
                case skillLayer:
                    curLayer.skill = a("skill");
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
                    else if (curNodeGroup != null)
                        curNodeGroup.notUsed = true;
                    break;
                case skipSizeInPalette:
                    if (curArc != null)
                        curArc.skipSizeInPalette = true;
                    else if (curNodeGroup != null)
                        curNodeGroup.skipSizeInPalette = true;
                    break;
                case diskOffset:
                    if (curArc != null)
                        curArc.diskOffset.put(new Integer(Integer.parseInt(a("untilVersion"))),
                        	new Double(Double.parseDouble(a("width"))));
                    else if (curNodeGroup != null)
                        curNodeGroup.diskOffset.put(new Integer(Integer.parseInt(a("untilVersion"))),
                        	EPoint.fromLambda(Double.parseDouble(a("x")), Double.parseDouble(a("y"))));
                    break;
                case defaultWidth:
                    if (curArc != null)
                        curDistance = curArc.defaultWidth;
                    else if (curNodeGroup != null)
                        curDistance = curNodeGroup.defaultWidth;
                    break;
                case arcLayer:
                    ArcLayer arcLayer = new ArcLayer();
                    arcLayer.layer = a("layer");
                    curDistance = arcLayer.extend;
                    arcLayer.style = Poly.Type.valueOf(a("style"));
                    curArc.arcLayers.add(arcLayer);
                    break;
                case primitiveNodeGroup:
                    curNodeGroup = new PrimitiveNodeGroup();
                    curNodeGroupHasNodeBase = false;
                    curNode = null;
                    if (a_("name") != null)
                        System.out.println("Attribute 'name' in <primitiveNodeGroup> is deprecated");
                    if (a_("fun") != null)
                        System.out.println("Attribute 'fun' in <primitiveNodeGroup> is deprecated");
                    break;
                case inNodes:
                    if (curNodeGroup.isSingleton)
                        throw new SAXException("<inNodes> can be used only inside <primitiveNodeGroup>");
                    curNodeLayer.inNodes = new BitSet();
                    break;
                case primitiveNode:
                    if (curNodeLayer != null) {
                        assert !curNodeGroup.isSingleton && curNode == null;
                        String nodeName = a("name");
                        int i = 0;
                        while (i < curNodeGroup.nodes.size() && !curNodeGroup.nodes.get(i).name.equals(nodeName))
                            i++;
                        if (i >= curNodeGroup.nodes.size())
                            throw new SAXException("No node "+nodeName+" in group");
                        curNodeLayer.inNodes.set(i);
                    } else if (curNodeGroup != null) {
                        assert !curNodeGroup.isSingleton;
                        curNode = new PrimitiveNode();
                        curNode.name = a("name");
                        curNode.function = com.sun.electric.technology.PrimitiveNode.Function.valueOf(a("fun"));
                        curNodeGroup.nodes.add(curNode);
                    } else {
                        curNodeGroup = new PrimitiveNodeGroup();
                        curNodeGroupHasNodeBase = false;
                        curNodeGroup.isSingleton = true;
                        curNode = new PrimitiveNode();
                        curNode.name = a("name");
                        curNode.function = com.sun.electric.technology.PrimitiveNode.Function.valueOf(a("fun"));
                        curNodeGroup.nodes.add(curNode);
                    }
                    break;
                case shrinkArcs:
                    curNodeGroup.shrinkArcs = true;
                    break;
                case square:
                    curNodeGroup.square = true;
                    break;
                case canBeZeroSize:
                    curNodeGroup.canBeZeroSize = true;
                    break;
                case wipes:
                    curNodeGroup.wipes = true;
                    break;
                case lockable:
                    curNodeGroup.lockable = true;
                    break;
                case edgeSelect:
                    curNodeGroup.edgeSelect = true;
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
                    curDistance = curNodeGroup.defaultHeight;
                    break;
                case nodeBase:
                    curNodeGroupHasNodeBase = true;
                    break;
                case sizeOffset:
                    curNodeGroup.baseLX.value = Double.parseDouble(a("lx"));
                    curNodeGroup.baseHX.value = -Double.parseDouble(a("hx"));
                    curNodeGroup.baseLY.value = Double.parseDouble(a("ly"));
                    curNodeGroup.baseHY.value = -Double.parseDouble(a("hy"));
                    break;
                case protection:
                    curNodeGroup.protection = ProtectionType.valueOf(a("location"));
                    break;
                case nodeLayer:
                    curNodeLayer = new NodeLayer();
                    curNodeLayer.layer = a("layer");
                    if (tech.findLayer(curNodeLayer.layer) == null)
                    {
                        throw new SAXException("Error: cannot find layer '" + curNodeLayer.layer + "' in primitive node '" +
                        curNode.name + "'. Skiping this NodeLayer");
                    }
                    else
                    {
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
                    }
                    break;
                case box:
                    if (curNodeLayer != null) {
                        curNodeLayer.representation = com.sun.electric.technology.Technology.NodeLayer.BOX;
                        curNodeLayer.lx.k = da_("klx", -1);
                        curNodeLayer.hx.k = da_("khx", 1);
                        curNodeLayer.ly.k = da_("kly", -1);
                        curNodeLayer.hy.k = da_("khy", 1);
                    } else if (curPort != null) {
                        curPort.lx.k = da_("klx", -1);
                        curPort.hx.k = da_("khx", 1);
                        curPort.ly.k = da_("kly", -1);
                        curPort.hy.k = da_("khy", 1);
                    } else {
                        assert curNodeGroupHasNodeBase;
                        curNodeGroup.baseLX.k = curNodeGroup.baseLY.k = -1;
                        curNodeGroup.baseHX.k = curNodeGroup.baseHY.k = 1;
                    }
                    break;
                case points:
                    curNodeLayer.representation = com.sun.electric.technology.Technology.NodeLayer.POINTS;
                    break;
                case multicutbox:
                    curNodeLayer.representation = com.sun.electric.technology.Technology.NodeLayer.MULTICUTBOX;
                    curNodeLayer.lx.k = da_("klx", -1);
                    curNodeLayer.hx.k = da_("khx", 1);
                    curNodeLayer.ly.k = da_("kly", -1);
                    curNodeLayer.hy.k = da_("khy", 1);
                    curNodeLayer.sizex = Double.parseDouble(a("sizex"));
                    curNodeLayer.sizey = Double.parseDouble(a("sizey"));
                    curNodeLayer.sep1d = Double.parseDouble(a("sep1d"));
                    curNodeLayer.sep2d = Double.parseDouble(a("sep2d"));
                    break;
                case serpbox:
                    curNodeLayer.representation = com.sun.electric.technology.Technology.NodeLayer.BOX;
                    curNodeLayer.lx.k = da_("klx", -1);
                    curNodeLayer.hx.k = da_("khx", 1);
                    curNodeLayer.ly.k = da_("kly", -1);
                    curNodeLayer.hy.k = da_("khy", 1);
                    curNodeLayer.lWidth = Double.parseDouble(a("lWidth"));
                    curNodeLayer.rWidth = Double.parseDouble(a("rWidth"));
                    curNodeLayer.tExtent = Double.parseDouble(a("tExtent"));
                    curNodeLayer.bExtent = Double.parseDouble(a("bExtent"));
                    break;
                case lambdaBox:
                    if (curNodeLayer != null) {
                        curNodeLayer.lx.value = Double.parseDouble(a("klx"));
                        curNodeLayer.hx.value = Double.parseDouble(a("khx"));
                        curNodeLayer.ly.value = Double.parseDouble(a("kly"));
                        curNodeLayer.hy.value = Double.parseDouble(a("khy"));
                    } else if (curPort != null) {
                        curPort.lx.value = Double.parseDouble(a("klx"));
                        curPort.hx.value = Double.parseDouble(a("khx"));
                        curPort.ly.value = Double.parseDouble(a("kly"));
                        curPort.hy.value = Double.parseDouble(a("khy"));
                    } else {
                        assert curNodeGroupHasNodeBase;
                        curNodeGroup.baseLX.value = Double.parseDouble(a("klx"));
                        curNodeGroup.baseHX.value = Double.parseDouble(a("khx"));
                        curNodeGroup.baseLY.value = Double.parseDouble(a("kly"));
                        curNodeGroup.baseHY.value = Double.parseDouble(a("khy"));
                    }
                    break;
                case techPoint:
                    double xm = Double.parseDouble(a("xm"));
                    double xa = Double.parseDouble(a("xa"));
                    double ym = Double.parseDouble(a("ym"));
                    double ya = Double.parseDouble(a("ya"));
                    TechPoint p = new TechPoint(new EdgeH(xm, xa), new EdgeV(ym, ya));
                    if (curNodeLayer != null)
                        curNodeLayer.techPoints.add(p);
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
                    curNodeGroup.specialType = com.sun.electric.technology.PrimitiveNode.POLYGONAL;
                    break;
                case serpTrans:
                    curNodeGroup.specialType = com.sun.electric.technology.PrimitiveNode.SERPTRANS;
                    curNodeGroup.specialValues = new double[6];
                    curSpecialValueIndex = 0;
                    break;
                case minSizeRule:
                    curNodeGroup.nodeSizeRule = new NodeSizeRule();
                    curNodeGroup.nodeSizeRule.width = Double.parseDouble(a("width"));
                    curNodeGroup.nodeSizeRule.height = Double.parseDouble(a("height"));
                    curNodeGroup.nodeSizeRule.rule = a("rule");
                    break;
                case spiceTemplate:
                    curNodeGroup.spiceTemplate = a("value");
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
                case menuNodeInst:
                    curMenuNodeInst = new MenuNodeInst();
                    curMenuNodeInst.protoName = a("protoName");
                    if (tech.findNode(curMenuNodeInst.protoName) == null)
                        System.out.println("Warning: cannot find node '" + curMenuNodeInst.protoName + "' for component menu");
                    curMenuNodeInst.function =  com.sun.electric.technology.PrimitiveNode.Function.findType(a("function"));
                    if (curMenuNodeInst.function == null)
                        System.out.println("Error: cannot find function '" + a("function") + "' for node '" +
                        a("protoName" + "'"));
                    String techBits = a_("techBits");
                    if (techBits != null)
                        curMenuNodeInst.techBits = Integer.parseInt(techBits);
                    String rotField = a_("rotation");
                    if (rotField != null) curMenuNodeInst.rotation = Integer.parseInt(rotField);
                    break;
                case menuNodeText:
                    curMenuNodeInst.text = a("text");
//                    curMenuNodeInst.fontSize = Double.parseDouble(a("size"));
                    break;
                case Foundry:
                    curFoundry = new Foundry();
                    curFoundry.name = a("name");
                    tech.foundries.add(curFoundry);
                    break;
                case layerGds:
                    curFoundry.layerGds.put(a("layer"), a("gds"));
                    break;
                case LayerRule:
                case LayersRule:
                case NodeLayersRule:
                case NodeRule:
                    if (curLayerNamesList == null)
                        curLayerNamesList = tech.getLayerNames();
                    if (curNodeNamesList == null)
                        curNodeNamesList = tech.getNodeNames();
                    if (!DRCTemplate.parseXmlElement(curFoundry.rules, curLayerNamesList, curNodeNamesList,
                        key.name(), attributes, localName))
                        System.out.println("Warning: cannot find layer name in DRC rule '" + key.name());
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

        private double da_(String attrName, double defaultValue) {
            String s = a_(attrName);
            return s != null ? Double.parseDouble(s) : defaultValue;
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
                    case oldName:
                        if (curLayer != null) {
                            curLayer.pureLayerNode.oldName = text;
                        } else if (curArc != null) {
                            curArc.oldName = text;
                        } else {
                            curNode.oldName = text;
                        }
                        break;
                    case extended:
                        curArc.extended = Boolean.parseBoolean(text);
                        break;
                    case fixedAngle:
                        curArc.fixedAngle = Boolean.parseBoolean(text);
                        break;
//                    case wipable:
//                        curArc.wipable = Boolean.parseBoolean(text);
//                        break;
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
                        curNodeGroup.specialValues[curSpecialValueIndex++] = Double.parseDouble(text);
                        break;
                    case menuArc:
                    	ArcProto ap = tech.findArc(text);
                    	if (ap == null) System.out.println("Warning: cannot find arc '" + text + "' for component menu"); else
                    		curMenuBox.add(ap);
                        break;
                    case menuNode:
                    	PrimitiveNode np = tech.findNode(text);
                    	if (np == null) System.out.println("Warning: cannot find node '" + text + "' for component menu"); else
                    		curMenuBox.add(np);
                        break;
                    case menuText:
                        curMenuBox.add(text);
                        break;
                    case lambda:
                        curDistance.addLambda(Double.parseDouble(text));
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
                            curR, curG, curB, opacity, foreground, pattern.clone(), transparencyMode, transparencyFactor);
                    assert tech.findLayer(curLayer.name) == null;
                    tech.layers.add(curLayer);
                    curLayer = null;
                    break;
                case arcProto:
                    tech.arcs.add(curArc);
                    curArc = null;
                    break;
                case primitiveNodeGroup:
                    fixNodeBase();
                    tech.nodeGroups.add(curNodeGroup);
                    curNodeGroup = null;
                    curNode = null;
                    break;
                case primitiveNode:
                    if (curNodeGroup.isSingleton) {
                        fixNodeBase();
                        tech.nodeGroups.add(curNodeGroup);
                        curNodeGroup = null;
                        curNode = null;
                    } else if (curNodeLayer == null) {
                        assert !curNodeGroup.isSingleton;
                        curNode = null;
                    }
                    break;
                case nodeLayer:
                    if (curNodeLayer != null)
                    {
                        curNodeGroup.nodeLayers.add(curNodeLayer);
                        curNodeLayer = null;
                    }
                    break;
                case primitivePort:
                    curNodeGroup.ports.add(curPort);
                    curPort = null;
                    break;
                case menuNodeInst:
                	curMenuBox.add(curMenuNodeInst);
                    curMenuNodeInst = null;
                    break;

                case version:
                case spiceHeader:
                case numMetals:
                case scale:
                case resolution:
                case defaultFoundry:
                case minResistance:
                case minCapacitance:
                case logicalEffort:
                case transparentColor:
                case opaqueColor:
                case display3D:
                case cifLayer:
                case skillLayer:
                case parasitics:
                case pureLayerNode:

                case wipable:
                case curvable:
                case special:
                case notUsed:
                case skipSizeInPalette:
                case diskOffset:
                case defaultWidth:
                case arcLayer:

                case inNodes:
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
                case nodeBase:
                case sizeOffset:
                case protection:
                case box:
                case points:
                case multicutbox:
                case serpbox:
                case lambdaBox:
                case techPoint:
                case portAngle:
                case polygonal:
                case serpTrans:
                case minSizeRule:
                case spiceLine:
                case spiceTemplate:
                case menuPalette:
                case menuBox:
                case menuNodeText:
                case Foundry:
                case layerGds:
                case LayerRule:
                case LayersRule:
                case NodeLayersRule:
                case NodeRule:
                    break;
                default:
                    assert false;
            }
        }

        private void fixNodeBase() {
            if (curNodeGroupHasNodeBase) return;
            double lx, hx, ly, hy;
            if (curNodeGroup.nodeSizeRule != null) {
                hx = 0.5*curNodeGroup.nodeSizeRule.width;
                lx = -hx;
                hy = 0.5*curNodeGroup.nodeSizeRule.height;
                ly = -hy;
            } else {
                lx = Double.POSITIVE_INFINITY;
                hx = Double.NEGATIVE_INFINITY;
                ly = Double.POSITIVE_INFINITY;
                hy = Double.NEGATIVE_INFINITY;
                for (int i = 0; i < curNodeGroup.nodeLayers.size(); i++) {
                    Xml.NodeLayer nl = curNodeGroup.nodeLayers.get(i);
                    double x, y;
                    if (nl.representation == com.sun.electric.technology.Technology.NodeLayer.BOX || nl.representation == com.sun.electric.technology.Technology.NodeLayer.MULTICUTBOX) {
                        x = nl.lx.value;
                        lx = Math.min(lx, x);
                        hx = Math.max(hx, x);
                        x = nl.hx.value;
                        lx = Math.min(lx, x);
                        hx = Math.max(hx, x);
                        y = nl.ly.value;
                        ly = Math.min(ly, y);
                        hy = Math.max(hy, y);
                        y = nl.hy.value;
                        ly = Math.min(ly, y);
                        hy = Math.max(hy, y);
                    } else {
                        for (com.sun.electric.technology.Technology.TechPoint p: nl.techPoints) {
                            x = p.getX().getAdder();
                            lx = Math.min(lx, x);
                            hx = Math.max(hx, x);
                            y = p.getY().getAdder();
                            ly = Math.min(ly, y);
                            hy = Math.max(hy, y);
                        }
                    }
                }
            }
            curNodeGroup.baseLX.value = DBMath.round(lx + curNodeGroup.baseLX.value);
            curNodeGroup.baseHX.value = DBMath.round(hx + curNodeGroup.baseHX.value);
            curNodeGroup.baseLY.value = DBMath.round(ly + curNodeGroup.baseLY.value);
            curNodeGroup.baseHY.value = DBMath.round(hy + curNodeGroup.baseHY.value);
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
                    nonBlank = nonBlank || c != ' ' && c != '\n' && c != '\t';
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
//            int x = 0;
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
//            int x = 0;
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
//            int x = 0;
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
            System.out.println("warning publicId=" + e.getPublicId() + " systemId=" + e.getSystemId() +
                    " line=" + e.getLineNumber() + " column=" + e.getColumnNumber() + " message=" + e.getMessage() + " exception=" + e.getException());
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
//            System.out.println("error publicId=" + e.getPublicId() + " systemId=" + e.getSystemId() +
//                    " line=" + e.getLineNumber() + " column=" + e.getColumnNumber() + " message=" + e.getMessage() + " exception=" + e.getException());
            throw e;
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
//            System.out.println("fatal error publicId=" + e.getPublicId() + " systemId=" + e.getSystemId() +
//                    " line=" + e.getLineNumber() + " column=" + e.getColumnNumber() + " message=" + e.getMessage() + " exception=" + e.getException());
            throw e;
        }
    }

    private static class Writer {
        private static final int INDENT_WIDTH = 4;
        protected final PrintWriter out;
        private int indent;
        protected boolean indentEmitted;

        private Writer(PrintWriter out) {
            this.out = out;
        }

        private void writeTechnology(Xml.Technology t, boolean includeDateAndVersion, String copyrightMessage) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(new Date());

            header();

            pl("");
            out.println("<!--");
            pl(" *");
    		if (includeDateAndVersion)
    		{
    			pl(" * Electric(tm) VLSI Design System, version " + com.sun.electric.database.text.Version.getVersion());
    		} else
    		{
    			pl(" * Electric(tm) VLSI Design System");
    		}
            pl(" *");
            pl(" * File: " + t.techName + ".xml");
            pl(" * " + t.techName + " technology description");
            pl(" * Generated automatically from a library");
            pl(" *");
    		if (copyrightMessage != null)
    		{
	    		int start = 0;
	    		while (start < copyrightMessage.length())
	    		{
	    			int endPos = copyrightMessage.indexOf('\n', start);
	    			if (endPos < 0) endPos = copyrightMessage.length();
	    			String oneLine = copyrightMessage.substring(start, endPos);
	                pl(" * " + oneLine);
	    			start = endPos+1;
	    		}
    		}
            out.println("-->");
            l();

            b(XmlKeyword.technology); a("name", t.techName); a("class", t.className); l();
            a("xmlns", "http://electric.sun.com/Technology"); l();
            a("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance"); l();
            a("xsi:schemaLocation", "http://electric.sun.com/Technology ../../technology/Technology.xsd"); cl();
            l();

            bcpel(XmlKeyword.shortName, t.shortTechName);
            bcpel(XmlKeyword.description, t.description);
            for (Version version: t.versions) {
                b(XmlKeyword.version); a("tech", version.techVersion); a("electric", version.electricVersion); el();
            }
            b(XmlKeyword.numMetals); a("min", t.minNumMetals); a("max", t.maxNumMetals); a("default", t.defaultNumMetals); el();
            b(XmlKeyword.scale); a("value", t.scaleValue); a("relevant", Boolean.valueOf(t.scaleRelevant)); el();
            b(XmlKeyword.resolution); a("value", t.resolutionValue); el();
            b(XmlKeyword.defaultFoundry); a("value", t.defaultFoundry); el();
            b(XmlKeyword.minResistance); a("value", t.minResistance); el();
            b(XmlKeyword.minCapacitance); a("value", t.minCapacitance); el();
            if (t.leGateCapacitance != DEFAULT_LE_GATECAP || t.leWireRatio != DEFAULT_LE_WIRERATIO || t.leDiffAlpha != DEFAULT_LE_DIFFALPHA) {
                b(XmlKeyword.logicalEffort);
                a("gateCapacitance", t.leGateCapacitance);
                a("wireRatio", t.leWireRatio);
                a("diffAlpha", t.leDiffAlpha);
                el();
            }
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
            for (Xml.PrimitiveNodeGroup nodeGroup: t.nodeGroups) {
                writeXml(nodeGroup);
                l();
            }

            for (Xml.SpiceHeader spiceHeader: t.spiceHeaders)
                writeSpiceHeaderXml(spiceHeader);

            writeMenuPaletteXml(t.menuPalette);

            for (Xml.Foundry foundry: t.foundries)
                writeFoundryXml(foundry);

            el(XmlKeyword.technology);
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

            bcpel(XmlKeyword.patternedOnDisplay, Boolean.valueOf(desc.isPatternedOnDisplay()));
            bcpel(XmlKeyword.patternedOnPrinter, Boolean.valueOf(desc.isPatternedOnPrinter()));

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
            bcpel(XmlKeyword.foreground, Boolean.valueOf(desc.getForeground()));

            // write the 3D information
            b(XmlKeyword.display3D);
            a("thick", li.thick3D); a("height", li.height3D);
            a("mode", li.desc.getTransparencyMode());
           	a("factor", li.desc.getTransparencyFactor());
            el();

            if (li.cif != null && li.cif.length() > 0) {
                b(XmlKeyword.cifLayer); a("cif", li.cif); el();
            }
            if (li.skill != null && li.skill.length() > 0) {
                b(XmlKeyword.skillLayer); a("skill", li.skill); el();
            }

            // write the SPICE information
            if (li.resistance != 0 || li.capacitance != 0 || li.edgeCapacitance != 0) {
                b(XmlKeyword.parasitics); a("resistance", li.resistance); a("capacitance", li.capacitance); a("edgeCapacitance", li.edgeCapacitance); el();
            }
            if (li.pureLayerNode != null) {
                String nodeName = li.pureLayerNode.name;
                Poly.Type style = li.pureLayerNode.style;
                String styleStr = style == Poly.Type.FILLED ? null : style.name();
                String portName = li.pureLayerNode.port;
                b(XmlKeyword.pureLayerNode); a("name", nodeName); a("style", styleStr); a("port", portName); cl();
                bcpel(XmlKeyword.oldName, li.pureLayerNode.oldName);
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
            bcpel(XmlKeyword.oldName, ai.oldName);

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
            bcpel(XmlKeyword.extended, Boolean.valueOf(ai.extended));
            bcpel(XmlKeyword.fixedAngle, Boolean.valueOf(ai.fixedAngle));
            bcpel(XmlKeyword.angleIncrement, ai.angleIncrement);
            if (ai.antennaRatio != 0)
                bcpel(XmlKeyword.antennaRatio, ai.antennaRatio);

            for (Map.Entry<Integer,Double> e: ai.diskOffset.entrySet()) {
                b(XmlKeyword.diskOffset); a("untilVersion", e.getKey()); a("width", e.getValue()); el();
            }

            if (ai.defaultWidth.value != 0) {
                bcl(XmlKeyword.defaultWidth);
                bcpel(XmlKeyword.lambda, ai.defaultWidth.value);
                el(XmlKeyword.defaultWidth);
            }

            for (Xml.ArcLayer al: ai.arcLayers) {
                String style = al.style == Poly.Type.FILLED ? "FILLED" : "CLOSED";
                b(XmlKeyword.arcLayer); a("layer", al.layer); a("style", style);
                double extend = al.extend.value;
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

        private void writeXml(Xml.PrimitiveNodeGroup ng) {
            if (ng.isSingleton) {
                PrimitiveNode n = ng.nodes.get(0);
                b(XmlKeyword.primitiveNode); a("name", n.name); a("fun", n.function.name()); cl();
                bcpel(XmlKeyword.oldName, n.oldName);
            } else {
                bcl(XmlKeyword.primitiveNodeGroup);
                for (PrimitiveNode n: ng.nodes) {
                    b(XmlKeyword.primitiveNode); a("name", n.name); a("fun", n.function.name());
                    if (n.oldName != null || n.highVt || n.lowVt || n.nativeBit || n.od18 || n.od25 || n.od33) {
                        cl();
                        bcpel(XmlKeyword.oldName, n.oldName);
                        if (n.lowVt)
                            bel(XmlKeyword.lowVt);
                        if (n.highVt)
                            bel(XmlKeyword.highVt);
                        if (n.nativeBit)
                            bel(XmlKeyword.nativeBit);
                        if (n.od18)
                            bel(XmlKeyword.od18);
                        if (n.od25)
                            bel(XmlKeyword.od25);
                        if (n.od33)
                            bel(XmlKeyword.od33);
                        el(XmlKeyword.primitiveNode);
                    } else {
                        el();
                    }
                }
            }

            if (ng.shrinkArcs)
                bel(XmlKeyword.shrinkArcs);
            if (ng.square)
                bel(XmlKeyword.square);
            if (ng.canBeZeroSize)
                bel(XmlKeyword.canBeZeroSize);
            if (ng.wipes)
                bel(XmlKeyword.wipes);
            if (ng.lockable)
                bel(XmlKeyword.lockable);
            if (ng.edgeSelect)
                bel(XmlKeyword.edgeSelect);
            if (ng.skipSizeInPalette)
                bel(XmlKeyword.skipSizeInPalette);
            if (ng.notUsed)
                bel(XmlKeyword.notUsed);
            if (ng.isSingleton) {
                PrimitiveNode n = ng.nodes.get(0);
                if (n.lowVt)
                    bel(XmlKeyword.lowVt);
                if (n.highVt)
                    bel(XmlKeyword.highVt);
                if (n.nativeBit)
                    bel(XmlKeyword.nativeBit);
                if (n.od18)
                    bel(XmlKeyword.od18);
                if (n.od25)
                    bel(XmlKeyword.od25);
                if (n.od33)
                    bel(XmlKeyword.od33);
            }

            for (Map.Entry<Integer,EPoint> e: ng.diskOffset.entrySet()) {
                EPoint p = e.getValue();
                b(XmlKeyword.diskOffset); a("untilVersion", e.getKey()); a("x", p.getLambdaX()); a("y", p.getLambdaY()); el();
            }

            if (ng.defaultWidth.value != 0) {
                bcl(XmlKeyword.defaultWidth);
                bcpel(XmlKeyword.lambda, ng.defaultWidth.value);
                el(XmlKeyword.defaultWidth);
            }

            if (ng.defaultHeight.value != 0) {
                bcl(XmlKeyword.defaultHeight);
                bcpel(XmlKeyword.lambda, ng.defaultHeight.value);
                el(XmlKeyword.defaultHeight);
            }

            bcl(XmlKeyword.nodeBase);
            bcl(XmlKeyword.box);
            b(XmlKeyword.lambdaBox); a("klx", ng.baseLX.value); a("khx", ng.baseHX.value); a("kly", ng.baseLY.value); a("khy", ng.baseHY.value); el();
            el(XmlKeyword.box);
            el(XmlKeyword.nodeBase);

            if (ng.protection != null) {
                b(XmlKeyword.protection); a("location", ng.protection); el();
            }

            for(int j=0; j<ng.nodeLayers.size(); j++) {
                Xml.NodeLayer nl = ng.nodeLayers.get(j);
                b(XmlKeyword.nodeLayer); a("layer", nl.layer); a("style", nl.style.name());
                if (nl.portNum != 0) a("portNum", Integer.valueOf(nl.portNum));
                if (!(nl.inLayers && nl.inElectricalLayers))
                    a("electrical", Boolean.valueOf(nl.inElectricalLayers));
                cl();
                if (nl.inNodes != null) {
                    assert !ng.isSingleton;
                    bcl(XmlKeyword.inNodes);
                    for (int i = 0; i < ng.nodes.size(); i++) {
                        if (nl.inNodes.get(i)) {
                            b(XmlKeyword.primitiveNode); a("name", ng.nodes.get(i).name); el();
                        }
                    }
                    el(XmlKeyword.inNodes);
                }
                switch (nl.representation) {
                    case com.sun.electric.technology.Technology.NodeLayer.BOX:
                        if (ng.specialType == com.sun.electric.technology.PrimitiveNode.SERPTRANS) {
                            writeBox(XmlKeyword.serpbox, nl.lx, nl.hx, nl.ly, nl.hy);
                            a("lWidth", nl.lWidth); a("rWidth", nl.rWidth); a("tExtent", nl.tExtent); a("bExtent", nl.bExtent); cl();
                            b(XmlKeyword.lambdaBox); a("klx", nl.lx.value); a("khx", nl.hx.value); a("kly", nl.ly.value); a("khy", nl.hy.value); el();
                            el(XmlKeyword.serpbox);
                        } else {
                            writeBox(XmlKeyword.box, nl.lx, nl.hx, nl.ly, nl.hy); cl();
                            b(XmlKeyword.lambdaBox); a("klx", nl.lx.value); a("khx", nl.hx.value); a("kly", nl.ly.value); a("khy", nl.hy.value); el();
                            el(XmlKeyword.box);
                        }
                        break;
                    case com.sun.electric.technology.Technology.NodeLayer.POINTS:
                        b(XmlKeyword.points); el();
                        break;
                    case com.sun.electric.technology.Technology.NodeLayer.MULTICUTBOX:
                        writeBox(XmlKeyword.multicutbox, nl.lx, nl.hx, nl.ly, nl.hy);
                        a("sizex", nl.sizex); a("sizey", nl.sizey); a("sep1d", nl.sep1d);  a("sep2d", nl.sep2d); cl();
                        b(XmlKeyword.lambdaBox); a("klx", nl.lx.value); a("khx", nl.hx.value); a("kly", nl.ly.value); a("khy", nl.hy.value); el();
                        el(XmlKeyword.multicutbox);
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
            for (int j = 0; j < ng.ports.size(); j++) {
                Xml.PrimitivePort pd = ng.ports.get(j);
                b(XmlKeyword.primitivePort); a("name", pd.name); cl();
                b(XmlKeyword.portAngle); a("primary", pd.portAngle); a("range", pd.portRange); el();
                bcpel(XmlKeyword.portTopology, pd.portTopology);

                writeBox(XmlKeyword.box, pd.lx, pd.hx, pd.ly, pd.hy); cl();
                b(XmlKeyword.lambdaBox); a("klx", pd.lx.value); a("khx", pd.hx.value); a("kly", pd.ly.value); a("khy", pd.hy.value); el();
                el(XmlKeyword.box);

                for (String portArc: pd.portArcs)
                    bcpel(XmlKeyword.portArc, portArc);
                el(XmlKeyword.primitivePort);
            }
            switch (ng.specialType) {
                case com.sun.electric.technology.PrimitiveNode.POLYGONAL:
                    bel(XmlKeyword.polygonal);
                    break;
                case com.sun.electric.technology.PrimitiveNode.SERPTRANS:
                    b(XmlKeyword.serpTrans); cl();
                    for (int i = 0; i < 6; i++) {
                        bcpel(XmlKeyword.specialValue, ng.specialValues[i]);
                    }
                    el(XmlKeyword.serpTrans);
                    break;
            }
            if (ng.nodeSizeRule != null) {
                NodeSizeRule r = ng.nodeSizeRule;
                b(XmlKeyword.minSizeRule); a("width", r.width); a("height", r.height); a("rule", r.rule); el();
            }
            if (ng.spiceTemplate != null)
            {
                b(XmlKeyword.spiceTemplate); a("value", ng.spiceTemplate); el();
            }

            el(ng.isSingleton ? XmlKeyword.primitiveNode : XmlKeyword.primitiveNodeGroup);
        }

        private void writeBox(XmlKeyword keyword, Distance lx, Distance hx, Distance ly, Distance hy) {
            b(keyword);
            if (lx.k != -1) a("klx", lx.k);
            if (hx.k != 1) a("khx", hx.k);
            if (ly.k != -1) a("kly", ly.k);
            if (hy.k != 1) a("khy", hy.k);
        }

        private void writeSpiceHeaderXml(Xml.SpiceHeader spiceHeader) {
            b(XmlKeyword.spiceHeader); a("level", spiceHeader.level); cl();
            for (String line: spiceHeader.spiceLines) {
                b(XmlKeyword.spiceLine); a("line", line); el();
            }
            el(XmlKeyword.spiceHeader);
            l();
        }

        public void writeMenuPaletteXml(Xml.MenuPalette menuPalette) {
            if (menuPalette == null) return;
            b(XmlKeyword.menuPalette); a("numColumns", menuPalette.numColumns); cl();
            for (int i = 0; i < menuPalette.menuBoxes.size(); i++) {
                if (i % menuPalette.numColumns == 0)
                    l();
                writeMenuBoxXml(menuPalette.menuBoxes.get(i));
            }
            l();
            el(XmlKeyword.menuPalette);
            l();
        }

        private void writeMenuBoxXml(List<?> list) {
            b(XmlKeyword.menuBox);
            if (list == null || list.size() == 0) {
                el();
                return;
            }
            cl();
            for (Object o: list) {
                if (o instanceof Xml.ArcProto) {
                    bcpel(XmlKeyword.menuArc, ((Xml.ArcProto)o).name);
                } else if (o instanceof Xml.PrimitiveNode) {
                    bcpel(XmlKeyword.menuNode, ((Xml.PrimitiveNode)o).name);
                } else if (o instanceof Xml.MenuNodeInst) {
                    Xml.MenuNodeInst ni = (Xml.MenuNodeInst)o;
                    b(XmlKeyword.menuNodeInst); a("protoName", ni.protoName); a("function", ni.function.name());
                    if (ni.techBits != 0) a("techBits", ni.techBits);
                    if (ni.rotation != 0) a("rotation", ni.rotation);
                    if (ni.text == null) {
                        el();
                    } else {
                        cl();
                        b(XmlKeyword.menuNodeText); a("text", ni.text); /*a("size", ni.fontSize);*/ el();
                        el(XmlKeyword.menuNodeInst);
                    }
                } else {
                	if (o == null) bel(XmlKeyword.menuText); else
                		bcpel(XmlKeyword.menuText, o);
                }
            }
            el(XmlKeyword.menuBox);
        }

        private void writeFoundryXml(Xml.Foundry foundry) {
            b(XmlKeyword.Foundry); a("name", foundry.name); cl();
            l();
            for (Map.Entry<String,String> e: foundry.layerGds.entrySet()) {
                b(XmlKeyword.layerGds); a("layer", e.getKey()); a("gds", e.getValue()); el();
            }
            l();
            for (DRCTemplate rule: foundry.rules)
                DRCTemplate.exportDRCRule(out, rule);
            el(XmlKeyword.Foundry);
        }

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
        private void a(String name, double value) { a(name, new Double(value)); }
        private void a(String name, int value) { a(name, new Integer(value)); }

        private void bcpel(XmlKeyword key, Object v) {
            if (v == null) return;
            b(key); c(); p(v.toString()); el(key);
        }
        private void bcpel(XmlKeyword key, int v) { bcpel(key, new Integer(v)); }
        private void bcpel(XmlKeyword key, double v) { bcpel(key, new Double(v)); }

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
        protected void p(String s) {
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

        protected void checkIndent() {
            if (indentEmitted) return;
            for (int i = 0; i < indent; i++)
                out.print(' ');
            indentEmitted = true;
        }

        /**
         *  Print new line.
         */
        protected void l() {
            out.println();
            indentEmitted = false;
        }
    }

    /**
     * Class to write the XML without multiple lines and indentation.
     * Useful when the XML is to be a single string.
     */
	private static class OneLineWriter extends Writer
	{
		private OneLineWriter(PrintWriter out)
		{
			super(out);
		}

		/**
		 * Print text without replacement of special chars.
		 */
        @Override
		protected void p(String s)
		{
			for (int i = 0; i < s.length(); i++)
				out.print(s.charAt(i));
		}

        @Override
		protected void checkIndent() { indentEmitted = true; }

		/**
		 * Do not print new line.
		 */
        @Override
		protected void l() { indentEmitted = false; }
	}
}
