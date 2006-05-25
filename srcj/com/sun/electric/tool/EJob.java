/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: EJob.java
 *
 * Copyright (c) 2003 Sun Microsystems and Static Free Software
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

import com.sun.electric.database.EObjectInputStream;
import com.sun.electric.database.EObjectOutputStream;
import com.sun.electric.database.Snapshot;
import com.sun.electric.database.hierarchy.EDatabase;
import com.sun.electric.tool.Job.Type;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.logging.Level;

/**
 * Class to track Job serializing and execution.
 */
class EJob {
    
    enum State {
        /** waiting on client */                CLIENT_WAITING,
        /** waiting on server */                WAITING,
        /** running on server */                RUNNING,
        /** done on server */                   SERVER_DONE,
        /** done on client */                   CLIENT_DONE;
    };
    
    /*private*/ final static String WAITING_NOW = "waiting now";
    /*private*/ final static String ABORTING = "aborting";
    
    /** Client which is owner of the Job. */    Client client;
    /** True if this Job was started by server */boolean startedByServer;
    int jobId;
    /** type of job (change or examine) */      final Type jobType;
    /** name of job */                          final String jobName;
    
    Snapshot oldSnapshot;
    Snapshot newSnapshot;
    
    /** progress */                             /*private*/ String progress = null;
    byte[] serializedJob;
    byte[] serializedResult;
    Job serverJob;
    Job clientJob;
    State state;
    /** list of saved Highlights */             int savedHighlights = -1;
    /** Fields changed on server side. */       ArrayList<Field> changedFields;
    
    /** Creates a new instance of EJob */
    EJob(StreamClient connection, int jobId, Job.Type jobType, String jobName, byte[] bytes) {
        this.client = connection;
        this.jobId = jobId;
        this.jobType = jobType;
        this.jobName = jobName;
        state = State.WAITING;
        serializedJob = bytes;
    }
    
    EJob(Job job, Job.Type jobType, String jobName) {
        this.jobType = jobType;
        this.jobName = jobName;
        state = State.CLIENT_WAITING;
        serverJob = clientJob = job;
    }
    
    Job getJob() { return clientJob != null ? clientJob : serverJob; }
    
    boolean isExamine() {
        return jobType == Job.Type.EXAMINE || jobType == Job.Type.REMOTE_EXAMINE;
    }   
    
    Throwable serialize(EDatabase database) {
        try {
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            ObjectOutputStream out = new EObjectOutputStream(byteStream, database);
            out.writeObject(clientJob);
            out.flush();
            serializedJob = byteStream.toByteArray();
            return null;
        } catch (Throwable e) {
//        	if (e instanceof NotSerializableException)
        	{
//        		NotSerializableException nse = (NotSerializableException)e;
        		System.out.println("ERROR: Job '" + jobName + "' cannot serialize parameter: " + e.getMessage());
        		System.out.println("------------- Begin serialize() Exception stack trace --------------\n");
    			e.printStackTrace();
        		System.out.println("------------- End serialize() Exception stack trace ----------------\n");
        	}
            return e;
        }
    }
    
    Throwable deserializeToServer() {
        try {
            ObjectInputStream in = new EObjectInputStream(new ByteArrayInputStream(serializedJob), EDatabase.serverDatabase());
            Job job = (Job)in.readObject();
            in.close();
            job.ejob = this;
            serverJob = job;
            return null;
        } catch (Throwable e) {
            return e;
        }
    }
    
    Throwable deserializeToClient() {
        try {
            ObjectInputStream in = new EObjectInputStream(new ByteArrayInputStream(serializedJob), EDatabase.clientDatabase());
            Job job = (Job)in.readObject();
            in.close();
            job.ejob = this;
            clientJob = job;
            return null;
        } catch (Throwable e) {
            return e;
        }
    }
    
    void serializeResult(EDatabase database) {
        try {
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            ObjectOutputStream out = new EObjectOutputStream(byteStream, database);
            out.writeObject(null); // No exception
            out.writeInt(changedFields.size());
            Job job = jobType == Job.Type.EXAMINE ? clientJob : serverJob;
            for (Field f: changedFields) {
                String fieldName = f.getName();
                Object value = f.get(job);
                out.writeUTF(fieldName);
                try {
                    out.writeObject(value);
                } catch (NotSerializableException e) {
                    System.out.println("ERROR: Job '" + jobName + "' cannot serialize returned field " +
                            fieldName + " = " + value + " : " + e.getMessage());
                    throw e;
                }
            }
            out.close();
            serializedResult = byteStream.toByteArray();
        } catch (Throwable e) {
            Job.logger.logp(Level.WARNING, getClass().getName(), "serializeResult", "failure", e);
            serializeExceptionResult(e, database);
        }
    }
    
    void serializeExceptionResult(Throwable jobException, EDatabase database) {
        try {
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            ObjectOutputStream out = new EObjectOutputStream(byteStream, database);
            jobException.getStackTrace();
                out.writeObject(jobException);
            out.writeInt(0);
            out.close();
            serializedResult = byteStream.toByteArray();
        } catch (Throwable e) {
            Job.logger.logp(Level.WARNING, getClass().getName(), "serializeExceptionResult", "failure", e);
            serializedResult = new byte[0];
        }
    }

        
    Throwable deserializeResult() {
        try {
            Class jobClass = clientJob.getClass();
            ObjectInputStream in = new EObjectInputStream(new ByteArrayInputStream(serializedResult), EDatabase.clientDatabase());
            Throwable jobException = (Throwable)in.readObject();
            int numFields = in.readInt();
            for (int i = 0; i < numFields; i++) {
                String fieldName = in.readUTF();
                Object value = in.readObject();
                Field f = findField(fieldName);
                f.setAccessible(true);
                f.set(clientJob, value);
            }
            in.close();
            return jobException;
        } catch (Throwable e) {
            Job.logger.logp(Level.WARNING, getClass().getName(), "deserializeResult", "failure", e);
            return e;
        }
    }
    
    /**
     * Method to remember that a field variable of the Job has been changed by the doIt() method.
     * @param fieldName the name of the variable that changed.
     */
    protected void fieldVariableChanged(String fieldName) {
        Field fld = findField(fieldName);
        fld.setAccessible(true);
        changedFields.add(fld);
    }
    
    private Field findField(String fieldName) {
        Class jobClass = getJob().getClass();
        Field fld = null;
        while (jobClass != Job.class) {
            try {
                return jobClass.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                jobClass = jobClass.getSuperclass();
            }
        }
        return null;
    }
}
