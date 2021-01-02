package org.sharebook;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.sharebook.chat.ChatClient;
import org.sharebook.controllers.InitialViewController;

import java.io.IOException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.logging.Level;

public class ShareBook extends Application {
    private static ResourceBundle messagesBundle;
    private static Properties appConfig; // Properties is thread-safe
    private static Stage primaryStage;

    public static void main(String... args) {
        ShareBook.changeMessagesBundle(Locale.ENGLISH); // change message bundle to default

        // load configuration
        ShareBook.appConfig = new Properties();
        try {
            ShareBook.appConfig.load(ShareBook.class.getResourceAsStream("/resources/config.properties"));
        } catch (IOException e) {
            // TODO: handle error
            e.printStackTrace();
            System.exit(1);
        }

        launch(args); // launch JavaFX app
    }

    /**
     * @return the current loaded messages bundle
     */
    public static ResourceBundle getMessagesBundle() {
        return ShareBook.messagesBundle;
    }

    /**
     * @return the application configuration
     */
    public static Properties getAppConfig() {
        return ShareBook.appConfig;
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
            // ENGLISH SHOULD be in the resources, if not bad things can happen
        }

        ShareBook.messagesBundle = newBundle;
    }

    /**
     * Starts the app, sets the main scene
     * It also sets min dimensions
     * and callbacks when the user wants to close the application
     * @param primaryStage the primary stage
     */
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

        primaryStage.setOnCloseRequest((WindowEvent evt) -> ShareBook.exit());
    }

    /**
     * Exits the app by first closing the main stage
     *
     * This method will also cancel all secondary threads (like the chat thread)
     */
    public static void exit() {
        ShareBook.primaryStage.close();

        if (ChatClient.alreadyInitialized())
            ChatClient.getThread().interrupt();
    }
}
