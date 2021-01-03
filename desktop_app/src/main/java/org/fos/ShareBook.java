/*
 * Copyright (c) 2021. Benjamín Antonio Velasco Guzmán
 * Author: Benjamín Antonio Velasco Guzmán <9benjaminguzman@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.fos;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.fos.chat.ChatClient;
import org.fos.controllers.InitialViewController;

import java.io.IOException;
import java.net.URL;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.ResourceBundle;

public class ShareBook extends Application {
    private static ResourceBundle messagesBundle;
    private static Properties appConfig; // Properties is thread-safe
    private static Stage primaryStage;

    public static void main(String... args) {
        ShareBook.changeMessagesBundle(Locale.ENGLISH); // change message bundle to default
        ShareBook.loadConfig();
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
     * Loads the application configuration from the property file
     */
    public static void loadConfig() {
        ShareBook.appConfig = new Properties();
        try {
            ShareBook.appConfig.load(ShareBook.class.getResourceAsStream("/resources/config.properties"));
        } catch (IOException e) {
            // TODO: handle error
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Starts the app, sets the main scene
     * It also sets min dimensions
     * and callbacks when the user wants to close the application
     *
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

        if (ChatClient.getThread() != null)
            ChatClient.getThread().interrupt();
    }
}
