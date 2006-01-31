package com.sun.electric.tool.io.output;

import com.sun.electric.database.geometry.Orientation;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Schematics;
import com.sun.electric.tool.io.IOTool;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads in a configuration file that specifies the equivalent Electric nodes for Nodes
 * found in (or to be written to) the EDIF file.  This allows mapping of Electric primitives
 * and Cells to primitives and cells in the target Tool which will read in the EDIF, or vice versa.
 * Most importantly, it specifies equivalences between ports on the two nodes which may be in
 * different locations.  Differing sizes of nodes does not matter.
 * <P>
 * This is currently only being used with Cadence Virtuoso Composer.
 */
public class EDIFEquiv {



    private HashMap<Object,NodeEquivalence> equivsByNodeProto;      // key: Electric hash (getElectricKey()), value: NodeEquivalence
    private HashMap<Object,NodeEquivalence> equivsByExternal;       // key: External hash (getExternalKey()), value: NodeEquivalence
    private HashMap exportEquivs;           // key: External hash (getExternalKey()), value: ExportEquivalence
    private HashMap<String,VariableEquivalence> exportByVariable;       // key: External hash (electric varname), value: VariableEquivalence
    private HashMap<String,VariableEquivalence> exportFromVariable;     // key: External hash (external varname), value: VariableEquivalence
    private HashMap<String,FigureGroupEquivalence> exportByFigureGroup;    // key: External hash (electric figureGroupName), value: FigureGroupEquivalence
    private HashMap<String,FigureGroupEquivalence> exportFromFigureGroup;  // key: External hash (external figureGroupName), value: FigureGroupEquivalence
    private HashMap<String,GlobalEquivalence> exportByGlobal;    		// key: External hash (electric figureGroupName), value: GlobalEquivalence
    private HashMap<String,GlobalEquivalence> exportFromGlobal;  		// key: External hash (external figureGroupName), value: GlobalEquivalence

    /**
     * Get the node equivalence for the NodeInst.  This must be a NodeInst, not a
     * NodeProto, because different transistor primitives share the same Primitive
     * prototype.
     * @param ni the NodeInst to look up
     * @return null if none found
     */
    public NodeEquivalence getNodeEquivalence(NodeInst ni) {
        NodeProto np = ni.getProto();
        PrimitiveNode.Function func = np.getFunction();
        PortCharacteristic exportType = null;
        if (!ni.isCellInstance()) {
            PrimitiveNode pn = (PrimitiveNode)np;
            func = pn.getTechnology().getPrimitiveFunction(pn, ni.getTechSpecific());
            // if this is an off page node and one of it's ports is exported, find out type
            if (np == Schematics.tech.offpageNode) {
                for (Iterator<PortProto> it = ni.getParent().getPorts(); it.hasNext(); ) {
                    Export e = (Export)it.next();
                    if (e.getOriginalPort().getNodeInst() == ni) {
                        exportType = e.getCharacteristic();
                        break;
                    }
                }
            }
        }
        return equivsByNodeProto.get(getElectricKey(np, func, exportType));
    }

	/**
	 * Method to get the VariableEquivalence that maps Electric variable names to external names.
	 * @param varName the Electric variable name.
	 * @return the VariableEquivalence that tells how to do the mapping (null if none).
	 */
	public VariableEquivalence getElectricVariableEquivalence(String varName)
	{
		VariableEquivalence ve = exportByVariable.get(varName);
		return ve;
	}

	/**
	 * Method to get the VariableEquivalence that maps external variable names to Electric names.
	 * @param varName the external variable name.
	 * @return the VariableEquivalence that tells how to do the mapping (null if none).
	 */
	public VariableEquivalence getExternalVariableEquivalence(String varName)
	{
		VariableEquivalence ve = exportFromVariable.get(varName);
		return ve;
	}

