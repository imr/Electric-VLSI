/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Crash.java
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

import com.sun.electric.database.text.Version;
import com.sun.electric.database.EditingPreferences;
import com.sun.electric.database.Environment;
import com.sun.electric.database.constraint.Constraints;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.EDatabase;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.id.IdManager;
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.text.Setting;
import com.sun.electric.technology.TechFactory;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.generator.layout.LayoutLib;
import com.sun.electric.tool.user.ActivityLogger;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.tool.user.MessagesStream;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

/**
 *
 */
public class Crash {

    private static class CrashJob extends Job {

        protected CrashJob() {
            super("CrashTest", null, Job.Type.CHANGE, null, null, Job.Priority.USER);
        }

        public boolean doIt() throws JobException {
            String regressionname = "qFourP2-electric-final-jelib";
            String libname = "qFourP2.jelib";
            String cellname = "qFourP1top";
            String rootPath = "../../";
            boolean caching = true;

            String logname = "output/" + libname + "_" + cellname + "_LE_" + (caching ? "C" : "NC") + "-" + Version.getVersion() + ".log";

            MessagesStream.getMessagesStream().save(logname);

            Library rootLib = LayoutLib.openLibForRead(rootPath + "data/" + regressionname + "/" + libname);
            ErrorLogger repairLogger = ErrorLogger.newInstance("Repair Libraries");
            for (Iterator it = Library.getLibraries(); it.hasNext();) {
                Library lib = (Library) it.next();
                lib.checkAndRepair(true, repairLogger);
            }

            System.out.println("Repair Libraries: " + repairLogger.getNumErrors() + " errors," + repairLogger.getNumWarnings() + " warnings");
            Cell lay = rootLib.findNodeProto(cellname + "{sch}");
            System.out.println("Cell = " + lay);
            System.exit(0);
            return true;
        }
    }

    private static EditingPreferences initDatabase() {
        Job.setDebug(true);
        ActivityLogger.initialize("electricserver", true, true, true/*false*/, true, false);
//        Job.threadSafe = true;
        Pref.forbidPreferences();
        EDatabase database = new EDatabase(IdManager.stdIdManager.getInitialEnvironment());
        EDatabase.setServerDatabase(database);
        Tool.initAllTools();

        EditingPreferences ep = new EditingPreferences(true, database.getTechPool());

        // Init Job
        Job.setUserInterface(new UserInterfaceInitial(database));
        database.lock(true);
        database.lowLevelBeginChanging(null);
        database.setToolSettings((Setting.RootGroup) ToolSettings.getToolSettings(""));
        assert database.getGeneric() == null;
        Generic generic = Generic.newInstance(database.getIdManager());
        database.addTech(generic);
        for (TechFactory techFactory : TechFactory.getKnownTechs("").values()) {
            Map<TechFactory.Param, Object> paramValues = Collections.emptyMap();
            Technology tech = techFactory.newInstance(generic, paramValues);
            if (tech != null) {
                database.addTech(tech);
            }
        }
        database.lowLevelEndChanging();
        database.unlock();
        return ep;
    }

    // 5 failures
    private static void crash1() {
        EditingPreferences ep = initDatabase();

        Job.serverJobManager = new ServerJobManager();
        int connectionId = 0;

        Job job = new CrashJob();
        job.ejob.jobKey = new Job.Key(connectionId, 2, true);
        job.ejob.client = Job.serverJobManager.serverConnections.get(0);
        job.ejob.editingPreferences = ep;
        Job.serverJobManager.addJob(job.ejob, false);
    }

    // 1 failure
    private static void crash2() {
        EditingPreferences ep = initDatabase();

        Job.serverJobManager = new ServerJobManager();
        int connectionId = 0;

        Job job = new CrashJob();
        EJob ejob = job.ejob;
        ejob.jobKey = new Job.Key(connectionId, 2, true);
        ejob.client = Job.serverJobManager.serverConnections.get(0);
        ejob.editingPreferences = ep;
//        Job.serverJobManager.addJob(job.ejob, false);

        EDatabase database = EDatabase.serverDatabase();
        Environment.setThreadEnvironment(database.getEnvironment());
        EditingPreferences.setThreadEditingPreferences(ejob.editingPreferences);
        ServerJobManager.UserInterfaceRedirect userInterface = new ServerJobManager.UserInterfaceRedirect(ejob.jobKey);
        Job.setUserInterface(userInterface);
        database.lock(!ejob.isExamine());
        ejob.oldSnapshot = database.backup();
        database.lowLevelBeginChanging(ejob.serverJob.tool);
        Constraints.getCurrent().startBatch(ejob.oldSnapshot);
        userInterface.setCurrents(ejob.serverJob);
        try {
            if (!ejob.serverJob.doIt()) {
                throw new JobException("Job '" + ejob.jobName + "' failed");
            }
        } catch (JobException e) {
            e.printStackTrace();
        }
    }

    // 1 failure
    private static void crash3() {
        EditingPreferences ep = initDatabase();

        Job.serverJobManager = new ServerJobManager();
        int connectionId = 0;

        Job job = new CrashJob();
        EJob ejob = job.ejob;
        ejob.jobKey = new Job.Key(connectionId, 2, true);
        ejob.client = Job.serverJobManager.serverConnections.get(0);
        ejob.editingPreferences = ep;
//        Job.serverJobManager.addJob(job.ejob, false);

        EDatabase database = EDatabase.serverDatabase();
        Environment.setThreadEnvironment(database.getEnvironment());
        EditingPreferences.setThreadEditingPreferences(ejob.editingPreferences);
        ServerJobManager.UserInterfaceRedirect userInterface = new ServerJobManager.UserInterfaceRedirect(ejob.jobKey);
        Job.setUserInterface(userInterface);
        database.lock(!ejob.isExamine());
        ejob.oldSnapshot = database.backup();
        database.lowLevelBeginChanging(ejob.serverJob.tool);
//        Constraints.getCurrent().startBatch(ejob.oldSnapshot);
//        userInterface.setCurrents(ejob.serverJob);
        try {
            if (!ejob.serverJob.doIt()) {
                throw new JobException("Job '" + ejob.jobName + "' failed");
            }
        } catch (JobException e) {
            e.printStackTrace();
        }
    }

    // 0 failures
    // 0 failures
    private static void crash4() {
        EditingPreferences ep = initDatabase();

        Job.serverJobManager = new ServerJobManager();
        int connectionId = 0;

        Job job = new CrashJob();
        EJob ejob = job.ejob;
        ejob.jobKey = new Job.Key(connectionId, 2, true);
        ejob.client = Job.serverJobManager.serverConnections.get(0);
        ejob.editingPreferences = ep;
//        Job.serverJobManager.addJob(job.ejob, false);

        EDatabase database = EDatabase.serverDatabase();
        Environment.setThreadEnvironment(database.getEnvironment());
        EditingPreferences.setThreadEditingPreferences(ejob.editingPreferences);
        ServerJobManager.UserInterfaceRedirect userInterface = new ServerJobManager.UserInterfaceRedirect(ejob.jobKey);
        Job.setUserInterface(userInterface);
        database.lock(!ejob.isExamine());
        ejob.oldSnapshot = database.backup();
        database.lowLevelBeginChanging(ejob.serverJob.tool);
//        database.getNetworkManager().startBatch();
//        Constraints.getCurrent().startBatch(ejob.oldSnapshot);
//        userInterface.setCurrents(ejob.serverJob);
        try {
            if (!ejob.serverJob.doIt()) {
                throw new JobException("Job '" + ejob.jobName + "' failed");
            }
        } catch (JobException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        crash4();
    }
}
