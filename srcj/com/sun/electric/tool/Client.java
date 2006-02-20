/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Client.java
 *
 * Copyright (c) 2006 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool;

import com.sun.electric.database.change.Undo;
import com.sun.electric.tool.user.User;

import java.io.IOException;

/**
 * 
 */
public abstract class Client {
    final int connectionId;
    final String userName = System.getProperty("user.name");
    
    /** Creates a new instance of AbstractClient */
    public Client(int connectionId) {
        this.connectionId = connectionId;
    }
   
    public static class ServerEvent implements Runnable {
        ServerEvent next;
                
        ServerEvent() {}
        
        public void run() {}
        void dispatchOnStreamClient(StreamClient client) throws IOException {}
    }
    
    static void fireEJobEvent(EJob ejob) {
        fireServerEvent(new EJobEvent(ejob, ejob.state));
    }
    
    static void print(Client client, String s) {
        fireServerEvent(new PrintEvent(client, s));
    }
    
    private static void fireServerEvent(ServerEvent serverEvent) {
        if (Job.currentUI != null)
            Job.currentUI.addEvent(serverEvent);
        StreamClient.addEvent(serverEvent);
    }
    
    static class EJobEvent extends ServerEvent {
        private final EJob ejob;
        private final EJob.State newState;
        
        private EJobEvent(EJob ejob, EJob.State newState) {
            this.ejob = ejob;
            this.newState = newState;
        }
        
        public void run() {
            if (newState == EJob.State.SERVER_DONE && ejob.client == Job.getExtendedUserInterface()) {
                boolean undoRedo = ejob.jobType == Job.Type.UNDO;
                Job.getExtendedUserInterface().showSnapshot(ejob.newSnapshot, undoRedo);
                if (!undoRedo)
                    Undo.endChanges(ejob.oldSnapshot, ejob.getJob().tool, ejob.jobName, ejob.savedHighlights, ejob.newSnapshot);
                
                Throwable jobException = null;
                if (ejob.startedByServer)
                    jobException = ejob.deserializeToClient();
                else if (ejob.jobType != Job.Type.EXAMINE)
                    jobException = ejob.deserializeResult();
                
                Job job = ejob.clientJob;
                try {
                    job.terminateIt(jobException);
                } catch (Throwable e) {
                    System.out.println("Exception executing terminateIt");
                    e.printStackTrace(System.out);
                }
                job.endTime = System.currentTimeMillis();
                job.finished = true;                        // is this redundant with Thread.isAlive()?
                
                // say something if it took more than a minute by default
                if (job.reportExecution || (job.endTime - job.startTime) >= Job.MIN_NUM_SECONDS) {
                    
                    if (User.isBeepAfterLongJobs())
                        Job.getExtendedUserInterface().beep();
                    System.out.println(job.getInfo());
                }
            }
        }
        
        void dispatchOnStreamClient(StreamClient client) throws IOException {
            client.writeEJobEvent(ejob, newState);
        }
    }
    
    static class PrintEvent extends ServerEvent {
        private final Client client;
        private final String s;
        
        private PrintEvent(Client client, String s) {
            this.client = client;
            this.s = s;
        }
        
        public void run() {
        }
        
        void dispatchOnStreamClient(StreamClient client) throws IOException {
            if (client == this.client) // What about protocol logs ?
                client.writeString(s);
        }
    }
    
}