	/**
	 * Method to get the FigureGroupEquivalence that maps Electric figuregroup names to external names.
	 * @param fgName the Electric figuregroup name.
	 * @return the FigureGroupEquivalence that tells how to do the mapping (null if none).
	 */
	public FigureGroupEquivalence getElectricFigureGroupEquivalence(String fgName)
	{
		FigureGroupEquivalence fge = exportByFigureGroup.get(fgName);
		return fge;
	}

	/**
	 * Method to get the FigureGroupEquivalence that maps external figuregroup names to Electric names.
	 * @param fgName the external figuregroup name.
	 * @return the FigureGroupEquivalence that tells how to do the mapping (null if none).
	 */
	public FigureGroupEquivalence getExternalFigureGroupEquivalence(String fgName)
	{
		FigureGroupEquivalence fge = exportFromFigureGroup.get(fgName);
		return fge;
	}

	/**
	 * Method to get the GlobalEquivalence that maps Electric global names to external names.
	 * @param gName the Electric global name.
	 * @return the GlobalEquivalence that tells how to do the mapping (null if none).
	 */
	public GlobalEquivalence getElectricGlobalEquivalence(String gName)
	{
		GlobalEquivalence ge = exportByGlobal.get(gName);
		return ge;
	}

	/**
	 * Method to get the GlobalEquivalence that maps external global names to Electric names.
	 * @param gName the external global name.
	 * @return the GlobalEquivalence that tells how to do the mapping (null if none).
	 */
	public GlobalEquivalence getExternalGlobalEquivalence(String gName)
	{
		GlobalEquivalence ge = exportFromGlobal.get(gName);
		return ge;
	}

	/**
     * Get the node equivalence for the external reference.
     * @param extLib
     * @param extCell
     * @param extView
     * @return  null if none found
     */
    public NodeEquivalence getNodeEquivalence(String extLib, String extCell, String extView) {
        Object key = getExternalKey(extLib, extCell, extView);
        return equivsByExternal.get(key);
    }

    /**
     * Get a list of NodeEquivalences
     * @return  a list of NodeEquivalence objects
     */
    public List<NodeEquivalence> getNodeEquivs() {
        return new ArrayList<NodeEquivalence>(equivsByExternal.values());
    }

    /**
     * Translate a port location on an Electric node to a the equivalent port
     * location on the equivalent external node instance.
     * @param connPoint the electric connection point
     * @param pi the port inst
     * @return the connection point on the equivalent external node instance
     */
    public Point2D translatePortConnection(Point2D connPoint, PortInst pi) {
        NodeInst ni = pi.getNodeInst();
        NodeProto np = ni.getProto();
        NodeEquivalence equiv = getNodeEquivalence(ni);
        if (equiv == null) return connPoint;
        PortEquivalence pe = equiv.getPortEquivElec(pi.getPortProto().getName());
        if (pe == null) return connPoint;
        AffineTransform af2 = ni.getOrient().concatenate(Orientation.fromAngle(-equiv.rotation*10)).pureRotate();
//        AffineTransform af2 = NodeInst.pureRotate(ni.getAngle()-(equiv.rotation*10),
//                ni.isMirroredAboutYAxis(), ni.isMirroredAboutXAxis());
        return pe.translateElecToExt(connPoint, af2);
    }

