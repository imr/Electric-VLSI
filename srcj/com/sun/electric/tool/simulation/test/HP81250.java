/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: HP81250.java
 * Written by Ajanta Chakraborty, Sun Microsystems.
 *
 * Copyright (c) 2003 Sun Microsystems and Static Free Software
 *
 * Electric(tm) is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * Electric(tm) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Electric(tm); see the file COPYING.  If not, write to
 * the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, Mass 02111-1307, USA.
 */
package com.sun.electric.tool.simulation.test;

public class HP81250 extends Equipment {

    String s = new String("null");

    String handle = new String();

    /** Creates a new instance of HP81250 */
    public HP81250(String name) {
        super(name);
    }

    void init(String _handle) {
        write(":DVT:IDN?");
        s = read(200).trim();
        //s = s.substring(0,s.length()-1);
        System.out.println("dvt idn " + s);
        try { Thread.sleep(100); } catch (InterruptedException _) { }
        handle = _handle;
    }//end testConnection

    void start() {
        write(":dvt:instrument:handle:create? " + handle + ", \"DSR\",\"DSRA\"");
        s = read(200).trim();
        System.out.println("successfully created handle " + s);
        try { Thread.sleep(100); } catch (InterruptedException _) { }
    }//end start

    void run(boolean state) {
        if (state)
            write(":_" + handle + ":sgen:glob:init:cont ON");
        else
            write(":_" + handle + ":sgen:glob:init:cont OFF");
        try { Thread.sleep(300); } catch (InterruptedException _) { }

        //write(":_"+handle+":sgen:glob:init:cont?");
        //s = read(200).trim();
        //System.out.println("system state " + s);
        //Infrastructure.waitSeconds(.1f);
    }//end run

    void list() {
        write(":dvt:inst:hand:list?");
        s = read(200).trim();
        //s = s.substring(0,s.length()-1);
        System.out.println("list of handles " + s);
        try { Thread.sleep(100); } catch (InterruptedException _) { }
    }//end list

    void load_setting(String setting_name) {

        String actualSetting = new String();
        while (!actualSetting.equals(setting_name.toUpperCase())) {
            //write(":dvt:mmem:setting:import _"+handle+", \"" + setting_name +
            // "\" ");
            write(":_" + handle + ":mmem:setting:load \"" + setting_name
                    + "\" ");
            try { Thread.sleep(100); } catch (InterruptedException _) { }
            String acSt = get_setting();
            actualSetting = acSt.substring(1, acSt.length() - 1);
            //System.out.println(actualSetting + " " + setting_name);
        }//end while
        //System.out.println(actualSetting);
    }//end load_setting

    String get_setting() {
        write(":_" + handle + ":mmem:sett:name?");
        s = read(200).trim();
        //System.out.println("setting " + s);
        try { Thread.sleep(100); } catch (InterruptedException _) { }
        return s;
    }//end get_setting

    void countConnector() {
        write(":_" + handle + ":conf:cgr1:mod?");
        s = read(200).trim();

        System.out.println("number of modules " + s);
        //loop over modules

        Integer iObj = new Integer(s);

        for (int i = 1; i <= (iObj.intValue()); i++) {
            //find out how many connectors
            write(":_" + handle + ":conf:cgr1:mod" + i + ":conn?");
            s = read(200).trim();
            System.out.println("module number " + i + " has " + s
                    + " connectors");
            try { Thread.sleep(100); } catch (InterruptedException _) { }
        }//end for

    }//end countConnector

    void setFreq(int f) {
        write(":_" + handle + ":sgen:global:freq " + f + "e6");
        write(":_" + handle + ":sgen:global:mux 4");
        try { Thread.sleep(100); } catch (InterruptedException _) { }
        write(":_" + handle + ":sgen:global:freq?");
        s = read(200).trim();
        //System.out.println("freq set to " + s);
    }//end setFreq

