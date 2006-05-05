/*
 * ERectangleTest.java
 * JUnit based test
 *
 * Created on May 4, 2006, 9:26 AM
 */

package com.sun.electric.database.geometry;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import junit.framework.*;

/**
 *
 * @author dn146861
 */
public class ERectangleTest extends TestCase {
    
    ERectangle rect;
    
    public ERectangleTest(String testName) {
        super(testName);
    }

    protected void setUp() throws Exception {
        rect = new ERectangle(100, 100, 10, 20);
    }

    protected void tearDown() throws Exception {
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(ERectangleTest.class);
        
        return suite;
    }

    /**
     * Test of setRect method, of class com.sun.electric.database.geometry.ERectangle.
     */
    public void testSerialization() {
        try {
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(byteStream);
            out.writeObject(rect);
            out.close();
            byte[] serializedRect = byteStream.toByteArray();
            
            ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(serializedRect));
            ERectangle r = (ERectangle)in.readObject();
            in.close();
            
            assertEquals(rect, r);
        } catch (IOException e) {
            fail(e.getMessage());
        } catch (ClassNotFoundException e) {
            fail(e.getMessage());
        }
        
    }
    
}
