package com.sun.electric.plugins.j3d;

import java.io.IOException;
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
public class J3DClientApp
{
    public static void main (String args[])
    {
        try
        {
            DatagramSocket socket = new DatagramSocket();
            InetAddress localAddress = InetAddress.getLocalHost();
            String localHost = localAddress.getHostName();
            int bufferLenght = 256;
            byte outBuffer[];
            byte inBuffer[] = new byte[bufferLenght];
            DatagramPacket outDatagram;
            DatagramPacket inDatagram = new DatagramPacket(inBuffer, inBuffer.length);

            for (int i = 0; i < 5; i++)
            {
                outBuffer = new byte[bufferLenght];
                outBuffer = "time".getBytes();
                outDatagram = new DatagramPacket(outBuffer, outBuffer.length, localAddress, 2345);
                socket.send(outDatagram);
                System.out.println("Sent time request to " + localHost + " at port 2345");
                socket.receive(inDatagram);
            }
            int localPort = socket.getLocalPort();
            System.out.println(localAddress + ":");
            System.out.println("Capacitance Server is listening on port " + localPort + ".");
            boolean finished = false;
            do {
                socket.receive(inDatagram);
                InetAddress destAddress = inDatagram.getAddress();
                String destHost = destAddress.getHostName().trim();

            } while (!finished);
        } catch (IOException ex)
        {
            ex.printStackTrace();
        }
    }
}
