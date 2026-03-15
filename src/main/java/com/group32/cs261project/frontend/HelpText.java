package com.group32.cs261project.frontend;

/**
 * Centralised provider for help text used throughout the application.
 *
 * Each method returns a formatted help description for a specific page
 * in the system. These strings are displayed in the help overlay dialog
 * when the user presses the help button on that page.
 *
 * Keeping help text here ensures that explanatory content is separated
 * from UI logic and can be easily maintained.
 */
public final class HelpText {

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private HelpText() {}

    // Returns the help text shown on the Configure Page.
    public static String configurePage() {
        return """
Use this page to set up the simulation before it starts.

• Choose the number of runways.
• Choose the length of simulation to run.
• Choose the maximum amount of time a plane is in the takeoff queue before being cancelled.
• Set the inbound and outbound traffic rates.
• Adjust the emergency probabilities - this is the chance of any inbound plane having a passenger health or mechanical emergency. 
• Select each runway's operational status and operating mode (note that these are adjustable during the run).
• Check the configuration carefully before starting, because these values determine how busy the airport will be.

NOTE: Any changes can be undone or redone using the Undo, Redo, and Reset buttons.

When you are happy with the setup, start the simulation by pressing the "Run Simulation" button.
""";
    }

    // Returns the help text shown on the Running Page.
    public static String runningPage() {
        return """
This page shows the simulation while it is running and allows you to update the runway configuration during runtime.

• The visual shows aircraft moving through the airport system.
• Emergency aircraft are highlighted in red so they are easy to spot.
• Queue and runway information updates live.
• You can change the simulation speed and each runway's operational status and operating mode during the run.
• At high simulation speeds, the visual disables itself, as the aircraft move too fast for it to be useful.

Use this page to monitor congestion, delays, and runway usage in real time.

The simulation will automatically end after the chosen simulation time.
To see the end-of-run results, click the "End Simulation" button.
Otherwise, to go back to the Configuration Page, press the "Restart" button.

To end the simulation early, click the "End Simulation" button to see the results gathered so far, or return to the Configuration Page using the "Restart" button.
""";
    }

    // Returns the help text shown on the End-of-Run Summary Page.
    public static String endOfRunSummaryPage() {
        return """
This page shows the results of a completed simulation run.

• The results are displayed alongside the configuration that produced them.
• Average values show overall performance.
• Maximum values show the worst-case situation reached during the run.
• Delay, cancellation, and diversion metrics help evaluate how well the airport operated.

Use this page to judge how successful a run was.

To compare runs, you have to have completed at least two runs.
Click the "Compare Any Two Runs" button to select two runs to compare, or the "Compare with Previous Run" button to compare the most recent run with the previous run.
""";
    }

    // Returns the help text shown on the run selection page used for comparing two completed simulation runs.
    public static String compareAnyTwoRunsPage() {
        return """
This page allows you to select two of your completed simulation runs to compare.

• Select two runs to compare.

Click the "Compare Selected Runs" button when you are ready to see the side-by-side comparison of the two runs.
""";
    }

    // Returns the help text shown on the run comparison page.
    public static String comparisonPage() {
        return """
This page gives a comparison view of two completed runs.

• Review two runs side-by-side and compare their outcomes.
• Look for patterns in delays, queues, cancellations, and diversions.
• Use this page to understand which configurations perform best.

This page is useful for analysing trends across runs.
""";
    }
}