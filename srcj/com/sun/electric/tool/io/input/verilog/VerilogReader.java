package com.sun.electric.tool.io.input.verilog;

import com.sun.electric.database.geometry.Orientation;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.id.CellId;
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.technology.technologies.Schematics;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.placement.Placement;
import com.sun.electric.tool.simulation.Simulation;
import com.sun.electric.tool.io.input.Input;
import com.sun.electric.tool.user.ViewChanges;
import com.sun.electric.tool.user.IconParameters;

import java.awt.geom.Point2D;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * User: gg151869
 * Date: Oct 23, 2006
 */
public class VerilogReader extends Input
{
    List<NodeInst> transistors = new ArrayList<NodeInst>();
    double maxWidth = 100, nodeWidth = 10;
    double primitiveHeight = 0.5, primitiveWidth = 0.5;
    Map<Cell, Point2D.Double> locationMap = new HashMap<Cell, Point2D.Double>();
    PrimitiveNode essentialBounds = Generic.tech().findNodeProto("Essential-Bounds");
    Cell topCell = null;
    Map<String, NodeInst> pinsMap = new HashMap<String, NodeInst>();
    private String typicalSkipStrings = "\t\\"; // strings that should be ignored by the StringTokenizer()
	private VerilogPreferences localPrefs;

	public static class VerilogPreferences extends InputPreferences
    {
		public boolean runPlacement = Simulation.getFactoryVerilogRunPlacementTool();
        Placement.PlacementPreferences placementPrefs;
        IconParameters iconParameters = IconParameters.makeInstance(true);

        public VerilogPreferences(boolean factory)
        {
            super(factory);
            if (!factory)
                runPlacement = Simulation.getVerilogRunPlacementTool();
            // need to cache placement preference here even though it might not be used later
            placementPrefs = new Placement.PlacementPreferences(factory);
            placementPrefs.getOptionsFromPreferences();
        }

        @Override
        public Library doInput(URL fileURL, Library lib, Technology tech, Map<Library,Cell> currentCells, Map<CellId,BitSet> nodesToExpand, Job job)
        {
        	VerilogReader in = new VerilogReader(this);
			if (in.openTextInput(fileURL)) return null;
			lib = in.importALibrary(lib, tech, currentCells);
            // running placement tool if selected
            if (lib != null && runPlacement)
            {
                Placement.placeCellNoJob(currentCells.get(lib), placementPrefs);
            }
            in.closeInput();
			return lib;
        }
    }

	/**
	 * Creates a new instance of VerilogReader.
	 */
	public VerilogReader(VerilogPreferences ap) { localPrefs = ap; }

    private String readCellHeader(List<String> inputs) throws IOException
    {
        for (;;)
        {
            String key = getAKeyword();
            StringTokenizer parse = new StringTokenizer(key, "( ),\t", false);
            while (parse.hasMoreTokens())
            {
                String value = parse.nextToken();
                if (value.equals(";")) // done with header
                    return null;
                inputs.add(value);
            }
        }
    }

    private static class CellInstance
    {
        String name;
        List<PortInfo> list = new ArrayList<PortInfo>();

        CellInstance(String n)
        {
            this.name = TextUtils.correctName(n, false, true);
        }
        static class PortInfo
        {
            String local;
            boolean isBus;
            PortProto ex;

            PortInfo(String local, boolean isBus, PortProto ex)
            {
                // Doesn't correct name if it is a bus
                this.local = (isBus) ? local : TextUtils.correctName(local, false, true);
                this.isBus = isBus;
                this.ex = ex;
            }
        }

