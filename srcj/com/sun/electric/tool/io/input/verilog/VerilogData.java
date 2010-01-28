package com.sun.electric.tool.io.input.verilog;

import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.tool.Job;

import java.util.*;
import java.io.Serializable;

/**
 * User: gg151869
 * Date: Jan 19, 2007
 */
public class VerilogData implements Serializable
{
    String name;
    private Map<String, VerilogModule> modules = new HashMap<String, VerilogModule>();
    private VerilogModule lastModule; // last module read is the top cell info

    VerilogData(String name)
    {
        this.name = name;
    }

    public String getName() {return name;}

    public Cell getTopSchematicCell()
    {
        String topCellName = TextUtils.getFileNameWithoutExtension(name, true);
        String cellName = topCellName + View.SCHEMATIC.getAbbreviationExtension();
        Cell top = Library.findCellInLibraries(cellName, View.SCHEMATIC, null);
        if (top == null) // found a cell called like the filename
        {
            // taking the first cell found in module
            assert(lastModule != null); // it should have read at least one from file
            top = Library.findCellInLibraries(lastModule.name + View.SCHEMATIC.getAbbreviationExtension(),
                View.SCHEMATIC, null);
        }
        return top;
    }

    VerilogModule addModule(String name, boolean primitive, boolean definedInFile)
    {
        VerilogModule module = new VerilogModule(name, primitive);
        if (definedInFile) // read from the file, not instance
            lastModule = module;
        modules.put(name, module);
        return module;
    }

    /**
     * Compare class for VerilogModule
     */
    private static VerilogModuleSort compareVerilogModules = new VerilogModuleSort();

    private static class VerilogModuleSort implements Comparator<VerilogModule>
    {
        public int compare(VerilogModule a1, VerilogModule a2)
        {
            int cmp = TextUtils.STRING_NUMBER_ORDER.compare(a1.getName(), a2.getName());
            return cmp;
//            return (a1.getName().compareTo(a2.getName()));
        }
    }

    /**
     * Function to return a collection of modules defined.
     * The collection is sorted by name.
     * @return Collection of VerilogModule objects
     */
    public Collection<VerilogModule> getModules()
    {
        List<VerilogModule> list = new ArrayList<VerilogModule>(modules.size());
        list.addAll(modules.values());
        Collections.sort(list, compareVerilogModules);
        return list;
    }

    /**
     * Function to return VerilogModule object for a given name
     * @param name name of the module
     * @return VerilogModule object
     */
    VerilogModule getModule(String name) {return modules.get(name);}

    /**
     * Function to write in standard output the modules in Verilog format
     */
    void write()
    {
        for (VerilogModule module : modules.values())
        {
            module.write();
        }
    }

    void simplifyWires()
    {
        for (VerilogModule module : modules.values())
        {
            module.simplifyWires();
        }
    }

    /********************** AUXILIAR CLASSES *************************************/
    /**
     * Covers supplies
     */
    abstract static class VerilogConnection implements Serializable
    {
        protected String name;
        int start;
        int end;

        VerilogConnection(String name)
        {
            this.name = name;
        }

        /**
         * Method to control if busses are converted into single pins or treated as bus in Electric.
         * For now, busses are converted into single pins. More memory is used though.
         * @return the list of pin names.
         * @param fullOyster
         */
        abstract List<String> getPinNames(boolean fullOyster);
        PortCharacteristic getPortType() {return null; } // not valid
        String getName() {return name;}

        /**
         * Function to know if a wire represents a wire
         * @return true if wire is a bus
         */
        boolean isBusConnection()
        {
            return (start != end);
        }

        String getConnectionName()
        {
            if (start != -1) // it could be a bus or simple wire
            {
                if (isBusConnection())
                    return name + "[" + start + ":" + end + "]";
                else
                    return name + "[" + start + "]";
            }
            else // simple wire
                return name;
        }
    }

    /**
     * Compare class for VerilogModule
     */
    private static VerilogPortSort compareVerilogPorts = new VerilogPortSort();

    private static class VerilogPortSort implements Comparator<VerilogPort>
    {
        public int compare(VerilogPort a1, VerilogPort a2)
        {
            int cmp = TextUtils.STRING_NUMBER_ORDER.compare(a1.name, a2.name);
            return cmp;
//            return (a1.name.compareTo(a2.name));
        }
    }

    /**
     * This class covers input/output/inout
     */
    public class VerilogPort extends VerilogConnection
    {
        PortCharacteristic type;

        VerilogPort(String name, PortCharacteristic type)
        {
            super(name);
            this.type = type;
            start = end = -1;
        }
        PortCharacteristic getPortType() {return type; } // not valid

