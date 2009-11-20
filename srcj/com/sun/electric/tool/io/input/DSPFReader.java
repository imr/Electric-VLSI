package com.sun.electric.tool.io.input;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.id.CellId;
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.Job;

import java.net.URL;
import java.util.*;
import java.io.IOException;

/**
 * User: gg151869
 * Date: Nov 10, 2009
 */
public class DSPFReader extends Input
{
    private DSPFReaderPreferences localPrefs;

     static class DSPFReaderPreferences extends InputPreferences
    {
        public DSPFReaderPreferences(boolean factory)
        {
            super(factory);
        }

        @Override
        public Library doInput(URL fileURL, Library lib, Technology tech, Map<Library,Cell> currentCells,
                               Map<CellId, BitSet> nodesToExpand, Job job)
        {
        	DSPFReader in = new DSPFReader(this);
			if (in.openTextInput(fileURL)) return null;
			lib = in.importALibrary(lib, tech, currentCells);
            in.closeInput();
			return lib;
        }
    }

    static class DSPFInstance
    {
        String name;
        List<DSPNode> ports = new ArrayList<DSPNode>();

        DSPFInstance(String n)
        {
            name = n;
        }

         void addPort(DSPNode p) { ports.add(p);}
    }

    public static class DSPFData
    {
        Cell cell;
        String dspfDesign, dspfVendor, dspfVersion, dspfDivider, dspfDelimiter, dspfBusbit;
        DSPFNet groundNet;
        DSPFSubckt currentSubckt;
        List<DSPFSubckt> subckts;

        DSPFData (Cell c)
        {
            cell = c;
            subckts = new ArrayList<DSPFSubckt>();
        }

        DSPFSubckt addSubCircuit(String name)
        {
            assert(subckts.size() == 0);
            DSPFSubckt tmp = new DSPFSubckt(name);
            subckts.add(tmp);
            currentSubckt = tmp;
            return tmp;
        }
    }

    static class DSPNode
    {
        String name;
        EPoint point;

        DSPNode(String n, EPoint p)
        {
            name = n;
            point = p;
        }
    }

    static class DSPPort extends DSPNode
    {
        float cap;
        PortCharacteristic type;

        DSPPort(String n, double c, PortCharacteristic t, EPoint p)
        {
            super(n, p);
            cap = (float)c;
            type = t;
        }
    }

    static class DSPFPortInst extends DSPPort
    {
        String instName, pinName;

        DSPFPortInst(String n, String i, String p, double c, PortCharacteristic t, EPoint pt)
        {
            super(n, c, t, pt);
            instName = i;
            pinName = p;
        }

    }

    static class DSPFParaElem
    {
        DSPNode pin1, pin2;
        ParasiticType type;
        private float value;
        private String name;

        DSPFParaElem(String n, DSPNode s1, DSPNode s2, double v, ParasiticType t)
        {
            name = n;
            pin1 = s1;
            pin2 = s2;
            value = (float)v;
            type = t;
        }
    }

    static class DSPFNet
    {
        String name; // name of the network
        float netCap;
        Map<String, DSPFPortInst> portInstancesN = new HashMap<String, DSPFPortInst>();
        Map<String, DSPPort> portsN = new HashMap<String, DSPPort>();
        Map<String, DSPNode> subnetsN = new HashMap<String, DSPNode>();
        List<DSPFParaElem> parasitics = new ArrayList<DSPFParaElem>();

        DSPFNet(String n) {name = n;}
        public void setCap(double c) {netCap = (float)c;}

        DSPFPortInst addPortInst(String instPinName, String instName, String pinName, double pinCap,
                                        PortCharacteristic pinType, EPoint point)
        {
            DSPFPortInst inst = new DSPFPortInst(instPinName, instName, pinName, pinCap, pinType, point);
            portInstancesN.put(instPinName, inst);
            return inst;
        }

        DSPPort getExport(int i) {return (DSPPort)portsN.values().toArray()[i];}
        
        DSPPort addExport(String pinName, double pinCap, PortCharacteristic pinType, EPoint point)
        {
            DSPPort port = new DSPPort(pinName, pinCap, pinType, point);
            portsN.put(pinName, port);
            return port;
        }

        DSPNode addSubnet(String subnodeName, EPoint point)
        {
            DSPNode sub = new DSPNode(subnodeName, point);
            subnetsN.put(subnodeName, sub);
            return sub;
        }

        DSPNode findNet(String netName, DSPNode groundPort)
        {
            // look for subcircuits first
            DSPNode newPa = subnetsN.get(netName);
            if (newPa != null) return newPa;

            // look in instances
            newPa = portInstancesN.get(netName);
            if (newPa != null) return newPa;

            // look in ports
            newPa = portsN.get(netName);
            if (newPa != null) return newPa;

            if (groundPort.name.equals(name)) return groundPort;

            // Add a subnet with that name
            newPa = addSubnet(name, null);
            return newPa;
        }

