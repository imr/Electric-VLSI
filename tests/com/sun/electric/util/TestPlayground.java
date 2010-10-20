package com.sun.electric.util;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class TestPlayground {

    public interface ExtEnum {
        public boolean optional();
    }
    
    public enum ExtEnumee implements ExtEnum {
        test1(), test2(true);

        private boolean opt = false;
        
        ExtEnumee() {
            opt = false;
        }

        ExtEnumee(boolean optional) {
            this.opt = optional;
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.sun.electric.util.TestPlayground.ExtEnum#optional()
         */
        public boolean optional() {
            return opt;

        }

    }

    @Before
    public void printLine() {
        System.out.println("===================================================");
    }

    private enum TestEnum {
        test1, test2;
    }

    @Ignore
    @Test
    public void testEnum() {
        this.deepSearch(TestEnum.class);
    }

    @Test
    public void testUserHome() {
        System.out.println("User Property: " + System.getProperty("user.home"));
    }

    @Ignore
    @Test
    public void printMethodNames() {
        Method[] methods = TestPlayground.class.getMethods();
        for (Method method : methods) {
            System.out.println(method.getName());
        }
    }

    @Ignore
    @Test
    public void testInstance() {
        Number n = new Double(12.3);
        Assert.assertTrue(n instanceof Number);
        Assert.assertTrue(n instanceof Double);

        Class<?> c1 = n.getClass();
        System.out.println(c1);

        Method[] methods = TestPlayground.class.getMethods();
        for (Method method : methods) {
            System.out.println(method.getName() + ": ");
            for (Class<?> c : method.getParameterTypes())
                System.out.println("  parameter: " + c.getName());
        }

    }

    @Ignore
    @Test
    public void testDeepSearch() {
        deepSearch(Double.class);
    }

    public void testFunc(Number n) {

    }

    public void deepSearch(Class<?> clazz) {
        if (clazz == null) {
            return;
        }
        System.out.println(clazz.getName());
        for (Class<?> c : clazz.getInterfaces()) {
            deepSearch(c);
        }

        deepSearch(clazz.getSuperclass());
    }

    @Test
    public void testURI() throws URISyntaxException {
        String uri = "jar:file:/E:/workspaceElectric2/electric-public/target/electric-9.0-SNAPSHOT-bin.jar!/econfig.xml";
        URI tmpUri = new URI(uri);
        System.out.println(tmpUri);
        File file = new File(tmpUri.toString());
        System.out.println(file.exists());
    }
}
