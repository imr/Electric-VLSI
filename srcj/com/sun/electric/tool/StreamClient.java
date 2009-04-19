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

import com.sun.electric.database.Snapshot;
import com.sun.electric.database.hierarchy.EDatabase;
import com.sun.electric.database.id.IdManager;
import com.sun.electric.database.id.IdWriter;
import com.sun.electric.tool.Client.ServerEvent;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Class for maintaining Connection on Server side.
 */
public class StreamClient extends Client {
    private final IdWriter writer;
    private Snapshot currentSnapshot = EDatabase.serverDatabase().getInitialSnapshot();
    private final ServerEventDispatcher dispatcher;
    private final ClientReader reader;
    private static final long STACK_SIZE_EVENT = isOSMac()?0:20*(1 << 10);
    private final static int STACK_SIZE_READER = isOSMac()?0:1024;

    StreamClient(int connectionId, InputStream inputStream, OutputStream outputStream) {
        super(connectionId);
        writer = new IdWriter(IdManager.stdIdManager, new DataOutputStream(outputStream));
        dispatcher = new ServerEventDispatcher();
        reader = inputStream != null ? new ClientReader(inputStream) : null;
    }

    void start() { dispatcher.start(); }

    class ServerEventDispatcher extends Thread {
        private ServerEvent lastEvent = getQueueTail();

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
                writeSnapshot(lastEvent.getSnapshot());
                writer.flush();
                for (;;) {
                    lastEvent = getEvent(lastEvent);
                    lastEvent.dispatch(StreamClient.this);
                    writer.flush();
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                Job.jobManager.connectionClosed();
            }
        }
    }

    @Override
    protected void consume(EJobEvent e) throws Exception {
        EJob ejob = e.ejob;
        if (ejob.newSnapshot != null && ejob.newSnapshot != currentSnapshot) {
            writeSnapshot(ejob.newSnapshot);
        }
        assert e.newState == EJob.State.SERVER_DONE;
        writer.writeByte((byte)2);
        writer.writeInt(ejob.jobKey.jobId);
        writer.writeString(ejob.jobName);
        writer.writeString(ejob.jobType.toString());
        writer.writeString(e.newState.toString());
        writer.writeLong(e.timeStamp);
        if (e.newState == EJob.State.WAITING) {
            writer.writeBoolean(ejob.serializedJob != null);
            if (ejob.serializedJob != null)
                writer.writeBytes(ejob.serializedJob);
        }
        if (e.newState == EJob.State.SERVER_DONE)
            writer.writeBytes(ejob.serializedResult);
    }

    @Override
    protected void consume(PrintEvent e) throws Exception {
        writer.writeByte((byte)3);
        writer.writeString(e.s);
    }

    @Override
    protected void consume(SavePrintEvent e) throws Exception {
        writer.writeByte((byte)5);
        String filePath = e.filePath;
        if (filePath == null)
            filePath = "";
        writer.writeString(filePath);
    }

    @Override
    protected void consume(ShowMessageEvent e) throws Exception {
        writer.writeByte((byte)6);
        writer.writeString(e.message);
        writer.writeString(e.title);
        writer.writeBoolean(e.isError);
    }

    @Override
    protected void consume(JobQueueEvent e) throws Exception {
        writer.writeByte((byte)4);
        writer.writeInt(e.jobQueue.length);
        for (Job.Inform j: e.jobQueue)
            j.write(writer);
    }

    private void writeSnapshot(Snapshot newSnapshot) throws IOException {
        writer.writeByte((byte)1);
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
                for (;;) {
                    int jobId = in.readInt();
                    Job.Type jobType = Job.Type.valueOf(in.readUTF());
                    String jobName = in.readUTF();
                    int len = in.readInt();
                    byte[] bytes = new byte[len];
                    in.readFully(bytes);
                    Job.jobManager.addJob(new EJob(StreamClient.this, jobId, jobType, jobName, bytes), false);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                dispatcher.interrupt();
            }
        }
    }
}
