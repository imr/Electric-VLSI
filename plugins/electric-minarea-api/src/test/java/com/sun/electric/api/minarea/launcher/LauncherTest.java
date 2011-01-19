/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.electric.api.minarea.launcher;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author dn146861
 */
public class LauncherTest {

    public LauncherTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of main method, of class Launcher.
     */
    @Test
    public void testMain() {
        System.out.println("main");
        Launcher.main(new String[] {"BasicAreas_CPG.lay", "100000", "com.sun.electric.api.minarea.SimpleChecker"});
        Launcher.main(new String[] {"BasicAreas_CMF.lay", "100000", "com.sun.electric.api.minarea.SimpleChecker"});
        Launcher.main(new String[] {"BasicAreas_CSP.lay", "100000", "com.sun.electric.api.minarea.SimpleChecker"});
        Launcher.main(new String[] {"SimpleHierarchy_CPG.lay", "100000", "com.sun.electric.api.minarea.SimpleChecker"});
        Launcher.main(new String[] {"SimpleHierarchy_CMF.lay", "100000", "com.sun.electric.api.minarea.SimpleChecker"});
        Launcher.main(new String[] {"SimpleHierarchy_CSP.lay", "100000", "com.sun.electric.api.minarea.SimpleChecker"});
    }

}