    /**
     * Translate a port location on an external node instance to the equivalent
     * port location on the equivalent Electric node instance
     * @param connPoint the connection point on the external node instance
     * @param externalLib the external node's library
     * @param externalCell the external node
     * @param externalView the external node's view
     * @param externalPort the external node's port in question
     * @return the connection point on the equivalent electric node instance
     */
    public Point2D translatePortConnection(Point2D connPoint, String externalLib, String externalCell,
                                           String externalView, String externalPort, String orientation) {
        NodeEquivalence equiv = getNodeEquivalence(externalLib, externalCell, externalView);
        if (equiv == null) return connPoint;
        PortEquivalence pe = equiv.getPortEquivExt(externalPort);
        if (pe == null) return connPoint;
        int angle = 0;
        boolean mirroredAboutXAxis = false;
        boolean mirroredAboutYAxis = false;
        if (orientation.indexOf("R90") != -1) angle = 900;
        if (orientation.indexOf("R180") != -1) angle = 1800;
        if (orientation.indexOf("R270") != -1) angle = 2700;
        if (orientation.indexOf("MX") != -1) mirroredAboutXAxis = true;
        if (orientation.indexOf("MY") != -1) mirroredAboutYAxis = true;
        Orientation orient = Orientation.fromJava(angle, mirroredAboutYAxis, mirroredAboutXAxis);
        orient = orient.concatenate(Orientation.fromAngle(equiv.rotation*10));
        AffineTransform af2 = orient.pureRotate();
        return pe.translateExtToElec(connPoint, af2);
    }

    // hash map key
    private Object getElectricKey(NodeProto np, PrimitiveNode.Function func, PortCharacteristic portType) {
        if (func == null) func = PrimitiveNode.Function.UNKNOWN;
        if (portType == null) portType = PortCharacteristic.UNKNOWN;
        return np.getName() + " " + func.toString() + " " + portType.getName();
    }

    // hash map key
    private Object getExternalKey(String externalLib, String externalCell, String externalView) {
        return externalLib + " " + externalCell + " " + externalView;
    }

    // size of elecPorts and extPorts lists must be equal
    private void addNodeEquiv(NodeProto np, PrimitiveNode.Function func, PortCharacteristic exportType, int rot,
                         String extLib, String extCell, String extView, List<Port> elecPorts, List<Port> extPorts) {
        List<PortEquivalence> portEquivs = new ArrayList<PortEquivalence>();
        if (elecPorts.size() != extPorts.size()) {
            System.out.println("Error, port lists differ in size!");
            return;
        }
        for (int i=0; i<elecPorts.size(); i++) {
            PortEquivalence pe = new PortEquivalence(elecPorts.get(i), extPorts.get(i));
            portEquivs.add(pe);
        }
        NodeEquivalence equiv = new NodeEquivalence(np, func, exportType, extLib, extCell, extView, rot, portEquivs);
        equivsByExternal.put(getExternalKey(extLib, extCell, extView), equiv);
        equivsByNodeProto.put(getElectricKey(np, func, exportType), equiv);
    }

    /**
     * Create a new EDIF equivalence object. This contains equivalence information between
     * Electric cells/prims and Exteranal EDIF cells/prims. The equivalence information is
     * read from a configuration file. The file has the format:
     * <pre>
     * C Lib Cell View rotation { porta(x,y), ... } ExternalLib ExternalCell ExternalView { porta(x,y), ... }
     * P Tech NodeName Function rotation { porta(x,y), ... } ExternalLib ExternalCell ExternalView { porta(x,y), ... }
     * F FigureGroup ExternalFigureGroup
     * V VariableName ExternalVariableName [appendToElectricOutput]
     * # comment
     * </pre>
     * 'C' is for Cell, and 'P' is for Primitive. The left hand size specifies the Electric cell/node,
     * while the right hand side specifies the External tool's cell/node.  The list of ports must be the
     * same in length, and specify the x,y coordinate of the port.  This coordinate is on the prototype of
     * the node, or also when the node is default size at 0,0.  Note that Electric port locations should
     * be locations after the node has been rotated, if rot is not 0.  Rotation should be in tenth-degrees.
     * 
     * For 'F', an association between internal FigureGroup names and external names is declared.
     * For 'V', an association between internal Variable names and external names is declared.  You
     * can also specify a string to be append to all matching Variable values.
     */
    public EDIFEquiv() {
        equivsByNodeProto = new HashMap<Object,NodeEquivalence>();
        equivsByExternal = new HashMap<Object,NodeEquivalence>();
        exportEquivs = new HashMap();
		exportByVariable = new HashMap<String,VariableEquivalence>();
		exportFromVariable = new HashMap<String,VariableEquivalence>();
		exportByFigureGroup = new HashMap<String,FigureGroupEquivalence>();
		exportFromFigureGroup = new HashMap<String,FigureGroupEquivalence>();
		exportByGlobal = new HashMap<String,GlobalEquivalence>();
		exportFromGlobal = new HashMap<String,GlobalEquivalence>();

		String file = IOTool.getEDIFConfigurationFile();
		if (file.length() == 0)
		{
            System.out.println("No EDIF configuration information being used");
            return;
		}
        File fd = new File(file);
        if (!fd.exists()) {
            System.out.println("Error: EDIF configuration file not found: "+fd.getAbsolutePath());
            return;
        }
        System.out.println("Using EDIF configuration file: "+fd.getAbsolutePath());
        try {
            FileReader reader = new FileReader(fd);
            BufferedReader bufReader = new BufferedReader(reader);
            String line = "";
            int lineno = 1;
            while ((line = bufReader.readLine()) != null) {
                readLine(line, lineno);
                lineno++;
            }
        } catch (IOException e) {
            System.out.println("Error reading EDIF config file ("+fd.getAbsolutePath()+"): "+e.getMessage());
            return;
        }
    }

