package org.sharebook;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import org.sharebook.controllers.LoginController;

import java.io.IOException;
import java.net.URL;

public class ShareBook extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        Parent primaryView;
        try{
            primaryView = FXMLLoader.<GridPane>load(
                    new URL(LoginController.class.getResource("/resources/views/InitialView.fxml").toString())
            );

        }catch (IOException e){
            e.printStackTrace();
            return;
        }
        Scene scene = new Scene(primaryView);
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}
