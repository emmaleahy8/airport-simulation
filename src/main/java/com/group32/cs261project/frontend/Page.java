package com.group32.cs261project.frontend;

import javafx.scene.Parent;
/**
 * Interface representing a navigable page in the application.
 *
 * Each page provides a JavaFX view and lifecycle hooks that allow the
 * application to notify the page when navigation occurs.
 *
 * Pages are managed by the main application controller, which switches
 * between them based on the current {@link AppState}.
 */
public interface Page {
    // Returns the root JavaFX node representing this page.
    Parent getView();

    // Called when the application navigates to this page.
    void onEnter(Object data);
    
    // Called when the app navigates away.
    default void onExit() {}
}