    private static final Pattern portsPat = Pattern.compile("(.+?)\\{(.+?)\\}(.+?)\\{(.+?)\\}");
    /**
     * Read one line of the configuration file.  See Constructor for file format.
     * @param line
     * @param lineno
     * @return false on error
     */
    private boolean readLine(String line, int lineno) {
        line = line.trim();
        if (line.equals("")) return true;
        if (line.startsWith("#")) return true;

		// special case for Variable equivalences
		if (line.startsWith("V"))
		{
	        String [] parts = line.split("\\s+");
			String append = "";
			double scale = TextUtils.atof(parts[3]);
			if (parts.length == 5) append = parts[4];
			VariableEquivalence ve = new VariableEquivalence(parts[1], parts[2], scale, append);
			exportByVariable.put(parts[1], ve);
			exportFromVariable.put(parts[2], ve);
			return true;
		}

		// special case for FigureGroup equivalences
		if (line.startsWith("F"))
		{
	        String [] parts = line.split("\\s+");
			FigureGroupEquivalence fge = new FigureGroupEquivalence(parts[1], parts[2]);
			exportByFigureGroup.put(parts[1], fge);
			exportFromFigureGroup.put(parts[2], fge);
			return true;
		}

		// special case for Global equivalences
		if (line.startsWith("G"))
		{
	        String [] parts = line.split("\\s+");
			GlobalEquivalence ge = new GlobalEquivalence(parts[1], parts[2]);
			exportByGlobal.put(parts[1], ge);
			exportFromGlobal.put(parts[2], ge);
			return true;
		}

		// grab port parts
        Matcher mat = portsPat.matcher(line);
        if (!mat.find()) {
            System.out.println("Wrong number of curly brackets for ports on line "+lineno);
            return false;
        }
        String elec = mat.group(1).trim();
        String elec_ports = mat.group(2).trim();
        String ext = mat.group(3).trim();
        String ext_ports = mat.group(4).trim();
        String [] parts;

        // internal definition
        NodeProto np = null;
        PrimitiveNode.Function func = null;
        PortCharacteristic portType = null;
        int rot = 0;
        parts = elec.split("\\s+");
        if (parts.length < 1) {
            System.out.println("No Electric arguments on line "+lineno);
            return false;
        }
        boolean keyE = parts[0].equalsIgnoreCase("E") ? true : false;
        boolean keyP = parts[0].equalsIgnoreCase("P") ? true : false;
        boolean keyC = parts[0].equalsIgnoreCase("C") ? true : false;
        if (keyP && parts.length != 5) {
            System.out.println("Wrong number of arguments for Electric Primitive, expected 'P tech node func rot' on line "+lineno);
            return false;
        }
        if (keyE && parts.length != 6) {
            System.out.println("Wrong number of arguments for Electric Primitive, expected 'E tech node func rot porttype' on line "+lineno);
            return false;
        }
        if (keyC && parts.length != 5) {
            System.out.println("Wrong number of arguments for Electric cell, expected 'C lib cell view rot' on line "+lineno);
            return false;
        }
        if (keyP || keyE) {
            // primitive node
            Technology tech = Technology.findTechnology(parts[1]);
            if (tech == null) {
                System.out.println("Could not find Technology "+parts[1]+" on line "+lineno);
                return false;
            }
            np = tech.findNodeProto(parts[2]);
            if (np == null) {
                System.out.println("Could not find PrimitiveNode "+parts[2]+" in technology "+parts[1]+" on line "+lineno);
                return false;
            }
            for (PrimitiveNode.Function function : PrimitiveNode.Function.getFunctions()) {
                if (parts[3].equals(function.getName()) || parts[3].equals(function.getShortName()) ||
                        parts[3].equals(function.getConstantName())) {
                    func = function;
                    break;
                }
            }
            if (func == null) {
                System.out.println("Could not find Function "+parts[3]+" on line "+lineno);
                return false;
            }
            try {
                rot = Integer.parseInt(parts[4]);
            } catch (NumberFormatException e) {
                System.out.println("Rotation "+parts[4]+" is not an integer on line "+lineno);
            }
            if (keyE) {
                // get port type
                portType = PortCharacteristic.findCharacteristic(parts[5]);
                if (portType == null) {
                    System.out.println("Unable to find Export type "+parts[5]+" on line "+lineno);
                    return false;
                }
            }
        } else if (keyC) {
            // cell
            Library lib = Library.findLibrary(parts[1]);
            if (lib == null) {
                System.out.println("Could not find Library "+parts[1]+" on line "+lineno);
                return false;
            }
            np = lib.findNodeProto(parts[2]+"{"+parts[3]+"}");
            if (np == null) {
                System.out.println("Could not find Cell "+parts[2]+", view "+parts[3]+" in library "+parts[1]+" on line "+lineno);
                return false;
            }
            func = PrimitiveNode.Function.UNKNOWN;
            try {
                rot = Integer.parseInt(parts[4]);
            } catch (NumberFormatException e) {
                System.out.println("Rotation "+parts[4]+" is not an integer on line "+lineno);
            }
        } else {
            System.out.println("Unrecognized key "+parts[0]+", expected 'P', 'C', or 'E' on line "+lineno);
            return false;
        }

        // external definition
        String extlib, extname, extview;
        parts = ext.split("\\s+");
        if (parts.length != 3) {
            System.out.println("Wrong number of arguments for external lib, expected 'lib name view' on line "+lineno);
            return false;
        }
        extlib = parts[0];
        extname = parts[1];
        extview = parts[2];

        // port equivalences
        List<Port> elecPorts = parsePortsList(elec_ports, lineno);
        List<Port> extPorts = parsePortsList(ext_ports, lineno);
        if (elecPorts.size() != extPorts.size()) {
            System.out.println("Port lists are not the same size on line "+lineno);
            return false;
        }

        addNodeEquiv(np, func, portType, rot, extlib, extname, extview, elecPorts, extPorts);
        return true;
    }

