/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: T2500.java
 * Written by Hesam Fathi Moghadam, Sun Microsystems.
 *
 * Copyright (c) 2008 Sun Microsystems and Static Free Software
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

public class T2500 extends Equipment {
    
    /** Creates a new instance of T2500 */
    public T2500 (String name) {
        super(name);
        logInit("Initialized T2500 Temperature Forcer named " + name);
    }

    /**
     * Queries the T2500 device to obtain the air flow temperature.
     * @return Returns the current temperature of the air inside the thermal mixture, in Degrees Celsius
     */
    public float getAirTemp() {
        write("AIRTEMP?");
        String [] s = read(40).split(" ");
        return Float.parseFloat(s[0]);
    }

    /**
     * Queries the T2500 device to obtain the device under test temperature.
     * @return Returns the current temperatur of the device under test, in Degrees Celsius
     */
    public float getDeviceTemp() {
        write("DUTTEMP?");
        String [] s = read(40).split(" ");
        return Float.parseFloat(s[0]);
    }

    /**
     * Queries the T2500 device to obtain the soak time.
     * @return Returns the soak time (values from 0-9999 seconds)
     */
    public float getSoakTime() {
        write("SOAKTIME?");
        String [] s = read(40).split(" ");
        return Float.parseFloat(s[0]);
    }

    /**
     * Queries the T2500 device to obtain the current setpoint.
     * @return Returns the current setpoint (values from -99.9C to 230C)
     */
    public float getSetpoint() {
        write("SETPOINT?");
        String [] s = read(40).split(" ");
        return Float.parseFloat(s[0]);
    }

    /**
     * Queiries the T2500 for its current status.
     * @return Returns the current state of the T2500 (values from 0 - 255)
     */
    public float getStatus() {
        write("TESR?");
        String [] s = read(40).split(" ");
        return Float.parseFloat(s[0]);
    }

    /**
     * Queiries the T2500 for its current head position.
     * @return Returns false if head is down, true if head is up.
     */
    public boolean getHeadState() {
        write("HEAD?");
        String [] s = read(40).split(" ");
        return Boolean.valueOf(s[0]).booleanValue();
        //return read(40);
    }

    /**
     * Brings the head of the T2500 down.
     * @param state If true, head goes down, otherwise head comes up.
     */
    public void headDown(boolean state) {
        if (state) {write("HEAD 0");}
        else { write("HEAD 1"); }
    }

    /**
     * Queiries the T2500 for its current compressor state.
     * @return Returns false if compressor off, true if compressor on.
     */
    public boolean getCompressorState() {
        write("COMP?");
        String [] s = read(40).split(" ");
        return Boolean.valueOf(s[0]).booleanValue();
    }

    /**
     * Turn the compressor of the T2500 on or off.
     * @param state If state is true, compressor will turn on, else compressor will turn off
     */
    public void setCompressorState(boolean state) {
        if (state){ write("COMP 1"); }
        else { write("COMP 0"); }
    }

    /**
     * Sets one of five available temperature presets for manual control.
     * @param presetNum Preset Number, 1-5
     * @param value Temperature, -99.9C to 230C
     */
    public void setTemp (int presetNum, float value){
        write("TEMP" + presetNum + " " + value);
    }

    /**
     * Gets the specified temperature preset
     * @param presetNum Preset Number, 1-5
     * @return Returns the set temperature of the specified preset
     */
    public float getTemp (int presetNum){
        write ("TEMP"+presetNum+"?");
        String [] s = read(40).split(" ");
        return Float.parseFloat(s[0]);
    }

    /**
     * Activates one of the temperature presets that has been already set up on the manual control screen or through GPIB.
     * @param presetNum preset temperature number, 1-5
     */
    public void goTemp (int presetNum){
        write("GOTEMP" + presetNum);
    }

    /**
     * Sets the soak time for one of the temperature presets
     * @param presetNum Preset Number, 1-5
     * @param soakTime Amount of time to soak device in set temperature, 0-9999 seconds
     */
    public void setSoakTime (int presetNum, float soakTime){
        write("SOAK" + presetNum + " " + soakTime);
    }

