/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: DebugMenuJonG.java
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
package com.sun.electric.tool.user.menus;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.EDatabase;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.logicaleffort.LENetlister1;
import com.sun.electric.tool.user.CircuitChanges;
import com.sun.electric.tool.user.Clipboard;
import com.sun.electric.tool.user.Highlight2;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.dialogs.ExecDialog;
import com.sun.electric.tool.user.menus.MenuCommands.EMenu;
import com.sun.electric.tool.user.menus.MenuCommands.EMenuItem;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.ui.WindowFrame;
import com.sun.electric.tool.user.ui.ZoomAndPanListener;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Random;

/**
 * Jon's TEST MENU
 */
public class DebugMenuJonG {

    static EMenu makeMenu() {
        return new EMenu("_JonG",
            new EMenuItem("Describe Vars") { public void run() {
                listVarsOnObject(false); }},
            new EMenuItem("Describe Proto Vars") { public void run() {
                listVarsOnObject(true); }},
            new EMenuItem("Describe Current Library Vars") { public void run() {
                listLibVars(); }},
            new EMenuItem("Eval Vars") { public void run() {
                evalVarsOnObject(); }},
            new EMenuItem("LE test1") { public void run() {
                LENetlister1.test1(); }},
            new EMenuItem("Display shaker") { public void run() {
                shakeDisplay(); }},
            new EMenuItem("Run command") { public void run() {
                runCommand(); }},
            new EMenuItem("Start defunct Job") { public void run() {
                startDefunctJob(); }},
            new EMenuItem("Add String var") { public void run() {
                addStringVar(); }},
            new EMenuItem("Edit clipboard") { public void run() {
                Clipboard.editClipboard(); }},
            new EMenuItem("Cause stack overflow") { public void run() {
                causeStackOverflow(true, false, "blah", 234, "xvsdf"); }},
            new EMenuItem("Cause stack overflow in Job") { public void run() {
                causeStackOverflowJob(); }},
            new EMenuItem("Time method calls") { public void run() {
                timeMethodCalls(); }},
            new EMenuItem("Delete layout cells in current library") { public void run() {
                deleteCells(View.LAYOUT); }},
            Technology.getTSMC90Technology() != null ? new EMenuItem("fill generator 90nm test") { public void run() {
				    invokeTSMC90FillGenerator(); }} : null);
    }
    
	// ---------------------- THE JON GAINSLEY MENU -----------------

	private static void invokeTSMC90FillGenerator()
	{
		try
		{
			Class tsmc90FillGeneratorClass = Class.forName("com.sun.electric.plugins.tsmc.fill90nm.FillGenerator90");
			Class [] parameterTypes = new Class[] {};
			Method testMethod = tsmc90FillGeneratorClass.getDeclaredMethod("test", parameterTypes);
			testMethod.invoke(null, new Object[] {});
 		} catch (Exception e)
        {
 			System.out.println("ERROR invoking the Fill Generator test");
        }
	}

	public static void listVarsOnObject(boolean useproto) {
        EditWindow wnd = EditWindow.getCurrent();
        if (wnd == null) return;
		if (wnd.getHighlighter().getNumHighlights() == 0) {
			// list vars on cell
			WindowFrame wf = WindowFrame.getCurrentWindowFrame();
			if (wf == null) return;
            Cell cell = wf.getContent().getCell();
            cell.getInfo();
			return;
		}
		for (Highlight2 h : wnd.getHighlighter().getHighlights()) {
			if (!h.isHighlightEOBJ()) continue;
			ElectricObject eobj = h.getElectricObject();
            if (eobj instanceof PortInst) {
                PortInst pi = (PortInst)eobj;
                pi.getInfo();
                eobj = pi.getNodeInst();
            }
			if (eobj instanceof NodeInst) {
				NodeInst ni = (NodeInst)eobj;
				if (useproto) {
					System.out.println("using prototype");
					if (ni.isCellInstance())
						((Cell)ni.getProto()).getInfo();
				} else {
					ni.getInfo();
				}
			}
		}
	}

	public static void evalVarsOnObject() {
		EditWindow curEdit = EditWindow.needCurrent();
        if (curEdit == null) return;

		if (curEdit.getHighlighter().getNumHighlights() == 0) return;
		for (Highlight2 h : curEdit.getHighlighter().getHighlights()) {
            if (!h.isHighlightEOBJ()) continue;
			ElectricObject eobj = h.getElectricObject();
			Iterator<Variable> itVar = eobj.getVariables();
			while(itVar.hasNext()) {
				Variable var = itVar.next();
				Object obj = curEdit.getVarContext().evalVar(var);
				System.out.print(var.getKey().getName() + ": ");
				System.out.println(obj);
			}
		}
	}

