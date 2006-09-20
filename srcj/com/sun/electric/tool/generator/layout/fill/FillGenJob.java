/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: FillGenJob.java
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
package com.sun.electric.tool.generator.layout.fill;

import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.text.TextUtils;

/**
 * Created by IntelliJ IDEA.
 * User: gg151869
 * Date: Sep 19, 2006
 * Time: 2:52:39 PM
 * To change this template use File | Settings | File Templates.
 */
public class FillGenJob extends Job
{

    protected FillGenConfig fillGenConfig;
    protected Cell topCell;
    protected ErrorLogger log;
    private boolean doItNow;

    public FillGenJob(Cell cell, FillGenConfig gen, boolean doItNow)
    {
        super("Fill generator job", null, Type.CHANGE, null, null, Priority.USER);
        this.fillGenConfig = gen;
        this.topCell = cell; // Only if 1 cell is generated.
        this.doItNow = doItNow;

        assert(fillGenConfig.evenLayersHorizontal);

        if (doItNow) // call from regressions
        {
            try
            {
                if (doIt())
                    terminateOK();

            } catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        else
            startJob(); // queue the job
    }

    public ErrorLogger getLogger() { return log; }

    public Library getAutoFilLibrary()
    {
        return Library.findLibrary(fillGenConfig.fillLibName);
    }

    public void terminateOK()
    {
        log.termLogging(false);
        long endTime = System.currentTimeMillis();
        int errorCount = log.getNumErrors();
        int warnCount = log.getNumWarnings();
        System.out.println(errorCount + " errors and " + warnCount + " warnings found (took " + TextUtils.getElapsedTime(endTime - startTime) + ")");
    }

    protected FillGeneratorTool setUpJob()
    {
        FillGeneratorTool fillGen = FillGeneratorTool.getTool();
        fillGen.setConfig(fillGenConfig);

        // logger must be created in server otherwise it won't return the elements.
        log = ErrorLogger.newInstance("Fill");
        if (!doItNow)
            fieldVariableChanged("log");

        fillGenConfig.job = this; // to abort job.
        return fillGen;
    }

    /**
     * Implementation of Job.doIt() running only the doTemplateFill function. GNU version
     * @return true if ran without errors.
     */
    public boolean doIt()
    {
        FillGeneratorTool fillGen = setUpJob();
        return doTemplateFill(fillGen);
    }

    public boolean doTemplateFill(FillGeneratorTool fillGen)
    {
        fillGen.standardMakeFillCell(fillGenConfig.firstLayer, fillGenConfig.lastLayer, fillGenConfig.perim,
                fillGenConfig.cellTiles, false);
        fillGen.makeGallery();
        return true;
    }

}