    /**
     * Queries the T2500 for the soak time of supplied preset number
     * @param presetNum Preset number, 1-5
     * @return Returns soak time for supplied preset number
     */
    public float getSoakTime (int presetNum){
        write ("SOAK"+presetNum+"?");
        String [] s = read(40).split(" ");
        return Float.parseFloat(s[0]);
    }

    /**
     * Turns the air and the heaters in the thermal mixutre off.
     */
    public void airOff (){
        write("AIROFF");
    }

    /**
     * Sets the type of cold air functioning of the cold-boost feaure
     * @param level 0 = no boost, normal air functioning; 1 = Air, causing the cold air regulator to be bypassed,
     * allowing un-regulated cold air to flow; 2 = LN2, activates use of liquid nitrogen if the option is installed
     */
    public void setBoost (int level){
            write("BOOST "+level);
    }

    /**
     * This method returns the boost level
     * @return 0 = no boost, normal air functioning; 1 = Air, causing the cold air regulator to be bypassed,
     * allowing un-regulated cold air to flow; 2 = LN2, activates use of liquid nitrogen if the option is installed
     */
    public float getBoostLevel (){
        write ("BOOST?");
        String [] s = read(40).split(" ");
        return Float.parseFloat(s[0]);
    }

    /**
     * Sets the flow rate of the cold air.
     * @param value Valid values range from 100 - 999 SCFH (Cubic Feet per Hour at Standard Conditions)
     */
    public void setColdFlow (int value){
            write("COLDFLOW "+value);
    }

    /**
     * Gets the flow rate of the cold air.
     * @return Valid values range from 100 - 999 SCFH (Cubic Feet per Hour at Standard Conditions)
     */
    public float getColdFlow (){
        write ("COLDFLOW?");
        String [] s = read(40).split(" ");
        return Float.parseFloat(s[0]);
    }

    /**
     * Sets the flow rate of the hot air.
     * @param value Valid values range from 100 - 999 SCFH (Cubic Feet per Hour at Standard Conditions)
     */
    public void setHotFlow (int value){
            write("HOTFLOW "+value);
    }

    /**
     * Gets the flow rate of the hot air.
     * @return Valid values range from 100 - 999 SCFH (Cubic Feet per Hour at Standard Conditions)
     */
    public float getHotFlow (){
        write ("HOTFLOW?");
        String [] s = read(40).split(" ");
        return Float.parseFloat(s[0]);
    }

    /**
     * Turns temperature ramping on or off. Must be on for value set by RAMPRATE to function.
     * @param state if true, ramp = on, else ramp = off
     */
    public void setRampState (boolean state){
        if (state) {write("RAMP 1");}
        else { write("RAMP 0"); }
    }

    /**
     * @return Returns true if RAMP is enabled, false if RAMP is disabled
     */
    public boolean getRampState() {
            write("RAMP?");
            String [] s = read(40).split(" ");
            return Boolean.valueOf(s[0]).booleanValue();
        }


    /**
     * Determines ramprate, which is the number of degrees/minute the temperature setpoint will change when the ramp
     * feature has been activated. 
     * @param value Value of ramp rate, from 0 - 999 degrees/minute
     */
    public void setRampRate (int value){
        write("RAMPRATE "+value);
    }

    /**
     *
     * @return Returns the currently set up ramp rate value.
     */
    public float getRampRate (){
        write ("RAMPRATE?");
        String [] s = read(40).split(" ");
        return Float.parseFloat(s[0]);
    }

    /**
     * Determines the type (mode) of termperature sensing
     * @param value 0 = Fixture/air sense control; 1 = DUT sense control, K-type thermocouple; 2 = DUT sense control,
     * T-type thermocouple; 3 = RTD sensor control
     */
    public void setTempSensing (int value){
        write("TCONTROL "+value);
    }

    /**
     * Returns the type (mode) of temperature sensing
     * @return value 0 = Fixture/air sense control; 1 = DUT sense control, K-type thermocouple; 2 = DUT sense control,
     * T-type thermocouple; 3 = RTD sensor control
     */
    public float getTempSensingMode (){
        write ("TCONTROL?");
        String [] s = read(40).split(" ");
        return Float.parseFloat(s[0]);
    }

