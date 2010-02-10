/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: PrefPackage.java
 * Written by: Dmitry Nadezhin, Sun Microsystems.
 *
 * Copyright (c) 2009 Sun Microsystems and Static Free Software
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
package com.sun.electric.database.text;

import com.sun.electric.database.id.ArcProtoId;
import com.sun.electric.database.id.LayerId;
import com.sun.electric.database.id.PrimitiveNodeId;
import com.sun.electric.database.id.TechId;
import com.sun.electric.technology.Technology;

import java.io.Serializable;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.prefs.Preferences;

/**
 * Abstract class to define a package of appearence options.
 * Subclass can define Java annotations on its fields, describing
 * their persistence representation in Java Preferences.
 */
public abstract class PrefPackage implements Serializable, Cloneable {

    protected static final String TECH_NODE = Technology.TECH_NODE;
    protected static final String USER_NODE = "tool/user";

    /**
     * Protected constuctor fills annotated option fields of the subclass from Preferences subtree.
     * Now possible root can be obtained by {@link com.sun.electric.database.text.Pref#getPrefRoot()}
     * and {@link com.sun.electric.database.text.Pref#getFactoryPrefRoot()} methods.
     * @param factory use the Factory Pref root
     */
    protected PrefPackage(boolean factory) {
        this(factory ? getFactoryPrefRoot() : getPrefRoot());
    }

    protected PrefPackage withField(String fieldName, Object value) {
        try {
            PrefPackage that = (PrefPackage) clone();
            Field field = findField(fieldName);
            field.setAccessible(true);
            field.set(that, value);
            return that;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            throw new AssertionError();
        }
    }

    private Field findField(String fieldName) {
        Class cls = getClass();
//        Field fld = null;
        while (cls != PrefPackage.class) {
            try {
                return cls.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                cls = cls.getSuperclass();
            }
        }
        return null;
    }

