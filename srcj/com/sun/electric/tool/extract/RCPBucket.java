package com.sun.electric.tool.extract;

/**
 * Created by IntelliJ IDEA.
 * User: gg151869
 * Date: Mar 17, 2005
 * Time: 11:04:49 AM
 * To change this template use File | Settings | File Templates.
 */
/**
 * The main purpose of this class is to store parasitic information as string.
 * It will cover parasitics for Capacitors and Resistors (schematics)
 */
public class RCPBucket implements ExtractedPBucket
{
    private char type;
    public String net1;
    public String net2;
    public double rcValue;

    public RCPBucket(char type, String net1, String net2, double rcValue)
    {
        this.type = type;
        this.net1 = net1;
        this.net2 = net2;
        this.rcValue = rcValue;
    }

    public char getType() {return type;}

    /**
     * Method to be used to retrieve information while printing the deck
     * @return
     */
    public String getInfo(double scale)
    {
        String info = type + " " + net1 + " " + net2 + " " + rcValue;;
        return info;
    }
}