        void addConnection(String local, boolean isBus, PortProto ex)
        {
            PortInfo port = new PortInfo(local, isBus, ex);
            list.add(port);
        }
    }

//    private void createInstance(Cell parent, VerilogData verilogData,
//                                VerilogData.VerilogModule module, Cell icon, CellInstance info)
//    {
//        NodeInst cellInst = NodeInst.newInstance(icon, getNextLocation(parent), 10, 10, parent,
//                        Orientation.IDENT, info.name, 0);
//
//        List<String> localPorts = new ArrayList<String>();
//
//        for (CellInstance.PortInfo port : info.list)
//        {
//            localPorts.clear();
//
//            String portLocal = port.local;
//
//            // It is unknown how many pins are coming in the stream
//            if (portLocal.contains("{"))
//            {
//                StringTokenizer parse = new StringTokenizer(portLocal, "{,}", false); // extracting pins
//                while (parse.hasMoreTokens())
//                {
//                    String name = parse.nextToken();
//                    name = name.replaceAll(" ", "");
//                    localPorts.add(name);
//                }
//            }
//            else
//                localPorts.add(portLocal);
//
//            for (String s : localPorts)
//            {
//                NodeInst pin = pinsMap.get(s);
//
//                if (pin == null)
//                {
//                    int index = s.indexOf("[");
//                    if (index != -1)
//                    {
//                        s = s.substring(0, index);
//                        pin = pinsMap.get(s);
//                    }
//                }
//
//                if (pin == null)
//                {
//                    if (s.equals("vss")) // ground
//                    {
//                        pin = readSupply(module, false, s);
//                    }
//                    else
//                    {
//                        if (Job.getDebug())
//                            System.out.println("Unknown signal " + s + " in cell " + parent.describe(false));
//                        PrimitiveNode primitive = (port.isBus) ? Schematics.tech().busPinNode : Schematics.tech().wirePinNode;
//                        pin = NodeInst.newInstance(primitive, getNextLocation(parent),
//                                primitiveWidth, primitiveHeight,
////                                        primitive.getDefWidth(), primitive.getDefHeight(),
//                                parent, Orientation.IDENT, null/*s*/, 0);
//                        pinsMap.put(s, pin);
//                    }
//                }
//
////                ArcProto node = (port.isBus) ? Schematics.tech.bus_arc : Schematics.tech.wire_arc;
//                ArcProto node = (pin.getProto() == Schematics.tech().busPinNode) ? Schematics.tech().bus_arc : Schematics.tech().wire_arc;
//                PortInst ex = cellInst.findPortInst(port.ex.getName());
//                ArcInst ai = ArcInst.makeInstanceBase(node, 0.0,
////                ArcInst ai = ArcInst.makeInstanceFull(node, 0.0 /*node.getDefaultLambdaFullWidth()*/,
//                        pin.getOnlyPortInst(), ex, null, null, s);
//                assert(ai != null);
//                ai.setFixedAngle(false);
//            }
//        }
//    }

    private NodeInst readSupply(VerilogData.VerilogModule module, boolean power, String name)
    {
        VerilogData.VerilogPort supply = module.addPort(name, false, false);
        supply.type = (power) ? PortCharacteristic.PWR : PortCharacteristic.GND;
        return null;
    }

    private CellInstance readInstance(VerilogData.VerilogModule module,
                                      VerilogData.VerilogModule element) throws IOException
    {
        StringBuffer signature = new StringBuffer();
        List<String> exports = new ArrayList<String>();
        List<String> pins = new ArrayList<String>();

        for (;;)
        {
            String key = getRestOfLine();

            if (key.contains("//")) continue; // comment

            signature.append(key);

            if (key.contains(";")) // found end of signature
            {
                String line = signature.toString();
                int index = line.indexOf("("); // searching for first (
                String instanceName = element.getName() + "-instance";

                // if index==0, no name provided -> name is null
                assert(index > -1); // do we have cases with -1?
                if (index > 0)
                {
                    instanceName = line.substring(0, index);
                }
                line = line.substring(index+1, line.length());
                StringTokenizer parse = new StringTokenizer(line, ")", false);  //typicalSkipStrings can't be used
                exports.clear(); pins.clear();

                while (parse.hasMoreTokens())
                {
                    String value = parse.nextToken();
                    value = value.replaceAll(" ", "");
                    index = value.indexOf("."); // look for first .
                    if (index == -1) // end of tokens
                        continue; // or break?
                    int index2 = value.indexOf("("); // look for first (
                    assert(index2 != -1);
                    String n = value.substring(index+1, index2);
                    n = TextUtils.correctName(n, false, true);
//                    int index3 = n.indexOf("\\"); // those \ are a problem!
//                    if (index3 != -1)
//                        n = n.substring(index3+1);
                    exports.add(n);
                    n = value.substring(index2+1);
                    n = TextUtils.correctName(n, false, false);
                    if (n.contains(" "))
                        assert(false); // get rid of those empty sapces?
//                    pins.add(value.substring(index2+1));
                    pins.add(n);
                }

                // remove extra white spaces
                instanceName = TextUtils.correctName(instanceName, false, true);
                instanceName = instanceName.replaceAll(" ", "");
                CellInstance localCell = new CellInstance(instanceName);
                VerilogData.VerilogInstance verilogInst = null;

                verilogInst = module.addInstance(instanceName, element);

                for (int i = 0; i < exports.size(); i++)
                {
                    String export = exports.get(i);
                    String pin = pins.get(i);

                    pin = pin.replaceAll(" ", "");
                    export = export.replaceAll(" ", "");

                    VerilogData.VerilogPort exp = element.findPort(export);
                    // fixing original export if not found
                    if (exp == null)
                    {
//                            System.out.println("Warning: port " + export + " not found in module " + element.name + " yet");
                        exp = element.addPort(export, false, true);
                    }
                    verilogInst.addPortInstance(pin, exp);
                }
                return localCell;
            }
        }
        // never reach this point
    }

