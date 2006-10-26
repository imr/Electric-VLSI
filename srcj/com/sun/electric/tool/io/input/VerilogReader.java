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
import java.util.List;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.Iterator;
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

    private String readCell() throws IOException
    {
        List<String> inputs = new ArrayList<String>(10);
        String key = readCellHeader(inputs);
        String cellName = inputs.get(0);
        cellName += "{" + View.SCHEMATIC.getAbbreviation() + "}";
        Cell cell = Cell.makeInstance(Library.getCurrent(), cellName);
        cell.setTechnology(Schematics.tech);
//        cell.setView(View.SCHEMATIC);

        String nextToken = null;

        for (;;)
        {
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
            if (key.equals("input"))
            {
                String input = getRestOfLine();
                StringTokenizer parse = new StringTokenizer(input, "; ", false); // extracting only input name
                List<String> l = new ArrayList<String>(2);
                while (parse.hasMoreTokens())
                {
                    String name = parse.nextToken();
                    l.add(name); // it could be "input a;" or "input [9:0] a;"
                }
                String name;
                String extra = "";
                PrimitiveNode primitive = Schematics.tech.wirePinNode;
                int size = l.size();
                assert(size == 1 || size == 2);
                if (l.size() == 1) // "input a;"
                    name = l.get(0);
                else
                {
                    name = l.get(1);
                    extra = l.get(0);
                    primitive = Schematics.tech.busPinNode;
                }
                assert (inputs.contains(name));

                name += extra;
                //Point2D center, double width, double height, Cell parent)
                NodeInst ni = NodeInst.newInstance(primitive, new Point2D.Double(0, 0),
                        primitive.getDefWidth(), primitive.getDefHeight(),
                        cell, Orientation.fromAngle(0), name, 0);
                Export e = Export.newInstance(cell, ni.getOnlyPortInst(), name);
                continue;
            }

            if (key.startsWith("supply"))
            {
                key = getAKeyword();
                StringTokenizer parse = new StringTokenizer(key, ";", false); // extracting only input name
                assert(parse.hasMoreTokens());
                String name = parse.nextToken();
                Orientation orient = Orientation.fromAngle(0);
                PrimitiveNode np = (name.equals("vdd")) ? Schematics.tech.powerNode : Schematics.tech.groundNode;

                NodeInst.newInstance(np, new Point2D.Double(0, 0),
                        np.getDefWidth(), np.getDefHeight(),
                        cell, orient, name, 0);
                continue;
            }

            if (key.startsWith("tranif")) // transistors
            {
                // reading instances
                //tranif1 nmos4p_0(gnd, gnd, vPlt);
                nextToken = readInstance(cell);
            }
        }
        // not reaching this point.
    }

    private String readInstance(Cell cell)
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
        NodeInst ni = NodeInst.newInstance(Schematics.tech.transistorNode, new Point2D.Double(0, 0),
                Schematics.tech.transistorNode.getDefWidth(), Schematics.tech.transistorNode.getDefHeight(),
                                       cell, orient, list.get(0), 0);
        Schematics.tech.transistorNode.getTechnology().setPrimitiveFunction(ni, PrimitiveNode.Function.TRANMOS);
        List<PortInst> ports = new ArrayList<PortInst>(3);

        for (Iterator<PortInst> it = ni.getPortInsts(); it.hasNext();)
        {
            ports.add(it.next());
        }
        assert(ports.size() == 3);

        for (int i = 1; i < list.size(); i++)
        {
            String name = list.get(i);
            NodeInst pin = cell.findNode(name);
            ArcInst.makeInstance(Schematics.tech.wire_arc, Schematics.tech.wire_arc.getDefaultWidth(),
                    pin.getOnlyPortInst(), ports.get(i-1));
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
