/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: StreamClient.java
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

import com.sun.electric.database.EObjectInputStream;
import com.sun.electric.database.EditingPreferences;
import com.sun.electric.database.Snapshot;
import com.sun.electric.database.hierarchy.EDatabase;
import com.sun.electric.database.id.IdManager;
import com.sun.electric.database.id.IdWriter;
import com.sun.electric.tool.Client.ServerEvent;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;

/**
 * Class for maintaining Connection on Server side.
 */
public class StreamClient extends Client {
    private final IdWriter writer;
    private Snapshot currentSnapshot = EDatabase.serverDatabase().getInitialSnapshot();
    private final ServerEventDispatcher dispatcher;
    private final ClientReader reader;
    private static final long STACK_SIZE_EVENT = 0/*isOSMac()?0:32*(1 << 10)*/;
    private final static int STACK_SIZE_READER = 0/*isOSMac()?0:32*(1 << 10)*/;

    StreamClient(int connectionId, InputStream inputStream, OutputStream outputStream) {
        super(connectionId);
        writer = new IdWriter(IdManager.stdIdManager, new DataOutputStream(outputStream));
        dispatcher = new ServerEventDispatcher();
        reader = inputStream != null ? new ClientReader(inputStream) : null;
    }

    void start() { dispatcher.start(); }

    class ServerEventDispatcher extends Thread {
        private ServerEvent lastEvent = Client.getQueueTail();

        private ServerEventDispatcher() {
            super(null, null, "Dispatcher-" + connectionId, STACK_SIZE_EVENT);
        }

        @Override
        public void run() {
            try {
                if (reader != null)
                    reader.start();
                writer.writeInt(Job.PROTOCOL_VERSION);
                writer.writeInt(connectionId);
                writeSnapshot(lastEvent);
                for (;;) {
                    writer.flush();
                    lastEvent = Client.getEvent(lastEvent);
                    for (;;) {
                        if (lastEvent.getSnapshot() != currentSnapshot)
                            writeSnapshot(lastEvent);
                        lastEvent.write(writer);
                        if (lastEvent instanceof ShutdownEvent) {
                            writer.close();
                            return;
                        }
                        ServerEvent event = lastEvent.getNext();
                        if (event == null)
                            break;
                        lastEvent = event;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                lastEvent = null;
                Job.serverJobManager.connectionClosed();
            }
        }
    }

    private void writeSnapshot(ServerEvent event) throws IOException {
        writer.writeByte((byte)1);
        writer.writeLong(event.getTimeStamp());
        Snapshot newSnapshot = event.getSnapshot();
        newSnapshot.writeDiffs(writer, currentSnapshot);
        currentSnapshot = newSnapshot;
    }

    private class ClientReader extends Thread {
        private final DataInputStream in;

        private ClientReader(InputStream inputStream) {
            super(null, null, "ClientReader-" + connectionId, STACK_SIZE_READER);
            in = new DataInputStream(new BufferedInputStream(inputStream));
        }

        public void run() {
            try {
                EditingPreferences clientEp = null;
                for (;;) {
                    int tag = in.read();
                    if (tag == -1)
                        break;
                    switch (tag) {
                        case 1:
                            int jobId = in.readInt();
                            Job.Type jobType = Job.Type.valueOf(in.readUTF());
                            String jobName = in.readUTF();
                            int len = in.readInt();
                            byte[] bytes = new byte[len];
                            in.readFully(bytes);
                            EJob ejob = new EJob(StreamClient.this, jobId, jobType, jobName, bytes);
                            ejob.editingPreferences = clientEp;
                            Job.serverJobManager.addJob(ejob, false);
                            break;
                        case 2:
                            byte[] serializedEp = new byte[in.readInt()];
                            in.readFully(serializedEp);
                            try {
                                EDatabase database = EDatabase.serverDatabase();
                                ObjectInputStream in = new EObjectInputStream(new ByteArrayInputStream(serializedEp), database);
                                EditingPreferences ep = (EditingPreferences)in.readObject();
                                in.close();
                                clientEp = ep;
                            } catch (Throwable e) {
                                e.printStackTrace();
                            }
                            break;
                        default:
                            assert false;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
//                dispatcher.interrupt();
            }
        }
    }
}
