package com.example.offlinetranslator.core;

public enum Language {
    AUTO("Auto"),
    EN("English"),
    AR("Arabic"),
    HI("Hindi"),
    UR("Urdu"),
    FR("French"),
    ES("Spanish"),
    DE("German");

    public final String label;
    Language(String label) { this.label = label; }
    @Override public String toString() { return label; }
}
