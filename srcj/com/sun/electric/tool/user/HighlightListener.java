package com.sun.electric.tool.user;

/**
 * Created by IntelliJ IDEA.
 * User: root
 * Date: Jun 11, 2004
 * Time: 8:34:45 PM
 * To change this template use File | Settings | File Templates.
 */
public interface HighlightListener {

    /**
     * Called by the Highlight manager when highlights have changed
     * and will be updated on the screen.
     */
    public void highlightChanged();
}
