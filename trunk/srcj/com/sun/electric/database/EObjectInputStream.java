/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: EObjectInputStream.java
 * Written by: Dmitry Nadezhin, Sun Microsystems.
 *
 * Copyright (c) 2003 Sun Microsystems and Static Free Software
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
package com.sun.electric.database;

import com.sun.electric.database.hierarchy.EDatabase;
import com.sun.electric.database.id.IdManager;

import java.io.Externalizable;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;

/**
 * EObjectInputStream resolves Electric objects in specified database from Key objects.
 */
public class EObjectInputStream extends ObjectInputStream {

    public abstract static class Key<T> implements Externalizable {

        private T obj;

        public Key(T obj) {
            this.obj = obj;
        }

        public void writeExternal(ObjectOutput out) throws IOException {
            writeExternal((EObjectOutputStream) out, obj);
        }

        public Key() {
        }

        public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
            obj = readExternal((EObjectInputStream) in);
        }

        /**
         * The class implements the writeExternal method to save an object
         * by calling the methods of DataOutput for its primitive values or
         * calling the writeObject method of ObjectOutput for objects, strings,
         * and arrays. This method can get context by methods
         * of EObjectOutputStream like getDatabase and getIdManager.
         *
         * @serialData Overriding methods should use this tag to describe
         *             the data layout of this Externalizable object.
         *             List the sequence of element types and, if possible,
         *             relate the element to a public/protected field and/or
         *             method of this Externalizable class.
         *
         * @param out the EObjectOutputStream stream to write the object to
         * @param obj object to save
         * @exception IOException Includes any I/O exceptions that may occur
         */
        public abstract void writeExternal(EObjectOutputStream out, T obj) throws IOException;

        /**
         * The oclass implements the readExternal method to restore an
         * object by calling the methods of DataInput for primitive
         * types and readObject for objects, strings and arrays.  The
         * readExternal method must read the values in the same sequence
         * and with the same types as were written by writeExternal.
         * This method can get context by methods
         * of EObjectOutputStream like getDatabase and getIdManager.
         *
         * @param in the stream to read data from in order to restore the object
         * @return restored object
         * @exception IOException if I/O errors occur
         * @exception ClassNotFoundException If the class for an object being
         *              restored cannot be found.
         */
        public abstract T readExternal(EObjectInputStream in) throws IOException, ClassNotFoundException;
    }
    private final EDatabase database;

    /** Creates a new instance of EObjectInputStream */
    public EObjectInputStream(InputStream in, EDatabase database) throws IOException {
        super(in);
        enableResolveObject(true);
        this.database = database;
    }

    protected Object resolveObject(Object obj) throws IOException {
        if (obj instanceof Key) {
            return ((Key) obj).obj;
        }
        return obj;
    }

    public EDatabase getDatabase() {
        return database;
    }

    public IdManager getIdManager() {
        return database.getIdManager();
    }
}
