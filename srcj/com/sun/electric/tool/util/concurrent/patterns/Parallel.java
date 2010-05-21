/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.electric.tool.util.concurrent.patterns;

import com.sun.electric.tool.util.concurrent.patterns.PForJob.BlockedRange;

/**
 *
 * @author fs239085
 */
public class Parallel {

    /**
     * Parallel For Loop (1- and 2-dimensional)
     * @param range
     * @param task
     */
    public static void For(BlockedRange range, Class<? extends PForTask> task) {
        (new PForJob(range, task)).execute();
    }

}
