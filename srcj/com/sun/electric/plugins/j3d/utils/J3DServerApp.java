/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: J3DServerApp.java
 * Written by Gilda Garreton, Sun Microsystems.
 *
 * Copyright (c) 2005 Sun Microsystems and Static Free Software
 *
 * Electric(tm) is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
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

package com.sun.electric.plugins.j3d.utils;

import java.io.*;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.DatagramPacket;

/**
 * Support class for 3D viewing.
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
