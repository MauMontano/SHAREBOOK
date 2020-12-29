package org.sharebook.controllers;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.sharebook.I18nable;
import org.sharebook.ShareBook;
import org.sharebook.core.AvailableLanguage;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

public class InitialViewController implements Initializable, I18nable {
	@FXML
	public Button toggleLoginRegister; // this button will toggle between login and register panels
	@FXML
	private ScrollPane mainScrollPane; // this panel will switch contents from login to register
	@FXML
	private ComboBox<String> langComboBox; // this panel will switch contents from login to register

	// i18nable texts
	@FXML
	private Text whatIsSB;
	@FXML
	private Text whatIsSBDesc;

	private boolean is_login_active = false;

	/**
	 * Sets the action panel
	 * adds languages options to the language combobox
	 * changes the text of the elements in the view according to the language
	 * @param url the url
	 * @param resourceBundle the resource bundle
	 */
	@Override
	public void initialize(URL url, ResourceBundle resourceBundle) {
		if (!this.changeActionsPanelContent()) {
			new Alert(
				Alert.AlertType.ERROR,
				"Fatal error, could not load the FXML for the register/login. Please retry",
				ButtonType.CLOSE
			);
			ShareBook.exit();
			return;
		}

		// add languages to the languages combobox
		List<String> availableLangs = Arrays.stream(AvailableLanguage.values())
			.map(AvailableLanguage::getLocale)
			.map(Locale::getDisplayLanguage)
			.collect(Collectors.toList());
		this.langComboBox.getItems().addAll(availableLangs);

		// set the current active language
		Locale currentLocale = ShareBook.getMessagesBundle().getLocale();
		Optional<AvailableLanguage> currentLang = Arrays.stream(AvailableLanguage.values())
			.filter(lang -> lang.getLocale().equals(currentLocale))
			.findAny(); // get the current lang

		// if it exists, set the current lang in the combobox
		if (currentLang.isPresent())
			this.langComboBox
				.getSelectionModel()
				.select(availableLangs.indexOf(currentLang.get().getLocale().getDisplayLanguage()));
		else
			throw new RuntimeException(
				"The current active language is not within the combobox list. Current active language: "
					+ currentLocale
			);

		// display text according to language
		this.displayTextWithLang();
	}

	/**
	 * Invoked when the user clicks the go to register/login button
	 * This methods replaces the scrollpane content to the login or register panels depending on what the user selected
	 * @param actionEvent the event
	 */
	@FXML
	private void onToggleLoginRegister(@Nullable ActionEvent actionEvent) {
		this.is_login_active = !this.is_login_active;
		this.changeToggleButtonText(ShareBook.getMessagesBundle());

		// TODO: change content inside the scrollpane
	}

	/**
	 * Invoked when the user clicks the combobox
	 * This will change the language of the displayed text
	 * @param actionEvent the event
	 */
	@FXML
	private void onChangeLanguage(@Nullable ActionEvent actionEvent) {
		if (this.langComboBox.getValue().equals(AvailableLanguage.EN.getLocale().getDisplayLanguage())) // english
			ShareBook.changeMessagesBundle(new Locale("es"));
		else if (this.langComboBox.getValue().equals(AvailableLanguage.ES.getLocale().getDisplayLanguage())) // spanish
			ShareBook.changeMessagesBundle(new Locale("en"));
		else
			System.err.println("This shouldn't be printed");
		// TODO: log the abnormal execution

		this.displayTextWithLang();
	}

	/**
	 * Changes the text for the toggle register/login button
	 * @param messagesBundle the bundle containing the messages to show
	 */
	private void changeToggleButtonText(@NotNull ResourceBundle messagesBundle) {
		String toggleButtonMessage = this.is_login_active
			? messagesBundle.getString("toggle_register_btn")
			: messagesBundle.getString("toggle_login_btn");
		this.toggleLoginRegister.setText(toggleButtonMessage);
	}

	/**
	 * Replaces the actions panel content
	 * It changes the login for the register panel
	 * or vice versa
	 * @return true if the panel could be correctly loaded
	 */
	private boolean changeActionsPanelContent() {
		String fxmlPath = "/resources/views/Login.fxml";
		if (this.is_login_active)
			fxmlPath = "/resources/views/Register.fxml";

		Parent panel;
		try {
			panel = FXMLLoader.load(this.getClass().getResource(fxmlPath));
		} catch (IOException e) {
			// TODO: handle the exception
			return false;
		}
		this.mainScrollPane.setContent(panel);

		return true;
	}

	/**
	 * Displays the text for the view in the current selected language
	 */
	@Override
	public void displayTextWithLang() {
		ResourceBundle messagesBundle = ShareBook.getMessagesBundle();

		this.whatIsSB.setText(messagesBundle.getString("what_is_sb"));
		this.whatIsSBDesc.setText(messagesBundle.getString("what_is_sb_desc"));
		this.changeToggleButtonText(messagesBundle);
	}

	@Override
	public String toString() {
		return "InitialViewController{" +
			"is_login_active=" + is_login_active +
			'}';
	}
}