    private String readWiresAndSupplies(VerilogData.VerilogModule module,
                                        boolean readWires, boolean power) throws IOException
    {
        List<String> values = new ArrayList<String>(2);
        for (;;)
        {
            String input = getRestOfLine();
            StringTokenizer parse = new StringTokenizer(input, ",;", true); // net1, net2, [9:0] net4;

            while (parse.hasMoreTokens())
            {
                String net = parse.nextToken();
                if (net.equals(",")) continue;
                if (net.equals(";"))
                {
                    return null; // done
                }
                if (readWires)   // wires
                {
                    StringTokenizer p = new StringTokenizer(net, typicalSkipStrings+" ", false);
                    values.clear(); // clean reset
                    while (p.hasMoreTokens())
                    {
                        values.add(p.nextToken());
                    }
                    int size = values.size();
                    if (size == 0) continue;
                    assert(size == 1 || size == 2);
                    PrimitiveNode primitive = Schematics.tech().wirePinNode;
                    String pinName = values.get(size-1);
                    int[] vals = {0, 0};
                    int count = 0;

                    if (values.size() == 2)
                    {
                        p = new StringTokenizer(values.get(0), "[:]", false);
                        while (p.hasMoreTokens())
                        {
                            String s = p.nextToken();
                            if (TextUtils.isANumber(s))
                                vals[count++] = Integer.parseInt(s);
                        }

                        if (count == 2 && vals[0] != vals[1]) // only if it is a real bus
                        {
//                          pinName += values.get(0);
                            primitive = Schematics.tech().busPinNode;
                        }
                        else
                            System.out.println(net + " is not a bus wire");
                    }
                    pinName = TextUtils.correctName(pinName, false, true);

                    // also considering [x:x]. Not doing the exception here as above
                    module.addWire(pinName, (values.size() == 2) ? values.get(0) : null);
                }
                else // supplies
                {
                    StringTokenizer p = new StringTokenizer(net, "\t ", false);
                    String name = p.nextToken();
                    name = TextUtils.correctName(name, false, true);
                    readSupply(module, power, name); // supply1 -> vdd, supply0 -> gnd or vss
                }
            }
        }
        // never reach this point
    }

    /**
     * Method to ignore certain amount of lines. Useful for begin/end blocks and tables
     * @param endString
     * @throws IOException
     */
    private void ignoreUntilEndOfStatement(String endString) throws IOException
    {
        String key = (endString != null) ? endString : ";";  // endString != null for table for example

        for (;;)
        {
            String input = getRestOfLine();
            if (endString == null && input.contains("begin")) // swtch to end only if it is not a table
                key = "end";
            if (input.contains(key))
                return; // finish
        }
    }