        void setBusInformation(String s)
        {
            int pos = s.indexOf(":");
            start = Integer.parseInt(s.substring(1, pos)); // first number
            end = Integer.parseInt(s.substring(pos+1, s.length()-1)); // second number
        }

        public List<String> getPinNames(boolean fullOyster)
        {
            List<String> list = new ArrayList<String>();

            if (fullOyster && isBusConnection())
            {
                extractPinNames(start, end, name, list);
            } else
            {
                list.add(name);
            }

            return list;
        }

        void write()
        {
            String typeName = "";
            if (type == PortCharacteristic.BIDIR) typeName = "inout";
            else if (type == PortCharacteristic.IN) typeName = "input";
            else if (type == PortCharacteristic.OUT) typeName = "output";
            else if (type == PortCharacteristic.GND) typeName = "supply0";
            else if (type == PortCharacteristic.PWR) typeName = "supply1";
            System.out.println("\t" + typeName + " " + ((isBusConnection())?"["+start+":"+end+"]":"") + " " + name + ";");
        }
    }

    /**
     * This class covers wires. To avoid confusion, VerilogExport is not used for this type.
     */
    public static class VerilogWire extends VerilogConnection
    {
        public VerilogWire(String name, String busPins)
        {
            super(name);
            this.name = name;

            if (busPins == null)
            {
                int index = name.indexOf("[");
                if (index != -1)
                {
                    this.name = name.substring(0, index);
                    index = Integer.parseInt(name.substring(index+1, name.length()-1));
                }
                this.start = this.end = index;
            }
            else
            {
                int index2 = busPins.indexOf(":");
                assert(index2 != -1);
                start = Integer.parseInt(busPins.substring(1, index2)); // assuming 0 contains "["
                end = Integer.parseInt(busPins.substring(index2+1, busPins.length()-1));
            }
        }

        List<String> getPinNames(boolean fullOyster)
        {
            List<String> list = new ArrayList<String>();

            if (fullOyster && isBusConnection())
            {
                extractPinNames(start, end, name, list);
            } else
            {
                list.add(name);
            }

            return list;
        }

        void write()
        {
            System.out.println("\twire " + ((start != end)?("["+start+":"+end+"["):"") + " " + name + ";");
        }
    }

    private static void extractPinNames(int start, int end, String root, List<String> l)
    {
        if (start > end)
        {
            for (int i = start; i >= end; i--)
            {
                String thisName = root+"["+i+"]";
                l.add(thisName);
            }
        }
        else
        {
            for (int i = start; i <= end; i++)
            {
                String thisName = root+"["+i+"]";
                l.add(thisName);
            }
        }
    }

    public class VerilogPortInst implements Serializable
    {
        String name;
        VerilogPort port;

        VerilogPortInst(String name, VerilogPort port)
        {
            this.name = name;
            this.port = port;
        }

        List<String> getPortNames()
        {
            List<String> list = new ArrayList<String>();
             // It is unknown how many pins are coming in the stream
            if (name.contains("{"))
            {
                StringTokenizer parse = new StringTokenizer(name, "\t{,}", false); // extracting pins
                while (parse.hasMoreTokens())
                {
                    String name = parse.nextToken();
                    name = name.replaceAll(" ", "");
                    list.add(name);
                    if (Job.getDebug())
                        assert(!name.contains(":")); // this case not handled yet!
//                    else
//                        System.out.println("This case not handled yet in getPortNames");
                }
            }
            else
                list.add(name);
            // Now to really extract every individual pin
            List<String> l = new ArrayList<String>(list.size());
            for (String s : list)
            {
                int pos = s.indexOf(":");
                if (pos != -1)
                {
                    int index1 = s.indexOf("[");
                    int index2 = s.indexOf("]");
                    String root = s.substring(0, index1);
                    int start = Integer.parseInt(s.substring(index1+1, pos)); // first number
                    int end = Integer.parseInt(s.substring(pos+1, index2)); // second number
                    extractPinNames(start, end, root, l);
                }
                else
                    l.add(s);
            }

            // sort list
            return l;
        }
    }

    /**
     * Compare class for VerilogInstance
     */
    private static VerilogInstanceSort compareVerilogInstances = new VerilogInstanceSort();

    private static class VerilogInstanceSort implements Comparator<VerilogInstance>
    {
        public int compare(VerilogInstance a1, VerilogInstance a2)
        {
            int cmp = TextUtils.STRING_NUMBER_ORDER.compare(a1.getName(), a2.getName());
            return cmp;
//            return (a1.getName().compareTo(a2.getName()));
        }
    }

