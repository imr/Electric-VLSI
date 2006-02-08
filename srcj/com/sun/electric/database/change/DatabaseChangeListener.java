/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: DatabaseChangeListener.java
 *
 * Copyright (c) 2004 Sun Microsystems and Static Free Software
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

import java.util.EventListener;


/**
 * The database changes its state in transactional manner.
 * When the state is changed, DatabaseChangeEvent occurs.
 * Any class that implements the DatabaseChangeListener interface, and is added
 * to the Undo class's listeners will receive DatabaseChangeEvent.
 * <P>
 * <B>IMPORTANT:</B> The listener should be a Swing object,
 * and it is invoked in the java AWT Event thread.
 */
// /**
//  * The Undo class handles logging and broadcasting of changes to the database.
//  * Any class that implements the DatabaseChangeListener interface, and is added
//  * to the Undo class's listeners will receive change events.
//  * <P>
//  * <B>IMPORTANT:</B> If the listener is a Swing object, or will modify Swing objects
//  * as a result of a database change, you will want to return <i>true</i> for your
//  * implementation of method 'isGUIListener'.  This is because Swing is not thread safe,
//  * so care must be taken when the database thread generats event that will cause modification
//  * of Swing objects.  Otherwise, deadlock is possible.
//  */
public interface DatabaseChangeListener extends EventListener {

    /**
     * Invoked in the java AWT Event thread when the database state changes.
     * @param e database change event.
     */
    public void databaseChanged(DatabaseChangeEvent e);

//     /**
//      * Called when a batch of changes has been completed by a Tool.
//      * The batch contains all the Change events done for that batch.
//      * It is recommened that casual listeners use this method to listen for changes.
//      * @param batch a batch of changes.
//      */
//     public void databaseEndChangeBatch(Undo.ChangeBatch batch);

//     /**
//      * Called every time a change is made. It is not recommened you
//      * use this method unless (a) you need fine-grained change notification,
//      * and (b) you know what you're doing.  Also, currently if your
//      * isGUIListener() returns true, you will not be notified of these
//      * events.  This is because you will be notified via SwingUtils.invokeLater,
//      * which means you probably won't get the event until after everything has
//      * finished.
//      * @param evt the change event.
//      */
//     public void databaseChanged(Undo.Change evt);


//     /**
//      * It is very important that you take care to implement this method properly.
//      * If the listener is a Swing component, or will modify Swing components as a result
//      * of a database change, this method should return true.  Otherwise, it should return
//      * false.  This is because Swing is not thread safe, so the database thread must
//      * take care when generating change events that will modify Swing components. All
//      * Swing component modification must take place in the java AWT Event thread.
//      * @return true if the listener is a Swing component, or will modify Swing
//      * components as a result of a database change event.  False otherwise.
//      */
//     public boolean isGUIListener();
}
