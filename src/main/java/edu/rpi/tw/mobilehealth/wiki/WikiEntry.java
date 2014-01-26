package edu.rpi.tw.mobilehealth.wiki;

import java.util.HashMap;

public class WikiEntry extends HashMap<String, String> {
    /**
     *
     */
    private static final long serialVersionUID = 6896858695079717475L;

    private final WikiEntryType type;

    public WikiEntry(WikiEntryType type) {
        this.type = type;
    }

    public WikiEntryType getType() {
        return type;
    }
}
