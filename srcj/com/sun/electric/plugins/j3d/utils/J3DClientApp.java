/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: J3DClientApp.java
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

import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.plugins.j3d.ui.J3DViewDialog;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.DatagramPacket;

/**
 * Support class for 3D viewing.
 */
public class J3DClientApp extends Job
{

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
    public boolean doIt() throws JobException
    {
        try
        {
            DatagramSocket socket = new DatagramSocket();
            InetAddress address = InetAddress.getByName(hostname);
            //address = InetAddress.getLocalHost();
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