	public static void listLibVars() {
		Library lib = Library.getCurrent();
		Iterator<Variable> itVar = lib.getVariables();
		System.out.println("----------"+lib+" Vars-----------");
		while(itVar.hasNext()) {
			Variable var = itVar.next();
			Object obj = VarContext.globalContext.evalVar(var);
			System.out.println(var.getKey().getName() + ": " +obj);
		}
	}

    public static void addStringVar() {
        EditWindow wnd = EditWindow.needCurrent();
        if (wnd == null) return;

        if (wnd.getHighlighter().getNumHighlights() == 0) return;
        for (Highlight2 h : wnd.getHighlighter().getHighlights()) {
            if (h.isHighlightEOBJ()) {
                ElectricObject eobj = h.getElectricObject();
                AddStringVar job = new AddStringVar(eobj);
                break;
            }
        }
    }

    private static class AddStringVar extends Job {
        private ElectricObject eobj;

        private AddStringVar(ElectricObject eobj) {
            super("AddStringVar", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
            this.eobj = eobj;
            startJob();
        }

        public boolean doIt() throws JobException {
            eobj.newVar("ATTR_XXX", "1");
            System.out.println("Added var ATTR_XXX as String \"1\"");
            return true;
        }
    }


    public static void causeStackOverflow(boolean x, boolean y, String l, int r, String f) {
        // this will cause a stack overflow
        causeStackOverflow(x, y, l, r, f);
    }

    public static void causeStackOverflowJob() {
        StackOverflowJob job = new StackOverflowJob();
    }

	private static class StackOverflowJob extends Job {
        private StackOverflowJob() {
            super("overflow", User.getUserTool(), Job.Type.EXAMINE, null, null, Job.Priority.USER);
            startJob();
        }
        public boolean doIt() throws JobException {
            dosomething(true, "asfjka;dj");
            return true;
        }
        private void dosomething(boolean b, String str) {
            dosomething(b, str);
        }
    }

    public static void shakeDisplay() {
        //RedisplayTest job = new RedisplayTest(50);
        //RedrawTest test = new RedrawTest();
        long startTime = System.currentTimeMillis();

        EditWindow wnd = EditWindow.getCurrent();
        for (int i=0; i<100; i++) {
            //wnd.redrawTestOnly();
            //doWait();
        }
        long endTime = System.currentTimeMillis();

        StringBuffer buf = new StringBuffer();
        Date start = new Date(startTime);
        buf.append("  start time: "+start+"\n");
        Date end = new Date(endTime);
        buf.append("  end time: "+end+"\n");
        long time = endTime - startTime;
        buf.append("  time taken: "+TextUtils.getElapsedTime(time)+"\n");
        System.out.println(buf.toString());

    }

    private static class RedrawTest extends Job {
        private RedrawTest() {
            super("RedrawTest", User.getUserTool(), Job.Type.EXAMINE, null, null, Job.Priority.USER);
            startJob();
        }

        public boolean doIt() throws JobException {
            long startTime = System.currentTimeMillis();
            for (int i=0; i<100; i++) {
                if (getScheduledToAbort()) return false;
                //wnd.redrawTestOnly();
                //doWait();
            }
            long endTime = System.currentTimeMillis();

            StringBuffer buf = new StringBuffer();
            Date start = new Date(startTime);
            buf.append("  start time: "+start+"\n");
            Date end = new Date(endTime);
            buf.append("  end time: "+end+"\n");
            long time = endTime - startTime;
            buf.append("  time taken: "+TextUtils.getElapsedTime(time)+"\n");
            System.out.println(buf.toString());

            return true;
        }

        private void doWait() {
            try {
                boolean donesleeping = false;
                while (!donesleeping) {
                    Thread.sleep(100);
                    donesleeping = true;
                }
            } catch (InterruptedException e) {}
        }
    }

    private static class RedisplayTest extends Job {

        private long delayTimeMS;

        private RedisplayTest(long delayTimeMS) {
            super("RedisplayTest", User.getUserTool(), Job.Type.EXAMINE, null, null, Job.Priority.USER);
            this.delayTimeMS = delayTimeMS;
            startJob();
        }

