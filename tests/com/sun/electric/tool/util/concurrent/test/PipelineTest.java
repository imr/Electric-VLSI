/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: PipelineTest.java
 *
 * Copyright (c) 2010 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.util.concurrent.test;

import org.junit.Ignore;
import org.junit.Test;

import com.sun.electric.tool.Job;
import com.sun.electric.tool.util.concurrent.runtime.ThreadID;
import com.sun.electric.tool.util.concurrent.runtime.pipeline.PipelineRuntime;
import com.sun.electric.tool.util.concurrent.runtime.pipeline.PipelineRuntime.StageImpl;

/**
 * @author Felix Schmidt
 * 
 */
public class PipelineTest {

    @Ignore
    @Test
    public void testPipelineConstruction() throws InterruptedException {
    	
    	Job.setDebug(true);
    	
        PipelineRuntime<Integer, Integer> testPipe = new PipelineRuntime<Integer, Integer>();
        testPipe.addStage(new SimpleStage(),3);
        testPipe.addStage(new SimpleStage2(), 2);
        for(int i = 0; i < 100; i++) {
        	testPipe.input(i);
        }
        
        Thread.sleep(2000);
        
        testPipe.shutdown();
    }

    public static class SimpleStage extends StageImpl<Integer, Integer> {

        /*
         * (non-Javadoc)
         * 
         * @see
         * com.sun.electric.tool.util.concurrent.runtime.pipeline.PipelineRuntime
         * .StageImpl#execute(java.lang.Object)
         */
        @Override
        public Integer execute(Integer item) {
            int id = ThreadID.get();
            System.out.println("stage 1 - " + id + ": " + item);
            return item + 1;
        }

    }
    
    public static class SimpleStage2 extends StageImpl<Integer, Integer> {

        /*
         * (non-Javadoc)
         * 
         * @see
         * com.sun.electric.tool.util.concurrent.runtime.pipeline.PipelineRuntime
         * .StageImpl#execute(java.lang.Object)
         */
        @Override
        public Integer execute(Integer item) {
            int id = ThreadID.get();
            System.out.println("stage 2 - " + id + ": " + item);
            return item;
        }

    }

}