        void addParasitics(String paramName, String pin1, String pin2, DSPPort groundPort, double parasitic, ParasiticType type)
        {
            DSPNode port1 = findNet(pin1, groundPort);
            DSPNode port2 = findNet(pin2, groundPort);
            DSPFParaElem par = new DSPFParaElem(paramName, port1, port2, parasitic, type);
            parasitics.add(par);
        }
    }

    static class DSPFSubckt
    {
        String name; // circuit name
        List<DSPFNet> nets = new ArrayList<DSPFNet>();
        List<DSPFInstance> instances = new ArrayList<DSPFInstance>();

        DSPFSubckt(String n)
        {
            name = n;
        }

        DSPFNet addNetwork(String netName)
        {
            assert(findNetwork(netName) == null); // only add once
            DSPFNet net = new DSPFNet(netName);
            nets.add(net);
            return net;
        }

        DSPFNet findNetwork(String netName)
        {
            for (DSPFNet net : nets)
            {
                if (net.name.equals(netName))
                    return net;
            }
            return null;
        }

        DSPFNet getNetwork(String netName)
        {
            for (DSPFNet net : nets)
            {
                if (net.name.equals(netName))
                    return net;
            }
            return addNetwork(netName); // doesn't return null if not found. It adds it.
        }

        DSPFInstance addInstance(String instName)
        {
            DSPFInstance newI = new DSPFInstance(instName);
            instances.add(newI);
            return newI;
        }
    }

    /**
	 * Method to import a DSPF file from disk.
	 * @param lib the library to ready
     * @param currentCells this map will be filled with currentCells in Libraries found in library file
	 * @return the created library (null on error).
	 */
    @Override
	protected Library importALibrary(Library lib, Technology tech, Map<Library,Cell> currentCells)
    {
        DSPFData data = parseDSPFFile(null);
        return null;
    }

    DSPFReader(DSPFReaderPreferences prefs)
    {
        localPrefs = prefs;
    }

    /**
     *
     * @param file
     * @return true if read the file without errors
     */
    public DSPFData readDSPFFile(String file, Cell cell)
    {
        URL fileURL = TextUtils.makeURLToFile(file);
        if (openTextInput(fileURL))
        {
            System.out.println("Cannot open the DSPF file: " + file);
            return null;
        }
        System.out.println("Reading DSPF file: " + file);
        return parseDSPFFile(cell);
    }