        public boolean doIt() throws JobException {
            Random rand = new Random(143137493);

            for (int i=0; i<200; i++) {
                if (getScheduledToAbort()) return false;

                WindowFrame wf = WindowFrame.getCurrentWindowFrame();
                //int next = rand.nextInt(4);
                int next = i % 4;
                switch(next) {
                    case 0: { ZoomAndPanListener.panXOrY(0, wf, 1); break; }
                    case 1: { ZoomAndPanListener.panXOrY(1, wf, 1); break; }
                    case 2: { ZoomAndPanListener.panXOrY(0, wf, -1); break; }
                    case 3: { ZoomAndPanListener.panXOrY(1, wf, -1); break; }
                }
                doWait();
            }
            System.out.println(getInfo());
            return true;
        }

        private void doWait() {
            try {
                boolean donesleeping = false;
                while (!donesleeping) {
                    Thread.sleep(delayTimeMS);
                    donesleeping = true;
                }
            } catch (InterruptedException e) {}
        }
    }

    public static void startDefunctJob() {
        DefunctJob j = new DefunctJob();
    }

    private static ArrayList<Object> sharedList = new ArrayList<Object>();

    private static void changeSharedList() {
        //if (sharedList.size() < 100) sharedList.add(new Integer(sharedList.size()));
        //else sharedList.remove(sharedList.size()-1);
        Object o = sharedList.get(0);
    }

    private static class DefunctJob extends Job {

        public DefunctJob() {
            super("Defunct Job", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
            startJob();
        }

        public boolean doIt() throws JobException {
            while (true) {
                changeSharedList();
            }
        }
    }

    private static class TestObject {
        private int count;
        private final Object mutex;
        private TestObject() {
            mutex = new Object();
            count = 0;
        }
        private final int getCount() { return count; }
        private synchronized int getCountSync() { return count; }
        private int getCountExamineCheck() {
            EDatabase.theDatabase.checkExamine();
            return count;
        }
        private int getCountExamineLock() {
            Job.acquireExamineLock(false);
            try {
                Job.releaseExamineLock();
            } catch (Error e) {
                Job.releaseExamineLock();                
            }
            return count;
        }
        private int getCountJob() {
            CountJob job = new CountJob(mutex);
            synchronized(mutex) {
                job.startJob(false, true);
                try {
                    mutex.wait();
                } catch (InterruptedException e) {}
            }
            return count;
        }

        private static class CountJob extends Job {
            private final Object mutex;

            private CountJob(Object mutex) {
                super("CountJob", User.getUserTool(), Job.Type.EXAMINE, null, null, Job.Priority.USER);
                this.mutex = mutex;
            }

            public boolean doIt() throws JobException {
                synchronized(mutex) { mutex.notify(); }
                return true;
            }
        }
    }

    public static void timeMethodCalls() {
        TestObject obj = new TestObject();
        int limit = 500000;

        long start = System.currentTimeMillis();
        for (int i=0; i<limit; i++) {
            obj.getCount();
        }
        System.out.println("Baseline case: "+TextUtils.getElapsedTime(System.currentTimeMillis()-start));

        start = System.currentTimeMillis();
        for (int i=0; i<limit; i++) {
            obj.getCountSync();
        }
        System.out.println("Synchronized case: "+TextUtils.getElapsedTime(System.currentTimeMillis()-start));

        start = System.currentTimeMillis();
        for (int i=0; i<limit; i++) {
            obj.getCountExamineCheck();
        }
        System.out.println("Checking case (no sync): "+TextUtils.getElapsedTime(System.currentTimeMillis()-start));

        start = System.currentTimeMillis();
        for (int i=0; i<limit; i++) {
            obj.getCountExamineLock();
        }
        System.out.println("Locking case (no sync): "+TextUtils.getElapsedTime(System.currentTimeMillis()-start));

        start = System.currentTimeMillis();
        for (int i=0; i<limit; i++) {
            obj.getCountJob();
        }
        System.out.println("Job case: "+TextUtils.getElapsedTime(System.currentTimeMillis()-start));
    }

    public static void runCommand() {
        ExecDialog d = new ExecDialog(TopLevel.getCurrentJFrame(), false);
        File dir = new File("/home/gainsley");
        d.startProcess("/bin/tcsh", null, dir);
    }

    public static void deleteCells(View view) {
        Library lib = Library.getCurrent();
        int deleted = 0;
        int notDeleted = 0;
        for (Iterator<Cell> it = lib.getCells(); it.hasNext(); ) {
            Cell cell = it.next();
            if (cell.getView() != view) continue;
            if (CircuitChanges.deleteCell(cell, false, false))
                deleted++;
            else
                notDeleted++;
        }
        System.out.println("Deleted: "+deleted);
        System.out.println("Not deleted: "+ notDeleted);
    }
}
