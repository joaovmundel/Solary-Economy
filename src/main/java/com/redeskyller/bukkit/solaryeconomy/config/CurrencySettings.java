package com.redeskyller.bukkit.solaryeconomy.config;

public final class CurrencySettings {

	private final String singularName;
	private final String pluralName;
	private final AbbreviationSettings abbreviations;

	public CurrencySettings(String singularName, String pluralName, AbbreviationSettings abbreviations) {
		this.singularName = singularName;
		this.pluralName = pluralName;
		this.abbreviations = abbreviations;
	}

	public String getSingularName() {
		return this.singularName;
	}

	public String getPluralName() {
		return this.pluralName;
	}

	public AbbreviationSettings getAbbreviations() {
		return this.abbreviations;
	}
}