    /**
     * String should be of the format (ignore spaces):
     * a(x,y), b(x,y), ... z(x,y)
     * @param portsList
     * @return a list of Port objects
     */
    private List<Port> parsePortsList(String portsList, int lineno) {
        // split by commas not contained in ()
        boolean opened = false;
        List<Port> ports = new ArrayList<Port>();
        int i = 0, last = 0;
        for (i=0; i<portsList.length(); i++) {
            char c = portsList.charAt(i);
            if (c == '(') {
                if (opened == true) {
                    System.out.println("Unmatched open parenthesis in ports list on line "+lineno);
                    return ports;
                }
                opened = true;
                continue;
            }
            if (c == ')') {
                if (opened == false) {
                    System.out.println("Unmatched close parenthesis in ports list on line "+lineno);
                    return ports;
                }
                opened = false;
                continue;
            }
            if (c == ',') {
                if (opened == true) // ignore commands in coords (x,y)
                    continue;
                String portDef = portsList.substring(last, i);
                last = i+1;
                Port port = parsePort(portDef, lineno);
                if (port != null) ports.add(port);
            }
        }
        // last one does not have trailing comma
        if (last < i) {
            String portDef = portsList.substring(last, i);
            Port port = parsePort(portDef, lineno);
            if (port != null) ports.add(port);
        }
        return ports;
    }