    void createSetting(String setting_name) {
        run(false);
        //renew setting
        write(":_" + handle + ":mmem:sett:new");

        //create 3 ports, 1 data, 2 clock, one terminal under each port
        write(":_" + handle + ":sgen:pdat1:app \"INPUT_PORT\",1,\"data\",ELEC");
        write(":_" + handle + ":sgen:PPUL1:app \"INPUT_PORT\",1,\"pulse\",ELEC");
        write(":_" + handle + ":sgen:PPUL2:app \"INPUT_PORT\",1,\"pulse\",ELEC");

        //connect connectors to terminals
        write(":_" + handle + ":sgen:conn:pdat1:term1:to (@0102004)");
        write(":_" + handle + ":sgen:conn:ppul1:term1:to (@0103002)");
        write(":_" + handle + ":sgen:conn:ppul2:term1:to (@0103001)");

        //set the voltage levels
        write(":_" + handle + ":mod2:conn4:volt:high 1.8");
        write(":_" + handle + ":mod3:conn2:volt:high 1.8");
        write(":_" + handle + ":mod3:conn1:volt:high 1.8");

        //RZ or NRZ
        write(":_" + handle + ":mod2:conn4:dig:sign:format NRZ");
        write(":_" + handle + ":mod3:conn2:dig:sign:format RZ");
        write(":_" + handle + ":mod3:conn1:dig:sign:format RZ");

        //duty cycle
        write(":_" + handle + ":mod2:conn4:pulse:dcycle 10");
        write(":_" + handle + ":mod3:conn2:pulse:dcycle 30");
        write(":_" + handle + ":mod3:conn1:pulse:dcycle 30");

        //global parameters period and segment resolution
        write(":_" + handle + ":sgen:global:period 40e-9");
        write(":_" + handle + ":sgen:global:mux 4");

        //segment resolution
        write(":_" + handle + ":sgen:pdat1:mux 2.5E-1");
        write(":_" + handle + ":cgr1:mod3:conn2:mux 1");
        write(":_" + handle + ":cgr1:mod3:conn1:mux 1");

        write(":_" + handle + ":sgen:pdat1:mux?");
        String s = read(200).trim();
        System.out.println("mux " + s);

        //switch on the terminals
        write(":_" + handle + ":mod2:conn4:outp on");
        write(":_" + handle + ":mod3:conn2:outp on");
        write(":_" + handle + ":mod3:conn1:outp on");

        //set sequence
        write(":_"
                + handle
                + ":SGEN:GLOB:SEQ (3.0,\"\",(SEQ,(LOOP4,0,20,\"00\",(BLOCK,0,16,0,\"00\",(),PAUSE0,0,0)),\"\",(BLOCK,0,16,0,\"00\",(),\"REFRESHCLK\",0,0),\"\",(LOOP5,0,INF,\"00\",(BLOCK,0,16,0,\"00\",(),PAUSE0,0,0))))");
        write(":_"
                + handle
                + ":SGEN:GLOB:SEQ:EVEN (3.0,(\"\",0,(('x','xxxxxxxx','xx',IGNORE))),(\"\",0,(('x','xxxxxxxx','xx',IGNORE))),(\"\",0,(('x','xxxxxxxx','xx',IGNORE))),(\"\",0,(('x','xxxxxxxx','xx',IGNORE))),(\"\",0,(('x','xxxxxxxx','xx',IGNORE))),(\"\",0,(('x','xxxxxxxx','xx',IGNORE))),(\"\",0,(('x','xxxxxxxx','xx',IGNORE))),(\"\",0,(('x','xxxxxxxx','xx',IGNORE))),(\"\",0,(('x','xxxxxxxx','xx',IGNORE))),(\"\",0,(('x','xxxxxxxx','xx',IGNORE))))");
        write(":_" + handle + ":SGEN:GLOB:SEQ:PCON 0");

        //save the setting
        write(":_" + handle + ":mmem:sett:save \"" + setting_name + "\"");

    }//end createSetting

    void switchConnector(String mod, String conn, String state) {
        write(":_" + handle + ":mod" + mod + ":conn" + conn + ":outp " + state
                + "");
        //Infrastructure.waitSeconds(.1f);
    }//end switchConnector

    void getConnectorStat() {
        write(":_" + handle + ":sgen:glob:conn?");
        String s = read(200).trim();
        System.out.println("connector stat " + s);
    }//end getConnectorStat

    void destroyHandle() {
        //write(":_" +handle+":sgen:glob:init:cont OFF");
        //Infrastructure.waitSeconds(.1f);

        write("dvt:instrument:handle:destroy _" + handle + "");
        System.out.println("stopped");
        write(":dvt:instrument:handle:list?");
        String s = read(200).trim();
        System.out.println("handles left " + s);

        try { Thread.sleep(100); } catch (InterruptedException _) { }
    }//end stop

    private void stepThruRefresh() {
        run(true);
        run(false);
    }

    private void checkConnectors() {
        start();
        list();
        load_setting("try2");
        setFreq(20);
        switchConnector("3", "1", "on");
        switchConnector("3", "2", "on");
        run(true);
        run(false);
        switchConnector("3", "1", "off");
        switchConnector("3", "2", "off");
        destroyHandle();
    }

    public static void main(String args[]) {
        //boolean onlyKill = false;
        boolean onlyKill = true;

        HP81250 bert = new HP81250("hp81250");
        bert.init("HANDLEA");
        /*
         * if(!onlyKill) { //dont use this
         * bert.load_setting("c:\\81250\\settings\\try_load");
         * //bert.countConnector(); bert.start(); bert.list();
         * bert.load_setting("try2"); //for(int i=0; i <3; i++) //{
         * bert.run(true); //GPIB.waitFor(0.1f); bert.run(false); //
         * GPIB.waitFor(0.1f); bert.run(true); bert.run(false); //}//end for
         * //bert.createSetting("code"); //stepThruRefresh(); //int count=1;
         * //while(count==1) //stepThruRefresh();
         * 
         * }//end if
         * 
         * else bert.destroyHandle();
         */
        bert.destroyHandle();
        //checkConnectors();

    }//end main

}//end class
