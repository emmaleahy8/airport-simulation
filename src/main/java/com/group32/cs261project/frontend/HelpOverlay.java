package com.group32.cs261project.frontend;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;

/**
 * Utility class for adding a help button overlay to a page.
 *
 * This class wraps existing page content with a small help button placed
 * in the top-right corner. When pressed, the button opens a dialog showing
 * the supplied help title and explanatory text.
 *
 * The class is static-only and is not intended to be instantiated.
 */
public final class HelpOverlay {

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private HelpOverlay() {}

    /**
     * Wraps page content with a help button overlay.
     *
     * The help button is positioned in the top-right corner of the returned
     * container and opens a help dialog containing the supplied title and text.
     *
     * @param pageContent the main page content to wrap
     * @param title the title shown in the help dialog
     * @param helpText the explanatory help text shown in the dialog
     * @return a parent node containing the page content and help button overlay
     */
    public static Parent wrap(Parent pageContent, String title, String helpText) {
        Button helpButton = createHelpButton(title, helpText);

        StackPane overlay = new StackPane(pageContent, helpButton);
        StackPane.setAlignment(helpButton, Pos.TOP_RIGHT);
        StackPane.setMargin(helpButton, new Insets(16, 16, 0, 0));

        return overlay;
    }

    /**
     * Creates the help button used by the overlay.
     *
     * The button is styled as a circular icon and configured to open the
     * help dialog when clicked.
     *
     * @param title the help dialog title
     * @param helpText the help dialog content text
     * @return the configured help button
     */
    private static Button createHelpButton(String title, String helpText) {
        Button helpButton = new Button("?");
        helpButton.setFocusTraversable(false);

        helpButton.setMinSize(36, 36);
        helpButton.setPrefSize(36, 36);
        helpButton.setMaxSize(36, 36);

        helpButton.setStyle(
            "-fx-background-color: #0f172a;" +
            "-fx-border-color: #63e6ff;" +
            "-fx-border-width: 2;" +
            "-fx-background-radius: 18;" +
            "-fx-border-radius: 18;" +
            "-fx-text-fill: #63e6ff;" +
            "-fx-font-size: 18px;" +
            "-fx-font-weight: bold;" +
            "-fx-padding: 0;" +        
            "-fx-alignment: center;" +
            "-fx-cursor: hand;"
        );

        DropShadow glow = new DropShadow();
        glow.setColor(Color.web("#63e6ff"));
        glow.setRadius(18);
        glow.setSpread(0.4);

        helpButton.setEffect(glow);

        helpButton.setOnAction(e -> showHelpDialog(title, helpText));
        return helpButton;
    }

    /**
     * Displays the help dialog for the given page.
     *
     * The dialog contains a heading, the supplied help text, and a close
     * button. A scroll pane is used so longer help content remains readable.
     *
     * @param title the title of the page or section requiring help
     * @param helpText the explanatory help text to display
     */
    private static void showHelpDialog(String title, String helpText) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(title + " Help");
        dialog.getDialogPane().getButtonTypes().add(javafx.scene.control.ButtonType.CLOSE);

        Label heading = new Label(title);
        heading.setFont(Font.font("System", FontWeight.BOLD, 20));
        heading.setStyle("-fx-text-fill: #63e6ff;");

        Label content = new Label(helpText);
        content.setWrapText(true);
        content.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");

        VBox box = new VBox(12, heading, content);
        box.setPadding(new Insets(20));
        box.setStyle("-fx-background-color: #111827;");

        ScrollPane scrollPane = new ScrollPane(box);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefViewportWidth(520);
        scrollPane.setPrefViewportHeight(420);
        scrollPane.setStyle("-fx-background: #111827; -fx-background-color: #111827;");

        dialog.getDialogPane().setContent(scrollPane);
        dialog.getDialogPane().setStyle("-fx-background-color: #111827;");
        dialog.showAndWait();
    }
}