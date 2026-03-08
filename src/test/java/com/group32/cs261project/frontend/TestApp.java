package com.group32.cs261project.frontend;

import com.group32.cs261project.App;

public class TestApp extends App {

    public AppState lastState;
    public Object lastData;

    @Override
    public void switchTo(AppState next, Object data) {
        lastState = next;
        lastData = data;
    }
}