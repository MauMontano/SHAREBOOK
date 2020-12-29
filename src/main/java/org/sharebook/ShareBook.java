package org.sharebook;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import org.sharebook.controllers.InitialViewController;

import java.io.IOException;
import java.net.URL;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.logging.Level;

public class ShareBook extends Application {
    private static ResourceBundle messagesBundle;
    private static Stage primaryStage;

    public static void main(String... args) {
        ShareBook.changeMessagesBundle(Locale.ENGLISH); // change message bundle
        launch(args); // launch JavaFX app
    }

    /**
     * @return the current loaded messages bundle
     */
    public static ResourceBundle getMessagesBundle() {
        return ShareBook.messagesBundle;
    }

    /**
     * Loads a new bundle for the given locale
     * @param locale the message's locale to be loaded
     */
    public static void changeMessagesBundle(final Locale locale) {
        ResourceBundle newBundle;

        try {
            newBundle = ResourceBundle.getBundle("resources.bundles.messages", locale);
        } catch (NullPointerException | MissingResourceException e) {
            // TODO: USE ERROR HANDLING STRATEGY
            newBundle = ResourceBundle.getBundle("resources.bundles.messages", Locale.ENGLISH);
            // US SHOULD be in the resources, if not bad things can happen
        }

        ShareBook.messagesBundle = newBundle;
    }

    @Override
    public void start(Stage primaryStage) {
        ShareBook.primaryStage = primaryStage;

        Parent primaryView;
        try {
            primaryView = FXMLLoader.load(
                    new URL(InitialViewController.class.getResource("/resources/views/InitialView.fxml").toString())
            );
        } catch (IOException e) {
            // TODO: handle exception
            e.printStackTrace();
            return;
        }
        Scene scene = new Scene(primaryView);
        primaryStage.setScene(scene);
        primaryStage.show();

        primaryStage.setMinWidth(700);
        primaryStage.setMinHeight(500);
    }

    /**
     * Exits the app by first closing the main stage
     */
    public static void exit() {
        ShareBook.primaryStage.close();
    }
}
