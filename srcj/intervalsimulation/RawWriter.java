package trick;
import Jama.Matrix;
import java.io.*;
import java.util.Locale;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

class RawWriter {
    static void write(String fileName, String varName[], String varType[], Matrix data)
    throws FileNotFoundException {
       DecimalFormat fmt = new DecimalFormat("0.000000000000000000E00");
       fmt.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.US));

       PrintWriter FILE = new PrintWriter(new FileOutputStream(fileName));

       int i;
       FILE.println("Title: %s" + fileName);
       FILE.println("Plotname: Waveform");
       FILE.println("Flags: real");
       FILE.println("No. Variables: " + data.getColumnDimension());
       FILE.println("No. Points: " + data.getRowDimension());
       FILE.println("Variables:");
       for (i = 0; i < data.getColumnDimension(); i++) {
	   FILE.println("\t" + i + "\t" + varName[i] + "\t" + varType[i]);
       }
       FILE.println("Values:");

       for (int j = 0; j < data.getRowDimension(); j++) {
	   FILE.println(" " + j + "\t" + fmt.format(data.get(j, 0)));

	   for (i = 1; i < data.getColumnDimension(); i++) {
	       FILE.println("\t" + fmt.format(data.get(j, i)));
	   }
	   FILE.println();
	}
       FILE.close();
    }
}
