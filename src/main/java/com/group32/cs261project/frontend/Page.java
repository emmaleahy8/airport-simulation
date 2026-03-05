package com.group32.cs261project.frontend;

import javafx.scene.Parent;

public interface Page {
    Parent getView();
    // Called when the app navigates to this page
    void onEnter(Object data);
    
    // Called when the app navigates away (optional).
    default void onExit() {}
}