    private String readInputOutput(VerilogData.VerilogModule module,
                                   PortCharacteristic portType) throws IOException
    {
        for (;;)
        {
            String input = getRestOfLine();
            if (!input.contains(";"))
            {
                // doesn't contain proper end of line
                String msg = "Missing end of line character ';' in input/output'" + input + "'";
                System.out.println(msg);
               throw new IOException(msg);
            }
            StringTokenizer parse = new StringTokenizer(input, ";,", true); // extracting only input name

            while (parse.hasMoreTokens())
            {
                String net = parse.nextToken();
                if (net.equals(","))
                    continue;
                if (net.equals(";"))
                {
                    return null; // done
                }
                StringTokenizer p = new StringTokenizer(net, " \t", false); // extracting only input name
                List<String> l = new ArrayList<String>(2);
                while (p.hasMoreTokens())
                {
                    String name = p.nextToken();
                    l.add(name); // it could be "input a;" or "input [9:0] a;"
                }
//                PrimitiveNode primitive = Schematics.tech().wirePinNode;
                int size = l.size();
                if (size == 0) continue;
                assert(size == 1 || size == 2);
                String name = l.get(size - 1);
                if (l.size() == 2) // "input a[];"
                {
//                    name += l.get(0); busPin not longer containing [x:y]
//                    primitive = Schematics.tech().busPinNode;
                }

                VerilogData.VerilogPort export = module.findPort(name);
                // input a, b, c
                // ,d, c got problems to parse
                if (Job.getDebug())
                    assert(export != null);
                if (export != null)
                {
                    // except for clk!!
                    if (export.type != PortCharacteristic.UNKNOWN && export.type != portType)
                        System.out.println("Inconsistency in asigning port type in " + name + ". Found " + portType +
                        " and was " + export.type);
    //                    else
                        export.type = portType;
                    if (l.size() == 2)
                        export.setBusInformation(l.get(0));
                }
            }
        }
        // never reach this point
    }

    private String readCell(VerilogData verilogData, boolean primitive) throws IOException
    {
        List<String> inputs = new ArrayList<String>(10);
        readCellHeader(inputs);

        String cellName = inputs.get(0);
        VerilogData.VerilogModule module = null;
        Cell cell = null;

        module = verilogData.getModule(cellName);
        if (module == null)
            module = verilogData.addModule(cellName, primitive, true);
        module.setValid(true);
        // adding ports in modules: from 1 -> inputs.size()-1;
        for (int i = 1; i < inputs.size(); i++)
            module.addPort(inputs.get(i), true, true);

        String nextToken = null;

        for (;;)
        {
            String key = null;
            if (nextToken != null) // get last token read by network section
            {
                key = nextToken;
                nextToken = null;
            }
            else
                key = getAKeyword();

            if (key == null)
            {
                String msg = "Reach end of file without finding a valid key, i.e. end of a comment";
                System.out.println(msg);
                throw new IOException(msg);
            }
            if (key.startsWith("/*"))
            {
                // read until */ is found
                getRestOfComment();
                continue;
            }
            if (key.startsWith("/")) // comment like //
            {
                getRestOfLine();
                continue;
            }

            if (key.startsWith("endmodule") || key.startsWith("endprimitive"))
            {
                // done with this cell
                return null;
            }

            if (key.equals("wire"))
            {
                readWiresAndSupplies(module, true, false);
                continue;
            }
            if (key.startsWith("tri"))
                assert(false); // not implemented

            if (key.equals("input"))
            {
                readInputOutput(module, PortCharacteristic.IN);
                continue;
            }
            if (key.equals("output"))
            {
                readInputOutput(module, PortCharacteristic.OUT);
                continue;
            }
            if (key.equals("inout"))
            {
                readInputOutput(module, PortCharacteristic.BIDIR);
                continue;
            }

            if (key.startsWith("supply"))
            {
                boolean power = key.contains("supply1");
                readWiresAndSupplies(module, false, power);
                continue;
            }

            // ignoring some elements
            if (key.equals("assign") || key.startsWith("always") || key.startsWith("initial")
                    || key.startsWith("reg") || key.startsWith("table") || key.startsWith("specify"))
            {
                if (Job.getDebug())
                    System.out.println("Ignoring " + key);
                String endStatement = null;
                if (key.startsWith("table"))
                    endStatement = "endtable";
                else if (key.startsWith("specify"))
                    endStatement = "endspecify";
                ignoreUntilEndOfStatement(endStatement); // either ; or end
                continue;
            }
            
            if (key.equals("tranif1")) // transistors
            {
                // reading gates
                //tranif1 nmos4p_0(gnd, gnd, vPlt); -> nmos
                assert(false); // implement again
                nextToken = readGate(cell, PrimitiveNode.Function.TRANMOS);
                continue;
            }
            if (key.equals("tranif0")) // transistors
            {
                // reading gates
                //tranif1 nmos4p_0(gnd, gnd, vPlt); -> nmos
                assert(false); // implement again
                nextToken = readGate(cell, PrimitiveNode.Function.TRAPMOS);
                continue;
            }

            // reading cell instances
            VerilogData.VerilogModule element = verilogData.getModule(key);

            if (element == null) // it hasn't been created
            {
                element = verilogData.addModule(key, false, false); // assuming latches and other elements are treat as subcells
            }

            readInstance(module, element);
        }
        // not reaching this point.
    }

