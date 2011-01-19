/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.electric.plugins.minarea.deltamerge0;

import com.sun.electric.api.minarea.launcher.Launcher;
import com.sun.electric.api.minarea.LayoutCell;
import com.sun.electric.api.minarea.MinAreaChecker.ErrorLogger;
import java.util.Properties;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author dn146861
 */
public class SimpleCheckerTest {

    public SimpleCheckerTest() {
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
     * Test of getAlgorithmName method, of class SimpleChecker.
     */
    @Test
    public void testGetAlgorithmName() {
        System.out.println("getAlgorithmName");
        SimpleChecker instance = new SimpleChecker();
        String expResult = "DeltaMerge0";
        String result = instance.getAlgorithmName();
        assertEquals(expResult, result);
    }

    /**
     * Test of getDefaultParameters method, of class SimpleChecker.
     */
    @Test
    public void testGetDefaultParameters() {
        System.out.println("getDefaultParameters");
        SimpleChecker instance = new SimpleChecker();
        Properties expResult = new Properties();
        Properties result = instance.getDefaultParameters();
        assertEquals(expResult, result);
    }

    @Test
    public void testLauncher() {
        Launcher.main(new String[] {"BasicAreas_CPG.lay", "1000", "com.sun.electric.plugins.minarea.deltamerge0.SimpleChecker"});
    }
    
    /**
     * Test of check method, of class SimpleChecker.
     */
    @Ignore
    public void testCheck() {
        System.out.println("check");
        LayoutCell topCell = null;
        long minArea = 0L;
        Properties parameters = null;
        ErrorLogger errorLogger = null;
        SimpleChecker instance = new SimpleChecker();
        instance.check(topCell, minArea, parameters, errorLogger);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

}