    public class VerilogInstance implements Serializable
    {
        String name;
        VerilogModule element;
        List<VerilogPortInst> ports = new ArrayList<VerilogPortInst>();
        // number of ports in the instance doesn't necessarily match with number of ports in original elements.

        VerilogInstance(String name, VerilogModule elem)
        {
            this.name = name;
            this.element = elem;
        }

        /**
         * Function to return the name of this instance
         * @return String with the name of the instance
         */
        public String getName() {return name;}

        /**
         * Function to return the module of this instance
         * @return Verilog object
         */
        public VerilogModule getModule() {return element;}

        VerilogPortInst addPortInstance(String name, VerilogPort port)
        {
            VerilogPortInst inst = new VerilogPortInst(name, port);
            ports.add(inst);
            return inst;
        }

        void write()
        {
            System.out.print("\t" + element.name + " " + name + " (");
            int size = ports.size();
//            assert(size == element.getNumPorts());
            for (int i = 0; i < size; i++)
            {
                VerilogPortInst port = ports.get(i);
                System.out.print("." + port.port.name + " (" + port.name + ")");
                if (i < size - 1)
                    System.out.print(", ");
            }
            System.out.println(");");
        }
    }

    /**
         * Compare class for VerilogAll
         */
        private static VerilogAllSort compareVerilogAll = new VerilogAllSort();

        private static class VerilogAllSort implements Comparator<Object>
        {
            public int compare(Object a1, Object a2)
            {
                String name1, name2;
                if (a1 instanceof VerilogInstance)
                    name1 = ((VerilogInstance)a1).getName();
                else if (a1 instanceof VerilogWire)
                    name1 = ((VerilogWire)a1).getName();
                else
                    name1 = ((VerilogPort)a1).getName();
                if (a2 instanceof VerilogInstance)
                    name2 = ((VerilogInstance)a2).getName();
                else if (a2 instanceof VerilogWire)
                    name2 = ((VerilogWire)a2).getName();
                else
                    name2 = ((VerilogPort)a2).getName();
                int cmp = TextUtils.STRING_NUMBER_ORDER.compare(name1, name2);
                return cmp;
            }
        }

    /**
     * Class to represent subcells
     */
    public class VerilogModule implements Serializable
    {
        String name;
        boolean fullInfo; // in case the module information was found in the file
        private List<VerilogWire> wires = new ArrayList<VerilogWire>();
        private Map<String,VerilogPort> ports = new LinkedHashMap<String,VerilogPort>(); // collection of input/output/inout/supply elements, ordering is important
        List<VerilogInstance> instances = new ArrayList<VerilogInstance>();
        boolean primitive; // if this is a primitive instead of a module

        VerilogModule(String name, boolean primitive)
        {
            this.name = name;
            this.fullInfo = false;
        }

        /**
         * Function to mark module as fully read it from the file
         * @param flag
         */
        void setValid(boolean flag) {fullInfo = flag;}

        /**
         * Returns if module is valid, i.e., theinformation was 100% read from the file
         * @return true if is a valid module
         */
        public boolean isValid() {return fullInfo;}

        /**
         * Function returning the name of the module
         * @return String with name of the module
         */
        public String getName() {return name;}

        /**
         * Returns true if this was defined as a 'primitive' instead of
         * a 'module'.
         * @return true if this module was defined as a primitive
         */
        public boolean isPrimitive() { return primitive; }

        /**
         * Returns list of ports and wires sorted by name
         * @return list of ports and wires sorted by namea
         */
        public List<Object> getAllSorted() {

            List<Object> list = new ArrayList<Object>(ports.size() + wires.size());
            list.addAll(ports.values());
            list.addAll(wires);
            Collections.sort(list, compareVerilogAll);
            return list;
        }

        /**
         * Function to return list of VerilogInstance objects in the module.
         * The list is sorted.
         * @return List of VerilogInstance objects
         */
        public List<VerilogInstance> getInstances()
        {
            Collections.sort(instances, compareVerilogInstances);
            return instances;
        }

        /**
         * Function to return list of VerilogWire objects in the module.
         * The list is sorted.
         * @return List of VerilogWire objects
         */
        public List<VerilogWire> getWires()
        {
            Collections.sort(wires, compareVerilogWires);
            return wires;
        }

        /**
         * Function to return collection of VerilogPort objects in the module.
         * The ports are sorted by the name
         * @return Collection of VerilogPort objects
         */
        public Collection<VerilogPort> getPorts()
        {
            List<VerilogPort> list = new ArrayList<VerilogPort>(ports.size());
            list.addAll(ports.values());
            Collections.sort(list, compareVerilogPorts);
            return list;
        }