    /**
     * Method to get next X,Y position of the NodeInst in matrix. It also increments the counter for the next elements.
     * @return Point2D.Double represeting the NodeInst location
     */
    private Point2D.Double getNextLocation(Cell cell)
    {
        Point2D.Double point = locationMap.get(cell);
        double xPos = 0, yPos = 0;

        if (point != null) // first time
        {
            xPos = point.getX();
            yPos = point.getY();
        }
        double x = xPos*nodeWidth, y = yPos*nodeWidth;
        Point2D.Double p = new Point2D.Double(x, y);
        if (x > maxWidth)
        {
            yPos++; xPos = 0;
        }
        else
            xPos++;
        point = new Point2D.Double(xPos, yPos);
        locationMap.put(cell, point); // storing data for next node in cell
        return p;
    }

    /**
     * Method to read gate information including ports
     * @param cell
     * @param function
     * @return Next string to evaluate
     */
    private String readGate(Cell cell, PrimitiveNode.Function function) throws IOException
    {
        String input = getRestOfLine();
        StringTokenizer parse = new StringTokenizer(input, "(;, \t)", false); // extracting only input name
        List<String> list = new ArrayList<String>(2);

        while (parse.hasMoreTokens())
        {
            String value = parse.nextToken();
            list.add(value) ;
        }
        Orientation orient = Orientation.fromAngle(900);
//        String gateName = list.get(0);
        double width = Schematics.tech().transistorNode.getDefWidth();
        double height = Schematics.tech().transistorNode.getDefHeight();
        Point2D p = getNextLocation(cell);
        NodeInst ni = NodeInst.newInstance(Schematics.tech().transistorNode, p, width, height,
                                       cell, orient, null /*gateName*/);
        Schematics.tech().transistorNode.getTechnology().setPrimitiveFunction(ni, function);
        transistors.add(ni);
        PortInst[] ports = new PortInst[3];
        int count = 0;

        for (Iterator<PortInst> it = ni.getPortInsts(); it.hasNext();)
        {
            ports[count++] = it.next();
        }

        for (int i = 1; i < list.size(); i++)
        {
            // Gate is the first port, then source and the last one is drain
            String name = list.get(i);
//            NodeInst pin = cell.findNode(name);
            // if pin already exists, the name will be composed
//            String pinName = (pin!=null) ? gateName+"-"+name : name;
//            parse = new StringTokenizer(name, "[", false); // extracting possible bus name
//            String realName = parse.nextToken();
//            NodeInst pin = cell.findNode(realName);
//            boolean wirePin = (name.equals(realName));
//            assert(pin != null);
            int pos = (3 + i) % 3; // the first port in g in Electric primitive which is the last (control) port in Verilog
            double posX = p.getX(), posY = p.getY();
            switch (pos)
            {
                case 0: // gnd
                    posX -= width/2;
                    break;
                case 1: // source
                    posX += width/2; posY -= height/2;
                    break;
                case 2: // drain
                    posX += width/2; posY += height/2;
                    break;
            }
//            PrimitiveNode primitive = (wirePin) ? Schematics.tech.wirePinNode : Schematics.tech.busPinNode;
            PrimitiveNode primitive = Schematics.tech().wirePinNode;
            ni = NodeInst.newInstance(primitive, new Point2D.Double(posX, posY),
                        primitiveWidth /*primitive.getDefWidth()*/, primitiveHeight /*primitive.getDefHeight()*/,
                        cell, Orientation.IDENT, null /*pinName*/);

            ArcInst.makeInstanceBase(Schematics.tech().wire_arc, 0.0,
//            ArcInst.makeInstanceFull(Schematics.tech.wire_arc, 0.0 /*Schematics.tech.wire_arc.getDefaultLambdaFullWidth()*/,
                    ni.getOnlyPortInst(), ports[pos], null, null, name);
        }
        return null;
    }

