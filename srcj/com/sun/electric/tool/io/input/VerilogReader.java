package com.sun.electric.tool.io.input;

import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.technology.technologies.Schematics;

import java.net.URL;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.StringTokenizer;

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
        Cell cell = Cell.makeInstance(Library.getCurrent(), cellName);
        cell.setTechnology(Schematics.tech);
        cell.setView(View.SCHEMATIC);

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
                String input = getAKeyword();
                StringTokenizer parse = new StringTokenizer(input, ";", false); // extracting only input name
                assert(parse.hasMoreTokens());
                String name = parse.nextToken();
                assert (inputs.contains(name));

                //Point2D center, double width, double height, Cell parent)
//                NodeInst ni = NodeInst.newInstance(cell, Schematics.tech.busPinNode, cell);
//                Export e = Export.newInstance(cell, );
                continue;
            }

            if (key.startsWith("supply"))
            {

            }

            if (key.startsWith("tranif1"))
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
        StringTokenizer parse = new StringTokenizer(input, "(; )", false); // extracting only input name
        List<String> list = new ArrayList<String>(2);

        while (parse.hasMoreTokens())
        {
            String value = parse.nextToken();
            list.add(value) ;
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
