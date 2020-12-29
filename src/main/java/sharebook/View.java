package sharebook;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;

import java.net.URL;
import java.util.ResourceBundle;

public class View implements Initializable {
    //Inyecting the code
    @FXML
    private Label idHello;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

    }

    @Override
    public String toString() {
        return super.toString();
    }

    @FXML
    private void changeText(ActionEvent actionEvent) {
        idHello.setText("Welcome to sharebook");
    }
}