    /**
	 * Method to import a Verilog file from disk.
	 * @param lib the library to ready
     * @param currentCells this map will be filled with currentCells in Libraries found in library file
	 * @return the created library (null on error).
	 */
    @Override
	protected Library importALibrary(Library lib, Technology tech, Map<Library,Cell> currentCells)
    {
        initKeywordParsing();
        VerilogData verilogData = parseVerilogInternal(lib.getName(), true);
        buildCells(verilogData, lib, true);
        topCell = verilogData.getTopSchematicCell();
        if (topCell != null)
        {
            currentCells.put(topCell.getLibrary(), topCell);
            return topCell.getLibrary();
        }
        return null;
    }

    public VerilogData parseVerilog(String[] lines, String verilogName)
    {
        if (openStringsInput(lines))

        {
            System.out.println("Cannot open string set " + verilogName + " as Verilog");
            return null;
        }
        System.out.println("Reading Verilog format " + verilogName);
        initKeywordParsing();
        setProgressValue(0);
        setProgressNote("Reading Verilog format " + verilogName);
        VerilogData verilogData = parseVerilogInternal(verilogName, true);
        System.out.println("Verilog format " + verilogName + " read");
        return verilogData;
    }

    /**
     * Function to parse Verilog file without creating Electric objects.
     * @param file
     * @param simplifyWires
     * @return VerilogData object
     */
    public VerilogData parseVerilog(String file, boolean simplifyWires)
    {
        URL fileURL = TextUtils.makeURLToFile(file);
        if (openTextInput(fileURL))
        {
            System.out.println("Cannot open the Verilog file: " + file);
            return null;
        }
        System.out.println("Reading Verilog file: " + file);
        initKeywordParsing();
        setProgressValue(0);
        setProgressNote("Reading Verilog file:" + file);
        VerilogData verilogData = parseVerilogInternal(file, simplifyWires);
        System.out.println("Verilog file: " + file + " read");
        return verilogData;
    }

    public void createCellsOnly(VerilogData verilogData, Job job)
    {
        Library library = Library.newInstance(verilogData.name, null);
//        String topCellName = TextUtils.getFileNameWithoutExtension(verilogData.name, true);
        buildCells(verilogData, library, false);
//        Cell theCell = library.findNodeProto(topCellName);
        if (job != null)
            System.out.println("Accumulative time after creating cells '" + verilogData.name + "' " + job.getInfo());
//        return theCell; // still work because VerilogReader remembers the top cell
    }

    public VerilogData readVerilogOnly(String file, boolean fullOyster, Job job)
    {
        VerilogData verilogData = parseVerilog(file, fullOyster);

        if (verilogData == null) return null; // error

        if (job != null)
            System.out.println("Accumulative time before creating cells '" + file + "' " + job.getInfo());
        return verilogData;
    }

    public Cell readVerilog(String testName, String file, boolean createCells, boolean simplifyWires, Job job)
    {
        URL fileURL = TextUtils.makeURLToFile(file);
        VerilogData verilogData = parseVerilog(file, simplifyWires);
        if (verilogData == null) return null; // error
        int index = file.lastIndexOf("/");
        String libName = file.substring(index+1);

        if (job != null)
            System.out.println("Accumulative time before creating cells '" + testName + "' " + job.getInfo());
        // Last verilogName must be the top one
        if (createCells)
        {
            Library library = Library.newInstance(libName, null);
            String topCellName = TextUtils.getFileNameWithoutExtension(fileURL);
            buildCells(verilogData, library, simplifyWires);
            topCell = verilogData.getTopSchematicCell();
            if (topCell == null)
            {
                System.out.println("Check this case in readVerilog");  // is it relevant?
            }
        }
        if (job != null)
            System.out.println("Accumulative time after creating cells '" + testName + "' " + job.getInfo());
        return topCell; // still work because VerilogReader remembers the top cell
    }