    /**
     * Protected constuctor fills annotated option fields of the subclass from Preferences subtree.
     * Now possible root can be obtained by {@link com.sun.electric.database.text.Pref#getPrefRoot()}
     * and {@link com.sun.electric.database.text.Pref#getFactoryPrefRoot()} methods.
     * @param prefRoot the root of the Preferences subtree.
     */
    protected PrefPackage(Preferences prefRoot) {
        Class cls = getClass();
        for (Field field : cls.getDeclaredFields()) {
            try {
                BooleanPref ba = field.getAnnotation(BooleanPref.class);
                if (ba != null) {
                    assert field.getType() == Boolean.TYPE;
                    field.setAccessible(true);
                    field.setBoolean(this, prefRoot.node(ba.node()).getBoolean(ba.key(), ba.factory()));
                }
                IntegerPref ia = field.getAnnotation(IntegerPref.class);
                if (ia != null) {
                    assert field.getType() == Integer.TYPE;
                    field.setAccessible(true);
                    field.setInt(this, prefRoot.node(ia.node()).getInt(ia.key(), ia.factory()));
                }
                LongPref la = field.getAnnotation(LongPref.class);
                if (la != null) {
                    assert field.getType() == Long.TYPE;
                    field.setAccessible(true);
                    field.setLong(this, prefRoot.node(la.node()).getLong(la.key(), la.factory()));
                }
                DoublePref da = field.getAnnotation(DoublePref.class);
                if (da != null) {
                    assert field.getType() == Double.TYPE;
                    field.setAccessible(true);
                    field.setDouble(this, prefRoot.node(da.node()).getDouble(da.key(), da.factory()));
                }
                StringPref sa = field.getAnnotation(StringPref.class);
                if (sa != null) {
                    assert field.getType() == String.class;
                    field.setAccessible(true);
                    field.set(this, prefRoot.node(sa.node()).get(sa.key(), sa.factory()));
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Store annotated option fields of the subclass into the speciefied Preferences subtree.
     * @param prefRoot the root of the Preferences subtree.
     * @param removeDefaults remove from the Preferences subtree options which have factory default value.
     */
    protected void putPrefs(Preferences prefRoot, boolean removeDefaults) {
        Class cls = getClass();
        for (Field field : cls.getDeclaredFields()) {
            try {
                BooleanPref ba = field.getAnnotation(BooleanPref.class);
                if (ba != null) {
                    assert field.getType() == Boolean.TYPE;
                    boolean v = field.getBoolean(this);
                    Preferences prefs = prefRoot.node(ba.node());
                    if (removeDefaults && v == ba.factory()) {
                        prefs.remove(ba.key());
                    } else {
                        prefs.putBoolean(ba.key(), v);
                    }
                }
                IntegerPref ia = field.getAnnotation(IntegerPref.class);
                if (ia != null) {
                    assert field.getType() == Integer.TYPE;
                    int v = field.getInt(this);
                    Preferences prefs = prefRoot.node(ia.node());
                    if (removeDefaults && v == ia.factory()) {
                        prefs.remove(ia.key());
                    } else {
                        prefs.putInt(ia.key(), v);
                    }
                }
                LongPref la = field.getAnnotation(LongPref.class);
                if (la != null) {
                    assert field.getType() == Long.TYPE;
                    long v = field.getLong(this);
                    Preferences prefs = prefRoot.node(la.node());
                    if (removeDefaults && v == la.factory()) {
                        prefs.remove(la.key());
                    } else {
                        prefs.putLong(la.key(), v);
                    }
                }
                DoublePref da = field.getAnnotation(DoublePref.class);
                if (da != null) {
                    assert field.getType() == Double.TYPE;
                    double v = field.getDouble(this);
                    Preferences prefs = prefRoot.node(da.node());
                    if (removeDefaults && v == da.factory()) {
                        prefs.remove(da.key());
                    } else {
                        prefs.putDouble(da.key(), v);
                    }
                }
                StringPref sa = field.getAnnotation(StringPref.class);
                if (sa != null) {
                    assert field.getType() == String.class;
                    String v = (String) field.get(this);
                    Preferences prefs = prefRoot.node(sa.node());
                    if (removeDefaults && v.equals(sa.factory())) {
                        prefs.remove(sa.key());
                    } else {
                        prefs.put(sa.key(), v);
                    }
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Store annotated option fields of the subclass into the speciefied Preferences subtree.
     * @param prefRoot the root of the Preferences subtree.
     * @param removeDefaults remove from the Preferences subtree options which have factory default value.
     */
    public static void lowLevelPutPrefs(PrefPackage prefPackage, Preferences prefRoot, boolean removeDefaults) {
        prefPackage.putPrefs(prefRoot, removeDefaults);
    }

    /**
     * Returns the root of Preferences subtree with Electric options.
     * Currently this is "/com/sun/electric/" subtree.
     * @return the root of Preferences subtree with Electric options.
     */
    public static Preferences getPrefRoot() {
        return Pref.getPrefRoot();
    }

    /**
     * Returns the root of dummy Preferences subtree with factory default Electric options.
     * @return the root of Preferences subtree with factory default Electric options.
     */
    public static Preferences getFactoryPrefRoot() {
        return Pref.getFactoryPrefRoot();
    }

    protected String getKey(String what, TechId techId) {
        int len = what.length() + techId.techName.length();
        StringBuilder sb = new StringBuilder(len);
        sb.append(what);
        sb.append(techId.techName);
        assert sb.length() == len;
        return sb.toString();
    }

    protected String getKey(String what, LayerId layerId) {
        int len = what.length() + layerId.fullName.length() + 1;
        StringBuilder sb = new StringBuilder(len);
        sb.append(what);
        sb.append(layerId.name);
        sb.append("IN");
        sb.append(layerId.techId.techName);
        assert sb.length() == len;
        return sb.toString();
    }

    protected String getKey(String what, PrimitiveNodeId pnId) {
        int len = what.length() + pnId.fullName.length() + 4;
        StringBuilder sb = new StringBuilder(len);
        sb.append(what);
        sb.append("For");
        sb.append(pnId.name);
        sb.append("IN");
        sb.append(pnId.techId.techName);
        assert sb.length() == len;
        return sb.toString();
    }

    protected String getKey(String what, ArcProtoId apId) {
        int len = what.length() + apId.fullName.length() + 4;
        StringBuilder sb = new StringBuilder(len);
        sb.append(what);
        sb.append("For");
        sb.append(apId.name);
        sb.append("IN");
        sb.append(apId.techId.techName);
        assert sb.length() == len;
        return sb.toString();
    }

    /**
     * Indicates that a field declaration is intended to keep value of a boolean option.
     */
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    protected @interface BooleanPref {

        /**
         * Preferences node path where the annotated option is stored.
         * The path is relative.
         */
        public String node();

        /**
         * Preferences key with which the annotated option is associated.
         */
        public String key();

        /**
         * Factory default value of the annotated option.
         */
        public boolean factory();
    }

    /**
     * Indicates that a field declaration is intended to keep value of an integer option.
     */
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    protected @interface IntegerPref {

        /**
         * Preferences node path where the annotated option is stored.
         * The path is relative.
         */
        public String node();

        /**
         * Preferences key with which the annotated option is associated.
         */
        public String key();

        /**
         * Factory default value of the annotated option.
         */
        public int factory();
    }

    /**
     * Indicates that a field declaration is intended to keep value of a long option.
     */
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    protected @interface LongPref {

        /**
         * Preferences node path where the annotated option is stored.
         * The path is relative.
         */
        public String node();

        /**
         * Preferences key with which the annotated option is associated.
         */
        public String key();

        /**
         * Factory default value of the annotated option.
         */
        public long factory();
    }

    /**
     * Indicates that a field declaration is intended to keep value of a double option.
     */
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    protected @interface DoublePref {

        /**
         * Preferences node path where the annotated option is stored.
         * The path is relative.
         */
        public String node();

        /**
         * Preferences key with which the annotated option is associated.
         */
        public String key();

        /**
         * Factory default value of the annotated option.
         */
        public double factory();
    }

    /**
     * Indicates that a field declaration is intended to keep value of a String option.
     */
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    protected @interface StringPref {

        /**
         * Preferences node path where the annotated option is stored.
         * The path is relative.
         */
        public String node();

        /**
         * Preferences key with which the annotated option is associated.
         */
        public String key();

        /**
         * Factory default value of the annotated option.
         */
        public String factory();
    }
}
