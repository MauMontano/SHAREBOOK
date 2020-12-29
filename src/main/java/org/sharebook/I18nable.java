package org.sharebook;

import java.util.ResourceBundle;

public interface I18nable {
	/**
	 * Updates & displays the text according to the current loaded message bundle
	 */
	void displayTextWithLang();

	/**
	 * Updates & displays the text according to the given bundle
	 * @param bundle the bundle containing the resources (the strings)
	 */
	default void displayTextWithLang(ResourceBundle bundle) {
		this.displayTextWithLang();
	}
}