    private VerilogData parseVerilogInternal(String fileName, boolean simplifyWires)
    {
        VerilogData verilogData = new VerilogData(fileName);

        try
        {
            String nextToken = null;
            String key = null;

            for(;;)
            {
                if (nextToken != null) // get last token read by network section
                {
                    key = nextToken;
                    nextToken = null;
                }
                else
                    key = getAKeyword();

                if (key == null) break; // end of the file

                if (key.startsWith("/"))
                {
                    getRestOfLine();
                    continue; // comments
                }
                if (key.equals("module") || key.equals("primitive"))
                {
                    boolean primitive = key.equals("primitive");
                    nextToken = readCell(verilogData, primitive);
                }
            }
        } catch (IOException e)
        {
            System.out.println("ERROR reading Verilog file");
        }

        // Simplify wires?: a[1], a[2], a[3] -> a[1:3]
        if (simplifyWires) verilogData.simplifyWires();
        return verilogData;
    }

    /**************************************************************************************************************
     * Functions to build Electric cells from VerilogData. Functions are here to simplify the VerilogData class
     * and dependent classes
     *************************************************************************************************************/

    /**
     * Function to build cells from a VerilogData object
     * @param verilogCell
     * @param lib
     * @param createIconCells
     */
    private void buildCells(VerilogData verilogCell, Library lib, boolean createIconCells)
    {
        for (VerilogData.VerilogModule module : verilogCell.getModules())
        {
            Cell cell = buildCellFromModule(module, lib, createIconCells);
//            if (topCell == null && cell.getLibrary() == lib) // first new
//                topCell = cell;
        }
    }

    private void addPins(VerilogData.VerilogConnection port, Cell cell, boolean addExport, boolean fullOyster)
    {
        PortCharacteristic portType = port.getPortType();

        List<String> pinNames = port.getPinNames(fullOyster); // This function controls if busses are split into multi pins
        // or as just one element

        Collections.sort(pinNames);
        for (String pinName : pinNames)
        {
            PrimitiveNode primitive = Schematics.tech().wirePinNode;
            NodeInst ni = cell.findNode(pinName);

            if (ni == null)
            {
                ni = NodeInst.newInstance(primitive, getNextLocation(cell),
                        primitiveWidth, primitiveHeight,
                        cell, Orientation.IDENT, pinName);
                if (addExport)
                {
                    Export.newInstance(cell, ni.getOnlyPortInst(), pinName, portType, localPrefs.iconParameters);
                }
            }
            else
            {
                assert(false);
                 System.out.println("Wire/Input/Output " + pinName + " exists");
            }
        }
    }

    /**
     * Function to build cell fro a VerilogModule object
     * @param lib
     * @param createIconcells
     * @return Cell object representing this module
     */
    private Cell buildCellFromModule(VerilogData.VerilogModule module, Library lib, boolean createIconcells)
    {
        String cellName = module.name + View.SCHEMATIC.getAbbreviationExtension();
        Cell cell = Library.findCellInLibraries(cellName, View.SCHEMATIC, null);
        if (cell != null) return cell; // already created;

        cell = Cell.makeInstance(lib, cellName);
        cell.setTechnology(Schematics.tech());
        // Adding essential bounds for now
        // Change Sept 08, 07 Out
//        NodeInst.makeInstance(essentialBounds, new Point2D.Double(10,10), 1, 1, cell,
//                Orientation.IDENT, null, 0);
//        NodeInst.makeInstance(essentialBounds, new Point2D.Double(-10,-10), 1, 1, cell,
//                Orientation.RR, null, 0);

        List<Object> all = module.getAllSorted();
        for (Object obj : all)
        {

            if (obj instanceof VerilogData.VerilogWire)
        // wires first to determine which pins are busses or simple pins
//        for (VerilogData.VerilogWire wire : module.getWires())
        {
            VerilogData.VerilogWire wire = (VerilogData.VerilogWire)obj;
            addPins(wire, cell, false, createIconcells);
        }

        else if (obj instanceof VerilogData.VerilogPort)
        // inputs/outputs/inouts/supplies
//        for (VerilogData.VerilogPort port : module.getPorts())
        {
            //Point2D center, double width, double height, Cell parent)
            VerilogData.VerilogPort port = (VerilogData.VerilogPort)obj;
            String name = port.name;
            PortCharacteristic portType = port.type;

            // input/output/inout
            if (portType == PortCharacteristic.BIDIR ||
                    portType == PortCharacteristic.IN ||
                    portType == PortCharacteristic.OUT ||
                    portType == PortCharacteristic.CLK ||
                    portType == PortCharacteristic.UNKNOWN) // unknown when modules are read as instances
            {
                // new code
                addPins(port, cell, true, createIconcells);
            }
            else if (portType == PortCharacteristic.PWR ||
                    portType == PortCharacteristic.GND)
            {
                boolean power = portType == PortCharacteristic.PWR;
                PrimitiveNode np = (power) ? Schematics.tech().powerNode : Schematics.tech().groundNode;
                Point2D.Double p = getNextLocation(cell);
                double height = primitiveHeight; //np.getDefHeight();
                NodeInst supply = NodeInst.newInstance(np, p,
                        primitiveWidth, height,
                        cell, Orientation.IDENT, name);
                // extra pin
                NodeInst ni = NodeInst.newInstance(Schematics.tech().wirePinNode, new Point2D.Double(p.getX(), p.getY()+height/2),
                        0.5, 0.5,
                        cell, Orientation.IDENT, name+"@0");

                ArcInst.makeInstanceBase(Schematics.tech().wire_arc, 0.0,
                    ni.getOnlyPortInst(), supply.getOnlyPortInst(), null, null, name);

                Export.newInstance(cell, ni.getOnlyPortInst(), name, portType, localPrefs.iconParameters);
            }
            else
                System.out.println("Skipping this characteristic?");
        }
        }

        // instances
        for (VerilogData.VerilogInstance inst : module.getInstances())
        {
            buildNodeInstFromModule(inst, lib, cell, createIconcells);
        }

        // making icon
        if (createIconcells)
            ViewChanges.makeIconViewNoGUI(cell, true, true);

        return cell; // not too much sense?
    }

