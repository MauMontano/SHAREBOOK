package org.sharebook.core;

import java.util.Locale;

public enum AvailableLanguage {
	EN(Locale.ENGLISH), ES(new Locale("es"));

	private final Locale locale;

	AvailableLanguage(Locale locale) {
		this.locale = locale;
	}

	public Locale getLocale() {
		return this.locale;
	}
}
