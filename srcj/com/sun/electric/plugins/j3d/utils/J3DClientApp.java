package com.sun.electric.plugins.j3d.utils;

import com.sun.electric.tool.Job;
import com.sun.electric.plugins.j3d.ui.J3DViewDialog;
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
    public boolean doIt()
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
