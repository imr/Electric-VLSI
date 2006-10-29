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
import com.sun.electric.technology.technologies.Schematics;
import com.sun.electric.technology.PrimitiveNode;

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
    Map<Cell, Point2D.Double> locationMap = new HashMap<Cell, Point2D.Double>();

    private String readCellHeader(List<String> inputs) throws IOException
    {
        for (;;)
        {
            String key = getAKeyword();
            StringTokenizer parse = new StringTokenizer(key, "( ),", false);
            while (parse.hasMoreTokens())
            {
                String value = parse.nextToken();
                if (value.equals(";")) // done with header
                    return null;
                inputs.add(value);
            }
        }
    }

    private String readInstance(Cell cell, Cell instance, List<NodeInst> wires) throws IOException
    {
        List<StringBuffer> inputs = new ArrayList<StringBuffer>(2);
        int pos = 0;
        inputs.add(new StringBuffer()); // adding slot for the name

        for (;;)
        {
            String key = getRestOfLine();

            StringTokenizer parse = new StringTokenizer(key, ".;", true);
            while (parse.hasMoreTokens())
            {
                String value = parse.nextToken();
                if (value.equals("."))
                {
                    pos++; // new input start
                    inputs.add(new StringBuffer());
                    continue;
                }
                if (value.equals(";")) // done with header
                {
                    String name = inputs.get(0).toString(); // in pos==0 then instance name
                    NodeInst cellInst = NodeInst.newInstance(instance, new Point2D.Double(0, 0), 10, 10, cell,
                            Orientation.IDENT, name, 0);
                    // matching export
                    for (int i = 1; i < inputs.size(); i++)
                    {
                        StringBuffer s = inputs.get(i);
                        StringTokenizer p = new StringTokenizer(s.toString(), "(){},  ", false);
                        List<String> nets = new ArrayList<String>();
                        while (p.hasMoreTokens())
                        {
                            nets.add(p.nextToken());
                        }
                        // nets.get(0) is the export name
                        assert(nets.size() > 1);
                        String local = nets.get(1);
                        String e = nets.get(0);
                        if (nets.size() == 2) // experiment
                        {
                            String bus = nets.get(1);
                            local = bus;
                            int start = bus.indexOf("[");
                            int end = bus.indexOf("]");
                            if (start < end)
                            {
                                bus = bus.substring(start+1, end);
                                e += "[" + bus + "]";
                            }
                            assert(bus != null);

                        }
                        PortInst ex = null;

                        for (Iterator<PortInst> pIt = cellInst.getPortInsts(); pIt.hasNext(); )
                        {
                            PortInst pi = pIt.next();
                            if (pi.getPortProto().getName().equals(e))
                            {
                                // found
                                ex = pi;
                                break;
                            }
                        }
                        if (ex != null)
                        {
                            NodeInst pin = cell.findNode(local);
                            if (pin != null)
                            {
                                ArcInst ai = ArcInst.makeInstance(Schematics.tech.wire_arc, Schematics.tech.wire_arc.getDefaultWidth(),
                    pin.getOnlyPortInst(), ex, null, null, local);
                                assert(ai != null);
                            }

                        }
                    }
                    return null;
                }
                // It could be done by StringBuffer
                StringBuffer val = inputs.get(pos);
                assert (val != null);
                val.append(value);
            }
        }
        // never reach this point
    }

    private String getWires(Cell cell, List<NodeInst> wires) throws IOException
    {
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
                StringTokenizer p = new StringTokenizer(net, " ", false);
                List<String> values = new ArrayList<String>(2);
                while (p.hasMoreTokens())
                {
                    values.add(p.nextToken());
                }
                int size = values.size();
                assert(size == 1 || size == 2);
                PrimitiveNode primitive = Schematics.tech.wirePinNode;
                String pinName = values.get(size-1);
                if (values.size() == 2)
                {
                    pinName += values.get(0);
                }
                NodeInst ni = NodeInst.newInstance(primitive, getNextLocation(cell),
                        primitive.getDefWidth(), primitive.getDefHeight(),
                        cell, Orientation.IDENT, pinName, 0);
                wires.add(ni);
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
        Cell cell = Cell.makeInstance(Library.getCurrent(), cellName);
        cell.setTechnology(Schematics.tech);
        List<NodeInst> wires = new ArrayList<NodeInst>();

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

            if (key.equals("endmodule"))
            {
                // done with this cell
                return null;
            }
            if (key.equals("wire"))
            {
                getWires(cell, wires);
                continue;
            }
            if (key.equals("input") || key.equals("output"))
            {
                String input = getRestOfLine();
                StringTokenizer parse = new StringTokenizer(input, "; ", false); // extracting only input name
                List<String> l = new ArrayList<String>(2);
                while (parse.hasMoreTokens())
                {
                    String name = parse.nextToken();
                    l.add(name); // it could be "input a;" or "input [9:0] a;"
                }
                PrimitiveNode primitive = Schematics.tech.wirePinNode;
                int size = l.size();
                assert(size == 1 || size == 2);
                String name = l.get(size - 1);
                if (l.size() == 2) // "input a[];"
                {
                    name += l.get(0);
                    primitive = Schematics.tech.busPinNode;
                }
//                assert (inputs.contains(name));

                //Point2D center, double width, double height, Cell parent)
                NodeInst ni = NodeInst.newInstance(primitive, getNextLocation(cell),
                        primitive.getDefWidth(), primitive.getDefHeight(),
                        cell, Orientation.IDENT, name, 0);
                Export.newInstance(cell, ni.getOnlyPortInst(), name);
                continue;
            }

            if (key.startsWith("supply"))
            {
                key = getAKeyword();
                StringTokenizer parse = new StringTokenizer(key, ";", false); // extracting only input name
                assert(parse.hasMoreTokens());
                String name = parse.nextToken();
                PrimitiveNode np = (name.equals("vdd")) ? Schematics.tech.powerNode : Schematics.tech.groundNode;

                Point2D.Double p = getNextLocation(cell);
                double height = np.getDefHeight();
                NodeInst supply = NodeInst.newInstance(np, p,
                        np.getDefWidth(), height,
                        cell, Orientation.IDENT, name, 0);
                // extra pin
                NodeInst ni = NodeInst.newInstance(Schematics.tech.wirePinNode, new Point2D.Double(p.getX(), p.getY()+height/2),
                        Schematics.tech.wirePinNode.getDefWidth(), Schematics.tech.wirePinNode.getDefHeight(), cell);

                ArcInst.makeInstance(Schematics.tech.wire_arc, Schematics.tech.wire_arc.getDefaultWidth(),
                    ni.getOnlyPortInst(), supply.getOnlyPortInst(), null, null, name);
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
            Cell instance = Library.getCurrent().findNodeProto(key);
            nextToken = readInstance(cell, instance, wires);
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
        StringTokenizer parse = new StringTokenizer(input, "(;, )", false); // extracting only input name
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
                        primitive.getDefWidth(), primitive.getDefHeight(),
                        cell, Orientation.IDENT, null /*pinName*/, 0);
            ArcInst.makeInstance(Schematics.tech.wire_arc, Schematics.tech.wire_arc.getDefaultWidth(),
                    ni.getOnlyPortInst(), ports[pos], null, null, name);
        }
        return null;
    }

    public void readVerilog(String file)
    {
        URL fileURL = TextUtils.makeURLToFile(file);
        if (openTextInput(fileURL))
        {
            System.out.println("Cannot open the Verilog file: " + file);
            return;
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
    }
}
