package com.sun.electric.plugins.j3d;

import com.sun.electric.database.text.TextUtils;
import com.sun.electric.tool.Job;

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
    public static double covertToDegrees(double radiant)
    {
        return ((180*radiant)/Math.PI);
    }

    public static double convertToRadiant(double degrees)
    {
        return ((Math.PI*degrees)/180);
    }

    public static void parseValues(String line, int lineNumner, double[] values)
    {
        int count = 0;
        StringTokenizer parse = new StringTokenizer(line, " ", false);
        while (parse.hasMoreTokens())
        {
            String value = parse.nextToken();
            if (count > 8)
            {
                System.out.println("Error reading capacitance file in line " + lineNumner);
                break;
            }
            values[count] = TextUtils.atof(value);
            if (2 < count && count < 6 )
                values[count] = convertToRadiant(values[count]);   // original value is in degrees
            count++;
        }
        if (count != 9)
        {
            System.out.println("Error reading capacitance file in line " + lineNumner);
        }
    }

    private J3DViewDialog dialog;
    private String hostname;

    public J3DClientApp(J3DViewDialog dialog, String hostname)
    {
        super("Socket Connection", null, Job.Type.EXAMINE, null, null, Job.Priority.ANALYSIS);
        this.dialog = dialog;
        this.hostname = hostname;
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
                outBuffer = "request".getBytes();
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
                    dialog.socketAction(inData);
            }
        } catch (IOException ex)
        {
            ex.printStackTrace();
        }
        return true;
    }
}
