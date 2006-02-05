/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: DumpHeap.java
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

package com.sun.electric.database;

import com.sun.electric.database.geometry.GenMath;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Class to dump JVM heap.
 */
public class DumpHeap {
    private static final boolean REFERENCES = false;
    
    private int[] objHash = new int[1];
    private ArrayList<Object> objs = (new ArrayList<Object>());
    { objs.add(null); }
    private HashMap<Class,ClassDescriptor> classes = new HashMap<Class,ClassDescriptor>();
    
    private DumpHeap() {}
    
    /**
     * Method to dump JVM heap.
     */
    public static void dump(String fileName)
    {
        try {
            System.gc();
            DumpHeap dumpHeap = new DumpHeap();
            dumpHeap.handler(ClassLoader.class);
            dumpHeap.sweeps(100);
            DataOutputStream s = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(fileName)));
            try {
                dumpHeap.write(s);
            } finally {
                s.close();
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private ClassDescriptor classDescriptorOf(Class cls) {
        ClassDescriptor cd = (ClassDescriptor)classes.get(cls);
        if (cd == null) {
            cd = new ClassDescriptor(cls);
            classes.put(cls, cd);
        }
        return cd;
    }
   
    private int handler(Object obj) { return handler(obj, true); }
    private int handler0(Object obj) { return handler(obj, false); }
    
    private int handler(Object obj, boolean create) {
        if (obj == null) return 0;
        int i = System.identityHashCode(obj) & 0x7FFFFFFF;
        i %= objHash.length;
        for (int j = 1; objHash[i] != 0; j += 2) {
            Object o = objs.get(objHash[i]);
            if (o == obj) return objHash[i];
            i += j;
            if (i >= objHash.length) i -= objHash.length;
        }
        if (!create) return 0;
        
        if (objs.size()*2 <= objHash.length - 3) {
            objHash[i] = objs.size();
            objs.add(obj);
            return i;
        }
        
        rehash();
        return handler(obj);
    }
    
    void rehash() {
        int newSize = objs.size()*2 + 3;
        if (newSize < 0) throw new IndexOutOfBoundsException();
        int[] newObjHash = new int[GenMath.primeSince(newSize)];
        for (int k = 0; k < objs.size(); k++) {
            Object obj = objs.get(k);
            int i = System.identityHashCode(obj) & 0x7FFFFFFF;
            i %= newObjHash.length;
            for (int j = 1; newObjHash[i] != 0; j += 2) {
                assert objs.get(newObjHash[i]) != obj;
                i += j;
                if (i >= newObjHash.length) i -= newObjHash.length;
            }
            newObjHash[i] = k;
        }
        objHash = newObjHash;
    }
    
    private void sweep()
        throws SecurityException, IllegalAccessException
    {
        for (int scanned = 1; scanned < objs.size(); scanned++) {
            Object obj = objs.get(scanned);
            
            handler(obj.getClass());

            if (obj instanceof Object[]) {
                Object[] array = (Object[])obj;
                for (int i = 0; i < array.length; i++)
                    handler(array[i]);
            } else if (obj instanceof Collection) {
                Collection coll = (Collection)obj;
                for (Iterator it = coll.iterator(); it.hasNext(); )
                    handler(it.next());
            } else if (obj instanceof Map) {
                Map map = (Map)obj;
                for (Iterator it = map.entrySet().iterator(); it.hasNext(); ) {
                    Map.Entry entry = (Map.Entry)it.next();
                    handler(entry.getKey());
                    handler(entry.getValue());
                }
            } else if (obj instanceof String) {
            } else {
                Class cls = obj.getClass();
                ClassDescriptor cd = classDescriptorOf(cls);

                for (int i = 0; i < cd.fields.length; i++) {
                    Field f = cd.fields[i];
                    handler(f.get(obj));
                }
                if (obj instanceof Class) {
                    cls = (Class)obj;
                    cd = classDescriptorOf(cls);
                    
                    handler(cls.getComponentType());
                    handler(cls.getSuperclass());
//                    handler(cls.getEnclosingClass());
                    for (int i = 0; i < cd.staticFields.length; i++) {
                        Field f = cd.staticFields[i];
                        handler(f.get(null));
                    }
                }
            }
        }
    }
    
    private void reflectClass(Class cls)
        throws SecurityException, IllegalAccessException
    {
       
        Field[] fields = cls.getDeclaredFields();
        for (int i = 0; i < fields.length; i++) {
            Field f = fields[i];
            int fm = f.getModifiers();
            if (!Modifier.isStatic(fm)) continue;
            Class tf = f.getType();
            if (tf.isPrimitive()) continue;
            f.setAccessible(true);
            Object value = f.get(null);
            handler(value);
       }
    }
    
    private void sweeps(int maxSweep)
        throws SecurityException, IllegalAccessException
    {
        int numSweep = 0;
        for (;;) {
            int numObjects = objs.size();
            sweep();
            if (++numSweep >= maxSweep) break;
            if (objs.size() == numObjects) break;
        }
        System.out.println(numSweep + " sweeps");
    }
    
    private void write(DataOutputStream out)
        throws IOException, IllegalAccessException
    {
        int numObjs = objs.size() - 1;
        out.writeInt(numObjs);
        for (Iterator it = classes.values().iterator(); it.hasNext(); ) {
            ClassDescriptor cd = (ClassDescriptor)it.next();
            Class cls = cd.cls;
            int h = handler0(cd.cls);
            assert h != 0;
            out.writeInt(h);
            out.writeUTF(cd.cls.getName());
            int mode;
            if (cls.isArray() && !cls.getComponentType().isPrimitive()) {
                mode = MyClass.ARRAY;
            } else if (Collection.class.isAssignableFrom(cls)) {
                mode = MyClass.ARRAY;
            } else if (Map.class.isAssignableFrom(cls)) {
                mode = MyClass.MAP;
            } else if (cls == String.class) {
                mode = MyClass.STRING;
            } else if (cls == Class.class) {
                mode = MyClass.CLASS;
            } else {
                mode = MyClass.NORMAL;
            }
            out.writeByte(mode);
            out.writeInt(cd.staticFields.length);
            for (int i = 0; i < cd.staticFields.length; i++)
                out.writeUTF(cd.staticFields[i].getName());
            out.writeInt(cd.fields.length);
            for (int i = 0; i < cd.fields.length; i++)
                out.writeUTF(cd.fields[i].getName());
        }
        out.writeInt(0);
        
        for (int h = 1; h <= numObjs; h++) {
            Object obj = objs.get(h);
            if (obj instanceof String) {
                out.writeInt(h);
                out.writeUTF((String)obj);
            }
        }
        out.writeInt(0);
        
        for (int h = 1; h <= numObjs; h++) {
            Object obj = objs.get(h);
            out.writeInt(handler0(obj.getClass()));
            if (obj instanceof Object[]) {
                Object[] array = (Object[])obj;
                out.writeInt(array.length);
                for (int i = 0; i < array.length; i++)
                    out.writeInt(handler0(array[i]));
            } else if (obj instanceof Collection) {
                Collection coll = (Collection)obj;
                int length = coll.size();
                out.writeInt(length);
                int i = 0;
                for (Iterator it = coll.iterator(); i < length && it.hasNext(); i++)
                    out.writeInt(handler0(it.next()));
                while (i < length) out.writeInt(0);
            } else if (obj instanceof Map) {
                Map map = (Map)obj;
                int length = 0;
                out.writeInt(length);
                int i = 0;
                for (Iterator it = map.entrySet().iterator(); i < length && it.hasNext(); i++) {
                    Map.Entry entry = (Map.Entry)it.next();
                    out.writeInt(handler0(entry.getKey()));
                    out.writeInt(handler0(entry.getValue()));
                }
                while (i < length) { out.writeInt(0); out.writeInt(0); }
            } else if (obj instanceof String) {
            } else {
                Class cls = obj.getClass();
                ClassDescriptor cd = classDescriptorOf(cls);

                for (int i = 0; i < cd.fields.length; i++) {
                    Field f = cd.fields[i];
                    out.writeInt(handler0(f.get(obj)));
                }
                if (obj instanceof Class) {
                    cd = classDescriptorOf((Class)obj);
                    for (int i = 0; i < cd.staticFields.length; i++) {
                        Field f = cd.staticFields[i];
                        out.writeInt(handler0(f.get(null)));
                    }
                }
            }
        }
        out.writeInt(0);
    }
    
    private class ClassDescriptor implements Serializable {
        private final Class cls;
        private final Field[] fields;
        private final Field[] staticFields;
        private int numObjects;
        
        private ClassDescriptor(Class cls) {
            this.cls = cls;
            ArrayList<Field> fieldList = new ArrayList<Field>();
            ArrayList<Field> staticFieldList = new ArrayList<Field>();
            Class superCls = cls.getSuperclass();
            if (superCls != null)
                fieldList.addAll(Arrays.asList(classDescriptorOf(superCls).fields));
            Field[] flds;
            try {
                flds = cls.getDeclaredFields();
            } catch (NoClassDefFoundError e) {
                System.out.println("Can't getDeclaredFields in " + cls);
                flds = new Field[0];
            }
            for (int i = 0; i < flds.length; i++) {
                Field f = flds[i];
                Class tf = f.getType();
                if (tf.isPrimitive()) continue;
                if (!REFERENCES && Reference.class.isAssignableFrom(cls) && f.getName().equals("referent"))
                    continue;
                f.setAccessible(true);
                int fm = f.getModifiers();
                if (Modifier.isStatic(fm))
                    staticFieldList.add(f);
                else
                    fieldList.add(f);
            }
            Field[] NULL_FIELD_ARRAY = {};
            this.fields = (Field[])fieldList.toArray(NULL_FIELD_ARRAY);
            this.staticFields = (Field[])staticFieldList.toArray(NULL_FIELD_ARRAY);
        }
   }
 

}
