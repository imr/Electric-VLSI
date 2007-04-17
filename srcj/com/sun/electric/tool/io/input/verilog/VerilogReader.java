package com.sun.electric.tool.io.input.verilog;

import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.geometry.Orientation;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.technology.technologies.Schematics;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.tool.user.ViewChanges;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.io.input.Input;

import java.net.URL;
import java.io.IOException;
import java.util.*;
import java.awt.geom.Point2D;

/**
 * Created by IntelliJ IDEA.
 * User: gg151869
 * Date: Oct 23, 2006
 * Time: 3:58:57 PM
 * To change this template use File | Settings | File Templates.
 */
public class VerilogReader extends Input
{
    List<NodeInst> transistors = new ArrayList<NodeInst>();
    double maxWidth = 100, nodeWidth = 10;
    double primitiveHeight = 0.5, primitiveWidth = 0.5;
    Map<Cell, Point2D.Double> locationMap = new HashMap<Cell, Point2D.Double>();
    PrimitiveNode essentialBounds = Generic.tech.findNodeProto("Essential-Bounds");
    Cell topCell = null;
    Map<String, NodeInst> pinsMap = new HashMap<String, NodeInst>();

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
            this.name = TextUtils.correctName(n);
        }
        static class PortInfo
        {
            String local;
            boolean isBus;
            PortProto ex;

            PortInfo(String local, boolean isBus, PortProto ex)
            {
                // Doesn't correct name if it is a bus
                this.local = (isBus) ? local : TextUtils.correctName(local);
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

    private void createInstance(Cell parent, VerilogData verilogData,
                                VerilogData.VerilogModule module, Cell icon, CellInstance info)
    {
        NodeInst cellInst = NodeInst.newInstance(icon, getNextLocation(parent), 10, 10, parent,
                        Orientation.IDENT, info.name, 0);

        List<String> localPorts = new ArrayList<String>();

        for (CellInstance.PortInfo port : info.list)
        {
            localPorts.clear();

            String portLocal = port.local;

            // It is unknown how many pins are coming in the stream
            if (portLocal.contains("{"))
            {
                StringTokenizer parse = new StringTokenizer(portLocal, "{,}", false); // extracting pins
                while (parse.hasMoreTokens())
                {
                    String name = parse.nextToken();
                    name = name.replaceAll(" ", "");
                    localPorts.add(name);
                }
            }
            else
                localPorts.add(portLocal);

            for (String s : localPorts)
            {
                NodeInst pin = pinsMap.get(s);

                if (pin == null)
                {
                    int index = s.indexOf("[");
                    if (index != -1)
                    {
                        s = s.substring(0, index);
                        pin = pinsMap.get(s);
                    }
                }

                if (pin == null)
                {
                    if (s.equals("vss")) // ground
                    {
                        pin = readSupply(parent, verilogData, module, false, s);
                    }
                    else
                    {
                        if (Job.getDebug())
                            System.out.println("Unknown signal " + s + " in cell " + parent.describe(false));
                        PrimitiveNode primitive = (port.isBus) ? Schematics.tech.busPinNode : Schematics.tech.wirePinNode;
                        pin = NodeInst.newInstance(primitive, getNextLocation(parent),
                                primitiveWidth, primitiveHeight,
//                                        primitive.getDefWidth(), primitive.getDefHeight(),
                                parent, Orientation.IDENT, null/*s*/, 0);
                        pinsMap.put(s, pin);
                    }
                }

//                ArcProto node = (port.isBus) ? Schematics.tech.bus_arc : Schematics.tech.wire_arc;
                ArcProto node = (pin.getProto() == Schematics.tech.busPinNode) ? Schematics.tech.bus_arc : Schematics.tech.wire_arc;
                PortInst ex = cellInst.findPortInst(port.ex.getName());
                ArcInst ai = ArcInst.makeInstance(node, 0.0 /*node.getDefaultLambdaFullWidth()*/,
                        pin.getOnlyPortInst(), ex, null, null, s);
                assert(ai != null);
                ai.setFixedAngle(false);
            }
        }
    }

    private NodeInst readSupply(Cell cell, VerilogData verilogData, VerilogData.VerilogModule module, boolean power, String name)
    {
        if (verilogData != null)
        {
            VerilogData.VerilogPort supply = module.addPort(name);
            supply.type = (power) ? PortCharacteristic.PWR : PortCharacteristic.GND;
        }
        else
        {
            PrimitiveNode np = (power) ? Schematics.tech.powerNode : Schematics.tech.groundNode;
            Point2D.Double p = getNextLocation(cell);
            double height = primitiveHeight; //np.getDefHeight();
            NodeInst supply = NodeInst.newInstance(np, p,
                    primitiveWidth, height,
                    cell, Orientation.IDENT, name, 0);
            // extra pin
            NodeInst ni = NodeInst.newInstance(Schematics.tech.wirePinNode, new Point2D.Double(p.getX(), p.getY()+height/2),
                    0.5, 0.5,
    //                Schematics.tech.wirePinNode.getDefWidth(), Schematics.tech.wirePinNode.getDefHeight(),
                    cell);

            ArcInst.makeInstance(Schematics.tech.wire_arc, 0.0 /*Schematics.tech.wire_arc.getDefaultLambdaFullWidth()*/,
                ni.getOnlyPortInst(), supply.getOnlyPortInst(), null, null, name);
            pinsMap.put(name, ni); // not sure if this is the correct pin
            return ni;
        }
        return null;
    }

    private CellInstance readInstance(Cell instance, VerilogData verilogData, VerilogData.VerilogModule module,
                                      VerilogData.VerilogModule element,
                                      boolean noMoreInfo) throws IOException
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
                assert(index > 0);
                String instanceName = line.substring(0, index);
                line = line.substring(index+1, line.length());
                StringTokenizer parse = new StringTokenizer(line, ")", false);
                exports.clear(); pins.clear();

                while (parse.hasMoreTokens())
                {
                    String value = parse.nextToken();
                    index = value.indexOf("."); // look for first .
                    int index2 = value.indexOf("("); // look for first (
                    if (index == -1) // end of tokens
                        continue; // or break?
                    assert(index2 != -1);
                    String n = value.substring(index+1, index2);
                    int index3 = n.indexOf("\\"); // those \ are a problem!
                    if (index3 != -1)
                        n = n.substring(index3+1);
                    exports.add(n);
                    pins.add(value.substring(index2+1));
                }

                // remove extra white spaces
                instanceName = instanceName.replaceAll(" ", "");
                CellInstance localCell = new CellInstance(instanceName);
                VerilogData.VerilogInstance verilogInst = null;

                if (verilogData != null)
                {
                    verilogInst = module.addInstance(instanceName, element);
                }

                for (int i = 0; i < exports.size(); i++)
                {
                    String export = exports.get(i);
                    String pin = pins.get(i);

                    pin = pin.replaceAll(" ", "");
                    export = export.replaceAll(" ", "");

                    if (verilogData != null)
                    {
                        VerilogData.VerilogPort exp = element.findPort(export);
                        // fixing original export if not found
                        if (exp == null)
                        {
//                            System.out.println("Warning: port " + export + " not found in module " + element.name + " yet");
                            exp = element.addPort(export);
                        }
                        verilogInst.addPortInstance(pin, exp);
                    }
                    else
                    {
                        String local = pin; // simple case .w(w)
                        int start = pin.indexOf("[");
                        boolean isBus = pin.contains("{");
                        String e = export;// nets.get(0);
                        PortProto ex = instance.findPortProto(e);

                        int dot = (!isBus) ? pin.indexOf(":") : -1; // I must skip this case
                        if (dot != -1)// && start < end)
                        {
    //                        bus = bus.substring(start+1, end);
                            local = local.substring(0, start); // case of .w(a[a:b])
                            isBus = true;
    //                        ex = instance.findPortProto(e+"[" + bus + "]");
                        }
                        if (ex == null && noMoreInfo) // ports in std cell are not available
                        {
                            PrimitiveNode primitive = (isBus) ? Schematics.tech.busPinNode : Schematics.tech.wirePinNode;
                            NodeInst ni = NodeInst.newInstance(primitive, getNextLocation(instance),
                                    primitiveWidth, primitiveHeight,
    //                                        primitive.getDefWidth(), primitive.getDefHeight(),
                                    instance, Orientation.IDENT, e, 0);
                            Export ex1 = Export.newInstance(instance, ni.getOnlyPortInst(), e);
                            ex = instance.findPortProto(e);
                            assert(ex1 == ex);
                            assert(ex != null);
                        }

                        if (ex != null)
                            localCell.addConnection(local, isBus, ex);
                    }
                }
                return localCell;
            }
        }
        // never reach this point
    }

    private String readWiresAndSupplies(Cell cell, VerilogData verilogData, VerilogData.VerilogModule module,
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
                    StringTokenizer p = new StringTokenizer(net, "\t ", false);
                    values.clear(); // clean reset
                    while (p.hasMoreTokens())
                    {
                        values.add(p.nextToken());
                    }
                    int size = values.size();
                    if (size == 0) continue;
                    assert(size == 1 || size == 2);
                    PrimitiveNode primitive = Schematics.tech.wirePinNode;
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
    //                        pinName += values.get(0);
                            primitive = Schematics.tech.busPinNode;
                        }
                        else
                            System.out.println(net + " is not a bus wire");
                    }
                    pinName = TextUtils.correctName(pinName);

                    if (verilogData != null)
                    {
                        // also considering [x:x]. Not doing the exception here as above
                        module.addWire(pinName, (values.size() == 2) ? values.get(0) : null);
                    }
                    else
                    {
                        NodeInst ni = NodeInst.newInstance(primitive, getNextLocation(cell),
                                primitiveWidth, primitiveHeight,
        //                        primitive.getDefWidth(), primitive.getDefHeight(),
                                cell, Orientation.IDENT, pinName, 0);
                        pinsMap.put(pinName, ni);
                        assert(ni != null);
                    }
                }
                else // supplies
                {
                    StringTokenizer p = new StringTokenizer(net, "\t ", false);
                    String name = p.nextToken();
                    name = TextUtils.correctName(name);
                    readSupply(cell, verilogData, module, power, name); // supply1 -> vdd, supply0 -> gnd or vss
                }
            }
        }
        // never reach this point
    }

    private void ignoreUntilEndOfStatement() throws IOException
    {
        String key = ";";

        for (;;)
        {
            String input = getRestOfLine();
            if (input.contains("begin"))
                key = "end";
            if (input.contains(key))
                return; // finish
        }
    }

    private String readInputOutput(Cell cell, VerilogData verilogData, VerilogData.VerilogModule module,
                                   PortCharacteristic portType) throws IOException
    {
        for (;;)
        {
            String input = getRestOfLine();
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
                PrimitiveNode primitive = Schematics.tech.wirePinNode;
                int size = l.size();
                if (size == 0) continue;
                assert(size == 1 || size == 2);
                String name = l.get(size - 1);
                if (l.size() == 2) // "input a[];"
                {
//                    name += l.get(0); busPin not longer containing [x:y]
                    primitive = Schematics.tech.busPinNode;
                }

                if (verilogData != null)
                {
                    VerilogData.VerilogPort export = module.findPort(name);
                    assert(export != null);
                    export.type = portType;
                    if (l.size() == 2)
                        export.busPins = l.get(0);
                }
                else
                {
                    //Point2D center, double width, double height, Cell parent)
                    NodeInst ni = NodeInst.newInstance(primitive, getNextLocation(cell),
                            primitiveWidth, primitiveHeight,
    //                        primitive.getDefWidth(), primitive.getDefHeight(),
                            cell, Orientation.IDENT, name, 0);
                    pinsMap.put(name, ni);
                    Export ex = Export.newInstance(cell, ni.getOnlyPortInst(), name);
                    ex.setCharacteristic(portType);
                }
            }
        }
        // never reach this point
    }

    private String readCell(VerilogData verilogData) throws IOException
    {
        List<String> inputs = new ArrayList<String>(10);
        readCellHeader(inputs);

        String cellName = inputs.get(0);
        VerilogData.VerilogModule module = null;
        Cell cell = null;
        Library lib = null;

        if (verilogData != null)
        {
            module = verilogData.getModule(cellName);
            if (module == null)
                module = verilogData.addModule(cellName);
            module.setValid(true);
            // adding ports in modules: from 1 -> inputs.size()-1;
            for (int i = 1; i < inputs.size(); i++)
                module.addPort(inputs.get(i));
        }
        else
        {
            cellName += "{" + View.SCHEMATIC.getAbbreviation() + "}";
            lib = Library.getCurrent();
            if (lib == null)
                lib = Library.newInstance("Verilog", null);

            cell = Cell.makeInstance(lib, cellName);
            cell.setTechnology(Schematics.tech);

            if (topCell == null)
                topCell = cell;
        }

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

            if (key.startsWith("/")) // comment
            {
                getRestOfLine();
                continue;
            }

            if (key.equals("endmodule"))
            {
                // done with this cell
                return null;
            }

            if (key.equals("wire"))
            {
                readWiresAndSupplies(cell, verilogData, module, true, false);
                continue;
            }
            if (key.startsWith("tri"))
                assert(false); // not implemented

            if (key.equals("input"))
            {
                readInputOutput(cell, verilogData, module, PortCharacteristic.IN);
                continue;
            }
            if (key.equals("output"))
            {
                readInputOutput(cell, verilogData, module, PortCharacteristic.OUT);
                continue;
            }
            if (key.equals("inout"))
            {
                readInputOutput(cell, verilogData, module, PortCharacteristic.BIDIR);
                continue;
            }

            if (key.startsWith("supply"))
            {
                boolean power = key.contains("supply1");
                readWiresAndSupplies(cell, verilogData, module, false, power);
                continue;
            }

            // ignoring some elements
            if (key.equals("assign") || key.startsWith("always") || key.startsWith("initial") || key.startsWith("reg"))
            {
                if (Job.getDebug())
                    System.out.println("Ignoring " + key);
                ignoreUntilEndOfStatement(); // either ; or end
                continue;
            }

            if (verilogData == null)
            {
                if (key.equals("tranif1")) // transistors
                {
                    // reading gates
                    //tranif1 nmos4p_0(gnd, gnd, vPlt); -> nmos
                    nextToken = readGate(cell, PrimitiveNode.Function.TRANMOS);
                    continue;
                }
                if (key.equals("tranif0")) // transistors
                {
                    // reading gates
                    //tranif1 nmos4p_0(gnd, gnd, vPlt); -> nmos
                    nextToken = readGate(cell, PrimitiveNode.Function.TRAPMOS);
                    continue;
                }
            }

            // reading cell instances
            VerilogData.VerilogModule element = null;
            Cell schematics = null;
            boolean noMoreInfo = false;

            if (verilogData != null)
            {
                element = verilogData.getModule(key);
                if (element == null) // it hasn't been created
                {
                    element = verilogData.addModule(key); // assuming latches and other elements are treat as subcells
                }
            }
            else
            {
                schematics = lib.findNodeProto(key);
                noMoreInfo = (schematics == null);

                if (noMoreInfo)
                {
                    String name = key + "{" + View.SCHEMATIC.getAbbreviation() + "}";
                    schematics = Cell.makeInstance(lib, name);
                    schematics.setTechnology(Schematics.tech);
                    // Adding essential bounds for now
                    NodeInst.makeInstance(essentialBounds, new Point2D.Double(10,10), 1, 1, schematics,
                            Orientation.IDENT, null, 0);
                    NodeInst.makeInstance(essentialBounds, new Point2D.Double(-10,-10), 1, 1, schematics,
                            Orientation.RR, null, 0);
                }
            }

            CellInstance info = readInstance(schematics, verilogData, module, element, noMoreInfo);
            if (verilogData == null)
            {
                Cell icon = schematics.iconView();
                if (icon == null) // creates one only after adding all missing ports
                {
                    ViewChanges.makeIconViewNoGUI(schematics, true, true);
                    icon = schematics.iconView();
                    assert(icon != null);
                }
                createInstance(cell, verilogData, module, icon, info);
            }
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
        double width = Schematics.tech.transistorNode.getDefWidth();
        double height = Schematics.tech.transistorNode.getDefHeight();
        Point2D p = getNextLocation(cell);
        NodeInst ni = NodeInst.newInstance(Schematics.tech.transistorNode, p, width, height,
                                       cell, orient, null /*gateName*/, 0);
        Schematics.tech.transistorNode.getTechnology().setPrimitiveFunction(ni, function);
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
            PrimitiveNode primitive = Schematics.tech.wirePinNode;
            ni = NodeInst.newInstance(primitive, new Point2D.Double(posX, posY),
                        primitiveWidth /*primitive.getDefWidth()*/, primitiveHeight /*primitive.getDefHeight()*/,
                        cell, Orientation.IDENT, null /*pinName*/, 0);

            ArcInst.makeInstance(Schematics.tech.wire_arc, 0.0 /*Schematics.tech.wire_arc.getDefaultLambdaFullWidth()*/,
                    ni.getOnlyPortInst(), ports[pos], null, null, name);
        }
        return null;
    }

    /**
	 * Method to import a Verilog file from disk.
	 * @param lib the library to ready
	 * @return true on error.
	 */
	protected boolean importALibrary(Library lib)
    {
        initKeywordParsing();
        VerilogData verilogData = parseVerilog(lib.getName(), true);
        buildCells(verilogData, lib);
        return verilogData == null;
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
        VerilogData verilogData = parseVerilog(verilogName, true);
        System.out.println("Verilog format " + verilogName + " read");
        return verilogData;
    }

    /**
     * Function to parse Verilog file without creating Electric objects.
     * @param file
     * @return VerilogData object
     */
    public VerilogData parseVerilog(String file)
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
        VerilogData verilogData = parseVerilog(file, true);
        System.out.println("Verilog file: " + file + " read");
        return verilogData;
    }

    public Cell readVerilog(String testName, String file, boolean newStrategy)
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
        setProgressNote("Reading Verilog file");
        VerilogData verilogData = parseVerilog(file, newStrategy);
        if (newStrategy)
        {
            Library library = Library.newInstance(testName, null);
            String topCellName = TextUtils.getFileNameWithoutExtension(fileURL);
            topCell = buildCells(verilogData, library);
            topCell = library.findNodeProto(topCellName);
        }
        return topCell; // still work because VerilogReader remembers the top cell
    }

    private VerilogData parseVerilog(String fileName, boolean newObjects)
    {
        VerilogData verilogData = null;
        if (newObjects) verilogData = new VerilogData(fileName);

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
                if (key.equals("module"))
                {
                    nextToken = readCell(verilogData);
                }
            }
        } catch (IOException e)
        {
            System.out.println("ERROR reading Verilog file");
        }

        // Simplify wires?: a[1], a[2], a[3] -> a[1:3]
        if (newObjects) verilogData.simplifyWires();
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
     */
    private Cell buildCells(VerilogData verilogCell, Library lib)
    {
        Cell topCell = null; // assumes the first module in the list is the top cell
        for (VerilogData.VerilogModule module : verilogCell.getModules())
        {
            Cell cell = buildCellFromModule(module, lib);
            if (topCell == null)
                topCell = cell;
        }
        return topCell;
    }

    /**
     * Function to build cell fro a VerilogModule object
     * @param lib
     * @return Cell object representing this module
     */
    private Cell buildCellFromModule(VerilogData.VerilogModule module, Library lib)
    {
        String cellName = module.name + "{" + View.SCHEMATIC.getAbbreviation() + "}";
        Cell cell = lib.findNodeProto(cellName);
        if (cell != null) return cell; // already created;

        cell = Cell.makeInstance(lib, cellName);
        cell.setTechnology(Schematics.tech);
        // Adding essential bounds for now
        NodeInst.makeInstance(essentialBounds, new Point2D.Double(10,10), 1, 1, cell,
                Orientation.IDENT, null, 0);
        NodeInst.makeInstance(essentialBounds, new Point2D.Double(-10,-10), 1, 1, cell,
                Orientation.RR, null, 0);

        // wires first to determine which pins are busses or simple pins
        for (VerilogData.VerilogWire wire : module.wires)
        {
            String pinName = wire.name;
            NodeInst ni = cell.findNode(pinName);

            if (ni == null)
            {
                PrimitiveNode primitive = (!wire.isBusWire()) ? Schematics.tech.wirePinNode :
                            Schematics.tech.busPinNode;
                ni = NodeInst.newInstance(primitive, getNextLocation(cell),
                                    primitiveWidth, primitiveHeight,
            //                        primitive.getDefWidth(), primitive.getDefHeight(),
                                    cell, Orientation.IDENT, pinName, 0);
            }
            else
                System.out.println("Wire " + pinName + " exists");

            if (ni == null)
            assert(ni != null);
//            pinsMap.put(pinName, ni);
        }

        // inputs/outputs/inouts/supplies
        for (VerilogData.VerilogPort port : module.ports.values())
        {
            //Point2D center, double width, double height, Cell parent)
            String name = port.name;
            PortCharacteristic portType = port.type;

            // input/output/inout
            if (portType == PortCharacteristic.BIDIR ||
                    portType == PortCharacteristic.IN ||
                    portType == PortCharacteristic.OUT ||
                    portType == PortCharacteristic.CLK ||
                    portType == PortCharacteristic.UNKNOWN) // unknown when modules are read as instances
            {
                PrimitiveNode primitive = (port.busPins==null) ? Schematics.tech.wirePinNode :
                        Schematics.tech.busPinNode;
                NodeInst ni = NodeInst.newInstance(primitive, getNextLocation(cell),
                        primitiveWidth, primitiveHeight,
//                        primitive.getDefWidth(), primitive.getDefHeight(),
                        cell, Orientation.IDENT, name, 0);
//                    pinsMap.put(name, ni);
                Export ex = Export.newInstance(cell, ni.getOnlyPortInst(), name);
                ex.setCharacteristic(portType);
            }
            else if (portType == PortCharacteristic.PWR ||
                    portType == PortCharacteristic.GND)
            {
                boolean power = portType == PortCharacteristic.PWR;
                PrimitiveNode np = (power) ? Schematics.tech.powerNode : Schematics.tech.groundNode;
                Point2D.Double p = getNextLocation(cell);
                double height = primitiveHeight; //np.getDefHeight();
                NodeInst supply = NodeInst.newInstance(np, p,
                        primitiveWidth, height,
                        cell, Orientation.IDENT, name, 0);
                // extra pin
                NodeInst ni = NodeInst.newInstance(Schematics.tech.wirePinNode, new Point2D.Double(p.getX(), p.getY()+height/2),
                        0.5, 0.5,
        //                Schematics.tech.wirePinNode.getDefWidth(), Schematics.tech.wirePinNode.getDefHeight(),
                        cell);

                ArcInst.makeInstance(Schematics.tech.wire_arc, 0.0 /*Schematics.tech.wire_arc.getDefaultLambdaFullWidth()*/,
                    ni.getOnlyPortInst(), supply.getOnlyPortInst(), null, null, name);
//                    pinsMap.put(name, ni); // not sure if this is the correct pin
            }
            else
                System.out.println("Skipping this characteristic?");
//                    assert(false); // it should not reach this point.
        }

        // instances
        for (VerilogData.VerilogInstance inst : module.instances)
        {
            buildNodeInstFromModule(inst, lib, cell);
        }

        // making icon
        ViewChanges.makeIconViewNoGUI(cell, true, true);

        return cell; // not too much sense?
    }

    /**
     * Function to build a NodeInst object from a VerilogInstance object
     * @param inst
     * @param lib
     * @param parent
     */
    Cell buildNodeInstFromModule(VerilogData.VerilogInstance inst, Library lib, Cell parent)
    {
        Cell schematics = buildCellFromModule(inst.element, lib);
        Cell icon = schematics.iconView();
        if (icon == null)
        assert(icon != null);

        NodeInst cellInst = NodeInst.newInstance(icon, getNextLocation(parent), 10, 10, parent,
                Orientation.IDENT, inst.name, 0);

        List<String> localPorts = new ArrayList<String>();
        for (VerilogData.VerilogPortInst port : inst.ports)
        {
            String portLocal = port.name;
            localPorts.clear();

             // It is unknown how many pins are coming in the stream
            if (portLocal.contains("{"))
            {
                StringTokenizer parse = new StringTokenizer(portLocal, "{,}", false); // extracting pins
                while (parse.hasMoreTokens())
                {
                    String name = parse.nextToken();
                    name = name.replaceAll(" ", "");
                    localPorts.add(name);
                }
            }
            else
                localPorts.add(portLocal);

            for (String s : localPorts)
            {
                NodeInst pin = parent.findNode(s);  // not sure if this should be done per cell or ask

                if (pin == null)
                {
                        int index = s.indexOf("[");
                        if (index != -1)
                        {
                            s = s.substring(0, index);
                            pin = parent.findNode(s);//pinsMap.get(s);
                        }
                }
                if (pin == null)
                {
                    // Still missing vss code?
                     if (Job.getDebug())
                            System.out.println("Unknown signal " + s + " in cell " + parent.describe(false));
                        PrimitiveNode primitive = (port.port.busPins!=null) ? Schematics.tech.busPinNode : Schematics.tech.wirePinNode;
                        pin = NodeInst.newInstance(primitive, getNextLocation(parent),
                                primitiveWidth, primitiveHeight,
                                parent, Orientation.IDENT, /*null*/s, 0);  // not sure why it has to be null?
                }

                ArcProto node = (pin.getProto() == Schematics.tech.busPinNode) ? Schematics.tech.bus_arc : Schematics.tech.wire_arc;
                PortInst ex = cellInst.findPortInst(port.port.name);
                ArcInst ai = ArcInst.makeInstance(node, 0.0 /*node.getDefaultLambdaFullWidth()*/,
                        pin.getOnlyPortInst(), ex, null, null, s);
                if (ai == null)
                    assert(ai != null);
                ai.setFixedAngle(false);
            }
        }
        return schematics;
    }
}