    /**
     * String should be of the format (ignore spaces):
     * a(x,y)
     * @param port
     * @return a Port object
     */
    private Port parsePort(String port, int lineno) {
        boolean ignorePort = false;
        if (port.trim().equals("NA")) return new Port("NA", new Point2D.Double(0,0), true);
        String [] fields = port.split("[(),]");
        if (fields.length != 3) {
            System.out.println("Expected port format portname(x,y), but got "+port+" on line "+lineno);
            return null;
        }
        double x = 0, y = 0;
        try {
            x = Double.parseDouble(fields[1]);
            y = Double.parseDouble(fields[2]);
        } catch (NumberFormatException e) {
            System.out.println("Could not convert port coordinate to number: "+port+", on line "+lineno);
            return null;
        }
        String name = fields[0].trim();
        if (name.equals("NA")) ignorePort = true;
        if (name.equals("\\NA")) name = "NA";
        return new Port(fields[0].trim(), new Point2D.Double(x, y), ignorePort);
    }

    public void print() {
        for (NodeEquivalence ne : equivsByNodeProto.values()) {
            System.out.println(ne.toString());
        }
    }

    // ==================================================================================
    //                            Equivalence Classes

    public static class NodeEquivalence {
        public final NodeProto np;
        public final PrimitiveNode.Function function;
        public final PortCharacteristic exortedType;

        public final String externalLib;
        public final String externalCell;
        public final String externalView;
        public final List<PortEquivalence> portEquivs;
        public final int rotation;         // in degrees, rotate the electric prim by this value to match the cadence prim

        private NodeEquivalence(NodeProto np, PrimitiveNode.Function func, PortCharacteristic exportedType,
                String externalLib, String externalCell, String externalView, int rotation, List<PortEquivalence> portEquivs) {
            this.np = np;
            this.function = func;
            this.exortedType = exportedType;
            this.externalLib = externalLib;
            this.externalCell = externalCell;
            this.externalView = externalView;
            this.rotation = rotation;
            this.portEquivs = portEquivs;
        }
        /**
         * Get the PortEquivalence object for the Electric port name.
         * @param elecPortName
         * @return null if no such port
         */
        public PortEquivalence getPortEquivElec(String elecPortName) {
            for (PortEquivalence pe : portEquivs) {
                if (pe.getElecPort().name.equals(elecPortName))
                    return pe;
            }
            return null;
        }
        /**
         * Get the PortEquivalence object for the external node's port name.
         * @param extPortName
         * @return null if no such port
         */
        public PortEquivalence getPortEquivExt(String extPortName) {
            for (PortEquivalence pe : portEquivs) {
                if (pe.getExtPort().name.equals(extPortName))
                    return pe;
            }
            return null;
        }

        /**
         * Get a list of the external ports of this equivalence class
         * @return a list of EDIFEquiv.Port objects
         */
        public List<Port> getExtPorts() {
            List<Port> extPorts = new ArrayList<Port>();
            for (PortEquivalence pe : portEquivs) {
                extPorts.add(pe.getExtPort());
            }
            return extPorts;
        }