    /**
     * Function to build a NodeInst object from a VerilogInstance object
     * @param inst
     * @param lib
     * @param parent
     * @param useIconCell
     */
    Cell buildNodeInstFromModule(VerilogData.VerilogInstance inst, Library lib, Cell parent, boolean useIconCell)
    {
        Cell schematics = buildCellFromModule(inst.element, lib, useIconCell);
        Cell icon = (useIconCell) ? schematics.iconView() : schematics;
//        if (icon == null)
//        assert(icon != null);

        // Only for benchmarks schematics in
        NodeInst cellInst = NodeInst.newInstance(icon, getNextLocation(parent), 10, 10, parent,
                Orientation.IDENT, inst.name);

        for (VerilogData.VerilogPortInst port : inst.ports)
        {
            List<String> localPorts = port.getPortNames();

            // Start and end are only valid for bus wires/inputs. The pin numbers should be in the order
            // they were defined in the port (ascendent or descendent)
            int startPort = port.port.start;
            int endPort = port.port.end;
            int count = startPort;
            boolean asc = (startPort < endPort);

            for (String s : localPorts)
            {
                NodeInst pin = parent.findNode(s);  // not sure if this should be done per cell or ask

                if (pin == null)
                {
                        int index = s.indexOf("[");
                        if (index != -1)
                        {
                            s = s.substring(0, index);
                            pin = parent.findNode(s);
                        }
                }
                if (pin == null)
                {
                    // Still missing vss code?
                     if (Job.getDebug())
                            System.out.println("Unknown signal " + s + " in cell " + parent.describe(false));
                        PrimitiveNode primitive = (port.port.isBusConnection()) ? Schematics.tech().busPinNode : Schematics.tech().wirePinNode;
                        pin = NodeInst.newInstance(primitive, getNextLocation(parent),
                                primitiveWidth, primitiveHeight,
                                parent, Orientation.IDENT, /*null*/s);  // not sure why it has to be null?
                }

                ArcProto node = (pin.getProto() == Schematics.tech().busPinNode) ? Schematics.tech().bus_arc : Schematics.tech().wire_arc;
                String exportName = port.port.name;
                if (port.port.isBusConnection())
                {
                    // add bit so the pin can be found.
                    exportName += "[" + count + "]";
                }
                PortInst ex = cellInst.findPortInst(exportName);
                assert(ex != null); // it can't work without export. Check this case if fails
                ArcInst ai = ArcInst.makeInstanceBase(node, 0.0,
//                ArcInst ai = ArcInst.makeInstanceFull(node, 0.0 /*node.getDefaultLambdaFullWidth()*/,
                        pin.getOnlyPortInst(), ex, null, null, s);
                if (ai == null)
                    assert(ai != null);
                ai.setFixedAngle(false);
                if (asc) count++;
                else count--;
            }
        }
        return schematics;
    }
}
