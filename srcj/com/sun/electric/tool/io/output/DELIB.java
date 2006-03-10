package com.sun.electric.tool.io.output;

import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.Cell;

import java.io.*;

/**
 * Created by IntelliJ IDEA.
 * User: gainsley
 * Date: Mar 8, 2006
 * Time: 5:00:09 PM
 * To change this template use File | Settings | File Templates.
 */
public class DELIB extends JELIB {

    DELIB() {}

    private String headerFile;

    /**
     * Write a cell. Instead of writing it to the jelib file,
     * write a reference to an external file, and write the contents there
     * @param cell
     */
    protected void writeCell(Cell cell) {
        String cellDir = cell.getName();
        File cellFD = new File(filePath + File.separator + cellDir);
        if (cellFD.exists()) {
            if (!cellFD.isDirectory()) {
                System.out.println("Error, file "+cellFD+" is not a directory, moving it to "+cellDir+".old");
                if (!cellFD.renameTo(new File(cellDir+".old"))) {
                    System.out.println("Error, unable to rename file "+cellFD+" to "+cellDir+".old, skipping cell "+cell.describe(false));
                    return;
                }
            }
        } else {
            // create the directory
            if (!cellFD.mkdir()) {
                System.out.println("Failed to make directory: "+cellFD+", skipping cell "+cell.describe(false));
                return;
            }
        }

        // create cell file in directory
        String cellFile = cellDir + File.separator + cell.getName() + "." + cell.getView().getAbbreviation();
        String cellFileAbs = filePath + File.separator + cellFile;
        // save old printWriter
        PrintWriter headerWriter = printWriter;
        // set current print writer to cell file
        try {
            printWriter = new PrintWriter(new BufferedWriter(new FileWriter(cellFileAbs)));
        } catch (IOException e) {
            System.out.println("Error opening "+cellFileAbs+", skipping cell: "+e.getMessage());
            printWriter = headerWriter;
            return;
        }

        // write out the cell into the new file
        super.writeCell(cell);

        printWriter.close();
        // set the print writer back
        printWriter = headerWriter;
        printWriter.println("C"+cellFile);
    }

    /**
     * Open the printWriter for writing text files
     * @return true on error.
     */
    protected boolean openTextOutputStream(String filePath) {
        // first, create a directory for the library
        File f = new File(filePath);
        this.filePath = filePath;
        if (f.exists()) {
            if (!f.isDirectory()) {
                // not a directory, issue error
                System.out.println("Error, file "+f+" is not a directory");
                return true;
            }
        } else {
            // create a directory
            if (!f.mkdir()) {
                System.out.println("Failed to make directory: "+f);
                return true;
            }
        }
        headerFile = filePath + File.separator + "header";
        // open new printWriter for cell
        try {
            printWriter = new PrintWriter(new BufferedWriter(new FileWriter(headerFile)));
        } catch (IOException e)
		{
            System.out.println("Error opening " + filePath);
            return true;
        }
        return false;
    }

}
