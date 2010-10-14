package com.sun.electric.util;

import java.lang.reflect.Method;

import junit.framework.Assert;

import org.junit.Test;

public class TestPlayground {

    @Test
    public void testUserHome() {
        System.out.println(System.getProperty("user.home"));
    }

    @Test
    public void printMethodNames() {
        Method[] methods = TestPlayground.class.getMethods();
        for (Method method : methods) {
            System.out.println(method.getName());
        }
    }

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

    @Test
    public void testDeepSearch() {
        deepSearch(Double.class);
    }

    public void testFunc(Number n) {

    }

    public void deepSearch(Class<?> clazz) {
        if(clazz == null) {
            return;
        }
        System.out.println(clazz.getName());
        for (Class<?> c : clazz.getInterfaces()) {
            deepSearch(c);
        }

        deepSearch(clazz.getSuperclass());
    }

}