        public String toString() {
            StringBuffer buf = new StringBuffer();
            buf.append("NodeEquivalence Elec: "+np.describe(false)+", func: "+function+"\n");
            buf.append("  Ext: "+externalLib+" "+externalCell+" "+externalView+"\n");
            for (PortEquivalence pe : portEquivs) {
                buf.append(pe.toString()+"\n");
            }
            return buf.toString();
        }
    }

    public static class PortEquivalence {
        private final Port elecPort;
        private final Port extPort;
        private PortEquivalence(Port elecPort, Port extPort) {
            this.elecPort = elecPort;
            this.extPort = extPort;
        }
        public Port getElecPort() { return elecPort; }
        public Port getExtPort() { return extPort; }
        /**
         * Translate the location of the electric port to the external port
         * @param point the location of the port in Electric.
         * @return the location of the port in EDIF.
         */
        public Point2D translateElecToExt(Point2D point, AffineTransform niPureRotation) {
            Point2D elecPoint = new Point2D.Double(elecPort.loc.getX(), elecPort.loc.getY());
            Point2D extPoint = new Point2D.Double(extPort.loc.getX(), extPort.loc.getY());
            if (niPureRotation != null) {
                elecPoint = niPureRotation.transform(elecPort.loc, elecPoint);
                extPoint = niPureRotation.transform(extPort.loc, extPoint);
            }
            return new Point2D.Double(point.getX()-(elecPoint.getX()-extPoint.getX()),
                                      point.getY()-(elecPoint.getY()-extPoint.getY()));
        }
        /**
         * Translate the location of the external port to the electric port
         * @param point the location of the port in EDIF.
         * @return the location of the port in Electric.
         */
        public Point2D translateExtToElec(Point2D point, AffineTransform niPureRotation) {
            Point2D elecPoint = new Point2D.Double(elecPort.loc.getX(), elecPort.loc.getY());
            Point2D extPoint = new Point2D.Double(extPort.loc.getX(), extPort.loc.getY());
            if (niPureRotation != null) {
                elecPoint = niPureRotation.transform(elecPort.loc, elecPoint);
                extPoint = niPureRotation.transform(extPort.loc, extPoint);
            }
            return new Point2D.Double(point.getX()-(elecPoint.getX()-extPoint.getX()),
                                      point.getY()-(elecPoint.getY()-extPoint.getY()));
        }
        public String toString() {
            return "PortEquiv Elec{ "+elecPort+" } - Ext{ "+extPort+" }";
        }
    }

    public static class VariableEquivalence {
        public final String elecVarName;
        public final String externVarName;
        public final double scale;
        public final String appendElecOutput;

        private VariableEquivalence(String elecVarName, String externVarName, double scale, String appendElecOutput) {
            this.elecVarName = elecVarName;
            this.externVarName = externVarName;
            this.scale = scale;
            this.appendElecOutput = appendElecOutput;
        }
    }

    public static class FigureGroupEquivalence {
        public final String elecFGName;
        public final String externFGName;

        private FigureGroupEquivalence(String elecFGName, String externFGName) {
            this.elecFGName = elecFGName;
            this.externFGName = externFGName;
        }
    }

    public static class GlobalEquivalence {
        public final String elecGName;
        public final String externGName;

        private GlobalEquivalence(String elecGName, String externGName) {
            this.elecGName = elecGName;
            this.externGName = externGName;
        }
    }

    public static class Port {
        public final String name;
        public final Point2D loc;
        public final boolean ignorePort;
        private Port(String name, Point2D loc, boolean ignorePort) {
            this.name = name;
            this.loc = loc;
            this.ignorePort = ignorePort;
        }
        public String toString() {
            return name+"("+loc.getX()+","+loc.getY()+")"+(ignorePort?"[ignored]":"");
        }
    }

    /** Unit Test */
    public static void mainTest() {
        EDIFEquiv eq = new EDIFEquiv();
        eq.print();
    }
}
