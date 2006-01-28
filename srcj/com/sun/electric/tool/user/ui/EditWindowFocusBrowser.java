package com.sun.electric.tool.user.ui;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

/**
 * This class saves foci much like a web browser saves page history.
 * A focus is a particular zoom and pan location of a cell in an EditWindow.
 * The browser saves old foci, and lets you move
 * forward and backward between them.  When a new focus is created,
 * all foci forward of the current one are destroyed, much like typing
 * in a new URL into a web browser.
 */
public class EditWindowFocusBrowser {

    // The associated EditWindow whose foci we will remember
    private EditWindow editWindow;

    // The list of saved foci
    private List<Focus> savedFoci;

    // A pointer to the current focus (which is saved)
    private int currentFocus;

    // flag to check if call to updateCurrentFocus was the first
    // call since the last save
    private boolean firstUpdate;

    // maximum number of saved foci
    private static final int MAXSAVEDFOCI = 10;

    private static final boolean DEBUG = false;

    private static class Focus {
        private final Point2D offset;
        private final double scale;
        private Focus(Point2D offset, double scale) {
            this.offset = offset;
            this.scale = scale;
        }
        public String toString() {
            return "offset="+offset.getX()+","+offset.getY()+" scale="+scale;
        }
    }

    /**
     * Create a new Focus Browser associated with the given EditWindow
     * @param controller
     */
	EditWindowFocusBrowser(EditWindow controller) {
        this.editWindow = controller;
        savedFoci = new ArrayList<Focus>();
        clear();
    }

    /**
     * Go back to the last saved focus
     */
    public void goBack() {
        int previousFocus = currentFocus-1;
        if (previousFocus < 0) {
            System.out.println("No more previous focus history");
            return;
        }
        if (previousFocus >= savedFoci.size()) return;

        // first update current focus
		updateCurrentFocus();

        // go to previous focus
		Focus focus = savedFoci.get(previousFocus);
		restoreFocus(focus);
		currentFocus = previousFocus;
        if (DEBUG) System.out.println("Went back, last saved focus is "+currentFocus+" out of "+savedFoci.size());
    }

    /**
     * Go forward to the next saved focus
     */
    public void goForward() {
        if (currentFocus < 0) return;
        int nextFocus = currentFocus+1;
        if (nextFocus >= savedFoci.size()) {
            System.out.println("No more forward focus history");
            return;
        }

        // first update current focus
		updateCurrentFocus();

        // go to next focus
		Focus focus = savedFoci.get(nextFocus);
		restoreFocus(focus);
		currentFocus = nextFocus;
        if (DEBUG) System.out.println("Went forward, last saved focus is "+currentFocus+" out of "+savedFoci.size());
    }

    /**
     * Restore the edit window to the given focus
     * @param focus
     */
    private void restoreFocus(Focus focus) {
        editWindow.setOffset(focus.offset);
        editWindow.setScale(focus.scale);
        editWindow.repaintContents(null, false);
        if (DEBUG) System.out.println("restored focus "+focus);
    }

    /**
     * Save the current focus. Note that this destroys all
     * foci forward of the last saved focus.
     */
    void saveCurrentFocus() {
		saveCurrentFocusNoUpdateClear();
        firstUpdate = true;
    }

    // separate method for internal use that does not clear firstUpdate flag
    private void saveCurrentFocusNoUpdateClear() {
		Focus focus = new Focus(editWindow.getOffset(), editWindow.getScale());
        // delete foci forward of this focus
        while (currentFocus < savedFoci.size()-1) {
            if (savedFoci.size() == 0) break;
			savedFoci.remove(savedFoci.size()-1);
            if (DEBUG) System.out.println("removed old focus "+(savedFoci.size()-1));
        }
		savedFoci.add(focus);
		currentFocus++;
        if (DEBUG) System.out.println("Added new saved focus as "+currentFocus+" out of "+savedFoci.size());
        if (DEBUG) System.out.println("saved focus "+focus);
        // remove old ones if we are over the limit
        if (currentFocus > MAXSAVEDFOCI) {
			savedFoci.remove(0);
			currentFocus--;
        }
    }

    /**
     * Clear all saved foci
     */
    void clear() {
		currentFocus = -1;       // no current focus
		savedFoci.clear();
        firstUpdate = true;
    }

    /**
     * Updates the current focus with any changes to panning
     */
    public void updateCurrentFocus() {
        if (currentFocus < 0) return;
        if (currentFocus >= savedFoci.size()) return;

        if (firstUpdate) {
            // save current focus as a new focus
            // call special function that won't reset firstUpdate flag
			saveCurrentFocusNoUpdateClear();
            firstUpdate = false;
            return;
        }

        // otherwise, we just update the current focus
		Focus focus = new Focus(editWindow.getOffset(), editWindow.getScale());
		savedFoci.set(currentFocus, focus);
    }
}
