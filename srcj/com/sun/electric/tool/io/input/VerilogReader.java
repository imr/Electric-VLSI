package com.sun.electric.tool.io.input;

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
import com.sun.electric.technology.technologies.Schematics;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.tool.user.ViewChanges;

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
                this.local = TextUtils.correctName(local);
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

    private void createInstance(Cell parent, Cell icon, CellInstance info)
    {
        NodeInst cellInst = NodeInst.newInstance(icon, getNextLocation(parent), 10, 10, parent,
                        Orientation.IDENT, info.name, 0);

        for (CellInstance.PortInfo port : info.list)
        {
            if (port.local.contains("{"))
                continue; // skipping this case for now.

            NodeInst pin = pinsMap.get(port.local);

            if (pin == null)
            {
                StringTokenizer parse = new StringTokenizer(port.local, "[]", false); // extracting only input name
                String busName = parse.nextToken(); // only bus name, root

                pin = pinsMap.get(busName);

                if (pin == null)
                {
                    if (busName.equals("vss")) // ground
                    {
                        pin = addSupply(parent, false, busName);
                    }
                    else
                    {
                        System.out.println("Unknown signal " + busName + " in cell " + parent.describe(false));
                        continue; // temporary
                    }
                }
            }

            ArcProto node = (port.isBus) ? Schematics.tech.bus_arc : Schematics.tech.wire_arc;
            PortInst ex = cellInst.findPortInst(port.ex.getName());
            ArcInst ai = ArcInst.makeInstance(node, 0.0 /*node.getDefaultLambdaFullWidth()*/,
                    pin.getOnlyPortInst(), ex, null, null, port.local);
            if (ai == null)
                assert(ai != null);
            ai.setFixedAngle(false);
        }
    }

    private NodeInst addSupply(Cell cell, boolean power, String name)
    {
        PrimitiveNode np = (power) ? Schematics.tech.powerNode : Schematics.tech.groundNode;

        Point2D.Double p = getNextLocation(cell);
        double height = primitiveHeight; //np.getDefHeight();

//        System.out.println("addSupply " + height);

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

    private CellInstance readInstance(Cell instance, boolean noMoreInfo) throws IOException
    {
//        List<StringBuffer> inputs = new ArrayList<StringBuffer>(2);
//        int pos = 0;
//        inputs.add(new StringBuffer()); // adding slot for the name
        StringBuffer signature = new StringBuffer();
//        List<String> nets = new ArrayList<String>();
//        List<String> list = new ArrayList<String>();
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
//                list.clear();
                exports.clear(); pins.clear();

                while (parse.hasMoreTokens())
                {
                    String value = parse.nextToken();
                    index = value.indexOf("."); // look for first .
                    int index2 = value.indexOf("("); // look for first (
                    if (index == -1) // end of tokens
                        continue; // or break?
                    assert(index2 != -1);
                    exports.add(value.substring(index+1, index2));
                    pins.add(value.substring(index2+1));
//                    if (value.startsWith("(") || value.startsWith(")"))
//                        continue;
//                    list.add(value);
                }

                // remove extra white spaces
                instanceName = instanceName.replaceAll(" ", "");
                CellInstance localCell = new CellInstance(instanceName);

                for (int i = 0; i < exports.size(); i++)
                {
                    String export = exports.get(i);
                    String pin = pins.get(i);
//                    parse = new StringTokenizer(s, "(){};", false);
//                    nets.clear();
//                    while (parse.hasMoreTokens())
//                    {
//                        String value = parse.nextToken();
//                        StringTokenizer t = new StringTokenizer(value, " .\t\\", false);
//                        if (!t.hasMoreTokens()) continue;
//                        value = t.nextToken();
//                        nets.add(value);
//                    }

//                    if (nets.size() != 2)
//                    {
//                    assert(nets.size()==2);
//                        System.out.println("Problem reading this instance " + instanceName);
//                        return localCell;
//                    }

                    pin = pin.replaceAll(" ", "");
                    export = export.replaceAll(" ", "");
//                    String bus = pin; // nets.get(1);
                    String local = pin; // simple case .w(w)
                    int start = pin.indexOf("[");
//                    int end = bus.indexOf("]");
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
                    if (ex == null && noMoreInfo) // exports in std cell are not available
                    {
                        PrimitiveNode primitive = (isBus) ? Schematics.tech.busPinNode : Schematics.tech.wirePinNode;
                        NodeInst ni = NodeInst.newInstance(primitive, getNextLocation(instance),
                                primitiveWidth, primitiveHeight,
//                                        primitive.getDefWidth(), primitive.getDefHeight(),
                                instance, Orientation.IDENT, e, 0);
                        Export.newInstance(instance, ni.getOnlyPortInst(), e);
                        ex = instance.findPortProto(e);
                        assert(ex != null);
                    }

                    if (ex != null)
                        localCell.addConnection(local, isBus, ex);
                }
                return localCell;
            }

//            StringTokenizer parse = new StringTokenizer(key, ".; \t", true);
//            // .out ( \cl_u1_nor3_4x_2.out ) )
//            while (parse.hasMoreTokens())
//            {
//                String value = parse.nextToken();
//
//                if (value.equals("."))
//                {
//                    pos++; // new input start
//                    inputs.add(new StringBuffer());
//                    continue;
//                }
//                if (value.equals(";")) // done with header
//                {
//                    String name = inputs.get(0).toString(); // in pos==0 then instance name
//                    StringTokenizer p = new StringTokenizer(name, "(\t ", false);
//                    name = p.nextToken(); // remove extra ( and white spaces
//                    CellInstance localCell = new CellInstance(name);
//
//                    // matching export
//                    for (int i = 1; i < inputs.size(); i++)
//                    {
//                        StringBuffer s = inputs.get(i);
//                        p = new StringTokenizer(s.toString(), "(){}, \t", false);
//                        nets.clear();
//                        while (p.hasMoreTokens())
//                        {
//                            nets.add(p.nextToken());
//                        }
//                        // nets.get(0) is the export name
//                        if (nets.size() == 3 && nets.get(2).startsWith("[")) // special case where local rootname is not consecutive with bus number
//                        {
//                            String n = nets.get(1) + nets.get(2);
//                            nets.remove(2); nets.remove(1);
//                            nets.add(n);
//                        }
//                        if (Job.getDebug() && (nets.size() < 2 || nets.size() > 2))
//                            System.out.println("Error here: less than 2 nets!");
////                        assert(nets.size() > 1);
//                        String local = nets.get(1); // simple case .w(w)
//                        String e = nets.get(0);
//                        boolean isBus = false;
//                        PortProto ex = instance.findPortProto(e);
//
//                        if (nets.size() > 2) // checking bus export
//                        {
//                            String rootName = null;
//                            int firstPin = -1, lastPin = -1;
//
//                            for (int j = 1; j < nets.size();  j++)
//                            {
//                                String pin = nets.get(j);
//                                int start = pin.indexOf("[");
//                                if (start == -1)
//                                    break; // no bus case
//                                int end = pin.indexOf("]");
//                                assert(end != -1);
//                                String root = pin.substring(0, start);
//                                if (rootName == null) // first time
//                                    rootName = root;
//                                else if (!rootName.equals(root))
//                                {
//                                    rootName = null;
//                                    break; // no the same root name
//                                }
//
//                                int pinNum = 0;
//
//                                try {
//                                    pinNum = Integer.parseInt(pin.substring(start+1, end));
//                                }
//                                catch (Exception exc)
//                                {
//                                    if (Job.getDebug())
//                                    System.out.println("Wrong pin detected "+ pin + " " + pin.substring(start+1, end));
//                                    pinNum = Integer.parseInt(pin.substring(start+1, start+2));
////                                    exc.printStackTrace();
//                                }
//
//                                if (firstPin == -1)
//                                {
//                                    firstPin = pinNum;
//                                    lastPin = firstPin;
//                                }
//                                else if (pinNum != (lastPin+1))
//                                {
//                                    // only consecutive numbers
//                                    rootName = null;
//                                    break;
//                                }
//                                else
//                                    lastPin = pinNum;
//                            }
//                            if (rootName != null)
//                            {
//                                isBus = true;
//                                String extra = "[" + firstPin + ":" + lastPin + "]";
//                                ex = instance.findPortProto(e+extra);
//                                local = rootName + extra;
//                                if (ex == null) // try the inverse. This is tricky
//                                {
//                                    extra = "[" + lastPin + ":" + firstPin + "]";
//                                    ex = instance.findPortProto(e+extra);
//                                }
//                            }
//                        }
//                        else if (nets.size() == 2) // simple wire or bus case
//                        {
//                            String bus = nets.get(1);
//                            int dot = bus.indexOf(":");
//                            local = bus;
//                            int start = bus.indexOf("[");
//                            int end = bus.indexOf("]");
//                            if (dot != -1 && start < end)
//                            {
//                                bus = bus.substring(start+1, end);
//                                isBus = true;
//                                ex = instance.findPortProto(e+"[" + bus + "]");
//                            }
//                            if (ex == null && noMoreInfo) // exports in std cell are not available
//                            {
//                                PrimitiveNode primitive = Schematics.tech.wirePinNode;
//                                NodeInst ni = NodeInst.newInstance(primitive, getNextLocation(instance),
//                                        primitiveWidth, primitiveHeight,
////                                        primitive.getDefWidth(), primitive.getDefHeight(),
//                                        instance, Orientation.IDENT, e, 0);
//                                Export.newInstance(instance, ni.getOnlyPortInst(), e);
//                                ex = instance.findPortProto(e);
//                                assert(ex != null);
//                            }
//                        }
//                        if (ex != null)
//                            localCell.addConnection(local, isBus, ex);
//                    }
//                    return localCell;
//                }
//                // It could be done by StringBuffer
//                StringBuffer val = inputs.get(pos);
//                assert (val != null);
//                val.append(value);
//            }
        }
        // never reach this point
    }

    private String readWires(Cell cell) throws IOException
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

                NodeInst ni = NodeInst.newInstance(primitive, getNextLocation(cell),
                        primitiveWidth, primitiveHeight,
//                        primitive.getDefWidth(), primitive.getDefHeight(),
                        cell, Orientation.IDENT, pinName, 0);
                pinsMap.put(pinName, ni);
                assert(ni != null);
            }

        }
        // never reach this point
    }

    private String readInputOutput(Cell cell) throws IOException
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
                //Point2D center, double width, double height, Cell parent)
                NodeInst ni = NodeInst.newInstance(primitive, getNextLocation(cell),
                        primitiveWidth, primitiveHeight,
//                        primitive.getDefWidth(), primitive.getDefHeight(),
                        cell, Orientation.IDENT, name, 0);
                pinsMap.put(name, ni);
                Export.newInstance(cell, ni.getOnlyPortInst(), name);
            }
        }
        // never reach this point
    }

    private String readCell() throws IOException
    {
        List<String> inputs = new ArrayList<String>(10);
        readCellHeader(inputs);

        String cellName = inputs.get(0);
        cellName += "{" + View.SCHEMATIC.getAbbreviation() + "}";
        Library lib = Library.getCurrent();
        if (lib == null)
            lib = Library.newInstance("Verilog", null);

        Cell cell = Cell.makeInstance(lib, cellName);
        cell.setTechnology(Schematics.tech);

        if (topCell == null)
            topCell = cell;

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

            if (key.equals("assign"))
            {
                getRestOfLine(); // ignoring for now
                continue;
            }

            if (key.equals("wire"))
            {
                readWires(cell);
                continue;
            }

            if (key.equals("input") || key.equals("output") || key.equals("inout"))
            {
                readInputOutput(cell);
                continue;
            }

            if (key.startsWith("supply"))
            {
                boolean power = key.contains("supply1");
                key = getRestOfLine();
                StringTokenizer parse = new StringTokenizer(key, " ;\t", false); // extracting only input name
//                assert(parse.hasMoreTokens());
                String name = parse.nextToken();
                addSupply(cell, power, name); // supply1 -> vdd, supply0 -> gnd or vss
                continue;
            }

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

            // reading cell instances
            Cell schematics = lib.findNodeProto(key);
            boolean noMoreInfo = (schematics == null);

            if (noMoreInfo)
            {
                String name = key;
                name += "{" + View.SCHEMATIC.getAbbreviation() + "}";
                schematics = Cell.makeInstance(lib, name);
                schematics.setTechnology(Schematics.tech);
                // Adding essential bounds for now
                NodeInst.makeInstance(essentialBounds, new Point2D.Double(10,10), 1, 1, schematics,
                        Orientation.IDENT, null, 0);
                NodeInst.makeInstance(essentialBounds, new Point2D.Double(-10,-10), 1, 1, schematics,
                        Orientation.RR, null, 0);
            }

            CellInstance info = readInstance(schematics, noMoreInfo);
            Cell icon = schematics.iconView();
            if (icon == null) // creates one only after adding all missing ports
            {
                ViewChanges.makeIconViewNoGUI(schematics, true, true);
                icon = schematics.iconView();
                assert(icon != null);
            }
            createInstance(cell, icon, info);
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

    public Cell readVerilog(String file)
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
                    nextToken = readCell();
                }
            }
        } catch (IOException e)
        {
            System.out.println("ERROR reading Dais technology file");
        }
        return topCell;
    }
}
