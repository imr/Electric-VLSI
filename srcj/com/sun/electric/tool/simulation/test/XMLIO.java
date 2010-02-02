/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: XMLIO.java
 * Written by Eric Kim, Sun Microsystems.
 *
 * Copyright (c) 2004 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.simulation.test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * XML input helper class, used by ChainModel to read in the scan chain XML
 * file. For more information on the XML file, see <code>ChainG.dtd</code> and
 * ``The Scan Chain XML File Format'' by Tom O'Neill.
 */
class XMLIO {

    /**
     * Character string that identifies readable chain elements when it occurs
     * in the access attribute
     */
    public final static String READ_ACCESS_STRING = "R";

    /**
     * Character string that identifies readable chain elements when it occurs
     * in the access attribute
     */
    public final static String WRITE_ACCESS_STRING = "W";

    /**
     * Character string that identifies chain elements that are neither readable
     * nor writeable when it occurs in the access attribute.
     */
    public final static String NO_ACCESS_STRING = "-";

    /**
     * Character string that identifies chain elements that read and write to
     * and from the same shadow register. If the element is master clearable,
     * master clear writes to this register. No other circuits write to this
     * register.
     */
    public final static String SHADOW_ACCESS_STRING = "S";

    /**
     * Character string that identifies chain elements with unknown read/write
     * access
     */
    public final static String UNKNOWN_ACCESS_STRING = "?";

    /**
     * Character string that identifies chain elements whose scan out bits
     * cannot be predicted.
     */
    public final static String UNPREDICTABLE_ACCESS_STRING = "U";

    /**
     * Character string that indentifies chain elements that write to
     * a shadow register, but read from a different location. If the
     * element is master clearable, master clear writes to the shadow
     * register. No other circuits write to this register.
     */
    public final static String DUAL_PORTED_SHADOW_ACCESS_STRING = "D";

    /** Character string that identifies chain elements that clear HI */
    public final static String CLEARS_HI_STRING = "H";

    /** Character string that identifies chain elements that clear LO */
    public final static String CLEARS_LO_STRING = "L";

    /** Character string that identifies chain elements that clear NOT */
    public final static String CLEARS_NOT_STRING = "-";

    /** Character string that identifies chain elements that clear NOT */
    public final static String CLEARS_UNKNOWN_STRING = "?";

    protected final static String SCAN_CHAIN_DATA_NETS = "scanChainDataNets";

    public XMLIO() {
    }

    /**
     * Read a scan chain xml file, creating a TestNode tree representing the
     * scan chain. Return the system node at the root of the tree.
     * 
     * @return the TestNode object created to represent the <tt>system</tt>
     *         tag
     */
    public static TestNode read(String filename) throws IOException,
            SAXException, ParserConfigurationException, Exception {

        // Make a Document Object Model (DOM) of the XML file. The dbf
        // is used to obtain a parser db, which is then used to create the
        // DOM document object.
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setValidating(true);
        DocumentBuilder db = null;
        try {
            db = dbf.newDocumentBuilder();
        } catch (ParserConfigurationException pce) {
            pce.printStackTrace();
        }
        DefaultHandler er = new XMLEntityResolver();
        db.setEntityResolver(er);
        db.setErrorHandler(er);
        Document doc = db.parse(filename);


        // Extract scan chain tree from DOM. Start at system tag, then
        // descend.
        Element root = doc.getDocumentElement();
        Element element = (Element) root.getElementsByTagName("system").item(0);
        TestNode system = readSystem(element, null);

        Logger.logInit("Read XML file " + filename + ", system=" + system);

        return system;
    }