        /**
         * Function to search an export for a given name
         * @param name export name
         * @return VerilogExport represeting the export
         */
        VerilogPort findPort(String name)
        {
            // In case of large set, better if ports are in a map.
            return ports.get(name);
        }

        /**
         * Function to add a given export to the list
         * @param name name of the new export
         * @param checkClock
         * @param checkDuplicatedPortName
         */
        VerilogPort addPort(String name, boolean checkClock, boolean checkDuplicatedPortName)
        {
            if (checkDuplicatedPortName && findPort(name) != null)
            {
                System.out.println("Duplicated port name? " + name);
                if (Job.getDebug())
                    assert(false); // force to take a look
            }

            PortCharacteristic def = PortCharacteristic.UNKNOWN;
            String lowerName = name.toLowerCase();
            // attempt to get the type based on port name (in,out)
            if (lowerName.startsWith("in"))
                def = PortCharacteristic.IN;
            else if (lowerName.startsWith("out"))
                def = PortCharacteristic.OUT;
            // so far having problems to detect clk signals as input. Only done in case of matching
            // name from the module input
            else if (checkClock && lowerName.endsWith("clk"))  //
                def = PortCharacteristic.CLK;
            VerilogPort export = new VerilogPort(name, def);
            ports.put(name, export);
            return export;
        }

        VerilogInstance addInstance(String name, VerilogModule element)
        {
            VerilogInstance inst = new VerilogInstance(name, element);
            instances.add(inst);
            return inst;
        }

        VerilogWire addWire(String name, String busInfo)
        {
            VerilogData.VerilogWire wire = new VerilogData.VerilogWire(name, busInfo);
            wires.add(wire);
            return wire;
        }

        /**
         * Function to print information in Verilog format. For testing purposes mainly
         */
        void write()
        {
            System.out.print("module " + name + " (");
            Set<String> ports = this.ports.keySet();
            int size = ports.size();
            int count = 0;
            for (String s : ports)
            {
                System.out.print(s);
                if (count < size - 1)
                    System.out.print(", ");
                count++;
            }
            System.out.println(");");
            System.out.println();

            // inputs/outputs/inouts/supplies
            for (VerilogPort e : this.ports.values())
            {
                e.write();
            }
            System.out.println();

            // wires
            for (VerilogWire w : wires)
            {
                w.write();
            }

            // instances
            for (VerilogInstance i : instances)
            {
                i.write();
            }

            System.out.println("endmodule");
            System.out.println();
        }

        /**
         * Simplify wires?: a[1], a[2], a[3] -> a[1:3]
         */
        void simplifyWires()
        {
            Collections.sort(wires, compareVerilogWires);
            int i = 0;
            List<VerilogWire> toDelete = new ArrayList<VerilogWire>();

            while (i < wires.size())
            {
                VerilogWire w = wires.get(i);

                if (w.start == -1)
                {
                    i++;
                    continue; // nothing to do with this one
                }

                // searching for all wire pins with identical root name
                int j, end = w.end;
                List<VerilogWire> toMerge = new ArrayList<VerilogWire>();

                // This algorithm doesn't check for overlapping in pin numbers
                for (j = i+1; j < wires.size(); j++)
                {
                    VerilogWire r = wires.get(j);
                    // in case the element is a wire pin abc[x:y]
                    if (!w.name.equals(r.name))
                    {
                        break; // stop here
                    }
                     // look for next bit
//                    if (r.start != end && r.start != end+1)
//                        break; // stop here

                    end = r.end;
                    toMerge.add(r);
                }
                if (toMerge.size() > 0)
                {
                    // check if pins are conse
                    toDelete.addAll(toMerge);
                    w.end = end;
                }
                i = j;
            }
            wires.removeAll(toDelete);
        }
    }

    private static VerilogWireSort compareVerilogWires = new VerilogWireSort();

    private static class VerilogWireSort implements Comparator<VerilogWire>
    {
        public int compare(VerilogWire a1, VerilogWire a2)
        {
            int diff = TextUtils.STRING_NUMBER_ORDER.compare(a1.name, a2.name);
//            int diff = (a1.name.compareTo(a2.name));
            if (diff == 0) // identical
            {
                diff = a1.start - a2.start;
                if (diff == 0) // try with end pins
                {
                    diff = a1.end - a2.end;
                    assert(diff!=0); // can't have identical wires
                }
            }
            return (diff);
        }
    }
}
