/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: DatabaseChangeListener.java
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
package com.sun.electric.database.change;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: root
 * Date: Jun 11, 2004
 * Time: 11:43:12 PM
 * <p>
 * The Undo class handles logging and broadcasting of changes to the database.
 * Any class that implements the DatabaseChangeListener interface, and is added
 * to the Undo class's listeners will receive change events.
 */
public interface DatabaseChangeListener {

    /**
     * Called when a batch of changes has been completed by a Tool.
     * The batch contains all the Change events done for that batch.
     * It is recommened that casual listeners use this method to listen for changes.
     * @param batch a batch of changes.
     */
    public void databaseEndChangeBatch(Undo.ChangeBatch batch);

    /**
     * Called every time a change is made. It is not recommened you
     * use this method unless (a) you need fine-grained change notification,
     * and (b) you know what you're doing.
     * @param evt the change event.
     */
    public void databaseChanged(Undo.Change evt);

}
