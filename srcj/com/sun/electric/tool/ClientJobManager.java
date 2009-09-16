/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ClientJobManager.java
 *
 * Copyright (c) 2003 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool;

import com.sun.electric.database.EObjectOutputStream;
import com.sun.electric.database.EditingPreferences;
import com.sun.electric.database.Snapshot;
import com.sun.electric.database.hierarchy.EDatabase;
import com.sun.electric.database.id.IdManager;
import com.sun.electric.database.id.IdReader;
import com.sun.electric.tool.user.UserInterfaceMain;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.net.Socket;
import java.util.ArrayList;
import javax.swing.SwingUtilities;

/**
 *
 */
class ClientJobManager {
    /** stream for cleint read Snapshots. */    private final IdReader reader;
    /** stream for cleint to send Jobs. */      private final DataOutputStream clientOutputStream;
    /** Process that launched this. */          private final Process process;

    private EditingPreferences currentEp = new EditingPreferences(true, IdManager.stdIdManager.getInitialTechPool());
    private boolean skipOneLine;

    /** Creates a new instance of ClientJobManager */
    public ClientJobManager(String serverMachineName, int serverPort) throws IOException {
        process = null;
        System.out.println("Attempting to connect to port " + serverPort + " ...");
        Socket socket = new Socket(serverMachineName, serverPort);
        reader = new IdReader(new DataInputStream(new BufferedInputStream(socket.getInputStream())), IdManager.stdIdManager);
        clientOutputStream = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
    }

    public ClientJobManager(Process process, boolean skipOneLine) throws IOException {
        this.process = process;
        this.skipOneLine = skipOneLine;
        System.out.println("Attempting to connect to server subprocess ...");
        reader = new IdReader(new DataInputStream(new BufferedInputStream(process.getInputStream())), IdManager.stdIdManager);
        clientOutputStream = new DataOutputStream(new BufferedOutputStream(process.getOutputStream()));
    }

    void writeEJob(EJob ejob) throws IOException {
        writeEditingPreferences();
        clientOutputStream.writeByte((byte)1);
        clientOutputStream.writeInt(ejob.jobKey.jobId);
        clientOutputStream.writeUTF(ejob.jobType.toString());
        clientOutputStream.writeUTF(ejob.jobName);
        clientOutputStream.writeInt(ejob.serializedJob.length);
        clientOutputStream.write(ejob.serializedJob);
        clientOutputStream.flush();
    }

    private void writeEditingPreferences() throws IOException {
        EditingPreferences ep = UserInterfaceMain.getEditingPreferences();
        if (ep == currentEp) return;
        currentEp = ep;
        byte[] serializedEp;
        try {
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            ObjectOutputStream out = new EObjectOutputStream(byteStream, EDatabase.clientDatabase());
            out.writeObject(ep);
            out.flush();
            serializedEp = byteStream.toByteArray();
        } catch (Throwable e) {
            e.printStackTrace();
            return;
        }
        clientOutputStream.writeByte((byte)2);
        clientOutputStream.writeInt(serializedEp.length);
        clientOutputStream.write(serializedEp);
    }

    public void runLoop(final Job initialJob) {
        Snapshot oldSnapshot = EDatabase.clientDatabase().getInitialSnapshot();
        Snapshot currentSnapshot = EDatabase.clientDatabase().backup();
        assert currentSnapshot == oldSnapshot;
        try {
            if (skipOneLine) {
                for (int i = 0; i < 150; i++) {
                    char ch = (char)reader.readByte();
                    if (ch == '\n') break;
                    System.err.print(ch);
                }
                System.err.println();
            }

            int protocolVersion = reader.readInt();
            if (protocolVersion != Job.PROTOCOL_VERSION) {
                System.err.println("Client's protocol version " + Job.PROTOCOL_VERSION + " is incompatible with Server's protocol version " + protocolVersion);
                System.exit(1);
            }
            int connectionId = reader.readInt();
            Job.currentUI.patchConnectionId(connectionId);
            System.out.println("Connected id="+connectionId);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        Job.currentUI.startDispatcher();

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                initialJob.startJob();
            }
        });

        for (;;) {
            try {
                byte tag = reader.readByte();
                long timeStamp = reader.readLong();
                if (tag == 1) {
                    currentSnapshot = Snapshot.readSnapshot(reader, currentSnapshot);
                } else {
                    Client.ServerEvent serverEvent = Client.read(reader, tag, timeStamp, Job.currentUI, currentSnapshot);
                    Client.putEvent(serverEvent);
                }
            } catch (IOException e) {
                // reader.in.close();
//                reader = null;
                System.out.println("END OF FILE reading from server");
                if (process != null)
                    printErrorStream(process);
                return;
            }
        }
    }

    private static void printErrorStream(Process process) {
        try {
            process.getOutputStream().close();
            InputStream errStream = new BufferedInputStream(process.getErrorStream());
            System.err.println("StdErr:");
            for (;;) {
                if (errStream.available() == 0) break;
                int c = errStream.read();
                if (c < 0) break;
                System.err.print((char)c);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
