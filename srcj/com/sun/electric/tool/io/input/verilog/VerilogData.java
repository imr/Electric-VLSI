package com.sun.electric.tool.io.input.verilog;

import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.tool.Job;

import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: gg151869
 * Date: Jan 19, 2007
 * Time: 1:04:47 PM
 * To change this template use File | Settings | File Templates.
 */
public class VerilogData
{
    String name;
    private Map<String, VerilogModule> modules = new HashMap<String, VerilogModule>();

    VerilogData(String name)
    {
        this.name = name;
    }

    VerilogModule addModule(String name)
    {
        VerilogModule module = new VerilogModule(name);
        modules.put(name, module);
        return module;
    }

    /**
     * Function to return a collection of modules defined
     * @return Collection of VerilogModule objects
     */
    public Collection<VerilogModule> getModules() {return modules.values();}

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


    /********************** AUXILIAR CLASSES *************************************/
    /**
     * Covers supplies
     */
    static class VerilogConnection
    {
        String name;

        VerilogConnection(String name)
        {
            this.name = name;
        }
    }

    /**
     * This class covers input/output/inout
     */
    public class VerilogPort extends VerilogConnection
    {
        String busPins; // null if it is not a bus otherwise it will store pin sequence. Eg [0:9]
        PortCharacteristic type;

        VerilogPort(String name, PortCharacteristic type)
        {
            super(name);
            this.type = type;
        }

        void write()
        {
            String typeName = "";
            if (type == PortCharacteristic.BIDIR) typeName = "inout";
            else if (type == PortCharacteristic.IN) typeName = "input";
            else if (type == PortCharacteristic.OUT) typeName = "output";
            else if (type == PortCharacteristic.GND) typeName = "supply0";
            else if (type == PortCharacteristic.PWR) typeName = "supply1";
            System.out.println("\t" + typeName + " " + ((busPins!=null)?busPins:"") + " " + name + ";");
        }
    }

    /**
     * This class covers wires. To avoid confusion, VerilogExport is not used for this type.
     */
    public static class VerilogWire extends VerilogConnection
    {
        String busPins; // null if it is not a bus otherwise it will store pin sequence. Eg [0:9]

        public VerilogWire(String name)
        {
            super(name);
        }

        void write()
        {
            System.out.println("\twire " + ((busPins!=null)?busPins:"") + " " + name + ";");
        }
    }

    public class VerilogPortInst
    {
        String name;
        VerilogPort port;

        VerilogPortInst(String name, VerilogPort port)
        {
            this.name = name;
            this.port = port;
        }
    }

    public class VerilogInstance
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
     * Class to represent subcells
     */
    public class VerilogModule //extends VerilogElement
    {
        String name;
        List<VerilogWire> wires = new ArrayList<VerilogWire>();
        Map<String,VerilogPort> ports = new HashMap<String,VerilogPort>(); // collection of input/output/inout/supply elements
        List<VerilogInstance> instances = new ArrayList<VerilogInstance>();

        VerilogModule(String name)
        {
            this.name = name;
        }

        /**
         * Function returning the name of the module
         * @return String with name of the module
         */
        public String getName() {return name;}

        /**
         * Function to return list of VerilogInstance objects in the module
         * @return List of VerilogInstance objects
         */
        public List<VerilogInstance> getInstances() {return instances;}

        /**
         * Function to return collection of VerilogPort objects in the module
         * @return Collection of VerilogPort objects
         */
        public Collection<VerilogPort> getPorts() {return ports.values();}

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
         */
        VerilogPort addPort(String name)
        {
            if (Job.getDebug())
            {
                if (findPort(name) != null)
                assert(findPort(name) == null);
            }

            VerilogPort export = new VerilogPort(name, PortCharacteristic.UNKNOWN);
            ports.put(name, export);
            return export;
        }

        VerilogInstance addInstance(String name, VerilogModule element)
        {
            VerilogInstance inst = new VerilogInstance(name, element);
            instances.add(inst);
            return inst;
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
    }

}
