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
public class J3DServerApp
{
    public static void main (String args[])
    {
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
                System.out.println("Received a datagram from " + destHost + "at port" + destPort);
                String data = new String(inDatagram.getData()).trim();
                System.out.println("It contained data " + data);
            } while (!finished);
        } catch (IOException ex)
        {
            ex.printStackTrace();
        }
    }
}