    public DSPFData parseDSPFFile(Cell cell)
    {
        initKeywordParsing();
        setProgressValue(0);
        setProgressNote("Reading DSPF file");
        DSPFData parseData = new DSPFData(cell);
        HashMap<String,DSPFNet> netsMap = new HashMap<String,DSPFNet>(); // to speed up the code
        HashMap<String,DSPNode> portsMap = new HashMap<String,DSPNode>(); // to speed up the code

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
                if (key == null) break;

                if (key.toUpperCase().equals(".ENDS"))
                    break; // done

                if (key.equals("*|DSPF"))
                {
                    String option = processOption(key);
                    if (option == null) return null;
                    parseData.dspfDesign = option;
                    continue;
                }

                if (key.equals("*|DESIGN"))
                {
                    String option = processOption(key);
                    if (option == null) return null;
                    parseData.dspfVersion = option;
                    continue;
                }

                if (key.equals("*|VENDOR"))
                {
                    String option = processOption(key);
                    if (option == null) return null;
                    parseData.dspfVendor = option;
                    continue;
                }

                if (key.equals("*|DIVIDER"))
                {
                    String option = processOption(key);
                    if (option == null) return null;
                    parseData.dspfDivider = option;
                    continue;
                }

                if (key.equals("*|DELIMITER"))
                {
                    String option = processOption(key);
                    if (option == null) return null;
                    parseData.dspfDelimiter = option;
                    continue;
                }

                if (key.equals("*|BUSBIT"))
                {
                    String option = processOption(key);
                    if (option == null) return null;
                    parseData.dspfBusbit = option;
                    continue;
                }

                if (key.equals("*|GROUND_NET"))
                {
                    String option = processOption(key);
                    if (option == null) return null;
                    parseData.groundNet = parseData.currentSubckt.getNetwork(option);
                    parseData.groundNet.addExport(option, 0, PortCharacteristic.GND, null/*new EPoint(0,0)*/);
                    continue;
                }

                // Read list of subnets in this file
                if (key.toUpperCase().equals(".SUBCKT"))
                {
                    nextToken = readSubCircuit(parseData, netsMap);
                    continue;
                }

                // reading networks
                if (key.equals("*|NET"))
                {
                    nextToken = readNetwork(parseData, portsMap);
                    continue;
                }

                if (key.equals("+")) continue; // keep reading

                // reading instances
                if (key.toUpperCase().startsWith("X"))
                {
                    String instName = key.substring(1, key.length());
                    nextToken = readInstance(parseData, instName, portsMap);
                    continue;
                }

                // Ignoring globlas for now
                if (key.toUpperCase().startsWith(".GLOBAL"))
                {
                    getRestOfLine();
                    continue;
                }

                // Comments or Coupled capacitors
                if (key.startsWith("CC"))
                {
                    String line = getRestOfLine();
                    StringTokenizer parse = new StringTokenizer(line, " ", false);

                    assert(false); // implement
//                    DParaNode p1 = null, p2 = null;
//                    double cap = 0;
//                    int count = 0;
//                    while (parse.hasMoreTokens())
//                    {
//                        String value = parse.nextToken();
//                        switch (count)
//                        {
//                            case 0:
//                                p1 = nodes.get(value); break;
//                            case 1:
//                                p2 = nodes.get(value); break;
//                            case 2:
//                                cap = TextUtils.atof(value); break;
//                        }
//                        count++;
//                    }
//                    assert(count == 3);
//                    assert(p1 != null && p2 != null);
//                    parseData.addCoupling(key, p1, p2, cap, DParaElemBase.ParasiticType.CC);
                    continue;
                }
                if (!key.startsWith("*"))
                assert (key.startsWith("*"));
                getRestOfLine();
            }
        } catch (IOException e)
        {
            System.out.println("ERROR reading DSPF technology file");
        }
        return parseData;
    }

    // support functions

    /**
     * Method to handle the options in DSPF header.
     * @param key
     * @return String with the option read
     * @throws java.io.IOException
     */
    private String processOption(String key)
        throws IOException
    {
        String option = getRestOfLine();
        if (option == null) eofDuring(key);

        return option;
    }

    private String readSubCircuit(DSPFData parseData, HashMap<String,DSPFNet> netsMap) throws IOException
    {
        List<String> tokens = new ArrayList<String>(10);
        for(;;)
        {
            String key = getAKeyword();
            if (key == null) return key; // end of file
            if (key.startsWith("+"))
                continue; // reading new line
            else if (key.startsWith("*"))
            {
                assert(parseData.currentSubckt == null); // only 1 circuit per DSPF file
                // done with the reading of the subcircuit
                DSPFSubckt subckt = parseData.addSubCircuit(tokens.get(0));
                for (int i = 1; i < tokens.size(); i++)
                {
                    String name = tokens.get(i);
                    DSPFNet net = subckt.addNetwork(name);
                    assert(netsMap.get(name) == null);
                    netsMap.put(name, net);
                }
                return key; // new instance
            }
            tokens.add(key);
        }
    }

    private String readNetwork(DSPFData parseData, HashMap<String,DSPNode> portsMap) throws IOException
    {
        // Format: NET netName netCap
        String key = getAKeyword();
        String netName = TextUtils.correctName(key, false, true);
        DSPFNet net = parseData.currentSubckt.getNetwork(netName);
        assert (net != null); // if net is not found then it is an internal net
        net.setCap(TextUtils.atof(getAKeyword()));
        DSPPort gndPort = parseData.groundNet.getExport(0);

        // Reading pin instances
        for(;;)
        {
            key = getAKeyword();  // *|P or *|I or *|S

            if (key.equals("*|I"))
                readPinInstance(net, portsMap);
            else if (key.equals("*|P"))
                readPin(net);
            else if (key.equals("*|S"))
                readSubnet(net);
            else if (key.startsWith("CC")) // Coupling capacitors
               return key; // beginning of coupling capacitors
            else if (key.startsWith("C"))
                readParasitic(key, net, ParasiticType.C, gndPort);
            else if (key.startsWith("R"))
                readParasitic(key, net, ParasiticType.R, gndPort);
            else if (key.equals("*|NET") || key.startsWith("X") || key.toUpperCase().equals(".ENDS"))
                return key; // beginning of new net or instance section
            else if (key.startsWith("*"))
            {
                getRestOfLine(); // reading comments
            }
            else
            {
                System.out.println("The key here " + key);
                if (Job.getDebug())
                assert(false);
                getRestOfLine(); // reading comments
            }
        }
    }

    private PortCharacteristic getPortPortCharacteristic(String name)
    {
        if (name.equals("I"))
            return PortCharacteristic.IN;
        else if (name.equals("O"))
            return PortCharacteristic.OUT;
        else if (name.equals("B"))
            return PortCharacteristic.BIDIR;
        else if (name.equals("X"))
            return PortCharacteristic.UNKNOWN;
        assert(false);
        return null;
    }
        
    private void readPinInstance(DSPFNet net, HashMap<String,DSPNode> portsMap) throws IOException
    {
//        DSSubckt subckt = data.currentSubckt;
//        Cell cell = data.cell;
        // Format I: instPinName instName pinName pinType pinCap [xCoord yCoord])
        String values = getRestOfLine();
        StringTokenizer parse = new StringTokenizer(values, "( )", false);
        String instPinName = null, instName = null, pinName = null;
        PortCharacteristic pinType = PortCharacteristic.UNKNOWN;
        double pinCap = 0, xCoord = 0, yCoord = 0;
        int count = 0;

        while (parse.hasMoreTokens())
        {
            String value = parse.nextToken();
            switch (count)
            {
                case 0: instPinName = TextUtils.correctName(value, false, true); break;
                case 1: instName = TextUtils.correctName(value, false, true); break;
                case 2: pinName = value; break;
                case 3: pinType = getPortPortCharacteristic(value); break;
                case 4: pinCap = TextUtils.atof(value); break;
                case 5: xCoord = TextUtils.atof(value); break;
                case 6: yCoord = TextUtils.atof(value); break;
            }
            count++;
        }
        DSPFPortInst port = net.addPortInst(instPinName, instName, pinName, pinCap, pinType, new EPoint(xCoord, yCoord));
        portsMap.put(instPinName, port);
    }

    private void readPin(DSPFNet net) throws IOException
    {
        // Format: P (pinName pinType pinCap [xCoord yCoord])
        String values = getRestOfLine();
        StringTokenizer parse = new StringTokenizer(values, "( )", false);
        String pinName = null;
        PortCharacteristic pinType = PortCharacteristic.UNKNOWN;
        double pinCap = 0, xCoord = 0, yCoord = 0;
        int count = 0;

        while (parse.hasMoreTokens())
        {
            String value = parse.nextToken();
            switch (count)
            {
                case 0: pinName = value; break;
                case 1: pinType = getPortPortCharacteristic(value); break;
                case 2: pinCap = TextUtils.atof(value); break;
                case 3: xCoord = TextUtils.atof(value); break;
                case 4: yCoord = TextUtils.atof(value); break;
            }
            count++;
        }

        net.addExport(pinName, pinCap, pinType, new EPoint(xCoord, yCoord));
    }

    public enum ParasiticType {C, R, CC}

    private void readParasitic(String name, DSPFNet net, ParasiticType type, DSPPort groundPort) throws IOException
    {
        String values = getRestOfLine();
        StringTokenizer parse = new StringTokenizer(values, " ", false);
        String pin1 = null, pin2 = null;
        double parasitic = 0;
        int count = 0;

        while (parse.hasMoreTokens())
        {
            String value = parse.nextToken();
            switch (count)
            {
                case 0: pin1 = TextUtils.correctName(value, false, true); break;
                case 1: pin2 = TextUtils.correctName(value, false, true); break;
                case 2: parasitic = TextUtils.atof(value); break;
            }
            count++;
        }
        net.addParasitics(name, pin1, pin2, groundPort, parasitic, type);
    }

    private void readSubnet(DSPFNet net) throws IOException
    {
        // Format: S (subnodeName [xCoord yCoord])
        String values = getRestOfLine();
        StringTokenizer parse = new StringTokenizer(values, "( )", false);
        String subnodeName = null;
        double xCoord = 0, yCoord = 0;
        int count = 0;

        while (parse.hasMoreTokens())
        {
            String value = parse.nextToken();
            switch (count)
            {
                case 0: subnodeName = value; break;
                case 1: xCoord = TextUtils.atof(value); break;
                case 2: yCoord = TextUtils.atof(value); break;
            }
            count++;
        }

        net.addSubnet(subnodeName, new EPoint(xCoord, yCoord)); //new EPoint(xCoord, yCoord)));
    }

    private String readInstance(DSPFData parseData, String instName, HashMap<String,DSPNode> portsMap) throws IOException
    {
        DSPFInstance instance = parseData.currentSubckt.addInstance(instName);
        for(;;)
        {
            String key = getAKeyword();
            if (key == null) return key; // end of file
            if (key.startsWith("+"))
            {
                String portName = processOption(key);
                DSPNode port = portsMap.get(portName);
                if (port == null)
                {
                    port = new DSPNode(portName, null); // export?
                    portsMap.put(portName, port);
                }
                instance.addPort(port);
            }
            else if (key.startsWith("X"))
            {
                return key; // new instance
            }
        }
    }
}
