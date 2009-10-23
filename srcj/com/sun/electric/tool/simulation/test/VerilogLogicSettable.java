package com.sun.electric.tool.simulation.test;

/*
 * VerilogScan.java
 *
 * Copyright (c) 2004,2005 by Sun Microsystems, Inc.
 *
 * Created on Apr 28, 2005
 */

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * A Logic Settable device that interfaces with a verilog simulation.
 *
 * @author gainsley
 */
public class VerilogLogicSettable implements LogicSettable {

    private final VerilogModel vm;
    /** port(s) this logical settable checks: list of Strings */
    private final String port;
    private final VerilogModel.AliasedNames replacedNames;

    VerilogLogicSettable(VerilogModel vm, String port) {
        this.vm = vm;
        this.port = port;
        this.replacedNames = null;
    }

    VerilogLogicSettable(VerilogModel vm, List ports) {
        this.vm = vm;
        if (ports == null) {
            this.port = null;
            this.replacedNames = null;
            return;
        }
        this.port = (String)ports.get(0);
        this.replacedNames = new VerilogModel.AliasedNames(port, ports);
    }

    private String getPortName() {
        return port;
    }

    public boolean isLogicStateHigh() {
        String p = getPortName();
        if (p == null) return false;

        int state = vm.getNodeState(port);
        return (state == 1);
    }

    public void setLogicState(boolean logicState) {
        String p = getPortName();
        if (p == null) return;

        vm.setNodeState(port, logicState ? 1 : 0);
    }

    List getPorts() {
        List ports = new ArrayList();
        if (getPortName() == null) return ports;
        ports.add(new VerilogParser.Port(getPortName(), VerilogParser.Port.INOUT));
        return ports;
    }

    VerilogModel.AliasedNames getAliasedNames() {
        return replacedNames;
    }

    /** Unit Test */
    public static void main(String [] args) {
        VerilogModel vm = new VerilogModel();
        ArrayList list = new ArrayList();
        list.add("TCK");
        list.add("TMS");
        list.add("TDOb");
        vm.createLogicSettable(list);
        vm.start("verilog", VerilogModel.getExampleVerilogChipFile(), VerilogModel.NORECORD);
        vm.finish();
    }

}
