package com.sun.electric.plugins.j3d.utils;

import java.io.*;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.DatagramPacket;

/**
 * Created by IntelliJ IDEA.
 * User: Gilda
 * Date: Feb 24, 2005
 * Time: 5:58:39 PM
 * To change this template use File | Settings | File Templates.
 */
public class J3DServerApp
{
    public static void main (String args[])
    {
        // By default reads from standard input
        BufferedReader lineReader = new BufferedReader(new InputStreamReader(System.in));
        long timeout = 1;

        // Name is given as -name=<filename>
        for (int i = 0; i < args.length; i++)
        {
            if (args[i].startsWith("-h"))
            {
                System.out.println("J3DServerApp: Usage 'java J3DServerApp [options]'");
                System.out.println("\tOptions:");
                System.out.println("\t\t-name=<filename>: filename containing the data");
                System.out.println("\t\t-time=<time>: waiting time between lines. Default 0");
                System.exit(0);
            }
            if (args[i].startsWith("-time="))
            {
                String time = args[i].substring(6);
                timeout = Long.parseLong(time);
            }

            if (args[i].startsWith("-name="))
            {
                String fileName = args[i].substring(6);
                try
                {
                    lineReader = new LineNumberReader(new FileReader(fileName));
                } catch (Exception e)
                {
                    e.printStackTrace();
                    lineReader = null;
                }
            }
        }

        try
        {
            DatagramSocket socket = new DatagramSocket(2345);
            String localAddress = InetAddress.getLocalHost().getHostName().trim();
            int localPort = socket.getLocalPort();
            System.out.println(localAddress + ":");
            System.out.println("Capacitance Server is listening on port " + localPort + ".");
            int bufferLenght = 256;
            byte outBuffer[];
            byte inBuffer[] = new byte[bufferLenght];
            DatagramPacket outDatagram;
            DatagramPacket inDatagram = new DatagramPacket(inBuffer, inBuffer.length);
            boolean finished = false;

            do {
                socket.receive(inDatagram);
                InetAddress destAddress = inDatagram.getAddress();
                String destHost = destAddress.getHostName().trim();
                int destPort = inDatagram.getPort();
                System.out.println("Received a datagram from " + destHost + " at port" + destPort);
                String inData = new String(inDatagram.getData()).trim();
                System.out.println("It contained data '" + inData + "'");
                if (inData.equalsIgnoreCase("quit")) finished = true;
                String outData = lineReader.readLine();
                try {lineReader.wait(timeout);} catch (Exception e) {e.printStackTrace();}
                System.out.println(outData);
                if (outData != null)
                {
                    outBuffer = outData.getBytes();
                    outDatagram = new DatagramPacket(outBuffer, outBuffer.length, destAddress, destPort);
                    socket.send(outDatagram);
                    System.out.println("Sent '" + outData + "' to " + destHost + " at port " + destPort);
                }
            } while (!finished);
        } catch (IOException ex)
        {
            ex.printStackTrace();
        }
        finally
        {
            System.out.println("Quitting");
        }
    }
}