    /**
     * Sets the temperature resolution window around the setpoint at which the system is considered to be at temperature.
     * @param value temperature tolerance, 0.1 - 15.0 degrees
     */
    public void setTempTolerance (float value){
        write("TEMPTOL "+value);
    }

    /**
     * Returns the current temperature resolution window
     * @return Resolution, 0.1 - 15.0 degrees
     */
    public float getTempTol (){
        write ("TEMPTOL?");
        String [] s = read(40).split(" ");
        return Float.parseFloat(s[0]);
    }

    /**
     * Sets the minimum temperature of the cold air that the DUT is exposed to in DUT-sense
     * @param value Valid values range from -99 - 30.9
     */
    public void setMinTemp (float value){
            write("MINTEMP "+value);
    }

    /**
     * Returns the min temp of the cold air that the DUT is exposed to in DUT-sense
     * @return Min temp, -99 to 30.9
     */
    public float getMinTemp (){
        write ("MINTEMP?");
        String [] s = read(40).split(" ");
        return Float.parseFloat(s[0]);
    }

    /**
     * Sets the minimum temperature of the  of the hot air that the DUT is exposed to in DUT-sense
     * @param value Valid values range from 31.0 - 230.0
     */
    public void setMaxTemp (float value){
            write("MAXTEMP "+value);
    }

    /**
     * Returns the max temp of the hot air that the DUT is exposed to in DUT-sense
     * @return Max temp, 31.0 to 230.0
     */
    public float getMaxTemp (){
        write ("MAXTEMP?");
        String [] s = read(40).split(" ");
        return Float.parseFloat(s[0]);
    }

    public String test() {
           write("FLOW?");
           return read(40);
    }

	public float getFlow() {
		write("FLOW?");
		String [] s = read(40).split(" ");
        return Float.parseFloat(s[0]);
	}

    public static void main(String[] args) {
        int [] gpibController = new int[] {0};
        Infrastructure.gpibControllers = gpibController;

        T2500 DUT = new T2500("T2500");
        DUT.airOff();





        /*DUT.setTemp(2, 23);

        System.out.println("Air Temp: " + DUT.getAirTemp());
        System.out.println("Device Temp: " + DUT.getDeviceTemp());
        System.out.println("Soak Time: " + DUT.getSoakTime());
        System.out.println("Set Point: " + DUT.getSetpoint());
        System.out.println("Status: " + DUT.getStatus());
        System.out.println("Head State " + DUT.getHeadState());
        //DUT.headDown(false);
        //System.out.println("Head State " + DUT.getHeadState());
        //DUT.headDown(true);
        //System.out.println("Head State " + DUT.getHeadState());        
        //DUT.setCompressorState(true); //works
        System.out.println("Compressor State " + DUT.getCompressorState());   //check to make sure this is not backwards, doesnt seem to work
        System.out.println("Preset 2 temp: " + DUT.getTemp(2));
        //DUT.goTemp(2);
        DUT.setSoakTime (2, 120);
        System.out.println("Preset 2 soak time: " + DUT.getSoakTime (2));
        //DUT.airOff();
        DUT.setBoost(0);
        System.out.println("Boost level: " + DUT.getBoostLevel());
        DUT.setColdFlow(300);
        System.out.println("Cold flow level: " + DUT.getColdFlow());
        DUT.setHotFlow(300);
        System.out.println("Hot flow level: " + DUT.getHotFlow());
        DUT.setRampState(true);  //not working
        DUT.setRampRate(10);
        System.out.println("Ramp State: " + DUT.getRampState()); //may not be working b/c setRampState giving bad result
        System.out.println("Ramp Rate: " + DUT.getRampRate());
        DUT.setTempSensing(1);
        System.out.println("Temp Sensing Mode: " + DUT.getTempSensingMode());
        DUT.setTempTolerance(0.5f);
        System.out.println("Temp Tol: " + DUT.getTempTol());

        */
        DUT.setMaxTemp(150f);
        DUT.setMinTemp(-60f);

        System.out.println("Min Temp: " + DUT.getMinTemp());
        System.out.println("Max Temp: " + DUT.getMaxTemp());
        //TO DO: test

    }
}
