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
