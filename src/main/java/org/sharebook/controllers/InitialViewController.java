package org.sharebook.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.text.Text;
import org.sharebook.I18nable;
import org.sharebook.ShareBook;

import java.net.URL;
import java.util.ResourceBundle;

public class InitialViewController implements Initializable, I18nable {
    @FXML
    public Button toggleLoginRegister; // this button will toggle between login and register panels
    @FXML
    private Parent actionsPanel; // this panel will switch contents from login to register

    // i18nable texts
    @FXML
    private Text whatIsSB;
    @FXML
    private Text whatIsSBDesc;

    private boolean is_login_active = false;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        if (!this.is_login_active) {
            // TODO: add login to actionsPanel
        } else {
            // TODO: add register to actionsPanel
        }

        this.displayTextWithLang();
    }

    @Override
    public String toString() {
        return "InitialViewController{" +
                "is_login_active=" + is_login_active +
                '}';
    }

    @FXML
    private void onToggleLoginRegister(ActionEvent actionEvent) {
        this.is_login_active = !this.is_login_active;

    }

    @Override
    public void displayTextWithLang() {
        ResourceBundle messagesBundle = ShareBook.getMessagesBundle();

        String toggleButtonMessage = this.is_login_active
                ? messagesBundle.getString("toggle_register_btn")
                : messagesBundle.getString("toggle_login_btn");

        this.toggleLoginRegister.setText(toggleButtonMessage);
        this.whatIsSB.setText(messagesBundle.getString("what_is_sb"));
        this.whatIsSBDesc.setText(messagesBundle.getString("what_is_sb_desc"));
    }
}
