package com.sun.electric.tool.user.ui;

import java.awt.geom.Point2D;
import java.util.List;
import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: root
 * Date: Oct 12, 2005
 * Time: 11:06:33 PM
 * To change this template use File | Settings | File Templates.
 */

/**
 * This class saves views much like a web browser saves page history.
 * A view here is a particular zoom and pan location of a cell in an
 * EditWindow.  The browser saves old views, and lets you move
 * forward and backward between them.  When a new view is created,
 * all views forward of the current view are destroyed, much like typing
 * in a new URL into a web browser.
 */
public class EditWindowViewBrowser {

    // The associated EditWindow whose views we will remember
    private EditWindow editWindow;

    // The list of saved views
    private List savedViews;

    // A pointer to the current view (which is saved)
    private int currentView;

    // flag to check if call to updateCurrentView was the first
    // call since the last save
    private boolean firstUpdate;

    // maximum number of saved views
    private static final int MAXSAVEDVIEWS = 10;

    private static final boolean DEBUG = false;

    private static class View {
        private final Point2D offset;
        private final double scale;
        private View(Point2D offset, double scale) {
            this.offset = offset;
            this.scale = scale;
        }
        public String toString() {
            return "offset="+offset.getX()+","+offset.getY()+" scale="+scale;
        }
    }

    /**
     * Create a new View Browser associated with the given EditWindow
     * @param controller
     */
    EditWindowViewBrowser(EditWindow controller) {
        this.editWindow = controller;
        savedViews = new ArrayList();
        clear();
    }

    /**
     * Go back to the last saved view
     */
    public void goBack() {
        int previousView = currentView-1;
        if (previousView < 0) {
            System.out.println("No more previous view history");
            return;
        }
        if (previousView >= savedViews.size()) return;

        // first update current view
        updateCurrentView();

        // go to previous view
        View view = (View)savedViews.get(previousView);
        restoreView(view);
        currentView = previousView;
        if (DEBUG) System.out.println("Went back, last saved view is "+currentView+" out of "+savedViews.size());
    }

    /**
     * Go forward to the next saved view
     */
    public void goForward() {
        if (currentView < 0) return;
        int nextView = currentView+1;
        if (nextView >= savedViews.size()) {
            System.out.println("No more forward view history");
            return;
        }

        // first update current view
        updateCurrentView();

        // go to next view
        View view = (View)savedViews.get(nextView);
        restoreView(view);
        currentView = nextView;
        if (DEBUG) System.out.println("Went forward, last saved view is "+currentView+" out of "+savedViews.size());
    }

    /**
     * Restore the edit window to the given view
     * @param view
     */
    private void restoreView(View view) {
        editWindow.setOffset(view.offset);
        editWindow.setScale(view.scale);
        editWindow.repaintContents(null, false);
        if (DEBUG) System.out.println("restored view "+view);
    }

    /**
     * Save the current view. Note that this destroys all
     * views forward of the last saved view.
     */
    void saveCurrentView() {
        saveCurrentViewNoUpdateClear();
        firstUpdate = true;
    }

    // separate method for internal use that does not clear firstUpdate flag
    private void saveCurrentViewNoUpdateClear() {
        View view = new View(editWindow.getOffset(), editWindow.getScale());
        // delete views forward of this view
        while (currentView < savedViews.size()-1) {
            if (savedViews.size() == 0) break;
            savedViews.remove(savedViews.size()-1);
            if (DEBUG) System.out.println("removed old view "+(savedViews.size()-1));
        }
        savedViews.add(view);
        currentView++;
        if (DEBUG) System.out.println("Added new saved view as "+currentView+" out of "+savedViews.size());
        if (DEBUG) System.out.println("saved view "+view);
        // remove old ones if we are over the limit
        if (currentView > MAXSAVEDVIEWS) {
            savedViews.remove(0);
            currentView--;
        }
    }

    /**
     * Clear all saved views
     */
    void clear() {
        currentView = -1;       // no current view
        savedViews.clear();
        firstUpdate = true;
    }

    /**
     * Updates the current view with any changes to panning
     */
    public void updateCurrentView() {
        if (currentView < 0) return;
        if (currentView >= savedViews.size()) return;

        if (firstUpdate) {
            // save current view as a new view
            // call special function that won't reset firstUpdate flag
            saveCurrentViewNoUpdateClear();
            firstUpdate = false;
            return;
        }

        // otherwise, we just update the current view
        View view = new View(editWindow.getOffset(), editWindow.getScale());
        savedViews.set(currentView, view);
    }
}
