package com.sun.electric.plugins.j3d.utils;

import com.sun.electric.tool.Job;
import com.sun.electric.plugins.j3d.ui.J3DViewDialog;
import com.sun.electric.plugins.j3d.utils.J3DUtils;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.DatagramPacket;
import java.util.StringTokenizer;

/**
 * Created by IntelliJ IDEA.
 * User: Gilda
 * Date: Feb 24, 2005
 * Time: 5:58:39 PM
 * To change this template use File | Settings | File Templates.
 */
public class J3DClientApp extends Job
{
    private static final int VALUES_PER_LINE = 11;
    private static double[] lastValidValues = new double[VALUES_PER_LINE];

    public static double[] convertValues(String[] stringValues)
    {
        double[] values = new double[stringValues.length];
        for (int i = 0; i < stringValues.length; i++)
        {
            try
            {
                values[i] = Double.parseDouble(stringValues[i]);
            }
            catch (Exception e) // invalid number in line
            {
                values[i] = lastValidValues[i];
            }
            lastValidValues[i] = values[i];
            if (2 < i && i < 6 )
                values[i] = J3DUtils.convertToRadiant(values[i]);   // original value is in degrees
        }
        return values;
    }

    /**
     * To parse capacitance data from line
     * Format: posX posY posZ rotX rotY rotZ rotPosX rotPosY rotPosZ capacitance radius error
     * @param line
     * @param lineNumner
     */
    public static String[] parseValues(String line, int lineNumner)
    {
        int count = 0;
        String[] strings = new String[VALUES_PER_LINE]; // 12 is the max value including errors
        StringTokenizer parse = new StringTokenizer(line, " ", false);

        while (parse.hasMoreTokens() && count < VALUES_PER_LINE)
        {
            strings[count++] = parse.nextToken();
        }
        if (count < 9 || count > 13)
        {
            System.out.println("Error reading capacitance file in line " + lineNumner);
        }
        return strings;
    }

    /** dialog box which owns this job */   private J3DViewDialog dialog;
    /** hostname to connect to */           private String hostname;

    public J3DClientApp(J3DViewDialog dialog, String hostname)
    {
        super("Socket Connection", null, Job.Type.EXAMINE, null, null, Job.Priority.ANALYSIS);
        this.dialog = dialog;
        this.hostname = hostname;
    }

    /**
     * To kill this particular job
     */
    public void killJob()
    {
        abort();
        checkAbort();
        remove();
    }

    /**
     *
     * @return
     */
    public boolean doIt()
    {
        try
        {
            DatagramSocket socket = new DatagramSocket();
            InetAddress address = InetAddress.getByName(hostname);
            address = InetAddress.getLocalHost();
            String localHost = address.getHostName();
            int bufferLenght = 256;
            byte outBuffer[];
            byte inBuffer[] = new byte[bufferLenght];
            DatagramPacket outDatagram;
            DatagramPacket inDatagram = new DatagramPacket(inBuffer, inBuffer.length);
            boolean finished = false;

            while (!finished)
            {
                outBuffer = new byte[bufferLenght];
                outBuffer = dialog.getToggleInfo().getBytes();
                outDatagram = new DatagramPacket(outBuffer, outBuffer.length, address, 2345);
                socket.send(outDatagram);
                System.out.println("Sent request to " + localHost + " at port 2345");
                socket.receive(inDatagram);
                InetAddress destAddress = inDatagram.getAddress();
                String destHost = destAddress.getHostName().trim();
                int destPort = inDatagram.getPort();
                System.out.println("Received a datagram from " + destHost + " at port" + destPort);
                String inData = new String(inDatagram.getData()).trim();
                System.out.println("It contained inData '" + inData);
                if (inData.equalsIgnoreCase("quit"))
                    finished = true;
                else
                {
                    dialog.socketAction(inData);
                    finished = checkAbort();
                }
            }
        } catch (IOException ex)
        {
            ex.printStackTrace();
        }
        return true;
    }
}
