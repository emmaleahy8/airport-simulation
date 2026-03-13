package com.group32.cs261project;

import javafx.application.Application; 
import javafx.scene.Scene;  
import javafx.stage.Stage;  

import java.util.EnumMap; 
import java.util.Map;

import com.group32.cs261project.frontend.AppState;
import com.group32.cs261project.frontend.BackendSimulationService;
import com.group32.cs261project.frontend.ConfigurePage;
import com.group32.cs261project.frontend.Page;
import com.group32.cs261project.frontend.ResultsPage;
import com.group32.cs261project.frontend.RunningPage;
import com.group32.cs261project.frontend.SimulationService;
import com.group32.cs261project.frontend.compare.RunCompareHistoryPage;
import com.group32.cs261project.frontend.compare.RunComparePage;
// import com.group32.cs261project.frontend.StubSimulationService;

public class App extends Application {

    private Stage stage; // Main window of the application
    private final SimulationService sim = new BackendSimulationService(); // Simulation logic (stubbed for now - replace with real implementation later)

    private final Map<AppState, Page> pages = new EnumMap<>(AppState.class); // Stores all app pages for easy switching
    private AppState currentState = null; // Tracks which page is currently active

    // App entry point - sets up the stage and initializes pages
    @Override
    public void start(Stage primaryStage) {
        this.stage = primaryStage;

        // Create pages (one file per page)
        // Each page is responsible for its own UI and logic, and can call app.switchTo() to navigate to other pages
        pages.put(AppState.CONFIGURE, new ConfigurePage(this, sim));
        pages.put(AppState.RUNNING, new RunningPage(this, sim));
        pages.put(AppState.RESULTS, new ResultsPage(this, sim));
        pages.put(AppState.COMPARE, new RunComparePage(this));
        pages.put(AppState.COMPARE_HISTORY, new RunCompareHistoryPage(this, sim));

        // Set window title and initial scene then switch to the configure page
        stage.setTitle("CS261 Airport Simulation");
        stage.setScene(new Scene(pages.get(AppState.CONFIGURE).getView(), 900, 600));
        switchTo(AppState.CONFIGURE, null);
        stage.show(); // Display the window
    }

    // Method for switching between pages. Called by pages when they want to navigate somewhere else.
    public void switchTo(AppState next, Object data) {
        if (currentState != null) pages.get(currentState).onExit();
        currentState = next;

        Page page = pages.get(next);
        stage.getScene().setRoot(page.getView()); // Change the scene's root to the new page's view instead of creating a new scene (preserves window size and other properties)
        page.onEnter(data);
    }

    // Main method - launches the JavaFX application
    public static void main(String[] args) {
        launch(args);
    }
}