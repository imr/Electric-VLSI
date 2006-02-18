/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: EObjectInputStream.java
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

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.EDatabase;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;

/**
 * EObjectInputStream resolves Electric objects in sepcified database from Key objects.
 */
public class EObjectInputStream extends ObjectInputStream {
    
    public abstract static class Key implements Serializable {
        protected abstract Object readResolveInDatabase(EDatabase database) throws InvalidObjectException;
    }
    
    private final EDatabase database;
    
    /** Creates a new instance of EObjectInputStream */
    public EObjectInputStream(InputStream in, EDatabase database) throws IOException {
        super(in);
        enableResolveObject(true);
        this.database = database;
    }
    
    protected Object resolveObject(Object obj) throws IOException {
        if (obj instanceof Key)
            return ((Key)obj).readResolveInDatabase(database);
        return obj;
    }
}