    private static class XMLEntityResolver extends DefaultHandler
    {
        public InputSource resolveEntity (String publicId,
					       String systemId) throws SAXException
        {
            InputStream inputStream  = null;

            try
            {
                URL fileURL = this.getClass().getResource("ChainG.dtd");
                URLConnection urlCon = fileURL.openConnection();
                inputStream = urlCon.getInputStream();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            return new InputSource(inputStream);
        }

        public void fatalError(SAXParseException e)
        {
            System.out.println("Parser Fatal Error on line "+e.getLineNumber()+
                    ", column "+e.getColumnNumber()+
                    ". Check Validation rules in ChainG.dtd");
            e.printStackTrace();
            System.exit(1);
        }

        public void warning(SAXParseException e)
        {
            System.out.println("Parser Warning");
            e.printStackTrace();
        }

        public void error(SAXParseException e)
        {
            System.out.println("Parser Error on line "+e.getLineNumber()+
                    ", column "+e.getColumnNumber()+
                    ". Check Validation rules in ChainG.dtd");
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Create the TestNode (scan chain hierarchy) object corresponding to the
     * specified element in the DOM tree. Adds it as a child to the parent
     * TestNode object.
     * 
     * Recursively traverses the Document Obeject Model tree starting at
     * specified element and constructs our own TestNode representation of the
     * scan chain hieararchy.
     * 
     * @param element
     *            A Document Element in the XML file's DOM tree
     * @param parentNode
     *            Parent node in TestNode hierarchy
     * @return The TestNode object created to represent the element
     */
    private static TestNode readSystem(Element element, TestNode parentNode)
            throws Exception {

        // Create the TestNode object, if any, corresponding to the element.
        // Add the new node to its parent
        TestNode newNode = createTreeNode(element, parentNode);
        if (newNode == null) {
            return newNode;
        }
        if (parentNode != null)
            parentNode.addChild(newNode);

        // Add all of the children for this node to the TestNode
        // hierarchy. Note they will add their own children, and the
        // current node will be added when it returns to the calling
        // instance of readSystem(). Mmmmm....recursion.
        NodeList children = element.getChildNodes();
        for (int ind = 0; ind < children.getLength(); ind++) {
            Node childElement = children.item(ind);

            // If candidate element is an element node (insted of, e.g.,
            // a text node), add it and its children to parentNode.
            if (childElement.getNodeType() == Node.ELEMENT_NODE) {
                readSystem((Element) childElement, newNode);
            }
        }

        return newNode;
    }

    /**
     * If appropriate, create a new node in the <code>TestNode</code>
     * hierarchy representing element e in the Document Obeject Model of the
     * scan chain XML file.
     * 
     * @param element
     *            A Document Element in the XML file's DOM tree
     * @param parentNode
     *            Parent node in TestNode hierarchy
     * @return The TestNode object, if any, created to represent element e
     */
    protected static TestNode createTreeNode(Element element, TestNode parentNode)
            throws Exception {
        String name; //node name
        int length; //number of scan chain elements in node
        String comment; //optional comment for node

        // If element is null, no object to create
        if (element == null) {
            System.err.println("Warning null element");
            return null;
        }

        TestNode newNode = null; //Return value; no new node identified yet

        // Create new node for the TestNode object tree, based on the XML tag
        // name and parameters
        String tagName = element.getNodeName();
        if (tagName.equals("system")) {
            comment = getField(element, "comment");
            if (comment == null || comment.length() < 1)
                comment = "System Top Level";
            newNode = new TestNode("System", comment);
        } else if (tagName.equals("chip")) {
            name = element.getAttribute("name").trim();
            int lenIR = Integer.parseInt(element.getAttribute("lengthIR"));
            comment = getField(element, "comment");
            newNode = new ChipNode(name, lenIR, comment);
        } else if (tagName.equals("chain")) {
            newNode = processChain(element);
        } else if (tagName.equals("subchain")) {
            name = element.getAttribute("name").trim();
            String pin = element.getAttribute("pin");
            length = parseLength(element);
            comment = getField(element, "comment");
            String dataNet = element.getAttribute("dataNet");
            String dataNetBar = element.getAttribute("dataNet2");
            newNode = new SubchainNode(name, length, pin, comment, dataNet, dataNetBar);
        } else if (tagName.equals("duplicatechain")) {
            newNode = processDuplicateChain(element, parentNode);
        } else if (tagName.equals("forloop")) {
            newNode = processForLoop(element, parentNode);
        } else if (tagName.equals("comment")) {
            newNode = null;
        } else if (tagName.equals("scandatanets")) {
            comment = getField(element, "comment");
            newNode = new TestNode(SCAN_CHAIN_DATA_NETS, comment);
        } else if (tagName.equals("datachain")) {
            newNode = processChain(element);
        } else if (tagName.equals("datanet")) {
            comment = getField(element, "comment");
            name = element.getAttribute("name").trim();
            String dataNet = element.getAttribute("net");
            String dataNet2 = element.getAttribute("net2");
            newNode = new SubchainNode(name, 1, "", comment, dataNet, dataNet2);
        } else {
            Infrastructure.fatal("Unrecognized tag name " + tagName
                    + " in XML file\nparent node: " + parentNode);
        }

        if (newNode != null) {
            setAccess(element, parentNode, newNode);
            setClearBehavior(element, parentNode, newNode);
        }

        return newNode;
    }

    /** Creates <code>ChainNode</code> object corresponding to the element */
    private static TestNode processChain(Element element) {
        String name = element.getAttribute("name").trim();
        String opcode = element.getAttribute("opcode").trim();
        int length = parseLength(element);
        String comment = getField(element, "comment");

        return new ChainNode(name, opcode, length, comment);
    }

    private static TestNode processDuplicateChain(Element element, TestNode parentNode) {
        String name = element.getAttribute("name").trim();
        String opcode = element.getAttribute("opcode").trim();
        String reference = element.getAttribute("sameas").trim();
        TestNode pp = parentNode;
        while (!(parentNode instanceof ChipNode)) {
            parentNode = (TestNode)parentNode.getParent();
        }
        for (int i=0; i<parentNode.getChildCount(); i++) {
            MyTreeNode node = parentNode.getChildAt(i);
            if (node instanceof ChainNode) {
                if (node.getName().equals(reference)) {
                    return new ChainNodeDuplicate(name, opcode, (ChainNode)node);
                }
            }
        }
        Infrastructure.fatal("Could not find reference chain " + reference
                    + " in XML file\nparent node: " + pp + " for duplicate chain: "+name);
        return null;
    }

    /**
     * Adds children of a forloop element multiple times, directly to the
     * parentNode TestNode object. Note that forloop nodes do not appear in the
     * TestNode hierarchy, so this routine must return null.
     * 
     * @param element
     *            A Document Element in the XML file's DOM tree
     * @param parentNode
     *            Parent node in TestNode hierarchy
     * @return null (for loop node disappears in TestNode hierarchy)
     */
    private static TestNode processForLoop(Element element, TestNode parentNode)
            throws Exception {

        int start = Integer.parseInt(element.getAttribute("initial"));
        int end = Integer.parseInt(element.getAttribute("final"));
        int increment = Integer.parseInt(element.getAttribute("increment"));

        if (end >= start && increment > 0) {
            for (int ind = start; ind <= end; ind += increment) {
                addForLoopChildren(element, parentNode, ind);
            }
        } else if (end <= start && increment < 0) {
            for (int ind = start; ind >= end; ind += increment) {
                addForLoopChildren(element, parentNode, ind);
            }
        } else {
            Infrastructure.fatal("Infinite <forloop> tag specified."
                    + " Bad tag, bad!\n  parent='" + parentNode
                    + "'\n  element='" + element + "'");
        }

        return null;
    }

    /**
     * Add the children of the a forloop tag, during a single iteration of the
     * loop. The children are added directly to <code>parentNode</code> (no
     * forloop node is created), and will add their own children.
     * 
     * @param element
     *            The forloop Document Element in the XML file's DOM tree
     * @param parentNode
     *            The forloop's parent node
     * @param ind
     *            The loop index to append to the child nodes' names
     * @throws Exception
     */
    private static void addForLoopChildren(Element element,
            TestNode parentNode, int ind) throws Exception {
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node childElement = children.item(i);
            if (childElement.getNodeType() != Node.ELEMENT_NODE)
                continue;
            TestNode childNode = readSystem((Element) childElement, parentNode);
            if (childNode == null)
                continue;

            //Append index value to name of child tree node
            String name = childNode.getName() + Integer.toString(ind);
            childNode.setName(name);
        }
    }

    /**
     * Extracts length from current element
     * 
     * @return integer parsed from attribute length or -1 on error
     */
    private static int parseLength(Element e) {
        int length = -1;
        try {
            String lengthS = e.getAttribute("length");
            length = Integer.parseInt(lengthS);
        } catch (Exception ignore) {
        }
        return length;
    }

    /**
     * Return the text from the specified element. Non recursive. Find only one
     * level. Assume element has one field element, one text
     */
    private static String getField(Node n, String field) {
        NodeList children = n.getChildNodes();
        boolean found = false;
        String ret = null;

        for (int i = 0; i < children.getLength(); i++) {
            Node childNode = children.item(i);
            if (childNode.getNodeType() == Node.ELEMENT_NODE
                    && childNode.getNodeName().equals(field)) {
                if (found) {
                    Infrastructure.fatal("getField: node '" + n
                            + "' has more than one child named " + field);
                } else {
                    found = true;
                    ret = childNode.getFirstChild().getNodeValue();
                }
            }
        }

        return ret;
    }

    /**
     * Sets the readable, writeable characteristics of newNode according to the
     * "access" attribute in the corresponding element of the DOM. If unknown
     * access is specified, access is set to RW (this is the most conservative
     * selection, because it disables tests that depend on the access
     * attribute). If the access attribute is not provided, default to access of
     * parentNode.
     * 
     * @param element
     * @param parentNode
     * @param newNode
     */
    private static void setAccess(Element element, TestNode parentNode,
            TestNode newNode) {
        boolean unpredictable, readable, writeable, usesShadow, usesDualPortedShadow;
        String access = element.getAttribute("access").trim().toUpperCase();

        // If the access was not specified, then use the parent's
        // accessability as the default.
        if (access.length() == 0) {
            unpredictable = parentNode.isUnpredictable();
            readable = parentNode.isReadable();
            writeable = parentNode.isWriteable();
            usesShadow = parentNode.usesShadow();
            usesDualPortedShadow = parentNode.usesDualPortedShadow();
        } else if (access.equals(UNKNOWN_ACCESS_STRING)) {
            readable = writeable = true;
            unpredictable = usesShadow = usesDualPortedShadow = false;
        } else if (access.equals(NO_ACCESS_STRING)) {
            unpredictable = readable = writeable = usesShadow = usesDualPortedShadow = false;
        } else if (access.equals(UNPREDICTABLE_ACCESS_STRING)) {
            unpredictable = true;
            readable = writeable = usesShadow = usesDualPortedShadow = false;
        } else {
            int goodLength = 0;

            unpredictable = false;
            readable = access.indexOf(READ_ACCESS_STRING) >= 0;
            if (readable) {
                goodLength += READ_ACCESS_STRING.length();
            }
            writeable = access.indexOf(WRITE_ACCESS_STRING) >= 0;
            if (writeable) {
                goodLength += WRITE_ACCESS_STRING.length();
            }
            usesShadow = access.indexOf(SHADOW_ACCESS_STRING) >= 0;
            if (usesShadow) {
                goodLength += SHADOW_ACCESS_STRING.length();
            }
            usesDualPortedShadow = access.indexOf(DUAL_PORTED_SHADOW_ACCESS_STRING) >= 0;
            if (usesDualPortedShadow) {
                goodLength += DUAL_PORTED_SHADOW_ACCESS_STRING.length();
            }

            if (usesShadow && usesDualPortedShadow) {
                Infrastructure.fatal("Bad access string '"
                        + element.getAttribute("access") + "' in XML file."
                        + "\nCannot have S and D specified at the same time."
                        + "\nerror node: '" + newNode
                        + "'\nparent node: '" + parentNode + "'");
            }

            // READ_ACCESS_STRING and WRITE_ACCESS string may appear
            // together, but not with any other characters.
            if (access.length() != goodLength) {
                Infrastructure.fatal("Bad access string '"
                        + element.getAttribute("access") + "' in XML file."
                        + "\nAllowed values are "
                        + "?, U, -, R, W, RW, RWS, or RWD.\nerror node: '" + newNode
                        + "'\nparent node: '" + parentNode + "'");
            }
        }

        newNode.setUnpredictable(unpredictable);
        newNode.setReadable(readable);
        newNode.setWriteable(writeable);
        newNode.setUsesShadow(usesShadow);
        newNode.setUsesDualPortedShadow(usesDualPortedShadow);
    }

    /**
     * Sets the clearBehavior property of newNode according to the "clears"
     * attribute in the corresponding element of the DOM. If the clears
     * attribute is not provided, default to clearing behavior of parentNode.
     * 
     * @param element
     * @param parentNode
     * @param newNode
     */
    private static void setClearBehavior(Element element, TestNode parentNode,
            TestNode newNode) {
        int clearBehavior = TestNode.CLEARS_UNKNOWN;
        String clears = element.getAttribute("clears").trim().toUpperCase();

        // If the clears was not specified, then use the parent's
        // clear behavior as the default.
        if (clears.length() == 0) {
            clearBehavior = parentNode.getClearBehavior();
        } else if (clears.equals(CLEARS_LO_STRING)) {
            clearBehavior = TestNode.CLEARS_LO;
        } else if (clears.equals(CLEARS_HI_STRING)) {
            clearBehavior = TestNode.CLEARS_HI;
        } else if (clears.equals(CLEARS_NOT_STRING)) {
            clearBehavior = TestNode.CLEARS_NOT;
        } else if (clears.equals(CLEARS_UNKNOWN_STRING)) {
            clearBehavior = TestNode.CLEARS_UNKNOWN;
        } else {
            Infrastructure.fatal("Bad clears string '"
                    + element.getAttribute("clears") + "' in XML file."
                    + "\nAllowed values are ?, -, L, or H." + "\nerror node: "
                    + newNode + "\nparent node: " + parentNode);
        }

        newNode.setClearBehavior(clearBehavior);
    }
}
